package com.neipa.domain;

/**
 * Niveles de excepcion soportados (RF05-08: "excepciones de 3 niveles",
 * seccion 4 - Limites del sistema de la primera entrega). Una excepcion
 * puede aplicarse a:
 * <ul>
 *   <li>{@link #ELEMENTO}: un ElementoDeRed puntual (requiere idElemento).</li>
 *   <li>{@link #SEGMENTO}: todos los ElementoDeRed de un Segmento (requiere idSegmento).</li>
 *   <li>{@link #GLOBAL}: todos los elementos del sistema, sin importar segmento.</li>
 * </ul>
 * Equivalente Java del atributo "alcance" de la clase Excepcion definida en el
 * diagrama de dominio de la primera entrega.
 */
public enum AlcanceExcepcion {
    ELEMENTO,
    /**
     * Ampliacion: todos los elementos cuyo nombre coincide con un patron
     * (ej. "*_test_*"). '*' = cualquier texto, '?' = un caracter.
     */
    PATRON,
    SEGMENTO,
    GLOBAL
}
