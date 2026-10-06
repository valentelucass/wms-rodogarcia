package br.com.rodogarcia.wms.services;

import br.com.rodogarcia.wms.exceptions.RegraNegocioException;
import br.com.rodogarcia.wms.models.Endereco;
import br.com.rodogarcia.wms.models.MedidasUnidade;
import java.math.BigDecimal;

final class CapacidadeSupport {
    private CapacidadeSupport() {}

    static void configurado(Endereco e) {
        if (e.getTipoUnidadePermitido() == null
                || e.getCapacidadePesoKg() == null
                || e.getAlturaMetros() == null
                || e.getLarguraMetros() == null
                || e.getProfundidadeMetros() == null
                || e.getEmpilhamentoMaximo() == null)
            throw RegraNegocioException.conflito(
                    "CAPACIDADE_NAO_CONFIGURADA",
                    "Configure peso, dimensões, empilhamento e tipo de unidade do endereço.");
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
