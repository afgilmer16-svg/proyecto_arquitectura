# spec.md — 0001 Casos de Uso del Sistema

**Feature**: Casos de Uso del Sistema de Facturación Electrónica  
**ID**: 0001  
**Estado**: EN REVISIÓN  
**Autor**: Gilmer Ayala Falcón (Grupo A, UNAS-FIIS)  
**Fecha**: 2026-10-22  
**Fuentes**: RS 097-2012/SUNAT (F-03), RS 234-2006/SUNAT (F-04), OASIS UBL 2.1 (F-01), Catálogos SUNAT Anexo 8 (F-02)

---

## 1. Matriz General de Casos de Uso

| Código | Nombre del Caso de Uso | Actor Principal | Descripción |
|---|---|---|---|
| **CU-01** | Emitir Factura Electrónica | Cajero / Vendedor | Registra venta a adquirente con RUC válido, calcula tributos (IGV, ISC, ICBPER), genera XML UBL 2.1, firma digitalmente y envía a OSE/SUNAT obteniendo CDR. |
| **CU-02** | Emitir Boleta de Venta Electrónica | Cajero / Vendedor | Registra venta a consumidor final (persona natural con DNI o sin documento si el importe no es mayor a S/ 700), calcula tributos y prepara comprobante para envío individual o consolidado. |
| **CU-03** | Emitir Nota de Crédito Electrónica | Contador / Administrador | Modifica o anula una factura o boleta previa reduciendo el monto por devolución, descuento o corrección (Catálogo N° 09 SUNAT). |
| **CU-04** | Emitir Nota de Débito Electrónica | Contador / Administrador | Incrementa el valor de un comprobante previo por penalidades, moras o gastos adicionales (Catálogo N° 10 SUNAT). |
| **CU-05** | Consultar Comprobante y Estado CDR | Cajero / Contador / Cliente | Recupera los datos de un comprobante y su estado de procesamiento fiscal (EMITIDO, FIRMADO, ACEPTADO, RECHAZADO, DE_BAJA) y código de respuesta SUNAT. |
| **CU-06** | Descargar Artefactos Electrónicos | Contador / Cliente | Permite descargar los archivos oficiales: XML firmado (UBL 2.1), PDF (Representación Impresa con QR y hash) y CDR (Constancia de Recepción firmada por OSE/SUNAT). |
| **CU-07** | Comunicar de Baja Comprobante | Contador / Administrador | Genera y envía a SUNAT la Comunicación de Baja (`UBL-VoidedDocuments-1.0`) para anular comprobantes emitidos el mismo día o dentro del plazo normativo de 7 días. |
| **CU-08** | Generar y Enviar Resumen Diario | Contador / Sistema Batch | Consolida las boletas de venta y sus notas asociadas emitidas durante la jornada en un archivo XML resumen (`UBL-SummaryDocuments-1.0`) para su procesamiento asíncrono en SUNAT. |
| **CU-09** | Validar Contribuyente (RUC) | Cajero / Sistema | Valida que el RUC cumpla con el algoritmo Módulo 11 y consulta ante SUNAT su estado (ACTIVO) y condición de domicilio (HABIDO). |
| **CU-10** | Gestionar Clientes y Receptores | Vendedor / Administrador | Operaciones CRUD de clientes frecuentes, almacenando número de documento (RUC/DNI), razón social, dirección fiscal y correo electrónico para envío de comprobantes. |
| **CU-11** | Gestionar Catálogo de Productos | Almacenero / Administrador | Operaciones CRUD del maestro de productos de abarrotes, incluyendo código interno, descripción, unidad de medida (Cat. 03), precio unitario, código SUNAT y tipo de afectación al IGV (Cat. 07). |
| **CU-12** | Gestionar Series y Correlativos | Administrador | Configura y controla las series por tipo de comprobante (e.g. `F001`, `B001`, `FC01`) y asegura la asignación secuencial y atómica del correlativo sin huecos. |
| **CU-13** | Consultar Catálogos SUNAT | Sistema / Operador | Consulta de tablas normativas del Anexo 8 (Catálogo 01: Tipo comprobante, 02: Monedas, 05: Tributos, 06: Documento identidad, 07: Afectación IGV, etc.). |
| **CU-14** | Migrar Datos del Sistema Legado | Administrador / Técnico | Procesa archivos planos o bases de datos del sistema antiguo de abarrotes para migrar saldos, productos, clientes y series al nuevo modelo. No emite, firma ni envía comprobantes. |
| **CU-15** | Generar Registro de Ventas (PLE 14.1) | Contador / Administrador | Genera por periodo el Registro de Ventas e Ingresos a partir de los comprobantes aceptados y dados de baja. ⚠️ Por verificar si aplica SIRE/RVIE. |
| **CU-16** | Generar Registro de Inventario Permanente (PLE 12.1) | Contador / Almacenero | Genera por periodo el Registro de Inventario Permanente en Unidades Físicas desde los movimientos de almacén (ingresos, salidas y saldo). |

---

## 2. Detalle de Casos de Uso Principales

### CU-01: Emitir Factura Electrónica
- **Precondiciones**:
  - Cliente adquirente identificado con RUC de 11 dígitos en estado ACTIVO y HABIDO (F-03).
  - Serie autorizada que inicie con `F` (ejemplo: `F001`) y correlativo secuencial disponible.
  - Al menos un ítem con cantidad, precio unitario y tipo de afectación al IGV.
- **Flujo Principal**:
  1. El cajero ingresa los ítems de la venta (e.g., sacos de arroz, botellas de aceite, bolsas plásticas).
  2. El sistema calcula la base imponible por tipo de afectación (Gravado, Exonerado, Inafecto), el IGV (18%), el ISC (si aplica) y el ICBPER (importe por bolsa vigente a la fecha de emisión, S/ 0.50 desde 2023; parametrizado en `CatalogoSunatPort`).
  3. El sistema valida las reglas tributarias de cuadre de importes según la RS N° 097-2012.
  4. Se asigna el siguiente número correlativo de manera transaccional.
  5. Se construye el XML bajo la especificación OASIS UBL 2.1 (`UBL-Invoice-2.1.xsd`).
  6. Se firma digitalmente el XML con el certificado X.509 vía XMLDSig y se extrae el valor hash SHA-256.
  7. Se envía el archivo ZIP que contiene el XML firmado al endpoint del OSE (o SUNAT en contingencia).
  8. Se recibe el CDR del OSE con el estado ACEPTADO (código `0`) o RECHAZADO (códigos `2000` a `3999`) o ACEPTADO con observaciones (códigos `4000` en adelante).
  9. Se almacena el comprobante, el XML, el CDR y se genera el PDF con el código de barras bidimensional (QR) conforme a la RS N° 097-2012.
- **Postcondición**: Comprobante registrado y legalmente válido ante el fisco.

### CU-07: Comunicar de Baja Comprobante
- **Precondiciones**: Factura o nota previamente emitida en estado ACEPTADO, dentro del plazo legal de 7 días calendario posteriores a su emisión.
- **Flujo Principal**:
  1. El contador selecciona el comprobante y el motivo de baja (texto libre obligatorio, máx. 100 caracteres; ⚠️ Por verificar longitud contra F-05).
  2. El sistema genera el XML de comunicación de baja (`UBL-VoidedDocuments-1.0`).
  3. Se firma digitalmente y se envía al OSE/SUNAT.
  4. Se recibe el ticket y/o CDR que certifica la anulación legal del documento.
  5. El comprobante cambia de estado a `DE_BAJA` en el repositorio local.

### CU-09: Validar Contribuyente (RUC)
- **Precondiciones**: Número de RUC ingresado por el usuario.
- **Flujo Principal**:
  1. El sistema verifica la longitud (11 dígitos) y los dos primeros dígitos (`10`, `15`, `16`, `17`, `20`; ⚠️ Por verificar lista vigente [F-05]).
  2. Se ejecuta el cálculo del dígito verificador bajo el algoritmo Módulo 11 con pesos `[5, 4, 3, 2, 7, 6, 5, 4, 3, 2]`.
  3. Se consulta en la caché local o API de SUNAT para verificar la razón social, estado (ACTIVO) y condición (HABIDO).
  4. Se devuelven los datos validados para autocompletar el comprobante.

---

## 3. Detalle de los demás casos de uso

### CU-02: Emitir Boleta de Venta Electrónica
- **Precondiciones**: serie que inicia con `B`; al menos un ítem. Si el importe total es mayor a S/ 700, el comprador debe identificarse (DNI, CE o pasaporte). ⚠️ Por verificar umbral vigente [F-06, Reglamento de Comprobantes de Pago].
- **Flujo Principal**: igual a CU-01 desde el paso 2, con receptor DNI, CE, pasaporte o sin documento (`0`).
- **Diferencia clave**: la boleta no se envía de forma individual a SUNAT; se informa en el **resumen diario** (CU-08). El comprobante queda `FIRMADO` hasta que el resumen sea aceptado.
- **Alterno**: receptor con RUC → se permite boleta, pero se recomienda factura.
- **Excepciones**: serie con prefijo `F` → `422`; importe ≥ umbral sin identificar → `422`.

### CU-03: Emitir Nota de Crédito
- **Precondiciones**: comprobante afectado en estado `ACEPTADO`; motivo del Catálogo 09; serie con el prefijo del afectado.
- **Flujo Principal**: (1) el contador elige el comprobante y el motivo; (2) el sistema precarga las líneas; (3) valida que el total de la nota no supere el del afectado salvo motivo permitido; (4) asigna correlativo, genera `CreditNote`, firma y envía; (5) recibe CDR.
- **Excepciones**: comprobante no encontrado o no aceptado → `422`; motivo `01` (anulación) con importe distinto del total → `422`.

### CU-04: Emitir Nota de Débito
- **Precondiciones**: comprobante afectado `ACEPTADO`; motivo del Catálogo 10.
- **Flujo Principal**: igual a CU-03 con `DebitNote` y `RequestedMonetaryTotal`.
- **Excepciones**: igual a CU-03.

### CU-05: Consultar Comprobante y Estado CDR
- **Flujo Principal**: por `id` o por filtros (fecha, tipo, estado, RUC receptor, paginado) se devuelve el comprobante, su estado y el CDR.
- **Alterno**: estado `FIRMADO` con más de N minutos sin respuesta → el sistema consulta el estado al OSE (`getStatus`) y actualiza. ⚠️ Por verificar operación SOAP exacta [F-05].

### CU-06: Descargar Artefactos
- **Flujo Principal**: XML firmado, PDF (Representación Impresa con QR y hash) y CDR por `id`.
- **Excepciones**: artefacto aún inexistente (comprobante sin CDR) → `404`. Los comprobantes migrados del legado devuelven `404` en XML/CDR salvo que existan como evidencia.

### CU-08: Generar y Enviar Resumen Diario
- **Precondiciones**: boletas `FIRMADO` del día; correlativo diario del resumen.
- **Flujo Principal**: (1) el sistema (o el contador) selecciona las boletas del día; (2) genera `SummaryDocuments`; (3) firma y envía; (4) recibe ticket (`202`); (5) consulta el ticket hasta `TERMINADO`; (6) actualiza cada boleta a `ACEPTADO` o `RECHAZADO`.
- **Alterno**: ticket con error → el resumen se corrige y se reenvía con nuevo correlativo.

### CU-10: Gestionar Clientes
- **Flujo**: alta, consulta, actualización y desactivación lógica. Validación del tipo y número de documento (Catálogo 06; RUC con módulo 11).
- **Excepciones**: documento duplicado → `409`; desactivar cliente con comprobantes → solo desactivación lógica.

### CU-11: Gestionar Catálogo de Productos
- **Flujo**: CRUD con unidad (Catálogo 03), afectación IGV (Cat. 07), sistema ISC (Cat. 08) y marca de bolsa plástica.
- **Excepciones**: código duplicado → `409`; unidad fuera del catálogo → `400`.

### CU-12: Gestionar Series
- **Flujo**: crear serie con prefijo coherente al tipo (`F` ↔ `01`, `B` ↔ `03`); el correlativo lo avanza solo el servidor. Se puede activar o desactivar; nunca modificar el correlativo.
- **Excepciones**: serie duplicada → `409`.

### CU-13: Consultar Catálogos
- **Flujo**: devuelve códigos y descripciones por número de catálogo y versión vigente a una fecha.
- **Excepciones**: catálogo inexistente → `404`.

### CU-14: Migrar Datos del Sistema Legado
- **Flujo**: ver `specs/features/0006-migracion-legado/design.md`. **Nunca** emite ni envía comprobantes.
- **Excepciones**: lote con errores → cuarentena y reporte; conciliación distinta de cero → lote bloqueado.

### CU-15: Generar Registro de Ventas (PLE 14.1)
- **Precondiciones**: periodo `AAAAMM`; comprobantes `ACEPTADO` o `DE_BAJA` del periodo.
- **Flujo Principal**: el contador pide el periodo; el sistema arma las filas (ver `specs/features/0005-trazabilidad-sunat-ubl/spec.md`, sección 13) y entrega el TXT.
- **Excepciones**: periodo sin comprobantes → `404`.

### CU-16: Generar Registro de Inventario Permanente (PLE 12.1)
- **Flujo Principal**: por periodo y establecimiento se calculan entradas, salidas y saldo por producto desde `MovimientoAlmacen` (sección 14 de la spec 0005) y se entrega el TXT.
- **Excepciones**: periodo sin movimientos → `404`; saldo negativo → el reporte se genera con advertencia, no se corrige.

---

## 4. Flujos alternos transversales

### 4.1 Contingencia: OSE no disponible (CU-01, CU-02, CU-03, CU-04)
1. `EnvioOsePort` detecta timeout o error de conexión tras los reintentos configurados.
2. El comprobante permanece `FIRMADO` y se encola con reintento exponencial.
3. Si el OSE sigue caído más allá del umbral configurado, el adaptador de contingencia envía directo a SUNAT (ADR-002).
4. Al recibir el CDR, el estado pasa a `ACEPTADO` o `RECHAZADO`.
5. La caja entrega al cliente la representación impresa con la leyenda de estado pendiente. ⚠️ Por verificar el tratamiento legal del plazo de envío en contingencia [F-03].

### 4.2 Modo sin conexión en caja o móvil (ADR-003)
1. La app registra la operación localmente con UUID idempotente (`PENDIENTE`).
2. No se muestra "aceptado por SUNAT"; se muestra "registrado localmente".
3. Al recuperar red, sincroniza por serie y orden; el servidor asigna el correlativo y devuelve el resultado (`CONFIRMADO` o `ERROR`).
4. Un UUID ya procesado devuelve el resultado original (idempotencia).
