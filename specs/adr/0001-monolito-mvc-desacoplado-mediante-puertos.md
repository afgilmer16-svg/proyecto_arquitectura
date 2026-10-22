# ADR-0001 — Monolito MVC Desacoplado mediante Puertos y Adaptadores

**Estado**: APROBADO  
**Fecha**: 2026-10-22  
**Autores**: Equipo Grupo A — UNAS-FIIS  
**Fuentes**: RS 097-2012/SUNAT (F-03), RS 234-2006/SUNAT (F-04), OASIS UBL 2.1 (F-01)

*(Ver versión oficial en [`docs/adr/ADR-001-monolito-mvc-desacoplado-mediante-puertos.md`](../../docs/adr/ADR-001-monolito-mvc-desacoplado-mediante-puertos.md))*

---

## Contexto

ABC Software Corp debe implementar la facturación electrónica bajo normativa SUNAT con un equipo reducido y plazo ajustado, manteniendo la capacidad de escalar y extraer microservicios a futuro.

## Opciones evaluadas

1. Monolito tradicional fuertemente acoplado (ActiveRecord / lógica en controladores).
2. Microservicios distribuidos desde el día 1 (alta complejidad de infraestructura y operaciones).
3. **Monolito MVC Desacoplado mediante Puertos y Adaptadores (Elegida)**.

## Decisión

Se elige implementar un **Monolito Modular con Puertos y Adaptadores (Arquitectura Hexagonal)**. El dominio no tiene dependencias externas; los casos de uso exponen puertos de entrada y consumen puertos de salida implementados por adaptadores en la capa de infraestructura.

## Consecuencias

- **Positivas**: Rápida entrega inicial, 100% de testabilidad unitaria del dominio y casos de uso, migración limpia a microservicios con el patrón Strangler Fig.
- **Compromisos**: Requiere disciplina estricta para respetar las fronteras de los paquetes y desacoplar DTOs de las entidades de dominio.
