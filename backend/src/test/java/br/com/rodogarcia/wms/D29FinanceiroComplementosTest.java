package br.com.rodogarcia.wms;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import br.com.rodogarcia.wms.dto.CalculoCobrancaDto;
import br.com.rodogarcia.wms.models.BaixaSaida;
import br.com.rodogarcia.wms.models.CondicaoMercadoria;
import br.com.rodogarcia.wms.models.ConteudoUnidade;
import br.com.rodogarcia.wms.models.FatoPermanencia;
import br.com.rodogarcia.wms.models.ItemTabelaCobranca;
import br.com.rodogarcia.wms.models.MarcoFinanceiroAvaria;
import br.com.rodogarcia.wms.models.MovimentoEstoque;
import br.com.rodogarcia.wms.models.TabelaCobranca;
import br.com.rodogarcia.wms.models.TipoUnidadeLogistica;
import br.com.rodogarcia.wms.models.UnidadeLogistica;
import br.com.rodogarcia.wms.models.VinculoTabelaCliente;
import br.com.rodogarcia.wms.repositories.AvariaEstoqueRepository;
import br.com.rodogarcia.wms.repositories.BaixaSaidaRepository;
import br.com.rodogarcia.wms.repositories.ConteudoUnidadeRepository;
import br.com.rodogarcia.wms.repositories.ContratoCobrancaRepository;
import br.com.rodogarcia.wms.repositories.FatoPermanenciaRepository;
import br.com.rodogarcia.wms.repositories.ItemTabelaCobrancaRepository;
import br.com.rodogarcia.wms.repositories.MarcoFinanceiroAvariaRepository;
import br.com.rodogarcia.wms.repositories.MovimentoEstoqueRepository;
import br.com.rodogarcia.wms.repositories.UnidadeLogisticaRepository;
import br.com.rodogarcia.wms.repositories.VinculoTabelaClienteRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

/** D29: expectativas VigiaF/Lume definidas antes, motor publico real e fontes isoladas. */
class D29FinanceiroComplementosTest {
    final D29FinanceiroOraculosTest f;

    D29FinanceiroComplementosTest() throws Exception {
        f = new D29FinanceiroOraculosTest();
    }

    BigDecimal n(String s) {
        return new BigDecimal(s);
    }

    ConteudoUnidade origin(long entry, String qty, String value, Instant unitized) {
        var c = mock(ConteudoUnidade.class, RETURNS_DEEP_STUBS);
        when(c.getQuantidade()).thenReturn(n(qty));
        when(c.getEntrada().getId()).thenReturn(entry);
        when(c.getEntrada().getUnitizadaEm()).thenReturn(unitized);
        when(c.getEntrada().getItemChegada().getItemNota().getQuantidadePrevista())
                .thenReturn(n(qty));
        when(c.getEntrada().getItemChegada().getItemNota().getValorMercadoria())
                .thenReturn(n(value));
        return c;
    }

    UnidadeLogistica extra(
            long id, Instant start, String qty, String value, TipoUnidadeLogistica type) {
        var u = mock(UnidadeLogistica.class, RETURNS_DEEP_STUBS);
        when(u.getId()).thenReturn(id);
        when(u.getCodigo()).thenReturn("D29-LOCAL-U" + id);
        when(u.getCriadaEm()).thenReturn(start);
        when(u.getInicioArmazenagemEm()).thenReturn(start);
        when(u.getQuantidade()).thenReturn(n(qty));
        when(u.getPosicoesEquivalentes()).thenReturn(1);
        when(u.isAtiva()).thenReturn(true);
        when(u.getTipo()).thenReturn(type);
        when(u.getCondicao()).thenReturn(CondicaoMercadoria.BOA);
        when(u.getPedido().getId()).thenReturn(id + 100);
        var c = origin(id + 200, qty, value, start);
        when(f.d(ConteudoUnidadeRepository.class).buscarOrigens(id)).thenReturn(List.of(c));
        return u;
    }

    FatoPermanencia event(
            UnidadeLogistica u,
            long id,
            String type,
            Instant at,
            String before,
            String after,
            String eqBefore,
            String eqAfter) {
        var e =
                new FatoPermanencia(
                        u,
                        UUID.randomUUID().toString(),
                        type,
                        at,
                        at,
                        n(before),
                        n(after),
                        n(eqBefore),
                        n(eqAfter));
        ReflectionTestUtils.setField(e, "id", id);
        when(f.d(FatoPermanenciaRepository.class)
                        .findByUnidadeIdOrderByOcorridaEmAscIdAsc(u.getId()))
                .thenReturn(List.of(e));
        return e;
    }

    void withdraw(UnidadeLogistica u, Instant at, String originalQty, String remains) {
        var old = f.d(ConteudoUnidadeRepository.class).buscarOrigens(u.getId()).getFirst();
        when(u.getQuantidade()).thenReturn(n(remains));
        when(u.isAtiva()).thenReturn(n(remains).signum() > 0);
        when(old.getQuantidade()).thenReturn(n(remains));
        event(
                u,
                u.getId() + 400,
                "RETIRADA",
                at,
                originalQty,
                remains,
                "1",
                n(remains).signum() == 0 ? "0" : "1");
        var b = mock(BaixaSaida.class, RETURNS_DEEP_STUBS);
        long entryId = old.getEntrada().getId();
        when(b.getEntradaOrigem().getId()).thenReturn(entryId);
        when(b.getRetirada().getRetiradaEm()).thenReturn(at);
        when(b.getQuantidade()).thenReturn(n(originalQty).subtract(n(remains)));
        when(f.d(BaixaSaidaRepository.class).buscarDaUnidade(u.getId())).thenReturn(List.of(b));
    }

    void complete(CalculoCobrancaDto.Resultado r, String total) {
        assertThat(r.pendencias()).isEmpty();
        assertThat(r.situacao()).isEqualTo("COMPLETO");
        assertThat(r.total()).isEqualByComparingTo(total);
    }

    @Test
    void d29VigF01WithdrawalDateExcludedSeven() {
        var u = f.storageFixture("7", 1);
        withdraw(u, f.synthetic.plusSeconds(86400 + 3600), "100", "0");
        var r = f.calculate(2);
        complete(r, "7");
        assertThat(r.memoria().diarias())
                .extracting(CalculoCobrancaDto.Diaria::valor)
                .containsExactly(n("7.00"), n("0.00"));
    }

    @Test
    void d29VigF02SameDayWithdrawalStorageZeroPhysicalGris15() {
        var u = f.storageFixture("7", 1);
        var c =
                f.d(ContratoCobrancaRepository.class)
                        .findByClienteIdAndArmazemIdOrderByVigenciaInicioAsc(1L, 2L)
                        .getFirst();
        when(u.getCriadaEm()).thenReturn(f.synthetic.plusSeconds(3600));
        when(u.getInicioArmazenagemEm()).thenReturn(f.synthetic.plusSeconds(3600));
        var content = f.d(ConteudoUnidadeRepository.class).buscarOrigens(90L).getFirst();
        when(content.getEntrada().getUnitizadaEm()).thenReturn(f.synthetic.plusSeconds(3600));
        when(content.getEntrada().getItemChegada().getItemNota().getValorMercadoria())
                .thenReturn(n("1500"));
        withdraw(u, f.synthetic.plusSeconds(7200), "100", "0");
        f.gris(c, "DIARIA", "INTEGRAL", "1", "VALOR_ESTOQUE_PICO");
        var r = f.calculate(1);
        complete(r, "15");
        assertThat(r.subtotalConhecido()).isZero();
        assertThat(r.grisCalculado()).isEqualByComparingTo("15");
        assertThat(r.memoria().diarias().getFirst().intervalosValor())
                .anyMatch(v -> v.total() != null && v.total().compareTo(n("1500")) == 0);
    }

    @Test
    void d29VigF03ThreeRemovementsNoExtraEquivalence14() {
        f.storageFixture("7", 1);
        var moves = new ArrayList<MovimentoEstoque>();
        for (int i = 1; i <= 3; i++) {
            var m = mock(MovimentoEstoque.class);
            when(m.getAcao()).thenReturn("REManejamento".toUpperCase(Locale.ROOT));
            when(m.getInstante()).thenReturn(f.synthetic.plusSeconds(i * 3600));
            moves.add(m);
        }
        when(f.d(MovimentoEstoqueRepository.class).findByUnidadeIdOrderByInstanteAscIdAsc(90L))
                .thenReturn(moves);
        complete(f.calculate(2), "14");
    }

    @Test
    void d29VigF04SeparationRetainsOneAndOtherUsesReleasedSpace35() {
        var a = f.storageFixture("7", 1);
        var b =
                extra(
                        91,
                        f.synthetic.plusSeconds(86400),
                        "100",
                        "1000",
                        TipoUnidadeLogistica.PALLET);
        var move = mock(MovimentoEstoque.class);
        when(move.getAcao()).thenReturn("SEPARACAO");
        when(move.getInstante()).thenReturn(f.synthetic.plusSeconds(86400));
        when(f.d(MovimentoEstoqueRepository.class).findByUnidadeIdOrderByInstanteAscIdAsc(90L))
                .thenReturn(List.of(move));
        when(f.d(UnidadeLogisticaRepository.class).historicoCobranca(1L, 2L))
                .thenReturn(List.of(a, b));
        var r = f.calculate(3);
        complete(r, "35");
        assertThat(r.memoria().diarias())
                .extracting(CalculoCobrancaDto.Diaria::picoCobravel)
                .usingComparatorForType(BigDecimal::compareTo, BigDecimal.class)
                .containsExactly(n("1"), n("2"), n("2"));
    }

    @Test
    void d29VigF05SuccessivePermanencesNotSummed14() {
        var a = f.storageFixture("7", 1);
        withdraw(a, f.synthetic.plusSeconds(86400), "100", "0");
        var b =
                extra(
                        91,
                        f.synthetic.plusSeconds(86400),
                        "100",
                        "1000",
                        TipoUnidadeLogistica.PALLET);
        when(f.d(UnidadeLogisticaRepository.class).historicoCobranca(1L, 2L))
                .thenReturn(List.of(a, b));
        complete(f.calculate(2), "14");
    }

    @Test
    void d29VigF06SameDayPassageDoesNotIncreaseOtherDailyPeak14() {
        var a = f.storageFixture("7", 1);
        var b =
                extra(
                        91,
                        f.synthetic.plusSeconds(86400 + 3600),
                        "100",
                        "1000",
                        TipoUnidadeLogistica.PALLET);
        withdraw(b, f.synthetic.plusSeconds(86400 + 7200), "100", "0");
        when(f.d(UnidadeLogisticaRepository.class).historicoCobranca(1L, 2L))
                .thenReturn(List.of(a, b));
        var r = f.calculate(2);
        complete(r, "14");
        assertThat(r.memoria().diarias().getLast().picoCobravel()).isEqualByComparingTo("1");
    }

    @Test
    void d29VigF07EntryExitFactsNotDoubledByTwoPositions51() {
        f.storageFixture("7", 2);
        f.service(5, "4", false, "1", List.of(11L));
        f.service(6, "5", false, "1", List.of(11L));
        when(f.serviceFacts.get(0).getServico().getTipo()).thenReturn("ENTRADA");
        when(f.serviceFacts.get(1).getServico().getTipo()).thenReturn("SAIDA");
        var r = f.calculate(3);
        complete(r, "51");
        assertThat(r.memoria().servicos()).hasSize(2);
        assertThat(r.memoria().servicos().get(0).valor()).isEqualByComparingTo("4");
        assertThat(r.memoria().servicos().get(1).valor()).isEqualByComparingTo("5");
    }

    @Test
    void d29VigF09PartialWithdrawalPreservesPositionAndOriginValue720() {
        var u = f.storageFixture("7", 1);
        var c = f.d(ConteudoUnidadeRepository.class).buscarOrigens(90L).getFirst();
        when(c.getEntrada().getItemChegada().getItemNota().getValorMercadoria())
                .thenReturn(n("900"));
        withdraw(u, f.synthetic, "100", "80");
        var r = f.calculate(2);
        complete(r, "14");
        assertThat(r.memoria().diarias().getLast().valorEstoque()).isEqualByComparingTo("720");
        assertThat(u.getPosicoesEquivalentes()).isEqualTo(1);
    }

    @Test
    void d29VigF13Base100Then50WithTwentyAffectedProved9Point80() {
        var u = f.storageFixture("7", 1);
        withdraw(u, f.synthetic.plusSeconds(86400), "100", "50");
        var damage = f.damage("20", 0, "RODOGARCIA", true, null);
        when(f.d(AvariaEstoqueRepository.class).findByUnidadeIdOrderById(90L))
                .thenReturn(List.of(damage));
        var event =
                f.d(FatoPermanenciaRepository.class)
                        .findByUnidadeIdOrderByOcorridaEmAscIdAsc(90L)
                        .getFirst();
        var mark =
                new MarcoFinanceiroAvaria(
                        damage,
                        event,
                        n("20"),
                        n("50"),
                        n("1"),
                        "D29-local",
                        f.synthetic,
                        "D29 prova isolada destino20");
        when(f.d(MarcoFinanceiroAvariaRepository.class).findByAvariaIdOrderByIdAsc(70L))
                .thenReturn(List.of(mark));
        var r = f.calculate(2);
        complete(r, "9.80");
        assertThat(r.memoria().diarias())
                .extracting(CalculoCobrancaDto.Diaria::equivalenciaSuspensa)
                .containsExactly(n("0.200000"), n("0.400000"));
    }

    @Test
    void d29VigF16TwoPricesFourDays28() {
        f.storageFixture("5", 1);
        var second = mock(TabelaCobranca.class);
        when(second.getId()).thenReturn(44L);
        when(second.getVigenciaInicio()).thenReturn(f.start.plusDays(2));
        when(f.table.getVigenciaFim()).thenReturn(f.start.plusDays(2));
        var l1 = mock(VinculoTabelaCliente.class);
        when(l1.getTabela()).thenReturn(f.table);
        when(l1.getVigenciaInicio()).thenReturn(f.start);
        when(l1.getVigenciaFim()).thenReturn(f.start.plusDays(2));
        var l2 = mock(VinculoTabelaCliente.class);
        when(l2.getTabela()).thenReturn(second);
        when(l2.getVigenciaInicio()).thenReturn(f.start.plusDays(2));
        when(f.d(VinculoTabelaClienteRepository.class)
                        .findByClienteIdAndArmazemIdOrderByVigenciaInicioAsc(1L, 2L))
                .thenReturn(List.of(l1, l2));
        var item = mock(ItemTabelaCobranca.class);
        when(item.getId()).thenReturn(110L);
        when(item.getTabela()).thenReturn(second);
        doReturn(f.prices.getFirst().getServico()).when(item).getServico();
        when(item.getCategoria()).thenReturn("");
        when(item.getPreco()).thenReturn(n("9"));
        when(f.d(ItemTabelaCobrancaRepository.class).findByTabelaIdOrderByIdAsc(44L))
                .thenReturn(List.of(item));
        var r = f.calculate(4);
        complete(r, "28");
        assertThat(r.memoria().diarias())
                .extracting(CalculoCobrancaDto.Diaria::tarifa)
                .containsExactly(n("5"), n("5"), n("9"), n("9"));
    }

    @Test
    void d29VigF20SeparateCategoriesAndTariffs18() {
        var a = f.storageFixture("7", 1);
        when(f.prices.getFirst().getCategoria()).thenReturn("PALLET");
        var b = extra(91, f.synthetic, "100", "1000", TipoUnidadeLogistica.BOBINA);
        when(f.d(UnidadeLogisticaRepository.class).historicoCobranca(1L, 2L))
                .thenReturn(List.of(a, b));
        var item = mock(ItemTabelaCobranca.class);
        when(item.getId()).thenReturn(110L);
        when(item.getTabela()).thenReturn(f.table);
        doReturn(f.prices.getFirst().getServico()).when(item).getServico();
        when(item.getCategoria()).thenReturn("BOBINA");
        when(item.getPreco()).thenReturn(n("11"));
        f.prices.add(item);
        var r = f.calculate(1);
        complete(r, "18");
        assertThat(r.memoria().diarias().getFirst().tarifa()).isNull();
        assertThat(r.memoria().diarias().getFirst().regras()).hasSize(2);
    }

    void physicalPeaks() {
        var first = f.storageFixture("0", 1);
        var c = f.d(ConteudoUnidadeRepository.class).buscarOrigens(90L).getFirst();
        when(c.getEntrada().getItemChegada().getItemNota().getValorMercadoria())
                .thenReturn(n("1000"));
        var second =
                extra(
                        91,
                        f.synthetic.plusSeconds(86400),
                        "100",
                        "1000",
                        TipoUnidadeLogistica.PALLET);
        withdraw(second, f.synthetic.plusSeconds(172800), "100", "0");
        when(f.d(UnidadeLogisticaRepository.class).historicoCobranca(1L, 2L))
                .thenReturn(List.of(first, second));
    }

    @Test
    void d29VigF25CyclePeak20() {
        physicalPeaks();
        var c =
                f.d(ContratoCobrancaRepository.class)
                        .findByClienteIdAndArmazemIdOrderByVigenciaInicioAsc(1L, 2L)
                        .getFirst();
        f.gris(c, "POR_CICLO", "INTEGRAL", "1", "VALOR_ESTOQUE_PICO");
        var r = f.calculate(3);
        complete(r, "20");
        assertThat(r.memoria().diarias())
                .extracting(CalculoCobrancaDto.Diaria::valorEstoque)
                .usingComparatorForType(BigDecimal::compareTo, BigDecimal.class)
                .containsExactly(
                        n("1000.000000000000"), n("2000.000000000000"), n("1000.000000000000"));
    }

    @Test
    void d29VigF26CycleMean13Point33() {
        physicalPeaks();
        var c =
                f.d(ContratoCobrancaRepository.class)
                        .findByClienteIdAndArmazemIdOrderByVigenciaInicioAsc(1L, 2L)
                        .getFirst();
        f.gris(c, "POR_CICLO", "INTEGRAL", "1", "VALOR_ESTOQUE_MEDIA");
        complete(f.calculate(3), "13.33");
    }

    @Test
    void d29VigF27DailyPeaks40WithoutSecondDuration() {
        physicalPeaks();
        var c =
                f.d(ContratoCobrancaRepository.class)
                        .findByClienteIdAndArmazemIdOrderByVigenciaInicioAsc(1L, 2L)
                        .getFirst();
        f.gris(c, "DIARIA", "INTEGRAL", "1", "VALOR_ESTOQUE_PICO");
        complete(f.calculate(3), "40");
    }

    @Test
    void d29VigF36ExecutionPriceFiveInsteadOfLateRegistrationNine() {
        f.contract(null, "NAO_APLICAVEL", null);
        f.service(5, "5", false, "2", List.of(11L));
        when(f.table.getVigenciaFim()).thenReturn(f.start.plusDays(2));
        var l1 = mock(VinculoTabelaCliente.class);
        when(l1.getTabela()).thenReturn(f.table);
        when(l1.getVigenciaInicio()).thenReturn(f.start);
        when(l1.getVigenciaFim()).thenReturn(f.start.plusDays(2));
        var t2 = mock(TabelaCobranca.class);
        when(t2.getId()).thenReturn(44L);
        when(t2.getVigenciaInicio()).thenReturn(f.start.plusDays(2));
        var l2 = mock(VinculoTabelaCliente.class);
        when(l2.getTabela()).thenReturn(t2);
        when(l2.getVigenciaInicio()).thenReturn(f.start.plusDays(2));
        when(f.d(VinculoTabelaClienteRepository.class)
                        .findByClienteIdAndArmazemIdOrderByVigenciaInicioAsc(1L, 2L))
                .thenReturn(List.of(l1, l2));
        when(f.serviceFacts.getFirst().getRegistradoEm())
                .thenReturn(f.synthetic.plusSeconds(172800));
        var later = mock(ItemTabelaCobranca.class);
        when(later.getId()).thenReturn(111L);
        when(later.getTabela()).thenReturn(t2);
        doReturn(f.serviceFacts.getFirst().getServico()).when(later).getServico();
        when(later.getCategoria()).thenReturn("");
        when(later.getPreco()).thenReturn(n("9"));
        when(f.d(ItemTabelaCobrancaRepository.class).findByTabelaIdOrderByIdAsc(44L))
                .thenReturn(List.of(later));
        var r = f.calculate(3);
        complete(r, "10");
        assertThat(r.memoria().servicos().getFirst().preco()).isEqualByComparingTo("5");
        assertThat(r.memoria().servicos().getFirst().executadoEm())
                .isEqualTo(f.synthetic.plusSeconds(3600));
    }

    @Test
    void d29VigF37RepeatedOrderedRateioConservesDeterministicCentavo() {
        f.contract(null, "NAO_APLICAVEL", null);
        f.service(5, "0.01", false, "1", List.of(11L, 12L));
        var first = f.calculate(1).memoria().servicos().getFirst().parcelas();
        var second = f.calculate(1).memoria().servicos().getFirst().parcelas();
        assertThat(first).isEqualTo(second);
        assertThat(first).extracting(CalculoCobrancaDto.Parcela::notaId).containsExactly(11L, 12L);
        assertThat(
                        first.stream()
                                .map(CalculoCobrancaDto.Parcela::valor)
                                .reduce(BigDecimal.ZERO, BigDecimal::add))
                .isEqualByComparingTo("0.01");
        assertThat(first).filteredOn(p -> p.valor().compareTo(n("0.01")) == 0).hasSize(1);
    }

    @Test
    void d29VigF38CivilMinusThreeIncludesUtcNextDateInPreviousDay() {
        var c = f.contract(null, "NAO_APLICAVEL", null);
        ReflectionTestUtils.setField(c, "fuso", "America/Sao_Paulo");
        f.service(5, "7", false, "1", List.of(11L));
        var instant = Instant.parse("2026-10-08T02:59:59Z");
        when(f.serviceFacts.getFirst().getExecutadoEm()).thenReturn(instant);
        var r =
                f.calculator.calcular(
                        new CalculoCobrancaDto.Calcular(
                                UUID.randomUUID(),
                                1L,
                                2L,
                                LocalDate.of(2026, 10, 7),
                                LocalDate.of(2026, 10, 8),
                                "D29 instante sintetico so local",
                                null));
        complete(r, "7");
        assertThat(instant.atZone(ZoneId.of("America/Sao_Paulo")).toLocalDate())
                .isEqualTo(LocalDate.of(2026, 10, 7));
        assertThat(instant.atZone(ZoneOffset.UTC).toLocalDate())
                .isEqualTo(LocalDate.of(2026, 10, 8));
    }

    @Test
    void d29VigF39NewCalculationDoesNotRewritePriorPublicResult() {
        f.contract(null, "NAO_APLICAVEL", null);
        f.service(5, "5", false, "1", List.of(11L));
        var original = f.calculate(1);
        String bytes = f.mapper.writeValueAsString(original);
        f.service(6, "7", false, "1", List.of(12L));
        var current = f.calculate(1);
        complete(current, "12");
        assertThat(original.total()).isEqualByComparingTo("5");
        assertThat(current.id()).isNotEqualTo(original.id());
        assertThat(current.entradasHash()).isNotEqualTo(original.entradasHash());
        assertThat(f.mapper.writeValueAsString(original)).isEqualTo(bytes);
    }

    @Test
    void d29VigF08DatedEquivalenceIncreaseDoesNotRestartPermanence35() {
        var u = f.storageFixture("7", 2);
        event(u, 501L, "AJUSTE_ESTOQUE", f.synthetic.plusSeconds(86400), "100", "100", "1", "2");
        var r = f.calculate(3);
        complete(r, "35");
        assertThat(r.memoria().diarias())
                .extracting(CalculoCobrancaDto.Diaria::picoCobravel)
                .usingComparatorForType(BigDecimal::compareTo, BigDecimal.class)
                .containsExactly(n("1"), n("2"), n("2"));
        assertThat(u.getInicioArmazenagemEm()).isEqualTo(f.synthetic);
    }

    @Test
    void d29VigF30DifferentOriginPricesWithoutDamagedOriginStayPendingNull() {
        f.storageFixture("7", 1);
        var a = origin(51L, "40", "400", f.synthetic);
        var b = origin(52L, "60", "1200", f.synthetic);
        when(f.d(ConteudoUnidadeRepository.class).buscarOrigens(90L)).thenReturn(List.of(a, b));
        var damage = f.damage("20", 0, "RODOGARCIA", true, null);
        when(f.d(AvariaEstoqueRepository.class).findByUnidadeIdOrderById(90L))
                .thenReturn(List.of(damage));
        var r = f.calculate(1);
        assertThat(r.situacao()).isEqualTo("PENDENTE");
        assertThat(r.total()).isNull();
        assertThat(r.pendencias())
                .extracting(CalculoCobrancaDto.Pendencia::codigo)
                .contains("ORIGEM_AVARIA_VALOR_INSUFICIENTE");
    }

    @Test
    void d29VigF12MissingTemporalBaseDoesNotPresumeDamagePercentage() {
        var u = f.storageFixture("7", 1);
        when(u.getQuantidade()).thenReturn(n("50"));
        event(u, 501L, "RETIRADA", f.synthetic.plusSeconds(86400), "100", "80", "1", "1");
        var damage = f.damage("20", 0, "RODOGARCIA", true, null);
        when(f.d(AvariaEstoqueRepository.class).findByUnidadeIdOrderById(90L))
                .thenReturn(List.of(damage));
        var r = f.calculate(2);
        assertThat(r.total()).isNull();
        assertThat(r.pendencias())
                .extracting(CalculoCobrancaDto.Pendencia::codigo)
                .contains("HISTORICO_QUANTIDADE_INSUFICIENTE");
    }

    @Test
    void d29VigF12UndatedLegacyDamageFlagDoesNotFabricateSuspension() {
        var u = f.storageFixture("7", 1);
        when(u.isAvariaPosterior()).thenReturn(true);
        var r = f.calculate(2);
        assertThat(r.total()).isNull();
        assertThat(r.pendencias())
                .extracting(CalculoCobrancaDto.Pendencia::codigo)
                .contains("HISTORICO_AVARIA_INSUFICIENTE");
    }

    @Test
    void d29VigF30KnownDamagedOriginViaSeparateIdentitiesValue1400() {
        var a = f.storageFixture("0", 1);
        when(a.getQuantidade()).thenReturn(n("40"));
        var ca = origin(51L, "40", "400", f.synthetic);
        when(f.d(ConteudoUnidadeRepository.class).buscarOrigens(90L)).thenReturn(List.of(ca));
        var b = extra(91L, f.synthetic, "60", "1200", TipoUnidadeLogistica.PALLET);
        when(f.d(UnidadeLogisticaRepository.class).historicoCobranca(1L, 2L))
                .thenReturn(List.of(a, b));
        var damage = f.damage("20", 0, "CLIENTE", true, null);
        when(f.d(AvariaEstoqueRepository.class).findByUnidadeIdOrderById(90L))
                .thenReturn(List.of(damage));
        var r = f.calculate(1);
        complete(r, "0");
        assertThat(r.memoria().diarias().getFirst().valorEstoque()).isEqualByComparingTo("1400");
        assertThat(a.getQuantidade()).isEqualByComparingTo("40");
        assertThat(b.getQuantidade()).isEqualByComparingTo("60");
    }

    @Test
    void d29VigF39PendingSnapshotRemainsNullAfterConfigurationCompletes() {
        f.contract(null, "NAO_APLICAVEL", null);
        f.service(5, "5", false, "1", List.of(11L));
        var originalLinks =
                f.d(VinculoTabelaClienteRepository.class)
                        .findByClienteIdAndArmazemIdOrderByVigenciaInicioAsc(1L, 2L);
        when(f.d(VinculoTabelaClienteRepository.class)
                        .findByClienteIdAndArmazemIdOrderByVigenciaInicioAsc(1L, 2L))
                .thenReturn(List.of());
        var original = f.calculate(1);
        String bytes = f.mapper.writeValueAsString(original);
        assertThat(original.total()).isNull();
        assertThat(original.situacao()).isEqualTo("PENDENTE");
        when(f.d(VinculoTabelaClienteRepository.class)
                        .findByClienteIdAndArmazemIdOrderByVigenciaInicioAsc(1L, 2L))
                .thenReturn(originalLinks);
        var current = f.calculate(1);
        complete(current, "5");
        assertThat(current.id()).isNotEqualTo(original.id());
        assertThat(current.entradasHash()).isNotEqualTo(original.entradasHash());
        assertThat(f.mapper.writeValueAsString(original)).isEqualTo(bytes);
        assertThat(original.total()).isNull();
    }
}
