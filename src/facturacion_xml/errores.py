# Autor: Gilmer Ayala Falcón · UNAS-FIIS · Arquitectura de Software · Grupo A
"""Excepciones del generador de XML UBL 2.1."""
from __future__ import annotations


class ErrorFacturacionXml(Exception):
    """Base de los errores del generador."""


class DatosFacturaInvalidosError(ErrorFacturacionXml):
    """Los datos de entrada no cumplen las reglas del comprobante.

    ``errores`` contiene un mensaje por cada problema, con la ruta del campo
    (por ejemplo ``items[2].unidad_medida``), para corregirlos todos a la vez.
    """

    def __init__(self, errores: list[str]):
        self.errores = list(errores)
        super().__init__("Datos de la factura inválidos:\n- " + "\n- ".join(self.errores))


class XmlNoConformeXsdError(ErrorFacturacionXml):
    """El XML no es válido contra ``UBL-Invoice-2.1.xsd``."""

    def __init__(self, errores: list[str]):
        self.errores = list(errores)
        super().__init__("El XML no cumple el XSD UBL-Invoice-2.1:\n- " + "\n- ".join(self.errores))


class FirmaDigitalError(ErrorFacturacionXml):
    """No se pudo firmar el XML (certificado o contraseña inválidos, XML ya firmado) o la firma no se puede leer."""


class EsquemaXsdIncompletoError(ErrorFacturacionXml):
    """Falta un archivo del esquema UBL 2.1 y el XSD no se puede compilar."""
