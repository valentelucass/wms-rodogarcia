import java.time.*;
import java.nio.file.*;
import java.util.*;

/** Sem segredo, SQL, listener ou transporte HTTP: recusas antes de URI/cliente. */
public class GuardasOfflineD29 {
 public static void main(String[]args)throws Exception{
  var x=new EnsaioD29("FICTICIO_SEM_CREDENCIAL");var cases=new ArrayList<Map<String,Object>>();
  for(var key:List.of("emissao","chegouEm","chegadaReal","dataFifo","ocorridaEm","executadoEm","observadoEm")){
   Object value=key.equals("emissao")?LocalDate.now().minusDays(1).toString():Instant.now().minusSeconds(86400).toString();
   String expected=key.equals("emissao")?"D29_DATA_EMISSAO_NAO_ATUAL_RECUSADA_ANTES_HTTP":"D29_FATO_TEMPORAL_NAO_ATUAL_RECUSADO_ANTES_HTTP";String got="SEM_RECUSA";
   try{x.callToken("offlinecampo"+key,"POST","/api/v1/FICTICIO",EnsaioD29.m("nested",List.of(EnsaioD29.m(key,value))),"GESTOR",null,200);}catch(IllegalStateException e){got=e.getMessage();}
   boolean pass=expected.equals(got)&&x.cases.isEmpty()&&x.app==null&&x.fixture==null;cases.add(EnsaioD29.m("campo",key,"esperado",expected,"obtido",got,"semHTTP",x.cases.isEmpty(),"aprovado",pass));if(!pass)throw new AssertionError(key);
  }
  x.guardBusinessDates(x.json.valueToTree(EnsaioD29.m("emissao",LocalDate.now().toString(),"chegouEm",Instant.now().toString())));
  cases.add(EnsaioD29.m("caso","controlepositivo datas atuais","aprovado",true));
  String got="SEM_RECUSA";try{x.guardBusinessDates(x.json.valueToTree(EnsaioD29.m("chegouEm",Instant.now().plusSeconds(86400).toString())));}catch(IllegalStateException e){got=e.getMessage();}
  if(!got.equals("D29_FATO_TEMPORAL_NAO_ATUAL_RECUSADO_ANTES_HTTP"))throw new AssertionError("futuro");cases.add(EnsaioD29.m("caso","futurofisicorecusado","aprovado",true));
  var captureAt=Instant.now().minusSeconds(7200);var real=x.json.valueToTree(EnsaioD29.m("chegadaReal",captureAt.minusSeconds(2).toString(),"dataFifo",captureAt.minusSeconds(2).toString()));
  x.preserveCapturedDates(real,real,captureAt);x.guardBusinessDates(real);cases.add(EnsaioD29.m("caso","copia fiel de fato atual na captura anterior nao altera clock","aprovado",true));
  boolean rejected=false;try{x.preserveCapturedDates(real,x.json.valueToTree(EnsaioD29.m("chegadaReal",captureAt.minusSeconds(3).toString())),captureAt);}catch(IllegalStateException e){rejected=e.getMessage().equals("D29_ORIGEM_TEMPORAL_RETORNO_DIVERGENTE");}if(!rejected)throw new AssertionError("copiaalterada");cases.add(EnsaioD29.m("caso","GET alterado contra captura original recusado","aprovado",true));
  rejected=false;var fake=x.json.valueToTree(EnsaioD29.m("chegadaReal",captureAt.minusSeconds(86400).toString()));try{x.preserveCapturedDates(fake,fake,captureAt);}catch(IllegalStateException e){rejected=e.getMessage().equals("D29_ORIGEM_TEMPORAL_NAO_REAL_NA_CAPTURA");}if(!rejected)throw new AssertionError("passadofabricado");cases.add(EnsaioD29.m("caso","fato antigo na captura original nao autoriza excecao","aprovado",true));
  var archive=x.json.readTree(Files.readAllBytes(Path.of("target-d29-helper/versao-atual.json")));
  Files.writeString(Path.of("evidencias/d29-guardas-datas-offline-"+archive.path("hashConjunto").asString().substring(0,16)+".json"),x.json.writerWithDefaultPrettyPrinter().writeValueAsString(EnsaioD29.m("demanda","D29","modalidade","isolado sem HTTP SQL app","utc",Instant.now().toString(),"versao",archive,"casos",cases,"falhas",0)));
  System.out.println("D29 guardas temporais offline"+cases.size()+"/0 semHTTP/SQL/listener");
 }
}
