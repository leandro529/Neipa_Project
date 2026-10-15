package com.neipa.cm;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Origen de los datos de CM (hoy, el dump SQLite de NetAct). Cada tabla es
 * una clase de objeto gestionado; cada fila, un objeto con su distName, los
 * ids de su jerarquia (PLMN_id, MRBTS_id, LNBTS_id, ...) y una columna por
 * parametro.
 */
public interface CmFuente extends AutoCloseable {

    /** Tablas (clases) disponibles. */
    List<String> tablas();

    /** Columnas de una tabla. */
    List<String> columnas(String tabla);

    /** Recorre las filas de la tabla trayendo solo las columnas pedidas. */
    void recorrer(String tabla, List<String> columnas, Consumer<Map<String, Object>> fila);

    @Override
    void close();
}
