# Diccionario del sistema legado (supuestos de trabajo)

**Estado:** BORRADOR — todo lo de este documento es **supuesto** hasta confirmarlo con una muestra real del sistema antiguo.  
**Fecha:** 2026-11-07  
**Fuente:** descripción del caso en `docs/enunciado.md`: el software actual de ABC Software Corp controla *"fichas sencillas con campos como fecha, ingreso, salida, producto y un número de documento interno bastante informal"*.

> ⚠️ Por verificar: no se dispone del DDL, de un archivo de exportación ni de una muestra anonimizada. Cada fila de este diccionario es un supuesto (`S-xx`) que debe confirmarse en el perfilado (tarea T-0006-01) o reemplazarse por el dato real. Ningún supuesto debe usarse para corregir datos: solo para diseñar el mapeo y las validaciones.

## Entidad de origen supuesta

Una única "ficha de movimiento de almacén" por fila. El sistema antiguo no distingue entre productos, movimientos y comprobantes: todo viene en la misma fila.

| # | Campo | Tipo supuesto | Nulo | Formato / dominio supuesto | Significado supuesto | Id. |
|---|---|---|---|---|---|---|
| 1 | `fecha` | Texto o fecha | No | Uno o más de: `dd/MM/yyyy`, `yyyy-MM-dd`, `dd-MM-yyyy`; posible hora. Zona horaria no registrada (se asume `America/Lima`). | Fecha del movimiento de almacén. | S-01 |
| 2 | `ingreso` | Texto numérico | Sí | Entero o decimal; separador decimal `.` o `,`; sin unidad de medida explícita. | Cantidad que entra al almacén. Vacío o `0` si la ficha es una salida. | S-02 |
| 3 | `salida` | Texto numérico | Sí | Igual que `ingreso`. | Cantidad que sale del almacén. Vacío o `0` si la ficha es un ingreso. | S-03 |
| 4 | `producto` | Texto | No | Código alfanumérico corto, o descripción libre; puede traer espacios y mayúsculas/minúsculas mezcladas; puede tener ceros iniciales. | Identifica el producto. No se sabe si es código, descripción o ambos. | S-04 |
| 5 | `nro_documento` | Texto | Sí | Formato informal: a veces `SERIE-NUMERO` (p. ej. `F001-42`), a veces solo un número, a veces texto libre. Sin validación en origen. | Número de documento interno que respalda el movimiento. No es necesariamente un comprobante de pago. | S-05 |

### Supuestos transversales

| Id. | Supuesto |
|---|---|
| S-06 | No existe clave primaria física confiable; se construye `legado_id` como `sistema:tabla:clave` o, si no hay clave, `SHA-256(archivo + número de línea + contenido canónico)` (ver mapeo). |
| S-07 | No hay maestro de productos separado: el catálogo de productos se deduce de los valores distintos de `producto`. Si existe un maestro, se usa como fuente de `Producto.descripcion`. |
| S-08 | Una fila representa un solo sentido de movimiento (`INGRESO` o `SALIDA`); filas con ambos valores positivos son errores (`MOVIMIENTO_DOBLE`). |
| S-09 | La fuente se entrega como CSV, DBF o base de datos (consistente con `formato_origen` del contrato `MigracionLegadoRequest`). |
| S-10 | La codificación de caracteres es desconocida; se detecta antes de convertir a UTF-8 (ver mapeo, normalización). |
| S-11 | No hay precios, impuestos ni clientes en la ficha; por tanto, los comprobantes migrados solo conservan referencia histórica (`numeroDocumentoOriginal`), sin importes tributarios. |

## Cabecera y filas de ejemplo (ilustrativas, no son datos reales)

```csv
fecha,ingreso,salida,producto,nro_documento
2026-10-01,10,,A1,F001-42
05/06/2026,,3,a1 ,
2026-10-02,"1,5",,000123,GR-0007
```

Cada fila cubre un caso del mapeo: formato de fecha distinto, decimal con coma, producto con espacios y ceros iniciales, documento ausente o con prefijo distinto de `F`/`B`.

## Preguntas para confirmar los supuestos

1. ¿Existe una exportación (CSV/DBF/BD) o un DDL del sistema antiguo? ¿Se puede obtener una muestra anonimizada?
2. ¿Qué formatos de fecha usa la ficha y se guarda la hora?
3. ¿Cuál es la unidad de medida de `ingreso` y `salida`?
4. ¿`producto` es un código, una descripción o ambos? ¿Hay un maestro de productos?
5. ¿Qué significa `nro_documento`: factura, boleta, guía interna u otro? ¿Hay un patrón o es libre?
6. ¿Desde qué fecha se debe migrar el historial? (T: periodo mínimo de historia, abierta)
7. ¿Qué política contable se aplica a movimientos sin documento de origen (`SIN_DOCUMENTO_ORIGEN`)?

## Trazabilidad

| Supuesto | Dónde se usa |
|---|---|
| S-01 | Mapeo de `fecha` → `fechaMovimiento` / `fechaEmision`; códigos `FECHA_*`; pruebas T-MIG-01, T-MIG-02 |
| S-02, S-03 | Mapeo de `ingreso` / `salida`; códigos `CANTIDAD_*`, `MOVIMIENTO_*`; T-MIG-03 a T-MIG-05 |
| S-04 | Mapeo de `producto`; códigos `PRODUCTO_*`; T-MIG-06, T-MIG-12 |
| S-05 | Mapeo de `nro_documento`; códigos `DOCUMENTO_*`, `SIN_DOCUMENTO_ORIGEN`; T-MIG-08, T-MIG-09 |
| S-06 | `COLISION_LEGADO_ID`, auditoría `migracion_trazabilidad`; T-MIG-07 |
