package neipa.estructuras;

/**
 * Nodo de una estructura enlazada simple: guarda un dato y la referencia
 * al nodo siguiente. Solo es visible dentro del paquete de estructuras.
 */
class Nodo<T> {

    private T dato;
    private Nodo<T> siguiente;

    Nodo(T dato) {
        this.dato = dato;
        this.siguiente = null;
    }

    T getDato() {
        return dato;
    }

    Nodo<T> getSiguiente() {
        return siguiente;
    }

    void setSiguiente(Nodo<T> siguiente) {
        this.siguiente = siguiente;
    }
}
