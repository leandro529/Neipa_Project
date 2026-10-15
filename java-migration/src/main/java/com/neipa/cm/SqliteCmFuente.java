package com.neipa.cm;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

/**
 * Dump SQLite de NetAct (driver org.xerial:sqlite-jdbc), siempre en modo
 * solo lectura: NEIPA nunca modifica el archivo.
 *
 * Sobrescribe la lectura de tablas y columnas: no usa el metadata del driver
 * porque con mas de 500 columnas (LNBTS tiene ~930, LNCEL ~950) sqlite-jdbc
 * arma un SELECT compuesto que supera el limite ("too many terms in compound
 * SELECT"). Tampoco califica las tablas: un archivo SQLite no tiene schema.
 */
public class SqliteCmFuente extends JdbcCmFuente {

    /** Abre el archivo del dump. */
    public SqliteCmFuente(Path archivo) {
        this(abrir(archivo));
    }

    /** Usa una conexion ya abierta a un dump (la crea {@link JdbcCmFuenteFactory}). */
    public SqliteCmFuente(Connection conexion) {
        super(conexion, MotorCm.SQLITE, null);
    }

    private static Connection abrir(Path archivo) {
        try {
            Properties propiedades = new Properties();
            propiedades.setProperty("open_mode", "1"); // SQLITE_OPEN_READONLY
            return DriverManager.getConnection("jdbc:sqlite:" + archivo.toAbsolutePath(), propiedades);
        } catch (SQLException ex) {
            throw new IllegalStateException("No se pudo abrir el dump de CM " + archivo + ": " + ex.getMessage(), ex);
        }
    }

    @Override
    protected String esquema(Connection conexion, String pedido) {
        return null;
    }

    /** Directo de sqlite_master: el dump de NetAct tiene mas de 3000 tablas. */
    @Override
    protected Map<String, String> leerTablas() throws SQLException {
        Map<String, String> tablas = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        try (Statement st = conexion().createStatement();
             ResultSet rs = st.executeQuery("SELECT name FROM sqlite_master WHERE type IN ('table', 'view')")) {
            while (rs.next()) {
                tablas.putIfAbsent(rs.getString(1), rs.getString(1));
            }
        }
        return tablas;
    }

    /** PRAGMA table_info en lugar de getColumns() (ver el comentario de la clase). */
    @Override
    protected Map<String, String> leerColumnas(String tablaReal) throws SQLException {
        Map<String, String> columnas = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        try (Statement st = conexion().createStatement();
             ResultSet rs = st.executeQuery("PRAGMA table_info(" + motor().citar(tablaReal) + ")")) {
            while (rs.next()) {
                String c = rs.getString("name");
                columnas.putIfAbsent(c, c);
            }
        }
        return columnas;
    }
}
