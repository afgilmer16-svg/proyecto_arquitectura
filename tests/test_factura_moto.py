"""Caso de negocio: factura por la venta de una motocicleta (un ítem de alto valor con datos del vehículo)."""
import copy
import json
from datetime import date
from decimal import Decimal
from pathlib import Path

import pytest
from lxml import etree

from facturacion_xml import DatosFacturaInvalidosError, ValidadorXsd, generar_xml_factura

RAIZ = Path(__file__).resolve().parents[1]
JSON_MOTO = RAIZ / "specs" / "ejemplos" / "validos" / "05-factura-moto.json"
XML_REFERENCIA = RAIZ / "specs" / "ejemplos" / "xml-referencia" / "05-factura-moto.xml"
NS = {"cbc": "urn:oasis:names:specification:ubl:schema:xsd:CommonBasicComponents-2",
      "cac": "urn:oasis:names:specification:ubl:schema:xsd:CommonAggregateComponents-2"}
HOY = date(2026, 11, 12)


@pytest.fixture(scope="module")
def datos():
    return json.loads(JSON_MOTO.read_text(encoding="utf-8"))


@pytest.fixture(scope="module")
def raiz(datos):
    xml = generar_xml_factura(datos, 1, hoy=HOY)
    ValidadorXsd().validar(xml)
    return etree.fromstring(xml)


def _t(raiz, ruta):
    return raiz.findtext(ruta, namespaces=NS)


def test_importes_de_la_moto(raiz):
    # 6779.66 + IGV 18 % (1220.34) = 8000.00
    assert _t(raiz, "cac:LegalMonetaryTotal/cbc:LineExtensionAmount") == "6779.66"
    assert _t(raiz, "cac:TaxTotal/cbc:TaxAmount") == "1220.34"
    assert _t(raiz, "cac:LegalMonetaryTotal/cbc:PayableAmount") == "8000.00"
    base = Decimal(_t(raiz, "cac:LegalMonetaryTotal/cbc:LineExtensionAmount"))
    assert base + Decimal(_t(raiz, "cac:TaxTotal/cbc:TaxAmount")) == Decimal("8000.00")


def test_monto_en_letras(raiz):
    assert raiz.find("cbc:Note", NS).text == "OCHO MIL CON 00/100 SOLES"


def test_una_sola_linea_de_una_unidad(raiz):
    lineas = raiz.findall("cac:InvoiceLine", NS)
    assert len(lineas) == 1
    cantidad = lineas[0].find("cbc:InvoicedQuantity", NS)
    assert (cantidad.text, cantidad.get("unitCode")) == ("1", "NIU")
    assert _t(raiz, "cbc:LineCountNumeric") == "1"


def test_la_descripcion_conserva_chasis_motor_y_tildes(raiz):
    descripcion = _t(raiz, "cac:InvoiceLine/cac:Item/cbc:Description")
    for dato in ("LTMKD0001R5000123", "KD01E-5000123", "AÑO 2026", "150CC"):
        assert dato in descripcion
    assert len("LTMKD0001R5000123") == 17  # VIN


def test_igv_de_la_linea_y_precio_con_impuestos(raiz):
    linea = raiz.find("cac:InvoiceLine", NS)
    assert _t(linea, "cac:TaxTotal/cbc:TaxAmount") == "1220.34"
    assert _t(linea, "cac:PricingReference/cac:AlternativeConditionPrice/cbc:PriceAmount") == "8000.00"
    assert _t(linea, "cac:Price/cbc:PriceAmount") == "6779.66"


def test_receptor_con_ruc_y_numero_de_comprobante(raiz):
    assert _t(raiz, "cbc:ID") == "F001-00000001"
    assert _t(raiz, "cbc:InvoiceTypeCode") == "01"
    cliente = raiz.find("cac:AccountingCustomerParty/cac:Party/cac:PartyIdentification/cbc:ID", NS)
    assert (cliente.get("schemeID"), cliente.text) == ("6", "20555555556")


def test_el_xml_de_referencia_versionado_es_el_que_se_genera(datos):
    generado = generar_xml_factura(datos, 1, hoy=HOY).replace(b"\r\n", b"\n")
    assert generado == XML_REFERENCIA.read_bytes().replace(b"\r\n", b"\n")


@pytest.mark.parametrize("campo,valor,mensaje", [
    ("descripcion", "MOTO\x07 ROJA", "caracteres de control"),
    ("descripcion", "X" * 251, "supera 250"),
    ("cantidad", "0", "mayor que cero"),
])
def test_datos_invalidos_de_la_linea(datos, campo, valor, mensaje):
    d = copy.deepcopy(datos)
    d["items"][0][campo] = valor
    with pytest.raises(DatosFacturaInvalidosError, match=mensaje):
        generar_xml_factura(d, 1, hoy=HOY)


def test_la_descripcion_se_escapa_en_el_xml(datos):
    d = copy.deepcopy(datos)
    d["items"][0]["descripcion"] = 'MOTO "TIGRE" & <CIA> 150cc'
    raiz = etree.fromstring(generar_xml_factura(d, 1, hoy=HOY))
    assert _t(raiz, "cac:InvoiceLine/cac:Item/cbc:Description") == 'MOTO "TIGRE" & <CIA> 150cc'


# ------------------------------------------------------------------ fecha de emisión
def test_no_se_emite_con_fecha_futura(datos):
    d = copy.deepcopy(datos)
    d["fecha_emision"] = "2026-11-13"
    with pytest.raises(DatosFacturaInvalidosError, match="posterior a la fecha actual"):
        generar_xml_factura(d, 1, hoy=HOY)


def test_se_emite_con_la_fecha_de_hoy(datos):
    d = copy.deepcopy(datos)
    d["fecha_emision"] = HOY.isoformat()
    assert b"<cbc:IssueDate>2026-11-12</cbc:IssueDate>" in generar_xml_factura(d, 1, hoy=HOY)


def test_la_fecha_de_referencia_por_defecto_es_la_actual(datos):
    d = copy.deepcopy(datos)
    d["fecha_emision"] = "2999-01-01"
    with pytest.raises(DatosFacturaInvalidosError, match="posterior a la fecha actual"):
        generar_xml_factura(d, 1)
