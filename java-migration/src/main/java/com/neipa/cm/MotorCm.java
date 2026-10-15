package com.neipa.cm;

import java.util.Locale;

/**
 * Motores de base de datos desde los que se puede leer el CM de NSN/Nokia con
 * la misma estructura que el dump SQLite de NetAct (una tabla por clase,
 * columnas distName, PLMN_id, MRBTS_id, LNBTS_id, ... y una por parametro).
 */
public enum MotorCm {

    SQLITE("jdbc:sqlite:", "\"", "\"", ""),
    MYSQL("jdbc:mysql:", "`", "`", "jdbc:mysql://servidor:3306/base_cm"),
    POSTGRESQL("jdbc:postgresql:", "\"", "\"", "jdbc:postgresql://servidor:5432/base_cm"),
    ORACLE("jdbc:oracle:", "\"", "\"", "jdbc:oracle:thin:@//servidor:1521/SERVICIO"),
    SQLSERVER("jdbc:sqlserver:", "[", "]", "jdbc:sqlserver://servidor:1433;databaseName=base_cm;encrypt=false");

    private final String prefijoUrl;
    private final String abre;
    private final String cierra;
    private final String urlEjemplo;

    MotorCm(String prefijoUrl, String abre, String cierra, String urlEjemplo) {
        this.prefijoUrl = prefijoUrl;
        this.abre = abre;
        this.cierra = cierra;
        this.urlEjemplo = urlEjemplo;
    }

    /** Identificador entre los delimitadores del motor (escapando el delimitador de cierre). */
    public String citar(String identificador) {
        return abre + identificador.replace(cierra, cierra + cierra) + cierra;
    }

    public boolean aceptaUrl(String url) {
        return url != null && url.toLowerCase(Locale.ROOT).startsWith(prefijoUrl);
    }

    public String getPrefijoUrl() {
        return prefijoUrl;
    }

    public String getUrlEjemplo() {
        return urlEjemplo;
    }

}
