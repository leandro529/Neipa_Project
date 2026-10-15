package com.neipa.persistence;

/**
 * Motores de base de datos de origen/destino soportados por el patron
 * Factory de persistencia (RF20, RF21). Equivalente Java del enum ConnType
 * (include/loaders/utilitiesModule/ConnTypes.py del repo original).
 */
public enum ConnType {
    MYSQL,
    MSSQL,
    POSTGRES,
    BIGQUERY
}
