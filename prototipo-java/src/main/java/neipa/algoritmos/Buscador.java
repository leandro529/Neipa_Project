package neipa.algoritmos;

import neipa.estructuras.ListaEnlazada;
import neipa.modelo.ElementoDeRed;

/**
 * Algoritmos de busqueda sobre los elementos de red. Guarda la cantidad de
 * comparaciones de la ultima busqueda para poder mostrarla en pantalla.
 */
public class Buscador {

    private int comparaciones;

    /**
     * Busqueda binaria por nombre exacto. El arreglo debe estar ordenado por
     * nombre. Devuelve la posicion encontrada o -1 si el nombre no existe.
     */
    public int busquedaBinaria(ElementoDeRed[] ordenados, String nombre) {
        comparaciones = 0;
        int inicio = 0;
        int fin = ordenados.length - 1;
        while (inicio <= fin) {
            int medio = (inicio + fin) / 2;
            comparaciones++;
            int diferencia = ordenados[medio].getNombre().compareToIgnoreCase(nombre);
            if (diferencia == 0) {
                return medio;
            } else if (diferencia < 0) {
                inicio = medio + 1;
            } else {
                fin = medio - 1;
            }
        }
        return -1;
    }

    /** Busqueda secuencial: devuelve los elementos cuyo nombre contiene el texto. */
    public ListaEnlazada<ElementoDeRed> busquedaSecuencial(ListaEnlazada<ElementoDeRed> elementos,
            String texto) {
        comparaciones = 0;
        ListaEnlazada<ElementoDeRed> coincidencias = new ListaEnlazada<ElementoDeRed>();
        String buscado = texto.toLowerCase();
        for (ElementoDeRed elemento : elementos) {
            comparaciones++;
            if (elemento.getNombre().toLowerCase().contains(buscado)) {
                coincidencias.agregarAlFinal(elemento);
            }
        }
        return coincidencias;
    }

    public int getComparaciones() {
        return comparaciones;
    }
}
