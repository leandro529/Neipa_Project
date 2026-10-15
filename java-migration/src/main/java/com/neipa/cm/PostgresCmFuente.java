package com.neipa.cm;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * CM en PostgreSQL. El driver solo lee por cursor (respetando fetchSize) con
 * el autocommit apagado; al cerrar, la clase base hace rollback.
 */
public class PostgresCmFuente extends JdbcCmFuente {

    public PostgresCmFuente(Connection conexion, String esquema) {
        super(conexion, MotorCm.POSTGRESQL, esquema);
    }

    @Override
    protected void prepararConexion(Connection conexion) throws SQLException {
        conexion.setAutoCommit(false);
    }
}
