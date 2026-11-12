"""Exporta las tablas de la matriz de trazabilidad (specs/features/0005/spec.md) a CSV.

Uso:  python tools/exportar_matriz.py [--verificar]
Genera un CSV por seccion numerada (3 a 14) en specs/trazabilidad/csv/ (UTF-8 con BOM, para Excel)
y un indice. La fuente es el .md; los CSV no se editan a mano.
"""
from __future__ import annotations

import csv
import io
import re
import sys
import unicodedata
from pathlib import Path

RAIZ = Path(__file__).resolve().parents[1]
FUENTE = RAIZ / "specs" / "features" / "0005-trazabilidad-sunat-ubl" / "spec.md"
DESTINO = RAIZ / "specs" / "trazabilidad" / "csv"
SECCION = re.compile(r"^## (\d+)\. (.+)$", re.M)


def _slug(texto: str) -> str:
    texto = unicodedata.normalize("NFKD", texto).encode("ascii", "ignore").decode()
    return re.sub(r"[^a-z0-9]+", "-", texto.lower()).strip("-")[:45]


def _celdas(linea: str) -> list[str]:
    interior = linea.strip().strip("|")
    barra = chr(92)  # '\' (las barras escapadas "\|" de Markdown no separan celdas)
    celdas = re.split("(?<!" + re.escape(barra) + ")" + re.escape("|"), interior)
    return [c.strip().replace(barra + "|", "|") for c in celdas]


def _tablas(cuerpo: str) -> list[list[list[str]]]:
    tablas, actual = [], []
    for linea in cuerpo.splitlines() + [""]:
        if linea.lstrip().startswith("|"):
            actual.append(linea)
        elif actual:
            filas = [_celdas(l) for l in actual if not re.match(r"^\s*\|[\s:|-]+\|\s*$", l)]
            if len(filas) >= 2:
                tablas.append(filas)
            actual = []
    return tablas


def exportar() -> dict[str, str]:
    texto = FUENTE.read_text(encoding="utf-8")
    marcas = list(SECCION.finditer(texto))
    salida: dict[str, str] = {}
    indice = io.StringIO()
    esc = csv.writer(indice, lineterminator="\n")
    esc.writerow(["archivo", "seccion", "titulo", "filas"])
    for i, m in enumerate(marcas):
        numero = int(m.group(1))
        if not 3 <= numero <= 14:
            continue
        fin = marcas[i + 1].start() if i + 1 < len(marcas) else len(texto)
        tablas = _tablas(texto[m.end():fin])
        if not tablas:
            continue
        buf = io.StringIO()
        w = csv.writer(buf, lineterminator="\n")
        for t, filas in enumerate(tablas):
            if t:
                w.writerow([])
            w.writerows(filas)
        nombre = f"{numero:02d}-{_slug(m.group(2))}.csv"
        salida[nombre] = buf.getvalue()
        esc.writerow([nombre, numero, m.group(2), sum(len(f) - 1 for f in tablas)])
    salida["00-indice.csv"] = indice.getvalue()
    return salida


def main(argv: list[str]) -> int:
    esperado = exportar()
    if "--verificar" in argv:
        actual = {p.name: p.read_bytes().decode("utf-8-sig").replace("\r\n", "\n") for p in DESTINO.glob("*.csv")}
        if actual != esperado:
            print("Los CSV no coinciden con la matriz: ejecuta python tools/exportar_matriz.py", file=sys.stderr)
            return 1
        print(f"{len(esperado)} CSV al dia")
        return 0
    DESTINO.mkdir(parents=True, exist_ok=True)
    for viejo in DESTINO.glob("*.csv"):
        viejo.unlink()
    for nombre, contenido in esperado.items():
        (DESTINO / nombre).write_bytes(contenido.encode("utf-8-sig"))
    print(f"{len(esperado)} CSV escritos en {DESTINO.relative_to(RAIZ)}")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
