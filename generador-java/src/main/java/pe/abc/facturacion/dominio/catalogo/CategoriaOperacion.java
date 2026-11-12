package pe.abc.facturacion.dominio.catalogo;

/**
 * Categorias tributarias de la operacion vistas en clase (1001-1004).
 *
 * <p>En UBL 2.0 SUNAT estos codigos (Catalogo 14) iban en {@code sac:AdditionalMonetaryTotal}. Ese
 * elemento <b>no existe</b> en {@code UBL-Invoice-2.1.xsd}: en UBL 2.1 cada categoria se informa como un
 * {@code cac:TaxSubtotal} con su propio tributo del Catalogo 05 (ver {@link TributoUbl#porCategoria}).
 */
public enum CategoriaOperacion {
    GRAVADA("1001"),
    INAFECTA("1002"),
    EXONERADA("1003"),
    GRATUITA("1004");

    private final String codigo;

    CategoriaOperacion(String codigo) {
        this.codigo = codigo;
    }

    public String codigo() {
        return codigo;
    }
}
