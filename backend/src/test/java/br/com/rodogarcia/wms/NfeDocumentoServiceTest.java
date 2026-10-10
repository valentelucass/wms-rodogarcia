package br.com.rodogarcia.wms;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.rodogarcia.wms.exceptions.RegraNegocioException;
import br.com.rodogarcia.wms.services.NfeDocumentoService;
import br.com.rodogarcia.wms.services.NfeXmlService;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class NfeDocumentoServiceTest {
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
