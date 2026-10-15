package com.neipa.reconfiguracion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.neipa.domain.Auditoria;
import com.neipa.domain.ElementoDeRed;
import com.neipa.domain.ResultadoAuditoria;
import com.neipa.domain.ValorEstandar;

class GeneradorRamlTest {

    private static final LocalDateTime FECHA = LocalDateTime.of(2026, 10, 13, 22, 30, 5);

    private static Auditoria auditoria(long id, long idElemento, String parametro, String vigente,
                                       ResultadoAuditoria resultado, Long idRegla, String objetivo) {
        Auditoria a = new Auditoria(idElemento, parametro, vigente, "x", resultado, FECHA);
        a.setIdAuditoria(id);
        a.setIdValorEstandar(idRegla);
        a.setValorObjetivo(objetivo);
        return a;
    }

    private static List<Desvio> desvios() {
        ElementoDeRed lab1 = new ElementoDeRed("Sitio-Lab-001", "4G", "NSN", 3L);
        lab1.setIdElemento(8L);
        lab1.setDistName("PLMN-PLMN/MRBTS-9001/LNBTS-9001");
        ElementoDeRed lab2 = new ElementoDeRed("Sitio-Lab-002", "4G", "NSN", 3L);
        lab2.setIdElemento(9L);
        Map<Long, ElementoDeRed> elementos = new HashMap<>();
        elementos.put(8L, lab1);
        elementos.put(9L, lab2);

        ValorEstandar srvcc = new ValorEstandar();
        srvcc.setIdValorEstandar(100L);
        ValorEstandar tilt = new ValorEstandar();
        tilt.setIdValorEstandar(101L);
        tilt.setObjetoGestionado("LNCEL-1");
        ValorEstandar tac = new ValorEstandar();
        tac.setIdValorEstandar(4L);
        tac.setObjetoGestionado("LTAC-1");
        Map<Long, ValorEstandar> reglas = new HashMap<>();
        reglas.put(100L, srvcc);
        reglas.put(101L, tilt);
        reglas.put(4L, tac);

        List<Auditoria> auditorias = Arrays.asList(
                // Lab-001 srvcc: primero DESVIO, sigue en DESVIO
                auditoria(1, 8, "srvccDelayTimer", "4", ResultadoAuditoria.DESVIO, 100L, "5"),
                auditoria(5, 8, "srvccDelayTimer", "4", ResultadoAuditoria.DESVIO, 100L, "5"),
                // Lab-001 tac: sin valor objetivo -> pendiente
                auditoria(6, 8, "tacLimitGbrEmergency", "500000", ResultadoAuditoria.DESVIO, 4L, null),
                // Lab-002 tilt: DESVIO y valor vigente vacio (backup con <p/>)
                auditoria(7, 9, "AnguloTilt_grados", "", ResultadoAuditoria.DESVIO, 101L, "12"),
                // Lab-002 srvcc: estuvo en DESVIO pero ya se corrigio -> no sale
                auditoria(2, 9, "srvccDelayTimer", "4", ResultadoAuditoria.DESVIO, 100L, "5"),
                auditoria(8, 9, "srvccDelayTimer", "5", ResultadoAuditoria.CONFORME, 100L, null));
        return DesvioService.armar(auditorias, elementos, reglas, null);
    }

    private static Document parsear(String xml) throws Exception {
        return DocumentBuilderFactory.newInstance().newDocumentBuilder()
                .parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void soloQuedanLosDesviosVigentesConElDistNameDelObjeto() {
        List<Desvio> desvios = desvios();
        assertEquals(3, desvios.size());
        Desvio srvcc = desvios.stream().filter(d -> d.getParametro().equals("srvccDelayTimer")).findFirst().get();
        assertEquals("PLMN-PLMN/MRBTS-9001/LNBTS-9001", srvcc.getDistName());
        assertEquals("LNBTS", srvcc.getClaseMo());
        Desvio tilt = desvios.stream().filter(d -> d.getParametro().equals("AnguloTilt_grados")).findFirst().get();
        assertEquals("PLMN-PLMN/MRBTS-9/LNBTS-9/LNCEL-1", tilt.getDistName());
        assertEquals("LNCEL", tilt.getClaseMo());
    }

    @Test
    void elPlanYElBackupTienenLosMismosObjetosConValorObjetivoYVigente() throws Exception {
        GeneradorRaml generador = new GeneradorRaml();
        String plan = generador.generar(desvios(), "LABORATORIO_4G", GeneradorRaml.Tipo.PLAN, FECHA);
        String backup = generador.generar(desvios(), "LABORATORIO_4G", GeneradorRaml.Tipo.BACKUP, FECHA);

        assertTrue(plan.startsWith("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n<raml xmlns=\"raml20.xsd\" version=\"2.0\">"));
        assertTrue(plan.contains("name=\"auditorias_estrategia_2026-10-13_LABORATORIO_4G_online_\""));
        assertFalse(plan.contains("tacLimitGbrEmergency"), "sin valor objetivo no va al plan");

        Document dPlan = parsear(plan);
        Document dBackup = parsear(backup);
        Element cmData = (Element) dPlan.getElementsByTagName("cmData").item(0);
        assertEquals("plan", cmData.getAttribute("type"));
        assertEquals("changes", cmData.getAttribute("scope"));

        NodeList objetosPlan = dPlan.getElementsByTagName("managedObject");
        NodeList objetosBackup = dBackup.getElementsByTagName("managedObject");
        assertEquals(2, objetosPlan.getLength());
        assertEquals(2, objetosBackup.getLength());
        for (int i = 0; i < objetosPlan.getLength(); i++) {
            Element mp = (Element) objetosPlan.item(i);
            Element mb = (Element) objetosBackup.item(i);
            assertEquals(mp.getAttribute("distName"), mb.getAttribute("distName"));
            assertEquals("update", mp.getAttribute("operation"));
        }

        Map<String, String> valoresPlan = parametros(dPlan);
        Map<String, String> valoresBackup = parametros(dBackup);
        assertEquals("5", valoresPlan.get("srvccDelayTimer"));
        assertEquals("4", valoresBackup.get("srvccDelayTimer"));
        assertEquals("12", valoresPlan.get("AnguloTilt_grados"));
        assertEquals("", valoresBackup.get("AnguloTilt_grados"));
        assertTrue(backup.contains("<p name=\"AnguloTilt_grados\"/>"));
    }

    private static Map<String, String> parametros(Document d) {
        Map<String, String> m = new HashMap<>();
        NodeList ps = d.getElementsByTagName("p");
        for (int i = 0; i < ps.getLength(); i++) {
            Element p = (Element) ps.item(i);
            m.put(p.getAttribute("name"), p.getTextContent());
        }
        return m;
    }

    @Test
    void nombresDeArchivoComoLosDelArea() {
        assertEquals("LABORATORIO_4G", GeneradorRaml.normalizarGrupo("Laboratorio 4G"));
        assertEquals("ZONA_NORTE_4G", GeneradorRaml.normalizarGrupo("Zona Norte (4G)"));
        assertEquals("LABORATORIO_4G_online_10-13-2026.xml",
                GeneradorRaml.nombreArchivo("LABORATORIO_4G", GeneradorRaml.Tipo.PLAN, FECHA));
        assertEquals("LABORATORIO_4G_online_10-13-2026_backup.xml",
                GeneradorRaml.nombreArchivo("LABORATORIO_4G", GeneradorRaml.Tipo.BACKUP, FECHA));
    }
}
