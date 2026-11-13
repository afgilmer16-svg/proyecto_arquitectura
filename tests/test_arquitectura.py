"""Reglas de arquitectura del generador (equivalente a ArchUnit en Python).

El generador sigue el diseno de la feature 0008 y el ADR-005: un nucleo de reglas sin dependencias
de infraestructura (catalogos, modelo, calculo, numero_letras) y dos adaptadores que si usan lxml
(generador_ubl y validador_xsd). Estas pruebas fallan si alguien rompe esa separacion.
"""
import ast
from pathlib import Path

import pytest

PAQUETE = Path(__file__).resolve().parents[1] / "src" / "facturacion_xml"
MODULOS = sorted(p.stem for p in PAQUETE.glob("*.py") if p.stem not in {"__init__", "__main__"})

NUCLEO = {"catalogos", "modelo", "calculo", "numero_letras", "errores"}
ADAPTADORES_LXML = {"generador_ubl", "validador_xsd", "firmador"}
# modulo -> modulos internos que NO puede importar
PROHIBIDOS = {
    "catalogos": {"modelo", "calculo", "generador_ubl", "validador_xsd", "numero_letras"},
    "errores": {"catalogos", "modelo", "calculo", "generador_ubl", "validador_xsd", "numero_letras"},
    "numero_letras": {"modelo", "calculo", "generador_ubl", "validador_xsd"},
    "modelo": {"generador_ubl", "validador_xsd"},
    "calculo": {"generador_ubl", "validador_xsd"},
    "validador_xsd": {"modelo", "calculo", "generador_ubl", "catalogos"},
    "firmador": {"modelo", "calculo", "generador_ubl", "catalogos", "validador_xsd", "numero_letras"},
}
EXTERNOS_PROHIBIDOS_EN_NUCLEO = {"lxml", "xml", "requests", "sqlalchemy", "psycopg2", "flask", "django"}


def _importaciones(modulo: str) -> tuple[set[str], set[str]]:
    """(modulos internos importados, paquetes externos importados) de un archivo."""
    arbol = ast.parse((PAQUETE / f"{modulo}.py").read_text(encoding="utf-8"))
    internos, externos = set(), set()
    for nodo in ast.walk(arbol):
        if isinstance(nodo, ast.ImportFrom):
            if nodo.level >= 1:  # from . import x / from .x import y
                if nodo.module:
                    internos.add(nodo.module.split(".")[0])
                else:
                    internos.update(a.name for a in nodo.names)
            elif nodo.module:
                externos.add(nodo.module.split(".")[0])
        elif isinstance(nodo, ast.Import):
            externos.update(a.name.split(".")[0] for a in nodo.names)
    return internos & set(MODULOS), externos


def test_se_conocen_todos_los_modulos():
    assert NUCLEO | ADAPTADORES_LXML == set(MODULOS)


@pytest.mark.parametrize("modulo", sorted(NUCLEO))
def test_el_nucleo_no_depende_de_infraestructura(modulo):
    _, externos = _importaciones(modulo)
    assert not externos & EXTERNOS_PROHIBIDOS_EN_NUCLEO, externos


def test_solo_los_adaptadores_importan_lxml():
    con_lxml = {m for m in MODULOS if "lxml" in _importaciones(m)[1]}
    assert con_lxml == ADAPTADORES_LXML


@pytest.mark.parametrize("modulo,prohibidos", sorted(PROHIBIDOS.items()))
def test_dependencias_prohibidas_entre_modulos(modulo, prohibidos):
    internos, _ = _importaciones(modulo)
    assert not internos & prohibidos, internos & prohibidos


def test_el_validador_xsd_es_independiente_del_dominio():
    internos, _ = _importaciones("validador_xsd")
    assert internos <= {"errores"}


def _aristas_de_carga(modulo: str) -> set[str]:
    """Importaciones de nivel superior (las que se ejecutan al cargar el modulo)."""
    arbol = ast.parse((PAQUETE / f"{modulo}.py").read_text(encoding="utf-8"))
    aristas = set()
    for nodo in arbol.body:
        if isinstance(nodo, ast.ImportFrom) and nodo.level >= 1:
            aristas.update([nodo.module.split(".")[0]] if nodo.module else [a.name for a in nodo.names])
    return aristas & set(MODULOS)


def test_no_hay_ciclos_entre_modulos():
    # modelo.py importa calculo dentro de una funcion para evitar el ciclo: esa arista no es de carga.
    visitado, pila = set(), []

    def visitar(m):
        assert m not in pila, f"ciclo: {' -> '.join(pila + [m])}"
        if m in visitado:
            return
        pila.append(m)
        for v in _aristas_de_carga(m):
            visitar(v)
        pila.pop()
        visitado.add(m)

    for m in MODULOS:
        visitar(m)


@pytest.mark.parametrize("modulo", sorted(NUCLEO))
def test_los_importes_nunca_se_calculan_con_float(modulo):
    arbol = ast.parse((PAQUETE / f"{modulo}.py").read_text(encoding="utf-8"))
    literales = [n.lineno for n in ast.walk(arbol) if isinstance(n, ast.Constant) and isinstance(n.value, float)]
    llamadas = [n.lineno for n in ast.walk(arbol)
                if isinstance(n, ast.Call) and isinstance(n.func, ast.Name) and n.func.id == "float"]
    assert not literales and not llamadas, f"{modulo}: float en lineas {literales + llamadas}"
