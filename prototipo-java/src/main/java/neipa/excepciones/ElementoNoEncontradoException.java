package neipa.excepciones;

/** Se lanza cuando se pide un dato que no existe en el repositorio. */
public class ElementoNoEncontradoException extends NeipaException {

    private static final long serialVersionUID = 1L;

    public ElementoNoEncontradoException(String mensaje) {
        super(mensaje);
    }
}
