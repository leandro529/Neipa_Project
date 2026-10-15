package neipa.modelo;

import java.time.LocalDateTime;

/**
 * Resultado de auditar un parametro de un elemento de red: guarda el valor
 * vigente, el estandar contra el que se comparo, el resultado y la fecha
 * (RF10).
 */
public class Auditoria {

    public static final String MSG_CONFORME = "Parametro conforme al estandar.";
    public static final String MSG_FUERA_DE_RANGO =
            "Parametro fuera de rango permitido (desvio registrado).";
    public static final String MSG_DISTINTO =
            "Valor distinto del estandar del segmento (desvio registrado).";

    private final ElementoDeRed elementoDeRed;
    private final Parametro parametro;
    private final String valorVigente;
    private final ValorEstandar valorEstandar;
    private final LocalDateTime fecha;
    private ResultadoAuditoria resultado;
    private String observacion;
    private Excepcion excepcionAplicada;

    public Auditoria(ElementoDeRed elementoDeRed, Parametro parametro, String valorVigente,
            ValorEstandar valorEstandar) {
        this.elementoDeRed = elementoDeRed;
        this.parametro = parametro;
        this.valorVigente = valorVigente;
        this.valorEstandar = valorEstandar;
        this.fecha = LocalDateTime.now();
    }

    /**
     * Aplica las reglas de CU001: primero la excepcion vigente (flujo
     * alternativo S4), despues el rango permitido (RF04) y por ultimo la
     * comparacion contra el estandar del segmento (RF03).
     */
    public ResultadoAuditoria compararConEstandar(Excepcion excepcionVigente) {
        if (excepcionVigente != null) {
            resultado = ResultadoAuditoria.EXCEPTUADO;
            excepcionAplicada = excepcionVigente;
            observacion = "Excepcion vigente: " + excepcionVigente.getMotivo() + ".";
        } else if (!parametro.validarRango(valorVigente)) {
            resultado = ResultadoAuditoria.DESVIO;
            observacion = MSG_FUERA_DE_RANGO;
        } else if (!valorEstandar.cumple(valorVigente)) {
            resultado = ResultadoAuditoria.DESVIO;
            observacion = MSG_DISTINTO;
        } else {
            resultado = ResultadoAuditoria.CONFORME;
            observacion = MSG_CONFORME;
        }
        return resultado;
    }

    public boolean esDesvio() {
        return resultado == ResultadoAuditoria.DESVIO;
    }

    public ElementoDeRed getElementoDeRed() {
        return elementoDeRed;
    }

    public Parametro getParametro() {
        return parametro;
    }

    public String getValorVigente() {
        return valorVigente;
    }

    public ValorEstandar getValorEstandar() {
        return valorEstandar;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public ResultadoAuditoria getResultado() {
        return resultado;
    }

    public String getObservacion() {
        return observacion;
    }

    public Excepcion getExcepcionAplicada() {
        return excepcionAplicada;
    }

    @Override
    public String toString() {
        return elementoDeRed.getNombre() + " / " + parametro.getNombre() + ": vigente "
                + valorVigente + ", estandar " + valorEstandar.obtenerEstandar() + " -> " + resultado;
    }
}
