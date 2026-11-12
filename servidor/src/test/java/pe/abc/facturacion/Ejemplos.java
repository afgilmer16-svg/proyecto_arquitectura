package pe.abc.facturacion;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import pe.abc.facturacion.infraestructura.entrada.LectorJson;

/** Acceso a los ejemplos compartidos del repositorio ({@code specs/ejemplos} y la carpeta del parcial). */
public final class Ejemplos {

    public static final Path RAIZ = Path.of("..").toAbsolutePath().normalize();
    public static final Path VALIDOS = RAIZ.resolve("specs/ejemplos/validos");
    public static final Path INVALIDOS = RAIZ.resolve("specs/ejemplos/invalidos");
    public static final Path REFERENCIA_XML = RAIZ.resolve("specs/ejemplos/xml-referencia");
    public static final Path EJEMPLO_PARCIAL = RAIZ.resolve("docs/segundo-parcial/ejemplos/factura-abarrotes.json");

    private Ejemplos() {
    }

    public static Map<String, Object> json(Path ruta) {
        return LectorJson.leer(ruta);
    }

    public static Map<String, Object> parcial() {
        return json(EJEMPLO_PARCIAL);
    }

    /** Ejemplos {@code .json} de una carpeta, ordenados por nombre. */
    public static List<Path> listar(Path carpeta) {
        try (Stream<Path> s = Files.list(carpeta)) {
            return s.filter(p -> p.toString().endsWith(".json")).sorted().toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** Nombre sin extension: {@code 01-factura-credito-con-cuotas}. */
    public static String nombre(Path ruta) {
        String n = ruta.getFileName().toString();
        return n.substring(0, n.length() - ".json".length());
    }
}
