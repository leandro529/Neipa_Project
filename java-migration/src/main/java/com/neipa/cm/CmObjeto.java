package com.neipa.cm;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Un objeto gestionado del CM de NetAct (una fila de una tabla del dump):
 * su distName, la clase (LNBTS, LNCEL, SIB, ...), el RC (PLMN_id), los ids
 * de su jerarquia, el nombre (propio o del ancestro mas cercano que lo tenga)
 * y, cuando hace falta, los valores de los parametros que se auditan.
 */
public class CmObjeto {

    private final String distName;
    private final String clase;
    private final String rc;
    private final Integer mrbtsId;
    private final Integer lnbtsId;
    private final Integer lncelId;
    private String nombre;
    private final String version;
    private final Map<String, String> valores = new LinkedHashMap<>();

    public CmObjeto(String distName, String clase, String rc, Integer mrbtsId, Integer lnbtsId,
                    Integer lncelId, String nombre, String version) {
        this.distName = distName;
        this.clase = clase;
        this.rc = rc;
        this.mrbtsId = mrbtsId;
        this.lnbtsId = lnbtsId;
        this.lncelId = lncelId;
        this.nombre = nombre;
        this.version = version;
    }

    public String getDistName() {
        return distName;
    }

    public String getClase() {
        return clase;
    }

    public String getRc() {
        return rc;
    }

    public Integer getMrbtsId() {
        return mrbtsId;
    }

    public Integer getLnbtsId() {
        return lnbtsId;
    }

    public Integer getLncelId() {
        return lncelId;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getVersion() {
        return version;
    }

    public Map<String, String> getValores() {
        return valores;
    }
}
