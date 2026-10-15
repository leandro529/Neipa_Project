package com.neipa.cm;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.neipa.domain.Auditoria;

/** CmRepositorio en memoria para los tests. */
class MemoriaCmRepositorio implements CmRepositorio {

    final Map<String, CmObjeto> objetos = new LinkedHashMap<>();
    final Map<String, Map<String, String>> valores = new HashMap<>(); // distName -> param -> valor
    final List<Auditoria> auditorias = new ArrayList<>();
    final Map<Long, List<ResumenRegla>> resumenes = new HashMap<>();
    final Map<Long, Map<String, Object>> importaciones = new LinkedHashMap<>();
    private long siguienteImportacion = 1;
    private long siguienteAuditoria = 1;

    @Override
    public long crearImportacion(String archivo) {
        long id = siguienteImportacion++;
        Map<String, Object> m = new HashMap<>();
        m.put("archivo", archivo);
        m.put("estado", "EN_CURSO");
        importaciones.put(id, m);
        return id;
    }

    @Override
    public void actualizarImportacion(long id, String estado, String fechaDump, long objetos, long valores,
                                      String mensaje, boolean terminada) {
        Map<String, Object> m = importaciones.get(id);
        m.put("estado", estado);
        m.put("fecha_dump", fechaDump);
        m.put("objetos", objetos);
        m.put("valores", valores);
        m.put("mensaje", mensaje);
    }

    @Override
    public List<Map<String, Object>> listarImportaciones() {
        return new ArrayList<>(importaciones.values());
    }

    @Override
    public void guardarObjetos(List<CmObjeto> lista, long idImportacion) {
        for (CmObjeto o : lista) {
            importacionDe.put(o.getDistName(), idImportacion);
            objetos.put(o.getDistName(), new CmObjeto(o.getDistName(), o.getClase(), o.getRc(), o.getMrbtsId(),
                    o.getLnbtsId(), o.getLncelId(), o.getNombre(), o.getVersion()));
        }
    }

    final Map<String, Long> importacionDe = new HashMap<>();

    @Override
    public void borrarObjetosAnteriores(String clase, long idImportacion) {
        objetos.values().removeIf(o -> o.getClase().equals(clase)
                && !Long.valueOf(idImportacion).equals(importacionDe.get(o.getDistName())));
        valores.keySet().removeIf(dn -> !objetos.containsKey(dn));
    }

    @Override
    public void borrarValores(String clase, Collection<String> parametros) {
        for (Map.Entry<String, Map<String, String>> e : valores.entrySet()) {
            CmObjeto o = objetos.get(e.getKey());
            if (o != null && o.getClase().equals(clase)) {
                e.getValue().keySet().removeAll(parametros);
            }
        }
    }

    @Override
    public void guardarValores(List<CmObjeto> lista) {
        for (CmObjeto o : lista) {
            valores.computeIfAbsent(o.getDistName(), k -> new HashMap<>()).putAll(o.getValores());
        }
    }

    void cambiarValor(String distName, String parametro, String valor) {
        valores.get(distName).put(parametro, valor);
    }

    @Override
    public List<CmObjeto> objetos(String clase, String rc, Collection<String> parametros) {
        List<CmObjeto> resultado = new ArrayList<>();
        for (CmObjeto o : objetos.values()) {
            if (o.getClase().equals(clase) && rc.equals(o.getRc())) {
                CmObjeto copia = new CmObjeto(o.getDistName(), o.getClase(), o.getRc(), o.getMrbtsId(),
                        o.getLnbtsId(), o.getLncelId(), o.getNombre(), o.getVersion());
                Map<String, String> v = valores.getOrDefault(o.getDistName(), new HashMap<>());
                for (String p : parametros) {
                    if (v.get(p) != null) {
                        copia.getValores().put(p, v.get(p));
                    }
                }
                resultado.add(copia);
            }
        }
        return resultado;
    }

    @Override
    public List<Map<String, Object>> resumenObjetos() {
        return new ArrayList<>();
    }

    @Override
    public void guardarAuditorias(List<Auditoria> lista) {
        for (Auditoria a : lista) {
            a.setIdAuditoria(siguienteAuditoria++);
            auditorias.add(a);
        }
    }

    @Override
    public void guardarResumen(long idCorrida, Collection<ResumenRegla> resumen) {
        resumenes.put(idCorrida, new ArrayList<>(resumen));
    }

    @Override
    public List<ResumenRegla> resumenDeCorrida(long idCorrida) {
        return resumenes.getOrDefault(idCorrida, new ArrayList<>());
    }
}
