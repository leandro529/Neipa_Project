package com.neipa.extraction;

import org.springframework.stereotype.Component;

/**
 * Factory que entrega la implementacion de VendorExtractor correspondiente a
 * un fabricante (RF01, RF21). Equivalente Java de VendorFactory.py.
 *
 * Igual que en el codigo Python original (donde solo NsnOperations.py tenia
 * un principio de implementacion y HuaweiOperations.py estaba vacio), aca
 * solo NSN tiene una implementacion concreta; Huawei y Ericsson lanzan
 * UnsupportedOperationException hasta que se agregue su extractor.
 */
@Component
public class VendorExtractorFactory {

    private final NsnVendorExtractor nsnVendorExtractor;

    public VendorExtractorFactory(NsnVendorExtractor nsnVendorExtractor) {
        this.nsnVendorExtractor = nsnVendorExtractor;
    }

    public VendorExtractor crear(Fabricante fabricante) {
        switch (fabricante) {
            case NSN:
                return nsnVendorExtractor;
            case HUAWEI:
            case ERICSSON:
                throw new UnsupportedOperationException(
                        "Extractor no implementado para el fabricante: " + fabricante);
            default:
                throw new IllegalArgumentException("Fabricante desconocido: " + fabricante);
        }
    }

    public VendorExtractor crear(String fabricante) {
        return crear(Fabricante.valueOf(fabricante.toUpperCase()));
    }
}
