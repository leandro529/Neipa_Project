package com.neipa.domain;

/**
 * Resultado posible de una Auditoria (RF10): conforme si el valor vigente cae
 * dentro del rango permitido definido para el parametro, desvio en caso
 * contrario (RF04) — salvo que exista una excepcion vigente que lo cubra
 * (RF05-08), en cuyo caso el resultado es exceptuado en lugar de desvio.
 */
public enum ResultadoAuditoria {
    CONFORME,
    DESVIO,
    EXCEPTUADO;

    @Override
    public String toString() {
        return name().toLowerCase();
    }
}
