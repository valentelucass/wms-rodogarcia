package br.com.rodogarcia.wms;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.rodogarcia.wms.dto.ArmazemDto;
import br.com.rodogarcia.wms.dto.ClienteDto;
import br.com.rodogarcia.wms.dto.EmbalagemDto;
import br.com.rodogarcia.wms.dto.EnderecoDto;
import br.com.rodogarcia.wms.dto.FiscalCadastroDto;
import br.com.rodogarcia.wms.dto.ProdutoDto;
import br.com.rodogarcia.wms.models.Armazem;
import br.com.rodogarcia.wms.models.CadastroBase;
import br.com.rodogarcia.wms.models.Cliente;
import br.com.rodogarcia.wms.models.ConjuntoPosicoes;
import br.com.rodogarcia.wms.models.Embalagem;
import br.com.rodogarcia.wms.models.Endereco;
import br.com.rodogarcia.wms.models.Produto;
import br.com.rodogarcia.wms.models.TipoEndereco;
import br.com.rodogarcia.wms.models.TipoQuantidade;
import br.com.rodogarcia.wms.models.TipoUnidadeLogistica;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Provas dos construtores/fábricas reais; não substituem persistência ou callers de negócio. */
class D30CadastrosTransformacoesTest {
    private static final Instant CRIACAO = Instant.parse("2026-09-01T10:11:12.123456Z");
    private static final Instant ALTERACAO = Instant.parse("2026-09-02T13:14:15.654321Z");

    private record Familia(
            Cliente cliente,
            Armazem armazem,
            Produto produto,
            Embalagem embalagem,
            Endereco endereco,
            ConjuntoPosicoes conjunto) {
        List<CadastroBase> cadastros() {
            return List.of(cliente, armazem, produto, embalagem, endereco, conjunto);
        }
    }

    private Familia familia() {
        var cliente = new Cliente("C-D30-TEMPO", "Cliente fictício", "11111111000111", CRIACAO);
        var armazem =
                new Armazem(
                        "A-D30-TEMPO",
                        "Armazém fictício",
                        "22222222000122",
                        "Cidade",
                        "SP",
                        CRIACAO);
        var produto =
                new Produto(
                        cliente,
                        "SKU-TEMPO",
                        "Produto fictício",
                        "UN",
                        TipoQuantidade.CONTAGEM,
                        0,
                        false,
                        false,
                        null,
                        CRIACAO);
        var embalagem =
                new Embalagem(
                        produto, "DUN-TEMPO", "Embalagem fictícia", new BigDecimal("2"), CRIACAO);
        var a =
                new Endereco(
                        armazem,
                        "A",
                        "R",
                        0,
                        "01",
                        "Endereço A",
                        TipoEndereco.ARMAZENAGEM,
                        new BigDecimal("100"),
                        BigDecimal.ONE,
                        BigDecimal.ONE,
                        BigDecimal.ONE,
                        1,
                        1,
                        CRIACAO);
        var b =
                new Endereco(
                        armazem,
                        "B",
                        "R",
                        0,
                        "02",
                        "Endereço B",
                        TipoEndereco.ARMAZENAGEM,
                        new BigDecimal("100"),
                        BigDecimal.ONE,
                        BigDecimal.ONE,
                        BigDecimal.ONE,
                        1,
                        2,
                        CRIACAO);
        var conjunto =
                new ConjuntoPosicoes(
                        armazem,
                        "AB",
                        a,
                        b,
                        new BigDecimal("200"),
                        BigDecimal.ONE,
                        new BigDecimal("2"),
                        BigDecimal.ONE,
                        1,
                        CRIACAO);
        return new Familia(cliente, armazem, produto, embalagem, a, conjunto);
    }

    @Test
    void seisConstrutoresECincoRespostasConservamInstanteIndependente() {
        var f = familia();
        for (var cadastro : f.cadastros()) {
            assertThat(cadastro.getCriadoEm()).isEqualTo(CRIACAO);
            assertThat(cadastro.getAlteradoEm()).isEqualTo(CRIACAO);
        }
        conferirRespostas(f);
    }

    @Test
    void alteracoesReaisConservamCriacaoEContextoSemTrocarIdentidade() {
        var f = familia();
        f.cliente().alterarDescricao("Cliente alterado", ALTERACAO);
        f.armazem().alterarDescricao("Armazém alterado", ALTERACAO);
        f.produto().alterarDescricao("Produto alterado", ALTERACAO);
        f.embalagem().alterarDescricao("Embalagem alterada", ALTERACAO);
        f.endereco().alterarDescricao("Endereço alterado", ALTERACAO);
        f.cliente().complementar(null, "Cidade C", "MG", "c@example.invalid", "REF-C", ALTERACAO);
        f.armazem().complementar(null, "Cidade A", "RJ", null, null, ALTERACAO);
        for (var cadastro :
                List.of(f.cliente(), f.armazem(), f.produto(), f.embalagem(), f.endereco())) {
            assertThat(cadastro.getAlteradoEm()).isEqualTo(ALTERACAO);
        }
        f.endereco()
                .configurarFisico(
                        TipoUnidadeLogistica.PALLET,
                        new BigDecimal("150"),
                        new BigDecimal("2"),
                        new BigDecimal("3"),
                        new BigDecimal("4"),
                        2,
                        ALTERACAO);
        for (var cadastro :
                List.of(f.cliente(), f.armazem(), f.produto(), f.embalagem(), f.endereco())) {
            assertThat(cadastro.getCriadoEm()).isEqualTo(CRIACAO);
            assertThat(cadastro.getId()).isNull();
            assertThat(cadastro.getVersao()).isZero();
        }
        for (var cadastro : List.of(f.cliente(), f.armazem(), f.produto(), f.embalagem())) {
            assertThat(cadastro.getAlteradoEm()).isEqualTo(ALTERACAO);
        }
        assertThat(f.endereco().getAlteradoEm())
                .isEqualTo(Instant.parse("2026-09-02T13:14:15.654322Z"));
        assertThat(f.cliente().getCodigo()).isEqualTo("C-D30-TEMPO");
        assertThat(f.armazem().getCodigo()).isEqualTo("A-D30-TEMPO");
        assertThat(f.produto().getCliente()).isSameAs(f.cliente());
        assertThat(f.embalagem().getProduto()).isSameAs(f.produto());
        assertThat(f.embalagem().getQuantidadeProduto()).isEqualByComparingTo("2");
        assertThat(f.endereco().getArmazem()).isSameAs(f.armazem());
        assertThat(f.conjunto().getCriadoEm()).isEqualTo(CRIACAO);
        assertThat(f.conjunto().getAlteradoEm()).isEqualTo(CRIACAO);
        conferirRespostas(f);
    }

    private void conferirRespostas(Familia f) {
        assertThat(ClienteDto.Resposta.de(f.cliente()).criadoEm()).isEqualTo(CRIACAO);
        assertThat(ArmazemDto.Resposta.de(f.armazem()).criadoEm()).isEqualTo(CRIACAO);
        assertThat(ProdutoDto.Resposta.de(f.produto()).criadoEm()).isEqualTo(CRIACAO);
        assertThat(EmbalagemDto.Resposta.de(f.embalagem()).criadoEm()).isEqualTo(CRIACAO);
        assertThat(EnderecoDto.Resposta.de(f.endereco()).criadoEm()).isEqualTo(CRIACAO);
    }

    @Test
    void fabricaFiscalNulaPreservaQuatroParametrosSemInventarOnzeStrings() {
        var esperado =
                new FiscalCadastroDto.Dados(
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        "Cidade independente",
                        "SP",
                        null,
                        null,
                        null,
                        null,
                        null,
                        "fiscal@example.invalid",
                        "REF-INDEPENDENTE");
        var atual =
                FiscalCadastroDto.Dados.de(
                        null,
                        "Cidade independente",
                        "SP",
                        "fiscal@example.invalid",
                        "REF-INDEPENDENTE");
        assertThat(atual).isEqualTo(esperado);
        assertThat(atual.modelo())
                .extracting(
                        "razaoSocial",
                        "inscricaoEstadual",
                        "logradouro",
                        "numeroEndereco",
                        "complemento",
                        "bairro",
                        "cep",
                        "pais",
                        "contatoNome",
                        "contatoEmail",
                        "contatoTelefone")
                .containsExactly(null, null, null, null, null, null, null, null, null, null, null);
    }
}
