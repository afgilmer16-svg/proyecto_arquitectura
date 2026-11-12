package pe.abc.facturacion;

import java.io.ByteArrayInputStream;
import java.util.Iterator;
import java.util.Map;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

/** Utilidades de prueba: parsear un XML y consultarlo con XPath usando los prefijos de UBL. */
public final class XmlPrueba {

    private static final Map<String, String> ESPACIOS = Map.of(
            "i", "urn:oasis:names:specification:ubl:schema:xsd:Invoice-2",
            "cac", "urn:oasis:names:specification:ubl:schema:xsd:CommonAggregateComponents-2",
            "cbc", "urn:oasis:names:specification:ubl:schema:xsd:CommonBasicComponents-2");

    private final Document documento;
    private final XPath xpath = XPathFactory.newInstance().newXPath();

    public XmlPrueba(byte[] xml) {
        try {
            DocumentBuilderFactory fabrica = DocumentBuilderFactory.newInstance();
            fabrica.setNamespaceAware(true);
            this.documento = fabrica.newDocumentBuilder().parse(new ByteArrayInputStream(xml));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
        xpath.setNamespaceContext(new NamespaceContext() {
            @Override
            public String getNamespaceURI(String prefijo) {
                return ESPACIOS.getOrDefault(prefijo, XMLConstants.NULL_NS_URI);
            }

            @Override
            public String getPrefix(String uri) {
                return null;
            }

            @Override
            public Iterator<String> getPrefixes(String uri) {
                return null;
            }
        });
    }

    public Document documento() {
        return documento;
    }

    /** Valor de texto de la expresion, o cadena vacia si no existe. */
    public String texto(String expresion) {
        try {
            return (String) xpath.evaluate("string(" + expresion + ")", documento, XPathConstants.STRING);
        } catch (Exception e) {
            throw new IllegalArgumentException(expresion, e);
        }
    }

    public int cantidad(String expresion) {
        try {
            return ((NodeList) xpath.evaluate(expresion, documento, XPathConstants.NODESET)).getLength();
        } catch (Exception e) {
            throw new IllegalArgumentException(expresion, e);
        }
    }
}
