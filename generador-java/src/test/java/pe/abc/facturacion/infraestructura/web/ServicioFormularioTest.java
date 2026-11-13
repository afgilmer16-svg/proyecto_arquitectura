// Autor: Gilmer Ayala Falcón · UNAS-FIIS · Arquitectura de Software · Grupo A
package pe.abc.facturacion.infraestructura.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import pe.abc.facturacion.aplicacion.GenerarFacturaXmlService;
import pe.abc.facturacion.dominio.error.DatosFacturaInvalidosException;
import pe.abc.facturacion.dominio.error.FirmaDigitalException;
import pe.abc.facturacion.dominio.validacion.ValidadorRuc;
import pe.abc.facturacion.infraestructura.ubl.GeneradorUblJaxbAdapter;

/** Lo que hace el formulario web, sin pasar por HTTP: vista previa, generacion, correlativos y firma opcional. */
class ServicioFormularioTest {

    private static String pfxBase64;

    private final ContadorCorrelativos correlativos = new ContadorCorrelativos(() -> 500L);
    private final ServicioFormulario servicio = new ServicioFormulario(
            new GenerarFacturaXmlService(new GeneradorUblJaxbAdapter()), correlativos, new Random(42));

    @BeforeAll
    static void certificadoDePrueba(@TempDir Path carpeta) throws Exception {
        Path almacen = carpeta.resolve("prueba.p12");
        Process proceso = new ProcessBuilder(
                Path.of(System.getProperty("java.home"), "bin", "keytool").toString(),
                "-genkeypair", "-alias", "prueba", "-keyalg", "RSA", "-keysize", "2048", "-validity", "30",
                "-storetype", "PKCS12", "-keystore", almacen.toString(), "-storepass", "clave-web",
                "-dname", "CN=PRUEBA WEB")
                .redirectErrorStream(true).start();
        proceso.getInputStream().readAllBytes();
        assertThat(proceso.waitFor(60, TimeUnit.SECONDS)).isTrue();
        pfxBase64 = Base64.getEncoder().encodeToString(Files.readAllBytes(almacen));
    }

    // ------------------------------------------------------------------ datos de entrada de las pruebas
    private static Map<String, Object> linea(String descripcion, String cantidad, String valor, String afectacion) {
        Map<String, Object> l = new LinkedHashMap<>();
        l.put("numero_linea", 1);
        l.put("codigo_producto", "PROD-01");
        l.put("descripcion", descripcion);
        l.put("unidad_medida", "NIU");
        l.put("cantidad", cantidad);
        l.put("valor_unitario", valor);
        l.put("tipo_afectacion_igv", afectacion);
        return l;
    }

    private static Map<String, Object> numerada(Map<String, Object> linea, int numero) {
        linea.put("numero_linea", numero);
        return linea;
    }

    private static Map<String, Object> factura(List<Map<String, Object>> items) {
        Map<String, Object> d = new LinkedHashMap<>();
        d.put("tipo_comprobante", "01");
        d.put("serie", "F9A1");
        d.put("emisor", new LinkedHashMap<>(Map.of("ruc", "20526018151", "razon_social", "EMISOR DE PRUEBA S.A.C.",
                "direccion", "Av. Prueba 100", "ubigeo", "150101")));
        d.put("receptor", new LinkedHashMap<>(Map.of("tipo_documento", "6", "numero_documento", "20555555556",
                "razon_social", "CLIENTE DE PRUEBA S.A.C.")));
        d.put("items", items);
        return d;
    }

    private static Map<String, Object> boleta(String tipoDoc, String numero, String nombre, String valor) {
        Map<String, Object> d = factura(List.of(linea("Producto", "1", valor, "10")));
        d.put("tipo_comprobante", "03");
        d.put("serie", "B9A1");
        d.put("receptor", new LinkedHashMap<>(Map.of("tipo_documento", tipoDoc, "numero_documento", numero,
                "razon_social", nombre)));
        return d;
    }

    // ------------------------------------------------------------------ vista previa
    @Test
    void laVistaPreviaCalculaValorIgvTotalYLetras() {
        Map<String, Object> r = servicio.calcular(Map.of("items", List.of(linea("Moto", "1", "6779.66", "10"))));
        assertThat(r).containsEntry("valor_venta", "6779.66").containsEntry("igv", "1220.34")
                .containsEntry("total", "8000.00").containsEntry("letras", "OCHO MIL CON 00/100 SOLES");
    }

    @Test
    void laVistaPreviaSumaVariosProductosYSeparaLasOperaciones() {
        Map<String, Object> r = servicio.calcular(Map.of("moneda", "USD", "items", List.of(
                linea("A", "2", "100", "10"), numerada(linea("B", "1", "50", "20"), 2),
                numerada(linea("C", "1", "10", "30"), 3))));
        assertThat(r).containsEntry("gravadas", "200.00").containsEntry("exoneradas", "50.00")
                .containsEntry("inafectas", "10.00").containsEntry("igv", "36.00")
                .containsEntry("total", "296.00").containsEntry("moneda", "USD");
        assertThat((String) r.get("letras")).endsWith("DÓLARES AMERICANOS");
    }

    @Test
    void laVistaPreviaRechazaUnaLineaIncompleta() {
        assertThatThrownBy(() -> servicio.calcular(Map.of("items", List.of(linea("", "1", "10", "10")))))
                .isInstanceOf(DatosFacturaInvalidosException.class);
        assertThatThrownBy(() -> servicio.calcular(Map.of("items", List.of(linea("X", "uno", "10", "10")))))
                .isInstanceOf(DatosFacturaInvalidosException.class);
    }

    // ------------------------------------------------------------------ generacion
    @Test
    void generaElXmlSinFirmaConLaMarcaYElCorrelativoDelServidor() {
        Map<String, Object> r = servicio.generar(factura(List.of(linea("Producto de prueba", "3", "100", "10"))));
        assertThat(r).containsEntry("comprobante", "F9A1-00000500").containsEntry("firmado", false)
                .containsEntry("archivo", "20526018151-01-F9A1-00000500.xml");
        String xml = (String) r.get("xml");
        assertThat(xml).contains("<cbc:ID>F9A1-00000500</cbc:ID>").contains("FIRMAR AQUÍ")
                .doesNotContain("ext:UBLExtensions>");
        assertThat(xml).contains("<cbc:PayableAmount currencyID=\"PEN\">354.00</cbc:PayableAmount>");
    }

    @Test
    void usaLaFechaYLaHoraDelMomentoCuandoNoSeEscriben() {
        String xml = (String) servicio.generar(factura(List.of(linea("P", "1", "10", "10")))).get("xml");
        assertThat(xml).containsPattern("<cbc:IssueDate>\\d{4}-\\d{2}-\\d{2}</cbc:IssueDate>")
                .containsPattern("<cbc:IssueTime>\\d{2}:\\d{2}:\\d{2}</cbc:IssueTime>");
    }

    @Test
    void cadaSerieLlevaSuPropiaCuentaYSigueSubiendo() {
        servicio.generar(factura(List.of(linea("P", "1", "10", "10"))));
        Map<String, Object> segunda = servicio.generar(factura(List.of(linea("P", "1", "10", "10"))));
        assertThat(segunda).containsEntry("comprobante", "F9A1-00000501");
        Map<String, Object> otraSerie = factura(List.of(linea("P", "1", "10", "10")));
        otraSerie.put("serie", "F9A2");
        assertThat(servicio.generar(otraSerie)).containsEntry("comprobante", "F9A2-00000500");
    }

    @Test
    void unaSolicitudInvalidaNoGastaCorrelativo() {
        Map<String, Object> mala = factura(List.of(linea("P", "1", "10", "10")));
        mala.put("serie", "B9A1"); // serie de boleta en una factura
        assertThatThrownBy(() -> servicio.generar(mala)).isInstanceOf(DatosFacturaInvalidosException.class);
        assertThat(servicio.generar(factura(List.of(linea("P", "1", "10", "10"))))).containsEntry("comprobante",
                "F9A1-00000500");
    }

    @Test
    void laBoletaMayorASetecientosExigeDni() {
        assertThatThrownBy(() -> servicio.generar(boleta("0", "0", "CLIENTES VARIOS", "800")))
                .isInstanceOf(DatosFacturaInvalidosException.class)
                .hasMessageContaining("700");
        assertThat(servicio.generar(boleta("1", "12345678", "JUAN PEREZ", "800"))).containsEntry("comprobante",
                "B9A1-00000500");
    }

    @Test
    void laBoletaHastaSetecientosPuedeIrSinDocumento() {
        assertThat(servicio.generar(boleta("0", "0", "CLIENTES VARIOS", "100"))).containsEntry("firmado", false);
    }

    @Test
    void elFormularioNoPuedeEnviarElCorrelativo() {
        Map<String, Object> d = factura(List.of(linea("P", "1", "10", "10")));
        d.put("correlativo", 7);
        assertThatThrownBy(() -> servicio.generar(d)).isInstanceOf(DatosFacturaInvalidosException.class);
    }

    // ------------------------------------------------------------------ firma opcional
    @Test
    void conCertificadoElXmlSaleFirmadoYSinLaMarca() {
        Map<String, Object> d = factura(List.of(linea("P", "1", "10", "10")));
        d.put("firma", Map.of("pfx_base64", pfxBase64, "clave", "clave-web"));
        Map<String, Object> r = servicio.generar(d);
        String xml = (String) r.get("xml");
        assertThat(r).containsEntry("firmado", true);
        assertThat(xml).contains("<ds:Signature").contains("<ext:UBLExtensions>").doesNotContain("FIRMAR AQUÍ");
    }

    @Test
    void sinCertificadoOConClaveIncorrectaNoFirma() {
        Map<String, Object> sin = factura(List.of(linea("P", "1", "10", "10")));
        sin.put("firma", Map.of("clave", "x"));
        assertThatThrownBy(() -> servicio.generar(sin)).isInstanceOf(FirmaDigitalException.class);

        Map<String, Object> mala = factura(List.of(linea("P", "1", "10", "10")));
        mala.put("firma", Map.of("pfx_base64", pfxBase64, "clave", "incorrecta"));
        assertThatThrownBy(() -> servicio.generar(mala)).isInstanceOf(FirmaDigitalException.class);

        Map<String, Object> roto = factura(List.of(linea("P", "1", "10", "10")));
        roto.put("firma", Map.of("pfx_base64", "%%%no-es-base64", "clave", "x"));
        assertThatThrownBy(() -> servicio.generar(roto)).isInstanceOf(FirmaDigitalException.class);
    }

    // ------------------------------------------------------------------ datos aleatorios
    @Test
    void elRucAleatorioCumpleElModulo11() {
        List<String> rucs = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            rucs.add((String) servicio.rucAleatorio().get("ruc"));
        }
        assertThat(rucs).allMatch(ValidadorRuc::esValido).allMatch(r -> r.startsWith("20")).doesNotHaveDuplicates();
    }

    @Test
    void laSerieAleatoriaEmpiezaConLaLetraDelTipo() {
        assertThat((String) servicio.serieAleatoria("01").get("serie")).matches("F[A-Z0-9]{3}");
        assertThat((String) servicio.serieAleatoria("03").get("serie")).matches("B[A-Z0-9]{3}");
        assertThat((String) servicio.serieAleatoria("").get("serie")).matches("F[A-Z0-9]{3}");
    }

    // ------------------------------------------------------------------ contador
    @Test
    void elContadorAvisaCuandoUnaSerieSeAgota() {
        ContadorCorrelativos casiLleno = new ContadorCorrelativos(() -> ContadorCorrelativos.MAXIMO);
        assertThat(casiLleno.siguiente("F001")).isEqualTo(ContadorCorrelativos.MAXIMO);
        assertThatThrownBy(() -> casiLleno.siguiente("F001")).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void elContadorPorDefectoDaNumerosValidosYCrecientes() {
        ContadorCorrelativos c = new ContadorCorrelativos();
        long primero = c.siguiente("F001");
        assertThat(primero).isBetween(1L, ContadorCorrelativos.MAXIMO);
        assertThat(c.siguiente("F001")).isEqualTo(primero + 1);
    }
}
