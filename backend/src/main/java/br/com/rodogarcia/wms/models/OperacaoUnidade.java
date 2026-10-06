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
        name = "operacao_unidade",
        schema = "wms",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uk_operacao_unidade",
                        columnNames = {"pedido_id", "operacao_id"}))
public class OperacaoUnidade {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pedido_id", nullable = false, updatable = false)
    private PedidoEntrada pedido;

    @Column(name = "operacao_id", nullable = false, length = 36, updatable = false)
    private String operacaoId;

    @Column(name = "conteudo_hash", nullable = false, length = 64, updatable = false)
    private String conteudoHash;

    @Column(nullable = false, length = 24, updatable = false)
    private String tipo;

    @Nationalized
    @Column(nullable = false, length = 200, updatable = false)
    private String usuario;

    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name = "registrada_em", nullable = false, updatable = false)
    private Instant registradaEm;

    @Nationalized
    @Column(nullable = false, columnDefinition = "nvarchar(max)", updatable = false)
    private String resultado;

    protected OperacaoUnidade() {}

    public OperacaoUnidade(
            PedidoEntrada pedido,
            String operacaoId,
            String conteudoHash,
            String tipo,
            String usuario,
            Instant registradaEm,
            String resultado) {
        this.pedido = pedido;
        this.operacaoId = operacaoId;
        this.conteudoHash = conteudoHash;
        this.tipo = tipo;
        this.usuario = usuario;
        this.registradaEm = registradaEm;
        this.resultado = resultado;
    }

    public String getConteudoHash() {
        return conteudoHash;
    }

    public String getResultado() {
        return resultado;
    }

    public Instant getRegistradaEm() {
        return registradaEm;
    }
}
