package br.com.rodogarcia.wms;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.rodogarcia.wms.config.IdentidadeTesteConfig;
import br.com.rodogarcia.wms.models.Armazem;
import br.com.rodogarcia.wms.models.Cliente;
import br.com.rodogarcia.wms.models.Endereco;
import br.com.rodogarcia.wms.models.OcupacaoEndereco;
import br.com.rodogarcia.wms.models.TipoEndereco;
import br.com.rodogarcia.wms.services.VisaoOperacaoService;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(
        locations = "classpath:cadastros-test.properties",
        properties =
                "spring.datasource.url=jdbc:h2:mem:visao-operacao;DB_CLOSE_DELAY=-1;INIT=CREATE SCHEMA IF NOT EXISTS wms")
@Import(IdentidadeTesteConfig.class)
@Transactional
class VisaoOperacaoIntegrationTest {
    @Autowired EntityManager em;
    @Autowired VisaoOperacaoService service;
    private final Instant now = Instant.parse("2026-10-09T12:00:00Z");

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    private void login(String perfil, List<Long> clients, List<Long> warehouses) {
        var jwt =
                Jwt.withTokenValue("ficticio-isolado")
                        .header("alg", "RS256")
                        .subject("teste-visao")
                        .claim("wms_perfil", perfil)
                        .claim("wms_clientes", clients.stream().map(String::valueOf).toList())
                        .claim("wms_armazens", warehouses.stream().map(String::valueOf).toList())
                        .build();
        SecurityContextHolder.getContext()
                .setAuthentication(new JwtAuthenticationToken(jwt, List.of()));
    }

    private Cliente client() {
        var c = new Cliente("DEMO", "Cliente fictício", "00000000000000", now);
        em.persist(c);
        return c;
    }

    private Armazem warehouse(String code) {
        var a =
                new Armazem(
                        code, code + " fictício", "00000000000000", "Cidade fictícia", "SP", now);
        em.persist(a);
        return a;
    }

    private Endereco address(Armazem a, String code, int level, TipoEndereco tipo) {
        var e =
                new Endereco(
                        a,
                        code,
                        "A",
                        level,
                        code,
                        "Fictício",
                        tipo,
                        new BigDecimal("1000"),
                        null,
                        null,
                        null,
                        null,
                        level,
                        now);
        em.persist(e);
        em.persist(new OcupacaoEndereco(e));
        return e;
    }

    @Test
    void mapaAgregaArmazensReaisOrdenaNiveisEFiltraSemAlterarIndicadores() {
        var c = client();
        var a = warehouse("DEMO1");
        var b = warehouse("DEMO2");
        var low = address(a, "A01", 1, TipoEndereco.ARMAZENAGEM);
        var high = address(a, "A03", 3, TipoEndereco.ARMAZENAGEM);
        address(a, "Q01", 2, TipoEndereco.QUARENTENA);
        address(b, "B01", 1, TipoEndereco.ARMAZENAGEM);
        em.flush();
        login("GESTOR", List.of(), List.of());
        var all = service.consultar(null, null, "America/Sao_Paulo", "", "TODAS", 0, 100);
        assertThat(all.capacidade()).isEqualTo(3);
        assertThat(all.posicoesLivres()).isEqualTo(3);
        assertThat(all.ocupacao()).isEqualByComparingTo("0");
        assertThat(all.mapa().totalItens()).isEqualTo(4);
        assertThat(all.mapa().itens().get(0).id()).isEqualTo(high.getId());
        assertThat(
                        all.mapa().itens().stream()
                                .filter(p -> p.tipo() == TipoEndereco.QUARENTENA)
                                .findFirst()
                                .orElseThrow()
                                .disponivel())
                .isFalse();
        var filtered =
                service.consultar(
                        c.getId(), a.getId(), "America/Sao_Paulo", "A01", "DISPONIVEL", 0, 1);
        assertThat(filtered.capacidade()).isEqualTo(2);
        assertThat(filtered.posicoesLivres()).isEqualTo(2);
        assertThat(filtered.mapa().totalItens()).isEqualTo(1);
        assertThat(filtered.mapa().itens().get(0).id()).isEqualTo(low.getId());
        assertThat(service.detalhe(low.getId()).unidades()).isEmpty();
        assertThat(filtered.valorArmazenado()).isEqualByComparingTo("0");
        assertThat(filtered.faturamentoMes()).isEqualByComparingTo("0");
    }

    @Test
    void operacaoRecebeSomenteArmazensPermitidosEValoresNaoSaoZero() {
        var c = client();
        var a = warehouse("DEMO1");
        var b = warehouse("DEMO2");
        address(a, "A01", 1, TipoEndereco.ARMAZENAGEM);
        var outside = address(b, "B01", 1, TipoEndereco.ARMAZENAGEM);
        em.flush();
        login("OPERACAO", List.of(c.getId()), List.of(a.getId()));
        var result = service.consultar(null, null, "America/Sao_Paulo", "", "TODAS", 0, 100);
        assertThat(result.capacidade()).isEqualTo(1);
        assertThat(result.financeiroPermitido()).isFalse();
        assertThat(result.valorArmazenado()).isNull();
        assertThat(result.faturamentoMes()).isNull();
        assertThatThrownBy(
                        () ->
                                service.consultar(
                                        null, b.getId(), "America/Sao_Paulo", "", "TODAS", 0, 100))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> service.detalhe(outside.getId()))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void ausenciaDeCapacidadeNaoDividePorZeroENaoOcultaAreaEspecial() {
        client();
        var a = warehouse("DEMO");
        address(a, "Q01", 1, TipoEndereco.QUARENTENA);
        em.flush();
        login("GESTOR", List.of(), List.of());
        var result = service.consultar(null, a.getId(), "UTC", "", "TODAS", 0, 100);
        assertThat(result.capacidade()).isZero();
        assertThat(result.ocupacao()).isEqualByComparingTo("0");
        assertThat(result.mapa().totalItens()).isEqualTo(1);
    }
}
