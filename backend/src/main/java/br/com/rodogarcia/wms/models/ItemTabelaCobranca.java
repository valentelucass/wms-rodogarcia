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
import java.math.BigDecimal;

@Entity
@Table(name = "item_tabela_cobranca", schema = "wms")
public class ItemTabelaCobranca {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tabela_id", nullable = false, updatable = false)
    private TabelaCobranca tabela;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "servico_id", nullable = false, updatable = false)
    private ServicoCobranca servico;

    @Column(name = "categoria", nullable = false, length = 40, updatable = false)
    private String categoria;

    @Column(name = "preco", nullable = true, precision = 19, scale = 6, updatable = false)
    private BigDecimal preco;

    @Column(name = "percentual", nullable = true, precision = 9, scale = 6, updatable = false)
    private BigDecimal percentual;

    @Column(nullable = false)
    private boolean ativo = true;

    public void retirar() {
        ativo = false;
    }

    public boolean isAtivo() {
        return ativo;
    }

    protected ItemTabelaCobranca() {}

    public ItemTabelaCobranca(
            TabelaCobranca tabela,
            ServicoCobranca servico,
            String categoria,
            BigDecimal preco,
            BigDecimal percentual) {
        this.tabela = tabela;
        this.servico = servico;
        this.categoria = categoria;
        this.preco = preco;
        this.percentual = percentual;
    }

    public Long getId() {
        return id;
    }

    public TabelaCobranca getTabela() {
        return tabela;
    }

    public ServicoCobranca getServico() {
        return servico;
    }

    public String getCategoria() {
        return categoria;
    }

    public BigDecimal getPreco() {
        return preco;
    }

    public BigDecimal getPercentual() {
        return percentual;
    }
}
