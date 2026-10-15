package neipa.estructuras;

/**
 * Excepcion no verificada que se lanza al intentar sacar un dato de una
 * lista, pila o cola que no tiene elementos.
 */
public class EstructuraVaciaException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public EstructuraVaciaException(String mensaje) {
        super(mensaje);
    }
}
