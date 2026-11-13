# tasks.md — 0004 Contratos de API

**Feature**: Contratos de API  
**ID**: 0004  
**Autor**: Gilmer Ayala Falcón (Grupo A, UNAS-FIIS)  
**Fecha**: 2026-11-06

---

## Tareas de especificación

- [x] Leer fuentes normativas relacionadas
- [x] Redactar `specs/contracts/facturacion-api.yaml` (OpenAPI 3.1) y `specs/features/0004-contratos-api/design.md`
- [x] Quitar `correlativo` de las solicitudes de emisión (lo asigna el servidor)
- [x] Declarar `monto_igv` y `monto_icbper` como propiedades informativas
- [x] Unificar tipos decimales (texto con patrón) en ítems y totales
- [x] Corregir patrones de serie (`[F|B]` aceptaba `|`)
- [x] Corregir el ejemplo de factura (ICBPER S/ 0.50, `cantidad_bolsas`, totales)
- [x] Sincronizar `specs/contracts/facturacion-api.yaml` con `specs/contracts/facturacion-api.yaml`
- [x] Quitar totales e impuestos de las solicitudes; agregar `ItemCalculadoDto` a la respuesta
- [x] Agregar `UnidadMedidaEnum`, `TipoAfectacionIgvEnum`, `SistemaIscEnum` y validación condicional de ISC
- [x] Agregar `SerieUpdateRequest`, `securitySchemes` usado, `401`/`403`/`202` y los endpoints de libros electrónicos
- [x] Agregar ejemplos de nota de crédito, nota de débito, baja y resumen diario
- [ ] Documentar autenticación y roles por operación (JWT)
- [ ] Marcar todos los "Por verificar"
- [ ] Revisión por segundo miembro del equipo

## Tareas de verificación

- [x] El contrato valida con `openapi-spec-validator` (`pytest -v`: 15/15 pruebas, 2026-11-11)
- [x] El ejemplo de factura valida contra `EmitirComprobanteRequest`
- [x] Validar ejemplos de respuesta contra sus esquemas
- [x] Regla de sincronización (verificada con `cmp` el 2026-11-07): `specs/contracts/facturacion-api.yaml` es copia de `specs/contracts/facturacion-api.yaml`; cualquier cambio se hace en el segundo y se copia (`cp`) antes de cada commit

## Notas de progreso

- 2026-11-06: `facturacion-api.yaml` usaba `$ref` sobre `paths` y `components`, algo no permitido en OpenAPI; ahora es una copia íntegra.
- 2026-11-06: ejemplos de nota de crédito, nota de débito, baja, resumen y respuesta de emisión validados contra sus esquemas. Cabecera `Idempotency-Key` en `POST /comprobantes`. Falta correr `redocly lint` (requiere Node.js).
- 2026-11-07: se agregaron `GET /migraciones/{id}/cuarentena` y `POST /migraciones/{id}/reprocesos` (con `Idempotency-Key`) y los esquemas `ParcheCuarentena`, `ReprocesoCuarentenaRequest`, `CuarentenaRegistro`, `CuarentenaListaResponse`; `MigracionResponse` ganó `lote_origen_id`. Revisados: YAML válido, 47 referencias `$ref` resueltas, `operationId` sin repetidos. **Pendiente**: correr `redocly lint` y `openapi-spec-validator` en una máquina con Node.js/PyPI.
- 2026-11-07: el estado de la spec pasó de APROBADO a EN REVISIÓN: sigue sin revisión de un segundo miembro y con verificaciones abiertas. Vuelve a APROBADO al cerrarlas.
- 2026-11-11: reorganización a la estructura `specs/features/NNNN/{spec,design,tasks}.md`; el contenido de las carpetas `specs/0X-*` pasó a `design.md` y los ADR completos a `specs/adr/`.
