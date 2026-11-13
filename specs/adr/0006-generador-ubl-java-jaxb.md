# ADR-006 — Generador XML UBL 2.1 en Java con clases JAXB generadas desde el XSD

**Estado**: APROBADO
**Fecha**: 2026-11-11
**Autores**: Gilmer Ayala Falcón (Grupo A, UNAS-FIIS)
**Fuentes**: F-01 (XSD OASIS UBL 2.1), F-05 (Guía XML SUNAT v1.0), retroalimentación del docente (`docs/retroalimentacion-docente.md`)
**Relacionado**: ADR-001, ADR-003, [ADR-005](0005-generador-ubl-python-lxml.md) (prototipo), feature [0008](../features/0008-generacion-xml-ubl/spec.md)
**Reemplaza a**: ADR-005 como implementación principal

---

## Contexto

El segundo parcial es la continuación del primero: pide implementar la generación del XML a partir de `UBL-Invoice-2.1.xsd`, **en el lenguaje del dominio**, con pruebas unitarias de generación. El primer parcial fijó el stack: backend **Java** con arquitectura MVC desacoplada por puertos (ADR-001, feature 0003, cuyo árbol de paquetes está en clases `.java`) y UI en Kotlin Multiplatform (ADR-003).

En la evaluación del primer parcial, el docente observó que el XML se generaba con plantillas Freemarker y recomendó “clases generadas desde los XSD oficiales (JAXB con xjc, ya que el stack es Java), validar contra el XSD antes de firmar, y dejar ese adaptador como único”.

Antes de esta decisión se construyó un prototipo en Python con lxml (ADR-005). Funciona, pero rompe la continuidad con el stack del primer parcial y no genera clases desde el XSD.

## Decisión

El adaptador principal es `GeneradorUblJaxbAdapter`, en Java 21, dentro de [`generador-java/`](../../generador-java/):

- **Clases generadas desde el XSD.** En cada compilación, `jaxb-maven-plugin` (xjc) genera ≈ 1 240 clases a partir de `docs/referencias/ubl/UBL-Invoice-2.1.xsd`. El XML se produce llenando esos objetos y serializándolos con JAXB; nunca con plantillas ni concatenando texto. Un elemento mal escrito o fuera de orden no compila.
- **Un solo origen del esquema.** Los XSD se leen de `docs/referencias/ubl/` (sin copias en el repositorio); Maven los copia a `target/` con la estructura `maindoc/` + `common/` que esperan sus `schemaLocation`.
- **Validación antes de entregar.** Cada XML se valida contra el XSD oficial (`ValidadorXsd`) y, si no cumple, se lanza `XmlNoConformeXsdException`.
- **Hexagonal.** Puerto de salida `GeneradorXmlUblPort` (paquete `aplicacion.puerto`); dominio sin dependencias (`dominio`); caso de uso `GenerarFacturaXmlService`; adaptadores en `infraestructura` (`ubl`, `entrada`). Las reglas se verifican con ArchUnit.
- **Contrato de entrada aplicado, no copiado.** El primer control valida con `specs/schemas/factura-entrada.schema.json` (JSON Schema 2020-12); el segundo son las reglas de dominio (`LectorFactura`) y el tercero es el XSD.
- **Importes exactos.** `BigDecimal` con redondeo HALF_UP por línea; nunca `double`.
- **Calidad exigida en el build.** `mvn verify` falla si la cobertura de líneas del código propio baja del 90 % (hoy 97 %). El CI lo ejecuta en cada `push`.

El prototipo Python (`src/facturacion_xml/`, ADR-005) **se conserva** como implementación de referencia: `RegresionCruzadaTest` comprueba que ambos adaptadores producen **el mismo documento** para los 5 ejemplos (`specs/ejemplos/xml-referencia/`, generados por Python).

## Consecuencias

- **Positivas**: continuidad con el primer parcial (Java, puertos y adaptadores), cumplimiento literal de la recomendación del docente y una segunda implementación independiente que sirve de oráculo.
- **Compromisos**: el build necesita JDK 21 y descarga dependencias de Maven Central la primera vez (el Maven Wrapper evita instalar Maven). Hay dos implementaciones que mantener; el prototipo Python puede retirarse cuando ya no se necesite como oráculo.
- **Riesgo**: que los dos adaptadores se separen. Se mitiga con la regresión cruzada y con los ejemplos compartidos de `specs/ejemplos/`.
