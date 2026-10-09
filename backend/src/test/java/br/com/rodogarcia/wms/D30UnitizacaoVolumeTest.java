package br.com.rodogarcia.wms;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.rodogarcia.wms.dto.PedidoEntradaDto;
import br.com.rodogarcia.wms.dto.UnidadeLogisticaDto;
import br.com.rodogarcia.wms.models.Armazem;
import br.com.rodogarcia.wms.models.ChegadaRecebimento;
import br.com.rodogarcia.wms.models.Cliente;
import br.com.rodogarcia.wms.models.CondicaoMercadoria;
import br.com.rodogarcia.wms.models.ConteudoUnidade;
import br.com.rodogarcia.wms.models.Embalagem;
import br.com.rodogarcia.wms.models.EntradaConferida;
import br.com.rodogarcia.wms.models.ItemChegada;
import br.com.rodogarcia.wms.models.ItemNotaEntrada;
import br.com.rodogarcia.wms.models.NotaEntrada;
import br.com.rodogarcia.wms.models.OperacaoUnidade;
import br.com.rodogarcia.wms.models.PedidoEntrada;
import br.com.rodogarcia.wms.models.Produto;
import br.com.rodogarcia.wms.models.TipoQuantidade;
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
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.json.JsonMapper;

/** VOLUME de C2 no servico real; persistencia/controle de pedido sao doubles locais. */
class D30UnitizacaoVolumeTest {
    @Test
    void confirmaVolumeDeOutroClienteProdutoComOrigemDtoEtiquetaEReplaySemDuplicacao() {
        var mapperRef = new AtomicReference<JsonMapper>();
        new ApplicationContextRunner()
                .withInitializer(new ConfigDataApplicationContextInitializer())
                .withConfiguration(AutoConfigurations.of(JacksonAutoConfiguration.class))
                .withPropertyValues("spring.profiles.active=local")
                .run(
                        context -> {
                            assertThat(context).hasNotFailed();
                            assertThat(context.getBeansOfType(DataSource.class)).isEmpty();
                            mapperRef.set(context.getBean(JsonMapper.class));
                        });
        var mapper = mapperRef.get();
        var instante = Instant.parse("2026-09-05T12:00:00.567890Z");
        var fifo = Instant.parse("2026-09-01T12:00:00.123456Z");
        var chegada = Instant.parse("2026-09-02T12:00:00.234567Z");
        var cliente = new Cliente("C2", "Cliente distinto", "44444444000144", fifo);
        var armazem = new Armazem("A2", "Armazem distinto", "55555555000155", "Osasco", "SP", fifo);
        var produto =
                new Produto(
                        cliente,
                        "SKU-VOL-C2",
                        "Volume distinto",
                        "UN",
                        TipoQuantidade.CONTAGEM,
                        0,
                        false,
                        false,
                        null,
                        fifo);
        var embalagem =
                new Embalagem(produto, "DUN-VOL", "Volume de dois", new BigDecimal("2"), fifo);
        var pedido = new PedidoEntrada(cliente, armazem, "PE-C2-VOL", fifo);
        pedido.concluir("Conferencia ficticia", chegada, chegada);
        var nota = new NotaEntrada(pedido, "44444444000144", 2, 71, LocalDate.of(2026, 9, 1), null);
        var item = new ItemNotaEntrada(nota, 1, produto, new BigDecimal("6"), new BigDecimal("60"));
        var ch =
                new ChegadaRecebimento(
                        pedido,
                        UUID.randomUUID().toString(),
                        "2".repeat(64),
                        chegada,
                        chegada,
                        "Cedro-ficticio",
                        "Chegada ficticia C2");
        var ic = new ItemChegada(ch, 1, item, null, null, new BigDecimal("6"), BigDecimal.ZERO);
        var entrada = new EntradaConferida(ic, fifo, chegada, new BigDecimal("6"), BigDecimal.ZERO);
        var entidades =
                List.of(cliente, armazem, produto, embalagem, pedido, nota, item, ch, ic, entrada);
        for (int i = 0; i < entidades.size(); i++)
            ReflectionTestUtils.setField(entidades.get(i), "id", 22L + i);

        var anteriorC1 = D30MarcosTest.origem().unidade();
        var fotoC1 = UnidadeLogisticaDto.Resumo.de(anteriorC1, false);
        var pedidos = mock(PedidoEntradaService.class);
        var entradas = mock(EntradaConferidaRepository.class);
        var unidades = mock(UnidadeLogisticaRepository.class);
        var conteudos = mock(ConteudoUnidadeRepository.class);
        var operacoes = mock(OperacaoUnidadeRepository.class);
        var embalagens = mock(EmbalagemRepository.class);
        var acesso = mock(AcessoService.class);
        var estoque = mock(EstoqueService.class);
        var criadas = new ArrayList<UnidadeLogistica>();
        var origens = new ArrayList<ConteudoUnidade>();
        var operacao = new AtomicReference<OperacaoUnidade>();
        when(pedidos.bloquear(26L)).thenReturn(pedido);
        when(pedidos.registrar(
                        eq(pedido), any(), eq("UNIDADES_CRIADAS"), eq("Volume ficticio C2"), any()))
                .thenReturn(PedidoEntradaDto.Resumo.de(pedido));
        when(entradas.findByIdAndItemChegadaChegadaPedidoId(31L, 26L))
                .thenReturn(Optional.of(entrada));
        when(embalagens.buscarParaAtualizar(25L)).thenReturn(Optional.of(embalagem));
        when(unidades.countByPedidoId(26L)).thenReturn(0L);
        when(unidades.save(any(UnidadeLogistica.class)))
                .thenAnswer(
                        inv -> {
                            var e = inv.getArgument(0, UnidadeLogistica.class);
                            ReflectionTestUtils.setField(e, "id", 201L + criadas.size());
                            criadas.add(e);
                            return e;
                        });
        when(conteudos.save(any(ConteudoUnidade.class)))
                .thenAnswer(
                        inv -> {
                            var c = inv.getArgument(0, ConteudoUnidade.class);
                            origens.add(c);
                            return c;
                        });
        when(conteudos.buscarOrigens(any()))
                .thenAnswer(
                        inv ->
                                origens.stream()
                                        .filter(
                                                c ->
                                                        c.getUnidade()
                                                                .getId()
                                                                .equals(inv.getArgument(0)))
                                        .toList());
        when(estoque.disponiveis(any())).thenReturn(Set.of());
        when(acesso.usuario()).thenReturn("Cedro-ficticio");
        when(operacoes.findByPedidoIdAndOperacaoId(eq(26L), anyString()))
                .thenAnswer(inv -> Optional.ofNullable(operacao.get()));
        when(operacoes.saveAndFlush(any(OperacaoUnidade.class)))
                .thenAnswer(
                        inv -> {
                            operacao.set(inv.getArgument(0));
                            return operacao.get();
                        });
        var service =
                new UnidadeLogisticaService(
                        pedidos,
                        entradas,
                        unidades,
                        conteudos,
                        operacoes,
                        embalagens,
                        acesso,
                        mapper,
                        Clock.fixed(instante, ZoneOffset.UTC),
                        estoque);
        var nova =
                new UnidadeLogisticaDto.NovaUnidade(
                        25L,
                        TipoUnidadeLogistica.VOLUME,
                        CondicaoMercadoria.BOA,
                        new BigDecimal("2"));
        var operacaoId = UUID.fromString("00000000-0000-0000-0000-000000000031");
        var dados =
                new UnidadeLogisticaDto.Unitizar(
                        operacaoId, 0L, "Volume ficticio C2", List.of(nova, nova, nova));
        assertThat(entrada.getUnitizadaEm()).isNull();
        var resultado = service.unitizar(26L, 31L, dados);
        assertThat(resultado.operacaoId()).isEqualTo(operacaoId);
        assertThat(resultado.pedidoId()).isEqualTo(26);
        assertThat(resultado.unidades()).hasSize(3);
        assertThat(criadas).extracting(UnidadeLogistica::getCodigo).doesNotHaveDuplicates();
        for (var u : criadas) assertThat(UUID.fromString(u.getCodigo())).isNotNull();
        assertThat(
                        criadas.stream()
                                .map(UnidadeLogistica::getQuantidade)
                                .reduce(BigDecimal.ZERO, BigDecimal::add))
                .isEqualByComparingTo("6");
        for (int i = 0; i < 3; i++) {
            var detalhe = resultado.unidades().get(i);
            var resumo = detalhe.unidade();
            assertThat(resumo.id()).isEqualTo(201L + i);
            assertThat(resumo.clienteId()).isEqualTo(22);
            assertThat(resumo.armazemId()).isEqualTo(23);
            assertThat(resumo.pedidoId()).isEqualTo(26);
            assertThat(resumo.notaId()).isEqualTo(27);
            assertThat(resumo.serieNota()).isEqualTo(2);
            assertThat(resumo.numeroNota()).isEqualTo(71);
            assertThat(resumo.produtoId()).isEqualTo(24);
            assertThat(resumo.sku()).isEqualTo("SKU-VOL-C2");
            assertThat(resumo.unidadeMedida()).isEqualTo("UN");
            assertThat(resumo.controlaLote()).isFalse();
            assertThat(resumo.lote()).isNull();
            assertThat(resumo.validade()).isNull();
            assertThat(resumo.embalagemId()).isEqualTo(25);
            assertThat(resumo.codigoDun()).isEqualTo("DUN-VOL");
            assertThat(resumo.quantidadeProdutoPorDun()).isEqualByComparingTo("2");
            assertThat(resumo.tipo()).isEqualTo(TipoUnidadeLogistica.VOLUME);
            assertThat(resumo.condicao()).isEqualTo(CondicaoMercadoria.BOA);
            assertThat(resumo.quantidade()).isEqualByComparingTo("2");
            assertThat(resumo.dataFifo()).isEqualTo(fifo);
            assertThat(resumo.chegadaReal()).isEqualTo(chegada);
            assertThat(resumo.criadaEm()).isEqualTo(instante);
            assertThat(resumo.ativa()).isTrue();
            assertThat(resumo.disponivelParaSaida()).isFalse();
            assertThat(detalhe.origens())
                    .containsExactly(
                            new UnidadeLogisticaDto.Origem(31L, 30L, 28L, new BigDecimal("2")));
            var etiqueta = UnidadeLogisticaDto.Etiqueta.de(criadas.get(i));
            assertThat(etiqueta.tipo()).isEqualTo(TipoUnidadeLogistica.VOLUME);
            assertThat(etiqueta.sku()).isEqualTo("SKU-VOL-C2");
            assertThat(etiqueta.clienteId()).isEqualTo(22);
            assertThat(etiqueta.armazemId()).isEqualTo(23);
            assertThat(etiqueta.quantidadeProduto()).isEqualByComparingTo("2");
            assertThat(etiqueta.dataEntrada()).isEqualTo(fifo);
            assertThat(etiqueta.chegadaReal()).isEqualTo(chegada);
        }
        assertThat(entrada.getUnitizadaEm()).isEqualTo(instante);
        assertThat(
                        mapper.readTree(mapper.writeValueAsString(resultado))
                                .get("unidades")
                                .get(0)
                                .get("unidade")
                                .get("tipo")
                                .asString())
                .isEqualTo("VOLUME");
        assertThat(service.unitizar(26L, 31L, dados)).isEqualTo(resultado);
        verify(unidades, times(3)).save(any(UnidadeLogistica.class));
        verify(conteudos, times(3)).save(any(ConteudoUnidade.class));
        verify(operacoes).saveAndFlush(any(OperacaoUnidade.class));
        assertThat(UnidadeLogisticaDto.Resumo.de(anteriorC1, false)).isEqualTo(fotoC1);
        assertThat(anteriorC1.getQuantidade()).isEqualByComparingTo("10");
        assertThat(anteriorC1.getTipo()).isEqualTo(TipoUnidadeLogistica.PALLET);
    }
}
