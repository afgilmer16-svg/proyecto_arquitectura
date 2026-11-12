package pe.abc.facturacion.dominio.catalogo;

import java.util.Arrays;
import java.util.Optional;

/** Catalogo 08 de SUNAT: las tres formas de calcular el ISC (Apendice IV del TUO de la Ley del IGV e ISC). */
public enum SistemaCalculoIsc {
    /** 01: porcentaje sobre el valor de venta. */
    AL_VALOR("01"),
    /** 02: monto fijo por unidad. */
    ESPECIFICO("02"),
    /** 03: porcentaje sobre el precio de venta al publico sugerido, sin IGV. */
    PRECIO_VENTA_PUBLICO("03");

    private final String codigo;

    SistemaCalculoIsc(String codigo) {
        this.codigo = codigo;
    }

    public String codigo() {
        return codigo;
    }

    public static Optional<SistemaCalculoIsc> desde(Object codigo) {
        return Arrays.stream(values()).filter(s -> s.codigo.equals(codigo)).findFirst();
    }
}
