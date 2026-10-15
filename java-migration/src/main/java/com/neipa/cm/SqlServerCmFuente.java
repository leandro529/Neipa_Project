package com.neipa.cm;

import java.sql.Connection;

/**
 * CM en SQL Server. Usa el comportamiento JDBC estandar de la clase base; lo
 * propio del motor (citar con corchetes) ya esta en {@link MotorCm#SQLSERVER}.
 */
public class SqlServerCmFuente extends JdbcCmFuente {

    public SqlServerCmFuente(Connection conexion, String esquema) {
        super(conexion, MotorCm.SQLSERVER, esquema);
    }
}
