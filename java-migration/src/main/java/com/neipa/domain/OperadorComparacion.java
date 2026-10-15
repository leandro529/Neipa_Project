package com.neipa.domain;

/**
 * Operador de comparacion de un ValorEstandar contra el valor vigente de un
 * parametro. RANGO es el original de RF04 (minimo/maximo, caso de prueba
 * CPF02 de la segunda entrega). El resto se agrego a partir de las
 * auditorias reales que el usuario ya tenia cargadas fuera de este repo
 * (AUDIT-xx.xlsx, hoja "Parametros"): la mayoria de esas reglas no son un
 * rango sino una condicion puntual contra un unico valor de referencia — por
 * ejemplo, AUDIT-22_4G_AJUSTES-UMBRALES-MINIMOS-LTAC exige
 * "LTAC.tacLimitGbrEmergency &lt; 500000" (MENOR_QUE), y
 * AUDIT-01_4G_CARRIER-AGGREGATION exige "CADPR.a3Offset = -6" (IGUAL).
 *
 * EN_LISTA y COINCIDE completan el resto del vocabulario de esas plantillas
 * ("in", "like") para parametros de tipo lista o texto; el seed de esta
 * ampliacion no carga ningun ejemplo real con esos dos (no se encontro
 * ninguna fila real que los usara en la muestra revisada), pero quedan
 * soportados por el modelo y se pueden cargar desde la pantalla de
 * Configuraciones como cualquier otro.
 */
public enum OperadorComparacion {
    RANGO,
    IGUAL,
    DISTINTO,
    MENOR_QUE,
    MENOR_IGUAL,
    MAYOR_QUE,
    MAYOR_IGUAL,
    EN_LISTA,
    COINCIDE
}
