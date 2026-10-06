package br.com.rodogarcia.wms.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(
        name = "fato_permanencia",
        schema = "wms",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uk_fato_permanencia",
                        columnNames = {"unidade_id", "operacao_id", "tipo"}))
public class FatoPermanencia {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "unidade_id", nullable = false, updatable = false)
    private UnidadeLogistica unidade;

    @Column(name = "operacao_id", nullable = false, length = 36, updatable = false)
    private String operacaoId;

    @Column(nullable = false, length = 32, updatable = false)
    private String tipo;

    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name = "ocorrida_em", nullable = false, updatable = false)
    private Instant ocorridaEm;

    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name = "registrada_em", nullable = false, updatable = false)
    private Instant registradaEm;

    @Column(
            name = "quantidade_antes",
            nullable = false,
            precision = 19,
            scale = 6,
            updatable = false)
    private BigDecimal quantidadeAntes;

    @Column(
            name = "quantidade_depois",
            nullable = false,
            precision = 19,
            scale = 6,
            updatable = false)
    private BigDecimal quantidadeDepois;

    @Column(
            name = "equivalencia_antes",
            nullable = false,
            precision = 19,
            scale = 6,
            updatable = false)
    private BigDecimal equivalenciaAntes;

    @Column(
            name = "equivalencia_depois",
            nullable = false,
            precision = 19,
            scale = 6,
            updatable = false)
    private BigDecimal equivalenciaDepois;

    protected FatoPermanencia() {}

    public Long getId() {
        return id;
    }

    public FatoPermanencia(
            UnidadeLogistica unidade,
            String operacao,
            String tipo,
            Instant ocorrida,
            Instant registrada,
            BigDecimal antes,
            BigDecimal depois,
            BigDecimal equivalenciaAntes,
            BigDecimal equivalenciaDepois) {
        this.unidade = unidade;
        operacaoId = operacao;
        this.tipo = tipo;
        ocorridaEm = ocorrida.truncatedTo(java.time.temporal.ChronoUnit.MICROS);
        registradaEm = registrada;
        quantidadeAntes = antes;
        quantidadeDepois = depois;
        this.equivalenciaAntes = equivalenciaAntes;
        this.equivalenciaDepois = equivalenciaDepois;
    }

    public UnidadeLogistica getUnidade() {
        return unidade;
    }

    public String getOperacaoId() {
        return operacaoId;
    }

    public String getTipo() {
        return tipo;
    }

    public Instant getOcorridaEm() {
        return ocorridaEm;
    }

    public Instant getRegistradaEm() {
        return registradaEm;
    }

    public BigDecimal getQuantidadeAntes() {
        return quantidadeAntes;
    }

    public BigDecimal getQuantidadeDepois() {
        return quantidadeDepois;
    }

    public BigDecimal getEquivalenciaAntes() {
        return equivalenciaAntes;
    }

    public BigDecimal getEquivalenciaDepois() {
        return equivalenciaDepois;
    }
}
