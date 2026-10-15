package com.neipa.extraction;

/**
 * Valor vigente de un parametro de configuracion, tal como se obtiene desde
 * la plataforma de gestion (OSS) del fabricante (RF01). Equivalente, para un
 * unico parametro, a una fila del DataFrame que devolvia
 * ExtractAndTransform.get_dataset() en el codigo Python original.
 */
public class ParametroVigente {

    private final String nombreParametro;
    private final String valorVigente;

    public ParametroVigente(String nombreParametro, String valorVigente) {
        this.nombreParametro = nombreParametro;
        this.valorVigente = valorVigente;
    }

    public String getNombreParametro() {
        return nombreParametro;
    }

    public String getValorVigente() {
        return valorVigente;
    }
}
