package pe.abc.facturacion.dominio.catalogo;

import java.util.Optional;

/** Catalogo 01: tipos de documento que usan UBL-Invoice-2.1.xsd. */
public enum TipoComprobante {
    FACTURA("01", 'F'),
    BOLETA("03", 'B');

    private final String codigo;
    private final char prefijoSerie;

    TipoComprobante(String codigo, char prefijoSerie) {
        this.codigo = codigo;
        this.prefijoSerie = prefijoSerie;
    }

    public String codigo() {
        return codigo;
    }

    public char prefijoSerie() {
        return prefijoSerie;
    }

    public static Optional<TipoComprobante> desde(Object codigo) {
        for (TipoComprobante t : values()) {
            if (t.codigo.equals(codigo)) {
                return Optional.of(t);
            }
        }
        return Optional.empty();
    }
}
