// Autor: Gilmer Ayala Falcón · UNAS-FIIS · Arquitectura de Software · Grupo A
package pe.abc.facturacion.infraestructura.firma;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

import javax.xml.XMLConstants;
import javax.xml.crypto.dsig.CanonicalizationMethod;
import javax.xml.crypto.dsig.DigestMethod;
import javax.xml.crypto.dsig.Reference;
import javax.xml.crypto.dsig.SignedInfo;
import javax.xml.crypto.dsig.Transform;
import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.dom.DOMSignContext;
import javax.xml.crypto.dsig.keyinfo.KeyInfo;
import javax.xml.crypto.dsig.keyinfo.KeyInfoFactory;
import javax.xml.crypto.dsig.spec.C14NMethodParameterSpec;
import javax.xml.crypto.dsig.spec.TransformParameterSpec;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import pe.abc.facturacion.aplicacion.puerto.FirmaDigitalPort;
import pe.abc.facturacion.dominio.error.FirmaDigitalException;

/**
 * Firma XMLDSig <em>enveloped</em> (C14N 1.0, SHA-256, RSA-SHA256) con la API estandar del JDK
 * ({@code javax.xml.crypto.dsig}); no agrega dependencias.
 *
 * <p>La firma queda en {@code ext:UBLExtensions/ext:UBLExtension/ext:ExtensionContent} como primer hijo de la
 * raiz, con {@code Id} igual al fragmento que {@code cac:Signature} ya referencia. La clave privada se lee del
 * {@code .pfx}/{@code .p12} y no se guarda ni se escribe. Mismo algoritmo y misma forma que el firmador de
 * Python ({@code facturacion_xml/firmador.py}); las pruebas verifican en Java un XML firmado en Python.
 */
public final class FirmadorXmlDsigAdapter implements FirmaDigitalPort {

    public static final String NS_DS = "http://www.w3.org/2000/09/xmldsig#";
    public static final String NS_EXT = "urn:oasis:names:specification:ubl:schema:xsd:CommonExtensionComponents-2";
    public static final String NS_CAC = "urn:oasis:names:specification:ubl:schema:xsd:CommonAggregateComponents-2";
    public static final String NS_CBC = "urn:oasis:names:specification:ubl:schema:xsd:CommonBasicComponents-2";
    static final String RSA_SHA256 = "http://www.w3.org/2001/04/xmldsig-more#rsa-sha256";
    static final String ID_FIRMA_POR_DEFECTO = "SignatureSP";

    private final PrivateKey clavePrivada;
    private final X509Certificate certificado;

    /**
     * @param pfx   contenido del archivo {@code .pfx}/{@code .p12}
     * @param clave contrasena del archivo
     * @throws FirmaDigitalException si el archivo o la contrasena no son validos
     */
    public FirmadorXmlDsigAdapter(byte[] pfx, char[] clave) {
        try {
            KeyStore almacen = KeyStore.getInstance("PKCS12");
            almacen.load(new ByteArrayInputStream(pfx), clave);
            PrivateKey llave = null;
            X509Certificate cert = null;
            for (Enumeration<String> alias = almacen.aliases(); alias.hasMoreElements() && llave == null;) {
                String a = alias.nextElement();
                if (almacen.isKeyEntry(a)) {
                    llave = (PrivateKey) almacen.getKey(a, clave);
                    cert = (X509Certificate) almacen.getCertificate(a);
                }
            }
            if (llave == null || cert == null) {
                throw new FirmaDigitalException("El archivo .pfx/.p12 no contiene la clave privada y el certificado.");
            }
            if (!"RSA".equals(llave.getAlgorithm())) {
                throw new FirmaDigitalException(
                        "La firma de comprobantes SUNAT usa RSA; el certificado entregado usa otro algoritmo.");
            }
            this.clavePrivada = llave;
            this.certificado = cert;
        } catch (FirmaDigitalException e) {
            throw e;
        } catch (Exception e) {
            throw new FirmaDigitalException(
                    "No se pudo abrir el certificado: archivo .pfx/.p12 invalido o contrasena incorrecta.", e);
        }
    }

    @Override
    public byte[] firmar(byte[] xml) {
        try {
            Document doc = leer(xml);
            Element raiz = doc.getDocumentElement();
            if (doc.getElementsByTagNameNS(NS_DS, "Signature").getLength() > 0) {
                throw new FirmaDigitalException("El XML ya contiene una firma digital.");
            }
            Element contenido = crearExtensionContent(doc, raiz);

            XMLSignatureFactory fabrica = XMLSignatureFactory.getInstance("DOM");
            Reference referencia = fabrica.newReference("",
                    fabrica.newDigestMethod(DigestMethod.SHA256, null),
                    List.of(fabrica.newTransform(Transform.ENVELOPED, (TransformParameterSpec) null)),
                    null, null);
            SignedInfo info = fabrica.newSignedInfo(
                    fabrica.newCanonicalizationMethod(CanonicalizationMethod.INCLUSIVE, (C14NMethodParameterSpec) null),
                    fabrica.newSignatureMethod(RSA_SHA256, null),
                    Collections.singletonList(referencia));
            KeyInfoFactory fabricaKeyInfo = fabrica.getKeyInfoFactory();
            KeyInfo keyInfo = fabricaKeyInfo.newKeyInfo(
                    List.of(fabricaKeyInfo.newX509Data(List.of(certificado))));

            DOMSignContext contexto = new DOMSignContext(clavePrivada, contenido);
            contexto.setDefaultNamespacePrefix("ds");
            fabrica.newXMLSignature(info, keyInfo, null, idDeFirma(raiz), null).sign(contexto);
            return escribir(doc);
        } catch (FirmaDigitalException e) {
            throw e;
        } catch (Exception e) {
            throw new FirmaDigitalException("No se pudo firmar el XML: " + e.getMessage(), e);
        }
    }

    // ---------------------------------------------------------------- utilidades
    private static Document leer(byte[] xml) {
        try {
            DocumentBuilderFactory fabrica = DocumentBuilderFactory.newInstance();
            fabrica.setNamespaceAware(true);
            fabrica.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            fabrica.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            return fabrica.newDocumentBuilder().parse(new ByteArrayInputStream(xml));
        } catch (Exception e) {
            throw new FirmaDigitalException("El XML a firmar no esta bien formado: " + e.getMessage(), e);
        }
    }

    /** Crea {@code ext:UBLExtensions} como primer hijo (orden de {@code InvoiceType}) y devuelve {@code ExtensionContent}. */
    private static Element crearExtensionContent(Document doc, Element raiz) {
        for (Node hijo = raiz.getFirstChild(); hijo != null;) {
            Node siguiente = hijo.getNextSibling();
            if (hijo.getNodeType() == Node.COMMENT_NODE && hijo.getNodeValue().contains("FIRMAR AQUÍ")) {
                raiz.removeChild(hijo); // ya no hace falta: el XML queda firmado
            }
            hijo = siguiente;
        }
        Element extensiones = doc.createElementNS(NS_EXT, "ext:UBLExtensions");
        Element extension = doc.createElementNS(NS_EXT, "ext:UBLExtension");
        Element contenido = doc.createElementNS(NS_EXT, "ext:ExtensionContent");
        extension.appendChild(contenido);
        extensiones.appendChild(extension);
        Node primero = raiz.getFirstChild();
        raiz.insertBefore(extensiones, primero);
        raiz.insertBefore(doc.createTextNode("\n  "), primero);
        return contenido;
    }

    /** Id de la firma: el fragmento que {@code cac:Signature} ya referencia ({@code #SignatureSP<RUC>}). */
    private static String idDeFirma(Element raiz) {
        NodeList firmas = raiz.getElementsByTagNameNS(NS_CAC, "Signature");
        if (firmas.getLength() > 0) {
            NodeList uris = ((Element) firmas.item(0)).getElementsByTagNameNS(NS_CBC, "URI");
            if (uris.getLength() > 0) {
                String uri = uris.item(0).getTextContent();
                if (uri != null && uri.startsWith("#") && uri.length() > 1) {
                    return uri.substring(1);
                }
            }
        }
        return ID_FIRMA_POR_DEFECTO;
    }

    private static byte[] escribir(Document doc) throws Exception {
        Transformer t = TransformerFactory.newInstance().newTransformer();
        t.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        t.setOutputProperty(OutputKeys.INDENT, "no");
        t.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        salida.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        t.transform(new DOMSource(doc), new StreamResult(salida));
        return salida.toByteArray();
    }
}
