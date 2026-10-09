package br.com.rodogarcia.wms;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import br.com.rodogarcia.wms.config.IdentidadeTesteConfig;
import br.com.rodogarcia.wms.models.Armazem;
import br.com.rodogarcia.wms.models.ChegadaRecebimento;
import br.com.rodogarcia.wms.models.Cliente;
import br.com.rodogarcia.wms.models.CondicaoMercadoria;
import br.com.rodogarcia.wms.models.ConteudoUnidade;
import br.com.rodogarcia.wms.models.Embalagem;
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
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
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
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Provas locais F33/F34 e PRIO06. H2 exclusivo; nao valida corte ou aprovacao no SQL Server. */
@ActiveProfiles("test")
@TestPropertySource(
        locations = "classpath:cadastros-test.properties",
        properties = {
            "spring.datasource.url=jdbc:h2:mem:wms-d29-retomada;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000;INIT=CREATE SCHEMA IF NOT EXISTS wms",
            "spring.datasource.username=d29_retomada_local",
            "spring.datasource.password=",
            "spring.datasource.driver-class-name=org.h2.Driver",
            "spring.jpa.hibernate.ddl-auto=create-drop",
            "spring.flyway.enabled=false"
        })
@Import(IdentidadeTesteConfig.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class D29FinanceiroRetomadaTest {
    @Autowired Environment environment;
    @Autowired JsonMapper mapper;
    @Autowired JwtEncoder encoder;
    @Autowired JdbcTemplate jdbc;
    @Autowired PlatformTransactionManager transactions;
    @MockitoBean Clock clock;
    @PersistenceContext EntityManager em;
    private final HttpClient http =
            HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private static final Instant BASE = Instant.parse("2026-08-01T12:00:00Z");
    private static final AtomicInteger FAMILIAS = new AtomicInteger(7000);
    private Long clienteId, armazemId, produtoId, embalagemId, servicoId;
    private int numero;
    private String gestor, supervisor;

    private record Unidade(Long id, Long pedidoId, Long notaId, String codigo) {}

    private record SnapshotContrato(
            List<Map<String, Object>> contratos,
            List<Map<String, Object>> servicosMinimo,
            List<Map<String, Object>> operacoes,
            List<Map<String, Object>> auditorias) {}

    @BeforeEach
    void prepararFixtureLocal() throws Exception {
        try (var conexao = jdbc.getDataSource().getConnection()) {
            assertThat(conexao.getMetaData().getURL()).startsWith("jdbc:h2:mem:wms-d29-retomada");
            assertThat(conexao.getMetaData().getDatabaseProductName()).isEqualTo("H2");
        }
        when(clock.instant()).thenReturn(Instant.parse("2028-06-06T12:00:00Z"));
        when(clock.getZone()).thenReturn(java.time.ZoneOffset.UTC);
        when(clock.withZone(any(java.time.ZoneId.class)))
                .thenAnswer(inv -> Clock.fixed(clock.instant(), inv.getArgument(0)));
        SecurityContextHolder.clearContext();
        numero = FAMILIAS.addAndGet(100);
        new TransactionTemplate(transactions)
                .executeWithoutResult(
                        tx -> {
                            var c =
                                    new Cliente(
                                            "C" + numero,
                                            "Cliente ficticio",
                                            Long.toString(11111111000000L + numero),
                                            BASE);
                            var a =
                                    new Armazem(
                                            "A" + numero,
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
    }

    @Test
    void f33ReabrirV1AprovadaPreservaMemoriaBytesHashEExigeNovaAprovacaoV2() throws Exception {
        configurar("2026-09-01", "DIAS_CORRIDOS", null, 30, null);
        fato("10", "2026-09-02T12:00:00Z");
        var v1 = aprovar(preparar(calcular("2026-09-01", "2026-10-01")));
        assertThat(v1.get("versao").get("situacao").asString()).isEqualTo("APROVADA");
        assertThat(v1.get("versao").get("saldo").decimalValue()).isEqualByComparingTo("100");
        long fechamentoId = v1.get("fechamento").get("id").longValue();
        long versaoId = v1.get("versao").get("id").longValue();
        byte[] arquivoAntes =
                get(rota(v1) + "/versoes/1/demonstrativo", supervisor)
                        .body()
                        .getBytes(StandardCharsets.UTF_8);
        String memoriaAntes =
                jdbc.queryForObject(
                        "select memoria_json from wms.versao_fechamento where id=?",
                        String.class,
                        versaoId);
        String hashAntes =
                jdbc.queryForObject(
                        "select conteudo_hash from wms.versao_fechamento where id=?",
                        String.class,
                        versaoId);
        String digestIndependente =
                HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(arquivoAntes));
        assertThat(hashAntes).isEqualTo(digestIndependente);
        assertThat(memoriaAntes.getBytes(StandardCharsets.UTF_8)).isEqualTo(arquivoAntes);
        var v2 = reabrir(v1, calcular("2026-09-01", "2026-10-01"));
        assertThat(v2.get("versao").get("numero").asInt()).isEqualTo(2);
        assertThat(v2.get("versao").get("situacao").asString()).isEqualTo("PENDENTE_REVISAO");
        assertThat(v2.get("versao").get("decisor").isNull()).isTrue();
        assertThat(
                        jdbc.queryForObject(
                                "select decidida_em from wms.versao_fechamento where id=?",
                                java.sql.Timestamp.class,
                                v2.get("versao").get("id").longValue()))
                .isNull();
        assertThat(
                        jdbc.queryForObject(
                                "select situacao from wms.versao_fechamento where id=?",
                                String.class,
                                versaoId))
                .isEqualTo("SUPERADA");
        assertThat(
                        jdbc.queryForObject(
                                "select memoria_json from wms.versao_fechamento where id=?",
                                String.class,
                                versaoId))
                .isEqualTo(memoriaAntes);
        assertThat(
                        jdbc.queryForObject(
                                "select conteudo_hash from wms.versao_fechamento where id=?",
                                String.class,
                                versaoId))
                .isEqualTo(hashAntes);
        assertThat(
                        get(rota(v2) + "/versoes/1/demonstrativo", supervisor)
                                .body()
                                .getBytes(StandardCharsets.UTF_8))
                .isEqualTo(arquivoAntes);
        var recusada = resposta(post(rota(v2) + "/entregas", entregaComando(v2), gestor), 409);
        assertThat(recusada.toString()).contains("VERSAO_NAO_APROVADA");
        assertThat(
                        jdbc.queryForObject(
                                "select count(*) from wms.entrega_esl where versao_id=?",
                                Integer.class,
                                v2.get("versao").get("id").longValue()))
                .isZero();
        var aprovadaV2 = aprovar(v2);
        assertThat(aprovadaV2.get("versao").get("situacao").asString()).isEqualTo("APROVADA");
        assertThat(aprovadaV2.get("versao").get("decisor").isNull()).isFalse();
        assertThat(aprovadaV2.get("fechamento").get("id").longValue()).isEqualTo(fechamentoId);
        assertThat(
                        get(rota(v2) + "/versoes/1/demonstrativo", supervisor)
                                .body()
                                .getBytes(StandardCharsets.UTF_8))
                .isEqualTo(arquivoAntes);
        assertThat(
                        jdbc.queryForObject(
                                "select conteudo_hash from wms.versao_fechamento where id=?",
                                String.class,
                                versaoId))
                .isEqualTo(hashAntes);
    }

    @Test
    void f34RecusaDesconhecidoNaoAlteraVersoesMemoriaAuditoriaOuReplayAntesDeclaracao()
            throws Exception {
        configurar("2026-09-01", "DIAS_CORRIDOS", null, 30, null);
        fato("10", "2026-09-02T12:00:00Z");
        var f = entregar(aprovar(preparar(calcular("2026-09-01", "2026-10-01"))));
        assertThat(f.get("versao").get("estadoExterno").asString()).isEqualTo("DESCONHECIDO");
        var novo = calcular("2026-09-01", "2026-10-01");
        var comandoRecusado = reabrirComando(f, novo);
        long fechamentoId = f.get("fechamento").get("id").longValue();
        var versoesAntes =
                jdbc.queryForList(
                        "select * from wms.versao_fechamento where fechamento_id=? order by id",
                        fechamentoId);
        var fechamentoAntes =
                jdbc.queryForList("select * from wms.fechamento_cobranca where id=?", fechamentoId);
        var auditoriaAntes = jdbc.queryForList("select * from wms.auditoria_cadastro order by id");
        var operacoesAntes =
                jdbc.queryForList("select * from wms.operacao_administrativa order by id");
        var diasAntes = jdbc.queryForList("select * from wms.dia_fechamento order by id");
        var fatosAntes = jdbc.queryForList("select * from wms.fato_fechamento order by id");
        var entregasAntes = jdbc.queryForList("select * from wms.entrega_esl order by id");
        byte[] memoriaAntes =
                get(rota(f) + "/versoes/1/demonstrativo", supervisor)
                        .body()
                        .getBytes(StandardCharsets.UTF_8);
        var recusa = resposta(post(rota(f) + "/reabertura", comandoRecusado, gestor), 409);
        assertThat(recusa.toString()).contains("EMISSAO_DESCONHECIDA");
        // Todas estas consultas ocorrem imediatamente apos a recusa, antes de qualquer declaracao.
        assertThat(
                        jdbc.queryForList(
                                "select * from wms.versao_fechamento where fechamento_id=? order by id",
                                fechamentoId))
                .isEqualTo(versoesAntes);
        assertThat(
                        jdbc.queryForList(
                                "select * from wms.fechamento_cobranca where id=?", fechamentoId))
                .isEqualTo(fechamentoAntes);
        assertThat(jdbc.queryForList("select * from wms.auditoria_cadastro order by id"))
                .isEqualTo(auditoriaAntes);
        assertThat(jdbc.queryForList("select * from wms.operacao_administrativa order by id"))
                .isEqualTo(operacoesAntes);
        assertThat(jdbc.queryForList("select * from wms.dia_fechamento order by id"))
                .isEqualTo(diasAntes);
        assertThat(jdbc.queryForList("select * from wms.fato_fechamento order by id"))
                .isEqualTo(fatosAntes);
        assertThat(jdbc.queryForList("select * from wms.entrega_esl order by id"))
                .isEqualTo(entregasAntes);
        assertThat(
                        get(rota(f) + "/versoes/1/demonstrativo", supervisor)
                                .body()
                                .getBytes(StandardCharsets.UTF_8))
                .isEqualTo(memoriaAntes);
        f = confirmar(f);
        assertThat(f.get("versao").get("estadoExterno").asString())
                .isEqualTo("NAO_EMITIDO_CONFIRMADO");
        var aceita = resposta(post(rota(f) + "/reabertura", reabrirComando(f, novo), gestor), 200);
        assertThat(aceita.get("versao").get("numero").asInt()).isEqualTo(2);
        assertThat(aceita.get("versao").get("situacao").asString()).isEqualTo("PENDENTE_REVISAO");
        assertThat(
                        get(rota(aceita) + "/versoes/1/demonstrativo", supervisor)
                                .body()
                                .getBytes(StandardCharsets.UTF_8))
                .isEqualTo(memoriaAntes);
    }

    @ParameterizedTest(name = "{index} {0} corte={1} dias={2}")
    @CsvSource(
            nullValues = "NULL",
            value = {
                "DIAS_CORRIDOS, NULL, 1",
                "DIAS_CORRIDOS, NULL, 366",
                "MES_DIA_FIXO, 31, NULL"
            })
    void prio06ContratoAceitaLimitesPublicosEPreservaReplay(
            String modalidade, Integer diaCorte, Integer duracaoDias) throws Exception {
        var dados = contratoComando(modalidade, diaCorte, duracaoDias);
        var antes = snapshotContrato();
        var criado = resposta(post("/api/v1/contratos-cobranca", dados, gestor), 200);
        long contratoId = criado.get("id").longValue();
        assertThat(criado.get("clienteId").longValue()).isEqualTo(clienteId);
        assertThat(criado.get("armazemId").longValue()).isEqualTo(armazemId);
        assertThat(criado.get("modalidadeCiclo").asString()).isEqualTo(modalidade);
        assertThat(inteiroOuAusente(criado, "diaCorte")).isEqualTo(diaCorte);
        assertThat(inteiroOuAusente(criado, "duracaoDias")).isEqualTo(duracaoDias);
        assertThat(
                        jdbc.queryForObject(
                                "select dia_corte from wms.contrato_cobranca where id=?",
                                Integer.class,
                                contratoId))
                .isEqualTo(diaCorte);
        assertThat(
                        jdbc.queryForObject(
                                "select duracao_dias from wms.contrato_cobranca where id=?",
                                Integer.class,
                                contratoId))
                .isEqualTo(duracaoDias);
        var depois = snapshotContrato();
        assertThat(depois.contratos()).hasSize(antes.contratos().size() + 1);
        assertThat(depois.operacoes()).hasSize(antes.operacoes().size() + 1);
        assertThat(depois.auditorias()).hasSize(antes.auditorias().size() + 1);
        assertThat(depois.servicosMinimo()).isEqualTo(antes.servicosMinimo());
        var consulta =
                resposta(
                        get(
                                "/api/v1/contratos-cobranca?clienteId="
                                        + clienteId
                                        + "&armazemId="
                                        + armazemId,
                                gestor),
                        200);
        assertThat(consulta.size()).isEqualTo(1);
        assertThat(consulta.get(0)).isEqualTo(criado);
        assertThat(resposta(post("/api/v1/contratos-cobranca", dados, gestor), 200))
                .isEqualTo(criado);
        assertThat(snapshotContrato()).isEqualTo(depois);
    }

    @ParameterizedTest(name = "{index} {0} corte={1} dias={2} restricao={3}")
    @CsvSource(
            nullValues = "NULL",
            value = {
                "DIAS_CORRIDOS, NULL, 0, Min",
                "DIAS_CORRIDOS, NULL, 367, Max",
                "MES_DIA_FIXO, 31, 1, MODALIDADE",
                "DIAS_CORRIDOS, 31, 1, MODALIDADE",
                "MES_DIA_FIXO, NULL, NULL, MODALIDADE",
                "DIAS_CORRIDOS, NULL, NULL, MODALIDADE"
            })
    void prio06ContratoRecusaCamposInvalidosSemAlterarSnapshot(
            String modalidade, Integer diaCorte, Integer duracaoDias, String restricao)
            throws Exception {
        // Contrato valido preexistente, no mesmo contexto, torna o snapshot nao vazio.
        resposta(
                post(
                        "/api/v1/contratos-cobranca",
                        contratoComando("MES_DIA_FIXO", 31, null),
                        gestor),
                200);
        var dados = contratoComando(modalidade, diaCorte, duracaoDias);
        String uuidRecusado = dados.get("operacaoId").toString();
        var antes = snapshotContrato();
        var recusa = resposta(post("/api/v1/contratos-cobranca", dados, gestor), 400);
        // Sem outra escrita entre a recusa e a comparacao integral das mesmas linhas.
        assertThat(snapshotContrato()).isEqualTo(antes);
        assertThat(
                        jdbc.queryForObject(
                                "select count(*) from wms.operacao_administrativa where operacao_id=?",
                                Integer.class,
                                uuidRecusado))
                .isZero();
        assertThat(recusa.get("codigo").asString()).isEqualTo("DADOS_INVALIDOS");
        if ("MODALIDADE".equals(restricao)) {
            assertThat(recusa.get("detail").asString())
                    .isEqualTo("Configure corte ou duração conforme modalidade.");
        } else {
            var campos = recusa.get("campos");
            assertThat(campos.size()).isEqualTo(1);
            assertThat(campos.get(0).get("campo").asString()).isEqualTo("duracaoDias");
            assertThat(campos.get(0).get("codigo").asString()).isEqualTo(restricao);
        }
    }

    private SnapshotContrato snapshotContrato() {
        return new SnapshotContrato(
                jdbc.queryForList("select * from wms.contrato_cobranca order by id"),
                jdbc.queryForList("select * from wms.servico_minimo_contrato order by id"),
                jdbc.queryForList("select * from wms.operacao_administrativa order by id"),
                jdbc.queryForList("select * from wms.auditoria_cadastro order by id"));
    }

    private Integer inteiroOuAusente(JsonNode resposta, String campo) {
        var valor = resposta.get(campo);
        return valor == null || valor.isNull() ? null : valor.asInt();
    }

    private HashMap<String, Object> contratoComando(
            String modalidade, Integer diaCorte, Integer duracaoDias) {
        var dados = comando();
        dados.put("clienteId", clienteId);
        dados.put("armazemId", armazemId);
        dados.put("vigenciaInicio", "2026-09-01");
        dados.put("fuso", "UTC");
        dados.put("moeda", "BRL");
        dados.put("modalidadeCiclo", modalidade);
        if (diaCorte != null) dados.put("diaCorte", diaCorte);
        if (duracaoDias != null) dados.put("duracaoDias", duracaoDias);
        dados.put("minimoModo", "NAO_APLICAVEL");
        dados.put("grisModo", "NAO_APLICAVEL");
        dados.put("servicosMinimo", List.of());
        return dados;
    }

    private JsonNode configurar(
            String inicio, String modalidade, Integer nominal, Integer dias, String fim)
            throws Exception {
        var s = servico("ADICIONAL", "VEICULO");
        servicoId = s.get("id").longValue();
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
        d.put("servicoId", servicoId);
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
