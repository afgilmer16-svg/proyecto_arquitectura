# Autor: Gilmer Ayala Falcón · UNAS-FIIS · Arquitectura de Software · Grupo A
"""Catálogos SUNAT y constantes UBL 2.1 usados por el generador.

Cada valor indica su fuente. Las fuentes locales están en ``docs/referencias``:

* ``docs/referencias/ubl/UBL-Invoice-2.1.xsd`` (OASIS UBL 2.1 OS, 04-nov-2013).
* ``docs/referencias/sunat/guia-xml-factura-ubl21-v1.0.pdf`` (SUNAT, mayo 2017).

Lo que no figura en esas fuentes se marca como "Por verificar" en
``docs/pendientes-verificacion.md``.
"""
from __future__ import annotations

from dataclasses import dataclass
from datetime import date
from decimal import Decimal
from enum import Enum

# --------------------------------------------------------------------------
# Espacios de nombres UBL 2.1 (XSD OASIS)
# --------------------------------------------------------------------------
NS_INVOICE = "urn:oasis:names:specification:ubl:schema:xsd:Invoice-2"
NS_CAC = "urn:oasis:names:specification:ubl:schema:xsd:CommonAggregateComponents-2"
NS_CBC = "urn:oasis:names:specification:ubl:schema:xsd:CommonBasicComponents-2"
# Prefijos que la Guia XML de SUNAT declara en la raiz del Invoice (p. 79). Aunque este generador no
# usa ext:/ds:, **deben estar declarados**: los firmadores (por ejemplo, el de probar-xml.nubefact.com,
# que responde con el error 0306 "The prefix ext ... is not bound") insertan ext:UBLExtensions con ds:Signature
# dentro de un XML que ya declare esos prefijos. Declarar un prefijo sin usarlo es valido en XML y en el XSD.
NS_EXT = "urn:oasis:names:specification:ubl:schema:xsd:CommonExtensionComponents-2"
NS_DS = "http://www.w3.org/2000/09/xmldsig#"
NS_CCTS = "urn:un:unece:uncefact:documentation:2"
NS_QDT = "urn:oasis:names:specification:ubl:schema:xsd:QualifiedDatatypes-2"
NS_UDT = "urn:un:unece:uncefact:data:specification:UnqualifiedDataTypesSchemaModule:2"
NS_XSI = "http://www.w3.org/2001/XMLSchema-instance"
NSMAP = {None: NS_INVOICE, "cac": NS_CAC, "cbc": NS_CBC, "ccts": NS_CCTS, "ds": NS_DS, "ext": NS_EXT,
         "qdt": NS_QDT, "udt": NS_UDT, "xsi": NS_XSI}

# Constantes de la cabecera (Guía XML SUNAT v1.0, pp. 4 y 79).
UBL_VERSION_ID = "2.1"
CUSTOMIZATION_ID = "2.0"

# Atributos de listas y esquemas (Guía XML SUNAT v1.0, ejemplo pp. 79-84).
AGENCIA_SUNAT = "PE:SUNAT"
AGENCIA_UNECE = "United Nations Economic Commission for Europe"
URI_CATALOGO = "urn:pe:gob:sunat:cpe:see:gem:catalogos:catalogo{:02d}"


# --------------------------------------------------------------------------
# Catálogo 01 — Tipo de documento (solo los que usan el XSD Invoice)
# --------------------------------------------------------------------------
class TipoComprobante(str, Enum):
    FACTURA = "01"
    BOLETA = "03"

    @property
    def prefijo_serie(self) -> str:
        return "F" if self is TipoComprobante.FACTURA else "B"


# Las notas de crédito (07) y débito (08) usan CreditNote/DebitNote-2.1.xsd:
# están fuera del alcance de este generador (solo Invoice-2.1.xsd).
TIPOS_COMPROBANTE_OTRO_XSD = {"07": "UBL-CreditNote-2.1.xsd", "08": "UBL-DebitNote-2.1.xsd"}

# Catálogo 02 — Monedas admitidas por el contrato API (ISO 4217).
MONEDAS = {"PEN": "SOLES", "USD": "DÓLARES AMERICANOS"}

# Catálogo 03 — Unidades de medida (UN/ECE rec 20) admitidas por el contrato
# (enum UnidadMedidaEnum de specs/contracts/facturacion-api.yaml).
UNIDADES_MEDIDA = {
    "NIU", "ZZ", "KGM", "GRM", "TNE", "LTR", "MLT", "GLL",
    "MTR", "BX", "BG", "SA", "PK", "DZN", "CS",
}


# Catálogo 06 — Tipo de documento de identidad.
class TipoDocumentoIdentidad(str, Enum):
    SIN_DOCUMENTO = "0"
    DNI = "1"
    CARNE_EXTRANJERIA = "4"
    RUC = "6"
    PASAPORTE = "7"


# --------------------------------------------------------------------------
# Categorías de operación vistas en clase (1001-1004)
# --------------------------------------------------------------------------
class CategoriaOperacion(str, Enum):
    """Categorías tributarias de la operación, con el código visto en clase.

    En UBL 2.0 SUNAT estos códigos (Catálogo 14) iban en
    ``sac:AdditionalMonetaryTotal``. Ese elemento **no existe** en
    ``UBL-Invoice-2.1.xsd``; en UBL 2.1 cada categoría se informa como un
    ``cac:TaxSubtotal`` con su propio tributo del Catálogo 05 (ver
    :data:`TRIBUTO_POR_CATEGORIA`).
    """

    GRAVADA = "1001"
    INAFECTA = "1002"
    EXONERADA = "1003"
    GRATUITA = "1004"


# Catálogo 07 — Tipo de afectación del IGV admitidos por este generador.
# 10: gravado - operación onerosa.
# 20: exonerado - operación onerosa; 21: exonerado - transferencia gratuita.
# 30: inafecto - operación onerosa; 31-37: inafecto, retiros y transferencias gratuitas.
CATEGORIA_POR_AFECTACION: dict[str, CategoriaOperacion] = {
    "10": CategoriaOperacion.GRAVADA,
    "20": CategoriaOperacion.EXONERADA,
    "21": CategoriaOperacion.GRATUITA,
    "30": CategoriaOperacion.INAFECTA,
    **{str(c): CategoriaOperacion.GRATUITA for c in range(31, 38)},
}

# Afectaciones del Catálogo 07 que existen en el contrato pero no se generan
# en este parcial, con el motivo (se rechazan con un error explícito).
AFECTACIONES_NO_SOPORTADAS = {
    **{str(c): "gravado - retiro / transferencia gratuita (el IGV lo asume el emisor)" for c in range(11, 17)},
    "17": "IVAP (Impuesto a la Venta del Arroz Pilado)",
    "40": "exportación (requiere otro tipo de operación, Catálogo 51)",
}


@dataclass(frozen=True)
class Tributo:
    """Fila del Catálogo 05 (Guía XML SUNAT v1.0, §24-27 y ejemplo p. 81)."""

    codigo: str          # TaxScheme/cbc:ID
    nombre: str          # TaxScheme/cbc:Name
    tipo_codigo: str     # TaxScheme/cbc:TaxTypeCode
    categoria_unece: str | None  # TaxCategory/cbc:ID (UN/ECE 5305)


IGV = Tributo("1000", "IGV", "VAT", "S")
ISC = Tributo("2000", "ISC", "EXC", "S")
EXONERADO = Tributo("9997", "EXO", "VAT", "E")
INAFECTO = Tributo("9998", "INA", "FRE", "O")
GRATUITO = Tributo("9996", "GRA", "FRE", "Z")
# ICBPER: Catálogo 05 vigente (la guía v1.0 de 2017 es anterior a la Ley 30884).
ICBPER = Tributo("7152", "ICBPER", "OTH", None)

TRIBUTO_POR_CATEGORIA = {
    CategoriaOperacion.GRAVADA: IGV,
    CategoriaOperacion.EXONERADA: EXONERADO,
    CategoriaOperacion.INAFECTA: INAFECTO,
    CategoriaOperacion.GRATUITA: GRATUITO,
}


# --------------------------------------------------------------------------
# Tasas del IGV (reglas vistas en clase; parametrizadas, no universales)
# --------------------------------------------------------------------------
class RegimenIgv(str, Enum):
    """Tasa de IGV (incluye IPM) según el tipo de operación.

    * GENERAL: 18 % (16 % IGV + 2 % IPM).
    * RESTAURANTES_HOTELES: 10 %, tasa especial explicada en clase para
      restaurantes y hoteles. Su vigencia y requisitos se deben confirmar en
      la norma (ver docs/pendientes-verificacion.md); no se aplica por defecto.
    """

    GENERAL = "GENERAL"
    RESTAURANTES_HOTELES = "RESTAURANTES_HOTELES"


TASA_IGV = {
    RegimenIgv.GENERAL: Decimal("18.00"),
    RegimenIgv.RESTAURANTES_HOTELES: Decimal("10.00"),
}


# Catálogo 08 — Sistema de cálculo del ISC (Apéndice IV del TUO de la Ley del IGV e ISC).
class SistemaIsc(str, Enum):
    AL_VALOR = "01"
    ESPECIFICO = "02"
    PRECIO_VENTA_PUBLICO = "03"


# Catálogo 16 — Tipo de precio de venta unitario.
PRECIO_UNITARIO_CON_TRIBUTOS = "01"
VALOR_REFERENCIAL_GRATUITO = "02"

# Catálogo 52 — Leyendas.
LEYENDA_MONTO_EN_LETRAS = "1000"
LEYENDA_TRANSFERENCIA_GRATUITA = "1002"
TEXTO_TRANSFERENCIA_GRATUITA = "TRANSFERENCIA GRATUITA DE UN BIEN Y/O SERVICIO PRESTADO GRATUITAMENTE"

# Catálogo 51 — Tipo de operación por defecto: venta interna.
TIPO_OPERACION_VENTA_INTERNA = "0101"

# ICBPER — importe por bolsa según el año de emisión (Ley 30884).
# 2019: 0.10 · 2020: 0.20 · 2021: 0.30 · 2022: 0.40 · desde 2023: 0.50.
_ICBPER_POR_ANIO = {2019: Decimal("0.10"), 2020: Decimal("0.20"), 2021: Decimal("0.30"), 2022: Decimal("0.40")}
ICBPER_DESDE_2023 = Decimal("0.50")


def importe_icbper(fecha_emision: date) -> Decimal:
    """Importe del ICBPER por bolsa vigente en la fecha de emisión."""
    if fecha_emision.year < 2019:
        raise ValueError("El ICBPER rige desde el 01/08/2019")
    return _ICBPER_POR_ANIO.get(fecha_emision.year, ICBPER_DESDE_2023)


# Código de establecimiento anexo del emisor (Guía XML v1.0, p. 7):
# "0000" identifica el domicilio fiscal.
CODIGO_DOMICILIO_FISCAL = "0000"
