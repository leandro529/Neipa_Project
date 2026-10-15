package com.neipa.persistence;

import java.util.List;

/**
 * Operaciones SQL que dependen del motor de base de datos concreto (RF20,
 * RF21: poder incorporar un nuevo motor de base de datos sin modificar la
 * logica de auditoria/carga). Equivalente Java, acotado a lo que usa esta
 * migracion, de la clase abstracta SqlOperations
 * (include/loaders/utilitiesModule/SqlOperations.py) del repo original.
 */
public interface SqlDialectOperations {

    String existeTabla(String schema, String tabla);

    String truncarTabla(String schema, String tabla);

    String borrarDeTabla(String schema, String tabla, String filtro);

    /**
     * Sentencia de "upsert" (insertar o actualizar si ya existe), usada para
     * cargar resultados sin duplicar filas.
     */
    String upsert(String schema, String tabla, List<String> columnas, List<String> columnasClave);
}
