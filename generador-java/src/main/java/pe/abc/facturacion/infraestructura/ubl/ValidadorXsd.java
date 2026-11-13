// Autor: Gilmer Ayala Falcón · UNAS-FIIS · Arquitectura de Software · Grupo A
package pe.abc.facturacion.infraestructura.ubl;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import pe.abc.facturacion.dominio.error.EsquemaXsdIncompletoException;
import pe.abc.facturacion.dominio.error.XmlNoConformeXsdException;

/**
 * Valida un XML contra {@code UBL-Invoice-2.1.xsd} (OASIS UBL 2.1 OS, sin modificar).
 *
 * <p>Los XSD se empaquetan en el jar con la estructura oficial ({@code ubl/maindoc} y {@code ubl/common}); se
 * leen desde la carpeta unica {@code docs/referencias/ubl} del repositorio durante la compilacion. Nunca se
 * accede a la red.
 */
public class ValidadorXsd {

    static final String RECURSO_INVOICE = "/ubl/maindoc/UBL-Invoice-2.1.xsd";

    private final Schema esquema;

    public ValidadorXsd() {
        this(ValidadorXsd.class.getResource(RECURSO_INVOICE));
    }

    ValidadorXsd(URL xsd) {
        if (xsd == null) {
            throw new EsquemaXsdIncompletoException("No se encuentra " + RECURSO_INVOICE
                    + " en el classpath: ejecute 'mvn generate-resources'", null);
        }
        try {
            SchemaFactory fabrica = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            fabrica.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            fabrica.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "file,jar");
            fabrica.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "file,jar");
            this.esquema = fabrica.newSchema(xsd);
        } catch (SAXException e) {
            throw new EsquemaXsdIncompletoException(
                    "Falta un archivo del esquema UBL 2.1 y el XSD no se puede compilar: " + e.getMessage(), e);
        }
    }

    /** @throws XmlNoConformeXsdException con todos los errores del esquema */
    public void validar(byte[] xml) {
        List<String> errores = new ArrayList<>();
        Validator validador = esquema.newValidator();
        validador.setErrorHandler(new ErrorHandler() {
            @Override
            public void warning(SAXParseException e) {
                // las advertencias no invalidan el documento
            }

            @Override
            public void error(SAXParseException e) {
                errores.add("línea " + e.getLineNumber() + ": " + e.getMessage());
            }

            @Override
            public void fatalError(SAXParseException e) {
                errores.add("línea " + e.getLineNumber() + ": " + e.getMessage());
            }
        });
        try {
            validador.validate(new StreamSource(new ByteArrayInputStream(xml)));
        } catch (SAXException e) {
            if (errores.isEmpty()) {
                errores.add(e.getMessage());
            }
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        if (!errores.isEmpty()) {
            throw new XmlNoConformeXsdException(errores);
        }
    }
}
