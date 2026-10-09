package br.com.rodogarcia.wms;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.rodogarcia.wms.models.AuditoriaCadastro;
import br.com.rodogarcia.wms.repositories.AuditoriaCadastroRepository;
import br.com.rodogarcia.wms.services.AcessoService;
import br.com.rodogarcia.wms.services.AuditoriaService;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import tools.jackson.databind.json.JsonMapper;

/** Delta005: somente fixtures sintéticas, serviço real e repositories mock, sem Boot/banco. */
class CorrecaoAuditoria005EnvelopeParcialTest {
    private static String resultado(long id) {
        return "{\"tipo\":\"VINCULO\",\"id\":" + id + "}";
    }

    static Stream<Arguments> envelopes() {
        String resultado7 = resultado(7L);
        String completo7 =
                "{\"tipoRegistro\":\"VINCULO_COBRANCA\",\"registroId\":7,\"resultado\":"
                        + resultado7
                        + "}";
        long grande = 9007199254740993L;
        return Stream.of(
                Arguments.of(
                        "VIGIA-ENVELOPE-PARCIAL-CONTRADITORIO",
                        "TABELA_COBRANCA",
                        "ENCERRAMENTO_VIGENCIA",
                        7L,
                        null,
                        "{\"registroId\":8,\"resultado\":" + resultado7 + "}",
                        false),
                Arguments.of(
                        "REGISTROID-SEM-TIPO-MESMO-ID",
                        "CONTRATO_COBRANCA",
                        "ENCERRAMENTO_VIGENCIA",
                        7L,
                        null,
                        "{\"registroId\":7,\"resultado\":" + resultado7 + "}",
                        false),
                Arguments.of(
                        "REGISTROID-NULL-SEM-TIPO",
                        "CONTRATO_COBRANCA",
                        "ENCERRAMENTO_VIGENCIA",
                        7L,
                        null,
                        "{\"registroId\":null,\"resultado\":" + resultado7 + "}",
                        false),
                Arguments.of(
                        "TIPO-SEM-REGISTROID",
                        "CONTRATO_COBRANCA",
                        "ENCERRAMENTO_VIGENCIA",
                        7L,
                        null,
                        "{\"tipoRegistro\":\"VINCULO_COBRANCA\",\"resultado\":" + resultado7 + "}",
                        false),
                Arguments.of(
                        "MARCADORES-SEM-RESULTADO",
                        "CONTRATO_COBRANCA",
                        "ENCERRAMENTO_VIGENCIA",
                        7L,
                        null,
                        "{\"tipoRegistro\":\"VINCULO_COBRANCA\",\"registroId\":7,\"id\":7}",
                        false),
                Arguments.of(
                        "ENVELOPE-PARCIAL-NO-RESULTADO",
                        "TABELA_COBRANCA",
                        "ENCERRAMENTO_VIGENCIA",
                        7L,
                        null,
                        "{\"resultado\":{\"registroId\":8,\"resultado\":" + resultado7 + "}}",
                        false),
                Arguments.of(
                        "ENVELOPE-PARCIAL-ANTES",
                        "CONTRATO_COBRANCA",
                        "ENCERRAMENTO_VIGENCIA",
                        7L,
                        "{\"registroId\":8,\"resultado\":" + resultado7 + "}",
                        completo7,
                        false),
                Arguments.of(
                        "ENVELOPE-PARCIAL-LONG-EXATO",
                        "TABELA_COBRANCA",
                        "ENCERRAMENTO_VIGENCIA",
                        grande,
                        null,
                        "{\"registroId\":" + grande + ",\"resultado\":" + resultado(grande) + "}",
                        false),
                Arguments.of(
                        "TIPOREGISTRO-NULL-PRESENTE",
                        "CONTRATO_COBRANCA",
                        "ENCERRAMENTO_VIGENCIA",
                        7L,
                        null,
                        "{\"tipoRegistro\":null,\"registroId\":7,\"resultado\":" + resultado7 + "}",
                        false),
                Arguments.of(
                        "ACAO-EXCLUSIVA-ENVELOPE-PARCIAL",
                        "CONTRATO_COBRANCA",
                        "VINCULO_TABELA",
                        7L,
                        null,
                        "{\"registroId\":8,\"id\":7,\"tabelaId\":8}",
                        false),
                Arguments.of(
                        "ENVELOPE-COMPLETO-ID-CONTRADITORIO",
                        "CONTRATO_COBRANCA",
                        "ENCERRAMENTO_VIGENCIA",
                        7L,
                        null,
                        "{\"tipoRegistro\":\"VINCULO_COBRANCA\",\"registroId\":8,\"resultado\":"
                                + resultado7
                                + "}",
                        false),
                Arguments.of(
                        "ENVELOPE-COMPLETO-LONG-MAX",
                        "CONTRATO_COBRANCA",
                        "ENCERRAMENTO_VIGENCIA",
                        Long.MAX_VALUE,
                        null,
                        "{\"tipoRegistro\":\"VINCULO_COBRANCA\",\"registroId\":"
                                + Long.MAX_VALUE
                                + ",\"resultado\":"
                                + resultado(Long.MAX_VALUE)
                                + "}",
                        true),
                Arguments.of(
                        "ENVELOPE-WRAPPER-RESOLUCAO-LONG",
                        "CONTRATO_COBRANCA",
                        "ENCERRAMENTO_VIGENCIA",
                        grande,
                        null,
                        "{\"tipoRegistro\":\"VINCULO_COBRANCA\",\"registroId\":"
                                + grande
                                + ",\"resultado\":{\"resultado\":"
                                + resultado(grande)
                                + ",\"resolucao\":{\"tipo\":\"FECHAMENTO\",\"id\":8,\"registroId\":8}}}",
                        true),
                Arguments.of(
                        "WRAPPER-LEGITIMO-SEM-MARCADORES",
                        "TABELA_COBRANCA",
                        "ENCERRAMENTO_VIGENCIA",
                        7L,
                        null,
                        "{\"resultado\":"
                                + resultado7
                                + ",\"resolucao\":{\"tipo\":\"FECHAMENTO\",\"id\":8,\"registroId\":8}}",
                        true));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("envelopes")
    void marcadorExigeEnvelopeCompletoSemSubstituirIdDoCadastro(
            String caso,
            String carrier,
            String acao,
            Long id,
            String antes,
            String depois,
            boolean confirmado) {
        var repository = mock(AuditoriaCadastroRepository.class);
        var acesso = mock(AcessoService.class);
        var clock = Clock.fixed(Instant.parse("2026-10-09T16:00:00Z"), ZoneOffset.UTC);
        var evento =
                new AuditoriaCadastro(
                        carrier,
                        id,
                        acao,
                        "cedro-fixture",
                        Instant.now(clock),
                        "operacao-ficticia",
                        "Delta005 ficticio",
                        antes,
                        depois);
        var fechados = new AtomicInteger();
        when(repository.findByTipoInAndRegistroIdOrderByIdAsc(any(), eq(id)))
                .thenAnswer(i -> Stream.of(evento).onClose(fechados::incrementAndGet));
        var auditoria =
                new AuditoriaService(repository, acesso, JsonMapper.builder().build(), clock);
        var vinculo = auditoria.listar("VINCULO_COBRANCA", id, 0, 20);
        var legado = auditoria.listar("VINCULO_COBRANCA_LEGADO", id, 0, 20);
        assertThat(vinculo.totalItens()).as(caso).isEqualTo(confirmado ? 1L : 0L);
        assertThat(legado.totalItens()).as(caso).isEqualTo(confirmado ? 0L : 1L);
        var resposta = (confirmado ? vinculo : legado).itens().getFirst();
        assertThat(resposta.tipo())
                .isEqualTo(confirmado ? "VINCULO_COBRANCA" : "LEGADO_NAO_ATRIBUIDO");
        assertThat(resposta.tipoFisico()).isEqualTo(carrier);
        assertThat(resposta.registroId()).isEqualTo(id);
        assertThat(resposta.dadosAntes()).isEqualTo(antes);
        assertThat(resposta.dadosDepois()).isEqualTo(depois);
        assertThat(fechados.get()).isEqualTo(2);
        verify(repository, never()).saveAndFlush(any());
    }
}
