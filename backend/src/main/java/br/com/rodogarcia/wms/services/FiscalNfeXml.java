package br.com.rodogarcia.wms.services;

import br.com.rodogarcia.wms.dto.ExpedicaoDto;
import br.com.rodogarcia.wms.dto.FiscalNfeDto;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import org.springframework.stereotype.Service;
import org.w3c.dom.Element;

/**
 * Conciliação do XML obtido pela consulta HTTPS ao emissor; não substitui auditoria de assinatura
 * SEFAZ.
 */
@Service
public class FiscalNfeXml {
    private static final String NS = "http://www.portalfiscal.inf.br/nfe";
    private final NfeXmlService parser;

    public FiscalNfeXml(NfeXmlService parser) {
        this.parser = parser;
    }

    public record Autorizacao(ExpedicaoDto.Nota nota, String protocolo, Instant autorizadaEm) {}

    public Autorizacao conferir(String xml, FiscalNfeDto.DocumentoPlanejado doc) {
        var root = parser.raiz(xml);
        if (!"nfeProc".equals(root.getLocalName())) throw invalido();
        var nfe = filho(root, "NFe");
        var info = filho(nfe, "infNFe");
        var ide = filho(info, "ide");
        var nf = parser.ler(xml);
        var prot = filho(filho(root, "protNFe"), "infProt");
        if (!java.util.Set.of("100", "150").contains(texto(prot, "cStat"))
                || !nf.chaveAcesso().equals(texto(prot, "chNFe"))
                || !doc.emitenteCnpj().equals(nf.emitente())
                || !Integer.toString(doc.ambiente()).equals(texto(prot, "tpAmb"))
                || !Integer.toString(doc.ambiente()).equals(texto(ide, "tpAmb"))
                || !Integer.toString(doc.natureza().equals("REGULARIZACAO_ENTRADA") ? 0 : 1)
                        .equals(texto(ide, "tpNF"))) throw invalido();
        if (!doc.finalidade().equals(texto(ide, "finNFe"))
                || !doc.naturezaOperacao().strip().equalsIgnoreCase(texto(ide, "natOp")))
            throw invalido();
        validarChave(nf.chaveAcesso());
        if (!nf.chaveAcesso().substring(6, 20).equals(nf.emitente())
                || Integer.parseInt(nf.chaveAcesso().substring(22, 25)) != nf.serie()
                || Long.parseLong(nf.chaveAcesso().substring(25, 34)) != nf.numero())
            throw invalido();
        var dest = filho(info, "dest");
        String destinatario =
                doc.destinatarioDocumento().length() == 11
                        ? texto(dest, "CPF")
                        : texto(dest, "CNPJ");
        if (!doc.destinatarioDocumento().equals(destinatario)) throw invalido();
        var referencias = new HashSet<String>();
        for (var ref : filhos(ide, "NFref")) referencias.add(texto(ref, "refNFe"));
        if (!referencias.equals(new HashSet<>(doc.chavesReferenciadas()))) throw invalido();
        var esperado = new HashMap<String, Integer>();
        var recebido = new HashMap<String, Integer>();
        for (var i : doc.itens())
            esperado.merge(
                    assinatura(
                            i.sku(),
                            i.unidade(),
                            i.ncm(),
                            i.cfop(),
                            i.quantidade(),
                            i.valorTotal()),
                    1,
                    Integer::sum);
        for (var det : filhos(info, "det")) {
            var prod = filho(det, "prod");
            recebido.merge(
                    assinatura(
                            texto(prod, "cProd"),
                            texto(prod, "uCom"),
                            texto(prod, "NCM"),
                            texto(prod, "CFOP"),
                            new BigDecimal(texto(prod, "qCom")),
                            new BigDecimal(texto(prod, "vProd"))),
                    1,
                    Integer::sum);
        }
        if (!esperado.equals(recebido)) throw invalido();
        String protocolo = texto(prot, "nProt");
        if (!protocolo.matches("[0-9]{15}")) throw invalido();
        return new Autorizacao(
                new ExpedicaoDto.Nota(
                        nf.emitente(),
                        Integer.toString(nf.serie()),
                        Long.toString(nf.numero()),
                        nf.emissao(),
                        nf.chaveAcesso()),
                protocolo,
                OffsetDateTime.parse(texto(prot, "dhRecbto")).toInstant());
    }

    public void conferirCancelamento(String xml, String chave, String cnpj, int ambiente) {
        var root = parser.raiz(xml);
        if (!java.util.Set.of("procEventoNFe", "procEvento").contains(root.getLocalName()))
            throw invalido();
        var evento = filho(filho(root, "evento"), "infEvento");
        var retorno = filho(filho(root, "retEvento"), "infEvento");
        if (!chave.equals(texto(evento, "chNFe"))
                || !chave.equals(texto(retorno, "chNFe"))
                || !"110111".equals(texto(evento, "tpEvento"))
                || !"110111".equals(texto(retorno, "tpEvento"))
                || !java.util.Set.of("135", "155").contains(texto(retorno, "cStat"))
                || !cnpj.equals(texto(evento, "CNPJ"))
                || !Integer.toString(ambiente).equals(texto(retorno, "tpAmb"))) throw invalido();
    }

    public static void validarChave(String chave) {
        if (chave == null || !chave.matches("[0-9]{44}")) throw invalido();
        int soma = 0, peso = 2;
        for (int i = 42; i >= 0; i--) {
            soma += (chave.charAt(i) - '0') * peso;
            peso = peso == 9 ? 2 : peso + 1;
        }
        int dv = 11 - soma % 11;
        if (dv >= 10) dv = 0;
        if (dv != chave.charAt(43) - '0') throw invalido();
    }

    private static String assinatura(
            String sku, String unidade, String ncm, String cfop, BigDecimal q, BigDecimal v) {
        return sku
                + "|"
                + unidade
                + "|"
                + ncm
                + "|"
                + cfop
                + "|"
                + q.stripTrailingZeros().toPlainString()
                + "|"
                + v.stripTrailingZeros().toPlainString();
    }

    private static List<Element> filhos(Element e, String name) {
        var out = new ArrayList<Element>();
        for (var n = e.getFirstChild(); n != null; n = n.getNextSibling())
            if (n instanceof Element a
                    && NS.equals(a.getNamespaceURI())
                    && name.equals(a.getLocalName())) out.add(a);
        return out;
    }

    private static Element filho(Element e, String name) {
        var list = filhos(e, name);
        if (list.size() != 1) throw invalido();
        return list.getFirst();
    }

    private static String texto(Element e, String name) {
        return filho(e, name).getTextContent().strip();
    }

    private static br.com.rodogarcia.wms.exceptions.RegraNegocioException invalido() {
        return CadastroSupport.invalido(
                "O XML retornado não corresponde à autorização, empresa, ambiente, referências ou itens desta solicitação.");
    }
}
