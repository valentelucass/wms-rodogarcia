package br.com.rodogarcia.wms;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.rodogarcia.wms.exceptions.RegraNegocioException;
import br.com.rodogarcia.wms.services.ExcelEnderecoService;
import jakarta.validation.Validation;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

/** Modelo efetivamente publicado no frontend, parser real e cópias em memória; sem banco/Spring. */
class ModeloEnderecosTest {
    private byte[] modelo() throws Exception {
        return Files.readAllBytes(Path.of("../frontend/public/modelos/enderecos.xlsx"));
    }

    @Test
    void modeloVazioNaoImportaPosicoesFicticias() throws Exception {
        var bytes = modelo();
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var parser = new ExcelEnderecoService(factory.getValidator(), 5242880, 5000, 52428800);
            assertThatThrownBy(() -> parser.ler(42L, bytes))
                    .isInstanceOf(RegraNegocioException.class)
                    .hasMessage("Informe ao menos uma linha de dados.");
        }
    }

    @Test
    void modeloPreenchidoPreservaZerosEUsaColunasPublicadas() throws Exception {
        var lote = preencher(false);
        assertThat(lote.erros()).isEmpty();
        assertThat(lote.linhas()).hasSize(1);
        var endereco = lote.linhas().getFirst().endereco();
        assertThat(endereco.codigo()).isEqualTo("00001");
        assertThat(endereco.rua()).isEqualTo("001");
        assertThat(endereco.posicao()).isEqualTo("002");
        assertThat(endereco.armazemId()).isEqualTo(42L);
    }

    @Test
    void tipoDeUnidadeSemLimitesRetornaErroDePrevia() throws Exception {
        var lote = preencher(true);
        assertThat(lote.erros())
                .anySatisfy(erro -> assertThat(erro.codigo()).isEqualTo("CAPACIDADE_INCOMPLETA"));
    }

    private ExcelEnderecoService.Lote preencher(boolean capacidadeIncompleta) throws Exception {
        try (var workbook = new XSSFWorkbook(new ByteArrayInputStream(modelo()));
                var output = new ByteArrayOutputStream();
                var factory = Validation.buildDefaultValidatorFactory()) {
            var row = workbook.getSheetAt(0).getRow(1);
            row.getCell(0).setCellValue("00001");
            row.getCell(1).setCellValue("001");
            row.getCell(2).setCellValue(0);
            row.getCell(3).setCellValue("002");
            row.getCell(4).setCellValue("Posição fictícia de conferência do modelo");
            row.getCell(5).setCellValue("ARMAZENAGEM");
            row.getCell(6).setCellValue(1);
            if (capacidadeIncompleta) row.getCell(7).setCellValue("BOBINA");
            workbook.write(output);
            return new ExcelEnderecoService(factory.getValidator(), 5242880, 5000, 52428800)
                    .ler(42L, output.toByteArray());
        }
    }
}
