package br.com.rodogarcia.wms;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;

import br.com.rodogarcia.wms.config.IdentidadeTesteConfig;
import br.com.rodogarcia.wms.models.Armazem;
import br.com.rodogarcia.wms.models.Cliente;
import br.com.rodogarcia.wms.models.Produto;
import br.com.rodogarcia.wms.models.TipoQuantidade;
import br.com.rodogarcia.wms.repositories.ArmazemRepository;
import br.com.rodogarcia.wms.repositories.ClienteRepository;
import br.com.rodogarcia.wms.repositories.ProdutoRepository;
import br.com.rodogarcia.wms.services.OperacaoAdministrativaService;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.util.AopTestUtils;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@ActiveProfiles("test")
@TestPropertySource(
        locations = "classpath:cadastros-test.properties",
        properties =
                "spring.datasource.url=jdbc:h2:mem:wms-entrada-xml;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000;INIT=CREATE SCHEMA IF NOT EXISTS wms")
@Import(IdentidadeTesteConfig.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PedidoEntradaXmlIntegrationTest {
    @Autowired private Environment env;
    @Autowired private JsonMapper mapper;
    @Autowired private JwtEncoder encoder;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ClienteRepository clientes;
    @Autowired private ArmazemRepository armazens;
    @Autowired private ProdutoRepository produtos;
    @MockitoSpyBean private OperacaoAdministrativaService operacoes;
    private final HttpClient http = HttpClient.newHttpClient();
    private final AtomicInteger numeros = new AtomicInteger(293730);
    private Cliente tigre, outro;
    private Armazem dalga;
    private Produto produto, unidadeDiferente;
    private String token;

    @BeforeAll
    void prepararSomenteH2() {
        assertThat(env.getRequiredProperty("spring.datasource.url"))
                .startsWith("jdbc:h2:mem:wms-entrada-xml;");
        var now = Instant.now();
        tigre =
                clientes.saveAndFlush(
                        new Cliente("TIGRE", "Proprietário fictício", "33064262000250", now));
        outro =
                clientes.saveAndFlush(
                        new Cliente("OUTRO", "Outro proprietário fictício", "12345678000199", now));
        dalga =
                armazens.saveAndFlush(
                        new Armazem(
                                "DALGA",
                                "Armazém fictício",
                                "04547874000203",
                                "Curitiba",
                                "PR",
                                now));
        produto =
                produtos.saveAndFlush(
                        new Produto(
                                tigre,
                                "69230424",
                                "Bandeja fictícia",
                                "PEC",
                                TipoQuantidade.CONTAGEM,
                                0,
                                false,
                                false,
                                null,
                                now));
        unidadeDiferente =
                produtos.saveAndFlush(
                        new Produto(
                                tigre,
                                "OUTRA-UNIDADE",
                                "Produto em caixa",
                                "CX",
                                TipoQuantidade.CONTAGEM,
                                0,
                                false,
                                false,
                                null,
                                now));
        token = token(List.of(tigre.getId(), outro.getId()), List.of(dalga.getId()));
    }

    @Test
    void previaNaoGravaEConfirmacaoPreservaOriginalSemMovimentoFisico() throws Exception {
        var xml = xml();
        var antes = contagens();
        var previa = previa(xml, List.of());
        assertThat(previa.path("podeConfirmar").asBoolean()).isTrue();
        assertThat(previa.path("clienteId").asLong()).isEqualTo(tigre.getId());
        assertThat(previa.path("armazemId").asLong()).isEqualTo(dalga.getId());
        assertThat(contagens()).isEqualTo(antes);
        var comando = comando(xml, previa, List.of());
        var criado = resposta(post("xml/confirmacao", comando), 201);
        var id = criado.path("pedido").path("id").asLong();
        assertThat(criado.path("pedido").path("situacao").asString()).isEqualTo("RASCUNHO");
        assertThat(criado.path("pedido").path("origemCriacao").asString()).isEqualTo("XML");
        assertThat(resposta(post("xml/confirmacao", comando), 201)).isEqualTo(criado);
        assertThat(resposta(get("xml/operacoes/" + comando.get("operacaoId")), 200))
                .isEqualTo(criado);
        var detail = resposta(get(String.valueOf(id)), 200);
        var note = detail.path("notas").get(0);
        assertThat(detail.path("pedido").path("origemCriacao").asString()).isEqualTo("XML");
        assertThat(note.path("primeiraChegada").isNull()).isTrue();
        assertThat(note.path("itens").get(0).path("recebidaBoa").decimalValue()).isZero();
        var documento =
                resposta(get(id + "/notas/" + note.path("id").asLong() + "/documento"), 200);
        assertThat(documento.path("xmlOriginal").asString()).isEqualTo(xml);
        assertThat(
                        documento
                                .path("documento")
                                .path("itens")
                                .get(0)
                                .path("quantidadeComercial")
                                .decimalValue())
                .isEqualByComparingTo("32000");
        assertThat(
                        jdbc.queryForObject(
                                "select count(*) from wms.nota_entrada where pedido_id=?",
                                Long.class,
                                id))
                .isEqualTo(1);
        for (var table : List.of("chegada_recebimento", "entrada_conferida", "unidade_logistica"))
            assertThat(contar(table)).isEqualTo(antes.get(table));
        comando.put("referencia", "ALTERADA");
        resposta(post("xml/confirmacao", comando), 409);
    }

    @Test
    void duasConfirmacoesSimultaneasDoMesmoComandoCriamUmaNota() throws Exception {
        var xml = xml();
        var comando = comando(xml, previa(xml, List.of()), List.of());
        var responses = paralelo(comando, false);
        assertThat(responses.stream().map(HttpResponse::statusCode))
                .containsExactlyInAnyOrder(201, 201);
        assertThat(mapper.readTree(responses.getFirst().body()).path("pedido").path("id"))
                .isEqualTo(mapper.readTree(responses.getLast().body()).path("pedido").path("id"));
    }

    @Test
    void duasOperacoesDaMesmaNotaNaoCriamPedidosOrfaos() throws Exception {
        var xml = xml();
        var comando = comando(xml, previa(xml, List.of()), List.of());
        long antes = contar("pedido_entrada");
        assertThat(paralelo(comando, true).stream().map(HttpResponse::statusCode))
                .containsExactlyInAnyOrder(201, 409);
        assertThat(contar("pedido_entrada")).isEqualTo(antes + 1);
        var duplicada = previa(xml, List.of());
        assertThat(duplicada.path("podeConfirmar").asBoolean()).isFalse();
        assertThat(duplicada.path("pedidoExistenteId").isNull()).isFalse();
        var oculto =
                resposta(
                        enviar(
                                "POST",
                                "xml/previa",
                                Map.of("xml", xml, "associacoes", List.of()),
                                token(List.of(), List.of())),
                        200);
        assertThat(oculto.path("pedidoExistenteId").isNull()).isTrue();
    }

    @Test
    void associacaoManualMantemLinhasSeparadasMesmoProdutoERecusaUnidade() throws Exception {
        var xml = xml();
        int ini = xml.indexOf("<det "), fim = xml.indexOf("</det>") + 6;
        xml =
                xml.substring(0, fim)
                        + xml.substring(ini, fim)
                                .replace("nItem=\"1\"", "nItem=\"2\"")
                                .replace("<cProd>69230424</cProd>", "<cProd>OUTRO-CODIGO</cProd>")
                        + xml.substring(fim);
        var pendente = previa(xml, List.of());
        assertThat(pendente.path("podeConfirmar").asBoolean()).isFalse();
        var associacoes = List.of(Map.of("numeroItem", 2, "produtoId", produto.getId()));
        var previa = previa(xml, associacoes);
        assertThat(previa.path("podeConfirmar").asBoolean()).isTrue();
        var criado = resposta(post("xml/confirmacao", comando(xml, previa, associacoes)), 201);
        var detail = resposta(get(criado.path("pedido").path("id").asString()), 200);
        assertThat(detail.path("notas").get(0).path("itens").size()).isEqualTo(2);
        assertThat(
                        previa(
                                        xml(),
                                        List.of(
                                                Map.of(
                                                        "numeroItem",
                                                        1,
                                                        "produtoId",
                                                        unidadeDiferente.getId())))
                                .path("podeConfirmar")
                                .asBoolean())
                .isFalse();
    }

    @Test
    void recusaProprietarioErradoPermissaoERevisaoAlterada() throws Exception {
        var xml = xml();
        var previa = previa(xml, List.of());
        var errado =
                resposta(
                        post(
                                "xml/previa",
                                Map.of(
                                        "xml",
                                        xml,
                                        "clienteId",
                                        outro.getId(),
                                        "armazemId",
                                        dalga.getId(),
                                        "associacoes",
                                        List.of())),
                        200);
        assertThat(errado.path("podeConfirmar").asBoolean()).isFalse();
        var cmd = comando(xml, previa, List.of());
        resposta(enviar("POST", "xml/confirmacao", cmd, token(List.of(), List.of())), 403);
        jdbc.update("update wms.produto set versao=versao+1 where id=?", produto.getId());
        assertThat(resposta(post("xml/confirmacao", cmd), 409).path("codigo").asString())
                .isEqualTo("PREVIA_DESATUALIZADA");
        var feito =
                resposta(
                        post("xml/confirmacao", comando(xml, previa(xml, List.of()), List.of())),
                        201);
        resposta(
                enviar(
                        "GET",
                        "xml/operacoes/" + feito.path("operacaoId").asString(),
                        null,
                        token(List.of(), List.of())),
                403);
    }

    @Test
    void falhaAoSalvarReciboRevertePedidoNotaItensEAuditoria() throws Exception {
        var xml = xml();
        var cmd = comando(xml, previa(xml, List.of()), List.of());
        var antes = contagens();
        OperacaoAdministrativaService alvo = AopTestUtils.getUltimateTargetObject(operacoes);
        doThrow(new IllegalStateException("Falha fictícia no recibo"))
                .when(alvo)
                .salvar(any(), any(), any(), any(), any(), any(), any());
        try {
            resposta(post("xml/confirmacao", cmd), 500);
        } finally {
            reset(alvo);
        }
        assertThat(contagens()).isEqualTo(antes);
    }

    @Test
    void manualComXmlPosteriorContinuaManualEConferenciaPodeDivergir() throws Exception {
        var xml = xml();
        var doc = previa(xml, List.of()).path("documento");
        var manual =
                resposta(
                        post(
                                "",
                                Map.of(
                                        "clienteId",
                                        tigre.getId(),
                                        "armazemId",
                                        dalga.getId(),
                                        "referencia",
                                        "MANUAL-" + doc.path("numero").asString())),
                        201);
        var id = manual.path("id").asLong();
        var importada =
                resposta(
                        post(
                                id + "/notas/xml",
                                Map.of("versao", manual.path("versao").asLong(), "xml", xml)),
                        200);
        assertThat(importada.path("origemCriacao").asString()).isEqualTo("MANUAL");
        var iniciada =
                resposta(
                        post(
                                id + "/iniciar-conferencia",
                                Map.of(
                                        "versao",
                                        importada.path("versao").asLong(),
                                        "motivo",
                                        "Conferência local fictícia")),
                        200);
        var item =
                resposta(get(String.valueOf(id)), 200)
                        .path("notas")
                        .get(0)
                        .path("itens")
                        .get(0)
                        .path("id")
                        .asLong();
        resposta(
                post(
                        id + "/chegadas",
                        Map.of(
                                "versao",
                                iniciada.path("versao").asLong(),
                                "operacaoId",
                                UUID.randomUUID(),
                                "chegouEm",
                                "2026-09-03T12:00:00Z",
                                "observacao",
                                "Recebimento parcial fictício",
                                "itens",
                                List.of(
                                        Map.of(
                                                "itemNotaId",
                                                item,
                                                "quantidadeBoa",
                                                100,
                                                "quantidadeAvariada",
                                                0)))),
                200);
        var detail = resposta(get(String.valueOf(id)), 200);
        assertThat(detail.path("divergente").asBoolean()).isTrue();
        assertThat(
                        detail.path("notas")
                                .get(0)
                                .path("itens")
                                .get(0)
                                .path("diferenca")
                                .decimalValue())
                .isEqualByComparingTo("-31900");
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void qualConf01AlcanceParcialRecusaPreviaReplayReciboEDocumentoSemEfeitos(
            boolean clientePermitido) throws Exception {
        var xml = xml();
        var cmd = comando(xml, previa(xml, List.of()), List.of());
        var criado = resposta(post("xml/confirmacao", cmd), 201);
        long id = criado.path("pedido").path("id").asLong();
        long nota = resposta(get(String.valueOf(id)), 200).path("notas").get(0).path("id").asLong();
        var antes = contagens();
        String restrito =
                token(
                        clientePermitido ? List.of(tigre.getId()) : List.of(),
                        clientePermitido ? List.of() : List.of(dalga.getId()));
        var leitura =
                Map.of(
                        "xml",
                        xml,
                        "clienteId",
                        tigre.getId(),
                        "armazemId",
                        dalga.getId(),
                        "associacoes",
                        List.of());
        resposta(enviar("POST", "xml/previa", leitura, restrito), 403);
        resposta(enviar("POST", "xml/confirmacao", cmd, restrito), 403);
        resposta(enviar("GET", "xml/operacoes/" + cmd.get("operacaoId"), null, restrito), 403);
        var recusado = enviar("GET", id + "/notas/" + nota + "/documento", null, restrito);
        resposta(recusado, 403);
        assertThat(recusado.body()).doesNotContain(xml, "xmlOriginal");
        assertThat(contagens()).isEqualTo(antes);
        assertThat(resposta(get("xml/operacoes/" + cmd.get("operacaoId")), 200)).isEqualTo(criado);
    }

    @Test
    void qualConf01ProdutoInativadoInvalidaRevisaoERecusaConfirmacaoAtualizadaSemFisico()
            throws Exception {
        String sku =
                ("QUAL-" + UUID.randomUUID().toString().substring(0, 8))
                        .toUpperCase(java.util.Locale.ROOT);
        var isolado =
                produtos.saveAndFlush(
                        new Produto(
                                tigre,
                                sku,
                                "Produto fictício exclusivo",
                                "PEC",
                                TipoQuantidade.CONTAGEM,
                                0,
                                false,
                                false,
                                null,
                                Instant.now()));
        var xml = xml().replace("<cProd>69230424</cProd>", "<cProd>" + sku + "</cProd>");
        var inicial = previa(xml, List.of());
        var prova =
                Map.of(
                        "demanda", "QUAL-CONF01",
                        "clienteId", tigre.getId(),
                        "produtoId", isolado.getId(),
                        "skuGravado", produtos.findById(isolado.getId()).orElseThrow().getSku(),
                        "skuNormalizadoDaBusca",
                                br.com.rodogarcia.wms.services.CadastroSupport.codigo(sku),
                        "produtoEncontradoPeloSkuNormalizado",
                                produtos.buscarIdPorSku(
                                                tigre.getId(),
                                                br.com.rodogarcia.wms.services.CadastroSupport
                                                        .codigo(sku))
                                        .orElse(-1L),
                        "xmlFicticio", xml,
                        "previaCompleta", inicial);
        String evidencias = System.getProperty("wms.test.evidencias.dir");
        if (evidencias != null)
            java.nio.file.Files.writeString(
                    java.nio.file.Path.of(
                            evidencias,
                            "qual-conf01-previa-inicial-" + UUID.randomUUID() + ".json"),
                    mapper.writeValueAsString(prova),
                    java.nio.file.StandardOpenOption.CREATE_NEW);
        assertThat(
                        produtos.buscarIdPorSku(
                                tigre.getId(),
                                br.com.rodogarcia.wms.services.CadastroSupport.codigo(sku)))
                .contains(isolado.getId());
        assertThat(inicial.path("itens").get(0).path("produtoId").asLong())
                .isEqualTo(isolado.getId());
        assertThat(inicial.path("podeConfirmar").asBoolean()).as(inicial.toString()).isTrue();
        var cmd = comando(xml, inicial, List.of());
        var antes = contagens();
        assertThat(
                        jdbc.update(
                                "update wms.produto set situacao='INATIVO', versao=versao+1 where id=?",
                                isolado.getId()))
                .isEqualTo(1);
        assertThat(resposta(post("xml/confirmacao", cmd), 409).path("codigo").asString())
                .isEqualTo("PREVIA_DESATUALIZADA");
        var atualizada = previa(xml, List.of());
        assertThat(atualizada.path("revisaoPrevia").asString())
                .isNotEqualTo(inicial.path("revisaoPrevia").asString());
        assertThat(atualizada.path("podeConfirmar").asBoolean()).isFalse();
        assertThat(
                        resposta(post("xml/confirmacao", comando(xml, atualizada, List.of())), 400)
                                .path("codigo")
                                .asString())
                .isEqualTo("DADOS_INVALIDOS");
        assertThat(contagens()).isEqualTo(antes);
    }

    @Test
    void qualConf01D33ArmazemAutorizadoComDocumentoDiferenteMantemEmitenteProprietario()
            throws Exception {
        var alternativo =
                armazens.saveAndFlush(
                        new Armazem(
                                "QUAL-" + UUID.randomUUID().toString().substring(0, 8),
                                "Outro local fictício",
                                "88888888000100",
                                "Curitiba",
                                "PR",
                                Instant.now()));
        String auth = token(List.of(tigre.getId()), List.of(alternativo.getId()));
        var xml = xml();
        var antes = contagens();
        var previa =
                resposta(
                        enviar(
                                "POST",
                                "xml/previa",
                                Map.of(
                                        "xml",
                                        xml,
                                        "clienteId",
                                        tigre.getId(),
                                        "armazemId",
                                        alternativo.getId(),
                                        "associacoes",
                                        List.of()),
                                auth),
                        200);
        assertThat(previa.path("podeConfirmar").asBoolean()).isTrue();
        assertThat(previa.path("clienteId").asLong()).isEqualTo(tigre.getId());
        assertThat(previa.path("armazemId").asLong()).isEqualTo(alternativo.getId());
        assertThat(previa.path("avisos").toString()).contains("difere do destinatário");
        var cmd = comando(xml, previa, List.of());
        cmd.put("armazemId", alternativo.getId());
        var criado = resposta(enviar("POST", "xml/confirmacao", cmd, auth), 201);
        assertThat(criado.path("pedido").path("clienteId").asLong()).isEqualTo(tigre.getId());
        assertThat(criado.path("pedido").path("armazemId").asLong()).isEqualTo(alternativo.getId());
        assertThat(produtos.findById(produto.getId()).orElseThrow().getCliente().getId())
                .isEqualTo(tigre.getId());
        for (var tabela : List.of("chegada_recebimento", "entrada_conferida", "unidade_logistica"))
            assertThat(contar(tabela)).isEqualTo(antes.get(tabela));
    }

    private String xml() throws Exception {
        return NfeDocumentoServiceTest.exemplo()
                .replace("293729", String.valueOf(numeros.getAndIncrement()));
    }

    private JsonNode previa(String xml, List<?> assoc) throws Exception {
        return resposta(post("xml/previa", Map.of("xml", xml, "associacoes", assoc)), 200);
    }

    private Map<String, Object> comando(String xml, JsonNode previa, List<?> assoc) {
        var cmd = new HashMap<String, Object>();
        cmd.put("operacaoId", UUID.randomUUID());
        cmd.put("xml", xml);
        cmd.put("clienteId", tigre.getId());
        cmd.put("armazemId", dalga.getId());
        cmd.put("referencia", "XML-" + previa.path("documento").path("numero").asString());
        cmd.put("revisaoPrevia", previa.path("revisaoPrevia").asString());
        cmd.put("associacoes", assoc);
        return cmd;
    }

    private List<HttpResponse<String>> paralelo(Map<String, Object> cmd, boolean diferentes)
            throws Exception {
        var inicio = new CountDownLatch(1);
        try (var pool = Executors.newFixedThreadPool(2)) {
            var tarefas = new ArrayList<java.util.concurrent.Future<HttpResponse<String>>>();
            for (int n = 0; n < 2; n++) {
                var dados = new HashMap<>(cmd);
                if (diferentes) dados.put("operacaoId", UUID.randomUUID());
                tarefas.add(
                        pool.submit(
                                () -> {
                                    inicio.await(5, TimeUnit.SECONDS);
                                    return post("xml/confirmacao", dados);
                                }));
            }
            inicio.countDown();
            return List.of(
                    tarefas.getFirst().get(30, TimeUnit.SECONDS),
                    tarefas.getLast().get(30, TimeUnit.SECONDS));
        }
    }

    private long contar(String tabela) {
        return jdbc.queryForObject("select count(*) from wms." + tabela, Long.class);
    }

    private Map<String, Long> contagens() {
        var counts = new HashMap<String, Long>();
        for (var table :
                List.of(
                        "pedido_entrada",
                        "nota_entrada",
                        "item_nota_entrada",
                        "operacao_administrativa",
                        "auditoria_cadastro",
                        "chegada_recebimento",
                        "entrada_conferida",
                        "unidade_logistica")) counts.put(table, contar(table));
        return counts;
    }

    private HttpResponse<String> post(String path, Object data) throws Exception {
        return enviar("POST", path, data, token);
    }

    private HttpResponse<String> get(String path) throws Exception {
        return enviar("GET", path, null, token);
    }

    private HttpResponse<String> enviar(String method, String path, Object data, String auth)
            throws Exception {
        var request =
                HttpRequest.newBuilder(
                                URI.create(
                                        "http://127.0.0.1:"
                                                + env.getRequiredProperty("local.server.port")
                                                + "/api/v1/pedidos-entrada"
                                                + (path.isEmpty() || path.startsWith("?")
                                                        ? ""
                                                        : "/")
                                                + path))
                        .timeout(Duration.ofSeconds(25))
                        .header("Content-Type", "application/json")
                        .header("Authorization", "Bearer " + auth)
                        .method(
                                method,
                                data == null
                                        ? HttpRequest.BodyPublishers.noBody()
                                        : HttpRequest.BodyPublishers.ofString(
                                                mapper.writeValueAsString(data)))
                        .build();
        return http.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private JsonNode resposta(HttpResponse<String> response, int status) {
        assertThat(response.statusCode()).as(response.body()).isEqualTo(status);
        return mapper.readTree(response.body());
    }

    private String token(List<Long> clientes, List<Long> armazens) {
        var now = Instant.now();
        return encoder.encode(
                        JwtEncoderParameters.from(
                                JwsHeader.with(SignatureAlgorithm.RS256).build(),
                                JwtClaimsSet.builder()
                                        .issuer("https://identidade.test.invalid")
                                        .subject("recebedor-xml-teste")
                                        .audience(List.of("wms-testes"))
                                        .issuedAt(now)
                                        .expiresAt(now.plusSeconds(300))
                                        .claim("wms_perfil", "OPERACAO")
                                        .claim(
                                                "wms_clientes",
                                                clientes.stream().map(String::valueOf).toList())
                                        .claim(
                                                "wms_armazens",
                                                armazens.stream().map(String::valueOf).toList())
                                        .build()))
                .getTokenValue();
    }
}
