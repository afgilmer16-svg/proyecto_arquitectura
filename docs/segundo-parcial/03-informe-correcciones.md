# Segundo parcial — Informe de correcciones y verificación

**Fecha**: 2026-11-11  
**Base de comparación**: estado del repositorio en GitHub antes de este trabajo (commit `f5b2be4`, "Corrige observaciones del docente…").  
**Referencias del docente**: observaciones escritas sobre el primer parcial (`gilmer_50.docx`, resumidas en [`docs/retroalimentacion-docente.md`](../retroalimentacion-docente.md)) y requisitos del segundo parcial vistos en clase (descripción del caso, generación XML desde el XSD Invoice 2.1, lenguaje del dominio y pruebas unitarias de generación).

---

## 1. Errores que tenía el proyecto

| # | Error encontrado | Impacto | Corrección |
|---|---|---|---|
| E-01 | **El esquema XSD estaba incompleto.** Faltaban `UBL-xmldsig-core-schema-2.1.xsd`, `UBL-XAdESv132-2.1.xsd` y `UBL-XAdESv141-2.1.xsd`. `docs/referencias/ubl/README.md` afirmaba que estaban "todos los módulos comunes". | `UBL-Invoice-2.1.xsd` **no compilaba**: ningún XML podía validarse contra el XSD. | Se agregaron los 3 archivos de la versión OS con su procedencia y SHA-256, y se corrigió el README. |
| E-02 | No existía el generador XML (`src/` vacío por diseño en el primer parcial). | El segundo parcial lo exige. | Se implementó `src/facturacion_xml/` (ADR-005, feature 0008). |
| E-03 | **Los RUC de ejemplo del contrato eran inválidos** (`20601234567` y `20555555551` no cumplen el módulo 11). | Un validador correcto rechazaría los ejemplos del propio contrato. | Se reemplazaron por `20601234565` y `20555555556`, ambos válidos (`facturacion-api.yaml`, `0004/design.md`). |
| E-04 | La matriz 0005 ubicaba las cuotas en `PaymentTerms[cbc:ID='Cuota{n}']`. | XPath incorrecto: `cbc:ID` lleva `FormaPago` y el número de cuota va en `cbc:PaymentMeansID`. | Corregido a `PaymentMeansID='Cuota001'…`, marcado "Por verificar". |
| E-05 | La matriz 0005 fijaba el IGV como "constante 18.00". | Contradecía lo visto en clase (10 % para restaurantes y hoteles). | La tasa pasa a ser el parámetro `regimen_igv` (18 % por defecto, 10 % solo si se indica). |
| E-06 | La matriz 0005 usaba `@schemeName="RUC"` para el emisor. | No coincidía con la Guía XML SUNAT. | `schemeName="SUNAT:Identificador de Documento de Identidad"` y `schemeURI` del Catálogo 06. |
| E-07 | La arquitectura nombraba un adaptador Java (`JaxbUblAdapter`). | No coincidía con el generador implementado en Python. | ADR-005: `LxmlUblAdapter` en Python, con los mismos principios que pidió el docente; se actualizaron 0002, 0003 y 0005. |
| E-08 | `tests/test_contratos.py` exigía `src/` vacío. | La prueba fallaría al implementar el parcial. | Ahora verifica que `src/` solo contenga el generador autorizado. |
| E-09 | `requirements.txt` no incluía `lxml`. | El generador no se podría instalar. | Agregado `lxml>=5.0`. |
| E-10 | No había tratamiento de la codificación de la entrada. | Un JSON guardado en WIN1252 corrompería la "ñ" y las tildes. | Lectura UTF-8 estricta con soporte de BOM y error explícito si el archivo está en WIN1252. |
| E-11 | Las categorías 1001–1004 vistas en clase no estaban modeladas. | No había correspondencia entre la clase y el XML. | `CategoriaOperacion` y su correspondencia con los tributos 1000, 9998, 9997 y 9996. Se documenta que `AdditionalMonetaryTotal` (UBL 2.0) no existe en UBL 2.1. |

### 1.1 Errores encontrados por la verificación integral (2026-11-11, segunda ronda)

| # | Error encontrado | Cómo se detectó | Corrección |
|---|---|---|---|
| E-12 | El precio unitario con tributos se redondeaba a 2 decimales: precio × cantidad no reconstruía la línea (bolsa `0.12` en lugar de `0.118`). | Falló el contraste con la respuesta de ejemplo del contrato y 234 de 300 comprobantes aleatorios. | Se conserva con hasta 10 decimales (Guía: n(12,10)). Se regeneró `F001-00000001.xml`. |
| E-13 | 4 elementos que emite el generador no figuraban en la matriz 0005 (`LineCountNumeric`, `AddressTypeCode`, `Country`, `IdentificationCode`). | Prueba de trazabilidad elemento ↔ matriz. | Filas agregadas a la matriz 0005 (§3 y §4). |
| E-14 | Ninguna prueba demostraba que `generar()` valida contra el XSD **antes de entregar** el XML: la prueba con ese nombre validaba por fuera. | Prueba de mutación M04 (quitar la validación): la suite seguía en verde. | Nueva prueba que rompe el árbol dentro del generador y exige `XmlNoConformeXsdError`, más una prueba con un validador espía. M04 ahora se detecta. |

### 1.2 Mejoras de ingeniería agregadas antes de la entrega (2026-11-11, tercera ronda)

Tras comparar el proyecto con otro trabajo del curso se agregaron complementos que no cambian el generador:

| # | Mejora | Evidencia |
|---|---|---|
| M-01 | **Clon limpio en Windows**: `core.autocrlf` convertía el XML de ejemplo a CRLF y fallaba una prueba. Se agregó `.gitattributes` (LF) y la prueba tolera CRLF. | `.gitattributes`, `TestRegresion` |
| M-02 | **Integración continua** en GitHub Actions (Python 3.10 y 3.12): pruebas, cobertura y mutación. | `.github/workflows/pruebas.yml` |
| M-03 | **Cobertura mínima del 90 %** (hoy 95 %). | `.coveragerc`, `pytest-cov` en `requirements.txt` |
| M-04 | **JSON Schema 2020-12** de la entrada (capa 1) y **12 ejemplos inválidos** + 4 válidos adicionales como archivos. Una prueba comprueba que el esquema y el modelo Python no se contradicen. | `specs/schemas/`, `specs/ejemplos/`, `tests/test_esquemas_y_ejemplos.py` |
| M-05 | **Reglas de arquitectura** ejecutables (núcleo sin infraestructura, `lxml` solo en adaptadores, sin ciclos, sin `float`). | `tests/test_arquitectura.py` |
| M-06 | **Diagramas `.mmd`** como fuente (18) y **matriz 0005 en CSV** (12 archivos + índice), regenerables y vigilados por pruebas. | `tools/`, `diagramas/fuentes/`, `specs/trazabilidad/csv/` |
| M-07 | `redocly.yaml` para el lint del contrato. | `redocly.yaml` |

## 2. Observaciones del docente atendidas

| Observación del docente | Cómo se atendió en este parcial | Evidencia |
|---|---|---|
| "XML con plantillas Freemarker" | El XML se construye como árbol lxml en el orden del XSD; no hay plantillas. | `generador_ubl.py`; `test_el_generador_no_usa_plantillas_y_valida_antes_de_entregar` |
| "Validar contra el XSD antes de firmar" | Cada XML se valida contra `UBL-Invoice-2.1.xsd` antes de devolverse. | `GeneradorFacturaUbl.generar`; `TestValidacionXsd` |
| "Dejar el adaptador como el único" | `LxmlUblAdapter` es el único adaptador de `GeneradorXmlUblPort`. | ADR-005, `0003/design.md` |
| "Modelar el ISC (Catálogo 08) y calcularlo antes que el IGV" | Los 3 sistemas, con validación condicional; el ISC integra la base del IGV. | `calculo.py`; `TestIscEIcbper` |
| "Unidad de medida como enum del Catálogo 03" | Se rechazan códigos fuera del catálogo. | `test_unidad_de_medida_fuera_del_catalogo_03` |
| "Patrón de serie sin barra; serie coherente con el tipo" | `^[FB][A-Z0-9]{3}$` y F↔01, B↔03. | `test_serie_invalida_o_incoherente_con_el_tipo` (incluye `F\|01`) |
| "Quitar el correlativo de la solicitud" | Lo asigna el servidor; si el cliente lo envía, se rechaza. | `test_el_cliente_no_puede_enviar_el_correlativo` |
| "Importes como texto decimal; sin double" | Entrada en texto y cálculo con `Decimal`; se rechazan `float`. | `test_importe_como_numero_flotante_se_rechaza` |
| "ICBPER parametrizado por fecha" | Importe por año de emisión (0.50 desde 2023). | `test_icbper_2022_usa_el_importe_de_ese_anio` |
| "Que el cliente no envíe impuestos" | El generador calcula todos los tributos y totales. | `calculo.py` |
| "Correr la validación antes de marcar el criterio" | Las pruebas se ejecutaron y su salida está guardada; no se marcó nada sin correr. | `resultados-pruebas.txt` |

## 3. Archivos modificados

| Archivo | Cambio |
|---|---|
| `README.md` | Sección del segundo parcial y estructura actualizada |
| `requirements.txt` | `lxml` y comentarios por grupo |
| `.agent/rules/spec-first.md` | `src/` autorizado solo para el generador de la feature 0008 |
| `docs/pendientes-verificacion.md` | Nuevos puntos por verificar del generador |
| `docs/retroalimentacion-docente.md` | `JaxbUblAdapter` → `LxmlUblAdapter` (ADR-005) |
| `docs/referencias/ubl/README.md` | Procedencia, versión, SHA-256 y corrección del error E-01 |
| `specs/contracts/facturacion-api.yaml` | RUC de ejemplo válidos (E-03) |
| `specs/features/0002-modelo-dominio/design.md` | Nombre del adaptador |
| `specs/features/0003-arquitectura-mvc-hexagonal/design.md` y `tasks.md` | `LxmlUblAdapter` implementado |
| `specs/features/0004-contratos-api/design.md` | RUC de ejemplo válido |
| `specs/features/0005-trazabilidad-sunat-ubl/spec.md` y `design.md` | Cuotas, `schemeName` del RUC y tasa de IGV (E-04 a E-06) |
| `src/README.md` | Estado del código autorizado |
| `tests/test_contratos.py` | Prueba de `src/` (E-08) |

## 4. Archivos nuevos

| Archivo | Contenido |
|---|---|
| `src/facturacion_xml/__init__.py`, `__main__.py`, `catalogos.py`, `modelo.py`, `calculo.py`, `generador_ubl.py`, `validador_xsd.py`, `numero_letras.py`, `errores.py` | Generador XML UBL 2.1 |
| `tests/test_generador_xml.py` | 89 pruebas unitarias de generación |
| `tests/test_verificacion_integral.py` | 310 pruebas: coherencia contrato ↔ código, 300 comprobantes aleatorios con oráculo independiente, regresión y trazabilidad |
| `tests/mutacion.py` | Pruebas de mutación (16 errores introducidos a propósito) |
| `tests/conftest.py` | Hace importable `src/` para pytest |
| `docs/referencias/ubl/UBL-xmldsig-core-schema-2.1.xsd`, `UBL-XAdESv132-2.1.xsd`, `UBL-XAdESv141-2.1.xsd` | XSD oficiales que faltaban |
| `docs/segundo-parcial/01-descripcion-caso.md` | Requisito 1 |
| `docs/segundo-parcial/02-documentacion-tecnica.md` | Documentación técnica |
| `docs/segundo-parcial/03-informe-correcciones.md` | Este informe |
| `docs/segundo-parcial/resultados-pruebas.txt` | Salida real de `pytest -v` |
| `docs/segundo-parcial/ejemplos/factura-abarrotes.json` y `F001-00000001.xml` | Entrada de ejemplo y XML generado |
| `specs/features/0008-generacion-xml-ubl/spec.md`, `design.md`, `tasks.md` | Feature del segundo parcial |
| `specs/adr/0005-generador-ubl-python-lxml.md` | Decisión del lenguaje y la técnica |

## 5. Funcionalidades implementadas

- Generación del XML `Invoice` UBL 2.1 para factura (`01`) y boleta (`03`), en PEN o USD.
- Validación de la entrada con todos los errores juntos y la ruta de cada campo.
- Cálculo de IGV (18 % / 10 %), ISC (sistemas 01, 02 y 03) e ICBPER por fecha.
- Categorías 1001 gravada, 1002 inafecta, 1003 exonerada y 1004 gratuita, cada una con su tributo.
- Forma de pago al contado y al crédito con cuotas.
- Leyendas 1000 (importe en letras) y 1002 (transferencia gratuita).
- Validación contra `UBL-Invoice-2.1.xsd` sin acceso a red.
- Línea de comandos con lectura UTF-8 y detección de WIN1252.

## 6. Pruebas ejecutadas

| Ejecución | Resultado |
|---|---|
| `pytest -v` (suite completa, con `openapi-spec-validator` 0.9.0 real, Windows 11 / Python 3.12.7) | **460 aprobadas, 0 fallidas**. Salida completa en [`resultados-pruebas.txt`](resultados-pruebas.txt). |
| `python tests/mutacion.py` | **16 de 16 mutantes detectados**. |
| CLI con el ejemplo | XML generado y validado: `docs/segundo-parcial/ejemplos/F001-00000001.xml`. |
| CLI con correlativo `0` | Rechazado con mensaje claro y código de salida 1. |
| Los 18 diagramas Mermaid del proyecto | Se renderizan sin errores. |

## 7. Pendiente y por qué

| Pendiente | Motivo | Acción |
|---|---|---|
| Volver a correr `redocly lint` | Cambiaron los RUC de ejemplo del contrato. | `npx.cmd @redocly/cli lint specs/contracts/facturacion-api.yaml`. |
| Contrastar la estructura de las gratuitas, los códigos del ICBPER y las cuotas con el anexo de validaciones vigente de SUNAT | La guía local es la v1.0 (2017), anterior a esas reglas. | Ver `docs/pendientes-verificacion.md`. |
| Firma, envío al OSE, notas, bajas y resúmenes | Fuera del alcance del segundo parcial. | Parciales posteriores. |

## 8. Verificación final: requisito → evidencia

| Requisito | Cumple | Evidencia |
|---|---|---|
| **R1** Descripción del caso: problema, contexto técnico, necesidad del XML, relación con SUNAT, objetivo, alcance y funcionamiento | ✅ | [`01-descripcion-caso.md`](01-descripcion-caso.md) §1–§7 |
| R1 · Dónde encaja el generador en la arquitectura (puerto `GeneradorXmlUblPort`) | ✅ | `01-descripcion-caso.md` §2.1 |
| R1 · Tecnologías utilizadas (Python, lxml, XSD, pytest, Git) | ✅ | `01-descripcion-caso.md` §2.2 |
| R1 · Codificación UTF-8 / WIN1252 | ✅ | `01-descripcion-caso.md` §2.3; `__main__.py`; `test_rechaza_json_guardado_en_win1252` |
| R1 · Categorías 1001–1004 y tasas 18 % / 10 % | ✅ | `01-descripcion-caso.md` §4; `catalogos.py`; `TestCategoriasDeOperacion` |
| **R2** XML generado a partir del XSD Invoice 2.1 real, con procedencia y versión documentadas | ✅ | `generador_ubl.py`; `docs/referencias/ubl/README.md` |
| R2 · Elementos obligatorios y opcionales identificados | ✅ | `02-documentacion-tecnica.md` §5 |
| R2 · Tipos y restricciones del esquema respetados | ✅ | `test_xml_generado_es_valido_contra_el_xsd_oficial` y validación en cada `generar()` |
| R2 · Sin etiquetas ni estructuras ajenas al XSD | ✅ | `test_no_genera_elementos_ajenos_al_xsd_ubl_21`; `test_detecta_elemento_inexistente_en_el_xsd` |
| R2 · Si falta el esquema, se identifica qué documento falta | ✅ | Error E-01; `EsquemaXsdIncompletoError`; `test_falla_con_mensaje_claro_si_falta_un_xsd` |
| **R3** Lenguaje del dominio (Python) y términos del dominio | ✅ | ADR-005; nombres `Factura`, `CategoriaOperacion`, `TaxSubtotal`, `RegimenIgv`… |
| **R4** Pruebas unitarias de generación | ✅ | `tests/test_generador_xml.py` (89) + `tests/test_verificacion_integral.py` (310) |
| R4 · Se genera el archivo XML | ✅ | `TestGeneracion` |
| R4 · Estructura con los elementos esperados | ✅ | `TestEstructura` |
| R4 · Datos en sus etiquetas | ✅ | `TestDatosEnEtiquetas`, `TestCategoriasDeOperacion`, `TestIscEIcbper` |
| R4 · Elementos obligatorios | ✅ | `test_elementos_obligatorios_del_xsd_presentes`, `test_detecta_falta_de_elemento_obligatorio` |
| R4 · Formatos esperados | ✅ | `TestFormatos` |
| R4 · Datos faltantes o inválidos detectados | ✅ | `TestDatosInvalidos` (30) |
| R4 · Validación contra el XSD | ✅ | `TestValidacionXsd` y validación de cada XML positivo |
| R4 · Casos negativos que detectan errores | ✅ | `TestDatosInvalidos` + `TestValidacionXsd` (36) |
| R4 · Resultados reales y verificables | ✅ | `resultados-pruebas.txt`; `pytest -v` reproducible |
| R4 · Las pruebas detectan errores reales | ✅ | `tests/mutacion.py`: 16/16 mutantes detectados |
| No se avanzó al tercer parcial ni posteriores | ✅ | Sin Docker, sin base de datos, sin API REST ni interfaz de usuario |
