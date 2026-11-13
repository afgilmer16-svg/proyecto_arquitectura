// Autor: Gilmer Ayala Falcón · UNAS-FIIS · Arquitectura de Software · Grupo A
package pe.abc.facturacion.dominio.validacion;

import java.util.Set;
import java.util.stream.IntStream;

/**
 * Comprueba un numero de RUC: 11 digitos, prefijo de contribuyente conocido y digito verificador por
 * modulo 11 (los pesos 5-4-3-2-7-6-5-4-3-2 se aplican a los 10 primeros digitos).
 */
public final class ValidadorRuc {

    private static final int[] FACTORES = {5, 4, 3, 2, 7, 6, 5, 4, 3, 2};
    private static final Set<String> PREFIJOS = Set.of("10", "15", "16", "17", "20");

    private ValidadorRuc() {
    }

    public static boolean esValido(String ruc) {
        if (ruc == null || !ruc.matches("[0-9]{11}") || !PREFIJOS.contains(ruc.substring(0, 2))) {
            return false;
        }
        int suma = IntStream.range(0, 10).map(i -> digito(ruc, i) * FACTORES[i]).sum();
        // 11 - resto vale 10 u 11 solo en dos casos, que la norma convierte en 0 y 1: eso es "% 10".
        int esperado = (11 - suma % 11) % 10;
        return esperado == digito(ruc, 10);
    }

    private static int digito(String texto, int posicion) {
        return texto.charAt(posicion) - '0';
    }
}
