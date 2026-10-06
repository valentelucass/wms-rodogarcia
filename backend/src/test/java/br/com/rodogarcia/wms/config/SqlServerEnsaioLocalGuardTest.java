package br.com.rodogarcia.wms.config;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.mock.env.MockEnvironment;

class SqlServerEnsaioLocalGuardTest {
    @ParameterizedTest
    @CsvSource({
        "sql.test.invalid,WMS_DEV",
        "localhost,WMS_DEV",
        "127.0.0.2,WMS_DEV",
        "127.0.0.1,WMS_PROD"
    })
    void recusaRedeDnsOuBancoComum(String host, String banco) {
        assertThatThrownBy(() -> SqlServerEnsaioLocalGuard.validar(ambiente(), alvo(host, banco)))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void recusaSemHabilitacaoOuConfirmacaoExata() {
        assertThatThrownBy(
                        () ->
                                SqlServerEnsaioLocalGuard.validar(
                                        new MockEnvironment(), alvo("127.0.0.1", "WMS_DEV")))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(
                        () ->
                                SqlServerEnsaioLocalGuard.validar(
                                        ambiente()
                                                .withProperty(
                                                        "WMS_SQLSERVER_IT_CONFIRMED_DATABASE",
                                                        "outro"),
                                        alvo("127.0.0.1", "WMS_DEV")))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void permiteConfiguracaoLocalConferidaSemConectar() {
        assertThatCode(
                        () ->
                                SqlServerEnsaioLocalGuard.validar(
                                        ambiente(), alvo("127.0.0.1", "WMS_DEV")))
                .doesNotThrowAnyException();
    }

    private MockEnvironment ambiente() {
        return new MockEnvironment()
                .withProperty("WMS_SQLSERVER_IT", "D20_LOCAL_ISOLADO")
                .withProperty("WMS_SQLSERVER_IT_CONFIRMED_DATABASE", "WMS_DEV");
    }

    private SqlServerProperties alvo(String host, String banco) {
        return new SqlServerProperties(
                host,
                1433,
                banco,
                "usuario-ficticio",
                "senha-ficticia",
                host + ":1433/" + banco,
                "SQL-FICTICIO");
    }
}
