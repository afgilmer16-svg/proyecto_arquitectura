// Autor: Gilmer Ayala Falcón · UNAS-FIIS · Arquitectura de Software · Grupo A
package pe.abc.facturacion.dominio.error;

import java.util.List;

/**
 * Los datos de entrada no cumplen las reglas del comprobante.
 *
 * <p>{@link #errores()} trae un mensaje por problema, con la ruta del campo (por ejemplo
 * {@code items[2].unidad_medida}), para corregirlos todos a la vez.
 */
public class DatosFacturaInvalidosException extends ErrorFacturacionXml {

    private final List<String> errores;

    public DatosFacturaInvalidosException(List<String> errores) {
        super("Datos de la factura inválidos:\n- " + String.join("\n- ", errores));
        this.errores = List.copyOf(errores);
    }

    public List<String> errores() {
        return errores;
    }
}
