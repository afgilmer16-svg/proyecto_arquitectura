package pe.abc.facturacion.dominio.error;

import java.util.List;

/** El XML no es valido contra {@code UBL-Invoice-2.1.xsd}: nunca se entrega. */
public class XmlNoConformeXsdException extends ErrorFacturacionXml {

    private final List<String> errores;

    public XmlNoConformeXsdException(List<String> errores) {
        super("El XML no cumple el XSD UBL-Invoice-2.1:\n- " + String.join("\n- ", errores));
        this.errores = List.copyOf(errores);
    }

    public List<String> errores() {
        return errores;
    }
}
