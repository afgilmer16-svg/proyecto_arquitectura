# spec.md — 0007 Distribución Multiplataforma

**Feature**: Distribución Multiplataforma  
**ID**: 0007  
**Estado**: EN REVISIÓN  
**Autor**: Equipo Grupo A — UNAS-FIIS  
**Fecha**: 2026-11-06  
**Fuentes**: RS 097-2012/SUNAT (F-03), ADR-003

---

## 1. Descripción

El enunciado exige una interfaz multiplataforma para el personal operativo. Esta feature fija los requisitos de distribución: aplicación de escritorio **Windows 10/11 x64 (MSI)** para cajas y aplicación **Android (APK)** para preventa y reparto, ambas con operación sin conexión.

Decisión de tecnología: [`specs/adr/0003-kotlin-multiplatform-compose.md`](../../adr/0003-kotlin-multiplatform-compose.md) (Kotlin Multiplatform + Compose Multiplatform). Estrategia detallada de empaquetado, firma y modo sin conexión: [`specs/features/0007-distribucion-multiplataforma/design.md`](design.md).

## 2. Alcance

**Dentro del alcance**

- Instalador MSI firmado (Authenticode) para Windows y APK firmado para Android.
- Operación sin conexión con cola local idempotente y sincronización posterior.
- Reglas de arquitectura que mantienen la UI como adaptador de entrada.
- Pipeline de compilación, firma, verificación y publicación.

**Fuera de alcance**

- Aplicaciones para macOS, Linux o iOS.
- Publicación en Google Play (se documenta la opción AAB, pero no es requisito de esta fase).
- Backoffice web (su tecnología queda por definir).

## 3. Requisitos

| ID | Requisito |
|---|---|
| RF-DIS-01 | Se entrega un `.msi` para Windows 10/11 x64, firmado con Authenticode y sello de tiempo. |
| RF-DIS-02 | Se entrega un `.apk` firmado con keystore de release; se verifica con `apksigner`. |
| RF-DIS-03 | La UI solo captura datos y llama puertos de entrada. No calcula tributos, no construye UBL, no firma, no asigna correlativos y no llama directamente a OSE/SUNAT. |
| RF-DIS-04 | Sin conexión, toda operación se registra localmente con UUID idempotente y estado `PENDIENTE`, `ENVIANDO`, `CONFIRMADO` o `ERROR`. La UI distingue "registrado localmente" de "aceptado por SUNAT". |
| RF-DIS-05 | La sincronización respeta el orden por serie/correlativo, reintenta con espera exponencial y evita duplicados. |
| RF-DIS-06 | Las claves privadas de firma fiscal no se copian a dispositivos móviles. Firma y envío permanecen en el servicio central u OSE. |
| RF-DIS-07 | Certificados, keystores y contraseñas viven en el almacén de secretos del CI; nunca en Git (`.gitignore` ya excluye `.pfx`, `.p12`, `.jks`, `.keystore`). |
| RF-DIS-08 | Cada versión se publica con hash SHA-256, número de versión y notas de cambio; el MSI debe permitir actualizar conservando datos. |

## 4. Criterios de aceptación

- [x] Existe una comparación de al menos tres alternativas (.NET MAUI, Flutter, KMP + Compose) con criterio explícito de MSI, APK y firma.
- [x] Hay un procedimiento reproducible para generar y firmar el MSI y el APK.
- [x] El modo sin conexión está descrito con estados, orden de sincronización y manejo de duplicados.
- [ ] Una prueba de arquitectura impide que `domain` y `application` dependan de Compose, Android o AWT/Swing.
- [ ] El MSI se instala, actualiza (sin perder datos) y desinstala en una VM limpia.
- [ ] El APK se instala y actualiza en dispositivos representativos, con datos sin conexión intactos.

## 5. Notas

> `minSdk` 26 (decisión propuesta; el mínimo técnico de Compose Multiplatform es API 21). ⚠️ Por verificar: confirmarlo con los dispositivos reales de preventa y definir los modelos de impresora térmica de las cajas.

> Versiones fijadas para el primer release (verificadas el 2026-11-07): Kotlin 2.4.21, Compose Multiplatform 1.12.2 y JDK 21 (el mínimo para `jpackage` es 17). Ver `specs/features/0007-distribucion-multiplataforma/design.md`. ⚠️ Por verificar: versiones de Gradle y del plugin de Android al crear el proyecto.

> ⚠️ Por verificar: proveedor y costo del certificado de firma de código para Windows.

> ⚠️ Por verificar: las actualizaciones de las dependencias citadas en ADR-003 (enlaces a documentación oficial) deben revisarse al implementar.
