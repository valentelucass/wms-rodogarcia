import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.HexFormat;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/** Helper de configuração: stdin privado, stdout capturado pelo PowerShell e cifrado com DPAPI. */
class PrepararLogin {
    public static void main(String[] args) throws Exception {
        var input = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
        char[] senha = input.readLine().toCharArray();
        if (senha.length < 12 || senha.length > 128) System.exit(2);
        byte[] salt = new byte[16]; new SecureRandom().nextBytes(salt);
        var spec = new PBEKeySpec(senha, salt, 600000, 256);
        byte[] hash = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        spec.clearPassword(); Arrays.fill(senha, '\0');
        var generator = KeyPairGenerator.getInstance("RSA"); generator.initialize(3072);
        byte[] key = generator.generateKeyPair().getPrivate().getEncoded();
        System.out.print("{\"schema\":1,\"privateKey\":\"" + Base64.getEncoder().encodeToString(key)
                + "\",\"bootstrapHash\":\"{pbkdf2-600k}" + HexFormat.of().formatHex(salt)
                + HexFormat.of().formatHex(hash) + "\"}");
        Arrays.fill(key, (byte) 0); Arrays.fill(hash, (byte) 0);
    }
}
