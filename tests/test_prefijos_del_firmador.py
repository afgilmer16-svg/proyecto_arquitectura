# Autor: Gilmer Ayala Falcón · UNAS-FIIS · Arquitectura de Software · Grupo A
"""El firmador (SUNAT/OSE, p. ej. probar-xml.nubefact.com) inserta ``ext:UBLExtensions`` con ``ds:Signature``.

Si la raiz del XML no declara esos prefijos, el OSE responde con el error 0306
("The prefix "ext" for element "ext:UBLExtensions" is not bound"). Estas pruebas fijan el hallazgo.
"""
import json
from pathlib import Path

import pytest
from lxml import etree

from facturacion_xml import generar_xml_factura

RAIZ = Path(__file__).resolve().parents[1]
EJEMPLOS = [RAIZ / "docs" / "ejemplos" / "factura-abarrotes.json"] + sorted(
    (RAIZ / "specs" / "ejemplos" / "validos").glob("*.json"))
PREFIJOS = {
    "cac": "urn:oasis:names:specification:ubl:schema:xsd:CommonAggregateComponents-2",
    "cbc": "urn:oasis:names:specification:ubl:schema:xsd:CommonBasicComponents-2",
    "ext": "urn:oasis:names:specification:ubl:schema:xsd:CommonExtensionComponents-2",
    "ds": "http://www.w3.org/2000/09/xmldsig#",
    "ccts": "urn:un:unece:uncefact:documentation:2",
    "qdt": "urn:oasis:names:specification:ubl:schema:xsd:QualifiedDatatypes-2",
    "udt": "urn:un:unece:uncefact:data:specification:UnqualifiedDataTypesSchemaModule:2",
    "xsi": "http://www.w3.org/2001/XMLSchema-instance",
}
BLOQUE_DE_FIRMA = ('<ext:UBLExtensions><ext:UBLExtension><ext:ExtensionContent><ds:Signature Id="SignatureSP"/>'
                   '</ext:ExtensionContent></ext:UBLExtension></ext:UBLExtensions>')


def _xml(ruta: Path) -> bytes:
    return generar_xml_factura(json.loads(ruta.read_text(encoding="utf-8")), 1)


@pytest.mark.parametrize("ruta", EJEMPLOS, ids=lambda p: p.stem)
def test_la_raiz_declara_los_prefijos_de_la_guia_xml_de_sunat(ruta):
    raiz = etree.fromstring(_xml(ruta))
    for prefijo, uri in PREFIJOS.items():
        assert raiz.nsmap.get(prefijo) == uri, prefijo
    assert raiz.nsmap.get(None) == "urn:oasis:names:specification:ubl:schema:xsd:Invoice-2"


@pytest.mark.parametrize("ruta", EJEMPLOS, ids=lambda p: p.stem)
def test_se_puede_insertar_el_bloque_de_firma_sin_el_error_0306(ruta):
    texto = _xml(ruta).decode("utf-8")
    con_firma = texto.replace("<cbc:UBLVersionID>", BLOQUE_DE_FIRMA + "<cbc:UBLVersionID>", 1)
    assert con_firma != texto
    raiz = etree.fromstring(con_firma.encode("utf-8"))  # sin "prefix not bound"
    assert raiz.find("{%s}UBLExtensions" % PREFIJOS["ext"]) is not None


def test_el_xml_publicado_como_ejemplo_tambien_los_declara():
    raiz = etree.parse(str(RAIZ / "docs" / "ejemplos" / "F001-00000001.xml")).getroot()
    assert "ext" in raiz.nsmap and "ds" in raiz.nsmap
