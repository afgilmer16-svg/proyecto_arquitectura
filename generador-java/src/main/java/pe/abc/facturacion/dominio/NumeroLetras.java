// Autor: Gilmer Ayala Falcón · UNAS-FIIS · Arquitectura de Software · Grupo A
package pe.abc.facturacion.dominio;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Importe en letras para la leyenda 1000 del Catalogo 52 ({@code cbc:Note}). Formato de la matriz 0005
 * (seccion 18.3): {@code <ENTERO EN LETRAS> CON <CENTIMOS>/100 <MONEDA>}.
 */
public final class NumeroLetras {

    private static final String[] UNIDADES = {"CERO", "UNO", "DOS", "TRES", "CUATRO", "CINCO", "SEIS", "SIETE",
        "OCHO", "NUEVE", "DIEZ", "ONCE", "DOCE", "TRECE", "CATORCE", "QUINCE", "DIECISEIS", "DIECISIETE",
        "DIECIOCHO", "DIECINUEVE", "VEINTE", "VEINTIUNO", "VEINTIDOS", "VEINTITRES", "VEINTICUATRO",
        "VEINTICINCO", "VEINTISEIS", "VEINTISIETE", "VEINTIOCHO", "VEINTINUEVE"};
    private static final String[] DECENAS = {"", "", "", "TREINTA", "CUARENTA", "CINCUENTA", "SESENTA", "SETENTA",
        "OCHENTA", "NOVENTA"};
    private static final String[] CENTENAS = {"", "CIENTO", "DOSCIENTOS", "TRESCIENTOS", "CUATROCIENTOS",
        "QUINIENTOS", "SEISCIENTOS", "SETECIENTOS", "OCHOCIENTOS", "NOVECIENTOS"};

    public static final long MAXIMO = 999_999_999_999L;

    private NumeroLetras() {
    }

    private static String menorMil(int n) {
        if (n == 100) {
            return "CIEN";
        }
        int centena = n / 100;
        int resto = n % 100;
        StringBuilder sb = new StringBuilder();
        if (centena > 0) {
            sb.append(CENTENAS[centena]);
        }
        if (resto > 0) {
            if (sb.length() > 0) {
                sb.append(' ');
            }
            if (resto < 30) {
                sb.append(UNIDADES[resto]);
            } else {
                int decena = resto / 10;
                int unidad = resto % 10;
                sb.append(DECENAS[decena]);
                if (unidad > 0) {
                    sb.append(" Y ").append(UNIDADES[unidad]);
                }
            }
        }
        return sb.toString();
    }

    /** Convierte un entero de 0 a 999 999 999 999 a letras (mayusculas, sin tildes). */
    public static String enteroEnLetras(long n) {
        if (n < 0 || n > MAXIMO) {
            throw new IllegalArgumentException("fuera de rango: " + n);
        }
        if (n == 0) {
            return "CERO";
        }
        long millones = n / 1_000_000;
        long resto = n % 1_000_000;
        int miles = (int) (resto / 1000);
        int unidades = (int) (resto % 1000);
        StringBuilder sb = new StringBuilder();
        if (millones > 0) {
            sb.append(millones == 1 ? "UN MILLON" : enteroEnLetras(millones) + " MILLONES");
        }
        if (miles > 0) {
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(miles == 1 ? "MIL" : menorMil(miles) + " MIL");
        }
        if (unidades > 0) {
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(menorMil(unidades));
        }
        // "UNO" se apocopa a "UN" delante de MIL / MILLONES (por ejemplo, VEINTIUN MIL).
        return sb.toString().replace("UNO MIL", "UN MIL").replace("UNO MILLONES", "UN MILLONES")
                .replace("VEINTIUNO MIL", "VEINTIUN MIL");
    }

    /** {@code 2130.18, "SOLES"} produce {@code DOS MIL CIENTO TREINTA CON 18/100 SOLES}. */
    public static String importeEnLetras(BigDecimal importe, String moneda) {
        BigDecimal redondeado = importe.setScale(2, RoundingMode.HALF_EVEN);
        long entero = redondeado.toBigInteger().longValueExact();
        int centimos = redondeado.subtract(new BigDecimal(entero)).movePointRight(2).intValueExact();
        return String.format("%s CON %02d/100 %s", enteroEnLetras(entero), centimos, moneda);
    }
}
