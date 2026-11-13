# tasks.md — 0001 Casos de Uso

**Feature**: Casos de Uso del Sistema  
**ID**: 0001  
**Autor**: Gilmer Ayala Falcón (Grupo A, UNAS-FIIS)  
**Fecha**: 2026-11-06

---

## Tareas de especificación

- [x] Leer fuentes normativas relacionadas (`docs/enunciado.md`, `docs/caso.md`)
- [x] Redactar `spec.md` con la matriz de CU-01 a CU-16
- [x] Detallar CU-01 (factura), CU-07 (baja) y CU-09 (RUC)
- [x] Detallar CU-15 (PLE 14.1) y CU-16 (PLE 12.1)
- [x] Detallar flujo principal, alternos y excepciones de CU-02, CU-03, CU-04, CU-05, CU-06, CU-08 y CU-10 a CU-14
- [x] Agregar flujo alterno de contingencia (OSE caído) y de modo sin conexión a CU-01 y CU-02
- [ ] Marcar todos los "Por verificar" pendientes
- [ ] Revisión por segundo miembro del equipo
- [ ] Actualizar matriz de trazabilidad
- [x] Redactar `design.md` (2026-11-11: reorganización a la estructura del curso)

## Tareas de verificación

- [ ] Confirmar plazos de baja (7 días calendario) y de resumen diario contra F-03 y F-05
- [ ] Confirmar umbral de boleta sin identificar al comprador (S/ 700) contra F-06 (Reglamento de Comprobantes de Pago, RS 007-99/SUNAT)
- [ ] Confirmar rangos de códigos de respuesta del CDR (aceptado, rechazo, observaciones) contra F-05
- [x] Confirmar que cada CU tiene al menos una operación en `specs/contracts/facturacion-api.yaml`

## Notas de progreso

- 2026-11-06: se corrigió el ICBPER (importe vigente por fecha, S/ 0.50 desde 2023), el rango de rechazos (`2000`–`3999`) y el motivo de baja (texto libre, no Catálogo 09).
- 2026-11-06: se detallaron CU-02 a CU-16 y los flujos alternos de contingencia y modo sin conexión (secciones 3 y 4 de la spec). Falta la revisión por un segundo miembro y la verificación normativa de plazos.
- 2026-11-07: el estado de la spec pasó de APROBADO a EN REVISIÓN: sigue sin revisión de un segundo miembro y con verificaciones abiertas. Vuelve a APROBADO al cerrarlas.
- 2026-11-11: reorganización a la estructura `specs/features/NNNN/{spec,design,tasks}.md`; el contenido de las carpetas `specs/0X-*` pasó a `design.md` y los ADR completos a `specs/adr/`.
