package br.com.rodogarcia.wms.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Nationalized;
import org.hibernate.type.SqlTypes;

@Entity
@org.hibernate.annotations.DynamicUpdate
@Table(name = "usuario_acesso", schema = "wms")
public class UsuarioAcesso {
    @Id
    @Column(length = 36)
    private String id;

    @Nationalized
    @Column(nullable = false, length = 200)
    private String nome;

    @Nationalized
    @Column(nullable = false, unique = true, length = 254, updatable = false)
    private String email;

    @Column(name = "senha_hash", nullable = false, length = 255)
    private String senhaHash;

    @Column(nullable = false, length = 20)
    private String perfil;

    @Column(nullable = false)
    private boolean administrador;

    @Column(nullable = false, updatable = false)
    private boolean principal;

    @Column(nullable = false)
    private boolean ativo;

    @Column(name = "trocar_senha", nullable = false)
    private boolean trocarSenha;

    @Column(name = "versao_tokens", nullable = false)
    private long versaoTokens;

    @Column(nullable = false)
    private int falhas;

    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name = "bloqueado_ate")
    private Instant bloqueadoAte;

    @Nationalized
    @Column(nullable = false, columnDefinition = "nvarchar(max)")
    private String clientes;

    @Nationalized
    @Column(nullable = false, columnDefinition = "nvarchar(max)")
    private String armazens;

    @Version private long versao;

    protected UsuarioAcesso() {}

    public UsuarioAcesso(
            String id,
            String nome,
            String email,
            String hash,
            String perfil,
            boolean administrador,
            boolean principal,
            String clientes,
            String armazens) {
        this.id = id;
        this.nome = nome;
        this.email = email;
        this.senhaHash = hash;
        this.perfil = perfil;
        this.administrador = administrador;
        this.principal = principal;
        this.clientes = clientes;
        this.armazens = armazens;
        this.ativo = true;
        this.trocarSenha = true;
    }

    public String getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public String getEmail() {
        return email;
    }

    public String getSenhaHash() {
        return senhaHash;
    }

    public String getPerfil() {
        return perfil;
    }

    public boolean isAdministrador() {
        return administrador;
    }

    public boolean isPrincipal() {
        return principal;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public boolean isTrocarSenha() {
        return trocarSenha;
    }

    public long getVersaoTokens() {
        return versaoTokens;
    }

    public Instant getBloqueadoAte() {
        return bloqueadoAte;
    }

    public String getClientes() {
        return clientes;
    }

    public String getArmazens() {
        return armazens;
    }

    public long getVersao() {
        return versao;
    }

    public void falhou(Instant agora) {
        if (bloqueadoAte != null && !bloqueadoAte.isAfter(agora)) falhas = 0;
        falhas++;
        if (falhas >= 5) bloqueadoAte = agora.plusSeconds(900);
    }

    public void limparFalhas() {
        falhas = 0;
        bloqueadoAte = null;
    }

    public void senha(String hash, boolean temporaria) {
        senhaHash = hash;
        trocarSenha = temporaria;
        versaoTokens++;
        limparFalhas();
    }

    public void atualizar(
            String nome,
            String perfil,
            boolean administrador,
            boolean ativo,
            String clientes,
            String armazens) {
        this.nome = nome;
        this.perfil = perfil;
        this.administrador = administrador;
        this.ativo = ativo;
        this.clientes = clientes;
        this.armazens = armazens;
        versaoTokens++;
    }
}
