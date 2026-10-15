package com.neipa.cm;

import java.sql.Connection;

/**
 * Factory de las fuentes de CM por JDBC: segun el motor devuelve la subclase
 * de {@link JdbcCmFuente} que corresponde. Quien la usa trabaja siempre con
 * el tipo base ({@link CmFuente}) y no sabe que motor hay detras.
 */
public final class JdbcCmFuenteFactory {

    private JdbcCmFuenteFactory() {
    }

    /**
     * @param conexion conexion JDBC ya abierta (la fuente la cierra)
     * @param motor    motor de la base
     * @param esquema  base (MySQL) o schema (resto); vacio = el de la conexion. SQLite lo ignora.
     */
    public static JdbcCmFuente crear(Connection conexion, MotorCm motor, String esquema) {
        if (motor == null) {
            throw new IllegalArgumentException("Falta el motor de la base de CM");
        }
        switch (motor) {
            case SQLITE:
                return new SqliteCmFuente(conexion);
            case MYSQL:
                return new MySqlCmFuente(conexion, esquema);
            case POSTGRESQL:
                return new PostgresCmFuente(conexion, esquema);
            case ORACLE:
                return new OracleCmFuente(conexion, esquema);
            case SQLSERVER:
                return new SqlServerCmFuente(conexion, esquema);
            default:
                throw new IllegalArgumentException("Motor de CM no soportado: " + motor);
        }
    }
}
