package br.com.rodogarcia.wms.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Nationalized;
import org.hibernate.type.SqlTypes;

@Entity
@Table(
        name = "carga_inicial",
        schema = "wms",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uk_carga_referencia",
                        columnNames = {"cliente_id", "armazem_id", "referencia"}))
public class CargaInicial {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cliente_id", nullable = false, updatable = false)
    private Cliente cliente;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "armazem_id", nullable = false, updatable = false)
    private Armazem armazem;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "produto_id", nullable = false, updatable = false)
    private Produto produto;

    @Column(nullable = false, length = 100, updatable = false)
    private String referencia;

    @Nationalized
    @Column(name = "etiqueta_fornecida", length = 200, updatable = false)
    private String etiquetaFornecida;

    @Column(nullable = false, precision = 19, scale = 6, updatable = false)
    private BigDecimal quantidade;

    @Version
    @Column(nullable = false)
    private long versao;

    @Column(name = "revisao_atual", nullable = false)
    private int revisaoAtual;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private SituacaoCargaInicial situacao;

    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name = "criada_em", nullable = false, updatable = false)
    private Instant criadaEm;

    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name = "alterada_em", nullable = false)
    private Instant alteradaEm;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entrada_id", unique = true)
    private EntradaConferida entrada;

    protected CargaInicial() {}

    public CargaInicial(
            Cliente cliente,
            Armazem armazem,
            Produto produto,
            String referencia,
            String etiqueta,
            BigDecimal quantidade,
            Instant instante) {
        this.cliente = cliente;
        this.armazem = armazem;
        this.produto = produto;
        this.referencia = referencia;
        etiquetaFornecida = etiqueta;
        this.quantidade = quantidade;
        criadaEm = instante;
        alteradaEm = instante;
        situacao = SituacaoCargaInicial.PENDENTE;
    }

    public void revisar(Instant instante) {
        exigirPendente();
        revisaoAtual++;
        alteradaEm = instante;
    }

    public void preparar(EntradaConferida entrada, Instant instante) {
        exigirPendente();
        this.entrada = entrada;
        situacao = SituacaoCargaInicial.PREPARADA;
        alteradaEm = instante;
    }

    public void confirmar(Instant instante) {
        if (situacao != SituacaoCargaInicial.PREPARADA || entrada == null)
            throw new IllegalStateException("Estágio não preparado.");
        situacao = SituacaoCargaInicial.REGULARIZADA;
        alteradaEm = instante;
    }

    public void cancelar(Instant instante) {
        exigirPendente();
        situacao = SituacaoCargaInicial.CANCELADA;
        alteradaEm = instante;
    }

    public void cancelarPreparada(Instant instante) {
        if (situacao != SituacaoCargaInicial.PREPARADA || entrada == null)
            throw new IllegalStateException("Preparação não resolvida.");
        situacao = SituacaoCargaInicial.CANCELADA;
        alteradaEm = instante;
    }

    private void exigirPendente() {
        if (situacao != SituacaoCargaInicial.PENDENTE)
            throw new IllegalStateException("Estágio finalizado.");
    }

    public Long getId() {
        return id;
    }

    public Cliente getCliente() {
        return cliente;
    }

    public Armazem getArmazem() {
        return armazem;
    }

    public Produto getProduto() {
        return produto;
    }

    public String getReferencia() {
        return referencia;
    }

    public String getEtiquetaFornecida() {
        return etiquetaFornecida;
    }

    public BigDecimal getQuantidade() {
        return quantidade;
    }

    public long getVersao() {
        return versao;
    }

    public int getRevisaoAtual() {
        return revisaoAtual;
    }

    public SituacaoCargaInicial getSituacao() {
        return situacao;
    }

    public EntradaConferida getEntrada() {
        return entrada;
    }
}
