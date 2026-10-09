package br.com.rodogarcia.wms;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.rodogarcia.wms.exceptions.RegraNegocioException;
import br.com.rodogarcia.wms.services.ExcelEnderecoService;
import jakarta.validation.Validation;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Parser real, dados fictícios e oráculos de fronteira; nenhum Spring/DataSource/HTTP. */
class D30ExcelLimitesTest {
    private static final int ARQUIVO = 5 * 1024 * 1024;
    private static final int EXPANDIDO = 50 * 1024 * 1024;

    @Test
    void arquivoCincoMiBExatoAceitaEVizinhoRecusa() throws Exception {
        byte[] valido = arquivoExato(ARQUIVO);
        assertThat(valido.length).isEqualTo(5242880);
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var service =
                    new ExcelEnderecoService(factory.getValidator(), ARQUIVO, 5000, EXPANDIDO);
            var lote = service.ler(42L, valido);
            assertThat(lote.quantidade()).isEqualTo(1);
            assertThat(lote.linhas()).hasSize(1);
            assertThat(lote.erros()).isEmpty();
            assertThat(lote.linhas().getFirst().endereco().codigo()).isEqualTo("D30-X1");
            byte[] acima = java.util.Arrays.copyOf(valido, 5242881);
            assertThatThrownBy(() -> service.ler(42L, acima))
                    .isInstanceOf(RegraNegocioException.class);
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {5000, 5001})
    void linhasDeDadosNaoContamCabecalho(int quantidade) throws Exception {
        byte[] bytes = workbook(quantidade);
        assertThat(bytes.length).isLessThan(ARQUIVO);
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var service =
                    new ExcelEnderecoService(factory.getValidator(), ARQUIVO, 5000, EXPANDIDO);
            if (quantidade == 5001) {
                assertThatThrownBy(() -> service.ler(42L, bytes))
                        .isInstanceOf(RegraNegocioException.class);
            } else {
                var lote = service.ler(42L, bytes);
                assertThat(lote.quantidade()).isEqualTo(5000);
                assertThat(lote.linhas()).hasSize(5000);
                assertThat(lote.erros()).isEmpty();
                assertThat(lote.linhas().getFirst().endereco().codigo()).isEqualTo("D30-X1");
                assertThat(lote.linhas().getLast().endereco().codigo()).isEqualTo("D30-X5000");
            }
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {1000, 1001})
    void partesZipExatasNaoSeConfundemComLinhas(int partes) throws Exception {
        var entries = partesBase();
        int i = 0;
        while (entries.size() < partes) entries.put("d30/p" + i++ + ".bin", new byte[0]);
        byte[] bytes = zip(entries, false);
        assertThat(metricas(bytes)[0]).isEqualTo(partes);
        assertThat(bytes.length).isLessThan(ARQUIVO);
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var service =
                    new ExcelEnderecoService(factory.getValidator(), ARQUIVO, 5000, EXPANDIDO);
            if (partes == 1001) {
                assertThatThrownBy(() -> service.ler(42L, bytes))
                        .isInstanceOf(RegraNegocioException.class);
            } else {
                var lote = service.ler(42L, bytes);
                assertThat(lote.linhas()).hasSize(1);
                assertThat(lote.erros()).isEmpty();
            }
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {52428800, 52428801})
    void expandidoZipTotalCinquentaMiBNaoEhArquivoComprimido(int total) throws Exception {
        var entries = partesBase();
        long base = entries.values().stream().mapToLong(b -> b.length).sum();
        byte[] filler = new byte[Math.toIntExact(total - base)];
        var random = new Random(20261008L);
        byte[] ruido = new byte[32];
        for (int i = 0; i < filler.length; i += 512) {
            random.nextBytes(ruido);
            System.arraycopy(ruido, 0, filler, i, Math.min(ruido.length, filler.length - i));
        }
        entries.put("d30/filler.bin", filler);
        byte[] bytes = zip(entries, false);
        assertThat(metricas(bytes)[1]).isEqualTo(total);
        assertThat(bytes.length).isLessThan(ARQUIVO);
        assertThat((double) bytes.length / total).isGreaterThan(0.01);
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var service =
                    new ExcelEnderecoService(factory.getValidator(), ARQUIVO, 5000, EXPANDIDO);
            if (total == 52428801) {
                assertThatThrownBy(() -> service.ler(42L, bytes))
                        .isInstanceOf(RegraNegocioException.class);
            } else {
                var lote = service.ler(42L, bytes);
                assertThat(lote.linhas()).hasSize(1);
                assertThat(lote.erros()).isEmpty();
            }
        }
    }

    @ParameterizedTest
    @ValueSource(
            strings = {"xl/vbaProject.bin", "xl/externalLinks/d30.xml", "xl/embeddings/d30.bin"})
    void partesNaoExecutaveisSaoRecusadasPeloNome(String parte) throws Exception {
        var entries = partesBase();
        entries.put(parte, new byte[] {1, 2, 3});
        byte[] bytes = zip(entries, false);
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var service =
                    new ExcelEnderecoService(factory.getValidator(), ARQUIVO, 5000, EXPANDIDO);
            assertThatThrownBy(() -> service.ler(42L, bytes))
                    .isInstanceOf(RegraNegocioException.class);
        }
    }

    static byte[] arquivoExato(int tamanho) throws Exception {
        var entries = partesBase();
        entries.put("d30/filler.bin", new byte[0]);
        int vazio = zip(entries, true).length;
        entries.put("d30/filler.bin", new byte[tamanho - vazio]);
        byte[] bytes = zip(entries, true);
        assertThat(bytes.length).isEqualTo(tamanho);
        return bytes;
    }

    private static LinkedHashMap<String, byte[]> partesBase() throws Exception {
        var entries = new LinkedHashMap<String, byte[]>();
        try (var zip = new ZipInputStream(new ByteArrayInputStream(workbook(1)))) {
            for (var e = zip.getNextEntry(); e != null; e = zip.getNextEntry()) {
                byte[] bytes = zip.readAllBytes();
                if (e.getName().equals("[Content_Types].xml")) {
                    String xml = new String(bytes, StandardCharsets.UTF_8);
                    assertThat(xml).contains("</Types>");
                    bytes =
                            xml.replace(
                                            "</Types>",
                                            "<Default Extension=\"bin\" ContentType=\"application/octet-stream\"/></Types>")
                                    .getBytes(StandardCharsets.UTF_8);
                }
                entries.put(e.getName(), bytes);
            }
        }
        return entries;
    }

    private static byte[] workbook(int quantidade) throws Exception {
        try (var w = new XSSFWorkbook();
                var out = new ByteArrayOutputStream()) {
            var sheet = w.createSheet("Enderecos");
            String[] cols = {
                "codigo", "rua", "nivel", "posicao", "descricao", "tipo", "sequenciaColeta"
            };
            var header = sheet.createRow(0);
            for (int c = 0; c < cols.length; c++) header.createCell(c).setCellValue(cols[c]);
            for (int i = 1; i <= quantidade; i++) {
                var row = sheet.createRow(i);
                String[] data = {
                    "D30-X" + i, "A", "0", "P" + i, "Endereco ficticio " + i, "ARMAZENAGEM", "0"
                };
                for (int c = 0; c < data.length; c++) row.createCell(c).setCellValue(data[c]);
            }
            w.write(out);
            return out.toByteArray();
        }
    }

    private static byte[] zip(Map<String, byte[]> entries, boolean stored) throws Exception {
        try (var out = new ByteArrayOutputStream();
                var zip = new ZipOutputStream(out)) {
            for (var p : entries.entrySet()) {
                var e = new ZipEntry(p.getKey());
                e.setTime(0);
                if (stored) {
                    var crc = new CRC32();
                    crc.update(p.getValue());
                    e.setMethod(ZipEntry.STORED);
                    e.setSize(p.getValue().length);
                    e.setCompressedSize(p.getValue().length);
                    e.setCrc(crc.getValue());
                }
                zip.putNextEntry(e);
                zip.write(p.getValue());
                zip.closeEntry();
            }
            zip.finish();
            return out.toByteArray();
        }
    }

    private static long[] metricas(byte[] bytes) throws Exception {
        long partes = 0, total = 0;
        byte[] buffer = new byte[8192];
        try (var zip = new ZipInputStream(new ByteArrayInputStream(bytes))) {
            for (var e = zip.getNextEntry(); e != null; e = zip.getNextEntry()) {
                partes++;
                for (int l; (l = zip.read(buffer)) != -1; ) total += l;
            }
        }
        return new long[] {partes, total};
    }
}
