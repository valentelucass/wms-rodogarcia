package br.com.rodogarcia.wms;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@ActiveProfiles("test")
@TestPropertySource(
        locations = "classpath:cadastros-test.properties",
        properties =
                "spring.datasource.url=jdbc:h2:mem:wms-estoque;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000;INIT=CREATE SCHEMA IF NOT EXISTS wms")
@Import(IdentidadeTesteConfig.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class EstoqueIntegrationTest {
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

    @BeforeEach
    void preparar() {
        for (String tabela :
                List.of(
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
        var agora = Instant.now();
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
        criarProduto();
        gestor = token("GESTOR", List.of(), List.of());
        operador = token("OPERACAO", List.of(cliente.getId()), List.of(armazem.getId()));
        supervisor = token("SUPERVISOR", List.of(cliente.getId()), List.of(armazem.getId()));
        numero = 0;
    }

    @Test
    void saldoSeparaPendenteUnitizadoEDisponivelSemDuplicarOrigem() throws Exception {
        long pedido = receber("10", "0");
        assertSaldo("fisicoTotal", "10");
        assertSaldo("pendenteUnitizacao", "10");
        assertSaldo("fisicoUnitizado", "0");
        var u = unitizar(pedido, "10", "BOA");
        assertSaldo("fisicoTotal", "10");
        assertSaldo("pendenteUnitizacao", "0");
        assertSaldo("fisicoUnitizado", "10");
        assertSaldo("naoEnderecado", "10");
        assertSaldo("disponivel", "0");
        assertSaldo("bloqueado", "10");
        assertThat(listar("&disponivel=false").get("itens").size()).isEqualTo(1);
        assertThat(listar("&disponivel=true").get("itens").size()).isZero();
        var estoque = posicionar(u, endereco("A", "ARMAZENAGEM"));
        assertThat(estoque.get("unidade").get("disponivelParaSaida").booleanValue()).isTrue();
        assertSaldo("disponivel", "10");
        assertSaldo("reservado", "0");
        assertSaldo("bloqueado", "0");
        assertSaldo("emArmazenagem", "10");
        assertThat(listar("&disponivel=true").get("itens").size()).isEqualTo(1);
    }

    @Test
    void remanejamentoPreservaIdentidadeOrigemEtiquetaFifoEInicioArmazenagem() throws Exception {
        var u = unidade();
        var a = endereco("A", "ARMAZENAGEM");
        var b = endereco("B", "ARMAZENAGEM");
        var etiqueta = resposta(get(rota(u, "/etiqueta"), operador), 200);
        var primeiro = posicionar(u, a);
        assertThat(primeiro.get("posicoesEquivalentes").intValue()).isEqualTo(1);
        assertThat(primeiro.get("inicioArmazenagemEm").isNull()).isFalse();
        assertThat(primeiro.get("unidade").get("dataFifo").asString()).isEqualTo(CHEGADA);
        var movido = posicionar(primeiro.get("unidade"), b);
        assertThat(movido.get("inicioArmazenagemEm"))
                .isEqualTo(primeiro.get("inicioArmazenagemEm"));
        assertThat(movido.get("primeiroEnderecamentoEm"))
                .isEqualTo(primeiro.get("primeiroEnderecamentoEm"));
        assertThat(movido.get("unidade").get("versao").longValue())
                .isGreaterThan(u.get("versao").longValue());
        assertThat(resposta(get(rota(u, "/etiqueta"), operador), 200)).isEqualTo(etiqueta);
        assertThat(ocupante(a)).isNull();
        assertThat(ocupante(b)).isEqualTo(u.get("id").longValue());
        assertSaldo("fisicoTotal", "10");
        var historico = resposta(get(rota(u, "/movimentos"), operador), 200).get("itens");
        assertThat(historico.size()).isEqualTo(2);
        assertThat(historico.get(0).get("acao").asString()).isEqualTo("ENDERECAMENTO");
        assertThat(historico.get(1).get("antes").get("posicoes").get(0).get("codigo").asString())
                .isEqualTo("A");
        assertThat(
                        historico
                                .get(1)
                                .get("depois")
                                .get("estoque")
                                .get("posicoes")
                                .get(0)
                                .get("codigo")
                                .asString())
                .isEqualTo("B");
    }

    @Test
    void repeticaoRecuperaConfirmacaoOriginalERecusaChaveComOutroConteudo() throws Exception {
        var u = unidade();
        var a = endereco("A", "ARMAZENAGEM");
        var dados = posicionamento(u, a);
        var confirmado = resposta(post(rota(u, "/movimentos"), dados, operador), 200);
        posicionar(confirmado.get("estoque").get("unidade"), endereco("B", "ARMAZENAGEM"));
        assertThat(resposta(post(rota(u, "/movimentos"), dados, operador), 200))
                .isEqualTo(confirmado);
        dados.put("motivo", "Conteudo alterado na repeticao");
        resposta(post(rota(u, "/movimentos"), dados, operador), 409);
        assertThat(contar("movimento_estoque")).isEqualTo(2);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void confirmaUmaVezSobDisputaDaMesmaUnidade(boolean mesmaChave) throws Exception {
        var u = unidade();
        var a = endereco("A", "ARMAZENAGEM");
        var dados = posicionamento(u, a);
        var outros = new HashMap<>(dados);
        if (!mesmaChave) outros.put("operacaoId", UUID.randomUUID());
        assertThat(
                        paralelo(
                                () -> post(rota(u, "/movimentos"), dados, operador),
                                () -> post(rota(u, "/movimentos"), outros, operador)))
                .containsExactlyInAnyOrder(
                        mesmaChave ? new Integer[] {200, 200} : new Integer[] {200, 409});
        assertThat(contar("movimento_estoque")).isEqualTo(1);
        assertThat(ocupadas()).isEqualTo(1);
        assertSaldo("fisicoTotal", "10");
    }

    @Test
    void clientesDiferentesDisputamMesmaPosicaoSemSobreporEstoque() throws Exception {
        var a = unidade();
        cliente =
                clientes.saveAndFlush(
                        new Cliente("OUTRO", "Outro ficticio", "11111111000111", Instant.now()));
        criarProduto();
        operador = token("OPERACAO", List.of(cliente.getId()), List.of(armazem.getId()));
        supervisor = token("SUPERVISOR", List.of(cliente.getId()), List.of(armazem.getId()));
        var b = unidade();
        var destino = endereco("DISPUTADO", "ARMAZENAGEM");
        var da = posicionamento(a, destino);
        var db = posicionamento(b, destino);
        assertThat(
                        paralelo(
                                () -> post(rota(a, "/movimentos"), da, gestor),
                                () -> post(rota(b, "/movimentos"), db, gestor)))
                .containsExactlyInAnyOrder(200, 409);
        assertThat(contar("movimento_estoque")).isEqualTo(1);
        assertThat(ocupadas()).isEqualTo(1);
        assertThat(
                        jdbc.queryForObject(
                                "select sum(quantidade) from wms.unidade_logistica",
                                BigDecimal.class))
                .isEqualByComparingTo("20");
    }

    @Test
    void triagemNaoDisponibilizaENaoIniciaArmazenagem() throws Exception {
        var u = unidade();
        var triagem = posicionar(u, endereco("T", "TRIAGEM"));
        assertThat(triagem.get("inicioArmazenagemEm").isNull()).isTrue();
        assertThat(triagem.get("posicoesEquivalentes").intValue()).isZero();
        assertThat(triagem.get("unidade").get("disponivelParaSaida").booleanValue()).isFalse();
        assertSaldo("emTriagem", "10");
        var armazenada = posicionar(triagem.get("unidade"), endereco("A", "ARMAZENAGEM"));
        assertThat(armazenada.get("inicioArmazenagemEm").isNull()).isFalse();
        assertThat(armazenada.get("primeiroEnderecamentoEm"))
                .isEqualTo(triagem.get("primeiroEnderecamentoEm"));
        assertSaldo("disponivel", "10");
    }

    @Test
    void quarentenaMantemBloqueioAteLiberacaoExpressaDoSupervisor() throws Exception {
        var u = unidade();
        var q = posicionar(u, endereco("Q", "QUARENTENA"));
        assertThat(q.get("bloqueada").booleanValue()).isTrue();
        resposta(post(rota(u, "/liberacao"), bloqueio(q.get("unidade")), supervisor), 409);
        assertSaldo("emQuarentena", "10");
        var a = posicionar(q.get("unidade"), endereco("A", "ARMAZENAGEM"));
        assertThat(a.get("bloqueada").booleanValue()).isTrue();
        assertSaldo("disponivel", "0");
        var dados = bloqueio(a.get("unidade"));
        resposta(post(rota(u, "/liberacao"), dados, operador), 403);
        var liberada = resposta(post(rota(u, "/liberacao"), dados, supervisor), 200);
        assertThat(liberada.get("estoque").get("bloqueada").booleanValue()).isFalse();
        assertThat(resposta(post(rota(u, "/liberacao"), dados, supervisor), 200))
                .isEqualTo(liberada);
        assertSaldo("disponivel", "10");
    }

    @Test
    void avariadaSoVaiParaQuarentenaENaoPodeSerLiberada() throws Exception {
        var u = unitizar(receber("0", "10"), "10", "AVARIADA");
        var a = endereco("A", "ARMAZENAGEM");
        resposta(post(rota(u, "/movimentos"), posicionamento(u, a), operador), 409);
        var q = posicionar(u, endereco("Q", "QUARENTENA"));
        resposta(post(rota(u, "/liberacao"), bloqueio(q.get("unidade")), gestor), 409);
        resposta(post(rota(u, "/movimentos"), posicionamento(q.get("unidade"), a), gestor), 409);
        assertSaldo("avariado", "10");
        assertSaldo("disponivel", "0");
        assertThat(ocupadas()).isEqualTo(1);
    }

    @Test
    void bloqueioOperacionalPreservaQuantidadeEImpedeDivisao() throws Exception {
        var u = unidade();
        var dados = bloqueio(u);
        var bloqueada = resposta(post(rota(u, "/bloqueio"), dados, operador), 200);
        assertThat(resposta(post(rota(u, "/bloqueio"), dados, operador), 200)).isEqualTo(bloqueada);
        resposta(
                post(
                        rota(u, "/liberacao"),
                        bloqueio(bloqueada.get("estoque").get("unidade")),
                        supervisor),
                409);
        recusarDivisao(bloqueada.get("estoque").get("unidade"));
        assertSaldo("fisicoTotal", "10");
        assertThat(contar("unidade_logistica")).isEqualTo(1);
    }

    @Test
    void unidadeEnderecadaNaoPodeSerDivididaPeloFluxoAnterior() throws Exception {
        var posicionada = posicionar(unidade(), endereco("A", "ARMAZENAGEM"));
        recusarDivisao(posicionada.get("unidade"));
        assertSaldo("disponivel", "10");
    }

    @Test
    void duasPosicoesExigemConjuntoEConservamOcupacaoCobravelAoRemanejar() throws Exception {
        var u = unidade();
        var a = endereco("A", "ARMAZENAGEM");
        var b = endereco("B", "ARMAZENAGEM");
        var c = endereco("C", "ARMAZENAGEM");
        var ab = conjunto(a, b, "AB");
        var bc = conjunto(b, c, "BC");
        var dados = posicionamento(u, a, b);
        resposta(post(rota(u, "/movimentos"), dados, operador), 400);
        dados.put("conjuntoId", ab.get("id").longValue());
        var primeiro = resposta(post(rota(u, "/movimentos"), dados, operador), 200).get("estoque");
        assertThat(ocupadas()).isEqualTo(2);
        assertSaldo("disponivel", "10");
        var remanejar = posicionamento(primeiro.get("unidade"), b, c);
        remanejar.put("conjuntoId", bc.get("id").longValue());
        var depois =
                resposta(post(rota(u, "/movimentos"), remanejar, operador), 200).get("estoque");
        assertThat(ocupadas()).isEqualTo(2);
        assertThat(ocupante(a)).isNull();
        assertThat(ocupante(b)).isEqualTo(u.get("id").longValue());
        assertThat(ocupante(c)).isEqualTo(u.get("id").longValue());
        assertThat(depois.get("posicoesEquivalentes").intValue()).isEqualTo(2);
        assertThat(depois.get("inicioArmazenagemEm"))
                .isEqualTo(primeiro.get("inicioArmazenagemEm"));
        resposta(
                post(
                        "/api/v1/conjuntos-posicoes/" + bc.get("id").longValue() + "/encerramento",
                        Map.of("versao", 0, "motivo", "Encerramento solicitado"),
                        gestor),
                409);
        resposta(
                post(
                        "/api/v1/conjuntos-posicoes/" + ab.get("id").longValue() + "/encerramento",
                        Map.of("versao", 0, "motivo", "Encerramento solicitado"),
                        gestor),
                200);
    }

    @Test
    void posicaoOcupadaImpedeTodoConjuntoSemReservaParcial() throws Exception {
        var a = endereco("A", "ARMAZENAGEM");
        var b = endereco("B", "ARMAZENAGEM");
        var par = conjunto(a, b, "AB");
        var primeira = unidade();
        posicionar(primeira, b);
        var segunda = unidade();
        var dados = posicionamento(segunda, a, b);
        dados.put("conjuntoId", par.get("id").longValue());
        resposta(post(rota(segunda, "/movimentos"), dados, operador), 409);
        assertThat(ocupante(a)).isNull();
        assertThat(ocupante(b)).isEqualTo(primeira.get("id").longValue());
        assertThat(estoque(segunda).get("primeiroEnderecamentoEm").isNull()).isTrue();
        assertThat(contar("movimento_estoque")).isEqualTo(1);
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "pesoKg",
                "alturaMetros",
                "larguraMetros",
                "profundidadeMetros",
                "empilhamento"
            })
    void rejeitaCadaLimiteFisicoSemOcupar(String campo) throws Exception {
        var u = unidade();
        var dados = posicionamento(u, endereco("A", "ARMAZENAGEM"));
        var medidas = medidas(1);
        medidas.put(campo, campo.equals("pesoKg") ? 1001 : 3);
        dados.put("medidas", medidas);
        resposta(post(rota(u, "/movimentos"), dados, operador), 409);
        assertThat(ocupadas()).isZero();
        assertThat(contar("movimento_estoque")).isZero();
    }

    @Test
    void confereCodigoLidoTipoAreaConfiguracaoERevisaoAtual() throws Exception {
        var u = unidade();
        var semPerfil = criarEndereco("SEM", "ARMAZENAGEM");
        resposta(post(rota(u, "/movimentos"), posicionamento(u, semPerfil), operador), 409);
        var a = endereco("A", "ARMAZENAGEM");
        var errado = posicionamento(u, a);
        errado.put(
                "destinos",
                List.of(Map.of("enderecoId", a.get("id").longValue(), "codigoLido", "OUTRO")));
        resposta(post(rota(u, "/movimentos"), errado, operador), 400);
        var bobina = configurar(a, "BOBINA", gestor, 200);
        resposta(post(rota(u, "/movimentos"), posicionamento(u, bobina), operador), 409);
        resposta(
                post(
                        rota(u, "/movimentos"),
                        posicionamento(u, endereco("S", "SEPARACAO")),
                        operador),
                409);
        var semMedidas = posicionamento(u, semPerfil);
        semMedidas.remove("medidas");
        resposta(post(rota(u, "/movimentos"), semMedidas, operador), 400);
        var atual = posicionar(u, endereco("VALIDO", "ARMAZENAGEM"));
        resposta(
                post(
                        rota(u, "/movimentos"),
                        posicionamento(u, endereco("NOVO", "ARMAZENAGEM")),
                        operador),
                409);
        var alteraMedidas = posicionamento(atual.get("unidade"), semPerfil);
        alteraMedidas.put("medidas", medidas(1));
        resposta(post(rota(u, "/movimentos"), alteraMedidas, operador), 400);
        assertThat(ocupadas()).isEqualTo(1);
    }

    @Test
    void rejeitaPosicoesRepetidasEConjuntoQueNaoCorrespondeAosCodigos() throws Exception {
        var u = unidade();
        var a = endereco("A", "ARMAZENAGEM");
        var b = endereco("B", "ARMAZENAGEM");
        var c = endereco("C", "ARMAZENAGEM");
        var par = conjunto(a, b, "AB");
        var repetidas = posicionamento(u, a, a);
        repetidas.put("conjuntoId", par.get("id").longValue());
        resposta(post(rota(u, "/movimentos"), repetidas, operador), 400);
        var errado = posicionamento(u, a, c);
        errado.put("conjuntoId", par.get("id").longValue());
        resposta(post(rota(u, "/movimentos"), errado, operador), 400);
        assertThat(ocupadas()).isZero();
    }

    @Test
    void capacidadeSoMudaPorGestorEmEnderecoLivreSemConjuntoAtivo() throws Exception {
        var a = endereco("A", "ARMAZENAGEM");
        configurar(a, "PALLET", supervisor, 403);
        configurar(a, "PALLET", operador, 403);
        var b = endereco("B", "ARMAZENAGEM");
        var par = conjunto(a, b, "AB");
        configurar(a, "PALLET", gestor, 409);
        resposta(
                post(
                        "/api/v1/conjuntos-posicoes/" + par.get("id").longValue() + "/encerramento",
                        Map.of("versao", 0, "motivo", "Encerramento solicitado"),
                        gestor),
                200);
        a = configurar(a, "PALLET", gestor, 200);
        posicionar(unidade(), a);
        configurar(a, "PALLET", gestor, 409);
    }

    @Test
    void cadastroDeConjuntoRecusaIncompatibilidadeELimitesInventados() throws Exception {
        var a = endereco("A", "ARMAZENAGEM");
        var b = endereco("B", "TRIAGEM");
        resposta(post("/api/v1/conjuntos-posicoes", dadosConjunto(a, b, "AB"), gestor), 400);
        resposta(post("/api/v1/conjuntos-posicoes", dadosConjunto(a, a, "AA"), gestor), 400);
        var c = endereco("C", "ARMAZENAGEM");
        var dados = dadosConjunto(a, c, "AC");
        dados.put(
                "limites",
                Map.of(
                        "pesoKg",
                        2001,
                        "alturaMetros",
                        2,
                        "larguraMetros",
                        4,
                        "profundidadeMetros",
                        2,
                        "empilhamentoMaximo",
                        2));
        resposta(post("/api/v1/conjuntos-posicoes", dados, gestor), 400);
        resposta(post("/api/v1/conjuntos-posicoes", dadosConjunto(a, c, "AC"), operador), 403);
        assertThat(contar("conjunto_posicoes")).isZero();
    }

    @Test
    void encerramentoDeEnderecoRetiraDisponibilidadeMasPreservaPosicao() throws Exception {
        var a = endereco("A", "ARMAZENAGEM");
        var u = unidade();
        var atual = posicionar(u, a);
        resposta(
                post(
                        "/api/v1/enderecos/" + a.get("id").longValue() + "/encerramento",
                        Map.of(
                                "versao",
                                a.get("versao").longValue(),
                                "motivo",
                                "Endereco em manutencao"),
                        gestor),
                200);
        assertSaldo("disponivel", "0");
        assertThat(ocupante(a)).isEqualTo(u.get("id").longValue());
        assertThat(estoque(u).get("unidade").get("disponivelParaSaida").booleanValue()).isFalse();
        posicionar(atual.get("unidade"), endereco("B", "ARMAZENAGEM"));
        assertSaldo("disponivel", "10");
    }

    @Test
    void destinoDeOutroArmazemNaoRecebeUnidade() throws Exception {
        var u = unidade();
        var original = armazem;
        armazem =
                armazens.saveAndFlush(
                        new Armazem(
                                "OUTRO",
                                "Outro armazem",
                                "22222222000122",
                                "Osasco",
                                "SP",
                                Instant.now()));
        var a = endereco("OUTRO", "ARMAZENAGEM");
        armazem = original;
        resposta(post(rota(u, "/movimentos"), posicionamento(u, a), gestor), 400);
        assertThat(ocupadas()).isZero();
    }

    @Test
    void escopoProtegeSaldoDetalheHistoricoComandosERepeticao() throws Exception {
        var u = unidade();
        var dados = posicionamento(u, endereco("A", "ARMAZENAGEM"));
        resposta(post(rota(u, "/movimentos"), dados, operador), 200);
        for (String estranho :
                List.of(
                        token("OPERACAO", List.of(99999L), List.of(armazem.getId())),
                        token("SUPERVISOR", List.of(cliente.getId()), List.of(99999L)))) {
            resposta(get(rota(u, "/estoque"), estranho), 403);
            resposta(get(rota(u, "/movimentos"), estranho), 403);
            resposta(get(saldoRota(), estranho), 403);
            resposta(get(listaRota(), estranho), 403);
            resposta(post(rota(u, "/movimentos"), dados, estranho), 403);
        }
        resposta(get(listaRota(), null), 401);
        resposta(get(listaRota() + "&tamanho=101", operador), 400);
        resposta(get("/api/v1/estoque/saldo?clienteId=-1&armazemId=1&produtoId=1", gestor), 400);
        assertThat(contar("movimento_estoque")).isEqualTo(1);
    }

    @Test
    void falhaDeAuditoriaRevertePosicoesUnidadePedidoEPermiteTentarMesmaChave() throws Exception {
        var a = endereco("A", "ARMAZENAGEM");
        var b = endereco("B", "ARMAZENAGEM");
        var u = posicionar(unidade(), a).get("unidade");
        var antes = estoque(u);
        long pedido = u.get("pedidoId").longValue();
        long revisao = versao(pedido);
        int auditorias = contar("auditoria_cadastro");
        var dados = posicionamento(u, b);
        jdbc.execute(
                "alter table wms.auditoria_cadastro add constraint falha_movimento_teste check (acao <> 'MOVIMENTACAO')");
        try {
            resposta(post(rota(u, "/movimentos"), dados, operador), 409);
            assertThat(estoque(u)).isEqualTo(antes);
            assertThat(versao(pedido)).isEqualTo(revisao);
            assertThat(ocupante(a)).isEqualTo(u.get("id").longValue());
            assertThat(ocupante(b)).isNull();
            assertThat(contar("auditoria_cadastro")).isEqualTo(auditorias);
            assertThat(contar("movimento_estoque")).isEqualTo(1);
        } finally {
            jdbc.execute(
                    "alter table wms.auditoria_cadastro drop constraint falha_movimento_teste");
        }
        resposta(post(rota(u, "/movimentos"), dados, operador), 200);
        assertThat(ocupante(a)).isNull();
        assertThat(ocupante(b)).isEqualTo(u.get("id").longValue());
    }

    @Test
    void falhaDoHistoricoTambemRevertePrimeiroEnderecamentoEAuditoria() throws Exception {
        var u = unidade();
        var a = endereco("A", "ARMAZENAGEM");
        var dados = posicionamento(u, a);
        int auditorias = contar("auditoria_cadastro");
        jdbc.execute(
                "alter table wms.movimento_estoque add constraint falha_historico_teste check (acao <> 'ENDERECAMENTO')");
        try {
            resposta(post(rota(u, "/movimentos"), dados, operador), 409);
            assertThat(estoque(u).get("medidas").isNull()).isTrue();
            assertThat(estoque(u).get("inicioArmazenagemEm").isNull()).isTrue();
            assertThat(ocupadas()).isZero();
            assertThat(contar("auditoria_cadastro")).isEqualTo(auditorias);
        } finally {
            jdbc.execute("alter table wms.movimento_estoque drop constraint falha_historico_teste");
        }
        resposta(post(rota(u, "/movimentos"), dados, operador), 200);
        assertThat(contar("movimento_estoque")).isEqualTo(1);
    }

    @Test
    void validacaoDoServicoNaoDependeDoController() {
        assertThatThrownBy(() -> movimentos.posicionar(UUID.randomUUID(), null))
                .isInstanceOf(ConstraintViolationException.class);
    }

    @Test
    void liberacaoRevalidaEnderecoSemRemoverBloqueioEmLocalEncerrado() throws Exception {
        var a = endereco("A", "ARMAZENAGEM");
        var u = posicionar(unidade(), a).get("unidade");
        var bloqueada =
                resposta(post(rota(u, "/bloqueio"), bloqueio(u), operador), 200).get("estoque");
        resposta(
                post(
                        "/api/v1/enderecos/" + a.get("id").longValue() + "/encerramento",
                        Map.of(
                                "versao",
                                a.get("versao").longValue(),
                                "motivo",
                                "Endereco em manutencao"),
                        gestor),
                200);
        resposta(post(rota(u, "/liberacao"), bloqueio(bloqueada.get("unidade")), supervisor), 409);
        assertThat(estoque(u).get("bloqueada").booleanValue()).isTrue();
        assertThat(estoque(u).get("unidade").get("versao"))
                .isEqualTo(bloqueada.get("unidade").get("versao"));
        assertThat(contar("movimento_estoque")).isEqualTo(2);
        assertSaldo("disponivel", "0");
    }

    @Test
    void produtoEncerradoImpedeNovoEnderecoMasPermiteBloqueioPreventivo() throws Exception {
        var u = unidade();
        var a = endereco("A", "ARMAZENAGEM");
        resposta(
                post(
                        "/api/v1/produtos/" + produto.getId() + "/encerramento",
                        Map.of("versao", 0, "motivo", "Produto em encerramento"),
                        gestor),
                200);
        resposta(post(rota(u, "/movimentos"), posicionamento(u, a), operador), 409);
        resposta(post(rota(u, "/bloqueio"), bloqueio(u), operador), 200);
        assertThat(ocupadas()).isZero();
        assertSaldo("fisicoTotal", "10");
        assertSaldo("disponivel", "0");
    }

    @Test
    void saldoDuranteUnitizacaoNaoPerdeNemDuplicaQuantidade() throws Exception {
        long pedido = receber("10", "0");
        var inicio = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var leitura =
                    executor.submit(
                            () -> {
                                inicio.await();
                                for (int i = 0; i < 12; i++) assertSaldo("fisicoTotal", "10");
                                return true;
                            });
            var gravacao =
                    executor.submit(
                            () -> {
                                inicio.await();
                                return unitizar(pedido, "10", "BOA");
                            });
            inicio.countDown();
            assertThat(leitura.get(30, TimeUnit.SECONDS)).isTrue();
            assertThat(gravacao.get(30, TimeUnit.SECONDS).get("quantidade").decimalValue())
                    .isEqualByComparingTo("10");
        }
        assertSaldo("fisicoTotal", "10");
        assertSaldo("pendenteUnitizacao", "0");
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
                                Instant.now()));
        embalagem =
                embalagens.saveAndFlush(
                        new Embalagem(
                                produto,
                                "DUN-10",
                                "Embalagem ficticia",
                                new BigDecimal("10"),
                                Instant.now()));
    }

    private long receber(String boa, String avariada) throws Exception {
        long pedido =
                resposta(
                                post(
                                        "/api/v1/pedidos-entrada",
                                        Map.of(
                                                "clienteId",
                                                cliente.getId(),
                                                "armazemId",
                                                armazem.getId(),
                                                "referencia",
                                                "PED-" + ++numero),
                                        operador),
                                201)
                        .get("id")
                        .longValue();
        resposta(
                post(
                        PEDIDOS + pedido + "/notas",
                        Map.of(
                                "versao",
                                versao(pedido),
                                "serie",
                                1,
                                "numero",
                                numero,
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
                                                new BigDecimal(boa)
                                                        .add(new BigDecimal(avariada))))),
                        operador),
                200);
        long item =
                resposta(get(PEDIDOS + pedido, operador), 200)
                        .get("notas")
                        .get(0)
                        .get("itens")
                        .get(0)
                        .get("id")
                        .longValue();
        resposta(
                post(
                        PEDIDOS + pedido + "/iniciar-conferencia",
                        Map.of("versao", versao(pedido), "motivo", "Conferencia iniciada"),
                        operador),
                200);
        resposta(
                post(
                        PEDIDOS + pedido + "/chegadas",
                        Map.of(
                                "versao",
                                versao(pedido),
                                "operacaoId",
                                UUID.randomUUID(),
                                "chegouEm",
                                CHEGADA,
                                "observacao",
                                "Conferencia fisica",
                                "itens",
                                List.of(
                                        Map.of(
                                                "itemNotaId",
                                                item,
                                                "quantidadeBoa",
                                                boa,
                                                "quantidadeAvariada",
                                                avariada))),
                        operador),
                200);
        resposta(
                post(
                        PEDIDOS + pedido + "/efetivacao",
                        Map.of(
                                "versao",
                                versao(pedido),
                                "motivo",
                                "Conferencia aprovada",
                                "aceitarDivergencias",
                                true),
                        supervisor),
                200);
        return pedido;
    }

    private long versao(long pedido) throws Exception {
        return resposta(get(PEDIDOS + pedido, operador), 200)
                .get("pedido")
                .get("versao")
                .longValue();
    }

    private JsonNode unidade() throws Exception {
        return unitizar(receber("10", "0"), "10", "BOA");
    }

    private JsonNode unitizar(long pedido, String quantidade, String condicao) throws Exception {
        long entrada =
                resposta(get(PEDIDOS + pedido + "/entradas", operador), 200)
                        .get("itens")
                        .get(0)
                        .get("id")
                        .longValue();
        return resposta(
                        post(
                                PEDIDOS + pedido + "/entradas/" + entrada + "/unitizacao",
                                Map.of(
                                        "operacaoId",
                                        UUID.randomUUID(),
                                        "versaoPedido",
                                        versao(pedido),
                                        "motivo",
                                        "Unitizacao conferida",
                                        "unidades",
                                        List.of(
                                                Map.of(
                                                        "embalagemId",
                                                        embalagem.getId(),
                                                        "tipo",
                                                        "PALLET",
                                                        "condicao",
                                                        condicao,
                                                        "quantidade",
                                                        quantidade))),
                                operador),
                        200)
                .get("unidades")
                .get(0)
                .get("unidade");
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

    private Map<String, Object> dadosConjunto(JsonNode a, JsonNode b, String codigo) {
        return new HashMap<>(
                Map.of(
                        "armazemId",
                        armazem.getId(),
                        "codigo",
                        codigo,
                        "enderecoAId",
                        a.get("id").longValue(),
                        "enderecoBId",
                        b.get("id").longValue(),
                        "limites",
                        Map.of(
                                "pesoKg",
                                1500,
                                "alturaMetros",
                                2,
                                "larguraMetros",
                                4,
                                "profundidadeMetros",
                                2,
                                "empilhamentoMaximo",
                                2),
                        "motivo",
                        "Compatibilidade fisica conferida"));
    }

    private JsonNode conjunto(JsonNode a, JsonNode b, String codigo) throws Exception {
        return resposta(
                post("/api/v1/conjuntos-posicoes", dadosConjunto(a, b, codigo), gestor), 201);
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

    private Map<String, Object> bloqueio(JsonNode u) {
        return Map.of(
                "operacaoId",
                UUID.randomUUID(),
                "versaoUnidade",
                u.get("versao").longValue(),
                "motivo",
                "Verificacao operacional");
    }

    private void recusarDivisao(JsonNode u) throws Exception {
        resposta(
                post(
                        PEDIDOS
                                + u.get("pedidoId").longValue()
                                + "/unidades/"
                                + u.get("id").longValue()
                                + "/divisao",
                        Map.of(
                                "operacaoId",
                                UUID.randomUUID(),
                                "versao",
                                u.get("versao").longValue(),
                                "quantidadeNovaUnidade",
                                2,
                                "motivo",
                                "Divisao fisica solicitada"),
                        supervisor),
                409);
    }

    private String rota(JsonNode u, String sufixo) {
        return UNIDADES + u.get("codigo").asString() + sufixo;
    }

    private JsonNode estoque(JsonNode u) throws Exception {
        return resposta(get(rota(u, "/estoque"), operador), 200);
    }

    private String listaRota() {
        return "/api/v1/estoque?clienteId=" + cliente.getId() + "&armazemId=" + armazem.getId();
    }

    private String saldoRota() {
        return "/api/v1/estoque/saldo?clienteId="
                + cliente.getId()
                + "&armazemId="
                + armazem.getId()
                + "&produtoId="
                + produto.getId();
    }

    private JsonNode listar(String filtro) throws Exception {
        return resposta(get(listaRota() + filtro, operador), 200);
    }

    private void assertSaldo(String campo, String valor) throws Exception {
        assertThat(resposta(get(saldoRota(), operador), 200).get(campo).decimalValue())
                .as(campo)
                .isEqualByComparingTo(valor);
    }

    private int contar(String tabela) {
        return jdbc.queryForObject("select count(*) from wms." + tabela, Integer.class);
    }

    private int ocupadas() {
        return jdbc.queryForObject(
                "select count(*) from wms.ocupacao_endereco where unidade_id is not null",
                Integer.class);
    }

    private Long ocupante(JsonNode e) {
        var encontrados =
                jdbc.query(
                        "select unidade_id from wms.ocupacao_endereco where endereco_id=?",
                        (rs, n) -> rs.getObject(1, Long.class),
                        e.get("id").longValue());
        return encontrados.isEmpty() ? null : encontrados.getFirst();
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
}
