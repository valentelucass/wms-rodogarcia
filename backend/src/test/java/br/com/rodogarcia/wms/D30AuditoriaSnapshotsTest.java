package br.com.rodogarcia.wms;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.rodogarcia.wms.config.IdentificacaoOperacaoFilter;
import br.com.rodogarcia.wms.dto.AuditoriaResponse;
import br.com.rodogarcia.wms.models.AuditoriaCadastro;
import br.com.rodogarcia.wms.repositories.AuditoriaCadastroRepository;
import br.com.rodogarcia.wms.services.AcessoService;
import br.com.rodogarcia.wms.services.AuditoriaService;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/** Conteudo do evento fornecido ao servico real; nao simula transacao ou todos callers. */
class D30AuditoriaSnapshotsTest {
    static final Instant INSTANTE = Instant.parse("2026-09-05T12:00:00.567890Z");
    static final String OPERACAO = "00000000-0000-0000-0000-000000000030";
    private JsonMapper mapper;
    private AuditoriaCadastroRepository repository;
    private AuditoriaService service;
    private List<AuditoriaCadastro> gravadas;

    @BeforeEach
    void prepararSemBanco() {
        new ApplicationContextRunner()
                .withInitializer(new ConfigDataApplicationContextInitializer())
                .withConfiguration(AutoConfigurations.of(JacksonAutoConfiguration.class))
                .withPropertyValues("spring.profiles.active=local")
                .run(
                        context -> {
                            assertThat(context).hasNotFailed();
                            assertThat(context.getBeansOfType(DataSource.class)).isEmpty();
                            mapper = context.getBean(JsonMapper.class);
                        });
        repository = mock(AuditoriaCadastroRepository.class);
        var acesso = mock(AcessoService.class);
        when(acesso.usuario()).thenReturn("Cedro-ficticio");
        gravadas = new ArrayList<>();
        when(repository.saveAndFlush(any(AuditoriaCadastro.class)))
                .thenAnswer(
                        inv -> {
                            var e = inv.getArgument(0, AuditoriaCadastro.class);
                            ReflectionTestUtils.setField(e, "id", 77L + gravadas.size());
                            gravadas.add(e);
                            return e;
                        });
        service =
                new AuditoriaService(
                        repository, acesso, mapper, Clock.fixed(INSTANTE, ZoneOffset.UTC));
    }

    @Test
    void criacaoConservaSujeitoTempoMotivoCorrelacaoAntesNuloEDepoisJsonString() {
        comCorrelacao(
                OPERACAO,
                () ->
                        service.registrar(
                                "CLIENTE",
                                22L,
                                "CRIACAO",
                                "Cadastro ficticio",
                                null,
                                Map.of("id", 22L, "versao", 0L, "descricao", "Cliente C2")));
        var e = gravadas.getFirst();
        var resposta = AuditoriaResponse.de(e);
        assertThat(resposta)
                .isEqualTo(
                        new AuditoriaResponse(
                                77L,
                                "CLIENTE",
                                22L,
                                "CRIACAO",
                                "Cedro-ficticio",
                                INSTANTE,
                                OPERACAO,
                                "Cadastro ficticio",
                                null,
                                e.getDadosDepois()));
        assertThat(resposta.dadosAntes()).isNull();
        var json = mapper.readTree(mapper.writeValueAsString(resposta));
        assertThat(json.get("id").longValue()).isEqualTo(77);
        assertThat(json.get("registroId").longValue()).isEqualTo(22);
        assertThat(json.get("usuario").asString()).isEqualTo("Cedro-ficticio");
        assertThat(json.get("instante").asString()).isEqualTo("2026-09-05T12:00:00.567890Z");
        assertThat(json.get("motivo").asString()).isEqualTo("Cadastro ficticio");
        assertThat(json.get("idOperacao").asString()).isEqualTo(OPERACAO);
        assertThat(json.get("dadosAntes").isNull()).isTrue();
        assertThat(json.get("dadosDepois").isString()).isTrue();
        assertThat(
                        mapper.readValue(
                                resposta.dadosDepois(),
                                new TypeReference<Map<String, Object>>() {}))
                .containsExactlyInAnyOrderEntriesOf(
                        Map.of("id", 22, "versao", 0, "descricao", "Cliente C2"));
        verify(repository).saveAndFlush(e);
    }

    @Test
    void alteracaoRecuperaOrigemDestinoVersaoEQuantidadeSemReescreverSnapshot() {
        var antes =
                new HashMap<String, Object>(
                        Map.of("endereco", "A", "posicao", 1, "versao", 0, "quantidade", 6));
        var depois =
                new HashMap<String, Object>(
                        Map.of("endereco", "B", "posicao", 2, "versao", 1, "quantidade", 6));
        comCorrelacao(
                OPERACAO,
                () ->
                        service.registrar(
                                "UNIDADE",
                                201L,
                                "REMANEJAMENTO",
                                "Remanejamento ficticio",
                                antes,
                                depois));
        antes.put("endereco", "ALTERADO");
        depois.put("quantidade", 999);
        var e = gravadas.getFirst();
        var resposta = AuditoriaResponse.de(e);
        assertThat(
                        mapper.readValue(
                                resposta.dadosAntes(),
                                new TypeReference<Map<String, Object>>() {}))
                .containsExactlyInAnyOrderEntriesOf(
                        Map.of("endereco", "A", "posicao", 1, "versao", 0, "quantidade", 6));
        assertThat(
                        mapper.readValue(
                                resposta.dadosDepois(),
                                new TypeReference<Map<String, Object>>() {}))
                .containsExactlyInAnyOrderEntriesOf(
                        Map.of("endereco", "B", "posicao", 2, "versao", 1, "quantidade", 6));
        assertThat(resposta.tipo()).isEqualTo("UNIDADE");
        assertThat(resposta.registroId()).isEqualTo(201);
        assertThat(resposta.acao()).isEqualTo("REMANEJAMENTO");
        assertThat(resposta.usuario()).isEqualTo("Cedro-ficticio");
        assertThat(resposta.instante()).isEqualTo(INSTANTE);
        assertThat(resposta.idOperacao()).isEqualTo(OPERACAO);
        assertThat(resposta.motivo()).isEqualTo("Remanejamento ficticio");
        var json = mapper.readTree(mapper.writeValueAsString(resposta));
        assertThat(json.get("dadosAntes").isString()).isTrue();
        assertThat(json.get("dadosDepois").isString()).isTrue();
    }

    @Test
    void semCorrelacaoExternaGeraUuidValidoDistintoParaCadaRegistro() {
        comCorrelacao(
                null,
                () -> {
                    service.registrar(
                            "CLIENTE", 22L, "CRIACAO", "Primeiro ficticio", null, Map.of("id", 22));
                    service.registrar(
                            "CLIENTE", 23L, "CRIACAO", "Segundo ficticio", null, Map.of("id", 23));
                });
        assertThat(gravadas).hasSize(2);
        for (var e : gravadas)
            assertThat(UUID.fromString(e.getIdOperacao()).toString()).isEqualTo(e.getIdOperacao());
        assertThat(gravadas.get(0).getIdOperacao()).isNotEqualTo(gravadas.get(1).getIdOperacao());
        assertThat(gravadas)
                .extracting(AuditoriaCadastro::getInstante)
                .containsExactly(INSTANTE, INSTANTE);
    }

    private static void comCorrelacao(String valor, Runnable executar) {
        String anterior = MDC.get(IdentificacaoOperacaoFilter.ATRIBUTO);
        try {
            if (valor == null) MDC.remove(IdentificacaoOperacaoFilter.ATRIBUTO);
            else MDC.put(IdentificacaoOperacaoFilter.ATRIBUTO, valor);
            executar.run();
        } finally {
            if (anterior == null) MDC.remove(IdentificacaoOperacaoFilter.ATRIBUTO);
            else MDC.put(IdentificacaoOperacaoFilter.ATRIBUTO, anterior);
        }
    }
}
