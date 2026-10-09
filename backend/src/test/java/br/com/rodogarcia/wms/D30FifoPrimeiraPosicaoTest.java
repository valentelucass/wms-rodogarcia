package br.com.rodogarcia.wms;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import br.com.rodogarcia.wms.models.Armazem;
import br.com.rodogarcia.wms.models.ConjuntoPosicoes;
import br.com.rodogarcia.wms.models.Endereco;
import br.com.rodogarcia.wms.models.ItemPedidoSaida;
import br.com.rodogarcia.wms.models.MedidasUnidade;
import br.com.rodogarcia.wms.models.OcupacaoEndereco;
import br.com.rodogarcia.wms.models.PedidoSaida;
import br.com.rodogarcia.wms.models.TipoEndereco;
import br.com.rodogarcia.wms.models.TipoUnidadeLogistica;
import br.com.rodogarcia.wms.models.UnidadeLogistica;
import br.com.rodogarcia.wms.repositories.ConteudoUnidadeRepository;
import br.com.rodogarcia.wms.repositories.ItemPedidoSaidaRepository;
import br.com.rodogarcia.wms.repositories.OcupacaoEnderecoRepository;
import br.com.rodogarcia.wms.repositories.OperacaoSaidaRepository;
import br.com.rodogarcia.wms.repositories.PedidoSaidaRepository;
import br.com.rodogarcia.wms.repositories.ReservaSaidaRepository;
import br.com.rodogarcia.wms.services.AuditoriaService;
import br.com.rodogarcia.wms.services.EstoqueService;
import br.com.rodogarcia.wms.services.OperacaoAdministrativaService;
import br.com.rodogarcia.wms.services.PedidoSaidaService;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.json.JsonMapper;

/** Isola o merge Java de duas posições; não comprova filtros/materialização SQL ou ID final. */
class D30FifoPrimeiraPosicaoTest {
    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void primeiraPosicaoPorNivelVenceIdContrarioSemAlterarEstado(boolean inverter)
            throws Exception {
        var origem = D30MarcosTest.origem();
        ReflectionTestUtils.setField(origem.cliente(), "id", 1L);
        ReflectionTestUtils.setField(origem.armazem(), "id", 2L);
        ReflectionTestUtils.setField(origem.produto(), "id", 3L);
        UnidadeLogistica a = origem.unidade();
        UnidadeLogistica b = new UnidadeLogistica(a, new BigDecimal("10"), D30MarcosTest.CHEGADA);
        ReflectionTestUtils.setField(a, "id", 11L);
        ReflectionTestUtils.setField(b, "id", 12L);
        Endereco a9 = endereco(origem.armazem(), 1, "A9", 9);
        Endereco a0 = endereco(origem.armazem(), 99, "A0", 0);
        Endereco b1 = endereco(origem.armazem(), 2, "B1", 1);
        var conjunto =
                new ConjuntoPosicoes(
                        origem.armazem(),
                        "A9-A0",
                        a9,
                        a0,
                        new BigDecimal("2000"),
                        new BigDecimal("2"),
                        new BigDecimal("2"),
                        new BigDecimal("2"),
                        1,
                        D30MarcosTest.FIFO);
        ReflectionTestUtils.setField(conjunto, "id", 31L);
        a.posicionar(medidas(2), TipoEndereco.ARMAZENAGEM, conjunto, D30MarcosTest.ARMAZENAGEM);
        b.posicionar(medidas(1), TipoEndereco.ARMAZENAGEM, null, D30MarcosTest.ARMAZENAGEM);
        var oa9 = new OcupacaoEndereco(a9);
        oa9.atribuir(a);
        var oa0 = new OcupacaoEndereco(a0);
        oa0.atribuir(a);
        var ob1 = new OcupacaoEndereco(b1);
        ob1.atribuir(b);
        var ocupadas = inverter ? List.of(oa0, ob1, oa9) : List.of(oa9, ob1, oa0);
        var candidatas = inverter ? List.of(a, b) : List.of(b, a);
        var pedido =
                new PedidoSaida(
                        origem.cliente(), origem.armazem(), "D30-FIFO", D30MarcosTest.ARMAZENAGEM);
        ReflectionTestUtils.setField(pedido, "id", 44L);
        var item = new ItemPedidoSaida(pedido, origem.produto(), BigDecimal.ONE);
        var fotoA = foto(a);
        var fotoB = foto(b);
        var fotoOcupacoes =
                ocupadas.stream()
                        .map(o -> List.of(o.getEndereco().getId(), o.getUnidade().getId()))
                        .toList();
        var pedidoAlterado = pedido.getAlteradoEm();

        Map<Class<?>, Object> dependencias = new HashMap<>();
        dependencias.put(Clock.class, Clock.systemUTC());
        dependencias.put(JsonMapper.class, JsonMapper.builder().build());
        var constructor = PedidoSaidaService.class.getConstructors()[0];
        var argumentos =
                java.util.Arrays.stream(constructor.getParameterTypes())
                        .map(
                                tipo ->
                                        dependencias.computeIfAbsent(
                                                tipo, D30FifoPrimeiraPosicaoTest::mockTipo))
                        .toArray();
        var service = (PedidoSaidaService) constructor.newInstance(argumentos);
        var pedidos = (PedidoSaidaRepository) dependencias.get(PedidoSaidaRepository.class);
        var itens = (ItemPedidoSaidaRepository) dependencias.get(ItemPedidoSaidaRepository.class);
        var ocupacoes =
                (OcupacaoEnderecoRepository) dependencias.get(OcupacaoEnderecoRepository.class);
        var estoque = (EstoqueService) dependencias.get(EstoqueService.class);
        when(pedidos.findById(44L)).thenReturn(Optional.of(pedido));
        when(itens.buscarDoPedido(44L)).thenReturn(List.of(item));
        when(estoque.candidatas(1L, 2L, List.of(3L))).thenReturn(candidatas);
        when(ocupacoes.buscarDasUnidades(candidatas.stream().map(UnidadeLogistica::getId).toList()))
                .thenReturn(ocupadas);

        var sugestao = service.sugerir(44L);
        assertThat(sugestao.selecoes()).hasSize(1);
        assertThat(sugestao.selecoes().getFirst().unidadeId()).isEqualTo(11L);
        assertThat(sugestao.selecoes().getFirst().quantidade()).isEqualByComparingTo("1");
        assertThat(foto(a)).isEqualTo(fotoA);
        assertThat(foto(b)).isEqualTo(fotoB);
        assertThat(
                        ocupadas.stream()
                                .map(o -> List.of(o.getEndereco().getId(), o.getUnidade().getId()))
                                .toList())
                .isEqualTo(fotoOcupacoes);
        assertThat(pedido.getAlteradoEm()).isEqualTo(pedidoAlterado);
        assertThat(pedido.getVersao()).isZero();
        assertThat(item.getQuantidade()).isEqualByComparingTo("1");
        verify(pedidos).findById(44L);
        verify(itens).buscarDoPedido(44L);
        verify(estoque).candidatas(1L, 2L, List.of(3L));
        verify(ocupacoes)
                .buscarDasUnidades(candidatas.stream().map(UnidadeLogistica::getId).toList());
        verifyNoMoreInteractions(pedidos, itens, estoque, ocupacoes);
        verifyNoInteractions(
                dependencias.get(ReservaSaidaRepository.class),
                dependencias.get(OperacaoSaidaRepository.class),
                dependencias.get(AuditoriaService.class),
                dependencias.get(ConteudoUnidadeRepository.class),
                dependencias.get(OperacaoAdministrativaService.class));
    }

    private static Object mockTipo(Class<?> tipo) {
        return mock(tipo);
    }

    private static Endereco endereco(Armazem armazem, long id, String codigo, int nivel) {
        var e =
                new Endereco(
                        armazem,
                        codigo,
                        "A",
                        nivel,
                        codigo,
                        "Posição fictícia",
                        TipoEndereco.ARMAZENAGEM,
                        new BigDecimal("1000"),
                        new BigDecimal("2"),
                        BigDecimal.ONE,
                        new BigDecimal("2"),
                        1,
                        1,
                        D30MarcosTest.FIFO);
        ReflectionTestUtils.setField(e, "id", id);
        e.configurarFisico(
                TipoUnidadeLogistica.PALLET,
                new BigDecimal("1000"),
                new BigDecimal("2"),
                BigDecimal.ONE,
                new BigDecimal("2"),
                1,
                D30MarcosTest.CHEGADA);
        return e;
    }

    private static MedidasUnidade medidas(int posicoes) {
        return new MedidasUnidade(
                new BigDecimal("10"),
                BigDecimal.ONE,
                new BigDecimal(posicoes),
                BigDecimal.ONE,
                1,
                posicoes);
    }

    record Foto(
            String codigo,
            BigDecimal quantidade,
            Instant fifo,
            Instant chegada,
            Instant primeiro,
            Instant inicio,
            Instant alterada,
            long versao,
            long revisao,
            TipoEndereco localizacao,
            Long conjunto,
            int posicoes,
            boolean ativa,
            boolean bloqueada,
            PedidoSaida reserva) {}

    private static Foto foto(UnidadeLogistica u) {
        return new Foto(
                u.getCodigo(),
                u.getQuantidade(),
                u.getDataFifo(),
                u.getChegadaReal(),
                u.getPrimeiroEnderecamentoEm(),
                u.getInicioArmazenagemEm(),
                u.getAlteradaEm(),
                u.getVersao(),
                u.getRevisaoConteudo(),
                u.getTipoLocalizacao(),
                u.getConjuntoAtual() == null ? null : u.getConjuntoAtual().getId(),
                u.getPosicoesEquivalentes(),
                u.isAtiva(),
                u.isBloqueada(),
                u.getReservaSaida());
    }
}
