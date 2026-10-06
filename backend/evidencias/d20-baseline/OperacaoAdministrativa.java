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
import org.hibernate.annotations.Nationalized;
import org.hibernate.type.SqlTypes;

@Entity
@Table(
        name = "operacao_administrativa",
        schema = "wms",
        uniqueConstraints = {
            @UniqueConstraint(
                    name = "uk_operacao_administrativa_1",
                    columnNames = {"operacao_id"})
        })
public class OperacaoAdministrativa {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "operacao_id", nullable = false, length = 36, updatable = false)
    private String operacaoId;

    @Column(name = "tipo", nullable = false, length = 32, updatable = false)
    private String tipo;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "cliente_id", nullable = true, updatable = false)
    private Cliente cliente;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "armazem_id", nullable = true, updatable = false)
    private Armazem armazem;

    @Column(name = "recurso_id", nullable = true)
    private Long recursoId;

    @Column(name = "conteudo_hash", nullable = false, length = 64, updatable = false)
    private String conteudoHash;

    @Nationalized
    @Column(
            name = "resultado",
            nullable = false,
            columnDefinition = "nvarchar(max)",
            updatable = false)
    private String resultado;

    @Nationalized
    @Column(name = "usuario", nullable = false, length = 200, updatable = false)
    private String usuario;

    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name = "registrada_em", nullable = false, updatable = false)
    private Instant registradaEm;

    protected OperacaoAdministrativa() {}

    public OperacaoAdministrativa(
            String operacaoId,
            String tipo,
            Cliente cliente,
            Armazem armazem,
            Long recursoId,
            String conteudoHash,
            String resultado,
            String usuario,
            Instant registradaEm) {
        this.operacaoId = operacaoId;
        this.tipo = tipo;
        this.cliente = cliente;
        this.armazem = armazem;
        this.recursoId = recursoId;
        this.conteudoHash = conteudoHash;
        this.resultado = resultado;
        this.usuario = usuario;
        this.registradaEm =
                registradaEm == null
                        ? null
                        : registradaEm.truncatedTo(java.time.temporal.ChronoUnit.MICROS);
    }

    public Long getId() {
        return id;
    }

    public String getOperacaoId() {
        return operacaoId;
    }

    public String getTipo() {
        return tipo;
    }

    public Cliente getCliente() {
        return cliente;
    }

    public Armazem getArmazem() {
        return armazem;
    }

    public Long getRecursoId() {
        return recursoId;
    }

    public String getConteudoHash() {
        return conteudoHash;
    }

    public String getResultado() {
        return resultado;
    }

    public String getUsuario() {
        return usuario;
    }

    public Instant getRegistradaEm() {
        return registradaEm;
    }
}
