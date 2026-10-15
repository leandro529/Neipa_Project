package com.neipa.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * Entidad de dominio "ElementoDeRed" (sitio/celda/core/antena de un fabricante,
 * agrupado en un Segmento). Mapea 1:1 contra la tabla "elementos_red".
 *
 * El fabricante se guarda como texto (coincide con el nombre del enum
 * {@link com.neipa.extraction.Fabricante}) para no atarse a un tipo enumerado
 * a nivel de base de datos, igual que en el DER ya aprobado.
 */
@Entity
@Table(name = "elementos_red")
public class ElementoDeRed {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_elemento")
    private Long idElemento;

    @Column(name = "nombre", nullable = false, length = 60)
    private String nombre;

    @Column(name = "tecnologia", length = 20)
    private String tecnologia;

    @Column(name = "fabricante", length = 30)
    private String fabricante;

    @Column(name = "id_segmento")
    private Long idSegmento;

    /**
     * distName del elemento en el OSS (NetAct), ej:
     * "PLMN-PLMN/MRBTS-1001/LNBTS-1001". Se usa para armar el XML RAML 2.0 de
     * reconfiguracion. Si no se cargo, se arma uno por defecto con el id.
     */
    @Column(name = "dist_name", length = 200)
    private String distName;

    public ElementoDeRed() {
    }

    public ElementoDeRed(String nombre, String tecnologia, String fabricante, Long idSegmento) {
        this.nombre = nombre;
        this.tecnologia = tecnologia;
        this.fabricante = fabricante;
        this.idSegmento = idSegmento;
    }

    public Long getIdElemento() {
        return idElemento;
    }

    public void setIdElemento(Long idElemento) {
        this.idElemento = idElemento;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getTecnologia() {
        return tecnologia;
    }

    public void setTecnologia(String tecnologia) {
        this.tecnologia = tecnologia;
    }

    public String getFabricante() {
        return fabricante;
    }

    public void setFabricante(String fabricante) {
        this.fabricante = fabricante;
    }

    public Long getIdSegmento() {
        return idSegmento;
    }

    public void setIdSegmento(Long idSegmento) {
        this.idSegmento = idSegmento;
    }

    public String getDistName() {
        return distName;
    }

    public void setDistName(String distName) {
        this.distName = distName;
    }

    /** distName cargado, o uno por defecto armado con el id del elemento. */
    public String distNameEfectivo() {
        if (distName != null && !distName.trim().isEmpty()) {
            return distName.trim();
        }
        return "PLMN-PLMN/MRBTS-" + idElemento + "/LNBTS-" + idElemento;
    }
}
