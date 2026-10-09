import com.microsoft.sqlserver.jdbc.SQLServerDataSource;
import java.nio.file.*;
import java.net.*;
import java.time.*;
import java.util.*;

/** Transporte TCP ficticio sem resposta, nao SQLServer; sem segredo real/SQL/HTTP negocio. */
public class SqlTimeoutOfflineD29 {
 public static void main(String[]args)throws Exception {
  var json=new EnsaioD29("FICTICIO").json;var archive=json.readTree(Files.readAllBytes(Path.of("target-d29-helper/versao-atual.json")));long started=System.nanoTime();String err="SEM_RECUSA";boolean accepted=false;
  try(var listener=new ServerSocket(0,1,InetAddress.getByName("127.0.0.1"))) {
   var peer=new java.util.concurrent.atomic.AtomicReference<Socket>();var receiver=Thread.ofVirtual().start(()->{try{peer.set(listener.accept());Thread.sleep(30000);}catch(Exception expected){}finally{if(peer.get()!=null)try{peer.get().close();}catch(Exception ignored){}}});
   var ds=new SQLServerDataSource();ds.setServerName("127.0.0.1");ds.setPortNumber(listener.getLocalPort());ds.setDatabaseName("WMS_DEV");ds.setUser("WMSDEV");ds.setPassword("D29_FICTICIO_OFFLINE_NAO_E_CREDENCIAL_REAL_32");ds.setEncrypt("true");ds.setTrustServerCertificate(false);EnsaioD29.guardTransport(ds);
   try(var connection=ds.getConnection()){err="CONEXAO_INDEVIDA";}catch(Exception e){err=e.getClass().getSimpleName();}finally{accepted=peer.get()!=null;if(peer.get()!=null)peer.get().close();receiver.interrupt();receiver.join(2000);}
  }
  double ms=(System.nanoTime()-started)/1_000_000.0;boolean pass=accepted&&!err.equals("SEM_RECUSA")&&!err.equals("CONEXAO_INDEVIDA")&&ms>=9000&&ms<25000;var doc=EnsaioD29.m("demanda","D29","utc",Instant.now().toString(),"helperHash",archive.path("hashConjunto").asString(),"checks",1,"falhas",pass?0:1,"caso","TCP aceito sem resposta prelogin termina com prazo","esperado","falha de transporte em ate25s, nenhuma conexao/SQL","classeObtida",err,"duracaoMs",ms,"tcpAceito",accepted,"SQLServerReal",false,"SQL",false,"APInegocio",false,"credencialReal",false,"listenerFicticioEncerrado",true);Files.writeString(Path.of("evidencias/d29-sql-timeout-offline-"+archive.path("hashConjunto").asString()+".json"),json.writerWithDefaultPrettyPrinter().writeValueAsString(doc));System.out.println("D29 transporte ficticio "+(pass?"1/0":"1/1"));if(!pass)throw new AssertionError("D29_TIMEOUT_TRANSPORTE_RED_PRESERVADO");
 }
}
