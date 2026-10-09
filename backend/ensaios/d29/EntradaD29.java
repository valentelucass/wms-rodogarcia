import tools.jackson.databind.JsonNode;
import java.time.*;
import java.util.*;

/** Oraculos de quantidade/origem/estado definidos por V01-04/V27-29 e AC03. */
final class EntradaD29 {
    final EnsaioD29 x;final JornadasD29 j;
    EntradaD29(EnsaioD29 x){this.x=x;this.j=new JornadasD29(x);}
    static Map<String,Object> m(Object...v){return EnsaioD29.m(v);}
    JsonNode get(String n,String r)throws Exception{return x.call(n,"GET",r,null,"OPERACAO",200);}
    void run()throws Exception{
        x.ids.put("oraculosSQL",m("mil","pedidoEFETIVADO/entrada1000/2unidades500/notas e chegadas preservadas/replayunico","divergencias","100previstas vs98/102/95boas5avariadas; aceiteSupervisor; historicos intactos; pendenteunitizacao","XML","importacao sem chegada nao geraentrada/unidade/saldo; seguranca recusas sem notas novas"));x.save();
        x.precheck("entrada-cadastros",false);j.cadastros();
        x.precheck("entrada-manual-mil",false);
        long pe=j.entry("MIL",1000);x.ids.put("pedidoEntradaId",pe);x.save();
        var documental=get("V01 manual previsto nao e estoque",j.pe(pe));
        x.assertion("V01 pedido em conferencia sem chegada e saldo zero",documental.path("notas").get(0).path("itens").get(0).path("prevista").decimalValue().intValueExact()==1000&&j.balance().path("fisicoTotal").decimalValue().signum()==0,"previsto1000/fisico0",m("pedido",documental,"saldo",j.balance()));
        j.arrival(pe,1000,Instant.now());
        var accepted=m("versao",j.pv(pe),"aceitarDivergencias",false,"motivo",x.round+" mil unidades conferidas fisicamente ficticias");
        x.call("V02 Operacao nao efetiva","POST",j.pe(pe)+"/efetivacao",accepted,"OPERACAO",403);
        x.call("V02 Supervisor efetiva1000","POST",j.pe(pe)+"/efetivacao",accepted,"SUPERVISOR",200);
        var entr=get("V02 entrada conferida antes unitizacao",j.pe(pe)+"/entradas");long en=entr.path("itens").get(0).path("id").longValue();
        x.ids.put("entradaConferidaId",en);x.save();
        var before=j.balance();x.assertion("I04 mil produtos zero unidades e pendente1000",before.path("fisicoTotal").decimalValue().intValueExact()==1000&&before.path("pendenteUnitizacao").decimalValue().intValueExact()==1000&&before.path("disponivel").decimalValue().signum()==0,"fisico1000/pendente1000/disponivel0",before);
        var wrong=unitBody(pe,List.of(m("embalagemId",x.ids.get("embalagemId"),"tipo","PALLET","condicao","BOA","quantidade",1001)));
        x.call("V04 unitizacao1001 excede1000","POST",j.pe(pe)+"/entradas/"+en+"/unitizacao",wrong,"OPERACAO",409);
        x.assertion("V04 rollback saldo e pendente integrais",before.equals(j.balance()),before,j.balance());
        var good=unitBody(pe,List.of(m("embalagemId",x.ids.get("embalagemId"),"tipo","PALLET","condicao","BOA","quantidade",500),m("embalagemId",x.ids.get("embalagemId"),"tipo","PALLET","condicao","BOA","quantidade",500)));
        var units=x.call("V02 dois pallets500 milprodutos","POST",j.pe(pe)+"/entradas/"+en+"/unitizacao",good,"OPERACAO",200);
        var replay=x.call("V14 replay mesma unitizacao","POST",j.pe(pe)+"/entradas/"+en+"/unitizacao",good,"OPERACAO",200);
        var ul=new ArrayList<JsonNode>();for(var u:units.path("unidades"))ul.add(u.path("unidade"));x.ids.put("unidades",ul);x.save();
        x.assertion("V02 exatamente2 IDs distintos500 e origem igual",ul.size()==2&&!ul.get(0).path("id").equals(ul.get(1).path("id"))&&ul.stream().allMatch(u->u.path("quantidade").decimalValue().intValueExact()==500&&u.path("pedidoId").asLong()==pe),"2palIets500/origem"+pe,ul);
        x.assertion("V14 replay nao cria terceiro ID",units.equals(replay)&&j.balance().path("fisicoUnitizado").decimalValue().intValueExact()==1000&&j.balance().path("pendenteUnitizacao").decimalValue().signum()==0,units,replay);
        for(var u:ul){var tag=get("V09 etiqueta antes reimpressao",j.unit(u)+"/etiqueta");var again=get("V09 reimpressao somente documento",j.unit(u)+"/etiqueta");x.assertion("V09 reimpressao ID/data/quantidade invariaveis",tag.equals(again),tag,again);}
        divergence("FALTA",98,0,false);divergence("SOBRA",102,0,false);divergence("AVARIA",95,5,true);
        xmlEntry();
        x.ids.put("oraculosSQL",m("mil","pedidoEFETIVADO/entrada1000/2unidades500/notas e chegadas preservadas/replayunico","divergencias","100previstas vs98/102/95boas5avariadas; aceiteSupervisor; historicos intactos; pendenteunitizacao","XML","importacao sem chegada nao geraentrada/unidade/saldo; segurançarecusassemnotasnovas"));x.save();
    }
    Map<String,Object> unitBody(long pe,List<?> units)throws Exception{return m("operacaoId",UUID.randomUUID().toString(),"versaoPedido",j.pv(pe),"motivo",x.round+" unitizacao ficticia","unidades",units);}
    void divergence(String suffix,int good,int bad,boolean avaria)throws Exception{
        x.precheck("entrada-"+suffix,false);long pe=j.entry(suffix,100);var before=j.balance();
        var note=get("AC03 item previsto100",j.pe(pe)).path("notas").get(0).path("itens").get(0);
        var body=m("versao",j.pv(pe),"operacaoId",UUID.randomUUID().toString(),"chegouEm",Instant.now().toString(),"observacao",x.round+" divergencia ficticia","itens",List.of(m("itemNotaId",note.path("id").longValue(),"quantidadeBoa",good,"quantidadeAvariada",bad)));
        x.call("V27 V28 chegada "+suffix,"POST",j.pe(pe)+"/chegadas",body,"OPERACAO",200);
        var current=get("V27 V28 toda carga quarentena "+suffix,j.pe(pe));
        x.assertion("V27 V28 quarentena e previsto preservados "+suffix,current.path("pedido").path("situacao").asString().equals("QUARENTENA")&&current.path("divergente").asBoolean()&&j.balance().equals(before),"QUARENTENA/previsto100/sem saldo confirmado",current);
        var reject=m("versao",j.pv(pe),"aceitarDivergencias",false,"motivo",x.round+" sem aceite da divergencia");
        x.call("V27 divergencia exige aceite "+suffix,"POST",j.pe(pe)+"/efetivacao",reject,"SUPERVISOR",409);
        x.assertion("V27 recusa conserva fisico anterior "+suffix,before.equals(j.balance()),before,j.balance());
        x.call("AC03 solucao ficticia cliente aceite explicito "+suffix,"POST",j.pe(pe)+"/efetivacao",m("versao",j.pv(pe),"aceitarDivergencias",true,"motivo",x.round+" solucao ficticia acordada cliente: aceitar real "+good+" boas e "+bad+" avariadas vs100 previstas; fiscal pendente"),"SUPERVISOR",200);
        var after=j.balance();x.assertion("AC03 reconhece somente quantidade real "+suffix,after.path("fisicoTotal").decimalValue().subtract(before.path("fisicoTotal").decimalValue()).intValueExact()==good+bad&&after.path("disponivel").equals(before.path("disponivel")),"fisico+"+(good+bad)+"/disponivel preservado",after);
        var entry=get("AC03 entradas boas avariadas "+suffix,j.pe(pe)+"/entradas");x.ids.put("entrada"+suffix,m("pedidoEntradaId",pe,"entradas",entry));x.save();
        x.call("AC03 chegada apos efetivacao recusada "+suffix,"POST",j.pe(pe)+"/chegadas",m("versao",j.pv(pe),"operacaoId",UUID.randomUUID().toString(),"chegouEm",Instant.now().toString(),"observacao",x.round+" recusa apos efetivacao","itens",body.get("itens")),"OPERACAO",409);
    }
    void xmlEntry()throws Exception{
        x.precheck("entrada-XML",false);var before=j.balance();
        long pe=x.call("V01 novo pedido documental XML","POST","/api/v1/pedidos-entrada",m("clienteId",x.clientId,"armazemId",x.warehouseId,"referencia",x.round+"XML"),"OPERACAO",201).path("id").longValue();
        var client=get("V01 documento cliente proprio","/api/v1/clientes/"+x.clientId);
        String key=Long.toUnsignedString(new Random().nextLong());key=(key.repeat(5)).substring(0,44);
        String xml="<NFe xmlns='http://www.portalfiscal.inf.br/nfe'><infNFe Id='NFe"+key+"' versao='4.00'><ide><mod>55</mod><serie>1</serie><nNF>"+(x.nextDocumentNumber())+"</nNF><dhEmi>"+Instant.now()+"</dhEmi></ide><emit><CNPJ>"+client.path("documentoFiscal").asString()+"</CNPJ></emit><det nItem='1'><prod><cProd>"+x.product.path("sku").asString()+"</cProd><uCom>UN</uCom><qCom>5</qCom><vProd>50.00</vProd></prod></det></infNFe></NFe>";
        x.call("V01 importar XML apenas previsao","POST",j.pe(pe)+"/notas/xml",m("versao",j.pv(pe),"xml",xml),"OPERACAO",200);
        x.assertion("V01 XML nao cria fisico disponivel ou unidade",before.equals(j.balance()),before,j.balance());
        x.call("RN12 XXE externo recusado","POST",j.pe(pe)+"/notas/xml",m("versao",j.pv(pe),"xml","<!DOCTYPE NFe [<!ENTITY ext SYSTEM 'file:///D29-ficticio-inexistente'>]><NFe>&ext;</NFe>"),"OPERACAO",400);
        var current=get("V01 GET final documento XML",j.pe(pe));x.ids.put("entradaXML",m("pedidoEntradaId",pe,"pedido",current));x.save();
    }
}
