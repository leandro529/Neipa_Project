package neipa.estructuras;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Lista enlazada simple de implementacion propia. Se utiliza para el
 * inventario de elementos de red, el catalogo de parametros y el historial
 * de auditoria. Implementa Iterable para poder recorrerla con for-each.
 */
public class ListaEnlazada<T> implements Iterable<T> {

    private Nodo<T> primero;
    private Nodo<T> ultimo;
    private int tamanio;

    public ListaEnlazada() {
        this.primero = null;
        this.ultimo = null;
        this.tamanio = 0;
    }

    public void agregarAlInicio(T dato) {
        Nodo<T> nuevo = new Nodo<T>(dato);
        if (estaVacia()) {
            primero = nuevo;
            ultimo = nuevo;
        } else {
            nuevo.setSiguiente(primero);
            primero = nuevo;
        }
        tamanio++;
    }

    public void agregarAlFinal(T dato) {
        Nodo<T> nuevo = new Nodo<T>(dato);
        if (estaVacia()) {
            primero = nuevo;
            ultimo = nuevo;
        } else {
            ultimo.setSiguiente(nuevo);
            ultimo = nuevo;
        }
        tamanio++;
    }

    public T eliminarPrimero() {
        if (estaVacia()) {
            throw new EstructuraVaciaException("La lista no tiene elementos.");
        }
        T dato = primero.getDato();
        primero = primero.getSiguiente();
        if (primero == null) {
            ultimo = null;
        }
        tamanio--;
        return dato;
    }

    public T verPrimero() {
        if (estaVacia()) {
            throw new EstructuraVaciaException("La lista no tiene elementos.");
        }
        return primero.getDato();
    }

    /** Devuelve el dato ubicado en la posicion indicada (la primera es 0). */
    public T obtener(int posicion) {
        if (posicion < 0 || posicion >= tamanio) {
            throw new IndexOutOfBoundsException("Posicion fuera de la lista: " + posicion);
        }
        Nodo<T> actual = primero;
        for (int i = 0; i < posicion; i++) {
            actual = actual.getSiguiente();
        }
        return actual.getDato();
    }

    /** Elimina la primera aparicion del dato. Devuelve true si lo encontro. */
    public boolean eliminar(T dato) {
        Nodo<T> anterior = null;
        Nodo<T> actual = primero;
        while (actual != null) {
            if (actual.getDato().equals(dato)) {
                if (anterior == null) {
                    primero = actual.getSiguiente();
                } else {
                    anterior.setSiguiente(actual.getSiguiente());
                }
                if (actual == ultimo) {
                    ultimo = anterior;
                }
                tamanio--;
                return true;
            }
            anterior = actual;
            actual = actual.getSiguiente();
        }
        return false;
    }

    /** Copia los datos de la lista en un arreglo ya creado por quien llama. */
    public void copiarEn(T[] destino) {
        if (destino.length < tamanio) {
            throw new IllegalArgumentException("El arreglo destino es mas chico que la lista.");
        }
        int i = 0;
        for (T dato : this) {
            destino[i] = dato;
            i++;
        }
    }

    public boolean estaVacia() {
        return tamanio == 0;
    }

    public int tamanio() {
        return tamanio;
    }

    @Override
    public Iterator<T> iterator() {
        return new Iterator<T>() {
            private Nodo<T> actual = primero;

            @Override
            public boolean hasNext() {
                return actual != null;
            }

            @Override
            public T next() {
                if (actual == null) {
                    throw new NoSuchElementException();
                }
                T dato = actual.getDato();
                actual = actual.getSiguiente();
                return dato;
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }
}
