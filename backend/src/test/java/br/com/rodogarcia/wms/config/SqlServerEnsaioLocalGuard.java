package br.com.rodogarcia.wms.config;

import org.springframework.core.env.Environment;

/** Guarda do ensaio optativo; não autoriza nem provisiona banco ou credenciais. */
public final class SqlServerEnsaioLocalGuard {
    private SqlServerEnsaioLocalGuard() {}

    public static void validar(Environment env, SqlServerProperties alvo) {
        alvo.validarAlvo();
        if (!"D20_LOCAL_ISOLADO".equals(env.getProperty("WMS_SQLSERVER_IT"))
                || !"127.0.0.1".equals(alvo.host())
                || !alvo.name().equals("WMS_DEV")
                || !alvo.name().equals(env.getProperty("WMS_SQLSERVER_IT_CONFIRMED_DATABASE"))) {
            throw new IllegalStateException(
                    "Ensaio SQL Server exige alvo local isolado confirmado.");
        }
    }
}
