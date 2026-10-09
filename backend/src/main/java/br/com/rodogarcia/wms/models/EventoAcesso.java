package br.com.rodogarcia.wms.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Apenas IDs, ação e data. Nunca senha, hash de senha, JWT ou token de renovação. */
@Entity
@Table(name = "evento_acesso", schema = "wms")
public class EventoAcesso {
    @Id
    @Column(length = 36)
    private String id;

    @Column(nullable = false, length = 36)
    private String ator;

    @Column(nullable = false, length = 36)
    private String alvo;

    @Column(nullable = false, length = 40)
    private String acao;

    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(nullable = false)
    private Instant instante;

    protected EventoAcesso() {}

    public EventoAcesso(String id, String ator, String alvo, String acao, Instant instante) {
        this.id = id;
        this.ator = ator;
        this.alvo = alvo;
        this.acao = acao;
        this.instante = instante;
    }
}
