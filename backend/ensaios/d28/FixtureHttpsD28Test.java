import java.nio.file.*;
import java.net.*;
import java.net.http.*;
import java.security.*;
import javax.net.ssl.*;

/** HTTPS efêmero real e falha CREATE_NEW; mede listeners do próprio PID no Windows. */
public class FixtureHttpsD28Test {
    static int listeners() throws Exception {
        var p=new ProcessBuilder("C:/Windows/System32/WindowsPowerShell/v1.0/powershell.exe","-NoProfile","-Command","@(Get-NetTCPConnection -State Listen -OwningProcess "+ProcessHandle.current().pid()+" -ErrorAction SilentlyContinue).Count").start();
        return Integer.parseInt(EnsaioD28.processOutputLimited(p,10000).trim());
    }
    public static void main(String[] args) throws Exception {
        Path root=Path.of("target-d28-helper/https-offline-"+java.util.UUID.randomUUID());
        Files.createDirectories(root);
        int baseline=listeners();
        int port;
        boolean jwksPublico;
        try(var fixture=new FixtureHttps(root)) {
            port=fixture.server.getAddress().getPort();
            var ks=KeyStore.getInstance("PKCS12");
            try(var in=Files.newInputStream(fixture.trustStore)) { ks.load(in,"d28-public-cert".toCharArray()); }
            var tm=TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm()); tm.init(ks);
            var ctx=SSLContext.getInstance("TLS");ctx.init(null,tm.getTrustManagers(),null);
            var response=HttpClient.newBuilder().sslContext(ctx).build().send(HttpRequest.newBuilder(URI.create(fixture.issuer+"/jwks")).GET().build(),HttpResponse.BodyHandlers.ofString());
            jwksPublico=response.statusCode()==200 && response.body().contains("\"kty\":\"RSA\"") && !response.body().contains("\"d\":") && !response.body().contains("\"p\":");
        }
        boolean released;
        try(var socket=new ServerSocket()) { socket.bind(new InetSocketAddress("127.0.0.1",port));released=true; }
        int beforeFailure=listeners();
        boolean refused=false;
        try(var ignored=new FixtureHttps(root)) {} catch(FileAlreadyExistsException expected) { refused=true; }
        int afterFailure=listeners();
        boolean pass=jwksPublico && released && refused && beforeFailure==baseline && afterFailure==baseline;
        System.out.println("{\"caso\":\"HTTPS-real-e-CREATE_NEW-sem-listener\",\"SQL\":false,\"jwksPublico\":"+jwksPublico+",\"portaLiberada\":"+released+",\"falhaCreateNew\":"+refused+",\"listenersAntes\":"+beforeFailure+",\"listenersDepois\":"+afterFailure+",\"aprovado\":"+pass+"}");
        if(!pass) throw new IllegalStateException("D28_FIXTURE_HTTPS_FALHOU");
    }
}
