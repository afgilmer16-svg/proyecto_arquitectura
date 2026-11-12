"""Los archivos derivados (.mmd, .csv) deben coincidir con sus fuentes en Markdown.

Si alguien edita un diagrama o la matriz en el .md y olvida regenerar, estas pruebas fallan:
    python tools/extraer_diagramas.py
    python tools/exportar_matriz.py
"""
import importlib.util
from pathlib import Path

RAIZ = Path(__file__).resolve().parents[1]


def _cargar(nombre: str):
    espec = importlib.util.spec_from_file_location(nombre, RAIZ / "tools" / f"{nombre}.py")
    modulo = importlib.util.module_from_spec(espec)
    espec.loader.exec_module(modulo)
    return modulo


def test_los_diagramas_mmd_estan_al_dia():
    assert _cargar("extraer_diagramas").main(["--verificar"]) == 0


def test_los_csv_de_la_matriz_estan_al_dia():
    assert _cargar("exportar_matriz").main(["--verificar"]) == 0


def test_cada_diagrama_mermaid_tiene_un_tipo_conocido():
    tipos = ("flowchart", "graph", "sequenceDiagram", "stateDiagram", "classDiagram", "erDiagram", "gantt")
    for ruta in (RAIZ / "diagramas" / "fuentes").glob("*.mmd"):
        primera = next(l for l in ruta.read_text(encoding="utf-8").splitlines() if l.strip() and not l.startswith("%%"))
        assert primera.strip().startswith(tipos), (ruta.name, primera)


def test_la_matriz_csv_cubre_las_secciones_3_a_14():
    indice = (RAIZ / "specs" / "trazabilidad" / "csv" / "00-indice.csv").read_text(encoding="utf-8-sig")
    secciones = {int(l.split(",")[1]) for l in indice.splitlines()[1:]}
    assert secciones == set(range(3, 15))
