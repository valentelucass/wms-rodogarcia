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
import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(
        name = "devolucao_saida",
        schema = "wms",
        uniqueConstraints =
                @UniqueConstraint(name = "uk_devolucao_entrada", columnNames = "entrada_nova_id"))
public class DevolucaoSaida {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "baixa_id", nullable = false, updatable = false)
    private BaixaSaida baixa;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pedido_entrada_id", nullable = false, updatable = false)
    private PedidoEntrada pedidoEntrada;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "entrada_nova_id", nullable = false, updatable = false)
    private EntradaConferida entradaNova;

    @Column(name = "quantidade", nullable = false, precision = 19, scale = 6, updatable = false)
    private BigDecimal quantidade;

    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name = "registrada_em", nullable = false, updatable = false)
    private Instant registradaEm;

    protected DevolucaoSaida() {}

    public Long getId() {
        return id;
    }

    public DevolucaoSaida(
            BaixaSaida baixa,
            PedidoEntrada pedido,
            EntradaConferida entrada,
            BigDecimal quantidade,
            Instant agora) {
        this.baixa = baixa;
        pedidoEntrada = pedido;
        entradaNova = entrada;
        this.quantidade = quantidade;
        registradaEm = agora;
    }

    public BaixaSaida getBaixa() {
        return baixa;
    }

    public PedidoEntrada getPedidoEntrada() {
        return pedidoEntrada;
    }

    public EntradaConferida getEntradaNova() {
        return entradaNova;
    }

    public BigDecimal getQuantidade() {
        return quantidade;
    }

    public Instant getRegistradaEm() {
        return registradaEm;
    }
}
