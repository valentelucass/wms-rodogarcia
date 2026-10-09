package br.com.rodogarcia.wms;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockingDetails;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import br.com.rodogarcia.wms.config.IdentificacaoOperacaoFilter;
import br.com.rodogarcia.wms.controllers.EnderecoController;
import br.com.rodogarcia.wms.controllers.ProdutoController;
import br.com.rodogarcia.wms.dto.EnderecoDto;
import br.com.rodogarcia.wms.dto.ProdutoDto;
import br.com.rodogarcia.wms.exceptions.ApiExceptionHandler;
import br.com.rodogarcia.wms.exceptions.ProblemasApi;
import br.com.rodogarcia.wms.models.SituacaoCadastro;
import br.com.rodogarcia.wms.models.TipoEndereco;
import br.com.rodogarcia.wms.models.TipoQuantidade;
import br.com.rodogarcia.wms.models.TipoUnidadeLogistica;
import br.com.rodogarcia.wms.services.EnderecoService;
import br.com.rodogarcia.wms.services.ProdutoService;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/** Fronteiras Integer nao equivalem a Long ou quantidade decimal. */
class D30IntegerFracaoTest {
    @ParameterizedTest
    @CsvSource({
        "produto,precisaoQuantidade,3.9",
        "endereco,nivel,1.9",
        "endereco,sequenciaColeta,0.9",
        "endereco,empilhamentoMaximo,1.9"
    })
    void inteiroFracionarioRecusaSemTruncarNemChamarNegocio(
            String recurso, String campo, String literal) throws Exception {
        var mapper = D30LongFracaoTest.mapperLocal();
        var produtos = mock(ProdutoService.class);
        var enderecos = mock(EnderecoService.class);
        var data = Instant.parse("2026-09-01T12:00:00Z");
        when(produtos.criar(any()))
                .thenReturn(
                        new ProdutoDto.Resposta(
                                22L,
                                0L,
                                SituacaoCadastro.ATIVO,
                                data,
                                null,
                                1L,
                                "D30",
                                "Produto ficticio D30",
                                "UN",
                                TipoQuantidade.MEDIDA,
                                3,
                                false,
                                false,
                                null));
        when(enderecos.criar(any()))
                .thenReturn(
                        new EnderecoDto.Resposta(
                                33L,
                                0L,
                                SituacaoCadastro.ATIVO,
                                data,
                                null,
                                2L,
                                "A101",
                                "A",
                                1,
                                "01",
                                "Endereco ficticio D30",
                                TipoEndereco.ARMAZENAGEM,
                                new BigDecimal("100"),
                                new BigDecimal("2"),
                                BigDecimal.ONE,
                                BigDecimal.ONE,
                                1,
                                0,
                                TipoUnidadeLogistica.PALLET));
        var mvc =
                MockMvcBuilders.standaloneSetup(
                                new ProdutoController(produtos), new EnderecoController(enderecos))
                        .setMessageConverters(new JacksonJsonHttpMessageConverter(mapper))
                        .setControllerAdvice(new ApiExceptionHandler(new ProblemasApi(mapper)))
                        .addFilters(new IdentificacaoOperacaoFilter())
                        .build();
        var dados = new LinkedHashMap<String, Object>();
        boolean produto = recurso.equals("produto");
        if (produto) {
            dados.putAll(
                    Map.of(
                            "clienteId",
                            1L,
                            "sku",
                            "D30",
                            "descricao",
                            "Produto ficticio D30",
                            "unidadeMedida",
                            "UN",
                            "tipoQuantidade",
                            "MEDIDA",
                            "precisaoQuantidade",
                            3,
                            "controlaLote",
                            false,
                            "controlaValidade",
                            false));
        } else {
            dados.putAll(
                    Map.of(
                            "armazemId",
                            2L,
                            "codigo",
                            "A101",
                            "rua",
                            "A",
                            "nivel",
                            1,
                            "posicao",
                            "01",
                            "descricao",
                            "Endereco ficticio D30",
                            "tipo",
                            "ARMAZENAGEM"));
            dados.putAll(
                    Map.of(
                            "capacidadePesoKg",
                            new BigDecimal("100"),
                            "alturaMetros",
                            new BigDecimal("2"),
                            "larguraMetros",
                            BigDecimal.ONE,
                            "profundidadeMetros",
                            BigDecimal.ONE,
                            "empilhamentoMaximo",
                            1,
                            "sequenciaColeta",
                            0));
        }
        dados.put(campo, new BigDecimal(literal));
        String body = mapper.writeValueAsString(dados);
        Object[] lido = new Object[1];
        var erro =
                catchThrowable(
                        () ->
                                lido[0] =
                                        produto
                                                ? mapper.readValue(body, ProdutoDto.Criar.class)
                                                : mapper.readValue(body, EnderecoDto.Criar.class));
        var response =
                mvc.perform(
                                post(produto ? "/api/v1/produtos" : "/api/v1/enderecos")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(body))
                        .andReturn()
                        .getResponse();
        Integer convertido = null;
        if (lido[0] instanceof ProdutoDto.Criar p) convertido = p.precisaoQuantidade();
        if (lido[0] instanceof EnderecoDto.Criar e)
            convertido =
                    switch (campo) {
                        case "nivel" -> e.nivel();
                        case "sequenciaColeta" -> e.sequenciaColeta();
                        case "empilhamentoMaximo" -> e.empilhamentoMaximo();
                        default -> throw new IllegalStateException("Vetor Integer desconhecido");
                    };
        var proof = new LinkedHashMap<String, Object>();
        proof.put("id", "D30-C-CASO-INTEGER-FRACAO-001");
        proof.put("recurso", recurso);
        proof.put("campo", campo);
        proof.put("entradaLiteral", literal);
        proof.put("integerConvertido", convertido);
        proof.put("mapperErroTipo", erro == null ? null : erro.getClass().getName());
        proof.put("HTTP", response.getStatus());
        proof.put(
                "chamadasNegocio",
                mockingDetails(produtos).getInvocations().size()
                        + mockingDetails(enderecos).getInvocations().size());
        proof.put("SQLServer", false);
        var dir = Path.of(System.getProperty("wms.test.evidencias.dir"));
        Files.createDirectories(dir);
        Files.writeString(
                dir.resolve("d30-cedro-integer-fracao-" + UUID.randomUUID() + ".json"),
                mapper.writerWithDefaultPrettyPrinter().writeValueAsString(proof),
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE_NEW,
                StandardOpenOption.WRITE);
        assertThat(response.getStatus()).isEqualTo(400);
        assertThat(erro).isNotNull();
        assertThat(lido[0]).isNull();
        assertThat(response.getContentAsString())
                .doesNotContain(
                        "Produto ficticio",
                        "Endereco ficticio",
                        "ArithmeticException",
                        "br.com",
                        "trace");
        verifyNoInteractions(produtos, enderecos);
    }
}
