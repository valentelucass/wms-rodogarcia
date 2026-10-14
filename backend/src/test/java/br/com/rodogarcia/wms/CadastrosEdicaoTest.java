package br.com.rodogarcia.wms;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import br.com.rodogarcia.wms.dto.ArmazemDto;
import br.com.rodogarcia.wms.dto.ClienteDto;
import br.com.rodogarcia.wms.dto.EmbalagemDto;
import br.com.rodogarcia.wms.dto.EnderecoDto;
import br.com.rodogarcia.wms.dto.ProdutoDto;
import br.com.rodogarcia.wms.models.Armazem;
import br.com.rodogarcia.wms.models.Cliente;
import br.com.rodogarcia.wms.models.Embalagem;
import br.com.rodogarcia.wms.models.Endereco;
import br.com.rodogarcia.wms.models.Produto;
import br.com.rodogarcia.wms.models.TipoEndereco;
import br.com.rodogarcia.wms.models.TipoQuantidade;
import br.com.rodogarcia.wms.models.TipoUnidadeLogistica;
import br.com.rodogarcia.wms.repositories.ArmazemRepository;
import br.com.rodogarcia.wms.repositories.ClienteRepository;
import br.com.rodogarcia.wms.repositories.EmbalagemRepository;
import br.com.rodogarcia.wms.repositories.EnderecoRepository;
import br.com.rodogarcia.wms.repositories.ProdutoRepository;
import br.com.rodogarcia.wms.services.AcessoService;
import br.com.rodogarcia.wms.services.ArmazemService;
import br.com.rodogarcia.wms.services.AuditoriaService;
import br.com.rodogarcia.wms.services.ClienteService;
import br.com.rodogarcia.wms.services.EmbalagemService;
import br.com.rodogarcia.wms.services.EnderecoService;
import br.com.rodogarcia.wms.services.ProdutoService;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

/** Regras e contratos em memória, sem Spring, contexto, conexão, H2 ou persistência. */
class CadastrosEdicaoTest {
    private final Instant agora = Instant.parse("2026-10-14T12:00:00Z");
    private final Clock clock = Clock.fixed(agora, ZoneOffset.UTC);
    private final AcessoService acesso = mock(AcessoService.class);
    private final AuditoriaService auditoria = mock(AuditoriaService.class);
    private final ClienteRepository clientes = mock(ClienteRepository.class);
    private final ProdutoRepository produtos = mock(ProdutoRepository.class);
    private final EmbalagemRepository embalagens = mock(EmbalagemRepository.class);
    private final ArmazemRepository armazens = mock(ArmazemRepository.class);
    private final EnderecoRepository enderecos = mock(EnderecoRepository.class);
    private final Cliente cliente = id(new Cliente("CLI-1", "Cliente", "123", agora), 1L);
    private final Armazem armazem =
            id(new Armazem("ARM-1", "Armazém", "123", "Bauru", "SP", agora), 2L);
    private final Produto produto =
            id(
                    new Produto(
                            cliente,
                            "ANTIGO",
                            "Papel",
                            "KG",
                            TipoQuantidade.MEDIDA,
                            3,
                            false,
                            false,
                            null,
                            agora),
                    3L);
    private final Embalagem embalagem =
            id(new Embalagem(produto, "DUN-1", "Bobina", BigDecimal.ONE, agora), 4L);
    private final Endereco endereco =
            id(
                    new Endereco(
                            armazem,
                            "A-1",
                            "A",
                            1,
                            "1",
                            "Posição",
                            TipoEndereco.ARMAZENAGEM,
                            null,
                            null,
                            null,
                            null,
                            null,
                            1,
                            agora),
                    5L);

    private static <T> T id(T entidade, Long id) {
        ReflectionTestUtils.setField(entidade, "id", id);
        return entidade;
    }

    private ClienteService clienteService() {
        return new ClienteService(clientes, acesso, auditoria, clock);
    }

    private ProdutoService produtoService() {
        return new ProdutoService(produtos, acesso, auditoria, clock, clientes);
    }

    private EmbalagemService embalagemService() {
        return new EmbalagemService(embalagens, acesso, auditoria, clock, produtos, clientes);
    }

    private EnderecoService enderecoService() {
        return new EnderecoService(enderecos, acesso, auditoria, clock, armazens);
    }

    @Test
    void corrigeDocumentoComNormalizacaoEAuditoriaAntesDepois() {
        when(clientes.buscarParaAtualizar(1L)).thenReturn(Optional.of(cliente));
        var depois =
                clienteService()
                        .alterar(
                                1L,
                                new ClienteDto.Alterar(
                                        0L,
                                        "Cliente corrigido",
                                        "Correção cadastral",
                                        "12.345.678/0001-90"));
        assertThat(depois.documentoFiscal()).isEqualTo("12345678000190");
        assertThat(depois.codigo()).isEqualTo("CLI-1");
        verify(auditoria)
                .registrar(
                        eq("CLIENTE"),
                        eq(1L),
                        eq("ALTERACAO"),
                        eq("Correção cadastral"),
                        argThat(a -> ((ClienteDto.Resposta) a).documentoFiscal().equals("123")),
                        eq(depois));
    }

    @Test
    void corrigeArmazemCompleto() {
        when(armazens.buscarParaAtualizar(2L)).thenReturn(Optional.of(armazem));
        var depois =
                new ArmazemService(armazens, acesso, auditoria, clock)
                        .alterar(
                                2L,
                                new ArmazemDto.Alterar(
                                        0L,
                                        "Novo",
                                        "Correção cadastral",
                                        "987/65",
                                        " Marília ",
                                        "sp"));
        assertThat(depois.documentoFiscal()).isEqualTo("98765");
        assertThat(depois.cidade()).isEqualTo("Marília");
        assertThat(depois.uf()).isEqualTo("SP");
    }

    @Test
    void alteraSkuMesmoComDependenciasSemTrocarProprietarioOuMedida() {
        when(produtos.buscarParaAtualizar(3L)).thenReturn(Optional.of(produto));
        var depois =
                produtoService()
                        .alterar(
                                3L,
                                new ProdutoDto.Alterar(
                                        0L,
                                        "Papel",
                                        "Correção de SKU",
                                        "novo-123",
                                        null,
                                        null,
                                        null,
                                        null,
                                        null,
                                        null));
        assertThat(depois.sku()).isEqualTo("NOVO-123");
        assertThat(depois.clienteId()).isEqualTo(1L);
        assertThat(depois.unidadeMedida()).isEqualTo("KG");
        verify(produtos, never()).contarDependencias(anyLong());
    }

    @Test
    void bloqueiaReinterpretacaoDeProdutoEmUsoAntesDeMutar() {
        when(produtos.buscarParaAtualizar(3L)).thenReturn(Optional.of(produto));
        when(produtos.contarDependencias(3L)).thenReturn(1L);
        assertThatThrownBy(
                        () ->
                                produtoService()
                                        .alterar(
                                                3L,
                                                new ProdutoDto.Alterar(
                                                        0L,
                                                        "Outro",
                                                        "Correção cadastral",
                                                        "NOVO",
                                                        "UN",
                                                        TipoQuantidade.CONTAGEM,
                                                        0,
                                                        false,
                                                        false,
                                                        null)))
                .hasMessageContaining("reinterpretar");
        assertThat(produto.getSku()).isEqualTo("ANTIGO");
        verifyNoInteractions(auditoria);
        verify(produtos, never()).flush();
    }

    @Test
    void corrigeConfiguracaoAntesDoUsoEValidaContagem() {
        when(produtos.buscarParaAtualizar(3L)).thenReturn(Optional.of(produto));
        assertThatThrownBy(
                        () ->
                                produtoService()
                                        .alterar(
                                                3L,
                                                new ProdutoDto.Alterar(
                                                        0L,
                                                        "Papel",
                                                        "Correção cadastral",
                                                        null,
                                                        "UN",
                                                        TipoQuantidade.CONTAGEM,
                                                        2,
                                                        null,
                                                        null,
                                                        null)))
                .hasMessageContaining("precisão zero");
        var depois =
                produtoService()
                        .alterar(
                                3L,
                                new ProdutoDto.Alterar(
                                        0L,
                                        "Papel",
                                        "Correção cadastral",
                                        null,
                                        "UN",
                                        TipoQuantidade.CONTAGEM,
                                        0,
                                        false,
                                        true,
                                        30));
        assertThat(depois.controlaValidade()).isTrue();
        assertThat(depois.antecedenciaAvisoDias()).isEqualTo(30);
        depois =
                produtoService()
                        .alterar(
                                3L,
                                new ProdutoDto.Alterar(
                                        0L,
                                        "Papel",
                                        "Correção cadastral",
                                        null,
                                        null,
                                        null,
                                        null,
                                        null,
                                        false,
                                        null));
        assertThat(depois.antecedenciaAvisoDias()).isNull();
    }

    @Test
    void dunPodeMudarMasConversaoEmUsoNao() {
        when(embalagens.buscarParaAtualizar(4L)).thenReturn(Optional.of(embalagem));
        when(embalagens.contarUnidades(4L)).thenReturn(1L);
        var depois =
                embalagemService()
                        .alterar(
                                4L,
                                new EmbalagemDto.Alterar(
                                        0L,
                                        "Bobina",
                                        "Correção de DUN",
                                        "dun-2",
                                        new BigDecimal("1.000000")));
        assertThat(depois.codigoDun()).isEqualTo("DUN-2");
        assertThatThrownBy(
                        () ->
                                embalagemService()
                                        .alterar(
                                                4L,
                                                new EmbalagemDto.Alterar(
                                                        0L,
                                                        "Bobina",
                                                        "Correção cadastral",
                                                        "DUN-3",
                                                        BigDecimal.TEN)))
                .hasMessageContaining("quantidade por DUN");
        assertThat(embalagem.getCodigoDun()).isEqualTo("DUN-2");
    }

    @Test
    void editaPosicaoLivreEBloqueiaEstruturaOcupada() {
        when(enderecos.buscarArmazemId(5L)).thenReturn(Optional.of(2L));
        when(armazens.buscarParaAtualizar(2L)).thenReturn(Optional.of(armazem));
        when(enderecos.buscarParaAtualizar(5L)).thenReturn(Optional.of(endereco));
        var dados =
                new EnderecoDto.Alterar(
                        0L,
                        "Nova posição",
                        "Correção cadastral",
                        "b-2",
                        "b",
                        2,
                        "2",
                        TipoEndereco.ARMAZENAGEM,
                        BigDecimal.TEN,
                        null,
                        null,
                        null,
                        2,
                        7);
        var depois = enderecoService().alterar(5L, dados);
        assertThat(depois.codigo()).isEqualTo("B-2");
        assertThat(depois.rua()).isEqualTo("B");
        assertThat(depois.nivel()).isEqualTo(2);
        assertThat(depois.sequenciaColeta()).isEqualTo(7);
        when(enderecos.contarUsoAtual(5L)).thenReturn(1L);
        var proibido =
                new EnderecoDto.Alterar(
                        0L,
                        "Posição",
                        "Correção cadastral",
                        null,
                        "C",
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null);
        assertThatThrownBy(() -> enderecoService().alterar(5L, proibido))
                .hasMessageContaining("Desocupe");
        assertThat(endereco.getRua()).isEqualTo("B");
        var ordem = inOrder(armazens, enderecos);
        ordem.verify(enderecos).buscarArmazemId(5L);
        ordem.verify(armazens).buscarParaAtualizar(2L);
        ordem.verify(enderecos).buscarParaAtualizar(5L);
    }

    @Test
    void recusaVersaoAntigaAntesDeMutar() {
        when(clientes.buscarParaAtualizar(1L)).thenReturn(Optional.of(cliente));
        assertThatThrownBy(
                        () ->
                                clienteService()
                                        .alterar(
                                                1L,
                                                new ClienteDto.Alterar(
                                                        9L, "Novo", "Correção cadastral", "999")))
                .isInstanceOf(br.com.rodogarcia.wms.exceptions.RegraNegocioException.class);
        assertThat(cliente.getDocumentoFiscal()).isEqualTo("123");
        verifyNoInteractions(auditoria);
    }

    @Test
    void cadastraPosicaoComTipoSemExigirComandoSeparadoDeCapacidade() {
        when(armazens.buscarParaAtualizar(2L)).thenReturn(Optional.of(armazem));
        when(enderecos.saveAndFlush(org.mockito.ArgumentMatchers.any(Endereco.class)))
                .thenAnswer(i -> id(i.getArgument(0), 6L));
        var resposta =
                enderecoService()
                        .criar(
                                new EnderecoDto.Criar(
                                        2L,
                                        "B-1",
                                        "B",
                                        0,
                                        "1",
                                        "Bobinas",
                                        TipoEndereco.ARMAZENAGEM,
                                        new BigDecimal("2000"),
                                        new BigDecimal("2"),
                                        new BigDecimal("2"),
                                        new BigDecimal("2"),
                                        1,
                                        1,
                                        TipoUnidadeLogistica.BOBINA));
        assertThat(resposta.tipoUnidadePermitido()).isEqualTo(TipoUnidadeLogistica.BOBINA);
        assertThat(resposta.capacidadePesoKg()).isEqualByComparingTo("2000");
        verify(auditoria)
                .registrar(
                        eq("ENDERECO"),
                        eq(6L),
                        eq("CRIACAO"),
                        eq("Cadastro inicial"),
                        eq(null),
                        eq(resposta));
    }

    @Test
    void completaTipoDaPosicaoExistenteEPreservaEmComandoLegado() {
        when(enderecos.buscarArmazemId(5L)).thenReturn(Optional.of(2L));
        when(armazens.buscarParaAtualizar(2L)).thenReturn(Optional.of(armazem));
        when(enderecos.buscarParaAtualizar(5L)).thenReturn(Optional.of(endereco));
        endereco.configurarFisico(
                null,
                new BigDecimal("2000"),
                BigDecimal.TEN,
                BigDecimal.TEN,
                BigDecimal.TEN,
                1,
                agora);
        var resposta = enderecoService().alterar(5L, tipoPosicao(TipoUnidadeLogistica.BOBINA, 0L));
        assertThat(resposta.tipoUnidadePermitido()).isEqualTo(TipoUnidadeLogistica.BOBINA);
        assertThat(resposta.capacidadePesoKg()).isEqualByComparingTo("2000");
        var legado =
                enderecoService()
                        .alterar(
                                5L,
                                new EnderecoDto.Alterar(
                                        0L, "Descrição corrigida", "Correção cadastral"));
        assertThat(legado.tipoUnidadePermitido()).isEqualTo(TipoUnidadeLogistica.BOBINA);
        assertThat(legado.alturaMetros()).isEqualByComparingTo(BigDecimal.TEN);
    }

    @Test
    void recusaTrocaDeTipoEmPosicaoUsadaEComRevisaoAntiga() {
        when(enderecos.buscarArmazemId(5L)).thenReturn(Optional.of(2L));
        when(armazens.buscarParaAtualizar(2L)).thenReturn(Optional.of(armazem));
        when(enderecos.buscarParaAtualizar(5L)).thenReturn(Optional.of(endereco));
        endereco.alterarTipoUnidadePermitido(TipoUnidadeLogistica.BOBINA, agora);
        when(enderecos.contarUsoAtual(5L)).thenReturn(1L);
        assertThatThrownBy(
                        () ->
                                enderecoService()
                                        .alterar(5L, tipoPosicao(TipoUnidadeLogistica.PALLET, 0L)))
                .hasMessageContaining("Desocupe");
        assertThatThrownBy(
                        () ->
                                enderecoService()
                                        .alterar(5L, tipoPosicao(TipoUnidadeLogistica.PALLET, 9L)))
                .isInstanceOf(br.com.rodogarcia.wms.exceptions.RegraNegocioException.class);
        assertThat(endereco.getTipoUnidadePermitido()).isEqualTo(TipoUnidadeLogistica.BOBINA);
        verifyNoInteractions(auditoria);
    }

    private EnderecoDto.Alterar tipoPosicao(TipoUnidadeLogistica tipo, Long versao) {
        return new EnderecoDto.Alterar(
                versao,
                "Posição",
                "Correção cadastral",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                tipo);
    }

    @ParameterizedTest
    @ValueSource(strings = {"OPERACAO", "SUPERVISOR"})
    void exigeGestorAntesDeQualquerConsulta(String perfil) {
        doThrow(new AccessDeniedException(perfil)).when(acesso).exigirGestor();
        assertThatThrownBy(
                        () ->
                                clienteService()
                                        .alterar(
                                                1L,
                                                new ClienteDto.Alterar(
                                                        0L, "Cliente", "Correção cadastral")))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(
                        () ->
                                new ArmazemService(armazens, acesso, auditoria, clock)
                                        .alterar(
                                                2L,
                                                new ArmazemDto.Alterar(
                                                        0L, "Armazém", "Correção cadastral")))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(
                        () ->
                                produtoService()
                                        .alterar(
                                                3L,
                                                new ProdutoDto.Alterar(
                                                        0L, "Produto", "Correção cadastral")))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(
                        () ->
                                embalagemService()
                                        .alterar(
                                                4L,
                                                new EmbalagemDto.Alterar(
                                                        0L, "DUN", "Correção cadastral")))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(
                        () ->
                                enderecoService()
                                        .alterar(
                                                5L,
                                                new EnderecoDto.Alterar(
                                                        0L, "Posição", "Correção cadastral")))
                .isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(clientes, armazens, produtos, embalagens, enderecos, auditoria);
    }
}
