package br.com.rodogarcia.wms;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.rodogarcia.wms.config.AmbienteConfig;
import java.sql.SQLException;
import java.util.concurrent.ExecutionException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.context.ActiveProfiles;

/** Confere o oráculo e a preparação do IT sem abrir contexto, pool ou conexão SQL. */
class SqlServerLocalITTest {
    @Test
    void aceitaSomente1222OriginadoDaBusca() {
        var busca = falha(1222);
        assertThatCode(
                        () ->
                                SqlServerLocalIT.exigirTimeoutDeLock(
                                        new ExecutionException(busca), busca))
                .doesNotThrowAnyException();
    }

    @ParameterizedTest
    @ValueSource(ints = {229, 0, 1205})
    void recusaPermissaoConexaoOuDeadlock(int codigo) {
        var busca = falha(codigo);
        assertThatThrownBy(
                        () ->
                                SqlServerLocalIT.exigirTimeoutDeLock(
                                        new ExecutionException(busca), busca))
                .isInstanceOf(AssertionError.class);
    }

    @Test
    void recusaFalhaDoResetMesmoComBusca1222() {
        var busca = falha(1222);
        assertThatThrownBy(
                        () ->
                                SqlServerLocalIT.exigirTimeoutDeLock(
                                        new ExecutionException(falha(229)), busca))
                .isInstanceOf(AssertionError.class);
    }

    @Test
    void recusaErroSemSqlNativoOuSemFalhaNaBusca() {
        var busca = new DataAccessResourceFailureException("Falha fictícia sem SQLException");
        assertThatThrownBy(
                        () ->
                                SqlServerLocalIT.exigirTimeoutDeLock(
                                        new ExecutionException(busca), busca))
                .isInstanceOf(AssertionError.class);
        assertThatThrownBy(
                        () ->
                                SqlServerLocalIT.exigirTimeoutDeLock(
                                        new ExecutionException(falha(1222)), null))
                .isInstanceOf(AssertionError.class);
    }

    @Test
    void perfilDoItPassaPelaGuardaRealDePerfilUnico() {
        var ambiente = new MockEnvironment();
        ambiente.setActiveProfiles(
                SqlServerLocalIT.class.getAnnotation(ActiveProfiles.class).value());
        assertThatCode(() -> new AmbienteConfig(ambiente)).doesNotThrowAnyException();
    }

    @Test
    void rodadaInvalidaERecusadaAntesDeBindPoolOuConexao() {
        var contexto = new org.springframework.context.support.GenericApplicationContext();
        contexto.setEnvironment(
                new MockEnvironment().withProperty("WMS_SQLSERVER_IT_ROUND", "D26';SQL"));
        try (contexto) {
            assertThatThrownBy(() -> new SqlServerLocalIT.Guarda().initialize(contexto))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Identificador de rodada SQL D26 inválido.");
        }
    }

    private DataAccessResourceFailureException falha(int codigo) {
        return new DataAccessResourceFailureException(
                "Falha fictícia",
                new IllegalStateException(new SQLException("SQL fictício", "HY000", codigo)));
    }
}
