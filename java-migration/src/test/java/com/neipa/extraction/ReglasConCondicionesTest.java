package com.neipa.extraction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.neipa.domain.CondicionEstandar;
import com.neipa.domain.ElementoDeRed;
import com.neipa.domain.OperadorComparacion;
import com.neipa.domain.OrigenCondicion;
import com.neipa.domain.ValorEstandar;

/**
 * Ampliacion "reglas por segmento y con condiciones": para un mismo
 * parametro se elige la regla que aplica al elemento y, entre varias, la de
 * mayor prioridad y luego la mas especifica.
 */
class ReglasConCondicionesTest {

    private static ValorEstandar igual(long id, String valor) {
        ValorEstandar v = new ValorEstandar("srvccDelayTimer", OperadorComparacion.IGUAL, valor, null);
        v.setIdValorEstandar(id);
        return v;
    }

    private static ElementoDeRed elemento(String nombre, Long idSegmento) {
        ElementoDeRed e = new ElementoDeRed(nombre, "4G", "NSN", idSegmento);
        e.setIdElemento(1L);
        return e;
    }

    private static Map<String, String> vigentes(String... pares) {
        Map<String, String> m = new HashMap<>();
        for (int i = 0; i < pares.length; i += 2) {
            m.put(pares[i], pares[i + 1]);
        }
        return m;
    }

    @Test
    void sinCondicionesSeUsaLaReglaGeneral() {
        ValorEstandar general = igual(1, "4");
        assertEquals(general, EstandarProvider.elegir(Collections.singletonList(general),
                elemento("Sitio-1", 1L), vigentes()).get());
    }

    @Test
    void laReglaDelSegmentoLeGanaALaGeneral() {
        ValorEstandar general = igual(1, "4");
        ValorEstandar laboratorio = igual(2, "5");
        laboratorio.setIdSegmento(3L);
        List<ValorEstandar> reglas = Arrays.asList(general, laboratorio);

        assertEquals("5", EstandarProvider.elegir(reglas, elemento("Lab", 3L), vigentes()).get().getValorReferencia());
        assertEquals("4", EstandarProvider.elegir(reglas, elemento("Norte", 1L), vigentes()).get().getValorReferencia());
    }

    @Test
    void unaCondicionSobreOtroParametroCambiaElValorEsperado() {
        // Ejemplo del usuario: si la potencia es 20 dBm, el parametro tiene que valer 10.
        ValorEstandar general = igual(1, "4");
        ValorEstandar conPotencia20 = igual(2, "10");
        conPotencia20.setCondiciones(Collections.singletonList(new CondicionEstandar(
                OrigenCondicion.PARAMETRO, "PotenciaTX_dBm", OperadorComparacion.IGUAL, "20")));
        List<ValorEstandar> reglas = Arrays.asList(general, conPotencia20);

        assertEquals("10", EstandarProvider.elegir(reglas, elemento("S", 1L),
                vigentes("PotenciaTX_dBm", "20")).get().getValorReferencia());
        assertEquals("4", EstandarProvider.elegir(reglas, elemento("S", 1L),
                vigentes("PotenciaTX_dBm", "43")).get().getValorReferencia());
        // Si el otro parametro no se extrajo, la condicion no se cumple.
        assertEquals("4", EstandarProvider.elegir(reglas, elemento("S", 1L), vigentes()).get().getValorReferencia());
    }

    @Test
    void todasLasCondicionesSeTienenQueCumplir() {
        ValorEstandar regla = igual(1, "7");
        regla.setCondiciones(Arrays.asList(
                new CondicionEstandar(OrigenCondicion.ATRIBUTO_ELEMENTO, "NOMBRE", OperadorComparacion.COINCIDE, "*_URB_*"),
                new CondicionEstandar(OrigenCondicion.PARAMETRO, "PotenciaTX_dBm", OperadorComparacion.MAYOR_IGUAL, "40")));
        List<ValorEstandar> reglas = Collections.singletonList(regla);

        assertEquals(regla, EstandarProvider.elegir(reglas, elemento("S_URB_1", 1L),
                vigentes("PotenciaTX_dBm", "41")).get());
        assertFalse(EstandarProvider.elegir(reglas, elemento("S_URB_1", 1L),
                vigentes("PotenciaTX_dBm", "39")).isPresent());
        assertFalse(EstandarProvider.elegir(reglas, elemento("S_RUR_1", 1L),
                vigentes("PotenciaTX_dBm", "41")).isPresent());
    }

    @Test
    void aIgualEspecificidadGanaLaDeMayorPrioridad() {
        ValorEstandar porSegmento = igual(1, "5");
        porSegmento.setIdSegmento(3L);
        ValorEstandar porTecnologia = igual(2, "6");
        porTecnologia.setCondiciones(Collections.singletonList(new CondicionEstandar(
                OrigenCondicion.ATRIBUTO_ELEMENTO, "TECNOLOGIA", OperadorComparacion.IGUAL, "4G")));
        List<ValorEstandar> reglas = Arrays.asList(porSegmento, porTecnologia);

        // Empate: gana la de menor id.
        assertEquals("5", EstandarProvider.elegir(reglas, elemento("S", 3L), vigentes()).get().getValorReferencia());
        porTecnologia.setPrioridad(10);
        assertEquals("6", EstandarProvider.elegir(reglas, elemento("S", 3L), vigentes()).get().getValorReferencia());
    }

    @Test
    void noSePuedeGuardarUnaCondicionConRango() {
        ValorEstandar regla = igual(1, "5");
        regla.setCondiciones(Collections.singletonList(new CondicionEstandar(
                OrigenCondicion.PARAMETRO, "PotenciaTX_dBm", OperadorComparacion.RANGO, "30")));
        assertThrows(IllegalArgumentException.class, regla::validar);
    }

    @Test
    void elValorObjetivoSeDerivaDeLaRegla() {
        assertEquals("4", igual(1, "4").valorObjetivoPara("5").get());

        ValorEstandar rango = new ValorEstandar("AnguloTilt_grados", new BigDecimal("0.00"), new BigDecimal("12.00"), "grados");
        assertEquals("12", rango.valorObjetivoPara("14").get());
        assertEquals("0", rango.valorObjetivoPara("-2").get());

        ValorEstandar menorQue = new ValorEstandar("tacLimitGbrEmergency", OperadorComparacion.MENOR_QUE, "500000", null);
        assertFalse(menorQue.valorObjetivoPara("500000").isPresent());
        menorQue.setValorObjetivo("450000");
        assertEquals("450000", menorQue.valorObjetivoPara("500000").get());
    }
}
