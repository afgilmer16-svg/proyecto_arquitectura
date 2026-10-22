# spec.md — 0001 Casos de Uso del Sistema

**Feature**: Casos de Uso del Sistema de Facturación Electrónica  
**ID**: 0001  
**Estado**: APROBADO  
**Autor**: Equipo Grupo A — UNAS-FIIS  
**Fecha**: 2026-10-22  
**Fuentes**: RS 097-2012/SUNAT (F-03), RS 234-2006/SUNAT (F-04), OASIS UBL 2.1 (F-01), Catálogos SUNAT Anexo 8 (F-02)

---

## 1. Matriz General de Casos de Uso

| Código | Nombre del Caso de Uso | Actor Principal | Descripción |
|---|---|---|---|
| **CU-01** | Emitir Factura Electrónica | Cajero / Vendedor | Registra venta a adquirente con RUC válido, calcula tributos (IGV, ISC, ICBPER), genera XML UBL 2.1, firma digitalmente y envía a OSE/SUNAT obteniendo CDR. |
| **CU-02** | Emitir Boleta de Venta Electrónica | Cajero / Vendedor | Registra venta a consumidor final (persona natural con DNI o sin documento si importe < S/ 700), calcula tributos y prepara comprobante para envío individual o consolidado. |
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
| **CU-14** | Migrar Datos del Sistema Legado | Administrador / Técnico | Procesa archivos planos o bases de datos del sistema antiguo de abarrotes para migrar saldos, productos, clientes y series al nuevo modelo. |

---

## 2. Detalle de Casos de Uso Principales

### CU-01: Emitir Factura Electrónica
- **Precondiciones**:
  - Cliente adquirente identificado con RUC de 11 dígitos en estado ACTIVO y HABIDO (F-03).
  - Serie autorizada que inicie con `F` (ejemplo: `F001`) y correlativo secuencial disponible.
  - Al menos un ítem con cantidad, precio unitario y tipo de afectación al IGV.
- **Flujo Principal**:
  1. El cajero ingresa los ítems de la venta (e.g., sacos de arroz, botellas de aceite, bolsas plásticas).
  2. El sistema calcula la base imponible por tipo de afectación (Gravado, Exonerado, Inafecto), el IGV (18%), el ISC (si aplica) y el ICBPER (S/ 0.20 por bolsa plástica).
  3. El sistema valida las reglas tributarias de cuadre de importes según la RS N° 097-2012.
  4. Se asigna el siguiente número correlativo de manera transaccional.
  5. Se construye el XML bajo la especificación OASIS UBL 2.1 (`UBL-Invoice-2.1.xsd`).
  6. Se firma digitalmente el XML con el certificado X.509 vía XMLDSig y se extrae el valor hash SHA-256.
  7. Se envía el archivo ZIP que contiene el XML firmado al endpoint del OSE (o SUNAT en contingencia).
  8. Se recibe el CDR del OSE con el estado ACEPTADO (código `0`) o RECHAZADO (códigos `2000` a `4000`).
  9. Se almacena el comprobante, el XML, el CDR y se genera el PDF con el código de barras bidimensional (QR) conforme a la RS N° 097-2012.
- **Postcondición**: Comprobante registrado y legalmente válido ante el fisco.

### CU-07: Comunicar de Baja Comprobante
- **Precondiciones**: Factura o nota previamente emitida en estado ACEPTADO, dentro del plazo legal de 7 días calendario posteriores a su emisión.
- **Flujo Principal**:
  1. El contador selecciona el comprobante y el motivo de baja (Catálogo 09 / RS 097-2012).
  2. El sistema genera el XML de comunicación de baja (`UBL-VoidedDocuments-1.0`).
  3. Se firma digitalmente y se envía al OSE/SUNAT.
  4. Se recibe el ticket y/o CDR que certifica la anulación legal del documento.
  5. El comprobante cambia de estado a `DE_BAJA` en el repositorio local.

### CU-09: Validar Contribuyente (RUC)
- **Precondiciones**: Número de RUC ingresado por el usuario.
- **Flujo Principal**:
  1. El sistema verifica la longitud (11 dígitos) y los dos primeros dígitos (`10`, `15`, `17`, `20`).
  2. Se ejecuta el cálculo del dígito verificador bajo el algoritmo Módulo 11 con pesos `[5, 4, 3, 2, 7, 6, 5, 4, 3, 2]`.
  3. Se consulta en la caché local o API de SUNAT para verificar la razón social, estado (ACTIVO) y condición (HABIDO).
  4. Se devuelven los datos validados para autocompletar el comprobante.
