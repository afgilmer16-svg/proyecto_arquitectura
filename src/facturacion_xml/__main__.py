"""Línea de comandos: genera el XML de un comprobante a partir de un JSON.

    python -m facturacion_xml ENTRADA.json SALIDA.xml --correlativo 1 [--regimen-igv GENERAL]
        [--firmar CERTIFICADO.pfx --clave-env VARIABLE]

El JSON se lee **siempre en UTF-8**. En Windows la página de códigos por defecto
suele ser WIN1252: si el archivo se guardó así, las tildes y la "ñ" se
corromperían en silencio. Por eso se lee como UTF-8 estricto y, si falla, se
explica el problema en lugar de generar un XML con caracteres dañados.
"""
from __future__ import annotations

import argparse
import json
import os
import sys
from pathlib import Path

from . import RegimenIgv, ValidadorXsd, firmar_xml, generar_xml_factura
from .errores import ErrorFacturacionXml


def leer_json_utf8(ruta: Path) -> dict:
    datos = ruta.read_bytes()
    if datos.startswith(b"\xef\xbb\xbf"):  # BOM de UTF-8 (Bloc de notas de Windows)
        datos = datos[3:]
    try:
        texto = datos.decode("utf-8")
    except UnicodeDecodeError as exc:
        raise ErrorFacturacionXml(
            f"{ruta} no está en UTF-8 (byte {datos[exc.start]:#04x} en la posición {exc.start}). "
            "Probablemente se guardó en WIN1252/ANSI: vuelve a guardarlo como UTF-8."
        ) from exc
    return json.loads(texto)


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(prog="facturacion_xml", description=__doc__.splitlines()[0])
    parser.add_argument("entrada", type=Path, help="JSON con la forma de EmitirComprobanteRequest")
    parser.add_argument("salida", type=Path, help="ruta del XML a generar")
    parser.add_argument("--correlativo", type=int, required=True, help="número asignado por la serie (1-99999999)")
    parser.add_argument("--regimen-igv", default=RegimenIgv.GENERAL.value, choices=[r.value for r in RegimenIgv])
    parser.add_argument("--firmar", type=Path, metavar="CERTIFICADO.pfx",
                        help="firma el XML (XMLDSig) con este certificado .pfx/.p12; es opcional")
    parser.add_argument("--clave-env", default="FIRMA_CLAVE", metavar="VARIABLE",
                        help="variable de entorno con la contraseña del .pfx (por defecto FIRMA_CLAVE); "
                             "no se acepta la contraseña por argumento para no dejarla en el historial")
    args = parser.parse_args(argv)
    try:
        xml = generar_xml_factura(leer_json_utf8(args.entrada), args.correlativo, args.regimen_igv)
        if args.firmar:
            xml = firmar_xml(xml, args.firmar.read_bytes(), os.environ.get(args.clave_env))
            ValidadorXsd().validar(xml)  # el XML firmado también debe seguir siendo válido
    except (ErrorFacturacionXml, json.JSONDecodeError, OSError) as exc:
        print(f"ERROR: {exc}", file=sys.stderr)
        return 1
    args.salida.parent.mkdir(parents=True, exist_ok=True)
    args.salida.write_bytes(xml)
    estado = "firmado y " if args.firmar else ""
    print(f"XML generado, {estado}validado contra UBL-Invoice-2.1.xsd: {args.salida}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
