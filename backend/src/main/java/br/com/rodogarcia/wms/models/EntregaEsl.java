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
        name = "entrega_esl",
        schema = "wms",
        uniqueConstraints = {
            @UniqueConstraint(
                    name = "uk_entrega_esl_1",
                    columnNames = {"versao_id", "sequencia"})
        })
public class EntregaEsl {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "versao_id", nullable = false, updatable = false)
    private VersaoFechamento versao;

    @Column(name = "sequencia", nullable = false, updatable = false)
    private int sequencia;

    @Column(name = "layout_versao", nullable = false, updatable = false)
    private int layoutVersao;

    @Column(name = "arquivo_hash", nullable = false, updatable = false, length = 64)
    private String arquivoHash;

    @Nationalized
    @Column(name = "destino_referencia", nullable = false, updatable = false, length = 200)
    private String destinoReferencia;

    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name = "entregue_em", nullable = false, updatable = false)
    private Instant entregueEm;

    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name = "registrada_em", nullable = false, updatable = false)
    private Instant registradaEm;

    @Nationalized
    @Column(name = "usuario", nullable = false, updatable = false, length = 200)
    private String usuario;

    @Nationalized
    @Column(name = "motivo", nullable = false, updatable = false, length = 500)
    private String motivo;

    protected EntregaEsl() {}

    public EntregaEsl(
            VersaoFechamento versao,
            int sequencia,
            int layoutVersao,
            String arquivoHash,
            String destinoReferencia,
            Instant entregueEm,
            Instant registradaEm,
            String usuario,
            String motivo) {
        this.versao = versao;
        this.sequencia = sequencia;
        this.layoutVersao = layoutVersao;
        this.arquivoHash = arquivoHash;
        this.destinoReferencia = destinoReferencia;
        this.entregueEm =
                entregueEm == null
                        ? null
                        : entregueEm.truncatedTo(java.time.temporal.ChronoUnit.MICROS);
        this.registradaEm =
                registradaEm == null
                        ? null
                        : registradaEm.truncatedTo(java.time.temporal.ChronoUnit.MICROS);
        this.usuario = usuario;
        this.motivo = motivo;
    }

    public Long getId() {
        return id;
    }

    public VersaoFechamento getVersao() {
        return versao;
    }

    public int getSequencia() {
        return sequencia;
    }

    public int getLayoutVersao() {
        return layoutVersao;
    }

    public String getArquivoHash() {
        return arquivoHash;
    }

    public String getDestinoReferencia() {
        return destinoReferencia;
    }

    public Instant getEntregueEm() {
        return entregueEm;
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
