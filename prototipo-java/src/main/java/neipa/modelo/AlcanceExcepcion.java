package neipa.modelo;

/** Niveles en los que se puede registrar una excepcion (RF05, RF06 y RF07). */
public enum AlcanceExcepcion {
    ELEMENTO,
    SEGMENTO,
    PARAMETRO,
    /** Elementos cuyo nombre coincide con un patron, ej "*_test_*". */
    PATRON
}
