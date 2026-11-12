"""Validación del XML contra el XSD oficial ``UBL-Invoice-2.1.xsd``.

Los esquemas están en ``docs/referencias/ubl/`` (OASIS UBL 2.1 OS, 04-nov-2013)
y no se modifican. En la distribución oficial, ``UBL-Invoice-2.1.xsd`` está
en ``xsd/maindoc/`` e importa ``../common/...``; aquí todos los archivos están
en una sola carpeta, así que un *resolver* de lxml traduce cada
``schemaLocation`` al archivo local con el mismo nombre. No se descarga nada
de internet.
"""
from __future__ import annotations

import os
from functools import lru_cache
from pathlib import Path

from lxml import etree

from .errores import EsquemaXsdIncompletoError, XmlNoConformeXsdError

RAIZ_PROYECTO = Path(__file__).resolve().parents[2]
DIRECTORIO_XSD = RAIZ_PROYECTO / "docs" / "referencias" / "ubl"
XSD_INVOICE = "UBL-Invoice-2.1.xsd"

# Archivos que el XSD de Invoice necesita, directa o indirectamente.
ARCHIVOS_ESQUEMA = (
    "UBL-Invoice-2.1.xsd",
    "UBL-CommonAggregateComponents-2.1.xsd",
    "UBL-CommonBasicComponents-2.1.xsd",
    "UBL-CommonExtensionComponents-2.1.xsd",
    "UBL-ExtensionContentDataType-2.1.xsd",
    "UBL-CommonSignatureComponents-2.1.xsd",
    "UBL-SignatureAggregateComponents-2.1.xsd",
    "UBL-SignatureBasicComponents-2.1.xsd",
    "UBL-QualifiedDataTypes-2.1.xsd",
    "UBL-UnqualifiedDataTypes-2.1.xsd",
    "CCTS_CCT_SchemaModule-2.1.xsd",
    "UBL-xmldsig-core-schema-2.1.xsd",
    "UBL-XAdESv132-2.1.xsd",
    "UBL-XAdESv141-2.1.xsd",
)


class _ResolverLocal(etree.Resolver):
    def __init__(self, directorio: Path):
        super().__init__()
        self.directorio = directorio

    def resolve(self, url, pubid, context):  # noqa: D401 - firma de lxml
        local = self.directorio / os.path.basename(url)
        if local.is_file():
            return self.resolve_filename(str(local), context)
        return None


@lru_cache(maxsize=4)
def _cargar_esquema(directorio: str) -> etree.XMLSchema:
    carpeta = Path(directorio)
    faltantes = [f for f in ARCHIVOS_ESQUEMA if not (carpeta / f).is_file()]
    if faltantes:
        raise EsquemaXsdIncompletoError(
            f"Faltan archivos del esquema UBL 2.1 en {carpeta}: {', '.join(faltantes)}. "
            "Descárgalos de https://docs.oasis-open.org/ubl/os-UBL-2.1/xsd/ (ver docs/referencias/ubl/README.md)."
        )
    parser = etree.XMLParser(no_network=True)
    parser.resolvers.add(_ResolverLocal(carpeta))
    try:
        return etree.XMLSchema(etree.parse(str(carpeta / XSD_INVOICE), parser))
    except etree.XMLSchemaParseError as exc:  # pragma: no cover - depende de los archivos
        raise EsquemaXsdIncompletoError(f"No se pudo compilar {XSD_INVOICE}: {exc}") from exc


class ValidadorXsd:
    """Valida un XML ``Invoice`` contra ``UBL-Invoice-2.1.xsd``."""

    def __init__(self, directorio_xsd: Path | str = DIRECTORIO_XSD):
        self.directorio_xsd = str(directorio_xsd)

    @property
    def esquema(self) -> etree.XMLSchema:
        return _cargar_esquema(self.directorio_xsd)

    def errores(self, xml: bytes | str | etree._Element) -> list[str]:
        """Lista de errores del XML (vacía si es válido)."""
        if isinstance(xml, etree._Element):
            documento = xml
        else:
            datos = xml.encode("utf-8") if isinstance(xml, str) else xml
            try:
                documento = etree.fromstring(datos, etree.XMLParser(no_network=True, resolve_entities=False))
            except etree.XMLSyntaxError as exc:
                return [f"XML mal formado: {exc}"]
        esquema = self.esquema
        if esquema.validate(documento):
            return []
        return [f"línea {e.line}: {e.message}" for e in esquema.error_log]

    def es_valido(self, xml: bytes | str | etree._Element) -> bool:
        return not self.errores(xml)

    def validar(self, xml: bytes | str | etree._Element) -> None:
        """Lanza :class:`XmlNoConformeXsdError` si el XML no cumple el XSD."""
        errores = self.errores(xml)
        if errores:
            raise XmlNoConformeXsdError(errores)
