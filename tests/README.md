# tests/

Pruebas de los **contratos** de la fase Spec-First. No prueban lógica de negocio (todavía no existe código en `src/`).

`test_contratos.py` comprueba que:

- `specs/contracts/facturacion-api.yaml` es OpenAPI 3.1 válido y todos sus `$ref` existen;
- cada operación tiene `operationId` único y cita su caso de uso (`CU-xx`), y los 16 casos de uso tienen al menos un endpoint;
- los ejemplos de las peticiones cumplen su schema y el contrato modela el Catálogo 08 (`sistema_isc`);
- cada feature tiene `spec.md`, `design.md` y `tasks.md`, y no hay enlaces rotos en los `.md`;
- `src/` sigue vacío.

```bash
pip install -r requirements.txt
pytest -v
```
