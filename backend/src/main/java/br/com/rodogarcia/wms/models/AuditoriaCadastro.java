package br.com.rodogarcia.wms.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Nationalized;
import org.hibernate.type.SqlTypes;

@Entity
@Table(
        name = "auditoria_cadastro",
        schema = "wms",
        indexes = {@Index(name = "ix_auditoria_registro", columnList = "tipo,registro_id,id")})
public class AuditoriaCadastro {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20, updatable = false)
    private String tipo;

    @Column(name = "registro_id", nullable = false, updatable = false)
    private Long registroId;

    @Column(nullable = false, length = 30, updatable = false)
    private String acao;

    @Nationalized
    @Column(nullable = false, length = 200, updatable = false)
    private String usuario;

    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(nullable = false, updatable = false)
    private Instant instante;

    @Column(name = "id_operacao", nullable = false, length = 36, updatable = false)
    private String idOperacao;

    @Nationalized
    @Column(nullable = false, length = 500, updatable = false)
    private String motivo;

    @Nationalized
    @Column(name = "dados_antes", columnDefinition = "nvarchar(max)", updatable = false)
    private String dadosAntes;

    @Nationalized
    @Column(
            name = "dados_depois",
            nullable = false,
            columnDefinition = "nvarchar(max)",
            updatable = false)
    private String dadosDepois;

    protected AuditoriaCadastro() {}

    public AuditoriaCadastro(
            String tipo,
            Long registroId,
            String acao,
            String usuario,
            Instant instante,
            String idOperacao,
            String motivo,
            String antes,
            String depois) {
        this.tipo = tipo;
        this.registroId = registroId;
        this.acao = acao;
        this.usuario = usuario;
        this.instante = instante;
        this.idOperacao = idOperacao;
        this.motivo = motivo;
        this.dadosAntes = antes;
        this.dadosDepois = depois;
    }

    public Long getId() {
        return id;
    }

    public String getTipo() {
        return tipo;
    }

    public Long getRegistroId() {
        return registroId;
    }

    public String getAcao() {
        return acao;
    }

    public String getUsuario() {
        return usuario;
    }

    public Instant getInstante() {
        return instante;
    }

    public String getIdOperacao() {
        return idOperacao;
    }

    public String getMotivo() {
        return motivo;
    }

    public String getDadosAntes() {
        return dadosAntes;
    }

    public String getDadosDepois() {
        return dadosDepois;
    }
}
