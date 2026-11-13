# spec.md — 0003 Arquitectura MVC / Hexagonal

**Feature**: Arquitectura MVC y Hexagonal  
**ID**: 0003  
**Estado**: EN REVISIÓN  
**Autor**: Gilmer Ayala Falcón (Grupo A, UNAS-FIIS)  
**Fecha**: 2026-10-22  
**Fuentes**: RS 097-2012/SUNAT (F-03), RS 234-2006/SUNAT (F-04), OASIS UBL 2.1 (F-01)

---

## 1. Descripción

Esta especificación define la arquitectura del sistema de facturación electrónica de **ABC Software Corp**, iniciando con un **Monolito MVC Desacoplado mediante Puertos** y trazando la ruta de transición hacia una **Arquitectura Hexagonal (Ports & Adapters)** y la eventual extracción de microservicios con el patrón Strangler Fig.

El documento maestro completo de arquitectura se encuentra en:
👉 [`specs/features/0003-arquitectura-mvc-hexagonal/design.md`](design.md)

---

## 2. Decisiones Arquitectónicas Principales

- **ADR-001**: Monolito MVC Desacoplado mediante Puertos y Adaptadores ([`specs/adr/0001-monolito-mvc-desacoplado-mediante-puertos.md`](../../adr/0001-monolito-mvc-desacoplado-mediante-puertos.md)).
- **ADR-003**: Kotlin Multiplatform + Compose Multiplatform para los adaptadores de UI Windows (MSI) y Android (APK) ([`specs/adr/0003-kotlin-multiplatform-compose.md`](../../adr/0003-kotlin-multiplatform-compose.md)).
- **ADR-002**: Integración mediante Operador de Servicios Electrónicos (OSE) con contingencia de envío directo a SUNAT ([`specs/adr/0002-ose-en-lugar-de-envio-directo-a-sunat.md`](../../adr/0002-ose-en-lugar-de-envio-directo-a-sunat.md)).

---

## 3. Resumen de Puertos y Adaptadores

- **Puertos de Entrada (Driver)**: `EmitirComprobanteUseCase`, `AnularComprobanteUseCase`, `ConsultarEstadoUseCase`, `GestionarMaestrosUseCase`, `EnviarResumenDiarioUseCase`, `ValidarContribuyenteUseCase`, `GenerarLibrosElectronicosUseCase`, `MigrarLegadoUseCase`.
- **Puertos de Salida (Driven)**: `ComprobanteRepository`, `GeneradorXmlUblPort`, `FirmaDigitalPort`, `EnvioOsePort` (con contingencia SUNAT), `ValidadorRucPort`, `CatalogoSunatPort`, `LibroElectronicoPort`, `KardexElectronicoPort`, `LegadoReaderPort`.

---

## 4. Criterios de Aceptación de Arquitectura

- [x] Diagrama Mermaid del monolito MVC actual con interfaces y repositorios desacoplados.
- [x] Matriz de puertos y adaptadores con adaptadores iniciales y futuros.
- [x] Diagrama Mermaid de la arquitectura hexagonal futura con la regla de dependencias hacia el centro.
- [x] Plan de migración a microservicios justificando la extracción inicial del módulo de envío OSE / SUNAT.
- [x] Estructura de paquetes demostrando la inmutabilidad de la capa de dominio frente a la evolución tecnológica.
