package br.com.rodogarcia.wms.services;

import br.com.rodogarcia.wms.dto.NfeImportacaoDto;
import br.com.rodogarcia.wms.exceptions.RegraNegocioException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javax.xml.XMLConstants;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import org.springframework.stereotype.Service;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.bootstrap.DOMImplementationRegistry;
import org.w3c.dom.ls.DOMImplementationLS;

/** Projeção documental e XSD local; não verifica assinatura ou situação externa. */
@Service
public class NfeDocumentoService {
    private static final String NS = "http://www.portalfiscal.inf.br/nfe";
    private static final String BASE = "/nfe/PL_010b_v1.30/";
    private static final Set<String> SCHEMAS =
            Set.of(
                    "nfe_v4.00.xsd",
                    "wms_procNFe_v4.00.xsd",
                    "leiauteNFe_v4.00.xsd",
                    "tiposBasico_v4.00.xsd",
                    "DFeTiposBasicos_v1.00.xsd",
                    "xmldsig-core-schema_v1.01.xsd");
    private final NfeXmlService xml;
    private final Schema schema;
    private final RegraBobinaPorItem bobinas;

    public NfeDocumentoService(NfeXmlService xml) {
        this(xml, new RegraBobinaPorItem(""));
    }

    @org.springframework.beans.factory.annotation.Autowired
    public NfeDocumentoService(NfeXmlService xml, RegraBobinaPorItem bobinas) {
        this.xml = xml;
        this.bobinas = bobinas;
        try {
            var factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            var dom =
                    (DOMImplementationLS)
                            DOMImplementationRegistry.newInstance().getDOMImplementation("LS");
            factory.setResourceResolver(
                    (type, namespace, publicId, systemId, baseUri) -> {
                        if (!SCHEMAS.contains(systemId))
                            throw new IllegalStateException("XSD externo recusado");
                        var input = dom.createLSInput();
                        input.setByteStream(
                                NfeDocumentoService.class.getResourceAsStream(BASE + systemId));
                        input.setSystemId("classpath:" + BASE + systemId);
                        return input;
                    });
            var source =
                    new StreamSource(
                            getClass().getResourceAsStream(BASE + "wms_procNFe_v4.00.xsd"));
            source.setSystemId("classpath:" + BASE + "wms_procNFe_v4.00.xsd");
            schema = factory.newSchema(source);
        } catch (Exception e) {
            throw new IllegalStateException("Esquema NF-e local indisponível", e);
        }
    }

    public NfeImportacaoDto.Documento ler(String original) {
        var base = xml.ler(original);
        try {
            var root = xml.raiz(original);
            boolean exportacao = NfeExportacaoOperacional.reconhece(root);
            if (exportacao) NfeExportacaoOperacional.preparar(root);
            var validator = schema.newValidator();
            validator.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            validator.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            if (!exportacao) validator.validate(new DOMSource(root));
            var nfe = "NFe".equals(root.getLocalName()) ? root : unico(root, "NFe");
            var info = unico(nfe, "infNFe");
            var ide = unico(info, "ide");
            var chave = base.chaveAcesso();
            if (!chave.substring(6, 20)
                            .equals("0".repeat(14 - base.emitente().length()) + base.emitente())
                    || !chave.substring(20, 22).equals("55")
                    || Integer.parseInt(chave.substring(22, 25)) != base.serie()
                    || Long.parseLong(chave.substring(25, 34)) != base.numero()) throw invalido();
            var itens = new ArrayList<NfeImportacaoDto.Item>();
            for (var det : filhos(info, "det")) {
                var p = unico(det, "prod");
                itens.add(
                        new NfeImportacaoDto.Item(
                                Integer.parseInt(det.getAttribute("nItem")),
                                texto(p, "cProd"),
                                texto(p, "xProd"),
                                texto(p, "cEAN"),
                                texto(p, "NCM"),
                                texto(p, "CFOP"),
                                texto(p, "uCom"),
                                decimal(p, "qCom"),
                                decimal(p, "vUnCom"),
                                decimal(p, "vProd"),
                                texto(p, "uTrib"),
                                decimal(p, "qTrib"),
                                texto(p, "cEANTrib"),
                                texto(det, "infAdProd"),
                                bobinas.aplica(base.emitente(), texto(p, "cProd"), texto(p, "uCom"))
                                        ? 1
                                        : null));
            }
            var volumes = new ArrayList<NfeImportacaoDto.Volume>();
            for (var vol : filhos(unico(info, "transp"), "vol"))
                volumes.add(
                        new NfeImportacaoDto.Volume(
                                decimal(vol, "qVol"),
                                texto(vol, "esp"),
                                decimal(vol, "pesoL"),
                                decimal(vol, "pesoB")));
            NfeImportacaoDto.Protocolo protocolo = null;
            var prot = opcional(root, "protNFe");
            if (prot != null) {
                var infProt = unico(prot, "infProt");
                protocolo =
                        new NfeImportacaoDto.Protocolo(
                                texto(infProt, "chNFe"),
                                texto(infProt, "tpAmb"),
                                texto(infProt, "nProt"),
                                texto(infProt, "dhRecbto"),
                                texto(infProt, "cStat"),
                                texto(infProt, "xMotivo"));
                if (!chave.equals(protocolo.chave())
                        || (!(exportacao && protocolo.ambiente() == null)
                                && !texto(ide, "tpAmb").equals(protocolo.ambiente())))
                    throw invalido();
            }
            var additional = opcional(info, "infAdic");
            return new NfeImportacaoDto.Documento(
                    "4.00",
                    "55",
                    chave,
                    base.serie(),
                    base.numero(),
                    texto(ide, "dhEmi"),
                    texto(ide, "tpNF"),
                    texto(ide, "tpAmb"),
                    texto(ide, "natOp"),
                    participante(unico(info, "emit")),
                    participante(unico(info, "dest")),
                    decimal(unico(unico(info, "total"), "ICMSTot"), "vNF"),
                    List.copyOf(itens),
                    List.copyOf(volumes),
                    protocolo,
                    texto(additional, "infCpl"),
                    texto(additional, "infAdFisco"),
                    exportacao ? "EXPORTACAO_OPERACIONAL" : "NFE_ESQUEMA_LOCAL",
                    avisos(itens, volumes, exportacao));
        } catch (RegraNegocioException e) {
            throw e;
        } catch (Exception e) {
            throw invalido();
        }
    }

    private static List<String> avisos(
            List<NfeImportacaoDto.Item> itens,
            List<NfeImportacaoDto.Volume> volumes,
            boolean exportacao) {
        var avisos = new ArrayList<String>();
        avisos.add(
                exportacao
                        ? "Arquivo de exportação operacional: campos de recebimento conferidos, sem validação pelo esquema fiscal completo e sem assinatura. O XML original será preservado; autenticidade e situação na SEFAZ não foram consultadas."
                        : "Arquivo conferido pelo esquema local. Assinatura, autenticidade e situação atual não foram verificadas na SEFAZ.");
        BigDecimal totalKg = BigDecimal.ZERO;
        boolean massa = true;
        for (var i : itens) {
            if (i.unidadeComercial().equals(i.unidadeTributavel())
                    && i.quantidadeComercial().compareTo(i.quantidadeTributavel()) != 0)
                avisos.add(
                        "Item "
                                + i.numeroItem()
                                + ": quantidade comercial "
                                + i.quantidadeComercial().toPlainString()
                                + " "
                                + i.unidadeComercial()
                                + " difere da tributável "
                                + i.quantidadeTributavel().toPlainString()
                                + " "
                                + i.unidadeTributavel()
                                + ". A previsão usa a quantidade comercial; confira o peso físico.");
            try {
                totalKg =
                        totalKg.add(
                                ConversaoUnidadeNfe.converter(
                                        i.quantidadeComercial(), i.unidadeComercial(), "KG"));
            } catch (RegraNegocioException e) {
                massa = false;
            }
        }
        if (massa
                && !volumes.isEmpty()
                && volumes.stream().allMatch(v -> v.pesoLiquido() != null)) {
            var liquido =
                    volumes.stream()
                            .map(NfeImportacaoDto.Volume::pesoLiquido)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
            if (liquido.compareTo(totalKg) != 0)
                avisos.add(
                        "A soma comercial dos itens é "
                                + totalKg.stripTrailingZeros().toPlainString()
                                + " KG; o peso líquido informado é "
                                + liquido.toPlainString()
                                + " KG. Valores preservados, sem correção automática de escala. Confira na chegada.");
        }
        if (itens.stream().anyMatch(i -> i.bobinasPrevistas() != null))
            avisos.add(
                    "Regra deste cliente/produto: cada item indica uma bobina e uma posição prevista. Linhas do mesmo SKU permanecem separadas. A conferência confirma peso, condição e contagem; a posição será escolhida no endereçamento. Lotes em texto livre não são associados automaticamente.");
        return List.copyOf(avisos);
    }

    private static NfeImportacaoDto.Participante participante(Element e) {
        String doc = texto(e, "CNPJ");
        if (doc == null) doc = texto(e, "CPF");
        return new NfeImportacaoDto.Participante(doc, texto(e, "xNome"));
    }

    private static List<Element> filhos(Element e, String nome) {
        var list = new ArrayList<Element>();
        if (e != null)
            for (Node n = e.getFirstChild(); n != null; n = n.getNextSibling())
                if (n instanceof Element child
                        && NS.equals(child.getNamespaceURI())
                        && nome.equals(child.getLocalName())) list.add(child);
        return list;
    }

    private static Element opcional(Element e, String nome) {
        var found = filhos(e, nome);
        if (found.size() > 1) throw invalido();
        return found.isEmpty() ? null : found.getFirst();
    }

    private static Element unico(Element e, String nome) {
        var found = opcional(e, nome);
        if (found == null) throw invalido();
        return found;
    }

    private static String texto(Element e, String nome) {
        var found = opcional(e, nome);
        return found == null ? null : found.getTextContent().strip();
    }

    private static BigDecimal decimal(Element e, String nome) {
        var s = texto(e, nome);
        return s == null ? null : new BigDecimal(s);
    }

    private static RegraNegocioException invalido() {
        return CadastroSupport.invalido(
                "XML não corresponde à NF-e 4.00/modelo 55 e ao esquema suportado, ou sua identificação/protocolo é inconsistente. Selecione o arquivo completo da nota.");
    }
}
