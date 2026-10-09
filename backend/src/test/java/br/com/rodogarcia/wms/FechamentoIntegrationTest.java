package br.com.rodogarcia.wms;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import br.com.rodogarcia.wms.config.IdentidadeTesteConfig;
import br.com.rodogarcia.wms.models.Armazem;
import br.com.rodogarcia.wms.models.ChegadaRecebimento;
import br.com.rodogarcia.wms.models.Cliente;
import br.com.rodogarcia.wms.models.CondicaoMercadoria;
import br.com.rodogarcia.wms.models.ConteudoUnidade;
import br.com.rodogarcia.wms.models.Embalagem;
import br.com.rodogarcia.wms.models.Endereco;
import br.com.rodogarcia.wms.models.EntradaConferida;
import br.com.rodogarcia.wms.models.ItemChegada;
import br.com.rodogarcia.wms.models.ItemNotaEntrada;
import br.com.rodogarcia.wms.models.MedidasUnidade;
import br.com.rodogarcia.wms.models.NotaEntrada;
import br.com.rodogarcia.wms.models.PedidoEntrada;
import br.com.rodogarcia.wms.models.Produto;
import br.com.rodogarcia.wms.models.TipoEndereco;
import br.com.rodogarcia.wms.models.TipoQuantidade;
import br.com.rodogarcia.wms.models.TipoUnidadeLogistica;
import br.com.rodogarcia.wms.models.UnidadeLogistica;
import br.com.rodogarcia.wms.services.AuditoriaService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
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
                "spring.datasource.url=jdbc:h2:mem:wms-fechamento;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000;INIT=CREATE SCHEMA IF NOT EXISTS wms")
@Import(IdentidadeTesteConfig.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class FechamentoIntegrationTest {
    @Autowired Environment environment;
    @Autowired JsonMapper mapper;
    @Autowired JwtEncoder encoder;
    @Autowired JdbcTemplate jdbc;
    @Autowired PlatformTransactionManager transactions;
    @MockitoSpyBean AuditoriaService auditoria;
    @MockitoSpyBean br.com.rodogarcia.wms.services.AcessoService acessoD30;
    private br.com.rodogarcia.wms.services.AcessoService alvoAcessoD30;
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
        when(clock.instant()).thenReturn(Instant.parse("2028-06-06T12:00:00Z"));
        when(clock.getZone()).thenReturn(java.time.ZoneOffset.UTC);
        when(clock.withZone(any(java.time.ZoneId.class)))
                .thenAnswer(inv -> Clock.fixed(clock.instant(), inv.getArgument(0)));
        alvoAuditoria =
                org.springframework.test.util.AopTestUtils.getUltimateTargetObject(auditoria);
        reset(alvoAuditoria);
        alvoAcessoD30 =
                org.springframework.test.util.AopTestUtils.getUltimateTargetObject(acessoD30);
        reset(alvoAcessoD30);
        SecurityContextHolder.clearContext();
        jdbc.update("update wms.unidade_logistica set reserva_saida_id=null");
        for (String t :
                List.of(
                        "resolucao_financeira_fechamento",
                        "referencia_nfse",
                        "confirmacao_externa_fechamento",
                        "entrega_esl",
                        "ajuste_versao_fechamento",
                        "ajuste_fechamento",
                        "tratativa_externa_fechamento",
                        "fato_fechamento",
                        "dia_fechamento",
                        "versao_fechamento",
                        "fechamento_cobranca",
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

    @ParameterizedTest
    @org.junit.jupiter.params.provider.CsvSource({
        "2027,29,2027-02-28,2027-03-29",
        "2027,30,2027-02-28,2027-03-30",
        "2027,31,2027-02-28,2027-03-31",
        "2028,29,2028-02-29,2028-03-29",
        "2028,30,2028-02-29,2028-03-30",
        "2028,31,2028-02-29,2028-03-31"
    })
    void calendarioConservaNominalEmFevereiro(int ano, int nominal, String fevereiro, String marco)
            throws Exception {
        String inicio = ano + "-01-" + nominal;
        configurar(inicio, "MES_DIA_FIXO", nominal, null, null);
        var f = preparar(calcular(inicio, fevereiro));
        var f2 = preparar(calcular(fevereiro, marco));
        assertThat(f2.get("fechamento").get("periodoInicio").asString()).isEqualTo(fevereiro);
        var memoria = resposta(get(rota(f) + "/versoes/1/demonstrativo", supervisor), 200);
        assertThat(memoria.get("ciclo").get("diaNominal").asInt()).isEqualTo(nominal);
        assertThat(contar("dia_fechamento"))
                .isEqualTo(
                        (int)
                                java.time.temporal.ChronoUnit.DAYS.between(
                                        LocalDate.parse(inicio), LocalDate.parse(marco)));
    }

    @Test
    void trintaDiasPreservaAncoraEPrimeiroParcialMensal() throws Exception {
        configurar("2027-01-31", "DIAS_CORRIDOS", null, 30, null);
        preparar(calcular("2027-01-31", "2027-03-02"));
        var f2 = preparar(calcular("2027-03-02", "2027-04-01"));
        assertThat(f2.get("fechamento").get("periodoFim").asString()).isEqualTo("2027-04-01");
        resposta(
                post(
                        "/api/v1/fechamentos-cobranca",
                        prepararComando(calcular("2027-04-02", "2027-05-02")),
                        gestor),
                409);
        assertThat(contar("fechamento_cobranca")).isEqualTo(2);
    }

    @Test
    void primeiroCicloParcialEContratoContiguoSeguemCorte() throws Exception {
        configurar("2027-01-15", "MES_DIA_FIXO", 31, null, "2027-01-31");
        configurarContrato("2027-01-31", "DIAS_CORRIDOS", null, 30, null);
        var f = preparar(calcular("2027-01-15", "2027-01-31"));
        var f2 = preparar(calcular("2027-01-31", "2027-03-02"));
        assertThat(f.get("fechamento").get("contratoId").longValue())
                .isNotEqualTo(f2.get("fechamento").get("contratoId").longValue());
        assertThat(
                        resposta(get(rota(f) + "/versoes/1/demonstrativo", supervisor), 200)
                                .get("ciclo")
                                .get("cicloInicio")
                                .asString())
                .isEqualTo("2026-12-31");
    }

    @Test
    void memoriaBytesHashNaoMudamComAprovacaoEntregaDeclaracaoOuDocumento() throws Exception {
        configurar("2026-09-01", "DIAS_CORRIDOS", null, 30, null);
        fato("10", "2026-09-02T12:00:00Z");
        var f = preparar(calcular("2026-09-01", "2026-10-01"));
        String arquivo = get(rota(f) + "/versoes/1/demonstrativo", supervisor).body();
        assertThat(
                        br.com.rodogarcia.wms.services.OperacaoAdministrativaService.digest(
                                arquivo.getBytes(StandardCharsets.UTF_8)))
                .isEqualTo(f.get("versao").get("conteudoHash").asString());
        f = aprovar(f);
        f = entregar(f);
        assertThat(f.get("versao").get("estadoExterno").asString()).isEqualTo("DESCONHECIDO");
        f = confirmar(f);
        f = nfse(f, "DOC1");
        assertThat(get(rota(f) + "/versoes/1/demonstrativo", supervisor).body()).isEqualTo(arquivo);
        assertThat(f.get("fechamento").get("situacao").asString()).isEqualTo("EMITIDO");
        assertThat(contar("calculo_cobranca")).isEqualTo(1);
        assertThat(contar("movimento_estoque")).isZero();
        assertThat(
                        d30GetFotografiaIntegral(
                                        "/api/v1/fechamentos-cobranca?clienteId="
                                                + clienteId
                                                + "&armazemId="
                                                + armazemId,
                                        supervisor)
                                .get("totalItens")
                                .longValue())
                .isPositive();
        assertThat(d30GetFotografiaIntegral(rota(f) + "/versoes", supervisor).size()).isEqualTo(1);
    }

    @Test
    void calculoPendenteEPeriodoFuturoImpedemAprovacao() throws Exception {
        configurar("2026-09-01", "DIAS_CORRIDOS", null, 30, null);
        jdbc.update("update wms.contrato_cobranca set minimo_modo='NAO_INFORMADO'");
        var f = preparar(calcular("2026-09-01", "2026-10-01"));
        assertThat(f.get("versao").get("saldo").isNull()).isTrue();
        resposta(post(rota(f) + "/aprovacao", decisao(f), gestor), 409);
        jdbc.update("update wms.contrato_cobranca set minimo_modo='NAO_APLICAVEL'");
        when(clock.instant()).thenReturn(Instant.parse("2026-09-15T12:00:00Z"));
        f = reabrir(f, calcular("2026-09-01", "2026-10-01"));
        resposta(post(rota(f) + "/aprovacao", decisao(f), gestor), 409);
    }

    @Test
    void fatoPosteriorInvalidaAprovacaoSemPersistirCalculoExtra() throws Exception {
        configurar("2026-09-01", "DIAS_CORRIDOS", null, 30, null);
        var f = preparar(calcular("2026-09-01", "2026-10-01"));
        fato("10", "2026-09-02T12:00:00Z");
        var r = resposta(post(rota(f) + "/aprovacao", decisao(f), gestor), 409);
        assertThat(r.toString()).contains("CALCULO_DESATUALIZADO");
        assertThat(contar("calculo_cobranca")).isEqualTo(1);
        f = reabrir(f, calcular("2026-09-01", "2026-10-01"));
        f = aprovar(f);
        assertThat(f.get("versao").get("saldo").decimalValue()).isEqualByComparingTo("100");
        assertThat(contar("fato_fechamento")).isEqualTo(1);
    }

    @Test
    void repeticaoPerfilAlcancePayloadERespostaOriginal() throws Exception {
        configurar("2026-09-01", "DIAS_CORRIDOS", null, 30, null);
        var d = prepararComando(calcular("2026-09-01", "2026-10-01"));
        var f = resposta(post("/api/v1/fechamentos-cobranca", d, gestor), 200);
        var a = decisao(f);
        var aprovado = resposta(post(rota(f) + "/aprovacao", a, gestor), 200);
        assertThat(resposta(post("/api/v1/fechamentos-cobranca", d, gestor), 200)).isEqualTo(f);
        assertThat(resposta(post(rota(f) + "/aprovacao", a, gestor), 200)).isEqualTo(aprovado);
        for (String perfil : List.of(supervisor, operador))
            resposta(post(rota(f) + "/aprovacao", a, perfil), 403);
        d.put("motivo", "Motivo divergente comprovado");
        resposta(post("/api/v1/fechamentos-cobranca", d, gestor), 409);
        resposta(get(rota(f), null), 401);
        resposta(get(rota(f), operador), 403);
        resposta(
                get(rota(f), token("SUPERVISOR", List.of(clienteId + 999), List.of(armazemId))),
                403);
        assertThat(contar("dia_fechamento")).isEqualTo(30);
    }

    @Test
    void entregaDesconhecidaExigeDeclaracaoEReaberturaPreservaVersao() throws Exception {
        configurar("2026-09-01", "DIAS_CORRIDOS", null, 30, null);
        var f = aprovar(preparar(calcular("2026-09-01", "2026-10-01")));
        f = entregar(f);
        var novo = calcular("2026-09-01", "2026-10-01");
        resposta(post(rota(f) + "/reabertura", reabrirComando(f, novo), gestor), 409);
        f = confirmar(f);
        f = reabrir(f, novo);
        assertThat(f.get("versao").get("numero").asInt()).isEqualTo(2);
        var v1 = resposta(get(rota(f) + "/versoes/1", supervisor), 200);
        assertThat(v1.get("situacao").asString()).isEqualTo("SUPERADA");
        assertThat(v1.get("decisor").isNull()).isFalse();
        assertThat(v1.get("entregas").size()).isEqualTo(1);
        var antigo = entregaComando(f);
        antigo.put("numero", 1);
        resposta(post(rota(f) + "/entregas", antigo, gestor), 409);
        assertThat(contar("dia_fechamento")).isEqualTo(30);
    }

    @Test
    void nfseTardiaV1ConflitaV2ReconciliacaoIntegralEOrigemSuperadaElegivel() throws Exception {
        configurar("2026-09-01", "DIAS_CORRIDOS", null, 30, null);
        var fato = fato("10", "2026-09-02T12:00:00Z");
        var f = preparar(calcular("2026-09-01", "2026-10-01"));
        var aprovacaoHistorica = decisao(f);
        aprovacaoHistorica.put(
                "resolucao",
                Map.of(
                        "tipo",
                        "FECHAMENTO",
                        "compromissoId",
                        f.get("fechamento").get("id").longValue()));
        f = resposta(post(rota(f) + "/aprovacao", aprovacaoHistorica, gestor), 200);
        f = entregar(f);
        f = confirmar(f);
        f = reabrir(f, calcular("2026-09-01", "2026-10-01"));
        f = nfseNumero(f, 1, "ANTIGA");
        assertThat(f.get("fechamento").get("situacao").asString()).isEqualTo("CONFLITO_EXTERNO");
        resposta(post(rota(f) + "/aprovacao", decisaoNumero(f, 2), gestor), 409);
        var documentos = resposta(get(rota(f) + "/versoes/1", supervisor), 200).get("nfse");
        var trat =
                tratativaComando(
                        f,
                        1,
                        "EMITIDO",
                        List.of(
                                Map.of(
                                        "referenciaId",
                                        documentos.get(0).get("id").longValue(),
                                        "situacao",
                                        "MANTIDO")));
        f = resposta(post(rota(f) + "/tratativas-externas", trat, gestor), 200);
        assertThat(f.get("fechamento").get("situacao").asString()).isEqualTo("EMITIDO");
        assertThat(f.get("versao").get("situacao").asString()).isEqualTo("SUPERADA");
        anular(fato);
        var corrigido = calcular("2026-09-01", "2026-10-01");
        var destino = preparar(calcular("2026-10-01", "2026-10-31"));
        var a =
                resposta(
                        post(
                                "/api/v1/ajustes-fechamento",
                                ajusteComando(f, destino, corrigido),
                                gestor),
                        200);
        assertThat(a.get("diferenca").decimalValue()).isEqualByComparingTo("-100");
    }

    @Test
    void
            encerramentoReconheceSuperadaEmitidaReconciliadaERecusaCorteDeTabelaOuVinculoComprometidos()
                    throws Exception {
        var servico = configurar("2026-09-01", "DIAS_CORRIDOS", null, 30, null);
        fato("10", "2026-09-02T12:00:00Z");
        var f = confirmar(entregar(aprovar(preparar(calcular("2026-09-01", "2026-10-01")))));
        f = reabrir(f, calcular("2026-09-01", "2026-10-01"));
        f = nfseNumero(f, 1, "BE14TARDIA");
        long documento =
                resposta(get(rota(f) + "/versoes/1", supervisor), 200)
                        .get("nfse")
                        .get(0)
                        .get("id")
                        .longValue();
        f =
                resposta(
                        post(
                                rota(f) + "/tratativas-externas",
                                tratativaComando(
                                        f,
                                        1,
                                        "EMITIDO",
                                        List.of(
                                                Map.of(
                                                        "referenciaId",
                                                        documento,
                                                        "situacao",
                                                        "MANTIDO"))),
                                gestor),
                        200);
        assertThat(f.get("versao").get("situacao").asString()).isEqualTo("SUPERADA");
        var ref =
                Map.of(
                        "tipo",
                        "FECHAMENTO",
                        "compromissoId",
                        f.get("fechamento").get("id").longValue());
        for (String tipo : List.of("TABELA", "VINCULO")) {
            String tabela = tipo.equals("TABELA") ? "tabela_cobranca" : "vinculo_tabela_cliente";
            long id = jdbc.queryForObject("select id from wms." + tabela, Long.class);
            var d = comando();
            d.put(
                    "versao",
                    jdbc.queryForObject(
                            "select versao from wms." + tabela + " where id=?", Long.class, id));
            d.put("corte", "2026-09-02");
            d.put("resolucao", ref);
            assertThat(
                            resposta(
                                            post(
                                                    "/api/v1/encerramentos/vigencias/"
                                                            + tipo
                                                            + "/"
                                                            + id
                                                            + "/encerrar",
                                                    d,
                                                    gestor),
                                            409)
                                    .get("codigo")
                                    .asString())
                    .isEqualTo("VIGENCIA_COMPROMETIDA");
            assertThat(
                            jdbc.queryForObject(
                                    "select vigencia_fim from wms." + tabela + " where id=?",
                                    java.sql.Date.class,
                                    id))
                    .isNull();
        }
        var tab = jdbc.queryForObject("select id from wms.tabela_cobranca", Long.class);
        var d = comando();
        d.put(
                "versao",
                jdbc.queryForObject(
                        "select versao from wms.tabela_cobranca where id=?", Long.class, tab));
        d.put("corte", "2026-09-03");
        d.put("resolucao", ref);
        var corte =
                resposta(
                        post(
                                "/api/v1/encerramentos/vigencias/TABELA/" + tab + "/encerrar",
                                d,
                                gestor),
                        200);
        assertThat(
                        resposta(
                                post(
                                        "/api/v1/encerramentos/vigencias/TABELA/"
                                                + tab
                                                + "/encerrar",
                                        d,
                                        gestor),
                                200))
                .isEqualTo(corte);
        resposta(
                post("/api/v1/encerramentos/vigencias/TABELA/" + tab + "/encerrar", d, supervisor),
                403);
        long sid = servico.get("id").longValue();
        var pendentes =
                resposta(
                        get(
                                "/api/v1/encerramentos/SERVICO_COBRANCA/" + sid + "/impedimentos",
                                gestor),
                        200);
        assertThat(pendentes.get("impedimentos").isEmpty()).isTrue();
    }

    @Test
    void tabelaPadraoPodeEncerrarNoArmazemPendenteComEscopoIntegralEProva() throws Exception {
        configurar("2026-09-01", "DIAS_CORRIDOS", null, 30, null);
        fato("10", "2026-09-02T12:00:00Z");
        var f = nfse(aprovar(preparar(calcular("2026-09-01", "2026-10-01"))), "BE14PADRAO");
        var tabela = jdbc.queryForObject("select id from wms.tabela_cobranca", Long.class);
        jdbc.update(
                "update wms.tabela_cobranca set cliente_id=null,tipo='PADRAO' where id=?", tabela);
        long segundoCliente =
                new TransactionTemplate(transactions)
                        .execute(
                                tx -> {
                                    var c =
                                            new Cliente(
                                                    "D30-MULTI",
                                                    "Segundo proprietario ficticio",
                                                    "33333333000133",
                                                    BASE);
                                    em.persist(c);
                                    em.flush();
                                    em.persist(
                                            new br.com.rodogarcia.wms.models.VinculoTabelaCliente(
                                                    c,
                                                    em.find(Armazem.class, armazemId),
                                                    em.find(
                                                            br.com.rodogarcia.wms.models
                                                                    .TabelaCobranca.class,
                                                            tabela),
                                                    LocalDate.parse("2026-09-01"),
                                                    null,
                                                    BASE,
                                                    BASE));
                                    em.flush();
                                    return c.getId();
                                });
        var solicitar = comando();
        solicitar.put(
                "versao",
                jdbc.queryForObject(
                        "select versao from wms.armazem where id=?", Long.class, armazemId));
        resposta(
                post(
                        "/api/v1/encerramentos/ARMAZEM/" + armazemId + "/solicitar",
                        solicitar,
                        gestor),
                200);
        var d = comando();
        d.put(
                "versao",
                jdbc.queryForObject(
                        "select versao from wms.tabela_cobranca where id=?", Long.class, tabela));
        d.put("corte", "2026-09-03");
        d.put(
                "resolucao",
                Map.of(
                        "tipo",
                        "FECHAMENTO",
                        "compromissoId",
                        f.get("fechamento").get("id").longValue()));
        var ordemClientes = new java.util.concurrent.CopyOnWriteArrayList<Long>();
        doAnswer(
                        inv -> {
                            ordemClientes.add(inv.getArgument(0));
                            return inv.callRealMethod();
                        })
                .when(alvoAcessoD30)
                .cliente(org.mockito.ArgumentMatchers.anyLong());
        var fotoCorte = D30FotografiaFisica.capturar(jdbc);
        var r =
                resposta(
                        post(
                                "/api/v1/encerramentos/vigencias/TABELA/" + tabela + "/encerrar",
                                d,
                                gestor),
                        200);
        assertThat(ordemClientes)
                .containsExactlyElementsOf(
                        java.util.stream.Stream.of(clienteId, segundoCliente).sorted().toList());
        var depoisCorte = D30FotografiaFisica.capturar(jdbc);
        for (String t : fotoCorte.keySet())
            if (!List.of("TABELA_COBRANCA", "AUDITORIA_CADASTRO", "OPERACAO_ADMINISTRATIVA")
                    .contains(t)) assertThat(depoisCorte.get(t)).as(t).isEqualTo(fotoCorte.get(t));
        assertThat(r.get("fim").asString()).isEqualTo("2026-09-03");
        assertThat(
                        jdbc.queryForObject(
                                "select cliente_id from wms.tabela_cobranca where id=?",
                                Long.class,
                                tabela))
                .isNull();
        assertThat(
                        jdbc.queryForObject(
                                "select situacao from wms.armazem where id=?",
                                String.class,
                                armazemId))
                .isEqualTo("ENCERRAMENTO_PENDENTE");
        resposta(
                post(
                        "/api/v1/encerramentos/vigencias/TABELA/" + tabela + "/encerrar",
                        d,
                        supervisor),
                403);
        assertThat(
                        resposta(
                                post(
                                        "/api/v1/encerramentos/vigencias/TABELA/"
                                                + tabela
                                                + "/encerrar",
                                        d,
                                        gestor),
                                200))
                .isEqualTo(r);
    }

    @Test
    void duasNfseDaMesmaVersaoSaoPreservadasETratativaExigeTodasSemEmitir() throws Exception {
        configurar("2026-09-01", "DIAS_CORRIDOS", null, 30, null);
        fato("10", "2026-09-02T12:00:00Z");
        var f = aprovar(preparar(calcular("2026-09-01", "2026-10-01")));
        f = entregar(f);
        f = entregar(f);
        f = nfse(f, "PRIMEIRA");
        f = nfse(f, "SEGUNDA");
        assertThat(f.get("versao").get("nfse").size()).isEqualTo(2);
        assertThat(f.get("fechamento").get("situacao").asString()).isEqualTo("CONFLITO_EXTERNO");
        long a = f.get("versao").get("nfse").get(0).get("id").longValue(),
                b = f.get("versao").get("nfse").get(1).get("id").longValue();
        resposta(
                post(
                        rota(f) + "/tratativas-externas",
                        tratativaComando(
                                f,
                                1,
                                "EMITIDO",
                                List.of(Map.of("referenciaId", a, "situacao", "MANTIDO"))),
                        gestor),
                409);
        var t =
                tratativaComando(
                        f,
                        1,
                        "EMITIDO",
                        List.of(
                                Map.of("referenciaId", a, "situacao", "CANCELAMENTO_COMPROVADO"),
                                Map.of("referenciaId", b, "situacao", "MANTIDO")));
        f = resposta(post(rota(f) + "/tratativas-externas", t, gestor), 200);
        assertThat(f.get("versao").get("nfse").size()).isEqualTo(2);
        assertThat(contar("tratativa_externa_fechamento")).isEqualTo(1);
        resposta(post(rota(f) + "/tratativas-externas", t, supervisor), 403);
    }

    @Test
    void saldoZeroDesconhecidoNaoFinalizaDeclaracaoSeparadaFinalizaSemNfse() throws Exception {
        configurar("2026-09-01", "DIAS_CORRIDOS", null, 30, null);
        var f = entregar(aprovar(preparar(calcular("2026-09-01", "2026-10-01"))));
        f = resolver(f);
        assertThat(f.get("fechamento").get("situacao").asString()).isEqualTo("APROVADO");
        assertThat(f.get("versao").get("estadoExterno").asString()).isEqualTo("DESCONHECIDO");
        f = confirmar(f);
        assertThat(f.get("fechamento").get("situacao").asString())
                .isEqualTo("FINALIZADO_SEM_EMISSAO");
        assertThat(contar("referencia_nfse")).isZero();
        resposta(
                post(
                        rota(f) + "/reabertura",
                        reabrirComando(f, calcular("2026-09-01", "2026-10-01")),
                        gestor),
                409);
    }

    @Test
    void ajustesAssinadosEncadeiamBaseConservamRecebidoEAplicamSoNaFinalizacao() throws Exception {
        var servicoD30 = configurar("2026-09-01", "DIAS_CORRIDOS", null, 30, null);
        var origemFato = fato("10", "2026-09-02T12:00:00Z");
        var origem = nfse(aprovar(preparar(calcular("2026-09-01", "2026-10-01"))), "ORIGEM");
        anular(origemFato);
        fato("7", "2026-10-02T12:00:00Z");
        var dez = fato("1", "2026-10-03T12:00:00Z");
        var vinte = fato("2", "2026-10-04T12:00:00Z");
        var destino = preparar(calcular("2026-10-01", "2026-10-31"));
        var a =
                resposta(
                        post(
                                "/api/v1/ajustes-fechamento",
                                ajusteComando(
                                        origem, destino, calcular("2026-09-01", "2026-10-01")),
                                gestor),
                        200);
        destino = atual(destino);
        assertThat(destino.get("versao").get("saldo").decimalValue()).isEqualByComparingTo("0");
        destino = aprovar(destino);
        assertThat(
                        jdbc.queryForObject(
                                "select situacao from wms.ajuste_fechamento where id=?",
                                String.class,
                                a.get("id").longValue()))
                .isEqualTo("VALIDADO");
        d30CompromissoServicoSemEfeito(
                servicoD30.get("id").longValue(),
                "OBRIGACAO_FINANCEIRA_PENDENTE",
                "FECHAMENTO_COBRANCA",
                destino.get("fechamento").get("id").longValue());
        d30CompromissoServicoSemEfeito(
                servicoD30.get("id").longValue(),
                "AJUSTE_FINANCEIRO_PENDENTE",
                "AJUSTE_FECHAMENTO",
                a.get("id").longValue());
        destino = resolver(confirmar(destino));
        assertThat(
                        jdbc.queryForObject(
                                "select situacao from wms.ajuste_fechamento where id=?",
                                String.class,
                                a.get("id").longValue()))
                .isEqualTo("APLICADO");
        long aplicado =
                jdbc.queryForObject(
                        "select aplicado_versao_id from wms.ajuste_fechamento where id=?",
                        Long.class,
                        a.get("id").longValue());
        String arquivo = get(rota(destino) + "/versoes/2/demonstrativo", supervisor).body();
        destino = nfseNumero(destino, 2, "TARDIAZERO");
        assertThat(destino.get("fechamento").get("situacao").asString())
                .isEqualTo("CONFLITO_EXTERNO");
        long doc = destino.get("versao").get("nfse").get(0).get("id").longValue();
        destino =
                resposta(
                        post(
                                rota(destino) + "/tratativas-externas",
                                tratativaComando(
                                        destino,
                                        2,
                                        "EMITIDO",
                                        List.of(
                                                Map.of(
                                                        "referenciaId",
                                                        doc,
                                                        "situacao",
                                                        "MANTIDO"))),
                                gestor),
                        200);
        assertThat(
                        jdbc.queryForObject(
                                "select aplicado_versao_id from wms.ajuste_fechamento where id=?",
                                Long.class,
                                a.get("id").longValue()))
                .isEqualTo(aplicado);
        assertThat(get(rota(destino) + "/versoes/2/demonstrativo", supervisor).body())
                .isEqualTo(arquivo);
        var proximo = preparar(calcular("2026-10-31", "2026-11-30"));
        anular(vinte);
        var ajuste1 =
                resposta(
                        post(
                                "/api/v1/ajustes-fechamento",
                                ajusteComando(
                                        destino, proximo, calcular("2026-10-01", "2026-10-31")),
                                gestor),
                        200);
        assertThat(ajuste1.get("valorBase").decimalValue()).isEqualByComparingTo("100");
        assertThat(ajuste1.get("diferenca").decimalValue()).isEqualByComparingTo("-20");
        proximo = atual(proximo);
        anular(dez);
        var calculo70 = calcular("2026-10-01", "2026-10-31");
        var ajuste2 =
                resposta(
                        post(
                                "/api/v1/ajustes-fechamento",
                                ajusteComando(destino, proximo, calculo70),
                                gestor),
                        200);
        assertThat(ajuste2.get("valorBase").decimalValue()).isEqualByComparingTo("80");
        assertThat(ajuste2.get("diferenca").decimalValue()).isEqualByComparingTo("-10");
        proximo = atual(proximo);
        resposta(
                post(
                        "/api/v1/ajustes-fechamento",
                        ajusteComando(destino, proximo, calculo70),
                        gestor),
                409);
        proximo = aprovar(proximo);
        assertThat(proximo.get("versao").get("natureza").asString()).isEqualTo("CREDITO");
        proximo = resolver(confirmar(proximo));
        assertThat(proximo.get("fechamento").get("situacao").asString())
                .isEqualTo("FINALIZADO_SEM_EMISSAO");
        assertThat(contar("ajuste_fechamento")).isEqualTo(3);
        assertThat(contar("fato_fechamento")).isEqualTo(4);
        assertThat(
                        d30GetFotografiaIntegral(
                                        "/api/v1/ajustes-fechamento?clienteId="
                                                + clienteId
                                                + "&armazemId="
                                                + armazemId,
                                        gestor)
                                .size())
                .isEqualTo(3);
        D30VersaoJpaProvider.conferir(
                em.getEntityManagerFactory(),
                jdbc,
                br.com.rodogarcia.wms.models.AjusteFechamento.class,
                "AJUSTE_FECHAMENTO",
                a.get("id").longValue(),
                "FechamentoIntegrationTest#ajustesAssinadosEncadeiamBaseConservamRecebidoEAplicamSoNaFinalizacao");
        D30VersaoJpaProvider.conferir(
                em.getEntityManagerFactory(),
                jdbc,
                br.com.rodogarcia.wms.models.VersaoFechamento.class,
                "VERSAO_FECHAMENTO",
                aplicado,
                "FechamentoIntegrationTest#ajustesAssinadosEncadeiamBaseConservamRecebidoEAplicamSoNaFinalizacao");
    }

    @Test
    void auditoriaFalhaReverteDiasVersaoRespostaEPermiteRepetir() throws Exception {
        configurar("2026-09-01", "DIAS_CORRIDOS", null, 30, null);
        var d = prepararComando(calcular("2026-09-01", "2026-10-01"));
        int ops = contar("operacao_administrativa");
        doAnswer(
                        inv -> {
                            if ("PREPARACAO_FECHAMENTO".equals(inv.getArgument(2)))
                                throw new IllegalStateException("Falha tardia ficticia");
                            return inv.callRealMethod();
                        })
                .when(alvoAuditoria)
                .registrar(any(), any(), any(), any(), any(), any());
        resposta(post("/api/v1/fechamentos-cobranca", d, gestor), 500);
        assertThat(contar("fechamento_cobranca")).isZero();
        assertThat(contar("dia_fechamento")).isZero();
        assertThat(contar("versao_fechamento")).isZero();
        assertThat(contar("operacao_administrativa")).isEqualTo(ops);
        reset(alvoAuditoria);
        resposta(post("/api/v1/fechamentos-cobranca", d, gestor), 200);
    }

    @Test
    void duasTransacoesMesmoPeriodoSerializamEReplayConservaUnicidade() throws Exception {
        configurar("2026-09-01", "DIAS_CORRIDOS", null, 30, null);
        var d = prepararComando(calcular("2026-09-01", "2026-10-01"));
        var travou = new CountDownLatch(1);
        var liberar = new CountDownLatch(1);
        var segundoEntrou = new CountDownLatch(1);
        AtomicInteger gravacoes = new AtomicInteger();
        doAnswer(
                        inv -> {
                            if ("PREPARACAO_FECHAMENTO".equals(inv.getArgument(2))
                                    && gravacoes.incrementAndGet() == 1) {
                                travou.countDown();
                                assertThat(liberar.await(10, TimeUnit.SECONDS)).isTrue();
                            }
                            return inv.callRealMethod();
                        })
                .when(alvoAuditoria)
                .registrar(any(), any(), any(), any(), any(), any());
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var a = executor.submit(() -> post("/api/v1/fechamentos-cobranca", d, gestor));
            assertThat(travou.await(10, TimeUnit.SECONDS)).isTrue();
            var b =
                    executor.submit(
                            () -> {
                                segundoEntrou.countDown();
                                return post("/api/v1/fechamentos-cobranca", d, gestor);
                            });
            assertThat(segundoEntrou.await(10, TimeUnit.SECONDS)).isTrue();
            try {
                assertThat(b.get(150, TimeUnit.MILLISECONDS))
                        .as("Second transaction must wait for context lock")
                        .isNull();
            } catch (java.util.concurrent.TimeoutException esperado) {
            } finally {
                liberar.countDown();
            }
            assertThat(resposta(a.get(15, TimeUnit.SECONDS), 200))
                    .isEqualTo(resposta(b.get(15, TimeUnit.SECONDS), 200));
        }
        assertThat(contar("fechamento_cobranca")).isEqualTo(1);
        assertThat(contar("dia_fechamento")).isEqualTo(30);
        assertThat(contar("versao_fechamento")).isEqualTo(1);
        resposta(
                post(
                        "/api/v1/fechamentos-cobranca",
                        prepararComando(calcular("2026-09-01", "2026-10-01")),
                        gestor),
                409);
    }

    @ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(booleans = {false, true})
    void redirecionaValidadoAtomicamenteSegundaReconcilicaoNaoAplicaSnapshotAntigo(
            boolean falhaAuditoria) throws Exception {
        configurar("2026-09-01", "DIAS_CORRIDOS", null, 30, null);
        fato("7", "2026-09-02T12:00:00Z");
        var dez = fato("1", "2026-09-03T12:00:00Z");
        var vinte = fato("2", "2026-09-04T12:00:00Z");
        var origem = nfse(aprovar(preparar(calcular("2026-09-01", "2026-10-01"))), "ORIGINAL");
        fato("10", "2026-10-02T12:00:00Z");
        var a = entregar(aprovar(preparar(calcular("2026-10-01", "2026-10-31"))));
        String snapshot1 = get(rota(a) + "/versoes/1/demonstrativo", supervisor).body();
        a = confirmar(a);
        a = reabrir(a, calcular("2026-10-01", "2026-10-31"));
        anular(vinte);
        var menos20 =
                resposta(
                        post(
                                "/api/v1/ajustes-fechamento",
                                ajusteComando(origem, a, calcular("2026-09-01", "2026-10-01")),
                                gestor),
                        200);
        long ajusteId = menos20.get("id").longValue();
        a = entregar(aprovar(atual(a)));
        String snapshot3 = get(rota(a) + "/versoes/3/demonstrativo", supervisor).body();
        a = confirmar(a);
        a = nfseNumero(a, 1, "TARDIA1");
        long doc1 = a.get("versao").get("nfse").get(0).get("id").longValue();
        var b = preparar(calcular("2026-10-31", "2026-11-30"));
        var t =
                tratativaComando(
                        a,
                        1,
                        "EMITIDO",
                        List.of(Map.of("referenciaId", doc1, "situacao", "MANTIDO")));
        resposta(post(rota(a) + "/tratativas-externas", t, gestor), 409);
        assertThat(contar("tratativa_externa_fechamento")).isZero();
        t.put("destinoAjustesId", b.get("fechamento").get("id").longValue());
        t.put("versaoDestinoAjustes", b.get("fechamento").get("versao").longValue());
        var rotaA = rota(a);
        long idA = a.get("fechamento").get("id").longValue();
        if (falhaAuditoria) {
            doAnswer(
                            inv -> {
                                if ("TRATATIVA_EXTERNA".equals(inv.getArgument(2)))
                                    throw new IllegalStateException("Falha tardia ficticia");
                                return inv.callRealMethod();
                            })
                    .when(alvoAuditoria)
                    .registrar(any(), any(), any(), any(), any(), any());
            resposta(post(rotaA + "/tratativas-externas", t, gestor), 500);
            assertThat(
                            jdbc.queryForObject(
                                    "select destino_fechamento_id from wms.ajuste_fechamento where id=?",
                                    Long.class,
                                    ajusteId))
                    .isEqualTo(idA);
            assertThat(atual(b).get("versao").get("numero").asInt()).isEqualTo(1);
            assertThat(atual(a).get("fechamento").get("situacao").asString())
                    .isEqualTo("CONFLITO_EXTERNO");
            assertThat(contar("tratativa_externa_fechamento")).isZero();
            reset(alvoAuditoria);
        }
        a = resposta(post(rotaA + "/tratativas-externas", t, gestor), 200);
        assertThat(resposta(post(rotaA + "/tratativas-externas", t, gestor), 200)).isEqualTo(a);
        resposta(post(rotaA + "/tratativas-externas", t, supervisor), 403);
        b = atual(b);
        assertThat(b.get("versao").get("saldo").decimalValue()).isEqualByComparingTo("-20");
        assertThat(
                        jdbc.queryForObject(
                                "select destino_fechamento_id from wms.ajuste_fechamento where id=?",
                                Long.class,
                                ajusteId))
                .isEqualTo(b.get("fechamento").get("id").longValue());
        a = nfseNumero(a, 3, "TARDIA3");
        long doc3 = a.get("versao").get("nfse").get(0).get("id").longValue();
        var tentativa =
                tratativaComando(
                        a,
                        3,
                        "EMITIDO",
                        List.of(
                                Map.of("referenciaId", doc1, "situacao", "CANCELAMENTO_COMPROVADO"),
                                Map.of("referenciaId", doc3, "situacao", "MANTIDO")));
        resposta(post(rotaA + "/tratativas-externas", tentativa, gestor), 409);
        assertThat(
                        jdbc.queryForObject(
                                "select situacao from wms.ajuste_fechamento where id=?",
                                String.class,
                                ajusteId))
                .isEqualTo("VALIDADO");
        assertThat(contar("tratativa_externa_fechamento")).isEqualTo(1);
        assertThatThrownBy(
                        () ->
                                new TransactionTemplate(transactions)
                                        .executeWithoutResult(
                                                tx -> {
                                                    var ajuste =
                                                            em.find(
                                                                    br.com.rodogarcia.wms.models
                                                                            .AjusteFechamento.class,
                                                                    ajusteId);
                                                    var v =
                                                            em.createQuery(
                                                                            "select v from VersaoFechamento v where v.fechamento.id=:id and v.numero=3",
                                                                            br.com.rodogarcia.wms
                                                                                    .models
                                                                                    .VersaoFechamento
                                                                                    .class)
                                                                    .setParameter("id", idA)
                                                                    .getSingleResult();
                                                    ajuste.aplicar(v, BASE);
                                                }))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("destino atual");
        assertThat(get(rotaA + "/versoes/1/demonstrativo", supervisor).body()).isEqualTo(snapshot1);
        assertThat(get(rotaA + "/versoes/3/demonstrativo", supervisor).body()).isEqualTo(snapshot3);
        a =
                resposta(
                        post(
                                rotaA + "/tratativas-externas",
                                tratativaComando(
                                        a,
                                        1,
                                        "EMITIDO",
                                        List.of(
                                                Map.of("referenciaId", doc1, "situacao", "MANTIDO"),
                                                Map.of(
                                                        "referenciaId",
                                                        doc3,
                                                        "situacao",
                                                        "CANCELAMENTO_COMPROVADO"))),
                                gestor),
                        200);
        anular(dez);
        var menos10 =
                resposta(
                        post(
                                "/api/v1/ajustes-fechamento",
                                ajusteComando(origem, b, calcular("2026-09-01", "2026-10-01")),
                                gestor),
                        200);
        assertThat(menos10.get("valorBase").decimalValue()).isEqualByComparingTo("80");
        assertThat(menos10.get("diferenca").decimalValue()).isEqualByComparingTo("-10");
        b = resolver(confirmar(aprovar(atual(b))));
        assertThat(b.get("versao").get("saldo").decimalValue()).isEqualByComparingTo("-30");
        assertThat(
                        jdbc.queryForObject(
                                "select count(*) from wms.ajuste_fechamento where situacao='APLICADO'",
                                Integer.class))
                .isEqualTo(2);
        assertThat(
                        jdbc.queryForObject(
                                "select dados_antes from wms.auditoria_cadastro where tipo='FECHAMENTO_COBRANCA' and acao='TRATATIVA_EXTERNA' order by id fetch first 1 row only",
                                String.class))
                .contains("destinoFechamentoId");
    }

    @ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {"ENCERRAMENTO_PENDENTE", "INATIVO"})
    void resolucaoHistoricaIdentificadaMantemCadastroEExigeGestorAntesReplay(String situacao)
            throws Exception {
        var servico = configurar("2026-09-01", "DIAS_CORRIDOS", null, 30, null);
        var fato = fato("10", "2026-09-02T12:00:00Z");
        var fora = fato("1", "2026-10-02T12:00:00Z");
        var f = preparar(calcular("2026-09-01", "2026-10-01"));
        var referencia =
                Map.of(
                        "tipo",
                        "FECHAMENTO",
                        "compromissoId",
                        f.get("fechamento").get("id").longValue());
        String snapshot = get(rota(f) + "/versoes/1/demonstrativo", supervisor).body();
        jdbc.update("update wms.cliente set situacao=? where id=?", situacao, clienteId);
        jdbc.update("update wms.armazem set situacao=? where id=?", situacao, armazemId);
        var d = calcularComando("2026-09-01", "2026-10-01");
        resposta(post("/api/v1/calculos-cobranca", d, gestor), 409);
        d.put(
                "resolucao",
                Map.of(
                        "tipo",
                        "FECHAMENTO",
                        "compromissoId",
                        f.get("fechamento").get("id").longValue()));
        long antesCalculos = contar("calculo_cobranca");
        falharAuditoria("CALCULO");
        resposta(post("/api/v1/calculos-cobranca", d, gestor), 500);
        reset(alvoAuditoria);
        assertThat(contar("calculo_cobranca")).isEqualTo(antesCalculos);
        var calc = resposta(post("/api/v1/calculos-cobranca", d, gestor), 200);
        conferirAuditoriaResolucao(
                "CALCULO_COBRANCA", calc.get("id").longValue(), "CALCULO", referencia);
        assertThat(resposta(post("/api/v1/calculos-cobranca", d, gestor), 200)).isEqualTo(calc);
        resposta(post("/api/v1/calculos-cobranca", d, supervisor), 403);
        d.put("operacaoId", UUID.randomUUID());
        d.put("periodoInicio", "2026-10-01");
        d.put("periodoFim", "2026-10-31");
        resposta(post("/api/v1/calculos-cobranca", d, gestor), 409);
        var a = decisao(f);
        a.put(
                "resolucao",
                Map.of(
                        "tipo",
                        "FECHAMENTO",
                        "compromissoId",
                        f.get("fechamento").get("id").longValue()));
        falharAuditoria("APROVACAO_FECHAMENTO");
        resposta(post(rota(f) + "/aprovacao", a, gestor), 500);
        reset(alvoAuditoria);
        assertThat(atual(f).get("versao").get("situacao").asString()).isEqualTo("PENDENTE_REVISAO");
        f = resposta(post(rota(f) + "/aprovacao", a, gestor), 200);
        conferirAuditoriaResolucao(
                "FECHAMENTO_COBRANCA",
                f.get("fechamento").get("id").longValue(),
                "APROVACAO_FECHAMENTO",
                referencia);
        resposta(post(rota(f) + "/aprovacao", a, supervisor), 403);
        var tabela = comando();
        tabela.put("armazemId", armazemId);
        tabela.put("clienteId", clienteId);
        tabela.put("codigo", "CORRECAO");
        tabela.put("descricao", "Configuracao historica ficticia comprovada");
        tabela.put("tipo", "ESPECIFICA");
        tabela.put("vigenciaInicio", "2026-09-01");
        tabela.put("vigenciaFim", "2026-10-01");
        tabela.put("itens", List.of(item(servico, "", "11")));
        resposta(post("/api/v1/tabelas-cobranca", tabela, gestor), 409);
        tabela.put("resolucao", referencia);
        long antesTabelas = contar("tabela_cobranca");
        falharAuditoria("CRIACAO");
        resposta(post("/api/v1/tabelas-cobranca", tabela, gestor), 500);
        reset(alvoAuditoria);
        assertThat(contar("tabela_cobranca")).isEqualTo(antesTabelas);
        var criada = resposta(post("/api/v1/tabelas-cobranca", tabela, gestor), 200);
        conferirAuditoriaResolucao(
                "TABELA_COBRANCA", criada.get("id").longValue(), "CRIACAO", referencia);
        assertThat(resposta(post("/api/v1/tabelas-cobranca", tabela, gestor), 200))
                .isEqualTo(criada);
        resposta(post("/api/v1/tabelas-cobranca", tabela, supervisor), 403);
        var anulacao = comando();
        anulacao.put("versao", fora.get("versao").longValue());
        anulacao.put("resolucao", referencia);
        resposta(
                post(
                        "/api/v1/fatos-servico/" + fora.get("id").longValue() + "/anulacao",
                        anulacao,
                        gestor),
                409);
        anulacao.put("operacaoId", UUID.randomUUID());
        anulacao.put("versao", fato.get("versao").longValue());
        String rotaFato = "/api/v1/fatos-servico/" + fato.get("id").longValue() + "/anulacao";
        var anulado = resposta(post(rotaFato, anulacao, gestor), 200);
        assertThat(resposta(post(rotaFato, anulacao, gestor), 200)).isEqualTo(anulado);
        resposta(post(rotaFato, anulacao, supervisor), 403);
        assertThat(get(rota(f) + "/versoes/1/demonstrativo", supervisor).body())
                .isEqualTo(snapshot);
        assertThat(contar("movimento_estoque")).isZero();
        assertThat(
                        jdbc.queryForObject(
                                "select situacao from wms.cliente where id=?",
                                String.class,
                                clienteId))
                .isEqualTo(situacao);
        assertThat(
                        jdbc.queryForObject(
                                "select situacao from wms.armazem where id=?",
                                String.class,
                                armazemId))
                .isEqualTo(situacao);
    }

    private void d30CompromissoServicoSemEfeito(
            long servico, String codigo, String recurso, long id) throws Exception {
        var antes = D30FotografiaFisica.capturar(jdbc);
        var resposta =
                resposta(
                        get(
                                "/api/v1/encerramentos/SERVICO_COBRANCA/"
                                        + servico
                                        + "/impedimentos",
                                gestor),
                        200);
        var encontrados = new java.util.ArrayList<JsonNode>();
        for (var item : resposta.get("impedimentos"))
            if (item.get("codigo").asString().equals(codigo)
                    && item.get("recurso").asString().equals(recurso)
                    && item.get("id").longValue() == id) encontrados.add(item);
        assertThat(encontrados).as(codigo + "#" + id).hasSize(1);
        assertThat(encontrados.getFirst().get("detalhe").asString()).isNotBlank();
        assertThat(D30FotografiaFisica.capturar(jdbc)).isEqualTo(antes);
    }

    private void falharAuditoria(String acao) {
        doAnswer(
                        inv -> {
                            if (inv.getArgument(2).equals(acao))
                                throw new IllegalStateException("Falha de auditoria historica");
                            return inv.callRealMethod();
                        })
                .when(alvoAuditoria)
                .registrar(any(), any(), any(), any(), any(), any());
    }

    private void conferirAuditoriaResolucao(
            String tipo, long id, String acao, Map<String, ?> referencia) throws Exception {
        var linhas =
                jdbc.queryForList(
                        "select dados_depois from wms.auditoria_cadastro where tipo=? and registro_id=? and acao=? order by id",
                        String.class,
                        tipo,
                        id,
                        acao);
        assertThat(linhas).hasSize(1);
        var viaHttp =
                resposta(get("/api/v1/auditoria?tipo=" + tipo + "&registroId=" + id, gestor), 200);
        boolean encontrado = false;
        for (var item : viaHttp.get("itens")) {
            if (item.get("acao").asString().equals(acao)) {
                assertThat(item.get("dadosDepois").asString()).isEqualTo(linhas.getFirst());
                encontrado = true;
            }
        }
        assertThat(encontrado).isTrue();
        resposta(get("/api/v1/auditoria?tipo=" + tipo + "&registroId=" + id, supervisor), 403);
        var r = mapper.readTree(linhas.getFirst()).get("resolucao");
        assertThat(r.get("tipo").asString()).isEqualTo(referencia.get("tipo"));
        assertThat(r.get("compromissoId").longValue()).isEqualTo(referencia.get("compromissoId"));
        assertThat(r.get("clienteId").longValue()).isEqualTo(clienteId);
        assertThat(r.get("armazemId").longValue()).isEqualTo(armazemId);
        assertThat(r.get("periodoInicio").asString()).isEqualTo("2026-09-01");
        assertThat(r.get("periodoFim").asString()).isEqualTo("2026-10-01");
        if (!tipo.equals("TABELA_COBRANCA")) {
            assertThat(r.get("periodoEfeitoInicio").asString()).isEqualTo("2026-09-01");
            assertThat(r.get("periodoEfeitoFim").asString()).isEqualTo("2026-10-01");
        }
    }

    @Test
    void prepararConferePeriodoHistoricoAntesReplayENaoAdmiteCalculoFuturoSalvo() throws Exception {
        configurar("2026-09-01", "DIAS_CORRIDOS", null, 30, null);
        var u = unidade("10", TipoUnidadeLogistica.PALLET, "2026-08-01T12:00:00Z", 1);
        var refPedido = Map.of("tipo", "PEDIDO_ENTRADA", "compromissoId", u.pedidoId());
        var calcSet = calcular("2026-09-01", "2026-10-01");
        var calcOut = calcular("2026-10-01", "2026-10-31");
        var futuro = calcular("2030-01-01", "2030-01-31");
        var preparar = comando();
        preparar.put("calculoId", calcSet.get("id").longValue());
        preparar.put("resolucao", refPedido);
        var f = resposta(post("/api/v1/fechamentos-cobranca", preparar, gestor), 200);
        jdbc.update("update wms.cliente set situacao='INATIVO' where id=?", clienteId);
        jdbc.update("update wms.armazem set situacao='INATIVO' where id=?", armazemId);
        assertThat(resposta(post("/api/v1/fechamentos-cobranca", preparar, gestor), 200))
                .isEqualTo(f);
        resposta(post("/api/v1/fechamentos-cobranca", preparar, supervisor), 403);
        var fora = comando();
        fora.put("calculoId", calcOut.get("id").longValue());
        fora.put(
                "resolucao",
                Map.of(
                        "tipo",
                        "FECHAMENTO",
                        "compromissoId",
                        f.get("fechamento").get("id").longValue()));
        resposta(post("/api/v1/fechamentos-cobranca", fora, gestor), 409);
        fora.put("calculoId", futuro.get("id").longValue());
        fora.put("resolucao", refPedido);
        resposta(post("/api/v1/fechamentos-cobranca", fora, gestor), 409);
        assertThat(contar("fechamento_cobranca")).isEqualTo(1);
        fora.put("calculoId", calcOut.get("id").longValue());
        var valido = resposta(post("/api/v1/fechamentos-cobranca", fora, gestor), 200);
        assertThat(valido.get("fechamento").get("periodoInicio").asString())
                .isEqualTo("2026-10-01");
        assertThat(contar("movimento_estoque")).isZero();
        assertThat(
                        jdbc.queryForObject(
                                "select situacao from wms.cliente where id=?",
                                String.class,
                                clienteId))
                .isEqualTo("INATIVO");
    }

    @ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(booleans = {false, true})
    void trocaBaseConfrontaTodasOrigensRegularizaExplicitamenteSemDesfazerAplicado(boolean aplicado)
            throws Exception {
        var caso = casoTrocaOrigem(aplicado);
        var origem = caso.origem();
        var destino = caso.destino();
        var comando = tratativaTroca(caso);
        var dtoSemRegularizacao =
                mapper.readValue(
                        mapper.writeValueAsString(comando),
                        br.com.rodogarcia.wms.dto.FechamentoCobrancaDto.Tratar.class);
        assertThat(mapper.valueToTree(dtoSemRegularizacao).has("regularizacaoOrigem")).isFalse();
        String rota = rota(origem) + "/tratativas-externas";
        long quantidade = contar("ajuste_fechamento");
        long tratativas = contar("tratativa_externa_fechamento");
        String snapshotA = get(rota(origem) + "/versoes/1/demonstrativo", supervisor).body();
        String snapshotB =
                get(rota(caso.primeiroDestino()) + "/versoes/2/demonstrativo", supervisor).body();
        var antigo =
                jdbc.queryForMap(
                        "select * from wms.ajuste_fechamento where id=?",
                        caso.ajuste().get("id").longValue());
        resposta(post(rota, comando, gestor), 409);
        assertThat(atual(origem).get("fechamento").get("situacao").asString())
                .isEqualTo("CONFLITO_EXTERNO");
        comando.put(
                "regularizacaoOrigem",
                regularizacao(caso, List.of(caso.ajuste().get("id").longValue()), "19"));
        resposta(post(rota, comando, gestor), 409);
        comando.put(
                "regularizacaoOrigem",
                regularizacao(caso, List.of(caso.ajuste().get("id").longValue() + 9000), "20"));
        resposta(post(rota, comando, gestor), 409);
        comando.put(
                "regularizacaoOrigem",
                regularizacao(caso, List.of(caso.ajuste().get("id").longValue()), "20"));
        doAnswer(
                        inv -> {
                            if (inv.getArgument(2).equals("TRATATIVA_EXTERNA"))
                                throw new IllegalStateException("Falha tardia p2 origem");
                            return inv.callRealMethod();
                        })
                .when(alvoAuditoria)
                .registrar(any(), any(), any(), any(), any(), any());
        resposta(post(rota, comando, gestor), 500);
        reset(alvoAuditoria);
        assertThat(contar("ajuste_fechamento")).isEqualTo(quantidade);
        assertThat(contar("tratativa_externa_fechamento")).isEqualTo(tratativas);
        assertThat(
                        jdbc.queryForMap(
                                "select * from wms.ajuste_fechamento where id=?",
                                caso.ajuste().get("id").longValue()))
                .isEqualTo(antigo);
        assertThat(atual(destino).get("fechamento").get("versao").asLong())
                .isEqualTo(destino.get("fechamento").get("versao").asLong());
        origem = resposta(post(rota, comando, gestor), 200);
        assertThat(resposta(post(rota, comando, gestor), 200)).isEqualTo(origem);
        resposta(post(rota, comando, supervisor), 403);
        resposta(post(rota, comando, operador), 403);
        var divergente = new HashMap<>(comando);
        divergente.put("motivo", "Outra comprovacao da mesma operacao");
        resposta(post(rota, divergente, gestor), 409);
        assertThat(
                        jdbc.queryForMap(
                                "select * from wms.ajuste_fechamento where id=?",
                                caso.ajuste().get("id").longValue()))
                .isEqualTo(antigo);
        destino = atual(destino);
        assertThat(
                        jdbc.queryForObject(
                                "select diferenca from wms.ajuste_fechamento where tipo='REGULARIZACAO_ORIGEM'",
                                BigDecimal.class))
                .isEqualByComparingTo("20");
        assertThat(
                        jdbc.queryForObject(
                                "select sum(diferenca) from wms.ajuste_fechamento",
                                BigDecimal.class))
                .isEqualByComparingTo("0");
        assertThat(origem.get("versao").get("saldo").decimalValue()).isEqualByComparingTo("80");
        var historico = resposta(get(rota(origem) + "/tratativas-externas", supervisor), 200);
        var ultima = historico.get(historico.size() - 1);
        assertThat(ultima.get("versaoBaseAnteriorId").longValue())
                .isEqualTo(caso.ajuste().get("origemVersaoId").longValue());
        assertThat(ultima.get("dependenciasOrigem").size()).isEqualTo(1);
        assertThat(ultima.get("dependenciasOrigem").get(0).get("situacao").asString())
                .isEqualTo(aplicado ? "APLICADO" : "VALIDADO");
        resposta(
                post(
                        "/api/v1/ajustes-fechamento",
                        ajusteComando(origem, destino, caso.correcao80()),
                        gestor),
                409);
        anular(caso.fato80());
        fato("7", "2026-09-02T12:00:00Z");
        var calc70 = calcular("2026-09-01", "2026-10-01");
        var velha = new HashMap<>(ajusteComando(origem, destino, calc70));
        velha.put("origemVersaoId", caso.ajuste().get("origemVersaoId").longValue());
        resposta(post("/api/v1/ajustes-fechamento", velha, gestor), 409);
        var novo =
                resposta(
                        post(
                                "/api/v1/ajustes-fechamento",
                                ajusteComando(origem, destino, calc70),
                                gestor),
                        200);
        assertThat(novo.get("valorBase").decimalValue()).isEqualByComparingTo("80");
        assertThat(novo.get("diferenca").decimalValue()).isEqualByComparingTo("-10");
        assertThat(
                        jdbc.queryForObject(
                                "select sum(diferenca) from wms.ajuste_fechamento",
                                BigDecimal.class))
                .isEqualByComparingTo("-10");
        assertThat(get(rota(origem) + "/versoes/1/demonstrativo", supervisor).body())
                .isEqualTo(snapshotA);
        assertThat(
                        get(rota(caso.primeiroDestino()) + "/versoes/2/demonstrativo", supervisor)
                                .body())
                .isEqualTo(snapshotB);
        destino = atual(destino);
        destino =
                reabrir(
                        destino,
                        calcular(
                                destino.get("fechamento").get("periodoInicio").asString(),
                                destino.get("fechamento").get("periodoFim").asString()));
        destino = nfse(aprovar(destino), "REGULARIZACAO-FINAL");
        assertThat(
                        jdbc.queryForObject(
                                "select count(*) from wms.ajuste_fechamento where situacao='VALIDADO'",
                                Long.class))
                .isZero();
        assertThat(
                        jdbc.queryForObject(
                                "select aplicado_versao_id from wms.ajuste_fechamento where tipo='REGULARIZACAO_ORIGEM'",
                                Long.class))
                .isEqualTo(destino.get("versao").get("id").longValue());
        new TransactionTemplate(transactions)
                .executeWithoutResult(
                        tx -> {
                            var reg =
                                    em.createQuery(
                                                    "select a from AjusteFechamento a where a.tipo='REGULARIZACAO_ORIGEM'",
                                                    br.com.rodogarcia.wms.models.AjusteFechamento
                                                            .class)
                                            .getSingleResult();
                            assertThatThrownBy(
                                            () ->
                                                    reg.identificarRegularizacao(
                                                            reg.getTratativaOrigem()))
                                    .isInstanceOf(IllegalStateException.class);
                        });
    }

    @Test
    void segundaTrocaDeBaseExigeTambemDeltasOriginadosNaOutraVersao() throws Exception {
        var caso = casoTrocaOrigem(false);
        var d = tratativaTroca(caso);
        d.put(
                "regularizacaoOrigem",
                regularizacao(caso, List.of(caso.ajuste().get("id").longValue()), "20"));
        var a = resposta(post(rota(caso.origem()) + "/tratativas-externas", d, gestor), 200);
        var b = atual(caso.destino());
        anular(caso.fato80());
        var fato70 = fato("7", "2026-09-02T12:00:00Z");
        resposta(
                post(
                        "/api/v1/ajustes-fechamento",
                        ajusteComando(a, b, calcular("2026-09-01", "2026-10-01")),
                        gestor),
                200);
        b = atual(b);
        a = nfseNumero(atual(a), 2, "OUTRA-V2");
        var v1 = resposta(get(rota(a) + "/versoes/1", supervisor), 200);
        var v2 = resposta(get(rota(a) + "/versoes/2", supervisor), 200);
        var refs = new java.util.ArrayList<Map<String, Object>>();
        refs.add(
                Map.of(
                        "referenciaId",
                        v1.get("nfse").get(0).get("id").longValue(),
                        "situacao",
                        "MANTIDO"));
        for (var r : v2.get("nfse"))
            refs.add(
                    Map.of(
                            "referenciaId",
                            r.get("id").longValue(),
                            "situacao",
                            "CANCELAMENTO_COMPROVADO"));
        var troca = tratativaComando(a, 1, "EMITIDO", refs);
        troca.put(
                "regularizacaoOrigem",
                Map.of(
                        "ajustesDependentesIds",
                        List.of(caso.ajuste().get("id").longValue()),
                        "destinoFechamentoId",
                        b.get("fechamento").get("id").longValue(),
                        "versaoDestino",
                        b.get("fechamento").get("versao").longValue(),
                        "diferencaEsperada",
                        "-20"));
        resposta(post(rota(a) + "/tratativas-externas", troca, gestor), 409);
        var todos =
                jdbc.queryForList("select id from wms.ajuste_fechamento order by id", Long.class);
        assertThat(todos).hasSize(3);
        troca.put(
                "regularizacaoOrigem",
                Map.of(
                        "ajustesDependentesIds",
                        todos,
                        "destinoFechamentoId",
                        b.get("fechamento").get("id").longValue(),
                        "versaoDestino",
                        b.get("fechamento").get("versao").longValue(),
                        "diferencaEsperada",
                        "-20"));
        a = resposta(post(rota(a) + "/tratativas-externas", troca, gestor), 200);
        assertThat(a.get("versao").get("saldo").decimalValue()).isEqualByComparingTo("100");
        assertThat(
                        jdbc.queryForObject(
                                "select sum(diferenca) from wms.ajuste_fechamento",
                                BigDecimal.class))
                .isEqualByComparingTo("-30");
        anular(fato70);
        fato("6", "2026-09-02T12:00:00Z");
        var novo =
                resposta(
                        post(
                                "/api/v1/ajustes-fechamento",
                                ajusteComando(a, atual(b), calcular("2026-09-01", "2026-10-01")),
                                gestor),
                        200);
        assertThat(novo.get("valorBase").decimalValue()).isEqualByComparingTo("70");
        assertThat(novo.get("diferenca").decimalValue()).isEqualByComparingTo("-10");
        assertThat(
                        jdbc.queryForObject(
                                "select sum(diferenca) from wms.ajuste_fechamento",
                                BigDecimal.class))
                .isEqualByComparingTo("-40");
    }

    @Test
    void regularizacaoOrigemConcorrenteSerializaConferenciaEReplay() throws Exception {
        var caso = casoTrocaOrigem(true);
        var d = tratativaTroca(caso);
        d.put(
                "regularizacaoOrigem",
                regularizacao(caso, List.of(caso.ajuste().get("id").longValue()), "20"));
        var entrou = new CountDownLatch(1);
        var liberar = new CountDownLatch(1);
        doAnswer(
                        inv -> {
                            if (inv.getArgument(2).equals("TRATATIVA_EXTERNA")) {
                                entrou.countDown();
                                assertThat(liberar.await(10, TimeUnit.SECONDS)).isTrue();
                            }
                            return inv.callRealMethod();
                        })
                .when(alvoAuditoria)
                .registrar(any(), any(), any(), any(), any(), any());
        String rota = rota(caso.origem()) + "/tratativas-externas";
        try (var pool = Executors.newFixedThreadPool(2)) {
            var primeira = pool.submit(() -> post(rota, d, gestor));
            assertThat(entrou.await(10, TimeUnit.SECONDS)).isTrue();
            var segunda = pool.submit(() -> post(rota, d, gestor));
            Thread.sleep(150);
            assertThat(segunda.isDone()).isFalse();
            liberar.countDown();
            var r = resposta(primeira.get(15, TimeUnit.SECONDS), 200);
            assertThat(resposta(segunda.get(15, TimeUnit.SECONDS), 200)).isEqualTo(r);
        } finally {
            liberar.countDown();
            reset(alvoAuditoria);
        }
        assertThat(
                        jdbc.queryForObject(
                                "select count(*) from wms.ajuste_fechamento where tipo='REGULARIZACAO_ORIGEM'",
                                Long.class))
                .isEqualTo(1);
        assertThat(
                        jdbc.queryForObject(
                                "select sum(diferenca) from wms.ajuste_fechamento",
                                BigDecimal.class))
                .isEqualByComparingTo("0");
    }

    private record CasoOrigem(
            JsonNode origem,
            JsonNode destino,
            JsonNode primeiroDestino,
            JsonNode ajuste,
            JsonNode correcao80,
            JsonNode fato80) {}

    private CasoOrigem casoTrocaOrigem(boolean aplicado) throws Exception {
        configurar("2026-09-01", "DIAS_CORRIDOS", null, 30, null);
        var fato100 = fato("10", "2026-09-02T12:00:00Z");
        var a = entregar(aprovar(preparar(calcular("2026-09-01", "2026-10-01"))));
        a = confirmar(a);
        anular(fato100);
        var fato80 = fato("8", "2026-09-02T12:00:00Z");
        var calc80 = calcular("2026-09-01", "2026-10-01");
        a = aprovar(reabrir(a, calc80));
        a = nfseNumero(a, 1, "V1-100");
        var ref1 = a.get("versao").get("nfse").get(0).get("id").longValue();
        a =
                resposta(
                        post(
                                rota(a) + "/tratativas-externas",
                                tratativaComando(
                                        a,
                                        1,
                                        "EMITIDO",
                                        List.of(
                                                Map.of(
                                                        "referenciaId",
                                                        ref1,
                                                        "situacao",
                                                        "MANTIDO"))),
                                gestor),
                        200);
        fato("10", "2026-10-02T12:00:00Z");
        calc80 = calcular("2026-09-01", "2026-10-01");
        var b = preparar(calcular("2026-10-01", "2026-10-31"));
        var ajuste =
                resposta(
                        post("/api/v1/ajustes-fechamento", ajusteComando(a, b, calc80), gestor),
                        200);
        b = atual(b);
        var destino = b;
        if (aplicado) {
            b = nfse(aprovar(b), "B-80");
            destino = preparar(calcular("2026-10-31", "2026-11-30"));
        }
        a = nfseNumero(atual(a), 2, "V2-80");
        return new CasoOrigem(a, destino, b, ajuste, calc80, fato80);
    }

    private Map<String, Object> tratativaTroca(CasoOrigem caso) throws Exception {
        var v1 = resposta(get(rota(caso.origem()) + "/versoes/1", supervisor), 200);
        var v2 = resposta(get(rota(caso.origem()) + "/versoes/2", supervisor), 200);
        return tratativaComando(
                caso.origem(),
                2,
                "EMITIDO",
                List.of(
                        Map.of(
                                "referenciaId",
                                v1.get("nfse").get(0).get("id").longValue(),
                                "situacao",
                                "CANCELAMENTO_COMPROVADO"),
                        Map.of(
                                "referenciaId",
                                v2.get("nfse").get(0).get("id").longValue(),
                                "situacao",
                                "MANTIDO")));
    }

    private Map<String, Object> regularizacao(CasoOrigem caso, List<Long> ajustes, String delta) {
        return Map.of(
                "ajustesDependentesIds",
                ajustes,
                "destinoFechamentoId",
                caso.destino().get("fechamento").get("id").longValue(),
                "versaoDestino",
                caso.destino().get("fechamento").get("versao").longValue(),
                "diferencaEsperada",
                delta);
    }

    @Test
    void rejeicaoIntegralPreservaDiasFatosArquivoEReabreSomenteNovaVersao() throws Exception {
        configurar("2026-09-01", "DIAS_CORRIDOS", null, 30, null);
        fato("2", "2026-09-02T12:00:00Z");
        var f = preparar(calcular("2026-09-01", "2026-10-01"));
        String arquivo = get(rota(f) + "/versoes/1/demonstrativo", supervisor).body();
        var d = decisao(f);
        var rejeitado = resposta(post(rota(f) + "/rejeicao", d, gestor), 200);
        assertThat(rejeitado.get("fechamento").get("situacao").asString()).isEqualTo("REJEITADO");
        assertThat(resposta(post(rota(f) + "/rejeicao", d, gestor), 200)).isEqualTo(rejeitado);
        resposta(post(rota(f) + "/rejeicao", d, supervisor), 403);
        resposta(post(rota(f) + "/aprovacao", decisao(rejeitado), gestor), 409);
        resposta(post(rota(f) + "/entregas", entregaComando(rejeitado), gestor), 409);
        resposta(
                post(
                        "/api/v1/fechamentos-cobranca",
                        prepararComando(calcular("2026-09-01", "2026-10-01")),
                        gestor),
                409);
        var nova = aprovar(reabrir(rejeitado, calcular("2026-09-01", "2026-10-01")));
        assertThat(nova.get("versao").get("numero").asInt()).isEqualTo(2);
        assertThat(contar("dia_fechamento")).isEqualTo(30);
        assertThat(contar("fato_fechamento")).isEqualTo(1);
        assertThat(get(rota(f) + "/versoes/1/demonstrativo", supervisor).body()).isEqualTo(arquivo);
        assertThat(contar("entrega_esl")).isZero();
    }

    @Test
    void compatibilidadeHashCalculoAnteriorSemCampoNovo() throws Exception {
        var antiga =
                new br.com.rodogarcia.wms.dto.CalculoCobrancaDto.Calcular(
                        UUID.fromString("00000000-0000-0000-0000-000000000001"),
                        1L,
                        2L,
                        LocalDate.parse("2026-09-01"),
                        LocalDate.parse("2026-10-01"),
                        "Motivo ficticio",
                        null);
        assertThat(mapper.writeValueAsString(antiga))
                .isEqualTo(
                        "{\"operacaoId\":\"00000000-0000-0000-0000-000000000001\",\"clienteId\":1,\"armazemId\":2,\"periodoInicio\":\"2026-09-01\",\"periodoFim\":\"2026-10-01\",\"motivo\":\"Motivo ficticio\"}");
    }

    @Test
    void novaEntregaAvancaVersaoOtimistaERecusaDecisaoAntiga() throws Exception {
        configurar("2026-09-01", "DIAS_CORRIDOS", null, 30, null);
        var f = aprovar(preparar(calcular("2026-09-01", "2026-10-01")));
        long versao = f.get("fechamento").get("versao").longValue();
        f = entregar(f);
        assertThat(f.get("fechamento").get("versao").longValue()).isGreaterThan(versao);
        resposta(
                post(
                        rota(f) + "/confirmacoes-externas",
                        new HashMap<>(
                                Map.of(
                                        "operacaoId",
                                        UUID.randomUUID(),
                                        "versao",
                                        versao,
                                        "numero",
                                        1,
                                        "fonte",
                                        "Fonte ficticia",
                                        "confirmadaPor",
                                        "Responsavel ficticio",
                                        "confirmadaEm",
                                        "2028-06-06T12:00:00Z",
                                        "motivo",
                                        "Motivo ficticio")),
                        gestor),
                409);
    }

    @ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(booleans = {true, false})
    void emissaoEReaberturaConcorrentesPreservamReferenciaOriginal(boolean emissaoPrimeiro)
            throws Exception {
        configurar("2026-09-01", "DIAS_CORRIDOS", null, 30, null);
        fato("10", "2026-09-02T12:00:00Z");
        var f = confirmar(entregar(aprovar(preparar(calcular("2026-09-01", "2026-10-01")))));
        var reabrir = reabrirComando(f, calcular("2026-09-01", "2026-10-01"));
        var documento = nfseComando(f, 1, "CONCORRENTE");
        String rota = rota(f);
        var travou = new CountDownLatch(1);
        var liberar = new CountDownLatch(1);
        var entrou = new CountDownLatch(1);
        String acao = emissaoPrimeiro ? "REFERENCIA_NFSE" : "REABERTURA_FECHAMENTO";
        doAnswer(
                        inv -> {
                            if (acao.equals(inv.getArgument(2))) {
                                travou.countDown();
                                assertThat(liberar.await(10, TimeUnit.SECONDS)).isTrue();
                            }
                            return inv.callRealMethod();
                        })
                .when(alvoAuditoria)
                .registrar(any(), any(), any(), any(), any(), any());
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var a =
                    executor.submit(
                            () ->
                                    post(
                                            rota
                                                    + (emissaoPrimeiro
                                                            ? "/referencias-nfse"
                                                            : "/reabertura"),
                                            emissaoPrimeiro ? documento : reabrir,
                                            gestor));
            assertThat(travou.await(10, TimeUnit.SECONDS)).isTrue();
            var b =
                    executor.submit(
                            () -> {
                                entrou.countDown();
                                return post(
                                        rota
                                                + (emissaoPrimeiro
                                                        ? "/reabertura"
                                                        : "/referencias-nfse"),
                                        emissaoPrimeiro ? reabrir : documento,
                                        gestor);
                            });
            assertThat(entrou.await(10, TimeUnit.SECONDS)).isTrue();
            try {
                b.get(150, TimeUnit.MILLISECONDS);
                throw new AssertionError("Segundo comando não aguardou lock do contexto");
            } catch (java.util.concurrent.TimeoutException esperado) {
            } finally {
                liberar.countDown();
            }
            f = resposta(a.get(15, TimeUnit.SECONDS), 200);
            resposta(b.get(15, TimeUnit.SECONDS), 409);
        }
        reset(alvoAuditoria);
        if (emissaoPrimeiro) {
            assertThat(contar("versao_fechamento")).isEqualTo(1);
            assertThat(contar("referencia_nfse")).isEqualTo(1);
        } else {
            documento.put("versao", f.get("fechamento").get("versao").longValue());
            f = resposta(post(rota + "/referencias-nfse", documento, gestor), 200);
            assertThat(f.get("fechamento").get("situacao").asString())
                    .isEqualTo("CONFLITO_EXTERNO");
            assertThat(f.get("versao").get("numero").asInt()).isEqualTo(1);
            assertThat(contar("referencia_nfse")).isEqualTo(1);
        }
    }

    @Test
    void finalizacaoFalhaAuditoriaReverteDocumentoAplicacaoEResposta() throws Exception {
        configurar("2026-09-01", "DIAS_CORRIDOS", null, 30, null);
        fato("8", "2026-09-02T12:00:00Z");
        var vinte = fato("2", "2026-09-03T12:00:00Z");
        var origem =
                nfse(aprovar(preparar(calcular("2026-09-01", "2026-10-01"))), "ORIGEMROLLBACK");
        anular(vinte);
        fato("10", "2026-10-02T12:00:00Z");
        var destino = preparar(calcular("2026-10-01", "2026-10-31"));
        var a =
                resposta(
                        post(
                                "/api/v1/ajustes-fechamento",
                                ajusteComando(
                                        origem, destino, calcular("2026-09-01", "2026-10-01")),
                                gestor),
                        200);
        destino = aprovar(atual(destino));
        var d = nfseComando(destino, 2, "FINALROLLBACK");
        doAnswer(
                        inv -> {
                            if ("REFERENCIA_NFSE".equals(inv.getArgument(2)))
                                throw new IllegalStateException("Falha ficticia de auditoria");
                            return inv.callRealMethod();
                        })
                .when(alvoAuditoria)
                .registrar(any(), any(), any(), any(), any(), any());
        resposta(post(rota(destino) + "/referencias-nfse", d, gestor), 500);
        assertThat(contar("referencia_nfse")).isEqualTo(1);
        assertThat(
                        jdbc.queryForObject(
                                "select situacao from wms.ajuste_fechamento where id=?",
                                String.class,
                                a.get("id").longValue()))
                .isEqualTo("VALIDADO");
        assertThat(
                        jdbc.queryForObject(
                                "select aplicado_versao_id from wms.ajuste_fechamento where id=?",
                                Long.class,
                                a.get("id").longValue()))
                .isNull();
        reset(alvoAuditoria);
        destino = resposta(post(rota(destino) + "/referencias-nfse", d, gestor), 200);
        assertThat(destino.get("fechamento").get("situacao").asString()).isEqualTo("EMITIDO");
        assertThat(destino.get("versao").get("ajustes").get(0).get("situacao").asString())
                .isEqualTo("APLICADO");
        assertThat(resposta(post(rota(destino) + "/referencias-nfse", d, gestor), 200))
                .isEqualTo(destino);
    }

    @Test
    void referenciaFiscalNovaNaoPodeOcultarFatoTardioAposAprovacao() throws Exception {
        configurar("2026-09-01", "DIAS_CORRIDOS", null, 30, null);
        fato("10", "2026-09-02T12:00:00Z");
        var f = aprovar(preparar(calcular("2026-09-01", "2026-10-01")));
        fato("1", "2026-09-03T12:00:00Z");
        resposta(post(rota(f) + "/entregas", entregaComando(f), gestor), 409);
        f = nfse(f, "DOCUMENTOJAEXISTENTE");
        assertThat(f.get("fechamento").get("situacao").asString()).isEqualTo("CONFLITO_EXTERNO");
        assertThat(f.get("versao").get("nfse").size()).isEqualTo(1);
    }

    @ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(
            strings = {
                "cliente",
                "armazem",
                "produto",
                "embalagem",
                "endereco",
                "conjunto_posicoes"
            })
    void seisCadastrosNaoEntramInativoPorTransicaoComumNemReativamHistorico(String tabela)
            throws Exception {
        var ids =
                new TransactionTemplate(transactions)
                        .execute(
                                tx -> {
                                    var a = em.find(Armazem.class, armazemId);
                                    var e1 =
                                            new Endereco(
                                                    a,
                                                    "E1",
                                                    "R1",
                                                    0,
                                                    "01",
                                                    "Endereco ficticio",
                                                    TipoEndereco.ARMAZENAGEM,
                                                    BigDecimal.TEN,
                                                    BigDecimal.ONE,
                                                    BigDecimal.ONE,
                                                    BigDecimal.ONE,
                                                    1,
                                                    1,
                                                    BASE);
                                    var e2 =
                                            new Endereco(
                                                    a,
                                                    "E2",
                                                    "R2",
                                                    0,
                                                    "01",
                                                    "Endereco ficticio",
                                                    TipoEndereco.ARMAZENAGEM,
                                                    BigDecimal.TEN,
                                                    BigDecimal.ONE,
                                                    BigDecimal.ONE,
                                                    BigDecimal.ONE,
                                                    1,
                                                    1,
                                                    BASE);
                                    em.persist(e1);
                                    em.persist(e2);
                                    var conjunto =
                                            new br.com.rodogarcia.wms.models.ConjuntoPosicoes(
                                                    a,
                                                    "CO",
                                                    e1,
                                                    e2,
                                                    BigDecimal.TEN,
                                                    BigDecimal.ONE,
                                                    BigDecimal.ONE,
                                                    BigDecimal.ONE,
                                                    1,
                                                    BASE);
                                    em.persist(conjunto);
                                    em.flush();
                                    return Map.of(
                                            "cliente",
                                            clienteId,
                                            "armazem",
                                            armazemId,
                                            "produto",
                                            produtoId,
                                            "embalagem",
                                            embalagemId,
                                            "endereco",
                                            e1.getId(),
                                            "conjunto_posicoes",
                                            conjunto.getId());
                                });
        long id = ids.get(tabela);
        var tipo =
                switch (tabela) {
                    case "cliente" -> Cliente.class;
                    case "armazem" -> Armazem.class;
                    case "produto" -> Produto.class;
                    case "embalagem" -> Embalagem.class;
                    case "endereco" -> Endereco.class;
                    default -> br.com.rodogarcia.wms.models.ConjuntoPosicoes.class;
                };
        assertThatThrownBy(
                        () ->
                                new TransactionTemplate(transactions)
                                        .executeWithoutResult(
                                                tx ->
                                                        br.com.rodogarcia.wms.services
                                                                .CadastroSupport.transicao(
                                                                em.find(tipo, id),
                                                                br.com.rodogarcia.wms.models
                                                                        .SituacaoCadastro.INATIVO)))
                .isInstanceOf(br.com.rodogarcia.wms.exceptions.RegraNegocioException.class);
        jdbc.update("update wms." + tabela + " set situacao='INATIVO' where id=?", id);
        String recurso =
                switch (tabela) {
                    case "cliente" -> "clientes";
                    case "armazem" -> "armazens";
                    case "produto" -> "produtos";
                    case "embalagem" -> "embalagens";
                    case "endereco" -> "enderecos";
                    default -> "conjuntos-posicoes";
                };
        var d = comando();
        d.remove("operacaoId");
        d.put(
                "versao",
                jdbc.queryForObject(
                        "select versao from wms." + tabela + " where id=?", Long.class, id));
        resposta(post("/api/v1/" + recurso + "/" + id + "/encerramento", d, gestor), 409);
        if (!tabela.equals("conjunto_posicoes"))
            resposta(post("/api/v1/" + recurso + "/" + id + "/reativacao", d, gestor), 409);
        assertThat(
                        jdbc.queryForObject(
                                "select situacao from wms." + tabela + " where id=?",
                                String.class,
                                id))
                .isEqualTo("INATIVO");
    }

    @ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(
            strings = {
                "CLIENTE",
                "ARMAZEM",
                "PRODUTO",
                "EMBALAGEM",
                "ENDERECO",
                "CONJUNTO_POSICOES",
                "SERVICO_COBRANCA"
            })
    void d30SeteAlvosSemCompromissoConcluemEstadoComReplayEConservacao(String tipo)
            throws Exception {
        long id =
                new TransactionTemplate(transactions)
                        .execute(
                                tx -> {
                                    if (tipo.equals("CLIENTE")) return clienteId;
                                    if (tipo.equals("ARMAZEM")) return armazemId;
                                    if (tipo.equals("PRODUTO")) return produtoId;
                                    if (tipo.equals("EMBALAGEM")) return embalagemId;
                                    if (tipo.equals("SERVICO_COBRANCA")) {
                                        var s =
                                                new br.com.rodogarcia.wms.models.ServicoCobranca(
                                                        "D30-LIVRE",
                                                        "Servico livre ficticio",
                                                        "ADICIONAL",
                                                        "VEICULO",
                                                        "ATIVO",
                                                        BASE,
                                                        BASE);
                                        em.persist(s);
                                        em.flush();
                                        return s.getId();
                                    }
                                    var a = em.find(Armazem.class, armazemId);
                                    var e1 =
                                            new Endereco(
                                                    a,
                                                    "D30-LIVRE-A",
                                                    "A",
                                                    0,
                                                    "01",
                                                    "Endereco livre",
                                                    TipoEndereco.ARMAZENAGEM,
                                                    BigDecimal.TEN,
                                                    BigDecimal.ONE,
                                                    BigDecimal.ONE,
                                                    BigDecimal.ONE,
                                                    1,
                                                    0,
                                                    BASE);
                                    em.persist(e1);
                                    em.flush();
                                    if (tipo.equals("ENDERECO")) return e1.getId();
                                    var e2 =
                                            new Endereco(
                                                    a,
                                                    "D30-LIVRE-B",
                                                    "A",
                                                    0,
                                                    "02",
                                                    "Endereco livre",
                                                    TipoEndereco.ARMAZENAGEM,
                                                    BigDecimal.TEN,
                                                    BigDecimal.ONE,
                                                    BigDecimal.ONE,
                                                    BigDecimal.ONE,
                                                    1,
                                                    1,
                                                    BASE);
                                    em.persist(e2);
                                    var c =
                                            new br.com.rodogarcia.wms.models.ConjuntoPosicoes(
                                                    a,
                                                    "D30-LIVRE",
                                                    e1,
                                                    e2,
                                                    new BigDecimal("20"),
                                                    new BigDecimal("2"),
                                                    BigDecimal.ONE,
                                                    BigDecimal.ONE,
                                                    1,
                                                    BASE);
                                    em.persist(c);
                                    em.flush();
                                    return c.getId();
                                });
        String tabela = tipo.toLowerCase(java.util.Locale.ROOT);
        String rota = "/api/v1/encerramentos/" + tipo + "/" + id;
        var antes = D30FotografiaFisica.capturar(jdbc);
        var livre = resposta(get(rota + "/impedimentos", gestor), 200);
        assertThat(livre.get("situacao").asString()).isEqualTo("ATIVO");
        assertThat(livre.get("versao").longValue()).isZero();
        assertThat(livre.get("impedimentos").isEmpty()).isTrue();
        assertThat(D30FotografiaFisica.capturar(jdbc)).isEqualTo(antes);
        var solicitar = comando();
        solicitar.put("versao", 0L);
        solicitar.put("motivo", "D30OK");
        var pendente = resposta(post(rota + "/solicitar", solicitar, gestor), 200);
        assertThat(pendente.get("situacao").asString()).isEqualTo("ENCERRAMENTO_PENDENTE");
        assertThat(pendente.get("versao").longValue()).isEqualTo(1);
        var inativar = comando();
        inativar.put("versao", 1L);
        inativar.put("motivo", "M".repeat(500));
        var inativo = resposta(post(rota + "/inativar", inativar, gestor), 200);
        assertThat(inativo.get("situacao").asString()).isEqualTo("INATIVO");
        assertThat(inativo.get("versao").longValue()).isEqualTo(2);
        var depois = D30FotografiaFisica.capturar(jdbc);
        for (String t : antes.keySet()) {
            if (!java.util.Set.of(
                            tabela.toUpperCase(java.util.Locale.ROOT),
                            "AUDITORIA_CADASTRO",
                            "OPERACAO_ADMINISTRATIVA")
                    .contains(t)) assertThat(depois.get(t)).as(t).isEqualTo(antes.get(t));
        }
        assertThat(
                        jdbc.queryForObject(
                                "select count(*) from wms.operacao_administrativa where tipo in ('SOLICITACAO_ENCERRAMENTO','INATIVACAO_DEFINITIVA')",
                                Integer.class))
                .isEqualTo(2);
        assertThat(resposta(post(rota + "/solicitar", solicitar, gestor), 200)).isEqualTo(pendente);
        assertThat(resposta(post(rota + "/inativar", inativar, gestor), 200)).isEqualTo(inativo);
        resposta(post(rota + "/inativar", inativar, supervisor), 403);
        assertThat(resposta(get(rota + "/impedimentos", gestor), 200)).isEqualTo(inativo);
        assertThat(D30FotografiaFisica.capturar(jdbc)).isEqualTo(depois);
    }

    private Map<String, Object> nfseComando(JsonNode f, int numero, String referencia) {
        var d = decisaoNumero(f, numero);
        d.put("emissorDocumento", "99999999000199");
        d.put("referenciaExterna", referencia);
        d.put("emitidaEm", "2028-06-06T12:00:00Z");
        d.put("fonte", "Documento externo existente ficticio");
        d.put("conferidaPor", "Natalina ficticia");
        return d;
    }

    private JsonNode configurar(
            String inicio, String modalidade, Integer nominal, Integer dias, String fim)
            throws Exception {
        var s = servico("ADICIONAL", "VEICULO");
        var t = tabela("T", inicio, null, List.of(item(s, "", "10")));
        vincular(t, inicio, null);
        configurarContrato(inicio, modalidade, nominal, dias, fim);
        return s;
    }

    private void configurarContrato(
            String inicio, String modalidade, Integer nominal, Integer dias, String fim)
            throws Exception {
        var d = comando();
        d.put("clienteId", clienteId);
        d.put("armazemId", armazemId);
        d.put("vigenciaInicio", inicio);
        if (fim != null) d.put("vigenciaFim", fim);
        d.put("fuso", "UTC");
        d.put("moeda", "BRL");
        d.put("modalidadeCiclo", modalidade);
        if (nominal != null) d.put("diaCorte", nominal);
        if (dias != null) d.put("duracaoDias", dias);
        d.put("minimoModo", "NAO_APLICAVEL");
        d.put("grisModo", "NAO_APLICAVEL");
        d.put("servicosMinimo", List.of());
        resposta(post("/api/v1/contratos-cobranca", d, gestor), 200);
    }

    private JsonNode fato(String quantidade, String data) throws Exception {
        var u = unidade("10", TipoUnidadeLogistica.PALLET, data, 1, TipoEndereco.TRIAGEM);
        var d = comando();
        d.put("clienteId", clienteId);
        d.put("armazemId", armazemId);
        d.put(
                "servicoId",
                jdbc.queryForObject(
                        "select id from wms.servico_cobranca where tipo='ADICIONAL'", Long.class));
        d.put("origem", "MANUAL");
        d.put("pedidoEntradaId", u.pedidoId());
        d.put("referenciaExecucao", "EXEC" + (++numero));
        d.put("executadoEm", data);
        d.put("quantidade", quantidade);
        d.put("categoria", "");
        d.put("cotas", List.of(Map.of("notaId", u.notaId(), "cota", "1")));
        d.put("criterioRateio", "Criterio ficticio comprovado");
        return resposta(post("/api/v1/fatos-servico", d, supervisor), 200);
    }

    private void anular(JsonNode fato) throws Exception {
        var d = comando();
        d.put("versao", fato.get("versao").longValue());
        resposta(
                post(
                        "/api/v1/fatos-servico/" + fato.get("id").longValue() + "/anulacao",
                        d,
                        gestor),
                200);
    }

    private Map<String, Object> prepararComando(JsonNode calc) {
        var d = comando();
        d.put("calculoId", calc.get("id").longValue());
        return d;
    }

    private JsonNode preparar(JsonNode calc) throws Exception {
        return resposta(post("/api/v1/fechamentos-cobranca", prepararComando(calc), gestor), 200);
    }

    private String rota(JsonNode f) {
        return "/api/v1/fechamentos-cobranca/" + f.get("fechamento").get("id").longValue();
    }

    private Map<String, Object> decisao(JsonNode f) {
        return decisaoNumero(f, f.get("fechamento").get("versaoAtual").asInt());
    }

    private Map<String, Object> decisaoNumero(JsonNode f, int numero) {
        var d = comando();
        d.put("versao", f.get("fechamento").get("versao").longValue());
        d.put("numero", numero);
        return d;
    }

    private JsonNode aprovar(JsonNode f) throws Exception {
        return resposta(post(rota(f) + "/aprovacao", decisao(f), gestor), 200);
    }

    private Map<String, Object> reabrirComando(JsonNode f, JsonNode calc) {
        var d = decisao(f);
        d.put("calculoId", calc.get("id").longValue());
        return d;
    }

    private JsonNode reabrir(JsonNode f, JsonNode calc) throws Exception {
        return resposta(post(rota(f) + "/reabertura", reabrirComando(f, calc), gestor), 200);
    }

    private Map<String, Object> entregaComando(JsonNode f) {
        var d = decisao(f);
        d.put("layoutVersao", 1);
        d.put("arquivoHash", f.get("versao").get("conteudoHash").asString());
        d.put("destinoReferencia", "Entrega manual ficticia ESL");
        d.put("entregueEm", "2028-06-06T12:00:00Z");
        return d;
    }

    private JsonNode entregar(JsonNode f) throws Exception {
        return resposta(post(rota(f) + "/entregas", entregaComando(f), gestor), 200);
    }

    private JsonNode confirmar(JsonNode f) throws Exception {
        var d = decisao(f);
        d.put("fonte", "Conferencia externa ficticia comprovada");
        d.put("confirmadaPor", "Natalina ficticia");
        d.put("confirmadaEm", "2028-06-06T12:00:00Z");
        return resposta(post(rota(f) + "/confirmacoes-externas", d, gestor), 200);
    }

    private JsonNode resolver(JsonNode f) throws Exception {
        var d = decisao(f);
        d.put("referenciaExterna", "Resolucao financeira ficticia");
        d.put("fonte", "Comprovacao externa ficticia");
        d.put("confirmadaPor", "Responsavel ficticio");
        d.put("confirmadaEm", "2028-06-06T12:00:00Z");
        return resposta(post(rota(f) + "/resolucao-financeira", d, gestor), 200);
    }

    private JsonNode nfse(JsonNode f, String referencia) throws Exception {
        return nfseNumero(f, f.get("fechamento").get("versaoAtual").asInt(), referencia);
    }

    private JsonNode nfseNumero(JsonNode f, int numero, String referencia) throws Exception {
        var d = decisaoNumero(f, numero);
        d.put("emissorDocumento", "99999999000199");
        d.put("referenciaExterna", referencia);
        d.put("emitidaEm", "2028-06-06T12:00:00Z");
        d.put("fonte", "Documento externo existente ficticio");
        d.put("conferidaPor", "Natalina ficticia");
        return resposta(post(rota(f) + "/referencias-nfse", d, gestor), 200);
    }

    private Map<String, Object> tratativaComando(
            JsonNode f, int numero, String resultado, List<Map<String, Object>> refs) {
        var d = decisaoNumero(f, numero);
        d.remove("numero");
        d.put("numeroResultado", numero);
        d.put("resultado", resultado);
        d.put("referencias", refs);
        d.put("fonte", "Cancelamento externo comprovado ficticio");
        d.put("conferidaPor", "Natalina ficticia");
        d.put("conferidaEm", "2028-06-06T12:00:00Z");
        return d;
    }

    private Map<String, Object> ajusteComando(JsonNode origem, JsonNode destino, JsonNode calc) {
        var d = comando();
        d.put("origemVersaoId", origem.get("versao").get("id").longValue());
        d.put("destinoFechamentoId", destino.get("fechamento").get("id").longValue());
        d.put("versaoDestino", destino.get("fechamento").get("versao").longValue());
        d.put("calculoCorrigidoId", calc.get("id").longValue());
        d.put("evidencia", "Correcoes dos fatos executados ficticios");
        return d;
    }

    private JsonNode atual(JsonNode f) throws Exception {
        var fechamento = resposta(get(rota(f), supervisor), 200);
        var v =
                resposta(
                        get(
                                rota(f) + "/versoes/" + fechamento.get("versaoAtual").asInt(),
                                supervisor),
                        200);
        return mapper.valueToTree(Map.of("fechamento", fechamento, "versao", v));
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

    private JsonNode d30GetFotografiaIntegral(String rota, String token) throws Exception {
        var antes = D30FotografiaFisica.capturar(jdbc);
        var resultado = resposta(get(rota, token), 200);
        assertThat(resultado.isNull()).as(rota + " resposta positiva").isFalse();
        assertThat(D30FotografiaFisica.capturar(jdbc))
                .as(rota + " conserva todas64tabelas")
                .isEqualTo(antes);
        return resultado;
    }
}
