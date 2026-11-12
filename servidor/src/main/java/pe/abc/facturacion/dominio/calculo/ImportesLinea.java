package pe.abc.facturacion.dominio.calculo;

import java.math.BigDecimal;
import java.math.RoundingMode;

import pe.abc.facturacion.dominio.catalogo.CategoriaOperacion;
import pe.abc.facturacion.dominio.modelo.Modelo.LineaFactura;

/**
 * Importes calculados de una linea.
 *
 * @param valorVenta {@code cbc:LineExtensionAmount}
 */
public record ImportesLinea(LineaFactura linea, BigDecimal valorVenta, BigDecimal baseIsc, BigDecimal isc,
                            BigDecimal baseIgv, BigDecimal igv, BigDecimal icbper,
                            BigDecimal importeIcbperUnitario, BigDecimal tasaIgv) {

    public boolean esGratuita() {
        return linea.categoria() == CategoriaOperacion.GRATUITA;
    }

    /** {@code cac:InvoiceLine/cac:TaxTotal/cbc:TaxAmount}. */
    public BigDecimal totalTributos() {
        return igv.add(isc).add(icbper);
    }

    /**
     * Precio unitario con tributos (Catalogo 16 = 01): {@code (valor + ISC + IGV) / cantidad}.
     *
     * <p>No se redondea a 2 decimales: la Guia XML admite hasta 10 (n(12,10)) y, si se redondeara,
     * precio x cantidad dejaria de reconstruir el importe de la linea (bolsa: 1.18 / 10 = 0.118).
     */
    public BigDecimal precioUnitario() {
        if (esGratuita()) {
            return linea.valorUnitario();
        }
        BigDecimal exacto = MotorTributario.dividir(valorVenta.add(isc).add(igv), linea.cantidad());
        return exacto.setScale(10, RoundingMode.HALF_UP);
    }

    /** {@code cbc:Percent} del IGV: la tasa en gravadas y 0.00 en el resto (matriz 0005, seccion 8). */
    public BigDecimal tasaIgvInformada() {
        return linea.categoria() == CategoriaOperacion.GRAVADA ? tasaIgv : MotorTributario.CERO;
    }
}
