// Autor: Gilmer Ayala Falcón · UNAS-FIIS · Arquitectura de Software · Grupo A
package pe.abc.facturacion.dominio.catalogo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Catalogos SUNAT y constantes de UBL 2.1 usados por el generador.
 *
 * <p>Fuentes locales: {@code docs/referencias/ubl/UBL-Invoice-2.1.xsd} (OASIS UBL 2.1 OS) y
 * {@code docs/referencias/sunat/guia-xml-factura-ubl21-v1.0.pdf} (SUNAT, mayo 2017). Lo que no figura
 * en esas fuentes esta marcado como "Por verificar" en {@code docs/pendientes-verificacion.md}.
 */
public final class Catalogos {

    private Catalogos() {
    }

    // Constantes de la cabecera (Guia XML SUNAT v1.0, pp. 4 y 79).
    public static final String UBL_VERSION_ID = "2.1";
    public static final String CUSTOMIZATION_ID = "2.0";
    public static final String AGENCIA_SUNAT = "PE:SUNAT";
    public static final String AGENCIA_UNECE = "United Nations Economic Commission for Europe";

    /** Catalogo 51: tipo de operacion por defecto (venta interna). */
    public static final String TIPO_OPERACION_VENTA_INTERNA = "0101";
    /** Codigo de establecimiento anexo: "0000" es el domicilio fiscal (Guia XML v1.0, p. 7). */
    public static final String CODIGO_DOMICILIO_FISCAL = "0000";

    // Catalogo 16: tipo de precio de venta unitario.
    public static final String PRECIO_UNITARIO_CON_TRIBUTOS = "01";
    public static final String VALOR_REFERENCIAL_GRATUITO = "02";

    // Catalogo 52: leyendas.
    public static final String LEYENDA_MONTO_EN_LETRAS = "1000";
    public static final String LEYENDA_TRANSFERENCIA_GRATUITA = "1002";
    public static final String TEXTO_TRANSFERENCIA_GRATUITA =
            "TRANSFERENCIA GRATUITA DE UN BIEN Y/O SERVICIO PRESTADO GRATUITAMENTE";

    /** URI del catalogo SUNAT numero {@code n} (por ejemplo, 07 para el tipo de afectacion del IGV). */
    public static String uriCatalogo(int n) {
        return String.format("urn:pe:gob:sunat:cpe:see:gem:catalogos:catalogo%02d", n);
    }

    /** Las notas de credito (07) y debito (08) usan otros XSD: fuera del alcance de este generador. */
    public static final Map<String, String> TIPOS_COMPROBANTE_OTRO_XSD =
            Map.of("07", "UBL-CreditNote-2.1.xsd", "08", "UBL-DebitNote-2.1.xsd");

    /** Catalogo 02: monedas admitidas por el contrato (ISO 4217). */
    public static final Map<String, String> MONEDAS = Map.of("PEN", "SOLES", "USD", "DÓLARES AMERICANOS");

    /** Catalogo 03: unidades de medida (UN/ECE rec 20) admitidas por el contrato. */
    public static final Set<String> UNIDADES_MEDIDA = Set.of(
            "NIU", "ZZ", "KGM", "GRM", "TNE", "LTR", "MLT", "GLL", "MTR", "BX", "BG", "SA", "PK", "DZN", "CS");

    /** Catalogo 07 admitido: codigo de afectacion del IGV -> categoria de la operacion. */
    public static final Map<String, CategoriaOperacion> CATEGORIA_POR_AFECTACION;

    /** Afectaciones del Catalogo 07 que existen en el contrato pero no se generan en este parcial. */
    public static final Map<String, String> AFECTACIONES_NO_SOPORTADAS;

    static {
        Map<String, CategoriaOperacion> afectaciones = new TreeMap<>();
        afectaciones.put("10", CategoriaOperacion.GRAVADA);
        afectaciones.put("20", CategoriaOperacion.EXONERADA);
        afectaciones.put("21", CategoriaOperacion.GRATUITA);
        afectaciones.put("30", CategoriaOperacion.INAFECTA);
        for (int c = 31; c <= 37; c++) {
            afectaciones.put(String.valueOf(c), CategoriaOperacion.GRATUITA);
        }
        CATEGORIA_POR_AFECTACION = Map.copyOf(afectaciones);

        Map<String, String> noSoportadas = new TreeMap<>();
        for (int c = 11; c <= 16; c++) {
            noSoportadas.put(String.valueOf(c), "gravado - retiro / transferencia gratuita (el IGV lo asume el emisor)");
        }
        noSoportadas.put("17", "IVAP (Impuesto a la Venta del Arroz Pilado)");
        noSoportadas.put("40", "exportación (requiere otro tipo de operación, Catálogo 51)");
        AFECTACIONES_NO_SOPORTADAS = Map.copyOf(noSoportadas);
    }

    private static final Map<Integer, BigDecimal> ICBPER_POR_ANIO = Map.of(
            2019, new BigDecimal("0.10"), 2020, new BigDecimal("0.20"),
            2021, new BigDecimal("0.30"), 2022, new BigDecimal("0.40"));
    private static final BigDecimal ICBPER_DESDE_2023 = new BigDecimal("0.50");

    /**
     * Importe del ICBPER por bolsa vigente en la fecha de emision (Ley 30884):
     * 2019 0.10, 2020 0.20, 2021 0.30, 2022 0.40 y desde 2023 0.50.
     */
    public static BigDecimal importeIcbper(LocalDate fechaEmision) {
        if (fechaEmision.getYear() < 2019) {
            throw new IllegalArgumentException("El ICBPER rige desde el 01/08/2019");
        }
        return ICBPER_POR_ANIO.getOrDefault(fechaEmision.getYear(), ICBPER_DESDE_2023);
    }
}
