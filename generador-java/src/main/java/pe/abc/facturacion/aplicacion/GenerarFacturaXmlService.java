package pe.abc.facturacion.aplicacion;

import java.util.Map;

import pe.abc.facturacion.aplicacion.puerto.GeneradorXmlUblPort;
import pe.abc.facturacion.dominio.catalogo.RegimenIgv;
import pe.abc.facturacion.dominio.modelo.Modelo.Factura;
import pe.abc.facturacion.dominio.validacion.LectorFactura;

/**
 * Caso de uso: generar el XML de un comprobante a partir de sus datos.
 *
 * <p>Valida los datos en el dominio (todos los errores juntos) y delega la construccion del XML en el puerto
 * de salida. No conoce JAXB ni ninguna libreria XML.
 */
public final class GenerarFacturaXmlService {

    private final GeneradorXmlUblPort generador;

    public GenerarFacturaXmlService(GeneradorXmlUblPort generador) {
        this.generador = generador;
    }

    public byte[] generar(Map<String, Object> datos, long correlativo, RegimenIgv regimenIgv) {
        Factura factura = LectorFactura.desdeMapa(datos, correlativo, regimenIgv);
        return generador.generar(factura);
    }

    public byte[] generar(Map<String, Object> datos, long correlativo) {
        return generar(datos, correlativo, RegimenIgv.GENERAL);
    }
}
