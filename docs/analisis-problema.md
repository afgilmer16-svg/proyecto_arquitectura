# Análisis del Problema

**Autor**: Gilmer Ayala Falcón · UNAS-FIIS · Arquitectura de Software · Grupo A

**Estado**: EN REVISIÓN  
**Fecha**: 2026-11-11  
**Fuentes**: RS 097-2012/SUNAT (F-03), RS 234-2006/SUNAT (F-04), Reglamento de Comprobantes de Pago (F-06), UBL 2.1 (F-01)

---

## Problema central

ABC Software Corp, empresa de abarrotes, factura en papel o con un sistema antiguo sin conexión con SUNAT. No valida el RUC del cliente, no calcula de forma confiable IGV, ISC ni ICBPER, y no genera XML UBL 2.1 firmado ni recibe el CDR. Esto la expone a sanciones y a la pérdida del derecho a deducir gastos de sus clientes. Detalle de brechas: [`docs/caso.md`](caso.md).

Hace falta un sistema de facturación electrónica que:

1. emita facturas, boletas, notas de crédito y de débito en XML UBL 2.1 firmado (XMLDSig, X.509);
2. las envíe a SUNAT mediante un OSE (con contingencia directa) y almacene el CDR;
3. gestione bajas y resúmenes diarios;
4. siga operando en caja ante cortes de red o caídas de SUNAT;
5. conserve los comprobantes por el período legal;
6. incorpore el historial del sistema antiguo sin reemitirlo;
7. se use desde Windows (cajas) y Android (preventa y reparto).

## Situación actual vs. situación deseada

| Aspecto | AS-IS (sistema actual) | TO-BE (sistema nuevo) |
|---|---|---|
| Registro | Fichas de almacén con `fecha`, `ingreso`, `salida`, `producto`, `nro_documento` informal | Comprobantes electrónicos y kardex formal, con trazabilidad al registro antiguo |
| Cliente | No se valida el RUC | RUC con módulo 11 y consulta de estado (CU-09) |
| Impuestos | No calcula IGV, ISC ni ICBPER | El dominio calcula IGV, ISC según el Catálogo 08 e ICBPER |
| Documento | Número interno sin formato | Serie `F###`/`B###` y correlativo sin huecos; XML UBL 2.1 firmado |
| Envío | No hay comunicación con SUNAT | Envío al OSE (contingencia: SUNAT directo) y recepción del CDR |
| Arquitectura | Software operativo básico | Monolito MVC desacoplado por puertos, listo para Hexagonal |
| Plataformas | — | Windows (MSI) y Android (APK) |

## Marco normativo y su impacto

| Norma / estándar | Qué regula | Impacto en el sistema | Dónde se especifica |
|---|---|---|---|
| RS 097-2012/SUNAT (F-03) | Sistema de emisión electrónica desde los sistemas del contribuyente | Emitir factura, boleta y notas en XML firmado; enviarlas y guardar el CDR; bajas y resúmenes diarios | 0001, 0003, 0004 |
| RS 234-2006/SUNAT (F-04) | Libros y registros vinculados a asuntos tributarios | Las fichas de almacén deben convertirse en el Registro de Inventario Permanente (kardex) y las ventas en el Registro de Ventas | 0005 §13–14, 0006 |
| Reglamento de Comprobantes de Pago (F-06) | Requisitos de facturas y boletas | Datos obligatorios del comprobante e identificación del comprador en boletas | 0002 |
| Catálogos SUNAT (F-02) | Códigos oficiales: 01, 02, 03, 05, 06, 07, **08**, 09, 10, 12, 16, 51 | Enums del contrato y validaciones del dominio | 0004, 0005 §15 |
| Catálogo 08 | Sistema de cálculo del ISC: `01` al valor, `02` específico, `03` al valor según precio de venta al público | Cada sistema usa una fórmula y elementos UBL distintos; aplica a licores, cervezas y bebidas azucaradas del negocio | 0002 §5, 0005 §9 y `design.md` |
| OASIS UBL 2.1 (F-01) | Estructura del XML | `UBL-Invoice-2.1.xsd` define las rutas de la matriz | 0005 §17 |

## Stakeholders

| Stakeholder | Interés principal |
|---|---|
| Dirección / representante legal de ABC | Cumplir la norma, evitar sanciones, costo controlado. |
| Cajeros y vendedores | Emitir rápido (< 500 ms por comprobante en caja) y seguir vendiendo sin conexión. |
| Almacenero | Catálogo de productos y stock consistentes. |
| Contador / administrador | Series, bajas, resúmenes, registros de ventas e inventario. |
| Clientes (adquirentes) | Recibir comprobantes válidos (XML, PDF, CDR). |
| OSE | Recibir paquetes válidos y devolver CDR. |
| SUNAT | Recibir comprobantes conformes a la norma. |
| Equipo de desarrollo (Grupo A) | Especificaciones claras, trazables y aprobadas antes de programar. |
| Docente / evaluador | Rigor Spec-First y trazabilidad normativa. |

## Restricciones

- **Normativas**: RS 097-2012/SUNAT, RS 234-2006/SUNAT, OASIS UBL 2.1 y Catálogos del Anexo 8.
- **Metodología**: Spec-First. No hay código de producción hasta aprobar las especificaciones (`.agent/rules/spec-first.md`).
- **Idioma**: español, salvo identificadores técnicos y fragmentos normativos.
- **Seguridad**: las claves privadas y certificados no se guardan en el repositorio.
- **Técnicas**: monolito modular con puertos y adaptadores (ADR-001); OSE como canal principal (ADR-002); Kotlin Multiplatform + Compose para MSI y APK (ADR-003).
- **Equipo y plazo**: equipo reducido y plazo académico acotado.
- **Decimales**: importes con decimal exacto; nunca punto flotante.

## Supuestos

1. ABC tiene RUC activo y habido, y régimen que obliga a emitir comprobantes electrónicos.
2. ABC obtendrá un certificado digital X.509 vigente y un contrato con un OSE.
3. Habrá acceso a internet intermitente en las cajas; el modo sin conexión es indispensable.
4. El sistema antiguo conserva al menos `fecha`, `ingreso`, `salida`, `producto` y `nro_documento`.
5. Los catálogos SUNAT se tratan como datos versionados por fecha de vigencia.
6. Los valores tributarios (IGV 18 %, ICBPER por año) se parametrizan; no se fijan en el dominio.

> ⚠️ Por verificar: régimen tributario de ABC y plazo legal de conservación de comprobantes [F-03, F-04].

## Riesgos identificados

| ID | Riesgo | Probabilidad | Impacto | Mitigación |
|---|---|---|---|---|
| R-01 | Caída o lentitud de SUNAT/OSE bloquea las cajas. | Alta | Alto | Emisión local y envío asíncrono; contingencia directa a SUNAT (ADR-002). |
| R-02 | Valores normativos desactualizados (p. ej. ICBPER, catálogos). | Media | Alto | Parametrizar por fecha en `CatalogoSunatPort`; revisar catálogos periódicamente. |
| R-03 | Robo o fuga de la clave privada del certificado. | Baja | Crítico | Almacén protegido/HSM; la firma no se hace en dispositivos móviles. |
| R-04 | Rechazos por XML mal formado o cálculo que no cuadra. | Media | Medio | Validación contra XSD y reglas fiscales antes del envío; matriz de trazabilidad. |
| R-05 | Duplicación de comprobantes al sincronizar sin conexión. | Media | Alto | UUID idempotente, restricciones únicas y bloqueo por serie. |
| R-06 | Datos del sistema antiguo inconsistentes o sin diccionario. | Alta | Medio | Perfilado, cuarentena, conciliación y auditoría (`specs/features/0006-migracion-legado/`). |
| R-07 | Reglas de negocio filtradas a la UI. | Media | Medio | Pruebas de arquitectura y revisión (ADR-003). |
| R-08 | Fuentes normativas locales ausentes (XSD y catálogos). | Alta | Medio | Incorporarlas a `docs/referencias/` antes de aprobar las matrices de trazabilidad. |
| R-09 | Pérdida de correlativos o huecos de numeración. | Baja | Alto | Asignación atómica en servidor y registro de auditoría. |
