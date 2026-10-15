package com.neipa.cm;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * CM en MySQL / MariaDB. En MySQL el "esquema" es la base (el catalogo de
 * JDBC), y Connector/J solo lee de a poco, sin cargar toda la tabla en
 * memoria, con fetchSize = Integer.MIN_VALUE.
 */
public class MySqlCmFuente extends JdbcCmFuente {

    public MySqlCmFuente(Connection conexion, String base) {
        super(conexion, MotorCm.MYSQL, base);
    }

    @Override
    protected String catalogo(Connection conexion, String pedido) throws SQLException {
        return pedido != null ? pedido : conexion.getCatalog();
    }

    @Override
    protected String esquema(Connection conexion, String pedido) {
        return null;
    }

    @Override
    protected int filasPorLectura() {
        return Integer.MIN_VALUE;
    }
}
