// Autor: Gilmer Ayala Falcón · UNAS-FIIS · Arquitectura de Software · Grupo A
package pe.abc.facturacion.infraestructura.web;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongSupplier;

/**
 * Asigna el correlativo de cada serie en el servidor: el formulario nunca lo envia.
 *
 * <p>El primer numero de una serie se toma de un valor inicial (por defecto, segundos desde 1970 acotados a
 * 8 digitos) y luego sube de uno en uno. Asi una serie reutilizada despues de reiniciar el servidor no repite
 * numeros que el OSE ya registro (error 1033, comprobante registrado previamente).
 */
public final class ContadorCorrelativos {

    /** Maximo que admite el comprobante: 8 digitos. */
    public static final long MAXIMO = 99_999_999L;

    private final Map<String, AtomicLong> porSerie = new ConcurrentHashMap<>();
    private final LongSupplier inicio;

    public ContadorCorrelativos() {
        this(() -> System.currentTimeMillis() / 1000 % 90_000_000L + 1);
    }

    /** @param inicio valor del que parte cada serie nueva (para pruebas deterministas) */
    public ContadorCorrelativos(LongSupplier inicio) {
        this.inicio = inicio;
    }

    /** @return el siguiente correlativo de la serie, entre 1 y {@link #MAXIMO} */
    public long siguiente(String serie) {
        AtomicLong contador = porSerie.computeIfAbsent(serie, s -> new AtomicLong(inicio.getAsLong() - 1));
        long numero = contador.incrementAndGet();
        if (numero > MAXIMO) {
            throw new IllegalStateException("La serie " + serie + " agotó sus 99 999 999 números.");
        }
        return numero;
    }
}
