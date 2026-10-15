package com.neipa.cm;

import java.util.Collection;
import java.util.Locale;

import com.neipa.validation.PatronNombre;

/** Utilidades sobre distName y clases del CM de NetAct. */
public final class CmNombres {

    public static final String LNBTS = "LNBTS";
    public static final String LNCEL = "LNCEL";

    private CmNombres() {
    }

    /** "PLMN-PLMN/MRBTS-1/LNBTS-1/LNCEL-51/SIB-0" -> "SIB". */
    public static String claseDe(String distName) {
        String ultimo = distName.substring(distName.lastIndexOf('/') + 1);
        int guion = ultimo.lastIndexOf('-');
        return guion > 0 ? ultimo.substring(0, guion) : ultimo;
    }

    /**
     * distName del ancestro de una clase dada, o null si no lo tiene.
     * ancestro(".../LNBTS-1/LNCEL-51/SIB-0", "LNCEL") -> ".../LNBTS-1/LNCEL-51".
     */
    public static String ancestro(String distName, String clase) {
        String buscado = "/" + clase + "-";
        int i = distName.indexOf(buscado);
        if (i < 0) {
            return null;
        }
        int fin = distName.indexOf('/', i + buscado.length());
        return fin < 0 ? distName : distName.substring(0, fin);
    }

    /**
     * Clase del CM a la que apunta el objeto gestionado de una regla:
     * "LNCEL" -> LNCEL, "LNCEL-1/CADPR-1" -> CADPR, vacio -> LNBTS.
     */
    public static String claseDeObjetoGestionado(String objetoGestionado) {
        if (objetoGestionado == null || objetoGestionado.trim().isEmpty()) {
            return LNBTS;
        }
        String o = objetoGestionado.trim();
        String ultimo = o.substring(o.lastIndexOf('/') + 1);
        int guion = ultimo.indexOf('-');
        return (guion > 0 ? ultimo.substring(0, guion) : ultimo).toUpperCase(Locale.ROOT);
    }

    /** True si la clase esta excluida (patrones como "LNREL*", "LNADJ*"). */
    public static boolean excluida(String clase, Collection<String> patronesExcluidos) {
        for (String patron : patronesExcluidos) {
            if (PatronNombre.coincide(clase, patron)) {
                return true;
            }
        }
        return false;
    }
}
