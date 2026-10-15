package neipa.modelo;

/** Antena: elemento radiante instalado a una altura determinada. */
public class Antena extends ElementoDeRed {

    private double alturaMetros;

    public Antena(String nombre, String tecnologia, Fabricante fabricante, Segmento segmento,
            double alturaMetros) {
        super(nombre, tecnologia, fabricante, segmento);
        setAlturaMetros(alturaMetros);
    }

    @Override
    public String getTipo() {
        return "Antena";
    }

    @Override
    public String getDetalle() {
        return "altura " + alturaMetros + " m";
    }

    /** Una antena se gestiona como ANTL del LNBTS: .../LNBTS-n/ANTL-n. */
    @Override
    protected String distNameGenerado() {
        return super.distNameGenerado() + "/ANTL-" + getId();
    }

    public double getAlturaMetros() {
        return alturaMetros;
    }

    public final void setAlturaMetros(double alturaMetros) {
        if (alturaMetros <= 0) {
            throw new IllegalArgumentException("La altura debe ser mayor que cero.");
        }
        this.alturaMetros = alturaMetros;
    }
}
