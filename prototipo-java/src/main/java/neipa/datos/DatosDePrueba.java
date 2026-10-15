package neipa.datos;

import java.time.LocalDate;

import neipa.excepciones.NeipaException;
import neipa.modelo.Antena;
import neipa.modelo.Celda;
import neipa.modelo.CondicionPorParametro;
import neipa.modelo.ElementoDeRed;
import neipa.modelo.Excepcion;
import neipa.modelo.Fabricante;
import neipa.modelo.Parametro;
import neipa.modelo.ParametroEnumerado;
import neipa.modelo.ParametroNumerico;
import neipa.modelo.Segmento;
import neipa.modelo.Sitio;
import neipa.modelo.ValorEstandar;

/**
 * Carga inicial del prototipo. Retoma los datos de prueba de la base de
 * datos de la segunda entrega (fabricantes, segmentos, Site_CBA_014,
 * Site_CBA_022 y Site_RUR_102 con sus valores) y agrega mas elementos para
 * poder mostrar la ordenacion, la busqueda y los distintos resultados.
 */
public final class DatosDePrueba {

    private DatosDePrueba() {
        // Clase utilitaria: no se crean instancias.
    }

    public static void cargar(RepositorioEnMemoria repo) throws NeipaException {
        // Fabricantes y su plataforma de gestion
        Fabricante ericsson = new Fabricante("Ericsson", "ENM");
        Fabricante huawei = new Fabricante("Huawei", "U2020");
        Fabricante nsn = new Fabricante("NSN", "NetAct");
        repo.agregarFabricante(ericsson);
        repo.agregarFabricante(huawei);
        repo.agregarFabricante(nsn);

        // Segmentos
        Segmento urbano = new Segmento("Urbano alta densidad", "tecnologia=5G, zona=urbana");
        Segmento rural = new Segmento("Rural cobertura extendida", "tecnologia=4G, zona=rural");
        repo.agregarSegmento(urbano);
        repo.agregarSegmento(rural);
        Segmento laboratorio = new Segmento("Laboratorio 4G",
                "tecnologia=4G, sitios de laboratorio y de prueba");
        repo.agregarSegmento(laboratorio);

        // Catalogo de parametros (rango permitido segun el tipo de dato)
        Parametro potenciaEricsson = new ParametroNumerico(ericsson, "EUtranCellFDD",
                "PotenciaTX_dBm", "Potencia de transmision", "dBm", 30, 46, 1, "43");
        Parametro potenciaHuawei = new ParametroNumerico(huawei, "Cell",
                "PotenciaTX_dBm", "Potencia de transmision", "dBm", 30, 46, 1, "43");
        Parametro a3Offset = new ParametroNumerico(nsn, "CADPR",
                "a3Offset", "A3 offset", "dB", -15, 15, 0.5, null);
        Parametro estadoAdmin = new ParametroEnumerado(ericsson, "EUtranCellFDD",
                "administrativeState", "Estado administrativo de la celda",
                new String[] {"LOCKED", "UNLOCKED", "SHUTTING_DOWN"}, null);
        repo.agregarParametro(potenciaEricsson);
        repo.agregarParametro(potenciaHuawei);
        repo.agregarParametro(a3Offset);
        repo.agregarParametro(estadoAdmin);
        Parametro pMax = new ParametroNumerico(nsn, "LNCEL",
                "pMax", "Potencia maxima de la celda", "dBm", 0, 46, 1, "43");
        repo.agregarParametro(pMax);

        // Valores estandar por segmento
        repo.agregarEstandar(new ValorEstandar(potenciaEricsson, urbano, "46"));
        repo.agregarEstandar(new ValorEstandar(potenciaHuawei, urbano, 43, 46));
        repo.agregarEstandar(new ValorEstandar(a3Offset, rural, "-6"));
        repo.agregarEstandar(new ValorEstandar(estadoAdmin, urbano, "UNLOCKED"));

        // Laboratorio: un estandar general por segmento y otro con una
        // condicion sobre OTRO parametro (si pMax esta entre 40 y 46 dBm, el
        // a3Offset tiene que ser -3). Se aplica el mas especifico.
        repo.agregarEstandar(new ValorEstandar(a3Offset, laboratorio, "-6"));
        repo.agregarEstandar(new ValorEstandar(a3Offset, laboratorio, "-3")
                .conCondicion(new CondicionPorParametro(pMax, 40, 46)));
        repo.agregarEstandar(new ValorEstandar(pMax, laboratorio, 30, 43));

        // Elementos de red (cargados sin orden, a proposito)
        ElementoDeRed siteCba014 = new Sitio("Site_CBA_014", "5G", ericsson, urbano, 3);
        siteCba014.registrarValorVigente(potenciaEricsson, "44");
        siteCba014.registrarValorVigente(estadoAdmin, "UNLOCKED");

        ElementoDeRed siteRur102 = new Sitio("Site_RUR_102", "4G", nsn, rural, 3);
        siteRur102.registrarValorVigente(a3Offset, "-4");

        ElementoDeRed siteCba022 = new Sitio("Site_CBA_022", "5G", huawei, urbano, 3);
        siteCba022.registrarValorVigente(potenciaHuawei, "45");

        ElementoDeRed cellCba031B = new Celda("Cell_CBA_031_B", "5G", ericsson, urbano, 3500);
        cellCba031B.registrarValorVigente(potenciaEricsson, "48");
        cellCba031B.registrarValorVigente(estadoAdmin, "LOCKED");

        ElementoDeRed cellRur087C = new Celda("Cell_RUR_087_C", "4G", nsn, rural, 700);
        cellRur087C.registrarValorVigente(a3Offset, "-3.5");

        ElementoDeRed antCba0221 = new Antena("Ant_CBA_022_1", "5G", huawei, urbano, 32.5);
        antCba0221.registrarValorVigente(potenciaHuawei, "41");

        ElementoDeRed cellCba014A = new Celda("Cell_CBA_014_A", "5G", ericsson, urbano, 3500);
        cellCba014A.registrarValorVigente(potenciaEricsson, "46");
        cellCba014A.registrarValorVigente(estadoAdmin, "UNLOCKED");

        ElementoDeRed cellRur102A = new Celda("Cell_RUR_102_A", "4G", nsn, rural, 700);
        cellRur102A.registrarValorVigente(a3Offset, "-6");

        ElementoDeRed cellRur102B = new Celda("Cell_RUR_102_B", "4G", nsn, rural, 700);
        cellRur102B.registrarValorVigente(a3Offset, "-6");

        ElementoDeRed labSite001 = new Celda("Lab_SITE_001_A", "4G", nsn, laboratorio, 700);
        labSite001.registrarValorVigente(a3Offset, "-6");
        labSite001.registrarValorVigente(pMax, "30");

        ElementoDeRed labSite002 = new Celda("Lab_SITE_002_A", "4G", nsn, laboratorio, 700);
        labSite002.registrarValorVigente(a3Offset, "-6");
        labSite002.registrarValorVigente(pMax, "43");

        ElementoDeRed labSite003 = new Celda("Lab_SITE_003_A", "4G", nsn, laboratorio, 700);
        labSite003.registrarValorVigente(a3Offset, "-3");
        labSite003.registrarValorVigente(pMax, "46");

        ElementoDeRed labTest004 = new Celda("Lab_test_004_A", "4G", nsn, laboratorio, 700);
        labTest004.registrarValorVigente(a3Offset, "-1");
        labTest004.registrarValorVigente(pMax, "30");

        repo.agregarElemento(siteCba014);
        repo.agregarElemento(siteRur102);
        repo.agregarElemento(siteCba022);
        repo.agregarElemento(cellCba031B);
        repo.agregarElemento(cellRur087C);
        repo.agregarElemento(antCba0221);
        repo.agregarElemento(cellCba014A);
        repo.agregarElemento(cellRur102A);
        repo.agregarElemento(cellRur102B);
        repo.agregarElemento(labSite001);
        repo.agregarElemento(labSite002);
        repo.agregarElemento(labSite003);
        repo.agregarElemento(labTest004);

        // Excepcion vigente: se calcula respecto de la fecha de ejecucion
        LocalDate hoy = LocalDate.now();
        repo.agregarExcepcion(Excepcion.porElemento(siteRur102, a3Offset,
                "Sitio de borde con ajuste de handover propio",
                hoy.minusDays(30), hoy.plusDays(90)));
        // Excepcion por patron: los sitios de prueba ("_test_" en el nombre)
        // no se reconfiguran, en ningun parametro.
        repo.agregarExcepcion(Excepcion.porPatron("*_test_*", null,
                "Sitios de prueba: no se reconfiguran", hoy, hoy.plusDays(365)));
    }
}
