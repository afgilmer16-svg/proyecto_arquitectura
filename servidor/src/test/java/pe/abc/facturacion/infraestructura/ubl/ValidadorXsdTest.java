package pe.abc.facturacion.infraestructura.ubl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URL;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import pe.abc.facturacion.Ejemplos;
import pe.abc.facturacion.aplicacion.GenerarFacturaXmlService;
import pe.abc.facturacion.dominio.error.EsquemaXsdIncompletoException;
import pe.abc.facturacion.dominio.error.XmlNoConformeXsdException;

/** El XSD oficial detecta XML alterados: demuestra que la validacion previa a la entrega es real. */
class ValidadorXsdTest {

    private static final ValidadorXsd VALIDADOR = new ValidadorXsd();
    private static String xml;

    @BeforeAll
    static void generar() {
        xml = new String(new GenerarFacturaXmlService(new GeneradorUblJaxbAdapter()).generar(Ejemplos.parcial(), 1),
                StandardCharsets.UTF_8);
    }

    private static void rechaza(String modificado, String fragmentoDelError) {
        assertThatThrownBy(() -> VALIDADOR.validar(modificado.getBytes(StandardCharsets.UTF_8)))
                .isInstanceOf(XmlNoConformeXsdException.class).hasMessageContaining(fragmentoDelError);
    }

    @Test
    void aceptaElXmlGenerado() {
        assertThatCode(() -> VALIDADOR.validar(xml.getBytes(StandardCharsets.UTF_8))).doesNotThrowAnyException();
    }

    @Test
    void detectaUnElementoObligatorioFaltante() {
        rechaza(xml.replaceAll("(?s)<cbc:ID>F001-00000001</cbc:ID>", ""), "ID");
    }

    @Test
    void detectaUnElementoInexistenteEnElXsd() {
        rechaza(xml.replace("<cbc:UBLVersionID>", "<cbc:EtiquetaInventada>x</cbc:EtiquetaInventada><cbc:UBLVersionID>"),
                "EtiquetaInventada");
    }

    @Test
    void detectaElementosFueraDeOrden() {
        String sinVersion = xml.replace("<cbc:UBLVersionID>2.1</cbc:UBLVersionID>", "");
        rechaza(sinVersion.replace("</cbc:LineCountNumeric>", "</cbc:LineCountNumeric><cbc:UBLVersionID>2.1</cbc:UBLVersionID>"),
                "UBLVersionID");
    }

    @Test
    void detectaUnTipoInvalido() {
        rechaza(xml.replace("<cbc:IssueDate>2026-11-11</cbc:IssueDate>", "<cbc:IssueDate>no-es-fecha</cbc:IssueDate>"),
                "IssueDate");
    }

    @Test
    void detectaUnAtributoObligatorioFaltante() {
        rechaza(xml.replaceFirst(" currencyID=\"PEN\"", ""), "currencyID");
    }

    @Test
    void detectaXmlMalFormado() {
        rechaza("<Invoice>", "Invoice");
    }

    @Test
    void informaTodosLosErroresDelEsquema() {
        String roto = xml.replace("<cbc:IssueDate>2026-11-11</cbc:IssueDate>", "<cbc:IssueDate>x</cbc:IssueDate>")
                .replaceFirst(" currencyID=\"PEN\"", "");
        assertThatThrownBy(() -> VALIDADOR.validar(roto.getBytes(StandardCharsets.UTF_8)))
                .isInstanceOfSatisfying(XmlNoConformeXsdException.class, e -> assertThat(e.errores()).hasSizeGreaterThan(1));
    }

    @Test
    void siFaltaElEsquemaSeIdentificaElProblema() {
        assertThatThrownBy(() -> new ValidadorXsd((URL) null)).isInstanceOf(EsquemaXsdIncompletoException.class)
                .hasMessageContaining("UBL-Invoice-2.1.xsd");
    }

    @Test
    void siUnXsdNoCompilaSeInformaQueFaltaUnArchivo() throws Exception {
        java.nio.file.Path dir = java.nio.file.Files.createTempDirectory("xsd");
        java.nio.file.Path raiz = dir.resolve("UBL-Invoice-2.1.xsd");
        java.nio.file.Files.writeString(raiz, "<xsd:schema xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\">"
                + "<xsd:include schemaLocation=\"no-existe.xsd\"/>"
                + "<xsd:element name=\"Invoice\" type=\"TipoQueVivia-En-El-Archivo-Faltante\"/></xsd:schema>");
        assertThatThrownBy(() -> new ValidadorXsd(raiz.toUri().toURL()))
                .isInstanceOf(EsquemaXsdIncompletoException.class).hasMessageContaining("no se puede compilar");
    }
}
