"""Extrae los diagramas Mermaid de los .md a archivos .mmd (fuente unica de cada diagrama).

Uso:  python tools/extraer_diagramas.py [--verificar]
  sin opciones   escribe diagramas/fuentes/NN-<documento>-<titulo>.mmd
  --verificar    no escribe; falla (codigo 1) si algun .mmd no coincide con su .md de origen

Para renderizar a SVG/PNG (opcional, requiere Node.js):
  npx.cmd @mermaid-js/mermaid-cli -i diagramas/fuentes/<archivo>.mmd -o diagramas/svg/<archivo>.svg
"""
from __future__ import annotations

import re
import sys
import unicodedata
from pathlib import Path

RAIZ = Path(__file__).resolve().parents[1]
DESTINO = RAIZ / "diagramas" / "fuentes"
BLOQUE = re.compile(r"^```mermaid\r?\n(.*?)^```", re.S | re.M)


def _slug(texto: str) -> str:
    texto = unicodedata.normalize("NFKD", texto).encode("ascii", "ignore").decode()
    return re.sub(r"[^a-z0-9]+", "-", texto.lower()).strip("-")[:50] or "diagrama"


def _documentos() -> list[Path]:
    docs = [p for p in RAIZ.rglob("*.md") if ".git" not in p.parts and "node_modules" not in p.parts]
    return sorted(docs)


def diagramas() -> dict[str, str]:
    """nombre de archivo -> contenido, en orden estable."""
    salida: dict[str, str] = {}
    n = 0
    for doc in _documentos():
        texto = doc.read_text(encoding="utf-8")
        for m in BLOQUE.finditer(texto):
            n += 1
            titulos = re.findall(r"^#{1,4} (.+)$", texto[:m.start()], re.M)
            titulo = _slug(titulos[-1])[:32].strip("-") if titulos else "diagrama"
            origen = _slug(f"{doc.parent.name}-{doc.stem}")[:36].strip("-")
            salida[f"{n:02d}-{origen}-{titulo}.mmd"] = m.group(1).replace("\r\n", "\n")
    return salida


def main(argv: list[str]) -> int:
    esperado = diagramas()
    if "--verificar" in argv:
        actual = {p.name: p.read_text(encoding="utf-8").replace("\r\n", "\n") for p in DESTINO.glob("*.mmd")}
        if actual != esperado:
            sobran, faltan = sorted(set(actual) - set(esperado)), sorted(set(esperado) - set(actual))
            distintos = sorted(k for k in set(actual) & set(esperado) if actual[k] != esperado[k])
            print(f"Desactualizado. sobran={sobran} faltan={faltan} distintos={distintos}", file=sys.stderr)
            return 1
        print(f"{len(esperado)} diagramas .mmd al dia")
        return 0
    DESTINO.mkdir(parents=True, exist_ok=True)
    for viejo in DESTINO.glob("*.mmd"):
        viejo.unlink()
    for nombre, contenido in esperado.items():
        (DESTINO / nombre).write_text(contenido, encoding="utf-8", newline="\n")
    print(f"{len(esperado)} diagramas escritos en {DESTINO.relative_to(RAIZ)}")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
