package com.neipa.domain;

import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * Una corrida de auditoria de un segmento sobre los datos de CM importados.
 * Guarda los totales (los conformes no se guardan fila por fila): asi se ve
 * como baja la cantidad de desvios a medida que la red se estandariza.
 */
@Entity
@Table(name = "corridas")
public class Corrida {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_corrida")
    private Long idCorrida;

    @Column(name = "id_segmento")
    private Long idSegmento;

    @Column(name = "id_importacion")
    private Long idImportacion;

    @Column(name = "fecha")
    private LocalDateTime fecha;

    @Column(name = "objetos_evaluados")
    private long objetosEvaluados;

    @Column(name = "comparaciones")
    private long comparaciones;

    @Column(name = "conformes")
    private long conformes;

    @Column(name = "desvios")
    private long desvios;

    @Column(name = "exceptuados")
    private long exceptuados;

    @Column(name = "sin_regla")
    private long sinRegla;

    @Column(name = "duracion_ms")
    private long duracionMs;

    public double getTasaConformidadPct() {
        return comparaciones == 0 ? 0.0 : conformes * 100.0 / comparaciones;
    }

    public Long getIdCorrida() {
        return idCorrida;
    }

    public void setIdCorrida(Long idCorrida) {
        this.idCorrida = idCorrida;
    }

    public Long getIdSegmento() {
        return idSegmento;
    }

    public void setIdSegmento(Long idSegmento) {
        this.idSegmento = idSegmento;
    }

    public Long getIdImportacion() {
        return idImportacion;
    }

    public void setIdImportacion(Long idImportacion) {
        this.idImportacion = idImportacion;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public void setFecha(LocalDateTime fecha) {
        this.fecha = fecha;
    }

    public long getObjetosEvaluados() {
        return objetosEvaluados;
    }

    public void setObjetosEvaluados(long objetosEvaluados) {
        this.objetosEvaluados = objetosEvaluados;
    }

    public long getComparaciones() {
        return comparaciones;
    }

    public void setComparaciones(long comparaciones) {
        this.comparaciones = comparaciones;
    }

    public long getConformes() {
        return conformes;
    }

    public void setConformes(long conformes) {
        this.conformes = conformes;
    }

    public long getDesvios() {
        return desvios;
    }

    public void setDesvios(long desvios) {
        this.desvios = desvios;
    }

    public long getExceptuados() {
        return exceptuados;
    }

    public void setExceptuados(long exceptuados) {
        this.exceptuados = exceptuados;
    }

    public long getSinRegla() {
        return sinRegla;
    }

    public void setSinRegla(long sinRegla) {
        this.sinRegla = sinRegla;
    }

    public long getDuracionMs() {
        return duracionMs;
    }

    public void setDuracionMs(long duracionMs) {
        this.duracionMs = duracionMs;
    }
}
