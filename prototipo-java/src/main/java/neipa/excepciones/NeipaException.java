package neipa.excepciones;

/**
 * Excepcion base (verificada) de las reglas de negocio de NEIPA. El resto
 * de las excepciones propias del sistema heredan de esta clase.
 */
public class NeipaException extends Exception {

    private static final long serialVersionUID = 1L;

    public NeipaException(String mensaje) {
        super(mensaje);
    }
}
