package br.com.rodogarcia.wms.services;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Rateio cumulativo: parcelas sucessivas conservam o valor da origem até o último centavo. */
public final class FiscalNfeValores {
    private FiscalNfeValores() {}
    public static BigDecimal parcela(BigDecimal valor,BigDecimal quantidadeOriginal,BigDecimal anterior,BigDecimal quantidade) {
        if(valor==null || valor.signum()<0 || quantidadeOriginal==null || quantidadeOriginal.signum()<=0
                || anterior==null || anterior.signum()<0 || quantidade==null || quantidade.signum()<=0
                || anterior.add(quantidade).compareTo(quantidadeOriginal)>0)
            throw CadastroSupport.invalido("Quantidade/valor fiscal da origem não permite o rateio.");
        return valor.multiply(anterior.add(quantidade)).divide(quantidadeOriginal,2,RoundingMode.HALF_UP)
                .subtract(valor.multiply(anterior).divide(quantidadeOriginal,2,RoundingMode.HALF_UP));
    }
}
