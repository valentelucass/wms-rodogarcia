package br.com.rodogarcia.wms.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.rodogarcia.wms.exceptions.RegraNegocioException;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class NfeXmlServiceTest {
    private final NfeXmlService service = new NfeXmlService();

    @Test
    void recusaDtdSemConsultarEnderecoExterno() throws Exception {
        var acessos = new AtomicInteger();
        var servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        servidor.createContext(
                "/externo",
                exchange -> {
                    acessos.incrementAndGet();
                    exchange.sendResponseHeaders(200, 0);
                    exchange.close();
                });
        servidor.start();
        try {
            String ataque =
                    "<!DOCTYPE nfeProc [<!ENTITY externo SYSTEM 'http://127.0.0.1:"
                            + servidor.getAddress().getPort()
                            + "/externo'>]>"
                            + xml().replace("<cProd>SKU</cProd>", "<cProd>&externo;</cProd>");
            assertThatThrownBy(() -> service.ler(ataque))
                    .isInstanceOf(RegraNegocioException.class)
                    .hasMessageNotContaining("127.0.0.1")
                    .hasMessageNotContaining("SYSTEM");
            assertThat(acessos).hasValue(0);
        } finally {
            servidor.stop(0);
        }
    }

    @Test
    void recusaEntidadesInternasEReferenciaAArquivo() {
        for (String declaracao :
                java.util.List.of(
                        "<!DOCTYPE NFe [<!ENTITY x 'segredo-ficticio'>]>",
                        "<!DOCTYPE NFe [<!ENTITY x SYSTEM 'file:///arquivo-ficticio'>]>")) {
            assertThatThrownBy(() -> service.ler(declaracao + xml().replace("SKU", "&x;")))
                    .isInstanceOf(RegraNegocioException.class)
                    .hasMessageNotContaining("segredo-ficticio")
                    .hasMessageNotContaining("file:");
        }
    }

    @Test
    void recusaProfundidadeExcessivaMesmoEmCampoIgnorado() {
        String profundo =
                xml().replace(
                                "</infNFe>",
                                "<extra>"
                                        + "<x>".repeat(70)
                                        + "valor"
                                        + "</x>".repeat(70)
                                        + "</extra></infNFe>");
        assertThatThrownBy(() -> service.ler(profundo)).isInstanceOf(RegraNegocioException.class);
    }

    @Test
    void recusaLimiteDeBytesUtf8() {
        String grande = xml().replace("</infNFe>", "<!--" + "á".repeat(500000) + "--></infNFe>");
        assertThat(grande.length()).isLessThan(1000000);
        assertThatThrownBy(() -> service.ler(grande)).isInstanceOf(RegraNegocioException.class);
    }

    @Test
    void recusaCamposObrigatoriosDuplicadosNamespaceOuModeloIncorretos() {
        for (String invalido :
                java.util.List.of(
                        xml().replace("</ide>", "<nNF>9</nNF></ide>"),
                        xml().replace(
                                        "http://www.portalfiscal.inf.br/nfe",
                                        "https://outro.invalid"),
                        xml().replace("<mod>55</mod>", "<mod>65</mod>"),
                        xml().replace("versao=\"4.00\"", "versao=\"3.10\""),
                        xml().replace("</prod>", "</ERRADO>"))) {
            assertThatThrownBy(() -> service.ler(invalido))
                    .isInstanceOf(RegraNegocioException.class);
        }
    }

    @Test
    void aceitaNfeOuEnvelopeSemConfundirDocumentoComMovimento() {
        var direto = service.ler(xml());
        var envelope =
                service.ler(
                        "<nfeProc xmlns=\"http://www.portalfiscal.inf.br/nfe\">"
                                + xml()
                                + "</nfeProc>");
        assertThat(direto).isEqualTo(envelope);
        assertThat(direto.itens().getFirst().quantidade()).isEqualByComparingTo("10");
        assertThat(direto.emissao()).hasToString("2026-09-01");
    }

    private String xml() {
        return """
 <NFe xmlns="http://www.portalfiscal.inf.br/nfe"><infNFe Id="NFe11111111111111111111111111111111111111111111" versao="4.00">
 <ide><mod>55</mod><serie>1</serie><nNF>123</nNF><dhEmi>2026-09-01T08:00:00-03:00</dhEmi></ide>
 <emit><CNPJ>12345678000199</CNPJ></emit><det nItem="1"><prod><cProd>SKU</cProd><uCom>UN</uCom><qCom>10</qCom><vProd>100.00</vProd></prod></det>
 </infNFe></NFe>
 """;
    }
}
