package neipa.modelo;

import java.time.LocalDateTime;

import neipa.estructuras.ListaEnlazada;
import neipa.excepciones.ExtraccionException;
import neipa.excepciones.ValorInvalidoException;

/**
 * Clase abstracta que representa cualquier elemento de red auditable.
 * Reune los atributos y el comportamiento comunes; cada subclase (Sitio,
 * Celda, Antena) aporta su tipo y su dato particular.
 */
public abstract class ElementoDeRed {

    /** Contador compartido por todas las instancias para generar el id. */
    private static int ultimoId = 0;

    private final int id;
    private String nombre;
    private String tecnologia;
    private final Fabricante fabricante;
    private Segmento segmento;
    private final ListaEnlazada<ValorVigente> valoresVigentes;
    private String distNameCm;

    protected ElementoDeRed(String nombre, String tecnologia, Fabricante fabricante,
            Segmento segmento) {
        if (fabricante == null || segmento == null) {
            throw new IllegalArgumentException("El elemento necesita un fabricante y un segmento.");
        }
        setNombre(nombre);
        setTecnologia(tecnologia);
        ultimoId++;
        this.id = ultimoId;
        this.fabricante = fabricante;
        this.segmento = segmento;
        this.valoresVigentes = new ListaEnlazada<ValorVigente>();
    }

    /** Tipo de elemento, definido por cada subclase. */
    public abstract String getTipo();

    /** Dato particular del tipo de elemento, definido por cada subclase. */
    public abstract String getDetalle();

    /** Descripcion completa: combina lo comun con lo que aporta cada subclase. */
    public String describir() {
        return getTipo() + " " + nombre + " (" + tecnologia + ", " + fabricante.getNombre()
                + ", segmento " + segmento.getNombre() + ") - " + getDetalle();
    }

    /**
     * Pide a la plataforma del fabricante los parametros vigentes del
     * elemento (paso 3 del diagrama de secuencia de CU001).
     */
    public ListaEnlazada<ValorVigente> obtenerParametrosVigentes() throws ExtraccionException {
        return fabricante.obtenerConfigVigente(this);
    }

    /** Carga o reemplaza el valor vigente de un parametro del elemento. */
    public void registrarValorVigente(Parametro parametro, String valor)
            throws ValorInvalidoException {
        if (parametro.getFabricante() != fabricante) {
            throw new ValorInvalidoException("El parametro " + parametro.getNombre()
                    + " pertenece a " + parametro.getFabricante().getNombre()
                    + " y el elemento es de " + fabricante.getNombre() + ".");
        }
        ValorVigente existente = buscarValorVigente(parametro);
        if (existente != null) {
            existente.actualizar(valor, LocalDateTime.now());
        } else {
            valoresVigentes.agregarAlFinal(
                    new ValorVigente(this, parametro, valor, LocalDateTime.now()));
        }
    }

    /** Busqueda secuencial del valor vigente de un parametro (null si no esta). */
    public ValorVigente buscarValorVigente(Parametro parametro) {
        for (ValorVigente valor : valoresVigentes) {
            if (valor.getParametro() == parametro) {
                return valor;
            }
        }
        return null;
    }

    ListaEnlazada<ValorVigente> getValoresVigentes() {
        return valoresVigentes;
    }

    /**
     * Nombre distintivo (distName) del elemento en el OSS, que se usa en el
     * XML de reconfiguracion RAML 2.0. Por defecto, el del sitio (LNBTS);
     * las subclases lo extienden (polimorfismo).
     */
    /**
     * distName del elemento en NetAct: el real si se importo del CM, o uno
     * armado con el id para los elementos cargados a mano.
     */
    public String getDistName() {
        return distNameCm != null ? distNameCm : distNameGenerado();
    }

    /** distName armado con el id (cada subclase agrega su objeto). */
    protected String distNameGenerado() {
        return "PLMN-PLMN/MRBTS-" + id + "/LNBTS-" + id;
    }

    /** Asigna el distName real leido de una fuente de CM. */
    public void asignarDistName(String distName) {
        if (distName == null || !distName.trim().startsWith("PLMN-")) {
            throw new IllegalArgumentException("distName invalido: " + distName);
        }
        this.distNameCm = distName.trim();
    }

    public int getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public final void setNombre(String nombre) {
        if (nombre == null || nombre.trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre del elemento es obligatorio.");
        }
        this.nombre = nombre.trim();
    }

    public String getTecnologia() {
        return tecnologia;
    }

    public final void setTecnologia(String tecnologia) {
        if (tecnologia == null || tecnologia.trim().isEmpty()) {
            throw new IllegalArgumentException("La tecnologia del elemento es obligatoria.");
        }
        this.tecnologia = tecnologia.trim().toUpperCase();
    }

    public Fabricante getFabricante() {
        return fabricante;
    }

    public Segmento getSegmento() {
        return segmento;
    }

    public void setSegmento(Segmento segmento) {
        if (segmento == null) {
            throw new IllegalArgumentException("El segmento es obligatorio.");
        }
        this.segmento = segmento;
    }

    @Override
    public String toString() {
        return "#" + id + " " + nombre;
    }
}
