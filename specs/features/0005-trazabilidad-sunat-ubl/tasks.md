# tasks.md — 0005 Trazabilidad SUNAT / UBL

**Feature**: Trazabilidad SUNAT / UBL  
**ID**: 0005  
**Autor**: Equipo Grupo A — UNAS-FIIS  
**Fecha**: 2026-11-06

---

## Tareas de especificación

- [x] Redactar `spec.md` con matrices de cabecera, emisor, receptor, forma de pago, líneas, IGV, ISC, ICBPER, totales, PLE 14.1 y 12.1
- [x] Redactar `specs/features/0005-trazabilidad-sunat-ubl/design.md` (matriz dominio ↔ DTO ↔ XPath)
- [x] Extender el DTO con ISC por línea y `cantidad_bolsas`
- [x] Extender el dominio con versión UBL, tipo de operación (Cat. 51), firma y totales UBL (hoy solo en la matriz)
- [x] Ampliar la cobertura de notas de crédito/débito (líneas y totales), baja y resumen diario
- [ ] Resolver "SIRE/RVIE vs PLE" con la normativa vigente
- [ ] Completar equivalencias Cat. 03 ↔ Tabla 6 PLE para `GLL` y `SA`
- [ ] Revisión por segundo miembro del equipo

## Tareas de verificación

- [x] Incorporar a `docs/referencias/ubl/` los XSD oficiales (Invoice, CreditNote, DebitNote y dependencias)
- [x] Incorporar a `docs/referencias/sunat/` la guía XML de factura (v1.0) y el Manual del Programador
- [ ] Incorporar a `docs/referencias/sunat/` el Anexo 8 vigente, la guía del ICBPER y las guías de baja y resumen
- [ ] Confirmar el elemento UBL exacto para el PVP sugerido (ISC sistema 03)
- [x] Confirmar contra el XSD local los elementos `BaseUnitMeasure` y `PerUnitAmount` del ICBPER y del ISC sistema 02
- [ ] Confirmar el importe ICBPER por año (Ley N° 30884)
- [ ] Confirmar el rango de prefijos de RUC (`10`, `15`, `16`, `17`, `20`)

## Notas de progreso

- 2026-11-06: `specs/features/0005-trazabilidad-sunat-ubl/design.md` se actualizó con el estado real del DTO (patrón de serie corregido, ISC por línea incorporado, ICBPER a S/ 0.50).
- Esta feature depende de fuentes locales ausentes; no debe pasar a APROBADO sin la verificación contra los XSD.
- 2026-11-06: XSD de UBL 2.1 incorporados y rutas verificadas (sección 17 de la spec). Siguen abiertos: catálogos del Anexo 8 y guía XML de SUNAT, XSD propios de SUNAT para baja/resumen, y la posición exacta de `PerUnitAmount`.
- 2026-11-07: el estado de la spec pasó de APROBADO a EN REVISIÓN: sigue sin revisión de un segundo miembro y con verificaciones abiertas. Vuelve a APROBADO al cerrarlas.
- 2026-11-11: se verificó la spec contra la guía XML de SUNAT (sección 18 de la spec). Corrige un error normativo: las líneas exoneradas, inafectas y gratuitas no se declaran como IGV (`1000`) con monto cero, sino con `TaxScheme` `9997`, `9998` y `9996`. También se corrigieron los atributos de `ProfileID` y de `InvoicedQuantity/@unitCode`, y se agregaron las leyendas (Cat. 52). Quedan abiertos: elemento del PVP (ISC sistema 03), posición del ICBPER y descuentos por línea (fuera de alcance).
- 2026-11-11: reorganización a la estructura `specs/features/NNNN/{spec,design,tasks}.md`; el contenido de las carpetas `specs/0X-*` pasó a `design.md` y los ADR completos a `specs/adr/`.
