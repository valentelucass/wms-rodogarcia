package br.com.rodogarcia.wms.services;

import br.com.rodogarcia.wms.dto.EnderecoDto;
import br.com.rodogarcia.wms.dto.ImportacaoEnderecoDto;
import br.com.rodogarcia.wms.models.TipoEndereco;
import br.com.rodogarcia.wms.models.TipoUnidadeLogistica;
import jakarta.validation.Validator;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.Serial;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipInputStream;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/** Leitura sem avaliar fórmulas e sem acesso a conteúdo externo. */
@Service
@ConditionalOnProperty(name = "wms.cadastros.enabled", havingValue = "true")
public class ExcelEnderecoService {
    private static final Set<String> OBRIGATORIAS =
            Set.of("codigo", "rua", "nivel", "posicao", "descricao", "tipo", "sequenciaColeta");
    private static final Set<String> OPCIONAIS =
            Set.of(
                    "tipoUnidadePermitido",
                    "capacidadePesoKg",
                    "alturaMetros",
                    "larguraMetros",
                    "profundidadeMetros",
                    "empilhamentoMaximo");
    private final Validator validator;
    private final int bytesMax;
    private final int linhasMax;
    private final long expandidoMax;

    public ExcelEnderecoService(
            Validator validator,
            @Value("${wms.importacao-enderecos.bytes-max:5242880}") int bytesMax,
            @Value("${wms.importacao-enderecos.linhas-max:5000}") int linhasMax,
            @Value("${wms.importacao-enderecos.expandido-max:52428800}") long expandidoMax) {
        this.validator = validator;
        if (bytesMax < 1 || linhasMax < 1 || linhasMax > 5000 || expandidoMax < 1)
            throw new IllegalArgumentException("Limites XLSX incompatíveis com o contrato/schema.");
        this.bytesMax = bytesMax;
        this.linhasMax = linhasMax;
        this.expandidoMax = expandidoMax;
    }

    public record Lote(
            List<ImportacaoEnderecoDto.Linha> linhas,
            List<ImportacaoEnderecoDto.Erro> erros,
            int quantidade) {}

    public Lote ler(Long armazem, byte[] bytes) {
        if (bytes == null || bytes.length == 0 || bytes.length > bytesMax)
            throw CadastroSupport.invalido("Arquivo XLSX vazio ou acima do limite configurado.");
        conferirZip(bytes);
        var linhas = new ArrayList<ImportacaoEnderecoDto.Linha>();
        var erros = new ArrayList<ImportacaoEnderecoDto.Erro>();
        int quantidade = 0;
        try (var workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            if (workbook.getNumberOfSheets() != 1
                    || !workbook.getSheetName(0).equals("Enderecos")
                    || !workbook.getExternalLinksTables().isEmpty())
                throw CadastroSupport.invalido(
                        "Use somente a aba Enderecos, sem vínculos externos.");
            var sheet = workbook.getSheetAt(0);
            if (sheet.getLastRowNum() > linhasMax)
                throw CadastroSupport.invalido(
                        "Arquivo excede o limite de linhas; nenhum lote é truncado.");
            var header = sheet.getRow(0);
            if (header == null) throw CadastroSupport.invalido("Cabeçalho ausente.");
            var colunas = new HashMap<String, Integer>();
            for (Cell c : header) {
                if (c.getHyperlink() != null)
                    throw CadastroSupport.invalido("Cabeçalhos não aceitam hyperlinks.");
                String valor = texto(c, false);
                if (valor.isBlank()
                        || (!OBRIGATORIAS.contains(valor) && !OPCIONAIS.contains(valor))
                        || colunas.put(valor, c.getColumnIndex()) != null)
                    throw CadastroSupport.invalido(
                            "Cabeçalhos únicos/exatos e conhecidos são obrigatórios.");
            }
            if (!colunas.keySet().containsAll(OBRIGATORIAS))
                throw CadastroSupport.invalido("Cabeçalhos obrigatórios ausentes.");
            for (int r = 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null || vazia(row)) continue;
                quantidade++;
                int linha = r + 1;
                try {
                    for (Cell cell : row) {
                        if (cell.getHyperlink() != null)
                            throw new LinhaInvalida(
                                    "celula",
                                    "HYPERLINK_RECUSADO",
                                    "Hyperlinks não são aceitos, inclusive em descrições.");
                        if (cell.getCellType() == CellType.FORMULA
                                || cell.getCellType() == CellType.ERROR)
                            throw new LinhaInvalida(
                                    "celula", "FORMULA_OU_ERRO", "Fórmulas/erros não são aceitos.");
                        if (!colunas.containsValue(cell.getColumnIndex())
                                && cell.getCellType() != CellType.BLANK)
                            throw new LinhaInvalida(
                                    "celula", "COLUNA_DESCONHECIDA", "Conteúdo fora do cabeçalho.");
                    }
                    var e =
                            new EnderecoDto.Criar(
                                    armazem,
                                    identidade(row, colunas, "codigo"),
                                    identidade(row, colunas, "rua"),
                                    inteiro(row, colunas, "nivel", false),
                                    identidade(row, colunas, "posicao"),
                                    valor(row, colunas, "descricao"),
                                    enumerado(
                                            TipoEndereco.class, valor(row, colunas, "tipo"), false),
                                    numero(row, colunas, "capacidadePesoKg"),
                                    numero(row, colunas, "alturaMetros"),
                                    numero(row, colunas, "larguraMetros"),
                                    numero(row, colunas, "profundidadeMetros"),
                                    inteiro(row, colunas, "empilhamentoMaximo", true),
                                    inteiro(row, colunas, "sequenciaColeta", false));
                    var tipo =
                            enumerado(
                                    TipoUnidadeLogistica.class,
                                    valor(row, colunas, "tipoUnidadePermitido"),
                                    true);
                    var violacoes = validator.validate(e);
                    for (var v : violacoes)
                        erros.add(
                                new ImportacaoEnderecoDto.Erro(
                                        linha,
                                        v.getPropertyPath().toString(),
                                        "DADOS_INVALIDOS",
                                        v.getMessage()));
                    if (tipo != null
                            && (e.capacidadePesoKg() == null
                                    || e.alturaMetros() == null
                                    || e.larguraMetros() == null
                                    || e.profundidadeMetros() == null
                                    || e.empilhamentoMaximo() == null))
                        erros.add(
                                new ImportacaoEnderecoDto.Erro(
                                        linha,
                                        "tipoUnidadePermitido",
                                        "CAPACIDADE_INCOMPLETA",
                                        "Tipo permitido exige todos os limites explícitos."));
                    if (violacoes.isEmpty())
                        linhas.add(new ImportacaoEnderecoDto.Linha(linha, e, tipo));
                } catch (LinhaInvalida ex) {
                    erros.add(
                            new ImportacaoEnderecoDto.Erro(
                                    linha, ex.coluna, ex.codigo, ex.getMessage()));
                } catch (IllegalArgumentException | ArithmeticException ex) {
                    erros.add(
                            new ImportacaoEnderecoDto.Erro(
                                    linha,
                                    "linha",
                                    "FORMATO_INVALIDO",
                                    "Tipo ou número inválido; não há conversão aproximada."));
                }
            }
        } catch (IllegalArgumentException | org.apache.poi.ooxml.POIXMLException ex) {
            throw CadastroSupport.invalido("Estrutura, cabeçalho ou tipo de célula XLSX inválido.");
        } catch (IOException ex) {
            throw CadastroSupport.invalido("Não foi possível ler o XLSX.");
        }
        if (quantidade == 0) throw CadastroSupport.invalido("Informe ao menos uma linha de dados.");
        return new Lote(List.copyOf(linhas), List.copyOf(erros), quantidade);
    }

    private void conferirZip(byte[] bytes) {
        try (var zip = new ZipInputStream(new ByteArrayInputStream(bytes))) {
            long total = 0;
            int entradas = 0;
            boolean conteudo = false;
            byte[] buffer = new byte[8192];
            for (var entry = zip.getNextEntry(); entry != null; entry = zip.getNextEntry()) {
                if (++entradas > 1000)
                    throw CadastroSupport.invalido("XLSX com estrutura excessiva.");
                String n = entry.getName().toLowerCase(Locale.ROOT);
                if (n.contains("vbaproject")
                        || n.contains("externallinks")
                        || n.startsWith("xl/embeddings/"))
                    throw CadastroSupport.invalido(
                            "Macros, vínculos externos e objetos incorporados não são aceitos.");
                if (n.equals("[content_types].xml")) conteudo = true;
                int l;
                while ((l = zip.read(buffer)) != -1) {
                    total += l;
                    if (total > expandidoMax)
                        throw CadastroSupport.invalido("Conteúdo descompactado excede o limite.");
                }
            }
            if (!conteudo) throw CadastroSupport.invalido("Formato XLSX obrigatório.");
        } catch (IOException e) {
            throw CadastroSupport.invalido("Arquivo ZIP/XLSX inválido.");
        }
    }

    private static boolean vazia(Row row) {
        for (Cell c : row)
            if (c.getHyperlink() != null
                    || c.getCellType() != CellType.BLANK
                            && (c.getCellType() != CellType.STRING
                                    || !c.getStringCellValue().isBlank())) return false;
        return true;
    }

    private static String texto(Cell c, boolean identidade) {
        if (c == null || c.getCellType() == CellType.BLANK) return "";
        if (c.getCellType() == CellType.STRING) return c.getStringCellValue().strip();
        if (c.getCellType() == CellType.NUMERIC && identidade) {
            String formato = c.getCellStyle().getDataFormatString();
            if (!formato.matches("0{2,}"))
                throw new LinhaInvalida(
                        "identidade",
                        "IDENTIDADE_AMBIGUA",
                        "Identidade numérica exige texto ou formato explícito que preserve zeros.");
            String apresentado = new DataFormatter(Locale.ROOT).formatCellValue(c);
            BigDecimal bruto =
                    new BigDecimal(((org.apache.poi.xssf.usermodel.XSSFCell) c).getRawValue());
            if (bruto.compareTo(new BigDecimal(apresentado)) != 0)
                throw new LinhaInvalida(
                        "identidade",
                        "IDENTIDADE_AMBIGUA",
                        "Formato de identidade não pode arredondar ou alterar o valor numérico.");
            return apresentado;
        }
        throw new LinhaInvalida(
                "celula", "TIPO_INVALIDO", "Campo textual/identidade com tipo inadequado.");
    }

    private static String valor(Row row, Map<String, Integer> cols, String n) {
        Integer col = cols.get(n);
        return col == null ? "" : texto(row.getCell(col), false);
    }

    private static String identidade(Row row, Map<String, Integer> cols, String n) {
        try {
            return CadastroSupport.codigo(texto(row.getCell(cols.get(n)), true));
        } catch (LinhaInvalida ex) {
            throw new LinhaInvalida(n, ex.codigo, ex.getMessage());
        }
    }

    private static BigDecimal numero(Row row, Map<String, Integer> cols, String n) {
        Integer col = cols.get(n);
        Cell c = col == null ? null : row.getCell(col);
        if (c == null || c.getCellType() == CellType.BLANK) return null;
        if (c.getCellType() == CellType.STRING) {
            String v = c.getStringCellValue().strip();
            return v.isEmpty() ? null : new BigDecimal(v);
        }
        if (c.getCellType() == CellType.NUMERIC)
            return new BigDecimal(((org.apache.poi.xssf.usermodel.XSSFCell) c).getRawValue());
        throw new LinhaInvalida(n, "NUMERO_INVALIDO", "Número exato obrigatório.");
    }

    private static Integer inteiro(Row row, Map<String, Integer> cols, String n, boolean opcional) {
        var v = numero(row, cols, n);
        if (v == null) {
            if (opcional) return null;
            throw new LinhaInvalida(n, "OBRIGATORIO", "Número obrigatório.");
        }
        return v.intValueExact();
    }

    private static <E extends Enum<E>> E enumerado(Class<E> tipo, String valor, boolean opcional) {
        if (valor.isEmpty() && opcional) return null;
        return Enum.valueOf(tipo, valor);
    }

    private static class LinhaInvalida extends IllegalArgumentException {
        @Serial private static final long serialVersionUID = 1L;

        private final String coluna;
        private final String codigo;

        LinhaInvalida(String coluna, String codigo, String mensagem) {
            super(mensagem);
            this.coluna = coluna;
            this.codigo = codigo;
        }
    }
}
