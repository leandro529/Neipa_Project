package neipa.modelo;

import neipa.excepciones.ValorInvalidoException;

/**
 * Clase abstracta del catalogo de parametros de configuracion. Cada
 * subclase define como se valida el rango permitido segun su tipo de dato
 * (numerico o enumerado).
 */
public abstract class Parametro {

    private final Fabricante fabricante;
    private final String objetoGestionado;
    private final String nombre;
    private final String descripcion;
    private final String unidad;
    private final String valorDefault;

    protected Parametro(Fabricante fabricante, String objetoGestionado, String nombre,
            String descripcion, String unidad, String valorDefault) {
        if (fabricante == null) {
            throw new IllegalArgumentException("El parametro necesita un fabricante.");
        }
        if (nombre == null || nombre.trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre del parametro es obligatorio.");
        }
        this.fabricante = fabricante;
        this.objetoGestionado = objetoGestionado;
        this.nombre = nombre.trim();
        this.descripcion = descripcion;
        this.unidad = unidad;
        this.valorDefault = valorDefault;
    }

    /** Indica si el valor esta dentro del rango permitido del parametro (RF04). */
    public abstract boolean validarRango(String valor);

    /** Verifica que el valor sea compatible con el tipo de dato del parametro. */
    public abstract void validarTipo(String valor) throws ValorInvalidoException;

    /** Compara dos valores segun el tipo de dato del parametro. */
    public abstract boolean sonEquivalentes(String valorA, String valorB);

    /** Texto del rango permitido, para mostrar en pantalla. */
    public abstract String getRangoPermitido();

    /** Tipo de dato del parametro (columna tipo_dato del modelo de datos). */
    public abstract String getTipoDato();

    /**
     * Devuelve el valor tal como debe guardarse. Por defecto solo quita los
     * espacios; las subclases pueden redefinirlo.
     */
    public String normalizar(String valor) {
        return valor.trim();
    }

    public Fabricante getFabricante() {
        return fabricante;
    }

    public String getObjetoGestionado() {
        return objetoGestionado;
    }

    public String getNombre() {
        return nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public String getUnidad() {
        return unidad;
    }

    public String getValorDefault() {
        return valorDefault;
    }

    @Override
    public String toString() {
        return fabricante.getNombre() + " " + objetoGestionado + "." + nombre;
    }
}
