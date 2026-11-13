"""Generador del XML de factura/boleta electrónica en UBL 2.1 (``Invoice``).

El documento se construye como **árbol de elementos** con lxml, no con
plantillas de texto: cada elemento se crea con su espacio de nombres y en el
orden de la secuencia ``xsd:sequence`` de ``InvoiceType`` y de cada tipo
agregado (``PartyType``, ``TaxSubtotalType``, ``InvoiceLineType``…) de los
XSD OASIS UBL 2.1. Así no puede salir una etiqueta mal escrita ni un
elemento que el esquema no admita; y si algo cambia, el validador XSD lo
detecta antes de entregar el XML.

Firma digital: el bloque ``ext:UBLExtensions`` con ``ds:Signature`` lo agrega
el firmador (``FirmaDigitalPort``), que no forma parte de este parcial. El
XSD lo declara opcional, por lo que el XML sin firmar es válido.
"""
from __future__ import annotations

from datetime import date
from decimal import Decimal
from typing import Any

from lxml import etree

from . import catalogos as cat
from .calculo import CERO, ImportesLinea, Totales, calcular_totales
from .modelo import Factura, factura_desde_dict
from .numero_letras import importe_en_letras
from .validador_xsd import ValidadorXsd

DECLARACION_XML = b'<?xml version="1.0" encoding="UTF-8"?>\n'


def _cbc(padre: etree._Element, nombre: str, texto: str | None = None, **atributos: str) -> etree._Element:
    el = etree.SubElement(padre, f"{{{cat.NS_CBC}}}{nombre}", {k: v for k, v in atributos.items() if v is not None})
    if texto is not None:
        el.text = texto
    return el


def _cac(padre: etree._Element, nombre: str) -> etree._Element:
    return etree.SubElement(padre, f"{{{cat.NS_CAC}}}{nombre}")


def _importe(valor: Decimal) -> str:
    """Importe con 2 decimales (``AmountType``)."""
    return f"{valor:.2f}"


def _decimal(valor: Decimal) -> str:
    """Cantidad sin ceros sobrantes (``QuantityType``): ``10``, ``2.5``."""
    return format(valor.normalize(), "f")


def _unitario(valor: Decimal) -> str:
    """Valor unitario: conserva hasta 10 decimales y como mínimo 2 (Guía XML, n(12,10))."""
    texto = format(valor.normalize(), "f")
    if "." not in texto:
        return f"{texto}.00"
    entero, fraccion = texto.split(".")
    return f"{entero}.{fraccion.ljust(2, '0')}"


class GeneradorFacturaUbl:
    """Construye el XML ``Invoice`` UBL 2.1 de una :class:`Factura`."""

    def __init__(self, validador: ValidadorXsd | None = None):
        self.validador = validador or ValidadorXsd()

    # ------------------------------------------------------------------ API
    def generar(self, factura: Factura, validar: bool = True) -> bytes:
        """Devuelve el XML en UTF-8 con declaración. Si ``validar``, lo valida contra el XSD."""
        totales = calcular_totales(factura)
        raiz = self._construir(factura, totales)
        xml = DECLARACION_XML + etree.tostring(raiz, encoding="UTF-8", pretty_print=True)
        if validar:
            self.validador.validar(xml)
        return xml

    def generar_desde_dict(self, datos: dict[str, Any], correlativo: int,
                           regimen_igv: cat.RegimenIgv | str = cat.RegimenIgv.GENERAL,
                           hoy: date | None = None) -> bytes:
        return self.generar(factura_desde_dict(datos, correlativo, regimen_igv, hoy))

    # ------------------------------------------------------------ cabecera
    def _construir(self, f: Factura, t: Totales) -> etree._Element:
        inv = etree.Element(f"{{{cat.NS_INVOICE}}}Invoice", nsmap=cat.NSMAP)
        moneda = f.moneda

        _cbc(inv, "UBLVersionID", cat.UBL_VERSION_ID)
        _cbc(inv, "CustomizationID", cat.CUSTOMIZATION_ID)
        _cbc(inv, "ProfileID", f.tipo_operacion,
             schemeName="SUNAT:Identificador de Tipo de Operación",
             schemeAgencyName=cat.AGENCIA_SUNAT,
             schemeURI=cat.URI_CATALOGO.format(17))
        _cbc(inv, "ID", f.numero)
        _cbc(inv, "IssueDate", f.fecha_emision.isoformat())
        if f.hora_emision:
            _cbc(inv, "IssueTime", f.hora_emision.isoformat())
        _cbc(inv, "InvoiceTypeCode", f.tipo_comprobante.value,
             listID=f.tipo_operacion,
             listAgencyName=cat.AGENCIA_SUNAT,
             listName="Tipo de Documento",
             listURI=cat.URI_CATALOGO.format(1))
        _cbc(inv, "Note", importe_en_letras(t.importe_total, cat.MONEDAS[moneda]),
             languageLocaleID=cat.LEYENDA_MONTO_EN_LETRAS)
        if t.solo_gratuitas:
            _cbc(inv, "Note", cat.TEXTO_TRANSFERENCIA_GRATUITA, languageLocaleID=cat.LEYENDA_TRANSFERENCIA_GRATUITA)
        _cbc(inv, "DocumentCurrencyCode", moneda,
             listID="ISO 4217 Alpha", listName="Currency", listAgencyName=cat.AGENCIA_UNECE)
        _cbc(inv, "LineCountNumeric", str(len(f.lineas)))

        self._firma(inv, f)
        self._emisor(inv, f)
        self._receptor(inv, f)
        self._forma_pago(inv, f, moneda)
        self._total_tributos(inv, t, moneda)
        self._totales(inv, t, moneda)
        for importes in t.lineas:
            self._linea(inv, importes, moneda)
        return inv

    def _firma(self, inv: etree._Element, f: Factura) -> None:
        """``cac:Signature``: referencia a la firma que agregará el firmador (Guía XML, p. 80)."""
        sig = _cac(inv, "Signature")
        _cbc(sig, "ID", f"IDSign{f.emisor.ruc}")
        parte = _cac(sig, "SignatoryParty")
        _cbc(_cac(parte, "PartyIdentification"), "ID", f.emisor.ruc)
        _cbc(_cac(parte, "PartyName"), "Name", f.emisor.razon_social)
        adjunto = _cac(sig, "DigitalSignatureAttachment")
        _cbc(_cac(adjunto, "ExternalReference"), "URI", f"#SignatureSP{f.emisor.ruc}")

    def _identificacion(self, party: etree._Element, tipo_doc: str, numero: str) -> None:
        _cbc(_cac(party, "PartyIdentification"), "ID", numero,
             schemeID=tipo_doc,
             schemeName="Documento de Identidad",
             schemeAgencyName=cat.AGENCIA_SUNAT,
             schemeURI=cat.URI_CATALOGO.format(6))

    def _emisor(self, inv: etree._Element, f: Factura) -> None:
        e = f.emisor
        party = _cac(_cac(inv, "AccountingSupplierParty"), "Party")
        self._identificacion(party, cat.TipoDocumentoIdentidad.RUC.value, e.ruc)
        if e.nombre_comercial:
            _cbc(_cac(party, "PartyName"), "Name", e.nombre_comercial)
        legal = _cac(party, "PartyLegalEntity")
        _cbc(legal, "RegistrationName", e.razon_social)
        direccion = _cac(legal, "RegistrationAddress")
        _cbc(direccion, "ID", e.ubigeo, schemeAgencyName="PE:INEI", schemeName="Ubigeos")
        _cbc(direccion, "AddressTypeCode", e.codigo_establecimiento)
        _cbc(_cac(direccion, "AddressLine"), "Line", e.direccion)
        _cbc(_cac(direccion, "Country"), "IdentificationCode", "PE",
             listID="ISO 3166-1", listAgencyName=cat.AGENCIA_UNECE, listName="Country")

    def _receptor(self, inv: etree._Element, f: Factura) -> None:
        r = f.receptor
        party = _cac(_cac(inv, "AccountingCustomerParty"), "Party")
        self._identificacion(party, r.tipo_documento.value, r.numero_documento)
        legal = _cac(party, "PartyLegalEntity")
        _cbc(legal, "RegistrationName", r.razon_social)
        if r.direccion:
            _cbc(_cac(_cac(legal, "RegistrationAddress"), "AddressLine"), "Line", r.direccion)

    def _forma_pago(self, inv: etree._Element, f: Factura, moneda: str) -> None:
        """Forma de pago (RS 000193-2020/SUNAT): Contado, o Crédito con sus cuotas."""
        fp = f.forma_pago
        termino = _cac(inv, "PaymentTerms")
        _cbc(termino, "ID", "FormaPago")
        _cbc(termino, "PaymentMeansID", fp.tipo)
        if fp.tipo == "Credito":
            _cbc(termino, "Amount", _importe(fp.monto_neto_pendiente), currencyID=moneda)
            for n, cuota in enumerate(fp.cuotas, start=1):
                c = _cac(inv, "PaymentTerms")
                _cbc(c, "ID", "FormaPago")
                _cbc(c, "PaymentMeansID", f"Cuota{n:03d}")
                _cbc(c, "Amount", _importe(cuota.monto), currencyID=moneda)
                _cbc(c, "PaymentDueDate", cuota.fecha_vencimiento.isoformat())

    # -------------------------------------------------------------- tributos
    def _subtotal(self, padre: etree._Element, tributo: cat.Tributo, moneda: str, monto: Decimal,
                  base: Decimal | None = None, *, porcentaje: Decimal | None = None,
                  afectacion: str | None = None, sistema_isc: str | None = None,
                  unidad_base: tuple[str, Decimal] | None = None, monto_por_unidad: Decimal | None = None) -> None:
        """``cac:TaxSubtotal`` en el orden de ``TaxSubtotalType`` y ``TaxCategoryType``."""
        sub = _cac(padre, "TaxSubtotal")
        if base is not None:
            _cbc(sub, "TaxableAmount", _importe(base), currencyID=moneda)
        _cbc(sub, "TaxAmount", _importe(monto), currencyID=moneda)
        if unidad_base:
            _cbc(sub, "BaseUnitMeasure", _decimal(unidad_base[1]), unitCode=unidad_base[0])
        categoria = _cac(sub, "TaxCategory")
        if tributo.categoria_unece:
            _cbc(categoria, "ID", tributo.categoria_unece, schemeID="UN/ECE 5305",
                 schemeName="Tax Category Identifier", schemeAgencyName=cat.AGENCIA_UNECE)
        if porcentaje is not None:
            _cbc(categoria, "Percent", _importe(porcentaje))
        if monto_por_unidad is not None:
            _cbc(categoria, "PerUnitAmount", _importe(monto_por_unidad), currencyID=moneda)
        if afectacion:
            _cbc(categoria, "TaxExemptionReasonCode", afectacion, listAgencyName=cat.AGENCIA_SUNAT,
                 listName="Afectacion del IGV", listURI=cat.URI_CATALOGO.format(7))
        if sistema_isc:
            _cbc(categoria, "TierRange", sistema_isc)
        esquema = _cac(categoria, "TaxScheme")
        _cbc(esquema, "ID", tributo.codigo, schemeID="UN/ECE 5153", schemeAgencyID="6")
        _cbc(esquema, "Name", tributo.nombre)
        _cbc(esquema, "TaxTypeCode", tributo.tipo_codigo)

    def _total_tributos(self, inv: etree._Element, t: Totales, moneda: str) -> None:
        """``cac:TaxTotal`` de cabecera: un ``TaxSubtotal`` por cada tributo presente."""
        total = _cac(inv, "TaxTotal")
        _cbc(total, "TaxAmount", _importe(t.total_tributos), currencyID=moneda)
        categorias = {li.linea.categoria for li in t.lineas}
        if t.total_isc:
            self._subtotal(total, cat.ISC, moneda, t.total_isc, t.base_isc)
        if cat.CategoriaOperacion.GRAVADA in categorias:
            self._subtotal(total, cat.IGV, moneda, t.total_igv, t.total_gravadas)
        if cat.CategoriaOperacion.EXONERADA in categorias:
            self._subtotal(total, cat.EXONERADO, moneda, CERO, t.total_exoneradas)
        if cat.CategoriaOperacion.INAFECTA in categorias:
            self._subtotal(total, cat.INAFECTO, moneda, CERO, t.total_inafectas)
        if cat.CategoriaOperacion.GRATUITA in categorias:
            self._subtotal(total, cat.GRATUITO, moneda, CERO, t.total_gratuitas)
        if t.total_icbper:
            self._subtotal(total, cat.ICBPER, moneda, t.total_icbper)

    def _totales(self, inv: etree._Element, t: Totales, moneda: str) -> None:
        """``cac:LegalMonetaryTotal`` en el orden de ``MonetaryTotalType``."""
        lmt = _cac(inv, "LegalMonetaryTotal")
        _cbc(lmt, "LineExtensionAmount", _importe(t.total_valor_venta), currencyID=moneda)
        _cbc(lmt, "TaxInclusiveAmount", _importe(t.importe_total), currencyID=moneda)
        _cbc(lmt, "PayableAmount", _importe(t.importe_total), currencyID=moneda)

    # ----------------------------------------------------------------- líneas
    def _linea(self, inv: etree._Element, li: ImportesLinea, moneda: str) -> None:
        """``cac:InvoiceLine`` en el orden de ``InvoiceLineType``."""
        linea = li.linea
        il = _cac(inv, "InvoiceLine")
        _cbc(il, "ID", str(linea.numero_linea))
        _cbc(il, "InvoicedQuantity", _decimal(linea.cantidad), unitCode=linea.unidad_medida,
             unitCodeListID="UN/ECE rec 20", unitCodeListAgencyName=cat.AGENCIA_UNECE)
        _cbc(il, "LineExtensionAmount", _importe(li.valor_venta), currencyID=moneda)

        precio = _cac(_cac(il, "PricingReference"), "AlternativeConditionPrice")
        tipo_precio = cat.VALOR_REFERENCIAL_GRATUITO if li.es_gratuita else cat.PRECIO_UNITARIO_CON_TRIBUTOS
        _cbc(precio, "PriceAmount", _unitario(li.precio_unitario), currencyID=moneda)
        _cbc(precio, "PriceTypeCode", tipo_precio, listName="Tipo de Precio",
             listAgencyName=cat.AGENCIA_SUNAT, listURI=cat.URI_CATALOGO.format(16))

        total = _cac(il, "TaxTotal")
        _cbc(total, "TaxAmount", _importe(li.total_tributos), currencyID=moneda)
        if linea.sistema_isc:
            sistema = linea.sistema_isc
            self._subtotal(
                total, cat.ISC, moneda, li.isc, li.base_isc,
                porcentaje=linea.tasa_isc if sistema is not cat.SistemaIsc.ESPECIFICO else None,
                unidad_base=(linea.unidad_base_isc, linea.cantidad) if sistema is cat.SistemaIsc.ESPECIFICO else None,
                monto_por_unidad=linea.monto_fijo_isc if sistema is cat.SistemaIsc.ESPECIFICO else None,
                sistema_isc=sistema.value,
            )
        self._subtotal(total, cat.TRIBUTO_POR_CATEGORIA[linea.categoria], moneda, li.igv, li.base_igv,
                       porcentaje=li.tasa_igv_informada, afectacion=linea.tipo_afectacion_igv)
        if li.icbper:
            self._subtotal(total, cat.ICBPER, moneda, li.icbper,
                           unidad_base=("NIU", Decimal(linea.cantidad_bolsas)),
                           monto_por_unidad=li.importe_icbper_unitario)

        item = _cac(il, "Item")
        _cbc(item, "Description", linea.descripcion)
        _cbc(_cac(item, "SellersItemIdentification"), "ID", linea.codigo_producto)

        # Valor unitario sin tributos; en gratuitas no hay contraprestación (0.00).
        valor = CERO if li.es_gratuita else linea.valor_unitario
        _cbc(_cac(il, "Price"), "PriceAmount", _unitario(valor), currencyID=moneda)
