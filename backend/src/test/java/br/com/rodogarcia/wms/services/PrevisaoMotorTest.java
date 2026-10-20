package br.com.rodogarcia.wms.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.rodogarcia.wms.dto.CalculoCobrancaDto;
import br.com.rodogarcia.wms.exceptions.RegraNegocioException;
import br.com.rodogarcia.wms.models.CondicaoMercadoria;
import br.com.rodogarcia.wms.models.ConteudoUnidade;
import br.com.rodogarcia.wms.models.ContratoCobranca;
import br.com.rodogarcia.wms.models.EntradaConferida;
import br.com.rodogarcia.wms.models.FatoServico;
import br.com.rodogarcia.wms.models.ItemChegada;
import br.com.rodogarcia.wms.models.ItemNotaEntrada;
import br.com.rodogarcia.wms.models.ItemTabelaCobranca;
import br.com.rodogarcia.wms.models.NotaEntrada;
import br.com.rodogarcia.wms.models.PedidoEntrada;
import br.com.rodogarcia.wms.models.RateioFatoServico;
import br.com.rodogarcia.wms.models.ServicoCobranca;
import br.com.rodogarcia.wms.models.ServicoMinimoContrato;
import br.com.rodogarcia.wms.models.TabelaCobranca;
import br.com.rodogarcia.wms.models.TipoUnidadeLogistica;
import br.com.rodogarcia.wms.models.UnidadeLogistica;
import br.com.rodogarcia.wms.models.VinculoTabelaCliente;
import br.com.rodogarcia.wms.repositories.AvariaEstoqueRepository;
import br.com.rodogarcia.wms.repositories.BaixaSaidaRepository;
import br.com.rodogarcia.wms.repositories.CalculoCobrancaRepository;
import br.com.rodogarcia.wms.repositories.ConteudoUnidadeRepository;
import br.com.rodogarcia.wms.repositories.ContratoCobrancaRepository;
import br.com.rodogarcia.wms.repositories.EntradaConferidaRepository;
import br.com.rodogarcia.wms.repositories.FatoPermanenciaRepository;
import br.com.rodogarcia.wms.repositories.FatoServicoRepository;
import br.com.rodogarcia.wms.repositories.ItemTabelaCobrancaRepository;
import br.com.rodogarcia.wms.repositories.MarcoFinanceiroAvariaRepository;
import br.com.rodogarcia.wms.repositories.MemoriaDiariaRepository;
import br.com.rodogarcia.wms.repositories.MemoriaServicoRepository;
import br.com.rodogarcia.wms.repositories.MovimentoEstoqueRepository;
import br.com.rodogarcia.wms.repositories.RateioFatoServicoRepository;
import br.com.rodogarcia.wms.repositories.RevisaoContagemRepository;
import br.com.rodogarcia.wms.repositories.ServicoMinimoContratoRepository;
import br.com.rodogarcia.wms.repositories.TabelaCobrancaRepository;
import br.com.rodogarcia.wms.repositories.UnidadeLogisticaRepository;
import br.com.rodogarcia.wms.repositories.VinculoTabelaClienteRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

/** Motor real com repositórios Mockito. Não carrega aplicação, driver ou banco. */
class PrevisaoMotorTest {
    private static final Instant AGORA = Instant.parse("2026-10-20T15:00:00Z");
    private final CalculoCobrancaRepository calculos = mock(CalculoCobrancaRepository.class);
    private final ContratoCobrancaRepository contratos = mock(ContratoCobrancaRepository.class);
    private final ServicoMinimoContratoRepository minimos =
            mock(ServicoMinimoContratoRepository.class);
    private final VinculoTabelaClienteRepository vinculos =
            mock(VinculoTabelaClienteRepository.class);
    private final ItemTabelaCobrancaRepository itens = mock(ItemTabelaCobrancaRepository.class);
    private final FatoServicoRepository fatos = mock(FatoServicoRepository.class);
    private final RateioFatoServicoRepository rateios = mock(RateioFatoServicoRepository.class);
    private final MemoriaDiariaRepository diarias = mock(MemoriaDiariaRepository.class);
    private final MemoriaServicoRepository memorias = mock(MemoriaServicoRepository.class);
    private final OperacaoAdministrativaService operacoes =
            mock(OperacaoAdministrativaService.class);
    private final AuditoriaService auditoria = mock(AuditoriaService.class);
    private final FontesPrevisaoServicoService fontes = mock(FontesPrevisaoServicoService.class);
    private final ContextoCobrancaService contextos = mock(ContextoCobrancaService.class);
    private final UnidadeLogisticaRepository unidades = mock(UnidadeLogisticaRepository.class);
    private final ConteudoUnidadeRepository conteudos = mock(ConteudoUnidadeRepository.class);
    private final BaixaSaidaRepository baixas = mock(BaixaSaidaRepository.class);
    private final FatoPermanenciaRepository permanencias = mock(FatoPermanenciaRepository.class);
    private final AvariaEstoqueRepository avarias = mock(AvariaEstoqueRepository.class);
    private final MarcoFinanceiroAvariaRepository marcos =
            mock(MarcoFinanceiroAvariaRepository.class);
    private final MovimentoEstoqueRepository movimentos = mock(MovimentoEstoqueRepository.class);
    private final LinhaTemporalEstoqueService temporal = mock(LinhaTemporalEstoqueService.class);
    private final RevisaoContagemRepository contagens = mock(RevisaoContagemRepository.class);
    private final EntradaConferidaRepository entradas = mock(EntradaConferidaRepository.class);
    private final ContratoCobranca contrato = mock(ContratoCobranca.class);
    private final ServicoCobranca entrada = mock(ServicoCobranca.class);
    private final TabelaCobranca tabela = mock(TabelaCobranca.class);
    private final CalculoCobrancaService motor =
            new CalculoCobrancaService(
                    calculos,
                    contratos,
                    minimos,
                    vinculos,
                    itens,
                    mock(TabelaCobrancaRepository.class),
                    fatos,
                    rateios,
                    unidades,
                    conteudos,
                    baixas,
                    permanencias,
                    avarias,
                    marcos,
                    movimentos,
                    diarias,
                    memorias,
                    contextos,
                    operacoes,
                    auditoria,
                    temporal,
                    mock(AcessoService.class),
                    JsonMapper.builder().findAndAddModules().build(),
                    Clock.fixed(AGORA, ZoneOffset.UTC),
                    contagens,
                    entradas,
                    mock(ResolucaoFinanceiraCadastroService.class),
                    fontes);

    private FontesPrevisaoServicoService.Evento configurar() {
        when(contrato.getId()).thenReturn(1L);
        when(contrato.getVigenciaInicio()).thenReturn(LocalDate.parse("2026-01-01"));
        when(contrato.getFuso()).thenReturn("America/Sao_Paulo");
        when(contrato.getModalidadeCiclo()).thenReturn("MES_CALENDARIO");
        when(contrato.getDiaCorte()).thenReturn(1);
        when(contrato.getMinimoModo()).thenReturn("NAO_APLICAVEL");
        when(contrato.getGrisModo()).thenReturn("NAO_APLICAVEL");
        when(contratos.findByClienteIdAndArmazemIdOrderByVigenciaInicioAsc(1L, 2L))
                .thenReturn(List.of(contrato));
        when(entrada.getId()).thenReturn(10L);
        when(entrada.getTipo()).thenReturn("ENTRADA");
        when(entrada.getUnidade()).thenReturn("QUANTIDADE_PRODUTO");
        when(tabela.getId()).thenReturn(20L);
        when(tabela.getVigenciaInicio()).thenReturn(LocalDate.parse("2026-01-01"));
        var vinculo = mock(VinculoTabelaCliente.class);
        when(vinculo.getVigenciaInicio()).thenReturn(LocalDate.parse("2026-01-01"));
        when(vinculo.getTabela()).thenReturn(tabela);
        when(vinculos.findByClienteIdAndArmazemIdOrderByVigenciaInicioAsc(1L, 2L))
                .thenReturn(List.of(vinculo));
        var item = new ItemTabelaCobranca(tabela, entrada, "", new BigDecimal("15"), null);
        when(itens.findByTabelaIdOrderByIdAsc(20L)).thenReturn(List.of(item));
        var unidade = mock(UnidadeLogistica.class);
        when(unidade.getId()).thenReturn(30L);
        when(unidade.getTipo()).thenReturn(TipoUnidadeLogistica.BOBINA);
        var evento =
                new FontesPrevisaoServicoService.Evento(
                        "ENTRADA", AGORA.minusSeconds(60), unidade, null, List.of());
        when(fontes.eventos(eq(1L), eq(2L), any(), any(), any(), eq(AGORA), anyList()))
                .thenReturn(List.of(evento));
        when(fontes.comprovar(1L, 2L, evento, entrada, null))
                .thenReturn(
                        new FatoServicoService.Execucao(
                                "ENTRADA:UNIDADE:30",
                                evento.instante(),
                                new BigDecimal("3"),
                                unidade,
                                null,
                                null,
                                null,
                                Map.of(50L, new BigDecimal("3"))));
        return evento;
    }

    private CalculoCobrancaService.Previa prever() {
        return motor.prever(
                1L, 2L, LocalDate.parse("2026-10-20"), LocalDate.parse("2026-10-21"), AGORA);
    }

    @Test
    void operacaoComprovadaAindaSemFatoEntraUmaVezSemNenhumaEscrita() {
        configurar();
        var r = prever();
        assertThat(r.total()).isEqualByComparingTo("45.00");
        assertThat(r.pendencias()).isEmpty();
        verify(contextos).autorizar(1L, 2L);
        verify(calculos, never()).saveAndFlush(any());
        verify(fatos, never()).save(any());
        verify(diarias, never()).save(any());
        verify(memorias, never()).save(any());
        org.mockito.Mockito.verifyNoInteractions(auditoria);
        verify(operacoes, never()).salvar(any(), any(), any(), any(), any(), any(), any());
        verify(contextos, never()).bloquear(any(), any());
    }

    private void existente(String situacao) {
        var f = mock(FatoServico.class);
        when(f.getId()).thenReturn(40L);
        when(f.getServico()).thenReturn(entrada);
        when(f.getChaveFato()).thenReturn("ENTRADA:UNIDADE:30");
        when(f.getExecutadoEm()).thenReturn(AGORA.minusSeconds(60));
        when(f.getSituacao()).thenReturn(situacao);
        when(f.getQuantidade()).thenReturn(new BigDecimal("3"));
        when(f.getCategoria()).thenReturn("");
        when(fatos.paraPrevisao(1L, 2L, AGORA)).thenReturn(List.of(f));
        var nota = mock(NotaEntrada.class);
        when(nota.getId()).thenReturn(50L);
        when(rateios.findByFatoIdOrderByNotaIdAsc(40L))
                .thenReturn(List.of(new RateioFatoServico(f, nota, new BigDecimal("3"))));
    }

    @Test
    void fatoConfirmadoPrevaleceSobreProjecaoDaMesmaChave() {
        var evento = configurar();
        existente("CONFIRMADO");
        assertThat(prever().total()).isEqualByComparingTo("45.00");
        verify(fontes, never()).comprovar(1L, 2L, evento, entrada, null);
    }

    @Test
    void fatoAnuladoNaoERecriadoPelaProjecaoDaOperacao() {
        var evento = configurar();
        existente("ANULADO");
        assertThat(prever().total()).isEqualByComparingTo("0.00");
        verify(fontes, never()).comprovar(1L, 2L, evento, entrada, null);
    }

    @Test
    void origemInsuficienteGeraPendenciaSemFatoNemZeroCompleto() {
        var evento = configurar();
        when(fontes.comprovar(1L, 2L, evento, entrada, null))
                .thenThrow(
                        RegraNegocioException.conflito(
                                "RATEIO_INSUFICIENTE", "Origem insuficiente"));
        var r = prever();
        assertThat(r.total()).isNull();
        assertThat(r.subtotalConhecido()).isEqualByComparingTo("0.00");
        assertThat(r.pendencias()).extracting(p -> p.codigo()).contains("RATEIO_INSUFICIENTE");
    }

    @Test
    void tabelaAusenteNaoFabricaPrecoZero() {
        configurar();
        when(contrato.getMinimoModo()).thenReturn("APLICAVEL");
        when(contrato.getMinimoValor()).thenReturn(new BigDecimal("100"));
        when(contrato.getMinimoProporcao()).thenReturn("INTEGRAL");
        when(vinculos.findByClienteIdAndArmazemIdOrderByVigenciaInicioAsc(1L, 2L))
                .thenReturn(List.of());
        var r = prever();
        assertThat(r.total()).isNull();
        assertThat(r.minimo()).isNull();
        assertThat(r.pendencias()).extracting(p -> p.codigo()).contains("TABELA_AUSENTE");
    }

    @Test
    void fatosAposReferenciaNaoSaoCobradosEConsultaOriginalDeRegistroNaoEUsada() {
        configurar();
        when(fontes.eventos(eq(1L), eq(2L), any(), any(), any(), eq(AGORA), anyList()))
                .thenReturn(List.of());
        var futuro = mock(FatoServico.class);
        when(futuro.getServico()).thenReturn(entrada);
        when(futuro.getChaveFato()).thenReturn("ENTRADA:UNIDADE:FUTURA");
        when(futuro.getSituacao()).thenReturn("CONFIRMADO");
        when(futuro.getExecutadoEm()).thenReturn(AGORA.plusSeconds(60));
        when(fatos.paraPrevisao(1L, 2L, AGORA)).thenReturn(List.of(futuro));
        assertThat(prever().total()).isEqualByComparingTo("0.00");
        verify(fatos).paraPrevisao(1L, 2L, AGORA);
        verify(fatos, never()).findByClienteIdAndArmazemIdOrderByExecutadoEmAscIdAsc(any(), any());
        verify(rateios, never()).findByFatoIdOrderByNotaIdAsc(any());
    }

    @Test
    void tresDiasReutilizamUmLoteDeHistoricosEOrigensPreservandoDiariaEValor() {
        configurar();
        when(fontes.eventos(eq(1L), eq(2L), any(), any(), any(), eq(AGORA), anyList()))
                .thenReturn(List.of());
        var armazenamento = mock(ServicoCobranca.class);
        when(armazenamento.getId()).thenReturn(11L);
        when(armazenamento.getTipo()).thenReturn("ARMAZENAGEM");
        when(itens.findByTabelaIdOrderByIdAsc(20L))
                .thenReturn(
                        List.of(
                                new ItemTabelaCobranca(
                                        tabela, armazenamento, "", new BigDecimal("5"), null)));
        var unidade = mock(UnidadeLogistica.class);
        var pedido = mock(PedidoEntrada.class);
        when(pedido.getId()).thenReturn(50L);
        when(unidade.getId()).thenReturn(30L);
        when(unidade.getPedido()).thenReturn(pedido);
        when(unidade.getTipo()).thenReturn(TipoUnidadeLogistica.BOBINA);
        when(unidade.getCondicao()).thenReturn(CondicaoMercadoria.BOA);
        when(unidade.isAtiva()).thenReturn(true);
        var inicio = Instant.parse("2026-10-18T03:00:00Z");
        when(unidade.getCriadaEm()).thenReturn(inicio);
        when(unidade.getInicioArmazenagemEm()).thenReturn(inicio);
        when(unidades.paraPrevisao(1L, 2L, inicio, AGORA)).thenReturn(List.of(unidade));
        var nota = mock(NotaEntrada.class);
        when(nota.getId()).thenReturn(60L);
        var item = mock(ItemNotaEntrada.class);
        when(item.getId()).thenReturn(61L);
        when(item.getNota()).thenReturn(nota);
        when(item.getQuantidadePrevista()).thenReturn(new BigDecimal("3"));
        when(item.getValorMercadoria()).thenReturn(new BigDecimal("10000"));
        var chegada = mock(ItemChegada.class);
        when(chegada.getItemNota()).thenReturn(item);
        var entrada = mock(EntradaConferida.class);
        when(entrada.getId()).thenReturn(62L);
        when(entrada.getItemChegada()).thenReturn(chegada);
        when(entrada.getUnitizadaEm()).thenReturn(inicio);
        var origem = mock(ConteudoUnidade.class);
        when(origem.getUnidade()).thenReturn(unidade);
        when(origem.getEntrada()).thenReturn(entrada);
        when(origem.getQuantidade()).thenReturn(new BigDecimal("3"));
        when(conteudos.buscarParaIndicador(List.of(30L))).thenReturn(List.of(origem));
        var base = new LinhaTemporalEstoqueService.Base(new BigDecimal("3"), new BigDecimal("2"));
        when(temporal.noInstante(eq(unidade), any(), anyList(), anyList())).thenReturn(base);
        when(temporal.fisicoNoInstante(eq(unidade), any(), anyList(), anyList())).thenReturn(base);
        var r =
                motor.prever(
                        1L,
                        2L,
                        LocalDate.parse("2026-10-18"),
                        LocalDate.parse("2026-10-21"),
                        AGORA);
        // Dois lugares x três dias x cinco, com a mesma origem conhecida de R$10.000.
        assertThat(r.total()).isEqualByComparingTo("30.00");
        assertThat(r.pendencias()).isEmpty();
        verify(permanencias).buscarParaIndicador(List.of(30L));
        verify(avarias).buscarParaIndicador(List.of(30L));
        verify(movimentos).buscarParaIndicador(List.of(30L));
        verify(conteudos).buscarParaIndicador(List.of(30L));
        verify(contagens).buscarParaIndicador(List.of(30L));
        verify(baixas).buscarParaIndicador(List.of(30L));
        verify(marcos).buscarParaIndicador(List.of(30L));
        verify(entradas).paraPrevisao(1L, 2L, inicio, AGORA);
        verify(permanencias, never()).findByUnidadeIdOrderByOcorridaEmAscIdAsc(any());
        verify(conteudos, never()).buscarOrigens(any());
        verify(entradas, never()).historicoCobranca(any(), any());
        verify(temporal, never()).noInstante(any(), any());
        verify(temporal, never()).fisicoNoInstante(any(), any());
        verify(unidades, never()).historicoCobranca(any(), any());
    }

    @Test
    void inativaAntigaSemFimComprovadoContinuaPendenteEmVezDeDesaparecerDaPrevia() {
        configurar();
        when(fontes.eventos(eq(1L), eq(2L), any(), any(), any(), eq(AGORA), anyList()))
                .thenReturn(List.of());
        var unidade = mock(UnidadeLogistica.class);
        var pedido = mock(PedidoEntrada.class);
        when(pedido.getId()).thenReturn(50L);
        when(unidade.getPedido()).thenReturn(pedido);
        when(unidade.getId()).thenReturn(30L);
        when(unidade.getTipo()).thenReturn(TipoUnidadeLogistica.BOBINA);
        when(unidade.getCriadaEm()).thenReturn(Instant.parse("2026-09-01T03:00:00Z"));
        when(unidade.getInicioArmazenagemEm()).thenReturn(Instant.parse("2026-09-01T03:00:00Z"));
        when(unidades.paraPrevisao(eq(1L), eq(2L), any(), eq(AGORA))).thenReturn(List.of(unidade));
        when(temporal.fisicoNoInstante(eq(unidade), any(), anyList(), anyList()))
                .thenReturn(new LinhaTemporalEstoqueService.Base(BigDecimal.ZERO, BigDecimal.ZERO));
        var r = prever();
        assertThat(r.total()).isNull();
        assertThat(r.pendencias())
                .extracting(p -> p.codigo())
                .contains("FIM_PERMANENCIA_INSUFICIENTE");
    }

    @Test
    void minimoEGrisReutilizamFormulaDoMotorComOraculoIndependente() {
        configurar();
        when(contrato.getMinimoModo()).thenReturn("APLICAVEL");
        when(contrato.getMinimoValor()).thenReturn(new BigDecimal("100"));
        when(contrato.getMinimoProporcao()).thenReturn("INTEGRAL");
        when(contrato.getGrisModo()).thenReturn("APLICAVEL");
        when(contrato.getGrisPeriodicidade()).thenReturn("CICLO");
        when(contrato.getGrisBase()).thenReturn("VALOR_ESTOQUE_PICO");
        when(contrato.getGrisProporcao()).thenReturn("INTEGRAL");
        when(contrato.getGrisPercentual()).thenReturn(new BigDecimal("0.1"));
        var abrangencia = mock(ServicoMinimoContrato.class);
        when(abrangencia.getServico()).thenReturn(entrada);
        when(minimos.findByContratoId(1L)).thenReturn(List.of(abrangencia));
        var dias =
                List.of(
                        new CalculoCobrancaDto.Diaria(
                                LocalDate.parse("2026-10-20"),
                                null,
                                null,
                                BigDecimal.ZERO,
                                BigDecimal.ZERO,
                                new BigDecimal("10000"),
                                null,
                                null,
                                List.of(),
                                List.of(),
                                List.of()));
        var pendencias = new ArrayList<CalculoCobrancaDto.Pendencia>();
        var r =
                motor.ajustes(
                        contrato,
                        new CalculoCobrancaDto.Calcular(
                                null,
                                1L,
                                2L,
                                LocalDate.parse("2026-10-01"),
                                LocalDate.parse("2026-11-01"),
                                "Oraculo ficticio",
                                null),
                        dias,
                        Map.of(10L, new BigDecimal("91")),
                        pendencias);
        assertThat(r.minimoComplemento()).isEqualByComparingTo("9.00");
        assertThat(r.valorGris()).isEqualByComparingTo("10.00");
        assertThat(new BigDecimal("91").add(r.minimoComplemento()).add(r.valorGris()))
                .isEqualByComparingTo("110.00");
        assertThat(pendencias).isEmpty();
    }
}
