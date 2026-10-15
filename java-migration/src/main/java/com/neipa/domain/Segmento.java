package com.neipa.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * Entidad de dominio "Segmento" (agrupacion de elementos de red por criterio,
 * ej: tecnologia, zona). Mapea 1:1 contra la tabla "segmentos" tal como quedo
 * definida en la segunda entrega (Figura 11 - DER).
 */
@Entity
@Table(name = "segmentos")
public class Segmento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_segmento")
    private Long idSegmento;

    @Column(name = "nombre", nullable = false, length = 60)
    private String nombre;

    @Column(name = "criterio", length = 120)
    private String criterio;

    /** RC del CM de NetAct (PLMN_id del dump, ej "rc7"). Null = segmento manual. */
    @Column(name = "rc", length = 20)
    private String rc;

    /** Filtro opcional sobre el nombre del objeto ('*' y '?'), ej "BOG*". */
    @Column(name = "filtro_nombre", length = 80)
    private String filtroNombre;

    public Segmento() {
    }

    public Segmento(String nombre, String criterio) {
        this.nombre = nombre;
        this.criterio = criterio;
    }

    public Long getIdSegmento() {
        return idSegmento;
    }

    public void setIdSegmento(Long idSegmento) {
        this.idSegmento = idSegmento;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getRc() {
        return rc;
    }

    public void setRc(String rc) {
        this.rc = rc;
    }

    public String getFiltroNombre() {
        return filtroNombre;
    }

    public void setFiltroNombre(String filtroNombre) {
        this.filtroNombre = filtroNombre;
    }

    /** True si el segmento se audita sobre los datos de CM importados. */
    public boolean esDeCm() {
        return rc != null && !rc.trim().isEmpty();
    }

    public String getCriterio() {
        return criterio;
    }

    public void setCriterio(String criterio) {
        this.criterio = criterio;
    }
}
