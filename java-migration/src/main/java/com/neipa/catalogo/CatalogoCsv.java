package com.neipa.catalogo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PushbackReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Lee el CSV que genera herramientas/convertir_diccionarios_nokia.py
 * (RFC 4180: separador coma, comillas dobles, saltos de linea dentro de
 * un campo entre comillas). La primera fila trae los nombres de columna.
 */
public final class CatalogoCsv {

    private CatalogoCsv() {
    }

    public static int leer(Reader lector, Consumer<ParametroCatalogo> destino) throws IOException {
        PushbackReader entrada = new PushbackReader(lector instanceof BufferedReader ? lector
                : new BufferedReader(lector, 1 << 16), 1);
        List<String> cabecera = null;
        int filas = 0;
        List<String> campos;
        while ((campos = siguienteFila(entrada)) != null) {
            if (cabecera == null) {
                cabecera = campos;
                if (!cabecera.isEmpty() && cabecera.get(0).startsWith("﻿")) {
                    cabecera.set(0, cabecera.get(0).substring(1));
                }
                if (!cabecera.contains("clase") || !cabecera.contains("parametro")) {
                    throw new IllegalArgumentException(
                            "El CSV no es un catalogo de parametros (faltan las columnas clase y parametro)");
                }
                continue;
            }
            if (campos.size() == 1 && campos.get(0).isEmpty()) {
                continue;
            }
            Map<String, String> fila = new HashMap<>();
            for (int i = 0; i < cabecera.size() && i < campos.size(); i++) {
                fila.put(cabecera.get(i), campos.get(i));
            }
            ParametroCatalogo p = desdeFila(fila);
            if (p.getClase() != null && p.getParametro() != null) {
                destino.accept(p);
                filas++;
            }
        }
        return filas;
    }

    static ParametroCatalogo desdeFila(Map<String, String> f) {
        ParametroCatalogo p = new ParametroCatalogo();
        p.setFuente(texto(f.get("fuente")));
        p.setRelease(texto(f.get("release")));
        p.setTecnologia(texto(f.get("tecnologia")));
        p.setClase(texto(f.get("clase")));
        p.setRutaClase(texto(f.get("ruta_clase")));
        p.setParametro(texto(f.get("parametro")));
        p.setEstructura(texto(f.get("estructura")));
        p.setNombre(texto(f.get("nombre")));
        p.setTipo(texto(f.get("tipo")));
        p.setUnidad(texto(f.get("unidad")));
        p.setRango(texto(f.get("rango")));
        p.setMinimoInterno(numero(f.get("minimo_interno")));
        p.setMaximoInterno(numero(f.get("maximo_interno")));
        p.setEnumeracion(texto(f.get("enumeracion")));
        p.setFormula(texto(f.get("formula")));
        p.setDefaultValor(texto(f.get("default_valor")));
        p.setDefaultInterno(texto(f.get("default_interno")));
        p.setModificacion(texto(f.get("modificacion")));
        p.setFunciones(texto(f.get("funciones")));
        p.setRelacionados(texto(f.get("relacionados")));
        p.setRelaciones(texto(f.get("relaciones")));
        p.setDescripcion(texto(f.get("descripcion")));
        if (p.getFuente() == null) {
            p.setFuente("catalogo");
        }
        return p;
    }

    private static String texto(String v) {
        if (v == null) {
            return null;
        }
        String t = v.trim();
        return t.isEmpty() ? null : t;
    }

    private static Double numero(String v) {
        String t = texto(v);
        if (t == null) {
            return null;
        }
        try {
            return Double.valueOf(t);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    /** Lee una fila; null al final del archivo. */
    private static List<String> siguienteFila(PushbackReader in) throws IOException {
        List<String> campos = new ArrayList<>();
        StringBuilder actual = new StringBuilder();
        boolean comillas = false;
        boolean leyo = false;
        int c;
        while ((c = in.read()) != -1) {
            leyo = true;
            char ch = (char) c;
            if (comillas) {
                if (ch == '"') {
                    int sig = in.read();
                    if (sig == '"') {
                        actual.append('"');
                    } else {
                        comillas = false;
                        if (sig != -1) {
                            in.unread(sig);
                        }
                    }
                } else {
                    actual.append(ch);
                }
            } else if (ch == '"') {
                comillas = true;
            } else if (ch == ',') {
                campos.add(actual.toString());
                actual.setLength(0);
            } else if (ch == '\n') {
                campos.add(actual.toString());
                return campos;
            } else if (ch != '\r') {
                actual.append(ch);
            }
        }
        if (!leyo) {
            return null;
        }
        campos.add(actual.toString());
        return campos;
    }
}
