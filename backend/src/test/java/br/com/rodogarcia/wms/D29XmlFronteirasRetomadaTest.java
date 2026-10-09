package br.com.rodogarcia.wms;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.rodogarcia.wms.exceptions.RegraNegocioException;
import br.com.rodogarcia.wms.services.NfeXmlService;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

/** Fronteiras locais do parser publico; nao autentica NF-e nem comprova API/SQL Server. */
class D29XmlFronteirasRetomadaTest {
    private final NfeXmlService service = new NfeXmlService();

    @Test
    void profundidadeExata64AceitaE65RecusaNfeBemFormada() throws Exception {
        String aceita =
                xml(1).replace(
                                "</infNFe>",
                                "<x>".repeat(62) + "valor" + "</x>".repeat(62) + "</infNFe>");
        String recusa =
                xml(1).replace(
                                "</infNFe>",
                                "<x>".repeat(63) + "valor" + "</x>".repeat(63) + "</infNFe>");
        assertThat(profundidade(documento(aceita), 1)).isEqualTo(64);
        assertThat(profundidade(documento(recusa), 1)).isEqualTo(65);
        assertThat(service.ler(aceita).itens()).hasSize(1);
        assertThatThrownBy(() -> service.ler(recusa)).isInstanceOf(RegraNegocioException.class);
    }

    @Test
    void duzentosItensAceitaEDuzentosEUmRecusaComItensValidos() throws Exception {
        String aceita = xml(200);
        String recusa = xml(201);
        assertThat(
                        documento(aceita)
                                .getElementsByTagNameNS("http://www.portalfiscal.inf.br/nfe", "det")
                                .getLength())
                .isEqualTo(200);
        assertThat(
                        documento(recusa)
                                .getElementsByTagNameNS("http://www.portalfiscal.inf.br/nfe", "det")
                                .getLength())
                .isEqualTo(201);
        var itens = service.ler(aceita).itens();
        assertThat(itens).hasSize(200);
        assertThat(itens.getFirst().numeroItem()).isEqualTo(1);
        assertThat(itens.getLast().numeroItem()).isEqualTo(200);
        assertThat(itens.getLast().quantidade()).isEqualByComparingTo("1");
        assertThatThrownBy(() -> service.ler(recusa)).isInstanceOf(RegraNegocioException.class);
    }

    @Test
    void milhaoDeBytesUtf8AceitaEMaisUmRecusaMesmoComMenosCaracteres() throws Exception {
        String aceita = tamanhoUtf8(1_000_000);
        String recusa = tamanhoUtf8(1_000_001);
        assertThat(aceita.getBytes(StandardCharsets.UTF_8)).hasSize(1_000_000);
        assertThat(recusa.getBytes(StandardCharsets.UTF_8)).hasSize(1_000_001);
        assertThat(aceita.length()).isLessThan(1_000_000);
        assertThat(recusa.length()).isLessThan(1_000_000);
        assertThat(documento(aceita).getLocalName()).isEqualTo("NFe");
        assertThat(documento(recusa).getLocalName()).isEqualTo("NFe");
        var esperado = service.ler(xml(1));
        assertThat(service.ler(aceita)).isEqualTo(esperado);
        assertThatThrownBy(() -> service.ler(recusa)).isInstanceOf(RegraNegocioException.class);
    }

    private String tamanhoUtf8(int bytes) {
        String base = xml(1).replace("</infNFe>", "<!--PADDING--></infNFe>");
        int restantes = bytes - base.replace("PADDING", "").getBytes(StandardCharsets.UTF_8).length;
        return base.replace("PADDING", "á".repeat(restantes / 2) + "x".repeat(restantes % 2));
    }

    private Element documento(String xml) throws Exception {
        var factory = DocumentBuilderFactory.newDefaultInstance();
        factory.setNamespaceAware(true);
        return factory.newDocumentBuilder()
                .parse(new InputSource(new StringReader(xml)))
                .getDocumentElement();
    }

    private int profundidade(Element elemento, int atual) {
        int maxima = atual;
        for (Node n = elemento.getFirstChild(); n != null; n = n.getNextSibling()) {
            if (n instanceof Element filho)
                maxima = Math.max(maxima, profundidade(filho, atual + 1));
        }
        return maxima;
    }

    private String xml(int itens) {
        StringBuilder detalhes = new StringBuilder();
        for (int i = 1; i <= itens; i++)
            detalhes.append("<det nItem='")
                    .append(i)
                    .append("'><prod><cProd>SKU")
                    .append(i)
                    .append(
                            "</cProd><uCom>UN</uCom><qCom>1</qCom><vProd>2.00</vProd></prod></det>");
        return "<NFe xmlns='http://www.portalfiscal.inf.br/nfe'><infNFe Id='NFe"
                + "1".repeat(44)
                + "' versao='4.00'><ide><mod>55</mod><serie>1</serie><nNF>123</nNF>"
                + "<dhEmi>2026-09-01T08:00:00-03:00</dhEmi></ide><emit><CNPJ>12345678000199</CNPJ></emit>"
                + detalhes
                + "</infNFe></NFe>";
    }
}
