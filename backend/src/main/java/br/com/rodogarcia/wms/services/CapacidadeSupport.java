package br.com.rodogarcia.wms.services;

import br.com.rodogarcia.wms.exceptions.RegraNegocioException;
import br.com.rodogarcia.wms.models.Endereco;
import br.com.rodogarcia.wms.models.MedidasUnidade;
import java.math.BigDecimal;
import java.util.ArrayList;

final class CapacidadeSupport {
    private CapacidadeSupport() {}

    static void configurado(Endereco e) {
        var faltantes = new ArrayList<String>();
        if (e.getTipoUnidadePermitido() == null)
            faltantes.add("tipo de unidade permitido (bobina, pallet ou volume)");
        if (e.getCapacidadePesoKg() == null) faltantes.add("capacidade de peso (kg)");
        if (e.getAlturaMetros() == null) faltantes.add("altura (m)");
        if (e.getLarguraMetros() == null) faltantes.add("largura (m)");
        if (e.getProfundidadeMetros() == null) faltantes.add("profundidade (m)");
        if (e.getEmpilhamentoMaximo() == null) faltantes.add("empilhamento máximo");
        if (!faltantes.isEmpty())
            throw RegraNegocioException.conflito(
                    "CAPACIDADE_NAO_CONFIGURADA",
                    "A posição "
                            + e.getCodigo()
                            + " está sem: "
                            + String.join(", ", faltantes)
                            + ". Peça ao Gestor para completar em Cadastros > Endereços > Editar."
                            + " Para ocupar uma única posição, não é necessário criar conjunto.");
    }

    static void conferir(
            MedidasUnidade m,
            BigDecimal peso,
            BigDecimal altura,
            BigDecimal largura,
            BigDecimal profundidade,
            int empilhamento) {
        if (m.getPesoKg().compareTo(peso) > 0
                || m.getAlturaMetros().compareTo(altura) > 0
                || m.getLarguraMetros().compareTo(largura) > 0
                || m.getProfundidadeMetros().compareTo(profundidade) > 0
                || m.getEmpilhamento() > empilhamento)
            throw RegraNegocioException.conflito(
                    "CAPACIDADE_EXCEDIDA",
                    "Peso, dimensões ou empilhamento excedem os limites configurados do destino.");
    }
}
