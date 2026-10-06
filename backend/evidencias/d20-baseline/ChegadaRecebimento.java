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
        name = "chegada_recebimento",
        schema = "wms",
        uniqueConstraints = {
            @UniqueConstraint(
                    name = "uk_chegada_recebimento_1",
                    columnNames = {"pedido_id", "operacao_id"})
        })
public class ChegadaRecebimento {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pedido_id", nullable = false, updatable = false)
    private PedidoEntrada pedido;

    @Column(name = "operacao_id", nullable = false, length = 36)
    private String operacaoId;

    @Column(name = "conteudo_hash", nullable = false, length = 64)
    private String conteudoHash;

    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name = "chegou_em", nullable = false)
    private Instant chegouEm;

    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name = "registrada_em", nullable = false)
    private Instant registradaEm;

    @Nationalized
    @Column(name = "usuario", nullable = false, length = 200)
    private String usuario;

    @Nationalized
    @Column(name = "observacao", nullable = false, length = 500)
    private String observacao;

    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name = "estornada_em", nullable = true)
    private Instant estornadaEm;

    @Nationalized
    @Column(name = "estornada_por", nullable = true, length = 200)
    private String estornadaPor;

    @Nationalized
    @Column(name = "motivo_estorno", nullable = true, length = 500)
    private String motivoEstorno;

    protected ChegadaRecebimento() {}

    public ChegadaRecebimento(
            PedidoEntrada pedido,
            String operacaoId,
            String conteudoHash,
            Instant chegouEm,
            Instant registradaEm,
            String usuario,
            String observacao) {
        this.pedido = pedido;
        this.operacaoId = operacaoId;
        this.conteudoHash = conteudoHash;
        this.chegouEm = chegouEm;
        this.registradaEm = registradaEm;
        this.usuario = usuario;
        this.observacao = observacao;
    }

    public Long getId() {
        return id;
    }

    public PedidoEntrada getPedido() {
        return pedido;
    }

    public String getOperacaoId() {
        return operacaoId;
    }

    public String getConteudoHash() {
        return conteudoHash;
    }

    public Instant getChegouEm() {
        return chegouEm;
    }

    public Instant getRegistradaEm() {
        return registradaEm;
    }

    public String getUsuario() {
        return usuario;
    }

    public String getObservacao() {
        return observacao;
    }

    public Instant getEstornadaEm() {
        return estornadaEm;
    }

    public String getEstornadaPor() {
        return estornadaPor;
    }

    public String getMotivoEstorno() {
        return motivoEstorno;
    }

    public boolean isEstornada() {
        return estornadaEm != null;
    }

    public void estornar(Instant agora, String usuario, String motivo) {
        this.estornadaEm = agora;
        this.estornadaPor = usuario;
        this.motivoEstorno = motivo;
    }
}
