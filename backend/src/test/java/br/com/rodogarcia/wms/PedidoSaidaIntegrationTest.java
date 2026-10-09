package br.com.rodogarcia.wms;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.reset;

import br.com.rodogarcia.wms.config.IdentidadeTesteConfig;
import br.com.rodogarcia.wms.dto.PedidoSaidaDto;
import br.com.rodogarcia.wms.dto.RevisaoCadastroRequest;
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
import br.com.rodogarcia.wms.models.OcupacaoEndereco;
import br.com.rodogarcia.wms.models.PedidoEntrada;
import br.com.rodogarcia.wms.models.Produto;
import br.com.rodogarcia.wms.models.TipoEndereco;
import br.com.rodogarcia.wms.models.TipoQuantidade;
import br.com.rodogarcia.wms.models.TipoUnidadeLogistica;
import br.com.rodogarcia.wms.models.UnidadeLogistica;
import br.com.rodogarcia.wms.services.EnderecoService;
import br.com.rodogarcia.wms.services.EstoqueService;
import br.com.rodogarcia.wms.services.PedidoSaidaService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.validation.ConstraintViolationException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
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
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.util.AopTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@ActiveProfiles("test")
@TestPropertySource(
        locations = "classpath:cadastros-test.properties",
        properties =
                "spring.datasource.url=jdbc:h2:mem:wms-saida;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000;INIT=CREATE SCHEMA IF NOT EXISTS wms")
@Import(IdentidadeTesteConfig.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PedidoSaidaIntegrationTest {
    private static final String SAIDAS = "/api/v1/pedidos-saida/";
    private static final Instant FIFO = Instant.parse("2026-09-01T12:00:00Z");
    private final HttpClient http =
            HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    @Autowired private Environment environment;
    @Autowired private JsonMapper mapper;
    @Autowired private JwtEncoder encoder;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private PlatformTransactionManager transactions;
    @Autowired private PedidoSaidaService service;
    @Autowired private java.time.Clock d30Clock;
    @MockitoSpyBean private EstoqueService estoqueService;
    @MockitoSpyBean private EnderecoService enderecoService;
    @PersistenceContext private EntityManager em;
    private Long clienteId;
    private Long armazemId;
    private Long produtoId;
    private Long segundoProdutoId;
    private int numero;
    private String operador;
    private String supervisor;
    private String gestor;

    private record Unidade(Long id, String codigo, Long notaId) {}

    @BeforeEach
    void preparar() {
        SecurityContextHolder.clearContext();
        jdbc.update("update wms.unidade_logistica set reserva_saida_id=null");
        for (String tabela :
                List.of(
                        "operacao_administrativa",
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
                        "auditoria_cadastro",
                        "embalagem",
                        "endereco",
                        "produto",
                        "cliente",
                        "armazem")) jdbc.update("delete from wms." + tabela);
        numero = 0;
        new TransactionTemplate(transactions)
                .executeWithoutResult(
                        s -> {
                            var agora = Instant.now();
                            var cliente =
                                    new Cliente(
                                            "CLIENTE", "Cliente ficticio", "12345678000199", agora);
                            var armazem =
                                    new Armazem(
                                            "ARM",
                                            "Armazem ficticio",
                                            "98765432000188",
                                            "Osasco",
                                            "SP",
                                            agora);
                            em.persist(cliente);
                            em.persist(armazem);
                            em.flush();
                            clienteId = cliente.getId();
                            armazemId = armazem.getId();
                            var produto =
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
                                            agora);
                            var segundo =
                                    new Produto(
                                            cliente,
                                            "SKU2",
                                            "Outro ficticio",
                                            "UN",
                                            TipoQuantidade.CONTAGEM,
                                            0,
                                            false,
                                            false,
                                            null,
                                            agora);
                            em.persist(produto);
                            em.persist(segundo);
                            em.flush();
                            produtoId = produto.getId();
                            segundoProdutoId = segundo.getId();
                            em.persist(
                                    new Embalagem(
                                            produto,
                                            "DUN",
                                            "Embalagem ficticia",
                                            new BigDecimal("10"),
                                            agora));
                            em.persist(
                                    new Embalagem(
                                            segundo,
                                            "DUN2",
                                            "Outra ficticia",
                                            new BigDecimal("10"),
                                            agora));
                        });
        operador = token("OPERACAO", List.of(clienteId), List.of(armazemId));
        supervisor = token("SUPERVISOR", List.of(clienteId), List.of(armazemId));
        gestor = token("GESTOR", List.of(), List.of());
    }

    @ParameterizedTest
    @ValueSource(strings = {"OPERACAO", "SUPERVISOR", "GESTOR"})
    void d27XmlCriaPedidoIntegralPreservaNotaReplayEPermiteReserva(String perfil) throws Exception {
        unidade("10");
        String identidade = token(perfil, List.of(clienteId), List.of(armazemId));
        var d = d27ImportarXml("2", "3");
        var fotoCriacao = D30FotografiaFisica.capturar(jdbc);
        var imported = resposta(post(SAIDAS + "xml", d, identidade), 201);
        d30CriacaoNaoMovimentaFisico(fotoCriacao, null, imported);
        assertThat(imported.get("documento").get("numero").longValue()).isEqualTo(271);
        assertThat(imported.get("documento").get("itens").size()).isEqualTo(2);
        assertThat(imported.get("pedido").get("itens").size()).isEqualTo(1);
        assertThat(imported.get("pedido").get("itens").get(0).get("quantidade").decimalValue())
                .isEqualByComparingTo("5");
        assertThat(resposta(post(SAIDAS + "xml", d, identidade), 201)).isEqualTo(imported);
        assertThat(contar("pedido_saida")).isEqualTo(1);
        assertThat(contar("operacao_administrativa")).isEqualTo(1);
        assertThat(contar("auditoria_cadastro")).isEqualTo(2);
        assertThat(contar("reserva_saida")).isZero();
        var altered = new HashMap<>(d);
        altered.put("xml", d.get("xml").toString().replace("<qCom>2</qCom>", "<qCom>1</qCom>"));
        assertCodigo(post(SAIDAS + "xml", altered, identidade), 409, "OPERACAO_DIVERGENTE");
        var duplicate = new HashMap<>(d);
        duplicate.put("operacaoId", UUID.randomUUID());
        assertCodigo(post(SAIDAS + "xml", duplicate, identidade), 409, "REFERENCIA_DUPLICADA");
        var p = imported.get("pedido");
        var reserved =
                resposta(post(rota(p, "/reserva"), reservar(p), identidade), 200).get("pedido");
        assertThat(reserved.get("situacao").asString()).isEqualTo("RESERVADO");
        saldo("10", "0", "5", "5");
        assertThat(
                        d30GetFotografiaIntegral(
                                        "/api/v1/pedidos-saida?clienteId="
                                                + clienteId
                                                + "&armazemId="
                                                + armazemId,
                                        gestor)
                                .get("totalItens")
                                .longValue())
                .isPositive();
    }

    @Test
    void d27XmlRecusaXXEUnidadeAlcanceInsuficienciaSemPersistenciaParcial() throws Exception {
        unidade("10");
        var d = d27ImportarXml("2", "3");
        assertThat(post(SAIDAS + "xml", d, null).statusCode()).isEqualTo(401);
        assertThat(
                        post(SAIDAS + "xml", d, token("OPERACAO", List.of(), List.of(armazemId)))
                                .statusCode())
                .isEqualTo(403);
        assertThat(
                        post(SAIDAS + "xml", d, token("OPERACAO", List.of(clienteId), List.of()))
                                .statusCode())
                .isEqualTo(403);
        Map<String, Object> bad = new HashMap<>(d);
        bad.put(
                "xml",
                "<!DOCTYPE NFe [<!ENTITY x SYSTEM 'file:///arquivo-ficticio'>]>"
                        + d.get("xml").toString().replace("SKU", "&x;"));
        assertThat(post(SAIDAS + "xml", bad, operador).statusCode()).isEqualTo(400);
        bad = new HashMap<>(d);
        bad.put("xml", d.get("xml").toString().replace("<uCom>UN</uCom>", "<uCom>KG</uCom>"));
        assertThat(post(SAIDAS + "xml", bad, operador).statusCode()).isEqualTo(400);
        bad = new HashMap<>(d);
        bad.put("xml", d.get("xml").toString().replace("<qCom>2</qCom>", "<qCom>-1</qCom>"));
        assertThat(post(SAIDAS + "xml", bad, operador).statusCode()).isEqualTo(400);
        bad = d27ImportarXml("6", "5");
        assertCodigo(post(SAIDAS + "xml", bad, operador), 409, "SALDO_INSUFICIENTE");
        assertThat(contar("pedido_saida")).isZero();
        assertThat(contar("item_pedido_saida")).isZero();
        assertThat(contar("operacao_administrativa")).isZero();
        assertThat(contar("operacao_saida")).isZero();
        assertThat(contar("auditoria_cadastro")).isZero();
    }

    @Test
    void d27XmlRecusaExpoentesExtremosAntesDeSomarSemEfeitos() throws Exception {
        unidade("10");
        for (String quantity :
                List.of("1E2147483647", "100E2147483647", "1E13", "0.0000001", "1E-2147483647")) {
            assertThat(post(SAIDAS + "xml", d27ImportarXml(quantity, "1"), operador).statusCode())
                    .as("quantidade fora de DECIMAL19,6: %s", quantity)
                    .isEqualTo(400);
            assertThat(contar("pedido_saida")).isZero();
            assertThat(contar("operacao_administrativa")).isZero();
            assertThat(contar("auditoria_cadastro")).isZero();
        }
        saldo("10", "10", "0", "0");
    }

    @Test
    void d27XmlNotacaoCientificaDentroDaFaixaMantemSomaEReplay() throws Exception {
        unidade("10");
        var d = d27ImportarXml("2E0", "3E0");
        var created = resposta(post(SAIDAS + "xml", d, operador), 201);
        assertThat(created.get("pedido").get("itens").get(0).get("quantidade").decimalValue())
                .isEqualByComparingTo("5");
        assertThat(resposta(post(SAIDAS + "xml", d, operador), 201)).isEqualTo(created);
        assertThat(contar("pedido_saida")).isEqualTo(1);
        assertThat(contar("operacao_administrativa")).isEqualTo(1);
        assertThat(contar("reserva_saida")).isZero();
    }

    private Map<String, Object> d27ImportarXml(String first, String second) {
        String det =
                "<det nItem='%s'><prod><cProd>SKU</cProd><uCom>UN</uCom><qCom>%s</qCom><vProd>10</vProd></prod></det>";
        String xml =
                "<NFe xmlns='http://www.portalfiscal.inf.br/nfe'><infNFe versao='4.00' Id='NFe"
                        + "7".repeat(44)
                        + "'><ide><mod>55</mod><serie>1</serie><nNF>271</nNF><dhEmi>2026-10-07T00:00:00Z</dhEmi></ide><emit><CNPJ>12345678000199</CNPJ></emit>"
                        + det.formatted("1", first)
                        + det.formatted("2", second)
                        + "</infNFe></NFe>";
        return new HashMap<>(
                Map.of(
                        "operacaoId",
                        UUID.randomUUID(),
                        "clienteId",
                        clienteId,
                        "armazemId",
                        armazemId,
                        "xml",
                        xml,
                        "motivo",
                        "D27 NF-e ficticia sem emissao real"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"OPERACAO", "SUPERVISOR", "GESTOR"})
    void perfisReservamFifoConservandoFisicoEProtegendoRestanteDoPallet(String perfil)
            throws Exception {
        var u = unidade("10");
        String identidade = token(perfil, List.of(clienteId), List.of(armazemId));
        var p = criar(produtoId, "4", identidade);
        var outro = criar(produtoId, "1", identidade);
        var etiqueta = resposta(get(unidadeRota(u, "/etiqueta"), operador), 200);
        var antes = resposta(get(unidadeRota(u, "/estoque"), operador), 200);
        var reserva =
                resposta(post(rota(p, "/reserva"), reservar(p), identidade), 200).get("pedido");
        assertThat(reserva.get("situacao").asString()).isEqualTo("RESERVADO");
        assertThat(reserva.get("podeProsseguir").booleanValue()).isTrue();
        assertThat(reserva.get("reservas").get(0).get("notaOrigemId").longValue())
                .isEqualTo(u.notaId());
        assertThat(reserva.get("reservas").get(0).get("dataFifo").asString())
                .isEqualTo(FIFO.toString());
        saldo("10", "0", "4", "6");
        var depois = resposta(get(unidadeRota(u, "/estoque"), operador), 200);
        assertThat(depois.get("unidade").get("disponivelParaSaida").booleanValue()).isFalse();
        assertThat(depois.get("inicioArmazenagemEm")).isEqualTo(antes.get("inicioArmazenagemEm"));
        assertThat(depois.get("unidade").get("chegadaReal"))
                .isEqualTo(antes.get("unidade").get("chegadaReal"));
        assertThat(resposta(get(unidadeRota(u, "/etiqueta"), operador), 200)).isEqualTo(etiqueta);
        assertCodigo(
                post(rota(outro, "/reserva"), reservar(outro), identidade),
                409,
                "SALDO_INSUFICIENTE");
        assertThat(contar("reserva_saida")).isEqualTo(1);
    }

    @Test
    void reservaNaoVenceComTempoENaoAlteraOrigem() throws Exception {
        unidade("10");
        var p = criar(produtoId, "10", operador);
        var r = resposta(post(rota(p, "/reserva"), reservar(p), operador), 200).get("pedido");
        jdbc.update(
                "update wms.reserva_saida set criada_em=? where pedido_id=?",
                java.sql.Timestamp.from(Instant.parse("2020-01-01T00:00:00Z")),
                r.get("id").longValue());
        var atual = resposta(get(rota(p, ""), operador), 200);
        assertThat(atual.get("reservas").get(0).get("situacao").asString()).isEqualTo("ATIVA");
        assertThat(atual.get("podeProsseguir").booleanValue()).isTrue();
        saldo("10", "0", "10", "0");
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void disputaDuasTransacoesComSaldoLimitadoOuRepeticaoDaMesmaChave(boolean mesmaChave)
            throws Exception {
        unidade("10");
        var p = criar(produtoId, "8", operador);
        var outro = mesmaChave ? p : criar(produtoId, "8", operador);
        var a = reservar(p);
        var b = mesmaChave ? a : reservar(outro);
        var resultados =
                paralelo(
                        () -> post(rota(p, "/reserva"), a, operador),
                        () -> post(rota(outro, "/reserva"), b, operador));
        assertThat(resultados)
                .containsExactlyInAnyOrder(
                        mesmaChave ? new Integer[] {200, 200} : new Integer[] {200, 409});
        assertThat(contar("reserva_saida")).isEqualTo(1);
        assertThat(
                        jdbc.queryForObject(
                                "select count(*) from wms.unidade_logistica where reserva_saida_id is not null",
                                Integer.class))
                .isEqualTo(1);
        saldo("10", "0", "8", "2");
    }

    @Test
    void duasChavesDoMesmoPedidoNaoReservamDuasVezes() throws Exception {
        unidade("10");
        var p = criar(produtoId, "4", operador);
        var a = reservar(p);
        var b = reservar(p);
        assertThat(
                        paralelo(
                                () -> post(rota(p, "/reserva"), a, operador),
                                () -> post(rota(p, "/reserva"), b, operador)))
                .containsExactlyInAnyOrder(200, 409);
        assertThat(contar("reserva_saida")).isEqualTo(1);
    }

    @Test
    void pedidoMultiitemReservaTudoOuReverteTudo() throws Exception {
        unidade("10");
        var segundo =
                unidade(
                        segundoProdutoId,
                        "3",
                        FIFO,
                        0,
                        1,
                        "B",
                        TipoEndereco.ARMAZENAGEM,
                        CondicaoMercadoria.BOA);
        var p = criarItens(List.of(item(produtoId, "5"), item(segundoProdutoId, "2")), operador);
        resposta(post(unidadeRota(segundo, "/bloqueio"), bloqueio(segundo), operador), 200);
        var dados = reservar(p);
        assertCodigo(post(rota(p, "/reserva"), dados, operador), 409, "SALDO_INSUFICIENTE");
        assertThat(contar("reserva_saida")).isZero();
        assertThat(contar("operacao_saida")).isEqualTo(1);
        assertThat(resposta(get(rota(p, ""), operador), 200)).isEqualTo(p);
        saldo("10", "10", "0", "0");
        resposta(post(unidadeRota(segundo, "/liberacao"), bloqueio(segundo), supervisor), 200);
        var r = resposta(post(rota(p, "/reserva"), dados, operador), 200).get("pedido");
        assertThat(r.get("reservas").size()).isEqualTo(2);
        assertThat(r.get("podeProsseguir").booleanValue()).isTrue();
        saldo("10", "0", "5", "5");
        saldo(segundoProdutoId, "3", "0", "2", "1");
    }

    @Test
    void fifoEscolheDataOriginalAntesDeNivelOuChegadaReal() throws Exception {
        var recente =
                unidade(
                        produtoId,
                        "5",
                        FIFO.plusSeconds(86400),
                        0,
                        1,
                        "A",
                        TipoEndereco.ARMAZENAGEM,
                        CondicaoMercadoria.BOA);
        var antiga =
                unidade(
                        produtoId,
                        "5",
                        FIFO,
                        4,
                        90,
                        "Z",
                        TipoEndereco.ARMAZENAGEM,
                        CondicaoMercadoria.BOA);
        var p = criar(produtoId, "3", operador);
        var sugestao = resposta(get(rota(p, "/fifo"), operador), 200);
        assertThat(sugestao.get("selecoes").get(0).get("unidadeId").longValue())
                .isEqualTo(antiga.id());
        assertThat(sugestao.get("selecoes").get(0).get("unidadeId").longValue())
                .isNotEqualTo(recente.id());
    }

    @Test
    void desempateSegueNivelSequenciaCodigoEId() throws Exception {
        unidade(produtoId, "2", FIFO, 2, 1, "A", TipoEndereco.ARMAZENAGEM, CondicaoMercadoria.BOA);
        unidade(produtoId, "2", FIFO, 0, 20, "B", TipoEndereco.ARMAZENAGEM, CondicaoMercadoria.BOA);
        var primeira =
                unidade(
                        produtoId,
                        "2",
                        FIFO,
                        0,
                        10,
                        "C",
                        TipoEndereco.ARMAZENAGEM,
                        CondicaoMercadoria.BOA);
        var segunda =
                unidade(
                        produtoId,
                        "2",
                        FIFO,
                        0,
                        10,
                        "D",
                        TipoEndereco.ARMAZENAGEM,
                        CondicaoMercadoria.BOA);
        var p = criar(produtoId, "4", operador);
        var sugerida = resposta(get(rota(p, "/fifo"), operador), 200).get("selecoes");
        assertThat(sugerida.get(0).get("unidadeId").longValue()).isEqualTo(primeira.id());
        assertThat(sugerida.get(1).get("unidadeId").longValue()).isEqualTo(segunda.id());
        assertThat(resposta(get(rota(p, "/fifo"), operador), 200).get("selecoes"))
                .isEqualTo(sugerida);
    }

    @ParameterizedTest
    @ValueSource(strings = {"OPERACAO", "SUPERVISOR", "GESTOR"})
    void qualquerPerfilJustificaMasSomenteSupervisorGestorAutoriza(String perfil) throws Exception {
        unidade("10");
        var recente =
                unidade(
                        produtoId,
                        "10",
                        FIFO.plusSeconds(86400),
                        0,
                        1,
                        "RECENTE",
                        TipoEndereco.ARMAZENAGEM,
                        CondicaoMercadoria.BOA);
        var p = criar(produtoId, "4", operador);
        String identidade = token(perfil, List.of(clienteId), List.of(armazemId));
        var proposta =
                resposta(
                        post(
                                rota(p, "/justificativas-fifo"),
                                justificar(p, recente, "4"),
                                identidade),
                        200);
        assertThat(proposta.get("excecaoFifo").booleanValue()).isTrue();
        assertThat(proposta.get("justificadaPor").asString()).isEqualTo("saida-" + perfil);
        var dados = reservar(proposta.get("pedido"));
        dados.put("justificativaId", proposta.get("operacaoId").asString());
        resposta(post(rota(p, "/reserva"), dados, operador), 403);
        var reserva = resposta(post(rota(p, "/reserva"), dados, supervisor), 200);
        assertThat(reserva.get("excecaoFifo").booleanValue()).isTrue();
        assertThat(reserva.get("pedido").get("reservas").get(0).get("unidadeId").longValue())
                .isEqualTo(recente.id());
        resposta(post(rota(p, "/reserva"), dados, operador), 403);
        assertThat(resposta(post(rota(p, "/reserva"), dados, gestor), 200)).isEqualTo(reserva);
    }

    @Test
    void justificativaParcialDuplicadaOuSemMotivoNaoAutorizaReserva() throws Exception {
        var u = unidade("10");
        var p = criar(produtoId, "4", operador);
        assertCodigo(
                post(rota(p, "/justificativas-fifo"), justificar(p, u, "3"), operador),
                409,
                "PEDIDO_NAO_INTEGRAL");
        var repetida = justificar(p, u, "2");
        repetida.put(
                "selecoes",
                List.of(
                        Map.of("unidadeId", u.id(), "quantidade", 2),
                        Map.of("unidadeId", u.id(), "quantidade", 2)));
        resposta(post(rota(p, "/justificativas-fifo"), repetida, operador), 400);
        var semMotivo = justificar(p, u, "4");
        semMotivo.put("motivo", "   ");
        resposta(post(rota(p, "/justificativas-fifo"), semMotivo, operador), 400);
        assertThat(contar("reserva_saida")).isZero();
    }

    @Test
    void justificativaRevalidaDisponibilidadeERecusaPropostaAntiga() throws Exception {
        var u = unidade("10");
        var p = criar(produtoId, "4", operador);
        var proposta =
                resposta(
                        post(rota(p, "/justificativas-fifo"), justificar(p, u, "4"), operador),
                        200);
        var nova =
                resposta(
                        post(
                                rota(p, "/justificativas-fifo"),
                                justificar(proposta.get("pedido"), u, "4"),
                                operador),
                        200);
        var dados = reservar(nova.get("pedido"));
        dados.put("justificativaId", proposta.get("operacaoId").asString());
        assertCodigo(
                post(rota(p, "/reserva"), dados, supervisor), 409, "JUSTIFICATIVA_DESATUALIZADA");
        dados.put("justificativaId", nova.get("operacaoId").asString());
        resposta(post(unidadeRota(u, "/bloqueio"), bloqueio(u), operador), 200);
        resposta(post(rota(p, "/reserva"), dados, supervisor), 409);
        assertThat(contar("reserva_saida")).isZero();
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "TRIAGEM",
                "QUARENTENA",
                "AVARIA",
                "BLOQUEIO",
                "CLIENTE",
                "ARMAZEM",
                "PRODUTO",
                "ENDERECO"
            })
    void indisponibilidadeCentralImpedeReservaEExcecao(String causa) throws Exception {
        var u = unidade("10");
        var p = criar(produtoId, "4", operador);
        if (causa.equals("AVARIA"))
            resposta(post(unidadeRota(u, "/avaria"), bloqueio(u), operador), 200);
        else if (causa.equals("BLOQUEIO"))
            resposta(post(unidadeRota(u, "/bloqueio"), bloqueio(u), operador), 200);
        else if (causa.equals("TRIAGEM") || causa.equals("QUARENTENA")) {
            jdbc.update("update wms.unidade_logistica set tipo_localizacao=?", causa);
            jdbc.update("update wms.endereco set tipo=?", causa);
        } else
            jdbc.update(
                    "update wms."
                            + causa.toLowerCase(java.util.Locale.ROOT)
                            + " set situacao='ENCERRAMENTO_PENDENTE'");
        resposta(post(rota(p, "/reserva"), reservar(p), operador), 409);
        resposta(post(rota(p, "/justificativas-fifo"), justificar(p, u, "4"), supervisor), 409);
        assertThat(contar("reserva_saida")).isZero();
        saldo("10", "0", "0", "10");
    }

    @ParameterizedTest
    @ValueSource(strings = {"bloqueio", "avaria"})
    void ocorrenciaPosteriorSinalizaPedidoMantemReservaEImpedeProsseguimento(String acao)
            throws Exception {
        var u = unidade("10");
        var p = criar(produtoId, "4", operador);
        var dados = reservar(p);
        var original = resposta(post(rota(p, "/reserva"), dados, operador), 200);
        var comando = bloqueio(u);
        var evento = resposta(post(unidadeRota(u, "/" + acao), comando, operador), 200);
        assertThat(resposta(post(unidadeRota(u, "/" + acao), comando, operador), 200))
                .isEqualTo(evento);
        var impedido = resposta(get(rota(p, ""), operador), 200);
        assertThat(impedido.get("podeProsseguir").booleanValue()).isFalse();
        assertThat(impedido.get("unidadesImpedidas").get(0).longValue()).isEqualTo(u.id());
        assertCodigo(post(rota(p, "/revalidacao"), null, operador), 409, "RESERVA_IMPEDIDA");
        assertThat(resposta(post(rota(p, "/reserva"), dados, operador), 200)).isEqualTo(original);
        saldo("10", "0", "4", "6");
        var liberar = bloqueio(u);
        if (acao.equals("avaria")) {
            resposta(post(unidadeRota(u, "/liberacao"), liberar, supervisor), 409);
            assertThat(evento.get("estoque").get("avariaPosterior").booleanValue()).isTrue();
            assertThat(evento.get("estoque").get("unidade").get("condicao").asString())
                    .isEqualTo("BOA");
        } else {
            resposta(post(unidadeRota(u, "/liberacao"), liberar, supervisor), 200);
            resposta(post(rota(p, "/revalidacao"), null, operador), 200);
        }
        saldo("10", "0", "4", "6");
    }

    @Test
    void reservaEDanoConcorrentesConservamEstoqueSemLiberarReserva() throws Exception {
        var u = unidade("10");
        var p = criar(produtoId, "4", operador);
        var dados = reservar(p);
        var dano = bloqueio(u);
        var resultados =
                paralelo(
                        () -> post(rota(p, "/reserva"), dados, operador),
                        () -> post(unidadeRota(u, "/avaria"), dano, operador));
        // Reserva altera revisão da unidade: avaria com revisão antiga deve reler.
        assertThat(resultados).allMatch(s -> s == 200 || s == 409);
        if (resultados.get(1) == 409)
            resposta(post(unidadeRota(u, "/avaria"), bloqueio(u), operador), 200);
        var atual = resposta(get(rota(p, ""), operador), 200);
        assertThat(atual.get("podeProsseguir").booleanValue()).isFalse();
        assertThat(atual.get("reservas").size()).isEqualTo(resultados.get(0) == 200 ? 1 : 0);
        saldo(
                "10",
                "0",
                resultados.get(0) == 200 ? "4" : "0",
                resultados.get(0) == 200 ? "6" : "10");
    }

    @ParameterizedTest
    @ValueSource(strings = {"cancelamento", "reversao-reserva"})
    void cancelamentoOuReversaoLiberaTudoSemBaixaFisicaEPermiteRepeticao(String acao)
            throws Exception {
        unidade("10");
        unidade(
                segundoProdutoId,
                "10",
                FIFO,
                0,
                1,
                "B",
                TipoEndereco.ARMAZENAGEM,
                CondicaoMercadoria.BOA);
        var p = criarItens(List.of(item(produtoId, "4"), item(segundoProdutoId, "3")), operador);
        var reservada =
                resposta(post(rota(p, "/reserva"), reservar(p), operador), 200).get("pedido");
        var comando = comando(reservada);
        var d30ReservasAntes = D30ReservaFotografia.capturar(jdbc, p.get("id").longValue());
        D30ReservaFotografia.exigirAtivas(d30ReservasAntes);
        resposta(post(rota(p, "/" + acao), comando, operador), 403);
        var d30Inicio = d30Clock.instant();
        var resultado = resposta(post(rota(p, "/" + acao), comando, supervisor), 200);
        var d30Termino = d30Clock.instant();
        var d30ReservasDepois = D30ReservaFotografia.capturar(jdbc, p.get("id").longValue());
        D30ReservaFotografia.exigirEncerradas(
                d30ReservasAntes,
                d30ReservasDepois,
                acao.equals("cancelamento") ? "CANCELADA" : "REVERTIDA",
                d30Inicio,
                d30Termino);
        assertThat(resposta(post(rota(p, "/" + acao), comando, supervisor), 200))
                .isEqualTo(resultado);
        assertThat(D30ReservaFotografia.capturar(jdbc, p.get("id").longValue()))
                .isEqualTo(d30ReservasDepois);
        saldo("10", "10", "0", "0");
        saldo(segundoProdutoId, "10", "10", "0", "0");
        assertThat(resultado.get("pedido").get("reservas").size()).isEqualTo(2);
        assertThat(resultado.get("pedido").get("reservas").get(0).get("situacao").asString())
                .isEqualTo(acao.equals("cancelamento") ? "CANCELADA" : "REVERTIDA");
        if (acao.equals("reversao-reserva")) {
            var nova =
                    resposta(
                                    post(
                                            rota(p, "/reserva"),
                                            reservar(resultado.get("pedido")),
                                            operador),
                                    200)
                            .get("pedido");
            assertThat(nova.get("reservas").size()).isEqualTo(4);
            saldo("10", "0", "4", "6");
        } else
            resposta(post(rota(p, "/reserva"), reservar(resultado.get("pedido")), operador), 409);
    }

    @Test
    void cancelarReservaAvariadaNaoLiberaMercadoriaIndisponivel() throws Exception {
        var u = unidade("10");
        var p = criar(produtoId, "4", operador);
        var reservada =
                resposta(post(rota(p, "/reserva"), reservar(p), operador), 200).get("pedido");
        resposta(post(unidadeRota(u, "/avaria"), bloqueio(u), operador), 200);
        resposta(post(rota(p, "/cancelamento"), comando(reservada), supervisor), 200);
        saldo("10", "0", "0", "10");
    }

    @ParameterizedTest
    @ValueSource(strings = {"RESERVA_SAIDA", "CANCELAMENTO_SAIDA", "REVERSAO_RESERVA"})
    void falhaDeAuditoriaReverteEstadoLinhasPonteVersoesESaldo(String acao) throws Exception {
        unidade("10");
        unidade(
                segundoProdutoId,
                "10",
                FIFO,
                0,
                1,
                "B",
                TipoEndereco.ARMAZENAGEM,
                CondicaoMercadoria.BOA);
        var p = criarItens(List.of(item(produtoId, "4"), item(segundoProdutoId, "3")), operador);
        if (!acao.equals("RESERVA_SAIDA"))
            p = resposta(post(rota(p, "/reserva"), reservar(p), operador), 200).get("pedido");
        var antes = p;
        var rota =
                rota(
                        p,
                        acao.equals("RESERVA_SAIDA")
                                ? "/reserva"
                                : acao.equals("CANCELAMENTO_SAIDA")
                                        ? "/cancelamento"
                                        : "/reversao-reserva");
        var dados = acao.equals("RESERVA_SAIDA") ? reservar(p) : comando(p);
        var identidade = acao.equals("RESERVA_SAIDA") ? operador : supervisor;
        int linhas = contar("reserva_saida"),
                operacoes = contar("operacao_saida"),
                auditorias = contar("auditoria_cadastro");
        jdbc.execute(
                "alter table wms.auditoria_cadastro add constraint falha_saida_teste check (acao <> '"
                        + acao
                        + "')");
        try {
            resposta(post(rota, dados, identidade), 409);
            assertThat(resposta(get(rota(antes, ""), operador), 200)).isEqualTo(antes);
            assertThat(contar("reserva_saida")).isEqualTo(linhas);
            assertThat(contar("operacao_saida")).isEqualTo(operacoes);
            assertThat(contar("auditoria_cadastro")).isEqualTo(auditorias);
            saldo(
                    "10",
                    acao.equals("RESERVA_SAIDA") ? "10" : "0",
                    acao.equals("RESERVA_SAIDA") ? "0" : "4",
                    acao.equals("RESERVA_SAIDA") ? "0" : "6");
            saldo(
                    segundoProdutoId,
                    "10",
                    acao.equals("RESERVA_SAIDA") ? "10" : "0",
                    acao.equals("RESERVA_SAIDA") ? "0" : "3",
                    acao.equals("RESERVA_SAIDA") ? "0" : "7");
        } finally {
            jdbc.execute("alter table wms.auditoria_cadastro drop constraint falha_saida_teste");
        }
        resposta(post(rota, dados, identidade), 200);
    }

    @Test
    void falhaDaRespostaIdempotenteReverteAuditoriaEEstoque() throws Exception {
        unidade("10");
        var p = criar(produtoId, "4", operador);
        var dados = reservar(p);
        int auditorias = contar("auditoria_cadastro");
        jdbc.execute(
                "alter table wms.operacao_saida add constraint falha_operacao_teste check (tipo <> 'RESERVA_SAIDA')");
        try {
            resposta(post(rota(p, "/reserva"), dados, operador), 409);
            assertThat(resposta(get(rota(p, ""), operador), 200)).isEqualTo(p);
            assertThat(contar("reserva_saida")).isZero();
            assertThat(contar("auditoria_cadastro")).isEqualTo(auditorias);
            saldo("10", "10", "0", "0");
        } finally {
            jdbc.execute("alter table wms.operacao_saida drop constraint falha_operacao_teste");
        }
        resposta(post(rota(p, "/reserva"), dados, operador), 200);
    }

    @Test
    void criacaoIdempotenteConcorrenteEConflitoDePayload() throws Exception {
        unidade("10");
        var dados = criarDados(List.of(item(produtoId, "4")));
        assertThat(
                        paralelo(
                                () -> post("/api/v1/pedidos-saida", dados, operador),
                                () -> post("/api/v1/pedidos-saida", dados, operador)))
                .containsExactly(201, 201);
        assertThat(contar("pedido_saida")).isEqualTo(1);
        assertThat(contar("item_pedido_saida")).isEqualTo(1);
        dados.put("motivo", "Conteudo divergente");
        assertCodigo(post("/api/v1/pedidos-saida", dados, operador), 409, "OPERACAO_REUTILIZADA");
    }

    @Test
    void repeticaoRetornaOriginalDepoisDeReversaoERecusaPayloadDivergente() throws Exception {
        unidade("10");
        var p = criar(produtoId, "4", operador);
        var dados = reservar(p);
        var original = resposta(post(rota(p, "/reserva"), dados, operador), 200);
        resposta(
                post(rota(p, "/reversao-reserva"), comando(original.get("pedido")), supervisor),
                200);
        assertThat(resposta(post(rota(p, "/reserva"), dados, operador), 200)).isEqualTo(original);
        dados.put("motivo", "Outro motivo divergente");
        assertCodigo(post(rota(p, "/reserva"), dados, operador), 409, "OPERACAO_REUTILIZADA");
        assertThat(contar("reserva_saida")).isEqualTo(1);
        saldo("10", "10", "0", "0");
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void alcanceSimultaneoPorClienteEArmazemEmConsultasComandosERepeticao(boolean clienteCorreto)
            throws Exception {
        unidade("10");
        var p = criar(produtoId, "4", operador);
        var dados = reservar(p);
        resposta(post(rota(p, "/reserva"), dados, operador), 200);
        String semAlcance =
                token(
                        "SUPERVISOR",
                        List.of(clienteCorreto ? clienteId : clienteId + 999),
                        List.of(clienteCorreto ? armazemId + 999 : armazemId));
        resposta(get(rota(p, ""), semAlcance), 403);
        resposta(post(rota(p, "/reserva"), dados, semAlcance), 403);
        resposta(
                get(
                        "/api/v1/pedidos-saida?clienteId=" + clienteId + "&armazemId=" + armazemId,
                        semAlcance),
                403);
        resposta(
                post(
                        rota(p, "/cancelamento"),
                        comando(resposta(get(rota(p, ""), operador), 200)),
                        semAlcance),
                403);
    }

    @Test
    void servicoValidaParametrosEPermissoesSemController() throws Exception {
        assertThatThrownBy(() -> service.criar(null))
                .isInstanceOf(ConstraintViolationException.class);
        assertThatThrownBy(() -> service.consultar(-1L))
                .isInstanceOf(ConstraintViolationException.class);
        unidade("10");
        var p = criar(produtoId, "4", operador);
        assertThatThrownBy(() -> service.consultar(p.get("id").longValue()))
                .isInstanceOf(AccessDeniedException.class);
        var jwt =
                Jwt.withTokenValue("ficticio")
                        .header("alg", "RS256")
                        .subject("direto")
                        .claim("wms_perfil", "OPERACAO")
                        .claim("wms_clientes", List.of(clienteId.toString()))
                        .claim("wms_armazens", List.of(armazemId.toString()))
                        .build();
        SecurityContextHolder.getContext()
                .setAuthentication(new JwtAuthenticationToken(jwt, List.of()));
        try {
            assertThatThrownBy(
                            () ->
                                    service.cancelar(
                                            p.get("id").longValue(),
                                            new PedidoSaidaDto.Comando(
                                                    UUID.randomUUID(),
                                                    p.get("versao").longValue(),
                                                    "Cancelamento solicitado")))
                    .isInstanceOf(AccessDeniedException.class);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    void validacaoQuantidadeSkuDuplicadoRevisaoEPagina() throws Exception {
        resposta(
                post(
                        "/api/v1/pedidos-saida",
                        criarDados(List.of(item(produtoId, "1.5"))),
                        operador),
                400);
        resposta(
                post(
                        "/api/v1/pedidos-saida",
                        criarDados(List.of(item(produtoId, "1"), item(produtoId, "2"))),
                        operador),
                400);
        assertThat(contar("pedido_saida")).isZero();
        unidade("10");
        var p = criar(produtoId, "4", operador);
        var dados = reservar(p);
        dados.put("versao", p.get("versao").longValue() + 1);
        assertCodigo(post(rota(p, "/reserva"), dados, operador), 409, "VERSAO_DESATUALIZADA");
        resposta(
                get(
                        "/api/v1/pedidos-saida?clienteId="
                                + clienteId
                                + "&armazemId="
                                + armazemId
                                + "&tamanho=101",
                        operador),
                400);
        resposta(get(rota(p, ""), null), 401);
    }

    @Test
    void criacaoNaoExcedeSaldoDeNenhumItemNemDeixaPedidoParcial() throws Exception {
        unidade("10");
        var excedente = criarDados(List.of(item(produtoId, "11")));
        assertCodigo(post("/api/v1/pedidos-saida", excedente, operador), 409, "SALDO_INSUFICIENTE");
        assertCodigo(
                post(
                        "/api/v1/pedidos-saida",
                        criarDados(List.of(item(produtoId, "5"), item(segundoProdutoId, "1"))),
                        operador),
                409,
                "SALDO_INSUFICIENTE");
        assertThat(contar("pedido_saida")).isZero();
        assertThat(contar("item_pedido_saida")).isZero();
        assertThat(contar("operacao_saida")).isZero();
        assertThat(contar("auditoria_cadastro")).isZero();
        saldo("10", "10", "0", "0");
    }

    @Test
    void bobinasQuatroESeisPermitemExcecaoIntegralSeFifoGulosoNaoFecha() throws Exception {
        var antiga = bobina("4", FIFO, "ANTIGA");
        var recente = bobina("6", FIFO.plusSeconds(86400), "RECENTE");
        var p = criar(produtoId, "6", operador);
        assertCodigo(get(rota(p, "/fifo"), operador), 409, "SALDO_INSUFICIENTE");
        assertCodigo(post(rota(p, "/reserva"), reservar(p), operador), 409, "SALDO_INSUFICIENTE");
        assertThat(contar("reserva_saida")).isZero();
        var proposta =
                resposta(
                        post(
                                rota(p, "/justificativas-fifo"),
                                justificar(p, recente, "6"),
                                operador),
                        200);
        assertThat(proposta.get("excecaoFifo").booleanValue()).isTrue();
        var dados = reservar(proposta.get("pedido"));
        dados.put("justificativaId", proposta.get("operacaoId").asString());
        resposta(post(rota(p, "/reserva"), dados, operador), 403);
        var r = resposta(post(rota(p, "/reserva"), dados, supervisor), 200).get("pedido");
        assertThat(r.get("reservas").size()).isEqualTo(1);
        assertThat(r.get("reservas").get(0).get("unidadeId").longValue()).isEqualTo(recente.id());
        assertThat(r.get("reservas").get(0).get("unidadeId").longValue()).isNotEqualTo(antiga.id());
        saldo("10", "4", "6", "0");
    }

    @Test
    void bobinasInteirasFechamAutomaticamentePelaOrdemFifoESemFracao() throws Exception {
        var antiga = bobina("4", FIFO, "ANTIGA");
        var recente = bobina("6", FIFO.plusSeconds(86400), "RECENTE");
        var p = criar(produtoId, "10", operador);
        var selecao = resposta(get(rota(p, "/fifo"), operador), 200).get("selecoes");
        assertThat(selecao.get(0).get("unidadeId").longValue()).isEqualTo(antiga.id());
        assertThat(selecao.get(1).get("unidadeId").longValue()).isEqualTo(recente.id());
        var r = resposta(post(rota(p, "/reserva"), reservar(p), operador), 200);
        assertThat(r.get("excecaoFifo").booleanValue()).isFalse();
        assertThat(r.get("pedido").get("podeProsseguir").booleanValue()).isTrue();
        saldo("10", "0", "10", "0");
    }

    @Test
    void bobinaNaoPodeSerFracionadaParaForcarFechamentoDoPedido() throws Exception {
        var u = bobina("6", FIFO, "BOBINA");
        var p = criar(produtoId, "4", operador);
        resposta(post(rota(p, "/justificativas-fifo"), justificar(p, u, "4"), supervisor), 400);
        resposta(post(rota(p, "/reserva"), reservar(p), operador), 409);
        assertThat(contar("reserva_saida")).isZero();
        saldo("6", "6", "0", "0");
    }

    @Test
    void encerramentoDeEnderecoAguardaCommitDaReservaDepoisDeLerCandidatas() throws Exception {
        var u = unidade("10");
        var p = criar(produtoId, "4", operador);
        var dados = reservar(p);
        Long enderecoId =
                jdbc.queryForObject(
                        "select endereco_id from wms.ocupacao_endereco where unidade_id=?",
                        Long.class,
                        u.id());
        long versao =
                resposta(get("/api/v1/enderecos/" + enderecoId, gestor), 200)
                        .get("versao")
                        .longValue();
        var candidatasLidas = new CountDownLatch(1);
        var concluirReserva = new CountDownLatch(1);
        var encerramentoIniciado = new CountDownLatch(1);
        EstoqueService estoqueTarget = AopTestUtils.getUltimateTargetObject(estoqueService);
        EnderecoService enderecoTarget = AopTestUtils.getUltimateTargetObject(enderecoService);
        doAnswer(
                        inv -> {
                            var candidatas = inv.callRealMethod();
                            candidatasLidas.countDown();
                            if (!concluirReserva.await(10, TimeUnit.SECONDS))
                                throw new IllegalStateException("Teste não liberou reserva");
                            return candidatas;
                        })
                .when(estoqueTarget)
                .candidatas(eq(clienteId), eq(armazemId), anyList());
        doAnswer(
                        inv -> {
                            encerramentoIniciado.countDown();
                            return inv.callRealMethod();
                        })
                .when(enderecoTarget)
                .encerrar(eq(enderecoId), any(RevisaoCadastroRequest.class));
        try (var executor = Executors.newFixedThreadPool(2)) {
            var reserva = executor.submit(() -> post(rota(p, "/reserva"), dados, operador));
            assertThat(candidatasLidas.await(10, TimeUnit.SECONDS)).isTrue();
            var encerramento =
                    executor.submit(
                            () ->
                                    post(
                                            "/api/v1/enderecos/" + enderecoId + "/encerramento",
                                            Map.of(
                                                    "versao",
                                                    versao,
                                                    "motivo",
                                                    "Encerramento ficticio da posicao"),
                                            gestor));
            assertThat(encerramentoIniciado.await(10, TimeUnit.SECONDS)).isTrue();
            try {
                assertThatThrownBy(() -> encerramento.get(300, TimeUnit.MILLISECONDS))
                        .isInstanceOf(TimeoutException.class);
            } finally {
                concluirReserva.countDown();
            }
            resposta(reserva.get(10, TimeUnit.SECONDS), 200);
            resposta(encerramento.get(10, TimeUnit.SECONDS), 200);
        } finally {
            concluirReserva.countDown();
            reset(estoqueTarget, enderecoTarget);
        }
        var atual = resposta(get(rota(p, ""), operador), 200);
        assertThat(atual.get("unidadesImpedidas").get(0).longValue()).isEqualTo(u.id());
        assertThat(atual.get("podeProsseguir").booleanValue()).isFalse();
        assertThat(atual.get("reservas").get(0).get("situacao").asString()).isEqualTo("ATIVA");
        saldo("10", "0", "4", "6");
    }

    @Test
    void d30DezBobinasFisicasReservamTresIdentidadesInteiras() throws Exception {
        var bobinas = new java.util.ArrayList<Unidade>();
        for (int i = 0; i < 10; i++) bobinas.add(bobina("1", FIFO.plusSeconds(i), "D30-B" + i));
        assertThat(bobinas.stream().map(Unidade::id).distinct().count()).isEqualTo(10);
        assertThat(bobinas.stream().map(Unidade::codigo).distinct().count()).isEqualTo(10);
        var origens = D30FotografiaFisica.capturar(jdbc);
        var p = criar(produtoId, "3", operador);
        var esperado = bobinas.subList(0, 3).stream().map(Unidade::id).toList();
        var fifo = resposta(get(rota(p, "/fifo"), operador), 200).get("selecoes");
        assertThat(fifo.size()).isEqualTo(3);
        for (int i = 0; i < 3; i++) {
            assertThat(fifo.get(i).get("unidadeId").longValue()).isEqualTo(esperado.get(i));
            assertThat(fifo.get(i).get("quantidade").decimalValue()).isEqualByComparingTo("1");
        }
        resposta(post(rota(p, "/reserva"), reservar(p), operador), 200);
        assertThat(
                        jdbc.queryForList(
                                "select unidade_id from wms.reserva_saida where situacao='ATIVA' order by unidade_id",
                                Long.class))
                .containsExactlyElementsOf(esperado);
        assertThat(
                        jdbc.queryForObject(
                                "select count(*) from wms.reserva_saida where quantidade=1 and situacao='ATIVA'",
                                Integer.class))
                .isEqualTo(3);
        assertThat(
                        jdbc.queryForObject(
                                "select count(*) from wms.unidade_logistica where tipo='BOBINA' and quantidade=1 and ativa=true",
                                Integer.class))
                .isEqualTo(10);
        saldo("10", "7", "3", "0");
        var depois = D30FotografiaFisica.capturar(jdbc);
        for (String tabela :
                List.of(
                        "NOTA_ENTRADA",
                        "ITEM_NOTA_ENTRADA",
                        "ENTRADA_CONFERIDA",
                        "CONTEUDO_UNIDADE",
                        "OCUPACAO_ENDERECO"))
            assertThat(depois.get(tabela)).as(tabela).isEqualTo(origens.get(tabela));
    }

    private Unidade bobina(String q, Instant fifo, String codigo) {
        var u =
                unidade(
                        produtoId,
                        q,
                        fifo,
                        0,
                        1,
                        codigo,
                        TipoEndereco.ARMAZENAGEM,
                        CondicaoMercadoria.BOA);
        jdbc.update("update wms.unidade_logistica set tipo='BOBINA' where id=?", u.id());
        jdbc.update(
                "update wms.endereco set tipo_unidade_permitido='BOBINA' where id=(select endereco_id from wms.ocupacao_endereco where unidade_id=?)",
                u.id());
        return u;
    }

    private Unidade unidade(String quantidade) {
        return unidade(
                produtoId,
                quantidade,
                FIFO,
                0,
                1,
                "A" + (++numero),
                TipoEndereco.ARMAZENAGEM,
                CondicaoMercadoria.BOA);
    }

    private Unidade unidade(
            Long produtoId,
            String quantidade,
            Instant fifo,
            int nivel,
            int sequencia,
            String codigo,
            TipoEndereco tipo,
            CondicaoMercadoria condicao) {
        return new TransactionTemplate(transactions)
                .execute(
                        s -> {
                            var agora = Instant.now();
                            var cliente = em.find(Cliente.class, clienteId);
                            var armazem = em.find(Armazem.class, armazemId);
                            var produto = em.find(Produto.class, produtoId);
                            var embalagem =
                                    em.createQuery(
                                                    "select e from Embalagem e where e.produto.id=:id",
                                                    Embalagem.class)
                                            .setParameter("id", produtoId)
                                            .getSingleResult();
                            var pedido =
                                    new PedidoEntrada(
                                            cliente, armazem, "ENTRADA-" + (++numero), agora);
                            pedido.concluir("Entrada ficticia conferida", agora);
                            em.persist(pedido);
                            var nota =
                                    new NotaEntrada(
                                            pedido,
                                            "12345678000199",
                                            1,
                                            numero,
                                            LocalDate.of(2026, 9, 1),
                                            null);
                            em.persist(nota);
                            var q = new BigDecimal(quantidade);
                            var item = new ItemNotaEntrada(nota, 1, produto, q, null);
                            em.persist(item);
                            var chegada =
                                    new ChegadaRecebimento(
                                            pedido,
                                            UUID.randomUUID().toString(),
                                            "a".repeat(64),
                                            fifo.plusSeconds(3600),
                                            agora,
                                            "fixture",
                                            "Conferido");
                            em.persist(chegada);
                            var fisico =
                                    new ItemChegada(
                                            chegada,
                                            1,
                                            item,
                                            null,
                                            null,
                                            condicao == CondicaoMercadoria.BOA
                                                    ? q
                                                    : BigDecimal.ZERO,
                                            condicao == CondicaoMercadoria.AVARIADA
                                                    ? q
                                                    : BigDecimal.ZERO);
                            em.persist(fisico);
                            var entrada =
                                    new EntradaConferida(
                                            fisico,
                                            fifo,
                                            agora,
                                            condicao == CondicaoMercadoria.BOA
                                                    ? q
                                                    : BigDecimal.ZERO,
                                            condicao == CondicaoMercadoria.AVARIADA
                                                    ? q
                                                    : BigDecimal.ZERO);
                            entrada.marcarUnitizada(agora);
                            em.persist(entrada);
                            var u =
                                    new UnidadeLogistica(
                                            entrada,
                                            embalagem,
                                            TipoUnidadeLogistica.PALLET,
                                            condicao,
                                            q,
                                            agora);
                            em.persist(u);
                            em.persist(new ConteudoUnidade(u, entrada, q));
                            var endereco =
                                    new Endereco(
                                            armazem,
                                            codigo,
                                            "R" + numero,
                                            nivel,
                                            "P" + numero,
                                            "Posicao ficticia",
                                            tipo,
                                            new BigDecimal("1000"),
                                            new BigDecimal("2"),
                                            new BigDecimal("2"),
                                            new BigDecimal("2"),
                                            2,
                                            sequencia,
                                            agora);
                            endereco.configurarFisico(
                                    TipoUnidadeLogistica.PALLET,
                                    new BigDecimal("1000"),
                                    new BigDecimal("2"),
                                    new BigDecimal("2"),
                                    new BigDecimal("2"),
                                    2,
                                    agora);
                            em.persist(endereco);
                            var ocupacao = new OcupacaoEndereco(endereco);
                            ocupacao.atribuir(u);
                            em.persist(ocupacao);
                            u.posicionar(
                                    new MedidasUnidade(
                                            new BigDecimal("500"),
                                            BigDecimal.ONE,
                                            BigDecimal.ONE,
                                            BigDecimal.ONE,
                                            1,
                                            1),
                                    tipo,
                                    null,
                                    agora);
                            em.flush();
                            return new Unidade(u.getId(), u.getCodigo(), nota.getId());
                        });
    }

    private Map<String, Object> item(Long id, String q) {
        return Map.of("produtoId", id, "quantidade", new BigDecimal(q));
    }

    @ParameterizedTest
    @ValueSource(ints = {100, 101})
    void d30LimiteSkuDistintoExigeEstoqueValidoSemReservar(int total) throws Exception {
        var itens = new ArrayList<Map<String, Object>>();
        for (int i = 0; i < total; i++) {
            int indice = i;
            long produto =
                    new TransactionTemplate(transactions)
                            .execute(
                                    s -> {
                                        var p =
                                                new Produto(
                                                        em.find(Cliente.class, clienteId),
                                                        "D30-SKU-" + indice,
                                                        "Fronteira ficticia D30",
                                                        "UN",
                                                        TipoQuantidade.CONTAGEM,
                                                        0,
                                                        false,
                                                        false,
                                                        null,
                                                        Instant.now());
                                        em.persist(p);
                                        em.persist(
                                                new Embalagem(
                                                        p,
                                                        "D30-DUN-" + indice,
                                                        "Embalagem D30",
                                                        BigDecimal.ONE,
                                                        Instant.now()));
                                        em.flush();
                                        return p.getId();
                                    });
            unidade(
                    produto,
                    "1",
                    FIFO,
                    0,
                    1,
                    "D30-SKU-END-" + i,
                    TipoEndereco.ARMAZENAGEM,
                    CondicaoMercadoria.BOA);
            itens.add(item(produto, "1"));
        }
        var antes = D30FotografiaFisica.capturar(jdbc);
        var retorno = post("/api/v1/pedidos-saida", criarDados(itens), operador);
        if (total == 101) {
            resposta(retorno, 400);
            assertThat(D30FotografiaFisica.capturar(jdbc)).isEqualTo(antes);
            assertThat(contar("pedido_saida")).isZero();
        } else {
            var p = resposta(retorno, 201).get("pedido");
            assertThat(p.get("itens").size()).isEqualTo(100);
            assertThat(p.get("situacao").asString()).isEqualTo("RASCUNHO");
            var ids = new ArrayList<Long>();
            for (var it : p.get("itens")) {
                ids.add(it.get("produtoId").longValue());
                assertThat(it.get("quantidade").decimalValue()).isEqualByComparingTo("1");
            }
            assertThat(ids).doesNotHaveDuplicates();
            assertThat(contar("reserva_saida")).isZero();
        }
        var depois = D30FotografiaFisica.capturar(jdbc);
        for (String tabela :
                List.of(
                        "UNIDADE_LOGISTICA",
                        "CONTEUDO_UNIDADE",
                        "OCUPACAO_ENDERECO",
                        "MOVIMENTO_ESTOQUE",
                        "NOTA_ENTRADA",
                        "ITEM_NOTA_ENTRADA"))
            assertThat(depois.get(tabela)).isEqualTo(antes.get(tabela));
    }

    @ParameterizedTest
    @ValueSource(ints = {500, 501})
    void d30SelecaoJustificadaLimitaUnidadesDistintasSemReservaOuBaixa(int total) throws Exception {
        var selecoes = new ArrayList<Map<String, Object>>();
        for (int i = 0; i < total; i++) {
            var u = unidade("1");
            selecoes.add(Map.of("unidadeId", u.id(), "quantidade", BigDecimal.ONE));
        }
        var p = criar(produtoId, String.valueOf(total), operador);
        var comando = comando(p);
        comando.put("selecoes", selecoes);
        comando.put("motivo", "Selecao integral ficticia na fronteira D30");
        var antes = D30FotografiaFisica.capturar(jdbc);
        var retorno = post(rota(p, "/justificativas-fifo"), comando, supervisor);
        if (total == 501) {
            resposta(retorno, 400);
            assertThat(D30FotografiaFisica.capturar(jdbc)).isEqualTo(antes);
        } else {
            var confirmado = resposta(retorno, 200);
            assertThat(confirmado.get("selecoes").size()).isEqualTo(500);
            assertThat(confirmado.get("pedido").get("situacao").asString()).isEqualTo("RASCUNHO");
            var ids = new ArrayList<Long>();
            BigDecimal soma = BigDecimal.ZERO;
            for (var selecao : confirmado.get("selecoes")) {
                ids.add(selecao.get("unidadeId").longValue());
                soma = soma.add(selecao.get("quantidade").decimalValue());
            }
            assertThat(ids).doesNotHaveDuplicates();
            assertThat(soma).isEqualByComparingTo("500");
            assertThat(confirmado.get("justificadaPor").asString()).isEqualTo("saida-SUPERVISOR");
        }
        assertThat(contar("reserva_saida")).isZero();
        var depois = D30FotografiaFisica.capturar(jdbc);
        for (String tabela :
                List.of(
                        "UNIDADE_LOGISTICA",
                        "CONTEUDO_UNIDADE",
                        "OCUPACAO_ENDERECO",
                        "MOVIMENTO_ESTOQUE",
                        "NOTA_ENTRADA",
                        "ITEM_NOTA_ENTRADA"))
            assertThat(depois.get(tabela)).isEqualTo(antes.get(tabela));
    }

    @ParameterizedTest
    @ValueSource(strings = {"CLIENTE", "ARMAZEM", "PRODUTO"})
    void d30CadastroInativoRecusaCriacaoAntesDeConsultarSaldo(String tipo) throws Exception {
        long id =
                tipo.equals("CLIENTE") ? clienteId : tipo.equals("ARMAZEM") ? armazemId : produtoId;
        String rota = "/api/v1/encerramentos/" + tipo + "/" + id;
        var atual = resposta(get(rota + "/impedimentos", gestor), 200);
        assertThat(atual.get("impedimentos").size()).isZero();
        var solicitar =
                new HashMap<String, Object>(
                        Map.of(
                                "operacaoId",
                                UUID.randomUUID(),
                                "versao",
                                atual.get("versao").longValue(),
                                "motivo",
                                "Inativacao legitima de cadastro vazio D30"));
        var pendente = resposta(post(rota + "/solicitar", solicitar, gestor), 200);
        solicitar.put("operacaoId", UUID.randomUUID());
        solicitar.put("versao", pendente.get("versao").longValue());
        assertThat(
                        resposta(post(rota + "/inativar", solicitar, gestor), 200)
                                .get("situacao")
                                .asString())
                .isEqualTo("INATIVO");
        var antes = D30FotografiaFisica.capturar(jdbc);
        var recusado =
                resposta(
                        post(
                                "/api/v1/pedidos-saida",
                                criarDados(List.of(item(produtoId, "1"))),
                                operador),
                        409);
        assertThat(recusado.get("codigo").asString()).isEqualTo("CADASTRO_EM_ENCERRAMENTO");
        assertThat(D30FotografiaFisica.capturar(jdbc)).isEqualTo(antes);
        assertThat(contar("pedido_saida")).isZero();
    }

    private Map<String, Object> criarDados(List<Map<String, Object>> itens) {
        return new HashMap<>(
                Map.of(
                        "operacaoId",
                        UUID.randomUUID(),
                        "clienteId",
                        clienteId,
                        "armazemId",
                        armazemId,
                        "referencia",
                        "SAIDA-" + (++numero),
                        "itens",
                        itens,
                        "motivo",
                        "Pedido ficticio solicitado"));
    }

    private JsonNode criar(Long produtoId, String q, String token) throws Exception {
        return criarItens(List.of(item(produtoId, q)), token);
    }

    private JsonNode criarItens(List<Map<String, Object>> itens, String token) throws Exception {
        var antes = D30FotografiaFisica.capturar(jdbc);
        var dados = criarDados(itens);
        var confirmacao = resposta(post("/api/v1/pedidos-saida", dados, token), 201);
        var pedido = confirmacao.get("pedido");
        assertThat(pedido.get("situacao").asString()).isEqualTo("RASCUNHO");
        d30CriacaoNaoMovimentaFisico(antes, dados, confirmacao);
        return pedido;
    }

    private void d30CriacaoNaoMovimentaFisico(
            Map<String, List<Map<String, Object>>> antes,
            Map<String, Object> dados,
            JsonNode confirmacao)
            throws Exception {
        var depois = D30FotografiaFisica.capturar(jdbc);
        for (String tabela : antes.keySet())
            if (!List.of(
                            "PEDIDO_SAIDA",
                            "ITEM_PEDIDO_SAIDA",
                            "OPERACAO_SAIDA",
                            "AUDITORIA_CADASTRO",
                            "OPERACAO_ADMINISTRATIVA")
                    .contains(tabela))
                assertThat(depois.get(tabela))
                        .as("Criacao valida conserva " + tabela)
                        .isEqualTo(antes.get(tabela));
        assertThat(depois.get("OPERACAO_SAIDA")).containsAll(antes.get("OPERACAO_SAIDA"));
        var novas =
                depois.get("OPERACAO_SAIDA").stream()
                        .filter(l -> !antes.get("OPERACAO_SAIDA").contains(l))
                        .toList();
        assertThat(novas).hasSize(1);
        var op = novas.getFirst();
        assertThat(op.get("TIPO")).isEqualTo("CRIACAO_SAIDA");
        if (dados == null) {
            assertThat(op.get("OPERACAO_ID")).isEqualTo(confirmacao.get("operacaoId").asString());
            assertThat(((Number) op.get("PEDIDO_ID")).longValue())
                    .isEqualTo(confirmacao.get("pedido").get("id").longValue());
            assertThat(mapper.readTree((String) op.get("RESULTADO")).get("pedido"))
                    .isEqualTo(confirmacao.get("pedido"));
            return;
        }
        assertThat(op.get("OPERACAO_ID")).isEqualTo(dados.get("operacaoId").toString());
        assertThat(((Number) op.get("PEDIDO_ID")).longValue())
                .isEqualTo(confirmacao.get("pedido").get("id").longValue());
        assertThat(op.get("TIPO")).isEqualTo("CRIACAO_SAIDA");
        assertThat(op.get("MOTIVO")).isEqualTo(dados.get("motivo"));
        var dto = mapper.convertValue(dados, br.com.rodogarcia.wms.dto.PedidoSaidaDto.Criar.class);
        String hashEsperado =
                java.util.HexFormat.of()
                        .formatHex(
                                java.security.MessageDigest.getInstance("SHA-256")
                                        .digest(
                                                ("CRIACAO_SAIDA:" + mapper.writeValueAsString(dto))
                                                        .getBytes(
                                                                java.nio.charset.StandardCharsets
                                                                        .UTF_8)));
        assertThat(op.get("CONTEUDO_HASH")).isEqualTo(hashEsperado);
        assertThat(mapper.readTree((String) op.get("RESULTADO"))).isEqualTo(confirmacao);
    }

    @Test
    void d30FifoExcluiQuarentenaMaisAntigaSemQualquerEfeitoFisico() throws Exception {
        var antiga =
                unidade(
                        produtoId,
                        "10",
                        FIFO.minusSeconds(86400),
                        0,
                        0,
                        "D30-ANTIGA-Q",
                        TipoEndereco.QUARENTENA,
                        CondicaoMercadoria.BOA);
        var disponivel =
                unidade(
                        produtoId,
                        "10",
                        FIFO,
                        4,
                        90,
                        "D30-POSTERIOR-A",
                        TipoEndereco.ARMAZENAGEM,
                        CondicaoMercadoria.BOA);
        var p = criar(produtoId, "4", operador);
        var antes = D30FotografiaFisica.capturar(jdbc);
        var s = resposta(get(rota(p, "/fifo"), operador), 200).get("selecoes");
        assertThat(s.size()).isEqualTo(1);
        assertThat(s.get(0).get("unidadeId").longValue())
                .isEqualTo(disponivel.id())
                .isNotEqualTo(antiga.id());
        assertThat(s.get(0).get("quantidade").decimalValue()).isEqualByComparingTo("4");
        assertThat(D30FotografiaFisica.capturar(jdbc)).isEqualTo(antes);
        assertThat(contar("reserva_saida")).isZero();
        assertThat(contar("separacao_saida")).isZero();
        assertThat(contar("retirada_saida")).isZero();
    }

    private String rota(JsonNode p, String sufixo) {
        return SAIDAS + p.get("id").longValue() + sufixo;
    }

    private String unidadeRota(Unidade u, String sufixo) {
        return "/api/v1/unidades-logisticas/" + u.codigo() + sufixo;
    }

    private Map<String, Object> reservar(JsonNode p) {
        return comando(p);
    }

    private Map<String, Object> comando(JsonNode p) {
        return new HashMap<>(
                Map.of(
                        "operacaoId",
                        UUID.randomUUID(),
                        "versao",
                        p.get("versao").longValue(),
                        "motivo",
                        "Operacao ficticia solicitada"));
    }

    private Map<String, Object> justificar(JsonNode p, Unidade u, String q) {
        var dados = comando(p);
        dados.put(
                "selecoes", List.of(Map.of("unidadeId", u.id(), "quantidade", new BigDecimal(q))));
        dados.put("motivo", "Cliente solicita origem especifica");
        return dados;
    }

    private Map<String, Object> bloqueio(Unidade u) throws Exception {
        long versao =
                resposta(get(unidadeRota(u, "/estoque"), operador), 200)
                        .get("unidade")
                        .get("versao")
                        .longValue();
        return Map.of(
                "operacaoId",
                UUID.randomUUID(),
                "versaoUnidade",
                versao,
                "motivo",
                "Ocorrencia fisica constatada");
    }

    private void saldo(String fisico, String disponivel, String reservado, String bloqueado)
            throws Exception {
        saldo(produtoId, fisico, disponivel, reservado, bloqueado);
    }

    private void saldo(
            Long produto, String fisico, String disponivel, String reservado, String bloqueado)
            throws Exception {
        var saldo =
                resposta(
                        get(
                                "/api/v1/estoque/saldo?clienteId="
                                        + clienteId
                                        + "&armazemId="
                                        + armazemId
                                        + "&produtoId="
                                        + produto,
                                operador),
                        200);
        assertThat(saldo.get("fisicoTotal").decimalValue()).isEqualByComparingTo(fisico);
        assertThat(saldo.get("disponivel").decimalValue()).isEqualByComparingTo(disponivel);
        assertThat(saldo.get("reservado").decimalValue()).isEqualByComparingTo(reservado);
        assertThat(saldo.get("bloqueado").decimalValue()).isEqualByComparingTo(bloqueado);
        assertThat(saldo.get("fisicoUnitizado").decimalValue())
                .isEqualByComparingTo(
                        saldo.get("disponivel")
                                .decimalValue()
                                .add(saldo.get("reservado").decimalValue())
                                .add(saldo.get("bloqueado").decimalValue()));
    }

    private int contar(String tabela) {
        return jdbc.queryForObject("select count(*) from wms." + tabela, Integer.class);
    }

    private void assertCodigo(HttpResponse<String> response, int status, String codigo) {
        assertThat(resposta(response, status).get("codigo").asString()).isEqualTo(codigo);
    }

    private HttpResponse<String> get(String rota, String token) throws Exception {
        return enviar("GET", rota, null, token);
    }

    private HttpResponse<String> post(String rota, Object dados, String token) throws Exception {
        return enviar("POST", rota, dados, token);
    }

    private HttpResponse<String> enviar(String metodo, String rota, Object dados, String token)
            throws Exception {
        var request =
                HttpRequest.newBuilder(
                                URI.create(
                                        "http://127.0.0.1:"
                                                + environment.getRequiredProperty(
                                                        "local.server.port")
                                                + rota))
                        .timeout(Duration.ofSeconds(30))
                        .header("Content-Type", "application/json");
        if (token != null) request.header("Authorization", "Bearer " + token);
        return http.send(
                request.method(
                                metodo,
                                dados == null
                                        ? HttpRequest.BodyPublishers.noBody()
                                        : HttpRequest.BodyPublishers.ofString(
                                                mapper.writeValueAsString(dados)))
                        .build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private JsonNode resposta(HttpResponse<String> response, int status) {
        assertThat(response.statusCode()).as(response.body()).isEqualTo(status);
        return mapper.readTree(response.body());
    }

    private List<Integer> paralelo(
            Callable<HttpResponse<String>> a, Callable<HttpResponse<String>> b) throws Exception {
        var inicio = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var primeira =
                    executor.submit(
                            () -> {
                                inicio.await();
                                return a.call().statusCode();
                            });
            var segunda =
                    executor.submit(
                            () -> {
                                inicio.await();
                                return b.call().statusCode();
                            });
            inicio.countDown();
            return List.of(primeira.get(30, TimeUnit.SECONDS), segunda.get(30, TimeUnit.SECONDS));
        }
    }

    private String token(String perfil, List<Long> clientes, List<Long> armazens) {
        var agora = Instant.now();
        var claims =
                JwtClaimsSet.builder()
                        .issuer("https://identidade.test.invalid")
                        .subject("saida-" + perfil)
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
