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
        name = "retirada_saida",
        schema = "wms",
        uniqueConstraints =
                @UniqueConstraint(name = "uk_retirada_pedido", columnNames = "pedido_id"))
public class RetiradaSaida {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pedido_id", nullable = false, updatable = false)
    private PedidoSaida pedido;

    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name = "retirada_em", nullable = false, updatable = false)
    private Instant retiradaEm;

    @Nationalized
    @Column(nullable = false, length = 200, updatable = false)
    private String usuario;

    @Column(name = "comprovante_hash", nullable = false, length = 64, updatable = false)
    private String comprovanteHash;

    protected RetiradaSaida() {}

    public Long getId() {
        return id;
    }

    public RetiradaSaida(PedidoSaida pedido, Instant agora, String usuario, String hash) {
        this.pedido = pedido;
        retiradaEm = agora;
        this.usuario = usuario;
        comprovanteHash = hash;
    }

    public PedidoSaida getPedido() {
        return pedido;
    }

    public Instant getRetiradaEm() {
        return retiradaEm;
    }

    public String getUsuario() {
        return usuario;
    }

    public String getComprovanteHash() {
        return comprovanteHash;
    }
}
