# ADR-005 — Generador XML UBL 2.1 en Python con lxml

**Estado**: APROBADO como prototipo · la implementación principal es el [ADR-006](0006-generador-ubl-java-jaxb.md) (Java + JAXB)  
**Fecha**: 2026-11-11  
**Autores**: Gilmer Ayala Falcón (Grupo A, UNAS-FIIS)  
**Fuentes**: F-01 (XSD OASIS UBL 2.1), F-05 (Guía XML SUNAT v1.0), retroalimentación del docente (`docs/retroalimentacion-docente.md`)  
**Relacionado**: ADR-001, feature [0008](../features/0008-generacion-xml-ubl/spec.md)

---

## Contexto

El segundo parcial pide implementar la generación del XML a partir del XSD `UBL-Invoice-2.1.xsd`, **en el lenguaje del dominio**, y probarla con pruebas unitarias de generación.

En la evaluación del primer parcial, el docente observó que el XML se generaba con plantillas Freemarker y recomendó “clases generadas desde los XSD oficiales (JAXB con xjc, **ya que el stack es Java**), validar contra el XSD antes de firmar, y dejar ese adaptador como único”. La arquitectura del primer parcial (ADR-001) desacopla el núcleo mediante puertos: el generador es un **adaptador del puerto `GeneradorXmlUblPort`** y su lenguaje no afecta al resto del sistema.

## Opciones consideradas

1. **JAXB + xjc (Java).** Cumple la recomendación al pie de la letra y coincide con el backend documentado en el primer parcial. Exige un proyecto Maven/Gradle completo y la generación de cientos de clases solo para producir un documento.
2. **Plantillas de texto (Jinja2, Freemarker).** Es justo lo que el docente penalizó: una etiqueta mal escrita no se detecta hasta validar.
3. **xsdata (genera clases Python desde el XSD, equivalente a xjc).** Es fiel a la idea de “clases desde el XSD”, pero agrega una dependencia y un paso de generación de código que no aporta al alcance del parcial.
4. **Árbol de elementos con lxml + validación contra el XSD oficial (elegida).** lxml construye cada elemento con su espacio de nombres (no hay texto suelto) y valida con libxml2 contra el XSD real. Python aporta `decimal` para importes exactos y `pytest` para pruebas parametrizadas, de mutación y de arquitectura.

## Decisión

Se implementa `LxmlUblAdapter` en Python, en `src/facturacion_xml/`:

- el XML se construye como árbol (`etree.SubElement`), en el orden de la secuencia de `InvoiceType` y de cada tipo agregado del XSD;
- **cada XML se valida contra `UBL-Invoice-2.1.xsd`** (OASIS UBL 2.1 OS, sin modificar) antes de entregarlo; si no cumple, se lanza `XmlNoConformeXsdError`;
- es el **único** adaptador de `GeneradorXmlUblPort`;
- los datos se validan antes de generar (`DatosFacturaInvalidosError`) y los importes se calculan con `Decimal`, nunca con punto flotante.

Se conservan los principios que pidió el docente (sin plantillas, fiel al XSD, validado antes de firmar, adaptador único). Solo cambia la técnica: un árbol construido con lxml en lugar de clases generadas.

## Consecuencias

- **Positivas**: el generador es Python puro, sin base de datos ni servidor web, y se prueba de forma aislada con `pytest` contra el XSD real.
- **Compromisos**: el resto del stack documentado en el primer parcial (backend Java y UI con Kotlin Multiplatform, ADR-001 y ADR-003) no se modifica. Si el backend necesita un adaptador en el mismo proceso, un `JaxbUblAdapter` implementaría el mismo puerto `GeneradorXmlUblPort`; el contrato de la entrada (`specs/schemas/factura-entrada.schema.json`) y el XSD siguen siendo los mismos, y los ejemplos de `specs/ejemplos/` sirven para comprobar que ambos adaptadores producen el mismo documento.
- **Riesgo**: que el orden de los elementos dependa del código y no de clases generadas. Se mitiga con pruebas que validan contra el XSD y comprueban el orden de la cabecera (`tests/test_generador_xml.py`).
