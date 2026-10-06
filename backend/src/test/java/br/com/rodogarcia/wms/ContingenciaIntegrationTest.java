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
        var d = conferencia(c, List.of());
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

    private JsonNode atual(JsonNode u) throws Exception {
        return resposta(get(UNIDADES + u.get("codigo").asString(), operador), 200).get("unidade");
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
