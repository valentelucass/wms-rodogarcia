package br.com.rodogarcia.wms.services;

import br.com.rodogarcia.wms.config.NotazzProperties;
import br.com.rodogarcia.wms.dto.FiscalNfeDto;
import br.com.rodogarcia.wms.dto.PaginaResponse;
import br.com.rodogarcia.wms.exceptions.RegraNegocioException;
import br.com.rodogarcia.wms.models.SolicitacaoNfe;
import br.com.rodogarcia.wms.repositories.FiscalNfeRepository;
import br.com.rodogarcia.wms.repositories.PedidoSaidaRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import tools.jackson.databind.json.JsonMapper;

/** Orquestra I/O fora das transações curtas de FiscalNfeTransacaoService. */
@Service
@Validated
@ConditionalOnProperty(name="wms.cadastros.enabled",havingValue="true")
public class FiscalNfeService {
    private final FiscalNfeRepository repository;
    private final FiscalNfePreparacaoService preparacao;
    private final FiscalNfeTransacaoService transacoes;
    private final PedidoSaidaRepository pedidos;
    private final AcessoService acesso;
    private final NotazzProperties properties;
    private final NotazzGateway gateway;
    private final JsonMapper mapper;
    private final org.springframework.context.ApplicationEventPublisher events;
    private final ExpedicaoService expedicao;
    private final FiscalNfeXml xml;
    public FiscalNfeService(FiscalNfeRepository repository,FiscalNfePreparacaoService preparacao,FiscalNfeTransacaoService transacoes,
            PedidoSaidaRepository pedidos,AcessoService acesso,NotazzProperties properties,NotazzGateway gateway,JsonMapper mapper,org.springframework.context.ApplicationEventPublisher events,ExpedicaoService expedicao,FiscalNfeXml xml) {
        this.repository=repository;this.preparacao=preparacao;this.transacoes=transacoes;this.pedidos=pedidos;this.acesso=acesso;this.properties=properties;this.gateway=gateway;this.mapper=mapper;this.events=events;
        this.expedicao=expedicao;this.xml=xml;
    }
    @Transactional(readOnly=true)
    public PaginaResponse<FiscalNfeDto.Linha> listar(@NotNull @Positive Long clienteId,@NotNull @Positive Long armazemId,String situacao,String busca,int pagina,int tamanho) {
        acesso.cliente(clienteId);acesso.armazem(armazemId);estrutura();CadastroSupport.pagina(pagina,tamanho);
        if(!Set.of("TODOS","PENDENTE","FINALIZADO").contains(situacao))throw CadastroSupport.invalido("Situação fiscal inválida.");
        String termo=busca==null?"":busca.strip();if(termo.length()>40)throw CadastroSupport.invalido("Busca excede 40 caracteres.");
        long total=repository.total(clienteId,armazemId,situacao,termo);
        var rows=repository.fila(clienteId,armazemId,situacao,termo,pagina,tamanho).stream().map(r->{
            String numeros=(String)r.get("numeros");String status=(String)r.get("situacao_fiscal");
            return new FiscalNfeDto.Linha(((Number)r.get("id")).longValue(),((Number)r.get("versao")).longValue(),(String)r.get("referencia"),((Timestamp)r.get("criado_em")).toInstant(),clienteId,(String)r.get("cliente"),armazemId,(String)r.get("situacao"),status,numeros==null?List.<String>of():Arrays.asList(numeros.split(", ")),"PENDENTE".equals(status)&&"SEPARADO".equals(r.get("situacao")));
        }).toList();
        return new PaginaResponse<>(rows,pagina,tamanho,total,(int)((total+tamanho-1)/tamanho));
    }
    public FiscalNfeDto.Previa previa(@NotNull @Positive Long pedidoId,@NotNull @Valid FiscalNfeDto.Preparar comando) {
        acesso.exigirSupervisor();estrutura();return preparacao.preparar(pedidoId,comando).previa();
    }
    public FiscalNfeDto.Andamento confirmar(@NotNull @Positive Long pedidoId,@NotNull @Valid FiscalNfeDto.Confirmar comando) {
        acesso.exigirSupervisor();estrutura();var confirmadas=transacoes.confirmar(pedidoId,comando);
        try { events.publishEvent(new FiscalNfeEnvioService.Confirmada(pedidoId,confirmadas.stream().map(SolicitacaoNfe::id).toList())); }
        catch(org.springframework.core.task.TaskRejectedException ex) { /* Intenções já persistidas ficam recuperáveis no acompanhamento. */ }
        return andamento(pedidoId);
    }
    @Transactional(readOnly=true)
    public FiscalNfeDto.Andamento andamento(@NotNull @Positive Long pedidoId) {
        var escopo=pedidos.buscarEscopo(pedidoId).orElseThrow(RegraNegocioException::naoEncontrado);
        acesso.cliente(escopo.getClienteId());acesso.armazem(escopo.getArmazemId());estrutura();
        var list=repository.doPedido(pedidoId);
        return new FiscalNfeDto.Andamento(pedidoId,repository.situacaoFiscal(pedidoId),list.stream().map(s->dto(s,pedidoId)).toList());
    }
    @Transactional(propagation=Propagation.NEVER)
    public FiscalNfeDto.Andamento processar(@NotNull @Positive Long pedidoId,@NotNull UUID solicitacaoId) {
        acesso.exigirSupervisor();estrutura();String id=solicitacaoId.toString();var atual=transacoes.obter(pedidoId,id);
        executar(pedidoId,id,atual.situacao().equals("PREPARADA")?"CRIAR":"CONSULTAR",null);
        return andamento(pedidoId);
    }
    @Transactional(propagation=Propagation.NEVER)
    public FiscalNfeDto.Andamento cancelar(@NotNull @Positive Long pedidoId,@NotNull UUID solicitacaoId,@NotNull @Valid FiscalNfeDto.Comando comando) {
        acesso.exigirSupervisor();estrutura();executar(pedidoId,solicitacaoId.toString(),"CANCELAR",comando);return andamento(pedidoId);
    }
    private void executar(Long pedidoId,String id,String acao,FiscalNfeDto.Comando comando) {
        String dono=UUID.randomUUID().toString();var s=transacoes.adquirir(pedidoId,id,acao,comando,dono);if(s==null)return;
        var escopo=pedidos.buscarEscopo(pedidoId).orElseThrow(RegraNegocioException::naoEncontrado);
        var emitente=properties.getEmitentes().get(escopo.getArmazemId());
        var dados=new LinkedHashMap<String,Object>();
        if(acao.equals("CRIAR"))dados.putAll(mapper.readValue(s.corpoJson(),Map.class));
        dados.put("EXTERNAL_ID",s.notazzId()==null||acao.equals("CRIAR")?s.id():"");
        if(!acao.equals("CRIAR"))dados.put("DOCUMENT_ID",s.notazzId()==null?"":s.notazzId());
        if(acao.equals("CANCELAR"))dados.put("REASON",comando.motivo());
        try {
            var retorno=gateway.chamar(emitente,acao.equals("CRIAR")?"create_nfe_55":acao.equals("CANCELAR")?"cancel_nfe_55":"consult_nfe_55",dados);
            transacoes.recebido(pedidoId,id,acao,retorno,dono);
        } catch(NotazzGateway.ResultadoDesconhecido ex) {transacoes.falhou(pedidoId,id,dono,false);}
        catch(RegraNegocioException|IllegalArgumentException ex) {transacoes.falhou(pedidoId,id,dono,true);}
    }
    public FiscalNfeDto.Arquivo arquivo(@NotNull @Positive Long pedidoId,@NotNull UUID solicitacaoId) {
        estrutura();var s=transacoes.obter(pedidoId,solicitacaoId.toString());
        if(s.xml()==null)throw CadastroSupport.invalido("XML autorizado ainda não está disponível.");
        return new FiscalNfeDto.Arquivo("NFe-"+s.numero()+".xml",s.xml());
    }
    @Transactional(readOnly=true)
    public FiscalNfeDto.RetornoOperacional retorno(@NotNull @Positive Long pedidoId,@NotNull UUID solicitacaoId) {
        acesso.exigirSupervisor();estrutura();var s=transacoes.obter(pedidoId,solicitacaoId.toString());
        if(!s.natureza().equals("REGULARIZACAO_ENTRADA")||!s.situacao().equals("AUTORIZADA")||repository.regularizada(s.id()))throw CadastroSupport.invalido("A entrada fiscal deve estar autorizada e aguardar o processamento físico.");
        var snap=mapper.readValue(s.documentoJson(),FiscalNfeDto.DocumentoPlanejado.class);var auth=xml.conferir(s.xml(),snap);var d=expedicao.consultar(pedidoId);
        var ids=repository.doPedido(pedidoId).stream().filter(a->a.natureza().equals("REGULARIZACAO_ENTRADA")&&a.situacao().equals("AUTORIZADA")&&!repository.regularizada(a.id())).map(SolicitacaoNfe::id).toList();
        var itens=new java.util.ArrayList<br.com.rodogarcia.wms.dto.ExpedicaoDto.ItemDevolucao>();
        for(var i:snap.itens()) {
            var baixa=d.baixas().stream().filter(b->b.reservaId().equals(i.reservaId())&&b.entradaOrigemId().equals(i.entradaOrigemId())).findFirst().orElse(null);
            if(baixa!=null)itens.add(new br.com.rodogarcia.wms.dto.ExpedicaoDto.ItemDevolucao(baixa.id(),i.quantidade(),java.math.BigDecimal.ZERO));
        }
        return new FiscalNfeDto.RetornoOperacional(pedidoId,d.pedido().versao(),d.retiradaEm()!=null,ids,s.id(),auth.nota(),itens);
    }
    private FiscalNfeDto.Solicitacao dto(SolicitacaoNfe s,Long pedidoId) {
        boolean supervisor=acesso.supervisor();
        return new FiscalNfeDto.Solicitacao(s.id(),s.planoId(),s.natureza(),s.situacao(),s.numero(),s.serie(),s.chaveAcesso(),s.protocolo(),s.documentoId(),s.documentoOrigemId(),s.mensagem(),supervisor&&!s.situacao().equals("CANCELADA"),supervisor&&s.situacao().equals("AUTORIZADA")&&!s.natureza().equals("REGULARIZACAO_ENTRADA"),s.xml()!=null,s.criadaEm(),s.atualizadaEm());
    }
    private void estrutura() {if(!repository.disponivel())throw RegraNegocioException.conflito("ESTRUTURA_FISCAL_PENDENTE","A estrutura da integração fiscal ainda não foi ativada neste ambiente.");}
}
