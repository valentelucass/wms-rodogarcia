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
        name = "pedido_entrada",
        schema = "wms",
        uniqueConstraints = {
            @UniqueConstraint(
                    name = "uk_pedido_entrada_1",
                    columnNames = {"cliente_id", "armazem_id", "referencia"})
        })
public class PedidoEntrada {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cliente_id", nullable = false, updatable = false)
    private Cliente cliente;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "armazem_id", nullable = false, updatable = false)
    private Armazem armazem;

    @Column(name = "referencia", nullable = false, length = 40)
    private String referencia;

    @Enumerated(EnumType.STRING)
    @Column(name = "situacao", nullable = false, length = 24)
    private SituacaoPedidoEntrada situacao;

    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm;

    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name = "alterado_em", nullable = false)
    private Instant alteradoEm;

    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name = "efetivado_em", nullable = true)
    private Instant efetivadoEm;

    @Nationalized
    @Column(name = "motivo_conclusao", nullable = true, length = 500)
    private String motivoConclusao;

    protected PedidoEntrada() {}

    public Long getId() {
        return id;
    }

    public Cliente getCliente() {
        return cliente;
    }

    public Armazem getArmazem() {
        return armazem;
    }

    public String getReferencia() {
        return referencia;
    }

    public SituacaoPedidoEntrada getSituacao() {
        return situacao;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }

    public Instant getAlteradoEm() {
        return alteradoEm;
    }

    public Instant getEfetivadoEm() {
        return efetivadoEm;
    }

    public String getMotivoConclusao() {
        return motivoConclusao;
    }

    @Version
    @Column(nullable = false)
    private long versao;

    public long getVersao() {
        return versao;
    }

    public PedidoEntrada(Cliente cliente, Armazem armazem, String referencia, Instant agora) {
        this.cliente = cliente;
        this.armazem = armazem;
        this.referencia = referencia;
        this.criadoEm = agora;
        this.alteradoEm = agora;
        this.situacao = SituacaoPedidoEntrada.RASCUNHO;
    }

    public void atualizar(SituacaoPedidoEntrada situacao, Instant agora) {
        this.situacao = situacao;
        this.alteradoEm = agora.isAfter(alteradoEm) ? agora : alteradoEm.plusNanos(1000);
    }

    public void concluir(String motivo, Instant agora) {
        concluir(motivo, agora, agora);
    }

    public void concluir(String motivo, Instant efetivadaEm, Instant registradaEm) {
        if (efetivadaEm == null || registradaEm == null || efetivadaEm.isAfter(registradaEm))
            throw new IllegalArgumentException("Efetivação física deve anteceder seu registro.");
        atualizar(SituacaoPedidoEntrada.EFETIVADO, registradaEm);
        this.motivoConclusao = motivo;
        this.efetivadoEm = efetivadaEm;
    }
}
