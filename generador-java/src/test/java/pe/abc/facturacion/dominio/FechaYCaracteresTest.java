// Autor: Gilmer Ayala Falcón · UNAS-FIIS · Arquitectura de Software · Grupo A
package pe.abc.facturacion.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import pe.abc.facturacion.Ejemplos;
import pe.abc.facturacion.dominio.catalogo.RegimenIgv;
import pe.abc.facturacion.dominio.error.DatosFacturaInvalidosException;
import pe.abc.facturacion.dominio.validacion.LectorFactura;

/** Fecha de emision no futura y caracteres que XML 1.0 no admite (mismas reglas que el prototipo Python). */
class FechaYCaracteresTest {

    private static final LocalDate HOY = LocalDate.of(2026, 11, 12);
    private static final Path MOTO = Ejemplos.VALIDOS.resolve("05-factura-moto.json");

    private static Map<String, Object> moto() {
        return Ejemplos.json(MOTO);
    }

    @SuppressWarnings("unchecked")
    private static void descripcion(Map<String, Object> datos, String texto) {
        ((Map<String, Object>) ((List<Object>) datos.get("items")).get(0)).put("descripcion", texto);
    }

    @Test
    void laFacturaDeLaMotoEsValida() {
        assertThat(LectorFactura.desdeMapa(moto(), 1, RegimenIgv.GENERAL, HOY).lineas()).hasSize(1);
    }

    @Test
    void noSeEmiteConFechaFutura() {
        Map<String, Object> datos = new java.util.HashMap<>(moto());
        datos.put("fecha_emision", "2026-11-13");
        assertThatThrownBy(() -> LectorFactura.desdeMapa(datos, 1, RegimenIgv.GENERAL, HOY))
                .isInstanceOf(DatosFacturaInvalidosException.class)
                .hasMessageContaining("fecha_emision: no puede ser posterior a la fecha actual");
    }

    @Test
    void seEmiteConLaFechaDeHoy() {
        Map<String, Object> datos = new java.util.HashMap<>(moto());
        datos.put("fecha_emision", HOY.toString());
        assertThat(LectorFactura.desdeMapa(datos, 1, RegimenIgv.GENERAL, HOY).fechaEmision()).isEqualTo(HOY);
    }

    @Test
    void laFechaDeReferenciaPorDefectoEsLaActual() {
        Map<String, Object> datos = new java.util.HashMap<>(moto());
        datos.put("fecha_emision", "2999-01-01");
        assertThatThrownBy(() -> LectorFactura.desdeMapa(datos, 1))
                .isInstanceOf(DatosFacturaInvalidosException.class)
                .hasMessageContaining("posterior a la fecha actual");
        assertThat(LectorFactura.hoyEnLima()).isBetween(LocalDate.now().minusDays(1), LocalDate.now().plusDays(1));
    }

    @ParameterizedTest
    @ValueSource(strings = {"MOTO\u0007 ROJA", "MOTO\u0000", "MO\u001FTO", "MOTO￾", "MOTO\uD800 SUELTO", "MOTO\uDC00"})
    void rechazaCaracteresNoXml(String texto) {
        Map<String, Object> datos = new java.util.HashMap<>(moto());
        List<Object> items = new ArrayList<>((List<?>) datos.get("items"));
        items.set(0, new java.util.HashMap<>((Map<?, ?>) items.get(0)));
        datos.put("items", items);
        descripcion(datos, texto);
        assertThatThrownBy(() -> LectorFactura.desdeMapa(datos, 1, RegimenIgv.GENERAL, HOY))
                .isInstanceOf(DatosFacturaInvalidosException.class)
                .hasMessageContaining("caracteres de control");
    }

    @Test
    void admiteTabulacionSaltoYEmojiValido() {
        assertThat(LectorFactura.tieneCaracteresNoXml("a\tb\nc\r")).isFalse();
        assertThat(LectorFactura.tieneCaracteresNoXml("MOTO 🏍️")).isFalse();
    }
}
