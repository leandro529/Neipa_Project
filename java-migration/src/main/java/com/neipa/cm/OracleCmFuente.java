package com.neipa.cm;

import java.sql.Connection;

/**
 * CM en Oracle. Usa el comportamiento JDBC estandar de la clase base: schema
 * de la conexion (o el indicado, resuelto sin distinguir mayusculas porque
 * Oracle guarda LNCEL_ID) y metadata del driver para tablas y columnas.
 */
public class OracleCmFuente extends JdbcCmFuente {

    public OracleCmFuente(Connection conexion, String esquema) {
        super(conexion, MotorCm.ORACLE, esquema);
    }
}
