package br.com.rodogarcia.wms;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.rodogarcia.wms.config.IdentidadeTesteConfig;
import br.com.rodogarcia.wms.dto.UnidadeLogisticaDto;
import br.com.rodogarcia.wms.models.Armazem;
import br.com.rodogarcia.wms.models.Cliente;
import br.com.rodogarcia.wms.models.Embalagem;
import br.com.rodogarcia.wms.models.Produto;
import br.com.rodogarcia.wms.models.TipoQuantidade;
import br.com.rodogarcia.wms.repositories.ArmazemRepository;
import br.com.rodogarcia.wms.repositories.ClienteRepository;
import br.com.rodogarcia.wms.repositories.EmbalagemRepository;
import br.com.rodogarcia.wms.repositories.ProdutoRepository;
import br.com.rodogarcia.wms.services.UnidadeLogisticaService;
import jakarta.validation.ConstraintViolationException;
import java.math.BigDecimal;
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
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
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
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@ActiveProfiles("test")
@TestPropertySource(
        locations = "classpath:cadastros-test.properties",
        properties =
                "spring.datasource.url=jdbc:h2:mem:wms-unidades;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000;INIT=CREATE SCHEMA IF NOT EXISTS wms")
@Import(IdentidadeTesteConfig.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class UnidadeLogisticaIntegrationTest {
    private static final String BASE = "/api/v1/pedidos-entrada/";
    private static final String CODIGOS = "/api/v1/unidades-logisticas/";
    private static final String DIA1 = "2026-09-01T12:00:00Z";
    private static final String DIA2 = "2026-09-02T12:00:00Z";
    private final HttpClient client =
            HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    @Autowired private Environment environment;
    @Autowired private JsonMapper mapper;
    @Autowired private JwtEncoder encoder;
    @Autowired private JwtDecoder decoder;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ClienteRepository clientes;
    @Autowired private ArmazemRepository armazens;
    @Autowired private ProdutoRepository produtos;
    @Autowired private EmbalagemRepository embalagens;
    @Autowired private UnidadeLogisticaService service;
    @Autowired private br.com.rodogarcia.wms.repositories.UnidadeLogisticaRepository d30Unidades;
    private Cliente cliente;
    private Armazem armazem;
    private Produto produto;
    private Embalagem embalagem;
    private String operador;
    private String supervisor;
    private String gestor;
    private long numeroNota;

    @BeforeEach
    void preparar() {
        for (String tabela :
                List.of(
                        "operacao_unidade",
                        "conteudo_unidade",
                        "unidade_logistica",
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
        Instant agora = Instant.now();
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
        produto = produto("SKU-01", TipoQuantidade.CONTAGEM, 0);
        embalagem = embalagem(produto, "DUN-10", "10");
        gestor = token("GESTOR", List.of(), List.of());
        operador = token("OPERACAO", List.of(cliente.getId()), List.of(armazem.getId()));
        supervisor = token("SUPERVISOR", List.of(cliente.getId()), List.of(armazem.getId()));
        numeroNota = 0;
    }

    @Test
    void unitizacaoFechaQuantidadePorCondicaoEEtiquetaNaoCriaSaldo() throws Exception {
        long pedido = receber("8", "2");
        long entrada = entrada(pedido, 0).get("id").longValue();
        var resultado =
                resposta(
                        post(
                                unitizacao(pedido, entrada),
                                dados(
                                        pedido,
                                        List.of(
                                                nova("5", "BOA"),
                                                nova("3", "BOA"),
                                                nova("2", "AVARIADA"))),
                                operador),
                        200);
        assertThat(contar("unidade_logistica")).isEqualTo(3);
        assertThat(contar("entrada_conferida")).isEqualTo(1);
        assertThat(saldo()).isEqualByComparingTo("10");
        assertThat(
                        resposta(get(BASE + pedido + "/unitizacao", operador), 200)
                                .get("concluida")
                                .booleanValue())
                .isTrue();
        assertThat(entrada(pedido, 0).get("unitizadaEm").isNull()).isFalse();
        var codigos = new ArrayList<String>();
        BigDecimal boas = BigDecimal.ZERO, avariadas = BigDecimal.ZERO;
        long revisaoPedido = versao(pedido);
        int auditorias = contar("auditoria_cadastro");
        for (var detalhe : resultado.get("unidades")) {
            var unidade = detalhe.get("unidade");
            assertThat(unidade.get("clienteId").longValue()).isEqualTo(cliente.getId());
            assertThat(unidade.get("produtoId").longValue()).isEqualTo(produto.getId());
            assertThat(unidade.get("unidadeMedida").asString()).isEqualTo("UN");
            if (unidade.get("condicao").asString().equals("AVARIADA"))
                avariadas = avariadas.add(unidade.get("quantidade").decimalValue());
            else boas = boas.add(unidade.get("quantidade").decimalValue());
            String codigo = unidade.get("codigo").asString();
            codigos.add(codigo);
            assertThat(UUID.fromString(codigo).toString()).isEqualTo(codigo);
            assertThat(unidade.get("ativa").booleanValue()).isTrue();
            assertThat(unidade.get("disponivelParaSaida").booleanValue()).isFalse();
            assertThat(unidade.get("codigoDun").asString()).isEqualTo("DUN-10");
            assertThat(unidade.get("quantidadeProdutoPorDun").decimalValue())
                    .isEqualByComparingTo("10");
            assertThat(unidade.get("controlaLote").booleanValue()).isFalse();
            assertThat(unidade.get("lote").isNull()).isTrue();
            assertThat(detalhe.get("origens").get(0).get("entradaId").longValue())
                    .isEqualTo(entrada);
            var etiqueta = resposta(get(CODIGOS + codigo + "/etiqueta", operador), 200);
            assertThat(resposta(get(CODIGOS + codigo + "/etiqueta", operador), 200))
                    .isEqualTo(etiqueta);
            assertThat(etiqueta.get("quantidadeProduto").decimalValue())
                    .isEqualByComparingTo(unidade.get("quantidade").decimalValue());
            assertThat(etiqueta.get("dataEntrada").asString()).isEqualTo(DIA1);
            assertThat(etiqueta.has("endereco")).isFalse();
            var leitura = resposta(get(CODIGOS + codigo, operador), 200).get("unidade");
            assertThat(leitura.get("codigo")).isEqualTo(unidade.get("codigo"));
            assertThat(leitura.get("versao")).isEqualTo(unidade.get("versao"));
            assertThat(leitura.get("quantidade").decimalValue())
                    .isEqualByComparingTo(unidade.get("quantidade").decimalValue());
        }
        assertThat(codigos).doesNotHaveDuplicates();
        assertThat(boas).isEqualByComparingTo("8");
        assertThat(avariadas).isEqualByComparingTo("2");
        assertThat(
                        jdbc.queryForObject(
                                "select sum(quantidade) from wms.unidade_logistica where condicao='AVARIADA'",
                                BigDecimal.class))
                .isEqualByComparingTo("2");
        assertThat(versao(pedido)).isEqualTo(revisaoPedido);
        assertThat(contar("auditoria_cadastro")).isEqualTo(auditorias);
        assertThat(contar("operacao_unidade")).isEqualTo(1);
        var primeiraUnidade = resultado.get("unidades").get(0).get("unidade");
        var detalheConsultado =
                d30GetFotografiaIntegral(
                        "/api/v1/pedidos-entrada/"
                                + pedido
                                + "/unidades/"
                                + primeiraUnidade.get("id").longValue(),
                        gestor);
        assertThat(detalheConsultado.get("unidade").get("id").longValue())
                .isEqualTo(primeiraUnidade.get("id").longValue());
    }

    @ParameterizedTest
    @ValueSource(strings = {"7", "9", "10"})
    void naoAceitaFaltaSobraOuConverterAvariaEmBoa(String boa) throws Exception {
        long pedido = receber("8", "2");
        var lista =
                boa.equals("10")
                        ? List.of(nova(boa, "BOA"))
                        : List.of(nova(boa, "BOA"), nova("2", "AVARIADA"));
        resposta(
                post(
                        unitizacao(pedido, entrada(pedido, 0).get("id").longValue()),
                        dados(pedido, lista),
                        operador),
                409);
        assertThat(contar("unidade_logistica")).isZero();
        assertThat(contar("conteudo_unidade")).isZero();
        assertThat(contar("operacao_unidade")).isZero();
        assertThat(entrada(pedido, 0).get("unitizadaEm").isNull()).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1", "0.5"})
    void quantidadesInvalidasNaoGeramIdentidades(String quantidade) throws Exception {
        long pedido = receber("10", "0");
        resposta(
                post(
                        unitizacao(pedido, entrada(pedido, 0).get("id").longValue()),
                        dados(pedido, List.of(nova(quantidade, "BOA"))),
                        operador),
                400);
        assertThat(contar("unidade_logistica")).isZero();
    }

    @Test
    void naoUnitizaAntesDeEfetivarNemEntradaDeOutroPedido() throws Exception {
        long pedido = criarPedido();
        resposta(
                post(unitizacao(pedido, 999L), dados(pedido, List.of(nova("10", "BOA"))), operador),
                409);
        long outro = receber("10", "0");
        long terceiro = receber("10", "0");
        resposta(
                post(
                        unitizacao(outro, entrada(terceiro, 0).get("id").longValue()),
                        dados(outro, List.of(nova("10", "BOA"))),
                        operador),
                404);
        assertThat(contar("unidade_logistica")).isZero();
    }

    @Test
    void rejeitaEmbalagemDeOutroProdutoEConteudoExtra() throws Exception {
        long pedido = receber("10", "0");
        String rota = unitizacao(pedido, entrada(pedido, 0).get("id").longValue());
        var outra = embalagem(produto("OUTRO", TipoQuantidade.CONTAGEM, 0), "DUN-OUTRO", "10");
        var unidade = new HashMap<>(nova("10", "BOA"));
        unidade.put("embalagemId", outra.getId());
        resposta(post(rota, dados(pedido, List.of(unidade)), operador), 400);
        unidade.put("embalagemId", embalagem.getId());
        unidade.put("produtoId", produto.getId());
        resposta(post(rota, dados(pedido, List.of(unidade)), operador), 400);
        assertThat(contar("unidade_logistica")).isZero();
    }

    @Test
    void repeticaoDevolveConfirmacaoOriginalMesmoDepoisDeDividir() throws Exception {
        long pedido = receber("10", "0");
        String rota = unitizacao(pedido, entrada(pedido, 0).get("id").longValue());
        var dados = dados(pedido, List.of(nova("10", "BOA")));
        var primeiro = resposta(post(rota, dados, operador), 200);
        var u = primeiro.get("unidades").get(0).get("unidade");
        resposta(post(divisao(pedido, u), dividir(u, "4"), supervisor), 200);
        assertThat(resposta(post(rota, dados, operador), 200)).isEqualTo(primeiro);
        var alterados = new HashMap<>(dados);
        alterados.put("motivo", "Conteudo diferente na repeticao");
        resposta(post(rota, alterados, operador), 409);
        resposta(post(rota, dados(pedido, List.of(nova("10", "BOA"))), operador), 409);
        assertThat(contar("unidade_logistica")).isEqualTo(2);
        assertThat(contar("operacao_unidade")).isEqualTo(2);
        assertThat(saldo()).isEqualByComparingTo("10");
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void confirmacoesSimultaneasNaoDuplicamQuantidade(boolean mesmaChave) throws Exception {
        long pedido = receber("10", "0");
        String rota = unitizacao(pedido, entrada(pedido, 0).get("id").longValue());
        var dados = dados(pedido, List.of(nova("10", "BOA")));
        var outros = mesmaChave ? dados : new HashMap<>(dados);
        if (!mesmaChave) outros.put("operacaoId", UUID.randomUUID());
        assertThat(paralelo(() -> post(rota, dados, operador), () -> post(rota, outros, operador)))
                .containsExactlyInAnyOrder(
                        mesmaChave ? new Integer[] {200, 200} : new Integer[] {200, 409});
        assertThat(contar("unidade_logistica")).isEqualTo(1);
        assertThat(contar("operacao_unidade")).isEqualTo(1);
        assertThat(saldo()).isEqualByComparingTo("10");
    }

    @Test
    void dividirMantemRemanescenteIdentidadeFifoEAtualizaEtiqueta() throws Exception {
        long pedido = receber("10", "0");
        var u =
                unitizar(pedido, 0, List.of(nova("10", "BOA")))
                        .get("unidades")
                        .get(0)
                        .get("unidade");
        String codigo = u.get("codigo").asString();
        var dados = dividir(u, "4");
        var resultado = resposta(post(divisao(pedido, u), dados, supervisor), 200);
        assertThat(resposta(post(divisao(pedido, u), dados, supervisor), 200)).isEqualTo(resultado);
        var fotoReplay = D30FotografiaFisica.capturar(jdbc);
        resposta(post(divisao(pedido, u), dados, operador), 403);
        assertThat(D30FotografiaFisica.capturar(jdbc)).isEqualTo(fotoReplay);
        var atual = resposta(get(CODIGOS + codigo + "/etiqueta", operador), 200);
        assertThat(atual.get("quantidadeProduto").decimalValue()).isEqualByComparingTo("6");
        assertThat(atual.get("versaoConteudo").longValue()).isEqualTo(1);
        assertThat(atual.get("dataEntrada").asString()).isEqualTo(DIA1);
        var nova = resultado.get("unidades").get(1).get("unidade");
        assertThat(nova.get("codigo").asString()).isNotEqualTo(codigo);
        assertThat(nova.get("quantidade").decimalValue()).isEqualByComparingTo("4");
        assertThat(nova.get("dataFifo")).isEqualTo(u.get("dataFifo"));
        assertThat(saldo()).isEqualByComparingTo("10");
        resposta(post(divisao(pedido, u), dividir(u, "1"), supervisor), 409);
        assertThat(contar("operacao_unidade")).isEqualTo(2);
        var auditoria =
                jdbc.queryForObject(
                        "select dados_depois from wms.auditoria_cadastro where acao='UNIDADE_DIVIDIDA'",
                        String.class);
        var evento = mapper.readTree(auditoria).get("evento");
        assertThat(evento.get("antes").get(0).get("unidade").get("quantidade").decimalValue())
                .isEqualByComparingTo("10");
        assertThat(evento.get("depois").size()).isEqualTo(2);
    }

    @ParameterizedTest
    @ValueSource(strings = {"10", "11", "0.5"})
    void divisaoExigeRemanescenteEPrecisao(String quantidade) throws Exception {
        long pedido = receber("10", "0");
        var u =
                unitizar(pedido, 0, List.of(nova("10", "BOA")))
                        .get("unidades")
                        .get(0)
                        .get("unidade");
        resposta(post(divisao(pedido, u), dividir(u, quantidade), supervisor), 400);
        assertThat(contar("unidade_logistica")).isEqualTo(1);
        assertThat(saldo()).isEqualByComparingTo("10");
    }

    @Test
    void reagrupamentoEDivisaoConservamCadaOrigemEEncerramIdentidadeConsumida() throws Exception {
        long pedido = receberPar("IGUAIS");
        var a =
                unitizar(pedido, 0, List.of(nova("5", "BOA")))
                        .get("unidades")
                        .get(0)
                        .get("unidade");
        assertThat(
                        resposta(get(BASE + pedido + "/unitizacao", operador), 200)
                                .get("concluida")
                                .booleanValue())
                .isFalse();
        var b =
                unitizar(pedido, 1, List.of(nova("5", "BOA")))
                        .get("unidades")
                        .get(0)
                        .get("unidade");
        var dados = reagrupar(a, b);
        var agrupadas = resposta(post(reagrupamento(pedido, a), dados, supervisor), 200);
        assertThat(resposta(post(reagrupamento(pedido, a), dados, supervisor), 200))
                .isEqualTo(agrupadas);
        var fotoReplay = D30FotografiaFisica.capturar(jdbc);
        resposta(post(reagrupamento(pedido, a), dados, operador), 403);
        assertThat(D30FotografiaFisica.capturar(jdbc)).isEqualTo(fotoReplay);
        var destino = agrupadas.get("unidades").get(0).get("unidade");
        var consumida = agrupadas.get("unidades").get(1).get("unidade");
        assertThat(destino.get("codigo")).isEqualTo(a.get("codigo"));
        assertThat(destino.get("quantidade").decimalValue()).isEqualByComparingTo("10");
        assertThat(consumida.get("ativa").booleanValue()).isFalse();
        assertThat(consumida.get("quantidade").decimalValue()).isZero();
        assertThat(agrupadas.get("unidades").get(1).get("origens").size()).isEqualTo(1);
        resposta(get(CODIGOS + b.get("codigo").asString() + "/etiqueta", operador), 409);
        resposta(get(CODIGOS + b.get("codigo").asString(), operador), 200);
        resposta(post(divisao(pedido, consumida), dividir(consumida, "1"), supervisor), 409);
        resposta(post(divisao(pedido, destino), dividir(destino, "6"), supervisor), 200);
        assertThat(saldo()).isEqualByComparingTo("10");
        for (var soma :
                jdbc.queryForList(
                        "select entrada_id, sum(quantidade) as quantidade from wms.conteudo_unidade group by entrada_id")) {
            assertThat((BigDecimal) soma.get("quantidade")).isEqualByComparingTo("5");
        }
        var registros = resposta(get(BASE + pedido + "/unidades?tamanho=1", operador), 200);
        assertThat(registros.get("totalItens").longValue()).isEqualTo(3);
        assertThat(registros.get("itens").size()).isEqualTo(1);
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "PRODUTO",
                "NOTA",
                "LOTE",
                "VALIDADE",
                "CHEGADA",
                "EMBALAGEM",
                "TIPO",
                "CONDICAO"
            })
    void reagrupamentoRejeitaOrigensIncompativeis(String diferenca) throws Exception {
        long pedido = receberPar(diferenca);
        var a =
                unitizar(pedido, 0, List.of(nova("5", "BOA")))
                        .get("unidades")
                        .get(0)
                        .get("unidade");
        var unidadeB = new HashMap<>(nova("5", diferenca.equals("CONDICAO") ? "AVARIADA" : "BOA"));
        if (diferenca.equals("PRODUTO"))
            unidadeB.put("embalagemId", embalagens.findAll().get(1).getId());
        if (diferenca.equals("EMBALAGEM"))
            unidadeB.put("embalagemId", embalagem(produto, "OUTRA", "5").getId());
        if (diferenca.equals("TIPO")) unidadeB.put("tipo", "BOBINA");
        var b = unitizar(pedido, 1, List.of(unidadeB)).get("unidades").get(0).get("unidade");
        if (diferenca.equals("LOTE")) {
            assertThat(a.get("controlaLote").booleanValue()).isFalse();
            assertThat(b.get("controlaLote").booleanValue()).isFalse();
            assertThat(a.get("lote").asString()).isEqualTo("LOTE-A");
            assertThat(b.get("lote").asString()).isEqualTo("LOTE-B");
        }
        if (diferenca.equals("CHEGADA")) assertThat(a.get("dataFifo")).isEqualTo(b.get("dataFifo"));
        resposta(post(reagrupamento(pedido, a), reagrupar(a, b), supervisor), 409);
        assertThat(saldo()).isEqualByComparingTo("10");
        assertThat(
                        jdbc.queryForObject(
                                "select count(*) from wms.unidade_logistica where ativa=true",
                                Integer.class))
                .isEqualTo(2);
        assertThat(contar("operacao_unidade")).isEqualTo(2);
    }

    @Test
    void reagrupamentoRejeitaOrigemDuplicadaDestinoEOutroPedido() throws Exception {
        long pedido = receber("10", "0");
        var lista =
                unitizar(pedido, 0, List.of(nova("5", "BOA"), nova("5", "BOA"))).get("unidades");
        var a = lista.get(0).get("unidade");
        var b = lista.get(1).get("unidade");
        var dados = new HashMap<>(reagrupar(a, b));
        dados.put("origens", List.of(revisao(b), revisao(b)));
        resposta(post(reagrupamento(pedido, a), dados, supervisor), 400);
        resposta(post(reagrupamento(pedido, a), reagrupar(a, a), supervisor), 400);
        long outro = receber("10", "0");
        var c =
                unitizar(outro, 0, List.of(nova("10", "BOA")))
                        .get("unidades")
                        .get(0)
                        .get("unidade");
        resposta(post(reagrupamento(pedido, a), reagrupar(a, c), supervisor), 404);
        assertThat(saldo()).isEqualByComparingTo("20");
    }

    @Test
    void operadorUnitizaMasNaoDivideNemReagrupaEServicoValidaSemHttp() throws Exception {
        long pedido = receber("10", "0");
        var lista =
                unitizar(pedido, 0, List.of(nova("5", "BOA"), nova("5", "BOA"))).get("unidades");
        var a = lista.get(0).get("unidade");
        var b = lista.get(1).get("unidade");
        resposta(post(divisao(pedido, a), dividir(a, "1"), operador), 403);
        resposta(post(reagrupamento(pedido, a), reagrupar(a, b), operador), 403);
        var contexto = SecurityContextHolder.createEmptyContext();
        contexto.setAuthentication(new JwtAuthenticationToken(decoder.decode(operador), List.of()));
        SecurityContextHolder.setContext(contexto);
        try {
            assertThatThrownBy(() -> service.unitizar(pedido, 1L, null))
                    .isInstanceOf(ConstraintViolationException.class);
            assertThatThrownBy(
                            () ->
                                    service.dividir(
                                            pedido,
                                            a.get("id").longValue(),
                                            new UnidadeLogisticaDto.Dividir(
                                                    UUID.randomUUID(),
                                                    0L,
                                                    BigDecimal.ONE,
                                                    "Divisao solicitada")))
                    .isInstanceOf(AccessDeniedException.class);
        } finally {
            SecurityContextHolder.clearContext();
        }
        assertThat(contar("unidade_logistica")).isEqualTo(2);
    }

    @Test
    void alcanceClienteEArmazemTambemProtegeCodigoEtiquetaERepeticao() throws Exception {
        long pedido = receber("10", "0");
        String rota = unitizacao(pedido, entrada(pedido, 0).get("id").longValue());
        var dados = dados(pedido, List.of(nova("10", "BOA")));
        var u = resposta(post(rota, dados, operador), 200).get("unidades").get(0).get("unidade");
        for (String restrito :
                List.of(
                        token("OPERACAO", List.of(cliente.getId()), List.of()),
                        token("OPERACAO", List.of(), List.of(armazem.getId())))) {
            resposta(get(BASE + pedido + "/unidades", restrito), 403);
            resposta(get(BASE + pedido + "/unitizacao", restrito), 403);
            resposta(get(CODIGOS + u.get("codigo").asString(), restrito), 403);
            resposta(get(CODIGOS + u.get("codigo").asString() + "/etiqueta", restrito), 403);
            resposta(post(rota, dados, restrito), 403);
        }
        resposta(get(BASE + pedido + "/unidades", null), 401);
        resposta(get(BASE + pedido + "/unidades?tamanho=101", operador), 400);
        resposta(get(CODIGOS + "codigo-invalido", operador), 400);
        resposta(get(CODIGOS + UUID.randomUUID(), operador), 404);
        resposta(get(CODIGOS + u.get("codigo").asString(), gestor), 200);
    }

    @Test
    void cadastrosEmEncerramentoImpedemNovasTransformacoesMasPreservamEtiqueta() throws Exception {
        long pedido = receber("10", "0");
        var u =
                unitizar(pedido, 0, List.of(nova("10", "BOA")))
                        .get("unidades")
                        .get(0)
                        .get("unidade");
        resposta(
                post(
                        "/api/v1/embalagens/" + embalagem.getId() + "/encerramento",
                        Map.of("versao", 0, "motivo", "Encerramento cadastral de teste"),
                        gestor),
                200);
        resposta(post(divisao(pedido, u), dividir(u, "1"), supervisor), 409);
        resposta(get(CODIGOS + u.get("codigo").asString() + "/etiqueta", operador), 200);
        assertThat(saldo()).isEqualByComparingTo("10");
    }

    @Test
    void falhaNaAuditoriaReverteUnidadesOrigensMarcacaoEOperacao() throws Exception {
        long pedido = receber("10", "0");
        long anterior = versao(pedido);
        String rota = unitizacao(pedido, entrada(pedido, 0).get("id").longValue());
        var dados = dados(pedido, List.of(nova("10", "BOA")));
        jdbc.execute(
                "alter table wms.auditoria_cadastro add constraint teste_falha_unidade check (acao <> 'UNIDADES_CRIADAS')");
        try {
            resposta(post(rota, dados, operador), 409);
        } finally {
            jdbc.execute("alter table wms.auditoria_cadastro drop constraint teste_falha_unidade");
        }
        assertThat(contar("unidade_logistica")).isZero();
        assertThat(contar("conteudo_unidade")).isZero();
        assertThat(contar("operacao_unidade")).isZero();
        assertThat(entrada(pedido, 0).get("unitizadaEm").isNull()).isTrue();
        assertThat(versao(pedido)).isEqualTo(anterior);
        resposta(post(rota, dados, operador), 200);
    }

    @Test
    void falhaNoRegistroDeRepeticaoReverteDivisaoEAuditoria() throws Exception {
        long pedido = receber("10", "0");
        var u =
                unitizar(pedido, 0, List.of(nova("10", "BOA")))
                        .get("unidades")
                        .get(0)
                        .get("unidade");
        int auditorias = contar("auditoria_cadastro");
        long anterior = versao(pedido);
        var dados = dividir(u, "4");
        jdbc.execute(
                "alter table wms.operacao_unidade add constraint teste_falha_operacao check (tipo <> 'UNIDADE_DIVIDIDA')");
        try {
            resposta(post(divisao(pedido, u), dados, supervisor), 409);
        } finally {
            jdbc.execute("alter table wms.operacao_unidade drop constraint teste_falha_operacao");
        }
        assertThat(contar("unidade_logistica")).isEqualTo(1);
        assertThat(contar("conteudo_unidade")).isEqualTo(1);
        assertThat(contar("auditoria_cadastro")).isEqualTo(auditorias);
        assertThat(contar("operacao_unidade")).isEqualTo(1);
        assertThat(versao(pedido)).isEqualTo(anterior);
        assertThat(saldo()).isEqualByComparingTo("10");
        resposta(post(divisao(pedido, u), dados, supervisor), 200);
    }

    @Test
    void divisoesConcorrentesRespeitamVersaoENaoPerdemQuantidade() throws Exception {
        long pedido = receber("10", "0");
        var u =
                unitizar(pedido, 0, List.of(nova("10", "BOA")))
                        .get("unidades")
                        .get(0)
                        .get("unidade");
        assertThat(
                        paralelo(
                                () -> post(divisao(pedido, u), dividir(u, "4"), supervisor),
                                () -> post(divisao(pedido, u), dividir(u, "4"), supervisor)))
                .containsExactlyInAnyOrder(200, 409);
        assertThat(contar("unidade_logistica")).isEqualTo(2);
        assertThat(saldo()).isEqualByComparingTo("10");
    }

    @Test
    void pesoUsaPrecisaoDoProdutoSemArredondar() throws Exception {
        produto = produto("PESO", TipoQuantidade.MEDIDA, 3);
        embalagem = embalagem(produto, "BOBINA", "1.250");
        long pedido = receber("1.250", "0");
        var u =
                unitizar(pedido, 0, List.of(nova("1.250", "BOA")))
                        .get("unidades")
                        .get(0)
                        .get("unidade");
        resposta(post(divisao(pedido, u), dividir(u, "0.0001"), supervisor), 400);
        var dividido = resposta(post(divisao(pedido, u), dividir(u, "0.125"), supervisor), 200);
        assertThat(dividido.get("unidades").get(0).get("unidade").get("quantidade").decimalValue())
                .isEqualByComparingTo("1.125");
        assertThat(saldo()).isEqualByComparingTo("1.250");
    }

    @Test
    void cemUnidadesSaoPersistidasComAuditoriaCompletaESemTruncamento() throws Exception {
        long pedido = receber("100", "0");
        var lista = IntStream.range(0, 100).mapToObj(i -> nova("1", "BOA")).toList();
        var resultado = unitizar(pedido, 0, lista);
        assertThat(resultado.get("unidades").size()).isEqualTo(100);
        var auditoria =
                jdbc.queryForObject(
                        "select dados_depois from wms.auditoria_cadastro where acao='UNIDADES_CRIADAS'",
                        String.class);
        assertThat(mapper.readTree(auditoria).get("evento").get("depois").size()).isEqualTo(100);
        assertThat(saldo()).isEqualByComparingTo("100");
    }

    @Test
    void reagrupamentosConcorrentesNaoConsomemAMesmaOrigemDuasVezes() throws Exception {
        long pedido = receber("15", "0");
        var lista =
                unitizar(pedido, 0, List.of(nova("5", "BOA"), nova("5", "BOA"), nova("5", "BOA")))
                        .get("unidades");
        var a = lista.get(0).get("unidade");
        var b = lista.get(1).get("unidade");
        var origem = lista.get(2).get("unidade");
        assertThat(
                        paralelo(
                                () ->
                                        post(
                                                reagrupamento(pedido, a),
                                                reagrupar(a, origem),
                                                supervisor),
                                () ->
                                        post(
                                                reagrupamento(pedido, b),
                                                reagrupar(b, origem),
                                                supervisor)))
                .containsExactlyInAnyOrder(200, 409);
        assertThat(saldo()).isEqualByComparingTo("15");
        assertThat(contar("operacao_unidade")).isEqualTo(2);
        assertThat(
                        jdbc.queryForObject(
                                "select count(*) from wms.unidade_logistica where ativa=true",
                                Integer.class))
                .isEqualTo(2);
        assertThat(
                        jdbc.queryForObject(
                                "select sum(quantidade) from wms.conteudo_unidade",
                                BigDecimal.class))
                .isEqualByComparingTo("15");
    }

    @Test
    void falhaNoReagrupamentoPreservaQuantidadesOrigensVersoesEIdentidades() throws Exception {
        long pedido = receberPar("IGUAIS");
        var a =
                unitizar(pedido, 0, List.of(nova("5", "BOA")))
                        .get("unidades")
                        .get(0)
                        .get("unidade");
        var b =
                unitizar(pedido, 1, List.of(nova("5", "BOA")))
                        .get("unidades")
                        .get(0)
                        .get("unidade");
        long anterior = versao(pedido);
        int auditorias = contar("auditoria_cadastro");
        var dados = reagrupar(a, b);
        jdbc.execute(
                "alter table wms.auditoria_cadastro add constraint teste_falha_reagrupar check (acao <> 'UNIDADES_REAGRUPADAS')");
        try {
            resposta(post(reagrupamento(pedido, a), dados, supervisor), 409);
        } finally {
            jdbc.execute(
                    "alter table wms.auditoria_cadastro drop constraint teste_falha_reagrupar");
        }
        assertThat(contar("conteudo_unidade")).isEqualTo(2);
        assertThat(contar("operacao_unidade")).isEqualTo(2);
        assertThat(contar("auditoria_cadastro")).isEqualTo(auditorias);
        assertThat(versao(pedido)).isEqualTo(anterior);
        for (var unidade : List.of(a, b)) {
            var atual =
                    resposta(get(CODIGOS + unidade.get("codigo").asString(), operador), 200)
                            .get("unidade");
            assertThat(atual.get("versao").longValue()).isZero();
            assertThat(atual.get("quantidade").decimalValue()).isEqualByComparingTo("5");
            assertThat(atual.get("ativa").booleanValue()).isTrue();
        }
        resposta(post(reagrupamento(pedido, a), dados, supervisor), 200);
        assertThat(saldo()).isEqualByComparingTo("10");
    }

    private Produto produto(String sku, TipoQuantidade tipo, int precisao) {
        return produtos.saveAndFlush(
                new Produto(
                        cliente,
                        sku,
                        "Produto ficticio",
                        tipo == TipoQuantidade.MEDIDA ? "KG" : "UN",
                        tipo,
                        precisao,
                        false,
                        false,
                        null,
                        Instant.now()));
    }

    private Embalagem embalagem(Produto produto, String codigo, String quantidade) {
        return embalagens.saveAndFlush(
                new Embalagem(
                        produto,
                        codigo,
                        "Embalagem ficticia",
                        new BigDecimal(quantidade),
                        Instant.now()));
    }

    @Test
    void d30CentoEUmaUnidadesRecusamAntesDaUnitizacao() throws Exception {
        long pedido = receber("101", "0");
        var e = entrada(pedido, 0);
        var lista = IntStream.range(0, 101).mapToObj(i -> nova("1", "BOA")).toList();
        var antes = D30FotografiaFisica.capturar(jdbc);
        resposta(
                post(unitizacao(pedido, e.get("id").longValue()), dados(pedido, lista), operador),
                400);
        assertThat(D30FotografiaFisica.capturar(jdbc)).isEqualTo(antes);
        assertThat(contar("unidade_logistica")).isZero();
    }

    @ParameterizedTest
    @ValueSource(ints = {20, 21})
    void d30LimiteDeOrigensNaoIncluiDestinoEContaEncerradas(int origens) throws Exception {
        long pedido = receber(String.valueOf(origens + 1), "0");
        var resultado =
                unitizar(
                        pedido,
                        0,
                        IntStream.range(0, origens + 1).mapToObj(i -> nova("1", "BOA")).toList());
        var lista = resultado.get("unidades");
        var destino = lista.get(0).get("unidade");
        var revisoes = new ArrayList<Map<String, Object>>();
        for (int i = 1; i <= origens; i++) revisoes.add(revisao(lista.get(i).get("unidade")));
        var comando =
                Map.of(
                        "operacaoId",
                        UUID.randomUUID(),
                        "versaoDestino",
                        destino.get("versao").longValue(),
                        "origens",
                        revisoes,
                        "motivo",
                        "Fronteira local D30 preserva origem");
        var antes = D30FotografiaFisica.capturar(jdbc);
        var retorno = post(reagrupamento(pedido, destino), comando, supervisor);
        if (origens == 21) {
            resposta(retorno, 400);
            assertThat(D30FotografiaFisica.capturar(jdbc)).isEqualTo(antes);
        } else {
            var confirmado = resposta(retorno, 200);
            assertThat(confirmado.get("unidades").size()).isEqualTo(21);
            assertThat(
                            jdbc.queryForObject(
                                    "select quantidade from wms.unidade_logistica where id=?",
                                    BigDecimal.class,
                                    destino.get("id").longValue()))
                    .isEqualByComparingTo("21");
            assertThat(
                            jdbc.queryForObject(
                                    "select count(*) from wms.unidade_logistica where pedido_id=? and ativa=false and quantidade=0",
                                    Integer.class,
                                    pedido))
                    .isEqualTo(20);
            assertThat(d30Unidades.countByPedidoId(pedido)).isEqualTo(21);
            assertThat(saldo()).isEqualByComparingTo("21");
            for (String tabela :
                    List.of(
                            "NOTA_ENTRADA",
                            "ITEM_NOTA_ENTRADA",
                            "CHEGADA_RECEBIMENTO",
                            "ITEM_CHEGADA",
                            "ENTRADA_CONFERIDA"))
                assertThat(D30FotografiaFisica.capturar(jdbc).get(tabela))
                        .isEqualTo(antes.get(tabela));
        }
    }

    private long criarPedido() throws Exception {
        return resposta(
                        post(
                                "/api/v1/pedidos-entrada",
                                Map.of(
                                        "clienteId",
                                        cliente.getId(),
                                        "armazemId",
                                        armazem.getId(),
                                        "referencia",
                                        "PED-" + UUID.randomUUID().toString().substring(0, 20)),
                                operador),
                        201)
                .get("id")
                .longValue();
    }

    private long receber(String boa, String avariada) throws Exception {
        long pedido = criarPedido();
        incluirNota(
                pedido,
                List.of(
                        itemNota(
                                1,
                                produto.getId(),
                                new BigDecimal(boa)
                                        .add(new BigDecimal(avariada))
                                        .toPlainString())));
        var item =
                detalhePedido(pedido).get("notas").get(0).get("itens").get(0).get("id").longValue();
        iniciar(pedido);
        chegada(pedido, DIA1, List.of(itemFisico(item, boa, avariada)));
        efetivar(pedido);
        return pedido;
    }

    private long receberPar(String diferenca) throws Exception {
        long pedido = criarPedido();
        long produtoB = produto.getId();
        if (diferenca.equals("PRODUTO")) {
            var segundo = produto("SKU-02", TipoQuantidade.CONTAGEM, 0);
            produtoB = segundo.getId();
            embalagem(segundo, "DUN-02", "5");
        }
        incluirNota(
                pedido,
                diferenca.equals("NOTA")
                        ? List.of(itemNota(1, produto.getId(), "5"))
                        : List.of(itemNota(1, produto.getId(), "5"), itemNota(2, produtoB, "5")));
        if (diferenca.equals("NOTA")) incluirNota(pedido, List.of(itemNota(1, produtoB, "5")));
        var notas = detalhePedido(pedido).get("notas");
        long itemA = notas.get(0).get("itens").get(0).get("id").longValue();
        long itemB =
                diferenca.equals("NOTA")
                        ? notas.get(1).get("itens").get(0).get("id").longValue()
                        : notas.get(0).get("itens").get(1).get("id").longValue();
        iniciar(pedido);
        var a = new HashMap<>(itemFisico(itemA, "5", "0"));
        var b =
                new HashMap<>(
                        itemFisico(
                                itemB,
                                diferenca.equals("CONDICAO") ? "0" : "5",
                                diferenca.equals("CONDICAO") ? "5" : "0"));
        if (diferenca.equals("LOTE")) {
            a.put("lote", "LOTE-A");
            b.put("lote", "LOTE-B");
        }
        if (diferenca.equals("VALIDADE")) {
            a.put("validade", "2027-01-01");
            b.put("validade", "2027-02-01");
        }
        if (diferenca.equals("CHEGADA")) {
            chegada(pedido, DIA1, List.of(a));
            chegada(pedido, DIA2, List.of(b));
        } else chegada(pedido, DIA1, List.of(a, b));
        efetivar(pedido);
        return pedido;
    }

    private Map<String, Object> itemNota(int numero, long produtoId, String quantidade) {
        return Map.of(
                "numeroItem", numero, "produtoId", produtoId, "quantidadePrevista", quantidade);
    }

    private void incluirNota(long pedido, List<Map<String, Object>> itens) throws Exception {
        resposta(
                post(
                        BASE + pedido + "/notas",
                        Map.of(
                                "versao",
                                versao(pedido),
                                "serie",
                                1,
                                "numero",
                                ++numeroNota,
                                "emissao",
                                "2026-09-01",
                                "itens",
                                itens),
                        operador),
                200);
    }

    private void iniciar(long pedido) throws Exception {
        resposta(
                post(
                        BASE + pedido + "/iniciar-conferencia",
                        Map.of("versao", versao(pedido), "motivo", "Conferencia ficticia iniciada"),
                        operador),
                200);
    }

    private Map<String, Object> itemFisico(long id, String boa, String avariada) {
        return Map.of("itemNotaId", id, "quantidadeBoa", boa, "quantidadeAvariada", avariada);
    }

    private void chegada(long pedido, String data, List<Map<String, Object>> itens)
            throws Exception {
        resposta(
                post(
                        BASE + pedido + "/chegadas",
                        Map.of(
                                "versao",
                                versao(pedido),
                                "operacaoId",
                                UUID.randomUUID(),
                                "chegouEm",
                                data,
                                "observacao",
                                "Conferencia ficticia realizada",
                                "itens",
                                itens),
                        operador),
                200);
    }

    private void efetivar(long pedido) throws Exception {
        resposta(
                post(
                        BASE + pedido + "/efetivacao",
                        Map.of(
                                "versao",
                                versao(pedido),
                                "motivo",
                                "Quantidade real aceita na conferencia",
                                "aceitarDivergencias",
                                true),
                        supervisor),
                200);
    }

    private JsonNode entrada(long pedido, int indice) throws Exception {
        return resposta(get(BASE + pedido + "/entradas", operador), 200).get("itens").get(indice);
    }

    private JsonNode detalhePedido(long pedido) throws Exception {
        return resposta(get(BASE + pedido, operador), 200);
    }

    private long versao(long pedido) throws Exception {
        return detalhePedido(pedido).get("pedido").get("versao").longValue();
    }

    @Test
    void d30HashUnitizacaoDistingueAcaoRecursoVersoesEOrdemDasListas() throws Exception {
        long pedido = receber("10", "0");
        long entradaId = entrada(pedido, 0).get("id").longValue();
        var dados = dados(pedido, List.of(nova("5", "BOA"), nova("3", "BOA"), nova("2", "BOA")));
        String payload =
                "{\"operacaoId\":\""
                        + dados.get("operacaoId")
                        + "\",\"versaoPedido\":"
                        + dados.get("versaoPedido")
                        + ",\"motivo\":\"Organizacao fisica confirmada\",\"unidades\":["
                        + d30LiteralNova("5")
                        + ","
                        + d30LiteralNova("3")
                        + ","
                        + d30LiteralNova("2")
                        + "]}";
        String esperado = d30Sha("UNITIZAR:" + entradaId + ":" + payload);
        var resultado = resposta(post(unitizacao(pedido, entradaId), dados, operador), 200);
        String uuid = dados.get("operacaoId").toString();
        var op = jdbc.queryForMap("select * from wms.operacao_unidade where operacao_id=?", uuid);
        assertThat(op.get("PEDIDO_ID")).isEqualTo(pedido);
        assertThat(op.get("TIPO")).isEqualTo("UNIDADES_CRIADAS");
        assertThat(op.get("CONTEUDO_HASH")).isEqualTo(esperado);
        assertThat(mapper.readTree(op.get("RESULTADO").toString())).isEqualTo(resultado);
        var foto = D30FotografiaFisica.capturar(jdbc);
        var alterada = new HashMap<String, Object>(dados);
        alterada.put("versaoPedido", ((Number) dados.get("versaoPedido")).longValue() + 1);
        resposta(post(unitizacao(pedido, entradaId), alterada, operador), 409);
        alterada = new HashMap<>(dados);
        alterada.put("unidades", List.of(nova("2", "BOA"), nova("3", "BOA"), nova("5", "BOA")));
        resposta(post(unitizacao(pedido, entradaId), alterada, operador), 409);
        resposta(post(unitizacao(pedido, entradaId + 99999), dados, operador), 409);
        var unidades = resultado.get("unidades");
        var destino = unidades.get(0).get("unidade");
        var outraAcao = new HashMap<String, Object>(dividir(destino, "2"));
        outraAcao.put("operacaoId", dados.get("operacaoId"));
        resposta(post(divisao(pedido, destino), outraAcao, supervisor), 409);
        assertThat(D30FotografiaFisica.capturar(jdbc)).isEqualTo(foto);
        assertThat(resposta(post(unitizacao(pedido, entradaId), dados, operador), 200))
                .isEqualTo(resultado);
        assertThat(D30FotografiaFisica.capturar(jdbc)).isEqualTo(foto);
        var a = unidades.get(1).get("unidade");
        var b = unidades.get(2).get("unidade");
        var agrupar = new HashMap<String, Object>(reagrupar(destino, a));
        agrupar.put("origens", List.of(revisao(a), revisao(b)));
        var agrupada = resposta(post(reagrupamento(pedido, destino), agrupar, supervisor), 200);
        var fotoAgrupada = D30FotografiaFisica.capturar(jdbc);
        var invertida = new HashMap<String, Object>(agrupar);
        invertida.put("origens", List.of(revisao(b), revisao(a)));
        resposta(post(reagrupamento(pedido, destino), invertida, supervisor), 409);
        var v = new HashMap<String, Object>(revisao(a));
        v.put("versao", a.get("versao").longValue() + 1);
        invertida = new HashMap<>(agrupar);
        invertida.put("origens", List.of(v, revisao(b)));
        resposta(post(reagrupamento(pedido, destino), invertida, supervisor), 409);
        invertida = new HashMap<>(agrupar);
        invertida.put("versaoDestino", destino.get("versao").longValue() + 1);
        resposta(post(reagrupamento(pedido, destino), invertida, supervisor), 409);
        assertThat(resposta(post(reagrupamento(pedido, destino), agrupar, supervisor), 200))
                .isEqualTo(agrupada);
        assertThat(D30FotografiaFisica.capturar(jdbc)).isEqualTo(fotoAgrupada);
    }

    private String d30LiteralNova(String quantidade) {
        return "{\"embalagemId\":"
                + embalagem.getId()
                + ",\"tipo\":\"PALLET\",\"condicao\":\"BOA\",\"quantidade\":"
                + quantidade
                + "}";
    }

    private String d30Sha(String texto) throws Exception {
        return java.util.HexFormat.of()
                .formatHex(
                        java.security.MessageDigest.getInstance("SHA-256")
                                .digest(texto.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
    }

    private Map<String, Object> nova(String quantidade, String condicao) {
        return Map.of(
                "embalagemId",
                embalagem.getId(),
                "tipo",
                "PALLET",
                "condicao",
                condicao,
                "quantidade",
                quantidade);
    }

    private Map<String, Object> dados(long pedido, List<Map<String, Object>> unidades)
            throws Exception {
        return Map.of(
                "operacaoId",
                UUID.randomUUID(),
                "versaoPedido",
                versao(pedido),
                "motivo",
                "Organizacao fisica confirmada",
                "unidades",
                unidades);
    }

    private String unitizacao(long pedido, long entrada) {
        return BASE + pedido + "/entradas/" + entrada + "/unitizacao";
    }

    private JsonNode unitizar(long pedido, int indice, List<Map<String, Object>> unidades)
            throws Exception {
        return resposta(
                post(
                        unitizacao(pedido, entrada(pedido, indice).get("id").longValue()),
                        dados(pedido, unidades),
                        operador),
                200);
    }

    private String divisao(long pedido, JsonNode unidade) {
        return BASE + pedido + "/unidades/" + unidade.get("id").longValue() + "/divisao";
    }

    private Map<String, Object> dividir(JsonNode unidade, String quantidade) {
        return Map.of(
                "operacaoId",
                UUID.randomUUID(),
                "versao",
                unidade.get("versao").longValue(),
                "quantidadeNovaUnidade",
                quantidade,
                "motivo",
                "Divisao fisica confirmada");
    }

    private String reagrupamento(long pedido, JsonNode unidade) {
        return BASE + pedido + "/unidades/" + unidade.get("id").longValue() + "/reagrupamento";
    }

    private Map<String, Object> revisao(JsonNode unidade) {
        return Map.of(
                "unidadeId",
                unidade.get("id").longValue(),
                "versao",
                unidade.get("versao").longValue());
    }

    private Map<String, Object> reagrupar(JsonNode destino, JsonNode origem) {
        return Map.of(
                "operacaoId",
                UUID.randomUUID(),
                "versaoDestino",
                destino.get("versao").longValue(),
                "origens",
                List.of(revisao(origem)),
                "motivo",
                "Reagrupamento fisico confirmado");
    }

    private int contar(String tabela) {
        return jdbc.queryForObject("select count(*) from wms." + tabela, Integer.class);
    }

    private BigDecimal saldo() {
        return jdbc.queryForObject(
                "select sum(quantidade) from wms.unidade_logistica", BigDecimal.class);
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
        return client.send(
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
        Instant agora = Instant.now();
        var claims =
                JwtClaimsSet.builder()
                        .issuer("https://identidade.test.invalid")
                        .subject("unitizador-teste")
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
