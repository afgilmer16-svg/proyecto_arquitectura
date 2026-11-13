// Autor: Gilmer Ayala Falcón · UNAS-FIIS · Arquitectura de Software · Grupo A
package pe.abc.facturacion.infraestructura.entrada;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import pe.abc.facturacion.dominio.error.ErrorFacturacionXml;

/**
 * Lee el JSON de entrada <b>siempre en UTF-8</b>.
 *
 * <p>En Windows la pagina de codigos por defecto suele ser WIN1252: si el archivo se guardo asi, las tildes y
 * la "ñ" se corromperian en silencio. Por eso se lee como UTF-8 estricto (con soporte del BOM del Bloc de
 * notas) y, si falla, se explica el problema en lugar de generar un XML con caracteres danados.
 *
 * <p>Los numeros decimales se leen como {@code BigDecimal} para no perder exactitud.
 */
public final class LectorJson {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS);

    private LectorJson() {
    }

    public static Map<String, Object> leer(Path ruta) {
        try {
            return leer(Files.readAllBytes(ruta), ruta.toString());
        } catch (IOException e) {
            throw new ErrorFacturacionXml("No se puede leer " + ruta + ": " + e.getMessage(), e);
        }
    }

    public static Map<String, Object> leer(byte[] datos, String origen) {
        int desde = datos.length >= 3 && (datos[0] & 0xFF) == 0xEF && (datos[1] & 0xFF) == 0xBB
                && (datos[2] & 0xFF) == 0xBF ? 3 : 0;
        String texto;
        try {
            texto = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(datos, desde, datos.length - desde)).toString();
        } catch (CharacterCodingException e) {
            throw new ErrorFacturacionXml(origen + " no está en UTF-8. Probablemente se guardó en WIN1252/ANSI: "
                    + "vuelve a guardarlo como UTF-8.", e);
        }
        try {
            return MAPPER.readValue(texto, new TypeReference<Map<String, Object>>() {
            });
        } catch (JsonProcessingException e) {
            throw new ErrorFacturacionXml(origen + " no es un JSON válido: " + e.getOriginalMessage(), e);
        }
    }
}
