import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.net.*;
import java.net.http.*;
import java.security.*;
import javax.net.ssl.*;

/** CREATE_NEW real e controle TLS privado; nao importa credencial, SQL ou API negocio. */
public class TlsCreateNewOfflineD29 {
 public static void main(String[]args)throws Exception {
  var json=new EnsaioD29("FICTICIO").json;var work=Path.of("target-d29-helper","tls-offline-"+UUID.randomUUID());Files.createDirectories(work);var file=work.resolve("issuer-public.p12");byte[]marker="D29 ARQUIVO PUBLICO FICTICIO PREEXISTENTE".getBytes(java.nio.charset.StandardCharsets.UTF_8);Files.write(file,marker,StandardOpenOption.CREATE_NEW);boolean rejected=false;String err="nenhum";try(var fixture=new FixtureHttps(work)){throw new AssertionError("D29_CREATE_NEW_NAO_RECUSOU");}catch(FileAlreadyExistsException e){rejected=true;err=e.getClass().getSimpleName();}
  var archive=json.readTree(Files.readAllBytes(Path.of("target-d29-helper/versao-atual.json")));var report=Path.of("evidencias/d29-tls-offline-"+archive.path("hashConjunto").asString()+".json");var rows=new ArrayList<Map<String,Object>>();rows.add(EnsaioD29.m("caso","CREATE_NEW recusa sem alterar arquivo", "esperado","FileAlreadyExistsException/bytesiguais","obtido",err,"aprovado",rejected&&Arrays.equals(marker,Files.readAllBytes(file))));var doc=EnsaioD29.m("demanda","D29","utc",Instant.now().toString(),"fase","FALHA_CREATE_NEW_ANTES_CONTROLE_POSITIVO","pid",ProcessHandle.current().pid(),"criadoUtc",ProcessHandle.current().info().startInstant().map(Instant::toString).orElse(null),"helperHash",archive.path("hashConjunto").asString(),"SQL",false,"APInegocio",false,"segredos",false,"cases",rows);Files.writeString(report,json.writerWithDefaultPrettyPrinter().writeValueAsString(doc));System.out.println("D29 CREATE_NEW recusado PID="+ProcessHandle.current().pid());Thread.sleep(12000);
  var good=work.resolve("controle");Files.createDirectories(good);int port;
  try(var fixture=new FixtureHttps(good)) {port=fixture.server.getAddress().getPort();var ks=KeyStore.getInstance("PKCS12");try(var in=Files.newInputStream(fixture.trustStore)){ks.load(in,"d29-public-cert".toCharArray());}var tm=TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());tm.init(ks);var ctx=SSLContext.getInstance("TLS");ctx.init(null,tm.getTrustManagers(),new SecureRandom());var client=HttpClient.newBuilder().sslContext(ctx).connectTimeout(Duration.ofSeconds(3)).build();var r=client.send(HttpRequest.newBuilder(URI.create(fixture.issuer+"/jwks")).GET().timeout(Duration.ofSeconds(3)).build(),HttpResponse.BodyHandlers.ofString());var jwks=json.readTree(r.body());rows.add(EnsaioD29.m("caso","controle TLS hostname privado/JWKS publico", "esperado","200/keyRSA/semprivado/semcookie","aprovado",r.statusCode()==200&&jwks.path("keys").size()==1&&!jwks.path("keys").get(0).has("d")&&!r.headers().firstValue("Set-Cookie").isPresent()));}
  long failures=rows.stream().filter(r->!Boolean.TRUE.equals(r.get("aprovado"))).count();doc.putAll(EnsaioD29.m("fase","ENCERRADO","controlePorta",port,"checks",rows.size(),"falhas",failures,"fimUtc",Instant.now().toString()));Files.writeString(report,json.writerWithDefaultPrettyPrinter().writeValueAsString(doc));System.out.println("D29 TLS offline "+rows.size()+"/"+failures);if(failures!=0)throw new AssertionError("D29_TLS_OFFLINE_RED");
 }
}
