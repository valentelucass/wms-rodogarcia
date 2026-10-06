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
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Nationalized;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "avaria_estoque", schema = "wms")
public class AvariaEstoque {
    @Column(name = "ciclo_id", nullable = false, length = 36, updatable = false)
    private String cicloId;

    @Column(
            name = "equivalencia_base",
            nullable = false,
            precision = 19,
            scale = 6,
            updatable = false)
    private BigDecimal equivalenciaBase;

    @Column(name = "bloqueio_previo", nullable = false, updatable = false)
    private boolean bloqueioPrevio;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version private long versao;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "unidade_id", nullable = false, updatable = false)
    private UnidadeLogistica unidade;

    @Column(name = "quantidade", nullable = false, precision = 19, scale = 6, updatable = false)
    private BigDecimal quantidade;

    @Column(
            name = "quantidade_base",
            nullable = false,
            precision = 19,
            scale = 6,
            updatable = false)
    private BigDecimal quantidadeBase;

    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name = "ocorrida_em", nullable = false, updatable = false)
    private Instant ocorridaEm;

    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name = "registrada_em", nullable = false, updatable = false)
    private Instant registradaEm;

    @Column(nullable = false, length = 16)
    private String responsabilidade;

    @Nationalized
    @Column(
            name = "destino_json",
            nullable = false,
            columnDefinition = "nvarchar(max)",
            updatable = false)
    private String destinoJson;

    @Nationalized
    @Column(nullable = false, length = 500, updatable = false)
    private String relato;

    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name = "reconhecida_em", nullable = true)
    private Instant reconhecidaEm;

    @Nationalized
    @Column(name = "validada_por", length = 200)
    private String validadaPor;

    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name = "resolvida_em", nullable = true)
    private Instant resolvidaEm;

    @Column(nullable = false, length = 16)
    private String tratativa;

    protected AvariaEstoque() {}

    public Long getId() {
        return id;
    }

    public AvariaEstoque(
            UnidadeLogistica unidade,
            BigDecimal quantidade,
            BigDecimal quantidadeBase,
            BigDecimal equivalenciaBase,
            String cicloId,
            Instant ocorridaEm,
            Instant agora,
            String destino,
            String relato) {
        this.unidade = unidade;
        this.quantidade = quantidade;
        this.quantidadeBase = quantidadeBase;
        this.equivalenciaBase = equivalenciaBase;
        this.cicloId = cicloId;
        this.ocorridaEm = ocorridaEm.truncatedTo(java.time.temporal.ChronoUnit.MICROS);
        registradaEm = agora;
        destinoJson = destino;
        this.relato = relato;
        responsabilidade = "PENDENTE";
        tratativa = "EM_TRATAMENTO";
        bloqueioPrevio =
                unidade.isBloqueada()
                        && !unidade.isAvariaPosterior()
                        && unidade.isCondicaoApta()
                        && unidade.getTipoLocalizacao() != TipoEndereco.QUARENTENA;
    }

    public boolean isBloqueioPrevio() {
        return bloqueioPrevio;
    }

    public void reconhecer(String responsabilidade, String usuario, Instant agora) {
        this.responsabilidade = responsabilidade;
        validadaPor = usuario;
        reconhecidaEm = agora;
    }

    public void reparar(Instant agora) {
        tratativa = "REPARADA";
        resolvidaEm = agora;
    }

    public long getVersao() {
        return versao;
    }

    public UnidadeLogistica getUnidade() {
        return unidade;
    }

    public BigDecimal getQuantidade() {
        return quantidade;
    }

    public BigDecimal getQuantidadeBase() {
        return quantidadeBase;
    }

    public BigDecimal getEquivalenciaBase() {
        return equivalenciaBase;
    }

    public String getCicloId() {
        return cicloId;
    }

    public Instant getOcorridaEm() {
        return ocorridaEm;
    }

    public Instant getRegistradaEm() {
        return registradaEm;
    }

    public String getResponsabilidade() {
        return responsabilidade;
    }

    public String getDestinoJson() {
        return destinoJson;
    }

    public String getRelato() {
        return relato;
    }

    public Instant getReconhecidaEm() {
        return reconhecidaEm;
    }

    public String getValidadaPor() {
        return validadaPor;
    }

    public Instant getResolvidaEm() {
        return resolvidaEm;
    }

    public String getTratativa() {
        return tratativa;
    }
}
