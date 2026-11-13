// Autor: Gilmer Ayala Falcón · UNAS-FIIS · Arquitectura de Software · Grupo A
package pe.abc.facturacion.dominio;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Random;

import org.junit.jupiter.api.Test;

import pe.abc.facturacion.dominio.validacion.GeneradorDatosDePrueba;
import pe.abc.facturacion.dominio.validacion.ValidadorRuc;

class GeneradorDatosDePruebaTest {

    @Test
    void miles_de_rucs_aleatorios_pasan_el_modulo_11() {
        Random azar = new Random(2026);
        for (int i = 0; i < 5000; i++) {
            String ruc = GeneradorDatosDePrueba.rucAleatorio(azar);
            assertThat(ValidadorRuc.esValido(ruc)).as(ruc).isTrue();
            assertThat(ruc).hasSize(11).startsWith("20");
        }
    }

    @Test
    void la_misma_semilla_da_el_mismo_ruc() {
        assertThat(GeneradorDatosDePrueba.rucAleatorio(new Random(1))).isEqualTo(GeneradorDatosDePrueba.rucAleatorio(new Random(1)));
    }

    @Test
    void la_serie_tiene_la_letra_pedida_y_tres_caracteres() {
        Random azar = new Random(5);
        for (int i = 0; i < 500; i++) {
            assertThat(GeneradorDatosDePrueba.serieAleatoria('F', azar)).matches("F[A-Z0-9]{3}");
            assertThat(GeneradorDatosDePrueba.serieAleatoria('B', azar)).matches("B[A-Z0-9]{3}");
        }
    }
}
