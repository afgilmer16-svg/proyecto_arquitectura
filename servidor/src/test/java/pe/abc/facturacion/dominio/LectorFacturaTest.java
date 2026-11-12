package pe.abc.facturacion.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import pe.abc.facturacion.Ejemplos;
import pe.abc.facturacion.dominio.catalogo.RegimenIgv;
import pe.abc.facturacion.dominio.catalogo.TipoComprobante;
import pe.abc.facturacion.dominio.error.DatosFacturaInvalidosException;
import pe.abc.facturacion.dominio.modelo.Modelo.Factura;
import pe.abc.facturacion.dominio.validacion.LectorFactura;

/** Validacion de la entrada (capa 2): reglas de dominio, con todos los errores juntos y la ruta del campo. */
class LectorFacturaTest {

    /** archivo de specs/ejemplos/invalidos -> texto que debe aparecer en el error del dominio. */
    static Stream<Arguments> invalidos() {
        return Stream.of(
                Arguments.of("i01-boleta-con-serie-de-factura", "serie"),
                Arguments.of("i02-factura-a-cliente-con-dni", "receptor.tipo_documento"),
                Arguments.of("i03-ruc-con-digito-verificador-incorrecto", "emisor.ruc"),
                Arguments.of("i04-unidad-fuera-del-catalogo", "unidad_medida"),
                Arguments.of("i05-importe-como-numero-flotante", "valor_unitario"),
                Arguments.of("i06-credito-sin-cuotas", "forma_pago"),
                Arguments.of("i07-isc-al-valor-sin-tasa", "tasa_isc"),
                Arguments.of("i08-cliente-envia-correlativo", "correlativo"),
                Arguments.of("i09-afectacion-ivap-no-soportada", "tipo_afectacion_igv"),
                Arguments.of("i10-boleta-mayor-a-700-sin-documento", "boleta"),
                Arguments.of("i11-suma-de-cuotas-distinta-al-pendiente", "cuotas"),
                Arguments.of("i12-cuota-vence-antes-de-la-emision", "fecha_vencimiento"));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidos")
    void rechazaCadaEjemploInvalido(String archivo, String texto) {
        Map<String, Object> datos = Ejemplos.json(Ejemplos.INVALIDOS.resolve(archivo + ".json"));
        assertThatThrownBy(() -> LectorFactura.desdeMapa(datos, 1))
                .isInstanceOf(DatosFacturaInvalidosException.class)
                .hasMessageContaining(texto);
    }

    @Test
    void hayUnEjemploInvalidoPorCadaCasoProbado() {
        assertThat(Ejemplos.listar(Ejemplos.INVALIDOS)).hasSize((int) invalidos().count());
    }

    @Test
    void aceptaLosEjemplosValidos() {
        for (Path p : Ejemplos.listar(Ejemplos.VALIDOS)) {
            Factura f = LectorFactura.desdeMapa(Ejemplos.json(p), 1);
            assertThat(f.lineas()).isNotEmpty();
        }
        Factura parcial = LectorFactura.desdeMapa(Ejemplos.parcial(), 42);
        assertThat(parcial.numero()).isEqualTo("F001-00000042");
        assertThat(parcial.tipoComprobante()).isEqualTo(TipoComprobante.FACTURA);
        assertThat(parcial.regimenIgv()).isEqualTo(RegimenIgv.GENERAL);
        assertThat(parcial.lineas()).hasSize(7);
    }

    // ------------------------------------------------------------ utilidades
    @SuppressWarnings("unchecked")
    private static Map<String, Object> copia() {
        Map<String, Object> d = new HashMap<>(Ejemplos.parcial());
        d.put("emisor", new HashMap<>((Map<String, Object>) d.get("emisor")));
        d.put("receptor", new HashMap<>((Map<String, Object>) d.get("receptor")));
        List<Object> items = new ArrayList<>();
        for (Object i : (List<Object>) d.get("items")) {
            items.add(new HashMap<>((Map<String, Object>) i));
        }
        d.put("items", items);
        return d;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> item(Map<String, Object> datos, int i) {
        return (Map<String, Object>) ((List<Object>) datos.get("items")).get(i);
    }

    private static List<String> errores(Map<String, Object> datos) {
        try {
            LectorFactura.desdeMapa(datos, 1);
        } catch (DatosFacturaInvalidosException e) {
            return e.errores();
        }
        return List.of();
    }

    private static void esperaError(Map<String, Object> datos, String fragmento) {
        assertThat(errores(datos)).anyMatch(e -> e.contains(fragmento));
    }

    // ------------------------------------------------------------- reglas
    @Test
    void informaTodosLosErroresJuntosConSuRuta() {
        Map<String, Object> d = copia();
        d.put("serie", "X001");
        item(d, 0).put("unidad_medida", "SACO");
        item(d, 1).put("cantidad", "0");
        List<String> e = errores(d);
        assertThat(e).hasSizeGreaterThanOrEqualTo(3);
        assertThat(e).anyMatch(m -> m.startsWith("serie:"))
                .anyMatch(m -> m.startsWith("items[0].unidad_medida:"))
                .anyMatch(m -> m.startsWith("items[1].cantidad:"));
    }

    @Test
    void entradaNulaOTipoDeComprobanteDeOtroXsd() {
        assertThatThrownBy(() -> LectorFactura.desdeMapa(null, 1)).isInstanceOf(DatosFacturaInvalidosException.class);
        Map<String, Object> d = copia();
        d.put("tipo_comprobante", "07");
        esperaError(d, "UBL-CreditNote-2.1.xsd");
        d.put("tipo_comprobante", "99");
        esperaError(d, "debe ser '01' (factura) o '03' (boleta)");
    }

    @Test
    void correlativoFueraDeRango() {
        for (long malo : new long[] {0, -1, 100_000_000L}) {
            assertThatThrownBy(() -> LectorFactura.desdeMapa(Ejemplos.parcial(), malo))
                    .isInstanceOf(DatosFacturaInvalidosException.class).hasMessageContaining("correlativo");
        }
        assertThat(LectorFactura.desdeMapa(Ejemplos.parcial(), 99_999_999L).numero()).isEqualTo("F001-99999999");
    }

    @Test
    void fechaHoraMonedaYOperacion() {
        Map<String, Object> d = copia();
        d.put("fecha_emision", "11/11/2026");
        d.put("hora_emision", "25:61:00");
        d.put("moneda", "EUR");
        d.put("tipo_operacion", "0200");
        esperaError(d, "fecha_emision: debe tener formato AAAA-MM-DD");
        esperaError(d, "hora_emision: debe tener formato HH:MM:SS");
        esperaError(d, "moneda: debe ser una de");
        esperaError(d, "tipo_operacion");
        d = copia();
        d.remove("fecha_emision");
        esperaError(d, "fecha_emision: es obligatorio");
        d = copia();
        d.put("fecha_emision", 20261111);
        esperaError(d, "fecha_emision: debe tener formato");
    }

    @Test
    void emisorYReceptor() {
        Map<String, Object> d = copia();
        d.remove("emisor");
        d.remove("receptor");
        esperaError(d, "emisor: es obligatorio");
        esperaError(d, "receptor: es obligatorio");

        d = copia();
        @SuppressWarnings("unchecked") Map<String, Object> emisor = (Map<String, Object>) d.get("emisor");
        emisor.put("ubigeo", "12");
        emisor.put("razon_social", "X".repeat(101));
        emisor.put("direccion", 5);
        esperaError(d, "emisor.ubigeo: debe tener 6 dígitos");
        esperaError(d, "emisor.razon_social: supera 100 caracteres");
        esperaError(d, "emisor.direccion: debe ser texto");

        d = copia();
        @SuppressWarnings("unchecked") Map<String, Object> receptor = (Map<String, Object>) d.get("receptor");
        receptor.put("tipo_documento", "9");
        esperaError(d, "receptor.tipo_documento: debe ser un código del Catálogo 06");
        receptor.put("tipo_documento", "6");
        receptor.put("numero_documento", "20555555551");
        esperaError(d, "receptor.numero_documento: RUC inválido");
    }

    @Test
    void boletaConDniYSinDocumento() {
        Map<String, Object> d = copia();
        d.put("tipo_comprobante", "03");
        d.put("serie", "B001");
        @SuppressWarnings("unchecked") Map<String, Object> receptor = (Map<String, Object>) d.get("receptor");
        receptor.put("tipo_documento", "1");
        receptor.put("numero_documento", "1234");
        esperaError(d, "el DNI debe tener 8 dígitos");
        receptor.put("numero_documento", "12345678");
        assertThat(errores(d)).isEmpty();
        receptor.put("tipo_documento", "0");
        receptor.put("numero_documento", "-");
        esperaError(d, "una boleta mayor a S/ 700.00 debe identificar al comprador");
    }

    @Test
    void lineas() {
        Map<String, Object> d = copia();
        d.put("items", List.of());
        esperaError(d, "items: debe contener al menos una línea");
        d.put("items", List.of("no es un objeto"));
        esperaError(d, "items[0]: debe ser un objeto");

        d = copia();
        item(d, 0).put("numero_linea", 3);
        item(d, 0).put("cantidad", "-1");
        item(d, 0).put("valor_unitario", "1,5");
        item(d, 0).put("tipo_afectacion_igv", "99");
        item(d, 0).put("cantidad_bolsas", 0);
        esperaError(d, "items[0].numero_linea: debe ser 1");
        esperaError(d, "items[0].cantidad: debe ser un decimal exacto");
        esperaError(d, "items[0].valor_unitario: debe ser un decimal exacto");
        esperaError(d, "items[0].tipo_afectacion_igv: '99' no es un código del Catálogo 07");
        esperaError(d, "items[0].cantidad_bolsas: debe ser un entero mayor o igual a 1");

        d = copia();
        item(d, 0).put("cantidad", "0");
        esperaError(d, "items[0].cantidad: debe ser mayor que cero");
        item(d, 0).put("codigo_producto", "");
        esperaError(d, "items[0].codigo_producto: es obligatorio");
    }

    @Test
    void iscPorSistema() {
        Map<String, Object> d = copia();
        item(d, 0).put("sistema_isc", "09");
        esperaError(d, "debe ser '01', '02' o '03'");

        d = copia();
        item(d, 0).put("sistema_isc", "02");
        esperaError(d, "monto_fijo_isc: es obligatorio");
        esperaError(d, "unidad_base_isc: es obligatorio en el sistema '02'");

        d = copia();
        item(d, 0).put("sistema_isc", "03");
        esperaError(d, "precio_venta_publico_sugerido: es obligatorio");
        esperaError(d, "tasa_isc: es obligatorio");

        d = copia();
        item(d, 4).put("sistema_isc", "01");
        item(d, 4).put("tasa_isc", "10.00");
        esperaError(d, "el ISC solo se admite en líneas gravadas");

        d = copia();
        item(d, 0).put("monto_fijo_isc", "1.00");
        esperaError(d, "items[0].monto_fijo_isc: requiere 'sistema_isc'");
    }

    @Test
    void formaDePago() {
        Map<String, Object> d = copia();
        d.put("forma_pago", "contado");
        esperaError(d, "forma_pago: debe ser un objeto");
        d.put("forma_pago", Map.of("tipo", "Trueque"));
        esperaError(d, "forma_pago.tipo: debe ser 'Contado' o 'Credito'");
        d.put("forma_pago", Map.of("tipo", "Credito", "monto_neto_pendiente", "100.00", "cuotas", List.of("x")));
        esperaError(d, "forma_pago.cuotas[0]: debe ser un objeto");
        d.put("forma_pago", Map.of("tipo", "Credito", "monto_neto_pendiente", "99999.00",
                "cuotas", List.of(Map.of("monto", "99999.00", "fecha_vencimiento", "2027-01-01"))));
        esperaError(d, "no puede superar el importe total del comprobante");
        d.remove("forma_pago");
        assertThat(errores(d)).isEmpty();
        d.put("forma_pago", Map.of());
        assertThat(errores(d)).isEmpty();
    }

    @Test
    void aceptaImportesConHastaDiezDecimales() {
        Map<String, Object> d = copia();
        item(d, 0).put("valor_unitario", "140.1234567890");
        assertThat(errores(d)).isEmpty();
        item(d, 0).put("valor_unitario", "140.12345678901");
        esperaError(d, "decimal exacto");
        assertThat(new BigDecimal("140.1234567890")).isNotNull();
    }
}
