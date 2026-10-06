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
    @Autowired JsonMapper mapper;
    @Autowired JwtEncoder encoder;
    @Autowired JdbcTemplate jdbc;
    @Autowired PlatformTransactionManager transactions;
    @Autowired FiscalCadastroService fiscal;
    @Autowired ImportacaoEnderecoService importacao;
    @MockitoSpyBean AuditoriaService auditoria;
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
        alvoAuditoria =
                org.springframework.test.util.AopTestUtils.getUltimateTargetObject(auditoria);
        reset(alvoAuditoria);
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

    private void contrato(
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
        resposta(post("/api/v1/contratos-cobranca", d, gestor), 200);
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
        return resposta(
                multipart(
                        arquivo,
                        Map.of(
                                "operacaoId",
                                UUID.randomUUID(),
                                "motivo",
                                "Previa ficticia validada"),
                        perfil),
                200);
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
        return http.send(
                request(rota, token)
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(d)))
                        .build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> get(String rota, String token) throws Exception {
        return http.send(request(rota, token).GET().build(), HttpResponse.BodyHandlers.ofString());
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
}
