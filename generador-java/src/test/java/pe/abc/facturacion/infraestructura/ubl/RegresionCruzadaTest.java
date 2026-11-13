// Autor: Gilmer Ayala Falcón · UNAS-FIIS · Arquitectura de Software · Grupo A
package pe.abc.facturacion.infraestructura.ubl;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.builder.Input;
import org.xmlunit.diff.Diff;

import pe.abc.facturacion.Ejemplos;
import pe.abc.facturacion.aplicacion.GenerarFacturaXmlService;

/**
 * Regresion cruzada: los XML de {@code specs/ejemplos/xml-referencia} los genero el prototipo en Python a partir
 * de los mismos JSON. El adaptador Java (JAXB) debe producir el mismo documento: mismos elementos, en el mismo
 * orden, con los mismos atributos y valores. Solo se ignoran los espacios entre etiquetas.
 */
class RegresionCruzadaTest {

    private final GenerarFacturaXmlService servicio = new GenerarFacturaXmlService(new GeneradorUblJaxbAdapter());

    @TestFactory
    List<DynamicTest> elDocumentoJavaEsIgualAlDeReferencia() throws IOException {
        List<Path> entradas = new ArrayList<>(Ejemplos.listar(Ejemplos.VALIDOS));
        entradas.add(0, Ejemplos.EJEMPLO_PARCIAL);
        List<DynamicTest> pruebas = new ArrayList<>();
        for (Path json : entradas) {
            String nombre = json.equals(Ejemplos.EJEMPLO_PARCIAL) ? "00-factura-abarrotes" : Ejemplos.nombre(json);
            pruebas.add(DynamicTest.dynamicTest(nombre, () -> {
                byte[] java = servicio.generar(Ejemplos.json(json), 1);
                byte[] referencia = Files.readAllBytes(Ejemplos.REFERENCIA_XML.resolve(nombre + ".xml"));
                Diff diff = DiffBuilder.compare(Input.fromByteArray(referencia))
                        .withTest(Input.fromByteArray(java))
                        .ignoreWhitespace()
                        .checkForSimilar()
                        .build();
                assertThat(diff.hasDifferences()).as(diff.toString()).isFalse();
            }));
        }
        return pruebas;
    }

    @TestFactory
    Stream<DynamicTest> hayUnaReferenciaPorCadaEjemplo() {
        return Stream.of(DynamicTest.dynamicTest("referencias completas", () -> {
            for (Path json : Ejemplos.listar(Ejemplos.VALIDOS)) {
                assertThat(Ejemplos.REFERENCIA_XML.resolve(Ejemplos.nombre(json) + ".xml")).exists();
            }
        }));
    }

    @TestFactory
    Stream<DynamicTest> laGeneracionEsDeterminista() {
        return Stream.of(DynamicTest.dynamicTest("mismo JSON, mismo XML", () ->
                assertThat(servicio.generar(Ejemplos.parcial(), 5)).isEqualTo(servicio.generar(Ejemplos.parcial(), 5))));
    }
}
