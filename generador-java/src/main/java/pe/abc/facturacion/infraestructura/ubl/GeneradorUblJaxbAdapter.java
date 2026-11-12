package pe.abc.facturacion.infraestructura.ubl;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Set;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;

import org.glassfish.jaxb.runtime.marshaller.NamespacePrefixMapper;

import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_2.AddressLineType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_2.AddressType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_2.AttachmentType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_2.CountryType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_2.CustomerPartyType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_2.ExternalReferenceType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_2.InvoiceLineType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_2.ItemIdentificationType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_2.ItemType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_2.MonetaryTotalType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_2.PartyIdentificationType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_2.PartyLegalEntityType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_2.PartyNameType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_2.PartyType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_2.PaymentTermsType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_2.PriceType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_2.PricingReferenceType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_2.SignatureType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_2.SupplierPartyType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_2.TaxCategoryType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_2.TaxSchemeType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_2.TaxSubtotalType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_2.TaxTotalType;
import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_2.AddressTypeCodeType;
import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_2.BaseUnitMeasureType;
import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_2.CustomizationIDType;
import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_2.DescriptionType;
import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_2.DocumentCurrencyCodeType;
import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_2.IDType;
import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_2.IdentificationCodeType;
import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_2.InvoiceTypeCodeType;
import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_2.InvoicedQuantityType;
import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_2.IssueDateType;
import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_2.IssueTimeType;
import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_2.LineCountNumericType;
import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_2.LineExtensionAmountType;
import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_2.LineType;
import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_2.NameType;
import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_2.NoteType;
import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_2.PayableAmountType;
import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_2.PaymentDueDateType;
import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_2.PaymentMeansIDType;
import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_2.PerUnitAmountType;
import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_2.PercentType;
import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_2.PriceAmountType;
import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_2.PriceTypeCodeType;
import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_2.ProfileIDType;
import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_2.RegistrationNameType;
import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_2.TaxAmountType;
import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_2.TaxExemptionReasonCodeType;
import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_2.TaxInclusiveAmountType;
import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_2.TaxTypeCodeType;
import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_2.TaxableAmountType;
import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_2.TierRangeType;
import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_2.UBLVersionIDType;
import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_2.URIType;
import oasis.names.specification.ubl.schema.xsd.invoice_2.InvoiceType;
import oasis.names.specification.ubl.schema.xsd.invoice_2.ObjectFactory;

import un.unece.uncefact.data.specification.corecomponenttypeschemamodule._2.AmountType;
import un.unece.uncefact.data.specification.corecomponenttypeschemamodule._2.CodeType;
import un.unece.uncefact.data.specification.corecomponenttypeschemamodule._2.IdentifierType;
import un.unece.uncefact.data.specification.corecomponenttypeschemamodule._2.TextType;

import pe.abc.facturacion.aplicacion.puerto.GeneradorXmlUblPort;
import pe.abc.facturacion.dominio.NumeroLetras;
import pe.abc.facturacion.dominio.calculo.ImportesLinea;
import pe.abc.facturacion.dominio.calculo.CalculadoraTributaria;
import pe.abc.facturacion.dominio.calculo.Totales;
import pe.abc.facturacion.dominio.catalogo.CategoriaOperacion;
import pe.abc.facturacion.dominio.catalogo.Catalogos;
import pe.abc.facturacion.dominio.catalogo.SistemaCalculoIsc;
import pe.abc.facturacion.dominio.catalogo.TipoDocumentoIdentidad;
import pe.abc.facturacion.dominio.catalogo.TributoUbl;
import pe.abc.facturacion.dominio.error.ErrorFacturacionXml;
import pe.abc.facturacion.dominio.modelo.Modelo.Cuota;
import pe.abc.facturacion.dominio.modelo.Modelo.Emisor;
import pe.abc.facturacion.dominio.modelo.Modelo.Factura;
import pe.abc.facturacion.dominio.modelo.Modelo.FormaPago;
import pe.abc.facturacion.dominio.modelo.Modelo.LineaFactura;
import pe.abc.facturacion.dominio.modelo.Modelo.Receptor;

/**
 * Adaptador del puerto {@link GeneradorXmlUblPort}: genera el XML {@code Invoice} UBL 2.1 con las
 * <b>clases que JAXB (xjc) genera desde {@code UBL-Invoice-2.1.xsd}</b> en cada compilacion.
 *
 * <p>El documento no se arma con plantillas ni concatenando texto: se llenan objetos Java cuyo orden de
 * campos es el de {@code xsd:sequence} del XSD oficial, asi que no puede salir una etiqueta mal escrita ni
 * un elemento fuera de orden. Cada XML se valida contra el XSD antes de entregarlo.
 *
 * <p>Firma digital: el bloque {@code ext:UBLExtensions} lo agrega el firmador (fuera de este parcial); el XSD
 * lo declara opcional, por lo que el XML sin firmar es valido.
 */
public class GeneradorUblJaxbAdapter implements GeneradorXmlUblPort {

    private static final byte[] DECLARACION = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            .getBytes(StandardCharsets.UTF_8);
    private static final String NS_INVOICE = "urn:oasis:names:specification:ubl:schema:xsd:Invoice-2";
    private static final String NS_CAC = "urn:oasis:names:specification:ubl:schema:xsd:CommonAggregateComponents-2";
    private static final String NS_EXT = "urn:oasis:names:specification:ubl:schema:xsd:CommonExtensionComponents-2";
    private static final String NS_DS = "http://www.w3.org/2000/09/xmldsig#";
    private static final String NS_CCTS = "urn:un:unece:uncefact:documentation:2";
    private static final String NS_QDT = "urn:oasis:names:specification:ubl:schema:xsd:QualifiedDatatypes-2";
    private static final String NS_UDT = "urn:un:unece:uncefact:data:specification:UnqualifiedDataTypesSchemaModule:2";
    private static final String NS_XSI = "http://www.w3.org/2001/XMLSchema-instance";
    private static final String NS_CBC = "urn:oasis:names:specification:ubl:schema:xsd:CommonBasicComponents-2";

    private static final JAXBContext CONTEXTO = crearContexto();

    private final ValidadorXsd validador;
    private final DatatypeFactory fechas;

    public GeneradorUblJaxbAdapter() {
        this(new ValidadorXsd());
    }

    public GeneradorUblJaxbAdapter(ValidadorXsd validador) {
        this.validador = validador;
        try {
            this.fechas = DatatypeFactory.newInstance();
        } catch (DatatypeConfigurationException e) {
            throw new IllegalStateException(e);
        }
    }

    private static JAXBContext crearContexto() {
        try {
            return JAXBContext.newInstance(ObjectFactory.class);
        } catch (JAXBException e) {
            throw new IllegalStateException("No se pudo crear el contexto JAXB de UBL 2.1", e);
        }
    }

    // ------------------------------------------------------------------ API
    @Override
    public byte[] generar(Factura factura) {
        InvoiceType invoice = construir(factura, CalculadoraTributaria.calcularTotales(factura));
        byte[] xml = serializar(invoice);
        validador.validar(xml);
        return xml;
    }

    /** Construye el objeto {@code Invoice} (sin serializar ni validar). Visible para pruebas. */
    InvoiceType construir(Factura f, Totales t) {
        String moneda = f.moneda();
        InvoiceType inv = new InvoiceType();
        inv.setUBLVersionID(ident(new UBLVersionIDType(), Catalogos.UBL_VERSION_ID));
        inv.setCustomizationID(ident(new CustomizationIDType(), Catalogos.CUSTOMIZATION_ID));
        ProfileIDType perfil = ident(new ProfileIDType(), f.tipoOperacion());
        perfil.setSchemeName("SUNAT:Identificador de Tipo de Operación");
        perfil.setSchemeAgencyName(Catalogos.AGENCIA_SUNAT);
        perfil.setSchemeURI(Catalogos.uriCatalogo(17));
        inv.setProfileID(perfil);
        inv.setID(ident(new IDType(), f.numero()));
        IssueDateType fecha = new IssueDateType();
        fecha.setValue(fechas.newXMLGregorianCalendar(f.fechaEmision().toString()));
        inv.setIssueDate(fecha);
        if (f.horaEmision() != null) {
            IssueTimeType hora = new IssueTimeType();
            hora.setValue(fechas.newXMLGregorianCalendar(f.horaEmision().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"))));
            inv.setIssueTime(hora);
        }
        InvoiceTypeCodeType tipo = codigo(new InvoiceTypeCodeType(), f.tipoComprobante().codigo());
        tipo.setListID(f.tipoOperacion());
        tipo.setListAgencyName(Catalogos.AGENCIA_SUNAT);
        tipo.setListName("Tipo de Documento");
        tipo.setListURI(Catalogos.uriCatalogo(1));
        inv.setInvoiceTypeCode(tipo);

        inv.getNote().add(nota(NumeroLetras.importeEnLetras(t.importeTotal(), Catalogos.MONEDAS.get(moneda)),
                Catalogos.LEYENDA_MONTO_EN_LETRAS));
        if (t.soloGratuitas()) {
            inv.getNote().add(nota(Catalogos.TEXTO_TRANSFERENCIA_GRATUITA, Catalogos.LEYENDA_TRANSFERENCIA_GRATUITA));
        }
        DocumentCurrencyCodeType divisa = codigo(new DocumentCurrencyCodeType(), moneda);
        divisa.setListID("ISO 4217 Alpha");
        divisa.setListName("Currency");
        divisa.setListAgencyName(Catalogos.AGENCIA_UNECE);
        inv.setDocumentCurrencyCode(divisa);
        LineCountNumericType cuenta = new LineCountNumericType();
        cuenta.setValue(BigDecimal.valueOf(f.lineas().size()));
        inv.setLineCountNumeric(cuenta);

        inv.getSignature().add(firma(f));
        inv.setAccountingSupplierParty(emisor(f.emisor()));
        inv.setAccountingCustomerParty(receptor(f.receptor()));
        formaPago(inv, f.formaPago(), moneda);
        inv.getTaxTotal().add(totalTributos(t, moneda));
        inv.setLegalMonetaryTotal(totales(t, moneda));
        for (ImportesLinea linea : t.lineas()) {
            inv.getInvoiceLine().add(linea(linea, moneda));
        }
        return inv;
    }

    private byte[] serializar(InvoiceType invoice) {
        try {
            Marshaller marshaller = CONTEXTO.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            marshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
            marshaller.setProperty(Marshaller.JAXB_FRAGMENT, true);
            marshaller.setProperty("org.glassfish.jaxb.namespacePrefixMapper", new Prefijos());
            ByteArrayOutputStream salida = new ByteArrayOutputStream();
            salida.write(DECLARACION);
            marshaller.marshal(new ObjectFactory().createInvoice(invoice), salida);
            return salida.toByteArray();
        } catch (JAXBException | java.io.IOException e) {
            throw new ErrorFacturacionXml("No se pudo serializar el XML: " + e.getMessage(), e);
        }
    }

    /** Prefijos del documento: Invoice como espacio por defecto, y cac:/cbc: como en la Guia XML de SUNAT. */
    private static final class Prefijos extends NamespacePrefixMapper {
        @Override
        public String getPreferredPrefix(String uri, String sugerido, boolean requerido) {
            return switch (uri) {
                case NS_INVOICE -> "";
                case NS_CAC -> "cac";
                case NS_CBC -> "cbc";
                case NS_EXT -> "ext";
                case NS_DS -> "ds";
                case NS_CCTS -> "ccts";
                case NS_QDT -> "qdt";
                case NS_UDT -> "udt";
                case NS_XSI -> "xsi";
                default -> sugerido;
            };
        }

        @Override
        public String[] getPreDeclaredNamespaceUris() {
            // Se declaran en la raiz aunque no se usen: el firmador de SUNAT/OSE inserta ext:UBLExtensions
            // y ds:Signature y necesita estos prefijos ya enlazados (error 0306 si faltan).
            return new String[] {NS_CAC, NS_CBC, NS_CCTS, NS_DS, NS_EXT, NS_QDT, NS_UDT, NS_XSI};
        }
    }

    // ----------------------------------------------------------- partes
    /** {@code cac:Signature}: referencia a la firma que agregara el firmador (Guia XML, p. 80). */
    private SignatureType firma(Factura f) {
        String ruc = f.emisor().ruc();
        SignatureType sig = new SignatureType();
        sig.setID(ident(new IDType(), "IDSign" + ruc));
        PartyType parte = new PartyType();
        PartyIdentificationType identificacion = new PartyIdentificationType();
        identificacion.setID(ident(new IDType(), ruc));
        parte.getPartyIdentification().add(identificacion);
        PartyNameType nombre = new PartyNameType();
        nombre.setName(nombre(f.emisor().razonSocial()));
        parte.getPartyName().add(nombre);
        sig.setSignatoryParty(parte);
        ExternalReferenceType referencia = new ExternalReferenceType();
        URIType uri = new URIType();
        uri.setValue("#SignatureSP" + ruc);
        referencia.setURI(uri);
        AttachmentType adjunto = new AttachmentType();
        adjunto.setExternalReference(referencia);
        sig.setDigitalSignatureAttachment(adjunto);
        return sig;
    }

    private PartyIdentificationType identificacion(String tipoDoc, String numero) {
        IDType id = ident(new IDType(), numero);
        id.setSchemeID(tipoDoc);
        id.setSchemeName("Documento de Identidad");
        id.setSchemeAgencyName(Catalogos.AGENCIA_SUNAT);
        id.setSchemeURI(Catalogos.uriCatalogo(6));
        PartyIdentificationType pi = new PartyIdentificationType();
        pi.setID(id);
        return pi;
    }

    private SupplierPartyType emisor(Emisor e) {
        PartyType party = new PartyType();
        party.getPartyIdentification().add(identificacion(TipoDocumentoIdentidad.RUC.codigo(), e.ruc()));
        if (e.nombreComercial() != null && !e.nombreComercial().isEmpty()) {
            PartyNameType nombre = new PartyNameType();
            nombre.setName(nombre(e.nombreComercial()));
            party.getPartyName().add(nombre);
        }
        PartyLegalEntityType legal = new PartyLegalEntityType();
        legal.setRegistrationName(texto(new RegistrationNameType(), e.razonSocial()));
        AddressType direccion = new AddressType();
        IDType ubigeo = ident(new IDType(), e.ubigeo());
        ubigeo.setSchemeAgencyName("PE:INEI");
        ubigeo.setSchemeName("Ubigeos");
        direccion.setID(ubigeo);
        direccion.setAddressTypeCode(codigo(new AddressTypeCodeType(), e.codigoEstablecimiento()));
        direccion.getAddressLine().add(linea(e.direccion()));
        CountryType pais = new CountryType();
        IdentificationCodeType codigoPais = codigo(new IdentificationCodeType(), "PE");
        codigoPais.setListID("ISO 3166-1");
        codigoPais.setListAgencyName(Catalogos.AGENCIA_UNECE);
        codigoPais.setListName("Country");
        pais.setIdentificationCode(codigoPais);
        direccion.setCountry(pais);
        legal.setRegistrationAddress(direccion);
        party.getPartyLegalEntity().add(legal);
        SupplierPartyType supplier = new SupplierPartyType();
        supplier.setParty(party);
        return supplier;
    }

    private CustomerPartyType receptor(Receptor r) {
        PartyType party = new PartyType();
        party.getPartyIdentification().add(identificacion(r.tipoDocumento().codigo(), r.numeroDocumento()));
        PartyLegalEntityType legal = new PartyLegalEntityType();
        legal.setRegistrationName(texto(new RegistrationNameType(), r.razonSocial()));
        if (r.direccion() != null && !r.direccion().isEmpty()) {
            AddressType direccion = new AddressType();
            direccion.getAddressLine().add(linea(r.direccion()));
            legal.setRegistrationAddress(direccion);
        }
        party.getPartyLegalEntity().add(legal);
        CustomerPartyType customer = new CustomerPartyType();
        customer.setParty(party);
        return customer;
    }

    private static AddressLineType linea(String texto) {
        LineType linea = new LineType();
        linea.setValue(texto);
        AddressLineType al = new AddressLineType();
        al.setLine(linea);
        return al;
    }

    /** Forma de pago (RS 000193-2020/SUNAT): Contado, o Credito con sus cuotas. */
    private void formaPago(InvoiceType inv, FormaPago fp, String moneda) {
        PaymentTermsType termino = new PaymentTermsType();
        termino.setID(ident(new IDType(), "FormaPago"));
        termino.getPaymentMeansID().add(ident(new PaymentMeansIDType(), fp.tipo()));
        if (fp.esCredito()) {
            termino.setAmount(importe(new oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_2.AmountType(),
                    fp.montoNetoPendiente(), moneda));
            inv.getPaymentTerms().add(termino);
            int n = 1;
            for (Cuota cuota : fp.cuotas()) {
                PaymentTermsType c = new PaymentTermsType();
                c.setID(ident(new IDType(), "FormaPago"));
                c.getPaymentMeansID().add(ident(new PaymentMeansIDType(), String.format("Cuota%03d", n++)));
                c.setAmount(importe(new oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_2.AmountType(),
                        cuota.monto(), moneda));
                PaymentDueDateType vence = new PaymentDueDateType();
                vence.setValue(fechas.newXMLGregorianCalendar(cuota.fechaVencimiento().toString()));
                c.setPaymentDueDate(vence);
                inv.getPaymentTerms().add(c);
            }
        } else {
            inv.getPaymentTerms().add(termino);
        }
    }

    // --------------------------------------------------------- tributos
    private TaxSubtotalType subtotal(TributoUbl tributo, String moneda, BigDecimal monto, BigDecimal base,
                                     BigDecimal porcentaje, String afectacion, String sistemaIsc,
                                     String unidadBase, BigDecimal cantidadBase, BigDecimal montoPorUnidad) {
        TaxSubtotalType sub = new TaxSubtotalType();
        if (base != null) {
            sub.setTaxableAmount(importe(new TaxableAmountType(), base, moneda));
        }
        sub.setTaxAmount(importe(new TaxAmountType(), monto, moneda));
        if (unidadBase != null) {
            BaseUnitMeasureType medida = new BaseUnitMeasureType();
            medida.setValue(sinCeros(cantidadBase));
            medida.setUnitCode(unidadBase);
            sub.setBaseUnitMeasure(medida);
        }
        TaxCategoryType categoria = new TaxCategoryType();
        if (tributo.categoriaUnece() != null) {
            IDType id = ident(new IDType(), tributo.categoriaUnece());
            id.setSchemeID("UN/ECE 5305");
            id.setSchemeName("Tax Category Identifier");
            id.setSchemeAgencyName(Catalogos.AGENCIA_UNECE);
            categoria.setID(id);
        }
        if (porcentaje != null) {
            PercentType p = new PercentType();
            p.setValue(importeTexto(porcentaje));
            categoria.setPercent(p);
        }
        if (montoPorUnidad != null) {
            categoria.setPerUnitAmount(importe(new PerUnitAmountType(), montoPorUnidad, moneda));
        }
        if (afectacion != null) {
            TaxExemptionReasonCodeType exencion = codigo(new TaxExemptionReasonCodeType(), afectacion);
            exencion.setListAgencyName(Catalogos.AGENCIA_SUNAT);
            exencion.setListName("Afectacion del IGV");
            exencion.setListURI(Catalogos.uriCatalogo(7));
            categoria.setTaxExemptionReasonCode(exencion);
        }
        if (sistemaIsc != null) {
            TierRangeType rango = new TierRangeType();
            rango.setValue(sistemaIsc);
            categoria.setTierRange(rango);
        }
        TaxSchemeType esquema = new TaxSchemeType();
        IDType idEsquema = ident(new IDType(), tributo.codigo());
        idEsquema.setSchemeID("UN/ECE 5153");
        idEsquema.setSchemeAgencyID("6");
        esquema.setID(idEsquema);
        esquema.setName(nombre(tributo.nombre()));
        esquema.setTaxTypeCode(codigo(new TaxTypeCodeType(), tributo.tipoCodigo()));
        categoria.setTaxScheme(esquema);
        sub.setTaxCategory(categoria);
        return sub;
    }

    /** {@code cac:TaxTotal} de cabecera: un {@code TaxSubtotal} por cada tributo presente. */
    private TaxTotalType totalTributos(Totales t, String moneda) {
        TaxTotalType total = new TaxTotalType();
        total.setTaxAmount(importe(new TaxAmountType(), t.totalTributos(), moneda));
        Set<CategoriaOperacion> categorias = new java.util.HashSet<>();
        t.lineas().forEach(l -> categorias.add(l.linea().categoria()));
        BigDecimal cero = CalculadoraTributaria.CERO;
        if (t.totalIsc().signum() != 0) {
            total.getTaxSubtotal().add(subtotal(TributoUbl.ISC, moneda, t.totalIsc(), t.baseIsc(),
                    null, null, null, null, null, null));
        }
        if (categorias.contains(CategoriaOperacion.GRAVADA)) {
            total.getTaxSubtotal().add(subtotal(TributoUbl.IGV, moneda, t.totalIgv(), t.totalGravadas(),
                    null, null, null, null, null, null));
        }
        if (categorias.contains(CategoriaOperacion.EXONERADA)) {
            total.getTaxSubtotal().add(subtotal(TributoUbl.EXONERADO, moneda, cero, t.totalExoneradas(),
                    null, null, null, null, null, null));
        }
        if (categorias.contains(CategoriaOperacion.INAFECTA)) {
            total.getTaxSubtotal().add(subtotal(TributoUbl.INAFECTO, moneda, cero, t.totalInafectas(),
                    null, null, null, null, null, null));
        }
        if (categorias.contains(CategoriaOperacion.GRATUITA)) {
            total.getTaxSubtotal().add(subtotal(TributoUbl.GRATUITO, moneda, cero, t.totalGratuitas(),
                    null, null, null, null, null, null));
        }
        if (t.totalIcbper().signum() != 0) {
            total.getTaxSubtotal().add(subtotal(TributoUbl.ICBPER, moneda, t.totalIcbper(), null,
                    null, null, null, null, null, null));
        }
        return total;
    }

    private MonetaryTotalType totales(Totales t, String moneda) {
        MonetaryTotalType lmt = new MonetaryTotalType();
        lmt.setLineExtensionAmount(importe(new LineExtensionAmountType(), t.totalValorVenta(), moneda));
        lmt.setTaxInclusiveAmount(importe(new TaxInclusiveAmountType(), t.importeTotal(), moneda));
        lmt.setPayableAmount(importe(new PayableAmountType(), t.importeTotal(), moneda));
        return lmt;
    }

    // ----------------------------------------------------------- lineas
    private InvoiceLineType linea(ImportesLinea li, String moneda) {
        LineaFactura linea = li.linea();
        InvoiceLineType il = new InvoiceLineType();
        il.setID(ident(new IDType(), String.valueOf(linea.numeroLinea())));
        InvoicedQuantityType cantidad = new InvoicedQuantityType();
        cantidad.setValue(sinCeros(linea.cantidad()));
        cantidad.setUnitCode(linea.unidadMedida());
        cantidad.setUnitCodeListID("UN/ECE rec 20");
        cantidad.setUnitCodeListAgencyName(Catalogos.AGENCIA_UNECE);
        il.setInvoicedQuantity(cantidad);
        il.setLineExtensionAmount(importe(new LineExtensionAmountType(), li.valorVenta(), moneda));

        PriceType precio = new PriceType();
        precio.setPriceAmount(importeUnitario(li.precioUnitario(), moneda));
        PriceTypeCodeType tipoPrecio = codigo(new PriceTypeCodeType(),
                li.esGratuita() ? Catalogos.VALOR_REFERENCIAL_GRATUITO : Catalogos.PRECIO_UNITARIO_CON_TRIBUTOS);
        tipoPrecio.setListName("Tipo de Precio");
        tipoPrecio.setListAgencyName(Catalogos.AGENCIA_SUNAT);
        tipoPrecio.setListURI(Catalogos.uriCatalogo(16));
        precio.setPriceTypeCode(tipoPrecio);
        PricingReferenceType referencia = new PricingReferenceType();
        referencia.getAlternativeConditionPrice().add(precio);
        il.setPricingReference(referencia);

        TaxTotalType total = new TaxTotalType();
        total.setTaxAmount(importe(new TaxAmountType(), li.totalTributos(), moneda));
        if (linea.sistemaIsc() != null) {
            boolean especifico = linea.sistemaIsc() == SistemaCalculoIsc.ESPECIFICO;
            total.getTaxSubtotal().add(subtotal(TributoUbl.ISC, moneda, li.isc(), li.baseIsc(),
                    especifico ? null : linea.tasaIsc(), null, linea.sistemaIsc().codigo(),
                    especifico ? linea.unidadBaseIsc() : null, especifico ? linea.cantidad() : null,
                    especifico ? linea.montoFijoIsc() : null));
        }
        total.getTaxSubtotal().add(subtotal(TributoUbl.porCategoria(linea.categoria()), moneda, li.igv(), li.baseIgv(),
                li.tasaIgvInformada(), linea.tipoAfectacionIgv(), null, null, null, null));
        if (li.icbper().signum() != 0) {
            total.getTaxSubtotal().add(subtotal(TributoUbl.ICBPER, moneda, li.icbper(), null, null, null, null,
                    "NIU", BigDecimal.valueOf(linea.cantidadBolsas()), li.importeIcbperUnitario()));
        }
        il.getTaxTotal().add(total);

        ItemType item = new ItemType();
        DescriptionType descripcion = new DescriptionType();
        descripcion.setValue(linea.descripcion());
        item.getDescription().add(descripcion);
        ItemIdentificationType vendedor = new ItemIdentificationType();
        vendedor.setID(ident(new IDType(), linea.codigoProducto()));
        item.setSellersItemIdentification(vendedor);
        il.setItem(item);

        // Valor unitario sin tributos; en gratuitas no hay contraprestacion (0.00).
        PriceType valor = new PriceType();
        valor.setPriceAmount(importeUnitario(li.esGratuita() ? CalculadoraTributaria.CERO : linea.valorUnitario(), moneda));
        il.setPrice(valor);
        return il;
    }

    // -------------------------------------------------------- utilidades
    private <T extends IdentifierType> T ident(T objeto, String valor) {
        objeto.setValue(valor);
        return objeto;
    }

    private <T extends TextType> T texto(T objeto, String valor) {
        objeto.setValue(valor);
        return objeto;
    }

    private <T extends CodeType> T codigo(T objeto, String valor) {
        objeto.setValue(valor);
        return objeto;
    }

    private <T extends AmountType> T importe(T objeto, BigDecimal valor, String moneda) {
        objeto.setValue(importeTexto(valor));
        objeto.setCurrencyID(moneda);
        return objeto;
    }

    private PriceAmountType importeUnitario(BigDecimal valor, String moneda) {
        PriceAmountType p = new PriceAmountType();
        p.setValue(unitario(valor));
        p.setCurrencyID(moneda);
        return p;
    }

    private NoteType nota(String texto, String leyenda) {
        NoteType nota = new NoteType();
        nota.setValue(texto);
        nota.setLanguageLocaleID(leyenda);
        return nota;
    }

    private NameType nombre(String texto) {
        NameType nombre = new NameType();
        nombre.setValue(texto);
        return nombre;
    }

    /** Importe con 2 decimales ({@code AmountType}). */
    static BigDecimal importeTexto(BigDecimal valor) {
        return valor.setScale(2, java.math.RoundingMode.HALF_UP);
    }

    /** Cantidad sin ceros sobrantes ({@code QuantityType}): 10, 2.5. */
    static BigDecimal sinCeros(BigDecimal valor) {
        return new BigDecimal(valor.stripTrailingZeros().toPlainString());
    }

    /** Valor unitario: hasta 10 decimales y como minimo 2 (Guia XML, n(12,10)). */
    static BigDecimal unitario(BigDecimal valor) {
        BigDecimal limpio = new BigDecimal(valor.stripTrailingZeros().toPlainString());
        return limpio.scale() < 2 ? limpio.setScale(2) : limpio;
    }
}
