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
        name = "armazem",
        schema = "wms",
        uniqueConstraints = {
            @UniqueConstraint(
                    name = "uk_armazem_1",
                    columnNames = {"codigo"})
        })
public class Armazem extends CadastroBase {
    @Column(nullable = false, length = 30, updatable = false)
    private String codigo;

    @Nationalized
    @Column(nullable = false, length = 120)
    private String nome;

    @Column(name = "documento_fiscal", nullable = false, length = 30, updatable = false)
    private String documentoFiscal;

    @Nationalized
    @Column(nullable = false, length = 100)
    private String cidade;

    @Column(nullable = false, length = 2)
    private String uf;

    @Embedded private ComplementoFiscal complementoFiscal;

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
        registrarAlteracao(agora);
    }

    protected Armazem() {}

    public Armazem(
            String codigo,
            String nome,
            String documentoFiscal,
            String cidade,
            String uf,
            Instant instante) {
        super(instante);
        this.codigo = codigo;
        this.nome = nome;
        this.documentoFiscal = documentoFiscal;
        this.cidade = cidade;
        this.uf = uf;
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

    public String getCidade() {
        return cidade;
    }

    public String getUf() {
        return uf;
    }

    public void alterarDescricao(String descricao, Instant instante) {
        this.nome = descricao;
        registrarAlteracao(instante);
    }
}
