"""Modelo de entrada del generador y su validación.

La entrada sigue el DTO ``EmitirComprobanteRequest`` del contrato
``specs/contracts/facturacion-api.yaml`` (nombres en snake_case, importes como
texto decimal). El correlativo **no** viene del cliente: lo asigna el servidor
(la serie) y se pasa aparte a :func:`factura_desde_dict`.

Todas las reglas se verifican antes de construir el XML y los problemas se
devuelven juntos en :class:`DatosFacturaInvalidosError`.
"""
from __future__ import annotations

import re
from dataclasses import dataclass, field
from datetime import date, time
from decimal import Decimal, InvalidOperation
from typing import Any

from . import catalogos as cat
from .errores import DatosFacturaInvalidosError

_PATRON_SERIE = re.compile(r"^[FB][A-Z0-9]{3}$")
_PATRON_DECIMAL = re.compile(r"^[0-9]+(\.[0-9]{1,10})?$")
_PATRON_TASA = re.compile(r"^[0-9]{1,3}\.[0-9]{2}$")
_PATRON_HORA = re.compile(r"^(0[0-9]|1[0-9]|2[0-3]):[0-5][0-9]:[0-5][0-9]$")
_PESOS_RUC = (5, 4, 3, 2, 7, 6, 5, 4, 3, 2)
_PREFIJOS_RUC = ("10", "15", "16", "17", "20")
CORRELATIVO_MAXIMO = 99_999_999
# Umbral para identificar al comprador en boletas (Reglamento de Comprobantes
# de Pago); el monto vigente está "Por verificar" (docs/pendientes-verificacion.md).
UMBRAL_IDENTIFICACION_BOLETA = Decimal("700.00")


@dataclass(frozen=True)
class Emisor:
    ruc: str
    razon_social: str
    direccion: str
    ubigeo: str
    nombre_comercial: str | None = None
    codigo_establecimiento: str = cat.CODIGO_DOMICILIO_FISCAL


@dataclass(frozen=True)
class Receptor:
    tipo_documento: cat.TipoDocumentoIdentidad
    numero_documento: str
    razon_social: str
    direccion: str | None = None


@dataclass(frozen=True)
class Cuota:
    monto: Decimal
    fecha_vencimiento: date


@dataclass(frozen=True)
class FormaPago:
    tipo: str = "Contado"  # "Contado" | "Credito"
    monto_neto_pendiente: Decimal | None = None
    cuotas: tuple[Cuota, ...] = ()


@dataclass(frozen=True)
class LineaFactura:
    numero_linea: int
    codigo_producto: str
    descripcion: str
    unidad_medida: str
    cantidad: Decimal
    valor_unitario: Decimal
    tipo_afectacion_igv: str
    sistema_isc: cat.SistemaIsc | None = None
    tasa_isc: Decimal | None = None
    monto_fijo_isc: Decimal | None = None
    unidad_base_isc: str | None = None
    precio_venta_publico_sugerido: Decimal | None = None
    cantidad_bolsas: int | None = None

    @property
    def categoria(self) -> cat.CategoriaOperacion:
        return cat.CATEGORIA_POR_AFECTACION[self.tipo_afectacion_igv]


@dataclass(frozen=True)
class Factura:
    tipo_comprobante: cat.TipoComprobante
    serie: str
    correlativo: int
    fecha_emision: date
    moneda: str
    emisor: Emisor
    receptor: Receptor
    lineas: tuple[LineaFactura, ...]
    hora_emision: time | None = None
    tipo_operacion: str = cat.TIPO_OPERACION_VENTA_INTERNA
    forma_pago: FormaPago = field(default_factory=FormaPago)
    regimen_igv: cat.RegimenIgv = cat.RegimenIgv.GENERAL

    @property
    def numero(self) -> str:
        """Serie y número del comprobante (``cbc:ID``), p. ej. ``F001-00000042``."""
        return f"{self.serie}-{self.correlativo:08d}"


# --------------------------------------------------------------------------
# Validaciones de dominio reutilizables
# --------------------------------------------------------------------------
def ruc_valido(ruc: str) -> bool:
    """RUC de 11 dígitos, prefijo válido y dígito verificador (módulo 11)."""
    if not (isinstance(ruc, str) and ruc.isdigit() and len(ruc) == 11 and ruc[:2] in _PREFIJOS_RUC):
        return False
    resto = sum(int(d) * p for d, p in zip(ruc[:10], _PESOS_RUC)) % 11
    verificador = {10: 0, 11: 1}.get(11 - resto, 11 - resto)
    return verificador == int(ruc[10])


class _Lector:
    """Lee el diccionario de entrada acumulando errores con la ruta del campo."""

    def __init__(self) -> None:
        self.errores: list[str] = []

    def error(self, ruta: str, mensaje: str) -> None:
        self.errores.append(f"{ruta}: {mensaje}")

    def texto(self, datos: dict, clave: str, ruta: str, requerido: bool = True, maximo: int | None = None) -> str | None:
        valor = datos.get(clave)
        if valor is None or (isinstance(valor, str) and not valor.strip()):
            if requerido:
                self.error(f"{ruta}{clave}", "es obligatorio")
            return None
        if not isinstance(valor, str):
            self.error(f"{ruta}{clave}", "debe ser texto")
            return None
        valor = valor.strip()
        if maximo and len(valor) > maximo:
            self.error(f"{ruta}{clave}", f"supera {maximo} caracteres")
        return valor

    def decimal(self, datos: dict, clave: str, ruta: str, requerido: bool = True, positivo: bool = False,
                patron: re.Pattern = _PATRON_DECIMAL) -> Decimal | None:
        valor = datos.get(clave)
        if valor is None:
            if requerido:
                self.error(f"{ruta}{clave}", "es obligatorio")
            return None
        if not isinstance(valor, str) or not patron.match(valor):
            self.error(f"{ruta}{clave}", f"debe ser un decimal exacto en texto (ej. '140.00'); se recibió {valor!r}")
            return None
        try:
            numero = Decimal(valor)
        except InvalidOperation:  # pragma: no cover - el patrón ya lo impide
            self.error(f"{ruta}{clave}", "decimal inválido")
            return None
        if positivo and numero <= 0:
            self.error(f"{ruta}{clave}", "debe ser mayor que cero")
            return None
        return numero

    def fecha(self, datos: dict, clave: str, ruta: str, requerido: bool = True) -> date | None:
        valor = datos.get(clave)
        if valor is None:
            if requerido:
                self.error(f"{ruta}{clave}", "es obligatorio")
            return None
        try:
            return date.fromisoformat(valor)
        except (TypeError, ValueError):
            self.error(f"{ruta}{clave}", f"debe tener formato AAAA-MM-DD; se recibió {valor!r}")
            return None


# --------------------------------------------------------------------------
# Construcción del modelo
# --------------------------------------------------------------------------
def factura_desde_dict(datos: dict[str, Any], correlativo: int,
                       regimen_igv: cat.RegimenIgv | str = cat.RegimenIgv.GENERAL) -> Factura:
    """Convierte el DTO de entrada en una :class:`Factura` validada.

    :param datos: diccionario con la forma de ``EmitirComprobanteRequest``.
    :param correlativo: número asignado por el servidor a la serie (1–99 999 999).
    :param regimen_igv: tasa de IGV aplicable (por defecto, régimen general 18 %).
    :raises DatosFacturaInvalidosError: con todos los problemas encontrados.
    """
    if not isinstance(datos, dict):
        raise DatosFacturaInvalidosError(["la entrada debe ser un objeto JSON (diccionario)"])
    lec = _Lector()

    # Tipo de comprobante y serie -----------------------------------------
    tipo = None
    tipo_txt = datos.get("tipo_comprobante")
    if tipo_txt in cat.TIPOS_COMPROBANTE_OTRO_XSD:
        lec.error("tipo_comprobante", f"'{tipo_txt}' se genera con {cat.TIPOS_COMPROBANTE_OTRO_XSD[tipo_txt]}, "
                                      "fuera del alcance de este generador (UBL-Invoice-2.1.xsd)")
    else:
        try:
            tipo = cat.TipoComprobante(tipo_txt)
        except ValueError:
            lec.error("tipo_comprobante", f"debe ser '01' (factura) o '03' (boleta); se recibió {tipo_txt!r}")

    serie = lec.texto(datos, "serie", "")
    if serie and not _PATRON_SERIE.match(serie):
        lec.error("serie", f"debe cumplir ^[FB][A-Z0-9]{{3}}$; se recibió {serie!r}")
    elif serie and tipo and serie[0] != tipo.prefijo_serie:
        lec.error("serie", f"un comprobante '{tipo.value}' debe usar una serie que empiece con '{tipo.prefijo_serie}'")

    if not isinstance(correlativo, int) or isinstance(correlativo, bool) or not 1 <= correlativo <= CORRELATIVO_MAXIMO:
        lec.error("correlativo", f"lo asigna el servidor y debe ser un entero de 1 a {CORRELATIVO_MAXIMO}")

    if "correlativo" in datos:
        lec.error("correlativo", "el cliente no envía el correlativo; lo asigna el servidor")

    # Fechas, moneda y tipo de operación ------------------------------------
    fecha_emision = lec.fecha(datos, "fecha_emision", "")
    hora = None
    hora_txt = datos.get("hora_emision")
    if hora_txt is not None:
        if isinstance(hora_txt, str) and _PATRON_HORA.match(hora_txt):
            hora = time.fromisoformat(hora_txt)
        else:
            lec.error("hora_emision", f"debe tener formato HH:MM:SS; se recibió {hora_txt!r}")

    moneda = datos.get("moneda", "PEN")
    if moneda not in cat.MONEDAS:
        lec.error("moneda", f"debe ser una de {sorted(cat.MONEDAS)} (Catálogo 02); se recibió {moneda!r}")

    tipo_operacion = datos.get("tipo_operacion", cat.TIPO_OPERACION_VENTA_INTERNA)
    if tipo_operacion != cat.TIPO_OPERACION_VENTA_INTERNA:
        lec.error("tipo_operacion", "este generador emite venta interna ('0101', Catálogo 51)")

    try:
        regimen = cat.RegimenIgv(regimen_igv)
    except ValueError:
        lec.error("regimen_igv", f"debe ser uno de {[r.value for r in cat.RegimenIgv]}")
        regimen = cat.RegimenIgv.GENERAL

    # Emisor -----------------------------------------------------------------
    emisor = None
    d_emisor = datos.get("emisor")
    if not isinstance(d_emisor, dict):
        lec.error("emisor", "es obligatorio")
    else:
        ruc = lec.texto(d_emisor, "ruc", "emisor.")
        if ruc and not ruc_valido(ruc):
            lec.error("emisor.ruc", f"RUC inválido {ruc!r}: 11 dígitos, prefijo 10/15/16/17/20 y dígito verificador módulo 11")
        razon = lec.texto(d_emisor, "razon_social", "emisor.", maximo=100)
        direccion = lec.texto(d_emisor, "direccion", "emisor.", maximo=200)
        ubigeo = lec.texto(d_emisor, "ubigeo", "emisor.")
        if ubigeo and not (ubigeo.isdigit() and len(ubigeo) == 6):
            lec.error("emisor.ubigeo", "debe tener 6 dígitos (Catálogo 13)")
        nombre_comercial = lec.texto(d_emisor, "nombre_comercial", "emisor.", requerido=False, maximo=100)
        if ruc and razon and direccion and ubigeo:
            emisor = Emisor(ruc, razon, direccion, ubigeo, nombre_comercial)

    # Receptor ---------------------------------------------------------------
    receptor = None
    d_receptor = datos.get("receptor")
    if not isinstance(d_receptor, dict):
        lec.error("receptor", "es obligatorio")
    else:
        tipo_doc = None
        try:
            tipo_doc = cat.TipoDocumentoIdentidad(d_receptor.get("tipo_documento"))
        except ValueError:
            lec.error("receptor.tipo_documento", "debe ser un código del Catálogo 06: '0', '1', '4', '6' o '7'")
        numero = lec.texto(d_receptor, "numero_documento", "receptor.")
        razon_r = lec.texto(d_receptor, "razon_social", "receptor.", maximo=100)
        direccion_r = lec.texto(d_receptor, "direccion", "receptor.", requerido=False, maximo=200)
        if tipo is cat.TipoComprobante.FACTURA and tipo_doc and tipo_doc is not cat.TipoDocumentoIdentidad.RUC:
            lec.error("receptor.tipo_documento", "la factura exige un adquirente con RUC (tipo '6')")
        if numero and tipo_doc is cat.TipoDocumentoIdentidad.RUC and not ruc_valido(numero):
            lec.error("receptor.numero_documento", f"RUC inválido {numero!r} (módulo 11)")
        if numero and tipo_doc is cat.TipoDocumentoIdentidad.DNI and not (numero.isdigit() and len(numero) == 8):
            lec.error("receptor.numero_documento", "el DNI debe tener 8 dígitos")
        if tipo_doc and numero and razon_r:
            receptor = Receptor(tipo_doc, numero, razon_r, direccion_r)

    # Líneas -----------------------------------------------------------------
    lineas: list[LineaFactura] = []
    d_items = datos.get("items")
    if not isinstance(d_items, list) or not d_items:
        lec.error("items", "debe contener al menos una línea")
        d_items = []
    for i, item in enumerate(d_items):
        ruta = f"items[{i}]."
        if not isinstance(item, dict):
            lec.error(f"items[{i}]", "debe ser un objeto")
            continue
        linea = _leer_linea(lec, item, ruta, i + 1)
        if linea:
            lineas.append(linea)

    # Forma de pago ----------------------------------------------------------
    forma_pago = _leer_forma_pago(lec, datos.get("forma_pago") or {"tipo": "Contado"}, fecha_emision)

    if lec.errores:
        raise DatosFacturaInvalidosError(lec.errores)

    factura = Factura(tipo, serie, correlativo, fecha_emision, moneda, emisor, receptor, tuple(lineas),
                      hora, tipo_operacion, forma_pago, regimen)
    _validar_reglas_de_conjunto(factura)
    return factura


def _leer_linea(lec: _Lector, item: dict, ruta: str, esperado: int) -> LineaFactura | None:
    errores_previos = len(lec.errores)
    numero = item.get("numero_linea")
    if numero != esperado:
        lec.error(f"{ruta}numero_linea", f"debe ser {esperado} (numeración única y secuencial)")
    codigo = lec.texto(item, "codigo_producto", ruta, maximo=30)
    descripcion = lec.texto(item, "descripcion", ruta, maximo=250)
    unidad = item.get("unidad_medida")
    if unidad not in cat.UNIDADES_MEDIDA:
        lec.error(f"{ruta}unidad_medida", f"{unidad!r} no es un código del Catálogo 03 admitido {sorted(cat.UNIDADES_MEDIDA)}")
    cantidad = lec.decimal(item, "cantidad", ruta, positivo=True)
    valor_unitario = lec.decimal(item, "valor_unitario", ruta)

    afectacion = item.get("tipo_afectacion_igv")
    if afectacion in cat.AFECTACIONES_NO_SOPORTADAS:
        lec.error(f"{ruta}tipo_afectacion_igv",
                  f"'{afectacion}' ({cat.AFECTACIONES_NO_SOPORTADAS[afectacion]}) no se genera en este parcial")
    elif afectacion not in cat.CATEGORIA_POR_AFECTACION:
        lec.error(f"{ruta}tipo_afectacion_igv", f"{afectacion!r} no es un código del Catálogo 07")

    # ISC (Catálogo 08) con validación condicional por sistema
    sistema = tasa = monto_fijo = unidad_base = pvp = None
    if item.get("sistema_isc") is not None:
        try:
            sistema = cat.SistemaIsc(item["sistema_isc"])
        except ValueError:
            lec.error(f"{ruta}sistema_isc", "debe ser '01', '02' o '03' (Catálogo 08)")
        if afectacion != "10":
            lec.error(f"{ruta}sistema_isc", "el ISC solo se admite en líneas gravadas (afectación '10')")
        tasa = lec.decimal(item, "tasa_isc", ruta, requerido=sistema in (cat.SistemaIsc.AL_VALOR, cat.SistemaIsc.PRECIO_VENTA_PUBLICO),
                           patron=_PATRON_TASA)
        monto_fijo = lec.decimal(item, "monto_fijo_isc", ruta, requerido=sistema is cat.SistemaIsc.ESPECIFICO)
        unidad_base = item.get("unidad_base_isc")
        if sistema is cat.SistemaIsc.ESPECIFICO and unidad_base not in cat.UNIDADES_MEDIDA:
            lec.error(f"{ruta}unidad_base_isc", "es obligatorio en el sistema '02' y debe ser del Catálogo 03")
        pvp = lec.decimal(item, "precio_venta_publico_sugerido", ruta,
                          requerido=sistema is cat.SistemaIsc.PRECIO_VENTA_PUBLICO, positivo=True)
    else:
        for clave in ("tasa_isc", "monto_fijo_isc", "unidad_base_isc", "precio_venta_publico_sugerido"):
            if item.get(clave) is not None:
                lec.error(f"{ruta}{clave}", "requiere 'sistema_isc'")

    bolsas = item.get("cantidad_bolsas")
    if bolsas is not None and (not isinstance(bolsas, int) or isinstance(bolsas, bool) or bolsas < 1):
        lec.error(f"{ruta}cantidad_bolsas", "debe ser un entero mayor o igual a 1")

    if len(lec.errores) > errores_previos:
        return None
    return LineaFactura(esperado, codigo, descripcion, unidad, cantidad, valor_unitario, afectacion,
                        sistema, tasa, monto_fijo, unidad_base, pvp, bolsas)


def _leer_forma_pago(lec: _Lector, d: Any, fecha_emision: date | None) -> FormaPago:
    if not isinstance(d, dict):
        lec.error("forma_pago", "debe ser un objeto")
        return FormaPago()
    tipo = d.get("tipo")
    if tipo == "Contado":
        return FormaPago("Contado")
    if tipo != "Credito":
        lec.error("forma_pago.tipo", "debe ser 'Contado' o 'Credito'")
        return FormaPago()
    pendiente = lec.decimal(d, "monto_neto_pendiente", "forma_pago.", positivo=True)
    cuotas = []
    d_cuotas = d.get("cuotas")
    if not isinstance(d_cuotas, list) or not d_cuotas:
        lec.error("forma_pago.cuotas", "el pago al crédito exige al menos una cuota")
        d_cuotas = []
    for i, c in enumerate(d_cuotas):
        ruta = f"forma_pago.cuotas[{i}]."
        monto = lec.decimal(c, "monto", ruta, positivo=True)
        venc = lec.fecha(c, "fecha_vencimiento", ruta)
        if venc and fecha_emision and venc < fecha_emision:
            lec.error(f"{ruta}fecha_vencimiento", "no puede ser anterior a la fecha de emisión")
        if monto and venc:
            cuotas.append(Cuota(monto, venc))
    if pendiente and cuotas and sum(c.monto for c in cuotas) != pendiente:
        lec.error("forma_pago.cuotas", "la suma de las cuotas debe ser igual al monto neto pendiente")
    return FormaPago("Credito", pendiente, tuple(cuotas))


def _validar_reglas_de_conjunto(factura: Factura) -> None:
    """Reglas que dependen de los importes calculados."""
    from .calculo import calcular_totales  # import local para evitar ciclo

    totales = calcular_totales(factura)
    errores = []
    if (factura.tipo_comprobante is cat.TipoComprobante.BOLETA
            and factura.receptor.tipo_documento is cat.TipoDocumentoIdentidad.SIN_DOCUMENTO
            and totales.importe_total > UMBRAL_IDENTIFICACION_BOLETA):
        errores.append(f"receptor: una boleta mayor a S/ {UMBRAL_IDENTIFICACION_BOLETA} debe identificar al comprador")
    if (factura.forma_pago.tipo == "Credito" and factura.forma_pago.monto_neto_pendiente
            and factura.forma_pago.monto_neto_pendiente > totales.importe_total):
        errores.append("forma_pago.monto_neto_pendiente: no puede superar el importe total del comprobante")
    if errores:
        raise DatosFacturaInvalidosError(errores)
