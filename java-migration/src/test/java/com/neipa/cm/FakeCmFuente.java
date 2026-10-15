package com.neipa.cm;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/** Dump de CM en memoria, con la misma estructura que el SQLite de NetAct. */
public class FakeCmFuente implements CmFuente {

    final Map<String, List<String>> columnas = new LinkedHashMap<>();
    final Map<String, List<Map<String, Object>>> filas = new LinkedHashMap<>();
    final List<String> tablasLeidas = new ArrayList<>();

    public void tabla(String nombre, String... cols) {
        List<String> c = new ArrayList<>();
        for (String x : cols) {
            c.add(x);
        }
        columnas.put(nombre, c);
        filas.put(nombre, new ArrayList<>());
    }

    public void fila(String tabla, Object... valores) {
        Map<String, Object> f = new LinkedHashMap<>();
        List<String> c = columnas.get(tabla);
        for (int i = 0; i < valores.length; i++) {
            f.put(c.get(i), valores[i]);
        }
        filas.get(tabla).add(f);
    }

    @Override
    public List<String> tablas() {
        return new ArrayList<>(columnas.keySet());
    }

    @Override
    public List<String> columnas(String tabla) {
        return columnas.get(tabla);
    }

    @Override
    public void recorrer(String tabla, List<String> pedidas, Consumer<Map<String, Object>> consumidor) {
        tablasLeidas.add(tabla);
        for (Map<String, Object> f : filas.get(tabla)) {
            Map<String, Object> salida = new LinkedHashMap<>();
            for (String p : pedidas) {
                if (!columnas.get(tabla).contains(p)) {
                    throw new IllegalStateException("columna inexistente " + p);
                }
                salida.put(p, f.get(p));
            }
            consumidor.accept(salida);
        }
    }

    @Override
    public void close() {
    }
}
