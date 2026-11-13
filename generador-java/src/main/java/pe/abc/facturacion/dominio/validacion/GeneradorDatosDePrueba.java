// Autor: Gilmer Ayala Falcón · UNAS-FIIS · Arquitectura de Software · Grupo A
package pe.abc.facturacion.dominio.validacion;

import java.util.Random;

/**
 * Inventa datos de prueba que cumplen las reglas de forma del comprobante: un RUC con prefijo 20 y digito
 * verificador por modulo 11, y una serie {@code Fxxx} o {@code Bxxx}.
 *
 * <p>El digito verificador solo asegura que el numero esta bien formado. Que un OSE lo acepte depende ademas
 * de su padron de contribuyentes, que esta fuera del alcance de este codigo.
 */
public final class GeneradorDatosDePrueba {

    private static final int[] FACTORES = {5, 4, 3, 2, 7, 6, 5, 4, 3, 2};
    private static final String CARACTERES_SERIE = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

    private GeneradorDatosDePrueba() {
    }

    /** RUC de 11 digitos que empieza con 20 y pasa {@link ValidadorRuc#esValido(String)}. */
    public static String rucAleatorio(Random azar) {
        StringBuilder ruc = new StringBuilder("20");
        for (int i = 0; i < 8; i++) {
            ruc.append(azar.nextInt(10));
        }
        int suma = 0;
        for (int i = 0; i < 10; i++) {
            suma += (ruc.charAt(i) - '0') * FACTORES[i];
        }
        ruc.append((11 - suma % 11) % 10);
        return ruc.toString();
    }

    /**
     * @param prefijo {@code 'F'} para factura o {@code 'B'} para boleta
     * @return serie de 4 caracteres, por ejemplo {@code F7K2}
     */
    public static String serieAleatoria(char prefijo, Random azar) {
        StringBuilder serie = new StringBuilder().append(prefijo);
        for (int i = 0; i < 3; i++) {
            serie.append(CARACTERES_SERIE.charAt(azar.nextInt(CARACTERES_SERIE.length())));
        }
        return serie.toString();
    }
}
