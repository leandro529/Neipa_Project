package com.neipa.extraction;

/**
 * Rango permitido (valor estandar) definido para un parametro (RF02, RF04).
 * Ejemplo usado en la segunda entrega (caso de prueba CPF02): "Potencia de
 * transmision (dBm)" con rango 30 a 46.
 */
public class RangoPermitido {

    private final double minimo;
    private final double maximo;

    public RangoPermitido(double minimo, double maximo) {
        this.minimo = minimo;
        this.maximo = maximo;
    }

    public double getMinimo() {
        return minimo;
    }

    public double getMaximo() {
        return maximo;
    }

    /**
     * Texto de referencia para guardar en auditorias.valor_estandar,
     * por ejemplo "30-46".
     */
    public String comoTexto() {
        return minimo + "-" + maximo;
    }
}
