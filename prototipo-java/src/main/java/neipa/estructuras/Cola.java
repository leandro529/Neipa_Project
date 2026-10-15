package neipa.estructuras;

/**
 * Cola (FIFO) construida sobre la lista enlazada propia: se encola por el
 * final y se desencola por el inicio. Se utiliza para procesar en orden de
 * llegada los elementos de red pendientes de auditar.
 */
public class Cola<T> {

    private final ListaEnlazada<T> datos = new ListaEnlazada<T>();

    public void encolar(T dato) {
        datos.agregarAlFinal(dato);
    }

    public T desencolar() {
        if (estaVacia()) {
            throw new EstructuraVaciaException("La cola no tiene elementos.");
        }
        return datos.eliminarPrimero();
    }

    public T verFrente() {
        if (estaVacia()) {
            throw new EstructuraVaciaException("La cola no tiene elementos.");
        }
        return datos.verPrimero();
    }

    public boolean estaVacia() {
        return datos.estaVacia();
    }

    public int tamanio() {
        return datos.tamanio();
    }
}
