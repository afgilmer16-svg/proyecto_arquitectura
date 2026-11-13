# spec.md — 0004 Contratos de API

**Feature**: Contratos de API RESTful y Esquemas OpenAPI 3.1  
**ID**: 0004  
**Estado**: EN REVISIÓN  
**Autor**: Gilmer Ayala Falcón (Grupo A, UNAS-FIIS)  
**Fecha**: 2026-10-22  
**Fuentes**: RS 097-2012/SUNAT (F-03), RS 234-2006/SUNAT (F-04), OASIS UBL 2.1 (F-01), OpenAPI 3.1.0

---

## 1. Descripción

Esta especificación formaliza los contratos de API para el sistema de facturación electrónica de **ABC Software Corp**.

Los artefactos principales correspondientes son:
- **Especificación OpenAPI 3.1**: [`specs/contracts/facturacion-api.yaml`](../../contracts/facturacion-api.yaml) (fuente única del contrato; los DTO están en `components/schemas`)
- **Mapeo de Rutas, Casos de Uso y DTOs**: [`specs/features/0004-contratos-api/design.md`](design.md)

---

## 2. Cobertura de Endpoints

- Emisión de Comprobantes: `POST /comprobantes`, `GET /comprobantes/{id}`, `GET /comprobantes` (filtros)
- Descarga de Artefactos: `GET /comprobantes/{id}/xml`, `GET /comprobantes/{id}/pdf`, `GET /comprobantes/{id}/cdr`
- Notas Modificatorias: `POST /notas-credito`, `POST /notas-debito`
- Bajas y Resúmenes: `POST /comunicaciones-baja`, `POST /resumenes-diarios`
- Validación de Contribuyentes: `GET /contribuyentes/{ruc}/validar`
- Mantenimiento Maestro: CRUD `/clientes`, CRUD `/productos`, CRUD `/series`
- Catálogos Normativos: `GET /catalogos/{numero}`
- Migración de Datos: `POST /migraciones/legado`, `GET /migraciones/{id}`, `GET /migraciones/{id}/cuarentena`, `POST /migraciones/{id}/reprocesos`
- Libros electrónicos: `GET /libros/registro-ventas` (PLE 14.1), `GET /libros/inventario-permanente` (PLE 12.1)

---

## 3. Criterios de Aceptación

- [x] Validación estructural ejecutada el 2026-11-11 con un verificador propio: YAML válido, 33 operaciones, 0 `$ref` rotos, `operationId` únicos, sin `nullable` y los 6 ejemplos cumplen su schema (JSON Schema 2020-12).
- [x] `openapi-spec-validator` sin errores: `pytest -v` pasó 15/15 el 2026-11-11 (Python 3.12.7, pytest 9.1.1).
- [ ] `redocly lint`: pendiente de correr (requiere Node.js, no instalado en el equipo). No marcar hasta ver 0 errores.
- [x] Uso estricto de `$ref` hacia `components/schemas` sin esquemas duplicados.
- [x] Todos los endpoints cuentan con `operationId` y vinculación explícita al caso de uso `CU-xx`.
- [x] Códigos HTTP estandarizados (`200`, `201`, `202` para baja y resumen asíncronos, `400`, `401`, `403`, `404`, `409`, `422`, `502`).
- [x] Esquema unificado `ErrorResponse` con soporte de `codigo_sunat`.
- [x] Ejemplo completo y válido de factura para el sector de abarrotes con IGV e ICBPER.
- [x] `securitySchemes` (`bearerAuth`, JWT) declarado y respuestas `401`/`403` en todas las operaciones.
- [x] El cliente no envía `correlativo`, impuestos ni totales; los importes son texto decimal.
- [x] `unidad_medida` es un enum del Catálogo 03 y el ISC (Catálogo 08) tiene validación condicional por sistema.
- [x] `PUT /series/{id}` no permite modificar el correlativo.
- [x] El identificador de la baja y del resumen (`RA-`/`RC-AAAAMMDD-n`) lo asigna el servidor; el cliente no lo envía.
- [x] El producto guarda los parámetros del ISC (`sistema_isc`, `tasa_isc`, `monto_fijo_isc`, `unidad_base_isc`, `precio_venta_publico_sugerido`) con validación condicional por sistema, igual que el ítem.
- [x] Sin `nullable` (no válido en OpenAPI 3.1).
