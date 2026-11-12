# src — Código fuente

Según las reglas **Spec-First** del proyecto (`.agent/rules/spec-first.md`), aquí solo hay código de features **aprobadas**.

| Paquete | Feature | Estado | Descripción |
|---|---|---|---|
| [`facturacion_xml/`](facturacion_xml/) | [0008](../specs/features/0008-generacion-xml-ubl/spec.md) | APROBADO · implementado (segundo parcial) | Generador del XML `Invoice` UBL 2.1 (factura y boleta), validado contra el XSD oficial. Python + lxml (ADR-005). |

## Uso

```powershell
pip install -r requirements.txt
$env:PYTHONPATH = "src"
python -m facturacion_xml docs\segundo-parcial\ejemplos\factura-abarrotes.json salida\F001-00000001.xml --correlativo 1
```

Desde Python:

```python
from facturacion_xml import generar_xml_factura

xml = generar_xml_factura(datos, correlativo=1)   # bytes UTF-8, ya validados contra el XSD
```

Documentación técnica: [`docs/segundo-parcial/02-documentacion-tecnica.md`](../docs/segundo-parcial/02-documentacion-tecnica.md).

## Estado de las demás specs (primer parcial)

| Artefacto | Estado |
|-----------|--------|
| `specs/features/0001` a `0007` | EN REVISIÓN: completas, con datos marcados "Por verificar" en `docs/pendientes-verificacion.md` |

Lo que corresponde a parciales posteriores (base de datos, API REST, interfaz de usuario, despliegue) **no** se implementa aquí todavía.
