# Facturación Electrónica SUNAT — ABC Software Corp

**Curso**: Arquitectura de Software · Examen de medio curso (Grupo A)  
**Universidad**: UNAS — Facultad de Ingeniería en Informática y Sistemas  
**Alumno**: Jorge Steven Tapullima Villar  
**Metodología**: Spec-First  
**Última actualización**: 2026-11-11

---

## Caso

**ABC Software Corp** comercializa abarrotes al por mayor y menor. Su software actual solo registra fichas de almacén (`fecha`, `ingreso`, `salida`, `producto`, `nro_documento`) y no cumple con SUNAT: no valida RUC, no calcula IGV, ISC ni ICBPER, no genera XML UBL 2.1 firmado y no se comunica con un OSE ni con SUNAT.

Marco normativo:

- **RS N° 097-2012/SUNAT**: sistema de emisión electrónica desde los sistemas del contribuyente (factura, boleta y notas en XML firmado, envío y CDR).
- **RS N° 234-2006/SUNAT**: libros y registros vinculados a asuntos tributarios. Exige llevar registros formales, como el Registro de Ventas y el Registro de Inventario Permanente (kardex), que reemplaza a las fichas informales de almacén.
- **Catálogos SUNAT** (Anexo 8), en especial el **Catálogo 08** (sistema de cálculo del ISC).
- **OASIS UBL 2.1** (`UBL-Invoice-2.1.xsd`).

Enunciado completo: [`docs/enunciado.md`](docs/enunciado.md) · Análisis: [`docs/analisis-problema.md`](docs/analisis-problema.md)

Esta es la **primera fase Spec-First**: se definen reglas, contratos de datos, validaciones y contratos de interfaz **antes** de escribir lógica de negocio. Por eso `src/` está vacío. Reglas: [`.agent/rules/spec-first.md`](.agent/rules/spec-first.md).

---

## Entregables del examen (sección 4)

| Entregable | Dónde está |
|---|---|
| 1. Modelo de dominio y contratos (DTOs / Schemas) | [`specs/features/0002-modelo-dominio/`](specs/features/0002-modelo-dominio/) · DTO en `components/schemas` de [`facturacion-api.yaml`](specs/contracts/facturacion-api.yaml) |
| 2. Diagramas de arquitectura MVC con visión a Hexagonal | [`specs/features/0003-arquitectura-mvc-hexagonal/design.md`](specs/features/0003-arquitectura-mvc-hexagonal/design.md) · [ADR](specs/adr/) |
| 3. Especificación de endpoints / servicios (contratos API) | [`specs/contracts/facturacion-api.yaml`](specs/contracts/facturacion-api.yaml) (OpenAPI 3.1, 33 operaciones) · [`specs/features/0004-contratos-api/`](specs/features/0004-contratos-api/) |
| 4. Matriz de trazabilidad normativa SUNAT ↔ UBL | [`specs/features/0005-trazabilidad-sunat-ubl/`](specs/features/0005-trazabilidad-sunat-ubl/) (incluye el Catálogo 08) |

## Restricciones del enunciado (sección 2) y casos de uso (sección 3)

| Requisito | Dónde se cubre |
|---|---|
| Descubrimiento de casos de uso (benchmark Nubefact) | [`specs/features/0001-casos-uso/`](specs/features/0001-casos-uso/) — CU-01 a CU-16 |
| Legado y migración de datos con trazabilidad | [`specs/features/0006-migracion-legado/`](specs/features/0006-migracion-legado/) |
| Normativa tributaria peruana (catálogos, series, correlativos, Catálogo 08) | [0002](specs/features/0002-modelo-dominio/spec.md) (reglas) y [0005](specs/features/0005-trazabilidad-sunat-ubl/spec.md) (matriz) |
| Monolito MVC desacoplado, listo para Hexagonal / microservicios | [`specs/features/0003-arquitectura-mvc-hexagonal/`](specs/features/0003-arquitectura-mvc-hexagonal/) · [ADR-0001](specs/adr/0001-monolito-mvc-desacoplado-mediante-puertos.md) |
| Windows (MSI) y Android (APK) | [`specs/features/0007-distribucion-multiplataforma/`](specs/features/0007-distribucion-multiplataforma/) · [ADR-0003](specs/adr/0003-kotlin-multiplatform-compose.md) |

Cada feature tiene:

- **`spec.md`**: qué se exige (requisitos, reglas, criterios de aceptación);
- **`design.md`**: cómo se resuelve (diagramas Mermaid, tablas, decisiones);
- **`tasks.md`**: avance y pendientes.

---

## Estructura

```
proyecto_arquitectura/
├── docs/
│   ├── enunciado.md · analisis-problema.md · caso.md · glosario.md
│   ├── pendientes-verificacion.md        ← datos "Por verificar" y su fuente
│   ├── retroalimentacion-docente.md      ← observaciones del docente y su estado
│   └── referencias/
│       ├── ubl/                          ← XSD oficiales de UBL 2.1
│       └── sunat/                        ← Guía XML de factura y manual del programador
├── specs/
│   ├── adr/                              ← 0000 plantilla · 0001 a 0004
│   ├── contracts/
│   │   ├── facturacion-api.yaml          ← contrato OpenAPI 3.1 (endpoints + DTO)
│   │   └── schemas/
│   ├── features/
│   │   ├── 0001-casos-uso/               ← spec.md · design.md · tasks.md
│   │   ├── 0002-modelo-dominio/
│   │   ├── 0003-arquitectura-mvc-hexagonal/
│   │   ├── 0004-contratos-api/
│   │   ├── 0005-trazabilidad-sunat-ubl/
│   │   ├── 0006-migracion-legado/        ← + diccionario-legado.md
│   │   └── 0007-distribucion-multiplataforma/
│   └── templates/                        ← spec, design y tasks
├── src/                                  ← vacío a propósito (fase Spec-First)
├── tests/
│   └── test_contratos.py                 ← valida los contratos, no lógica de negocio
├── .gitignore
├── README.md
└── requirements.txt
```

## Validar los contratos

```bash
pip install -r requirements.txt
pytest -v
```

Para ver el contrato como documentación, pega `specs/contracts/facturacion-api.yaml` en [editor.swagger.io](https://editor.swagger.io). Para ver los diagramas, abre cualquier `design.md` con la vista previa de Markdown (`Ctrl + Shift + V`).

## Estado

Las observaciones del docente sobre la versión anterior y dónde se resolvió cada una están en [`docs/retroalimentacion-docente.md`](docs/retroalimentacion-docente.md).

Todas las features están **EN REVISIÓN**: el contenido está completo, pero hay datos normativos marcados **Por verificar** contra los PDF oficiales de SUNAT. La lista está en [`docs/pendientes-verificacion.md`](docs/pendientes-verificacion.md).

| Etiqueta | Significado |
|---|---|
| `BORRADOR` | Primer borrador, no revisado |
| `EN REVISIÓN` | Completo; pendiente de revisión o de verificación normativa |
| `APROBADO` | Validado contra las fuentes; listo para implementar |
| `⚠️ Por verificar` | Dato pendiente de confirmar contra la fuente |
