package pe.abc.facturacion;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Linea de comandos: codigos de salida y mensajes claros. */
class AplicacionTest {

    private final ByteArrayOutputStream out = new ByteArrayOutputStream();
    private final ByteArrayOutputStream err = new ByteArrayOutputStream();

    private int ejecutar(String... args) {
        return Aplicacion.ejecutar(args, new PrintStream(out, true, StandardCharsets.UTF_8),
                new PrintStream(err, true, StandardCharsets.UTF_8));
    }

    private String salida() {
        return out.toString(StandardCharsets.UTF_8);
    }

    private String error() {
        return err.toString(StandardCharsets.UTF_8);
    }

    @Test
    void generaYValidaElXmlDelEjemplo(@TempDir Path dir) throws Exception {
        Path xml = dir.resolve("salida/F001-00000001.xml");
        int codigo = ejecutar(Ejemplos.EJEMPLO_PARCIAL.toString(), xml.toString(), "--correlativo", "1");
        assertThat(codigo).isZero();
        assertThat(salida()).contains("XML generado y validado contra UBL-Invoice-2.1.xsd");
        assertThat(Files.readString(xml)).contains("<cbc:ID>F001-00000001</cbc:ID>");
    }

    @Test
    void aceptaElRegimenDeRestaurantesYHoteles(@TempDir Path dir) {
        int codigo = ejecutar(Ejemplos.EJEMPLO_PARCIAL.toString(), dir.resolve("a.xml").toString(),
                "--correlativo", "1", "--regimen-igv", "RESTAURANTES_HOTELES");
        assertThat(codigo).isZero();
    }

    @Test
    void rechazaUnCorrelativoInvalidoConCodigo1ONoNumericoConCodigo2(@TempDir Path dir) {
        assertThat(ejecutar(Ejemplos.EJEMPLO_PARCIAL.toString(), dir.resolve("a.xml").toString(), "--correlativo", "0"))
                .isEqualTo(1);
        assertThat(error()).contains("correlativo");
        assertThat(ejecutar(Ejemplos.EJEMPLO_PARCIAL.toString(), dir.resolve("a.xml").toString(), "--correlativo", "uno"))
                .isEqualTo(2);
    }

    @Test
    void usoIncorrectoYRegimenDesconocido(@TempDir Path dir) {
        assertThat(ejecutar("solo-un-argumento")).isEqualTo(2);
        assertThat(error()).contains("Uso:");
        assertThat(ejecutar(Ejemplos.EJEMPLO_PARCIAL.toString(), dir.resolve("a.xml").toString(), "--correlativo", "1",
                "--regimen-igv", "OTRO")).isEqualTo(2);
    }

    @Test
    void unEjemploInvalidoSeRechazaSinEscribirElArchivo(@TempDir Path dir) {
        Path xml = dir.resolve("no-debe-existir.xml");
        int codigo = ejecutar(Ejemplos.INVALIDOS.resolve("i08-cliente-envia-correlativo.json").toString(),
                xml.toString(), "--correlativo", "1");
        assertThat(codigo).isEqualTo(1);
        assertThat(error()).contains("factura-entrada.schema.json");
        assertThat(xml).doesNotExist();
    }

    @Test
    void unaViolacionDeDominioSeInformaConLaRutaDelCampo(@TempDir Path dir) {
        Path xml = dir.resolve("x.xml");
        int codigo = ejecutar(Ejemplos.INVALIDOS.resolve("i03-ruc-con-digito-verificador-incorrecto.json").toString(),
                xml.toString(), "--correlativo", "1");
        assertThat(codigo).isEqualTo(1);
        assertThat(error()).contains("emisor.ruc");
        assertThat(xml).doesNotExist();
    }

    @Test
    void entradaInexistenteOWin1252(@TempDir Path dir) throws Exception {
        assertThat(ejecutar(dir.resolve("no-existe.json").toString(), dir.resolve("a.xml").toString(),
                "--correlativo", "1")).isEqualTo(1);
        Path ansi = dir.resolve("ansi.json");
        Files.write(ansi, "{\"a\": \"ñ\"}".getBytes(java.nio.charset.Charset.forName("windows-1252")));
        assertThat(ejecutar(ansi.toString(), dir.resolve("b.xml").toString(), "--correlativo", "1")).isEqualTo(1);
        assertThat(error()).contains("UTF-8");
    }
}
