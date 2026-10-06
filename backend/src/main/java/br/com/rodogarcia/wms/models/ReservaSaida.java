package br.com.rodogarcia.wms.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
import org.hibernate.type.SqlTypes;

@Entity
@Table(
        name = "reserva_saida",
        schema = "wms",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uk_reserva_saida_operacao_unidade",
                        columnNames = {"pedido_id", "operacao_reserva_id", "unidade_id"}))
public class ReservaSaida {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pedido_id", nullable = false, updatable = false)
    private PedidoSaida pedido;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_id", nullable = false, updatable = false)
    private ItemPedidoSaida item;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "unidade_id", nullable = false, updatable = false)
    private UnidadeLogistica unidade;

    @Column(name = "operacao_reserva_id", nullable = false, length = 36, updatable = false)
    private String operacaoReservaId;

    @Column(nullable = false, precision = 19, scale = 6, updatable = false)
    private BigDecimal quantidade;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private SituacaoReservaSaida situacao;

    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name = "criada_em", nullable = false, updatable = false)
    private Instant criadaEm;

    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name = "encerrada_em")
    private Instant encerradaEm;

    protected ReservaSaida() {}

    public ReservaSaida(
            ItemPedidoSaida item,
            UnidadeLogistica unidade,
            String operacaoId,
            BigDecimal quantidade,
            Instant instante) {
        this.pedido = item.getPedido();
        this.item = item;
        this.unidade = unidade;
        this.operacaoReservaId = operacaoId;
        this.quantidade = quantidade;
        this.situacao = SituacaoReservaSaida.ATIVA;
        this.criadaEm = instante;
    }

    public void encerrar(SituacaoReservaSaida situacao, Instant instante) {
        this.situacao = situacao;
        this.encerradaEm = instante;
    }

    public Long getId() {
        return id;
    }

    public ItemPedidoSaida getItem() {
        return item;
    }

    public UnidadeLogistica getUnidade() {
        return unidade;
    }

    public BigDecimal getQuantidade() {
        return quantidade;
    }

    public SituacaoReservaSaida getSituacao() {
        return situacao;
    }

    public Instant getCriadaEm() {
        return criadaEm;
    }

    public Instant getEncerradaEm() {
        return encerradaEm;
    }
}
