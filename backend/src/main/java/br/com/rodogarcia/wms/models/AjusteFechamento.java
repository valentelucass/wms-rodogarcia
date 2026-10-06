package br.com.rodogarcia.wms.models;

import jakarta.persistence.CheckConstraint;
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
        name = "ajuste_fechamento",
        schema = "wms",
        check =
                @CheckConstraint(
                        constraint =
                                "(tipo='CORRECAO_CALCULO' and tratativa_origem_id is null) or (tipo='REGULARIZACAO_ORIGEM' and tratativa_origem_id is not null)"),
        indexes = {
            @jakarta.persistence.Index(
                    name = "uk_ajuste_tratativa_origem",
                    columnList = "tratativa_origem_id",
                    unique = true)
        },
        uniqueConstraints = {
            @UniqueConstraint(
                    name = "uk_ajuste_fechamento_1",
                    columnNames = {"origem_versao_id", "hash_correcao"})
        })
public class AjusteFechamento {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tipo", nullable = false, updatable = false, length = 24)
    private String tipo = "CORRECAO_CALCULO";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tratativa_origem_id", updatable = false)
    private TratativaExternaFechamento tratativaOrigem;

    @Version
    @Column(name = "versao", nullable = false)
    private long versao;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "origem_versao_id", nullable = false, updatable = false)
    private VersaoFechamento origemVersao;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "destino_fechamento_id", nullable = false, updatable = true)
    private FechamentoCobranca destinoFechamento;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "calculo_base_id", nullable = false, updatable = false)
    private CalculoCobranca calculoBase;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "calculo_corrigido_id", nullable = false, updatable = false)
    private CalculoCobranca calculoCorrigido;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "aplicado_versao_id", nullable = true, updatable = true)
    private VersaoFechamento aplicadoVersao;

    @Column(name = "hash_correcao", nullable = false, updatable = false, length = 64)
    private String hashCorrecao;

    @Column(name = "valor_base", nullable = false, updatable = false, precision = 19, scale = 2)
    private BigDecimal valorBase;

    @Column(
            name = "valor_corrigido",
            nullable = false,
            updatable = false,
            precision = 19,
            scale = 2)
    private BigDecimal valorCorrigido;

    @Column(name = "diferenca", nullable = false, updatable = false, precision = 19, scale = 2)
    private BigDecimal diferenca;

    @Column(name = "situacao", nullable = false, updatable = true, length = 16)
    private String situacao;

    @Nationalized
    @Column(name = "motivo", nullable = false, updatable = false, length = 500)
    private String motivo;

    @Nationalized
    @Column(name = "evidencia", nullable = false, updatable = false, length = 500)
    private String evidencia;

    @Nationalized
    @Column(name = "usuario", nullable = false, updatable = false, length = 200)
    private String usuario;

    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name = "registrado_em", nullable = false, updatable = false)
    private Instant registradoEm;

    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name = "aplicado_em", nullable = true, updatable = true)
    private Instant aplicadoEm;

    protected AjusteFechamento() {}

    public AjusteFechamento(
            VersaoFechamento origemVersao,
            FechamentoCobranca destinoFechamento,
            CalculoCobranca calculoBase,
            CalculoCobranca calculoCorrigido,
            VersaoFechamento aplicadoVersao,
            String hashCorrecao,
            BigDecimal valorBase,
            BigDecimal valorCorrigido,
            BigDecimal diferenca,
            String situacao,
            String motivo,
            String evidencia,
            String usuario,
            Instant registradoEm,
            Instant aplicadoEm) {
        this.origemVersao = origemVersao;
        this.destinoFechamento = destinoFechamento;
        this.calculoBase = calculoBase;
        this.calculoCorrigido = calculoCorrigido;
        this.aplicadoVersao = aplicadoVersao;
        this.hashCorrecao = hashCorrecao;
        this.valorBase = valorBase;
        this.valorCorrigido = valorCorrigido;
        this.diferenca = diferenca;
        this.situacao = situacao;
        this.motivo = motivo;
        this.evidencia = evidencia;
        this.usuario = usuario;
        this.registradoEm =
                registradoEm == null
                        ? null
                        : registradoEm.truncatedTo(java.time.temporal.ChronoUnit.MICROS);
        this.aplicadoEm =
                aplicadoEm == null
                        ? null
                        : aplicadoEm.truncatedTo(java.time.temporal.ChronoUnit.MICROS);
    }

    public Long getId() {
        return id;
    }

    public long getVersao() {
        return versao;
    }

    public String getTipo() {
        return tipo;
    }

    public TratativaExternaFechamento getTratativaOrigem() {
        return tratativaOrigem;
    }

    public void identificarRegularizacao(TratativaExternaFechamento tratativa) {
        if (id != null || tratativaOrigem != null || !situacao.equals("VALIDADO"))
            throw new IllegalStateException("Regularizacao deve nascer identificada e validada");
        if (tratativa.getVersaoBaseAnterior() == null
                || !origemVersao.getId().equals(tratativa.getVersaoResultado().getId())
                || !calculoBase.getId().equals(tratativa.getVersaoResultado().getCalculo().getId())
                || !calculoCorrigido
                        .getId()
                        .equals(tratativa.getVersaoBaseAnterior().getCalculo().getId())
                || valorBase.compareTo(calculoBase.getTotal()) != 0
                || valorCorrigido.compareTo(calculoCorrigido.getTotal()) != 0
                || diferenca.compareTo(valorCorrigido.subtract(valorBase)) != 0
                || !destinoFechamento
                        .getCliente()
                        .getId()
                        .equals(origemVersao.getFechamento().getCliente().getId())
                || !destinoFechamento
                        .getArmazem()
                        .getId()
                        .equals(origemVersao.getFechamento().getArmazem().getId())
                || destinoFechamento
                        .getPeriodoInicio()
                        .isBefore(origemVersao.getFechamento().getPeriodoFim()))
            throw new IllegalStateException("Base/contexto de regularizacao divergente");
        tipo = "REGULARIZACAO_ORIGEM";
        tratativaOrigem = tratativa;
    }

    public VersaoFechamento getOrigemVersao() {
        return origemVersao;
    }

    public FechamentoCobranca getDestinoFechamento() {
        return destinoFechamento;
    }

    public CalculoCobranca getCalculoBase() {
        return calculoBase;
    }

    public CalculoCobranca getCalculoCorrigido() {
        return calculoCorrigido;
    }

    public VersaoFechamento getAplicadoVersao() {
        return aplicadoVersao;
    }

    public String getHashCorrecao() {
        return hashCorrecao;
    }

    public BigDecimal getValorBase() {
        return valorBase;
    }

    public BigDecimal getValorCorrigido() {
        return valorCorrigido;
    }

    public BigDecimal getDiferenca() {
        return diferenca;
    }

    public String getSituacao() {
        return situacao;
    }

    public String getMotivo() {
        return motivo;
    }

    public String getEvidencia() {
        return evidencia;
    }

    public String getUsuario() {
        return usuario;
    }

    public Instant getRegistradoEm() {
        return registradoEm;
    }

    public Instant getAplicadoEm() {
        return aplicadoEm;
    }

    public void aplicar(VersaoFechamento versao, Instant agora) {
        if (!destinoFechamento.getId().equals(versao.getFechamento().getId()))
            throw new IllegalStateException("Versao nao pertence ao destino atual do ajuste");
        if (!situacao.equals("VALIDADO")) throw new IllegalStateException("Ajuste ja aplicado");
        situacao = "APLICADO";
        aplicadoVersao = versao;
        aplicadoEm = agora;
    }

    public void redirecionar(FechamentoCobranca destino) {
        if (!situacao.equals("VALIDADO"))
            throw new IllegalStateException("Ajuste aplicado nao muda destino");
        destinoFechamento = destino;
    }
}
