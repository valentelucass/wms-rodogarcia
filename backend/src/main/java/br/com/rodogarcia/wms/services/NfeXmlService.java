package br.com.rodogarcia.wms.services;

import br.com.rodogarcia.wms.dto.NfeEntradaDto;
import br.com.rodogarcia.wms.exceptions.RegraNegocioException;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.io.StringReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

/** Extracao operacional limitada da NF-e 4.00; nao autentica documento fiscal. */
@Service
@Validated
public class NfeXmlService {
    private static final String NS = "http://www.portalfiscal.inf.br/nfe";

    public NfeEntradaDto ler(@NotBlank @Size(max = 1000000) String xml) {
        if (xml.getBytes(StandardCharsets.UTF_8).length > 1000000) throw invalido();
        try {
            var factory = DocumentBuilderFactory.newDefaultInstance();
            factory.setNamespaceAware(true);
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            factory.setAttribute("jdk.xml.maxElementDepth", 64);
            var builder = factory.newDocumentBuilder();
            builder.setEntityResolver(
                    (publicId, systemId) -> {
                        throw new SAXException("Referencia externa recusada");
                    });
            builder.setErrorHandler(
                    new DefaultHandler() {
                        @Override
                        public void error(SAXParseException e) throws SAXException {
                            throw e;
                        }

                        @Override
                        public void fatalError(SAXParseException e) throws SAXException {
                            throw e;
                        }
                    });
            Element root =
                    builder.parse(new InputSource(new StringReader(xml))).getDocumentElement();
            if (root.getElementsByTagNameNS("http://www.w3.org/2001/XInclude", "*").getLength()
                    != 0) throw invalido();
            Element nfe = nome(root, "nfeProc") ? unico(root, "NFe") : root;
            if (!nome(nfe, "NFe")) throw invalido();
            Element info = unico(nfe, "infNFe");
            if (!"4.00".equals(info.getAttribute("versao"))) throw invalido();
            String id = info.getAttribute("Id");
            if (!id.matches("NFe[0-9]{44}")) throw invalido();
            Element ide = unico(info, "ide");
            if (!"55".equals(texto(ide, "mod"))) throw invalido();
            String emitente = texto(unico(info, "emit"), "CNPJ").toUpperCase(java.util.Locale.ROOT);
            if (!emitente.matches("[A-Z0-9]{14}")) throw invalido();
            int serie = Integer.parseInt(texto(ide, "serie"));
            long numero = Long.parseLong(texto(ide, "nNF"));
            var emissao = OffsetDateTime.parse(texto(ide, "dhEmi")).toLocalDate();
            var itens = new ArrayList<NfeEntradaDto.Item>();
            for (Element det : filhos(info, "det")) {
                var produto = unico(det, "prod");
                itens.add(
                        new NfeEntradaDto.Item(
                                Integer.parseInt(det.getAttribute("nItem")),
                                CadastroSupport.codigo(texto(produto, "cProd")),
                                CadastroSupport.codigo(texto(produto, "uCom")),
                                new BigDecimal(texto(produto, "qCom")),
                                new BigDecimal(texto(produto, "vProd"))));
            }
            if (itens.isEmpty() || itens.size() > 200) throw invalido();
            return new NfeEntradaDto(
                    emitente, serie, numero, emissao, id.substring(3), List.copyOf(itens));
        } catch (RegraNegocioException ex) {
            throw ex;
        } catch (Exception ex) {
            throw invalido();
        }
    }

    public static String hash(String texto) {
        try {
            return HexFormat.of()
                    .formatHex(
                            MessageDigest.getInstance("SHA-256")
                                    .digest(texto.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 indisponivel", ex);
        }
    }

    private static boolean nome(Element e, String nome) {
        return NS.equals(e.getNamespaceURI()) && nome.equals(e.getLocalName());
    }

    private static List<Element> filhos(Element e, String nome) {
        var result = new ArrayList<Element>();
        for (Node n = e.getFirstChild(); n != null; n = n.getNextSibling())
            if (n instanceof Element child && nome(child, nome)) result.add(child);
        return result;
    }

    private static Element unico(Element e, String nome) {
        var encontrados = filhos(e, nome);
        if (encontrados.size() != 1) throw invalido();
        return encontrados.getFirst();
    }

    private static String texto(Element e, String nome) {
        var campo = unico(e, nome);
        for (Node n = campo.getFirstChild(); n != null; n = n.getNextSibling())
            if (n instanceof Element) throw invalido();
        String valor = campo.getTextContent().strip();
        if (valor.isEmpty() || valor.length() > 160) throw invalido();
        return valor;
    }

    private static RegraNegocioException invalido() {
        return CadastroSupport.invalido(
                "XML de entrada inválido ou fora do formato NF-e 4.00 suportado.");
    }
}
