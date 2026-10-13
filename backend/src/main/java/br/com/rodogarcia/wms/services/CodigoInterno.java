package br.com.rodogarcia.wms.services;

import java.util.Locale;
import java.util.function.LongSupplier;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** Código interno atribuído na mesma transação que grava o novo registro. */
public final class CodigoInterno {
    private CodigoInterno() {}

    public static String resolver(String informado, String prefixo, LongSupplier ultimo) {
        if (informado != null && !informado.isBlank()) return CadastroSupport.codigo(informado);
        if (!TransactionSynchronizationManager.isActualTransactionActive()
                || TransactionSynchronizationManager.isCurrentTransactionReadOnly()) {
            throw new IllegalStateException("Código automático exige transação de escrita.");
        }
        long atual = ultimo.getAsLong();
        if (atual < 0 || atual == Long.MAX_VALUE) {
            throw CadastroSupport.invalido("Sequência de códigos internos esgotada.");
        }
        return prefixo + "-" + String.format(Locale.ROOT, "%06d", atual + 1);
    }
}
