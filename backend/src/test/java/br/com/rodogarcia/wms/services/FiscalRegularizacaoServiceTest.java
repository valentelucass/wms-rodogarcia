package br.com.rodogarcia.wms.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import br.com.rodogarcia.wms.dto.ExpedicaoDto;
import br.com.rodogarcia.wms.models.CondicaoMercadoria;
import br.com.rodogarcia.wms.models.ConteudoUnidade;
import br.com.rodogarcia.wms.models.DocumentoSaida;
import br.com.rodogarcia.wms.models.EntradaConferida;
import br.com.rodogarcia.wms.models.PedidoSaida;
import br.com.rodogarcia.wms.models.ReservaSaida;
import br.com.rodogarcia.wms.models.SolicitacaoNfe;
import br.com.rodogarcia.wms.models.UnidadeLogistica;
import br.com.rodogarcia.wms.repositories.BaixaSaidaRepository;
import br.com.rodogarcia.wms.repositories.ChegadaRecebimentoRepository;
import br.com.rodogarcia.wms.repositories.ConteudoUnidadeRepository;
import br.com.rodogarcia.wms.repositories.DevolucaoSaidaRepository;
import br.com.rodogarcia.wms.repositories.DocumentoSaidaRepository;
import br.com.rodogarcia.wms.repositories.EntradaConferidaRepository;
import br.com.rodogarcia.wms.repositories.FiscalNfeRepository;
import br.com.rodogarcia.wms.repositories.ItemChegadaRepository;
import br.com.rodogarcia.wms.repositories.ItemNotaEntradaRepository;
import br.com.rodogarcia.wms.repositories.NotaEntradaRepository;
import br.com.rodogarcia.wms.repositories.PedidoEntradaRepository;
import br.com.rodogarcia.wms.repositories.ReservaSaidaRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

class FiscalRegularizacaoServiceTest {
    final FiscalNfeRepository repository = mock(FiscalNfeRepository.class);
    final DocumentoSaidaRepository documentos = mock(DocumentoSaidaRepository.class);
    final BaixaSaidaRepository baixas = mock(BaixaSaidaRepository.class);
    final DevolucaoSaidaRepository devolucoes = mock(DevolucaoSaidaRepository.class);
    final AcessoService acesso = mock(AcessoService.class);
    final RegistroRegularizacaoInternaService regularizacaoInterna =
            mock(RegistroRegularizacaoInternaService.class);
    final PedidoSaida pedido = mock(PedidoSaida.class, RETURNS_DEEP_STUBS);
    final DocumentoSaida documentoAtivo = documento();
    final FiscalRegularizacaoService service =
            new FiscalRegularizacaoService(
                    repository,
                    documentos,
                    baixas,
                    devolucoes,
                    new JsonMapper(),
                    acesso,
                    Clock.fixed(FiscalNfeTestFixture.AGORA, ZoneOffset.UTC),
                    regularizacaoInterna);

    FiscalRegularizacaoServiceTest() {
        when(repository.disponivel()).thenReturn(true);
        when(pedido.getId()).thenReturn(61L);
        when(pedido.getCliente().getId()).thenReturn(83L);
        when(pedido.getArmazem().getId()).thenReturn(46L);
        when(acesso.usuario()).thenReturn("usuario");
    }

    SolicitacaoNfe entrada(long pedidoId, String estado) {
        var s = FiscalNfeTestFixture.solicitacao(estado);
        var json = (ObjectNode) new JsonMapper().readTree(s.documentoJson());
        json.put("natureza", "REGULARIZACAO_ENTRADA");
        json.put("documentoOrigemId", 90L);
        return new SolicitacaoNfe(
                s.id(),
                s.planoId(),
                s.operacaoId(),
                pedidoId,
                s.grupo(),
                "REGULARIZACAO_ENTRADA",
                s.versaoPedido(),
                s.hashConfirmacao(),
                s.corpoJson(),
                new JsonMapper().writeValueAsString(json),
                s.emitenteCnpj(),
                s.ambiente(),
                s.situacao(),
                s.notazzId(),
                null,
                90L,
                "123",
                "1",
                FiscalNfeTestFixture.CHAVE,
                "123456789012345",
                s.xml() == null ? null : s.xml().replace("<tpNF>1</tpNF>", "<tpNF>0</tpNF>"),
                s.mensagem(),
                s.usuario(),
                s.criadaEm(),
                s.atualizadaEm());
    }

    DocumentoSaida documento() {
        var d = mock(DocumentoSaida.class);
        when(d.getId()).thenReturn(90L);
        when(d.getSituacao()).thenReturn("AUTORIZADO");
        return d;
    }

    @Test
    void documentoAtivoExigeCancelamentoOuNovaEntradaAntesDoReenderecamento() {
        when(documentos.findByPedidoIdOrderById(61L)).thenReturn(List.of(documentoAtivo));
        assertThatThrownBy(() -> service.conferirRetorno(pedido, List.of()))
                .hasMessageContaining("Cancele");
        verify(repository, never()).concluirRegularizacao(any(), any(), any(), any());
    }

    @Test
    void entradaAutorizadaVinculadaPermiteReenderecarSemCancelarSaida() {
        var d = documento();
        when(documentos.findByPedidoIdOrderById(61L)).thenReturn(List.of(d));
        when(repository.consultar(FiscalNfeTestFixture.ID, true))
                .thenReturn(entrada(61L, "AUTORIZADA"));
        when(regularizacaoInterna.registrar(eq(pedido), any(), any(), any())).thenReturn(301L);
        service.conferirRetorno(pedido, List.of(FiscalNfeTestFixture.ID));
        service.concluir(pedido, List.of(FiscalNfeTestFixture.ID), UUID.randomUUID(), null);
        verify(repository)
                .concluirRegularizacao(eq(FiscalNfeTestFixture.ID), any(), eq(301L), any());
        verify(d, never()).cancelar(any());
        verifyNoInteractions(baixas, devolucoes);
    }

    @Test
    void entradaDeOutroPedidoNaoRegularizaEsteEstoque() {
        when(documentos.findByPedidoIdOrderById(61L)).thenReturn(List.of(documentoAtivo));
        when(repository.consultar(FiscalNfeTestFixture.ID, true))
                .thenReturn(entrada(62L, "AUTORIZADA"));
        assertThatThrownBy(() -> service.conferirRetorno(pedido, List.of(FiscalNfeTestFixture.ID)))
                .hasMessageContaining("pertencer ao pedido");
    }

    @Test
    void notaAindaProcessandoNaoRegulariza() {
        when(documentos.findByPedidoIdOrderById(61L)).thenReturn(List.of(documentoAtivo));
        when(repository.consultar(FiscalNfeTestFixture.ID, true))
                .thenReturn(entrada(61L, "PROCESSANDO"));
        assertThatThrownBy(() -> service.conferirRetorno(pedido, List.of(FiscalNfeTestFixture.ID)))
                .hasMessageContaining("autorizada");
    }

    @Test
    void entradaJaUtilizadaNaoPodeSerReaproveitada() {
        when(documentos.findByPedidoIdOrderById(61L)).thenReturn(List.of(documentoAtivo));
        when(repository.consultar(FiscalNfeTestFixture.ID, true))
                .thenReturn(entrada(61L, "AUTORIZADA"));
        when(repository.regularizada(FiscalNfeTestFixture.ID)).thenReturn(true);
        assertThatThrownBy(() -> service.conferirRetorno(pedido, List.of(FiscalNfeTestFixture.ID)))
                .hasMessageContaining("processamento físico");
        assertThatThrownBy(
                        () ->
                                service.concluir(
                                        pedido,
                                        List.of(FiscalNfeTestFixture.ID),
                                        UUID.randomUUID(),
                                        null))
                .hasMessageContaining("processamento físico");
        verifyNoInteractions(regularizacaoInterna);
    }

    @Test
    void emissaoSemResultadoBloqueiaRetirada() {
        when(repository.doPedido(61L))
                .thenReturn(List.of(FiscalNfeTestFixture.solicitacao("DESCONHECIDO")));
        assertThatThrownBy(() -> service.conferirRetirada(61L)).hasMessageContaining("Resolva");
    }

    @Test
    void resultadoFiscalPendenteImpedeReenderecarMesmoSemDocumentoLocal() {
        for (String estado : List.of("PREPARADA", "ENVIANDO", "DESCONHECIDO", "CANCELANDO")) {
            when(repository.doPedido(61L))
                    .thenReturn(List.of(FiscalNfeTestFixture.solicitacao(estado)));
            assertThatThrownBy(() -> service.conferirRetorno(pedido, List.of()))
                    .hasMessageContaining("Resolva o resultado");
        }
        verifyNoInteractions(documentos, baixas, devolucoes);
        verify(repository, never()).concluirRegularizacao(any(), any(), any(), any());
    }

    @Test
    void entradaAutorizadaExigeProcessamentoFisicoAntesDeOutraRetirada() {
        when(repository.doPedido(61L)).thenReturn(List.of(entrada(61L, "AUTORIZADA")));
        assertThatThrownBy(() -> service.conferirRetirada(61L))
                .hasMessageContaining("regularização");
    }

    @Test
    void quantidadeRetornadaDeveConferirComEntradaFiscal() {
        when(repository.consultar(FiscalNfeTestFixture.ID, true))
                .thenReturn(entrada(61L, "AUTORIZADA"));
        var nota =
                new ExpedicaoDto.Nota(
                        FiscalNfeTestFixture.CNPJ,
                        "1",
                        "123",
                        java.time.LocalDate.of(2026, 10, 23),
                        FiscalNfeTestFixture.CHAVE);
        var c =
                new ExpedicaoDto.Devolver(
                        UUID.randomUUID(),
                        4L,
                        null,
                        nota,
                        FiscalNfeTestFixture.AGORA,
                        List.of(),
                        "Retorno de teste",
                        false,
                        FiscalNfeTestFixture.ID);
        assertThatThrownBy(() -> service.conferirDevolucao(pedido, c))
                .hasMessageContaining("quantidades");
        verifyNoInteractions(devolucoes);
    }

    @Test
    void novaEntradaSemTransitoReatribuiSomenteParcelaPreservandoQuantidadeFifoEValor() {
        var f = new ReentradaFixture(new BigDecimal("10"));
        Long id;
        org.springframework.transaction.support.TransactionSynchronizationManager
                .setActualTransactionActive(true);
        try {
            id =
                    f.service.registrar(
                            pedido,
                            List.of(entrada(61L, "AUTORIZADA")),
                            UUID.randomUUID(),
                            FiscalNfeTestFixture.AGORA);
        } finally {
            org.springframework.transaction.support.TransactionSynchronizationManager.clear();
        }
        assertThat(id).isEqualTo(301L);
        assertThat(f.origem.getQuantidade()).isEqualByComparingTo("8");
        var nova = ArgumentCaptor.forClass(ConteudoUnidade.class);
        verify(f.conteudos).save(nova.capture());
        assertThat(nova.getValue().getUnidade()).isSameAs(f.unidade);
        assertThat(nova.getValue().getQuantidade()).isEqualByComparingTo("2");
        assertThat(nova.getValue().getEntrada().getDataFifo()).isEqualTo(f.fifo);
        assertThat(nova.getValue().getEntrada().getUnitizadaEm())
                .isEqualTo(FiscalNfeTestFixture.AGORA);
        var linha = nova.getValue().getEntrada().getItemChegada().getItemNota();
        assertThat(linha.getQuantidadePrevista()).isEqualByComparingTo("2");
        assertThat(linha.getValorMercadoria()).isEqualByComparingTo("10.00");
        assertThat(linha.getNota().getChaveAcesso()).isEqualTo(FiscalNfeTestFixture.CHAVE);
        verify(f.unidade)
                .alterarQuantidade(eq(new BigDecimal("10")), eq(FiscalNfeTestFixture.AGORA));
        assertThat(f.origem.getQuantidade().add(nova.getValue().getQuantidade()))
                .isEqualByComparingTo("10");
        verify(f.auditoria)
                .registrar(
                        eq("PEDIDO_ENTRADA"),
                        eq(301L),
                        eq("ENTRADA_EFETIVADA"),
                        contains("sem trânsito externo"),
                        isNull(),
                        argThat(
                                value ->
                                        value instanceof Map<?, ?> dados
                                                && "REGULARIZACAO_SEM_TRANSITO"
                                                        .equals(dados.get("tipoEntrada"))
                                                && Boolean.FALSE.equals(
                                                        dados.get("quantidadeFisicaAlterada"))));
    }

    @Test
    void parcelaAusenteDoEstoqueRecusaAntesDeCriarNovoPedido() {
        var f = new ReentradaFixture(BigDecimal.ONE);
        assertThatThrownBy(
                        () ->
                                f.service.registrar(
                                        pedido,
                                        List.of(entrada(61L, "AUTORIZADA")),
                                        UUID.randomUUID(),
                                        FiscalNfeTestFixture.AGORA))
                .hasMessageContaining("composição presente");
        verifyNoInteractions(f.pedidos, f.notas);
        assertThat(f.origem.getQuantidade()).isEqualByComparingTo("1");
        verify(f.unidade, never()).alterarQuantidade(any(), any());
    }

    private class ReentradaFixture {
        final PedidoEntradaRepository pedidos = mock(PedidoEntradaRepository.class);
        final NotaEntradaRepository notas = mock(NotaEntradaRepository.class);
        final ItemNotaEntradaRepository itens = mock(ItemNotaEntradaRepository.class);
        final ChegadaRecebimentoRepository chegadas = mock(ChegadaRecebimentoRepository.class);
        final ItemChegadaRepository itensChegada = mock(ItemChegadaRepository.class);
        final EntradaConferidaRepository entradas = mock(EntradaConferidaRepository.class);
        final ReservaSaidaRepository reservas = mock(ReservaSaidaRepository.class);
        final ConteudoUnidadeRepository conteudos = mock(ConteudoUnidadeRepository.class);
        final AuditoriaService auditoria = mock(AuditoriaService.class);
        final UnidadeLogistica unidade = mock(UnidadeLogistica.class, RETURNS_DEEP_STUBS);
        final Instant fifo = FiscalNfeTestFixture.AGORA.minusSeconds(86400);
        final ConteudoUnidade origem;
        final RegistroRegularizacaoInternaService service;

        ReentradaFixture(BigDecimal quantidade) {
            var original = mock(EntradaConferida.class, RETURNS_DEEP_STUBS);
            when(original.getId()).thenReturn(8L);
            when(original.getItemChegada().getItemNota().getId()).thenReturn(9L);
            when(original.getDataFifo()).thenReturn(fifo);
            when(unidade.getId()).thenReturn(12L);
            when(unidade.getProduto().getId()).thenReturn(11L);
            when(unidade.getQuantidade()).thenReturn(quantidade);
            when(unidade.getCondicao()).thenReturn(CondicaoMercadoria.BOA);
            origem = new ConteudoUnidade(unidade, original, quantidade);
            var reserva = mock(ReservaSaida.class);
            when(reserva.getId()).thenReturn(7L);
            when(reserva.getUnidade()).thenReturn(unidade);
            when(reservas.buscarDoPedido(61L)).thenReturn(List.of(reserva));
            when(conteudos.buscarOrigens(12L)).thenReturn(List.of(origem));
            when(pedidos.save(any()))
                    .thenAnswer(
                            i -> {
                                var p = i.getArgument(0);
                                ReflectionTestUtils.setField(p, "id", 301L);
                                return p;
                            });
            when(notas.save(any())).thenAnswer(i -> i.getArgument(0));
            when(itens.save(any())).thenAnswer(i -> i.getArgument(0));
            when(chegadas.save(any())).thenAnswer(i -> i.getArgument(0));
            when(itensChegada.save(any())).thenAnswer(i -> i.getArgument(0));
            when(entradas.save(any())).thenAnswer(i -> i.getArgument(0));
            service =
                    new RegistroRegularizacaoInternaService(
                            pedidos,
                            notas,
                            itens,
                            chegadas,
                            itensChegada,
                            entradas,
                            reservas,
                            conteudos,
                            new FiscalNfeXml(new NfeXmlService()),
                            new JsonMapper(),
                            acesso,
                            auditoria);
        }
    }
}
