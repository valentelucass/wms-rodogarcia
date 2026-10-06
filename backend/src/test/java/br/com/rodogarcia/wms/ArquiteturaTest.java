package br.com.rodogarcia.wms;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noFields;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class ArquiteturaTest {
    private static final JavaClasses CLASSES =
            new ClassFileImporter()
                    .importUrl(
                            WmsApplication.class
                                    .getProtectionDomain()
                                    .getCodeSource()
                                    .getLocation());

    @Test
    void controllersUsamServicosEDtosSemAcessarPersistencia() {
        noClasses()
                .that()
                .resideInAPackage("..controllers..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(
                        "..repositories..",
                        "..models..",
                        "jakarta.persistence..",
                        "org.springframework.jdbc..")
                .because("controllers delimitam HTTP; regras e persistencia passam pelos servicos")
                .check(CLASSES);
    }

    @Test
    void modelsNaoDependemDasCamadasDeAplicacao() {
        noClasses()
                .that()
                .resideInAPackage("..models..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(
                        "..controllers..",
                        "..services..",
                        "..repositories..",
                        "..dto..",
                        "..config..",
                        "jakarta.servlet..")
                .check(CLASSES);
    }

    @Test
    void repositoriesEDtosNaoInvertemAsDependencias() {
        noClasses()
                .that()
                .resideInAnyPackage("..repositories..", "..dto..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage("..controllers..", "..services..")
                .check(CLASSES);
        noClasses()
                .that()
                .resideInAPackage("..dto..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage("..repositories..", "jakarta.persistence..")
                .check(CLASSES);
    }

    @Test
    void servicesNaoDependemDeControllersOuServlets() {
        noClasses()
                .that()
                .resideInAPackage("..services..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage("..controllers..", "jakarta.servlet..")
                .check(CLASSES);
    }

    @Test
    void dependenciasSaoRecebidasPeloConstrutor() {
        org.assertj.core.api.Assertions.assertThat(CLASSES.contain(WmsApplication.class)).isTrue();
        org.assertj.core.api.Assertions.assertThat(
                        CLASSES.contain(
                                br.com.rodogarcia.wms.controllers.CargaInicialController.class))
                .isTrue();
        org.assertj.core.api.Assertions.assertThat(
                        CLASSES.contain(
                                br.com.rodogarcia.wms.services.ContagemEstoqueService.class))
                .isTrue();
        org.assertj.core.api.Assertions.assertThat(
                        CLASSES.contain(br.com.rodogarcia.wms.models.LinhaContingencia.class))
                .isTrue();
        org.assertj.core.api.Assertions.assertThat(
                        CLASSES.contain(br.com.rodogarcia.wms.services.EncerramentoService.class))
                .isTrue();
        org.assertj.core.api.Assertions.assertThat(CLASSES.contain(ArquiteturaTest.class))
                .isFalse();
        noFields().should().beAnnotatedWith(Autowired.class).check(CLASSES);
    }
}
