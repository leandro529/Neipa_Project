package com.neipa.cm;

import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Una "base de datos" JDBC en memoria (proxies de Connection, DatabaseMetaData,
 * Statement y ResultSet) con las tablas de un FakeCmFuente, para probar
 * JdbcCmFuente sin un motor real. Los nombres se pueden transformar (por
 * ejemplo a minuscula, como PostgreSQL con tablas creadas sin comillas).
 */
public class BaseCmFicticia {

    final FakeCmFuente datos;
    final Function<String, String> nombres;
    final String esquema;
    public final List<String> consultas = new ArrayList<>();
    final List<Integer> fetchSizes = new ArrayList<>();
    public boolean soloLectura;
    /** Como sqlite-jdbc con tablas de mas de 500 columnas: getColumns/getTables fallan. */
    public boolean metadataFalla;
    boolean autoCommit = true;
    public boolean cerrada;

    public BaseCmFicticia(FakeCmFuente datos, Function<String, String> nombres, String esquema) {
        this.datos = datos;
        this.nombres = nombres;
        this.esquema = esquema;
    }

    public Connection conexion() {
        return proxy(Connection.class, (metodo, args) -> {
            switch (metodo) {
                case "getMetaData": return metadata();
                case "createStatement": return statement();
                case "setReadOnly": soloLectura = (Boolean) args[0]; return null;
                case "setAutoCommit": autoCommit = (Boolean) args[0]; return null;
                case "getAutoCommit": return autoCommit;
                case "getSchema": return esquema;
                case "getCatalog": return esquema;
                case "close": cerrada = true; return null;
                case "rollback": return null;
                default: throw new UnsupportedOperationException(metodo);
            }
        });
    }

    private DatabaseMetaData metadata() {
        return proxy(DatabaseMetaData.class, (metodo, args) -> {
            switch (metodo) {
                case "getSchemas":
                    return resultSet(Arrays.asList("TABLE_SCHEM"), Arrays.asList(Arrays.asList((Object) esquema)));
                case "getTables":
                    fallarSiCorresponde();
                    return resultSet(Arrays.asList("TABLE_NAME"), tablasReales());
                case "getColumns":
                    fallarSiCorresponde();
                    return columnasRs((String) args[2]);
                default: throw new UnsupportedOperationException(metodo);
            }
        });
    }

    private void fallarSiCorresponde() throws java.sql.SQLException {
        if (metadataFalla) {
            throw new java.sql.SQLException(
                    "[SQLITE_ERROR] SQL error or missing database (too many terms in compound SELECT)");
        }
    }

    private List<List<Object>> tablasReales() {
        List<List<Object>> filas = new ArrayList<>();
        for (String t : datos.columnas.keySet()) {
            filas.add(Arrays.asList((Object) nombres.apply(t)));
        }
        return filas;
    }

    private ResultSet columnasRs(String patron) {
        List<List<Object>> filas = new ArrayList<>();
        for (Map.Entry<String, List<String>> t : datos.columnas.entrySet()) {
            String real = nombres.apply(t.getKey());
            // Como el patron de JDBC: "_" es comodin.
            if (real.matches(patron.replace("_", "."))) {
                for (String c : t.getValue()) {
                    filas.add(Arrays.asList((Object) real, nombres.apply(c)));
                }
            }
        }
        return resultSet(Arrays.asList("TABLE_NAME", "COLUMN_NAME"), filas);
    }

    private static final Pattern PRAGMA = Pattern.compile("PRAGMA table_info\\(\"(.+)\"\\)");

    private static final Pattern SELECT = Pattern.compile("SELECT (.+) FROM (.+)");

    private Statement statement() {
        return proxy(Statement.class, (metodo, args) -> {
            switch (metodo) {
                case "setFetchSize": fetchSizes.add((Integer) args[0]); return null;
                case "close": return null;
                case "executeQuery": {
                    String sql = (String) args[0];
                    consultas.add(sql);
                    if (sql.startsWith("SELECT name FROM sqlite_master")) {
                        return resultSet(Arrays.asList("name"), tablasReales());
                    }
                    Matcher pragma = PRAGMA.matcher(sql);
                    if (pragma.matches()) {
                        List<List<Object>> cols = new ArrayList<>();
                        for (Map.Entry<String, List<String>> t : datos.columnas.entrySet()) {
                            if (nombres.apply(t.getKey()).equals(pragma.group(1))) {
                                for (String c : t.getValue()) {
                                    cols.add(Arrays.asList((Object) nombres.apply(c)));
                                }
                            }
                        }
                        return resultSet(Arrays.asList("name"), cols);
                    }
                    Matcher m = SELECT.matcher(sql);
                    if (!m.matches()) {
                        throw new IllegalArgumentException(sql);
                    }
                    List<String> cols = new ArrayList<>();
                    for (String c : m.group(1).split(", ")) {
                        cols.add(sinComillas(c));
                    }
                    String[] partes = m.group(2).split("\\.");
                    String tablaReal = sinComillas(partes[partes.length - 1]);
                    String tabla = null;
                    for (String t : datos.columnas.keySet()) {
                        if (nombres.apply(t).equals(tablaReal)) {
                            tabla = t;
                        }
                    }
                    List<List<Object>> filas = new ArrayList<>();
                    for (Map<String, Object> f : datos.filas.get(tabla)) {
                        List<Object> fila = new ArrayList<>();
                        for (String c : cols) {
                            String original = null;
                            for (String x : datos.columnas.get(tabla)) {
                                if (nombres.apply(x).equals(c)) {
                                    original = x;
                                }
                            }
                            if (original == null) {
                                throw new IllegalArgumentException("columna inexistente " + c);
                            }
                            fila.add(f.get(original));
                        }
                        filas.add(fila);
                    }
                    return resultSet(cols, filas);
                }
                default: throw new UnsupportedOperationException(metodo);
            }
        });
    }

    private static String sinComillas(String s) {
        s = s.trim();
        char a = s.charAt(0);
        if (a == '"' || a == '`' || a == '[') {
            return s.substring(1, s.length() - 1);
        }
        return s;
    }

    private static ResultSet resultSet(List<String> columnas, List<List<Object>> filas) {
        int[] actual = {-1};
        return proxy(ResultSet.class, (metodo, args) -> {
            switch (metodo) {
                case "next": return ++actual[0] < filas.size();
                case "close": return null;
                case "getString": {
                    Object v = valor(columnas, filas.get(actual[0]), args[0]);
                    return v == null ? null : v.toString();
                }
                case "getObject": return valor(columnas, filas.get(actual[0]), args[0]);
                default: throw new UnsupportedOperationException(metodo);
            }
        });
    }

    private static Object valor(List<String> columnas, List<Object> fila, Object indice) {
        if (indice instanceof Integer) {
            return fila.get((Integer) indice - 1);
        }
        return fila.get(columnas.indexOf((String) indice));
    }

    interface Manejador {
        Object llamar(String metodo, Object[] args) throws Exception;
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> tipo, Manejador m) {
        return (T) Proxy.newProxyInstance(BaseCmFicticia.class.getClassLoader(), new Class<?>[] {tipo},
                (p, metodo, args) -> {
                    if (metodo.getDeclaringClass() == Object.class) {
                        switch (metodo.getName()) {
                            case "hashCode": return System.identityHashCode(p);
                            case "equals": return p == args[0];
                            default: return tipo.getSimpleName();
                        }
                    }
                    return m.llamar(metodo.getName(), args);
                });
    }
}
