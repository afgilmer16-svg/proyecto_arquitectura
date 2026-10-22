# spec.md — 0003 Arquitectura MVC / Hexagonal

**Feature**: Arquitectura MVC y Hexagonal  
**ID**: 0003  
**Estado**: APROBADO  
**Autor**: Equipo Grupo A — UNAS-FIIS  
**Fecha**: 2026-10-22  
**Fuentes**: RS 097-2012/SUNAT (F-03), RS 234-2006/SUNAT (F-04), OASIS UBL 2.1 (F-01)

---

## 1. Descripción

Esta especificación define la arquitectura del sistema de facturación electrónica de **ABC Software Corp**, iniciando con un **Monolito MVC Desacoplado mediante Puertos** y trazando la ruta de transición hacia una **Arquitectura Hexagonal (Ports & Adapters)** y la eventual extracción de microservicios con el patrón Strangler Fig.

El documento maestro completo de arquitectura se encuentra en:
👉 [`specs/02-arquitectura/mvc-hexagonal.md`](../../02-arquitectura/mvc-hexagonal.md)

---

## 2. Decisiones Arquitectónicas Principales

- **ADR-001**: Monolito MVC Desacoplado mediante Puertos y Adaptadores ([`docs/adr/ADR-001-monolito-mvc-desacoplado-mediante-puertos.md`](../../../docs/adr/ADR-001-monolito-mvc-desacoplado-mediante-puertos.md)).
- **ADR-002**: Integración mediante Operador de Servicios Electrónicos (OSE) con contingencia de envío directo a SUNAT ([`docs/adr/ADR-002-ose-en-lugar-de-envio-directo-a-sunat.md`](../../../docs/adr/ADR-002-ose-en-lugar-de-envio-directo-a-sunat.md)).

---

## 3. Resumen de Puertos y Adaptadores

- **Puertos de Entrada (Driver)**: `EmitirComprobanteUseCase`, `AnularComprobanteUseCase`, `ConsultarEstadoUseCase`, `MigrarLegadoUseCase`.
- **Puertos de Salida (Driven)**: `ComprobanteRepository`, `GeneradorXmlUblPort`, `FirmaDigitalPort`, `EnvioOsePort` (con contingencia SUNAT), `ValidadorRucPort`, `CatalogoSunatPort`, `LegadoReaderPort`.

---

## 4. Criterios de Aceptación de Arquitectura

- [x] Diagrama Mermaid del monolito MVC actual con interfaces y repositorios desacoplados.
- [x] Matriz de puertos y adaptadores con adaptadores iniciales y futuros.
- [x] Diagrama Mermaid de la arquitectura hexagonal futura con la regla de dependencias hacia el centro.
- [x] Plan de migración a microservicios justificando la extracción inicial del módulo de envío OSE / SUNAT.
- [x] Estructura de paquetes demostrando la inmutabilidad de la capa de dominio frente a la evolución tecnológica.
