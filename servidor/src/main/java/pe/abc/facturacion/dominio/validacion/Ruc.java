package pe.abc.facturacion.dominio.validacion;

import java.util.List;

/** Validacion del RUC: 11 digitos, prefijo valido y digito verificador (modulo 11). */
public final class Ruc {

    private static final int[] PESOS = {5, 4, 3, 2, 7, 6, 5, 4, 3, 2};
    private static final List<String> PREFIJOS = List.of("10", "15", "16", "17", "20");

    private Ruc() {
    }

    public static boolean esValido(String ruc) {
        if (ruc == null || ruc.length() != 11 || !ruc.chars().allMatch(c -> c >= '0' && c <= '9')
                || !PREFIJOS.contains(ruc.substring(0, 2))) {
            return false;
        }
        int suma = 0;
        for (int i = 0; i < 10; i++) {
            suma += (ruc.charAt(i) - '0') * PESOS[i];
        }
        int verificador = 11 - (suma % 11);
        if (verificador == 10) {
            verificador = 0;
        } else if (verificador == 11) {
            verificador = 1;
        }
        return verificador == ruc.charAt(10) - '0';
    }
}
