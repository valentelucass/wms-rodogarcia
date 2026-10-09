import tools.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;

/** Variantes técnicas fictícias por HTTP; nenhuma escrita JDBC ou alteração de Clock. */
final class VariantesD29 {
    final PendenciasD29 p;
    final EnsaioD29 x;
    final JornadasD29 j;
    final String G = "GESTOR", S = "SUPERVISOR", O = "OPERACAO";

    VariantesD29(PendenciasD29 p) {
        this.p = p;
        this.x = p.x;
        this.j = p.j;
    }

    static Map<String, Object> m(Object... args) {
        return EnsaioD29.m(args);
    }

    long id(JsonNode n) {
        return n.get("id").longValue();
    }

    void equal(String name, JsonNode n, String field, String value) throws Exception {
        x.assertion(
                name,
                n.get(field).decimalValue().compareTo(new BigDecimal(value)) == 0,
                field + "=" + value,
                n);
    }

    void run() throws Exception {
        x.precheck("variantes-estoque-financeiro", false);
        JsonNode aProduct = x.product, aPack = x.pack;
        JsonNode cProduct, cPack;
        long pe;
        if (x.ids.containsKey("retomadaDe")) {
            var prior =
                    x.json.readTree(
                            java.nio.file.Files.readAllBytes(
                                    x.evidence.resolve(
                                            "d29-" + x.ids.get("retomadaDe") + "-http.json")));
            if (!prior.get("bloqueio")
                    .asString()
                    .startsWith("RN23 FIFO chegada parcial0 expected=200 actual=400"))
                throw new IllegalStateException("D29_RETOMADA_VARIANTE_FORA_PONTO");
            cProduct =
                    p.get(
                            "RN15 GET SKU lote da fixture preservada",
                            "/api/v1/produtos/" + id(response(prior, "RN15 segundo SKU com lote")),
                            O);
            cPack =
                    p.get(
                            "RN15 GET DUN da fixture preservada",
                            "/api/v1/embalagens/" + id(response(prior, "RN15 DUN segundo SKU")),
                            O);
            pe = id(response(prior, "RN15 pedido misto fonte"));
        } else {
            cProduct =
                    p.post(
                            "RN15 segundo SKU com lote",
                            "/api/v1/produtos",
                            m(
                                    "clienteId",
                                    x.clientId,
                                    "sku",
                                    x.round + "LOT",
                                    "descricao",
                                    x.round + " produto lote ficticio",
                                    "unidadeMedida",
                                    "UN",
                                    "tipoQuantidade",
                                    "CONTAGEM",
                                    "precisaoQuantidade",
                                    0,
                                    "controlaLote",
                                    true,
                                    "controlaValidade",
                                    false),
                            G,
                            201);
            cPack =
                    p.post(
                            "RN15 DUN segundo SKU",
                            "/api/v1/embalagens",
                            m(
                                    "produtoId",
                                    id(cProduct),
                                    "codigoDun",
                                    x.round + "DUNLOT",
                                    "descricao",
                                    x.round + " DUN ficticio lote",
                                    "quantidadeProduto",
                                    10),
                            G,
                            201);
            pe =
                    id(
                            p.post(
                                    "RN15 pedido misto fonte",
                                    "/api/v1/pedidos-entrada",
                                    m(
                                            "clienteId",
                                            x.clientId,
                                            "armazemId",
                                            x.warehouseId,
                                            "referencia",
                                            x.round + "MISTO"),
                                    O,
                                    201));
            p.post(
                    "RN15 nota preserva dois SKUs",
                    j.pe(pe) + "/notas",
                    m(
                            "versao",
                            j.pv(pe),
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
                                            id(aProduct),
                                            "quantidadePrevista",
                                            40,
                                            "valorMercadoria",
                                            400),
                                    m(
                                            "numeroItem",
                                            2,
                                            "produtoId",
                                            id(cProduct),
                                            "quantidadePrevista",
                                            20,
                                            "valorMercadoria",
                                            200))),
                    O,
                    200);
            p.post(
                    "RN15 conferencia",
                    j.pe(pe) + "/iniciar-conferencia",
                    m("versao", j.pv(pe), "motivo", x.round + " conferir fisicamente"),
                    O,
                    200);
        }
        var nota = j.get(j.pe(pe)).get("notas").get(0);
        var items = nota.get("itens");
        Instant old = Instant.now(), later = Instant.now();
        for (int i = 0; i < 2; i++) {
            var d = x.command();
            d.putAll(
                    m(
                            "versao",
                            j.pv(pe),
                            "chegouEm",
                            (i == 0 ? old : later).toString(),
                            "observacao",
                            x.round + " chegada parcial ficticia",
                            "itens",
                            List.of(
                                    m(
                                            "itemNotaId",
                                            id(items.get(0)),
                                            "quantidadeBoa",
                                            20,
                                            "quantidadeAvariada",
                                            0),
                                    m(
                                            "itemNotaId",
                                            id(items.get(1)),
                                            "lote",
                                            x.round + "L" + (i + 1),
                                            "quantidadeBoa",
                                            10,
                                            "quantidadeAvariada",
                                            0))));
            d.remove("motivo");
            var arrived =
                    p.post("RN23 FIFO chegada parcial" + i, j.pe(pe) + "/chegadas", d, O, 200);
            x.assertion(
                    "RN23 FIFO chegada replay" + i,
                    arrived.equals(
                            p.post(
                                    "RN23 FIFO chegada replay HTTP" + i,
                                    j.pe(pe) + "/chegadas",
                                    d,
                                    O,
                                    200)),
                    "mesmo snapshot",
                    arrived);
        }
        p.post(
                "RN15 efetivacao misto",
                j.pe(pe) + "/efetivacao",
                m(
                        "versao",
                        j.pv(pe),
                        "aceitarDivergencias",
                        false,
                        "motivo",
                        x.round + " conferencia integral"),
                S,
                200);
        var entries = j.get(j.pe(pe) + "/entradas").get("itens");
        var au = new ArrayList<JsonNode>();
        var cu = new ArrayList<JsonNode>();
        for (var e : entries) {
            boolean a = e.get("produtoId").longValue() == id(aProduct);
            x.pack = a ? aPack : cPack;
            var u = j.unitize(pe, id(e), a ? 20 : 10);
            (a ? au : cu).add(u);
        }
        x.pack = aPack;
        x.assertion(
                "RN15 duas unidades de cada SKU",
                au.size() == 2 && cu.size() == 2,
                "2 A/2 C",
                entries);
        x.ids.put(
                "variantesFonte",
                m(
                        "pedidoEntradaId",
                        pe,
                        "notaId",
                        id(nota),
                        "produtoA",
                        aProduct,
                        "produtoC",
                        cProduct,
                        "embalagemA",
                        aPack,
                        "embalagemC",
                        cPack,
                        "unidadesA",
                        au,
                        "unidadesC",
                        cu));
        x.save();
        rejeitarReagrupamento(pe, au.get(0), cu.get(0), "RN15 mistura SKU recusada");
        rejeitarReagrupamento(
                pe, au.get(0), au.get(1), "RN23 FIFO chegada distinta nao pode reagrupamento");
        rejeitarReagrupamento(
                pe, cu.get(0), cu.get(1), "RN23 FIFO lotes distintos nao pode reagrupamento");
        long newer = j.entry("RECENTE", 20);
        j.arrival(newer, 20, Instant.now());
        p.post(
                "RN23 FIFO efetivacao origem recente",
                j.pe(newer) + "/efetivacao",
                m(
                        "versao",
                        j.pv(newer),
                        "aceitarDivergencias",
                        false,
                        "motivo",
                        x.round + " fonte recente conferida"),
                S,
                200);
        var recent = j.unitize(newer, id(j.get(j.pe(newer) + "/entradas").get("itens").get(0)), 20);
        var noteNew = j.get(j.pe(newer)).get("notas").get(0);
        var all = List.of(au.get(0), au.get(1), cu.get(0), cu.get(1), recent);
        for (int i = 0; i < all.size(); i++) {
            var e = j.address("VAR" + i, i == 1 ? "QUARENTENA" : "ARMAZENAGEM");
            p.post(
                    "RN17 posicionamento " + (i == 1 ? "quarentena inicial" : "armazenagem") + i,
                    j.unit(all.get(i)) + "/movimentos",
                    j.move(all.get(i), e),
                    O,
                    200);
        }
        var a1 = j.actual(au.get(0));
        var a2 = j.actual(au.get(1));
        var c1 = j.actual(cu.get(0));
        var c2 = j.actual(cu.get(1));
        recent = j.actual(recent);
        x.ids.put("variantesUnidades", List.of(a1, a2, c1, c2, recent));
        x.ids.put("variantesNotaRecenteId", id(noteNew));
        x.save();
        x.assertion(
                "RN23 FIFO origem nota primeira chegada vs chegada real",
                a1.get("dataFifo").equals(a2.get("dataFifo"))
                        && !a1.get("chegadaReal").equals(a2.get("chegadaReal"))
                        && Instant.parse(a1.get("dataFifo").asString())
                                .isBefore(Instant.parse(recent.get("dataFifo").asString())),
                "mesma nota FIFO antigo/chegadas distintas/nova nota recente",
                List.of(a1, a2, recent));
        equal("RN17 saldo fisico preserva quarentena", j.balance(), "fisicoTotal", "60");
        equal("RN17 disponivel exclui quarentena inicial", j.balance(), "disponivel", "40");
        var aOrder = j.order("FIFO-A", 25);
        var aFifo = p.get("RN23 FIFO sugestao origem antiga primeiro", j.ps(aOrder) + "/fifo", O);
        var selections = aFifo.get("selecoes");
        x.assertion(
                "RN23 FIFO sugestao25 exclui quarentena e usa origem antiga",
                selections.size() == 2
                        && selections.get(0).get("unidadeId").longValue() == id(a1)
                        && selections
                                        .get(0)
                                        .get("quantidade")
                                        .decimalValue()
                                        .compareTo(new BigDecimal("20"))
                                == 0
                        && selections.get(1).get("unidadeId").longValue() == id(recent),
                "antiga20+recente5; quarentena0",
                aFifo);
        var reserve = x.version(aOrder);
        var aReserved =
                p.post(
                        "RN23 FIFO reserva integral A25",
                        j.ps(aOrder) + "/reserva",
                        reserve,
                        O,
                        200);
        x.assertion(
                "RN23 FIFO reserva A replay",
                aReserved.equals(
                        p.post(
                                "RN23 FIFO reserva A replay HTTP",
                                j.ps(aOrder) + "/reserva",
                                reserve,
                                O,
                                200)),
                "mesmo snapshot",
                aReserved);
        aOrder = aReserved.get("pedido");
        x.product = cProduct;
        var cOrder = j.order("FIFO-C", 15);
        var cReserve =
                p.post(
                        "RN23 FIFO reserva lotes C15",
                        j.ps(cOrder) + "/reserva",
                        x.version(cOrder),
                        O,
                        200);
        cOrder = cReserve.get("pedido");
        var rs = cOrder.get("reservas");
        x.assertion(
                "RN23 FIFO reservas preservam nota lote data e parcial pallet",
                rs.size() == 2
                        && rs.get(0).get("unidadeId").longValue() == id(c1)
                        && rs.get(0).get("lote").asString().equals(x.round + "L1")
                        && rs.get(1).get("lote").asString().equals(x.round + "L2")
                        && rs.get(0).get("notaOrigemId").longValue() == id(nota)
                        && rs.get(0).get("dataFifo").equals(c1.get("dataFifo"))
                        && rs.get(1).get("quantidade").decimalValue().compareTo(new BigDecimal("5"))
                                == 0,
                "loteL1 10/loteL2 5/origem e FIFO preservados",
                rs);
        x.product = aProduct;
        x.ids.put("variantesPedidos", List.of(id(aOrder), id(cOrder)));
        x.save();
        financeiro(a1, aOrder);
        p.get("RN17 GET pedido lotes reserva preservada", j.ps(cOrder), O);
        p.get("RN17 GET quarentena inicial sem baixa", j.unit(a2), O);
        x.ids.put("variantesSaldoFinal", j.balance());
        x.save();
    }

    JsonNode response(JsonNode prior, String name) {
        for (var c : prior.get("cases"))
            if (c.path("caso").asString().equals(name)
                    && c.path("estado").asString().equals("aprovado")) return c.get("resposta");
        throw new IllegalStateException("D29_FONTE_VARIANTE_AUSENTE");
    }

    void rejeitarReagrupamento(long pe, JsonNode to, JsonNode from, String name) throws Exception {
        var beforeA = j.actual(to);
        var beforeB = j.actual(from);
        var d = x.command();
        d.putAll(
                m(
                        "versaoDestino",
                        beforeA.get("versao").longValue(),
                        "origens",
                        List.of(
                                m(
                                        "unidadeId",
                                        id(beforeB),
                                        "versao",
                                        beforeB.get("versao").longValue()))));
        p.code(
                name + " codigo",
                p.post(name, j.pe(pe) + "/unidades/" + id(to) + "/reagrupamento", d, S, 409),
                "ORIGENS_INCOMPATIVEIS");
        x.assertion(
                name + " rollback GET",
                beforeA.equals(j.actual(to)) && beforeB.equals(j.actual(from)),
                "fontes e quantidades intactas",
                List.of(beforeA, beforeB));
    }

    Map<String, Object> table(String code, String type, long service, int price, LocalDate today) {
        var d = x.command();
        d.putAll(
                m(
                        "armazemId",
                        x.warehouseId,
                        "codigo",
                        x.round + code,
                        "descricao",
                        x.round + " tarifa FICTICIA",
                        "tipo",
                        type,
                        "vigenciaInicio",
                        today.toString(),
                        "itens",
                        List.of(m("servicoId", service, "categoria", "", "preco", price))));
        if (type.equals("ESPECIFICA")) d.put("clienteId", x.clientId);
        return d;
    }

    void financeiro(JsonNode unit, JsonNode order) throws Exception {
        x.precheck("variantes-financeiro", false);
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        var d = x.command();
        d.putAll(
                m(
                        "codigo",
                        x.round + "ARM",
                        "descricao",
                        x.round + " armazenagem FICTICIA",
                        "tipo",
                        "ARMAZENAGEM",
                        "unidade",
                        "POSICAO_DIA"));
        JsonNode service, standard, specific;
        if (x.ids.containsKey("retomarFinanceiroVariantes")) {
            var prior =
                    x.json.readTree(
                            java.nio.file.Files.readAllBytes(
                                    x.evidence.resolve(
                                            "d29-" + x.ids.get("retomadaDe") + "-http.json")));
            service = response(prior, "RN08 servico armazenagem");
            standard = response(prior, "RN08 tabela padrao5");
            specific = response(prior, "RN08 tabela especifica7");
            p.get(
                    "RN08 GET tabela especifica preservada",
                    "/api/v1/tabelas-cobranca/" + id(specific),
                    G);
        } else {
            service = p.post("RN08 servico armazenagem", "/api/v1/servicos-cobranca", d, G, 200);
            standard =
                    p.post(
                            "RN08 tabela padrao5",
                            "/api/v1/tabelas-cobranca",
                            table("PAD", "PADRAO", id(service), 5, today),
                            G,
                            200);
            specific =
                    p.post(
                            "RN08 tabela especifica7",
                            "/api/v1/tabelas-cobranca",
                            table("ESP", "ESPECIFICA", id(service), 7, today),
                            G,
                            200);
        }
        var duplicated = table("ESP", "ESPECIFICA", id(service), 9, today);
        duplicated.put("codigo", specific.get("codigo").asString());
        p.code(
                "RN08 tabela codigo duplicado exato",
                p.post(
                        "RN08 tabela especifica codigo duplicado",
                        "/api/v1/tabelas-cobranca",
                        duplicated,
                        G,
                        409),
                "TABELA_DUPLICADA");
        d = x.command();
        d.putAll(
                m(
                        "clienteId",
                        x.clientId,
                        "armazemId",
                        x.warehouseId,
                        "tabelaId",
                        id(specific),
                        "vigenciaInicio",
                        today.toString()));
        p.post("RN08 vinculo operador recusado", "/api/v1/vinculos-tabela", d, O, 403);
        var link = p.post("RN08 vinculo especifico7", "/api/v1/vinculos-tabela", d, G, 200);
        x.assertion(
                "RN08 vinculo replay",
                link.equals(
                        p.post("RN08 vinculo replay HTTP", "/api/v1/vinculos-tabela", d, G, 200)),
                "mesmo snapshot",
                link);
        var overlap = x.command();
        overlap.putAll(
                m(
                        "clienteId",
                        x.clientId,
                        "armazemId",
                        x.warehouseId,
                        "tabelaId",
                        id(standard),
                        "vigenciaInicio",
                        today.toString()));
        p.code(
                "RN08 conflito vinculos codigo",
                p.post("RN08 conflito vinculos", "/api/v1/vinculos-tabela", overlap, G, 409),
                "VIGENCIA_SOBREPOSTA");
        var other =
                p.post(
                        "RN08 cliente ficticio contexto distinto",
                        "/api/v1/clientes",
                        m(
                                "codigo",
                                x.round + "OUT",
                                "nome",
                                x.round + " cliente ficticio tabela",
                                "documentoFiscal",
                                x.round + "OUTDOC"),
                        G,
                        201);
        var wrong = x.command();
        wrong.putAll(
                m(
                        "clienteId",
                        id(other),
                        "armazemId",
                        x.warehouseId,
                        "tabelaId",
                        id(specific),
                        "vigenciaInicio",
                        today.toString()));
        p.code(
                "RN08 tabela especifica outro cliente codigo",
                p.post(
                        "RN08 tabela especifica outro cliente",
                        "/api/v1/vinculos-tabela",
                        wrong,
                        G,
                        409),
                "CONTEXTO_TABELA_INVALIDO");
        d = x.command();
        d.putAll(
                m(
                        "clienteId",
                        x.clientId,
                        "armazemId",
                        x.warehouseId,
                        "vigenciaInicio",
                        today.toString(),
                        "fuso",
                        "UTC",
                        "moeda",
                        "BRL",
                        "modalidadeCiclo",
                        "DIAS_CORRIDOS",
                        "duracaoDias",
                        2,
                        "minimoModo",
                        "APLICAVEL",
                        "minimoValor",
                        100,
                        "minimoProporcao",
                        "PROPORCIONAL_DIAS",
                        "servicosMinimo",
                        List.of(id(service)),
                        "grisModo",
                        "APLICAVEL",
                        "grisPercentual",
                        1,
                        "grisBase",
                        "VALOR_ESTOQUE_PICO",
                        "grisPeriodicidade",
                        "POR_CICLO",
                        "grisProporcao",
                        "PROPORCIONAL_DIAS"));
        var contract =
                p.post(
                        "AC06 contrato minimo e GRIS proporcionais",
                        "/api/v1/contratos-cobranca",
                        d,
                        G,
                        200);
        x.ids.put(
                "variantesComercial",
                m(
                        "servico",
                        service,
                        "padrao",
                        standard,
                        "especifica",
                        specific,
                        "vinculo",
                        link,
                        "contrato",
                        contract,
                        "outroClienteId",
                        id(other)));
        x.save();
        var before =
                p.calcular(
                        today.plusDays(1),
                        today.plusDays(2),
                        "AC04 previsao pico4 tabela especifica7");
        x.ids.put("variantesCalculoAntesId", id(before));
        x.save();
        verificarCalculo(before, "4", "28", "800", "4", "54", "22", "AC04 antes avaria");
        unit = j.actual(unit);
        var q = j.address("AVARIAQ", "QUARENTENA");
        d = x.command();
        d.putAll(
                m(
                        "versaoUnidade",
                        unit.get("versao").longValue(),
                        "quantidade",
                        5,
                        "ocorridaEm",
                        Instant.now().toString(),
                        "destinos",
                        j.destinations(q)));
        String avroute = "/api/v1/estoque/unidades/" + unit.get("codigo").asString() + "/avarias";
        p.post("RN17 avaria operador recusado", avroute, d, O, 403);
        var av = p.post("RN17 avaria parcial5 em unidade reservada", avroute, d, S, 200);
        x.assertion(
                "RN17 avaria replay",
                av.equals(p.post("RN17 avaria replay HTTP", avroute, d, S, 200)),
                "mesmo snapshot",
                av);
        x.ids.put("variantesAvariaId", id(av.get("avaria")));
        x.save();
        p.code(
                "RN17 impedimento reserva por avaria codigo",
                p.post(
                        "RN17 revalidacao avaria nao libera reserva",
                        j.ps(order) + "/revalidacao",
                        null,
                        O,
                        409),
                "RESERVA_IMPEDIDA");
        var live = p.get("RN17 GET reserva ativa com impedimento", j.ps(order), O);
        x.assertion(
                "RN17 reserva preservada e prosseguir false",
                live.get("reservas").size() == 2
                        && !live.get("podeProsseguir").booleanValue()
                        && live.get("situacao").asString().equals("RESERVADO"),
                "reserva25 mantida; impedimento",
                live);
        equal("RN17 fisico apos avaria nao baixa", j.balance(), "fisicoTotal", "60");
        equal("RN17 reserva apos avaria nao libera", j.balance(), "reservado", "25");
        var pending =
                p.calcular(
                        today.plusDays(1),
                        today.plusDays(2),
                        "AC08 avaria sem responsabilidade bloqueia calculo");
        x.ids.put("variantesCalculoPendenteId", id(pending));
        x.save();
        x.assertion(
                "AC08 pendencia exata e totalNULL",
                pending.get("situacao").asString().equals("PENDENTE")
                        && pending.get("total").isNull()
                        && pending.get("pendencias")
                                .toString()
                                .contains("RESPONSABILIDADE_AVARIA_PENDENTE"),
                "PENDENTE RESPONSABILIDADE_AVARIA_PENDENTE",
                pending);
        d = x.command();
        d.putAll(
                m(
                        "versao",
                        av.get("avaria").get("versao").longValue(),
                        "responsabilidade",
                        "RODOGARCIA"));
        String rr = avroute + "/" + id(av.get("avaria")) + "/responsabilidade";
        p.post("AC08 responsabilidade supervisor recusado", rr, d, S, 403);
        var recognised = p.post("AC08 responsabilidade Gestor Rodogarcia", rr, d, G, 200);
        x.assertion(
                "AC08 responsabilidade replay",
                recognised.equals(p.post("AC08 responsabilidade replay HTTP", rr, d, G, 200)),
                "mesmo snapshot",
                recognised);
        p.post("AC08 replay supervisor nao ganha permissao", rr, d, S, 403);
        equal(
                "AC08 suspensao proporcional5sobre20",
                recognised.get("avaria"),
                "proporcaoSuspensa",
                "0.25");
        var after =
                p.calcular(
                        today.plusDays(1),
                        today.plusDays(2),
                        "AC08 previsao avaria responsabilidade reconhecida");
        x.ids.put("variantesCalculoDepoisId", id(after));
        x.save();
        verificarCalculo(
                after, "3.75", "26.25", "750", "3.75", "53.75", "23.75", "AC08 depois avaria");
        x.assertion(
                "AC08 fonte muda hash e snapshot antigo permanece",
                !after.get("entradasHash").equals(before.get("entradasHash"))
                        && before.equals(
                                p.get(
                                        "AC08 GET calculo anterior imutavel",
                                        "/api/v1/calculos-cobranca/" + id(before),
                                        G)),
                "hash mudou; anterior54 intacto",
                after);
        p.get("AC08 GET calculo final memoria53.75", "/api/v1/calculos-cobranca/" + id(after), G);
        p.get("AC08 GET ocorrencia reconhecida", avroute, O);
        var reverse = x.version(live);
        var reversed =
                p.post(
                        "RN17 reversao explicita Supervisor",
                        j.ps(order) + "/reversao-reserva",
                        reverse,
                        S,
                        200);
        x.assertion(
                "RN17 reversao replay",
                reversed.equals(
                        p.post(
                                "RN17 reversao replay HTTP",
                                j.ps(order) + "/reversao-reserva",
                                reverse,
                                S,
                                200)),
                "mesmo snapshot",
                reversed);
        equal("RN17 reversao libera somente reserva explicitamente", j.balance(), "reservado", "0");
        equal("RN17 avaria quarentena seguem sem disponibilidade", j.balance(), "disponivel", "20");
        x.ids.put(
                "variantesFinanceiroFinal",
                m("antes", before, "pendente", pending, "depois", after, "avaria", recognised));
        x.save();
    }

    void retomarFinanceiro() throws Exception {
        if (!x.ids.containsKey("variantesFonte") || !x.ids.containsKey("variantesPedidos"))
            throw new IllegalStateException("D29_VARIANTES_FIXTURE_AUSENTE");
        var source = x.json.valueToTree(x.ids.get("variantesFonte"));
        var u = source.get("unidadesA").get(0);
        var orders = x.json.valueToTree(x.ids.get("variantesPedidos"));
        var order =
                p.get(
                        "RN17 GET reserva para retomar variante financeira",
                        "/api/v1/pedidos-saida/" + orders.get(0).longValue(),
                        O);
        x.ids.put("retomarFinanceiroVariantes", true);
        financeiro(u, order);
        p.get(
                "RN17 GET pedido lotes preservado final",
                "/api/v1/pedidos-saida/" + orders.get(1).longValue(),
                O);
    }

    void validarFinal() throws Exception {
        x.precheck("variantes-validacao-final", false);
        var prior =
                x.json.readTree(
                        java.nio.file.Files.readAllBytes(
                                x.evidence.resolve(
                                        "d29-" + x.ids.get("retomadaDe") + "-http.json")));
        var before =
                p.get(
                        "AC04 GET previsao pico4 e valor54",
                        "/api/v1/calculos-cobranca/" + x.ids.get("variantesCalculoAntesId"),
                        G);
        var pending =
                p.get(
                        "AC08 GET calculo pendente responsabilidade",
                        "/api/v1/calculos-cobranca/" + x.ids.get("variantesCalculoPendenteId"),
                        S);
        var after =
                p.get(
                        "AC08 GET calculo memoria avaria final",
                        "/api/v1/calculos-cobranca/" + x.ids.get("variantesCalculoDepoisId"),
                        G);
        verificarCalculo(before, "4", "28", "800", "4", "54", "22", "AC04 antes avaria snapshot");
        verificarCalculo(
                after,
                "3.75",
                "26.25",
                "750",
                "3.75",
                "53.75",
                "23.75",
                "AC08 depois avaria snapshot");
        for (var c : prior.get("cases"))
            if (c.path("caso")
                            .asString()
                            .equals("AC08 previsao avaria responsabilidade reconhecida")
                    || c.path("caso").asString().equals("AC08 replay calculo final nova JVM")) {
                var replay =
                        p.post(
                                "AC08 replay calculo final nova JVM",
                                c.get("rota").asString(),
                                c.get("payload"),
                                S,
                                200);
                x.assertion(
                        "AC08 replay calculo snapshot53.75",
                        after.equals(replay),
                        "mesmo snapshot",
                        replay);
            }
        var orders = x.json.valueToTree(x.ids.get("variantesPedidos"));
        var live =
                p.get(
                        "RN17 GET reserva antes reversao canonica",
                        "/api/v1/pedidos-saida/" + orders.get(0).longValue(),
                        O);
        Map<String, Object> reverse = null;
        for (var c : prior.get("cases"))
            if (c.path("caso").asString().equals("RN17 reversao explicita Supervisor")
                    || c.path("caso")
                            .asString()
                            .equals("RN17 reversao canonica mesma UUID preservada"))
                reverse =
                        x.json.convertValue(
                                c.get("payload"),
                                new tools.jackson.core.type.TypeReference<
                                        Map<String, Object>>() {});
        if (reverse == null) throw new IllegalStateException("D29_REPRO_REVERSAO_AUSENTE");
        p.post(
                "RN17 reversao Operacao recusada",
                j.ps(live) + "/reversao-reserva",
                reverse,
                O,
                403);
        var reversed =
                p.post(
                        "RN17 reversao canonica mesma UUID preservada",
                        j.ps(live) + "/reversao-reserva",
                        reverse,
                        S,
                        200);
        x.assertion(
                "RN17 reversao canonica replay",
                reversed.equals(
                        p.post(
                                "RN17 reversao canonica replay HTTP",
                                j.ps(live) + "/reversao-reserva",
                                reverse,
                                S,
                                200)),
                "mesmo snapshot",
                reversed);
        equal("RN17 saldo fisico final avaria e quarentena", j.balance(), "fisicoTotal", "60");
        equal("RN17 reserva final somente liberacao explicita", j.balance(), "reservado", "0");
        equal(
                "RN17 avaria e quarentena excluem disponibilidade final",
                j.balance(),
                "disponivel",
                "20");
        var cOrder =
                p.get(
                        "RN23 FIFO GET lotes reservados sem remutacao",
                        "/api/v1/pedidos-saida/" + orders.get(1).longValue(),
                        O);
        x.ids.put(
                "variantesFinanceiroFinal",
                m(
                        "antes",
                        before,
                        "pendente",
                        pending,
                        "depois",
                        after,
                        "pedidoA",
                        reversed,
                        "pedidoC",
                        cOrder));
        x.save();
        var links =
                p.get(
                        "RN08 GET vinculo especifico lista contexto",
                        "/api/v1/vinculos-tabela" + p.ctx(),
                        G);
        x.assertion(
                "RN08 lista vinculo unico especifico",
                links.size() == 1
                        && links.get(0).get("tabelaId").longValue()
                                == x.json.valueToTree(x.ids.get("variantesComercial"))
                                        .get("especifica")
                                        .get("id")
                                        .longValue(),
                "1 vinculo especifico7; conflitos nao alteraram",
                links);
        var u = x.json.valueToTree(x.ids.get("variantesFonte")).get("unidadesA").get(0);
        p.get(
                "AC08 GET avaria responsabilidade final",
                "/api/v1/estoque/unidades/" + u.get("codigo").asString() + "/avarias",
                O);
    }

    void verificarCalculo(
            JsonNode c,
            String peak,
            String subtotal,
            String base,
            String gris,
            String total,
            String complemento,
            String prefix)
            throws Exception {
        x.assertion(
                prefix + " completo",
                c.get("situacao").asString().equals("COMPLETO"),
                "COMPLETO",
                c.get("pendencias"));
        var daily = c.get("memoria").get("diarias").get(0);
        var adj = c.get("memoria").get("ajustes");
        equal(prefix + " pico", daily, "picoCobravel", peak);
        equal(prefix + " tarifa especifica", daily, "tarifa", "7");
        equal(prefix + " subtotal", daily, "valor", subtotal);
        equal("AC06 " + prefix + " minimo metade100", adj, "minimoAplicavel", "50");
        equal("AC06 " + prefix + " complemento", adj, "minimoComplemento", complemento);
        equal("AC06 " + prefix + " base GRIS", adj, "baseGris", base);
        equal("AC06 " + prefix + " GRIS1pct metade", adj, "valorGris", gris);
        equal(prefix + " total", c, "total", total);
        x.assertion(
                "AC06 " + prefix + " dias1de2",
                adj.get("diasIncluidos").longValue() == 1
                        && adj.get("diasNominais").longValue() == 2,
                "1/2 dias",
                adj);
    }
}
