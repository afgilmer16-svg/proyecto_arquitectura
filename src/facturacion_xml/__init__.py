"""Generación de XML UBL 2.1 (Invoice) para la factura y boleta electrónica peruana.

Uso típico::

    from facturacion_xml import generar_xml_factura

    xml = generar_xml_factura(datos, correlativo=1)   # bytes UTF-8, validado contra el XSD

``datos`` sigue el DTO ``EmitirComprobanteRequest`` de
``specs/contracts/facturacion-api.yaml``. Ver ``docs/documentacion-tecnica-xml.md``.
"""
from __future__ import annotations

from datetime import date
from typing import Any

from .catalogos import CategoriaOperacion, RegimenIgv, SistemaIsc, TipoComprobante
from .errores import (
    DatosFacturaInvalidosError,
    ErrorFacturacionXml,
    EsquemaXsdIncompletoError,
    FirmaDigitalError,
    XmlNoConformeXsdError,
)
from .firmador import firmar_xml, verificar_firma
from .generador_ubl import GeneradorFacturaUbl
from .modelo import Factura, factura_desde_dict
from .validador_xsd import ValidadorXsd

__all__ = [
    "CategoriaOperacion", "DatosFacturaInvalidosError", "ErrorFacturacionXml", "EsquemaXsdIncompletoError",
    "Factura", "FirmaDigitalError", "GeneradorFacturaUbl", "RegimenIgv", "SistemaIsc", "TipoComprobante", "ValidadorXsd",
    "XmlNoConformeXsdError", "factura_desde_dict", "firmar_xml", "generar_xml_factura", "verificar_firma",
]


def generar_xml_factura(datos: dict[str, Any], correlativo: int,
                        regimen_igv: RegimenIgv | str = RegimenIgv.GENERAL,
                        hoy: date | None = None) -> bytes:
    """Valida los datos, genera el XML UBL 2.1 y lo valida contra ``UBL-Invoice-2.1.xsd``.

    ``hoy`` fija la fecha de referencia para rechazar emisiones futuras (por defecto, hoy en Lima).
    """
    return GeneradorFacturaUbl().generar_desde_dict(datos, correlativo, regimen_igv, hoy)
