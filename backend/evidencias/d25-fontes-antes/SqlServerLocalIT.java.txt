package br.com.rodogarcia.wms;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.rodogarcia.wms.config.IdentidadeTesteConfig;
import br.com.rodogarcia.wms.config.SqlServerEnsaioLocalGuard;
import br.com.rodogarcia.wms.config.SqlServerProperties;
import br.com.rodogarcia.wms.models.Armazem;
import br.com.rodogarcia.wms.models.AuditoriaCadastro;
import br.com.rodogarcia.wms.models.Cliente;
import br.com.rodogarcia.wms.models.NotaEntrada;
import br.com.rodogarcia.wms.models.OperacaoAdministrativa;
import br.com.rodogarcia.wms.models.PedidoEntrada;
import br.com.rodogarcia.wms.repositories.ArmazemRepository;
import br.com.rodogarcia.wms.repositories.AuditoriaCadastroRepository;
import br.com.rodogarcia.wms.repositories.ClienteRepository;
import br.com.rodogarcia.wms.repositories.NotaEntradaRepository;
import br.com.rodogarcia.wms.repositories.OperacaoAdministrativaRepository;
import br.com.rodogarcia.wms.repositories.PedidoEntradaRepository;
import jakarta.persistence.EntityManagerFactory;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** Nunca incluído pelo Surefire; exige -Psqlserver-it e guardas antes do DataSource. */
@ActiveProfiles({"sqlserver-dev", "sqlserver-it"})
@Import(IdentidadeTesteConfig.class)
@ContextConfiguration(initializers = SqlServerLocalIT.Guarda.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SqlServerLocalIT {
    @Autowired private JdbcTemplate jdbc;
    @Autowired private EntityManagerFactory emf;
    @Autowired private PlatformTransactionManager transactions;
    @Autowired private ClienteRepository clientes;
    @Autowired private AuditoriaCadastroRepository auditoria;
    @Autowired private OperacaoAdministrativaRepository operacoes;
    @Autowired private ArmazemRepository armazens;
    @Autowired private PedidoEntradaRepository entradas;
    @Autowired private NotaEntradaRepository notas;

    public static class Guarda
            implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        @Override
        public void initialize(ConfigurableApplicationContext contexto) {
            var env = contexto.getEnvironment();
            SqlServerEnsaioLocalGuard.validar(
                    env,
                    Binder.get(env)
                            .bind("wms.database", Bindable.of(SqlServerProperties.class))
                            .get());
        }
    }

    @BeforeAll
    void exigeMigrationsCompletasBancoVazioEIdentidadeRestrita() {
        assertThat(
                        emf.unwrap(SessionFactoryImplementor.class)
                                .getJdbcServices()
                                .getDialect()
                                .getClass()
                                .getSimpleName())
                .isEqualTo("SQLServerDialect");
        assertThat(jdbc.queryForObject("SELECT IS_SRVROLEMEMBER('sysadmin')", Integer.class))
                .isZero();
        assertThat(jdbc.queryForObject("SELECT IS_MEMBER('db_owner')", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT IS_MEMBER('db_ddladmin')", Integer.class)).isZero();
        assertThat(
                        jdbc.queryForObject(
                                "SELECT HAS_PERMS_BY_NAME('wms','SCHEMA','ALTER')", Integer.class))
                .isZero();
        assertThat(
                        jdbc.queryForObject(
                                "SELECT COUNT(*) FROM wms.flyway_schema_history WHERE success=1 AND version IN ('1','2','3','4','5','6','7','8','9')",
                                Integer.class))
                .isEqualTo(9);
        var tabelas =
                jdbc.queryForList(
                        "SELECT t.name FROM sys.tables t JOIN sys.schemas s ON s.schema_id=t.schema_id WHERE s.name='wms' AND t.name<>'flyway_schema_history'",
                        String.class);
        assertThat(tabelas).hasSize(64);
        for (String tabela : tabelas) {
            assertThat(
                            jdbc.queryForObject(
                                    "SELECT COUNT_BIG(*) FROM wms.["
                                            + tabela.replace("]", "]]")
                                            + "]",
                                    Long.class))
                    .as("Ensaio exige todas as tabelas operacionais vazias")
                    .isZero();
        }
        for (String tabela :
                List.of("auditoria_cadastro", "operacao_administrativa", "movimento_estoque")) {
            for (String privilegio : List.of("UPDATE", "DELETE")) {
                assertThat(
                                jdbc.queryForObject(
                                        "SELECT HAS_PERMS_BY_NAME(?, 'OBJECT', ?)",
                                        Integer.class,
                                        "wms." + tabela,
                                        privilegio))
                        .isZero();
            }
        }
    }

    @Test
    void sessoesDoPoolTemOpcoesDosIndicesFiltrados() {
        for (String opcao :
                List.of(
                        "ANSI_NULLS",
                        "ANSI_PADDING",
                        "ANSI_WARNINGS",
                        "ARITHABORT",
                        "CONCAT_NULL_YIELDS_NULL",
                        "QUOTED_IDENTIFIER")) {
            assertThat(
                            jdbc.queryForObject(
                                    "SELECT CONVERT(int,SESSIONPROPERTY(?))", Integer.class, opcao))
                    .isEqualTo(1);
        }
        assertThat(
                        jdbc.queryForObject(
                                "SELECT CONVERT(int,SESSIONPROPERTY('NUMERIC_ROUNDABORT'))",
                                Integer.class))
                .isZero();
    }

    @Test
    void constraintsDoSchemaSaoConfiaveisEAtivas() {
        for (String catalogo : List.of("sys.check_constraints", "sys.foreign_keys")) {
            assertThat(
                            jdbc.queryForObject(
                                    "SELECT COUNT(*) FROM "
                                            + catalogo
                                            + " WHERE schema_id=SCHEMA_ID('wms') AND (is_not_trusted=1 OR is_disabled=1)",
                                    Integer.class))
                    .isZero();
        }
    }

    @Test
    void indiceFiltradoAceitaDuasChavesNulasERecusaPreenchidaDuplicada() {
        new TransactionTemplate(transactions)
                .executeWithoutResult(
                        status -> {
                            var instante = Instant.now();
                            var cliente = clientes.saveAndFlush(novoCliente(instante));
                            String codigo = "D20" + UUID.randomUUID().toString().substring(0, 20);
                            var armazem =
                                    armazens.saveAndFlush(
                                            new Armazem(
                                                    codigo,
                                                    "Armazém fictício",
                                                    codigo,
                                                    "Cidade fictícia",
                                                    "SP",
                                                    instante));
                            var pedido =
                                    entradas.saveAndFlush(
                                            new PedidoEntrada(cliente, armazem, codigo, instante));
                            var primeira =
                                    notas.saveAndFlush(
                                            new NotaEntrada(
                                                    pedido,
                                                    cliente.getDocumentoFiscal(),
                                                    1,
                                                    1,
                                                    LocalDate.of(2026, 10, 6),
                                                    null));
                            var segunda =
                                    notas.saveAndFlush(
                                            new NotaEntrada(
                                                    pedido,
                                                    cliente.getDocumentoFiscal(),
                                                    1,
                                                    2,
                                                    LocalDate.of(2026, 10, 6),
                                                    null));
                            assertThat(primeira.getId()).isNotEqualTo(segunda.getId());
                            primeira.vincularXml("1".repeat(44), "0".repeat(64), "<ficticio/>");
                            notas.flush();
                            segunda.vincularXml("1".repeat(44), "0".repeat(64), "<ficticio/>");
                            assertThatThrownBy(notas::flush)
                                    .isInstanceOf(DataAccessException.class);
                            status.setRollbackOnly();
                        });
    }

    @Test
    void atualizaCadastroComColunasPermitidasEPreservaUnicodeEDatas() {
        var tx = new TransactionTemplate(transactions);
        tx.executeWithoutResult(
                status -> {
                    var instante = Instant.parse("2026-10-06T10:00:00.123456Z");
                    var cliente = clientes.saveAndFlush(novoCliente(instante));
                    cliente.alterarDescricao("Ação fictícia revisada", instante.plusSeconds(1));
                    clientes.flush();
                    assertThat(clientes.buscarParaAtualizar(cliente.getId())).isPresent();
                    assertThat(
                                    jdbc.queryForObject(
                                            "SELECT nome FROM wms.cliente WHERE id=?",
                                            String.class,
                                            cliente.getId()))
                            .isEqualTo("Ação fictícia revisada");
                    var salvo =
                            jdbc.queryForObject(
                                    "SELECT criado_em FROM wms.cliente WHERE id=?",
                                    java.sql.Timestamp.class,
                                    cliente.getId());
                    assertThat(
                                    salvo.toLocalDateTime()
                                            .toInstant(ZoneOffset.UTC)
                                            .truncatedTo(ChronoUnit.MICROS))
                            .isEqualTo(instante);
                    status.setRollbackOnly();
                });
    }

    @Test
    void falhaNaAuditoriaReverteCadastroNaMesmaTransacao() {
        long antes = clientes.count();
        assertThatThrownBy(
                        () ->
                                new TransactionTemplate(transactions)
                                        .executeWithoutResult(
                                                status -> {
                                                    var cliente =
                                                            clientes.saveAndFlush(
                                                                    novoCliente(Instant.now()));
                                                    auditoria.saveAndFlush(
                                                            new AuditoriaCadastro(
                                                                    "CLIENTE",
                                                                    cliente.getId(),
                                                                    "ACAO_INVALIDA",
                                                                    "usuario-ficticio",
                                                                    Instant.now(),
                                                                    UUID.randomUUID().toString(),
                                                                    "Falha fictícia",
                                                                    null,
                                                                    "{}"));
                                                }))
                .isInstanceOf(DataAccessException.class);
        assertThat(clientes.count()).isEqualTo(antes);
    }

    @Test
    void chaveIdempotenteDuplicadaERecusadaPeloIndiceReal() {
        new TransactionTemplate(transactions)
                .executeWithoutResult(
                        status -> {
                            String chave = UUID.randomUUID().toString();
                            operacoes.saveAndFlush(operacao(chave));
                            assertThatThrownBy(() -> operacoes.saveAndFlush(operacao(chave)))
                                    .isInstanceOf(DataAccessException.class);
                            status.setRollbackOnly();
                        });
    }

    @Test
    void lockDoRepositorySerializaDuasTransacoes() throws Exception {
        Long id =
                new TransactionTemplate(transactions)
                        .execute(
                                status ->
                                        clientes.saveAndFlush(novoCliente(Instant.now())).getId());
        var bloqueado = new CountDownLatch(1);
        var liberar = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var primeiro =
                    executor.submit(
                            () ->
                                    new TransactionTemplate(transactions)
                                            .executeWithoutResult(
                                                    status -> {
                                                        assertThat(clientes.buscarParaAtualizar(id))
                                                                .isPresent();
                                                        bloqueado.countDown();
                                                        aguardar(liberar);
                                                    }));
            assertThat(bloqueado.await(10, TimeUnit.SECONDS)).isTrue();
            try {
                var segundo =
                        executor.submit(
                                () ->
                                        new TransactionTemplate(transactions)
                                                .executeWithoutResult(
                                                        status -> {
                                                            jdbc.execute("SET LOCK_TIMEOUT 1000");
                                                            try {
                                                                clientes.buscarParaAtualizar(id);
                                                            } finally {
                                                                jdbc.execute("SET LOCK_TIMEOUT -1");
                                                            }
                                                        }));
                assertThatThrownBy(() -> segundo.get(10, TimeUnit.SECONDS))
                        .hasCauseInstanceOf(DataAccessException.class);
            } finally {
                liberar.countDown();
            }
            primeiro.get(10, TimeUnit.SECONDS);
        } finally {
            liberar.countDown();
        }
        // A fixture confirmada fica no alvo isolado: não excluir histórico nem limpar banco.
    }

    private static void aguardar(CountDownLatch latch) {
        try {
            if (!latch.await(15, TimeUnit.SECONDS))
                throw new IllegalStateException("Prazo do ensaio excedido.");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Ensaio interrompido.", e);
        }
    }

    private Cliente novoCliente(Instant instante) {
        String codigo = "D20" + UUID.randomUUID().toString().replace("-", "").substring(0, 20);
        return new Cliente(codigo, "Ação fictícia", codigo, instante);
    }

    private OperacaoAdministrativa operacao(String chave) {
        return new OperacaoAdministrativa(
                chave,
                "CRIACAO_SERVICO",
                null,
                null,
                null,
                "0".repeat(64),
                "{}",
                "usuario-ficticio",
                Instant.now());
    }
}
