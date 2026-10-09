import tools.jackson.databind.JsonNode;
import java.time.*;
import java.util.*;

/** Casos D29 por HTTP: primeira revisão de carga e pendência temporal após transformação real. */
final class RegressaoD29 {
    final EnsaioD29 x;
    final JornadasD29 j;
    RegressaoD29(EnsaioD29 x) { this.x=x; j=new JornadasD29(x); }
    Map<String,Object> m(Object... p) { return EnsaioD29.m(p); }
    JsonNode get(String p) throws Exception { return x.call("D29 GET "+p,"GET",p,null,"GESTOR",200); }
    JsonNode post(String n,String p,Object b) throws Exception { return x.call(n,"POST",p,b,"GESTOR",200); }
    void run() throws Exception {
        j.cadastros();
        // Configuração fictícia explícita; corte real encerrado e aprovação sem fatos inventados.
        var p=new PendenciasD29(x);
        p.financeiro();
        p.financeiroValidar();
        cargas();
        temporal();
    }
    void temporal() throws Exception {
        x.precheck("temporal-fixture-nova",false);
        long pe=j.entry("TEMPORAL",50);
        j.arrival(pe,50,Instant.now());
        post("D29 temporal efetivar entrada",j.pe(pe)+"/efetivacao",m("versao",j.pv(pe),"aceitarDivergencias",false,"motivo",x.round+" prova ficticia"));
        long en=get(j.pe(pe)+"/entradas").get("itens").get(0).get("id").longValue();
        var u=j.unitize(pe,en,50);
        String route=j.pe(pe)+"/unidades/"+u.get("id").longValue();
        var d=x.version(u); d.put("quantidadeNovaUnidade",10);
        var split=x.call("D29 temporal divisão real","POST",route+"/divisao",d,"SUPERVISOR",200);
        x.assertion("D29 divisão replay",split.equals(post("D29 divisão replay",route+"/divisao",d)),split,"snapshot igual");
        var dest=split.get("unidades").get(0).get("unidade");
        var origin=split.get("unidades").get(1).get("unidade");
        d=x.command(); d.putAll(m("versaoDestino",dest.get("versao").longValue(),"origens",List.of(m("unidadeId",origin.get("id").longValue(),"versao",origin.get("versao").longValue()))));
        var regroup=post("D29 temporal reagrupamento real",route+"/reagrupamento",d);
        post("D29 reagrupamento replay",route+"/reagrupamento",d);
        u=j.actual(dest);
        var address=j.address("TEMPORAL","ARMAZENAGEM");
        post("D29 temporal primeiro endereçamento",j.unit(u)+"/movimentos",j.move(u,address));
        x.ids.put("transformacaoTemporal",m("pedidoEntradaId",pe,"entradaId",en,"divisao",split,"reagrupamento",regroup,"unidade",j.actual(u)));
        d=x.command(); d.putAll(m("clienteId",x.clientId,"armazemId",x.warehouseId,"periodoInicio",LocalDate.now(ZoneOffset.UTC).toString(),"periodoFim",LocalDate.now(ZoneOffset.UTC).plusDays(1).toString()));
        var calc=post("D29 temporal persiste PENDENTE sem rollback-only","/api/v1/calculos-cobranca",d);
        x.ids.put("calculoTemporalId",calc.get("id").longValue()); x.ids.put("calculoTemporal",calc); x.save();
        x.assertion("D29 temporal histórico insuficiente explícito",calc.get("situacao").asString().equals("PENDENTE") && calc.get("total").isNull() && calc.get("pendencias").toString().contains("HISTORICO_QUANTIDADE_INSUFICIENTE"),"PENDENTE/totalNULL/HISTORICO_QUANTIDADE_INSUFICIENTE",calc);
        x.assertion("D29 temporal replay/GET único",calc.equals(post("D29 temporal replay","/api/v1/calculos-cobranca",d)) && calc.equals(get("/api/v1/calculos-cobranca/"+calc.get("id").longValue())),calc,"replay e GET iguais");
        get("/api/v1/auditoria?tipo=CALCULO_COBRANCA&registroId="+calc.get("id").longValue());
    }
    void cargas() throws Exception {
        x.precheck("complementos-carga", false);
        var before = j.balance();
        JsonNode load;
        if(x.ids.containsKey("retomadaDe")) {
            var prior=x.json.readTree(java.nio.file.Files.readAllBytes(x.evidence.resolve("d29-"+x.ids.get("retomadaDe")+"-http.json")));
            JsonNode created=null;
            for(var c:prior.path("cases"))if(c.path("rota").asString().equals("/api/v1/cargas-iniciais")&&c.path("actual").asInt()==200){created=c;break;}
            if(created==null)throw new IllegalStateException("D29_CARGA_PARCIAL_NAO_ENCONTRADA");
            load=get("/api/v1/cargas-iniciais/"+created.path("resposta").path("id").asLong());
            if(!load.path("situacao").asString().equals("PENDENTE"))throw new IllegalStateException("D29_RETOMADA_CARGA_JA_AVANCADA_RECUSADA");
            var data=load.path("revisao").path("dados");
            x.preserveCapturedDates(data,created.path("resposta").path("revisao").path("dados"),Instant.parse(created.path("instanteRespostaUtc").asString()));
            var corrected=x.json.convertValue(data,Map.class);
            var note=new LinkedHashMap<String,Object>((Map)corrected.get("nota"));note.put("numero",x.nextDocumentNumber());corrected.put("nota",note);
            var revision=x.version(load);revision.put("dados",corrected);
            load=post("D29 corrigir colisao nota fixture carga pendente rev3", "/api/v1/cargas-iniciais/"+load.path("id").asLong()+"/revisoes",revision);
            x.ids.put("correcaoFixtureCarga","Somente numero da nota ficticia pendente101→numeroNovoSemColisao; chegada/FIFO originais reais preservados por captura HTTP anterior. Sem repetir contagem/ajustes/recebimento concluido.");
        } else load = criarCarga("LOAD", 20);
        String route = "/api/v1/cargas-iniciais/" + load.get("id").longValue();
        get(route);
        get(route + "/revisoes");
        var d = x.version(load);
        if(!x.ids.containsKey("retomadaDe")) {
            d.put("dados", load.get("revisao").get("dados"));
            load = post("carga revisao preservada", route + "/revisoes", d);
        }
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
        String when = Instant.now().toString();
        var nota =
                m(
                        "versao",
                        0,
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


}
