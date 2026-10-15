package neipa.modelo;

import neipa.estructuras.ListaEnlazada;
import neipa.excepciones.ExtraccionException;

/**
 * Fabricante de equipos de red (Ericsson, Huawei, NSN) y su plataforma de
 * gestion (OSS). En el prototipo la plataforma esta simulada: los valores
 * vigentes se guardan en memoria dentro de cada elemento de red.
 */
public class Fabricante {

    private final String nombre;
    private final String tipoOss;
    private boolean plataformaDisponible;

    public Fabricante(String nombre, String tipoOss) {
        if (nombre == null || nombre.trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre del fabricante es obligatorio.");
        }
        this.nombre = nombre.trim();
        this.tipoOss = tipoOss;
        this.plataformaDisponible = true;
    }

    /**
     * Devuelve la configuracion vigente de un elemento de red (RF01). Si la
     * plataforma del fabricante no esta disponible lanza ExtraccionException.
     */
    public ListaEnlazada<ValorVigente> obtenerConfigVigente(ElementoDeRed elemento)
            throws ExtraccionException {
        if (!plataformaDisponible) {
            throw new ExtraccionException("la plataforma " + tipoOss + " de " + nombre
                    + " no responde (elemento " + elemento.getNombre() + ").");
        }
        return elemento.getValoresVigentes();
    }

    public String getNombre() {
        return nombre;
    }

    public String getTipoOss() {
        return tipoOss;
    }

    public boolean isPlataformaDisponible() {
        return plataformaDisponible;
    }

    public void setPlataformaDisponible(boolean plataformaDisponible) {
        this.plataformaDisponible = plataformaDisponible;
    }

    @Override
    public String toString() {
        return nombre + " (" + tipoOss + ")";
    }
}
