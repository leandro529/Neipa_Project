package neipa.modelo;

/** Celda: portadora de un sector, identificada por su banda de frecuencia. */
public class Celda extends ElementoDeRed {

    private int bandaMHz;

    public Celda(String nombre, String tecnologia, Fabricante fabricante, Segmento segmento,
            int bandaMHz) {
        super(nombre, tecnologia, fabricante, segmento);
        setBandaMHz(bandaMHz);
    }

    @Override
    public String getTipo() {
        return "Celda";
    }

    @Override
    public String getDetalle() {
        return "banda " + bandaMHz + " MHz";
    }

    /** Una celda cuelga del LNBTS: .../LNBTS-n/LNCEL-n. */
    @Override
    protected String distNameGenerado() {
        return super.distNameGenerado() + "/LNCEL-" + getId();
    }

    public int getBandaMHz() {
        return bandaMHz;
    }

    public final void setBandaMHz(int bandaMHz) {
        if (bandaMHz <= 0) {
            throw new IllegalArgumentException("La banda debe ser un valor positivo en MHz.");
        }
        this.bandaMHz = bandaMHz;
    }
}
