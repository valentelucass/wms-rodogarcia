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
        name = "cobertura_documento_saida",
        schema = "wms",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uk_cobertura_documento_reserva",
                        columnNames = {"documento_id", "reserva_id"}))
public class CoberturaDocumentoSaida {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "documento_id", nullable = false, updatable = false)
    private DocumentoSaida documento;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reserva_id", nullable = false, updatable = false)
    private ReservaSaida reserva;

    @Column(name = "quantidade", nullable = false, precision = 19, scale = 6, updatable = false)
    private BigDecimal quantidade;

    protected CoberturaDocumentoSaida() {}

    public Long getId() {
        return id;
    }

    public CoberturaDocumentoSaida(
            DocumentoSaida documento, ReservaSaida reserva, BigDecimal quantidade) {
        this.documento = documento;
        this.reserva = reserva;
        this.quantidade = quantidade;
    }

    public DocumentoSaida getDocumento() {
        return documento;
    }

    public ReservaSaida getReserva() {
        return reserva;
    }

    public BigDecimal getQuantidade() {
        return quantidade;
    }
}
