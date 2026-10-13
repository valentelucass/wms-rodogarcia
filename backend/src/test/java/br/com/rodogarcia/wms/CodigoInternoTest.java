package br.com.rodogarcia.wms;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import br.com.rodogarcia.wms.dto.ClienteDto;
import br.com.rodogarcia.wms.models.Cliente;
import br.com.rodogarcia.wms.repositories.ArmazemRepository;
import br.com.rodogarcia.wms.repositories.CargaInicialRepository;
import br.com.rodogarcia.wms.repositories.ClienteRepository;
import br.com.rodogarcia.wms.repositories.ConjuntoPosicoesRepository;
import br.com.rodogarcia.wms.repositories.OperacaoSaidaRepository;
import br.com.rodogarcia.wms.repositories.PedidoEntradaRepository;
import br.com.rodogarcia.wms.repositories.PedidoSaidaRepository;
import br.com.rodogarcia.wms.repositories.ServicoCobrancaRepository;
import br.com.rodogarcia.wms.repositories.TabelaCobrancaRepository;
import br.com.rodogarcia.wms.services.AcessoService;
import br.com.rodogarcia.wms.services.AuditoriaService;
import br.com.rodogarcia.wms.services.ClienteService;
import br.com.rodogarcia.wms.services.CodigoInterno;
import br.com.rodogarcia.wms.services.NfeXmlService;
import br.com.rodogarcia.wms.services.PedidoSaidaService;
import java.time.Clock;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** Testes puros: flags locais de transação e repositories mockados, sem contexto/conexão. */
class CodigoInternoTest {
    @Test
    void repeticaoDeSaidaAutomaticaRecuperaResultadoAntesDeReservarOutroCodigo() throws Exception {
        var constructor = PedidoSaidaService.class.getConstructors()[0];
        Object[] dependencies =
                java.util.Arrays.stream(constructor.getParameterTypes())
                        .map(org.mockito.Mockito::mock)
                        .toArray();
        var service = (PedidoSaidaService) constructor.newInstance(dependencies);
        var clientes = dependency(dependencies, ClienteRepository.class);
        var armazens = dependency(dependencies, ArmazemRepository.class);
        var operacoes = dependency(dependencies, OperacaoSaidaRepository.class);
        var pedidos = dependency(dependencies, PedidoSaidaRepository.class);
        var mapper = dependency(dependencies, tools.jackson.databind.json.JsonMapper.class);
        when(clientes.buscarParaAtualizar(1L))
                .thenReturn(java.util.Optional.of(mock(Cliente.class)));
        when(armazens.buscarParaAtualizar(2L))
                .thenReturn(
                        java.util.Optional.of(mock(br.com.rodogarcia.wms.models.Armazem.class)));
        var id = java.util.UUID.randomUUID();
        var request =
                new br.com.rodogarcia.wms.dto.PedidoSaidaDto.Criar(
                        id, 1L, 2L, null, List.of(), "Teste puro");
        var old = mock(br.com.rodogarcia.wms.models.OperacaoSaida.class);
        when(mapper.writeValueAsString(request)).thenReturn("conteudo-original");
        when(old.getConteudoHash())
                .thenReturn(NfeXmlService.hash("CRIACAO_SAIDA:conteudo-original"));
        when(old.getResultado()).thenReturn("resultado-original");
        when(operacoes
                        .findFirstByPedidoClienteIdAndPedidoArmazemIdAndOperacaoIdAndTipoOrderByIdDesc(
                                1L, 2L, id.toString(), "CRIACAO_SAIDA"))
                .thenReturn(java.util.Optional.of(old));
        var result = mock(br.com.rodogarcia.wms.dto.PedidoSaidaDto.Confirmacao.class);
        when(mapper.readValue(
                        "resultado-original",
                        br.com.rodogarcia.wms.dto.PedidoSaidaDto.Confirmacao.class))
                .thenReturn(result);
        assertThat(service.criar(request)).isSameAs(result);
        verifyNoInteractions(pedidos);
        when(mapper.writeValueAsString(request)).thenReturn("conteudo-alterado");
        assertThatThrownBy(() -> service.criar(request)).hasMessageContaining("outro conteúdo");
        verifyNoInteractions(pedidos);
    }

    private static <T> T dependency(Object[] dependencies, Class<T> type) {
        return java.util.Arrays.stream(dependencies)
                .filter(type::isInstance)
                .map(type::cast)
                .findFirst()
                .orElseThrow();
    }

    @AfterEach
    void limpar() {
        TransactionSynchronizationManager.clear();
    }

    @Test
    void geraSequenciaSemTruncarDepoisDeSeisDigitos() {
        TransactionSynchronizationManager.setActualTransactionActive(true);
        assertThat(CodigoInterno.resolver(null, "CLI", () -> 0)).isEqualTo("CLI-000001");
        assertThat(CodigoInterno.resolver(null, "ENT", () -> 41)).isEqualTo("ENT-000042");
        assertThat(CodigoInterno.resolver(" ", "SRV", () -> 999999)).isEqualTo("SRV-1000000");
        assertThatThrownBy(() -> CodigoInterno.resolver(null, "CLI", () -> Long.MAX_VALUE))
                .hasMessageContaining("esgotada");
    }

    @Test
    void recusaSemTransacaoOuSomenteLeituraAntesDeConsultar() {
        var calls = new AtomicInteger();
        assertThatThrownBy(() -> CodigoInterno.resolver(null, "CLI", calls::incrementAndGet))
                .isInstanceOf(IllegalStateException.class);
        TransactionSynchronizationManager.setActualTransactionActive(true);
        TransactionSynchronizationManager.setCurrentTransactionReadOnly(true);
        assertThatThrownBy(() -> CodigoInterno.resolver(null, "CLI", calls::incrementAndGet))
                .isInstanceOf(IllegalStateException.class);
        assertThat(calls.get()).isZero();
    }

    @Test
    void codigoExplicitoMantemCompatibilidadeSemReservarSequencia() {
        assertThat(
                        CodigoInterno.resolver(
                                " existente-10 ",
                                "CLI",
                                () -> {
                                    throw new AssertionError();
                                }))
                .isEqualTo("EXISTENTE-10");
    }

    @Test
    void clienteAutomaticoPassaPeloServicoEPermissaoAntesDaSequencia() {
        TransactionSynchronizationManager.setActualTransactionActive(true);
        var repository = mock(ClienteRepository.class);
        var acesso = mock(AcessoService.class);
        var auditoria = mock(AuditoriaService.class);
        when(repository.ultimoCodigoInterno()).thenReturn(12L);
        when(repository.saveAndFlush(any(Cliente.class))).thenAnswer(call -> call.getArgument(0));
        var service = new ClienteService(repository, acesso, auditoria, Clock.systemUTC());
        assertThat(
                        service.criar(new ClienteDto.Criar(null, "Cliente fictício", "12345678901"))
                                .codigo())
                .isEqualTo("CLI-000013");
        var order = inOrder(acesso, repository);
        order.verify(acesso).exigirGestor();
        order.verify(repository).ultimoCodigoInterno();
        order.verify(repository).saveAndFlush(any(Cliente.class));
        clearInvocations(repository);
        doThrow(new IllegalStateException("Sem permissão")).when(acesso).exigirGestor();
        assertThatThrownBy(
                        () ->
                                service.criar(
                                        new ClienteDto.Criar(
                                                null, "Cliente fictício", "12345678901")))
                .hasMessage("Sem permissão");
        verifyNoInteractions(repository);
    }

    @Test
    void consultasDeSequenciaConservamSerializacaoSqlSemExecutarBanco() throws Exception {
        for (Class<?> repository :
                List.of(
                        ClienteRepository.class,
                        ArmazemRepository.class,
                        ConjuntoPosicoesRepository.class,
                        ServicoCobrancaRepository.class,
                        TabelaCobrancaRepository.class,
                        PedidoEntradaRepository.class,
                        PedidoSaidaRepository.class,
                        CargaInicialRepository.class)) {
            var query = repository.getMethod("ultimoCodigoInterno").getAnnotation(Query.class);
            assertThat(query.nativeQuery()).isTrue();
            assertThat(query.value())
                    .contains("WITH (TABLOCKX, HOLDLOCK)", "TRY_CONVERT(bigint", "FROM wms.");
        }
    }
}
