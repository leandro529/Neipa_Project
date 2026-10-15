package neipa.modelo;

import java.time.LocalDateTime;

import neipa.excepciones.ValorInvalidoException;

/** Valor de un parametro extraido de un elemento de red en una fecha dada. */
public class ValorVigente {

    private final ElementoDeRed elementoDeRed;
    private final Parametro parametro;
    private String valor;
    private LocalDateTime fecha;

    public ValorVigente(ElementoDeRed elementoDeRed, Parametro parametro, String valor,
            LocalDateTime fecha) throws ValorInvalidoException {
        this.elementoDeRed = elementoDeRed;
        this.parametro = parametro;
        actualizar(valor, fecha);
    }

    /** Cambia el valor, validando antes que respete el tipo de dato del parametro. */
    public final void actualizar(String nuevoValor, LocalDateTime nuevaFecha)
            throws ValorInvalidoException {
        parametro.validarTipo(nuevoValor);
        this.valor = parametro.normalizar(nuevoValor);
        this.fecha = nuevaFecha;
    }

    public String obtenerValor() {
        return valor;
    }

    public ElementoDeRed getElementoDeRed() {
        return elementoDeRed;
    }

    public Parametro getParametro() {
        return parametro;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    @Override
    public String toString() {
        return parametro.getNombre() + " = " + valor;
    }
}
