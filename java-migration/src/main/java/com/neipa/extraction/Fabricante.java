package com.neipa.extraction;

/**
 * Fabricantes soportados por el sistema (equivalente al enum ConnType de
 * vendor_utilities/Vendors.py del repo original). En esta migracion solo NSN
 * tiene una implementacion de extraccion; Huawei y Ericsson quedan como
 * puntos de extension (RF21: incorporar un nuevo fabricante sin modificar la
 * logica de auditoria), igual que en el codigo Python original, donde
 * NsnOperations.py y EricsonOperations.py estaban sin terminar y
 * HuaweiOperations.py estaba vacio.
 */
public enum Fabricante {
    NSN,
    HUAWEI,
    ERICSSON
}
