package br.com.rodogarcia.wms;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.rodogarcia.wms.exceptions.RegraNegocioException;
import br.com.rodogarcia.wms.services.NfeXmlService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Fronteira publica do parser: namespace independente de prefixo, sem SQL/HTTP/acesso externo. */
class D29XmlSeguroTest {
    String xml(String extra) {
        return "<NFe xmlns='http://www.portalfiscal.inf.br/nfe'><infNFe versao='4.00' Id='NFe"
                + "1".repeat(44)
                + "'>"
                + extra
                + "<ide><mod>55</mod><serie>1</serie><nNF>123</nNF><dhEmi>2026-10-07T12:00:00-03:00</dhEmi></ide><emit><CNPJ>12345678000199</CNPJ></emit><det nItem='1'><prod><cProd>D29FICTICIO</cProd><uCom>UN</uCom><qCom>2E0</qCom><vProd>10</vProd></prod></det></infNFe></NFe>";
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "<xi:include xmlns:xi='http://www.w3.org/2001/XInclude' href='file:///d29-nao-abrir'/>",
                "<outro:include xmlns:outro='http://www.w3.org/2001/XInclude' href='http://127.0.0.1:9/d29-nao-acessar'/>",
                "<include xmlns='http://www.w3.org/2001/XInclude' href='urn:d29:ficticio'/>",
                "<ext><fallback xmlns='http://www.w3.org/2001/XInclude'/></ext>"
            })
    void rejectsXIncludeWithoutResolvingOrEchoing(String extra) {
        assertThatThrownBy(() -> new NfeXmlService().ler(xml(extra)))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageNotContaining("d29-nao")
                .hasMessageNotContaining("127.0.0.1");
    }

    @Test
    void validScientificQuantityKeepsLiteralValue() {
        var r = new NfeXmlService().ler(xml(""));
        assertThat(r.itens().getFirst().quantidade()).isEqualByComparingTo("2");
    }

    @Test
    void harmlessCommentAndDifferentNamespaceAreNotXInclude() {
        var r =
                new NfeXmlService()
                        .ler(
                                xml(
                                        "<!-- http://www.w3.org/2001/XInclude --><include xmlns='urn:d29:anotacao'/>"));
        assertThat(r.itens()).hasSize(1);
    }
}
