package br.com.rodogarcia.wms;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.rodogarcia.wms.config.IdentidadeTesteConfig;
import br.com.rodogarcia.wms.models.Armazem;
import br.com.rodogarcia.wms.models.Cliente;
import br.com.rodogarcia.wms.models.Embalagem;
import br.com.rodogarcia.wms.models.Produto;
import br.com.rodogarcia.wms.models.TipoQuantidade;
import br.com.rodogarcia.wms.repositories.ArmazemRepository;
import br.com.rodogarcia.wms.repositories.ClienteRepository;
import br.com.rodogarcia.wms.repositories.EmbalagemRepository;
import br.com.rodogarcia.wms.repositories.ProdutoRepository;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@ActiveProfiles("test")
@TestPropertySource(
        locations = "classpath:cadastros-test.properties",
        properties =
                "spring.datasource.url=jdbc:h2:mem:wms-be14;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000;INIT=CREATE SCHEMA IF NOT EXISTS wms")
@Import(IdentidadeTesteConfig.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ContingenciaIntegrationTest {
    @org.springframework.test.context.bean.override.mockito.MockitoSpyBean
    private br.com.rodogarcia.wms.services.AuditoriaService auditoria;

    @org.springframework.test.context.bean.override.mockito.MockitoSpyBean
    private br.com.rodogarcia.wms.services.ContingenciaService contingencia;

    @org.springframework.test.context.bean.override.mockito.MockitoSpyBean
    private br.com.rodogarcia.wms.services.ContagemEstoqueService contagens;

    @org.springframework.test.context.bean.override.mockito.MockitoSpyBean
    private br.com.rodogarcia.wms.services.PedidoEntradaService pedidosEntrada;

    private void limpar() {
        br.com.rodogarcia.wms.services.AuditoriaService alvo =
                org.springframework.test.util.AopTestUtils.getUltimateTargetObject(auditoria);
        org.mockito.Mockito.reset(alvo);
        br.com.rodogarcia.wms.services.ContingenciaService alvoContingencia =
                org.springframework.test.util.AopTestUtils.getUltimateTargetObject(contingencia);
        org.mockito.Mockito.reset(alvoContingencia);
        br.com.rodogarcia.wms.services.ContagemEstoqueService alvoContagens =
                org.springframework.test.util.AopTestUtils.getUltimateTargetObject(contagens);
        br.com.rodogarcia.wms.services.PedidoEntradaService alvoPedidos =
                org.springframework.test.util.AopTestUtils.getUltimateTargetObject(pedidosEntrada);
        org.mockito.Mockito.reset(alvoContagens, alvoPedidos);
        var tabelas =
                jdbc.queryForList(
                        "select table_name from information_schema.tables where table_schema='WMS' and table_type='BASE TABLE'",
                        String.class);
        var filhos = new HashMap<String, java.util.Set<String>>();
        jdbc.query(
                "select fk.table_name filha,pk.table_name pai from information_schema.referential_constraints r join information_schema.table_constraints fk on fk.constraint_schema=r.constraint_schema and fk.constraint_name=r.constraint_name join information_schema.table_constraints pk on pk.constraint_schema=r.unique_constraint_schema and pk.constraint_name=r.unique_constraint_name where fk.table_schema='WMS'",
                rs -> {
                    filhos.computeIfAbsent(rs.getString("pai"), k -> new java.util.HashSet<>())
                            .add(rs.getString("filha"));
                });
        var limpas = new java.util.HashSet<String>();
        for (var tabela : tabelas) limparTabela(tabela, filhos, limpas, new java.util.HashSet<>());
    }

    private void limparTabela(
            String tabela,
            Map<String, java.util.Set<String>> filhos,
            java.util.Set<String> limpas,
            java.util.Set<String> pilha) {
        if (limpas.contains(tabela)) return;
        assertThat(pilha.add(tabela)).as("Ciclo de FKs no fixture").isTrue();
        for (var filha : filhos.getOrDefault(tabela, java.util.Set.of()))
            limparTabela(filha, filhos, limpas, pilha);
        jdbc.update("delete from wms." + tabela);
        pilha.remove(tabela);
        limpas.add(tabela);
    }

    private static final String PEDIDOS = "/api/v1/pedidos-entrada/";
    private static final String UNIDADES = "/api/v1/unidades-logisticas/";
    private static final String CHEGADA = "2026-09-01T12:00:00Z";
    private final HttpClient http =
            HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    @Autowired private Environment environment;
    @Autowired private JsonMapper mapper;
    @Autowired private JwtEncoder encoder;
    @Autowired private JwtDecoder d30Decoder;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ClienteRepository clientes;
    @Autowired private ArmazemRepository armazens;
    @Autowired private ProdutoRepository produtos;
    @Autowired private EmbalagemRepository embalagens;
    private Cliente cliente;
    private Armazem armazem;
    private Produto produto;
    private Embalagem embalagem;
    private String operador;
    private String supervisor;
    private String gestor;
    private long numero;

    @BeforeEach
    void preparar() {
        limpar();
        var agora = Instant.now();
        cliente =
                clientes.saveAndFlush(
                        new Cliente("CLIENTE", "Cliente ficticio", "12345678000199", agora));
        armazem =
                armazens.saveAndFlush(
                        new Armazem(
                                "ARM",
                                "Armazem ficticio",
                                "98765432000188",
                                "Osasco",
                                "SP",
                                agora));
        criarProduto();
        gestor = token("GESTOR", List.of(), List.of());
        operador = token("OPERACAO", List.of(cliente.getId()), List.of(armazem.getId()));
        supervisor = token("SUPERVISOR", List.of(cliente.getId()), List.of(armazem.getId()));
        numero = 0;
    }

    @Test
    void d26PrimeiraContagemRespeitaRevisaoMinimaPersistida() throws Exception {
        // Restrição independente copiada do contrato V9; H2 não prova SQL Server.
        jdbc.execute(
                "ALTER TABLE wms.contagem_estoque ADD CONSTRAINT d26_contagem_revisao CHECK (revisao_atual >= 1)");
        try {
            var u = unitizar(receber("100", "0"), "100", "BOA");
            var resultado = contar(u, "100");
            assertThat(resultado.get("revisao").intValue()).isEqualTo(1);
            assertThat(contar("contagem_estoque")).isEqualTo(1);
            assertThat(contar("revisao_contagem")).isEqualTo(1);
        } finally {
            jdbc.execute("ALTER TABLE wms.contagem_estoque DROP CONSTRAINT d26_contagem_revisao");
        }
    }

    @Test
    void d26PrimeiraCargaRespeitaRevisaoMinimaPersistida() throws Exception {
        jdbc.execute(
                "ALTER TABLE wms.carga_inicial ADD CONSTRAINT d26_carga_revisao CHECK (revisao_atual >= 1)");
        try {
            var resultado = carga();
            assertThat(resultado.get("revisao").get("numero").intValue()).isEqualTo(1);
            assertThat(contar("carga_inicial")).isEqualTo(1);
            assertThat(contar("revisao_carga_inicial")).isEqualTo(1);
        } finally {
            jdbc.execute("ALTER TABLE wms.carga_inicial DROP CONSTRAINT d26_carga_revisao");
        }
    }

    @Test
    void consultasAtuaisPaginamFiltramUnidadeEnderecoSituacaoEAlcance() throws Exception {
        var a = unitizar(receber("100", "0"), "100", "BOA");
        var b = unitizar(receber("50", "0"), "50", "BOA");
        var ea = endereco("A01", "ARMAZENAGEM");
        var eb = endereco("B01", "ARMAZENAGEM");
        posicionar(a, ea);
        posicionar(b, eb);
        a = atual(a);
        b = atual(b);
        var ca = contar(a, "70");
        var cb = contar(b, "50");
        String ctx = "?clienteId=" + cliente.getId() + "&armazemId=" + armazem.getId();
        var pag = resposta(get("/api/v1/contagens" + ctx + "&tamanho=1", operador), 200);
        assertThat(pag.get("totalItens").longValue()).isEqualTo(2);
        assertThat(pag.get("itens").get(0).get("id").longValue())
                .isEqualTo(ca.get("id").longValue());
        assertThat(
                        resposta(
                                        get(
                                                "/api/v1/contagens" + ctx + "&tamanho=1&pagina=1",
                                                operador),
                                        200)
                                .get("itens")
                                .get(0)
                                .get("id")
                                .longValue())
                .isEqualTo(cb.get("id").longValue());
        var fil =
                resposta(
                        get(
                                "/api/v1/contagens"
                                        + ctx
                                        + "&situacao=PENDENTE&impedimento=true&enderecoId="
                                        + ea.get("id").longValue()
                                        + "&codigoUnidade="
                                        + a.get("codigo").asString()
                                        + "&produtoId="
                                        + produto.getId(),
                                operador),
                        200);
        assertThat(fil.get("totalItens").longValue()).isEqualTo(1);
        assertThat(
                        resposta(
                                        get(
                                                "/api/v1/contagens/"
                                                        + ca.get("id").longValue()
                                                        + "/revisoes?situacao=APLICADA",
                                                operador),
                                        200)
                                .get("totalItens")
                                .longValue())
                .isZero();
        var carga = carga();
        var cancelar =
                Map.of(
                        "operacaoId",
                        UUID.randomUUID(),
                        "versao",
                        carga.get("versao").longValue(),
                        "motivo",
                        "Cancelamento de proposta fornecida");
        resposta(
                post(
                        "/api/v1/cargas-iniciais/" + carga.get("id").longValue() + "/cancelar",
                        cancelar,
                        gestor),
                200);
        var cargas =
                resposta(
                        get(
                                "/api/v1/cargas-iniciais"
                                        + ctx
                                        + "&situacao=CANCELADA&referencia=l1&produtoId="
                                        + produto.getId(),
                                supervisor),
                        200);
        assertThat(cargas.get("totalItens").longValue()).isEqualTo(1);
        assertThat(
                        resposta(
                                        get(
                                                "/api/v1/cargas-iniciais"
                                                        + ctx
                                                        + "&situacao=PENDENTE",
                                                supervisor),
                                        200)
                                .get("totalItens")
                                .longValue())
                .isZero();
        assertThat(
                        resposta(
                                        get(
                                                "/api/v1/cargas-iniciais/"
                                                        + carga.get("id").longValue()
                                                        + "/revisoes?tamanho=1",
                                                supervisor),
                                        200)
                                .get("totalItens")
                                .longValue())
                .isEqualTo(1);
        var stock =
                resposta(
                        get(
                                "/api/v1/estoque"
                                        + ctx
                                        + "&situacao=CONTAGEM_PENDENTE&enderecoId="
                                        + ea.get("id").longValue()
                                        + "&codigoUnidade="
                                        + a.get("codigo").asString(),
                                operador),
                        200);
        assertThat(stock.get("totalItens").longValue()).isEqualTo(1);
        assertThat(
                        resposta(
                                        get(
                                                "/api/v1/estoque" + ctx + "&situacao=DISPONIVEL",
                                                operador),
                                        200)
                                .get("totalItens")
                                .longValue())
                .isEqualTo(1);
        var produto2 =
                produtos.saveAndFlush(
                        new Produto(
                                cliente,
                                "SKU2",
                                "SKU sem saldo",
                                "UN",
                                TipoQuantidade.CONTAGEM,
                                0,
                                false,
                                false,
                                null,
                                Instant.now()));
        var indicadores =
                resposta(
                        get("/api/v1/indicadores-estoque" + ctx + "&fuso=UTC&tamanho=1", operador),
                        200);
        assertThat(indicadores.get("totalItens").longValue()).isEqualTo(2);
        assertThat(indicadores.get("itens").size()).isEqualTo(1);
        var filtrado =
                resposta(
                        get(
                                "/api/v1/indicadores-estoque"
                                        + ctx
                                        + "&fuso=UTC&produtoId="
                                        + produto2.getId(),
                                operador),
                        200);
        assertThat(filtrado.get("totalItens").longValue()).isEqualTo(1);
        assertThat(filtrado.get("itens").get(0).get("produtoId").longValue())
                .isEqualTo(produto2.getId());
        var semAlcance = token("SUPERVISOR", List.of(), List.of());
        for (String rota :
                List.of(
                        "/api/v1/contagens",
                        "/api/v1/cargas-iniciais",
                        "/api/v1/estoque",
                        "/api/v1/indicadores-estoque")) {
            String fuso = rota.endsWith("indicadores-estoque") ? "&fuso=UTC" : "";
            resposta(get(rota + ctx + fuso, semAlcance), 403);
            resposta(get(rota + ctx + fuso, null), 401);
            resposta(get(rota + ctx + fuso + "&pagina=-1", supervisor), 400);
            resposta(get(rota + ctx + fuso + "&tamanho=101", supervisor), 400);
            assertThat(
                            resposta(get(rota + ctx + fuso + "&tamanho=100", supervisor), 200)
                                    .get("tamanho")
                                    .intValue())
                    .isEqualTo(100);
        }
        resposta(get("/api/v1/estoque" + ctx + "&situacao=INVALIDA", operador), 400);
        resposta(get("/api/v1/cargas-iniciais/" + carga.get("id").longValue(), operador), 403);
    }

    @Test
    void vinculoSeparacaoERetornoConfereMarcoSnapshotENaoRepeteEfeito() throws Exception {
        var u = unitizar(receber("100", "0"), "100", "BOA");
        var origem = endereco("A01", "ARMAZENAGEM");
        posicionar(u, origem);
        u = atual(u);
        var criado =
                resposta(
                                post(
                                        "/api/v1/pedidos-saida",
                                        Map.of(
                                                "operacaoId",
                                                UUID.randomUUID(),
                                                "clienteId",
                                                cliente.getId(),
                                                "armazemId",
                                                armazem.getId(),
                                                "referencia",
                                                "SAIDA-MARCOS",
                                                "motivo",
                                                "Pedido integral comprovado",
                                                "itens",
                                                List.of(
                                                        Map.of(
                                                                "produtoId",
                                                                produto.getId(),
                                                                "quantidade",
                                                                "100"))),
                                        operador),
                                201)
                        .get("pedido");
        long id = criado.get("id").longValue();
        String rota = "/api/v1/pedidos-saida/" + id;
        var p =
                resposta(post(rota + "/reserva", comandoSaida(criado), operador), 200)
                        .get("pedido");
        long reserva = p.get("reservas").get(0).get("id").longValue();
        var leitura = comandoSaida(p);
        leitura.put("reservaId", reserva);
        leitura.put("codigoLido", u.get("codigo").asString());
        leitura.put(
                "revisaoConteudo",
                resposta(get(UNIDADES + u.get("codigo").asString() + "/etiqueta", operador), 200)
                        .get("versaoConteudo")
                        .longValue());
        p =
                resposta(post(rota + "/leituras", leitura, operador), 200)
                        .get("expedicao")
                        .get("pedido");
        var destino = endereco("S01", "SEPARACAO");
        var separar = comandoSaida(p);
        separar.put(
                "destinacao",
                Map.of(
                        "reservaId",
                        reserva,
                        "destinos",
                        List.of(
                                Map.of(
                                        "enderecoId",
                                        destino.get("id").longValue(),
                                        "codigoLido",
                                        destino.get("codigo").asString()))));
        var separada = resposta(post(rota + "/separacoes", separar, operador), 200);
        conferirVinculoTemporal(
                "SEPARACAO",
                id,
                separar,
                separada.get("expedicao").get("separacoes").get(0).get("separadaEm").asString());
        p = separada.get("expedicao").get("pedido");
        var retornar = comandoSaida(p);
        retornar.put(
                "unidades",
                List.of(
                        Map.of(
                                "reservaId",
                                reserva,
                                "destinos",
                                List.of(
                                        Map.of(
                                                "enderecoId",
                                                origem.get("id").longValue(),
                                                "codigoLido",
                                                origem.get("codigo").asString())))));
        var retornada = resposta(post(rota + "/retorno-interno", retornar, supervisor), 200);
        conferirVinculoTemporal(
                "RETORNO",
                id,
                retornar,
                retornada.get("expedicao").get("separacoes").get(0).get("encerradaEm").asString());
        assertThat(saldo().get("fisicoTotal").decimalValue()).isEqualByComparingTo("100");
        assertThat(saldo().get("reservado").decimalValue()).isZero();
        assertThat(saldo().get("disponivel").decimalValue()).isEqualByComparingTo("100");
        assertThat(contar("pedido_entrada")).isEqualTo(1);
        assertThat(contar("separacao_saida")).isEqualTo(1);
    }

    private void conferirVinculoTemporal(
            String tipo, long pedido, Map<String, Object> dados, String marco) throws Exception {
        String operacao = dados.get("operacaoId").toString();
        String hash =
                jdbc.queryForObject(
                        "select conteudo_hash from wms.operacao_saida where pedido_id=? and operacao_id=?",
                        String.class,
                        pedido,
                        operacao);
        var instante = Instant.parse(marco);
        var incorreta =
                resposta(
                        post(
                                "/api/v1/contingencias",
                                linha(
                                        tipo + "ERRADA",
                                        tipo,
                                        instante.minusSeconds(86400),
                                        List.of(),
                                        true,
                                        Map.of("pedidoId", pedido, "dados", dados)),
                                supervisor),
                        200);
        var c = new HashMap<String, Object>(conciliar(incorreta));
        c.put("modo", "VINCULAR");
        c.put("prova", Map.of("operacaoOriginal", operacao, "conteudoHash", hash));
        var r =
                resposta(
                        post(
                                "/api/v1/contingencias/"
                                        + incorreta.get("id").longValue()
                                        + "/conciliar",
                                c,
                                supervisor),
                        200);
        assertThat(r.get("pendencia").asString()).isEqualTo("INSTANTE_CONTINGENCIA_DIVERGENTE");
        var correta =
                resposta(
                        post(
                                "/api/v1/contingencias",
                                linha(
                                        tipo + "VALIDA",
                                        tipo,
                                        instante,
                                        List.of(),
                                        true,
                                        Map.of("pedidoId", pedido, "dados", dados)),
                                supervisor),
                        200);
        c.put("operacaoId", UUID.randomUUID());
        c.put("versao", correta.get("versao").longValue());
        long ops = contar("operacao_saida");
        long movimentos = contar("movimento_estoque");
        var atual =
                resposta(
                        post(
                                "/api/v1/contingencias/"
                                        + correta.get("id").longValue()
                                        + "/conciliar",
                                c,
                                supervisor),
                        200);
        assertThat(atual.get("situacao").asString()).isEqualTo("CONCILIADA");
        assertThat(
                        resposta(
                                post(
                                        "/api/v1/contingencias/"
                                                + correta.get("id").longValue()
                                                + "/conciliar",
                                        c,
                                        supervisor),
                                200))
                .isEqualTo(atual);
        resposta(
                post(
                        "/api/v1/contingencias/" + correta.get("id").longValue() + "/conciliar",
                        c,
                        operador),
                403);
        assertThat(contar("operacao_saida")).isEqualTo(ops);
        assertThat(contar("movimento_estoque")).isEqualTo(movimentos);
        var duplicada =
                resposta(
                        post(
                                "/api/v1/contingencias",
                                linha(
                                        tipo + "DUPLICADA",
                                        tipo,
                                        instante,
                                        List.of(),
                                        true,
                                        Map.of("pedidoId", pedido, "dados", dados)),
                                supervisor),
                        200);
        c.put("operacaoId", UUID.randomUUID());
        c.put("versao", duplicada.get("versao").longValue());
        assertThat(
                        resposta(
                                        post(
                                                "/api/v1/contingencias/"
                                                        + duplicada.get("id").longValue()
                                                        + "/conciliar",
                                                c,
                                                supervisor),
                                        200)
                                .get("pendencia")
                                .asString())
                .isEqualTo("EFEITO_CONTINGENCIA_JA_VINCULADO");
    }

    private HashMap<String, Object> comandoSaida(JsonNode p) {
        return new HashMap<>(
                Map.of(
                        "operacaoId",
                        UUID.randomUUID(),
                        "versao",
                        p.get("versao").longValue(),
                        "motivo",
                        "Operação integral identificada"));
    }

    @Test
    void dependenciaAusentePreservaJsonReplayHistoricoEConsultaAtualFiltrada() throws Exception {
        var u = unitizar(receber("100", "0"), "100", "BOA");
        posicionar(u, endereco("A01", "ARMAZENAGEM"));
        u = atual(u);
        var t1 = Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MICROS);
        var t2 = Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MICROS);
        var l2dados =
                linha(
                        "L2",
                        "CONTAGEM",
                        t2,
                        List.of("L1"),
                        false,
                        Map.of(
                                "codigoUnidade",
                                u.get("codigo").asString(),
                                "dados",
                                leituraContagem(u, "70", t2)));
        var l2 = resposta(post("/api/v1/contingencias", l2dados, supervisor), 200);
        String original =
                jdbc.queryForObject(
                        "select conteudo_json from wms.linha_contingencia where id=?",
                        String.class,
                        l2.get("id").longValue());
        var pendente =
                resposta(
                        post(
                                "/api/v1/contingencias/" + l2.get("id").longValue() + "/conciliar",
                                conciliar(l2),
                                supervisor),
                        200);
        assertThat(pendente.get("pendencia").asString()).isEqualTo("DEPENDENCIA_AUSENTE");
        assertThat(contar("dependencia_contingencia")).isZero();
        assertThat(contar("contagem_estoque")).isZero();
        var l1 =
                resposta(
                        post(
                                "/api/v1/contingencias",
                                linha(
                                        "L1",
                                        "CONTAGEM",
                                        t1,
                                        List.of(),
                                        false,
                                        Map.of(
                                                "codigoUnidade",
                                                u.get("codigo").asString(),
                                                "dados",
                                                leituraContagem(u, "70", t1))),
                                supervisor),
                        200);
        resposta(
                post(
                        "/api/v1/contingencias/" + l1.get("id").longValue() + "/conciliar",
                        conciliar(l1),
                        supervisor),
                200);
        var comando = conciliar(pendente);
        var r =
                resposta(
                        post(
                                "/api/v1/contingencias/" + l2.get("id").longValue() + "/conciliar",
                                comando,
                                supervisor),
                        200);
        assertThat(r.get("situacao").asString()).isEqualTo("CONCILIADA");
        assertThat(resposta(post("/api/v1/contingencias", l2dados, supervisor), 200)).isEqualTo(l2);
        assertThat(
                        resposta(
                                post(
                                        "/api/v1/contingencias/"
                                                + l2.get("id").longValue()
                                                + "/conciliar",
                                        comando,
                                        supervisor),
                                200))
                .isEqualTo(r);
        assertThat(
                        resposta(
                                        get(
                                                "/api/v1/contingencias/" + l2.get("id").longValue(),
                                                supervisor),
                                        200)
                                .get("situacao")
                                .asString())
                .isEqualTo("CONCILIADA");
        assertThat(
                        jdbc.queryForObject(
                                "select conteudo_json from wms.linha_contingencia where id=?",
                                String.class,
                                l2.get("id").longValue()))
                .isEqualTo(original);
        assertThat(contar("dependencia_contingencia")).isEqualTo(1);
        assertThat(contar("revisao_contagem")).isEqualTo(2);
        String ctx = "?clienteId=" + cliente.getId() + "&armazemId=" + armazem.getId();
        var lista =
                resposta(
                        get(
                                "/api/v1/contingencias"
                                        + ctx
                                        + "&situacao=CONCILIADA&tipo=CONTAGEM&tamanho=1",
                                supervisor),
                        200);
        assertThat(lista.get("totalItens").longValue()).isEqualTo(2);
        assertThat(lista.get("itens").size()).isEqualTo(1);
        assertThat(
                        resposta(
                                        get(
                                                "/api/v1/contingencias"
                                                        + ctx
                                                        + "&identidadeFato=L2&pendencia=DEPENDENCIA_AUSENTE",
                                                supervisor),
                                        200)
                                .get("totalItens")
                                .longValue())
                .isZero();
        resposta(get("/api/v1/contingencias" + ctx, operador), 403);
        resposta(get("/api/v1/contingencias" + ctx + "&tamanho=101", supervisor), 400);
        var fora = token("SUPERVISOR", List.of(), List.of());
        resposta(get("/api/v1/contingencias/" + l2.get("id").longValue(), fora), 403);
        var divergente = new HashMap<>(l2dados);
        divergente.put("operacaoId", UUID.randomUUID());
        divergente.put("tipo", "AVARIA");
        resposta(post("/api/v1/contingencias", divergente, supervisor), 409);
    }

    @Test
    void registroDependenteEConciliacaoSerializamContextoSemInverterLinha() throws Exception {
        var u = unitizar(receber("100", "0"), "100", "BOA");
        posicionar(u, endereco("A01", "ARMAZENAGEM"));
        u = atual(u);
        var instante = Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MICROS);
        var b =
                resposta(
                        post(
                                "/api/v1/contingencias",
                                linha(
                                        "B",
                                        "CONTAGEM",
                                        instante,
                                        List.of(),
                                        false,
                                        Map.of(
                                                "codigoUnidade",
                                                u.get("codigo").asString(),
                                                "dados",
                                                leituraContagem(u, "70", instante))),
                                supervisor),
                        200);
        var entrou = new CountDownLatch(1);
        var liberar = new CountDownLatch(1);
        var conciliarEntrou = new CountDownLatch(1);
        br.com.rodogarcia.wms.services.AuditoriaService alvoAudit =
                org.springframework.test.util.AopTestUtils.getUltimateTargetObject(auditoria);
        br.com.rodogarcia.wms.services.ContingenciaService alvo =
                org.springframework.test.util.AopTestUtils.getUltimateTargetObject(contingencia);
        org.mockito.Mockito.doAnswer(
                        inv -> {
                            if ("REGISTRO_CONTINGENCIA".equals(inv.getArgument(2))) {
                                entrou.countDown();
                                if (!liberar.await(10, TimeUnit.SECONDS))
                                    throw new IllegalStateException("Barreira sem liberação");
                            }
                            return inv.callRealMethod();
                        })
                .when(alvoAudit)
                .registrar(
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any());
        org.mockito.Mockito.doAnswer(
                        inv -> {
                            conciliarEntrou.countDown();
                            return inv.callRealMethod();
                        })
                .when(alvo)
                .conciliar(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        var dadosA =
                linha(
                        "A",
                        "CONTAGEM",
                        instante,
                        List.of("B"),
                        false,
                        Map.of(
                                "codigoUnidade",
                                u.get("codigo").asString(),
                                "dados",
                                leituraContagem(u, "70", instante)));
        try (var executor = Executors.newFixedThreadPool(2)) {
            var registro = executor.submit(() -> post("/api/v1/contingencias", dadosA, supervisor));
            assertThat(entrou.await(10, TimeUnit.SECONDS)).isTrue();
            var conciliacao =
                    executor.submit(
                            () ->
                                    post(
                                            "/api/v1/contingencias/"
                                                    + b.get("id").longValue()
                                                    + "/conciliar",
                                            conciliar(b),
                                            supervisor));
            assertThat(conciliarEntrou.await(10, TimeUnit.SECONDS)).isTrue();
            org.assertj.core.api.Assertions.assertThatThrownBy(
                            () -> conciliacao.get(150, TimeUnit.MILLISECONDS))
                    .isInstanceOf(java.util.concurrent.TimeoutException.class);
            liberar.countDown();
            resposta(registro.get(15, TimeUnit.SECONDS), 200);
            var r = resposta(conciliacao.get(15, TimeUnit.SECONDS), 200);
            assertThat(r.get("situacao").asString()).isEqualTo("CONCILIADA");
        } finally {
            liberar.countDown();
            org.mockito.Mockito.reset(alvoAudit, alvo);
        }
        assertThat(contar("linha_contingencia")).isEqualTo(2);
        assertThat(contar("revisao_contagem")).isEqualTo(1);
    }

    private Map<String, Object> leituraContagem(JsonNode u, String q, Instant observado) {
        return Map.of(
                "codigoUnidade",
                u.get("codigo").asString(),
                "versaoUnidade",
                u.get("versao").longValue(),
                "contado",
                q,
                "observadoEm",
                observado.toString(),
                "motivo",
                "Leitura física comprovada");
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(booleans = {true, false})
    void contagemDiretaEContingenciaComAlvoAninhadoSerializamPedidoAntesContexto(
            boolean diretaPrimeiro) throws Exception {
        var u = unitizar(receber("100", "0"), "100", "BOA");
        posicionar(u, endereco("A01", "ARMAZENAGEM"));
        u = atual(u);
        var instante = Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MICROS);
        var dados = new HashMap<>(leituraContagem(u, "100", instante));
        dados.put("operacaoId", UUID.randomUUID());
        var l =
                resposta(
                        post(
                                "/api/v1/contingencias",
                                linha(
                                        "CONTAGEM-SEM-RAIZ",
                                        "CONTAGEM",
                                        instante,
                                        List.of(),
                                        false,
                                        Map.of("dados", leituraContagem(u, "100", instante))),
                                supervisor),
                        200);
        var comando = conciliar(l);
        var primeiraSegura = new CountDownLatch(1);
        var segundaTentou = new CountDownLatch(1);
        var liberar = new CountDownLatch(1);
        br.com.rodogarcia.wms.services.PedidoEntradaService alvoPedido =
                org.springframework.test.util.AopTestUtils.getUltimateTargetObject(pedidosEntrada);
        br.com.rodogarcia.wms.services.ContagemEstoqueService alvoContagem =
                org.springframework.test.util.AopTestUtils.getUltimateTargetObject(contagens);
        br.com.rodogarcia.wms.services.ContingenciaService alvoContingencia =
                org.springframework.test.util.AopTestUtils.getUltimateTargetObject(contingencia);
        org.mockito.Mockito.doAnswer(
                        inv -> {
                            if (!diretaPrimeiro) segundaTentou.countDown();
                            var p = inv.callRealMethod();
                            if (diretaPrimeiro) {
                                primeiraSegura.countDown();
                                if (!liberar.await(10, TimeUnit.SECONDS))
                                    throw new IllegalStateException("Barreira pedido");
                            }
                            return p;
                        })
                .when(alvoPedido)
                .bloquear(org.mockito.ArgumentMatchers.any());
        org.mockito.Mockito.doAnswer(
                        inv -> {
                            if (!diretaPrimeiro && primeiraSegura.getCount() > 0) {
                                primeiraSegura.countDown();
                                if (!liberar.await(10, TimeUnit.SECONDS))
                                    throw new IllegalStateException("Barreira comando");
                            }
                            return inv.callRealMethod();
                        })
                .when(alvoContagem)
                .contar(org.mockito.ArgumentMatchers.any());
        org.mockito.Mockito.doAnswer(
                        inv -> {
                            if (diretaPrimeiro) segundaTentou.countDown();
                            return inv.callRealMethod();
                        })
                .when(alvoContingencia)
                .conciliar(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        Callable<HttpResponse<String>> direta = () -> post("/api/v1/contagens", dados, supervisor);
        Callable<HttpResponse<String>> conciliacao =
                () ->
                        post(
                                "/api/v1/contingencias/" + l.get("id").longValue() + "/conciliar",
                                comando,
                                supervisor);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var primeira = executor.submit(diretaPrimeiro ? direta : conciliacao);
            assertThat(primeiraSegura.await(10, TimeUnit.SECONDS)).isTrue();
            var segunda = executor.submit(diretaPrimeiro ? conciliacao : direta);
            assertThat(segundaTentou.await(10, TimeUnit.SECONDS)).isTrue();
            org.assertj.core.api.Assertions.assertThatThrownBy(
                            () -> segunda.get(150, TimeUnit.MILLISECONDS))
                    .isInstanceOf(java.util.concurrent.TimeoutException.class);
            liberar.countDown();
            resposta(primeira.get(15, TimeUnit.SECONDS), 200);
            resposta(segunda.get(15, TimeUnit.SECONDS), 200);
        } finally {
            liberar.countDown();
            org.mockito.Mockito.reset(alvoPedido, alvoContagem, alvoContingencia);
        }
        var repetida =
                resposta(
                        post(
                                "/api/v1/contingencias/" + l.get("id").longValue() + "/conciliar",
                                comando,
                                supervisor),
                        200);
        assertThat(repetida.get("situacao").asString()).isEqualTo("CONCILIADA");
        resposta(
                post(
                        "/api/v1/contingencias/" + l.get("id").longValue() + "/conciliar",
                        comando,
                        operador),
                403);
        assertThat(contar("contagem_estoque")).isEqualTo(1);
        assertThat(contar("revisao_contagem")).isEqualTo(2);
        assertSaldo("fisicoUnitizado", "100");
        assertSaldo("disponivel", "100");
        assertSaldo("reservado", "0");
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(
            strings = {"codigoUnidade", "pedidoId", "remanejamento"})
    void referenciasDivergentesNaoEscolhemOutroPedidoNemProduzemEfeito(String variante)
            throws Exception {
        var u = unitizar(receber("100", "0"), "100", "BOA");
        posicionar(u, endereco("A01", "ARMAZENAGEM"));
        u = atual(u);
        var outra = unitizar(receber("50", "0"), "50", "BOA");
        var instante = Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MICROS);
        var raiz = new HashMap<String, Object>();
        raiz.put("dados", leituraContagem(u, "70", instante));
        String tipo = "CONTAGEM";
        if ("remanejamento".equals(variante)) {
            tipo = "REMANEJAMENTO";
            raiz.put("codigoUnidade", u.get("codigo").asString());
            raiz.put("dados", Map.of("codigoUnidade", outra.get("codigo").asString()));
        } else if ("codigoUnidade".equals(variante)) {
            raiz.put("codigoUnidade", outra.get("codigo").asString());
        } else {
            raiz.put("pedidoId", outra.get("pedidoId").longValue());
        }
        var l =
                resposta(
                        post(
                                "/api/v1/contingencias",
                                linha("ALVO-DIVERGENTE", tipo, instante, List.of(), false, raiz),
                                supervisor),
                        200);
        var comando = conciliar(l);
        var erro =
                resposta(
                        post(
                                "/api/v1/contingencias/" + l.get("id").longValue() + "/conciliar",
                                comando,
                                supervisor),
                        400);
        assertThat(erro.get("codigo").asString()).isEqualTo("DADOS_INVALIDOS");
        var atual =
                resposta(get("/api/v1/contingencias/" + l.get("id").longValue(), supervisor), 200);
        assertThat(atual.get("situacao").asString()).isEqualTo("PENDENTE");
        assertThat(atual.get("versao").longValue()).isEqualTo(l.get("versao").longValue());
        assertThat(atual.get("conteudoHash").asString())
                .isEqualTo(l.get("conteudoHash").asString());
        assertThat(contar("revisao_contagem")).isZero();
        assertThat(contar("operacao_administrativa")).isEqualTo(1);
        assertThat(atual(u).get("quantidade").decimalValue()).isEqualByComparingTo("100");
        assertThat(atual(outra).get("quantidade").decimalValue()).isEqualByComparingTo("50");
    }

    @Test
    void falhaAtomicaEGravacaoDePendenciaMantemAlvoAninhadoSemConsumirConfirmacao()
            throws Exception {
        var u = unitizar(receber("100", "0"), "100", "BOA");
        posicionar(u, endereco("A01", "ARMAZENAGEM"));
        u = atual(u);
        var instante = Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MICROS);
        var l =
                resposta(
                        post(
                                "/api/v1/contingencias",
                                linha(
                                        "PENDENCIA-ALVO-ANINHADO",
                                        "CONTAGEM",
                                        instante,
                                        List.of(),
                                        false,
                                        Map.of("dados", leituraContagem(u, "70", instante))),
                                supervisor),
                        200);
        var comando = new HashMap<>(conciliar(l));
        br.com.rodogarcia.wms.services.AuditoriaService alvo =
                org.springframework.test.util.AopTestUtils.getUltimateTargetObject(auditoria);
        org.mockito.Mockito.doAnswer(
                        inv -> {
                            if ("LEITURA_CONTAGEM".equals(inv.getArgument(2)))
                                throw br.com.rodogarcia.wms.exceptions.RegraNegocioException
                                        .conflito("FALHA_AUDITORIA_TESTE", "Falha fictícia tardia");
                            return inv.callRealMethod();
                        })
                .when(alvo)
                .registrar(
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any());
        JsonNode pendente;
        try {
            pendente =
                    resposta(
                            post(
                                    "/api/v1/contingencias/"
                                            + l.get("id").longValue()
                                            + "/conciliar",
                                    comando,
                                    supervisor),
                            200);
        } finally {
            org.mockito.Mockito.reset(alvo);
        }
        assertThat(pendente.get("pendencia").asString()).isEqualTo("FALHA_AUDITORIA_TESTE");
        assertThat(contar("revisao_contagem")).isZero();
        assertThat(contar("operacao_administrativa")).isEqualTo(1);
        assertSaldo("disponivel", "100");
        comando.put("versao", pendente.get("versao").longValue());
        var r =
                resposta(
                        post(
                                "/api/v1/contingencias/" + l.get("id").longValue() + "/conciliar",
                                comando,
                                supervisor),
                        200);
        assertThat(r.get("situacao").asString()).isEqualTo("CONCILIADA");
        assertThat(r.get("conteudoHash").asString()).isEqualTo(l.get("conteudoHash").asString());
        assertThat(
                        resposta(
                                post(
                                        "/api/v1/contingencias/"
                                                + l.get("id").longValue()
                                                + "/conciliar",
                                        comando,
                                        supervisor),
                                200))
                .isEqualTo(r);
        assertThat(contar("revisao_contagem")).isEqualTo(1);
        assertSaldo("bloqueado", "100");
        assertSaldo("fisicoUnitizado", "100");
    }

    @Test
    void fatoServicoUsaUnidadeIdAninhadaERecusaRaizOuContextoDivergentes() throws Exception {
        var u = unitizar(receber("100", "0"), "100", "BOA");
        posicionar(u, endereco("A01", "ARMAZENAGEM"));
        u = atual(u);
        var outra = unitizar(receber("50", "0"), "50", "BOA");
        var servico =
                resposta(
                        post(
                                "/api/v1/servicos-cobranca",
                                Map.of(
                                        "codigo",
                                        "ADICIONAL",
                                        "descricao",
                                        "Serviço fictício",
                                        "tipo",
                                        "ADICIONAL",
                                        "unidade",
                                        "QUANTIDADE_PRODUTO",
                                        "motivo",
                                        "Configuração fictícia comprovada",
                                        "operacaoId",
                                        UUID.randomUUID()),
                                gestor),
                        200);
        var instante = Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MICROS);
        var dados = new HashMap<String, Object>();
        dados.put("clienteId", cliente.getId());
        dados.put("armazemId", armazem.getId());
        dados.put("servicoId", servico.get("id").longValue());
        dados.put("origem", "MANUAL");
        dados.put("unidadeId", u.get("id").longValue());
        dados.put("pedidoEntradaId", u.get("pedidoId").longValue());
        dados.put("referenciaExecucao", "EXECUCAO-ANINHADA");
        dados.put("executadoEm", instante.toString());
        dados.put("quantidade", "1");
        dados.put("categoria", "PALLET");
        dados.put("cotas", List.of());
        dados.put("criterioRateio", "Origem comprovada da unidade");
        dados.put("motivo", "Execução adicional fictícia comprovada");
        var divergente =
                resposta(
                        post(
                                "/api/v1/contingencias",
                                linha(
                                        "FATO-RAIZ-DIVERGENTE",
                                        "FATO_SERVICO",
                                        instante,
                                        List.of(),
                                        false,
                                        Map.of(
                                                "codigoUnidade",
                                                outra.get("codigo").asString(),
                                                "dados",
                                                dados)),
                                supervisor),
                        200);
        resposta(
                post(
                        "/api/v1/contingencias/" + divergente.get("id").longValue() + "/conciliar",
                        conciliar(divergente),
                        supervisor),
                400);
        assertThat(contar("fato_servico")).isZero();
        var pedidoDivergente =
                resposta(
                        post(
                                "/api/v1/contingencias",
                                linha(
                                        "FATO-PEDIDO-DIVERGENTE",
                                        "FATO_SERVICO",
                                        instante,
                                        List.of(),
                                        false,
                                        Map.of(
                                                "pedidoId",
                                                outra.get("pedidoId").longValue(),
                                                "dados",
                                                dados)),
                                supervisor),
                        200);
        resposta(
                post(
                        "/api/v1/contingencias/"
                                + pedidoDivergente.get("id").longValue()
                                + "/conciliar",
                        conciliar(pedidoDivergente),
                        supervisor),
                400);
        var dadosOutroContexto = new HashMap<>(dados);
        dadosOutroContexto.put("clienteId", cliente.getId() + 10000);
        var outroContexto =
                resposta(
                        post(
                                "/api/v1/contingencias",
                                linha(
                                        "FATO-CONTEXTO-DIVERGENTE",
                                        "FATO_SERVICO",
                                        instante,
                                        List.of(),
                                        false,
                                        Map.of("dados", dadosOutroContexto)),
                                supervisor),
                        200);
        resposta(
                post(
                        "/api/v1/contingencias/"
                                + outroContexto.get("id").longValue()
                                + "/conciliar",
                        conciliar(outroContexto),
                        supervisor),
                409);
        assertThat(contar("fato_servico")).isZero();
        var l =
                resposta(
                        post(
                                "/api/v1/contingencias",
                                linha(
                                        "FATO-SEM-RAIZ",
                                        "FATO_SERVICO",
                                        instante,
                                        List.of(),
                                        false,
                                        Map.of("dados", dados)),
                                supervisor),
                        200);
        var comando = conciliar(l);
        var r =
                resposta(
                        post(
                                "/api/v1/contingencias/" + l.get("id").longValue() + "/conciliar",
                                comando,
                                supervisor),
                        200);
        assertThat(r.get("situacao").asString()).isEqualTo("CONCILIADA");
        assertThat(r.get("resultado").get("efeito").get("unidadeId").longValue())
                .isEqualTo(u.get("id").longValue());
        resposta(
                post(
                        "/api/v1/contingencias/" + l.get("id").longValue() + "/conciliar",
                        comando,
                        operador),
                403);
        resposta(
                post(
                        "/api/v1/contingencias/" + l.get("id").longValue() + "/conciliar",
                        comando,
                        supervisor),
                200);
        assertThat(contar("fato_servico")).isEqualTo(1);
        assertThat(
                        resposta(
                                        get(
                                                "/api/v1/contingencias/"
                                                        + divergente.get("id").longValue(),
                                                supervisor),
                                        200)
                                .get("situacao")
                                .asString())
                .isEqualTo("PENDENTE");
    }

    private Map<String, Object> linha(
            String identidade,
            String tipo,
            Instant instante,
            List<String> deps,
            boolean registrado,
            Map<String, Object> dados) {
        var d = new HashMap<String, Object>();
        d.put("operacaoId", UUID.randomUUID());
        d.put("identidadeFato", identidade);
        d.put("clienteId", cliente.getId());
        d.put("armazemId", armazem.getId());
        d.put("tipo", tipo);
        d.put("ocorridaEm", instante.toString());
        d.put("operador", "Operador fictício");
        d.put("fonte", "Relatório físico fictício comprovado");
        d.put("efeitoRegistradoNoWms", registrado);
        d.put("dependencias", deps);
        d.put("dados", dados);
        d.put("motivo", "Conciliação identificada fictícia");
        return d;
    }

    private Map<String, Object> conciliar(JsonNode l) {
        return Map.of(
                "operacaoId",
                UUID.randomUUID(),
                "versao",
                l.get("versao").longValue(),
                "modo",
                "EXECUTAR",
                "motivo",
                "Conciliação física comprovada");
    }

    @Test
    void leituraAnteriorOuMesmoInstanteConflitanteNaoLiberaSaldo() throws Exception {
        var u = unitizar(receber("100", "0"), "100", "BOA");
        posicionar(u, endereco("A01", "ARMAZENAGEM"));
        u = atual(u);
        var instante = Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MICROS);
        var d = new HashMap<String, Object>();
        d.put("operacaoId", UUID.randomUUID());
        d.put("codigoUnidade", u.get("codigo").asString());
        d.put("versaoUnidade", u.get("versao").longValue());
        d.put("contado", "70");
        d.put("observadoEm", instante.toString());
        d.put("motivo", "Leitura vigente identificada");
        var primeira = resposta(post("/api/v1/contagens", d, operador), 200);
        var antiga = new HashMap<>(d);
        antiga.put("operacaoId", UUID.randomUUID());
        antiga.put("contado", "100");
        antiga.put("observadoEm", instante.minusNanos(1000).toString());
        assertThat(
                        resposta(post("/api/v1/contagens", antiga, operador), 409)
                                .get("codigo")
                                .asString())
                .isEqualTo("CONTAGEM_TEMPORAL");
        antiga.put("observadoEm", instante.toString());
        resposta(post("/api/v1/contagens", antiga, operador), 409);
        assertThat(contar("revisao_contagem")).isEqualTo(1);
        var vigente =
                resposta(get("/api/v1/contagens/" + primeira.get("id").longValue(), operador), 200);
        assertThat(vigente.get("revisao").intValue()).isEqualTo(1);
        assertThat(vigente.get("observadoEm").asString())
                .isEqualTo(primeira.get("observadoEm").asString());
        assertThat(vigente.get("contado").decimalValue()).isEqualByComparingTo("70");
        assertThat(vigente.get("impedimento").booleanValue()).isTrue();
        assertThat(saldo().get("disponivel").decimalValue()).isZero();
        assertThat(saldo().get("fisicoTotal").decimalValue()).isEqualByComparingTo("100");
        var novo = contar(atual(u), "100");
        assertThat(novo.get("impedimento").booleanValue()).isFalse();
        assertThat(saldo().get("disponivel").decimalValue()).isEqualByComparingTo("100");
    }

    @Test
    void contagem100Reserva80Observado70PreservaIntegralAteCancelar() throws Exception {
        var u = unitizar(receber("100", "0"), "100", "BOA");
        posicionar(u, endereco("A01", "ARMAZENAGEM"));
        u = atual(u);
        var criado =
                resposta(
                        post(
                                "/api/v1/pedidos-saida",
                                Map.of(
                                        "operacaoId",
                                        UUID.randomUUID(),
                                        "clienteId",
                                        cliente.getId(),
                                        "armazemId",
                                        armazem.getId(),
                                        "referencia",
                                        "SAIDA80",
                                        "motivo",
                                        "Pedido integral ficticio",
                                        "itens",
                                        List.of(
                                                Map.of(
                                                        "produtoId",
                                                        produto.getId(),
                                                        "quantidade",
                                                        80))),
                                operador),
                        201);
        long pedido = criado.get("pedido").get("id").longValue();
        resposta(
                post(
                        "/api/v1/pedidos-saida/" + pedido + "/reserva",
                        Map.of(
                                "operacaoId",
                                UUID.randomUUID(),
                                "versao",
                                criado.get("pedido").get("versao").longValue(),
                                "motivo",
                                "Reserva integral ficticia"),
                        operador),
                200);
        u = atual(u);
        var contagem = contar(u, "70");
        assertThat(contagem.get("situacao").asString()).isEqualTo("PENDENTE_RESERVA");
        assertThat(contagem.get("reservado").decimalValue()).isEqualByComparingTo("80");
        assertThat(saldo().get("fisicoTotal").decimalValue()).isEqualByComparingTo("100");
        assertThat(saldo().get("disponivel").decimalValue()).isZero();
        var aplicar = aplicar(contagem, atual(u));
        resposta(
                post(
                        "/api/v1/contagens/" + contagem.get("id").longValue() + "/aplicar",
                        aplicar,
                        supervisor),
                409);
        assertThat(contar("fato_permanencia")).isZero();
        var p = resposta(get("/api/v1/pedidos-saida/" + pedido, operador), 200);
        resposta(
                post(
                        "/api/v1/pedidos-saida/" + pedido + "/cancelamento",
                        Map.of(
                                "operacaoId",
                                UUID.randomUUID(),
                                "versao",
                                p.get("versao").longValue(),
                                "motivo",
                                "Cancelamento integral para conciliacao"),
                        supervisor),
                200);
        var resultado =
                resposta(
                        post(
                                "/api/v1/contagens/" + contagem.get("id").longValue() + "/aplicar",
                                aplicar(contagem, atual(u)),
                                supervisor),
                        200);
        assertThat(resultado.get("situacao").asString()).isEqualTo("APLICADA");
        assertThat(saldo().get("fisicoTotal").decimalValue()).isEqualByComparingTo("70");
        assertThat(saldo().get("reservado").decimalValue()).isZero();
        assertThat(
                        jdbc.queryForObject(
                                "select quantidade_prevista from wms.item_nota_entrada",
                                BigDecimal.class))
                .isEqualByComparingTo("100");
    }

    @Test
    void recontagemInvalidaDiferencaAntigaEPreservaBloqueioIndependente() throws Exception {
        var u = unitizar(receber("100", "0"), "100", "BOA");
        posicionar(u, endereco("A01", "ARMAZENAGEM"));
        u = atual(u);
        resposta(
                post(UNIDADES + u.get("codigo").asString() + "/bloqueio", bloqueio(u), supervisor),
                200);
        u = atual(u);
        var primeira = contar(u, "70");
        var segunda = contar(atual(u), "100");
        assertThat(segunda.get("impedimento").booleanValue()).isFalse();
        assertThat(atual(u).get("quantidade").decimalValue()).isEqualByComparingTo("100");
        assertThat(
                        resposta(
                                        get(
                                                UNIDADES + u.get("codigo").asString() + "/estoque",
                                                operador),
                                        200)
                                .get("bloqueada")
                                .booleanValue())
                .isTrue();
        resposta(
                post(
                        "/api/v1/contagens/" + primeira.get("id").longValue() + "/aplicar",
                        aplicar(primeira, atual(u)),
                        supervisor),
                409);
        assertThat(
                        jdbc.queryForObject(
                                "select situacao from wms.revisao_contagem where numero=1",
                                String.class))
                .isEqualTo("SUBSTITUIDA");
    }

    @Test
    void ajusteAuditoriaFalhaReverteConteudoFatoMovimentoEReplay() throws Exception {
        var u = unitizar(receber("100", "0"), "100", "BOA");
        var c = contar(u, "70");
        var d = aplicar(c, atual(u));
        var alvo =
                org.springframework.test.util.AopTestUtils
                        .<br.com.rodogarcia.wms.services.AuditoriaService>getUltimateTargetObject(
                                auditoria);
        org.mockito.Mockito.doAnswer(
                        inv -> {
                            if ("APLICACAO_CONTAGEM".equals(inv.getArgument(2)))
                                throw new IllegalStateException("Falha de auditoria ficticia");
                            return inv.callRealMethod();
                        })
                .when(alvo)
                .registrar(
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any());
        resposta(
                post("/api/v1/contagens/" + c.get("id").longValue() + "/aplicar", d, supervisor),
                500);
        assertThat(atual(u).get("quantidade").decimalValue()).isEqualByComparingTo("100");
        assertThat(contar("fato_permanencia")).isZero();
        assertThat(contar("movimento_estoque")).isZero();
        org.mockito.Mockito.reset(alvo);
        var r =
                resposta(
                        post(
                                "/api/v1/contagens/" + c.get("id").longValue() + "/aplicar",
                                d,
                                supervisor),
                        200);
        assertThat(
                        resposta(
                                post(
                                        "/api/v1/contagens/" + c.get("id").longValue() + "/aplicar",
                                        d,
                                        supervisor),
                                200))
                .isEqualTo(r);
        resposta(
                post("/api/v1/contagens/" + c.get("id").longValue() + "/aplicar", d, operador),
                403);
        assertThat(contar("fato_permanencia")).isEqualTo(1);
    }

    @Test
    void preparaCargaUmaVezMasSoConfirmaTodasAsEtiquetas() throws Exception {
        var c = carga();
        var d30AntesLeitura = D30FotografiaFisica.capturar(jdbc);
        assertThat(
                        resposta(
                                        get(
                                                "/api/v1/cargas-iniciais/"
                                                        + c.get("id").longValue(),
                                                supervisor),
                                        200)
                                .get("id"))
                .isEqualTo(c.get("id"));
        var d30Revisoes =
                resposta(
                        get(
                                "/api/v1/cargas-iniciais/" + c.get("id").longValue() + "/revisoes",
                                supervisor),
                        200);
        assertThat(d30Revisoes.get("totalItens").longValue()).isEqualTo(1);
        assertThat(d30Revisoes.get("itens").get(0).get("numero").intValue()).isEqualTo(1);
        assertThat(D30FotografiaFisica.capturar(jdbc)).isEqualTo(d30AntesLeitura);
        var d = conferencia(c, List.of());
        var d30AntesHash = D30FotografiaFisica.capturar(jdbc);
        for (String hash : java.util.Arrays.asList(null, "Z".repeat(63), "Z".repeat(64))) {
            var divergente = new HashMap<>(d);
            divergente.put("conteudoHash", hash);
            var recusada =
                    resposta(
                            post(
                                    "/api/v1/cargas-iniciais/"
                                            + c.get("id").longValue()
                                            + "/preparar",
                                    divergente,
                                    supervisor),
                            hash != null && hash.length() == 64 ? 409 : 400);
            if (hash != null && hash.length() == 64)
                assertThat(recusada.get("codigo").asString()).isEqualTo("REVISAO_DESATUALIZADA");
            assertThat(D30FotografiaFisica.capturar(jdbc)).isEqualTo(d30AntesHash);
        }
        var preparada =
                resposta(
                        post(
                                "/api/v1/cargas-iniciais/" + c.get("id").longValue() + "/preparar",
                                d,
                                supervisor),
                        200);
        assertThat(preparada.get("situacao").asString()).isEqualTo("PREPARADA");
        assertThat(preparada.get("etiquetas").size()).isEqualTo(2);
        assertThat(
                        resposta(
                                post(
                                        "/api/v1/cargas-iniciais/"
                                                + c.get("id").longValue()
                                                + "/preparar",
                                        d,
                                        supervisor),
                                200))
                .isEqualTo(preparada);
        assertThat(contar("entrada_conferida")).isEqualTo(1);
        assertThat(contar("unidade_logistica")).isEqualTo(2);
        assertThat(saldo().get("fisicoTotal").decimalValue()).isEqualByComparingTo("20");
        var etiquetas = new java.util.ArrayList<String>();
        for (var e : preparada.get("etiquetas")) etiquetas.add(e.asString());
        for (int i = 0; i < etiquetas.size(); i++) {
            var u = resposta(get(UNIDADES + etiquetas.get(i), operador), 200).get("unidade");
            posicionar(u, endereco("A0" + (i + 1), "ARMAZENAGEM"));
        }
        assertThat(saldo().get("disponivel").decimalValue()).isZero();
        resposta(
                post(
                        "/api/v1/cargas-iniciais/" + c.get("id").longValue() + "/confirmar",
                        conferencia(preparada, List.of(etiquetas.getFirst())),
                        supervisor),
                409);
        var confirmado =
                resposta(
                        post(
                                "/api/v1/cargas-iniciais/" + c.get("id").longValue() + "/confirmar",
                                conferencia(preparada, etiquetas),
                                supervisor),
                        200);
        assertThat(confirmado.get("situacao").asString()).isEqualTo("REGULARIZADA");
        assertThat(d30Decoder.decode(supervisor).getSubject()).isEqualTo("estoquista-teste");
        var atoresD30 =
                jdbc.queryForList(
                        "select acao,usuario,tipo,registro_id from wms.auditoria_cadastro where acao in ('PREPARACAO_CARGA','CONFIRMACAO_CARGA') order by id");
        assertThat(atoresD30).hasSize(2);
        assertThat(atoresD30)
                .extracting(linha -> linha.get("ACAO"))
                .containsExactly("PREPARACAO_CARGA", "CONFIRMACAO_CARGA");
        for (var linha : atoresD30) {
            assertThat(linha.get("USUARIO")).isEqualTo("estoquista-teste");
            assertThat(linha.get("TIPO")).isEqualTo("CARGA_INICIAL");
            assertThat(((Number) linha.get("REGISTRO_ID")).longValue())
                    .isEqualTo(c.get("id").longValue());
        }
        assertThat(saldo().get("disponivel").decimalValue()).isEqualByComparingTo("20");
        var indicador =
                resposta(
                                get(
                                        "/api/v1/indicadores-estoque?clienteId="
                                                + cliente.getId()
                                                + "&armazemId="
                                                + armazem.getId()
                                                + "&valor=true&fuso=UTC",
                                        supervisor),
                                200)
                        .get("itens")
                        .get(0);
        assertThat(indicador.get("valorExato").isNull()).isTrue();
        assertThat(indicador.get("saldo").get("fisicoTotal").decimalValue())
                .isEqualByComparingTo("20");
    }

    @Test
    void cancelarPreparadaExigeResolucaoEfetivaPreservaEntradaE403Replay() throws Exception {
        var c = carga();
        var preparada =
                resposta(
                        post(
                                "/api/v1/cargas-iniciais/" + c.get("id").longValue() + "/preparar",
                                conferencia(c, List.of()),
                                supervisor),
                        200);
        var ids = new java.util.ArrayList<Long>();
        var pedido = new HashMap<String, Object>();
        pedido.put("operacaoId", UUID.randomUUID());
        pedido.put("versao", preparada.get("versao").longValue());
        pedido.put("contagensIds", ids);
        pedido.put("motivo", "Resolucao fisica integral comprovada");
        resposta(
                post(
                        "/api/v1/cargas-iniciais/"
                                + c.get("id").longValue()
                                + "/resolver-cancelamento",
                        pedido,
                        gestor),
                409);
        for (var e : preparada.get("etiquetas")) {
            var u = resposta(get(UNIDADES + e.asString(), operador), 200).get("unidade");
            var leitura = contar(u, "0");
            resposta(
                    post(
                            "/api/v1/contagens/" + leitura.get("id").longValue() + "/aplicar",
                            aplicar(leitura, atual(u)),
                            supervisor),
                    200);
            ids.add(leitura.get("id").longValue());
        }
        assertThat(ids).hasSize(2);
        var d30AntesResolucao = D30FotografiaFisica.capturar(jdbc);
        for (var selecao :
                List.of(
                        java.util.Arrays.asList(ids.getFirst(), null),
                        List.of(ids.getFirst()),
                        List.of(ids.getFirst(), ids.getFirst()),
                        List.of(0L, ids.getLast()),
                        List.of(-1L, ids.getLast()))) {
            var divergente = new HashMap<>(pedido);
            divergente.put("contagensIds", selecao);
            boolean validacao =
                    selecao.size() == 2
                            && selecao.getFirst() != null
                            && (selecao.getFirst() <= 0
                                    || selecao.getFirst().equals(selecao.getLast()));
            var recusa =
                    resposta(
                            post(
                                    "/api/v1/cargas-iniciais/"
                                            + c.get("id").longValue()
                                            + "/resolver-cancelamento",
                                    divergente,
                                    gestor),
                            validacao ? 400 : 409);
            if (!validacao)
                assertThat(recusa.get("codigo").asString())
                        .isEqualTo("RESOLUCAO_ESTAGIO_INCOMPLETA");
            assertThat(D30FotografiaFisica.capturar(jdbc)).isEqualTo(d30AntesResolucao);
        }
        var r =
                resposta(
                        post(
                                "/api/v1/cargas-iniciais/"
                                        + c.get("id").longValue()
                                        + "/resolver-cancelamento",
                                pedido,
                                gestor),
                        200);
        assertThat(r.get("situacao").asString()).isEqualTo("CANCELADA");
        assertThat(r.get("entradaId")).isEqualTo(preparada.get("entradaId"));
        assertThat(contar("entrada_conferida")).isEqualTo(1);
        assertThat(
                        resposta(
                                post(
                                        "/api/v1/cargas-iniciais/"
                                                + c.get("id").longValue()
                                                + "/resolver-cancelamento",
                                        pedido,
                                        gestor),
                                200))
                .isEqualTo(r);
        resposta(
                post(
                        "/api/v1/cargas-iniciais/"
                                + c.get("id").longValue()
                                + "/resolver-cancelamento",
                        pedido,
                        supervisor),
                403);
    }

    @Test
    void cargaSemOrigemNaoCriaEstoqueECancelamentoPendenteSemMovimento() throws Exception {
        var c =
                resposta(
                        post(
                                "/api/v1/cargas-iniciais",
                                Map.of(
                                        "operacaoId",
                                        UUID.randomUUID(),
                                        "clienteId",
                                        cliente.getId(),
                                        "armazemId",
                                        armazem.getId(),
                                        "produtoId",
                                        produto.getId(),
                                        "referencia",
                                        "LEGADA",
                                        "quantidade",
                                        20,
                                        "dados",
                                        Map.of(),
                                        "motivo",
                                        "Carga somente fornecida incompleta"),
                                supervisor),
                        200);
        assertThat(c.get("pendencias").toString()).contains("ORIGEM_DESCONHECIDA");
        assertThat(contar("pedido_entrada")).isZero();
        assertThat(saldo().get("fisicoTotal").decimalValue()).isZero();
        resposta(
                post(
                        "/api/v1/cargas-iniciais/" + c.get("id").longValue() + "/preparar",
                        conferencia(c, List.of()),
                        supervisor),
                409);
        var d =
                Map.of(
                        "operacaoId",
                        UUID.randomUUID(),
                        "versao",
                        c.get("versao").longValue(),
                        "motivo",
                        "Cancelamento somente do estagio");
        var r =
                resposta(
                        post(
                                "/api/v1/cargas-iniciais/" + c.get("id").longValue() + "/cancelar",
                                d,
                                gestor),
                        200);
        assertThat(r.get("situacao").asString()).isEqualTo("CANCELADA");
        assertThat(contar("movimento_estoque")).isZero();
    }

    @Test
    void inativacaoDefinitivaRevalidaFisicoERejeitaReplaySemGestor() throws Exception {
        var solicitado =
                resposta(
                        post(
                                "/api/v1/encerramentos/CLIENTE/" + cliente.getId() + "/solicitar",
                                Map.of(
                                        "operacaoId",
                                        UUID.randomUUID(),
                                        "versao",
                                        0,
                                        "motivo",
                                        "Encerramento identificado sem compromissos"),
                                gestor),
                        200);
        var d =
                Map.of(
                        "operacaoId",
                        UUID.randomUUID(),
                        "versao",
                        solicitado.get("versao").longValue(),
                        "motivo",
                        "Inativacao revalidada integralmente");
        var r =
                resposta(
                        post(
                                "/api/v1/encerramentos/CLIENTE/" + cliente.getId() + "/inativar",
                                d,
                                gestor),
                        200);
        assertThat(r.get("situacao").asString()).isEqualTo("INATIVO");
        assertThat(
                        resposta(
                                post(
                                        "/api/v1/encerramentos/CLIENTE/"
                                                + cliente.getId()
                                                + "/inativar",
                                        d,
                                        gestor),
                                200))
                .isEqualTo(r);
        resposta(
                post("/api/v1/encerramentos/CLIENTE/" + cliente.getId() + "/inativar", d, operador),
                403);
        resposta(
                post(
                        "/api/v1/pedidos-entrada",
                        Map.of(
                                "clienteId",
                                cliente.getId(),
                                "armazemId",
                                armazem.getId(),
                                "referencia",
                                "NAO-ABRIR"),
                        gestor),
                409);
    }

    @Test
    void d27AjusteZeroRespeitaCheckSQLLocalizacaoSemApagarMarcoHistorico() throws Exception {
        jdbc.execute(
                "ALTER TABLE wms.unidade_logistica ADD CONSTRAINT d27_localizacao_sql CHECK ((tipo_localizacao IS NULL AND primeiro_enderecamento_em IS NULL AND peso_kg IS NULL AND conjunto_atual_id IS NULL) OR (tipo_localizacao IS NOT NULL AND tipo_localizacao IN ('ARMAZENAGEM','TRIAGEM','QUARENTENA','SEPARACAO') AND primeiro_enderecamento_em IS NOT NULL AND peso_kg IS NOT NULL AND ((posicoes_necessarias = 1 AND conjunto_atual_id IS NULL) OR (posicoes_necessarias = 2 AND conjunto_atual_id IS NOT NULL))))");
        try {
            d27AjusteZeroPreservaPosicaoDesassociaUnidadeEReplayNaoDuplicaFato();
        } finally {
            jdbc.execute("ALTER TABLE wms.unidade_logistica DROP CONSTRAINT d27_localizacao_sql");
        }
    }

    @Test
    void d27AjusteZeroPreservaPosicaoDesassociaUnidadeEReplayNaoDuplicaFato() throws Exception {
        var u = unitizar(receber("10", "0"), "10", "BOA");
        var e = endereco("D27-ZERO", "ARMAZENAGEM");
        posicionar(u, e);
        u = atual(u);
        var antes =
                resposta(get(UNIDADES + u.get("codigo").asString() + "/estoque", operador), 200);
        Long ocupacao =
                jdbc.queryForObject(
                        "select id from wms.ocupacao_endereco where unidade_id=?",
                        Long.class,
                        u.get("id").longValue());
        var c = contar(u, "0");
        var d = aplicar(c, atual(u));
        String rota = "/api/v1/contagens/" + c.get("id").longValue() + "/aplicar";
        var aplicado = resposta(post(rota, d, supervisor), 200);
        assertThat(aplicado.get("situacao").asString()).isEqualTo("APLICADA");
        assertThat(resposta(post(rota, d, supervisor), 200)).isEqualTo(aplicado);
        assertThat(
                        jdbc.queryForObject(
                                "select count(*) from wms.ocupacao_endereco where id=? and unidade_id is null",
                                Long.class,
                                ocupacao))
                .isEqualTo(1);
        assertThat(atual(u).get("ativa").booleanValue()).isFalse();
        var depois =
                resposta(get(UNIDADES + u.get("codigo").asString() + "/estoque", operador), 200);
        assertThat(depois.get("primeiroEnderecamentoEm"))
                .isEqualTo(antes.get("primeiroEnderecamentoEm"));
        assertThat(depois.get("inicioArmazenagemEm")).isEqualTo(antes.get("inicioArmazenagemEm"));
        assertThat(depois.get("tipoLocalizacao")).isEqualTo(antes.get("tipoLocalizacao"));
        assertThat(depois.get("posicoes").isEmpty()).isTrue();
        assertThat(saldo().get("fisicoTotal").decimalValue()).isEqualByComparingTo("0");
        assertThat(
                        jdbc.queryForObject(
                                "select count(*) from wms.fato_permanencia where tipo='AJUSTE_ESTOQUE'",
                                Long.class))
                .isEqualTo(1);
        assertThat(
                        jdbc.queryForObject(
                                "select count(*) from wms.movimento_estoque where acao='AJUSTE_ESTOQUE'",
                                Long.class))
                .isEqualTo(1);
    }

    @Test
    void d30FalhaTardiaDeAuditoriaNoEncerramentoDefinitivoReverteEPermiteRetry() throws Exception {
        String rota = "/api/v1/encerramentos/CLIENTE/" + cliente.getId();
        var solicitado =
                resposta(
                        post(
                                rota + "/solicitar",
                                Map.of(
                                        "operacaoId",
                                        UUID.randomUUID(),
                                        "versao",
                                        0,
                                        "motivo",
                                        "Encerramento identificado D30 sem compromissos"),
                                gestor),
                        200);
        var comando =
                Map.of(
                        "operacaoId",
                        UUID.randomUUID(),
                        "versao",
                        solicitado.get("versao").longValue(),
                        "motivo",
                        "Inativacao definitiva D30 com auditoria atomica");
        var antes = D30FotografiaFisica.capturar(jdbc);
        jdbc.execute(
                "alter table wms.auditoria_cadastro add constraint d30_falha_inativacao check (acao <> 'INATIVACAO_DEFINITIVA')");
        try {
            resposta(post(rota + "/inativar", comando, gestor), 409);
            assertThat(D30FotografiaFisica.capturar(jdbc)).isEqualTo(antes);
            assertThat(resposta(get(rota + "/impedimentos", gestor), 200)).isEqualTo(solicitado);
        } finally {
            jdbc.execute("alter table wms.auditoria_cadastro drop constraint d30_falha_inativacao");
        }
        var concluido = resposta(post(rota + "/inativar", comando, gestor), 200);
        assertThat(concluido.get("situacao").asString()).isEqualTo("INATIVO");
        assertThat(concluido.get("versao").longValue())
                .isEqualTo(solicitado.get("versao").longValue() + 1);
        assertThat(concluido.get("impedimentos").size()).isZero();
        var apos = D30FotografiaFisica.capturar(jdbc);
        assertThat(resposta(post(rota + "/inativar", comando, gestor), 200)).isEqualTo(concluido);
        assertThat(D30FotografiaFisica.capturar(jdbc)).isEqualTo(apos);
        assertThat(
                        jdbc.queryForObject(
                                "select count(*) from wms.auditoria_cadastro where acao='INATIVACAO_DEFINITIVA'",
                                Integer.class))
                .isEqualTo(1);
        assertThat(
                        jdbc.queryForObject(
                                "select count(*) from wms.operacao_administrativa where tipo='INATIVACAO_DEFINITIVA'",
                                Integer.class))
                .isEqualTo(1);
    }

    private JsonNode atual(JsonNode u) throws Exception {
        return resposta(get(UNIDADES + u.get("codigo").asString(), operador), 200).get("unidade");
    }

    @Test
    void d30EnvelopeDecimalNormalizadoConservaSnapshotExecucaoEReplay() throws Exception {
        var u = unitizar(receber("100", "0"), "100", "BOA");
        posicionar(u, endereco("D30-ESCALA", "ARMAZENAGEM"));
        u = atual(u);
        var instante = Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MICROS);
        var leitura = new HashMap<String, Object>(leituraContagem(u, "70", instante));
        leitura.put("contado", new BigDecimal("70.0"));
        var envelope =
                linha(
                        "D30-ESCALA-NUMERICA",
                        "CONTAGEM",
                        instante,
                        List.of(),
                        false,
                        Map.of("dados", leitura));
        var original = resposta(post("/api/v1/contingencias", envelope, supervisor), 200);
        var fotoRegistro = D30FotografiaFisica.capturar(jdbc);
        leitura.put("contado", new BigDecimal("70.000000"));
        assertThat(resposta(post("/api/v1/contingencias", envelope, supervisor), 200))
                .isEqualTo(original);
        assertThat(D30FotografiaFisica.capturar(jdbc)).isEqualTo(fotoRegistro);
        var comando = conciliar(original);
        var conciliado =
                resposta(
                        post(
                                "/api/v1/contingencias/"
                                        + original.get("id").longValue()
                                        + "/conciliar",
                                comando,
                                supervisor),
                        200);
        assertThat(conciliado.get("situacao").asString()).isEqualTo("CONCILIADA");
        assertThat(contar("contagem_estoque")).isEqualTo(1);
        assertThat(
                        jdbc.queryForObject(
                                "select contado from wms.revisao_contagem", BigDecimal.class))
                .isEqualByComparingTo("70");
        assertThat(
                        jdbc.queryForObject(
                                "select quantidade from wms.unidade_logistica where id=?",
                                BigDecimal.class,
                                u.get("id").longValue()))
                .isEqualByComparingTo("100");
        var depois = D30FotografiaFisica.capturar(jdbc);
        assertThat(
                        resposta(
                                post(
                                        "/api/v1/contingencias/"
                                                + original.get("id").longValue()
                                                + "/conciliar",
                                        comando,
                                        supervisor),
                                200))
                .isEqualTo(conciliado);
        assertThat(resposta(post("/api/v1/contingencias", envelope, supervisor), 200))
                .isEqualTo(original);
        assertThat(D30FotografiaFisica.capturar(jdbc)).isEqualTo(depois);
        leitura.put("contado", new BigDecimal("71.0"));
        resposta(post("/api/v1/contingencias", envelope, supervisor), 409);
        assertThat(D30FotografiaFisica.capturar(jdbc)).isEqualTo(depois);
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(
            strings = {"0", "1.123456", "9999999999999.999999"})
    void d30NumeroRealEmMapEObjetoConservaContagemExata(String literal) throws Exception {
        var plano =
                java.nio.file.Path.of(
                        "evidencias/d30-cedro-contingencia-numeros-v24-plano-antes.json");
        assertThat(
                        java.util.HexFormat.of()
                                .formatHex(
                                        java.security.MessageDigest.getInstance("SHA-256")
                                                .digest(java.nio.file.Files.readAllBytes(plano)))
                                .toUpperCase())
                .isEqualTo("6C3BD75F03DC96BA43DCB0A88239B2EEA9FB46BE1F8076F50A7D4B603BF34D68");
        produto =
                produtos.saveAndFlush(
                        new Produto(
                                cliente,
                                "D30-NUMERO",
                                "Medido ficticio",
                                "KG",
                                TipoQuantidade.MEDIDA,
                                6,
                                false,
                                false,
                                null,
                                Instant.now()));
        embalagem =
                embalagens.saveAndFlush(
                        new Embalagem(
                                produto,
                                "D30-NUMERO-DUN",
                                "DUN ficticio",
                                BigDecimal.ONE,
                                Instant.now()));
        String fisico = "9999999999999.999999";
        var u = unitizar(receber(fisico, "0"), fisico, "BOA");
        posicionar(u, endereco("D30-NUMERO", "ARMAZENAGEM"));
        u = atual(u);
        var instante = Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MICROS);
        var contado = new BigDecimal(literal);
        var leitura = new HashMap<String, Object>(leituraContagem(u, literal, instante));
        leitura.put("contado", contado); // numero JSON real, nao String decimal.
        var envelope =
                linha(
                        "D30-NUMERO-REAL",
                        "CONTAGEM",
                        instante,
                        List.of(),
                        false,
                        Map.of("dados", leitura));
        var observador =
                tools.jackson.databind.json.JsonMapper.builder()
                        .enable(
                                tools.jackson.databind.DeserializationFeature
                                        .USE_BIG_DECIMAL_FOR_FLOATS)
                        .build();
        var canonicoLeitura = new java.util.TreeMap<String, Object>(leitura);
        canonicoLeitura.put("contado", contado.stripTrailingZeros());
        canonicoLeitura.put("versaoUnidade", BigDecimal.valueOf(u.get("versao").longValue()));
        var canonico = new java.util.TreeMap<String, Object>();
        canonico.put("dados", canonicoLeitura);
        var conteudoEsperado =
                new br.com.rodogarcia.wms.dto.ContingenciaDto.Conteudo(canonico, List.of(), false);
        String hashEsperado =
                java.util.HexFormat.of()
                        .formatHex(
                                java.security.MessageDigest.getInstance("SHA-256")
                                        .digest(
                                                mapper.writeValueAsBytes(
                                                        List.of(
                                                                "FATO_CONTINGENCIA",
                                                                "D30-NUMERO-REAL",
                                                                List.of(
                                                                        cliente.getId(),
                                                                        armazem.getId(),
                                                                        br.com.rodogarcia.wms.models
                                                                                .TipoContingencia
                                                                                .CONTAGEM,
                                                                        instante,
                                                                        "Operador fictício",
                                                                        "Relatório físico fictício comprovado",
                                                                        conteudoEsperado)))));
        var respostaRegistro = post("/api/v1/contingencias", envelope, supervisor);
        assertThat(respostaRegistro.statusCode())
                .as("registro valido do Number " + literal)
                .isEqualTo(200);
        var original = observador.readTree(respostaRegistro.body());
        assertThat(original.get("resultado").isNull()).isTrue();
        java.nio.file.Files.writeString(
                java.nio.file.Path.of(System.getProperty("wms.test.evidencias.dir"))
                        .resolve(
                                "d30-cedro-contingencia-numero-red-observacao-"
                                        + UUID.randomUUID()
                                        + ".json"),
                observador.writeValueAsString(
                        Map.of(
                                "literalFixture",
                                literal,
                                "hashEsperadoIndependente",
                                hashEsperado,
                                "respostaRawFicticia",
                                respostaRegistro.body(),
                                "limite",
                                "Mapper/config/rota reais somente H2mem ficticio; observadorBigDecimal nao conserta entrada")),
                java.nio.file.StandardOpenOption.CREATE_NEW);
        var numeroConteudo = original.get("conteudo").get("dados").get("dados").get("contado");
        assertThat(numeroConteudo.decimalValue()).isEqualByComparingTo(contado);
        assertThat(original.get("conteudoHash").asString()).isEqualTo(hashEsperado);
        assertThat(numeroConteudo.isNumber()).isTrue();
        assertThat(numeroConteudo.decimalValue()).isEqualByComparingTo(contado);
        String consultaRota = "/api/v1/contingencias/" + original.get("id").longValue();
        var fotoRegistro = D30FotografiaFisica.capturar(jdbc);
        var consultaOriginal = get(consultaRota, supervisor);
        assertThat(consultaOriginal.statusCode()).isEqualTo(200);
        assertThat(observador.readTree(consultaOriginal.body())).isEqualTo(original);
        assertThat(D30FotografiaFisica.capturar(jdbc)).isEqualTo(fotoRegistro);
        for (int cardinalidade : List.of(0, 101)) {
            var invalido = new HashMap<String, Object>(envelope);
            var mapa = new java.util.LinkedHashMap<String, Object>();
            for (int i = 0; i < cardinalidade; i++) mapa.put("campo" + i, "ficticio");
            invalido.put("dados", mapa);
            var recusa = post("/api/v1/contingencias", invalido, supervisor);
            assertThat(recusa.statusCode()).isEqualTo(400);
            var problema = observador.readTree(recusa.body());
            assertThat(problema.get("codigo").asString()).isEqualTo("DADOS_INVALIDOS");
            var campos = new java.util.ArrayList<String>();
            for (var erro : problema.get("campos"))
                campos.add(erro.get("campo").asString() + "#" + erro.get("codigo").asString());
            assertThat(campos).contains("dados#Size");
            assertThat(problema.get("idOperacao").asString()).isNotBlank();
            assertThat(recusa.body()).doesNotContain("jdbc:", "SQLException");
            assertThat(D30FotografiaFisica.capturar(jdbc)).isEqualTo(fotoRegistro);
        }
        var mapaCem = new java.util.LinkedHashMap<String, Object>();
        mapaCem.put("dados", new HashMap<>(leitura));
        for (int i = 0; i < 99; i++) mapaCem.put("campo" + i, "ficticio");
        var cem = new HashMap<String, Object>(envelope);
        cem.put("operacaoId", UUID.randomUUID());
        cem.put("identidadeFato", "D30-MAP-CEM");
        cem.put("dados", mapaCem);
        var positivoCem = post("/api/v1/contingencias", cem, supervisor);
        assertThat(positivoCem.statusCode()).as("Map100 otherwise valido").isEqualTo(200);
        assertThat(observador.readTree(positivoCem.body()).get("conteudo").get("dados").size())
                .isEqualTo(100);
        assertThat(contar("contagem_estoque")).isZero();
        fotoRegistro = D30FotografiaFisica.capturar(jdbc);
        leitura.put("contado", contado.setScale(6));
        var replayRegistro = post("/api/v1/contingencias", envelope, supervisor);
        assertThat(replayRegistro.statusCode()).isEqualTo(200);
        assertThat(observador.readTree(replayRegistro.body())).isEqualTo(original);
        assertThat(D30FotografiaFisica.capturar(jdbc)).isEqualTo(fotoRegistro);
        var comando = conciliar(original);
        String rota = "/api/v1/contingencias/" + original.get("id").longValue() + "/conciliar";
        var comandoTipado =
                mapper.convertValue(
                        comando, br.com.rodogarcia.wms.dto.ContingenciaDto.Conciliar.class);
        String hashConciliacaoEsperado =
                java.util.HexFormat.of()
                        .formatHex(
                                java.security.MessageDigest.getInstance("SHA-256")
                                        .digest(
                                                mapper.writeValueAsBytes(
                                                        List.of(
                                                                "CONCILIACAO_CONTINGENCIA",
                                                                original.get("id").longValue(),
                                                                comandoTipado))));
        var respostaEfeito = post(rota, comando, supervisor);
        assertThat(respostaEfeito.statusCode())
                .as("efeito CONTAGEM do Number " + literal)
                .isEqualTo(200);
        var resultado = observador.readTree(respostaEfeito.body());
        assertThat(resultado.get("situacao").asString()).isEqualTo("CONCILIADA");
        assertThat(resultado.get("resultado").get("efeito").get("contado").isNumber()).isTrue();
        assertThat(resultado.get("resultado").get("efeito").get("contado").decimalValue())
                .isEqualByComparingTo(contado);
        assertThat(contar("contagem_estoque")).isEqualTo(1);
        assertThat(contar("revisao_contagem")).isEqualTo(1);
        assertThat(
                        jdbc.queryForObject(
                                "select contado from wms.revisao_contagem", BigDecimal.class))
                .isEqualByComparingTo(contado);
        assertThat(
                        jdbc.queryForObject(
                                "select quantidade from wms.unidade_logistica where id=?",
                                BigDecimal.class,
                                u.get("id").longValue()))
                .isEqualByComparingTo(fisico);
        assertThat(
                        resultado
                                .get("resultado")
                                .get("efeito")
                                .get("origens")
                                .get(0)
                                .get("quantidade")
                                .decimalValue())
                .isEqualByComparingTo(fisico);
        var depois = D30FotografiaFisica.capturar(jdbc);
        assertThat(
                        jdbc.queryForObject(
                                "select conteudo_hash from wms.operacao_administrativa where operacao_id=?",
                                String.class,
                                comando.get("operacaoId").toString()))
                .isEqualTo(hashConciliacaoEsperado);
        assertThat(hashConciliacaoEsperado).isNotEqualTo(hashEsperado);
        var consultaEfeito = get(consultaRota, supervisor);
        assertThat(consultaEfeito.statusCode()).isEqualTo(200);
        assertThat(observador.readTree(consultaEfeito.body())).isEqualTo(resultado);
        assertThat(D30FotografiaFisica.capturar(jdbc)).isEqualTo(depois);
        var replayEfeito = post(rota, comando, supervisor);
        assertThat(replayEfeito.statusCode()).isEqualTo(200);
        assertThat(observador.readTree(replayEfeito.body())).isEqualTo(resultado);
        assertThat(observador.readTree(post("/api/v1/contingencias", envelope, supervisor).body()))
                .isEqualTo(original);
        assertThat(D30FotografiaFisica.capturar(jdbc)).isEqualTo(depois);
        leitura.put("contado", contado.equals(BigDecimal.ZERO) ? BigDecimal.ONE : BigDecimal.ZERO);
        resposta(post("/api/v1/contingencias", envelope, supervisor), 409);
        assertThat(D30FotografiaFisica.capturar(jdbc)).isEqualTo(depois);
        java.nio.file.Files.writeString(
                java.nio.file.Path.of(System.getProperty("wms.test.evidencias.dir"))
                        .resolve("d30-cedro-contingencia-number-" + UUID.randomUUID() + ".json"),
                mapper.writeValueAsString(
                        Map.of(
                                "caso",
                                "d30NumeroRealEmMapEObjetoConservaContagemExata",
                                "entradaNumber",
                                literal,
                                "hashEsperadoAntes",
                                hashEsperado,
                                "contadoSQL",
                                literal,
                                "originalResultadoNULL",
                                true,
                                "saldoFisico",
                                fisico,
                                "replay64Igual",
                                true,
                                "limite",
                                "Efeito CONTAGEM; nao hash universal nem SQLServer")),
                java.nio.file.StandardOpenOption.CREATE_NEW);
    }

    @Test
    void d30CompromissosDeEmbalagemEArmazemIncluemCargaContagemEOutroCliente() throws Exception {
        long embalagemC1 = embalagem.getId(), clienteC1 = cliente.getId();
        String operadorC1 = operador;
        long pedidoC1 = receber("100", "0");
        long entradaC1 =
                resposta(get(PEDIDOS + pedidoC1 + "/entradas", operador), 200)
                        .get("itens")
                        .get(0)
                        .get("id")
                        .longValue();
        d30CompromissoSemEfeito(
                "EMBALAGEM", embalagemC1, "UNITIZACAO_PENDENTE", "ENTRADA_CONFERIDA", entradaC1);
        d30CompromissoSemEfeito(
                "ARMAZEM", armazem.getId(), "UNITIZACAO_PENDENTE", "ENTRADA_CONFERIDA", entradaC1);
        var u = unitizar(pedidoC1, "100", "BOA");
        posicionar(u, endereco("D30-COMPROMISSO-C1", "ARMAZENAGEM"));
        u = atual(u);
        var contagem = contar(u, "70");
        var carga = carga();
        for (String dono : List.of("EMBALAGEM", "ARMAZEM")) {
            long id = dono.equals("EMBALAGEM") ? embalagemC1 : armazem.getId();
            d30CompromissoSemEfeito(
                    dono,
                    id,
                    "CONTAGEM_PENDENTE",
                    "CONTAGEM_ESTOQUE",
                    contagem.get("id").longValue());
            d30CompromissoSemEfeito(
                    dono, id, "CARGA_PENDENTE", "CARGA_INICIAL", carga.get("id").longValue());
        }
        cliente =
                clientes.saveAndFlush(
                        new Cliente(
                                "D30-C2",
                                "Outro cliente ficticio",
                                "11111111000191",
                                Instant.now()));
        criarProduto();
        assertThat(cliente.getId()).isNotEqualTo(clienteC1);
        operador = token("OPERACAO", List.of(cliente.getId()), List.of(armazem.getId()));
        supervisor = token("SUPERVISOR", List.of(cliente.getId()), List.of(armazem.getId()));
        long pedidoC2 = receber("50", "0");
        long entradaC2 =
                resposta(get(PEDIDOS + pedidoC2 + "/entradas", operador), 200)
                        .get("itens")
                        .get(0)
                        .get("id")
                        .longValue();
        var comando =
                Map.of(
                        "operacaoId",
                        UUID.randomUUID(),
                        "versaoPedido",
                        versao(pedidoC2),
                        "motivo",
                        "Volumes fisicos distintos do segundo cliente",
                        "unidades",
                        List.of(
                                Map.of(
                                        "embalagemId",
                                        embalagem.getId(),
                                        "tipo",
                                        "VOLUME",
                                        "condicao",
                                        "BOA",
                                        "quantidade",
                                        "25"),
                                Map.of(
                                        "embalagemId",
                                        embalagem.getId(),
                                        "tipo",
                                        "VOLUME",
                                        "condicao",
                                        "BOA",
                                        "quantidade",
                                        "25")));
        var volumes =
                resposta(
                                post(
                                        PEDIDOS
                                                + pedidoC2
                                                + "/entradas/"
                                                + entradaC2
                                                + "/unitizacao",
                                        comando,
                                        operador),
                                200)
                        .get("unidades");
        assertThat(volumes.size()).isEqualTo(2);
        assertThat(volumes.get(0).get("unidade").get("id").longValue())
                .isNotEqualTo(volumes.get(1).get("unidade").get("id").longValue());
        for (var volume : volumes) {
            var v = volume.get("unidade");
            assertThat(v.get("tipo").asString()).isEqualTo("VOLUME");
            assertThat(v.get("clienteId").longValue()).isEqualTo(cliente.getId());
            assertThat(v.get("produtoId").longValue()).isEqualTo(produto.getId());
            assertThat(v.get("quantidade").decimalValue()).isEqualByComparingTo("25");
            assertThat(
                            jdbc.queryForObject(
                                    "select entrada_id from wms.conteudo_unidade where unidade_id=?",
                                    Long.class,
                                    v.get("id").longValue()))
                    .isEqualTo(entradaC2);
            assertThat(
                            jdbc.queryForObject(
                                    "select quantidade from wms.conteudo_unidade where unidade_id=?",
                                    BigDecimal.class,
                                    v.get("id").longValue()))
                    .isEqualByComparingTo("25");
            var antes = D30FotografiaFisica.capturar(jdbc);
            resposta(get(UNIDADES + v.get("codigo").asString(), operadorC1), 403);
            assertThat(
                            resposta(get(UNIDADES + v.get("codigo").asString(), operador), 200)
                                    .get("unidade")
                                    .get("id")
                                    .longValue())
                    .isEqualTo(v.get("id").longValue());
            assertThat(D30FotografiaFisica.capturar(jdbc)).isEqualTo(antes);
            d30CompromissoSemEfeito(
                    "ARMAZEM",
                    armazem.getId(),
                    "SALDO_FISICO",
                    "UNIDADE_LOGISTICA",
                    v.get("id").longValue());
        }
        d30CompromissoSemEfeito(
                "ARMAZEM",
                armazem.getId(),
                "SALDO_FISICO",
                "UNIDADE_LOGISTICA",
                u.get("id").longValue());
        for (String dono : List.of("EMBALAGEM", "ARMAZEM")) {
            long id = dono.equals("EMBALAGEM") ? embalagemC1 : armazem.getId();
            String rota = "/api/v1/encerramentos/" + dono + "/" + id;
            var atual = resposta(get(rota + "/impedimentos", gestor), 200);
            var solicitar = new HashMap<String, Object>();
            solicitar.put("operacaoId", UUID.randomUUID());
            solicitar.put("versao", atual.get("versao").longValue());
            solicitar.put("motivo", "Encerramento preserva compromissos identificados");
            var pendente = resposta(post(rota + "/solicitar", solicitar, gestor), 200);
            solicitar.put("operacaoId", UUID.randomUUID());
            solicitar.put("versao", pendente.get("versao").longValue());
            var antes = D30FotografiaFisica.capturar(jdbc);
            assertThat(
                            resposta(post(rota + "/inativar", solicitar, gestor), 409)
                                    .get("codigo")
                                    .asString())
                    .isEqualTo("ENCERRAMENTO_IMPEDIDO");
            assertThat(D30FotografiaFisica.capturar(jdbc)).isEqualTo(antes);
        }
    }

    private void d30CompromissoSemEfeito(
            String dono, long idDono, String codigo, String recurso, long id) throws Exception {
        var antes = D30FotografiaFisica.capturar(jdbc);
        var resposta =
                resposta(
                        get(
                                "/api/v1/encerramentos/" + dono + "/" + idDono + "/impedimentos",
                                gestor),
                        200);
        var encontrados = new java.util.ArrayList<JsonNode>();
        for (var item : resposta.get("impedimentos"))
            if (item.get("codigo").asString().equals(codigo)
                    && item.get("recurso").asString().equals(recurso)
                    && item.get("id").longValue() == id) encontrados.add(item);
        assertThat(encontrados).as(dono + " compromisso " + codigo + "#" + id).hasSize(1);
        assertThat(encontrados.getFirst().get("detalhe").asString()).isNotBlank();
        assertThat(D30FotografiaFisica.capturar(jdbc)).isEqualTo(antes);
    }

    private JsonNode saldo() throws Exception {
        return resposta(
                get(
                        "/api/v1/estoque/saldo?clienteId="
                                + cliente.getId()
                                + "&armazemId="
                                + armazem.getId()
                                + "&produtoId="
                                + produto.getId(),
                        operador),
                200);
    }

    private JsonNode contar(JsonNode u, String q) throws Exception {
        return resposta(
                post(
                        "/api/v1/contagens",
                        Map.of(
                                "operacaoId",
                                UUID.randomUUID(),
                                "codigoUnidade",
                                u.get("codigo").asString(),
                                "versaoUnidade",
                                u.get("versao").longValue(),
                                "contado",
                                q,
                                "observadoEm",
                                Instant.now().toString(),
                                "motivo",
                                "Leitura fisica identificada"),
                        operador),
                200);
    }

    private Map<String, Object> aplicar(JsonNode c, JsonNode u) {
        return Map.of(
                "operacaoId",
                UUID.randomUUID(),
                "revisao",
                c.get("revisao").intValue(),
                "versaoUnidade",
                u.get("versao").longValue(),
                "motivo",
                "Ajuste com prova fisica",
                "causa",
                "Diferenca conferida",
                "destino",
                "Destino interno comprovado",
                "comprovacao",
                "Comprovacao ficticia identificada",
                "origens",
                List.of(
                        Map.of(
                                "entradaId",
                                c.get("origens").get(0).get("entradaId").longValue(),
                                "delta",
                                c.get("diferenca").decimalValue())));
    }

    @Test
    void d30RevisaoPublicaCargaPreservaOrigemHistoricoReplayERollback() throws Exception {
        var original = carga();
        assertThat(original.get("revisao").get("numero").intValue()).isEqualTo(1);
        assertThat(original.get("situacao").asString()).isEqualTo("PENDENTE");
        var dados =
                new HashMap<String, Object>(
                        mapper.convertValue(original.get("revisao").get("dados"), Map.class));
        dados.put("fonte", "Nota original preservada; revisao documental ficticia D30");
        var dadosTipados =
                mapper.convertValue(dados, br.com.rodogarcia.wms.dto.CargaInicialDto.Dados.class);
        String hashEsperado =
                java.util.HexFormat.of()
                        .formatHex(
                                java.security.MessageDigest.getInstance("SHA-256")
                                        .digest(mapper.writeValueAsBytes(dadosTipados)));
        var operacao = UUID.randomUUID();
        var comando =
                Map.of(
                        "operacaoId",
                        operacao,
                        "versao",
                        original.get("versao").longValue(),
                        "dados",
                        dados,
                        "motivo",
                        "Revisao documental identificada sem movimentacao fisica");
        String rota = "/api/v1/cargas-iniciais/" + original.get("id").longValue() + "/revisoes";
        // A fixture20 foi registrada em memoria; o wrapper le DECIMAL(19,6) no JPA.
        var originalJpa = new HashMap<String, Object>(mapper.convertValue(original, Map.class));
        originalJpa.put("quantidadeEstagio", new BigDecimal("20.000000"));
        var antesEsperadoJpa = mapper.valueToTree(originalJpa);
        var esperado = new HashMap<String, Object>(originalJpa);
        esperado.put("versao", original.get("versao").longValue() + 1);
        esperado.put(
                "revisao",
                Map.of("numero", 2, "conteudoHash", hashEsperado, "dados", dadosTipados));
        var respostaEsperada = mapper.valueToTree(esperado);
        var antes = D30FotografiaFisica.capturar(jdbc);
        resposta(post(rota, comando, operador), 403);
        assertThat(D30FotografiaFisica.capturar(jdbc)).isEqualTo(antes);
        var divergente = new HashMap<String, Object>(comando);
        divergente.put("operacaoId", UUID.randomUUID());
        divergente.put("versao", original.get("versao").longValue() + 1);
        assertThat(resposta(post(rota, divergente, supervisor), 409).get("codigo").asString())
                .isEqualTo("ESTAGIO_DESATUALIZADO");
        assertThat(D30FotografiaFisica.capturar(jdbc)).isEqualTo(antes);
        br.com.rodogarcia.wms.services.AuditoriaService alvo =
                org.springframework.test.util.AopTestUtils.getUltimateTargetObject(auditoria);
        org.mockito.Mockito.doThrow(
                        new IllegalStateException("D30 falha tardia ficticia de auditoria"))
                .when(alvo)
                .registrar(
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any());
        try {
            resposta(post(rota, comando, supervisor), 500);
            assertThat(D30FotografiaFisica.capturar(jdbc)).isEqualTo(antes);
        } finally {
            org.mockito.Mockito.reset(alvo);
        }
        var revisada = resposta(post(rota, comando, supervisor), 200);
        assertThat(revisada).isEqualTo(respostaEsperada);
        var depois = D30FotografiaFisica.capturar(jdbc);
        for (var tabela : antes.keySet()) {
            var pre = antes.get(tabela);
            var pos = depois.get(tabela);
            switch (tabela) {
                case "CARGA_INICIAL" -> {
                    assertThat(pos).hasSize(pre.size());
                    for (int i = 0; i < pre.size(); i++) {
                        if (((Number) pre.get(i).get("ID")).longValue()
                                != original.get("id").longValue()) {
                            assertThat(pos.get(i)).isEqualTo(pre.get(i));
                            continue;
                        }
                        assertThat(((Number) pos.get(i).get("VERSAO")).longValue())
                                .isEqualTo(original.get("versao").longValue() + 1);
                        assertThat(((Number) pos.get(i).get("REVISAO_ATUAL")).intValue())
                                .isEqualTo(2);
                        var preConservado = new HashMap<>(pre.get(i));
                        var posConservado = new HashMap<>(pos.get(i));
                        for (var campo : List.of("VERSAO", "REVISAO_ATUAL", "ALTERADA_EM")) {
                            preConservado.remove(campo);
                            posConservado.remove(campo);
                        }
                        assertThat(posConservado).isEqualTo(preConservado);
                    }
                }
                case "REVISAO_CARGA_INICIAL", "AUDITORIA_CADASTRO", "OPERACAO_ADMINISTRATIVA" -> {
                    assertThat(pos).hasSize(pre.size() + 1).containsAll(pre);
                    var novas = pos.stream().filter(linha -> !pre.contains(linha)).toList();
                    assertThat(novas).hasSize(1);
                    var nova = novas.getFirst();
                    if (tabela.equals("REVISAO_CARGA_INICIAL")) {
                        assertThat(((Number) nova.get("CARGA_ID")).longValue())
                                .isEqualTo(original.get("id").longValue());
                        assertThat(((Number) nova.get("NUMERO")).intValue()).isEqualTo(2);
                        assertThat(nova.get("CONTEUDO_HASH")).isEqualTo(hashEsperado);
                        assertThat(mapper.readTree((String) nova.get("DADOS_JSON")))
                                .isEqualTo(mapper.valueToTree(dadosTipados));
                    } else {
                        if (tabela.equals("AUDITORIA_CADASTRO")) {
                            assertThat(((Number) nova.get("REGISTRO_ID")).longValue())
                                    .isEqualTo(original.get("id").longValue());
                            assertThat(nova.get("TIPO")).isEqualTo("CARGA_INICIAL");
                            assertThat(nova.get("ACAO")).isEqualTo("REVISAO_CARGA");
                            assertThat(mapper.readTree((String) nova.get("DADOS_ANTES")))
                                    .isEqualTo(antesEsperadoJpa);
                            assertThat(mapper.readTree((String) nova.get("DADOS_DEPOIS")))
                                    .isEqualTo(respostaEsperada);
                        } else {
                            assertThat(((Number) nova.get("RECURSO_ID")).longValue())
                                    .isEqualTo(original.get("id").longValue());
                            assertThat(nova.get("TIPO")).isEqualTo("REVISAO_CARGA");
                            assertThat(nova.get("OPERACAO_ID").toString())
                                    .isEqualTo(operacao.toString());
                            assertThat(mapper.readTree((String) nova.get("RESULTADO")))
                                    .isEqualTo(respostaEsperada);
                        }
                    }
                }
                default -> assertThat(pos).isEqualTo(pre);
            }
        }
        assertThat(resposta(post(rota, comando, supervisor), 200)).isEqualTo(revisada);
        assertThat(D30FotografiaFisica.capturar(jdbc)).isEqualTo(depois);
        divergente = new HashMap<>(comando);
        divergente.put("motivo", "Mesmo UUID com conteudo documental divergente D30");
        assertThat(resposta(post(rota, divergente, supervisor), 409).get("codigo").asString())
                .isEqualTo("OPERACAO_REUTILIZADA");
        assertThat(D30FotografiaFisica.capturar(jdbc)).isEqualTo(depois);
        divergente = new HashMap<>(comando);
        divergente.put("operacaoId", UUID.randomUUID());
        assertThat(resposta(post(rota, divergente, supervisor), 409).get("codigo").asString())
                .isEqualTo("ESTAGIO_DESATUALIZADO");
        assertThat(D30FotografiaFisica.capturar(jdbc)).isEqualTo(depois);
        java.nio.file.Files.writeString(
                java.nio.file.Path.of(System.getProperty("wms.test.evidencias.dir"))
                        .resolve("d30-cedro-revisao-carga-" + operacao + ".json"),
                mapper.writeValueAsString(
                        Map.of(
                                "caso",
                                "d30RevisaoPublicaCargaPreservaOrigemHistoricoReplayERollback",
                                "esperadoAntes",
                                respostaEsperada,
                                "original",
                                original,
                                "resposta",
                                revisada,
                                "antes64",
                                antes,
                                "depois64",
                                depois,
                                "replay64Igual",
                                true,
                                "rollback64Igual",
                                true)),
                java.nio.file.StandardOpenOption.CREATE_NEW);
    }

    private JsonNode carga() throws Exception {
        var nota =
                Map.of(
                        "versao",
                        0,
                        "serie",
                        1,
                        "numero",
                        ++numero,
                        "emissao",
                        "2026-09-01",
                        "itens",
                        List.of(
                                Map.of(
                                        "numeroItem",
                                        1,
                                        "produtoId",
                                        produto.getId(),
                                        "quantidadePrevista",
                                        20)));
        var dados =
                Map.of(
                        "referenciaPedido",
                        "REG-" + numero,
                        "nota",
                        nota,
                        "chegadaReal",
                        CHEGADA,
                        "dataFifo",
                        CHEGADA,
                        "quantidadeBoa",
                        20,
                        "quantidadeAvariada",
                        0,
                        "unidades",
                        List.of(
                                Map.of(
                                        "embalagemId",
                                        embalagem.getId(),
                                        "tipo",
                                        "PALLET",
                                        "condicao",
                                        "BOA",
                                        "quantidade",
                                        10),
                                Map.of(
                                        "embalagemId",
                                        embalagem.getId(),
                                        "tipo",
                                        "PALLET",
                                        "condicao",
                                        "BOA",
                                        "quantidade",
                                        10)),
                        "fonte",
                        "Origem documental ficticia fornecida");
        return resposta(
                post(
                        "/api/v1/cargas-iniciais",
                        Map.of(
                                "operacaoId",
                                UUID.randomUUID(),
                                "clienteId",
                                cliente.getId(),
                                "armazemId",
                                armazem.getId(),
                                "produtoId",
                                produto.getId(),
                                "referencia",
                                "L1",
                                "etiquetaFornecida",
                                "L1",
                                "quantidade",
                                20,
                                "dados",
                                dados,
                                "motivo",
                                "Carga inicial ficticia comprovada"),
                        supervisor),
                200);
    }

    private Map<String, Object> conferencia(JsonNode c, List<String> etiquetas) {
        return Map.of(
                "operacaoId",
                UUID.randomUUID(),
                "versao",
                c.get("versao").longValue(),
                "revisao",
                c.get("revisao").get("numero").intValue(),
                "conteudoHash",
                c.get("revisao").get("conteudoHash").asString(),
                "leitura",
                c.get("referencia").asString(),
                "etiquetasUnidades",
                etiquetas,
                "motivo",
                "Conferencia por todas etiquetas");
    }

    private void criarProduto() {
        produto =
                produtos.saveAndFlush(
                        new Produto(
                                cliente,
                                "SKU",
                                "Produto ficticio",
                                "UN",
                                TipoQuantidade.CONTAGEM,
                                0,
                                false,
                                false,
                                null,
                                Instant.now()));
        embalagem =
                embalagens.saveAndFlush(
                        new Embalagem(
                                produto,
                                "DUN-10",
                                "Embalagem ficticia",
                                new BigDecimal("10"),
                                Instant.now()));
    }

    private long receber(String boa, String avariada) throws Exception {
        long pedido =
                resposta(
                                post(
                                        "/api/v1/pedidos-entrada",
                                        Map.of(
                                                "clienteId",
                                                cliente.getId(),
                                                "armazemId",
                                                armazem.getId(),
                                                "referencia",
                                                "PED-" + ++numero),
                                        operador),
                                201)
                        .get("id")
                        .longValue();
        resposta(
                post(
                        PEDIDOS + pedido + "/notas",
                        Map.of(
                                "versao",
                                versao(pedido),
                                "serie",
                                1,
                                "numero",
                                numero,
                                "emissao",
                                "2026-09-01",
                                "itens",
                                List.of(
                                        Map.of(
                                                "numeroItem",
                                                1,
                                                "produtoId",
                                                produto.getId(),
                                                "quantidadePrevista",
                                                new BigDecimal(boa)
                                                        .add(new BigDecimal(avariada))))),
                        operador),
                200);
        long item =
                resposta(get(PEDIDOS + pedido, operador), 200)
                        .get("notas")
                        .get(0)
                        .get("itens")
                        .get(0)
                        .get("id")
                        .longValue();
        resposta(
                post(
                        PEDIDOS + pedido + "/iniciar-conferencia",
                        Map.of("versao", versao(pedido), "motivo", "Conferencia iniciada"),
                        operador),
                200);
        resposta(
                post(
                        PEDIDOS + pedido + "/chegadas",
                        Map.of(
                                "versao",
                                versao(pedido),
                                "operacaoId",
                                UUID.randomUUID(),
                                "chegouEm",
                                CHEGADA,
                                "observacao",
                                "Conferencia fisica",
                                "itens",
                                List.of(
                                        Map.of(
                                                "itemNotaId",
                                                item,
                                                "quantidadeBoa",
                                                boa,
                                                "quantidadeAvariada",
                                                avariada))),
                        operador),
                200);
        resposta(
                post(
                        PEDIDOS + pedido + "/efetivacao",
                        Map.of(
                                "versao",
                                versao(pedido),
                                "motivo",
                                "Conferencia aprovada",
                                "aceitarDivergencias",
                                true),
                        supervisor),
                200);
        return pedido;
    }

    private long versao(long pedido) throws Exception {
        return resposta(get(PEDIDOS + pedido, operador), 200)
                .get("pedido")
                .get("versao")
                .longValue();
    }

    private JsonNode unitizar(long pedido, String quantidade, String condicao) throws Exception {
        long entrada =
                resposta(get(PEDIDOS + pedido + "/entradas", operador), 200)
                        .get("itens")
                        .get(0)
                        .get("id")
                        .longValue();
        return resposta(
                        post(
                                PEDIDOS + pedido + "/entradas/" + entrada + "/unitizacao",
                                Map.of(
                                        "operacaoId",
                                        UUID.randomUUID(),
                                        "versaoPedido",
                                        versao(pedido),
                                        "motivo",
                                        "Unitizacao conferida",
                                        "unidades",
                                        List.of(
                                                Map.of(
                                                        "embalagemId",
                                                        embalagem.getId(),
                                                        "tipo",
                                                        "PALLET",
                                                        "condicao",
                                                        condicao,
                                                        "quantidade",
                                                        quantidade))),
                                operador),
                        200)
                .get("unidades")
                .get(0)
                .get("unidade");
    }

    private JsonNode criarEndereco(String codigo, String tipo) throws Exception {
        return resposta(
                post(
                        "/api/v1/enderecos",
                        Map.of(
                                "armazemId",
                                armazem.getId(),
                                "codigo",
                                codigo,
                                "rua",
                                "R",
                                "nivel",
                                1,
                                "posicao",
                                codigo,
                                "descricao",
                                "Posicao ficticia",
                                "tipo",
                                tipo,
                                "sequenciaColeta",
                                0),
                        gestor),
                201);
    }

    private JsonNode endereco(String codigo, String tipo) throws Exception {
        return configurar(criarEndereco(codigo, tipo), "PALLET", gestor, 200);
    }

    private Map<String, Object> limites() {
        return Map.of(
                "pesoKg",
                1000,
                "alturaMetros",
                2,
                "larguraMetros",
                2,
                "profundidadeMetros",
                2,
                "empilhamentoMaximo",
                2);
    }

    private JsonNode configurar(JsonNode e, String tipo, String token, int status)
            throws Exception {
        return resposta(
                enviar(
                        "PUT",
                        "/api/v1/enderecos/" + e.get("id").longValue() + "/capacidade",
                        Map.of(
                                "versao",
                                e.get("versao").longValue(),
                                "tipoUnidadePermitido",
                                tipo,
                                "limites",
                                limites(),
                                "motivo",
                                "Capacidade conferida"),
                        token),
                status);
    }

    private Map<String, Object> medidas(int posicoes) {
        return new HashMap<>(
                Map.of(
                        "pesoKg",
                        500,
                        "alturaMetros",
                        1,
                        "larguraMetros",
                        posicoes == 1 ? 1 : 3,
                        "profundidadeMetros",
                        1,
                        "empilhamento",
                        1,
                        "posicoesNecessarias",
                        posicoes));
    }

    private Map<String, Object> posicionamento(JsonNode u, JsonNode... destinos) {
        var dados = new HashMap<String, Object>();
        dados.put("operacaoId", UUID.randomUUID());
        dados.put("versaoUnidade", u.get("versao").longValue());
        dados.put("motivo", "Posicao fisica confirmada");
        dados.put(
                "destinos",
                java.util.Arrays.stream(destinos)
                        .map(
                                e ->
                                        Map.of(
                                                "enderecoId",
                                                e.get("id").longValue(),
                                                "codigoLido",
                                                e.get("codigo").asString()))
                        .toList());
        if (u.get("versao").longValue() == 0) dados.put("medidas", medidas(destinos.length));
        return dados;
    }

    private JsonNode posicionar(JsonNode u, JsonNode e) throws Exception {
        return resposta(post(rota(u, "/movimentos"), posicionamento(u, e), operador), 200)
                .get("estoque");
    }

    private Map<String, Object> bloqueio(JsonNode u) {
        return Map.of(
                "operacaoId",
                UUID.randomUUID(),
                "versaoUnidade",
                u.get("versao").longValue(),
                "motivo",
                "Verificacao operacional");
    }

    private String rota(JsonNode u, String sufixo) {
        return UNIDADES + u.get("codigo").asString() + sufixo;
    }

    private String saldoRota() {
        return "/api/v1/estoque/saldo?clienteId="
                + cliente.getId()
                + "&armazemId="
                + armazem.getId()
                + "&produtoId="
                + produto.getId();
    }

    private void assertSaldo(String campo, String valor) throws Exception {
        assertThat(resposta(get(saldoRota(), operador), 200).get(campo).decimalValue())
                .as(campo)
                .isEqualByComparingTo(valor);
    }

    private int contar(String tabela) {
        return jdbc.queryForObject("select count(*) from wms." + tabela, Integer.class);
    }

    private HttpResponse<String> post(String rota, Object dados, String token) throws Exception {
        return enviar("POST", rota, dados, token);
    }

    private HttpResponse<String> get(String rota, String token) throws Exception {
        return enviar("GET", rota, null, token);
    }

    private HttpResponse<String> enviar(String metodo, String rota, Object dados, String token)
            throws Exception {
        var builder =
                HttpRequest.newBuilder(
                                URI.create(
                                        "http://127.0.0.1:"
                                                + environment.getRequiredProperty(
                                                        "local.server.port")
                                                + rota))
                        .timeout(Duration.ofSeconds(30))
                        .header("Content-Type", "application/json");
        if (token != null) builder.header("Authorization", "Bearer " + token);
        return http.send(
                builder.method(
                                metodo,
                                dados == null
                                        ? HttpRequest.BodyPublishers.noBody()
                                        : HttpRequest.BodyPublishers.ofString(
                                                mapper.writeValueAsString(dados)))
                        .build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private JsonNode resposta(HttpResponse<String> resposta, int status) {
        assertThat(resposta.statusCode()).as(resposta.body()).isEqualTo(status);
        return mapper.readTree(resposta.body());
    }

    private String token(String perfil, List<Long> clientes, List<Long> armazens) {
        var agora = Instant.now();
        var claims =
                JwtClaimsSet.builder()
                        .issuer("https://identidade.test.invalid")
                        .subject("estoquista-teste")
                        .audience(List.of("wms-testes"))
                        .issuedAt(agora)
                        .expiresAt(agora.plusSeconds(300))
                        .claim("wms_perfil", perfil)
                        .claim("wms_clientes", clientes.stream().map(String::valueOf).toList())
                        .claim("wms_armazens", armazens.stream().map(String::valueOf).toList())
                        .build();
        return encoder.encode(
                        JwtEncoderParameters.from(
                                JwsHeader.with(SignatureAlgorithm.RS256).build(), claims))
                .getTokenValue();
    }
}
