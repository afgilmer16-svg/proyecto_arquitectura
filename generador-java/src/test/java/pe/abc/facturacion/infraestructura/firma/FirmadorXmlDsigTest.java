// Autor: Gilmer Ayala Falcón · UNAS-FIIS · Arquitectura de Software · Grupo A
package pe.abc.facturacion.infraestructura.firma;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

import javax.xml.crypto.AlgorithmMethod;
import javax.xml.crypto.KeySelector;
import javax.xml.crypto.KeySelectorException;
import javax.xml.crypto.KeySelectorResult;
import javax.xml.crypto.XMLCryptoContext;
import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.dom.DOMValidateContext;
import javax.xml.crypto.dsig.keyinfo.KeyInfo;
import javax.xml.crypto.dsig.keyinfo.X509Data;
import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import pe.abc.facturacion.Ejemplos;
import pe.abc.facturacion.aplicacion.GenerarFacturaXmlService;
import pe.abc.facturacion.dominio.error.FirmaDigitalException;
import pe.abc.facturacion.infraestructura.ubl.GeneradorUblJaxbAdapter;
import pe.abc.facturacion.infraestructura.ubl.ValidadorXsd;

/**
 * Firma digital opcional: se firma con un certificado autofirmado creado en la prueba (con {@code keytool} del
 * JDK; nunca se versiona un .pfx real) y se verifica con la API estandar {@code javax.xml.crypto.dsig}. Ademas se
 * verifica en Java un XML firmado por el firmador de Python: ambos lenguajes producen firmas compatibles.
 */
class FirmadorXmlDsigTest {

    private static final String CLAVE = "clave-de-prueba";
    private static byte[] pfx;
    private static byte[] xmlSinFirmar;
    private static byte[] xmlFirmado;

    @BeforeAll
    static void preparar(@TempDir Path carpeta) throws Exception {
        Path almacen = carpeta.resolve("prueba.p12");
        Process proceso = new ProcessBuilder(
                Path.of(System.getProperty("java.home"), "bin", "keytool").toString(),
                "-genkeypair", "-alias", "prueba", "-keyalg", "RSA", "-keysize", "2048", "-validity", "30",
                "-storetype", "PKCS12", "-keystore", almacen.toString(), "-storepass", CLAVE,
                "-dname", "CN=ABC SOFTWARE CORP (PRUEBA)")
                .redirectErrorStream(true).start();
        proceso.getInputStream().readAllBytes();
        assertThat(proceso.waitFor(60, TimeUnit.SECONDS)).isTrue();
        assertThat(proceso.exitValue()).isZero();
        pfx = Files.readAllBytes(almacen);

        xmlSinFirmar = new GenerarFacturaXmlService(new GeneradorUblJaxbAdapter()).generar(Ejemplos.parcial(), 1);
        xmlFirmado = new FirmadorXmlDsigAdapter(pfx, CLAVE.toCharArray()).firmar(xmlSinFirmar);
    }

    // ------------------------------------------------------------------ resultado de la firma
    @Test
    void elXmlFirmadoSigueSiendoValidoContraElXsd() {
        new ValidadorXsd().validar(xmlFirmado);
    }

    @Test
    void laFirmaEsVerificableConLaApiDelJdk() throws Exception {
        assertThat(verificar(xmlFirmado)).isTrue();
    }

    @Test
    void ublExtensionsEsElPrimerHijoDeLaRaiz() throws Exception {
        Element raiz = documento(xmlFirmado).getDocumentElement();
        Element primero = primerHijoElemento(raiz);
        assertThat(primero.getNamespaceURI()).isEqualTo(FirmadorXmlDsigAdapter.NS_EXT);
        assertThat(primero.getLocalName()).isEqualTo("UBLExtensions");
        assertThat(raiz.getElementsByTagNameNS(FirmadorXmlDsigAdapter.NS_DS, "Signature").getLength()).isEqualTo(1);
    }

    @Test
    void elIdDeLaFirmaCoincideConCacSignature() throws Exception {
        Element raiz = documento(xmlFirmado).getDocumentElement();
        String uri = raiz.getElementsByTagNameNS(FirmadorXmlDsigAdapter.NS_CBC, "URI").item(0).getTextContent();
        Element firma = (Element) raiz.getElementsByTagNameNS(FirmadorXmlDsigAdapter.NS_DS, "Signature").item(0);
        assertThat(firma.getAttribute("Id")).isEqualTo(uri.substring(1));
    }

    @Test
    void usaLosAlgoritmosDeSunat() throws Exception {
        String texto = new String(xmlFirmado, StandardCharsets.UTF_8);
        assertThat(texto)
                .contains("http://www.w3.org/TR/2001/REC-xml-c14n-20010315")
                .contains("http://www.w3.org/2001/04/xmldsig-more#rsa-sha256")
                .contains("http://www.w3.org/2001/04/xmlenc#sha256")
                .contains("http://www.w3.org/2000/09/xmldsig#enveloped-signature")
                .contains("<ds:X509Certificate>");
    }

    @Test
    void firmarNoCambiaElRestoDelDocumento() throws Exception {
        Element sin = documento(xmlSinFirmar).getDocumentElement();
        Element con = documento(xmlFirmado).getDocumentElement();
        assertThat(con.getElementsByTagNameNS(FirmadorXmlDsigAdapter.NS_CAC, "InvoiceLine").getLength())
                .isEqualTo(sin.getElementsByTagNameNS(FirmadorXmlDsigAdapter.NS_CAC, "InvoiceLine").getLength());
        assertThat(con.getElementsByTagNameNS(FirmadorXmlDsigAdapter.NS_CBC, "PayableAmount").item(0).getTextContent())
                .isEqualTo(sin.getElementsByTagNameNS(FirmadorXmlDsigAdapter.NS_CBC, "PayableAmount").item(0).getTextContent());
    }

    // ------------------------------------------------------------------ alteraciones
    @Test
    void unImporteAlteradoInvalidaLaFirma() throws Exception {
        String texto = new String(xmlFirmado, StandardCharsets.UTF_8);
        String alterado = texto.replace("<cbc:PayableAmount currencyID=\"PEN\">", "<cbc:PayableAmount currencyID=\"PEN\">9");
        assertThat(alterado).isNotEqualTo(texto);
        assertThat(verificar(alterado.getBytes(StandardCharsets.UTF_8))).isFalse();
    }

    @Test
    void unaFirmaAlteradaSeDetecta() throws Exception {
        Document doc = documento(xmlFirmado);
        Element valor = (Element) doc.getElementsByTagNameNS(FirmadorXmlDsigAdapter.NS_DS, "SignatureValue").item(0);
        byte[] bytes = Base64.getMimeDecoder().decode(valor.getTextContent());
        bytes[0] ^= (byte) 0xFF;
        valor.setTextContent(Base64.getEncoder().encodeToString(bytes));
        assertThat(verificar(Ejemplos_texto(doc))).isFalse();
    }

    // ------------------------------------------------------------------ errores
    @Test
    void contrasenaIncorrecta() {
        assertThatThrownBy(() -> new FirmadorXmlDsigAdapter(pfx, "otra".toCharArray()))
                .isInstanceOf(FirmaDigitalException.class).hasMessageContaining("contrasena");
    }

    @Test
    void pfxInvalido() {
        assertThatThrownBy(() -> new FirmadorXmlDsigAdapter("no es un pfx".getBytes(StandardCharsets.UTF_8), CLAVE.toCharArray()))
                .isInstanceOf(FirmaDigitalException.class);
    }

    @Test
    void noSeFirmaDosVeces() {
        assertThatThrownBy(() -> new FirmadorXmlDsigAdapter(pfx, CLAVE.toCharArray()).firmar(xmlFirmado))
                .isInstanceOf(FirmaDigitalException.class).hasMessageContaining("ya contiene");
    }

    @Test
    void xmlMalFormado() {
        assertThatThrownBy(() -> new FirmadorXmlDsigAdapter(pfx, CLAVE.toCharArray())
                .firmar("<Invoice>".getBytes(StandardCharsets.UTF_8)))
                .isInstanceOf(FirmaDigitalException.class).hasMessageContaining("bien formado");
    }

    @Test
    void noAdmiteDoctype() {
        byte[] xxe = "<!DOCTYPE a [<!ENTITY x SYSTEM \"file:///etc/passwd\">]><a>&x;</a>".getBytes(StandardCharsets.UTF_8);
        assertThatThrownBy(() -> new FirmadorXmlDsigAdapter(pfx, CLAVE.toCharArray()).firmar(xxe))
                .isInstanceOf(FirmaDigitalException.class);
    }

    // ------------------------------------------------------------------ interoperabilidad con Python
    @Test
    void javaVerificaUnXmlFirmadoPorElFirmadorDePython() throws Exception {
        byte[] deFirmadoPython = Files.readAllBytes(Ejemplos.RAIZ.resolve(
                "specs/ejemplos/xml-firmado/F001-00000001-firmado-con-certificado-de-prueba.xml"));
        assertThat(verificar(deFirmadoPython)).isTrue();
    }

    // ------------------------------------------------------------------ utilidades
    private static Document documento(byte[] xml) throws Exception {
        DocumentBuilderFactory fabrica = DocumentBuilderFactory.newInstance();
        fabrica.setNamespaceAware(true);
        return fabrica.newDocumentBuilder().parse(new ByteArrayInputStream(xml));
    }

    private static Element primerHijoElemento(Element raiz) {
        for (var n = raiz.getFirstChild(); n != null; n = n.getNextSibling()) {
            if (n instanceof Element e) {
                return e;
            }
        }
        throw new IllegalStateException("la raiz no tiene elementos");
    }

    private static byte[] Ejemplos_texto(Document doc) throws Exception {
        var t = javax.xml.transform.TransformerFactory.newInstance().newTransformer();
        var salida = new java.io.ByteArrayOutputStream();
        t.setOutputProperty(javax.xml.transform.OutputKeys.OMIT_XML_DECLARATION, "yes");
        t.transform(new javax.xml.transform.dom.DOMSource(doc), new javax.xml.transform.stream.StreamResult(salida));
        return salida.toByteArray();
    }

    /** Verifica la firma con la clave publica del certificado que viaja en KeyInfo. */
    private static boolean verificar(byte[] xml) throws Exception {
        Document doc = documento(xml);
        NodeList firmas = doc.getElementsByTagNameNS(XMLSignature.XMLNS, "Signature");
        assertThat(firmas.getLength()).isEqualTo(1);
        DOMValidateContext contexto = new DOMValidateContext(new SelectorX509(), firmas.item(0));
        XMLSignature firma = XMLSignatureFactory.getInstance("DOM").unmarshalXMLSignature(contexto);
        return firma.validate(contexto);
    }

    private static final class SelectorX509 extends KeySelector {
        @Override
        public KeySelectorResult select(KeyInfo keyInfo, Purpose proposito, AlgorithmMethod metodo,
                                        XMLCryptoContext contexto) throws KeySelectorException {
            for (Object estructura : keyInfo.getContent()) {
                if (estructura instanceof X509Data datos) {
                    for (Object o : datos.getContent()) {
                        if (o instanceof X509Certificate cert) {
                            PublicKey clave = cert.getPublicKey();
                            return () -> clave;
                        }
                    }
                }
            }
            throw new KeySelectorException("KeyInfo sin certificado X.509");
        }
    }
}
