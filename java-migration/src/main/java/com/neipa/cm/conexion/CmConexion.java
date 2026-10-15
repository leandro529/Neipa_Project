package com.neipa.cm.conexion;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.neipa.cm.MotorCm;

/**
 * Conexion a una base con la estructura del CM de NetAct. La contrasena solo
 * se recibe (al crear o cambiar la conexion): nunca se serializa en las
 * respuestas; en su lugar se informa tieneContrasena.
 */
public class CmConexion {

    private Long idConexion;
    private String nombre;
    private MotorCm motor;
    private String url;
    private String usuario;
    private String contrasena;
    private String contrasenaCifrada;
    private String esquema;
    private LocalDateTime creada;
    private LocalDateTime ultimaPrueba;
    private String ultimoResultado;

    public Long getIdConexion() { return idConexion; }
    public void setIdConexion(Long v) { idConexion = v; }
    public String getNombre() { return nombre; }
    public void setNombre(String v) { nombre = v; }
    public MotorCm getMotor() { return motor; }
    public void setMotor(MotorCm v) { motor = v; }
    public String getUrl() { return url; }
    public void setUrl(String v) { url = v; }
    public String getUsuario() { return usuario; }
    public void setUsuario(String v) { usuario = v; }

    /** Solo de entrada (JSON): texto plano recibido del formulario. */
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    public String getContrasena() { return contrasena; }
    public void setContrasena(String v) { contrasena = v; }

    @JsonIgnore
    public String getContrasenaCifrada() { return contrasenaCifrada; }
    @JsonIgnore
    public void setContrasenaCifrada(String v) { contrasenaCifrada = v; }

    public boolean isTieneContrasena() { return contrasenaCifrada != null && !contrasenaCifrada.isEmpty(); }

    public String getEsquema() { return esquema; }
    public void setEsquema(String v) { esquema = v; }
    public LocalDateTime getCreada() { return creada; }
    public void setCreada(LocalDateTime v) { creada = v; }
    public LocalDateTime getUltimaPrueba() { return ultimaPrueba; }
    public void setUltimaPrueba(LocalDateTime v) { ultimaPrueba = v; }
    public String getUltimoResultado() { return ultimoResultado; }
    public void setUltimoResultado(String v) { ultimoResultado = v; }
}
