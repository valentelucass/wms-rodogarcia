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
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Nationalized;
import org.hibernate.type.SqlTypes;

@Entity
@Table(
        name = "fato_servico",
        schema = "wms",
        uniqueConstraints = {
            @UniqueConstraint(
                    name = "uk_fato_servico_1",
                    columnNames = {"cliente_id", "armazem_id", "servico_id", "chave_fato"})
        })
public class FatoServico {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    @Column(nullable = false)
    private long versao;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cliente_id", nullable = false, updatable = false)
    private Cliente cliente;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "armazem_id", nullable = false, updatable = false)
    private Armazem armazem;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "servico_id", nullable = false, updatable = false)
    private ServicoCobranca servico;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "pedido_entrada_id", nullable = true, updatable = false)
    private PedidoEntrada pedidoEntrada;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "pedido_saida_id", nullable = true, updatable = false)
    private PedidoSaida pedidoSaida;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "unidade_id", nullable = true, updatable = false)
    private UnidadeLogistica unidade;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "produto_id", nullable = true, updatable = false)
    private Produto produto;

    @Column(name = "chave_fato", nullable = false, length = 160, updatable = false)
    private String chaveFato;

    @Column(name = "origem", nullable = false, length = 16, updatable = false)
    private String origem;

    @Column(name = "referencia_execucao", nullable = true, length = 80, updatable = false)
    private String referenciaExecucao;

    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name = "executado_em", nullable = false, updatable = false)
    private Instant executadoEm;

    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name = "registrado_em", nullable = false, updatable = false)
    private Instant registradoEm;

    @Column(name = "quantidade", nullable = false, precision = 19, scale = 6, updatable = false)
    private BigDecimal quantidade;

    @Column(name = "categoria", nullable = false, length = 40, updatable = false)
    private String categoria;

    @Column(name = "valor_base", nullable = true, precision = 19, scale = 2, updatable = false)
    private BigDecimal valorBase;

    @Nationalized
    @Column(name = "criterio_rateio", nullable = false, length = 200, updatable = false)
    private String criterioRateio;

    @Column(name = "situacao", nullable = false, length = 16)
    private String situacao;

    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name = "anulado_em", nullable = true)
    private Instant anuladoEm;

    @Nationalized
    @Column(name = "usuario", nullable = false, length = 200, updatable = false)
    private String usuario;

    protected FatoServico() {}

    public FatoServico(
            Cliente cliente,
            Armazem armazem,
            ServicoCobranca servico,
            PedidoEntrada pedidoEntrada,
            PedidoSaida pedidoSaida,
            UnidadeLogistica unidade,
            Produto produto,
            String chaveFato,
            String origem,
            String referenciaExecucao,
            Instant executadoEm,
            Instant registradoEm,
            BigDecimal quantidade,
            String categoria,
            BigDecimal valorBase,
            String criterioRateio,
            String situacao,
            Instant anuladoEm,
            String usuario) {
        this.cliente = cliente;
        this.armazem = armazem;
        this.servico = servico;
        this.pedidoEntrada = pedidoEntrada;
        this.pedidoSaida = pedidoSaida;
        this.unidade = unidade;
        this.produto = produto;
        this.chaveFato = chaveFato;
        this.origem = origem;
        this.referenciaExecucao = referenciaExecucao;
        this.executadoEm =
                executadoEm == null
                        ? null
                        : executadoEm.truncatedTo(java.time.temporal.ChronoUnit.MICROS);
        this.registradoEm =
                registradoEm == null
                        ? null
                        : registradoEm.truncatedTo(java.time.temporal.ChronoUnit.MICROS);
        this.quantidade = quantidade;
        this.categoria = categoria;
        this.valorBase = valorBase;
        this.criterioRateio = criterioRateio;
        this.situacao = situacao;
        this.anuladoEm =
                anuladoEm == null
                        ? null
                        : anuladoEm.truncatedTo(java.time.temporal.ChronoUnit.MICROS);
        this.usuario = usuario;
    }

    public Long getId() {
        return id;
    }

    public long getVersao() {
        return versao;
    }

    public Cliente getCliente() {
        return cliente;
    }

    public Armazem getArmazem() {
        return armazem;
    }

    public ServicoCobranca getServico() {
        return servico;
    }

    public PedidoEntrada getPedidoEntrada() {
        return pedidoEntrada;
    }

    public PedidoSaida getPedidoSaida() {
        return pedidoSaida;
    }

    public UnidadeLogistica getUnidade() {
        return unidade;
    }

    public Produto getProduto() {
        return produto;
    }

    public String getChaveFato() {
        return chaveFato;
    }

    public String getOrigem() {
        return origem;
    }

    public String getReferenciaExecucao() {
        return referenciaExecucao;
    }

    public Instant getExecutadoEm() {
        return executadoEm;
    }

    public Instant getRegistradoEm() {
        return registradoEm;
    }

    public BigDecimal getQuantidade() {
        return quantidade;
    }

    public String getCategoria() {
        return categoria;
    }

    public BigDecimal getValorBase() {
        return valorBase;
    }

    public String getCriterioRateio() {
        return criterioRateio;
    }

    public String getSituacao() {
        return situacao;
    }

    public Instant getAnuladoEm() {
        return anuladoEm;
    }

    public String getUsuario() {
        return usuario;
    }

    public void anular(Instant agora) {
        situacao = "ANULADO";
        anuladoEm = agora.truncatedTo(java.time.temporal.ChronoUnit.MICROS);
    }
}
