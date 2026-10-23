package br.com.rodogarcia.wms.services;

import br.com.rodogarcia.wms.config.NotazzProperties;
import br.com.rodogarcia.wms.dto.ExpedicaoDto;
import br.com.rodogarcia.wms.dto.FiscalCadastroDto;
import br.com.rodogarcia.wms.dto.FiscalNfeDto;
import br.com.rodogarcia.wms.exceptions.RegraNegocioException;
import br.com.rodogarcia.wms.models.CoberturaDocumentoSaida;
import br.com.rodogarcia.wms.models.ComplementoFiscal;
import br.com.rodogarcia.wms.models.DocumentoSaida;
import br.com.rodogarcia.wms.models.EntradaConferida;
import br.com.rodogarcia.wms.models.PedidoSaida;
import br.com.rodogarcia.wms.models.ReferenciaFiscalProduto;
import br.com.rodogarcia.wms.models.SituacaoCadastro;
import br.com.rodogarcia.wms.models.SituacaoPedidoSaida;
import br.com.rodogarcia.wms.models.SituacaoReservaSaida;
import br.com.rodogarcia.wms.models.TipoEndereco;
import br.com.rodogarcia.wms.models.SolicitacaoNfe;
import br.com.rodogarcia.wms.repositories.BaixaSaidaRepository;
import br.com.rodogarcia.wms.repositories.CoberturaDocumentoSaidaRepository;
import br.com.rodogarcia.wms.repositories.ConteudoUnidadeRepository;
import br.com.rodogarcia.wms.repositories.DocumentoSaidaRepository;
import br.com.rodogarcia.wms.repositories.FiscalNfeRepository;
import br.com.rodogarcia.wms.repositories.PedidoSaidaRepository;
import br.com.rodogarcia.wms.repositories.ReferenciaFiscalProdutoRepository;
import br.com.rodogarcia.wms.repositories.ReservaSaidaRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

@Service
@Transactional(readOnly=true)
@ConditionalOnProperty(name="wms.cadastros.enabled",havingValue="true")
public class FiscalNfePreparacaoService {
    public record Preparacao(FiscalNfeDto.Previa previa,List<String> corpos) {}
    private final PedidoSaidaRepository pedidos;
    private final ReservaSaidaRepository reservas;
    private final ConteudoUnidadeRepository conteudos;
    private final ReferenciaFiscalProdutoRepository referencias;
    private final BaixaSaidaRepository baixas;
    private final DocumentoSaidaRepository documentos;
    private final CoberturaDocumentoSaidaRepository coberturas;
    private final FiscalNfeRepository fiscal;
    private final ExpedicaoService expedicao;
    private final EstoqueService estoque;
    private final AcessoService acesso;
    private final NotazzProperties properties;
    private final JsonMapper mapper;
    public FiscalNfePreparacaoService(PedidoSaidaRepository pedidos,ReservaSaidaRepository reservas,
            ConteudoUnidadeRepository conteudos,ReferenciaFiscalProdutoRepository referencias,
            BaixaSaidaRepository baixas,DocumentoSaidaRepository documentos,CoberturaDocumentoSaidaRepository coberturas,
            FiscalNfeRepository fiscal,ExpedicaoService expedicao,EstoqueService estoque,AcessoService acesso,
            NotazzProperties properties,JsonMapper mapper) {
        this.pedidos=pedidos;this.reservas=reservas;this.conteudos=conteudos;this.referencias=referencias;
        this.baixas=baixas;this.documentos=documentos;this.coberturas=coberturas;this.fiscal=fiscal;
        this.expedicao=expedicao;this.estoque=estoque;this.acesso=acesso;this.properties=properties;this.mapper=mapper;
    }
    public Preparacao preparar(Long id,FiscalNfeDto.Preparar comando) {
        acesso.exigirSupervisor();
        var p=pedidos.findById(id).orElseThrow(RegraNegocioException::naoEncontrado);
        acesso.cliente(p.getCliente().getId());acesso.armazem(p.getArmazem().getId());
        var erros=new ArrayList<String>();
        boolean entrada="REGULARIZACAO_ENTRADA".equals(comando.natureza());
        if(p.getVersao()!=comando.versao()) erros.add("O pedido mudou. Atualize a consulta antes de conferir.");
        if(!entrada && p.getSituacao()!=SituacaoPedidoSaida.SEPARADO) erros.add("A emissão exige o pedido integralmente separado.");
        if(entrada && !Set.of(SituacaoPedidoSaida.SEPARADO,SituacaoPedidoSaida.RETIRADO).contains(p.getSituacao())) erros.add("A regularização exige pedido separado ou retirado.");
        if(p.getCliente().getSituacao()!=SituacaoCadastro.ATIVO || p.getArmazem().getSituacao()!=SituacaoCadastro.ATIVO) erros.add("Cliente/armazém deve estar ativo para nova emissão.");
        var emitente=properties.getEmitentes().get(p.getArmazem().getId());
        if(!properties.isEnabled()) erros.add("A integração Notazz ainda não está habilitada.");
        if(emitente==null || !Objects.equals(emitente.getCnpj(),p.getArmazem().getDocumentoFiscal())) erros.add("Vincule o CNPJ deste armazém à empresa emissora correta na Notazz.");
        if(emitente==null || emitente.getApiKey()==null || emitente.getApiKey().isBlank() || !emitente.isConfiguracaoConferida()) erros.add("A configuração protegida e a conferência fiscal da empresa emissora estão pendentes.");
        int ambiente=emitente==null?2:emitente.getAmbiente();
        if(!Set.of(1,2).contains(ambiente) || ambiente==1 && !properties.isProductionEnabled()) erros.add("Ambiente fiscal não liberado para emissão.");
        String finalidade=emitente==null?null:entrada?emitente.getFinalidadeEntrada():emitente.getFinalidade();
        String natureza=emitente==null?null:entrada?emitente.getNaturezaEntrada():emitente.getNaturezaOperacao();
        if(finalidade==null || !finalidade.matches("[1-6]") || natureza==null || natureza.isBlank()) erros.add("Configure finalidade e natureza fiscal da operação, conferidas pelo fiscal.");
        dados(p.getCliente().getComplementoFiscal(),p.getCliente().getCidade(),p.getCliente().getUf(),"Cliente",erros);
        dados(p.getArmazem().getComplementoFiscal(),p.getArmazem().getCidade(),p.getArmazem().getUf(),"Armazém",erros);
        var cf=p.getCliente().getComplementoFiscal();
        String agrupamento=cf==null?null:cf.getAgrupamentoNfe();
        if(!Set.of("CONSOLIDAR","POR_NOTA").contains(Objects.toString(agrupamento,""))) erros.add("Cliente: informe o critério de agrupamento das notas de entrada.");
        if(cf==null || cf.getIndicadorIe()==null || !Set.of(1,2,9).contains(cf.getIndicadorIe())) erros.add("Cliente: informe o indicador de inscrição estadual (1, 2 ou 9).");
        if(cf!=null && Integer.valueOf(1).equals(cf.getIndicadorIe()) && vazio(cf.getInscricaoEstadual())) erros.add("Cliente contribuinte: informe a inscrição estadual.");
        String destinatario=p.getCliente().getDocumentoFiscal();
        if(destinatario==null || !destinatario.matches("[0-9]{11}|[0-9A-Z]{14}")) erros.add("Cliente: CPF/CNPJ fiscal inválido.");
        DocumentoSaida origem=null;
        if(entrada) {
            origem=comando.documentoOrigemId()==null?null:documentos.findById(comando.documentoOrigemId()).orElse(null);
            if(origem==null || !origem.getPedido().getId().equals(id) || !"AUTORIZADO".equals(origem.getSituacao()) || !"RETORNO_MERCADORIA".equals(origem.getNatureza())) erros.add("Selecione uma nota de saída física ativa deste pedido para a nova entrada.");
            if(origem!=null) {
                Long documentoDaSaida=origem.getId();
                if(fiscal.doPedido(id).stream().anyMatch(s->Objects.equals(s.documentoId(),documentoDaSaida)&&!s.situacao().equals("AUTORIZADA")))erros.add("A nota de saída tem cancelamento ou conciliação pendente. Resolva o resultado antes de emitir uma entrada em paralelo.");
            }
        } else if(comando.documentoOrigemId()!=null) erros.add("Documento de saída de referência pertence somente à regularização de entrada.");
        var linhas=reservas.buscarDoPedido(id);
        var itensSaida=new HashMap<String,FiscalNfeDto.Item>();
        if(entrada && origem!=null) {
            long documentoOrigem=origem.getId();
            fiscal.doPedido(id).stream().filter(s->Objects.equals(s.documentoId(),documentoOrigem)).findFirst().ifPresent(s->{
                var snapshot=mapper.readValue(s.documentoJson(),FiscalNfeDto.DocumentoPlanejado.class);
                for(var i:snapshot.itens())itensSaida.put(i.reservaId()+":"+i.entradaOrigemId(),i);
            });
        }
        var coberturasAtuais=coberturas.buscarDoPedido(id);
        var grupos=new LinkedHashMap<String,List<FiscalNfeDto.Item>>();
        var rateados=new HashMap<Long,BigDecimal>();
        var tributacoes=new HashMap<Long,ReferenciaFiscalProduto>();
        if(linhas.isEmpty()) erros.add("O pedido não possui reservas para conferir.");
        for(var r:linhas) {
            if(!entrada && r.getSituacao()!=SituacaoReservaSaida.ATIVA) continue;
            var u=r.getUnidade();var produto=u.getProduto();
            if(!u.getPedido().getArmazem().getId().equals(p.getArmazem().getId()) || !u.getPedido().getCliente().getId().equals(p.getCliente().getId())) { erros.add("A origem da reserva não pertence ao cliente/armazém deste pedido.");continue; }
            if(!entrada && !estoque.elegiveisSaida(List.of(u.getId()),p.getId(),TipoEndereco.SEPARACAO,false).contains(u.getId())) erros.add("Reserva "+r.getId()+": mercadoria indisponível ou impedida; resolva a condição física.");
            var ref=referencias.findByProdutoIdAndArmazemIdAndOperacao(produto.getId(),p.getArmazem().getId(),comando.natureza()).orElse(null);
            if(ref==null || vazio(ref.getNcm()) || vazio(ref.getCfop()) || ref.getConferidaEm()==null || vazio(ref.getConferidaPor()) || ref.getTributacaoNfe()==null) { erros.add("Produto "+produto.getSku()+": complete e confira a referência fiscal da operação "+comando.natureza()+".");continue; }
            tributacoes.put(produto.getId(),ref);
            BigDecimal quantidade=r.getQuantidade();BigDecimal pular=BigDecimal.ZERO;
            if(entrada) {
                if(origem==null) continue;
                long origemId=origem.getId();
                quantidade=coberturasAtuais.stream().filter(c->c.getDocumento().getId().equals(origemId)&&c.getReserva().getId().equals(r.getId())).map(CoberturaDocumentoSaida::getQuantidade).reduce(BigDecimal.ZERO,BigDecimal::add);
            } else if("RETORNO_MERCADORIA".equals(comando.natureza())) {
                var ja=coberturasAtuais.stream().filter(c->c.getReserva().getId().equals(r.getId())&&"AUTORIZADO".equals(c.getDocumento().getSituacao())&&"RETORNO_MERCADORIA".equals(c.getDocumento().getNatureza())).map(CoberturaDocumentoSaida::getQuantidade).reduce(BigDecimal.ZERO,BigDecimal::add);
                quantidade=quantidade.subtract(ja);pular=ja;
            }
            if(quantidade.signum()<=0) continue;
            var fontes=new ArrayList<Map.Entry<EntradaConferida,BigDecimal>>();
            if(p.getSituacao()==SituacaoPedidoSaida.RETIRADO) {
                for(var b:baixas.buscarDaUnidade(u.getId())) if(b.getReserva().getId().equals(r.getId())) fontes.add(Map.entry(b.getEntradaOrigem(),b.getQuantidade()));
            } else for(var c:conteudos.buscarOrigens(u.getId())) fontes.add(Map.entry(c.getEntrada(),c.getQuantidade()));
            for(var fonte:fontes) {
                if(quantidade.signum()==0) break;
                var item=fonte.getKey().getItemChegada().getItemNota();var nota=item.getNota();
                BigDecimal disponivel=fonte.getValue();
                BigDecimal pulada=disponivel.min(pular);pular=pular.subtract(pulada);disponivel=disponivel.subtract(pulada);
                BigDecimal q=quantidade.min(disponivel);
                var parcelaSaida=itensSaida.get(r.getId()+":"+fonte.getKey().getId());
                if(entrada&&!itensSaida.isEmpty()) {
                    if(parcelaSaida==null)continue;
                    q=parcelaSaida.quantidade();
                    if(q.compareTo(disponivel)>0||q.compareTo(quantidade)>0){erros.add("A composição do retorno diverge da nota de saída original.");continue;}
                }
                if(q.signum()<=0) continue;
                if(vazio(nota.getChaveAcesso())) {erros.add("Nota de origem "+nota.getNumero()+": falta a chave de acesso.");continue;}
                if(item.getValorMercadoria()==null) {erros.add("Nota de origem "+nota.getNumero()+", SKU "+produto.getSku()+": falta o valor fiscal da entrada.");continue;}
                BigDecimal anterior=rateados.computeIfAbsent(item.getId(),key->entrada||!"RETORNO_MERCADORIA".equals(comando.natureza())?BigDecimal.ZERO:fiscal.quantidadeComprometida(key));
                BigDecimal valor;
                try {valor=parcelaSaida!=null?parcelaSaida.valorTotal():FiscalNfeValores.parcela(item.getValorMercadoria(),item.getQuantidadePrevista(),anterior,q);} catch(RegraNegocioException ex) {erros.add("Origem "+nota.getNumero()+": quantidades já documentadas exigem conciliação.");continue;}
                rateados.put(item.getId(),anterior.add(q));
                var linha=new FiscalNfeDto.Item(r.getId(),fonte.getKey().getId(),item.getId(),nota.getId(),produto.getId(),produto.getSku(),produto.getDescricao(),produto.getUnidadeMedida(),q,valor.divide(q,10,RoundingMode.HALF_UP),valor,ref.getNcm(),ref.getCfop(),nota.getChaveAcesso(),u.getRevisaoConteudo());
                String grupo=entrada?"ENTRADA-"+comando.documentoOrigemId():"POR_NOTA".equals(agrupamento)?"NOTA-"+nota.getId():"CONSOLIDADO";
                grupos.computeIfAbsent(grupo,key->new ArrayList<>()).add(linha);
                quantidade=quantidade.subtract(q);
            }
            if(quantidade.signum()>0) erros.add("Reserva "+r.getId()+": origens não cobrem integralmente a quantidade fiscal.");
        }
        var planejados=new ArrayList<FiscalNfeDto.DocumentoPlanejado>();var corpos=new ArrayList<String>();
        for(var entry:grupos.entrySet()) {
            var itens=entry.getValue();var cov=new LinkedHashMap<Long,ExpedicaoDto.Cobertura>();
            for(var item:itens) {var old=cov.get(item.reservaId());cov.put(item.reservaId(),new ExpedicaoDto.Cobertura(item.reservaId(),item.notaOrigemId(),item.sku(),item.quantidade().add(old==null?BigDecimal.ZERO:old.quantidade())));}
            var chaves=entrada && origem!=null?List.of(origem.getChaveAcesso()==null?"":origem.getChaveAcesso()):itens.stream().map(FiscalNfeDto.Item::chaveOrigem).distinct().toList();
            if(chaves.stream().anyMatch(c->!c.matches("[0-9]{44}"))) erros.add("Documento referenciado sem chave de acesso fiscal válida.");
            var doc=new FiscalNfeDto.DocumentoPlanejado(entry.getKey(),comando.natureza(),p.getArmazem().getDocumentoFiscal(),destinatario,cf==null?p.getCliente().getNome():cf.getRazaoSocial(),natureza,finalidade,ambiente,comando.documentoOrigemId(),List.copyOf(itens),List.copyOf(cov.values()),chaves,itens.stream().map(FiscalNfeDto.Item::valorTotal).reduce(BigDecimal.ZERO,BigDecimal::add),"Saída WMS: "+p.getReferencia());
            if(itens.size()>200)erros.add("A nota excede o limite de 200 linhas conferidas por documento; revise o agrupamento fiscal.");
            planejados.add(doc);
            try {corpos.add(corpo(p,doc,tributacoes,emitente));} catch(RuntimeException ex) {erros.add("Confira a configuração fiscal do estabelecimento e dos produtos.");corpos.add("{}");}
        }
        if(planejados.isEmpty()) erros.add("Não há quantidade fiscal pendente para esta operação. Consulte os documentos existentes.");
        String hash=NfeXmlService.hash(mapper.writeValueAsString(List.of(p.getVersao(),planejados,corpos)));
        return new Preparacao(new FiscalNfeDto.Previa(id,p.getVersao(),hash,erros.isEmpty(),properties.isEnabled(),erros.stream().distinct().toList(),List.copyOf(planejados),expedicao.consultar(id)),List.copyOf(corpos));
    }
    public void validarEnvio(SolicitacaoNfe solicitacao,PedidoSaida pedido) {
        var snapshot=mapper.readValue(solicitacao.documentoJson(),FiscalNfeDto.DocumentoPlanejado.class);
        CadastroSupport.ativo(pedido.getCliente());CadastroSupport.ativo(pedido.getArmazem());
        if(snapshot.natureza().equals("REGULARIZACAO_ENTRADA")) {
            var d=documentos.findById(snapshot.documentoOrigemId()).orElseThrow(RegraNegocioException::naoEncontrado);
            if(!"AUTORIZADO".equals(d.getSituacao())||!d.getPedido().getId().equals(pedido.getId()))throw CadastroSupport.invalido("A nota de saída referenciada mudou; confira a regularização.");
            return;
        }
        var atuais=reservas.buscarDoPedido(pedido.getId());
        for(var item:snapshot.itens()) {
            var r=atuais.stream().filter(x->x.getId().equals(item.reservaId())).findFirst().orElseThrow(RegraNegocioException::naoEncontrado);
            CadastroSupport.ativo(r.getUnidade().getProduto());
            if(r.getSituacao()!=SituacaoReservaSaida.ATIVA || r.getUnidade().getRevisaoConteudo()!=item.revisaoConteudo()
                    || !estoque.elegiveisSaida(List.of(r.getUnidade().getId()),pedido.getId(),TipoEndereco.SEPARACAO,false).contains(r.getUnidade().getId()))throw RegraNegocioException.conflito("RESERVA_IMPEDIDA","A composição ou a condição física mudou. Nenhuma nova nota foi enviada.");
        }
    }
    private String corpo(PedidoSaida p,FiscalNfeDto.DocumentoPlanejado doc,Map<Long,ReferenciaFiscalProduto> refs,NotazzProperties.Emitente emissor) {
        var cf=p.getCliente().getComplementoFiscal();var body=new LinkedHashMap<String,Object>();
        if(cf==null || emissor==null) return "{}";
        body.put("DESTINATION_NAME",doc.destinatarioNome());body.put("DESTINATION_TAXID",doc.destinatarioDocumento());
        body.put("DESTINATION_TAXTYPE",doc.destinatarioDocumento().length()==11?"F":"J");
        put(body,"DESTINATION_IE",cf.getInscricaoEstadual());put(body,"DESTINATION_STREET",cf.getLogradouro());put(body,"DESTINATION_NUMBER",cf.getNumeroEndereco());
        put(body,"DESTINATION_COMPLEMENT",cf.getComplemento());put(body,"DESTINATION_DISTRICT",cf.getBairro());put(body,"DESTINATION_CITY",p.getCliente().getCidade());
        put(body,"DESTINATION_UF",p.getCliente().getUf());put(body,"DESTINATION_ZIPCODE",cf.getCep());put(body,"DESTINATION_COUNTRY",cf.getPais());
        body.put("DOCUMENT_BASEVALUE",doc.valorTotal().toPlainString());body.put("DOCUMENT_NATURE_OPERATION",doc.naturezaOperacao());body.put("DOCUMENT_GOAL",doc.finalidade());
        body.put("DOCUMENT_OPERATION_TYPE",doc.natureza().equals("REGULARIZACAO_ENTRADA")?"0":"1");body.put("DOCUMENT_DESCRIPTION",doc.observacao());
        body.put("SALE_ID",p.getReferencia());body.put("TRANSMITIR","1");body.put("SINGLE_NOTE_UPSELL","0");body.put("REQUEST_ORIGIN","WMS_RODOGARCIA");
        var custom=mapper.readTree(emissor.getDocumentoConfiguracao()).deepCopy();
        if(!custom.isObject()) throw CadastroSupport.invalido("Configuração fiscal inválida.");
        var objeto=(tools.jackson.databind.node.ObjectNode)custom;
        var ide=objeto.withObject("/ide");ide.put("tpAmb",doc.ambiente());ide.put("tpNF",doc.natureza().equals("REGULARIZACAO_ENTRADA")?0:1);
        ide.put("finNFe",doc.finalidade());ide.put("natOp",doc.naturezaOperacao());
        var nfref=ide.putArray("NFref");for(var chave:doc.chavesReferenciadas())nfref.addObject().put("refNFe",chave);
        if(objeto.has("det")||objeto.has("total"))throw CadastroSupport.invalido("Itens e totais são montados a partir do pedido; configure a tributação nos produtos.");
        var emit=objeto.withObject("/emit");emit.remove("CPF");emit.put("CNPJ",doc.emitenteCnpj());
        var dest=objeto.withObject("/dest");dest.remove("CNPJ");dest.remove("CPF");dest.put(doc.destinatarioDocumento().length()==11?"CPF":"CNPJ",doc.destinatarioDocumento());
        dest.put("indIEDest",cf.getIndicadorIe());dest.withObject("/enderDest").put("cMun",cf.getCodigoMunicipio());
        var adicional=objeto.withObject("/infAdic");String obs=adicional.path("infCpl").asString("");adicional.put("infCpl",(obs.isBlank()?"":obs+" | ")+doc.observacao());
        body.put("DOCUMENT_CUSTOM_CONFIGURATION",base64(mapper.writeValueAsString(objeto)));
        var produtos=new LinkedHashMap<String,Object>();int index=0;
        for(var item:doc.itens()) {
            var ref=refs.get(item.produtoId());var t=mapper.readValue(ref.getTributacaoNfe(),FiscalCadastroDto.Tributacao.class);
            if(vazio(t.origem())||vazio(t.icmsCst())||vazio(t.pisCst())||vazio(t.cofinsCst())) throw CadastroSupport.invalido("Tributação incompleta.");
            var x=new LinkedHashMap<String,Object>();x.put("DOCUMENT_PRODUCT_COD",item.sku());x.put("DOCUMENT_PRODUCT_NAME",item.descricao());
            x.put("DOCUMENT_PRODUCT_QTD",item.quantidade().toPlainString());x.put("DOCUMENT_PRODUCT_UNITARY_VALUE",item.valorUnitario().toPlainString());
            x.put("DOCUMENT_PRODUCT_NCM",item.ncm());x.put("DOCUMENT_PRODUCT_CFOP",item.cfop());put(x,"DOCUMENT_PRODUCT_CEST",ref.getCest());
            put(x,"DOCUMENT_PRODUCT_ICMS_CST",t.icmsCst());put(x,"DOCUMENT_PRODUCT_IPI_CST",t.ipiCst());put(x,"DOCUMENT_PRODUCT_PIS_CST",t.pisCst());put(x,"DOCUMENT_PRODUCT_COFINS_CST",t.cofinsCst());
            put(x,"DOCUMENT_PRODUCT_ICMS_ALIQUOTA",ref.getAliquotaIcms());put(x,"DOCUMENT_PRODUCT_IPI_ALIQUOTA",ref.getAliquotaIpi());
            put(x,"DOCUMENT_PRODUCT_PIS_ALIQUOTA",t.aliquotaPis());put(x,"DOCUMENT_PRODUCT_COFINS_ALIQUOTA",t.aliquotaCofins());put(x,"DOCUMENT_PRODUCT_TAX_COD_BENEF",t.codigoBeneficio());
            put(x,"DOCUMENT_PRODUCT_IBS_UF",t.aliquotaIbsUf());put(x,"DOCUMENT_PRODUCT_IBS_MUN",t.aliquotaIbsMunicipio());put(x,"DOCUMENT_PRODUCT_CBS",t.aliquotaCbs());put(x,"DOCUMENT_PRODUCT_IS",t.aliquotaIs());
            put(x,"DOCUMENT_PRODUCT_CCLASSTRIBIBSCBS",t.classificacaoIbsCbs());put(x,"DOCUMENT_PRODUCT_CCLASSTRIBIS",t.classificacaoIs());
            BigDecimal fator=t.fatorTributavel()==null?BigDecimal.ONE:t.fatorTributavel();String unidade=t.unidadeTributavel()==null?item.unidade():t.unidadeTributavel();
            BigDecimal qt=item.quantidade().multiply(fator).setScale(6,RoundingMode.UNNECESSARY);
            var prod=new LinkedHashMap<String,Object>();prod.put("uCom",item.unidade());prod.put("qCom",item.quantidade().toPlainString());prod.put("vUnCom",item.valorUnitario().toPlainString());prod.put("vProd",item.valorTotal().toPlainString());prod.put("uTrib",unidade);prod.put("qTrib",qt.toPlainString());prod.put("vUnTrib",item.valorTotal().divide(qt,10,RoundingMode.HALF_UP).toPlainString());
            String cst=t.icmsCst();String grupo=cst.length()==3?"ICMSSN"+(Set.of("102","103","300","400").contains(cst)?"102":Set.of("202","203").contains(cst)?"202":cst):"ICMS"+(Set.of("40","41","50").contains(cst)?"40":cst);
            var icms=new LinkedHashMap<String,Object>();icms.put("orig",t.origem());icms.put(cst.length()==3?"CSOSN":"CST",cst);
            x.put("DOCUMENT_PRODUCT_CUSTOM_CONFIGURATION",base64(mapper.writeValueAsString(Map.of("det",Map.of("prod",prod,"imposto",Map.of("ICMS",Map.of(grupo,icms)))))));
            produtos.put(Integer.toString(++index),x);
        }
        body.put("DOCUMENT_PRODUCT",produtos);return mapper.writeValueAsString(body);
    }
    private static boolean vazio(String x) {return x==null||x.isBlank();}
    private static void dados(ComplementoFiscal c,String cidade,String uf,String nome,List<String> erros) {
        if(c==null || vazio(c.getRazaoSocial()) || vazio(c.getLogradouro()) || vazio(c.getNumeroEndereco()) || vazio(c.getBairro()) || vazio(c.getCep()) || vazio(cidade) || vazio(uf) || vazio(c.getCodigoMunicipio())) erros.add(nome+": complete razão social, endereço, CEP e código IBGE no cadastro fiscal.");
    }
    private static void put(Map<String,Object> m,String k,Object v) {if(v!=null&&!v.toString().isBlank())m.put(k,v instanceof BigDecimal d?d.toPlainString():v);}
    private static String base64(String x) {return Base64.getEncoder().encodeToString(x.getBytes(StandardCharsets.UTF_8));}
}
