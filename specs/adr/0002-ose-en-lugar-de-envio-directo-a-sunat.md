# ADR-0002 — Integración mediante OSE en lugar de Envío Directo a SUNAT

**Estado**: APROBADO  
**Fecha**: 2026-10-22  
**Autores**: Equipo Grupo A — UNAS-FIIS  
**Fuentes**: RS 097-2012/SUNAT (F-03), RS 234-2006/SUNAT (F-04), Manual OSE/SOL SUNAT (F-05)

*(Ver versión oficial en [`docs/adr/ADR-002-ose-en-lugar-de-envio-directo-a-sunat.md`](../../docs/adr/ADR-002-ose-en-lugar-de-envio-directo-a-sunat.md))*

---

## Contexto

ABC Software Corp necesita asegurar tiempos de respuesta rápidos (< 500 ms) en sus puntos de venta de abarrotes y protegerse de la inestabilidad recurrente de los servidores de SUNAT.

## Opciones evaluadas

1. Envío directo exclusivo a SUNAT vía SOAP (servicio gratuito pero con alta latencia y caídas).
2. Integración exclusiva con OSE (alta disponibilidad pero dependencia de un único proveedor).
3. **Integración primaria con OSE respaldada por un adaptador secundario de contingencia directa a SUNAT (Elegida)**.

## Decisión

Se elige **OSE como canal principal de emisión y validación electrónica**, canalizado a través del puerto abstracto `EnvioOsePort`, respaldado por un adaptador secundario de contingencia directa a SUNAT.

## Consecuencias

- **Positivas**: Tiempo de respuesta en cajas inferior a 500 ms, validez legal inmediata del CDR, absorción del mantenimiento normativo por parte del OSE, y alta resiliencia ante contingencias.
- **Compromisos**: Costo por volumen de comprobantes emitidos y necesidad de mantener operativo el adaptador de respaldo.
