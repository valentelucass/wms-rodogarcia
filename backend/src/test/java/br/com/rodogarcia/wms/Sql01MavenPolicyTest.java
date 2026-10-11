package br.com.rodogarcia.wms;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.util.Arrays;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPathFactory;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;

/** Confere seleção e recusas do Maven lendo somente XML local; nenhum contexto ou banco. */
class Sql01MavenPolicyTest {
    @Test
    void padraoRecusaAntesDeExecutarTestesSemProvaSql() throws Exception {
        var pom = pom();
        assertThat(text(pom, "/project/properties/wms.tests.mode")).isEqualTo("SQL_PROOF_REQUIRED");
        assertThat(text(pom, policy() + "/phase")).isEqualTo("validate");
        assertThat("SQL_PROOF_REQUIRED")
                .doesNotMatch(
                        text(
                                pom,
                                policy()
                                        + "/configuration/rules/requireProperty[property='wms.tests.mode']/regex"));
    }

    @Test
    void selecaoPuraTemSomenteClassesComprovadasSemBanco() throws Exception {
        var pom = pom();
        assertThat(Arrays.asList(text(pom, "/project/properties/test").split(",")))
                .containsExactlyInAnyOrder(
                        "Sql01ObservationConfigTest",
                        "Sql01JdbcObservationTest",
                        "SqlServerConfigTest",
                        "SqlServerLocalITTest",
                        "SqlServerUpdateMappingTest",
                        "Sql01MavenPolicyTest");
        assertThat(
                        text(
                                pom,
                                "/project/build/plugins/plugin[artifactId='maven-surefire-plugin']/configuration/failIfNoTests"))
                .isEqualTo("true");
        assertThat(text(pom, profile("pure-no-db") + "/properties/wms.tests.mode"))
                .isEqualTo("PURE_NO_DATABASE");
    }

    @Test
    void seletoresDePersistenciaEWildcardRecusadosNoPerfilPuro() throws Exception {
        var pom = pom();
        var regex =
                text(pom, policy() + "/configuration/rules/requireProperty[property='test']/regex");
        assertThat(regex).isNotBlank();
        assertThat(regex).doesNotContain("${");
        assertThat(text(pom, "/project/properties/wms.tests.allowedSelection")).isEmpty();
        assertThat(text(pom, "/project/properties/test")).matches(regex);
        for (var proibido :
                Arrays.asList(
                        "CobrancaIntegrationTest",
                        "SqlServerLocalIT",
                        "*Test",
                        "!LoginBrowserTest",
                        "SqlServerConfigTest,CobrancaIntegrationTest",
                        "SqlServerConfigTest#qualquer")) {
            assertThat(proibido).doesNotMatch(regex);
        }
    }

    @Test
    void h2HistoricoPreservadoNaoPodeSerExecutadoNemDeclararAceiteSql() throws Exception {
        var pom = pom();
        var historico = profile("h2-historical");
        assertThat(text(pom, historico + "/properties/wms.tests.mode"))
                .isEqualTo("H2_HISTORICAL_ONLY");
        assertThat(text(pom, historico + "/activation")).isEmpty();
        var modos =
                text(
                        pom,
                        policy()
                                + "/configuration/rules/requireProperty[property='wms.tests.mode']/regex");
        assertThat("PURE_NO_DATABASE").matches(modos);
        assertThat("H2_HISTORICAL_ONLY").doesNotMatch(modos);
        for (var indevido :
                Arrays.asList("SQL_PROOF_REQUIRED", "SQLServer", "SQL_VALIDATED", "true")) {
            assertThat(indevido).doesNotMatch(modos);
        }
    }

    @Test
    void perfilH2RecusaMesmoComOverrideDeModoPuro() throws Exception {
        var pom = pom();
        var historico = profile("h2-historical");
        assertThat(text(pom, historico + "/properties/test")).isEmpty();
        var regra =
                historico
                        + "/build/plugins/plugin[artifactId='maven-enforcer-plugin']/executions/execution[id='recusar-h2-sql01']";
        assertThat(text(pom, regra + "/phase")).isEqualTo("validate");
        var regex = text(pom, regra + "/configuration/rules/requireProperty/regex");
        assertThat(regex).isNotBlank();
        for (var modo :
                Arrays.asList("PURE_NO_DATABASE", "H2_HISTORICAL_ONLY", "SQL_PROOF_REQUIRED")) {
            assertThat(modo).doesNotMatch(regex);
        }
    }

    @Test
    void itAntigoDeBancoVazioPermaneceRecusadoMesmoComPerfilPuro() throws Exception {
        var pom = pom();
        var regex =
                text(
                        pom,
                        profile("sqlserver-it")
                                + "/build/plugins/plugin[artifactId='maven-enforcer-plugin']/executions/execution[id='recusar-it-vazio-sql01']/configuration/rules/requireProperty/regex");
        assertThat(regex).isNotBlank();
        for (var modo :
                Arrays.asList("SQL_PROOF_REQUIRED", "PURE_NO_DATABASE", "H2_HISTORICAL_ONLY")) {
            assertThat(modo).doesNotMatch(regex);
        }
    }

    private static String policy() {
        return "/project/build/plugins/plugin[artifactId='maven-enforcer-plugin']/executions/execution[id='politica-testes-sql01']";
    }

    private static String profile(String id) {
        return "/project/profiles/profile[id='" + id + "']";
    }

    private static String text(Document document, String path) throws Exception {
        return XPathFactory.newInstance()
                .newXPath()
                .evaluate("string(" + path + ")", document)
                .trim();
    }

    private static Document pom() throws Exception {
        var factory = DocumentBuilderFactory.newInstance();
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        return factory.newDocumentBuilder().parse(Path.of("pom.xml").toFile());
    }
}
