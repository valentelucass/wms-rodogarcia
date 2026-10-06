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
import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Nationalized;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "importacao_endereco", schema = "wms")
public class ImportacaoEndereco {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    @Column(nullable = false)
    private long versao;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "armazem_id", nullable = false, updatable = false)
    private Armazem armazem;

    @Column(name = "arquivo_hash", nullable = false, length = 64, updatable = false)
    private String arquivoHash;

    @Column(name = "layout_versao", nullable = false, updatable = false)
    private int layoutVersao;

    @Column(name = "quantidade_linhas", nullable = false, updatable = false)
    private int quantidadeLinhas;

    @Column(name = "situacao", nullable = false, length = 16)
    private String situacao;

    @Nationalized
    @Column(
            name = "linhas_json",
            nullable = false,
            columnDefinition = "nvarchar(max)",
            updatable = false)
    private String linhasJson;

    @Nationalized
    @Column(
            name = "erros_json",
            nullable = false,
            columnDefinition = "nvarchar(max)",
            updatable = false)
    private String errosJson;

    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name = "criada_em", nullable = false, updatable = false)
    private Instant criadaEm;

    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name = "confirmada_em", nullable = true)
    private Instant confirmadaEm;

    @Nationalized
    @Column(name = "usuario", nullable = false, length = 200, updatable = false)
    private String usuario;

    protected ImportacaoEndereco() {}

    public ImportacaoEndereco(
            Armazem armazem,
            String arquivoHash,
            int layoutVersao,
            int quantidadeLinhas,
            String situacao,
            String linhasJson,
            String errosJson,
            Instant criadaEm,
            Instant confirmadaEm,
            String usuario) {
        this.armazem = armazem;
        this.arquivoHash = arquivoHash;
        this.layoutVersao = layoutVersao;
        this.quantidadeLinhas = quantidadeLinhas;
        this.situacao = situacao;
        this.linhasJson = linhasJson;
        this.errosJson = errosJson;
        this.criadaEm =
                criadaEm == null
                        ? null
                        : criadaEm.truncatedTo(java.time.temporal.ChronoUnit.MICROS);
        this.confirmadaEm =
                confirmadaEm == null
                        ? null
                        : confirmadaEm.truncatedTo(java.time.temporal.ChronoUnit.MICROS);
        this.usuario = usuario;
    }

    public Long getId() {
        return id;
    }

    public long getVersao() {
        return versao;
    }

    public Armazem getArmazem() {
        return armazem;
    }

    public String getArquivoHash() {
        return arquivoHash;
    }

    public int getLayoutVersao() {
        return layoutVersao;
    }

    public int getQuantidadeLinhas() {
        return quantidadeLinhas;
    }

    public String getSituacao() {
        return situacao;
    }

    public String getLinhasJson() {
        return linhasJson;
    }

    public String getErrosJson() {
        return errosJson;
    }

    public Instant getCriadaEm() {
        return criadaEm;
    }

    public Instant getConfirmadaEm() {
        return confirmadaEm;
    }

    public String getUsuario() {
        return usuario;
    }

    public void confirmar(Instant agora) {
        situacao = "CONFIRMADA";
        confirmadaEm = agora.truncatedTo(java.time.temporal.ChronoUnit.MICROS);
    }
}
