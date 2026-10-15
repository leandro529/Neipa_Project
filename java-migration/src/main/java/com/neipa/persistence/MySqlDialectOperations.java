package com.neipa.persistence;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

/**
 * Implementacion MySQL de SqlDialectOperations. Migra directamente la logica
 * de include/loaders/utilitiesModule/MySqlOperations.py del repo original (misma consulta de
 * existencia de tabla via information_schema, mismo DELETE con filtro
 * opcional, y el mismo "INSERT ... ON DUPLICATE KEY UPDATE" que ya usaba el
 * codigo Python original para MySQL en su metodo merge()).
 */
@Component
public class MySqlDialectOperations implements SqlDialectOperations {

    @Override
    public String existeTabla(String schema, String tabla) {
        return "SELECT EXISTS (" +
                " SELECT TABLE_NAME" +
                " FROM information_schema.TABLES" +
                " WHERE TABLE_SCHEMA LIKE '" + schema + "'" +
                " AND TABLE_TYPE LIKE 'BASE TABLE'" +
                " AND TABLE_NAME = '" + tabla + "'" +
                ") AS EXISTE;";
    }

    @Override
    public String truncarTabla(String schema, String tabla) {
        return "TRUNCATE TABLE " + schema + "." + tabla + ";";
    }

    @Override
    public String borrarDeTabla(String schema, String tabla, String filtro) {
        String condicion = (filtro == null || filtro.isEmpty()) ? "" : " AND " + filtro + " ";
        return "DELETE FROM " + schema + "." + tabla +
                " WHERE 1 = 1" + condicion + ";";
    }

    @Override
    public String upsert(String schema, String tabla, List<String> columnas, List<String> columnasClave) {
        String columnasInsert = String.join(",", columnas);
        String columnasSelect = columnas.stream().map(c -> "b." + c).collect(Collectors.joining(","));
        String actualizaciones = columnas.stream()
                .filter(c -> !columnasClave.contains(c))
                .map(c -> c + " = b." + c)
                .collect(Collectors.joining(","));

        return "INSERT INTO " + schema + "." + tabla + " (" + columnasInsert + ")" +
                " SELECT " + columnasSelect +
                " FROM " + schema + "." + tabla + "_stg b" +
                " ON DUPLICATE KEY UPDATE " + actualizaciones + ";";
    }
}
