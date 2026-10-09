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
@Table(name = "sessao_acesso", schema = "wms")
public class SessaoAcesso {
    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "usuario_id", nullable = false, length = 36)
    private String usuarioId;

    @Column(name = "versao_tokens", nullable = false)
    private long versaoTokens;

    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(nullable = false)
    private Instant expira;

    @Column(nullable = false)
    private boolean revogada;

    protected SessaoAcesso() {}

    public SessaoAcesso(String id, String usuarioId, long versaoTokens, Instant expira) {
        this.id = id;
        this.usuarioId = usuarioId;
        this.versaoTokens = versaoTokens;
        this.expira = expira;
    }

    public String getId() {
        return id;
    }

    public String getUsuarioId() {
        return usuarioId;
    }

    public long getVersaoTokens() {
        return versaoTokens;
    }

    public Instant getExpira() {
        return expira;
    }

    public boolean isRevogada() {
        return revogada;
    }

    public void revogar() {
        revogada = true;
    }
}
