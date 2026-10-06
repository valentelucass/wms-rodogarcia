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
import java.time.LocalDate;

@Entity
@Table(
        name = "dia_fechamento",
        schema = "wms",
        uniqueConstraints = {
            @UniqueConstraint(
                    name = "uk_dia_fechamento_1",
                    columnNames = {"cliente_id", "armazem_id", "data"}),
            @UniqueConstraint(
                    name = "uk_dia_fechamento_2",
                    columnNames = {"fechamento_id", "data"})
        })
public class DiaFechamento {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "fechamento_id", nullable = false, updatable = false)
    private FechamentoCobranca fechamento;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cliente_id", nullable = false, updatable = false)
    private Cliente cliente;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "armazem_id", nullable = false, updatable = false)
    private Armazem armazem;

    @Column(name = "data", nullable = false, updatable = false)
    private LocalDate data;

    protected DiaFechamento() {}

    public DiaFechamento(
            FechamentoCobranca fechamento, Cliente cliente, Armazem armazem, LocalDate data) {
        this.fechamento = fechamento;
        this.cliente = cliente;
        this.armazem = armazem;
        this.data = data;
    }

    public Long getId() {
        return id;
    }

    public FechamentoCobranca getFechamento() {
        return fechamento;
    }

    public Cliente getCliente() {
        return cliente;
    }

    public Armazem getArmazem() {
        return armazem;
    }

    public LocalDate getData() {
        return data;
    }
}
