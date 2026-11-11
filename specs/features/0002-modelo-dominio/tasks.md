# tasks.md — 0002 Modelo de Dominio

**Feature**: Modelo de Dominio  
**ID**: 0002  
**Autor**: Equipo Grupo A — UNAS-FIIS  
**Fecha**: 2026-11-06

---

## Tareas de especificación

- [x] Leer fuentes normativas relacionadas
- [x] Redactar `spec.md` con el agregado `ComprobantePago`, notas y value objects
- [x] Completar entidades faltantes (Emisor, Totales, FormaPago, CDR, Serie, Producto, MovimientoAlmacen)
- [x] Definir estados del comprobante y sus transiciones
- [x] Definir el modelo de notas de crédito y débito con sus ítems y totales
- [x] Definir `Detraccion` y `Cuota` (hoy solo se nombran en el diagrama)
- [x] Definir `CalculadorImpuestos` con ejemplos numéricos (IGV, ISC en tres sistemas, ICBPER)
- [ ] Marcar todos los "Por verificar"
- [ ] Revisión por segundo miembro del equipo
- [ ] Actualizar matriz de trazabilidad
- [x] Redactar `design.md` (2026-11-11: reorganización a la estructura del curso)

## Tareas de verificación

- [ ] Confirmar prefijos de RUC válidos (`10`, `15`, `16`, `17`, `20`) contra F-05
- [ ] Confirmar códigos de Catálogo 05, 07 y 08 contra F-02
- [ ] Confirmar importe de ICBPER por año contra la Ley N° 30884
- [x] Verificar que cada atributo del DTO tiene correspondencia en el dominio (matriz de `specs/features/0005-trazabilidad-sunat-ubl/spec.md`)

## Notas de progreso

- 2026-11-06: se agregó la sección 3 (entidades complementarias, estados e invariantes adicionales).
- 2026-11-06: se agregaron las secciones 4 (detracción, cuotas, notas modificatorias) y 5 (ejemplos numéricos de IGV, ISC en los tres sistemas e ICBPER). Las tasas y montos de ISC son ilustrativos y están marcados "Por verificar".
- 2026-11-07: el estado de la spec pasó de APROBADO a EN REVISIÓN: sigue sin revisión de un segundo miembro y con verificaciones abiertas. Vuelve a APROBADO al cerrarlas.
- 2026-11-11: reorganización a la estructura `specs/features/NNNN/{spec,design,tasks}.md`; el contenido de las carpetas `specs/0X-*` pasó a `design.md` y los ADR completos a `specs/adr/`.
