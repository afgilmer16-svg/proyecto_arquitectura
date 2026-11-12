# tests/

> Estas son las pruebas del **prototipo en Python** y de los contratos. Las pruebas de la implementación principal (Java + JAXB) están en [`generador-java/src/test/java`](../generador-java/src/test/java/pe/abc/facturacion/) y se ejecutan con `cd generador-java && ./mvnw verify` (resultados: [`resultados-pruebas-java.txt`](../docs/resultados-pruebas-java.txt)).

| Archivo | Qué prueba |
|---|---|
| `test_generador_xml.py` | **Pruebas unitarias de generación** (segundo parcial, 89 pruebas): que el XML se genere y sea válido contra `UBL-Invoice-2.1.xsd`; estructura y orden; datos en su etiqueta; categorías 1001–1004; ISC e ICBPER; formatos y UTF-8; datos faltantes o inválidos; y que el XSD detecte XML alterados. |
| `test_contratos.py` | Contratos de la fase Spec-First: OpenAPI 3.1 válido, `$ref` existentes, ejemplos conformes, Catálogo 08, cada feature con `spec`/`design`/`tasks`, enlaces sin romper y que `src/` solo tenga el generador autorizado. |
| `test_verificacion_integral.py` | **Verificación integral** (310 pruebas): coherencia entre el contrato OpenAPI y el código, 300 comprobantes aleatorios recalculados con un oráculo independiente, regresión del ejemplo versionado y trazabilidad con la matriz 0005. |
| `mutacion.py` | **Pruebas de mutación** (no las recoge pytest): introduce 16 errores en el generador y comprueba que la suite los detecta. `python tests/mutacion.py` |
| `test_esquemas_y_ejemplos.py` | **Control de forma**: el JSON Schema `specs/schemas/factura-entrada.schema.json` es válido; 5 ejemplos válidos generan XML válido contra el XSD; 12 ejemplos inválidos (`specs/ejemplos/invalidos`) son rechazados con el motivo esperado. |
| `test_arquitectura.py` | **Reglas de arquitectura** (equivalente a ArchUnit): el núcleo no depende de infraestructura, solo los adaptadores usan `lxml`, sin ciclos de carga y sin `float` en los importes. |
| `test_herramientas_y_derivados.py` | Los 18 diagramas `.mmd` y los CSV de la matriz 0005 coinciden con sus fuentes en Markdown. |
| `conftest.py` | Hace importable `src/facturacion_xml` para pytest. |

```bash
pip install -r requirements.txt
pytest -v
pytest --cov=facturacion_xml   # exige 90 % (.coveragerc); hoy 95 %
```

Resultados de la última ejecución: [`docs/resultados-pruebas-python.txt`](../docs/resultados-pruebas-python.txt).
