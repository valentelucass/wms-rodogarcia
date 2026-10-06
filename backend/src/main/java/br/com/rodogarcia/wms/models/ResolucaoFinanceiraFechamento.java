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
        name = "resolucao_financeira_fechamento",
        schema = "wms",
        uniqueConstraints = {
            @UniqueConstraint(
                    name = "uk_resolucao_financeira_fechamento_1",
                    columnNames = {"versao_id"})
        })
public class ResolucaoFinanceiraFechamento {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "versao_id", nullable = false, updatable = false)
    private VersaoFechamento versao;

    @Column(name = "tipo", nullable = false, updatable = false, length = 24)
    private String tipo;

    @Nationalized
    @Column(name = "referencia_externa", nullable = false, updatable = false, length = 200)
    private String referenciaExterna;

    @Nationalized
    @Column(name = "confirmada_por", nullable = false, updatable = false, length = 200)
    private String confirmadaPor;

    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name = "confirmada_em", nullable = false, updatable = false)
    private Instant confirmadaEm;

    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name = "registrada_em", nullable = false, updatable = false)
    private Instant registradaEm;

    @Nationalized
    @Column(name = "fonte", nullable = false, updatable = false, length = 500)
    private String fonte;

    @Nationalized
    @Column(name = "usuario", nullable = false, updatable = false, length = 200)
    private String usuario;

    @Nationalized
    @Column(name = "motivo", nullable = false, updatable = false, length = 500)
    private String motivo;

    protected ResolucaoFinanceiraFechamento() {}

    public ResolucaoFinanceiraFechamento(
            VersaoFechamento versao,
            String tipo,
            String referenciaExterna,
            String confirmadaPor,
            Instant confirmadaEm,
            Instant registradaEm,
            String fonte,
            String usuario,
            String motivo) {
        this.versao = versao;
        this.tipo = tipo;
        this.referenciaExterna = referenciaExterna;
        this.confirmadaPor = confirmadaPor;
        this.confirmadaEm =
                confirmadaEm == null
                        ? null
                        : confirmadaEm.truncatedTo(java.time.temporal.ChronoUnit.MICROS);
        this.registradaEm =
                registradaEm == null
                        ? null
                        : registradaEm.truncatedTo(java.time.temporal.ChronoUnit.MICROS);
        this.fonte = fonte;
        this.usuario = usuario;
        this.motivo = motivo;
    }

    public Long getId() {
        return id;
    }

    public VersaoFechamento getVersao() {
        return versao;
    }

    public String getTipo() {
        return tipo;
    }

    public String getReferenciaExterna() {
        return referenciaExterna;
    }

    public String getConfirmadaPor() {
        return confirmadaPor;
    }

    public Instant getConfirmadaEm() {
        return confirmadaEm;
    }

    public Instant getRegistradaEm() {
        return registradaEm;
    }

    public String getFonte() {
        return fonte;
    }

    public String getUsuario() {
        return usuario;
    }

    public String getMotivo() {
        return motivo;
    }
}
