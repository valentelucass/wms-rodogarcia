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
import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Nationalized;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "confirmacao_externa_fechamento", schema = "wms")
public class ConfirmacaoExternaFechamento {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "versao_id", nullable = false, updatable = false)
    private VersaoFechamento versao;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "entrega_id", nullable = true, updatable = false)
    private EntregaEsl entrega;

    @Column(name = "situacao", nullable = false, updatable = false, length = 24)
    private String situacao;

    @Nationalized
    @Column(name = "fonte", nullable = false, updatable = false, length = 500)
    private String fonte;

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
    @Column(name = "usuario", nullable = false, updatable = false, length = 200)
    private String usuario;

    @Nationalized
    @Column(name = "motivo", nullable = false, updatable = false, length = 500)
    private String motivo;

    protected ConfirmacaoExternaFechamento() {}

    public ConfirmacaoExternaFechamento(
            VersaoFechamento versao,
            EntregaEsl entrega,
            String situacao,
            String fonte,
            String confirmadaPor,
            Instant confirmadaEm,
            Instant registradaEm,
            String usuario,
            String motivo) {
        this.versao = versao;
        this.entrega = entrega;
        this.situacao = situacao;
        this.fonte = fonte;
        this.confirmadaPor = confirmadaPor;
        this.confirmadaEm =
                confirmadaEm == null
                        ? null
                        : confirmadaEm.truncatedTo(java.time.temporal.ChronoUnit.MICROS);
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

    public EntregaEsl getEntrega() {
        return entrega;
    }

    public String getSituacao() {
        return situacao;
    }

    public String getFonte() {
        return fonte;
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

    public String getUsuario() {
        return usuario;
    }

    public String getMotivo() {
        return motivo;
    }
}
