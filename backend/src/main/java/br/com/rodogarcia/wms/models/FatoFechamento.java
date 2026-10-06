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
import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(
        name = "fato_fechamento",
        schema = "wms",
        uniqueConstraints = {
            @UniqueConstraint(
                    name = "uk_fato_fechamento_1",
                    columnNames = {"fato_id"})
        })
public class FatoFechamento {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "fechamento_id", nullable = false, updatable = false)
    private FechamentoCobranca fechamento;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "fato_id", nullable = false, updatable = false)
    private FatoServico fato;

    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name = "registrado_em", nullable = false, updatable = false)
    private Instant registradoEm;

    protected FatoFechamento() {}

    public FatoFechamento(FechamentoCobranca fechamento, FatoServico fato, Instant registradoEm) {
        this.fechamento = fechamento;
        this.fato = fato;
        this.registradoEm =
                registradoEm == null
                        ? null
                        : registradoEm.truncatedTo(java.time.temporal.ChronoUnit.MICROS);
    }

    public Long getId() {
        return id;
    }

    public FechamentoCobranca getFechamento() {
        return fechamento;
    }

    public FatoServico getFato() {
        return fato;
    }

    public Instant getRegistradoEm() {
        return registradoEm;
    }
}
