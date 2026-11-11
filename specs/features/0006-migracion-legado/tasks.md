# tasks.md — 0006 Migración de Legado

**Feature**: Migración de Legado  
**ID**: 0006  
**Autor**: Equipo Grupo A — UNAS-FIIS  
**Fecha**: 2026-11-06

---

## Tareas de especificación

- [x] Redactar `specs/features/0006-migracion-legado/design.md` (mapeo de campos, ETL, auditoría, cuarentena)
- [x] Redactar `spec.md` con requisitos RF-MIG y criterios de aceptación
- [ ] T-0006-01: obtener DDL, diccionario o muestra anonimizada del sistema antiguo y perfilarlo (mientras tanto, supuestos S-01 a S-11 en `specs/features/0006-migracion-legado/diccionario-legado.md`)
- [x] Definir la tabla de códigos de error de cuarentena con su descripción
- [x] Definir los casos de prueba por código de error
- [ ] Definir la política para movimientos sin documento de origen
- [ ] Definir el periodo mínimo de historia a migrar
- [x] Definir el contrato de reproceso de cuarentena en la API (`GET /migraciones/{id}/cuarentena`, `POST /migraciones/{id}/reprocesos`)
- [ ] Revisión por segundo miembro del equipo

## Tareas de verificación

- [ ] Verificar que el mapeo coincide con las columnas reales del sistema antiguo
- [ ] Verificar la zona horaria y los formatos de fecha del origen
- [x] Verificar que `MigrarLegadoService` no depende de `EmitirComprobanteUseCase`
- [x] Verificar que `POST /migraciones/legado` y `GET /migraciones/{id}` cubren avance, conteos y errores

## Notas de progreso

- 2026-11-06: la arquitectura (`mvc-hexagonal.md`) mostraba que la migración reemitía comprobantes; se corrigió para alinearla con el mapeo.
- 2026-11-06: se agregaron el catálogo de códigos de error y 12 casos de prueba a `specs/features/0006-migracion-legado/design.md`. Sigue pendiente obtener el diccionario del sistema antiguo.
- 2026-11-07: se creó `specs/features/0006-migracion-legado/diccionario-legado.md` (borrador con 11 supuestos derivados del enunciado) y se definió el contrato de reproceso de cuarentena en el OpenAPI. Sigue abierto obtener una muestra real del sistema antiguo y la revisión por un segundo miembro.
- 2026-11-07: los 15 códigos de cuarentena tienen ahora al menos un caso de prueba (T-MIG-01 a T-MIG-24).
- 2026-11-11: reorganización a la estructura `specs/features/NNNN/{spec,design,tasks}.md`; el contenido de las carpetas `specs/0X-*` pasó a `design.md` y los ADR completos a `specs/adr/`.
