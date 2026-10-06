package br.com.rodogarcia.wms.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.Instant;
import org.hibernate.annotations.Nationalized;

@Entity
@Table(
        name = "endereco",
        schema = "wms",
        uniqueConstraints = {
            @UniqueConstraint(
                    name = "uk_endereco_1",
                    columnNames = {"armazem_id", "codigo"}),
            @UniqueConstraint(
                    name = "uk_endereco_2",
                    columnNames = {"armazem_id", "rua", "nivel", "posicao"})
        })
public class Endereco extends CadastroBase {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "armazem_id", nullable = false, updatable = false)
    private Armazem armazem;

    @Column(nullable = false, length = 40, updatable = false)
    private String codigo;

    @Column(nullable = false, length = 20, updatable = false)
    private String rua;

    @Column(nullable = false, updatable = false)
    private int nivel;

    @Column(nullable = false, length = 20, updatable = false)
    private String posicao;

    @Nationalized
    @Column(nullable = false, length = 160)
    private String descricao;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16, updatable = false)
    private TipoEndereco tipo;

    @Column(name = "capacidade_peso_kg", precision = 19, scale = 3)
    private BigDecimal capacidadePesoKg;

    @Column(name = "altura_metros", precision = 10, scale = 3)
    private BigDecimal alturaMetros;

    @Column(name = "largura_metros", precision = 10, scale = 3)
    private BigDecimal larguraMetros;

    @Column(name = "profundidade_metros", precision = 10, scale = 3)
    private BigDecimal profundidadeMetros;

    @Column(name = "empilhamento_maximo")
    private Integer empilhamentoMaximo;

    @Column(name = "sequencia_coleta", nullable = false, updatable = false)
    private int sequenciaColeta;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_unidade_permitido", length = 16)
    private TipoUnidadeLogistica tipoUnidadePermitido;

    protected Endereco() {}

    public Endereco(
            Armazem armazem,
            String codigo,
            String rua,
            int nivel,
            String posicao,
            String descricao,
            TipoEndereco tipo,
            BigDecimal capacidadePesoKg,
            BigDecimal alturaMetros,
            BigDecimal larguraMetros,
            BigDecimal profundidadeMetros,
            Integer empilhamentoMaximo,
            int sequenciaColeta,
            Instant instante) {
        super(instante);
        this.armazem = armazem;
        this.codigo = codigo;
        this.rua = rua;
        this.nivel = nivel;
        this.posicao = posicao;
        this.descricao = descricao;
        this.tipo = tipo;
        this.capacidadePesoKg = capacidadePesoKg;
        this.alturaMetros = alturaMetros;
        this.larguraMetros = larguraMetros;
        this.profundidadeMetros = profundidadeMetros;
        this.empilhamentoMaximo = empilhamentoMaximo;
        this.sequenciaColeta = sequenciaColeta;
    }

    public Armazem getArmazem() {
        return armazem;
    }

    public String getCodigo() {
        return codigo;
    }

    public String getRua() {
        return rua;
    }

    public int getNivel() {
        return nivel;
    }

    public String getPosicao() {
        return posicao;
    }

    public String getDescricao() {
        return descricao;
    }

    public TipoEndereco getTipo() {
        return tipo;
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

    public Integer getEmpilhamentoMaximo() {
        return empilhamentoMaximo;
    }

    public int getSequenciaColeta() {
        return sequenciaColeta;
    }

    public void alterarDescricao(String descricao, Instant instante) {
        this.descricao = descricao;
        registrarAlteracao(instante);
    }

    public TipoUnidadeLogistica getTipoUnidadePermitido() {
        return tipoUnidadePermitido;
    }

    public void configurarFisico(
            TipoUnidadeLogistica tipo,
            BigDecimal peso,
            BigDecimal altura,
            BigDecimal largura,
            BigDecimal profundidade,
            int empilhamento,
            Instant instante) {
        tipoUnidadePermitido = tipo;
        capacidadePesoKg = peso;
        alturaMetros = altura;
        larguraMetros = largura;
        profundidadeMetros = profundidade;
        empilhamentoMaximo = empilhamento;
        registrarAlteracao(
                instante.isAfter(getAlteradoEm()) ? instante : getAlteradoEm().plusNanos(1000));
    }
}
