# Enunciado del Caso — Examen de Arquitectura de Software

**Curso**: Arquitectura de Software  
**Universidad**: UNAS-FIIS  
**Grupo**: A  
**Empresa**: ABC Software Corp  
**Fecha**: 2026-10-22

---

## Descripción del caso

**ABC Software Corp** es una empresa peruana del sector **abarrotes** (distribución y venta de productos de consumo masivo). Debe implementar un sistema de **facturación electrónica** que cumpla con las normas vigentes de SUNAT.

## Normas aplicables

| Norma | Descripción |
|-------|-------------|
| RS N° 097-2012/SUNAT | Sistema de Emisión Electrónica (SEE); requisitos técnicos y canales autorizados |
| RS N° 234-2006/SUNAT | Requisitos generales de los comprobantes de pago |
| OASIS UBL 2.1 | Estándar XML para documentos de negocio (`UBL-Invoice-2.1.xsd`) |

## Requerimientos generales

- Emisión de facturas electrónicas en formato XML UBL 2.1.
- Firma digital con certificado X.509 (XML-DSig).
- Envío a SUNAT vía SEE del Contribuyente u OSE.
- Recepción y almacenamiento del CDR (Constancia de Recepción).
- Soporte de modo contingencia ante indisponibilidad de SUNAT.
- Conservación de comprobantes por el período legal establecido.
- Interfaz multiplataforma (web y/o escritorio) para el personal operativo.

## Alcance del proyecto académico

El proyecto es **Spec-First**: produce especificaciones, contratos y decisiones de arquitectura — no código de producción. Ver reglas en [`.agent/rules/spec-first.md`](../.agent/rules/spec-first.md).
