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
import org.hibernate.annotations.Nationalized;

@Entity
@Table(
        name = "nota_entrada",
        schema = "wms",
        uniqueConstraints = {
            @UniqueConstraint(
                    name = "uk_nota_entrada_1",
                    columnNames = {"emitente", "serie", "numero"})
        })
public class NotaEntrada {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pedido_id", nullable = false, updatable = false)
    private PedidoEntrada pedido;

    @Column(name = "emitente", nullable = false, length = 30)
    private String emitente;

    @Column(name = "serie", nullable = false)
    private int serie;

    @Column(name = "numero", nullable = false)
    private long numero;

    @Column(name = "emissao", nullable = false)
    private LocalDate emissao;

    @Column(name = "chave_acesso", nullable = true, length = 44, unique = true)
    private String chaveAcesso;

    @Column(name = "xml_hash", nullable = true, length = 64)
    private String xmlHash;

    @Nationalized
    @Column(name = "xml_original", nullable = true, columnDefinition = "nvarchar(max)")
    private String xmlOriginal;

    protected NotaEntrada() {}

    public NotaEntrada(
            PedidoEntrada pedido,
            String emitente,
            int serie,
            long numero,
            LocalDate emissao,
            String chaveAcesso) {
        this.pedido = pedido;
        this.emitente = emitente;
        this.serie = serie;
        this.numero = numero;
        this.emissao = emissao;
        this.chaveAcesso = chaveAcesso;
    }

    public Long getId() {
        return id;
    }

    public PedidoEntrada getPedido() {
        return pedido;
    }

    public String getEmitente() {
        return emitente;
    }

    public int getSerie() {
        return serie;
    }

    public long getNumero() {
        return numero;
    }

    public LocalDate getEmissao() {
        return emissao;
    }

    public String getChaveAcesso() {
        return chaveAcesso;
    }

    public String getXmlHash() {
        return xmlHash;
    }

    public String getXmlOriginal() {
        return xmlOriginal;
    }

    public void vincularXml(String chave, String hash, String xml) {
        this.chaveAcesso = chave;
        this.xmlHash = hash;
        this.xmlOriginal = xml;
    }
}
