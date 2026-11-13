// Autor: Gilmer Ayala Falcón · UNAS-FIIS · Arquitectura de Software · Grupo A
package pe.abc.facturacion.infraestructura.web;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import pe.abc.facturacion.aplicacion.GenerarFacturaXmlService;
import pe.abc.facturacion.aplicacion.puerto.FirmaDigitalPort;
import pe.abc.facturacion.dominio.NumeroLetras;
import pe.abc.facturacion.dominio.calculo.CalculadoraTributaria;
import pe.abc.facturacion.dominio.calculo.Totales;
import pe.abc.facturacion.dominio.catalogo.Catalogos;
import pe.abc.facturacion.dominio.catalogo.RegimenIgv;
import pe.abc.facturacion.dominio.error.DatosFacturaInvalidosException;
import pe.abc.facturacion.dominio.error.FirmaDigitalException;
import pe.abc.facturacion.dominio.modelo.Modelo.Factura;
import pe.abc.facturacion.dominio.validacion.GeneradorDatosDePrueba;
import pe.abc.facturacion.dominio.validacion.LectorFactura;
import pe.abc.facturacion.infraestructura.entrada.EsquemaEntrada;
import pe.abc.facturacion.infraestructura.ubl.ValidadorXsd;

/**
 * Logica del formulario web, sin HTTP: cada operacion recibe y devuelve mapas (JSON) para poder probarla
 * directamente.
 *
 * <ul>
 *   <li>{@link #calcular}: vista previa de importes mientras se escribe; solo mira las lineas.
 *   <li>{@link #generar}: valida, asigna el correlativo, genera el XML, lo valida contra el XSD y, si se pide,
 *       lo firma con el certificado enviado.
 * </ul>
 *
 * <p>No guarda ningun dato de la venta: todo viene del formulario y se descarta al responder.
 */
public final class ServicioFormulario {

    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final ZoneOffset ZONA_PERU = ZoneOffset.ofHours(-5);
    /** Datos de relleno para la vista previa: los importes no dependen de quien emite ni de quien compra. */
    private static final String RUC_RELLENO = GeneradorDatosDePrueba.rucAleatorio(new Random(7));

    private final GenerarFacturaXmlService generador;
    private final ContadorCorrelativos correlativos;
    private final EsquemaEntrada esquema = new EsquemaEntrada();
    private final Random azar;

    public ServicioFormulario(GenerarFacturaXmlService generador, ContadorCorrelativos correlativos, Random azar) {
        this.generador = generador;
        this.correlativos = correlativos;
        this.azar = azar;
    }

    // ------------------------------------------------------------------ datos de prueba
    public Map<String, Object> rucAleatorio() {
        return Map.of("ruc", GeneradorDatosDePrueba.rucAleatorio(azar));
    }

    public Map<String, Object> serieAleatoria(String tipoComprobante) {
        char prefijo = "03".equals(tipoComprobante) ? 'B' : 'F';
        return Map.of("serie", GeneradorDatosDePrueba.serieAleatoria(prefijo, azar));
    }

    // ------------------------------------------------------------------ vista previa
    /** @param solicitud {@code moneda} (opcional) e {@code items} */
    public Map<String, Object> calcular(Map<String, Object> solicitud) {
        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("tipo_comprobante", "01");
        datos.put("serie", "F001");
        datos.put("fecha_emision", hoy().toString());
        datos.put("moneda", solicitud.getOrDefault("moneda", "PEN"));
        datos.put("emisor", Map.of("ruc", RUC_RELLENO, "razon_social", "VISTA PREVIA",
                "direccion", "VISTA PREVIA", "ubigeo", "150101"));
        datos.put("receptor", Map.of("tipo_documento", "6", "numero_documento", RUC_RELLENO,
                "razon_social", "VISTA PREVIA"));
        datos.put("items", solicitud.get("items"));
        Factura factura = leer(datos, 1);
        Totales t = CalculadoraTributaria.calcularTotales(factura);

        Map<String, Object> respuesta = new LinkedHashMap<>();
        respuesta.put("moneda", factura.moneda());
        respuesta.put("valor_venta", t.totalValorVenta().toPlainString());
        respuesta.put("gravadas", t.totalGravadas().toPlainString());
        respuesta.put("exoneradas", t.totalExoneradas().toPlainString());
        respuesta.put("inafectas", t.totalInafectas().toPlainString());
        respuesta.put("igv", t.totalIgv().toPlainString());
        respuesta.put("total", t.importeTotal().toPlainString());
        respuesta.put("letras", NumeroLetras.importeEnLetras(t.importeTotal(), Catalogos.MONEDAS.get(factura.moneda())));
        return respuesta;
    }

    // ------------------------------------------------------------------ generacion
    /**
     * @param solicitud los datos del comprobante (sin correlativo) y, opcionalmente, {@code firma} con
     *                  {@code pfx_base64} y {@code clave}
     */
    public Map<String, Object> generar(Map<String, Object> solicitud) {
        Map<String, Object> datos = new LinkedHashMap<>(solicitud);
        Object firma = datos.remove("firma");
        datos.putIfAbsent("tipo_operacion", "0101");
        datos.putIfAbsent("fecha_emision", hoy().toString());
        datos.putIfAbsent("hora_emision", LocalTime.now(ZONA_PERU).format(HORA));
        datos.putIfAbsent("moneda", "PEN");
        datos.putIfAbsent("forma_pago", Map.of("tipo", "Contado"));

        leer(datos, 1); // todos los errores juntos antes de gastar un correlativo
        String serie = (String) datos.get("serie");
        long correlativo = correlativos.siguiente(serie);
        byte[] xml = generador.generar(datos, correlativo, RegimenIgv.GENERAL);

        boolean firmado = false;
        if (firma != null) {
            xml = firmador(firma).firmar(xml);
            new ValidadorXsd().validar(xml); // el XML firmado tambien debe seguir cumpliendo el XSD
            firmado = true;
        }

        String ruc = (String) ((Map<?, ?>) datos.get("emisor")).get("ruc");
        String comprobante = "%s-%08d".formatted(serie, correlativo);
        Map<String, Object> respuesta = new LinkedHashMap<>();
        respuesta.put("comprobante", comprobante);
        respuesta.put("archivo", "%s-%s-%s.xml".formatted(ruc, datos.get("tipo_comprobante"), comprobante));
        respuesta.put("firmado", firmado);
        respuesta.put("xml", new String(xml, StandardCharsets.UTF_8));
        return respuesta;
    }

    // ------------------------------------------------------------------ utilidades
    private Factura leer(Map<String, Object> datos, long correlativo) {
        List<String> errores = esquema.validar(datos);
        if (!errores.isEmpty()) {
            throw new DatosFacturaInvalidosException(errores);
        }
        return LectorFactura.desdeMapa(datos, correlativo, RegimenIgv.GENERAL);
    }

    private static LocalDate hoy() {
        return LectorFactura.hoyEnLima();
    }

    private static FirmaDigitalPort firmador(Object firma) {
        if (!(firma instanceof Map<?, ?> mapa) || !(mapa.get("pfx_base64") instanceof String pfx) || pfx.isBlank()) {
            throw new FirmaDigitalException("Para firmar hay que elegir el certificado .pfx/.p12.");
        }
        byte[] contenido;
        try {
            contenido = Base64.getDecoder().decode(pfx);
        } catch (IllegalArgumentException e) {
            throw new FirmaDigitalException("El certificado no llegó completo; vuelve a elegirlo.", e);
        }
        String clave = mapa.get("clave") instanceof String c ? c : "";
        return new pe.abc.facturacion.infraestructura.firma.FirmadorXmlDsigAdapter(contenido, clave.toCharArray());
    }
}
