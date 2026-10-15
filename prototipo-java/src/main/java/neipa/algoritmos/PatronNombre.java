package neipa.algoritmos;

/**
 * Compara un nombre contra un patron con comodines, sin distinguir
 * mayusculas: '*' representa cualquier texto (incluso vacio) y '?' un solo
 * caracter. El resto de los caracteres (incluido '_') es literal. Ejemplo:
 * "*_test_*" coincide con "Lab_test_003" y no con "Lab_SITE_001".
 *
 * Algoritmo de dos punteros con vuelta atras: recorre el nombre y el patron
 * a la vez; cuando encuentra un '*' recuerda la posicion y, si mas adelante
 * algo no coincide, vuelve a ese '*' y le hace "absorber" un caracter mas.
 * Es O(n*m) en el peor caso y no usa expresiones regulares.
 */
public final class PatronNombre {

    private PatronNombre() {
        // Clase utilitaria: no se crean instancias.
    }

    public static boolean coincide(String nombre, String patron) {
        if (nombre == null || patron == null) {
            return false;
        }
        String texto = nombre.trim().toUpperCase();
        String comodin = patron.trim().toUpperCase();

        int i = 0;              // posicion en el texto
        int j = 0;              // posicion en el patron
        int ultimoAsterisco = -1;
        int marcaEnTexto = 0;

        while (i < texto.length()) {
            if (j < comodin.length()
                    && (comodin.charAt(j) == '?' || comodin.charAt(j) == texto.charAt(i))) {
                i++;
                j++;
            } else if (j < comodin.length() && comodin.charAt(j) == '*') {
                ultimoAsterisco = j;
                marcaEnTexto = i;
                j++;
            } else if (ultimoAsterisco != -1) {
                // Volver al ultimo '*' y que absorba un caracter mas.
                j = ultimoAsterisco + 1;
                marcaEnTexto++;
                i = marcaEnTexto;
            } else {
                return false;
            }
        }
        // Lo que sobra del patron solo puede ser '*'.
        while (j < comodin.length() && comodin.charAt(j) == '*') {
            j++;
        }
        return j == comodin.length();
    }
}
