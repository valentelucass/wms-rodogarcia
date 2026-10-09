import tools.jackson.databind.JsonNode;
import java.nio.file.*;
import java.util.*;
import java.time.*;

/** Executa geradores reais com transporte ficticio que nao abre socket nem SQL. */
public class FixtureDocumentosOfflineD29 {
 static Map<String,Object>m(Object...v){return EnsaioD29.m(v);}
 static class Capture extends EnsaioD29 {
  final List<JsonNode> notes=new ArrayList<>();
  Capture()throws Exception{super("FICTICIO_SEM_SEGREDO");clientId=1;warehouseId=2;product=json.valueToTree(m("id",3,"sku","D29FICTICIO"));ids.put("produtoId",3L);ids.put("embalagemId",4L);}
  @Override JsonNode call(String n,String method,String route,Object payload,String role,int expected){var data=json.valueToTree(payload);if(route.endsWith("/notas"))notes.add(data);if(route.equals("/api/v1/cargas-iniciais"))notes.add(data.path("dados").path("nota"));return json.valueToTree(m("id",1,"pedido",m("id",1,"versao",0)));}
 }
 public static void main(String[]a)throws Exception {
  var x=new Capture();new JornadasD29(x).entry("MANUAL",1);var p=new PendenciasD29(x);var xml=p.xmlPayload(x.nextDocumentNumber(),"2","3");var matcher=java.util.regex.Pattern.compile("<nNF>([0-9]+)</nNF>").matcher(xml.get("xml").toString());if(!matcher.find())throw new AssertionError("nNF");long xmlNumber=Long.parseLong(matcher.group(1));new RegressaoD29(x).criarCarga("LOAD",1);var numbers=List.of(x.notes.get(0).path("numero").asLong(),xmlNumber,x.notes.get(1).path("numero").asLong());if(new HashSet<>(numbers).size()!=3)throw new AssertionError("colisao manual/XML/carga");
  var resumed=new Capture();resumed.recoverDocumentSequence(x.json.valueToTree(x.notes));new JornadasD29(resumed).entry("NOVO",1);long next=resumed.notes.getFirst().path("numero").asLong();if(numbers.contains(next)||next<=Collections.max(numbers))throw new AssertionError("retomada colide");
  var archive=x.json.readTree(Files.readAllBytes(Path.of("target-d29-helper/versao-atual.json")));var result=m("utc",Instant.now().toString(),"modalidade","geradores atuais isolados com transporte ficticio; sem HTTP SQL listener","versao",archive,"casos",List.of(m("caso","manual/XML/carga mesmo contexto sem colisao","numeros",numbers,"aprovado",true),m("caso","retomada captura anterior e novo gerador","numeroNovo",next,"aprovado",true)),"falhas",0);
  Files.writeString(Path.of("evidencias/d29-fixture-documentos-offline-"+archive.path("hashConjunto").asString().substring(0,16)+".json"),x.json.writerWithDefaultPrettyPrinter().writeValueAsString(result));System.out.println("D29 geradores documentos offline2/0, sem HTTP/SQL/listener");
 }
}
