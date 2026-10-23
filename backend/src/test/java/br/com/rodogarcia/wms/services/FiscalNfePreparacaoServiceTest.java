package br.com.rodogarcia.wms.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import br.com.rodogarcia.wms.config.NotazzProperties;
import br.com.rodogarcia.wms.dto.FiscalCadastroDto;
import br.com.rodogarcia.wms.dto.FiscalNfeDto;
import br.com.rodogarcia.wms.models.Armazem;
import br.com.rodogarcia.wms.models.Cliente;
import br.com.rodogarcia.wms.models.ComplementoFiscal;
import br.com.rodogarcia.wms.models.ConteudoUnidade;
import br.com.rodogarcia.wms.models.EntradaConferida;
import br.com.rodogarcia.wms.models.ItemChegada;
import br.com.rodogarcia.wms.models.ItemNotaEntrada;
import br.com.rodogarcia.wms.models.NotaEntrada;
import br.com.rodogarcia.wms.models.PedidoEntrada;
import br.com.rodogarcia.wms.models.PedidoSaida;
import br.com.rodogarcia.wms.models.Produto;
import br.com.rodogarcia.wms.models.ReferenciaFiscalProduto;
import br.com.rodogarcia.wms.models.ReservaSaida;
import br.com.rodogarcia.wms.models.SituacaoCadastro;
import br.com.rodogarcia.wms.models.SituacaoPedidoSaida;
import br.com.rodogarcia.wms.models.SituacaoReservaSaida;
import br.com.rodogarcia.wms.models.UnidadeLogistica;
import br.com.rodogarcia.wms.repositories.BaixaSaidaRepository;
import br.com.rodogarcia.wms.repositories.CoberturaDocumentoSaidaRepository;
import br.com.rodogarcia.wms.repositories.ConteudoUnidadeRepository;
import br.com.rodogarcia.wms.repositories.DocumentoSaidaRepository;
import br.com.rodogarcia.wms.repositories.FiscalNfeRepository;
import br.com.rodogarcia.wms.repositories.PedidoSaidaRepository;
import br.com.rodogarcia.wms.repositories.ReferenciaFiscalProdutoRepository;
import br.com.rodogarcia.wms.repositories.ReservaSaidaRepository;
import java.math.BigDecimal;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import tools.jackson.databind.json.JsonMapper;

class FiscalNfePreparacaoServiceTest {
    final PedidoSaidaRepository pedidos=mock(PedidoSaidaRepository.class);
    final ReservaSaidaRepository reservas=mock(ReservaSaidaRepository.class);
    final ConteudoUnidadeRepository conteudos=mock(ConteudoUnidadeRepository.class);
    final ReferenciaFiscalProdutoRepository refs=mock(ReferenciaFiscalProdutoRepository.class);
    final BaixaSaidaRepository baixas=mock(BaixaSaidaRepository.class);
    final DocumentoSaidaRepository docs=mock(DocumentoSaidaRepository.class);
    final CoberturaDocumentoSaidaRepository coberturas=mock(CoberturaDocumentoSaidaRepository.class);
    final FiscalNfeRepository fiscal=mock(FiscalNfeRepository.class);
    final ExpedicaoService expedicao=mock(ExpedicaoService.class);
    final EstoqueService estoque=mock(EstoqueService.class);
    final AcessoService acesso=mock(AcessoService.class);
    final NotazzProperties properties=new NotazzProperties();
    final JsonMapper mapper=new JsonMapper();
    final PedidoSaida pedido=mock(PedidoSaida.class);
    final Cliente cliente=mock(Cliente.class);
    final Armazem armazem=mock(Armazem.class);
    final Produto produto=mock(Produto.class);
    final ReferenciaFiscalProduto ref=mock(ReferenciaFiscalProduto.class);
    final ComplementoFiscal dados=new ComplementoFiscal("Empresa de teste","123456","Rua Teste","1",null,"Centro","01000000","BR",null,null,null);
    record Origem(ReservaSaida reserva,ItemNotaEntrada item) {}
    final Origem primeira;
    FiscalNfePreparacaoServiceTest() {
        dados.configurarNfe("3550308",1,"CONSOLIDAR");
        when(pedidos.findById(61L)).thenReturn(Optional.of(pedido));when(pedido.getId()).thenReturn(61L);when(pedido.getVersao()).thenReturn(4L);when(pedido.getReferencia()).thenReturn("SAI-TESTE");when(pedido.getSituacao()).thenReturn(SituacaoPedidoSaida.SEPARADO);when(pedido.getCliente()).thenReturn(cliente);when(pedido.getArmazem()).thenReturn(armazem);
        when(cliente.getId()).thenReturn(83L);when(cliente.getSituacao()).thenReturn(SituacaoCadastro.ATIVO);when(cliente.getDocumentoFiscal()).thenReturn(FiscalNfeTestFixture.CLIENTE);when(cliente.getComplementoFiscal()).thenReturn(dados);when(cliente.getCidade()).thenReturn("São Paulo");when(cliente.getUf()).thenReturn("SP");
        when(armazem.getId()).thenReturn(46L);when(armazem.getSituacao()).thenReturn(SituacaoCadastro.ATIVO);when(armazem.getDocumentoFiscal()).thenReturn(FiscalNfeTestFixture.CNPJ);when(armazem.getComplementoFiscal()).thenReturn(dados);when(armazem.getCidade()).thenReturn("São Paulo");when(armazem.getUf()).thenReturn("SP");
        when(produto.getId()).thenReturn(11L);when(produto.getSku()).thenReturn("SKU1");when(produto.getDescricao()).thenReturn("Produto de teste");when(produto.getUnidadeMedida()).thenReturn("PC");when(produto.getSituacao()).thenReturn(SituacaoCadastro.ATIVO);
        when(ref.getNcm()).thenReturn("39269090");when(ref.getCfop()).thenReturn("5906");when(ref.getConferidaPor()).thenReturn("Fiscal de teste");when(ref.getConferidaEm()).thenReturn(FiscalNfeTestFixture.AGORA);when(ref.getTributacaoNfe()).thenReturn(mapper.writeValueAsString(new FiscalCadastroDto.Tributacao("0","41",null,"99","99",BigDecimal.ZERO,BigDecimal.ZERO,null,null,null,null,null,null,null,null,null)));
        when(refs.findByProdutoIdAndArmazemIdAndOperacao(anyLong(),anyLong(),any())).thenReturn(Optional.of(ref));
        when(fiscal.quantidadeComprometida(anyLong())).thenReturn(BigDecimal.ZERO);when(coberturas.buscarDoPedido(61L)).thenReturn(List.of());
        when(estoque.elegiveisSaida(any(),anyLong(),any(),anyBoolean())).thenAnswer(a->Set.copyOf(a.<List<Long>>getArgument(0)));
        properties.setEnabled(true);var e=new NotazzProperties.Emitente();e.setCnpj(FiscalNfeTestFixture.CNPJ);e.setApiKey("ficticio");e.setAmbiente(2);e.setFinalidade("4");e.setNaturezaOperacao("Retorno de depósito");e.setConfiguracaoConferida(true);properties.getEmitentes().put(46L,e);
        primeira=origem(7L,10L,"2","10.00",FiscalNfeTestFixture.ORIGEM);when(reservas.buscarDoPedido(61L)).thenReturn(List.of(primeira.reserva()));
    }
    Origem origem(long id,long notaId,String q,String valor,String chave) {
        var r=mock(ReservaSaida.class);var u=mock(UnidadeLogistica.class);var entrada=mock(EntradaConferida.class);var chegada=mock(ItemChegada.class);var item=mock(ItemNotaEntrada.class);var nota=mock(NotaEntrada.class);var pe=mock(PedidoEntrada.class);
        when(pe.getArmazem()).thenReturn(armazem);when(pe.getCliente()).thenReturn(cliente);when(u.getId()).thenReturn(id+100);when(u.getPedido()).thenReturn(pe);when(u.getProduto()).thenReturn(produto);when(u.getRevisaoConteudo()).thenReturn(3L);
        when(r.getId()).thenReturn(id);when(r.getUnidade()).thenReturn(u);when(r.getQuantidade()).thenReturn(new BigDecimal(q));when(r.getSituacao()).thenReturn(SituacaoReservaSaida.ATIVA);
        when(entrada.getId()).thenReturn(id+200);when(entrada.getItemChegada()).thenReturn(chegada);when(chegada.getItemNota()).thenReturn(item);when(item.getId()).thenReturn(id+300);when(item.getNota()).thenReturn(nota);when(item.getQuantidadePrevista()).thenReturn(new BigDecimal(q));when(item.getValorMercadoria()).thenReturn(new BigDecimal(valor));when(nota.getId()).thenReturn(notaId);when(nota.getNumero()).thenReturn(notaId);when(nota.getChaveAcesso()).thenReturn(chave);
        when(conteudos.buscarOrigens(id+100)).thenReturn(List.of(new ConteudoUnidade(u,entrada,new BigDecimal(q))));return new Origem(r,item);
    }
    FiscalNfePreparacaoService service(){return new FiscalNfePreparacaoService(pedidos,reservas,conteudos,refs,baixas,docs,coberturas,fiscal,expedicao,estoque,acesso,properties,mapper);}
    FiscalNfePreparacaoService.Preparacao preparar(){return service().preparar(61L,new FiscalNfeDto.Preparar(4L,"RETORNO_MERCADORIA",null));}
    @Test void previaConservaOrigemValorDestinoEObservacaoSemExporCredencial() {
        var p=preparar();assertThat(p.previa().pendencias()).isEmpty();assertThat(p.previa().podeEmitir()).isTrue();
        var doc=p.previa().documentos().getFirst();assertThat(doc.valorTotal()).isEqualByComparingTo("10.00");assertThat(doc.destinatarioDocumento()).isEqualTo(FiscalNfeTestFixture.CLIENTE);assertThat(doc.emitenteCnpj()).isEqualTo(FiscalNfeTestFixture.CNPJ);assertThat(doc.observacao()).contains("SAI-TESTE");
        assertThat(mapper.writeValueAsString(p.previa())).doesNotContain("API_KEY","ficticio");var body=mapper.readTree(p.corpos().getFirst());assertThat(body.path("TRANSMITIR").asString()).isEqualTo("1");
        var custom=mapper.readTree(Base64.getDecoder().decode(body.path("DOCUMENT_CUSTOM_CONFIGURATION").asString()));assertThat(custom.path("ide").path("NFref").get(0).path("refNFe").asString()).isEqualTo(FiscalNfeTestFixture.ORIGEM);
    }
    @Test void mesmoSkuComPrecosDiferentesNaoUsaMedia() {
        var segunda=origem(8L,20L,"2","20.00",FiscalNfeTestFixture.CHAVE);when(reservas.buscarDoPedido(61L)).thenReturn(List.of(primeira.reserva(),segunda.reserva()));
        var d=preparar().previa().documentos().getFirst();assertThat(d.itens()).hasSize(2);assertThat(d.itens().get(0).valorUnitario()).isEqualByComparingTo("5");assertThat(d.itens().get(1).valorUnitario()).isEqualByComparingTo("10");assertThat(d.valorTotal()).isEqualByComparingTo("30");
        dados.configurarNfe("3550308",1,"POR_NOTA");assertThat(preparar().previa().documentos()).hasSize(2);
    }
    @Test void dadosAusentesBloqueiamEIdentificamOCadastro() {
        when(primeira.item().getValorMercadoria()).thenReturn(null);var p=preparar().previa();assertThat(p.podeEmitir()).isFalse();assertThat(p.pendencias()).anyMatch(m->m.contains("valor fiscal da entrada"));
    }
    @Test void naoTrocaEmitentePeloContextoSelecionado() {
        properties.getEmitentes().get(46L).setCnpj("33333333000191");assertThat(preparar().previa().pendencias()).anyMatch(m->m.contains("CNPJ deste armazém"));
    }
    @Test void mudancaFiscalAlteraHashEInvalidaConferenciaAnterior() {
        String hash=preparar().previa().hash();when(ref.getCfop()).thenReturn("6906");assertThat(preparar().previa().hash()).isNotEqualTo(hash);
    }
    @Test void bloqueioFisicoOuVersaoAntigaNaoLiberaEmissao() {
        when(estoque.elegiveisSaida(any(),anyLong(),any(),anyBoolean())).thenReturn(Set.of());assertThat(preparar().previa().podeEmitir()).isFalse();
        assertThat(service().preparar(61L,new FiscalNfeDto.Preparar(3L,"RETORNO_MERCADORIA",null)).previa().pendencias()).anyMatch(m->m.contains("pedido mudou"));
    }
    @Test void permissaoEhVerificadaAntesDaLeituraDosDadosFiscais() {
        doThrow(new AccessDeniedException("negado")).when(acesso).exigirSupervisor();assertThatThrownBy(this::preparar).isInstanceOf(AccessDeniedException.class);verifyNoInteractions(pedidos);
    }
}
