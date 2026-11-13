// Autor: Gilmer Ayala Falcón · UNAS-FIIS · Arquitectura de Software · Grupo A
package pe.abc.facturacion.infraestructura.entrada;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import pe.abc.facturacion.Ejemplos;
import pe.abc.facturacion.dominio.error.ErrorFacturacionXml;

/** Lectura del JSON en UTF-8 estricto y control de forma (JSON Schema 2020-12 del contrato). */
class EntradaTest {

    /** Ejemplos invalidos que el JSON Schema rechaza (los demas los rechaza el dominio). */
    private static final Set<String> RECHAZADOS_POR_EL_ESQUEMA = Set.of(
            "rechazo-01-boleta-serie-f", "rechazo-02-factura-sin-ruc-del-cliente", "rechazo-04-unidad-de-medida-desconocida",
            "rechazo-05-importe-numerico", "rechazo-06-credito-sin-cuotas", "rechazo-07-isc-sin-tasa",
            "rechazo-08-correlativo-en-la-entrada", "rechazo-09-afectacion-ivap");

    private static final EsquemaEntrada ESQUEMA = new EsquemaEntrada();

    static java.util.List<Path> todosLosEjemplos() {
        var lista = new java.util.ArrayList<Path>(Ejemplos.listar(Ejemplos.VALIDOS));
        lista.add(Ejemplos.EJEMPLO_PARCIAL);
        lista.addAll(Ejemplos.listar(Ejemplos.INVALIDOS));
        return lista;
    }

    // ------------------------------------------------------------ LectorJson
    @Test
    void leeUtf8ConTildesYEnie(@TempDir Path dir) throws Exception {
        Path f = dir.resolve("a.json");
        Files.writeString(f, "{\"razon\": \"Costeño S.A.\"}", StandardCharsets.UTF_8);
        assertThat(LectorJson.leer(f)).containsEntry("razon", "Costeño S.A.");
    }

    @Test
    void aceptaElBomDelBlocDeNotas(@TempDir Path dir) throws Exception {
        Path f = dir.resolve("bom.json");
        byte[] cuerpo = "{\"a\": \"ñ\"}".getBytes(StandardCharsets.UTF_8);
        byte[] conBom = new byte[cuerpo.length + 3];
        conBom[0] = (byte) 0xEF;
        conBom[1] = (byte) 0xBB;
        conBom[2] = (byte) 0xBF;
        System.arraycopy(cuerpo, 0, conBom, 3, cuerpo.length);
        Files.write(f, conBom);
        assertThat(LectorJson.leer(f)).containsEntry("a", "ñ");
    }

    @Test
    void rechazaUnArchivoGuardadoEnWin1252SinCorromperlo(@TempDir Path dir) throws Exception {
        Path f = dir.resolve("ansi.json");
        Files.write(f, "{\"razon\": \"Costeño\"}".getBytes(Charset.forName("windows-1252")));
        assertThatThrownBy(() -> LectorJson.leer(f)).isInstanceOf(ErrorFacturacionXml.class)
                .hasMessageContaining("no está en UTF-8").hasMessageContaining("WIN1252");
    }

    @Test
    void jsonMalFormadoYArchivoInexistente(@TempDir Path dir) throws Exception {
        Path f = dir.resolve("roto.json");
        Files.writeString(f, "{\"a\": ");
        assertThatThrownBy(() -> LectorJson.leer(f)).isInstanceOf(ErrorFacturacionXml.class)
                .hasMessageContaining("no es un JSON válido");
        assertThatThrownBy(() -> LectorJson.leer(dir.resolve("no-existe.json")))
                .isInstanceOf(ErrorFacturacionXml.class).hasMessageContaining("No se puede leer");
    }

    @Test
    void losDecimalesSeLeenSinPerderExactitud() {
        Map<String, Object> datos = LectorJson.leer("{\"x\": 0.1}".getBytes(StandardCharsets.UTF_8), "prueba");
        assertThat(datos.get("x")).isInstanceOf(BigDecimal.class);
        assertThat((BigDecimal) datos.get("x")).isEqualByComparingTo("0.1");
    }

    // --------------------------------------------------------- EsquemaEntrada
    @ParameterizedTest(name = "{0}")
    @MethodSource("todosLosEjemplos")
    void elEsquemaAceptaLosValidosYRechazaSoloLoQueLeCorresponde(Path ejemplo) {
        String nombre = Ejemplos.nombre(ejemplo);
        boolean esperaRechazo = RECHAZADOS_POR_EL_ESQUEMA.contains(nombre);
        assertThat(ESQUEMA.validar(Ejemplos.json(ejemplo)).isEmpty()).as(nombre).isEqualTo(!esperaRechazo);
    }

    @Test
    void elEsquemaNoAdmiteElCorrelativoNiImportesNumericos() {
        Map<String, Object> d = new java.util.HashMap<>(Ejemplos.parcial());
        d.put("correlativo", 1);
        assertThat(ESQUEMA.validar(d)).anyMatch(m -> m.contains("correlativo"));
    }
}
