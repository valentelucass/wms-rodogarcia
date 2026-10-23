package br.com.rodogarcia.wms.services;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import br.com.rodogarcia.wms.config.NotazzProperties;
import br.com.rodogarcia.wms.repositories.FiscalNfeRepository;
import br.com.rodogarcia.wms.repositories.PedidoSaidaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import tools.jackson.databind.json.JsonMapper;

class FiscalNfeServiceTest {
    final FiscalNfeRepository repository = mock(FiscalNfeRepository.class);
    final FiscalNfePreparacaoService preparo = mock(FiscalNfePreparacaoService.class);
    final FiscalNfeTransacaoService tx = mock(FiscalNfeTransacaoService.class);
    final PedidoSaidaRepository pedidos = mock(PedidoSaidaRepository.class);
    final AcessoService acesso = mock(AcessoService.class);
    final NotazzProperties props = new NotazzProperties();
    final NotazzGateway gateway = mock(NotazzGateway.class);
    final JsonMapper mapper = new JsonMapper();

    FiscalNfeServiceTest() {
        when(repository.disponivel()).thenReturn(true);
        when(repository.situacaoFiscal(61L)).thenReturn("PENDENTE");
        var scope = mock(PedidoSaidaRepository.Escopo.class);
        when(scope.getClienteId()).thenReturn(83L);
        when(scope.getArmazemId()).thenReturn(46L);
        when(pedidos.buscarEscopo(61L)).thenReturn(Optional.of(scope));
        props.getEmitentes().put(46L, new NotazzProperties.Emitente());
    }

    FiscalNfeService service() {
        return new FiscalNfeService(
                repository,
                preparo,
                tx,
                pedidos,
                acesso,
                props,
                gateway,
                mapper,
                mock(ApplicationEventPublisher.class),
                mock(ExpedicaoService.class),
                mock(FiscalNfeXml.class));
    }

    void preparar(String estado) {
        var s = FiscalNfeTestFixture.solicitacao(estado);
        when(tx.obter(61L, s.id())).thenReturn(s);
        when(tx.adquirir(eq(61L), eq(s.id()), any(), any(), any())).thenReturn(s);
        when(repository.doPedido(61L)).thenReturn(List.of(s));
        when(gateway.chamar(any(), any(), any()))
                .thenReturn(
                        mapper.readTree(
                                "{\"statusProcessamento\":\"sucesso\",\"codigoProcessamento\":\"000\",\"id\":\"remoto\"}"));
    }

    @Test
    void pedidoNovoUsaCriacaoComMesmaIdentidadePersistida() {
        preparar("PREPARADA");
        service().processar(61L, UUID.fromString(FiscalNfeTestFixture.ID));
        verify(gateway)
                .chamar(
                        any(),
                        eq("create_nfe_55"),
                        org.mockito.ArgumentMatchers.argThat(
                                m ->
                                        FiscalNfeTestFixture.ID.equals(m.get("EXTERNAL_ID"))
                                                && "1".equals(m.get("TRANSMITIR"))));
    }

    @Test
    void envioSemRespostaUsaConsultaNuncaNovaCriacao() {
        preparar("DESCONHECIDO");
        service().processar(61L, UUID.fromString(FiscalNfeTestFixture.ID));
        verify(gateway).chamar(any(), eq("consult_nfe_55"), any());
        verify(gateway, never()).chamar(any(), eq("create_nfe_55"), any());
    }

    @Test
    void leaseOcupadaNaoDisparaChamadaExterna() {
        preparar("PREPARADA");
        when(tx.adquirir(eq(61L), any(), any(), any(), any())).thenReturn(null);
        service().processar(61L, UUID.fromString(FiscalNfeTestFixture.ID));
        verifyNoInteractions(gateway);
    }

    @Test
    void falhaDeRedeMantemSolicitacaoParaConciliacao() {
        preparar("PREPARADA");
        when(gateway.chamar(any(), any(), any()))
                .thenThrow(new NotazzGateway.ResultadoDesconhecido());
        service().processar(61L, UUID.fromString(FiscalNfeTestFixture.ID));
        verify(tx).falhou(eq(61L), eq(FiscalNfeTestFixture.ID), any(), eq(false));
    }

    @Test
    void erroDeConciliacaoNaoEhSucessoFiscal() {
        preparar("PROCESSANDO");
        doThrow(CadastroSupport.invalido("XML divergente"))
                .when(tx)
                .recebido(eq(61L), any(), any(), any(), any());
        service().processar(61L, UUID.fromString(FiscalNfeTestFixture.ID));
        verify(tx).falhou(eq(61L), eq(FiscalNfeTestFixture.ID), any(), eq(true));
    }

    @Test
    void operacaoSemPapelFiscalNaoConsultaNemEnviaPeloProcessador() {
        doThrow(new AccessDeniedException("negado")).when(acesso).exigirSupervisor();
        assertThatThrownBy(() -> service().processar(61L, UUID.fromString(FiscalNfeTestFixture.ID)))
                .isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(repository, gateway, tx);
    }
}
