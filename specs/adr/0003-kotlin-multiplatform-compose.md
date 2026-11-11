# ADR-003 — Kotlin Multiplatform y Compose Multiplatform para Windows y Android

**Estado:** APROBADO  
**Fecha:** 2026-10-22  
**Autores:** Equipo Grupo A — UNAS-FIIS  
**Relacionado:** `specs/features/0007-distribucion-multiplataforma/design.md`, ADR-001

## Contexto

ABC Software Corp necesita una aplicación de escritorio para cajas Windows y una aplicación Android para preventa/reparto. Ambas deben operar temporalmente sin conexión, consumir los mismos casos de uso y distribuirse como MSI y APK firmados.

ADR-001 establece un núcleo independiente mediante puertos y adaptadores. En consecuencia, el framework visual no puede contener reglas fiscales, persistencia directa, generación UBL ni integración OSE/SUNAT. La UI debe limitarse a adaptar eventos del usuario a puertos de entrada y presentar sus respuestas.

## Fuerzas de decisión

- MSI nativo para Windows y APK firmado para Android.
- Operación offline con persistencia local y sincronización idempotente.
- Máxima reutilización sin acoplar dominio/aplicación al framework visual.
- Integración con el ecosistema JVM/Kotlin sugerido por la arquitectura existente.
- Automatización de compilación, firma y verificación en CI.
- Equipo pequeño y necesidad de minimizar toolchains y lenguajes adicionales.

## Opciones consideradas

### .NET MAUI

Ofrece una base C# común para Windows y Android y buena integración con el ecosistema Microsoft. Sin embargo, el formato oficial principal para Windows es MSIX o una publicación sin empaquetar; producir MSI requiere una herramienta/proyecto adicional. Además introduciría C#/.NET junto al núcleo JVM previsto.

### Flutter

Tiene una experiencia sólida de UI compartida y generación directa de APK. En Windows produce una aplicación Win32, pero MSI requiere empaquetado adicional. Introduce Dart y un segundo modelo de dependencias para DTO, persistencia y adaptadores.

### Kotlin Multiplatform + Compose Multiplatform

Comparte Kotlin entre Android y desktop, usa Jetpack Compose en Android y el plugin oficial de Compose Desktop en Windows. El plugin soporta `TargetFormat.Msi` y genera distribuciones autocontenidas mediante `jpackage`. Android conserva el flujo Gradle estándar para APK/AAB.

## Decisión

Adoptar **Kotlin Multiplatform con Compose Multiplatform** para los adaptadores de UI Windows y Android.

La solución se organiza así:

- `domain`: entidades, value objects y reglas fiscales, sin Compose ni APIs de plataforma;
- `application`: puertos de entrada y casos de uso;
- `shared`: DTO, clientes de puertos, coordinación offline y contratos reutilizables;
- `desktopApp`: adaptador Compose Desktop, integración Windows y empaquetado MSI;
- `androidApp`: adaptador Compose Android, integración Android y empaquetado APK/AAB;
- `infrastructure`: adaptadores de base local, red, impresión, archivos y secretos.

La UI es **exclusivamente un adaptador de entrada**. Ningún `@Composable` puede calcular tributos, construir UBL, firmar XML, asignar series/correlativos ni llamar directamente al OSE/SUNAT.

## Justificación

1. Cumple directamente el formato MSI mediante el plugin oficial (`TargetFormat.Msi`) y el APK mediante Android Gradle Plugin.
2. Reduce duplicación de modelos y orquestación al conservar Kotlin/JVM.
3. Permite encapsular SQLite/SQLDelight y la cola de sincronización detrás de repositorios/puertos.
4. Mantiene intercambiable la presentación conforme a ADR-001.
5. Evita mantener una definición WiX adicional solo para obtener MSI.

Fuentes técnicas: [distribuciones nativas de Compose Multiplatform](https://kotlinlang.org/docs/multiplatform/compose-native-distribution.html), [Compose Multiplatform y Jetpack Compose](https://kotlinlang.org/docs/multiplatform/compose-multiplatform-and-jetpack-compose.html), [firma de aplicaciones Android](https://developer.android.com/studio/publish/app-signing) y [SignTool de Windows](https://learn.microsoft.com/windows/desktop/seccrypto/signtool).

## Consecuencias

### Positivas

- Un lenguaje y sistema de construcción principal para núcleo, escritorio y Android.
- MSI soportado por el empaquetador oficial y APK por el toolchain oficial Android.
- Reutilización de UI cuando sea conveniente sin impedir adaptadores específicos.
- Distribuciones desktop autocontenidas con runtime JVM reducido por `jlink`.
- Pruebas del dominio y casos de uso sin iniciar Compose ni dispositivos.

### Negativas y compromisos

- El MSI debe construirse en Windows; no existe compilación cruzada para ese formato.
- Es necesario declarar/probar módulos JDK de `jlink`; una omisión puede fallar solo en la distribución empacada.
- Impresión, almacenamiento seguro, cámara y actualizaciones requieren adaptadores por plataforma.
- El runtime incluido incrementa el tamaño del MSI.
- La firma Authenticode del MSI es una etapa separada y requiere certificado protegido.

## Riesgos y mitigaciones

| Riesgo | Mitigación |
|---|---|
| Reglas de negocio filtradas a composables | Pruebas de arquitectura que impidan dependencias `domain/application → compose`, revisiones y ViewModels que solo invoquen puertos. |
| Divergencia Windows/Android | Contratos comunes, pruebas compartidas y adaptadores pequeños por plataforma. |
| Pérdida o exposición de claves | Keystore Android y certificado Windows en HSM/secret store de CI; rotación, acceso mínimo y respaldo controlado. |
| Duplicación durante sincronización | UUID idempotente por comando, outbox local, restricciones únicas y confirmación del servidor. |
| Fallos del instalador o actualización | Pruebas en VM limpia, firma y verificación, canal piloto, respaldo y migraciones de BD reversibles. |

## Reglas de implementación

1. `domain` y `application` no importan Compose, Android, Swing/AWT ni librerías de empaquetado.
2. Los composables no llaman repositorios ni clientes HTTP directamente.
3. Toda operación offline se registra antes de mostrarse como aceptada localmente y distingue estado local de aceptación fiscal.
4. MSI y APK de producción se firman y verifican; los secretos nunca se guardan en Git.
5. Las versiones de Kotlin, Compose, JDK, Android Gradle Plugin y SDK se fijan y actualizan mediante un proceso controlado.

## Estado de revisión

- [x] Alternativas comparadas
- [x] Coherencia con ADR-001 verificada
- [x] Estrategia MSI/APK y firma documentada
- [x] Decisión aprobada
