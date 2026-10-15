package com.neipa.cm.conexion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.neipa.cm.BaseCmFicticia;
import com.neipa.cm.FakeCmFuente;
import com.neipa.cm.MotorCm;

/** Conexiones directas a bases de CM: validacion, contrasena cifrada y prueba de conexion. */
class CmConexionServiceTest {

    /** Contrasena de prueba generada al ejecutar (no se versiona ninguna). */
    private static final String CLAVE = "p" + java.util.UUID.randomUUID().toString().replace("-", "");

    private final Map<Long, CmConexion> guardadas = new LinkedHashMap<>();
    private final List<String> urlsAbiertas = new ArrayList<>();
    private final List<Properties> propiedades = new ArrayList<>();
    private BaseCmFicticia base;
    private CmConexionService servicio;

    @BeforeEach
    void preparar() {
        FakeCmFuente datos = new FakeCmFuente();
        datos.tabla("LNBTS", "distName", "PLMN_id", "LNBTS_id", "name");
        datos.tabla("LNCEL", "distName", "PLMN_id", "LNBTS_id", "LNCEL_id", "n310Qci1");
        datos.tabla("SIB", "distName", "PLMN_id", "LNBTS_id", "n310");
        base = new BaseCmFicticia(datos, String::toLowerCase, "public");
        CmConexionRepositorio repo = new CmConexionRepositorio() {
            public List<CmConexion> listar() { return new ArrayList<>(guardadas.values()); }
            public Optional<CmConexion> buscar(long id) { return Optional.ofNullable(guardadas.get(id)); }
            public Optional<CmConexion> buscarPorNombre(String n) {
                return guardadas.values().stream().filter(c -> c.getNombre().equals(n)).findFirst();
            }
            public CmConexion guardar(CmConexion c) {
                if (c.getIdConexion() == null) {
                    c.setIdConexion((long) guardadas.size() + 1);
                }
                c.setContrasena(null);
                guardadas.put(c.getIdConexion(), c);
                return c;
            }
            public void borrar(long id) { guardadas.remove(id); }
            public void registrarPrueba(long id, String r) { guardadas.get(id).setUltimoResultado(r); }
        };
        servicio = new CmConexionService(repo, java.util.UUID.randomUUID().toString(), (url, p) -> {
            urlsAbiertas.add(url);
            propiedades.add(p);
            if (url.contains("caida")) {
                throw new SQLException("Connection refused");
            }
            return base.conexion();
        });
    }

    private static CmConexion conexion(String nombre, MotorCm motor, String url, String contrasena) {
        CmConexion c = new CmConexion();
        c.setNombre(nombre);
        c.setMotor(motor);
        c.setUrl(url);
        c.setUsuario("lectura");
        c.setContrasena(contrasena);
        return c;
    }

    @Test
    void guardaLaContrasenaCifradaYNuncaLaDevuelve() throws Exception {
        CmConexion c = servicio.crear(conexion(" CM Postgres ", MotorCm.POSTGRESQL,
                "jdbc:postgresql://cm:5432/netact", CLAVE));
        assertEquals("CM Postgres", c.getNombre());
        assertTrue(c.getContrasenaCifrada().startsWith("v1:"));
        assertFalse(c.getContrasenaCifrada().contains(CLAVE));

        String json = new ObjectMapper().registerModule(new JavaTimeModule()).writeValueAsString(guardadas.get(1L));
        assertFalse(json.contains(CLAVE), json);
        assertFalse(json.contains("contrasena\""), json);
        assertFalse(json.contains("v1:"), json);
        assertTrue(json.contains("\"tieneContrasena\":true"), json);
        // Pero si se recibe del formulario.
        CmConexion leida = new ObjectMapper().readValue("{\"nombre\":\"x\",\"contrasena\":\"abc\"}", CmConexion.class);
        assertEquals("abc", leida.getContrasena());

        // Modificar sin contrasena conserva la anterior; con contrasena, la reemplaza.
        String cifrada = c.getContrasenaCifrada();
        CmConexion cambio = conexion("CM Postgres", MotorCm.POSTGRESQL, "jdbc:postgresql://cm2:5432/netact", "");
        servicio.actualizar(1L, cambio);
        assertEquals(cifrada, guardadas.get(1L).getContrasenaCifrada());
        servicio.actualizar(1L, conexion("CM Postgres", MotorCm.POSTGRESQL, "jdbc:postgresql://cm2:5432/netact", CLAVE + "-2"));
        assertNotEquals(cifrada, guardadas.get(1L).getContrasenaCifrada());

        Map<String, Object> r = servicio.probar(1L);
        assertEquals(CLAVE + "-2", propiedades.get(0).getProperty("password"), "se descifra para conectarse");
        assertEquals("lectura", propiedades.get(0).getProperty("user"));
        assertEquals(Boolean.TRUE, r.get("ok"), r.toString());
        assertTrue(base.soloLectura, "la conexion se abre en solo lectura");
        assertTrue(base.cerrada);
    }

    @Test
    void laPruebaVerificaLaEstructuraDelCm() {
        Map<String, Object> ok = servicio.probarSinGuardar(conexion("pg", MotorCm.POSTGRESQL,
                "jdbc:postgresql://cm:5432/netact", "x"));
        assertEquals(Boolean.TRUE, ok.get("ok"), ok.toString());
        assertEquals(3, ok.get("tablas"));
        assertTrue(ok.get("clasesCm").toString().contains("SIB"));

        Map<String, Object> caida = servicio.probarSinGuardar(conexion("pg", MotorCm.POSTGRESQL,
                "jdbc:postgresql://caida:5432/netact", "x"));
        assertEquals(Boolean.FALSE, caida.get("ok"));
        assertTrue(caida.get("mensaje").toString().contains("Connection refused"), caida.toString());

        FakeCmFuente otra = new FakeCmFuente();
        otra.tabla("CLIENTES", "id", "nombre");
        base = new BaseCmFicticia(otra, x -> x, "public");
        Map<String, Object> sinCm = servicio.probarSinGuardar(conexion("pg", MotorCm.POSTGRESQL,
                "jdbc:postgresql://cm:5432/otra", "x"));
        assertEquals(Boolean.FALSE, sinCm.get("ok"));
        assertTrue(sinCm.get("mensaje").toString().contains("no tiene la estructura"));
    }

    @Test
    void validaMotorUrlYPropiedadesPeligrosas() {
        assertThrows(IllegalArgumentException.class,
                () -> servicio.crear(conexion("a", MotorCm.ORACLE, "jdbc:mysql://x/cm", "")));
        assertThrows(IllegalArgumentException.class,
                () -> servicio.crear(conexion("a", MotorCm.SQLITE, "jdbc:sqlite:/etc/passwd", "")));
        assertThrows(IllegalArgumentException.class,
                () -> servicio.crear(conexion("a", null, "jdbc:mysql://x/cm", "")));
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> servicio.crear(
                conexion("a", MotorCm.MYSQL, "jdbc:mysql://x/cm?autoDeserialize=true&queryInterceptors=x", "")));
        assertTrue(ex.getMessage().contains("autodeserialize"));
        assertThrows(IllegalArgumentException.class, () -> servicio.crear(
                conexion("a", MotorCm.MYSQL, "jdbc:mysql://x/cm?allowLoadLocalInfile=true", "")));
        assertThrows(IllegalArgumentException.class, () -> servicio.crear(
                conexion("a", MotorCm.POSTGRESQL, "jdbc:postgresql://x/cm?socketFactory=a.B", "")));
        assertThrows(IllegalArgumentException.class, () -> servicio.crear(conexion("", MotorCm.MYSQL, "jdbc:mysql://x/cm", "")));
        assertTrue(urlsAbiertas.isEmpty(), "nunca se intenta conectar con datos invalidos");

        servicio.crear(conexion("SQL Server", MotorCm.SQLSERVER,
                "jdbc:sqlserver://x:1433;databaseName=cm;encrypt=true;trustServerCertificate=true", null));
        servicio.crear(conexion("Oracle", MotorCm.ORACLE, "jdbc:oracle:thin:@//x:1521/CM", null));
        assertThrows(IllegalArgumentException.class,
                () -> servicio.crear(conexion("Oracle", MotorCm.ORACLE, "jdbc:oracle:thin:@//y:1521/CM", null)),
                "nombre repetido");
        assertFalse(guardadas.get(1L).isTieneContrasena());
    }

    @Test
    void cifradorDetectaUnaClaveDistinta() {
        CifradorSecretos a = new CifradorSecretos("uno");
        String c1 = a.cifrar("hola");
        assertNotEquals(c1, a.cifrar("hola"), "IV aleatorio");
        assertEquals("hola", a.descifrar(c1));
        assertThrows(IllegalStateException.class, () -> new CifradorSecretos("dos").descifrar(c1));
    }
}
