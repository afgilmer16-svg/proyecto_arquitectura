# tasks.md — 0007 Distribución Multiplataforma

**Feature**: Distribución Multiplataforma  
**ID**: 0007  
**Autor**: Gilmer Ayala Falcón (Grupo A, UNAS-FIIS)  
**Fecha**: 2026-11-06

---

## Tareas de especificación

- [x] Redactar ADR-003 (`specs/adr/0003-kotlin-multiplatform-compose.md`)
- [x] Redactar `specs/features/0007-distribucion-multiplataforma/design.md` (comparación, MSI, APK, modo sin conexión)
- [x] Redactar `spec.md` con requisitos RF-DIS y criterios de aceptación
- [x] Definir la matriz de compatibilidad (Windows 10/11 x64 y niveles Android)
- [x] Definir el protocolo de sincronización sin conexión (estados, orden, conflictos)
- [x] Definir el canal de actualización y la estrategia de reversión
- [x] Definir el modelo de amenazas del almacén de secretos y la firma
- [ ] Revisión por segundo miembro del equipo

## Tareas de verificación

- [ ] Confirmar `TargetFormat.Msi` y `packageMsi` contra la documentación vigente de Compose Multiplatform
- [ ] Confirmar los módulos `jlink` necesarios del MSI en una VM limpia
- [ ] Confirmar el procedimiento `signtool` y `apksigner` en el CI
- [x] Verificar el entregable `.gitignore` contra credenciales (`.pfx`, `.p12`, `.jks`, `.keystore`)

## Notas de progreso

- 2026-11-06: se agregó la `spec.md` de la feature, que faltaba aunque el ADR y el empaquetado ya estaban redactados.
- 2026-11-06: se agregaron compatibilidad, protocolo de sincronización, canal de actualización y modelo de amenazas a `specs/features/0007-distribucion-multiplataforma/design.md`. Faltan las versiones exactas de JDK/Kotlin/Compose y `minSdk`.
- 2026-11-07: se fijaron Kotlin 2.4.21, Compose Multiplatform 1.12.2, JDK 21 y `minSdk` 26 (fuentes y decisiones en `docs/pendientes-verificacion.md`). Quedan por fijar Gradle y el Android Gradle Plugin, y confirmar `minSdk` con los dispositivos de preventa.
- 2026-11-11: reorganización a la estructura `specs/features/NNNN/{spec,design,tasks}.md`; el contenido de las carpetas `specs/0X-*` pasó a `design.md` y los ADR completos a `specs/adr/`.
