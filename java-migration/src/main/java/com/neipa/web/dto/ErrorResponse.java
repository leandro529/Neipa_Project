package com.neipa.web.dto;

/**
 * Cuerpo JSON uniforme para los errores devueltos por la API REST
 * (ver GlobalExceptionHandler), para que el frontend React pueda mostrar
 * un mensaje sin tener que parsear el stacktrace por default de Spring.
 */
public class ErrorResponse {

    private final String mensaje;

    public ErrorResponse(String mensaje) {
        this.mensaje = mensaje;
    }

    public String getMensaje() {
        return mensaje;
    }
}
