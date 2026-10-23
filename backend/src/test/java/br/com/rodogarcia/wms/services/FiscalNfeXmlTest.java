package br.com.rodogarcia.wms.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class FiscalNfeXmlTest {
    final FiscalNfeXml parser = new FiscalNfeXml(new NfeXmlService());

    @Test
    void conciliaProtocoloItensReferenciasAmbienteEDestinatario() {
        var a = parser.conferir(FiscalNfeTestFixture.xml(), FiscalNfeTestFixture.documento());
        assertThat(a.nota().numero()).isEqualTo("123");
        assertThat(a.protocolo()).isEqualTo("123456789012345");
        assertThat(a.nota().emitenteCnpj()).isEqualTo(FiscalNfeTestFixture.CNPJ);
    }

    @ParameterizedTest
    @CsvSource({
        "<cStat>100</cStat>,<cStat>110</cStat>",
        "<tpAmb>2</tpAmb>,<tpAmb>1</tpAmb>",
        "<qCom>2</qCom>,<qCom>3</qCom>",
        "<vProd>10.00</vProd>,<vProd>11.00</vProd>",
        "<uCom>PC</uCom>,<uCom>KG</uCom>",
        "<tpNF>1</tpNF>,<tpNF>0</tpNF>",
        "<CFOP>5906</CFOP>,<CFOP>5102</CFOP>",
        "<NCM>39269090</NCM>,<NCM>12345678</NCM>",
        "<finNFe>4</finNFe>,<finNFe>1</finNFe>"
    })
    void recusaDocumentoDivergenteAntesDeCriarCobertura(String original, String alterado) {
        assertThatThrownBy(
                        () ->
                                parser.conferir(
                                        FiscalNfeTestFixture.xml().replace(original, alterado),
                                        FiscalNfeTestFixture.documento()))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void xmlDoOutroProprietarioOuOutraOrigemNaoAutorizaPedido() {
        assertThatThrownBy(
                        () ->
                                parser.conferir(
                                        FiscalNfeTestFixture.xml()
                                                .replace(
                                                        "<dest><CNPJ>"
                                                                + FiscalNfeTestFixture.CLIENTE,
                                                        "<dest><CNPJ>33333333000191"),
                                        FiscalNfeTestFixture.documento()))
                .isInstanceOf(RuntimeException.class);
        assertThatThrownBy(
                        () ->
                                parser.conferir(
                                        FiscalNfeTestFixture.xml()
                                                .replace(
                                                        FiscalNfeTestFixture.ORIGEM,
                                                        FiscalNfeTestFixture.CHAVE),
                                        FiscalNfeTestFixture.documento()))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void chaveComDigitoErradoNaoEhAceita() {
        assertThatThrownBy(
                        () ->
                                FiscalNfeXml.validarChave(
                                        FiscalNfeTestFixture.CHAVE.substring(0, 43)
                                                + ((FiscalNfeTestFixture.CHAVE.charAt(43) - '0' + 1)
                                                        % 10)))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void rejeitaEntidadeExternaESemProtocolo() {
        assertThatThrownBy(
                        () ->
                                parser.conferir(
                                        "<!DOCTYPE x [<!ENTITY y SYSTEM 'file:///segredo'>]><x>&y;</x>",
                                        FiscalNfeTestFixture.documento()))
                .isInstanceOf(RuntimeException.class);
        assertThatThrownBy(
                        () ->
                                parser.conferir(
                                        FiscalNfeTestFixture.xml()
                                                .replaceFirst("<protNFe[\\s\\S]*</protNFe>", ""),
                                        FiscalNfeTestFixture.documento()))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void cancelamentoExigeEventoCorrespondenteEConfirmado() {
        String xml =
                "<procEventoNFe xmlns=\"http://www.portalfiscal.inf.br/nfe\"><evento><infEvento><CNPJ>"
                        + FiscalNfeTestFixture.CNPJ
                        + "</CNPJ><chNFe>"
                        + FiscalNfeTestFixture.CHAVE
                        + "</chNFe><tpEvento>110111</tpEvento></infEvento></evento><retEvento><infEvento><tpAmb>2</tpAmb><chNFe>"
                        + FiscalNfeTestFixture.CHAVE
                        + "</chNFe><tpEvento>110111</tpEvento><cStat>135</cStat></infEvento></retEvento></procEventoNFe>";
        parser.conferirCancelamento(xml, FiscalNfeTestFixture.CHAVE, FiscalNfeTestFixture.CNPJ, 2);
        assertThatThrownBy(
                        () ->
                                parser.conferirCancelamento(
                                        xml.replace("135", "573"),
                                        FiscalNfeTestFixture.CHAVE,
                                        FiscalNfeTestFixture.CNPJ,
                                        2))
                .isInstanceOf(RuntimeException.class);
    }
}
