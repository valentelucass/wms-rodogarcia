package br.com.rodogarcia.wms;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.rodogarcia.wms.exceptions.RegraNegocioException;
import br.com.rodogarcia.wms.services.NfeXmlService;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

/** Doc24:101. Fronteiras documentais medidas antes do parser publico; sem SQL/HTTP fiscal. */
class D29XmlFronteirasTest {
    String xml(int lines, String extra) {
        var body = new StringBuilder();
        for (int n = 1; n <= lines; n++)
            body.append("<det nItem='")
                    .append(n)
                    .append(
                            "'><prod><cProd>D29LOCAL</cProd><uCom>UN</uCom><qCom>1.000001</qCom><vProd>10</vProd></prod></det>");
        return "<NFe xmlns='http://www.portalfiscal.inf.br/nfe'><infNFe versao='4.00' Id='NFe"
                + "9".repeat(44)
                + "'><ide><mod>55</mod><serie>1</serie><nNF>291</nNF><dhEmi>2026-10-07T12:00:00-03:00</dhEmi></ide><emit><CNPJ>12345678000199</CNPJ></emit>"
                + body
                + extra
                + "</infNFe></NFe>";
    }

    Element wellFormed(String text) throws Exception {
        var f = DocumentBuilderFactory.newDefaultInstance();
        f.setNamespaceAware(true);
        f.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        return f.newDocumentBuilder()
                .parse(new InputSource(new StringReader(text)))
                .getDocumentElement();
    }

    int depth(Node node) {
        int max = 0;
        for (Node c = node.getFirstChild(); c != null; c = c.getNextSibling())
            if (c instanceof Element) max = Math.max(max, depth(c));
        return 1 + max;
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void acceptsExactlyOneMillionUtf8BytesIncludingMultibyte(boolean multibyte) throws Exception {
        String base = xml(2, "");
        int padding =
                1000000 - base.getBytes(StandardCharsets.UTF_8).length - "<extra></extra>".length();
        String text =
                multibyte ? "é".repeat(padding / 2) + "x".repeat(padding % 2) : "x".repeat(padding);
        String document = xml(2, "<extra>" + text + "</extra>");
        assertThat(document.getBytes(StandardCharsets.UTF_8)).hasSize(1000000);
        assertThat(
                        wellFormed(document)
                                .getElementsByTagNameNS(
                                        "http://www.portalfiscal.inf.br/nfe", "det"))
                .satisfies(v -> assertThat(v.getLength()).isEqualTo(2));
        assertThat(new NfeXmlService().ler(document).itens()).hasSize(2);
        if (multibyte) assertThat(document.length()).isLessThan(1000000);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void rejectsOneByteOverLimitEvenWhenCharacterCountBelowLimit(boolean multibyte)
            throws Exception {
        String base = xml(2, "");
        int padding =
                1000001 - base.getBytes(StandardCharsets.UTF_8).length - "<extra></extra>".length();
        String text =
                multibyte ? "é".repeat(padding / 2) + "x".repeat(padding % 2) : "x".repeat(padding);
        String document = xml(2, "<extra>" + text + "</extra>");
        assertThat(document.getBytes(StandardCharsets.UTF_8)).hasSize(1000001);
        assertThat(wellFormed(document)).isNotNull();
        if (multibyte) assertThat(document.length()).isLessThan(1000000);
        assertThatThrownBy(() -> new NfeXmlService().ler(document))
                .isInstanceOf(RegraNegocioException.class);
    }

    @ParameterizedTest
    @ValueSource(ints = {64, 65})
    void documentedDepthBoundaryOnWellFormedNfe(int levels) throws Exception {
        String document =
                xml(2, "<extra>".repeat(levels - 2) + "ok" + "</extra>".repeat(levels - 2));
        assertThat(depth(wellFormed(document))).isEqualTo(levels);
        if (levels == 64) assertThat(new NfeXmlService().ler(document).itens()).hasSize(2);
        else
            assertThatThrownBy(() -> new NfeXmlService().ler(document))
                    .isInstanceOf(RegraNegocioException.class);
    }

    @ParameterizedTest
    @ValueSource(ints = {200, 201})
    void documentedLineBoundaryKeepsAllOriginalRows(int lines) throws Exception {
        String document = xml(lines, "");
        assertThat(
                        wellFormed(document)
                                .getElementsByTagNameNS("http://www.portalfiscal.inf.br/nfe", "det")
                                .getLength())
                .isEqualTo(lines);
        if (lines == 200) {
            var result = new NfeXmlService().ler(document);
            assertThat(result.itens()).hasSize(200);
            assertThat(result.itens().getFirst().numeroItem()).isEqualTo(1);
            assertThat(result.itens().getLast().numeroItem()).isEqualTo(200);
        } else
            assertThatThrownBy(() -> new NfeXmlService().ler(document))
                    .isInstanceOf(RegraNegocioException.class);
    }

    @Test
    void nfeProcWrapperKeepsSourceAndDecimalSixLiteral() throws Exception {
        String document =
                "<nfeProc xmlns='http://www.portalfiscal.inf.br/nfe' versao='4.00'>"
                        + xml(2, "")
                        + "</nfeProc>";
        assertThat(depth(wellFormed(document))).isEqualTo(6);
        var result = new NfeXmlService().ler(document);
        assertThat(result.numero()).isEqualTo(291);
        assertThat(result.itens()).hasSize(2);
        assertThat(result.itens().getFirst().quantidade()).isEqualByComparingTo("1.000001");
        assertThat(
                        result.itens().stream()
                                .map(v -> v.quantidade())
                                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add))
                .isEqualByComparingTo("2.000002");
    }
}
