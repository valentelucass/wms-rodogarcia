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
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Nationalized;
import org.hibernate.type.SqlTypes;

@Entity
@Table(
        name = "referencia_fiscal_produto",
        schema = "wms",
        uniqueConstraints = {
            @UniqueConstraint(
                    name = "uk_referencia_fiscal_produto_1",
                    columnNames = {"produto_id", "armazem_id", "operacao"})
        })
public class ReferenciaFiscalProduto {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    @Column(nullable = false)
    private long versao;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "produto_id", nullable = false, updatable = false)
    private Produto produto;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "armazem_id", nullable = false, updatable = false)
    private Armazem armazem;

    @Column(name = "operacao", nullable = false, length = 40, updatable = false)
    private String operacao;

    @Column(name = "ncm", nullable = true, length = 8)
    private String ncm;

    @Column(name = "cfop", nullable = true, length = 4)
    private String cfop;

    @Column(name = "cest", nullable = true, length = 7)
    private String cest;

    @Nationalized
    @Column(name = "enquadramento", nullable = true, length = 500)
    private String enquadramento;

    @Column(name = "aliquota_icms", nullable = true, precision = 9, scale = 6)
    private BigDecimal aliquotaIcms;

    @Column(name = "aliquota_ipi", nullable = true, precision = 9, scale = 6)
    private BigDecimal aliquotaIpi;

    @Nationalized
    @Column(name = "fonte", nullable = false, length = 200)
    private String fonte;

    @Nationalized
    @Column(name = "conferida_por", nullable = true, length = 200)
    private String conferidaPor;

    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name = "conferida_em", nullable = true)
    private Instant conferidaEm;

    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name = "criada_em", nullable = false, updatable = false)
    private Instant criadaEm;

    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name = "alterada_em", nullable = false)
    private Instant alteradaEm;

    protected ReferenciaFiscalProduto() {}

    public ReferenciaFiscalProduto(
            Produto produto,
            Armazem armazem,
            String operacao,
            String ncm,
            String cfop,
            String cest,
            String enquadramento,
            BigDecimal aliquotaIcms,
            BigDecimal aliquotaIpi,
            String fonte,
            String conferidaPor,
            Instant conferidaEm,
            Instant criadaEm,
            Instant alteradaEm) {
        this.produto = produto;
        this.armazem = armazem;
        this.operacao = operacao;
        this.ncm = ncm;
        this.cfop = cfop;
        this.cest = cest;
        this.enquadramento = enquadramento;
        this.aliquotaIcms = aliquotaIcms;
        this.aliquotaIpi = aliquotaIpi;
        this.fonte = fonte;
        this.conferidaPor = conferidaPor;
        this.conferidaEm =
                conferidaEm == null
                        ? null
                        : conferidaEm.truncatedTo(java.time.temporal.ChronoUnit.MICROS);
        this.criadaEm =
                criadaEm == null
                        ? null
                        : criadaEm.truncatedTo(java.time.temporal.ChronoUnit.MICROS);
        this.alteradaEm =
                alteradaEm == null
                        ? null
                        : alteradaEm.truncatedTo(java.time.temporal.ChronoUnit.MICROS);
    }

    public Long getId() {
        return id;
    }

    public long getVersao() {
        return versao;
    }

    public Produto getProduto() {
        return produto;
    }

    public Armazem getArmazem() {
        return armazem;
    }

    public String getOperacao() {
        return operacao;
    }

    public String getNcm() {
        return ncm;
    }

    public String getCfop() {
        return cfop;
    }

    public String getCest() {
        return cest;
    }

    public String getEnquadramento() {
        return enquadramento;
    }

    public BigDecimal getAliquotaIcms() {
        return aliquotaIcms;
    }

    public BigDecimal getAliquotaIpi() {
        return aliquotaIpi;
    }

    public String getFonte() {
        return fonte;
    }

    public String getConferidaPor() {
        return conferidaPor;
    }

    public Instant getConferidaEm() {
        return conferidaEm;
    }

    public Instant getCriadaEm() {
        return criadaEm;
    }

    public Instant getAlteradaEm() {
        return alteradaEm;
    }

    public void revisar(
            String ncm,
            String cfop,
            String cest,
            String enquadramento,
            BigDecimal icms,
            BigDecimal ipi,
            String fonte,
            String por,
            Instant em,
            Instant agora) {
        this.ncm = ncm;
        this.cfop = cfop;
        this.cest = cest;
        this.enquadramento = enquadramento;
        aliquotaIcms = icms;
        aliquotaIpi = ipi;
        this.fonte = fonte;
        conferidaPor = por;
        conferidaEm = em == null ? null : em.truncatedTo(java.time.temporal.ChronoUnit.MICROS);
        alteradaEm = agora.truncatedTo(java.time.temporal.ChronoUnit.MICROS);
    }
}
