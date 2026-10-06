package br.com.rodogarcia.wms.models;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
        name = "servico_minimo_contrato",
        schema = "wms",
        uniqueConstraints = {
            @UniqueConstraint(
                    name = "uk_servico_minimo_contrato_1",
                    columnNames = {"contrato_id", "servico_id"})
        })
public class ServicoMinimoContrato {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "contrato_id", nullable = false, updatable = false)
    private ContratoCobranca contrato;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "servico_id", nullable = false, updatable = false)
    private ServicoCobranca servico;

    protected ServicoMinimoContrato() {}

    public ServicoMinimoContrato(ContratoCobranca contrato, ServicoCobranca servico) {
        this.contrato = contrato;
        this.servico = servico;
    }

    public Long getId() {
        return id;
    }

    public ContratoCobranca getContrato() {
        return contrato;
    }

    public ServicoCobranca getServico() {
        return servico;
    }
}
