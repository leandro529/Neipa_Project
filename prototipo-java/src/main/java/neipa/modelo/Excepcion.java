package neipa.modelo;

import java.time.LocalDate;

import neipa.algoritmos.PatronNombre;

/**
 * Excepcion de configuracion: indica que un desvio no debe informarse para
 * un elemento, un segmento o un parametro durante un periodo de vigencia.
 * (No es una excepcion de Java: es un concepto del negocio.)
 */
public class Excepcion {

    private final AlcanceExcepcion alcance;
    private final ElementoDeRed elementoDeRed;
    private final Segmento segmento;
    private final Parametro parametro;
    private final String patron;
    private final String motivo;
    private final LocalDate vigenteDesde;
    private final LocalDate vigenteHasta;
    private boolean activa;

    private Excepcion(AlcanceExcepcion alcance, ElementoDeRed elementoDeRed, Segmento segmento,
            Parametro parametro, String motivo, LocalDate vigenteDesde, LocalDate vigenteHasta) {
        this(alcance, elementoDeRed, segmento, parametro, null, motivo, vigenteDesde, vigenteHasta);
    }

    private Excepcion(AlcanceExcepcion alcance, ElementoDeRed elementoDeRed, Segmento segmento,
            Parametro parametro, String patron, String motivo, LocalDate vigenteDesde,
            LocalDate vigenteHasta) {
        if (motivo == null || motivo.trim().isEmpty()) {
            throw new IllegalArgumentException("La excepcion necesita un motivo.");
        }
        if (vigenteHasta.isBefore(vigenteDesde)) {
            throw new IllegalArgumentException("La vigencia termina antes de empezar.");
        }
        this.alcance = alcance;
        this.elementoDeRed = elementoDeRed;
        this.segmento = segmento;
        this.parametro = parametro;
        this.patron = patron;
        this.motivo = motivo.trim();
        this.vigenteDesde = vigenteDesde;
        this.vigenteHasta = vigenteHasta;
        this.activa = true;
    }

    /** Excepcion para un parametro de un elemento puntual. */
    public static Excepcion porElemento(ElementoDeRed elemento, Parametro parametro, String motivo,
            LocalDate desde, LocalDate hasta) {
        if (elemento == null || parametro == null) {
            throw new IllegalArgumentException("Se necesita el elemento y el parametro.");
        }
        return new Excepcion(AlcanceExcepcion.ELEMENTO, elemento, null, parametro, motivo,
                desde, hasta);
    }

    /** Excepcion para todos los parametros de los elementos de un segmento. */
    public static Excepcion porSegmento(Segmento segmento, String motivo, LocalDate desde,
            LocalDate hasta) {
        if (segmento == null) {
            throw new IllegalArgumentException("Se necesita el segmento.");
        }
        return new Excepcion(AlcanceExcepcion.SEGMENTO, null, segmento, null, motivo, desde, hasta);
    }

    /** Excepcion para un parametro en todos los elementos de la red. */
    public static Excepcion porParametro(Parametro parametro, String motivo, LocalDate desde,
            LocalDate hasta) {
        if (parametro == null) {
            throw new IllegalArgumentException("Se necesita el parametro.");
        }
        return new Excepcion(AlcanceExcepcion.PARAMETRO, null, null, parametro, motivo,
                desde, hasta);
    }

    /** Indica si la excepcion esta activa y dentro de su periodo de vigencia. */
    /**
     * Exceptua a todos los elementos cuyo nombre coincide con el patron
     * ('*' = cualquier texto, '?' = un caracter). Con parametro null cubre
     * todos los parametros. Ejemplo: porPatron("*_test_*", null, ...).
     */
    public static Excepcion porPatron(String patron, Parametro parametro, String motivo,
            LocalDate desde, LocalDate hasta) {
        if (patron == null || patron.trim().isEmpty()) {
            throw new IllegalArgumentException("Se necesita el patron de nombre.");
        }
        return new Excepcion(AlcanceExcepcion.PATRON, null, null, parametro, patron.trim(),
                motivo, desde, hasta);
    }

    public boolean estaVigente(LocalDate fecha) {
        return activa && !fecha.isBefore(vigenteDesde) && !fecha.isAfter(vigenteHasta);
    }

    /** Indica si la excepcion alcanza a la combinacion elemento-parametro evaluada. */
    public boolean aplicaA(ElementoDeRed elemento, Parametro parametroEvaluado) {
        switch (alcance) {
            case ELEMENTO:
                return elementoDeRed == elemento && parametro == parametroEvaluado;
            case SEGMENTO:
                return segmento == elemento.getSegmento();
            case PARAMETRO:
                return parametro == parametroEvaluado;
            case PATRON:
                return PatronNombre.coincide(elemento.getNombre(), patron)
                        && (parametro == null || parametro == parametroEvaluado);
            default:
                return false;
        }
    }

    /** Texto de a que se aplica la excepcion, para mostrar en pantalla. */
    public String describirAlcance() {
        switch (alcance) {
            case ELEMENTO:
                return "elemento " + elementoDeRed.getNombre() + " / " + parametro.getNombre();
            case SEGMENTO:
                return "segmento " + segmento.getNombre();
            case PATRON:
                return "nombre como " + patron + " / "
                        + (parametro == null ? "todos los parametros" : parametro.getNombre());
            default:
                return "parametro " + parametro.getNombre();
        }
    }

    public void desactivar() {
        this.activa = false;
    }

    public AlcanceExcepcion getAlcance() {
        return alcance;
    }

    public String getMotivo() {
        return motivo;
    }

    public LocalDate getVigenteDesde() {
        return vigenteDesde;
    }

    public LocalDate getVigenteHasta() {
        return vigenteHasta;
    }

    public boolean isActiva() {
        return activa;
    }

    @Override
    public String toString() {
        return alcance + " - " + describirAlcance() + " (vigente hasta " + vigenteHasta + ")";
    }
}
