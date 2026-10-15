package neipa.estructuras;

/**
 * Pila (LIFO) construida sobre la lista enlazada propia: se apila y se
 * desapila siempre por el inicio. Se utiliza para guardar las correcciones
 * aplicadas, de modo que el rollback deshaga primero la ultima.
 */
public class Pila<T> {

    private final ListaEnlazada<T> datos = new ListaEnlazada<T>();

    public void apilar(T dato) {
        datos.agregarAlInicio(dato);
    }

    public T desapilar() {
        if (estaVacia()) {
            throw new EstructuraVaciaException("La pila no tiene elementos.");
        }
        return datos.eliminarPrimero();
    }

    public T verTope() {
        if (estaVacia()) {
            throw new EstructuraVaciaException("La pila no tiene elementos.");
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
