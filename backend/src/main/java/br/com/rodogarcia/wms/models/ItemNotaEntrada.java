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
        name = "item_nota_entrada",
        schema = "wms",
        uniqueConstraints = {
            @UniqueConstraint(
                    name = "uk_item_nota_entrada_1",
                    columnNames = {"nota_id", "numero_item"})
        })
public class ItemNotaEntrada {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "nota_id", nullable = false, updatable = false)
    private NotaEntrada nota;

    @Column(name = "numero_item", nullable = false, updatable = false)
    private int numeroItem;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "produto_id", nullable = false, updatable = false)
    private Produto produto;

    @Column(
            name = "quantidade_prevista",
            nullable = false,
            precision = 19,
            scale = 6,
            updatable = false)
    private BigDecimal quantidadePrevista;

    @Column(name = "valor_mercadoria", nullable = true, precision = 19, scale = 2)
    private BigDecimal valorMercadoria;

    protected ItemNotaEntrada() {}

    public void complementarValor(BigDecimal valor) {
        if (valorMercadoria == null) valorMercadoria = valor;
    }

    public ItemNotaEntrada(
            NotaEntrada nota,
            int numeroItem,
            Produto produto,
            BigDecimal quantidadePrevista,
            BigDecimal valorMercadoria) {
        this.nota = nota;
        this.numeroItem = numeroItem;
        this.produto = produto;
        this.quantidadePrevista = quantidadePrevista;
        this.valorMercadoria = valorMercadoria;
    }

    public Long getId() {
        return id;
    }

    public NotaEntrada getNota() {
        return nota;
    }

    public int getNumeroItem() {
        return numeroItem;
    }

    public Produto getProduto() {
        return produto;
    }

    public BigDecimal getQuantidadePrevista() {
        return quantidadePrevista;
    }

    public BigDecimal getValorMercadoria() {
        return valorMercadoria;
    }
}
