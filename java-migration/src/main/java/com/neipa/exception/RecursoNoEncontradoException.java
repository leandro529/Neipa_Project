package com.neipa.exception;

/**
 * Lanzada por los controllers REST cuando se pide por id un recurso
 * (Segmento, ElementoDeRed, ValorEstandar, Excepcion, etc.) que no existe.
 * GlobalExceptionHandler la traduce a un 404 con cuerpo JSON.
 */
public class RecursoNoEncontradoException extends RuntimeException {

    public RecursoNoEncontradoException(String mensaje) {
        super(mensaje);
    }
}
