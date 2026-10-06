package br.com.rodogarcia.wms.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(
        name = "conjunto_posicoes",
        schema = "wms",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uk_conjunto_codigo",
                        columnNames = {"armazem_id", "codigo"}))
public class ConjuntoPosicoes extends CadastroBase {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "armazem_id", nullable = false, updatable = false)
    private Armazem armazem;

    @Column(nullable = false, length = 40, updatable = false)
    private String codigo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "endereco_a_id", nullable = false, updatable = false)
    private Endereco enderecoA;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "endereco_b_id", nullable = false, updatable = false)
    private Endereco enderecoB;

    @Column(
            name = "capacidade_peso_kg",
            nullable = false,
            precision = 19,
            scale = 3,
            updatable = false)
    private BigDecimal capacidadePesoKg;

    @Column(name = "altura_metros", nullable = false, precision = 10, scale = 3, updatable = false)
    private BigDecimal alturaMetros;

    @Column(name = "largura_metros", nullable = false, precision = 10, scale = 3, updatable = false)
    private BigDecimal larguraMetros;

    @Column(
            name = "profundidade_metros",
            nullable = false,
            precision = 10,
            scale = 3,
            updatable = false)
    private BigDecimal profundidadeMetros;

    @Column(name = "empilhamento_maximo", nullable = false, updatable = false)
    private int empilhamentoMaximo;

    protected ConjuntoPosicoes() {}

    public ConjuntoPosicoes(
            Armazem armazem,
            String codigo,
            Endereco enderecoA,
            Endereco enderecoB,
            BigDecimal peso,
            BigDecimal altura,
            BigDecimal largura,
            BigDecimal profundidade,
            int empilhamento,
            Instant instante) {
        super(instante);
        this.armazem = armazem;
        this.codigo = codigo;
        this.enderecoA = enderecoA;
        this.enderecoB = enderecoB;
        this.capacidadePesoKg = peso;
        this.alturaMetros = altura;
        this.larguraMetros = largura;
        this.profundidadeMetros = profundidade;
        this.empilhamentoMaximo = empilhamento;
    }

    public Armazem getArmazem() {
        return armazem;
    }

    public String getCodigo() {
        return codigo;
    }

    public Endereco getEnderecoA() {
        return enderecoA;
    }

    public Endereco getEnderecoB() {
        return enderecoB;
    }

    public BigDecimal getCapacidadePesoKg() {
        return capacidadePesoKg;
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

    public int getEmpilhamentoMaximo() {
        return empilhamentoMaximo;
    }
}
