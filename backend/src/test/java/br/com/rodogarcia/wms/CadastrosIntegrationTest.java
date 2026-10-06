package br.com.rodogarcia.wms;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.rodogarcia.wms.config.IdentidadeTesteConfig;
import br.com.rodogarcia.wms.dto.ClienteDto;
import br.com.rodogarcia.wms.dto.ProdutoDto;
import br.com.rodogarcia.wms.models.TipoQuantidade;
import br.com.rodogarcia.wms.services.ArmazemService;
import br.com.rodogarcia.wms.services.ClienteService;
import br.com.rodogarcia.wms.services.EmbalagemService;
import br.com.rodogarcia.wms.services.EnderecoService;
import br.com.rodogarcia.wms.services.ProdutoService;
import jakarta.validation.ConstraintViolationException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
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
@TestPropertySource("classpath:cadastros-test.properties")
@Import(IdentidadeTesteConfig.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ExtendWith(OutputCaptureExtension.class)
class CadastrosIntegrationTest {
    private static final AtomicLong SEQUENCIA = new AtomicLong(100);
    private final HttpClient client =
            HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    @Autowired private Environment environment;
    @Autowired private JsonMapper mapper;
    @Autowired private JwtEncoder encoder;
    @Autowired private JdbcTemplate jdbc;
    private String gestor;

    @Autowired private ClienteService clientes;
    @Autowired private ArmazemService armazens;
    @Autowired private ProdutoService produtos;
    @Autowired private EmbalagemService embalagens;
    @Autowired private EnderecoService enderecos;
    @Autowired private JwtDecoder decoder;

    @Test
    void servicosRecusamContratosNulosMesmoForaDoHttp() {
        comoUsuario(
                gestor,
                () -> {
                    for (Runnable criar :
                            List.<Runnable>of(
                                    () -> clientes.criar(null),
                                    () -> armazens.criar(null),
                                    () -> produtos.criar(null),
                                    () -> embalagens.criar(null),
                                    () -> enderecos.criar(null))) {
                        assertThatThrownBy(criar::run)
                                .isInstanceOf(ConstraintViolationException.class);
                    }
                });
        assertThat(
                        jdbc.queryForObject(
                                "select count(*) from wms.auditoria_cadastro", Integer.class))
                .isZero();
    }

    @Test
    void servicoRecusaCamposInvalidosAntesDeExecutarRegraOuGravar() throws Exception {
        long clienteId = criarCliente();
        comoUsuario(
                gestor,
                () ->
                        assertThatThrownBy(
                                        () ->
                                                produtos.criar(
                                                        new ProdutoDto.Criar(
                                                                clienteId,
                                                                "SKU-TESTE",
                                                                "Produto ficticio",
                                                                "UN",
                                                                TipoQuantidade.CONTAGEM,
                                                                null,
                                                                true,
                                                                false,
                                                                null)))
                                .isInstanceOf(ConstraintViolationException.class));
        assertThat(jdbc.queryForObject("select count(*) from wms.produto", Integer.class)).isZero();
        assertThat(
                        jdbc.queryForObject(
                                "select count(*) from wms.auditoria_cadastro", Integer.class))
                .isEqualTo(1);
    }

    @Test
    void servicoRecusaRevisaoInvalidaSemAlterarCadastro() throws Exception {
        long id = criarCliente();
        String nome =
                jdbc.queryForObject("select nome from wms.cliente where id = ?", String.class, id);
        comoUsuario(
                gestor,
                () ->
                        assertThatThrownBy(
                                        () ->
                                                clientes.alterar(
                                                        id,
                                                        new ClienteDto.Alterar(
                                                                null, "Nome indevido", "")))
                                .isInstanceOf(ConstraintViolationException.class));
        assertThat(
                        jdbc.queryForObject(
                                "select nome from wms.cliente where id = ?", String.class, id))
                .isEqualTo(nome);
        assertThat(
                        jdbc.queryForObject(
                                "select count(*) from wms.auditoria_cadastro", Integer.class))
                .isEqualTo(1);
    }

    @Test
    void servicoExigePermissaoMesmoForaDoHttp() {
        comoUsuario(
                token("OPERACAO", List.of(), List.of()),
                () ->
                        assertThatThrownBy(
                                        () ->
                                                clientes.criar(
                                                        new ClienteDto.Criar(
                                                                "CLI-DIRETO",
                                                                "Cliente ficticio",
                                                                "12345678000199")))
                                .isInstanceOf(AccessDeniedException.class));
        assertThat(jdbc.queryForObject("select count(*) from wms.cliente", Integer.class)).isZero();
        assertThat(
                        jdbc.queryForObject(
                                "select count(*) from wms.auditoria_cadastro", Integer.class))
                .isZero();
    }

    private void comoUsuario(String token, Runnable acao) {
        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new JwtAuthenticationToken(decoder.decode(token), List.of()));
        SecurityContextHolder.setContext(context);
        try {
            acao.run();
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @BeforeEach
    void preparar() {
        for (String tabela :
                List.of(
                        "auditoria_cadastro",
                        "embalagem",
                        "endereco",
                        "produto",
                        "cliente",
                        "armazem")) {
            jdbc.update("delete from wms." + tabela);
        }
        gestor = token("GESTOR", List.of(), List.of());
    }

    @Test
    void percorreOsCincoCadastrosComDadosPersistidosEOrigemNaAuditoria() throws Exception {
        long cliente = criarCliente();
        long armazem = criarArmazem();
        long produto = criarProduto(cliente, "SKU-01", "CONTAGEM", 0);
        long embalagem =
                criar("embalagens", embalagem(produto, "DUN-01", "500")).get("id").longValue();
        long endereco =
                criar("enderecos", endereco(armazem, "A101", "A", "01")).get("id").longValue();
        String operador = token("OPERACAO", List.of(cliente), List.of(armazem));

        assertThat(enviar("GET", "/produtos/" + produto, null, operador).statusCode())
                .isEqualTo(200);
        assertThat(
                        json(enviar("GET", "/embalagens/" + embalagem, null, operador))
                                .get("quantidadeProduto")
                                .decimalValue())
                .isEqualByComparingTo("500");
        assertThat(
                        json(enviar("GET", "/enderecos/" + endereco, null, operador))
                                .get("armazemId")
                                .longValue())
                .isEqualTo(armazem);
        assertThat(
                        jdbc.queryForObject(
                                "select count(*) from wms.auditoria_cadastro", Integer.class))
                .isEqualTo(5);
        var historico =
                enviar("GET", "/auditoria?tipo=EMBALAGEM&registroId=" + embalagem, null, gestor);
        assertThat(historico.statusCode()).isEqualTo(200);
        var evento = json(historico).get("itens").get(0);
        assertThat(evento.get("usuario").asString()).isEqualTo("usuario-teste");
        assertThat(evento.get("dadosDepois").asString()).contains("DUN-01", "quantidadeProduto");
    }

    @Test
    void exigeTokenAssinadoParaCadastros() throws Exception {
        assertThat(enviar("GET", "/clientes", null, null).statusCode()).isEqualTo(401);
        var invalido = enviar("GET", "/clientes", null, "token-invalido");
        assertThat(invalido.statusCode()).isEqualTo(401);
        assertThat(json(invalido).get("codigo").asString()).isEqualTo("NAO_AUTENTICADO");
        assertThat(invalido.headers().firstValue("WWW-Authenticate")).contains("Bearer");
        assertThat(invalido.headers().firstValue("Set-Cookie")).isEmpty();
    }

    @Test
    void rejeitaTokensExpiradosComEmissorOuAudienciaIncorretos() throws Exception {
        Instant agora = Instant.now();
        for (String invalido :
                List.of(
                        assinar(
                                "GESTOR",
                                List.of(),
                                List.of(),
                                "https://outro.invalid",
                                "wms-testes",
                                agora,
                                agora.plusSeconds(300)),
                        assinar(
                                "GESTOR",
                                List.of(),
                                List.of(),
                                "https://identidade.test.invalid",
                                "outro",
                                agora,
                                agora.plusSeconds(300)),
                        assinar(
                                "GESTOR",
                                List.of(),
                                List.of(),
                                "https://identidade.test.invalid",
                                "wms-testes",
                                agora.minusSeconds(600),
                                agora.minusSeconds(300)),
                        assinar(
                                "GESTOR",
                                List.of(),
                                List.of(),
                                "https://identidade.test.invalid",
                                "wms-testes",
                                agora,
                                agora.plusSeconds(3600)))) {
            assertThat(enviar("GET", "/clientes", null, invalido).statusCode()).isEqualTo(401);
        }
    }

    @Test
    void rejeitaPerfilDesconhecidoEAssinaturaAdulterada() throws Exception {
        assertThat(
                        enviar("GET", "/clientes", null, token("ADMIN", List.of(), List.of()))
                                .statusCode())
                .isEqualTo(401);
        String[] partes = gestor.split("\\.");
        String assinatura = (partes[2].charAt(0) == 'A' ? "B" : "A") + partes[2].substring(1);
        assertThat(
                        enviar(
                                        "GET",
                                        "/clientes",
                                        null,
                                        partes[0] + "." + partes[1] + "." + assinatura)
                                .statusCode())
                .isEqualTo(401);
    }

    @Test
    void supervisorEOperacaoNaoAlteramCadastrosNemLeemAuditoria() throws Exception {
        long id = criarCliente();
        for (String perfil : List.of("SUPERVISOR", "OPERACAO")) {
            String token = token(perfil, List.of(id), List.of());
            assertThat(enviar("POST", "/clientes", cliente(), token).statusCode()).isEqualTo(403);
            assertThat(
                            enviar("PUT", "/clientes/" + id, alteracao("Nome indevido", 0), token)
                                    .statusCode())
                    .isEqualTo(403);
            assertThat(
                            enviar("POST", "/clientes/" + id + "/encerramento", revisao(0), token)
                                    .statusCode())
                    .isEqualTo(403);
            assertThat(
                            enviar("GET", "/auditoria?tipo=CLIENTE&registroId=" + id, null, token)
                                    .statusCode())
                    .isEqualTo(403);
        }
        assertThat(
                        jdbc.queryForObject(
                                "select count(*) from wms.auditoria_cadastro", Integer.class))
                .isEqualTo(1);
    }

    @Test
    void restringeListasEDetalhesAoClienteDoUsuario() throws Exception {
        long permitido = criarCliente();
        long proibido = criarCliente();
        long produto = criarProduto(proibido, "SKU-OCULTO", "CONTAGEM", 0);
        long embalagem =
                criar("embalagens", embalagem(produto, "DUN-OCULTO", "10")).get("id").longValue();
        String operador = token("OPERACAO", List.of(permitido), List.of());
        var clientes = json(enviar("GET", "/clientes", null, operador));
        assertThat(clientes.get("totalItens").longValue()).isEqualTo(1);
        assertThat(clientes.get("itens").get(0).get("id").longValue()).isEqualTo(permitido);
        for (String rota :
                List.of(
                        "/clientes/" + proibido,
                        "/produtos?clienteId=" + proibido,
                        "/produtos/" + produto,
                        "/embalagens?produtoId=" + produto,
                        "/embalagens/" + embalagem)) {
            assertThat(enviar("GET", rota, null, operador).statusCode()).as(rota).isEqualTo(403);
        }
    }

    @Test
    void restringeArmazensEEnderecos() throws Exception {
        long permitido = criarArmazem();
        long proibido = criarArmazem();
        long endereco =
                criar("enderecos", endereco(proibido, "A101", "A", "01")).get("id").longValue();
        String operador = token("SUPERVISOR", List.of(), List.of(permitido));
        assertThat(json(enviar("GET", "/armazens", null, operador)).get("totalItens").longValue())
                .isEqualTo(1);
        for (String rota :
                List.of(
                        "/armazens/" + proibido,
                        "/enderecos?armazemId=" + proibido,
                        "/enderecos/" + endereco)) {
            assertThat(enviar("GET", rota, null, operador).statusCode()).isEqualTo(403);
        }
    }

    @Test
    void usuarioSemEscopoRecebeListaVazia() throws Exception {
        criarCliente();
        String operador = token("OPERACAO", List.of(), List.of());
        assertThat(json(enviar("GET", "/clientes", null, operador)).get("totalItens").longValue())
                .isZero();
    }

    @Test
    void normalizaIdentidadeERecusaDocumentoFiscalDuplicado(CapturedOutput output)
            throws Exception {
        Map<String, Object> dados = new LinkedHashMap<>(cliente());
        dados.put("codigo", "cliente-01");
        dados.put("documentoFiscal", "00.000.000/0000-01");
        var primeiro = criar("clientes", dados);
        assertThat(primeiro.get("codigo").asString()).isEqualTo("CLIENTE-01");
        assertThat(primeiro.get("documentoFiscal").asString()).isEqualTo("00000000000001");
        dados.put("codigo", "cliente-02");
        dados.put("documentoFiscal", "00000000000001");
        var duplicado = enviar("POST", "/clientes", dados, gestor);
        assertThat(duplicado.statusCode()).isEqualTo(409);
        assertThat(duplicado.body()).doesNotContain("00000000000001", "insert into", "SQL");
        assertThat(output.getAll()).doesNotContain("00000000000001", "insert into wms.cliente");
        assertThat(jdbc.queryForObject("select count(*) from wms.cliente", Integer.class))
                .isEqualTo(1);
    }

    @Test
    void permiteMesmoSkuEntreClientesMasImpedeDuplicacaoNoProprietario() throws Exception {
        long primeiro = criarCliente();
        long segundo = criarCliente();
        criarProduto(primeiro, "SKU-01", "CONTAGEM", 0);
        criarProduto(segundo, "SKU-01", "CONTAGEM", 0);
        assertThat(
                        enviar(
                                        "POST",
                                        "/produtos",
                                        produto(primeiro, "sku-01", "CONTAGEM", 0),
                                        gestor)
                                .statusCode())
                .isEqualTo(409);
    }

    @Test
    void rejeitaConversaoFracionariaDeItemContado() throws Exception {
        long produto = criarProduto(criarCliente(), "SKU-01", "CONTAGEM", 0);
        assertThat(
                        enviar("POST", "/embalagens", embalagem(produto, "DUN-01", "1.5"), gestor)
                                .statusCode())
                .isEqualTo(400);
        assertThat(jdbc.queryForObject("select count(*) from wms.embalagem", Integer.class))
                .isZero();
    }

    @Test
    void respeitaPrecisaoDeProdutoMedidoEConversaoNaoCriaEstoque() throws Exception {
        long produto = criarProduto(criarCliente(), "PESO", "MEDIDA", 3);
        criar("embalagens", embalagem(produto, "BOBINA", "10.125"));
        assertThat(
                        enviar(
                                        "POST",
                                        "/embalagens",
                                        embalagem(produto, "INVALIDA", "1.0001"),
                                        gestor)
                                .statusCode())
                .isEqualTo(400);
        assertThat(jdbc.queryForObject("select count(*) from wms.embalagem", Integer.class))
                .isEqualTo(1);
    }

    @Test
    void exigeConfiguracaoCoerenteDeContagemEValidade() throws Exception {
        long cliente = criarCliente();
        assertThat(
                        enviar(
                                        "POST",
                                        "/produtos",
                                        produto(cliente, "CONTADO", "CONTAGEM", 3),
                                        gestor)
                                .statusCode())
                .isEqualTo(400);
        Map<String, Object> dados =
                new LinkedHashMap<>(produto(cliente, "VALIDADE", "CONTAGEM", 0));
        dados.put("controlaValidade", true);
        assertThat(enviar("POST", "/produtos", dados, gestor).statusCode()).isEqualTo(400);
        dados.put("antecedenciaAvisoDias", 30);
        assertThat(enviar("POST", "/produtos", dados, gestor).statusCode()).isEqualTo(201);
    }

    @Test
    void recusaCamposDesconhecidosSemAlterarIdentidadeDoProduto() throws Exception {
        long produto = criarProduto(criarCliente(), "SKU-01", "CONTAGEM", 0);
        var response =
                enviar(
                        "PUT",
                        "/produtos/" + produto,
                        Map.of(
                                "versao",
                                0,
                                "descricao",
                                "Outra descrição",
                                "motivo",
                                "Correção de descrição",
                                "clienteId",
                                999),
                        gestor);
        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(
                        json(enviar("GET", "/produtos/" + produto, null, gestor))
                                .get("versao")
                                .longValue())
                .isZero();
    }

    @Test
    void identificaEnderecoSemAmbiguidadeDentroDoArmazem() throws Exception {
        long armazem = criarArmazem();
        criar("enderecos", endereco(armazem, "A101", "A", "01"));
        assertThat(
                        enviar("POST", "/enderecos", endereco(armazem, "OUTRO", "A", "01"), gestor)
                                .statusCode())
                .isEqualTo(409);
        assertThat(
                        enviar("POST", "/enderecos", endereco(armazem, "A101", "B", "02"), gestor)
                                .statusCode())
                .isEqualTo(409);
        criar("enderecos", endereco(criarArmazem(), "A101", "A", "01"));
    }

    @Test
    void encerramentoPreservaHistoricoEBloqueiaNovosVinculosAteReativacao() throws Exception {
        long cliente = criarCliente();
        var encerramento =
                enviar("POST", "/clientes/" + cliente + "/encerramento", revisao(0), gestor);
        assertThat(encerramento.statusCode()).isEqualTo(200);
        assertThat(json(encerramento).get("situacao").asString())
                .isEqualTo("ENCERRAMENTO_PENDENTE");
        assertThat(
                        enviar("POST", "/produtos", produto(cliente, "NOVO", "CONTAGEM", 0), gestor)
                                .statusCode())
                .isEqualTo(409);
        assertThat(enviar("DELETE", "/clientes/" + cliente, null, gestor).statusCode())
                .isEqualTo(405);
        assertThat(
                        enviar("POST", "/clientes/" + cliente + "/reativacao", revisao(1), gestor)
                                .statusCode())
                .isEqualTo(200);
        criarProduto(cliente, "NOVO", "CONTAGEM", 0);
        assertThat(
                        jdbc.queryForObject(
                                "select count(*) from wms.auditoria_cadastro where tipo='CLIENTE'",
                                Integer.class))
                .isEqualTo(3);
    }

    @Test
    void encerramentoDeClienteOuProdutoImpedeNovaEmbalagem() throws Exception {
        long cliente = criarCliente();
        long produto = criarProduto(cliente, "SKU-01", "CONTAGEM", 0);
        assertThat(
                        enviar("POST", "/produtos/" + produto + "/encerramento", revisao(0), gestor)
                                .statusCode())
                .isEqualTo(200);
        assertThat(
                        enviar("POST", "/embalagens", embalagem(produto, "DUN-01", "10"), gestor)
                                .statusCode())
                .isEqualTo(409);
        assertThat(
                        enviar("POST", "/produtos/" + produto + "/reativacao", revisao(1), gestor)
                                .statusCode())
                .isEqualTo(200);
        assertThat(
                        enviar("POST", "/clientes/" + cliente + "/encerramento", revisao(0), gestor)
                                .statusCode())
                .isEqualTo(200);
        assertThat(
                        enviar("POST", "/embalagens", embalagem(produto, "DUN-01", "10"), gestor)
                                .statusCode())
                .isEqualTo(409);
    }

    @Test
    void armazemEmEncerramentoImpedeNovoEndereco() throws Exception {
        long armazem = criarArmazem();
        assertThat(
                        enviar("POST", "/armazens/" + armazem + "/encerramento", revisao(0), gestor)
                                .statusCode())
                .isEqualTo(200);
        assertThat(
                        enviar("POST", "/enderecos", endereco(armazem, "A101", "A", "01"), gestor)
                                .statusCode())
                .isEqualTo(409);
    }

    @Test
    void rejeitaEdicaoDesatualizadaSemGerarAuditoriaAdicional() throws Exception {
        long cliente = criarCliente();
        var alterado =
                enviar("PUT", "/clientes/" + cliente, alteracao("Nome corrigido", 0), gestor);
        assertThat(alterado.statusCode()).isEqualTo(200);
        assertThat(json(alterado).get("versao").longValue()).isEqualTo(1);
        var repetido = enviar("PUT", "/clientes/" + cliente, alteracao("Nome atrasado", 0), gestor);
        assertThat(repetido.statusCode()).isEqualTo(409);
        assertThat(json(repetido).get("codigo").asString()).isEqualTo("VERSAO_DESATUALIZADA");
        assertThat(
                        jdbc.queryForObject(
                                "select count(*) from wms.auditoria_cadastro", Integer.class))
                .isEqualTo(2);
    }

    @Test
    void duasEdicoesSimultaneasNaoSobrescrevemUmaAOutra() throws Exception {
        long cliente = criarCliente();
        try (var executor = Executors.newFixedThreadPool(2)) {
            var sinal = new CountDownLatch(1);
            Callable<Integer> primeira =
                    () -> {
                        sinal.await();
                        return enviar(
                                        "PUT",
                                        "/clientes/" + cliente,
                                        alteracao("Primeira", 0),
                                        gestor)
                                .statusCode();
                    };
            Callable<Integer> segunda =
                    () -> {
                        sinal.await();
                        return enviar(
                                        "PUT",
                                        "/clientes/" + cliente,
                                        alteracao("Segunda", 0),
                                        gestor)
                                .statusCode();
                    };
            var a = executor.submit(primeira);
            var b = executor.submit(segunda);
            sinal.countDown();
            assertThat(List.of(a.get(15, TimeUnit.SECONDS), b.get(15, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(200, 409);
        }
        assertThat(
                        jdbc.queryForObject(
                                "select count(*) from wms.auditoria_cadastro", Integer.class))
                .isEqualTo(2);
    }

    @Test
    void revalidaProdutoEncerradoEnquantoCriacaoDeEmbalagemAguardaLock() throws Exception {
        long cliente = criarCliente();
        long produto = criarProduto(cliente, "SKU-01", "CONTAGEM", 0);
        try (var connection = jdbc.getDataSource().getConnection();
                var executor = Executors.newSingleThreadExecutor()) {
            connection.setAutoCommit(false);
            try (var lock =
                    connection.prepareStatement(
                            "select id from wms.cliente where id = ? for update")) {
                lock.setLong(1, cliente);
                try (var resultado = lock.executeQuery()) {
                    assertThat(resultado.next()).isTrue();
                }
            }
            var criacao =
                    executor.submit(
                            () ->
                                    enviar(
                                            "POST",
                                            "/embalagens",
                                            embalagem(produto, "DUN-01", "10"),
                                            gestor));
            try {
                Instant limite = Instant.now().plusSeconds(5);
                boolean aguardando = false;
                while (Instant.now().isBefore(limite) && !aguardando) {
                    aguardando =
                            jdbc.queryForObject(
                                            "select count(*) from information_schema.sessions "
                                                    + "where blocker_id is not null and lower(executing_statement) like '%wms.cliente%'",
                                            Integer.class)
                                    > 0;
                    if (!aguardando) {
                        Thread.sleep(25);
                    }
                }
                assertThat(aguardando)
                        .as("Criação deve estar aguardando o lock do cliente")
                        .isTrue();
                assertThat(
                                enviar(
                                                "POST",
                                                "/produtos/" + produto + "/encerramento",
                                                revisao(0),
                                                gestor)
                                        .statusCode())
                        .isEqualTo(200);
            } finally {
                connection.rollback();
            }
            var resultado = criacao.get(15, TimeUnit.SECONDS);
            assertThat(resultado.statusCode()).as(resultado.body()).isEqualTo(409);
            assertThat(json(resultado).get("codigo").asString())
                    .isEqualTo("CADASTRO_EM_ENCERRAMENTO");
        }
        assertThat(jdbc.queryForObject("select count(*) from wms.embalagem", Integer.class))
                .isZero();
    }

    @Test
    void falhaNaAuditoriaDesfazCadastroNaMesmaTransacao() throws Exception {
        jdbc.execute(
                "alter table wms.auditoria_cadastro add constraint teste_falha_auditoria check (tipo <> 'CLIENTE')");
        try {
            assertThat(enviar("POST", "/clientes", cliente(), gestor).statusCode()).isEqualTo(409);
            assertThat(jdbc.queryForObject("select count(*) from wms.cliente", Integer.class))
                    .isZero();
            assertThat(
                            jdbc.queryForObject(
                                    "select count(*) from wms.auditoria_cadastro", Integer.class))
                    .isZero();
        } finally {
            jdbc.execute(
                    "alter table wms.auditoria_cadastro drop constraint teste_falha_auditoria");
        }
    }

    @Test
    void paginacaoTemLimitesEOrdemEstavel() throws Exception {
        long primeiro = criarCliente();
        long segundo = criarCliente();
        assertThat(
                        json(enviar("GET", "/clientes?tamanho=1&pagina=0", null, gestor))
                                .get("itens")
                                .get(0)
                                .get("id")
                                .longValue())
                .isEqualTo(primeiro);
        assertThat(
                        json(enviar("GET", "/clientes?tamanho=1&pagina=1", null, gestor))
                                .get("itens")
                                .get(0)
                                .get("id")
                                .longValue())
                .isEqualTo(segundo);
        assertThat(enviar("GET", "/clientes?tamanho=101", null, gestor).statusCode())
                .isEqualTo(400);
        assertThat(enviar("GET", "/clientes?pagina=-1", null, gestor).statusCode()).isEqualTo(400);
        assertThat(
                        enviar("GET", "/clientes?pagina=30000000&tamanho=100", null, gestor)
                                .statusCode())
                .isEqualTo(400);
    }

    @Test
    void rejeitaVinculoInexistenteEValoresInvalidos() throws Exception {
        assertThat(
                        enviar(
                                        "POST",
                                        "/produtos",
                                        produto(Long.MAX_VALUE, "SKU", "CONTAGEM", 0),
                                        gestor)
                                .statusCode())
                .isEqualTo(404);
        Map<String, Object> dados = new LinkedHashMap<>(cliente());
        dados.put("nome", " ");
        assertThat(enviar("POST", "/clientes", dados, gestor).statusCode()).isEqualTo(400);
        long armazem = criarArmazem();
        dados = new LinkedHashMap<>(endereco(armazem, "A101", "A", "01"));
        dados.put("capacidadePesoKg", -1);
        assertThat(enviar("POST", "/enderecos", dados, gestor).statusCode()).isEqualTo(400);
    }

    private long criarCliente() throws Exception {
        return criar("clientes", cliente()).get("id").longValue();
    }

    private long criarArmazem() throws Exception {
        return criar(
                        "armazens",
                        Map.of(
                                "codigo",
                                "ARM-" + SEQUENCIA.incrementAndGet(),
                                "nome",
                                "Armazém fictício",
                                "documentoFiscal",
                                "00000000000001",
                                "cidade",
                                "Cidade fictícia",
                                "uf",
                                "SP"))
                .get("id")
                .longValue();
    }

    private long criarProduto(long cliente, String sku, String tipo, int precisao)
            throws Exception {
        return criar("produtos", produto(cliente, sku, tipo, precisao)).get("id").longValue();
    }

    private Map<String, Object> cliente() {
        long numero = SEQUENCIA.incrementAndGet();
        return Map.of(
                "codigo",
                "CLI-" + numero,
                "nome",
                "Cliente fictício",
                "documentoFiscal",
                String.format("%014d", numero));
    }

    private Map<String, Object> produto(long cliente, String sku, String tipo, int precisao) {
        return Map.of(
                "clienteId",
                cliente,
                "sku",
                sku,
                "descricao",
                "Produto fictício",
                "unidadeMedida",
                "MEDIDA".equals(tipo) ? "KG" : "UN",
                "tipoQuantidade",
                tipo,
                "precisaoQuantidade",
                precisao,
                "controlaLote",
                false,
                "controlaValidade",
                false);
    }

    private Map<String, Object> embalagem(long produto, String codigo, String quantidade) {
        return Map.of(
                "produtoId",
                produto,
                "codigoDun",
                codigo,
                "descricao",
                "Embalagem fictícia",
                "quantidadeProduto",
                new BigDecimal(quantidade));
    }

    private Map<String, Object> endereco(long armazem, String codigo, String rua, String posicao) {
        return Map.of(
                "armazemId",
                armazem,
                "codigo",
                codigo,
                "rua",
                rua,
                "nivel",
                1,
                "posicao",
                posicao,
                "descricao",
                "Posição fictícia",
                "tipo",
                "ARMAZENAGEM",
                "sequenciaColeta",
                1);
    }

    private Map<String, Object> alteracao(String nome, long versao) {
        return Map.of("nome", nome, "versao", versao, "motivo", "Correção cadastral");
    }

    private Map<String, Object> revisao(long versao) {
        return Map.of("versao", versao, "motivo", "Solicitação de teste");
    }

    private JsonNode criar(String recurso, Object dados) throws Exception {
        var response = enviar("POST", "/" + recurso, dados, gestor);
        assertThat(response.statusCode()).as(response.body()).isEqualTo(201);
        assertThat(response.headers().firstValue("Location")).isPresent();
        return json(response);
    }

    private HttpResponse<String> enviar(String metodo, String caminho, Object dados, String token)
            throws Exception {
        var request =
                HttpRequest.newBuilder(
                                URI.create(
                                        "http://127.0.0.1:"
                                                + environment.getRequiredProperty(
                                                        "local.server.port")
                                                + "/api/v1"
                                                + caminho))
                        .timeout(Duration.ofSeconds(15))
                        .header("Content-Type", "application/json");
        if (token != null) {
            request.header("Authorization", "Bearer " + token);
        }
        return client.send(
                request.method(
                                metodo,
                                dados == null
                                        ? HttpRequest.BodyPublishers.noBody()
                                        : HttpRequest.BodyPublishers.ofString(
                                                mapper.writeValueAsString(dados)))
                        .build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private JsonNode json(HttpResponse<String> response) {
        return mapper.readTree(response.body());
    }

    private String token(String perfil, List<Long> clientes, List<Long> armazens) {
        Instant agora = Instant.now();
        return assinar(
                perfil,
                clientes,
                armazens,
                "https://identidade.test.invalid",
                "wms-testes",
                agora,
                agora.plusSeconds(300));
    }

    private String assinar(
            String perfil,
            List<Long> clientes,
            List<Long> armazens,
            String issuer,
            String audience,
            Instant inicio,
            Instant fim) {
        var claims =
                JwtClaimsSet.builder()
                        .issuer(issuer)
                        .subject("usuario-teste")
                        .audience(List.of(audience))
                        .issuedAt(inicio)
                        .expiresAt(fim)
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
