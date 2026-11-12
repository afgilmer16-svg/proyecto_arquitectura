# Especificación: Trazabilidad SUNAT / UBL 2.1

**Feature**: Matriz de trazabilidad SUNAT — UBL 2.1
**ID**: 0005
**Estado**: EN REVISIÓN
**Autor**: Equipo Grupo A — UNAS-FIIS
**Fecha**: 2026-11-05
**Fuentes**: F-01, F-02, F-03, F-04, F-05

---

## 1. Propósito

Este artefacto establece la correspondencia exacta entre:
- cada **dato normativo SUNAT** exigido en un comprobante electrónico,
- el **catálogo / norma** que lo define,
- el **XPath UBL 2.1** con atributos obligatorios (`listAgencyName`, `listURI`, `schemeID`, `unitCode`, etc.),
- el **campo del DTO** (request/response de `specs/contracts/facturacion-api.yaml`), y
- la **regla de validación** que el dominio debe aplicar antes de generar el XML.

La trazabilidad cubre: cabecera de factura/boleta, datos de emisor y receptor, forma de pago, tributos (IGV, ISC, ICBPER), totales monetarios y, por línea, cantidad, precio, referencias de precio y TaxExemptionReasonCode.

---

## 2. Alcance

| Documento UBL | Raíz XML | Cubierto |
|---|---|---|
| Factura Electrónica | `Invoice` | ✅ |
| Boleta de Venta | `Invoice` (tipo `03`) | ✅ |
| Nota de Crédito | `CreditNote` | ✅ (parcial, cabecera y referencia) |
| Nota de Débito | `DebitNote` | ✅ (parcial, cabecera y referencia) |
| Comunicación de Baja | `VoidedDocuments` | ✅ (cabecera) |
| Resumen Diario | `SummaryDocuments` | ✅ (cabecera) |

---

## 3. Matriz de trazabilidad — Cabecera del comprobante

Abreviaturas: **O** = obligatorio; **C** = condicional; **OP** = opcional.

| Dato SUNAT | Catálogo / Norma | XPath UBL 2.1 (con atributos) | Campo DTO | Regla de validación |
|---|---|---|---|---|
| Versión UBL | UBL 2.1 / guía XML SUNAT | `/Invoice/cbc:UBLVersionID` | *(constante en servidor)* | Valor fijo `"2.1"`. No expuesto al cliente. **O** |
| Versión de personalización peruana | Perfil SUNAT | `/Invoice/cbc:CustomizationID` | *(constante en servidor)* | Valor `"2.0"` según guía XML SUNAT vigente. **O** |
| Tipo de operación | Cat. 51 | `/Invoice/cbc:ProfileID` con `@schemeName="SUNAT:Identificador de Tipo de Operación"` `@schemeAgencyName="PE:SUNAT"` `@schemeURI` (la guía v1.0 consigna `urn:pe:gob:sunat:cpe:see:gem:catalogos:catalogo17`; ⚠️ Por verificar si la URI vigente es `catalogo51`) | `tipo_operacion` (default `"0101"`) | Código Cat. 51, ej. `"0101"` venta interna. **O** en factura/boleta |
| Tipo de comprobante | Cat. 01 | `/Invoice/cbc:InvoiceTypeCode` con `@listAgencyName="PE:SUNAT"` `@listURI="urn:pe:gob:sunat:cpe:see:gem:catalogos:catalogo01"` `@listID="0101"` | `tipo_comprobante` | Enum `"01"` (Factura), `"03"` (Boleta); patrón `^(0[13])$`. **O** |
| Serie y correlativo (ID del comprobante) | RS 097-2012 / guía SEE | `/Invoice/cbc:ID` | *(concatenado en servidor:* `serie + "-" + correlativo`*)* | Serie: `^[FB][A-Z0-9]{3}$` (F para facturas, B para boletas). Correlativo asignado atómicamente por el servidor; entero 1–99 999 999. El cliente **no** envía correlativo. **O** |
| Fecha de emisión | Guía XML SUNAT | `/Invoice/cbc:IssueDate` | `fecha_emision` | ISO 8601 `YYYY-MM-DD`; no futura; dentro de plazo de envío vigente. **O** |
| Hora de emisión | Guía XML SUNAT | `/Invoice/cbc:IssueTime` | `hora_emision` | Formato `HH:mm:ss`; zona horaria `America/Lima`. **O** |
| Código de moneda | Cat. 02 | `/Invoice/cbc:DocumentCurrencyCode` con `@listID="ISO 4217 Alpha"` | `moneda` | Enum ISO 4217: `"PEN"`, `"USD"`. Todos los `@currencyID` del documento deben coincidir. **O** |
| Cantidad de líneas | Guía XML SUNAT (p. 5) | `/Invoice/cbc:LineCountNumeric` | *(calculado en servidor)* | Igual al número de `cac:InvoiceLine`. **O** |

---

## 4. Matriz de trazabilidad — Emisor (Supplier)

| Dato SUNAT | Catálogo / Norma | XPath UBL 2.1 | Campo DTO | Regla de validación |
|---|---|---|---|---|
| RUC del emisor | Cat. 06 / RUC | `/Invoice/cac:AccountingSupplierParty/cac:Party/cac:PartyIdentification/cbc:ID` con `@schemeID="6"` `@schemeName="SUNAT:Identificador de Documento de Identidad"` `@schemeAgencyName="PE:SUNAT"` `@schemeURI="urn:pe:gob:sunat:cpe:see:gem:catalogos:catalogo06"` (Guía XML v1.0, p. 7) | `emisor.ruc` | 11 dígitos; prefijo `10/15/16/17/20`; dígito verificador módulo 11 (pesos 5-4-3-2-7-6-5-4-3-2). **O** |
| Razón social del emisor | RUC / guía XML | `/Invoice/cac:AccountingSupplierParty/cac:Party/cac:PartyLegalEntity/cbc:RegistrationName` | `emisor.razon_social` | No vacío; debe corresponder al RUC en el padrón SUNAT. **O** |
| Nombre comercial | Guía XML SUNAT | `/Invoice/cac:AccountingSupplierParty/cac:Party/cac:PartyName/cbc:Name` | `emisor.nombre_comercial` | Texto libre; omitir elemento si no existe. **OP** |
| Ubigeo del emisor | Cat. 13 | `/Invoice/cac:AccountingSupplierParty/cac:Party/cac:PartyLegalEntity/cac:RegistrationAddress/cbc:ID` con `@schemeAgencyName="PE:INEI"` | `emisor.ubigeo` | 6 dígitos numéricos. **O** |
| Dirección fiscal del emisor | Guía XML SUNAT | `/Invoice/cac:AccountingSupplierParty/cac:Party/cac:PartyLegalEntity/cac:RegistrationAddress/cac:AddressLine/cbc:Line` | `emisor.direccion` | No vacío. **O** |
| Código de establecimiento anexo | Guía XML SUNAT (p. 7) | `/Invoice/cac:AccountingSupplierParty/cac:Party/cac:PartyLegalEntity/cac:RegistrationAddress/cbc:AddressTypeCode` | *(constante `"0000"` = domicilio fiscal)* | 4 dígitos; `0000` cuando se emite desde el domicilio fiscal. **O** |
| País del emisor | ISO 3166-1 | `/Invoice/cac:AccountingSupplierParty/cac:Party/cac:PartyLegalEntity/cac:RegistrationAddress/cac:Country/cbc:IdentificationCode` con `@listID="ISO 3166-1"` | *(constante `"PE"`)* | Código ISO 3166-1 alfa-2. **O** |

---

## 5. Matriz de trazabilidad — Receptor (Customer)

| Dato SUNAT | Catálogo / Norma | XPath UBL 2.1 | Campo DTO | Regla de validación |
|---|---|---|---|---|
| Tipo de documento del receptor | Cat. 06 | `/Invoice/cac:AccountingCustomerParty/cac:Party/cac:PartyIdentification/cbc:ID/@schemeID` | `receptor.tipo_documento` | Enum Cat. 06: `"6"` RUC, `"1"` DNI, `"4"` Carné Extranjería, `"7"` Pasaporte, `"0"` Sin documento. En factura (tipo `01`) el receptor debe tener RUC (`"6"`). **O/C** |
| Número de documento del receptor | Cat. 06 | `/Invoice/cac:AccountingCustomerParty/cac:Party/cac:PartyIdentification/cbc:ID` | `receptor.numero_documento` | Longitud y formato según tipo: RUC = 11 dígitos + módulo 11; DNI = 8 dígitos; Pasaporte ≤ 12 caracteres. **O/C** |
| Razón social / nombres del receptor | Guía XML SUNAT | `/Invoice/cac:AccountingCustomerParty/cac:Party/cac:PartyLegalEntity/cbc:RegistrationName` | `receptor.razon_social` | No vacío cuando el receptor tiene documento. **O/C** |
| Dirección del receptor | Guía XML SUNAT | `/Invoice/cac:AccountingCustomerParty/cac:Party/cac:PartyLegalEntity/cac:RegistrationAddress/cac:AddressLine/cbc:Line` | `receptor.direccion` | Texto; recomendado cuando el receptor tiene RUC. **C/OP** |

---

## 6. Matriz de trazabilidad — Forma de pago

| Dato SUNAT | Catálogo / Norma | XPath UBL 2.1 | Campo DTO | Regla de validación |
|---|---|---|---|---|
| Indicador de pago | RS 000193-2020 / guía XML | `/Invoice/cac:PaymentTerms/cbc:ID` con valor `"FormaPago"` | *(derivado de `forma_pago.tipo`)* | Siempre presente; valor literal `"FormaPago"`. **O** |
| Tipo de pago (Contado / Crédito) | RS 000193-2020 | `/Invoice/cac:PaymentTerms/cbc:PaymentMeansID` | `forma_pago.tipo` | `"Contado"` o `"Credito"`. Si es `"Credito"`, debe haber al menos una cuota. **O** |
| Monto neto pendiente (crédito) | RS 000193-2020 | `/Invoice/cac:PaymentTerms/cbc:Amount` con `@currencyID` | `forma_pago.monto_neto_pendiente` | Requerido cuando tipo es `"Credito"`; `> 0`; misma moneda del documento. **C** |
| Cuotas de pago | RS 000193-2020 | `/Invoice/cac:PaymentTerms[cbc:PaymentMeansID='Cuota001']/cbc:Amount` y `cbc:PaymentDueDate` (cada cuota repite `cbc:ID='FormaPago'`; numeración `Cuota001`, `Cuota002`…; ⚠️ Por verificar contra la guía posterior a la RS 193-2020) | `forma_pago.cuotas[].monto` / `fecha_vencimiento` | Fecha de vencimiento ≥ fecha de emisión; suma de cuotas = monto neto pendiente. **C** |

---

## 7. Matriz de trazabilidad — Línea de detalle (InvoiceLine)

> **Nota de contrato**: los campos DTO de precio con impuestos (`precio_unitario`), `valor_total` y los montos de IGV/ISC/ICBPER son **calculados por el servidor** y solo aparecen en la respuesta (`ItemCalculadoDto`); el cliente envía únicamente los datos comerciales (`ItemComprobanteDto`). Importes como texto decimal; `unidad_medida` es un enum cerrado del Catálogo 03.

| Dato SUNAT | Catálogo / Norma | XPath UBL 2.1 (con atributos) | Campo DTO | Regla de validación |
|---|---|---|---|---|
| Número de línea | UBL 2.1 | `/Invoice/cac:InvoiceLine/cbc:ID` | `items[].numero_linea` | Entero positivo, único y secuencial dentro del documento. **O** |
| Cantidad facturada | UBL 2.1 | `/Invoice/cac:InvoiceLine/cbc:InvoicedQuantity` con `@unitCode` (Cat. 03), `@unitCodeListID="UN/ECE rec 20"` y `@unitCodeListAgencyName="United Nations Economic Commission for Europe"` | `items[].cantidad` | `> 0`; precisión decimal conforme a guía; `unitCode` debe ser un código UNECE válido del Cat. 03. **O** |
| Unidad de medida | Cat. 03 (UNECE) | `/Invoice/cac:InvoiceLine/cbc:InvoicedQuantity/@unitCode` | `items[].unidad_medida` | Enum del Catálogo 03 de SUNAT (ej.: `"NIU"` unidad, `"KGM"` kilogramo, `"BX"` caja, `"GLL"` galón). **No** texto libre. **O** |
| Descripción del ítem | Guía XML SUNAT | `/Invoice/cac:InvoiceLine/cac:Item/cbc:Description` | `items[].descripcion` | No vacío; ≤ 250 caracteres. **O** |
| Código del producto (interno) | Guía XML SUNAT | `/Invoice/cac:InvoiceLine/cac:Item/cac:SellersItemIdentification/cbc:ID` | `items[].codigo_producto` | Código SKU interno estable; no confundir con UNSPSC. **OP/C** |
| Valor unitario sin impuestos | Guía XML SUNAT | `/Invoice/cac:InvoiceLine/cac:Price/cbc:PriceAmount` con `@currencyID` | `items[].valor_unitario` | `≥ 0`; decimal exacto como string; misma moneda del documento; redondeo a 10 decimales. **O** |
| Precio de venta con impuestos | Cat. 16 | `/Invoice/cac:InvoiceLine/cac:PricingReference/cac:AlternativeConditionPrice/cbc:PriceAmount` con `@currencyID` y `cbc:PriceTypeCode="01"` (Cat. 16) | `items[].precio_unitario` | Valor unitario + IGV/ISC aplicable; `PriceTypeCode="01"` precio unitario con tributos. **O/C** |
| Valor de venta total de la línea | Guía XML SUNAT | `/Invoice/cac:InvoiceLine/cbc:LineExtensionAmount` con `@currencyID` | `items[].valor_total` | `= cantidad × valor_unitario ± ajustes de línea`; redondeo a 2 decimales. **O** |
| Tipo de afectación al IGV | Cat. 07 | `/Invoice/cac:InvoiceLine/cac:TaxTotal/cac:TaxSubtotal/cac:TaxCategory/cbc:TaxExemptionReasonCode` con `@listAgencyName="PE:SUNAT"` `@listName="Afectacion del IGV"` `@listURI="urn:pe:gob:sunat:cpe:see:gem:catalogos:catalogo07"` | `items[].tipo_afectacion_igv` | Código Cat. 07 (ej. `"10"` Gravado, `"20"` Exonerado, `"30"` Inafecto). Coherente con tasa e ISC. **O** |

---

## 8. Matriz de trazabilidad — IGV por línea (TaxSubtotal ID=1000)

| Dato SUNAT | Catálogo / Norma | XPath UBL 2.1 | Campo DTO | Regla de validación |
|---|---|---|---|---|
| Base imponible IGV | Cat. 05 / guía | `/Invoice/cac:InvoiceLine/cac:TaxTotal/cac:TaxSubtotal[cac:TaxCategory/cac:TaxScheme/cbc:ID='1000']/cbc:TaxableAmount` con `@currencyID` | *(calculado por dominio)* | `= valor_total` cuando afectación `"10"`; solo líneas gravadas; las exoneradas, inafectas y gratuitas usan `TaxScheme` `9997`, `9998` y `9996` (sección 18.1). **O/C** |
| Monto IGV de la línea | Cat. 05 | `.../cbc:TaxAmount` con `@currencyID` | *(calculado por dominio)* | `= base × 0.18`; redondeo a 2 decimales; `0.00` en líneas exoneradas/inafectas, que no se declaran bajo el tributo `1000` sino con su propio `TaxScheme` (sección 18.1). **O/C** |
| Código de tributo IGV | Cat. 05 | `.../cac:TaxScheme/cbc:ID="1000"`, `cbc:Name="IGV"`, `cbc:TaxTypeCode="VAT"` | *(constante servidor)* | Fijo: `ID=1000`, `Name=IGV`, `TaxTypeCode=VAT`. **O** |
| Tasa IGV | Cat. 05 / TUO IGV | `.../cac:TaxCategory/cbc:Percent` | *(parámetro `regimen_igv` del servidor)* | `18.00` en el régimen general; `10.00` solo si se indica el régimen de restaurantes y hoteles (regla vista en clase; ⚠️ Por verificar vigencia); `0.00` si no gravado. **O/C** |

---

## 9. Matriz de trazabilidad — ISC por línea (TaxSubtotal ID=2000)

> El ISC **debe calcularse antes que el IGV** porque forma parte de la base imponible del IGV en los sistemas al valor. Aplica principalmente a bebidas alcohólicas, tabaco, combustibles y algunos bienes; es **C**ondicional para un negocio de abarrotes con esos productos.

| Dato SUNAT | Catálogo / Norma | XPath UBL 2.1 | Campo DTO | Regla de validación |
|---|---|---|---|---|
| Sistema de cálculo ISC | Cat. 08 | `/Invoice/cac:InvoiceLine/cac:TaxTotal/cac:TaxSubtotal[.../cbc:ID='2000']/cac:TaxCategory/cbc:TierRange` | `items[].sistema_isc` | Enum Cat. 08: `"01"` Al valor, `"02"` Específico, `"03"` Al valor según PVP. Requerido si el ítem está afecto a ISC. **C** |
| Base imponible ISC | Cat. 05 / TUO | `.../cbc:TaxableAmount` con `@currencyID` | *(calculado por dominio)* | Sistema `01`: base = valor de venta; Sistema `02`: base = cantidad × monto físico; Sistema `03`: base derivada del PVP. **C** |
| Tasa ISC (sistemas 01 y 03) | TUO ISC | `.../cac:TaxCategory/cbc:Percent` | `items[].tasa_isc` | Porcentaje según el bien y fecha; dato del catálogo vigente. Solo para sistemas `01` y `03`. **C** |
| Monto fijo por unidad (sistema 02) | TUO ISC | `.../cac:TaxCategory/cbc:PerUnitAmount` con `@currencyID` | `items[].monto_fijo_isc` | Importe fijo normativo vigente por unidad física. Solo para sistema `02`. **C** |
| Volumen / cantidad base (sistema 02) | TUO ISC | `.../cbc:BaseUnitMeasure` con `@unitCode` (Cat. 03) | `items[].unidad_base_isc` | Cantidad física en la unidad normativa del bien. **C** |
| PVP sugerido (sistema 03) | TUO ISC | ⚠️ Por verificar: elemento exacto en perfil SUNAT para consignar PVP | `items[].precio_venta_publico_sugerido` | Precio de venta al público sugerido del fabricante. **C** ⚠️ |
| Monto ISC de la línea | Cat. 05 | `.../cbc:TaxAmount` con `@currencyID` | *(calculado por dominio)* | Resultado de la fórmula del sistema aplicado; redondeo a 2 decimales. **C** |
| Código tributo ISC | Cat. 05 | `.../cac:TaxScheme/cbc:ID="2000"`, `Name="ISC"`, `TaxTypeCode="EXC"` | *(constante servidor)* | Fijo. **C** |

---

## 10. Matriz de trazabilidad — ICBPER por línea (TaxSubtotal ID=7152)

| Dato SUNAT | Catálogo / Norma | XPath UBL 2.1 | Campo DTO | Regla de validación |
|---|---|---|---|---|
| Número de bolsas plásticas | Ley 30884 / guía XML | `.../cbc:BaseUnitMeasure` con `@unitCode="NIU"` | `items[].cantidad_bolsas` | Entero ≥ 1; distintas de la cantidad comercial del ítem. **C** |
| Importe por bolsa (parametrizado) | Ley 30884 / D.S. vigente | `.../cac:TaxCategory/cbc:PerUnitAmount` con `@currencyID="PEN"` | *(del catálogo vigente por fecha)* | ⚠️ **S/ 0.50** desde 01/01/2023. No codificar como constante; parametrizar por fecha de vigencia en `CatalogoSunatPort`. **C** |
| Monto ICBPER de la línea | Ley 30884 | `.../cbc:TaxAmount` con `@currencyID="PEN"` | *(calculado: cantidad_bolsas × importe_vigente)* | `= BaseUnitMeasure × PerUnitAmount`; redondeo a 2 decimales. **C** |
| Código tributo ICBPER | Cat. 05 | `.../cac:TaxScheme/cbc:ID="7152"`, `Name="ICBPER"`, `TaxTypeCode="OTH"` | *(constante servidor)* | Fijo. **C** |

---

## 11. Matriz de trazabilidad — TaxTotal (cabecera)

| Dato SUNAT | Catálogo / Norma | XPath UBL 2.1 | Campo DTO | Regla de validación |
|---|---|---|---|---|
| Total IGV del comprobante | Cat. 05 | `/Invoice/cac:TaxTotal/cac:TaxSubtotal[.../cbc:ID='1000']/cbc:TaxAmount` | *(calculado servidor)* | Suma de IGV de líneas redondeada a 2 decimales. **O** |
| Total ISC del comprobante | Cat. 05 | `/Invoice/cac:TaxTotal/cac:TaxSubtotal[.../cbc:ID='2000']/cbc:TaxAmount` | *(calculado servidor)* | Suma de ISC de líneas. **C** |
| Total ICBPER del comprobante | Cat. 05 | `/Invoice/cac:TaxTotal/cac:TaxSubtotal[.../cbc:ID='7152']/cbc:TaxAmount` | *(calculado servidor)* | Suma de ICBPER de líneas. **C** |
| Total tributos del comprobante | Guía XML SUNAT | `/Invoice/cac:TaxTotal/cbc:TaxAmount` con `@currencyID` | *(calculado servidor)* | `= total_IGV + total_ISC + total_ICBPER`. **O** |

---

## 12. Matriz de trazabilidad — LegalMonetaryTotal

| Dato SUNAT | Catálogo / Norma | XPath UBL 2.1 | Campo DTO | Regla de validación |
|---|---|---|---|---|
| Total valor de venta (antes de impuestos) | Guía XML SUNAT | `/Invoice/cac:LegalMonetaryTotal/cbc:LineExtensionAmount` con `@currencyID` | *(suma server)* | `= Σ LineExtensionAmount de líneas`. **O** |
| Total precio de venta con impuestos | Guía XML SUNAT | `/Invoice/cac:LegalMonetaryTotal/cbc:TaxInclusiveAmount` con `@currencyID` | *(calculado server)* | `= LineExtensionAmount + TaxTotal`. **O** |
| Descuentos globales | Cat. 53 | `/Invoice/cac:LegalMonetaryTotal/cbc:AllowanceTotalAmount` | *(calculado server)* | Suma de descuentos globales. **C** |
| Cargos globales | Cat. 53 | `/Invoice/cac:LegalMonetaryTotal/cbc:ChargeTotalAmount` | *(calculado server)* | Suma de cargos globales. **C** |
| Importe total a pagar | Guía XML SUNAT | `/Invoice/cac:LegalMonetaryTotal/cbc:PayableAmount` con `@currencyID` | *(calculado server)* | `= TaxInclusiveAmount − AllowanceTotalAmount + ChargeTotalAmount`; ≥ 0. **O** |

---

## 13. Sección 14.1 — Registro de ventas (Libro Electrónico PLE / SIRE)

> Todo emisor electrónico debe mantener el **Registro de Ventas e Ingresos** conforme al PLE de SUNAT (formato 14.1) o SIRE/RVIE según corresponda.

| Campo PLE 14.1 | Fuente | Mapeo al dominio | Regla |
|---|---|---|---|
| Período tributario (`YYYYMM`) | PLE / guía SUNAT | `ComprobantePago.fechaEmision` → año-mes | **O** |
| CUO | PLE | `ComprobantePago.id` (UUID convertido a correlativo de libro) | **O** |
| Número correlativo del asiento | PLE | Correlativo de registro contable | Asignado al cierre del período. **O** |
| Fecha de emisión | PLE | `ComprobantePago.fechaEmision` | Formato `DD/MM/YYYY`. **O** |
| Fecha de vencimiento (crédito) | PLE | `FormaPago.cuotas[0].fechaVencimiento` | **C** |
| Tipo de comprobante | Cat. 01 / PLE | `ComprobantePago.tipoComprobante` | Código Cat. 01. **O** |
| Serie | RS 097-2012 | `ComprobantePago.serie` | 4 caracteres. **O** |
| Número | RS 097-2012 | `ComprobantePago.correlativo` | Entero 1–99 999 999. **O** |
| Tipo doc. identidad del cliente | Cat. 06 / PLE | `ClienteReceptor.tipoDocumento` | **O** |
| Número de identidad del cliente | Cat. 06 / PLE | `ClienteReceptor.numeroDocumento` | **O** |
| Apellidos y nombres / razón social | PLE | `ClienteReceptor.razonSocialNombres` | **O** |
| Base imponible op. gravadas | PLE | Suma `LineExtensionAmount` gravados | **O/C** |
| IGV y/o IPM | PLE | Total IGV de cabecera | **O/C** |
| Base imponible op. exoneradas | PLE | Suma `LineExtensionAmount` exonerados (`"20"`) | **C** |
| Base imponible op. inafectas | PLE | Suma `LineExtensionAmount` inafectos (`"30"`) | **C** |
| ISC | PLE | Total ISC de cabecera | **C** |
| ICBPER | PLE | Total ICBPER de cabecera | **C** |
| Importe total del comprobante | PLE | `LegalMonetaryTotal/cbc:PayableAmount` | **O** |
| Código de moneda | Cat. 02 / PLE | `ComprobantePago.moneda` | **O** |
| Tipo de cambio (si moneda ≠ PEN) | SUNAT / SBS | TC publicado por SBS fecha de operación | **C** |
| Estado del comprobante | PLE | `EstadoComprobante` → código PLE: `"1"` vigente, `"2"` anulado, `"8"` baja | **O** |

**Puerto de dominio requerido**:
```
domain/ports/
  LibroElectronicoPort.java          ← Interfaz de salida para PLE/SIRE

infrastructure/adapters/ple/
  PleTxtRegistroVentasAdapter.java   ← Genera TXT PLE 14.1 (formato SUNAT)
  SireRvieApiClientAdapter.java      ← Cliente REST SIRE/RVIE (futuro)
```

---

## 14. Sección 12.1 — Registro de inventario (Kardex electrónico PLE)

> Como sistema proveniente de almacén, ABC Software Corp debe mantener el **Registro de Inventario Permanente en Unidades Físicas** (12.1) conforme al Anexo 1 de RS N° 234-2006/SUNAT.

| Campo PLE 12.1 | Fuente | Mapeo al dominio | Regla |
|---|---|---|---|
| Período (`YYYYMM`) | RS 234-2006 / PLE | `MovimientoAlmacen.fechaMovimiento` → mes | **O** |
| CUO | PLE | `MovimientoAlmacen.id` | **O** |
| Código del establecimiento | RS 234-2006 | `MovimientoAlmacen.codigoEstablecimiento` | **O** |
| Tipo de existencia (Tabla 5 PLE) | Tabla 5 SUNAT | `Producto.codigoTipoExistencia` | Ej. `"01"` Mercaderías, `"02"` Productos terminados. **O** |
| Tipo de operación (Tabla 12 PLE) | Tabla 12 SUNAT | `MovimientoAlmacen.tipoOperacionPle` | Ej. `"16"` Ventas, `"20"` Compras. **O** |
| Tipo doc. soporte (Tabla 10 PLE) | Tabla 10 SUNAT | Derivado del `tipoComprobante` | Código Tabla 10 PLE. **O** |
| Número del documento de soporte | RS 234-2006 | `MovimientoAlmacen.comprobanteId` → `serie-correlativo` | **O** |
| Código de producto (interno) | RS 234-2006 | `Producto.codigo` | SKU interno único. **O** |
| Descripción del producto | RS 234-2006 | `Producto.descripcion` | **O** |
| Código de unidad de medida (Tabla 6 PLE) | Tabla 6 SUNAT | `Producto.unidadMedida` → equivalencia Tabla 6 | La Tabla 6 PLE puede diferir del Cat. 03 UBL; mantener tabla de equivalencias. **O** |
| Entrada (ingresos en unidades físicas) | RS 234-2006 | `MovimientoAlmacen.cantidad` cuando `tipo=INGRESO` | Decimal exacto ≥ 0. **O** |
| Salida (egresos en unidades físicas) | RS 234-2006 | `MovimientoAlmacen.cantidad` cuando `tipo=SALIDA` | Decimal exacto ≥ 0. **O** |
| Saldo final (unidades físicas) | RS 234-2006 | `Σ ingresos − Σ salidas` por producto y establecimiento | No se fuerza; recalculado. **O** |
| Estado del asiento | PLE | `"1"` vigente, `"8"` anulado | **O** |

### 14.1 Tabla de equivalencias Cat. 03 UBL ↔ Tabla 6 PLE

| Código Cat. 03 | Descripción UBL | Código Tabla 6 PLE | Descripción PLE |
|---|---|---|---|
| `NIU` | Unidad | `006` | Unidad |
| `KGM` | Kilogramo | `003` | Kilogramo |
| `BX` | Caja | `058` | Caja |
| `LTR` | Litro | `056` | Litro |
| `BG` | Bolsa | `057` | Bolsa |
| `GLL` | Galón | ⚠️ Por verificar | ⚠️ Por verificar |
| `SA` | Saco | ⚠️ Por verificar | ⚠️ Por verificar |

**Puerto de dominio requerido**:
```
domain/ports/
  KardexElectronicoPort.java           ← Interfaz de salida (inventario)

infrastructure/adapters/ple/
  PleTxtKardex121Adapter.java          ← Genera TXT PLE 12.1
```

---

## 15. Catálogos utilizados

| Catálogo / Tabla | Propósito | Uso principal |
|---|---|---|
| Cat. 01 | Tipo de comprobante | `InvoiceTypeCode` y PLE 14.1 |
| Cat. 02 | Moneda | `DocumentCurrencyCode` y `@currencyID` |
| Cat. 03 | Unidad de medida (UNECE) | `InvoicedQuantity/@unitCode` y base ISC/ICBPER |
| Cat. 05 | Códigos de tributos | `TaxScheme/ID`: IGV=`1000`, ISC=`2000`, ICBPER=`7152` |
| Cat. 06 | Tipo de documento de identidad | `PartyIdentification/ID/@schemeID` |
| Cat. 07 | Tipo de afectación al IGV | `TaxExemptionReasonCode` por línea |
| Cat. 08 | Sistema de cálculo del ISC | `TaxCategory/TierRange`: `01`, `02`, `03` |
| Cat. 09 | Tipo/motivo de nota de crédito | `CreditNote/DiscrepancyResponse/ResponseCode` |
| Cat. 10 | Tipo/motivo de nota de débito | `DebitNote/DiscrepancyResponse/ResponseCode` |
| Cat. 12 | Tipo de documento relacionado | `BillingReference/.../DocumentTypeCode` |
| Cat. 13 | Ubigeo | Dirección del emisor y receptor |
| Cat. 16 | Tipo de precio de venta unitario | `AlternativeConditionPrice/PriceTypeCode` |
| Cat. 51 | Tipo de operación | `ProfileID` (`0101` venta interna) |
| Cat. 53 | Motivo de descuento/cargo | `AllowanceChargeReasonCode` |
| Tabla 5 PLE | Tipo de existencia | Kardex 12.1 |
| Tabla 6 PLE | Unidad de medida PLE | Kardex 12.1 — equivalencia a Cat. 03 |
| Tabla 10 PLE | Tipo de documento soporte | Kardex 12.1 |
| Tabla 12 PLE | Tipo de operación de inventario | Kardex 12.1 |

---

## 16. Brechas pendientes

| Brecha | Impacto | Acción |
|---|---|---|
| ~~XSD locales no incorporados~~ | Resuelto 2026-11-06 | XSD de Invoice, CreditNote, DebitNote y módulos comunes en `docs/referencias/ubl/`; rutas verificadas en la sección 17 |
| Catálogos SUNAT versionados ausentes | Riesgo de usar valores derogados | Incorporar Anexo 8 actual en `docs/referencias/sunat/` |
| Elemento UBL exacto para PVP en ISC sistema 03 | No se puede generar XML correcto para sistema 03 | ⚠️ Por verificar contra guía XML SUNAT y XSD local |
| Diccionario del sistema legado | Columna origen no puede confirmarse | Obtener DDL o muestra anonimizada |
| Tabla 6 PLE para algunas unidades | Equivalencia incompleta | Contrastar con guía PLE SUNAT vigente |
| SIRE/RVIE vs PLE | No definido cuál aplica | ⚠️ Por verificar con normativa vigente |

---

## 17. Verificación contra los XSD oficiales de UBL 2.1

Fuente: `docs/referencias/ubl/` (F-01). Se inspeccionaron los tipos complejos `InvoiceType`, `CreditNoteType`, `DebitNoteType`, `TaxSubtotalType`, `TaxCategoryType`, `PaymentTermsType`, `PricingReferenceType`, `BillingReferenceType`, `ResponseType`, `MonetaryTotalType` y `InvoiceLineType`.

| Ruta de la matriz | Resultado contra el XSD |
|---|---|
| `Invoice/cbc:UBLVersionID`, `CustomizationID`, `ProfileID`, `ID`, `IssueDate`, `IssueTime`, `InvoiceTypeCode`, `DocumentCurrencyCode` | ✅ Existen en `InvoiceType`. Solo `ID` e `IssueDate` son obligatorios en el XSD; el resto es obligatorio por el perfil SUNAT, no por UBL. |
| `cac:PaymentTerms/cbc:ID`, `PaymentMeansID`, `Amount`, `PaymentDueDate` | ✅ Existen en `PaymentTermsType`. También existe `InstallmentDueDate`. ⚠️ Por verificar cuál usa la guía SUNAT para las cuotas. |
| `cac:PricingReference/cac:AlternativeConditionPrice/cbc:PriceAmount` y `PriceTypeCode` | ✅ `AlternativeConditionPrice` es de tipo `PriceType`, que contiene `PriceAmount` y `PriceTypeCode`. |
| `TaxSubtotal/cbc:BaseUnitMeasure` y `TaxSubtotal/cbc:PerUnitAmount` | ✅ Ambos existen **directamente en `TaxSubtotalType`**. |
| `TaxSubtotal/cac:TaxCategory/cbc:PerUnitAmount`, `BaseUnitMeasure`, `TierRange`, `Percent`, `TaxExemptionReasonCode` | ✅ Existen también en `TaxCategoryType`. Hay dos posiciones válidas para `PerUnitAmount`/`BaseUnitMeasure` (en `TaxSubtotal` y en `TaxCategory`); el XML debe usar la que fije la guía SUNAT. ⚠️ Por verificar con la guía de factura (F-05). |
| `TaxSubtotal/cbc:TierRange` | ✅ Existe también en `TaxSubtotalType`; la matriz lo ubica en `TaxCategory/TierRange`, que también es válido. |
| `cac:LegalMonetaryTotal/cbc:LineExtensionAmount`, `TaxInclusiveAmount`, `AllowanceTotalAmount`, `ChargeTotalAmount`, `PayableAmount` | ✅ Existen en `MonetaryTotalType`. |
| `CreditNote/cac:BillingReference/cac:InvoiceDocumentReference` y `CreditNote/cac:DiscrepancyResponse/cbc:ResponseCode`, `Description`, `ReferenceID` | ✅ Existen. |
| `DebitNote/cac:DiscrepancyResponse` | ✅ Existe. |

**Diferencias de las notas respecto de `Invoice`** (relevantes para el generador UBL):

| Aspecto | `Invoice` | `CreditNote` | `DebitNote` |
|---|---|---|---|
| Elemento de tipo de documento | `cbc:InvoiceTypeCode` | `cbc:CreditNoteTypeCode` (opcional) | *(no existe)* |
| Total monetario | `cac:LegalMonetaryTotal` | `cac:LegalMonetaryTotal` | **`cac:RequestedMonetaryTotal`** |
| Línea | `cac:InvoiceLine` / `cbc:InvoicedQuantity` | `cac:CreditNoteLine` / `cbc:CreditedQuantity` | `cac:DebitNoteLine` / `cbc:DebitedQuantity` |
| Fecha de vencimiento | `cbc:DueDate` | *(no existe)* | *(no existe)* |

> El generador no debe asumir que `RequestedMonetaryTotal` y `LegalMonetaryTotal` son intercambiables: son elementos distintos con el mismo tipo `MonetaryTotalType`.

### 17.1 Cobertura de notas de crédito y débito

| Dato | Nota de crédito | Nota de débito | Regla |
|---|---|---|---|
| Documento afectado | `CreditNote/cac:BillingReference/cac:InvoiceDocumentReference/cbc:ID` (+ `DocumentTypeCode`, Cat. 12) | `DebitNote/cac:BillingReference/cac:InvoiceDocumentReference/cbc:ID` | `serie-correlativo` de un comprobante en estado `ACEPTADO`; moneda heredada. |
| Motivo | `cac:DiscrepancyResponse/cbc:ResponseCode` (Cat. 09) | `cac:DiscrepancyResponse/cbc:ResponseCode` (Cat. 10) | Código vigente del catálogo respectivo. |
| Sustento | `DiscrepancyResponse/cbc:Description` | idem | Texto no vacío. |
| Referencia | `DiscrepancyResponse/cbc:ReferenceID` | idem | Identifica el comprobante afectado. |
| Líneas | `cac:CreditNoteLine` (`CreditedQuantity`, `LineExtensionAmount`, `PricingReference`, `TaxTotal`, `Item`, `Price`) | `cac:DebitNoteLine` (`DebitedQuantity`, ...) | Mismas reglas de afectación IGV/ISC/ICBPER que `InvoiceLine`. |
| Totales | `cac:LegalMonetaryTotal` | `cac:RequestedMonetaryTotal` | `PayableAmount` obligatorio. |
| Serie | `^[FB][A-Z0-9]{3}$` (conserva el prefijo del documento afectado) | idem | `422` si el prefijo no coincide con el afectado. |

### 17.2 Comunicación de baja y resumen diario

Estos documentos **no son** UBL 2.1 de OASIS: usan esquemas propios de SUNAT (`VoidedDocuments`, `SummaryDocuments`, versión 1.0) que no están en `docs/referencias/ubl/`. Estructura mínima esperada (⚠️ Por verificar contra F-05 y el XSD de SUNAT):

| Dato | Baja (`VoidedDocuments`) | Resumen (`SummaryDocuments`) |
|---|---|---|
| ID | `RA-AAAAMMDD-n` | `RC-AAAAMMDD-n` |
| Fecha de los documentos | `cbc:ReferenceDate` | `cbc:ReferenceDate` |
| Fecha de generación | `cbc:IssueDate` | `cbc:IssueDate` |
| Línea | `sac:VoidedDocumentsLine` (tipo, serie, correlativo, motivo) | `sac:SummaryDocumentsLine` (tipo, serie, correlativo, estado, importes, tributos) |
| Firma | `ext:UBLExtensions` | `ext:UBLExtensions` |

---

## 18. Verificación contra la Guía XML de Factura de SUNAT (UBL 2.1, v1.0)

Fuente: *Guía de elaboración de documentos electrónicos XML – Factura Electrónica UBL 2.1* (SUNAT, mayo 2017), copia en `docs/referencias/sunat/guia-xml-factura-ubl21-v1.0.pdf` (F-05). Esta versión es anterior al ICBPER (2019), por lo que **no cubre** ese tributo.

### 18.1 Tributos por tipo de operación (Catálogo 05)

Cada tipo de afectación se informa en su propio `cac:TaxSubtotal`, con `TaxScheme` distinto. Un ítem exonerado o inafecto **no** se declara como IGV con monto cero.

| Operación | `TaxCategory/cbc:ID` (UN/ECE 5305) | `TaxScheme/cbc:ID` | `TaxScheme/cbc:Name` | `TaxScheme/cbc:TaxTypeCode` | Afectación (Cat. 07) |
|---|---|---|---|---|---|
| Gravada con IGV | `S` | `1000` | `IGV` | `VAT` | `10`–`17` |
| Exonerada | `E` | `9997` | `EXONERADO` | `VAT` | `20`, `21` |
| Inafecta | `O` | `9998` | `INAFECTO` | `FRE` | `30`–`37` |
| Gratuita | `Z` | `9996` | `GRATUITO` | `FRE` | transferencias gratuitas |
| ISC | `S` | `2000` | `ISC` | `EXC` | con `cbc:TierRange` (Cat. 08) |
| Otros tributos | — | `9999` | `OTROS` | `OTH` | — |

- `TaxScheme/cbc:ID` lleva `@schemeID="UN/ECE 5153"` y `@schemeAgencyID="6"`.
- Con exonerado/inafecto, `TaxAmount` es `0.00` y `TaxableAmount` es la base de ese tipo; el total de cabecera (`TaxTotal`) incluye un `TaxSubtotal` por cada tipo presente.
- Los totales `total_operaciones_exoneradas` y `total_operaciones_inafectas` del DTO se mapean a los `TaxableAmount` de los subtotales `9997` y `9998`.
- ⚠️ Por verificar: el código y nombre del ICBPER (`7152`, `ICBPER`, `OTH`) provienen del Catálogo 05 vigente, no de esta guía.

### 18.2 Otros puntos verificados

| Dato | Resultado |
|---|---|
| `cbc:UBLVersionID` = `2.1` y `cbc:CustomizationID` = `2.0` | ✅ Confirmado. |
| `cbc:TierRange` | ✅ Ubicación `.../cac:TaxSubtotal/cac:TaxCategory/cbc:TierRange` confirmada; valores `01`, `02`, `03` (Apéndice IV, lit. A, B y C del TUO). |
| `cbc:PriceTypeCode` | ✅ `listName="SUNAT:Indicador de Tipo de Precio"`, `listAgencyName="PE:SUNAT"`, `listURI="...catalogo16"`; valores `01` y `02`. |
| ISC sistema `03` | La guía solo muestra `Percent` + `TierRange=03`; **no** indica dónde se consigna el PVP sugerido. Sigue ⚠️ Por verificar con una guía posterior. |
| Descuentos por línea | La guía admite `cac:AllowanceCharge` por línea (`ChargeIndicator`, `AllowanceChargeReasonCode` Cat. 53, `MultiplierFactorNumeric`, `Amount`, `BaseAmount`). **El DTO actual no los incluye**: queda fuera de alcance de esta fase y se registra como brecha. |

### 18.3 Leyendas (Catálogo 52)

| Leyenda | `cbc:Note/@languageLocaleID` | Regla |
|---|---|---|
| Monto expresado en letras | `1000` | Opcional según la guía. El servidor lo genera (p. ej. `DOS MIL CIENTO TREINTA CON 18/100 Soles`); el cliente no lo envía. |
| Transferencia gratuita / servicio prestado gratuitamente | `1002` | Solo si **todas** las líneas son gratuitas. |
| Código interno del software de facturación | `3000` | Aparece en los ejemplos de la guía. ⚠️ Por verificar si es obligatorio. |
