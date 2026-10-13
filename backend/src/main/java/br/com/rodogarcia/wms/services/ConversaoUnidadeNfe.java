package br.com.rodogarcia.wms.services;

import java.math.BigDecimal;

/** Conversões exatas de massa; nunca deduz embalagem ou número de unidades físicas. */
public final class ConversaoUnidadeNfe {
    private ConversaoUnidadeNfe() {}

    public static BigDecimal fator(String origem, String destino) {
        String de = CadastroSupport.codigo(origem);
        String para = CadastroSupport.codigo(destino);
        if (de.equals(para)) return BigDecimal.ONE;
        if (tonelada(de) && "KG".equals(para)) return new BigDecimal("1000");
        if ("KG".equals(de) && tonelada(para)) return new BigDecimal("0.001");
        if (tonelada(de) && tonelada(para)) return BigDecimal.ONE;
        throw CadastroSupport.invalido(
                "Unidades incompatíveis. A conversão automática aceita somente massa em TO/T/TON e KG.");
    }

    private static boolean tonelada(String unidade) {
        return java.util.Set.of("TO", "T", "TON").contains(unidade);
    }

    public static BigDecimal converter(BigDecimal quantidade, String origem, String destino) {
        return quantidade.multiply(fator(origem, destino));
    }
}
