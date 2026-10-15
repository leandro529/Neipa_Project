package com.neipa.extraction;

import java.util.List;

import com.neipa.domain.ElementoDeRed;

/**
 * Contrato de extraccion de parametros vigentes desde la plataforma de
 * gestion (OSS) de un fabricante (RF01). Equivalente Java de la clase
 * abstracta ManagementObject (vendor_utilities/ManagementObject.py) del repo
 * original, acotado a la unica operacion que necesita el flujo de auditoria
 * (CU001): obtener los parametros vigentes de un elemento de red.
 */
public interface VendorExtractor {

    List<ParametroVigente> extraerParametrosVigentes(ElementoDeRed elemento);
}
