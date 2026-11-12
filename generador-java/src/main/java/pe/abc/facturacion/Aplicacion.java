package pe.abc.facturacion;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import pe.abc.facturacion.aplicacion.GenerarFacturaXmlService;
import pe.abc.facturacion.dominio.catalogo.RegimenIgv;
import pe.abc.facturacion.dominio.error.ErrorFacturacionXml;
import pe.abc.facturacion.infraestructura.entrada.EsquemaEntrada;
import pe.abc.facturacion.infraestructura.entrada.LectorJson;
import pe.abc.facturacion.infraestructura.ubl.GeneradorUblJaxbAdapter;

/**
 * Linea de comandos: genera el XML de un comprobante a partir de un JSON.
 *
 * <pre>
 * java -jar facturacion-xml.jar ENTRADA.json SALIDA.xml --correlativo 1 [--regimen-igv GENERAL]
 * </pre>
 *
 * Codigos de salida: 0 correcto, 1 datos invalidos o XML no conforme, 2 uso incorrecto.
 */
public final class Aplicacion {

    private Aplicacion() {
    }

    public static void main(String[] args) {
        System.exit(ejecutar(args, System.out, System.err));
    }

    static int ejecutar(String[] args, PrintStream salidaEstandar, PrintStream salidaError) {
        if (args.length < 4 || !"--correlativo".equals(args[2])) {
            salidaError.println("Uso: ENTRADA.json SALIDA.xml --correlativo N [--regimen-igv GENERAL|RESTAURANTES_HOTELES]");
            return 2;
        }
        long correlativo;
        try {
            correlativo = Long.parseLong(args[3]);
        } catch (NumberFormatException e) {
            salidaError.println("ERROR: el correlativo debe ser un entero");
            return 2;
        }
        RegimenIgv regimen = RegimenIgv.GENERAL;
        if (args.length >= 6 && "--regimen-igv".equals(args[4])) {
            regimen = RegimenIgv.desde(args[5]).orElse(null);
            if (regimen == null) {
                salidaError.println("ERROR: --regimen-igv debe ser GENERAL o RESTAURANTES_HOTELES");
                return 2;
            }
        }
        try {
            Map<String, Object> datos = LectorJson.leer(Path.of(args[0]));
            List<String> errores = new EsquemaEntrada().validar(datos);
            if (!errores.isEmpty()) {
                salidaError.println("ERROR: la entrada no cumple factura-entrada.schema.json:\n- "
                        + String.join("\n- ", errores));
                return 1;
            }
            byte[] xml = new GenerarFacturaXmlService(new GeneradorUblJaxbAdapter()).generar(datos, correlativo, regimen);
            Path salida = Path.of(args[1]);
            if (salida.toAbsolutePath().getParent() != null) {
                Files.createDirectories(salida.toAbsolutePath().getParent());
            }
            Files.write(salida, xml);
            salidaEstandar.println("XML generado y validado contra UBL-Invoice-2.1.xsd: " + salida);
            return 0;
        } catch (ErrorFacturacionXml e) {
            salidaError.println("ERROR: " + e.getMessage());
            return 1;
        } catch (IOException e) {
            salidaError.println("ERROR: " + e.getMessage());
            return 1;
        }
    }
}
