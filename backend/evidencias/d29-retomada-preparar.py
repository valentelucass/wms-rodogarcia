import hashlib
import json
import re
from pathlib import Path

root = Path(__file__).resolve().parents[1]
original = root / "src/test/java/br/com/rodogarcia/wms/FechamentoIntegrationTest.java"
source = original.read_text(encoding="utf-8-sig")
out = root / "src/test/java/br/com/rodogarcia/wms"

def method(name):
    parts = []
    pattern = re.compile(r"(?m)^    private [^{;]*?\b" + re.escape(name) + r"\(")
    for match in pattern.finditer(source):
        start = match.start()
        brace = source.index("{", match.end())
        depth = 1
        cursor = brace + 1
        while depth:
            if source[cursor] == "{":
                depth += 1
            elif source[cursor] == "}":
                depth -= 1
            cursor += 1
        parts.append(source[start:cursor])
    if not parts:
        raise RuntimeError("Metodo de apoio ausente: " + name)
    return "\n\n".join(parts)

imports = source[:source.index('@ActiveProfiles')]
imports = imports.replace("import java.math.BigDecimal;", "import java.math.BigDecimal;\nimport java.security.MessageDigest;\nimport java.util.HexFormat;")
setup_start = source.index("        new TransactionTemplate(transactions)", source.index("    void preparar()"))
setup_end = source.index("\n    }\n", setup_start)
setup = source[setup_start:setup_end]
setup = setup.replace('new Cliente("C",', 'new Cliente("C" + numero,')
setup = setup.replace('"A",\n', '"A" + numero,\n')
methods = "\n\n".join(method(n) for n in [
    "configurar", "configurarContrato", "fato", "prepararComando", "preparar", "rota",
    "decisao", "decisaoNumero", "aprovar", "reabrirComando", "reabrir", "entregaComando",
    "entregar", "confirmar", "servico", "item", "tabela", "vinculoComando", "vincular",
    "calcularComando", "calcular", "unidade", "comando", "request", "post", "get",
    "resposta", "token"
])
methods = methods.replace('        var s = servico("ADICIONAL", "VEICULO");',
                          '        var s = servico("ADICIONAL", "VEICULO");\n        servicoId = s.get("id").longValue();')
methods = methods.replace('jdbc.queryForObject(\n                        "select id from wms.servico_cobranca where tipo=\'ADICIONAL\'", Long.class)',
                          "servicoId")
header = '''
/** Provas locais F33/F34. H2 exclusivo; nao valida corte ou aprovacao no SQL Server. */
@ActiveProfiles("test")
@TestPropertySource(
        locations = "classpath:cadastros-test.properties",
        properties = {
            "spring.datasource.url=jdbc:h2:mem:wms-d29-retomada;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000;INIT=CREATE SCHEMA IF NOT EXISTS wms",
            "spring.datasource.username=d29_retomada_local",
            "spring.datasource.password=",
            "spring.datasource.driver-class-name=org.h2.Driver",
            "spring.jpa.hibernate.ddl-auto=create-drop",
            "spring.flyway.enabled=false"
        })
@Import(IdentidadeTesteConfig.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class D29FinanceiroRetomadaTest {
    @Autowired Environment environment;
    @Autowired JsonMapper mapper;
    @Autowired JwtEncoder encoder;
    @Autowired JdbcTemplate jdbc;
    @Autowired PlatformTransactionManager transactions;
    @MockitoBean Clock clock;
    @PersistenceContext EntityManager em;
    private final HttpClient http =
            HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private static final Instant BASE = Instant.parse("2026-08-01T12:00:00Z");
    private static final AtomicInteger FAMILIAS = new AtomicInteger(7000);
    private Long clienteId, armazemId, produtoId, embalagemId, servicoId;
    private int numero;
    private String gestor, supervisor, operador;

    private record Unidade(Long id, Long pedidoId, Long notaId, String codigo) {}

    @BeforeEach
    void prepararFixtureLocal() throws Exception {
        try (var conexao = jdbc.getDataSource().getConnection()) {
            assertThat(conexao.getMetaData().getURL()).startsWith("jdbc:h2:mem:wms-d29-retomada");
            assertThat(conexao.getMetaData().getDatabaseProductName()).isEqualTo("H2");
        }
        when(clock.instant()).thenReturn(Instant.parse("2028-06-06T12:00:00Z"));
        SecurityContextHolder.clearContext();
        numero = FAMILIAS.addAndGet(100);
'''
tests = '''
    @Test
    void f33ReabrirV1AprovadaPreservaMemoriaBytesHashEExigeNovaAprovacaoV2() throws Exception {
        configurar("2026-09-01", "DIAS_CORRIDOS", null, 30, null);
        fato("10", "2026-09-02T12:00:00Z");
        var v1 = aprovar(preparar(calcular("2026-09-01", "2026-10-01")));
        assertThat(v1.get("versao").get("situacao").asString()).isEqualTo("APROVADA");
        assertThat(v1.get("versao").get("saldo").decimalValue()).isEqualByComparingTo("100");
        long fechamentoId = v1.get("fechamento").get("id").longValue();
        long versaoId = v1.get("versao").get("id").longValue();
        byte[] arquivoAntes = get(rota(v1) + "/versoes/1/demonstrativo", supervisor).body()
                .getBytes(StandardCharsets.UTF_8);
        String memoriaAntes = jdbc.queryForObject(
                "select memoria_json from wms.versao_fechamento where id=?", String.class, versaoId);
        String hashAntes = jdbc.queryForObject(
                "select conteudo_hash from wms.versao_fechamento where id=?", String.class, versaoId);
        String digestIndependente = HexFormat.of().formatHex(
                MessageDigest.getInstance("SHA-256").digest(arquivoAntes));
        assertThat(hashAntes).isEqualTo(digestIndependente);
        assertThat(memoriaAntes.getBytes(StandardCharsets.UTF_8)).isEqualTo(arquivoAntes);
        var v2 = reabrir(v1, calcular("2026-09-01", "2026-10-01"));
        assertThat(v2.get("versao").get("numero").asInt()).isEqualTo(2);
        assertThat(v2.get("versao").get("situacao").asString()).isEqualTo("PENDENTE_REVISAO");
        assertThat(v2.get("versao").get("decisor").isNull()).isTrue();
        assertThat(jdbc.queryForObject(
                "select decidida_em from wms.versao_fechamento where id=?",
                java.sql.Timestamp.class, v2.get("versao").get("id").longValue())).isNull();
        assertThat(jdbc.queryForObject(
                "select situacao from wms.versao_fechamento where id=?", String.class, versaoId))
                .isEqualTo("SUPERADA");
        assertThat(jdbc.queryForObject(
                "select memoria_json from wms.versao_fechamento where id=?", String.class, versaoId))
                .isEqualTo(memoriaAntes);
        assertThat(jdbc.queryForObject(
                "select conteudo_hash from wms.versao_fechamento where id=?", String.class, versaoId))
                .isEqualTo(hashAntes);
        assertThat(get(rota(v2) + "/versoes/1/demonstrativo", supervisor).body()
                .getBytes(StandardCharsets.UTF_8)).isEqualTo(arquivoAntes);
        var recusada = resposta(post(rota(v2) + "/entregas", entregaComando(v2), gestor), 409);
        assertThat(recusada.toString()).contains("VERSAO_NAO_APROVADA");
        assertThat(jdbc.queryForObject(
                "select count(*) from wms.entrega_esl where versao_id=?",
                Integer.class, v2.get("versao").get("id").longValue())).isZero();
        var aprovadaV2 = aprovar(v2);
        assertThat(aprovadaV2.get("versao").get("situacao").asString()).isEqualTo("APROVADA");
        assertThat(aprovadaV2.get("versao").get("decisor").isNull()).isFalse();
        assertThat(aprovadaV2.get("fechamento").get("id").longValue()).isEqualTo(fechamentoId);
        assertThat(get(rota(v2) + "/versoes/1/demonstrativo", supervisor).body()
                .getBytes(StandardCharsets.UTF_8)).isEqualTo(arquivoAntes);
        assertThat(jdbc.queryForObject(
                "select conteudo_hash from wms.versao_fechamento where id=?", String.class, versaoId))
                .isEqualTo(hashAntes);
    }

    @Test
    void f34RecusaDesconhecidoNaoAlteraVersoesMemoriaAuditoriaOuReplayAntesDeclaracao()
            throws Exception {
        configurar("2026-09-01", "DIAS_CORRIDOS", null, 30, null);
        fato("10", "2026-09-02T12:00:00Z");
        var f = entregar(aprovar(preparar(calcular("2026-09-01", "2026-10-01"))));
        assertThat(f.get("versao").get("estadoExterno").asString()).isEqualTo("DESCONHECIDO");
        var novo = calcular("2026-09-01", "2026-10-01");
        var comandoRecusado = reabrirComando(f, novo);
        long fechamentoId = f.get("fechamento").get("id").longValue();
        var versoesAntes = jdbc.queryForList(
                "select * from wms.versao_fechamento where fechamento_id=? order by id", fechamentoId);
        var fechamentoAntes = jdbc.queryForList(
                "select * from wms.fechamento_cobranca where id=?", fechamentoId);
        var auditoriaAntes = jdbc.queryForList("select * from wms.auditoria_cadastro order by id");
        var operacoesAntes = jdbc.queryForList("select * from wms.operacao_administrativa order by id");
        byte[] memoriaAntes = get(rota(f) + "/versoes/1/demonstrativo", supervisor).body()
                .getBytes(StandardCharsets.UTF_8);
        var recusa = resposta(post(rota(f) + "/reabertura", comandoRecusado, gestor), 409);
        assertThat(recusa.toString()).contains("EMISSAO_DESCONHECIDA");
        // Todas estas consultas ocorrem imediatamente apos a recusa, antes de qualquer declaracao.
        assertThat(jdbc.queryForList(
                "select * from wms.versao_fechamento where fechamento_id=? order by id", fechamentoId))
                .isEqualTo(versoesAntes);
        assertThat(jdbc.queryForList("select * from wms.fechamento_cobranca where id=?", fechamentoId))
                .isEqualTo(fechamentoAntes);
        assertThat(jdbc.queryForList("select * from wms.auditoria_cadastro order by id"))
                .isEqualTo(auditoriaAntes);
        assertThat(jdbc.queryForList("select * from wms.operacao_administrativa order by id"))
                .isEqualTo(operacoesAntes);
        assertThat(get(rota(f) + "/versoes/1/demonstrativo", supervisor).body()
                .getBytes(StandardCharsets.UTF_8)).isEqualTo(memoriaAntes);
        f = confirmar(f);
        assertThat(f.get("versao").get("estadoExterno").asString()).isEqualTo("NAO_EMITIDO");
        var aceita = resposta(post(rota(f) + "/reabertura", reabrirComando(f, novo), gestor), 200);
        assertThat(aceita.get("versao").get("numero").asInt()).isEqualTo(2);
        assertThat(aceita.get("versao").get("situacao").asString()).isEqualTo("PENDENTE_REVISAO");
        assertThat(get(rota(aceita) + "/versoes/1/demonstrativo", supervisor).body()
                .getBytes(StandardCharsets.UTF_8)).isEqualTo(memoriaAntes);
    }
'''
finance = imports + header + setup + '\n    }\n' + tests + "\n" + methods + "\n}\n"
for filename, content in [("D29FinanceiroRetomadaTest.java", finance)]:
    target = out / filename
    with target.open("x", encoding="utf-8", newline="\n") as f:
        f.write(content)

xml = '''package br.com.rodogarcia.wms;

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
        String aceita = xml(1).replace("</infNFe>", "<x>".repeat(62) + "valor" + "</x>".repeat(62) + "</infNFe>");
        String recusa = xml(1).replace("</infNFe>", "<x>".repeat(63) + "valor" + "</x>".repeat(63) + "</infNFe>");
        assertThat(profundidade(documento(aceita), 1)).isEqualTo(64);
        assertThat(profundidade(documento(recusa), 1)).isEqualTo(65);
        assertThat(service.ler(aceita).itens()).hasSize(1);
        assertThatThrownBy(() -> service.ler(recusa)).isInstanceOf(RegraNegocioException.class);
    }

    @Test
    void duzentosItensAceitaEDuzentosEUmRecusaComItensValidos() throws Exception {
        String aceita = xml(200);
        String recusa = xml(201);
        assertThat(documento(aceita).getElementsByTagNameNS("http://www.portalfiscal.inf.br/nfe", "det").getLength()).isEqualTo(200);
        assertThat(documento(recusa).getElementsByTagNameNS("http://www.portalfiscal.inf.br/nfe", "det").getLength()).isEqualTo(201);
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
        return factory.newDocumentBuilder().parse(new InputSource(new StringReader(xml))).getDocumentElement();
    }

    private int profundidade(Element elemento, int atual) {
        int maxima = atual;
        for (Node n = elemento.getFirstChild(); n != null; n = n.getNextSibling()) {
            if (n instanceof Element filho) maxima = Math.max(maxima, profundidade(filho, atual + 1));
        }
        return maxima;
    }

    private String xml(int itens) {
        StringBuilder detalhes = new StringBuilder();
        for (int i = 1; i <= itens; i++) detalhes.append("<det nItem='").append(i).append("'><prod><cProd>SKU")
                .append(i).append("</cProd><uCom>UN</uCom><qCom>1</qCom><vProd>2.00</vProd></prod></det>");
        return "<NFe xmlns='http://www.portalfiscal.inf.br/nfe'><infNFe Id='NFe"
                + "1".repeat(44) + "' versao='4.00'><ide><mod>55</mod><serie>1</serie><nNF>123</nNF>"
                + "<dhEmi>2026-09-01T08:00:00-03:00</dhEmi></ide><emit><CNPJ>12345678000199</CNPJ></emit>"
                + detalhes + "</infNFe></NFe>";
    }
}
'''
with (out / "D29XmlFronteirasRetomadaTest.java").open("x", encoding="utf-8", newline="\n") as f:
    f.write(xml)
print(json.dumps({"created": ["D29FinanceiroRetomadaTest.java", "D29XmlFronteirasRetomadaTest.java"],
                  "apoioExtraidoDe": str(original), "shaFonteApoio": hashlib.sha256(original.read_bytes()).hexdigest().upper()}))
