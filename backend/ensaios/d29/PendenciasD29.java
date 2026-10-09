import tools.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;

/** Casos novos D29; não executa jornadas/mutações históricas D26. */
final class PendenciasD29 {
    final EnsaioD29 x;
    final JornadasD29 j;
    final String G = "GESTOR", S = "SUPERVISOR", O = "OPERACAO";

    PendenciasD29(EnsaioD29 x) {
        this.x = x;
        this.j = new JornadasD29(x);
    }

    static Map<String, Object> m(Object... p) {
        return EnsaioD29.m(p);
    }

    String ctx() {
        return "?clienteId=" + x.clientId + "&armazemId=" + x.warehouseId;
    }

    JsonNode get(String name, String route, String role) throws Exception {
        return x.call(name, "GET", route, null, role, 200);
    }

    JsonNode post(String name, String route, Object body, String role, int expected)
            throws Exception {
        return x.call(name, "POST", route, body, role, expected);
    }

    void run(String mode) throws Exception {
        if (x.ids.containsKey("retomadaDe")) {
            x.product =
                    get(
                            "produto retomada própria",
                            "/api/v1/produtos/" + x.ids.get("produtoId"),
                            O);
            x.pack =
                    get(
                            "DUN retomada própria",
                            "/api/v1/embalagens/" + x.ids.get("embalagemId"),
                            O);
        } else j.cadastros();
        switch (mode) {
            case "xml-ajuste" -> {
                xml();
                if (System.getenv("WMS_D29_V10_CHECKSUM") != null) ajuste();
                else
                    x.ids.put(
                            "ajustePendente",
                            "V10 ainda não atestada; fixtures novas preparadas, nenhuma aplicação");
            }
            case "ajuste" -> ajuste();
            case "financeiro" -> financeiro();
            case "financeiro-validar" -> financeiroValidar();
            case "financeiro-limites" -> financeiroLimites();
            case "xml-validar" -> xmlValidar();
            case "xml-limites" -> xmlLimites();
            case "variantes" -> new VariantesD29(this).run();
            case "variantes-financeiro" -> new VariantesD29(this).retomarFinanceiro();
            case "variantes-validar" -> new VariantesD29(this).validarFinal();
            case "listas-estoque" -> listasEstoque();
            case "listas" -> listas();
            case "concorrencia" -> throw new IllegalStateException("D29_NATIVE_NAO_EXECUTAVEL");
            case "get-final" -> {
                x.precheck("GET-final-familia-existente", false);
                x.ids.put("complementoLeituraDe", x.ids.get("retomadaDe"));
                var ids = new LinkedHashSet<Long>();
                var prior = x.json.readTree(java.nio.file.Files.readAllBytes(x.evidence.resolve("d29-"+x.ids.get("retomadaDe")+"-http.json")));
                for (var c : prior.get("cases")) {
                    String route = c.path("rota").asString();
                    if (c.path("metodo").asString().equals("GET") && c.path("actual").asInt() == 200
                            && route.matches("/api/v1/pedidos-entrada/[0-9]+")) {
                        long id = Long.parseLong(route.substring(route.lastIndexOf('/')+1));
                        if (c.path("resposta").path("pedido").path("id").asLong() == id) ids.add(id);
                    }
                }
                if (x.ids.containsKey("pedidoEntradaId")) ids.add(((Number)x.ids.get("pedidoEntradaId")).longValue());
                if (x.ids.containsKey("transformacaoTemporal")) {
                    var temporal = x.json.valueToTree(x.ids.get("transformacaoTemporal"));
                    ids.add(temporal.get("pedidoEntradaId").longValue());
                }
                var finals = new ArrayList<JsonNode>();
                for (long id : ids) {
                    var response = get("D29 GET FINAL pedido entrada atual " + id, j.pe(id), O);
                    x.assertion("D29 GET FINAL confirma ID do pedido " + id, response.path("pedido").path("id").asLong()==id, id, response.path("pedido").path("id"));
                    finals.add(response);
                }
                x.ids.put("pedidosEntradaGETFinal", finals);
                if (x.ids.containsKey("concorrenciaPedidos")) {
                    var saidas = new ArrayList<JsonNode>();
                    for (var id : (List<?>)x.ids.get("concorrenciaPedidos")) {
                        var response = get("D29 GET FINAL saida apos replay " + id, "/api/v1/pedidos-saida/" + id, O);
                        x.assertion("D29 GET FINAL saida confirma ID " + id, response.path("id").asLong()==((Number)id).longValue(),id,response.path("id"));
                        saidas.add(response);
                    }
                    x.ids.put("pedidosSaidaGETFinalAposReplay", saidas);
                }
                for (String route : List.of("/api/v1/clientes/"+x.clientId,"/api/v1/armazens/"+x.warehouseId)) get("D29 GET FINAL cadastro atual",route,G);
                x.save();
            }
            default -> throw new IllegalStateException("D29_ETAPA_NAO_IMPLEMENTADA");
        }
    }

    Map<String, Object> xmlPayload(long numero, String a, String b) {
        String key = Long.toUnsignedString(UUID.randomUUID().getMostSignificantBits());
        key = (key.repeat(4)).substring(0, 44);
        String det =
                "<det nItem='%s'><prod><cProd>"
                        + x.product.get("sku").asString()
                        + "</cProd><uCom>UN</uCom><qCom>%s</qCom><vProd>10</vProd></prod></det>";
        String xml =
                "<NFe xmlns='http://www.portalfiscal.inf.br/nfe'><infNFe versao='4.00' Id='NFe"
                        + key
                        + "'><ide><mod>55</mod><serie>1</serie><nNF>"
                        + numero
                        + "</nNF><dhEmi>"
                        + Instant.now()
                        + "</dhEmi></ide><emit><CNPJ>12345678000199</CNPJ></emit>"
                        + det.formatted(1, a)
                        + det.formatted(2, b)
                        + "</infNFe></NFe>";
        var d = x.command();
        d.putAll(m("clienteId", x.clientId, "armazemId", x.warehouseId, "xml", xml));
        return d;
    }

    void xml() throws Exception {
        x.precheck("rn22", false);
        var units = j.prepararRecebimento();
        x.ids.put("unidades", units);
        var orders = new ArrayList<Long>();
        long number = j.number;
        for (String role : List.of(G, S, O)) {
            var d = xmlPayload(number = x.nextDocumentNumber(), "2", "3");
            var a = post("RN22 XML integral " + role, "/api/v1/pedidos-saida/xml", d, role, 201);
            var b = post("RN22 replay " + role, "/api/v1/pedidos-saida/xml", d, role, 201);
            x.callToken(
                    "RN22 replay sem alcance " + role,
                    "POST",
                    "/api/v1/pedidos-saida/xml",
                    d,
                    O,
                    x.token(O, m("wms_clientes", List.of())),
                    403);
            orders.add(a.get("pedido").get("id").longValue());
            x.ids.put("pedidosXml", orders);
            x.save();
            x.assertion(
                    "RN22 origem/linhas/soma/replay " + role,
                    a.equals(b)
                            && a.get("documento").get("numero").longValue() == number
                            && a.get("documento").get("itens").size() == 2
                            && a.get("pedido").get("itens").size() == 1
                            && a.get("pedido")
                                            .get("itens")
                                            .get(0)
                                            .get("quantidade")
                                            .decimalValue()
                                            .compareTo(new BigDecimal("5"))
                                    == 0,
                    "nota/2 linhas/5 integral/mesmo snapshot",
                    a);
            var live =
                    get(
                            "RN22 GET pedido " + role,
                            "/api/v1/pedidos-saida/" + orders.getLast(),
                            role);
            x.assertion(
                    "RN22 GET sem reserva " + role,
                    live.get("reservas").isEmpty()
                            && live.get("situacao").asString().equals("RASCUNHO"),
                    "RASCUNHO/reservas0",
                    live);
            var duplicate = new LinkedHashMap<>(d);
            duplicate.put("operacaoId", UUID.randomUUID().toString());
            var refused =
                    post(
                            "RN22 nota duplicada nova UUID " + role,
                            "/api/v1/pedidos-saida/xml",
                            duplicate,
                            role,
                            409);
            code("RN22 duplicidade exata " + role, refused, "REFERENCIA_DUPLICADA");
            var changed = new LinkedHashMap<>(d);
            changed.put("xml", d.get("xml").toString().replace("<qCom>2</qCom>", "<qCom>1</qCom>"));
            code(
                    "RN22 UUID divergente " + role,
                    post(
                            "RN22 recusa replay alterado " + role,
                            "/api/v1/pedidos-saida/xml",
                            changed,
                            role,
                            409),
                    "OPERACAO_DIVERGENTE");
        }
        var valid = xmlPayload(number = x.nextDocumentNumber(), "2", "3");
        post("RN22 anônimo", "/api/v1/pedidos-saida/xml", valid, null, 401);
        for (String scope : List.of("wms_clientes", "wms_armazens"))
            x.callToken(
                    "RN22 alcance " + scope,
                    "POST",
                    "/api/v1/pedidos-saida/xml",
                    valid,
                    O,
                    x.token(O, m(scope, List.of())),
                    403);
        x.callToken(
                "RN22 token inválido",
                "POST",
                "/api/v1/pedidos-saida/xml",
                valid,
                O,
                "invalido",
                401);
        for (var item :
                List.of(
                        m(
                                "name",
                                "XXE arquivo",
                                "xml",
                                "<!DOCTYPE NFe [<!ENTITY x SYSTEM 'file:///d29-ficticio'>]>"
                                        + valid.get("xml")),
                        m(
                                "name",
                                "DTD rede recusada",
                                "xml",
                                "<!DOCTYPE NFe SYSTEM 'http://127.0.0.1:9/d29-nao-acessar'>"
                                        + valid.get("xml")),
                        m(
                                "name",
                                "SKU inexistente",
                                "xml",
                                valid.get("xml")
                                        .toString()
                                        .replace(
                                                x.product.get("sku").asString(),
                                                x.round + "NAOEXISTE")),
                        m(
                                "name",
                                "unidade incompatível",
                                "xml",
                                valid.get("xml")
                                        .toString()
                                        .replace("<uCom>UN</uCom>", "<uCom>KG</uCom>")),
                        m(
                                "name",
                                "quantidade negativa",
                                "xml",
                                valid.get("xml")
                                        .toString()
                                        .replace("<qCom>2</qCom>", "<qCom>-1</qCom>")),
                        m(
                                "name",
                                "precisão incompatível",
                                "xml",
                                valid.get("xml")
                                        .toString()
                                        .replace("<qCom>2</qCom>", "<qCom>2.5</qCom>")))) {
            var bad = new LinkedHashMap<>(valid);
            bad.put("xml", item.get("xml"));
            post("RN22 " + item.get("name"), "/api/v1/pedidos-saida/xml", bad, O, 400);
        }
        code(
                "RN22 integralidade insuficiência",
                post(
                        "RN22 saldo insuficiente rollback",
                        "/api/v1/pedidos-saida/xml",
                        xmlPayload(number = x.nextDocumentNumber(), "60", "41"),
                        O,
                        409),
                "SALDO_INSUFICIENTE");
        var list = get("RN22 pedidos após recusas", "/api/v1/pedidos-saida" + ctx(), O);
        x.assertion(
                "RN22 rollback pedidos/saldo",
                list.get("totalItens").longValue() == 3
                        && j.balance().get("fisicoTotal").decimalValue().intValueExact() == 100
                        && j.balance().get("reservado").decimalValue().signum() == 0,
                "3 pedidos/100 físico/0 reservado",
                list);
        paginas("RN22 pedidos", "/api/v1/pedidos-saida" + ctx(), O, 3);
        for (long id : orders)
            paginas(
                    "RN22 auditoria " + id,
                    "/api/v1/auditoria?tipo=PEDIDO_SAIDA&registroId=" + id,
                    G,
                    2);
        x.ids.put("pedidosXml", orders);
        x.save();
    }

    void code(String name, JsonNode r, String expected) throws Exception {
        x.assertion(
                name, r.has("codigo") && r.get("codigo").asString().equals(expected), expected, r);
    }

    void paginas(String name, String route, String role, int count) throws Exception {
        String sep = route.contains("?") ? (route.endsWith("?") ? "" : "&") : "?";
        var a = get(name + " página0", route + sep + "pagina=0&tamanho=1", role);
        var b = get(name + " página1", route + sep + "pagina=1&tamanho=1", role);
        boolean valid =
                a.get("totalItens").longValue() == count
                        && a.get("totalPaginas").intValue() == count
                        && a.get("pagina").intValue() == 0
                        && b.get("pagina").intValue() == 1
                        && a.get("tamanho").intValue() == 1
                        && a.get("itens").size() == 1
                        && b.get("itens").size() == 1
                        && a.get("itens").get(0).get("id").longValue()
                                < b.get("itens").get(0).get("id").longValue();
        x.assertion(
                name + " conteúdo/metadados/ordem/sem repetição",
                valid,
                "páginas0/1,1 item,crescente,total=" + count,
                m("p0", a, "p1", b));
        var empty =
                get(
                        name + " página após fim",
                        route + sep + "pagina=" + count + "&tamanho=1",
                        role);
        x.assertion(
                name + " vazio preserva total",
                empty.get("itens").isEmpty() && empty.get("totalItens").longValue() == count,
                count,
                empty);
        x.call(name + " limite101", "GET", route + sep + "tamanho=101", null, role, 400);
        x.call(name + " página negativa", "GET", route + sep + "pagina=-1", null, role, 400);
    }

    Map<String, Object> reading(JsonNode u, int counted) {
        var d = x.command();
        d.putAll(
                m(
                        "codigoUnidade",
                        u.get("codigo").asString(),
                        "versaoUnidade",
                        u.get("versao").longValue(),
                        "contado",
                        counted,
                        "observadoEm",
                        Instant.now().toString()));
        return d;
    }

    Map<String, Object> application(JsonNode c, JsonNode u) {
        var d = x.command();
        d.putAll(
                m(
                        "revisao",
                        c.get("revisao").intValue(),
                        "versaoUnidade",
                        u.get("versao").longValue(),
                        "causa",
                        x.round + " divergência conferida",
                        "destino",
                        x.round + " destino fictício identificado",
                        "comprovacao",
                        x.round + " comprovação física fictícia",
                        "origens",
                        List.of(
                                m(
                                        "entradaId",
                                        c.get("origens").get(0).get("entradaId").longValue(),
                                        "delta",
                                        c.get("diferenca").decimalValue()))));
        return d;
    }

    void ajuste() throws Exception {
        if (System.getenv("WMS_D29_V10_CHECKSUM") == null)
            throw new IllegalStateException("D29_V10_NAO_ATESTADA");
        x.precheck("ajuste-v10", false);
        var units = x.json.valueToTree(x.ids.get("unidades"));
        var counts = new ArrayList<Long>();
        int i = 0;
        for (var saved : units) {
            var u = j.actual(saved);
            int quantity = i++ == 0 ? 45 : 0;
            boolean resume = x.ids.containsKey("retomadaDe");
            JsonNode c;
            Map<String, Object> d = reading(u, quantity);
            if (resume) {
                var previous =
                        x.json.readTree(
                                java.nio.file.Files.readString(
                                        x.evidence.resolve(
                                                "d29-" + x.ids.getOrDefault("retomadaDe", x.round) + "-http.json")));
                c =
                        get(
                                "AC13 revisão preservada da tentativa anterior " + quantity,
                                "/api/v1/contagens/"
                                        + previous.get("ids")
                                                .get("contagens")
                                                .get(i - 1)
                                                .longValue(),
                                O);
                if (c.get("situacao").asString().equals("APLICADA")) {
                    counts.add(c.get("id").longValue());
                    x.assertion(
                            "AC13 ajuste45 já concluído, somente GET",
                            u.get("quantidade").decimalValue().intValueExact() == 45,
                            "45 preservado sem remutação",
                            c);
                    continue;
                }
            } else c = post("AC13 leitura " + quantity, "/api/v1/contagens", d, O, 200);
            counts.add(c.get("id").longValue());
            x.ids.put("contagens", counts);
            x.save();
            if (!resume)
                x.assertion(
                        "AC13 leitura replay " + quantity,
                        c.equals(
                                post(
                                        "AC13 replay leitura " + quantity,
                                        "/api/v1/contagens",
                                        d,
                                        O,
                                        200)),
                        "mesmo snapshot",
                        c);
            u = j.actual(saved);
            var apply = application(c, u);
            String route = "/api/v1/contagens/" + c.get("id").longValue() + "/aplicar";
            if (resume) {
                var previous =
                        x.json.readTree(
                                java.nio.file.Files.readString(
                                        x.evidence.resolve(
                                                "d29-" + x.ids.getOrDefault("retomadaDe", x.round) + "-http.json")));
                for (var prior : previous.get("cases"))
                    if (prior.path("caso").asString().equals("AC13 aplicação " + quantity))
                        apply =
                                x.json.convertValue(
                                        prior.get("payload"),
                                        new tools.jackson.core.type.TypeReference<
                                                Map<String, Object>>() {});
            }
            post("AC13 Operação negada " + quantity, route, apply, O, 403);
            x.callToken(
                    "AC13 alcance negado " + quantity,
                    "POST",
                    route,
                    apply,
                    S,
                    x.token(S, m("wms_clientes", List.of())),
                    403);
            var bad = new LinkedHashMap<>(apply);
            bad.put(
                    "origens",
                    List.of(
                            m(
                                    "entradaId",
                                    c.get("origens").get(0).get("entradaId").longValue(),
                                    "delta",
                                    -1)));
            post("AC13 soma divergente rollback " + quantity, route, bad, S, 400);
            var before =
                    get(
                            "AC13 após recusa " + quantity,
                            "/api/v1/contagens/" + c.get("id").longValue(),
                            O);
            x.assertion(
                    "AC13 recusa conserva pendência " + quantity,
                    before.get("situacao").asString().equals("PENDENTE")
                            && j.actual(saved).get("quantidade").decimalValue().intValueExact()
                                    == 50,
                    "PENDENTE/físico50",
                    before);
            var applied = post("AC13 aplicação " + quantity, route, apply, S, 200);
            x.assertion(
                    "AC13 aplicação/replay " + quantity,
                    applied.get("situacao").asString().equals("APLICADA")
                            && applied.equals(
                                    post(
                                            "AC13 replay aplicação " + quantity,
                                            route,
                                            apply,
                                            S,
                                            200)),
                    "APLICADA/mesmo snapshot",
                    applied);
            var changed = new LinkedHashMap<>(apply);
            changed.put("comprovacao", x.round + " prova divergente");
            code(
                    "AC13 replay alterado " + quantity,
                    post("AC13 divergência " + quantity, route, changed, S, 409),
                    "OPERACAO_DIVERGENTE");
            post("AC13 replay ainda não permite Operação " + quantity, route, apply, O, 403);
            var detail =
                    get(
                            "AC13 GET estado " + quantity,
                            "/api/v1/contagens/" + c.get("id").longValue(),
                            O);
            var live = j.actual(saved);
            var stock = get("AC13 GET posições " + quantity, j.unit(saved) + "/estoque", O);
            x.assertion(
                    "AC13 físico/origens/atividade " + quantity,
                    live.get("quantidade").decimalValue().intValueExact() == quantity
                            && live.get("ativa").booleanValue() == (quantity > 0)
                            && (quantity != 0 || stock.get("posicoes").isEmpty()),
                    "quantidade=" + quantity + "/posição liberada se0",
                    m("contagem", detail, "unidade", live, "estoque", stock));
            get("AC13 movimentos " + quantity, j.unit(saved) + "/movimentos", O);
            get(
                    "AC13 auditoria " + quantity,
                    "/api/v1/auditoria?tipo=CONTAGEM_ESTOQUE&registroId=" + c.get("id").longValue(),
                    G);
        }
        x.assertion(
                "AC13 saldo45 sem reserva",
                j.balance().get("fisicoTotal").decimalValue().intValueExact() == 45
                        && j.balance().get("reservado").decimalValue().signum() == 0,
                "45 físico/0 reservado",
                j.balance());
        var active = j.actual(units.get(0));
        var order = j.order("RESCONT", 40);
        var reserve =
                post(
                                "AC13 reserva integral40 antes contagem",
                                j.ps(order) + "/reserva",
                                x.version(order),
                                O,
                                200)
                        .get("pedido");
        active = j.actual(units.get(0));
        var r =
                post(
                        "AC13 observação35 abaixo reserva40",
                        "/api/v1/contagens",
                        reading(active, 35),
                        O,
                        200);
        counts.add(r.get("id").longValue());
        x.ids.put("contagens", counts);
        x.ids.put("pedidoReservaContagemId", order.get("id").longValue());
        x.save();
        String rr = "/api/v1/contagens/" + r.get("id").longValue() + "/aplicar";
        code(
                "AC13 reserva preservada recusa exata",
                post(
                        "AC13 ajuste não libera reserva silenciosa",
                        rr,
                        application(r, j.actual(units.get(0))),
                        S,
                        409),
                "PENDENTE_RESERVA");
        x.assertion(
                "AC13 rollback físico45/reservado40",
                j.balance().get("fisicoTotal").decimalValue().intValueExact() == 45
                        && j.balance().get("reservado").decimalValue().intValueExact() == 40,
                "45/40",
                j.balance());
        post(
                "AC13 reversão explícita Supervisor",
                j.ps(reserve) + "/reversao-reserva",
                x.version(reserve),
                S,
                200);
        var old = application(r, j.actual(units.get(0)));
        old.put("revisao", 1);
        code(
                "AC13 revisão obsoleta",
                post("AC13 ajuste revisão antiga", rr, old, S, 409),
                "CONTAGEM_DESATUALIZADA");
        var last = application(r, j.actual(units.get(0)));
        var applied = post("AC13 aplicar35 após reversão explícita", rr, last, S, 200);
        x.assertion(
                "AC13 replay35",
                applied.equals(post("AC13 replay35 sem novo efeito", rr, last, S, 200)),
                "mesmo snapshot",
                applied);
        x.assertion(
                "AC13 saldo final35 reserva0",
                j.balance().get("fisicoTotal").decimalValue().intValueExact() == 35
                        && j.balance().get("reservado").decimalValue().signum() == 0,
                "35/0",
                j.balance());
        var filtered =
                get(
                        "AC13 filtro aplicada conteúdo",
                        "/api/v1/contagens"
                                + ctx()
                                + "&produtoId="
                                + x.product.get("id").longValue()
                                + "&situacao=APLICADA&impedimento=false",
                        O);
        x.assertion(
                "AC13 filtro somente contexto/aplicadas",
                filtered.get("totalItens").longValue() == 2,
                "2 unidades/contagens aplicadas próprias, revisão atual",
                filtered);
        paginas("AC13 contagens", "/api/v1/contagens" + ctx(), O, 2);
        x.ids.put("contagens", counts);
        x.ids.put("saldoFinal", j.balance());
        x.save();
        var calc =
                calcular(
                        LocalDate.now(ZoneOffset.UTC),
                        LocalDate.now(ZoneOffset.UTC).plusDays(1),
                        "AC13 cálculo após ajustes sem contrato informado");
        x.ids.put("calculoAjusteId", calc.get("id").longValue());
        x.save();
        var live =
                get(
                        "AC13 GET cálculo preserva pendências",
                        "/api/v1/calculos-cobranca/" + calc.get("id").longValue(),
                        S);
        x.assertion(
                "AC13 não inventa cobrança sem configuração",
                live.get("situacao").asString().equals("PENDENTE") && live.get("total").isNull(),
                "PENDENTE totalNULL",
                live);
    }

    JsonNode calcular(LocalDate a, LocalDate b, String name) throws Exception {
        var d = x.command();
        d.putAll(
                m(
                        "clienteId",
                        x.clientId,
                        "armazemId",
                        x.warehouseId,
                        "periodoInicio",
                        a.toString(),
                        "periodoFim",
                        b.toString()));
        var c = post(name, "/api/v1/calculos-cobranca", d, S, 200);
        x.assertion(
                name + " replay",
                c.equals(post(name + " replay HTTP", "/api/v1/calculos-cobranca", d, S, 200)),
                "mesmo snapshot",
                c);
        return c;
    }

    Map<String, Object> decision(JsonNode f) {
        var d = x.version(f.get("fechamento"));
        d.put("numero", f.get("versao").get("numero").intValue());
        return d;
    }

    void financeiro() throws Exception {
        x.precheck("financeiro-nova-fixture", false);
        LocalDate today = LocalDate.now(ZoneOffset.UTC), start = today.minusDays(1);
        var d = x.command();
        d.putAll(
                m(
                        "codigo",
                        x.round + "MIN",
                        "descricao",
                        x.round + " mínimo FICTÍCIO abrangência armazenagem",
                        "tipo",
                        "ARMAZENAGEM",
                        "unidade",
                        "POSICAO_DIA"));
        var s = post("AC06 serviço fictício", "/api/v1/servicos-cobranca", d, G, 200);
        d = x.command();
        d.putAll(
                m(
                        "armazemId",
                        x.warehouseId,
                        "codigo",
                        x.round + "TAR",
                        "descricao",
                        x.round + " tarifa FICTÍCIA",
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
                                        5))));
        var t = post("AC06 tabela fictícia5", "/api/v1/tabelas-cobranca", d, G, 200);
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
        var v = post("AC06 vínculo fictício", "/api/v1/vinculos-tabela", d, G, 200);
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
                        "APLICAVEL",
                        "minimoValor",
                        50,
                        "minimoProporcao",
                        "INTEGRAL",
                        "servicosMinimo",
                        List.of(s.get("id").longValue()),
                        "grisModo",
                        "NAO_APLICAVEL"));
        var contract =
                post(
                        "AC06 contrato mínimo50 explícito FICTÍCIO",
                        "/api/v1/contratos-cobranca",
                        d,
                        G,
                        200);
        x.ids.put(
                "financeiroConfiguracao",
                m(
                        "servico",
                        s,
                        "tabela",
                        t,
                        "vinculo",
                        v,
                        "contrato",
                        contract,
                        "clockRealUtc",
                        Instant.now().toString(),
                        "origemValor",
                        "Complemento mínimo contratual50 menos subtotal elegível0; sem"
                                + " serviços/estoque/fatos retroativos"));
        x.save();
        var forecast = calcular(today, today.plusDays(1), "RN27 previsão ciclo ainda aberto");
        var calc = calcular(start, today, "RN27 cálculo corte real encerrado");
        x.ids.put("calculoFinanceiroId", calc.get("id").longValue());
        x.ids.put("previsaoFinanceiraId", forecast.get("id").longValue());
        x.save();
        var adj = calc.get("memoria").get("ajustes");
        x.assertion(
                "AC06 memória nãozero0+50+0",
                calc.get("situacao").asString().equals("COMPLETO")
                        && calc.get("total").decimalValue().compareTo(new BigDecimal("50")) == 0
                        && calc.get("subtotalConhecido").decimalValue().signum() == 0
                        && adj.get("subtotalElegivel").decimalValue().signum() == 0
                        && adj.get("minimoComplemento").decimalValue().intValueExact() == 50
                        && calc.get("memoria").get("servicos").isEmpty(),
                "COMPLETO subtotal0 mínimo50 GRIS0 total50; sem fato inventado",
                calc);
        var live =
                get(
                        "RN27 GET cálculo50",
                        "/api/v1/calculos-cobranca/" + calc.get("id").longValue(),
                        S);
        x.assertion("RN27 GET snapshot50", calc.equals(live), calc, live);
        x.call(
                "RN27 cálculo Operação negada",
                "GET",
                "/api/v1/calculos-cobranca/" + calc.get("id").longValue(),
                null,
                O,
                403);
        x.callToken(
                "RN27 cálculo alcance negado",
                "GET",
                "/api/v1/calculos-cobranca/" + calc.get("id").longValue(),
                null,
                S,
                x.token(S, m("wms_armazens", List.of())),
                403);
        var prepare = x.command();
        prepare.put("calculoId", calc.get("id").longValue());
        post("BE13 preparo Supervisor negado", "/api/v1/fechamentos-cobranca", prepare, S, 403);
        var f =
                post(
                        "BE13 preparar mínimo50 corte real",
                        "/api/v1/fechamentos-cobranca",
                        prepare,
                        G,
                        200);
        x.ids.put("fechamentoFinanceiroId", f.get("fechamento").get("id").longValue());
        x.save();
        x.assertion(
                "BE13 preparo replay único",
                f.equals(
                        post(
                                "BE13 replay preparo50",
                                "/api/v1/fechamentos-cobranca",
                                prepare,
                                G,
                                200)),
                "mesmo fechamento/versão",
                f);
        String route = "/api/v1/fechamentos-cobranca/" + f.get("fechamento").get("id").longValue();
        var dec = decision(f);
        post("BE13 aprovação Operação negada", route + "/aprovacao", dec, O, 403);
        post("BE13 aprovação Supervisor negada", route + "/aprovacao", dec, S, 403);
        var wrong = new LinkedHashMap<>(dec);
        wrong.put("versao", -1);
        post("BE13 versão inválida atomicidade", route + "/aprovacao", wrong, G, 400);
        var before = get("BE13 GET antes aprovação após recusas", route, G);
        x.assertion(
                "BE13 recusas sem decisão",
                before.get("situacao").asString().equals("EM_REVISAO"),
                "EM_REVISAO",
                before);
        var approved = post("BE13 aprovação nãozero50 real", route + "/aprovacao", dec, G, 200);
        x.assertion(
                "BE13 aprovado/replay50",
                approved.get("fechamento").get("situacao").asString().equals("APROVADO")
                        && approved.get("versao").get("saldo").decimalValue().intValueExact() == 50
                        && approved.equals(
                                post("BE13 replay aprovação50", route + "/aprovacao", dec, G, 200)),
                "APROVADO saldo50; mesmo snapshot",
                approved);
        get("BE13 GET fechamento50", route, G);
        get("BE13 GET versão50", route + "/versoes/1", G);
        get("BE13 memória demonstrativo50", route + "/versoes/1/demonstrativo", G);
        get(
                "BE13 auditoria50",
                "/api/v1/auditoria?tipo=FECHAMENTO_COBRANCA&registroId="
                        + f.get("fechamento").get("id").longValue(),
                G);
        var repeat = calcular(start, today, "RN27 recálculo explícito sem alteração de fonte");
        x.ids.put("recalculoFinanceiroId", repeat.get("id").longValue());
        x.assertion(
                "RN27 recálculo novo ID fonte preservada",
                repeat.get("id").longValue() != calc.get("id").longValue()
                        && repeat.get("entradasHash")
                                .asString()
                                .equals(calc.get("entradasHash").asString())
                        && get(
                                        "RN27 original permanece imutável",
                                        "/api/v1/calculos-cobranca/" + calc.get("id").longValue(),
                                        G)
                                .equals(calc),
                "novo snapshot50, hash igual/original preservado",
                repeat);
        var next = x.command();
        next.put("calculoId", forecast.get("id").longValue());
        var future =
                post(
                        "BE13 preparar previsão ciclo aberto",
                        "/api/v1/fechamentos-cobranca",
                        next,
                        G,
                        200);
        var futureDec = decision(future);
        String fr =
                "/api/v1/fechamentos-cobranca/" + future.get("fechamento").get("id").longValue();
        code(
                "BE13 corte futuro recusa exata",
                post("BE13 não aprova futuro Clock real", fr + "/aprovacao", futureDec, G, 409),
                "PERIODO_NAO_ENCERRADO");
        x.ids.put("fechamentoPrevisaoId", future.get("fechamento").get("id").longValue());
        x.ids.put(
                "financeiroFinal",
                m(
                        "calculo",
                        calc,
                        "fechamento",
                        approved,
                        "recalculo",
                        repeat,
                        "previsao",
                        forecast,
                        "cicloAberto",
                        future));
        x.save();
        paginas("RN27 cálculos", "/api/v1/calculos-cobranca" + ctx(), S, 3);
    }

    void listas() throws Exception {
        x.precheck("filtros-paginacao", false);
        var products = new ArrayList<Long>();
        products.add(x.product.get("id").longValue());
        for (int i = 1; i <= 2; i++) {
            var p =
                    post(
                            "filtro produto próprio" + i,
                            "/api/v1/produtos",
                            m(
                                    "clienteId",
                                    x.clientId,
                                    "sku",
                                    x.round + "SKU" + i,
                                    "descricao",
                                    x.round + " produto " + i,
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
            products.add(p.get("id").longValue());
        }
        x.ids.put("produtosLista", products);
        x.save();
        paginas("produtos alcance/conteúdo", "/api/v1/produtos?clienteId=" + x.clientId, O, 3);
        for (String scope : List.of("wms_clientes", "wms_armazens")) {
            String route =
                    scope.equals("wms_clientes")
                            ? "/api/v1/produtos?clienteId=" + x.clientId
                            : "/api/v1/enderecos?armazemId=" + x.warehouseId;
            x.callToken(
                    "filtro alcance explícito " + scope,
                    "GET",
                    route,
                    null,
                    O,
                    x.token(O, m(scope, List.of())),
                    403);
        }
        var filter =
                x.callToken(
                        "clientes lista filtrada sem alcance",
                        "GET",
                        "/api/v1/clientes?tamanho=1",
                        null,
                        O,
                        x.token(O, m("wms_clientes", List.of())),
                        200);
        x.assertion(
                "clientes lista isolada total0",
                filter.get("itens").isEmpty() && filter.get("totalItens").longValue() == 0,
                0,
                filter);
        for (int i = 0; i < 3; i++) j.address("LISTA" + i, i == 2 ? "QUARENTENA" : "ARMAZENAGEM");
        paginas("endereços por armazém", "/api/v1/enderecos?armazemId=" + x.warehouseId, O, 3);
        var limited =
                get(
                        "limite100 e isolamento produtos",
                        "/api/v1/produtos?clienteId=" + x.clientId + "&tamanho=100",
                        O);
        var actual = new ArrayList<Long>();
        boolean isolated = true;
        for (var p : limited.get("itens")) {
            actual.add(p.get("id").longValue());
            isolated &= p.get("clienteId").longValue() == x.clientId;
        }
        x.assertion(
                "limite100 inclui somente três IDs próprios ordenados",
                isolated && actual.equals(products),
                products,
                actual);
        x.call(
                "overflow página produto",
                "GET",
                "/api/v1/produtos?clienteId=" + x.clientId + "&pagina=2147483647&tamanho=100",
                null,
                O,
                400);
    }

    void financeiroLimites() throws Exception {
        x.precheck("financeiro-limites-temporais", false);
        var before =
                get(
                        "financeiro fatos próprios antes negativas",
                        "/api/v1/fatos-servico" + ctx(),
                        S);
        var pedido =
                get(
                        "financeiro fonte criada em agora, sem retroagir",
                        "/api/v1/pedidos-entrada/" + x.ids.get("pedidoEntradaId"),
                        O);
        var d = x.command();
        d.putAll(
                m(
                        "codigo",
                        x.round + "ADIC",
                        "descricao",
                        x.round + " ensaio limite histórico FICTÍCIO",
                        "tipo",
                        "ADICIONAL",
                        "unidade",
                        "QUANTIDADE_PRODUTO"));
        post(
                "financeiro cadastrar adicional Operação negada",
                "/api/v1/servicos-cobranca",
                d,
                O,
                403);
        var service =
                post(
                        "financeiro adicional somente configuração fictícia",
                        "/api/v1/servicos-cobranca",
                        d,
                        G,
                        200);
        x.ids.put("servicoLimiteTemporalId", service.get("id").longValue());
        d = x.command();
        d.putAll(
                m(
                        "clienteId",
                        x.clientId,
                        "armazemId",
                        x.warehouseId,
                        "servicoId",
                        service.get("id").longValue(),
                        "pedidoEntradaId",
                        x.ids.get("pedidoEntradaId"),
                        "produtoId",
                        x.product.get("id").longValue(),
                        "origem",
                        "MANUAL",
                        "referenciaExecucao",
                        x.round + "LIM",
                        "executadoEm",
                        LocalDate.now(ZoneOffset.UTC)
                                .minusDays(1)
                                .atTime(12, 0)
                                .toInstant(ZoneOffset.UTC)
                                .toString(),
                        "quantidade",
                        1,
                        "categoria",
                        "",
                        "cotas",
                        List.of(m("notaId", x.ids.get("notaId"), "cota", 1)),
                        "criterioRateio",
                        "Ensaio fictício proporcional da própria nota"));
        post("financeiro fato retroativo Operação negada", "/api/v1/fatos-servico", d, O, 403);
        var past =
                post(
                        "financeiro retroagir fato antes criação recusado",
                        "/api/v1/fatos-servico",
                        d,
                        S,
                        400);
        x.assertion(
                "financeiro limite real impede fato inventado no corte passado",
                past.path("detail").asString().contains("instante comprovado não futuro"),
                "400 execução exige instante >= pedido.criadoEm e <= Clock real",
                m("pedido", pedido, "recusa", past));
        d.put("operacaoId", UUID.randomUUID().toString());
        d.put("executadoEm", Instant.now().plusSeconds(60).toString());
        post("financeiro fato futuro também recusado", "/api/v1/fatos-servico", d, S, 400);
        var after =
                get(
                        "financeiro GET fatos sem efeitos após negativas",
                        "/api/v1/fatos-servico" + ctx(),
                        S);
        x.assertion(
                "financeiro recusas atômicas nenhum fato novo",
                before.equals(after),
                "mesma página/snapshot sem registro/rateio",
                after);
        x.ids.put(
                "limiteDesatualizacaoFinanceira",
                "Nesta fixture pedido criado07/10; novo fato não pode ter execução06/10. Fonte nova"
                    + " atual integra ciclo07→08, cuja aprovação recusa PERIODO_NAO_ENCERRADO. Não"
                    + " modificar Clock/histórico D26 para forçar CALCULO_DESATUALIZADO no corte"
                    + " anterior. D26 contém recusa real desse código; regressão existente também"
                    + " cobre alteração de fato, separadamente.");
        x.save();
    }

    void financeiroValidar() throws Exception {
        x.precheck("financeiro-get-jar-final", false);
        var calc =
                get(
                        "financeiro GET23 JAR final",
                        "/api/v1/calculos-cobranca/" + x.ids.get("calculoFinanceiroId"),
                        S);
        var close =
                get(
                        "financeiro GET14 APROVADO JAR final",
                        "/api/v1/fechamentos-cobranca/" + x.ids.get("fechamentoFinanceiroId"),
                        G);
        var version =
                get(
                        "financeiro versão50 JAR final",
                        "/api/v1/fechamentos-cobranca/"
                                + x.ids.get("fechamentoFinanceiroId")
                                + "/versoes/1",
                        G);
        get(
                "financeiro demonstrativo50 JAR final",
                "/api/v1/fechamentos-cobranca/"
                        + x.ids.get("fechamentoFinanceiroId")
                        + "/versoes/1/demonstrativo",
                G);
        x.assertion(
                "financeiro fonte/estado50 preservados no JAR final",
                calc.get("total").decimalValue().intValueExact() == 50
                        && close.get("situacao").asString().equals("APROVADO")
                        && version.get("saldo").decimalValue().intValueExact() == 50,
                "COMPLETO/APROVADO/saldo50",
                m("calculo", calc, "fechamento", close, "versao", version));
        var previous =
                x.json.readTree(
                        java.nio.file.Files.readString(
                                x.evidence.resolve(
                                        "d29-" + x.ids.getOrDefault("retomadaDe", x.round) + "-http.json")));
        for (var c : previous.get("cases"))
            if (c.path("caso").asString().equals("BE13 aprovação nãozero50 real")) {
                var replay =
                        post(
                                "financeiro replay aprovação original JAR final",
                                c.get("rota").asString(),
                                c.get("payload"),
                                G,
                                200);
                x.assertion(
                        "financeiro replay bytes/resultado preservado",
                        replay.equals(c.get("resposta")),
                        "mesmo snapshot aprovado50",
                        replay);
                post(
                        "financeiro replay Operação recusada JAR final",
                        c.get("rota").asString(),
                        c.get("payload"),
                        O,
                        403);
            }
        x.callToken(
                "financeiro GET alcance JAR final",
                "GET",
                "/api/v1/calculos-cobranca/" + x.ids.get("calculoFinanceiroId"),
                null,
                S,
                x.token(S, m("wms_clientes", List.of())),
                403);
        String route = "/api/v1/fechamentos-cobranca/" + x.ids.get("fechamentoFinanceiroId");
        var memory =
                get(
                        "financeiro memória v1 antes reabertura",
                        route + "/versoes/1/demonstrativo",
                        G);
        var reopen = x.version(close);
        reopen.put("numero", 1);
        reopen.put("calculoId", x.ids.get("recalculoFinanceiroId"));
        post("financeiro reabertura Supervisor negada", route + "/reabertura", reopen, S, 403);
        var reopened =
                post(
                        "financeiro reabertura v2 snapshot novo50",
                        route + "/reabertura",
                        reopen,
                        G,
                        200);
        x.assertion(
                "financeiro reabertura replay v2",
                reopened.equals(
                        post(
                                "financeiro replay reabertura",
                                route + "/reabertura",
                                reopen,
                                G,
                                200)),
                "mesmo fechamento/versão2",
                reopened);
        var old = decision(reopened);
        old.put("numero", 1);
        code(
                "financeiro versão anterior superada",
                post("financeiro nova aprovação da v1 recusada", route + "/aprovacao", old, G, 409),
                "VERSAO_SUPERADA");
        x.assertion(
                "financeiro memória v1 não reescrita",
                memory.equals(
                        get(
                                "financeiro memória v1 após reabertura",
                                route + "/versoes/1/demonstrativo",
                                G)),
                "mesma memória50",
                memory);
        var decision = decision(reopened);
        var approved =
                post("financeiro aprovação v2 atual50", route + "/aprovacao", decision, G, 200);
        x.assertion(
                "financeiro v2 aprovada/replay/saldo50",
                approved.get("versao").get("numero").intValue() == 2
                        && approved.get("versao").get("saldo").decimalValue().intValueExact() == 50
                        && approved.equals(
                                post(
                                        "financeiro replay aprovação v2",
                                        route + "/aprovacao",
                                        decision,
                                        G,
                                        200)),
                "versão2 APROVADO50/replay único",
                approved);
        get("financeiro GET versão2 atual50", route + "/versoes/2", G);
        get("financeiro GET fechamento versão2", route, G);
        x.ids.put("financeiroVersaoAtual", 2);
        x.save();
    }

    void xmlLimites() throws Exception {
        x.precheck("rn22-limites-numericos", false);
        if (!x.ids.containsKey("unidades")) j.prepararRecebimento();
        var before = get("RN22 limites GET antes", "/api/v1/pedidos-saida" + ctx(), O);
        var ops = new ArrayList<Map<String, Object>>();
        for (String q :
                List.of("1E2147483647", "1E13", "0.0000001", "1E-2147483647", "100E2147483647")) {
            var d = xmlPayload(x.nextDocumentNumber(), q, "1");
            ops.add(m("quantidade", q, "operacaoId", d.get("operacaoId")));
            x.ids.put("xmlLimitesOperacoes", ops);
            x.save();
            post("RN22 limite numerico " + q, "/api/v1/pedidos-saida/xml", d, O, 400);
            var after = get("RN22 limite GET sem efeito " + q, "/api/v1/pedidos-saida" + ctx(), O);
            x.assertion(
                    "RN22 limite rollback GET " + q,
                    before.equals(after),
                    "lista preservada",
                    after);
        }
        var positive = xmlPayload(x.nextDocumentNumber(), "2E0", "3E0");
        var created =
                post(
                        "RN22 cientifico valido2E0mais3E0",
                        "/api/v1/pedidos-saida/xml",
                        positive,
                        O,
                        201);
        x.ids.put("xmlLimitesPedidoPositivoId", created.get("pedido").get("id").longValue());
        x.ids.put("xmlLimitesOperacaoPositiva", positive.get("operacaoId"));
        x.save();
        x.assertion(
                "RN22 cientifico soma5 e replay",
                created.get("pedido")
                                        .get("itens")
                                        .get(0)
                                        .get("quantidade")
                                        .decimalValue()
                                        .compareTo(new BigDecimal("5"))
                                == 0
                        && created.equals(
                                post(
                                        "RN22 cientifico replay HTTP",
                                        "/api/v1/pedidos-saida/xml",
                                        positive,
                                        O,
                                        201)),
                "5 integral/mesmo snapshot",
                created);
        get(
                "RN22 cientifico GET proprio",
                "/api/v1/pedidos-saida/" + x.ids.get("xmlLimitesPedidoPositivoId"),
                O);
    }

    void xmlValidar() throws Exception {
        x.precheck("rn22-jar-final", false);
        var prior =
                x.json.readTree(
                        java.nio.file.Files.readString(
                                x.evidence.resolve(
                                        "d29-" + x.ids.getOrDefault("retomadaDe", x.round) + "-http.json")));
        for (var c : prior.get("cases"))
            if (c.path("caso").asString().startsWith("RN22 XML integral ")) {
                var replay =
                        post(
                                "RN22 replay original JAR final " + c.get("perfil").asString(),
                                "/api/v1/pedidos-saida/xml",
                                c.get("payload"),
                                c.get("perfil").asString(),
                                201);
                x.assertion(
                        "RN22 origem/hash/snapshot mantidos JAR final "
                                + c.get("perfil").asString(),
                        replay.equals(c.get("resposta")),
                        "mesmo documento/pedido/linhas/hash",
                        replay);
                get(
                        "RN22 GET pedido atual JAR final " + c.get("perfil").asString(),
                        "/api/v1/pedidos-saida/" + replay.get("pedido").get("id").longValue(),
                        O);
            }
        JsonNode other = null;
        try (var files = java.nio.file.Files.list(x.evidence)) {
            for (var path :
                    files.filter(
                                    p ->
                                            p.getFileName()
                                                    .toString()
                                                    .matches("d29-D29[A-F0-9]{8}-http\\.json"))
                            .toList()) {
                var candidate = x.json.readTree(java.nio.file.Files.readString(path));
                if (candidate.path("fase").asString().equals("concluido")
                        && candidate.path("ids").path("etapa").asString().equals("listas")
                        && candidate.path("ids").path("clienteId").longValue() != x.clientId) {
                    other = candidate;
                    break;
                }
            }
        }
        if (other == null)
            throw new IllegalStateException("D29_FIXTURE_LISTAS_OUTRO_CLIENTE_AUSENTE");
        x.ids.put("fonteSKUOutraFixture", other.get("rodada").asString());
        var otherProduct =
                get(
                        "RN22 fonte SKU outra fixture D29",
                        "/api/v1/produtos/" + other.get("ids").get("produtoId").longValue(),
                        G);
        var d = xmlPayload(x.nextDocumentNumber(), "2", "3");
        d.put(
                "xml",
                d.get("xml")
                        .toString()
                        .replace(
                                x.product.get("sku").asString(),
                                otherProduct.get("sku").asString()));
        post(
                "RN22 SKU existente de outro cliente recusado",
                "/api/v1/pedidos-saida/xml",
                d,
                O,
                400);
        var large = xmlPayload(x.nextDocumentNumber(), "2", "3");
        large.put("xml", "X".repeat(1000001));
        post("RN22 limite XML1000001", "/api/v1/pedidos-saida/xml", large, O, 400);
        var empty = xmlPayload(x.nextDocumentNumber(), "0", "3");
        post("RN22 quantidade zero recusada", "/api/v1/pedidos-saida/xml", empty, O, 400);
        var list = get("RN22 pós recusas JAR final", "/api/v1/pedidos-saida" + ctx(), O);
        x.assertion(
                "RN22 recusas sem novos pedidos",
                list.get("totalItens").longValue() == 4,
                "3 XML+1 reserva/contagem; nenhum novo",
                list);
    }

    void listasEstoque() throws Exception {
        x.precheck("listas-estoque-jar-final", false);
        var units = x.json.valueToTree(x.ids.get("unidades"));
        var a = j.actual(units.get(0));
        var b = j.actual(units.get(1));
        String route = "/api/v1/estoque" + ctx() + "&produtoId=" + x.product.get("id").longValue();
        var p =
                get(
                        "filtro estoque disponível conteúdo",
                        route + "&disponivel=true&situacao=ARMAZENAGEM&pagina=0&tamanho=1",
                        O);
        x.assertion(
                "filtro estoque35/ID/contexto/metadados",
                p.get("totalItens").longValue() == 1
                        && p.get("totalPaginas").intValue() == 1
                        && p.get("itens").size() == 1
                        && p.get("itens").get(0).get("unidade").get("id").longValue()
                                == a.get("id").longValue()
                        && p.get("itens")
                                        .get(0)
                                        .get("unidade")
                                        .get("quantidade")
                                        .decimalValue()
                                        .intValueExact()
                                == 35,
                "unidadeA35 própria/1 item/1 página",
                p);
        for (String filter :
                List.of(
                        "&disponivel=false",
                        "&situacao=QUARENTENA",
                        "&situacao=RESERVADO",
                        "&situacao=CONTAGEM_PENDENTE",
                        "&codigoUnidade=" + b.get("codigo").asString(),
                        "&pagina=1&tamanho=1")) {
            var empty = get("filtro estoque vazio " + filter, route + filter, O);
            x.assertion(
                    "filtro conteúdo vazio " + filter,
                    empty.get("itens").isEmpty(),
                    "itens0",
                    empty);
        }
        x.call(
                "filtro estoque situação inválida",
                "GET",
                route + "&situacao=INVENTADO",
                null,
                O,
                400);
        x.callToken(
                "filtro estoque alcance armazém",
                "GET",
                route,
                null,
                O,
                x.token(O, m("wms_armazens", List.of())),
                403);
        paginas(
                "unidades histórico ativa/inativa",
                j.pe(((Number) x.ids.get("pedidoEntradaId")).longValue()) + "/unidades",
                O,
                2);
        paginas("movimentos unidadeA", j.unit(a) + "/movimentos", O, 3);
        paginas("movimentos unidadeB", j.unit(b) + "/movimentos", O, 2);
        var s = j.balance();
        x.assertion(
                "saldo final35 sem phantom/reserva",
                s.get("fisicoTotal").decimalValue().intValueExact() == 35
                        && s.get("disponivel").decimalValue().intValueExact() == 35
                        && s.get("reservado").decimalValue().signum() == 0
                        && s.get("bloqueado").decimalValue().signum() == 0,
                "35/35/0/0",
                s);
    }

    JsonNode concurrentCall(String name, String route, Object payload, String jwt)
            throws Exception {
        var req =
                java.net.http.HttpRequest.newBuilder(java.net.URI.create(x.base + route))
                        .timeout(Duration.ofSeconds(45))
                        .header("X-Request-Id", x.round + "-" + UUID.randomUUID())
                        .header("Authorization", "Bearer " + jwt)
                        .header("Content-Type", "application/json")
                        .POST(
                                java.net.http.HttpRequest.BodyPublishers.ofString(
                                        x.json.writeValueAsString(payload)))
                        .build();
        var r = x.http.send(req, java.net.http.HttpResponse.BodyHandlers.ofString());
        synchronized (x) {
            return x.recordResponse(name, "POST", route, payload, O, 200, r, true);
        }
    }

    JsonNode ready(java.nio.file.Path file) {
        try {
            var r = x.json.readTree(java.nio.file.Files.readAllBytes(file));
            x.ids.put(
                    "readinessUltimaLeitura",
                    m(
                            "utc",
                            Instant.now().toString(),
                            "parse",
                            true,
                            "rodadaIgual",
                            r.path("rodada").asString().equals(x.round),
                            "pronto",
                            r.path("pronto").asBoolean(false),
                            "observadorAtestado",
                            r.path("observadorAtestado").asBoolean(false),
                            "bancoIgual",
                            r.path("banco").asString().equals("WMS_DEV"),
                            "servidorIgual",
                            r.path("servidor").asString().equals("ROD-SRVW-001"),
                            "publicacaoUtc",
                            r.path("utc").asString()));
            if (r.has("expiraUtc")
                    && !Instant.parse(r.path("expiraUtc").asString()).isAfter(Instant.now()))
                return null;
            return r.path("rodada").asString().equals(x.round)
                            && r.path("pronto").asBoolean(false)
                            && r.path("observadorAtestado").asBoolean(false)
                            && r.path("banco").asString().equals("WMS_DEV")
                            && r.path("servidor").asString().equals("ROD-SRVW-001")
                    ? r
                    : null;
        } catch (Exception e) {
            x.ids.put(
                    "readinessUltimaLeitura",
                    m(
                            "utc",
                            Instant.now().toString(),
                            "parse",
                            false,
                            "classeErro",
                            e.getClass().getSimpleName()));
            return null;
        }
    }

    void publishConc(Map<String, Object> d) throws Exception {
        java.nio.file.Files.writeString(
                x.evidence.resolve("d29-concorrencia-pedido.json"),
                x.json.writerWithDefaultPrettyPrinter().writeValueAsString(d));
    }
}
