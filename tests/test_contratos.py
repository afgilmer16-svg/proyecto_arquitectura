# Autor: Gilmer Ayala Falcón · UNAS-FIIS · Arquitectura de Software · Grupo A
"""Pruebas de los contratos Spec-First.

No prueban la lógica del generador (eso está en test_generador_xml.py): verifican que las
especificaciones sean válidas y consistentes entre sí.

Ejecutar desde la raíz del proyecto:
    pip install -r requirements.txt
    pytest -v
"""
import json
import re
from pathlib import Path

import pytest
import yaml
from jsonschema import Draft202012Validator
from openapi_spec_validator import validate
from referencing import Registry, Resource

RAIZ = Path(__file__).resolve().parents[1]
CONTRATO = RAIZ / "specs" / "contracts" / "facturacion-api.yaml"
FEATURES = RAIZ / "specs" / "features"
METODOS = {"get", "post", "put", "patch", "delete"}


@pytest.fixture(scope="module")
def spec():
    return yaml.safe_load(CONTRATO.read_text(encoding="utf-8"))


def _operaciones(spec):
    for ruta, item in spec["paths"].items():
        for metodo, op in item.items():
            if metodo in METODOS:
                yield ruta, metodo, op


def _refs(nodo):
    if isinstance(nodo, dict):
        for clave, valor in nodo.items():
            if clave == "$ref" and isinstance(valor, str):
                yield valor
            yield from _refs(valor)
    elif isinstance(nodo, list):
        for valor in nodo:
            yield from _refs(valor)


def test_openapi_31_valido(spec):
    assert spec["openapi"].startswith("3.1")
    validate(spec)


def test_todos_los_ref_existen(spec):
    rotos = []
    for ref in _refs(spec):
        nodo = spec
        try:
            for parte in ref.removeprefix("#/").split("/"):
                nodo = nodo[parte.replace("~1", "/").replace("~0", "~")]
        except (KeyError, TypeError):
            rotos.append(ref)
    assert rotos == []


def test_operation_id_unico_y_con_caso_de_uso(spec):
    ids = []
    for ruta, metodo, op in _operaciones(spec):
        assert op.get("operationId"), f"{metodo.upper()} {ruta} sin operationId"
        texto = op.get("summary", "") + op.get("description", "")
        assert re.search(r"CU-\d{2}", texto), f"{op['operationId']} no cita su caso de uso"
        ids.append(op["operationId"])
    assert len(ids) == len(set(ids))


def test_cada_caso_de_uso_tiene_endpoint(spec):
    texto = json.dumps(spec, ensure_ascii=False)
    faltan = [f"CU-{n:02d}" for n in range(1, 17) if f"CU-{n:02d}" not in texto]
    assert faltan == []


def test_ejemplos_cumplen_su_schema(spec):
    componentes = json.loads(
        json.dumps(spec.get("components", {})).replace('"#/components', '"urn:oas#/components')
    )
    registro = Registry().with_resource("urn:oas", Resource.from_contents(
        {"$schema": "https://json-schema.org/draft/2020-12/schema", "components": componentes}))
    for ruta, metodo, op in _operaciones(spec):
        for contenido in op.get("requestBody", {}).get("content", {}).values():
            esquema = json.loads(json.dumps(contenido["schema"]).replace('"#/components', '"urn:oas#/components'))
            ejemplos = [contenido["example"]] if "example" in contenido else [
                e["value"] for e in contenido.get("examples", {}).values() if "value" in e]
            for ejemplo in ejemplos:
                errores = list(Draft202012Validator(esquema, registry=registro).iter_errors(ejemplo))
                assert errores == [], f"{metodo.upper()} {ruta}: {errores[0].message}"


def test_catalogo_08_isc(spec):
    texto = CONTRATO.read_text(encoding="utf-8")
    assert "sistema_isc" in texto
    for codigo in ("01", "02", "03"):
        assert f'"{codigo}"' in texto


@pytest.mark.parametrize("feature", sorted(p.name for p in FEATURES.iterdir() if p.is_dir()))
def test_cada_feature_tiene_spec_design_tasks(feature):
    for archivo in ("spec.md", "design.md", "tasks.md"):
        assert (FEATURES / feature / archivo).is_file(), f"{feature}/{archivo} no existe"


def test_enlaces_markdown_no_rotos():
    rotos = []
    for md in RAIZ.rglob("*.md"):
        if ".git" in md.parts:
            continue
        for destino in re.findall(r"\]\(([^)\s]+)\)", md.read_text(encoding="utf-8")):
            ruta = destino.split("#")[0]
            if not ruta or re.match(r"(https?|mailto):", ruta):
                continue
            if not (md.parent / ruta).exists():
                rotos.append(f"{md.relative_to(RAIZ)} -> {destino}")
    assert rotos == []


def test_src_solo_contiene_el_generador_autorizado():
    """Spec-First: el único código de producción autorizado es el generador XML (feature 0008)."""
    permitidos = {"README.md", ".gitkeep"}
    archivos = [p for p in (RAIZ / "src").rglob("*")
                if p.is_file() and p.name not in permitidos and "__pycache__" not in p.parts]
    fuera = [str(p.relative_to(RAIZ)) for p in archivos if p.parts[len(RAIZ.parts) + 1] != "facturacion_xml"]
    assert fuera == [], f"código fuera del alcance autorizado: {fuera}"
