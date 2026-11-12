package pe.abc.facturacion.dominio.calculo;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.List;
import java.util.function.Function;

import pe.abc.facturacion.dominio.catalogo.CategoriaOperacion;
import pe.abc.facturacion.dominio.catalogo.Catalogos;
import pe.abc.facturacion.dominio.modelo.Modelo.Factura;
import pe.abc.facturacion.dominio.modelo.Modelo.LineaFactura;

/**
 * Calculo de importes del comprobante con decimales exactos ({@link BigDecimal}, nunca {@code double}).
 *
 * <ol>
 *   <li>{@code valorVenta = cantidad x valorUnitario} (en gratuitas es el valor referencial).
 *   <li>El ISC se calcula <b>antes</b> que el IGV porque integra su base (Catalogo 08): 01 al valor,
 *       02 especifico y 03 al valor segun el precio de venta al publico.
 *   <li>{@code IGV = (valorVenta + ISC) x tasa} solo en operaciones gravadas.
 *   <li>{@code ICBPER = bolsas x importe vigente en la fecha de emision}.
 *   <li>Exoneradas, inafectas y gratuitas no generan IGV; las gratuitas no suman al importe a pagar.
 * </ol>
 *
 * <p>Cada importe se redondea a 2 decimales (HALF_UP) por linea y los totales suman los importes ya
 * redondeados, como hace SUNAT al validar el cuadre.
 */
public final class CalculadoraTributaria {

    private static final BigDecimal CIEN = BigDecimal.valueOf(100);
    public static final BigDecimal CERO = new BigDecimal("0.00");
    /** Equivale a la precision por defecto de decimal en Python (28 digitos, HALF_EVEN) en las divisiones. */
    private static final MathContext DIVISION = new MathContext(28, RoundingMode.HALF_EVEN);

    private CalculadoraTributaria() {
    }

    public static BigDecimal redondear(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_UP);
    }

    public static ImportesLinea calcularLinea(LineaFactura linea, BigDecimal tasaIgv, BigDecimal importeBolsa) {
        BigDecimal valorVenta = redondear(linea.cantidad().multiply(linea.valorUnitario()));
        BigDecimal tasa = tasaIgv.divide(CIEN);

        BigDecimal baseIsc = CERO;
        BigDecimal isc = CERO;
        if (linea.sistemaIsc() != null) {
            switch (linea.sistemaIsc()) {
                case AL_VALOR -> {
                    baseIsc = valorVenta;
                    isc = redondear(baseIsc.multiply(linea.tasaIsc()).divide(CIEN));
                }
                case ESPECIFICO -> {
                    baseIsc = valorVenta;
                    isc = redondear(linea.cantidad().multiply(linea.montoFijoIsc()));
                }
                case PRECIO_VENTA_PUBLICO -> {
                    baseIsc = redondear(linea.cantidad().multiply(linea.precioVentaPublicoSugerido())
                            .divide(BigDecimal.ONE.add(tasa), DIVISION));
                    isc = redondear(baseIsc.multiply(linea.tasaIsc()).divide(CIEN));
                }
            }
        }

        BigDecimal baseIgv;
        BigDecimal igv;
        if (linea.categoria() == CategoriaOperacion.GRAVADA) {
            baseIgv = valorVenta.add(isc);
            igv = redondear(baseIgv.multiply(tasa));
        } else {
            baseIgv = valorVenta;
            igv = CERO;
        }

        BigDecimal icbper = linea.cantidadBolsas() != null
                ? redondear(BigDecimal.valueOf(linea.cantidadBolsas()).multiply(importeBolsa)) : CERO;
        return new ImportesLinea(linea, valorVenta, baseIsc, isc, baseIgv, igv, icbper, importeBolsa, tasaIgv);
    }

    public static Totales calcularTotales(Factura factura) {
        BigDecimal tasaIgv = factura.regimenIgv().tasa();
        BigDecimal importeBolsa = Catalogos.importeIcbper(factura.fechaEmision());
        List<ImportesLinea> lineas = factura.lineas().stream()
                .map(l -> calcularLinea(l, tasaIgv, importeBolsa)).toList();

        BigDecimal totalValorVenta = suma(lineas.stream().filter(l -> !l.esGratuita()).toList(), ImportesLinea::valorVenta);
        return new Totales(
                lineas,
                sumaCategoria(lineas, CategoriaOperacion.GRAVADA),
                sumaCategoria(lineas, CategoriaOperacion.EXONERADA),
                sumaCategoria(lineas, CategoriaOperacion.INAFECTA),
                sumaCategoria(lineas, CategoriaOperacion.GRATUITA),
                totalValorVenta,
                suma(lineas.stream().filter(l -> l.linea().sistemaIsc() != null).toList(), ImportesLinea::baseIsc),
                suma(lineas, ImportesLinea::isc),
                suma(lineas, ImportesLinea::igv),
                suma(lineas, ImportesLinea::icbper),
                lineas.stream().mapToInt(l -> l.linea().cantidadBolsas() == null ? 0 : l.linea().cantidadBolsas()).sum());
    }

    private static BigDecimal sumaCategoria(List<ImportesLinea> lineas, CategoriaOperacion categoria) {
        return suma(lineas.stream().filter(l -> l.linea().categoria() == categoria).toList(), ImportesLinea::valorVenta);
    }

    private static BigDecimal suma(List<ImportesLinea> lineas, Function<ImportesLinea, BigDecimal> campo) {
        return lineas.stream().map(campo).reduce(CERO, BigDecimal::add);
    }

    /** Division con la misma precision que Python, para el precio unitario con tributos. */
    static BigDecimal dividir(BigDecimal dividendo, BigDecimal divisor) {
        return dividendo.divide(divisor, DIVISION);
    }
}
