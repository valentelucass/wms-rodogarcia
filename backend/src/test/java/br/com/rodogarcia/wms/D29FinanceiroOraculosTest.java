package br.com.rodogarcia.wms;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import br.com.rodogarcia.wms.dto.CalculoCobrancaDto;
import br.com.rodogarcia.wms.models.Armazem;
import br.com.rodogarcia.wms.models.AvariaEstoque;
import br.com.rodogarcia.wms.models.CalculoCobranca;
import br.com.rodogarcia.wms.models.Cliente;
import br.com.rodogarcia.wms.models.CondicaoMercadoria;
import br.com.rodogarcia.wms.models.ConteudoUnidade;
import br.com.rodogarcia.wms.models.ContratoCobranca;
import br.com.rodogarcia.wms.models.EntradaConferida;
import br.com.rodogarcia.wms.models.FatoServico;
import br.com.rodogarcia.wms.models.ItemTabelaCobranca;
import br.com.rodogarcia.wms.models.NotaEntrada;
import br.com.rodogarcia.wms.models.RateioFatoServico;
import br.com.rodogarcia.wms.models.ServicoCobranca;
import br.com.rodogarcia.wms.models.ServicoMinimoContrato;
import br.com.rodogarcia.wms.models.TabelaCobranca;
import br.com.rodogarcia.wms.models.TipoUnidadeLogistica;
import br.com.rodogarcia.wms.models.UnidadeLogistica;
import br.com.rodogarcia.wms.models.VinculoTabelaCliente;
import br.com.rodogarcia.wms.repositories.AvariaEstoqueRepository;
import br.com.rodogarcia.wms.repositories.CalculoCobrancaRepository;
import br.com.rodogarcia.wms.repositories.ConteudoUnidadeRepository;
import br.com.rodogarcia.wms.repositories.ContratoCobrancaRepository;
import br.com.rodogarcia.wms.repositories.EntradaConferidaRepository;
import br.com.rodogarcia.wms.repositories.FatoPermanenciaRepository;
import br.com.rodogarcia.wms.repositories.FatoServicoRepository;
import br.com.rodogarcia.wms.repositories.ItemTabelaCobrancaRepository;
import br.com.rodogarcia.wms.repositories.OperacaoUnidadeRepository;
import br.com.rodogarcia.wms.repositories.RateioFatoServicoRepository;
import br.com.rodogarcia.wms.repositories.ServicoMinimoContratoRepository;
import br.com.rodogarcia.wms.repositories.UnidadeLogisticaRepository;
import br.com.rodogarcia.wms.repositories.VinculoTabelaClienteRepository;
import br.com.rodogarcia.wms.services.AcessoService;
import br.com.rodogarcia.wms.services.CalculoCobrancaService;
import br.com.rodogarcia.wms.services.ContextoCobrancaService;
import br.com.rodogarcia.wms.services.LinhaTemporalEstoqueService;
import br.com.rodogarcia.wms.services.OperacaoAdministrativaService;
import br.com.rodogarcia.wms.services.PeriodoCobrancaService;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.json.JsonMapper;

/**
 * D29: calculador real, fontes de repositorio isoladas. Nenhum SQL, HTTP, reset ou Clock alterado.
 */
class D29FinanceiroOraculosTest {
    final Map<Class<?>, Object> doubles = new HashMap<>();
    final JsonMapper mapper = JsonMapper.builder().build();
    final LocalDate start = LocalDate.of(2026, 9, 1);
    final Instant synthetic = start.atStartOfDay(ZoneOffset.UTC).toInstant();
    final Cliente client =
            new Cliente("D29LOCAL", "Cliente ficticio local", "11111111000111", synthetic);
    final Armazem warehouse =
            new Armazem(
                    "D29LOCAL", "Armazem ficticio", "22222222000122", "Osasco", "SP", synthetic);
    final CalculoCobrancaService calculator;
    final List<FatoServico> serviceFacts = new ArrayList<>();
    final List<ItemTabelaCobranca> prices = new ArrayList<>();
    final TabelaCobranca table = mock(TabelaCobranca.class);

    D29FinanceiroOraculosTest() throws Exception {
        ReflectionTestUtils.setField(client, "id", 1L);
        ReflectionTestUtils.setField(warehouse, "id", 2L);
        var ctor = CalculoCobrancaService.class.getConstructors()[0];
        var types = ctor.getParameterTypes();
        var args = new Object[types.length];
        for (int i = 0; i < types.length; i++) {
            args[i] =
                    types[i] == JsonMapper.class
                            ? mapper
                            : types[i] == Clock.class
                                    ? Clock.systemUTC()
                                    : types[i] == LinhaTemporalEstoqueService.class
                                            ? new LinhaTemporalEstoqueService(
                                                    d(FatoPermanenciaRepository.class),
                                                    mock(OperacaoUnidadeRepository.class),
                                                    mapper)
                                            : doubles.computeIfAbsent(types[i], t -> mock(t));
        }
        calculator = (CalculoCobrancaService) ctor.newInstance(args);
        when(d(ContextoCobrancaService.class).bloquear(1L, 2L))
                .thenReturn(new ContextoCobrancaService.Contexto(client, warehouse));
        when(d(AcessoService.class).usuario()).thenReturn("D29-local-ficticio");
        when(d(OperacaoAdministrativaService.class).hash(anyString(), nullable(Long.class), any()))
                .thenAnswer(a -> "D29-local-" + Objects.hash(a.getArgument(0), a.getArgument(2)));
        AtomicLong ids = new AtomicLong(10);
        when(d(CalculoCobrancaRepository.class).saveAndFlush(any()))
                .thenAnswer(
                        a -> {
                            CalculoCobranca c = a.getArgument(0);
                            ReflectionTestUtils.setField(c, "id", ids.incrementAndGet());
                            return c;
                        });
        when(d(FatoServicoRepository.class)
                        .findByClienteIdAndArmazemIdOrderByExecutadoEmAscIdAsc(1L, 2L))
                .thenReturn(serviceFacts);
        when(table.getId()).thenReturn(4L);
        when(table.getVigenciaInicio()).thenReturn(start);
        var link = mock(VinculoTabelaCliente.class);
        when(link.getTabela()).thenReturn(table);
        when(link.getVigenciaInicio()).thenReturn(start);
        when(d(VinculoTabelaClienteRepository.class)
                        .findByClienteIdAndArmazemIdOrderByVigenciaInicioAsc(1L, 2L))
                .thenReturn(List.of(link));
        when(d(ItemTabelaCobrancaRepository.class).findByTabelaIdOrderByIdAsc(4L))
                .thenReturn(prices);
    }

    <T> T d(Class<T> type) {
        return type.cast(doubles.get(type));
    }

    BigDecimal n(String value) {
        return new BigDecimal(value);
    }

    ContratoCobranca contract(String minimum, String mode, String proportion) {
        var c =
                new ContratoCobranca(
                        client,
                        warehouse,
                        start,
                        null,
                        "UTC",
                        "BRL",
                        "DIAS_CORRIDOS",
                        null,
                        30,
                        mode,
                        "NAO_APLICAVEL",
                        minimum == null ? null : n(minimum),
                        proportion,
                        null,
                        null,
                        null,
                        null,
                        synthetic,
                        synthetic);
        ReflectionTestUtils.setField(c, "id", 3L);
        when(d(ContratoCobrancaRepository.class)
                        .findByClienteIdAndArmazemIdOrderByVigenciaInicioAsc(1L, 2L))
                .thenReturn(List.of(c));
        return c;
    }

    void service(long id, String price, boolean covered, String qty, List<Long> notes) {
        var s = mock(ServicoCobranca.class);
        when(s.getId()).thenReturn(id);
        when(s.getTipo()).thenReturn("ADICIONAL");
        var item = mock(ItemTabelaCobranca.class);
        when(item.getId()).thenReturn(id + 100);
        when(item.getServico()).thenReturn(s);
        when(item.getCategoria()).thenReturn("");
        when(item.getPreco()).thenReturn(price == null ? null : n(price));
        when(item.getTabela()).thenReturn(table);
        prices.add(item);
        var f = mock(FatoServico.class);
        when(f.getId()).thenReturn(id + 200);
        when(f.getServico()).thenReturn(s);
        when(f.getQuantidade()).thenReturn(n(qty));
        when(f.getExecutadoEm()).thenReturn(synthetic.plusSeconds(3600));
        when(f.getSituacao()).thenReturn("CONFIRMADO");
        when(f.getCategoria()).thenReturn("");
        when(f.getChaveFato()).thenReturn("D29-local-" + id);
        serviceFacts.add(f);
        var shares = new ArrayList<RateioFatoServico>();
        for (long noteId : notes) {
            var note = mock(NotaEntrada.class);
            when(note.getId()).thenReturn(noteId);
            var share = mock(RateioFatoServico.class);
            when(share.getNota()).thenReturn(note);
            when(share.getCota()).thenReturn(BigDecimal.ONE);
            shares.add(share);
        }
        when(d(RateioFatoServicoRepository.class).findByFatoIdOrderByNotaIdAsc(id + 200))
                .thenReturn(shares);
        if (covered) {
            var m = mock(ServicoMinimoContrato.class);
            when(m.getServico()).thenReturn(s);
            when(d(ServicoMinimoContratoRepository.class).findByContratoId(3L))
                    .thenReturn(List.of(m));
        }
    }

    CalculoCobrancaDto.Resultado calculate(int days) {
        return calculator.calcular(
                new CalculoCobrancaDto.Calcular(
                        UUID.randomUUID(),
                        1L,
                        2L,
                        start,
                        start.plusDays(days),
                        "D29 calculo local fontes ficticias",
                        null));
    }

    void physicalEntry(String value) {
        var e = mock(EntradaConferida.class, RETURNS_DEEP_STUBS);
        when(e.getId()).thenReturn(51L);
        when(e.getUnitizadaEm()).thenReturn(null);
        when(e.getItemChegada().getChegada().getChegouEm()).thenReturn(synthetic);
        when(e.getQuantidadeTriagem()).thenReturn(n("100"));
        when(e.getQuantidadeQuarentena()).thenReturn(BigDecimal.ZERO);
        when(e.getItemChegada().getItemNota().getQuantidadePrevista()).thenReturn(n("100"));
        when(e.getItemChegada().getItemNota().getValorMercadoria()).thenReturn(n(value));
        when(d(EntradaConferidaRepository.class).historicoCobranca(1L, 2L)).thenReturn(List.of(e));
    }

    void gris(ContratoCobranca c, String period, String proportion, String pct, String base) {
        ReflectionTestUtils.setField(c, "grisModo", "APLICAVEL");
        ReflectionTestUtils.setField(c, "grisPeriodicidade", period);
        ReflectionTestUtils.setField(c, "grisProporcao", proportion);
        ReflectionTestUtils.setField(c, "grisPercentual", n(pct));
        ReflectionTestUtils.setField(c, "grisBase", base);
    }

    @Test
    void d29VigF21MinimumCoveredAndGrisExcluded66() {
        var c = contract("50", "APLICAVEL", "INTEGRAL");
        service(5, "30", true, "1", List.of(11L));
        service(6, "12", false, "1", List.of(12L));
        physicalEntry("400");
        gris(c, "DIARIA", "INTEGRAL", "1", "VALOR_ESTOQUE_PICO");
        var r = calculate(1);
        assertThat(r.total()).isEqualByComparingTo("66");
        assertThat(r.minimoCalculado()).isEqualByComparingTo("20");
        assertThat(r.grisCalculado()).isEqualByComparingTo("4");
    }

    @Test
    void d29VigF22MinimumNotAddedTwice76() {
        var c = contract("50", "APLICAVEL", "INTEGRAL");
        service(5, "60", true, "1", List.of(11L));
        service(6, "12", false, "1", List.of(12L));
        physicalEntry("400");
        gris(c, "DIARIA", "INTEGRAL", "1", "VALOR_ESTOQUE_PICO");
        var r = calculate(1);
        assertThat(r.total()).isEqualByComparingTo("76");
        assertThat(r.minimoCalculado()).isZero();
    }

    @Test
    void d29VigF23ProrationTenOfThirty30() {
        contract("90", "APLICAVEL", "PROPORCIONAL_DIAS");
        service(5, "12", true, "1", List.of(11L));
        var r = calculate(10);
        assertThat(r.total()).isEqualByComparingTo("30");
        assertThat(r.memoria().ajustes().minimoAplicavel()).isEqualByComparingTo("30");
        assertThat(r.minimoCalculado()).isEqualByComparingTo("18");
    }

    @ParameterizedTest
    @ValueSource(strings = {"NAO_INFORMADO", "NAO_APLICAVEL", "APLICAVEL"})
    void d29VigF24AbsentNonApplicableAndExplicitZero(String mode) {
        contract(mode.equals("APLICAVEL") ? "0" : null, mode, "INTEGRAL");
        service(5, "0", true, "1", List.of(11L));
        var r = calculate(1);
        assertThat(r.situacao()).isEqualTo(mode.equals("NAO_INFORMADO") ? "PENDENTE" : "COMPLETO");
        if (mode.equals("NAO_INFORMADO")) assertThat(r.total()).isNull();
        else assertThat(r.total()).isZero();
    }

    @Test
    void d29VigF28CycleGrisProratedTen() {
        var c = contract(null, "NAO_APLICAVEL", null);
        physicalEntry("3000");
        gris(c, "POR_CICLO", "PROPORCIONAL_DIAS", "1", "VALOR_ESTOQUE_PICO");
        var r = calculate(10);
        assertThat(r.grisCalculado()).isEqualByComparingTo("10");
    }

    @Test
    void d29VigF29DailyGrisNoSecondProration300() {
        var c = contract(null, "NAO_APLICAVEL", null);
        physicalEntry("3000");
        gris(c, "DIARIA", "PROPORCIONAL_DIAS", "1", "VALOR_ESTOQUE_PICO");
        var r = calculate(10);
        assertThat(r.grisCalculado()).isEqualByComparingTo("300");
    }

    @Test
    void d29VigF18ExplicitZeroComplete() {
        contract(null, "NAO_APLICAVEL", null);
        service(5, "0", false, "1", List.of(11L));
        var r = calculate(1);
        assertThat(r.situacao()).isEqualTo("COMPLETO");
        assertThat(r.total()).isZero();
    }

    @Test
    void d29VigF19NoLinkNoFallback() {
        contract(null, "NAO_APLICAVEL", null);
        service(5, "7", false, "1", List.of(11L));
        when(d(VinculoTabelaClienteRepository.class)
                        .findByClienteIdAndArmazemIdOrderByVigenciaInicioAsc(1L, 2L))
                .thenReturn(List.of());
        var r = calculate(1);
        assertThat(r.situacao()).isEqualTo("PENDENTE");
        assertThat(r.total()).isNull();
        assertThat(r.pendencias())
                .extracting(CalculoCobrancaDto.Pendencia::codigo)
                .contains("TABELA_AUSENTE");
    }

    @Test
    void d29VigF31Monthly24DiffersFromThirtyDays() {
        var c = contract(null, "NAO_APLICAVEL", null);
        ReflectionTestUtils.setField(c, "modalidadeCiclo", "MES_DIA_FIXO");
        ReflectionTestUtils.setField(c, "duracaoDias", null);
        ReflectionTestUtils.setField(c, "diaCorte", 24);
        var one = PeriodoCobrancaService.ciclo(c, LocalDate.of(2026, 10, 24));
        var next = PeriodoCobrancaService.ciclo(c, one.fim());
        assertThat(one.dias()).isEqualTo(31);
        ReflectionTestUtils.setField(c, "modalidadeCiclo", "DIAS_CORRIDOS");
        ReflectionTestUtils.setField(c, "diaCorte", null);
        ReflectionTestUtils.setField(c, "duracaoDias", 30);
        assertThat(PeriodoCobrancaService.ciclo(c, LocalDate.of(2026, 10, 24)).dias())
                .isEqualTo(30);
        ReflectionTestUtils.setField(c, "modalidadeCiclo", "MES_DIA_FIXO");
        ReflectionTestUtils.setField(c, "diaCorte", 24);
        ReflectionTestUtils.setField(c, "duracaoDias", null);
        assertThat(next.inicio()).isEqualTo(one.fim());
        assertThat(
                        PeriodoCobrancaService.sobrepoe(
                                one.inicio(), one.fim(), next.inicio(), next.fim()))
                .isFalse();
    }

    @Test
    void d29VigF32Nominal31SurvivesFebruary() {
        var c = contract(null, "NAO_APLICAVEL", null);
        ReflectionTestUtils.setField(c, "modalidadeCiclo", "MES_DIA_FIXO");
        ReflectionTestUtils.setField(c, "duracaoDias", null);
        ReflectionTestUtils.setField(c, "diaCorte", 31);
        var feb = PeriodoCobrancaService.ciclo(c, LocalDate.of(2026, 1, 31));
        var mar = PeriodoCobrancaService.ciclo(c, feb.fim());
        assertThat(feb.dias()).isEqualTo(28);
        assertThat(mar.dias()).isEqualTo(31);
        assertThat(c.getDiaCorte()).isEqualTo(31);
    }

    @Test
    void d29VigF37CentavoContractConservation() {
        contract(null, "NAO_APLICAVEL", null);
        service(5, "0.01", false, "1", List.of(11L, 12L));
        var r = calculate(1);
        assertThat(r.total()).isEqualByComparingTo("0.01");
        assertThat(
                        r.memoria().servicos().getFirst().parcelas().stream()
                                .map(CalculoCobrancaDto.Parcela::valor)
                                .reduce(BigDecimal.ZERO, BigDecimal::add))
                .isEqualByComparingTo("0.01");
    }

    @Test
    void d29LumeF19DamageAtNoonKeepsPeakThenFutureEight() {
        contract(null, "NAO_APLICAVEL", null);
        var storage = mock(ServicoCobranca.class);
        when(storage.getId()).thenReturn(9L);
        when(storage.getTipo()).thenReturn("ARMAZENAGEM");
        var price = mock(ItemTabelaCobranca.class);
        when(price.getId()).thenReturn(109L);
        when(price.getServico()).thenReturn(storage);
        when(price.getTabela()).thenReturn(table);
        when(price.getCategoria()).thenReturn("");
        when(price.getPreco()).thenReturn(n("10"));
        prices.add(price);
        var u = mock(UnidadeLogistica.class, RETURNS_DEEP_STUBS);
        when(u.getId()).thenReturn(90L);
        when(u.getCodigo()).thenReturn("D29-LOCAL-U90");
        when(u.getCriadaEm()).thenReturn(synthetic);
        when(u.getInicioArmazenagemEm()).thenReturn(synthetic);
        when(u.getQuantidade()).thenReturn(n("100"));
        when(u.getPosicoesEquivalentes()).thenReturn(1);
        when(u.isAtiva()).thenReturn(true);
        when(u.getTipo()).thenReturn(TipoUnidadeLogistica.PALLET);
        when(u.getCondicao()).thenReturn(CondicaoMercadoria.BOA);
        when(u.getPedido().getId()).thenReturn(20L);
        when(d(UnidadeLogisticaRepository.class).historicoCobranca(1L, 2L)).thenReturn(List.of(u));
        var content = mock(ConteudoUnidade.class, RETURNS_DEEP_STUBS);
        when(content.getQuantidade()).thenReturn(n("100"));
        when(content.getEntrada().getId()).thenReturn(51L);
        when(content.getEntrada().getUnitizadaEm()).thenReturn(synthetic);
        when(content.getEntrada().getItemChegada().getItemNota().getQuantidadePrevista())
                .thenReturn(n("100"));
        when(content.getEntrada().getItemChegada().getItemNota().getValorMercadoria())
                .thenReturn(n("1000"));
        when(d(ConteudoUnidadeRepository.class).buscarOrigens(90L)).thenReturn(List.of(content));
        var av = mock(AvariaEstoque.class);
        when(av.getId()).thenReturn(70L);
        when(av.getQuantidade()).thenReturn(n("20"));
        when(av.getOcorridaEm()).thenReturn(synthetic.plusSeconds(86400 + 43200));
        when(av.getReconhecidaEm()).thenReturn(synthetic.plusSeconds(86400 + 43200));
        when(av.getResponsabilidade()).thenReturn("RODOGARCIA");
        when(d(AvariaEstoqueRepository.class).findByUnidadeIdOrderById(90L))
                .thenReturn(List.of(av));
        var r = calculate(3);
        assertThat(r.situacao()).isEqualTo("COMPLETO");
        assertThat(r.total()).isEqualByComparingTo("28");
        assertThat(r.memoria().diarias().get(1).valor()).isEqualByComparingTo("10");
        assertThat(r.memoria().diarias().get(2).valor()).isEqualByComparingTo("8");
        assertThat(u.getQuantidade()).isEqualByComparingTo("100");
        assertThat(u.getPosicoesEquivalentes()).isEqualTo(1);
    }

    UnidadeLogistica storageFixture(String priceValue, int positions) {
        contract(null, "NAO_APLICAVEL", null);
        var storage = mock(ServicoCobranca.class);
        when(storage.getId()).thenReturn(9L);
        when(storage.getTipo()).thenReturn("ARMAZENAGEM");
        var price = mock(ItemTabelaCobranca.class);
        when(price.getId()).thenReturn(109L);
        when(price.getServico()).thenReturn(storage);
        when(price.getTabela()).thenReturn(table);
        when(price.getCategoria()).thenReturn("");
        when(price.getPreco()).thenReturn(priceValue == null ? null : n(priceValue));
        prices.add(price);
        var u = mock(UnidadeLogistica.class, RETURNS_DEEP_STUBS);
        when(u.getId()).thenReturn(90L);
        when(u.getCodigo()).thenReturn("D29-LOCAL-U90");
        when(u.getCriadaEm()).thenReturn(synthetic);
        when(u.getInicioArmazenagemEm()).thenReturn(synthetic);
        when(u.getQuantidade()).thenReturn(n("100"));
        when(u.getPosicoesEquivalentes()).thenReturn(positions);
        when(u.isAtiva()).thenReturn(true);
        when(u.getTipo()).thenReturn(TipoUnidadeLogistica.PALLET);
        when(u.getCondicao()).thenReturn(CondicaoMercadoria.BOA);
        when(u.getPedido().getId()).thenReturn(20L);
        when(d(UnidadeLogisticaRepository.class).historicoCobranca(1L, 2L)).thenReturn(List.of(u));
        var content = mock(ConteudoUnidade.class, RETURNS_DEEP_STUBS);
        when(content.getQuantidade()).thenReturn(n("100"));
        when(content.getEntrada().getId()).thenReturn(51L);
        when(content.getEntrada().getUnitizadaEm()).thenReturn(synthetic);
        when(content.getEntrada().getItemChegada().getItemNota().getQuantidadePrevista())
                .thenReturn(n("100"));
        when(content.getEntrada().getItemChegada().getItemNota().getValorMercadoria())
                .thenReturn(n("1000"));
        when(d(ConteudoUnidadeRepository.class).buscarOrigens(90L)).thenReturn(List.of(content));
        return u;
    }

    AvariaEstoque damage(
            String qty, long seconds, String owner, boolean known, Long repairSeconds) {
        var a = mock(AvariaEstoque.class);
        when(a.getId()).thenReturn(70L);
        when(a.getQuantidade()).thenReturn(n(qty));
        when(a.getOcorridaEm()).thenReturn(synthetic.plusSeconds(seconds));
        when(a.getReconhecidaEm()).thenReturn(known ? synthetic.plusSeconds(seconds) : null);
        when(a.getResponsabilidade()).thenReturn(owner);
        when(a.getResolvidaEm())
                .thenReturn(repairSeconds == null ? null : synthetic.plusSeconds(repairSeconds));
        return a;
    }

    @Test
    void d29VigF01OneFullDaySeven() {
        storageFixture("7", 1);
        var r = calculate(1);
        assertThat(r.situacao()).isEqualTo("COMPLETO");
        assertThat(r.total()).isEqualByComparingTo("7");
        assertThat(r.memoria().diarias().getFirst().picoCobravel()).isEqualByComparingTo("1");
    }

    @Test
    void d29VigF07TwoPositionsThreeDaysAndServices51() {
        var u = storageFixture("7", 2);
        service(5, "4", false, "1", List.of(11L));
        service(6, "5", false, "1", List.of(11L));
        var r = calculate(3);
        assertThat(r.total()).isEqualByComparingTo("51");
        assertThat(r.memoria().diarias())
                .allSatisfy(day -> assertThat(day.valor()).isEqualByComparingTo("14"));
        assertThat(u.getQuantidade()).isEqualByComparingTo("100");
    }

    @Test
    void d29VigF10TwentyPercentAtDayStart18Point20() {
        var u = storageFixture("7", 1);
        var damage = damage("20", 86400, "RODOGARCIA", true, null);
        when(d(AvariaEstoqueRepository.class).findByUnidadeIdOrderById(90L))
                .thenReturn(List.of(damage));
        var r = calculate(3);
        assertThat(r.total()).isEqualByComparingTo("18.20");
        assertThat(u.getPosicoesEquivalentes()).isEqualTo(1);
        assertThat(u.getQuantidade()).isEqualByComparingTo("100");
    }

    @Test
    void d29VigF11ClientDamageDoesNotSuspendStorage21() {
        storageFixture("7", 1);
        var damage = damage("20", 86400, "CLIENTE", true, null);
        when(d(AvariaEstoqueRepository.class).findByUnidadeIdOrderById(90L))
                .thenReturn(List.of(damage));
        var r = calculate(3);
        assertThat(r.total()).isEqualByComparingTo("21");
        assertThat(r.memoria().diarias().get(1).valorEstoque()).isEqualByComparingTo("800");
    }

    @Test
    void d29VigF12UnknownResponsibilityPendingNull() {
        storageFixture("7", 1);
        var damage = damage("20", 86400, "NAO_INFORMADA", false, null);
        when(d(AvariaEstoqueRepository.class).findByUnidadeIdOrderById(90L))
                .thenReturn(List.of(damage));
        var r = calculate(3);
        assertThat(r.situacao()).isEqualTo("PENDENTE");
        assertThat(r.total()).isNull();
        assertThat(r.pendencias())
                .extracting(CalculoCobrancaDto.Pendencia::codigo)
                .contains("RESPONSABILIDADE_AVARIA_PENDENTE");
    }

    @Test
    void d29VigF14Aggregate160Of100PendingNoClamp() {
        storageFixture("7", 1);
        var a = damage("80", 86400, "RODOGARCIA", true, null);
        var b = damage("80", 86400, "RODOGARCIA", true, null);
        when(b.getId()).thenReturn(71L);
        when(d(AvariaEstoqueRepository.class).findByUnidadeIdOrderById(90L))
                .thenReturn(List.of(a, b));
        var r = calculate(3);
        assertThat(r.situacao()).isEqualTo("PENDENTE");
        assertThat(r.total()).isNull();
        assertThat(r.pendencias())
                .extracting(CalculoCobrancaDto.Pendencia::codigo)
                .contains("AVARIAS_HISTORICAS_INCONSISTENTES");
    }

    @Test
    void d29VigF15RepairRestoresFutureDay19Point60() {
        storageFixture("7", 1);
        var damage = damage("20", 86400, "RODOGARCIA", true, 172800L);
        when(d(AvariaEstoqueRepository.class).findByUnidadeIdOrderById(90L))
                .thenReturn(List.of(damage));
        var r = calculate(3);
        assertThat(r.total()).isEqualByComparingTo("19.60");
        assertThat(r.memoria().diarias().get(2).valor()).isEqualByComparingTo("7");
    }

    @Test
    void d29VigF17MissingPriceIsPendingNotZero() {
        storageFixture("7", 1);
        prices.clear();
        service(5, "0", false, "1", List.of(11L));
        var r = calculate(1);
        assertThat(r.situacao()).isEqualTo("PENDENTE");
        assertThat(r.total()).isNull();
    }
}
