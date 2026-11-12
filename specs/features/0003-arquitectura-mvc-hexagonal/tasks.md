# tasks.md — 0003 Arquitectura MVC / Hexagonal

**Feature**: Arquitectura MVC y Hexagonal  
**ID**: 0003  
**Autor**: Equipo Grupo A — UNAS-FIIS  
**Fecha**: 2026-11-06

---

## Tareas de especificación

- [x] Leer ADR-001, ADR-002 y ADR-003
- [x] Redactar `spec.md` y `specs/features/0003-arquitectura-mvc-hexagonal/design.md`
- [x] Alinear la capa de presentación con ADR-003 (Compose Desktop y Android)
- [x] Corregir la dependencia de la migración: `MigrarLegadoService` no invoca `EmitirComprobanteUseCase`
- [x] Incluir `GestionarMaestrosUseCase` en la matriz de puertos
- [x] Reemplazar Freemarker por un adaptador que construye el XML desde el XSD y lo valida (2026-11-11: implementado como `LxmlUblAdapter` en Python, ADR-0005)
- [x] Agregar `LibroElectronicoPort`, `KardexElectronicoPort` y `GenerarLibrosElectronicosUseCase`
- [x] Agregar los nuevos puertos a los diagramas Mermaid
- [x] Agregar vista C4 de contexto y de contenedores
- [x] Agregar diagrama de secuencia de CU-01 con contingencia (OSE caído) y modo sin conexión
- [x] Definir la tecnología del backoffice web
- [ ] Marcar todos los "Por verificar"
- [ ] Revisión por segundo miembro del equipo

## Tareas de verificación

- [x] Verificar que cada caso de uso (CU-01 a CU-16; CU-15 y CU-16 en `GenerarLibrosElectronicosUseCase`) se asigna a un puerto de entrada
- [x] Verificar que ningún adaptador aparece en `domain/` ni `application/`
- [ ] Definir las pruebas de arquitectura (dependencias prohibidas hacia Compose, Android, AWT/Swing)
- [ ] Revisar fechas del cronograma Strangler Fig cuando cambie el plan del curso

## Notas de progreso

- 2026-11-06: el cronograma Gantt marcaba como terminadas fases con fechas futuras; se corrigió el estado.
- 2026-11-06: se agregaron vistas C4 (contexto y contenedores), dos diagramas de secuencia y los puertos de libros electrónicos en ambos diagramas de puertos. ADR-004 propone el backoffice web y está pendiente de aprobación.
- 2026-11-07: el estado de la spec pasó de APROBADO a EN REVISIÓN: sigue sin revisión de un segundo miembro y con verificaciones abiertas. Vuelve a APROBADO al cerrarlas.
- 2026-11-11: reorganización a la estructura `specs/features/NNNN/{spec,design,tasks}.md`; el contenido de las carpetas `specs/0X-*` pasó a `design.md` y los ADR completos a `specs/adr/`.
