---
**Documento**: Descripción del caso y contexto del problema
**Carpeta**: specs/00-contexto/
**Estado**: BORRADOR
**Autor**: Equipo Grupo A
**Fecha**: 2026-10-22
**Fuentes**: RS 097-2012/SUNAT (F-03), RS 234-2006/SUNAT (F-04), UBL 2.1 (F-01)
---

# Caso: ABC Software Corp — Sistema de Facturación Electrónica

---

## 1. Descripción del problema

ABC Software Corp es una empresa peruana del sector **abarrotes** (distribución y venta de productos de consumo masivo). Actualmente opera con un sistema de facturación **físico o semi-automatizado** que emite comprobantes en papel o talonario, sin ninguna integración con los sistemas electrónicos de SUNAT.

SUNAT exige, mediante la RS 097-2012 y sus modificatorias, que los contribuyentes del régimen general emitan sus comprobantes de pago en formato electrónico (XML UBL 2.1), firmados digitalmente y enviados a través de un canal autorizado (SEE propio u OSE). El incumplimiento de esta obligación expone a la empresa a **sanciones tributarias** y al **impedimento de deducir gastos** ante el fisco.

### Situación actual (AS-IS)

| Área | Estado actual |
|------|--------------|
| Emisión de comprobantes | Física o en sistema sin integración SUNAT |
| Validación de RUC del receptor | No se realiza; se acepta cualquier número |
| Cálculo de tributos (IGV, ISC, ICBPER) | Manual o ausente en el sistema |
| Generación de XML UBL 2.1 | No existe |
| Firma digital (XMLDSig) | No existe |
| Comunicación con OSE o SUNAT | No existe |
| Recepción de CDR | No existe |
| Comunicación de baja | No existe |
| Resumen diario de boletas | No existe |

---

## 2. Brecha frente a los requisitos de SUNAT

Las siguientes brechas impiden que ABC Software Corp cumpla con la normativa vigente:

### 2.1 No valida el RUC del receptor

El sistema actual acepta cualquier número como RUC sin verificarlo ante los registros de SUNAT. La RS 097-2012 exige que el RUC del adquirente sea válido (11 dígitos, con dígito verificador correcto y en estado activo) para que la factura sea aceptada por SUNAT.

> ⚠️ Por verificar: si la validación del RUC debe realizarse en línea (consultando el padrón de SUNAT) o si es suficiente la validación del algoritmo del dígito verificador [F-03, F-05].

### 2.2 No calcula IGV, ISC ni ICBPER

El sistema no aplica los impuestos correctamente:

- **IGV** (Impuesto General a las Ventas): 18% sobre la base imponible. ⚠️ Por verificar: tasa vigente y excepciones [F-03, Catálogo de afectación de IGV, F-02].
- **ISC** (Impuesto Selectivo al Consumo): aplica a bienes específicos (bebidas, tabacos, combustibles). ⚠️ Por verificar: qué productos de abarrotes están afectos [F-03, F-02].
- **ICBPER** (Impuesto al Consumo de Bolsas de Plástico): S/ 0.20 por bolsa de plástico entregada al momento de la venta. ⚠️ Por verificar: monto vigente y obligaciones de reporte [F-03].

La estructura de tributos en el XML UBL 2.1 requiere declarar explícitamente cada impuesto con su código de catálogo SUNAT, base imponible, porcentaje y monto [F-01, F-02].

### 2.3 No genera XML firmado en formato UBL 2.1

SUNAT exige que los comprobantes electrónicos sean archivos XML que:
1. Cumplan el esquema `UBL-Invoice-2.1.xsd` (facturas) o `UBL-CreditNote-2.1.xsd` / `UBL-DebitNote-2.1.xsd` (notas).
2. Incluyan las extensiones propietarias de SUNAT (namespace `ext:UBLExtensions`) para la firma digital.
3. Estén firmados con **XMLDSig** usando un certificado X.509 emitido por una entidad certificadora autorizada.

El sistema actual no tiene capacidad para generar ni firmar XML.

### 2.4 No se comunica con OSE ni con SUNAT

Una vez generado el XML firmado, el sistema debe enviarlo a través de:
- **OSE (Operador de Servicios Electrónicos)**: tercero autorizado que valida y envía a SUNAT.
- **SEE del Contribuyente**: web service SOAP propio, conectando directamente al endpoint de SUNAT.

La comunicación implica:
- Envío del XML (o ZIP con el XML) vía SOAP.
- Recepción del **CDR** (Constancia de Recepción) con el estado ACEPTADO, RECHAZADO u OBSERVADO.
- Reenvío en caso de rechazo corregido.
- Envío de **comunicaciones de baja** para anular comprobantes ya emitidos.
- Envío de **resúmenes diarios** para consolidar las boletas emitidas el día anterior.

El sistema actual no tiene ninguno de estos mecanismos.

---

## 3. Alcance de esta fase Spec-First

### 3.1 Qué entra en esta fase

| Entregable | Descripción |
|-----------|-------------|
| Especificación de casos de uso | Todos los flujos funcionales del sistema (emisión, consulta, baja, etc.) |
| Modelo de dominio | Entidades, reglas de negocio, catálogos SUNAT |
| Arquitectura del sistema | Capas MVC / Hexagonal, vistas C4 |
| Contratos de API | OpenAPI 3.1 para la API interna; referencia al WSDL de SUNAT/OSE |
| Matriz de trazabilidad | Requisitos ↔ normas ↔ artefactos ↔ casos de uso |
| Plan de migración | Estrategia de transición desde el sistema actual |
| Topología de despliegue | Nodos, zonas de red, requisitos multiplataforma |
| ADR | Decisiones de arquitectura documentadas |

### 3.2 Qué NO entra en esta fase

| Fuera de alcance | Motivo |
|-----------------|--------|
| Código fuente de producción | Principio Spec-First: se escribe solo tras aprobación de specs |
| Pruebas automatizadas | Dependen del código, que aún no existe |
| Integración real con OSE/SUNAT | Requiere certificado digital y entorno de homologación |
| Declaraciones juradas y libros electrónicos | Fuera del alcance del sistema de facturación |
| Módulo contable / ERP | Integración futura; no está en el alcance de esta fase |
| Bolsas de plástico (ICBPER) como producto | ⚠️ Por verificar: si se incluye en esta fase o se posterga |

---

## 4. Actores del sistema

| Actor | Tipo | Descripción |
|-------|------|-------------|
| **Emisor** | Interno (empresa) | ABC Software Corp en su rol de contribuyente. Es quien firma y emite los comprobantes. |
| **Cajero / Vendedor** | Interno (usuario) | Persona que opera el punto de venta, registra las ventas y genera los comprobantes. |
| **Almacenero** | Interno (usuario) | Gestiona el catálogo de productos y el stock. Mantiene la información de artículos necesaria para la emisión. |
| **Contador / Administrador** | Interno (usuario) | Supervisa la emisión, gestiona series/correlativos, consulta el estado de comprobantes, genera resúmenes y comunicaciones de baja. |
| **Cliente / Adquirente** | Externo | Persona natural o jurídica que recibe el comprobante. Puede ser identificado con RUC (factura) o DNI/CE (boleta). |
| **OSE** | Externo (sistema) | Operador de Servicios Electrónicos autorizado por SUNAT. Recibe el XML, lo valida y devuelve el CDR. ⚠️ Por verificar: decisión pendiente en ADR-001 [F-03]. |
| **SUNAT** | Externo (sistema/regulador) | Superintendencia Nacional de Aduanas y de Administración Tributaria. Receptora final de los comprobantes (vía OSE o SEE directo). Fuente de las normas RS 097-2012 y RS 234-2006. |
