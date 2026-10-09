package br.com.rodogarcia.wms;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import br.com.rodogarcia.wms.dto.CalculoCobrancaDto;
import br.com.rodogarcia.wms.dto.FatoServicoDto;
import br.com.rodogarcia.wms.models.FatoServico;
import br.com.rodogarcia.wms.models.NotaEntrada;
import br.com.rodogarcia.wms.models.PedidoEntrada;
import br.com.rodogarcia.wms.models.RateioFatoServico;
import br.com.rodogarcia.wms.repositories.FatoServicoRepository;
import br.com.rodogarcia.wms.repositories.NotaEntradaRepository;
import br.com.rodogarcia.wms.repositories.PedidoEntradaRepository;
import br.com.rodogarcia.wms.repositories.RateioFatoServicoRepository;
import br.com.rodogarcia.wms.repositories.ServicoCobrancaRepository;
import br.com.rodogarcia.wms.services.FatoServicoService;
import jakarta.validation.Validation;
import java.math.BigDecimal;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.test.util.ReflectionTestUtils;

/** RN09.03/CT29-L043-01: oraculo de um centavo, sem banco, contexto HTTP ou credenciais. */
class D30RateioUmCentavoTest {
    @ParameterizedTest(name = "um centavo ordem={0},{1},{2}")
    @CsvSource({"11,12,13", "11,13,12", "12,11,13", "12,13,11", "13,11,12", "13,12,11"})
    void umCentavoEntreTresNotasConservaTotalEIdentidadeEmTodasAsOrdens(
            Long primeira, Long segunda, Long terceira) throws Exception {
        var f = new D29FinanceiroOraculosTest();
        f.contract(null, "NAO_APLICAVEL", null);
        f.service(5, "0.01", false, "1", List.of());
        f.serviceFacts.clear();
        var servico = f.prices.getFirst().getServico();
        when(servico.getSituacao()).thenReturn("ATIVO");
        when(servico.getUnidade()).thenReturn("UNIDADE_LOGISTICA");
        when(dependencia(f.doubles, ServicoCobrancaRepository.class).buscarParaAtualizar(5L))
                .thenReturn(Optional.of(servico));
        var pedido = mock(PedidoEntrada.class);
        when(pedido.getId()).thenReturn(30L);
        when(pedido.getCliente()).thenReturn(f.client);
        when(pedido.getArmazem()).thenReturn(f.warehouse);
        when(pedido.getCriadoEm()).thenReturn(f.synthetic);
        when(dependencia(f.doubles, PedidoEntradaRepository.class).findById(30L))
                .thenReturn(Optional.of(pedido));
        var ordem = List.of(primeira, segunda, terceira);
        var notas = dependencia(f.doubles, NotaEntradaRepository.class);
        for (Long id : ordem) {
            var nota = mock(NotaEntrada.class);
            when(nota.getId()).thenReturn(id);
            when(nota.getPedido()).thenReturn(pedido);
            when(notas.findById(id)).thenReturn(Optional.of(nota));
            when(notas.getReferenceById(id)).thenReturn(nota);
        }
        var gravadas = new ArrayList<RateioFatoServico>();
        var rateios = dependencia(f.doubles, RateioFatoServicoRepository.class);
        when(rateios.save(any(RateioFatoServico.class)))
                .thenAnswer(
                        inv -> {
                            RateioFatoServico cota = inv.getArgument(0);
                            gravadas.add(cota);
                            return cota;
                        });
        // Mock da fronteira ordenada; nao comprova a ordenacao nativa do SQL Server.
        when(rateios.findByFatoIdOrderByNotaIdAsc(205L))
                .thenAnswer(
                        inv ->
                                gravadas.stream()
                                        .sorted(Comparator.comparing(c -> c.getNota().getId()))
                                        .toList());
        when(f.d(FatoServicoRepository.class).saveAndFlush(any(FatoServico.class)))
                .thenAnswer(
                        inv -> {
                            FatoServico fato = inv.getArgument(0);
                            ReflectionTestUtils.setField(fato, "id", 205L);
                            f.serviceFacts.add(fato);
                            return fato;
                        });
        var construtor = FatoServicoService.class.getConstructors()[0];
        var tipos = construtor.getParameterTypes();
        var argumentos = new Object[tipos.length];
        for (int i = 0; i < tipos.length; i++) {
            argumentos[i] =
                    tipos[i] == Clock.class
                            ? Clock.systemUTC()
                            : f.doubles.computeIfAbsent(tipos[i], t -> mock(t));
        }
        var registrador = (FatoServicoService) construtor.newInstance(argumentos);
        var comando =
                new FatoServicoDto.Registrar(
                        UUID.randomUUID(),
                        1L,
                        2L,
                        5L,
                        "MANUAL",
                        null,
                        30L,
                        null,
                        null,
                        "D30-UM-CENTAVO",
                        f.synthetic.plusSeconds(3600),
                        BigDecimal.ONE,
                        "",
                        null,
                        ordem.stream()
                                .map(id -> new FatoServicoDto.Cota(id, BigDecimal.ONE))
                                .toList(),
                        "Cotas iguais ficticias",
                        "Caso local D30",
                        null);
        try (var validation = Validation.buildDefaultValidatorFactory()) {
            assertThat(validation.getValidator().validate(comando)).isEmpty();
        }
        var registrado = registrador.registrar(comando);
        assertThat(gravadas).extracting(c -> c.getNota().getId()).containsExactlyElementsOf(ordem);
        assertThat(registrado.cotas())
                .extracting(FatoServicoDto.Cota::notaId)
                .containsExactly(11L, 12L, 13L);
        assertThat(f.serviceFacts).hasSize(1);
        var resultado = f.calculate(1);
        assertThat(resultado.situacao()).isEqualTo("COMPLETO");
        assertThat(resultado.pendencias()).isEmpty();
        assertThat(resultado.total()).isEqualByComparingTo("0.01");
        assertThat(resultado.memoria().servicos()).hasSize(1);
        var parcelas = resultado.memoria().servicos().getFirst().parcelas();
        assertThat(parcelas)
                .extracting(CalculoCobrancaDto.Parcela::notaId)
                .containsExactly(11L, 12L, 13L);
        // Oraculo independente pre-execucao: 1 centavo = 0 + 0 + 1 por ID crescente.
        var centavos = Map.of(11L, 0L, 12L, 0L, 13L, 1L);
        for (var parcela : parcelas) {
            assertThat(parcela.cota()).isEqualByComparingTo(BigDecimal.ONE);
            assertThat(parcela.valor().movePointRight(2).longValueExact())
                    .isEqualTo(centavos.get(parcela.notaId()));
        }
        assertThat(
                        parcelas.stream()
                                .map(CalculoCobrancaDto.Parcela::valor)
                                .reduce(BigDecimal.ZERO, BigDecimal::add))
                .isEqualByComparingTo("0.01");
    }

    private static <T> T dependencia(Map<Class<?>, Object> mocks, Class<T> tipo) {
        return tipo.cast(mocks.computeIfAbsent(tipo, t -> mock(t)));
    }
}
