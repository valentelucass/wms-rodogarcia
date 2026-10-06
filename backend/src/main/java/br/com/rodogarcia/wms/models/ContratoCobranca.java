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
import java.time.LocalDate;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(
        name = "contrato_cobranca",
        schema = "wms",
        uniqueConstraints = {
            @UniqueConstraint(
                    name = "uk_contrato_cobranca_1",
                    columnNames = {"cliente_id", "armazem_id", "vigencia_inicio"})
        })
public class ContratoCobranca {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    @Column(nullable = false)
    private long versao;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cliente_id", nullable = false, updatable = false)
    private Cliente cliente;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "armazem_id", nullable = false, updatable = false)
    private Armazem armazem;

    @Column(name = "vigencia_inicio", nullable = false, updatable = false)
    private LocalDate vigenciaInicio;

    @Column(name = "vigencia_fim", nullable = true)
    private LocalDate vigenciaFim;

    @Column(name = "fuso", nullable = false, length = 60, updatable = false)
    private String fuso;

    @Column(name = "moeda", nullable = false, length = 3, updatable = false)
    private String moeda;

    @Column(name = "modalidade_ciclo", nullable = false, length = 20, updatable = false)
    private String modalidadeCiclo;

    @Column(name = "dia_corte", nullable = true, updatable = false)
    private Integer diaCorte;

    @Column(name = "duracao_dias", nullable = true, updatable = false)
    private Integer duracaoDias;

    @Column(name = "minimo_modo", nullable = false, length = 16, updatable = false)
    private String minimoModo;

    @Column(name = "gris_modo", nullable = false, length = 16, updatable = false)
    private String grisModo;

    @Column(name = "minimo_valor", nullable = true, precision = 19, scale = 2, updatable = false)
    private BigDecimal minimoValor;

    @Column(name = "minimo_proporcao", nullable = true, length = 20, updatable = false)
    private String minimoProporcao;

    @Column(name = "gris_percentual", nullable = true, precision = 9, scale = 6, updatable = false)
    private BigDecimal grisPercentual;

    @Column(name = "gris_base", nullable = true, length = 24, updatable = false)
    private String grisBase;

    @Column(name = "gris_periodicidade", nullable = true, length = 16, updatable = false)
    private String grisPeriodicidade;

    @Column(name = "gris_proporcao", nullable = true, length = 20, updatable = false)
    private String grisProporcao;

    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name = "criada_em", nullable = false, updatable = false)
    private Instant criadaEm;

    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name = "alterada_em", nullable = false)
    private Instant alteradaEm;

    protected ContratoCobranca() {}

    public ContratoCobranca(
            Cliente cliente,
            Armazem armazem,
            LocalDate vigenciaInicio,
            LocalDate vigenciaFim,
            String fuso,
            String moeda,
            String modalidadeCiclo,
            Integer diaCorte,
            Integer duracaoDias,
            String minimoModo,
            String grisModo,
            BigDecimal minimoValor,
            String minimoProporcao,
            BigDecimal grisPercentual,
            String grisBase,
            String grisPeriodicidade,
            String grisProporcao,
            Instant criadaEm,
            Instant alteradaEm) {
        this.cliente = cliente;
        this.armazem = armazem;
        this.vigenciaInicio = vigenciaInicio;
        this.vigenciaFim = vigenciaFim;
        this.fuso = fuso;
        this.moeda = moeda;
        this.modalidadeCiclo = modalidadeCiclo;
        this.diaCorte = diaCorte;
        this.duracaoDias = duracaoDias;
        this.minimoModo = minimoModo;
        this.grisModo = grisModo;
        this.minimoValor = minimoValor;
        this.minimoProporcao = minimoProporcao;
        this.grisPercentual = grisPercentual;
        this.grisBase = grisBase;
        this.grisPeriodicidade = grisPeriodicidade;
        this.grisProporcao = grisProporcao;
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

    public Cliente getCliente() {
        return cliente;
    }

    public Armazem getArmazem() {
        return armazem;
    }

    public LocalDate getVigenciaInicio() {
        return vigenciaInicio;
    }

    public LocalDate getVigenciaFim() {
        return vigenciaFim;
    }

    public String getFuso() {
        return fuso;
    }

    public String getMoeda() {
        return moeda;
    }

    public String getModalidadeCiclo() {
        return modalidadeCiclo;
    }

    public Integer getDiaCorte() {
        return diaCorte;
    }

    public Integer getDuracaoDias() {
        return duracaoDias;
    }

    public String getMinimoModo() {
        return minimoModo;
    }

    public String getGrisModo() {
        return grisModo;
    }

    public BigDecimal getMinimoValor() {
        return minimoValor;
    }

    public String getMinimoProporcao() {
        return minimoProporcao;
    }

    public BigDecimal getGrisPercentual() {
        return grisPercentual;
    }

    public String getGrisBase() {
        return grisBase;
    }

    public String getGrisPeriodicidade() {
        return grisPeriodicidade;
    }

    public String getGrisProporcao() {
        return grisProporcao;
    }

    public Instant getCriadaEm() {
        return criadaEm;
    }

    public Instant getAlteradaEm() {
        return alteradaEm;
    }

    public void encerrar(LocalDate fim, Instant agora) {
        vigenciaFim = fim;
        alteradaEm = agora.truncatedTo(java.time.temporal.ChronoUnit.MICROS);
    }
}
