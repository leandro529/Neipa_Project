package neipa.modelo;

import java.time.LocalDateTime;

/**
 * Correccion aplicada sobre un parametro de un elemento. Conserva el valor
 * anterior para poder revertirla (rollback, RF28).
 */
public class CambioConfiguracion {

    private final ElementoDeRed elementoDeRed;
    private final Parametro parametro;
    private final String valorAnterior;
    private final String valorNuevo;
    private final LocalDateTime fecha;

    public CambioConfiguracion(ElementoDeRed elementoDeRed, Parametro parametro,
            String valorAnterior, String valorNuevo) {
        this.elementoDeRed = elementoDeRed;
        this.parametro = parametro;
        this.valorAnterior = valorAnterior;
        this.valorNuevo = valorNuevo;
        this.fecha = LocalDateTime.now();
    }

    public ElementoDeRed getElementoDeRed() {
        return elementoDeRed;
    }

    public Parametro getParametro() {
        return parametro;
    }

    public String getValorAnterior() {
        return valorAnterior;
    }

    public String getValorNuevo() {
        return valorNuevo;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    @Override
    public String toString() {
        return elementoDeRed.getNombre() + " / " + parametro.getNombre() + ": " + valorAnterior
                + " -> " + valorNuevo;
    }
}
