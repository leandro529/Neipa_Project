package com.neipa.cm.conexion;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

import com.neipa.cm.MotorCm;

/** Conexiones de CM sobre MySQL (tabla cm_conexiones). */
@Repository
public class JdbcCmConexionRepositorio implements CmConexionRepositorio {

    private final JdbcTemplate jdbc;

    public JdbcCmConexionRepositorio(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<CmConexion> listar() {
        return jdbc.query("SELECT * FROM cm_conexiones ORDER BY nombre", this::mapear);
    }

    @Override
    public Optional<CmConexion> buscar(long id) {
        return jdbc.query("SELECT * FROM cm_conexiones WHERE id_conexion = ?", this::mapear, id).stream().findFirst();
    }

    @Override
    public Optional<CmConexion> buscarPorNombre(String nombre) {
        return jdbc.query("SELECT * FROM cm_conexiones WHERE nombre = ?", this::mapear, nombre).stream().findFirst();
    }

    @Override
    public CmConexion guardar(CmConexion c) {
        if (c.getIdConexion() == null) {
            GeneratedKeyHolder clave = new GeneratedKeyHolder();
            LocalDateTime ahora = LocalDateTime.now();
            jdbc.update(con -> {
                PreparedStatement ps = con.prepareStatement("INSERT INTO cm_conexiones"
                        + " (nombre, motor, url, usuario, contrasena_cifrada, esquema, creada) VALUES (?,?,?,?,?,?,?)",
                        Statement.RETURN_GENERATED_KEYS);
                ps.setString(1, c.getNombre());
                ps.setString(2, c.getMotor().name());
                ps.setString(3, c.getUrl());
                ps.setString(4, c.getUsuario());
                ps.setString(5, c.getContrasenaCifrada());
                ps.setString(6, c.getEsquema());
                ps.setTimestamp(7, Timestamp.valueOf(ahora));
                return ps;
            }, clave);
            c.setIdConexion(clave.getKey().longValue());
            c.setCreada(ahora);
        } else {
            jdbc.update("UPDATE cm_conexiones SET nombre = ?, motor = ?, url = ?, usuario = ?, contrasena_cifrada = ?,"
                    + " esquema = ? WHERE id_conexion = ?", c.getNombre(), c.getMotor().name(), c.getUrl(),
                    c.getUsuario(), c.getContrasenaCifrada(), c.getEsquema(), c.getIdConexion());
        }
        c.setContrasena(null);
        return c;
    }

    @Override
    public void borrar(long id) {
        jdbc.update("DELETE FROM cm_conexiones WHERE id_conexion = ?", id);
    }

    @Override
    public void registrarPrueba(long id, String resultado) {
        jdbc.update("UPDATE cm_conexiones SET ultima_prueba = ?, ultimo_resultado = ? WHERE id_conexion = ?",
                Timestamp.valueOf(LocalDateTime.now()), resultado, id);
    }

    private CmConexion mapear(ResultSet rs, int fila) throws SQLException {
        CmConexion c = new CmConexion();
        c.setIdConexion(rs.getLong("id_conexion"));
        c.setNombre(rs.getString("nombre"));
        c.setMotor(MotorCm.valueOf(rs.getString("motor")));
        c.setUrl(rs.getString("url"));
        c.setUsuario(rs.getString("usuario"));
        c.setContrasenaCifrada(rs.getString("contrasena_cifrada"));
        c.setEsquema(rs.getString("esquema"));
        Timestamp creada = rs.getTimestamp("creada");
        c.setCreada(creada == null ? null : creada.toLocalDateTime());
        Timestamp prueba = rs.getTimestamp("ultima_prueba");
        c.setUltimaPrueba(prueba == null ? null : prueba.toLocalDateTime());
        c.setUltimoResultado(rs.getString("ultimo_resultado"));
        return c;
    }
}
