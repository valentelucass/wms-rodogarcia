package br.com.rodogarcia.wms;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.rodogarcia.wms.exceptions.RegraNegocioException;
import br.com.rodogarcia.wms.services.NfeDocumentoService;
import br.com.rodogarcia.wms.services.NfeXmlService;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class NfeDocumentoServiceTest {
    private final NfeDocumentoService exportacoes =
            new NfeDocumentoService(
                    new NfeXmlService(),
                    new br.com.rodogarcia.wms.services.RegraBobinaPorItem(
                            "33064262000250:BOBINA-TESTE"));

    static String exportacao(String caso) throws Exception {
        try (var input =
                NfeDocumentoServiceTest.class.getResourceAsStream(
                        "/nfe/exportacao-" + caso + ".xml")) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @Test
    void exportacaoUmaBobinaPreservaDiferencaTributavel() throws Exception {
        var original = exportacao("uma");
        var doc = exportacoes.ler(original);
        assertThat(doc.formato()).isEqualTo("EXPORTACAO_OPERACIONAL");
        assertThat(doc.itens()).hasSize(1);
        assertThat(doc.itens().getFirst().bobinasPrevistas()).isEqualTo(1);
        assertThat(doc.itens().getFirst().quantidadeComercial()).isEqualByComparingTo("1.5870");
        assertThat(doc.itens().getFirst().quantidadeTributavel()).isEqualByComparingTo("1.5480");
        assertThat(doc.protocolo().ambiente()).isNull();
        assertThat(doc.avisos()).anyMatch(s -> s.contains("difere da tributável"));
        assertThat(original).contains("<nItem>1</nItem>").doesNotContain("nItem=\"1\"");
    }

    @Test
    void oitoLinhasMesmoSkuPermanecemSeparadasESemCorrecaoDeEscala() throws Exception {
        var doc = exportacoes.ler(exportacao("oito"));
        assertThat(doc.itens()).hasSize(8).allMatch(i -> i.bobinasPrevistas() == 1);
        assertThat(doc.itens().stream().map(i -> i.codigo()).distinct()).hasSize(1);
        assertThat(
                        doc.itens().stream()
                                .map(i -> i.quantidadeComercial())
                                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add))
                .isEqualByComparingTo("14.1720");
        assertThat(doc.volumes().getFirst().pesoLiquido()).isEqualByComparingTo("14.172");
        assertThat(doc.avisos()).anyMatch(s -> s.contains("14172 KG") && s.contains("14.172 KG"));
        assertThat(doc.avisos()).noneMatch(s -> s.contains("difere da tributável"));
    }

    @Test
    void naoGeneralizaRegraDeBobinasParaOutroClienteProdutoOuUnidade() throws Exception {
        assertThat(service.ler(exportacao("oito")).itens())
                .allMatch(i -> i.bobinasPrevistas() == null);
        var regra =
                new br.com.rodogarcia.wms.services.RegraBobinaPorItem(
                        "33064262000250:BOBINA-TESTE");
        assertThat(regra.aplica("33064262000250", "BOBINA-TESTE", "KG")).isTrue();
        assertThat(regra.aplica("00000000000000", "BOBINA-TESTE", "KG")).isFalse();
        assertThat(regra.aplica("33064262000250", "OUTRO", "KG")).isFalse();
        assertThat(regra.aplica("33064262000250", "BOBINA-TESTE", "UN")).isFalse();
    }

    @Test
    void exportacaoRecusaAmbiguidadeQuantidadesChaveEEntidades() throws Exception {
        var xml = exportacao("oito");
        for (var adulterado :
                java.util.List.of(
                        xml.replace("<qCom>1.8480</qCom>", "<qCom>1.8480</qCom><qCom>2.0</qCom>"),
                        xml.replace("<nItem>2</nItem>", "<nItem>1</nItem>"),
                        xml.replace("<qCom>1.8480</qCom>", "<qCom>-1.8480</qCom>"),
                        xml.replace("<qCom>1.8480</qCom>", "<qCom>1,8480</qCom>"),
                        xml.replace("<qCom>1.8480</qCom>", "<qCom>1E4</qCom>"),
                        xml.replace("<chNFe>4126", "<chNFe>4226"),
                        xml.replace("<vProd>80.00</vProd>", "<vProd>81.00</vProd>"),
                        xml.replace(
                                "<nfeProc ",
                                "<!DOCTYPE nfeProc [<!ENTITY externo SYSTEM 'file:///nao-existe'>]><nfeProc "),
                        xml.replace("<dest>", "<dest><xNome>Duplicado</xNome>"))) {
            assertThatThrownBy(() -> exportacoes.ler(adulterado))
                    .isInstanceOf(RegraNegocioException.class);
        }
    }

    @Test
    void formatoFiscalInvalidoNaoRecebeFallbackOperacional() throws Exception {
        var xml = exemplo().replace("<qCom>2400.0000</qCom>", "<qCom>2400.0000</qCom>");
        assertThatThrownBy(() -> exportacoes.ler(xml.replace("versao=\"4.00\"", "versao=\"3.10\"")))
                .isInstanceOf(RegraNegocioException.class);
    }

    private final NfeDocumentoService service = new NfeDocumentoService(new NfeXmlService());

    static String exemplo() throws Exception {
        try (var input =
                NfeDocumentoServiceTest.class.getResourceAsStream(
                        "/nfe/exemplo-reconstruido.xml")) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @Test
    void leDocumentoCompletoSemConverterVolumesEmQuantidade() throws Exception {
        var doc = service.ler(exemplo());
        assertThat(doc.emitente().documento()).isEqualTo("33064262000250");
        assertThat(doc.destinatario().documento()).isEqualTo("04547874000203");
        assertThat(doc.chaveAcesso()).isEqualTo("41260933064262000250550010002937291595471168");
        assertThat(doc.itens().getFirst().quantidadeComercial()).isEqualByComparingTo("32000");
        assertThat(doc.itens().getFirst().valorUnitario().toPlainString())
                .isEqualTo("0.7083000000");
        assertThat(doc.volumes().getFirst().quantidade()).isEqualByComparingTo("24");
        assertThat(doc.protocolo().codigoSituacao()).isEqualTo("100");
        assertThat(doc.emitidaEm()).isEqualTo("2026-09-01T10:40:20-03:00");
    }

    @Test
    void recusaDocumentoIncompletoEProtocoloDeOutraNota() throws Exception {
        var xml = exemplo();
        assertThatThrownBy(() -> service.ler(xml.replace("<mod>55</mod>", "<mod>65</mod>")))
                .isInstanceOf(RegraNegocioException.class);
        assertThatThrownBy(() -> service.ler(xml.replace("<chNFe>4126", "<chNFe>4226")))
                .isInstanceOf(RegraNegocioException.class);
        assertThatThrownBy(() -> service.ler(xml.replace("<dest>", "<destExtra>")))
                .isInstanceOf(RegraNegocioException.class);
    }

    @Test
    void suportaGruposIbsCbsDoEsquemaControlado() throws Exception {
        var xml =
                exemplo()
                        .replace(
                                "</imposto>",
                                "<IBSCBS><CST>410</CST><cClassTrib>410001</cClassTrib></IBSCBS></imposto>")
                        .replace(
                                "</ICMSTot>",
                                "</ICMSTot><IBSCBSTot><vBCIBSCBS>0.00</vBCIBSCBS></IBSCBSTot>");
        assertThat(service.ler(xml).itens()).hasSize(1);
    }

    @Test
    void aceitaMarcadorUtf8SemModificarOriginalDoChamador() throws Exception {
        var original = "\uFEFF" + exemplo();
        assertThat(service.ler(original).numero()).isEqualTo(293729);
        assertThat(original).startsWith("\uFEFF");
    }

    @Test
    void preservaCpfEZerosDaIdentificacaoNaChave() throws Exception {
        var xml =
                exemplo()
                        .replace("<CNPJ>33064262000250</CNPJ>", "<CPF>12345678901</CPF>")
                        .replace("33064262000250", "00012345678901");
        var doc = service.ler(xml);
        assertThat(doc.emitente().documento()).isEqualTo("12345678901");
        assertThat(doc.chaveAcesso()).hasSize(44).contains("00012345678901");
    }

    @Test
    void recusaDtdSemLerRecursoExterno() throws Exception {
        var xml =
                exemplo()
                        .replace(
                                "<nfeProc ",
                                "<!DOCTYPE nfeProc [<!ENTITY externo SYSTEM 'file:///nao-existe'>]><nfeProc ");
        assertThatThrownBy(() -> service.ler(xml)).isInstanceOf(RegraNegocioException.class);
    }
}
