package com.neipa.persistence;

import org.springframework.stereotype.Component;

/**
 * Factory de operaciones SQL segun el motor de base de datos (RF20, RF21).
 * Equivalente Java de SqlFactory.py. Solo MySQL tiene implementacion en esta
 * migracion (persistencia propia de NEIPA, RNF14); MsSql, Postgres y BigQuery
 * (usados para leer las bases de los clientes o para BigQuery/Domo) quedan
 * fuera del alcance acordado y lanzan UnsupportedOperationException, tal
 * como en el codigo Python original las variantes no usadas por un DAG dado
 * tampoco estaban invocadas.
 */
@Component
public class SqlDialectFactory {

    private final MySqlDialectOperations mySqlDialectOperations;

    public SqlDialectFactory(MySqlDialectOperations mySqlDialectOperations) {
        this.mySqlDialectOperations = mySqlDialectOperations;
    }

    public SqlDialectOperations crear(ConnType tipo) {
        switch (tipo) {
            case MYSQL:
                return mySqlDialectOperations;
            case MSSQL:
            case POSTGRES:
            case BIGQUERY:
                throw new UnsupportedOperationException(
                        "Operaciones SQL no implementadas para el motor: " + tipo);
            default:
                throw new IllegalArgumentException("Motor de base de datos desconocido: " + tipo);
        }
    }
}
