# ADR-005 — Generador XML UBL 2.1 en Python con lxml

**Estado**: APROBADO  
**Fecha**: 2026-11-11  
**Autores**: Equipo Grupo A — UNAS-FIIS  
**Fuentes**: F-01 (XSD OASIS UBL 2.1), F-05 (Guía XML SUNAT v1.0), retroalimentación del docente (`docs/retroalimentacion-docente.md`)  
**Relacionado**: ADR-001, feature [0008](../features/0008-generacion-xml-ubl/spec.md)

---

## Contexto

El segundo parcial pide implementar la generación del XML a partir del XSD `UBL-Invoice-2.1.xsd`, **en el lenguaje del dominio** del curso, y probarla con pruebas unitarias de generación. El dominio del curso es Odoo, un ERP escrito en Python: su núcleo es monolítico y se extiende con módulos (addons) también escritos en Python, que acceden a PostgreSQL mediante su ORM.

En la evaluación del primer parcial, el docente observó que el XML se generaba con plantillas Freemarker y recomendó “clases generadas desde los XSD oficiales (JAXB con xjc, **ya que el stack es Java**), validar contra el XSD antes de firmar, y dejar ese adaptador como único”. La recomendación de JAXB depende de que el stack sea Java. Con el contexto Odoo del segundo parcial, el lenguaje pasa a ser Python.

## Opciones consideradas

1. **JAXB + xjc (Java).** Cumple la recomendación al pie de la letra, pero obliga a usar Java en un curso cuyo dominio es Odoo/Python y no podría reutilizarse como addon.
2. **Plantillas de texto (Jinja2/QWeb).** Es lo que hace Odoo en sus módulos EDI, pero es justo lo que el docente penalizó: una etiqueta mal escrita no se detecta hasta validar.
3. **xsdata (genera clases Python desde el XSD, equivalente a xjc).** Es fiel a la idea de “clases desde el XSD”, pero agrega una dependencia y un paso de generación de código que no aporta al alcance del parcial.
4. **Árbol de elementos con lxml + validación contra el XSD oficial (elegida).** lxml ya es dependencia de Odoo, construye cada elemento con su espacio de nombres (no hay texto suelto) y valida con libxml2 contra el XSD real.

## Decisión

Se implementa `LxmlUblAdapter` en Python, en `src/facturacion_xml/`:

- el XML se construye como árbol (`etree.SubElement`), en el orden de la secuencia de `InvoiceType` y de cada tipo agregado del XSD;
- **cada XML se valida contra `UBL-Invoice-2.1.xsd`** (OASIS UBL 2.1 OS, sin modificar) antes de entregarlo; si no cumple, se lanza `XmlNoConformeXsdError`;
- es el **único** adaptador de `GeneradorXmlUblPort`;
- los datos se validan antes de generar (`DatosFacturaInvalidosError`) y los importes se calculan con `Decimal`, nunca con punto flotante.

Se conservan los principios que pidió el docente (sin plantillas, fiel al XSD, validado antes de firmar, adaptador único). Solo cambia el lenguaje, por el contexto Odoo.

## Consecuencias

- **Positivas**: el generador es Python puro, sin dependencias de Odoo, y podrá empaquetarse como addon en un parcial posterior sin reescribirlo. Las pruebas corren con `pytest` y validan contra el XSD real.
- **Compromisos**: el resto del stack documentado en el primer parcial (backend Java y UI con Kotlin Multiplatform, ADR-001 y ADR-003) no se modifica en este parcial. Su alineación con Odoo corresponde a la arquitectura de plugins del cuarto parcial y queda fuera de este alcance.
- **Riesgo**: que el orden de los elementos dependa del código y no de clases generadas. Se mitiga con pruebas que validan contra el XSD y comprueban el orden de la cabecera (`tests/test_generador_xml.py`).
