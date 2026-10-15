package com.neipa.batch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.neipa.domain.AlcanceExcepcion;
import com.neipa.domain.Auditoria;
import com.neipa.domain.CondicionEstandar;
import com.neipa.domain.ElementoDeRed;
import com.neipa.domain.Excepcion;
import com.neipa.domain.OperadorComparacion;
import com.neipa.domain.OrigenCondicion;
import com.neipa.domain.ValorEstandar;
import com.neipa.extraction.EstandarProvider;
import com.neipa.extraction.NsnVendorExtractor;
import com.neipa.extraction.VendorExtractorFactory;
import com.neipa.repository.ExcepcionRepository;
import com.neipa.repository.ValorEstandarRepository;
import com.neipa.service.ExcepcionService;

/**
 * Corre el processor real (extractor NSN simulado + eleccion de reglas +
 * excepciones) con las mismas reglas y excepciones que carga data.sql, sin
 * base de datos. Verifica los resultados esperados del segmento 3
 * (Laboratorio 4G) y que Zona Norte no cambio con la ampliacion.
 */
class EscenarioLaboratorioTest {

    private AuditoriaItemProcessor processor;

    @BeforeEach
    void preparar() {
        Map<String, List<ValorEstandar>> reglas = new HashMap<>();
        agregar(reglas, rango(1, "PotenciaTX_dBm", "30", "46", "LNCEL-1"));
        agregar(reglas, rango(2, "AnguloTilt_grados", "0", "15", "LNCEL-1"));
        agregar(reglas, regla(3, "srvccDelayTimer", OperadorComparacion.IGUAL, "4", null));
        agregar(reglas, regla(4, "tacLimitGbrEmergency", OperadorComparacion.MENOR_QUE, "500000", "LTAC-1"));
        ValorEstandar laboratorio = regla(100, "srvccDelayTimer", OperadorComparacion.IGUAL, "5", null);
        laboratorio.setIdSegmento(3L);
        agregar(reglas, laboratorio);
        ValorEstandar tiltConPotenciaAlta = rango(101, "AnguloTilt_grados", "0", "12", "LNCEL-1");
        tiltConPotenciaAlta.setCondiciones(Collections.singletonList(new CondicionEstandar(
                OrigenCondicion.PARAMETRO, "PotenciaTX_dBm", OperadorComparacion.MAYOR_IGUAL, "45")));
        agregar(reglas, tiltConPotenciaAlta);

        ValorEstandarRepository valorRepo = mock(ValorEstandarRepository.class);
        when(valorRepo.findByParametro(anyString()))
                .thenAnswer(inv -> reglas.getOrDefault(inv.getArgument(0, String.class), Collections.emptyList()));

        Excepcion potenciaNorte = new Excepcion();
        potenciaNorte.setIdExcepcion(1L);
        potenciaNorte.setAlcance(AlcanceExcepcion.SEGMENTO);
        potenciaNorte.setIdSegmento(1L);
        potenciaNorte.setParametro("PotenciaTX_dBm");
        potenciaNorte.setVigenteDesde(LocalDateTime.now().minusDays(1));
        potenciaNorte.setVigenteHasta(LocalDateTime.now().plusDays(90));
        Excepcion sitiosDePrueba = new Excepcion();
        sitiosDePrueba.setIdExcepcion(2L);
        sitiosDePrueba.setAlcance(AlcanceExcepcion.PATRON);
        sitiosDePrueba.setPatronNombre("*_test_*");
        sitiosDePrueba.setParametro("*");
        ExcepcionRepository excepcionRepo = mock(ExcepcionRepository.class);
        when(excepcionRepo.findByActivaTrue()).thenReturn(Arrays.asList(potenciaNorte, sitiosDePrueba));

        processor = new AuditoriaItemProcessor(new VendorExtractorFactory(new NsnVendorExtractor()),
                new EstandarProvider(valorRepo), new ExcepcionService(excepcionRepo));
    }

    private static void agregar(Map<String, List<ValorEstandar>> reglas, ValorEstandar v) {
        reglas.computeIfAbsent(v.getParametro(), k -> new ArrayList<>()).add(v);
    }

    private static ValorEstandar rango(long id, String parametro, String min, String max, String mo) {
        ValorEstandar v = new ValorEstandar(parametro, new BigDecimal(min), new BigDecimal(max), null);
        v.setIdValorEstandar(id);
        v.setObjetoGestionado(mo);
        return v;
    }

    private static ValorEstandar regla(long id, String parametro, OperadorComparacion op, String ref, String mo) {
        ValorEstandar v = new ValorEstandar(parametro, op, ref, null);
        v.setIdValorEstandar(id);
        v.setObjetoGestionado(mo);
        return v;
    }

    private Map<String, String> auditar(long id, String nombre, long segmento) {
        ElementoDeRed e = new ElementoDeRed(nombre, "4G", "NSN", segmento);
        e.setIdElemento(id);
        return processor.process(e).stream()
                .collect(Collectors.toMap(Auditoria::getParametro, a -> a.getResultado().toUpperCase()
                        + (a.getValorObjetivo() != null ? "->" + a.getValorObjetivo() : "")));
    }

    @Test
    void segmentoLaboratorio() {
        // Lab-001: pot 44, tilt 14 (0-15 general), srvcc 4 (el segmento pide 5), tac 500000
        Map<String, String> lab1 = auditar(8, "Sitio-Lab-001", 3);
        assertEquals("CONFORME", lab1.get("PotenciaTX_dBm"));
        assertEquals("CONFORME", lab1.get("AnguloTilt_grados"));
        assertEquals("DESVIO->5", lab1.get("srvccDelayTimer"));
        assertEquals("DESVIO", lab1.get("tacLimitGbrEmergency"));

        // Lab-002: pot 45 => tilt tiene que estar en 0-12, y esta en 14
        Map<String, String> lab2 = auditar(9, "Sitio-Lab-002", 3);
        assertEquals("DESVIO->12", lab2.get("AnguloTilt_grados"));
        assertEquals("CONFORME", lab2.get("srvccDelayTimer"));

        // Sitios de prueba: los desvios quedan EXCEPTUADOS por el patron *_test_*
        Map<String, String> test1 = auditar(10, "Sitio-Lab_test_01", 3);
        assertEquals("EXCEPTUADO->12", test1.get("AnguloTilt_grados"));
        Map<String, String> test2 = auditar(11, "Sitio-Lab_test_02", 3);
        assertEquals("EXCEPTUADO->5", test2.get("srvccDelayTimer"));
        assertEquals("EXCEPTUADO", test2.get("tacLimitGbrEmergency"));
    }

    @Test
    void zonaNorteNoCambiaConLaAmpliacion() {
        int conformes = 0;
        int desvios = 0;
        String[] nombres = {"Sitio-Norte-001", "Sitio-Norte-002", "Sitio-Norte-003", "Antena-Norte-A1"};
        for (int i = 0; i < nombres.length; i++) {
            for (String r : auditar(i + 1, nombres[i], 1).values()) {
                if (r.startsWith("CONFORME")) {
                    conformes++;
                } else if (r.startsWith("DESVIO")) {
                    desvios++;
                }
            }
        }
        assertEquals(12, conformes);
        assertEquals(4, desvios);
    }
}
