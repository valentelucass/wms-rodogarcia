package br.com.rodogarcia.wms;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.rodogarcia.wms.config.IdentidadeTesteConfig;
import br.com.rodogarcia.wms.dto.PedidoEntradaDto;
import br.com.rodogarcia.wms.models.Armazem;
import br.com.rodogarcia.wms.models.Cliente;
import br.com.rodogarcia.wms.models.Produto;
import br.com.rodogarcia.wms.models.TipoQuantidade;
import br.com.rodogarcia.wms.repositories.ArmazemRepository;
import br.com.rodogarcia.wms.repositories.ClienteRepository;
import br.com.rodogarcia.wms.repositories.ProdutoRepository;
import br.com.rodogarcia.wms.services.RecebimentoService;
import jakarta.validation.ConstraintViolationException;
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
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
                "spring.datasource.url=jdbc:h2:mem:wms-recebimento;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000;INIT=CREATE SCHEMA IF NOT EXISTS wms")
@Import(IdentidadeTesteConfig.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class RecebimentoIntegrationTest {
    private static final String DIA1 = "2026-09-01T12:00:00Z";
    private static final String DIA2 = "2026-09-02T12:00:00Z";
    private final HttpClient client =
            HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    @Autowired private Environment environment;
    @Autowired private JsonMapper mapper;
    @Autowired private JwtEncoder encoder;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ClienteRepository clientes;
    @Autowired private ArmazemRepository armazens;
    @Autowired private ProdutoRepository produtos;
    @Autowired private RecebimentoService recebimento;
    @Autowired private JwtDecoder decoder;
    private Cliente cliente;
    private Armazem armazem;
    private Produto produto;
    private String supervisor;
    private String operador;

    @Test
    void servicoRecusaDadosInvalidosEOperadorMesmoSemController() throws Exception {
        long id = prepararPedido("10");
        chegada(id, itemId(id), "10", "0", DIA1);
        long atual = versao(id);
        var contexto = SecurityContextHolder.createEmptyContext();
        contexto.setAuthentication(new JwtAuthenticationToken(decoder.decode(operador), List.of()));
        SecurityContextHolder.setContext(contexto);
        try {
            assertThatThrownBy(() -> recebimento.efetivar(id, null))
                    .isInstanceOf(ConstraintViolationException.class);
            assertThatThrownBy(
                            () ->
                                    recebimento.efetivar(
                                            id,
                                            new PedidoEntradaDto.Efetivar(
                                                    atual, false, "Tratativa com o cliente")))
                    .isInstanceOf(AccessDeniedException.class);
        } finally {
            SecurityContextHolder.clearContext();
        }
        assertThat(contar("entrada_conferida")).isZero();
    }

    @Test
    void chegadasDistintasComMesmaVersaoNaoSobrescrevemConferencia() throws Exception {
        long id = prepararPedido("10");
        long atual = versao(id);
        long item = itemId(id);
        assertThat(
                        paralelo(
                                () ->
                                        post(
                                                id + "/chegadas",
                                                dadosChegada(atual, item, "5", "0", DIA1),
                                                operador)))
                .containsExactlyInAnyOrder(200, 409);
        assertThat(contar("chegada_recebimento")).isEqualTo(1);
    }

    @BeforeEach
    void preparar() {
        for (String tabela :
                List.of(
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
        produto =
                produtos.saveAndFlush(
                        new Produto(
                                cliente,
                                "SKU-01",
                                "Produto ficticio",
                                "UN",
                                TipoQuantidade.CONTAGEM,
                                0,
                                false,
                                false,
                                null,
                                agora));
        supervisor = token("SUPERVISOR", List.of(cliente.getId()), List.of(armazem.getId()));
        operador = token("OPERACAO", List.of(cliente.getId()), List.of(armazem.getId()));
    }

    @Test
    void duasChegadasMantemFifoDaNotaEEntradaUnicaSemSaldoDisponivel() throws Exception {
        long id = prepararPedido("100");
        long item = itemId(id);
        chegada(id, item, "40", "0", DIA1);
        assertThat(detalhe(id).get("pedido").get("situacao").asString()).isEqualTo("QUARENTENA");
        assertThat(contar("entrada_conferida")).isZero();
        chegada(id, item, "60", "0", DIA2);
        assertThat(detalhe(id).get("divergente").booleanValue()).isFalse();
        assertThat(detalhe(id).get("pedido").get("situacao").asString()).isEqualTo("QUARENTENA");
        long versao = versao(id);
        resposta(post(id + "/efetivacao", efetivar(versao, false), supervisor), 200);
        var entradas = resposta(get(id + "/entradas", operador), 200).get("itens");
        assertThat(entradas.size()).isEqualTo(2);
        for (var entrada : entradas) {
            assertThat(entrada.get("dataFifo").asString()).isEqualTo(DIA1);
            assertThat(entrada.get("disponivelParaSaida").booleanValue()).isFalse();
        }
        assertThat(entradas.get(1).get("chegadaReal").asString()).isEqualTo(DIA2);
        assertThat(entradas.get(0).get("quantidadeTriagem").decimalValue())
                .isEqualByComparingTo("40");
        resposta(post(id + "/efetivacao", efetivar(versao, false), supervisor), 409);
        assertThat(contar("entrada_conferida")).isEqualTo(2);
        assertThat(
                        jdbc.queryForObject(
                                "select count(*) from wms.auditoria_cadastro where acao='ENTRADA_EFETIVADA'",
                                Integer.class))
                .isEqualTo(1);
    }

    @ParameterizedTest
    @ValueSource(strings = {"98", "102"})
    void divergenciaPreservaPrevistoERealExigindoAceiteExplicito(String quantidade)
            throws Exception {
        long id = prepararPedido("100");
        chegada(id, itemId(id), quantidade, "0", DIA1);
        var nota = detalhe(id).get("notas").get(0).get("itens").get(0);
        assertThat(nota.get("prevista").decimalValue()).isEqualByComparingTo("100");
        assertThat(nota.get("recebidaBoa").decimalValue()).isEqualByComparingTo(quantidade);
        resposta(post(id + "/efetivacao", efetivar(versao(id), false), supervisor), 409);
        resposta(post(id + "/efetivacao", efetivar(versao(id), true), supervisor), 200);
        assertThat(
                        resposta(get(id + "/entradas", operador), 200)
                                .get("itens")
                                .get(0)
                                .get("quantidadeTriagem")
                                .decimalValue())
                .isEqualByComparingTo(quantidade);
    }

    @Test
    void avariaBloqueiaPedidoInteiroEPreservaQuantidadeEmQuarentena() throws Exception {
        long id = prepararPedido("100");
        chegada(id, itemId(id), "95", "5", DIA1);
        assertThat(detalhe(id).get("pedido").get("situacao").asString()).isEqualTo("QUARENTENA");
        resposta(post(id + "/efetivacao", efetivar(versao(id), false), supervisor), 409);
        resposta(post(id + "/efetivacao", efetivar(versao(id), true), supervisor), 200);
        var entrada = resposta(get(id + "/entradas", supervisor), 200).get("itens").get(0);
        assertThat(entrada.get("quantidadeTriagem").decimalValue()).isEqualByComparingTo("95");
        assertThat(entrada.get("quantidadeQuarentena").decimalValue()).isEqualByComparingTo("5");
    }

    @Test
    void pedidoReuneNotasEQuarentenaNaoContaminaOutroPedido() throws Exception {
        long id = criarPedido();
        adicionarNota(id, 1, "10", produto.getId(), null);
        adicionarNota(id, 2, "20", produto.getId(), null);
        iniciar(id);
        long independente = criarPedido();
        chegada(id, itemId(id), "10", "0", DIA1);
        assertThat(detalhe(id).get("divergente").booleanValue()).isTrue();
        assertThat(detalhe(independente).get("pedido").get("situacao").asString())
                .isEqualTo("RASCUNHO");
        long segundo = detalhe(id).get("notas").get(1).get("itens").get(0).get("id").longValue();
        chegada(id, segundo, "20", "0", DIA2);
        resposta(post(id + "/efetivacao", efetivar(versao(id), false), supervisor), 200);
        var entradas = resposta(get(id + "/entradas", operador), 200).get("itens");
        assertThat(entradas.get(0).get("dataFifo").asString()).isEqualTo(DIA1);
        assertThat(entradas.get(1).get("dataFifo").asString()).isEqualTo(DIA2);
    }

    @Test
    void exigeAlcanceSimultaneoPorClienteEArmazemEmLeiturasEEscritas() throws Exception {
        long id = prepararPedido("10");
        for (String semAlcance :
                List.of(
                        token("SUPERVISOR", List.of(), List.of(armazem.getId())),
                        token("SUPERVISOR", List.of(cliente.getId()), List.of()))) {
            resposta(get(String.valueOf(id), semAlcance), 403);
            resposta(
                    get(
                            "?clienteId=" + cliente.getId() + "&armazemId=" + armazem.getId(),
                            semAlcance),
                    403);
            resposta(get(id + "/chegadas", semAlcance), 403);
            resposta(
                    post(
                            id + "/chegadas",
                            dadosChegada(versao(id), itemId(id), "10", "0", DIA1),
                            semAlcance),
                    403);
        }
        assertThat(contar("chegada_recebimento")).isZero();
    }

    @Test
    void operadorRecebeMasNaoLiberaEstornaOuCancela() throws Exception {
        long id = prepararPedido("10");
        chegada(id, itemId(id), "10", "0", DIA1);
        long chegada =
                resposta(get(id + "/chegadas", operador), 200)
                        .get("itens")
                        .get(0)
                        .get("id")
                        .longValue();
        resposta(post(id + "/efetivacao", efetivar(versao(id), false), operador), 403);
        resposta(
                post(id + "/chegadas/" + chegada + "/estorno", revisao(versao(id)), operador), 403);
        resposta(post(id + "/cancelamento", revisao(versao(id)), operador), 403);
        assertThat(contar("entrada_conferida")).isZero();
    }

    @Test
    void estornoPreservaHistoricoERecalculaPrimeiraChegadaValida() throws Exception {
        long id = prepararPedido("100");
        long item = itemId(id);
        chegada(id, item, "30", "0", DIA1);
        chegada(id, item, "40", "0", DIA2);
        long original =
                resposta(get(id + "/chegadas", supervisor), 200)
                        .get("itens")
                        .get(0)
                        .get("id")
                        .longValue();
        resposta(
                post(id + "/chegadas/" + original + "/estorno", revisao(versao(id)), supervisor),
                200);
        resposta(
                post(id + "/chegadas/" + original + "/estorno", revisao(versao(id)), supervisor),
                409);
        assertThat(detalhe(id).get("notas").get(0).get("primeiraChegada").asString())
                .isEqualTo(DIA2);
        chegada(id, item, "60", "0", "2026-09-03T12:00:00Z");
        resposta(post(id + "/efetivacao", efetivar(versao(id), false), supervisor), 200);
        assertThat(contar("chegada_recebimento")).isEqualTo(3);
        assertThat(contar("item_chegada")).isEqualTo(3);
        assertThat(contar("entrada_conferida")).isEqualTo(2);
        assertThat(
                        resposta(get(id + "/chegadas", supervisor), 200)
                                .get("itens")
                                .get(0)
                                .get("estornadaEm")
                                .isNull())
                .isFalse();
        var chegadaAtiva = resposta(get(id + "/chegadas", supervisor), 200).get("itens").get(1);
        assertThat(chegadaAtiva.get("estornadaEm").isNull()).isTrue();
        var fotoEfetivada = D30FotografiaFisica.capturar(jdbc);
        resposta(
                post(
                        id + "/chegadas/" + chegadaAtiva.get("id").longValue() + "/estorno",
                        revisao(versao(id)),
                        supervisor),
                409);
        assertThat(D30FotografiaFisica.capturar(jdbc)).isEqualTo(fotoEfetivada);
        resposta(
                post(id + "/chegadas/" + original + "/estorno", revisao(versao(id)), supervisor),
                409);
    }

    @Test
    void repeticaoComMesmoIdentificadorNaoDuplicaEConteudoDiferenteConflita() throws Exception {
        long id = prepararPedido("10");
        var dados = dadosChegada(versao(id), itemId(id), "10", "0", DIA1);
        resposta(post(id + "/chegadas", dados, operador), 200);
        long atual = versao(id);
        resposta(post(id + "/chegadas", dados, operador), 200);
        assertThat(versao(id)).isEqualTo(atual);
        assertThat(contar("chegada_recebimento")).isEqualTo(1);
        var modificado = new HashMap<>(dados);
        modificado.put("observacao", "Outro conteudo fisico");
        resposta(post(id + "/chegadas", modificado, operador), 409);
        resposta(post(id + "/efetivacao", efetivar(atual, false), supervisor), 200);
        resposta(post(id + "/chegadas", dados, operador), 200);
        assertThat(contar("entrada_conferida")).isEqualTo(1);
    }

    @Test
    void repeticoesSimultaneasDaMesmaChegadaGravamUmFato() throws Exception {
        long id = prepararPedido("10");
        var dados = dadosChegada(versao(id), itemId(id), "10", "0", DIA1);
        assertThat(paralelo(() -> post(id + "/chegadas", dados, operador)))
                .containsExactlyInAnyOrder(200, 200);
        assertThat(contar("chegada_recebimento")).isEqualTo(1);
        assertThat(
                        jdbc.queryForObject(
                                "select count(*) from wms.auditoria_cadastro where acao='CHEGADA_REGISTRADA'",
                                Integer.class))
                .isEqualTo(1);
    }

    @Test
    void efetivacoesSimultaneasGeramUmaUnicaEntrada() throws Exception {
        long id = prepararPedido("10");
        chegada(id, itemId(id), "10", "0", DIA1);
        var comando = efetivar(versao(id), false);
        assertThat(paralelo(() -> post(id + "/efetivacao", comando, supervisor)))
                .containsExactlyInAnyOrder(200, 409);
        assertThat(contar("entrada_conferida")).isEqualTo(1);
    }

    @Test
    void falhaNaAuditoriaDesfazEntradasEstadoEVersao() throws Exception {
        long id = prepararPedido("10");
        chegada(id, itemId(id), "10", "0", DIA1);
        long anterior = versao(id);
        jdbc.execute(
                "alter table wms.auditoria_cadastro add constraint teste_falha_recebimento check (acao <> 'ENTRADA_EFETIVADA')");
        try {
            resposta(post(id + "/efetivacao", efetivar(anterior, false), supervisor), 409);
        } finally {
            jdbc.execute(
                    "alter table wms.auditoria_cadastro drop constraint teste_falha_recebimento");
        }
        assertThat(contar("entrada_conferida")).isZero();
        assertThat(versao(id)).isEqualTo(anterior);
        assertThat(detalhe(id).get("pedido").get("situacao").asString())
                .isEqualTo("EM_CONFERENCIA");
        resposta(post(id + "/efetivacao", efetivar(anterior, false), supervisor), 200);
    }

    @Test
    void notaNaoPodePertencerADoisPedidosMesmoSemChave() throws Exception {
        long primeiro = criarPedido();
        adicionarNota(primeiro, 1, "10", produto.getId(), null);
        long segundo = criarPedido();
        resposta(
                post(
                        segundo + "/notas",
                        nota(versao(segundo), 1, "10", produto.getId(), null),
                        operador),
                409);
        assertThat(contar("nota_entrada")).isEqualTo(1);
    }

    @Test
    void previsaoCongelaAoIniciarERecebimentoFechaAposEfetivacao() throws Exception {
        long id = prepararPedido("10");
        resposta(
                post(id + "/notas", nota(versao(id), 2, "10", produto.getId(), null), operador),
                409);
        chegada(id, itemId(id), "10", "0", DIA1);
        resposta(post(id + "/efetivacao", efetivar(versao(id), false), supervisor), 200);
        resposta(
                post(
                        id + "/chegadas",
                        dadosChegada(versao(id), itemId(id), "1", "0", DIA2),
                        operador),
                409);
    }

    @Test
    void naoEfetivaSemQuantidadeFisicaENaoCancelaHistoricoFisico() throws Exception {
        long id = prepararPedido("10");
        resposta(post(id + "/efetivacao", efetivar(versao(id), true), supervisor), 400);
        chegada(id, itemId(id), "10", "0", DIA1);
        resposta(post(id + "/cancelamento", revisao(versao(id)), supervisor), 409);
        long chegadaId =
                resposta(get(id + "/chegadas", supervisor), 200)
                        .get("itens")
                        .get(0)
                        .get("id")
                        .longValue();
        resposta(
                post(id + "/chegadas/" + chegadaId + "/estorno", revisao(versao(id)), supervisor),
                200);
        var todas = resposta(get(id + "/chegadas", supervisor), 200).get("itens");
        assertThat(todas.size()).isEqualTo(1);
        assertThat(todas.get(0).get("estornadaEm").isNull()).isFalse();
        var fotoEstornada = D30FotografiaFisica.capturar(jdbc);
        resposta(post(id + "/cancelamento", revisao(versao(id)), supervisor), 409);
        assertThat(D30FotografiaFisica.capturar(jdbc)).isEqualTo(fotoEstornada);
        long vazio = criarPedido();
        var resumoVazio = detalhe(vazio).get("pedido");
        var cancelarVazio = revisao(versao(vazio));
        var fotoVazio = D30FotografiaFisica.capturar(jdbc);
        var cancelado = resposta(post(vazio + "/cancelamento", cancelarVazio, supervisor), 200);
        assertThat(cancelado.get("id").longValue()).isEqualTo(vazio);
        assertThat(cancelado.get("situacao").asString()).isEqualTo("CANCELADO");
        assertThat(cancelado.get("versao").longValue())
                .isEqualTo(resumoVazio.get("versao").longValue() + 1);
        assertThat(detalhe(vazio).get("pedido")).isEqualTo(cancelado);
        var fotoCancelado = D30FotografiaFisica.capturar(jdbc);
        assertThat(fotoVazio).hasSize(64);
        for (String tabela : fotoVazio.keySet()) {
            if (tabela.equals("PEDIDO_ENTRADA") || tabela.equals("AUDITORIA_CADASTRO")) continue;
            assertThat(fotoCancelado.get(tabela)).as(tabela).isEqualTo(fotoVazio.get(tabela));
        }
        assertThat(fotoCancelado.get("PEDIDO_ENTRADA"))
                .hasSameSizeAs(fotoVazio.get("PEDIDO_ENTRADA"));
        assertThat(
                        fotoCancelado.get("PEDIDO_ENTRADA").stream()
                                .filter(x -> ((Number) x.get("ID")).longValue() != vazio)
                                .toList())
                .isEqualTo(
                        fotoVazio.get("PEDIDO_ENTRADA").stream()
                                .filter(x -> ((Number) x.get("ID")).longValue() != vazio)
                                .toList());
        var linhaAntes =
                fotoVazio.get("PEDIDO_ENTRADA").stream()
                        .filter(x -> ((Number) x.get("ID")).longValue() == vazio)
                        .findFirst()
                        .orElseThrow();
        var linhaDepois =
                fotoCancelado.get("PEDIDO_ENTRADA").stream()
                        .filter(x -> ((Number) x.get("ID")).longValue() == vazio)
                        .findFirst()
                        .orElseThrow();
        var dadosAntes = new HashMap<>(linhaAntes);
        var dadosDepois = new HashMap<>(linhaDepois);
        for (String campo : List.of("SITUACAO", "VERSAO", "ALTERADO_EM")) {
            dadosAntes.remove(campo);
            dadosDepois.remove(campo);
        }
        assertThat(dadosDepois).isEqualTo(dadosAntes);
        assertThat(linhaDepois.get("SITUACAO")).isEqualTo("CANCELADO");
        assertThat(((Number) linhaDepois.get("VERSAO")).longValue())
                .isEqualTo(((Number) linhaAntes.get("VERSAO")).longValue() + 1);
        assertThat(fotoCancelado.get("AUDITORIA_CADASTRO"))
                .containsAll(fotoVazio.get("AUDITORIA_CADASTRO"));
        var auditorias =
                fotoCancelado.get("AUDITORIA_CADASTRO").stream()
                        .filter(x -> !fotoVazio.get("AUDITORIA_CADASTRO").contains(x))
                        .toList();
        assertThat(auditorias).hasSize(1);
        var auditoriaCancelamento = auditorias.get(0);
        assertThat(auditoriaCancelamento.get("TIPO")).isEqualTo("PEDIDO_ENTRADA");
        assertThat(((Number) auditoriaCancelamento.get("REGISTRO_ID")).longValue())
                .isEqualTo(vazio);
        assertThat(auditoriaCancelamento.get("ACAO")).isEqualTo("PEDIDO_CANCELADO");
        assertThat(auditoriaCancelamento.get("USUARIO")).isEqualTo("recebedor-teste");
        assertThat(auditoriaCancelamento.get("MOTIVO")).isEqualTo(cancelarVazio.get("motivo"));
        assertThat(mapper.readTree(auditoriaCancelamento.get("DADOS_ANTES").toString()))
                .isEqualTo(resumoVazio);
        var conteudoAuditado =
                mapper.readTree(auditoriaCancelamento.get("DADOS_DEPOIS").toString());
        assertThat(conteudoAuditado.get("pedido")).isEqualTo(cancelado);
        assertThat(conteudoAuditado.get("evento").isObject()).isTrue();
        assertThat(conteudoAuditado.get("evento").size()).isZero();
        resposta(
                post(
                        vazio + "/notas",
                        nota(versao(vazio), 55, "10", produto.getId(), null),
                        operador),
                409);
    }

    @Test
    void revalidaCadastroEmEncerramentoAntesDeEfetivar() throws Exception {
        long id = prepararPedido("10");
        chegada(id, itemId(id), "10", "0", DIA1);
        jdbc.update(
                "update wms.produto set situacao='ENCERRAMENTO_PENDENTE' where id=?",
                produto.getId());
        resposta(post(id + "/efetivacao", efetivar(versao(id), false), supervisor), 409);
        assertThat(contar("entrada_conferida")).isZero();
    }

    @Test
    void loteValidadePrecisaoEItensDeOutroPedidoSaoValidadosAtomicamente() throws Exception {
        produto =
                produtos.saveAndFlush(
                        new Produto(
                                cliente,
                                "LOTE",
                                "Produto controlado",
                                "KG",
                                TipoQuantidade.MEDIDA,
                                3,
                                true,
                                true,
                                30,
                                Instant.now()));
        long id = prepararPedido("1.500");
        long item = itemId(id);
        resposta(
                post(
                        id + "/chegadas",
                        dadosChegada(versao(id), item, "1.500", "0", DIA1),
                        operador),
                400);
        var valido = new HashMap<>(item(item, "1.500", "0"));
        valido.put("lote", "L-01");
        valido.put("validade", "2026-08-01");
        var comando = new HashMap<>(dadosChegada(versao(id), item, "1.500", "0", DIA1));
        comando.put("itens", List.of(valido));
        resposta(post(id + "/chegadas", comando, operador), 200);
        // Validade passada gera dado para alerta futuro, sem bloqueio automatico adicional.
        long outro = criarPedido();
        adicionarNota(outro, 2, "1.500", produto.getId(), null);
        iniciar(outro);
        resposta(
                post(
                        outro + "/chegadas",
                        dadosChegada(versao(outro), item, "1", "0", DIA1),
                        operador),
                400);
        assertThat(contar("chegada_recebimento")).isEqualTo(1);
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "0.5", "-1"})
    void rejeitaQuantidadesInvalidasParaProdutoContadoSemDeixarChegada(String quantidade)
            throws Exception {
        long id = prepararPedido("10");
        resposta(
                post(
                        id + "/chegadas",
                        dadosChegada(versao(id), itemId(id), quantidade, "0", DIA1),
                        operador),
                400);
        assertThat(contar("chegada_recebimento")).isZero();
        assertThat(contar("item_chegada")).isZero();
    }

    @Test
    void rejeitaDataFuturaCamposDesconhecidosEProdutoDeOutroCliente() throws Exception {
        long id = prepararPedido("10");
        resposta(
                post(
                        id + "/chegadas",
                        dadosChegada(
                                versao(id),
                                itemId(id),
                                "10",
                                "0",
                                Instant.now().plusSeconds(3600).toString()),
                        operador),
                400);
        var outro =
                clientes.saveAndFlush(
                        new Cliente("OUTRO", "Outro cliente", "11111111000111", Instant.now()));
        var alheio =
                produtos.saveAndFlush(
                        new Produto(
                                outro,
                                "ALHEIO",
                                "Produto alheio",
                                "UN",
                                TipoQuantidade.CONTAGEM,
                                0,
                                false,
                                false,
                                null,
                                Instant.now()));
        long novo = criarPedido();
        resposta(
                post(novo + "/notas", nota(versao(novo), 2, "10", alheio.getId(), null), operador),
                400);
        var desconhecido = new HashMap<>(revisao(versao(id)));
        desconhecido.put("liberarSaldo", true);
        resposta(post(id + "/cancelamento", desconhecido, supervisor), 400);
    }

    @Test
    void importacaoXmlNaoRegistraChegadaENaoDuplicaNota() throws Exception {
        long id = criarPedido();
        String xml = xml();
        var comando = Map.of("versao", versao(id), "xml", xml);
        resposta(post(id + "/notas/xml", comando, operador), 200);
        long versao = versao(id);
        resposta(post(id + "/notas/xml", comando, operador), 200);
        assertThat(versao(id)).isEqualTo(versao);
        assertThat(contar("nota_entrada")).isEqualTo(1);
        assertThat(contar("chegada_recebimento")).isZero();
        assertThat(contar("entrada_conferida")).isZero();
        assertThat(jdbc.queryForObject("select xml_original from wms.nota_entrada", String.class))
                .isEqualTo(xml);
        assertThat(detalhe(id).toString()).doesNotContain("xmlOriginal", "infNFe");
        long outro = criarPedido();
        resposta(
                post(outro + "/notas/xml", Map.of("versao", versao(outro), "xml", xml), operador),
                409);
    }

    @Test
    void xmlPosteriorComplementaValorDesconhecidoSemReescreverRecebimento() throws Exception {
        long id = criarPedido();
        adicionarNota(id, 123, "10", produto.getId(), null);
        iniciar(id);
        chegada(id, itemId(id), "10", "0", DIA1);
        resposta(post(id + "/efetivacao", efetivar(versao(id), false), supervisor), 200);
        resposta(
                post(id + "/notas/xml", Map.of("versao", versao(id), "xml", xml()), operador), 200);
        assertThat(contar("entrada_conferida")).isEqualTo(1);
        assertThat(
                        detalhe(id)
                                .get("notas")
                                .get(0)
                                .get("itens")
                                .get(0)
                                .get("valorMercadoria")
                                .decimalValue())
                .isEqualByComparingTo("100.00");
        assertThat(detalhe(id).get("pedido").get("situacao").asString()).isEqualTo("EFETIVADO");
    }

    @Test
    void xmlDivergenteNaoSubstituiPrevisaoManualOuValorConhecido() throws Exception {
        long id = criarPedido();
        adicionarNota(id, 123, "9", produto.getId(), new BigDecimal("90"));
        resposta(
                post(id + "/notas/xml", Map.of("versao", versao(id), "xml", xml()), operador), 409);
        assertThat(
                        detalhe(id)
                                .get("notas")
                                .get(0)
                                .get("itens")
                                .get(0)
                                .get("prevista")
                                .decimalValue())
                .isEqualByComparingTo("9");
        assertThat(jdbc.queryForObject("select xml_original from wms.nota_entrada", String.class))
                .isNull();
    }

    @Test
    void xmlRecusaEmitenteSkuUnidadeOuPrecisaoIncompativeis() throws Exception {
        long id = criarPedido();
        for (String invalido :
                List.of(
                        xml().replace("12345678000199", "11111111000111"),
                        xml().replace("SKU-01", "NAO-CADASTRADO"),
                        xml().replace("<uCom>UN</uCom>", "<uCom>KG</uCom>"),
                        xml().replace("<qCom>10</qCom>", "<qCom>0.5</qCom>"))) {
            resposta(
                    post(
                            id + "/notas/xml",
                            Map.of("versao", versao(id), "xml", invalido),
                            operador),
                    400);
        }
        assertThat(contar("nota_entrada")).isZero();
    }

    @Test
    void entradasSaoPaginadasSemAtravessarEscopo() throws Exception {
        long id = prepararPedido("10");
        chegada(id, itemId(id), "10", "0", DIA1);
        resposta(post(id + "/efetivacao", efetivar(versao(id), false), supervisor), 200);
        assertThat(
                        resposta(get(id + "/entradas?tamanho=1", operador), 200)
                                .get("totalItens")
                                .longValue())
                .isEqualTo(1);
        resposta(get(id + "/entradas?tamanho=101", operador), 400);
        resposta(get(id + "/entradas", token("OPERACAO", List.of(), List.of())), 403);
    }

    @ParameterizedTest
    @ValueSource(strings = {"99999999999999999.99", "0.00", "NULL"})
    void d30ValorMercadoriaManualPreservaMaximoZeroENulo(String literal) throws Exception {
        long id = criarPedido();
        BigDecimal esperado = literal.equals("NULL") ? null : new BigDecimal(literal);
        adicionarNota(id, 1, "1", produto.getId(), esperado);
        var response = get(String.valueOf(id), operador);
        assertThat(response.statusCode()).isEqualTo(200);
        var dto = mapper.readValue(response.body(), PedidoEntradaDto.Detalhe.class);
        var item = dto.notas().getFirst().itens().getFirst();
        if (esperado == null) {
            assertThat(item.valorMercadoria()).isNull();
            assertThat(
                            jdbc.queryForObject(
                                    "select valor_mercadoria from wms.item_nota_entrada",
                                    BigDecimal.class))
                    .isNull();
        } else {
            assertThat(item.valorMercadoria()).isEqualByComparingTo(esperado);
            assertThat(
                            jdbc.queryForObject(
                                    "select valor_mercadoria from wms.item_nota_entrada",
                                    BigDecimal.class))
                    .isEqualByComparingTo(esperado);
        }
        assertThat(item.prevista()).isEqualByComparingTo("1");
        assertThat(item.produtoId()).isEqualTo(produto.getId());
        assertThat(dto.notas().getFirst().numero()).isEqualTo(1);
    }

    @ParameterizedTest
    @ValueSource(strings = {"-0.01", "100000000000000000.00", "0.001"})
    void d30ValorMercadoriaManualRejeitaForaDoDominioSemEfeitos(String literal) throws Exception {
        long id = criarPedido();
        var antes = D30FotografiaFisica.capturar(jdbc);
        resposta(
                post(
                        id + "/notas",
                        nota(versao(id), 1, "1", produto.getId(), new BigDecimal(literal)),
                        operador),
                400);
        assertThat(D30FotografiaFisica.capturar(jdbc)).isEqualTo(antes);
        assertThat(contar("nota_entrada")).isZero();
        assertThat(contar("item_nota_entrada")).isZero();
    }

    @Test
    void d30LimiteManualVinteNotasNaoSeConfundeComMilItens() throws Exception {
        long id = criarPedido();
        for (int n = 1; n <= 20; n++) adicionarNota(id, n, "1", produto.getId(), null);
        assertThat(detalhe(id).get("notas").size()).isEqualTo(20);
        assertThat(contar("item_nota_entrada")).isEqualTo(20);
        var antes = D30FotografiaFisica.capturar(jdbc);
        resposta(
                post(id + "/notas", nota(versao(id), 21, "1", produto.getId(), null), operador),
                400);
        assertThat(D30FotografiaFisica.capturar(jdbc)).isEqualTo(antes);
    }

    @Test
    void d30LimiteManualMilItensEPorNotaSaoIndependentes() throws Exception {
        long id = criarPedido();
        for (int n = 1; n <= 5; n++)
            resposta(post(id + "/notas", d30NotaComItens(id, n, 200), operador), 200);
        assertThat(contar("nota_entrada")).isEqualTo(5);
        assertThat(contar("item_nota_entrada")).isEqualTo(1000);
        var antes = D30FotografiaFisica.capturar(jdbc);
        resposta(post(id + "/notas", d30NotaComItens(id, 6, 1), operador), 400);
        assertThat(D30FotografiaFisica.capturar(jdbc)).isEqualTo(antes);
        long outro = criarPedido();
        antes = D30FotografiaFisica.capturar(jdbc);
        resposta(post(outro + "/notas", d30NotaComItens(outro, 7, 201), operador), 400);
        assertThat(D30FotografiaFisica.capturar(jdbc)).isEqualTo(antes);
    }

    @Test
    void d30ChegadaDuzentosItensAceitaEDuzentosEUmRecusaComOrigensValidas() throws Exception {
        long id = criarPedido();
        resposta(post(id + "/notas", d30NotaComItens(id, 1, 200), operador), 200);
        resposta(post(id + "/notas", d30NotaComItens(id, 2, 1), operador), 200);
        var itens =
                jdbc.queryForList("select id from wms.item_nota_entrada order by id", Long.class);
        assertThat(itens).hasSize(201);
        iniciar(id);
        var chegada = new HashMap<String, Object>();
        chegada.put("versao", versao(id));
        chegada.put("operacaoId", UUID.randomUUID());
        chegada.put("chegouEm", DIA1);
        chegada.put("observacao", "D30 conferencia de itens distintos");
        chegada.put("itens", itens.stream().map(i -> item(i, "1", "0")).toList());
        var antes = D30FotografiaFisica.capturar(jdbc);
        resposta(post(id + "/chegadas", chegada, operador), 400);
        assertThat(D30FotografiaFisica.capturar(jdbc)).isEqualTo(antes);
        chegada.put("itens", itens.subList(0, 200).stream().map(i -> item(i, "1", "0")).toList());
        resposta(post(id + "/chegadas", chegada, operador), 200);
        assertThat(contar("chegada_recebimento")).isEqualTo(1);
        assertThat(contar("item_chegada")).isEqualTo(200);
        assertThat(
                        jdbc.queryForObject(
                                "select sum(quantidade_boa) from wms.item_chegada",
                                BigDecimal.class))
                .isEqualByComparingTo("200");
        assertThat(
                        jdbc.queryForObject(
                                "select count(distinct item_nota_id) from wms.item_chegada",
                                Integer.class))
                .isEqualTo(200);
    }

    @Test
    void d30LimiteCemChegadasIncluiEstornadaSemConfundirQuantidade() throws Exception {
        long id = prepararPedido("101"), item = itemId(id);
        chegada(id, item, "1", "0", DIA1);
        long primeira = jdbc.queryForObject("select id from wms.chegada_recebimento", Long.class);
        resposta(
                post(id + "/chegadas/" + primeira + "/estorno", revisao(versao(id)), supervisor),
                200);
        for (int i = 1; i < 100; i++)
            chegada(id, item, "1", "0", Instant.parse(DIA1).plusSeconds(i).toString());
        assertThat(contar("chegada_recebimento")).isEqualTo(100);
        assertThat(
                        jdbc.queryForObject(
                                "select count(*) from wms.chegada_recebimento where estornada_em is not null",
                                Integer.class))
                .isEqualTo(1);
        assertThat(
                        jdbc.queryForObject(
                                "select sum(i.quantidade_boa) from wms.item_chegada i join wms.chegada_recebimento c on c.id=i.chegada_id where c.estornada_em is null",
                                BigDecimal.class))
                .isEqualByComparingTo("99");
        var antes = D30FotografiaFisica.capturar(jdbc);
        resposta(
                post(
                        id + "/chegadas",
                        dadosChegada(
                                versao(id),
                                item,
                                "1",
                                "0",
                                Instant.parse(DIA1).plusSeconds(100).toString()),
                        operador),
                400);
        assertThat(D30FotografiaFisica.capturar(jdbc)).isEqualTo(antes);
        assertThat(
                        detalhe(id)
                                .get("notas")
                                .get(0)
                                .get("itens")
                                .get(0)
                                .get("prevista")
                                .decimalValue())
                .isEqualByComparingTo("101");
    }

    private Map<String, Object> d30NotaComItens(long pedido, int numeroNota, int quantidade)
            throws Exception {
        var itens =
                java.util.stream.IntStream.rangeClosed(1, quantidade)
                        .mapToObj(
                                n ->
                                        Map.of(
                                                "numeroItem",
                                                n,
                                                "produtoId",
                                                produto.getId(),
                                                "quantidadePrevista",
                                                "1"))
                        .toList();
        return Map.of(
                "versao",
                versao(pedido),
                "serie",
                1,
                "numero",
                numeroNota,
                "emissao",
                "2026-09-01",
                "itens",
                itens);
    }

    @Test
    void d30ReferenciaDaEntradaEhUnicaNoContextoSemDependerDeReplay() throws Exception {
        var d =
                new HashMap<String, Object>(
                        Map.of(
                                "clienteId",
                                cliente.getId(),
                                "armazemId",
                                armazem.getId(),
                                "referencia",
                                "D30-REFERENCIA-CONTEXTO"));
        var original = resposta(post("", d, operador), 201);
        var antes = D30FotografiaFisica.capturar(jdbc);
        var duplicada = resposta(post("", new HashMap<>(d), operador), 409);
        assertThat(duplicada.get("codigo").asString()).isEqualTo("CONFLITO_DE_INTEGRIDADE");
        assertThat(D30FotografiaFisica.capturar(jdbc)).isEqualTo(antes);
        var outro =
                armazens.saveAndFlush(
                        new Armazem(
                                "D30-REF-OUTRO",
                                "Outro ficticio",
                                "33333333000133",
                                "Osasco",
                                "SP",
                                Instant.now()));
        String tokenOutro = token("OPERACAO", List.of(cliente.getId()), List.of(outro.getId()));
        d.put("armazemId", outro.getId());
        var novo = resposta(post("", d, tokenOutro), 201);
        assertThat(novo.get("id").longValue()).isNotEqualTo(original.get("id").longValue());
        assertThat(novo.get("referencia")).isEqualTo(original.get("referencia"));
        assertThat(novo.get("armazemId").longValue()).isEqualTo(outro.getId());
        assertThat(contar("pedido_entrada")).isEqualTo(2);
        assertThat(contar("nota_entrada")).isZero();
        assertThat(contar("entrada_conferida")).isZero();
    }

    @Test
    void d30NotaSemRecebidoAceitaNaoCriaMercadoriaNoPedidoFisicoPositivo() throws Exception {
        long id = criarPedido();
        adicionarNota(id, 1, "10", produto.getId(), null);
        adicionarNota(id, 2, "20", produto.getId(), null);
        iniciar(id);
        long recebido = itemId(id);
        long semRecebido =
                detalhe(id).get("notas").get(1).get("itens").get(0).get("id").longValue();
        chegada(id, recebido, "10", "0", DIA1);
        var antesRecusa = D30FotografiaFisica.capturar(jdbc);
        resposta(post(id + "/efetivacao", efetivar(versao(id), false), supervisor), 409);
        assertThat(D30FotografiaFisica.capturar(jdbc)).isEqualTo(antesRecusa);
        var r = resposta(post(id + "/efetivacao", efetivar(versao(id), true), supervisor), 200);
        assertThat(r.get("situacao").asString()).isEqualTo("EFETIVADO");
        var entradas = resposta(get(id + "/entradas", supervisor), 200).get("itens");
        assertThat(entradas.size()).isEqualTo(1);
        assertThat(entradas.get(0).get("itemNotaId").longValue()).isEqualTo(recebido);
        assertThat(entradas.get(0).get("quantidadeTriagem").decimalValue())
                .isEqualByComparingTo("10");
        assertThat(
                        jdbc.queryForObject(
                                "select count(*) from wms.entrada_conferida e join wms.item_chegada i on e.item_chegada_id=i.id where i.item_nota_id=?",
                                Integer.class,
                                semRecebido))
                .isZero();
        var falta = detalhe(id).get("notas").get(1).get("itens").get(0);
        assertThat(falta.get("prevista").decimalValue()).isEqualByComparingTo("20");
        assertThat(falta.get("recebidaBoa").decimalValue()).isEqualByComparingTo("0");
        assertThat(falta.get("recebidaAvariada").decimalValue()).isEqualByComparingTo("0");
        assertThat(falta.get("diferenca").decimalValue()).isEqualByComparingTo("-20");
        assertThat(contar("unidade_logistica")).isZero();
    }

    @ParameterizedTest
    @CsvSource({"9999999999999.999999,500", "0.000001,5"})
    void d30ChegadaMedidaExataPreservaQuantidadeObservacaoELote(String quantidade, int tamanho)
            throws Exception {
        produto =
                produtos.saveAndFlush(
                        new Produto(
                                cliente,
                                "D30-CHEGADA6",
                                "Produto ficticio medido",
                                "KG",
                                TipoQuantidade.MEDIDA,
                                6,
                                false,
                                false,
                                null,
                                Instant.now()));
        long id = prepararPedido(quantidade);
        String observacao = "o".repeat(tamanho), lote = "l".repeat(60);
        var dados =
                new HashMap<String, Object>(
                        dadosChegada(versao(id), itemId(id), quantidade, "0", DIA1));
        var recebido = new HashMap<String, Object>(item(itemId(id), quantidade, "0"));
        recebido.put("lote", lote);
        dados.put("observacao", observacao);
        dados.put("itens", List.of(recebido));
        resposta(post(id + "/chegadas", dados, operador), 200);
        var chegadaResposta = get(id + "/chegadas", supervisor);
        assertThat(chegadaResposta.statusCode()).isEqualTo(200);
        var observadorExato =
                tools.jackson.databind.json.JsonMapper.builder()
                        .enable(
                                tools.jackson.databind.DeserializationFeature
                                        .USE_BIG_DECIMAL_FOR_FLOATS)
                        .build();
        var chegada = observadorExato.readTree(chegadaResposta.body()).get("itens").get(0);
        assertThat(chegada.get("observacao").asString()).isEqualTo(observacao);
        assertThat(chegada.get("itens").get(0).get("lote").asString()).isEqualTo(lote);
        assertThat(chegada.get("itens").get(0).get("quantidadeBoa").decimalValue())
                .isEqualByComparingTo(quantidade);
        assertThat(chegada.get("itens").get(0).get("quantidadeAvariada").decimalValue())
                .isEqualByComparingTo("0");
        assertThat(
                        jdbc.queryForObject(
                                "select quantidade_boa from wms.item_chegada", BigDecimal.class))
                .isEqualByComparingTo(quantidade);
        assertThat(jdbc.queryForObject("select lote from wms.item_chegada", String.class))
                .isEqualTo(lote);
        var foto = D30FotografiaFisica.capturar(jdbc);
        resposta(post(id + "/chegadas", dados, operador), 200);
        assertThat(D30FotografiaFisica.capturar(jdbc)).isEqualTo(foto);
    }

    private long prepararPedido(String quantidade) throws Exception {
        long id = criarPedido();
        adicionarNota(id, 1, quantidade, produto.getId(), null);
        iniciar(id);
        return id;
    }

    private long criarPedido() throws Exception {
        return resposta(
                        post(
                                "",
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

    private void adicionarNota(
            long id, long numero, String quantidade, long produtoId, BigDecimal valor)
            throws Exception {
        resposta(
                post(
                        id + "/notas",
                        nota(versao(id), numero, quantidade, produtoId, valor),
                        operador),
                200);
    }

    private Map<String, Object> nota(
            long versao, long numero, String quantidade, long produtoId, BigDecimal valor) {
        var item = new HashMap<String, Object>();
        item.put("numeroItem", 1);
        item.put("produtoId", produtoId);
        item.put("quantidadePrevista", quantidade);
        item.put("valorMercadoria", valor);
        return Map.of(
                "versao",
                versao,
                "serie",
                1,
                "numero",
                numero,
                "emissao",
                "2026-09-01",
                "itens",
                List.of(item));
    }

    private void iniciar(long id) throws Exception {
        resposta(post(id + "/iniciar-conferencia", revisao(versao(id)), operador), 200);
    }

    private void chegada(long id, long item, String boa, String avariada, String data)
            throws Exception {
        resposta(
                post(
                        id + "/chegadas",
                        dadosChegada(versao(id), item, boa, avariada, data),
                        operador),
                200);
    }

    private Map<String, Object> dadosChegada(
            long versao, long item, String boa, String avariada, String data) {
        return Map.of(
                "versao",
                versao,
                "operacaoId",
                UUID.randomUUID(),
                "chegouEm",
                data,
                "observacao",
                "Conferencia fisica de teste",
                "itens",
                List.of(item(item, boa, avariada)));
    }

    private Map<String, Object> item(long id, String boa, String avariada) {
        return Map.of("itemNotaId", id, "quantidadeBoa", boa, "quantidadeAvariada", avariada);
    }

    private Map<String, Object> revisao(long versao) {
        return Map.of("versao", versao, "motivo", "Tratativa ficticia com o cliente");
    }

    private Map<String, Object> efetivar(long versao, boolean aceitar) {
        return Map.of(
                "versao",
                versao,
                "motivo",
                "Cliente concordou com a conferencia fisica",
                "aceitarDivergencias",
                aceitar);
    }

    private JsonNode detalhe(long id) throws Exception {
        return resposta(get(String.valueOf(id), operador), 200);
    }

    private long versao(long id) throws Exception {
        return detalhe(id).get("pedido").get("versao").longValue();
    }

    private long itemId(long id) throws Exception {
        return detalhe(id).get("notas").get(0).get("itens").get(0).get("id").longValue();
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
        String caminho =
                "/api/v1/pedidos-entrada"
                        + (rota.isEmpty() || rota.startsWith("?") ? "" : "/")
                        + rota;
        var builder =
                HttpRequest.newBuilder(
                                URI.create(
                                        "http://127.0.0.1:"
                                                + environment.getRequiredProperty(
                                                        "local.server.port")
                                                + caminho))
                        .timeout(Duration.ofSeconds(20))
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

    private JsonNode resposta(HttpResponse<String> response, int codigo) {
        assertThat(response.statusCode()).as(response.body()).isEqualTo(codigo);
        return mapper.readTree(response.body());
    }

    private List<Integer> paralelo(Callable<HttpResponse<String>> acao) throws Exception {
        var inicio = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            Callable<Integer> tarefa =
                    () -> {
                        inicio.await(5, TimeUnit.SECONDS);
                        return acao.call().statusCode();
                    };
            var a = executor.submit(tarefa);
            var b = executor.submit(tarefa);
            inicio.countDown();
            return List.of(a.get(20, TimeUnit.SECONDS), b.get(20, TimeUnit.SECONDS));
        }
    }

    private String token(String perfil, List<Long> clientes, List<Long> armazens) {
        Instant agora = Instant.now();
        var claims =
                JwtClaimsSet.builder()
                        .issuer("https://identidade.test.invalid")
                        .subject("recebedor-teste")
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

    private String xml() {
        return """
 <nfeProc xmlns="http://www.portalfiscal.inf.br/nfe" versao="4.00">
 <NFe><infNFe Id="NFe11111111111111111111111111111111111111111111" versao="4.00">
 <ide><mod>55</mod><serie>1</serie><nNF>123</nNF><dhEmi>2026-09-01T08:00:00-03:00</dhEmi></ide>
 <emit><CNPJ>12345678000199</CNPJ></emit>
 <det nItem="1"><prod><cProd>SKU-01</cProd><uCom>UN</uCom><qCom>10</qCom><vProd>100.00</vProd></prod></det>
 </infNFe></NFe></nfeProc>
 """;
    }
}
