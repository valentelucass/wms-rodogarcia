package br.com.rodogarcia.wms.models;

import jakarta.persistence.CheckConstraint;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Nationalized;
import org.hibernate.type.SqlTypes;

@Entity
@Table(
        name = "servico_cobranca",
        schema = "wms",
        check =
                @CheckConstraint(
                        constraint = "situacao in ('ATIVO','ENCERRAMENTO_PENDENTE','INATIVO')"),
        uniqueConstraints = {
            @UniqueConstraint(
                    name = "uk_servico_cobranca_1",
                    columnNames = {"codigo"})
        })
public class ServicoCobranca {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    @Column(nullable = false)
    private long versao;

    @Column(name = "codigo", nullable = false, length = 40, updatable = false)
    private String codigo;

    @Nationalized
    @Column(name = "descricao", nullable = false, length = 160)
    private String descricao;

    @Column(name = "tipo", nullable = false, length = 16, updatable = false)
    private String tipo;

    @Column(name = "unidade", nullable = false, length = 24, updatable = false)
    private String unidade;

    @Column(name = "situacao", nullable = false, length = 24)
    private String situacao;

    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name = "alterado_em", nullable = false)
    private Instant alteradoEm;

    protected ServicoCobranca() {}

    public ServicoCobranca(
            String codigo,
            String descricao,
            String tipo,
            String unidade,
            String situacao,
            Instant criadoEm,
            Instant alteradoEm) {
        this.codigo = codigo;
        this.descricao = descricao;
        this.tipo = tipo;
        this.unidade = unidade;
        this.situacao = situacao;
        this.criadoEm =
                criadoEm == null
                        ? null
                        : criadoEm.truncatedTo(java.time.temporal.ChronoUnit.MICROS);
        this.alteradoEm =
                alteradoEm == null
                        ? null
                        : alteradoEm.truncatedTo(java.time.temporal.ChronoUnit.MICROS);
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

    public String getDescricao() {
        return descricao;
    }

    public String getTipo() {
        return tipo;
    }

    public String getUnidade() {
        return unidade;
    }

    public String getSituacao() {
        return situacao;
    }

    public void alterarSituacaoEncerramento(String destino, Instant instante) {
        if (!(situacao.equals("ATIVO") && destino.equals("ENCERRAMENTO_PENDENTE")
                || situacao.equals("ENCERRAMENTO_PENDENTE") && destino.equals("INATIVO")))
            throw new IllegalStateException("Transição de serviço inválida.");
        situacao = destino;
        alteradoEm = instante;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }

    public Instant getAlteradoEm() {
        return alteradoEm;
    }
}
