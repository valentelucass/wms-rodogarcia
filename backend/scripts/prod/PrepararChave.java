import java.security.KeyPairGenerator;
import java.util.Arrays;
import java.util.Base64;

/** stdout privado: o chamador captura em memoria e persiste somente sob DPAPI. */
class PrepararChave {
    public static void main(String[] args) throws Exception {
        var generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(3072);
        byte[] key = generator.generateKeyPair().getPrivate().getEncoded();
        try {
            System.out.print(Base64.getEncoder().encodeToString(key));
        } finally {
            Arrays.fill(key, (byte) 0);
        }
    }
}
