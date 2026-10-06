package br.com.rodogarcia.wms.models;

import jakarta.persistence.CheckConstraint;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Nationalized;
import org.hibernate.type.SqlTypes;

@Entity
@Table(
        name = "tratativa_externa_fechamento",
        schema = "wms",
        check =
                @CheckConstraint(
                        constraint =
                                "(versao_base_anterior_id is null and dependencias_origem_json is null) or (versao_base_anterior_id is not null and dependencias_origem_json is not null)"))
public class TratativaExternaFechamento {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "fechamento_id", nullable = false, updatable = false)
    private FechamentoCobranca fechamento;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "versao_resultado_id", nullable = false, updatable = false)
    private VersaoFechamento versaoResultado;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "versao_base_anterior_id", updatable = false)
    private VersaoFechamento versaoBaseAnterior;

    @Nationalized
    @Column(
            name = "dependencias_origem_json",
            updatable = false,
            columnDefinition = "nvarchar(max)")
    private String dependenciasOrigemJson;

    @Column(name = "resultado", nullable = false, updatable = false, length = 24)
    private String resultado;

    @Nationalized
    @Column(
            name = "referencias_json",
            nullable = false,
            updatable = false,
            columnDefinition = "nvarchar(max)")
    private String referenciasJson;

    @Nationalized
    @Column(name = "fonte", nullable = false, updatable = false, length = 500)
    private String fonte;

    @Nationalized
    @Column(name = "conferida_por", nullable = false, updatable = false, length = 200)
    private String conferidaPor;

    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name = "conferida_em", nullable = false, updatable = false)
    private Instant conferidaEm;

    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name = "registrada_em", nullable = false, updatable = false)
    private Instant registradaEm;

    @Nationalized
    @Column(name = "usuario", nullable = false, updatable = false, length = 200)
    private String usuario;

    @Nationalized
    @Column(name = "motivo", nullable = false, updatable = false, length = 500)
    private String motivo;

    protected TratativaExternaFechamento() {}

    public TratativaExternaFechamento(
            FechamentoCobranca fechamento,
            VersaoFechamento versaoResultado,
            String resultado,
            String referenciasJson,
            String fonte,
            String conferidaPor,
            Instant conferidaEm,
            Instant registradaEm,
            String usuario,
            String motivo) {
        this.fechamento = fechamento;
        this.versaoResultado = versaoResultado;
        this.resultado = resultado;
        this.referenciasJson = referenciasJson;
        this.fonte = fonte;
        this.conferidaPor = conferidaPor;
        this.conferidaEm = conferidaEm.truncatedTo(java.time.temporal.ChronoUnit.MICROS);
        this.registradaEm = registradaEm.truncatedTo(java.time.temporal.ChronoUnit.MICROS);
        this.usuario = usuario;
        this.motivo = motivo;
    }

    public Long getId() {
        return id;
    }

    public void identificarBaseAnterior(VersaoFechamento anterior, String dependencias) {
        if (id != null
                || versaoBaseAnterior != null
                || dependencias == null
                || !anterior.getFechamento().getId().equals(fechamento.getId())
                || anterior.getId().equals(versaoResultado.getId()))
            throw new IllegalStateException("Base anterior da tratativa divergente");
        versaoBaseAnterior = anterior;
        dependenciasOrigemJson = dependencias;
    }

    public VersaoFechamento getVersaoBaseAnterior() {
        return versaoBaseAnterior;
    }

    public String getDependenciasOrigemJson() {
        return dependenciasOrigemJson;
    }

    public FechamentoCobranca getFechamento() {
        return fechamento;
    }

    public VersaoFechamento getVersaoResultado() {
        return versaoResultado;
    }

    public String getResultado() {
        return resultado;
    }

    public String getReferenciasJson() {
        return referenciasJson;
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

    public Instant getRegistradaEm() {
        return registradaEm;
    }

    public String getUsuario() {
        return usuario;
    }

    public String getMotivo() {
        return motivo;
    }
}
