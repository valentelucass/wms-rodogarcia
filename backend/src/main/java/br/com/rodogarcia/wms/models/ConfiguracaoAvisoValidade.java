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
        name = "configuracao_aviso_validade",
        schema = "wms",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uk_aviso_contexto",
                        columnNames = {"cliente_id", "armazem_id"}))
public class ConfiguracaoAvisoValidade {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cliente_id", nullable = false, updatable = false)
    private Cliente cliente;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "armazem_id", nullable = false, updatable = false)
    private Armazem armazem;

    @Version
    @Column(nullable = false)
    private long versao;

    @Column(name = "dias_antecedencia", nullable = false)
    private int diasAntecedencia;

    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name = "alterada_em", nullable = false)
    private Instant alteradaEm;

    protected ConfiguracaoAvisoValidade() {}

    public ConfiguracaoAvisoValidade(Cliente cliente, Armazem armazem, int dias, Instant instante) {
        this.cliente = cliente;
        this.armazem = armazem;
        configurar(dias, instante);
    }

    public void configurar(int dias, Instant instante) {
        if (dias < 0) throw new IllegalArgumentException("Antecedência negativa.");
        diasAntecedencia = dias;
        alteradaEm = instante;
    }

    public Long getId() {
        return id;
    }

    public long getVersao() {
        return versao;
    }

    public int getDiasAntecedencia() {
        return diasAntecedencia;
    }
}
