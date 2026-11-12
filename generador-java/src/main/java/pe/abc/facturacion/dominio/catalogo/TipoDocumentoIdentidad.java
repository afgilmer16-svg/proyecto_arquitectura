package pe.abc.facturacion.dominio.catalogo;

import java.util.Optional;

/** Catalogo 06: tipo de documento de identidad. */
public enum TipoDocumentoIdentidad {
    SIN_DOCUMENTO("0"),
    DNI("1"),
    CARNE_EXTRANJERIA("4"),
    RUC("6"),
    PASAPORTE("7");

    private final String codigo;

    TipoDocumentoIdentidad(String codigo) {
        this.codigo = codigo;
    }

    public String codigo() {
        return codigo;
    }

    public static Optional<TipoDocumentoIdentidad> desde(Object codigo) {
        for (TipoDocumentoIdentidad t : values()) {
            if (t.codigo.equals(codigo)) {
                return Optional.of(t);
            }
        }
        return Optional.empty();
    }
}
