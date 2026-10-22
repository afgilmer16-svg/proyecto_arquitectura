---
**Documento**: Glosario de términos del dominio
**Carpeta**: specs/00-contexto/
**Estado**: BORRADOR
**Autor**: Equipo Grupo A
**Fecha**: 2026-10-22
**Fuentes**: RS 097-2012/SUNAT (F-03), RS 234-2006/SUNAT (F-04), UBL 2.1 (F-01), Catálogos SUNAT (F-02)
---

# Glosario de Términos — Facturación Electrónica SUNAT

> Los términos marcados con ⚠️ incluyen aspectos pendientes de verificación contra la fuente normativa citada.

---

## UBL — Universal Business Language

**Definición**: Estándar internacional de documentos de negocio en formato XML, desarrollado por OASIS (Organization for the Advancement of Structured Information Standards). La versión 2.1 es la exigida por SUNAT para la emisión de comprobantes electrónicos en el Perú.

**En el sistema**: Todos los comprobantes (facturas, boletas, notas de crédito, notas de débito) se generan como archivos XML que deben cumplir los esquemas XSD de UBL 2.1 con las extensiones propietarias de SUNAT.

**Esquemas principales**:
- `UBL-Invoice-2.1.xsd` — Factura y boleta de venta [F-01]
- `UBL-CreditNote-2.1.xsd` — Nota de crédito [F-01]
- `UBL-DebitNote-2.1.xsd` — Nota de débito [F-01]
- `UBL-VoidedDocuments-1.0.xsd` — Comunicación de baja [F-03]
- `UBL-SummaryDocuments-1.0.xsd` — Resumen diario [F-03]

**Fuente**: [F-01], OASIS UBL 2.1 — https://docs.oasis-open.org/ubl/os-UBL-2.1/

---

## OSE — Operador de Servicios Electrónicos

**Definición**: Empresa autorizada por SUNAT para actuar como intermediario en la emisión electrónica de comprobantes de pago. El OSE recibe el XML firmado del emisor, valida su conformidad con las normas SUNAT y devuelve el CDR. Simultáneamente, informa a SUNAT de los comprobantes procesados.

**Ejemplos conocidos**: Nubefact, Efact, Digiflow, Factura Perú, entre otros.

**Ventajas frente al SEE propio**: El OSE se encarga de actualizar su sistema ante cambios normativos; el emisor no necesita mantener la conexión directa con SUNAT.

**Fuente**: [F-03, RS 097-2012, Art. que regula el OSE] ⚠️ Por verificar: artículo exacto y lista actualizada de OSE autorizados [F-03, F-05].

---

## CDR — Constancia de Recepción

**Definición**: Documento XML generado y firmado por SUNAT o el OSE en respuesta al envío de un comprobante electrónico. El CDR certifica que el comprobante fue recibido y procesado, e indica el estado resultante.

**Estados posibles del CDR**:

| Estado | Descripción |
|--------|-------------|
| `0` — ACEPTADO | El comprobante es válido y ha sido aceptado por SUNAT. |
| ACEPTADO CON OBSERVACIONES | El comprobante fue aceptado pero contiene advertencias no bloqueantes. ⚠️ Por verificar: comportamiento exacto [F-05]. |
| RECHAZADO | El comprobante tiene errores que impiden su aceptación. Debe corregirse y reenviarse (con nuevo número correlativo). |

**Contenido del CDR**: incluye el código de respuesta SUNAT, la descripción del estado y la fecha/hora de procesamiento.

**Fuente**: [F-03, F-05].

---

## IGV — Impuesto General a las Ventas

**Definición**: Impuesto al valor agregado aplicado sobre la venta de bienes y la prestación de servicios en el Perú. Equivalente al IVA en otros países.

**Tasa**: 18% (incluye el 2% de Impuesto de Promoción Municipal — IPM). ⚠️ Por verificar: confirmar tasa vigente y si IPM debe desglosarse en el XML [F-03, F-02].

**En el XML UBL 2.1**: Se declara dentro del elemento `cac:TaxTotal` con el código de tributo `1000` (IGV) del Catálogo N° 05 de SUNAT [F-02].

**Tipos de afectación al IGV** (Catálogo N° 07 SUNAT) ⚠️ Por verificar lista completa [F-02]:
- `10` — Gravado — Operación onerosa
- `20` — Exonerado — Operación onerosa
- `30` — Inafecto — Operación onerosa
- `40` — Exportación de bienes y/o servicios

**Fuente**: [F-02, Catálogo 05 y 07], [F-03].

---

## ISC — Impuesto Selectivo al Consumo

**Definición**: Impuesto que grava la producción o importación de determinados bienes (bebidas alcohólicas, cigarrillos, combustibles, vehículos, entre otros). En el sector abarrotes puede afectar a bebidas gaseosas, cervezas y otros productos específicos.

**Sistemas de cálculo** ⚠️ Por verificar cuál aplica a los productos de ABC Software Corp [F-03, F-02]:
- **Al valor**: porcentaje sobre el precio de venta.
- **Específico**: monto fijo por unidad de medida.
- **Al precio de venta al público**: porcentaje sobre el PVP.

**En el XML UBL 2.1**: Se declara dentro de `cac:TaxTotal` con el código de tributo `2000` (ISC) del Catálogo N° 05 [F-02].

**Fuente**: [F-02, Catálogo 05], [F-03]. ⚠️ Por verificar: lista de productos de abarrotes sujetos a ISC.

---

## ICBPER — Impuesto al Consumo de Bolsas de Plástico

**Definición**: Impuesto creado por la Ley N° 30884 (Ley del Plástico) que grava la adquisición de bolsas de plástico en establecimientos comerciales. El monto se cobra por cada bolsa entregada al cliente al momento de la venta.

**Monto**: S/ 0.20 por bolsa. ⚠️ Por verificar: monto vigente (puede haber incrementos anuales según la ley) [F-03].

**En el XML UBL 2.1**: Se declara dentro de `cac:TaxTotal` con el código de tributo `7152` (ICBPER) del Catálogo N° 05 [F-02]. ⚠️ Por verificar: código exacto en el catálogo SUNAT [F-02].

**Fuente**: Ley N° 30884, [F-02, Catálogo 05].

---

## RUC — Registro Único de Contribuyentes

**Definición**: Número de identificación fiscal de 11 dígitos asignado por SUNAT a personas naturales con negocio y personas jurídicas (empresas) en el Perú. Es obligatorio para la emisión de facturas electrónicas tanto del emisor como del receptor.

**Estructura**: 11 dígitos numéricos. El último dígito es un dígito verificador calculado por un algoritmo de módulo 11. ⚠️ Por verificar: algoritmo exacto de validación del dígito verificador [F-05].

**Estados del RUC relevantes**: ACTIVO, SUSPENDIDO, BAJA DEFINITIVA, BAJA PROVISIONAL. Solo el estado ACTIVO permite emitir y recibir facturas [F-03]. ⚠️ Por verificar: si la validación del estado debe hacerse en línea o es responsabilidad del emisor [F-05].

**En el XML UBL 2.1**: Se declara en `cac:AccountingSupplierParty` (emisor) y `cac:AccountingCustomerParty` (receptor) con el esquema de identificación `6` (RUC) del Catálogo N° 06 [F-02].

**Fuente**: [F-02, Catálogo 06], [F-03].

---

## Serie

**Definición**: Código alfanumérico que identifica el punto de emisión de un comprobante. Junto con el número correlativo, forma el identificador único del comprobante.

**Formato por tipo de comprobante** ⚠️ Por verificar exactitud [F-03]:
| Tipo | Formato | Ejemplo |
|------|---------|---------|
| Factura electrónica | `F` + 3 dígitos | `F001` |
| Boleta de venta electrónica | `B` + 3 dígitos | `B001` |
| Nota de crédito (referenciada a factura) | `F` + 3 dígitos | `F001` |
| Nota de crédito (referenciada a boleta) | `B` + 3 dígitos | `B001` |
| Nota de débito (referenciada a factura) | `F` + 3 dígitos | `F001` |

**En el XML UBL 2.1**: Forma parte del campo `cbc:ID` con el formato `SERIE-CORRELATIVO` (ej. `F001-00000001`) [F-01, F-03].

**Fuente**: [F-03].

---

## Correlativo

**Definición**: Número secuencial de 8 dígitos que identifica de forma única un comprobante dentro de una serie. Se incrementa automáticamente con cada emisión y **nunca se reutiliza**, ni siquiera tras la anulación del comprobante.

**Formato**: 8 dígitos con ceros a la izquierda. Ejemplo: `00000001`, `00000002`. ⚠️ Por verificar: longitud exacta exigida por SUNAT [F-03].

**Reglas**:
- El correlativo debe ser estrictamente secuencial sin saltos. ⚠️ Por verificar si SUNAT acepta saltos en correlativos [F-03].
- Un comprobante rechazado por SUNAT que no se re-emite consume el correlativo igualmente (queda como baja).

**Fuente**: [F-03].

---

## Comunicación de Baja

**Definición**: Documento electrónico que el emisor envía a SUNAT para informar la anulación de uno o más comprobantes electrónicos (facturas, boletas, notas) ya emitidos. A diferencia de la nota de crédito, la comunicación de baja solo se puede utilizar cuando el comprobante **no ha sido pagado ni entregado al receptor**, o cuando SUNAT lo permite expresamente.

**Plazo**: Debe enviarse dentro de los **7 días hábiles** siguientes a la fecha de emisión del comprobante a anular. ⚠️ Por verificar: plazo exacto y excepciones [F-03, F-05].

**Formato**: XML con esquema `UBL-VoidedDocuments-1.0.xsd` (estructura propia de SUNAT, no estándar OASIS). ⚠️ Por verificar: esquema y namespace exacto [F-03, F-05].

**Respuesta de SUNAT**: Un CDR con el estado de la baja (aceptada o rechazada).

**Fuente**: [F-03, F-05].

---

## Resumen Diario de Boletas

**Definición**: Documento electrónico consolidado que el emisor envía a SUNAT al final de cada día (o al inicio del día siguiente) para informar todas las boletas de venta electrónicas emitidas durante esa jornada. A diferencia de las facturas, las boletas no se envían individualmente en tiempo real; se consolidan en el resumen diario.

**Plazo de envío**: Al cierre del día de emisión o hasta las 23:59 horas del día siguiente. ⚠️ Por verificar: plazo exacto [F-03, F-05].

**Formato**: XML con esquema `UBL-SummaryDocuments-1.0.xsd`. ⚠️ Por verificar: esquema exacto [F-03, F-05].

**Respuesta**: Un ticket de SUNAT (procesamiento asíncrono). El emisor debe consultar el resultado usando el ticket recibido.

**Fuente**: [F-03, F-05].

---

## Nota de Crédito

**Definición**: Documento electrónico que modifica (total o parcialmente) una factura o boleta ya emitida, reduciendo el importe. Se usa para corregir errores, aplicar descuentos posteriores o gestionar devoluciones.

**Tipos de motivo** (Catálogo N° 09 SUNAT) ⚠️ Por verificar lista completa [F-02]:
- `01` — Anulación de la operación
- `02` — Anulación por error en el RUC
- `03` — Corrección por error en la descripción
- `04` — Descuento global
- `05` — Descuento por ítem
- `06` — Devolución total
- `07` — Devolución por ítem
- `13` — Ajustes de operaciones de exportación

**Relación con el comprobante original**: La nota de crédito debe referenciar el número del comprobante original (serie y correlativo) [F-01, F-03].

**Formato**: XML `UBL-CreditNote-2.1.xsd` con extensiones SUNAT.

**Fuente**: [F-01, F-02, Catálogo 09], [F-03].

---

## Nota de Débito

**Definición**: Documento electrónico que modifica (total o parcialmente) una factura o boleta ya emitida, incrementando el importe. Se usa para cobrar penalidades, intereses u otros cargos adicionales no incluidos en el comprobante original.

**Tipos de motivo** (Catálogo N° 10 SUNAT) ⚠️ Por verificar lista completa [F-02]:
- `01` — Intereses por mora
- `02` — Aumento en el valor
- `03` — Penalidades / otros conceptos

**Relación con el comprobante original**: La nota de débito debe referenciar el número del comprobante original [F-01, F-03].

**Formato**: XML `UBL-DebitNote-2.1.xsd` con extensiones SUNAT.

**Fuente**: [F-01, F-02, Catálogo 10], [F-03].

---

## Firma Digital — XMLDSig

**Definición**: Mecanismo criptográfico de firma electrónica aplicado a documentos XML, basado en el estándar **XML Signature Syntax and Processing** (XMLDSig) del W3C. SUNAT exige que todos los comprobantes electrónicos estén firmados digitalmente antes de ser enviados.

**Componentes**:
- **Certificado digital X.509**: Emitido por una Entidad de Certificación (CA) autorizada por INDECOPI. ⚠️ Por verificar: lista de CAs autorizadas vigentes [F-03, F-05].
- **Clave privada**: Custodiada de forma segura por el emisor (en archivo `.pfx` o HSM). **Nunca debe estar en el repositorio de código.**
- **Clave pública**: Incluida en el certificado X.509 y embebida en el XML firmado.

**Ubicación en el XML UBL 2.1**: La firma se ubica dentro del elemento `ext:UBLExtensions / ext:UBLExtension / ext:ExtensionContent / ds:Signature`, usando el namespace `http://www.w3.org/2000/09/xmldsig#` [F-01, F-03].

**Algoritmos requeridos por SUNAT** ⚠️ Por verificar versiones exactas [F-03, F-05]:
- Canonicalización: `C14N` (Canonical XML 1.0)
- Firma: `RSA-SHA1` o `RSA-SHA256`
- Digest: `SHA-1` o `SHA-256`

**Fuente**: W3C XMLDSig, [F-01, F-03, F-05].
