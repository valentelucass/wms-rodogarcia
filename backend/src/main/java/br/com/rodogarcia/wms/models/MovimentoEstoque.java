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
        name = "movimento_estoque",
        schema = "wms",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uk_movimento_operacao",
                        columnNames = {"pedido_id", "operacao_id"}))
public class MovimentoEstoque {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pedido_id", nullable = false, updatable = false)
    private PedidoEntrada pedido;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "unidade_id", nullable = false, updatable = false)
    private UnidadeLogistica unidade;

    @Column(name = "operacao_id", nullable = false, length = 36, updatable = false)
    private String operacaoId;

    @Column(name = "conteudo_hash", nullable = false, length = 64, updatable = false)
    private String conteudoHash;

    @Column(nullable = false, length = 24, updatable = false)
    private String acao;

    @Nationalized
    @Column(nullable = false, length = 200, updatable = false)
    private String usuario;

    @Nationalized
    @Column(nullable = false, length = 500, updatable = false)
    private String motivo;

    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(nullable = false, updatable = false)
    private Instant instante;

    @Nationalized
    @Column(
            name = "dados_antes",
            nullable = false,
            columnDefinition = "nvarchar(max)",
            updatable = false)
    private String dadosAntes;

    @Nationalized
    @Column(nullable = false, columnDefinition = "nvarchar(max)", updatable = false)
    private String resultado;

    protected MovimentoEstoque() {}

    public MovimentoEstoque(
            PedidoEntrada pedido,
            UnidadeLogistica unidade,
            String operacaoId,
            String conteudoHash,
            String acao,
            String usuario,
            String motivo,
            Instant instante,
            String dadosAntes,
            String resultado) {
        this.pedido = pedido;
        this.unidade = unidade;
        this.operacaoId = operacaoId;
        this.conteudoHash = conteudoHash;
        this.acao = acao;
        this.usuario = usuario;
        this.motivo = motivo;
        this.instante = instante;
        this.dadosAntes = dadosAntes;
        this.resultado = resultado;
    }

    public Long getId() {
        return id;
    }

    public String getOperacaoId() {
        return operacaoId;
    }

    public String getConteudoHash() {
        return conteudoHash;
    }

    public String getAcao() {
        return acao;
    }

    public String getUsuario() {
        return usuario;
    }

    public String getMotivo() {
        return motivo;
    }

    public Instant getInstante() {
        return instante;
    }

    public String getDadosAntes() {
        return dadosAntes;
    }

    public String getResultado() {
        return resultado;
    }
}
