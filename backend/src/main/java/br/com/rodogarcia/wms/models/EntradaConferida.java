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
        name = "entrada_conferida",
        schema = "wms",
        uniqueConstraints = {
            @UniqueConstraint(
                    name = "uk_entrada_conferida_1",
                    columnNames = {"item_chegada_id"})
        })
public class EntradaConferida {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_chegada_id", nullable = false, updatable = false)
    private ItemChegada itemChegada;

    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name = "data_fifo", nullable = false, updatable = false)
    private Instant dataFifo;

    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name = "efetivada_em", nullable = false, updatable = false)
    private Instant efetivadaEm;

    @Column(
            name = "quantidade_triagem",
            nullable = false,
            precision = 19,
            scale = 6,
            updatable = false)
    private BigDecimal quantidadeTriagem;

    @Column(
            name = "quantidade_quarentena",
            nullable = false,
            precision = 19,
            scale = 6,
            updatable = false)
    private BigDecimal quantidadeQuarentena;

    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name = "unitizada_em")
    private Instant unitizadaEm;

    protected EntradaConferida() {}

    public EntradaConferida(
            ItemChegada itemChegada,
            Instant dataFifo,
            Instant efetivadaEm,
            BigDecimal quantidadeTriagem,
            BigDecimal quantidadeQuarentena) {
        this.itemChegada = itemChegada;
        this.dataFifo = dataFifo;
        this.efetivadaEm = efetivadaEm;
        this.quantidadeTriagem = quantidadeTriagem;
        this.quantidadeQuarentena = quantidadeQuarentena;
    }

    public Long getId() {
        return id;
    }

    public ItemChegada getItemChegada() {
        return itemChegada;
    }

    public Instant getDataFifo() {
        return dataFifo;
    }

    public Instant getEfetivadaEm() {
        return efetivadaEm;
    }

    public BigDecimal getQuantidadeTriagem() {
        return quantidadeTriagem;
    }

    public BigDecimal getQuantidadeQuarentena() {
        return quantidadeQuarentena;
    }

    public Instant getUnitizadaEm() {
        return unitizadaEm;
    }

    public void marcarUnitizada(Instant instante) {
        this.unitizadaEm = instante;
    }
}
