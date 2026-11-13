// Autor: Gilmer Ayala Falcón · UNAS-FIIS · Arquitectura de Software · Grupo A
package pe.abc.facturacion.infraestructura.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.URI;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/** El formulario por HTTP: se levanta el servidor en un puerto libre y se le habla como lo haria el navegador. */
class ServidorWebTest {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static ServidorWeb servidor;
    private static String base;

    private static final String VENTA = """
            {"tipo_comprobante":"01","serie":"F7Z1","moneda":"PEN",
             "emisor":{"ruc":"20526018151","razon_social":"EMISOR S.A.C.","direccion":"Av. Uno 1","ubigeo":"150101"},
             "receptor":{"tipo_documento":"6","numero_documento":"20555555556","razon_social":"CLIENTE S.A.C."},
             "items":[{"numero_linea":1,"codigo_producto":"P-01","descripcion":"Casco","unidad_medida":"NIU",
                       "cantidad":"2","valor_unitario":"50","tipo_afectacion_igv":"10"}]}""";

    @BeforeAll
    static void abrir() throws IOException {
        servidor = new ServidorWeb(0);
        servidor.iniciar();
        base = "http://127.0.0.1:" + servidor.puerto();
    }

    @AfterAll
    static void cerrar() {
        servidor.close();
    }

    /** Respuesta minima: estado, tipo de contenido y cuerpo. */
    private record Respuesta(int statusCode, String tipo, String body) {
    }

    private static Respuesta get(String ruta) throws Exception {
        return enviar(ruta, "GET", null);
    }

    private static Respuesta post(String ruta, String cuerpo) throws Exception {
        return enviar(ruta, "POST", cuerpo);
    }

    private static Respuesta enviar(String ruta, String metodo, String cuerpo) throws Exception {
        HttpURLConnection conexion = (HttpURLConnection) URI.create(base + ruta).toURL().openConnection();
        conexion.setRequestMethod(metodo);
        if (cuerpo != null) {
            conexion.setDoOutput(true);
            conexion.setFixedLengthStreamingMode(cuerpo.getBytes(StandardCharsets.UTF_8).length);
            try (OutputStream salida = conexion.getOutputStream()) {
                salida.write(cuerpo.getBytes(StandardCharsets.UTF_8));
            } catch (IOException e) {
                // el servidor puede cerrar antes de leer todo (solicitud enorme): se lee igual la respuesta
            }
        }
        int estado = conexion.getResponseCode();
        InputStream flujo = estado >= 400 ? conexion.getErrorStream() : conexion.getInputStream();
        String texto = flujo == null ? "" : new String(flujo.readAllBytes(), StandardCharsets.UTF_8);
        return new Respuesta(estado, conexion.getContentType(), texto);
    }

    @Test
    void elFormularioSeAbreVacio() throws Exception {
        Respuesta r = get("/");
        assertThat(r.statusCode()).isEqualTo(200);
        assertThat(r.tipo()).contains("text/html");
        String html = r.body();
        assertThat(html).contains("Registro de venta").contains("Generar RUC aleatorio válido")
                .contains("Incluir firma digital").contains("Gilmer Ayala Falcón");
        // nada prellenado: ningun campo de texto trae value=
        assertThat(html).doesNotContainPattern("<input[^>]*type=\"text\"[^>]*value=");
        assertThat(html).doesNotContainPattern("<input[^>]*id=\"firmar\"[^>]*checked");
        assertThat(get("/index.html").statusCode()).isEqualTo(200);
    }

    @Test
    void daUnRucYUnaSerieAleatorios() throws Exception {
        JsonNode ruc = JSON.readTree(get("/api/ruc-aleatorio").body());
        assertThat(ruc.get("ruc").asText()).matches("20[0-9]{9}");
        JsonNode serie = JSON.readTree(get("/api/serie-aleatoria?tipo=03").body());
        assertThat(serie.get("serie").asText()).matches("B[A-Z0-9]{3}");
    }

    @Test
    void calculaLosImportesDeLasLineas() throws Exception {
        Respuesta r = post("/api/calcular",
                "{\"moneda\":\"PEN\",\"items\":[{\"numero_linea\":1,\"codigo_producto\":\"P\",\"descripcion\":\"Casco\","
                        + "\"unidad_medida\":\"NIU\",\"cantidad\":\"2\",\"valor_unitario\":\"50\",\"tipo_afectacion_igv\":\"10\"}]}");
        assertThat(r.statusCode()).isEqualTo(200);
        JsonNode j = JSON.readTree(r.body());
        assertThat(j.get("valor_venta").asText()).isEqualTo("100.00");
        assertThat(j.get("igv").asText()).isEqualTo("18.00");
        assertThat(j.get("total").asText()).isEqualTo("118.00");
        assertThat(j.get("letras").asText()).isEqualTo("CIENTO DIECIOCHO CON 00/100 SOLES");
    }

    @Test
    void generaElXmlYLoDevuelveEnElJson() throws Exception {
        Respuesta r = post("/api/generar", VENTA);
        assertThat(r.statusCode()).isEqualTo(200);
        JsonNode j = JSON.readTree(r.body());
        assertThat(j.get("comprobante").asText()).startsWith("F7Z1-");
        assertThat(j.get("archivo").asText()).startsWith("20526018151-01-F7Z1-").endsWith(".xml");
        assertThat(j.get("firmado").asBoolean()).isFalse();
        assertThat(j.get("xml").asText()).startsWith("<?xml").contains("<cbc:PayableAmount currencyID=\"PEN\">118.00<");
    }

    @Test
    void losErroresDeDatosVuelvenComoListaConEstado422() throws Exception {
        Respuesta r = post("/api/generar", VENTA.replace("\"ruc\":\"20526018151\"", "\"ruc\":\"123\""));
        assertThat(r.statusCode()).isEqualTo(422);
        assertThat(JSON.readTree(r.body()).get("errores")).isNotEmpty();
    }

    @Test
    void unJsonRotoEsUnError422() throws Exception {
        Respuesta r = post("/api/calcular", "{no es json");
        assertThat(r.statusCode()).isEqualTo(422);
        assertThat(r.body()).contains("errores");
    }

    @Test
    void rechazaMetodosYRutasQueNoExisten() throws Exception {
        assertThat(get("/api/generar").statusCode()).isEqualTo(405);
        assertThat(get("/api/calcular").statusCode()).isEqualTo(405);
        assertThat(post("/", "{}").statusCode()).isEqualTo(405);
        assertThat(post("/api/ruc-aleatorio", "{}").statusCode()).isEqualTo(405);
        assertThat(post("/api/serie-aleatoria", "{}").statusCode()).isEqualTo(405);
        assertThat(get("/no-existe").statusCode()).isEqualTo(404);
    }

    @Test
    void rechazaSolicitudesDemasiadoGrandes() throws Exception {
        String enorme = "x".repeat(ServidorWeb.MAXIMO_BYTES + 10);
        assertThat(post("/api/generar", enorme).statusCode()).isEqualTo(413);
    }

    @Test
    void soloEscuchaEnLaMaquinaLocal() {
        assertThat(servidor.puerto()).isPositive();
        assertThat(base).startsWith("http://127.0.0.1:");
    }
}
