package pe.abc.facturacion.dominio.error;

/** Falta un archivo del esquema UBL 2.1 y el XSD no se puede compilar. */
public class EsquemaXsdIncompletoException extends ErrorFacturacionXml {

    public EsquemaXsdIncompletoException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
