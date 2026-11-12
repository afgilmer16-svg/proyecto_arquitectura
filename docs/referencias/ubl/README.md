# Referencias — UBL 2.1

Carpeta de **solo lectura** con los esquemas oficiales de OASIS. No se modifican: el generador (`src/facturacion_xml/validador_xsd.py`) los usa tal cual para validar cada XML.

## Procedencia y versión

| Dato | Valor |
|---|---|
| Estándar | OASIS Universal Business Language (UBL) **2.1 OS** (*OASIS Standard*) |
| Fecha de publicación | 04 de noviembre de 2013 |
| Fuente oficial | <https://docs.oasis-open.org/ubl/os-UBL-2.1/xsd/> (`maindoc/` y `common/`) |
| Documento raíz | `UBL-Invoice-2.1.xsd` |

En la distribución oficial, `UBL-Invoice-2.1.xsd` está en `xsd/maindoc/` e importa `../common/*.xsd`. Aquí todos están en una sola carpeta; el validador resuelve cada `schemaLocation` al archivo local del mismo nombre.

## Archivos

| Archivo | Rol | Incorporado | SHA-256 (finales de línea LF) |
|---|---|---|---|
| `UBL-Invoice-2.1.xsd` | Documento principal (factura y boleta) | 2026-11-06 | `40fae8cb436f3a9506d7acce65ba162caef3b0bed4d5cbc0992b2153ded4edf4` |
| `UBL-CommonAggregateComponents-2.1.xsd` | Componentes agregados (cac:) | 2026-11-06 | `939172ad8dd057cd403e7f763f6532184dd5ed4b9de24c42ebb35db4792ba613` |
| `UBL-CommonBasicComponents-2.1.xsd` | Componentes básicos (cbc:) | 2026-11-06 | `bd4ad043ee1d9da1c7f8018dabf739cfafdfb59143d0d16b9ef769e6b7c408a7` |
| `UBL-CommonExtensionComponents-2.1.xsd` | Extensiones (ext:, firma SUNAT) | 2026-11-06 | `ad7a4e490978adfbcfc5ec0bb20941cf11ac960ccf0c4de8791a7c731a8dbe87` |
| `UBL-ExtensionContentDataType-2.1.xsd` | Contenido de extensiones | 2026-11-06 | `fcee77a11870208e6377ea6311b9f2a050bca24bdad8606ea02d71e9f9e72f8d` |
| `UBL-CommonSignatureComponents-2.1.xsd` | Componentes de firma | 2026-11-06 | `3db472305f029bba5c1ae157bfd0178f715c3f9b94bd8e6c557dbce5e88da874` |
| `UBL-SignatureAggregateComponents-2.1.xsd` | Firma: agregados | 2026-11-06 | `17bb6b62d709b4fd81449a37655af36aa6a1276ad4fdb1b2e249a5ed4b7c2172` |
| `UBL-SignatureBasicComponents-2.1.xsd` | Firma: básicos | 2026-11-06 | `cef924d7ba3d1d8ade14469325cde1364f8c174e46f0198ec02da8e9e748a489` |
| `UBL-QualifiedDataTypes-2.1.xsd` | Tipos de dato calificados | 2026-11-06 | `7dcb156e610239c97ae70940cf4653b88e48c3595bf5f56a2204a32e2893e6cf` |
| `UBL-UnqualifiedDataTypes-2.1.xsd` | Tipos de dato no calificados | 2026-11-06 | `09052d406b4293e2a5f9c2bfee6df10ad4d8d5f0b36e24a6349d7f7936d89eb6` |
| `CCTS_CCT_SchemaModule-2.1.xsd` | Tipos base CCTS | 2026-11-06 | `dd546e4809df86b6445589f69f0d6c9df162840ae386574ddfc1da7638103e15` |
| `UBL-xmldsig-core-schema-2.1.xsd` | XML Digital Signature (W3C) | 2026-11-11 ¹ | `101909c9f06456d61ddcc4fb982f1d40dc357b439f393b1a2eb46e42acd60809` |
| `UBL-XAdESv132-2.1.xsd` | Firma avanzada XAdES (ETSI) | 2026-11-11 ¹ | `a4f726bcf8cc3f7d9ffa4dab99e005535a8e8b60dced1e5d94578d2e05afa96e` |
| `UBL-XAdESv141-2.1.xsd` | Firma avanzada XAdES (ETSI) | 2026-11-11 ¹ | `1fa4625e9cefcb7a9abb5ac1b64315547450031eece8a55bd584e4ba4b79dbc1` |
| `UBL-CreditNote-2.1.xsd` | Nota de crédito (fuera del alcance del generador) | 2026-11-06 | `a54651b1225052f811bf2ba01346f13f2454e7cc3e0be290c91dd680dc7b7b1a` |
| `UBL-DebitNote-2.1.xsd` | Nota de débito (fuera del alcance del generador) | 2026-11-06 | `295d142102a2a0cd8b223a79b8314c0fe1b55055478221dd2f0ed7603fecf920` |

¹ **Agregados el 2026-11-11.** Sin estos tres archivos, `UBL-Invoice-2.1.xsd` no compila (`'{http://www.w3.org/2000/09/xmldsig#}Signature' does not resolve to a(n) element declaration`). Son las copias de la versión **OS** (su cabecera indica *UBL 2.1 OS, Release Date: 04 November 2013*) tomadas del repositorio público `thegreenter/ubl-validator` (`src/xsd/2.1/common/`), cuyos otros 10 archivos comunes son idénticos byte a byte a los de esta carpeta. Para confirmar que coinciden con OASIS, compara los hashes con los descargados de `https://docs.oasis-open.org/ubl/os-UBL-2.1/xsd/common/` (comando en `docs/segundo-parcial/02-documentacion-tecnica.md`, §6.1).

> ⚠️ No modificar estos archivos.
