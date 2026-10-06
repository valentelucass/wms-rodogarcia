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
        name = "baixa_saida",
        schema = "wms",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uk_baixa_origem",
                        columnNames = {"retirada_id", "reserva_id", "entrada_origem_id"}))
public class BaixaSaida {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "retirada_id", nullable = false, updatable = false)
    private RetiradaSaida retirada;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reserva_id", nullable = false, updatable = false)
    private ReservaSaida reserva;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "entrada_origem_id", nullable = false, updatable = false)
    private EntradaConferida entradaOrigem;

    @Column(name = "quantidade", nullable = false, precision = 19, scale = 6, updatable = false)
    private BigDecimal quantidade;

    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name = "data_fifo", nullable = false, updatable = false)
    private Instant dataFifo;

    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name = "inicio_armazenagem_em", nullable = true, updatable = false)
    private Instant inicioArmazenagemEm;

    @Column(
            name = "posicoes_equivalentes",
            nullable = false,
            precision = 19,
            scale = 6,
            updatable = false)
    private BigDecimal posicoesEquivalentes;

    protected BaixaSaida() {}

    public Long getId() {
        return id;
    }

    public BaixaSaida(
            RetiradaSaida retirada,
            ReservaSaida reserva,
            EntradaConferida entrada,
            BigDecimal quantidade) {
        this.retirada = retirada;
        this.reserva = reserva;
        entradaOrigem = entrada;
        this.quantidade = quantidade;
        dataFifo = entrada.getDataFifo();
        inicioArmazenagemEm = reserva.getUnidade().getInicioArmazenagemEm();
        posicoesEquivalentes = BigDecimal.valueOf(reserva.getUnidade().getPosicoesEquivalentes());
    }

    public RetiradaSaida getRetirada() {
        return retirada;
    }

    public ReservaSaida getReserva() {
        return reserva;
    }

    public EntradaConferida getEntradaOrigem() {
        return entradaOrigem;
    }

    public BigDecimal getQuantidade() {
        return quantidade;
    }

    public Instant getDataFifo() {
        return dataFifo;
    }

    public Instant getInicioArmazenagemEm() {
        return inicioArmazenagemEm;
    }

    public BigDecimal getPosicoesEquivalentes() {
        return posicoesEquivalentes;
    }
}
