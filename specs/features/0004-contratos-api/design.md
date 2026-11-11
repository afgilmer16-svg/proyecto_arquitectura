# design.md — 0004 Contratos de API

> **Entregable 3** del examen. Contrato completo: [`facturacion-api.yaml`](../../contracts/facturacion-api.yaml). Complementa a [`spec.md`](spec.md).


**Documento**: Especificación y Mapeo de Contratos de API  
**Carpeta**: `specs/features/0004-contratos-api/`  
**Estado**: EN REVISIÓN  
**Autores**: Equipo Grupo A — UNAS-FIIS  
**Fecha**: 2026-10-22  
**Fuentes Normativas**: RS N° 097-2012/SUNAT (F-03), RS N° 234-2006/SUNAT (F-04), OASIS UBL 2.1 (F-01), Catálogos SUNAT Anexo 8 (F-02)

---

## 1. Visión General del Contrato de API

La interfaz de programación de aplicaciones (API) define los contratos formales para la interacción con el sistema de facturación electrónica de **ABC Software Corp**, formalizada bajo la especificación **OpenAPI 3.1.0** en [`specs/contracts/facturacion-api.yaml`](../../contracts/facturacion-api.yaml).

### Principios de Diseño:
0. **Correlativo asignado por el servidor**: ninguna solicitud de emisión (`/comprobantes`, `/notas-credito`, `/notas-debito`) incluye `correlativo`; el cliente solo indica la `serie`. El servidor lo asigna de forma atómica y lo devuelve en la respuesta.
1. **Reutilización Estricta mediante `$ref`**: Todos los modelos de datos y DTOs se definen de manera centralizada en `components/schemas` y se referencian mediante punteros JSON Schema (`$ref: '#/components/schemas/...'`), eliminando cualquier duplicidad en las definiciones de los endpoints.
2. **Trazabilidad Directa con Casos de Uso (CU-xx)**: Cada operación documenta de manera unívoca el caso de uso del dominio que implementa (definidos en [`specs/features/0001-casos-uso/spec.md`](../0001-casos-uso/spec.md)).
3. **Manejo Estandarizado de Errores con Códigos SUNAT**: Se define un esquema común `ErrorResponse` que propaga tanto el código HTTP estándar como el código oficial de error devuelto por el OSE o SUNAT (Catálogo de Errores: e.g. `0100`, `2017`, `2324`).
4. **Semántica REST y Códigos HTTP Uniformes**:
   - `201 Created`: Creación y emisión exitosa de comprobantes, notas o registros.
   - `202 Accepted`: Comunicación de baja y resumen diario; SUNAT los procesa de forma asíncrona y se consultan por ticket.
   - `401 Unauthorized` / `403 Forbidden`: autenticación JWT (`bearerAuth`) ausente o permisos insuficientes; declarados en todas las operaciones.
   - `200 OK`: Consultas, listados y descargas exitosas.
   - `400 Bad Request`: Error sintáctico, campos obligatorios faltantes o tipos de datos inválidos.
   - `404 Not Found`: El recurso **de la ruta** (comprobante, cliente, producto, serie, catálogo) no existe. Una serie o un comprobante referenciado inexistente dentro del cuerpo de una emisión se responde con `422`.
   - `409 Conflict`: Serie/correlativo duplicado o documento ya dado de baja.
   - `422 Unprocessable Entity`: Error semántico o rechazo en validación fiscal de SUNAT/OSE.
   - `502 Bad Gateway`: Caída, indisponibilidad o timeout de los servicios SOAP de OSE/SUNAT.

---

## 2. Matriz de Endpoints y Mapeo a Casos de Uso y DTOs

| Método | Ruta | Caso de Uso | DTO de Entrada | DTO de Salida | Códigos HTTP | Descripción / Observaciones |
|---|---|---|---|---|---|---|
| `POST` | `/comprobantes` | `CU-01` / `CU-02` | `EmitirComprobanteRequest` | `ComprobanteResponse` | `201`, `400`, `401`, `403`, `409`, `422`, `502` | Emisión de Facturas (`01`) y Boletas (`03`). Realiza cálculo de IGV (18%), ICBPER (importe vigente por fecha, S/ 0.50 desde 2023), firma XMLDSig y despacho a OSE con CDR. |
| `GET` | `/comprobantes` | `CU-05` | *Query Params* (filtros) | `ComprobanteListResponse` | `200`, `400` | Búsqueda paginada de comprobantes filtrando por fecha, tipo, estado y RUC del receptor. |
| `GET` | `/comprobantes/{id}` | `CU-05` | *Path Param* (`id`: UUID) | `ComprobanteResponse` | `200`, `404` | Obtiene el detalle completo del comprobante, estado tributario y datos del CDR emitido. |
| `GET` | `/comprobantes/{id}/xml` | `CU-06` | *Path Param* (`id`: UUID) | Archivo binario (`application/xml`) | `200`, `404` | Descarga el archivo oficial XML UBL 2.1 firmado digitalmente con certificado X.509. |
| `GET` | `/comprobantes/{id}/pdf` | `CU-06` | *Path Param* (`id`: UUID) | Archivo binario (`application/pdf`) | `200`, `404` | Descarga la Representación Impresa en PDF con código QR normativo y hash de firma. |
| `GET` | `/comprobantes/{id}/cdr` | `CU-06` | *Path Param* (`id`: UUID) | Archivo binario (`application/xml`) | `200`, `404` | Descarga el XML de la Constancia de Recepción (CDR) emitido por el OSE o SUNAT. |
| `POST` | `/notas-credito` | `CU-03` | `NotaCreditoRequest` | `ComprobanteResponse` | `201`, `400`, `401`, `403`, `409`, `422`, `502` | Emisión de Nota de Crédito (`07`) asociada a una factura/boleta previa (Catálogo N° 09 SUNAT). |
| `POST` | `/notas-debito` | `CU-04` | `NotaDebitoRequest` | `ComprobanteResponse` | `201`, `400`, `401`, `403`, `409`, `422`, `502` | Emisión de Nota de Débito (`08`) para recargo de intereses o mora (Catálogo N° 10 SUNAT). |
| `POST` | `/comunicaciones-baja` | `CU-07` | `ComunicacionBajaRequest` | `TicketResponse` | `202`, `400`, `401`, `403`, `404`, `409`, `422`, `502` | Envía la anulación de facturas/notas a SUNAT dentro del plazo legal de 7 días. |
| `POST` | `/resumenes-diarios` | `CU-08` | `ResumenDiarioRequest` | `TicketResponse` | `202`, `400`, `401`, `403`, `409`, `422`, `502` | Consolida y remite las boletas del día a SUNAT para aprobación asíncrona por ticket. |
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
| `GET` | `/libros/registro-ventas` | `CU-15` | *Query* (`periodo`) | Archivo TXT | `200`, `400`, `401`, `403`, `404` | Registro de Ventas e Ingresos (PLE 14.1) del periodo. |
| `GET` | `/libros/inventario-permanente` | `CU-16` | *Query* (`periodo`, `codigo_establecimiento`) | Archivo TXT | `200`, `400`, `401`, `403`, `404` | Registro de Inventario Permanente en Unidades Físicas (PLE 12.1). |
| `GET` | `/catalogos/{numero}` | `CU-13` | *Path Param* (`numero`: 2 dígitos) | `CatalogoSunatResponse` | `200`, `400`, `404` | Consulta de códigos y descripciones de las tablas normativas del Anexo 8 de SUNAT. |
| `POST` | `/migraciones/legado` | `CU-14` | `MigracionLegadoRequest` | `MigracionResponse` | `201`, `400`, `409`, `422` | Inicia procesamiento por lotes para importar ventas históricas desde CSV/DBF/JSON. |
| `GET` | `/migraciones/{id}` | `CU-14` | *Path Param* (`id`: UUID) | `MigracionResponse` | `200`, `404` | Consulta el avance y bitácora de errores del proceso de migración de datos históricos. |
| `GET` | `/migraciones/{id}/cuarentena` | `CU-14` | *Path Param* (`id`) + filtros `estado`, `codigo_error`, `pagina`, `tamano` | `CuarentenaListaResponse` | `200`, `400`, `404` | Lista los registros rechazados del lote con su código de error estable y su estado de reproceso. |
| `POST` | `/migraciones/{id}/reprocesos` | `CU-14` | `ReprocesoCuarentenaRequest` + cabecera `Idempotency-Key` | `MigracionResponse` | `202`, `400`, `404`, `409`, `422` | Aplica parches aprobados a registros en cuarentena y crea un lote enlazado (`lote_origen_id`). Nunca emite ni envía comprobantes. |

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
- Plazos de envío verificados en SUNAT Orientación (2026-11-07): factura, 7 días calendario desde el día siguiente a la emisión; resumen diario, el día de emisión o a más tardar el 7.º día calendario siguiente; comunicación de baja, hasta 7 días calendario. ⚠️ Por verificar: el código exacto de error por emisión tardía y un literal de 3 días en la página «Operatividad» (ver `docs/pendientes-verificacion.md`) [F-05].
- Clasificación de códigos de error SUNAT (Manual del Programador, §4.1, p. 29): `0100–0999` excepciones propias de SUNAT, `1000–1999` excepciones de formato y estructura, `2000–3999` rechazo, `4000+` observaciones que no invalidan el documento.
- ⚠️ Por verificar: códigos de error por firma digital inválida [F-05].

Rango de respuestas del CDR: `0` aceptado; `2000`–`3999` rechazo; `4000` en adelante aceptado con observaciones.


---

## 4. Reglas del contrato que corrigen revisiones previas

| Regla | Detalle |
|---|---|
| El cliente no envía impuestos ni totales | `ItemComprobanteDto` solo trae datos comerciales (`cantidad`, `valor_unitario`, afectación, parámetros de ISC, `cantidad_bolsas`). `ItemCalculadoDto` y `TotalesComprobanteDto` solo se devuelven en `ComprobanteResponse`. |
| Importes como texto decimal | Ningún importe usa `number`/`double`. |
| Unidad de medida cerrada | `UnidadMedidaEnum` (Catálogo 03); un texto como `UNID` o `SACO` se rechaza. ⚠️ Por verificar: ampliar con la lista vigente del Anexo 8. |
| ISC condicional | `sistema_isc` (Catálogo 08) exige `tasa_isc` (01), `monto_fijo_isc` + `unidad_base_isc` (02) o `tasa_isc` + `precio_venta_publico_sugerido` (03). Validado con `if/then` en el esquema. |
| Serie | Patrón `^[FB][A-Z0-9]{3}$`. El servidor rechaza con `422` un prefijo que no coincida con el tipo (`01` con `F`, `03` con `B`). `PUT /series/{id}` usa `SerieUpdateRequest` (solo `descripcion` y `activa`). |
| Seguridad | `bearerAuth` global; todas las operaciones declaran `401` y `403` mediante `components/responses`. |
| Validación | Verificación estructural propia (2026-11-11): 0 `$ref` rotos, `operationId` únicos, ejemplos conformes. `openapi-spec-validator` sin errores (`pytest -v`, 15/15, 2026-11-11). **Pendiente:** `redocly lint` (requiere Node.js). |
