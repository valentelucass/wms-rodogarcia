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
        name = "conteudo_unidade",
        schema = "wms",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uk_conteudo_origem",
                        columnNames = {"unidade_id", "entrada_id"}))
public class ConteudoUnidade {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "unidade_id", nullable = false, updatable = false)
    private UnidadeLogistica unidade;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "entrada_id", nullable = false, updatable = false)
    private EntradaConferida entrada;

    @Column(nullable = false, precision = 19, scale = 6)
    private BigDecimal quantidade;

    protected ConteudoUnidade() {}

    public ConteudoUnidade(
            UnidadeLogistica unidade, EntradaConferida entrada, BigDecimal quantidade) {
        this.unidade = unidade;
        this.entrada = entrada;
        this.quantidade = quantidade;
    }

    public Long getId() {
        return id;
    }

    public UnidadeLogistica getUnidade() {
        return unidade;
    }

    public EntradaConferida getEntrada() {
        return entrada;
    }

    public BigDecimal getQuantidade() {
        return quantidade;
    }

    public void alterarQuantidade(BigDecimal quantidade) {
        this.quantidade = quantidade;
    }
}
