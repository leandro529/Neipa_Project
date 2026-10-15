package com.neipa.cm.conexion;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.neipa.cm.CmFuente;
import com.neipa.cm.CmNombres;
import com.neipa.cm.JdbcCmFuenteFactory;
import com.neipa.cm.MotorCm;
import com.neipa.exception.RecursoNoEncontradoException;

/**
 * Conexiones a bases de datos con la estructura del CM de NetAct (NSN):
 * alta/baja/modificacion, prueba de conexion y apertura de la fuente para
 * importar. La conexion se abre siempre en solo lectura.
 */
@Service
public class CmConexionService {

    private static final Logger LOG = LoggerFactory.getLogger(CmConexionService.class);
    private static final String CLAVE_DESARROLLO = "neipa-desarrollo-cambiar-esta-clave";

    /**
     * Propiedades de URL que permiten ejecutar codigo, leer archivos locales o
     * cargar clases en el servidor de NEIPA (por ejemplo autoDeserialize o
     * allowLoadLocalInfile de MySQL, socketFactory de PostgreSQL): se rechazan.
     */
    private static final List<String> PROPIEDADES_PROHIBIDAS = Arrays.asList(
            "autodeserialize", "allowloadlocalinfile", "allowurlinlocalinfile", "allowloadlocalinfileinpath",
            "queryinterceptors", "statementinterceptors", "exceptioninterceptors", "connectionlifecycleinterceptors",
            "propertiestransform", "clientinfoprovider", "authenticationplugins", "defaultauthenticationplugin",
            "socketfactory", "sslfactory", "sslhostnameverifier", "sslpasswordcallback", "loggerfile",
            "authenticationpluginclassname");

    /** Clases que se informan al probar una conexion (las que usa NEIPA). */
    private static final List<String> CLASES_CM = Arrays.asList(CmNombres.LNBTS, CmNombres.LNCEL, "LNCEL_FDD",
            "LNCEL_TDD", "SIB", "CADPR", "LNBTS_FDD", "LNHOIF", "IRFIM", "MRBTS");

    /** Abre la conexion JDBC (reemplazable en los tests). */
    @FunctionalInterface
    public interface Abridor {
        Connection abrir(String url, Properties propiedades) throws SQLException;
    }

    private final CmConexionRepositorio repositorio;
    private final CifradorSecretos cifrador;
    private final Abridor abridor;

    @Autowired
    public CmConexionService(CmConexionRepositorio repositorio,
                             @Value("${neipa.cm.clave-conexiones:}") String clave) {
        this(repositorio, clave, DriverManager::getConnection);
    }

    public CmConexionService(CmConexionRepositorio repositorio, String clave, Abridor abridor) {
        this.repositorio = repositorio;
        if (clave == null || clave.trim().isEmpty()) {
            LOG.warn("neipa.cm.clave-conexiones no esta configurada: se usa una clave de desarrollo para cifrar"
                    + " las contrasenas de las conexiones de CM. Configurar NEIPA_CM_CLAVE_CONEXIONES.");
            clave = CLAVE_DESARROLLO;
        }
        this.cifrador = new CifradorSecretos(clave);
        this.abridor = abridor;
        DriverManager.setLoginTimeout(15);
    }

    public List<CmConexion> listar() {
        return repositorio.listar();
    }

    public CmConexion obtener(long id) {
        return repositorio.buscar(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la conexion de CM con id " + id));
    }

    public CmConexion crear(CmConexion c) {
        c.setIdConexion(null);
        validar(c);
        if (repositorio.buscarPorNombre(c.getNombre().trim()).isPresent()) {
            throw new IllegalArgumentException("Ya existe una conexion llamada " + c.getNombre().trim());
        }
        c.setContrasenaCifrada(vacio(c.getContrasena()) ? null : cifrador.cifrar(c.getContrasena()));
        return repositorio.guardar(normalizar(c));
    }

    /** Si no viene contrasena se conserva la guardada. */
    public CmConexion actualizar(long id, CmConexion cambios) {
        CmConexion existente = obtener(id);
        cambios.setIdConexion(id);
        validar(cambios);
        repositorio.buscarPorNombre(cambios.getNombre().trim()).ifPresent(otra -> {
            if (otra.getIdConexion() != id) {
                throw new IllegalArgumentException("Ya existe una conexion llamada " + cambios.getNombre().trim());
            }
        });
        cambios.setContrasenaCifrada(vacio(cambios.getContrasena()) ? existente.getContrasenaCifrada()
                : cifrador.cifrar(cambios.getContrasena()));
        return repositorio.guardar(normalizar(cambios));
    }

    public void borrar(long id) {
        obtener(id);
        repositorio.borrar(id);
    }

    /** Prueba una conexion guardada y registra el resultado. */
    public Map<String, Object> probar(long id) {
        CmConexion c = obtener(id);
        Map<String, Object> r = probar(c, cifrador.descifrar(c.getContrasenaCifrada()));
        repositorio.registrarPrueba(id, JdbcRecorte.recortar(String.valueOf(r.get("mensaje")), 500));
        return r;
    }

    /** Prueba una conexion sin guardarla (si trae id y no trae contrasena, usa la guardada). */
    public Map<String, Object> probarSinGuardar(CmConexion c) {
        validar(c);
        String contrasena = c.getContrasena();
        if (vacio(contrasena) && c.getIdConexion() != null) {
            contrasena = cifrador.descifrar(obtener(c.getIdConexion()).getContrasenaCifrada());
        }
        return probar(normalizar(c), contrasena);
    }

    private Map<String, Object> probar(CmConexion c, String contrasena) {
        Map<String, Object> r = new LinkedHashMap<>();
        long inicio = System.currentTimeMillis();
        try (CmFuente fuente = abrirFuente(c, contrasena)) {
            Set<String> tablas = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
            tablas.addAll(fuente.tablas());
            List<String> clases = new ArrayList<>();
            for (String clase : CLASES_CM) {
                if (tablas.contains(clase)) {
                    clases.add(clase);
                }
            }
            boolean estructura = tablas.contains(CmNombres.LNBTS) && tablas.contains(CmNombres.LNCEL);
            if (estructura) {
                Set<String> columnas = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
                columnas.addAll(fuente.columnas(CmNombres.LNCEL));
                estructura = columnas.containsAll(Arrays.asList("distName", "PLMN_id", "LNBTS_id"));
            }
            r.put("ok", estructura);
            r.put("tablas", tablas.size());
            r.put("clasesCm", clases);
            r.put("mensaje", estructura
                    ? "Conexion OK: " + tablas.size() + " tablas, con la estructura del CM de NetAct (" + String.join(", ", clases) + ")"
                    : "Se conecto, pero no tiene la estructura del CM de NetAct: faltan las tablas LNBTS/LNCEL"
                            + " o las columnas distName, PLMN_id y LNBTS_id (" + tablas.size() + " tablas)");
        } catch (RuntimeException ex) {
            r.put("ok", false);
            r.put("tablas", 0);
            r.put("clasesCm", new ArrayList<>());
            r.put("mensaje", "No se pudo conectar: " + mensaje(ex));
        }
        r.put("milisegundos", System.currentTimeMillis() - inicio);
        return r;
    }

    /** Fuente de CM de una conexion guardada (para importar). */
    public CmFuente abrirFuente(long id) {
        CmConexion c = obtener(id);
        return abrirFuente(c, cifrador.descifrar(c.getContrasenaCifrada()));
    }

    /** Texto con el que se registra la importacion. */
    public String origen(long id) {
        CmConexion c = obtener(id);
        return JdbcRecorte.recortar("conexion " + c.getNombre() + " (" + c.getMotor() + ")", 255);
    }

    private CmFuente abrirFuente(CmConexion c, String contrasena) {
        Properties p = new Properties();
        if (!vacio(c.getUsuario())) {
            p.setProperty("user", c.getUsuario());
        }
        if (contrasena != null) {
            p.setProperty("password", contrasena);
        }
        Connection conexion = null;
        try {
            conexion = abridor.abrir(c.getUrl(), p);
            if (conexion == null) {
                throw new IllegalStateException("No hay un driver JDBC para " + c.getUrl());
            }
            conexion.setReadOnly(true);
            return JdbcCmFuenteFactory.crear(conexion, c.getMotor(), c.getEsquema());
        } catch (SQLException ex) {
            cerrar(conexion);
            throw new IllegalStateException(ex.getMessage(), ex);
        } catch (RuntimeException ex) {
            cerrar(conexion);
            throw ex;
        }
    }

    private static void cerrar(Connection c) {
        if (c != null) {
            try {
                c.close();
            } catch (SQLException ignorada) {
                // nada
            }
        }
    }

    void validar(CmConexion c) {
        if (vacio(c.getNombre())) {
            throw new IllegalArgumentException("Falta el nombre de la conexion");
        }
        if (c.getNombre().trim().length() > 80) {
            throw new IllegalArgumentException("El nombre admite hasta 80 caracteres");
        }
        if (c.getMotor() == null || c.getMotor() == MotorCm.SQLITE) {
            throw new IllegalArgumentException(
                    "Motor invalido: usar MYSQL, POSTGRESQL, ORACLE o SQLSERVER (los dumps SQLite se importan como archivo)");
        }
        String url = c.getUrl() == null ? "" : c.getUrl().trim();
        if (!c.getMotor().aceptaUrl(url)) {
            throw new IllegalArgumentException("La URL de " + c.getMotor() + " tiene que empezar con "
                    + c.getMotor().getPrefijoUrl() + " (ejemplo: " + c.getMotor().getUrlEjemplo() + ")");
        }
        if (url.length() > 500) {
            throw new IllegalArgumentException("La URL admite hasta 500 caracteres");
        }
        String minuscula = url.toLowerCase(Locale.ROOT);
        for (String prohibida : PROPIEDADES_PROHIBIDAS) {
            if (minuscula.contains(prohibida)) {
                throw new IllegalArgumentException("La URL no puede incluir la propiedad " + prohibida
                        + " (por seguridad, NEIPA solo lee datos)");
            }
        }
        if (c.getEsquema() != null && c.getEsquema().length() > 120) {
            throw new IllegalArgumentException("El esquema admite hasta 120 caracteres");
        }
    }

    private static CmConexion normalizar(CmConexion c) {
        c.setNombre(c.getNombre().trim());
        c.setUrl(c.getUrl().trim());
        c.setUsuario(vacio(c.getUsuario()) ? null : c.getUsuario().trim());
        c.setEsquema(vacio(c.getEsquema()) ? null : c.getEsquema().trim());
        return c;
    }

    private static boolean vacio(String s) {
        return s == null || s.trim().isEmpty();
    }

    private static String mensaje(Throwable ex) {
        Throwable t = ex;
        while (t.getCause() != null && t.getCause() != t && t.getMessage() == null) {
            t = t.getCause();
        }
        return t.getMessage() == null ? t.getClass().getSimpleName() : t.getMessage();
    }

    /** Recorte de textos para las columnas VARCHAR. */
    static final class JdbcRecorte {
        private JdbcRecorte() {
        }

        static String recortar(String s, int max) {
            return s == null || s.length() <= max ? s : s.substring(0, max);
        }
    }
}
