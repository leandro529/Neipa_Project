package com.neipa.catalogo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Catalogo en memoria para los tests. */
@SuppressWarnings("unchecked")
public class MemoriaCatalogoRepositorio implements CatalogoRepositorio {

    public final List<ParametroCatalogo> parametros = new ArrayList<>();

    @Override
    public int reemplazar(List<ParametroCatalogo> nuevos) {
        parametros.clear();
        parametros.addAll(nuevos);
        return nuevos.size();
    }

    @Override
    public List<ParametroCatalogo> de(String clase, String parametro) {
        return parametros.stream().filter(p -> p.getClase().equalsIgnoreCase(clase) && p.getParametro().equals(parametro))
                .collect(Collectors.toList());
    }

    @Override
    public List<ParametroCatalogo> buscar(String texto, String clase, int limite) {
        return parametros.stream()
                .filter(p -> texto == null || p.getParametro().contains(texto))
                .filter(p -> clase == null || p.getClase().equalsIgnoreCase(clase))
                .limit(limite).collect(Collectors.toList());
    }

    @Override
    public List<Map<String, Object>> clases() {
        Map<String, Map<String, Object>> porClase = new java.util.TreeMap<>();
        for (ParametroCatalogo p : parametros) {
            Map<String, Object> c = porClase.computeIfAbsent(p.getClase(), k -> {
                Map<String, Object> m = new java.util.LinkedHashMap<>();
                m.put("clase", k);
                m.put("ruta", p.getRutaClase());
                m.put("parametros", new java.util.TreeSet<String>());
                return m;
            });
            ((java.util.Set<String>) c.get("parametros")).add(p.getParametro());
        }
        List<Map<String, Object>> lista = new java.util.ArrayList<>(porClase.values());
        for (Map<String, Object> c : lista) {
            c.put("parametros", ((java.util.Set<?>) c.get("parametros")).size());
        }
        return lista;
    }

    @Override
    public List<Map<String, Object>> resumen() {
        return Collections.emptyList();
    }
}
