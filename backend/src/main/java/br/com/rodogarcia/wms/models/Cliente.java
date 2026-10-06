package br.com.rodogarcia.wms.models;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import org.hibernate.annotations.Nationalized;

@Entity
@Table(
        name = "cliente",
        schema = "wms",
        uniqueConstraints = {
            @UniqueConstraint(
                    name = "uk_cliente_1",
                    columnNames = {"codigo"}),
            @UniqueConstraint(
                    name = "uk_cliente_2",
                    columnNames = {"documento_fiscal"})
        })
public class Cliente extends CadastroBase {
    @Column(nullable = false, length = 30, updatable = false)
    private String codigo;

    @Nationalized
    @Column(nullable = false, length = 120)
    private String nome;

    @Column(name = "documento_fiscal", nullable = false, length = 30, updatable = false)
    private String documentoFiscal;

    @Embedded private ComplementoFiscal complementoFiscal;

    @Nationalized
    @Column(length = 100)
    private String cidade;

    @Column(length = 2)
    private String uf;

    @Column(name = "faturamento_email", length = 160)
    private String faturamentoEmail;

    @Nationalized
    @Column(name = "faturamento_referencia", length = 500)
    private String faturamentoReferencia;

    public String getCidade() {
        return cidade;
    }

    public String getUf() {
        return uf;
    }

    public String getFaturamentoEmail() {
        return faturamentoEmail;
    }

    public String getFaturamentoReferencia() {
        return faturamentoReferencia;
    }

    public ComplementoFiscal getComplementoFiscal() {
        return complementoFiscal;
    }

    public void complementar(
            ComplementoFiscal dados,
            String cidade,
            String uf,
            String email,
            String referencia,
            Instant agora) {
        complementoFiscal = dados;
        this.cidade = cidade;
        this.uf = uf;
        faturamentoEmail = email;
        faturamentoReferencia = referencia;
        registrarAlteracao(agora);
    }

    protected Cliente() {}

    public Cliente(String codigo, String nome, String documentoFiscal, Instant instante) {
        super(instante);
        this.codigo = codigo;
        this.nome = nome;
        this.documentoFiscal = documentoFiscal;
    }

    public String getCodigo() {
        return codigo;
    }

    public String getNome() {
        return nome;
    }

    public String getDocumentoFiscal() {
        return documentoFiscal;
    }

    public void alterarDescricao(String descricao, Instant instante) {
        this.nome = descricao;
        registrarAlteracao(instante);
    }
}
