# Proyecto: Sistema de Facturación Electrónica — ABC Software Corp

**Curso**: Arquitectura de Software  
**Universidad**: UNAS-FIIS  
**Grupo**: A  
**Metodología**: Spec-First  
**Fecha de inicio**: 2026-10-22

---

## Enunciado del Caso

**ABC Software Corp** es una empresa peruana del sector **abarrotes** que debe implementar un sistema de **facturación electrónica** conforme a:

- **RS N° 097-2012/SUNAT** — Sistema de Emisión Electrónica (SEE), requisitos técnicos y canales autorizados.
- **RS N° 234-2006/SUNAT** — Requisitos generales de los comprobantes de pago.
- **OASIS UBL 2.1** — Estándar XML para documentos de negocio (`UBL-Invoice-2.1.xsd`).

Ver el enunciado completo en [`docs/enunciado.md`](docs/enunciado.md).

---

## Metodología: Spec-First

**No se escribe código de producción hasta que la especificación esté completa y aprobada.**  
Reglas completas: [`.agent/rules/spec-first.md`](.agent/rules/spec-first.md)

---

## Tabla de Entregables

| ID | Entregable | Carpeta / Archivo |
|----|-----------|-------------------|
| — | Enunciado del caso | [`docs/enunciado.md`](docs/enunciado.md) |
| — | Análisis del problema | [`docs/analisis-problema.md`](docs/analisis-problema.md) |
| — | Referencias UBL 2.1 | [`docs/referencias/ubl/`](docs/referencias/ubl/) |
| — | Referencias SUNAT (normas, catálogos, manuales) | [`docs/referencias/sunat/`](docs/referencias/sunat/) |
| — | Plantilla ADR | [`specs/adr/0000-plantilla-adr.md`](specs/adr/0000-plantilla-adr.md) |
| — | Contrato OpenAPI 3.1 | [`specs/contracts/facturacion-api.yaml`](specs/contracts/facturacion-api.yaml) |
| — | Esquemas XSD anotados | [`specs/contracts/schemas/`](specs/contracts/schemas/) |
| 0001 | Casos de uso | [`specs/features/0001-casos-uso/`](specs/features/0001-casos-uso/) |
| 0002 | Modelo de dominio | [`specs/features/0002-modelo-dominio/`](specs/features/0002-modelo-dominio/) |
| 0003 | Arquitectura MVC / Hexagonal | [`specs/features/0003-arquitectura-mvc-hexagonal/`](specs/features/0003-arquitectura-mvc-hexagonal/) |
| 0004 | Contratos de API | [`specs/features/0004-contratos-api/`](specs/features/0004-contratos-api/) |
| 0005 | Trazabilidad SUNAT / UBL | [`specs/features/0005-trazabilidad-sunat-ubl/`](specs/features/0005-trazabilidad-sunat-ubl/) |
| 0006 | Migración de legado | [`specs/features/0006-migracion-legado/`](specs/features/0006-migracion-legado/) |
| 0007 | Distribución multiplataforma | [`specs/features/0007-distribucion-multiplataforma/`](specs/features/0007-distribucion-multiplataforma/) |
| — | Plantillas de especificación | [`specs/templates/`](specs/templates/) |

Cada feature contiene:
- **`spec.md`** — Especificación de la funcionalidad
- **`tasks.md`** — Lista de tareas de especificación y verificación

---

## Estructura de Carpetas

```
proyecto_arquitectura/
│
├── .agent/
│   └── rules/
│       └── spec-first.md                      ← Reglas de la metodología (v2.0)
│
├── docs/
│   ├── enunciado.md                            ← Enunciado del caso
│   ├── analisis-problema.md                    ← Análisis del problema
│   └── referencias/
│       ├── ubl/                                ← Esquemas OASIS UBL 2.1 (solo lectura)
│       └── sunat/                              ← Normas, catálogos y manuales SUNAT
│
├── specs/
│   ├── adr/
│   │   └── 0000-plantilla-adr.md              ← Plantilla para ADR
│   ├── contracts/
│   │   ├── facturacion-api.yaml               ← Contrato OpenAPI 3.1 (vacío)
│   │   └── schemas/                           ← XSD UBL 2.1 anotados
│   ├── features/
│   │   ├── 0001-casos-uso/                    ← spec.md + tasks.md
│   │   ├── 0002-modelo-dominio/               ← spec.md + tasks.md
│   │   ├── 0003-arquitectura-mvc-hexagonal/   ← spec.md + tasks.md
│   │   ├── 0004-contratos-api/                ← spec.md + tasks.md
│   │   ├── 0005-trazabilidad-sunat-ubl/       ← spec.md + tasks.md
│   │   ├── 0006-migracion-legado/             ← spec.md + tasks.md
│   │   └── 0007-distribucion-multiplataforma/ ← spec.md + tasks.md
│   └── templates/
│       ├── spec-template.md                   ← Plantilla de especificación
│       ├── design-template.md                 ← Plantilla de diseño técnico
│       └── tasks-template.md                  ← Plantilla de tareas
│
├── src/                                        ← VACÍO — sin código hasta specs aprobadas
├── tests/                                      ← VACÍO — sin pruebas hasta specs aprobadas
├── requirements.txt                            ← Dependencias — por definir
├── .gitignore                                  ← Excluye .pfx, .p12, .env, node_modules, *.msi, *.apk
└── README.md                                   ← Este archivo
```

---

## Convenciones de estado

| Etiqueta | Significado |
|----------|-------------|
| `BORRADOR` | Primer borrador, no revisado |
| `EN REVISIÓN` | Bajo revisión por otro miembro del equipo |
| `APROBADO` | Validado contra fuentes normativas; listo para implementación |
| `⚠️ Por verificar` | Dato o restricción pendiente de confirmación contra la fuente |