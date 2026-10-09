package br.com.rodogarcia.wms.services;

import br.com.rodogarcia.wms.config.IdentidadeProperties;
import br.com.rodogarcia.wms.dto.AcessoDtos;
import br.com.rodogarcia.wms.exceptions.FalhaLoginException;
import br.com.rodogarcia.wms.exceptions.RegraNegocioException;
import br.com.rodogarcia.wms.models.EventoAcesso;
import br.com.rodogarcia.wms.models.RenovacaoAcesso;
import br.com.rodogarcia.wms.models.SessaoAcesso;
import br.com.rodogarcia.wms.models.UsuarioAcesso;
import br.com.rodogarcia.wms.repositories.ArmazemRepository;
import br.com.rodogarcia.wms.repositories.ClienteRepository;
import br.com.rodogarcia.wms.repositories.EventoAcessoRepository;
import br.com.rodogarcia.wms.repositories.RenovacaoAcessoRepository;
import br.com.rodogarcia.wms.repositories.SessaoAcessoRepository;
import br.com.rodogarcia.wms.repositories.UsuarioAcessoRepository;
import com.nimbusds.jose.jwk.RSAKey;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
@ConditionalOnProperty(name = "wms.auth.enabled", havingValue = "true")
public class LoginService {
    public static final String PRINCIPAL_EMAIL = "desenvolvedor@rodogarcia.com.br";
    public static final String PRINCIPAL_ID = "00000000-0000-4000-8000-000000000001";
    private final UsuarioAcessoRepository usuarios;
    private final SessaoAcessoRepository sessoes;
    private final RenovacaoAcessoRepository renovacoes;
    private final EventoAcessoRepository eventos;
    private final ClienteRepository clientes;
    private final ArmazemRepository armazens;
    private final PasswordEncoder senhas;
    private final JwtEncoder encoder;
    private final IdentidadeProperties identidade;
    private final String hashInexistente;
    private final SecureRandom random = new SecureRandom();
    private final RSAKey chave;

    public LoginService(
            UsuarioAcessoRepository usuarios,
            SessaoAcessoRepository sessoes,
            RenovacaoAcessoRepository renovacoes,
            EventoAcessoRepository eventos,
            ClienteRepository clientes,
            ArmazemRepository armazens,
            PasswordEncoder senhas,
            JwtEncoder encoder,
            IdentidadeProperties identidade,
            RSAKey chave) {
        this.usuarios = usuarios;
        this.sessoes = sessoes;
        this.renovacoes = renovacoes;
        this.eventos = eventos;
        this.clientes = clientes;
        this.armazens = armazens;
        this.senhas = senhas;
        this.encoder = encoder;
        this.identidade = identidade;
        this.chave = chave;
        this.hashInexistente = senhas.encode(UUID.randomUUID().toString());
    }

    public record Entrada(AcessoDtos.Tokens resposta, String renovacao) {
        @Override
        public String toString() {
            return "Entrada[protegido]";
        }
    }

    public Map<String, Object> chavesPublicas() {
        return Map.of("keys", List.of(chave.toPublicJWK().toJSONObject()));
    }

    @Transactional
    public void iniciarPrincipal(String hash) {
        var existente = usuarios.findById(PRINCIPAL_ID);
        if (existente.isPresent()) {
            var u = existente.get();
            if (!u.isPrincipal()
                    || !u.isAtivo()
                    || !u.isAdministrador()
                    || !PRINCIPAL_EMAIL.equals(u.getEmail())
                    || !"GESTOR".equals(u.getPerfil())) {
                throw new IllegalStateException("AUTH_PRINCIPAL_INCONSISTENTE");
            }
            return;
        }
        if (hash == null || !hash.matches("\\{pbkdf2-600k\\}[0-9a-f]{96}")) {
            throw new IllegalStateException("AUTH_BOOTSTRAP_PROTEGIDO_NECESSARIO");
        }
        usuarios.save(
                new UsuarioAcesso(
                        PRINCIPAL_ID,
                        "Administrador principal",
                        PRINCIPAL_EMAIL,
                        hash,
                        "GESTOR",
                        true,
                        true,
                        "",
                        ""));
        auditar(PRINCIPAL_ID, PRINCIPAL_ID, "PRINCIPAL_CRIADO");
    }

    @Transactional(noRollbackFor = FalhaLoginException.class)
    public Entrada entrar(@Valid @NotNull AcessoDtos.Login pedido, String sessaoAnterior) {
        var u = usuarios.porEmailComLock(normalizarEmail(pedido.email())).orElse(null);
        boolean confere =
                senhas.matches(pedido.senha(), u == null ? hashInexistente : u.getSenhaHash());
        Instant agora = Instant.now();
        if (u == null) throw new FalhaLoginException();
        if (!u.isAtivo() || (u.getBloqueadoAte() != null && u.getBloqueadoAte().isAfter(agora))) {
            throw new FalhaLoginException();
        }
        if (!confere) {
            u.falhou(agora);
            auditar(u.getId(), u.getId(), "LOGIN_RECUSADO");
            throw new FalhaLoginException();
        }
        u.limparFalhas();
        sair(sessaoAnterior);
        var s =
                sessoes.save(
                        new SessaoAcesso(
                                UUID.randomUUID().toString(),
                                u.getId(),
                                u.getVersaoTokens(),
                                agora.plusSeconds(u.isTrocarSenha() ? 900 : 28800)));
        auditar(u.getId(), u.getId(), "LOGIN");
        return emitir(u, s);
    }

    @Transactional(noRollbackFor = FalhaLoginException.class)
    public Entrada renovar(String token) {
        String hash = hashToken(token);
        String sid = renovacoes.sessaoDoToken(hash).orElseThrow(FalhaLoginException::new);
        String uid = sessoes.usuarioDaSessao(sid).orElseThrow(FalhaLoginException::new);
        var u = usuarios.porIdComLock(uid).orElseThrow(FalhaLoginException::new);
        var s = sessoes.comLock(sid).orElseThrow(FalhaLoginException::new);
        var r = renovacoes.findById(hash).orElseThrow(FalhaLoginException::new);
        if (r.isUsado()) {
            s.revogar();
            auditar(uid, uid, "RENOVACAO_REUTILIZADA");
            throw new FalhaLoginException();
        }
        if (!valida(u, s) || !r.getExpira().isAfter(Instant.now())) throw new FalhaLoginException();
        r.consumir();
        return emitir(u, s);
    }

    @Transactional
    public void sair(String token) {
        if (token == null || !token.matches("[A-Za-z0-9_-]{43}")) return;
        renovacoes
                .sessaoDoToken(hashToken(token))
                .flatMap(sessoes::comLock)
                .ifPresent(
                        s -> {
                            s.revogar();
                            auditar(s.getUsuarioId(), s.getUsuarioId(), "LOGOUT");
                        });
    }

    @Transactional(noRollbackFor = FalhaLoginException.class)
    public void trocarSenha(@Valid @NotNull AcessoDtos.TrocaSenha pedido) {
        var u = atual(true, true);
        if (u.getBloqueadoAte() != null && u.getBloqueadoAte().isAfter(Instant.now()))
            throw new FalhaLoginException();
        if (!senhas.matches(pedido.senhaAtual(), u.getSenhaHash())) {
            u.falhou(Instant.now());
            auditar(u.getId(), u.getId(), "TROCA_SENHA_RECUSADA");
            throw new FalhaLoginException();
        }
        if (senhas.matches(pedido.novaSenha(), u.getSenhaHash())) {
            throw new RegraNegocioException(
                    HttpStatus.BAD_REQUEST,
                    "SENHA_REPETIDA",
                    "Escolha uma senha diferente da atual.");
        }
        u.senha(senhas.encode(pedido.novaSenha()), false);
        auditar(u.getId(), u.getId(), "SENHA_ALTERADA");
    }

    @Transactional(readOnly = true)
    public AcessoDtos.Usuario eu() {
        return resposta(atual(false, true));
    }

    @Transactional(readOnly = true)
    public AcessoDtos.PaginaUsuarios listar(int pagina) {
        exigirAdministrador(false);
        if (pagina < 0 || pagina > 100000) throw new IllegalArgumentException("PAGINA_INVALIDA");
        var p = usuarios.findAll(PageRequest.of(pagina, 20, Sort.by("nome", "id")));
        return new AcessoDtos.PaginaUsuarios(
                p.getContent().stream().map(this::resposta).toList(),
                p.getNumber(),
                p.getTotalPages(),
                p.getTotalElements());
    }

    @Transactional
    public AcessoDtos.Usuario criar(@Valid @NotNull AcessoDtos.CriarUsuario pedido) {
        var ator = exigirAdministrador(true);
        validarEscopos(pedido.clientes(), pedido.armazens());
        String email = normalizarEmail(pedido.email());
        if (usuarios.findByEmail(email).isPresent())
            throw RegraNegocioException.conflito("EMAIL_CADASTRADO", "E-mail já cadastrado.");
        var u =
                usuarios.saveAndFlush(
                        new UsuarioAcesso(
                                UUID.randomUUID().toString(),
                                pedido.nome().strip(),
                                email,
                                senhas.encode(pedido.senhaTemporaria()),
                                pedido.perfil(),
                                pedido.administrador(),
                                false,
                                serializar(pedido.clientes()),
                                serializar(pedido.armazens())));
        auditar(ator.getId(), u.getId(), "USUARIO_CRIADO");
        return resposta(u);
    }

    @Transactional
    public AcessoDtos.Usuario editar(String id, @Valid @NotNull AcessoDtos.EditarUsuario pedido) {
        var ator = exigirAdministrador(true);
        var u = usuarios.porIdComLock(id).orElseThrow(RegraNegocioException::naoEncontrado);
        protegerPrincipal(u);
        conferirVersao(u, pedido.versao());
        if (ator.getId().equals(id) && (!pedido.ativo() || !pedido.administrador())) {
            throw RegraNegocioException.conflito(
                    "PROPRIO_ACESSO",
                    "Outro administrador deve alterar seu próprio acesso administrativo.");
        }
        validarEscopos(pedido.clientes(), pedido.armazens());
        u.atualizar(
                pedido.nome().strip(),
                pedido.perfil(),
                pedido.administrador(),
                pedido.ativo(),
                serializar(pedido.clientes()),
                serializar(pedido.armazens()));
        usuarios.flush();
        auditar(ator.getId(), id, "USUARIO_ALTERADO");
        return resposta(u);
    }

    @Transactional
    public void redefinirSenha(String id, @Valid @NotNull AcessoDtos.RedefinirSenha pedido) {
        var ator = exigirAdministrador(true);
        var u = usuarios.porIdComLock(id).orElseThrow(RegraNegocioException::naoEncontrado);
        protegerPrincipal(u);
        conferirVersao(u, pedido.versao());
        if (senhas.matches(pedido.senhaTemporaria(), u.getSenhaHash())) {
            throw new RegraNegocioException(
                    HttpStatus.BAD_REQUEST,
                    "SENHA_REPETIDA",
                    "Escolha uma senha temporária diferente da atual.");
        }
        u.senha(senhas.encode(pedido.senhaTemporaria()), true);
        auditar(ator.getId(), id, "SENHA_REDEFINIDA");
    }

    @Transactional(readOnly = true)
    public boolean jwtAtivo(Jwt jwt) {
        var u = usuarios.findById(jwt.getSubject()).orElse(null);
        String sid = jwt.getClaimAsString("sid");
        var s = sid == null ? null : sessoes.findById(sid).orElse(null);
        return valida(u, s)
                && Objects.equals(jwt.getClaimAsString("ver"), Long.toString(u.getVersaoTokens()))
                && Objects.equals(jwt.getClaim("trocar_senha"), u.isTrocarSenha())
                && Objects.equals(jwt.getClaimAsString("wms_perfil"), u.getPerfil());
    }

    private UsuarioAcesso atual(boolean lock, boolean permitirTroca) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null
                || !auth.isAuthenticated()
                || !(auth.getPrincipal() instanceof Jwt jwt)
                || jwt.getExpiresAt() == null
                || !jwt.getExpiresAt().isAfter(Instant.now()))
            throw new AccessDeniedException("Acesso não autorizado.");
        var u =
                (lock
                                ? usuarios.porIdComLock(jwt.getSubject())
                                : usuarios.findById(jwt.getSubject()))
                        .orElseThrow(FalhaLoginException::new);
        if (!jwtAtivo(jwt)) throw new FalhaLoginException();
        if (!permitirTroca && u.isTrocarSenha())
            throw new AccessDeniedException("Troque sua senha.");
        return u;
    }

    private UsuarioAcesso exigirAdministrador(boolean lock) {
        var u = atual(lock, false);
        if (!u.isAdministrador()) throw new AccessDeniedException("Acesso não autorizado.");
        return u;
    }

    private boolean valida(UsuarioAcesso u, SessaoAcesso s) {
        return u != null
                && s != null
                && u.isAtivo()
                && !s.isRevogada()
                && s.getUsuarioId().equals(u.getId())
                && s.getVersaoTokens() == u.getVersaoTokens()
                && s.getExpira().isAfter(Instant.now());
    }

    private Entrada emitir(UsuarioAcesso u, SessaoAcesso s) {
        Instant agora = Instant.now();
        var claims =
                JwtClaimsSet.builder()
                        .issuer(identidade.issuer())
                        .subject(u.getId())
                        .audience(List.of(identidade.audience()))
                        .issuedAt(agora)
                        .expiresAt(agora.plusSeconds(300))
                        .id(UUID.randomUUID().toString())
                        .claim("sid", s.getId())
                        .claim("ver", Long.toString(u.getVersaoTokens()))
                        .claim("trocar_senha", u.isTrocarSenha())
                        .claim("wms_perfil", u.getPerfil())
                        .claim("wms_clientes", ids(u.getClientes()))
                        .claim("wms_armazens", ids(u.getArmazens()))
                        .build();
        String access =
                encoder.encode(
                                JwtEncoderParameters.from(
                                        JwsHeader.with(SignatureAlgorithm.RS256).build(), claims))
                        .getTokenValue();
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String refresh = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        renovacoes.save(
                new RenovacaoAcesso(
                        hashToken(refresh),
                        s.getId(),
                        s.getExpira().isBefore(agora.plusSeconds(1800))
                                ? s.getExpira()
                                : agora.plusSeconds(1800)));
        return new Entrada(new AcessoDtos.Tokens(access, 300, resposta(u)), refresh);
    }

    private String hashToken(String token) {
        if (token == null || !token.matches("[A-Za-z0-9_-]{43}")) throw new FalhaLoginException();
        try {
            return HexFormat.of()
                    .formatHex(
                            MessageDigest.getInstance("SHA-256")
                                    .digest(token.getBytes(StandardCharsets.US_ASCII)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA256_INDISPONIVEL");
        }
    }

    private void validarEscopos(List<Long> cs, List<Long> ars) {
        if (clientes.findAllById(cs.stream().distinct().toList()).size()
                        != cs.stream().distinct().count()
                || armazens.findAllById(ars.stream().distinct().toList()).size()
                        != ars.stream().distinct().count()) {
            throw new RegraNegocioException(
                    HttpStatus.BAD_REQUEST,
                    "ESCOPO_INVALIDO",
                    "Selecione clientes e armazéns cadastrados.");
        }
    }

    private String normalizarEmail(String email) {
        return email.strip().toLowerCase(Locale.ROOT);
    }

    private String serializar(List<Long> ids) {
        return ids.stream()
                .distinct()
                .sorted()
                .map(String::valueOf)
                .collect(Collectors.joining(","));
    }

    private List<String> ids(String csv) {
        return csv.isBlank() ? List.of() : Arrays.asList(csv.split(","));
    }

    private void protegerPrincipal(UsuarioAcesso u) {
        if (u.isPrincipal())
            throw new AccessDeniedException(
                    "A conta principal é protegida. A senha só pode ser alterada pelo próprio titular.");
    }

    private void conferirVersao(UsuarioAcesso u, long versao) {
        if (u.getVersao() != versao)
            throw RegraNegocioException.conflito(
                    "VERSAO_DIVERGENTE", "O cadastro mudou. Atualize a lista antes de confirmar.");
    }

    private AcessoDtos.Usuario resposta(UsuarioAcesso u) {
        return new AcessoDtos.Usuario(
                u.getId(),
                u.getNome(),
                u.getEmail(),
                u.getPerfil(),
                u.isAdministrador(),
                u.isPrincipal(),
                u.isAtivo(),
                u.isTrocarSenha(),
                ids(u.getClientes()),
                ids(u.getArmazens()),
                u.getVersao());
    }

    private void auditar(String ator, String alvo, String acao) {
        eventos.save(
                new EventoAcesso(UUID.randomUUID().toString(), ator, alvo, acao, Instant.now()));
    }
}
