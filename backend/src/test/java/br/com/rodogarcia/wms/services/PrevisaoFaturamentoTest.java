package br.com.rodogarcia.wms.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import br.com.rodogarcia.wms.dto.CalculoCobrancaDto;
import br.com.rodogarcia.wms.models.Armazem;
import br.com.rodogarcia.wms.models.Cliente;
import br.com.rodogarcia.wms.models.ContratoCobranca;
import br.com.rodogarcia.wms.repositories.ContratoCobrancaRepository;
import br.com.rodogarcia.wms.repositories.VisaoOperacaoRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

/** Mockito e datas fixas somente; nenhum contexto Spring, JDBC ou banco. */
class PrevisaoFaturamentoTest {
    private final ContratoCobrancaRepository contratos = mock(ContratoCobrancaRepository.class);
    private final VisaoOperacaoRepository fontes = mock(VisaoOperacaoRepository.class);
    private final CalculoCobrancaService calculos = mock(CalculoCobrancaService.class);
    private final ContextoCobrancaService contextos = mock(ContextoCobrancaService.class);
    private final AcessoService acesso = mock(AcessoService.class);
    private final PrevisaoFaturamentoService service =
            new PrevisaoFaturamentoService(contratos, fontes, calculos, contextos, acesso);
    private static final ZoneId ZONA = ZoneId.of("America/Sao_Paulo");
    private static final Instant AGORA = Instant.parse("2026-10-20T15:00:00Z");

    private ContratoCobranca contrato(
            long cliente, long armazem, String inicio, String fim, int corte, String moeda) {
        var c = mock(ContratoCobranca.class);
        var cl = mock(Cliente.class);
        var ar = mock(Armazem.class);
        when(cl.getId()).thenReturn(cliente);
        when(ar.getId()).thenReturn(armazem);
        when(c.getId()).thenReturn(cliente * 100 + armazem);
        when(c.getCliente()).thenReturn(cl);
        when(c.getArmazem()).thenReturn(ar);
        when(c.getVigenciaInicio()).thenReturn(LocalDate.parse(inicio));
        when(c.getVigenciaFim()).thenReturn(fim == null ? null : LocalDate.parse(fim));
        when(c.getFuso()).thenReturn(ZONA.getId());
        when(c.getMoeda()).thenReturn(moeda);
        when(c.getModalidadeCiclo()).thenReturn("MES_CALENDARIO");
        when(c.getDiaCorte()).thenReturn(corte);
        return c;
    }

    private void configurar(ContratoCobranca... cs) {
        when(contratos.paraPrevisao(anyList(), anyList())).thenReturn(List.of(cs));
        when(calculos.prever(any(), any(), any(), any(), any()))
                .thenReturn(
                        new CalculoCobrancaService.Previa(
                                new BigDecimal("91"),
                                new BigDecimal("9"),
                                new BigDecimal("10"),
                                new BigDecimal("110"),
                                List.of()));
    }

    private void atividade(long cliente, long armazem, String inicio, String fim) {
        when(fontes.atividadesPrevisao(anyList(), anyList(), any(), any()))
                .thenReturn(
                        List.of(
                                new VisaoOperacaoRepository.AtividadeFinanceira(
                                        new VisaoOperacaoRepository.ContextoFinanceiro(
                                                cliente, armazem),
                                        Instant.parse(inicio),
                                        fim == null ? null : Instant.parse(fim))));
    }

    @Test
    void mesUsaUltimoDiaIncluidoDoCicloEReferenciaDoDiaAtual() {
        configurar(contrato(1, 2, "2026-01-01", null, 1, "BRL"));
        var r = service.consultar(List.of(1L), List.of(2L), AGORA, ZONA);
        assertThat(r.completo()).isTrue();
        assertThat(r.competencia()).isEqualTo("2026-10");
        assertThat(r.referenciaEm()).isEqualTo(AGORA);
        assertThat(r.total()).isEqualByComparingTo("110");
        assertThat(r.conhecido()).isEqualByComparingTo("110");
        assertThat(r.incluiDiaAtual()).isTrue();
        assertThat(r.periodos())
                .singleElement()
                .satisfies(
                        p -> {
                            assertThat(p.cicloInicio()).isEqualTo("2026-10-01");
                            assertThat(p.cicloFimExclusivo()).isEqualTo("2026-11-01");
                            assertThat(p.apuradoFimExclusivo()).isEqualTo("2026-10-21");
                        });
        verify(calculos)
                .prever(
                        1L,
                        2L,
                        LocalDate.parse("2026-10-01"),
                        LocalDate.parse("2026-10-21"),
                        AGORA);
    }

    @Test
    void aposCorteNaoRepeteMinimoDoCicloDaProximaCompetencia() {
        configurar(contrato(1, 2, "2026-01-25", null, 25, "BRL"));
        var referencia = Instant.parse("2026-10-26T15:00:00Z");
        var r = service.consultar(List.of(1L), List.of(2L), referencia, ZONA);
        assertThat(r.periodos()).hasSize(1);
        assertThat(r.incluiDiaAtual()).isFalse();
        verify(calculos)
                .prever(
                        1L,
                        2L,
                        LocalDate.parse("2026-09-25"),
                        LocalDate.parse("2026-10-25"),
                        referencia);
        verify(calculos, never())
                .prever(eq(1L), eq(2L), eq(LocalDate.parse("2026-10-25")), any(), any());
    }

    @Test
    void contratoExpiradoComAtividadeNaoViraZeroCompleto() {
        configurar(contrato(1, 2, "2026-01-01", "2026-10-01", 1, "BRL"));
        atividade(1, 2, "2026-10-01T12:00:00Z", null);
        var r = service.consultar(List.of(1L), List.of(2L), AGORA, ZONA);
        assertThat(r.completo()).isFalse();
        assertThat(r.total()).isNull();
        assertThat(r.pendencias())
                .extracting(p -> p.codigo())
                .contains("CONTRATO_NAO_COBRE_PERIODO");
        verifyNoInteractions(calculos);
    }

    @Test
    void inicioDeContratoNoMeioDoMesNaoOcultaAtividadeAnterior() {
        configurar(contrato(1, 2, "2026-10-16", null, 1, "BRL"));
        atividade(1, 2, "2026-10-01T12:00:00Z", null);
        var r = service.consultar(List.of(1L), List.of(2L), AGORA, ZONA);
        assertThat(r.total()).isNull();
        assertThat(r.conhecido()).isEqualByComparingTo("91");
        assertThat(r.pendencias())
                .anySatisfy(
                        p -> {
                            assertThat(p.codigo()).isEqualTo("CONTRATO_NAO_COBRE_PERIODO");
                            assertThat(p.data()).isEqualTo("2026-10-01");
                        });
        verify(calculos)
                .prever(
                        1L,
                        2L,
                        LocalDate.parse("2026-10-16"),
                        LocalDate.parse("2026-10-21"),
                        AGORA);
    }

    @Test
    void atividadeAposCorteCobertaNaoGeraLacunaNemCicloDuplicado() {
        configurar(contrato(1, 2, "2026-01-25", null, 25, "BRL"));
        atividade(1, 2, "2026-10-26T12:00:00Z", null);
        var r =
                service.consultar(
                        List.of(1L), List.of(2L), Instant.parse("2026-10-27T15:00:00Z"), ZONA);
        assertThat(r.pendencias()).isEmpty();
        assertThat(r.periodos()).hasSize(1);
    }

    @Test
    void ausenciaDeContratoComAtividadeEInformadaSemInventarParametros() {
        configurar();
        atividade(1, 2, "2026-10-12T12:00:00Z", null);
        var r = service.consultar(List.of(1L), List.of(2L), AGORA, ZONA);
        assertThat(r.total()).isNull();
        assertThat(r.pendencias()).extracting(p -> p.codigo()).containsExactly("CONTRATO_AUSENTE");
        verifyNoInteractions(calculos);
    }

    @Test
    void identidadeAntigaZeradaSemTerminalFinanceiroNaoAfirmaFalsaFaltaDeContratoAtual() {
        configurar();
        when(fontes.atividadesPrevisao(anyList(), anyList(), any(), any()))
                .thenReturn(
                        List.of(
                                new VisaoOperacaoRepository.AtividadeFinanceira(
                                        new VisaoOperacaoRepository.ContextoFinanceiro(1L, 2L),
                                        Instant.parse("2026-09-01T12:00:00Z"),
                                        null,
                                        true)));
        var r = service.consultar(List.of(1L), List.of(2L), AGORA, ZONA);
        assertThat(r.total()).isNull();
        assertThat(r.pendencias())
                .extracting(p -> p.codigo())
                .containsExactly("HISTORICO_OPERACIONAL_INSUFICIENTE")
                .doesNotContain("CONTRATO_AUSENTE");
        verifyNoInteractions(calculos);
    }

    @Test
    void moedasDiferentesNaoSaoSomadasNemConvertidas() {
        configurar(
                contrato(1, 2, "2026-01-01", null, 1, "BRL"),
                contrato(3, 4, "2026-01-01", null, 1, "USD"));
        var r = service.consultar(List.of(1L, 3L), List.of(2L, 4L), AGORA, ZONA);
        assertThat(r.total()).isNull();
        assertThat(r.conhecido()).isNull();
        assertThat(r.moeda()).isNull();
        assertThat(r.pendencias()).extracting(p -> p.codigo()).contains("MOEDAS_DIVERGENTES");
        verify(contextos).autorizar(1L, 2L);
        verify(contextos).autorizar(3L, 4L);
        verify(contextos, never()).autorizar(1L, 4L);
    }

    @Test
    void calculoIncompletoNaoSomaComplementoMinimoIncertoAoConhecido() {
        configurar(contrato(1, 2, "2026-01-01", null, 1, "BRL"));
        var p =
                new CalculoCobrancaDto.Pendencia(
                        "TARIFA_SERVICO_AUSENTE", null, null, null, "Falta tarifa");
        when(calculos.prever(any(), any(), any(), any(), any()))
                .thenReturn(
                        new CalculoCobrancaService.Previa(
                                new BigDecimal("40"),
                                new BigDecimal("60"),
                                BigDecimal.ZERO,
                                null,
                                List.of(p)));
        var r = service.consultar(List.of(1L), List.of(2L), AGORA, ZONA);
        assertThat(r.total()).isNull();
        assertThat(r.conhecido()).isEqualByComparingTo("40");
        assertThat(r.completo()).isFalse();
    }

    @Test
    void sobreposicaoNaoEscolheContratoArbitrarioNemDuplicaMinimo() {
        configurar(
                contrato(1, 2, "2026-01-01", null, 1, "BRL"),
                contrato(1, 2, "2026-10-10", null, 1, "BRL"));
        var r = service.consultar(List.of(1L), List.of(2L), AGORA, ZONA);
        assertThat(r.total()).isNull();
        assertThat(r.pendencias()).extracting(p -> p.codigo()).contains("CONTRATO_AMBIGUO");
        verifyNoInteractions(calculos);
    }

    @Test
    void acessoFinanceiroEConferidoAntesDasLeituras() {
        doThrow(new AccessDeniedException("Acesso negado")).when(acesso).exigirSupervisor();
        assertThatThrownBy(() -> service.consultar(List.of(1L), List.of(2L), AGORA, ZONA))
                .isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(contratos, fontes, calculos);
    }
}
