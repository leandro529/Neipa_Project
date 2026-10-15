package neipa.modelo;

/** Sitio (radiobase): agrupa una cantidad de sectores. */
public class Sitio extends ElementoDeRed {

    private int cantidadSectores;

    public Sitio(String nombre, String tecnologia, Fabricante fabricante, Segmento segmento,
            int cantidadSectores) {
        super(nombre, tecnologia, fabricante, segmento);
        setCantidadSectores(cantidadSectores);
    }

    @Override
    public String getTipo() {
        return "Sitio";
    }

    @Override
    public String getDetalle() {
        return cantidadSectores + " sectores";
    }

    public int getCantidadSectores() {
        return cantidadSectores;
    }

    public final void setCantidadSectores(int cantidadSectores) {
        if (cantidadSectores < 1) {
            throw new IllegalArgumentException("Un sitio tiene al menos un sector.");
        }
        this.cantidadSectores = cantidadSectores;
    }
}
