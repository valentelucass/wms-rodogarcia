package br.com.rodogarcia.wms.controllers;

import br.com.rodogarcia.wms.dto.AcessoDtos;
import br.com.rodogarcia.wms.services.LoginService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.time.Duration;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@ConditionalOnProperty(name = "wms.auth.enabled", havingValue = "true")
public class LoginController {
    private final LoginService login;
    private final boolean secure;

    public LoginController(
            LoginService login, @Value("${wms.auth.secure-cookie:true}") boolean secure) {
        this.login = login;
        this.secure = secure;
    }

    @GetMapping("/csrf")
    public Map<String, String> csrf(CsrfToken token) {
        return Map.of("token", token.getToken(), "header", token.getHeaderName());
    }

    @GetMapping("/jwks")
    public Map<String, Object> jwks() {
        return login.chavesPublicas();
    }

    @PostMapping("/entrar")
    public AcessoDtos.Tokens entrar(
            @Valid @RequestBody AcessoDtos.Login pedido,
            @CookieValue(name = "WMS_REFRESH", required = false) String anterior,
            HttpServletResponse response) {
        var entrada = login.entrar(pedido, anterior);
        cookie(response, entrada.renovacao(), 1800);
        return entrada.resposta();
    }

    @PostMapping("/renovar")
    public AcessoDtos.Tokens renovar(
            @CookieValue(name = "WMS_REFRESH", required = false) String refresh,
            HttpServletResponse response) {
        var entrada = login.renovar(refresh);
        cookie(response, entrada.renovacao(), 1800);
        return entrada.resposta();
    }

    @PostMapping("/sair")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void sair(
            @CookieValue(name = "WMS_REFRESH", required = false) String refresh,
            HttpServletResponse response) {
        login.sair(refresh);
        cookie(response, "", 0);
    }

    @PostMapping("/senha")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void senha(
            @Valid @RequestBody AcessoDtos.TrocaSenha pedido, HttpServletResponse response) {
        login.trocarSenha(pedido);
        cookie(response, "", 0);
    }

    @GetMapping("/eu")
    public AcessoDtos.Usuario eu() {
        return login.eu();
    }

    @GetMapping("/usuarios")
    public AcessoDtos.PaginaUsuarios usuarios(@RequestParam(defaultValue = "0") int pagina) {
        return login.listar(pagina);
    }

    @PostMapping("/usuarios")
    @ResponseStatus(HttpStatus.CREATED)
    public AcessoDtos.Usuario criar(@Valid @RequestBody AcessoDtos.CriarUsuario pedido) {
        return login.criar(pedido);
    }

    @PutMapping("/usuarios/{id}")
    public AcessoDtos.Usuario editar(
            @PathVariable String id, @Valid @RequestBody AcessoDtos.EditarUsuario pedido) {
        return login.editar(id, pedido);
    }

    @PostMapping("/usuarios/{id}/senha")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void redefinir(
            @PathVariable String id, @Valid @RequestBody AcessoDtos.RedefinirSenha pedido) {
        login.redefinirSenha(id, pedido);
    }

    private void cookie(HttpServletResponse response, String value, long age) {
        response.addHeader(
                HttpHeaders.SET_COOKIE,
                ResponseCookie.from("WMS_REFRESH", value)
                        .httpOnly(true)
                        .secure(secure)
                        .sameSite("Strict")
                        .path("/api/auth")
                        .maxAge(Duration.ofSeconds(age))
                        .build()
                        .toString());
    }
}
