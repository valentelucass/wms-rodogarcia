package br.com.rodogarcia.wms.models;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import org.hibernate.annotations.Nationalized;

@Embeddable
public class ComplementoFiscal {
    @Nationalized
    @Column(name = "razao_social", length = 160)
    private String razaoSocial;

    @Column(name = "inscricao_estadual", length = 30)
    private String inscricaoEstadual;

    @Nationalized
    @Column(name = "logradouro", length = 160)
    private String logradouro;

    @Column(name = "numero_endereco", length = 20)
    private String numeroEndereco;

    @Nationalized
    @Column(name = "complemento", length = 80)
    private String complemento;

    @Nationalized
    @Column(name = "bairro", length = 80)
    private String bairro;

    @Column(name = "cep", length = 16)
    private String cep;

    @Column(name = "pais", length = 2)
    private String pais;

    @Nationalized
    @Column(name = "contato_nome", length = 120)
    private String contatoNome;

    @Column(name = "contato_email", length = 160)
    private String contatoEmail;

    @Column(name = "contato_telefone", length = 30)
    private String contatoTelefone;

    protected ComplementoFiscal() {}

    public ComplementoFiscal(
            String razaoSocial,
            String inscricaoEstadual,
            String logradouro,
            String numeroEndereco,
            String complemento,
            String bairro,
            String cep,
            String pais,
            String contatoNome,
            String contatoEmail,
            String contatoTelefone) {
        this.razaoSocial = razaoSocial;
        this.inscricaoEstadual = inscricaoEstadual;
        this.logradouro = logradouro;
        this.numeroEndereco = numeroEndereco;
        this.complemento = complemento;
        this.bairro = bairro;
        this.cep = cep;
        this.pais = pais;
        this.contatoNome = contatoNome;
        this.contatoEmail = contatoEmail;
        this.contatoTelefone = contatoTelefone;
    }

    public String getRazaoSocial() {
        return razaoSocial;
    }

    public String getInscricaoEstadual() {
        return inscricaoEstadual;
    }

    public String getLogradouro() {
        return logradouro;
    }

    public String getNumeroEndereco() {
        return numeroEndereco;
    }

    public String getComplemento() {
        return complemento;
    }

    public String getBairro() {
        return bairro;
    }

    public String getCep() {
        return cep;
    }

    public String getPais() {
        return pais;
    }

    public String getContatoNome() {
        return contatoNome;
    }

    public String getContatoEmail() {
        return contatoEmail;
    }

    public String getContatoTelefone() {
        return contatoTelefone;
    }
}
