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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(
        name = "contagem_estoque",
        schema = "wms",
        uniqueConstraints =
                @UniqueConstraint(name = "uk_contagem_unidade", columnNames = "unidade_id"))
public class ContagemEstoque {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "unidade_id", nullable = false, updatable = false)
    private UnidadeLogistica unidade;

    @Version
    @Column(nullable = false)
    private long versao;

    @Column(name = "revisao_atual", nullable = false)
    private int revisaoAtual;

    @Column(nullable = false)
    private boolean impedimento;

    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name = "criada_em", nullable = false, updatable = false)
    private Instant criadaEm;

    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name = "alterada_em", nullable = false)
    private Instant alteradaEm;

    protected ContagemEstoque() {}

    public ContagemEstoque(UnidadeLogistica unidade, Instant instante) {
        this.unidade = unidade;
        criadaEm = instante;
        alteradaEm = instante;
    }

    public void revisar(boolean divergente, Instant instante) {
        revisaoAtual++;
        impedimento = divergente;
        alteradaEm = instante;
    }

    public void reconciliar(Instant instante) {
        impedimento = false;
        alteradaEm = instante;
    }

    public Long getId() {
        return id;
    }

    public UnidadeLogistica getUnidade() {
        return unidade;
    }

    public long getVersao() {
        return versao;
    }

    public int getRevisaoAtual() {
        return revisaoAtual;
    }

    public boolean isImpedimento() {
        return impedimento;
    }
}
