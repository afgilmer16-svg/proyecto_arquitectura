# src — Código fuente

**Autor**: Gilmer Ayala Falcón · UNAS-FIIS · Arquitectura de Software · Grupo A

> La implementación principal del generador (Java 21 + JAXB, ADR-006) está en [`../generador-java/`](../generador-java/). Esta carpeta conserva el **prototipo en Python** (ADR-005), usado como referencia: la regresión cruzada comprueba que ambos producen el mismo XML.

Según las reglas **Spec-First** del proyecto (`.agent/rules/spec-first.md`), aquí solo hay código de features **aprobadas**.

| Paquete | Feature | Estado | Descripción |
|---|---|---|---|
| [`facturacion_xml/`](facturacion_xml/) | [0008](../specs/features/0008-generacion-xml-ubl/spec.md) | APROBADO · implementado (segundo parcial) | Generador del XML `Invoice` UBL 2.1 (factura y boleta), validado contra el XSD oficial. Python + lxml (ADR-005). |

## Uso

```powershell
pip install -r requirements.txt
$env:PYTHONPATH = "src"
python -m facturacion_xml docs\ejemplos\factura-abarrotes.json salida\F001-00000001.xml --correlativo 1
```

Con firma digital opcional (el `.pfx` nunca se versiona; la contraseña va en una variable de entorno):

```powershell
$env:FIRMA_CLAVE = "contraseña-del-pfx"
python -m facturacion_xml docs\ejemplos\factura-abarrotes.json salida\F001-00000001.xml --correlativo 1 --firmar certificado.pfx
```

Desde Python:

```python
from facturacion_xml import generar_xml_factura

xml = generar_xml_factura(datos, correlativo=1)   # bytes UTF-8, ya validados contra el XSD
```

Documentación técnica: [`docs/documentacion-tecnica-xml.md`](../docs/documentacion-tecnica-xml.md).

## Estado de las demás specs (primer parcial)

| Artefacto | Estado |
|-----------|--------|
| `specs/features/0001` a `0007` | EN REVISIÓN: completas, con datos marcados "Por verificar" en `docs/pendientes-verificacion.md` |

Lo que corresponde a parciales posteriores (base de datos, API REST, interfaz de usuario, despliegue) **no** se implementa aquí todavía.
