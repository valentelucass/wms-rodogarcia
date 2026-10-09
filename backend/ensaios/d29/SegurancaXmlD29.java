import tools.jackson.databind.JsonNode;
import java.time.*;
import java.util.*;

/** Casos independentes de identidade, alcance, correlacao e RN22 sobre nova familia. */
final class SegurancaXmlD29 {
 final EnsaioD29 x;final JornadasD29 j;final PendenciasD29 p;
 SegurancaXmlD29(EnsaioD29 x){this.x=x;j=new JornadasD29(x);p=new PendenciasD29(x);}
 static Map<String,Object> m(Object...v){return EnsaioD29.m(v);}
 void run()throws Exception {
  if(x.ids.containsKey("retomadaDe")){x.product=x.call("D29 XML retoma GET produto","GET","/api/v1/produtos/"+x.ids.get("produtoId"),null,"GESTOR",200);x.pack=x.call("D29 XML retoma GET embalagem","GET","/api/v1/embalagens/"+x.ids.get("embalagemId"),null,"GESTOR",200);var prior=x.json.readTree(java.nio.file.Files.readAllBytes(x.evidence.resolve("d29-"+x.ids.get("retomadaDe")+"-http.json")));if(prior.path("bloqueio").asString().startsWith("D29 XML400 XInclude"))missingAfterXInclude(prior);else{p.xmlLimites();extraXml();}return;}
  x.ids.put("oraculosSQL",m("JWT","Assinatura/issuer/aud/sub/iat/exp/perfil/listas invalidos401; alcance vazio403ID e lista0; sem efeito recusas. Tokens efemeros nunca capturados.","RN22","3perfis XML2+3→5 e replay; recusa duplicidade/divergencia/DTD/XXE/SKU/unidade/quantidades/limites sempedido/auditoria; 2E0+3E0 positivo5 adicional; fisico100/reserva0.","correlacao","RequestId servidorUUID substitui enviado, idOperacao igualheader; sem cookie/eco detalhe privado.","limite","Sem provedor/SEFAZ nativo, lockSQL ou faultSQL tardio."));x.save();j.cadastros();identity();p.xml();p.xmlLimites();extraXml();
 }
 void identity()throws Exception {
  x.precheck("JWT-perfil-alcance",false);String route="/api/v1/clientes/"+x.clientId;var now=Instant.now();
  var invalid=List.of(m("nome","issuer","claims",m("iss","https://d29-issuer-invalido.local")),m("nome","audience","claims",m("aud",List.of("d29-audience-invalida"))),m("nome","sub vazio","claims",m("sub","")),m("nome","sub201","claims",m("sub","X".repeat(201))),m("nome","sem iat","claims",m("iat",null)),m("nome","sem exp","claims",m("exp",null)),m("nome","exp anterior iat","claims",m("exp",Date.from(now.minusSeconds(180)))),m("nome","vida maior15min","claims",m("exp",Date.from(now.plusSeconds(1800)))),m("nome","iat futuro","claims",m("iat",Date.from(now.plusSeconds(180)))),m("nome","perfil estranho","claims",m("wms_perfil","ADMIN")),m("nome","lista501","claims",m("wms_clientes",Collections.nCopies(501,Long.toString(x.clientId)))),m("nome","lista numero tipo incorreto","claims",m("wms_clientes",List.of(x.clientId))),m("nome","lista negativo","claims",m("wms_clientes",List.of("-1"))),m("nome","lista overflow","claims",m("wms_armazens",List.of("9223372036854775808"))));
  for(var bad:invalid)x.callToken("D29 JWT401 "+bad.get("nome"),"GET",route,null,"OPERACAO",x.token("OPERACAO",(Map)bad.get("claims")),401);
  String token=x.token("OPERACAO",Map.of());String[]parts=token.split("\\.");parts[2]=(parts[2].charAt(0)=='A'?"B":"A")+parts[2].substring(1);x.callToken("D29 JWT assinatura alterada","GET",route,null,"OPERACAO",String.join(".",parts),401);
  x.call("D29 JWT ausencia","GET",route,null,null,401);
  x.callToken("D29 alcance vazio ID403","GET",route,null,"OPERACAO",x.token("OPERACAO",m("wms_clientes",List.of())),403);
  var empty=x.callToken("D29 alcance vazio lista200zero","GET","/api/v1/clientes?tamanho=100",null,"OPERACAO",x.token("OPERACAO",m("wms_clientes",List.of())),200);x.assertion("D29 lista vazia nao vaza",empty.path("itens").isEmpty()&&empty.path("totalItens").asLong()==0,"total0/itens0",empty);
  var invalidDto=x.call("D29 erro DTO sem eco","POST","/api/v1/clientes",m("codigo","","razaoSocial","D29_MARCADOR_PRIVADO_FICTICIO","documento",""),"GESTOR",400);var row=x.cases.getLast();String requestId=String.valueOf(row.get("requestId"));UUID.fromString(requestId);x.assertion("D29 correlacao substitui enviado e nao ecoa",!requestId.equals(row.get("requestIdEnviado"))&&invalidDto.path("idOperacao").asString().equals(requestId)&&!invalidDto.toString().contains("D29_MARCADOR_PRIVADO_FICTICIO"),"UUIDservidor/header=idOperacao/semeco",invalidDto);
  x.call("D29 metodo semrota documentada405","PATCH",route,m("campo","D29_FICTICIO"),"GESTOR",405);
  x.call("D29 Operacao financeiro403","GET","/api/v1/calculos-cobranca?clienteId="+x.clientId+"&armazemId="+x.warehouseId,null,"OPERACAO",403);
 }
 void extraXml()throws Exception {
  x.precheck("RN22-estrutura-limites",false);var before=p.get("D29 XML extras antes","/api/v1/pedidos-saida"+p.ctx(),"OPERACAO");var valid=p.xmlPayload(90001,"2","3");String xml=valid.get("xml").toString();
  var bads=List.of(m("nome","zero","xml",xml.replace("<qCom>2</qCom>","<qCom>0</qCom>")),m("nome","modelo distinto","xml",xml.replace("<mod>55</mod>","<mod>65</mod>")),m("nome","versao distinta","xml",xml.replace("versao='4.00'","versao='3.10'")),m("nome","namespace distinto","xml",xml.replace("http://www.portalfiscal.inf.br/nfe","urn:d29:invalido")),m("nome","XInclude","xml",xml.replace("<ide>","<xi:include xmlns:xi='http://www.w3.org/2001/XInclude' href='file:///d29-ficticio'/><ide>")),m("nome","depth65","xml",xml.replace("<ide>","<x>".repeat(65)+"</x>".repeat(65)+"<ide>")),m("nome","XML1000001","xml","X".repeat(1000001)));
  for(var bad:bads){var d=x.command();d.putAll(m("clienteId",x.clientId,"armazemId",x.warehouseId,"xml",bad.get("xml")));p.post("D29 XML400 "+bad.get("nome"),"/api/v1/pedidos-saida/xml",d,"OPERACAO",400);var after=p.get("D29 XML semefeito "+bad.get("nome"),"/api/v1/pedidos-saida"+p.ctx(),"OPERACAO");x.assertion("D29 XML recusa lista igual "+bad.get("nome"),before.equals(after),"4pedidos/semnovo",after);}
  x.ids.put("pedidosXMLAtual",before);x.save();
 }
 void missingAfterXInclude(JsonNode prior)throws Exception {
  if("4304D4283BB21A0F80F3D6AD034F9891D560DFED754B6511E6BD89CDE8900CE1".equals(x.ids.get("jarSha256")))throw new IllegalStateException("D29_XINCLUDE_GREEN_EXIGE_JAR_CORRIGIDO");
  x.precheck("XInclude-green-so-faltantes",false);long preservedId=0;for(var c:prior.path("cases"))if(c.path("caso").asString().equals("D29 XML400 XInclude")&&c.path("actual").asInt()==201)preservedId=c.path("resposta").path("pedido").path("id").asLong();if(preservedId==0)throw new IllegalStateException("D29_RED_XINCLUDE_ID_AUSENTE");var preserved=p.get("D29 preservar pedido redXInclude","/api/v1/pedidos-saida/"+preservedId,"OPERACAO");var before=p.get("D29 XML green baseline corrente","/api/v1/pedidos-saida"+p.ctx(),"OPERACAO");
  for(String type:List.of("XInclude","depth65","XML1000001","linhas201")) {
   var d=p.xmlPayload(x.nextDocumentNumber(),"2","3");String xml=d.get("xml").toString();
   if(type.equals("XInclude"))xml=xml.replace("<ide>","<xi:include xmlns:xi='http://www.w3.org/2001/XInclude' href='file:///d29-ficticio'/><ide>");
   if(type.equals("depth65"))xml=xml.replace("<ide>","<x>".repeat(65)+"</x>".repeat(65)+"<ide>");
   if(type.equals("XML1000001"))xml="X".repeat(1000001);
   if(type.equals("linhas201")){int start=xml.indexOf("<det "),end=xml.indexOf("</det>",start)+6;String det=xml.substring(start,end);StringBuilder many=new StringBuilder();for(int n=1;n<=201;n++)many.append(det.replace("nItem='1'","nItem='"+n+"'"));xml=xml.substring(0,start)+many+"</infNFe></NFe>";}
   d.put("xml",xml);p.post("D29 GREEN XML400 "+type,"/api/v1/pedidos-saida/xml",d,"OPERACAO",400);var after=p.get("D29 GREEN XML semnovo pedido "+type,"/api/v1/pedidos-saida"+p.ctx(),"OPERACAO");x.assertion("D29 GREEN XML lista preservada "+type,before.equals(after),"baseline5/semnovo",after);
  }
  x.assertion("D29 pedido red permanece intacto ID derivado da resposta",preserved.equals(p.get("D29 GETfinal pedidoRed preservado","/api/v1/pedidos-saida/"+preservedId,"OPERACAO")),"original intacto",preserved);x.ids.put("redXIncludePedidoPreservado",preservedId);x.ids.put("redGreenBackend","NfeXmlService namespaceXInclude recusado; redpedido preservado; 4negativos novos semcadastros/recebimentos/reservas/calculos repetidos");x.save();
 }
}
