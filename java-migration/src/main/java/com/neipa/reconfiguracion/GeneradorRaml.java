package com.neipa.reconfiguracion;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.springframework.stereotype.Component;

/**
 * Genera los planes de reconfiguracion de los desvios en formato RAML 2.0
 * (planes de CM de Nokia NetAct), con el mismo formato que los planes que ya
 * usa el area (ej. NOROCCIDENTE_RC2_online_12-26-2023.xml):
 *
 * <pre>
 * &lt;?xml version="1.0" encoding="UTF-8"?&gt;
 * &lt;raml xmlns="raml20.xsd" version="2.0"&gt;
 *  &lt;cmData type="plan" scope="changes" name="auditorias_estrategia_2026-10-13_LABORATORIO_4G_online_"&gt;
 *      &lt;header&gt;
 *          &lt;log user="neipa" action="created" dateTime="..." appInfo="NEIPA" &gt;...&lt;/log&gt;
 *      &lt;/header&gt;
 *     &lt;managedObject class="LNCEL" operation="update" distName="PLMN-PLMN/MRBTS-9002/LNBTS-9002/LNCEL-1" &gt;
 *
 *         &lt;p name="AnguloTilt_grados"&gt;12&lt;/p&gt;
 *
 *     &lt;/managedObject&gt;
 *  &lt;/cmData&gt;
 * &lt;/raml&gt;
 * </pre>
 *
 * Siempre se generan dos archivos con los mismos objetos y parametros:
 * <ul>
 *   <li><b>plan</b>: el valor objetivo (lo que corrige el desvio).</li>
 *   <li><b>backup</b>: el valor vigente antes del cambio, para poder hacer
 *       rollback importando ese plan. Si el valor vigente estaba vacio se
 *       escribe {@code <p name="x"/>}, igual que en los backups del area.</li>
 * </ul>
 * Los desvios sin valor objetivo no entran en ninguno de los dos (no se
 * puede inventar el valor): se listan aparte como pendientes.
 */
@Component
public class GeneradorRaml {

    public enum Tipo {
        PLAN, BACKUP
    }

    private static final String CRLF = "\r\n";
    private static final DateTimeFormatter FORMATO_FECHA_LOG = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
    private static final DateTimeFormatter FORMATO_FECHA_PLAN = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter FORMATO_FECHA_ARCHIVO = DateTimeFormatter.ofPattern("MM-dd-yyyy");

    /**
     * @param desvios  desvios vigentes (solo se usan los reconfigurables)
     * @param grupo    nombre del grupo (segmento), ej "LABORATORIO_4G"
     */
    public String generar(List<Desvio> desvios, String grupo, Tipo tipo, LocalDateTime fecha) {
        Function<Desvio, String> valor = tipo == Tipo.PLAN ? Desvio::getValorObjetivo : Desvio::getValorVigente;

        Map<String, List<Desvio>> porObjeto = new LinkedHashMap<>();
        int cantidad = 0;
        for (Desvio d : desvios) {
            if (d.isReconfigurable()) {
                porObjeto.computeIfAbsent(d.getDistName(), k -> new java.util.ArrayList<>()).add(d);
                cantidad++;
            }
        }

        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>").append(CRLF);
        xml.append("<raml xmlns=\"raml20.xsd\" version=\"2.0\">").append(CRLF);
        xml.append(" <cmData type=\"plan\" scope=\"changes\" name=\"")
                .append(escapar(nombrePlan(grupo, fecha))).append("\">").append(CRLF);
        xml.append("     <header>").append(CRLF);
        xml.append("         <log user=\"neipa\" action=\"created\" dateTime=\"")
                .append(fecha.format(FORMATO_FECHA_LOG)).append("\" appInfo=\"NEIPA\" >")
                .append(tipo == Tipo.PLAN ? "Correccion de desvios" : "Backup para rollback")
                .append(" (").append(cantidad).append(" parametros en ").append(porObjeto.size())
                .append(" objetos)</log> ").append(CRLF);
        xml.append("     </header>").append(CRLF);

        for (Map.Entry<String, List<Desvio>> objeto : porObjeto.entrySet()) {
            Desvio primero = objeto.getValue().get(0);
            xml.append("\t<managedObject class=\"").append(escapar(primero.getClaseMo()))
                    .append("\" operation=\"update\" distName=\"").append(escapar(objeto.getKey()))
                    .append("\" >").append(CRLF).append(CRLF);
            for (Desvio d : objeto.getValue()) {
                String v = valor.apply(d);
                xml.append("\t\t<p name=\"").append(escapar(d.getParametro())).append("\"");
                if (v == null || v.trim().isEmpty()) {
                    xml.append("/>");
                } else {
                    xml.append(">").append(escapar(v.trim())).append("</p>");
                }
                xml.append(CRLF).append(CRLF);
            }
            xml.append("\t</managedObject>").append(CRLF);
        }

        xml.append(CRLF).append(" </cmData>").append(CRLF);
        xml.append("</raml>").append(CRLF);
        return xml.toString();
    }

    /** Nombre del plan, igual que los del area: auditorias_estrategia_FECHA_GRUPO_online_ */
    public static String nombrePlan(String grupo, LocalDateTime fecha) {
        return "auditorias_estrategia_" + fecha.format(FORMATO_FECHA_PLAN) + "_" + grupo + "_online_";
    }

    /** Nombre del archivo: GRUPO_online_MM-dd-yyyy.xml / ..._backup.xml */
    public static String nombreArchivo(String grupo, Tipo tipo, LocalDateTime fecha) {
        return grupo + "_online_" + fecha.format(FORMATO_FECHA_ARCHIVO)
                + (tipo == Tipo.BACKUP ? "_backup" : "") + ".xml";
    }

    /** "Laboratorio 4G" -> "LABORATORIO_4G" (apto para nombre de archivo y de plan). */
    public static String normalizarGrupo(String nombre) {
        String limpio = java.text.Normalizer.normalize(nombre == null ? "" : nombre, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replaceAll("[^A-Za-z0-9]+", "_")
                .replaceAll("^_+|_+$", "")
                .toUpperCase(java.util.Locale.ROOT);
        return limpio.isEmpty() ? "SIN_SEGMENTO" : limpio;
    }

    private static String escapar(String texto) {
        return texto.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&apos;");
    }
}
