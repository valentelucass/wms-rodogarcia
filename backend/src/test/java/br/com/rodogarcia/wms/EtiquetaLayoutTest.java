package br.com.rodogarcia.wms;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyList;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import br.com.rodogarcia.wms.dto.UnidadeLogisticaDto;
import br.com.rodogarcia.wms.models.ConteudoUnidade;
import br.com.rodogarcia.wms.models.OperacaoUnidade;
import br.com.rodogarcia.wms.models.UnidadeLogistica;
import br.com.rodogarcia.wms.repositories.ConteudoUnidadeRepository;
import br.com.rodogarcia.wms.repositories.ItemNotaEntradaRepository;
import br.com.rodogarcia.wms.repositories.OperacaoUnidadeRepository;
import br.com.rodogarcia.wms.repositories.UnidadeLogisticaRepository;
import br.com.rodogarcia.wms.repositories.UsuarioAcessoRepository;
import br.com.rodogarcia.wms.services.EtiquetaService;
import br.com.rodogarcia.wms.services.UnidadeLogisticaService;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import tools.jackson.databind.json.JsonMapper;

/** Enriquecimento em memória; não inicia aplicação nem comprova SQL/impressora. */
class EtiquetaLayoutTest {
    private final UnidadeLogisticaService acesso = mock(UnidadeLogisticaService.class);
    private final UnidadeLogisticaRepository unidades = mock(UnidadeLogisticaRepository.class);
    private final ConteudoUnidadeRepository conteudos = mock(ConteudoUnidadeRepository.class);
    private final ItemNotaEntradaRepository itens = mock(ItemNotaEntradaRepository.class);
    private final OperacaoUnidadeRepository operacoes = mock(OperacaoUnidadeRepository.class);
    private final UsuarioAcessoRepository usuarios = mock(UsuarioAcessoRepository.class);
    private final JsonMapper mapper = JsonMapper.builder().build();
    private final EtiquetaService service =
            new EtiquetaService(acesso, unidades, conteudos, itens, operacoes, usuarios, mapper);
    private final UUID codigo = UUID.fromString("00000000-0000-4000-8000-000000000001");

    @Test
    void exigeAlcanceAntesDeConsultarDadosAdicionais() {
        when(acesso.etiqueta(codigo)).thenThrow(new AccessDeniedException("Fora do alcance"));
        assertThatThrownBy(() -> service.consultar(codigo))
                .isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(unidades, conteudos, itens, operacoes, usuarios);
    }

    @Test
    void usaNotasDeOrigemSemDuplicarEAutorDaUnitizacaoEmVezDoUsuarioDaReimpressao() {
        var u = mock(UnidadeLogistica.class, RETURNS_DEEP_STUBS);
        when(u.getCodigo()).thenReturn(codigo.toString());
        when(u.getId()).thenReturn(5L);
        when(u.getPedido().getId()).thenReturn(10L);
        when(u.getProduto().getId()).thenReturn(11L);
        when(u.getProduto().getSku()).thenReturn("600366");
        when(u.getProduto().getDescricao()).thenReturn("Papel");
        when(u.getPedido().getCliente().getNome()).thenReturn("Cliente");
        when(u.getQuantidade()).thenReturn(BigDecimal.TEN);
        when(unidades.findByCodigo(codigo.toString())).thenReturn(Optional.of(u));
        var a = origem(20L, 123L);
        var b = origem(21L, 456L);
        when(conteudos.buscarOrigens(5L)).thenReturn(List.of(a, a, b));
        when(itens.quantidadeNasNotas(List.of(20L, 21L), 11L))
                .thenReturn(new BigDecimal("1820.000000"));
        when(unidades.countByPedidoIdAndAtivaTrue(10L)).thenReturn(10L);
        when(unidades.countByPedidoIdAndAtivaTrueAndIdLessThanEqual(10L, 5L)).thenReturn(2L);
        var op = mock(OperacaoUnidade.class);
        when(op.getResultado())
                .thenReturn("{\"unidades\":[{\"unidade\":{\"codigo\":\"" + codigo + "\"}}]}");
        when(op.getUsuario()).thenReturn("autor-historico");
        when(operacoes.findByPedidoIdAndTipoInOrderByRegistradaEmAscIdAsc(eq(10L), anyList()))
                .thenReturn(List.of(op));
        when(usuarios.buscarNome("autor-historico")).thenReturn(Optional.of("Roberto Carlos"));
        var etiqueta = service.consultar(codigo);
        assertThat(etiqueta.codigoLeitura()).isEqualTo(codigo.toString());
        assertThat(etiqueta.notas())
                .extracting(UnidadeLogisticaDto.NotaEtiqueta::numero)
                .containsExactly(123L, 456L);
        assertThat(etiqueta.quantidadeProdutoNasNotas()).isEqualByComparingTo("1820");
        assertThat(etiqueta.quantidadeProduto()).isEqualByComparingTo("10");
        assertThat(etiqueta.nomeConferente()).isEqualTo("Roberto Carlos");
        assertThat(etiqueta.numeroUnidade()).isEqualTo(2L);
        assertThat(etiqueta.totalUnidadesPedido()).isEqualTo(10L);
        verify(unidades, never()).save(any());
    }

    private ConteudoUnidade origem(Long id, long numero) {
        var c = mock(ConteudoUnidade.class, RETURNS_DEEP_STUBS);
        when(c.getEntrada().getItemChegada().getItemNota().getNota().getId()).thenReturn(id);
        when(c.getEntrada().getItemChegada().getItemNota().getNota().getNumero())
                .thenReturn(numero);
        return c;
    }
}
