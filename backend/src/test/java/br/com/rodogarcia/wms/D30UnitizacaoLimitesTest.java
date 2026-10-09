package br.com.rodogarcia.wms;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.rodogarcia.wms.dto.PedidoEntradaDto;
import br.com.rodogarcia.wms.dto.UnidadeLogisticaDto;
import br.com.rodogarcia.wms.exceptions.RegraNegocioException;
import br.com.rodogarcia.wms.models.ChegadaRecebimento;
import br.com.rodogarcia.wms.models.CondicaoMercadoria;
import br.com.rodogarcia.wms.models.ConteudoUnidade;
import br.com.rodogarcia.wms.models.EntradaConferida;
import br.com.rodogarcia.wms.models.ItemChegada;
import br.com.rodogarcia.wms.models.ItemNotaEntrada;
import br.com.rodogarcia.wms.models.TipoUnidadeLogistica;
import br.com.rodogarcia.wms.models.UnidadeLogistica;
import br.com.rodogarcia.wms.repositories.ConteudoUnidadeRepository;
import br.com.rodogarcia.wms.repositories.EmbalagemRepository;
import br.com.rodogarcia.wms.repositories.EntradaConferidaRepository;
import br.com.rodogarcia.wms.repositories.OperacaoUnidadeRepository;
import br.com.rodogarcia.wms.repositories.UnidadeLogisticaRepository;
import br.com.rodogarcia.wms.services.AcessoService;
import br.com.rodogarcia.wms.services.EstoqueService;
import br.com.rodogarcia.wms.services.PedidoEntradaService;
import br.com.rodogarcia.wms.services.UnidadeLogisticaService;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.json.JsonMapper;

/** Limites do serviço com entidades reais e repositories doubles; não prova persistência nativa. */
class D30UnitizacaoLimitesTest {
    private static final Instant AGORA = Instant.parse("2026-09-05T12:00:00.567890Z");

    private static class Fixture {
        final D30MarcosTest.Origem o = D30MarcosTest.origem();
        final PedidoEntradaService pedidos = mock(PedidoEntradaService.class);
        final EntradaConferidaRepository entradas = mock(EntradaConferidaRepository.class);
        final UnidadeLogisticaRepository unidades = mock(UnidadeLogisticaRepository.class);
        final ConteudoUnidadeRepository conteudos = mock(ConteudoUnidadeRepository.class);
        final OperacaoUnidadeRepository operacoes = mock(OperacaoUnidadeRepository.class);
        final EmbalagemRepository embalagens = mock(EmbalagemRepository.class);
        final AcessoService acesso = mock(AcessoService.class);
        final EstoqueService estoque = mock(EstoqueService.class);
        final List<UnidadeLogistica> salvas = new ArrayList<>();
        final Map<Long, List<ConteudoUnidade>> composicoes = new HashMap<>();
        final UnidadeLogisticaService service;

        Fixture() {
            var entidades =
                    List.of(
                            o.cliente(),
                            o.armazem(),
                            o.produto(),
                            o.embalagem(),
                            o.pedido(),
                            o.nota());
            for (int i = 0; i < entidades.size(); i++)
                ReflectionTestUtils.setField(entidades.get(i), "id", 1L + i);
            ReflectionTestUtils.setField(o.entrada1(), "id", 31L);
            ReflectionTestUtils.setField(o.item1(), "id", 21L);
            ReflectionTestUtils.setField(o.item(), "id", 11L);
            when(pedidos.bloquear(5L)).thenReturn(o.pedido());
            when(pedidos.registrar(eq(o.pedido()), any(), anyString(), anyString(), any()))
                    .thenReturn(PedidoEntradaDto.Resumo.de(o.pedido()));
            when(entradas.findByIdAndItemChegadaChegadaPedidoId(31L, 5L))
                    .thenReturn(Optional.of(o.entrada1()));
            when(embalagens.buscarParaAtualizar(4L)).thenReturn(Optional.of(o.embalagem()));
            when(estoque.disponiveis(anyList())).thenReturn(Set.of());
            when(acesso.usuario()).thenReturn("D30-limites-ficticio");
            when(unidades.save(any(UnidadeLogistica.class)))
                    .thenAnswer(
                            inv -> {
                                var u = inv.getArgument(0, UnidadeLogistica.class);
                                ReflectionTestUtils.setField(u, "id", 500L + salvas.size());
                                salvas.add(u);
                                return u;
                            });
            when(conteudos.buscarOrigens(anyLong()))
                    .thenAnswer(inv -> composicoes.getOrDefault(inv.getArgument(0), List.of()));
            when(conteudos.save(any(ConteudoUnidade.class)))
                    .thenAnswer(
                            inv -> {
                                var c = inv.getArgument(0, ConteudoUnidade.class);
                                composicoes
                                        .computeIfAbsent(
                                                c.getUnidade().getId(), id -> new ArrayList<>())
                                        .add(c);
                                return c;
                            });
            service =
                    new UnidadeLogisticaService(
                            pedidos,
                            entradas,
                            unidades,
                            conteudos,
                            operacoes,
                            embalagens,
                            acesso,
                            JsonMapper.builder().build(),
                            Clock.fixed(AGORA, ZoneOffset.UTC),
                            estoque);
        }
    }

    @ParameterizedTest
    @ValueSource(longs = {9999, 10000})
    void limiteHistoricoSomaUmaIdentidadeSemExcluirEncerradas(long historico) {
        var f = new Fixture();
        when(f.unidades.countByPedidoId(5L)).thenReturn(historico);
        var dados =
                new UnidadeLogisticaDto.Unitizar(
                        UUID.randomUUID(),
                        0L,
                        "Fronteira historica D30",
                        List.of(
                                new UnidadeLogisticaDto.NovaUnidade(
                                        4L,
                                        TipoUnidadeLogistica.PALLET,
                                        CondicaoMercadoria.BOA,
                                        BigDecimal.TEN)));
        assertThat(f.o.entrada1().getUnitizadaEm()).isNull();
        if (historico == 10000) {
            assertThatThrownBy(() -> f.service.unitizar(5L, 31L, dados))
                    .isInstanceOfSatisfying(
                            RegraNegocioException.class,
                            e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
            assertThat(f.salvas).isEmpty();
            assertThat(f.o.entrada1().getUnitizadaEm()).isNull();
            verify(f.unidades, never()).save(any());
            verify(f.conteudos, never()).save(any());
            verify(f.operacoes, never()).saveAndFlush(any());
            verify(f.pedidos, never()).registrar(any(), any(), anyString(), anyString(), any());
        } else {
            var confirmado = f.service.unitizar(5L, 31L, dados);
            assertThat(confirmado.unidades()).hasSize(1);
            assertThat(confirmado.unidades().getFirst().unidade().quantidade())
                    .isEqualByComparingTo("10");
            assertThat(confirmado.unidades().getFirst().origens())
                    .containsExactly(new UnidadeLogisticaDto.Origem(31L, 21L, 11L, BigDecimal.TEN));
            assertThat(f.o.entrada1().getUnitizadaEm()).isEqualTo(AGORA);
            verify(f.unidades).save(any());
            verify(f.conteudos).save(any());
            verify(f.operacoes).saveAndFlush(any());
        }
        verify(f.unidades).countByPedidoId(5L);
        assertThat(f.o.item().getQuantidadePrevista()).isEqualByComparingTo("20");
    }

    @ParameterizedTest
    @ValueSource(ints = {100, 101})
    void reagrupamentoConfrontaEntradasDistintasSemConfundirNumeroDeUnidades(int total) {
        var f = new Fixture();
        var chegadas = new ArrayList<ChegadaRecebimento>();
        for (int i = 0; i < 2; i++)
            chegadas.add(
                    new ChegadaRecebimento(
                            f.o.pedido(),
                            UUID.randomUUID().toString(),
                            String.valueOf(i).repeat(64),
                            D30MarcosTest.CHEGADA,
                            D30MarcosTest.CHEGADA,
                            "D30-local",
                            "Composicao ficticia"));
        var entradas = new ArrayList<EntradaConferida>();
        for (int i = 0; i < total; i++) {
            var item =
                    new ItemNotaEntrada(
                            f.o.nota(), i + 1, f.o.produto(), BigDecimal.ONE, BigDecimal.TEN);
            var ic =
                    new ItemChegada(
                            chegadas.get(i % 2),
                            i / 2 + 1,
                            item,
                            null,
                            null,
                            BigDecimal.ONE,
                            BigDecimal.ZERO);
            var e =
                    new EntradaConferida(
                            ic,
                            D30MarcosTest.FIFO,
                            D30MarcosTest.CHEGADA,
                            BigDecimal.ONE,
                            BigDecimal.ZERO);
            ReflectionTestUtils.setField(item, "id", 1000L + i);
            ReflectionTestUtils.setField(ic, "id", 2000L + i);
            ReflectionTestUtils.setField(e, "id", 3000L + i);
            e.marcarUnitizada(AGORA);
            entradas.add(e);
        }
        assertThat(entradas).hasSize(total);
        assertThat(chegadas).hasSize(2);
        assertThat(total).isLessThanOrEqualTo(200);
        var destino =
                new UnidadeLogistica(
                        entradas.getFirst(),
                        f.o.embalagem(),
                        TipoUnidadeLogistica.PALLET,
                        CondicaoMercadoria.BOA,
                        new BigDecimal(total - 1),
                        AGORA);
        var origem =
                new UnidadeLogistica(
                        entradas.getLast(),
                        f.o.embalagem(),
                        TipoUnidadeLogistica.PALLET,
                        CondicaoMercadoria.BOA,
                        BigDecimal.ONE,
                        AGORA);
        ReflectionTestUtils.setField(destino, "id", 100L);
        ReflectionTestUtils.setField(origem, "id", 101L);
        var composicao = new ArrayList<ConteudoUnidade>();
        for (int i = 0; i < total - 1; i++)
            composicao.add(new ConteudoUnidade(destino, entradas.get(i), BigDecimal.ONE));
        f.composicoes.put(100L, composicao);
        f.composicoes.put(
                101L,
                new ArrayList<>(
                        List.of(new ConteudoUnidade(origem, entradas.getLast(), BigDecimal.ONE))));
        when(f.unidades.findByIdAndPedidoId(100L, 5L)).thenReturn(Optional.of(destino));
        when(f.unidades.findByIdAndPedidoId(101L, 5L)).thenReturn(Optional.of(origem));
        when(f.unidades.buscarEmbalagens(eq(5L), anyList())).thenReturn(List.of(4L));
        var dados =
                new UnidadeLogisticaDto.Reagrupar(
                        UUID.randomUUID(),
                        0L,
                        List.of(new UnidadeLogisticaDto.RevisaoUnidade(101L, 0L)),
                        "Composicao fronteira D30");
        var antesDestino = UnidadeLogisticaDto.Resumo.de(destino, false);
        var antesOrigem = UnidadeLogisticaDto.Resumo.de(origem, false);
        if (total == 101) {
            assertThatThrownBy(() -> f.service.reagrupar(5L, 100L, dados))
                    .isInstanceOfSatisfying(
                            RegraNegocioException.class,
                            e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
            assertThat(UnidadeLogisticaDto.Resumo.de(destino, false)).isEqualTo(antesDestino);
            assertThat(UnidadeLogisticaDto.Resumo.de(origem, false)).isEqualTo(antesOrigem);
            assertThat(
                            f.composicoes.values().stream()
                                    .flatMap(List::stream)
                                    .map(ConteudoUnidade::getQuantidade))
                    .allSatisfy(q -> assertThat(q).isEqualByComparingTo("1"));
            verify(f.conteudos, never()).save(any());
            verify(f.unidades, never()).flush();
            verify(f.operacoes, never()).saveAndFlush(any());
        } else {
            var confirmado = f.service.reagrupar(5L, 100L, dados);
            assertThat(confirmado.unidades().getFirst().origens())
                    .hasSize(100)
                    .extracting(UnidadeLogisticaDto.Origem::entradaId)
                    .doesNotHaveDuplicates();
            assertThat(destino.getQuantidade()).isEqualByComparingTo("100");
            assertThat(origem.getQuantidade()).isEqualByComparingTo("0");
            assertThat(origem.isAtiva()).isFalse();
            assertThat(
                            f.composicoes.get(100L).stream()
                                    .map(ConteudoUnidade::getQuantidade)
                                    .reduce(BigDecimal.ZERO, BigDecimal::add))
                    .isEqualByComparingTo("100");
            assertThat(f.composicoes.get(101L).getFirst().getQuantidade())
                    .isEqualByComparingTo("0");
            verify(f.conteudos).save(any());
            verify(f.operacoes).saveAndFlush(any());
        }
    }
}
