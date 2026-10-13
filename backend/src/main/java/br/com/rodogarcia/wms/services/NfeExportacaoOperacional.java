package br.com.rodogarcia.wms.services;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/** Adaptador limitado da exportação com nItem como elemento. Nunca altera o arquivo guardado. */
final class NfeExportacaoOperacional {
    private static final String NS = "http://www.portalfiscal.inf.br/nfe";

    private NfeExportacaoOperacional() {}

    static boolean reconhece(Element root) {
        return "nfeProc".equals(root.getLocalName())
                && NS.equals(root.getNamespaceURI())
                && root.getElementsByTagNameNS(NS, "versao").getLength() == 1
                && root.getElementsByTagNameNS(NS, "nItem").getLength() > 0;
    }

    static void preparar(Element root) {
        verificarArvore(root);
        if (!"4.00".equals(root.getAttribute("versao")) || !"4.00".equals(texto(root, "versao")))
            falhar();
        if (root.getElementsByTagNameNS("http://www.w3.org/2000/09/xmldsig#", "Signature")
                        .getLength()
                != 0) falhar();
        var info = unico(unico(root, "NFe"), "infNFe");
        var id = info.getAttribute("Id");
        if (!id.matches("NFe[0-9]{44}") || !id.equals(texto(info, "Id"))) falhar();
        if (info.hasAttribute("versao") && !"4.00".equals(info.getAttribute("versao"))) falhar();
        info.setAttribute("versao", "4.00");
        var ide = unico(info, "ide");
        if (!"55".equals(texto(ide, "mod"))
                || !Set.of("1", "2").contains(texto(ide, "tpAmb"))
                || !Set.of("0", "1").contains(texto(ide, "tpNF"))) falhar();
        OffsetDateTime.parse(texto(ide, "dhEmi"));
        texto(ide, "natOp");
        inteiro(ide, "serie", 0, 999);
        inteiro(ide, "nNF", 1, 999999999);
        participante(unico(info, "emit"));
        participante(unico(info, "dest"));
        var chave = id.substring(3);
        var emitente = unico(info, "emit");
        var documento = texto(emitente, opcional(emitente, "CNPJ") == null ? "CPF" : "CNPJ");
        var emissao = OffsetDateTime.parse(texto(ide, "dhEmi"));
        if (!chave.substring(6, 20).equals("0".repeat(14 - documento.length()) + documento)
                || !chave.substring(2, 6)
                        .equals(
                                emissao.format(
                                        java.time.format.DateTimeFormatter.ofPattern("yyMM")))
                || !chave.substring(20, 22).equals("55")
                || Integer.parseInt(chave.substring(22, 25)) != inteiro(ide, "serie", 0, 999)
                || Long.parseLong(chave.substring(25, 34)) != inteiro(ide, "nNF", 1, 999999999))
            falhar();
        if (!chave.substring(0, 2).equals(texto(ide, "cUF"))
                || !chave.substring(34, 35).equals(texto(ide, "tpEmis"))
                || !chave.substring(35, 43).equals(texto(ide, "cNF"))
                || !chave.substring(43).equals(texto(ide, "cDV"))) falhar();
        int soma = 0, peso = 2;
        for (int i = 42; i >= 0; i--) {
            soma += (chave.charAt(i) - '0') * peso;
            peso = peso == 9 ? 2 : peso + 1;
        }
        int dv = 11 - soma % 11;
        if ((dv >= 10 ? 0 : dv) != chave.charAt(43) - '0') falhar();
        var protocolo = unico(unico(root, "protNFe"), "infProt");
        if (!chave.equals(texto(protocolo, "chNFe"))) falhar();
        if (opcional(protocolo, "tpAmb") != null
                && !texto(ide, "tpAmb").equals(texto(protocolo, "tpAmb"))) falhar();
        texto(protocolo, "nProt");
        texto(protocolo, "cStat");
        texto(protocolo, "xMotivo");
        OffsetDateTime.parse(texto(protocolo, "dhRecbto"));
        var numeros = new HashSet<Integer>();
        BigDecimal somaProdutos = BigDecimal.ZERO;
        for (Node n = info.getFirstChild(); n != null; n = n.getNextSibling()) {
            if (!(n instanceof Element det) || !"det".equals(det.getLocalName())) continue;
            int numero = inteiro(det, "nItem", 1, 990);
            if (!numeros.add(numero) || det.hasAttribute("nItem")) falhar();
            det.setAttribute("nItem", String.valueOf(numero));
            var prod = unico(det, "prod");
            texto(prod, "cProd");
            texto(prod, "xProd");
            texto(prod, "uCom");
            texto(prod, "uTrib");
            if (!texto(prod, "NCM").matches("[0-9]{2}|[0-9]{8}")
                    || !texto(prod, "CFOP").matches("[0-9]{4}")) falhar();
            numero(prod, "qCom", true, 11, 4);
            numero(prod, "qTrib", true, 11, 4);
            numero(prod, "vUnCom", false, 11, 10);
            numero(prod, "vUnTrib", false, 11, 10);
            somaProdutos = somaProdutos.add(numero(prod, "vProd", false, 13, 2));
        }
        if (numeros.isEmpty() || numeros.size() > 200) falhar();
        var total = unico(unico(info, "total"), "ICMSTot");
        if (somaProdutos.compareTo(numero(total, "vProd", false, 13, 2)) != 0) falhar();
        numero(total, "vNF", false, 13, 2);
        var transporte = unico(info, "transp");
        for (Node n = transporte.getFirstChild(); n != null; n = n.getNextSibling()) {
            if (!(n instanceof Element vol) || !"vol".equals(vol.getLocalName())) continue;
            for (var campo : new String[] {"pesoL", "pesoB", "qVol"})
                if (opcional(vol, campo) != null)
                    numero(vol, campo, false, 15, "qVol".equals(campo) ? 0 : 3);
        }
    }

    private static void verificarArvore(Element e) {
        if (!NS.equals(e.getNamespaceURI())) falhar();
        var nomes = new HashSet<String>();
        for (Node n = e.getFirstChild(); n != null; n = n.getNextSibling()) {
            if (n instanceof Element child) {
                var nome = child.getLocalName();
                if (!nomes.add(nome)
                        && !Set.of(
                                        "det",
                                        "vol",
                                        "detPag",
                                        "autXML",
                                        "dup",
                                        "NFref",
                                        "rastro",
                                        "reboque",
                                        "lacres",
                                        "obsCont",
                                        "obsFisco")
                                .contains(nome)) falhar();
                verificarArvore(child);
            }
        }
    }

    private static void participante(Element e) {
        var cnpj = opcional(e, "CNPJ");
        var cpf = opcional(e, "CPF");
        if ((cnpj == null) == (cpf == null)) falhar();
        if (!texto(e, cnpj == null ? "CPF" : "CNPJ")
                .matches(cnpj == null ? "[0-9]{11}" : "[A-Z0-9]{14}")) falhar();
        texto(e, "xNome");
    }

    private static int inteiro(Element e, String nome, int min, int max) {
        var v = texto(e, nome);
        if (!v.matches("[0-9]{1,9}")) {
            falhar();
        }
        int n = Integer.parseInt(v);
        if (n < min || n > max) falhar();
        return n;
    }

    private static BigDecimal numero(
            Element e, String nome, boolean positivo, int inteiros, int decimais) {
        var s = texto(e, nome);
        if (!s.matches("[0-9]{1," + inteiros + "}(\\.[0-9]{1," + Math.max(1, decimais) + "})?")
                || (decimais == 0 && s.contains("."))) falhar();
        var n = new BigDecimal(s);
        if (positivo && n.signum() <= 0) falhar();
        return n;
    }

    private static Element opcional(Element e, String nome) {
        Element found = null;
        for (Node n = e.getFirstChild(); n != null; n = n.getNextSibling())
            if (n instanceof Element child
                    && nome.equals(child.getLocalName())
                    && NS.equals(child.getNamespaceURI())) {
                if (found != null) falhar();
                found = child;
            }
        return found;
    }

    private static Element unico(Element e, String nome) {
        var v = opcional(e, nome);
        if (v == null) falhar();
        return v;
    }

    private static String texto(Element e, String nome) {
        var v = unico(e, nome);
        for (Node n = v.getFirstChild(); n != null; n = n.getNextSibling())
            if (n instanceof Element) falhar();
        var s = v.getTextContent().strip();
        if (s.isEmpty() || s.length() > 6000) falhar();
        return s;
    }

    private static void falhar() {
        throw CadastroSupport.invalido(
                "Exportação XML inconsistente: confira identificação, itens, totais e campos repetidos. O arquivo original foi preservado.");
    }
}
