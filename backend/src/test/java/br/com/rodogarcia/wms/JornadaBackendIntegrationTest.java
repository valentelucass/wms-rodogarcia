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
import br.com.rodogarcia.wms.services.MovimentacaoEstoqueService;
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
                "spring.datasource.url=jdbc:h2:mem:wms-jornada;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000;INIT=CREATE SCHEMA IF NOT EXISTS wms")
@Import(IdentidadeTesteConfig.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class JornadaBackendIntegrationTest {
    @org.springframework.test.context.bean.override.mockito.MockitoSpyBean
    private br.com.rodogarcia.wms.services.AuditoriaService auditoria;

    private void limpar() {
        br.com.rodogarcia.wms.services.AuditoriaService alvo =
                org.springframework.test.util.AopTestUtils.getUltimateTargetObject(auditoria);
        org.mockito.Mockito.reset(alvo);
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
    @Autowired private MovimentacaoEstoqueService movimentos;
    private Cliente cliente;
    private Armazem armazem;
    private Produto produto;
    private Embalagem embalagem;
    private String operador;
    private String supervisor;
    private String gestor;
    private long numero;
    private long clienteId;
    private long armazemId;
    @Autowired private org.springframework.security.oauth2.jwt.JwtDecoder decoder;
    @Autowired private br.com.rodogarcia.wms.services.ContagemEstoqueService contagens;
    @Autowired private br.com.rodogarcia.wms.services.ExpedicaoService expedicao;

    @org.springframework.test.context.bean.override.mockito.MockitoBean
    private java.time.Clock clock;

    private final java.util.concurrent.atomic.AtomicReference<Instant> instante =
            new java.util.concurrent.atomic.AtomicReference<>();

    @BeforeEach
    void preparar() {
        instante.set(Instant.parse("2026-09-01T12:00:00Z"));
        org.mockito.Mockito.when(clock.instant()).thenAnswer(i -> instante.get());
        org.mockito.Mockito.when(clock.getZone()).thenReturn(java.time.ZoneOffset.UTC);
        org.mockito.Mockito.when(
                        clock.withZone(org.mockito.ArgumentMatchers.any(java.time.ZoneId.class)))
                .thenAnswer(i -> java.time.Clock.fixed(instante.get(), i.getArgument(0)));
        limpar();
        var agora = clock.instant();
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
        clienteId = cliente.getId();
        armazemId = armazem.getId();
        criarProduto();
        gestor = token("GESTOR", List.of(), List.of());
        operador = token("OPERACAO", List.of(cliente.getId()), List.of(armazem.getId()));
        supervisor = token("SUPERVISOR", List.of(cliente.getId()), List.of(armazem.getId()));
        numero = 0;
    }

    @Test
    void recebimentoEmPartesAteFechamentoEResolucaoSemDuplicarSaldoOuFato() throws Exception {
        long pe = iniciarEntrada("JORNADA", "100", "1000");
        chegada(pe, "50", "2026-09-01T12:00:00Z");
        avancar("2026-09-02T12:00:00Z");
        chegada(pe, "50", "2026-09-02T12:00:00Z");
        efetivar(pe);
        var entradas = resposta(get(PEDIDOS + pe + "/entradas", operador), 200).get("itens");
        assertThat(entradas.size()).isEqualTo(2);
        var a = unitizarEntrada(pe, entradas.get(0).get("id").longValue(), "50");
        var b = unitizarEntrada(pe, entradas.get(1).get("id").longValue(), "50");
        assertThat(a.get("codigo").asString()).isNotEqualTo(b.get("codigo").asString());
        String etiquetaB = b.get("codigo").asString();
        long nota =
                resposta(get(PEDIDOS + pe, operador), 200)
                        .get("notas")
                        .get(0)
                        .get("id")
                        .longValue();
        var ea = endereco("ARM-A", "ARMAZENAGEM");
        var eb = endereco("ARM-B", "ARMAZENAGEM");
        posicionar(a, ea);
        posicionar(b, eb);
        a = atual(a);
        b = atual(b);
        assertThat(saldo().get("fisicoTotal").decimalValue()).isEqualByComparingTo("100");
        var sa = servico("ARMAZENAGEM", "POSICAO_DIA");
        var ss = servico("SAIDA", "QUANTIDADE_PRODUTO");
        var tab =
                tabela(
                        "JORNADA",
                        "2026-09-01",
                        null,
                        List.of(item(sa, "PALLET", "2"), item(ss, "", "1")));
        var vinc = vincular(tab, "2026-09-01", null);
        configurarContrato("2026-09-01", "MES_DIA_FIXO", 1, null, null);
        avancar("2026-09-10T12:00:00Z");
        var p = reservar("60");
        assertThat(p.get("reservas").size()).isEqualTo(2);
        assertThat(p.get("reservas").get(0).get("quantidade").decimalValue())
                .isEqualByComparingTo("50");
        assertThat(p.get("reservas").get(1).get("quantidade").decimalValue())
                .isEqualByComparingTo("10");
        assertThat(saldo().get("reservado").decimalValue()).isEqualByComparingTo("60");
        assertThat(saldo().get("disponivel").decimalValue()).isZero();
        var sp = endereco("SEP-A", "SEPARACAO");
        var sq = endereco("SEP-B", "SEPARACAO");
        p = separar(p, a, sp);
        p = separar(p, b, sq);
        var doc = documento(p, nota);
        var registrado = resposta(post(saida(p) + "/documentos", doc, supervisor), 200);
        assertThat(resposta(post(saida(p) + "/documentos", doc, supervisor), 200))
                .isEqualTo(registrado);
        p = registrado.get("expedicao").get("pedido");
        assertThat(saldo().get("fisicoTotal").decimalValue()).isEqualByComparingTo("100");
        var tirar = retirada(p, List.of(destinacao(p, b, eb)));
        resposta(post(saida(p) + "/retirada", tirar, operador), 403);
        var retirado = resposta(post(saida(p) + "/retirada", tirar, supervisor), 200);
        assertThat(resposta(post(saida(p) + "/retirada", tirar, supervisor), 200))
                .isEqualTo(retirado);
        p = retirado.get("expedicao").get("pedido");
        assertThat(p.get("situacao").asString()).isEqualTo("RETIRADO");
        assertThat(retirado.get("expedicao").get("baixas").size()).isEqualTo(2);
        assertThat(saldo().get("fisicoTotal").decimalValue()).isEqualByComparingTo("40");
        assertThat(saldo().get("reservado").decimalValue()).isZero();
        assertThat(saldo().get("disponivel").decimalValue()).isEqualByComparingTo("40");
        b = atual(b);
        assertThat(b.get("codigo").asString()).isEqualTo(etiquetaB);
        assertThat(b.get("quantidade").decimalValue()).isEqualByComparingTo("40");
        assertThat(b.get("dataFifo").asString()).isEqualTo("2026-09-01T12:00:00Z");
        assertThat(jdbc.queryForObject("select count(*) from wms.entrada_conferida", Integer.class))
                .isEqualTo(2);
        assertThat(
                        jdbc.queryForObject(
                                "select sum(quantidade) from wms.baixa_saida", BigDecimal.class))
                .isEqualByComparingTo("60");
        var fato = comando();
        fato.put("clienteId", clienteId);
        fato.put("armazemId", armazemId);
        fato.put("servicoId", ss.get("id").longValue());
        fato.put("origem", "MANUAL");
        fato.put("pedidoSaidaId", p.get("id").longValue());
        fato.put("produtoId", produto.getId());
        fato.put("categoria", "");
        fato.put("cotas", List.of());
        fato.put("criterioRateio", "Baixas comprovadas por origem");
        var f = resposta(post("/api/v1/fatos-servico", fato, supervisor), 200);
        assertThat(f.get("quantidade").decimalValue()).isEqualByComparingTo("60");
        assertThat(f.get("cotas").get(0).get("notaId").longValue()).isEqualTo(nota);
        assertThat(resposta(post("/api/v1/fatos-servico", fato, supervisor), 200)).isEqualTo(f);
        fato.put("operacaoId", UUID.randomUUID());
        fato.put("origem", "SUGESTAO");
        resposta(post("/api/v1/fatos-servico", fato, supervisor), 409);
        assertThat(jdbc.queryForObject("select count(*) from wms.fato_servico", Integer.class))
                .isEqualTo(1);
        avancar("2026-09-25T12:00:00Z");
        var c = contar(b, "0");
        resposta(
                post(
                        "/api/v1/contagens/" + c.get("id").longValue() + "/aplicar",
                        aplicar(c, b),
                        supervisor),
                200);
        assertThat(saldo().get("fisicoTotal").decimalValue()).isZero();
        assertThat(
                        jdbc.queryForObject(
                                "select quantidade_prevista from wms.item_nota_entrada where nota_id=?",
                                BigDecimal.class,
                                nota))
                .isEqualByComparingTo("100");
        avancar("2026-10-01T12:00:00Z");
        var calc = calcular("2026-09-01", "2026-10-01");
        assertThat(calc.get("situacao").asString())
                .as(calc.get("pendencias").toPrettyString())
                .isEqualTo("COMPLETO");
        assertThat(calc.get("pendencias").isEmpty()).isTrue();
        assertThat(calc.get("memoria").get("servicos").size()).isEqualTo(1);
        assertThat(calc.get("memoria").get("servicos").get(0).get("valor").decimalValue())
                .isEqualByComparingTo("60");
        assertPico(calc, "2026-09-02", "2");
        assertPico(calc, "2026-09-10", "1");
        assertPico(calc, "2026-09-24", "1");
        assertPico(calc, "2026-09-25", "0");
        assertThat(calc.get("total").decimalValue()).isEqualByComparingTo("122");
        var fecho = preparar(calc);
        String demonstrativo = get(rota(fecho) + "/versoes/1/demonstrativo", supervisor).body();
        String hash = fecho.get("versao").get("conteudoHash").asString();
        fecho = aprovar(fecho);
        fecho = entregar(fecho);
        assertThat(fecho.get("versao").get("estadoExterno").asString()).isEqualTo("DESCONHECIDO");
        fecho = nfseNumero(fecho, 1, "NFSE-FICTICIA-JORNADA");
        assertThat(fecho.get("fechamento").get("situacao").asString()).isEqualTo("EMITIDO");
        assertThat(fecho.get("versao").get("conteudoHash").asString()).isEqualTo(hash);
        assertThat(get(rota(fecho) + "/versoes/1/demonstrativo", supervisor).body())
                .isEqualTo(demonstrativo);
        long contrato = calc.get("contratoId").longValue();
        for (var t :
                List.of(
                        Map.entry("CONTRATO", contrato),
                        Map.entry("TABELA", tab.get("id").longValue()),
                        Map.entry("VINCULO", vinc.get("id").longValue()))) {
            var enc = comando();
            enc.put(
                    "versao",
                    jdbc.queryForObject(
                            "select versao from wms."
                                    + (t.getKey().equals("CONTRATO")
                                            ? "contrato_cobranca"
                                            : t.getKey().equals("TABELA")
                                                    ? "tabela_cobranca"
                                                    : "vinculo_tabela_cliente")
                                    + " where id=?",
                            Long.class,
                            t.getValue()));
            enc.put("corte", "2026-10-01");
            enc.put(
                    "resolucao",
                    Map.of(
                            "tipo",
                            "FECHAMENTO",
                            "compromissoId",
                            fecho.get("fechamento").get("id").longValue()));
            resposta(
                    post(
                            "/api/v1/encerramentos/vigencias/"
                                    + t.getKey()
                                    + "/"
                                    + t.getValue()
                                    + "/encerrar",
                            enc,
                            gestor),
                    200);
        }
        String encCliente = "/api/v1/encerramentos/CLIENTE/" + clienteId;
        var enc = comando();
        enc.put(
                "versao",
                resposta(get("/api/v1/clientes/" + clienteId, gestor), 200)
                        .get("versao")
                        .longValue());
        var pendente = resposta(post(encCliente + "/solicitar", enc, gestor), 200);
        var imped = resposta(get(encCliente + "/impedimentos", gestor), 200);
        assertThat(imped.get("impedimentos")).as(imped.toPrettyString()).isEmpty();
        enc = comando();
        enc.put("versao", pendente.get("versao").longValue());
        var inativo = resposta(post(encCliente + "/inativar", enc, gestor), 200);
        assertThat(inativo.get("situacao").asString()).isEqualTo("INATIVO");
        resposta(post(encCliente + "/inativar", enc, supervisor), 403);
        assertThat(get(rota(fecho) + "/versoes/1/demonstrativo", supervisor).body())
                .isEqualTo(demonstrativo);
    }

    @Test
    void entradaTemporalEfetivaUmaVezComFIFOComprovadoEDivergenciaSemNovoSaldo() throws Exception {
        long pe = iniciarEntrada("CONT-ENT", "100", null);
        chegada(pe, "100", "2026-09-01T12:00:00Z");
        long versao = versao(pe);
        avancar("2026-09-05T12:00:00Z");
        var dados =
                Map.<String, Object>of(
                        "versao",
                        versao,
                        "motivo",
                        "Conferencia integral comprovada",
                        "aceitarDivergencias",
                        false);
        var registro =
                linhaTemporal(
                        "ENTRADA-TEMPORAL",
                        "ENTRADA",
                        "2026-09-02T12:00:00Z",
                        false,
                        Map.of("pedidoId", pe, "dados", dados));
        var l = resposta(post("/api/v1/contingencias", registro, supervisor), 200);
        var conciliar = conciliacao(l);
        resposta(post(contingencia(l), conciliar, operador), 403);
        var original = resposta(post(contingencia(l), conciliar, supervisor), 200);
        assertThat(original.get("situacao").asString()).isEqualTo("CONCILIADA");
        assertThat(original.get("resultado").get("efeito").get("efetivadoEm").asString())
                .isEqualTo("2026-09-02T12:00:00Z");
        var pedidoEfetivado = resposta(get(PEDIDOS + pe, operador), 200).get("pedido");
        assertThat(pedidoEfetivado.get("efetivadoEm").asString()).isEqualTo("2026-09-02T12:00:00Z");
        assertThat(pedidoEfetivado.get("alteradoEm").asString()).startsWith("2026-09-05T12:00:00");
        var es = resposta(get(PEDIDOS + pe + "/entradas", operador), 200).get("itens");
        assertThat(es.size()).isEqualTo(1);
        assertThat(es.get(0).get("efetivadaEm").asString()).isEqualTo("2026-09-02T12:00:00Z");
        assertThat(es.get(0).get("dataFifo").asString()).isEqualTo("2026-09-01T12:00:00Z");
        assertThat(saldo().get("fisicoTotal").decimalValue()).isEqualByComparingTo("100");
        assertThat(resposta(post(contingencia(l), conciliar, supervisor), 200)).isEqualTo(original);
        registro.put("operacaoId", UUID.randomUUID());
        assertThat(resposta(post("/api/v1/contingencias", registro, supervisor), 200)).isEqualTo(l);
        registro.put("ocorridaEm", "2026-09-03T12:00:00Z");
        resposta(post("/api/v1/contingencias", registro, supervisor), 409);
        assertThat(jdbc.queryForObject("select count(*) from wms.entrada_conferida", Integer.class))
                .isEqualTo(1);
        assertThat(
                        jdbc.queryForObject(
                                "select count(*) from wms.operacao_administrativa where tipo='ENTRADA_CONTINGENCIA'",
                                Integer.class))
                .isEqualTo(1);
    }

    @Test
    void ajusteTemporalAuditoriaFalhaReverteERepeticaoConservaMarcoReal() throws Exception {
        var u = unidadeArmazenada("100", "AJUSTE");
        avancar("2026-09-02T12:00:00Z");
        var c = contar(u, "70");
        var a = aplicar(c, u);
        avancar("2026-09-05T12:00:00Z");
        var l =
                resposta(
                        post(
                                "/api/v1/contingencias",
                                linhaTemporal(
                                        "AJUSTE-TEMPORAL",
                                        "AJUSTE",
                                        "2026-09-02T13:00:00Z",
                                        false,
                                        Map.of("contagemId", c.get("id").longValue(), "dados", a)),
                                supervisor),
                        200);
        var d = conciliacao(l);
        br.com.rodogarcia.wms.services.AuditoriaService alvo =
                org.springframework.test.util.AopTestUtils.getUltimateTargetObject(auditoria);
        org.mockito.Mockito.doAnswer(
                        inv -> {
                            if ("CONCILIACAO_CONTINGENCIA".equals(inv.getArgument(2)))
                                throw new IllegalStateException("Falha de auditoria ficticia");
                            return inv.callRealMethod();
                        })
                .when(alvo)
                .registrar(
                        org.mockito.ArgumentMatchers.anyString(),
                        org.mockito.ArgumentMatchers.anyLong(),
                        org.mockito.ArgumentMatchers.anyString(),
                        org.mockito.ArgumentMatchers.anyString(),
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any());
        resposta(post(contingencia(l), d, supervisor), 500);
        assertThat(atual(u).get("quantidade").decimalValue()).isEqualByComparingTo("100");
        assertThat(
                        jdbc.queryForObject(
                                "select count(*) from wms.movimento_estoque where acao='AJUSTE_ESTOQUE'",
                                Integer.class))
                .isZero();
        assertThat(
                        jdbc.queryForObject(
                                "select count(*) from wms.operacao_administrativa where tipo='APLICACAO_CONTAGEM'",
                                Integer.class))
                .isZero();
        assertThat(
                        resposta(
                                        get(
                                                "/api/v1/contingencias/" + l.get("id").longValue(),
                                                supervisor),
                                        200)
                                .get("situacao")
                                .asString())
                .isEqualTo("PENDENTE");
        org.mockito.Mockito.reset(alvo);
        var r = resposta(post(contingencia(l), d, supervisor), 200);
        assertThat(r.get("situacao").asString()).isEqualTo("CONCILIADA");
        assertThat(atual(u).get("quantidade").decimalValue()).isEqualByComparingTo("70");
        assertThat(
                        jdbc.queryForObject(
                                        "select instante from wms.movimento_estoque where acao='AJUSTE_ESTOQUE'",
                                        java.time.LocalDateTime.class)
                                .toInstant(java.time.ZoneOffset.UTC))
                .isEqualTo(Instant.parse("2026-09-02T13:00:00Z"));
        assertThat(resposta(post(contingencia(l), d, supervisor), 200)).isEqualTo(r);
        assertThat(
                        resposta(
                                        get(
                                                "/api/v1/contagens/"
                                                        + c.get("id").longValue()
                                                        + "/revisoes",
                                                operador),
                                        200)
                                .get("itens")
                                .get(0)
                                .get("efeitoJson")
                                .asString())
                .contains("2026-09-02T13:00:00Z");
    }

    @Test
    void remanejamentoTemporalMantemOrigemEImpedimentoQuandoHistoriaPosterior() throws Exception {
        var u = unidadeArmazenada("100", "REM");
        var b = endereco("REM-B", "ARMAZENAGEM");
        var c = endereco("REM-C", "ARMAZENAGEM");
        var movimento = posicionamento(u, b);
        String fifo = u.get("dataFifo").asString();
        avancar("2026-09-05T12:00:00Z");
        var l =
                resposta(
                        post(
                                "/api/v1/contingencias",
                                linhaTemporal(
                                        "REM-TEMPORAL",
                                        "REMANEJAMENTO",
                                        "2026-09-02T12:00:00Z",
                                        false,
                                        Map.of(
                                                "codigoUnidade",
                                                u.get("codigo").asString(),
                                                "dados",
                                                movimento)),
                                supervisor),
                        200);
        var d = conciliacao(l);
        var r = resposta(post(contingencia(l), d, supervisor), 200);
        assertThat(r.get("situacao").asString()).isEqualTo("CONCILIADA");
        assertThat(atual(u).get("dataFifo").asString()).isEqualTo(fifo);
        assertThat(
                        jdbc.queryForObject(
                                        "select instante from wms.movimento_estoque where acao='MOVIMENTACAO'",
                                        java.time.LocalDateTime.class)
                                .toInstant(java.time.ZoneOffset.UTC))
                .isEqualTo(Instant.parse("2026-09-02T12:00:00Z"));
        assertThat(estoque(u).get("posicoes").get(0).get("enderecoId").longValue())
                .isEqualTo(b.get("id").longValue());
        assertThat(resposta(post(contingencia(l), d, supervisor), 200)).isEqualTo(r);
        u = atual(u);
        var antiga =
                resposta(
                        post(
                                "/api/v1/contingencias",
                                linhaTemporal(
                                        "REM-ANTERIOR",
                                        "REMANEJAMENTO",
                                        "2026-09-01T13:00:00Z",
                                        false,
                                        Map.of(
                                                "codigoUnidade",
                                                u.get("codigo").asString(),
                                                "dados",
                                                posicionamento(u, c))),
                                supervisor),
                        200);
        var pendente = resposta(post(contingencia(antiga), conciliacao(antiga), supervisor), 200);
        assertThat(pendente.get("situacao").asString()).isEqualTo("PENDENTE");
        assertThat(pendente.get("pendencia").asString())
                .isEqualTo("HISTORICO_CONTINGENCIA_INSUFICIENTE");
        assertThat(estoque(u).get("posicoes").get(0).get("enderecoId").longValue())
                .isEqualTo(b.get("id").longValue());
        assertThat(saldo().get("fisicoTotal").decimalValue()).isEqualByComparingTo("100");
    }

    @Test
    void retiradaTemporalPassadaBaixaPedidoInteiroMantemRemanescenteEReservaAteConfirmar()
            throws Exception {
        var u = unidadeArmazenada("100", "RET");
        long nota = jdbc.queryForObject("select nota_id from wms.item_nota_entrada", Long.class);
        var origem =
                resposta(
                        get(
                                "/api/v1/enderecos/"
                                        + estoque(u)
                                                .get("posicoes")
                                                .get(0)
                                                .get("enderecoId")
                                                .longValue(),
                                gestor),
                        200);
        var sep = endereco("RET-SEP", "SEPARACAO");
        avancar("2026-09-02T12:00:00Z");
        var p = separar(reservar("60"), u, sep);
        p =
                resposta(post(saida(p) + "/documentos", documento(p, nota), supervisor), 200)
                        .get("expedicao")
                        .get("pedido");
        avancar("2026-09-05T12:00:00Z");
        var comando = retirada(p, List.of(destinacao(p, u, origem)));
        var l =
                resposta(
                        post(
                                "/api/v1/contingencias",
                                linhaTemporal(
                                        "RET-TEMPORAL",
                                        "RETIRADA",
                                        "2026-09-03T12:00:00Z",
                                        false,
                                        Map.of(
                                                "pedidoId",
                                                p.get("id").longValue(),
                                                "dados",
                                                comando)),
                                supervisor),
                        200);
        assertThat(saldo().get("fisicoTotal").decimalValue()).isEqualByComparingTo("100");
        assertThat(saldo().get("reservado").decimalValue()).isEqualByComparingTo("60");
        var d = conciliacao(l);
        var r = resposta(post(contingencia(l), d, supervisor), 200);
        assertThat(r.get("situacao").asString()).isEqualTo("CONCILIADA");
        assertThat(saldo().get("fisicoTotal").decimalValue()).isEqualByComparingTo("40");
        assertThat(saldo().get("reservado").decimalValue()).isZero();
        assertThat(atual(u).get("codigo").asString()).isEqualTo(u.get("codigo").asString());
        assertThat(
                        jdbc.queryForObject(
                                        "select retirada_em from wms.retirada_saida",
                                        java.time.LocalDateTime.class)
                                .toInstant(java.time.ZoneOffset.UTC))
                .isEqualTo(Instant.parse("2026-09-03T12:00:00Z"));
        assertThat(resposta(post(contingencia(l), d, supervisor), 200)).isEqualTo(r);
        assertThat(jdbc.queryForObject("select count(*) from wms.retirada_saida", Integer.class))
                .isEqualTo(1);
        assertThat(jdbc.queryForObject("select count(*) from wms.entrada_conferida", Integer.class))
                .isEqualTo(1);
        u = atual(u);
        var contagem = contar(u, "70");
        var excesso =
                resposta(
                        post(
                                "/api/v1/contagens/" + contagem.get("id").longValue() + "/aplicar",
                                aplicar(contagem, u),
                                supervisor),
                        409);
        assertThat(excesso.get("codigo").asString()).isEqualTo("RECOMPOSICAO_PENDENTE");
        assertThat(atual(u).get("quantidade").decimalValue()).isEqualByComparingTo("40");
        assertThat(
                        jdbc.queryForObject(
                                "select sum(quantidade) from wms.baixa_saida", BigDecimal.class))
                .isEqualByComparingTo("60");
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(
            strings = {"AJUSTE", "REMANEJAMENTO", "RETIRADA"})
    void vincularEfeitoTemporalExistenteConfereIdentidadeHashESemReexecutar(String tipo)
            throws Exception {
        var u = unidadeArmazenada("100", "VINC");
        var origem =
                resposta(
                        get(
                                "/api/v1/enderecos/"
                                        + estoque(u)
                                                .get("posicoes")
                                                .get(0)
                                                .get("enderecoId")
                                                .longValue(),
                                gestor),
                        200);
        var destino = endereco("VINC-B", "ARMAZENAGEM");
        var sep = endereco("VINC-SEP", "SEPARACAO");
        avancar("2026-09-02T12:00:00Z");
        JsonNode c = null, p = null;
        Map<String, Object> dados;
        var raiz = new HashMap<String, Object>();
        if (tipo.equals("AJUSTE")) {
            c = contar(u, "70");
            dados = aplicar(c, u);
            raiz.put("contagemId", c.get("id").longValue());
        } else if (tipo.equals("REMANEJAMENTO")) {
            dados = posicionamento(u, destino);
            raiz.put("codigoUnidade", u.get("codigo").asString());
        } else {
            p = separar(reservar("60"), u, sep);
            long nota =
                    jdbc.queryForObject("select nota_id from wms.item_nota_entrada", Long.class);
            p =
                    resposta(post(saida(p) + "/documentos", documento(p, nota), supervisor), 200)
                            .get("expedicao")
                            .get("pedido");
            dados = retirada(p, List.of(destinacao(p, u, origem)));
            raiz.put("pedidoId", p.get("id").longValue());
        }
        String identidade = "ORIGINAL-" + tipo;
        Instant real = Instant.parse("2026-09-03T12:00:00Z");
        avancar("2026-09-05T12:00:00Z");
        Object resultado;
        org.springframework.security.core.context.SecurityContextHolder.getContext()
                .setAuthentication(
                        new org.springframework.security.oauth2.server.resource.authentication
                                .JwtAuthenticationToken(
                                decoder.decode(supervisor),
                                List.of(
                                        new org.springframework.security.core.authority
                                                .SimpleGrantedAuthority("ROLE_SUPERVISOR"))));
        try {
            if (tipo.equals("AJUSTE"))
                resultado =
                        contagens.aplicarContingencia(
                                c.get("id").longValue(),
                                mapper.convertValue(
                                        dados, br.com.rodogarcia.wms.dto.ContagemDto.Aplicar.class),
                                real,
                                identidade);
            else if (tipo.equals("REMANEJAMENTO"))
                resultado =
                        movimentos.posicionarContingencia(
                                UUID.fromString(u.get("codigo").asString()),
                                mapper.convertValue(
                                        dados,
                                        br.com.rodogarcia.wms.dto.EstoqueDto.Posicionar.class),
                                real,
                                identidade);
            else
                resultado =
                        expedicao.retirarContingencia(
                                p.get("id").longValue(),
                                mapper.convertValue(
                                        dados,
                                        br.com.rodogarcia.wms.dto.ExpedicaoDto.Retirar.class),
                                real,
                                identidade);
        } finally {
            org.springframework.security.core.context.SecurityContextHolder.clearContext();
        }
        String op = dados.get("operacaoId").toString();
        String tabela =
                tipo.equals("AJUSTE")
                        ? "operacao_administrativa"
                        : tipo.equals("REMANEJAMENTO") ? "movimento_estoque" : "operacao_saida";
        String hash =
                jdbc.queryForObject(
                        "select conteudo_hash from wms." + tabela + " where operacao_id=?",
                        String.class,
                        op);
        int quantidadeMovimentos =
                jdbc.queryForObject("select count(*) from wms.movimento_estoque", Integer.class);
        BigDecimal fisico = saldo().get("fisicoTotal").decimalValue();
        raiz.put("dados", dados);
        raiz.put("identidadeExecucao", "IDENTIDADE-DIVERGENTE");
        var errada =
                resposta(
                        post(
                                "/api/v1/contingencias",
                                linhaTemporal(
                                        "VINC-ERRADO-" + tipo, tipo, real.toString(), true, raiz),
                                supervisor),
                        200);
        var prova = Map.of("operacaoOriginal", op, "conteudoHash", hash);
        var d = conciliacao(errada);
        d.put("modo", "VINCULAR");
        d.put("prova", prova);
        if (tipo.equals("AJUSTE")) resposta(post(contingencia(errada), d, supervisor), 409);
        else {
            var pendente = resposta(post(contingencia(errada), d, supervisor), 200);
            assertThat(pendente.get("situacao").asString()).isEqualTo("PENDENTE");
        }
        raiz.put("identidadeExecucao", identidade);
        var correta =
                resposta(
                        post(
                                "/api/v1/contingencias",
                                linhaTemporal(
                                        "VINC-CORRETO-" + tipo, tipo, real.toString(), true, raiz),
                                supervisor),
                        200);
        d = conciliacao(correta);
        d.put("modo", "VINCULAR");
        d.put("prova", prova);
        var r = resposta(post(contingencia(correta), d, supervisor), 200);
        assertThat(r.get("situacao").asString()).isEqualTo("CONCILIADA");
        assertThat(r.get("resultado").get("efeito"))
                .isEqualTo(mapper.readTree(mapper.writeValueAsString(resultado)));
        assertThat(resposta(post(contingencia(correta), d, supervisor), 200)).isEqualTo(r);
        resposta(post(contingencia(correta), d, operador), 403);
        assertThat(saldo().get("fisicoTotal").decimalValue()).isEqualByComparingTo(fisico);
        assertThat(jdbc.queryForObject("select count(*) from wms.movimento_estoque", Integer.class))
                .isEqualTo(quantidadeMovimentos);
    }

    @Test
    void recomposicaoPropriaOrigemConservaNotaHistoricoBloqueioENaoExcedeConferido()
            throws Exception {
        var u = unidadeArmazenada("100", "RECOMP");
        avancar("2026-09-02T12:00:00Z");
        var c = contar(u, "70");
        var negativo = aplicar(c, u);
        var original =
                resposta(
                        post(
                                "/api/v1/contagens/" + c.get("id").longValue() + "/aplicar",
                                negativo,
                                supervisor),
                        200);
        u = atual(u);
        resposta(
                post(
                        rota(u, "/bloqueio"),
                        Map.of(
                                "operacaoId",
                                UUID.randomUUID(),
                                "versaoUnidade",
                                u.get("versao").longValue(),
                                "motivo",
                                "Bloqueio independente preservado"),
                        supervisor),
                200);
        u = atual(u);
        avancar("2026-09-03T12:00:00Z");
        var nova = contar(u, "100");
        var positivo = aplicar(nova, u);
        var aplicado =
                resposta(
                        post(
                                "/api/v1/contagens/" + c.get("id").longValue() + "/aplicar",
                                positivo,
                                supervisor),
                        200);
        assertThat(aplicado.get("situacao").asString()).isEqualTo("APLICADA");
        assertThat(atual(u).get("quantidade").decimalValue()).isEqualByComparingTo("100");
        assertThat(estoque(u).get("bloqueada").booleanValue()).isTrue();
        assertThat(saldo().get("disponivel").decimalValue()).isZero();
        assertThat(
                        jdbc.queryForObject(
                                "select sum(quantidade) from wms.conteudo_unidade",
                                BigDecimal.class))
                .isEqualByComparingTo("100");
        assertThat(
                        jdbc.queryForObject(
                                "select quantidade_prevista from wms.item_nota_entrada",
                                BigDecimal.class))
                .isEqualByComparingTo("100");
        assertThat(
                        resposta(
                                post(
                                        "/api/v1/contagens/" + c.get("id").longValue() + "/aplicar",
                                        negativo,
                                        supervisor),
                                200))
                .isEqualTo(original);
        assertThat(
                        resposta(
                                post(
                                        "/api/v1/contagens/" + c.get("id").longValue() + "/aplicar",
                                        positivo,
                                        supervisor),
                                200))
                .isEqualTo(aplicado);
        u = atual(u);
        avancar("2026-09-04T12:00:00Z");
        var excesso = contar(u, "101");
        var erro =
                resposta(
                        post(
                                "/api/v1/contagens/" + c.get("id").longValue() + "/aplicar",
                                aplicar(excesso, u),
                                supervisor),
                        409);
        assertThat(erro.get("codigo").asString()).isEqualTo("RECOMPOSICAO_PENDENTE");
        assertThat(atual(u).get("quantidade").decimalValue()).isEqualByComparingTo("100");
        assertThat(
                        jdbc.queryForObject(
                                "select count(*) from wms.movimento_estoque where acao='AJUSTE_ESTOQUE'",
                                Integer.class))
                .isEqualTo(2);
        var revisoes =
                resposta(
                        get("/api/v1/contagens/" + c.get("id").longValue() + "/revisoes", operador),
                        200);
        assertThat(revisoes.get("itens").size()).isEqualTo(3);
        assertThat(revisoes.get("itens").get(0).get("efeitoJson").asString())
                .contains("2026-09-02T12:00:00Z");
    }

    @Test
    void preparadaSoSaiPelaResolucaoIntegralECancelamentoAuditadoPreservaFK() throws Exception {
        var c = carga();
        c =
                resposta(
                        post(
                                "/api/v1/cargas-iniciais/" + c.get("id").longValue() + "/preparar",
                                conferencia(c, List.of()),
                                supervisor),
                        200);
        long cargaId = c.get("id").longValue(), entradaId = c.get("entradaId").longValue();
        var selecionadas = new java.util.ArrayList<Map<String, Object>>();
        var unidades = new java.util.ArrayList<JsonNode>();
        var etiquetas = new java.util.ArrayList<String>();
        for (var e : c.get("etiquetas")) {
            var u = resposta(get(UNIDADES + e.asString(), operador), 200).get("unidade");
            posicionar(u, endereco("CARGA-" + unidades.size(), "ARMAZENAGEM"));
            u = atual(u);
            selecionadas.add(
                    Map.of(
                            "unidadeId",
                            u.get("id").longValue(),
                            "versao",
                            u.get("versao").longValue(),
                            "quantidade",
                            u.get("quantidade").decimalValue()));
            unidades.add(u);
            etiquetas.add(e.asString());
        }
        assertThat(saldo().get("fisicoTotal").decimalValue()).isEqualByComparingTo("20");
        assertThat(saldo().get("disponivel").decimalValue()).isZero();
        var rr = comando();
        rr.put("clienteId", clienteId);
        rr.put("armazemId", armazemId);
        rr.put("referencia", "RESOLUCAO-CARGA");
        rr.put("cargaInicialId", cargaId);
        rr.put("unidades", selecionadas);
        rr.put("etiquetas", etiquetas);
        avancar("2026-09-03T12:00:00Z");
        resposta(post("/api/v1/encerramentos/remanescente", rr, supervisor), 403);
        var criado = resposta(post("/api/v1/encerramentos/remanescente", rr, gestor), 200);
        assertThat(resposta(post("/api/v1/encerramentos/remanescente", rr, gestor), 200))
                .isEqualTo(criado);
        var p = criado.get("pedido");
        assertThat(p.get("reservas").size()).isEqualTo(2);
        var resolver = comando();
        resolver.put("versao", c.get("versao").longValue());
        resolver.put("pedidoResolucaoId", p.get("id").longValue());
        resolver.put("contagensIds", List.of());
        String cancelamento = "/api/v1/cargas-iniciais/" + cargaId + "/resolver-cancelamento";
        resposta(post(cancelamento, resolver, gestor), 409);
        for (var u : unidades) {
            var sep = endereco("CARGA-SEP-" + u.get("id").longValue(), "SEPARACAO");
            var leitura = comandoPedido(p);
            leitura.put("resolverPendentes", true);
            leitura.put("reservaId", reserva(p, u));
            leitura.put("codigoLido", u.get("codigo").asString());
            leitura.put(
                    "revisaoConteudo",
                    resposta(get(rota(u, "/etiqueta"), operador), 200)
                            .get("versaoConteudo")
                            .longValue());
            p =
                    resposta(post(saida(p) + "/leituras", leitura, gestor), 200)
                            .get("expedicao")
                            .get("pedido");
            var separar = comandoPedido(p);
            separar.put("resolverPendentes", true);
            separar.put("destinacao", destinacao(p, u, sep));
            p =
                    resposta(post(saida(p) + "/separacoes", separar, gestor), 200)
                            .get("expedicao")
                            .get("pedido");
        }
        long nota = jdbc.queryForObject("select nota_id from wms.item_nota_entrada", Long.class);
        String xmlCarga = xml().replace("<qCom>60</qCom>", "<qCom>20</qCom>");
        var doc = documento(p, nota);
        doc.put("resolverPendentes", true);
        doc.put("xml", xmlCarga);
        p =
                resposta(post(saida(p) + "/documentos", doc, gestor), 200)
                        .get("expedicao")
                        .get("pedido");
        var tirar = retirada(p, List.of());
        tirar.put("resolverPendentes", true);
        tirar.put("xmls", List.of(xmlCarga));
        var r = resposta(post(saida(p) + "/retirada", tirar, gestor), 200);
        resposta(post(saida(p) + "/retirada", tirar, supervisor), 403);
        assertThat(r.get("expedicao").get("pedido").get("situacao").asString())
                .isEqualTo("RETIRADO");
        assertThat(saldo().get("fisicoTotal").decimalValue()).isZero();
        br.com.rodogarcia.wms.services.AuditoriaService alvo =
                org.springframework.test.util.AopTestUtils.getUltimateTargetObject(auditoria);
        org.mockito.Mockito.doAnswer(
                        inv -> {
                            if ("CANCELAMENTO_CARGA".equals(inv.getArgument(2)))
                                throw new IllegalStateException("Falha de auditoria ficticia");
                            return inv.callRealMethod();
                        })
                .when(alvo)
                .registrar(
                        org.mockito.ArgumentMatchers.anyString(),
                        org.mockito.ArgumentMatchers.anyLong(),
                        org.mockito.ArgumentMatchers.anyString(),
                        org.mockito.ArgumentMatchers.anyString(),
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any());
        resposta(post(cancelamento, resolver, gestor), 500);
        assertThat(
                        resposta(get("/api/v1/cargas-iniciais/" + cargaId, supervisor), 200)
                                .get("situacao")
                                .asString())
                .isEqualTo("PREPARADA");
        assertThat(
                        jdbc.queryForObject(
                                "select entrada_id from wms.carga_inicial where id=?",
                                Long.class,
                                cargaId))
                .isEqualTo(entradaId);
        assertThat(jdbc.queryForObject("select count(*) from wms.retirada_saida", Integer.class))
                .isEqualTo(1);
        org.mockito.Mockito.reset(alvo);
        var cancelada = resposta(post(cancelamento, resolver, gestor), 200);
        assertThat(cancelada.get("situacao").asString()).isEqualTo("CANCELADA");
        assertThat(cancelada.get("entradaId").longValue()).isEqualTo(entradaId);
        assertThat(resposta(post(cancelamento, resolver, gestor), 200)).isEqualTo(cancelada);
        resposta(post(cancelamento, resolver, supervisor), 403);
        assertThat(jdbc.queryForObject("select count(*) from wms.entrada_conferida", Integer.class))
                .isEqualTo(1);
        assertThat(
                        jdbc.queryForObject(
                                "select carga_id from wms.resolucao_remanescente", Long.class))
                .isEqualTo(cargaId);
    }

    @Test
    void validadeConfiguradaSinalizaSemBloqueioEValorDesconhecidoNaoViraZero() throws Exception {
        produto =
                produtos.saveAndFlush(
                        new Produto(
                                cliente,
                                "VALIDADE",
                                "Produto ficticio com validade",
                                "UN",
                                TipoQuantidade.CONTAGEM,
                                0,
                                false,
                                true,
                                null,
                                clock.instant()));
        embalagem =
                embalagens.saveAndFlush(
                        new Embalagem(
                                produto,
                                "DUN-VALIDADE",
                                "Embalagem ficticia",
                                BigDecimal.TEN,
                                clock.instant()));
        long pe = iniciarEntrada("VALIDADE", "10", null);
        long item =
                resposta(get(PEDIDOS + pe, operador), 200)
                        .get("notas")
                        .get(0)
                        .get("itens")
                        .get(0)
                        .get("id")
                        .longValue();
        resposta(
                post(
                        PEDIDOS + pe + "/chegadas",
                        Map.of(
                                "operacaoId",
                                UUID.randomUUID(),
                                "versao",
                                versao(pe),
                                "chegouEm",
                                clock.instant().toString(),
                                "observacao",
                                "Conferencia fisica com validade fornecida",
                                "itens",
                                List.of(
                                        Map.of(
                                                "itemNotaId",
                                                item,
                                                "quantidadeBoa",
                                                "10",
                                                "quantidadeAvariada",
                                                "0",
                                                "validade",
                                                "2026-09-04"))),
                        operador),
                200);
        efetivar(pe);
        long entrada =
                resposta(get(PEDIDOS + pe + "/entradas", operador), 200)
                        .get("itens")
                        .get(0)
                        .get("id")
                        .longValue();
        var u = unitizarEntrada(pe, entrada, "10");
        posicionar(u, endereco("VALIDADE-A", "ARMAZENAGEM"));
        String ctx = "?clienteId=" + clienteId + "&armazemId=" + armazemId;
        var ausente = resposta(get("/api/v1/avisos-validade" + ctx, operador), 200);
        assertThat(ausente.get("configurada").booleanValue()).isFalse();
        assertThat(ausente.get("diasAntecedencia").isNull()).isTrue();
        String indicadores =
                "/api/v1/indicadores-estoque" + ctx + "&produtoId=" + produto.getId() + "&fuso=UTC";
        assertThat(
                        resposta(get(indicadores, operador), 200)
                                .get("itens")
                                .get(0)
                                .get("avisos")
                                .isEmpty())
                .isTrue();
        var d = comando();
        d.put("clienteId", clienteId);
        d.put("armazemId", armazemId);
        d.put("versao", 0);
        d.put("diasAntecedencia", 3);
        resposta(enviar("PUT", "/api/v1/avisos-validade", d, supervisor), 403);
        var cfg = resposta(enviar("PUT", "/api/v1/avisos-validade", d, gestor), 200);
        assertThat(resposta(enviar("PUT", "/api/v1/avisos-validade", d, gestor), 200))
                .isEqualTo(cfg);
        var atual = resposta(get(indicadores, operador), 200).get("itens").get(0);
        assertThat(atual.get("avisos").get(0).get("situacao").asString()).isEqualTo("PROXIMA");
        avancar("2026-09-05T12:00:00Z");
        atual = resposta(get(indicadores, operador), 200).get("itens").get(0);
        assertThat(atual.get("avisos").get(0).get("situacao").asString()).isEqualTo("VENCIDA");
        assertThat(atual.get("saldo").get("disponivel").decimalValue()).isEqualByComparingTo("10");
        resposta(get(indicadores + "&valor=true", operador), 403);
        var valor = resposta(get(indicadores + "&valor=true", supervisor), 200).get("itens").get(0);
        assertThat(valor.get("valorConsultado").booleanValue()).isTrue();
        assertThat(valor.get("valorExato").isNull()).isTrue();
        assertThat(valor.get("pendencias").toString()).contains("VALOR_NOTA_AUSENTE");
        assertThat(saldo().get("disponivel").decimalValue()).isEqualByComparingTo("10");
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(booleans = {true, false})
    void encerramentoENovoCompromissoCompartilhamLockSemApagarHistoria(boolean pedidoPrimeiro)
            throws Exception {
        var chegouNaAuditoria = new java.util.concurrent.CountDownLatch(1);
        var liberarAuditoria = new java.util.concurrent.CountDownLatch(1);
        br.com.rodogarcia.wms.services.AuditoriaService alvo =
                org.springframework.test.util.AopTestUtils.getUltimateTargetObject(auditoria);
        org.mockito.Mockito.doAnswer(
                        i -> {
                            boolean primeiro =
                                    pedidoPrimeiro
                                            ? "PEDIDO_ENTRADA".equals(i.getArgument(0))
                                                    && "CRIACAO".equals(i.getArgument(2))
                                            : "CLIENTE".equals(i.getArgument(0))
                                                    && "SOLICITACAO_ENCERRAMENTO"
                                                            .equals(i.getArgument(2));
                            if (primeiro) {
                                chegouNaAuditoria.countDown();
                                if (!liberarAuditoria.await(
                                        5, java.util.concurrent.TimeUnit.SECONDS))
                                    throw new IllegalStateException(
                                            "Segundo comando não observado no teste");
                            }
                            return i.callRealMethod();
                        })
                .when(alvo)
                .registrar(
                        org.mockito.ArgumentMatchers.anyString(),
                        org.mockito.ArgumentMatchers.anyLong(),
                        org.mockito.ArgumentMatchers.anyString(),
                        org.mockito.ArgumentMatchers.anyString(),
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any());
        String enc = "/api/v1/encerramentos/CLIENTE/" + clienteId;
        var fechar = comando();
        fechar.put("versao", cliente.getVersao());
        var criar =
                Map.of(
                        "clienteId",
                        clienteId,
                        "armazemId",
                        armazemId,
                        "referencia",
                        "CORRIDA-ENCERRAMENTO");
        try (var executor = java.util.concurrent.Executors.newFixedThreadPool(2)) {
            var primeiro =
                    executor.submit(
                            () ->
                                    pedidoPrimeiro
                                            ? post("/api/v1/pedidos-entrada", criar, operador)
                                            : post(enc + "/solicitar", fechar, gestor));
            assertThat(chegouNaAuditoria.await(5, java.util.concurrent.TimeUnit.SECONDS)).isTrue();
            var iniciouSegundo = new java.util.concurrent.CountDownLatch(1);
            var segundo =
                    executor.submit(
                            () -> {
                                iniciouSegundo.countDown();
                                return pedidoPrimeiro
                                        ? post(enc + "/solicitar", fechar, gestor)
                                        : post("/api/v1/pedidos-entrada", criar, operador);
                            });
            assertThat(iniciouSegundo.await(5, java.util.concurrent.TimeUnit.SECONDS)).isTrue();
            org.assertj.core.api.Assertions.assertThatThrownBy(
                            () -> segundo.get(200, java.util.concurrent.TimeUnit.MILLISECONDS))
                    .isInstanceOf(java.util.concurrent.TimeoutException.class);
            liberarAuditoria.countDown();
            var p = primeiro.get(5, java.util.concurrent.TimeUnit.SECONDS);
            var s = segundo.get(5, java.util.concurrent.TimeUnit.SECONDS);
            var encerrado = resposta(pedidoPrimeiro ? s : p, 200);
            assertThat(encerrado.get("situacao").asString()).isEqualTo("ENCERRAMENTO_PENDENTE");
            resposta(pedidoPrimeiro ? p : s, pedidoPrimeiro ? 201 : 409);
            assertThat(
                            jdbc.queryForObject(
                                    "select count(*) from wms.pedido_entrada", Integer.class))
                    .isEqualTo(pedidoPrimeiro ? 1 : 0);
            var inativar = comando();
            inativar.put("versao", encerrado.get("versao").longValue());
            var r = resposta(post(enc + "/inativar", inativar, gestor), pedidoPrimeiro ? 409 : 200);
            assertThat(r.get(pedidoPrimeiro ? "codigo" : "situacao").asString())
                    .isEqualTo(pedidoPrimeiro ? "ENCERRAMENTO_IMPEDIDO" : "INATIVO");
            assertThat(
                            jdbc.queryForObject(
                                    "select count(*) from wms.pedido_entrada", Integer.class))
                    .isEqualTo(pedidoPrimeiro ? 1 : 0);
        } finally {
            liberarAuditoria.countDown();
        }
    }

    private JsonNode unidadeArmazenada(String q, String referencia) throws Exception {
        long pe = iniciarEntrada(referencia, q, "1000");
        chegada(pe, q, clock.instant().toString());
        efetivar(pe);
        long entrada =
                resposta(get(PEDIDOS + pe + "/entradas", operador), 200)
                        .get("itens")
                        .get(0)
                        .get("id")
                        .longValue();
        var u = unitizarEntrada(pe, entrada, q);
        posicionar(u, endereco(referencia + "-A", "ARMAZENAGEM"));
        return atual(u);
    }

    private Map<String, Object> linhaTemporal(
            String identidade,
            String tipo,
            String data,
            boolean registrado,
            Map<String, Object> dados) {
        var d = comando();
        d.put("identidadeFato", identidade);
        d.put("clienteId", clienteId);
        d.put("armazemId", armazemId);
        d.put("tipo", tipo);
        d.put("ocorridaEm", data);
        d.put("operador", "Operador ficticio");
        d.put("fonte", "Prova temporal ficticia identificada");
        d.put("efeitoRegistradoNoWms", registrado);
        d.put("dependencias", List.of());
        d.put("dados", dados);
        return d;
    }

    private Map<String, Object> conciliacao(JsonNode l) {
        var d = comando();
        d.put("versao", l.get("versao").longValue());
        d.put("modo", "EXECUTAR");
        return d;
    }

    private String contingencia(JsonNode l) {
        return "/api/v1/contingencias/" + l.get("id").longValue() + "/conciliar";
    }

    private void assertPico(JsonNode calc, String data, String esperado) {
        for (var d : calc.get("memoria").get("diarias"))
            if (d.get("data").asString().equals(data)) {
                assertThat(d.get("picoCobravel").decimalValue()).isEqualByComparingTo(esperado);
                return;
            }
        throw new AssertionError("Dia ausente: " + data);
    }

    private void avancar(String data) {
        instante.set(Instant.parse(data));
    }

    private long iniciarEntrada(String referencia, String qtd, String valor) throws Exception {
        long pe =
                resposta(
                                post(
                                        "/api/v1/pedidos-entrada",
                                        Map.of(
                                                "clienteId",
                                                clienteId,
                                                "armazemId",
                                                armazemId,
                                                "referencia",
                                                referencia),
                                        operador),
                                201)
                        .get("id")
                        .longValue();
        var i =
                new HashMap<String, Object>(
                        Map.of(
                                "numeroItem",
                                1,
                                "produtoId",
                                produto.getId(),
                                "quantidadePrevista",
                                qtd));
        if (valor != null) i.put("valorMercadoria", valor);
        resposta(
                post(
                        PEDIDOS + pe + "/notas",
                        Map.of(
                                "versao",
                                versao(pe),
                                "serie",
                                1,
                                "numero",
                                ++numero,
                                "emissao",
                                "2026-09-01",
                                "itens",
                                List.of(i)),
                        operador),
                200);
        resposta(
                post(
                        PEDIDOS + pe + "/iniciar-conferencia",
                        Map.of("versao", versao(pe), "motivo", "Conferencia fisica iniciada"),
                        operador),
                200);
        return pe;
    }

    private void chegada(long pe, String quantidade, String data) throws Exception {
        long i =
                resposta(get(PEDIDOS + pe, operador), 200)
                        .get("notas")
                        .get(0)
                        .get("itens")
                        .get(0)
                        .get("id")
                        .longValue();
        resposta(
                post(
                        PEDIDOS + pe + "/chegadas",
                        Map.of(
                                "versao",
                                versao(pe),
                                "operacaoId",
                                UUID.randomUUID(),
                                "chegouEm",
                                data,
                                "observacao",
                                "Chegada real ficticia identificada",
                                "itens",
                                List.of(
                                        Map.of(
                                                "itemNotaId",
                                                i,
                                                "quantidadeBoa",
                                                quantidade,
                                                "quantidadeAvariada",
                                                "0"))),
                        operador),
                200);
    }

    private void efetivar(long pe) throws Exception {
        resposta(
                post(
                        PEDIDOS + pe + "/efetivacao",
                        Map.of(
                                "versao",
                                versao(pe),
                                "motivo",
                                "Conferencia integral aprovada",
                                "aceitarDivergencias",
                                false),
                        supervisor),
                200);
    }

    private JsonNode unitizarEntrada(long pe, long entrada, String quantidade) throws Exception {
        return resposta(
                        post(
                                PEDIDOS + pe + "/entradas/" + entrada + "/unitizacao",
                                Map.of(
                                        "operacaoId",
                                        UUID.randomUUID(),
                                        "versaoPedido",
                                        versao(pe),
                                        "motivo",
                                        "Unitizacao fisica comprovada",
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
                                                        quantidade))),
                                operador),
                        200)
                .get("unidades")
                .get(0)
                .get("unidade");
    }

    private JsonNode reservar(String q) throws Exception {
        var c = comando();
        c.put("clienteId", clienteId);
        c.put("armazemId", armazemId);
        c.put("referencia", "SAIDA-JORNADA");
        c.put("itens", List.of(Map.of("produtoId", produto.getId(), "quantidade", q)));
        var p = resposta(post("/api/v1/pedidos-saida", c, operador), 201).get("pedido");
        return resposta(post(saida(p) + "/reserva", comandoPedido(p), operador), 200).get("pedido");
    }

    private String saida(JsonNode p) {
        return "/api/v1/pedidos-saida/" + p.get("id").longValue();
    }

    private Map<String, Object> comandoPedido(JsonNode p) {
        var c = comando();
        c.put("versao", p.get("versao").longValue());
        return c;
    }

    private long reserva(JsonNode p, JsonNode u) {
        for (var r : p.get("reservas"))
            if (r.get("unidadeId").longValue() == u.get("id").longValue())
                return r.get("id").longValue();
        throw new AssertionError("Reserva ausente");
    }

    private Map<String, Object> destinacao(JsonNode p, JsonNode u, JsonNode endereco) {
        return Map.of(
                "reservaId",
                reserva(p, u),
                "destinos",
                List.of(
                        Map.of(
                                "enderecoId",
                                endereco.get("id").longValue(),
                                "codigoLido",
                                endereco.get("codigo").asString())));
    }

    private JsonNode separar(JsonNode p, JsonNode u, JsonNode destino) throws Exception {
        var d = comandoPedido(p);
        d.put("reservaId", reserva(p, u));
        d.put("codigoLido", u.get("codigo").asString());
        d.put(
                "revisaoConteudo",
                resposta(get(rota(u, "/etiqueta"), operador), 200)
                        .get("versaoConteudo")
                        .longValue());
        p = resposta(post(saida(p) + "/leituras", d, operador), 200).get("expedicao").get("pedido");
        d = comandoPedido(p);
        d.put("destinacao", destinacao(p, u, destino));
        return resposta(post(saida(p) + "/separacoes", d, operador), 200)
                .get("expedicao")
                .get("pedido");
    }

    private Map<String, Object> documento(JsonNode p, long nota) {
        var d = comandoPedido(p);
        d.put("origem", "XML");
        d.put("natureza", "RETORNO_MERCADORIA");
        d.put("xml", xml());
        d.put("protocolo", "Documento ficticio fornecido");
        var cob = new java.util.ArrayList<Map<String, Object>>();
        for (var r : p.get("reservas"))
            cob.add(
                    Map.of(
                            "reservaId",
                            r.get("id").longValue(),
                            "notaOrigemId",
                            nota,
                            "sku",
                            "SKU",
                            "quantidade",
                            r.get("quantidade").decimalValue()));
        d.put("coberturas", cob);
        return d;
    }

    private Map<String, Object> retirada(JsonNode p, List<Map<String, Object>> rem) {
        var d = comandoPedido(p);
        d.put("xmls", List.of(xml()));
        d.put("remanescentes", rem);
        return d;
    }

    private String xml() {
        return "<NFe xmlns=\"http://www.portalfiscal.inf.br/nfe\"><infNFe Id=\"NFe"
                + "1".repeat(44)
                + "\" versao=\"4.00\"><ide><mod>55</mod><serie>1</serie><nNF>900</nNF><dhEmi>2026-09-02T12:00:00Z</dhEmi></ide><emit><CNPJ>98765432000188</CNPJ></emit><det nItem=\"1\"><prod><cProd>SKU</cProd><uCom>UN</uCom><qCom>60</qCom><vProd>600.00</vProd></prod></det></infNFe></NFe>";
    }

    private String rota(JsonNode f) {
        return "/api/v1/fechamentos-cobranca/" + f.get("fechamento").get("id").longValue();
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
                                clock.instant()));
        embalagem =
                embalagens.saveAndFlush(
                        new Embalagem(
                                produto,
                                "DUN-10",
                                "Embalagem ficticia",
                                new BigDecimal("10"),
                                clock.instant()));
    }

    private long versao(long pedido) throws Exception {
        return resposta(get(PEDIDOS + pedido, operador), 200)
                .get("pedido")
                .get("versao")
                .longValue();
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
                                clock.instant().toString(),
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

    private String rota(JsonNode u, String sufixo) {
        return UNIDADES + u.get("codigo").asString() + sufixo;
    }

    private JsonNode estoque(JsonNode u) throws Exception {
        return resposta(get(rota(u, "/estoque"), operador), 200);
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

    private HashMap<String, Object> comando() {
        return new HashMap<>(
                Map.of("operacaoId", UUID.randomUUID(), "motivo", "Operacao ficticia validada"));
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

    private Map<String, Object> prepararComando(JsonNode calc) {
        var d = comando();
        d.put("calculoId", calc.get("id").longValue());
        return d;
    }

    private JsonNode preparar(JsonNode calc) throws Exception {
        return resposta(post("/api/v1/fechamentos-cobranca", prepararComando(calc), gestor), 200);
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

    private Map<String, Object> entregaComando(JsonNode f) {
        var d = decisao(f);
        d.put("layoutVersao", 1);
        d.put("arquivoHash", f.get("versao").get("conteudoHash").asString());
        d.put("destinoReferencia", "Entrega manual ficticia ESL");
        d.put("entregueEm", "2026-10-01T12:00:00Z");
        return d;
    }

    private JsonNode entregar(JsonNode f) throws Exception {
        return resposta(post(rota(f) + "/entregas", entregaComando(f), gestor), 200);
    }

    private JsonNode nfseNumero(JsonNode f, int numero, String referencia) throws Exception {
        var d = decisaoNumero(f, numero);
        d.put("emissorDocumento", "99999999000199");
        d.put("referenciaExterna", referencia);
        d.put("emitidaEm", "2026-10-01T12:00:00Z");
        d.put("fonte", "Documento externo existente ficticio");
        d.put("conferidaPor", "Natalina ficticia");
        return resposta(post(rota(f) + "/referencias-nfse", d, gestor), 200);
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
}
