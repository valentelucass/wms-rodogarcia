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

@Entity
@Table(
        name = "item_pedido_saida",
        schema = "wms",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uk_item_saida_produto",
                        columnNames = {"pedido_id", "produto_id"}))
public class ItemPedidoSaida {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pedido_id", nullable = false, updatable = false)
    private PedidoSaida pedido;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "produto_id", nullable = false, updatable = false)
    private Produto produto;

    @Column(nullable = false, precision = 19, scale = 6, updatable = false)
    private BigDecimal quantidade;

    protected ItemPedidoSaida() {}

    public ItemPedidoSaida(PedidoSaida pedido, Produto produto, BigDecimal quantidade) {
        this.pedido = pedido;
        this.produto = produto;
        this.quantidade = quantidade;
    }

    public Long getId() {
        return id;
    }

    public PedidoSaida getPedido() {
        return pedido;
    }

    public Produto getProduto() {
        return produto;
    }

    public BigDecimal getQuantidade() {
        return quantidade;
    }
}
