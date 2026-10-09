package br.com.rodogarcia.wms;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import br.com.rodogarcia.wms.config.IdentidadeTesteConfig;
import br.com.rodogarcia.wms.dto.FiscalCadastroDto;
import br.com.rodogarcia.wms.dto.ImportacaoEnderecoDto;
import br.com.rodogarcia.wms.models.Armazem;
import br.com.rodogarcia.wms.models.AvariaEstoque;
import br.com.rodogarcia.wms.models.BaixaSaida;
import br.com.rodogarcia.wms.models.ChegadaRecebimento;
import br.com.rodogarcia.wms.models.Cliente;
import br.com.rodogarcia.wms.models.CondicaoMercadoria;
import br.com.rodogarcia.wms.models.ConteudoUnidade;
import br.com.rodogarcia.wms.models.Embalagem;
import br.com.rodogarcia.wms.models.Endereco;
import br.com.rodogarcia.wms.models.EntradaConferida;
import br.com.rodogarcia.wms.models.FatoPermanencia;
import br.com.rodogarcia.wms.models.ItemChegada;
import br.com.rodogarcia.wms.models.ItemNotaEntrada;
import br.com.rodogarcia.wms.models.ItemPedidoSaida;
import br.com.rodogarcia.wms.models.MedidasUnidade;
import br.com.rodogarcia.wms.models.NotaEntrada;
import br.com.rodogarcia.wms.models.OcupacaoEndereco;
import br.com.rodogarcia.wms.models.PedidoEntrada;
import br.com.rodogarcia.wms.models.PedidoSaida;
import br.com.rodogarcia.wms.models.Produto;
import br.com.rodogarcia.wms.models.ReservaSaida;
import br.com.rodogarcia.wms.models.RetiradaSaida;
import br.com.rodogarcia.wms.models.SituacaoPedidoSaida;
import br.com.rodogarcia.wms.models.SituacaoReservaSaida;
import br.com.rodogarcia.wms.models.TipoEndereco;
import br.com.rodogarcia.wms.models.TipoQuantidade;
import br.com.rodogarcia.wms.models.TipoUnidadeLogistica;
import br.com.rodogarcia.wms.models.UnidadeLogistica;
import br.com.rodogarcia.wms.services.AuditoriaService;
import br.com.rodogarcia.wms.services.FiscalCadastroService;
import br.com.rodogarcia.wms.services.ImportacaoEnderecoService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.poi.common.usermodel.HyperlinkType;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@ActiveProfiles("test")
@TestPropertySource(
        locations = "classpath:cadastros-test.properties",
        properties =
                "spring.datasource.url=jdbc:h2:mem:wms-cobranca;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000;INIT=CREATE SCHEMA IF NOT EXISTS wms")
@Import(IdentidadeTesteConfig.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CobrancaIntegrationTest {
    @Autowired Environment environment;

    @Autowired
    org.springframework.boot.web.server.servlet.context.ServletWebServerApplicationContext
            contextoD30;

    @Autowired JsonMapper mapper;
    @Autowired JwtEncoder encoder;
    @Autowired JdbcTemplate jdbc;
    @Autowired PlatformTransactionManager transactions;
    @Autowired FiscalCadastroService fiscal;
    @Autowired ImportacaoEnderecoService importacao;
    @MockitoSpyBean AuditoriaService auditoria;
    @MockitoSpyBean br.com.rodogarcia.wms.services.OperacaoAdministrativaService operacoesD30;
    @MockitoSpyBean br.com.rodogarcia.wms.exceptions.ApiExceptionHandler errosD30;
    @MockitoBean Clock clock;
    private AuditoriaService alvoAuditoria;
    @PersistenceContext EntityManager em;
    private final HttpClient http =
            HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private static final Instant BASE = Instant.parse("2026-08-01T12:00:00Z");
    private Long clienteId, armazemId, produtoId, embalagemId;
    private int numero;
    private String gestor, supervisor, operador;

    private record Unidade(Long id, Long pedidoId, Long notaId, String codigo) {}

    @BeforeEach
    void preparar() {
        when(clock.instant()).thenReturn(Instant.parse("2026-10-06T12:00:00Z"));
        when(clock.getZone()).thenReturn(java.time.ZoneOffset.UTC);
        when(clock.withZone(org.mockito.ArgumentMatchers.any(java.time.ZoneId.class)))
                .thenAnswer(inv -> Clock.fixed(clock.instant(), inv.getArgument(0)));
        alvoAuditoria =
                org.springframework.test.util.AopTestUtils.getUltimateTargetObject(auditoria);
        reset(alvoAuditoria);
        br.com.rodogarcia.wms.exceptions.ApiExceptionHandler alvoErrosD30 =
                org.springframework.test.util.AopTestUtils.getUltimateTargetObject(errosD30);
        reset(alvoErrosD30);
        org.mockito.Mockito.doAnswer(
                        inv -> {
                            Exception erro = inv.getArgument(0);
                            var causas = new java.util.ArrayList<Map<String, Object>>();
                            for (Throwable causa = erro;
                                    causa != null && causas.size() < 10;
                                    causa = causa.getCause()) {
                                causas.add(
                                        Map.of(
                                                "tipo",
                                                causa.getClass().getName(),
                                                "mensagemFicticiaLocal",
                                                String.valueOf(causa.getMessage()),
                                                "framesProjeto",
                                                java.util.Arrays.stream(causa.getStackTrace())
                                                        .filter(
                                                                f ->
                                                                        f.getClassName()
                                                                                .startsWith(
                                                                                        "br.com.rodogarcia.wms"))
                                                        .map(Object::toString)
                                                        .toList()));
                            }
                            java.nio.file.Files.writeString(
                                    java.nio.file.Path.of(
                                                    System.getProperty("wms.test.evidencias.dir"))
                                            .resolve(
                                                    "d30-cedro-cobranca-causa-local-"
                                                            + UUID.randomUUID()
                                                            + ".json"),
                                    mapper.writeValueAsString(
                                            Map.of(
                                                    "contexto",
                                                    "CobrancaIntegrationTest",
                                                    "H2MemFicticio",
                                                    true,
                                                    "causas",
                                                    causas)),
                                    java.nio.file.StandardOpenOption.CREATE_NEW);
                            return inv.callRealMethod();
                        })
                .when(alvoErrosD30)
                .tratarFalhaInesperada(
                        org.mockito.ArgumentMatchers.any(Exception.class),
                        org.mockito.ArgumentMatchers.any(
                                org.springframework.web.context.request.WebRequest.class));
        SecurityContextHolder.clearContext();
        jdbc.update("update wms.unidade_logistica set reserva_saida_id=null");
        for (String t :
                List.of(
                        "memoria_servico",
                        "memoria_diaria",
                        "calculo_cobranca",
                        "marco_financeiro_avaria",
                        "rateio_fato_servico",
                        "fato_servico",
                        "servico_minimo_contrato",
                        "contrato_cobranca",
                        "vinculo_tabela_cliente",
                        "item_tabela_cobranca",
                        "tabela_cobranca",
                        "servico_cobranca",
                        "operacao_administrativa",
                        "importacao_endereco",
                        "referencia_fiscal_produto",
                        "avaria_estoque",
                        "devolucao_saida",
                        "baixa_saida",
                        "retirada_saida",
                        "cobertura_documento_saida",
                        "documento_saida",
                        "separacao_saida",
                        "fato_permanencia",
                        "operacao_saida",
                        "reserva_saida",
                        "item_pedido_saida",
                        "pedido_saida",
                        "movimento_estoque",
                        "ocupacao_endereco",
                        "operacao_unidade",
                        "conteudo_unidade",
                        "unidade_logistica",
                        "conjunto_posicoes",
                        "entrada_conferida",
                        "item_chegada",
                        "chegada_recebimento",
                        "item_nota_entrada",
                        "nota_entrada",
                        "pedido_entrada",
                        "endereco",
                        "embalagem",
                        "produto",
                        "auditoria_cadastro",
                        "cliente",
                        "armazem")) jdbc.update("delete from wms." + t);
        numero = 0;
        new TransactionTemplate(transactions)
                .executeWithoutResult(
                        tx -> {
                            var c = new Cliente("C", "Cliente ficticio", "11111111000111", BASE);
                            var a =
                                    new Armazem(
                                            "A",
                                            "Armazem ficticio",
                                            "22222222000122",
                                            "Osasco",
                                            "SP",
                                            BASE);
                            em.persist(c);
                            em.persist(a);
                            em.flush();
                            clienteId = c.getId();
                            armazemId = a.getId();
                            var p =
                                    new Produto(
                                            c,
                                            "SKU",
                                            "Produto ficticio",
                                            "UN",
                                            TipoQuantidade.CONTAGEM,
                                            0,
                                            false,
                                            false,
                                            null,
                                            BASE);
                            em.persist(p);
                            em.flush();
                            produtoId = p.getId();
                            var e =
                                    new Embalagem(
                                            p,
                                            "DUN",
                                            "Embalagem ficticia",
                                            new BigDecimal("10"),
                                            BASE);
                            em.persist(e);
                            em.flush();
                            embalagemId = e.getId();
                        });
        gestor = token("GESTOR", List.of(), List.of());
        supervisor = token("SUPERVISOR", List.of(clienteId), List.of(armazemId));
        operador = token("OPERACAO", List.of(clienteId), List.of(armazemId));
    }

    @Test
    void multipartIntegralReplayPerfilEPosicao01() throws Exception {
        byte[] arquivo = excel("normal", 2);
        var pre = previa(arquivo, gestor);
        assertThat(pre.get("situacao").asString()).isEqualTo("PREVIA");
        assertThat(contar("endereco")).isZero();
        assertThat(pre.get("linhas").get(0).get("endereco").get("posicao").asString())
                .isEqualTo("01");
        var d = confirmacao(pre);
        var d30AntesConfirmar = D30FotografiaFisica.capturar(jdbc);
        var resultado =
                resposta(
                        post(
                                "/api/v1/importacoes-enderecos/"
                                        + pre.get("id").longValue()
                                        + "/confirmacao",
                                d,
                                gestor),
                        200);
        assertThat(resultado.get("enderecos").size()).isEqualTo(2);
        assertThat(resultado.get("enderecos").get(0).get("tipoUnidadePermitido").isNull()).isTrue();
        assertThat(resultado.get("enderecos").get(0).get("capacidadePesoKg").isNull()).isTrue();
        var d30DepoisConfirmar = D30FotografiaFisica.capturar(jdbc);
        for (String t : d30AntesConfirmar.keySet()) {
            if (!java.util.Set.of(
                            "ENDERECO",
                            "IMPORTACAO_ENDERECO",
                            "AUDITORIA_CADASTRO",
                            "OPERACAO_ADMINISTRATIVA")
                    .contains(t))
                assertThat(d30DepoisConfirmar.get(t)).as(t).isEqualTo(d30AntesConfirmar.get(t));
        }
        assertThat(
                        resposta(
                                post(
                                        "/api/v1/importacoes-enderecos/"
                                                + pre.get("id").longValue()
                                                + "/confirmacao",
                                        d,
                                        gestor),
                                200))
                .isEqualTo(resultado);
        assertThat(D30FotografiaFisica.capturar(jdbc)).isEqualTo(d30DepoisConfirmar);
        assertThat(
                        post(
                                        "/api/v1/importacoes-enderecos/"
                                                + pre.get("id").longValue()
                                                + "/confirmacao",
                                        d,
                                        supervisor)
                                .statusCode())
                .isEqualTo(403);
        d.put("motivo", "Outro conteudo divergente");
        assertThat(
                        post(
                                        "/api/v1/importacoes-enderecos/"
                                                + pre.get("id").longValue()
                                                + "/confirmacao",
                                        d,
                                        gestor)
                                .statusCode())
                .isEqualTo(409);
        assertThat(contar("endereco")).isEqualTo(2);
        assertThat(
                        multipart(
                                        arquivo,
                                        Map.of(
                                                "operacaoId",
                                                UUID.randomUUID(),
                                                "motivo",
                                                "Previa sem autenticacao"),
                                        null)
                                .statusCode())
                .isEqualTo(401);
        assertThat(
                        multipart(
                                        arquivo,
                                        Map.of(
                                                "operacaoId",
                                                UUID.randomUUID(),
                                                "motivo",
                                                "Operacao sem autorizacao"),
                                        operador)
                                .statusCode())
                .isEqualTo(403);
        var importacaoConsultada =
                d30GetFotografiaIntegral(
                        "/api/v1/importacoes-enderecos/" + pre.get("id").longValue(), gestor);
        assertThat(resultado.path("enderecos").size()).isEqualTo(2);
        assertThat(importacaoConsultada.path("enderecos").isArray()).isTrue();
        assertThat(importacaoConsultada.path("enderecos").size()).isZero();
        var reciboSemEnderecos = (tools.jackson.databind.node.ObjectNode) resultado.deepCopy();
        var consultaSemEnderecos =
                (tools.jackson.databind.node.ObjectNode) importacaoConsultada.deepCopy();
        reciboSemEnderecos.remove("enderecos");
        consultaSemEnderecos.remove("enderecos");
        assertThat(consultaSemEnderecos).isEqualTo(reciboSemEnderecos);
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "hyperlink",
                "hyperlinkVazio",
                "formula",
                "identidadeNumerica",
                "identidadeFracionadaFormatada",
                "inteiroFracionario",
                "duplicada",
                "tipoInvalido",
                "capacidadeIncompleta"
            })
    void errosExcelPorLinhaSemCriacao(String caso) throws Exception {
        var pre = previa(excel(caso, 2), gestor);
        assertThat(pre.get("situacao").asString()).isEqualTo("COM_ERROS");
        assertThat(pre.get("erros").size()).isGreaterThan(0);
        assertThat(pre.get("erros").get(0).get("linha").intValue()).isGreaterThan(1);
        if (caso.startsWith("hyperlink"))
            assertThat(pre.get("erros").get(0).get("codigo").asString())
                    .isEqualTo("HYPERLINK_RECUSADO");
        if (caso.equals("identidadeFracionadaFormatada"))
            assertThat(pre.get("erros").get(0).get("codigo").asString())
                    .isEqualTo("IDENTIDADE_AMBIGUA");
        assertThat(
                        post(
                                        "/api/v1/importacoes-enderecos/"
                                                + pre.get("id").longValue()
                                                + "/confirmacao",
                                        confirmacao(pre),
                                        gestor)
                                .statusCode())
                .isEqualTo(409);
        assertThat(contar("endereco")).isZero();
    }

    @ParameterizedTest
    @ValueSource(
            strings = {"formulaCabecalho", "numeroCabecalho", "abaErrada", "cabecalhoDuplicado"})
    void formatoGlobalInvalidoRetorna400(String caso) throws Exception {
        assertThat(
                        multipart(
                                        excel(caso, 1),
                                        Map.of(
                                                "operacaoId",
                                                UUID.randomUUID(),
                                                "motivo",
                                                "Formato invalido ficticio"),
                                        gestor)
                                .statusCode())
                .isEqualTo(400);
        assertThat(contar("importacao_endereco")).isZero();
        assertThat(contar("endereco")).isZero();
    }

    @Test
    void identidadeNumericaFormatadaPreservaZero() throws Exception {
        var pre = previa(excel("identidadeFormatada", 1), gestor);
        assertThat(pre.get("linhas").get(0).get("endereco").get("posicao").asString())
                .isEqualTo("01");
        assertThat(pre.get("erros").isEmpty()).isTrue();
    }

    @Test
    void previaDetectaExistenteEConfirmacaoRevalida() throws Exception {
        var pre = previa(excel("normal", 2), gestor);
        resposta(post("/api/v1/enderecos", enderecoComando("IMP1", "01"), gestor), 201);
        assertThat(
                        post(
                                        "/api/v1/importacoes-enderecos/"
                                                + pre.get("id").longValue()
                                                + "/confirmacao",
                                        confirmacao(pre),
                                        gestor)
                                .statusCode())
                .isEqualTo(409);
        assertThat(contar("endereco")).isEqualTo(1);
        assertThat(previa(excel("normal", 2), gestor).get("situacao").asString())
                .isEqualTo("COM_ERROS");
    }

    @Test
    void falhaTardiaAuditoriaDesfazLoteEResposta() throws Exception {
        var pre = previa(excel("normal", 2), gestor);
        var d = confirmacao(pre);
        var contador = new AtomicInteger();
        doAnswer(
                        inv -> {
                            if (inv.getArgument(0).equals("ENDERECO")
                                    && contador.incrementAndGet() == 2)
                                throw new IllegalStateException("Falha ficticia auditada");
                            return inv.callRealMethod();
                        })
                .when(alvoAuditoria)
                .registrar(any(), any(), any(), any(), any(), any());
        assertThat(
                        post(
                                        "/api/v1/importacoes-enderecos/"
                                                + pre.get("id").longValue()
                                                + "/confirmacao",
                                        d,
                                        gestor)
                                .statusCode())
                .isEqualTo(500);
        assertThat(contar("endereco")).isZero();
        assertThat(contar("operacao_administrativa")).isEqualTo(1);
        assertThat(
                        jdbc.queryForObject(
                                "select situacao from wms.importacao_endereco where id=?",
                                String.class,
                                pre.get("id").longValue()))
                .isEqualTo("PREVIA");
        reset(alvoAuditoria);
        resposta(
                post(
                        "/api/v1/importacoes-enderecos/"
                                + pre.get("id").longValue()
                                + "/confirmacao",
                        d,
                        gestor),
                200);
        assertThat(contar("endereco")).isEqualTo(2);
    }

    @Test
    void confirmacaoVersusCriacaoConcorrenteCompartilhaLock() throws Exception {
        var pre = previa(excel("normal", 2), gestor);
        var dentro = new CountDownLatch(1);
        var liberar = new CountDownLatch(1);
        doAnswer(
                        inv -> {
                            if (inv.getArgument(0).equals("ENDERECO")
                                    && inv.getArgument(3).equals("Confirmacao integral ficticia")) {
                                dentro.countDown();
                                assertThat(liberar.await(10, TimeUnit.SECONDS)).isTrue();
                            }
                            return inv.callRealMethod();
                        })
                .when(alvoAuditoria)
                .registrar(any(), any(), any(), any(), any(), any());
        try (var pool = Executors.newFixedThreadPool(2)) {
            var a =
                    pool.submit(
                            () ->
                                    post(
                                            "/api/v1/importacoes-enderecos/"
                                                    + pre.get("id").longValue()
                                                    + "/confirmacao",
                                            confirmacao(pre),
                                            gestor));
            assertThat(dentro.await(10, TimeUnit.SECONDS)).isTrue();
            var b =
                    pool.submit(
                            () -> post("/api/v1/enderecos", enderecoComando("IMP2", "02"), gestor));
            assertThatThrownBy(() -> b.get(250, TimeUnit.MILLISECONDS))
                    .isInstanceOf(java.util.concurrent.TimeoutException.class);
            liberar.countDown();
            assertThat(a.get(15, TimeUnit.SECONDS).statusCode()).isEqualTo(200);
            assertThat(b.get(15, TimeUnit.SECONDS).statusCode()).isEqualTo(409);
        } finally {
            liberar.countDown();
        }
        assertThat(contar("endereco")).isEqualTo(2);
    }

    @Test
    void complementoFiscalNaoTrocaIdentidadeEReplayExigeGestor() throws Exception {
        var dados = new HashMap<String, Object>();
        dados.put("razaoSocial", "Razao ficticia");
        dados.put("cidade", "  Osasco  ");
        dados.put("uf", "SP");
        dados.put("inscricaoEstadual", "IE-FICTICIA");
        dados.put("logradouro", "Rua ficticia");
        var d = comando();
        d.put("versao", 0);
        d.put("dados", dados);
        var r =
                resposta(
                        post("/api/v1/armazens/" + armazemId + "/complemento-fiscal", d, gestor),
                        200);
        assertThat(r.get("documentoFiscal").asString()).isEqualTo("22222222000122");
        assertThat(r.get("dados").get("cidade").asString()).isEqualTo("Osasco");
        assertThat(
                        resposta(
                                post(
                                        "/api/v1/armazens/" + armazemId + "/complemento-fiscal",
                                        d,
                                        gestor),
                                200))
                .isEqualTo(r);
        assertThat(
                        post("/api/v1/armazens/" + armazemId + "/complemento-fiscal", d, supervisor)
                                .statusCode())
                .isEqualTo(403);
        d.put("operacaoId", UUID.randomUUID());
        d.put("versao", r.get("versao").longValue());
        dados.put("cidade", "   ");
        assertThat(
                        post("/api/v1/armazens/" + armazemId + "/complemento-fiscal", d, gestor)
                                .statusCode())
                .isEqualTo(400);
        assertThat(
                        jdbc.queryForObject(
                                "select cidade from wms.armazem where id=?",
                                String.class,
                                armazemId))
                .isEqualTo("Osasco");
        d.put("documentoFiscal", "999");
        assertThat(
                        post("/api/v1/armazens/" + armazemId + "/complemento-fiscal", d, gestor)
                                .statusCode())
                .isEqualTo(400);
    }

    @Test
    void d30LeiturasFiscaisConservamIdentidadeOpcionaisEContextoSemEfeitos() throws Exception {
        String cliente = "/api/v1/clientes/" + clienteId + "/complemento-fiscal";
        String armazem = "/api/v1/armazens/" + armazemId + "/complemento-fiscal";
        String referencias = "/api/v1/produtos/" + produtoId + "/referencias-fiscais";
        var antes = D30FotografiaFisica.capturar(jdbc);
        var c = resposta(get(cliente, supervisor), 200);
        var a = resposta(get(armazem, supervisor), 200);
        assertThat(c.get("id").longValue()).isEqualTo(clienteId);
        assertThat(c.get("codigo").asString()).isEqualTo("C");
        assertThat(c.get("documentoFiscal").asString()).isEqualTo("11111111000111");
        assertThat(c.get("dados").get("razaoSocial").isNull()).isTrue();
        assertThat(c.get("dados").get("faturamentoEmail").isNull()).isTrue();
        assertThat(a.get("id").longValue()).isEqualTo(armazemId);
        assertThat(a.get("codigo").asString()).isEqualTo("A");
        assertThat(a.get("documentoFiscal").asString()).isEqualTo("22222222000122");
        assertThat(a.get("dados").get("cidade").asString()).isEqualTo("Osasco");
        assertThat(a.get("dados").get("uf").asString()).isEqualTo("SP");
        assertThat(resposta(get(referencias, supervisor), 200).size()).isZero();
        resposta(get("/api/v1/status", supervisor), 200);
        assertThat(D30FotografiaFisica.capturar(jdbc)).isEqualTo(antes);
        var d = comando();
        d.put("versao", c.get("versao").longValue());
        d.put(
                "dados",
                Map.of(
                        "razaoSocial",
                        "Cliente fiscal ficticio D30",
                        "cidade",
                        "Osasco",
                        "uf",
                        "SP",
                        "faturamentoEmail",
                        "ficticio@example.invalid",
                        "faturamentoReferencia",
                        "Referencia manual ficticia D30"));
        var gravado = resposta(post(cliente, d, gestor), 200);
        assertThat(gravado.get("documentoFiscal").asString()).isEqualTo("11111111000111");
        assertThat(gravado.get("codigo").asString()).isEqualTo("C");
        assertThat(gravado.get("dados").get("razaoSocial").asString())
                .isEqualTo("Cliente fiscal ficticio D30");
        assertThat(gravado.get("dados").get("faturamentoEmail").asString())
                .isEqualTo("ficticio@example.invalid");
        var depois = D30FotografiaFisica.capturar(jdbc);
        assertThat(resposta(get(cliente, supervisor), 200)).isEqualTo(gravado);
        resposta(post(cliente, d, supervisor), 403);
        var fora = token("SUPERVISOR", List.of(), List.of());
        resposta(get(cliente, fora), 403);
        resposta(get(armazem, fora), 403);
        resposta(get(referencias, fora), 403);
        assertThat(D30FotografiaFisica.capturar(jdbc)).isEqualTo(depois);
    }

    @Test
    void referenciaFiscalContextualSemParametrosInventados() throws Exception {
        var d = comando();
        d.put("versao", 0);
        d.put("armazemId", armazemId);
        d.put("operacao", "SAIDA");
        d.put("fonte", "Conferencia externa ficticia pendente");
        var r =
                resposta(
                        post("/api/v1/produtos/" + produtoId + "/referencias-fiscais", d, gestor),
                        200);
        assertThat(r.get("cfop").isNull()).isTrue();
        assertThat(r.get("aliquotaIcms").isNull()).isTrue();
        assertThat(
                        resposta(
                                post(
                                        "/api/v1/produtos/" + produtoId + "/referencias-fiscais",
                                        d,
                                        gestor),
                                200))
                .isEqualTo(r);
        d.put("operacaoId", UUID.randomUUID());
        d.put("ncm", "12345678");
        d.put("conferidaPor", "Responsavel ficticio");
        assertThat(
                        post("/api/v1/produtos/" + produtoId + "/referencias-fiscais", d, gestor)
                                .statusCode())
                .isEqualTo(400);
        assertThat(contar("referencia_fiscal_produto")).isEqualTo(1);
        assertThat(
                        get(
                                        "/api/v1/produtos/" + produtoId + "/referencias-fiscais",
                                        token("SUPERVISOR", List.of(999L), List.of(armazemId)))
                                .statusCode())
                .isEqualTo(403);
    }

    @Test
    void d30FiscalConservaCamposPorCallerERecusaContextosIncompletos() throws Exception {
        var dados = new HashMap<String, Object>();
        dados.put("razaoSocial", "Razao fiscal ficticia distinta D30");
        dados.put("inscricaoEstadual", "IE-D30-321");
        dados.put("logradouro", "Rua fiscal ficticia 456");
        dados.put("numeroEndereco", "789-B");
        dados.put("complemento", "Complemento fiscal ficticio");
        dados.put("bairro", "Bairro fiscal ficticio");
        dados.put("cidade", "Cidade fiscal ficticia");
        dados.put("uf", "MG");
        dados.put("cep", "12345678");
        dados.put("pais", "BR");
        dados.put("contatoNome", "Contato fiscal ficticio");
        dados.put("contatoEmail", "contato-d30@example.invalid");
        dados.put("contatoTelefone", "0000-1234");
        dados.put("faturamentoEmail", "faturamento-d30@example.invalid");
        dados.put("faturamentoReferencia", "Referencia faturamento ficticia independente");
        String rc = "/api/v1/clientes/" + clienteId + "/complemento-fiscal";
        String ra = "/api/v1/armazens/" + armazemId + "/complemento-fiscal";
        var dc = comando();
        dc.put("versao", 0);
        dc.put("dados", dados);
        var c = resposta(post(rc, dc, gestor), 200);
        assertThat(c.get("id").longValue()).isEqualTo(clienteId);
        assertThat(c.get("codigo").asString()).isEqualTo("C");
        assertThat(c.get("documentoFiscal").asString()).isEqualTo("11111111000111");
        assertThat(c.get("dados")).isEqualTo(mapper.valueToTree(dados));
        var foto = D30FotografiaFisica.capturar(jdbc);
        assertThat(resposta(get(rc, supervisor), 200)).isEqualTo(c);
        assertThat(resposta(post(rc, dc, gestor), 200)).isEqualTo(c);
        assertThat(D30FotografiaFisica.capturar(jdbc)).isEqualTo(foto);

        var da = new HashMap<>(dados);
        da.remove("faturamentoEmail");
        da.remove("faturamentoReferencia");
        var comandoArmazem = comando();
        comandoArmazem.put("versao", 0);
        comandoArmazem.put("dados", da);
        for (String campo : List.of("cidade", "uf", "faturamentoEmail", "faturamentoReferencia")) {
            var ruim = new HashMap<>(da);
            ruim.put(campo, campo.equals("cidade") || campo.equals("uf") ? null : dados.get(campo));
            var tentativa = new HashMap<>(comandoArmazem);
            tentativa.put("operacaoId", UUID.randomUUID());
            tentativa.put("dados", ruim);
            var p = resposta(post(ra, tentativa, gestor), 400);
            assertThat(p.get("codigo").asString()).isEqualTo("DADOS_INVALIDOS");
            assertThat(p.get("idOperacao").asString()).isNotBlank();
            assertThat(D30FotografiaFisica.capturar(jdbc)).as(campo).isEqualTo(foto);
        }
        da.put("cidade", "  Cidade fiscal ficticia  ");
        var a = resposta(post(ra, comandoArmazem, gestor), 200);
        var esperadoArmazem = new HashMap<>(dados);
        esperadoArmazem.put("faturamentoEmail", null);
        esperadoArmazem.put("faturamentoReferencia", null);
        assertThat(a.get("dados")).isEqualTo(mapper.valueToTree(esperadoArmazem));
        assertThat(a.get("id").longValue()).isEqualTo(armazemId);
        assertThat(a.get("codigo").asString()).isEqualTo("A");
        assertThat(a.get("documentoFiscal").asString()).isEqualTo("22222222000122");
        foto = D30FotografiaFisica.capturar(jdbc);
        assertThat(resposta(get(ra, supervisor), 200)).isEqualTo(a);
        assertThat(resposta(post(ra, comandoArmazem, gestor), 200)).isEqualTo(a);
        assertThat(D30FotografiaFisica.capturar(jdbc)).isEqualTo(foto);

        String rr = "/api/v1/produtos/" + produtoId + "/referencias-fiscais";
        var referencia = comando();
        referencia.put("versao", 0);
        referencia.put("armazemId", armazemId);
        referencia.put("operacao", "SAIDA-D30");
        referencia.put("fonte", "Fonte manual fiscal ficticia D30");
        for (int ramo = 0; ramo < 2; ramo++) {
            var ruim = new HashMap<>(referencia);
            ruim.put("operacaoId", UUID.randomUUID());
            ruim.put(
                    "conferidaEm",
                    ramo == 0 ? "2026-10-06T12:00:00Z" : "2026-10-06T12:00:00.000001Z");
            if (ramo == 1) ruim.put("conferidaPor", "Conferente fiscal ficticio D30");
            var p = resposta(post(rr, ruim, gestor), 400);
            assertThat(p.get("codigo").asString()).isEqualTo("DADOS_INVALIDOS");
            assertThat(p.get("idOperacao").asString()).isNotBlank();
            assertThat(D30FotografiaFisica.capturar(jdbc)).isEqualTo(foto);
        }
        referencia.put("ncm", "12345678");
        referencia.put("cfop", "1234");
        referencia.put("cest", "1234567");
        referencia.put("enquadramento", "Enquadramento ficticio D30");
        referencia.put("conferidaPor", "Conferente fiscal ficticio D30");
        referencia.put("conferidaEm", "2026-10-06T11:59:59.123456Z");
        referencia.put("aliquotaIcms", new BigDecimal("0.000001"));
        referencia.put("aliquotaIpi", new BigDecimal("100.000000"));
        var r = resposta(post(rr, referencia, gestor), 200);
        assertThat(r.get("produtoId").longValue()).isEqualTo(produtoId);
        assertThat(r.get("armazemId").longValue()).isEqualTo(armazemId);
        assertThat(r.get("operacao").asString()).isEqualTo("SAIDA-D30");
        for (String campo :
                List.of("ncm", "cfop", "cest", "enquadramento", "fonte", "conferidaPor"))
            assertThat(r.get(campo).asString()).as(campo).isEqualTo(referencia.get(campo));
        assertThat(r.get("aliquotaIcms").decimalValue()).isEqualByComparingTo("0.000001");
        assertThat(r.get("aliquotaIpi").decimalValue()).isEqualByComparingTo("100.000000");
        assertThat(
                        jdbc.queryForObject(
                                        "select conferida_em from wms.referencia_fiscal_produto where id=?",
                                        java.sql.Timestamp.class,
                                        r.get("id").longValue())
                                .toLocalDateTime())
                .isEqualTo(java.time.LocalDateTime.parse("2026-10-06T11:59:59.123456"));
        foto = D30FotografiaFisica.capturar(jdbc);
        assertThat(resposta(get(rr, supervisor), 200).get(0)).isEqualTo(r);
        assertThat(resposta(post(rr, referencia, gestor), 200)).isEqualTo(r);
        assertThat(D30FotografiaFisica.capturar(jdbc)).isEqualTo(foto);
        var zero = new HashMap<>(referencia);
        zero.put("operacaoId", UUID.randomUUID());
        zero.put("versao", r.get("versao").longValue());
        zero.put("aliquotaIcms", BigDecimal.ZERO);
        zero.put("aliquotaIpi", BigDecimal.ZERO);
        var z = resposta(post(rr, zero, gestor), 200);
        assertThat(z.get("id").longValue()).isEqualTo(r.get("id").longValue());
        assertThat(z.get("aliquotaIcms").decimalValue()).isEqualByComparingTo("0");
        assertThat(z.get("aliquotaIpi").decimalValue()).isEqualByComparingTo("0");
        foto = D30FotografiaFisica.capturar(jdbc);
        assertThat(resposta(post(rr, referencia, gestor), 200)).isEqualTo(r);
        var zAtual = resposta(get(rr, supervisor), 200).get(0);
        for (String aliquota : List.of("aliquotaIcms", "aliquotaIpi")) {
            assertThat(z.get(aliquota).decimalValue()).isEqualByComparingTo("0");
            assertThat(zAtual.get(aliquota).decimalValue()).isEqualByComparingTo("0");
        }
        var zOriginalSemAliquotas = (tools.jackson.databind.node.ObjectNode) z.deepCopy();
        var zAtualSemAliquotas = (tools.jackson.databind.node.ObjectNode) zAtual.deepCopy();
        for (String aliquota : List.of("aliquotaIcms", "aliquotaIpi")) {
            zOriginalSemAliquotas.remove(aliquota);
            zAtualSemAliquotas.remove(aliquota);
        }
        assertThat(zAtualSemAliquotas).isEqualTo(zOriginalSemAliquotas);
        assertThat(D30FotografiaFisica.capturar(jdbc)).isEqualTo(foto);
    }

    @Test
    void servicosValidamDiretamentePerfilMesmoEmReplay() {
        SecurityContextHolder.getContext()
                .setAuthentication(
                        new JwtAuthenticationToken(
                                Jwt.withTokenValue("direto")
                                        .header("alg", "RS256")
                                        .subject("operacao")
                                        .claim("wms_perfil", "OPERACAO")
                                        .claim("wms_clientes", List.of(clienteId.toString()))
                                        .claim("wms_armazens", List.of(armazemId.toString()))
                                        .build()));
        assertThatThrownBy(
                        () ->
                                importacao.previa(
                                        armazemId,
                                        new byte[] {1},
                                        new ImportacaoEnderecoDto.Previa(
                                                UUID.randomUUID(), "Previa ficticia")))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(
                        () ->
                                fiscal.complementarArmazem(
                                        armazemId,
                                        new FiscalCadastroDto.Complementar(
                                                UUID.randomUUID(),
                                                0L,
                                                new FiscalCadastroDto.Dados(
                                                        null, null, null, null, null, null,
                                                        "Osasco", "SP", null, null, null, null,
                                                        null, null, null),
                                                "Dados ficticios")))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void configuracaoAusenteExplicitaEFinanceiroProtegido() throws Exception {
        var d = calcularComando("2026-09-01", "2026-09-03");
        var r = resposta(post("/api/v1/calculos-cobranca", d, supervisor), 200);
        assertThat(r.get("situacao").asString()).isEqualTo("PENDENTE");
        assertThat(r.get("total").isNull()).isTrue();
        assertThat(r.get("pendencias").toString()).contains("CONTRATO_AUSENTE");
        assertThat(post("/api/v1/calculos-cobranca", d, operador).statusCode()).isEqualTo(403);
        assertThat(
                        post(
                                        "/api/v1/calculos-cobranca",
                                        d,
                                        token("SUPERVISOR", List.of(999L), List.of(armazemId)))
                                .statusCode())
                .isEqualTo(403);
        assertThat(resposta(post("/api/v1/calculos-cobranca", d, supervisor), 200)).isEqualTo(r);
    }

    @Test
    void tabelasContiguasTarifaExecutadaEPreservacao() throws Exception {
        var storage = servico("ARMAZENAGEM", "POSICAO_DIA");
        var entrada = servico("ENTRADA", "UNIDADE_LOGISTICA");
        var t1 =
                tabela(
                        "T1",
                        "2026-09-01",
                        "2026-09-03",
                        List.of(item(storage, "", "1"), item(entrada, "", "5")));
        var t2 =
                tabela(
                        "T2",
                        "2026-09-03",
                        null,
                        List.of(item(storage, "", "3"), item(entrada, "", "99")));
        vincular(t1, "2026-09-01", "2026-09-03");
        vincular(t2, "2026-09-03", null);
        contrato("NAO_APLICAVEL", null, List.of(), "NAO_APLICAVEL", null, null, null);
        var u = unidade("100", TipoUnidadeLogistica.PALLET, "2026-09-01T12:00:00Z", 1);
        var f = fatoEntrada(entrada, u, "MANUAL");
        var original = resposta(post("/api/v1/fatos-servico", f, supervisor), 200);
        var outro = new HashMap<>(f);
        outro.put("operacaoId", UUID.randomUUID());
        outro.put("origem", "SUGESTAO");
        assertThat(post("/api/v1/fatos-servico", outro, supervisor).statusCode()).isEqualTo(409);
        assertThat(resposta(post("/api/v1/fatos-servico", f, supervisor), 200)).isEqualTo(original);
        var calc = calcular("2026-09-01", "2026-09-05");
        assertThat(calc.get("total").decimalValue()).isEqualByComparingTo("13");
        assertThat(calc.get("memoria").get("servicos").get(0).get("preco").decimalValue())
                .isEqualByComparingTo("5");
        assertThat(calc.get("memoria").get("diarias").get(2).get("tarifa").decimalValue())
                .isEqualByComparingTo("3");
        assertThat(
                        post(
                                        "/api/v1/tabelas-cobranca/"
                                                + t2.get("id").longValue()
                                                + "/encerramento",
                                        Map.of(
                                                "operacaoId",
                                                UUID.randomUUID(),
                                                "versao",
                                                0,
                                                "vigenciaFim",
                                                "2026-09-04",
                                                "motivo",
                                                "Encerramento retroativo ficticio"),
                                        gestor)
                                .statusCode())
                .isEqualTo(409);
        assertThat(
                        resposta(
                                get(
                                        "/api/v1/calculos-cobranca/" + calc.get("id").longValue(),
                                        supervisor),
                                200))
                .isEqualTo(calc);
    }

    @Test
    void vinculosConcorrentesNaoSobrepoem() throws Exception {
        var storage = servico("ARMAZENAGEM", "POSICAO_DIA");
        var t = tabela("T1", "2026-09-01", null, List.of(item(storage, "", "1")));
        var a = vinculoComando(t, "2026-09-01", null);
        var b = vinculoComando(t, "2026-09-01", null);
        try (var pool = Executors.newFixedThreadPool(2)) {
            var inicio = new CountDownLatch(1);
            var fa =
                    pool.submit(
                            () -> {
                                inicio.await();
                                return post("/api/v1/vinculos-tabela", a, gestor).statusCode();
                            });
            var fb =
                    pool.submit(
                            () -> {
                                inicio.await();
                                return post("/api/v1/vinculos-tabela", b, gestor).statusCode();
                            });
            inicio.countDown();
            assertThat(List.of(fa.get(20, TimeUnit.SECONDS), fb.get(20, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(200, 409);
        }
        assertThat(contar("vinculo_tabela_cliente")).isEqualTo(1);
    }

    @Test
    void picoSimultaneoDuasPosicoesEPassagemIsenta() throws Exception {
        configBasica("1");
        var primeira = unidade("100", TipoUnidadeLogistica.PALLET, "2026-09-01T12:00:00Z", 2);
        retirar(primeira, "100", "2026-09-03T20:00:00Z");
        unidade("100", TipoUnidadeLogistica.PALLET, "2026-09-03T18:00:00Z", 1);
        var passagem = unidade("100", TipoUnidadeLogistica.PALLET, "2026-09-03T12:00:00Z", 2);
        retirar(passagem, "100", "2026-09-03T15:00:00Z");
        var calc = calcular("2026-09-01", "2026-09-05");
        assertThat(calc.get("total").decimalValue()).isEqualByComparingTo("6");
        var ds = calc.get("memoria").get("diarias");
        assertThat(ds.get(0).get("picoCobravel").decimalValue()).isEqualByComparingTo("2");
        assertThat(ds.get(2).get("picoCobravel").decimalValue()).isEqualByComparingTo("1");
        assertThat(ds.get(2).get("segmentos").toString()).doesNotContain(passagem.codigo());
        assertThat(ds.get(2).get("intervalosValor").toString()).contains(passagem.codigo());
    }

    @Test
    void picoPorCategoriaSemReplicarPosicao() throws Exception {
        var s = servico("ARMAZENAGEM", "POSICAO_DIA");
        var t =
                tabela(
                        "T",
                        "2026-09-01",
                        null,
                        List.of(item(s, "PALLET", "1"), item(s, "BOBINA", "3")));
        vincular(t, "2026-09-01", null);
        contrato("NAO_APLICAVEL", null, List.of(), "NAO_APLICAVEL", null, null, null);
        unidade("100", TipoUnidadeLogistica.PALLET, "2026-09-01T12:00:00Z", 1);
        unidade("100", TipoUnidadeLogistica.BOBINA, "2026-09-01T18:00:00Z", 2);
        var calc = calcular("2026-09-01", "2026-09-02");
        var dia = calc.get("memoria").get("diarias").get(0);
        assertThat(calc.get("total").decimalValue()).isEqualByComparingTo("7");
        assertThat(dia.get("picoCobravel").decimalValue()).isEqualByComparingTo("3");
        assertThat(dia.get("regras").size()).isEqualTo(2);
        assertThat(dia.get("itemTabelaId").isNull()).isTrue();
        assertThat(dia.get("tarifa").isNull()).isTrue();
    }

    @Test
    void separacaoQuarentenaEParcialConservamEquivalencia() throws Exception {
        configBasica("1");
        var u = unidade("100", TipoUnidadeLogistica.PALLET, "2026-09-01T12:00:00Z", 1);
        retirar(u, "50", "2026-09-03T12:00:00Z");
        new TransactionTemplate(transactions)
                .executeWithoutResult(
                        tx -> {
                            var entidade = em.find(UnidadeLogistica.class, u.id());
                            entidade.posicionar(
                                    null,
                                    TipoEndereco.SEPARACAO,
                                    null,
                                    Instant.parse("2026-09-02T12:00:00Z"));
                            entidade.posicionar(
                                    null,
                                    TipoEndereco.QUARENTENA,
                                    null,
                                    Instant.parse("2026-09-04T12:00:00Z"));
                        });
        var calc = calcular("2026-09-01", "2026-09-05");
        assertThat(calc.get("total").decimalValue()).isEqualByComparingTo("4");
        assertThat(calc.get("memoria").get("diarias").get(3).get("picoCobravel").decimalValue())
                .isEqualByComparingTo("1");
        assertThat(calc.get("memoria").get("diarias").get(3).get("valorEstoque").decimalValue())
                .isEqualByComparingTo("500");
    }

    @ParameterizedTest
    @ValueSource(strings = {"DIARIA", "POR_CICLO"})
    void minimoEGrisDezDiasDeTrinta(String periodicidade) throws Exception {
        var s = servico("ARMAZENAGEM", "POSICAO_DIA");
        var t = tabela("T", "2026-09-01", null, List.of(item(s, "", "1")));
        vincular(t, "2026-09-01", null);
        contrato(
                "APLICAVEL",
                "90",
                List.of(s.get("id").longValue()),
                "APLICAVEL",
                "3",
                periodicidade,
                "PROPORCIONAL_DIAS");
        unidade("100", TipoUnidadeLogistica.PALLET, "2026-09-01T12:00:00Z", 1);
        var c = calcular("2026-09-01", "2026-09-11");
        assertThat(c.get("minimoCalculado").decimalValue()).isEqualByComparingTo("20");
        assertThat(c.get("grisCalculado").decimalValue())
                .isEqualByComparingTo(periodicidade.equals("DIARIA") ? "300" : "10");
        assertThat(c.get("total").decimalValue())
                .isEqualByComparingTo(periodicidade.equals("DIARIA") ? "330" : "40");
        assertThat(c.get("memoria").get("ajustes").get("diasIncluidos").intValue()).isEqualTo(10);
        assertThat(c.get("memoria").get("ajustes").get("diasNominais").intValue()).isEqualTo(30);
    }

    @Test
    void avariaAntesEDepoisParcialExigeMarcoEConservaSnapshot() throws Exception {
        configBasica("1");
        var u = unidade("100", TipoUnidadeLogistica.PALLET, "2026-09-01T12:00:00Z", 1);
        long evento = retirar(u, "50", "2026-09-05T12:00:00Z");
        long a = avaria(u, "20", "100", "2026-09-02T00:00:00Z", null);
        var antes = calcular("2026-09-01", "2026-09-07");
        assertThat(antes.get("total").isNull()).isTrue();
        assertThat(antes.get("pendencias").toString()).contains("DESTINO_AVARIA_INSUFICIENTE");
        var d = comando();
        d.put("fatoPermanenciaId", evento);
        d.put("quantidadeAfetada", "20");
        resposta(post("/api/v1/avarias/" + a + "/marcos-financeiros", d, gestor), 200);
        assertThat(post("/api/v1/avarias/" + a + "/marcos-financeiros", d, supervisor).statusCode())
                .isEqualTo(403);
        var depois = calcular("2026-09-01", "2026-09-07");
        assertThat(depois.get("total").decimalValue()).isEqualByComparingTo("4.8");
        var ds = depois.get("memoria").get("diarias");
        assertThat(ds.get(1).get("equivalenciaSuspensa").decimalValue())
                .isEqualByComparingTo("0.2");
        assertThat(ds.get(5).get("equivalenciaSuspensa").decimalValue())
                .isEqualByComparingTo("0.4");
        assertThat(
                        resposta(
                                get(
                                        "/api/v1/calculos-cobranca/" + antes.get("id").longValue(),
                                        supervisor),
                                200))
                .isEqualTo(antes);
        assertThat(
                        jdbc.queryForObject(
                                "select quantidade from wms.unidade_logistica where id=?",
                                BigDecimal.class,
                                u.id()))
                .isEqualByComparingTo("50");
        assertThat(
                        d30GetFotografiaIntegral(
                                        "/api/v1/avarias/" + a + "/marcos-financeiros", gestor)
                                .size())
                .isPositive();
        assertThat(
                        d30GetFotografiaIntegral(
                                        "/api/v1/calculos-cobranca?clienteId="
                                                + clienteId
                                                + "&armazemId="
                                                + armazemId,
                                        supervisor)
                                .get("totalItens")
                                .longValue())
                .isPositive();
    }

    @Test
    void avariasRetroativasSobrepostasPendentesSemClamp() throws Exception {
        configBasica("1");
        var u = unidade("100", TipoUnidadeLogistica.PALLET, "2026-09-01T12:00:00Z", 1);
        avaria(u, "80", "100", "2026-09-02T00:00:00Z", "2026-09-04T00:00:00Z");
        var anterior = calcular("2026-09-01", "2026-09-06");
        assertThat(anterior.get("total").decimalValue()).isEqualByComparingTo("3.4");
        avaria(u, "80", "100", "2026-09-03T00:00:00Z", null);
        var nova = calcular("2026-09-01", "2026-09-06");
        assertThat(nova.get("total").isNull()).isTrue();
        assertThat(nova.get("pendencias").toString()).contains("AVARIAS_HISTORICAS_INCONSISTENTES");
        var sobreposto =
                nova.get("memoria")
                        .get("diarias")
                        .get(2)
                        .get("segmentos")
                        .get(0)
                        .get("contribuicoes")
                        .get(0);
        assertThat(sobreposto.get("quantidadeAvariada").decimalValue()).isEqualByComparingTo("160");
        assertThat(sobreposto.get("equivalenciaLiquida").isNull()).isTrue();
        assertThat(
                        resposta(
                                get(
                                        "/api/v1/calculos-cobranca/"
                                                + anterior.get("id").longValue(),
                                        supervisor),
                                200))
                .isEqualTo(anterior);
    }

    @Test
    void reparoNaFronteiraNaoSobrepoeDoisDanos() throws Exception {
        configBasica("1");
        var u = unidade("100", TipoUnidadeLogistica.PALLET, "2026-09-01T12:00:00Z", 1);
        avaria(u, "80", "100", "2026-09-02T00:00:00Z", "2026-09-03T00:00:00Z");
        avaria(u, "80", "100", "2026-09-03T00:00:00Z", "2026-09-04T00:00:00Z");
        var c = calcular("2026-09-01", "2026-09-05");
        assertThat(c.get("situacao").asString()).isEqualTo("COMPLETO");
        assertThat(c.get("total").decimalValue()).isEqualByComparingTo("2.4");
    }

    @Test
    void falhaAuditoriaDesfazMemoriaCalculoEIdempotencia() throws Exception {
        configBasica("1");
        unidade("100", TipoUnidadeLogistica.PALLET, "2026-09-01T12:00:00Z", 1);
        var d = calcularComando("2026-09-01", "2026-09-03");
        int operacoes = contar("operacao_administrativa");
        doAnswer(
                        inv -> {
                            if (inv.getArgument(0).equals("CALCULO_COBRANCA"))
                                throw new IllegalStateException("Falha ficticia de auditoria");
                            return inv.callRealMethod();
                        })
                .when(alvoAuditoria)
                .registrar(any(), any(), any(), any(), any(), any());
        assertThat(post("/api/v1/calculos-cobranca", d, supervisor).statusCode()).isEqualTo(500);
        assertThat(contar("calculo_cobranca")).isZero();
        assertThat(contar("memoria_diaria")).isZero();
        assertThat(contar("operacao_administrativa")).isEqualTo(operacoes);
        reset(alvoAuditoria);
        resposta(post("/api/v1/calculos-cobranca", d, supervisor), 200);
        assertThat(contar("calculo_cobranca")).isEqualTo(1);
    }

    @Test
    void grisIncluiRetiradaComArmazenagemZero() throws Exception {
        var s = servico("ARMAZENAGEM", "POSICAO_DIA");
        var t = tabela("T", "2026-09-01", null, List.of(item(s, "", "1")));
        vincular(t, "2026-09-01", null);
        contrato("NAO_APLICAVEL", null, List.of(), "APLICAVEL", "1", "DIARIA", "PROPORCIONAL_DIAS");
        var u = unidade("100", TipoUnidadeLogistica.PALLET, "2026-09-03T12:00:00Z", 1);
        retirar(u, "100", "2026-09-03T15:00:00Z");
        var c = calcular("2026-09-03", "2026-09-04");
        var dia = c.get("memoria").get("diarias").get(0);
        assertThat(dia.get("picoCobravel").decimalValue()).isEqualByComparingTo("0");
        assertThat(dia.get("valorEstoque").decimalValue()).isEqualByComparingTo("1000");
        assertThat(dia.get("intervalosValor").size()).isGreaterThan(1);
        assertThat(c.get("grisCalculado").decimalValue()).isEqualByComparingTo("10");
        assertThat(c.get("total").decimalValue()).isEqualByComparingTo("10");
    }

    @ParameterizedTest
    @ValueSource(strings = {"20", "10"})
    void danoComOrigensDePrecosDiferentesNaoEstima(String preco) throws Exception {
        configBasica("1");
        var u = duasOrigens(preco);
        avaria(u, "20", "100", "2026-09-02T00:00:00Z", null);
        var c = calcular("2026-09-02", "2026-09-03");
        var dia = c.get("memoria").get("diarias").get(0);
        assertThat(dia.get("segmentos").get(0).get("contribuicoes").get(0).get("origens").size())
                .isEqualTo(2);
        if (preco.equals("20")) {
            assertThat(c.get("total").isNull()).isTrue();
            assertThat(dia.get("valorEstoque").isNull()).isTrue();
            assertThat(c.get("pendencias").toString()).contains("ORIGEM_AVARIA_VALOR_INSUFICIENTE");
        } else {
            assertThat(c.get("situacao").asString()).isEqualTo("COMPLETO");
            assertThat(dia.get("valorEstoque").decimalValue()).isEqualByComparingTo("800");
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"ORDEM", "FORA_ORDEM", "REMOCAO_EXCESSIVA"})
    void marcosValidamContinuidadeCronologica(String caso) throws Exception {
        var u = unidade("100", TipoUnidadeLogistica.PALLET, "2026-09-01T12:00:00Z", 1);
        long primeiro =
                retirar(u, caso.equals("REMOCAO_EXCESSIVA") ? "10" : "30", "2026-09-03T12:00:00Z");
        long segundo = retirar(u, "30", "2026-09-04T12:00:00Z");
        long a = avaria(u, "20", "100", "2026-09-02T00:00:00Z", null);
        String rota = "/api/v1/avarias/" + a + "/marcos-financeiros";
        if (caso.equals("REMOCAO_EXCESSIVA")) {
            assertThat(post(rota, marco(primeiro, "0"), gestor).statusCode()).isEqualTo(409);
            assertThat(contar("marco_financeiro_avaria")).isZero();
        } else if (caso.equals("FORA_ORDEM")) {
            resposta(post(rota, marco(segundo, "20"), gestor), 200);
            assertThat(post(rota, marco(primeiro, "0"), gestor).statusCode()).isEqualTo(409);
            resposta(post(rota, marco(primeiro, "20"), gestor), 200);
            assertThat(contar("marco_financeiro_avaria")).isEqualTo(2);
        } else {
            resposta(post(rota, marco(primeiro, "0"), gestor), 200);
            assertThat(post(rota, marco(segundo, "20"), gestor).statusCode()).isEqualTo(409);
            assertThat(contar("marco_financeiro_avaria")).isEqualTo(1);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"0.01", "0.05"})
    void adicionalNaoDuplicaPorUnidadeOpcionalEConservaParcelas(String preco) throws Exception {
        var storage = servico("ARMAZENAGEM", "POSICAO_DIA");
        var adicional = servico("ADICIONAL", "VEICULO");
        var t =
                tabela(
                        "T",
                        "2026-09-01",
                        null,
                        List.of(item(storage, "", "0"), item(adicional, "VAN", preco)));
        vincular(t, "2026-09-01", null);
        contrato("NAO_APLICAVEL", null, List.of(), "NAO_APLICAVEL", null, null, null);
        var u = unidade("100", TipoUnidadeLogistica.PALLET, "2026-09-01T12:00:00Z", 1);
        Long nota2 =
                new TransactionTemplate(transactions)
                        .execute(
                                tx -> {
                                    var n =
                                            new NotaEntrada(
                                                    em.find(PedidoEntrada.class, u.pedidoId()),
                                                    "33333333000133",
                                                    1,
                                                    ++numero,
                                                    LocalDate.parse("2026-08-01"),
                                                    null);
                                    em.persist(n);
                                    em.flush();
                                    return n.getId();
                                });
        var d = comando();
        d.put("clienteId", clienteId);
        d.put("armazemId", armazemId);
        d.put("servicoId", adicional.get("id").longValue());
        d.put("origem", "MANUAL");
        d.put("pedidoEntradaId", u.pedidoId());
        d.put("referenciaExecucao", "EXEC-1");
        d.put("executadoEm", "2026-09-02T12:00:00Z");
        d.put("quantidade", "1");
        d.put("categoria", "VAN");
        d.put("criterioRateio", "Notas da execucao ficticia");
        d.put(
                "cotas",
                List.of(
                        Map.of("notaId", u.notaId(), "cota", "1"),
                        Map.of("notaId", nota2, "cota", "1")));
        resposta(post("/api/v1/fatos-servico", d, supervisor), 200);
        d.put("operacaoId", UUID.randomUUID());
        d.put("origem", "SUGESTAO");
        d.put("unidadeId", u.id());
        assertThat(post("/api/v1/fatos-servico", d, supervisor).statusCode()).isEqualTo(409);
        assertThat(contar("fato_servico")).isEqualTo(1);
        var ps =
                calcular("2026-09-01", "2026-09-03")
                        .get("memoria")
                        .get("servicos")
                        .get(0)
                        .get("parcelas");
        assertThat(ps.size()).isEqualTo(2);
        assertThat(ps.get(0).get("valor").decimalValue())
                .isEqualByComparingTo(preco.equals("0.01") ? "0" : "0.02");
        assertThat(ps.get(1).get("valor").decimalValue())
                .isEqualByComparingTo(preco.equals("0.01") ? "0.01" : "0.03");
        assertThat(ps.get(0).get("valor").decimalValue().add(ps.get(1).get("valor").decimalValue()))
                .isEqualByComparingTo(preco);
    }

    @Test
    void saidaPorQuantidadeMantemSugestaoDoOutroSkuComPaginacao() throws Exception {
        var s = servico("SAIDA", "QUANTIDADE_PRODUTO");
        var a = unidade("10", TipoUnidadeLogistica.PALLET, "2026-09-01T12:00:00Z", 1);
        Long produtoA = produtoId, embalagemA = embalagemId;
        new TransactionTemplate(transactions)
                .executeWithoutResult(
                        tx -> {
                            var p =
                                    new Produto(
                                            em.find(Cliente.class, clienteId),
                                            "SKU-B",
                                            "Produto B ficticio",
                                            "UN",
                                            TipoQuantidade.CONTAGEM,
                                            0,
                                            false,
                                            false,
                                            null,
                                            BASE);
                            em.persist(p);
                            var e =
                                    new Embalagem(
                                            p,
                                            "DUN-B",
                                            "Embalagem B ficticia",
                                            BigDecimal.TEN,
                                            BASE);
                            em.persist(e);
                            em.flush();
                            produtoId = p.getId();
                            embalagemId = e.getId();
                        });
        Long produtoB = produtoId;
        Unidade b;
        try {
            b = unidade("20", TipoUnidadeLogistica.PALLET, "2026-09-01T12:00:00Z", 1);
        } finally {
            produtoId = produtoA;
            embalagemId = embalagemA;
        }
        long pedidoSaida = retirarJuntas(List.of(a, b), "2026-09-03T12:00:00Z");
        String rota =
                "/api/v1/fatos-servico/sugestoes?clienteId="
                        + clienteId
                        + "&armazemId="
                        + armazemId
                        + "&servicoId="
                        + s.get("id").longValue()
                        + "&tamanho=1";
        var primeira = resposta(get(rota, supervisor), 200);
        assertThat(primeira.get("totalItens").longValue()).isEqualTo(2);
        assertThat(primeira.get("totalPaginas").intValue()).isEqualTo(2);
        assertThat(primeira.get("itens").get(0).get("produtoId").longValue()).isEqualTo(produtoA);
        assertThat(
                        resposta(get(rota + "&pagina=1", supervisor), 200)
                                .get("itens")
                                .get(0)
                                .get("produtoId")
                                .longValue())
                .isEqualTo(produtoB);
        var d = comando();
        d.put("clienteId", clienteId);
        d.put("armazemId", armazemId);
        d.put("servicoId", s.get("id").longValue());
        d.put("origem", "MANUAL");
        d.put("pedidoSaidaId", pedidoSaida);
        d.put("produtoId", produtoA);
        d.put("categoria", "");
        d.put("cotas", List.of());
        d.put("criterioRateio", "Baixas por SKU ficticio");
        var f = resposta(post("/api/v1/fatos-servico", d, supervisor), 200);
        assertThat(f.get("quantidade").decimalValue()).isEqualByComparingTo("10");
        var restante = resposta(get(rota, supervisor), 200);
        assertThat(restante.get("totalItens").longValue()).isEqualTo(1);
        assertThat(restante.get("itens").get(0).get("produtoId").longValue()).isEqualTo(produtoB);
        assertThat(restante.get("itens").get(0).get("quantidade").decimalValue())
                .isEqualByComparingTo("20");
        d.put("operacaoId", UUID.randomUUID());
        d.put("origem", "SUGESTAO");
        assertThat(post("/api/v1/fatos-servico", d, supervisor).statusCode()).isEqualTo(409);
        d.put("produtoId", produtoB);
        resposta(post("/api/v1/fatos-servico", d, supervisor), 200);
        assertThat(resposta(get(rota, supervisor), 200).get("totalItens").longValue()).isZero();
        var anulacao = comando();
        anulacao.put("versao", 0);
        String anular = "/api/v1/fatos-servico/" + f.get("id").longValue() + "/anulacao";
        resposta(post(anular, anulacao, gestor), 200);
        assertThat(post(anular, anulacao, supervisor).statusCode()).isEqualTo(403);
        assertThat(resposta(get(rota, supervisor), 200).get("totalItens").longValue()).isZero();
        assertThat(contar("fato_servico")).isEqualTo(2);
    }

    private long retirarJuntas(List<Unidade> refs, String instante) {
        return new TransactionTemplate(transactions)
                .execute(
                        tx -> {
                            Instant data = Instant.parse(instante);
                            var ps =
                                    new PedidoSaida(
                                            em.find(Cliente.class, clienteId),
                                            em.find(Armazem.class, armazemId),
                                            "PS" + (++numero),
                                            BASE);
                            em.persist(ps);
                            var retirada = new RetiradaSaida(ps, data, "fixture", "0".repeat(64));
                            em.persist(retirada);
                            for (var ref : refs) {
                                var u = em.find(UnidadeLogistica.class, ref.id());
                                var q = u.getQuantidade();
                                var item = new ItemPedidoSaida(ps, u.getProduto(), q);
                                em.persist(item);
                                var reserva =
                                        new ReservaSaida(
                                                item, u, UUID.randomUUID().toString(), q, BASE);
                                em.persist(reserva);
                                reserva.encerrar(SituacaoReservaSaida.RETIRADA, data);
                                var conteudo =
                                        em.createQuery(
                                                        "select c from ConteudoUnidade c where c.unidade.id=:id",
                                                        ConteudoUnidade.class)
                                                .setParameter("id", u.getId())
                                                .getSingleResult();
                                em.persist(
                                        new BaixaSaida(
                                                retirada, reserva, conteudo.getEntrada(), q));
                                conteudo.alterarQuantidade(BigDecimal.ZERO);
                                u.alterarQuantidade(BigDecimal.ZERO, data);
                                em.persist(
                                        new FatoPermanencia(
                                                u,
                                                UUID.randomUUID().toString(),
                                                "RETIRADA",
                                                data,
                                                data,
                                                q,
                                                BigDecimal.ZERO,
                                                BigDecimal.valueOf(u.getPosicoesEquivalentes()),
                                                BigDecimal.ZERO));
                            }
                            ps.atualizar(SituacaoPedidoSaida.RETIRADO, data);
                            em.flush();
                            return ps.getId();
                        });
    }

    @Test
    void triagemExecutaEntradaSemAnteciparDiariaOuTarifaNova() throws Exception {
        var armazenagem = servico("ARMAZENAGEM", "POSICAO_DIA");
        var entrada = servico("ENTRADA", "UNIDADE_LOGISTICA");
        var t1 =
                tabela(
                        "T1",
                        "2026-09-01",
                        "2026-09-03",
                        List.of(item(armazenagem, "", "1"), item(entrada, "", "5")));
        var t2 =
                tabela(
                        "T2",
                        "2026-09-03",
                        null,
                        List.of(item(armazenagem, "", "2"), item(entrada, "", "99")));
        vincular(t1, "2026-09-01", "2026-09-03");
        vincular(t2, "2026-09-03", null);
        contrato("NAO_APLICAVEL", null, List.of(), "NAO_APLICAVEL", null, null, null);
        var u =
                unidade(
                        "100",
                        TipoUnidadeLogistica.PALLET,
                        "2026-09-01T12:00:00Z",
                        1,
                        TipoEndereco.TRIAGEM);
        new TransactionTemplate(transactions)
                .executeWithoutResult(
                        tx -> {
                            var entidade = em.find(UnidadeLogistica.class, u.id());
                            entidade.posicionar(
                                    entidade.getMedidas(),
                                    TipoEndereco.ARMAZENAGEM,
                                    null,
                                    Instant.parse("2026-09-03T12:00:00Z"));
                        });
        var f =
                resposta(
                        post(
                                "/api/v1/fatos-servico",
                                fatoEntrada(entrada, u, "SUGESTAO"),
                                supervisor),
                        200);
        assertThat(f.get("executadoEm").asString()).isEqualTo("2026-09-01T12:00:00Z");
        var c = calcular("2026-09-01", "2026-09-05");
        assertThat(c.get("total").decimalValue()).isEqualByComparingTo("9");
        var ds = c.get("memoria").get("diarias");
        assertThat(ds.get(0).get("picoCobravel").decimalValue()).isZero();
        assertThat(ds.get(1).get("picoCobravel").decimalValue()).isZero();
        assertThat(ds.get(2).get("tarifa").decimalValue()).isEqualByComparingTo("2");
        assertThat(c.get("memoria").get("servicos").get(0).get("preco").decimalValue())
                .isEqualByComparingTo("5");
        assertThat(ds.get(0).get("valorEstoque").decimalValue()).isEqualByComparingTo("1000");
    }

    @ParameterizedTest
    @ValueSource(strings = {"NAO_INFORMADO", "NAO_APLICAVEL"})
    void zeroConfiguradoNaoSubstituiAusenciaDeMinimoEGris(String modo) throws Exception {
        var s = servico("ARMAZENAGEM", "POSICAO_DIA");
        var t = tabela("ZERO", "2026-09-01", null, List.of(item(s, "", "0")));
        vincular(t, "2026-09-01", null);
        contrato(modo, null, List.of(), modo, null, null, null);
        unidade("100", TipoUnidadeLogistica.PALLET, "2026-09-01T12:00:00Z", 1);
        var c = calcular("2026-09-01", "2026-09-03");
        assertThat(c.get("subtotalConhecido").decimalValue()).isZero();
        if (modo.equals("NAO_INFORMADO")) {
            assertThat(c.get("total").isNull()).isTrue();
            assertThat(c.get("pendencias").toString())
                    .contains("MINIMO_NAO_CONFIGURADO", "GRIS_NAO_CONFIGURADO");
        } else {
            assertThat(c.get("situacao").asString()).isEqualTo("COMPLETO");
            assertThat(c.get("total").decimalValue()).isZero();
        }
    }

    @Test
    void valorFisicoConservadoNaUnitizacaoSemSomarEntradaEUnidades() throws Exception {
        var s = servico("ARMAZENAGEM", "POSICAO_DIA");
        var t = tabela("T", "2026-09-01", null, List.of(item(s, "", "1")));
        vincular(t, "2026-09-01", null);
        contrato("NAO_APLICAVEL", null, List.of(), "APLICAVEL", "1", "DIARIA", "INTEGRAL");
        Long entradaId =
                new TransactionTemplate(transactions)
                        .execute(
                                tx -> {
                                    var pe =
                                            new PedidoEntrada(
                                                    em.find(Cliente.class, clienteId),
                                                    em.find(Armazem.class, armazemId),
                                                    "PE-FISICO",
                                                    BASE);
                                    em.persist(pe);
                                    var nota =
                                            new NotaEntrada(
                                                    pe,
                                                    "33333333000133",
                                                    1,
                                                    ++numero,
                                                    LocalDate.parse("2026-08-01"),
                                                    null);
                                    em.persist(nota);
                                    var item =
                                            new ItemNotaEntrada(
                                                    nota,
                                                    1,
                                                    em.find(Produto.class, produtoId),
                                                    new BigDecimal("100"),
                                                    new BigDecimal("1000"));
                                    em.persist(item);
                                    var ch =
                                            new ChegadaRecebimento(
                                                    pe,
                                                    UUID.randomUUID().toString(),
                                                    "0".repeat(64),
                                                    BASE,
                                                    BASE,
                                                    "fixture",
                                                    "Fisico ficticio comprovado");
                                    em.persist(ch);
                                    var ic =
                                            new ItemChegada(
                                                    ch,
                                                    1,
                                                    item,
                                                    null,
                                                    null,
                                                    new BigDecimal("80"),
                                                    new BigDecimal("20"));
                                    em.persist(ic);
                                    var entrada =
                                            new EntradaConferida(
                                                    ic,
                                                    BASE,
                                                    BASE,
                                                    new BigDecimal("80"),
                                                    new BigDecimal("20"));
                                    em.persist(entrada);
                                    em.flush();
                                    return entrada.getId();
                                });
        var anterior = calcular("2026-09-01", "2026-09-03");
        assertThat(anterior.get("total").decimalValue()).isEqualByComparingTo("16");
        assertThat(
                        anterior.get("memoria")
                                .get("diarias")
                                .get(0)
                                .get("intervalosValor")
                                .get(0)
                                .get("entradasNaoUnitizadas")
                                .get(0)
                                .get("quantidadeAvariada")
                                .decimalValue())
                .isEqualByComparingTo("20");
        new TransactionTemplate(transactions)
                .executeWithoutResult(
                        tx -> {
                            var e = em.find(EntradaConferida.class, entradaId);
                            Instant instante = Instant.parse("2026-09-02T12:00:00Z");
                            for (var condicao :
                                    List.of(CondicaoMercadoria.BOA, CondicaoMercadoria.AVARIADA)) {
                                var q =
                                        new BigDecimal(
                                                condicao == CondicaoMercadoria.BOA ? "80" : "20");
                                var u =
                                        new UnidadeLogistica(
                                                e,
                                                em.find(Embalagem.class, embalagemId),
                                                TipoUnidadeLogistica.PALLET,
                                                condicao,
                                                q,
                                                instante);
                                em.persist(u);
                                em.persist(new ConteudoUnidade(u, e, q));
                            }
                            e.marcarUnitizada(instante);
                        });
        var novo = calcular("2026-09-01", "2026-09-03");
        assertThat(novo.get("total").decimalValue()).isEqualByComparingTo("16");
        var diaria = novo.get("memoria").get("diarias").get(1);
        assertThat(diaria.get("picoCobravel").decimalValue()).isZero();
        assertThat(diaria.get("valorEstoque").decimalValue()).isEqualByComparingTo("800");
        var intervalos = diaria.get("intervalosValor");
        assertThat(intervalos.size()).isEqualTo(2);
        assertThat(intervalos.get(0).get("unidades").size()).isZero();
        assertThat(intervalos.get(0).get("entradasNaoUnitizadas").size()).isEqualTo(1);
        assertThat(intervalos.get(1).get("unidades").size()).isEqualTo(2);
        assertThat(intervalos.get(1).get("entradasNaoUnitizadas").size()).isZero();
        assertThat(intervalos.get(0).get("total").decimalValue()).isEqualByComparingTo("800");
        assertThat(intervalos.get(1).get("total").decimalValue()).isEqualByComparingTo("800");
        assertThat(
                        resposta(
                                get(
                                        "/api/v1/calculos-cobranca/"
                                                + anterior.get("id").longValue(),
                                        supervisor),
                                200))
                .isEqualTo(anterior);
    }

    private Map<String, Object> marco(long evento, String afetada) {
        var d = comando();
        d.put("fatoPermanenciaId", evento);
        d.put("quantidadeAfetada", afetada);
        return d;
    }

    private Unidade duasOrigens(String precoSegunda) {
        return new TransactionTemplate(transactions)
                .execute(
                        tx -> {
                            var pe =
                                    new PedidoEntrada(
                                            em.find(Cliente.class, clienteId),
                                            em.find(Armazem.class, armazemId),
                                            "PE" + (++numero),
                                            BASE);
                            em.persist(pe);
                            var n =
                                    new NotaEntrada(
                                            pe,
                                            "33333333000133",
                                            1,
                                            numero,
                                            LocalDate.parse("2026-08-01"),
                                            null);
                            em.persist(n);
                            var ch =
                                    new ChegadaRecebimento(
                                            pe,
                                            UUID.randomUUID().toString(),
                                            "0".repeat(64),
                                            BASE,
                                            BASE,
                                            "fixture",
                                            "Origens ficticias");
                            em.persist(ch);
                            var es = new ArrayList<EntradaConferida>();
                            for (int seq = 1; seq <= 2; seq++) {
                                var i =
                                        new ItemNotaEntrada(
                                                n,
                                                seq,
                                                em.find(Produto.class, produtoId),
                                                new BigDecimal("50"),
                                                new BigDecimal("50")
                                                        .multiply(
                                                                new BigDecimal(
                                                                        seq == 1
                                                                                ? "10"
                                                                                : precoSegunda)));
                                em.persist(i);
                                var ic =
                                        new ItemChegada(
                                                ch,
                                                seq,
                                                i,
                                                null,
                                                null,
                                                new BigDecimal("50"),
                                                BigDecimal.ZERO);
                                em.persist(ic);
                                var e =
                                        new EntradaConferida(
                                                ic,
                                                BASE,
                                                BASE,
                                                new BigDecimal("50"),
                                                BigDecimal.ZERO);
                                e.marcarUnitizada(BASE);
                                em.persist(e);
                                es.add(e);
                            }
                            var u =
                                    new UnidadeLogistica(
                                            es.getFirst(),
                                            em.find(Embalagem.class, embalagemId),
                                            TipoUnidadeLogistica.PALLET,
                                            CondicaoMercadoria.BOA,
                                            new BigDecimal("100"),
                                            BASE);
                            em.persist(u);
                            for (var e : es)
                                em.persist(new ConteudoUnidade(u, e, new BigDecimal("50")));
                            u.posicionar(
                                    new MedidasUnidade(
                                            new BigDecimal("100"),
                                            BigDecimal.ONE,
                                            BigDecimal.ONE,
                                            BigDecimal.ONE,
                                            1,
                                            1),
                                    TipoEndereco.ARMAZENAGEM,
                                    null,
                                    Instant.parse("2026-09-01T12:00:00Z"));
                            em.flush();
                            return new Unidade(u.getId(), pe.getId(), n.getId(), u.getCodigo());
                        });
    }

    private JsonNode servico(String tipo, String unidade) throws Exception {
        var d = comando();
        d.put("codigo", "S" + (++numero));
        d.put("descricao", "Servico ficticio");
        d.put("tipo", tipo);
        d.put("unidade", unidade);
        return resposta(post("/api/v1/servicos-cobranca", d, gestor), 200);
    }

    private Map<String, Object> item(JsonNode s, String categoria, String preco) {
        return Map.of("servicoId", s.get("id").longValue(), "categoria", categoria, "preco", preco);
    }

    private JsonNode tabela(
            String codigo, String inicio, String fim, List<Map<String, Object>> itens)
            throws Exception {
        var d = comando();
        d.put("armazemId", armazemId);
        d.put("clienteId", clienteId);
        d.put("codigo", codigo);
        d.put("descricao", "Tabela ficticia");
        d.put("tipo", "ESPECIFICA");
        d.put("vigenciaInicio", inicio);
        if (fim != null) d.put("vigenciaFim", fim);
        d.put("itens", itens);
        return resposta(post("/api/v1/tabelas-cobranca", d, gestor), 200);
    }

    private Map<String, Object> vinculoComando(JsonNode t, String inicio, String fim) {
        var d = comando();
        d.put("clienteId", clienteId);
        d.put("armazemId", armazemId);
        d.put("tabelaId", t.get("id").longValue());
        d.put("vigenciaInicio", inicio);
        if (fim != null) d.put("vigenciaFim", fim);
        return d;
    }

    private JsonNode vincular(JsonNode t, String inicio, String fim) throws Exception {
        return resposta(
                post("/api/v1/vinculos-tabela", vinculoComando(t, inicio, fim), gestor), 200);
    }

    private JsonNode contrato(
            String minimo,
            String valor,
            List<Long> servicos,
            String gris,
            String percentual,
            String periodicidade,
            String proporcao)
            throws Exception {
        var d = comando();
        d.put("clienteId", clienteId);
        d.put("armazemId", armazemId);
        d.put("vigenciaInicio", "2026-09-01");
        d.put("fuso", "UTC");
        d.put("moeda", "BRL");
        d.put("modalidadeCiclo", "DIAS_CORRIDOS");
        d.put("duracaoDias", 30);
        d.put("minimoModo", minimo);
        d.put("grisModo", gris);
        d.put("servicosMinimo", servicos);
        if (valor != null) {
            d.put("minimoValor", valor);
            d.put("minimoProporcao", "PROPORCIONAL_DIAS");
        }
        if (percentual != null) {
            d.put("grisPercentual", percentual);
            d.put("grisBase", "VALOR_ESTOQUE_PICO");
            d.put("grisPeriodicidade", periodicidade);
            d.put("grisProporcao", proporcao);
        }
        return resposta(post("/api/v1/contratos-cobranca", d, gestor), 200);
    }

    private void configBasica(String preco) throws Exception {
        var s = servico("ARMAZENAGEM", "POSICAO_DIA");
        var t = tabela("T", "2026-09-01", null, List.of(item(s, "", preco)));
        vincular(t, "2026-09-01", null);
        contrato("NAO_APLICAVEL", null, List.of(), "NAO_APLICAVEL", null, null, null);
    }

    private Map<String, Object> fatoEntrada(JsonNode servico, Unidade u, String origem) {
        var d = comando();
        d.put("clienteId", clienteId);
        d.put("armazemId", armazemId);
        d.put("servicoId", servico.get("id").longValue());
        d.put("origem", origem);
        d.put("unidadeId", u.id());
        d.put("categoria", "");
        d.put("cotas", List.of());
        d.put("criterioRateio", "Origem fisica da unidade");
        return d;
    }

    @Test
    void avariaBe08DatadaMantemReservaOrigensEBloqueioEDeixaFinanceiroPendente() throws Exception {
        configBasica("1");
        var u = unidadeOperacional();
        var criar = comando();
        criar.put("clienteId", clienteId);
        criar.put("armazemId", armazemId);
        criar.put("referencia", "SAIDA-AVARIA");
        criar.put("itens", List.of(Map.of("produtoId", produtoId, "quantidade", "80")));
        var ps = resposta(post("/api/v1/pedidos-saida", criar, operador), 201).get("pedido");
        var reservar = comando();
        reservar.put("versao", ps.get("versao").longValue());
        long pedido = ps.get("id").longValue();
        resposta(post("/api/v1/pedidos-saida/" + pedido + "/reserva", reservar, operador), 200);
        var antes = calcular("2026-09-01", "2026-09-02");
        var dano = bloqueioUnidade(u);
        when(clock.instant()).thenReturn(Instant.parse("2026-09-02T12:00:00Z"));
        var respostaDano = resposta(post(unidadeRota(u, "/avaria"), dano, operador), 200);
        assertThat(resposta(post(unidadeRota(u, "/avaria"), dano, operador), 200))
                .isEqualTo(respostaDano);
        var depois = calcular("2026-09-02", "2026-09-03");
        assertThat(depois.get("situacao").asString()).isEqualTo("PENDENTE");
        assertThat(depois.get("total").isNull()).isTrue();
        var pendencia =
                depois.get("pendencias")
                        .valueStream()
                        .filter(x -> x.get("codigo").asString().equals("AVARIA_BE08_SEM_DETALHE"))
                        .findFirst()
                        .orElseThrow();
        long movimento =
                jdbc.queryForObject(
                        "select id from wms.movimento_estoque where acao='AVARIA_ESTOQUE'",
                        Long.class);
        assertThat(pendencia.get("fatoId").longValue()).isEqualTo(movimento);
        assertThat(pendencia.get("detalhe").asString()).contains(dano.get("operacaoId").toString());
        var intervalos = depois.get("memoria").get("diarias").get(0).get("intervalosValor");
        assertThat(intervalos.get(0).get("total").decimalValue()).isEqualByComparingTo("1000");
        assertThat(intervalos.get(1).get("total").isNull()).isTrue();
        assertThat(intervalos.get(1).get("unidades").get(0).get("quantidadeAvariada").isNull())
                .isTrue();
        assertThat(calcular("2026-09-01", "2026-09-02").get("situacao").asString())
                .isEqualTo("COMPLETO");
        assertThat(
                        resposta(
                                get(
                                        "/api/v1/calculos-cobranca/" + antes.get("id").longValue(),
                                        supervisor),
                                200))
                .isEqualTo(antes);
        var atual = resposta(get("/api/v1/pedidos-saida/" + pedido, operador), 200);
        assertThat(atual.get("podeProsseguir").booleanValue()).isFalse();
        assertThat(atual.get("reservas").get(0).get("quantidade").decimalValue())
                .isEqualByComparingTo("80");
        assertThat(atual.get("reservas").get(0).get("situacao").asString()).isEqualTo("ATIVA");
        resposta(post(unidadeRota(u, "/liberacao"), bloqueioUnidade(u), supervisor), 409);
        assertThat(
                        jdbc.queryForObject(
                                "select quantidade from wms.conteudo_unidade where unidade_id=?",
                                BigDecimal.class,
                                u.id()))
                .isEqualByComparingTo("100");
        assertThat(
                        jdbc.queryForObject(
                                "select nota_id from wms.unidade_logistica where id=?",
                                Long.class,
                                u.id()))
                .isEqualTo(u.notaId());
        assertThat(
                        jdbc.queryForObject(
                                "select bloqueada from wms.unidade_logistica where id=?",
                                Boolean.class,
                                u.id()))
                .isTrue();
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void detalheEReparoPosterioresSoResolvemIntervaloComprovado(boolean cobreMarco)
            throws Exception {
        var s = servico("ARMAZENAGEM", "POSICAO_DIA");
        vincular(tabela("T", "2026-09-01", null, List.of(item(s, "", "1"))), "2026-09-01", null);
        contrato("NAO_APLICAVEL", null, List.of(), "APLICAVEL", "1", "DIARIA", "INTEGRAL");
        var u = unidadeOperacional();
        var origem = enderecoOperacional(TipoEndereco.ARMAZENAGEM);
        var quarentena = enderecoOperacional(TipoEndereco.QUARENTENA);
        when(clock.instant()).thenReturn(Instant.parse("2026-09-02T12:00:00Z"));
        resposta(post(unidadeRota(u, "/avaria"), bloqueioUnidade(u), operador), 200);
        var inicial = calcular("2026-09-02", "2026-09-05");
        assertThat(inicial.get("total").isNull()).isTrue();
        when(clock.instant()).thenReturn(Instant.parse("2026-09-03T12:00:00Z"));
        var dano =
                detalharAvaria(
                        u,
                        cobreMarco ? "2026-09-02T12:00:00Z" : "2026-09-03T12:00:00Z",
                        quarentena);
        var semResponsabilidade = calcular("2026-09-02", "2026-09-05");
        assertThat(semResponsabilidade.get("situacao").asString()).isEqualTo("PENDENTE");
        when(clock.instant()).thenReturn(Instant.parse("2026-09-04T12:00:00Z"));
        repararAvaria(u, dano, origem);
        var aposReparo = calcular("2026-09-02", "2026-09-05");
        assertThat(aposReparo.get("situacao").asString()).isEqualTo("PENDENTE");
        when(clock.instant()).thenReturn(Instant.parse("2026-09-05T12:00:00Z"));
        reconhecerAvaria(u, dano);
        var finalizado = calcular("2026-09-02", "2026-09-05");
        assertThat(finalizado.get("situacao").asString())
                .isEqualTo(cobreMarco ? "COMPLETO" : "PENDENTE");
        if (cobreMarco) {
            assertThat(finalizado.get("grisCalculado").decimalValue()).isEqualByComparingTo("20");
            assertThat(finalizado.get("total").decimalValue()).isEqualByComparingTo("22");
        } else {
            assertThat(finalizado.get("total").isNull()).isTrue();
            assertThat(finalizado.get("pendencias").toString())
                    .contains(
                            "lacuna anterior preservada",
                            dano.get("avaria").get("cicloId").asString());
        }
        assertThat(calcular("2026-09-04", "2026-09-05").get("situacao").asString())
                .isEqualTo("COMPLETO");
        assertThat(
                        resposta(
                                get(
                                        "/api/v1/calculos-cobranca/"
                                                + inicial.get("id").longValue(),
                                        supervisor),
                                200))
                .isEqualTo(inicial);
        assertThat(
                        resposta(
                                get(
                                        "/api/v1/calculos-cobranca/"
                                                + aposReparo.get("id").longValue(),
                                        supervisor),
                                200))
                .isEqualTo(aposReparo);
        assertThat(
                        jdbc.queryForObject(
                                "select avaria_posterior from wms.unidade_logistica where id=?",
                                Boolean.class,
                                u.id()))
                .isFalse();
        assertThat(
                        jdbc.queryForObject(
                                "select count(*) from wms.movimento_estoque where acao='AVARIA_ESTOQUE'",
                                Integer.class))
                .isEqualTo(1);
    }

    @Test
    void cicloReparadoNaoEncobreNovaAvariaBe08() throws Exception {
        configBasica("1");
        var u = unidadeOperacional();
        var origem = enderecoOperacional(TipoEndereco.ARMAZENAGEM);
        var quarentena = enderecoOperacional(TipoEndereco.QUARENTENA);
        when(clock.instant()).thenReturn(Instant.parse("2026-09-01T18:00:00Z"));
        var antigo = detalharAvaria(u, "2026-09-01T12:00:00Z", quarentena);
        reconhecerAvaria(u, antigo);
        when(clock.instant()).thenReturn(Instant.parse("2026-09-02T12:00:00Z"));
        repararAvaria(u, antigo, origem);
        var memoriaAntiga = calcular("2026-09-01", "2026-09-03");
        assertThat(memoriaAntiga.get("situacao").asString()).isEqualTo("COMPLETO");
        when(clock.instant()).thenReturn(Instant.parse("2026-09-03T12:00:00Z"));
        resposta(post(unidadeRota(u, "/avaria"), bloqueioUnidade(u), operador), 200);
        var novaMarca = calcular("2026-09-03", "2026-09-04");
        assertThat(novaMarca.get("situacao").asString()).isEqualTo("PENDENTE");
        assertThat(novaMarca.get("pendencias").toString()).contains("AVARIA_BE08_SEM_DETALHE");
        when(clock.instant()).thenReturn(Instant.parse("2026-09-04T12:00:00Z"));
        var novo = detalharAvaria(u, "2026-09-03T12:00:00Z", quarentena);
        assertThat(novo.get("avaria").get("cicloId").asString())
                .isNotEqualTo(antigo.get("avaria").get("cicloId").asString());
        reconhecerAvaria(u, novo);
        assertThat(calcular("2026-09-03", "2026-09-04").get("situacao").asString())
                .isEqualTo("COMPLETO");
        assertThat(
                        resposta(
                                get(
                                        "/api/v1/calculos-cobranca/"
                                                + novaMarca.get("id").longValue(),
                                        supervisor),
                                200))
                .isEqualTo(novaMarca);
        assertThat(
                        resposta(
                                get(
                                        "/api/v1/calculos-cobranca/"
                                                + memoriaAntiga.get("id").longValue(),
                                        supervisor),
                                200))
                .isEqualTo(memoriaAntiga);
    }

    @Test
    void avariaBe08ImpedeGrisMesmoNoDiaDaRetiradaSemDiaria() throws Exception {
        var s = servico("ARMAZENAGEM", "POSICAO_DIA");
        vincular(tabela("T", "2026-09-01", null, List.of(item(s, "", "1"))), "2026-09-01", null);
        contrato("NAO_APLICAVEL", null, List.of(), "APLICAVEL", "1", "DIARIA", "INTEGRAL");
        var u = unidadeOperacional();
        when(clock.instant()).thenReturn(Instant.parse("2026-09-02T12:00:00Z"));
        resposta(post(unidadeRota(u, "/avaria"), bloqueioUnidade(u), operador), 200);
        retirar(
                u,
                "100",
                "2026-09-02T18:00:00Z"); // Ledger comprovado para exercitar o intervalo físico; não
        // autoriza retirada bloqueada.
        var c = calcular("2026-09-02", "2026-09-03");
        assertThat(c.get("situacao").asString()).isEqualTo("PENDENTE");
        assertThat(c.get("total").isNull()).isTrue();
        assertThat(c.get("grisCalculado").isNull()).isTrue();
        assertThat(c.get("memoria").get("diarias").get(0).get("picoCobravel").decimalValue())
                .isZero();
        assertThat(c.get("pendencias").toString()).contains("AVARIA_BE08_SEM_DETALHE");
    }

    @Test
    void falhaAuditoriaDaMarcaBe08FazRollbackDoHistoricoEBloqueio() throws Exception {
        configBasica("1");
        var u = unidadeOperacional();
        when(clock.instant()).thenReturn(Instant.parse("2026-09-02T12:00:00Z"));
        var d = bloqueioUnidade(u);
        doAnswer(
                        invocacao -> {
                            if ("AVARIA_ESTOQUE".equals(invocacao.getArgument(2)))
                                throw new IllegalStateException(
                                        "Falha ficticia na auditoria da avaria legada");
                            return invocacao.callRealMethod();
                        })
                .when(alvoAuditoria)
                .registrar(any(), any(), any(), any(), any(), any());
        resposta(post(unidadeRota(u, "/avaria"), d, operador), 500);
        assertThat(contar("movimento_estoque")).isZero();
        assertThat(
                        jdbc.queryForObject(
                                "select avaria_posterior from wms.unidade_logistica where id=?",
                                Boolean.class,
                                u.id()))
                .isFalse();
        assertThat(
                        jdbc.queryForObject(
                                "select bloqueada from wms.unidade_logistica where id=?",
                                Boolean.class,
                                u.id()))
                .isFalse();
        assertThat(calcular("2026-09-02", "2026-09-03").get("situacao").asString())
                .isEqualTo("COMPLETO");
        reset(alvoAuditoria);
        resposta(post(unidadeRota(u, "/avaria"), d, operador), 200);
        assertThat(contar("movimento_estoque")).isEqualTo(1);
        assertThat(calcular("2026-09-02", "2026-09-03").get("situacao").asString())
                .isEqualTo("PENDENTE");
    }

    private Unidade unidadeOperacional() {
        var u = unidade("100", TipoUnidadeLogistica.PALLET, "2026-09-01T12:00:00Z", 1);
        long endereco = enderecoOperacional(TipoEndereco.ARMAZENAGEM);
        new TransactionTemplate(transactions)
                .executeWithoutResult(
                        tx -> {
                            em.find(PedidoEntrada.class, u.pedidoId())
                                    .concluir("Entrada ficticia conferida", BASE);
                            var o = new OcupacaoEndereco(em.find(Endereco.class, endereco));
                            o.atribuir(em.find(UnidadeLogistica.class, u.id()));
                            em.persist(o);
                        });
        return u;
    }

    private long enderecoOperacional(TipoEndereco tipo) {
        return new TransactionTemplate(transactions)
                .execute(
                        tx -> {
                            String codigo = "E" + (++numero);
                            var e =
                                    new Endereco(
                                            em.find(Armazem.class, armazemId),
                                            codigo,
                                            codigo,
                                            0,
                                            "01",
                                            "Destino ficticio",
                                            tipo,
                                            new BigDecimal("1000"),
                                            BigDecimal.TEN,
                                            BigDecimal.TEN,
                                            BigDecimal.TEN,
                                            10,
                                            1,
                                            BASE);
                            e.configurarFisico(
                                    TipoUnidadeLogistica.PALLET,
                                    new BigDecimal("1000"),
                                    BigDecimal.TEN,
                                    BigDecimal.TEN,
                                    BigDecimal.TEN,
                                    10,
                                    BASE);
                            em.persist(e);
                            em.flush();
                            return e.getId();
                        });
    }

    private String unidadeRota(Unidade u, String sufixo) {
        return "/api/v1/unidades-logisticas/" + u.codigo() + sufixo;
    }

    private String avariaRota(Unidade u, String sufixo) {
        return "/api/v1/estoque/unidades/" + u.codigo() + "/avarias" + sufixo;
    }

    private Map<String, Object> bloqueioUnidade(Unidade u) {
        var d = comando();
        d.put(
                "versaoUnidade",
                jdbc.queryForObject(
                        "select versao from wms.unidade_logistica where id=?", Long.class, u.id()));
        return d;
    }

    private List<Map<String, Object>> destino(long id) {
        return List.of(
                Map.of(
                        "enderecoId",
                        id,
                        "codigoLido",
                        jdbc.queryForObject(
                                "select codigo from wms.endereco where id=?", String.class, id)));
    }

    private JsonNode detalharAvaria(Unidade u, String ocorrida, long destino) throws Exception {
        var d = bloqueioUnidade(u);
        d.put("quantidade", "100");
        d.put("ocorridaEm", ocorrida);
        d.put("destinos", destino(destino));
        return resposta(post(avariaRota(u, ""), d, supervisor), 200);
    }

    private void reconhecerAvaria(Unidade u, JsonNode dano) throws Exception {
        long id = dano.get("avaria").get("id").longValue();
        var d = comando();
        d.put(
                "versao",
                jdbc.queryForObject(
                        "select versao from wms.avaria_estoque where id=?", Long.class, id));
        d.put("responsabilidade", "RODOGARCIA");
        resposta(post(avariaRota(u, "/" + id + "/responsabilidade"), d, gestor), 200);
    }

    private void repararAvaria(Unidade u, JsonNode dano, long destino) throws Exception {
        long id = dano.get("avaria").get("id").longValue();
        var d = bloqueioUnidade(u);
        d.put(
                "versao",
                jdbc.queryForObject(
                        "select versao from wms.avaria_estoque where id=?", Long.class, id));
        d.put("destinos", destino(destino));
        resposta(post(avariaRota(u, "/" + id + "/reparo"), d, supervisor), 200);
    }

    @Test
    void d26HistoricoAnteriorATransformacaoPersistePendenciaSemRollbackInesperado()
            throws Exception {
        configBasica("1");
        var u = unidade("100", TipoUnidadeLogistica.PALLET, "2026-09-01T12:00:00Z", 1);
        // Snapshot BE07 isolado: a identidade existe antes do último reagrupamento comprovado.
        new TransactionTemplate(transactions)
                .executeWithoutResult(
                        tx -> {
                            var entidade = em.find(UnidadeLogistica.class, u.id());
                            var snapshot =
                                    new br.com.rodogarcia.wms.dto.UnidadeLogisticaDto.Resultado(
                                            UUID.randomUUID(),
                                            u.pedidoId(),
                                            0,
                                            List.of(
                                                    new br.com.rodogarcia.wms.dto
                                                            .UnidadeLogisticaDto.Detalhe(
                                                            br.com.rodogarcia.wms.dto
                                                                    .UnidadeLogisticaDto.Resumo.de(
                                                                    entidade, false),
                                                            List.of())));
                            em.persist(
                                    new br.com.rodogarcia.wms.models.OperacaoUnidade(
                                            entidade.getPedido(),
                                            UUID.randomUUID().toString(),
                                            "0".repeat(64),
                                            "UNIDADES_REAGRUPADAS",
                                            "D26 ficticio",
                                            Instant.parse("2026-09-01T13:00:00Z"),
                                            mapper.writeValueAsString(snapshot)));
                        });
        var comando = calcularComando("2026-09-01", "2026-09-02");
        var r = resposta(post("/api/v1/calculos-cobranca", comando, supervisor), 200);
        assertThat(r.get("situacao").asString()).isEqualTo("PENDENTE");
        assertThat(r.get("total").isNull()).isTrue();
        assertThat(r.get("pendencias").toString()).contains("HISTORICO_QUANTIDADE_INSUFICIENTE");
        assertThat(contar("calculo_cobranca")).isEqualTo(1);
        assertThat(resposta(post("/api/v1/calculos-cobranca", comando, supervisor), 200))
                .isEqualTo(r);
        assertThat(contar("calculo_cobranca")).isEqualTo(1);
    }

    @Test
    void d30AdicionalAssociaSaidaFuturaSemNovaExecucao() throws Exception {
        var storage = servico("ARMAZENAGEM", "POSICAO_DIA");
        var adicional = servico("ADICIONAL", "VEICULO");
        vincular(
                tabela(
                        "D30-T",
                        "2026-09-01",
                        null,
                        List.of(item(storage, "", "0"), item(adicional, "VAN", "5"))),
                "2026-09-01",
                null);
        contrato("NAO_APLICAVEL", null, List.of(), "NAO_APLICAVEL", null, null, null);
        var u = unidadeOperacional();
        var inicial = comando();
        inicial.put("clienteId", clienteId);
        inicial.put("armazemId", armazemId);
        inicial.put("servicoId", adicional.get("id").longValue());
        inicial.put("origem", "MANUAL");
        inicial.put("unidadeId", u.id());
        inicial.put("pedidoEntradaId", u.pedidoId());
        inicial.put("referenciaExecucao", "D30-EXEC-FUTURA");
        inicial.put("executadoEm", "2026-09-02T12:00:00Z");
        inicial.put("quantidade", "1");
        inicial.put("categoria", "VAN");
        inicial.put("criterioRateio", "Nota original da execucao D30");
        inicial.put("cotas", List.of(Map.of("notaId", u.notaId(), "cota", "1")));
        var original = resposta(post("/api/v1/fatos-servico", inicial, supervisor), 200);
        assertThat(original.get("pedidoSaidaId").isNull()).isTrue();
        var calculoOriginal = calcular("2026-09-01", "2026-09-03");
        assertThat(calculoOriginal.get("total").decimalValue()).isEqualByComparingTo("5");
        long saida = d30CriarSaidaReal(armazemId, "D30-FUTURA-1");
        long segunda = d30CriarSaidaReal(armazemId, "D30-FUTURA-2");
        var outroArmazem =
                resposta(
                        post(
                                "/api/v1/armazens",
                                Map.of(
                                        "codigo",
                                        "D30-ARM-OUTRO",
                                        "nome",
                                        "Outro armazem ficticio",
                                        "documentoFiscal",
                                        "00000000000001",
                                        "cidade",
                                        "Cidade ficticia",
                                        "uf",
                                        "SP"),
                                gestor),
                        201);
        long armazemOriginal = armazemId;
        try {
            armazemId = outroArmazem.get("id").longValue();
            unidadeOperacional();
        } finally {
            armazemId = armazemOriginal;
        }
        long fora = d30CriarSaidaReal(outroArmazem.get("id").longValue(), "D30-FUTURA-FORA");
        var vincular = new HashMap<String, Object>(inicial);
        vincular.put("operacaoId", UUID.randomUUID());
        vincular.put("pedidoSaidaId", saida);
        var foto = d30FotoGenericaAssociacao();
        var operacaoOriginal =
                jdbc.queryForMap(
                        "select * from wms.operacao_administrativa where operacao_id=?",
                        inicial.get("operacaoId").toString());
        for (String campo :
                List.of(
                        "pedidoSaidaId",
                        "quantidade",
                        "origem",
                        "categoria",
                        "criterioRateio",
                        "cotas",
                        "unidadeId")) {
            var divergente = new HashMap<String, Object>(vincular);
            divergente.put("operacaoId", UUID.randomUUID());
            divergente.put(
                    campo,
                    switch (campo) {
                        case "pedidoSaidaId" -> fora;
                        case "quantidade" -> "2";
                        case "origem" -> "SUGESTAO";
                        case "categoria" -> "CAMINHAO";
                        case "criterioRateio" -> "Outro criterio divergente";
                        case "cotas" -> List.of(Map.of("notaId", u.notaId(), "cota", "2"));
                        case "unidadeId" -> null;
                        default -> throw new IllegalStateException("Vetor D30 desconhecido");
                    });
            resposta(post("/api/v1/fatos-servico", divergente, supervisor), 409);
            assertThat(d30FotoGenericaAssociacao()).isEqualTo(foto);
        }
        var response = post("/api/v1/fatos-servico", vincular, supervisor);
        var proof = new HashMap<String, Object>();
        proof.put("id", "D30-C-CASO-ASSOCIACAO-FUTURA-001");
        proof.put("HTTP", response.statusCode());
        proof.put("original", original);
        proof.put("resposta", mapper.readTree(response.body()));
        proof.put("antes", foto);
        proof.put("depois", d30FotoGenericaAssociacao());
        proof.put("operacaoOriginal", operacaoOriginal);
        proof.put(
                "oraculo",
                "Mesmo fato/execucao/cotas/valor5; somente FKsaida/versao e audit/op novos. JSONJDBCgenerico nao prova UTC/micros; A08 tipado somenteFINAL.");
        proof.put("SQLServer", false);
        var dir = java.nio.file.Path.of(System.getProperty("wms.test.evidencias.dir"));
        java.nio.file.Files.createDirectories(dir);
        java.nio.file.Files.writeString(
                dir.resolve("d30-cedro-associacao-futura-" + UUID.randomUUID() + ".json"),
                mapper.writerWithDefaultPrettyPrinter().writeValueAsString(proof),
                StandardCharsets.UTF_8,
                java.nio.file.StandardOpenOption.CREATE_NEW,
                java.nio.file.StandardOpenOption.WRITE);
        var associado = resposta(response, 200);
        assertThat(associado.get("id").longValue()).isEqualTo(original.get("id").longValue());
        assertThat(associado.get("pedidoSaidaId").longValue()).isEqualTo(saida);
        assertThat(associado.get("versao").longValue())
                .isEqualTo(original.get("versao").longValue() + 1);
        for (String campo :
                List.of(
                        "clienteId",
                        "armazemId",
                        "servicoId",
                        "chaveFato",
                        "origem",
                        "unidadeId",
                        "pedidoEntradaId",
                        "produtoId",
                        "referenciaExecucao",
                        "executadoEm",
                        "quantidade",
                        "categoria",
                        "valorBase",
                        "criterioRateio",
                        "situacao",
                        "cotas"))
            if (List.of("quantidade", "valorBase").contains(campo) && !original.get(campo).isNull())
                assertThat(associado.get(campo).decimalValue())
                        .as(campo)
                        .isEqualByComparingTo(original.get(campo).decimalValue());
            else if (campo.equals("cotas")) {
                assertThat(associado.get(campo).size()).isEqualTo(original.get(campo).size());
                for (int i = 0; i < original.get(campo).size(); i++) {
                    assertThat(associado.get(campo).get(i).get("notaId"))
                            .isEqualTo(original.get(campo).get(i).get("notaId"));
                    assertThat(associado.get(campo).get(i).get("cota").decimalValue())
                            .isEqualByComparingTo(
                                    original.get(campo).get(i).get("cota").decimalValue());
                }
            } else assertThat(associado.get(campo)).as(campo).isEqualTo(original.get(campo));
        var apos = d30FotoGenericaAssociacao();
        var fatoAntes = new HashMap<String, Object>(foto.get("FATO_SERVICO").get(0));
        var fatoDepois = new HashMap<String, Object>(apos.get("FATO_SERVICO").get(0));
        for (String campo : List.of("PEDIDO_SAIDA_ID", "VERSAO")) {
            fatoAntes.remove(campo);
            fatoDepois.remove(campo);
        }
        assertThat(fatoDepois).isEqualTo(fatoAntes);
        for (String tabela : foto.keySet())
            if (!List.of("FATO_SERVICO", "AUDITORIA_CADASTRO", "OPERACAO_ADMINISTRATIVA")
                    .contains(tabela))
                assertThat(apos.get(tabela)).as(tabela).isEqualTo(foto.get(tabela));
        assertThat(contar("fato_servico")).isEqualTo(1);
        assertThat(contar("rateio_fato_servico")).isEqualTo(1);
        assertThat(
                        jdbc.queryForMap(
                                "select * from wms.operacao_administrativa where operacao_id=?",
                                inicial.get("operacaoId").toString()))
                .isEqualTo(operacaoOriginal);
        assertThat(resposta(post("/api/v1/fatos-servico", inicial, supervisor), 200))
                .isEqualTo(original);
        assertThat(resposta(post("/api/v1/fatos-servico", vincular, supervisor), 200))
                .isEqualTo(associado);
        assertThat(d30FotoGenericaAssociacao()).isEqualTo(apos);
        var outro = new HashMap<String, Object>(vincular);
        outro.put("operacaoId", UUID.randomUUID());
        outro.put("pedidoSaidaId", segunda);
        resposta(post("/api/v1/fatos-servico", outro, supervisor), 409);
        assertThat(d30FotoGenericaAssociacao()).isEqualTo(apos);
        outro.put("operacaoId", inicial.get("operacaoId"));
        assertThat(
                        resposta(post("/api/v1/fatos-servico", outro, supervisor), 409)
                                .get("codigo")
                                .asString())
                .isEqualTo("OPERACAO_DIVERGENTE");
        resposta(post("/api/v1/fatos-servico", vincular, operador), 403);
        resposta(
                post("/api/v1/fatos-servico", vincular, token("SUPERVISOR", List.of(), List.of())),
                403);
        assertThat(d30FotoGenericaAssociacao()).isEqualTo(apos);
        var novoCalculo = calcular("2026-09-01", "2026-09-03");
        assertThat(novoCalculo.get("total").decimalValue()).isEqualByComparingTo("5");
        assertThat(novoCalculo.get("memoria").get("servicos").size()).isEqualTo(1);
        assertThat(
                        resposta(
                                get(
                                        "/api/v1/calculos-cobranca/"
                                                + calculoOriginal.get("id").longValue(),
                                        supervisor),
                                200))
                .isEqualTo(calculoOriginal);
    }

    @ParameterizedTest
    @ValueSource(strings = {"AUDITORIA", "OPERACAO"})
    void d30AssociacaoFuturaReverteFalhaTardiaEPermiteRetry(String canal) throws Exception {
        var servico = servico("ADICIONAL", "VEICULO");
        var u = unidadeOperacional();
        var originalComando = d30ComandoAdicional(servico, u, "D30-ROLLBACK");
        var original = resposta(post("/api/v1/fatos-servico", originalComando, supervisor), 200);
        var auditOriginal =
                jdbc.queryForMap(
                        "select * from wms.auditoria_cadastro where tipo='FATO_SERVICO' and registro_id=?",
                        original.get("id").longValue());
        var opOriginal =
                jdbc.queryForMap(
                        "select * from wms.operacao_administrativa where operacao_id=?",
                        originalComando.get("operacaoId").toString());
        var snapshotAntes =
                resposta(
                        get("/api/v1/fatos-servico/" + original.get("id").longValue(), supervisor),
                        200);
        long saida = d30CriarSaidaReal(armazemId, "D30-ROLLBACK-SAIDA");
        var comando = new HashMap<String, Object>(originalComando);
        comando.put("operacaoId", UUID.randomUUID());
        comando.put("pedidoSaidaId", saida);
        var antes = D30FotografiaFisica.capturar(jdbc);
        String tabela =
                canal.equals("AUDITORIA") ? "auditoria_cadastro" : "operacao_administrativa";
        String coluna = canal.equals("AUDITORIA") ? "acao" : "tipo";
        long ultimoIdAntes =
                jdbc.queryForObject("select coalesce(max(id),0) from wms." + tabela, Long.class);
        jdbc.execute(
                "alter table wms."
                        + tabela
                        + " add constraint d30_associacao_tardia check (id <= "
                        + ultimoIdAntes
                        + " OR "
                        + coluna
                        + " <> 'REGISTRO_SERVICO')");
        try {
            resposta(post("/api/v1/fatos-servico", comando, supervisor), 409);
            assertThat(D30FotografiaFisica.capturar(jdbc)).isEqualTo(antes);
        } finally {
            jdbc.execute("alter table wms." + tabela + " drop constraint d30_associacao_tardia");
        }
        var confirmacao = post("/api/v1/fatos-servico", comando, supervisor);
        var associado = resposta(confirmacao, 200);
        assertThat(associado.get("id")).isEqualTo(original.get("id"));
        assertThat(associado.get("pedidoSaidaId").longValue()).isEqualTo(saida);
        assertThat(associado.get("versao").longValue())
                .isEqualTo(original.get("versao").longValue() + 1);
        assertThat(contar("fato_servico")).isEqualTo(1);
        assertThat(contar("rateio_fato_servico")).isEqualTo(1);
        var audits =
                jdbc.queryForList(
                        "select * from wms.auditoria_cadastro where tipo='FATO_SERVICO' and registro_id=? order by id",
                        original.get("id").longValue());
        assertThat(audits).hasSize(2);
        assertThat(audits.getFirst()).isEqualTo(auditOriginal);
        var novoAudit = audits.getLast();
        assertThat(((Number) novoAudit.get("ID")).longValue())
                .isGreaterThan(((Number) auditOriginal.get("ID")).longValue());
        assertThat(novoAudit.get("TIPO")).isEqualTo("FATO_SERVICO");
        assertThat(novoAudit.get("ACAO")).isEqualTo("REGISTRO_SERVICO");
        assertThat(novoAudit.get("USUARIO")).isEqualTo("cobranca-SUPERVISOR");
        assertThat(novoAudit.get("MOTIVO")).isEqualTo(comando.get("motivo"));
        assertThat(novoAudit.get("ID_OPERACAO"))
                .isEqualTo(confirmacao.headers().firstValue("X-Request-Id").orElseThrow());
        assertThat(mapper.readTree((String) novoAudit.get("DADOS_ANTES"))).isEqualTo(snapshotAntes);
        assertThat(mapper.readTree((String) novoAudit.get("DADOS_DEPOIS"))).isEqualTo(associado);
        var novoOp =
                jdbc.queryForMap(
                        "select * from wms.operacao_administrativa where operacao_id=?",
                        comando.get("operacaoId").toString());
        assertThat(novoOp.get("TIPO")).isEqualTo("REGISTRO_SERVICO");
        assertThat(((Number) novoOp.get("RECURSO_ID")).longValue())
                .isEqualTo(original.get("id").longValue());
        assertThat(((Number) novoOp.get("CLIENTE_ID")).longValue()).isEqualTo(clienteId);
        assertThat(((Number) novoOp.get("ARMAZEM_ID")).longValue()).isEqualTo(armazemId);
        assertThat(novoOp.get("USUARIO")).isEqualTo("cobranca-SUPERVISOR");
        assertThat((String) novoOp.get("CONTEUDO_HASH"))
                .matches("[0-9a-f]{64}")
                .isNotEqualTo(opOriginal.get("CONTEUDO_HASH"));
        assertThat(mapper.readTree((String) novoOp.get("RESULTADO"))).isEqualTo(associado);
        assertThat(
                        jdbc.queryForObject(
                                "select count(*) from wms.operacao_administrativa where tipo='REGISTRO_SERVICO' and recurso_id=?",
                                Integer.class,
                                original.get("id").longValue()))
                .isEqualTo(2);
        assertThat(
                        jdbc.queryForMap(
                                "select * from wms.operacao_administrativa where operacao_id=?",
                                originalComando.get("operacaoId").toString()))
                .isEqualTo(opOriginal);
        var confirmado = D30FotografiaFisica.capturar(jdbc);
        assertThat(resposta(post("/api/v1/fatos-servico", originalComando, supervisor), 200))
                .isEqualTo(original);
        assertThat(resposta(post("/api/v1/fatos-servico", comando, supervisor), 200))
                .isEqualTo(associado);
        assertThat(D30FotografiaFisica.capturar(jdbc)).isEqualTo(confirmado);
        assertThat(confirmado.get("RATEIO_FATO_SERVICO"))
                .isEqualTo(antes.get("RATEIO_FATO_SERVICO"));
        var fisicoAntes = new HashMap<String, Object>(antes.get("FATO_SERVICO").get(0));
        var fisicoDepois = new HashMap<String, Object>(confirmado.get("FATO_SERVICO").get(0));
        for (String colunaMutavel : List.of("PEDIDO_SAIDA_ID", "VERSAO")) {
            fisicoAntes.remove(colunaMutavel);
            fisicoDepois.remove(colunaMutavel);
        }
        assertThat(fisicoDepois).isEqualTo(fisicoAntes);
    }

    @Test
    void d30ReservaMantemValorEDiariaEmFimDeSemanaSemAutorizarFechamento() throws Exception {
        configBasica("2");
        var u = unidadeOperacional();
        when(clock.instant()).thenReturn(Instant.parse("2026-09-04T12:00:00Z"));
        var pedido80 = comando();
        pedido80.put("clienteId", clienteId);
        pedido80.put("armazemId", armazemId);
        pedido80.put("referencia", "D30-RESERVA-80");
        pedido80.put("itens", List.of(Map.of("produtoId", produtoId, "quantidade", "80")));
        var ps = resposta(post("/api/v1/pedidos-saida", pedido80, operador), 201).get("pedido");
        var reserva = comando();
        reserva.put("versao", ps.get("versao").longValue());
        resposta(
                post(
                        "/api/v1/pedidos-saida/" + ps.get("id").longValue() + "/reserva",
                        reserva,
                        operador),
                200);
        when(clock.instant()).thenReturn(Instant.parse("2026-09-08T12:00:00Z"));
        var antes = D30FotografiaFisica.capturar(jdbc);
        var indicador =
                resposta(
                                get(
                                        "/api/v1/indicadores-estoque?clienteId="
                                                + clienteId
                                                + "&armazemId="
                                                + armazemId
                                                + "&produtoId="
                                                + produtoId
                                                + "&fuso=UTC&valor=true",
                                        supervisor),
                                200)
                        .get("itens")
                        .get(0);
        assertThat(indicador.get("saldo").get("fisicoTotal").decimalValue())
                .isEqualByComparingTo("100");
        assertThat(indicador.get("saldo").get("reservado").decimalValue())
                .isEqualByComparingTo("80");
        assertThat(indicador.get("saldo").get("bloqueado").decimalValue())
                .isEqualByComparingTo("20");
        assertThat(indicador.get("valorExato").decimalValue()).isEqualByComparingTo("1000");
        assertThat(D30FotografiaFisica.capturar(jdbc)).isEqualTo(antes);
        var calculo = calcular("2026-09-04", "2026-09-08");
        assertThat(calculo.get("total").decimalValue()).isEqualByComparingTo("8");
        var dias = calculo.get("memoria").get("diarias");
        assertThat(dias.size()).isEqualTo(4);
        for (int i = 0; i < 4; i++) {
            assertThat(dias.get(i).get("data").asString())
                    .isEqualTo(LocalDate.of(2026, 9, 4).plusDays(i).toString());
            assertThat(dias.get(i).get("tarifa").decimalValue()).isEqualByComparingTo("2");
            assertThat(dias.get(i).get("valor").decimalValue()).isEqualByComparingTo("2");
            assertThat(dias.get(i).get("valorEstoque").decimalValue()).isEqualByComparingTo("1000");
        }
        var depois = D30FotografiaFisica.capturar(jdbc);
        for (String tabela :
                List.of(
                        "RETIRADA_SAIDA",
                        "FECHAMENTO_COBRANCA",
                        "CONFIRMACAO_EXTERNA_FECHAMENTO",
                        "TRATATIVA_EXTERNA_FECHAMENTO",
                        "RESOLUCAO_FINANCEIRA_FECHAMENTO",
                        "FATO_SERVICO",
                        "RATEIO_FATO_SERVICO",
                        "CONTEUDO_UNIDADE",
                        "RESERVA_SAIDA",
                        "OCUPACAO_ENDERECO")) {
            assertThat(antes).containsKey(tabela);
            assertThat(depois.get(tabela)).as(tabela).isEqualTo(antes.get(tabela));
        }
        assertThat(
                        resposta(
                                        get(
                                                "/api/v1/pedidos-saida/" + ps.get("id").longValue(),
                                                operador),
                                        200)
                                .get("situacao")
                                .asString())
                .isEqualTo("RESERVADO");
        assertThat(
                        jdbc.queryForObject(
                                "select nota_id from wms.unidade_logistica where id=?",
                                Long.class,
                                u.id()))
                .isEqualTo(u.notaId());
    }

    @Test
    void d30CatalogoConfrontaTodosParesDeTipoEUnidade() throws Exception {
        var permitidos =
                java.util.Set.of(
                        "ARMAZENAGEM:POSICAO_DIA",
                        "ENTRADA:UNIDADE_LOGISTICA",
                        "ENTRADA:QUANTIDADE_PRODUTO",
                        "SAIDA:UNIDADE_LOGISTICA",
                        "SAIDA:QUANTIDADE_PRODUTO",
                        "ADICIONAL:UNIDADE_LOGISTICA",
                        "ADICIONAL:QUANTIDADE_PRODUTO",
                        "ADICIONAL:VEICULO",
                        "ADICIONAL:CONTEINER",
                        "ADICIONAL:PERCENTUAL");
        int validos = 0, invalidos = 0;
        for (String tipo : List.of("ARMAZENAGEM", "ENTRADA", "SAIDA", "ADICIONAL"))
            for (String unidade :
                    List.of(
                            "POSICAO_DIA",
                            "UNIDADE_LOGISTICA",
                            "QUANTIDADE_PRODUTO",
                            "VEICULO",
                            "CONTEINER",
                            "PERCENTUAL")) {
                var d = comando();
                d.put("codigo", "D30-CAT-" + (++numero));
                d.put("descricao", "Catalogo ficticio D30");
                d.put("tipo", tipo);
                d.put("unidade", unidade);
                var antes = D30FotografiaFisica.capturar(jdbc);
                if (permitidos.contains(tipo + ":" + unidade)) {
                    var r = resposta(post("/api/v1/servicos-cobranca", d, gestor), 200);
                    assertThat(r.get("tipo").asString()).isEqualTo(tipo);
                    assertThat(r.get("unidade").asString()).isEqualTo(unidade);
                    validos++;
                } else {
                    resposta(post("/api/v1/servicos-cobranca", d, gestor), 400);
                    assertThat(D30FotografiaFisica.capturar(jdbc)).isEqualTo(antes);
                    invalidos++;
                }
            }
        assertThat(validos).isEqualTo(10);
        assertThat(invalidos).isEqualTo(14);
        assertThat(contar("servico_cobranca")).isEqualTo(10);
    }

    @Test
    void d30TabelaConservaContextoPeriodoECodigoSemEfeitosInvalidos() throws Exception {
        var storage = servico("ARMAZENAGEM", "POSICAO_DIA");
        var padrao = comando();
        padrao.put("armazemId", armazemId);
        padrao.put("codigo", "D30-PADRAO");
        padrao.put("descricao", "Tabela padrao ficticia");
        padrao.put("tipo", "PADRAO");
        padrao.put("vigenciaInicio", "2026-09-01");
        padrao.put("itens", List.of(item(storage, "", "0")));
        var original = resposta(post("/api/v1/tabelas-cobranca", padrao, gestor), 200);
        assertThat(original.get("clienteId").isNull()).isTrue();
        assertThat(original.get("armazemId").longValue()).isEqualTo(armazemId);
        for (String variante :
                List.of(
                        "PADRAO_CLIENTE",
                        "ESPECIFICA_SEM_CLIENTE",
                        "FIM_IGUAL",
                        "FIM_ANTERIOR",
                        "CODIGO_DUPLICADO")) {
            var d = new HashMap<String, Object>(padrao);
            d.put("operacaoId", UUID.randomUUID());
            d.put("codigo", variante.equals("CODIGO_DUPLICADO") ? "D30-PADRAO" : "D30-" + variante);
            switch (variante) {
                case "PADRAO_CLIENTE" -> d.put("clienteId", clienteId);
                case "ESPECIFICA_SEM_CLIENTE" -> d.put("tipo", "ESPECIFICA");
                case "FIM_IGUAL" -> d.put("vigenciaFim", "2026-09-01");
                case "FIM_ANTERIOR" -> d.put("vigenciaFim", "2026-08-31");
                case "CODIGO_DUPLICADO" -> {}
                default -> throw new IllegalStateException("Vetor D30 desconhecido");
            }
            var antes = D30FotografiaFisica.capturar(jdbc);
            resposta(
                    post("/api/v1/tabelas-cobranca", d, gestor),
                    variante.equals("CODIGO_DUPLICADO") ? 409 : 400);
            assertThat(D30FotografiaFisica.capturar(jdbc)).isEqualTo(antes);
        }
        var outro =
                resposta(
                        post(
                                "/api/v1/armazens",
                                Map.of(
                                        "codigo",
                                        "D30-TABELA-OUTRO",
                                        "nome",
                                        "Outro armazem ficticio",
                                        "documentoFiscal",
                                        "00000000000001",
                                        "cidade",
                                        "Cidade ficticia",
                                        "uf",
                                        "SP"),
                                gestor),
                        201);
        var mesmoCodigo = new HashMap<String, Object>(padrao);
        mesmoCodigo.put("operacaoId", UUID.randomUUID());
        mesmoCodigo.put("armazemId", outro.get("id").longValue());
        var segundo = resposta(post("/api/v1/tabelas-cobranca", mesmoCodigo, gestor), 200);
        assertThat(segundo.get("codigo")).isEqualTo(original.get("codigo"));
        assertThat(segundo.get("armazemId")).isNotEqualTo(original.get("armazemId"));
        assertThat(contar("tabela_cobranca")).isEqualTo(2);
    }

    @Test
    void d30ContratoRecusaModoSemParametrosOuParametrosForaDoModo() throws Exception {
        var s = servico("ARMAZENAGEM", "POSICAO_DIA");
        for (String familia : List.of("MINIMO", "GRIS")) {
            var campos =
                    familia.equals("MINIMO")
                            ? List.of("minimoValor", "minimoProporcao", "servicosMinimo")
                            : List.of(
                                    "grisPercentual",
                                    "grisBase",
                                    "grisPeriodicidade",
                                    "grisProporcao");
            for (String modo : List.of("APLICAVEL", "NAO_INFORMADO", "NAO_APLICAVEL"))
                for (String campo : campos) {
                    var d = comando();
                    d.put("clienteId", clienteId);
                    d.put("armazemId", armazemId);
                    d.put("vigenciaInicio", "2026-09-01");
                    d.put("vigenciaFim", null);
                    d.put("fuso", "UTC");
                    d.put("moeda", "BRL");
                    d.put("modalidadeCiclo", "DIAS_CORRIDOS");
                    d.put("duracaoDias", 30);
                    d.put("minimoModo", familia.equals("MINIMO") ? modo : "NAO_APLICAVEL");
                    d.put("grisModo", familia.equals("GRIS") ? modo : "NAO_APLICAVEL");
                    d.put("servicosMinimo", List.of());
                    var valores =
                            Map.<String, Object>of(
                                    "minimoValor",
                                    "0",
                                    "minimoProporcao",
                                    "INTEGRAL",
                                    "servicosMinimo",
                                    List.of(s.get("id").longValue()),
                                    "grisPercentual",
                                    "0",
                                    "grisBase",
                                    "VALOR_ESTOQUE_PICO",
                                    "grisPeriodicidade",
                                    "DIARIA",
                                    "grisProporcao",
                                    "INTEGRAL");
                    if (modo.equals("APLICAVEL")) {
                        for (String k : campos) d.put(k, valores.get(k));
                        d.put(campo, campo.equals("servicosMinimo") ? List.of() : null);
                    } else d.put(campo, valores.get(campo));
                    var antes = D30FotografiaFisica.capturar(jdbc);
                    resposta(post("/api/v1/contratos-cobranca", d, gestor), 400);
                    assertThat(D30FotografiaFisica.capturar(jdbc))
                            .as(familia + ":" + modo + ":" + campo)
                            .isEqualTo(antes);
                }
        }
    }

    @Test
    void d30MinimoRecusaAbrangenciaVaziaRepetidaOuServicoAusente() throws Exception {
        var storage = servico("ARMAZENAGEM", "POSICAO_DIA");
        long s = storage.get("id").longValue();
        var abrangencias = List.<List<Long>>of(List.of(), List.of(s, s), List.of(s + 100000));
        for (int i = 0; i < abrangencias.size(); i++) {
            var d = comando();
            d.put("clienteId", clienteId);
            d.put("armazemId", armazemId);
            d.put("vigenciaInicio", "2026-09-01");
            d.put("fuso", "UTC");
            d.put("moeda", "BRL");
            d.put("modalidadeCiclo", "DIAS_CORRIDOS");
            d.put("duracaoDias", 30);
            d.put("minimoModo", "APLICAVEL");
            d.put("minimoValor", "10");
            d.put("minimoProporcao", "PROPORCIONAL_DIAS");
            d.put("servicosMinimo", abrangencias.get(i));
            d.put("grisModo", "NAO_APLICAVEL");
            var antes = D30FotografiaFisica.capturar(jdbc);
            resposta(post("/api/v1/contratos-cobranca", d, gestor), i == 2 ? 404 : 400);
            assertThat(D30FotografiaFisica.capturar(jdbc)).isEqualTo(antes);
            assertThat(contar("contrato_cobranca")).isZero();
        }
    }

    @Test
    void d30ServicoExecutadoConservaValorAposDesistenciaEAvaria() throws Exception {
        var storage = servico("ARMAZENAGEM", "POSICAO_DIA");
        var entrada = servico("ENTRADA", "UNIDADE_LOGISTICA");
        vincular(
                tabela(
                        "D30-EXECUTADO",
                        "2026-09-01",
                        null,
                        List.of(item(storage, "", "0"), item(entrada, "", "5"))),
                "2026-09-01",
                null);
        contrato("NAO_APLICAVEL", null, List.of(), "NAO_APLICAVEL", null, null, null);
        var u = unidadeOperacional();
        var fato =
                resposta(
                        post(
                                "/api/v1/fatos-servico",
                                fatoEntrada(entrada, u, "MANUAL"),
                                supervisor),
                        200);
        var calculoAntes = calcular("2026-09-01", "2026-09-02");
        assertThat(calculoAntes.get("total").decimalValue()).isEqualByComparingTo("5");
        long saida = d30CriarSaidaReal(armazemId, "D30-DESISTENCIA");
        var pedido = resposta(get("/api/v1/pedidos-saida/" + saida, operador), 200);
        var cancelar = comando();
        cancelar.put("versao", pedido.get("versao").longValue());
        resposta(
                post("/api/v1/pedidos-saida/" + saida + "/cancelamento", cancelar, supervisor),
                200);
        when(clock.instant()).thenReturn(Instant.parse("2026-09-02T12:00:00Z"));
        resposta(post(unidadeRota(u, "/avaria"), bloqueioUnidade(u), operador), 200);
        var atual =
                resposta(
                        get("/api/v1/fatos-servico/" + fato.get("id").longValue(), supervisor),
                        200);
        assertThat(atual.get("id")).isEqualTo(fato.get("id"));
        assertThat(atual.get("chaveFato")).isEqualTo(fato.get("chaveFato"));
        assertThat(atual.get("executadoEm")).isEqualTo(fato.get("executadoEm"));
        assertThat(atual.get("situacao").asString()).isEqualTo("CONFIRMADO");
        assertThat(atual.get("quantidade").decimalValue()).isEqualByComparingTo("1");
        assertThat(atual.get("cotas").get(0).get("notaId").longValue()).isEqualTo(u.notaId());
        assertThat(atual.get("cotas").get(0).get("cota").decimalValue())
                .isEqualByComparingTo("100");
        var depois = calcular("2026-09-01", "2026-09-02");
        assertThat(depois.get("total").decimalValue()).isEqualByComparingTo("5");
        assertThat(depois.get("memoria").get("servicos").get(0).get("valor").decimalValue())
                .isEqualByComparingTo("5");
        assertThat(contar("fato_servico")).isEqualTo(1);
        assertThat(contar("rateio_fato_servico")).isEqualTo(1);
        assertThat(
                        resposta(
                                get(
                                        "/api/v1/calculos-cobranca/"
                                                + calculoAntes.get("id").longValue(),
                                        supervisor),
                                200))
                .isEqualTo(calculoAntes);
    }

    @ParameterizedTest
    @ValueSource(strings = {"CONTEINER", "PERCENTUAL"})
    void d30AdicionalConfrontaBaseMonetariaECategoriaSemMudarTarifaExecutada(String unidade)
            throws Exception {
        var storage = servico("ARMAZENAGEM", "POSICAO_DIA");
        var adicional = servico("ADICIONAL", unidade);
        var tarifa =
                unidade.equals("PERCENTUAL")
                        ? Map.<String, Object>of(
                                "servicoId",
                                adicional.get("id").longValue(),
                                "categoria",
                                "FICTICIO",
                                "percentual",
                                "10")
                        : item(adicional, "FICTICIO", "3");
        var tabela =
                tabela(
                        "D30-BASE-" + unidade,
                        "2026-09-01",
                        null,
                        List.of(item(storage, "", "0"), tarifa));
        vincular(tabela, "2026-09-01", null);
        contrato("NAO_APLICAVEL", null, List.of(), "NAO_APLICAVEL", null, null, null);
        var u = unidade("100", TipoUnidadeLogistica.PALLET, "2026-09-01T12:00:00Z", 1);
        var d = d30ComandoAdicional(adicional, u, "D30-BASE");
        d.put("categoria", "FICTICIO");
        d.put("quantidade", unidade.equals("PERCENTUAL") ? "1" : "2");
        if (unidade.equals("PERCENTUAL")) d.put("valorBase", "100");
        var invalido = new HashMap<String, Object>(d);
        invalido.put("operacaoId", UUID.randomUUID());
        if (unidade.equals("PERCENTUAL")) invalido.remove("valorBase");
        else invalido.put("valorBase", "100");
        var antes = D30FotografiaFisica.capturar(jdbc);
        resposta(post("/api/v1/fatos-servico", invalido, supervisor), 400);
        assertThat(D30FotografiaFisica.capturar(jdbc)).isEqualTo(antes);
        var registrado = resposta(post("/api/v1/fatos-servico", d, supervisor), 200);
        var calculo = calcular("2026-09-01", "2026-09-03");
        String esperado = unidade.equals("PERCENTUAL") ? "10" : "6";
        assertThat(calculo.get("total").decimalValue()).isEqualByComparingTo(esperado);
        assertThat(calculo.get("memoria").get("servicos").get(0).get("valor").decimalValue())
                .isEqualByComparingTo(esperado);
        assertThat(resposta(post("/api/v1/fatos-servico", d, supervisor), 200))
                .isEqualTo(registrado);
        var imutavel = D30FotografiaFisica.capturar(jdbc);
        var tentativa = comando();
        tentativa.put("itens", List.of(item(adicional, "FICTICIO", "99")));
        resposta(
                http.send(
                        request("/api/v1/tabelas-cobranca/" + tabela.get("id").longValue(), gestor)
                                .header("Content-Type", "application/json")
                                .method(
                                        "PUT",
                                        HttpRequest.BodyPublishers.ofString(
                                                mapper.writeValueAsString(tentativa)))
                                .build(),
                        HttpResponse.BodyHandlers.ofString()),
                405);
        assertThat(D30FotografiaFisica.capturar(jdbc)).isEqualTo(imutavel);
        assertThat(
                        resposta(
                                get(
                                        "/api/v1/calculos-cobranca/"
                                                + calculo.get("id").longValue(),
                                        supervisor),
                                200))
                .isEqualTo(calculo);
    }

    @ParameterizedTest
    @ValueSource(strings = {"UNIDADE", "PEDIDO"})
    void d30AssociacaoFuturaConfrontaNulosNormalizacaoESituacaoOriginal(String origem)
            throws Exception {
        var adicional = servico("ADICIONAL", "VEICULO");
        var u = unidadeOperacional();
        var originalComando = d30ComandoAdicional(adicional, u, "D30-GUARDAS");
        if (origem.equals("PEDIDO")) originalComando.remove("unidadeId");
        var original = resposta(post("/api/v1/fatos-servico", originalComando, supervisor), 200);
        long saida = d30CriarSaidaReal(armazemId, "D30-GUARDAS-SAIDA");
        for (String campo :
                List.of("pedidoSaidaId", "unidadeId", "executadoEm", "cotas", "valorBase")) {
            var d = new HashMap<>(originalComando);
            d.put("operacaoId", UUID.randomUUID());
            d.put("pedidoSaidaId", saida);
            if (campo.equals("pedidoSaidaId")) d.put(campo, null);
            else if (campo.equals("unidadeId")) {
                if (origem.equals("PEDIDO")) d.put(campo, u.id());
                else d.put(campo, null);
            } else if (campo.equals("executadoEm")) d.put(campo, "2026-09-02T12:00:00.000001Z");
            else if (campo.equals("cotas"))
                d.put(campo, List.of(Map.of("notaId", u.notaId(), "cota", "2")));
            else d.put(campo, "1");
            var antes = D30FotografiaFisica.capturar(jdbc);
            resposta(
                    post("/api/v1/fatos-servico", d, supervisor),
                    campo.equals("valorBase") ? 400 : 409);
            assertThat(D30FotografiaFisica.capturar(jdbc)).isEqualTo(antes);
        }
        var associar = new HashMap<>(originalComando);
        associar.put("operacaoId", UUID.randomUUID());
        associar.put("pedidoSaidaId", saida);
        associar.put("quantidade", new BigDecimal("1.000000"));
        associar.put("categoria", "van");
        associar.put("criterioRateio", "  Cota original da nota da unidade  ");
        if (origem.equals("UNIDADE")) associar.put("pedidoEntradaId", null);
        var confirmado = resposta(post("/api/v1/fatos-servico", associar, supervisor), 200);
        assertThat(confirmado.get("id")).isEqualTo(original.get("id"));
        assertThat(confirmado.get("pedidoSaidaId").longValue()).isEqualTo(saida);
        assertThat(confirmado.get("quantidade").decimalValue()).isEqualByComparingTo("1");
        assertThat(confirmado.get("categoria").asString()).isEqualTo("VAN");
        assertThat(confirmado.get("executadoEm")).isEqualTo(original.get("executadoEm"));
        assertThat(confirmado.get("unidadeId")).isEqualTo(original.get("unidadeId"));
        assertThat(confirmado.get("produtoId")).isEqualTo(original.get("produtoId"));
        var originalAnular = d30ComandoAdicional(adicional, u, "D30-GUARDAS-ANULADO");
        var anulado = resposta(post("/api/v1/fatos-servico", originalAnular, supervisor), 200);
        var anulacao = comando();
        anulacao.put("versao", anulado.get("versao").longValue());
        resposta(
                post(
                        "/api/v1/fatos-servico/" + anulado.get("id").longValue() + "/anulacao",
                        anulacao,
                        gestor),
                200);
        originalAnular.put("operacaoId", UUID.randomUUID());
        originalAnular.put("pedidoSaidaId", saida);
        var antes = D30FotografiaFisica.capturar(jdbc);
        resposta(post("/api/v1/fatos-servico", originalAnular, supervisor), 409);
        assertThat(D30FotografiaFisica.capturar(jdbc)).isEqualTo(antes);
    }

    @Test
    void d30AssociacaoFuturaRecusaSaidaDeOutroSkuNoMesmoContexto() throws Exception {
        var adicional = servico("ADICIONAL", "VEICULO");
        var u = unidadeOperacional();
        var original = d30ComandoAdicional(adicional, u, "D30-SKU-ORIGINAL");
        resposta(post("/api/v1/fatos-servico", original, supervisor), 200);
        Long produtoOriginal = produtoId, embalagemOriginal = embalagemId;
        long outraSaida;
        try {
            new TransactionTemplate(transactions)
                    .executeWithoutResult(
                            tx -> {
                                var p =
                                        new Produto(
                                                em.find(Cliente.class, clienteId),
                                                "D30-ASSOC-SKU-B",
                                                "Outro SKU ficticio",
                                                "UN",
                                                TipoQuantidade.CONTAGEM,
                                                0,
                                                false,
                                                false,
                                                null,
                                                BASE);
                                em.persist(p);
                                var e =
                                        new Embalagem(
                                                p,
                                                "D30-ASSOC-DUN-B",
                                                "Outra embalagem",
                                                BigDecimal.TEN,
                                                BASE);
                                em.persist(e);
                                em.flush();
                                produtoId = p.getId();
                                embalagemId = e.getId();
                            });
            unidadeOperacional();
            outraSaida = d30CriarSaidaReal(armazemId, "D30-SKU-B-SAIDA");
        } finally {
            produtoId = produtoOriginal;
            embalagemId = embalagemOriginal;
        }
        original.put("operacaoId", UUID.randomUUID());
        original.put("pedidoSaidaId", outraSaida);
        var antes = D30FotografiaFisica.capturar(jdbc);
        var recusado = resposta(post("/api/v1/fatos-servico", original, supervisor), 409);
        assertThat(recusado.get("codigo").asString()).isEqualTo("CONTEXTO_EXECUCAO_INVALIDO");
        assertThat(D30FotografiaFisica.capturar(jdbc)).isEqualTo(antes);
    }

    @ParameterizedTest
    @ValueSource(strings = {"ARMAZEM", "EMBALAGEM", "ENDERECO"})
    void d30EncerramentoIdentificaCompromissosFisicosPorDono(String tipo) throws Exception {
        var u = unidadeOperacional();
        long id =
                tipo.equals("ARMAZEM")
                        ? armazemId
                        : tipo.equals("EMBALAGEM")
                                ? embalagemId
                                : jdbc.queryForObject(
                                        "select endereco_id from wms.ocupacao_endereco where unidade_id=?",
                                        Long.class,
                                        u.id());
        String rota = "/api/v1/encerramentos/" + tipo + "/" + id;
        var antesGet = D30FotografiaFisica.capturar(jdbc);
        var compromissos = resposta(get(rota + "/impedimentos", gestor), 200);
        assertThat(D30FotografiaFisica.capturar(jdbc)).isEqualTo(antesGet);
        d30AssertImpedimento(compromissos, "SALDO_FISICO", "UNIDADE_LOGISTICA", u.id());
        d30AssertImpedimento(compromissos, "OCUPACAO_ATIVA", "UNIDADE_LOGISTICA", u.id());
        var solicitar = comando();
        solicitar.put("versao", compromissos.get("versao").longValue());
        var pendente = resposta(post(rota + "/solicitar", solicitar, gestor), 200);
        assertThat(pendente.get("situacao").asString()).isEqualTo("ENCERRAMENTO_PENDENTE");
        var inativar = comando();
        inativar.put("versao", pendente.get("versao").longValue());
        var antes = D30FotografiaFisica.capturar(jdbc);
        var recusa = resposta(post(rota + "/inativar", inativar, gestor), 409);
        assertThat(recusa.get("codigo").asString()).isEqualTo("ENCERRAMENTO_IMPEDIDO");
        assertThat(D30FotografiaFisica.capturar(jdbc)).isEqualTo(antes);
    }

    @Test
    void d30EncerramentoServicoIdentificaFatoETabelaSemApagarExecucao() throws Exception {
        var storage = servico("ARMAZENAGEM", "POSICAO_DIA");
        var entrada = servico("ENTRADA", "UNIDADE_LOGISTICA");
        var t =
                tabela(
                        "D30-ENC-TABELA",
                        "2026-09-01",
                        null,
                        List.of(item(storage, "", "0"), item(entrada, "", "5")));
        vincular(t, "2026-09-01", null);
        var contratoMinimo =
                contrato(
                        "APLICAVEL",
                        "10",
                        List.of(entrada.get("id").longValue()),
                        "NAO_APLICAVEL",
                        null,
                        null,
                        null);
        var u = unidadeOperacional();
        var fato =
                resposta(
                        post(
                                "/api/v1/fatos-servico",
                                fatoEntrada(entrada, u, "MANUAL"),
                                supervisor),
                        200);
        String rota = "/api/v1/encerramentos/SERVICO_COBRANCA/" + entrada.get("id").longValue();
        var antes = D30FotografiaFisica.capturar(jdbc);
        var compromissos = resposta(get(rota + "/impedimentos", gestor), 200);
        d30AssertImpedimento(
                compromissos, "FATO_NAO_FECHADO", "FATO_SERVICO", fato.get("id").longValue());
        d30AssertImpedimento(
                compromissos,
                "SERVICO_EM_TABELA_VIGENTE",
                "TABELA_COBRANCA",
                t.get("id").longValue());
        d30AssertImpedimento(
                compromissos,
                "SERVICO_EM_MINIMO_VIGENTE",
                "CONTRATO_COBRANCA",
                contratoMinimo.get("id").longValue());
        for (String dono : List.of("CLIENTE", "ARMAZEM")) {
            long idDono = dono.equals("CLIENTE") ? clienteId : armazemId;
            var porDono =
                    resposta(
                            get(
                                    "/api/v1/encerramentos/"
                                            + dono
                                            + "/"
                                            + idDono
                                            + "/impedimentos",
                                    gestor),
                            200);
            d30AssertImpedimento(
                    porDono,
                    "VIGENCIA_CONTRATUAL_ABERTA",
                    "CONTRATO_COBRANCA",
                    contratoMinimo.get("id").longValue());
            d30AssertImpedimento(
                    porDono, "VIGENCIA_TABELA_ABERTA", "TABELA_COBRANCA", t.get("id").longValue());
            long vinculoId =
                    jdbc.queryForObject(
                            "select id from wms.vinculo_tabela_cliente where tabela_id=?",
                            Long.class,
                            t.get("id").longValue());
            d30AssertImpedimento(
                    porDono, "VIGENCIA_VINCULO_ABERTA", "VINCULO_TABELA_CLIENTE", vinculoId);
        }
        assertThat(D30FotografiaFisica.capturar(jdbc)).isEqualTo(antes);
        var solicitar = comando();
        solicitar.put("versao", compromissos.get("versao").longValue());
        var pendente = resposta(post(rota + "/solicitar", solicitar, gestor), 200);
        var inativar = comando();
        inativar.put("versao", pendente.get("versao").longValue());
        antes = D30FotografiaFisica.capturar(jdbc);
        assertThat(
                        resposta(post(rota + "/inativar", inativar, gestor), 409)
                                .get("codigo")
                                .asString())
                .isEqualTo("ENCERRAMENTO_IMPEDIDO");
        assertThat(D30FotografiaFisica.capturar(jdbc)).isEqualTo(antes);
        var fatoRelido =
                resposta(
                        get("/api/v1/fatos-servico/" + fato.get("id").longValue(), supervisor),
                        200);
        assertThat(new BigDecimal(fato.get("quantidade").asString())).isEqualByComparingTo("1");
        assertThat(new BigDecimal(fatoRelido.get("quantidade").asString()))
                .isEqualByComparingTo("1");
        var originalSemQuantidade = ((tools.jackson.databind.node.ObjectNode) fato).deepCopy();
        var relidoSemQuantidade = ((tools.jackson.databind.node.ObjectNode) fatoRelido).deepCopy();
        originalSemQuantidade.remove("quantidade");
        relidoSemQuantidade.remove("quantidade");
        assertThat(relidoSemQuantidade).isEqualTo(originalSemQuantidade);
        assertThat(
                        d30GetFotografiaIntegral(
                                        "/api/v1/fatos-servico?clienteId="
                                                + clienteId
                                                + "&armazemId="
                                                + armazemId,
                                        supervisor)
                                .get("totalItens")
                                .longValue())
                .isPositive();
    }

    @Test
    void d30EntradaESaidaPorUnidadeNaoDuplicamDuasPosicoes() throws Exception {
        var storage = servico("ARMAZENAGEM", "POSICAO_DIA");
        var entrada = servico("ENTRADA", "UNIDADE_LOGISTICA");
        var saida = servico("SAIDA", "UNIDADE_LOGISTICA");
        var tabela =
                tabela(
                        "D30-DUPLAS",
                        "2026-09-01",
                        null,
                        List.of(
                                item(storage, "", "0"),
                                item(entrada, "", "5"),
                                item(saida, "", "5")));
        vincular(tabela, "2026-09-01", null);
        contrato("NAO_APLICAVEL", null, List.of(), "NAO_APLICAVEL", null, null, null);
        var u = unidade("100", TipoUnidadeLogistica.PALLET, "2026-09-01T12:00:00Z", 2);
        long a = enderecoOperacional(TipoEndereco.ARMAZENAGEM),
                b = enderecoOperacional(TipoEndereco.ARMAZENAGEM);
        new TransactionTemplate(transactions)
                .executeWithoutResult(
                        tx -> {
                            em.find(PedidoEntrada.class, u.pedidoId())
                                    .concluir("Entrada ficticia conferida", BASE);
                            var unidade = em.find(UnidadeLogistica.class, u.id());
                            var origem =
                                    em.createQuery(
                                                    "select c from ConteudoUnidade c where c.unidade.id=:id",
                                                    ConteudoUnidade.class)
                                            .setParameter("id", u.id())
                                            .getSingleResult();
                            origem.getEntrada().marcarUnitizada(BASE);
                            var c =
                                    new br.com.rodogarcia.wms.models.ConjuntoPosicoes(
                                            em.find(Armazem.class, armazemId),
                                            "D30-DUPLAS",
                                            em.find(Endereco.class, a),
                                            em.find(Endereco.class, b),
                                            new BigDecimal("2000"),
                                            BigDecimal.TEN,
                                            new BigDecimal("20"),
                                            BigDecimal.TEN,
                                            10,
                                            BASE);
                            em.persist(c);
                            unidade.posicionar(
                                    unidade.getMedidas(),
                                    TipoEndereco.ARMAZENAGEM,
                                    c,
                                    Instant.parse("2026-09-01T12:00:00Z"));
                            for (long id : List.of(a, b)) {
                                var o = new OcupacaoEndereco(em.find(Endereco.class, id));
                                o.atribuir(unidade);
                                em.persist(o);
                            }
                        });
        assertThat(
                        jdbc.queryForObject(
                                "select count(*) from wms.ocupacao_endereco where unidade_id=?",
                                Integer.class,
                                u.id()))
                .isEqualTo(2);
        var antesEntrada = D30FotografiaFisica.capturar(jdbc);
        var de = fatoEntrada(entrada, u, "MANUAL");
        var fe = resposta(post("/api/v1/fatos-servico", de, supervisor), 200);
        assertThat(fe.get("quantidade").decimalValue()).isEqualByComparingTo("1");
        assertThat(fe.has("valor")).isFalse();
        d30FatoNaoMudaFisico(antesEntrada);
        retirar(u, "10", "2026-09-03T12:00:00Z");
        long ps = jdbc.queryForObject("select pedido_id from wms.retirada_saida", Long.class);
        var ds = comando();
        ds.put("clienteId", clienteId);
        ds.put("armazemId", armazemId);
        ds.put("servicoId", saida.get("id").longValue());
        ds.put("origem", "MANUAL");
        ds.put("pedidoSaidaId", ps);
        ds.put("categoria", "");
        ds.put("cotas", List.of());
        ds.put("criterioRateio", "Saida fisica parcial da unidade com duas posicoes");
        var antesSaida = D30FotografiaFisica.capturar(jdbc);
        var fs = resposta(post("/api/v1/fatos-servico", ds, supervisor), 200);
        assertThat(fs.get("quantidade").decimalValue()).isEqualByComparingTo("1");
        assertThat(fs.has("valor")).isFalse();
        d30FatoNaoMudaFisico(antesSaida);
        assertThat(
                        jdbc.queryForObject(
                                "select quantidade from wms.unidade_logistica where id=?",
                                BigDecimal.class,
                                u.id()))
                .isEqualByComparingTo("90");
        assertThat(
                        jdbc.queryForObject(
                                "select count(*) from wms.ocupacao_endereco where unidade_id=?",
                                Integer.class,
                                u.id()))
                .isEqualTo(2);
        var calculo = calcular("2026-09-01", "2026-09-04");
        assertThat(calculo.get("total").decimalValue()).isEqualByComparingTo("10");
        assertThat(calculo.get("memoria").get("servicos").size()).isEqualTo(2);
        for (var f : calculo.get("memoria").get("servicos")) {
            assertThat(f.get("quantidade").decimalValue()).isEqualByComparingTo("1");
            assertThat(f.get("valor").decimalValue()).isEqualByComparingTo("5");
        }
        var confirmado = D30FotografiaFisica.capturar(jdbc);
        assertThat(resposta(post("/api/v1/fatos-servico", de, supervisor), 200)).isEqualTo(fe);
        assertThat(resposta(post("/api/v1/fatos-servico", ds, supervisor), 200)).isEqualTo(fs);
        assertThat(D30FotografiaFisica.capturar(jdbc)).isEqualTo(confirmado);
    }

    private void d30FatoNaoMudaFisico(Map<String, List<Map<String, Object>>> antes)
            throws Exception {
        var depois = D30FotografiaFisica.capturar(jdbc);
        for (String tabela : antes.keySet())
            if (!List.of(
                            "FATO_SERVICO",
                            "RATEIO_FATO_SERVICO",
                            "AUDITORIA_CADASTRO",
                            "OPERACAO_ADMINISTRATIVA")
                    .contains(tabela))
                assertThat(depois.get(tabela)).as(tabela).isEqualTo(antes.get(tabela));
    }

    @Test
    void d30RetornoInternoExecutadoTemAdicionalManualUnicoConfigurado() throws Exception {
        var storage = servico("ARMAZENAGEM", "POSICAO_DIA");
        var adicional = servico("ADICIONAL", "VEICULO");
        var tabela =
                tabela(
                        "D30-RETORNO",
                        "2026-09-01",
                        null,
                        List.of(item(storage, "", "0"), item(adicional, "RETORNO", "3")));
        vincular(tabela, "2026-09-01", null);
        contrato("NAO_APLICAVEL", null, List.of(), "NAO_APLICAVEL", null, null, null);
        var u = unidadeOperacional();
        long origem =
                jdbc.queryForObject(
                        "select endereco_id from wms.ocupacao_endereco where unidade_id=?",
                        Long.class,
                        u.id());
        var composicaoOriginal =
                jdbc.queryForList(
                        "select * from wms.conteudo_unidade where unidade_id=? order by id",
                        u.id());
        long pedido = d30CriarSaidaReal(armazemId, "D30-RETORNO-REAL");
        String rota = "/api/v1/pedidos-saida/" + pedido;
        var p = resposta(get(rota, supervisor), 200);
        var reservar = comando();
        reservar.put("versao", p.get("versao").longValue());
        p = resposta(post(rota + "/reserva", reservar, supervisor), 200).get("pedido");
        long reserva = p.get("reservas").get(0).get("id").longValue();
        long revisao =
                resposta(
                                get(
                                        "/api/v1/unidades-logisticas/" + u.codigo() + "/etiqueta",
                                        supervisor),
                                200)
                        .get("versaoConteudo")
                        .longValue();
        var ler = comando();
        ler.put("versao", p.get("versao").longValue());
        ler.put("reservaId", reserva);
        ler.put("codigoLido", u.codigo());
        ler.put("revisaoConteudo", revisao);
        p = resposta(post(rota + "/leituras", ler, supervisor), 200).get("expedicao").get("pedido");
        long separacao = enderecoOperacional(TipoEndereco.SEPARACAO);
        var separar = comando();
        separar.put("versao", p.get("versao").longValue());
        separar.put(
                "destinacao",
                Map.of(
                        "reservaId",
                        reserva,
                        "destinos",
                        List.of(
                                Map.of(
                                        "enderecoId",
                                        separacao,
                                        "codigoLido",
                                        jdbc.queryForObject(
                                                "select codigo from wms.endereco where id=?",
                                                String.class,
                                                separacao)))));
        p =
                resposta(post(rota + "/separacoes", separar, supervisor), 200)
                        .get("expedicao")
                        .get("pedido");
        var voltar = comando();
        voltar.put("versao", p.get("versao").longValue());
        voltar.put(
                "unidades",
                List.of(
                        Map.of(
                                "reservaId",
                                reserva,
                                "destinos",
                                List.of(
                                        Map.of(
                                                "enderecoId",
                                                origem,
                                                "codigoLido",
                                                jdbc.queryForObject(
                                                        "select codigo from wms.endereco where id=?",
                                                        String.class,
                                                        origem))))));
        var retorno = resposta(post(rota + "/retorno-interno", voltar, supervisor), 200);
        assertThat(retorno.get("expedicao").get("pedido").get("situacao").asString())
                .isEqualTo("CANCELADO");
        assertThat(
                        jdbc.queryForObject(
                                "select quantidade from wms.unidade_logistica where id=?",
                                BigDecimal.class,
                                u.id()))
                .isEqualByComparingTo("100");
        assertThat(
                        jdbc.queryForObject(
                                "select endereco_id from wms.ocupacao_endereco where unidade_id=?",
                                Long.class,
                                u.id()))
                .isEqualTo(origem);
        assertThat(
                        jdbc.queryForList(
                                "select * from wms.conteudo_unidade where unidade_id=? order by id",
                                u.id()))
                .isEqualTo(composicaoOriginal);
        assertThat(contar("fato_servico")).isZero();
        long movimentoRetorno =
                jdbc.queryForObject(
                        "select id from wms.fato_permanencia where unidade_id=? and tipo='RETORNO_INTERNO'",
                        Long.class,
                        u.id());
        Instant executado =
                jdbc.queryForObject(
                        "select ocorrida_em from wms.fato_permanencia where id=?",
                        (rs, n) ->
                                rs.getTimestamp(
                                                1,
                                                java.util.Calendar.getInstance(
                                                        java.util.TimeZone.getTimeZone("UTC")))
                                        .toInstant(),
                        movimentoRetorno);
        var registrar = d30ComandoAdicional(adicional, u, "RETORNO-" + voltar.get("operacaoId"));
        registrar.put("categoria", "RETORNO");
        registrar.put("pedidoSaidaId", pedido);
        registrar.put("executadoEm", executado);
        var fato = resposta(post("/api/v1/fatos-servico", registrar, supervisor), 200);
        assertThat(fato.get("quantidade").decimalValue()).isEqualByComparingTo("1");
        var memoriaRetorno = calcular("2026-09-01", "2026-10-07").get("memoria").get("servicos");
        assertThat(memoriaRetorno.size()).isEqualTo(1);
        assertThat(memoriaRetorno.get(0).get("quantidade").decimalValue())
                .isEqualByComparingTo("1");
        assertThat(memoriaRetorno.get(0).get("valor").decimalValue()).isEqualByComparingTo("3");
        assertThat(fato.get("pedidoSaidaId").longValue()).isEqualTo(pedido);
        assertThat(fato.get("unidadeId").longValue()).isEqualTo(u.id());
        assertThat(fato.get("referenciaExecucao").asString())
                .isEqualTo("RETORNO-" + voltar.get("operacaoId"));
        assertThat(fato.get("cotas").size()).isEqualTo(1);
        assertThat(fato.get("cotas").get(0).get("notaId").longValue()).isEqualTo(u.notaId());
        assertThat(fato.get("cotas").get(0).get("cota").decimalValue()).isEqualByComparingTo("1");
        var antesReplay = D30FotografiaFisica.capturar(jdbc);
        assertThat(resposta(post(rota + "/retorno-interno", voltar, supervisor), 200))
                .isEqualTo(retorno);
        assertThat(resposta(post("/api/v1/fatos-servico", registrar, supervisor), 200))
                .isEqualTo(fato);
        assertThat(D30FotografiaFisica.capturar(jdbc)).isEqualTo(antesReplay);
        var mesmaExecucao = new HashMap<String, Object>(registrar);
        mesmaExecucao.put("operacaoId", UUID.randomUUID());
        var confirmado = resposta(post("/api/v1/fatos-servico", mesmaExecucao, supervisor), 409);
        assertThat(confirmado.get("codigo").asString()).isEqualTo("FATO_SERVICO_JA_CONFIRMADO");
        assertThat(D30FotografiaFisica.capturar(jdbc)).isEqualTo(antesReplay);
        assertThat(contar("fato_servico")).isEqualTo(1);
        assertThat(contar("rateio_fato_servico")).isEqualTo(1);
        var antesDivergencia = D30FotografiaFisica.capturar(jdbc);
        mesmaExecucao.put("operacaoId", UUID.randomUUID());
        mesmaExecucao.put("quantidade", "2");
        resposta(post("/api/v1/fatos-servico", mesmaExecucao, supervisor), 409);
        assertThat(D30FotografiaFisica.capturar(jdbc)).isEqualTo(antesDivergencia);
        assertThat(
                        jdbc.queryForList(
                                "select * from wms.conteudo_unidade where unidade_id=? order by id",
                                u.id()))
                .isEqualTo(composicaoOriginal);
    }

    private void d30AssertImpedimento(JsonNode resultado, String codigo, String recurso, long id) {
        var encontrados = new ArrayList<JsonNode>();
        for (var i : resultado.get("impedimentos"))
            if (i.get("codigo").asString().equals(codigo)
                    && i.get("recurso").asString().equals(recurso)
                    && i.get("id").longValue() == id) encontrados.add(i);
        assertThat(encontrados).hasSize(1);
        assertThat(encontrados.getFirst().get("detalhe").asString()).isNotBlank();
    }

    private Map<String, Object> d30ComandoAdicional(
            JsonNode servico, Unidade u, String referencia) {
        var d = comando();
        d.put("clienteId", clienteId);
        d.put("armazemId", armazemId);
        d.put("servicoId", servico.get("id").longValue());
        d.put("origem", "MANUAL");
        d.put("unidadeId", u.id());
        d.put("pedidoEntradaId", u.pedidoId());
        d.put("referenciaExecucao", referencia);
        d.put("executadoEm", "2026-09-02T12:00:00Z");
        d.put("quantidade", "1");
        d.put("categoria", "VAN");
        d.put("criterioRateio", "Cota original da nota da unidade");
        d.put("cotas", List.of(Map.of("notaId", u.notaId(), "cota", "1")));
        return d;
    }

    private long d30CriarSaidaReal(long armazem, String referencia) throws Exception {
        var d = comando();
        d.put("clienteId", clienteId);
        d.put("armazemId", armazem);
        d.put("referencia", referencia);
        d.put("itens", List.of(Map.of("produtoId", produtoId, "quantidade", "10")));
        return resposta(post("/api/v1/pedidos-saida", d, gestor), 201)
                .get("pedido")
                .get("id")
                .longValue();
    }

    private Map<String, List<Map<String, Object>>> d30FotoGenericaAssociacao() {
        var result = new java.util.LinkedHashMap<String, List<Map<String, Object>>>();
        for (String tabela :
                jdbc.queryForList(
                        "select table_name from information_schema.tables where table_schema='WMS' and table_type='BASE TABLE' order by table_name",
                        String.class)) {
            assertThat(tabela).matches("[A-Z_]+");
            result.put(tabela, jdbc.queryForList("select * from wms." + tabela + " order by id"));
        }
        assertThat(result).hasSize(64);
        return result;
    }

    private Map<String, Object> calcularComando(String inicio, String fim) {
        var d = comando();
        d.put("clienteId", clienteId);
        d.put("armazemId", armazemId);
        d.put("periodoInicio", inicio);
        d.put("periodoFim", fim);
        return d;
    }

    private JsonNode calcular(String inicio, String fim) throws Exception {
        return resposta(
                post("/api/v1/calculos-cobranca", calcularComando(inicio, fim), supervisor), 200);
    }

    private Unidade unidade(
            String quantidade, TipoUnidadeLogistica tipo, String storage, int posicoes) {
        return unidade(quantidade, tipo, storage, posicoes, TipoEndereco.ARMAZENAGEM);
    }

    private Unidade unidade(
            String quantidade,
            TipoUnidadeLogistica tipo,
            String storage,
            int posicoes,
            TipoEndereco primeiroDestino) {
        return new TransactionTemplate(transactions)
                .execute(
                        tx -> {
                            var pe =
                                    new PedidoEntrada(
                                            em.find(Cliente.class, clienteId),
                                            em.find(Armazem.class, armazemId),
                                            "PE" + (++numero),
                                            BASE);
                            em.persist(pe);
                            var n =
                                    new NotaEntrada(
                                            pe,
                                            "33333333000133",
                                            1,
                                            numero,
                                            LocalDate.parse("2026-08-01"),
                                            null);
                            em.persist(n);
                            var i =
                                    new ItemNotaEntrada(
                                            n,
                                            1,
                                            em.find(Produto.class, produtoId),
                                            new BigDecimal(quantidade),
                                            new BigDecimal(quantidade).multiply(BigDecimal.TEN));
                            em.persist(i);
                            var c =
                                    new ChegadaRecebimento(
                                            pe,
                                            UUID.randomUUID().toString(),
                                            "0".repeat(64),
                                            BASE,
                                            BASE,
                                            "fixture",
                                            "Chegada ficticia comprovada");
                            em.persist(c);
                            var ic =
                                    new ItemChegada(
                                            c,
                                            1,
                                            i,
                                            null,
                                            null,
                                            new BigDecimal(quantidade),
                                            BigDecimal.ZERO);
                            em.persist(ic);
                            var entrada =
                                    new EntradaConferida(
                                            ic,
                                            BASE,
                                            BASE,
                                            new BigDecimal(quantidade),
                                            BigDecimal.ZERO);
                            em.persist(entrada);
                            entrada.marcarUnitizada(BASE);
                            var u =
                                    new UnidadeLogistica(
                                            entrada,
                                            em.find(Embalagem.class, embalagemId),
                                            tipo,
                                            CondicaoMercadoria.BOA,
                                            new BigDecimal(quantidade),
                                            BASE);
                            em.persist(u);
                            em.persist(new ConteudoUnidade(u, entrada, new BigDecimal(quantidade)));
                            u.posicionar(
                                    new MedidasUnidade(
                                            new BigDecimal("100"),
                                            BigDecimal.ONE,
                                            BigDecimal.ONE,
                                            BigDecimal.ONE,
                                            1,
                                            posicoes),
                                    primeiroDestino,
                                    null,
                                    Instant.parse(storage));
                            em.flush();
                            return new Unidade(u.getId(), pe.getId(), n.getId(), u.getCodigo());
                        });
    }

    private long retirar(Unidade ref, String quantidade, String instante) {
        return new TransactionTemplate(transactions)
                .execute(
                        tx -> {
                            var u = em.find(UnidadeLogistica.class, ref.id());
                            Instant data = Instant.parse(instante);
                            BigDecimal q = new BigDecimal(quantidade),
                                    antes = u.getQuantidade(),
                                    depois = antes.subtract(q);
                            var ps =
                                    new PedidoSaida(
                                            em.find(Cliente.class, clienteId),
                                            em.find(Armazem.class, armazemId),
                                            "PS" + (++numero),
                                            BASE);
                            em.persist(ps);
                            var item = new ItemPedidoSaida(ps, u.getProduto(), q);
                            em.persist(item);
                            var reserva =
                                    new ReservaSaida(
                                            item, u, UUID.randomUUID().toString(), q, BASE);
                            em.persist(reserva);
                            reserva.encerrar(SituacaoReservaSaida.RETIRADA, data);
                            ps.atualizar(SituacaoPedidoSaida.RETIRADO, data);
                            var retirada = new RetiradaSaida(ps, data, "fixture", "0".repeat(64));
                            em.persist(retirada);
                            var conteudo =
                                    em.createQuery(
                                                    "select c from ConteudoUnidade c where c.unidade.id=:id",
                                                    ConteudoUnidade.class)
                                            .setParameter("id", u.getId())
                                            .getSingleResult();
                            em.persist(new BaixaSaida(retirada, reserva, conteudo.getEntrada(), q));
                            conteudo.alterarQuantidade(depois);
                            u.alterarQuantidade(depois, data);
                            var f =
                                    new FatoPermanencia(
                                            u,
                                            UUID.randomUUID().toString(),
                                            "RETIRADA",
                                            data,
                                            data,
                                            antes,
                                            depois,
                                            BigDecimal.valueOf(u.getPosicoesEquivalentes()),
                                            depois.signum() == 0
                                                    ? BigDecimal.ZERO
                                                    : BigDecimal.valueOf(
                                                            u.getPosicoesEquivalentes()));
                            em.persist(f);
                            em.flush();
                            return f.getId();
                        });
    }

    private long avaria(
            Unidade ref, String afetada, String base, String ocorrida, String resolvida) {
        return new TransactionTemplate(transactions)
                .execute(
                        tx -> {
                            var u = em.find(UnidadeLogistica.class, ref.id());
                            var a =
                                    new AvariaEstoque(
                                            u,
                                            new BigDecimal(afetada),
                                            new BigDecimal(base),
                                            BigDecimal.valueOf(u.getPosicoesEquivalentes()),
                                            UUID.randomUUID().toString(),
                                            Instant.parse(ocorrida),
                                            Instant.now(),
                                            "[]",
                                            "Dano historico ficticio comprovado");
                            em.persist(a);
                            a.reconhecer("RODOGARCIA", "gestor-fixture", Instant.now());
                            if (resolvida != null) a.reparar(Instant.parse(resolvida));
                            em.flush();
                            return a.getId();
                        });
    }

    @Test
    void d30MultipartSeisMiBExatoAceitaEVizinhoRecusaSemCriarFisico() throws Exception {
        var servidor = (org.springframework.boot.tomcat.TomcatWebServer) contextoD30.getWebServer();
        var protocolo =
                (org.apache.coyote.http11.AbstractHttp11Protocol<?>)
                        servidor.getTomcat().getConnector().getProtocolHandler();
        java.nio.file.Files.writeString(
                java.nio.file.Path.of(System.getProperty("wms.test.evidencias.dir"))
                        .resolve("d30-cedro-multipart-config-" + UUID.randomUUID() + ".json"),
                mapper.writeValueAsString(
                        Map.of(
                                "maxSwallowSizeEfetivo", protocolo.getMaxSwallowSize(),
                                "maxFileSize",
                                        environment.getProperty(
                                                "spring.servlet.multipart.max-file-size"),
                                "maxRequestSize",
                                        environment.getProperty(
                                                "spring.servlet.multipart.max-request-size"),
                                "oraculo",
                                        "5242880 arquivo;6291456 request aceita;6291457 recusa413;foto64")),
                java.nio.file.StandardOpenOption.CREATE_NEW);
        byte[] arquivo = D30ExcelLimitesTest.arquivoExato(5242880);
        var antes = D30FotografiaFisica.capturar(jdbc);
        var r = resposta(d30MultipartExato(arquivo, 6291456), 200);
        assertThat(r.get("quantidadeLinhas").intValue()).isEqualTo(1);
        assertThat(r.get("erros").isEmpty()).isTrue();
        var depois = D30FotografiaFisica.capturar(jdbc);
        for (String tabela : antes.keySet())
            if (!List.of("IMPORTACAO_ENDERECO", "AUDITORIA_CADASTRO", "OPERACAO_ADMINISTRATIVA")
                    .contains(tabela))
                assertThat(depois.get(tabela)).as(tabela).isEqualTo(antes.get(tabela));
        var fotoRecusa = D30FotografiaFisica.capturar(jdbc);
        var recusa = d30MultipartExato(arquivo, 6291457);
        assertThat(recusa.statusCode()).as(recusa.body()).isEqualTo(413);
        assertThat(D30FotografiaFisica.capturar(jdbc)).isEqualTo(fotoRecusa);
    }

    private HttpResponse<String> d30MultipartExato(byte[] arquivo, int tamanho) throws Exception {
        String boundary = "d30-local-limite";
        String prefixo =
                "--"
                        + boundary
                        + "\r\nContent-Disposition: form-data; name=\"comando\"\r\nContent-Type: application/json\r\n\r\n"
                        + mapper.writeValueAsString(comando())
                        + "\r\n--"
                        + boundary
                        + "\r\nContent-Disposition: form-data; name=\"arquivo\"; filename=\"enderecos.xlsx\"\r\nContent-Type: application/vnd.openxmlformats-officedocument.spreadsheetml.sheet\r\n\r\n";
        String meio =
                "\r\n--"
                        + boundary
                        + "\r\nContent-Disposition: form-data; name=\"d30padding\"\r\nContent-Type: application/octet-stream\r\n\r\n";
        String fim = "\r\n--" + boundary + "--\r\n";
        byte[] a = prefixo.getBytes(StandardCharsets.UTF_8),
                b = meio.getBytes(StandardCharsets.UTF_8),
                c = fim.getBytes(StandardCharsets.UTF_8);
        int padding = tamanho - a.length - arquivo.length - b.length - c.length;
        assertThat(padding).isPositive();
        var out = new ByteArrayOutputStream();
        out.write(a);
        out.write(arquivo);
        out.write(b);
        out.write("x".repeat(padding).getBytes(StandardCharsets.UTF_8));
        out.write(c);
        byte[] corpo = out.toByteArray();
        assertThat(arquivo.length).isEqualTo(5242880);
        assertThat(corpo.length).isEqualTo(tamanho);
        return http.send(
                request("/api/v1/armazens/" + armazemId + "/importacoes-enderecos/previa", gestor)
                        .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                        .POST(HttpRequest.BodyPublishers.ofByteArray(corpo))
                        .build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private byte[] excel(String caso, int linhas) throws Exception {
        try (var w = new XSSFWorkbook();
                var out = new ByteArrayOutputStream()) {
            var s = w.createSheet(caso.equals("abaErrada") ? "Outra" : "Enderecos");
            String[] cols = {
                "codigo",
                "rua",
                "nivel",
                "posicao",
                "descricao",
                "tipo",
                "sequenciaColeta",
                "tipoUnidadePermitido"
            };
            var h = s.createRow(0);
            for (int n = 0; n < cols.length; n++) h.createCell(n).setCellValue(cols[n]);
            if (caso.equals("formulaCabecalho")) h.getCell(0).setCellFormula("1+1");
            if (caso.equals("numeroCabecalho")) h.getCell(0).setCellValue(1);
            if (caso.equals("cabecalhoDuplicado")) h.getCell(1).setCellValue("codigo");
            for (int n = 1; n <= linhas; n++) {
                var r = s.createRow(n);
                r.createCell(0).setCellValue(caso.equals("duplicada") ? "IMP1" : "IMP" + n);
                r.createCell(1).setCellValue("R");
                r.createCell(2).setCellValue(0);
                r.createCell(3).setCellValue(String.format("%02d", n));
                r.createCell(4).setCellValue("Endereco ficticio");
                r.createCell(5).setCellValue("ARMAZENAGEM");
                r.createCell(6).setCellValue(n);
                if (n == 1) {
                    if (caso.startsWith("hyperlink")) {
                        if (caso.equals("hyperlinkVazio")) for (var cell : r) cell.setBlank();
                        var link = w.getCreationHelper().createHyperlink(HyperlinkType.URL);
                        link.setAddress("https://example.invalid");
                        r.getCell(4).setHyperlink(link);
                    }
                    if (caso.equals("formula")) r.getCell(4).setCellFormula("1+1");
                    if (caso.startsWith("identidade")) {
                        r.getCell(3)
                                .setCellValue(
                                        caso.equals("identidadeFracionadaFormatada") ? 1.5 : 1);
                        if (caso.endsWith("Formatada")) {
                            var style = w.createCellStyle();
                            style.setDataFormat(w.createDataFormat().getFormat("00"));
                            r.getCell(3).setCellStyle(style);
                        }
                    }
                    if (caso.equals("inteiroFracionario")) r.getCell(2).setCellValue(1.5);
                    if (caso.equals("tipoInvalido")) r.getCell(5).setCellValue("INFINITO");
                    if (caso.equals("capacidadeIncompleta")) r.createCell(7).setCellValue("PALLET");
                }
            }
            w.write(out);
            return out.toByteArray();
        }
    }

    private JsonNode previa(byte[] arquivo, String perfil) throws Exception {
        var antes = D30FotografiaFisica.capturar(jdbc);
        var resultado =
                resposta(
                        multipart(
                                arquivo,
                                Map.of(
                                        "operacaoId",
                                        UUID.randomUUID(),
                                        "motivo",
                                        "Previa ficticia validada"),
                                perfil),
                        200);
        var depois = D30FotografiaFisica.capturar(jdbc);
        for (String tabela : antes.keySet())
            if (!List.of("IMPORTACAO_ENDERECO", "AUDITORIA_CADASTRO", "OPERACAO_ADMINISTRATIVA")
                    .contains(tabela))
                assertThat(depois.get(tabela)).as(tabela).isEqualTo(antes.get(tabela));
        return resultado;
    }

    private Map<String, Object> confirmacao(JsonNode pre) {
        return new HashMap<>(
                Map.of(
                        "operacaoId",
                        UUID.randomUUID(),
                        "versao",
                        pre.get("versao").longValue(),
                        "arquivoHash",
                        pre.get("arquivoHash").asString(),
                        "motivo",
                        "Confirmacao integral ficticia"));
    }

    private Map<String, Object> enderecoComando(String codigo, String posicao) {
        return Map.of(
                "armazemId",
                armazemId,
                "codigo",
                codigo,
                "rua",
                "R",
                "nivel",
                0,
                "posicao",
                posicao,
                "descricao",
                "Endereco ficticio",
                "tipo",
                "ARMAZENAGEM",
                "sequenciaColeta",
                1);
    }

    private HttpResponse<String> multipart(byte[] arquivo, Object comando, String token)
            throws Exception {
        String boundary = "wms-" + UUID.randomUUID();
        var out = new ByteArrayOutputStream();
        out.write(
                ("--"
                                + boundary
                                + "\r\nContent-Disposition: form-data; name=\"comando\"\r\nContent-Type: application/json\r\n\r\n"
                                + mapper.writeValueAsString(comando)
                                + "\r\n--"
                                + boundary
                                + "\r\nContent-Disposition: form-data; name=\"arquivo\"; filename=\"enderecos.xlsx\"\r\nContent-Type: application/vnd.openxmlformats-officedocument.spreadsheetml.sheet\r\n\r\n")
                        .getBytes(StandardCharsets.UTF_8));
        out.write(arquivo);
        out.write(("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
        var b =
                request("/api/v1/armazens/" + armazemId + "/importacoes-enderecos/previa", token)
                        .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                        .POST(HttpRequest.BodyPublishers.ofByteArray(out.toByteArray()));
        return http.send(b.build(), HttpResponse.BodyHandlers.ofString());
    }

    private HashMap<String, Object> comando() {
        return new HashMap<>(
                Map.of("operacaoId", UUID.randomUUID(), "motivo", "Operacao ficticia validada"));
    }

    private int contar(String tabela) {
        return jdbc.queryForObject("select count(*) from wms." + tabela, Integer.class);
    }

    private HttpRequest.Builder request(String rota, String token) {
        var r =
                HttpRequest.newBuilder(
                                URI.create(
                                        "http://127.0.0.1:"
                                                + environment.getRequiredProperty(
                                                        "local.server.port")
                                                + rota))
                        .timeout(Duration.ofSeconds(30));
        if (token != null) r.header("Authorization", "Bearer " + token);
        return r;
    }

    private HttpResponse<String> post(String rota, Object d, String token) throws Exception {
        var d30Antes = D30ParesFinanceiros.antes(jdbc, mapper, rota, d, token);
        var resposta =
                http.send(
                        request(rota, token)
                                .header("Content-Type", "application/json")
                                .POST(
                                        HttpRequest.BodyPublishers.ofString(
                                                mapper.writeValueAsString(d)))
                                .build(),
                        HttpResponse.BodyHandlers.ofString());
        D30ParesFinanceiros.depois(jdbc, mapper, rota, d30Antes, resposta);
        return resposta;
    }

    private HttpResponse<String> get(String rota, String token) throws Exception {
        return http.send(request(rota, token).GET().build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> put(String rota, Object dados, String token) throws Exception {
        return http.send(
                request(rota, token)
                        .header("Content-Type", "application/json")
                        .PUT(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(dados)))
                        .build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private JsonNode resposta(HttpResponse<String> r, int status) {
        assertThat(r.statusCode()).as(r.body()).isEqualTo(status);
        return mapper.readTree(r.body());
    }

    private String token(String perfil, List<Long> clientes, List<Long> armazens) {
        var agora = Instant.now();
        var claims =
                JwtClaimsSet.builder()
                        .issuer("https://identidade.test.invalid")
                        .subject("cobranca-" + perfil)
                        .audience(List.of("wms-testes"))
                        .issuedAt(agora)
                        .expiresAt(agora.plusSeconds(900))
                        .claim("wms_perfil", perfil)
                        .claim("wms_clientes", clientes.stream().map(String::valueOf).toList())
                        .claim("wms_armazens", armazens.stream().map(String::valueOf).toList())
                        .build();
        return encoder.encode(
                        JwtEncoderParameters.from(
                                JwsHeader.with(SignatureAlgorithm.RS256).build(), claims))
                .getTokenValue();
    }

    private JsonNode d30GetFotografiaIntegral(String rota, String token) throws Exception {
        var antes = D30FotografiaFisica.capturar(jdbc);
        var resultado = resposta(get(rota, token), 200);
        assertThat(resultado.isNull()).as(rota + " resposta positiva").isFalse();
        assertThat(D30FotografiaFisica.capturar(jdbc))
                .as(rota + " conserva todas64tabelas")
                .isEqualTo(antes);
        return resultado;
    }

    @Test
    void d30GetCadastrosEConfiguracoesConservamFotografiaComDadosPertinentes() throws Exception {
        unidadeOperacional();
        var storage = servico("ARMAZENAGEM", "POSICAO_DIA");
        var tabela =
                tabela("D30-GET-TABELA", "2026-09-01", null, List.of(item(storage, "", "2.00")));
        vincular(tabela, "2026-09-01", null);
        String contexto = "?clienteId=" + clienteId + "&armazemId=" + armazemId;
        assertThat(
                        d30GetFotografiaIntegral("/api/v1/armazens/" + armazemId, gestor)
                                .get("id")
                                .longValue())
                .isEqualTo(armazemId);
        assertThat(
                        d30GetFotografiaIntegral("/api/v1/servicos-cobranca", gestor)
                                .get("totalItens")
                                .longValue())
                .isPositive();
        assertThat(d30GetFotografiaIntegral("/api/v1/tabelas-cobranca" + contexto, gestor).size())
                .isPositive();
        assertThat(
                        d30GetFotografiaIntegral(
                                        "/api/v1/tabelas-cobranca/" + tabela.get("id").longValue(),
                                        gestor)
                                .get("id"))
                .isEqualTo(tabela.get("id"));
        assertThat(d30GetFotografiaIntegral("/api/v1/vinculos-tabela" + contexto, gestor).size())
                .isPositive();
        assertThat(
                        d30GetFotografiaIntegral(
                                        "/api/v1/embalagens?produtoId=" + produtoId, gestor)
                                .get("totalItens")
                                .longValue())
                .isPositive();
        assertThat(
                        d30GetFotografiaIntegral("/api/v1/enderecos?armazemId=" + armazemId, gestor)
                                .get("totalItens")
                                .longValue())
                .isPositive();
        assertThat(
                        d30GetFotografiaIntegral("/api/v1/pedidos-entrada" + contexto, gestor)
                                .get("totalItens")
                                .longValue())
                .isPositive();
        assertThat(
                        d30GetFotografiaIntegral("/api/v1/produtos?clienteId=" + clienteId, gestor)
                                .get("totalItens")
                                .longValue())
                .isPositive();
    }

    @Test
    void d30ReativacaoCadastrosPreservaDadosFisicosEAuditoria() throws Exception {
        long endereco = enderecoOperacional(TipoEndereco.ARMAZENAGEM);
        var ids = Map.of("ARMAZEM", armazemId, "EMBALAGEM", embalagemId, "ENDERECO", endereco);
        for (String tabela : List.of("ARMAZEM", "EMBALAGEM", "ENDERECO")) {
            long id = ids.get(tabela);
            String colecao =
                    switch (tabela) {
                        case "ARMAZEM" -> "armazens";
                        case "EMBALAGEM" -> "embalagens";
                        default -> "enderecos";
                    };
            String rota = "/api/v1/" + colecao + "/" + id;
            assertThat(resposta(get(rota, gestor), 200).get("situacao").asString())
                    .isEqualTo("ATIVO");
            for (boolean reativar : new boolean[] {false, true}) {
                var original = resposta(get(rota, gestor), 200);
                String acao = reativar ? "REATIVACAO" : "SOLICITAR_ENCERRAMENTO";
                String destino = reativar ? "ATIVO" : "ENCERRAMENTO_PENDENTE";
                String comandoRota = rota + (reativar ? "/reativacao" : "/encerramento");
                var comando =
                        Map.<String, Object>of(
                                "versao",
                                original.get("versao").longValue(),
                                "motivo",
                                "Ciclo local de cadastro preservado");
                var antes = D30FotografiaFisica.capturar(jdbc);
                resposta(post(comandoRota, comando, supervisor), 403);
                assertThat(D30FotografiaFisica.capturar(jdbc)).isEqualTo(antes);
                if (reativar) {
                    var obsoleto = new HashMap<>(comando);
                    obsoleto.put("versao", original.get("versao").longValue() - 1);
                    resposta(post(comandoRota, obsoleto, gestor), 409);
                    assertThat(D30FotografiaFisica.capturar(jdbc)).isEqualTo(antes);
                }
                var recebido = post(comandoRota, comando, gestor);
                var depois = resposta(recebido, 200);
                assertThat(depois.get("id").longValue()).isEqualTo(id);
                assertThat(depois.get("situacao").asString()).isEqualTo(destino);
                assertThat(depois.get("versao").longValue())
                        .isEqualTo(original.get("versao").longValue() + 1);
                var a = (tools.jackson.databind.node.ObjectNode) original.deepCopy();
                var b = (tools.jackson.databind.node.ObjectNode) depois.deepCopy();
                for (String mutavel : List.of("situacao", "versao", "alteradoEm")) {
                    a.remove(mutavel);
                    b.remove(mutavel);
                }
                assertThat(b).isEqualTo(a);
                assertThat(resposta(get(rota, gestor), 200)).isEqualTo(depois);
                var fisico = D30FotografiaFisica.capturar(jdbc);
                d30AlvoConserva64(antes, fisico, tabela, id, "SITUACAO", destino, false);
                d30AuditoriaWrapper(
                        antes,
                        fisico,
                        tabela,
                        id,
                        acao,
                        original,
                        depois,
                        recebido.headers().firstValue("X-Request-Id").orElseThrow());
            }
        }
    }

    @Test
    void d30PutNomeArmazemEDescricaoEmbalagemExercitamCallersEAuditoriaCompleta() throws Exception {
        for (boolean armazem : new boolean[] {true, false}) {
            long id = armazem ? armazemId : embalagemId;
            String rota = (armazem ? "/api/v1/armazens/" : "/api/v1/embalagens/") + id;
            String campo = armazem ? "nome" : "descricao";
            String esperado = armazem ? "Armazem Cedro acao C" : "Embalagem Cedro acao C";
            var original = resposta(get(rota, gestor), 200);
            var comando =
                    Map.<String, Object>of(
                            "versao",
                            original.get("versao").longValue(),
                            campo,
                            esperado,
                            "motivo",
                            "Atualizacao local dos wrappers Cedro");
            var antes = D30FotografiaFisica.capturar(jdbc);
            var recebido = put(rota, comando, gestor);
            var depois = resposta(recebido, 200);
            assertThat(depois.get(campo).asString()).isEqualTo(esperado);
            assertThat(depois.get("id").longValue()).isEqualTo(id);
            assertThat(depois.get("versao").longValue())
                    .isEqualTo(original.get("versao").longValue() + 1);
            var anteriorConservado = (tools.jackson.databind.node.ObjectNode) original.deepCopy();
            var depoisConservado = (tools.jackson.databind.node.ObjectNode) depois.deepCopy();
            for (String mutavel : List.of(campo, "versao", "alteradoEm")) {
                anteriorConservado.remove(mutavel);
                depoisConservado.remove(mutavel);
            }
            assertThat(depoisConservado).isEqualTo(anteriorConservado);
            var fisico = D30FotografiaFisica.capturar(jdbc);
            d30AlvoConserva64(
                    antes,
                    fisico,
                    armazem ? "ARMAZEM" : "EMBALAGEM",
                    id,
                    campo.toUpperCase(java.util.Locale.ROOT),
                    esperado,
                    false);
            d30AuditoriaWrapper(
                    antes,
                    fisico,
                    armazem ? "ARMAZEM" : "EMBALAGEM",
                    id,
                    "ALTERACAO",
                    original,
                    depois,
                    recebido.headers().firstValue("X-Request-Id").orElseThrow());
            assertThat(fisico.get("OPERACAO_ADMINISTRATIVA"))
                    .isEqualTo(antes.get("OPERACAO_ADMINISTRATIVA"));
        }
    }

    @Test
    void d30EncerramentosContratoEVinculoExercitamWrappersRollbackEReplay() throws Exception {
        var storage = servico("ARMAZENAGEM", "POSICAO_DIA");
        var tabela = tabela("D30-WRAPPERS", "2026-09-01", null, List.of(item(storage, "", "2.00")));
        var vinculo = vincular(tabela, "2026-09-01", null);
        var contrato =
                contrato("NAO_APLICAVEL", null, List.of(), "NAO_APLICAVEL", null, null, null);
        var alvoOperacoes =
                org.springframework.test.util.AopTestUtils
                        .<br.com.rodogarcia.wms.services.OperacaoAdministrativaService>
                                getUltimateTargetObject(operacoesD30);
        for (boolean contratoAlvo : new boolean[] {true, false}) {
            var original = contratoAlvo ? contrato : vinculo;
            long id = original.get("id").longValue();
            String rota =
                    (contratoAlvo ? "/api/v1/contratos-cobranca/" : "/api/v1/vinculos-tabela/")
                            + id
                            + "/encerramento";
            var d = comando();
            d.put("versao", original.get("versao").longValue());
            d.put("vigenciaFim", "2026-10-31");
            var foto = D30FotografiaFisica.capturar(jdbc);
            resposta(post(rota, d, supervisor), 403);
            assertThat(D30FotografiaFisica.capturar(jdbc)).isEqualTo(foto);
            var obsoleto = new HashMap<>(d);
            obsoleto.put("versao", original.get("versao").longValue() + 1);
            resposta(post(rota, obsoleto, gestor), 409);
            assertThat(D30FotografiaFisica.capturar(jdbc)).isEqualTo(foto);
            if (contratoAlvo) {
                var foraCorte = new HashMap<>(d);
                foraCorte.put("vigenciaFim", "2026-09-02");
                var recusado = resposta(post(rota, foraCorte, gestor), 409);
                assertThat(recusado.get("codigo").asString()).isEqualTo("ALTERACAO_FORA_CORTE");
                assertThat(D30FotografiaFisica.capturar(jdbc)).isEqualTo(foto);
            }
            try {
                doAnswer(
                                inv -> {
                                    throw new IllegalStateException(
                                            "Falha ficticia auditoria wrapper D30");
                                })
                        .when(alvoAuditoria)
                        .registrar(any(), any(), any(), any(), any(), any());
                resposta(post(rota, d, gestor), 500);
                assertThat(D30FotografiaFisica.capturar(jdbc)).isEqualTo(foto);
            } finally {
                reset(alvoAuditoria);
            }
            try {
                doAnswer(
                                inv -> {
                                    throw new IllegalStateException(
                                            "Falha ficticia operacao wrapper D30");
                                })
                        .when(alvoOperacoes)
                        .salvar(any(), any(), any(), any(), any(), any(), any());
                resposta(post(rota, d, gestor), 500);
                assertThat(D30FotografiaFisica.capturar(jdbc)).isEqualTo(foto);
            } finally {
                reset(alvoOperacoes);
            }
            var recebido = post(rota, d, gestor);
            var encerrado = resposta(recebido, 200);
            assertThat(encerrado.get("id").longValue()).isEqualTo(id);
            assertThat(encerrado.get("versao").longValue())
                    .isEqualTo(original.get("versao").longValue() + 1);
            assertThat(encerrado.get("vigenciaFim").asString()).isEqualTo("2026-10-31");
            var originalConservado = (tools.jackson.databind.node.ObjectNode) original.deepCopy();
            var encerradoConservado = (tools.jackson.databind.node.ObjectNode) encerrado.deepCopy();
            for (String campo : List.of("versao", "vigenciaFim")) {
                originalConservado.remove(campo);
                encerradoConservado.remove(campo);
            }
            assertThat(encerradoConservado).isEqualTo(originalConservado);
            var depois = D30FotografiaFisica.capturar(jdbc);
            String tabelaAlvo = contratoAlvo ? "CONTRATO_COBRANCA" : "VINCULO_TABELA_CLIENTE";
            d30AlvoConserva64(
                    foto,
                    depois,
                    tabelaAlvo,
                    id,
                    "VIGENCIA_FIM",
                    java.sql.Date.valueOf("2026-10-31"),
                    true);
            d30AuditoriaWrapper(
                    foto,
                    depois,
                    "CONTRATO_COBRANCA",
                    id,
                    contratoAlvo ? "CONFIGURACAO" : "VINCULO_TABELA",
                    original,
                    encerrado,
                    recebido.headers().firstValue("X-Request-Id").orElseThrow());
            var ops =
                    depois.get("OPERACAO_ADMINISTRATIVA").stream()
                            .filter(x -> !foto.get("OPERACAO_ADMINISTRATIVA").contains(x))
                            .toList();
            assertThat(ops).hasSize(1);
            var op = ops.get(0);
            assertThat(op.get("OPERACAO_ID")).isEqualTo(d.get("operacaoId").toString());
            assertThat(op.get("TIPO")).isEqualTo(contratoAlvo ? "CONFIGURACAO" : "VINCULO_TABELA");
            assertThat(((Number) op.get("RECURSO_ID")).longValue()).isEqualTo(id);
            assertThat(((Number) op.get("CLIENTE_ID")).longValue()).isEqualTo(clienteId);
            assertThat(((Number) op.get("ARMAZEM_ID")).longValue()).isEqualTo(armazemId);
            assertThat(op.get("USUARIO")).isEqualTo("cobranca-GESTOR");
            assertThat(mapper.readTree(op.get("RESULTADO").toString())).isEqualTo(encerrado);
            assertThat(resposta(post(rota, d, gestor), 200)).isEqualTo(encerrado);
            assertThat(D30FotografiaFisica.capturar(jdbc)).isEqualTo(depois);
            var divergente = new HashMap<>(d);
            divergente.put("motivo", "Conteudo divergente neste replay");
            resposta(post(rota, divergente, gestor), 409);
            assertThat(D30FotografiaFisica.capturar(jdbc)).isEqualTo(depois);
            var ampliado = new HashMap<>(d);
            ampliado.put("operacaoId", UUID.randomUUID());
            ampliado.put("versao", encerrado.get("versao").longValue());
            ampliado.put("vigenciaFim", "2026-11-30");
            assertThat(resposta(post(rota, ampliado, gestor), 409).get("codigo").asString())
                    .isEqualTo("VIGENCIA_NAO_EXTENSIVEL");
            assertThat(D30FotografiaFisica.capturar(jdbc)).isEqualTo(depois);
        }
    }

    private void d30AlvoConserva64(
            Map<String, List<Map<String, Object>>> antes,
            Map<String, List<Map<String, Object>>> depois,
            String tabela,
            long id,
            String coluna,
            Object esperado,
            boolean operacao) {
        assertThat(antes).hasSize(64);
        assertThat(depois.keySet()).isEqualTo(antes.keySet());
        for (String nome : antes.keySet()) {
            if (nome.equals(tabela)
                    || nome.equals("AUDITORIA_CADASTRO")
                    || (operacao && nome.equals("OPERACAO_ADMINISTRATIVA"))) continue;
            assertThat(depois.get(nome)).as(nome).isEqualTo(antes.get(nome));
        }
        var original =
                antes.get(tabela).stream()
                        .filter(x -> ((Number) x.get("ID")).longValue() == id)
                        .findFirst()
                        .orElseThrow();
        var alvo =
                depois.get(tabela).stream()
                        .filter(x -> ((Number) x.get("ID")).longValue() == id)
                        .findFirst()
                        .orElseThrow();
        assertThat(depois.get(tabela)).hasSameSizeAs(antes.get(tabela));
        assertThat(
                        depois.get(tabela).stream()
                                .filter(x -> ((Number) x.get("ID")).longValue() != id)
                                .toList())
                .isEqualTo(
                        antes.get(tabela).stream()
                                .filter(x -> ((Number) x.get("ID")).longValue() != id)
                                .toList());
        if (esperado instanceof java.sql.Date data) {
            assertThat(alvo.get(coluna)).isInstanceOf(D30FotografiaTemporal.Campo.class);
            var campo = (D30FotografiaTemporal.Campo) alvo.get(coluna);
            assertThat(campo.coluna()).isEqualTo(coluna);
            assertThat(campo.tipoSQL()).isEqualTo("DATE");
            assertThat(campo.codigoSQL()).isEqualTo(java.sql.Types.DATE);
            assertThat(campo.escala()).isZero();
            assertThat(campo.nulo()).isFalse();
            assertThat(campo.local()).isEqualTo(data.toLocalDate().toString());
            assertThat(campo.utc()).isNull();
            assertThat(campo.nanos()).isNull();
            assertThat(campo.micros()).isNull();
            assertThat(campo.calendario()).isNull();
            assertThat(campo.semantica()).isEqualTo("SQL_DATE;DATA_CIVIL_SEM_INSTANT_OU_ZONA");
        } else {
            assertThat(alvo.get(coluna)).isEqualTo(esperado);
        }
        assertThat(((Number) alvo.get("VERSAO")).longValue())
                .isEqualTo(((Number) original.get("VERSAO")).longValue() + 1);
        String colunaAlteracao = tabela.equals("CONTRATO_COBRANCA") ? "ALTERADA_EM" : "ALTERADO_EM";
        Instant instanteUtc =
                jdbc.query(
                        con -> {
                            var consulta =
                                    con.prepareStatement(
                                            "select "
                                                    + colunaAlteracao.toLowerCase(
                                                            java.util.Locale.ROOT)
                                                    + " from wms."
                                                    + tabela.toLowerCase(java.util.Locale.ROOT)
                                                    + " where id=?");
                            consulta.setLong(1, id);
                            return consulta;
                        },
                        rs -> {
                            assertThat(rs.next()).isTrue();
                            return rs.getTimestamp(
                                            1,
                                            java.util.Calendar.getInstance(
                                                    java.util.TimeZone.getTimeZone("UTC")))
                                    .toInstant();
                        });
        assertThat(instanteUtc).isEqualTo(Instant.parse("2026-10-06T12:00:00Z"));
        var a = new HashMap<>(original);
        var b = new HashMap<>(alvo);
        for (String mutavel : List.of(coluna, "VERSAO", colunaAlteracao)) {
            a.remove(mutavel);
            b.remove(mutavel);
        }
        assertThat(b).isEqualTo(a);
    }

    private void d30AuditoriaWrapper(
            Map<String, List<Map<String, Object>>> antes,
            Map<String, List<Map<String, Object>>> depois,
            String tipo,
            long id,
            String acao,
            JsonNode original,
            JsonNode resultado,
            String requestId)
            throws Exception {
        assertThat(depois.get("AUDITORIA_CADASTRO")).containsAll(antes.get("AUDITORIA_CADASTRO"));
        var novas =
                depois.get("AUDITORIA_CADASTRO").stream()
                        .filter(x -> !antes.get("AUDITORIA_CADASTRO").contains(x))
                        .toList();
        assertThat(novas).hasSize(1);
        var audit = novas.get(0);
        assertThat(audit.get("TIPO")).isEqualTo(tipo);
        assertThat(((Number) audit.get("REGISTRO_ID")).longValue()).isEqualTo(id);
        assertThat(audit.get("ACAO")).isEqualTo(acao);
        assertThat(audit.get("USUARIO")).isEqualTo("cobranca-GESTOR");
        assertThat(audit.get("ID_OPERACAO")).isEqualTo(requestId);
        assertThat(mapper.readTree(audit.get("DADOS_ANTES").toString())).isEqualTo(original);
        assertThat(mapper.readTree(audit.get("DADOS_DEPOIS").toString())).isEqualTo(resultado);
    }
}
