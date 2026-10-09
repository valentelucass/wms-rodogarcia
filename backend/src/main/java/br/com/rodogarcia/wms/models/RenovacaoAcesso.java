package br.com.rodogarcia.wms.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@org.hibernate.annotations.DynamicUpdate
@Table(name = "renovacao_acesso", schema = "wms")
public class RenovacaoAcesso {
    @Id
    @Column(length = 64)
    private String hash;

    @Column(name = "sessao_id", nullable = false, length = 36)
    private String sessaoId;

    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(nullable = false)
    private Instant expira;

    @Column(nullable = false)
    private boolean usado;

    protected RenovacaoAcesso() {}

    public RenovacaoAcesso(String hash, String sessaoId, Instant expira) {
        this.hash = hash;
        this.sessaoId = sessaoId;
        this.expira = expira;
    }

    public String getSessaoId() {
        return sessaoId;
    }

    public Instant getExpira() {
        return expira;
    }

    public boolean isUsado() {
        return usado;
    }

    public void consumir() {
        usado = true;
    }
}
