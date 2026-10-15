package neipa.algoritmos;

import java.util.Comparator;

/**
 * Algoritmo de ordenacion por insercion, generico: ordena un arreglo de
 * cualquier tipo segun el criterio (Comparator) que recibe.
 */
public final class Ordenador {

    private Ordenador() {
        // Clase utilitaria: no se crean instancias.
    }

    public static <T> void ordenarPorInsercion(T[] arreglo, Comparator<? super T> criterio) {
        for (int i = 1; i < arreglo.length; i++) {
            T actual = arreglo[i];
            int j = i - 1;
            // Corre a la derecha los mayores que el actual para abrirle lugar.
            while (j >= 0 && criterio.compare(arreglo[j], actual) > 0) {
                arreglo[j + 1] = arreglo[j];
                j--;
            }
            arreglo[j + 1] = actual;
        }
    }
}
