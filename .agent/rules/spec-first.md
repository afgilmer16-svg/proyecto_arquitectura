# Reglas Spec-First — Proyecto Facturación Electrónica SUNAT
## UNAS-FIIS · Arquitectura de Software · Grupo A · ABC Software Corp

---

## 1. Principio fundamental

> **No se escribe lógica de negocio hasta que la especificación esté completa y verificada.**

Todo artefacto en este repositorio es una especificación, una referencia normativa o una decisión de arquitectura — **nunca código ejecutable de producción**.  
Las carpetas `src/` y `tests/` existen como marcadores de posición; permanecen vacías hasta que el equipo de implementación reciba las specs aprobadas.

---

## 2. Idioma

- **Todo el contenido debe redactarse en español**, salvo:
  - Identificadores técnicos definidos por estándares externos (e.g. `cbc:ID`, `UBLVersionID`, nombres de esquemas XSD).
  - Fragmentos de código de ejemplo (XML, JSON, YAML) tomados literalmente de las fuentes normativas.
  - Siglas internacionales reconocidas (UBL, SUNAT, RUC, XML, XSD, REST, JSON, OpenAPI, etc.).

---

## 3. Fuentes normativas autorizadas

Solo las siguientes fuentes son consideradas **verdad de referencia**. Cualquier afirmación que no pueda trazarse a ellas debe marcarse como **"Por verificar"**.

| Código | Fuente | Ubicación en este repositorio |
|--------|--------|-------------------------------|
| F-01 | `UBL-Invoice-2.1.xsd` (OASIS UBL 2.1) | `docs/referencias/ubl/` |
| F-02 | Catálogos SUNAT — Anexo 8 (RS 097-2012) | `docs/referencias/sunat/` |
| F-03 | Resolución de Superintendencia N° 097-2012/SUNAT | `docs/referencias/sunat/` |
| F-04 | Resolución de Superintendencia N° 234-2006/SUNAT | `docs/referencias/sunat/` |
| F-05 | Manual del Programador — OSE/SOL SUNAT (versión vigente) | `docs/referencias/sunat/` |

---

## 4. Marcado de incertidumbre

Cuando un dato, restricción o comportamiento **no esté confirmado** por alguna de las fuentes anteriores, se marcará con la etiqueta:

```
⚠️ Por verificar: <descripción breve de la duda> [referencia pendiente]
```

---

## 5. Estructura de carpetas y responsabilidades

```
docs/
  enunciado.md              → Enunciado del caso ABC Software Corp
  analisis-problema.md      → Contexto, stakeholders, restricciones, riesgos
  referencias/
    ubl/                    → Esquemas OASIS UBL 2.1 (solo lectura)
    sunat/                  → RS 097-2012, RS 234-2006, catálogos, manuales

specs/
  adr/                      → Architecture Decision Records
  contracts/
    facturacion-api.yaml    → Contrato OpenAPI 3.1 (solo interfaz, sin lógica)
    schemas/                → XSD UBL 2.1 anotados con restricciones SUNAT
  features/
    0001-casos-uso/         → spec.md + tasks.md
    0002-modelo-dominio/    → spec.md + tasks.md
    0003-arquitectura-mvc-hexagonal/  → spec.md + tasks.md
    0004-contratos-api/     → spec.md + tasks.md
    0005-trazabilidad-sunat-ubl/      → spec.md + tasks.md
    0006-migracion-legado/  → spec.md + tasks.md
    0007-distribucion-multiplataforma/ → spec.md + tasks.md
  templates/
    spec-template.md        → Plantilla de especificación de feature
    design-template.md      → Plantilla de diseño técnico
    tasks-template.md       → Plantilla de tareas

src/                        → VACÍO — sin código hasta specs aprobadas
tests/                      → VACÍO — sin pruebas hasta specs aprobadas
requirements.txt            → Dependencias — por definir tras aprobación de specs
```

---

## 6. Flujo de trabajo Spec-First

```
1. INVESTIGAR  →  Leer fuentes normativas (F-01 … F-05) en docs/referencias/
2. ESPECIFICAR →  Redactar spec.md en la carpeta specs/features/NNNN-*/
3. MARCAR      →  Etiquetar todo lo incierto como "Por verificar"
4. DISEÑAR     →  Completar design.md y tasks.md si aplica
5. REVISAR     →  Otro miembro del equipo valida contra las fuentes
6. APROBAR     →  El artefacto se marca como estado: APROBADO
7. IMPLEMENTAR →  Solo después de la aprobación se permite escribir en src/
```

---

## 7. Encabezado obligatorio en cada especificación

Cada `spec.md` o `design.md` en `specs/features/` debe comenzar con:

```markdown
**Feature**: [Nombre]
**ID**: NNNN
**Estado**: BORRADOR | EN REVISIÓN | APROBADO
**Autor**: <Nombre>
**Fecha**: AAAA-MM-DD
**Fuentes**: [F-01, F-02, ...]
```

---

## 8. Nomenclatura de archivos

| Tipo de artefacto | Convención | Ejemplo |
|-------------------|------------|---------|
| Especificación de feature | `spec.md` (dentro de la carpeta de la feature) | `specs/features/0001-casos-uso/spec.md` |
| Tareas de la feature | `tasks.md` (dentro de la carpeta de la feature) | `specs/features/0001-casos-uso/tasks.md` |
| ADR | `NNNN-titulo-kebab.md` | `specs/adr/0001-eleccion-canal-emision.md` |
| Contrato API | `nombre-api.yaml` (OpenAPI 3.1) | `specs/contracts/facturacion-api.yaml` |
| XSD anotado | `nombre-anotado.xsd` | `specs/contracts/schemas/UBL-Invoice-anotado.xsd` |

---

## 9. Prohibiciones explícitas

- ❌ No agregar archivos `.js`, `.ts`, `.java`, `.py`, `.cs` u otros lenguajes de implementación fuera de `src/`.
- ❌ No agregar archivos con credenciales (`.pfx`, `.p12`, `.env`, claves de API).
- ❌ No inventar valores de catálogo sin citar la fuente normativa.
- ❌ No modificar los archivos dentro de `docs/referencias/` — son solo lectura.
- ❌ No avanzar a implementación sin al menos un artefacto de cada `specs/features/NNNN-*/` en estado **APROBADO**.

---

*Última actualización: 2026-10-22 · Versión de las reglas: 2.0*
