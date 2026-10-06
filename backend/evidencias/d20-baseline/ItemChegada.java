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
import java.time.LocalDate;
import org.hibernate.annotations.Nationalized;

@Entity
@Table(
        name = "item_chegada",
        schema = "wms",
        uniqueConstraints = {
            @UniqueConstraint(
                    name = "uk_item_chegada_1",
                    columnNames = {"chegada_id", "sequencia"})
        })
public class ItemChegada {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "chegada_id", nullable = false, updatable = false)
    private ChegadaRecebimento chegada;

    @Column(name = "sequencia", nullable = false)
    private int sequencia;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_nota_id", nullable = false, updatable = false)
    private ItemNotaEntrada itemNota;

    @Nationalized
    @Column(name = "lote", nullable = true, length = 60)
    private String lote;

    @Column(name = "validade", nullable = true)
    private LocalDate validade;

    @Column(name = "quantidade_boa", nullable = false, precision = 19, scale = 6)
    private BigDecimal quantidadeBoa;

    @Column(name = "quantidade_avariada", nullable = false, precision = 19, scale = 6)
    private BigDecimal quantidadeAvariada;

    protected ItemChegada() {}

    public ItemChegada(
            ChegadaRecebimento chegada,
            int sequencia,
            ItemNotaEntrada itemNota,
            String lote,
            LocalDate validade,
            BigDecimal quantidadeBoa,
            BigDecimal quantidadeAvariada) {
        this.chegada = chegada;
        this.sequencia = sequencia;
        this.itemNota = itemNota;
        this.lote = lote;
        this.validade = validade;
        this.quantidadeBoa = quantidadeBoa;
        this.quantidadeAvariada = quantidadeAvariada;
    }

    public Long getId() {
        return id;
    }

    public ChegadaRecebimento getChegada() {
        return chegada;
    }

    public int getSequencia() {
        return sequencia;
    }

    public ItemNotaEntrada getItemNota() {
        return itemNota;
    }

    public String getLote() {
        return lote;
    }

    public LocalDate getValidade() {
        return validade;
    }

    public BigDecimal getQuantidadeBoa() {
        return quantidadeBoa;
    }

    public BigDecimal getQuantidadeAvariada() {
        return quantidadeAvariada;
    }
}
