package br.com.rodogarcia.wms;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.reset;

import br.com.rodogarcia.wms.config.IdentidadeTesteConfig;
import br.com.rodogarcia.wms.config.RelogioExpedicaoTesteConfig;
import br.com.rodogarcia.wms.dto.AvariaDto;
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
import br.com.rodogarcia.wms.services.AvariaService;
import br.com.rodogarcia.wms.services.EnderecoService;
import br.com.rodogarcia.wms.services.EstoqueService;
import br.com.rodogarcia.wms.services.MovimentoSaidaEstoqueService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.validation.ConstraintViolationException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
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
                "spring.datasource.url=jdbc:h2:mem:wms-expedicao;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000;INIT=CREATE SCHEMA IF NOT EXISTS wms")
@Import({IdentidadeTesteConfig.class, RelogioExpedicaoTesteConfig.class})
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ExpedicaoIntegrationTest {
    private static final String SAIDAS = "/api/v1/pedidos-saida/";
    private static final Instant FIFO = Instant.parse("2026-09-01T12:00:00Z");
    private final HttpClient http =
            HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    @Autowired private Environment environment;
    @Autowired private Clock clock;
    @Autowired private JsonMapper mapper;
    @Autowired private JwtEncoder encoder;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private PlatformTransactionManager transactions;
    @MockitoSpyBean private EstoqueService estoqueService;
    @MockitoSpyBean private EnderecoService enderecoService;
    @MockitoSpyBean private AvariaService avariaService;
    @MockitoSpyBean private MovimentoSaidaEstoqueService movimentoSaidaService;
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
                        "revisao_contagem",
                        "contagem_estoque",
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
                            var agora = clock.instant();
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

    @Test
    void parcialDePalletConservaOrigemEtiquetaEquivalenciaEFisicoAteRetirada() throws Exception {
        var u = unidade("10");
        long origem = enderecoAtual(u);
        var etiqueta = resposta(get(unidadeRota(u, "/etiqueta"), operador), 200);
        var p = reservado(u, "4");
        p = separar(p, u, endereco(TipoEndereco.SEPARACAO));
        saldo("10", "0", "4", "6");
        assertThat(
                        resposta(get(unidadeRota(u, "/estoque"), operador), 200)
                                .get("posicoesEquivalentes")
                                .intValue())
                .isEqualTo(1);
        var x = xmlSaida(101, "SKU", "4");
        p = documentar(p, u, x, "RETORNO_MERCADORIA");
        saldo("10", "0", "4", "6");
        var comando = retirada(p, List.of(x), List.of(destinacao(p, u, origem)));
        var d30ReservasAntes = D30ReservaFotografia.capturar(jdbc, p.get("id").longValue());
        D30ReservaFotografia.exigirAtivas(d30ReservasAntes);
        var d30Inicio = clock.instant();
        var resultado = resposta(post(rota(p, "/retirada"), comando, supervisor), 200);
        var d30Termino = clock.instant();
        var d30ReservasDepois = D30ReservaFotografia.capturar(jdbc, p.get("id").longValue());
        D30ReservaFotografia.exigirEncerradas(
                d30ReservasAntes, d30ReservasDepois, "RETIRADA", d30Inicio, d30Termino);
        assertThat(resultado.get("expedicao").get("pedido").get("situacao").asString())
                .isEqualTo("RETIRADO");
        assertThat(resposta(post(rota(p, "/retirada"), comando, supervisor), 200))
                .isEqualTo(resultado);
        assertThat(D30ReservaFotografia.capturar(jdbc, p.get("id").longValue()))
                .isEqualTo(d30ReservasDepois);
        saldo("6", "6", "0", "0");
        var nova = resposta(get(unidadeRota(u, "/etiqueta"), operador), 200);
        assertThat(nova.get("codigoLeitura")).isEqualTo(etiqueta.get("codigoLeitura"));
        assertThat(nova.get("dataEntrada")).isEqualTo(etiqueta.get("dataEntrada"));
        assertThat(nova.get("notaId")).isEqualTo(etiqueta.get("notaId"));
        assertThat(nova.get("quantidadeProduto").decimalValue()).isEqualByComparingTo("6");
        assertThat(nova.get("versaoConteudo").longValue())
                .isEqualTo(etiqueta.get("versaoConteudo").longValue() + 1);
        assertThat(
                        jdbc.queryForObject(
                                "select sum(quantidade) from wms.conteudo_unidade where unidade_id=?",
                                BigDecimal.class,
                                u.id()))
                .isEqualByComparingTo("6");
        var fatos = resultado.get("expedicao").get("fatos");
        var d30AntesFatos = D30FotografiaFisica.capturar(jdbc);
        var d30Fatos = resposta(get(rota(p, "/fatos"), operador), 200);
        assertThat(d30Fatos.size()).isEqualTo(2);
        assertThat(d30Fatos.get(0).get("tipo").asString()).isEqualTo("SEPARACAO");
        assertThat(d30Fatos.get(1).get("tipo").asString()).isEqualTo("RETIRADA");
        for (var f : d30Fatos) {
            assertThat(f.get("unidadeId").longValue()).isEqualTo(u.id());
            assertThat(f.get("quantidadeAntes").decimalValue()).isEqualByComparingTo("10");
            assertThat(f.get("equivalenciaAntes").decimalValue()).isEqualByComparingTo("1");
            assertThat(f.get("equivalenciaDepois").decimalValue()).isEqualByComparingTo("1");
        }
        assertThat(d30Fatos.get(0).get("quantidadeDepois").decimalValue())
                .isEqualByComparingTo("10");
        assertThat(d30Fatos.get(1).get("quantidadeDepois").decimalValue())
                .isEqualByComparingTo("6");
        assertThat(D30FotografiaFisica.capturar(jdbc)).isEqualTo(d30AntesFatos);
        assertThat(fatos.get(1).get("equivalenciaDepois").decimalValue()).isEqualByComparingTo("1");
        assertThat(enderecoAtual(u)).isEqualTo(origem);
        comando.put("motivo", "Payload diferente da retirada");
        assertCodigo(post(rota(p, "/retirada"), comando, supervisor), 409, "OPERACAO_REUTILIZADA");
    }

    @ParameterizedTest
    @ValueSource(strings = {"OPERACAO", "SUPERVISOR", "GESTOR"})
    void todosOsPerfisLeemESeparamMasOperacaoNaoRetira(String perfil) throws Exception {
        var u = unidade("4");
        var p = reservado(u, "4");
        String identidade = token(perfil, List.of(clienteId), List.of(armazemId));
        var leitura = leitura(p, u);
        var l = resposta(post(rota(p, "/leituras"), leitura, identidade), 200);
        assertThat(resposta(post(rota(p, "/leituras"), leitura, identidade), 200)).isEqualTo(l);
        p = l.get("expedicao").get("pedido");
        var separar = comando(p);
        separar.put("destinacao", destinacao(p, u, endereco(TipoEndereco.SEPARACAO)));
        var s = resposta(post(rota(p, "/separacoes"), separar, identidade), 200);
        assertThat(resposta(post(rota(p, "/separacoes"), separar, identidade), 200)).isEqualTo(s);
        p = s.get("expedicao").get("pedido");
        var x = xmlSaida(102, "SKU", "4");
        p = documentar(p, u, x, "RETORNO_MERCADORIA");
        resposta(post(rota(p, "/retirada"), retirada(p, List.of(x), List.of()), operador), 403);
        var fora = token("SUPERVISOR", List.of(clienteId + 99), List.of(armazemId));
        resposta(get(rota(p, "/expedicao"), fora), 403);
        resposta(post(rota(p, "/retirada"), retirada(p, List.of(x), List.of()), fora), 403);
        resposta(post(rota(p, "/retirada"), retirada(p, List.of(x), List.of()), supervisor), 200);
        saldo("0", "0", "0", "0");
    }

    @Test
    void leituraEtiquetaPosicaoECapacidadeSaoRevalidadas() throws Exception {
        var u = unidade("4");
        var p = reservado(u, "4");
        var dados = leitura(p, u);
        dados.put("revisaoConteudo", 99);
        assertCodigo(post(rota(p, "/leituras"), dados, operador), 409, "ETIQUETA_INVALIDA");
        dados.put("revisaoConteudo", 0);
        dados.put("codigoLido", UUID.randomUUID());
        assertCodigo(post(rota(p, "/leituras"), dados, operador), 409, "ETIQUETA_INVALIDA");
        var destino = endereco(TipoEndereco.SEPARACAO);
        var s = comando(p);
        s.put("destinacao", destinacao(p, u, destino));
        assertCodigo(post(rota(p, "/separacoes"), s, operador), 409, "LEITURA_NECESSARIA");
        p =
                resposta(post(rota(p, "/leituras"), leitura(p, u), operador), 200)
                        .get("expedicao")
                        .get("pedido");
        jdbc.update("update wms.endereco set capacidade_peso_kg=1 where id=?", destino);
        s = comando(p);
        s.put("destinacao", destinacao(p, u, destino));
        resposta(post(rota(p, "/separacoes"), s, operador), 409);
        assertThat(contar("fato_permanencia")).isZero();
        saldo("4", "0", "4", "0");
    }

    @Test
    void multiitemSoRetiraInteiroComVariasNotasEXmls() throws Exception {
        var a = unidade("4");
        var b =
                unidade(
                        segundoProdutoId,
                        "2",
                        FIFO,
                        0,
                        1,
                        "B",
                        TipoEndereco.ARMAZENAGEM,
                        CondicaoMercadoria.BOA);
        var p = criarItens(List.of(item(produtoId, "4"), item(segundoProdutoId, "2")), operador);
        p = resposta(post(rota(p, "/reserva"), reservar(p), operador), 200).get("pedido");
        p = separar(p, a, endereco(TipoEndereco.SEPARACAO));
        assertThat(p.get("situacao").asString()).isEqualTo("EM_SEPARACAO");
        resposta(
                post(
                        rota(p, "/retirada"),
                        retirada(p, List.of(xmlSaida(104, "SKU", "4")), List.of()),
                        supervisor),
                409);
        assertThat(contar("retirada_saida")).isZero();
        p = separar(p, b, endereco(TipoEndereco.SEPARACAO));
        var xa = xmlSaida(104, "SKU", "4");
        var xb = xmlSaida(105, "SKU2", "2");
        p = documentar(p, a, xa, "RETORNO_MERCADORIA");
        p = documentar(p, b, xb, "RETORNO_MERCADORIA");
        resposta(post(rota(p, "/retirada"), retirada(p, List.of(xa), List.of()), supervisor), 409);
        assertThat(contar("retirada_saida")).isZero();
        resposta(
                post(rota(p, "/retirada"), retirada(p, List.of(xa, xb), List.of()), supervisor),
                200);
        assertThat(contar("baixa_saida")).isEqualTo(2);
        assertThat(
                        jdbc.queryForObject(
                                "select sum(quantidade) from wms.unidade_logistica",
                                BigDecimal.class))
                .isEqualByComparingTo("0");
        assertThat(
                        jdbc.queryForObject(
                                "select count(*) from wms.ocupacao_endereco where unidade_id is not null",
                                Integer.class))
                .isZero();
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "INCOMPLETA",
                "SIMBOLICA",
                "DIVERGENTE",
                "AVARIA",
                "BLOQUEIO",
                "ENCERRAMENTO"
            })
    void impedimentosNaoBaixamNemLiberamReserva(String causa) throws Exception {
        var u = unidade("4");
        var p = separar(reservado(u, "4"), u, endereco(TipoEndereco.SEPARACAO));
        var x = xmlSaida(106, "SKU", "4");
        if (!causa.equals("INCOMPLETA"))
            p =
                    documentar(
                            p,
                            u,
                            x,
                            causa.equals("SIMBOLICA") ? "RETORNO_SIMBOLICO" : "RETORNO_MERCADORIA");
        if (causa.equals("AVARIA"))
            resposta(post(unidadeRota(u, "/avaria"), bloqueio(u), operador), 200);
        if (causa.equals("BLOQUEIO"))
            resposta(post(unidadeRota(u, "/bloqueio"), bloqueio(u), operador), 200);
        if (causa.equals("ENCERRAMENTO")) {
            long id = enderecoAtual(u);
            var e = resposta(get("/api/v1/enderecos/" + id, gestor), 200);
            resposta(
                    post(
                            "/api/v1/enderecos/" + id + "/encerramento",
                            Map.of(
                                    "versao",
                                    e.get("versao").longValue(),
                                    "motivo",
                                    "Encerramento ficticio"),
                            gestor),
                    200);
        }
        if (causa.equals("DIVERGENTE")) x = xmlSaida(106, "SKU", "3");
        resposta(post(rota(p, "/retirada"), retirada(p, List.of(x), List.of()), supervisor), 409);
        assertThat(contar("retirada_saida")).isZero();
        saldo("4", "0", "4", "0");
        var atual = resposta(get(rota(p, ""), operador), 200);
        assertThat(atual.get("reservas").get(0).get("situacao").asString()).isEqualTo("ATIVA");
    }

    @Test
    void documentoNaoPodeDuplicarExcederOrigemNemUsarXmlInseguro() throws Exception {
        var u = unidade("4");
        var p = separar(reservado(u, "4"), u, endereco(TipoEndereco.SEPARACAO));
        var x = xmlSaida(107, "SKU", "4");
        var dados = documento(p, u, x, "RETORNO_MERCADORIA");
        dados.put("coberturas", List.of(cobertura(p, u, "5")));
        resposta(post(rota(p, "/documentos"), dados, supervisor), 409);
        dados.put(
                "coberturas",
                List.of(
                        Map.of(
                                "reservaId",
                                reservaId(p, u),
                                "notaOrigemId",
                                u.notaId() + 50,
                                "sku",
                                "SKU",
                                "quantidade",
                                4)));
        resposta(post(rota(p, "/documentos"), dados, supervisor), 400);
        dados =
                documento(
                        p,
                        u,
                        "<!DOCTYPE NFe [<!ENTITY x SYSTEM 'file:///inexistente'>]><NFe>&x;</NFe>",
                        "RETORNO_MERCADORIA");
        resposta(post(rota(p, "/documentos"), dados, supervisor), 400);
        assertThat(contar("documento_saida")).isZero();
        var original = documento(p, u, x, "RETORNO_MERCADORIA");
        var res = resposta(post(rota(p, "/documentos"), original, supervisor), 200);
        assertThat(resposta(post(rota(p, "/documentos"), original, supervisor), 200))
                .isEqualTo(res);
        resposta(post(rota(p, "/documentos"), original, operador), 403);
        p = res.get("expedicao").get("pedido");
        dados = documento(p, u, x, "RETORNO_SIMBOLICO");
        resposta(post(rota(p, "/documentos"), dados, supervisor), 409);
        assertThat(contar("documento_saida")).isEqualTo(1);
    }

    @Test
    void cancelamentoRetornaIntegralSemNovaEntradaEExigeCancelarDocumento() throws Exception {
        var u = unidade("10");
        long origem = enderecoAtual(u);
        var p = separar(reservado(u, "4"), u, endereco(TipoEndereco.SEPARACAO));
        var x = xmlSaida(108, "SKU", "4");
        p = documentar(p, u, x, "RETORNO_MERCADORIA");
        var d = comando(p);
        d.put("unidades", List.of(destinacao(p, u, origem)));
        resposta(post(rota(p, "/retorno-interno"), d, supervisor), 409);
        resposta(post(rota(p, "/cancelamento"), comando(p), supervisor), 409);
        var exp = resposta(get(rota(p, "/expedicao"), operador), 200);
        long doc = exp.get("documentos").get(0).get("id").longValue();
        p =
                resposta(
                                post(
                                        rota(p, "/documentos/" + doc + "/cancelamento"),
                                        comando(p),
                                        supervisor),
                                200)
                        .get("expedicao")
                        .get("pedido");
        int entradas = contar("pedido_entrada");
        d = comando(p);
        d.put("unidades", List.of(destinacao(p, u, origem)));
        var r = resposta(post(rota(p, "/retorno-interno"), d, supervisor), 200);
        assertThat(resposta(post(rota(p, "/retorno-interno"), d, supervisor), 200)).isEqualTo(r);
        assertThat(contar("pedido_entrada")).isEqualTo(entradas);
        assertThat(enderecoAtual(u)).isEqualTo(origem);
        saldo("10", "10", "0", "0");
        assertThat(r.get("expedicao").get("pedido").get("situacao").asString())
                .isEqualTo("CANCELADO");
        assertThat(r.get("expedicao").get("fatos").get(1).get("tipo").asString())
                .isEqualTo("RETORNO_INTERNO");
    }

    @Test
    void devolucaoAposRetiradaCriaEntradaVinculadaPreservaFifoELimitaQuantidade() throws Exception {
        var u = unidade("4");
        var p = separar(reservado(u, "4"), u, endereco(TipoEndereco.SEPARACAO));
        var x = xmlSaida(109, "SKU", "4");
        p = documentar(p, u, x, "RETORNO_MERCADORIA");
        var exp =
                resposta(
                                post(
                                        rota(p, "/retirada"),
                                        retirada(p, List.of(x), List.of()),
                                        supervisor),
                                200)
                        .get("expedicao");
        p = exp.get("pedido");
        var dados = devolucao(p, exp.get("baixas").get(0).get("id").longValue(), "3", 201);
        var retorno = resposta(post(rota(p, "/devolucoes"), dados, supervisor), 200);
        assertThat(resposta(post(rota(p, "/devolucoes"), dados, supervisor), 200))
                .isEqualTo(retorno);
        var nova = retorno.get("expedicao").get("devolucoes").get(0);
        assertThat(nova.get("dataFifo").asString()).isEqualTo(FIFO.toString());
        assertThat(nova.get("pedidoEntradaId").longValue())
                .isNotEqualTo(
                        jdbc.queryForObject(
                                "select pedido_id from wms.unidade_logistica where id=?",
                                Long.class,
                                u.id()));
        saldo("3", "0", "0", "0");
        assertThat(contar("retirada_saida")).isEqualTo(1);
        p = retorno.get("expedicao").get("pedido");
        resposta(
                post(
                        rota(p, "/devolucoes"),
                        devolucao(p, exp.get("baixas").get(0).get("id").longValue(), "2", 202),
                        supervisor),
                409);
        assertThat(contar("devolucao_saida")).isEqualTo(1);
    }

    @Test
    void avariaEstruturadaMantemReservaResponsabilidadeGestorEReparoExigeNovaLeitura()
            throws Exception {
        var u = unidade("10");
        long origem = enderecoAtual(u);
        var p = separar(reservado(u, "4"), u, endereco(TipoEndereco.SEPARACAO));
        long quarentena = endereco(TipoEndereco.QUARENTENA);
        var d = avaria(u, "2", quarentena);
        resposta(post(avariaRota(u, ""), d, operador), 403);
        var r = resposta(post(avariaRota(u, ""), d, supervisor), 200);
        assertThat(resposta(post(avariaRota(u, ""), d, supervisor), 200)).isEqualTo(r);
        assertThat(r.get("avaria").get("proporcaoSuspensa").decimalValue())
                .isEqualByComparingTo("0");
        saldo("10", "0", "4", "6");
        var a = r.get("avaria");
        var reconhecer =
                Map.of(
                        "operacaoId",
                        UUID.randomUUID(),
                        "versao",
                        a.get("versao").longValue(),
                        "responsabilidade",
                        "RODOGARCIA",
                        "motivo",
                        "Responsabilidade ficticia reconhecida");
        resposta(
                post(
                        avariaRota(u, "/" + a.get("id").longValue() + "/responsabilidade"),
                        reconhecer,
                        supervisor),
                403);
        var financeira =
                resposta(
                        post(
                                avariaRota(u, "/" + a.get("id").longValue() + "/responsabilidade"),
                                reconhecer,
                                gestor),
                        200);
        assertThat(financeira.get("avaria").get("proporcaoSuspensa").decimalValue())
                .isEqualByComparingTo("0.2");
        assertThat(financeira.get("avaria").get("inicioSuspensao")).isEqualTo(a.get("ocorridaEm"));
        var reparo = reparo(financeira, u, origem);
        var reparada =
                resposta(
                        post(
                                avariaRota(u, "/" + a.get("id").longValue() + "/reparo"),
                                reparo,
                                supervisor),
                        200);
        assertThat(reparada.get("avaria").get("tratativa").asString()).isEqualTo("REPARADA");
        saldo("10", "0", "4", "6");
        p = resposta(get(rota(p, ""), operador), 200);
        assertThat(p.get("situacao").asString()).isEqualTo("EM_SEPARACAO");
        assertThat(p.get("reservas").get(0).get("situacao").asString()).isEqualTo("ATIVA");
        p = separar(p, u, endereco(TipoEndereco.SEPARACAO));
        assertThat(p.get("situacao").asString()).isEqualTo("SEPARADO");
        resposta(get(unidadeRota(u, "/movimentos"), operador), 200);
    }

    @Test
    void d30ReparoRecusaSepararAntesDaNovaLeituraSemEfeito() throws Exception {
        var u = unidade("10");
        long origem = enderecoAtual(u);
        long destinoSeparacao = endereco(TipoEndereco.SEPARACAO);
        var p = separar(reservado(u, "4"), u, destinoSeparacao);
        long quarentena = endereco(TipoEndereco.QUARENTENA);
        var a = resposta(post(avariaRota(u, ""), avaria(u, "2", quarentena), supervisor), 200);
        var reparada =
                resposta(
                        post(
                                avariaRota(
                                        u, "/" + a.get("avaria").get("id").longValue() + "/reparo"),
                                reparo(a, u, origem),
                                supervisor),
                        200);
        assertThat(reparada.get("avaria").get("tratativa").asString()).isEqualTo("REPARADA");
        p = resposta(get(rota(p, ""), operador), 200);
        assertThat(p.get("situacao").asString()).isEqualTo("EM_SEPARACAO");
        var dados = new HashMap<String, Object>(comando(p));
        dados.put("destinacao", destinacao(p, u, destinoSeparacao));
        UUID caso = UUID.randomUUID();
        var antes = d30Foto("reparo-antes", caso);
        assertCodigo(post(rota(p, "/separacoes"), dados, operador), 409, "LEITURA_DESATUALIZADA");
        assertThat(d30Foto("reparo-depois", caso)).isEqualTo(antes);
        var atual = resposta(get(rota(p, ""), operador), 200);
        assertThat(atual).isEqualTo(p);
        assertThat(atual.get("reservas").get(0).get("situacao").asString()).isEqualTo("ATIVA");
        assertThat(atual.get("reservas").get(0).get("quantidade").decimalValue())
                .isEqualByComparingTo("4");
        d30ConferirFisicoEConteudo(u, "10");
        assertThat(enderecoAtual(u)).isEqualTo(origem);
    }

    @Test
    void d30Contagem90ComReserva40JaSeparadaRecusaSemEfeito() throws Exception {
        var u = unidade("100");
        var p = separar(reservado(u, "40"), u, endereco(TipoEndereco.SEPARACAO));
        var estoque = resposta(get(unidadeRota(u, "/estoque"), operador), 200).get("unidade");
        var c =
                resposta(
                        post(
                                "/api/v1/contagens",
                                Map.of(
                                        "operacaoId",
                                        UUID.randomUUID(),
                                        "codigoUnidade",
                                        u.codigo(),
                                        "versaoUnidade",
                                        estoque.get("versao").longValue(),
                                        "contado",
                                        new BigDecimal("90"),
                                        "observadoEm",
                                        clock.instant(),
                                        "motivo",
                                        "Leitura fisica D30 apos separacao"),
                                operador),
                        200);
        assertThat(c.get("esperado").decimalValue()).isEqualByComparingTo("100");
        assertThat(c.get("contado").decimalValue()).isEqualByComparingTo("90");
        assertThat(c.get("diferenca").decimalValue()).isEqualByComparingTo("-10");
        assertThat(c.get("reservado").decimalValue()).isEqualByComparingTo("40");
        var dados =
                Map.of(
                        "operacaoId",
                        UUID.randomUUID(),
                        "revisao",
                        c.get("revisao").intValue(),
                        "versaoUnidade",
                        estoque.get("versao").longValue(),
                        "motivo",
                        "Ajuste D30 com prova fisica",
                        "causa",
                        "Diferenca conferida D30",
                        "destino",
                        "Destino interno ficticio D30",
                        "comprovacao",
                        "Comprovacao ficticia D30",
                        "origens",
                        List.of(
                                Map.of(
                                        "entradaId",
                                        c.get("origens").get(0).get("entradaId").longValue(),
                                        "delta",
                                        new BigDecimal("-10"))));
        UUID caso = UUID.randomUUID();
        var pedidoAntesDaRecusa = resposta(get(rota(p, ""), operador), 200);
        assertThat(pedidoAntesDaRecusa.get("unidadesImpedidas").get(0).longValue())
                .isEqualTo(u.id());
        assertThat(pedidoAntesDaRecusa.get("podeProsseguir").booleanValue()).isFalse();
        var antes = d30Foto("contagem-separada-antes", caso);
        assertCodigo(
                post(
                        "/api/v1/contagens/" + c.get("id").longValue() + "/aplicar",
                        dados,
                        supervisor),
                409,
                "PENDENTE_RESERVA");
        assertThat(d30Foto("contagem-separada-depois", caso)).isEqualTo(antes);
        assertThat(resposta(get(rota(p, ""), operador), 200)).isEqualTo(pedidoAntesDaRecusa);
        assertThat(p.get("situacao").asString()).isEqualTo("SEPARADO");
        d30ConferirFisicoEConteudo(u, "100");
        assertThat(
                        jdbc.queryForObject(
                                "select count(*) from wms.fato_permanencia where tipo='AJUSTE_ESTOQUE'",
                                Integer.class))
                .isZero();
    }

    @Test
    void d30AvariaAberta6Mais5RecusaEMais4AceitaLimite() throws Exception {
        var u = unidade("10");
        var p = reservado(u, "4");
        long quarentena = endereco(TipoEndereco.QUARENTENA);
        var primeira =
                resposta(post(avariaRota(u, ""), avaria(u, "6", quarentena), supervisor), 200);
        assertThat(primeira.get("avaria").get("quantidade").decimalValue())
                .isEqualByComparingTo("6");
        var excesso = avaria(u, "5", quarentena);
        UUID caso = UUID.randomUUID();
        var antes = d30Foto("avaria-excesso-antes", caso);
        assertCodigo(post(avariaRota(u, ""), excesso, supervisor), 409, "AVARIA_EXCEDIDA");
        assertThat(d30Foto("avaria-excesso-depois", caso)).isEqualTo(antes);
        assertThat(contar("avaria_estoque")).isEqualTo(1);
        d30ConferirFisicoEConteudo(u, "10");
        var segunda =
                resposta(post(avariaRota(u, ""), avaria(u, "4", quarentena), supervisor), 200);
        assertThat(segunda.get("avaria").get("quantidade").decimalValue())
                .isEqualByComparingTo("4");
        assertThat(segunda.get("avaria").get("id")).isNotEqualTo(primeira.get("avaria").get("id"));
        assertThat(contar("avaria_estoque")).isEqualTo(2);
        assertThat(
                        jdbc.queryForObject(
                                "select sum(quantidade) from wms.avaria_estoque where unidade_id=? and tratativa='EM_TRATAMENTO'",
                                BigDecimal.class,
                                u.id()))
                .isEqualByComparingTo("10");
        d30ConferirFisicoEConteudo(u, "10");
        var atual = resposta(get(rota(p, ""), operador), 200);
        assertThat(atual.get("reservas").get(0).get("situacao").asString()).isEqualTo("ATIVA");
        assertThat(atual.get("reservas").get(0).get("quantidade").decimalValue())
                .isEqualByComparingTo("4");
        assertThat(
                        jdbc.queryForObject(
                                "select sum(quantidade_prevista) from wms.item_nota_entrada where nota_id=?",
                                BigDecimal.class,
                                u.notaId()))
                .isEqualByComparingTo("10");
    }

    @Test
    void d30ReparoReplayOriginalPreservaOutraAvariaEBloqueio() throws Exception {
        var u = unidade("10");
        var p = reservado(u, "4");
        resposta(post(unidadeRota(u, "/bloqueio"), bloqueio(u), operador), 200);
        long quarentena = endereco(TipoEndereco.QUARENTENA);
        var primeira =
                resposta(post(avariaRota(u, ""), avaria(u, "2", quarentena), supervisor), 200);
        var segunda =
                resposta(post(avariaRota(u, ""), avaria(u, "3", quarentena), supervisor), 200);
        long outraId = segunda.get("avaria").get("id").longValue();
        var antes = D30FotografiaFisica.capturar(jdbc);
        var outraOriginal =
                antes.get("AVARIA_ESTOQUE").stream()
                        .filter(x -> ((Number) x.get("ID")).longValue() == outraId)
                        .findFirst()
                        .orElseThrow();
        var comando = reparo(primeira, u, quarentena);
        var reparada =
                resposta(
                        post(
                                avariaRota(
                                        u,
                                        "/"
                                                + primeira.get("avaria").get("id").longValue()
                                                + "/reparo"),
                                comando,
                                supervisor),
                        200);
        assertThat(reparada.get("avaria").get("id")).isEqualTo(primeira.get("avaria").get("id"));
        assertThat(reparada.get("avaria").get("tratativa").asString()).isEqualTo("REPARADA");
        assertThat(reparada.get("avaria").get("quantidade").decimalValue())
                .isEqualByComparingTo("2");
        assertThat(reparada.get("estoque").get("bloqueada").booleanValue()).isTrue();
        assertThat(reparada.get("estoque").get("avariaPosterior").booleanValue()).isTrue();
        assertThat(reparada.get("estoque").get("tipoLocalizacao").asString())
                .isEqualTo("QUARENTENA");
        var apos = D30FotografiaFisica.capturar(jdbc);
        assertThat(
                        apos.get("AVARIA_ESTOQUE").stream()
                                .filter(x -> ((Number) x.get("ID")).longValue() == outraId)
                                .findFirst()
                                .orElseThrow())
                .isEqualTo(outraOriginal);
        assertThat(outraOriginal.get("TRATATIVA")).isEqualTo("EM_TRATAMENTO");
        assertThat((BigDecimal) outraOriginal.get("QUANTIDADE")).isEqualByComparingTo("3");
        d30ConferirFisicoEConteudo(u, "10");
        var pedido = resposta(get(rota(p, ""), operador), 200);
        assertThat(pedido.get("podeProsseguir").booleanValue()).isFalse();
        assertThat(pedido.get("unidadesImpedidas").get(0).longValue()).isEqualTo(u.id());
        assertThat(pedido.get("reservas").get(0).get("situacao").asString()).isEqualTo("ATIVA");
        assertThat(pedido.get("reservas").get(0).get("quantidade").decimalValue())
                .isEqualByComparingTo("4");
        UUID caso = UUID.randomUUID();
        var semEfeito = d30Foto("reparo-outra-pendente-antes", caso);
        assertCodigo(post(rota(p, "/revalidacao"), null, operador), 409, "RESERVA_IMPEDIDA");
        assertCodigo(
                post(unidadeRota(u, "/liberacao"), bloqueio(u), supervisor),
                409,
                "LIBERACAO_INVALIDA");
        assertThat(d30Foto("reparo-outra-pendente-depois", caso)).isEqualTo(semEfeito);
        var replayAntes = d30Foto("reparo-replay-antes", caso);
        assertThat(
                        resposta(
                                post(
                                        avariaRota(
                                                u,
                                                "/"
                                                        + primeira.get("avaria")
                                                                .get("id")
                                                                .longValue()
                                                        + "/reparo"),
                                        comando,
                                        supervisor),
                                200))
                .isEqualTo(reparada);
        assertThat(d30Foto("reparo-replay-depois", caso)).isEqualTo(replayAntes);
        d30ConferirFisicoEConteudo(u, "10");
    }

    private Map<String, List<Map<String, Object>>> d30Foto(String etapa, UUID caso)
            throws Exception {
        var foto = D30FotografiaFisica.capturar(jdbc);
        assertThat(foto).hasSize(64);
        var dir = Path.of(System.getProperty("wms.test.evidencias.dir"));
        Files.createDirectories(dir);
        Files.writeString(
                dir.resolve("d30-cedro-" + etapa + "-" + caso + ".json"),
                mapper.writerWithDefaultPrettyPrinter().writeValueAsString(foto),
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE_NEW,
                StandardOpenOption.WRITE);
        return foto;
    }

    private void d30ConferirFisicoEConteudo(Unidade u, String esperado) {
        assertThat(
                        jdbc.queryForObject(
                                "select quantidade from wms.unidade_logistica where id=?",
                                BigDecimal.class,
                                u.id()))
                .isEqualByComparingTo(esperado);
        assertThat(
                        jdbc.queryForObject(
                                "select sum(quantidade) from wms.conteudo_unidade where unidade_id=?",
                                BigDecimal.class,
                                u.id()))
                .isEqualByComparingTo(esperado);
    }

    @Test
    void reparoDaAvariaInicialNaoReescreveCondicaoHistorica() throws Exception {
        var u =
                unidade(
                        produtoId,
                        "4",
                        FIFO,
                        0,
                        1,
                        "Q",
                        TipoEndereco.QUARENTENA,
                        CondicaoMercadoria.AVARIADA);
        var d = avaria(u, "4", enderecoAtual(u));
        var a = resposta(post(avariaRota(u, ""), d, supervisor), 200);
        var r =
                resposta(
                        post(
                                avariaRota(
                                        u, "/" + a.get("avaria").get("id").longValue() + "/reparo"),
                                reparo(a, u, endereco(TipoEndereco.ARMAZENAGEM)),
                                supervisor),
                        200);
        assertThat(r.get("estoque").get("unidade").get("condicao").asString())
                .isEqualTo("AVARIADA");
        assertThat(r.get("estoque").get("avariaInicialReparada").booleanValue()).isTrue();
        assertThat(
                        jdbc.queryForObject(
                                "select equivalencia_antes from wms.fato_permanencia where unidade_id=? and tipo='REPARO'",
                                BigDecimal.class,
                                u.id()))
                .isEqualByComparingTo("0");
        assertThat(
                        jdbc.queryForObject(
                                "select equivalencia_depois from wms.fato_permanencia where unidade_id=? and tipo='REPARO'",
                                BigDecimal.class,
                                u.id()))
                .isEqualByComparingTo("1");
        saldo("4", "4", "0", "0");
        var p = criar(produtoId, "4", operador);
        var reservada =
                resposta(post(rota(p, "/reserva"), reservar(p), operador), 200).get("pedido");
        assertThat(reservada.get("podeProsseguir").booleanValue()).isTrue();
    }

    @Test
    void gestorResolveRetiradaEmEncerramentoSemAbrirCadastroParaReservas() throws Exception {
        var u = unidade("4");
        var p = separar(reservado(u, "4"), u, endereco(TipoEndereco.SEPARACAO));
        var x = xmlSaida(110, "SKU", "4");
        p = documentar(p, u, x, "RETORNO_MERCADORIA");
        jdbc.update(
                "update wms.cliente set situacao='ENCERRAMENTO_PENDENTE' where id=?", clienteId);
        var d = retirada(p, List.of(x), List.of());
        resposta(post(rota(p, "/retirada"), d, supervisor), 409);
        d.put("resolverPendentes", true);
        resposta(post(rota(p, "/retirada"), d, supervisor), 403);
        resposta(post(rota(p, "/retirada"), d, gestor), 200);
        assertThat(
                        jdbc.queryForObject(
                                "select situacao from wms.cliente where id=?",
                                String.class,
                                clienteId))
                .isEqualTo("ENCERRAMENTO_PENDENTE");
        resposta(
                post("/api/v1/pedidos-saida", criarDados(List.of(item(produtoId, "1"))), gestor),
                409);
    }

    @ParameterizedTest
    @ValueSource(
            strings = {"SEPARACAO_SAIDA", "RETIRADA_FISICA", "DEVOLUCAO_SAIDA", "AVARIA_DETALHADA"})
    void falhaDaAuditoriaDesfazTodosOsEfeitosDoComando(String acao) throws Exception {
        var u = unidade("10");
        long origem = enderecoAtual(u);
        var p = reservado(u, "4");
        if (!acao.equals("SEPARACAO_SAIDA")) p = separar(p, u, endereco(TipoEndereco.SEPARACAO));
        String x = xmlSaida(111, "SKU", "4");
        if (acao.equals("RETIRADA_FISICA") || acao.equals("DEVOLUCAO_SAIDA"))
            p = documentar(p, u, x, "RETORNO_MERCADORIA");
        JsonNode exp = null;
        if (acao.equals("DEVOLUCAO_SAIDA")) {
            exp =
                    resposta(
                                    post(
                                            rota(p, "/retirada"),
                                            retirada(
                                                    p,
                                                    List.of(x),
                                                    List.of(destinacao(p, u, origem))),
                                            supervisor),
                                    200)
                            .get("expedicao");
            p = exp.get("pedido");
        }
        if (acao.equals("SEPARACAO_SAIDA"))
            p =
                    resposta(post(rota(p, "/leituras"), leitura(p, u), operador), 200)
                            .get("expedicao")
                            .get("pedido");
        int entradaAntes = contar("pedido_entrada"),
                baixaAntes = contar("baixa_saida"),
                avariaAntes = contar("avaria_estoque"),
                fatosAntes = contar("fato_permanencia");
        BigDecimal quantidade =
                jdbc.queryForObject(
                        "select quantidade from wms.unidade_logistica where id=?",
                        BigDecimal.class,
                        u.id());
        long local = enderecoAtual(u);
        long destinoFalha =
                acao.equals("SEPARACAO_SAIDA")
                        ? endereco(TipoEndereco.SEPARACAO)
                        : acao.equals("AVARIA_DETALHADA") ? endereco(TipoEndereco.QUARENTENA) : 0;
        var fotoRollback = D30FotografiaFisica.capturar(jdbc);
        jdbc.execute(
                "alter table wms.auditoria_cadastro add constraint ck_d19_auditoria check (acao <> '"
                        + acao
                        + "')");
        try {
            if (acao.equals("RETIRADA_FISICA"))
                resposta(
                        post(
                                rota(p, "/retirada"),
                                retirada(p, List.of(x), List.of(destinacao(p, u, origem))),
                                supervisor),
                        409);
            if (acao.equals("SEPARACAO_SAIDA")) {
                var d = comando(p);
                d.put("destinacao", destinacao(p, u, destinoFalha));
                resposta(post(rota(p, "/separacoes"), d, operador), 409);
            }
            if (acao.equals("DEVOLUCAO_SAIDA"))
                resposta(
                        post(
                                rota(p, "/devolucoes"),
                                devolucao(
                                        p,
                                        exp.get("baixas").get(0).get("id").longValue(),
                                        "2",
                                        203),
                                supervisor),
                        409);
            if (acao.equals("AVARIA_DETALHADA"))
                resposta(post(avariaRota(u, ""), avaria(u, "2", destinoFalha), supervisor), 409);
        } finally {
            jdbc.execute("alter table wms.auditoria_cadastro drop constraint ck_d19_auditoria");
        }
        assertThat(D30FotografiaFisica.capturar(jdbc)).isEqualTo(fotoRollback);
        assertThat(contar("pedido_entrada")).isEqualTo(entradaAntes);
        assertThat(contar("baixa_saida")).isEqualTo(baixaAntes);
        assertThat(contar("avaria_estoque")).isEqualTo(avariaAntes);
        assertThat(contar("fato_permanencia")).isEqualTo(fatosAntes);
        assertThat(enderecoAtual(u)).isEqualTo(local);
        assertThat(
                        jdbc.queryForObject(
                                "select quantidade from wms.unidade_logistica where id=?",
                                BigDecimal.class,
                                u.id()))
                .isEqualByComparingTo(quantidade);
        assertThat(resposta(get(rota(p, ""), operador), 200).get("versao"))
                .isEqualTo(p.get("versao"));
    }

    @Test
    void duasPosicoesSeparamRetornamERetiramParcialSemAfrouxarOcupacao() throws Exception {
        var u = unidade("10");
        long origem = enderecoAtual(u), segunda = endereco(TipoEndereco.ARMAZENAGEM);
        var grupoOrigem = grupo(origem, segunda);
        jdbc.update(
                "update wms.unidade_logistica set posicoes_necessarias=2,posicoes_equivalentes=2,conjunto_atual_id=? where id=?",
                grupoOrigem,
                u.id());
        new TransactionTemplate(transactions)
                .executeWithoutResult(
                        s -> {
                            var o = new OcupacaoEndereco(em.find(Endereco.class, segunda));
                            o.atribuir(em.find(UnidadeLogistica.class, u.id()));
                            em.persist(o);
                        });
        long sepA = endereco(TipoEndereco.SEPARACAO), sepB = endereco(TipoEndereco.SEPARACAO);
        long grupoSep = grupo(sepA, sepB);
        var p = reservado(u, "4");
        p =
                resposta(post(rota(p, "/leituras"), leitura(p, u), operador), 200)
                        .get("expedicao")
                        .get("pedido");
        var d = comando(p);
        d.put("destinacao", destinacaoDupla(p, u, sepA, sepB, grupoSep));
        p = resposta(post(rota(p, "/separacoes"), d, operador), 200).get("expedicao").get("pedido");
        assertThat(
                        jdbc.queryForObject(
                                "select count(*) from wms.ocupacao_endereco where unidade_id=?",
                                Integer.class,
                                u.id()))
                .isEqualTo(2);
        d = comando(p);
        d.put("unidades", List.of(destinacaoDupla(p, u, origem, segunda, grupoOrigem)));
        resposta(post(rota(p, "/retorno-interno"), d, supervisor), 200);
        saldo("10", "10", "0", "0");
        p = reservado(u, "4");
        p =
                resposta(post(rota(p, "/leituras"), leitura(p, u), operador), 200)
                        .get("expedicao")
                        .get("pedido");
        d = comando(p);
        d.put("destinacao", destinacaoDupla(p, u, sepA, sepB, grupoSep));
        p = resposta(post(rota(p, "/separacoes"), d, operador), 200).get("expedicao").get("pedido");
        var x = xmlSaida(120, "SKU", "4");
        p = documentar(p, u, x, "RETORNO_MERCADORIA");
        var r =
                resposta(
                        post(
                                rota(p, "/retirada"),
                                retirada(
                                        p,
                                        List.of(x),
                                        List.of(
                                                destinacaoDupla(
                                                        p, u, origem, segunda, grupoOrigem))),
                                supervisor),
                        200);
        saldo("6", "6", "0", "0");
        assertThat(
                        resposta(get(unidadeRota(u, "/estoque"), operador), 200)
                                .get("posicoesEquivalentes")
                                .intValue())
                .isEqualTo(2);
        var fatos = r.get("expedicao").get("fatos");
        assertThat(fatos.get(fatos.size() - 1).get("equivalenciaDepois").decimalValue())
                .isEqualByComparingTo("2");
        resposta(
                post(
                        "/api/v1/conjuntos-posicoes",
                        Map.of(
                                "armazemId",
                                armazemId,
                                "codigo",
                                "NEGADO",
                                "enderecoAId",
                                sepA,
                                "enderecoBId",
                                sepB,
                                "limites",
                                limites(),
                                "motivo",
                                "Conjunto ficticio negado"),
                        supervisor),
                403);
    }

    @Test
    void retiradaBloqueiaAvariaConcorrenteAteCommitEConservaRemanescente() throws Exception {
        var u = unidade("10");
        long origem = enderecoAtual(u);
        var p = separar(reservado(u, "4"), u, endereco(TipoEndereco.SEPARACAO));
        var x = xmlSaida(121, "SKU", "4");
        p = documentar(p, u, x, "RETORNO_MERCADORIA");
        String rotaRetirada = rota(p, "/retirada");
        var retirar = retirada(p, List.of(x), List.of(destinacao(p, u, origem)));
        var avariar = avaria(u, "2", endereco(TipoEndereco.QUARENTENA));
        var pronta = new CountDownLatch(1);
        var concluir = new CountDownLatch(1);
        var avariaIniciada = new CountDownLatch(1);
        MovimentoSaidaEstoqueService alvo =
                AopTestUtils.getUltimateTargetObject(movimentoSaidaService);
        AvariaService alvoAvaria = AopTestUtils.getUltimateTargetObject(avariaService);
        doAnswer(
                        inv -> {
                            pronta.countDown();
                            if (!concluir.await(10, TimeUnit.SECONDS))
                                throw new IllegalStateException("Teste não liberou retirada");
                            return inv.callRealMethod();
                        })
                .when(alvo)
                .mover(
                        any(UnidadeLogistica.class),
                        anyList(),
                        any(),
                        eq(TipoEndereco.ARMAZENAGEM),
                        anyBoolean(),
                        any(Instant.class));
        doAnswer(
                        inv -> {
                            avariaIniciada.countDown();
                            return inv.callRealMethod();
                        })
                .when(alvoAvaria)
                .registrar(any(UUID.class), any(AvariaDto.Registrar.class));
        try (var executor = Executors.newFixedThreadPool(2)) {
            var retirada = executor.submit(() -> post(rotaRetirada, retirar, supervisor));
            assertThat(pronta.await(10, TimeUnit.SECONDS)).isTrue();
            var avaria = executor.submit(() -> post(avariaRota(u, ""), avariar, supervisor));
            assertThat(avariaIniciada.await(10, TimeUnit.SECONDS)).isTrue();
            try {
                assertThatThrownBy(() -> avaria.get(300, TimeUnit.MILLISECONDS))
                        .isInstanceOf(TimeoutException.class);
            } finally {
                concluir.countDown();
            }
            resposta(retirada.get(10, TimeUnit.SECONDS), 200);
            assertCodigo(avaria.get(10, TimeUnit.SECONDS), 409, "VERSAO_DESATUALIZADA");
        } finally {
            concluir.countDown();
            reset(alvo, alvoAvaria);
        }
        saldo("6", "6", "0", "0");
        assertThat(contar("avaria_estoque")).isZero();
        assertThat(contar("retirada_saida")).isEqualTo(1);
    }

    @Test
    void duasConfirmacoesConcorrentesDaMesmaRetiradaRetornamMesmoOriginal() throws Exception {
        var u = unidade("4");
        var p = separar(reservado(u, "4"), u, endereco(TipoEndereco.SEPARACAO));
        var x = xmlSaida(122, "SKU", "4");
        p = documentar(p, u, x, "RETORNO_MERCADORIA");
        String caminho = rota(p, "/retirada");
        var d = retirada(p, List.of(x), List.of());
        assertThat(paralelo(() -> post(caminho, d, supervisor), () -> post(caminho, d, supervisor)))
                .containsExactlyInAnyOrder(200, 200);
        assertThat(contar("retirada_saida")).isEqualTo(1);
        assertThat(contar("baixa_saida")).isEqualTo(1);
        saldo("0", "0", "0", "0");
    }

    @Test
    void servicosValidamPerfilEContratoSemDependerDoController() {
        var jwt =
                Jwt.withTokenValue("ficticio")
                        .header("alg", "RS256")
                        .claim("sub", "direto")
                        .claim("wms_perfil", "OPERACAO")
                        .claim("wms_clientes", List.of(clienteId.toString()))
                        .claim("wms_armazens", List.of(armazemId.toString()))
                        .build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
        try {
            assertThatThrownBy(
                            () ->
                                    avariaService.reconhecer(
                                            UUID.randomUUID(),
                                            1L,
                                            new AvariaDto.Reconhecer(
                                                    UUID.randomUUID(),
                                                    0L,
                                                    "RODOGARCIA",
                                                    "Validacao ficticia")))
                    .isInstanceOf(AccessDeniedException.class);
            assertThatThrownBy(
                            () ->
                                    avariaService.registrar(
                                            UUID.randomUUID(),
                                            new AvariaDto.Registrar(
                                                    UUID.randomUUID(),
                                                    0L,
                                                    BigDecimal.ZERO,
                                                    clock.instant(),
                                                    null,
                                                    List.of(),
                                                    "Validacao ficticia",
                                                    false)))
                    .isInstanceOf(ConstraintViolationException.class);
        } finally {
            SecurityContextHolder.clearContext();
        }
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

    private long grupo(long a, long b) throws Exception {
        return resposta(
                        post(
                                "/api/v1/conjuntos-posicoes",
                                Map.of(
                                        "armazemId",
                                        armazemId,
                                        "codigo",
                                        "GRUPO-" + (++numero),
                                        "enderecoAId",
                                        a,
                                        "enderecoBId",
                                        b,
                                        "limites",
                                        limites(),
                                        "motivo",
                                        "Conjunto ficticio de duas posicoes"),
                                gestor),
                        201)
                .get("id")
                .longValue();
    }

    private Map<String, Object> destinacaoDupla(JsonNode p, Unidade u, long a, long b, long grupo) {
        return Map.of(
                "reservaId",
                reservaId(p, u),
                "conjuntoId",
                grupo,
                "destinos",
                List.of(
                        Map.of(
                                "enderecoId",
                                a,
                                "codigoLido",
                                jdbc.queryForObject(
                                        "select codigo from wms.endereco where id=?",
                                        String.class,
                                        a)),
                        Map.of(
                                "enderecoId",
                                b,
                                "codigoLido",
                                jdbc.queryForObject(
                                        "select codigo from wms.endereco where id=?",
                                        String.class,
                                        b))));
    }

    @Test
    void reseparacaoPreservaSnapshotImutavelDoCicloAnterior() throws Exception {
        var u = unidade("10");
        long origemInicial = enderecoAtual(u);
        var p = separar(reservado(u, "4"), u, endereco(TipoEndereco.SEPARACAO));
        String original =
                jdbc.queryForObject(
                        "select resultado from wms.operacao_saida where tipo='SEPARACAO_SAIDA'",
                        String.class);
        var snapshot = mapper.readTree(original);
        String operacao = snapshot.get("operacaoId").asString();
        var avaria =
                resposta(
                        post(
                                avariaRota(u, ""),
                                avaria(u, "2", endereco(TipoEndereco.QUARENTENA)),
                                supervisor),
                        200);
        long outraOrigem = endereco(TipoEndereco.ARMAZENAGEM);
        resposta(
                post(
                        avariaRota(u, "/" + avaria.get("avaria").get("id").longValue() + "/reparo"),
                        reparo(avaria, u, outraOrigem),
                        supervisor),
                200);
        p = resposta(get(rota(p, ""), operador), 200);
        p = separar(p, u, endereco(TipoEndereco.SEPARACAO));
        var corrente = resposta(get(rota(p, "/expedicao"), operador), 200);
        assertThat(
                        corrente.get("separacoes")
                                .get(0)
                                .get("posicoesOrigem")
                                .get(0)
                                .get("enderecoId")
                                .longValue())
                .isEqualTo(outraOrigem);
        assertThat(
                        snapshot.get("expedicao")
                                .get("separacoes")
                                .get(0)
                                .get("posicoesOrigem")
                                .get(0)
                                .get("enderecoId")
                                .longValue())
                .isEqualTo(origemInicial);
        assertThat(
                        jdbc.queryForObject(
                                "select resultado from wms.operacao_saida where operacao_id=?",
                                String.class,
                                operacao))
                .isEqualTo(original);
        assertThat(
                        jdbc.queryForObject(
                                "select count(*) from wms.auditoria_cadastro where tipo='PEDIDO_SAIDA' and acao='SEPARACAO_SAIDA'",
                                Integer.class))
                .isEqualTo(2);
    }

    @ParameterizedTest
    @ValueSource(strings = {"OPERACAO", "SUPERVISOR"})
    void resolucaoPorGestorNaoPodeSerRepetidaPorOutroPerfil(String perfil) throws Exception {
        String outro = token(perfil, List.of(clienteId), List.of(armazemId));
        var u = unidade("10");
        long origem = enderecoAtual(u);
        var p = reservado(u, "4");
        var ler = leitura(p, u);
        ler.put("resolverPendentes", true);
        p = resposta(post(rota(p, "/leituras"), ler, gestor), 200).get("expedicao").get("pedido");
        resposta(post(rota(p, "/leituras"), ler, outro), 403);
        var sep = comando(p);
        sep.put("resolverPendentes", true);
        sep.put("destinacao", destinacao(p, u, endereco(TipoEndereco.SEPARACAO)));
        p = resposta(post(rota(p, "/separacoes"), sep, gestor), 200).get("expedicao").get("pedido");
        resposta(post(rota(p, "/separacoes"), sep, outro), 403);
        var av = avaria(u, "2", endereco(TipoEndereco.QUARENTENA));
        av.put("resolverPendentes", true);
        var dano = resposta(post(avariaRota(u, ""), av, gestor), 200);
        resposta(post(avariaRota(u, ""), av, outro), 403);
        var reparar = reparo(dano, u, origem);
        reparar.put("resolverPendentes", true);
        String caminho = avariaRota(u, "/" + dano.get("avaria").get("id").longValue() + "/reparo");
        resposta(post(caminho, reparar, gestor), 200);
        resposta(post(caminho, reparar, outro), 403);
        p = resposta(get(rota(p, ""), operador), 200);
        p = separar(p, u, endereco(TipoEndereco.SEPARACAO));
        var x = xmlSaida(130, "SKU", "4");
        var doc = documento(p, u, x, "RETORNO_MERCADORIA");
        doc.put("resolverPendentes", true);
        p = resposta(post(rota(p, "/documentos"), doc, gestor), 200).get("expedicao").get("pedido");
        resposta(post(rota(p, "/documentos"), doc, outro), 403);
        var retirar = retirada(p, List.of(x), List.of(destinacao(p, u, origem)));
        retirar.put("resolverPendentes", true);
        var r = resposta(post(rota(p, "/retirada"), retirar, gestor), 200).get("expedicao");
        resposta(post(rota(p, "/retirada"), retirar, outro), 403);
        p = r.get("pedido");
        var dev = devolucao(p, r.get("baixas").get(0).get("id").longValue(), "2", 230);
        dev.put("resolverPendentes", true);
        resposta(post(rota(p, "/devolucoes"), dev, gestor), 200);
        resposta(post(rota(p, "/devolucoes"), dev, outro), 403);
    }

    @Test
    void resolucaoDeRetornoInternoExigeGestorTambemNoReplay() throws Exception {
        var u = unidade("4");
        long origem = enderecoAtual(u);
        var p = separar(reservado(u, "4"), u, endereco(TipoEndereco.SEPARACAO));
        var d = comando(p);
        d.put("resolverPendentes", true);
        d.put("unidades", List.of(destinacao(p, u, origem)));
        var original = resposta(post(rota(p, "/retorno-interno"), d, gestor), 200);
        resposta(post(rota(p, "/retorno-interno"), d, supervisor), 403);
        resposta(post(rota(p, "/retorno-interno"), d, operador), 403);
        assertThat(resposta(post(rota(p, "/retorno-interno"), d, gestor), 200)).isEqualTo(original);
    }

    @Test
    void documentoNotazzExistenteExigeComprovanteXmlSemBaixaAutomatica() throws Exception {
        var u = unidade("4");
        var p = separar(reservado(u, "4"), u, endereco(TipoEndereco.SEPARACAO));
        var doc = comando(p);
        doc.put("origem", "NOTAZZ");
        doc.put("natureza", "RETORNO_MERCADORIA");
        doc.put(
                "nota",
                Map.of(
                        "emitenteCnpj",
                        "98765432000188",
                        "serie",
                        "1",
                        "numero",
                        "131",
                        "emissao",
                        "2026-09-20"));
        doc.put("protocolo", "Autorizacao externa declarada");
        doc.put("coberturas", List.of(cobertura(p, u, "4")));
        p =
                resposta(post(rota(p, "/documentos"), doc, supervisor), 200)
                        .get("expedicao")
                        .get("pedido");
        saldo("4", "0", "4", "0");
        resposta(
                post(
                        rota(p, "/retirada"),
                        retirada(p, List.of(xmlSaida(131, "SKU", "4")), List.of()),
                        supervisor),
                200);
        saldo("0", "0", "0", "0");
    }

    @Test
    void retornoDe201ReservasConfereTodosDestinosERollbackMantemPedidoInteiro() throws Exception {
        var lista = new java.util.ArrayList<Unidade>();
        for (int i = 0; i < 201; i++) lista.add(unidade("1"));
        var p = reservado(lista.getFirst(), "201");
        assertThat(p.get("reservas").size()).isEqualTo(201);
        var destinos = new java.util.ArrayList<Map<String, Object>>();
        for (var u : lista) destinos.add(destinacao(p, u, enderecoAtual(u)));
        final long pedidoId = p.get("id").longValue();
        new TransactionTemplate(transactions)
                .executeWithoutResult(
                        tx -> {
                            var pedido =
                                    em.find(
                                            br.com.rodogarcia.wms.models.PedidoSaida.class,
                                            pedidoId);
                            var rs =
                                    em.createQuery(
                                                    "select r from ReservaSaida r where r.item.pedido.id=:id",
                                                    br.com.rodogarcia.wms.models.ReservaSaida.class)
                                            .setParameter("id", pedidoId)
                                            .getResultList();
                            for (var r : rs) {
                                var u = r.getUnidade();
                                long origem =
                                        enderecoAtual(
                                                new Unidade(
                                                        u.getId(),
                                                        u.getCodigo(),
                                                        u.getNota().getId()));
                                var separacao =
                                        new br.com.rodogarcia.wms.models.SeparacaoSaida(
                                                r, clock.instant());
                                separacao.separar(
                                        mapper.writeValueAsString(
                                                List.of(
                                                        Map.of(
                                                                "enderecoId",
                                                                origem,
                                                                "codigoLido",
                                                                jdbc.queryForObject(
                                                                        "select codigo from wms.endereco where id=?",
                                                                        String.class,
                                                                        origem)))),
                                        null,
                                        clock.instant());
                                em.persist(separacao);
                                var e = em.find(Endereco.class, endereco(TipoEndereco.SEPARACAO));
                                var o = new OcupacaoEndereco(e);
                                o.atribuir(u);
                                em.persist(o);
                                em.createQuery(
                                                "select o from OcupacaoEndereco o where o.endereco.id=:id",
                                                OcupacaoEndereco.class)
                                        .setParameter("id", origem)
                                        .getSingleResult()
                                        .atribuir(null);
                                u.posicionar(
                                        u.getMedidas(),
                                        TipoEndereco.SEPARACAO,
                                        null,
                                        clock.instant());
                            }
                            pedido.atualizar(
                                    br.com.rodogarcia.wms.models.SituacaoPedidoSaida.SEPARADO,
                                    clock.instant());
                        });
        p = resposta(get(rota(p, ""), operador), 200);
        var d = comando(p);
        d.put("unidades", destinos);
        var incompleto = comando(p);
        incompleto.put("unidades", destinos.subList(0, 200));
        resposta(post(rota(p, "/retorno-interno"), incompleto, supervisor), 400);
        jdbc.execute(
                "alter table wms.auditoria_cadastro add constraint ck_d19_201 check (acao <> 'RETORNO_INTERNO')");
        try {
            resposta(post(rota(p, "/retorno-interno"), d, supervisor), 409);
        } finally {
            jdbc.execute("alter table wms.auditoria_cadastro drop constraint ck_d19_201");
        }
        assertThat(
                        jdbc.queryForObject(
                                "select count(*) from wms.unidade_logistica where tipo_localizacao='SEPARACAO' and reserva_saida_id=?",
                                Integer.class,
                                pedidoId))
                .isEqualTo(201);
        saldo("201", "0", "201", "0");
        var r = resposta(post(rota(p, "/retorno-interno"), d, supervisor), 200);
        assertThat(r.get("expedicao").get("pedido").get("situacao").asString())
                .isEqualTo("CANCELADO");
        for (var dest : destinos) {
            var enderecos = (List<?>) dest.get("destinos");
            var e = (Map<?, ?>) enderecos.getFirst();
            assertThat(
                            jdbc.queryForObject(
                                    "select count(*) from wms.ocupacao_endereco where endereco_id=? and unidade_id is not null",
                                    Integer.class,
                                    e.get("enderecoId")))
                    .isEqualTo(1);
        }
        saldo("201", "201", "0", "0");
    }

    @Test
    void reparoInicialPodeReservarSepararECancelarVoltandoAArmazenagem() throws Exception {
        var u =
                unidade(
                        produtoId,
                        "4",
                        FIFO,
                        0,
                        1,
                        "Q-INICIAL",
                        TipoEndereco.QUARENTENA,
                        CondicaoMercadoria.AVARIADA);
        var a =
                resposta(
                        post(avariaRota(u, ""), avaria(u, "4", enderecoAtual(u)), supervisor), 200);
        long armazenagem = endereco(TipoEndereco.ARMAZENAGEM);
        resposta(
                post(
                        avariaRota(u, "/" + a.get("avaria").get("id").longValue() + "/reparo"),
                        reparo(a, u, armazenagem),
                        supervisor),
                200);
        var p = separar(reservado(u, "4"), u, endereco(TipoEndereco.SEPARACAO));
        var d = comando(p);
        d.put("unidades", List.of(destinacao(p, u, armazenagem)));
        resposta(post(rota(p, "/retorno-interno"), d, supervisor), 200);
        var e = resposta(get(unidadeRota(u, "/estoque"), operador), 200);
        assertThat(e.get("unidade").get("condicao").asString()).isEqualTo("AVARIADA");
        assertThat(e.get("avariaInicialReparada").booleanValue()).isTrue();
        assertThat(e.get("tipoLocalizacao").asString()).isEqualTo("ARMAZENAGEM");
        assertThat(e.get("bloqueada").booleanValue()).isFalse();
        saldo("4", "4", "0", "0");
    }

    @Test
    void bloqueioDeCicloAntigoLiberadoNaoRessurgeNoProximoReparo() throws Exception {
        var u = unidade("10");
        long origem = enderecoAtual(u), quarentena = endereco(TipoEndereco.QUARENTENA);
        resposta(post(unidadeRota(u, "/bloqueio"), bloqueio(u), operador), 200);
        var a = resposta(post(avariaRota(u, ""), avaria(u, "2", quarentena), supervisor), 200);
        var primeiro =
                resposta(
                        post(
                                avariaRota(
                                        u, "/" + a.get("avaria").get("id").longValue() + "/reparo"),
                                reparo(a, u, origem),
                                supervisor),
                        200);
        assertThat(primeiro.get("estoque").get("bloqueada").booleanValue()).isTrue();
        resposta(post(unidadeRota(u, "/liberacao"), bloqueio(u), supervisor), 200);
        var b = resposta(post(avariaRota(u, ""), avaria(u, "2", quarentena), supervisor), 200);
        var segundo =
                resposta(
                        post(
                                avariaRota(
                                        u, "/" + b.get("avaria").get("id").longValue() + "/reparo"),
                                reparo(b, u, origem),
                                supervisor),
                        200);
        assertThat(segundo.get("estoque").get("bloqueada").booleanValue()).isFalse();
        assertThat(a.get("avaria").get("cicloId")).isNotEqualTo(b.get("avaria").get("cicloId"));
        saldo("10", "10", "0", "0");
        assertThat(contar("avaria_estoque")).isEqualTo(2);
    }

    @Test
    void avariaRetroativaPreservaBase100AntesE50DepoisDaRetiradaParcial() throws Exception {
        var u = unidade("100");
        long origem = enderecoAtual(u);
        var p = separar(reservado(u, "50"), u, endereco(TipoEndereco.SEPARACAO));
        var x = xmlSaida(140, "SKU", "50");
        p = documentar(p, u, x, "RETORNO_MERCADORIA");
        var r =
                resposta(
                                post(
                                        rota(p, "/retirada"),
                                        retirada(p, List.of(x), List.of(destinacao(p, u, origem))),
                                        supervisor),
                                200)
                        .get("expedicao");
        Instant instante = Instant.parse(r.get("retiradaEm").asString());
        long q = endereco(TipoEndereco.QUARENTENA);
        var d = avaria(u, "20", q);
        d.put("ocorridaEm", instante.minusNanos(1000));
        var anterior = resposta(post(avariaRota(u, ""), d, supervisor), 200);
        assertThat(anterior.get("avaria").get("quantidadeBase").decimalValue())
                .isEqualByComparingTo("100");
        assertThat(anterior.get("avaria").get("equivalenciaBase").decimalValue())
                .isEqualByComparingTo("1");
        var f = reconhecer(anterior, u);
        assertThat(f.get("avaria").get("proporcaoSuspensa").decimalValue())
                .isEqualByComparingTo("0.2");
        var depois = avaria(u, "20", q);
        depois.put("ocorridaEm", instante.plusNanos(1000));
        var posterior = resposta(post(avariaRota(u, ""), depois, supervisor), 200);
        assertThat(posterior.get("avaria").get("quantidadeBase").decimalValue())
                .isEqualByComparingTo("50");
        assertThat(reconhecer(posterior, u).get("avaria").get("proporcaoSuspensa").decimalValue())
                .isEqualByComparingTo("0.4");
        assertThat(
                        jdbc.queryForObject(
                                "select quantidade_antes from wms.fato_permanencia where unidade_id=? and tipo='AVARIA' order by ocorrida_em fetch first 1 row only",
                                BigDecimal.class,
                                u.id()))
                .isEqualByComparingTo("100");
        saldo("50", "0", "0", "50");
        var memoria = resposta(get(avariaRota(u, ""), operador), 200);
        assertThat(memoria.get(0).get("quantidadeBase").decimalValue()).isEqualByComparingTo("100");
    }

    @Test
    void ausenciaDeBaseTemporalGeraPendenciaExplicitaSemInventarSaldoPassado() throws Exception {
        var u = unidade("10");
        var d = avaria(u, "2", endereco(TipoEndereco.QUARENTENA));
        d.put("ocorridaEm", FIFO.plusSeconds(3601));
        assertCodigo(post(avariaRota(u, ""), d, supervisor), 409, "HISTORICO_AVARIA_INSUFICIENTE");
        assertThat(contar("avaria_estoque")).isZero();
        assertThat(contar("fato_permanencia")).isZero();
        saldo("10", "10", "0", "0");
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "DIVISAO_ANTES",
                "DIVISAO_DEPOIS",
                "REAGRUPAMENTO_ANTES",
                "REAGRUPAMENTO_DEPOIS",
                "NOVA_IDENTIDADE_ANTES",
                "NOVA_IDENTIDADE_DEPOIS"
            })
    void transformacaoBe07NaoProjetaQuantidadeAtualParaOPassado(String caso) throws Exception {
        var original =
                unidade(
                        produtoId,
                        "100",
                        FIFO,
                        0,
                        1,
                        "TRANSFORMACAO",
                        TipoEndereco.ARMAZENAGEM,
                        CondicaoMercadoria.BOA,
                        false);
        long pedido =
                jdbc.queryForObject(
                        "select pedido_id from wms.unidade_logistica where id=?",
                        Long.class,
                        original.id());
        var resumo = resposta(get(unidadeRota(original, ""), operador), 200).get("unidade");
        Instant anterior = clock.instant();
        var divisao =
                resposta(
                        post(
                                "/api/v1/pedidos-entrada/"
                                        + pedido
                                        + "/unidades/"
                                        + original.id()
                                        + "/divisao",
                                Map.of(
                                        "operacaoId",
                                        UUID.randomUUID(),
                                        "versao",
                                        resumo.get("versao").longValue(),
                                        "quantidadeNovaUnidade",
                                        "50",
                                        "motivo",
                                        "Divisao fisica ficticia anterior ao dano"),
                                supervisor),
                        200);
        var remanescente = divisao.get("unidades").get(0).get("unidade");
        var criada = divisao.get("unidades").get(1).get("unidade");
        var u = original;
        String base = "50";
        if (caso.startsWith("REAGRUPAMENTO")) {
            anterior = clock.instant();
            resposta(
                    post(
                            "/api/v1/pedidos-entrada/"
                                    + pedido
                                    + "/unidades/"
                                    + original.id()
                                    + "/reagrupamento",
                            Map.of(
                                    "operacaoId",
                                    UUID.randomUUID(),
                                    "versaoDestino",
                                    remanescente.get("versao").longValue(),
                                    "origens",
                                    List.of(
                                            Map.of(
                                                    "unidadeId",
                                                    criada.get("id").longValue(),
                                                    "versao",
                                                    criada.get("versao").longValue())),
                                    "motivo",
                                    "Reagrupamento fisico ficticio anterior ao dano"),
                            supervisor),
                    200);
            base = "100";
        } else if (caso.startsWith("NOVA_IDENTIDADE")) {
            u =
                    new Unidade(
                            criada.get("id").longValue(),
                            criada.get("codigo").asString(),
                            original.notaId());
        }
        long destino = endereco(TipoEndereco.ARMAZENAGEM);
        var posicionar = new HashMap<String, Object>(bloqueio(u));
        posicionar.put(
                "medidas",
                Map.of(
                        "pesoKg",
                        "500",
                        "alturaMetros",
                        "1",
                        "larguraMetros",
                        "1",
                        "profundidadeMetros",
                        "1",
                        "empilhamento",
                        1,
                        "posicoesNecessarias",
                        1));
        posicionar.put(
                "destinos",
                List.of(
                        Map.of(
                                "enderecoId",
                                destino,
                                "codigoLido",
                                jdbc.queryForObject(
                                        "select codigo from wms.endereco where id=?",
                                        String.class,
                                        destino))));
        resposta(post(unidadeRota(u, "/movimentos"), posicionar, operador), 200);
        var dano = avaria(u, "10", endereco(TipoEndereco.QUARENTENA));
        if (caso.endsWith("ANTES")) {
            dano.put("ocorridaEm", anterior);
            assertCodigo(
                    post(avariaRota(u, ""), dano, supervisor),
                    409,
                    "HISTORICO_AVARIA_INSUFICIENTE");
            assertThat(contar("avaria_estoque")).isZero();
            assertThat(contar("fato_permanencia")).isZero();
            assertThat(enderecoAtual(u)).isEqualTo(destino);
        } else {
            var registrada = resposta(post(avariaRota(u, ""), dano, supervisor), 200);
            assertThat(registrada.get("avaria").get("quantidadeBase").decimalValue())
                    .isEqualByComparingTo(base);
            assertThat(registrada.get("avaria").get("equivalenciaBase").decimalValue())
                    .isEqualByComparingTo("1");
            assertThat(
                            reconhecer(registrada, u)
                                    .get("avaria")
                                    .get("proporcaoSuspensa")
                                    .decimalValue())
                    .isEqualByComparingTo(new BigDecimal("10").divide(new BigDecimal(base)));
            assertThat(
                            resposta(get(avariaRota(u, ""), operador), 200)
                                    .get(0)
                                    .get("quantidadeBase")
                                    .decimalValue())
                    .isEqualByComparingTo(base);
        }
        assertThat(
                        jdbc.queryForObject(
                                "select sum(quantidade) from wms.unidade_logistica",
                                BigDecimal.class))
                .isEqualByComparingTo("100");
        assertThat(
                        jdbc.queryForObject(
                                "select sum(quantidade) from wms.conteudo_unidade",
                                BigDecimal.class))
                .isEqualByComparingTo("100");
        assertThat(contar("operacao_unidade")).isEqualTo(caso.startsWith("REAGRUPAMENTO") ? 2 : 1);
    }

    private JsonNode reconhecer(JsonNode a, Unidade u) throws Exception {
        return resposta(
                post(
                        avariaRota(
                                u,
                                "/" + a.get("avaria").get("id").longValue() + "/responsabilidade"),
                        Map.of(
                                "operacaoId",
                                UUID.randomUUID(),
                                "versao",
                                a.get("avaria").get("versao").longValue(),
                                "responsabilidade",
                                "RODOGARCIA",
                                "motivo",
                                "Efeito financeiro ficticio validado"),
                        gestor),
                200);
    }

    @Test
    void datasInvalidasNaoGravamAvariaNemDevolucaoOuAlteramReserva() throws Exception {
        var u = unidade("4");
        var p = separar(reservado(u, "4"), u, endereco(TipoEndereco.SEPARACAO));
        var dano = avaria(u, "1", endereco(TipoEndereco.QUARENTENA));
        dano.put("ocorridaEm", clock.instant().plusSeconds(86400));
        assertCodigo(post(avariaRota(u, ""), dano, supervisor), 400, "DADOS_INVALIDOS");
        dano.put("ocorridaEm", FIFO.minusSeconds(1));
        assertCodigo(post(avariaRota(u, ""), dano, supervisor), 400, "DADOS_INVALIDOS");
        assertThat(contar("avaria_estoque")).isZero();
        saldo("4", "0", "4", "0");

        var x = xmlSaida(150, "SKU", "4");
        p = documentar(p, u, x, "RETORNO_MERCADORIA");
        var retirada =
                resposta(
                                post(
                                        rota(p, "/retirada"),
                                        retirada(p, List.of(x), List.of()),
                                        supervisor),
                                200)
                        .get("expedicao");
        p = retirada.get("pedido");
        var devolver = devolucao(p, retirada.get("baixas").get(0).get("id").longValue(), "1", 250);
        devolver.put("chegadaReal", clock.instant().plusSeconds(86400));
        assertCodigo(post(rota(p, "/devolucoes"), devolver, supervisor), 400, "DADOS_INVALIDOS");
        devolver.put(
                "chegadaReal",
                Instant.parse(retirada.get("retiradaEm").asString()).minusNanos(1000));
        assertCodigo(post(rota(p, "/devolucoes"), devolver, supervisor), 400, "DADOS_INVALIDOS");
        assertThat(contar("devolucao_saida")).isZero();
        assertThat(contar("pedido_entrada")).isEqualTo(1);
        assertThat(
                        resposta(get(rota(p, "/expedicao"), operador), 200)
                                .get("pedido")
                                .get("situacao")
                                .asString())
                .isEqualTo("RETIRADO");
        saldo("0", "0", "0", "0");
    }

    @Test
    void d30MedidaKgRetiraQuantidadeExataPreservandoNotaDunPesoEPrecisao() throws Exception {
        produtoId =
                new TransactionTemplate(transactions)
                        .execute(
                                tx -> {
                                    var produto =
                                            new Produto(
                                                    em.find(Cliente.class, clienteId),
                                                    "D30-KG",
                                                    "Medida explicita ficticia",
                                                    "KG",
                                                    TipoQuantidade.MEDIDA,
                                                    3,
                                                    false,
                                                    false,
                                                    null,
                                                    clock.instant());
                                    em.persist(produto);
                                    em.persist(
                                            new Embalagem(
                                                    produto,
                                                    "D30-KG-DUN",
                                                    "Embalagem independente",
                                                    new BigDecimal("2"),
                                                    clock.instant()));
                                    em.flush();
                                    return produto.getId();
                                });
        var u = unidade("10.125");
        long origem = enderecoAtual(u);
        var notaAntes = jdbc.queryForMap("select * from wms.nota_entrada where id=?", u.notaId());
        var itemAntes =
                jdbc.queryForMap("select * from wms.item_nota_entrada where nota_id=?", u.notaId());
        var produtoAntes = jdbc.queryForMap("select * from wms.produto where id=?", produtoId);
        var p = reservado(u, "2.500");
        p = separar(p, u, endereco(TipoEndereco.SEPARACAO));
        var xml = xmlSaida(130, "D30-KG", "2.500").replace("<uCom>UN</uCom>", "<uCom>KG</uCom>");
        p = documentar(p, u, xml, "RETORNO_MERCADORIA");
        var comando = retirada(p, List.of(xml), List.of(destinacao(p, u, origem)));
        var retirado = resposta(post(rota(p, "/retirada"), comando, supervisor), 200);
        assertThat(retirado.get("expedicao").get("pedido").get("situacao").asString())
                .isEqualTo("RETIRADO");
        assertThat(jdbc.queryForObject("select quantidade from wms.baixa_saida", BigDecimal.class))
                .isEqualByComparingTo("2.500");
        assertThat(
                        jdbc.queryForObject(
                                "select quantidade from wms.unidade_logistica where id=?",
                                BigDecimal.class,
                                u.id()))
                .isEqualByComparingTo("7.625");
        assertThat(
                        jdbc.queryForObject(
                                "select quantidade from wms.conteudo_unidade where unidade_id=?",
                                BigDecimal.class,
                                u.id()))
                .isEqualByComparingTo("7.625");
        assertThat(
                        jdbc.queryForObject(
                                "select peso_kg from wms.unidade_logistica where id=?",
                                BigDecimal.class,
                                u.id()))
                .isEqualByComparingTo("500");
        assertThat(
                        jdbc.queryForObject(
                                "select quantidade_produto from wms.embalagem where produto_id=?",
                                BigDecimal.class,
                                produtoId))
                .isEqualByComparingTo("2");
        assertThat(jdbc.queryForMap("select * from wms.nota_entrada where id=?", u.notaId()))
                .isEqualTo(notaAntes);
        assertThat(
                        jdbc.queryForMap(
                                "select * from wms.item_nota_entrada where nota_id=?", u.notaId()))
                .isEqualTo(itemAntes);
        assertThat(jdbc.queryForMap("select * from wms.produto where id=?", produtoId))
                .isEqualTo(produtoAntes);
        assertThat(produtoAntes.get("UNIDADE_MEDIDA")).isEqualTo("KG");
        assertThat(((Number) produtoAntes.get("PRECISAO_QUANTIDADE")).intValue()).isEqualTo(3);
        var antesReplay = D30FotografiaFisica.capturar(jdbc);
        assertThat(resposta(post(rota(p, "/retirada"), comando, supervisor), 200))
                .isEqualTo(retirado);
        assertThat(D30FotografiaFisica.capturar(jdbc)).isEqualTo(antesReplay);
        saldo("7.625", "7.625", "0", "0");
    }

    private JsonNode reservado(Unidade u, String q) throws Exception {
        var p = criar(produtoId, q, operador);
        return resposta(post(rota(p, "/reserva"), reservar(p), operador), 200).get("pedido");
    }

    private long reservaId(JsonNode p, Unidade u) {
        for (var r : p.get("reservas"))
            if (r.get("unidadeId").longValue() == u.id()
                    && r.get("situacao").asString().equals("ATIVA")) return r.get("id").longValue();
        throw new AssertionError("Reserva inexistente");
    }

    private Map<String, Object> leitura(JsonNode p, Unidade u) throws Exception {
        var d = comando(p);
        d.put("reservaId", reservaId(p, u));
        d.put("codigoLido", u.codigo());
        d.put(
                "revisaoConteudo",
                resposta(get(unidadeRota(u, "/etiqueta"), operador), 200)
                        .get("versaoConteudo")
                        .longValue());
        return d;
    }

    private long enderecoAtual(Unidade u) {
        return jdbc.queryForObject(
                "select min(endereco_id) from wms.ocupacao_endereco where unidade_id=?",
                Long.class,
                u.id());
    }

    private long endereco(TipoEndereco tipo) {
        return new TransactionTemplate(transactions)
                .execute(
                        s -> {
                            var e =
                                    new Endereco(
                                            em.find(Armazem.class, armazemId),
                                            "DEST-" + (++numero),
                                            "R",
                                            0,
                                            "P" + numero,
                                            "Destino ficticio",
                                            tipo,
                                            new BigDecimal("1000"),
                                            new BigDecimal("2"),
                                            new BigDecimal("2"),
                                            new BigDecimal("2"),
                                            2,
                                            numero,
                                            clock.instant());
                            e.configurarFisico(
                                    TipoUnidadeLogistica.PALLET,
                                    new BigDecimal("1000"),
                                    new BigDecimal("2"),
                                    new BigDecimal("2"),
                                    new BigDecimal("2"),
                                    2,
                                    clock.instant());
                            em.persist(e);
                            em.flush();
                            return e.getId();
                        });
    }

    private Map<String, Object> destinacao(JsonNode p, Unidade u, long id) {
        return Map.of(
                "reservaId",
                reservaId(p, u),
                "destinos",
                List.of(
                        Map.of(
                                "enderecoId",
                                id,
                                "codigoLido",
                                jdbc.queryForObject(
                                        "select codigo from wms.endereco where id=?",
                                        String.class,
                                        id))));
    }

    private JsonNode separar(JsonNode p, Unidade u, long destino) throws Exception {
        p =
                resposta(post(rota(p, "/leituras"), leitura(p, u), operador), 200)
                        .get("expedicao")
                        .get("pedido");
        var d = comando(p);
        d.put("destinacao", destinacao(p, u, destino));
        return resposta(post(rota(p, "/separacoes"), d, operador), 200)
                .get("expedicao")
                .get("pedido");
    }

    private Map<String, Object> cobertura(JsonNode p, Unidade u, String q) {
        return Map.of(
                "reservaId",
                reservaId(p, u),
                "notaOrigemId",
                u.notaId(),
                "sku",
                jdbc.queryForObject(
                        "select p.sku from wms.produto p join wms.unidade_logistica u on u.produto_id=p.id where u.id=?",
                        String.class,
                        u.id()),
                "quantidade",
                new BigDecimal(q));
    }

    private Map<String, Object> documento(JsonNode p, Unidade u, String x, String natureza) {
        var d = comando(p);
        d.put("origem", "XML");
        d.put("natureza", natureza);
        d.put("xml", x);
        d.put("protocolo", "Declaracao ficticia existente");
        String q = "0";
        for (var r : p.get("reservas"))
            if (r.get("unidadeId").longValue() == u.id())
                q = r.get("quantidade").decimalValue().toPlainString();
        d.put("coberturas", List.of(cobertura(p, u, q)));
        return d;
    }

    private JsonNode documentar(JsonNode p, Unidade u, String x, String natureza) throws Exception {
        return resposta(post(rota(p, "/documentos"), documento(p, u, x, natureza), supervisor), 200)
                .get("expedicao")
                .get("pedido");
    }

    private Map<String, Object> retirada(
            JsonNode p, List<String> xmls, List<Map<String, Object>> rem) {
        var d = comando(p);
        d.put("xmls", xmls);
        d.put("remanescentes", rem);
        return d;
    }

    private String xmlSaida(int numero, String sku, String qtd) {
        return "<NFe xmlns=\"http://www.portalfiscal.inf.br/nfe\"><infNFe Id=\"NFe"
                + "1".repeat(40)
                + String.format("%04d", numero)
                + "\" versao=\"4.00\"><ide><mod>55</mod><serie>1</serie><nNF>"
                + numero
                + "</nNF><dhEmi>2026-09-20T12:00:00-03:00</dhEmi></ide><emit><CNPJ>98765432000188</CNPJ></emit><det nItem=\"1\"><prod><cProd>"
                + sku
                + "</cProd><uCom>UN</uCom><qCom>"
                + qtd
                + "</qCom><vProd>0.00</vProd></prod></det></infNFe></NFe>";
    }

    private Map<String, Object> devolucao(JsonNode p, long baixa, String q, int n)
            throws Exception {
        var d = comando(p);
        d.put("referencia", "DEV-" + n);
        d.put(
                "nota",
                Map.of(
                        "emitenteCnpj",
                        "12345678000199",
                        "serie",
                        "2",
                        "numero",
                        Integer.toString(n),
                        "emissao",
                        "2026-09-20"));
        Instant retirada =
                Instant.parse(
                        resposta(get(rota(p, "/expedicao"), operador), 200)
                                .get("retiradaEm")
                                .asString());
        // Instante fictício comprovadamente posterior à retirada, sem depender da leitura
        // imediata do relógio do cliente em relação ao timestamp confirmado pelo servidor.
        d.put("chegadaReal", retirada.plusNanos(1000));
        d.put(
                "itens",
                List.of(
                        Map.of(
                                "baixaId",
                                baixa,
                                "quantidade",
                                new BigDecimal(q),
                                "quantidadeAvariada",
                                BigDecimal.ZERO)));
        return d;
    }

    private String avariaRota(Unidade u, String sufixo) {
        return "/api/v1/estoque/unidades/" + u.codigo() + "/avarias" + sufixo;
    }

    private Map<String, Object> avaria(Unidade u, String q, long destino) throws Exception {
        var d = new HashMap<String, Object>(bloqueio(u));
        d.put("quantidade", new BigDecimal(q));
        d.put("ocorridaEm", clock.instant());
        d.put(
                "destinos",
                List.of(
                        Map.of(
                                "enderecoId",
                                destino,
                                "codigoLido",
                                jdbc.queryForObject(
                                        "select codigo from wms.endereco where id=?",
                                        String.class,
                                        destino))));
        return d;
    }

    private Map<String, Object> reparo(JsonNode a, Unidade u, long destino) throws Exception {
        var d = new HashMap<String, Object>(bloqueio(u));
        d.put("versao", a.get("avaria").get("versao").longValue());
        d.put(
                "destinos",
                List.of(
                        Map.of(
                                "enderecoId",
                                destino,
                                "codigoLido",
                                jdbc.queryForObject(
                                        "select codigo from wms.endereco where id=?",
                                        String.class,
                                        destino))));
        return d;
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
        return unidade(produtoId, quantidade, fifo, nivel, sequencia, codigo, tipo, condicao, true);
    }

    private Unidade unidade(
            Long produtoId,
            String quantidade,
            Instant fifo,
            int nivel,
            int sequencia,
            String codigo,
            TipoEndereco tipo,
            CondicaoMercadoria condicao,
            boolean posicionada) {
        return new TransactionTemplate(transactions)
                .execute(
                        s -> {
                            var agora = clock.instant();
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
                            if (posicionada) {
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
                            }
                            em.flush();
                            return new Unidade(u.getId(), u.getCodigo(), nota.getId());
                        });
    }

    private Map<String, Object> item(Long id, String q) {
        return Map.of("produtoId", id, "quantidade", new BigDecimal(q));
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
        return resposta(post("/api/v1/pedidos-saida", criarDados(itens), token), 201).get("pedido");
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
}
