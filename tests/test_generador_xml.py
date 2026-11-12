"""Pruebas unitarias de generación del XML UBL 2.1 (Invoice).

Cubren lo que pide el segundo parcial:

* que se genere el XML y sea válido contra ``UBL-Invoice-2.1.xsd`` (OASIS);
* que la estructura tenga los elementos obligatorios y en su lugar;
* que los datos y los importes calculados queden en la etiqueta correcta;
* que los formatos (fechas, importes, códigos, codificación UTF-8) sean los esperados;
* que los datos faltantes o inválidos se rechacen (pruebas negativas);
* que el validador XSD detecte un XML alterado.

Ejecutar desde la raíz del proyecto:  ``pytest -v``
"""
from __future__ import annotations

import copy
import json
from decimal import Decimal
from pathlib import Path

import pytest
from lxml import etree

from facturacion_xml import (
    DatosFacturaInvalidosError,
    GeneradorFacturaUbl,
    RegimenIgv,
    ValidadorXsd,
    XmlNoConformeXsdError,
    generar_xml_factura,
)
from facturacion_xml.__main__ import leer_json_utf8, main
from facturacion_xml.errores import ErrorFacturacionXml, EsquemaXsdIncompletoError
from facturacion_xml.modelo import ruc_valido
from facturacion_xml.numero_letras import entero_en_letras, importe_en_letras

RAIZ = Path(__file__).resolve().parents[1]
EJEMPLO = RAIZ / "docs" / "segundo-parcial" / "ejemplos" / "factura-abarrotes.json"
NS = {
    "inv": "urn:oasis:names:specification:ubl:schema:xsd:Invoice-2",
    "cac": "urn:oasis:names:specification:ubl:schema:xsd:CommonAggregateComponents-2",
    "cbc": "urn:oasis:names:specification:ubl:schema:xsd:CommonBasicComponents-2",
}


# ---------------------------------------------------------------- utilidades
@pytest.fixture(scope="module")
def validador():
    return ValidadorXsd()


@pytest.fixture
def datos():
    """Factura representativa de abarrotes: gravadas, ISC, ICBPER, exonerada, inafecta y gratuita."""
    return json.loads(EJEMPLO.read_text(encoding="utf-8"))


def factura_simple(**cambios):
    """Factura mínima con una sola línea gravada (para pruebas puntuales)."""
    base = json.loads(EJEMPLO.read_text(encoding="utf-8"))
    base["items"] = [copy.deepcopy(base["items"][0])]
    base.update(cambios)
    return base


def generar(datos, correlativo=1, regimen=RegimenIgv.GENERAL):
    return etree.fromstring(generar_xml_factura(datos, correlativo, regimen))


def texto(nodo, xpath):
    encontrados = nodo.xpath(xpath, namespaces=NS)
    assert encontrados, f"no existe {xpath}"
    resultado = encontrados[0]
    return resultado if isinstance(resultado, str) else resultado.text


def subtotal(nodo, codigo_tributo, base="cac:TaxTotal"):
    xpath = f"{base}/cac:TaxSubtotal[cac:TaxCategory/cac:TaxScheme/cbc:ID='{codigo_tributo}']"
    encontrados = nodo.xpath(xpath, namespaces=NS)
    assert len(encontrados) == 1, f"se esperaba un TaxSubtotal {codigo_tributo} en {base}"
    return encontrados[0]


def linea(raiz, numero):
    return raiz.xpath(f"cac:InvoiceLine[cbc:ID='{numero}']", namespaces=NS)[0]


def errores_de(datos, correlativo=1, regimen=RegimenIgv.GENERAL):
    with pytest.raises(DatosFacturaInvalidosError) as exc:
        generar_xml_factura(datos, correlativo, regimen)
    return "\n".join(exc.value.errores)


# ============================================================ 1. Generación
class TestGeneracion:
    def test_genera_bytes_utf8_con_declaracion_y_raiz_invoice(self, datos):
        xml = generar_xml_factura(datos, 1)
        assert isinstance(xml, bytes)
        assert xml.startswith(b'<?xml version="1.0" encoding="UTF-8"?>')
        raiz = etree.fromstring(xml)
        assert raiz.tag == f"{{{NS['inv']}}}Invoice"

    def test_xml_generado_es_valido_contra_el_xsd_oficial(self, datos, validador):
        xml = generar_xml_factura(datos, 1)
        assert validador.errores(xml) == []

    def test_genera_archivo_xml_por_linea_de_comandos(self, tmp_path, capsys):
        salida = tmp_path / "F001-00000007.xml"
        codigo = main([str(EJEMPLO), str(salida), "--correlativo", "7"])
        assert codigo == 0
        assert salida.is_file()
        assert texto(etree.parse(str(salida)).getroot(), "cbc:ID") == "F001-00000007"
        assert "validado" in capsys.readouterr().out

    def test_generar_rechaza_el_xml_si_no_cumple_el_xsd_antes_de_entregarlo(self, datos, monkeypatch):
        """El docente exige validar contra el XSD antes de firmar: si el árbol construido
        quedara mal (aquí se le quita cbc:IssueDate), generar() no debe devolverlo."""
        construir_original = GeneradorFacturaUbl._construir

        def construir_roto(self, factura, totales):
            raiz = construir_original(self, factura, totales)
            raiz.remove(raiz.find("cbc:IssueDate", NS))
            return raiz

        monkeypatch.setattr(GeneradorFacturaUbl, "_construir", construir_roto)
        with pytest.raises(XmlNoConformeXsdError, match="IssueDate"):
            generar_xml_factura(datos, 1)

    def test_el_generador_usa_el_validador_inyectado(self, datos, validador):
        from facturacion_xml import factura_desde_dict
        llamadas = []

        class ValidadorEspia(ValidadorXsd):
            def validar(self, xml):
                llamadas.append(xml)
                super().validar(xml)

        xml = GeneradorFacturaUbl(ValidadorEspia()).generar(factura_desde_dict(datos, 1))
        assert llamadas == [xml]
        assert validador.es_valido(xml)


# ============================================================ 2. Estructura
class TestEstructura:
    def test_cabecera_obligatoria_ubl_y_sunat(self, datos):
        raiz = generar(datos, 42)
        assert texto(raiz, "cbc:UBLVersionID") == "2.1"
        assert texto(raiz, "cbc:CustomizationID") == "2.0"
        assert texto(raiz, "cbc:ID") == "F001-00000042"
        assert texto(raiz, "cbc:IssueDate") == "2026-11-11"
        assert texto(raiz, "cbc:InvoiceTypeCode") == "01"
        assert texto(raiz, "cbc:DocumentCurrencyCode") == "PEN"
        assert texto(raiz, "cbc:LineCountNumeric") == "7"

    def test_elementos_obligatorios_del_xsd_presentes(self, datos):
        raiz = generar(datos)
        for xpath in ("cbc:ID", "cbc:IssueDate", "cac:AccountingSupplierParty", "cac:AccountingCustomerParty",
                      "cac:LegalMonetaryTotal/cbc:PayableAmount", "cac:InvoiceLine"):
            assert raiz.xpath(xpath, namespaces=NS), f"falta {xpath}"

    def test_orden_de_la_cabecera_sigue_la_secuencia_de_invoicetype(self, datos):
        raiz = generar(datos)
        nombres = [etree.QName(hijo).localname for hijo in raiz]
        esperado = ["UBLVersionID", "CustomizationID", "ProfileID", "ID", "IssueDate", "IssueTime",
                    "InvoiceTypeCode", "Note", "DocumentCurrencyCode", "LineCountNumeric", "Signature",
                    "AccountingSupplierParty", "AccountingCustomerParty", "PaymentTerms", "TaxTotal",
                    "LegalMonetaryTotal"] + ["InvoiceLine"] * 7
        assert nombres == esperado

    def test_atributos_de_catalogos_sunat(self, datos):
        raiz = generar(datos)
        tipo = raiz.find("cbc:InvoiceTypeCode", NS)
        assert tipo.get("listAgencyName") == "PE:SUNAT"
        assert tipo.get("listURI").endswith("catalogo01")
        assert raiz.find("cbc:ProfileID", NS).text == "0101"
        cantidad = linea(raiz, 1).find("cbc:InvoicedQuantity", NS)
        assert cantidad.get("unitCode") == "SA"
        assert cantidad.get("unitCodeListID") == "UN/ECE rec 20"

    def test_no_genera_elementos_ajenos_al_xsd_ubl_21(self, datos):
        xml = generar_xml_factura(datos, 1)
        # AdditionalMonetaryTotal (1001-1004) era de UBL 2.0; en UBL 2.1 no existe.
        assert b"AdditionalMonetaryTotal" not in xml
        assert b"sac:" not in xml

    def test_un_taxsubtotal_por_tributo_en_la_cabecera(self, datos):
        raiz = generar(datos)
        codigos = raiz.xpath("cac:TaxTotal/cac:TaxSubtotal/cac:TaxCategory/cac:TaxScheme/cbc:ID/text()", namespaces=NS)
        assert sorted(codigos) == ["1000", "2000", "7152", "9996", "9997", "9998"]


# ============================================================ 3. Datos en su etiqueta
class TestDatosEnEtiquetas:
    def test_emisor_y_receptor(self, datos):
        raiz = generar(datos)
        emisor = "cac:AccountingSupplierParty/cac:Party"
        assert texto(raiz, f"{emisor}/cac:PartyIdentification/cbc:ID") == "20601234565"
        assert texto(raiz, f"{emisor}/cac:PartyIdentification/cbc:ID/@schemeID") == "6"
        assert texto(raiz, f"{emisor}/cac:PartyLegalEntity/cbc:RegistrationName") == "ABC SOFTWARE CORP S.A.C."
        assert texto(raiz, f"{emisor}/cac:PartyLegalEntity/cac:RegistrationAddress/cbc:ID") == "100601"
        assert texto(raiz, f"{emisor}/cac:PartyLegalEntity/cac:RegistrationAddress/cbc:AddressTypeCode") == "0000"
        receptor = "cac:AccountingCustomerParty/cac:Party"
        assert texto(raiz, f"{receptor}/cac:PartyIdentification/cbc:ID") == "20555555556"
        assert texto(raiz, f"{receptor}/cac:PartyLegalEntity/cbc:RegistrationName") == "COMERCIALIZADORA SAN JUAN S.A.C."

    def test_linea_gravada_con_igv_18(self, datos):
        li = linea(generar(datos), 1)
        assert texto(li, "cbc:LineExtensionAmount") == "1400.00"
        assert texto(li, "cac:Price/cbc:PriceAmount") == "140.00"
        assert texto(li, "cac:PricingReference/cac:AlternativeConditionPrice/cbc:PriceAmount") == "165.20"
        assert texto(li, "cac:PricingReference/cac:AlternativeConditionPrice/cbc:PriceTypeCode") == "01"
        igv = subtotal(li, "1000")
        assert texto(igv, "cbc:TaxableAmount") == "1400.00"
        assert texto(igv, "cbc:TaxAmount") == "252.00"
        assert texto(igv, "cac:TaxCategory/cbc:Percent") == "18.00"
        assert texto(igv, "cac:TaxCategory/cbc:TaxExemptionReasonCode") == "10"
        assert texto(igv, "cac:TaxCategory/cbc:ID") == "S"
        assert texto(li, "cac:Item/cbc:Description") == "Arroz Extra Costeño Saco 50 kg"
        assert texto(li, "cac:Item/cac:SellersItemIdentification/cbc:ID") == "PROD-ARR-01"

    def test_totales_de_cabecera(self, datos):
        raiz = generar(datos)
        assert texto(raiz, "cac:TaxTotal/cbc:TaxAmount") == "386.85"
        assert texto(subtotal(raiz, "1000"), "cbc:TaxableAmount") == "1909.00"
        assert texto(subtotal(raiz, "1000"), "cbc:TaxAmount") == "349.45"
        lmt = "cac:LegalMonetaryTotal"
        assert texto(raiz, f"{lmt}/cbc:LineExtensionAmount") == "1955.50"
        assert texto(raiz, f"{lmt}/cbc:TaxInclusiveAmount") == "2342.35"
        assert texto(raiz, f"{lmt}/cbc:PayableAmount") == "2342.35"
        assert texto(raiz, "cbc:Note[@languageLocaleID='1000']") == "DOS MIL TRESCIENTOS CUARENTA Y DOS CON 35/100 SOLES"

    def test_cuadre_payable_igual_valor_venta_mas_tributos(self, datos):
        raiz = generar(datos)
        valor = Decimal(texto(raiz, "cac:LegalMonetaryTotal/cbc:LineExtensionAmount"))
        tributos = Decimal(texto(raiz, "cac:TaxTotal/cbc:TaxAmount"))
        assert Decimal(texto(raiz, "cac:LegalMonetaryTotal/cbc:PayableAmount")) == valor + tributos
        suma_lineas = sum(Decimal(t) for t in raiz.xpath("cac:InvoiceLine/cac:TaxTotal/cbc:TaxAmount/text()", namespaces=NS))
        assert suma_lineas == tributos


# ============================================================ 4. Categorías 1001-1004
class TestCategoriasDeOperacion:
    @pytest.mark.parametrize("numero, tributo, nombre, tipo, categoria, afectacion, base", [
        (1, "1000", "IGV", "VAT", "S", "10", "1400.00"),          # 1001 gravada
        (6, "9998", "INAFECTO", "FRE", "O", "30", "24.00"),        # 1002 inafecta
        (5, "9997", "EXONERADO", "VAT", "E", "20", "22.50"),       # 1003 exonerada
        (7, "9996", "GRATUITO", "FRE", "Z", "31", "6.00"),         # 1004 gratuita
    ])
    def test_cada_categoria_usa_su_tributo_del_catalogo_05(self, datos, numero, tributo, nombre, tipo, categoria,
                                                           afectacion, base):
        st = subtotal(linea(generar(datos), numero), tributo)
        assert texto(st, "cac:TaxCategory/cac:TaxScheme/cbc:Name") == nombre
        assert texto(st, "cac:TaxCategory/cac:TaxScheme/cbc:TaxTypeCode") == tipo
        assert texto(st, "cac:TaxCategory/cbc:ID") == categoria
        assert texto(st, "cac:TaxCategory/cbc:TaxExemptionReasonCode") == afectacion
        assert texto(st, "cbc:TaxableAmount") == base

    @pytest.mark.parametrize("numero", [5, 6, 7])
    def test_exoneradas_inafectas_y_gratuitas_no_generan_igv(self, datos, numero):
        li = linea(generar(datos), numero)
        assert texto(li, "cac:TaxTotal/cbc:TaxAmount") == "0.00"
        assert texto(li, "cac:TaxTotal/cac:TaxSubtotal/cac:TaxCategory/cbc:Percent") == "0.00"
        assert not li.xpath("cac:TaxTotal/cac:TaxSubtotal[cac:TaxCategory/cac:TaxScheme/cbc:ID='1000']", namespaces=NS)

    def test_gratuita_usa_valor_referencial_y_no_suma_al_total(self, datos):
        raiz = generar(datos)
        li = linea(raiz, 7)
        assert texto(li, "cac:PricingReference/cac:AlternativeConditionPrice/cbc:PriceTypeCode") == "02"
        assert texto(li, "cac:PricingReference/cac:AlternativeConditionPrice/cbc:PriceAmount") == "1.20"
        assert texto(li, "cac:Price/cbc:PriceAmount") == "0.00"
        assert texto(subtotal(raiz, "9996"), "cbc:TaxableAmount") == "6.00"
        assert texto(raiz, "cac:LegalMonetaryTotal/cbc:LineExtensionAmount") == "1955.50"  # sin los 6.00

    def test_comprobante_solo_gratuito_lleva_leyenda_1002_y_total_cero(self, validador):
        datos = factura_simple()
        datos["items"][0].update(tipo_afectacion_igv="21")
        xml = generar_xml_factura(datos, 1)
        raiz = etree.fromstring(xml)
        assert texto(raiz, "cbc:Note[@languageLocaleID='1002']").startswith("TRANSFERENCIA GRATUITA")
        assert texto(raiz, "cac:LegalMonetaryTotal/cbc:PayableAmount") == "0.00"
        assert validador.es_valido(xml)

    def test_igv_10_por_ciento_solo_si_se_indica_el_regimen(self, validador):
        datos = factura_simple()
        general = generar(datos)
        especial_xml = generar_xml_factura(datos, 1, RegimenIgv.RESTAURANTES_HOTELES)
        especial = etree.fromstring(especial_xml)
        assert texto(subtotal(linea(general, 1), "1000"), "cac:TaxCategory/cbc:Percent") == "18.00"
        assert texto(subtotal(linea(especial, 1), "1000"), "cac:TaxCategory/cbc:Percent") == "10.00"
        assert texto(subtotal(linea(especial, 1), "1000"), "cbc:TaxAmount") == "140.00"
        assert validador.es_valido(especial_xml)


# ============================================================ 5. ISC (Catálogo 08) e ICBPER
class TestIscEIcbper:
    def test_isc_al_valor_sistema_01_y_base_del_igv(self, datos):
        li = linea(generar(datos), 4)
        isc = subtotal(li, "2000")
        assert texto(isc, "cbc:TaxableAmount") == "108.00"
        assert texto(isc, "cbc:TaxAmount") == "32.40"
        assert texto(isc, "cac:TaxCategory/cbc:TierRange") == "01"
        assert texto(isc, "cac:TaxCategory/cbc:Percent") == "30.00"
        igv = subtotal(li, "1000")
        assert texto(igv, "cbc:TaxableAmount") == "140.40"   # 108.00 + 32.40: el ISC integra la base
        assert texto(igv, "cbc:TaxAmount") == "25.27"

    def test_isc_especifico_sistema_02(self, validador):
        datos = factura_simple()
        datos["items"][0].update(unidad_medida="LTR", cantidad="12", valor_unitario="5.00", sistema_isc="02",
                                 monto_fijo_isc="1.25", unidad_base_isc="LTR")
        xml = generar_xml_factura(datos, 1)
        isc = subtotal(linea(etree.fromstring(xml), 1), "2000")
        assert texto(isc, "cbc:TaxAmount") == "15.00"
        assert texto(isc, "cbc:BaseUnitMeasure") == "12"
        assert texto(isc, "cbc:BaseUnitMeasure/@unitCode") == "LTR"
        assert texto(isc, "cac:TaxCategory/cbc:PerUnitAmount") == "1.25"
        assert texto(isc, "cac:TaxCategory/cbc:TierRange") == "02"
        assert validador.es_valido(xml)

    def test_isc_precio_venta_publico_sistema_03(self, validador):
        datos = factura_simple()
        datos["items"][0].update(cantidad="10", valor_unitario="3.00", sistema_isc="03", tasa_isc="17.00",
                                 precio_venta_publico_sugerido="5.90")
        xml = generar_xml_factura(datos, 1)
        isc = subtotal(linea(etree.fromstring(xml), 1), "2000")
        assert texto(isc, "cbc:TaxableAmount") == "50.00"   # 10 × 5.90 / 1.18
        assert texto(isc, "cbc:TaxAmount") == "8.50"
        assert texto(isc, "cac:TaxCategory/cbc:TierRange") == "03"
        assert validador.es_valido(xml)

    def test_icbper_por_bolsa_segun_fecha_de_emision(self, datos):
        li = linea(generar(datos), 3)
        icbper = subtotal(li, "7152")
        assert texto(icbper, "cbc:TaxAmount") == "5.00"
        assert texto(icbper, "cbc:BaseUnitMeasure") == "10"
        assert texto(icbper, "cac:TaxCategory/cbc:PerUnitAmount") == "0.50"

    def test_icbper_2022_usa_el_importe_de_ese_anio(self):
        datos = factura_simple(fecha_emision="2022-06-15")
        datos["items"][0]["cantidad_bolsas"] = 10
        icbper = subtotal(linea(generar(datos), 1), "7152")
        assert texto(icbper, "cac:TaxCategory/cbc:PerUnitAmount") == "0.40"
        assert texto(icbper, "cbc:TaxAmount") == "4.00"


# ============================================================ 6. Formatos y otras variantes válidas
class TestFormatos:
    def test_formatos_de_fecha_hora_e_importes(self, datos):
        raiz = generar(datos)
        assert texto(raiz, "cbc:IssueTime") == "10:30:00"
        importes = raiz.xpath("//cbc:TaxAmount/text() | //cbc:PayableAmount/text() | //cbc:LineExtensionAmount/text()",
                              namespaces=NS)
        assert importes and all(len(i.split(".")[1]) == 2 for i in importes)
        assert all(a.get("currencyID") == "PEN" for a in raiz.xpath("//*[@currencyID]", namespaces=NS))

    def test_conserva_tildes_y_enie_en_utf8(self, datos):
        datos["receptor"]["razon_social"] = "PANIFICADORA ÑUÑOA E.I.R.L."
        xml = generar_xml_factura(datos, 1)
        decodificado = xml.decode("utf-8")          # falla si hubiera bytes WIN1252
        assert "PANIFICADORA ÑUÑOA E.I.R.L." in decodificado
        assert "Arroz Extra Costeño" in decodificado
        assert texto(etree.fromstring(xml), "cac:AccountingCustomerParty/cac:Party/cac:PartyLegalEntity/cbc:RegistrationName") \
            == "PANIFICADORA ÑUÑOA E.I.R.L."

    def test_lee_json_utf8_con_bom(self, tmp_path):
        ruta = tmp_path / "entrada.json"
        ruta.write_bytes(b"\xef\xbb\xbf" + EJEMPLO.read_bytes())
        assert leer_json_utf8(ruta)["serie"] == "F001"

    def test_rechaza_json_guardado_en_win1252(self, tmp_path):
        ruta = tmp_path / "entrada-ansi.json"
        ruta.write_bytes(EJEMPLO.read_text(encoding="utf-8").encode("cp1252"))
        with pytest.raises(ErrorFacturacionXml, match="WIN1252"):
            leer_json_utf8(ruta)

    def test_boleta_con_dni(self, validador):
        datos = factura_simple(tipo_comprobante="03", serie="B001",
                               receptor={"tipo_documento": "1", "numero_documento": "45678912",
                                         "razon_social": "JUAN PÉREZ QUISPE"})
        xml = generar_xml_factura(datos, 15)
        raiz = etree.fromstring(xml)
        assert texto(raiz, "cbc:ID") == "B001-00000015"
        assert texto(raiz, "cbc:InvoiceTypeCode") == "03"
        assert texto(raiz, "cac:AccountingCustomerParty/cac:Party/cac:PartyIdentification/cbc:ID/@schemeID") == "1"
        assert validador.es_valido(xml)

    def test_pago_al_credito_con_cuotas(self, validador):
        datos = factura_simple(forma_pago={
            "tipo": "Credito", "monto_neto_pendiente": "1652.00",
            "cuotas": [{"monto": "826.00", "fecha_vencimiento": "2026-12-11"},
                       {"monto": "826.00", "fecha_vencimiento": "2027-01-11"}]})
        xml = generar_xml_factura(datos, 1)
        raiz = etree.fromstring(xml)
        terminos = raiz.findall("cac:PaymentTerms", NS)
        assert [t.find("cbc:PaymentMeansID", NS).text for t in terminos] == ["Credito", "Cuota001", "Cuota002"]
        assert terminos[0].find("cbc:Amount", NS).text == "1652.00"
        assert terminos[2].find("cbc:PaymentDueDate", NS).text == "2027-01-11"
        assert validador.es_valido(xml)

    def test_moneda_dolares(self):
        raiz = generar(factura_simple(moneda="USD"))
        assert texto(raiz, "cbc:DocumentCurrencyCode") == "USD"
        assert texto(raiz, "cbc:Note").endswith("DÓLARES AMERICANOS")
        assert all(a.get("currencyID") == "USD" for a in raiz.xpath("//*[@currencyID]", namespaces=NS))


# ============================================================ 7. Datos faltantes o inválidos
class TestDatosInvalidos:
    def test_campos_obligatorios_faltantes(self, datos):
        del datos["emisor"]["ruc"]
        del datos["fecha_emision"]
        datos["receptor"]["razon_social"] = "  "
        errores = errores_de(datos)
        assert "emisor.ruc: es obligatorio" in errores
        assert "fecha_emision: es obligatorio" in errores
        assert "receptor.razon_social: es obligatorio" in errores

    def test_sin_lineas(self, datos):
        datos["items"] = []
        assert "items: debe contener al menos una línea" in errores_de(datos)

    def test_ruc_con_digito_verificador_incorrecto(self, datos):
        datos["emisor"]["ruc"] = "20601234567"
        assert "emisor.ruc: RUC inválido" in errores_de(datos)

    @pytest.mark.parametrize("serie, mensaje", [
        ("F|01", "^[FB][A-Z0-9]{3}$"),     # el error del patrón con barra que observó el docente
        ("B001", "debe usar una serie que empiece con 'F'"),
        ("F1", "^[FB][A-Z0-9]{3}$"),
    ])
    def test_serie_invalida_o_incoherente_con_el_tipo(self, datos, serie, mensaje):
        datos["serie"] = serie
        assert mensaje in errores_de(datos)

    @pytest.mark.parametrize("correlativo", [0, -1, 100_000_000, "5", True])
    def test_correlativo_fuera_de_rango(self, datos, correlativo):
        assert "correlativo" in errores_de(datos, correlativo)

    def test_el_cliente_no_puede_enviar_el_correlativo(self, datos):
        datos["correlativo"] = 99
        assert "el cliente no envía el correlativo" in errores_de(datos)

    def test_unidad_de_medida_fuera_del_catalogo_03(self, datos):
        datos["items"][0]["unidad_medida"] = "SACO"
        assert "items[0].unidad_medida: 'SACO' no es un código del Catálogo 03" in errores_de(datos)

    @pytest.mark.parametrize("cantidad", ["0", "-3", "1,5", 10, "abc"])
    def test_cantidad_invalida(self, datos, cantidad):
        datos["items"][0]["cantidad"] = cantidad
        assert "items[0].cantidad" in errores_de(datos)

    def test_importe_como_numero_flotante_se_rechaza(self, datos):
        datos["items"][0]["valor_unitario"] = 140.0
        assert "decimal exacto en texto" in errores_de(datos)

    def test_factura_exige_receptor_con_ruc(self, datos):
        datos["receptor"].update(tipo_documento="1", numero_documento="45678912")
        assert "la factura exige un adquirente con RUC" in errores_de(datos)

    def test_isc_sin_tasa_en_sistema_01(self, datos):
        del datos["items"][3]["tasa_isc"]
        assert "items[3].tasa_isc: es obligatorio" in errores_de(datos)

    def test_isc_en_linea_no_gravada(self, datos):
        datos["items"][4].update(sistema_isc="01", tasa_isc="10.00")
        assert "el ISC solo se admite en líneas gravadas" in errores_de(datos)

    @pytest.mark.parametrize("afectacion", ["11", "17", "40"])
    def test_afectaciones_fuera_del_alcance_se_rechazan_explicitamente(self, datos, afectacion):
        datos["items"][0]["tipo_afectacion_igv"] = afectacion
        assert "no se genera en este parcial" in errores_de(datos)

    def test_nota_de_credito_no_usa_el_xsd_invoice(self, datos):
        datos["tipo_comprobante"] = "07"
        assert "UBL-CreditNote-2.1.xsd" in errores_de(datos)

    def test_cuotas_que_no_suman_el_pendiente(self, datos):
        datos["forma_pago"] = {"tipo": "Credito", "monto_neto_pendiente": "1000.00",
                               "cuotas": [{"monto": "400.00", "fecha_vencimiento": "2026-12-01"}]}
        assert "la suma de las cuotas debe ser igual al monto neto pendiente" in errores_de(datos)

    def test_boleta_mayor_a_700_sin_identificar_al_comprador(self):
        datos = factura_simple(tipo_comprobante="03", serie="B001",
                               receptor={"tipo_documento": "0", "numero_documento": "-", "razon_social": "CLIENTES VARIOS"})
        assert "debe identificar al comprador" in errores_de(datos)

    def test_numeracion_de_lineas_no_secuencial(self, datos):
        datos["items"][1]["numero_linea"] = 1
        assert "items[1].numero_linea: debe ser 2" in errores_de(datos)

    def test_reporta_todos_los_errores_juntos(self, datos):
        datos["moneda"] = "EUR"
        datos["items"][0]["unidad_medida"] = "SACO"
        datos["emisor"]["ubigeo"] = "1006"
        with pytest.raises(DatosFacturaInvalidosError) as exc:
            generar_xml_factura(datos, 1)
        assert len(exc.value.errores) == 3


# ============================================================ 8. Validación XSD (casos negativos)
class TestValidacionXsd:
    def _xml(self, datos):
        return etree.fromstring(generar_xml_factura(datos, 1))

    def test_detecta_falta_de_elemento_obligatorio(self, datos, validador):
        raiz = self._xml(datos)
        raiz.remove(raiz.find("cbc:IssueDate", NS))
        errores = validador.errores(raiz)
        assert errores and any("IssueDate" in e for e in errores)
        with pytest.raises(XmlNoConformeXsdError):
            validador.validar(raiz)

    def test_detecta_elemento_fuera_de_orden(self, datos, validador):
        raiz = self._xml(datos)
        version = raiz.find("cbc:UBLVersionID", NS)
        raiz.remove(version)
        raiz.append(version)
        assert not validador.es_valido(raiz)

    def test_detecta_elemento_inexistente_en_el_xsd(self, datos, validador):
        raiz = self._xml(datos)
        etree.SubElement(raiz.find("cac:LegalMonetaryTotal", NS), f"{{{NS['cbc']}}}TotalInventado").text = "1.00"
        assert not validador.es_valido(raiz)

    def test_detecta_tipo_de_dato_incorrecto(self, datos, validador):
        raiz = self._xml(datos)
        raiz.find("cbc:IssueDate", NS).text = "11/11/2026"
        assert not validador.es_valido(raiz)

    def test_detecta_xml_mal_formado(self, validador):
        assert validador.errores(b"<Invoice><sin-cerrar></Invoice>")[0].startswith("XML mal formado")

    def test_falla_con_mensaje_claro_si_falta_un_xsd(self, tmp_path):
        with pytest.raises(EsquemaXsdIncompletoError, match="UBL-Invoice-2.1.xsd"):
            ValidadorXsd(tmp_path).esquema


# ============================================================ 9. Utilidades de dominio
class TestUtilidades:
    @pytest.mark.parametrize("ruc, valido", [
        ("20131312955", True),    # RUC real de SUNAT
        ("20601234565", True),
        ("20601234567", False),   # dígito verificador incorrecto
        ("30601234565", False),   # prefijo inexistente
        ("2060123456", False),    # 10 dígitos
    ])
    def test_ruc_modulo_11(self, ruc, valido):
        assert ruc_valido(ruc) is valido

    @pytest.mark.parametrize("numero, letras", [
        (0, "CERO"), (16, "DIECISEIS"), (21, "VEINTIUNO"), (100, "CIEN"), (101, "CIENTO UNO"),
        (1000, "MIL"), (2130, "DOS MIL CIENTO TREINTA"), (21000, "VEINTIUN MIL"),
        (1_000_000, "UN MILLON"), (2_500_000, "DOS MILLONES QUINIENTOS MIL"),
    ])
    def test_entero_en_letras(self, numero, letras):
        assert entero_en_letras(numero) == letras

    def test_importe_en_letras(self):
        assert importe_en_letras(Decimal("2130.18"), "SOLES") == "DOS MIL CIENTO TREINTA CON 18/100 SOLES"
