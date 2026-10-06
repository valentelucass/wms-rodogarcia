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
import jakarta.persistence.Version;
import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(
        name = "pedido_saida",
        schema = "wms",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uk_pedido_saida_referencia",
                        columnNames = {"cliente_id", "armazem_id", "referencia"}))
public class PedidoSaida {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    @Column(nullable = false)
    private long versao;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cliente_id", nullable = false, updatable = false)
    private Cliente cliente;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "armazem_id", nullable = false, updatable = false)
    private Armazem armazem;

    @Column(nullable = false, length = 40, updatable = false)
    private String referencia;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private SituacaoPedidoSaida situacao;

    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name = "alterado_em", nullable = false)
    private Instant alteradoEm;

    protected PedidoSaida() {}

    public PedidoSaida(Cliente cliente, Armazem armazem, String referencia, Instant instante) {
        this.cliente = cliente;
        this.armazem = armazem;
        this.referencia = referencia;
        this.situacao = SituacaoPedidoSaida.RASCUNHO;
        this.criadoEm = instante;
        this.alteradoEm = instante;
    }

    public void atualizar(SituacaoPedidoSaida situacao, Instant instante) {
        this.situacao = situacao;
        this.alteradoEm = instante.isAfter(alteradoEm) ? instante : alteradoEm.plusNanos(1000);
    }

    public Long getId() {
        return id;
    }

    public long getVersao() {
        return versao;
    }

    public Cliente getCliente() {
        return cliente;
    }

    public Armazem getArmazem() {
        return armazem;
    }

    public String getReferencia() {
        return referencia;
    }

    public SituacaoPedidoSaida getSituacao() {
        return situacao;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }

    public Instant getAlteradoEm() {
        return alteradoEm;
    }
}
