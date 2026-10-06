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
        name = "memoria_diaria",
        schema = "wms",
        uniqueConstraints = {
            @UniqueConstraint(
                    name = "uk_memoria_diaria_1",
                    columnNames = {"calculo_id", "data"})
        })
public class MemoriaDiaria {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "calculo_id", nullable = false, updatable = false)
    private CalculoCobranca calculo;

    @Column(name = "data", nullable = false, updatable = false)
    private LocalDate data;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "tabela_id", nullable = true, updatable = false)
    private TabelaCobranca tabela;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "item_tabela_id", nullable = true, updatable = false)
    private ItemTabelaCobranca itemTabela;

    @Column(name = "pico_cobravel", nullable = false, precision = 19, scale = 6, updatable = false)
    private BigDecimal picoCobravel;

    @Column(
            name = "equivalencia_suspensa",
            nullable = false,
            precision = 19,
            scale = 6,
            updatable = false)
    private BigDecimal equivalenciaSuspensa;

    @Column(name = "valor_estoque", nullable = true, precision = 19, scale = 2, updatable = false)
    private BigDecimal valorEstoque;

    @Column(name = "tarifa", nullable = true, precision = 19, scale = 6, updatable = false)
    private BigDecimal tarifa;

    @Column(name = "valor", nullable = true, precision = 19, scale = 2, updatable = false)
    private BigDecimal valor;

    @Nationalized
    @Column(
            name = "segmentos_json",
            nullable = false,
            columnDefinition = "nvarchar(max)",
            updatable = false)
    private String segmentosJson;

    protected MemoriaDiaria() {}

    public MemoriaDiaria(
            CalculoCobranca calculo,
            LocalDate data,
            TabelaCobranca tabela,
            ItemTabelaCobranca itemTabela,
            BigDecimal picoCobravel,
            BigDecimal equivalenciaSuspensa,
            BigDecimal valorEstoque,
            BigDecimal tarifa,
            BigDecimal valor,
            String segmentosJson) {
        this.calculo = calculo;
        this.data = data;
        this.tabela = tabela;
        this.itemTabela = itemTabela;
        this.picoCobravel = picoCobravel;
        this.equivalenciaSuspensa = equivalenciaSuspensa;
        this.valorEstoque = valorEstoque;
        this.tarifa = tarifa;
        this.valor = valor;
        this.segmentosJson = segmentosJson;
    }

    public Long getId() {
        return id;
    }

    public CalculoCobranca getCalculo() {
        return calculo;
    }

    public LocalDate getData() {
        return data;
    }

    public TabelaCobranca getTabela() {
        return tabela;
    }

    public ItemTabelaCobranca getItemTabela() {
        return itemTabela;
    }

    public BigDecimal getPicoCobravel() {
        return picoCobravel;
    }

    public BigDecimal getEquivalenciaSuspensa() {
        return equivalenciaSuspensa;
    }

    public BigDecimal getValorEstoque() {
        return valorEstoque;
    }

    public BigDecimal getTarifa() {
        return tarifa;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public String getSegmentosJson() {
        return segmentosJson;
    }
}
