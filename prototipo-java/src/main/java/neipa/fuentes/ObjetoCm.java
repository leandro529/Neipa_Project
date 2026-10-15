package neipa.fuentes;

import neipa.estructuras.ListaEnlazada;

/**
 * Un objeto gestionado leido de una fuente de CM (por ejemplo una celda
 * LNCEL de NetAct): su clase, su distName y los valores de sus parametros.
 * Es inmutable desde afuera: los valores solo se cargan al crearlo.
 */
public class ObjetoCm {

    private final String clase;
    private final String distName;
    private final ListaEnlazada<String[]> valores = new ListaEnlazada<String[]>();

    public ObjetoCm(String clase, String distName) {
        if (clase == null || clase.trim().isEmpty()) {
            throw new IllegalArgumentException("El objeto de CM necesita una clase.");
        }
        if (distName == null || !distName.trim().startsWith("PLMN-")) {
            throw new IllegalArgumentException("distName invalido: " + distName);
        }
        this.clase = clase.trim().toUpperCase();
        this.distName = distName.trim();
    }

    /** Solo lo usan las fuentes del paquete al leer el objeto. */
    void agregarValor(String parametro, String valor) {
        if (parametro == null || parametro.trim().isEmpty() || valor == null || valor.trim().isEmpty()) {
            return; // valor vacio: el parametro no tiene dato
        }
        valores.agregarAlFinal(new String[] {parametro.trim(), valor.trim()});
    }

    /** Valor de un parametro (sin distinguir mayusculas) o null si no lo tiene. */
    public String getValor(String parametro) {
        for (String[] par : valores) {
            if (par[0].equalsIgnoreCase(parametro)) {
                return par[1];
            }
        }
        return null;
    }

    /** Nombre del objeto en la red (parametro "name"), si lo tiene. */
    public String getNombre() {
        return getValor("name");
    }

    /** True si este objeto esta dentro del objeto con ese distName (o es el mismo). */
    public boolean dependeDe(String distNamePadre) {
        return distName.equals(distNamePadre) || distName.startsWith(distNamePadre + "/");
    }

    public String getClase() {
        return clase;
    }

    public String getDistName() {
        return distName;
    }

    public int cantidadDeValores() {
        return valores.tamanio();
    }

    @Override
    public String toString() {
        return clase + " " + distName;
    }
}
