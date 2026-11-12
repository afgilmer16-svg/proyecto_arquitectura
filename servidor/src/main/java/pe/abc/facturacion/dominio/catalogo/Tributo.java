package pe.abc.facturacion.dominio.catalogo;

/**
 * Fila del Catalogo 05 (Guia XML SUNAT v1.0, secciones 24-27 y ejemplo p. 81).
 *
 * @param codigo          {@code TaxScheme/cbc:ID}
 * @param nombre          {@code TaxScheme/cbc:Name}
 * @param tipoCodigo      {@code TaxScheme/cbc:TaxTypeCode}
 * @param categoriaUnece  {@code TaxCategory/cbc:ID} (UN/ECE 5305); {@code null} si no se informa
 */
public record Tributo(String codigo, String nombre, String tipoCodigo, String categoriaUnece) {

    public static final Tributo IGV = new Tributo("1000", "IGV", "VAT", "S");
    public static final Tributo ISC = new Tributo("2000", "ISC", "EXC", "S");
    public static final Tributo EXONERADO = new Tributo("9997", "EXONERADO", "VAT", "E");
    public static final Tributo INAFECTO = new Tributo("9998", "INAFECTO", "FRE", "O");
    public static final Tributo GRATUITO = new Tributo("9996", "GRATUITO", "FRE", "Z");
    /** Catalogo 05 vigente (la guia v1.0 de 2017 es anterior a la Ley 30884). */
    public static final Tributo ICBPER = new Tributo("7152", "ICBPER", "OTH", null);

    /** Tributo del Catalogo 05 con el que se informa cada categoria de operacion. */
    public static Tributo porCategoria(CategoriaOperacion categoria) {
        return switch (categoria) {
            case GRAVADA -> IGV;
            case EXONERADA -> EXONERADO;
            case INAFECTA -> INAFECTO;
            case GRATUITA -> GRATUITO;
        };
    }
}
