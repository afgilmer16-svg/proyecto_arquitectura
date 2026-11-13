"""Verificación integral del generador XML (complementa a test_generador_xml.py).

Estas pruebas comprueban el proyecto "desde fuera", con oráculos que no dependen
del código que verifican:

1. Coherencia contrato ↔ código: los enums de ``facturacion-api.yaml`` y los
   catálogos del generador coinciden, y los ejemplos del contrato producen los
   mismos importes que el contrato publica.
2. Oráculo independiente: los totales se recalculan leyendo **solo el XML**,
   sin usar ``calculo.py``.
3. Pruebas por propiedades: 300 comprobantes aleatorios (semilla fija, es
   reproducible) deben generar XML válido contra el XSD y cuadrado.
4. Regresión: el ejemplo versionado ``F001-00000001.xml`` es idéntico a lo
   que genera hoy el código, y la generación es determinista.
5. Trazabilidad: cada elemento que emite el generador figura en la matriz 0005.
"""
from __future__ import annotations

import copy
import json
import random
import re
from decimal import ROUND_HALF_UP, Decimal
from pathlib import Path

import pytest
import yaml
from lxml import etree

from datetime import date

from facturacion_xml import DatosFacturaInvalidosError, ValidadorXsd, generar_xml_factura
from facturacion_xml import catalogos as cat

RAIZ = Path(__file__).resolve().parents[1]
CONTRATO = RAIZ / "specs" / "contracts" / "facturacion-api.yaml"
EJEMPLOS = RAIZ / "docs" / "ejemplos"
MATRIZ = RAIZ / "specs" / "features" / "0005-trazabilidad-sunat-ubl" / "spec.md"
NS = {
    "cac": "urn:oasis:names:specification:ubl:schema:xsd:CommonAggregateComponents-2",
    "cbc": "urn:oasis:names:specification:ubl:schema:xsd:CommonBasicComponents-2",
}
CENT = Decimal("0.01")


def r2(valor: Decimal) -> Decimal:
    return valor.quantize(CENT, rounding=ROUND_HALF_UP)


def dec(nodo, xpath) -> Decimal:
    valores = nodo.xpath(xpath, namespaces=NS)
    return sum((Decimal(v.text) for v in valores), Decimal("0"))


@pytest.fixture(scope="module")
def contrato():
    return yaml.safe_load(CONTRATO.read_text(encoding="utf-8"))


@pytest.fixture(scope="module")
def validador():
    return ValidadorXsd()


# ======================================================== 1. Contrato ↔ código
class TestCoherenciaContratoCodigo:
    def test_unidades_de_medida_iguales_al_enum_del_contrato(self, contrato):
        assert set(contrato["components"]["schemas"]["UnidadMedidaEnum"]["enum"]) == cat.UNIDADES_MEDIDA

    def test_cada_afectacion_del_contrato_se_genera_o_se_rechaza_explicitamente(self, contrato):
        enum = set(contrato["components"]["schemas"]["TipoAfectacionIgvEnum"]["enum"])
        cubiertas = set(cat.CATEGORIA_POR_AFECTACION) | set(cat.AFECTACIONES_NO_SOPORTADAS)
        assert enum == cubiertas, f"sin tratar: {enum - cubiertas}; sobrantes: {cubiertas - enum}"

    def test_sistemas_isc_iguales_al_catalogo_08_del_contrato(self, contrato):
        assert set(contrato["components"]["schemas"]["SistemaIscEnum"]["enum"]) == {s.value for s in cat.SistemaIsc}

    def test_tipos_de_comprobante_del_contrato(self, contrato):
        enum = set(contrato["components"]["schemas"]["TipoComprobanteEnum"]["enum"])
        assert enum == {t.value for t in cat.TipoComprobante} | set(cat.TIPOS_COMPROBANTE_OTRO_XSD)

    def test_ejemplo_del_contrato_produce_los_importes_que_publica_el_contrato(self, contrato, validador):
        operacion = contrato["paths"]["/comprobantes"]["post"]
        cuerpo = operacion["requestBody"]["content"]["application/json"]
        entrada = cuerpo.get("example") or next(iter(cuerpo["examples"].values()))["value"]
        salida = operacion["responses"]["201"]["content"]["application/json"]["example"]
        xml = generar_xml_factura(entrada, salida["correlativo"])
        assert validador.es_valido(xml)
        raiz = etree.fromstring(xml)
        assert raiz.findtext("cbc:ID", namespaces=NS) == f"{salida['serie']}-{salida['correlativo']:08d}"
        assert raiz.findtext("cac:LegalMonetaryTotal/cbc:PayableAmount", namespaces=NS) == salida["importe_total"]
        totales = salida["totales"]
        assert raiz.findtext("cac:LegalMonetaryTotal/cbc:LineExtensionAmount", namespaces=NS) == totales["total_valor_venta"]
        assert raiz.findtext("cac:TaxTotal/cbc:TaxAmount", namespaces=NS) == totales["total_impuestos"]
        for item in salida["items"]:
            linea = raiz.xpath(f"cac:InvoiceLine[cbc:ID='{item['numero_linea']}']", namespaces=NS)[0]
            assert linea.findtext("cbc:LineExtensionAmount", namespaces=NS) == item["valor_total"]
            igv = linea.xpath("cac:TaxTotal/cac:TaxSubtotal[cac:TaxCategory/cac:TaxScheme/cbc:ID='1000']/cbc:TaxAmount",
                              namespaces=NS)[0].text
            assert igv == item["monto_igv"]
            precio = linea.findtext("cac:PricingReference/cac:AlternativeConditionPrice/cbc:PriceAmount", namespaces=NS)
            assert Decimal(precio) == Decimal(item["precio_unitario"]), f"línea {item['numero_linea']}"

    def test_todos_los_ruc_de_ejemplo_del_contrato_son_validos(self):
        from facturacion_xml.modelo import ruc_valido
        texto = CONTRATO.read_text(encoding="utf-8")
        candidatos = set(re.findall(r'(?<![0-9])((?:10|15|16|17|20)[0-9]{9})(?![0-9])', texto))
        invalidos = sorted(r for r in candidatos if not ruc_valido(r))
        assert invalidos == []


# ======================================================== 2-3. Oráculo y propiedades
def factura_aleatoria(rng: random.Random) -> tuple[dict, int]:
    base = json.loads((EJEMPLOS / "factura-abarrotes.json").read_text(encoding="utf-8"))
    tipo = rng.choice(["01", "03"])
    base["tipo_comprobante"] = tipo
    base["serie"] = ("F" if tipo == "01" else "B") + rng.choice(["001", "002", "A01"])
    base["fecha_emision"] = f"{rng.choice([2020, 2022, 2024, 2026])}-{rng.randint(1, 12):02d}-{rng.randint(1, 28):02d}"
    base["moneda"] = rng.choice(["PEN", "USD"])
    if tipo == "03":
        base["receptor"] = {"tipo_documento": "1", "numero_documento": f"{rng.randint(10_000_000, 99_999_999)}",
                            "razon_social": "CLIENTE DE PRUEBA ÑANDÚ"}
    items = []
    for n in range(1, rng.randint(1, 8) + 1):
        afectacion = rng.choice(["10", "10", "10", "20", "30", "21", "31", "35"])
        item = {
            "numero_linea": n, "codigo_producto": f"P{n:03d}", "descripcion": f"Producto {n} de prueba",
            "unidad_medida": rng.choice(sorted(cat.UNIDADES_MEDIDA)),
            "cantidad": str(Decimal(rng.randint(1, 5000)) / Decimal(rng.choice([1, 10, 100, 1000]))),
            "valor_unitario": str(Decimal(rng.randint(1, 200_000)) / Decimal(rng.choice([100, 10_000]))),
            "tipo_afectacion_igv": afectacion,
        }
        if afectacion == "10" and rng.random() < 0.3:
            sistema = rng.choice(["01", "02", "03"])
            item["sistema_isc"] = sistema
            if sistema in ("01", "03"):
                item["tasa_isc"] = f"{rng.randint(1, 50)}.00"
            if sistema == "02":
                item["monto_fijo_isc"] = f"{rng.randint(1, 300) / 100:.2f}"
                item["unidad_base_isc"] = "LTR"
            if sistema == "03":
                item["precio_venta_publico_sugerido"] = f"{rng.randint(100, 5000) / 100:.2f}"
        if rng.random() < 0.2:
            item["cantidad_bolsas"] = rng.randint(1, 20)
        items.append(item)
    base["items"] = items
    return base, rng.randint(1, 99_999_999)


def _casos(n=300, semilla=20261111):
    rng = random.Random(semilla)
    return [factura_aleatoria(rng) for _ in range(n)]


CASOS = _casos()


@pytest.mark.parametrize("datos, correlativo", CASOS, ids=[f"caso{i:03d}" for i in range(len(CASOS))])
def test_propiedades_de_comprobantes_aleatorios(datos, correlativo, validador):
    try:
        xml = generar_xml_factura(datos, correlativo, hoy=date(2030, 1, 1))  # fechas aleatorias de 2026: no son "futuras"
    except DatosFacturaInvalidosError as exc:
        # Única causa válida de rechazo en estos datos: boleta > S/ 700 sin identificar (no ocurre: llevan DNI).
        pytest.fail(f"datos válidos rechazados: {exc}")
    raiz = etree.fromstring(xml)

    # P1. Válido contra el XSD oficial.
    assert validador.errores(xml) == []
    # P2. Identificación y conteo.
    assert raiz.findtext("cbc:ID", namespaces=NS) == f"{datos['serie']}-{correlativo:08d}"
    assert int(raiz.findtext("cbc:LineCountNumeric", namespaces=NS)) == len(datos["items"])
    assert raiz.xpath("cac:InvoiceLine/cbc:ID/text()", namespaces=NS) == [str(i) for i in range(1, len(datos["items"]) + 1)]
    # P3. Una sola moneda en todos los importes.
    assert {a.get("currencyID") for a in raiz.xpath("//*[@currencyID]")} == {datos["moneda"]}

    # P4. Oráculo independiente: recalcular desde el XML, no desde calculo.py.
    tasa_igv = Decimal("0.18")
    valor_venta_no_gratuito = Decimal("0")
    for linea, item in zip(raiz.findall("cac:InvoiceLine", NS), datos["items"]):
        cantidad, valor_unitario = Decimal(item["cantidad"]), Decimal(item["valor_unitario"])
        valor_venta = r2(cantidad * valor_unitario)
        assert Decimal(linea.findtext("cbc:LineExtensionAmount", namespaces=NS)) == valor_venta
        isc = dec(linea, "cac:TaxTotal/cac:TaxSubtotal[cac:TaxCategory/cac:TaxScheme/cbc:ID='2000']/cbc:TaxAmount")
        igv = dec(linea, "cac:TaxTotal/cac:TaxSubtotal[cac:TaxCategory/cac:TaxScheme/cbc:ID='1000']/cbc:TaxAmount")
        icbper = dec(linea, "cac:TaxTotal/cac:TaxSubtotal[cac:TaxCategory/cac:TaxScheme/cbc:ID='7152']/cbc:TaxAmount")
        if item["tipo_afectacion_igv"] == "10":
            assert igv == r2((valor_venta + isc) * tasa_igv)
            valor_venta_no_gratuito += valor_venta
        else:
            assert igv == 0
            if item["tipo_afectacion_igv"] in ("20", "30"):
                valor_venta_no_gratuito += valor_venta
            else:  # gratuita: valor referencial, sin contraprestación
                assert Decimal(linea.findtext("cac:Price/cbc:PriceAmount", namespaces=NS)) == 0
        bolsas = item.get("cantidad_bolsas") or 0
        anio = int(datos["fecha_emision"][:4])
        assert icbper == r2(Decimal(bolsas) * cat.importe_icbper(__import__("datetime").date(anio, 1, 1)))
        assert Decimal(linea.findtext("cac:TaxTotal/cbc:TaxAmount", namespaces=NS)) == igv + isc + icbper
        # El precio unitario con tributos reconstruye el importe de la línea.
        if item["tipo_afectacion_igv"] in ("10", "20", "30"):
            precio = Decimal(linea.findtext("cac:PricingReference/cac:AlternativeConditionPrice/cbc:PriceAmount", namespaces=NS))
            assert abs(precio * cantidad - (valor_venta + isc + igv)) <= CENT

    # P5. Cuadre de cabecera.
    tributos_lineas = dec(raiz, "cac:InvoiceLine/cac:TaxTotal/cbc:TaxAmount")
    tributos = Decimal(raiz.findtext("cac:TaxTotal/cbc:TaxAmount", namespaces=NS))
    assert tributos == tributos_lineas
    assert tributos == dec(raiz, "cac:TaxTotal/cac:TaxSubtotal/cbc:TaxAmount")
    valor_venta = Decimal(raiz.findtext("cac:LegalMonetaryTotal/cbc:LineExtensionAmount", namespaces=NS))
    assert valor_venta == valor_venta_no_gratuito
    pagar = Decimal(raiz.findtext("cac:LegalMonetaryTotal/cbc:PayableAmount", namespaces=NS))
    assert pagar == valor_venta + tributos
    # P6. Cada subtotal de cabecera es la suma de los subtotales de línea del mismo tributo.
    for codigo in ("1000", "2000", "9996", "9997", "9998", "7152"):
        cabecera = dec(raiz, f"cac:TaxTotal/cac:TaxSubtotal[cac:TaxCategory/cac:TaxScheme/cbc:ID='{codigo}']/cbc:TaxAmount")
        lineas = dec(raiz, f"cac:InvoiceLine/cac:TaxTotal/cac:TaxSubtotal[cac:TaxCategory/cac:TaxScheme/cbc:ID='{codigo}']/cbc:TaxAmount")
        assert cabecera == lineas, codigo
    # P7. El importe en letras corresponde al importe a pagar.
    centimos = f"{int((pagar - int(pagar)) * 100):02d}/100"
    assert centimos in raiz.findtext("cbc:Note[@languageLocaleID='1000']", namespaces=NS)


# ======================================================== 4. Regresión y determinismo
class TestRegresion:
    def test_ejemplo_versionado_igual_a_la_generacion_actual(self):
        datos = json.loads((EJEMPLOS / "factura-abarrotes.json").read_text(encoding="utf-8"))
        # Tolera CRLF: git en Windows (core.autocrlf) puede convertir los saltos de linea al clonar.
        crlf, lf = b"\r\n", b"\n"
        versionado = (EJEMPLOS / "F001-00000001.xml").read_bytes().replace(crlf, lf)
        assert generar_xml_factura(datos, 1).replace(crlf, lf) == versionado

    def test_generacion_determinista(self):
        datos = json.loads((EJEMPLOS / "factura-abarrotes.json").read_text(encoding="utf-8"))
        assert generar_xml_factura(copy.deepcopy(datos), 5) == generar_xml_factura(copy.deepcopy(datos), 5)

    def test_generar_no_modifica_los_datos_de_entrada(self):
        datos = json.loads((EJEMPLOS / "factura-abarrotes.json").read_text(encoding="utf-8"))
        copia = copy.deepcopy(datos)
        generar_xml_factura(datos, 1)
        assert datos == copia


# ======================================================== 5. Trazabilidad con la matriz 0005
def test_cada_elemento_generado_figura_en_la_matriz_0005():
    datos = json.loads((EJEMPLOS / "factura-abarrotes.json").read_text(encoding="utf-8"))
    raiz = etree.fromstring(generar_xml_factura(datos, 1))
    matriz = MATRIZ.read_text(encoding="utf-8")
    nombres = {etree.QName(e).localname for e in raiz.iter()} - {"Invoice"}
    # Elementos estructurales de firma: los documenta la Guía XML (p. 80), no la matriz de datos.
    estructurales = {"Signature", "SignatoryParty", "DigitalSignatureAttachment", "ExternalReference", "URI"}
    faltan = sorted(n for n in nombres - estructurales if n not in matriz)
    assert faltan == [], f"elementos generados sin trazabilidad en la matriz 0005: {faltan}"
