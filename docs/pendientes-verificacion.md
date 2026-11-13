# Pendientes de verificación — registro de fuentes

**Fecha de la última revisión:** 2026-11-11  
Este documento registra qué dato "Por verificar" ya se contrastó con una fuente oficial, cuál se contrastó solo en parte y cuál sigue abierto. Las fuentes son páginas y PDF públicos de SUNAT y de JetBrains/Kotlin consultados en línea ese día. Las fuentes F-01 a F-05 de `.agent/rules/spec-first.md` siguen siendo las de referencia: **conviene cotejar cada dato con el PDF original antes de la entrega.**

## 1. Verificado

| Dato | Valor | Fuente | Dónde aplicarlo |
|---|---|---|---|
| Clasificación de códigos de error SUNAT | `0100–0999`: excepciones propias de SUNAT. `1000–1999`: excepciones de formato y estructura del contribuyente. `2000–3999`: errores que generan **rechazo**. `4000+`: **observaciones** que no invalidan el documento. | Manual del Programador (SUNAT), sección 4.1, p. 29 | `ErrorResponse.codigo_sunat` en el OpenAPI; `specs/features/0004-contratos-api/design.md` |
| Ejemplos de códigos reales | `2047` (rechazo: falta `AdditionalMonetaryTotal` 1001/1002/1003), `4001` (observación: RUC del receptor no existe), `4031` (observación: falta nombre comercial) | Manual del Programador, Anexo 1, pp. 42–49 | Ejemplos de errores |
| El listado completo de errores no está en el manual | Remite al "parámetro 742" | Manual del Programador | Sigue abierto: ver 3 |
| Plazo de envío de **factura** | 7 días calendario contados desde el día siguiente a la emisión | SUNAT Orientación, "11. Emisión – Factura Electrónica" | `empaquetado.md`, `spec` de 0003/0005 |
| Plazo del **resumen diario** de boletas (y notas asociadas) | El día de emisión o, a más tardar, el 7.º día calendario siguiente | SUNAT Orientación, "04. Resumen Diario – Boleta de Venta Electrónica" | Resumen diario |
| Plazo de **comunicación de baja** | Hasta 7 días calendario desde el día siguiente a la recepción del CDR aceptado (facturas y boletas informadas); boletas no informadas, 7 días desde el día siguiente a su generación | SUNAT Orientación, "03. Operatividad" | Comunicación de baja |
| ISC en UBL | Se declara en `cbc:TierRange` con el catálogo 08; el tributo ISC lleva ID `2000`, nombre `ISC`, código internacional `EXC` | Guía XML Factura UBL 2.1 (SUNAT, v1.0, mayo 2017), pp. 17 y 23 | Matriz SUNAT↔UBL |
| Afectación al IGV | Se declara en `cbc:TaxExemptionReasonCode` con el catálogo 07 | Guía XML, p. 17 | Matriz |
| Documento del emisor | Tipo `6` (RUC), número de 11 dígitos (`n11`) | Guía XML, pp. 5 y 39 | Matriz |
| Versiones de la pila multiplataforma | Ver sección 4 | JetBrains y kotlinlang.org | `specs/features/0007-distribucion-multiplataforma/design.md` |

### 1.1 Verificado el 2026-11-11 (Guía XML de Factura v1.0, copia local)

| Dato | Valor | Dónde aplicarlo |
|---|---|---|
| Tributos por tipo de operación | Gravada `1000`/`IGV`/`VAT`; exonerada `9997`/`EXO`/`VAT`; inafecta `9998`/`INA`/`FRE`; gratuita `9996`/`GRATUITO`/`FRE`; ISC `2000`/`ISC`/`EXC`; otros `9999`/`OTH`. Categorías UN/ECE 5305: `S`, `E`, `O`, `Z` | Spec 0005, sección 18.1 |
| Ubicación de `TierRange` | `.../cac:TaxSubtotal/cac:TaxCategory/cbc:TierRange` con valores `01`, `02`, `03` | Spec 0005 |
| Atributos de `ProfileID` | `schemeName`, `schemeAgencyName="PE:SUNAT"`, `schemeURI` (la guía usa `catalogo17`) | Spec 0005, fila "Tipo de operación" |
| Atributos de unidad de medida | `unitCode`, `unitCodeListID="UN/ECE rec 20"`, `unitCodeListAgencyName` | Spec 0005 |
| Leyendas (Cat. 52) | `1000` monto en letras; `1002` transferencia gratuita | Spec 0005, sección 18.3 |
| `UBLVersionID` `2.1`, `CustomizationID` `2.0` | Confirmados | Spec 0005 |

## 2. Parcialmente verificado

| Dato | Qué se sabe | Qué falta |
|---|---|---|
| Plazo de la factura | La página "11. Emisión" dice 7 días. La página "03. Operatividad" contiene además un literal de **3 días** para la misma obligación. | Cotejar con el texto de la RS 097-2012/SUNAT (y sus modificatorias) para saber cuál rige según la modalidad. Mientras tanto, el contrato usa 7 días y lo marca. |
| Tasas de ISC | La guía solo muestra un ejemplo de ISC con el sistema `01`. Los sistemas `02` y `03` de la matriz son coherentes con el catálogo 08, pero la guía consultada no los lista. | Las tasas reales dependen del bien y de los Apéndices del TUO de la Ley del IGV e ISC y sus decretos vigentes. Los valores de los ejemplos del contrato son **ilustrativos**, no normativos. No hay que presentarlos como tasas oficiales. |
| Código del tributo ISC | Resuelto el 2026-11-11: en los ejemplos completos de la guía el ISC usa `2000`/`ISC`/`EXC`. | Ninguna. |

## 2.1 Corregido el 2026-11-11

| Dato | Antes | Ahora |
|---|---|---|
| Objeto de la RS 234-2006/SUNAT | Se describía como "requisitos generales de los comprobantes de pago" | Libros y registros vinculados a asuntos tributarios (Registro de Ventas, Inventario Permanente). Los requisitos de facturas y boletas están en el Reglamento de Comprobantes de Pago (RS 007-99/SUNAT, F-06) |
| Umbral de identificación en boletas | "mayor o igual a S/ 700" y citado a F-04 | "mayor a S/ 700", citado a F-06; sigue **Por verificar** el monto vigente |
| Puertos de entrada de CU-08 y CU-09 | Tenían servicio en el diagrama MVC pero no puerto en la tabla | `EnviarResumenDiarioUseCase` y `ValidarContribuyenteUseCase` |

## 2.2 Generador XML del segundo parcial (2026-11-11)

| Dato | Decisión tomada | Qué falta |
|---|---|---|
| 3 XSD de firma agregados (`UBL-xmldsig-core-schema-2.1.xsd`, `UBL-XAdESv132-2.1.xsd`, `UBL-XAdESv141-2.1.xsd`) | Copias OS 2.1 de `thegreenter/ubl-validator`, con SHA-256 en `docs/referencias/ubl/README.md` | Ninguna: el 2026-11-11 se compararon con `docs.oasis-open.org` y los SHA-256 coinciden byte a byte (también el de `UBL-Invoice-2.1.xsd`). |
| Línea gratuita | Tributo `9996` GRA (`Z`), `LineExtensionAmount` = valor referencial, `PriceTypeCode` `02`, `Price` `0.00`, sin sumar al total | Contrastar con el anexo de validaciones vigente; el ejemplo de la guía v1.0 usa `9998` en la línea y `LineExtensionAmount` `0.00`. |
| `Percent` en líneas no gravadas | `0.00` (matriz 0005 §8) | La guía v1.0 muestra `18.00` en exoneradas e inafectas. |
| Cuotas de crédito | `cbc:ID` = `FormaPago`, `cbc:PaymentMeansID` = `Cuota001`…, `cbc:Amount`, `cbc:PaymentDueDate` | Guía posterior a la RS 193-2020. |
| ICBPER | Tributo `7152` / `ICBPER` / `OTH`; `BaseUnitMeasure` en `TaxSubtotal` y `PerUnitAmount` en `TaxCategory`; importe por año (0.10 en 2019 a 0.50 desde 2023) | Guía SUNAT de ICBPER y Catálogo 05 vigente. |
| IGV 10 % (restaurantes y hoteles) | Parámetro `RegimenIgv.RESTAURANTES_HOTELES`, nunca por defecto | Norma y vigencia del régimen. |
| Boleta sin documento (`tipo_documento` `0`) | Número `-`; rechazada si supera S/ 700 | Formato exacto exigido por SUNAT. |
| ISC sistema 02 | `BaseUnitMeasure` = cantidad de la línea en `unidad_base_isc` | Que la cantidad se exprese en la unidad normativa del bien. |

## 3. Abierto (no se pudo verificar con las fuentes accesibles)

| Dato | Por qué sigue abierto | Dónde buscarlo |
|---|---|---|
| Prefijos de RUC válidos (10, 15, 16, 17, 20) | La guía XML solo exige RUC válido de 11 dígitos; no describe prefijos. | Ficha/Consulta RUC de SUNAT y la norma que regula el RUC |
| Lista completa de códigos de error por tipo de documento | El manual remite al parámetro 742 | Anexo de errores de la RS vigente del OSE/SOL |
| Plazo en contingencia | Ninguna de las páginas consultadas lo fija | RS 097-2012 y modificatorias |
| Texto completo del Anexo 8 (catálogos) | No está en `docs/referencias/sunat/` | RS 097-2012/SUNAT y sus anexos vigentes en sunat.gob.pe/legislacion |
| Guías de comunicación de baja y de resumen diario (XML/XSD) | No se accedió a los PDF específicos | Sección de documentación técnica de cpe.sunat.gob.pe |

## 4. Versiones de la pila multiplataforma (verificadas el 2026-11-07)

| Componente | Valor | Fuente |
|---|---|---|
| Compose Multiplatform | **1.12.2** (última estable listada) | Página de compatibilidad de Compose Multiplatform (kotlinlang.org) |
| Kotlin | **2.4.21** (8-oct-2026) | Página de releases de Kotlin (kotlinlang.org) |
| Kotlin mínimo requerido por Compose Multiplatform | 2.1.0 | Misma página de compatibilidad |
| JDK mínimo para escritorio | 11; para empaquetar distribuciones nativas (`jpackage`) **17 o superior** | Misma página de compatibilidad |
| Android mínimo de Compose Multiplatform | API 21 (Android 5.0) | Misma página de compatibilidad |
| Windows mínimo | Windows 10 (x86-64) | Misma página de compatibilidad |
| **Decisión del equipo:** JDK 21 (LTS) y `minSdk` 26 | No son datos de una fuente. Son decisiones de diseño dentro de los mínimos anteriores | `specs/features/0007-distribucion-multiplataforma/design.md` |

## 5. Tareas que requieren una acción humana

- [ ] Descargar a `docs/referencias/sunat/` los PDF de la RS 097-2012, RS 234-2006, Anexo 8 y las guías de baja/resumen, y cotejar la sección 2 y 3 de este documento.
- [x] Ejecutar `pytest -v` (incluye `openapi-spec-validator` real): 512 aprobadas el 2026-11-12 (`docs/resultados-pruebas-python.txt`). Queda opcional `npx @redocly/cli lint specs/contracts/facturacion-api.yaml` (requiere Node.js y descargar el paquete; no se pudo ejecutar en esta sesión).
- [ ] Obtener una muestra del sistema antiguo y confirmar los supuestos S-01 a S-11 de `specs/features/0006-migracion-legado/diccionario-legado.md`.
- [ ] Revisión por un segundo miembro del equipo (si el examen es en equipo).
- [ ] Confirmar `minSdk` con los dispositivos reales de preventa.

## Fuentes consultadas

- Guía XML Factura UBL 2.1 — https://cpe.sunat.gob.pe/sites/default/files/inline-files/guia%2Bxml%2Bfactura%2Bversion%202-1%2B1%2B0%20%282%29_0%20%282%29.pdf
- Manual del Programador — https://cpe.sunat.gob.pe/sites/default/files/inline-files/manual_programador%20%281%29.pdf
- SUNAT Orientación, Operatividad — https://orientacion.sunat.gob.pe/3529-operatividad
- SUNAT Orientación, Resumen Diario — https://orientacion.sunat.gob.pe/04-resumen-diario-boleta-de-venta-electronica
- SUNAT Orientación, Emisión de Factura — https://orientacion.sunat.gob.pe/11-emision-factura-electronica
- Compatibilidad de Compose Multiplatform — https://kotlinlang.org/docs/multiplatform/compose-compatibility-and-versioning.html
- Releases de Kotlin — https://kotlinlang.org/docs/releases.html
