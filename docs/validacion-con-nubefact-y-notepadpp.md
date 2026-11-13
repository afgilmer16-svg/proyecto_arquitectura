# Cómo validar el XML (Nubefact y Notepad++)

**Autor**: Gilmer Ayala Falcón · UNAS-FIIS · Arquitectura de Software · Grupo A
**Fecha**: 2026-11-12

El XML se genera **sin firma** y lleva el comentario `FIRMAR AQUÍ`. Hay tres formas de comprobar que es válido.

## 1. Lo que ya hace el generador

Antes de entregar el XML, el generador lo valida contra `UBL-Invoice-2.1.xsd` (Java y Python). Si el XML no cumple el XSD, no se genera.

## 2. Nubefact (OSE de demostración) — la validación más completa

Esta validación revisa, además del XSD, las reglas de negocio de SUNAT (nombres de tributo, catálogos, RUC, cuadre de importes).

1. Abre <https://probar-xml.nubefact.com>.
2. Pega el XML en el cuadro de la izquierda (por ejemplo, `specs/ejemplos/xml-referencia/05-factura-moto.xml`).
3. Marca **Firmar** (la herramienta inserta una firma de prueba) y pulsa **ENVIAR**.
4. A la derecha aparece la respuesta del OSE. Se busca `ResponseCode` `0` y la descripción **«La Factura Electrónica … ha sido ACEPTADA»**. Los códigos `4xxx` son observaciones; el resto son errores.

Cosas que conviene saber (salieron en las pruebas, ver [`informe-correcciones-xml.md`](informe-correcciones-xml.md)):

- **No se puede enviar dos veces el mismo número** (error `1033`, *comprobante registrado previamente*). Cambia el correlativo en `<cbc:ID>F001-0000000N</cbc:ID>` antes de volver a enviar. El formulario web lo hace solo.
- **El RUC del emisor debe existir para el OSE.** Un RUC inventado con dígito verificador válido puede ser rechazado con `2010` (*contribuyente no activo*) porque el OSE consulta su padrón. Para las pruebas se usó `20526018151`, que sí aceptó.
- Resultado obtenido el 12/11/2026: factura de la moto, factura de abarrotes (7 líneas con IGV, ISC, ICBPER, exonerada, inafecta y gratuita) → **ACEPTADA** sin observaciones.

## 3. Notepad++ (plugin XML Tools)

El XSD `UBL-Invoice-2.1.xsd` importa sus componentes con rutas relativas (`../common/…`), por eso no se puede validar desde la carpeta plana `docs/referencias/ubl`. La herramienta [`tools/preparar_validacion_notepadpp.py`](../tools/preparar_validacion_notepadpp.py) arma la estructura oficial; el resultado ya está en [`docs/validacion-notepadpp/`](validacion-notepadpp/):

```
docs/validacion-notepadpp/
  maindoc/UBL-Invoice-2.1.xsd     ← el XSD principal
  common/*.xsd                    ← los componentes que importa
  ejemplos/*.xml                  ← los XML de referencia
```

Para volver a armarla: `python tools/preparar_validacion_notepadpp.py`.

En Notepad++:

1. Instala el plugin **XML Tools** (*Plugins → Plugins Admin*).
2. Abre uno de los XML de `docs/validacion-notepadpp/ejemplos/`.
3. *Plugins → XML Tools → Validate Now* (`Ctrl+Alt+Shift+M`).
4. Cuando pida el esquema, elige `docs/validacion-notepadpp/maindoc/UBL-Invoice-2.1.xsd`.
5. Debe decir que el XML es válido.

**Limitación conocida:** el plugin usa una versión antigua de la biblioteca libxml y a veces falla con esquemas grandes como UBL aunque el XML esté bien. Si te da un error que no entiendes, no significa que el XML esté mal: comprueba con Nubefact. La prueba automática [`tests/test_validacion_notepadpp.py`](../tests/test_validacion_notepadpp.py) valida todos los ejemplos con esta misma estructura de carpetas usando lxml (libxml2 actual).

## 4. Firmar de verdad (opcional)

- **Nubefact:** marcar «Firmar» (certificado de prueba del OSE).
- **Con tu certificado `.pfx`:** `python -m facturacion_xml ENTRADA.json SALIDA.xml --correlativo 1 --firmar CERT.pfx` (la contraseña va en la variable de entorno `FIRMA_CLAVE`), o la casilla «Incluir firma digital» del formulario web.

Al firmar, la marca `FIRMAR AQUÍ` desaparece y en su lugar queda `ext:UBLExtensions` con la firma XMLDSig.
