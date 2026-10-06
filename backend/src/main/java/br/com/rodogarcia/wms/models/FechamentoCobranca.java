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
import org.hibernate.type.SqlTypes;

@Entity
@Table(
        name = "fechamento_cobranca",
        schema = "wms",
        uniqueConstraints = {
            @UniqueConstraint(
                    name = "uk_fechamento_cobranca_1",
                    columnNames = {"cliente_id", "armazem_id", "periodo_inicio"})
        })
public class FechamentoCobranca {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    @Column(name = "versao", nullable = false)
    private long versao;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cliente_id", nullable = false, updatable = false)
    private Cliente cliente;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "armazem_id", nullable = false, updatable = false)
    private Armazem armazem;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "contrato_id", nullable = false, updatable = false)
    private ContratoCobranca contrato;

    @Column(name = "periodo_inicio", nullable = false, updatable = false)
    private LocalDate periodoInicio;

    @Column(name = "periodo_fim", nullable = false, updatable = false)
    private LocalDate periodoFim;

    @Column(name = "situacao", nullable = false, updatable = true, length = 24)
    private String situacao;

    @Column(name = "versao_atual", nullable = false, updatable = true)
    private int versaoAtual;

    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name = "alterado_em", nullable = false, updatable = true)
    private Instant alteradoEm;

    protected FechamentoCobranca() {}

    public FechamentoCobranca(
            Cliente cliente,
            Armazem armazem,
            ContratoCobranca contrato,
            LocalDate periodoInicio,
            LocalDate periodoFim,
            String situacao,
            int versaoAtual,
            Instant criadoEm,
            Instant alteradoEm) {
        this.cliente = cliente;
        this.armazem = armazem;
        this.contrato = contrato;
        this.periodoInicio = periodoInicio;
        this.periodoFim = periodoFim;
        this.situacao = situacao;
        this.versaoAtual = versaoAtual;
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

    public Cliente getCliente() {
        return cliente;
    }

    public Armazem getArmazem() {
        return armazem;
    }

    public ContratoCobranca getContrato() {
        return contrato;
    }

    public LocalDate getPeriodoInicio() {
        return periodoInicio;
    }

    public LocalDate getPeriodoFim() {
        return periodoFim;
    }

    public String getSituacao() {
        return situacao;
    }

    public int getVersaoAtual() {
        return versaoAtual;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }

    public Instant getAlteradoEm() {
        return alteradoEm;
    }

    public void atualizar(String situacao, int numero, Instant agora) {
        this.situacao = situacao;
        versaoAtual = numero;
        alteradoEm = agora;
    }
}
