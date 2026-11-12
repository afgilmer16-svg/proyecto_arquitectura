# Retroalimentación del docente — estado de cada observación

**Fuente**: observaciones del docente sobre la versión anterior del proyecto (nota 50/100).  
**Revisado**: 2026-11-11

Cada observación se contrastó contra el proyecto actual. ✅ = resuelto, con el archivo donde se puede comprobar; ⏳ = depende de una acción pendiente.

## Entregable 1 — Modelo de dominio y DTO (antes 6/25)

| Observación | Estado | Dónde comprobarlo |
|---|---|---|
| XML generado con plantillas Freemarker | ✅ Único adaptador: `GeneradorUblJaxbAdapter` (Java 21), con **clases generadas por JAXB (xjc) desde `UBL-Invoice-2.1.xsd`** en cada compilación, sin plantillas y validado contra el XSD antes de entregar (ADR-0006). Se conserva un prototipo en Python como referencia (ADR-0005) | `servidor/src/main/java/pe/abc/facturacion/infraestructura/ubl/`, `servidor/src/test/` |
| `unidad_medida` como texto libre | ✅ `UnidadMedidaEnum` (Catálogo 03) en ítem y producto | `specs/contracts/facturacion-api.yaml` |
| Patrón `^[F\|B]…` acepta `\|001` | ✅ `^[FB][A-Z0-9]{3}$` | `facturacion-api.yaml` |
| El cliente envía el correlativo | ✅ Lo asigna el servidor; `PUT /series/{id}` no permite cambiarlo. También el ID de baja y resumen (`RA-`/`RC-`) | `facturacion-api.yaml`, `0004/spec.md` |
| No se valida 01 ↔ F y 03 ↔ B | ✅ Regla del value object `SerieCorrelativo` y respuesta `422` | `0002/spec.md` §2 |
| Falta el ISC (Catálogo 08) | ✅ En ítem **y** producto: `sistema_isc`, `tasa_isc`, `monto_fijo_isc`, `unidad_base_isc`, `precio_venta_publico_sugerido`, con validación condicional; se calcula antes que el IGV | `0002/spec.md` §5, `0005/design.md`, `facturacion-api.yaml` |
| ICBPER S/ 0.20 | ✅ S/ 0.50 desde 2023, parametrizado por fecha | `0002/spec.md` §2 y §5.5 |
| Importes `number`/`double` | ✅ Texto decimal exacto; no queda ningún `type: number` | `facturacion-api.yaml` |
| El cliente envía impuestos y totales | ✅ Los calcula el dominio; solo vienen en la respuesta (`ItemCalculadoDto`) | `facturacion-api.yaml`, `0002/design.md` |

## Entregable 2 — Arquitectura (antes 23/25)

| Observación | Estado | Dónde comprobarlo |
|---|---|---|
| El adaptador de plantillas contradice el “dominio intacto” | ✅ Sustituido por `LxmlUblAdapter` (ADR-0005) | `0003/design.md` |
| La migración pasa por la emisión | ✅ `MigrarLegadoService` no invoca `EmitirComprobanteUseCase`; lo migrado queda `origen = LEGADO` y nunca va al OSE | `0006/spec.md` RF-MIG-01, `0003/design.md` |

## Entregable 3 — Contratos API (antes 19/25)

| Observación | Estado | Dónde comprobarlo |
|---|---|---|
| Sin `securitySchemes`, `401`, `403` | ✅ `bearerAuth` (JWT) global; `401` y `403` en las 33 operaciones | `facturacion-api.yaml` |
| `nullable` (no válido en 3.1) | ✅ 0 usos | `facturacion-api.yaml` |
| Espejo `facturacion-api.yaml` con `$ref` inválido | ✅ Hay un solo archivo, sin espejo | `specs/contracts/` |
| Baja y resumen con `201` | ✅ `202` con ticket | `facturacion-api.yaml` |
| Serie inexistente → `404` | ✅ `422` | `POST /comprobantes` |
| Listados con solo `200` | ✅ Declaran también `400`, `401`, `403` | `facturacion-api.yaml` |
| “Validado sin advertencias” sin haberlo corrido | ✅ Corregido: el criterio dice qué se validó y qué falta | `0004/spec.md` §3 |
| Correr `openapi-spec-validator` y `redocly lint` | ⏳ Ejecutar `pytest -v` y `npx @redocly/cli lint specs/contracts/facturacion-api.yaml` | `tests/test_contratos.py` |

## Entregable 4 — Matriz de trazabilidad (antes 2/25)

| Observación | Estado | Dónde comprobarlo |
|---|---|---|
| Matriz no entregada | ✅ Matriz por bloques con dato SUNAT, catálogo, XPath con atributos (`listAgencyName`, `listURI`, `listID`, `schemeID`, `unitCode`), campo DTO y regla | `0005/spec.md` §3–12 |
| Cabecera, emisor, receptor, forma de pago, `TaxTotal`/`TaxSubtotal`, `LegalMonetaryTotal` | ✅ | `0005/spec.md` §3–12 |
| Por línea: `InvoicedQuantity@unitCode`, `PricingReference`, `TaxExemptionReasonCode`, `TierRange`, ICBPER con `PerUnitAmount` | ✅ | `0005/spec.md` §7–10 |
| Falta el formato 14.1 | ✅ Registro de Ventas desde comprobantes aceptados, con `LibroElectronicoPort` y adaptador PLE (o SIRE/RVIE, por verificar) | `0005/spec.md` §13, `0003/design.md` |
| Falta el formato 12.1 | ✅ Inventario permanente desde `MovimientoAlmacen`: establecimiento, tablas 5, 10 y 12, entradas, salidas, saldo y equivalencia Cat. 03 ↔ tabla 6 | `0005/spec.md` §14 y §14.1 |

## Errores normativos menores

| Observación | Estado | Dónde comprobarlo |
|---|---|---|
| CU-01: rechazo “2000 a 4000” | ✅ Rechazo `2000`–`3999`; `4000+` son observaciones (aceptado) | `0001/spec.md` CU-01 |
| CU-07: motivo de baja del Catálogo 09 | ✅ Texto libre (el Catálogo 09 es de notas de crédito) | `0001/spec.md` CU-07 |
| Migración con `COMPROBANTES_HISTORICOS` | ✅ No existe; la migración no emite | `0006/` |
| Estados inconsistentes y carpetas inexistentes en `src/README.md` | ✅ Todo `EN REVISIÓN` y rutas reales | `src/README.md` |
| `0005`, `0006`, `0007` y `analisis-problema.md` vacíos | ✅ Completos | `specs/features/`, `docs/` |
