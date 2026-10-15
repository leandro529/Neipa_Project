package com.neipa.cm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.neipa.domain.AlcanceExcepcion;
import com.neipa.domain.Auditoria;
import com.neipa.domain.CondicionEstandar;
import com.neipa.domain.Corrida;
import com.neipa.domain.ElementoDeRed;
import com.neipa.domain.Excepcion;
import com.neipa.domain.OperadorComparacion;
import com.neipa.domain.OrigenCondicion;
import com.neipa.domain.Segmento;
import com.neipa.domain.ValorEstandar;
import com.neipa.reconfiguracion.Desvio;
import com.neipa.reconfiguracion.GeneradorRaml;
import com.neipa.repository.CorridaRepository;
import com.neipa.repository.ElementoDeRedRepository;
import com.neipa.repository.ExcepcionRepository;
import com.neipa.repository.SegmentoRepository;
import com.neipa.repository.ValorEstandarRepository;

/**
 * Importacion del CM y auditoria por objeto, con un dump ficticio que tiene
 * la misma estructura que el SQLite de NetAct (LNBTS, LNCEL, SIB, LNREL,
 * LNHOIF; distName, PLMN_id = RC, MRBTS_id, LNBTS_id, LNCEL_id, name, ...).
 */
class CmEscenarioTest {

    private static final String TS = "2024-01-24 05:30";
    private FakeCmFuente fuente;
    private MemoriaCmRepositorio repo;
    private final List<Segmento> segmentos = new ArrayList<>();
    private final List<ElementoDeRed> elementos = new ArrayList<>();
    private final List<Corrida> corridas = new ArrayList<>();
    private ValorEstandarRepository valorRepo;
    private ExcepcionRepository excepcionRepo;
    private CmImportService importacion;
    private CmAuditoriaService auditoria;

    private static String dn(int mrbts) {
        return "PLMN-PLMN/MRBTS-" + mrbts + "/LNBTS-" + mrbts;
    }

    private static String dn(int mrbts, int lncel) {
        return dn(mrbts) + "/LNCEL-" + lncel;
    }

    private static ValorEstandar igual(long id, String objeto, String parametro, String valor) {
        ValorEstandar v = new ValorEstandar(parametro, OperadorComparacion.IGUAL, valor, null);
        v.setIdValorEstandar(id);
        v.setObjetoGestionado(objeto);
        return v;
    }

    @BeforeEach
    void preparar() {
        fuente = new FakeCmFuente();
        fuente.tabla("LNBTS", "timeStamp", "moVersion", "distName", "PLMN_id", "MRBTS_id", "LNBTS_id", "name", "actFoo");
        fuente.fila("LNBTS", TS, "xL23R3", dn(100), "rc7", 100, 100, "SITIO_A", 0);
        fuente.fila("LNBTS", TS, "xL23R3", dn(101), "rc7", 101, 101, "SITIO_test_B", 0);
        fuente.fila("LNBTS", TS, "xL23R3", dn(102), "rc7", 102, 102, "SITIO_D", 1);
        fuente.fila("LNBTS", TS, "xL23R3", dn(200), "rc8", 200, 200, "SITIO_C", 0);

        fuente.tabla("LNCEL", "timeStamp", "moVersion", "distName", "PLMN_id", "MRBTS_id", "LNBTS_id", "LNCEL_id",
                "name", "n310Qci1", "t310Qci1", "pMax");
        fuente.fila("LNCEL", TS, "xL23R3", dn(100, 51), "rc7", 100, 100, 51, "A_51", 6, 200, 430);
        fuente.fila("LNCEL", TS, "xL23R3", dn(100, 52), "rc7", 100, 100, 52, "A_52", 8, 1000, 430);
        fuente.fila("LNCEL", TS, "xL23R3", dn(101, 51), "rc7", 101, 101, 51, "B_test_51", 8, 1000, 430);
        fuente.fila("LNCEL", TS, "xL23R3", dn(102, 51), "rc7", 102, 102, 51, "D_51", 8, 200, 430);
        fuente.fila("LNCEL", TS, "xL23R3", dn(200, 51), "rc8", 200, 200, 51, "C_51", 8, 1000, 430);

        fuente.tabla("SIB", "timeStamp", "moVersion", "distName", "PLMN_id", "MRBTS_id", "LNBTS_id", "LNCEL_id",
                "SIB_id", "n310");
        fuente.fila("SIB", TS, "xL23R3", dn(100, 51) + "/SIB-0", "rc7", 100, 100, 51, 0, 4);
        fuente.fila("SIB", TS, "xL23R3", dn(100, 52) + "/SIB-0", "rc7", 100, 100, 52, 0, 6);
        fuente.fila("SIB", TS, "xL23R3", dn(101, 51) + "/SIB-0", "rc7", 101, 101, 51, 0, 6);
        fuente.fila("SIB", TS, "xL23R3", dn(102, 51) + "/SIB-0", "rc7", 102, 102, 51, 0, 4);

        fuente.tabla("LNREL", "timeStamp", "moVersion", "distName", "PLMN_id", "LNBTS_id", "LNREL_id", "cellIndOffNeigh");
        fuente.fila("LNREL", TS, "xL23R3", dn(100, 51) + "/LNREL-1", "rc7", 100, 1, 15);
        fuente.tabla("LTAC", "timeStamp", "moVersion", "distName", "PLMN_id", "MRBTS_id", "LTAC_id", "tacLimitGbrEmergency");
        fuente.fila("LTAC", TS, "xL23R3", "PLMN-PLMN/MRBTS-100/MNL-1/MNLENT-1/TAC-1/LTAC-1", "rc7", 100, 1, 500000);
        fuente.tabla("LNHOIF", "timeStamp", "moVersion", "distName", "PLMN_id", "LNHOIF_id", "a3OffsetRsrpInterFreq");

        ValorEstandar n310General = igual(1, "LNCEL", "n310Qci1", "6");
        ValorEstandar n310SiActFoo = igual(2, "LNCEL", "n310Qci1", "8");
        n310SiActFoo.setCondiciones(Collections.singletonList(new CondicionEstandar(
                OrigenCondicion.PARAMETRO, "LNBTS.actFoo", OperadorComparacion.IGUAL, "1")));
        List<ValorEstandar> reglas = Arrays.asList(n310General, n310SiActFoo,
                igual(3, "LNCEL", "t310Qci1", "200"), igual(4, "SIB", "n310", "4"),
                igual(5, "LNREL", "cellIndOffNeigh", "0"), igual(6, "LTAC-1", "tacLimitGbrEmergency", "450000"));
        valorRepo = mock(ValorEstandarRepository.class);
        when(valorRepo.findAll()).thenReturn(reglas);

        Excepcion sitiosDePrueba = new Excepcion();
        sitiosDePrueba.setIdExcepcion(9L);
        sitiosDePrueba.setAlcance(AlcanceExcepcion.PATRON);
        sitiosDePrueba.setPatronNombre("*_test_*");
        sitiosDePrueba.setParametro("*");
        excepcionRepo = mock(ExcepcionRepository.class);
        when(excepcionRepo.findByActivaTrue()).thenReturn(Collections.singletonList(sitiosDePrueba));

        SegmentoRepository segmentoRepo = mock(SegmentoRepository.class);
        when(segmentoRepo.findAll()).thenAnswer(inv -> new ArrayList<>(segmentos));
        when(segmentoRepo.save(any(Segmento.class))).thenAnswer(inv -> {
            Segmento s = inv.getArgument(0);
            s.setIdSegmento((long) segmentos.size() + 1);
            segmentos.add(s);
            return s;
        });
        ElementoDeRedRepository elementoRepo = mock(ElementoDeRedRepository.class);
        when(elementoRepo.findAll()).thenAnswer(inv -> new ArrayList<>(elementos));
        when(elementoRepo.saveAll(anyList())).thenAnswer(inv -> {
            List<ElementoDeRed> lista = inv.getArgument(0);
            for (ElementoDeRed e : lista) {
                if (e.getIdElemento() == null) {
                    e.setIdElemento((long) elementos.size() + 1);
                    elementos.add(e);
                }
            }
            return lista;
        });
        CorridaRepository corridaRepo = mock(CorridaRepository.class);
        when(corridaRepo.save(any(Corrida.class))).thenAnswer(inv -> {
            Corrida c = inv.getArgument(0);
            if (c.getIdCorrida() == null) {
                c.setIdCorrida((long) corridas.size() + 1);
                corridas.add(c);
            }
            return c;
        });

        repo = new MemoriaCmRepositorio();
        importacion = new CmImportService(repo, valorRepo, segmentoRepo, elementoRepo, archivo -> fuente,
                Paths.get("."), Arrays.asList("LNREL*", "LNADJ*"));
        auditoria = new CmAuditoriaService(repo, valorRepo, excepcionRepo, elementoRepo, corridaRepo);
    }

    private Segmento importarYSegmento(String rc) {
        importacion.importar(repo.crearImportacion("dump.dba"), Paths.get("dump.dba"));
        return segmentos.stream().filter(s -> rc.equals(s.getRc())).findFirst().get();
    }

    @Test
    void importaSoloLoNecesarioYArmaSegmentosYElementos() {
        importarYSegmento("rc7");
        Map<String, Object> imp = repo.importaciones.get(1L);
        assertEquals("OK", imp.get("estado"));
        assertEquals(TS, imp.get("fecha_dump"));
        assertTrue(imp.get("mensaje").toString().contains("LNREL esta excluida"));

        // No se leen las vecindades ni las clases sin reglas.
        assertFalse(fuente.tablasLeidas.contains("LNREL"));
        assertFalse(fuente.tablasLeidas.contains("LNHOIF"));
        assertFalse(fuente.tablasLeidas.contains("LTAC"));
        assertTrue(imp.get("mensaje").toString().contains("LTAC no depende de LNBTS"));
        // Solo los parametros con regla (pMax no tiene) y el de la condicion del ancestro.
        assertEquals(new java.util.HashSet<>(Arrays.asList("n310Qci1", "t310Qci1")),
                repo.valores.get(dn(100, 52)).keySet());
        assertEquals("1", repo.valores.get(dn(102)).get("actFoo"));
        // El SIB no tiene nombre propio: toma el de su celda.
        assertEquals("B_test_51", repo.objetos.get(dn(101, 51) + "/SIB-0").getNombre());

        assertEquals(Arrays.asList("RC7", "RC8"),
                segmentos.stream().map(Segmento::getNombre).collect(Collectors.toList()));
        assertEquals(4, elementos.size());
        assertEquals(dn(100), elementos.get(0).getDistName());
        assertEquals("SITIO_A", elementos.get(0).getNombre());

        // Reimportar no duplica segmentos ni elementos.
        importarYSegmento("rc7");
        assertEquals(2, segmentos.size());
        assertEquals(4, elementos.size());

        // Un dump nuevo sin el sitio 200 (dado de baja): sus objetos desaparecen.
        fuente.filas.get("LNBTS").removeIf(f -> f.get("distName").equals(dn(200)));
        fuente.filas.get("LNCEL").removeIf(f -> f.get("distName").toString().startsWith(dn(200)));
        importarYSegmento("rc7");
        assertFalse(repo.objetos.containsKey(dn(200)));
        assertFalse(repo.objetos.containsKey(dn(200, 51)));
        assertFalse(repo.valores.containsKey(dn(200, 51)));
        assertTrue(repo.objetos.containsKey(dn(100, 51)));
    }

    @Test
    void importaDesdeUnaBasePostgresConNombresEnMinusculaYElMismoResultado() {
        // PostgreSQL con tablas creadas sin comillas: lncel, lnbts_id, n310qci1; booleanos nativos.
        fuente.filas.get("LNBTS").stream().filter(f -> f.get("distName").equals(dn(102)))
                .forEach(f -> f.put("actFoo", Boolean.TRUE));
        BaseCmFicticia base = new BaseCmFicticia(fuente, x -> x.toLowerCase(java.util.Locale.ROOT), "cm");
        importacion.importar(repo.crearImportacion("conexion pg"),
                () -> JdbcCmFuenteFactory.crear(base.conexion(), MotorCm.POSTGRESQL, "CM"));
        assertEquals("OK", repo.importaciones.get(1L).get("estado"), String.valueOf(repo.importaciones.get(1L)));
        assertEquals("1", repo.valores.get(dn(102)).get("actFoo"), "booleano -> 1, como en el dump");
        assertTrue(base.consultas.contains("SELECT \"distname\", \"plmn_id\", \"mrbts_id\", \"lnbts_id\", \"lncel_id\","
                + " \"name\", \"moversion\", \"timestamp\", \"n310qci1\", \"t310qci1\" FROM \"cm\".\"lncel\""),
                base.consultas.toString());
        assertTrue(base.consultas.stream().noneMatch(c -> c.contains("lnrel")));
        assertTrue(base.fetchSizes.stream().allMatch(f -> f == 5000));
        assertFalse(base.autoCommit, "PostgreSQL lee por cursor solo sin autocommit");
        assertTrue(base.cerrada);

        Segmento rc7 = segmentos.stream().filter(x -> "rc7".equals(x.getRc())).findFirst().get();
        Corrida corrida = auditoria.auditar(rc7);
        assertEquals(12, corrida.getComparaciones());
        assertEquals(6, corrida.getConformes());
        assertEquals(3, corrida.getDesvios());
        assertEquals(3, corrida.getExceptuados());
    }

    @Test
    void dumpSqliteConTablasDeMasDe500ColumnasNoUsaElMetadataDelDriver() {
        // sqlite-jdbc arma un SELECT compuesto por columna en getColumns(): con LNBTS (~930 columnas)
        // falla con "too many terms in compound SELECT". Se usa sqlite_master y PRAGMA table_info.
        BaseCmFicticia dump = new BaseCmFicticia(fuente, x -> x, null);
        dump.metadataFalla = true;
        importacion.importar(repo.crearImportacion("20240124_sqlite.dba"),
                () -> JdbcCmFuenteFactory.crear(dump.conexion(), MotorCm.SQLITE, null));
        assertEquals("OK", repo.importaciones.get(1L).get("estado"), String.valueOf(repo.importaciones.get(1L)));
        assertTrue(dump.consultas.contains("PRAGMA table_info(\"LNBTS\")"), dump.consultas.toString());
        assertTrue(dump.consultas.contains("SELECT \"distName\", \"PLMN_id\", \"MRBTS_id\", \"LNBTS_id\", \"LNCEL_id\","
                + " \"name\", \"moVersion\", \"timeStamp\", \"n310Qci1\", \"t310Qci1\" FROM \"LNCEL\""), dump.consultas.toString());
        Segmento rc7 = segmentos.stream().filter(x -> "rc7".equals(x.getRc())).findFirst().get();
        Corrida corrida = auditoria.auditar(rc7);
        assertEquals(12, corrida.getComparaciones());
        assertEquals(3, corrida.getDesvios());
    }

    @Test
    void mysqlYSqlServerCitanYCalificanSegunElMotor() {
        BaseCmFicticia mysql = new BaseCmFicticia(fuente, x -> x, "cm_demo");
        try (JdbcCmFuente f = JdbcCmFuenteFactory.crear(mysql.conexion(), MotorCm.MYSQL, null)) {
            f.recorrer("LNCEL", Arrays.asList("distName", "n310Qci1"), fila -> { });
        }
        assertEquals("SELECT `distName`, `n310Qci1` FROM `cm_demo`.`LNCEL`", mysql.consultas.get(0));
        assertEquals(Integer.valueOf(Integer.MIN_VALUE), mysql.fetchSizes.get(0), "MySQL lee de a una fila");

        BaseCmFicticia sqlserver = new BaseCmFicticia(fuente, x -> x, "dbo");
        List<Map<String, Object>> filas = new ArrayList<>();
        try (JdbcCmFuente f = JdbcCmFuenteFactory.crear(sqlserver.conexion(), MotorCm.SQLSERVER, "")) {
            assertTrue(f.tablas().contains("SIB"));
            f.recorrer("lncel", Arrays.asList("DISTNAME", "N310QCI1"), filas::add);
        }
        assertEquals("SELECT [distName], [n310Qci1] FROM [dbo].[LNCEL]", sqlserver.consultas.get(0));
        assertEquals(dn(100, 51), filas.get(0).get("DISTNAME"), "las filas vuelven con los nombres pedidos");
        assertEquals("[a]]b]", MotorCm.SQLSERVER.citar("a]b"));
        assertEquals("`a``b`", MotorCm.MYSQL.citar("a`b"));
    }

    @Test
    void cadaCorridaGuardaSoloDesviosYExceptuadosYBajaAlCorregir() {
        Segmento rc7 = importarYSegmento("rc7");

        Corrida primera = auditoria.auditar(rc7);
        assertEquals(12, primera.getComparaciones());
        assertEquals(6, primera.getConformes());
        assertEquals(3, primera.getDesvios());
        assertEquals(3, primera.getExceptuados());
        assertEquals(8, primera.getObjetosEvaluados());
        List<Auditoria> filas = repo.auditorias.stream()
                .filter(a -> primera.getIdCorrida().equals(a.getIdCorrida())).collect(Collectors.toList());
        assertEquals(6, filas.size(), "solo desvios y exceptuados");

        Auditoria n310 = filas.stream().filter(a -> a.getDistName().equals(dn(100, 52))
                && a.getParametro().equals("n310Qci1")).findFirst().get();
        assertEquals("desvio", n310.getResultado());
        assertEquals("8", n310.getValorVigente());
        assertEquals("6", n310.getValorObjetivo());
        assertEquals("A_52", n310.getNombreObjeto());
        // La condicion sobre el LNBTS (actFoo = 1) hace que D_51 sea conforme con 8.
        assertTrue(filas.stream().noneMatch(a -> a.getDistName().startsWith(dn(102))));
        // Los objetos del sitio de prueba quedan exceptuados (tambien su SIB).
        assertEquals(3, filas.stream().filter(a -> a.getDistName().startsWith(dn(101))
                && a.getResultado().equals("exceptuado")).count());

        // Se corrige la red (se aplico el plan): la corrida siguiente ya no tiene desvios.
        repo.cambiarValor(dn(100, 52), "n310Qci1", "6");
        repo.cambiarValor(dn(100, 52), "t310Qci1", "200");
        repo.cambiarValor(dn(100, 52) + "/SIB-0", "n310", "4");
        Corrida segunda = auditoria.auditar(rc7);
        assertEquals(0, segunda.getDesvios());
        assertEquals(9, segunda.getConformes());
        assertEquals(3, repo.auditorias.stream()
                .filter(a -> segunda.getIdCorrida().equals(a.getIdCorrida())).count());
    }

    @Test
    void elPlanRamlUsaElDistNameRealDeCadaObjeto() {
        Segmento rc7 = importarYSegmento("rc7");
        Corrida corrida = auditoria.auditar(rc7);
        List<Desvio> desvios = repo.auditorias.stream()
                .filter(a -> corrida.getIdCorrida().equals(a.getIdCorrida()) && a.getResultado().equals("desvio"))
                .map(a -> com.neipa.reconfiguracion.DesvioServiceAcceso.deCm(a, rc7.getIdSegmento()))
                .collect(Collectors.toList());
        GeneradorRaml generador = new GeneradorRaml();
        LocalDateTime fecha = LocalDateTime.of(2026, 10, 14, 10, 0);
        String plan = generador.generar(desvios, "RC7", GeneradorRaml.Tipo.PLAN, fecha);
        String backup = generador.generar(desvios, "RC7", GeneradorRaml.Tipo.BACKUP, fecha);

        assertTrue(plan.contains("<managedObject class=\"LNCEL\" operation=\"update\" distName=\"" + dn(100, 52) + "\" >"));
        assertTrue(plan.contains("<managedObject class=\"SIB\" operation=\"update\" distName=\"" + dn(100, 52) + "/SIB-0\" >"));
        assertTrue(plan.contains("<p name=\"n310Qci1\">6</p>") && plan.contains("<p name=\"t310Qci1\">200</p>"));
        assertTrue(backup.contains("<p name=\"n310Qci1\">8</p>") && backup.contains("<p name=\"t310Qci1\">1000</p>"));
        assertEquals(2, plan.split("<managedObject").length - 1, "los dos parametros de la celda van en un solo objeto");
        assertFalse(plan.contains("_test_"));
    }

    @Test
    void utilidadesDeDistName() {
        assertEquals("SIB", CmNombres.claseDe(dn(1, 2) + "/SIB-0"));
        assertEquals(dn(1, 2), CmNombres.ancestro(dn(1, 2) + "/SIB-0", "LNCEL"));
        assertEquals(dn(1), CmNombres.ancestro(dn(1, 2), "LNBTS"));
        assertNull(CmNombres.ancestro(dn(1), "LNCEL"));
        assertEquals("CADPR", CmNombres.claseDeObjetoGestionado("LNCEL-1/CADPR-1"));
        assertEquals("LNBTS", CmNombres.claseDeObjetoGestionado(null));
        assertTrue(CmNombres.excluida("LNADJL_BCPLMNIDLIST", Arrays.asList("LNREL*", "LNADJ*")));
        assertFalse(CmNombres.excluida("LNCEL", Arrays.asList("LNREL*", "LNADJ*")));
        assertEquals("8", CmImportService.texto(8.0));
        assertEquals("-3.5", CmImportService.texto(-3.5));
    }

    @Test
    void soloSePuedenImportarArchivosDeLaCarpeta() {
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> importacion.resolver("../secreto.dba"));
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> importacion.resolver("no-existe.dba"));
    }
}
