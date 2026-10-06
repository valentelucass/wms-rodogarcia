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
import java.time.Instant;
import java.time.LocalDate;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Nationalized;
import org.hibernate.type.SqlTypes;

@Entity
@Table(
        name = "tabela_cobranca",
        schema = "wms",
        uniqueConstraints = {
            @UniqueConstraint(
                    name = "uk_tabela_cobranca_1",
                    columnNames = {"armazem_id", "codigo"})
        })
public class TabelaCobranca {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    @Column(nullable = false)
    private long versao;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "armazem_id", nullable = false, updatable = false)
    private Armazem armazem;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "cliente_id", nullable = true, updatable = false)
    private Cliente cliente;

    @Column(name = "codigo", nullable = false, length = 40, updatable = false)
    private String codigo;

    @Nationalized
    @Column(name = "descricao", nullable = false, length = 160, updatable = false)
    private String descricao;

    @Column(name = "tipo", nullable = false, length = 16, updatable = false)
    private String tipo;

    @Column(name = "vigencia_inicio", nullable = false, updatable = false)
    private LocalDate vigenciaInicio;

    @Column(name = "vigencia_fim", nullable = true)
    private LocalDate vigenciaFim;

    @Column(name = "situacao", nullable = false, length = 16)
    private String situacao;

    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name = "criada_em", nullable = false, updatable = false)
    private Instant criadaEm;

    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name = "alterada_em", nullable = false)
    private Instant alteradaEm;

    protected TabelaCobranca() {}

    public TabelaCobranca(
            Armazem armazem,
            Cliente cliente,
            String codigo,
            String descricao,
            String tipo,
            LocalDate vigenciaInicio,
            LocalDate vigenciaFim,
            String situacao,
            Instant criadaEm,
            Instant alteradaEm) {
        this.armazem = armazem;
        this.cliente = cliente;
        this.codigo = codigo;
        this.descricao = descricao;
        this.tipo = tipo;
        this.vigenciaInicio = vigenciaInicio;
        this.vigenciaFim = vigenciaFim;
        this.situacao = situacao;
        this.criadaEm =
                criadaEm == null
                        ? null
                        : criadaEm.truncatedTo(java.time.temporal.ChronoUnit.MICROS);
        this.alteradaEm =
                alteradaEm == null
                        ? null
                        : alteradaEm.truncatedTo(java.time.temporal.ChronoUnit.MICROS);
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

    public Cliente getCliente() {
        return cliente;
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

    public LocalDate getVigenciaInicio() {
        return vigenciaInicio;
    }

    public LocalDate getVigenciaFim() {
        return vigenciaFim;
    }

    public String getSituacao() {
        return situacao;
    }

    public Instant getCriadaEm() {
        return criadaEm;
    }

    public Instant getAlteradaEm() {
        return alteradaEm;
    }

    public void encerrar(LocalDate fim, Instant agora) {
        vigenciaFim = fim;
        situacao = "ENCERRADA";
        alteradaEm = agora.truncatedTo(java.time.temporal.ChronoUnit.MICROS);
    }
}
