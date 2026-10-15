package com.neipa.web.dto;

import java.util.List;
import java.util.Map;

import com.neipa.domain.Auditoria;

/**
 * Datos agregados para el "dashboard de control": lo que consumen los
 * jefes/gerentes (reportes ejecutivos) y los ingenieros de RF (revision
 * diaria, CU003) segun la primera entrega, seccion "Contexto de negocio".
 */
public class ResumenDashboardDTO {

    private long totalSegmentos;
    private long totalElementos;
    private long totalAuditorias;
    private long auditoriasConformes;
    private long auditoriasDesvio;
    private long auditoriasExceptuadas;
    private double tasaConformidadPct;
    private long excepcionesActivas;
    private Map<String, Long> elementosPorFabricante;
    private Map<String, Long> cambiosMasivosPorEstado;
    private List<Auditoria> auditoriasRecientes;

    public long getTotalSegmentos() {
        return totalSegmentos;
    }

    public void setTotalSegmentos(long totalSegmentos) {
        this.totalSegmentos = totalSegmentos;
    }

    public long getTotalElementos() {
        return totalElementos;
    }

    public void setTotalElementos(long totalElementos) {
        this.totalElementos = totalElementos;
    }

    public long getTotalAuditorias() {
        return totalAuditorias;
    }

    public void setTotalAuditorias(long totalAuditorias) {
        this.totalAuditorias = totalAuditorias;
    }

    public long getAuditoriasConformes() {
        return auditoriasConformes;
    }

    public void setAuditoriasConformes(long auditoriasConformes) {
        this.auditoriasConformes = auditoriasConformes;
    }

    public long getAuditoriasDesvio() {
        return auditoriasDesvio;
    }

    public void setAuditoriasDesvio(long auditoriasDesvio) {
        this.auditoriasDesvio = auditoriasDesvio;
    }

    public long getAuditoriasExceptuadas() {
        return auditoriasExceptuadas;
    }

    public void setAuditoriasExceptuadas(long auditoriasExceptuadas) {
        this.auditoriasExceptuadas = auditoriasExceptuadas;
    }

    public double getTasaConformidadPct() {
        return tasaConformidadPct;
    }

    public void setTasaConformidadPct(double tasaConformidadPct) {
        this.tasaConformidadPct = tasaConformidadPct;
    }

    public long getExcepcionesActivas() {
        return excepcionesActivas;
    }

    public void setExcepcionesActivas(long excepcionesActivas) {
        this.excepcionesActivas = excepcionesActivas;
    }

    public Map<String, Long> getElementosPorFabricante() {
        return elementosPorFabricante;
    }

    public void setElementosPorFabricante(Map<String, Long> elementosPorFabricante) {
        this.elementosPorFabricante = elementosPorFabricante;
    }

    public Map<String, Long> getCambiosMasivosPorEstado() {
        return cambiosMasivosPorEstado;
    }

    public void setCambiosMasivosPorEstado(Map<String, Long> cambiosMasivosPorEstado) {
        this.cambiosMasivosPorEstado = cambiosMasivosPorEstado;
    }

    public List<Auditoria> getAuditoriasRecientes() {
        return auditoriasRecientes;
    }

    public void setAuditoriasRecientes(List<Auditoria> auditoriasRecientes) {
        this.auditoriasRecientes = auditoriasRecientes;
    }
}
