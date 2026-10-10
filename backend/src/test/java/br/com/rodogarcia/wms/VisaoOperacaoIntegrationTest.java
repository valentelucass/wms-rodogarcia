package br.com.rodogarcia.wms;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.rodogarcia.wms.config.IdentidadeTesteConfig;
import br.com.rodogarcia.wms.config.Perf01StatementInspector;
import br.com.rodogarcia.wms.models.Armazem;
import br.com.rodogarcia.wms.models.AvariaEstoque;
import br.com.rodogarcia.wms.models.BaixaSaida;
import br.com.rodogarcia.wms.models.ChegadaRecebimento;
import br.com.rodogarcia.wms.models.Cliente;
import br.com.rodogarcia.wms.models.CondicaoMercadoria;
import br.com.rodogarcia.wms.models.ContagemEstoque;
import br.com.rodogarcia.wms.models.ConteudoUnidade;
import br.com.rodogarcia.wms.models.Embalagem;
import br.com.rodogarcia.wms.models.Endereco;
import br.com.rodogarcia.wms.models.EntradaConferida;
import br.com.rodogarcia.wms.models.FatoPermanencia;
import br.com.rodogarcia.wms.models.ItemChegada;
import br.com.rodogarcia.wms.models.ItemNotaEntrada;
import br.com.rodogarcia.wms.models.ItemPedidoSaida;
import br.com.rodogarcia.wms.models.MarcoFinanceiroAvaria;
import br.com.rodogarcia.wms.models.NotaEntrada;
import br.com.rodogarcia.wms.models.OcupacaoEndereco;
import br.com.rodogarcia.wms.models.PedidoEntrada;
import br.com.rodogarcia.wms.models.PedidoSaida;
import br.com.rodogarcia.wms.models.Produto;
import br.com.rodogarcia.wms.models.ReservaSaida;
import br.com.rodogarcia.wms.models.RetiradaSaida;
import br.com.rodogarcia.wms.models.RevisaoContagem;
import br.com.rodogarcia.wms.models.TipoEndereco;
import br.com.rodogarcia.wms.models.TipoQuantidade;
import br.com.rodogarcia.wms.models.TipoUnidadeLogistica;
import br.com.rodogarcia.wms.models.UnidadeLogistica;
import br.com.rodogarcia.wms.services.DashboardService;
import br.com.rodogarcia.wms.services.VisaoOperacaoService;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(
        locations = "classpath:cadastros-test.properties",
        properties = {
            "spring.datasource.url=jdbc:h2:mem:visao-operacao;DB_CLOSE_DELAY=-1;INIT=CREATE SCHEMA IF NOT EXISTS wms",
            "spring.jpa.properties.hibernate.session_factory.statement_inspector=br.com.rodogarcia.wms.config.Perf01StatementInspector"
        })
@Import(IdentidadeTesteConfig.class)
@Transactional
class VisaoOperacaoIntegrationTest {
    @Autowired EntityManager em;
    @Autowired VisaoOperacaoService service;
    @Autowired DashboardService dashboard;
    @Autowired br.com.rodogarcia.wms.services.CalculoCobrancaService calculos;
    @Autowired br.com.rodogarcia.wms.repositories.FatoPermanenciaRepository fatos;
    @Autowired br.com.rodogarcia.wms.repositories.AvariaEstoqueRepository avarias;
    @Autowired br.com.rodogarcia.wms.repositories.MovimentoEstoqueRepository movimentos;
    private final Instant now = Instant.parse("2026-10-09T12:00:00Z");

    @Test
    void perf01DashboardPaginaMantemSkuClienteUnidadeERecusaAntesDeConsulta() throws Exception {
        var c = client();
        var d = new Cliente("PERFC2", "Fictício", "11111111111111", now);
        var outside = new Cliente("PERFC3", "Fictício", "22222222222222", now);
        em.persist(d);
        em.persist(outside);
        var a = warehouse("PERFDASH");
        em.persist(
                new Produto(
                        c,
                        "A",
                        "Fictício",
                        "UN",
                        TipoQuantidade.CONTAGEM,
                        0,
                        false,
                        false,
                        null,
                        now));
        em.persist(
                new Produto(
                        d,
                        "A",
                        "Fictício",
                        "KG",
                        TipoQuantidade.MEDIDA,
                        6,
                        false,
                        false,
                        null,
                        now));
        em.persist(
                new Produto(
                        outside,
                        "A",
                        "Segregado",
                        "UN",
                        TipoQuantidade.CONTAGEM,
                        0,
                        false,
                        false,
                        null,
                        now));
        for (int i = 0; i < 100; i++)
            em.persist(
                    new Produto(
                            c,
                            String.format("B%03d", i),
                            "Fictício",
                            "UN",
                            TipoQuantidade.CONTAGEM,
                            0,
                            false,
                            false,
                            null,
                            now));
        em.flush();
        login("OPERACAO", List.of(c.getId(), d.getId()), List.of(a.getId()));
        var stats = em.getEntityManagerFactory().unwrap(SessionFactory.class).getStatistics();
        boolean previous = stats.isStatisticsEnabled();
        stats.setStatisticsEnabled(true);
        var measurements = new ArrayList<Map<String, Object>>();
        try {
            for (int size : List.of(1, 12)) {
                em.clear();
                stats.clear();
                Perf01StatementInspector.iniciar();
                var result = dashboard.consultar(null, null, "UTC", 0, size);
                var sql = Perf01StatementInspector.terminar();
                assertThat(result.produtos().totalItens()).isEqualTo(102);
                assertThat(result.produtos().itens()).hasSize(size);
                assertThat(result.areas())
                        .allSatisfy(
                                area -> {
                                    assertThat(area.capacidadeAtiva()).isNull();
                                    assertThat(area.livresArmazem()).isNull();
                                });
                assertThat(result.produtos().itens())
                        .allSatisfy(
                                p -> {
                                    assertThat(p.clienteId()).isIn(c.getId(), d.getId());
                                    assertThat(p.fisicoTotal()).isEqualByComparingTo("0");
                                });
                if (size == 12) {
                    assertThat(result.produtos().itens().get(0).sku()).isEqualTo("A");
                    assertThat(result.produtos().itens().get(1).sku()).isEqualTo("A");
                    assertThat(result.produtos().itens().subList(0, 2))
                            .extracting(p -> p.unidadeMedida())
                            .containsExactly("UN", "KG");
                    assertThat(result.produtos().itens().subList(0, 2))
                            .extracting(p -> p.clienteId())
                            .containsExactly(c.getId(), d.getId());
                }
                measurements.add(
                        Map.of(
                                "pageSize",
                                size,
                                "totalProductsAuthorized",
                                102,
                                "statements",
                                sql.size(),
                                "entitiesLoaded",
                                stats.getEntityLoadCount(),
                                "parameterPlaceholders",
                                sql.stream()
                                        .mapToLong(q -> q.chars().filter(ch -> ch == '?').count())
                                        .sum(),
                                "queries",
                                sql));
            }
            Perf01StatementInspector.iniciar();
            assertThatThrownBy(() -> dashboard.consultar(outside.getId(), a.getId(), "UTC", 0, 12))
                    .isInstanceOf(AccessDeniedException.class);
            assertThat(Perf01StatementInspector.terminar()).isEmpty();
            assertThatThrownBy(() -> dashboard.consultar(null, null, "UTC", 0, 13))
                    .isInstanceOf(br.com.rodogarcia.wms.exceptions.RegraNegocioException.class);
            Files.writeString(
                    Path.of(System.getProperty("wms.test.evidencias.dir"), "perf01-dashboard.json"),
                    JsonMapper.builder()
                            .build()
                            .writeValueAsString(
                                    Map.of(
                                            "context",
                                            "H2 visao-operacao",
                                            "measurements",
                                            measurements,
                                            "deniedScopeStatements",
                                            0)),
                    StandardOpenOption.CREATE_NEW);
            assertThat(((Number) measurements.getLast().get("statements")).longValue())
                    .isLessThanOrEqualTo(
                            ((Number) measurements.getFirst().get("statements")).longValue() + 1);
        } finally {
            stats.setStatisticsEnabled(previous);
        }
    }

    @Test
    void perf01OcupacaoFisicaIncluiOutroClienteSemExporUnidadeReservaOuBloqueio() {
        var c = client();
        var d = new Cliente("PERFORAFORA", "Fictício", "11111111111111", now);
        em.persist(d);
        var a = warehouse("PERFMASK");
        var b = warehouse("PERMFORA");
        var e = address(a, "M01", 1, TipoEndereco.ARMAZENAGEM);
        address(a, "M02", 1, TipoEndereco.ARMAZENAGEM);
        var outside = address(b, "M03", 1, TipoEndereco.ARMAZENAGEM);
        var entrada =
                perf01Entrada(d, a, "O4", new BigDecimal("10.00"), BigDecimal.ONE, BigDecimal.ZERO);
        entrada.marcarUnitizada(now);
        var p = entrada.getItemChegada().getItemNota().getProduto();
        var embalagem = new Embalagem(p, "FORA", "Fictícia", BigDecimal.ONE, now);
        em.persist(embalagem);
        var u =
                new UnidadeLogistica(
                        entrada,
                        embalagem,
                        TipoUnidadeLogistica.PALLET,
                        CondicaoMercadoria.BOA,
                        BigDecimal.ONE,
                        now);
        em.persist(u);
        u.registrarAvaria(now);
        em.createQuery(
                        "select o from OcupacaoEndereco o where o.endereco=:e",
                        OcupacaoEndereco.class)
                .setParameter("e", e)
                .getSingleResult()
                .atribuir(u);
        var saida = new PedidoSaida(d, a, "MASK", now);
        em.persist(saida);
        var item = new ItemPedidoSaida(saida, p, BigDecimal.ONE);
        em.persist(item);
        em.persist(new ReservaSaida(item, u, "mask", BigDecimal.ONE, now));
        em.flush();
        em.clear();
        login("SUPERVISOR", List.of(c.getId()), List.of(a.getId()));
        var result = service.consultar(c.getId(), a.getId(), "UTC", "M01", "OCUPADO", 0, 1);
        assertThat(result.capacidade()).isEqualTo(2);
        assertThat(result.posicoesOcupadas()).isEqualTo(1);
        assertThat(result.posicoesLivres()).isEqualTo(1);
        assertThat(result.unidadesArmazenadas()).isZero();
        assertThat(result.reservasAtivas()).isZero();
        assertThat(result.valorArmazenado()).isEqualByComparingTo("0");
        assertThat(result.mapa().totalItens()).isEqualTo(1);
        assertThat(result.mapa().itens().getFirst().ocupada()).isTrue();
        assertThat(result.mapa().itens().getFirst().bloqueada()).isFalse();
        assertThat(result.mapa().itens().getFirst().reservada()).isFalse();
        var detail = service.detalhe(e.getId());
        assertThat(detail.conteudoRestrito()).isTrue();
        assertThat(detail.unidades()).isEmpty();
        assertThat(detail.unidadesVisiveis()).isZero();
        assertThat(detail.endereco().bloqueada()).isFalse();
        assertThat(detail.endereco().reservada()).isFalse();
        assertThatThrownBy(() -> service.consultar(d.getId(), a.getId(), "UTC", "", "TODAS", 0, 1))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> service.detalhe(outside.getId()))
                .isInstanceOf(AccessDeniedException.class);
    }

    private EntradaConferida perf01Entrada(
            Cliente c,
            Armazem a,
            String sku,
            BigDecimal price,
            BigDecimal triagem,
            BigDecimal quarentena) {
        return perf01Entrada(c, a, sku, price, triagem, quarentena, new BigDecimal("10"));
    }

    private EntradaConferida perf01Entrada(
            Cliente c,
            Armazem a,
            String sku,
            BigDecimal price,
            BigDecimal triagem,
            BigDecimal quarentena,
            BigDecimal prevista) {
        var p =
                new Produto(
                        c,
                        sku,
                        "Fictício",
                        "KG",
                        TipoQuantidade.MEDIDA,
                        6,
                        false,
                        false,
                        null,
                        now);
        em.persist(p);
        var pedido = new PedidoEntrada(c, a, sku, now);
        em.persist(pedido);
        var nota =
                new NotaEntrada(
                        pedido,
                        c.getDocumentoFiscal(),
                        1,
                        Long.parseLong(sku.substring(1)),
                        LocalDate.of(2026, 10, 9),
                        null);
        em.persist(nota);
        var i = new ItemNotaEntrada(nota, 1, p, prevista, price);
        em.persist(i);
        var chegada =
                new ChegadaRecebimento(
                        pedido, sku, "ficticio", now, now, "teste", "Chegada fictícia");
        em.persist(chegada);
        var item = new ItemChegada(chegada, 1, i, null, null, triagem, quarentena);
        em.persist(item);
        var entrada = new EntradaConferida(item, now, now, triagem, quarentena);
        em.persist(entrada);
        return entrada;
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 8, 40})
    void perf01ContaValoracaoPositivaPorUnidadeSemDuplicarPosicoes(int cardinality)
            throws Exception {
        var c = client();
        var a = warehouse("PERFUNITS");
        var quantity = BigDecimal.valueOf(cardinality);
        var expected = quantity.multiply(new BigDecimal("2.50"));
        var entrada = perf01Entrada(c, a, "O5", expected, quantity, BigDecimal.ZERO, quantity);
        entrada.marcarUnitizada(now);
        var p = entrada.getItemChegada().getItemNota().getProduto();
        var embalagem = new Embalagem(p, "UNITS", "Fictícia", BigDecimal.ONE, now);
        em.persist(embalagem);
        for (int i = 0; i < cardinality; i++) {
            var u =
                    new UnidadeLogistica(
                            entrada,
                            embalagem,
                            TipoUnidadeLogistica.PALLET,
                            CondicaoMercadoria.BOA,
                            BigDecimal.ONE,
                            now);
            em.persist(u);
            em.persist(new ConteudoUnidade(u, entrada, BigDecimal.ONE));
            if (i == 0)
                for (String code : List.of("U1", "U2")) {
                    var e = address(a, code, 1, TipoEndereco.ARMAZENAGEM);
                    em.createQuery(
                                    "select o from OcupacaoEndereco o where o.endereco=:e",
                                    OcupacaoEndereco.class)
                            .setParameter("e", e)
                            .getSingleResult()
                            .atribuir(u);
                }
        }
        em.flush();
        em.clear();
        login("SUPERVISOR", List.of(c.getId()), List.of(a.getId()));
        var stats = em.getEntityManagerFactory().unwrap(SessionFactory.class).getStatistics();
        boolean previous = stats.isStatisticsEnabled();
        stats.setStatisticsEnabled(true);
        stats.clear();
        Perf01StatementInspector.iniciar();
        try {
            var result = service.consultar(c.getId(), a.getId(), "UTC", "", "TODAS", 0, 1);
            var sql = Perf01StatementInspector.terminar();
            assertThat(result.unidadesArmazenadas()).isEqualTo(cardinality);
            assertThat(result.valorArmazenado()).isEqualByComparingTo(expected);
            assertThat(result.valorCompleto()).isTrue();
            assertThat(result.capacidade()).isEqualTo(2);
            assertThat(result.posicoesOcupadas()).isEqualTo(2);
            assertThat(result.mapa().totalItens()).isEqualTo(2);
            assertThat(result.mapa().itens()).hasSize(1);
            Files.writeString(
                    Path.of(
                            System.getProperty("wms.test.evidencias.dir"),
                            "perf01-unidades-" + cardinality + ".json"),
                    JsonMapper.builder()
                            .build()
                            .writeValueAsString(
                                    Map.of(
                                            "context",
                                            "H2 visao-operacao",
                                            "fixtureUnits",
                                            cardinality,
                                            "physicalPositions",
                                            2,
                                            "pageSize",
                                            1,
                                            "expectedValue",
                                            expected,
                                            "statements",
                                            sql.size(),
                                            "entitiesLoaded",
                                            stats.getEntityLoadCount(),
                                            "parameterPlaceholders",
                                            sql.stream()
                                                    .mapToLong(
                                                            q ->
                                                                    q.chars()
                                                                            .filter(ch -> ch == '?')
                                                                            .count())
                                                    .sum(),
                                            "queries",
                                            sql,
                                            "beforeComparisonAvailable",
                                            false)),
                    StandardOpenOption.CREATE_NEW);
            assertThat(sql.stream().filter(q -> q.contains(" from wms.conteudo_unidade ")).count())
                    .isEqualTo(1);
        } finally {
            stats.setStatisticsEnabled(previous);
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 8, 40, 200, 501})
    void carga01ValoracaoMassaControladaSemConsultaPorUnidade(int cardinality) throws Exception {
        var c = client();
        var a = warehouse("PERFUNITS");
        var quantity = BigDecimal.valueOf(cardinality);
        var expected = quantity.multiply(new BigDecimal("2.50"));
        var entrada = perf01Entrada(c, a, "O5", expected, quantity, BigDecimal.ZERO, quantity);
        entrada.marcarUnitizada(now);
        var p = entrada.getItemChegada().getItemNota().getProduto();
        var embalagem = new Embalagem(p, "UNITS", "Fictícia", BigDecimal.ONE, now);
        em.persist(embalagem);
        for (int i = 0; i < cardinality; i++) {
            var u =
                    new UnidadeLogistica(
                            entrada,
                            embalagem,
                            TipoUnidadeLogistica.PALLET,
                            CondicaoMercadoria.BOA,
                            BigDecimal.ONE,
                            now);
            em.persist(u);
            em.persist(new ConteudoUnidade(u, entrada, BigDecimal.ONE));
            if (i == 0)
                for (String code : List.of("U1", "U2")) {
                    var e = address(a, code, 1, TipoEndereco.ARMAZENAGEM);
                    em.createQuery(
                                    "select o from OcupacaoEndereco o where o.endereco=:e",
                                    OcupacaoEndereco.class)
                            .setParameter("e", e)
                            .getSingleResult()
                            .atribuir(u);
                }
        }
        em.flush();
        em.clear();
        login("SUPERVISOR", List.of(c.getId()), List.of(a.getId()));
        var stats = em.getEntityManagerFactory().unwrap(SessionFactory.class).getStatistics();
        boolean previous = stats.isStatisticsEnabled();
        stats.setStatisticsEnabled(true);
        stats.clear();
        Perf01StatementInspector.iniciar();
        try {
            long started = System.nanoTime();
            var result = service.consultar(c.getId(), a.getId(), "UTC", "", "TODAS", 0, 1);
            long elapsed = System.nanoTime() - started;
            var sql = Perf01StatementInspector.terminar();
            assertThat(result.unidadesArmazenadas()).isEqualTo(cardinality);
            assertThat(result.valorArmazenado()).isEqualByComparingTo(expected);
            assertThat(result.valorCompleto()).isTrue();
            assertThat(result.capacidade()).isEqualTo(2);
            assertThat(result.posicoesOcupadas()).isEqualTo(2);
            assertThat(result.mapa().totalItens()).isEqualTo(2);
            assertThat(result.mapa().itens()).hasSize(1);
            Files.writeString(
                    Path.of(
                            System.getProperty("wms.test.evidencias.dir"),
                            "carga01-unidades-" + cardinality + ".json"),
                    JsonMapper.builder()
                            .build()
                            .writeValueAsString(
                                    Map.of(
                                            "context",
                                            "H2 visao-operacao",
                                            "fixtureUnits",
                                            cardinality,
                                            "physicalPositions",
                                            2,
                                            "pageSize",
                                            1,
                                            "expectedValue",
                                            expected,
                                            "statements",
                                            sql.size(),
                                            "entitiesLoaded",
                                            stats.getEntityLoadCount(),
                                            "parameterPlaceholders",
                                            sql.stream()
                                                    .mapToLong(
                                                            q ->
                                                                    q.chars()
                                                                            .filter(ch -> ch == '?')
                                                                            .count())
                                                    .sum(),
                                            "queries",
                                            sql,
                                            "elapsedNanos",
                                            elapsed)),
                    StandardOpenOption.CREATE_NEW);
            var rounds = new ArrayList<Map<String, Object>>();
            rounds.add(carga01Medida(stats, sql, elapsed, result));
            for (int repetition = 1; repetition < 3; repetition++) {
                em.clear();
                stats.clear();
                Perf01StatementInspector.iniciar();
                long repeatedStart = System.nanoTime();
                var repeated = service.consultar(c.getId(), a.getId(), "UTC", "", "TODAS", 0, 1);
                long repeatedElapsed = System.nanoTime() - repeatedStart;
                var repeatedSql = Perf01StatementInspector.terminar();
                assertThat(repeated.valorArmazenado()).isEqualByComparingTo(expected);
                assertThat(repeated.valorCompleto()).isTrue();
                assertThat(repeated.unidadesArmazenadas()).isEqualTo(cardinality);
                assertThat(repeated.capacidade()).isEqualTo(2);
                assertThat(repeated.posicoesOcupadas()).isEqualTo(2);
                rounds.add(carga01Medida(stats, repeatedSql, repeatedElapsed, repeated));
            }
            Files.writeString(
                    Path.of(
                            System.getProperty("wms.test.evidencias.dir"),
                            "carga01-rounds-" + cardinality + ".json"),
                    JsonMapper.builder().build().writeValueAsString(rounds),
                    StandardOpenOption.CREATE_NEW);
            assertThat(
                            sql.stream()
                                    .mapToInt(q -> (int) q.chars().filter(ch -> ch == '?').count())
                                    .max()
                                    .orElse(0))
                    .isLessThan(2000);
            assertThat(sql.stream().filter(q -> q.contains(" from wms.fato_permanencia ")).count())
                    .isEqualTo((cardinality + 499) / 500);
            assertThat(sql.stream().filter(q -> q.contains(" from wms.conteudo_unidade ")).count())
                    .isEqualTo((cardinality + 499) / 500);
        } finally {
            stats.setStatisticsEnabled(previous);
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void perf01ValorPreservaOrigemAvariaTriagemQuarentenaENulo(boolean unknown) {
        var c = client();
        var a = warehouse("PERFORACULO");
        var entrada =
                perf01Entrada(
                        c,
                        a,
                        "O1",
                        new BigDecimal("123.45"),
                        new BigDecimal("2.5"),
                        BigDecimal.ZERO);
        entrada.marcarUnitizada(now);
        var p = entrada.getItemChegada().getItemNota().getProduto();
        var embalagem = new Embalagem(p, "PERFORACULO", "Fictícia", new BigDecimal("2.5"), now);
        em.persist(embalagem);
        var u =
                new UnidadeLogistica(
                        entrada,
                        embalagem,
                        TipoUnidadeLogistica.PALLET,
                        CondicaoMercadoria.BOA,
                        new BigDecimal("2.5"),
                        now);
        em.persist(u);
        em.persist(new ConteudoUnidade(u, entrada, new BigDecimal("2.5")));
        u.registrarAvaria(now);
        em.persist(
                new AvariaEstoque(
                        u,
                        new BigDecimal("0.5"),
                        new BigDecimal("2.5"),
                        BigDecimal.ONE,
                        "ficticio",
                        now,
                        now,
                        "{}",
                        "Fictícia"));
        perf01Entrada(
                c, a, "O2", new BigDecimal("123.45"), new BigDecimal("2.5"), new BigDecimal("1.5"));
        // Quarentena sem triagem não é estoque valorizado; origem desconhecida permanece nula.
        perf01Entrada(
                c,
                a,
                "O3",
                unknown ? null : new BigDecimal("123.45"),
                BigDecimal.ZERO,
                BigDecimal.ONE);
        em.flush();
        em.clear();
        login("SUPERVISOR", List.of(c.getId()), List.of(a.getId()));
        var result = service.consultar(c.getId(), a.getId(), "UTC", "", "TODAS", 0, 1);
        if (unknown) {
            assertThat(result.valorArmazenado()).isNull();
            assertThat(result.valorCompleto()).isFalse();
        } else {
            // 123.45/10*(2.5-0.5) + 123.45/10*2.5 = 55.5525, calculado fora dos serviços.
            assertThat(result.valorArmazenado()).isEqualByComparingTo("55.5525");
            assertThat(result.valorCompleto()).isTrue();
        }
        assertThat(result.financeiroPermitido()).isTrue();
        assertThat(result.unidadesArmazenadas()).isEqualTo(1);
        assertThat(result.faturamentoMes()).isEqualByComparingTo("0");
    }

    @Test
    void perf01FinanceiroVazioNaoRelêEstoquePorProdutoNemPorPar() throws Exception {
        var c = client();
        var a = warehouse("PERFVAL1");
        var b = warehouse("PERFVAL2");
        var statistics = em.getEntityManagerFactory().unwrap(SessionFactory.class).getStatistics();
        boolean previous = statistics.isStatisticsEnabled();
        statistics.setStatisticsEnabled(true);
        login("GESTOR", List.of(), List.of());
        var measures = new ArrayList<Map<String, Object>>();
        try {
            int prepared = 0;
            for (int products : List.of(1, 12, 101)) {
                while (prepared < products) {
                    em.persist(
                            new Produto(
                                    c,
                                    "V" + prepared++,
                                    "Fictício",
                                    "UN",
                                    TipoQuantidade.CONTAGEM,
                                    0,
                                    false,
                                    false,
                                    null,
                                    now));
                }
                em.flush();
                em.clear();
                statistics.clear();
                Perf01StatementInspector.iniciar();
                long start = System.nanoTime();
                var result = service.consultar(c.getId(), a.getId(), "UTC", "", "TODAS", 0, 1);
                long elapsed = System.nanoTime() - start;
                var sql = Perf01StatementInspector.terminar();
                assertThat(result.valorArmazenado()).isEqualByComparingTo("0");
                assertThat(result.valorCompleto()).isTrue();
                assertThat(result.unidadesArmazenadas()).isZero();
                assertThat(result.mapa().itens()).isEmpty();
                var m = new LinkedHashMap<String, Object>();
                m.put("catalogProducts", products);
                m.put("scopedUnits", 0);
                m.put("statements", sql.size());
                m.put(
                        "parameterPlaceholders",
                        sql.stream()
                                .mapToLong(q -> q.chars().filter(ch -> ch == '?').count())
                                .sum());
                m.put("entitiesLoaded", statistics.getEntityLoadCount());
                m.put("elapsedNanosDiagnosticOnly", elapsed);
                m.put("queries", sql);
                measures.add(m);
            }
            em.clear();
            statistics.clear();
            Perf01StatementInspector.iniciar();
            var all = service.consultar(null, null, "UTC", "", "TODAS", 0, 1);
            var allSql = Perf01StatementInspector.terminar();
            assertThat(all.valorArmazenado()).isEqualByComparingTo("0");
            assertThat(all.valorCompleto()).isTrue();
            Files.writeString(
                    Path.of(
                            System.getProperty("wms.test.evidencias.dir"),
                            "perf01-financeiro-vazio.json"),
                    JsonMapper.builder()
                            .build()
                            .writeValueAsString(
                                    Map.of(
                                            "context",
                                            "H2 visao-operacao",
                                            "clienteId",
                                            c.getId(),
                                            "armazens",
                                            List.of(a.getId(), b.getId()),
                                            "measurements",
                                            measures,
                                            "allStatements",
                                            allSql.size(),
                                            "allQueries",
                                            allSql)),
                    StandardOpenOption.CREATE_NEW);
            long first = ((Number) measures.getFirst().get("statements")).longValue();
            assertThat(((Number) measures.getLast().get("statements")).longValue())
                    .isLessThanOrEqualTo(first + 2);
        } finally {
            statistics.setStatisticsEnabled(previous);
        }
    }

    @Test
    void perf01MapaOcupadoTemConsultasLimitadasPelaPaginaSemNMaisUm() throws Exception {
        var c = client();
        var a = warehouse("PERF01");
        var produto =
                new Produto(
                        c,
                        "PERF01",
                        "Fictício",
                        "UN",
                        TipoQuantidade.CONTAGEM,
                        0,
                        false,
                        false,
                        null,
                        now);
        em.persist(produto);
        var embalagem = new Embalagem(produto, "PERF01", "Fictícia", BigDecimal.ONE, now);
        em.persist(embalagem);
        var pedido = new PedidoEntrada(c, a, "PERF01", now);
        em.persist(pedido);
        var nota = new NotaEntrada(pedido, "00000000000000", 1, 1, LocalDate.of(2026, 10, 9), null);
        em.persist(nota);
        var itemNota =
                new ItemNotaEntrada(
                        nota, 1, produto, new BigDecimal("40"), new BigDecimal("40.00"));
        em.persist(itemNota);
        var chegada =
                new ChegadaRecebimento(
                        pedido, "perf01", "ficticio", now, now, "teste", "Chegada fictícia local");
        em.persist(chegada);
        var item =
                new ItemChegada(
                        chegada, 1, itemNota, null, null, new BigDecimal("40"), BigDecimal.ZERO);
        em.persist(item);
        var entrada = new EntradaConferida(item, now, now, new BigDecimal("40"), BigDecimal.ZERO);
        em.persist(entrada);
        var saida = new PedidoSaida(c, a, "PERF01", now);
        em.persist(saida);
        var itemSaida = new ItemPedidoSaida(saida, produto, new BigDecimal("20"));
        em.persist(itemSaida);
        var expected = new LinkedHashMap<Long, Boolean>();
        for (int i = 0; i < 40; i++) {
            var u =
                    new UnidadeLogistica(
                            entrada,
                            embalagem,
                            TipoUnidadeLogistica.PALLET,
                            CondicaoMercadoria.BOA,
                            BigDecimal.ONE,
                            now);
            em.persist(u);
            var e = address(a, String.format("P%02d", i), 1, TipoEndereco.ARMAZENAGEM);
            // Recupera a ocupação criada pelo helper via JPA, somente no H2 da classe.
            em.createQuery(
                            "select o from OcupacaoEndereco o where o.endereco=:e",
                            OcupacaoEndereco.class)
                    .setParameter("e", e)
                    .getSingleResult()
                    .atribuir(u);
            if (i % 2 == 0)
                em.persist(new ReservaSaida(itemSaida, u, "perf01", BigDecimal.ONE, now));
            expected.put(e.getId(), i % 2 == 0);
        }
        em.flush();
        login("OPERACAO", List.of(c.getId()), List.of(a.getId()));
        var statistics = em.getEntityManagerFactory().unwrap(SessionFactory.class).getStatistics();
        boolean previous = statistics.isStatisticsEnabled();
        statistics.setStatisticsEnabled(true);
        var measures = new ArrayList<Map<String, Object>>();
        try {
            for (int size : List.of(1, 8, 40)) {
                em.clear();
                statistics.clear();
                Perf01StatementInspector.iniciar();
                long start = System.nanoTime();
                var result = service.consultar(c.getId(), a.getId(), "UTC", "P", "TODAS", 0, size);
                long elapsed = System.nanoTime() - start;
                var sql = Perf01StatementInspector.terminar();
                assertThat(result.capacidade()).isEqualTo(40);
                assertThat(result.posicoesOcupadas()).isEqualTo(40);
                assertThat(result.posicoesLivres()).isZero();
                assertThat(result.unidadesArmazenadas()).isEqualTo(40);
                assertThat(result.reservasAtivas()).isEqualTo(20);
                assertThat(result.mapa().totalItens()).isEqualTo(40);
                assertThat(result.mapa().itens()).hasSize(size);
                assertThat(result.valorArmazenado()).isNull();
                assertThat(result.financeiroPermitido()).isFalse();
                for (var pos : result.mapa().itens())
                    assertThat(pos.reservada()).isEqualTo(expected.get(pos.id()));
                var m = new LinkedHashMap<String, Object>();
                m.put("pageSize", size);
                m.put("statements", sql.size());
                m.put(
                        "parameterPlaceholders",
                        sql.stream()
                                .mapToLong(q -> q.chars().filter(ch -> ch == '?').count())
                                .sum());
                m.put("entitiesLoaded", statistics.getEntityLoadCount());
                m.put("elapsedNanosDiagnosticOnly", elapsed);
                m.put("queries", sql);
                measures.add(m);
            }
            var proof =
                    Map.of(
                            "demand",
                            "QUAL-CONF01-PERF01",
                            "context",
                            "H2 visao-operacao",
                            "clienteId",
                            c.getId(),
                            "armazemId",
                            a.getId(),
                            "fixtureUnits",
                            40,
                            "fixtureReservations",
                            20,
                            "mapTotal",
                            40,
                            "measurements",
                            measures);
            Files.writeString(
                    Path.of(System.getProperty("wms.test.evidencias.dir"), "perf01-mapa.json"),
                    JsonMapper.builder().build().writeValueAsString(proof),
                    StandardOpenOption.CREATE_NEW);
            // Limite estrutural: ampliar uma página não deve acrescentar consulta por célula.
            long first = ((Number) measures.getFirst().get("statements")).longValue();
            assertThat(((Number) measures.getLast().get("statements")).longValue())
                    .isLessThanOrEqualTo(first + 2);
        } finally {
            statistics.setStatisticsEnabled(previous);
        }
    }

    private Map<String, Object> carga01Medida(
            org.hibernate.stat.Statistics stats, List<String> sql, long elapsed, Object result) {
        var measurement = new LinkedHashMap<String, Object>();
        measurement.put("statements", sql.size());
        measurement.put("elapsedNanos", elapsed);
        measurement.put("entitiesLoaded", stats.getEntityLoadCount());
        measurement.put(
                "hqlResultRows",
                java.util.Arrays.stream(stats.getQueries())
                        .mapToLong(q -> stats.getQueryStatistics(q).getExecutionRowCount())
                        .sum());
        measurement.put(
                "parameterPlaceholders",
                sql.stream().mapToLong(q -> q.chars().filter(ch -> ch == '?').count()).sum());
        measurement.put(
                "maxParameterPlaceholders",
                sql.stream()
                        .mapToInt(q -> (int) q.chars().filter(ch -> ch == '?').count())
                        .max()
                        .orElse(0));
        measurement.put(
                "sqlTextBytesUtf8",
                sql.stream()
                        .mapToInt(q -> q.getBytes(java.nio.charset.StandardCharsets.UTF_8).length)
                        .sum());
        measurement.put(
                "responseJsonBytesUtf8",
                JsonMapper.builder()
                        .build()
                        .writeValueAsString(result)
                        .getBytes(java.nio.charset.StandardCharsets.UTF_8)
                        .length);
        measurement.put("sql", sql);
        return measurement;
    }

    private EntradaConferida carga01OutraOrigem(EntradaConferida first, BigDecimal price) {
        var pedido = first.getItemChegada().getChegada().getPedido();
        var produto = first.getItemChegada().getItemNota().getProduto();
        var nota =
                new NotaEntrada(
                        pedido,
                        pedido.getCliente().getDocumentoFiscal(),
                        1,
                        999,
                        LocalDate.of(2026, 10, 9),
                        null);
        em.persist(nota);
        var itemNota = new ItemNotaEntrada(nota, 1, produto, new BigDecimal("10"), price);
        em.persist(itemNota);
        var chegada = first.getItemChegada().getChegada();
        var item =
                new ItemChegada(
                        chegada, 2, itemNota, null, null, new BigDecimal("10"), BigDecimal.ZERO);
        em.persist(item);
        var entrada = new EntradaConferida(item, now, now, new BigDecimal("10"), BigDecimal.ZERO);
        entrada.marcarUnitizada(now);
        em.persist(entrada);
        return entrada;
    }

    // Oráculos numéricos independentes: origens6x2+4x3=24, retirada2 ->20,
    // ajuste1/2 ->12; com preços iguais2 e dano2/2/1:16/12/8.
    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void carga01HistoricoTemporalRetiradaAjusteOrigensEMarcos(boolean precosDistintos)
            throws Exception {
        var c = client();
        var a = warehouse("CARGAHIST");
        var primeira =
                perf01Entrada(
                        c, a, "O90", new BigDecimal("20"), new BigDecimal("10"), BigDecimal.ZERO);
        primeira.marcarUnitizada(now);
        var segunda = carga01OutraOrigem(primeira, new BigDecimal(precosDistintos ? "30" : "20"));
        var embalagem =
                new Embalagem(
                        primeira.getItemChegada().getItemNota().getProduto(),
                        "CARGAHIST",
                        "Fictícia",
                        BigDecimal.ONE,
                        now);
        em.persist(embalagem);
        var u =
                new UnidadeLogistica(
                        primeira,
                        embalagem,
                        TipoUnidadeLogistica.PALLET,
                        CondicaoMercadoria.BOA,
                        new BigDecimal("10"),
                        now);
        em.persist(u);
        var origin1 = new ConteudoUnidade(u, primeira, new BigDecimal("6"));
        var origin2 = new ConteudoUnidade(u, segunda, new BigDecimal("4"));
        em.persist(origin1);
        em.persist(origin2);
        var dano =
                new AvariaEstoque(
                        u,
                        new BigDecimal("2"),
                        new BigDecimal("10"),
                        BigDecimal.ONE,
                        "carga-hist",
                        now.plusSeconds(10),
                        now.plusSeconds(10),
                        "{}",
                        "Fictícia");
        dano.reconhecer("RODOGARCIA", "teste", now.plusSeconds(10));
        em.persist(dano);
        u.registrarAvaria(now.plusSeconds(10));
        var ps = new PedidoSaida(c, a, "CARGAHIST", now);
        em.persist(ps);
        var item = new ItemPedidoSaida(ps, u.getProduto(), new BigDecimal("2"));
        em.persist(item);
        var reserva = new ReservaSaida(item, u, "carga-res", new BigDecimal("2"), now);
        em.persist(reserva);
        var retirada = new RetiradaSaida(ps, now.plusSeconds(60), "teste", "0".repeat(64));
        em.persist(retirada);
        em.persist(new BaixaSaida(retirada, reserva, primeira, new BigDecimal("2")));
        var f1 =
                new FatoPermanencia(
                        u,
                        "retirada",
                        "RETIRADA",
                        now.plusSeconds(60),
                        now.plusSeconds(60),
                        new BigDecimal("10"),
                        new BigDecimal("8"),
                        BigDecimal.ONE,
                        BigDecimal.ONE);
        em.persist(f1);
        em.persist(
                new MarcoFinanceiroAvaria(
                        dano,
                        f1,
                        new BigDecimal("2"),
                        new BigDecimal("8"),
                        BigDecimal.ONE,
                        "teste",
                        now.plusSeconds(60),
                        "Fictício"));
        u.alterarQuantidade(new BigDecimal("8"), now.plusSeconds(60));
        var contagem = new ContagemEstoque(u, now.plusSeconds(120));
        contagem.revisar(true, now.plusSeconds(120));
        em.persist(contagem);
        var revisao =
                new RevisaoContagem(
                        contagem,
                        new BigDecimal("5"),
                        now.plusSeconds(120),
                        now.plusSeconds(120),
                        "teste",
                        "Fictícia",
                        "{}");
        revisao.aplicar(
                now.plusSeconds(120),
                JsonMapper.builder()
                        .build()
                        .writeValueAsString(
                                Map.of(
                                        "origens",
                                        List.of(
                                                Map.of(
                                                        "entradaId",
                                                        primeira.getId(),
                                                        "quantidade",
                                                        "1"),
                                                Map.of(
                                                        "entradaId",
                                                        segunda.getId(),
                                                        "quantidade",
                                                        "2")))));
        em.persist(revisao);
        var f2 =
                new FatoPermanencia(
                        u,
                        "ajuste",
                        "AJUSTE_ESTOQUE",
                        now.plusSeconds(120),
                        now.plusSeconds(120),
                        new BigDecimal("8"),
                        new BigDecimal("5"),
                        BigDecimal.ONE,
                        BigDecimal.ONE);
        em.persist(f2);
        em.persist(
                new MarcoFinanceiroAvaria(
                        dano,
                        f2,
                        BigDecimal.ONE,
                        new BigDecimal("5"),
                        BigDecimal.ONE,
                        "teste",
                        now.plusSeconds(120),
                        "Fictício"));
        u.alterarQuantidade(new BigDecimal("5"), now.plusSeconds(120));
        origin1.alterarQuantidade(new BigDecimal("3"));
        origin2.alterarQuantidade(new BigDecimal("2"));
        em.flush();
        em.clear();
        login("SUPERVISOR", List.of(c.getId()), List.of(a.getId()));
        var units = List.of(em.find(UnidadeLogistica.class, u.getId()));
        var proofs = new ArrayList<Map<String, Object>>();
        int[] seconds = {1, 30, 60, 120};
        String[] quantity = {"10", "10", "8", "5"};
        String[] values = {precosDistintos ? "24" : "20", "16", "12", "8"};
        String[][] origins = {{"6", "4"}, {"6", "4"}, {"4", "4"}, {"3", "2"}};
        for (int i = 0; i < seconds.length; i++) {
            Perf01StatementInspector.iniciar();
            var v =
                    calculos.indicadoresUnidades(
                                    units, now.plusSeconds(seconds[i]), java.time.ZoneOffset.UTC)
                            .getFirst();
            var sql = Perf01StatementInspector.terminar();
            assertThat(v.unidade().quantidade()).isEqualByComparingTo(quantity[i]);
            assertThat(v.unidade().origens()).hasSize(2);
            assertThat(v.unidade().origens().get(0).quantidade())
                    .isEqualByComparingTo(origins[i][0]);
            assertThat(v.unidade().origens().get(1).quantidade())
                    .isEqualByComparingTo(origins[i][1]);
            if (precosDistintos && i > 0) {
                assertThat(v.unidade().valorEstoque()).isNull();
                assertThat(v.pendencias())
                        .extracting(pend -> pend.codigo())
                        .containsExactly("ORIGEM_AVARIA_VALOR_INSUFICIENTE");
            } else {
                assertThat(v.unidade().valorEstoque()).isEqualByComparingTo(values[i]);
                assertThat(v.pendencias()).isEmpty();
            }
            assertThat(
                            sql.stream()
                                    .filter(q -> q.contains(" from wms.marco_financeiro_avaria "))
                                    .count())
                    .isEqualTo(1);
            assertThat(sql.stream().filter(q -> q.contains(" from wms.fato_permanencia ")).count())
                    .isEqualTo(1);
            proofs.add(Map.of("second", seconds[i], "result", v, "sql", sql));
        }
        Files.writeString(
                Path.of(
                        System.getProperty("wms.test.evidencias.dir"),
                        "carga01-temporal-" + precosDistintos + ".json"),
                JsonMapper.builder().build().writeValueAsString(proofs),
                StandardOpenOption.CREATE_NEW);
    }

    @Test
    void carga01VazioEAlcanceRecusadoAntesHistoricos() {
        var c = client();
        var a = warehouse("CARGADENY");
        var entrada = perf01Entrada(c, a, "O91", BigDecimal.TEN, BigDecimal.ONE, BigDecimal.ZERO);
        var embalagem =
                new Embalagem(
                        entrada.getItemChegada().getItemNota().getProduto(),
                        "DENY",
                        "Fictícia",
                        BigDecimal.ONE,
                        now);
        em.persist(embalagem);
        var u =
                new UnidadeLogistica(
                        entrada,
                        embalagem,
                        TipoUnidadeLogistica.PALLET,
                        CondicaoMercadoria.BOA,
                        BigDecimal.ONE,
                        now);
        em.persist(u);
        em.flush();
        login("SUPERVISOR", List.of(c.getId()), List.of(a.getId()));
        Perf01StatementInspector.iniciar();
        assertThat(calculos.indicadoresUnidades(List.of(), now, java.time.ZoneOffset.UTC))
                .isEmpty();
        assertThat(Perf01StatementInspector.terminar()).isEmpty();
        login("SUPERVISOR", List.of(c.getId() + 1000), List.of(a.getId()));
        Perf01StatementInspector.iniciar();
        assertThatThrownBy(
                        () ->
                                calculos.indicadoresUnidades(
                                        List.of(u), now, java.time.ZoneOffset.UTC))
                .isInstanceOf(AccessDeniedException.class);
        assertThat(Perf01StatementInspector.terminar()).isEmpty();
        login("SUPERVISOR", List.of(c.getId()), List.of(a.getId() + 1000));
        Perf01StatementInspector.iniciar();
        assertThatThrownBy(
                        () ->
                                calculos.indicadoresUnidades(
                                        List.of(u), now, java.time.ZoneOffset.UTC))
                .isInstanceOf(AccessDeniedException.class);
        assertThat(Perf01StatementInspector.terminar()).isEmpty();
        login("OPERACAO", List.of(c.getId()), List.of(a.getId()));
        Perf01StatementInspector.iniciar();
        assertThatThrownBy(
                        () ->
                                calculos.indicadoresUnidades(
                                        List.of(u), now, java.time.ZoneOffset.UTC))
                .isInstanceOf(AccessDeniedException.class);
        assertThat(Perf01StatementInspector.terminar()).isEmpty();
    }

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
