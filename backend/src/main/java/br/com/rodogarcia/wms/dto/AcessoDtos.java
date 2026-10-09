package br.com.rodogarcia.wms.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;

public final class AcessoDtos {
    private AcessoDtos() {}

    public record Login(
            @NotBlank @Email @Size(max = 254) String email,
            @NotBlank @Size(max = 128) String senha) {
        @Override
        public String toString() {
            return "Login[protegido]";
        }
    }

    public record TrocaSenha(
            @NotBlank @Size(max = 128) String senhaAtual,
            @NotBlank @Size(min = 12, max = 128) String novaSenha) {
        @Override
        public String toString() {
            return "TrocaSenha[protegido]";
        }
    }

    public record CriarUsuario(
            @NotBlank @Size(max = 200) String nome,
            @NotBlank @Email @Size(max = 254) String email,
            @NotBlank @Size(min = 12, max = 128) String senhaTemporaria,
            @NotBlank @Pattern(regexp = "GESTOR|SUPERVISOR|OPERACAO") String perfil,
            boolean administrador,
            @NotNull @Size(max = 500) List<@NotNull @Positive Long> clientes,
            @NotNull @Size(max = 500) List<@NotNull @Positive Long> armazens) {
        @Override
        public String toString() {
            return "CriarUsuario[protegido]";
        }
    }

    public record EditarUsuario(
            @NotBlank @Size(max = 200) String nome,
            @NotBlank @Pattern(regexp = "GESTOR|SUPERVISOR|OPERACAO") String perfil,
            boolean administrador,
            boolean ativo,
            @NotNull @Size(max = 500) List<@NotNull @Positive Long> clientes,
            @NotNull @Size(max = 500) List<@NotNull @Positive Long> armazens,
            long versao) {}

    public record RedefinirSenha(
            @NotBlank @Size(min = 12, max = 128) String senhaTemporaria, long versao) {
        @Override
        public String toString() {
            return "RedefinirSenha[protegido]";
        }
    }

    public record Usuario(
            String id,
            String nome,
            String email,
            String perfil,
            boolean administrador,
            boolean principal,
            boolean ativo,
            boolean trocarSenha,
            List<String> clientes,
            List<String> armazens,
            long versao) {}

    public record Tokens(String accessToken, long expiresIn, Usuario usuario) {
        @Override
        public String toString() {
            return "Tokens[protegido]";
        }
    }

    public record PaginaUsuarios(
            List<Usuario> content, int number, int totalPages, long totalElements) {}
}
