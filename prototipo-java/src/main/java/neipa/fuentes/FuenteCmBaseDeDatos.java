package neipa.fuentes;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;

import neipa.estructuras.ListaEnlazada;
import neipa.excepciones.ExtraccionException;

/**
 * Fuente de CM en una base de datos con la estructura del dump de NetAct
 * (una tabla por clase): el dump SQLite o una copia en MySQL, PostgreSQL,
 * Oracle o SQL Server. Se conecta por JDBC en solo lectura.
 *
 * El usuario y la contrasena son privados y no tienen getters: la contrasena
 * solo se usa para conectarse y nunca aparece en describir() ni en toString().
 * Necesita el driver JDBC del motor en el classpath (el prototipo no trae
 * ninguno, para no tener dependencias).
 */
public class FuenteCmBaseDeDatos extends FuenteCm {

    private final String usuario;
    private final String contrasena;
    private Connection conexion;

    public FuenteCmBaseDeDatos(String url, String usuario, String contrasena) {
        super(url);
        if (!url.trim().toLowerCase().startsWith("jdbc:")) {
            throw new IllegalArgumentException("La URL de la base tiene que empezar con jdbc:");
        }
        this.usuario = usuario;
        this.contrasena = contrasena;
    }

    @Override
    public String getTipo() {
        return "Base de datos " + getMotor();
    }

    /** Motor segun la URL: jdbc:sqlite:, jdbc:mysql:, jdbc:postgresql:... */
    public String getMotor() {
        String resto = getOrigen().substring("jdbc:".length());
        int dosPuntos = resto.indexOf(':');
        return (dosPuntos > 0 ? resto.substring(0, dosPuntos) : resto).toUpperCase();
    }

    @Override
    public String describir() {
        // Nunca la contrasena.
        return getTipo() + ": " + getOrigen()
                + (usuario == null || usuario.isEmpty() ? "" : " (usuario " + usuario + ")");
    }

    @Override
    protected void abrir() throws ExtraccionException {
        try {
            conexion = DriverManager.getConnection(getOrigen(), usuario, contrasena);
            conexion.setReadOnly(true);
        } catch (SQLException e) {
            String causa = e.getMessage() != null && e.getMessage().startsWith("No suitable driver")
                    ? "no hay un driver JDBC de " + getMotor() + " en el classpath"
                    : e.getMessage();
            throw new ExtraccionException("no se pudo conectar a la base de CM: " + causa);
        }
    }

    @Override
    protected ListaEnlazada<ObjetoCm> leerFilas(String clase) throws ExtraccionException {
        ListaEnlazada<ObjetoCm> objetos = new ListaEnlazada<ObjetoCm>();
        String sql = "SELECT * FROM " + citar(clase);
        try (Statement consulta = conexion.createStatement(); ResultSet filas = consulta.executeQuery(sql)) {
            ResultSetMetaData columnas = filas.getMetaData();
            while (filas.next()) {
                ObjetoCm objeto = nuevoObjeto(clase, filas.getString("distName"));
                for (int i = 1; i <= columnas.getColumnCount(); i++) {
                    String nombre = columnas.getColumnLabel(i);
                    if (!nombre.equalsIgnoreCase("distName")) {
                        agregarValor(objeto, nombre, filas.getString(i));
                    }
                }
                objetos.agregarAlFinal(objeto);
            }
        } catch (SQLException e) {
            if (e.getMessage() != null && e.getMessage().toLowerCase().contains("no such table")) {
                return objetos; // la base no tiene esa clase
            }
            throw new ExtraccionException("no se pudo leer la tabla " + clase + ": " + e.getMessage());
        }
        return objetos;
    }

    /** Nombre de tabla entre los delimitadores del motor. */
    private String citar(String tabla) {
        String motor = getMotor();
        if (motor.equals("MYSQL") || motor.equals("MARIADB")) {
            return "`" + tabla.replace("`", "``") + "`";
        }
        if (motor.equals("SQLSERVER")) {
            return "[" + tabla.replace("]", "]]") + "]";
        }
        return "\"" + tabla.replace("\"", "\"\"") + "\"";
    }

    @Override
    public void cerrar() {
        if (conexion != null) {
            try {
                conexion.close();
            } catch (SQLException e) {
                // nada que hacer: se estaba cerrando
            }
            conexion = null;
        }
    }
}
