package neipa.algoritmos;

import java.util.Comparator;

import neipa.modelo.ElementoDeRed;

/** Criterio de orden alfabetico por nombre del elemento (sin distinguir mayusculas). */
public class ComparadorPorNombre implements Comparator<ElementoDeRed> {

    @Override
    public int compare(ElementoDeRed a, ElementoDeRed b) {
        return a.getNombre().compareToIgnoreCase(b.getNombre());
    }
}
