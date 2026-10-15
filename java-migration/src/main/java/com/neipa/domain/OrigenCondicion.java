package com.neipa.domain;

/**
 * De donde sale el dato que evalua una {@link CondicionEstandar}.
 *
 * ATRIBUTO_ELEMENTO: un dato del propio elemento de red (campo NOMBRE,
 * TECNOLOGIA o FABRICANTE). Ej: "el nombre coincide con *_URB_*".
 *
 * PARAMETRO: el valor vigente de OTRO parametro del mismo elemento, leido en
 * la misma extraccion. Ej: "si PotenciaTX_dBm es 20, entonces este parametro
 * tiene que valer 10".
 */
public enum OrigenCondicion {
    ATRIBUTO_ELEMENTO,
    PARAMETRO
}
