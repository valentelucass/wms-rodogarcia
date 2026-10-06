package br.com.rodogarcia.wms.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import org.hibernate.annotations.Nationalized;

@Entity
@Table(
        name = "produto",
        schema = "wms",
        uniqueConstraints = {
            @UniqueConstraint(
                    name = "uk_produto_1",
                    columnNames = {"cliente_id", "sku"})
        })
public class Produto extends CadastroBase {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cliente_id", nullable = false, updatable = false)
    private Cliente cliente;

    @Column(nullable = false, length = 40, updatable = false)
    private String sku;

    @Nationalized
    @Column(nullable = false, length = 160)
    private String descricao;

    @Column(name = "unidade_medida", nullable = false, length = 8, updatable = false)
    private String unidadeMedida;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_quantidade", nullable = false, length = 10, updatable = false)
    private TipoQuantidade tipoQuantidade;

    @Column(name = "precisao_quantidade", nullable = false, updatable = false)
    private int precisaoQuantidade;

    @Column(name = "controla_lote", nullable = false, updatable = false)
    private boolean controlaLote;

    @Column(name = "controla_validade", nullable = false, updatable = false)
    private boolean controlaValidade;

    @Column(name = "antecedencia_aviso_dias", updatable = false)
    private Integer antecedenciaAvisoDias;

    protected Produto() {}

    public Produto(
            Cliente cliente,
            String sku,
            String descricao,
            String unidadeMedida,
            TipoQuantidade tipoQuantidade,
            int precisaoQuantidade,
            boolean controlaLote,
            boolean controlaValidade,
            Integer antecedenciaAvisoDias,
            Instant instante) {
        super(instante);
        this.cliente = cliente;
        this.sku = sku;
        this.descricao = descricao;
        this.unidadeMedida = unidadeMedida;
        this.tipoQuantidade = tipoQuantidade;
        this.precisaoQuantidade = precisaoQuantidade;
        this.controlaLote = controlaLote;
        this.controlaValidade = controlaValidade;
        this.antecedenciaAvisoDias = antecedenciaAvisoDias;
    }

    public Cliente getCliente() {
        return cliente;
    }

    public String getSku() {
        return sku;
    }

    public String getDescricao() {
        return descricao;
    }

    public String getUnidadeMedida() {
        return unidadeMedida;
    }

    public TipoQuantidade getTipoQuantidade() {
        return tipoQuantidade;
    }

    public int getPrecisaoQuantidade() {
        return precisaoQuantidade;
    }

    public boolean getControlaLote() {
        return controlaLote;
    }

    public boolean getControlaValidade() {
        return controlaValidade;
    }

    public Integer getAntecedenciaAvisoDias() {
        return antecedenciaAvisoDias;
    }

    public void alterarDescricao(String descricao, Instant instante) {
        this.descricao = descricao;
        registrarAlteracao(instante);
    }
}
