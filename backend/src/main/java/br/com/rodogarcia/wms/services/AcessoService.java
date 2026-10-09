package br.com.rodogarcia.wms.services;

import br.com.rodogarcia.wms.config.JwtWmsValidator;
import java.util.List;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "wms.cadastros.enabled", havingValue = "true")
public class AcessoService {
    private final ObjectProvider<LoginService> login;

    public AcessoService(ObjectProvider<LoginService> login) {
        this.login = login;
    }

    private Jwt identidade() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null
                || !auth.isAuthenticated()
                || !(auth.getPrincipal() instanceof Jwt jwt)
                || Boolean.TRUE.equals(jwt.getClaim("trocar_senha"))
                || jwt.getClaimAsString("wms_perfil") == null
                || !JwtWmsValidator.PERFIS.contains(jwt.getClaimAsString("wms_perfil"))) {
            throw new AccessDeniedException("Acesso não autorizado.");
        }
        var autenticacaoLocal = login.getIfAvailable();
        if (autenticacaoLocal != null && !autenticacaoLocal.jwtAtivo(jwt)) {
            throw new AccessDeniedException("Acesso não autorizado.");
        }
        return jwt;
    }

    public String usuario() {
        return identidade().getSubject();
    }

    public boolean gestor() {
        return "GESTOR".equals(identidade().getClaimAsString("wms_perfil"));
    }

    public void exigirGestor() {
        if (!gestor()) {
            throw new AccessDeniedException("Acesso não autorizado.");
        }
    }

    public void exigirSupervisor() {
        if (!supervisor()) {
            throw new AccessDeniedException("Acesso não autorizado.");
        }
    }

    public boolean supervisor() {
        String perfil = identidade().getClaimAsString("wms_perfil");
        return "GESTOR".equals(perfil) || "SUPERVISOR".equals(perfil);
    }

    public List<Long> clientes() {
        return ids("wms_clientes");
    }

    public List<Long> armazens() {
        return ids("wms_armazens");
    }

    public void cliente(Long id) {
        if (!gestor() && !clientes().contains(id)) {
            throw new AccessDeniedException("Acesso não autorizado.");
        }
    }

    public void armazem(Long id) {
        if (!gestor() && !armazens().contains(id)) {
            throw new AccessDeniedException("Acesso não autorizado.");
        }
    }

    private List<Long> ids(String campo) {
        List<String> ids = identidade().getClaimAsStringList(campo);
        return ids == null ? List.of() : ids.stream().map(Long::valueOf).distinct().toList();
    }
}
