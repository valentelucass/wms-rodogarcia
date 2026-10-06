package br.com.rodogarcia.wms.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.Instant;
import org.hibernate.annotations.Nationalized;

@Entity
@Table(
        name = "embalagem",
        schema = "wms",
        uniqueConstraints = {
            @UniqueConstraint(
                    name = "uk_embalagem_1",
                    columnNames = {"produto_id", "codigo_dun"})
        })
public class Embalagem extends CadastroBase {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "produto_id", nullable = false, updatable = false)
    private Produto produto;

    @Column(name = "codigo_dun", nullable = false, length = 40, updatable = false)
    private String codigoDun;

    @Nationalized
    @Column(nullable = false, length = 160)
    private String descricao;

    @Column(
            name = "quantidade_produto",
            nullable = false,
            precision = 19,
            scale = 6,
            updatable = false)
    private BigDecimal quantidadeProduto;

    protected Embalagem() {}

    public Embalagem(
            Produto produto,
            String codigoDun,
            String descricao,
            BigDecimal quantidadeProduto,
            Instant instante) {
        super(instante);
        this.produto = produto;
        this.codigoDun = codigoDun;
        this.descricao = descricao;
        this.quantidadeProduto = quantidadeProduto;
    }

    public Produto getProduto() {
        return produto;
    }

    public String getCodigoDun() {
        return codigoDun;
    }

    public String getDescricao() {
        return descricao;
    }

    public BigDecimal getQuantidadeProduto() {
        return quantidadeProduto;
    }

    public void alterarDescricao(String descricao, Instant instante) {
        this.descricao = descricao;
        registrarAlteracao(instante);
    }
}
