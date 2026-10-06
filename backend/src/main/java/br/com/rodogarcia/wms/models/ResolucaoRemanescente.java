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
        name = "resolucao_remanescente",
        schema = "wms",
        uniqueConstraints =
                @UniqueConstraint(name = "uk_resolucao_pedido", columnNames = "pedido_saida_id"))
public class ResolucaoRemanescente {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pedido_saida_id", nullable = false, updatable = false)
    private PedidoSaida pedidoSaida;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cliente_id", nullable = false, updatable = false)
    private Cliente cliente;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "armazem_id", nullable = false, updatable = false)
    private Armazem armazem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "carga_id", updatable = false)
    private CargaInicial carga;

    @Nationalized
    @Column(
            name = "unidades_json",
            nullable = false,
            columnDefinition = "nvarchar(max)",
            updatable = false)
    private String unidadesJson;

    @Nationalized
    @Column(nullable = false, length = 500, updatable = false)
    private String motivo;

    @Nationalized
    @Column(nullable = false, length = 200, updatable = false)
    private String usuario;

    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name = "criada_em", nullable = false, updatable = false)
    private Instant criadaEm;

    protected ResolucaoRemanescente() {}

    public ResolucaoRemanescente(
            PedidoSaida pedido,
            CargaInicial carga,
            String unidades,
            String motivo,
            String usuario,
            Instant instante) {
        pedidoSaida = pedido;
        cliente = pedido.getCliente();
        armazem = pedido.getArmazem();
        this.carga = carga;
        unidadesJson = unidades;
        this.motivo = motivo;
        this.usuario = usuario;
        criadaEm = instante;
    }

    public PedidoSaida getPedidoSaida() {
        return pedidoSaida;
    }

    public CargaInicial getCarga() {
        return carga;
    }

    public String getUnidadesJson() {
        return unidadesJson;
    }
}
