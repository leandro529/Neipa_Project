package neipa.modelo;

import neipa.algoritmos.PatronNombre;

/** Se cumple si el nombre del elemento coincide con un patron (ej. "*_URB_*"). */
public class CondicionPorNombre extends Condicion {

    private final String patron;

    public CondicionPorNombre(String patron) {
        if (patron == null || patron.trim().isEmpty()) {
            throw new IllegalArgumentException("La condicion por nombre necesita un patron.");
        }
        this.patron = patron.trim();
    }

    @Override
    public boolean seCumple(ElementoDeRed elemento) {
        return PatronNombre.coincide(elemento.getNombre(), patron);
    }

    @Override
    public String describir() {
        return "nombre como " + patron;
    }
}
