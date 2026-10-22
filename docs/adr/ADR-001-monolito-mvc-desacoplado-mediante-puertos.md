# ADR-001 — Monolito MVC Desacoplado mediante Puertos y Adaptadores

**Estado**: APROBADO  
**Fecha**: 2026-10-22  
**Autores**: Equipo Grupo A — UNAS-FIIS  
**Fuentes**: RS 097-2012/SUNAT (F-03), RS 234-2006/SUNAT (F-04), OASIS UBL 2.1 (F-01)

---

## Contexto

**ABC Software Corp** es una empresa comercializadora de abarrotes de alta rotación en Perú que debe implementar urgentemente la facturación electrónica bajo la normativa de SUNAT (RS N° 097-2012). Actualmente, la empresa opera sin integración electrónica, lo que genera riesgos inminentes de sanciones fiscales y pérdida de deducción de costos/gastos para sus clientes adquirentes.

El equipo de desarrollo es de tamaño reducido (3 a 5 ingenieros) y el plazo para la puesta en marcha es ajustado. Sin embargo, la empresa planea a mediano plazo abrir nuevas sucursales y puntos de venta móviles para preventa, lo que demandará escalabilidad y eventualmente una separación en microservicios.

El desafío arquitectónico consiste en:
1. Diseñar un sistema que se pueda desarrollar, probar y desplegar rápidamente sin la sobrecarga operacional, de red y de monitoreo distribuido que exige una arquitectura de microservicios desde el día uno.
2. Evitar el "monolito de espagueti" tradicional (donde los controladores manipulan directamente la base de datos o librerías SOAP de SUNAT mediante Active Record o lógica mezclada), lo que tornaría imposible una separación futura.
3. Garantizar que las reglas de cálculo fiscal (IGV 18%, ISC, ICBPER) y de validación de comprobantes se mantengan 100% aisladas e independientes de cualquier cambio en la interfaz de usuario o en los servicios web de SUNAT/OSE.

---

## Opciones evaluadas

| Criterio | Opción A: Monolito Tradicional Acoplado | Opción B: Microservicios Distribuidos desde el Día 1 | Opción C: Monolito MVC Desacoplado con Puertos y Adaptadores (Elegida) |
|---|---|---|---|
| **Velocidad de entrega inicial** | Alta en el corto plazo; decae drásticamente con cada cambio fiscal. | Baja; requiere configurar service discovery, API gateway, mTLS, tracing distribuido y Sagas. | **Alta**; una sola base de código ejecutable y despliegue unificado. |
| **Complejidad de despliegue y Ops** | Mínima (un único instalable/binario). | Muy alta (Kubernetes, múltiples contenedores, monitoreo de red distribuido). | **Baja**; se empaqueta como un solo artefacto pero con fronteras de paquetes bien delimitadas. |
| **Aislamiento de reglas fiscales** | Nulo; lógica SUNAT dispersa en controladores y vistas. | Alto; en microservicio dedicado de facturación. | **Excelente**; reglas en el núcleo de dominio (`Domain Services`), sin dependencias de frameworks. |
| **Facilidad para pruebas unitarias** | Difícil; requiere bases de datos activas y endpoints reales. | Complejo; requiere dobles de red y pruebas de integración distribuidas. | **Óptima**; el 100% de los casos de uso se prueba con mocks de las interfaces de puertos. |
| **Ruta de migración futura** | Traumática; requiere reescribir el sistema completo. | Ya construida; pero asume deuda técnica prematura innecesaria. | **Limpia y progresiva**; basta con cambiar adaptadores de infraestructura para extraer servicios. |

---

## Decisión

**Decisión**: Se elige la **Opción C: Monolito MVC Desacoplado mediante Puertos y Adaptadores (Arquitectura Hexagonal Modular)**.

### Fundamentos de la Elección:
1. **Regla de Dependencia Estricta**: La capa de **Dominio** no depende de nada exterior. Define entidades puras (`ComprobantePago`, `Factura`, `BoletaVenta`, `Tributo`, `Ruc`) y servicios de dominio (`CalculadorImpuestosDomainService`), junto con interfaces de salida (**puertos** como `ComprobanteRepository`, `EnvioOsePort`, `FirmaDigitalPort`).
2. **Inversión de Dependencias (DIP)**: Los controladores (vistas de escritorio para cajeros, móviles o web) solo conocen los **puertos de entrada** (`EmitirComprobanteUseCase`, `AnularComprobanteUseCase`). Los adaptadores de infraestructura (base de datos relacional, serializador UBL 2.1, cliente SOAP OSE, almacén de claves criptográficas) implementan los puertos de salida.
3. **Evolución sin Reescritura**: Cuando se decida extraer un microservicio (por ejemplo, el módulo de envío OSE y firma digital), el código del dominio y de los casos de uso permanece intacto; únicamente se sustituye la clase `LocalPkcs12FirmaAdapter` por un `CloudKmsFirmaAdapter` o un despachador a colas RabbitMQ.

---

## Consecuencias

### Positivas:
- **Time-to-Market Acelerado**: Permite al equipo de ABC Software Corp cumplir con el plazo regulatorio de SUNAT utilizando un despliegue simple sin orquestadores complejos.
- **Testabilidad Integral**: Permite probar todas las casuísticas de cálculo de tributos (operaciones gravadas, exoneradas, inafectas, ICBPER en bolsas plásticas) en milisegundos mediante pruebas unitarias puras.
- **Preparado para Microservicios**: Establece una separación de responsabilidades clara que habilita el patrón *Strangler Fig* sin fricciones.
- **Soporte Multiplataforma**: Al estar desacoplado de la presentación, el mismo caso de uso `EmitirComprobanteUseCase` atiende concurrentemente a terminales de escritorio de cajeros, aplicaciones móviles de preventistas y llamadas de API externa.

### Negativas / Compromisos:
- **Mayor número de clases e interfaces**: Exige crear DTOs de entrada/salida, interfaces de puertos y clases adaptadoras, aumentando el *boilerplate* inicial frente a un diseño rudimentario CRUD.
- **Disciplina del equipo**: Requiere vigilancia continua mediante linters o reglas de arquitectura (e.g., ArchUnit) para evitar que desarrolladores inadvertidamente importen librerías de infraestructura dentro del paquete de dominio.

### Riesgos y Mitigaciones:
- *Riesgo*: Que el monolito crezca desordenadamente y se violen las fronteras de los módulos.  
  *Mitigación*: Validación automatizada en CI/CD que verifique que el paquete `domain` no posea importaciones hacia `infrastructure` ni dependencias de frameworks externos.

---

## Estado de revisión

- [x] Borrador inicial completado
- [x] Revisado contra principios de arquitectura limpia y requerimientos del caso
- [x] Aprobado por el equipo de arquitectura Grupo A
