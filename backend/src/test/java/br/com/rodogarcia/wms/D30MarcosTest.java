package br.com.rodogarcia.wms;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import br.com.rodogarcia.wms.dto.EstoqueDto;
import br.com.rodogarcia.wms.dto.UnidadeLogisticaDto;
import br.com.rodogarcia.wms.models.Armazem;
import br.com.rodogarcia.wms.models.ChegadaRecebimento;
import br.com.rodogarcia.wms.models.Cliente;
import br.com.rodogarcia.wms.models.CondicaoMercadoria;
import br.com.rodogarcia.wms.models.Embalagem;
import br.com.rodogarcia.wms.models.EntradaConferida;
import br.com.rodogarcia.wms.models.ItemChegada;
import br.com.rodogarcia.wms.models.ItemNotaEntrada;
import br.com.rodogarcia.wms.models.MedidasUnidade;
import br.com.rodogarcia.wms.models.NotaEntrada;
import br.com.rodogarcia.wms.models.PedidoEntrada;
import br.com.rodogarcia.wms.models.Produto;
import br.com.rodogarcia.wms.models.TipoEndereco;
import br.com.rodogarcia.wms.models.TipoQuantidade;
import br.com.rodogarcia.wms.models.TipoUnidadeLogistica;
import br.com.rodogarcia.wms.models.UnidadeLogistica;
import br.com.rodogarcia.wms.repositories.EntradaConferidaRepository;
import br.com.rodogarcia.wms.repositories.MovimentoEstoqueRepository;
import br.com.rodogarcia.wms.repositories.OcupacaoEnderecoRepository;
import br.com.rodogarcia.wms.repositories.ProdutoRepository;
import br.com.rodogarcia.wms.repositories.ReservaSaidaRepository;
import br.com.rodogarcia.wms.repositories.UnidadeLogisticaRepository;
import br.com.rodogarcia.wms.services.AcessoService;
import br.com.rodogarcia.wms.services.EstoqueService;
import br.com.rodogarcia.wms.services.PedidoEntradaService;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.json.JsonMapper;

/** Oráculos D30 fixados antes das operações; modelos/DTO/service reais, repos sem banco. */
class D30MarcosTest {
    static final Instant FIFO = Instant.parse("2026-09-01T12:00:00.123456Z");
    static final Instant CHEGADA = Instant.parse("2026-09-02T12:00:00.234567Z");
    static final Instant TRIAGEM = Instant.parse("2026-09-03T12:00:00.345678Z");
    static final Instant ARMAZENAGEM = Instant.parse("2026-09-04T12:00:00.456789Z");
    static final Instant REMANEJAMENTO = Instant.parse("2026-09-05T12:00:00.567890Z");

    record Origem(
            Cliente cliente,
            Armazem armazem,
            Produto produto,
            Embalagem embalagem,
            PedidoEntrada pedido,
            NotaEntrada nota,
            ItemNotaEntrada item,
            ChegadaRecebimento chegada1,
            ChegadaRecebimento chegada2,
            ItemChegada item1,
            ItemChegada item2,
            EntradaConferida entrada1,
            EntradaConferida entrada2,
            UnidadeLogistica unidade) {}

    static Origem origem() {
        var c = new Cliente("D30", "Cliente fictício", "11111111000111", FIFO);
        var a = new Armazem("D30", "Armazém fictício", "22222222000122", "Osasco", "SP", FIFO);
        var p =
                new Produto(
                        c,
                        "D30",
                        "Produto fictício",
                        "UN",
                        TipoQuantidade.CONTAGEM,
                        0,
                        false,
                        false,
                        null,
                        FIFO);
        var b =
                new Embalagem(
                        p,
                        "D30-ORIGEM",
                        "Embalagem preexistente fictícia",
                        new BigDecimal("10"),
                        FIFO);
        var pedido = new PedidoEntrada(c, a, "D30-MARCOS", FIFO);
        pedido.concluir("Origem fictícia local", CHEGADA, CHEGADA);
        var nota = new NotaEntrada(pedido, "11111111000111", 1, 30, LocalDate.of(2026, 9, 1), null);
        var item = new ItemNotaEntrada(nota, 1, p, new BigDecimal("20"), new BigDecimal("200"));
        var ch1 =
                new ChegadaRecebimento(
                        pedido,
                        UUID.randomUUID().toString(),
                        "0".repeat(64),
                        FIFO,
                        FIFO,
                        "D30-local",
                        "Primeira chegada fictícia");
        var ch2 =
                new ChegadaRecebimento(
                        pedido,
                        UUID.randomUUID().toString(),
                        "1".repeat(64),
                        CHEGADA,
                        CHEGADA,
                        "D30-local",
                        "Segunda chegada fictícia");
        var i1 = new ItemChegada(ch1, 1, item, null, null, new BigDecimal("10"), BigDecimal.ZERO);
        var i2 = new ItemChegada(ch2, 1, item, null, null, new BigDecimal("10"), BigDecimal.ZERO);
        var e1 = new EntradaConferida(i1, FIFO, CHEGADA, new BigDecimal("10"), BigDecimal.ZERO);
        var e2 = new EntradaConferida(i2, FIFO, CHEGADA, new BigDecimal("10"), BigDecimal.ZERO);
        e2.marcarUnitizada(CHEGADA.plusSeconds(3600));
        var u =
                new UnidadeLogistica(
                        e2,
                        b,
                        TipoUnidadeLogistica.PALLET,
                        CondicaoMercadoria.BOA,
                        new BigDecimal("10"),
                        CHEGADA.plusSeconds(3600));
        return new Origem(c, a, p, b, pedido, nota, item, ch1, ch2, i1, i2, e1, e2, u);
    }

    @Test
    void marcosIndependentesNaoNulosConservadosNoModeloServicoDtoEEtiqueta() {
        var origem = origem();
        var u = origem.unidade();
        ReflectionTestUtils.setField(u, "id", 101L);
        var unidades = mock(UnidadeLogisticaRepository.class);
        var ocupacoes = mock(OcupacaoEnderecoRepository.class);
        when(unidades.buscarDisponiveis(List.of(101L))).thenReturn(List.of());
        when(ocupacoes.buscarDasUnidades(List.of(101L))).thenReturn(List.of());
        var service =
                new EstoqueService(
                        unidades,
                        ocupacoes,
                        mock(EntradaConferidaRepository.class),
                        mock(MovimentoEstoqueRepository.class),
                        mock(ProdutoRepository.class),
                        mock(ReservaSaidaRepository.class),
                        mock(PedidoEntradaService.class),
                        mock(AcessoService.class),
                        JsonMapper.builder().build());
        assertThat(u.getDataFifo()).isEqualTo(FIFO);
        assertThat(u.getChegadaReal()).isEqualTo(CHEGADA);
        assertThat(u.getPrimeiroEnderecamentoEm()).isNull();
        assertThat(u.getInicioArmazenagemEm()).isNull();
        var medidas =
                new MedidasUnidade(
                        BigDecimal.TEN, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, 1, 1);
        u.posicionar(medidas, TipoEndereco.TRIAGEM, null, TRIAGEM);
        conferir(service.detalhe(u), TRIAGEM, null);
        u.posicionar(medidas, TipoEndereco.ARMAZENAGEM, null, ARMAZENAGEM);
        conferir(service.detalhe(u), TRIAGEM, ARMAZENAGEM);
        var etiquetaAntes = UnidadeLogisticaDto.Etiqueta.de(u);
        u.posicionar(medidas, TipoEndereco.ARMAZENAGEM, null, REMANEJAMENTO);
        conferir(service.detalhe(u), TRIAGEM, ARMAZENAGEM);
        assertThat(u.getDataFifo()).isEqualTo(FIFO);
        assertThat(u.getChegadaReal()).isEqualTo(CHEGADA);
        assertThat(u.getPrimeiroEnderecamentoEm()).isNotNull().isEqualTo(TRIAGEM);
        assertThat(u.getInicioArmazenagemEm()).isEqualTo(ARMAZENAGEM);
        var etiqueta = UnidadeLogisticaDto.Etiqueta.de(u);
        assertThat(etiqueta).isEqualTo(etiquetaAntes);
        assertThat(etiqueta.dataEntrada()).isEqualTo(FIFO);
        assertThat(etiqueta.chegadaReal()).isEqualTo(CHEGADA);
        assertThat(List.of(FIFO, CHEGADA, TRIAGEM, ARMAZENAGEM)).doesNotHaveDuplicates();
    }

    private void conferir(EstoqueDto.Unidade d, Instant primeiro, Instant inicio) {
        assertThat(d.unidade().dataFifo()).isEqualTo(FIFO);
        assertThat(d.unidade().chegadaReal()).isEqualTo(CHEGADA);
        assertThat(d.primeiroEnderecamentoEm()).isNotNull().isEqualTo(primeiro);
        assertThat(d.inicioArmazenagemEm()).isEqualTo(inicio);
    }
}
