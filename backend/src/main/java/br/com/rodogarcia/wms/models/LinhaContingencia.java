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
import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Nationalized;
import org.hibernate.type.SqlTypes;

@Entity
@Table(
        name = "linha_contingencia",
        schema = "wms",
        uniqueConstraints =
                @UniqueConstraint(name = "uk_contingencia_fato", columnNames = "identidade_fato"))
public class LinhaContingencia {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "identidade_fato", nullable = false, length = 200, updatable = false)
    private String identidadeFato;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cliente_id", nullable = false, updatable = false)
    private Cliente cliente;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "armazem_id", nullable = false, updatable = false)
    private Armazem armazem;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24, updatable = false)
    private TipoContingencia tipo;

    @Nationalized
    @Column(
            name = "conteudo_json",
            nullable = false,
            columnDefinition = "nvarchar(max)",
            updatable = false)
    private String conteudoJson;

    @Column(name = "conteudo_hash", nullable = false, length = 64, updatable = false)
    private String conteudoHash;

    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name = "ocorrida_em", nullable = false, updatable = false)
    private Instant ocorridaEm;

    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name = "registrada_em", nullable = false, updatable = false)
    private Instant registradaEm;

    @Nationalized
    @Column(nullable = false, length = 200, updatable = false)
    private String operador;

    @Nationalized
    @Column(nullable = false, length = 500, updatable = false)
    private String fonte;

    @Version
    @Column(nullable = false)
    private long versao;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private SituacaoContingencia situacao;

    @Nationalized
    @Column(length = 500)
    private String pendencia;

    @Nationalized
    @Column(name = "resultado_json", columnDefinition = "nvarchar(max)")
    private String resultadoJson;

    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name = "conciliada_em")
    private Instant conciliadaEm;

    protected LinhaContingencia() {}

    public LinhaContingencia(
            String identidade,
            Cliente cliente,
            Armazem armazem,
            TipoContingencia tipo,
            String conteudo,
            String hash,
            Instant ocorrida,
            Instant registrada,
            String operador,
            String fonte) {
        identidadeFato = identidade;
        this.cliente = cliente;
        this.armazem = armazem;
        this.tipo = tipo;
        conteudoJson = conteudo;
        conteudoHash = hash;
        ocorridaEm = ocorrida;
        registradaEm = registrada;
        this.operador = operador;
        this.fonte = fonte;
        situacao = SituacaoContingencia.PENDENTE;
        pendencia = "CONCILIACAO_PENDENTE";
    }

    public void pendente(String causa) {
        if (situacao == SituacaoContingencia.CONCILIADA)
            throw new IllegalStateException("Fato já conciliado.");
        pendencia = causa;
    }

    public void conciliar(String resultado, Instant instante) {
        if (situacao != SituacaoContingencia.PENDENTE)
            throw new IllegalStateException("Fato já conciliado.");
        resultadoJson = resultado;
        conciliadaEm = instante;
        pendencia = null;
        situacao = SituacaoContingencia.CONCILIADA;
    }

    public Long getId() {
        return id;
    }

    public String getIdentidadeFato() {
        return identidadeFato;
    }

    public Cliente getCliente() {
        return cliente;
    }

    public Armazem getArmazem() {
        return armazem;
    }

    public TipoContingencia getTipo() {
        return tipo;
    }

    public String getConteudoJson() {
        return conteudoJson;
    }

    public String getConteudoHash() {
        return conteudoHash;
    }

    public Instant getOcorridaEm() {
        return ocorridaEm;
    }

    public Instant getConciliadaEm() {
        return conciliadaEm;
    }

    public long getVersao() {
        return versao;
    }

    public SituacaoContingencia getSituacao() {
        return situacao;
    }

    public String getPendencia() {
        return pendencia;
    }

    public String getResultadoJson() {
        return resultadoJson;
    }
}
