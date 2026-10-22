# ADR-002 — Integración mediante Operador de Servicios Electrónicos (OSE) en lugar de Envío Directo a SUNAT

**Estado**: APROBADO  
**Fecha**: 2026-10-22  
**Autores**: Equipo Grupo A — UNAS-FIIS  
**Fuentes**: RS 097-2012/SUNAT (F-03), RS 234-2006/SUNAT (F-04), Manual del Programador OSE/SOL SUNAT (F-05)

---

## Contexto

Conforme a la **Resolución de Superintendencia N° 097-2012/SUNAT** y sus modificatorias, los emisores electrónicos en el Perú disponen de dos canales principales para la validación de sus comprobantes de pago electrónicos (facturas, boletas, notas de crédito/débito, comunicaciones de baja y resúmenes diarios):

1. **SEE - Del Contribuyente (Envío Directo a SUNAT)**: El emisor genera el XML firmado y lo envía directamente mediante web service SOAP al endpoint central de SUNAT (`https://e-factura.sunat.gob.pe/ol-ti-itcpfegem/billService`). SUNAT procesa el documento y devuelve la Constancia de Recepción (CDR).
2. **SEE - OSE (Operador de Servicios Electrónicos)**: El emisor envía el XML firmado a un OSE autorizado por SUNAT (e.g., Nubefact, Efact, Digiflow, Factura Perú). El OSE realiza la comprobación informática de las condiciones de emisión en tiempo real, emite y firma el CDR con validez legal inmediata, y posteriormente reporta el comprobante y el CDR a SUNAT.

### Restricciones y Realidad Operativa de ABC Software Corp:
- **Sector Abarrotes**: La empresa opera con alto volumen de transacciones diarias en mostrador. La lentitud en la emisión paraliza las cajas de cobro.
- **Disponibilidad de SUNAT**: Históricamente, los servidores SOAP de SUNAT sufren degradación de servicio y saturación en horas pico y cierres mensuales (tiempos de respuesta superiores a 20-30 segundos o caídas temporales).
- **Mantenimiento Normativo**: SUNAT actualiza periódicamente catálogos, reglas de validación (validaciones sintácticas y semánticas complejas con códigos de error 2000 a 4000) y certificados SSL de conexión. Un equipo pequeño de desarrollo no puede destinar tiempo continuo a la adaptación urgente a cambios de infraestructura estatal.

---

## Opciones evaluadas

| Criterio | Opción A: Envío Directo Exclusivo a SUNAT (SEE Propio) | Opción B: Integración Exclusiva con OSE | Opción C: Integración Primaria con OSE + Adaptador Secundario de Contingencia Directa a SUNAT (Elegida) |
|---|---|---|---|
| **Disponibilidad y Latencia (SLA)** | Baja; dependiente del uptime de SUNAT (latencias de 1s a 30s+ en cierres). | Alta; OSEs comerciales ofrecen SLAs contractuales de 99.8% y latencias < 500 ms. | **Máxima**; OSE como canal primario (< 500 ms) y conmutación automática o manual a SUNAT ante contingencia. |
| **Costo Operativo** | Sin costo directo por transacción (servicio estatal gratuito); alto costo oculto de mantenimiento. | Costo mensual o por volumen de comprobantes cobrado por el proveedor OSE. | Costo por comprobante asumido en OSE, amortizado por la continuidad operativa del negocio de abarrotes. |
| **Carga de Mantenimiento Fiscal** | Muy alta; el equipo debe actualizar validadores sintácticos con cada resolución de SUNAT. | Mínima; el OSE absorbe y homologa las nuevas versiones de catálogos y esquemas. | **Baja**; el OSE absorbe la carga diaria y el adaptador directo sirve de respaldo de emergencia. |
| **Riesgo de Bloqueo en Cajas** | Alto si el servicio central de SUNAT está caído o responde con timeout. | Bajo; el OSE responde con CDR inmediato. | **Nulo**; redundancia de canales de comunicación. |
| **Vendor Lock-in** | Nulo con terceros (pero atado a la infraestructura de SUNAT). | Dependencia del proveedor OSE elegido. | **Mitigado**; el puerto `EnvioOsePort` abstrae el proveedor, permitiendo alternar entre OSEs o conmutar a SUNAT. |

---

## Decisión

**Decisión**: Se elige la **Opción C: Integración Primaria con OSE mediante el Puerto `EnvioOsePort`, respaldada por un Adaptador Secundario de Contingencia Directa a SUNAT**.

### Justificación de la Elección:
1. **Garantía de Continuidad en el Punto de Venta**: Para un negocio de abarrotes, el tiempo de respuesta al cliente es crítico. Los OSEs autorizados procesan y generan el CDR en menos de 500 ms, garantizando fluidez en la atención en ventanilla.
2. **Validez Legal Inmediata**: Conforme a la RS N° 097-2012, el CDR emitido y firmado digitalmente por el OSE tiene plena validez jurídica tributaria desde el instante de su emisión, liberando a la empresa de la incertidumbre sobre si SUNAT ha procesado el comprobante.
3. **Desacoplamiento Mediante Interfaces**: En el diseño de arquitectura, la aplicación interactúa con la interfaz abstracta `EnvioOsePort`. Se provee una implementación principal `SoapOseClientAdapter` y una implementación de respaldo `SoapSunatDirectoFallbackAdapter`. Mediante configuración (`application.properties` o variables de entorno), el sistema puede cambiar de proveedor OSE o activar el envío directo a SUNAT sin modificar la lógica de negocio.

---

## Consecuencias

### Positivas:
- **Alta Disponibilidad Operativa**: Las colas de pago en las tiendas de abarrotes no se detienen por contingencias en los servidores de SUNAT.
- **Reducción de Deuda Técnica y Esfuerzo de Mantenimiento**: El proveedor OSE se encarga de implementar los cambios en esquemas XSD y validaciones de catálogos que SUNAT promulga periódicamente.
- **Respaldo de Emergencia**: Si el proveedor OSE experimenta una interrupción imprevista, el adaptador de contingencia permite despachar comprobantes críticos directamente al servidor de SUNAT.
- **CDR Inmediato**: Acceso garantizado al CDR para entrega inmediata del PDF/XML al adquirente (cliente).

### Negativas / Compromisos:
- **Costo Financiero**: Se debe contratar un plan comercial con un OSE autorizado (costo marginal por cada millar de comprobantes emitidos).
- **Mantenimiento Dual de Adaptadores**: El equipo técnico debe mantener actualizado el adaptador de contingencia directa a SUNAT y probarlo periódicamente en ambiente beta/homologación para asegurar que funcione cuando se requiera.

### Riesgos y Mitigaciones:
- *Riesgo*: Caída simultánea del OSE contratado.  
  *Mitigación*: Implementación de patrón *Circuit Breaker* (Resilience4j / Polly) que active automáticamente el canal de contingencia directa a SUNAT o almacene los comprobantes en cola local offline con reintento programado dentro del plazo legal de 3 días calendario que otorga la norma.

---

## Estado de revisión

- [x] Borrador inicial completado
- [x] Revisado contra normativas RS 097-2012 y análisis de costos/SLA
- [x] Aprobado por el equipo de arquitectura Grupo A
