"""Control de forma (JSON Schema) y ejemplos validos/invalidos del generador.

- Cada ejemplo de ``specs/ejemplos/validos`` cumple el esquema y genera un XML valido contra el XSD.
- Cada ejemplo de ``specs/ejemplos/invalidos`` es rechazado y con el motivo esperado.
- El esquema y el modelo en Python no se contradicen: lo que el esquema acepta no lo rechaza el
  modelo por la forma, y lo que el esquema rechaza tampoco lo acepta el modelo.
"""
import json
from pathlib import Path

import pytest
from jsonschema import Draft202012Validator

from facturacion_xml import generar_xml_factura
from facturacion_xml.errores import DatosFacturaInvalidosError

RAIZ = Path(__file__).resolve().parents[1]
ESQUEMA = RAIZ / "specs" / "schemas" / "factura-entrada.schema.json"
VALIDOS = sorted((RAIZ / "specs" / "ejemplos" / "validos").glob("*.json"))
INVALIDOS = sorted((RAIZ / "specs" / "ejemplos" / "invalidos").glob("*.json"))
EJEMPLO_PARCIAL = RAIZ / "docs" / "segundo-parcial" / "ejemplos" / "factura-abarrotes.json"

# archivo -> (lo rechaza el esquema JSON?, texto que debe aparecer en el error del modelo)
INVALIDOS_ESPERADOS = {
    "rechazo-01-boleta-serie-f": (True, "serie"),
    "rechazo-02-factura-sin-ruc-del-cliente": (True, "receptor.tipo_documento"),
    "rechazo-03-ruc-emisor-mal-digito": (False, "emisor.ruc"),
    "rechazo-04-unidad-de-medida-desconocida": (True, "unidad_medida"),
    "rechazo-05-importe-numerico": (True, "valor_unitario"),
    "rechazo-06-credito-sin-cuotas": (True, "forma_pago"),
    "rechazo-07-isc-sin-tasa": (True, "tasa_isc"),
    "rechazo-08-correlativo-en-la-entrada": (True, "correlativo"),
    "rechazo-09-afectacion-ivap": (True, "tipo_afectacion_igv"),
    "rechazo-10-boleta-grande-sin-comprador": (False, "boleta"),
    "rechazo-11-cuotas-no-suman": (False, "cuotas"),
    "rechazo-12-cuota-vencida": (False, "fecha_vencimiento"),
}


def _cargar(ruta: Path) -> dict:
    return json.loads(ruta.read_text(encoding="utf-8"))


@pytest.fixture(scope="module")
def validador() -> Draft202012Validator:
    esquema = _cargar(ESQUEMA)
    Draft202012Validator.check_schema(esquema)
    return Draft202012Validator(esquema, format_checker=Draft202012Validator.FORMAT_CHECKER)


def test_el_esquema_es_un_json_schema_2020_12_valido():
    Draft202012Validator.check_schema(_cargar(ESQUEMA))


def test_hay_ejemplos_suficientes():
    assert len(VALIDOS) >= 4
    assert {p.stem for p in INVALIDOS} == set(INVALIDOS_ESPERADOS)


@pytest.mark.parametrize("ruta", VALIDOS + [EJEMPLO_PARCIAL], ids=lambda p: p.stem)
def test_ejemplo_valido_cumple_el_esquema_y_genera_xml(ruta, validador):
    datos = _cargar(ruta)
    errores = [e.message for e in validador.iter_errors(datos)]
    assert not errores, errores
    xml = generar_xml_factura(datos, 1)  # valida contra UBL-Invoice-2.1.xsd antes de devolver
    assert xml.startswith(b"<?xml")


@pytest.mark.parametrize("ruta", INVALIDOS, ids=lambda p: p.stem)
def test_ejemplo_invalido_es_rechazado(ruta, validador):
    rechaza_esquema, texto = INVALIDOS_ESPERADOS[ruta.stem]
    datos = _cargar(ruta)
    errores_esquema = list(validador.iter_errors(datos))
    assert bool(errores_esquema) == rechaza_esquema, [e.message for e in errores_esquema]
    with pytest.raises(DatosFacturaInvalidosError) as exc:
        generar_xml_factura(datos, 1)
    assert texto in str(exc.value)


def test_el_correlativo_no_esta_en_el_esquema():
    esquema = _cargar(ESQUEMA)
    assert "correlativo" not in esquema["properties"]
    assert esquema["additionalProperties"] is False


def test_el_esquema_no_admite_numeros_para_importes():
    esquema = _cargar(ESQUEMA)
    def tipos(nodo):
        if isinstance(nodo, dict):
            if nodo.get("type") in ("number", "integer") and nodo is not None:
                yield nodo["type"]
            for v in nodo.values():
                yield from tipos(v)
        elif isinstance(nodo, list):
            for v in nodo:
                yield from tipos(v)

    assert "number" not in set(tipos(esquema))  # los importes son texto; solo hay enteros de conteo
