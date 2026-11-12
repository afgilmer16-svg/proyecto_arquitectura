# Reglas Spec-First — Proyecto Facturación Electrónica SUNAT
## UNAS-FIIS · Arquitectura de Software · Grupo A · ABC Software Corp

---

## 1. Principio fundamental

> **No se escribe lógica de negocio hasta que la especificación esté completa y verificada.**

Todo artefacto en este repositorio es una especificación, una referencia normativa o una decisión de arquitectura — **nunca código ejecutable de producción**.  
En `src/` solo hay código de features **APROBADAS**. Hoy la única es la 0008 (generador XML UBL 2.1, segundo parcial), en `src/facturacion_xml/`, en Python (ADR-005). `tests/` contiene las pruebas de los contratos (`test_contratos.py`) y las pruebas unitarias de generación (`test_generador_xml.py`). Ninguna prueba se marca como aprobada sin haberla ejecutado.

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
| F-03 | Resolución de Superintendencia N° 097-2012/SUNAT — sistema de emisión electrónica desde los sistemas del contribuyente | ⚠️ Por incorporar en `docs/referencias/sunat/` |
| F-04 | Resolución de Superintendencia N° 234-2006/SUNAT — libros y registros vinculados a asuntos tributarios (Registro de Ventas, Inventario Permanente) | ⚠️ Por incorporar en `docs/referencias/sunat/` |
| F-05 | Manual del Programador — OSE/SOL SUNAT (versión vigente) | `docs/referencias/sunat/` |
| F-06 | Reglamento de Comprobantes de Pago — RS N° 007-99/SUNAT (requisitos de facturas y boletas) | ⚠️ Por incorporar en `docs/referencias/sunat/` |

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
  analisis-problema.md      → AS-IS / TO-BE, stakeholders, restricciones, riesgos
  caso.md                   → Brecha frente a SUNAT, alcance de la fase y actores
  glosario.md               → Términos del dominio
  pendientes-verificacion.md → Registro de datos "Por verificar" y su fuente
  referencias/
    ubl/                    → Esquemas OASIS UBL 2.1 (solo lectura)
    sunat/                  → Guía XML, manual del programador, normas y catálogos

specs/
  adr/                      → Decisiones de arquitectura (NNNN-titulo.md)
  contracts/
    facturacion-api.yaml    → Contrato OpenAPI 3.1: endpoints + DTO en components/schemas (fuente única)
    schemas/                → Reservada (los DTO viven en facturacion-api.yaml)
  features/
    NNNN-nombre/
      spec.md               → Qué se exige: requisitos, reglas y criterios de aceptación
      design.md             → Cómo se resuelve: diagramas, tablas, decisiones
      tasks.md              → Avance y pendientes
  templates/                → Plantillas de spec, design y tasks

src/facturacion_xml/        → Generador XML UBL 2.1 (feature 0008, único código autorizado)
tests/                      → test_contratos.py (contratos) y test_generador_xml.py (generación XML)
requirements.txt            → Dependencias de las pruebas de contratos
```

Correspondencia con el enunciado: 0001 casos de uso (sección 3) · 0002 modelo de dominio y DTO (entregable 1) · 0003 MVC → Hexagonal (entregable 2) · 0004 contratos API (entregable 3) · 0005 matriz SUNAT ↔ UBL y Catálogo 08 (entregable 4) · 0006 legado · 0007 MSI / APK.

---

## 6. Flujo de trabajo Spec-First

```
1. INVESTIGAR  →  Leer fuentes normativas (F-01 … F-06) en docs/referencias/
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
| Diseño de la feature | `design.md` (dentro de la carpeta de la feature) | `specs/features/0003-arquitectura-mvc-hexagonal/design.md` |

---

## 9. Prohibiciones explícitas

- ❌ No agregar archivos `.js`, `.ts`, `.java`, `.py`, `.cs` u otros lenguajes de implementación fuera de `src/` y `tests/`.
- ❌ No implementar en `src/` nada fuera de la feature 0008 hasta que el docente habilite el siguiente parcial (base de datos, API REST, interfaz de usuario, despliegue).
- ❌ No modificar los XSD de `docs/referencias/ubl/` ni reemplazarlos por esquemas hechos a mano.
- ❌ No agregar archivos con credenciales (`.pfx`, `.p12`, `.env`, claves de API).
- ❌ No inventar valores de catálogo sin citar la fuente normativa.
- ❌ No modificar los archivos dentro de `docs/referencias/` — son solo lectura.
- ❌ No avanzar a implementación sin al menos un artefacto de cada `specs/features/NNNN-*/` en estado **APROBADO**.

---

*Última actualización: 2026-11-11 · Versión de las reglas: 3.0*
