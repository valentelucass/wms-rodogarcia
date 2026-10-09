package br.com.rodogarcia.wms;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.rodogarcia.wms.dto.ConfiguracaoCobrancaDto;
import br.com.rodogarcia.wms.exceptions.RegraNegocioException;
import br.com.rodogarcia.wms.models.Armazem;
import br.com.rodogarcia.wms.models.AuditoriaCadastro;
import br.com.rodogarcia.wms.models.ItemTabelaCobranca;
import br.com.rodogarcia.wms.models.ServicoCobranca;
import br.com.rodogarcia.wms.models.TabelaCobranca;
import br.com.rodogarcia.wms.repositories.ArmazemRepository;
import br.com.rodogarcia.wms.repositories.AuditoriaCadastroRepository;
import br.com.rodogarcia.wms.repositories.CalculoCobrancaRepository;
import br.com.rodogarcia.wms.repositories.ContratoCobrancaRepository;
import br.com.rodogarcia.wms.repositories.ItemTabelaCobrancaRepository;
import br.com.rodogarcia.wms.repositories.MemoriaDiariaRepository;
import br.com.rodogarcia.wms.repositories.ServicoCobrancaRepository;
import br.com.rodogarcia.wms.repositories.ServicoMinimoContratoRepository;
import br.com.rodogarcia.wms.repositories.TabelaCobrancaRepository;
import br.com.rodogarcia.wms.repositories.VinculoTabelaClienteRepository;
import br.com.rodogarcia.wms.services.AcessoService;
import br.com.rodogarcia.wms.services.AuditoriaService;
import br.com.rodogarcia.wms.services.CadastroSupport;
import br.com.rodogarcia.wms.services.ConfiguracaoCobrancaService;
import br.com.rodogarcia.wms.services.ContextoCobrancaService;
import br.com.rodogarcia.wms.services.OperacaoAdministrativaService;
import br.com.rodogarcia.wms.services.ResolucaoFinanceiraCadastroService;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Answers;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.json.JsonMapper;

/** Fixtures sinteticas; chamadas diretas e repositories mock, sem Spring Boot/banco/provider. */
class CorrecaoCadastros004005Test {
    private final Clock clock = Clock.fixed(Instant.parse("2026-10-09T16:00:00Z"), ZoneOffset.UTC);
    private final JsonMapper mapper = JsonMapper.builder().build();
    private AcessoService acesso;
    private TabelaCobrancaRepository tabelas;
    private ItemTabelaCobrancaRepository itens;
    private ServicoCobranca servico;
    private ConfiguracaoCobrancaService configuracao;
    private AuditoriaService auditoria;
    private AuditoriaCadastroRepository auditRepo;
    private Page<AuditoriaCadastro> auditPage;
    private List<AuditoriaCadastro> gravados;
    private List<ItemTabelaCobranca> itensGravados;
    private final java.util.concurrent.atomic.AtomicInteger streamsFechados =
            new java.util.concurrent.atomic.AtomicInteger();

    @BeforeEach
    void preparar() {
        acesso = mock(AcessoService.class);
        when(acesso.usuario()).thenReturn("cedro-fixture");
        gravados = new ArrayList<>();
        auditPage = Page.empty(PageRequest.of(0, 20));
        auditRepo =
                mock(
                        AuditoriaCadastroRepository.class,
                        invocation -> {
                            if (java.util.stream.Stream.class.isAssignableFrom(
                                    invocation.getMethod().getReturnType()))
                                return auditPage.getContent().stream()
                                        .onClose(streamsFechados::incrementAndGet);
                            if (Page.class.isAssignableFrom(invocation.getMethod().getReturnType()))
                                return auditPage;
                            if (invocation.getMethod().getName().equals("saveAndFlush")) {
                                AuditoriaCadastro row = invocation.getArgument(0);
                                gravados.add(row);
                                return row;
                            }
                            return Answers.RETURNS_DEFAULTS.answer(invocation);
                        });
        auditoria = new AuditoriaService(auditRepo, acesso, mapper, clock);
        tabelas = mock(TabelaCobrancaRepository.class);
        itens = mock(ItemTabelaCobrancaRepository.class);
        var armazens = mock(ArmazemRepository.class);
        var a = mock(Armazem.class);
        when(a.getId()).thenReturn(9L);
        when(a.getSituacao()).thenReturn(br.com.rodogarcia.wms.models.SituacaoCadastro.ATIVO);
        when(armazens.buscarParaAtualizar(9L)).thenReturn(Optional.of(a));
        var servicos = mock(ServicoCobrancaRepository.class);
        servico = mock(ServicoCobranca.class);
        when(servico.getId()).thenReturn(11L);
        when(servico.getSituacao()).thenReturn("ATIVO");
        when(servico.getTipo()).thenReturn("ENTRADA");
        when(servico.getUnidade()).thenReturn("UNIDADE_LOGISTICA");
        when(servicos.buscarParaAtualizar(11L)).thenReturn(Optional.of(servico));
        when(servicos.getReferenceById(11L)).thenReturn(servico);
        when(tabelas.saveAndFlush(any()))
                .thenAnswer(
                        invocation -> {
                            TabelaCobranca table = invocation.getArgument(0);
                            ReflectionTestUtils.setField(table, "id", 22L);
                            return table;
                        });
        itensGravados = new ArrayList<>();
        when(itens.save(any()))
                .thenAnswer(
                        invocation -> {
                            ItemTabelaCobranca item = invocation.getArgument(0);
                            ReflectionTestUtils.setField(item, "id", 33L);
                            itensGravados.add(item);
                            return item;
                        });
        when(itens.findByTabelaIdOrderByIdAsc(anyLong())).thenAnswer(i -> itensGravados);
        configuracao =
                new ConfiguracaoCobrancaService(
                        servicos,
                        tabelas,
                        itens,
                        mock(VinculoTabelaClienteRepository.class),
                        mock(ContratoCobrancaRepository.class),
                        mock(ServicoMinimoContratoRepository.class),
                        armazens,
                        mock(CalculoCobrancaRepository.class),
                        mock(MemoriaDiariaRepository.class),
                        mock(ContextoCobrancaService.class),
                        mock(OperacaoAdministrativaService.class),
                        mock(AuditoriaService.class),
                        acesso,
                        clock,
                        mock(ResolucaoFinanceiraCadastroService.class));
    }

    private ConfiguracaoCobrancaDto.CriarTabela tabela(String categoria) {
        return new ConfiguracaoCobrancaDto.CriarTabela(
                UUID.randomUUID(),
                9L,
                null,
                "CEDRO-004",
                "Tabela ficticia",
                "PADRAO",
                LocalDate.of(2026, 10, 1),
                null,
                List.of(new ConfiguracaoCobrancaDto.Item(11L, categoria, BigDecimal.ZERO, null)),
                "Correcao ficticia",
                null);
    }

    @Test
    void unicode40Expande80ERecusaAntesDePersistir() {
        assertThatThrownBy(() -> configuracao.criarTabela(tabela("ß".repeat(40))))
                .isInstanceOfSatisfying(
                        RegraNegocioException.class,
                        e -> {
                            assertThat(e.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                            assertThat(e.getCodigo()).isEqualTo("CATEGORIA_INVALIDA");
                            assertThat(e.getMessage()).contains("itens[].categoria");
                        });
        verify(tabelas, never()).saveAndFlush(any());
        verify(itens, never()).save(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"ascii40", "unicode20", "vazia", "categoriaLivre"})
    void categoriasDentroDoDominioConservamValorNormalizado(String caso) {
        String input =
                switch (caso) {
                    case "ascii40" -> "a".repeat(40);
                    case "unicode20" -> "ß".repeat(20);
                    case "vazia" -> "  ";
                    default -> "classe-é/livre";
                };
        var result = configuracao.criarTabela(tabela(input));
        assertThat(result.itens().getFirst().categoria()).isEqualTo(CadastroSupport.codigo(input));
        assertThat(itensGravados.getFirst().getCategoria()).hasSizeLessThanOrEqualTo(40);
    }

    @Test
    void ascii41TambemRecusaNoServicoSemDependerDoProxyValidation() {
        assertThatThrownBy(() -> configuracao.criarTabela(tabela("A".repeat(41))))
                .isInstanceOfSatisfying(
                        RegraNegocioException.class,
                        e -> assertThat(e.getCodigo()).isEqualTo("CATEGORIA_INVALIDA"));
        assertThat(itensGravados).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " pallet ", "bobina"})
    void armazenagemMantemWhitelistAposNormalizar(String input) {
        when(servico.getTipo()).thenReturn("ARMAZENAGEM");
        when(servico.getUnidade()).thenReturn("POSICAO_DIA");
        assertThat(configuracao.criarTabela(tabela(input)).itens().getFirst().categoria())
                .isEqualTo(CadastroSupport.codigo(input));
    }

    @Test
    void armazenagemRejeitaCategoriaLivreSemPersistencia() {
        when(servico.getTipo()).thenReturn("ARMAZENAGEM");
        assertThatThrownBy(() -> configuracao.criarTabela(tabela("caixa")))
                .isInstanceOf(RegraNegocioException.class);
        verify(tabelas, never()).saveAndFlush(any());
    }

    @Test
    void recusaDePerfilNaoProduzConsultaOuEscrita() {
        doThrow(new AccessDeniedException("Recusa ficticia")).when(acesso).exigirGestor();
        assertThatThrownBy(() -> auditoria.listar("VINCULO_COBRANCA", 7L, 0, 20))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> configuracao.criarTabela(tabela("OK")))
                .isInstanceOf(AccessDeniedException.class);
        assertThat(org.mockito.Mockito.mockingDetails(auditRepo).getInvocations()).isEmpty();
        assertThat(itensGravados).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"VINCULO_TABELA", "ENCERRAMENTO_VIGENCIA"})
    void novoNamespaceLogicoUsaCarrierPermitidoEEnvelopeExato(String acao) {
        auditoria.registrar(
                "VINCULO_COBRANCA",
                9007199254740993L,
                acao,
                "Focal ficticio",
                null,
                Map.of("id", 9007199254740993L, "tipo", "VINCULO"));
        var row = gravados.getFirst();
        assertThat(row.getTipo()).isEqualTo("CONTRATO_COBRANCA");
        var json = mapper.readTree(row.getDadosDepois());
        assertThat(json.get("tipoRegistro").asString()).isEqualTo("VINCULO_COBRANCA");
        assertThat(json.get("registroId").asLong()).isEqualTo(9007199254740993L);
        assertThat(json.get("resultado").get("id").asLong()).isEqualTo(9007199254740993L);
    }

    @Test
    void namespaceLogicoNaoAceitaAcaoDeContrato() {
        assertThatThrownBy(
                        () ->
                                auditoria.registrar(
                                        "VINCULO_COBRANCA",
                                        7L,
                                        "CONFIGURACAO",
                                        "Focal ficticio",
                                        null,
                                        Map.of("id", 7L)))
                .isInstanceOf(RegraNegocioException.class);
        assertThat(gravados).isEmpty();
    }

    private AuditoriaCadastro evento(String carrier, String acao, String antes, String depois) {
        return new AuditoriaCadastro(
                carrier,
                7L,
                acao,
                "cedro-fixture",
                Instant.now(clock),
                UUID.randomUUID().toString(),
                "Fixture ficticia",
                antes,
                depois);
    }

    private void pagina(AuditoriaCadastro e) {
        auditPage = new PageImpl<>(List.of(e), PageRequest.of(0, 20), 1L);
    }

    @ParameterizedTest
    @ValueSource(strings = {"acao", "raiz", "wrapper", "envelope"})
    void historicoInequivocoDeVinculoVemDoNamespaceLogico(String caso) {
        var e =
                switch (caso) {
                    case "acao" ->
                            evento(
                                    "CONTRATO_COBRANCA",
                                    "VINCULO_TABELA",
                                    null,
                                    "{\"id\":7,\"tabelaId\":8}");
                    case "raiz" ->
                            evento(
                                    "TABELA_COBRANCA",
                                    "ENCERRAMENTO_VIGENCIA",
                                    "{\"tipo\":\"VINCULO\",\"id\":7}",
                                    "{\"tipo\":\"VINCULO\",\"id\":7}");
                    case "wrapper" ->
                            evento(
                                    "TABELA_COBRANCA",
                                    "ENCERRAMENTO_VIGENCIA",
                                    null,
                                    "{\"resultado\":{\"tipo\":\"VINCULO\",\"id\":7},\"resolucao\":{\"tipo\":\"FECHAMENTO\"}}");
                    default ->
                            evento(
                                    "CONTRATO_COBRANCA",
                                    "ENCERRAMENTO_VIGENCIA",
                                    null,
                                    "{\"tipoRegistro\":\"VINCULO_COBRANCA\",\"registroId\":7,\"resultado\":{\"tipo\":\"VINCULO\",\"id\":7}}");
                };
        pagina(e);
        var result = auditoria.listar("VINCULO_COBRANCA", 7L, 0, 20);
        assertThat(result.totalItens()).isEqualTo(1L);
        assertThat(result.itens().getFirst().tipo()).isEqualTo("VINCULO_COBRANCA");
        assertThat(result.itens().getFirst().dadosDepois()).isEqualTo(e.getDadosDepois());
    }

    @ParameterizedTest
    @ValueSource(strings = {"ausente", "invalido", "idDivergente", "tipoContraditorio"})
    void legadoAmbiguoTemConsultaSeparadaSemAfirmarVinculo(String caso) {
        String depois =
                switch (caso) {
                    case "ausente" -> "{\"id\":7}";
                    case "invalido" -> "nao-json";
                    case "idDivergente" -> "{\"tipo\":\"VINCULO\",\"id\":8}";
                    default -> "{\"tipo\":\"CONTRATO\",\"id\":7}";
                };
        pagina(
                evento(
                        "TABELA_COBRANCA",
                        "ENCERRAMENTO_VIGENCIA",
                        caso.equals("tipoContraditorio") ? "{\"tipo\":\"VINCULO\",\"id\":7}" : null,
                        depois));
        assertThat(auditoria.listar("VINCULO_COBRANCA_LEGADO", 7L, 0, 20).itens().getFirst().tipo())
                .isEqualTo("LEGADO_NAO_ATRIBUIDO");
    }

    @Test
    void colisaoDeIdNaoConfirmaVinculoSeSnapshotDizContrato() {
        pagina(
                evento(
                        "CONTRATO_COBRANCA",
                        "ENCERRAMENTO_VIGENCIA",
                        null,
                        "{\"tipo\":\"CONTRATO\",\"id\":7}"));
        assertThat(auditoria.listar("VINCULO_COBRANCA", 7L, 0, 20).itens()).isEmpty();
    }

    @Test
    void tabelaNaoRecebeEncerramentoProvadoComoVinculo() {
        pagina(
                evento(
                        "TABELA_COBRANCA",
                        "ENCERRAMENTO_VIGENCIA",
                        null,
                        "{\"tipo\":\"VINCULO\",\"id\":7}"));
        assertThat(auditoria.listar("TABELA_COBRANCA", 7L, 0, 20).itens()).isEmpty();
    }

    @Test
    void namespaceAlheioConservaJsonSemEnvelope() {
        auditoria.registrar("CLIENTE", 7L, "CRIACAO", "Focal ficticio", null, Map.of("id", 7L));
        assertThat(gravados.getFirst().getTipo()).isEqualTo("CLIENTE");
        assertThat(gravados.getFirst().getDadosDepois()).isEqualTo("{\"id\":7}");
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "tabelaId", "tabelaIdNull", "depoisVazio", "depoisSemTipo",
                "antesSemTipo", "antesVazio", "envelopeAntesDepoisVazio", "tipoEIdSeparados"
            })
    void encerramentoExigeProvaCompletaEmCadaDocumento(String caso) {
        String prova = "{\"tipo\":\"VINCULO\",\"id\":7}";
        String antes =
                switch (caso) {
                    case "depoisVazio", "depoisSemTipo" -> prova;
                    case "antesSemTipo" -> "{\"id\":7}";
                    case "antesVazio" -> "{}";
                    case "envelopeAntesDepoisVazio" ->
                            "{\"tipoRegistro\":\"VINCULO_COBRANCA\",\"registroId\":7,\"resultado\":{\"id\":7}}";
                    case "tipoEIdSeparados" -> "{\"tipo\":\"VINCULO\"}";
                    default -> null;
                };
        String depois =
                switch (caso) {
                    case "tabelaId" -> "{\"id\":7,\"tabelaId\":8}";
                    case "tabelaIdNull" -> "{\"id\":7,\"tabelaId\":null}";
                    case "depoisVazio", "envelopeAntesDepoisVazio" -> "{}";
                    case "depoisSemTipo", "tipoEIdSeparados" -> "{\"id\":7}";
                    default -> prova;
                };
        pagina(
                evento(
                        caso.equals("envelopeAntesDepoisVazio")
                                ? "CONTRATO_COBRANCA"
                                : "TABELA_COBRANCA",
                        "ENCERRAMENTO_VIGENCIA",
                        antes,
                        depois));
        assertThat(auditoria.listar("VINCULO_COBRANCA", 7L, 0, 20).totalItens()).isZero();
        var legado = auditoria.listar("VINCULO_COBRANCA_LEGADO", 7L, 0, 20);
        assertThat(legado.totalItens()).isEqualTo(1L);
        assertThat(legado.itens().getFirst().tipo()).isEqualTo("LEGADO_NAO_ATRIBUIDO");
        assertThat(legado.itens().getFirst().dadosAntes()).isEqualTo(antes);
        assertThat(legado.itens().getFirst().dadosDepois()).isEqualTo(depois);
    }

    @org.junit.jupiter.api.TestFactory
    java.util.stream.Stream<org.junit.jupiter.api.DynamicTest>
            criteriosPrumoComSnapshotRealDoService() throws Exception {
        var cases = new ArrayList<Map<?, ?>>();
        for (String file :
                List.of("/cedro-correcao-casos-005.json", "/cedro-correcao-adicionais-005.json")) {
            try (var input = getClass().getResourceAsStream(file)) {
                assertThat(input).isNotNull();
                Map<?, ?> document = mapper.readValue(input, Map.class);
                for (var candidate : (List<?>) document.get("casos")) {
                    Map<?, ?> test = (Map<?, ?>) candidate;
                    if (!test.containsKey("candidatosEmOrdemRecebida")) cases.add(test);
                }
            }
        }
        return cases.stream()
                .map(
                        test ->
                                org.junit.jupiter.api.DynamicTest.dynamicTest(
                                        (String) test.get("id"),
                                        () -> {
                                            Map<?, ?> row =
                                                    test.containsKey("registro")
                                                            ? (Map<?, ?>) test.get("registro")
                                                            : test;
                                            Long id =
                                                    Long.valueOf(
                                                            (String)
                                                                    row.get(
                                                                            "registroIdDecimalExato"));
                                            var event =
                                                    new AuditoriaCadastro(
                                                            (String) row.get("tipoFisico"),
                                                            id,
                                                            (String) row.get("acao"),
                                                            "cedro-fixture",
                                                            Instant.now(clock),
                                                            UUID.randomUUID().toString(),
                                                            "Fixture ficticia",
                                                            (String) row.get("dadosAntes"),
                                                            (String) row.get("dadosDepois"));
                                            ReflectionTestUtils.setField(
                                                    event,
                                                    "id",
                                                    row.containsKey("idAuditoria")
                                                            ? Long.valueOf(
                                                                    (String) row.get("idAuditoria"))
                                                            : 127L);
                                            Object expected = test.get("esperado");
                                            String type =
                                                    expected instanceof Map<?, ?> map
                                                            ? (String) map.get("tipoLogico")
                                                            : (String) expected;
                                            auditPage = new PageImpl<>(List.of(event));
                                            String query =
                                                    type.equals("LEGADO_NAO_ATRIBUIDO")
                                                            ? "VINCULO_COBRANCA_LEGADO"
                                                            : type;
                                            var result = auditoria.listar(query, id, 0, 20);
                                            assertThat(result.itens()).hasSize(1);
                                            assertThat(result.totalItens()).isEqualTo(1L);
                                            assertThat(result.itens().getFirst().tipo())
                                                    .isEqualTo(type);
                                            assertThat(result.itens().getFirst().tipoFisico())
                                                    .isEqualTo(row.get("tipoFisico"));
                                            assertThat(result.itens().getFirst().registroId())
                                                    .isEqualTo(id);
                                            assertThat(result.itens().getFirst().dadosAntes())
                                                    .isEqualTo(row.get("dadosAntes"));
                                            assertThat(result.itens().getFirst().dadosDepois())
                                                    .isEqualTo(row.get("dadosDepois"));
                                            for (String other :
                                                    List.of(
                                                            "VINCULO_COBRANCA",
                                                            "TABELA_COBRANCA",
                                                            "CONTRATO_COBRANCA",
                                                            "VINCULO_COBRANCA_LEGADO")) {
                                                if (!other.equals(query))
                                                    assertThat(
                                                                    auditoria
                                                                            .listar(
                                                                                    other, id, 0,
                                                                                    20)
                                                                            .totalItens())
                                                            .isZero();
                                            }
                                        }));
    }

    @Test
    void classificacaoPrecedeCountOffsetUltimaPaginaEVazioEFechaStream() {
        var pool = new ArrayList<AuditoriaCadastro>();
        String[] kinds = {"CONTRATO", "VINCULO", "TABELA", "AMBIGUO", "VINCULO"};
        for (int n = 0; n < kinds.length; n++) {
            String body =
                    kinds[n].equals("AMBIGUO") ? "{}" : "{\"tipo\":\"" + kinds[n] + "\",\"id\":7}";
            var event =
                    evento(
                            kinds[n].equals("CONTRATO") ? "CONTRATO_COBRANCA" : "TABELA_COBRANCA",
                            "ENCERRAMENTO_VIGENCIA",
                            null,
                            body);
            ReflectionTestUtils.setField(event, "id", 100L + n);
            pool.add(event);
        }
        auditPage = new PageImpl<>(pool);
        for (int page = 0; page < 3; page++) {
            var result = auditoria.listar("VINCULO_COBRANCA", 7L, page, 1);
            assertThat(result.totalItens()).isEqualTo(2);
            assertThat(result.totalPaginas()).isEqualTo(2);
            assertThat(result.pagina()).isEqualTo(page);
            assertThat(result.itens().stream().map(r -> r.id()).toList())
                    .isEqualTo(page == 0 ? List.of(101L) : page == 1 ? List.of(104L) : List.of());
        }
        var legacy = auditoria.listar("VINCULO_COBRANCA_LEGADO", 7L, 0, 1);
        assertThat(legacy.totalItens()).isEqualTo(1);
        assertThat(legacy.itens().getFirst().id()).isEqualTo(103L);
        assertThat(streamsFechados.get()).isEqualTo(4);
    }

    private ConfiguracaoCobrancaService configurarWriter(
            VinculoTabelaClienteRepository repo,
            ContextoCobrancaService contexts,
            OperacaoAdministrativaService operations,
            AuditoriaService audit,
            ResolucaoFinanceiraCadastroService resolution) {
        return new ConfiguracaoCobrancaService(
                mock(ServicoCobrancaRepository.class),
                tabelas,
                itens,
                repo,
                mock(ContratoCobrancaRepository.class),
                mock(ServicoMinimoContratoRepository.class),
                mock(ArmazemRepository.class),
                mock(CalculoCobrancaRepository.class),
                mock(MemoriaDiariaRepository.class),
                contexts,
                operations,
                audit,
                acesso,
                clock,
                resolution);
    }

    @ParameterizedTest
    @ValueSource(strings = {"criar", "encerrar"})
    void writersConfiguracaoUsamNamespaceSemMudarOperacaoReplay(String etapa) {
        var repo = mock(VinculoTabelaClienteRepository.class);
        var contexts = mock(ContextoCobrancaService.class);
        var operations = mock(OperacaoAdministrativaService.class);
        var audit = mock(AuditoriaService.class);
        var resolution = mock(ResolucaoFinanceiraCadastroService.class);
        when(resolution.paraAuditoria(any(), any())).thenAnswer(i -> i.getArgument(1));
        var cliente = mock(br.com.rodogarcia.wms.models.Cliente.class);
        var armazem = mock(Armazem.class);
        when(cliente.getId()).thenReturn(1L);
        when(armazem.getId()).thenReturn(9L);
        when(contexts.bloquear(1L, 9L))
                .thenReturn(new ContextoCobrancaService.Contexto(cliente, armazem));
        var table = mock(TabelaCobranca.class);
        when(table.getId()).thenReturn(8L);
        when(table.getArmazem()).thenReturn(armazem);
        when(table.getVigenciaInicio()).thenReturn(LocalDate.of(2026, 10, 1));
        when(tabelas.findById(8L)).thenReturn(Optional.of(table));
        var vinculo =
                new br.com.rodogarcia.wms.models.VinculoTabelaCliente(
                        cliente,
                        armazem,
                        table,
                        LocalDate.of(2026, 10, 1),
                        null,
                        Instant.now(clock),
                        Instant.now(clock));
        ReflectionTestUtils.setField(vinculo, "id", 7L);
        when(repo.saveAndFlush(any())).thenReturn(vinculo);
        when(repo.findById(7L)).thenReturn(Optional.of(vinculo));
        var scope = mock(br.com.rodogarcia.wms.repositories.EscopoCobranca.class);
        when(scope.getClienteId()).thenReturn(1L);
        when(scope.getArmazemId()).thenReturn(9L);
        when(repo.buscarEscopo(7L)).thenReturn(Optional.of(scope));
        var service = configurarWriter(repo, contexts, operations, audit, resolution);
        if (etapa.equals("criar")) {
            var command =
                    new ConfiguracaoCobrancaDto.Vincular(
                            UUID.randomUUID(),
                            1L,
                            9L,
                            8L,
                            LocalDate.of(2026, 10, 1),
                            null,
                            "Fixture ficticia",
                            null);
            assertThat(service.vincular(command).id()).isEqualTo(7L);
            verify(operations).hash("VINCULO_TABELA", null, command);
        } else {
            var command =
                    new ConfiguracaoCobrancaDto.Encerrar(
                            UUID.randomUUID(),
                            0L,
                            LocalDate.of(2026, 10, 9),
                            "Fixture ficticia",
                            null);
            assertThat(service.encerrarVinculo(7L, command).id()).isEqualTo(7L);
            verify(operations).hash("VINCULO_TABELA", 7L, command);
        }
        verify(audit)
                .registrar(
                        org.mockito.ArgumentMatchers.eq("VINCULO_COBRANCA"),
                        org.mockito.ArgumentMatchers.eq(7L),
                        org.mockito.ArgumentMatchers.eq("VINCULO_TABELA"),
                        any(),
                        any(),
                        any());
    }

    @Test
    void writerEncerramentoVigenciaVinculoUsaMesmoNamespace() {
        var em = mock(jakarta.persistence.EntityManager.class, Answers.RETURNS_DEEP_STUBS);
        var clientes = mock(br.com.rodogarcia.wms.repositories.ClienteRepository.class);
        var armazens = mock(ArmazemRepository.class);
        var operations = mock(OperacaoAdministrativaService.class);
        var audit = mock(AuditoriaService.class);
        var resolution = mock(ResolucaoFinanceiraCadastroService.class);
        when(resolution.paraAuditoria(any(), any())).thenAnswer(i -> i.getArgument(1));
        var cliente = mock(br.com.rodogarcia.wms.models.Cliente.class);
        var armazem = mock(Armazem.class);
        when(cliente.getId()).thenReturn(1L);
        when(armazem.getId()).thenReturn(9L);
        when(clientes.buscarParaAtualizar(1L)).thenReturn(Optional.of(cliente));
        when(armazens.buscarParaAtualizar(9L)).thenReturn(Optional.of(armazem));
        var table = mock(TabelaCobranca.class);
        when(table.getId()).thenReturn(8L);
        var v =
                new br.com.rodogarcia.wms.models.VinculoTabelaCliente(
                        cliente,
                        armazem,
                        table,
                        LocalDate.of(2026, 10, 1),
                        null,
                        Instant.now(clock),
                        Instant.now(clock));
        ReflectionTestUtils.setField(v, "id", 7L);
        when(em.find(br.com.rodogarcia.wms.models.VinculoTabelaCliente.class, 7L)).thenReturn(v);
        var service =
                new br.com.rodogarcia.wms.services.EncerramentoService(
                        em,
                        clientes,
                        armazens,
                        operations,
                        audit,
                        resolution,
                        acesso,
                        mapper,
                        clock);
        var command =
                new br.com.rodogarcia.wms.dto.EncerramentoDto.EncerrarVigencia(
                        UUID.randomUUID(), 0L, LocalDate.of(2026, 10, 9), null, "Fixture ficticia");
        assertThat(service.encerrarVigencia("VINCULO", 7L, command).tipo()).isEqualTo("VINCULO");
        verify(audit)
                .registrar(
                        org.mockito.ArgumentMatchers.eq("VINCULO_COBRANCA"),
                        org.mockito.ArgumentMatchers.eq(7L),
                        org.mockito.ArgumentMatchers.eq("ENCERRAMENTO_VIGENCIA"),
                        any(),
                        any(),
                        any());
        verify(operations).hash("ENCERRAMENTO_VIGENCIA", List.of("VINCULO", 7L), command);
    }
}
