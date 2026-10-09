package br.com.rodogarcia.wms;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.rodogarcia.wms.controllers.ClienteController;
import br.com.rodogarcia.wms.dto.ClienteDto;
import br.com.rodogarcia.wms.exceptions.RegraNegocioException;
import br.com.rodogarcia.wms.models.Cliente;
import br.com.rodogarcia.wms.repositories.ClienteRepository;
import br.com.rodogarcia.wms.services.AcessoService;
import br.com.rodogarcia.wms.services.AuditoriaService;
import br.com.rodogarcia.wms.services.CadastroSupport;
import br.com.rodogarcia.wms.services.ClienteService;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.stream.LongStream;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tools.jackson.databind.json.JsonMapper;

/** GET concreto e envelope genérico reais, sem rede/DataSource; não aceita todos os GETs. */
class D30PaginacaoTest {
    @ParameterizedTest
    @CsvSource({
        "0,0,0,false",
        "2147483647,1,2147483647,true",
        "21474836,100,2147483600,true",
        "21474837,100,2147483700,false"
    })
    void limitesDeOffsetETamanhoNaoExecutamConsulta(
            int pagina, int tamanho, long offset, boolean permitido) {
        assertThat((long) pagina * tamanho).isEqualTo(offset);
        if (permitido) {
            var page = CadastroSupport.pagina(pagina, tamanho);
            assertThat(page.getOffset()).isEqualTo(offset);
            assertThat(page.getPageNumber()).isEqualTo(pagina);
            assertThat(page.getPageSize()).isEqualTo(tamanho);
        } else {
            var e =
                    org.assertj.core.api.Assertions.catchThrowableOfType(
                            RegraNegocioException.class,
                            () -> CadastroSupport.pagina(pagina, tamanho));
            assertThat(e).isNotNull();
            assertThat(e.getStatus().value()).isEqualTo(400);
            assertThat(e.getCodigo()).isEqualTo("DADOS_INVALIDOS");
        }
    }

    @Test
    void defaultVinteEPaginaVaziaConservamTotalPositivoSemEscrita() throws Exception {
        JsonMapper[] mappers = new JsonMapper[1];
        new ApplicationContextRunner()
                .withInitializer(new ConfigDataApplicationContextInitializer())
                .withConfiguration(AutoConfigurations.of(JacksonAutoConfiguration.class))
                .withPropertyValues("spring.profiles.active=local")
                .run(
                        context -> {
                            assertThat(context).hasNotFailed();
                            assertThat(context.getBeansOfType(DataSource.class)).isEmpty();
                            mappers[0] = context.getBean(JsonMapper.class);
                        });
        JsonMapper mapper = mappers[0];
        Instant instante = Instant.parse("2026-09-01T00:00:00Z");
        List<Cliente> conhecidos =
                LongStream.rangeClosed(1, 21)
                        .mapToObj(
                                id -> {
                                    var c =
                                            new Cliente(
                                                    "D30-" + id,
                                                    "Cliente fictício " + id,
                                                    "DOCUMENTO-" + id,
                                                    instante);
                                    ReflectionTestUtils.setField(c, "id", id);
                                    return c;
                                })
                        .toList();
        var antes = conhecidos.stream().map(ClienteDto.Resposta::de).toList();
        ClienteRepository repository = mock(ClienteRepository.class);
        AcessoService acesso = mock(AcessoService.class);
        AuditoriaService auditoria = mock(AuditoriaService.class);
        when(acesso.gestor()).thenReturn(true);
        when(repository.findAll(any(Pageable.class)))
                .thenAnswer(
                        call -> {
                            Pageable page = call.getArgument(0);
                            return new PageImpl<>(
                                    page.getPageNumber() == 0
                                            ? conhecidos.subList(0, 20)
                                            : List.of(),
                                    page,
                                    21);
                        });
        var service = new ClienteService(repository, acesso, auditoria, Clock.systemUTC());
        var mvc =
                MockMvcBuilders.standaloneSetup(new ClienteController(service))
                        .setMessageConverters(new JacksonJsonHttpMessageConverter(mapper))
                        .build();

        var primeira =
                mapper.readTree(
                        mvc.perform(get("/api/v1/clientes"))
                                .andExpect(status().isOk())
                                .andReturn()
                                .getResponse()
                                .getContentAsString());
        assertThat(primeira.get("pagina").intValue()).isZero();
        assertThat(primeira.get("tamanho").intValue()).isEqualTo(20);
        assertThat(primeira.get("totalItens").longValue()).isEqualTo(21);
        assertThat(primeira.get("totalPaginas").intValue()).isEqualTo(2);
        assertThat(primeira.get("itens").size()).isEqualTo(20);
        for (int i = 0; i < 20; i++) {
            assertThat(primeira.get("itens").get(i).get("id").longValue()).isEqualTo(i + 1L);
        }
        var vazia =
                mapper.readTree(
                        mvc.perform(get("/api/v1/clientes").param("pagina", "2"))
                                .andExpect(status().isOk())
                                .andReturn()
                                .getResponse()
                                .getContentAsString());
        assertThat(vazia.get("pagina").intValue()).isEqualTo(2);
        assertThat(vazia.get("tamanho").intValue()).isEqualTo(20);
        assertThat(vazia.get("totalItens").longValue()).isEqualTo(21);
        assertThat(vazia.get("totalPaginas").intValue()).isEqualTo(2);
        assertThat(vazia.get("itens").size()).isZero();

        var pages = ArgumentCaptor.forClass(Pageable.class);
        verify(repository, times(2)).findAll(pages.capture());
        assertThat(pages.getAllValues()).extracting(Pageable::getPageNumber).containsExactly(0, 2);
        for (var p : pages.getAllValues()) {
            assertThat(p.getPageSize()).isEqualTo(20);
            assertThat(p.getSort().getOrderFor("id")).isNotNull();
            assertThat(p.getSort().getOrderFor("id").isAscending()).isTrue();
        }
        verifyNoMoreInteractions(repository);
        verifyNoInteractions(auditoria);
        assertThat(conhecidos.stream().map(ClienteDto.Resposta::de).toList()).isEqualTo(antes);
    }
}
