import java.nio.file.*;
import java.time.*;
import java.util.*;

/** Testa aguardador real com arquivos publicos ficticios, sem sinais do runtime central. */
public class HoldOfflineD29 {
 static final class Capture extends EnsaioD29 {Capture(){super("FICTICIO_SEM_SEGREDO");} @Override void save(){} }
 static Map<String,Object> signal(Capture x){return EnsaioD29.m("rodada",x.round,"DB_NAME","WMS_DEV","login","WMSDEV","leiturasConcluidas",true);}
 public static void main(String[]a)throws Exception {
  var work=Path.of("target-d29-helper","hold-offline-"+UUID.randomUUID());Files.createDirectories(work);var rows=new ArrayList<Map<String,Object>>();
  for(String bad:List.of("legado","rodada","banco","login","ack","ausente")){
   var x=new Capture();var d=signal(x);String file="d29-prumo-select-liberar-"+x.round+".json";
   if(bad.equals("legado"))file="d29-select-liberar-"+x.round+".json";if(bad.equals("rodada"))d.put("rodada","D29FFFFFFFF");if(bad.equals("banco"))d.put("DB_NAME","WMS_PROD");if(bad.equals("login"))d.put("login","sa");if(bad.equals("ack"))d.put("leiturasConcluidas",false);if(bad.equals("ausente"))d.remove("login");
   Files.writeString(work.resolve(file),x.json.writeValueAsString(d),StandardOpenOption.CREATE_NEW);String error="SEM_RECUSA";long begin=System.nanoTime();try{x.holdForSelect(work,Duration.ofMillis(80));}catch(IllegalStateException e){error=e.getMessage();}
   boolean pass=error.equals("D29_SELECT_PRUMO_PRAZO_SEM_SINAL")&&!x.ids.containsKey("prumoSelectSinal");rows.add(EnsaioD29.m("caso",bad,"esperado","timeout sem sinalaceito","obtido",error,"duracaoMs",(System.nanoTime()-begin)/1_000_000.0,"aprovado",pass));
  }
  var x=new Capture();var path=work.resolve("d29-prumo-select-liberar-"+x.round+".json");Files.writeString(path,"{\"rodada\":",StandardOpenOption.CREATE_NEW);String complete=x.json.writeValueAsString(signal(x));var writer=new Thread(()->{try{Thread.sleep(100);Files.writeString(path,complete);}catch(Exception e){throw new RuntimeException("D29_WRITER_FICTICIO_FALHOU");}},"D29-PUBLICACAO-FICTICIA");writer.start();String err=null;try{x.holdForSelect(work,Duration.ofSeconds(2));}catch(Exception e){err=e.getClass().getSimpleName();}writer.join();rows.add(EnsaioD29.m("caso","publicacao parcial seguida de sinalcompleto","esperado","aguardar e aceitar somente completo","obtido",err==null?"completoaceito":err,"aprovado",err==null&&x.ids.containsKey("prumoSelectSinal")));
  var y=new Capture();var ready=signal(y);ready.put("aprovado",false);Files.writeString(work.resolve("d29-prumo-select-liberar-"+y.round+".json"),y.json.writeValueAsString(ready),StandardOpenOption.CREATE_NEW);y.holdForSelect(work,Duration.ofSeconds(1));rows.add(EnsaioD29.m("caso","leitura concluida de red nao vira green","esperado","consumir leitura; aprovadofalse preservado","aprovado",y.json.valueToTree(y.ids.get("prumoSelectSinal")).path("aprovado").isBoolean()&&!y.json.valueToTree(y.ids.get("prumoSelectSinal")).path("aprovado").asBoolean()));
  var archive=x.json.readTree(Files.readAllBytes(Path.of("target-d29-helper/versao-atual.json")));long errors=rows.stream().filter(r->!Boolean.TRUE.equals(r.get("aprovado"))).count();var doc=EnsaioD29.m("demanda","D29","utc",Instant.now().toString(),"helperHash",archive.path("hashConjunto").asString(),"checks",rows.size(),"falhas",errors,"cases",rows,"SQL",false,"APInegocio",false,"listener",false,"segredos",false,"pastaFicticia",work.toString());Files.writeString(Path.of("evidencias/d29-hold-offline-"+archive.path("hashConjunto").asString()+".json"),x.json.writerWithDefaultPrettyPrinter().writeValueAsString(doc));System.out.println("D29 aguardador offline "+rows.size()+"/"+errors);if(errors!=0)throw new AssertionError("D29_AGUARDADOR_RED_PRESERVADO");
 }
}
