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
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Nationalized;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "calculo_cobranca", schema = "wms")
public class CalculoCobranca {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cliente_id", nullable = false, updatable = false)
    private Cliente cliente;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "armazem_id", nullable = false, updatable = false)
    private Armazem armazem;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "contrato_id", nullable = true, updatable = false)
    private ContratoCobranca contrato;

    @Column(name = "periodo_inicio", nullable = false, updatable = false)
    private LocalDate periodoInicio;

    @Column(name = "periodo_fim", nullable = false, updatable = false)
    private LocalDate periodoFim;

    @Column(name = "fuso", nullable = true, length = 60, updatable = false)
    private String fuso;

    @Column(name = "moeda", nullable = true, length = 3, updatable = false)
    private String moeda;

    @Column(name = "situacao", nullable = false, length = 16, updatable = false)
    private String situacao;

    @Column(name = "regra_versao", nullable = false, updatable = false)
    private int regraVersao;

    @Column(name = "entradas_hash", nullable = false, length = 64, updatable = false)
    private String entradasHash;

    @Nationalized
    @Column(
            name = "memoria_json",
            nullable = false,
            columnDefinition = "nvarchar(max)",
            updatable = false)
    private String memoriaJson;

    @Nationalized
    @Column(
            name = "pendencias_json",
            nullable = false,
            columnDefinition = "nvarchar(max)",
            updatable = false)
    private String pendenciasJson;

    @Column(
            name = "subtotal_conhecido",
            nullable = false,
            precision = 19,
            scale = 2,
            updatable = false)
    private BigDecimal subtotalConhecido;

    @Column(
            name = "minimo_calculado",
            nullable = true,
            precision = 19,
            scale = 2,
            updatable = false)
    private BigDecimal minimoCalculado;

    @Column(name = "gris_calculado", nullable = true, precision = 19, scale = 2, updatable = false)
    private BigDecimal grisCalculado;

    @Column(name = "total", nullable = true, precision = 19, scale = 2, updatable = false)
    private BigDecimal total;

    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name = "calculado_em", nullable = false, updatable = false)
    private Instant calculadoEm;

    @Nationalized
    @Column(name = "usuario", nullable = false, length = 200, updatable = false)
    private String usuario;

    protected CalculoCobranca() {}

    public CalculoCobranca(
            Cliente cliente,
            Armazem armazem,
            ContratoCobranca contrato,
            LocalDate periodoInicio,
            LocalDate periodoFim,
            String fuso,
            String moeda,
            String situacao,
            int regraVersao,
            String entradasHash,
            String memoriaJson,
            String pendenciasJson,
            BigDecimal subtotalConhecido,
            BigDecimal minimoCalculado,
            BigDecimal grisCalculado,
            BigDecimal total,
            Instant calculadoEm,
            String usuario) {
        this.cliente = cliente;
        this.armazem = armazem;
        this.contrato = contrato;
        this.periodoInicio = periodoInicio;
        this.periodoFim = periodoFim;
        this.fuso = fuso;
        this.moeda = moeda;
        this.situacao = situacao;
        this.regraVersao = regraVersao;
        this.entradasHash = entradasHash;
        this.memoriaJson = memoriaJson;
        this.pendenciasJson = pendenciasJson;
        this.subtotalConhecido = subtotalConhecido;
        this.minimoCalculado = minimoCalculado;
        this.grisCalculado = grisCalculado;
        this.total = total;
        this.calculadoEm =
                calculadoEm == null
                        ? null
                        : calculadoEm.truncatedTo(java.time.temporal.ChronoUnit.MICROS);
        this.usuario = usuario;
    }

    public Long getId() {
        return id;
    }

    public Cliente getCliente() {
        return cliente;
    }

    public Armazem getArmazem() {
        return armazem;
    }

    public ContratoCobranca getContrato() {
        return contrato;
    }

    public LocalDate getPeriodoInicio() {
        return periodoInicio;
    }

    public LocalDate getPeriodoFim() {
        return periodoFim;
    }

    public String getFuso() {
        return fuso;
    }

    public String getMoeda() {
        return moeda;
    }

    public String getSituacao() {
        return situacao;
    }

    public int getRegraVersao() {
        return regraVersao;
    }

    public String getEntradasHash() {
        return entradasHash;
    }

    public String getMemoriaJson() {
        return memoriaJson;
    }

    public String getPendenciasJson() {
        return pendenciasJson;
    }

    public BigDecimal getSubtotalConhecido() {
        return subtotalConhecido;
    }

    public BigDecimal getMinimoCalculado() {
        return minimoCalculado;
    }

    public BigDecimal getGrisCalculado() {
        return grisCalculado;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public Instant getCalculadoEm() {
        return calculadoEm;
    }

    public String getUsuario() {
        return usuario;
    }
}
