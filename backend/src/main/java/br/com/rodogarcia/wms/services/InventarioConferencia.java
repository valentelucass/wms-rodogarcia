package br.com.rodogarcia.wms.services;

import br.com.rodogarcia.wms.dto.InventarioDto;
import br.com.rodogarcia.wms.models.InventarioItem;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/** Conferência pura; nenhuma baixa, reserva, reendereçamento ou ajuste é produzido aqui. */
public final class InventarioConferencia {
    private InventarioConferencia() {}

    public static List<InventarioDto.Linha> conferir(
            List<InventarioItem> itens, List<InventarioItem.Base> atuais) {
        var vigentes =
                atuais.stream()
                        .collect(Collectors.toMap(InventarioItem.Base::codigoUnidade, b -> b));
        var todas = new ArrayList<>(itens);
        var presentes =
                itens.stream().map(i -> i.base().codigoUnidade()).collect(Collectors.toSet());
        // Entradas posteriores à abertura ficam explícitas e não entram silenciosamente na base.
        atuais.stream()
                .filter(b -> !presentes.contains(b.codigoUnidade()))
                .forEach(
                        b -> todas.add(new InventarioItem(b, false, null, null, null, null, null)));
        var linhas = new ArrayList<InventarioDto.Linha>();
        for (var item : todas) {
            var base = item.base();
            var esperado = item.prevista() ? base.quantidade() : BigDecimal.ZERO;
            var contado = item.contado() == null ? BigDecimal.ZERO : item.contado();
            var diferenca = contado.subtract(esperado);
            var ocorrencias = new ArrayList<String>();
            if (item.contado() == null && item.prevista()) ocorrencias.add("NAO_LIDA");
            if (diferenca.signum() != 0) ocorrencias.add("QUANTIDADE");
            if (item.contado() != null && contado.signum() > 0) {
                boolean correto =
                        base.posicoes().isEmpty()
                                ? item.enderecoLido() == null
                                : base.posicoes().stream()
                                        .anyMatch(p -> Objects.equals(p.id(), item.enderecoLido()));
                if (!correto) ocorrencias.add("ENDERECO");
            }
            var atual = vigentes.get(base.codigoUnidade());
            if (!item.prevista() && item.contado() != null) ocorrencias.add("FORA_BASE");
            if ((item.prevista() && atual == null)
                    || (!item.prevista() && atual != null)
                    || (atual != null
                            && (atual.versaoUnidade() != base.versaoUnidade()
                                    || atual.quantidade().compareTo(base.quantidade()) != 0
                                    || !atual.posicoes().equals(base.posicoes()))))
                ocorrencias.add("MOVIMENTACAO");
            linhas.add(
                    new InventarioDto.Linha(
                            base.codigoUnidade(),
                            base.produtoId(),
                            base.sku(),
                            base.descricao(),
                            base.unidadeMedida(),
                            esperado,
                            contado,
                            diferenca,
                            base.posicoes().stream()
                                    .map(InventarioItem.Posicao::codigo)
                                    .collect(Collectors.joining(" + ")),
                            item.posicaoLida(),
                            List.copyOf(ocorrencias)));
        }
        return List.copyOf(linhas);
    }

    public static List<InventarioDto.PorSku> porSku(List<InventarioDto.Linha> linhas) {
        Map<Long, List<InventarioDto.Linha>> grupos = new LinkedHashMap<>();
        linhas.forEach(l -> grupos.computeIfAbsent(l.produtoId(), k -> new ArrayList<>()).add(l));
        return grupos.values().stream()
                .map(
                        grupo -> {
                            var primeiro = grupo.getFirst();
                            var esperado =
                                    grupo.stream()
                                            .map(InventarioDto.Linha::esperado)
                                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                            var contado =
                                    grupo.stream()
                                            .map(InventarioDto.Linha::contado)
                                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                            return new InventarioDto.PorSku(
                                    primeiro.produtoId(),
                                    primeiro.sku(),
                                    primeiro.descricao(),
                                    primeiro.unidadeMedida(),
                                    esperado,
                                    contado,
                                    contado.subtract(esperado),
                                    (int)
                                            grupo.stream()
                                                    .filter(l -> !l.ocorrencias().isEmpty())
                                                    .count());
                        })
                .toList();
    }
}
