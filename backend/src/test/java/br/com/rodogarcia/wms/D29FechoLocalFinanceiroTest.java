package br.com.rodogarcia.wms;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import br.com.rodogarcia.wms.dto.CalculoCobrancaDto;
import br.com.rodogarcia.wms.dto.ConfiguracaoCobrancaDto;
import br.com.rodogarcia.wms.dto.FatoServicoDto;
import br.com.rodogarcia.wms.models.Armazem;
import br.com.rodogarcia.wms.models.Cliente;
import br.com.rodogarcia.wms.models.ContratoCobranca;
import br.com.rodogarcia.wms.models.FatoServico;
import br.com.rodogarcia.wms.models.NotaEntrada;
import br.com.rodogarcia.wms.models.PedidoEntrada;
import br.com.rodogarcia.wms.models.RateioFatoServico;
import br.com.rodogarcia.wms.repositories.ContratoCobrancaRepository;
import br.com.rodogarcia.wms.repositories.FatoServicoRepository;
import br.com.rodogarcia.wms.repositories.NotaEntradaRepository;
import br.com.rodogarcia.wms.repositories.PedidoEntradaRepository;
import br.com.rodogarcia.wms.repositories.RateioFatoServicoRepository;
import br.com.rodogarcia.wms.repositories.ServicoCobrancaRepository;
import br.com.rodogarcia.wms.services.AcessoService;
import br.com.rodogarcia.wms.services.ConfiguracaoCobrancaService;
import br.com.rodogarcia.wms.services.ContextoCobrancaService;
import br.com.rodogarcia.wms.services.FatoServicoService;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.validation.beanvalidation.MethodValidationInterceptor;

/** Doc29:43/53/90. Servicos reais com mocks; sem Spring Boot, banco, rede ou credenciais. */
class D29FechoLocalFinanceiroTest {
    private static ValidatorFactory validation;

    @BeforeAll
    static void iniciarValidacaoLocal() {
        validation = Validation.buildDefaultValidatorFactory();
    }

    @AfterAll
    static void fecharValidacaoLocal() {
        validation.close();
    }

    @ParameterizedTest(name = "rateio ordem={0},{1},{2}")
    @CsvSource({"11,22,33", "11,33,22", "22,11,33", "22,33,11", "33,11,22", "33,22,11"})
    void f37TresNotasPreservamValoresPorIdNasSeisOrdens(Long primeira, Long segunda, Long terceira)
            throws Exception {
        var f = new D29FinanceiroOraculosTest();
        f.contract(null, "NAO_APLICAVEL", null);
        f.service(5, "1.00", false, "1", List.of());
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
        // Fronteira simulada cumpre o contrato OrderByNotaIdAsc; nao prova o ORDER BY real.
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
        var registrador = construir(FatoServicoService.class, f.doubles);
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
                        "D29-FECHO-LOCAL",
                        f.synthetic.plusSeconds(3600),
                        BigDecimal.ONE,
                        "",
                        null,
                        ordem.stream()
                                .map(id -> new FatoServicoDto.Cota(id, BigDecimal.ONE))
                                .toList(),
                        "Cotas iguais ficticias",
                        "Complemento local D29",
                        null);
        assertThat(validation.getValidator().validate(comando)).isEmpty();
        var registrado = registrador.registrar(comando);
        assertThat(gravadas).extracting(c -> c.getNota().getId()).containsExactlyElementsOf(ordem);
        assertThat(registrado.cotas())
                .extracting(FatoServicoDto.Cota::notaId)
                .containsExactly(11L, 22L, 33L);
        assertThat(f.serviceFacts).hasSize(1);

        var resultado = f.calculate(1);
        assertThat(resultado.situacao()).isEqualTo("COMPLETO");
        assertThat(resultado.pendencias()).isEmpty();
        assertThat(resultado.total()).isEqualByComparingTo("1.00");
        assertThat(resultado.memoria().servicos()).hasSize(1);
        var parcelas = resultado.memoria().servicos().getFirst().parcelas();
        assertThat(parcelas)
                .extracting(CalculoCobrancaDto.Parcela::notaId)
                .containsExactly(11L, 22L, 33L);
        // Oraculo anterior ao build: 100 centavos / 3 = 33 + 33 + 34; residuo no maior ID.
        var esperado = Map.of(11L, 33L, 22L, 33L, 33L, 34L);
        for (var parcela : parcelas) {
            assertThat(parcela.cota()).isEqualByComparingTo(BigDecimal.ONE);
            assertThat(parcela.valor().movePointRight(2).longValueExact())
                    .isEqualTo(esperado.get(parcela.notaId()));
        }
        assertThat(
                        parcelas.stream()
                                .map(CalculoCobrancaDto.Parcela::valor)
                                .reduce(BigDecimal.ZERO, BigDecimal::add))
                .isEqualByComparingTo("1.00");
    }

    @Test
    void prio06CorteUmAceitoNoServicoValidadoComDuracaoAusente() throws Exception {
        var mocks = new HashMap<Class<?>, Object>();
        var configurador = configuradorValidado(mocks);
        var inicio = Instant.parse("2026-09-01T00:00:00Z");
        var cliente = new Cliente("D29LOCAL", "Cliente ficticio", "11111111000111", inicio);
        var armazem =
                new Armazem(
                        "D29LOCAL", "Armazem ficticio", "22222222000122", "Osasco", "SP", inicio);
        ReflectionTestUtils.setField(cliente, "id", 1L);
        ReflectionTestUtils.setField(armazem, "id", 2L);
        when(dependencia(mocks, ContextoCobrancaService.class).bloquear(1L, 2L))
                .thenReturn(new ContextoCobrancaService.Contexto(cliente, armazem));
        var contratos = dependencia(mocks, ContratoCobrancaRepository.class);
        when(contratos.saveAndFlush(any(ContratoCobranca.class)))
                .thenAnswer(
                        inv -> {
                            ContratoCobranca contrato = inv.getArgument(0);
                            ReflectionTestUtils.setField(contrato, "id", 71L);
                            return contrato;
                        });
        var resultado = configurador.configurar(contrato(1));
        assertThat(resultado.id()).isEqualTo(71L);
        assertThat(resultado.diaCorte()).isEqualTo(1);
        assertThat(resultado.duracaoDias()).isNull();
        assertThat(resultado.modalidadeCiclo()).isEqualTo("MES_DIA_FIXO");
        var gravado = ArgumentCaptor.forClass(ContratoCobranca.class);
        verify(contratos).saveAndFlush(gravado.capture());
        assertThat(gravado.getValue().getDiaCorte()).isEqualTo(1);
        assertThat(gravado.getValue().getDuracaoDias()).isNull();
        verify(dependencia(mocks, AcessoService.class)).exigirGestor();
    }

    @ParameterizedTest(name = "corte={0} restricao={1}")
    @CsvSource({"0,Min", "32,Max"})
    void prio06CorteForaFaixaRecusadoAntesDeQualquerColaborador(int corte, String restricao)
            throws Exception {
        var mocks = new HashMap<Class<?>, Object>();
        var configurador = configuradorValidado(mocks);
        var erro = catchThrowable(() -> configurador.configurar(contrato(corte)));
        assertThat(erro).isInstanceOf(ConstraintViolationException.class);
        var violacoes = ((ConstraintViolationException) erro).getConstraintViolations();
        assertThat(violacoes).hasSize(1);
        var violacao = violacoes.iterator().next();
        assertThat(violacao.getPropertyPath().toString()).endsWith(".diaCorte");
        assertThat(
                        violacao.getConstraintDescriptor()
                                .getAnnotation()
                                .annotationType()
                                .getSimpleName())
                .isEqualTo(restricao);
        verifyNoInteractions(mocks.values().toArray());
    }

    private static ConfiguracaoCobrancaDto.ConfigurarContrato contrato(int corte) {
        return new ConfiguracaoCobrancaDto.ConfigurarContrato(
                UUID.randomUUID(),
                1L,
                2L,
                LocalDate.of(2026, 9, 1),
                null,
                "UTC",
                "BRL",
                "MES_DIA_FIXO",
                corte,
                null,
                "NAO_APLICAVEL",
                "NAO_APLICAVEL",
                null,
                null,
                List.of(),
                null,
                null,
                null,
                null,
                "Complemento local D29",
                null);
    }

    private static ConfiguracaoCobrancaService configuradorValidado(Map<Class<?>, Object> mocks)
            throws Exception {
        var proxy = new ProxyFactory(construir(ConfiguracaoCobrancaService.class, mocks));
        proxy.addAdvice(new MethodValidationInterceptor(validation.getValidator()));
        return (ConfiguracaoCobrancaService) proxy.getProxy();
    }

    private static <T> T construir(Class<T> tipo, Map<Class<?>, Object> mocks) throws Exception {
        var construtor = tipo.getConstructors()[0];
        var tipos = construtor.getParameterTypes();
        var argumentos = new Object[tipos.length];
        for (int i = 0; i < tipos.length; i++) {
            argumentos[i] =
                    tipos[i] == Clock.class
                            ? Clock.systemUTC()
                            : mocks.computeIfAbsent(tipos[i], t -> mock(t));
        }
        return tipo.cast(construtor.newInstance(argumentos));
    }

    private static <T> T dependencia(Map<Class<?>, Object> mocks, Class<T> tipo) {
        return tipo.cast(mocks.computeIfAbsent(tipo, t -> mock(t)));
    }
}
