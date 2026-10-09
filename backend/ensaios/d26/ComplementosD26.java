import tools.jackson.databind.JsonNode;

import java.time.*;
import java.util.*;

/** Cobertura HTTP complementar; preserva todas as fixtures anteriores. */
final class ComplementosD26 {
    final EnsaioD26 x;
    final JornadasD26 j;

    ComplementosD26(EnsaioD26 x) {
        this.x = x;
        j = new JornadasD26(x);
    }

    Map<String, Object> m(Object... p) {
        return EnsaioD26.m(p);
    }

    JsonNode get(String p) throws Exception {
        return x.call("complemento GET " + p, "GET", p, null, "GESTOR", 200);
    }

    JsonNode post(String n, String p, Object b) throws Exception {
        return x.call(n, "POST", p, b, "GESTOR", 200);
    }

    void run() throws Exception {
        leituras();
        tratarZeroAnterior();
        cadastrosFiscalTransformacoes();
        excel();
        cargas();
        financeiroZero();
    }

    void runAfterReads() throws Exception {
        tratarZeroAnterior();
        cadastrosFiscalTransformacoes();
        excel();
        cargas();
        financeiroZero();
    }

    void runFromCadastros() throws Exception {
        cadastrosFiscalTransformacoes(false);
        excel();
        cargas();
        financeiroZero();
    }

    void runFromCargas() throws Exception {
        x.product = get("/api/v1/produtos/" + x.ids.get("produtoId"));
        x.pack = get("/api/v1/embalagens/" + x.ids.get("embalagemId"));
        cargas();
        financeiroZero();
    }

    void leituras() throws Exception {
        x.precheck("complementos-leituras", false);
        String ctx = "clienteId=" + x.clientId + "&armazemId=" + x.warehouseId;
        for (String p :
                List.of(
                        "/armazens?pagina=0&tamanho=2",
                        "/estoque?" + ctx + "&tamanho=2",
                        "/servicos-cobranca",
                        "/tabelas-cobranca?armazemId=" + x.warehouseId,
                        "/vinculos-tabela?" + ctx,
                        "/contratos-cobranca?" + ctx,
                        "/calculos-cobranca?" + ctx,
                        "/fatos-servico?" + ctx,
                        "/fechamentos-cobranca?" + ctx,
                        "/contagens?" + ctx,
                        "/contingencias?" + ctx,
                        "/cargas-iniciais?" + ctx,
                        "/ajustes-fechamento?" + ctx,
                        "/pedidos-entrada?" + ctx,
                        "/pedidos-saida?" + ctx,
                        "/indicadores-estoque?" + ctx + "&fuso=UTC&valor=true",
                        "/avisos-validade?" + ctx,
                        "/auditoria?tipo=CLIENTE&registroId=" + x.clientId)) get("/api/v1" + p);
        long pe = ((Number) x.ids.get("pedidoEntradaId")).longValue();
        for (String p : List.of("/unitizacao", "/unidades", "/chegadas"))
            get("/api/v1/pedidos-entrada/" + pe + p);
        for (var u : x.json.valueToTree(x.ids.get("unidades"))) {
            get("/api/v1/pedidos-entrada/" + pe + "/unidades/" + u.get("id").longValue());
            get("/api/v1/unidades-logisticas/" + u.get("codigo").asString() + "/movimentos");
            get("/api/v1/estoque/unidades/" + u.get("codigo").asString() + "/avarias");
        }
        for (String p : List.of("/expedicao", "/fatos"))
            get("/api/v1/pedidos-saida/" + x.ids.get("pedidoSaidaId") + p);
        for (String key : List.of("calculoAtual", "calculoHistorico")) {
            var c = x.json.valueToTree(x.ids.get(key));
            get("/api/v1/calculos-cobranca/" + c.get("id").longValue());
        }
        var close = x.json.valueToTree(x.ids.get("fechamento")).get("fechamento");
        long id = close.get("id").longValue();
        for (String p : List.of("", "/versoes", "/versoes/1", "/tratativas-externas"))
            get("/api/v1/fechamentos-cobranca/" + id + p);
        var f = x.json.valueToTree(x.ids.get("fatoServico"));
        get("/api/v1/fatos-servico/" + f.get("id").longValue());
        get(
                "/api/v1/fatos-servico/sugestoes?"
                        + ctx
                        + "&servicoId="
                        + f.get("servicoId").longValue());
        get(
                "/api/v1/tabelas-cobranca/"
                        + x.json.valueToTree(x.ids.get("tabela")).get("id").longValue());
        get("/api/v1/avarias/" + x.ids.get("avariaId") + "/marcos-financeiros");
        var c = x.json.valueToTree(x.ids.get("contagem"));
        for (String p : List.of("", "/revisoes"))
            get("/api/v1/contagens/" + c.get("id").longValue() + p);
        var l = x.json.valueToTree(x.ids.get("contingencia"));
        get("/api/v1/contingencias/" + l.get("id").longValue());
        get("/api/v1/clientes/" + x.clientId + "/complemento-fiscal");
        get("/api/v1/armazens/" + x.warehouseId + "/complemento-fiscal");
        get("/api/v1/produtos/" + x.ids.get("produtoId") + "/referencias-fiscais");
    }

    void cadastrosFiscalTransformacoes() throws Exception {
        cadastrosFiscalTransformacoes(true);
    }

    void cadastrosFiscalTransformacoes(boolean fresh) throws Exception {
        // Nova família HTTP isolada para transições mutuamente exclusivas.
        if (fresh) {
            x.ids.put("familiaAnterior", new LinkedHashMap<>(x.ids));
            j.cadastros();
        }
        for (var item :
                List.of(
                        m("rota", "armazens", "id", x.warehouseId, "campo", "nome"),
                        m("rota", "produtos", "id", x.ids.get("produtoId"), "campo", "descricao"),
                        m(
                                "rota",
                                "embalagens",
                                "id",
                                x.ids.get("embalagemId"),
                                "campo",
                                "descricao"))) {
            String route = "/api/v1/" + item.get("rota") + "/" + item.get("id");
            var before = get(route);
            var d = x.version(before);
            d.remove("operacaoId");
            d.put((String) item.get("campo"), x.round + " alteracao ficticia");
            x.call("alteracao cadastro " + item.get("rota"), "PUT", route, d, "GESTOR", 200);
        }
        for (String route :
                List.of("/api/v1/clientes/" + x.clientId, "/api/v1/armazens/" + x.warehouseId)) {
            var before = get(route);
            var d = x.version(before);
            var fiscal =
                    m(
                            "razaoSocial",
                            x.round + " Ficticio",
                            "cidade",
                            "Osasco",
                            "uf",
                            "SP",
                            "pais",
                            "BR",
                            "contatoEmail",
                            "d26@example.invalid",
                            "faturamentoEmail",
                            "d26@example.invalid",
                            "faturamentoReferencia",
                            x.round + " ficticia");
            if (route.contains("/armazens/")) {
                fiscal.remove("faturamentoEmail");
                fiscal.remove("faturamentoReferencia");
            }
            d.put("dados", fiscal);
            post("complemento fiscal ficticio", route + "/complemento-fiscal", d);
            get(route + "/complemento-fiscal");
        }
        x.product = get("/api/v1/produtos/" + x.ids.get("produtoId"));
        x.pack = get("/api/v1/embalagens/" + x.ids.get("embalagemId"));
        var d = x.version(x.product);
        long referenceVersion = 0;
        for (var reference :
                get("/api/v1/produtos/" + x.ids.get("produtoId") + "/referencias-fiscais")) {
            if (reference.get("armazemId").longValue() == x.warehouseId
                    && reference.get("operacao").asString().equals("D26-FICTICIA")) {
                referenceVersion = reference.get("versao").longValue();
            }
        }
        d.put("versao", referenceVersion);
        d.putAll(
                m(
                        "armazemId",
                        x.warehouseId,
                        "operacao",
                        "D26-FICTICIA",
                        "ncm",
                        "99999999",
                        "cfop",
                        "9999",
                        "enquadramento",
                        x.round + " SOMENTE FICTICIO",
                        "aliquotaIcms",
                        0,
                        "aliquotaIpi",
                        0,
                        "fonte",
                        x.round + " sem parametrizacao real",
                        "conferidaPor",
                        x.round + " Gestor ficticio",
                        "conferidaEm",
                        Instant.now().toString()));
        post(
                "referencia fiscal ficticia",
                "/api/v1/produtos/" + x.ids.get("produtoId") + "/referencias-fiscais",
                d);
        get("/api/v1/produtos/" + x.ids.get("produtoId") + "/referencias-fiscais");
        String ctx = "clienteId=" + x.clientId + "&armazemId=" + x.warehouseId;
        var cfg = get("/api/v1/avisos-validade?" + ctx);
        d = x.version(cfg);
        d.putAll(m("clienteId", x.clientId, "armazemId", x.warehouseId, "diasAntecedencia", 30));
        x.call("avisos validade configurados", "PUT", "/api/v1/avisos-validade", d, "GESTOR", 200);
        get("/api/v1/avisos-validade?" + ctx);
        long pe = j.entry("TRANSFORM", 50);
        j.arrival(pe, 50, Instant.now().minusSeconds(60));
        post(
                "liberar entrada transformacao",
                j.pe(pe) + "/efetivacao",
                m(
                        "versao",
                        j.pv(pe),
                        "aceitarDivergencias",
                        false,
                        "motivo",
                        x.round + " conferida"));
        long en = get(j.pe(pe) + "/entradas").get("itens").get(0).get("id").longValue();
        var u = j.unitize(pe, en, 50);
        String route = j.pe(pe) + "/unidades/" + u.get("id").longValue();
        d = x.version(u);
        d.put("quantidadeNovaUnidade", 10);
        x.call("divisao operador negada", "POST", route + "/divisao", d, "OPERACAO", 403);
        var split = x.call("divisao supervisor", "POST", route + "/divisao", d, "SUPERVISOR", 200);
        var again = post("divisao replay", route + "/divisao", d);
        x.assertion("divisao replay preserva origens", split.equals(again), split, again);
        var dest = split.get("unidades").get(0).get("unidade");
        var origin = split.get("unidades").get(1).get("unidade");
        d = x.command();
        d.putAll(
                m(
                        "versaoDestino",
                        dest.get("versao").longValue(),
                        "origens",
                        List.of(
                                m(
                                        "unidadeId",
                                        origin.get("id").longValue(),
                                        "versao",
                                        origin.get("versao").longValue()))));
        var regroup = post("reagrupamento rastreavel", route + "/reagrupamento", d);
        post("reagrupamento replay", route + "/reagrupamento", d);
        u = j.actual(dest);
        x.assertion(
                "transformacoes conservam50 e nota",
                u.get("quantidade").decimalValue().intValueExact() == 50
                        && u.get("notaId").equals(dest.get("notaId")),
                50,
                u);
        var a = j.address("PA", "ARMAZENAGEM");
        var b = j.address("PB", "ARMAZENAGEM");
        var limits =
                m(
                        "pesoKg",
                        1000,
                        "alturaMetros",
                        1,
                        "larguraMetros",
                        2,
                        "profundidadeMetros",
                        1,
                        "empilhamentoMaximo",
                        1);
        var con =
                x.call(
                        "conjunto duas posicoes",
                        "POST",
                        "/api/v1/conjuntos-posicoes",
                        m(
                                "armazemId",
                                x.warehouseId,
                                "codigo",
                                x.round + "PAR",
                                "enderecoAId",
                                a.get("id").longValue(),
                                "enderecoBId",
                                b.get("id").longValue(),
                                "limites",
                                limits,
                                "motivo",
                                x.round + " capacidade ficticia"),
                        "GESTOR",
                        201);
        get("/api/v1/conjuntos-posicoes?armazemId=" + x.warehouseId);
        d = j.move(u, a);
        d.put("conjuntoId", con.get("id").longValue());
        d.put(
                "destinos",
                List.of(
                        m(
                                "enderecoId",
                                a.get("id").longValue(),
                                "codigoLido",
                                a.get("codigo").asString()),
                        m(
                                "enderecoId",
                                b.get("id").longValue(),
                                "codigoLido",
                                b.get("codigo").asString())));
        ((Map<String, Object>) d.get("medidas")).put("posicoesNecessarias", 2);
        post("movimento conjunto ocupa duas", j.unit(u) + "/movimentos", d);
        u = j.actual(u);
        x.assertion(
                "ocupacao duas posicoes",
                get(j.unit(u) + "/estoque").get("posicoes").size() == 2,
                2,
                get(j.unit(u) + "/estoque"));
        d = x.command();
        d.put("versaoUnidade", u.get("versao").longValue());
        x.call(
                "bloqueio estoque supervisor",
                "POST",
                j.unit(u) + "/bloqueio",
                d,
                "SUPERVISOR",
                200);
        x.assertion(
                "bloqueio impede disponivel",
                j.balance().get("disponivel").decimalValue().signum() == 0,
                0,
                j.balance());
        u = j.actual(u);
        d = x.command();
        d.put("versaoUnidade", u.get("versao").longValue());
        x.call(
                "liberacao estoque supervisor",
                "POST",
                j.unit(u) + "/liberacao",
                d,
                "SUPERVISOR",
                200);
        u = j.actual(u);
        x.assertion(
                "liberacao restaura50",
                j.balance().get("disponivel").decimalValue().intValueExact() == 50,
                50,
                j.balance());
        finalizarTransformacao(pe, u, origin, con, a);
    }

    void retomarMovimento() throws Exception {
        x.precheck("complementos-retomada-movimento", false);
        var previous =
                x.json.readTree(
                        java.nio.file.Files.readString(
                                x.evidence.resolve(
                                        "d26-" + x.ids.get("baseRodada") + "-http.json")));
        JsonNode regroup = null, con = null, a = null;
        for (var c : previous.get("cases")) {
            if (c.get("caso").asString().equals("reagrupamento rastreavel"))
                regroup = c.get("resposta");
            if (c.get("caso").asString().equals("conjunto duas posicoes")) con = c.get("resposta");
            if (c.get("caso").asString().equals("endereco ARMAZENAGEM") && a == null)
                a = c.get("resposta");
        }
        if (regroup == null || con == null || a == null)
            throw new IllegalStateException("D26_RETOMADA_TRANSFORMACAO_INCOMPLETA");
        x.pack = get("/api/v1/embalagens/" + x.ids.get("embalagemId"));
        finalizarTransformacao(
                regroup.get("pedidoId").longValue(),
                j.actual(regroup.get("unidades").get(0).get("unidade")),
                regroup.get("unidades").get(1).get("unidade"),
                con,
                get("/api/v1/enderecos/" + a.get("id").longValue()));
        excel();
        cargas();
        financeiroZero();
    }

    void finalizarTransformacao(long pe, JsonNode u, JsonNode origin, JsonNode con, JsonNode a)
            throws Exception {
        var moveTarget = j.address("PC2", "ARMAZENAGEM");
        var other = j.address("PD2", "ARMAZENAGEM");
        var newCon =
                x.call(
                        "conjunto remanejamento mesmas medidas",
                        "POST",
                        "/api/v1/conjuntos-posicoes",
                        m(
                                "armazemId",
                                x.warehouseId,
                                "codigo",
                                x.round + "PAR2",
                                "enderecoAId",
                                moveTarget.get("id").longValue(),
                                "enderecoBId",
                                other.get("id").longValue(),
                                "limites",
                                m(
                                        "pesoKg",
                                        1000,
                                        "alturaMetros",
                                        1,
                                        "larguraMetros",
                                        2,
                                        "profundidadeMetros",
                                        1,
                                        "empilhamentoMaximo",
                                        1),
                                "motivo",
                                x.round + " remanejamento ficticio"),
                        "GESTOR",
                        201);
        var d = j.move(u, moveTarget);
        d.remove("medidas");
        d.put("conjuntoId", newCon.get("id").longValue());
        d.put(
                "destinos",
                List.of(
                        m(
                                "enderecoId",
                                moveTarget.get("id").longValue(),
                                "codigoLido",
                                moveTarget.get("codigo").asString()),
                        m(
                                "enderecoId",
                                other.get("id").longValue(),
                                "codigoLido",
                                other.get("codigo").asString())));
        post("remanejar conserva medidas duas posicoes", j.unit(u) + "/movimentos", d);
        post(
                "encerrar conjunto desocupado",
                "/api/v1/conjuntos-posicoes/" + con.get("id").longValue() + "/encerramento",
                revisaoCadastro(con));
        d = revisaoCadastro(a);
        d.put("descricao", x.round + " posicao atualizada");
        a =
                x.call(
                        "atualizar endereco",
                        "PUT",
                        "/api/v1/enderecos/" + a.get("id").longValue(),
                        d,
                        "GESTOR",
                        200);
        ciclo("enderecos", a);
        ciclo("embalagens", x.pack);
        ciclo("produtos", get("/api/v1/produtos/" + x.ids.get("produtoId")));
        ciclo("armazens", get("/api/v1/armazens/" + x.warehouseId));
        ciclo("clientes", get("/api/v1/clientes/" + x.clientId));
        get("/api/v1/encerramentos/CLIENTE/" + x.clientId + "/impedimentos");
        x.ids.put(
                "transformacao",
                m(
                        "pedidoEntradaId",
                        pe,
                        "unidadeId",
                        u.get("id").longValue(),
                        "codigo",
                        u.get("codigo").asString(),
                        "origemReagrupadaId",
                        origin.get("id").longValue(),
                        "conjuntoId",
                        con.get("id").longValue()));
        x.save();
    }

    Map<String, Object> revisaoCadastro(JsonNode e) {
        var d = x.version(e);
        d.remove("operacaoId");
        return d;
    }

    void ciclo(String plural, JsonNode e) throws Exception {
        String r = "/api/v1/" + plural + "/" + e.get("id").longValue();
        e = post("encerramento reversivel " + plural, r + "/encerramento", revisaoCadastro(e));
        e = post("reativacao " + plural, r + "/reativacao", revisaoCadastro(e));
        x.assertion(
                "reativado " + plural,
                e.get("situacao").asString().equals("ATIVO"),
                "ATIVO",
                e.get("situacao"));
    }

    void excel() throws Exception {
        x.precheck("complementos-excel", false);
        byte[] sheet;
        try (var workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook();
                var bytes = new java.io.ByteArrayOutputStream()) {
            var s = workbook.createSheet("Enderecos");
            var header = s.createRow(0);
            String[] names = {
                "codigo", "rua", "nivel", "posicao", "descricao", "tipo", "sequenciaColeta"
            };
            for (int i = 0; i < names.length; i++) header.createCell(i).setCellValue(names[i]);
            var row = s.createRow(1);
            String[] values = {
                x.round + "XLS",
                x.round + "R",
                "0",
                "XLS",
                x.round + " XLSX ficticio",
                "ARMAZENAGEM",
                "500"
            };
            for (int i = 0; i < values.length; i++) row.createCell(i).setCellValue(values[i]);
            workbook.write(bytes);
            sheet = bytes.toByteArray();
        }
        String boundary = "D26" + UUID.randomUUID().toString().replace("-", "");
        var bytes = new java.io.ByteArrayOutputStream();
        bytes.write(
                ("--"
                                + boundary
                                + "\r\n"
                                + "Content-Disposition: form-data; name=\"comando\"\r\n"
                                + "Content-Type: application/json\r\n\r\n"
                                + x.json.writeValueAsString(x.command())
                                + "\r\n--"
                                + boundary
                                + "\r\n"
                                + "Content-Disposition: form-data; name=\"arquivo\"; filename=\""
                                + x.round
                                + ".xlsx\"\r\n"
                                + "Content-Type:"
                                + " application/vnd.openxmlformats-officedocument.spreadsheetml.sheet\r\n\r\n")
                        .getBytes(java.nio.charset.StandardCharsets.UTF_8));
        bytes.write(sheet);
        bytes.write(
                ("\r\n--" + boundary + "--\r\n").getBytes(java.nio.charset.StandardCharsets.UTF_8));
        String route = "/api/v1/armazens/" + x.warehouseId + "/importacoes-enderecos/previa";
        var req =
                java.net.http.HttpRequest.newBuilder(java.net.URI.create(x.base + route))
                        .timeout(Duration.ofSeconds(40))
                        .header("Authorization", "Bearer " + x.token("GESTOR", Map.of()))
                        .header("X-Request-Id", x.round + "-" + UUID.randomUUID())
                        .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                        .POST(
                                java.net.http.HttpRequest.BodyPublishers.ofByteArray(
                                        bytes.toByteArray()))
                        .build();
        var response = x.http.send(req, java.net.http.HttpResponse.BodyHandlers.ofString());
        var preview =
                x.recordResponse(
                        "XLSX ficticio previa",
                        "POST",
                        route,
                        m("arquivo", x.round + ".xlsx", "linhas", 1, "layout", 1),
                        "GESTOR",
                        200,
                        response,
                        false);
        x.assertion(
                "XLSX previa sem erros", preview.get("erros").isEmpty(), 0, preview.get("erros"));
        get("/api/v1/importacoes-enderecos/" + preview.get("id").longValue());
        var d = x.version(preview);
        d.put("arquivoHash", preview.get("arquivoHash").asString());
        var confirm =
                post(
                        "XLSX confirmacao atomica",
                        "/api/v1/importacoes-enderecos/"
                                + preview.get("id").longValue()
                                + "/confirmacao",
                        d);
        post(
                "XLSX replay confirmacao",
                "/api/v1/importacoes-enderecos/" + preview.get("id").longValue() + "/confirmacao",
                d);
        x.assertion(
                "XLSX um endereco",
                confirm.get("enderecos").size() == 1,
                1,
                confirm.get("enderecos"));
        x.ids.put("importacaoEndereco", confirm);
        x.save();
    }

    void cargas() throws Exception {
        x.precheck("complementos-carga", false);
        var before = j.balance();
        var load = criarCarga("LOAD", 20);
        String route = "/api/v1/cargas-iniciais/" + load.get("id").longValue();
        get(route);
        get(route + "/revisoes");
        var d = x.version(load);
        d.put("dados", load.get("revisao").get("dados"));
        load = post("carga revisao preservada", route + "/revisoes", d);
        d = confirmacaoCarga(load, List.of());
        load = post("carga preparar fisico", route + "/preparar", d);
        x.ids.put("cargaPreparada", load);
        x.save();
        d = confirmacaoCarga(load, x.json.convertValue(load.get("etiquetas"), List.class));
        var regular = post("carga confirmar todas etiquetas", route + "/confirmar", d);
        post("carga replay confirmacao", route + "/confirmar", d);
        x.assertion(
                "carga regularizada uma vez",
                regular.get("situacao").asString().equals("REGULARIZADA")
                        && j.balance()
                                        .get("fisicoTotal")
                                        .decimalValue()
                                        .subtract(before.get("fisicoTotal").decimalValue())
                                        .intValueExact()
                                == 20,
                "REGULARIZADA/fisico+20",
                j.balance());
        x.ids.put("cargaRegularizada", regular);
        var pending = criarCarga("CANCEL", 3);
        post(
                "carga pendente cancelada",
                "/api/v1/cargas-iniciais/" + pending.get("id").longValue() + "/cancelar",
                x.version(pending));
        get(
                "/api/v1/cargas-iniciais?clienteId="
                        + x.clientId
                        + "&armazemId="
                        + x.warehouseId
                        + "&situacao=REGULARIZADA&tamanho=1");
        x.save();
    }

    JsonNode criarCarga(String suffix, int qty) throws Exception {
        String when = Instant.now().minusSeconds(60).toString();
        var nota =
                m(
                        "versao",
                        0,
                        "serie",
                        1,
                        "numero",
                        ++j.number,
                        "emissao",
                        LocalDate.now(ZoneOffset.UTC).minusDays(1).toString(),
                        "itens",
                        List.of(
                                m(
                                        "numeroItem",
                                        1,
                                        "produtoId",
                                        x.ids.get("produtoId"),
                                        "quantidadePrevista",
                                        qty,
                                        "valorMercadoria",
                                        qty * 10)));
        var data =
                m(
                        "referenciaPedido",
                        x.round + suffix,
                        "nota",
                        nota,
                        "chegadaReal",
                        when,
                        "dataFifo",
                        when,
                        "quantidadeBoa",
                        qty,
                        "quantidadeAvariada",
                        0,
                        "unidades",
                        List.of(
                                m(
                                        "embalagemId",
                                        x.ids.get("embalagemId"),
                                        "tipo",
                                        "PALLET",
                                        "condicao",
                                        "BOA",
                                        "quantidade",
                                        qty)),
                        "fonte",
                        x.round + " origem documental/fisica FICTICIA");
        var d = x.command();
        d.putAll(
                m(
                        "clienteId",
                        x.clientId,
                        "armazemId",
                        x.warehouseId,
                        "produtoId",
                        x.ids.get("produtoId"),
                        "referencia",
                        x.round + suffix,
                        "etiquetaFornecida",
                        x.round + suffix,
                        "quantidade",
                        qty,
                        "dados",
                        data));
        return post("criar carga " + suffix, "/api/v1/cargas-iniciais", d);
    }

    Map<String, Object> confirmacaoCarga(JsonNode load, List labels) {
        var d = x.version(load);
        d.putAll(
                m(
                        "revisao",
                        load.get("revisao").get("numero").intValue(),
                        "conteudoHash",
                        load.get("revisao").get("conteudoHash").asString(),
                        "leitura",
                        load.get("etiquetaFornecida").asString(),
                        "etiquetasUnidades",
                        labels));
        return d;
    }

    String fc(JsonNode f) {
        return "/api/v1/fechamentos-cobranca/" + f.get("fechamento").get("id").longValue();
    }

    JsonNode atualFechamento(JsonNode previous) throws Exception {
        var head = get(fc(previous));
        return x.json.valueToTree(
                m(
                        "fechamento",
                        head,
                        "versao",
                        get(fc(previous) + "/versoes/" + head.get("versaoAtual").intValue())));
    }

    Map<String, Object> decisao(JsonNode f) {
        var d = x.version(f.get("fechamento"));
        d.put("numero", f.get("versao").get("numero").intValue());
        return d;
    }

    void tratarZeroAnterior() throws Exception {
        x.precheck("complementos-tratativa-zero", false);
        var old = x.json.valueToTree(x.ids.get("fechamento"));
        var f = atualFechamento(old);
        var doc = f.get("versao").get("nfse").get(0);
        var d = x.version(f.get("fechamento"));
        d.putAll(
                m(
                        "numeroResultado",
                        f.get("versao").get("numero").intValue(),
                        "resultado",
                        "NAO_EMITIDO_CONFIRMADO",
                        "referencias",
                        List.of(
                                m(
                                        "referenciaId",
                                        doc.get("id").longValue(),
                                        "situacao",
                                        "CANCELAMENTO_COMPROVADO")),
                        "fonte",
                        x.round + " CANCELAMENTO SOMENTE DOCUMENTO FICTICIO DO ENSAIO",
                        "conferidaPor",
                        x.round + " Gestor ficticio",
                        "conferidaEm",
                        Instant.now().toString()));
        f = post("tratativa documento ficticio saldo zero", fc(f) + "/tratativas-externas", d);
        post("tratativa replay", fc(f) + "/tratativas-externas", d);
        f = resolverZero(f);
        x.assertion(
                "zero finalizado sem emissao",
                f.get("fechamento").get("situacao").asString().equals("FINALIZADO_SEM_EMISSAO"),
                "FINALIZADO_SEM_EMISSAO",
                f.get("fechamento"));
        x.ids.put("fechamento", f);
        x.save();
        var out = get("/api/v1/pedidos-saida/" + x.ids.get("pedidoSaidaId"));
        var baixa = x.json.valueToTree(x.ids.get("baixas")).get(0);
        d = x.version(out);
        d.putAll(
                m(
                        "referencia",
                        x.round + "DEV-FICTICIA",
                        "nota",
                        m(
                                "emitenteCnpj",
                                x.json.valueToTree(x.ids.get("clienteDocumentoFiscal")).asString(),
                                "serie",
                                "1",
                                "numero",
                                Integer.toString(++j.number),
                                "emissao",
                                LocalDate.now(ZoneOffset.UTC).toString()),
                        "chegadaReal",
                        Instant.now().minusMillis(5).toString(),
                        "itens",
                        List.of(
                                m(
                                        "baixaId",
                                        baixa.get("id").longValue(),
                                        "quantidade",
                                        5,
                                        "quantidadeAvariada",
                                        0))));
        var returned =
                post(
                        "devolucao externa ficticia rastreada",
                        "/api/v1/pedidos-saida/" + out.get("id").longValue() + "/devolucoes",
                        d);
        post(
                "devolucao replay",
                "/api/v1/pedidos-saida/" + out.get("id").longValue() + "/devolucoes",
                d);
        x.ids.put("devolucaoExterna", returned);
        x.save();
    }

    JsonNode resolverZero(JsonNode f) throws Exception {
        var d = decisao(f);
        d.putAll(
                m(
                        "referenciaExterna",
                        x.round + "ZERO-FICTICIO",
                        "fonte",
                        x.round + " saldo zero ensaio sem emissao/cobranca",
                        "confirmadaPor",
                        x.round + " Gestor ficticio",
                        "confirmadaEm",
                        Instant.now().toString()));
        return post("resolucao financeira zero", fc(f) + "/resolucao-financeira", d);
    }

    JsonNode calcular(LocalDate ini, LocalDate fim) throws Exception {
        var d = x.command();
        d.putAll(
                m(
                        "clienteId",
                        x.clientId,
                        "armazemId",
                        x.warehouseId,
                        "periodoInicio",
                        ini.toString(),
                        "periodoFim",
                        fim.toString()));
        return post("calculo complementar " + ini, "/api/v1/calculos-cobranca", d);
    }

    JsonNode preparar(JsonNode calc) throws Exception {
        var d = x.command();
        d.put("calculoId", calc.get("id").longValue());
        return post("preparar fechamento zero", "/api/v1/fechamentos-cobranca", d);
    }

    void financeiroZero() throws Exception {
        x.precheck("complementos-financeiro", false);
        LocalDate today = LocalDate.now(ZoneOffset.UTC), start = today.minusDays(10);
        var d = x.command();
        d.putAll(
                m(
                        "codigo",
                        x.round + "ADIC",
                        "descricao",
                        x.round + " adicional ficticio",
                        "tipo",
                        "ADICIONAL",
                        "unidade",
                        "VEICULO"));
        var s = post("servico adicional ficticio", "/api/v1/servicos-cobranca", d);
        d = x.command();
        d.putAll(
                m(
                        "codigo",
                        x.round + "ARMZERO",
                        "descricao",
                        x.round + " armazenagem FICTICIA tarifa zero",
                        "tipo",
                        "ARMAZENAGEM",
                        "unidade",
                        "POSICAO_DIA"));
        var storage = post("servico armazenagem zero ficticio", "/api/v1/servicos-cobranca", d);
        d = x.command();
        d.putAll(
                m(
                        "armazemId",
                        x.warehouseId,
                        "codigo",
                        x.round + "TAR",
                        "descricao",
                        x.round + " tabela ficticia",
                        "vigenciaInicio",
                        start.toString(),
                        "tipo",
                        "PADRAO",
                        "itens",
                        List.of(
                                m(
                                        "servicoId",
                                        s.get("id").longValue(),
                                        "categoria",
                                        "",
                                        "preco",
                                        5),
                                m(
                                        "servicoId",
                                        storage.get("id").longValue(),
                                        "categoria",
                                        "",
                                        "preco",
                                        0))));
        var t = post("tarifa adicional ficticia", "/api/v1/tabelas-cobranca", d);
        d = x.command();
        d.putAll(
                m(
                        "clienteId",
                        x.clientId,
                        "armazemId",
                        x.warehouseId,
                        "tabelaId",
                        t.get("id").longValue(),
                        "vigenciaInicio",
                        start.toString()));
        var v = post("vinculo complementar", "/api/v1/vinculos-tabela", d);
        d = x.command();
        d.putAll(
                m(
                        "clienteId",
                        x.clientId,
                        "armazemId",
                        x.warehouseId,
                        "vigenciaInicio",
                        start.toString(),
                        "fuso",
                        "UTC",
                        "moeda",
                        "BRL",
                        "modalidadeCiclo",
                        "DIAS_CORRIDOS",
                        "duracaoDias",
                        1,
                        "minimoModo",
                        "NAO_APLICAVEL",
                        "grisModo",
                        "NAO_APLICAVEL",
                        "servicosMinimo",
                        List.of()));
        var contract = post("contrato complementar ficticio", "/api/v1/contratos-cobranca", d);
        long pe =
                x.json.valueToTree(
                                x.ids.get("entradaFinanceiro") == null
                                        ? x.ids.get("transformacao")
                                        : x.ids.get("entradaFinanceiro"))
                        .get("pedidoEntradaId")
                        .longValue();
        long nota = get(j.pe(pe)).get("notas").get(0).get("id").longValue();
        d = x.command();
        d.putAll(
                m(
                        "clienteId",
                        x.clientId,
                        "armazemId",
                        x.warehouseId,
                        "servicoId",
                        s.get("id").longValue(),
                        "origem",
                        "MANUAL",
                        "pedidoEntradaId",
                        pe,
                        "referenciaExecucao",
                        x.round + "ADIC-EXEC",
                        "executadoEm",
                        Instant.now().minusMillis(5).toString(),
                        "quantidade",
                        4,
                        "categoria",
                        "",
                        "cotas",
                        List.of(m("notaId", nota, "cota", 1)),
                        "criterioRateio",
                        x.round + " nota ficticia exclusiva"));
        var fact = post("adicional manual ficticio", "/api/v1/fatos-servico", d);
        var forecast = calcular(today, today.plusDays(1));
        x.assertion(
                "adicional quatro por cinco total20",
                forecast.get("total").decimalValue().intValueExact() == 20,
                20,
                forecast.get("total"));
        post(
                "anular adicional antes fechamento",
                "/api/v1/fatos-servico/" + fact.get("id").longValue() + "/anulacao",
                x.version(fact));
        financeiroFechamentos(today, start, s, storage, t, v, contract, fact, forecast);
    }

    void financeiroFechamentos(
            LocalDate today,
            LocalDate start,
            JsonNode s,
            JsonNode storage,
            JsonNode t,
            JsonNode v,
            JsonNode contract,
            JsonNode fact,
            JsonNode forecast)
            throws Exception {
        var d = x.command();
        var source = preparar(calcular(start, start.plusDays(1)));
        var reject =
                post(
                        "rejeicao integral preserva snapshot",
                        fc(source) + "/rejeicao",
                        decisao(source));
        var corr = calcular(start, start.plusDays(1));
        d = decisao(reject);
        d.put("calculoId", corr.get("id").longValue());
        source = post("reabertura nova versao", fc(reject) + "/reabertura", d);
        x.assertion(
                "reabertura versao2",
                source.get("versao").get("numero").intValue() == 2,
                2,
                source.get("versao"));
        source = post("aprovar zero reaberto", fc(source) + "/aprovacao", decisao(source));
        d = decisao(source);
        d.putAll(
                m(
                        "fonte",
                        x.round + " nao emissao FICTICIA",
                        "confirmadaPor",
                        x.round + " Gestor ficticio",
                        "confirmadaEm",
                        Instant.now().toString()));
        source =
                post(
                        "confirmacao externa ficticia sem provider",
                        fc(source) + "/confirmacoes-externas",
                        d);
        source = resolverZero(source);
        x.assertion(
                "finalizar zero sem nota",
                source.get("fechamento")
                        .get("situacao")
                        .asString()
                        .equals("FINALIZADO_SEM_EMISSAO"),
                "FINALIZADO_SEM_EMISSAO",
                source.get("fechamento"));
        var dest = preparar(calcular(start.plusDays(1), start.plusDays(2)));
        d = x.command();
        d.putAll(
                m(
                        "origemVersaoId",
                        source.get("versao").get("id").longValue(),
                        "destinoFechamentoId",
                        dest.get("fechamento").get("id").longValue(),
                        "versaoDestino",
                        dest.get("fechamento").get("versao").longValue(),
                        "calculoCorrigidoId",
                        calcular(start, start.plusDays(1)).get("id").longValue(),
                        "evidencia",
                        x.round + " ajustes zero FICTICIO sem cobranca"));
        var adj = post("ajuste origem zero identificado", "/api/v1/ajustes-fechamento", d);
        post("ajuste replay", "/api/v1/ajustes-fechamento", d);
        get("/api/v1/ajustes-fechamento?clienteId=" + x.clientId + "&armazemId=" + x.warehouseId);
        x.ids.put(
                "financeiroComplemento",
                m(
                        "servico",
                        s,
                        "tabela",
                        t,
                        "vinculo",
                        v,
                        "contrato",
                        contract,
                        "fatoAnulado",
                        fact,
                        "previsao",
                        forecast,
                        "origem",
                        source,
                        "destino",
                        get(fc(dest)),
                        "ajuste",
                        adj));
        x.save();
        d = x.version(contract);
        d.put("vigenciaFim", today.plusDays(1).toString());
        var ended =
                post(
                        "encerrar contrato no limite futuro do calculo ficticio",
                        "/api/v1/contratos-cobranca/"
                                + contract.get("id").longValue()
                                + "/encerramento",
                        d);
        d = x.command();
        d.putAll(
                m(
                        "clienteId",
                        x.clientId,
                        "armazemId",
                        x.warehouseId,
                        "vigenciaInicio",
                        today.plusDays(1).toString(),
                        "fuso",
                        "UTC",
                        "moeda",
                        "BRL",
                        "modalidadeCiclo",
                        "DIAS_CORRIDOS",
                        "duracaoDias",
                        1,
                        "minimoModo",
                        "APLICAVEL",
                        "minimoValor",
                        50,
                        "minimoProporcao",
                        "INTEGRAL",
                        "servicosMinimo",
                        List.of(s.get("id").longValue(), storage.get("id").longValue()),
                        "grisModo",
                        "APLICAVEL",
                        "grisPercentual",
                        1,
                        "grisBase",
                        "VALOR_ESTOQUE_PICO",
                        "grisPeriodicidade",
                        "DIARIA",
                        "grisProporcao",
                        "INTEGRAL"));
        var minContract =
                post("minimo50 GRIS1 SOMENTE FICTICIOS previsao", "/api/v1/contratos-cobranca", d);
        var minForecast = calcular(today.plusDays(1), today.plusDays(2));
        x.assertion(
                "previsao ficticia minimo50 GRIS1 sobre100",
                minForecast.get("total").decimalValue().intValueExact() == 51
                        && minForecast.get("minimoCalculado").decimalValue().intValueExact() == 50
                        && minForecast.get("grisCalculado").decimalValue().intValueExact() == 1,
                "50+1=51 PREVISAO",
                minForecast);
        x.ids.put(
                "minimoGrisFicticios",
                m(
                        "contratoAnteriorEncerrado",
                        ended,
                        "contrato",
                        minContract,
                        "previsao",
                        minForecast));
        x.save();
    }

    void extrasBasicos() throws Exception {
        x.precheck("complementos-transicoes", false);
        x.product = get("/api/v1/produtos/" + x.ids.get("produtoId"));
        x.pack = get("/api/v1/embalagens/" + x.ids.get("embalagemId"));
        get("/api/v1/status");
        long pe = j.entry("ESTORNO", 1);
        j.arrival(pe, 1, Instant.now().minusSeconds(5));
        var chegada = get(j.pe(pe) + "/chegadas").get("itens").get(0);
        var d = m("versao", j.pv(pe), "motivo", x.round + " estorno ficticio sem estoque");
        String route = j.pe(pe) + "/chegadas/" + chegada.get("id").longValue() + "/estorno";
        x.call("estorno operador negado", "POST", route, d, "OPERACAO", 403);
        x.call("estorno supervisor", "POST", route, d, "SUPERVISOR", 200);
        get(j.pe(pe) + "/chegadas");
        d = m("versao", j.pv(pe), "motivo", x.round + " cancelar entrada sem efeito fisico");
        x.call(
                "cancelar entrada com historico recusado",
                "POST",
                j.pe(pe) + "/cancelamento",
                d,
                "SUPERVISOR",
                409);
        get(j.pe(pe));
        x.ids.put("entradaEstornadaCancelada", pe);
        x.save();

        extrasDepoisEstorno();
    }

    void extrasDepoisEstorno() throws Exception {
        x.precheck("complementos-transicoes-sem-repetir-estorno", false);
        x.product = get("/api/v1/produtos/" + x.ids.get("produtoId"));
        x.pack = get("/api/v1/embalagens/" + x.ids.get("embalagemId"));
        long draft =
                x.call(
                                "rascunho independente cancelavel",
                                "POST",
                                "/api/v1/pedidos-entrada",
                                m(
                                        "clienteId",
                                        x.clientId,
                                        "armazemId",
                                        x.warehouseId,
                                        "referencia",
                                        x.round + "CANCELDRAFT"),
                                "OPERACAO",
                                201)
                        .get("id")
                        .longValue();
        x.call(
                "cancelamento rascunho sem historico",
                "POST",
                j.pe(draft) + "/cancelamento",
                m("versao", 0, "motivo", x.round + " ficticio sem chegada"),
                "SUPERVISOR",
                200);
        get(j.pe(draft));
        var d = x.command();
        d.putAll(
                m(
                        "codigo",
                        x.round + "VAZIO",
                        "descricao",
                        x.round + " servico sem uso ficticio",
                        "tipo",
                        "ADICIONAL",
                        "unidade",
                        "VEICULO"));
        var svc = post("servico vazio para encerramento", "/api/v1/servicos-cobranca", d);
        String enc = "/api/v1/encerramentos/SERVICO_COBRANCA/" + svc.get("id").longValue();
        var solicitado =
                post("solicitar encerramento sem compromissos", enc + "/solicitar", x.version(svc));
        d = x.version(solicitado);
        x.call("inativacao supervisor negada", "POST", enc + "/inativar", d, "SUPERVISOR", 403);
        var inactive = post("inativar cadastro sem compromissos", enc + "/inativar", d);
        post("inativar replay", enc + "/inativar", d);
        x.assertion(
                "cadastro inativo sem apagar",
                inactive.get("situacao").asString().equals("INATIVO"),
                "INATIVO",
                get(enc + "/impedimentos"));
        x.ids.put("servicoInativo", inactive);
        x.save();

        long pe = j.entry("FIFOEXTRA", 10);
        j.arrival(pe, 10, Instant.now().minusSeconds(5));
        post(
                "liberar entrada extra",
                j.pe(pe) + "/efetivacao",
                m(
                        "versao",
                        j.pv(pe),
                        "aceitarDivergencias",
                        false,
                        "motivo",
                        x.round + " conferida"));
        var u =
                j.unitize(
                        pe,
                        get(j.pe(pe) + "/entradas").get("itens").get(0).get("id").longValue(),
                        10);
        var arm = j.address("EXARM", "ARMAZENAGEM");
        post("enderecar unidade extra", j.unit(u) + "/movimentos", j.move(u, arm));
        u = j.actual(u);
        var p = j.order("FIFOJUST", 5);
        d = x.version(p);
        d.put("selecoes", List.of(m("unidadeId", u.get("id").longValue(), "quantidade", 5)));
        var just =
                x.call(
                        "justificativa FIFO selecao integral",
                        "POST",
                        j.ps(p) + "/justificativas-fifo",
                        d,
                        "SUPERVISOR",
                        200);
        post("justificativa FIFO replay", j.ps(p) + "/justificativas-fifo", d);
        x.assertion(
                "selecao nova excepciona estoque anterior",
                just.get("excecaoFifo").booleanValue(),
                true,
                just.get("excecaoFifo"));
        p = just.get("pedido");
        d = x.version(p);
        d.put("justificativaId", just.get("operacaoId").asString());
        x.call(
                "reserva excepcional operador negada",
                "POST",
                j.ps(p) + "/reserva",
                d,
                "OPERACAO",
                403);
        p =
                x.call(
                                "reserva excepcional supervisor",
                                "POST",
                                j.ps(p) + "/reserva",
                                d,
                                "SUPERVISOR",
                                200)
                        .get("pedido");
        x.call(
                "revalidacao reserva atual",
                "POST",
                j.ps(p) + "/revalidacao",
                null,
                "SUPERVISOR",
                200);
        var sep = j.address("EXSEP", "SEPARACAO");
        p = j.separate(p, u, sep);
        String xml = j.xml(5, "8");
        d = x.version(p);
        d.putAll(
                m(
                        "origem",
                        "XML",
                        "natureza",
                        "RETORNO_MERCADORIA",
                        "xml",
                        xml,
                        "protocolo",
                        x.round + " FICTICIO sem emissor real",
                        "coberturas",
                        List.of(
                                m(
                                        "reservaId",
                                        j.reserve(p, u),
                                        "notaOrigemId",
                                        u.get("notaId").longValue(),
                                        "sku",
                                        x.product.get("sku").asString(),
                                        "quantidade",
                                        5))));
        var registered =
                x.call(
                        "documento cancelavel ficticio",
                        "POST",
                        j.ps(p) + "/documentos",
                        d,
                        "SUPERVISOR",
                        200);
        p = registered.get("expedicao").get("pedido");
        long documento = registered.get("expedicao").get("documentos").get(0).get("id").longValue();
        d = x.version(p);
        var cancel =
                x.call(
                        "cancelamento documento antes baixa",
                        "POST",
                        j.ps(p) + "/documentos/" + documento + "/cancelamento",
                        d,
                        "SUPERVISOR",
                        200);
        x.call(
                "cancelamento documento replay",
                "POST",
                j.ps(p) + "/documentos/" + documento + "/cancelamento",
                d,
                "SUPERVISOR",
                200);
        p = cancel.get("expedicao").get("pedido");
        d = x.version(p);
        d.put(
                "unidades",
                List.of(m("reservaId", j.reserve(p, u), "destinos", j.destinations(arm))));
        var returned =
                x.call(
                        "retorno apos cancelamento documental",
                        "POST",
                        j.ps(p) + "/retorno-interno",
                        d,
                        "SUPERVISOR",
                        200);
        x.assertion(
                "retorno sem baixa cancela reserva",
                returned.get("expedicao")
                        .get("pedido")
                        .get("situacao")
                        .asString()
                        .equals("CANCELADO"),
                "CANCELADO",
                returned);
        u = j.actual(u);
        d = x.command();
        d.put("versaoUnidade", u.get("versao").longValue());
        var legacy =
                x.call(
                        "marcador avaria legado preservado",
                        "POST",
                        j.unit(u) + "/avaria",
                        d,
                        "OPERACAO",
                        200);
        x.assertion(
                "marcador avaria bloqueia unidade",
                legacy.get("estoque").get("avariaPosterior").booleanValue(),
                true,
                legacy.get("estoque"));
        x.ids.put(
                "extraFisico",
                m(
                        "pedidoEntradaId",
                        pe,
                        "unidade",
                        u,
                        "pedidoCancelado",
                        returned,
                        "documentoCancelado",
                        documento,
                        "avariaLegada",
                        legacy));
        x.save();
    }

    void retomarRetornoExtra() throws Exception {
        x.precheck("complementos-retorno-documento-cancelado", false);
        var previous =
                x.json.readTree(
                        java.nio.file.Files.readString(
                                x.evidence.resolve(
                                        "d26-" + x.ids.get("baseRodada") + "-http.json")));
        JsonNode p = null, arm = null;
        for (var c : previous.get("cases")) {
            if (c.get("caso").asString().equals("cancelamento documento antes baixa"))
                p = c.get("resposta").get("expedicao").get("pedido");
            if (c.get("caso").asString().equals("endereco ARMAZENAGEM")) arm = c.get("resposta");
        }
        if (p == null || arm == null)
            throw new IllegalStateException("D26_RETOMADA_RETORNO_INCOMPLETA");
        var u =
                get("/api/v1/unidades-logisticas/"
                                + p.get("reservas").get(0).get("codigoUnidade").asString())
                        .get("unidade");
        var d = x.version(p);
        d.put(
                "unidades",
                List.of(m("reservaId", j.reserve(p, u), "destinos", j.destinations(arm))));
        var returned =
                x.call(
                        "retorno apos cancelamento documental",
                        "POST",
                        j.ps(p) + "/retorno-interno",
                        d,
                        "SUPERVISOR",
                        200);
        u = j.actual(u);
        d = x.command();
        d.put("versaoUnidade", u.get("versao").longValue());
        var legacy =
                x.call(
                        "marcador avaria legado preservado",
                        "POST",
                        j.unit(u) + "/avaria",
                        d,
                        "OPERACAO",
                        200);
        x.assertion(
                "marcador bloqueia unidade",
                legacy.get("estoque").get("avariaPosterior").booleanValue(),
                true,
                legacy.get("estoque"));
        x.ids.put(
                "extraFisico",
                m("unidade", u, "pedidoCancelado", returned, "avariaLegada", legacy));
        x.save();
    }

    void retomarFinanceiro() throws Exception {
        x.precheck("complementos-repro-calculo-real", false);
        var previous =
                x.json.readTree(
                        java.nio.file.Files.readString(
                                x.evidence.resolve("d26-D26EB3EA406-http.json")));
        var d =
                x.json.convertValue(
                        previous.get("cases").get(previous.get("cases").size() - 1).get("payload"),
                        Map.class);
        var pending =
                post(
                        "calculo transformado conserva pendencias sem500",
                        "/api/v1/calculos-cobranca",
                        d);
        x.assertion(
                "calculo ambiguo nao aprova total",
                pending.get("situacao").asString().equals("PENDENTE")
                        && pending.get("total").isNull()
                        && pending.get("pendencias")
                                .toString()
                                .contains("HISTORICO_QUANTIDADE_INSUFICIENTE"),
                "PENDENTE/totalNULL/historico",
                pending);
        post("calculo pendente replay", "/api/v1/calculos-cobranca", d);
        x.ids.put("calculoPendenteTransformacao", pending);
        x.save();
        x.ids.put("familiaAnterior", new LinkedHashMap<>(x.ids));
        j.cadastros();
        long pe = j.entry("FINSEMTRANSFORM", 10);
        j.arrival(pe, 10, Instant.now().minusSeconds(5));
        post(
                "liberar entrada financeiro sem transformacao",
                j.pe(pe) + "/efetivacao",
                m(
                        "versao",
                        j.pv(pe),
                        "aceitarDivergencias",
                        false,
                        "motivo",
                        x.round + " conferida"));
        var u =
                j.unitize(
                        pe,
                        get(j.pe(pe) + "/entradas").get("itens").get(0).get("id").longValue(),
                        10);
        var arm = j.address("FINARM", "ARMAZENAGEM");
        post("enderecar financeiro sem transformacao", j.unit(u) + "/movimentos", j.move(u, arm));
        x.ids.put("entradaFinanceiro", m("pedidoEntradaId", pe, "unidade", j.actual(u)));
        x.save();
        financeiroZero();
    }

    JsonNode respostaAnterior(JsonNode previous, String name) {
        for (var c : previous.get("cases"))
            if ((c.get("caso").asString().equals(name)
                            || (name.equals("calculo complementar")
                                    && c.get("caso").asString().startsWith(name + " ")))
                    && c.get("actual").intValue() == 200) return c.get("resposta");
        throw new IllegalStateException("D26_RESPOSTA_BASE_AUSENTE_" + name);
    }

    void retomarFechamento() throws Exception {
        x.precheck("complementos-fechamento-corte-canonico", false);
        var previous =
                x.json.readTree(
                        java.nio.file.Files.readString(
                                x.evidence.resolve(
                                        "d26-" + x.ids.get("baseRodada") + "-http.json")));
        var contract = respostaAnterior(previous, "contrato complementar ficticio");
        financeiroFechamentos(
                LocalDate.now(ZoneOffset.UTC),
                LocalDate.parse(contract.get("vigenciaInicio").asString()),
                respostaAnterior(previous, "servico adicional ficticio"),
                respostaAnterior(previous, "servico armazenagem zero ficticio"),
                respostaAnterior(previous, "tarifa adicional ficticia"),
                respostaAnterior(previous, "vinculo complementar"),
                contract,
                respostaAnterior(previous, "anular adicional antes fechamento"),
                respostaAnterior(previous, "calculo complementar"));
    }

    void resolucaoCarga() throws Exception {
        x.precheck("complementos-resolucao-carga-integral", false);
        x.product = get("/api/v1/produtos/" + x.ids.get("produtoId"));
        x.pack = get("/api/v1/embalagens/" + x.ids.get("embalagemId"));
        var load = criarCarga("RESCARGA", 3);
        String cr = "/api/v1/cargas-iniciais/" + load.get("id").longValue();
        load =
                post(
                        "preparar carga para resolucao integral",
                        cr + "/preparar",
                        confirmacaoCarga(load, List.of()));
        x.ids.put("cargaResolucaoPreparada", load);
        x.save();
        var u =
                get("/api/v1/unidades-logisticas/" + load.get("etiquetas").get(0).asString())
                        .get("unidade");
        var arm = j.address("CRARM", "ARMAZENAGEM");
        post("enderecar carga preparada para resolucao", j.unit(u) + "/movimentos", j.move(u, arm));
        u = j.actual(u);
        var d = x.command();
        d.putAll(
                m(
                        "clienteId",
                        x.clientId,
                        "armazemId",
                        x.warehouseId,
                        "referencia",
                        x.round + "REM-CARGA",
                        "unidades",
                        List.of(
                                m(
                                        "unidadeId",
                                        u.get("id").longValue(),
                                        "versao",
                                        u.get("versao").longValue(),
                                        "quantidade",
                                        3)),
                        "cargaInicialId",
                        load.get("id").longValue(),
                        "etiquetas",
                        List.of(u.get("codigo").asString()),
                        "justificativaFifo",
                        x.round + " retirar todo estagio identificado"));
        x.call(
                "remanescente supervisor negado",
                "POST",
                "/api/v1/encerramentos/remanescente",
                d,
                "SUPERVISOR",
                403);
        var reserved =
                post("remanescente carga integral gestor", "/api/v1/encerramentos/remanescente", d);
        post("remanescente replay", "/api/v1/encerramentos/remanescente", d);
        var p = reserved.get("pedido");
        var sep = j.address("CRSEP", "SEPARACAO");
        p = j.separate(p, u, sep);
        String xml = j.xml(3, "9");
        d = x.version(p);
        d.putAll(
                m(
                        "origem",
                        "XML",
                        "natureza",
                        "RETORNO_MERCADORIA",
                        "xml",
                        xml,
                        "protocolo",
                        x.round + " RESOLUCAO FICTICIA",
                        "coberturas",
                        List.of(
                                m(
                                        "reservaId",
                                        j.reserve(p, u),
                                        "notaOrigemId",
                                        u.get("notaId").longValue(),
                                        "sku",
                                        x.product.get("sku").asString(),
                                        "quantidade",
                                        3))));
        p =
                x.call(
                                "documento resolucao carga ficticio",
                                "POST",
                                j.ps(p) + "/documentos",
                                d,
                                "SUPERVISOR",
                                200)
                        .get("expedicao")
                        .get("pedido");
        d = x.version(p);
        d.putAll(m("xmls", List.of(xml), "remanescentes", List.of()));
        var withdrawal =
                x.call(
                        "retirada integral carga preparada",
                        "POST",
                        j.ps(p) + "/retirada",
                        d,
                        "SUPERVISOR",
                        200);
        load = get(cr);
        d = x.version(load);
        d.putAll(m("pedidoResolucaoId", p.get("id").longValue(), "contagensIds", List.of()));
        var closed =
                post("cancelar preparacao fisicamente resolvida", cr + "/resolver-cancelamento", d);
        post("resolver cancelamento replay", cr + "/resolver-cancelamento", d);
        x.assertion(
                "resolucao integral conserva historico",
                closed.get("situacao").asString().equals("CANCELADA")
                        && !j.actual(u).get("ativa").booleanValue(),
                "CANCELADA/unidade inativa",
                closed);
        x.ids.put(
                "cargaResolvida",
                m("carga", closed, "retirada", withdrawal, "unidade", j.actual(u)));
        x.save();
    }

    void retomarResolucaoCarga() throws Exception {
        x.precheck("complementos-resolucao-carga-gestor", false);
        x.product = get("/api/v1/produtos/" + x.ids.get("produtoId"));
        var previous =
                x.json.readTree(
                        java.nio.file.Files.readString(
                                x.evidence.resolve(
                                        "d26-" + x.ids.get("baseRodada") + "-http.json")));
        JsonNode reserved = null, sep = null;
        for (var c : previous.get("cases")) {
            if (c.get("caso").asString().equals("remanescente carga integral gestor"))
                reserved = c.get("resposta");
            if (c.get("caso").asString().equals("endereco SEPARACAO")) sep = c.get("resposta");
        }
        if (reserved == null || sep == null)
            throw new IllegalStateException("D26_RETOMADA_RESOLUCAO_CARGA_INCOMPLETA");
        var load = x.json.valueToTree(x.ids.get("cargaResolucaoPreparada"));
        var p = get(j.ps(reserved.get("pedido")));
        var u =
                get("/api/v1/unidades-logisticas/"
                                + p.get("reservas").get(0).get("codigoUnidade").asString())
                        .get("unidade");
        var d = x.version(p);
        d.putAll(
                m(
                        "reservaId",
                        j.reserve(p, u),
                        "codigoLido",
                        u.get("codigo").asString(),
                        "revisaoConteudo",
                        get(j.unit(u) + "/etiqueta").get("versaoConteudo").longValue()));
        p =
                post("leitura resolucao exclusiva gestor", j.ps(p) + "/leituras", d)
                        .get("expedicao")
                        .get("pedido");
        d = x.version(p);
        d.put("destinacao", m("reservaId", j.reserve(p, u), "destinos", j.destinations(sep)));
        p =
                post("separacao resolucao exclusiva gestor", j.ps(p) + "/separacoes", d)
                        .get("expedicao")
                        .get("pedido");
        String xml = j.xml(3, "9");
        d = x.version(p);
        d.putAll(
                m(
                        "origem",
                        "XML",
                        "natureza",
                        "RETORNO_MERCADORIA",
                        "xml",
                        xml,
                        "protocolo",
                        x.round + " RESOLUCAO FICTICIA",
                        "coberturas",
                        List.of(
                                m(
                                        "reservaId",
                                        j.reserve(p, u),
                                        "notaOrigemId",
                                        u.get("notaId").longValue(),
                                        "sku",
                                        x.product.get("sku").asString(),
                                        "quantidade",
                                        3))));
        p =
                post("documento resolucao carga ficticio", j.ps(p) + "/documentos", d)
                        .get("expedicao")
                        .get("pedido");
        d = x.version(p);
        d.putAll(m("xmls", List.of(xml), "remanescentes", List.of()));
        var withdrawal = post("retirada integral carga preparada gestor", j.ps(p) + "/retirada", d);
        String cr = "/api/v1/cargas-iniciais/" + load.get("id").longValue();
        load = get(cr);
        d = x.version(load);
        d.putAll(m("pedidoResolucaoId", p.get("id").longValue(), "contagensIds", List.of()));
        var closed =
                post("cancelar preparacao fisicamente resolvida", cr + "/resolver-cancelamento", d);
        post("resolver cancelamento replay", cr + "/resolver-cancelamento", d);
        x.assertion(
                "resolucao integral conserva historico",
                closed.get("situacao").asString().equals("CANCELADA")
                        && !j.actual(u).get("ativa").booleanValue(),
                "CANCELADA/unidade inativa",
                closed);
        x.ids.put(
                "cargaResolvida",
                m("carga", closed, "retirada", withdrawal, "unidade", j.actual(u)));
        x.save();
    }

    void marcoAvariaAnterior() throws Exception {
        x.precheck("complementos-marco-financeiro", false);
        // Família original preservada; fato RETIRADA real, sem ajuste/schema nem relógio alterado.
        var original =
                x.json.readTree(
                                java.nio.file.Files.readString(
                                        x.evidence.resolve("d26-D2603638663-http.json")))
                        .get("ids");
        long oldClient = x.clientId, oldWarehouse = x.warehouseId;
        x.clientId = original.get("clienteId").longValue();
        x.warehouseId = original.get("armazemId").longValue();
        try {
            var u = j.actual(original.get("unidades").get(1));
            var estoque = get(j.unit(u) + "/estoque");
            var origin = estoque.get("posicoes").get(0);
            var fatos =
                    get(
                            "/api/v1/pedidos-saida/"
                                    + original.get("pedidoSaidaId").longValue()
                                    + "/fatos");
            JsonNode fact = null;
            for (var f : fatos)
                if (f.get("unidadeId").longValue() == u.get("id").longValue()
                        && f.get("tipo").asString().equals("RETIRADA")) fact = f;
            if (fact == null) throw new IllegalStateException("D26_MARCO_FATO_RETIRADA_AUSENTE");
            var q = j.address("MARQUA", "QUARENTENA");
            var d = x.command();
            d.putAll(
                    m(
                            "versaoUnidade",
                            u.get("versao").longValue(),
                            "quantidade",
                            2,
                            "ocorridaEm",
                            Instant.parse(fact.get("ocorridaEm").asString())
                                    .minusMillis(1)
                                    .toString(),
                            "destinos",
                            j.destinations(q)));
            String avarias = "/api/v1/estoque/unidades/" + u.get("codigo").asString() + "/avarias";
            var registered =
                    x.call(
                            "avaria ficticia retroativa comprovada antes retirada",
                            "POST",
                            avarias,
                            d,
                            "SUPERVISOR",
                            200);
            var a = registered.get("avaria");
            d = x.version(a);
            d.put("responsabilidade", "RODOGARCIA");
            a =
                    post(
                                    "reconhecimento marco ficticio",
                                    avarias + "/" + a.get("id").longValue() + "/responsabilidade",
                                    d)
                            .get("avaria");
            d = x.command();
            d.putAll(m("fatoPermanenciaId", fact.get("id").longValue(), "quantidadeAfetada", 2));
            x.call(
                    "marco financeiro supervisor negado",
                    "POST",
                    "/api/v1/avarias/" + a.get("id").longValue() + "/marcos-financeiros",
                    d,
                    "SUPERVISOR",
                    403);
            var mark =
                    post(
                            "marco financeiro parte afetada remanescente",
                            "/api/v1/avarias/" + a.get("id").longValue() + "/marcos-financeiros",
                            d);
            post(
                    "marco financeiro replay",
                    "/api/v1/avarias/" + a.get("id").longValue() + "/marcos-financeiros",
                    d);
            get("/api/v1/avarias/" + a.get("id").longValue() + "/marcos-financeiros");
            u = j.actual(u);
            d = x.version(a);
            d.putAll(
                    m(
                            "versaoUnidade",
                            u.get("versao").longValue(),
                            "destinos",
                            List.of(
                                    m(
                                            "enderecoId",
                                            origin.get("enderecoId").longValue(),
                                            "codigoLido",
                                            origin.get("codigo").asString()))));
            var repaired =
                    x.call(
                            "reparo marco conserva quantidade",
                            "POST",
                            avarias + "/" + a.get("id").longValue() + "/reparo",
                            d,
                            "SUPERVISOR",
                            200);
            x.ids.put(
                    "marcoComplementar",
                    m(
                            "clienteId",
                            x.clientId,
                            "armazemId",
                            x.warehouseId,
                            "marco",
                            mark,
                            "reparo",
                            repaired));
            x.save();
        } finally {
            x.clientId = oldClient;
            x.warehouseId = oldWarehouse;
        }
    }

    void vigenciasIsoladas() throws Exception {
        x.precheck("complementos-vigencias-isoladas", false);
        x.ids.put("familiaAnterior", new LinkedHashMap<>(x.ids));
        j.cadastros();
        LocalDate today = LocalDate.now(ZoneOffset.UTC), start = today.minusDays(10);
        var d = x.command();
        d.putAll(
                m(
                        "codigo",
                        x.round + "VIG",
                        "descricao",
                        x.round + " vigencia ficticia sem operacao",
                        "tipo",
                        "ADICIONAL",
                        "unidade",
                        "VEICULO"));
        var s = post("servico vigencia ficticio", "/api/v1/servicos-cobranca", d);
        d = x.command();
        d.putAll(
                m(
                        "armazemId",
                        x.warehouseId,
                        "codigo",
                        x.round + "TV",
                        "descricao",
                        x.round + " tabela ficticia",
                        "tipo",
                        "PADRAO",
                        "vigenciaInicio",
                        start.toString(),
                        "itens",
                        List.of(
                                m(
                                        "servicoId",
                                        s.get("id").longValue(),
                                        "categoria",
                                        "",
                                        "preco",
                                        1))));
        var t = post("tabela vigencia isolada", "/api/v1/tabelas-cobranca", d);
        d = x.command();
        d.putAll(
                m(
                        "clienteId",
                        x.clientId,
                        "armazemId",
                        x.warehouseId,
                        "tabelaId",
                        t.get("id").longValue(),
                        "vigenciaInicio",
                        start.toString()));
        var v = post("vinculo vigencia isolado", "/api/v1/vinculos-tabela", d);
        d = x.command();
        d.putAll(
                m(
                        "clienteId",
                        x.clientId,
                        "armazemId",
                        x.warehouseId,
                        "vigenciaInicio",
                        start.toString(),
                        "fuso",
                        "UTC",
                        "moeda",
                        "BRL",
                        "modalidadeCiclo",
                        "DIAS_CORRIDOS",
                        "duracaoDias",
                        1,
                        "minimoModo",
                        "NAO_APLICAVEL",
                        "grisModo",
                        "NAO_APLICAVEL",
                        "servicosMinimo",
                        List.of()));
        var c = post("contrato vigencia isolado", "/api/v1/contratos-cobranca", d);
        var close = preparar(calcular(start, start.plusDays(1)));
        d = x.version(t);
        d.put("vigenciaFim", today.toString());
        t =
                post(
                        "encerrar tabela sem historia posterior",
                        "/api/v1/tabelas-cobranca/" + t.get("id").longValue() + "/encerramento",
                        d);
        d = x.version(v);
        d.put("vigenciaFim", today.toString());
        v =
                post(
                        "encerrar vinculo sem historia posterior",
                        "/api/v1/vinculos-tabela/" + v.get("id").longValue() + "/encerramento",
                        d);
        d = x.version(c);
        d.put("vigenciaFim", today.toString());
        c =
                post(
                        "encerrar contrato no corte diario",
                        "/api/v1/contratos-cobranca/" + c.get("id").longValue() + "/encerramento",
                        d);
        d = x.version(c);
        d.putAll(
                m(
                        "corte",
                        start.plusDays(2).toString(),
                        "resolucao",
                        m(
                                "tipo",
                                "FECHAMENTO",
                                "compromissoId",
                                close.get("fechamento").get("id").longValue())));
        var ended =
                post(
                        "vigencia administrativa referencia no mesmo contexto",
                        "/api/v1/encerramentos/vigencias/CONTRATO/"
                                + c.get("id").longValue()
                                + "/encerrar",
                        d);
        post(
                "vigencia administrativa replay",
                "/api/v1/encerramentos/vigencias/CONTRATO/" + c.get("id").longValue() + "/encerrar",
                d);
        get("/api/v1/contratos-cobranca?clienteId=" + x.clientId + "&armazemId=" + x.warehouseId);
        x.ids.put(
                "vigenciasComplementares",
                m(
                        "tabela",
                        t,
                        "vinculo",
                        v,
                        "contrato",
                        c,
                        "corteAdministrativo",
                        ended,
                        "fechamento",
                        close));
        x.save();
    }

    void retomarVigenciaFinal() throws Exception {
        x.precheck("complementos-vigencia-corte-sem-historia-posterior", false);
        var previous =
                x.json.readTree(
                        java.nio.file.Files.readString(
                                x.evidence.resolve(
                                        "d26-" + x.ids.get("baseRodada") + "-http.json")));
        var c = respostaAnterior(previous, "encerrar contrato no corte diario");
        var close = respostaAnterior(previous, "preparar fechamento zero");
        var d = x.version(c);
        d.putAll(
                m(
                        "corte",
                        LocalDate.parse(c.get("vigenciaInicio").asString()).plusDays(2).toString(),
                        "resolucao",
                        m(
                                "tipo",
                                "FECHAMENTO",
                                "compromissoId",
                                close.get("fechamento").get("id").longValue())));
        String route =
                "/api/v1/encerramentos/vigencias/CONTRATO/" + c.get("id").longValue() + "/encerrar";
        x.call("vigencia administrativa supervisor negado", "POST", route, d, "SUPERVISOR", 403);
        var ended = post("vigencia administrativa corte posterior ao unico calculo", route, d);
        post("vigencia administrativa replay", route, d);
        get("/api/v1/contratos-cobranca?clienteId=" + x.clientId + "&armazemId=" + x.warehouseId);
        x.ids.put("vigenciaFinal", m("contrato", ended, "fechamento", close));
        x.save();
    }

    void leiturasFinais() throws Exception {
        x.precheck("leituras-finais-calculo4-corrigido", false);
        var repro =
                x.json.readTree(
                        java.nio.file.Files.readString(
                                x.evidence.resolve("d26-D26EB3EA406-http.json")));
        var fixed =
                x.json.readTree(
                        java.nio.file.Files.readString(
                                x.evidence.resolve("d26-D265CE7E9F0-http.json")));
        x.clientId = repro.get("ids").get("clienteId").longValue();
        x.warehouseId = repro.get("ids").get("armazemId").longValue();
        var expected = respostaAnterior(fixed, "calculo transformado conserva pendencias sem500");
        String route = "/api/v1/calculos-cobranca/" + expected.get("id").longValue();
        var actual = get(route);
        x.assertion(
                "GET calculo4 corresponde POST pendente completo",
                actual.equals(expected),
                expected,
                actual);
        x.call("GET calculo4 supervisor alcance correto", "GET", route, null, "SUPERVISOR", 200);
        x.call("GET calculo4 operacao negada", "GET", route, null, "OPERACAO", 403);
        x.callToken(
                "GET calculo4 supervisor cliente fora alcance",
                "GET",
                route,
                null,
                "SUPERVISOR",
                x.token("SUPERVISOR", m("wms_clientes", List.of())),
                403);
        x.callToken(
                "GET calculo4 supervisor armazem fora alcance",
                "GET",
                route,
                null,
                "SUPERVISOR",
                x.token("SUPERVISOR", m("wms_armazens", List.of())),
                403);
        get(
                "/api/v1/calculos-cobranca?clienteId="
                        + x.clientId
                        + "&armazemId="
                        + x.warehouseId
                        + "&pagina=0&tamanho=5");
        var payload =
                x.json.convertValue(
                        repro.get("cases").get(repro.get("cases").size() - 1).get("payload"),
                        Map.class);
        var replay =
                post(
                        "replay final mesmo GUID antes500 retorna4",
                        "/api/v1/calculos-cobranca",
                        payload);
        x.assertion(
                "replay final nao cria calculo novo", replay.equals(expected), expected, replay);
        x.ids.put("getCalculo4Corrigido", actual);
        x.save();
        x.clientId = fixed.get("ids").get("clienteId").longValue();
        x.warehouseId = fixed.get("ids").get("armazemId").longValue();
        get("/api/v1/calculos-cobranca/5");
        get("/api/v1/calculos-cobranca/11");
        get(
                "/api/v1/estoque/saldo?clienteId="
                        + x.clientId
                        + "&armazemId="
                        + x.warehouseId
                        + "&produtoId="
                        + fixed.get("ids").get("produtoId").longValue());
        get("/api/v1/auditoria?tipo=CALCULO_COBRANCA&registroId=4&pagina=0&tamanho=10");
    }

    void auditoriaFinal() throws Exception {
        x.precheck("leitura-final-auditoria-calculo4", false);
        var calc = get("/api/v1/calculos-cobranca/4");
        var audits =
                get("/api/v1/auditoria?tipo=CALCULO_COBRANCA&registroId=4&pagina=0&tamanho=10");
        x.assertion(
                "calculo4 pendente com auditoria unica",
                calc.get("total").isNull()
                        && calc.get("situacao").asString().equals("PENDENTE")
                        && audits.get("itens").size() == 1,
                "PENDENTE/totalNULL/auditoria1",
                m("calculo", calc, "auditoria", audits));
        x.ids.put("auditoriaCalculo4Final", audits);
        x.save();
    }

    void fechamentoCorteReal() throws Exception {
        x.precheck("extra-fechamento-corte-real", false);
        var old = get("/api/v1/calculos-cobranca/5");
        LocalDate target = LocalDate.parse(old.get("periodoInicio").asString());
        LocalDate end = LocalDate.parse(old.get("periodoFim").asString());
        x.ids.put("extraClockUTC", Instant.now().toString());
        if (end.isAfter(LocalDate.now(ZoneOffset.UTC)))
            throw new IllegalStateException("D26_EXTRA_CORTE_AINDA_FUTURO");
        var fact = get("/api/v1/fatos-servico/3");
        var contracts =
                get(
                        "/api/v1/contratos-cobranca?clienteId="
                                + x.clientId
                                + "&armazemId="
                                + x.warehouseId);
        var last = get("/api/v1/fechamentos-cobranca/3");
        var all =
                get(
                        "/api/v1/fechamentos-cobranca?clienteId="
                                + x.clientId
                                + "&armazemId="
                                + x.warehouseId
                                + "&pagina=0&tamanho=100");
        for (var f : all.get("itens"))
            if (LocalDate.parse(f.get("periodoFim").asString())
                    .isAfter(LocalDate.parse(last.get("periodoFim").asString()))) last = f;
        LocalDate next = LocalDate.parse(last.get("periodoFim").asString());
        if (next.isAfter(target)) throw new IllegalStateException("D26_EXTRA_PERIODO_JA_PREPARADO");
        if (java.time.temporal.ChronoUnit.DAYS.between(next, target) > 12)
            throw new IllegalStateException("D26_EXTRA_LIMITE_CICLOS");
        var intermediate = new java.util.ArrayList<Object>();
        while (next.isBefore(target)) {
            x.precheck("extra-ciclo-" + next, false);
            var zero = calcular(next, next.plusDays(1));
            x.assertion(
                    "ciclo intermediario real zero " + next,
                    zero.get("total").decimalValue().signum() == 0
                            && zero.get("situacao").asString().equals("COMPLETO"),
                    "COMPLETO/zero",
                    zero);
            var closed = preparar(zero);
            get(fc(closed));
            intermediate.add(
                    m(
                            "calculoId",
                            zero.get("id").longValue(),
                            "fechamentoId",
                            closed.get("fechamento").get("id").longValue(),
                            "inicio",
                            next.toString(),
                            "fim",
                            next.plusDays(1).toString()));
            x.ids.put("extraCiclosIntermediarios", intermediate);
            x.save();
            next = next.plusDays(1);
        }
        x.precheck("extra-aprovacao-corte-encerrado", false);
        var fresh = calcular(target, end);
        x.ids.put("extraCalculoAtual", fresh);
        x.ids.put("extraFatoServico", fact);
        x.ids.put("extraContratos", contracts);
        x.save();
        boolean twenty =
                fresh.get("total") != null
                        && !fresh.get("total").isNull()
                        && fresh.get("total")
                                        .decimalValue()
                                        .compareTo(new java.math.BigDecimal("20"))
                                == 0;
        if (!twenty
                && (!fact.get("situacao").asString().equals("ANULADO")
                        || fresh.get("total").decimalValue().signum() != 0))
            throw new IllegalStateException("D26_EXTRA_CALCULO_DIVERGENTE_CONFERIR_GUARDA");
        // Snapshot20 anterior é conservado. Após anulação, aprová-lo deve ser recusado por fonte
        // desatualizada.
        var selected = twenty ? fresh : old;
        var d = x.command();
        d.put("calculoId", selected.get("id").longValue());
        var prepared =
                post("extra preparar periodo civil encerrado", "/api/v1/fechamentos-cobranca", d);
        post("extra preparar replay", "/api/v1/fechamentos-cobranca", d);
        var decision = decisao(prepared);
        x.call(
                "extra aprovacao operacao negada",
                "POST",
                fc(prepared) + "/aprovacao",
                decision,
                "OPERACAO",
                403);
        x.call(
                "extra aprovacao supervisor negada",
                "POST",
                fc(prepared) + "/aprovacao",
                decision,
                "SUPERVISOR",
                403);
        if (twenty) {
            var approved =
                    post("extra aprovacao20 corte real", fc(prepared) + "/aprovacao", decision);
            post("extra aprovacao20 replay", fc(prepared) + "/aprovacao", decision);
            x.assertion(
                    "extra aprovado20 memoria",
                    approved.get("fechamento").get("situacao").asString().equals("APROVADO")
                            && approved.get("versao").get("saldo").decimalValue().intValueExact()
                                    == 20,
                    "APROVADO/20",
                    approved);
            x.ids.put("extraNaoZeroAprovado", approved);
        } else {
            var refused =
                    x.call(
                            "extra20 anulado impede aprovacao antiga",
                            "POST",
                            fc(prepared) + "/aprovacao",
                            decision,
                            "GESTOR",
                            409);
            x.call(
                    "extra20 recusa repetida conserva estado",
                    "POST",
                    fc(prepared) + "/aprovacao",
                    decision,
                    "GESTOR",
                    409);
            x.assertion(
                    "extra guarda fonte desatualizada",
                    refused.get("codigo").asString().equals("CALCULO_DESATUALIZADO"),
                    "CALCULO_DESATUALIZADO",
                    refused);
            x.ids.put(
                    "extraNaoZeroBloqueio",
                    m("causa", "FATO3_ANULADO_CALCULO5_DESATUALIZADO", "recusa", refused));
        }
        var head = get(fc(prepared));
        int number = head.get("versaoAtual").intValue();
        get(fc(prepared) + "/versoes");
        get(fc(prepared) + "/versoes/" + number);
        var demo = get(fc(prepared) + "/versoes/" + number + "/demonstrativo");
        x.ids.put("extraFechamento", m("cabecalho", head, "demonstrativo", demo));
        get(
                "/api/v1/auditoria?tipo=FECHAMENTO_COBRANCA&registroId="
                        + head.get("id").longValue()
                        + "&pagina=0&tamanho=20");
        x.save();
    }
}
