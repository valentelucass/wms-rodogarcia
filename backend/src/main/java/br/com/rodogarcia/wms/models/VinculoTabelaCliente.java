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
        name = "vinculo_tabela_cliente",
        schema = "wms",
        uniqueConstraints = {
            @UniqueConstraint(
                    name = "uk_vinculo_tabela_cliente_1",
                    columnNames = {"cliente_id", "armazem_id", "vigencia_inicio"})
        })
public class VinculoTabelaCliente {
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
    @JoinColumn(name = "tabela_id", nullable = false, updatable = false)
    private TabelaCobranca tabela;

    @Column(name = "vigencia_inicio", nullable = false, updatable = false)
    private LocalDate vigenciaInicio;

    @Column(name = "vigencia_fim", nullable = true)
    private LocalDate vigenciaFim;

    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name = "alterado_em", nullable = false)
    private Instant alteradoEm;

    protected VinculoTabelaCliente() {}

    public VinculoTabelaCliente(
            Cliente cliente,
            Armazem armazem,
            TabelaCobranca tabela,
            LocalDate vigenciaInicio,
            LocalDate vigenciaFim,
            Instant criadoEm,
            Instant alteradoEm) {
        this.cliente = cliente;
        this.armazem = armazem;
        this.tabela = tabela;
        this.vigenciaInicio = vigenciaInicio;
        this.vigenciaFim = vigenciaFim;
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

    public TabelaCobranca getTabela() {
        return tabela;
    }

    public LocalDate getVigenciaInicio() {
        return vigenciaInicio;
    }

    public LocalDate getVigenciaFim() {
        return vigenciaFim;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }

    public Instant getAlteradoEm() {
        return alteradoEm;
    }

    public void encerrar(LocalDate fim, Instant agora) {
        vigenciaFim = fim;
        alteradoEm = agora.truncatedTo(java.time.temporal.ChronoUnit.MICROS);
    }
}
