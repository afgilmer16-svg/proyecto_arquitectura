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

El **primer parcial** fue la fase Spec-First: reglas, contratos de datos, validaciones y contratos de interfaz **antes** de escribir lógica de negocio. El **segundo parcial** implementa la primera pieza ejecutable: el generador del XML UBL 2.1 (ver abajo). Reglas: [`.agent/rules/spec-first.md`](.agent/rules/spec-first.md).

---

## Entregables del examen (sección 4)

| Entregable | Dónde está |
|---|---|
| 1. Modelo de dominio y contratos (DTOs / Schemas) | [`specs/features/0002-modelo-dominio/`](specs/features/0002-modelo-dominio/) · DTO en `components/schemas` de [`facturacion-api.yaml`](specs/contracts/facturacion-api.yaml) |
| 2. Diagramas de arquitectura MVC con visión a Hexagonal | [`specs/features/0003-arquitectura-mvc-hexagonal/design.md`](specs/features/0003-arquitectura-mvc-hexagonal/design.md) · [ADR](specs/adr/) |
| 3. Especificación de endpoints / servicios (contratos API) | [`specs/contracts/facturacion-api.yaml`](specs/contracts/facturacion-api.yaml) (OpenAPI 3.1, 33 operaciones) · [`specs/features/0004-contratos-api/`](specs/features/0004-contratos-api/) |
| 4. Matriz de trazabilidad normativa SUNAT ↔ UBL | [`specs/features/0005-trazabilidad-sunat-ubl/`](specs/features/0005-trazabilidad-sunat-ubl/) (incluye el Catálogo 08) |

## Segundo parcial — Generación del XML UBL 2.1

| Requisito | Dónde está |
|---|---|
| 1. Descripción del caso (contexto técnico, codificación, categorías 1001–1004) | [`docs/segundo-parcial/01-descripcion-caso.md`](docs/segundo-parcial/01-descripcion-caso.md) |
| 2. Generación del XML a partir del XSD Invoice 2.1 | [`src/facturacion_xml/`](src/facturacion_xml/) · XSD en [`docs/referencias/ubl/`](docs/referencias/ubl/) |
| 3. Lenguaje del dominio (Python) | [ADR-0005](specs/adr/0005-generador-ubl-python-lxml.md) |
| 4. Pruebas unitarias de generación | [`tests/test_generador_xml.py`](tests/test_generador_xml.py) · [resultados](docs/segundo-parcial/resultados-pruebas.txt) |
| Documentación técnica | [`docs/segundo-parcial/02-documentacion-tecnica.md`](docs/segundo-parcial/02-documentacion-tecnica.md) |
| Informe de correcciones y verificación | [`docs/segundo-parcial/03-informe-correcciones.md`](docs/segundo-parcial/03-informe-correcciones.md) |
| Feature | [`specs/features/0008-generacion-xml-ubl/`](specs/features/0008-generacion-xml-ubl/) |
| Contrato de la entrada (JSON Schema 2020-12) y ejemplos válidos e inválidos | [`specs/schemas/`](specs/schemas/factura-entrada.schema.json) · [`specs/ejemplos/`](specs/ejemplos/) |
| Reglas de arquitectura, cobertura mínima del 90 % y CI | [`tests/test_arquitectura.py`](tests/test_arquitectura.py) · [`.coveragerc`](.coveragerc) · [`.github/workflows/pruebas.yml`](.github/workflows/pruebas.yml) |
| Diagramas Mermaid como fuente (`.mmd`) y matriz de trazabilidad en CSV | [`diagramas/fuentes/`](diagramas/fuentes/) · [`specs/trazabilidad/csv/`](specs/trazabilidad/csv/) · [`tools/`](tools/) |

Generar un XML de ejemplo (PowerShell, desde la raíz):

```powershell
pip install -r requirements.txt
$env:PYTHONPATH = "src"
python -m facturacion_xml docs\segundo-parcial\ejemplos\factura-abarrotes.json salida\F001-00000001.xml --correlativo 1
```

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
│   ├── segundo-parcial/                  ← descripción del caso, documentación técnica, informe, ejemplos
│   └── referencias/
│       ├── ubl/                          ← XSD oficiales de UBL 2.1 (14 archivos)
│       └── sunat/                        ← Guía XML de factura y manual del programador
├── specs/
│   ├── adr/                              ← 0000 plantilla · 0001 a 0005
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
│   │   ├── 0007-distribucion-multiplataforma/
│   │   └── 0008-generacion-xml-ubl/      ← segundo parcial
│   └── templates/                        ← spec, design y tasks
├── src/
│   └── facturacion_xml/                  ← generador XML UBL 2.1 (segundo parcial)
├── tests/
│   ├── test_contratos.py                 ← valida los contratos
│   ├── test_generador_xml.py             ← pruebas unitarias de generación
│   ├── test_verificacion_integral.py     ← 300 comprobantes aleatorios, regresión, trazabilidad
│   ├── test_esquemas_y_ejemplos.py       ← JSON Schema + ejemplos válidos e inválidos
│   ├── test_arquitectura.py              ← reglas de dependencias entre módulos
│   ├── test_herramientas_y_derivados.py  ← .mmd y CSV al día con sus fuentes
│   └── mutacion.py                       ← 16 mutantes
├── tools/                                ← extraer_diagramas.py · exportar_matriz.py
├── diagramas/fuentes/                    ← 18 diagramas Mermaid (.mmd)
├── .github/workflows/pruebas.yml         ← CI: pruebas, cobertura y mutación
├── .coveragerc · redocly.yaml
├── .gitignore
├── README.md
└── requirements.txt
```

## Ejecutar las pruebas

```bash
pip install -r requirements.txt
pytest -v
pytest --cov=facturacion_xml     # cobertura (mínimo 90 %, definido en .coveragerc)
python tests/mutacion.py         # 16 errores introducidos a propósito: todos deben detectarse
```

El mismo conjunto corre en GitHub Actions en cada `push` ([`pruebas.yml`](.github/workflows/pruebas.yml)).

Para ver el contrato como documentación, pega `specs/contracts/facturacion-api.yaml` en [editor.swagger.io](https://editor.swagger.io). Para ver los diagramas, abre cualquier `design.md` con la vista previa de Markdown (`Ctrl + Shift + V`).

## Estado

Las observaciones del docente sobre la versión anterior y dónde se resolvió cada una están en [`docs/retroalimentacion-docente.md`](docs/retroalimentacion-docente.md).

Las features 0001 a 0007 (primer parcial) están **EN REVISIÓN**: el contenido está completo, pero hay datos normativos marcados **Por verificar** contra los PDF oficiales de SUNAT. La feature 0008 (segundo parcial) está **APROBADA** e implementada. La lista de pendientes está en [`docs/pendientes-verificacion.md`](docs/pendientes-verificacion.md).

| Etiqueta | Significado |
|---|---|
| `BORRADOR` | Primer borrador, no revisado |
| `EN REVISIÓN` | Completo; pendiente de revisión o de verificación normativa |
| `APROBADO` | Validado contra las fuentes; listo para implementar |
| `⚠️ Por verificar` | Dato pendiente de confirmar contra la fuente |
