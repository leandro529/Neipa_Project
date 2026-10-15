package neipa.excepciones;

/**
 * Se lanza cuando un valor no es compatible con el tipo de dato del
 * parametro (por ejemplo, texto en un parametro numerico). No es lo mismo
 * que un valor fuera de rango: ese caso es un desvio que detecta la
 * auditoria, no un error de carga.
 */
public class ValorInvalidoException extends NeipaException {

    private static final long serialVersionUID = 1L;

    public ValorInvalidoException(String mensaje) {
        super(mensaje);
    }
}
