# spec.md — 0006 Migración de Legado

**Feature**: Migración de Legado  
**ID**: 0006  
**Estado**: EN REVISIÓN  
**Autor**: Gilmer Ayala Falcón (Grupo A, UNAS-FIIS)  
**Fecha**: 2026-11-06  
**Fuentes**: RS 097-2012/SUNAT (F-03), RS 234-2006/SUNAT (F-04), Catálogos SUNAT Anexo 8 (F-02)

---

## 1. Descripción

ABC Software Corp opera hoy con un sistema antiguo de abarrotes sin integración con SUNAT (ver `docs/caso.md`). Esta feature especifica cómo se importa su información histórica al nuevo sistema sin afectar el cumplimiento fiscal.

El diseño detallado (mapeo de campos, flujo ETL, auditoría, cuarentena) está en [`specs/features/0006-migracion-legado/design.md`](design.md). Este documento fija los requisitos y criterios de aceptación.

Caso de uso asociado: **CU-14 Migrar Datos del Sistema Legado**. Contrato: `POST /migraciones/legado`, `GET /migraciones/{id}`, `GET /migraciones/{id}/cuarentena` y `POST /migraciones/{id}/reprocesos` en [`specs/contracts/facturacion-api.yaml`](../../contracts/facturacion-api.yaml). Puerto de entrada: `MigrarLegadoUseCase`. Puerto de salida: `LegadoReaderPort`.

## 2. Alcance

**Dentro del alcance**

- Importación de productos, movimientos de almacén (`ingreso`, `salida`) y referencias de comprobantes (`nro_documento`) desde CSV, DBF o base relacional.
- Normalización, validación, cuarentena, carga idempotente, conciliación y auditoría `legado_id → nuevo_id`.
- Consulta del avance y de la bitácora de errores de cada lote.

**Fuera de alcance**

- Reemisión, refirma o reenvío a OSE/SUNAT de comprobantes antiguos.
- Migración de credenciales, certificados digitales o usuarios del sistema antiguo.
- Corrección fiscal de documentos históricos (se tramita por los casos de uso CU-03, CU-04 o CU-07).
- Migración de módulos contables o de planillas.

## 3. Requisitos

| ID | Requisito |
|---|---|
| RF-MIG-01 | La migración **no invoca** `EmitirComprobanteUseCase` ni genera XML, firma o envío. Todo registro migrado se guarda con `origen = LEGADO`, `es_historico = true` y `envio_sunat_habilitado = false`. |
| RF-MIG-02 | La extracción se hace sobre una copia de solo lectura con SHA-256 y conteo registrados; el archivo original nunca se modifica. |
| RF-MIG-03 | Cada fila recibe un `legado_id` estable y reproducible. La carga es idempotente por `(sistema_origen, tipo_entidad, legado_id)`. |
| RF-MIG-04 | Los registros inválidos van a cuarentena con código de error estable; no se insertan parcialmente en tablas productivas. |
| RF-MIG-05 | Un lote solo se cierra si la conciliación (conteos, sumas y saldos por producto) es cero o tiene una excepción aprobada y firmada. |
| RF-MIG-06 | La relación `legado_id → nuevo_id` queda en una tabla de solo anexado (`migracion_trazabilidad`). |
| RF-MIG-07 | Las cantidades usan decimal exacto; nunca punto flotante. Fechas se interpretan en `America/Lima`. |
| RF-MIG-08 | Una fecha o cantidad ambigua nunca se completa con un valor supuesto: va a cuarentena hasta recibir evidencia. |

## 4. Criterios de aceptación

- [x] Existe la tabla de mapeo campo a campo para `fecha`, `ingreso`, `salida`, `producto` y `nro_documento`, con transformación y regla de validación (`specs/features/0006-migracion-legado/design.md`).
- [x] El flujo ETL (extracción, normalización, validación, carga, conciliación) está diagramado y descrito.
- [x] Hay un caso de prueba especificado por cada código de error de cuarentena (`FECHA_AMBIGUA`, `MOVIMIENTO_DOBLE`, `PRODUCTO_NO_ENCONTRADO`, etc.).
- [ ] Repetir el mismo lote no duplica productos, movimientos ni comprobantes.
- [ ] Ningún registro migrado puede llegar a `EnvioOsePort` (verificable por prueba de arquitectura y restricción en base de datos).
- [ ] El reporte de conciliación del lote es inmutable y contiene los hashes de entrada y salida.

## 5. Riesgos y notas

> ⚠️ Por verificar: no existe diccionario físico del sistema antiguo. Los supuestos de trabajo (S-01 a S-11) están en [`specs/features/0006-migracion-legado/diccionario-legado.md`](diccionario-legado.md). Nombres de tablas, formatos de fecha, zona horaria, unidad de medida y significado de `nro_documento` se confirman en el perfilado inicial (ver tarea T-0006-01).

> ⚠️ Por verificar: política contable para movimientos sin documento de origen (`SIN_DOCUMENTO_ORIGEN`) y periodo mínimo de historia a migrar.

> ⚠️ Por verificar: plazo legal de conservación del historial migrado [F-03, F-04].
