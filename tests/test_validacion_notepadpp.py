# Autor: Gilmer Ayala Falcón · UNAS-FIIS · Arquitectura de Software · Grupo A
"""La carpeta para Notepad++ (maindoc/ + common/) debe poder compilar el XSD y validar los XML de referencia."""
import sys
from pathlib import Path

import pytest
from lxml import etree

RAIZ = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(RAIZ / "tools"))
import preparar_validacion_notepadpp as herramienta  # noqa: E402


@pytest.fixture(scope="module")
def carpeta(tmp_path_factory):
    destino = tmp_path_factory.mktemp("notepadpp") / "validacion"
    herramienta.preparar(destino)
    return destino


def test_tiene_la_estructura_oficial(carpeta):
    assert (carpeta / "maindoc" / "UBL-Invoice-2.1.xsd").is_file()
    assert any((carpeta / "common").glob("UBL-CommonBasicComponents-2.1.xsd"))
    assert not (carpeta / "common" / "UBL-Invoice-2.1.xsd").exists()
    assert len(list((carpeta / "ejemplos").glob("*.xml"))) >= 6


def test_el_xsd_principal_compila_desde_esa_carpeta(carpeta):
    etree.XMLSchema(etree.parse(str(carpeta / "maindoc" / "UBL-Invoice-2.1.xsd")))


def test_todos_los_xml_de_ejemplo_validan(carpeta):
    esquema = etree.XMLSchema(etree.parse(str(carpeta / "maindoc" / "UBL-Invoice-2.1.xsd")))
    for xml in sorted((carpeta / "ejemplos").glob("*.xml")):
        assert esquema.validate(etree.parse(str(xml))), f"{xml.name}: {esquema.error_log.last_error}"


def test_se_puede_regenerar_encima_de_una_carpeta_existente(carpeta):
    (carpeta / "sobrante.txt").write_text("x", encoding="utf-8")
    herramienta.preparar(carpeta)
    assert not (carpeta / "sobrante.txt").exists()
