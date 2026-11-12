package pe.abc.facturacion.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import pe.abc.facturacion.dominio.catalogo.CategoriaOperacion;
import pe.abc.facturacion.dominio.catalogo.RegimenIgv;
import pe.abc.facturacion.dominio.catalogo.SistemaCalculoIsc;
import pe.abc.facturacion.dominio.catalogo.ClaseComprobante;
import pe.abc.facturacion.dominio.catalogo.TipoDocumentoIdentidad;
import pe.abc.facturacion.dominio.catalogo.TributoUbl;
import pe.abc.facturacion.dominio.validacion.ValidadorRuc;

class RucYNumeroLetrasTest {

    @ParameterizedTest
    @ValueSource(strings = {"20526018151", "20555555556"})
    void rucValidoConDigitoVerificador(String ruc) {
        assertThat(ValidadorRuc.esValido(ruc)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"20601234566", "2060123456", "205260181510", "ABCDEFGHIJK", "30601234565", ""})
    void rucInvalido(String ruc) {
        assertThat(ValidadorRuc.esValido(ruc)).isFalse();
    }

    @Test
    void rucNuloEsInvalido() {
        assertThat(ValidadorRuc.esValido(null)).isFalse();
    }

    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource({"0,CERO", "1,UNO", "15,QUINCE", "21,VEINTIUNO", "30,TREINTA", "31,TREINTA Y UNO", "100,CIEN",
            "101,CIENTO UNO", "999,NOVECIENTOS NOVENTA Y NUEVE", "1000,MIL", "1001,MIL UNO",
            "21000,VEINTIUN MIL", "2342,DOS MIL TRESCIENTOS CUARENTA Y DOS", "1000000,UN MILLON",
            "2000000,DOS MILLONES", "21000000,VEINTIUN MILLONES"})
    void enteroEnLetras(long numero, String letras) {
        assertThat(NumeroLetras.enteroEnLetras(numero)).isEqualTo(letras);
    }

    @Test
    void fueraDeRango() {
        assertThatThrownBy(() -> NumeroLetras.enteroEnLetras(-1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> NumeroLetras.enteroEnLetras(NumeroLetras.MAXIMO + 1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void importeEnLetrasConCentimosYMoneda() {
        assertThat(NumeroLetras.importeEnLetras(new BigDecimal("2130.18"), "SOLES"))
                .isEqualTo("DOS MIL CIENTO TREINTA CON 18/100 SOLES");
        assertThat(NumeroLetras.importeEnLetras(new BigDecimal("0.05"), "SOLES")).isEqualTo("CERO CON 05/100 SOLES");
        assertThat(NumeroLetras.importeEnLetras(new BigDecimal("423225"), "SOLES"))
                .isEqualTo("CUATROCIENTOS VEINTITRES MIL DOSCIENTOS VEINTICINCO CON 00/100 SOLES");
    }

    @Test
    void catalogos() {
        assertThat(ClaseComprobante.desde("01")).contains(ClaseComprobante.FACTURA);
        assertThat(ClaseComprobante.desde("03").orElseThrow().prefijoSerie()).isEqualTo('B');
        assertThat(ClaseComprobante.desde("99")).isEmpty();
        assertThat(TipoDocumentoIdentidad.desde("6")).contains(TipoDocumentoIdentidad.RUC);
        assertThat(TipoDocumentoIdentidad.desde(null)).isEmpty();
        assertThat(SistemaCalculoIsc.desde("02").orElseThrow().codigo()).isEqualTo("02");
        assertThat(SistemaCalculoIsc.desde("07")).isEmpty();
        assertThat(RegimenIgv.desde("GENERAL")).contains(RegimenIgv.GENERAL);
        assertThat(RegimenIgv.desde("X")).isEmpty();
        assertThat(RegimenIgv.RESTAURANTES_HOTELES.tasa()).isEqualByComparingTo("10.00");
        assertThat(CategoriaOperacion.GRATUITA.codigo()).isEqualTo("1004");
    }

    @Test
    void cadaCategoriaTieneSuTributoDelCatalogo05() {
        assertThat(TributoUbl.porCategoria(CategoriaOperacion.GRAVADA)).isEqualTo(TributoUbl.IGV);
        assertThat(TributoUbl.porCategoria(CategoriaOperacion.EXONERADA).codigo()).isEqualTo("9997");
        assertThat(TributoUbl.porCategoria(CategoriaOperacion.INAFECTA).codigo()).isEqualTo("9998");
        assertThat(TributoUbl.porCategoria(CategoriaOperacion.GRATUITA).codigo()).isEqualTo("9996");
        assertThat(TributoUbl.ISC.codigo()).isEqualTo("2000");
        assertThat(TributoUbl.ICBPER.codigo()).isEqualTo("7152");
    }
}
