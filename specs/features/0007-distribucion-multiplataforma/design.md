# design.md — 0007 Distribución multiplataforma (MSI / APK)

> Restricción **Distribución y multiplataforma** del enunciado. Decisión en [ADR-003](../../adr/0003-kotlin-multiplatform-compose.md). Complementa a [`spec.md`](spec.md).


**Estado:** EN REVISIÓN  
**Fecha:** 2026-10-22  
**Plataformas objetivo:** Windows 10/11 x64 y Android.  
**Decisión:** Kotlin Multiplatform + Compose Multiplatform.

## Principio arquitectónico

La interfaz de usuario es **solo un adaptador de entrada**. Captura datos, presenta resultados y llama puertos de aplicación como `EmitirComprobanteUseCase`, `ConsultarEstadoUseCase` o `MigrarLegadoUseCase`. La UI no calcula IGV/ISC/ICBPER, no crea XML UBL, no firma, no accede directamente a la base de datos y no decide cuándo enviar a SUNAT/OSE.

La lógica de dominio y aplicación se comparte en módulos Kotlin independientes de Compose. Las integraciones locales —SQLite, archivos, impresora, cámara, almacenamiento seguro y conectividad— se implementan como adaptadores por plataforma.

## Comparación

| Criterio | .NET MAUI | Flutter | Kotlin Multiplatform + Compose Multiplatform |
|---|---|---|---|
| Windows y Android | Un proyecto C#/XAML para ambas plataformas. Windows usa WinUI; Android usa controles MAUI. | Un proyecto Dart con motor Flutter para Windows y Android. | Código común Kotlin; Compose comparte UI y usa Jetpack Compose en Android y Compose Desktop/JVM en Windows. |
| MSI | No es salida nativa primaria: la publicación oficial de Windows produce MSIX o ejecutable sin empaquetar. Un MSI requiere una etapa adicional, por ejemplo WiX. | `flutter build windows` produce una aplicación Win32; MSI requiere empaquetador adicional. La documentación oficial prioriza MSIX/ZIP. | Soporte directo del plugin oficial: `TargetFormat.Msi` y tarea Gradle `packageMsi`, basada en `jpackage`. Debe ejecutarse en Windows. |
| APK | Publicación Android mediante `dotnet publish`; admite firma de release. | `flutter build apk`; flujo bien documentado y maduro. | Target Android estándar; `assembleRelease` genera APK y `bundleRelease` AAB mediante Android Gradle Plugin. |
| Firma Windows | Firma de MSIX en el flujo Windows; un MSI adicional debe firmarse aparte. | Firma del ejecutable/paquete mediante herramientas Windows externas. | El MSI generado se firma con Authenticode usando `SignTool` y certificado de firma de código. El plugin no reemplaza la custodia del certificado. |
| Firma Android | Keystore de Android configurado para release. | Keystore y configuración Gradle. | Keystore y `signingConfig` estándar de Android; compatible con Play App Signing. |
| Modo sin conexión | Posible con SQLite y cola local; requiere diseñarlo. | Posible con SQLite/Isar/Drift y cola local; requiere diseñarlo. | SQLDelight/SQLite y coroutines permiten repositorio local y cola de sincronización compartidos; los controladores de plataforma permanecen separados. |
| Reutilización con arquitectura actual | Buena si el backend/dominio migra a .NET; introduce C# frente al diseño Java/Kotlin existente. | Buena para UI, pero introduce Dart y duplica DTO/adaptadores respecto del núcleo JVM previsto. | Muy alta: mantiene Kotlin/JVM, Gradle y contratos compartidos con el monolito/puertos descritos. |
| Riesgo principal | MSI no directo y dependencia de WinUI/Windows App SDK. | MSI y firma Windows dependen de herramientas adicionales; segundo ecosistema/lenguaje. | El instalador desktop incluye runtime JVM y debe probar módulos `jlink`; las APIs específicas necesitan adaptadores `expect/actual` o interfaces. |

Fuentes oficiales: [distribuciones nativas de Compose Multiplatform](https://kotlinlang.org/docs/multiplatform/compose-native-distribution.html), [relación Compose Multiplatform/Jetpack Compose](https://kotlinlang.org/docs/multiplatform/compose-multiplatform-and-jetpack-compose.html), [publicación Windows de .NET MAUI](https://learn.microsoft.com/dotnet/maui/windows/deployment/overview), [compilación Windows de Flutter](https://docs.flutter.dev/platform-integration/windows/building) y [firma Android](https://developer.android.com/studio/publish/app-signing).

## Decisión y justificación

Se elige **Kotlin Multiplatform con Compose Multiplatform** porque:

1. genera el MSI requerido mediante una tarea oficial, sin mantener un proyecto WiX separado;
2. conserva el ecosistema JVM/Kotlin ya implícito en la arquitectura y permite compartir DTO, validaciones de presentación y orquestación offline;
3. usa el toolchain Android estándar para APK, firma y publicación;
4. mantiene el dominio detrás de puertos: Compose es reemplazable y no pasa a ser una dependencia del núcleo;
5. permite una base SQLite local y una cola durable para operar sin conexión.

La elección no implica compartir todo. El núcleo y, cuando convenga, los composables viven en `commonMain`; impresoras, certificados, archivos y mecanismos de actualización pertenecen a adaptadores `androidMain` y `desktopMain`.

## Modo sin conexión

- Persistir productos, clientes, catálogos SUNAT versionados, series autorizadas y operaciones pendientes en SQLite.
- Registrar cada comando con UUID idempotente, hora local y estado `PENDIENTE`, `ENVIANDO`, `CONFIRMADO` o `ERROR`.
- La UI llama al mismo puerto de entrada tanto en línea como fuera de línea; un adaptador decide si ejecuta contra el servicio o encola.
- La reconexión sincroniza en orden, con bloqueo por serie/correlativo, reintento exponencial y prevención de duplicados.
- La aplicación muestra claramente qué comprobantes tienen CDR, cuáles esperan envío y cuáles fueron rechazados.
- Las claves privadas de firma fiscal no se copian a dispositivos móviles salvo decisión de seguridad explícita. Preferentemente firma y envío permanecen en el servicio central/OSE.

## Generación del MSI de Windows

### Configuración Gradle

En el módulo desktop:

```kotlin
import org.jetbrains.compose.desktop.application.dsl.TargetFormat

compose.desktop {
    application {
        mainClass = "pe.com.abc.facturacion.desktop.MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Msi)
            packageName = "ABC Facturacion"
            packageVersion = "1.0.0"
            modules("java.sql", "java.naming")
            windows {
                menuGroup = "ABC Software Corp"
                upgradeUuid = "REEMPLAZAR-POR-UUID-ESTABLE"
            }
        }
    }
}
```

Proceso reproducible:

1. Ejecutar pruebas unitarias, de arquitectura y de integración offline.
2. En un agente CI de Windows con JDK compatible y herramientas requeridas por `jpackage`, ejecutar `gradlew.bat packageMsi`.
3. Recoger el artefacto desde `build/compose/binaries/main/msi/` y calcular SHA-256.
4. Firmar el MSI con un certificado Authenticode de la empresa y sello de tiempo: `signtool sign /fd SHA256 /td SHA256 /tr <URL_TIMESTAMP> /f <CERTIFICADO.pfx> <ARTEFACTO.msi>`.
5. Verificar con `signtool verify /pa /v <ARTEFACTO.msi>` e instalar en una VM limpia, probando instalación, actualización conservando datos y desinstalación.
6. Publicar MSI, hash, versión y notas de cambio. El `.pfx` y su contraseña se mantienen fuera del repositorio, en el almacén secreto/HSM del CI.

El MSI solo puede construirse en Windows porque el plugin no soporta compilación cruzada de formatos nativos. `SignTool` forma parte del Windows SDK y admite firma de MSI; se recomienda SHA-256 ([documentación oficial](https://learn.microsoft.com/windows/desktop/seccrypto/signtool)).

## Generación del APK de Android

1. Definir `applicationId`, `versionCode`, `versionName`, SDK mínimo/objetivo y permisos mínimos.
2. Crear o registrar el keystore de release. Guardar ruta, alias y contraseñas como secretos de CI; nunca confirmar `.jks`, `.keystore` ni contraseñas.
3. Configurar `android.signingConfigs.release` y asociarlo a `buildTypes.release`; activar R8 y reglas de conservación probadas.
4. Ejecutar pruebas y luego `gradlew.bat :androidApp:assembleRelease` para APK o `:androidApp:bundleRelease` para Play Store.
5. Verificar el APK con `apksigner verify --verbose --print-certs <APK>` e instalarlo en dispositivos representativos para probar actualización y datos offline.
6. Publicar el APK firmado para distribución administrada. Para Google Play, preferir AAB y Play App Signing, conservando una clave de carga separada.

Android exige que todo APK instalable esté firmado. La salida suele quedar bajo `androidApp/build/outputs/apk/release/`; la ruta exacta depende del nombre real del módulo.

## Controles de entrega

- Versionado semántico del producto y números de compilación monotónicos.
- Builds de release reproducibles desde un tag; hashes y SBOM por artefacto.
- Secretos de firma solo disponibles en el job protegido de publicación.
- Pruebas de arranque sin red, sincronización, impresora y migración de base local.
- Matriz de compatibilidad con Windows 10/11 x64 y niveles Android soportados.
- Canal piloto antes de despliegue general y estrategia de reversión que nunca degrade la base de datos sin respaldo.


## Matriz de compatibilidad

| Plataforma | Mínimo | Objetivo | Notas |
|---|---|---|---|
| Windows | Windows 10 x64 (mínimo de Compose Multiplatform: Windows 10 x86-64) | Windows 11 x64 | Se instala con MSI por máquina; requiere permisos de administrador. ⚠️ Por verificar el build mínimo de Windows 10 que acepta el JDK 21 elegido. |
| Android | `minSdk` **26** (Android 8.0). Decisión propuesta: el mínimo técnico de Compose Multiplatform es API 21, pero 26 simplifica el trabajo en segundo plano de la cola de sincronización y los canales de notificación. ⚠️ Por verificar con los dispositivos reales de preventa. | Último nivel estable al publicar (`compileSdk` y `targetSdk` se fijan al crear el proyecto) | Los permisos se limitan a red, cámara (si se escanean códigos) e impresión. |
| JDK / Kotlin / Compose | JDK **21** (LTS), Kotlin **2.4.21**, Compose Multiplatform **1.12.2** | — | Versiones fijadas en el catálogo de Gradle (ver abajo). JDK 17 es el mínimo para `jpackage`. |

### Versiones fijadas para el primer release

Verificadas el 2026-11-07 (detalle y fuentes en `docs/pendientes-verificacion.md`): Compose Multiplatform 1.12.2 es la última versión estable listada por JetBrains; Kotlin 2.4.21 es la última estable (8-oct-2026); Compose Multiplatform exige Kotlin 2.1.0 o superior, JDK 11 o superior en escritorio y **JDK 17 o superior** para empaquetar distribuciones nativas. JDK 21 y `minSdk` 26 son **decisiones del equipo** dentro de esos mínimos, no datos de una fuente.

```toml
# gradle/libs.versions.toml
[versions]
kotlin = "2.4.21"
compose-multiplatform = "1.12.2"
jdk = "21"
android-minSdk = "26"
# Por fijar al crear el proyecto: Gradle, Android Gradle Plugin, compileSdk y targetSdk
# (deben ser compatibles con Kotlin 2.4.21 y JDK 21).
```

## Protocolo de sincronización sin conexión

1. **Comando local**: cada operación crea un registro `(uuid, tipo, payload, serie, creado_en, estado)` con estado `PENDIENTE`.
2. **Orden**: los comandos se envían por serie y por orden de creación. Dos dispositivos con la misma serie no se permiten: cada caja y cada dispositivo de preventa usa **series propias** (p. ej. `F001`, `F002`).
3. **Envío**: `POST` con la cabecera `Idempotency-Key = uuid`. El servidor asigna el correlativo.
4. **Respuesta**:
   - `2xx` → `CONFIRMADO`, se guarda el resultado y los enlaces a XML/PDF/CDR.
   - Respuesta previa para el mismo UUID → se acepta como `CONFIRMADO` sin crear duplicados.
   - `4xx` de negocio (`400`, `409`, `422`) → `ERROR`; no se reintenta automáticamente y requiere acción del usuario.
   - `5xx` o `502` o red → permanece `PENDIENTE` con reintento exponencial (p. ej. 5 s, 15 s, 1 min, 5 min, máximo 30 min).
5. **Conflictos**: como el correlativo lo asigna el servidor y las series son por dispositivo, no hay conflictos de numeración. Si un maestro (precio, afectación) cambió, el servidor valida con los datos vigentes y puede responder `422`.
6. **Visibilidad**: la UI distingue "registrado localmente", "enviado", "aceptado por SUNAT" y "rechazado".
7. **Límites**: un comprobante registrado sin conexión no se considera emitido legalmente hasta recibir CDR; el plazo máximo de envío se controla con una alerta. Plazo ordinario verificado: factura, 7 días calendario desde el día siguiente a la emisión (SUNAT Orientación, 2026-11-07). ⚠️ Por verificar el plazo en contingencia [F-03].

## Canal de actualización y reversión

- **Windows**: el MSI conserva `upgradeUuid` estable; una versión nueva reemplaza a la anterior (*major upgrade*) y conserva la base local. Las migraciones de base local son **hacia adelante** y se acompañan de respaldo previo; la reversión instala la versión anterior y restaura el respaldo.
- **Android**: distribución administrada del APK firmado con la misma clave; `versionCode` crece siempre. Una versión con migración de datos se publica primero a un grupo piloto.
- **Compatibilidad API**: las apps funcionan con `v1` y toleran campos nuevos. Un cambio incompatible exige una nueva versión de la API (`/v2`).
- **Canal piloto**: toda versión pasa por 1–2 cajas piloto antes del despliegue general.

## Modelo de amenazas (STRIDE resumido)

| Activo | Amenaza | Control |
|---|---|---|
| Certificado digital X.509 y clave privada | Robo o copia (Information disclosure) | Almacén protegido/HSM en el servicio central; nunca en dispositivos móviles ni en Git; acceso mínimo y auditoría. |
| Keystore de Android y certificado de firma de código | Uso indebido para firmar software malicioso (Spoofing) | Secretos solo en el job protegido de publicación; rotación; respaldo controlado. |
| Instalador MSI/APK | Manipulación (Tampering) | Firma Authenticode / APK, SHA-256 publicado, verificación con `signtool verify` y `apksigner verify`. |
| Base local offline | Lectura por terceros si se pierde el equipo | Cifrado de la base local y bloqueo de pantalla; datos mínimos necesarios. |
| Cola de sincronización | Reproducción o duplicación (Repudiation / Tampering) | UUID idempotente, restricciones únicas, TLS y JWT con expiración corta. |
| API | Acceso no autorizado (Elevation of privilege) | `bearerAuth` (JWT), roles por operación, `401`/`403`. |
| Comprobantes emitidos | Negación de la emisión (Repudiation) | Registro de auditoría con usuario, hora, hash y CDR. |

> ⚠️ Por verificar: este modelo es un resumen inicial; se amplía cuando se defina el despliegue del servicio central.
