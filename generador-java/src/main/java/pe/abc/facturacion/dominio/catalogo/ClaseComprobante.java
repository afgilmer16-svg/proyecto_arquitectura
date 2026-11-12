package pe.abc.facturacion.dominio.catalogo;

import java.util.Arrays;
import java.util.Optional;

/**
 * Catalogo 01 de SUNAT, limitado a los documentos que se describen con {@code UBL-Invoice-2.1.xsd}:
 * la factura (codigo 01, series F...) y la boleta de venta (codigo 03, series B...).
 */
public enum ClaseComprobante {
    FACTURA("01", 'F'),
    BOLETA("03", 'B');

    private final String codigo;
    private final char letraDeSerie;

    ClaseComprobante(String codigo, char letraDeSerie) {
        this.codigo = codigo;
        this.letraDeSerie = letraDeSerie;
    }

    public String codigo() {
        return codigo;
    }

    /** Letra con la que empieza la serie de este tipo (F o B). */
    public char prefijoSerie() {
        return letraDeSerie;
    }

    public static Optional<ClaseComprobante> desde(Object codigo) {
        return Arrays.stream(values()).filter(c -> c.codigo.equals(codigo)).findFirst();
    }
}
