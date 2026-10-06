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
        name = "revisao_carga_inicial",
        schema = "wms",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uk_revisao_carga",
                        columnNames = {"carga_id", "numero"}))
public class RevisaoCargaInicial {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "carga_id", nullable = false, updatable = false)
    private CargaInicial carga;

    @Column(nullable = false, updatable = false)
    private int numero;

    @Nationalized
    @Column(
            name = "dados_json",
            nullable = false,
            columnDefinition = "nvarchar(max)",
            updatable = false)
    private String dadosJson;

    @Column(name = "conteudo_hash", nullable = false, length = 64, updatable = false)
    private String conteudoHash;

    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name = "registrada_em", nullable = false, updatable = false)
    private Instant registradaEm;

    @Nationalized
    @Column(nullable = false, length = 200, updatable = false)
    private String usuario;

    @Nationalized
    @Column(nullable = false, length = 500, updatable = false)
    private String motivo;

    protected RevisaoCargaInicial() {}

    public RevisaoCargaInicial(
            CargaInicial carga,
            String dados,
            String hash,
            String usuario,
            String motivo,
            Instant instante) {
        this.carga = carga;
        numero = carga.getRevisaoAtual();
        dadosJson = dados;
        conteudoHash = hash;
        this.usuario = usuario;
        this.motivo = motivo;
        registradaEm = instante;
    }

    public Long getId() {
        return id;
    }

    public int getNumero() {
        return numero;
    }

    public String getDadosJson() {
        return dadosJson;
    }

    public String getConteudoHash() {
        return conteudoHash;
    }
}
