import tools.jackson.databind.JsonNode;

import java.time.*;
import java.util.*;
import java.util.concurrent.*;

final class JornadasD26 {
    static Map<String, Object> m(Object... pairs) {
        return EnsaioD26.m(pairs);
    }

    final EnsaioD26 x;
    int number = 100;
    final String G = "GESTOR", S = "SUPERVISOR", O = "OPERACAO";

    JornadasD26(EnsaioD26 x) {
        this.x = x;
        number = 100000 + Math.floorMod(x.round.hashCode(), 800000000);
    }

    JsonNode get(String path) throws Exception {
        return x.call("consulta " + path, "GET", path, null, O, 200);
    }

    JsonNode post(String name, String path, Object body, String role, int expected)
            throws Exception {
        return x.call(name, "POST", path, body, role, expected);
    }

    String pe(long id) {
        return "/api/v1/pedidos-entrada/" + id;
    }

    String ps(JsonNode p) {
        return "/api/v1/pedidos-saida/" + p.get("id").longValue();
    }

    String unit(JsonNode u) {
        return "/api/v1/unidades-logisticas/" + u.get("codigo").asString();
    }

    long pv(long id) throws Exception {
        return get(pe(id)).get("pedido").get("versao").longValue();
    }

    void run() throws Exception {
        auth();
        cadastros();
        recebimentoEstoqueSaida();
        financeiro();
        avariaRetornoXml();
        contagemContingencia();
        auditoriaListas();
    }

    void resumeAfterConcurrency() throws Exception {
        x.product = get("/api/v1/produtos/" + x.ids.get("produtoId"));
        x.pack = get("/api/v1/embalagens/" + x.ids.get("embalagemId"));
        entryId = ((Number) x.ids.get("pedidoEntradaId")).longValue();
        noteId = ((Number) x.ids.get("notaId")).longValue();
        var saved = x.json.valueToTree(x.ids.get("unidades"));
        var a = actual(saved.get(0));
        var b = actual(saved.get(1));
        storageA =
                get(
                        "/api/v1/enderecos/"
                                + get(unit(a) + "/estoque")
                                        .get("posicoes")
                                        .get(0)
                                        .get("enderecoId")
                                        .longValue());
        storageB =
                get(
                        "/api/v1/enderecos/"
                                + get(unit(b) + "/estoque")
                                        .get("posicoes")
                                        .get(0)
                                        .get("enderecoId")
                                        .longValue());
        var prior = x.json.valueToTree(x.ids.get("concorrencia"));
        for (var id : prior.get("pedidos")) {
            var state = get("/api/v1/pedidos-saida/" + id.longValue());
            if (state.get("situacao").asString().equals("RESERVADO"))
                post(
                        "retomada reversao auditada vencedor anterior",
                        ps(state) + "/reversao-reserva",
                        x.version(state),
                        S,
                        200);
            else if (state.get("situacao").asString().equals("RASCUNHO"))
                post(
                        "retomada cancelar rascunho supervisor",
                        ps(state) + "/cancelamento",
                        x.version(state),
                        S,
                        200);
        }
        x.assertion(
                "retomada reserva zero",
                balance().get("reservado").decimalValue().signum() == 0,
                0,
                balance());
        concurrency();
        concluirSaida(actual(a), actual(b));
        financeiro();
        avariaRetornoXml();
        contagemContingencia();
        auditoriaListas();
    }

    void runOnlyConcurrency() throws Exception {
        cadastros();
        prepararRecebimento();
        concurrency();
        x.ids.put("saldoFinal", balance());
        x.save();
    }

    void resumeOnlyConcurrency() throws Exception {
        resumeStock();
        concurrency();
        x.ids.put("saldoFinal", balance());
        x.save();
    }

    void resumeAfterAvaria() throws Exception {
        resumeStock();
        tratarAvariaXmlRetorno(x.json.valueToTree(x.ids.get("avariaRetomada")));
        contagemContingencia();
        auditoriaListas();
    }

    void resumeAfterXml() throws Exception {
        resumeStock();
        x.ids.put(
                "avariaId", x.json.valueToTree(x.ids.get("avariaRetomada")).get("id").longValue());
        xmlRetorno();
        contagemContingencia();
        auditoriaListas();
    }

    void resumeAfterReturn() throws Exception {
        resumeStock();
        contagemContingencia();
        auditoriaListas();
    }

    void resumeStock() throws Exception {
        x.precheck("retomada-avaria", false);
        x.product = get("/api/v1/produtos/" + x.ids.get("produtoId"));
        x.pack = get("/api/v1/embalagens/" + x.ids.get("embalagemId"));
        entryId = ((Number) x.ids.get("pedidoEntradaId")).longValue();
        noteId = ((Number) x.ids.get("notaId")).longValue();
        x.ids.put(
                "clienteDocumentoFiscal",
                get("/api/v1/clientes/" + x.clientId).get("documentoFiscal").asString());
        x.ids.put(
                "armazemDocumentoFiscal",
                get("/api/v1/armazens/" + x.warehouseId).get("documentoFiscal").asString());
        var saved = x.json.valueToTree(x.ids.get("unidades"));
        remaining = actual(saved.get(1));
        var list = get("/api/v1/enderecos?armazemId=" + x.warehouseId + "&tamanho=100");
        for (var e : list.get("itens"))
            if (e.get("codigo").asString().endsWith("ARM2")) storageB = e;
    }

    void auth() throws Exception {
        x.phase = "auth";
        var inventory =
                x.json.readTree(
                        java.nio.file.Files.readString(x.evidence.resolve("d26-inventario.json")));
        for (var e : inventory) {
            String path = e.get("rota").asString().replaceAll("\\{[^}]+}", "1");
            String method = e.get("metodo").asString();
            x.call(
                    "anonimo " + e.get("id").asString(),
                    method,
                    path,
                    method.equals("GET") ? null : Map.of(),
                    null,
                    path.equals("/api/v1/status") ? 200 : 401);
        }
        var bad =
                List.of(
                        m("aud", List.of("outro")),
                        m("iss", "https://invalido.local"),
                        m("exp", Date.from(Instant.now().minusSeconds(120))),
                        m("iat", Date.from(Instant.now().plusSeconds(300))),
                        m("wms_perfil", "ADMIN"),
                        m("wms_clientes", List.of("0")),
                        m("wms_armazens", List.of(1)),
                        m("sub", ""));
        int n = 0;
        for (var b : bad)
            x.callToken(
                    "JWT claims invalido " + (++n),
                    "GET",
                    "/api/v1/clientes",
                    null,
                    G,
                    x.token(G, b),
                    401);
        String token = x.token(G, Map.of());
        int at = token.lastIndexOf('.') + 1;
        String altered =
                token.substring(0, at)
                        + (token.charAt(at) == 'A' ? 'B' : 'A')
                        + token.substring(at + 1);
        x.callToken("JWT assinatura invalida", "GET", "/api/v1/clientes", null, G, altered, 401);
        x.call("rota inexistente", "GET", "/api/v1/rota-d26-inexistente", null, G, 403);
    }

    void cadastros() throws Exception {
        x.precheck("cadastros", false);
        var c =
                m(
                        "codigo",
                        x.round + "C",
                        "nome",
                        x.round + " Cliente ficticio Ação",
                        "documentoFiscal",
                        x.round + "CCC");
        post("cliente operador recusado", "/api/v1/clientes", c, O, 403);
        var client = post("cliente criado", "/api/v1/clientes", c, G, 201);
        x.clientId = client.get("id").longValue();
        x.ids.put("clienteId", x.clientId);
        post("cliente duplicado sem nova linha", "/api/v1/clientes", c, G, 409);
        x.call(
                "cliente validacao",
                "POST",
                "/api/v1/clientes",
                m("codigo", "", "nome", "", "documentoFiscal", ""),
                G,
                400);
        var wh =
                post(
                        "armazem criado",
                        "/api/v1/armazens",
                        m(
                                "codigo",
                                x.round + "A",
                                "nome",
                                x.round + " Armazem ficticio",
                                "documentoFiscal",
                                x.round + "AAA",
                                "cidade",
                                "Osasco",
                                "uf",
                                "SP"),
                        G,
                        201);
        x.warehouseId = wh.get("id").longValue();
        x.ids.put("armazemId", x.warehouseId);
        for (String role : List.of(G, S, O))
            x.call(
                    "JWT real perfil " + role,
                    "GET",
                    "/api/v1/clientes/" + x.clientId,
                    null,
                    role,
                    200);
        x.callToken(
                "cliente fora alcance",
                "GET",
                "/api/v1/clientes/" + x.clientId,
                null,
                O,
                x.token(O, m("wms_clientes", List.of())),
                403);
        var filtered =
                x.callToken(
                        "lista filtra alcance",
                        "GET",
                        "/api/v1/clientes",
                        null,
                        O,
                        x.token(O, m("wms_clientes", List.of())),
                        200);
        x.assertion(
                "lista fora alcance vazia",
                filtered.get("itens").isEmpty(),
                0,
                filtered.get("itens").size());
        x.call("cliente inexistente", "GET", "/api/v1/clientes/9223372036854775806", null, G, 404);
        x.call(
                "paginacao invalida",
                "GET",
                "/api/v1/clientes?pagina=-1&tamanho=1001",
                null,
                G,
                400);
        x.call(
                "cliente atualizado colunas permitidas",
                "PUT",
                "/api/v1/clientes/" + x.clientId,
                m(
                        "versao",
                        client.get("versao").longValue(),
                        "nome",
                        x.round + " Cliente revisado Ação",
                        "motivo",
                        x.round + " revisao ficticia"),
                G,
                200);
        x.product =
                post(
                        "produto criado",
                        "/api/v1/produtos",
                        m(
                                "clienteId",
                                x.clientId,
                                "sku",
                                x.round + "SKU",
                                "descricao",
                                x.round + " Produto ficticio",
                                "unidadeMedida",
                                "UN",
                                "tipoQuantidade",
                                "CONTAGEM",
                                "precisaoQuantidade",
                                0,
                                "controlaLote",
                                false,
                                "controlaValidade",
                                false),
                        G,
                        201);
        x.pack =
                post(
                        "DUN criado",
                        "/api/v1/embalagens",
                        m(
                                "produtoId",
                                x.product.get("id").longValue(),
                                "codigoDun",
                                x.round + "DUN",
                                "descricao",
                                x.round + " Configuracao DUN ficticia",
                                "quantidadeProduto",
                                10),
                        G,
                        201);
        x.ids.put("produtoId", x.product.get("id").longValue());
        x.ids.put("embalagemId", x.pack.get("id").longValue());
        x.save();
        x.call(
                "produto filtro paginado",
                "GET",
                "/api/v1/produtos?clienteId=" + x.clientId + "&pagina=0&tamanho=1",
                null,
                O,
                200);
        x.call(
                "DUN filtro paginado",
                "GET",
                "/api/v1/embalagens?produtoId="
                        + x.product.get("id").longValue()
                        + "&pagina=0&tamanho=1",
                null,
                O,
                200);
    }

    JsonNode address(String suffix, String type) throws Exception {
        var e =
                post(
                        "endereco " + type,
                        "/api/v1/enderecos",
                        m(
                                "armazemId",
                                x.warehouseId,
                                "codigo",
                                x.round + suffix,
                                "rua",
                                x.round,
                                "nivel",
                                1,
                                "posicao",
                                suffix,
                                "descricao",
                                x.round + " Posicao ficticia",
                                "tipo",
                                type,
                                "sequenciaColeta",
                                ++number),
                        G,
                        201);
        return x.call(
                "capacidade " + type,
                "PUT",
                "/api/v1/enderecos/" + e.get("id").longValue() + "/capacidade",
                m(
                        "versao",
                        e.get("versao").longValue(),
                        "tipoUnidadePermitido",
                        "PALLET",
                        "limites",
                        m(
                                "pesoKg",
                                1000,
                                "alturaMetros",
                                2,
                                "larguraMetros",
                                2,
                                "profundidadeMetros",
                                2,
                                "empilhamentoMaximo",
                                2),
                        "motivo",
                        x.round + " capacidade ficticia"),
                G,
                200);
    }

    long entry(String suffix, int quantity) throws Exception {
        long id =
                post(
                                "pedido entrada " + suffix,
                                "/api/v1/pedidos-entrada",
                                m(
                                        "clienteId",
                                        x.clientId,
                                        "armazemId",
                                        x.warehouseId,
                                        "referencia",
                                        x.round + suffix),
                                O,
                                201)
                        .get("id")
                        .longValue();
        post(
                "nota manual ficticia",
                pe(id) + "/notas",
                m(
                        "versao",
                        pv(id),
                        "serie",
                        1,
                        "numero",
                        ++number,
                        "emissao",
                        LocalDate.now().minusDays(3).toString(),
                        "itens",
                        List.of(
                                m(
                                        "numeroItem",
                                        1,
                                        "produtoId",
                                        x.product.get("id").longValue(),
                                        "quantidadePrevista",
                                        quantity,
                                        "valorMercadoria",
                                        quantity * 10))),
                O,
                200);
        post(
                "iniciar conferencia",
                pe(id) + "/iniciar-conferencia",
                m("versao", pv(id), "motivo", x.round + " conferencia fisica"),
                O,
                200);
        return id;
    }

    void arrival(long id, int quantity, Instant when) throws Exception {
        long item = get(pe(id)).get("notas").get(0).get("itens").get(0).get("id").longValue();
        var body =
                m(
                        "versao",
                        pv(id),
                        "operacaoId",
                        UUID.randomUUID().toString(),
                        "chegouEm",
                        when.toString(),
                        "observacao",
                        x.round + " chegada ficticia",
                        "itens",
                        List.of(
                                m(
                                        "itemNotaId",
                                        item,
                                        "quantidadeBoa",
                                        quantity,
                                        "quantidadeAvariada",
                                        0)));
        var a = post("chegada fisica", pe(id) + "/chegadas", body, O, 200);
        var replay = post("chegada replay", pe(id) + "/chegadas", body, O, 200);
        x.assertion("chegada replay igual", a.equals(replay), a, replay);
    }

    JsonNode unitize(long pe, long entry, int quantity) throws Exception {
        var cmd =
                m(
                        "operacaoId",
                        UUID.randomUUID().toString(),
                        "versaoPedido",
                        pv(pe),
                        "motivo",
                        x.round + " unitizacao ficticia",
                        "unidades",
                        List.of(
                                m(
                                        "embalagemId",
                                        x.pack.get("id").longValue(),
                                        "tipo",
                                        "PALLET",
                                        "condicao",
                                        "BOA",
                                        "quantidade",
                                        quantity)));
        var a = post("unitizar", pe(pe) + "/entradas/" + entry + "/unitizacao", cmd, O, 200);
        var b =
                post(
                        "unitizacao replay",
                        pe(pe) + "/entradas/" + entry + "/unitizacao",
                        cmd,
                        O,
                        200);
        x.assertion("unitizacao replay igual", a.equals(b), a, b);
        return a.get("unidades").get(0).get("unidade");
    }

    JsonNode actual(JsonNode u) throws Exception {
        return get(unit(u)).get("unidade");
    }

    List<Map<String, Object>> destinations(JsonNode e) {
        return List.of(
                m("enderecoId", e.get("id").longValue(), "codigoLido", e.get("codigo").asString()));
    }

    Map<String, Object> move(JsonNode u, JsonNode e) {
        var d = x.command();
        d.put("versaoUnidade", u.get("versao").longValue());
        d.put("destinos", destinations(e));
        d.put(
                "medidas",
                m(
                        "pesoKg",
                        500,
                        "alturaMetros",
                        1,
                        "larguraMetros",
                        1,
                        "profundidadeMetros",
                        1,
                        "empilhamento",
                        1,
                        "posicoesNecessarias",
                        1));
        return d;
    }

    JsonNode order(String suffix, int quantity) throws Exception {
        var d = x.command();
        d.putAll(
                m(
                        "clienteId",
                        x.clientId,
                        "armazemId",
                        x.warehouseId,
                        "referencia",
                        x.round + suffix,
                        "itens",
                        List.of(
                                m(
                                        "produtoId",
                                        x.product.get("id").longValue(),
                                        "quantidade",
                                        quantity))));
        return post("pedido saida " + suffix, "/api/v1/pedidos-saida", d, O, 201).get("pedido");
    }

    JsonNode balance() throws Exception {
        return get(
                "/api/v1/estoque/saldo?clienteId="
                        + x.clientId
                        + "&armazemId="
                        + x.warehouseId
                        + "&produtoId="
                        + x.product.get("id").longValue());
    }

    JsonNode remaining, stored;
    long entryId, noteId;
    JsonNode storageA, storageB;

    void recebimentoEstoqueSaida() throws Exception {
        var units = prepararRecebimento();
        concurrency();
        concluirSaida(actual(units.get(0)), actual(units.get(1)));
    }

    List<JsonNode> prepararRecebimento() throws Exception {
        x.precheck("recebimento", false);
        entryId = entry("ENT", 100);
        x.ids.put("pedidoEntradaId", entryId);
        arrival(entryId, 50, Instant.now().minusSeconds(172800));
        arrival(entryId, 50, Instant.now().minusSeconds(86400));
        var ef =
                m(
                        "versao",
                        pv(entryId),
                        "motivo",
                        x.round + " conferencia integral",
                        "aceitarDivergencias",
                        false);
        post("efetivacao operador negada", pe(entryId) + "/efetivacao", ef, O, 403);
        post("efetivacao supervisor", pe(entryId) + "/efetivacao", ef, S, 200);
        var entries = get(pe(entryId) + "/entradas").get("itens");
        x.assertion("duas entradas parciais", entries.size() == 2, 2, entries.size());
        var a = unitize(entryId, entries.get(0).get("id").longValue(), 50);
        var b = unitize(entryId, entries.get(1).get("id").longValue(), 50);
        noteId = get(pe(entryId)).get("notas").get(0).get("id").longValue();
        x.ids.put("notaId", noteId);
        x.ids.put("unidades", List.of(a, b));
        x.save();
        var label = get(unit(b) + "/etiqueta");
        x.assertion(
                "etiqueta reimpressao sem mutacao",
                label.equals(get(unit(b) + "/etiqueta")),
                label,
                "mesmo conteudo");
        storageA = address("ARM1", "ARMAZENAGEM");
        storageB = address("ARM2", "ARMAZENAGEM");
        var movement = move(a, storageA);
        var mv = post("enderecar A", unit(a) + "/movimentos", movement, O, 200);
        var rep = post("movimento replay", unit(a) + "/movimentos", movement, O, 200);
        x.assertion("movimento replay igual", mv.equals(rep), mv, rep);
        post("enderecar B", unit(b) + "/movimentos", move(b, storageB), O, 200);
        a = actual(a);
        b = actual(b);
        x.assertion(
                "saldo fisico 100",
                balance().get("fisicoTotal").decimalValue().intValueExact() == 100,
                100,
                balance().get("fisicoTotal"));
        var insufficient = x.command();
        insufficient.putAll(
                m(
                        "clienteId",
                        x.clientId,
                        "armazemId",
                        x.warehouseId,
                        "referencia",
                        x.round + "SEM-SALDO",
                        "itens",
                        List.of(
                                m(
                                        "produtoId",
                                        x.product.get("id").longValue(),
                                        "quantidade",
                                        101))));
        post("pedido excede saldo e faz rollback", "/api/v1/pedidos-saida", insufficient, O, 409);
        x.assertion(
                "recusa nao reserva",
                balance().get("reservado").decimalValue().signum() == 0,
                0,
                balance().get("reservado"));
        var after =
                get(
                        "/api/v1/pedidos-saida?clienteId="
                                + x.clientId
                                + "&armazemId="
                                + x.warehouseId);
        x.assertion(
                "rollback nao deixa pedido",
                after.get("itens").isEmpty(),
                0,
                after.get("itens").size());
        return List.of(a, b);
    }

    void concluirSaida(JsonNode a, JsonNode b) throws Exception {
        var p = order("RET", 60);
        get(ps(p) + "/fifo");
        var rc = x.version(p);
        var r = post("reserva parcial pallet integral pedido", ps(p) + "/reserva", rc, O, 200);
        var rr = post("reserva replay", ps(p) + "/reserva", rc, O, 200);
        x.assertion("reserva replay igual", r.equals(rr), r, rr);
        p = r.get("pedido");
        x.assertion(
                "reserva FIFO 50+10",
                p.get("reservas").size() == 2
                        && p.get("reservas").get(0).get("quantidade").decimalValue().intValueExact()
                                == 50,
                2,
                p.get("reservas"));
        var sepA = address("SEP1", "SEPARACAO");
        var sepB = address("SEP2", "SEPARACAO");
        p = separate(p, a, sepA);
        p = separate(p, b, sepB);
        String xml = xml(60, "1");
        var doc = x.version(p);
        var covers = new ArrayList<Map<String, Object>>();
        for (var reserve : p.get("reservas"))
            covers.add(
                    m(
                            "reservaId",
                            reserve.get("id").longValue(),
                            "notaOrigemId",
                            noteId,
                            "sku",
                            x.product.get("sku").asString(),
                            "quantidade",
                            reserve.get("quantidade").decimalValue()));
        doc.putAll(
                m(
                        "origem",
                        "XML",
                        "natureza",
                        "RETORNO_MERCADORIA",
                        "xml",
                        xml,
                        "protocolo",
                        x.round + " documento FICTICIO",
                        "coberturas",
                        covers));
        p =
                post("cobertura fiscal ficticia", ps(p) + "/documentos", doc, S, 200)
                        .get("expedicao")
                        .get("pedido");
        var withdraw = x.version(p);
        long reserveB = reserve(p, b);
        withdraw.putAll(
                m(
                        "xmls",
                        List.of(xml),
                        "remanescentes",
                        List.of(m("reservaId", reserveB, "destinos", destinations(storageB)))));
        post("retirada operador negada", ps(p) + "/retirada", withdraw, O, 403);
        var w = post("retirada integral supervisor", ps(p) + "/retirada", withdraw, S, 200);
        var wrep = post("retirada replay", ps(p) + "/retirada", withdraw, S, 200);
        x.assertion("retirada replay igual", w.equals(wrep), w, wrep);
        p = w.get("expedicao").get("pedido");
        x.ids.put("pedidoSaidaId", p.get("id").longValue());
        x.ids.put("baixas", w.get("expedicao").get("baixas"));
        remaining = actual(b);
        stored = remaining;
        x.assertion(
                "remanescente conserva codigo",
                remaining.get("codigo").equals(b.get("codigo")),
                b.get("codigo"),
                remaining.get("codigo"));
        x.assertion(
                "saldo remanescente 40",
                balance().get("fisicoTotal").decimalValue().intValueExact() == 40,
                40,
                balance());
        x.save();
    }

    long reserve(JsonNode p, JsonNode u) {
        for (var r : p.get("reservas"))
            if (r.get("unidadeId").longValue() == u.get("id").longValue())
                return r.get("id").longValue();
        throw new IllegalStateException("Reserva fixture ausente");
    }

    JsonNode separate(JsonNode p, JsonNode u, JsonNode e) throws Exception {
        var d = x.version(p);
        d.putAll(
                m(
                        "reservaId",
                        reserve(p, u),
                        "codigoLido",
                        u.get("codigo").asString(),
                        "revisaoConteudo",
                        get(unit(u) + "/etiqueta").get("versaoConteudo").longValue()));
        p =
                post("leitura etiqueta", "" + ps(p) + "/leituras", d, O, 200)
                        .get("expedicao")
                        .get("pedido");
        d = x.version(p);
        d.put("destinacao", m("reservaId", reserve(p, u), "destinos", destinations(e)));
        return post("separacao fisica", ps(p) + "/separacoes", d, O, 200)
                .get("expedicao")
                .get("pedido");
    }

    void concurrency() throws Exception {
        x.precheck("concorrencia", false);
        var p = order("CONC1", 100);
        var q = order("CONC2", 100);
        var go = new CountDownLatch(1);
        var ready = new CountDownLatch(2);
        var armedFile =
                x.backend
                        .resolve("../orchestracao/.runtime/d26-prumo-concorrencia-pronto.json")
                        .normalize();
        if (!java.nio.file.Files.exists(armedFile))
            throw new IllegalStateException("D26_PRUMO_WITNESS_NAO_ARMADO_ANTES_GATE");
        var armed = x.json.readTree(java.nio.file.Files.readString(armedFile));
        if (!armed.get("estado").asString().equals("PRONTO_WITNESS_LIMITADO_ATIVO")
                || !armed.get("banco").asString().equals("WMS_DEV")
                || !armed.get("loginObservado").asString().equals("WMSDEV")
                || !Instant.parse(armed.get("expiraUtc").asString())
                        .isAfter(Instant.now().plusSeconds(20)))
            throw new IllegalStateException("D26_PRUMO_READINESS_EXPIRADO_OU_DIVERGENTE");
        x.ids.put("prumoReadiness", armed);
        x.save();
        // Porteira SQL somente leitura: duas requisições ficam pendentes antes de liberar o lock.
        try (var gate = x.sqlDataSource().getConnection();
                var pool = Executors.newFixedThreadPool(2)) {
            gate.setAutoCommit(false);
            int spid;
            try (var st = gate.createStatement();
                    var r = st.executeQuery("SELECT @@SPID")) {
                r.next();
                spid = r.getInt(1);
            }
            try (var st =
                    gate.prepareStatement(
                            "SELECT nome FROM wms.cliente WITH (UPDLOCK,HOLDLOCK) WHERE id=?")) {
                st.setLong(1, x.clientId);
                try (var r = st.executeQuery()) {
                    if (!r.next()) throw new IllegalStateException("D26_PORTEIRA_CLIENTE_AUSENTE");
                }
            }
            x.ids.put(
                    "concorrencia",
                    m(
                            "gateSpid",
                            spid,
                            "clienteId",
                            x.clientId,
                            "pedidos",
                            List.of(p.get("id").longValue(), q.get("id").longValue()),
                            "porteira",
                            "SELECT UPDLOCK/HOLDLOCK sem DML; liberacao rollback finally",
                            "aguardandoObservacao",
                            true));
            x.save();
            var futures = new ArrayList<Future<Integer>>();
            for (var order : List.of(p, q))
                futures.add(
                        pool.submit(
                                () -> {
                                    var d = x.version(order);
                                    String jwt = x.token(O, Map.of());
                                    var req =
                                            java.net.http.HttpRequest.newBuilder(
                                                            java.net.URI.create(
                                                                    x.base
                                                                            + ps(order)
                                                                            + "/reserva"))
                                                    .timeout(Duration.ofSeconds(120))
                                                    .header("Content-Type", "application/json")
                                                    .header("Authorization", "Bearer " + jwt)
                                                    .header(
                                                            "X-Request-Id",
                                                            x.round + "-" + UUID.randomUUID())
                                                    .POST(
                                                            java.net.http.HttpRequest.BodyPublishers
                                                                    .ofString(
                                                                            x.json
                                                                                    .writeValueAsString(
                                                                                            d)))
                                                    .build();
                                    ready.countDown();
                                    go.await();
                                    var res =
                                            x.http.send(
                                                    req,
                                                    java.net.http.HttpResponse.BodyHandlers
                                                            .ofString());
                                    synchronized (x) {
                                        x.recordResponse(
                                                "concorrencia reserva barreira",
                                                "POST",
                                                ps(order) + "/reserva",
                                                d,
                                                O,
                                                200,
                                                res,
                                                true);
                                    }
                                    return res.statusCode();
                                }));
            try {
                if (!ready.await(10, TimeUnit.SECONDS))
                    throw new IllegalStateException("D26_BARREIRA_CLIENTE_FALHOU");
                go.countDown();
                var witness =
                        x.backend
                                .resolve(
                                        "../orchestracao/.runtime/d26-prumo-concorrencia-"
                                                + x.round
                                                + ".json")
                                .normalize();
                boolean observed = false;
                long deadline = System.nanoTime() + Duration.ofSeconds(10).toNanos();
                // Diagnóstico apenas da JVM própria: duas stacks de requests já dentro do service,
                // aguardando JDBC enquanto a porteira WMSDEV permanece adquirida.
                while (System.nanoTime() < deadline && !observed) {
                    var pb =
                            new ProcessBuilder(
                                    java.nio.file.Path.of(
                                                    System.getProperty("java.home"),
                                                    "bin",
                                                    "jcmd.exe")
                                            .toString(),
                                    Long.toString(x.app.pid()),
                                    "Thread.print");
                    var prior = new HashMap<>(pb.environment());
                    pb.environment().clear();
                    for (String key : List.of("SystemRoot", "WINDIR", "TEMP", "TMP"))
                        if (prior.containsKey(key)) pb.environment().put(key, prior.get(key));
                    var cmd = pb.start();
                    String dump = EnsaioD26.processOutputLimited(cmd, 2000);
                    if (!x.secret.isEmpty()) dump = dump.replace(x.secret, "[OMITIDO]");
                    int blocked = 0;
                    var threads = new ArrayList<String>();
                    for (String block : dump.split("(?m)^\"")) {
                        if (block.contains("http-nio-")
                                && block.contains("-exec-")
                                && block.contains("PedidoSaidaService.reservar")
                                && block.contains("com.microsoft.sqlserver.jdbc")) {
                            blocked++;
                            threads.add(block.split("\\R", 2)[0]);
                        }
                    }
                    if (blocked >= 2) {
                        observed = true;
                        String file = "d26-" + x.round + "-concorrencia-jvm.txt";
                        java.nio.file.Files.writeString(x.evidence.resolve(file), dump);
                        x.ids.put(
                                "provaConcorrenciaJVM",
                                m(
                                        "jarPid",
                                        x.app.pid(),
                                        "gateSpid",
                                        spid,
                                        "gateMantido",
                                        true,
                                        "threadsServiceReservaJDBC",
                                        threads,
                                        "arquivo",
                                        file));
                    } else Thread.sleep(250);
                }
                boolean sqlObserved = false;
                deadline = System.nanoTime() + Duration.ofSeconds(10).toNanos();
                while (System.nanoTime() < deadline) {
                    if (java.nio.file.Files.exists(witness)) {
                        var w = x.json.readTree(java.nio.file.Files.readString(witness));
                        if (w.get("rodada").asString().equals(x.round)
                                && w.get("gateSpid").intValue() == spid
                                && w.get("duasRequisicoesBloqueadas").booleanValue()) {
                            sqlObserved = true;
                            x.ids.put("provaConcorrenciaSQL", w);
                            break;
                        }
                    }
                    Thread.sleep(250);
                }
                x.ids.put("concorrenciaObservadaJVM", observed);
                x.ids.put("concorrenciaObservada", sqlObserved);
                x.cases.add(
                        m(
                                "caso",
                                "sobreposicao SQL independente",
                                "estado",
                                sqlObserved ? "aprovado" : "nao executado",
                                "motivo",
                                sqlObserved
                                        ? "Prumo armado antes gate observou duas requisicoes"
                                                + " bloqueadas simultaneamente"
                                        : "Sem testemunho SQL independente; JVM separada e par HTTP"
                                                + " nao substituem requisito",
                                "evidencia",
                                witness.getFileName().toString()));
                x.save();
            } finally {
                go.countDown();
                gate.rollback();
                var closed = x.json.valueToTree(x.ids.get("concorrencia"));
                var closedMap = x.json.convertValue(closed, Map.class);
                closedMap.put("aguardandoObservacao", false);
                x.ids.put("concorrencia", closedMap);
                x.save();
            }
            var statuses = new ArrayList<Integer>();
            for (var f : futures) statuses.add(f.get(130, TimeUnit.SECONDS));
            Collections.sort(statuses);
            x.assertion(
                    "concorrencia somente um integral",
                    statuses.equals(List.of(200, 409)),
                    List.of(200, 409),
                    statuses);
        } finally {
            go.countDown();
        }
        for (var order : List.of(p, q)) {
            var state = get(ps(order));
            if (state.get("situacao").asString().equals("RESERVADO"))
                post(
                        "reversao vencedor concorrencia",
                        ps(state) + "/reversao-reserva",
                        x.version(state),
                        S,
                        200);
            else
                post(
                        "cancelar perdedor concorrencia",
                        ps(state) + "/cancelamento",
                        x.version(state),
                        S,
                        200);
        }
        x.assertion(
                "reversao restaura reserva zero",
                balance().get("reservado").decimalValue().signum() == 0,
                0,
                balance());
    }

    String xml(int quantity, String digit) {
        return "<NFe xmlns=\"http://www.portalfiscal.inf.br/nfe\"><infNFe Id=\"NFe"
                + String.format(
                        "%044d",
                        new java.math.BigInteger(
                                1,
                                (x.round + digit)
                                        .getBytes(java.nio.charset.StandardCharsets.UTF_8)))
                + "\" versao=\"4.00\"><ide><mod>55</mod><serie>1</serie><nNF>"
                + (++number)
                + "</nNF><dhEmi>"
                + Instant.now().minusSeconds(3600)
                + "</dhEmi></ide><emit><CNPJ>"
                + x.round
                + "AAA</CNPJ></emit><det nItem=\"1\"><prod><cProd>"
                + x.product.get("sku").asString()
                + "</cProd><uCom>UN</uCom><qCom>"
                + quantity
                + "</qCom><vProd>"
                + quantity * 10
                + ".00</vProd></prod></det></infNFe></NFe>";
    }

    void financeiro() throws Exception {
        x.precheck("financeiro", false);
        var start = LocalDate.now(ZoneOffset.UTC).minusDays(1);
        var end = start.plusDays(1);
        var d = x.command();
        d.putAll(
                m(
                        "codigo",
                        x.round + "SA",
                        "descricao",
                        x.round + " Armazenagem ficticia",
                        "tipo",
                        "ARMAZENAGEM",
                        "unidade",
                        "POSICAO_DIA"));
        var sa = post("servico armazenagem", "/api/v1/servicos-cobranca", d, G, 200);
        d = x.command();
        d.putAll(
                m(
                        "codigo",
                        x.round + "SS",
                        "descricao",
                        x.round + " Saida ficticia",
                        "tipo",
                        "SAIDA",
                        "unidade",
                        "QUANTIDADE_PRODUTO"));
        var ss = post("servico saida", "/api/v1/servicos-cobranca", d, G, 200);
        d = x.command();
        d.putAll(
                m(
                        "clienteId",
                        x.clientId,
                        "armazemId",
                        x.warehouseId,
                        "codigo",
                        x.round + "T",
                        "descricao",
                        x.round + " Tabela ficticia sem homologacao comercial",
                        "tipo",
                        "ESPECIFICA",
                        "vigenciaInicio",
                        start.toString(),
                        "itens",
                        List.of(
                                m(
                                        "servicoId",
                                        sa.get("id").longValue(),
                                        "categoria",
                                        "PALLET",
                                        "preco",
                                        2),
                                m(
                                        "servicoId",
                                        ss.get("id").longValue(),
                                        "categoria",
                                        "",
                                        "preco",
                                        1))));
        post("operacao tabela negada", "/api/v1/tabelas-cobranca", d, O, 403);
        var t = post("tabela ficticia", "/api/v1/tabelas-cobranca", d, G, 200);
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
        var v = post("vinculo tarifa", "/api/v1/vinculos-tabela", d, G, 200);
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
        var contract = post("contrato ficticio", "/api/v1/contratos-cobranca", d, G, 200);
        d = x.command();
        d.putAll(
                m(
                        "clienteId",
                        x.clientId,
                        "armazemId",
                        x.warehouseId,
                        "servicoId",
                        ss.get("id").longValue(),
                        "origem",
                        "MANUAL",
                        "pedidoSaidaId",
                        x.ids.get("pedidoSaidaId"),
                        "produtoId",
                        x.product.get("id").longValue(),
                        "categoria",
                        "",
                        "cotas",
                        List.of(),
                        "criterioRateio",
                        x.round + " baixas ficticias por nota"));
        var f = post("fato saida 60", "/api/v1/fatos-servico", d, S, 200);
        var fr = post("fato replay", "/api/v1/fatos-servico", d, S, 200);
        x.assertion("fato replay nao duplica", f.equals(fr), f, fr);
        x.assertion(
                "fato saida quantidade60",
                f.get("quantidade").decimalValue().intValueExact() == 60,
                60,
                f.get("quantidade"));
        d.put("operacaoId", UUID.randomUUID().toString());
        d.put("origem", "SUGESTAO");
        post("mesma execucao nao duplica por UUID", "/api/v1/fatos-servico", d, S, 409);
        var current = calculate(end, end.plusDays(1));
        x.assertion(
                "calculo atual servico saida60",
                current.get("memoria")
                                .get("servicos")
                                .get(0)
                                .get("valor")
                                .decimalValue()
                                .intValueExact()
                        == 60,
                60,
                current.get("memoria").get("servicos"));
        var historical = calculate(start, end);
        x.assertion(
                "periodo anterior sem permanencia ou saida",
                historical.get("total").decimalValue().signum() == 0,
                0,
                historical.get("total"));
        d = x.command();
        d.put("calculoId", historical.get("id").longValue());
        post("preparar supervisor negado", "/api/v1/fechamentos-cobranca", d, S, 403);
        var close =
                post(
                        "fechamento periodo encerrado ficticio",
                        "/api/v1/fechamentos-cobranca",
                        d,
                        G,
                        200);
        String route =
                "/api/v1/fechamentos-cobranca/" + close.get("fechamento").get("id").longValue();
        var proof =
                x.call(
                        "demonstrativo imutavel",
                        "GET",
                        route + "/versoes/1/demonstrativo",
                        null,
                        S,
                        200);
        d = x.version(close.get("fechamento"));
        d.put("numero", 1);
        post("aprovacao supervisor negada", route + "/aprovacao", d, S, 403);
        close = post("aprovacao gestor", route + "/aprovacao", d, G, 200);
        d = x.version(close.get("fechamento"));
        d.putAll(
                m(
                        "numero",
                        1,
                        "layoutVersao",
                        1,
                        "arquivoHash",
                        close.get("versao").get("conteudoHash").asString(),
                        "destinoReferencia",
                        x.round + " registro manual FICTICIO sem envio",
                        "entregueEm",
                        Instant.now().toString()));
        close =
                post(
                        "registro entrega manual ficticio sem integracao",
                        route + "/entregas",
                        d,
                        G,
                        200);
        d = x.version(close.get("fechamento"));
        d.putAll(
                m(
                        "numero",
                        1,
                        "emissorDocumento",
                        x.round,
                        "referenciaExterna",
                        x.round + "NFSE-FICTICIA",
                        "emitidaEm",
                        Instant.now().toString(),
                        "fonte",
                        x.round + " documento ficticio sem emissao",
                        "conferidaPor",
                        x.round + " Gestor ficticio"));
        close =
                post(
                        "referencia nfse FICTICIA sem emissao",
                        route + "/referencias-nfse",
                        d,
                        G,
                        200);
        x.assertion(
                "demonstrativo preservado",
                proof.equals(
                        x.call(
                                "demonstrativo apos transicoes",
                                "GET",
                                route + "/versoes/1/demonstrativo",
                                null,
                                S,
                                200)),
                proof,
                "mesmo documento");
        x.ids.put("contrato", contract);
        x.ids.put("tabela", t);
        x.ids.put("vinculo", v);
        x.ids.put("fatoServico", f);
        x.ids.put("calculoAtual", current);
        x.ids.put("calculoHistorico", historical);
        x.ids.put("fechamento", close);
        x.save();
    }

    JsonNode calculate(LocalDate begin, LocalDate end) throws Exception {
        var d = x.command();
        d.putAll(
                m(
                        "clienteId",
                        x.clientId,
                        "armazemId",
                        x.warehouseId,
                        "periodoInicio",
                        begin.toString(),
                        "periodoFim",
                        end.toString()));
        var a =
                post(
                        "calculo ficticio " + begin + "/" + end,
                        "/api/v1/calculos-cobranca",
                        d,
                        S,
                        200);
        var b = post("calculo replay", "/api/v1/calculos-cobranca", d, S, 200);
        x.assertion("calculo replay snapshot", a.equals(b), a, b);
        return a;
    }

    void avariaRetornoXml() throws Exception {
        x.precheck("avaria-xml-retorno", false);
        var quarantine = address("QUA", "QUARENTENA");
        remaining = actual(remaining);
        var d = x.command();
        d.putAll(
                m(
                        "versaoUnidade",
                        remaining.get("versao").longValue(),
                        "quantidade",
                        2,
                        "ocorridaEm",
                        Instant.now().minusMillis(10).toString(),
                        "destinos",
                        destinations(quarantine)));
        String route =
                "/api/v1/estoque/unidades/" + remaining.get("codigo").asString() + "/avarias";
        post("avaria operador negada", route, d, O, 403);
        var result = post("avaria registrada bloqueia saldo", route, d, S, 200);
        var damaged = result.get("avaria");
        remaining = actual(remaining);
        var denied = x.command();
        denied.putAll(
                m(
                        "clienteId",
                        x.clientId,
                        "armazemId",
                        x.warehouseId,
                        "referencia",
                        x.round + "AVARIA",
                        "itens",
                        List.of(m("produtoId", x.product.get("id").longValue(), "quantidade", 1))));
        post("avaria quarentena nao atende criacao saida", "/api/v1/pedidos-saida", denied, O, 409);
        x.assertion(
                "saldo disponivel avaria zero",
                balance().get("disponivel").decimalValue().signum() == 0,
                0,
                balance());
        tratarAvariaXmlRetorno(damaged);
    }

    void tratarAvariaXmlRetorno(JsonNode damaged) throws Exception {
        String route =
                "/api/v1/estoque/unidades/" + remaining.get("codigo").asString() + "/avarias";
        var d = x.version(damaged);
        d.put("responsabilidade", "PROPRIETARIO");
        post(
                "responsabilidade supervisor negada",
                route + "/" + damaged.get("id").longValue() + "/responsabilidade",
                d,
                S,
                403);
        damaged =
                post(
                                "responsabilidade gestor",
                                route + "/" + damaged.get("id").longValue() + "/responsabilidade",
                                d,
                                G,
                                200)
                        .get("avaria");
        d = x.version(damaged);
        d.putAll(
                m(
                        "versaoUnidade",
                        remaining.get("versao").longValue(),
                        "destinos",
                        destinations(storageB)));
        post(
                "reparo avaria supervisor",
                route + "/" + damaged.get("id").longValue() + "/reparo",
                d,
                S,
                200);
        remaining = actual(remaining);
        x.ids.put("avariaId", damaged.get("id").longValue());
        x.save();
        xmlRetorno();
    }

    void xmlRetorno() throws Exception {
        // XML fictício próprio importa documento; não cria saldo ou chegada.
        long p =
                post(
                                "entrada XML",
                                "/api/v1/pedidos-entrada",
                                m(
                                        "clienteId",
                                        x.clientId,
                                        "armazemId",
                                        x.warehouseId,
                                        "referencia",
                                        x.round + "XML"),
                                O,
                                201)
                        .get("id")
                        .longValue();
        var before = balance().get("fisicoTotal");
        String customerDoc =
                x.ids.containsKey("clienteDocumentoFiscal")
                        ? x.ids.get("clienteDocumentoFiscal").toString()
                        : x.round + "CCC";
        post(
                "XML entrada ficticio",
                pe(p) + "/notas/xml",
                m("versao", pv(p), "xml", xml(5, "2").replace(x.round + "AAA", customerDoc)),
                O,
                200);
        x.assertion(
                "XML nao aumenta fisico",
                before.equals(balance().get("fisicoTotal")),
                before,
                balance().get("fisicoTotal"));
        post(
                "conferencia XML",
                pe(p) + "/iniciar-conferencia",
                m("versao", pv(p), "motivo", x.round + " conferencia XML ficticia"),
                O,
                200);
        arrival(p, 4, Instant.now().minusSeconds(10));
        var d =
                m(
                        "versao",
                        pv(p),
                        "motivo",
                        x.round + " divergencia identificada",
                        "aceitarDivergencias",
                        false);
        post("divergencia exige aceite supervisor", pe(p) + "/efetivacao", d, S, 409);
        d.put("aceitarDivergencias", true);
        post("liberacao real divergente supervisor", pe(p) + "/efetivacao", d, S, 200);
        x.ids.put("entradaXmlId", p);
        // Retorno interno: nova reserva/separação e retorno sem baixa física.
        var out = order("RET-INTERNO", 10);
        out =
                post("reserva para retorno", ps(out) + "/reserva", x.version(out), O, 200)
                        .get("pedido");
        var sep = address("SEPR", "SEPARACAO");
        out = separate(out, remaining, sep);
        d = x.version(out);
        d.put(
                "unidades",
                List.of(
                        m(
                                "reservaId",
                                reserve(out, remaining),
                                "destinos",
                                destinations(storageB))));
        var returned = post("retorno interno sem baixa", ps(out) + "/retorno-interno", d, S, 200);
        out = returned.get("expedicao").get("pedido");
        x.assertion(
                "retorno interno encerra pedido e reservas",
                out.get("situacao").asString().equals("CANCELADO")
                        && balance().get("reservado").decimalValue().signum() == 0,
                "CANCELADO/reserva0",
                out);
        post(
                "reversao apos retorno ja cancelado recusada",
                ps(out) + "/reversao-reserva",
                x.version(out),
                S,
                409);
        remaining = actual(remaining);
        x.ids.put("pedidoRetornoId", out.get("id").longValue());
        x.save();
    }

    void contagemContingencia() throws Exception {
        x.precheck("contagem-contingencia", false);
        remaining = actual(remaining);
        String when = Instant.now().minusMillis(10).toString();
        var observed = x.command();
        observed.putAll(
                m(
                        "codigoUnidade",
                        remaining.get("codigo").asString(),
                        "versaoUnidade",
                        remaining.get("versao").longValue(),
                        "contado",
                        40,
                        "observadoEm",
                        when));
        var line = x.command();
        line.putAll(
                m(
                        "identidadeFato",
                        x.round + "CONT",
                        "clienteId",
                        x.clientId,
                        "armazemId",
                        x.warehouseId,
                        "tipo",
                        "CONTAGEM",
                        "ocorridaEm",
                        when,
                        "operador",
                        x.round + " Operador ficticio",
                        "fonte",
                        x.round + " prova fisica ficticia",
                        "efeitoRegistradoNoWms",
                        false,
                        "dependencias",
                        List.of(),
                        "dados",
                        m("codigoUnidade", remaining.get("codigo").asString(), "dados", observed)));
        var l =
                x.ids.containsKey("contingenciaPendente")
                        ? x.call(
                                "consulta contingencia supervisor",
                                "GET",
                                "/api/v1/contingencias/"
                                        + x.json.valueToTree(x.ids.get("contingenciaPendente"))
                                                .get("id")
                                                .longValue(),
                                null,
                                S,
                                200)
                        : post(
                                "linha contingencia contagem",
                                "/api/v1/contingencias",
                                line,
                                S,
                                200);
        var command =
                x.ids.containsKey("comandoConciliacaoPendente")
                        ? x.json.convertValue(
                                x.json.valueToTree(x.ids.get("comandoConciliacaoPendente")),
                                Map.class)
                        : x.version(l);
        command.put("modo", "EXECUTAR");
        var conc =
                post(
                        "conciliar contagem HTTP",
                        "/api/v1/contingencias/" + l.get("id").longValue() + "/conciliar",
                        command,
                        S,
                        200);
        var replay =
                post(
                        "conciliacao replay",
                        "/api/v1/contingencias/" + l.get("id").longValue() + "/conciliar",
                        command,
                        S,
                        200);
        x.assertion("contingencia replay conserva efeito", conc.equals(replay), conc, replay);
        remaining = actual(remaining);
        var d = x.command();
        d.putAll(
                m(
                        "codigoUnidade",
                        remaining.get("codigo").asString(),
                        "versaoUnidade",
                        remaining.get("versao").longValue(),
                        "contado",
                        35,
                        "observadoEm",
                        Instant.now().minusMillis(10).toString()));
        var count = post("contagem diferenca identificada", "/api/v1/contagens", d, O, 200);
        var adjust = x.command();
        adjust.putAll(
                m(
                        "revisao",
                        count.get("revisao").intValue(),
                        "versaoUnidade",
                        remaining.get("versao").longValue(),
                        "causa",
                        x.round + " diferenca ficticia conferida",
                        "destino",
                        x.round + " destino interno ficticio",
                        "comprovacao",
                        x.round + " prova fisica ficticia",
                        "origens",
                        List.of(
                                m(
                                        "entradaId",
                                        count.get("origens").get(0).get("entradaId").longValue(),
                                        "delta",
                                        -5))));
        post(
                "aplicar contagem operador negado",
                "/api/v1/contagens/" + count.get("id").longValue() + "/aplicar",
                adjust,
                O,
                403);
        var applied =
                post(
                        "ajuste fisico supervisor",
                        "/api/v1/contagens/" + count.get("id").longValue() + "/aplicar",
                        adjust,
                        S,
                        200);
        x.assertion(
                "saldo ajuste35 unitizado +4 entrada XML",
                balance().get("fisicoTotal").decimalValue().intValueExact() == 39
                        && balance().get("fisicoUnitizado").decimalValue().intValueExact() == 35,
                39,
                balance());
        x.ids.put("contingencia", conc);
        x.ids.put("contagem", applied);
        x.save();
    }

    void auditoriaListas() throws Exception {
        x.precheck("auditoria-final", false);
        String ctx =
                "?clienteId=" + x.clientId + "&armazemId=" + x.warehouseId + "&pagina=0&tamanho=10";
        for (String route :
                List.of(
                        "pedidos-entrada",
                        "pedidos-saida",
                        "estoque",
                        "contagens",
                        "contingencias",
                        "indicadores-estoque"))
            x.call("lista/filtro " + route, "GET", "/api/v1/" + route + ctx, null, O, 200);
        x.call(
                "auditoria operador negada",
                "GET",
                "/api/v1/auditoria?tipo=CLIENTE&registroId=" + x.clientId,
                null,
                O,
                403);
        var audit =
                x.call(
                        "auditoria gestor cliente",
                        "GET",
                        "/api/v1/auditoria?tipo=CLIENTE&registroId=" + x.clientId,
                        null,
                        G,
                        200);
        x.assertion(
                "criacao/alteracao auditadas",
                audit.get("itens").size() >= 2,
                ">=2",
                audit.get("itens").size());
        x.ids.put("saldoFinal", balance());
        x.save();
    }
}
