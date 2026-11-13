// Autor: Gilmer Ayala Falcón · UNAS-FIIS · Arquitectura de Software · Grupo A
package pe.abc.facturacion.infraestructura.entrada;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.InputFormat;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SchemaValidatorsConfig;
import com.networknt.schema.SpecVersion;

/**
 * Primer control de la entrada: la forma de los datos, con el JSON Schema 2020-12 del contrato
 * ({@code specs/schemas/factura-entrada.schema.json}). Se aplica el archivo del repositorio, no una copia.
 *
 * <p>Las reglas que dependen de calculos o de digitos verificadores (RUC modulo 11, boleta mayor a S/ 700,
 * suma de cuotas) las aplica el dominio ({@code LectorFactura}).
 */
public final class EsquemaEntrada {

    static final String RECURSO = "/schemas/factura-entrada.schema.json";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final JsonSchema esquema;

    public EsquemaEntrada() {
        try (InputStream in = EsquemaEntrada.class.getResourceAsStream(RECURSO)) {
            if (in == null) {
                throw new IllegalStateException("No se encuentra " + RECURSO + " en el classpath");
            }
            SchemaValidatorsConfig config = SchemaValidatorsConfig.builder().formatAssertionsEnabled(true).build();
            this.esquema = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012)
                    .getSchema(in, InputFormat.JSON, config);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Errores del esquema (lista vacia si la entrada es valida). */
    public List<String> validar(Map<String, Object> datos) {
        JsonNode nodo = MAPPER.valueToTree(datos);
        return esquema.validate(nodo).stream().map(m -> m.getMessage()).sorted().collect(Collectors.toList());
    }
}
