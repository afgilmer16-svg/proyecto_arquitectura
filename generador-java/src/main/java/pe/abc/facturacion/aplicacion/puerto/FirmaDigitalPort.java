// Autor: Gilmer Ayala Falcón · UNAS-FIIS · Arquitectura de Software · Grupo A
package pe.abc.facturacion.aplicacion.puerto;

/**
 * Puerto de salida opcional: firma digitalmente el XML ya generado (XMLDSig, X.509).
 *
 * <p>El generador entrega el XML sin firma, que es valido contra el XSD. Quien necesite firmarlo usa un
 * adaptador de este puerto; tambien se puede dejar que el OSE (p. ej. Nubefact) firme por su cuenta.
 */
public interface FirmaDigitalPort {

    /**
     * @param xml el XML UTF-8 generado por {@link GeneradorXmlUblPort}
     * @return el mismo XML con {@code ext:UBLExtensions/.../ds:Signature} como primer hijo de la raiz
     * @throws pe.abc.facturacion.dominio.error.FirmaDigitalException si no se puede firmar
     */
    byte[] firmar(byte[] xml);
}
