package pe.abc.facturacion.aplicacion.puerto;

import pe.abc.facturacion.dominio.modelo.Modelo.Factura;

/**
 * Puerto de salida: genera el XML UBL 2.1 de un comprobante ya validado.
 *
 * <p>Es el puerto de la arquitectura del primer parcial (feature 0003). Hay un solo adaptador,
 * {@code GeneradorUblJaxbAdapter}, que usa las clases generadas desde {@code UBL-Invoice-2.1.xsd}.
 */
public interface GeneradorXmlUblPort {

    /**
     * @return el XML en UTF-8 con declaracion, ya validado contra el XSD
     * @throws pe.abc.facturacion.dominio.error.XmlNoConformeXsdException si el XML no cumple el esquema
     */
    byte[] generar(Factura factura);
}
