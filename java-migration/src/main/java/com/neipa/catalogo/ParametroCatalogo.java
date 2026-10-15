package com.neipa.catalogo;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Un parametro del diccionario de Nokia (una fila del catalogo): clase,
 * rango o enumeracion, valor por default, si se modifica en linea, etc.
 * Los limites "Interno" estan expresados en el valor que guarda el CM.
 */
public class ParametroCatalogo {

    private Long idCatalogo;
    private String fuente;
    private String release;
    private String tecnologia;
    private String clase;
    private String rutaClase;
    private String parametro;
    private String estructura;
    private String nombre;
    private String tipo;
    private String unidad;
    private String rango;
    private Double minimoInterno;
    private Double maximoInterno;
    private String enumeracion;
    private String formula;
    private String defaultValor;
    private String defaultInterno;
    private String modificacion;
    private String funciones;
    private String relacionados;
    private String relaciones;
    private String descripcion;

    /** "1=n1|2=n2" -> {1: n1, 2: n2}. Vacio si no es una enumeracion. */
    public Map<String, String> valoresEnumerados() {
        Map<String, String> valores = new LinkedHashMap<>();
        if (enumeracion == null || enumeracion.isEmpty()) {
            return valores;
        }
        for (String par : enumeracion.split("\\|")) {
            int igual = par.indexOf('=');
            if (igual > 0) {
                valores.put(par.substring(0, igual).trim(), par.substring(igual + 1).trim());
            }
        }
        return valores;
    }

    /** True si requiere bloquear el objeto para cambiarlo (no es "On-line"). */
    public boolean requiereBloqueo() {
        return modificacion != null && modificacion.toLowerCase().contains("locking");
    }

    public Long getIdCatalogo() { return idCatalogo; }
    public void setIdCatalogo(Long v) { idCatalogo = v; }
    public String getFuente() { return fuente; }
    public void setFuente(String v) { fuente = v; }
    public String getRelease() { return release; }
    public void setRelease(String v) { release = v; }
    public String getTecnologia() { return tecnologia; }
    public void setTecnologia(String v) { tecnologia = v; }
    public String getClase() { return clase; }
    public void setClase(String v) { clase = v; }
    public String getRutaClase() { return rutaClase; }
    public void setRutaClase(String v) { rutaClase = v; }
    public String getParametro() { return parametro; }
    public void setParametro(String v) { parametro = v; }
    public String getEstructura() { return estructura; }
    public void setEstructura(String v) { estructura = v; }
    public String getNombre() { return nombre; }
    public void setNombre(String v) { nombre = v; }
    public String getTipo() { return tipo; }
    public void setTipo(String v) { tipo = v; }
    public String getUnidad() { return unidad; }
    public void setUnidad(String v) { unidad = v; }
    public String getRango() { return rango; }
    public void setRango(String v) { rango = v; }
    public Double getMinimoInterno() { return minimoInterno; }
    public void setMinimoInterno(Double v) { minimoInterno = v; }
    public Double getMaximoInterno() { return maximoInterno; }
    public void setMaximoInterno(Double v) { maximoInterno = v; }
    public String getEnumeracion() { return enumeracion; }
    public void setEnumeracion(String v) { enumeracion = v; }
    public String getFormula() { return formula; }
    public void setFormula(String v) { formula = v; }
    public String getDefaultValor() { return defaultValor; }
    public void setDefaultValor(String v) { defaultValor = v; }
    public String getDefaultInterno() { return defaultInterno; }
    public void setDefaultInterno(String v) { defaultInterno = v; }
    public String getModificacion() { return modificacion; }
    public void setModificacion(String v) { modificacion = v; }
    public String getFunciones() { return funciones; }
    public void setFunciones(String v) { funciones = v; }
    public String getRelacionados() { return relacionados; }
    public void setRelacionados(String v) { relacionados = v; }
    public String getRelaciones() { return relaciones; }
    public void setRelaciones(String v) { relaciones = v; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String v) { descripcion = v; }
}
