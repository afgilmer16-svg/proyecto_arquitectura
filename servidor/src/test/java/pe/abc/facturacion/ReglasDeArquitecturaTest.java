package pe.abc.facturacion;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import org.junit.jupiter.api.Test;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;

/**
 * Reglas de arquitectura (hexagonal, feature 0003): el dominio no conoce nada de afuera, la aplicacion solo
 * conoce al dominio y a sus puertos, y solo la infraestructura toca JAXB, Jackson, JSON Schema o el XSD.
 */
class ReglasDeArquitecturaTest {

    private static final JavaClasses CLASES = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("pe.abc.facturacion");

    private static final String DOMINIO = "pe.abc.facturacion.dominio..";
    private static final String APLICACION = "pe.abc.facturacion.aplicacion..";
    private static final String INFRAESTRUCTURA = "pe.abc.facturacion.infraestructura..";

    @Test
    void elDominioNoDependeDeAplicacionNiDeInfraestructura() {
        noClasses().that().resideInAPackage(DOMINIO)
                .should().dependOnClassesThat().resideInAnyPackage(APLICACION, INFRAESTRUCTURA)
                .check(CLASES);
    }

    @Test
    void laAplicacionNoDependeDeLaInfraestructura() {
        noClasses().that().resideInAPackage(APLICACION)
                .should().dependOnClassesThat().resideInAPackage(INFRAESTRUCTURA)
                .check(CLASES);
    }

    @Test
    void elDominioYLaAplicacionNoUsanLibreriasXmlNiJson() {
        noClasses().that().resideInAnyPackage(DOMINIO, APLICACION)
                .should().dependOnClassesThat().resideInAnyPackage(
                        "jakarta.xml.bind..", "javax.xml..", "org.w3c..", "org.xml..", "org.glassfish..",
                        "com.fasterxml..", "com.networknt..", "oasis..", "un.unece..", "org.etsi..", "org.w3..")
                .check(CLASES);
    }

    @Test
    void soloElAdaptadorUblUsaLasClasesGeneradasDesdeElXsd() {
        noClasses().that().resideOutsideOfPackage("pe.abc.facturacion.infraestructura.ubl..")
                .should().dependOnClassesThat().resideInAnyPackage("oasis..", "un.unece..", "org.etsi..", "org.w3..")
                .check(CLASES);
    }

    @Test
    void elDominioNoUsaFrameworksNiPuntoFlotante() {
        noClasses().that().resideInAPackage(DOMINIO)
                .should().dependOnClassesThat().haveFullyQualifiedName("java.lang.Double")
                .orShould().dependOnClassesThat().haveFullyQualifiedName("java.lang.Float")
                .check(CLASES);
    }

    @Test
    void sinCiclosEntreLosPaquetesDelDominio() {
        slices().matching("pe.abc.facturacion.dominio.(*)..").should().beFreeOfCycles().check(CLASES);
    }

    @Test
    void elAdaptadorImplementaElPuertoDeSalida() {
        classes().that().haveSimpleName("GeneradorUblJaxbAdapter")
                .should().implement(pe.abc.facturacion.aplicacion.puerto.GeneradorXmlUblPort.class)
                .check(CLASES);
    }
}
