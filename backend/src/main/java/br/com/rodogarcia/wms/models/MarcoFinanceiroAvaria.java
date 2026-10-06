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
import org.hibernate.annotations.Nationalized;
import org.hibernate.type.SqlTypes;

@Entity
@Table(
        name = "marco_financeiro_avaria",
        schema = "wms",
        uniqueConstraints = {
            @UniqueConstraint(
                    name = "uk_marco_financeiro_avaria_1",
                    columnNames = {"avaria_id", "fato_permanencia_id"})
        })
public class MarcoFinanceiroAvaria {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "avaria_id", nullable = false, updatable = false)
    private AvariaEstoque avaria;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "fato_permanencia_id", nullable = false, updatable = false)
    private FatoPermanencia fatoPermanencia;

    @Column(
            name = "quantidade_afetada",
            nullable = false,
            precision = 19,
            scale = 6,
            updatable = false)
    private BigDecimal quantidadeAfetada;

    @Column(
            name = "quantidade_base",
            nullable = false,
            precision = 19,
            scale = 6,
            updatable = false)
    private BigDecimal quantidadeBase;

    @Column(
            name = "equivalencia_base",
            nullable = false,
            precision = 19,
            scale = 6,
            updatable = false)
    private BigDecimal equivalenciaBase;

    @Nationalized
    @Column(name = "usuario", nullable = false, length = 200, updatable = false)
    private String usuario;

    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name = "validado_em", nullable = false, updatable = false)
    private Instant validadoEm;

    @Nationalized
    @Column(name = "motivo", nullable = false, length = 500, updatable = false)
    private String motivo;

    protected MarcoFinanceiroAvaria() {}

    public MarcoFinanceiroAvaria(
            AvariaEstoque avaria,
            FatoPermanencia fatoPermanencia,
            BigDecimal quantidadeAfetada,
            BigDecimal quantidadeBase,
            BigDecimal equivalenciaBase,
            String usuario,
            Instant validadoEm,
            String motivo) {
        this.avaria = avaria;
        this.fatoPermanencia = fatoPermanencia;
        this.quantidadeAfetada = quantidadeAfetada;
        this.quantidadeBase = quantidadeBase;
        this.equivalenciaBase = equivalenciaBase;
        this.usuario = usuario;
        this.validadoEm =
                validadoEm == null
                        ? null
                        : validadoEm.truncatedTo(java.time.temporal.ChronoUnit.MICROS);
        this.motivo = motivo;
    }

    public Long getId() {
        return id;
    }

    public AvariaEstoque getAvaria() {
        return avaria;
    }

    public FatoPermanencia getFatoPermanencia() {
        return fatoPermanencia;
    }

    public BigDecimal getQuantidadeAfetada() {
        return quantidadeAfetada;
    }

    public BigDecimal getQuantidadeBase() {
        return quantidadeBase;
    }

    public BigDecimal getEquivalenciaBase() {
        return equivalenciaBase;
    }

    public String getUsuario() {
        return usuario;
    }

    public Instant getValidadoEm() {
        return validadoEm;
    }

    public String getMotivo() {
        return motivo;
    }
}
