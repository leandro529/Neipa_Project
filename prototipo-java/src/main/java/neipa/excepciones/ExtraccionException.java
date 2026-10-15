package neipa.excepciones;

/**
 * Se lanza cuando no se pueden obtener los parametros vigentes de un
 * elemento desde la plataforma de gestion de su fabricante (CU001,
 * apartado Excepciones).
 */
public class ExtraccionException extends NeipaException {

    private static final long serialVersionUID = 1L;

    public ExtraccionException(String mensaje) {
        super(mensaje);
    }
}
