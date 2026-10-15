package com.neipa.web;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import com.neipa.exception.RecursoNoEncontradoException;
import com.neipa.web.dto.ErrorResponse;

/**
 * Traduce las excepciones de negocio a respuestas HTTP con un cuerpo JSON
 * simple, en vez del default de Spring (whitelabel error page / stacktrace),
 * para que el frontend React pueda mostrar mensajes de error legibles.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<ErrorResponse> manejarNoEncontrado(RecursoNoEncontradoException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> manejarArgumentoInvalido(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(new ErrorResponse(ex.getMessage()));
    }

    /**
     * Borrar algo que otra tabla referencia (ej. una excepcion que ya se
     * aplico en auditorias, o un segmento con elementos) viola una clave
     * foranea: se responde 409 con un mensaje claro en vez de un 500.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> manejarEnUso(DataIntegrityViolationException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse(
                "No se puede borrar o modificar: el registro esta referenciado por otros datos "
                        + "(por ejemplo, una excepcion ya aplicada en auditorias). "
                        + "Para dejar de usarlo, desactivelo."));
    }

    @ExceptionHandler(UnsupportedOperationException.class)
    public ResponseEntity<ErrorResponse> manejarNoSoportado(UnsupportedOperationException ex) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(new ErrorResponse(ex.getMessage()));
    }
}
