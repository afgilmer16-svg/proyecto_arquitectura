// Autor: Gilmer Ayala Falcón · UNAS-FIIS · Arquitectura de Software · Grupo A
package pe.abc.facturacion.infraestructura.ubl;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import pe.abc.facturacion.Ejemplos;
import pe.abc.facturacion.aplicacion.GenerarFacturaXmlService;

/**
 * El firmador del OSE inserta {@code ext:UBLExtensions} con {@code ds:Signature}. Si la raiz no declara esos
 * prefijos, el OSE responde con el error 0306 ("The prefix "ext" ... is not bound"). Estas pruebas lo impiden.
 */
class PrefijosDelFirmadorTest {

    private static final String BLOQUE_DE_FIRMA = "<ext:UBLExtensions><ext:UBLExtension><ext:ExtensionContent>"
            + "<ds:Signature Id=\"SignatureSP\"/></ext:ExtensionContent></ext:UBLExtension></ext:UBLExtensions>";

    private final GenerarFacturaXmlService servicio = new GenerarFacturaXmlService(new GeneradorUblJaxbAdapter());

    static List<Path> ejemplos() {
        List<Path> lista = new ArrayList<>(Ejemplos.listar(Ejemplos.VALIDOS));
        lista.add(0, Ejemplos.EJEMPLO_PARCIAL);
        return lista;
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("ejemplos")
    void laRaizDeclaraLosPrefijosDeLaGuiaXmlDeSunat(Path ejemplo) {
        String xml = new String(servicio.generar(Ejemplos.json(ejemplo), 1), StandardCharsets.UTF_8);
        String raiz = xml.substring(xml.indexOf("<Invoice"), xml.indexOf('>', xml.indexOf("<Invoice")));
        for (String prefijo : List.of("cac", "cbc", "ext", "ds", "ccts", "qdt", "udt", "xsi")) {
            assertThat(raiz).as(prefijo).contains("xmlns:" + prefijo + "=");
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("ejemplos")
    void seInsertaElBloqueDeFirmaSinElError0306(Path ejemplo) throws Exception {
        String xml = new String(servicio.generar(Ejemplos.json(ejemplo), 1), StandardCharsets.UTF_8);
        String conFirma = xml.replace("<cbc:UBLVersionID>", BLOQUE_DE_FIRMA + "<cbc:UBLVersionID>");
        assertThat(conFirma).isNotEqualTo(xml);
        DocumentBuilderFactory fabrica = DocumentBuilderFactory.newInstance();
        fabrica.setNamespaceAware(true);
        var doc = fabrica.newDocumentBuilder().parse(new ByteArrayInputStream(conFirma.getBytes(StandardCharsets.UTF_8)));
        assertThat(doc.getElementsByTagNameNS(
                "urn:oasis:names:specification:ubl:schema:xsd:CommonExtensionComponents-2", "UBLExtensions").getLength())
                .isEqualTo(1);
    }
}
