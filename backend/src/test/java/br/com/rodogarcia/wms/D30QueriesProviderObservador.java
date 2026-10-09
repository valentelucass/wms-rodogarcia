package br.com.rodogarcia.wms;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.rodogarcia.wms.repositories.AjusteFechamentoRepository;
import br.com.rodogarcia.wms.repositories.CargaInicialRepository;
import br.com.rodogarcia.wms.repositories.LinhaContingenciaRepository;
import br.com.rodogarcia.wms.repositories.MemoriaDiariaRepository;
import br.com.rodogarcia.wms.repositories.MemoriaServicoRepository;
import br.com.rodogarcia.wms.repositories.RevisaoContagemRepository;
import br.com.rodogarcia.wms.repositories.UnidadeLogisticaRepository;
import jakarta.persistence.EntityManagerFactory;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.sql.DataSource;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestContext;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.json.JsonMapper;

/**
 * Seleção do provider contra a fotografia física anterior, sem fixture ou caller de negócio novo.
 */
final class D30QueriesProviderObservador {
    private static final Set<String> CONTEXTOS =
            Set.of(
                    "CobrancaIntegrationTest",
                    "ContingenciaIntegrationTest",
                    "FechamentoIntegrationTest",
                    "PedidoSaidaIntegrationTest");

    private D30QueriesProviderObservador() {}

    static void conferir(TestContext test) throws Exception {
        String nome = test.getTestClass().getSimpleName();
        if (!"D30_FINAL".equals(System.getProperty("wms.test.leituras.sem-efeito"))
                || !CONTEXTOS.contains(nome)) return;
        assertThat(System.getProperty("wms.test.local.guard")).isEqualTo("D30");
        var context = test.getApplicationContext();
        var factory =
                context.getBean(EntityManagerFactory.class).unwrap(SessionFactoryImplementor.class);
        assertThat(factory.getJdbcServices().getDialect()).isInstanceOf(H2Dialect.class);
        var jdbc = context.getBean(JdbcTemplate.class);
        assertThat(jdbc.getDataSource()).isSameAs(context.getBean(DataSource.class));
        var antes = D30FotografiaFisica.capturar(jdbc);
        List<Map<String, Object>> provas = new ArrayList<>();
        Set<String> vistos = new HashSet<>();
        var tx = new TransactionTemplate(context.getBean(PlatformTransactionManager.class));
        tx.executeWithoutResult(
                status -> {
                    for (var linha : antes.get("AJUSTE_FECHAMENTO")) {
                        Long origem = numero(linha, "ORIGEM_VERSAO_ID");
                        if (!vistos.add("ajuste:" + origem)) continue;
                        var esperado =
                                ids(
                                        filtrar(
                                                antes.get("AJUSTE_FECHAMENTO"),
                                                "ORIGEM_VERSAO_ID",
                                                origem));
                        var obtido =
                                context
                                        .getBean(AjusteFechamentoRepository.class)
                                        .findByOrigemVersaoIdOrderByIdAsc(origem)
                                        .stream()
                                        .map(x -> x.getId())
                                        .toList();
                        conferir(
                                provas,
                                "AjusteFechamentoRepository.findByOrigemVersaoIdOrderByIdAsc",
                                origem,
                                esperado,
                                obtido);
                    }
                    for (var linha : antes.get("CARGA_INICIAL")) {
                        Long cliente = numero(linha, "CLIENTE_ID"),
                                armazem = numero(linha, "ARMAZEM_ID");
                        if (!vistos.add("carga:" + cliente + ":" + armazem)) continue;
                        var selecionados =
                                filtrar(
                                        filtrar(antes.get("CARGA_INICIAL"), "CLIENTE_ID", cliente),
                                        "ARMAZEM_ID",
                                        armazem);
                        var esperado = ids(selecionados);
                        var page =
                                context.getBean(CargaInicialRepository.class)
                                        .findByClienteIdAndArmazemId(
                                                cliente, armazem, pagina(esperado.size()));
                        assertThat(page.getTotalElements()).isEqualTo(esperado.size());
                        conferir(
                                provas,
                                "CargaInicialRepository.findByClienteIdAndArmazemId",
                                List.of(cliente, armazem),
                                esperado,
                                page.stream().map(x -> x.getId()).toList());
                    }
                    for (var linha : antes.get("LINHA_CONTINGENCIA")) {
                        Long cliente = numero(linha, "CLIENTE_ID"),
                                armazem = numero(linha, "ARMAZEM_ID");
                        var esperado =
                                ids(
                                        filtrar(
                                                filtrar(
                                                        antes.get("LINHA_CONTINGENCIA"),
                                                        "CLIENTE_ID",
                                                        cliente),
                                                "ARMAZEM_ID",
                                                armazem));
                        var repo = context.getBean(LinhaContingenciaRepository.class);
                        if (vistos.add("linha:" + cliente + ":" + armazem)) {
                            var page =
                                    repo.findByClienteIdAndArmazemId(
                                            cliente, armazem, pagina(esperado.size()));
                            assertThat(page.getTotalElements()).isEqualTo(esperado.size());
                            conferir(
                                    provas,
                                    "LinhaContingenciaRepository.findByClienteIdAndArmazemId",
                                    List.of(cliente, armazem),
                                    esperado,
                                    page.stream().map(x -> x.getId()).toList());
                        }
                        Long id = numero(linha, "ID");
                        conferir(
                                provas,
                                "LinhaContingenciaRepository.buscarParaAtualizar",
                                id,
                                List.of(id),
                                List.of(repo.buscarParaAtualizar(id).orElseThrow().getId()));
                    }
                    for (var linha : antes.get("MEMORIA_DIARIA")) {
                        Long calculo = numero(linha, "CALCULO_ID");
                        if (!vistos.add("diaria:" + calculo)) continue;
                        var esperado =
                                ids(filtrar(antes.get("MEMORIA_DIARIA"), "CALCULO_ID", calculo));
                        var rows =
                                context.getBean(MemoriaDiariaRepository.class)
                                        .findByCalculoIdOrderByDataAsc(calculo);
                        assertThat(rows.stream().map(x -> x.getData()).toList()).isSorted();
                        var obtido = rows.stream().map(x -> x.getId()).sorted().toList();
                        conferir(
                                provas,
                                "MemoriaDiariaRepository.findByCalculoIdOrderByDataAsc",
                                calculo,
                                esperado,
                                obtido);
                    }
                    for (var linha : antes.get("MEMORIA_SERVICO")) {
                        Long calculo = numero(linha, "CALCULO_ID");
                        if (!vistos.add("servico:" + calculo)) continue;
                        var esperado =
                                ids(filtrar(antes.get("MEMORIA_SERVICO"), "CALCULO_ID", calculo));
                        var obtido =
                                context
                                        .getBean(MemoriaServicoRepository.class)
                                        .findByCalculoIdOrderByIdAsc(calculo)
                                        .stream()
                                        .map(x -> x.getId())
                                        .toList();
                        conferir(
                                provas,
                                "MemoriaServicoRepository.findByCalculoIdOrderByIdAsc",
                                calculo,
                                esperado,
                                obtido);
                    }
                    for (var linha : antes.get("REVISAO_CONTAGEM")) {
                        Long contagem = numero(linha, "CONTAGEM_ID");
                        if (!vistos.add("contagem:" + contagem)) continue;
                        var esperado =
                                ids(
                                        filtrar(
                                                antes.get("REVISAO_CONTAGEM"),
                                                "CONTAGEM_ID",
                                                contagem));
                        var page =
                                context.getBean(RevisaoContagemRepository.class)
                                        .findByContagemId(contagem, pagina(esperado.size()));
                        assertThat(page.getTotalElements()).isEqualTo(esperado.size());
                        conferir(
                                provas,
                                "RevisaoContagemRepository.findByContagemId",
                                contagem,
                                esperado,
                                page.stream().map(x -> x.getId()).toList());
                    }
                    Map<Long, Map<String, Object>> pedidos = new LinkedHashMap<>();
                    for (var p : antes.get("PEDIDO_ENTRADA")) pedidos.put(numero(p, "ID"), p);
                    for (var linha : antes.get("UNIDADE_LOGISTICA")) {
                        if (!Boolean.TRUE.equals(linha.get("ATIVA"))) continue;
                        var pedido = pedidos.get(numero(linha, "PEDIDO_ID"));
                        Long cliente = numero(pedido, "CLIENTE_ID"),
                                armazem = numero(pedido, "ARMAZEM_ID"),
                                produto = numero(linha, "PRODUTO_ID");
                        if (!vistos.add("unidade:" + cliente + ":" + armazem + ":" + produto))
                            continue;
                        var selecionados =
                                antes.get("UNIDADE_LOGISTICA").stream()
                                        .filter(x -> Boolean.TRUE.equals(x.get("ATIVA")))
                                        .filter(x -> produto.equals(numero(x, "PRODUTO_ID")))
                                        .filter(
                                                x ->
                                                        cliente.equals(
                                                                numero(
                                                                        pedidos.get(
                                                                                numero(
                                                                                        x,
                                                                                        "PEDIDO_ID")),
                                                                        "CLIENTE_ID")))
                                        .filter(
                                                x ->
                                                        armazem.equals(
                                                                numero(
                                                                        pedidos.get(
                                                                                numero(
                                                                                        x,
                                                                                        "PEDIDO_ID")),
                                                                        "ARMAZEM_ID")))
                                        .toList();
                        var esperado = ids(selecionados);
                        var page =
                                context.getBean(UnidadeLogisticaRepository.class)
                                        .consultarEstoque(
                                                cliente,
                                                armazem,
                                                produto,
                                                null,
                                                pagina(esperado.size()));
                        assertThat(page.getTotalElements()).isEqualTo(esperado.size());
                        conferir(
                                provas,
                                "UnidadeLogisticaRepository.consultarEstoque",
                                List.of(cliente, armazem, produto),
                                esperado,
                                page.stream().map(x -> x.getId()).toList());
                        if (vistos.add("unidade-todos:" + cliente + ":" + armazem)) {
                            var todos =
                                    antes.get("UNIDADE_LOGISTICA").stream()
                                            .filter(x -> Boolean.TRUE.equals(x.get("ATIVA")))
                                            .filter(
                                                    x ->
                                                            cliente.equals(
                                                                    numero(
                                                                            pedidos.get(
                                                                                    numero(
                                                                                            x,
                                                                                            "PEDIDO_ID")),
                                                                            "CLIENTE_ID")))
                                            .filter(
                                                    x ->
                                                            armazem.equals(
                                                                    numero(
                                                                            pedidos.get(
                                                                                    numero(
                                                                                            x,
                                                                                            "PEDIDO_ID")),
                                                                            "ARMAZEM_ID")))
                                            .toList();
                            var idsTodos = ids(todos);
                            var semProduto =
                                    context.getBean(UnidadeLogisticaRepository.class)
                                            .consultarEstoque(
                                                    cliente,
                                                    armazem,
                                                    null,
                                                    null,
                                                    pagina(idsTodos.size()));
                            assertThat(semProduto.getTotalElements()).isEqualTo(idsTodos.size());
                            conferir(
                                    provas,
                                    "UnidadeLogisticaRepository.consultarEstoque",
                                    parametrosEstoque(cliente, armazem, null, null),
                                    idsTodos,
                                    semProduto.stream().map(x -> x.getId()).toList());
                        }
                        if (selecionados.stream()
                                .allMatch(x -> Boolean.TRUE.equals(x.get("BLOQUEADA")))) {
                            for (boolean disponivel : new boolean[] {false, true}) {
                                List<Long> idsBloqueadas = disponivel ? List.of() : esperado;
                                var bloqueadas =
                                        context.getBean(UnidadeLogisticaRepository.class)
                                                .consultarEstoque(
                                                        cliente,
                                                        armazem,
                                                        produto,
                                                        disponivel,
                                                        pagina(esperado.size()));
                                assertThat(bloqueadas.getTotalElements())
                                        .isEqualTo(idsBloqueadas.size());
                                conferir(
                                        provas,
                                        "UnidadeLogisticaRepository.consultarEstoque",
                                        parametrosEstoque(cliente, armazem, produto, disponivel),
                                        idsBloqueadas,
                                        bloqueadas.stream().map(x -> x.getId()).toList());
                            }
                        }
                    }
                    if (test.getTestMethod()
                            .getName()
                            .equals(
                                    "d30GetCadastrosEConfiguracoesConservamFotografiaComDadosPertinentes")) {
                        assertThat(antes.get("UNIDADE_LOGISTICA")).hasSize(1);
                        var unidade = antes.get("UNIDADE_LOGISTICA").get(0);
                        var pedido = pedidos.get(numero(unidade, "PEDIDO_ID"));
                        Long cliente = numero(pedido, "CLIENTE_ID"),
                                armazem = numero(pedido, "ARMAZEM_ID"),
                                produto = numero(unidade, "PRODUTO_ID");
                        assertThat(pedido.get("SITUACAO")).isEqualTo("EFETIVADO");
                        assertThat(unidade.get("ATIVA")).isEqualTo(true);
                        assertThat(unidade.get("BLOQUEADA")).isEqualTo(false);
                        assertThat(unidade.get("AVARIA_POSTERIOR")).isEqualTo(false);
                        assertThat(unidade.get("RESERVA_SAIDA_ID")).isNull();
                        assertThat(unidade.get("CONDICAO")).isEqualTo("BOA");
                        assertThat(unidade.get("TIPO_LOCALIZACAO")).isEqualTo("ARMAZENAGEM");
                        assertThat(antes.get("OCUPACAO_ENDERECO")).hasSize(1);
                        assertThat(antes.get("CONTAGEM_ESTOQUE")).isEmpty();
                        assertThat(antes.get("CARGA_INICIAL")).isEmpty();
                        var repo = context.getBean(UnidadeLogisticaRepository.class);
                        for (boolean disponivel : new boolean[] {true, false}) {
                            List<Long> esperado =
                                    disponivel ? List.of(numero(unidade, "ID")) : List.of();
                            var page =
                                    repo.consultarEstoque(
                                            cliente, armazem, produto, disponivel, pagina(20));
                            assertThat(page.getTotalElements()).isEqualTo(esperado.size());
                            conferir(
                                    provas,
                                    "UnidadeLogisticaRepository.consultarEstoque",
                                    List.of(cliente, armazem, produto, disponivel),
                                    esperado,
                                    page.stream().map(x -> x.getId()).toList());
                        }
                    }
                    if (!provas.isEmpty()) {
                        Long ausente = Long.MAX_VALUE;
                        for (var tabela : antes.values())
                            for (var row : tabela) {
                                if (row.get("ID") instanceof Number id)
                                    assertThat(id.longValue()).isNotEqualTo(ausente);
                            }
                        conferir(
                                provas,
                                "AjusteFechamentoRepository.findByOrigemVersaoIdOrderByIdAsc",
                                ausente,
                                List.of(),
                                context
                                        .getBean(AjusteFechamentoRepository.class)
                                        .findByOrigemVersaoIdOrderByIdAsc(ausente)
                                        .stream()
                                        .map(x -> x.getId())
                                        .toList());
                        var carga =
                                context.getBean(CargaInicialRepository.class)
                                        .findByClienteIdAndArmazemId(ausente, ausente, pagina(20));
                        assertThat(carga.getTotalElements()).isZero();
                        conferir(
                                provas,
                                "CargaInicialRepository.findByClienteIdAndArmazemId",
                                List.of(ausente, ausente),
                                List.of(),
                                carga.stream().map(x -> x.getId()).toList());
                        var linhas =
                                context.getBean(LinhaContingenciaRepository.class)
                                        .findByClienteIdAndArmazemId(ausente, ausente, pagina(20));
                        assertThat(linhas.getTotalElements()).isZero();
                        conferir(
                                provas,
                                "LinhaContingenciaRepository.findByClienteIdAndArmazemId",
                                List.of(ausente, ausente),
                                List.of(),
                                linhas.stream().map(x -> x.getId()).toList());
                        var linha =
                                context.getBean(LinhaContingenciaRepository.class)
                                        .buscarParaAtualizar(ausente);
                        assertThat(linha).isEmpty();
                        conferir(
                                provas,
                                "LinhaContingenciaRepository.buscarParaAtualizar",
                                ausente,
                                List.of(),
                                List.of());
                        conferir(
                                provas,
                                "MemoriaDiariaRepository.findByCalculoIdOrderByDataAsc",
                                ausente,
                                List.of(),
                                context
                                        .getBean(MemoriaDiariaRepository.class)
                                        .findByCalculoIdOrderByDataAsc(ausente)
                                        .stream()
                                        .map(x -> x.getId())
                                        .toList());
                        conferir(
                                provas,
                                "MemoriaServicoRepository.findByCalculoIdOrderByIdAsc",
                                ausente,
                                List.of(),
                                context
                                        .getBean(MemoriaServicoRepository.class)
                                        .findByCalculoIdOrderByIdAsc(ausente)
                                        .stream()
                                        .map(x -> x.getId())
                                        .toList());
                        var revisoes =
                                context.getBean(RevisaoContagemRepository.class)
                                        .findByContagemId(ausente, pagina(20));
                        assertThat(revisoes.getTotalElements()).isZero();
                        conferir(
                                provas,
                                "RevisaoContagemRepository.findByContagemId",
                                ausente,
                                List.of(),
                                revisoes.stream().map(x -> x.getId()).toList());
                        var unidades =
                                context.getBean(UnidadeLogisticaRepository.class)
                                        .consultarEstoque(
                                                ausente, ausente, ausente, null, pagina(20));
                        assertThat(unidades.getTotalElements()).isZero();
                        conferir(
                                provas,
                                "UnidadeLogisticaRepository.consultarEstoque",
                                List.of(ausente, ausente, ausente),
                                List.of(),
                                unidades.stream().map(x -> x.getId()).toList());
                    }
                });
        assertThat(D30FotografiaFisica.capturar(jdbc)).isEqualTo(antes);
        var documento = new LinkedHashMap<String, Object>();
        documento.put("contexto", nome);
        documento.put("caso", test.getTestClass().getName() + "#" + test.getTestMethod().getName());
        documento.put("queries", provas);
        documento.put("fisico64Igual", true);
        documento.put("SQLServer", false);
        documento.put(
                "oraculo",
                "IDs/contextos do estado físico anterior, algoritmo Java separado das queries; total/conjunto/ordem declarada exatos.");
        documento.put(
                "limite",
                "Seleção H2 dos parâmetros registrados; disponibilidade Boolean true/false exige testemunha própria. Lock local não comprova lock SQLServer. Sem caller de negócio novo.");
        Path destino =
                Path.of(System.getProperty("wms.test.evidencias.dir")).toAbsolutePath().normalize();
        assertThat(destino.toString()).contains("d30-cedro-");
        Files.writeString(
                destino.resolve("d30-cedro-queries-provider-" + UUID.randomUUID() + ".json"),
                JsonMapper.builder()
                        .build()
                        .writerWithDefaultPrettyPrinter()
                        .writeValueAsString(documento),
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE_NEW);
    }

    private static PageRequest pagina(int quantidade) {
        return PageRequest.of(0, Math.max(20, quantidade), Sort.by("id"));
    }

    private static Map<String, Object> parametrosEstoque(
            Long cliente, Long armazem, Long produto, Boolean disponivel) {
        Map<String, Object> parametros = new LinkedHashMap<>();
        parametros.put("clienteId", cliente);
        parametros.put("armazemId", armazem);
        parametros.put("produtoId", produto);
        parametros.put("disponivel", disponivel);
        return parametros;
    }

    private static Long numero(Map<String, Object> row, String coluna) {
        return ((Number) row.get(coluna)).longValue();
    }

    private static List<Map<String, Object>> filtrar(
            List<Map<String, Object>> rows, String coluna, Long valor) {
        return rows.stream().filter(x -> valor.equals(numero(x, coluna))).toList();
    }

    private static List<Long> ids(List<Map<String, Object>> rows) {
        return rows.stream().map(x -> numero(x, "ID")).sorted().toList();
    }

    private static void conferir(
            List<Map<String, Object>> provas,
            String query,
            Object parametros,
            List<Long> esperado,
            List<Long> obtido) {
        assertThat(obtido).as(query).containsExactlyElementsOf(esperado);
        provas.add(
                Map.of(
                        "query",
                        query,
                        "parametros",
                        parametros,
                        "esperadoAntes",
                        esperado,
                        "obtido",
                        obtido,
                        "igual",
                        true));
    }
}
