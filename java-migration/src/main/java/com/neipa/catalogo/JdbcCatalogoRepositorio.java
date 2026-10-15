package com.neipa.catalogo;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/** Catalogo de parametros sobre MySQL (insercion por lotes). */
@Repository
public class JdbcCatalogoRepositorio implements CatalogoRepositorio {

    private static final int LOTE = 2000;
    private static final String COLUMNAS = "fuente, release_doc, tecnologia, clase, ruta_clase, parametro, estructura,"
            + " nombre, tipo, unidad, rango, minimo_interno, maximo_interno, enumeracion, formula, default_valor,"
            + " default_interno, modificacion, funciones, relacionados, relaciones, descripcion";

    private final JdbcTemplate jdbc;

    public JdbcCatalogoRepositorio(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    @Transactional
    public int reemplazar(List<ParametroCatalogo> parametros) {
        jdbc.update("DELETE FROM catalogo_parametros");
        String sql = "INSERT INTO catalogo_parametros (" + COLUMNAS + ") VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
        for (int desde = 0; desde < parametros.size(); desde += LOTE) {
            List<ParametroCatalogo> lote = parametros.subList(desde, Math.min(parametros.size(), desde + LOTE));
            jdbc.batchUpdate(sql, lote, lote.size(), this::cargar);
        }
        return parametros.size();
    }

    private void cargar(PreparedStatement ps, ParametroCatalogo p) throws SQLException {
        int i = 1;
        ps.setString(i++, corto(p.getFuente(), 120));
        ps.setString(i++, corto(p.getRelease(), 40));
        ps.setString(i++, corto(p.getTecnologia(), 20));
        ps.setString(i++, corto(p.getClase(), 60));
        ps.setString(i++, corto(p.getRutaClase(), 200));
        ps.setString(i++, corto(p.getParametro(), 120));
        ps.setString(i++, corto(p.getEstructura(), 120));
        ps.setString(i++, corto(p.getNombre(), 255));
        ps.setString(i++, corto(p.getTipo(), 40));
        ps.setString(i++, corto(p.getUnidad(), 40));
        ps.setString(i++, p.getRango());
        numero(ps, i++, p.getMinimoInterno());
        numero(ps, i++, p.getMaximoInterno());
        ps.setString(i++, p.getEnumeracion());
        ps.setString(i++, corto(p.getFormula(), 255));
        ps.setString(i++, corto(p.getDefaultValor(), 400));
        ps.setString(i++, corto(p.getDefaultInterno(), 60));
        ps.setString(i++, corto(p.getModificacion(), 60));
        ps.setString(i++, p.getFunciones());
        ps.setString(i++, p.getRelacionados());
        ps.setString(i++, p.getRelaciones());
        ps.setString(i, p.getDescripcion());
    }

    private static void numero(PreparedStatement ps, int i, Double v) throws SQLException {
        if (v == null) {
            ps.setNull(i, Types.DOUBLE);
        } else {
            ps.setDouble(i, v);
        }
    }

    static String corto(String v, int max) {
        return v == null || v.length() <= max ? v : v.substring(0, max);
    }

    @Override
    public List<ParametroCatalogo> de(String clase, String parametro) {
        return jdbc.query("SELECT * FROM catalogo_parametros WHERE parametro = ? AND clase = ? ORDER BY fuente",
                this::mapear, parametro, clase);
    }

    @Override
    public List<ParametroCatalogo> buscar(String texto, String clase, int limite) {
        StringBuilder sql = new StringBuilder("SELECT * FROM catalogo_parametros WHERE 1=1");
        List<Object> args = new ArrayList<>();
        if (texto != null && !texto.trim().isEmpty()) {
            sql.append(" AND (parametro LIKE ? OR nombre LIKE ?)");
            String patron = "%" + texto.trim() + "%";
            args.add(patron);
            args.add(patron);
        }
        if (clase != null && !clase.trim().isEmpty()) {
            sql.append(" AND clase = ?");
            args.add(clase.trim().toUpperCase());
        }
        sql.append(" ORDER BY (parametro = ?) DESC, parametro, clase, fuente LIMIT ?");
        args.add(texto == null ? "" : texto.trim());
        args.add(Math.max(1, Math.min(limite, 500)));
        return jdbc.query(sql.toString(), this::mapear, args.toArray());
    }

    @Override
    public List<Map<String, Object>> clases() {
        return jdbc.queryForList("SELECT clase, MIN(ruta_clase) AS ruta, COUNT(DISTINCT parametro) AS parametros"
                + " FROM catalogo_parametros GROUP BY clase ORDER BY clase");
    }

    @Override
    public List<Map<String, Object>> resumen() {
        return jdbc.queryForList("SELECT fuente, release_doc AS releaseDoc, COUNT(*) AS parametros,"
                + " COUNT(DISTINCT clase) AS clases FROM catalogo_parametros GROUP BY fuente, release_doc ORDER BY fuente");
    }

    private ParametroCatalogo mapear(ResultSet rs, int fila) throws SQLException {
        ParametroCatalogo p = new ParametroCatalogo();
        p.setIdCatalogo(rs.getLong("id_catalogo"));
        p.setFuente(rs.getString("fuente"));
        p.setRelease(rs.getString("release_doc"));
        p.setTecnologia(rs.getString("tecnologia"));
        p.setClase(rs.getString("clase"));
        p.setRutaClase(rs.getString("ruta_clase"));
        p.setParametro(rs.getString("parametro"));
        p.setEstructura(rs.getString("estructura"));
        p.setNombre(rs.getString("nombre"));
        p.setTipo(rs.getString("tipo"));
        p.setUnidad(rs.getString("unidad"));
        p.setRango(rs.getString("rango"));
        double mn = rs.getDouble("minimo_interno");
        p.setMinimoInterno(rs.wasNull() ? null : mn);
        double mx = rs.getDouble("maximo_interno");
        p.setMaximoInterno(rs.wasNull() ? null : mx);
        p.setEnumeracion(rs.getString("enumeracion"));
        p.setFormula(rs.getString("formula"));
        p.setDefaultValor(rs.getString("default_valor"));
        p.setDefaultInterno(rs.getString("default_interno"));
        p.setModificacion(rs.getString("modificacion"));
        p.setFunciones(rs.getString("funciones"));
        p.setRelacionados(rs.getString("relacionados"));
        p.setRelaciones(rs.getString("relaciones"));
        p.setDescripcion(rs.getString("descripcion"));
        return p;
    }
}
