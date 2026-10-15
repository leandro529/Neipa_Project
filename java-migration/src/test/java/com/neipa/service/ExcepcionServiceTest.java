package com.neipa.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.Test;

import com.neipa.domain.AlcanceExcepcion;
import com.neipa.domain.ElementoDeRed;
import com.neipa.domain.Excepcion;

class ExcepcionServiceTest {

    private static Excepcion excepcion(long id, AlcanceExcepcion alcance, String parametro) {
        Excepcion e = new Excepcion();
        e.setIdExcepcion(id);
        e.setAlcance(alcance);
        e.setParametro(parametro);
        return e;
    }

    private static ElementoDeRed elemento(long id, String nombre, long idSegmento) {
        ElementoDeRed e = new ElementoDeRed(nombre, "4G", "NSN", idSegmento);
        e.setIdElemento(id);
        return e;
    }

    @Test
    void laExcepcionPorPatronCubreLosSitiosDePrueba() {
        Excepcion patron = excepcion(2, AlcanceExcepcion.PATRON, "*");
        patron.setPatronNombre("*_test_*");

        assertTrue(ExcepcionService.elegir(Collections.singletonList(patron),
                elemento(10, "Sitio-Lab_test_01", 3)).isPresent());
        assertFalse(ExcepcionService.elegir(Collections.singletonList(patron),
                elemento(8, "Sitio-Lab-001", 3)).isPresent());
    }

    @Test
    void elParametroAsteriscoCubreTodosLosParametros() {
        Excepcion todos = excepcion(1, AlcanceExcepcion.GLOBAL, "*");
        assertTrue(todos.cubreParametro("srvccDelayTimer"));
        assertTrue(todos.cubreParametro("PotenciaTX_dBm"));
        assertFalse(excepcion(2, AlcanceExcepcion.GLOBAL, "srvccDelayTimer").cubreParametro("PotenciaTX_dBm"));
    }

    @Test
    void precedenciaElementoPatronSegmentoGlobal() {
        Excepcion global = excepcion(1, AlcanceExcepcion.GLOBAL, "*");
        Excepcion segmento = excepcion(2, AlcanceExcepcion.SEGMENTO, "*");
        segmento.setIdSegmento(3L);
        Excepcion patron = excepcion(3, AlcanceExcepcion.PATRON, "*");
        patron.setPatronNombre("*_test_*");
        Excepcion puntual = excepcion(4, AlcanceExcepcion.ELEMENTO, "*");
        puntual.setIdElemento(10L);

        ElementoDeRed sitio = elemento(10, "Sitio-Lab_test_01", 3);
        assertEquals(4L, ExcepcionService.elegir(Arrays.asList(global, segmento, patron, puntual), sitio).get().getIdExcepcion());
        assertEquals(3L, ExcepcionService.elegir(Arrays.asList(global, segmento, patron), sitio).get().getIdExcepcion());
        assertEquals(2L, ExcepcionService.elegir(Arrays.asList(global, segmento), sitio).get().getIdExcepcion());
        assertEquals(1L, ExcepcionService.elegir(Collections.singletonList(global), sitio).get().getIdExcepcion());
    }
}
