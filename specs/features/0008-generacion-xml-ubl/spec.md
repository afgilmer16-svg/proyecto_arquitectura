# spec.md — 0008 Generación del XML UBL 2.1 (Invoice)

**Feature**: Generación del XML de factura y boleta electrónica a partir del XSD UBL 2.1  
**ID**: 0008  
**Estado**: APROBADO (alcance del segundo parcial)  
**Autor**: Equipo Grupo A — UNAS-FIIS  
**Fecha**: 2026-11-11  
**Fuentes**: F-01 (`UBL-Invoice-2.1.xsd`, OASIS UBL 2.1 OS), F-05 (Guía XML Factura UBL 2.1, SUNAT v1.0), matriz [0005](../0005-trazabilidad-sunat-ubl/spec.md)  
**Capa**: Adaptador de salida (`GeneradorXmlUblPort` → `LxmlUblAdapter`)  
**Cubre**: segundo parcial, requisitos 2 (generación desde el XSD), 3 (lenguaje del dominio) y 4 (pruebas unitarias de generación)

---

## Descripción

A partir de los datos de un comprobante (DTO `EmitirComprobanteRequest` del contrato [`facturacion-api.yaml`](../../contracts/facturacion-api.yaml)) y del correlativo asignado por el servidor, el sistema genera el documento XML `Invoice` UBL 2.1 que exige SUNAT y comprueba que sea válido contra el XSD oficial. Descripción del caso: [`docs/segundo-parcial/01-descripcion-caso.md`](../../../docs/segundo-parcial/01-descripcion-caso.md).

## Alcance

**Incluye**

- Factura (`01`) y boleta (`03`) de venta interna (`0101`), en soles o dólares.
- Categorías de operación vistas en clase: gravada (1001), inafecta (1002), exonerada (1003) y gratuita (1004).
- IGV del 18 % (régimen general) y 10 % solo si se indica el régimen de restaurantes y hoteles.
- ISC en sus tres sistemas (Catálogo 08) e ICBPER según la fecha de emisión.
- Forma de pago al contado o al crédito con cuotas.
- Validación de los datos de entrada y validación del XML contra `UBL-Invoice-2.1.xsd`.

**No incluye** (parciales posteriores o fuera del caso)

- Firma digital (`ext:UBLExtensions` / `ds:Signature`), envío al OSE y CDR.
- Notas de crédito y débito (usan otros XSD), comunicación de baja y resumen diario.
- Afectaciones `11`–`17` (gravadas gratuitas e IVAP) y `40` (exportación): se rechazan con un error explícito.
- Descuentos y cargos (`cac:AllowanceCharge`), detracciones, anticipos.
- Integración con Odoo como addon, Docker y despliegue.

## Requisitos funcionales

| ID | Requisito | Fuente |
|---|---|---|
| RF-XML-01 | Generar un XML `Invoice` UBL 2.1 en UTF-8 con declaración XML, válido contra `UBL-Invoice-2.1.xsd`. | F-01 |
| RF-XML-02 | Construir el XML como árbol de elementos, en el orden de las secuencias del XSD, sin plantillas de texto. | Docente, ADR-005 |
| RF-XML-03 | Validar el XML contra el XSD antes de entregarlo; si no cumple, rechazarlo con la lista de errores. | Docente |
| RF-XML-04 | Ubicar cada dato en el XPath definido en la matriz 0005. | Matriz 0005 |
| RF-XML-05 | Calcular IGV, ISC e ICBPER en el dominio, con decimales exactos; el cliente no envía impuestos ni totales. | 0002, docente |
| RF-XML-06 | Informar cada categoría con su tributo del Catálogo 05: 1000 IGV (S), 9997 EXONERADO (E), 9998 INAFECTO (O) y 9996 GRATUITO (Z). | F-05 §23–26 |
| RF-XML-07 | Rechazar datos faltantes o inválidos con un mensaje por problema, indicando la ruta del campo. | Segundo parcial |
| RF-XML-08 | Leer la entrada como UTF-8 y detectar archivos guardados en WIN1252. | Clase (codificación) |

## Reglas de validación de la entrada

| ID | Regla |
|---|---|
| RV-01 | `tipo_comprobante` ∈ {`01`, `03`}; la serie cumple `^[FB][A-Z0-9]{3}$` y empieza con `F` para facturas y `B` para boletas. |
| RV-02 | El correlativo lo asigna el servidor (1–99 999 999); si el cliente lo envía, se rechaza. |
| RV-03 | RUC del emisor con 11 dígitos, prefijo 10/15/16/17/20 y dígito verificador módulo 11; ubigeo de 6 dígitos. |
| RV-04 | La factura exige receptor con RUC válido; el DNI tiene 8 dígitos; una boleta mayor a S/ 700 identifica al comprador (⚠️ Por verificar monto). |
| RV-05 | Unidad de medida del Catálogo 03; cantidad > 0 e importes como texto decimal exacto. |
| RV-06 | ISC: el sistema `01` exige tasa, el `02` monto fijo y unidad base, y el `03` tasa y PVP; solo en líneas gravadas. |
| RV-07 | Numeración de líneas 1, 2, 3… sin saltos. |
| RV-08 | Crédito: al menos una cuota, fechas ≥ emisión y suma de cuotas = monto pendiente ≤ total. |

## Criterios de aceptación

- [x] Dado el ejemplo de abarrotes, cuando se genera el XML, entonces es válido contra `UBL-Invoice-2.1.xsd` (`test_xml_generado_es_valido_contra_el_xsd_oficial`).
- [x] Dado un comprobante con las cuatro categorías, cuando se genera, entonces cada línea usa el tributo de su categoría (`TestCategoriasDeOperacion`).
- [x] Dado un ítem con ISC, cuando se genera, entonces el ISC entra en la base del IGV y `TierRange` lleva el código del Catálogo 08 (`TestIscEIcbper`).
- [x] Dado un dato faltante o inválido, cuando se intenta generar, entonces se lanza `DatosFacturaInvalidosError` con la ruta del campo (`TestDatosInvalidos`).
- [x] Dado un XML alterado (sin un elemento obligatorio, fuera de orden, con un elemento inexistente o con un tipo incorrecto), cuando se valida, entonces el validador lo rechaza (`TestValidacionXsd`).
- [x] `pytest -v` ejecuta 89 pruebas de generación y 310 de verificación integral sin fallos (`docs/segundo-parcial/resultados-pruebas.txt`).
- [x] Dado un error introducido a propósito en el generador, la suite lo detecta: 16/16 mutantes (`tests/mutacion.py`).
- [x] Dado el ejemplo del contrato, el XML reproduce los importes de la respuesta `201` (`test_ejemplo_del_contrato_produce_los_importes_que_publica_el_contrato`).

## Por verificar

> ⚠️ Estructura de las líneas gratuitas, códigos del ICBPER y numeración de cuotas: tomados de la matriz 0005 y de la Guía XML v1.0. Hay que contrastarlos con el anexo de validaciones vigente de SUNAT. Ver [`docs/pendientes-verificacion.md`](../../../docs/pendientes-verificacion.md).
