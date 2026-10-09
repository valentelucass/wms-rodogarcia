import tools.jackson.databind.JsonNode;

import java.time.*;
import java.util.*;
import java.util.concurrent.*;

final class JornadasD29 {
    static Map<String, Object> m(Object... pairs) {
        return EnsaioD29.m(pairs);
    }

    final EnsaioD29 x;
    int number = 100;
    final String G = "GESTOR", S = "SUPERVISOR", O = "OPERACAO";

    JornadasD29(EnsaioD29 x) {
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
                                x.nextDocumentNumber()),
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
                        x.nextDocumentNumber(),
                        "emissao",
                        LocalDate.now().toString(),
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

    long entryId, noteId;
    JsonNode storageA, storageB;

    List<JsonNode> prepararRecebimento() throws Exception {
        x.precheck("recebimento", false);
        entryId = entry("ENT", 100);
        x.ids.put("pedidoEntradaId", entryId);
        arrival(entryId, 50, Instant.now());
        arrival(entryId, 50, Instant.now());
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
}
