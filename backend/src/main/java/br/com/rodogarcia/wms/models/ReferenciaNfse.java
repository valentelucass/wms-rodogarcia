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
import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Nationalized;
import org.hibernate.type.SqlTypes;

@Entity
@Table(
        name = "referencia_nfse",
        schema = "wms",
        uniqueConstraints = {
            @UniqueConstraint(
                    name = "uk_referencia_nfse_2",
                    columnNames = {"emissor_documento", "referencia_externa"})
        })
public class ReferenciaNfse {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "versao_id", nullable = false, updatable = false)
    private VersaoFechamento versao;

    @Column(name = "emissor_documento", nullable = false, updatable = false, length = 20)
    private String emissorDocumento;

    @Column(name = "referencia_externa", nullable = false, updatable = false, length = 200)
    private String referenciaExterna;

    @Column(name = "numero", nullable = true, updatable = false, length = 80)
    private String numero;

    @Column(name = "serie", nullable = true, updatable = false, length = 20)
    private String serie;

    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name = "emitida_em", nullable = false, updatable = false)
    private Instant emitidaEm;

    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name = "registrada_em", nullable = false, updatable = false)
    private Instant registradaEm;

    @Nationalized
    @Column(name = "fonte", nullable = false, updatable = false, length = 500)
    private String fonte;

    @Nationalized
    @Column(name = "conferida_por", nullable = false, updatable = false, length = 200)
    private String conferidaPor;

    @Nationalized
    @Column(name = "usuario", nullable = false, updatable = false, length = 200)
    private String usuario;

    @Nationalized
    @Column(name = "motivo", nullable = false, updatable = false, length = 500)
    private String motivo;

    protected ReferenciaNfse() {}

    public ReferenciaNfse(
            VersaoFechamento versao,
            String emissorDocumento,
            String referenciaExterna,
            String numero,
            String serie,
            Instant emitidaEm,
            Instant registradaEm,
            String fonte,
            String conferidaPor,
            String usuario,
            String motivo) {
        this.versao = versao;
        this.emissorDocumento = emissorDocumento;
        this.referenciaExterna = referenciaExterna;
        this.numero = numero;
        this.serie = serie;
        this.emitidaEm =
                emitidaEm == null
                        ? null
                        : emitidaEm.truncatedTo(java.time.temporal.ChronoUnit.MICROS);
        this.registradaEm =
                registradaEm == null
                        ? null
                        : registradaEm.truncatedTo(java.time.temporal.ChronoUnit.MICROS);
        this.fonte = fonte;
        this.conferidaPor = conferidaPor;
        this.usuario = usuario;
        this.motivo = motivo;
    }

    public Long getId() {
        return id;
    }

    public VersaoFechamento getVersao() {
        return versao;
    }

    public String getEmissorDocumento() {
        return emissorDocumento;
    }

    public String getReferenciaExterna() {
        return referenciaExterna;
    }

    public String getNumero() {
        return numero;
    }

    public String getSerie() {
        return serie;
    }

    public Instant getEmitidaEm() {
        return emitidaEm;
    }

    public Instant getRegistradaEm() {
        return registradaEm;
    }

    public String getFonte() {
        return fonte;
    }

    public String getConferidaPor() {
        return conferidaPor;
    }

    public String getUsuario() {
        return usuario;
    }

    public String getMotivo() {
        return motivo;
    }
}
