package com.neipa.domain;

import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * Entidad de dominio "Auditoria": resultado de comparar el valor vigente de un
 * parametro contra su valor/rango estandar para un ElementoDeRed (CU001).
 * Mapea 1:1 contra la tabla "auditorias".
 *
 * idExcepcionAplicada queda en null salvo que el resultado sea EXCEPTUADO,
 * en cuyo caso referencia la Excepcion (RF05-08) que evito que se marcara
 * como DESVIO — para trazabilidad (RF29/RNF: toda excepcion aplicada debe
 * quedar registrada).
 */
@Entity
@Table(name = "auditorias")
public class Auditoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_auditoria")
    private Long idAuditoria;

    @Column(name = "id_elemento")
    private Long idElemento;

    @Column(name = "parametro", length = 60)
    private String parametro;

    @Column(name = "valor_vigente", length = 30)
    private String valorVigente;

    @Column(name = "valor_estandar", length = 120)
    private String valorEstandar;

    @Column(name = "resultado", length = 20)
    private String resultado;

    @Column(name = "fecha")
    private LocalDateTime fecha;

    @Column(name = "id_excepcion_aplicada")
    private Long idExcepcionAplicada;

    /** Regla (ValorEstandar) que se eligio para este elemento y parametro. */
    @Column(name = "id_valor_estandar")
    private Long idValorEstandar;

    /** Condiciones de la regla elegida, en texto ("general" si no tiene). */
    @Column(name = "regla_aplicada", length = 255)
    private String reglaAplicada;

    /** Valor a configurar para corregir el desvio (si se pudo determinar). */
    @Column(name = "valor_objetivo", length = 120)
    private String valorObjetivo;

    /** Corrida (auditoria sobre datos de CM) que genero la fila; null = auditoria simulada. */
    @Column(name = "id_corrida")
    private Long idCorrida;

    /** Objeto gestionado auditado (solo auditorias de CM). */
    @Column(name = "dist_name", length = 255)
    private String distName;

    @Column(name = "nombre_objeto", length = 120)
    private String nombreObjeto;

    public Auditoria() {
    }

    public Auditoria(Long idElemento, String parametro, String valorVigente,
                      String valorEstandar, ResultadoAuditoria resultado, LocalDateTime fecha) {
        this(idElemento, parametro, valorVigente, valorEstandar, resultado, fecha, null);
    }

    public Auditoria(Long idElemento, String parametro, String valorVigente,
                      String valorEstandar, ResultadoAuditoria resultado, LocalDateTime fecha,
                      Long idExcepcionAplicada) {
        this.idElemento = idElemento;
        this.parametro = parametro;
        this.valorVigente = valorVigente;
        this.valorEstandar = valorEstandar;
        this.resultado = resultado.toString();
        this.fecha = fecha;
        this.idExcepcionAplicada = idExcepcionAplicada;
    }

    public Long getIdAuditoria() {
        return idAuditoria;
    }

    public void setIdAuditoria(Long idAuditoria) {
        this.idAuditoria = idAuditoria;
    }

    public Long getIdElemento() {
        return idElemento;
    }

    public void setIdElemento(Long idElemento) {
        this.idElemento = idElemento;
    }

    public String getParametro() {
        return parametro;
    }

    public void setParametro(String parametro) {
        this.parametro = parametro;
    }

    public String getValorVigente() {
        return valorVigente;
    }

    public void setValorVigente(String valorVigente) {
        this.valorVigente = valorVigente;
    }

    public String getValorEstandar() {
        return valorEstandar;
    }

    public void setValorEstandar(String valorEstandar) {
        this.valorEstandar = valorEstandar;
    }

    public String getResultado() {
        return resultado;
    }

    public void setResultado(String resultado) {
        this.resultado = resultado;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public void setFecha(LocalDateTime fecha) {
        this.fecha = fecha;
    }

    public Long getIdExcepcionAplicada() {
        return idExcepcionAplicada;
    }

    public void setIdExcepcionAplicada(Long idExcepcionAplicada) {
        this.idExcepcionAplicada = idExcepcionAplicada;
    }

    public Long getIdValorEstandar() {
        return idValorEstandar;
    }

    public void setIdValorEstandar(Long idValorEstandar) {
        this.idValorEstandar = idValorEstandar;
    }

    public String getReglaAplicada() {
        return reglaAplicada;
    }

    public void setReglaAplicada(String reglaAplicada) {
        this.reglaAplicada = reglaAplicada;
    }

    public String getValorObjetivo() {
        return valorObjetivo;
    }

    public void setValorObjetivo(String valorObjetivo) {
        this.valorObjetivo = valorObjetivo;
    }

    public Long getIdCorrida() {
        return idCorrida;
    }

    public void setIdCorrida(Long idCorrida) {
        this.idCorrida = idCorrida;
    }

    public String getDistName() {
        return distName;
    }

    public void setDistName(String distName) {
        this.distName = distName;
    }

    public String getNombreObjeto() {
        return nombreObjeto;
    }

    public void setNombreObjeto(String nombreObjeto) {
        this.nombreObjeto = nombreObjeto;
    }
}
