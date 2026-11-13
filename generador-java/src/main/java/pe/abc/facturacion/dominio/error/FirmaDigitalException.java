// Autor: Gilmer Ayala Falcón · UNAS-FIIS · Arquitectura de Software · Grupo A
package pe.abc.facturacion.dominio.error;

/** No se pudo firmar el XML (certificado o contrasena invalidos, XML ya firmado) o la firma no se puede leer. */
public class FirmaDigitalException extends ErrorFacturacionXml {

    public FirmaDigitalException(String mensaje) {
        super(mensaje);
    }

    public FirmaDigitalException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
