package neipa.modelo;

import neipa.estructuras.ListaEnlazada;

/**
 * Segmento o perfil de elementos de red (por ejemplo, urbano de alta
 * densidad). Los valores estandar se definen por segmento.
 */
public class Segmento {

    private final String nombre;
    private String criterio;

    public Segmento(String nombre, String criterio) {
        if (nombre == null || nombre.trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre del segmento es obligatorio.");
        }
        this.nombre = nombre.trim();
        this.criterio = criterio;
    }

    /** Devuelve los elementos del inventario que pertenecen a este segmento (RF09). */
    public ListaEnlazada<ElementoDeRed> agrupar(ListaEnlazada<ElementoDeRed> inventario) {
        ListaEnlazada<ElementoDeRed> grupo = new ListaEnlazada<ElementoDeRed>();
        for (ElementoDeRed elemento : inventario) {
            if (elemento.getSegmento() == this) {
                grupo.agregarAlFinal(elemento);
            }
        }
        return grupo;
    }

    public String getNombre() {
        return nombre;
    }

    public String getCriterio() {
        return criterio;
    }

    public void setCriterio(String criterio) {
        this.criterio = criterio;
    }

    @Override
    public String toString() {
        return nombre + " [" + criterio + "]";
    }
}
