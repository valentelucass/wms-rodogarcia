import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import org.flywaydb.core.api.Location;
import org.flywaydb.core.internal.resource.filesystem.FileSystemResource;
import org.flywaydb.core.internal.resolver.ChecksumCalculator;

// Apenas leitura local de fontes; sem configuracao/conexao/Flyway runtime.
class PrumoDev02ChecksumLocal {
    public static void main(String[] args) {
        Path file = Path.of(args[0]).toAbsolutePath();
        var resource = new FileSystemResource(new Location("filesystem:" + file.getParent()),
                file.toString(), StandardCharsets.UTF_8, false);
        System.out.println(ChecksumCalculator.calculate(resource));
    }
}
