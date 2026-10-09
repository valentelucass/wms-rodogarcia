package br.com.rodogarcia.wms;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.rodogarcia.wms.config.IdentidadeTesteConfig;
import br.com.rodogarcia.wms.models.ConteudoUnidade;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.json.JsonMapper;

@ActiveProfiles("test")
@TestPropertySource(
        locations = "classpath:cadastros-test.properties",
        properties =
                "spring.datasource.url=jdbc:h2:mem:d30-cedro-embalagem;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000;INIT=CREATE SCHEMA IF NOT EXISTS wms")
@Import(IdentidadeTesteConfig.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class D30EmbalagemFisicoTest {
    @Autowired private EntityManager em;
    @Autowired private PlatformTransactionManager transactions;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private JsonMapper mapper;
    @Autowired private Environment env;
    @Autowired private JwtEncoder encoder;

    @Test
    void criarEmbalagemValidaPreservaFotografiaFisicaCompletaEEstoquePreexistente()
            throws Exception {
        var origem = D30MarcosTest.origem();
        new TransactionTemplate(transactions)
                .executeWithoutResult(
                        tx -> {
                            for (var entidade :
                                    List.of(
                                            origem.cliente(),
                                            origem.armazem(),
                                            origem.produto(),
                                            origem.embalagem(),
                                            origem.pedido(),
                                            origem.nota(),
                                            origem.item(),
                                            origem.chegada1(),
                                            origem.chegada2(),
                                            origem.item1(),
                                            origem.item2(),
                                            origem.entrada1(),
                                            origem.entrada2(),
                                            origem.unidade())) em.persist(entidade);
                            em.persist(
                                    new ConteudoUnidade(
                                            origem.unidade(), origem.entrada2(), BigDecimal.TEN));
                            em.flush();
                        });
        assertThat(jdbc.queryForObject("select count(*) from wms.unidade_logistica", Integer.class))
                .isEqualTo(1);
        assertThat(
                        jdbc.queryForObject(
                                "select sum(quantidade) from wms.unidade_logistica",
                                BigDecimal.class))
                .isEqualByComparingTo("10");
        var antes = fotografia();
        assertThat(antes).hasSize(64);
        salvar("d30-cedro-embalagem-fisico-antes.json", antes);
        var temporaisAntes = D30FotografiaTemporal.capturar(jdbc);
        assertThat(temporaisAntes).hasSize(64);
        conferirMarcosTipados(temporaisAntes, origem.unidade().getId());
        salvar("d30-cedro-embalagem-temporais-antes.json", temporaisAntes);
        var agora = Instant.now();
        var claims =
                JwtClaimsSet.builder()
                        .issuer("https://identidade.test.invalid")
                        .audience(List.of("wms-testes"))
                        .subject("D30-ficticio")
                        .issuedAt(agora)
                        .expiresAt(agora.plusSeconds(600))
                        .claim("wms_perfil", "GESTOR")
                        .claim("wms_clientes", List.of())
                        .claim("wms_armazens", List.of())
                        .build();
        String token =
                encoder.encode(
                                JwtEncoderParameters.from(
                                        JwsHeader.with(SignatureAlgorithm.RS256).build(), claims))
                        .getTokenValue();
        var dados =
                Map.of(
                        "produtoId",
                        origem.produto().getId(),
                        "codigoDun",
                        "D30-NOVA",
                        "descricao",
                        "Nova embalagem ficticia sem efeito fisico",
                        "quantidadeProduto",
                        new BigDecimal("2"));
        var http = HttpClient.newHttpClient();
        String base =
                "http://127.0.0.1:"
                        + env.getRequiredProperty("local.server.port")
                        + "/api/v1/embalagens";
        var response =
                http.send(
                        HttpRequest.newBuilder(URI.create(base))
                                .header("Authorization", "Bearer " + token)
                                .header("Content-Type", "application/json")
                                .POST(
                                        HttpRequest.BodyPublishers.ofString(
                                                mapper.writeValueAsString(dados)))
                                .build(),
                        HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(201);
        var criado = mapper.readTree(response.body());
        assertThat(criado.get("codigoDun").asString()).isEqualTo("D30-NOVA");
        assertThat(criado.get("quantidadeProduto").decimalValue()).isEqualByComparingTo("2");
        var read =
                http.send(
                        HttpRequest.newBuilder(
                                        URI.create(base + "/" + criado.get("id").longValue()))
                                .header("Authorization", "Bearer " + token)
                                .GET()
                                .build(),
                        HttpResponse.BodyHandlers.ofString());
        assertThat(read.statusCode()).isEqualTo(200);
        var lido = mapper.readTree(read.body());
        for (String campo :
                List.of("id", "versao", "situacao", "produtoId", "codigoDun", "descricao")) {
            assertThat(lido.get(campo)).as(campo).isEqualTo(criado.get(campo));
        }
        assertThat(lido.get("quantidadeProduto").decimalValue()).isEqualByComparingTo("2");
        for (String campo : List.of("criadoEm", "alteradoEm")) {
            var registro = Instant.parse(criado.get(campo).asString());
            var persistido = Instant.parse(lido.get(campo).asString());
            assertThat(persistido.getNano() % 1000).isZero();
            assertThat(Duration.between(registro, persistido).abs())
                    .isLessThanOrEqualTo(Duration.ofNanos(1000));
        }
        var depois = fotografia();
        salvar("d30-cedro-embalagem-fisico-depois.json", depois);
        var temporaisDepois = D30FotografiaTemporal.capturar(jdbc);
        conferirMarcosTipados(temporaisDepois, origem.unidade().getId());
        salvar("d30-cedro-embalagem-temporais-depois.json", temporaisDepois);
        for (String tabela : antes.keySet()) {
            if (!List.of("EMBALAGEM", "AUDITORIA_CADASTRO").contains(tabela))
                assertThat(depois.get(tabela)).as(tabela).isEqualTo(antes.get(tabela));
            if (!List.of("EMBALAGEM", "AUDITORIA_CADASTRO").contains(tabela))
                assertThat(temporaisDepois.get(tabela))
                        .as("Temporal " + tabela)
                        .isEqualTo(temporaisAntes.get(tabela));
        }
        assertThat(depois.get("EMBALAGEM"))
                .hasSize(antes.get("EMBALAGEM").size() + 1)
                .containsAll(antes.get("EMBALAGEM"));
        assertThat(depois.get("AUDITORIA_CADASTRO"))
                .hasSize(antes.get("AUDITORIA_CADASTRO").size() + 1);
        assertThat(
                        jdbc.queryForObject(
                                "select count(*) from wms.auditoria_cadastro where tipo='EMBALAGEM' and acao='CRIACAO'",
                                Integer.class))
                .isEqualTo(1);
        var dir = Path.of(System.getProperty("wms.test.evidencias.dir"));
        Files.writeString(
                dir.resolve("d30-cedro-embalagem-fisico-fotos.json"),
                mapper.writerWithDefaultPrettyPrinter()
                        .writeValueAsString(
                                Map.of(
                                        "caso",
                                        "D30-L-CASO-EMBALAGEM-FISICO-001",
                                        "antes",
                                        antes,
                                        "depois",
                                        depois,
                                        "unidadePreexistente",
                                        origem.unidade().getCodigo(),
                                        "quantidadePreexistente",
                                        "10",
                                        "HTTP",
                                        List.of(201, 200))),
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE_NEW,
                StandardOpenOption.WRITE);
    }

    private Map<String, List<Map<String, Object>>> fotografia() {
        var foto = new LinkedHashMap<String, List<Map<String, Object>>>();
        var tabelas =
                jdbc.queryForList(
                        "select table_name from information_schema.tables where table_schema='WMS' and table_type='BASE TABLE' order by table_name",
                        String.class);
        for (String tabela : tabelas) {
            assertThat(tabela).matches("[A-Z_]+");
            foto.put(tabela, jdbc.queryForList("select * from wms." + tabela + " order by id"));
        }
        return foto;
    }

    private void conferirMarcosTipados(Map<String, List<D30FotografiaTemporal.Linha>> foto, Long id)
            throws Exception {
        var linha =
                foto.get("UNIDADE_LOGISTICA").stream()
                        .filter(l -> l.id() == id)
                        .findFirst()
                        .orElseThrow();
        var fifo = linha.colunas().get("DATA_FIFO");
        var chegada = linha.colunas().get("CHEGADA_REAL");
        assertThat(fifo.utc()).isEqualTo("2026-09-01T12:00:00.123456Z");
        assertThat(fifo.local()).isEqualTo("2026-09-01T12:00:00.123456");
        assertThat(fifo.nanos()).isEqualTo(123456000);
        assertThat(fifo.micros()).isEqualTo(123456);
        assertThat(fifo.escala()).isEqualTo(6);
        assertThat(fifo.calendario()).isEqualTo("UTC");
        assertThat(chegada.utc()).isEqualTo("2026-09-02T12:00:00.234567Z");
        assertThat(chegada.local()).isEqualTo("2026-09-02T12:00:00.234567");
        assertThat(chegada.nanos()).isEqualTo(234567000);
        assertThat(chegada.micros()).isEqualTo(234567);
        assertThat(linha.colunas().get("PRIMEIRO_ENDERECAMENTO_EM").nulo()).isTrue();
        assertThat(linha.colunas().get("INICIO_ARMAZENAGEM_EM").nulo()).isTrue();
        var serializado = mapper.readTree(mapper.writeValueAsString(linha));
        assertThat(serializado.get("colunas").get("DATA_FIFO").get("utc").asString())
                .isEqualTo("2026-09-01T12:00:00.123456Z");
        assertThat(serializado.get("colunas").get("CHEGADA_REAL").get("micros").intValue())
                .isEqualTo(234567);
        assertThat(
                        mapper.readValue(
                                mapper.writeValueAsString(linha),
                                D30FotografiaTemporal.Linha.class))
                .isEqualTo(linha);
    }

    private void salvar(String arquivo, Object dados) throws Exception {
        var dir = Path.of(System.getProperty("wms.test.evidencias.dir"));
        Files.writeString(
                dir.resolve(arquivo),
                mapper.writerWithDefaultPrettyPrinter().writeValueAsString(dados),
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE_NEW,
                StandardOpenOption.WRITE);
    }
}
