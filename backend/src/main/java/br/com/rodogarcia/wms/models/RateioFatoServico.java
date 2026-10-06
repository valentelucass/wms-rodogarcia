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
import java.math.BigDecimal;

@Entity
@Table(
        name = "rateio_fato_servico",
        schema = "wms",
        uniqueConstraints = {
            @UniqueConstraint(
                    name = "uk_rateio_fato_servico_1",
                    columnNames = {"fato_id", "nota_id"})
        })
public class RateioFatoServico {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "fato_id", nullable = false, updatable = false)
    private FatoServico fato;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "nota_id", nullable = false, updatable = false)
    private NotaEntrada nota;

    @Column(name = "cota", nullable = false, precision = 19, scale = 6, updatable = false)
    private BigDecimal cota;

    protected RateioFatoServico() {}

    public RateioFatoServico(FatoServico fato, NotaEntrada nota, BigDecimal cota) {
        this.fato = fato;
        this.nota = nota;
        this.cota = cota;
    }

    public Long getId() {
        return id;
    }

    public FatoServico getFato() {
        return fato;
    }

    public NotaEntrada getNota() {
        return nota;
    }

    public BigDecimal getCota() {
        return cota;
    }
}
