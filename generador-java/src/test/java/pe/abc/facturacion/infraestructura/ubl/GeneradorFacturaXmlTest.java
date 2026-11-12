package pe.abc.facturacion.infraestructura.ubl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import pe.abc.facturacion.Ejemplos;
import pe.abc.facturacion.XmlPrueba;
import pe.abc.facturacion.aplicacion.GenerarFacturaXmlService;
import pe.abc.facturacion.dominio.catalogo.RegimenIgv;
import pe.abc.facturacion.dominio.error.DatosFacturaInvalidosException;

/** Pruebas unitarias de generacion: el XML sale, es valido contra el XSD y cada dato esta en su etiqueta. */
class GeneradorFacturaXmlTest {

    private static GenerarFacturaXmlService servicio;
    private static byte[] xml;
    private static XmlPrueba x;

    @BeforeAll
    static void generar() {
        servicio = new GenerarFacturaXmlService(new GeneradorUblJaxbAdapter());
        xml = servicio.generar(Ejemplos.parcial(), 1);
        x = new XmlPrueba(xml);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> copiaDelEjemplo() {
        Map<String, Object> copia = new HashMap<>(Ejemplos.parcial());
        copia.put("items", new java.util.ArrayList<>(((List<Object>) copia.get("items"))));
        return copia;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> unaLinea(String afectacion, String valor) {
        Map<String, Object> datos = copiaDelEjemplo();
        Map<String, Object> item = new HashMap<>((Map<String, Object>) ((List<Object>) datos.get("items")).get(0));
        item.put("tipo_afectacion_igv", afectacion);
        item.put("valor_unitario", valor);
        item.put("cantidad", "2");
        datos.put("items", List.of(item));
        return datos;
    }

    // ------------------------------------------------------------ generacion
    @Test
    void generaBytesUtf8ConDeclaracionYRaizInvoice() {
        String texto = new String(xml, StandardCharsets.UTF_8);
        assertThat(texto).startsWith("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        assertThat(x.documento().getDocumentElement().getLocalName()).isEqualTo("Invoice");
        assertThat(x.documento().getDocumentElement().getNamespaceURI())
                .isEqualTo("urn:oasis:names:specification:ubl:schema:xsd:Invoice-2");
    }

    @Test
    void preservaLaEnieYLasTildes() {
        assertThat(x.texto("/i:Invoice/cac:InvoiceLine[1]/cac:Item/cbc:Description"))
                .isEqualTo("Arroz Extra Costeño Saco 50 kg");
        assertThat(x.texto("/i:Invoice/cac:AccountingSupplierParty/cac:Party/cac:PartyLegalEntity/cac:RegistrationAddress/cac:AddressLine/cbc:Line"))
                .contains("Tingo María");
    }

    // ------------------------------------------------------------- cabecera
    @Test
    void cabeceraUblYSunat() {
        assertThat(x.texto("/i:Invoice/cbc:UBLVersionID")).isEqualTo("2.1");
        assertThat(x.texto("/i:Invoice/cbc:CustomizationID")).isEqualTo("2.0");
        assertThat(x.texto("/i:Invoice/cbc:ProfileID")).isEqualTo("0101");
        assertThat(x.texto("/i:Invoice/cbc:ID")).isEqualTo("F001-00000001");
        assertThat(x.texto("/i:Invoice/cbc:IssueDate")).isEqualTo("2026-11-11");
        assertThat(x.texto("/i:Invoice/cbc:IssueTime")).isEqualTo("10:30:00");
        assertThat(x.texto("/i:Invoice/cbc:InvoiceTypeCode")).isEqualTo("01");
        assertThat(x.texto("/i:Invoice/cbc:InvoiceTypeCode/@listID")).isEqualTo("0101");
        assertThat(x.texto("/i:Invoice/cbc:DocumentCurrencyCode")).isEqualTo("PEN");
        assertThat(x.texto("/i:Invoice/cbc:LineCountNumeric")).isEqualTo("7");
    }

    @Test
    void leyenda1000ConElImporteEnLetras() {
        assertThat(x.texto("/i:Invoice/cbc:Note[@languageLocaleID='1000']"))
                .isEqualTo("DOS MIL TRESCIENTOS CUARENTA Y DOS CON 35/100 SOLES");
    }

    @Test
    void elDocumentoNoGeneraElementosDeUbl20NiLaExtensionDeFirma() {
        assertThat(x.cantidad("//*[local-name()='AdditionalMonetaryTotal']")).isZero();
        assertThat(x.cantidad("/i:Invoice/*[local-name()='UBLExtensions']")).isZero();
        assertThat(x.cantidad("/i:Invoice/cac:Signature")).isEqualTo(1);
    }

    @Test
    void elOrdenDeLaCabeceraSigueLaSecuenciaDeInvoiceType() {
        List<String> orden = List.of("UBLVersionID", "CustomizationID", "ProfileID", "ID", "IssueDate", "IssueTime",
                "InvoiceTypeCode", "Note", "DocumentCurrencyCode", "LineCountNumeric", "Signature",
                "AccountingSupplierParty", "AccountingCustomerParty", "PaymentTerms", "TaxTotal",
                "LegalMonetaryTotal", "InvoiceLine");
        var hijos = x.documento().getDocumentElement().getChildNodes();
        java.util.List<String> reales = new java.util.ArrayList<>();
        for (int i = 0; i < hijos.getLength(); i++) {
            if (hijos.item(i).getNodeType() == org.w3c.dom.Node.ELEMENT_NODE
                    && (reales.isEmpty() || !reales.get(reales.size() - 1).equals(hijos.item(i).getLocalName()))) {
                reales.add(hijos.item(i).getLocalName());
            }
        }
        assertThat(reales).isEqualTo(orden);
    }

    // -------------------------------------------------------- emisor/receptor
    @Test
    void emisorYReceptor() {
        String emisor = "/i:Invoice/cac:AccountingSupplierParty/cac:Party";
        assertThat(x.texto(emisor + "/cac:PartyIdentification/cbc:ID")).isEqualTo("20526018151");
        assertThat(x.texto(emisor + "/cac:PartyIdentification/cbc:ID/@schemeID")).isEqualTo("6");
        assertThat(x.texto(emisor + "/cac:PartyName/cbc:Name")).isEqualTo("DISTRIBUIDORA DE ABARROTES ABC");
        assertThat(x.texto(emisor + "/cac:PartyLegalEntity/cbc:RegistrationName")).isEqualTo("ABC SOFTWARE CORP S.A.C.");
        assertThat(x.texto(emisor + "/cac:PartyLegalEntity/cac:RegistrationAddress/cbc:ID")).isEqualTo("100601");
        assertThat(x.texto(emisor + "/cac:PartyLegalEntity/cac:RegistrationAddress/cbc:AddressTypeCode")).isEqualTo("0000");
        assertThat(x.texto(emisor + "/cac:PartyLegalEntity/cac:RegistrationAddress/cac:Country/cbc:IdentificationCode")).isEqualTo("PE");

        String receptor = "/i:Invoice/cac:AccountingCustomerParty/cac:Party";
        assertThat(x.texto(receptor + "/cac:PartyIdentification/cbc:ID")).isEqualTo("20555555556");
        assertThat(x.texto(receptor + "/cac:PartyLegalEntity/cbc:RegistrationName")).isEqualTo("COMERCIALIZADORA SAN JUAN S.A.C.");
    }

    // ------------------------------------------------------------- importes
    @Test
    void totalesDeCabeceraYCuadre() {
        assertThat(x.texto("/i:Invoice/cac:LegalMonetaryTotal/cbc:LineExtensionAmount")).isEqualTo("1955.50");
        assertThat(x.texto("/i:Invoice/cac:LegalMonetaryTotal/cbc:TaxInclusiveAmount")).isEqualTo("2342.35");
        assertThat(x.texto("/i:Invoice/cac:LegalMonetaryTotal/cbc:PayableAmount")).isEqualTo("2342.35");
        assertThat(x.texto("/i:Invoice/cac:TaxTotal/cbc:TaxAmount")).isEqualTo("386.85");
        assertThat(x.texto("/i:Invoice/cac:LegalMonetaryTotal/cbc:PayableAmount/@currencyID")).isEqualTo("PEN");
    }

    @Test
    void unTaxSubtotalPorCadaTributoPresenteEnLaCabecera() {
        String sub = "/i:Invoice/cac:TaxTotal/cac:TaxSubtotal";
        assertThat(x.cantidad(sub)).isEqualTo(6);
        assertThat(x.texto(sub + "[cac:TaxCategory/cac:TaxScheme/cbc:ID='2000']/cbc:TaxAmount")).isEqualTo("32.40");
        assertThat(x.texto(sub + "[cac:TaxCategory/cac:TaxScheme/cbc:ID='1000']/cbc:TaxAmount")).isEqualTo("349.45");
        assertThat(x.texto(sub + "[cac:TaxCategory/cac:TaxScheme/cbc:ID='7152']/cbc:TaxAmount")).isEqualTo("5.00");
        assertThat(x.texto(sub + "[cac:TaxCategory/cac:TaxScheme/cbc:ID='9997']/cbc:TaxableAmount")).isEqualTo("22.50");
        assertThat(x.texto(sub + "[cac:TaxCategory/cac:TaxScheme/cbc:ID='9998']/cbc:TaxableAmount")).isEqualTo("24.00");
        assertThat(x.texto(sub + "[cac:TaxCategory/cac:TaxScheme/cbc:ID='9996']/cbc:TaxableAmount")).isEqualTo("6.00");
    }

    @ParameterizedTest(name = "afectacion {0} -> tributo {1} {2} {3} categoria {4}")
    @CsvSource({"10,1000,IGV,VAT,S", "20,9997,EXO,VAT,E", "30,9998,INA,FRE,O", "31,9996,GRA,FRE,Z"})
    void cadaCategoriaUsaSuTributoDelCatalogo05(String afectacion, String codigo, String nombre, String tipo,
                                                String categoria) {
        XmlPrueba doc = new XmlPrueba(servicio.generar(unaLinea(afectacion, "10.00"), 1));
        String ruta = "/i:Invoice/cac:InvoiceLine/cac:TaxTotal/cac:TaxSubtotal/cac:TaxCategory";
        assertThat(doc.texto(ruta + "/cac:TaxScheme/cbc:ID")).isEqualTo(codigo);
        assertThat(doc.texto(ruta + "/cac:TaxScheme/cbc:Name")).isEqualTo(nombre);
        assertThat(doc.texto(ruta + "/cac:TaxScheme/cbc:TaxTypeCode")).isEqualTo(tipo);
        assertThat(doc.texto(ruta + "/cbc:ID")).isEqualTo(categoria);
        assertThat(doc.texto(ruta + "/cbc:TaxExemptionReasonCode")).isEqualTo(afectacion);
    }

    @Test
    void lasNoGravadasNoGeneranIgv() {
        for (String afectacion : List.of("20", "30", "31")) {
            XmlPrueba doc = new XmlPrueba(servicio.generar(unaLinea(afectacion, "10.00"), 1));
            assertThat(doc.texto("/i:Invoice/cac:InvoiceLine/cac:TaxTotal/cbc:TaxAmount")).isEqualTo("0.00");
            assertThat(doc.texto("/i:Invoice/cac:InvoiceLine/cac:TaxTotal/cac:TaxSubtotal/cac:TaxCategory/cbc:Percent"))
                    .isEqualTo("0.00");
        }
    }

    @Test
    void laGratuitaUsaValorReferencialYNoSumaAlTotal() {
        XmlPrueba doc = new XmlPrueba(servicio.generar(unaLinea("31", "10.00"), 1));
        assertThat(doc.texto("/i:Invoice/cac:InvoiceLine/cac:PricingReference/cac:AlternativeConditionPrice/cbc:PriceTypeCode"))
                .isEqualTo("02");
        assertThat(doc.texto("/i:Invoice/cac:InvoiceLine/cac:Price/cbc:PriceAmount")).isEqualTo("0.00");
        assertThat(doc.texto("/i:Invoice/cac:LegalMonetaryTotal/cbc:PayableAmount")).isEqualTo("0.00");
        assertThat(doc.texto("/i:Invoice/cbc:Note[@languageLocaleID='1002']")).startsWith("TRANSFERENCIA GRATUITA");
    }

    @Test
    void iscAlValorYBaseDelIgv() {
        String linea = "/i:Invoice/cac:InvoiceLine[4]";
        assertThat(x.texto(linea + "/cac:TaxTotal/cac:TaxSubtotal[cac:TaxCategory/cac:TaxScheme/cbc:ID='2000']/cbc:TaxAmount"))
                .isEqualTo("32.40");
        assertThat(x.texto(linea + "/cac:TaxTotal/cac:TaxSubtotal[cac:TaxCategory/cac:TaxScheme/cbc:ID='2000']/cac:TaxCategory/cbc:TierRange"))
                .isEqualTo("01");
        assertThat(x.texto(linea + "/cac:TaxTotal/cac:TaxSubtotal[cac:TaxCategory/cac:TaxScheme/cbc:ID='1000']/cbc:TaxableAmount"))
                .isEqualTo("140.40");
    }

    @Test
    void icbperConUnidadBaseYMontoPorUnidad() {
        String sub = "/i:Invoice/cac:InvoiceLine[3]/cac:TaxTotal/cac:TaxSubtotal[cac:TaxCategory/cac:TaxScheme/cbc:ID='7152']";
        assertThat(x.texto(sub + "/cbc:TaxAmount")).isEqualTo("5.00");
        assertThat(x.texto(sub + "/cbc:BaseUnitMeasure")).isEqualTo("10");
        assertThat(x.texto(sub + "/cac:TaxCategory/cbc:PerUnitAmount")).isEqualTo("0.50");
    }

    @Test
    void elPrecioUnitarioConTributosConservaSusDecimales() {
        assertThat(x.texto("/i:Invoice/cac:InvoiceLine[3]/cac:PricingReference/cac:AlternativeConditionPrice/cbc:PriceAmount"))
                .isEqualTo("0.118");
        assertThat(x.texto("/i:Invoice/cac:InvoiceLine[1]/cac:PricingReference/cac:AlternativeConditionPrice/cbc:PriceAmount"))
                .isEqualTo("165.20");
    }

    @Test
    void regimenDeRestaurantesYHotelesAplicaIgvDel10PorCiento() {
        XmlPrueba doc = new XmlPrueba(servicio.generar(unaLinea("10", "100.00"), 1, RegimenIgv.RESTAURANTES_HOTELES));
        assertThat(doc.texto("/i:Invoice/cac:TaxTotal/cbc:TaxAmount")).isEqualTo("20.00");
        XmlPrueba general = new XmlPrueba(servicio.generar(unaLinea("10", "100.00"), 1));
        assertThat(general.texto("/i:Invoice/cac:TaxTotal/cbc:TaxAmount")).isEqualTo("36.00");
    }

    @Test
    void creditoConSusCuotas() {
        XmlPrueba doc = new XmlPrueba(servicio.generar(Ejemplos.json(Ejemplos.VALIDOS.resolve("01-factura-credito-con-cuotas.json")), 7));
        assertThat(doc.cantidad("/i:Invoice/cac:PaymentTerms")).isEqualTo(3);
        assertThat(doc.texto("/i:Invoice/cac:PaymentTerms[1]/cbc:PaymentMeansID")).isEqualTo("Credito");
        assertThat(doc.texto("/i:Invoice/cac:PaymentTerms[2]/cbc:PaymentMeansID")).isEqualTo("Cuota001");
        assertThat(doc.texto("/i:Invoice/cac:PaymentTerms[3]/cbc:PaymentDueDate")).isEqualTo("2027-01-11");
        assertThat(doc.texto("/i:Invoice/cbc:ID")).isEqualTo("F001-00000007");
    }

    @Test
    void comprobanteEnDolares() {
        XmlPrueba doc = new XmlPrueba(servicio.generar(Ejemplos.json(Ejemplos.VALIDOS.resolve("03-factura-en-dolares-con-gratuita.json")), 1));
        assertThat(doc.texto("/i:Invoice/cbc:DocumentCurrencyCode")).isEqualTo("USD");
        assertThat(doc.texto("/i:Invoice/cac:LegalMonetaryTotal/cbc:PayableAmount/@currencyID")).isEqualTo("USD");
        assertThat(doc.texto("/i:Invoice/cbc:Note[@languageLocaleID='1000']")).endsWith("DÓLARES AMERICANOS");
    }

    // --------------------------------------------------------------- errores
    @Test
    void sinCorrelativoValidoNoSeGenera() {
        assertThatThrownBy(() -> servicio.generar(Ejemplos.parcial(), 0))
                .isInstanceOf(DatosFacturaInvalidosException.class).hasMessageContaining("correlativo");
    }

    @Test
    void siElXmlNoCumpleElXsdNoSeEntrega() {
        var adaptador = new GeneradorUblJaxbAdapter(new ValidadorXsd() {
            @Override
            public void validar(byte[] xml) {
                throw new pe.abc.facturacion.dominio.error.XmlNoConformeXsdException(List.of("simulado"));
            }
        });
        var svc = new GenerarFacturaXmlService(adaptador);
        assertThatThrownBy(() -> svc.generar(Ejemplos.parcial(), 1))
                .isInstanceOf(pe.abc.facturacion.dominio.error.XmlNoConformeXsdException.class);
    }
}
