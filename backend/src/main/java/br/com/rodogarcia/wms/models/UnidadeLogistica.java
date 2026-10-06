package br.com.rodogarcia.wms.models;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
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
import java.time.LocalDate;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Nationalized;
import org.hibernate.type.SqlTypes;

@Entity
@Table(
        name = "unidade_logistica",
        schema = "wms",
        uniqueConstraints = @UniqueConstraint(name = "uk_unidade_codigo", columnNames = "codigo"))
public class UnidadeLogistica {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version private long versao;

    @Column(nullable = false, length = 36, updatable = false)
    private String codigo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pedido_id", nullable = false, updatable = false)
    private PedidoEntrada pedido;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "nota_id", nullable = false, updatable = false)
    private NotaEntrada nota;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "produto_id", nullable = false, updatable = false)
    private Produto produto;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "embalagem_id", nullable = false, updatable = false)
    private Embalagem embalagem;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16, updatable = false)
    private TipoUnidadeLogistica tipo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16, updatable = false)
    private CondicaoMercadoria condicao;

    @Nationalized
    @Column(length = 60, updatable = false)
    private String lote;

    @Column(updatable = false)
    private LocalDate validade;

    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name = "data_fifo", nullable = false, updatable = false)
    private Instant dataFifo;

    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name = "chegada_real", nullable = false, updatable = false)
    private Instant chegadaReal;

    @Column(nullable = false, precision = 19, scale = 6)
    private BigDecimal quantidade;

    @Column(nullable = false)
    private boolean ativa;

    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name = "criada_em", nullable = false, updatable = false)
    private Instant criadaEm;

    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name = "alterada_em", nullable = false)
    private Instant alteradaEm;

    @Column(name = "revisao_conteudo", nullable = false)
    private long revisaoConteudo;

    @Embedded private MedidasUnidade medidas;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_localizacao", length = 16)
    private TipoEndereco tipoLocalizacao;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conjunto_atual_id")
    private ConjuntoPosicoes conjuntoAtual;

    @Column(nullable = false)
    private boolean bloqueada;

    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name = "primeiro_enderecamento_em")
    private Instant primeiroEnderecamentoEm;

    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name = "inicio_armazenagem_em")
    private Instant inicioArmazenagemEm;

    @Column(name = "posicoes_equivalentes", nullable = false)
    private int posicoesEquivalentes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reserva_saida_id")
    private PedidoSaida reservaSaida;

    @Column(name = "avaria_posterior", nullable = false)
    private boolean avariaPosterior;

    @Column(name = "avaria_inicial_reparada", nullable = false)
    private boolean avariaInicialReparada;

    public boolean isAvariaInicialReparada() {
        return avariaInicialReparada;
    }

    public boolean isCondicaoApta() {
        return condicao == CondicaoMercadoria.BOA || avariaInicialReparada;
    }

    public PedidoSaida getReservaSaida() {
        return reservaSaida;
    }

    public boolean isAvariaPosterior() {
        return avariaPosterior;
    }

    public void reservarSaida(PedidoSaida pedido, Instant instante) {
        reservaSaida = pedido;
        registrarMovimento(instante);
    }

    public void registrarAvaria(Instant instante) {
        avariaPosterior = true;
        bloqueada = true;
        registrarMovimento(instante);
    }

    public void concluirReparo(Instant instante) {
        if (condicao == CondicaoMercadoria.AVARIADA) avariaInicialReparada = true;
        avariaPosterior = false;
        bloqueada = false;
        registrarMovimento(instante);
    }

    protected UnidadeLogistica() {}

    public UnidadeLogistica(
            EntradaConferida entrada,
            Embalagem embalagem,
            TipoUnidadeLogistica tipo,
            CondicaoMercadoria condicao,
            BigDecimal quantidade,
            Instant instante) {
        var item = entrada.getItemChegada();
        this.pedido = item.getChegada().getPedido();
        this.nota = item.getItemNota().getNota();
        this.produto = item.getItemNota().getProduto();
        this.embalagem = embalagem;
        this.tipo = tipo;
        this.condicao = condicao;
        this.lote = item.getLote();
        this.validade = item.getValidade();
        this.dataFifo = entrada.getDataFifo();
        this.chegadaReal = item.getChegada().getChegouEm();
        this.codigo = UUID.randomUUID().toString();
        this.quantidade = quantidade;
        this.ativa = true;
        this.criadaEm = instante;
        this.alteradaEm = instante;
    }

    public UnidadeLogistica(UnidadeLogistica origem, BigDecimal quantidade, Instant instante) {
        this.pedido = origem.pedido;
        this.nota = origem.nota;
        this.produto = origem.produto;
        this.embalagem = origem.embalagem;
        this.tipo = origem.tipo;
        this.condicao = origem.condicao;
        this.lote = origem.lote;
        this.validade = origem.validade;
        this.dataFifo = origem.dataFifo;
        this.chegadaReal = origem.chegadaReal;
        this.codigo = UUID.randomUUID().toString();
        this.quantidade = quantidade;
        this.ativa = true;
        this.criadaEm = instante;
        this.alteradaEm = instante;
    }

    public void alterarQuantidade(BigDecimal quantidade, Instant instante) {
        this.revisaoConteudo++;
        this.quantidade = quantidade;
        this.ativa = quantidade.signum() > 0;
        this.alteradaEm = instante.isAfter(alteradaEm) ? instante : alteradaEm.plusNanos(1000);
    }

    public Long getId() {
        return id;
    }

    public long getVersao() {
        return versao;
    }

    public String getCodigo() {
        return codigo;
    }

    public PedidoEntrada getPedido() {
        return pedido;
    }

    public NotaEntrada getNota() {
        return nota;
    }

    public Produto getProduto() {
        return produto;
    }

    public Embalagem getEmbalagem() {
        return embalagem;
    }

    public TipoUnidadeLogistica getTipo() {
        return tipo;
    }

    public CondicaoMercadoria getCondicao() {
        return condicao;
    }

    public String getLote() {
        return lote;
    }

    public LocalDate getValidade() {
        return validade;
    }

    public Instant getDataFifo() {
        return dataFifo;
    }

    public Instant getChegadaReal() {
        return chegadaReal;
    }

    public BigDecimal getQuantidade() {
        return quantidade;
    }

    public boolean isAtiva() {
        return ativa;
    }

    public Instant getCriadaEm() {
        return criadaEm;
    }

    public Instant getAlteradaEm() {
        return alteradaEm;
    }

    public void posicionar(
            MedidasUnidade medidas,
            TipoEndereco tipo,
            ConjuntoPosicoes conjunto,
            Instant instante) {
        if (this.medidas == null) this.medidas = medidas;
        if (primeiroEnderecamentoEm == null) primeiroEnderecamentoEm = instante;
        if (tipo == TipoEndereco.ARMAZENAGEM && inicioArmazenagemEm == null) {
            inicioArmazenagemEm = instante;
            posicoesEquivalentes = this.medidas.getPosicoesNecessarias();
        }
        if (tipo == TipoEndereco.QUARENTENA) bloqueada = true;
        tipoLocalizacao = tipo;
        conjuntoAtual = conjunto;
        registrarMovimento(instante);
    }

    public void alterarBloqueio(boolean bloqueada, Instant instante) {
        this.bloqueada = bloqueada;
        registrarMovimento(instante);
    }

    private void registrarMovimento(Instant instante) {
        alteradaEm = instante.isAfter(alteradaEm) ? instante : alteradaEm.plusNanos(1000);
    }

    public long getRevisaoConteudo() {
        return revisaoConteudo;
    }

    public MedidasUnidade getMedidas() {
        return medidas;
    }

    public TipoEndereco getTipoLocalizacao() {
        return tipoLocalizacao;
    }

    public ConjuntoPosicoes getConjuntoAtual() {
        return conjuntoAtual;
    }

    public boolean isBloqueada() {
        return bloqueada;
    }

    public Instant getPrimeiroEnderecamentoEm() {
        return primeiroEnderecamentoEm;
    }

    public Instant getInicioArmazenagemEm() {
        return inicioArmazenagemEm;
    }

    public int getPosicoesEquivalentes() {
        return posicoesEquivalentes;
    }
}
