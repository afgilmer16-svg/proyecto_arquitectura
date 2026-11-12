package pe.abc.facturacion.dominio.catalogo;

import java.util.Optional;

/** Catalogo 08: sistema de calculo del ISC (Apendice IV del TUO de la Ley del IGV e ISC). */
public enum SistemaIsc {
    AL_VALOR("01"),
    ESPECIFICO("02"),
    PRECIO_VENTA_PUBLICO("03");

    private final String codigo;

    SistemaIsc(String codigo) {
        this.codigo = codigo;
    }

    public String codigo() {
        return codigo;
    }

    public static Optional<SistemaIsc> desde(Object codigo) {
        for (SistemaIsc s : values()) {
            if (s.codigo.equals(codigo)) {
                return Optional.of(s);
            }
        }
        return Optional.empty();
    }
}
