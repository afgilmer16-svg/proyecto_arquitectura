# Documentación de Contratos de API RESTful — Facturación Electrónica SUNAT

**Documento**: Especificación y Mapeo de Contratos de API  
**Carpeta**: `specs/03-api/`  
**Estado**: APROBADO  
**Autores**: Equipo Grupo A — UNAS-FIIS  
**Fecha**: 2026-10-22  
**Fuentes Normativas**: RS N° 097-2012/SUNAT (F-03), RS N° 234-2006/SUNAT (F-04), OASIS UBL 2.1 (F-01), Catálogos SUNAT Anexo 8 (F-02)

---

## 1. Visión General del Contrato de API

La interfaz de programación de aplicaciones (API) define los contratos formales para la interacción con el sistema de facturación electrónica de **ABC Software Corp**, formalizada bajo la especificación **OpenAPI 3.1.0** en [`specs/03-api/openapi.yaml`](openapi.yaml).

### Principios de Diseño:
1. **Reutilización Estricta mediante `$ref`**: Todos los modelos de datos y DTOs se definen de manera centralizada en `components/schemas` y se referencian mediante punteros JSON Schema (`$ref: '#/components/schemas/...'`), eliminando cualquier duplicidad en las definiciones de los endpoints.
2. **Trazabilidad Directa con Casos de Uso (CU-xx)**: Cada operación documenta de manera unívoca el caso de uso del dominio que implementa (definidos en [`specs/features/0001-casos-uso/spec.md`](../features/0001-casos-uso/spec.md)).
3. **Manejo Estandarizado de Errores con Códigos SUNAT**: Se define un esquema común `ErrorResponse` que propaga tanto el código HTTP estándar como el código oficial de error devuelto por el OSE o SUNAT (Catálogo de Errores: e.g. `0100`, `2017`, `2324`).
4. **Semántica REST y Códigos HTTP Uniformes**:
   - `201 Created`: Creación y emisión exitosa de comprobantes, notas o registros.
   - `200 OK`: Consultas, listados y descargas exitosas.
   - `400 Bad Request`: Error sintáctico, campos obligatorios faltantes o tipos de datos inválidos.
   - `404 Not Found`: Recurso, comprobante o catálogo no encontrado.
   - `409 Conflict`: Serie/correlativo duplicado o documento ya dado de baja.
   - `422 Unprocessable Entity`: Error semántico o rechazo en validación fiscal de SUNAT/OSE.
   - `502 Bad Gateway`: Caída, indisponibilidad o timeout de los servicios SOAP de OSE/SUNAT.

---

## 2. Matriz de Endpoints y Mapeo a Casos de Uso y DTOs

| Método | Ruta | Caso de Uso | DTO de Entrada | DTO de Salida | Códigos HTTP | Descripción / Observaciones |
|---|---|---|---|---|---|---|
| `POST` | `/comprobantes` | `CU-01` / `CU-02` | `EmitirComprobanteRequest` | `ComprobanteResponse` | `201`, `400`, `404`, `409`, `422`, `502` | Emisión de Facturas (`01`) y Boletas (`03`). Realiza cálculo de IGV (18%), ICBPER (S/ 0.20), firma XMLDSig y despacho a OSE con CDR. |
| `GET` | `/comprobantes` | `CU-05` | *Query Params* (filtros) | `ComprobanteListResponse` | `200`, `400` | Búsqueda paginada de comprobantes filtrando por fecha, tipo, estado y RUC del receptor. |
| `GET` | `/comprobantes/{id}` | `CU-05` | *Path Param* (`id`: UUID) | `ComprobanteResponse` | `200`, `404` | Obtiene el detalle completo del comprobante, estado tributario y datos del CDR emitido. |
| `GET` | `/comprobantes/{id}/xml` | `CU-06` | *Path Param* (`id`: UUID) | Archivo binario (`application/xml`) | `200`, `404` | Descarga el archivo oficial XML UBL 2.1 firmado digitalmente con certificado X.509. |
| `GET` | `/comprobantes/{id}/pdf` | `CU-06` | *Path Param* (`id`: UUID) | Archivo binario (`application/pdf`) | `200`, `404` | Descarga la Representación Impresa en PDF con código QR normativo y hash de firma. |
| `GET` | `/comprobantes/{id}/cdr` | `CU-06` | *Path Param* (`id`: UUID) | Archivo binario (`application/xml`) | `200`, `404` | Descarga el XML de la Constancia de Recepción (CDR) emitido por el OSE o SUNAT. |
| `POST` | `/notas-credito` | `CU-03` | `NotaCreditoRequest` | `ComprobanteResponse` | `201`, `400`, `404`, `409`, `422`, `502` | Emisión de Nota de Crédito (`07`) asociada a una factura/boleta previa (Catálogo N° 09 SUNAT). |
| `POST` | `/notas-debito` | `CU-04` | `NotaDebitoRequest` | `ComprobanteResponse` | `201`, `400`, `404`, `409`, `422`, `502` | Emisión de Nota de Débito (`08`) para recargo de intereses o mora (Catálogo N° 10 SUNAT). |
| `POST` | `/comunicaciones-baja` | `CU-07` | `ComunicacionBajaRequest` | `TicketResponse` | `201`, `400`, `404`, `409`, `422`, `502` | Envía la anulación de facturas/notas a SUNAT dentro del plazo legal de 7 días. |
| `POST` | `/resumenes-diarios` | `CU-08` | `ResumenDiarioRequest` | `TicketResponse` | `201`, `400`, `404`, `409`, `422`, `502` | Consolida y remite las boletas del día a SUNAT para aprobación asíncrona por ticket. |
| `GET` | `/contribuyentes/{ruc}/validar` | `CU-09` | *Path Param* (`ruc`: 11 dígitos) | `ContribuyenteValidadoResponse` | `200`, `400`, `404`, `502` | Valida algoritmo Módulo 11 y consulta en padrón SUNAT si el RUC está ACTIVO y HABIDO. |
| `GET` | `/clientes` | `CU-10` | *Query Params* (`query`, `page`) | `ClienteListResponse` | `200` | Listado paginado de clientes registrados en el sistema. |
| `POST` | `/clientes` | `CU-10` | `ClienteRequest` | `ClienteResponse` | `201`, `400`, `409` | Registro de nuevo cliente con validación de tipo y número de documento (Catálogo 06). |
| `GET` | `/clientes/{id}` | `CU-10` | *Path Param* (`id`: UUID) | `ClienteResponse` | `200`, `404` | Consulta de información fiscal y de contacto de un cliente. |
| `PUT` | `/clientes/{id}` | `CU-10` | `ClienteRequest` | `ClienteResponse` | `200`, `400`, `404` | Actualización de razón social, dirección fiscal o correo para facturación. |
| `DELETE`| `/clientes/{id}` | `CU-10` | *Path Param* (`id`: UUID) | `OperacionExitosaResponse` | `200`, `404`, `409` | Desactivación lógica de un cliente (protege integridad si tiene comprobantes emitidos). |
| `GET` | `/productos` | `CU-11` | *Query Params* (`query`, `page`) | `ProductoListResponse` | `200` | Consulta del maestro de productos de abarrotes con precios y unidad de medida. |
| `POST` | `/productos` | `CU-11` | `ProductoRequest` | `ProductoResponse` | `201`, `400`, `409` | Alta de producto en el catálogo indicando afectación al IGV e indicador de ICBPER. |
| `GET` | `/productos/{id}` | `CU-11` | *Path Param* (`id`: UUID) | `ProductoResponse` | `200`, `404` | Detalle técnico y tributario de un producto de abarrotes. |
| `PUT` | `/productos/{id}` | `CU-11` | `ProductoRequest` | `ProductoResponse` | `200`, `400`, `404` | Actualización de precio, descripción o afectación impositiva de un producto. |
| `DELETE`| `/productos/{id}` | `CU-11` | *Path Param* (`id`: UUID) | `OperacionExitosaResponse` | `200`, `404`, `409` | Desactivación de producto de abarrotes del catálogo. |
| `GET` | `/series` | `CU-12` | *Query Params* (`tipo_comprobante`) | `SerieListResponse` | `200` | Listado de series autorizadas configuradas en el sistema. |
| `POST` | `/series` | `CU-12` | `SerieRequest` | `SerieResponse` | `201`, `400`, `409` | Habilitación de una nueva serie (e.g. `F001`, `B001`) con su correlativo inicial. |
| `GET` | `/series/{id}` | `CU-12` | *Path Param* (`id`: UUID) | `SerieResponse` | `200`, `404` | Consulta del estado y correlativo actual de una serie de emisión. |
| `PUT` | `/series/{id}` | `CU-12` | `SerieRequest` | `SerieResponse` | `200`, `400`, `404` | Ajuste de configuración operativa de la serie. |
| `DELETE`| `/series/{id}` | `CU-12` | *Path Param* (`id`: UUID) | `OperacionExitosaResponse` | `200`, `404` | Inhabilitación de serie para impedir nuevas emisiones. |
| `GET` | `/catalogos/{numero}` | `CU-13` | *Path Param* (`numero`: 2 dígitos) | `CatalogoSunatResponse` | `200`, `400`, `404` | Consulta de códigos y descripciones de las tablas normativas del Anexo 8 de SUNAT. |
| `POST` | `/migraciones/legado` | `CU-14` | `MigracionLegadoRequest` | `MigracionResponse` | `201`, `400`, `409`, `422` | Inicia procesamiento por lotes para importar ventas históricas desde CSV/DBF/JSON. |
| `GET` | `/migraciones/{id}` | `CU-14` | *Path Param* (`id`: UUID) | `MigracionResponse` | `200`, `404` | Consulta el avance y bitácora de errores del proceso de migración de datos históricos. |

---

## 3. Estructura del Esquema Común de Error (`ErrorResponse`)

Cuando ocurre cualquier condición de fallo en la API, el servidor retorna el esquema unificado `ErrorResponse`:

```json
{
  "codigo": "ERR_VALIDACION_FISCAL",
  "mensaje": "El comprobante no superó las validaciones del estándar UBL 2.1 o reglas de SUNAT.",
  "codigo_sunat": "2017",
  "mensaje_sunat": "El RUC del receptor no se encuentra en estado ACTIVO y HABIDO.",
  "fecha_hora": "2026-10-22T10:30:15Z",
  "detalles": [
    "receptor.numero_documento: El RUC '20555555551' reporta condición 'NO_HABIDO' ante SUNAT."
  ]
}
```

### Códigos de Error SUNAT Frecuentes Mapeados:
- `0100`: El sistema no puede emitir el comprobante (error en datos generales).
- `2017`: El número de RUC del receptor no existe o no está activo.
- `2024`: Comprobante ya fue informado con anterioridad (correlativo duplicado).
- `2324`: El valor unitario no coincide con el cálculo del importe total del ítem.
- `2800`: La fecha de emisión supera el plazo máximo legal permitido para su envío (máximo 3 días hacia atrás).
- `4000`: La firma digital del comprobante no coincide con el certificado registrado.
