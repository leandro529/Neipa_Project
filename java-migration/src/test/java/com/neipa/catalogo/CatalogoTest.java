package com.neipa.catalogo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.StringReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.neipa.domain.CondicionEstandar;
import com.neipa.domain.OperadorComparacion;
import com.neipa.domain.OrigenCondicion;
import com.neipa.domain.ValorEstandar;

/** Catalogo de parametros: lectura del CSV y validacion de reglas (datos ficticios con la forma real). */
class CatalogoTest {

    static final String CSV = "﻿fuente,release,tecnologia,clase,ruta_clase,parametro,parametro_doc,estructura,nombre,tipo,"
            + "unidad,rango,minimo,maximo,paso,minimo_interno,maximo_interno,paso_interno,enumeracion,formula,"
            + "default_valor,default_interno,modificacion,funciones,relacionados,relaciones,descripcion\r\n"
            + "LTE_demo.xls,20,LTE FDD,LNCEL,MRBTS/LNBTS/LNCEL,n310Qci1,n310Qci1,,N310 for QCI1,Enumeration,,"
            + "\"1: n1 \n2: n2 \n4: n4 \n6: n6 \n8: n8\",,,,,,,1=n1|2=n2|4=n4|6=n6|8=n8,,,,On-line,,"
            + "\"LNCEL - cellTechnology\nLNCEL - n310PubSafety\",,\"Constant \"\"n310\"\", con coma, y salto\nde linea\"\r\n"
            + "LTE_demo.xls,20,LTE FDD,LNCEL,MRBTS/LNBTS/LNCEL,pMax,pMax,,Maximum output power,Number,dBm,"
            + "\"11...43 dBm, step 1 dBm\",11,43,1,110,430,10,,internal_value = gui_value * 10,,,Requires object locking,,,,\r\n"
            + "SBTS_demo.xls,19b,SBTS,LNCEL,MRBTS/LNBTS/LNCEL,pMax,pMax,,Maximum output power,Number,dBm,"
            + "\"0...60 dBm, step 0.1 dBm\",0,60,0.1,0,600,1,,internal_value = gui_value * 10,,,Requires object locking,,,,\r\n"
            + "LTE_demo.xls,20,LTE FDD,LNBTS,MRBTS/LNBTS,actFoo,actFoo,,Activate foo,Boolean,,\"0 (false), 1 (true)\","
            + ",,,,,,0=false|1=true,,0,0,On-line,,,,\r\n";

    static CatalogoService servicio(Path carpeta) throws Exception {
        MemoriaCatalogoRepositorio repo = new MemoriaCatalogoRepositorio();
        Files.write(carpeta.resolve("catalogo_demo.csv"), CSV.getBytes(StandardCharsets.UTF_8));
        CatalogoService s = new CatalogoService(repo, carpeta.toString());
        s.importar("catalogo_demo.csv");
        return s;
    }

    @Test
    void leeElCsvConComillasComasYSaltosDeLinea() throws Exception {
        List<ParametroCatalogo> filas = new ArrayList<>();
        assertEquals(4, CatalogoCsv.leer(new StringReader(CSV), filas::add));
        ParametroCatalogo n310 = filas.get(0);
        assertEquals("LNCEL", n310.getClase());
        assertEquals("n310Qci1", n310.getParametro());
        assertEquals("Constant \"n310\", con coma, y salto\nde linea", n310.getDescripcion());
        assertEquals(Arrays.asList("1", "2", "4", "6", "8"), new ArrayList<>(n310.valoresEnumerados().keySet()));
        assertEquals(430.0, filas.get(1).getMinimoInterno() == null ? -1 : filas.get(1).getMaximoInterno());
        assertTrue(filas.get(1).requiereBloqueo());
    }

    @Test
    void validaEnumeracionesRangosInternosYCondiciones(@TempDir Path carpeta) throws Exception {
        CatalogoService s = servicio(carpeta);
        assertEquals(Arrays.asList("catalogo_demo.csv"), s.archivosDisponibles());

        assertTrue(s.problemas(regla("n310Qci1", "LNCEL", "8")).isEmpty());
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> s.validar(regla("n310Qci1", "LNCEL", "7")));
        assertTrue(ex.getMessage().contains("LNCEL.n310Qci1"), ex.getMessage());
        assertTrue(ex.getMessage().contains("8=n8"), ex.getMessage());

        // pMax: LTE 20 acepta hasta 430 y SBTS 19B hasta 600 -> 460 es valido (alguna release lo acepta).
        assertTrue(s.problemas(regla("pMax", "LNCEL", "460")).isEmpty());
        assertFalse(s.problemas(regla("pMax", "LNCEL", "700")).isEmpty());
        assertFalse(s.problemas(regla("pMax", "LNCEL", "alto")).isEmpty());

        ValorEstandar rango = new ValorEstandar();
        rango.setParametro("pMax");
        rango.setObjetoGestionado("LNCEL");
        rango.setOperador(OperadorComparacion.RANGO);
        rango.setMinimo(new BigDecimal("400"));
        rango.setMaximo(new BigDecimal("650"));
        List<String> p = s.problemas(rango);
        assertEquals(1, p.size(), p.toString());
        assertTrue(p.get(0).contains("maximo 650"), p.get(0));

        // Condicion sobre un ancestro: LNBTS.actFoo solo admite 0/1.
        ValorEstandar conCondicion = regla("n310Qci1", "LNCEL", "8");
        conCondicion.getCondiciones().add(new CondicionEstandar(OrigenCondicion.PARAMETRO, "LNBTS.actFoo",
                OperadorComparacion.IGUAL, "2"));
        assertEquals(1, s.problemas(conCondicion).size());

        // Un parametro que no esta en el catalogo no se valida (por ejemplo los de la demo legacy).
        assertTrue(s.problemas(regla("PotenciaTX_dBm", null, "999")).isEmpty());
        // La clase sale del objeto gestionado: n310Qci1 no existe para SIB en el catalogo.
        assertTrue(s.problemas(regla("n310Qci1", "LNCEL-1/SIB-0", "7")).isEmpty());

        List<ParametroCatalogo> ficha = s.ficha("LNCEL", "pMax");
        assertEquals(2, ficha.size());
        Map<String, String> enumerados = s.ficha("", "actFoo").get(0).valoresEnumerados();
        assertEquals("true", enumerados.get("1"));
    }

    @Test
    void clasesConSusObjetosPadre(@TempDir Path carpeta) throws Exception {
        CatalogoService s = servicio(carpeta);
        List<Map<String, Object>> clases = s.clases();
        assertEquals(Arrays.asList("LNBTS", "LNCEL"),
                clases.stream().map(c -> c.get("clase")).collect(java.util.stream.Collectors.toList()));
        Map<String, Object> lncel = clases.get(1);
        assertEquals(2, ((Number) lncel.get("parametros")).intValue());
        assertEquals(Arrays.asList("LNBTS"), lncel.get("ancestros"));
        assertEquals(Arrays.asList(), clases.get(0).get("ancestros"));
        assertEquals(Arrays.asList("LNBTS", "LNCEL"), CatalogoService.ancestros("SIB", "MRBTS/LNBTS/LNCEL/SIB"));
        assertEquals(Arrays.asList("LNBTS"), CatalogoService.ancestros("X", null));

        CatalogoService vacio = new CatalogoService(new MemoriaCatalogoRepositorio(), carpeta.toString());
        assertTrue(vacio.clases().size() >= 5, "sin catalogo ofrece las clases basicas del CM");
    }

    @Test
    void rechazaArchivosFueraDeLaCarpeta(@TempDir Path carpeta) throws Exception {
        CatalogoService s = servicio(carpeta);
        assertThrows(IllegalArgumentException.class, () -> s.importar("../secreto.csv"));
        assertThrows(IllegalArgumentException.class, () -> s.importar("dump.dba"));
        assertThrows(IllegalArgumentException.class, () -> s.importar("no_existe.csv"));
    }

    static ValorEstandar regla(String parametro, String objeto, String valor) {
        ValorEstandar v = new ValorEstandar();
        v.setParametro(parametro);
        v.setObjetoGestionado(objeto);
        v.setOperador(OperadorComparacion.IGUAL);
        v.setValorReferencia(valor);
        return v;
    }
}
