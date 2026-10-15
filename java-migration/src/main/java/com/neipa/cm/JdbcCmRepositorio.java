package com.neipa.cm;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import com.neipa.domain.Auditoria;

/** CmRepositorio sobre MySQL, con inserciones por lotes (JdbcTemplate). */
@Repository
public class JdbcCmRepositorio implements CmRepositorio {

    private static final int LOTE = 2000;

    private final JdbcTemplate jdbc;

    public JdbcCmRepositorio(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public long crearImportacion(String archivo) {
        KeyHolder clave = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO cm_importaciones (archivo, inicio, estado) VALUES (?, ?, 'EN_CURSO')",
                    Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, archivo);
            ps.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
            return ps;
        }, clave);
        return clave.getKey().longValue();
    }

    @Override
    public void actualizarImportacion(long id, String estado, String fechaDump, long objetos, long valores,
                                      String mensaje, boolean terminada) {
        jdbc.update("UPDATE cm_importaciones SET estado = ?, fecha_dump = ?, objetos = ?, valores = ?, mensaje = ?,"
                        + " fin = ? WHERE id_importacion = ?",
                estado, fechaDump, objetos, valores, mensaje,
                terminada ? Timestamp.valueOf(LocalDateTime.now()) : null, id);
    }

    @Override
    public List<Map<String, Object>> listarImportaciones() {
        return jdbc.queryForList("SELECT * FROM cm_importaciones ORDER BY id_importacion DESC");
    }

    @Override
    public void guardarObjetos(List<CmObjeto> objetos, long idImportacion) {
        String sql = "INSERT INTO cm_objetos (dist_name, clase, rc, mrbts_id, lnbts_id, lncel_id, nombre, version,"
                + " id_importacion) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE clase = VALUES(clase),"
                + " rc = VALUES(rc), nombre = VALUES(nombre), version = VALUES(version),"
                + " id_importacion = VALUES(id_importacion)";
        for (int i = 0; i < objetos.size(); i += LOTE) {
            List<CmObjeto> lote = objetos.subList(i, Math.min(i + LOTE, objetos.size()));
            jdbc.batchUpdate(sql, lote, lote.size(), (ps, o) -> {
                ps.setString(1, o.getDistName());
                ps.setString(2, o.getClase());
                ps.setString(3, o.getRc());
                setEntero(ps, 4, o.getMrbtsId());
                setEntero(ps, 5, o.getLnbtsId());
                setEntero(ps, 6, o.getLncelId());
                ps.setString(7, recortar(o.getNombre(), 120));
                ps.setString(8, recortar(o.getVersion(), 60));
                ps.setLong(9, idImportacion);
            });
        }
    }

    @Override
    public void borrarObjetosAnteriores(String clase, long idImportacion) {
        jdbc.update("DELETE v FROM cm_valores v JOIN cm_objetos o ON o.dist_name = v.dist_name"
                + " WHERE o.clase = ? AND o.id_importacion <> ?", clase, idImportacion);
        jdbc.update("DELETE FROM cm_objetos WHERE clase = ? AND id_importacion <> ?", clase, idImportacion);
    }

    @Override
    public void borrarValores(String clase, Collection<String> parametros) {
        for (String parametro : parametros) {
            jdbc.update("DELETE FROM cm_valores WHERE clase = ? AND parametro = ?", clase, parametro);
        }
    }

    @Override
    public void guardarValores(List<CmObjeto> objetos) {
        List<Object[]> filas = new ArrayList<>();
        for (CmObjeto o : objetos) {
            for (Map.Entry<String, String> v : o.getValores().entrySet()) {
                filas.add(new Object[] {o.getDistName(), v.getKey(), o.getClase(), o.getRc(), recortar(v.getValue(), 255)});
            }
        }
        String sql = "INSERT INTO cm_valores (dist_name, parametro, clase, rc, valor) VALUES (?, ?, ?, ?, ?)"
                + " ON DUPLICATE KEY UPDATE valor = VALUES(valor)";
        for (int i = 0; i < filas.size(); i += LOTE) {
            jdbc.batchUpdate(sql, filas.subList(i, Math.min(i + LOTE, filas.size())));
        }
    }

    @Override
    public List<CmObjeto> objetos(String clase, String rc, Collection<String> parametros) {
        Map<String, CmObjeto> porDistName = new LinkedHashMap<>();
        jdbc.query("SELECT dist_name, clase, rc, mrbts_id, lnbts_id, lncel_id, nombre, version FROM cm_objetos"
                        + " WHERE clase = ? AND rc = ?",
                rs -> {
                    porDistName.put(rs.getString(1), new CmObjeto(rs.getString(1), rs.getString(2), rs.getString(3),
                            (Integer) rs.getObject(4), (Integer) rs.getObject(5), (Integer) rs.getObject(6),
                            rs.getString(7), rs.getString(8)));
                }, clase, rc);
        for (String parametro : parametros) {
            jdbc.query("SELECT dist_name, valor FROM cm_valores WHERE clase = ? AND rc = ? AND parametro = ?",
                    rs -> {
                        CmObjeto o = porDistName.get(rs.getString(1));
                        if (o != null && rs.getString(2) != null) {
                            o.getValores().put(parametro, rs.getString(2));
                        }
                    }, clase, rc, parametro);
        }
        return new ArrayList<>(porDistName.values());
    }

    @Override
    public List<Map<String, Object>> resumenObjetos() {
        return jdbc.queryForList("SELECT clase, rc, COUNT(*) AS objetos FROM cm_objetos GROUP BY clase, rc"
                + " ORDER BY clase, rc");
    }

    @Override
    public void guardarAuditorias(List<Auditoria> auditorias) {
        String sql = "INSERT INTO auditorias (id_elemento, parametro, valor_vigente, valor_estandar, resultado, fecha,"
                + " id_excepcion_aplicada, id_valor_estandar, regla_aplicada, valor_objetivo, id_corrida, dist_name,"
                + " nombre_objeto) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        for (int i = 0; i < auditorias.size(); i += LOTE) {
            List<Auditoria> lote = auditorias.subList(i, Math.min(i + LOTE, auditorias.size()));
            jdbc.batchUpdate(sql, lote, lote.size(), (ps, a) -> {
                setLargo(ps, 1, a.getIdElemento());
                ps.setString(2, a.getParametro());
                ps.setString(3, recortar(a.getValorVigente(), 30));
                ps.setString(4, recortar(a.getValorEstandar(), 120));
                ps.setString(5, a.getResultado());
                ps.setTimestamp(6, Timestamp.valueOf(a.getFecha()));
                setLargo(ps, 7, a.getIdExcepcionAplicada());
                setLargo(ps, 8, a.getIdValorEstandar());
                ps.setString(9, recortar(a.getReglaAplicada(), 255));
                ps.setString(10, recortar(a.getValorObjetivo(), 120));
                setLargo(ps, 11, a.getIdCorrida());
                ps.setString(12, a.getDistName());
                ps.setString(13, recortar(a.getNombreObjeto(), 120));
            });
        }
    }

    @Override
    public void guardarResumen(long idCorrida, Collection<ResumenRegla> resumen) {
        List<ResumenRegla> lista = new ArrayList<>(resumen);
        jdbc.batchUpdate("INSERT INTO corrida_resumen (id_corrida, id_valor_estandar, clase, parametro, conformes,"
                        + " desvios, exceptuados) VALUES (?, ?, ?, ?, ?, ?, ?)", lista, Math.max(1, lista.size()),
                (ps, r) -> {
                    ps.setLong(1, idCorrida);
                    ps.setLong(2, r.getIdValorEstandar());
                    ps.setString(3, r.getClase());
                    ps.setString(4, r.getParametro());
                    ps.setLong(5, r.getConformes());
                    ps.setLong(6, r.getDesvios());
                    ps.setLong(7, r.getExceptuados());
                });
    }

    @Override
    public List<ResumenRegla> resumenDeCorrida(long idCorrida) {
        return jdbc.query("SELECT id_valor_estandar, clase, parametro, conformes, desvios, exceptuados"
                        + " FROM corrida_resumen WHERE id_corrida = ? ORDER BY clase, parametro",
                (rs, n) -> new ResumenRegla(rs.getLong(1), rs.getString(2), rs.getString(3),
                        rs.getLong(4), rs.getLong(5), rs.getLong(6)), idCorrida);
    }

    private static void setEntero(PreparedStatement ps, int i, Integer v) throws java.sql.SQLException {
        if (v == null) {
            ps.setNull(i, Types.INTEGER);
        } else {
            ps.setInt(i, v);
        }
    }

    private static void setLargo(PreparedStatement ps, int i, Long v) throws java.sql.SQLException {
        if (v == null) {
            ps.setNull(i, Types.BIGINT);
        } else {
            ps.setLong(i, v);
        }
    }

    static String recortar(String texto, int maximo) {
        if (texto == null || texto.length() <= maximo) {
            return texto;
        }
        return texto.substring(0, maximo);
    }
}
