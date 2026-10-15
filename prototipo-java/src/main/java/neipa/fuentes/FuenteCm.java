package neipa.fuentes;

import neipa.estructuras.ListaEnlazada;
import neipa.excepciones.ExtraccionException;

/**
 * Fuente de datos de CM (configuration management) de la red: de donde se
 * leen los objetos gestionados y los valores vigentes de sus parametros.
 *
 * Es una clase abstracta: define que sabe hacer cualquier fuente (leer los
 * objetos de una clase, describirse, cerrarse) y deja a cada subclase el como
 * (un XML RAML exportado de NetAct, archivos CSV o una base de datos).
 *
 * leerObjetos() es un metodo plantilla: valida la clase, abre la fuente una
 * sola vez, delega la lectura en leerFilas() de la subclase y lleva la cuenta
 * de objetos leidos. Las subclases no pueden saltearse esos pasos porque el
 * metodo es final.
 */
public abstract class FuenteCm {

    private final String origen;
    private boolean abierta;
    private int objetosLeidos;

    protected FuenteCm(String origen) {
        if (origen == null || origen.trim().isEmpty()) {
            throw new IllegalArgumentException("La fuente de CM necesita un origen.");
        }
        this.origen = origen.trim();
    }

    /** Objetos de una clase de CM (LNBTS, LNCEL, CADPR...). Lista vacia si no hay. */
    public final ListaEnlazada<ObjetoCm> leerObjetos(String clase) throws ExtraccionException {
        if (clase == null || clase.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "Indique la clase de CM a leer.");
        }
        if (!abierta) {
            abrir();
            abierta = true;
        }
        ListaEnlazada<ObjetoCm> objetos =
                leerFilas(clase.trim().toUpperCase());
        objetosLeidos += objetos.tamanio();
        return objetos;
    }

    /** Lectura propia de cada tipo de fuente. */
    protected abstract ListaEnlazada<ObjetoCm> leerFilas(String clase) throws ExtraccionException;

    /** Tipo de fuente, para mostrar al usuario. */
    public abstract String getTipo();

    /** Preparacion previa a la primera lectura (por defecto, nada). */
    protected void abrir() throws ExtraccionException {
    }

    /** Libera lo que la fuente tenga abierto (por defecto, nada). */
    public void cerrar() {
    }

    /** Descripcion para el usuario; una subclase puede ocultar datos sensibles. */
    public String describir() {
        return getTipo() + ": " + origen;
    }

    protected String getOrigen() {
        return origen;
    }

    public int getObjetosLeidos() {
        return objetosLeidos;
    }

    /** Crea un objeto de CM; las subclases cargan sus valores con agregarValor. */
    protected static ObjetoCm nuevoObjeto(String clase, String distName) {
        return new ObjetoCm(clase, distName);
    }

    protected static void agregarValor(ObjetoCm objeto, String parametro, String valor) {
        objeto.agregarValor(parametro, valor);
    }
}
