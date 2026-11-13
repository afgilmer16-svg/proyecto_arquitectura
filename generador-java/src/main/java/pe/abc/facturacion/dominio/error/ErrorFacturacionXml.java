// Autor: Gilmer Ayala Falcón · UNAS-FIIS · Arquitectura de Software · Grupo A
package pe.abc.facturacion.dominio.error;

/** Base de los errores del generador. */
public class ErrorFacturacionXml extends RuntimeException {

    public ErrorFacturacionXml(String mensaje) {
        super(mensaje);
    }

    public ErrorFacturacionXml(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
