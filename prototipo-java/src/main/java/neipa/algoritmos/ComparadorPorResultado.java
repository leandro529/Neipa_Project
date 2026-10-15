package neipa.algoritmos;

import java.util.Comparator;

import neipa.modelo.Auditoria;

/**
 * Criterio de orden del reporte tecnico: primero los desvios, despues los
 * exceptuados y al final los conformes; a igual resultado, por elemento.
 */
public class ComparadorPorResultado implements Comparator<Auditoria> {

    @Override
    public int compare(Auditoria a, Auditoria b) {
        int porResultado = a.getResultado().ordinal() - b.getResultado().ordinal();
        if (porResultado != 0) {
            return porResultado;
        }
        return a.getElementoDeRed().getNombre()
                .compareToIgnoreCase(b.getElementoDeRed().getNombre());
    }
}
