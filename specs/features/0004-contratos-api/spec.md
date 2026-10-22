# spec.md — 0004 Contratos de API

**Feature**: Contratos de API RESTful y Esquemas OpenAPI 3.1  
**ID**: 0004  
**Estado**: APROBADO  
**Autor**: Equipo Grupo A — UNAS-FIIS  
**Fecha**: 2026-10-22  
**Fuentes**: RS 097-2012/SUNAT (F-03), RS 234-2006/SUNAT (F-04), OASIS UBL 2.1 (F-01), OpenAPI 3.1.0

---

## 1. Descripción

Esta especificación formaliza los contratos de API para el sistema de facturación electrónica de **ABC Software Corp**.

Los artefactos principales correspondientes son:
- **Especificación OpenAPI 3.1**: [`specs/03-api/openapi.yaml`](../../03-api/openapi.yaml) (espejo en [`specs/contracts/facturacion-api.yaml`](../../contracts/facturacion-api.yaml))
- **Mapeo de Rutas, Casos de Uso y DTOs**: [`specs/03-api/README.md`](../../03-api/README.md)

---

## 2. Cobertura de Endpoints

- Emisión de Comprobantes: `POST /comprobantes`, `GET /comprobantes/{id}`, `GET /comprobantes` (filtros)
- Descarga de Artefactos: `GET /comprobantes/{id}/xml`, `GET /comprobantes/{id}/pdf`, `GET /comprobantes/{id}/cdr`
- Notas Modificatorias: `POST /notas-credito`, `POST /notas-debito`
- Bajas y Resúmenes: `POST /comunicaciones-baja`, `POST /resumenes-diarios`
- Validación de Contribuyentes: `GET /contribuyentes/{ruc}/validar`
- Mantenimiento Maestro: CRUD `/clientes`, CRUD `/productos`, CRUD `/series`
- Catálogos Normativos: `GET /catalogos/{numero}`
- Migración de Datos: `POST /migraciones/legado`, `GET /migraciones/{id}`

---

## 3. Criterios de Aceptación

- [x] OpenAPI 3.1 validado sin advertencias sintácticas.
- [x] Uso estricto de `$ref` hacia `components/schemas` sin esquemas duplicados.
- [x] Todos los endpoints cuentan con `operationId` y vinculación explícita al caso de uso `CU-xx`.
- [x] Códigos HTTP estandarizados (`201`, `200`, `400`, `404`, `409`, `422`, `502`).
- [x] Esquema unificado `ErrorResponse` con soporte de `codigo_sunat`.
- [x] Ejemplo completo y válido de factura para el sector de abarrotes con IGV e ICBPER.
