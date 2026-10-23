package br.com.rodogarcia.wms.services;

import br.com.rodogarcia.wms.dto.ExpedicaoDto;
import br.com.rodogarcia.wms.dto.FiscalNfeDto;
import br.com.rodogarcia.wms.models.SolicitacaoNfe;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import tools.jackson.databind.json.JsonMapper;

final class FiscalNfeTestFixture {
    static final String CNPJ = "11111111000191", CLIENTE = "22222222000191";
    static final String ID = "00000000-0000-4000-8000-000000000001";
    static final Instant AGORA = Instant.parse("2026-10-23T12:00:00Z");
    static final String CHAVE =
            chave("352610" + CNPJ + "55" + "001" + "000000123" + "1" + "00000001");
    static final String ORIGEM =
            chave("352610" + CLIENTE + "55" + "001" + "000000100" + "1" + "00000001");

    static String chave(String base) {
        int soma = 0;
        for (int i = 0; i < 43; i++) soma += (base.charAt(42 - i) - '0') * (2 + i % 8);
        int resto = soma % 11;
        return base + (resto < 2 ? 0 : 11 - resto);
    }

    static FiscalNfeDto.DocumentoPlanejado documento() {
        return new FiscalNfeDto.DocumentoPlanejado(
                "CONSOLIDADO",
                "RETORNO_MERCADORIA",
                CNPJ,
                CLIENTE,
                "Cliente de teste",
                "Retorno de depósito",
                "4",
                2,
                null,
                List.of(
                        new FiscalNfeDto.Item(
                                7L,
                                8L,
                                9L,
                                10L,
                                11L,
                                "SKU1",
                                "Produto de teste",
                                "PC",
                                new BigDecimal("2"),
                                new BigDecimal("5"),
                                new BigDecimal("10.00"),
                                "39269090",
                                "5906",
                                ORIGEM,
                                3)),
                List.of(new ExpedicaoDto.Cobertura(7L, 10L, "SKU1", new BigDecimal("2"))),
                List.of(ORIGEM),
                new BigDecimal("10.00"),
                "Saída WMS: SAI-TESTE");
    }

    static String xml() {
        return """
                <nfeProc xmlns="http://www.portalfiscal.inf.br/nfe" versao="4.00"><NFe><infNFe versao="4.00" Id="NFe%s">
                <ide><mod>55</mod><serie>1</serie><nNF>123</nNF><dhEmi>2026-10-23T08:00:00-03:00</dhEmi><tpAmb>2</tpAmb><tpNF>1</tpNF><finNFe>4</finNFe><natOp>Retorno de depósito</natOp><NFref><refNFe>%s</refNFe></NFref></ide>
                <emit><CNPJ>%s</CNPJ></emit><dest><CNPJ>%s</CNPJ></dest>
                <det nItem="1"><prod><cProd>SKU1</cProd><uCom>PC</uCom><NCM>39269090</NCM><CFOP>5906</CFOP><qCom>2</qCom><vProd>10.00</vProd></prod></det>
                </infNFe></NFe><protNFe versao="4.00"><infProt><tpAmb>2</tpAmb><chNFe>%s</chNFe><nProt>123456789012345</nProt><cStat>100</cStat><dhRecbto>2026-10-23T08:01:00-03:00</dhRecbto></infProt></protNFe></nfeProc>
                """
                .formatted(CHAVE, ORIGEM, CNPJ, CLIENTE, CHAVE);
    }

    static SolicitacaoNfe solicitacao(String estado) {
        return new SolicitacaoNfe(
                ID,
                ID,
                UUID.nameUUIDFromBytes(
                                "confirmacao".getBytes(java.nio.charset.StandardCharsets.UTF_8))
                        .toString(),
                61L,
                "CONSOLIDADO",
                "RETORNO_MERCADORIA",
                4L,
                "a".repeat(64),
                "{\"TRANSMITIR\":\"1\"}",
                new JsonMapper().writeValueAsString(documento()),
                CNPJ,
                2,
                estado,
                estado.equals("PREPARADA") ? null : "remoto",
                null,
                null,
                null,
                null,
                null,
                null,
                estado.equals("AUTORIZADA") ? xml() : null,
                "Aguardando",
                "usuario",
                AGORA,
                AGORA);
    }
}
