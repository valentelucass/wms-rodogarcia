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
import java.time.LocalDate;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Nationalized;
import org.hibernate.type.SqlTypes;

@Entity
@Table(
        name = "documento_saida",
        schema = "wms",
        uniqueConstraints = {
            @UniqueConstraint(
                    name = "uk_documento_saida_identidade",
                    columnNames = {"emitente_cnpj", "serie", "numero"}),
            @UniqueConstraint(name = "uk_documento_saida_chave", columnNames = "chave_acesso")
        })
public class DocumentoSaida {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pedido_id", nullable = false, updatable = false)
    private PedidoSaida pedido;

    @Column(nullable = false, length = 16, updatable = false)
    private String origem;

    @Column(nullable = false, length = 24, updatable = false)
    private String natureza;

    @Column(name = "emitente_cnpj", nullable = false, length = 14, updatable = false)
    private String emitenteCnpj;

    @Column(nullable = false, length = 3, updatable = false)
    private String serie;

    @Column(nullable = false, length = 9, updatable = false)
    private String numero;

    @Column(nullable = false, updatable = false)
    private LocalDate emissao;

    @Column(name = "chave_acesso", length = 44, updatable = false)
    private String chaveAcesso;

    @Nationalized
    @Column(nullable = false, length = 60, updatable = false)
    private String protocolo;

    @Column(name = "xml_hash", length = 64, updatable = false)
    private String xmlHash;

    @Column(nullable = false, length = 16)
    private String situacao;

    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name = "registrado_em", nullable = false, updatable = false)
    private Instant registradoEm;

    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name = "cancelado_em", nullable = true)
    private Instant canceladoEm;

    protected DocumentoSaida() {}

    public Long getId() {
        return id;
    }

    public DocumentoSaida(
            PedidoSaida pedido,
            String origem,
            String natureza,
            String cnpj,
            String serie,
            String numero,
            LocalDate emissao,
            String chave,
            String protocolo,
            String xmlHash,
            Instant agora) {
        this.pedido = pedido;
        this.origem = origem;
        this.natureza = natureza;
        this.emitenteCnpj = cnpj;
        this.serie = serie;
        this.numero = numero;
        this.emissao = emissao;
        chaveAcesso = chave;
        this.protocolo = protocolo;
        this.xmlHash = xmlHash;
        situacao = "AUTORIZADO";
        registradoEm = agora;
    }

    public void cancelar(Instant agora) {
        situacao = "CANCELADO";
        canceladoEm = agora;
    }

    public PedidoSaida getPedido() {
        return pedido;
    }

    public String getOrigem() {
        return origem;
    }

    public String getNatureza() {
        return natureza;
    }

    public String getEmitenteCnpj() {
        return emitenteCnpj;
    }

    public String getSerie() {
        return serie;
    }

    public String getNumero() {
        return numero;
    }

    public LocalDate getEmissao() {
        return emissao;
    }

    public String getChaveAcesso() {
        return chaveAcesso;
    }

    public String getProtocolo() {
        return protocolo;
    }

    public String getXmlHash() {
        return xmlHash;
    }

    public String getSituacao() {
        return situacao;
    }

    public Instant getRegistradoEm() {
        return registradoEm;
    }

    public Instant getCanceladoEm() {
        return canceladoEm;
    }
}
