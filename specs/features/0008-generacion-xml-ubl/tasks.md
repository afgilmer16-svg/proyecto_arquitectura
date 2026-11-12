# tasks.md — 0008 Generación del XML UBL 2.1

**Feature**: Generación del XML UBL 2.1  
**ID**: 0008  
**Autor**: Equipo Grupo A — UNAS-FIIS  
**Fecha**: 2026-11-11

---

## Tareas de especificación

- [x] Leer el XSD `UBL-Invoice-2.1.xsd` e identificar los elementos obligatorios y las secuencias
- [x] Completar el esquema con los 3 XSD de firma que faltaban (sin ellos no compila)
- [x] Redactar `spec.md` y `design.md`
- [x] Registrar la decisión de lenguaje en ADR-005
- [x] Alinear la matriz 0005 (cuotas, `schemeName` del RUC, tasa de IGV parametrizada)

## Tareas de implementación

- [x] Catálogos y categorías 1001–1004 (`catalogos.py`)
- [x] Modelo y validación de la entrada (`modelo.py`)
- [x] Cálculo de IGV, ISC (Catálogo 08) e ICBPER (`calculo.py`)
- [x] Generador del árbol XML (`generador_ubl.py`)
- [x] Validador XSD con resolver local (`validador_xsd.py`)
- [x] Línea de comandos con lectura UTF-8 (`__main__.py`)

## Tareas de verificación

- [x] 89 pruebas de generación (`tests/test_generador_xml.py`), ejecutadas: 89 aprobadas
- [x] 310 pruebas de verificación integral (`tests/test_verificacion_integral.py`), ejecutadas: 310 aprobadas
- [x] Pruebas de mutación (`tests/mutacion.py`): 16/16 detectados
- [x] XML de ejemplo generado y validado (`docs/ejemplos/F001-00000001.xml`)
- [x] Ejecutar `pytest -v` en la PC del equipo y guardar el resultado: 460 aprobadas (`docs/resultados-pruebas-python.txt`)
- [x] Confirmar los 3 XSD de firma contra `docs.oasis-open.org`: SHA-256 idénticos
- [ ] Contrastar los puntos "Por verificar" con el anexo de validaciones vigente de SUNAT

- [x] CI de GitHub Actions, cobertura mínima del 90 %, JSON Schema de la entrada, ejemplos inválidos, reglas de arquitectura, `.mmd` y CSV de la matriz (mejoras M-01 a M-07 del informe)

## Notas de progreso

- 2026-11-11: implementación completa del alcance del segundo parcial. Se detectó y corrigió que el esquema XSD local no compilaba (faltaban `UBL-xmldsig-core-schema-2.1.xsd`, `UBL-XAdESv132-2.1.xsd` y `UBL-XAdESv141-2.1.xsd`).
- 2026-11-11 (2.ª ronda): la verificación integral encontró que el precio unitario se redondeaba a 2 decimales (E-12), 4 elementos sin trazabilidad en la matriz (E-13) y una prueba que no comprobaba la validación previa a la entrega (E-14). Los tres quedaron corregidos.
