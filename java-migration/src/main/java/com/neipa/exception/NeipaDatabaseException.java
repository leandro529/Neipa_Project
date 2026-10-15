package com.neipa.exception;

/**
 * Equivalente Java de include/exceptions/DatabaseException.py del repo original.
 */
public class NeipaDatabaseException extends RuntimeException {

    private final int errorCode;

    public NeipaDatabaseException(int errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public NeipaDatabaseException(String message) {
        this(0, message);
    }

    public int getErrorCode() {
        return errorCode;
    }
}
