# Autor: Gilmer Ayala Falcón · UNAS-FIIS · Arquitectura de Software · Grupo A
"""Arma la carpeta para validar un XML con Notepad++ (plugin XML Tools).

El XSD ``UBL-Invoice-2.1.xsd`` importa sus componentes con rutas relativas (``../common/...``), así que no se
puede validar desde la carpeta plana ``docs/referencias/ubl``. Este script copia los XSD a la estructura oficial::

    docs/validacion-notepadpp/
        maindoc/UBL-Invoice-2.1.xsd
        common/*.xsd
        ejemplos/*.xml          (los XML de referencia, con la marca «FIRMAR AQUÍ»)

Uso (desde la raíz del repositorio)::

    python tools/preparar_validacion_notepadpp.py
"""
from __future__ import annotations

import shutil
from pathlib import Path

RAIZ = Path(__file__).resolve().parents[1]
ORIGEN = RAIZ / "docs" / "referencias" / "ubl"
DESTINO = RAIZ / "docs" / "validacion-notepadpp"
PRINCIPAL = "UBL-Invoice-2.1.xsd"
NO_COMUNES = {PRINCIPAL, "UBL-CreditNote-2.1.xsd", "UBL-DebitNote-2.1.xsd"}


def preparar(destino: Path = DESTINO, origen: Path = ORIGEN) -> Path:
    """Crea ``maindoc/``, ``common/`` y ``ejemplos/`` dentro de ``destino`` y devuelve la ruta del XSD principal."""
    if destino.exists():
        shutil.rmtree(destino)
    (destino / "maindoc").mkdir(parents=True)
    (destino / "common").mkdir()
    (destino / "ejemplos").mkdir()
    shutil.copy2(origen / PRINCIPAL, destino / "maindoc" / PRINCIPAL)
    for xsd in sorted(origen.glob("*.xsd")):
        if xsd.name not in NO_COMUNES:
            shutil.copy2(xsd, destino / "common" / xsd.name)
    for xml in sorted((RAIZ / "specs" / "ejemplos" / "xml-referencia").glob("*.xml")):
        shutil.copy2(xml, destino / "ejemplos" / xml.name)
    return destino / "maindoc" / PRINCIPAL


if __name__ == "__main__":
    xsd = preparar()
    print(f"Listo. XSD principal para Notepad++: {xsd}")
