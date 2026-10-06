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
        name = "ajuste_versao_fechamento",
        schema = "wms",
        uniqueConstraints = {
            @UniqueConstraint(
                    name = "uk_ajuste_versao_fechamento_1",
                    columnNames = {"versao_id", "ajuste_id"})
        })
public class AjusteVersaoFechamento {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "versao_id", nullable = false, updatable = false)
    private VersaoFechamento versao;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ajuste_id", nullable = false, updatable = false)
    private AjusteFechamento ajuste;

    protected AjusteVersaoFechamento() {}

    public AjusteVersaoFechamento(VersaoFechamento versao, AjusteFechamento ajuste) {
        this.versao = versao;
        this.ajuste = ajuste;
    }

    public Long getId() {
        return id;
    }

    public VersaoFechamento getVersao() {
        return versao;
    }

    public AjusteFechamento getAjuste() {
        return ajuste;
    }
}
