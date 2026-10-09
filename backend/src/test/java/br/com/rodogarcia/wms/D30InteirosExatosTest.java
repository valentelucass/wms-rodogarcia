package br.com.rodogarcia.wms;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.rodogarcia.wms.dto.EnderecoDto;
import br.com.rodogarcia.wms.dto.ProdutoDto;
import br.com.rodogarcia.wms.dto.RevisaoCadastroRequest;
import jakarta.validation.Validation;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import tools.jackson.databind.exc.MismatchedInputException;

/**
 * Oráculos literais de compatibilidade após Long/Integer: não usa o retorno para fixar esperado.
 */
class D30InteirosExatosTest {
    private static String produto(String precisao) {
        return "{\"clienteId\":1,\"sku\":\"D30\",\"descricao\":\"Ficticio D30\",\"unidadeMedida\":\"UN\",\"tipoQuantidade\":\"MEDIDA\",\"precisaoQuantidade\":"
                + precisao
                + ",\"controlaLote\":false,\"controlaValidade\":false,\"antecedenciaAvisoDias\":null}";
    }

    @ParameterizedTest
    @CsvSource({"0.0,0", "1e0,1", "9223372036854775807.0,9223372036854775807"})
    void conservaLongMatematicamenteExato(String literal, long esperado) throws Exception {
        var mapper = D30LongFracaoTest.mapperLocal();
        var dto =
                mapper.readValue(
                        "{\"versao\":" + literal + ",\"motivo\":\"Motivo ficticio D30\"}",
                        RevisaoCadastroRequest.class);
        assertThat(dto.versao()).isEqualTo(esperado);
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            assertThat(factory.getValidator().validate(dto)).isEmpty();
        }
        assertThat(mapper.writeValueAsString(dto)).contains("\"versao\":" + esperado);
        prova("Long", literal, esperado);
    }

    @ParameterizedTest
    @CsvSource({"0.0,0", "6e0,6"})
    void conservaIntegerExatoNosLimitesDeclarados(String literal, int esperado) throws Exception {
        var dto =
                D30LongFracaoTest.mapperLocal().readValue(produto(literal), ProdutoDto.Criar.class);
        assertThat(dto.precisaoQuantidade()).isEqualTo(esperado);
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            assertThat(factory.getValidator().validate(dto)).isEmpty();
        }
        prova("IntegerPrecisao", literal, esperado);
    }

    @Test
    void conservaIntegerMaxExatoNaSequenciaSemTetoNovo() throws Exception {
        var dto =
                D30LongFracaoTest.mapperLocal()
                        .readValue("{\"sequenciaColeta\":2147483647.0}", EnderecoDto.Criar.class);
        assertThat(dto.sequenciaColeta()).isEqualTo(2147483647);
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            assertThat(factory.getValidator().validateProperty(dto, "sequenciaColeta")).isEmpty();
        }
        prova("IntegerSequencia", "2147483647.0", 2147483647);
    }

    @Test
    void nullNaoViraZeroEObrigatoriedadeContinuaNaValidacao() throws Exception {
        var mapper = D30LongFracaoTest.mapperLocal();
        var revisao =
                mapper.readValue(
                        "{\"versao\":null,\"motivo\":\"Motivo ficticio D30\"}",
                        RevisaoCadastroRequest.class);
        var p = mapper.readValue(produto("null"), ProdutoDto.Criar.class);
        var e = mapper.readValue("{\"empilhamentoMaximo\":null}", EnderecoDto.Criar.class);
        assertThat(revisao.versao()).isNull();
        assertThat(p.precisaoQuantidade()).isNull();
        assertThat(p.antecedenciaAvisoDias()).isNull();
        assertThat(e.empilhamentoMaximo()).isNull();
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            assertThat(validator.validateProperty(revisao, "versao")).hasSize(1);
            assertThat(validator.validateProperty(p, "precisaoQuantidade")).hasSize(1);
            assertThat(validator.validateProperty(p, "antecedenciaAvisoDias")).isEmpty();
            assertThat(validator.validateProperty(e, "empilhamentoMaximo")).isEmpty();
        }
        prova(
                "NULL",
                "NULL_BOXED_OBRIGATORIO_E_OPCIONAL",
                "NULL_SEM_ZERO;VALIDACAO_DECLARADA_CONSERVADA");
    }

    @ParameterizedTest
    @ValueSource(strings = {"Long", "Integer"})
    void inteiroExatoForaDoDominioNaoSatura(String tipo) throws Exception {
        var mapper = D30LongFracaoTest.mapperLocal();
        if (tipo.equals("Long"))
            assertThatThrownBy(
                            () ->
                                    mapper.readValue(
                                            "{\"versao\":9223372036854775808.0}",
                                            RevisaoCadastroRequest.class))
                    .isInstanceOf(MismatchedInputException.class);
        else
            assertThatThrownBy(
                            () -> mapper.readValue(produto("2147483648.0"), ProdutoDto.Criar.class))
                    .isInstanceOf(MismatchedInputException.class);
        prova(tipo, "FORA_DOMINIO_EXATO", "ERRO_LEITURA_SEM_SATURACAO");
    }

    @Test
    void limiteNegocialDaPrecisaoNaoETransferidoAoMapper() throws Exception {
        var dto = D30LongFracaoTest.mapperLocal().readValue(produto("7.0"), ProdutoDto.Criar.class);
        assertThat(dto.precisaoQuantidade()).isEqualTo(7);
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            assertThat(factory.getValidator().validateProperty(dto, "precisaoQuantidade"))
                    .hasSize(1);
        }
        prova("IntegerPrecisao", "7.0", "INTEGER7_LIDO;MAX6_RECUSA_NA_VALIDACAO");
    }

    @Test
    void decimalMantemValorESeisCasas() throws Exception {
        var dto =
                D30LongFracaoTest.mapperLocal()
                        .readValue("{\"capacidadePesoKg\":1.900000}", EnderecoDto.Criar.class);
        assertThat(dto.capacidadePesoKg()).isEqualTo(new BigDecimal("1.900000"));
        assertThat(dto.capacidadePesoKg().scale()).isEqualTo(6);
        prova("BigDecimal", "1.900000", "1.900000;ESCALA6");
    }

    private static void prova(String tipo, String literal, Object observado) throws Exception {
        var mapper = D30LongFracaoTest.mapperLocal();
        var dir = Path.of(System.getProperty("wms.test.evidencias.dir"));
        Files.createDirectories(dir);
        Files.writeString(
                dir.resolve("d30-cedro-inteiros-exatos-" + UUID.randomUUID() + ".json"),
                mapper.writerWithDefaultPrettyPrinter()
                        .writeValueAsString(
                                Map.of(
                                        "id",
                                        "D30-C-CASO-INTEIROS-EXATOS-001",
                                        "tipo",
                                        tipo,
                                        "literal",
                                        literal,
                                        "observado",
                                        observado,
                                        "SQLServer",
                                        false,
                                        "DataSource",
                                        0)),
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE_NEW,
                StandardOpenOption.WRITE);
    }
}
