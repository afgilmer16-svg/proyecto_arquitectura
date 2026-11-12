package pe.abc.facturacion.dominio.modelo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import pe.abc.facturacion.dominio.catalogo.CategoriaOperacion;
import pe.abc.facturacion.dominio.catalogo.Catalogos;
import pe.abc.facturacion.dominio.catalogo.RegimenIgv;
import pe.abc.facturacion.dominio.catalogo.SistemaCalculoIsc;
import pe.abc.facturacion.dominio.catalogo.ClaseComprobante;
import pe.abc.facturacion.dominio.catalogo.TipoDocumentoIdentidad;

/**
 * Modelo de dominio del comprobante: objetos de valor inmutables, ya validados. Se construyen en
 * {@link pe.abc.facturacion.dominio.validacion.LectorFactura}; no dependen de ninguna libreria XML.
 */
public final class Modelo {

    private Modelo() {
    }

    public record Emisor(String ruc, String razonSocial, String direccion, String ubigeo, String nombreComercial,
                         String codigoEstablecimiento) {
        public Emisor(String ruc, String razonSocial, String direccion, String ubigeo, String nombreComercial) {
            this(ruc, razonSocial, direccion, ubigeo, nombreComercial, Catalogos.CODIGO_DOMICILIO_FISCAL);
        }
    }

    public record Receptor(TipoDocumentoIdentidad tipoDocumento, String numeroDocumento, String razonSocial,
                           String direccion) {
    }

    public record Cuota(BigDecimal monto, LocalDate fechaVencimiento) {
    }

    /** Forma de pago (RS 000193-2020/SUNAT): "Contado" o "Credito" con sus cuotas. */
    public record FormaPago(String tipo, BigDecimal montoNetoPendiente, List<Cuota> cuotas) {
        public static FormaPago contado() {
            return new FormaPago("Contado", null, List.of());
        }

        public boolean esCredito() {
            return "Credito".equals(tipo);
        }
    }

    public record LineaFactura(int numeroLinea, String codigoProducto, String descripcion, String unidadMedida,
                               BigDecimal cantidad, BigDecimal valorUnitario, String tipoAfectacionIgv,
                               SistemaCalculoIsc sistemaIsc, BigDecimal tasaIsc, BigDecimal montoFijoIsc,
                               String unidadBaseIsc, BigDecimal precioVentaPublicoSugerido,
                               Integer cantidadBolsas) {

        public CategoriaOperacion categoria() {
            return Catalogos.CATEGORIA_POR_AFECTACION.get(tipoAfectacionIgv);
        }
    }

    public record Factura(ClaseComprobante tipoComprobante, String serie, long correlativo, LocalDate fechaEmision,
                          String moneda, Emisor emisor, Receptor receptor, List<LineaFactura> lineas,
                          LocalTime horaEmision, String tipoOperacion, FormaPago formaPago,
                          RegimenIgv regimenIgv) {

        /** Serie y numero del comprobante ({@code cbc:ID}), por ejemplo {@code F001-00000042}. */
        public String numero() {
            return String.format("%s-%08d", serie, correlativo);
        }
    }
}
