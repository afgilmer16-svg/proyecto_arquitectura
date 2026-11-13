// Autor: Gilmer Ayala Falcón · UNAS-FIIS · Arquitectura de Software · Grupo A
package pe.abc.facturacion.dominio.calculo;

import java.math.BigDecimal;
import java.util.List;

/**
 * Totales del comprobante.
 *
 * @param totalGravadas valor de venta gravado, sin ISC (Guia XML seccion 23)
 * @param totalValorVenta {@code LegalMonetaryTotal/LineExtensionAmount} (sin gratuitas)
 */
public record Totales(List<ImportesLinea> lineas, BigDecimal totalGravadas, BigDecimal totalExoneradas,
                      BigDecimal totalInafectas, BigDecimal totalGratuitas, BigDecimal totalValorVenta,
                      BigDecimal baseIsc, BigDecimal totalIsc, BigDecimal totalIgv, BigDecimal totalIcbper,
                      int bolsas) {

    public BigDecimal totalTributos() {
        return totalIgv.add(totalIsc).add(totalIcbper);
    }

    /** {@code TaxInclusiveAmount} y {@code PayableAmount} (sin descuentos ni cargos globales). */
    public BigDecimal importeTotal() {
        return totalValorVenta.add(totalTributos());
    }

    public boolean soloGratuitas() {
        return lineas.stream().allMatch(ImportesLinea::esGratuita);
    }
}
