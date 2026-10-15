package br.com.rodogarcia.wms;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import br.com.rodogarcia.wms.dto.ConfiguracaoCobrancaDto;
import br.com.rodogarcia.wms.models.Armazem;
import br.com.rodogarcia.wms.models.Cliente;
import br.com.rodogarcia.wms.models.ItemTabelaCobranca;
import br.com.rodogarcia.wms.models.ServicoCobranca;
import br.com.rodogarcia.wms.models.TabelaCobranca;
import br.com.rodogarcia.wms.models.VinculoTabelaCliente;
import br.com.rodogarcia.wms.repositories.ArmazemRepository;
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
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

/** Serviços/DTOs em memória com mocks; nenhum contexto Spring, JDBC, H2 ou persistência. */
class TabelaCobrancaEdicaoTest {
    private final Instant agora = Instant.parse("2026-10-15T12:00:00Z");
    private final LocalDate inicio = LocalDate.of(2026, 10, 1);
    private final Armazem armazem =
            id(new Armazem("ARM-1", "Armazém", "123", "Bauru", "SP", agora), 1L);
    private final TabelaCobranca tabela =
            id(
                    new TabelaCobranca(
                            armazem,
                            null,
                            "TAB-1",
                            "Original",
                            "PADRAO",
                            inicio,
                            null,
                            "ATIVA",
                            agora,
                            agora),
                    10L);
    private final AcessoService acesso = mock(AcessoService.class);
    private final AuditoriaService auditoria = mock(AuditoriaService.class);
    private final OperacaoAdministrativaService operacoes =
            mock(OperacaoAdministrativaService.class);
    private final TabelaCobrancaRepository tabelas = mock(TabelaCobrancaRepository.class);
    private final ItemTabelaCobrancaRepository itens = mock(ItemTabelaCobrancaRepository.class);
    private final ServicoCobrancaRepository servicos = mock(ServicoCobrancaRepository.class);
    private final VinculoTabelaClienteRepository vinculos =
            mock(VinculoTabelaClienteRepository.class);
    private final CalculoCobrancaRepository calculos = mock(CalculoCobrancaRepository.class);
    private final MemoriaDiariaRepository diarias = mock(MemoriaDiariaRepository.class);
    private final ArmazemRepository armazens = mock(ArmazemRepository.class);
    private final ContextoCobrancaService contextos = mock(ContextoCobrancaService.class);
    private final List<ItemTabelaCobranca> registros = new ArrayList<>();
    private final ConfiguracaoCobrancaService service =
            new ConfiguracaoCobrancaService(
                    servicos,
                    tabelas,
                    itens,
                    vinculos,
                    mock(ContratoCobrancaRepository.class),
                    mock(ServicoMinimoContratoRepository.class),
                    armazens,
                    calculos,
                    diarias,
                    contextos,
                    operacoes,
                    auditoria,
                    acesso,
                    Clock.fixed(agora, ZoneOffset.UTC),
                    mock(ResolucaoFinanceiraCadastroService.class));

    private static <T> T id(T objeto, long id) {
        ReflectionTestUtils.setField(objeto, "id", id);
        return objeto;
    }

    @BeforeEach
    void preparar() {
        when(tabelas.findById(10L)).thenReturn(Optional.of(tabela));
        when(tabelas.buscarParaAtualizar(10L)).thenReturn(Optional.of(tabela));
        when(armazens.buscarParaAtualizar(1L)).thenReturn(Optional.of(armazem));
        when(operacoes.hash(eq("ALTERACAO_TABELA"), eq(10L), any())).thenReturn("hash");
        when(itens.findByTabelaIdOrderByIdAsc(10L))
                .thenAnswer(i -> registros.stream().filter(ItemTabelaCobranca::isAtivo).toList());
        when(itens.save(any()))
                .thenAnswer(
                        i -> {
                            var item = (ItemTabelaCobranca) i.getArgument(0);
                            id(item, 100 + registros.size());
                            registros.add(item);
                            return item;
                        });
        when(servicos.buscarParaAtualizar(anyLong()))
                .thenAnswer(i -> Optional.of(servico(i.getArgument(0))));
        when(servicos.getReferenceById(anyLong())).thenAnswer(i -> servico(i.getArgument(0)));
    }

    private ServicoCobranca servico(Long id) {
        return id(
                new ServicoCobranca(
                        "SRV-" + id,
                        "Serviço",
                        "ENTRADA",
                        "UNIDADE_LOGISTICA",
                        "ATIVO",
                        agora,
                        agora),
                id);
    }

    private ConfiguracaoCobrancaDto.Item item(long servico, String categoria, String preco) {
        return new ConfiguracaoCobrancaDto.Item(servico, categoria, new BigDecimal(preco), null);
    }

    private ConfiguracaoCobrancaDto.AlterarTabela comando(
            List<ConfiguracaoCobrancaDto.Item> novos) {
        return new ConfiguracaoCobrancaDto.AlterarTabela(
                UUID.randomUUID(),
                0L,
                1L,
                null,
                "Corrigida",
                "PADRAO",
                inicio,
                null,
                novos,
                "Correção dos valores contratados");
    }

    @Test
    void alteraPrecoRemoveAdicionaSemApagarTarifasHistoricas() {
        var antigo =
                id(
                        new ItemTabelaCobranca(
                                tabela, servico(2L), "BOBINA", new BigDecimal("2.50"), null),
                        20L);
        var removido =
                id(new ItemTabelaCobranca(tabela, servico(3L), "", BigDecimal.TEN, null), 21L);
        registros.addAll(List.of(antigo, removido));
        when(calculos.tabelaUsada(10L)).thenReturn(true);
        var depois =
                service.alterar(
                        10L, comando(List.of(item(2, "bobina", "3.123456"), item(4, "", "0"))));
        assertThat(depois.itens())
                .extracting(ConfiguracaoCobrancaDto.ItemResposta::servicoId)
                .containsExactly(2L, 4L);
        assertThat(depois.itens().getFirst().preco()).isEqualByComparingTo("3.123456");
        assertThat(antigo.getPreco()).isEqualByComparingTo("2.50");
        assertThat(antigo.isAtivo()).isFalse();
        assertThat(removido.isAtivo()).isFalse();
        assertThat(registros).hasSize(4);
        var antes = ArgumentCaptor.forClass(Object.class);
        verify(auditoria)
                .registrar(
                        eq("TABELA_COBRANCA"),
                        eq(10L),
                        eq("ALTERACAO"),
                        any(),
                        antes.capture(),
                        eq(depois));
        assertThat(((ConfiguracaoCobrancaDto.Tabela) antes.getValue()).itens()).hasSize(2);
        verify(itens, never()).delete(any());
        verify(calculos, never()).save(any());
    }

    @Test
    void mantemIdDeItemSemMudancaIncluindoEscalaDecimal() {
        registros.add(
                id(
                        new ItemTabelaCobranca(
                                tabela, servico(2L), "", new BigDecimal("2.50"), null),
                        20L));
        var depois = service.alterar(10L, comando(List.of(item(2, "", "2.500000"))));
        assertThat(depois.itens().getFirst().id()).isEqualTo(20L);
        verify(itens, never()).save(any());
    }

    @Test
    void recusaRevisaoAntigaAntesDeAlterarItens() {
        ReflectionTestUtils.setField(tabela, "versao", 2L);
        assertThatThrownBy(() -> service.alterar(10L, comando(List.of(item(2, "", "1")))))
                .hasMessageContaining("Configuração conflitante");
        verifyNoInteractions(itens, auditoria);
    }

    @Test
    void replayDevolveRespostaOriginalSemDuplicarItens() {
        var d = comando(List.of(item(2, "", "1")));
        var resposta = ConfiguracaoCobrancaDto.Tabela.de(tabela, List.of());
        when(operacoes.repetida(d.operacaoId(), "hash", ConfiguracaoCobrancaDto.Tabela.class))
                .thenReturn(resposta);
        assertThat(service.alterar(10L, d)).isSameAs(resposta);
        verifyNoInteractions(itens, auditoria);
    }

    @Test
    void exigeGestorAntesDeConsultarDados() {
        doThrow(new AccessDeniedException("Gestor obrigatório")).when(acesso).exigirGestor();
        assertThatThrownBy(() -> service.alterar(10L, comando(List.of(item(2, "", "1")))))
                .isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(tabelas, itens, armazens);
    }

    @Test
    void duplicataNormalizadaNaoRetiraItens() {
        assertThatThrownBy(
                        () ->
                                service.alterar(
                                        10L,
                                        comando(
                                                List.of(
                                                        item(2, "bobina", "1"),
                                                        item(2, "BOBINA", "2")))))
                .hasMessageContaining("Configuração conflitante");
        verify(itens, never()).flush();
    }

    @Test
    void naoEncurtaVigenciaComVinculoAberto() {
        var v = mock(VinculoTabelaCliente.class);
        when(v.getVigenciaInicio()).thenReturn(inicio);
        when(vinculos.findByTabelaId(10L)).thenReturn(List.of(v));
        var d = comando(List.of(item(2, "", "1")));
        var fim =
                new ConfiguracaoCobrancaDto.AlterarTabela(
                        d.operacaoId(),
                        d.versao(),
                        d.armazemId(),
                        null,
                        d.descricao(),
                        d.tipo(),
                        inicio,
                        inicio.plusDays(5),
                        d.itens(),
                        d.motivo());
        assertThatThrownBy(() -> service.alterar(10L, fim))
                .hasMessageContaining("abranger seus vínculos");
        verifyNoInteractions(itens);
    }

    @Test
    void naoMudaPeriodoDeTabelaJaCalculada() {
        when(diarias.existsByTabelaId(10L)).thenReturn(true);
        var d = comando(List.of(item(2, "", "1")));
        var novo =
                new ConfiguracaoCobrancaDto.AlterarTabela(
                        d.operacaoId(),
                        d.versao(),
                        d.armazemId(),
                        null,
                        d.descricao(),
                        d.tipo(),
                        inicio.minusDays(1),
                        null,
                        d.itens(),
                        d.motivo());
        assertThatThrownBy(() -> service.alterar(10L, novo))
                .hasMessageContaining("já possui cálculo");
        verifyNoInteractions(itens);
    }

    @Test
    void tabelaEncerradaNaoRecebeEdicao() {
        tabela.encerrar(inicio.plusDays(5), agora);
        assertThatThrownBy(() -> service.alterar(10L, comando(List.of(item(2, "", "1")))))
                .hasMessageContaining("Configuração conflitante");
        verifyNoInteractions(itens, auditoria);
    }

    @Test
    void corrigeClienteArmazemTipoEPeriodoSemUsoAnterior() {
        var destino = id(new Armazem("ARM-2", "Destino", "123", "Bauru", "SP", agora), 2L);
        var cliente = id(new Cliente("CLI-4", "Cliente", "123", agora), 4L);
        when(armazens.buscarParaAtualizar(2L)).thenReturn(Optional.of(destino));
        when(contextos.bloquearCliente(4L)).thenReturn(cliente);
        var d = comando(List.of(item(2, "", "1")));
        var novo =
                new ConfiguracaoCobrancaDto.AlterarTabela(
                        d.operacaoId(),
                        d.versao(),
                        2L,
                        4L,
                        d.descricao(),
                        "ESPECIFICA",
                        inicio.plusDays(1),
                        inicio.plusDays(20),
                        d.itens(),
                        d.motivo());
        var r = service.alterar(10L, novo);
        assertThat(r.clienteId()).isEqualTo(4L);
        assertThat(r.armazemId()).isEqualTo(2L);
        assertThat(r.tipo()).isEqualTo("ESPECIFICA");
        assertThat(r.vigenciaInicio()).isEqualTo(inicio.plusDays(1));
        assertThat(r.codigo()).isEqualTo("TAB-1");
        var ordem = org.mockito.Mockito.inOrder(contextos, armazens);
        ordem.verify(contextos).bloquearCliente(4L);
        ordem.verify(armazens).buscarParaAtualizar(1L);
        ordem.verify(armazens).buscarParaAtualizar(2L);
        verify(acesso).cliente(4L);
    }

    @Test
    void naoTransfereTabelaVinculadaParaOutroArmazem() {
        var destino = id(new Armazem("ARM-2", "Destino", "123", "Bauru", "SP", agora), 2L);
        when(armazens.buscarParaAtualizar(2L)).thenReturn(Optional.of(destino));
        when(vinculos.findByTabelaId(10L)).thenReturn(List.of(mock(VinculoTabelaCliente.class)));
        var d = comando(List.of(item(2, "", "1")));
        var novo =
                new ConfiguracaoCobrancaDto.AlterarTabela(
                        d.operacaoId(),
                        d.versao(),
                        2L,
                        null,
                        d.descricao(),
                        "PADRAO",
                        inicio,
                        null,
                        d.itens(),
                        d.motivo());
        assertThatThrownBy(() -> service.alterar(10L, novo))
                .hasMessageContaining("não podem mudar após vínculo");
        assertThat(tabela.getArmazem()).isSameAs(armazem);
        verifyNoInteractions(itens, auditoria);
    }
}
