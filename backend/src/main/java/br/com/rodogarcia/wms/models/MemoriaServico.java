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
import org.hibernate.annotations.Nationalized;

@Entity
@Table(
        name = "memoria_servico",
        schema = "wms",
        uniqueConstraints = {
            @UniqueConstraint(
                    name = "uk_memoria_servico_1",
                    columnNames = {"calculo_id", "fato_id"})
        })
public class MemoriaServico {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "calculo_id", nullable = false, updatable = false)
    private CalculoCobranca calculo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "fato_id", nullable = false, updatable = false)
    private FatoServico fato;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "item_tabela_id", nullable = true, updatable = false)
    private ItemTabelaCobranca itemTabela;

    @Column(name = "quantidade", nullable = false, precision = 19, scale = 6, updatable = false)
    private BigDecimal quantidade;

    @Column(name = "preco", nullable = true, precision = 19, scale = 6, updatable = false)
    private BigDecimal preco;

    @Column(name = "percentual", nullable = true, precision = 9, scale = 6, updatable = false)
    private BigDecimal percentual;

    @Column(name = "valor_base", nullable = true, precision = 19, scale = 2, updatable = false)
    private BigDecimal valorBase;

    @Column(name = "valor", nullable = true, precision = 19, scale = 2, updatable = false)
    private BigDecimal valor;

    @Nationalized
    @Column(
            name = "parcelas_json",
            nullable = false,
            columnDefinition = "nvarchar(max)",
            updatable = false)
    private String parcelasJson;

    protected MemoriaServico() {}

    public MemoriaServico(
            CalculoCobranca calculo,
            FatoServico fato,
            ItemTabelaCobranca itemTabela,
            BigDecimal quantidade,
            BigDecimal preco,
            BigDecimal percentual,
            BigDecimal valorBase,
            BigDecimal valor,
            String parcelasJson) {
        this.calculo = calculo;
        this.fato = fato;
        this.itemTabela = itemTabela;
        this.quantidade = quantidade;
        this.preco = preco;
        this.percentual = percentual;
        this.valorBase = valorBase;
        this.valor = valor;
        this.parcelasJson = parcelasJson;
    }

    public Long getId() {
        return id;
    }

    public CalculoCobranca getCalculo() {
        return calculo;
    }

    public FatoServico getFato() {
        return fato;
    }

    public ItemTabelaCobranca getItemTabela() {
        return itemTabela;
    }

    public BigDecimal getQuantidade() {
        return quantidade;
    }

    public BigDecimal getPreco() {
        return preco;
    }

    public BigDecimal getPercentual() {
        return percentual;
    }

    public BigDecimal getValorBase() {
        return valorBase;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public String getParcelasJson() {
        return parcelasJson;
    }
}
