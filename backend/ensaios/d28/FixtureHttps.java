import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsServer;

import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.nio.file.*;
import java.security.*;
import java.security.cert.*;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.*;
import java.time.format.DateTimeFormatter;

import javax.net.ssl.*;

/** TLS privado em memória; arquivo de confiança contém somente certificado público. */
final class FixtureHttps implements AutoCloseable {
    final HttpsServer server;
    final RSAKey jwt;
    final String issuer;
    final Path trustStore;

    FixtureHttps(Path work) throws Exception {
        var gen = KeyPairGenerator.getInstance("RSA");
        gen.initialize(2048);
        var tls = gen.generateKeyPair();
        var signing = gen.generateKeyPair();
        jwt =
                new RSAKey.Builder((RSAPublicKey) signing.getPublic())
                        .privateKey((RSAPrivateKey) signing.getPrivate())
                        .keyID("d28-ephemeral")
                        .build();
        var cert = certificate(tls);
        cert.verify(tls.getPublic());
        cert.checkValidity();
        var keys = KeyStore.getInstance("PKCS12");
        keys.load(null, null);
        keys.setKeyEntry(
                "tls",
                tls.getPrivate(),
                "ephemeral".toCharArray(),
                new java.security.cert.Certificate[] {cert});
        var km = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        km.init(keys, "ephemeral".toCharArray());
        var ctx = SSLContext.getInstance("TLS");
        ctx.init(km.getKeyManagers(), null, new SecureRandom());
        var trust = KeyStore.getInstance("PKCS12");
        trust.load(null, null);
        trust.setCertificateEntry("d28-loopback", cert);
        trustStore = work.resolve("issuer-public.p12");
        try (var out = Files.newOutputStream(trustStore, StandardOpenOption.CREATE_NEW)) {
            trust.store(out, "d28-public-cert".toCharArray());
        }
        server = HttpsServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.setHttpsConfigurator(new HttpsConfigurator(ctx));
        issuer = "https://127.0.0.1:" + server.getAddress().getPort();
        server.createContext(
                "/jwks",
                x -> {
                    var body =
                            new JWKSet(jwt.toPublicJWK())
                                    .toString()
                                    .getBytes(java.nio.charset.StandardCharsets.UTF_8);
                    x.getResponseHeaders().set("Content-Type", "application/json");
                    x.sendResponseHeaders(200, body.length);
                    try (var out = x.getResponseBody()) {
                        out.write(body);
                    }
                });
        server.start();
    }

    public void close() {
        server.stop(0);
    }

    static byte[] der(int tag, byte[] value) {
        var out = new ByteArrayOutputStream();
        out.write(tag);
        if (value.length < 128) out.write(value.length);
        else {
            int count = value.length > 65535 ? 3 : value.length > 255 ? 2 : 1;
            out.write(128 | count);
            for (int i = count - 1; i >= 0; i--) out.write(value.length >> (8 * i) & 255);
        }
        out.writeBytes(value);
        return out.toByteArray();
    }

    static byte[] join(byte[]... parts) {
        var out = new ByteArrayOutputStream();
        for (var p : parts) out.writeBytes(p);
        return out.toByteArray();
    }

    static byte[] seq(byte[]... p) {
        return der(0x30, join(p));
    }

    static byte[] oid(int... p) {
        byte[] b = new byte[p.length];
        for (int i = 0; i < p.length; i++) b[i] = (byte) p[i];
        return der(6, b);
    }

    static X509Certificate certificate(KeyPair key) throws Exception {
        var alg =
                seq(
                        oid(
                                (byte) 0x2a,
                                (byte) 0x86,
                                0x48,
                                (byte) 0x86,
                                (byte) 0xf7,
                                0x0d,
                                1,
                                1,
                                0x0b),
                        der(5, new byte[0]));
        var name =
                seq(
                        der(
                                0x31,
                                seq(
                                        oid(0x55, 4, 3),
                                        der(
                                                12,
                                                "localhost"
                                                        .getBytes(
                                                                java.nio.charset.StandardCharsets
                                                                        .UTF_8)))));
        var time = DateTimeFormatter.ofPattern("yyMMddHHmmss'Z'").withZone(ZoneOffset.UTC);
        var validity =
                seq(
                        der(23, time.format(Instant.now().minusSeconds(60)).getBytes()),
                        der(23, time.format(Instant.now().plusSeconds(86400)).getBytes()));
        var san = seq(der(0x87, new byte[] {127, 0, 0, 1}), der(0x82, "localhost".getBytes()));
        var ext = der(0xa3, seq(seq(oid(0x55, 0x1d, 0x11), der(4, san))));
        var tbs =
                seq(
                        der(0xa0, der(2, new byte[] {2})),
                        der(
                                2,
                                new BigInteger(80, new SecureRandom())
                                        .add(BigInteger.ONE)
                                        .toByteArray()),
                        alg,
                        name,
                        validity,
                        name,
                        key.getPublic().getEncoded(),
                        ext);
        var sig = Signature.getInstance("SHA256withRSA");
        sig.initSign(key.getPrivate());
        sig.update(tbs);
        return (X509Certificate)
                CertificateFactory.getInstance("X.509")
                        .generateCertificate(
                                new java.io.ByteArrayInputStream(
                                        seq(tbs, alg, der(3, join(new byte[] {0}, sig.sign())))));
    }
}
