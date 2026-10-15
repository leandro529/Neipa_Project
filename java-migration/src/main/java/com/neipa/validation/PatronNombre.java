package com.neipa.validation;

import java.util.regex.Pattern;

/**
 * Coincidencia de nombres contra patrones con comodines, sin distinguir
 * mayusculas de minusculas.
 *
 * - {@link #coincide}: estilo archivo, '*' = cualquier texto (incluso vacio),
 *   '?' = un caracter; todo lo demas es literal (el '_' tambien). Lo usan las
 *   excepciones de alcance PATRON, ej "*_test_*".
 * - {@link #coincideLike}: ademas acepta los comodines de SQL LIKE ('%' y
 *   '_'), que es lo que ya usaba el operador COINCIDE.
 */
public final class PatronNombre {

    private PatronNombre() {
    }

    public static boolean coincide(String nombre, String patron) {
        if (nombre == null || patron == null) {
            return false;
        }
        return aRegex(patron.trim(), false).matcher(nombre.trim()).matches();
    }

    public static boolean coincideLike(String nombre, String patron) {
        if (nombre == null || patron == null) {
            return false;
        }
        return aRegex(patron.trim(), true).matcher(nombre.trim()).matches();
    }

    private static Pattern aRegex(String patron, boolean comodinesSql) {
        StringBuilder regex = new StringBuilder();
        StringBuilder literal = new StringBuilder();
        for (char c : patron.toCharArray()) {
            boolean cualquierTexto = c == '*' || (comodinesSql && c == '%');
            boolean unCaracter = c == '?' || (comodinesSql && c == '_');
            if (cualquierTexto || unCaracter) {
                if (literal.length() > 0) {
                    regex.append(Pattern.quote(literal.toString()));
                    literal.setLength(0);
                }
                regex.append(cualquierTexto ? ".*" : ".");
            } else {
                literal.append(c);
            }
        }
        if (literal.length() > 0) {
            regex.append(Pattern.quote(literal.toString()));
        }
        return Pattern.compile(regex.toString(), Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE | Pattern.DOTALL);
    }
}
