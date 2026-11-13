# schemas/

**Autor**: Gilmer Ayala Falcón · UNAS-FIIS · Arquitectura de Software · Grupo A

Los DTO del sistema (entrada y salida de la API) están definidos **dentro del contrato** [`../facturacion-api.yaml`](../facturacion-api.yaml), en `components/schemas`, como en el contrato de ejemplo del curso. Así existe una sola fuente de verdad y los `$ref` no se duplican.

Esta carpeta queda reservada para esquemas externos que el contrato necesite en el futuro (por ejemplo, los XSD propios de SUNAT para `VoidedDocuments` y `SummaryDocuments`). Los XSD oficiales de UBL 2.1 están en `docs/referencias/ubl/` y no se copian aquí.
