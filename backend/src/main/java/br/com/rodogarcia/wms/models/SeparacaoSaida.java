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
        name = "separacao_saida",
        schema = "wms",
        uniqueConstraints =
                @UniqueConstraint(name = "uk_separacao_reserva", columnNames = "reserva_id"))
public class SeparacaoSaida {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reserva_id", nullable = false, updatable = false)
    private ReservaSaida reserva;

    @Column(name = "versao_unidade_lida", nullable = false)
    private long versaoUnidadeLida;

    @Column(name = "revisao_conteudo_lida", nullable = false)
    private long revisaoConteudoLida;

    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name = "lida_em", nullable = false)
    private Instant lidaEm;

    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name = "separada_em", nullable = true)
    private Instant separadaEm;

    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name = "encerrada_em", nullable = true)
    private Instant encerradaEm;

    @Column(nullable = false, length = 16)
    private String situacao;

    @Nationalized
    @Column(name = "origens_json", columnDefinition = "nvarchar(max)")
    private String origensJson;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conjunto_origem_id")
    private ConjuntoPosicoes conjuntoOrigem;

    protected SeparacaoSaida() {}

    public Long getId() {
        return id;
    }

    public SeparacaoSaida(ReservaSaida reserva, Instant agora) {
        this.reserva = reserva;
        ler(agora);
        situacao = "LEITURA";
    }

    public void ler(Instant agora) {
        versaoUnidadeLida = reserva.getUnidade().getVersao();
        revisaoConteudoLida = reserva.getUnidade().getRevisaoConteudo();
        lidaEm = agora;
    }

    public void separar(String origens, ConjuntoPosicoes conjunto, Instant agora) {
        origensJson = origens;
        conjuntoOrigem = conjunto;
        separadaEm = agora;
        situacao = "SEPARADA";
    }

    public void encerrar(String situacao, Instant agora) {
        this.situacao = situacao;
        encerradaEm = agora;
    }

    public void exigirNovaLeitura() {
        situacao = "LEITURA";
        versaoUnidadeLida = -1;
    }

    public ReservaSaida getReserva() {
        return reserva;
    }

    public long getVersaoUnidadeLida() {
        return versaoUnidadeLida;
    }

    public long getRevisaoConteudoLida() {
        return revisaoConteudoLida;
    }

    public String getSituacao() {
        return situacao;
    }

    public Instant getLidaEm() {
        return lidaEm;
    }

    public Instant getSeparadaEm() {
        return separadaEm;
    }

    public Instant getEncerradaEm() {
        return encerradaEm;
    }

    public String getOrigensJson() {
        return origensJson;
    }

    public ConjuntoPosicoes getConjuntoOrigem() {
        return conjuntoOrigem;
    }
}
