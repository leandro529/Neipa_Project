package com.neipa.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * Entidad de dominio "CambioMasivo" (RF11-13): correccion de un parametro que
 * afecta a un porcentaje de elementos de un Segmento y que puede requerir
 * autorizacion. Mapea 1:1 contra la tabla "cambios_masivos".
 *
 * Se incluye por completitud del modelo relacional ya definido en la segunda
 * entrega; la migracion a Java de este flujo (CU004 - Monitorear cambios
 * masivos) queda fuera del alcance acordado para esta iteracion, que se
 * concentro en CU001 (Auditar parametros de configuracion).
 */
@Entity
@Table(name = "cambios_masivos")
public class CambioMasivo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_cambio")
    private Long idCambio;

    @Column(name = "id_segmento")
    private Long idSegmento;

    @Column(name = "parametro", length = 60)
    private String parametro;

    @Column(name = "pct_afectado", precision = 5, scale = 2)
    private BigDecimal pctAfectado;

    @Column(name = "estado", length = 20)
    private String estado;

    @Column(name = "autorizado_por", length = 60)
    private String autorizadoPor;

    @Column(name = "fecha")
    private LocalDateTime fecha;

    public CambioMasivo() {
    }

    public Long getIdCambio() {
        return idCambio;
    }

    public void setIdCambio(Long idCambio) {
        this.idCambio = idCambio;
    }

    public Long getIdSegmento() {
        return idSegmento;
    }

    public void setIdSegmento(Long idSegmento) {
        this.idSegmento = idSegmento;
    }

    public String getParametro() {
        return parametro;
    }

    public void setParametro(String parametro) {
        this.parametro = parametro;
    }

    public BigDecimal getPctAfectado() {
        return pctAfectado;
    }

    public void setPctAfectado(BigDecimal pctAfectado) {
        this.pctAfectado = pctAfectado;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getAutorizadoPor() {
        return autorizadoPor;
    }

    public void setAutorizadoPor(String autorizadoPor) {
        this.autorizadoPor = autorizadoPor;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public void setFecha(LocalDateTime fecha) {
        this.fecha = fecha;
    }
}
