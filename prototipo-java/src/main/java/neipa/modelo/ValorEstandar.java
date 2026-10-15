package neipa.modelo;

import neipa.estructuras.ListaEnlazada;

/**
 * Valor de referencia de un parametro para un segmento (RF02). Puede ser
 * un valor exacto (operador IGUAL) o un intervalo (operador RANGO); cada
 * caso tiene su propio constructor.
 */
public class ValorEstandar {

    private final Parametro parametro;
    private final Segmento segmento;
    private final OperadorComparacion operador;
    private final String valorReferencia;
    private final double minimo;
    private final double maximo;
    /** Condiciones adicionales (todas se tienen que cumplir). Vacia = estandar general del segmento. */
    private final ListaEnlazada<Condicion> condiciones = new ListaEnlazada<Condicion>();

    /** Estandar de valor exacto: el vigente debe ser igual a la referencia. */
    public ValorEstandar(Parametro parametro, Segmento segmento, String valorReferencia) {
        validarObligatorios(parametro, segmento);
        if (!parametro.validarRango(valorReferencia)) {
            throw new IllegalArgumentException("El estandar " + valorReferencia
                    + " esta fuera del rango permitido de " + parametro.getNombre() + ".");
        }
        this.parametro = parametro;
        this.segmento = segmento;
        this.operador = OperadorComparacion.IGUAL;
        this.valorReferencia = valorReferencia.trim();
        this.minimo = 0;
        this.maximo = 0;
    }

    /** Estandar por intervalo: el vigente debe estar entre minimo y maximo. */
    public ValorEstandar(Parametro parametro, Segmento segmento, double minimo, double maximo) {
        validarObligatorios(parametro, segmento);
        if (!(parametro instanceof ParametroNumerico)) {
            throw new IllegalArgumentException("El operador RANGO solo aplica a parametros numericos.");
        }
        if (minimo > maximo) {
            throw new IllegalArgumentException("El minimo no puede ser mayor que el maximo.");
        }
        this.parametro = parametro;
        this.segmento = segmento;
        this.operador = OperadorComparacion.RANGO;
        this.valorReferencia = null;
        this.minimo = minimo;
        this.maximo = maximo;
    }

    private static void validarObligatorios(Parametro parametro, Segmento segmento) {
        if (parametro == null || segmento == null) {
            throw new IllegalArgumentException("El estandar necesita un parametro y un segmento.");
        }
    }

    /** Indica si el valor vigente cumple el estandar del segmento (RF03). */
    public boolean cumple(String valorVigente) {
        if (operador == OperadorComparacion.IGUAL) {
            return parametro.sonEquivalentes(valorVigente, valorReferencia);
        }
        try {
            double numero = ParametroNumerico.convertir(valorVigente);
            return numero >= minimo && numero <= maximo;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /** Texto del estandar, para mostrar en pantalla. */
    public String obtenerEstandar() {
        if (operador == OperadorComparacion.IGUAL) {
            return valorReferencia;
        }
        return ParametroNumerico.formatear(minimo) + ".." + ParametroNumerico.formatear(maximo);
    }

    /**
     * Valor que debe quedar configurado al corregir un desvio: la referencia
     * exacta o, si el estandar es un intervalo, el extremo mas cercano.
     */
    public String valorObjetivo(String valorVigente) {
        if (operador == OperadorComparacion.IGUAL) {
            return valorReferencia;
        }
        double numero;
        try {
            numero = ParametroNumerico.convertir(valorVigente);
        } catch (NumberFormatException e) {
            numero = minimo;
        }
        if (numero < minimo) {
            numero = minimo;
        } else if (numero > maximo) {
            numero = maximo;
        }
        return ParametroNumerico.formatear(numero);
    }

    public Parametro getParametro() {
        return parametro;
    }

    public Segmento getSegmento() {
        return segmento;
    }

    public OperadorComparacion getOperador() {
        return operador;
    }

    /**
     * Agrega una condicion de aplicacion. Devuelve el mismo objeto para
     * poder encadenar: new ValorEstandar(...).conCondicion(...).
     */
    public ValorEstandar conCondicion(Condicion condicion) {
        if (condicion == null) {
            throw new IllegalArgumentException("La condicion no puede ser nula.");
        }
        condiciones.agregarAlFinal(condicion);
        return this;
    }

    /** El estandar aplica si el elemento es del segmento y cumple todas las condiciones. */
    public boolean aplicaA(ElementoDeRed elemento) {
        if (elemento.getSegmento() != segmento) {
            return false;
        }
        for (Condicion condicion : condiciones) {
            if (!condicion.seCumple(elemento)) {
                return false;
            }
        }
        return true;
    }

    /** Cantidad de condiciones: cuanto mas alta, mas especifico es el estandar. */
    public int getCantidadCondiciones() {
        return condiciones.tamanio();
    }

    public String describirCondiciones() {
        if (condiciones.estaVacia()) {
            return "general";
        }
        StringBuilder texto = new StringBuilder();
        for (Condicion condicion : condiciones) {
            if (texto.length() > 0) {
                texto.append(" y ");
            }
            texto.append(condicion.describir());
        }
        return texto.toString();
    }
}
