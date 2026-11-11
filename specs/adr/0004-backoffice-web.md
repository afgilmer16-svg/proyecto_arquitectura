# ADR-004 — Tecnología del Backoffice Web

**Estado:** PROPUESTO (pendiente de aprobación del equipo)  
**Fecha:** 2026-11-06  
**Autores:** Equipo Grupo A — UNAS-FIIS  
**Relacionado:** ADR-001, ADR-003, `specs/contracts/facturacion-api.yaml`

## Contexto

La arquitectura prevé un backoffice web para administración y contabilidad (maestros, series, bajas, resúmenes, consulta de comprobantes y libros electrónicos PLE). ADR-003 fijó Windows (MSI) y Android (APK) pero dejó abierta la tecnología web. El backoffice es una interfaz de formularios y tablas sin operación sin conexión.

Restricciones de ADR-001 y ADR-003: la UI es solo un adaptador de entrada; no calcula tributos, no genera UBL, no firma, no llama a OSE/SUNAT.

## Opciones

| Opción | Ventajas | Desventajas |
|---|---|---|
| A. SPA web estándar (TypeScript) sobre la API REST, con cliente generado desde `openapi.yaml` | Tecnología madura para formularios y tablas; el contrato es la única fuente de verdad; no obliga al equipo a depender de la madurez de un target web experimental | Añade un segundo lenguaje y toolchain al proyecto |
| B. Compose Multiplatform para Web (Wasm) | Un solo lenguaje y modelo de UI con desktop y Android | Madurez, tamaño de descarga y accesibilidad del target web a verificar; menos ejemplos y librerías |
| C. No tener backoffice web: administradores usan la app desktop | Cero tecnología nueva | Obliga a instalar el MSI para tareas administrativas; no sirve para acceso remoto del contador |

## Decisión propuesta

Adoptar la **Opción A** como adaptador de entrada independiente: una SPA que consume exclusivamente la API REST. El cliente HTTP y los tipos se generan desde `specs/contracts/facturacion-api.yaml` para evitar divergencias.

> ⚠️ Por verificar: elección concreta del framework de la SPA y su versión; confirmar la madurez actual del target web de Compose Multiplatform antes de descartar la Opción B.

## Consecuencias

- **Positivas**: el contrato OpenAPI gobierna la integración; el backoffice se despliega y versiona de forma independiente; el dominio no cambia.
- **Negativas**: dos stacks de UI (Compose y web); hay que mantener la generación del cliente en CI.
- **Riesgos**: filtración de reglas fiscales a la SPA. Mitigación: la SPA solo envía datos comerciales; los totales y tributos siempre los devuelve el servidor (ver `ItemCalculadoDto`).

## Estado de revisión

- [x] Alternativas comparadas
- [ ] Revisado por un segundo miembro del equipo
- [ ] Aprobado
