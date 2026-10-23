package br.com.rodogarcia.wms.services;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import br.com.rodogarcia.wms.dto.ExpedicaoDto;
import br.com.rodogarcia.wms.models.DocumentoSaida;
import br.com.rodogarcia.wms.models.PedidoSaida;
import br.com.rodogarcia.wms.models.SolicitacaoNfe;
import br.com.rodogarcia.wms.repositories.BaixaSaidaRepository;
import br.com.rodogarcia.wms.repositories.DevolucaoSaidaRepository;
import br.com.rodogarcia.wms.repositories.DocumentoSaidaRepository;
import br.com.rodogarcia.wms.repositories.FiscalNfeRepository;
import java.time.Clock;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class FiscalRegularizacaoServiceTest {
    final FiscalNfeRepository repository = mock(FiscalNfeRepository.class);
    final DocumentoSaidaRepository documentos = mock(DocumentoSaidaRepository.class);
    final BaixaSaidaRepository baixas = mock(BaixaSaidaRepository.class);
    final DevolucaoSaidaRepository devolucoes = mock(DevolucaoSaidaRepository.class);
    final AcessoService acesso = mock(AcessoService.class);
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
                    Clock.fixed(FiscalNfeTestFixture.AGORA, ZoneOffset.UTC));

    FiscalRegularizacaoServiceTest() {
        when(repository.disponivel()).thenReturn(true);
        when(pedido.getId()).thenReturn(61L);
        when(pedido.getCliente().getId()).thenReturn(83L);
        when(pedido.getArmazem().getId()).thenReturn(46L);
        when(acesso.usuario()).thenReturn("usuario");
    }

    SolicitacaoNfe entrada(long pedidoId, String estado) {
        var s = FiscalNfeTestFixture.solicitacao(estado);
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
                s.documentoJson(),
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
                s.xml(),
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
        service.conferirRetorno(pedido, List.of(FiscalNfeTestFixture.ID));
        service.concluir(pedido, List.of(FiscalNfeTestFixture.ID), UUID.randomUUID(), null);
        verify(repository)
                .concluirRegularizacao(eq(FiscalNfeTestFixture.ID), any(), eq(null), any());
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
}
