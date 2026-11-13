# Autor: Gilmer Ayala Falcón · UNAS-FIIS · Arquitectura de Software · Grupo A
"""Cálculo de importes del comprobante con decimales exactos.

Reglas (spec 0002, §5, y Guía XML SUNAT v1.0, §27):

1. ``valor_venta = cantidad × valor_unitario`` (en gratuitas es el valor referencial).
2. El ISC se calcula **antes** que el IGV porque integra su base (Catálogo 08):
   * ``01`` al valor: ``ISC = valor_venta × tasa``.
   * ``02`` específico: ``ISC = cantidad × monto_fijo``.
   * ``03`` al valor según PVP: ``base = cantidad × PVP / (1 + tasa IGV)``; ``ISC = base × tasa``.
3. ``IGV = (valor_venta + ISC) × tasa IGV`` solo en operaciones gravadas.
4. ``ICBPER = bolsas × importe vigente en la fecha de emisión``.
5. Exoneradas, inafectas y gratuitas no generan IGV. Las gratuitas no suman
   al importe a pagar; se informan con su valor referencial (tributo 9996).

Cada importe se redondea a 2 decimales (ROUND_HALF_UP) por línea, y los totales
suman los importes ya redondeados, como hace SUNAT al validar el cuadre.
"""
from __future__ import annotations

from dataclasses import dataclass
from decimal import ROUND_HALF_UP, Decimal

from . import catalogos as cat
from .modelo import Factura, LineaFactura

CENTIMO = Decimal("0.01")
DIEZ_DECIMALES = Decimal("0.0000000001")  # precio unitario: n(12,10)
CERO = Decimal("0.00")


def redondear(valor: Decimal) -> Decimal:
    return valor.quantize(CENTIMO, rounding=ROUND_HALF_UP)


@dataclass(frozen=True)
class ImportesLinea:
    linea: LineaFactura
    valor_venta: Decimal          # cbc:LineExtensionAmount
    base_isc: Decimal
    isc: Decimal
    base_igv: Decimal
    igv: Decimal
    icbper: Decimal
    importe_icbper_unitario: Decimal
    tasa_igv: Decimal

    @property
    def es_gratuita(self) -> bool:
        return self.linea.categoria is cat.CategoriaOperacion.GRATUITA

    @property
    def total_tributos(self) -> Decimal:
        """``cac:InvoiceLine/cac:TaxTotal/cbc:TaxAmount``."""
        return self.igv + self.isc + self.icbper

    @property
    def precio_unitario(self) -> Decimal:
        """Precio unitario con tributos (Catálogo 16 = '01'): (valor + ISC + IGV) / cantidad.

        No se redondea a 2 decimales: la Guía XML admite hasta 10 (n(12,10)) y,
        si se redondeara, precio × cantidad dejaría de reconstruir el importe de
        la línea (p. ej. bolsa: 1.18 / 10 = 0.118, no 0.12).
        """
        if self.es_gratuita:
            return self.linea.valor_unitario
        exacto = (self.valor_venta + self.isc + self.igv) / self.linea.cantidad
        return exacto.quantize(DIEZ_DECIMALES, rounding=ROUND_HALF_UP)

    @property
    def tasa_igv_informada(self) -> Decimal:
        """``cbc:Percent`` del IGV: la tasa en gravadas y 0.00 en el resto (matriz 0005, §8)."""
        return self.tasa_igv if self.linea.categoria is cat.CategoriaOperacion.GRAVADA else CERO


@dataclass(frozen=True)
class Totales:
    lineas: tuple[ImportesLinea, ...]
    total_gravadas: Decimal       # valor de venta gravado, sin ISC (Guía §23)
    total_exoneradas: Decimal
    total_inafectas: Decimal
    total_gratuitas: Decimal
    total_valor_venta: Decimal    # LegalMonetaryTotal/LineExtensionAmount
    base_isc: Decimal
    total_isc: Decimal
    total_igv: Decimal
    total_icbper: Decimal
    bolsas: int

    @property
    def total_tributos(self) -> Decimal:
        return self.total_igv + self.total_isc + self.total_icbper

    @property
    def importe_total(self) -> Decimal:
        """``TaxInclusiveAmount`` y ``PayableAmount`` (sin descuentos ni cargos globales)."""
        return self.total_valor_venta + self.total_tributos

    @property
    def solo_gratuitas(self) -> bool:
        return all(li.es_gratuita for li in self.lineas)


def calcular_linea(linea: LineaFactura, tasa_igv: Decimal, importe_bolsa: Decimal) -> ImportesLinea:
    valor_venta = redondear(linea.cantidad * linea.valor_unitario)
    tasa = tasa_igv / 100

    base_isc = isc = CERO
    if linea.sistema_isc is cat.SistemaIsc.AL_VALOR:
        base_isc = valor_venta
        isc = redondear(base_isc * linea.tasa_isc / 100)
    elif linea.sistema_isc is cat.SistemaIsc.ESPECIFICO:
        base_isc = valor_venta
        isc = redondear(linea.cantidad * linea.monto_fijo_isc)
    elif linea.sistema_isc is cat.SistemaIsc.PRECIO_VENTA_PUBLICO:
        base_isc = redondear(linea.cantidad * linea.precio_venta_publico_sugerido / (1 + tasa))
        isc = redondear(base_isc * linea.tasa_isc / 100)

    if linea.categoria is cat.CategoriaOperacion.GRAVADA:
        base_igv = valor_venta + isc
        igv = redondear(base_igv * tasa)
    else:
        base_igv = valor_venta
        igv = CERO

    icbper = redondear(linea.cantidad_bolsas * importe_bolsa) if linea.cantidad_bolsas else CERO
    return ImportesLinea(linea, valor_venta, base_isc, isc, base_igv, igv, icbper, importe_bolsa, tasa_igv)


def calcular_totales(factura: Factura) -> Totales:
    tasa_igv = cat.TASA_IGV[factura.regimen_igv]
    importe_bolsa = cat.importe_icbper(factura.fecha_emision)
    lineas = tuple(calcular_linea(li, tasa_igv, importe_bolsa) for li in factura.lineas)

    def suma(categoria: cat.CategoriaOperacion, campo: str) -> Decimal:
        return sum((getattr(li, campo) for li in lineas if li.linea.categoria is categoria), CERO)

    total_gravadas = suma(cat.CategoriaOperacion.GRAVADA, "valor_venta")
    total_exoneradas = suma(cat.CategoriaOperacion.EXONERADA, "valor_venta")
    total_inafectas = suma(cat.CategoriaOperacion.INAFECTA, "valor_venta")
    total_gratuitas = suma(cat.CategoriaOperacion.GRATUITA, "valor_venta")
    total_valor_venta = sum((li.valor_venta for li in lineas if not li.es_gratuita), CERO)
    return Totales(
        lineas=lineas,
        total_gravadas=total_gravadas,
        total_exoneradas=total_exoneradas,
        total_inafectas=total_inafectas,
        total_gratuitas=total_gratuitas,
        total_valor_venta=total_valor_venta,
        base_isc=sum((li.base_isc for li in lineas if li.linea.sistema_isc), CERO),
        total_isc=sum((li.isc for li in lineas), CERO),
        total_igv=sum((li.igv for li in lineas), CERO),
        total_icbper=sum((li.icbper for li in lineas), CERO),
        bolsas=sum((li.linea.cantidad_bolsas or 0 for li in lineas)),
    )
