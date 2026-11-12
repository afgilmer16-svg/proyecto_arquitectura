# tests/

| Archivo | Qué prueba |
|---|---|
| `test_generador_xml.py` | **Pruebas unitarias de generación** (segundo parcial, 89 pruebas): que el XML se genere y sea válido contra `UBL-Invoice-2.1.xsd`; estructura y orden; datos en su etiqueta; categorías 1001–1004; ISC e ICBPER; formatos y UTF-8; datos faltantes o inválidos; y que el XSD detecte XML alterados. |
| `test_contratos.py` | Contratos de la fase Spec-First: OpenAPI 3.1 válido, `$ref` existentes, ejemplos conformes, Catálogo 08, cada feature con `spec`/`design`/`tasks`, enlaces sin romper y que `src/` solo tenga el generador autorizado. |
| `test_verificacion_integral.py` | **Verificación integral** (310 pruebas): coherencia entre el contrato OpenAPI y el código, 300 comprobantes aleatorios recalculados con un oráculo independiente, regresión del ejemplo versionado y trazabilidad con la matriz 0005. |
| `mutacion.py` | **Pruebas de mutación** (no las recoge pytest): introduce 16 errores en el generador y comprueba que la suite los detecta. `python tests/mutacion.py` |
| `conftest.py` | Hace importable `src/facturacion_xml` para pytest. |

```bash
pip install -r requirements.txt
pytest -v
```

Resultados de la última ejecución: [`docs/segundo-parcial/resultados-pruebas.txt`](../docs/segundo-parcial/resultados-pruebas.txt).
