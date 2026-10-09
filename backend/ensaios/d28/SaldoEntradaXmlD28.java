import java.time.*;
import java.util.*;

/** D26-VIG07: físico inclui recebimento XML que ainda não foi unitizado. */
final class SaldoEntradaXmlD28 {
    static void validateBase(Map<String,Object> ids) {
        if (!"D289A0CF1ED".equals(ids.get("retomadaDe")))
            throw new IllegalStateException("D28_R16_MODO_FRESCO_BLOQUEADO_SEM_HTTP_SQL");
    }
    static void run(EnsaioD28 x) throws Exception {
        validateBase(x.ids);
        var j=new JornadasD28(x);
        x.precheck("saldo-XML-nao-unitizado",false);
        var before=j.balance();
        if (x.ids.containsKey("retomadaDe") && x.ids.get("retomadaDe").equals("D289A0CF1ED")) {
            long pe=28;
            var current=x.call("D28 retomar somente entrada28 ficticia ainda pendente","GET",j.pe(pe),null,"SUPERVISOR",200);
            x.assertion("D28 precondicao entrada28 QUARENTENA prevista5 fisica4", current.path("pedido").path("referencia").asString().equals("D289A0CF1EDXMLNAOUNIT")
                    && current.path("pedido").path("situacao").asString().equals("QUARENTENA") && current.path("divergente").asBoolean()
                    && current.path("notas").get(0).path("itens").get(0).path("prevista").decimalValue().intValueExact()==5
                    && current.path("notas").get(0).path("itens").get(0).path("recebidaBoa").decimalValue().intValueExact()==4,
                    "pedido28 proprio,quarentena,previsto5,recebido4",current);
            x.ids.put("entradaXmlNaoUnitizadaId",pe);
            x.ids.put("saldoXmlRetomadaRedDe","D289A0CF1ED");
            x.save();
            var body=EnsaioD28.m("versao",current.path("pedido").path("versao").longValue(),"aceitarDivergencias",true,
                    "motivo",x.round+" AC03 solucao ficticia registrada com cliente D28: prevista5, recebida4; falta1 aceita explicitamente, efetivar somente4 reais; fiscal pendente, sem criar faltante");
            x.call("D28 supervisor aceita falta1 ficticia e efetiva4 reais","POST",j.pe(pe)+"/efetivacao",body,"SUPERVISOR",200);
            var after=j.balance();
            x.assertion("D28 saldo confirmado nao unitizado fisico+4 pendente4",after.path("fisicoTotal").decimalValue().subtract(before.path("fisicoTotal").decimalValue()).intValueExact()==4
                    && after.path("pendenteUnitizacao").decimalValue().intValueExact()==4
                    && after.path("fisicoUnitizado").equals(before.path("fisicoUnitizado")) && after.path("disponivel").equals(before.path("disponivel")),
                    "fisico+4,pendente4,unitizado/disponivel preservados",EnsaioD28.m("antes",before,"depois",after));
            var finalEntry=x.call("D28 GET FINAL entrada28 confirmada nao unitizada","GET",j.pe(pe),null,"OPERACAO",200);
            x.assertion("D28 entrada28 EFETIVADO e nao unitizada",finalEntry.path("pedido").path("situacao").asString().equals("EFETIVADO"),"EFETIVADO",finalEntry);
            x.call("D28 registros fisicos entrada28 sem unitizacao","GET",j.pe(pe)+"/entradas",null,"OPERACAO",200);
            x.ids.put("saldoXmlNaoUnitizado",EnsaioD28.m("antes",before,"depois",after));x.save();return;
        }
    }
}
