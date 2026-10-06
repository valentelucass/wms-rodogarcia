package br.com.rodogarcia.wms.models;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
        name = "ocupacao_endereco",
        schema = "wms",
        uniqueConstraints =
                @UniqueConstraint(name = "uk_ocupacao_endereco", columnNames = "endereco_id"))
public class OcupacaoEndereco {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "endereco_id", nullable = false, updatable = false)
    private Endereco endereco;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "unidade_id")
    private UnidadeLogistica unidade;

    protected OcupacaoEndereco() {}

    public OcupacaoEndereco(Endereco endereco) {
        this.endereco = endereco;
    }

    public Long getId() {
        return id;
    }

    public Endereco getEndereco() {
        return endereco;
    }

    public UnidadeLogistica getUnidade() {
        return unidade;
    }

    public void atribuir(UnidadeLogistica unidade) {
        this.unidade = unidade;
    }
}
