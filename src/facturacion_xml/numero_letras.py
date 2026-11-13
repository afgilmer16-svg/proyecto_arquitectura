# Autor: Gilmer Ayala Falcón · UNAS-FIIS · Arquitectura de Software · Grupo A
"""Importe en letras para la leyenda 1000 del Catálogo 52 (``cbc:Note``).

Ejemplo de la Guía XML SUNAT v1.0 (p. 79): ``CUATROCIENTOS VEINTITRES MIL
DOSCIENTOS VEINTICINCO Y 00/100``. Aquí se usa el formato de la matriz 0005
(§18.3): ``<ENTERO EN LETRAS> CON <CÉNTIMOS>/100 <MONEDA>``.
"""
from __future__ import annotations

from decimal import Decimal

_UNIDADES = ("CERO", "UNO", "DOS", "TRES", "CUATRO", "CINCO", "SEIS", "SIETE", "OCHO", "NUEVE",
             "DIEZ", "ONCE", "DOCE", "TRECE", "CATORCE", "QUINCE", "DIECISEIS", "DIECISIETE",
             "DIECIOCHO", "DIECINUEVE", "VEINTE", "VEINTIUNO", "VEINTIDOS", "VEINTITRES",
             "VEINTICUATRO", "VEINTICINCO", "VEINTISEIS", "VEINTISIETE", "VEINTIOCHO", "VEINTINUEVE")
_DECENAS = ("", "", "", "TREINTA", "CUARENTA", "CINCUENTA", "SESENTA", "SETENTA", "OCHENTA", "NOVENTA")
_CENTENAS = ("", "CIENTO", "DOSCIENTOS", "TRESCIENTOS", "CUATROCIENTOS", "QUINIENTOS",
             "SEISCIENTOS", "SETECIENTOS", "OCHOCIENTOS", "NOVECIENTOS")

MAXIMO = 999_999_999_999


def _menor_mil(n: int) -> str:
    if n == 100:
        return "CIEN"
    centena, resto = divmod(n, 100)
    partes = [_CENTENAS[centena]] if centena else []
    if resto < 30:
        if resto:
            partes.append(_UNIDADES[resto])
    else:
        decena, unidad = divmod(resto, 10)
        partes.append(_DECENAS[decena] + (f" Y {_UNIDADES[unidad]}" if unidad else ""))
    return " ".join(partes)


def entero_en_letras(n: int) -> str:
    """Convierte un entero de 0 a 999 999 999 999 a letras (mayúsculas, sin tildes)."""
    if not 0 <= n <= MAXIMO:
        raise ValueError(f"fuera de rango: {n}")
    if n == 0:
        return "CERO"
    millones, resto = divmod(n, 1_000_000)
    miles, unidades = divmod(resto, 1000)
    partes = []
    if millones:
        partes.append("UN MILLON" if millones == 1 else f"{entero_en_letras(millones)} MILLONES")
    if miles:
        partes.append("MIL" if miles == 1 else f"{_menor_mil(miles)} MIL")
    if unidades:
        partes.append(_menor_mil(unidades))
    # "UNO" se apocopa a "UN" delante de MIL / MILLONES (p. ej. VEINTIUN MIL).
    texto = " ".join(partes)
    return texto.replace("UNO MIL", "UN MIL").replace("UNO MILLONES", "UN MILLONES").replace("VEINTIUNO MIL", "VEINTIUN MIL")


def importe_en_letras(importe: Decimal, moneda: str) -> str:
    """``Decimal('2130.18'), 'SOLES'`` → ``'DOS MIL CIENTO TREINTA CON 18/100 SOLES'``."""
    importe = importe.quantize(Decimal("0.01"))
    entero = int(importe)
    centimos = int((importe - entero) * 100)
    return f"{entero_en_letras(entero)} CON {centimos:02d}/100 {moneda}"
