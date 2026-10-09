package br.com.rodogarcia.wms;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.rodogarcia.wms.dto.ConfiguracaoCobrancaDto;
import br.com.rodogarcia.wms.dto.FiscalCadastroDto;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Assertivas test-only dos dez pares fixados antes. Nao cria comandos nem aceita demais callers.
 */
final class D30ParesFinanceiros {
    private D30ParesFinanceiros() {}

    record Par(
            String tipo,
            String entidade,
            String acao,
            Class<?> dto,
            Object alvo,
            Long alvoAuditoria,
            Long cliente,
            Long armazem) {}

    record Antes(
            String uuid,
            List<Map<String, Object>> operacoes,
            List<Map<String, Object>> auditorias,
            Object dados,
            String token) {}

    static Antes antes(
            JdbcTemplate jdbc, JsonMapper mapper, String rota, Object dados, String token) {
        if (!"D30_FINAL".equals(System.getProperty("wms.test.leituras.sem-efeito"))) return null;
        var body = mapper.valueToTree(dados);
        if (!body.hasNonNull("operacaoId")) return null;
        if (!rota.matches(
                        "/api/v1/(servicos-cobranca|tabelas-cobranca|vinculos-tabela|contratos-cobranca)(/\\d+/encerramento)?")
                && !rota.matches(
                        "/api/v1/(clientes|armazens|produtos)/\\d+/(complemento-fiscal|referencias-fiscais)"))
            return null;
        return new Antes(
                body.get("operacaoId").asString(),
                jdbc.queryForList("select * from wms.operacao_administrativa order by id"),
                jdbc.queryForList("select * from wms.auditoria_cadastro order by id"),
                dados,
                token);
    }

    static void depois(
            JdbcTemplate jdbc,
            JsonMapper mapper,
            String rota,
            Antes antes,
            HttpResponse<String> resposta)
            throws Exception {
        if (antes == null || resposta.statusCode() < 200 || resposta.statusCode() >= 300) return;
        var par = par(jdbc, rota, mapper.valueToTree(antes.dados()));
        assertThat(par).isNotNull();
        var dto = mapper.convertValue(antes.dados(), par.dto());
        assertThat(antes.token()).isNotNull();
        String usuario =
                mapper.readTree(Base64.getUrlDecoder().decode(antes.token().split("\\.")[1]))
                        .get("sub")
                        .asString();
        var retorno = mapper.readTree(resposta.body());
        long recurso = retorno.get("id").longValue();
        long alvoAuditoria = par.alvoAuditoria() == null ? recurso : par.alvoAuditoria();
        String correlacao = resposta.headers().firstValue("X-Request-Id").orElseThrow();
        UUID.fromString(correlacao);
        var operacoes = jdbc.queryForList("select * from wms.operacao_administrativa order by id");
        var auditorias = jdbc.queryForList("select * from wms.auditoria_cadastro order by id");
        assertThat(operacoes).containsAll(antes.operacoes());
        assertThat(auditorias).containsAll(antes.auditorias());
        var pertinentes =
                operacoes.stream().filter(o -> antes.uuid().equals(o.get("OPERACAO_ID"))).toList();
        assertThat(pertinentes).hasSize(1);
        var op = pertinentes.getFirst();
        boolean replay =
                antes.operacoes().stream().anyMatch(o -> antes.uuid().equals(o.get("OPERACAO_ID")));
        String hash =
                HexFormat.of()
                        .formatHex(
                                MessageDigest.getInstance("SHA-256")
                                        .digest(
                                                mapper.writeValueAsBytes(
                                                        Arrays.asList(
                                                                par.tipo(), par.alvo(), dto))));
        assertThat(op.get("TIPO")).isEqualTo(par.tipo());
        assertThat(((Number) op.get("RECURSO_ID")).longValue()).isEqualTo(recurso);
        assertThat(numero(op.get("CLIENTE_ID"))).isEqualTo(par.cliente());
        assertThat(numero(op.get("ARMAZEM_ID"))).isEqualTo(par.armazem());
        assertThat(op.get("USUARIO")).isEqualTo(usuario);
        assertThat(op.get("CONTEUDO_HASH")).isEqualTo(hash);
        assertThat(mapper.readTree((String) op.get("RESULTADO"))).isEqualTo(retorno);
        var eventos =
                auditorias.stream().filter(a -> correlacao.equals(a.get("ID_OPERACAO"))).toList();
        assertThat(eventos).hasSize(replay ? 0 : 1);
        if (!replay) {
            var evento = eventos.getFirst();
            assertThat(evento.get("TIPO")).isEqualTo(par.entidade());
            assertThat(evento.get("ACAO")).isEqualTo(par.acao());
            assertThat(numero(evento.get("REGISTRO_ID"))).isEqualTo(alvoAuditoria);
            assertThat(evento.get("USUARIO")).isEqualTo(usuario);
            assertThat(evento.get("MOTIVO"))
                    .isEqualTo(mapper.valueToTree(dto).get("motivo").asString().strip());
            var depois = mapper.readTree((String) evento.get("DADOS_DEPOIS"));
            assertThat(depois.has("resultado") ? depois.get("resultado") : depois)
                    .isEqualTo(retorno);
        }
        var dir = Path.of(System.getProperty("wms.test.evidencias.dir"));
        Files.createDirectories(dir);
        Files.writeString(
                dir.resolve("d30-cedro-par-financeiro-" + UUID.randomUUID() + ".json"),
                mapper.writeValueAsString(
                        Map.of(
                                "caso",
                                D30CasoLocalListener.caso("CobrancaIntegrationTest"),
                                "rota",
                                rota,
                                "tipoOperacao",
                                par.tipo(),
                                "tipoAuditoria",
                                par.entidade(),
                                "acaoAuditoria",
                                par.acao(),
                                "recurso",
                                recurso,
                                "alvoAuditoria",
                                alvoAuditoria,
                                "hashEsperadoIndependente",
                                hash,
                                "replay",
                                replay,
                                "linhasHistoricasPreservadas",
                                true)),
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE_NEW);
    }

    private static Long numero(Object o) {
        return o == null ? null : ((Number) o).longValue();
    }

    private static Long id(JsonNode body, String campo) {
        return body.hasNonNull(campo) ? body.get(campo).longValue() : null;
    }

    private static Par par(JdbcTemplate jdbc, String rota, JsonNode body) {
        if (rota.equals("/api/v1/servicos-cobranca"))
            return new Par(
                    "CRIACAO_SERVICO",
                    "SERVICO_COBRANCA",
                    "CRIACAO",
                    ConfiguracaoCobrancaDto.CriarServico.class,
                    null,
                    null,
                    null,
                    null);
        if (rota.equals("/api/v1/tabelas-cobranca"))
            return new Par(
                    "CRIACAO_TABELA",
                    "TABELA_COBRANCA",
                    "CRIACAO",
                    ConfiguracaoCobrancaDto.CriarTabela.class,
                    null,
                    null,
                    id(body, "clienteId"),
                    id(body, "armazemId"));
        if (rota.equals("/api/v1/vinculos-tabela"))
            return new Par(
                    "VINCULO_TABELA",
                    "CONTRATO_COBRANCA",
                    "VINCULO_TABELA",
                    ConfiguracaoCobrancaDto.Vincular.class,
                    null,
                    null,
                    id(body, "clienteId"),
                    id(body, "armazemId"));
        if (rota.equals("/api/v1/contratos-cobranca"))
            return new Par(
                    "CONFIGURACAO",
                    "CONTRATO_COBRANCA",
                    "CONFIGURACAO",
                    ConfiguracaoCobrancaDto.ConfigurarContrato.class,
                    null,
                    null,
                    id(body, "clienteId"),
                    id(body, "armazemId"));
        var encerramento =
                java.util.regex.Pattern.compile(
                                "/api/v1/(tabelas-cobranca|vinculos-tabela|contratos-cobranca)/(\\d+)/encerramento")
                        .matcher(rota);
        if (encerramento.matches()) {
            long id = Long.parseLong(encerramento.group(2));
            String raiz = encerramento.group(1);
            boolean tabela = raiz.equals("tabelas-cobranca"),
                    vinculo = raiz.equals("vinculos-tabela");
            String nome =
                    tabela
                            ? "tabela_cobranca"
                            : vinculo ? "vinculo_tabela_cliente" : "contrato_cobranca";
            var ctx =
                    jdbc.queryForMap(
                            "select cliente_id,armazem_id from wms." + nome + " where id=?", id);
            String tipo =
                    tabela ? "ENCERRAMENTO_VIGENCIA" : vinculo ? "VINCULO_TABELA" : "CONFIGURACAO";
            return new Par(
                    tipo,
                    tabela ? "TABELA_COBRANCA" : "CONTRATO_COBRANCA",
                    tipo,
                    ConfiguracaoCobrancaDto.Encerrar.class,
                    tabela ? List.of("TABELA", id) : id,
                    id,
                    numero(ctx.get("CLIENTE_ID")),
                    numero(ctx.get("ARMAZEM_ID")));
        }
        var fiscal =
                java.util.regex.Pattern.compile(
                                "/api/v1/(clientes|armazens|produtos)/(\\d+)/(complemento-fiscal|referencias-fiscais)")
                        .matcher(rota);
        if (!fiscal.matches()) return null;
        String raiz = fiscal.group(1);
        long id = Long.parseLong(fiscal.group(2));
        if (raiz.equals("produtos")) {
            Long cliente =
                    jdbc.queryForObject(
                            "select cliente_id from wms.produto where id=?", Long.class, id);
            return new Par(
                    "REFERENCIA_FISCAL",
                    "PRODUTO",
                    "REFERENCIA_FISCAL",
                    FiscalCadastroDto.Referenciar.class,
                    id,
                    id,
                    cliente,
                    id(body, "armazemId"));
        }
        String tipo = raiz.equals("clientes") ? "CLIENTE" : "ARMAZEM";
        return new Par(
                "COMPLEMENTO_FISCAL",
                tipo,
                "COMPLEMENTO_FISCAL",
                FiscalCadastroDto.Complementar.class,
                List.of(tipo, id),
                id,
                raiz.equals("clientes") ? id : null,
                raiz.equals("armazens") ? id : null);
    }
}
