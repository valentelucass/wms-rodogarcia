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
        name = "versao_fechamento",
        schema = "wms",
        uniqueConstraints = {
            @UniqueConstraint(
                    name = "uk_versao_fechamento_1",
                    columnNames = {"fechamento_id", "numero"})
        })
public class VersaoFechamento {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    @Column(name = "versao", nullable = false)
    private long versao;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "fechamento_id", nullable = false, updatable = false)
    private FechamentoCobranca fechamento;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "calculo_id", nullable = false, updatable = false)
    private CalculoCobranca calculo;

    @Column(name = "numero", nullable = false, updatable = false)
    private int numero;

    @Column(name = "situacao", nullable = false, updatable = true, length = 24)
    private String situacao;

    @Column(name = "estado_externo", nullable = false, updatable = true, length = 24)
    private String estadoExterno;

    @Column(name = "saldo", nullable = true, updatable = false, precision = 19, scale = 2)
    private BigDecimal saldo;

    @Column(name = "natureza", nullable = true, updatable = false, length = 8)
    private String natureza;

    @Nationalized
    @Column(
            name = "memoria_json",
            nullable = false,
            updatable = false,
            columnDefinition = "nvarchar(max)")
    private String memoriaJson;

    @Column(name = "conteudo_hash", nullable = false, updatable = false, length = 64)
    private String conteudoHash;

    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name = "criada_em", nullable = false, updatable = false)
    private Instant criadaEm;

    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name = "decidida_em", nullable = true, updatable = true)
    private Instant decididaEm;

    @Nationalized
    @Column(name = "decisor", nullable = true, updatable = true, length = 200)
    private String decisor;

    @Nationalized
    @Column(name = "motivo_decisao", nullable = true, updatable = true, length = 500)
    private String motivoDecisao;

    protected VersaoFechamento() {}

    public VersaoFechamento(
            FechamentoCobranca fechamento,
            CalculoCobranca calculo,
            int numero,
            String situacao,
            String estadoExterno,
            BigDecimal saldo,
            String natureza,
            String memoriaJson,
            String conteudoHash,
            Instant criadaEm,
            Instant decididaEm,
            String decisor,
            String motivoDecisao) {
        this.fechamento = fechamento;
        this.calculo = calculo;
        this.numero = numero;
        this.situacao = situacao;
        this.estadoExterno = estadoExterno;
        this.saldo = saldo;
        this.natureza = natureza;
        this.memoriaJson = memoriaJson;
        this.conteudoHash = conteudoHash;
        this.criadaEm =
                criadaEm == null
                        ? null
                        : criadaEm.truncatedTo(java.time.temporal.ChronoUnit.MICROS);
        this.decididaEm =
                decididaEm == null
                        ? null
                        : decididaEm.truncatedTo(java.time.temporal.ChronoUnit.MICROS);
        this.decisor = decisor;
        this.motivoDecisao = motivoDecisao;
    }

    public Long getId() {
        return id;
    }

    public long getVersao() {
        return versao;
    }

    public FechamentoCobranca getFechamento() {
        return fechamento;
    }

    public CalculoCobranca getCalculo() {
        return calculo;
    }

    public int getNumero() {
        return numero;
    }

    public String getSituacao() {
        return situacao;
    }

    public String getEstadoExterno() {
        return estadoExterno;
    }

    public BigDecimal getSaldo() {
        return saldo;
    }

    public String getNatureza() {
        return natureza;
    }

    public String getMemoriaJson() {
        return memoriaJson;
    }

    public String getConteudoHash() {
        return conteudoHash;
    }

    public Instant getCriadaEm() {
        return criadaEm;
    }

    public Instant getDecididaEm() {
        return decididaEm;
    }

    public String getDecisor() {
        return decisor;
    }

    public String getMotivoDecisao() {
        return motivoDecisao;
    }

    public void decidir(String situacao, String usuario, String motivo, Instant agora) {
        this.situacao = situacao;
        decisor = usuario;
        motivoDecisao = motivo;
        decididaEm = agora;
    }

    public void situacao(String valor) {
        situacao = valor;
    }

    public void externo(String valor) {
        estadoExterno = valor;
    }
}
