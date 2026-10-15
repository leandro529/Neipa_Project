package com.neipa.cm;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Consumer;

/**
 * Base de las fuentes de CM que leen por JDBC una base con la estructura del
 * dump de NetAct (una tabla por clase, columnas distName, PLMN_id, ...). Solo
 * lectura.
 *
 * Es abstracta: cada motor tiene su subclase ({@link SqliteCmFuente},
 * {@link MySqlCmFuente}, {@link PostgresCmFuente}, {@link OracleCmFuente},
 * {@link SqlServerCmFuente}) y la que corresponde la crea
 * {@link JdbcCmFuenteFactory}. Esta clase tiene el algoritmo comun (resolver
 * nombres, armar el SELECT, recorrer filas) y deja como metodos protegidos los
 * puntos que cambian segun el motor; las subclases sobrescriben solo esos.
 *
 * Los nombres de tabla y columna se resuelven sin distinguir mayusculas: si
 * las tablas se crearon sin comillas, PostgreSQL las guarda en minuscula
 * (lncel, lnbts_id) y Oracle en mayuscula (LNCEL, LNBTS_ID). NEIPA pide los
 * nombres como en el dump (LNCEL, LNBTS_id, n310Qci1) y esta clase los
 * traduce a los reales; las filas vuelven con los nombres pedidos.
 */
public abstract class JdbcCmFuente implements CmFuente {

    /** Filas que se piden por viaje al leer una tabla (salvo que el motor diga otra cosa). */
    protected static final int FILAS_POR_LECTURA = 5000;

    private final Connection conexion;
    private final MotorCm motor;
    private final String esquemaPedido;
    private boolean preparada;
    private String catalogo;
    private String esquema;
    private Map<String, String> tablasReales;
    private String esquemaResuelto;
    private boolean esquemaBuscado;
    private final Map<String, Map<String, String>> columnasReales = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

    /**
     * @param esquema base (MySQL) o schema (PostgreSQL, Oracle, SQL Server); vacio = el de la conexion
     */
    protected JdbcCmFuente(Connection conexion, MotorCm motor, String esquema) {
        this.conexion = conexion;
        this.motor = motor;
        this.esquemaPedido = esquema == null || esquema.trim().isEmpty() ? null : esquema.trim();
    }

    /**
     * Se ejecuta una vez, antes de la primera lectura (y no en el constructor,
     * para no llamar metodos sobrescritos con la subclase a medio construir).
     */
    private void preparar() {
        if (preparada) {
            return;
        }
        try {
            prepararConexion(conexion);
            catalogo = catalogo(conexion, esquemaPedido);
            esquema = esquema(conexion, esquemaPedido);
        } catch (SQLException ex) {
            throw new IllegalStateException("No se pudo preparar la conexion: " + ex.getMessage(), ex);
        }
        preparada = true;
    }

    // ----- Puntos que cambian segun el motor (se sobrescriben en las subclases) -----

    /** Ajustes de la conexion antes de leer. Por defecto, ninguno. */
    protected void prepararConexion(Connection conexion) throws SQLException {
        // la mayoria de los motores no necesita nada
    }

    /** Catalogo con el que se califican las tablas. Por defecto no se usa. */
    protected String catalogo(Connection conexion, String pedido) throws SQLException {
        return null;
    }

    /** Schema con el que se califican las tablas: el pedido o el de la conexion. */
    protected String esquema(Connection conexion, String pedido) throws SQLException {
        return pedido != null ? pedido : conexion.getSchema();
    }

    /** Tablas y vistas (nombre real, sin distinguir mayusculas). Por defecto, con el metadata de JDBC. */
    protected Map<String, String> leerTablas() throws SQLException {
        Map<String, String> tablas = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        DatabaseMetaData meta = conexion.getMetaData();
        try (ResultSet rs = meta.getTables(catalogo, esquemaReal(meta), "%", new String[] {"TABLE", "VIEW"})) {
            while (rs.next()) {
                tablas.putIfAbsent(rs.getString("TABLE_NAME"), rs.getString("TABLE_NAME"));
            }
        }
        return tablas;
    }

    /** Columnas de una tabla (nombre real). Por defecto, con el metadata de JDBC. */
    protected Map<String, String> leerColumnas(String tablaReal) throws SQLException {
        Map<String, String> columnas = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        DatabaseMetaData meta = conexion.getMetaData();
        try (ResultSet rs = meta.getColumns(catalogo, esquemaReal(meta), tablaReal, "%")) {
            while (rs.next()) {
                // El nombre de tabla es un patron ("_" comodin): se filtra por igualdad.
                if (tablaReal.equals(rs.getString("TABLE_NAME"))) {
                    String c = rs.getString("COLUMN_NAME");
                    columnas.putIfAbsent(c, c);
                }
            }
        }
        return columnas;
    }

    /** Valor de fetchSize al recorrer una tabla. */
    protected int filasPorLectura() {
        return FILAS_POR_LECTURA;
    }

    protected final Connection conexion() {
        return conexion;
    }

    protected final MotorCm motor() {
        return motor;
    }

    // ----- Algoritmo comun -----

    @Override
    public List<String> tablas() {
        return new ArrayList<>(tablasReales().values());
    }

    private Map<String, String> tablasReales() {
        if (tablasReales == null) {
            preparar();
            try {
                tablasReales = leerTablas();
            } catch (SQLException ex) {
                throw new IllegalStateException("No se pudieron listar las tablas: " + ex.getMessage(), ex);
            }
        }
        return tablasReales;
    }

    /** El schema tal como lo guarda el motor (Oracle en mayuscula, PostgreSQL en minuscula). */
    private String esquemaReal(DatabaseMetaData meta) throws SQLException {
        if (esquemaBuscado) {
            return esquemaResuelto;
        }
        esquemaResuelto = esquema;
        if (esquema != null) {
            try (ResultSet rs = meta.getSchemas()) {
                while (rs.next()) {
                    String s = rs.getString("TABLE_SCHEM");
                    if (s != null && s.equalsIgnoreCase(esquema)) {
                        esquemaResuelto = s;
                        break;
                    }
                }
            }
        }
        esquemaBuscado = true;
        return esquemaResuelto;
    }

    @Override
    public List<String> columnas(String tabla) {
        return new ArrayList<>(columnasDe(tabla).values());
    }

    private Map<String, String> columnasDe(String tabla) {
        String real = tablaReal(tabla);
        Map<String, String> cols = columnasReales.get(real);
        if (cols == null) {
            try {
                cols = leerColumnas(real);
            } catch (SQLException ex) {
                throw new IllegalStateException("No se pudieron leer las columnas de " + tabla + ": " + ex.getMessage(), ex);
            }
            columnasReales.put(real, cols);
        }
        return cols;
    }

    private String tablaReal(String tabla) {
        String real = tablasReales().get(tabla);
        if (real == null) {
            throw new IllegalArgumentException("No existe la tabla " + tabla);
        }
        return real;
    }

    /** SELECT con los nombres reales, calificado con la base o el schema si se indico. */
    String consulta(String tabla, List<String> columnas) {
        Map<String, String> reales = columnasDe(tabla);
        StringBuilder sql = new StringBuilder("SELECT ");
        for (int i = 0; i < columnas.size(); i++) {
            String real = reales.get(columnas.get(i));
            if (real == null) {
                throw new IllegalArgumentException("La tabla " + tabla + " no tiene la columna " + columnas.get(i));
            }
            sql.append(i > 0 ? ", " : "").append(motor.citar(real));
        }
        sql.append(" FROM ");
        tablasReales(); // resuelve el esquema real
        String prefijo = catalogo != null ? catalogo : esquemaResuelto;
        if (prefijo != null) {
            sql.append(motor.citar(prefijo)).append('.');
        }
        sql.append(motor.citar(tablaReal(tabla)));
        return sql.toString();
    }

    @Override
    public void recorrer(String tabla, List<String> columnas, Consumer<Map<String, Object>> fila) {
        String sql = consulta(tabla, columnas);
        try (Statement st = conexion.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY)) {
            st.setFetchSize(filasPorLectura());
            try (ResultSet rs = st.executeQuery(sql)) {
                while (rs.next()) {
                    Map<String, Object> valores = new LinkedHashMap<>();
                    for (int i = 0; i < columnas.size(); i++) {
                        valores.put(columnas.get(i), valor(rs, i + 1));
                    }
                    fila.accept(valores);
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("No se pudo leer la tabla " + tabla + ": " + ex.getMessage(), ex);
        }
    }

    /** Numeros y texto tal cual; booleanos como 1/0 (como en el dump); fechas y tipos propios como texto. */
    private static Object valor(ResultSet rs, int i) throws SQLException {
        Object v = rs.getObject(i);
        if (v == null || v instanceof Number || v instanceof String) {
            return v;
        }
        if (v instanceof Boolean) {
            return ((Boolean) v) ? 1 : 0;
        }
        return rs.getString(i);
    }

    @Override
    public void close() {
        try {
            if (!conexion.getAutoCommit()) {
                conexion.rollback();
            }
        } catch (SQLException ignorada) {
            // solo lectura: no hay nada que deshacer
        }
        try {
            conexion.close();
        } catch (SQLException ignorada) {
            // nada que hacer
        }
    }

}
