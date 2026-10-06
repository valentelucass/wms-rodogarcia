package br.com.rodogarcia.wms.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Nationalized;
import org.hibernate.type.SqlTypes;

@Entity
@Table(
        name = "revisao_contagem",
        schema = "wms",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uk_revisao_contagem",
                        columnNames = {"contagem_id", "numero"}))
public class RevisaoContagem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "contagem_id", nullable = false, updatable = false)
    private ContagemEstoque contagem;

    @Column(nullable = false, updatable = false)
    private int numero;

    @Column(name = "versao_unidade", nullable = false, updatable = false)
    private long versaoUnidade;

    @Column(name = "revisao_conteudo", nullable = false, updatable = false)
    private long revisaoConteudo;

    @Column(nullable = false, precision = 19, scale = 6, updatable = false)
    private BigDecimal esperado;

    @Column(nullable = false, precision = 19, scale = 6, updatable = false)
    private BigDecimal contado;

    @Column(nullable = false, precision = 19, scale = 6, updatable = false)
    private BigDecimal diferenca;

    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name = "observado_em", nullable = false, updatable = false)
    private Instant observadoEm;

    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name = "registrada_em", nullable = false, updatable = false)
    private Instant registradaEm;

    @Nationalized
    @Column(nullable = false, length = 200, updatable = false)
    private String usuario;

    @Nationalized
    @Column(nullable = false, length = 500, updatable = false)
    private String motivo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private SituacaoRevisaoContagem situacao;

    @Nationalized
    @Column(
            name = "origens_json",
            nullable = false,
            columnDefinition = "nvarchar(max)",
            updatable = false)
    private String origensJson;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reserva_id", updatable = false)
    private PedidoSaida reserva;

    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name = "aplicada_em")
    private Instant aplicadaEm;

    @Nationalized
    @Column(name = "efeito_json", columnDefinition = "nvarchar(max)")
    private String efeitoJson;

    protected RevisaoContagem() {}

    public RevisaoContagem(
            ContagemEstoque c,
            BigDecimal contado,
            Instant observado,
            Instant registrada,
            String usuario,
            String motivo,
            String origens) {
        var u = c.getUnidade();
        contagem = c;
        numero = c.getRevisaoAtual();
        versaoUnidade = u.getVersao();
        revisaoConteudo = u.getRevisaoConteudo();
        esperado = u.getQuantidade();
        this.contado = contado;
        diferenca = contado.subtract(esperado);
        observadoEm = observado;
        registradaEm = registrada;
        this.usuario = usuario;
        this.motivo = motivo;
        origensJson = origens;
        reserva = u.getReservaSaida();
        situacao =
                diferenca.signum() == 0
                        ? SituacaoRevisaoContagem.RECONCILIADA
                        : reserva == null
                                ? SituacaoRevisaoContagem.PENDENTE
                                : SituacaoRevisaoContagem.PENDENTE_RESERVA;
    }

    public void substituir() {
        if (situacao == SituacaoRevisaoContagem.PENDENTE
                || situacao == SituacaoRevisaoContagem.PENDENTE_RESERVA)
            situacao = SituacaoRevisaoContagem.SUBSTITUIDA;
    }

    public void aplicar(Instant instante, String efeito) {
        if (situacao != SituacaoRevisaoContagem.PENDENTE
                && situacao != SituacaoRevisaoContagem.PENDENTE_RESERVA)
            throw new IllegalStateException("Revisão não aplicável.");
        situacao = SituacaoRevisaoContagem.APLICADA;
        aplicadaEm = instante;
        efeitoJson = efeito;
    }

    public Long getId() {
        return id;
    }

    public ContagemEstoque getContagem() {
        return contagem;
    }

    public int getNumero() {
        return numero;
    }

    public long getVersaoUnidade() {
        return versaoUnidade;
    }

    public long getRevisaoConteudo() {
        return revisaoConteudo;
    }

    public BigDecimal getEsperado() {
        return esperado;
    }

    public BigDecimal getContado() {
        return contado;
    }

    public BigDecimal getDiferenca() {
        return diferenca;
    }

    public Instant getObservadoEm() {
        return observadoEm;
    }

    public SituacaoRevisaoContagem getSituacao() {
        return situacao;
    }

    public String getOrigensJson() {
        return origensJson;
    }

    public String getEfeitoJson() {
        return efeitoJson;
    }

    public Instant getAplicadaEm() {
        return aplicadaEm;
    }

    public PedidoSaida getReserva() {
        return reserva;
    }
}
