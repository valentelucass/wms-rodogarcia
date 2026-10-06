package br.com.rodogarcia.wms.models;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.math.BigDecimal;

@Embeddable
public class MedidasUnidade {
    @Column(name = "peso_kg", precision = 19, scale = 3)
    private BigDecimal pesoKg;

    @Column(name = "altura_metros", precision = 10, scale = 3)
    private BigDecimal alturaMetros;

    @Column(name = "largura_metros", precision = 10, scale = 3)
    private BigDecimal larguraMetros;

    @Column(name = "profundidade_metros", precision = 10, scale = 3)
    private BigDecimal profundidadeMetros;

    @Column(name = "empilhamento")
    private Integer empilhamento;

    @Column(name = "posicoes_necessarias")
    private Integer posicoesNecessarias;

    protected MedidasUnidade() {}

    public MedidasUnidade(
            BigDecimal pesoKg,
            BigDecimal alturaMetros,
            BigDecimal larguraMetros,
            BigDecimal profundidadeMetros,
            Integer empilhamento,
            Integer posicoesNecessarias) {
        this.pesoKg = pesoKg;
        this.alturaMetros = alturaMetros;
        this.larguraMetros = larguraMetros;
        this.profundidadeMetros = profundidadeMetros;
        this.empilhamento = empilhamento;
        this.posicoesNecessarias = posicoesNecessarias;
    }

    public BigDecimal getPesoKg() {
        return pesoKg;
    }

    public BigDecimal getAlturaMetros() {
        return alturaMetros;
    }

    public BigDecimal getLarguraMetros() {
        return larguraMetros;
    }

    public BigDecimal getProfundidadeMetros() {
        return profundidadeMetros;
    }

    public Integer getEmpilhamento() {
        return empilhamento;
    }

    public Integer getPosicoesNecessarias() {
        return posicoesNecessarias;
    }
}
