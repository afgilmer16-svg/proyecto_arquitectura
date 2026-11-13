# Autor: Gilmer Ayala Falcón · UNAS-FIIS · Arquitectura de Software · Grupo A
"""Pruebas de mutación: comprueban que la suite detecta errores introducidos a propósito.

Cada "mutante" copia el proyecto a una carpeta temporal, cambia una línea del
generador (un error realista) y ejecuta ``pytest``. Si al menos una prueba falla,
el mutante queda "DETECTADO"; si todas pasan, la suite tiene un hueco.

No es una prueba de pytest (por eso no empieza con ``test_``). Ejecutar desde la raíz:

    python tests/mutacion.py
"""
from __future__ import annotations

import os
import shutil
import subprocess
import sys
import tempfile
from pathlib import Path

RAIZ = Path(__file__).resolve().parents[1]

M=[
 ("M01 IGV sin incluir el ISC en su base","calculo.py","base_igv = valor_venta + isc","base_igv = valor_venta"),
 ("M02 Tasa de IGV 19 %","catalogos.py",'RegimenIgv.GENERAL: Decimal("18.00")','RegimenIgv.GENERAL: Decimal("19.00")'),
 ("M03 Receptor antes que el emisor (orden XSD)","generador_ubl.py","        self._emisor(inv, f)\n        self._receptor(inv, f)","        self._receptor(inv, f)\n        self._emisor(inv, f)"),
 ("M04 Sin validar contra el XSD antes de entregar","generador_ubl.py","        if validar:\n            self.validador.validar(xml)","        pass"),
 ("M05 Exonerada informa Percent 18","calculo.py","return self.tasa_igv if self.linea.categoria is cat.CategoriaOperacion.GRAVADA else CERO","return self.tasa_igv"),
 ("M06 RUC sin dígito verificador","modelo.py","    return verificador == int(ruc[10])","    return True"),
 ("M07 ICBPER fijo 0.50 sin importar el año","catalogos.py","    return _ICBPER_POR_ANIO.get(fecha_emision.year, ICBPER_DESDE_2023)","    return ICBPER_DESDE_2023"),
 ("M08 Redondeo bancario (HALF_EVEN)","calculo.py","    return valor.quantize(CENTIMO, rounding=ROUND_HALF_UP)","    return valor.quantize(CENTIMO)"),
 ("M09 Gratuitas suman al valor de venta","calculo.py","total_valor_venta = sum((li.valor_venta for li in lineas if not li.es_gratuita), CERO)","total_valor_venta = sum((li.valor_venta for li in lineas), CERO)"),
 ("M10 Serie no coherente con el tipo","modelo.py","    elif serie and tipo and serie[0] != tipo.prefijo_serie:","    elif False:"),
 ("M11 Correlativo sin ceros a la izquierda","modelo.py",'return f"{self.serie}-{self.correlativo:08d}"','return f"{self.serie}-{self.correlativo}"'),
 ("M12 Lectura tolerante (corrompe WIN1252)","__main__.py",'        texto = datos.decode("utf-8")','        texto = datos.decode("utf-8", errors="replace")'),
 ("M13 Precio unitario redondeado a 2 decimales","calculo.py","return exacto.quantize(DIEZ_DECIMALES, rounding=ROUND_HALF_UP)","return exacto.quantize(CENTIMO, rounding=ROUND_HALF_UP)"),
 ("M14 ISC sistema 03 sin descontar el IGV del PVP","calculo.py","base_isc = redondear(linea.cantidad * linea.precio_venta_publico_sugerido / (1 + tasa))","base_isc = redondear(linea.cantidad * linea.precio_venta_publico_sugerido)"),
 ("M15 Gratuita con tributo 9998 en lugar de 9996","catalogos.py","    CategoriaOperacion.GRATUITA: GRATUITO,","    CategoriaOperacion.GRATUITA: INAFECTO,"),
 ("M16 Acepta afectación 11 sin rechazarla","modelo.py","    if afectacion in cat.AFECTACIONES_NO_SOPORTADAS:","    if False:"),
]


def main() -> int:
    vivos = 0
    for nombre, archivo, original, mutado in M:
        with tempfile.TemporaryDirectory() as tmp:
            copia = Path(tmp) / "proyecto"
            shutil.copytree(RAIZ, copia, ignore=shutil.ignore_patterns("__pycache__", ".git", ".pytest_cache", "salida", "target"))
            ruta = copia / "src" / "facturacion_xml" / archivo
            texto = ruta.read_text(encoding="utf-8")
            if texto.count(original) != 1:
                print(f"OMITIDO       {nombre} (el código cambió; actualiza el mutante)")
                continue
            ruta.write_text(texto.replace(original, mutado), encoding="utf-8")
            resultado = subprocess.run([sys.executable, "-m", "pytest", "-q", "-p", "no:cacheprovider", "--tb=no"],
                                       cwd=copia, capture_output=True, text=True, encoding="utf-8", errors="replace")
            resumen = (resultado.stdout.strip().splitlines() or ["(sin salida)"])[-1]
            if "passed" not in resumen and "failed" not in resumen:
                # pytest no llegó a ejecutar las pruebas: no se puede afirmar nada del mutante.
                print(f"{'ERROR':13} {nombre:50} pytest no se ejecutó: {resultado.stderr.strip()[-200:]}")
                vivos += 1
                continue
            detectado = "failed" in resumen
            vivos += not detectado
            print(f"{'DETECTADO' if detectado else 'NO DETECTADO':13} {nombre:50} {resumen}")
    print(f"\nMutantes: {len(M)} · detectados: {len(M) - vivos} · sobrevivientes: {vivos}")
    return 1 if vivos else 0


if __name__ == "__main__":
    sys.exit(main())
