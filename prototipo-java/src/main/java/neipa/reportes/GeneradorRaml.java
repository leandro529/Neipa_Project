package neipa.reportes;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import neipa.estructuras.ListaEnlazada;
import neipa.modelo.Auditoria;
import neipa.modelo.ElementoDeRed;

/**
 * Genera el XML de reconfiguracion de los desvios en formato RAML 2.0 (los
 * planes de cambios de Nokia NetAct), con el mismo formato que los planes
 * que ya usa el area. Implementa la interfaz Reporte: el mismo metodo
 * generar() produce el PLAN (valor objetivo, corrige el desvio) o el BACKUP
 * (valor vigente antes del cambio, para hacer rollback), segun el tipo con
 * que se creo el objeto.
 *
 * Solo entran los desvios de elementos gestionados por NetAct (fabricante
 * NSN): los de otros fabricantes usan otro formato y quedan como pendientes.
 */
public class GeneradorRaml implements Reporte {

    public enum Tipo {
        PLAN,
        BACKUP
    }

    private static final String FIN_LINEA = "\r\n";
    private static final String OSS_NETACT = "NetAct";

    private final Tipo tipo;
    private final String grupo;
    private final LocalDateTime fecha;

    public GeneradorRaml(Tipo tipo, String nombreSegmento, LocalDateTime fecha) {
        if (tipo == null || fecha == null) {
            throw new IllegalArgumentException("Se necesita el tipo de archivo y la fecha.");
        }
        this.tipo = tipo;
        this.grupo = normalizar(nombreSegmento);
        this.fecha = fecha;
    }

    @Override
    public String getTitulo() {
        return tipo == Tipo.PLAN ? "PLAN DE RECONFIGURACION (RAML 2.0)"
                : "BACKUP PARA ROLLBACK (RAML 2.0)";
    }

    /** Recibe los desvios de la ultima auditoria y devuelve el XML. */
    @Override
    public String generar(ListaEnlazada<Auditoria> desvios) {
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>").append(FIN_LINEA);
        xml.append("<raml xmlns=\"raml20.xsd\" version=\"2.0\">").append(FIN_LINEA);
        xml.append(" <cmData type=\"plan\" scope=\"changes\" name=\"auditorias_estrategia_")
                .append(fecha.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))).append('_')
                .append(grupo).append("_online_\">").append(FIN_LINEA);
        xml.append("     <header>").append(FIN_LINEA);
        xml.append("         <log user=\"neipa\" action=\"created\" dateTime=\"")
                .append(fecha.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")))
                .append("\" appInfo=\"NEIPA\" >")
                .append(tipo == Tipo.PLAN ? "Correccion de desvios" : "Backup para rollback")
                .append("</log> ").append(FIN_LINEA);
        xml.append("     </header>").append(FIN_LINEA);
        for (Auditoria desvio : desvios) {
            if (!esReconfigurable(desvio)) {
                continue;
            }
            String clase = desvio.getParametro().getObjetoGestionado();
            String valor = tipo == Tipo.PLAN
                    ? desvio.getValorEstandar().valorObjetivo(desvio.getValorVigente())
                    : desvio.getValorVigente();
            xml.append("\t<managedObject class=\"").append(clase)
                    .append("\" operation=\"update\" distName=\"")
                    .append(distName(desvio.getElementoDeRed(), clase)).append("\" >")
                    .append(FIN_LINEA).append(FIN_LINEA);
            xml.append("\t\t<p name=\"").append(desvio.getParametro().getNombre()).append('"');
            if (valor == null || valor.trim().isEmpty()) {
                xml.append("/>");
            } else {
                xml.append('>').append(escapar(valor.trim())).append("</p>");
            }
            xml.append(FIN_LINEA).append(FIN_LINEA);
            xml.append("\t</managedObject>").append(FIN_LINEA);
        }
        xml.append(FIN_LINEA).append(" </cmData>").append(FIN_LINEA);
        xml.append("</raml>").append(FIN_LINEA);
        return xml.toString();
    }

    /** Solo los elementos gestionados por NetAct van al plan RAML. */
    public static boolean esReconfigurable(Auditoria desvio) {
        return OSS_NETACT.equalsIgnoreCase(desvio.getElementoDeRed().getFabricante().getTipoOss());
    }

    /** distName del objeto: el del elemento, mas el objeto del parametro si es otro. */
    static String distName(ElementoDeRed elemento, String clase) {
        String base = elemento.getDistName();
        String ultimo = base.substring(base.lastIndexOf('/') + 1);
        if (clase == null || ultimo.startsWith(clase + "-")) {
            return base;
        }
        return base + "/" + clase + "-1";
    }

    /** Nombre de archivo como los del area: SEGMENTO_online_MM-dd-yyyy[_backup].xml */
    public String nombreDeArchivo() {
        return grupo + "_online_" + fecha.format(DateTimeFormatter.ofPattern("MM-dd-yyyy"))
                + (tipo == Tipo.BACKUP ? "_backup" : "") + ".xml";
    }

    /** "Rural cobertura extendida" -> "RURAL_COBERTURA_EXTENDIDA". */
    static String normalizar(String nombre) {
        StringBuilder texto = new StringBuilder();
        boolean separador = false;
        String origen = nombre == null ? "" : nombre.trim().toUpperCase();
        for (int i = 0; i < origen.length(); i++) {
            char c = origen.charAt(i);
            if ((c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9')) {
                if (separador && texto.length() > 0) {
                    texto.append('_');
                }
                texto.append(c);
                separador = false;
            } else {
                separador = true;
            }
        }
        return texto.length() == 0 ? "SIN_SEGMENTO" : texto.toString();
    }

    private static String escapar(String texto) {
        return texto.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
