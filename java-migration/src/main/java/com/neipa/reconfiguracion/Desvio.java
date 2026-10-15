package com.neipa.reconfiguracion;

/**
 * Un desvio vigente a corregir: la ultima auditoria de un parametro de un
 * elemento dio DESVIO (no exceptuado). Es lo que se lista en la pantalla de
 * reconfiguracion y lo que se escribe en el XML RAML 2.0.
 */
public class Desvio {

    private final Long idAuditoria;
    private final Long idElemento;
    private final String elemento;
    private final Long idSegmento;
    private final String distName;
    private final String claseMo;
    private final String parametro;
    private final String valorVigente;
    private final String valorEstandar;
    private final String reglaAplicada;
    private final String valorObjetivo;

    public Desvio(Long idAuditoria, Long idElemento, String elemento, Long idSegmento, String distName,
                  String claseMo, String parametro, String valorVigente, String valorEstandar,
                  String reglaAplicada, String valorObjetivo) {
        this.idAuditoria = idAuditoria;
        this.idElemento = idElemento;
        this.elemento = elemento;
        this.idSegmento = idSegmento;
        this.distName = distName;
        this.claseMo = claseMo;
        this.parametro = parametro;
        this.valorVigente = valorVigente;
        this.valorEstandar = valorEstandar;
        this.reglaAplicada = reglaAplicada;
        this.valorObjetivo = valorObjetivo;
    }

    /** True si se sabe a que valor llevar el parametro (va al XML). */
    public boolean isReconfigurable() {
        return valorObjetivo != null && !valorObjetivo.trim().isEmpty();
    }

    public Long getIdAuditoria() {
        return idAuditoria;
    }

    public Long getIdElemento() {
        return idElemento;
    }

    public String getElemento() {
        return elemento;
    }

    public Long getIdSegmento() {
        return idSegmento;
    }

    public String getDistName() {
        return distName;
    }

    public String getClaseMo() {
        return claseMo;
    }

    public String getParametro() {
        return parametro;
    }

    public String getValorVigente() {
        return valorVigente;
    }

    public String getValorEstandar() {
        return valorEstandar;
    }

    public String getReglaAplicada() {
        return reglaAplicada;
    }

    public String getValorObjetivo() {
        return valorObjetivo;
    }
}
