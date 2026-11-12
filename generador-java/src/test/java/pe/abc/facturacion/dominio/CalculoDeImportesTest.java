package pe.abc.facturacion.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import pe.abc.facturacion.Ejemplos;
import pe.abc.facturacion.dominio.calculo.ImportesLinea;
import pe.abc.facturacion.dominio.calculo.CalculadoraTributaria;
import pe.abc.facturacion.dominio.calculo.Totales;
import pe.abc.facturacion.dominio.catalogo.Catalogos;
import pe.abc.facturacion.dominio.catalogo.RegimenIgv;
import pe.abc.facturacion.dominio.catalogo.SistemaCalculoIsc;
import pe.abc.facturacion.dominio.modelo.Modelo.LineaFactura;
import pe.abc.facturacion.dominio.validacion.LectorFactura;

/** Motor tributario: IGV, ISC (3 sistemas), ICBPER por anio y redondeo, con decimales exactos. */
class CalculoDeImportesTest {

    private static BigDecimal d(String v) {
        return new BigDecimal(v);
    }

    private static LineaFactura linea(String cantidad, String valor, String afectacion, SistemaCalculoIsc sistema,
                                      String tasaIsc, String montoFijo, String pvp, Integer bolsas) {
        return new LineaFactura(1, "P-1", "Producto", "NIU", d(cantidad), d(valor), afectacion, sistema,
                tasaIsc == null ? null : d(tasaIsc), montoFijo == null ? null : d(montoFijo),
                montoFijo == null ? null : "LTR", pvp == null ? null : d(pvp), bolsas);
    }

    private static ImportesLinea calcular(LineaFactura linea) {
        return CalculadoraTributaria.calcularLinea(linea, RegimenIgv.GENERAL.tasa(), d("0.50"));
    }

    @Test
    void elEjemploDelParcialCuadraAlCentimo() {
        Totales t = CalculadoraTributaria.calcularTotales(LectorFactura.desdeMapa(Ejemplos.parcial(), 1));
        assertThat(t.totalValorVenta()).isEqualByComparingTo("1955.50");
        assertThat(t.totalIsc()).isEqualByComparingTo("32.40");
        assertThat(t.totalIgv()).isEqualByComparingTo("349.45");
        assertThat(t.totalIcbper()).isEqualByComparingTo("5.00");
        assertThat(t.totalTributos()).isEqualByComparingTo("386.85");
        assertThat(t.importeTotal()).isEqualByComparingTo("2342.35");
        assertThat(t.totalGravadas()).isEqualByComparingTo("1909.00");
        assertThat(t.totalExoneradas()).isEqualByComparingTo("22.50");
        assertThat(t.totalInafectas()).isEqualByComparingTo("24.00");
        assertThat(t.totalGratuitas()).isEqualByComparingTo("6.00");
        assertThat(t.bolsas()).isEqualTo(10);
        assertThat(t.soloGratuitas()).isFalse();
    }

    @Test
    void igvDel18PorCientoSobreElValorDeVenta() {
        ImportesLinea l = calcular(linea("10", "140.00", "10", null, null, null, null, null));
        assertThat(l.valorVenta()).isEqualByComparingTo("1400.00");
        assertThat(l.igv()).isEqualByComparingTo("252.00");
        assertThat(l.precioUnitario()).isEqualByComparingTo("165.20");
        assertThat(l.tasaIgvInformada()).isEqualByComparingTo("18.00");
    }

    @Test
    void iscAlValorIntegraLaBaseDelIgv() {
        ImportesLinea l = calcular(linea("24", "4.50", "10", SistemaCalculoIsc.AL_VALOR, "30.00", null, null, null));
        assertThat(l.isc()).isEqualByComparingTo("32.40");
        assertThat(l.baseIgv()).isEqualByComparingTo("140.40");
        assertThat(l.igv()).isEqualByComparingTo("25.27");
    }

    @Test
    void iscEspecificoPorUnidad() {
        ImportesLinea l = calcular(linea("6", "18.00", "10", SistemaCalculoIsc.ESPECIFICO, null, "1.50", null, null));
        assertThat(l.isc()).isEqualByComparingTo("9.00");
        assertThat(l.baseIsc()).isEqualByComparingTo("108.00");
    }

    @Test
    void iscSobrePrecioDeVentaAlPublicoDescuentaElIgv() {
        ImportesLinea l = calcular(linea("10", "5.00", "10", SistemaCalculoIsc.PRECIO_VENTA_PUBLICO, "20.00", null, "11.80", null));
        assertThat(l.baseIsc()).isEqualByComparingTo("100.00");
        assertThat(l.isc()).isEqualByComparingTo("20.00");
    }

    @ParameterizedTest(name = "{0} -> igv {1}")
    @CsvSource({"20,0.00", "30,0.00", "31,0.00", "21,0.00"})
    void lasNoGravadasNoPagaIgv(String afectacion, String igv) {
        ImportesLinea l = calcular(linea("2", "10.00", afectacion, null, null, null, null, null));
        assertThat(l.igv()).isEqualByComparingTo(igv);
        assertThat(l.tasaIgvInformada()).isEqualByComparingTo("0.00");
    }

    @Test
    void laGratuitaSeInformaConSuValorReferencial() {
        ImportesLinea l = calcular(linea("5", "1.20", "31", null, null, null, null, null));
        assertThat(l.esGratuita()).isTrue();
        assertThat(l.valorVenta()).isEqualByComparingTo("6.00");
        assertThat(l.precioUnitario()).isEqualByComparingTo("1.20");
    }

    @Test
    void icbperPorBolsaYElPrecioUnitarioNoSeRedondeaADosDecimales() {
        ImportesLinea l = calcular(linea("10", "0.10", "10", null, null, null, null, 10));
        assertThat(l.icbper()).isEqualByComparingTo("5.00");
        assertThat(l.precioUnitario()).isEqualByComparingTo("0.118");
        assertThat(l.totalTributos()).isEqualByComparingTo("5.18");
    }

    @Test
    void redondeoHalfUpPorLinea() {
        assertThat(CalculadoraTributaria.redondear(d("0.125"))).isEqualByComparingTo("0.13");
        assertThat(CalculadoraTributaria.redondear(d("0.135"))).isEqualByComparingTo("0.14");
        assertThat(CalculadoraTributaria.redondear(d("2.675"))).isEqualByComparingTo("2.68");
    }

    @Test
    void regimenDeRestaurantesYHotelesUsaDiezPorCiento() {
        ImportesLinea l = CalculadoraTributaria.calcularLinea(linea("1", "100.00", "10", null, null, null, null, null),
                RegimenIgv.RESTAURANTES_HOTELES.tasa(), d("0.50"));
        assertThat(l.igv()).isEqualByComparingTo("10.00");
    }

    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource({"2019-08-01,0.10", "2020-06-15,0.20", "2021-12-31,0.30", "2022-01-01,0.40", "2023-01-01,0.50",
            "2026-11-11,0.50", "2030-05-05,0.50"})
    void icbperVigenteSegunElAnioDeEmision(String fecha, String esperado) {
        assertThat(Catalogos.importeIcbper(LocalDate.parse(fecha))).isEqualByComparingTo(esperado);
    }

    @Test
    void icbperNoRigeAntesDe2019() {
        assertThatThrownBy(() -> Catalogos.importeIcbper(LocalDate.parse("2018-12-31")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void elCalculoNoUsaPuntoFlotante() {
        // 0.1 x 3 en double daria 0.30000000000000004; con BigDecimal el valor es exacto
        ImportesLinea l = calcular(linea("3", "0.1", "20", null, null, null, null, null));
        assertThat(l.valorVenta().toPlainString()).isEqualTo("0.30");
    }
}
