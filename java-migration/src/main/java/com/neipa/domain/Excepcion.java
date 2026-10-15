package com.neipa.domain;

import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * Entidad de dominio "Excepcion" (RF05-08): permite que un parametro fuera de
 * rango no se marque como DESVIO para un elemento, segmento, o todo el
 * sistema (segun {@link AlcanceExcepcion}), durante una ventana de vigencia.
 * Mapea 1:1 contra la tabla "excepciones", nueva en esta ampliacion (no
 * estaba en el modelo relacional de 4 tablas de la segunda entrega).
 *
 * Cargada desde la web de configuracion (ExcepcionController) por los
 * ingenieros de radiofrecuencia, es consultada por AuditoriaItemProcessor
 * (via ExcepcionService) antes de marcar una auditoria como DESVIO.
 */
@Entity
@Table(name = "excepciones")
public class Excepcion {

    public static final String TODOS_LOS_PARAMETROS = "*";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_excepcion")
    private Long idExcepcion;

    @Enumerated(EnumType.STRING)
    @Column(name = "alcance", length = 20, nullable = false)
    private AlcanceExcepcion alcance;

    /** Solo se usa cuando alcance = ELEMENTO. */
    @Column(name = "id_elemento")
    private Long idElemento;

    /** Solo se usa cuando alcance = SEGMENTO. */
    @Column(name = "id_segmento")
    private Long idSegmento;

    /** Para alcance PATRON: patron sobre el nombre del elemento, ej "*_test_*". */
    @Column(name = "patron_nombre", length = 80)
    private String patronNombre;

    /** Nombre del parametro exceptuado, o "*" para todos los parametros. */
    @Column(name = "parametro", length = 60, nullable = false)
    private String parametro;

    @Column(name = "motivo", length = 255)
    private String motivo;

    @Column(name = "vigente_desde")
    private LocalDateTime vigenteDesde;

    @Column(name = "vigente_hasta")
    private LocalDateTime vigenteHasta;

    @Column(name = "activa", nullable = false)
    private boolean activa = true;

    public Excepcion() {
    }

    public Long getIdExcepcion() {
        return idExcepcion;
    }

    public void setIdExcepcion(Long idExcepcion) {
        this.idExcepcion = idExcepcion;
    }

    public AlcanceExcepcion getAlcance() {
        return alcance;
    }

    public void setAlcance(AlcanceExcepcion alcance) {
        this.alcance = alcance;
    }

    public Long getIdElemento() {
        return idElemento;
    }

    public void setIdElemento(Long idElemento) {
        this.idElemento = idElemento;
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

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }

    public LocalDateTime getVigenteDesde() {
        return vigenteDesde;
    }

    public void setVigenteDesde(LocalDateTime vigenteDesde) {
        this.vigenteDesde = vigenteDesde;
    }

    public LocalDateTime getVigenteHasta() {
        return vigenteHasta;
    }

    public void setVigenteHasta(LocalDateTime vigenteHasta) {
        this.vigenteHasta = vigenteHasta;
    }

    public boolean isActiva() {
        return activa;
    }

    public void setActiva(boolean activa) {
        this.activa = activa;
    }

    /**
     * True si, a la fecha dada, la excepcion esta activa y dentro de su
     * ventana de vigencia (los limites son opcionales: null = sin limite).
     */
    public boolean esVigenteEn(LocalDateTime fecha) {
        if (!activa) {
            return false;
        }
        if (vigenteDesde != null && fecha.isBefore(vigenteDesde)) {
            return false;
        }
        if (vigenteHasta != null && fecha.isAfter(vigenteHasta)) {
            return false;
        }
        return true;
    }

    public String getPatronNombre() {
        return patronNombre;
    }

    public void setPatronNombre(String patronNombre) {
        this.patronNombre = patronNombre;
    }

    /** True si la excepcion cubre el parametro (mismo nombre, o "*"). */
    public boolean cubreParametro(String nombreParametro) {
        return TODOS_LOS_PARAMETROS.equals(parametro) || (parametro != null && parametro.equals(nombreParametro));
    }

    /** Valida los datos obligatorios segun el alcance, antes de guardar. */
    public void validar() {
        if (alcance == null) {
            throw new IllegalArgumentException("Falta el alcance de la excepcion");
        }
        if (parametro == null || parametro.trim().isEmpty()) {
            throw new IllegalArgumentException("Falta el parametro (o \"*\" para todos)");
        }
        if (alcance == AlcanceExcepcion.ELEMENTO && idElemento == null) {
            throw new IllegalArgumentException("Una excepcion de alcance ELEMENTO necesita el elemento");
        }
        if (alcance == AlcanceExcepcion.SEGMENTO && idSegmento == null) {
            throw new IllegalArgumentException("Una excepcion de alcance SEGMENTO necesita el segmento");
        }
        if (alcance == AlcanceExcepcion.PATRON && (patronNombre == null || patronNombre.trim().isEmpty())) {
            throw new IllegalArgumentException("Una excepcion de alcance PATRON necesita el patron de nombre");
        }
    }
}
