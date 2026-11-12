package pe.abc.facturacion.dominio.validacion;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.regex.Pattern;

import pe.abc.facturacion.dominio.calculo.CalculadoraTributaria;
import pe.abc.facturacion.dominio.calculo.Totales;
import pe.abc.facturacion.dominio.catalogo.Catalogos;
import pe.abc.facturacion.dominio.catalogo.RegimenIgv;
import pe.abc.facturacion.dominio.catalogo.SistemaCalculoIsc;
import pe.abc.facturacion.dominio.catalogo.ClaseComprobante;
import pe.abc.facturacion.dominio.catalogo.TipoDocumentoIdentidad;
import pe.abc.facturacion.dominio.error.DatosFacturaInvalidosException;
import pe.abc.facturacion.dominio.modelo.Modelo.Cuota;
import pe.abc.facturacion.dominio.modelo.Modelo.Emisor;
import pe.abc.facturacion.dominio.modelo.Modelo.Factura;
import pe.abc.facturacion.dominio.modelo.Modelo.FormaPago;
import pe.abc.facturacion.dominio.modelo.Modelo.LineaFactura;
import pe.abc.facturacion.dominio.modelo.Modelo.Receptor;

/**
 * Convierte el DTO de entrada ({@code EmitirComprobanteRequest}, como mapa) en una {@link Factura} validada.
 *
 * <p>La entrada tiene nombres en snake_case e importes como texto decimal. El correlativo <b>no</b> viene del
 * cliente: lo asigna el servidor (la serie) y se pasa aparte. Todos los problemas se informan juntos, cada
 * uno con la ruta del campo.
 */
public final class LectorFactura {

    private static final Pattern PATRON_SERIE = Pattern.compile("^[FB][A-Z0-9]{3}$");
    private static final Pattern PATRON_DECIMAL = Pattern.compile("^[0-9]+(\\.[0-9]{1,10})?$");
    private static final Pattern PATRON_TASA = Pattern.compile("^[0-9]{1,3}\\.[0-9]{2}$");
    private static final Pattern PATRON_HORA =
            Pattern.compile("^(0[0-9]|1[0-9]|2[0-3]):[0-5][0-9]:[0-5][0-9]$");

    public static final long CORRELATIVO_MAXIMO = 99_999_999L;
    /** Umbral para identificar al comprador en boletas; el monto vigente esta "Por verificar". */
    public static final BigDecimal UMBRAL_IDENTIFICACION_BOLETA = new BigDecimal("700.00");

    private LectorFactura() {
    }

    // ---------------------------------------------------------------- utilidades
    private static final class Lector {
        final List<String> errores = new ArrayList<>();

        void error(String ruta, String mensaje) {
            errores.add(ruta + ": " + mensaje);
        }

        String texto(Map<?, ?> datos, String clave, String ruta, boolean requerido, Integer maximo) {
            Object valor = datos.get(clave);
            if (valor == null || (valor instanceof String s && s.isBlank())) {
                if (requerido) {
                    error(ruta + clave, "es obligatorio");
                }
                return null;
            }
            if (!(valor instanceof String s)) {
                error(ruta + clave, "debe ser texto");
                return null;
            }
            String limpio = s.strip();
            if (maximo != null && limpio.length() > maximo) {
                error(ruta + clave, "supera " + maximo + " caracteres");
            }
            return limpio;
        }

        BigDecimal decimal(Map<?, ?> datos, String clave, String ruta, boolean requerido, boolean positivo,
                           Pattern patron) {
            Object valor = datos.get(clave);
            if (valor == null) {
                if (requerido) {
                    error(ruta + clave, "es obligatorio");
                }
                return null;
            }
            if (!(valor instanceof String s) || !patron.matcher(s).matches()) {
                error(ruta + clave, "debe ser un decimal exacto en texto (ej. '140.00'); se recibió " + repr(valor));
                return null;
            }
            BigDecimal numero = new BigDecimal(s);
            if (positivo && numero.signum() <= 0) {
                error(ruta + clave, "debe ser mayor que cero");
                return null;
            }
            return numero;
        }

        LocalDate fecha(Map<?, ?> datos, String clave, String ruta, boolean requerido) {
            Object valor = datos.get(clave);
            if (valor == null) {
                if (requerido) {
                    error(ruta + clave, "es obligatorio");
                }
                return null;
            }
            try {
                if (valor instanceof String s) {
                    return LocalDate.parse(s);
                }
            } catch (DateTimeParseException e) {
                // se informa abajo
            }
            error(ruta + clave, "debe tener formato AAAA-MM-DD; se recibió " + repr(valor));
            return null;
        }
    }

    /** Representacion al estilo de la de Python, para mensajes: 'texto', None, 12. */
    private static String repr(Object valor) {
        if (valor == null) {
            return "None";
        }
        return valor instanceof String s ? "'" + s + "'" : String.valueOf(valor);
    }

    private static boolean esEnteroPositivo(Object valor) {
        return (valor instanceof Integer || valor instanceof Long || valor instanceof BigInteger)
                && ((Number) valor).longValue() >= 1;
    }

    // ----------------------------------------------------------------------- API
    /** Lee con el regimen general de IGV (18 %). */
    public static Factura desdeMapa(Map<String, Object> datos, long correlativo) {
        return desdeMapa(datos, correlativo, RegimenIgv.GENERAL);
    }

    /**
     * @param datos        mapa con la forma de {@code EmitirComprobanteRequest}
     * @param correlativo  numero asignado por la serie (1 a 99 999 999)
     * @param regimenIgv   tasa de IGV aplicable
     * @throws DatosFacturaInvalidosException con todos los problemas encontrados
     */
    public static Factura desdeMapa(Map<String, Object> datos, long correlativo, RegimenIgv regimenIgv) {
        if (datos == null) {
            throw new DatosFacturaInvalidosException(List.of("la entrada debe ser un objeto JSON (diccionario)"));
        }
        Lector lec = new Lector();

        // Tipo de comprobante y serie
        ClaseComprobante tipo = null;
        Object tipoTxt = datos.get("tipo_comprobante");
        if (tipoTxt instanceof String t && Catalogos.TIPOS_COMPROBANTE_OTRO_XSD.containsKey(t)) {
            lec.error("tipo_comprobante", "'" + t + "' se genera con " + Catalogos.TIPOS_COMPROBANTE_OTRO_XSD.get(t)
                    + ", fuera del alcance de este generador (UBL-Invoice-2.1.xsd)");
        } else {
            tipo = ClaseComprobante.desde(tipoTxt).orElse(null);
            if (tipo == null) {
                lec.error("tipo_comprobante", "debe ser '01' (factura) o '03' (boleta); se recibió " + repr(tipoTxt));
            }
        }

        String serie = lec.texto(datos, "serie", "", true, null);
        if (serie != null && !PATRON_SERIE.matcher(serie).matches()) {
            lec.error("serie", "debe cumplir ^[FB][A-Z0-9]{3}$; se recibió " + repr(serie));
        } else if (serie != null && tipo != null && serie.charAt(0) != tipo.prefijoSerie()) {
            lec.error("serie", "un comprobante '" + tipo.codigo() + "' debe usar una serie que empiece con '"
                    + tipo.prefijoSerie() + "'");
        }

        if (correlativo < 1 || correlativo > CORRELATIVO_MAXIMO) {
            lec.error("correlativo", "lo asigna el servidor y debe ser un entero de 1 a " + CORRELATIVO_MAXIMO);
        }
        if (datos.containsKey("correlativo")) {
            lec.error("correlativo", "el cliente no envía el correlativo; lo asigna el servidor");
        }

        // Fechas, moneda y tipo de operacion
        LocalDate fechaEmision = lec.fecha(datos, "fecha_emision", "", true);
        LocalTime hora = null;
        Object horaTxt = datos.get("hora_emision");
        if (horaTxt != null) {
            if (horaTxt instanceof String h && PATRON_HORA.matcher(h).matches()) {
                hora = LocalTime.parse(h);
            } else {
                lec.error("hora_emision", "debe tener formato HH:MM:SS; se recibió " + repr(horaTxt));
            }
        }

        Object moneda = datos.containsKey("moneda") ? datos.get("moneda") : "PEN";
        if (!(moneda instanceof String m && Catalogos.MONEDAS.containsKey(m))) {
            lec.error("moneda", "debe ser una de " + new TreeSet<>(Catalogos.MONEDAS.keySet())
                    + " (Catálogo 02); se recibió " + repr(moneda));
        }

        Object tipoOperacion = datos.containsKey("tipo_operacion")
                ? datos.get("tipo_operacion") : Catalogos.TIPO_OPERACION_VENTA_INTERNA;
        if (!Catalogos.TIPO_OPERACION_VENTA_INTERNA.equals(tipoOperacion)) {
            lec.error("tipo_operacion", "este generador emite venta interna ('0101', Catálogo 51)");
        }

        // Emisor
        Emisor emisor = null;
        if (!(datos.get("emisor") instanceof Map<?, ?> dEmisor)) {
            lec.error("emisor", "es obligatorio");
        } else {
            String ruc = lec.texto(dEmisor, "ruc", "emisor.", true, null);
            if (ruc != null && !ValidadorRuc.esValido(ruc)) {
                lec.error("emisor.ruc", "RUC inválido " + repr(ruc)
                        + ": 11 dígitos, prefijo 10/15/16/17/20 y dígito verificador módulo 11");
            }
            String razon = lec.texto(dEmisor, "razon_social", "emisor.", true, 100);
            String direccion = lec.texto(dEmisor, "direccion", "emisor.", true, 200);
            String ubigeo = lec.texto(dEmisor, "ubigeo", "emisor.", true, null);
            if (ubigeo != null && !ubigeo.matches("[0-9]{6}")) {
                lec.error("emisor.ubigeo", "debe tener 6 dígitos (Catálogo 13)");
            }
            String nombreComercial = lec.texto(dEmisor, "nombre_comercial", "emisor.", false, 100);
            if (ruc != null && razon != null && direccion != null && ubigeo != null) {
                emisor = new Emisor(ruc, razon, direccion, ubigeo, nombreComercial);
            }
        }

        // Receptor
        Receptor receptor = null;
        if (!(datos.get("receptor") instanceof Map<?, ?> dReceptor)) {
            lec.error("receptor", "es obligatorio");
        } else {
            TipoDocumentoIdentidad tipoDoc = TipoDocumentoIdentidad.desde(dReceptor.get("tipo_documento")).orElse(null);
            if (tipoDoc == null) {
                lec.error("receptor.tipo_documento", "debe ser un código del Catálogo 06: '0', '1', '4', '6' o '7'");
            }
            String numero = lec.texto(dReceptor, "numero_documento", "receptor.", true, null);
            String razonR = lec.texto(dReceptor, "razon_social", "receptor.", true, 100);
            String direccionR = lec.texto(dReceptor, "direccion", "receptor.", false, 200);
            if (tipo == ClaseComprobante.FACTURA && tipoDoc != null && tipoDoc != TipoDocumentoIdentidad.RUC) {
                lec.error("receptor.tipo_documento", "la factura exige un adquirente con RUC (tipo '6')");
            }
            if (numero != null && tipoDoc == TipoDocumentoIdentidad.RUC && !ValidadorRuc.esValido(numero)) {
                lec.error("receptor.numero_documento", "RUC inválido " + repr(numero) + " (módulo 11)");
            }
            if (numero != null && tipoDoc == TipoDocumentoIdentidad.DNI && !numero.matches("[0-9]{8}")) {
                lec.error("receptor.numero_documento", "el DNI debe tener 8 dígitos");
            }
            if (tipoDoc != null && numero != null && razonR != null) {
                receptor = new Receptor(tipoDoc, numero, razonR, direccionR);
            }
        }

        // Lineas
        List<LineaFactura> lineas = new ArrayList<>();
        List<?> dItems = datos.get("items") instanceof List<?> l && !l.isEmpty() ? l : null;
        if (dItems == null) {
            lec.error("items", "debe contener al menos una línea");
            dItems = List.of();
        }
        for (int i = 0; i < dItems.size(); i++) {
            String ruta = "items[" + i + "].";
            if (!(dItems.get(i) instanceof Map<?, ?> item)) {
                lec.error("items[" + i + "]", "debe ser un objeto");
                continue;
            }
            LineaFactura linea = leerLinea(lec, item, ruta, i + 1);
            if (linea != null) {
                lineas.add(linea);
            }
        }

        // Forma de pago
        Object dPago = datos.get("forma_pago");
        if (dPago == null || (dPago instanceof Map<?, ?> m && m.isEmpty())) {
            dPago = Map.of("tipo", "Contado");
        }
        FormaPago formaPago = leerFormaPago(lec, dPago, fechaEmision);

        if (!lec.errores.isEmpty()) {
            throw new DatosFacturaInvalidosException(lec.errores);
        }

        Factura factura = new Factura(tipo, serie, correlativo, fechaEmision, (String) moneda, emisor, receptor,
                List.copyOf(lineas), hora, (String) tipoOperacion, formaPago, regimenIgv);
        validarReglasDeConjunto(factura);
        return factura;
    }

    private static LineaFactura leerLinea(Lector lec, Map<?, ?> item, String ruta, int esperado) {
        int erroresPrevios = lec.errores.size();
        Object numero = item.get("numero_linea");
        if (!(numero instanceof Integer || numero instanceof Long) || ((Number) numero).longValue() != esperado) {
            lec.error(ruta + "numero_linea", "debe ser " + esperado + " (numeración única y secuencial)");
        }
        String codigo = lec.texto(item, "codigo_producto", ruta, true, 30);
        String descripcion = lec.texto(item, "descripcion", ruta, true, 250);
        Object unidad = item.get("unidad_medida");
        if (!(unidad instanceof String u && Catalogos.UNIDADES_MEDIDA.contains(u))) {
            lec.error(ruta + "unidad_medida", repr(unidad) + " no es un código del Catálogo 03 admitido "
                    + new TreeSet<>(Catalogos.UNIDADES_MEDIDA));
        }
        BigDecimal cantidad = lec.decimal(item, "cantidad", ruta, true, true, PATRON_DECIMAL);
        BigDecimal valorUnitario = lec.decimal(item, "valor_unitario", ruta, true, false, PATRON_DECIMAL);

        Object afectacion = item.get("tipo_afectacion_igv");
        if (afectacion instanceof String a && Catalogos.AFECTACIONES_NO_SOPORTADAS.containsKey(a)) {
            lec.error(ruta + "tipo_afectacion_igv", "'" + a + "' (" + Catalogos.AFECTACIONES_NO_SOPORTADAS.get(a)
                    + ") no se genera en este parcial");
        } else if (!(afectacion instanceof String a && Catalogos.CATEGORIA_POR_AFECTACION.containsKey(a))) {
            lec.error(ruta + "tipo_afectacion_igv", repr(afectacion) + " no es un código del Catálogo 07");
        }

        // ISC (Catalogo 08) con validacion condicional por sistema
        SistemaCalculoIsc sistema = null;
        BigDecimal tasa = null;
        BigDecimal montoFijo = null;
        String unidadBase = null;
        BigDecimal pvp = null;
        if (item.get("sistema_isc") != null) {
            sistema = SistemaCalculoIsc.desde(item.get("sistema_isc")).orElse(null);
            if (sistema == null) {
                lec.error(ruta + "sistema_isc", "debe ser '01', '02' o '03' (Catálogo 08)");
            }
            if (!"10".equals(afectacion)) {
                lec.error(ruta + "sistema_isc", "el ISC solo se admite en líneas gravadas (afectación '10')");
            }
            tasa = lec.decimal(item, "tasa_isc", ruta,
                    sistema == SistemaCalculoIsc.AL_VALOR || sistema == SistemaCalculoIsc.PRECIO_VENTA_PUBLICO, false, PATRON_TASA);
            montoFijo = lec.decimal(item, "monto_fijo_isc", ruta, sistema == SistemaCalculoIsc.ESPECIFICO, false,
                    PATRON_DECIMAL);
            Object base = item.get("unidad_base_isc");
            unidadBase = base instanceof String b ? b : null;
            if (sistema == SistemaCalculoIsc.ESPECIFICO && !(base instanceof String b && Catalogos.UNIDADES_MEDIDA.contains(b))) {
                lec.error(ruta + "unidad_base_isc", "es obligatorio en el sistema '02' y debe ser del Catálogo 03");
            }
            pvp = lec.decimal(item, "precio_venta_publico_sugerido", ruta,
                    sistema == SistemaCalculoIsc.PRECIO_VENTA_PUBLICO, true, PATRON_DECIMAL);
        } else {
            for (String clave : List.of("tasa_isc", "monto_fijo_isc", "unidad_base_isc", "precio_venta_publico_sugerido")) {
                if (item.get(clave) != null) {
                    lec.error(ruta + clave, "requiere 'sistema_isc'");
                }
            }
        }

        Object bolsas = item.get("cantidad_bolsas");
        if (bolsas != null && !esEnteroPositivo(bolsas)) {
            lec.error(ruta + "cantidad_bolsas", "debe ser un entero mayor o igual a 1");
        }

        if (lec.errores.size() > erroresPrevios) {
            return null;
        }
        return new LineaFactura(esperado, codigo, descripcion, (String) unidad, cantidad, valorUnitario,
                (String) afectacion, sistema, tasa, montoFijo, unidadBase, pvp,
                bolsas == null ? null : ((Number) bolsas).intValue());
    }

    private static FormaPago leerFormaPago(Lector lec, Object d, LocalDate fechaEmision) {
        if (!(d instanceof Map<?, ?> mapa)) {
            lec.error("forma_pago", "debe ser un objeto");
            return FormaPago.contado();
        }
        Object tipo = mapa.get("tipo");
        if ("Contado".equals(tipo)) {
            return FormaPago.contado();
        }
        if (!"Credito".equals(tipo)) {
            lec.error("forma_pago.tipo", "debe ser 'Contado' o 'Credito'");
            return FormaPago.contado();
        }
        BigDecimal pendiente = lec.decimal(mapa, "monto_neto_pendiente", "forma_pago.", true, true, PATRON_DECIMAL);
        List<Cuota> cuotas = new ArrayList<>();
        List<?> dCuotas = mapa.get("cuotas") instanceof List<?> l && !l.isEmpty() ? l : null;
        if (dCuotas == null) {
            lec.error("forma_pago.cuotas", "el pago al crédito exige al menos una cuota");
            dCuotas = List.of();
        }
        for (int i = 0; i < dCuotas.size(); i++) {
            String ruta = "forma_pago.cuotas[" + i + "].";
            if (!(dCuotas.get(i) instanceof Map<?, ?> c)) {
                lec.error("forma_pago.cuotas[" + i + "]", "debe ser un objeto");
                continue;
            }
            BigDecimal monto = lec.decimal(c, "monto", ruta, true, true, PATRON_DECIMAL);
            LocalDate venc = lec.fecha(c, "fecha_vencimiento", ruta, true);
            if (venc != null && fechaEmision != null && venc.isBefore(fechaEmision)) {
                lec.error(ruta + "fecha_vencimiento", "no puede ser anterior a la fecha de emisión");
            }
            if (monto != null && venc != null) {
                cuotas.add(new Cuota(monto, venc));
            }
        }
        if (pendiente != null && !cuotas.isEmpty()
                && cuotas.stream().map(Cuota::monto).reduce(BigDecimal.ZERO, BigDecimal::add).compareTo(pendiente) != 0) {
            lec.error("forma_pago.cuotas", "la suma de las cuotas debe ser igual al monto neto pendiente");
        }
        return new FormaPago("Credito", pendiente, List.copyOf(cuotas));
    }

    /** Reglas que dependen de los importes calculados. */
    private static void validarReglasDeConjunto(Factura factura) {
        Totales totales = CalculadoraTributaria.calcularTotales(factura);
        List<String> errores = new ArrayList<>();
        if (factura.tipoComprobante() == ClaseComprobante.BOLETA
                && factura.receptor().tipoDocumento() == TipoDocumentoIdentidad.SIN_DOCUMENTO
                && totales.importeTotal().compareTo(UMBRAL_IDENTIFICACION_BOLETA) > 0) {
            errores.add("receptor: una boleta mayor a S/ " + UMBRAL_IDENTIFICACION_BOLETA
                    + " debe identificar al comprador");
        }
        if (factura.formaPago().esCredito() && factura.formaPago().montoNetoPendiente() != null
                && factura.formaPago().montoNetoPendiente().compareTo(totales.importeTotal()) > 0) {
            errores.add("forma_pago.monto_neto_pendiente: no puede superar el importe total del comprobante");
        }
        if (!errores.isEmpty()) {
            throw new DatosFacturaInvalidosException(errores);
        }
    }
}
