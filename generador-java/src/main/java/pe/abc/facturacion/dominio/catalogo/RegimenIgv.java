package pe.abc.facturacion.dominio.catalogo;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Tasa de IGV (incluye IPM) segun el tipo de operacion.
 *
 * <ul>
 *   <li>{@code GENERAL}: 18 % (16 % IGV + 2 % IPM).
 *   <li>{@code RESTAURANTES_HOTELES}: 10 %, tasa especial explicada en clase. Su vigencia se debe confirmar
 *       en la norma (docs/pendientes-verificacion.md); nunca se aplica por defecto.
 * </ul>
 */
public enum RegimenIgv {
    GENERAL(new BigDecimal("18.00")),
    RESTAURANTES_HOTELES(new BigDecimal("10.00"));

    private final BigDecimal tasa;

    RegimenIgv(BigDecimal tasa) {
        this.tasa = tasa;
    }

    /** Tasa en porcentaje, por ejemplo {@code 18.00}. */
    public BigDecimal tasa() {
        return tasa;
    }

    public static Optional<RegimenIgv> desde(String nombre) {
        for (RegimenIgv r : values()) {
            if (r.name().equals(nombre)) {
                return Optional.of(r);
            }
        }
        return Optional.empty();
    }
}
