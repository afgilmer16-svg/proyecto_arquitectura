// Autor: Gilmer Ayala Falcón · UNAS-FIIS · Arquitectura de Software · Grupo A
package pe.abc.facturacion.infraestructura.web;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;

import com.fasterxml.jackson.databind.ObjectMapper;

import pe.abc.facturacion.aplicacion.GenerarFacturaXmlService;
import pe.abc.facturacion.dominio.error.DatosFacturaInvalidosException;
import pe.abc.facturacion.dominio.error.ErrorFacturacionXml;
import pe.abc.facturacion.infraestructura.entrada.LectorJson;
import pe.abc.facturacion.infraestructura.ubl.GeneradorUblJaxbAdapter;

/**
 * Servidor HTTP del formulario web, solo para esta maquina (escucha en 127.0.0.1).
 *
 * <p>Usa {@link ServerSocket} con un hilo virtual por conexion: sin librerias adicionales y sin los
 * selectores NIO que algunos Windows con software de seguridad no dejan abrir (error "Unable to establish
 * loopback connection"). Entiende lo que el formulario necesita de HTTP/1.1: una solicitud por conexion, con
 * {@code Content-Length}.
 *
 * <pre>
 * GET  /                            el formulario (vacio)
 * GET  /api/ruc-aleatorio           RUC nuevo con digito verificador valido
 * GET  /api/serie-aleatoria?tipo=01 serie Fxxx (01) o Bxxx (03)
 * POST /api/calcular                importes de las lineas
 * POST /api/generar                 valida, genera y devuelve el XML
 * </pre>
 *
 * Los errores de datos responden 422 con {@code {"errores": [...]}}.
 */
public final class ServidorWeb implements AutoCloseable {

    /** Limite de la solicitud: alcanza para un certificado .pfx en base64. */
    static final int MAXIMO_BYTES = 2 * 1024 * 1024;
    private static final int MAXIMO_ENCABEZADOS = 16 * 1024;
    private static final int ESPERA_MS = 15_000;
    private static final ObjectMapper JSON = new ObjectMapper();

    private final ServerSocket socket;
    private final ServicioFormulario servicio;
    private final ExecutorService conexiones = Executors.newVirtualThreadPerTaskExecutor();
    private Thread aceptador;

    public ServidorWeb(int puerto) throws IOException {
        this(puerto, new ServicioFormulario(new GenerarFacturaXmlService(new GeneradorUblJaxbAdapter()),
                new ContadorCorrelativos(), new Random()));
    }

    public ServidorWeb(int puerto, ServicioFormulario servicio) throws IOException {
        this.servicio = servicio;
        this.socket = new ServerSocket(puerto, 50, InetAddress.getLoopbackAddress());
    }

    public void iniciar() {
        aceptador = new Thread(this::aceptar, "servidor-web");
        aceptador.start();
    }

    /** Puerto real (util cuando se pidio el 0 para que el sistema elija uno libre). */
    public int puerto() {
        return socket.getLocalPort();
    }

    @Override
    public void close() {
        try {
            socket.close();
        } catch (IOException e) {
            // ya estaba cerrado
        }
        conexiones.shutdownNow();
    }

    private void aceptar() {
        while (!socket.isClosed()) {
            try {
                Socket cliente = socket.accept();
                conexiones.execute(() -> atender(cliente));
            } catch (SocketException e) {
                return; // close()
            } catch (IOException e) {
                // una conexion fallida no tumba el servidor
            }
        }
    }

    // ------------------------------------------------------------------ una solicitud
    private record Solicitud(String metodo, String ruta, String consulta, byte[] cuerpo) {
    }

    private record Respuesta(int estado, String tipo, byte[] cuerpo) {
    }

    private void atender(Socket cliente) {
        try (cliente) {
            cliente.setSoTimeout(ESPERA_MS);
            InputStream entrada = new BufferedInputStream(cliente.getInputStream());
            Respuesta respuesta;
            try {
                respuesta = enrutar(leer(entrada));
            } catch (RuntimeException e) {
                respuesta = errores(500, "Error interno: " + e.getMessage());
            }
            escribir(cliente.getOutputStream(), respuesta);
        } catch (IOException e) {
            // el navegador cerro la conexion o se agoto la espera: no hay a quien responder
        }
    }

    /** Lee la linea de solicitud, los encabezados y el cuerpo. */
    private static Solicitud leer(InputStream entrada) throws IOException {
        String linea = linea(entrada, MAXIMO_ENCABEZADOS);
        String[] partes = linea == null ? new String[0] : linea.split(" ");
        if (partes.length < 2) {
            throw new IOException("solicitud vacia");
        }
        long largo = 0;
        boolean fragmentado = false;
        int leidos = linea.length();
        for (String encabezado = linea(entrada, MAXIMO_ENCABEZADOS); encabezado != null && !encabezado.isEmpty();
                encabezado = linea(entrada, MAXIMO_ENCABEZADOS)) {
            leidos += encabezado.length();
            if (leidos > MAXIMO_ENCABEZADOS) {
                throw new IOException("encabezados demasiado largos");
            }
            int dos = encabezado.indexOf(':');
            if (dos > 0) {
                String nombre = encabezado.substring(0, dos).trim().toLowerCase(Locale.ROOT);
                String valor = encabezado.substring(dos + 1).trim();
                if (nombre.equals("content-length")) {
                    try {
                        largo = Long.parseLong(valor);
                    } catch (NumberFormatException e) {
                        largo = -1;
                    }
                } else if (nombre.equals("transfer-encoding")) {
                    fragmentado = true;
                }
            }
        }
        String destino = partes[1];
        int interrogante = destino.indexOf('?');
        String ruta = interrogante < 0 ? destino : destino.substring(0, interrogante);
        String consulta = interrogante < 0 ? "" : destino.substring(interrogante + 1);
        byte[] cuerpo = new byte[0];
        if (fragmentado || largo < 0) {
            return new Solicitud(partes[0], ruta, consulta, null); // 411
        }
        if (largo > MAXIMO_BYTES) {
            // 413: se descarta el cuerpo (hasta 32 MB) para que el cliente alcance a leer la respuesta
            byte[] descarte = new byte[8192];
            for (long falta = Math.min(largo, 32L * 1024 * 1024); falta > 0;) {
                int n = entrada.read(descarte, 0, (int) Math.min(descarte.length, falta));
                if (n < 0) {
                    break;
                }
                falta -= n;
            }
            return new Solicitud(partes[0], ruta, consulta, new byte[MAXIMO_BYTES + 1]);
        }
        if (largo > 0) {
            cuerpo = entrada.readNBytes((int) largo);
        }
        return new Solicitud(partes[0], ruta, consulta, cuerpo);
    }

    /** Una linea terminada en CRLF (o LF), sin el terminador; {@code null} si el flujo termino. */
    private static String linea(InputStream entrada, int maximo) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        int b;
        while ((b = entrada.read()) != -1) {
            if (b == '\n') {
                break;
            }
            if (b != '\r') {
                bytes.write(b);
            }
            if (bytes.size() > maximo) {
                throw new IOException("linea demasiado larga");
            }
        }
        return b == -1 && bytes.size() == 0 ? null : bytes.toString(StandardCharsets.ISO_8859_1);
    }

    private Respuesta enrutar(Solicitud s) throws IOException {
        if (s.cuerpo() == null) {
            return errores(411, "Se necesita Content-Length.");
        }
        return switch (s.ruta()) {
            case "/", "/index.html" -> soloGet(s, () -> new Respuesta(200, "text/html; charset=utf-8", pagina()));
            case "/api/ruc-aleatorio" -> soloGet(s, () -> json(200, servicio.rucAleatorio()));
            case "/api/serie-aleatoria" -> soloGet(s, () -> json(200, servicio.serieAleatoria(parametro(s.consulta(), "tipo"))));
            case "/api/calcular" -> soloPost(s, servicio::calcular);
            case "/api/generar" -> soloPost(s, servicio::generar);
            default -> errores(404, "No existe " + s.ruta());
        };
    }

    private interface Accion {
        Respuesta ejecutar() throws IOException;
    }

    private static Respuesta soloGet(Solicitud s, Accion accion) throws IOException {
        return "GET".equals(s.metodo()) ? accion.ejecutar() : errores(405, "Método no permitido");
    }

    private static Respuesta soloPost(Solicitud s, Function<Map<String, Object>, Map<String, Object>> operacion) {
        if (!"POST".equals(s.metodo())) {
            return errores(405, "Método no permitido");
        }
        if (s.cuerpo().length > MAXIMO_BYTES) {
            return errores(413, "La solicitud es demasiado grande.");
        }
        try {
            return json(200, operacion.apply(LectorJson.leer(s.cuerpo(), "la solicitud")));
        } catch (DatosFacturaInvalidosException e) {
            return json(422, Map.of("errores", e.errores()));
        } catch (ErrorFacturacionXml e) {
            return errores(422, e.getMessage());
        }
    }

    // ------------------------------------------------------------------ utilidades
    private static String parametro(String consulta, String nombre) {
        for (String par : consulta.split("&")) {
            String[] clave = par.split("=", 2);
            if (clave.length == 2 && clave[0].equals(nombre)) {
                return URLDecoder.decode(clave[1], StandardCharsets.UTF_8);
            }
        }
        return "";
    }

    private static byte[] pagina() {
        try (InputStream recurso = ServidorWeb.class.getResourceAsStream("/web/index.html")) {
            if (recurso == null) {
                throw new IllegalStateException("Falta el recurso /web/index.html");
            }
            return recurso.readAllBytes();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static Respuesta errores(int estado, String mensaje) {
        return json(estado, Map.of("errores", List.of(mensaje)));
    }

    private static Respuesta json(int estado, Map<String, ?> cuerpo) {
        try {
            return new Respuesta(estado, "application/json; charset=utf-8",
                    JSON.writeValueAsBytes(new LinkedHashMap<>(cuerpo)));
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String motivo(int estado) {
        return switch (estado) {
            case 200 -> "OK";
            case 404 -> "Not Found";
            case 405 -> "Method Not Allowed";
            case 411 -> "Length Required";
            case 413 -> "Content Too Large";
            case 422 -> "Unprocessable Content";
            default -> "Internal Server Error";
        };
    }

    private static void escribir(OutputStream salida, Respuesta r) throws IOException {
        String cabecera = "HTTP/1.1 " + r.estado() + " " + motivo(r.estado()) + "\r\n"
                + "Content-Type: " + r.tipo() + "\r\n"
                + "Content-Length: " + r.cuerpo().length + "\r\n"
                + "Cache-Control: no-store\r\n"
                + "Connection: close\r\n\r\n";
        salida.write(cabecera.getBytes(StandardCharsets.ISO_8859_1));
        salida.write(r.cuerpo());
        salida.flush();
    }
}
