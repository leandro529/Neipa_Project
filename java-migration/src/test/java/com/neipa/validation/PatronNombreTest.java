package com.neipa.validation;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PatronNombreTest {

    @Test
    void elAsteriscoEsCualquierTextoYElGuionBajoEsLiteral() {
        assertTrue(PatronNombre.coincide("Sitio-Lab_test_01", "*_test_*"));
        assertTrue(PatronNombre.coincide("SITIO_TEST_9", "*_test_*"));
        assertFalse(PatronNombre.coincide("Sitio-Lab-001", "*_test_*"));
        // En LIKE el '_' es comodin: "XtestY" coincidiria con %_test_%; aca no.
        assertFalse(PatronNombre.coincide("AtestB", "*_test_*"));
    }

    @Test
    void elSignoDePreguntaEsUnSoloCaracter() {
        assertTrue(PatronNombre.coincide("RC2", "RC?"));
        assertFalse(PatronNombre.coincide("RC12", "RC?"));
    }

    @Test
    void losCaracteresEspecialesDeRegexSonLiterales() {
        assertTrue(PatronNombre.coincide("a.b(c)", "a.b(c)"));
        assertFalse(PatronNombre.coincide("axb(c)", "a.b(c)"));
    }

    @Test
    void coincideLikeAceptaAmbosEstilos() {
        assertTrue(PatronNombre.coincideLike("CO1L100X", "CO%L100%"));
        assertTrue(PatronNombre.coincideLike("CO1L100X", "CO*L100*"));
    }
}
