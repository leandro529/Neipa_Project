package com.neipa.domain;

import java.util.Locale;
import java.util.Map;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import com.neipa.validation.ValidadorRango;

/**
 * Condicion que tiene que cumplirse para que un {@link ValorEstandar} (una
 * regla) se aplique a un elemento. Una regla puede tener varias condiciones
 * y se aplica solo si se cumplen TODAS (Y logico). Permite expresar, para un
 * mismo parametro, valores esperados distintos segun el elemento: por su
 * nombre, tecnologia o fabricante, o segun el valor vigente de otro
 * parametro del mismo elemento.
 *
 * Usa los mismos operadores que la regla (OperadorComparacion), salvo RANGO:
 * un rango se expresa con dos condiciones (MAYOR_IGUAL y MENOR_IGUAL).
 */
@Entity
@Table(name = "condiciones_estandar")
public class CondicionEstandar {

    public static final String CAMPO_NOMBRE = "NOMBRE";
    public static final String CAMPO_TECNOLOGIA = "TECNOLOGIA";
    public static final String CAMPO_FABRICANTE = "FABRICANTE";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_condicion")
    private Long idCondicion;

    @Enumerated(EnumType.STRING)
    @Column(name = "origen", length = 20, nullable = false)
    private OrigenCondicion origen;

    /** NOMBRE / TECNOLOGIA / FABRICANTE, o el nombre del otro parametro. */
    @Column(name = "campo", length = 60, nullable = false)
    private String campo;

    @Enumerated(EnumType.STRING)
    @Column(name = "operador", length = 20, nullable = false)
    private OperadorComparacion operador;

    @Column(name = "valor", length = 120, nullable = false)
    private String valor;

    public CondicionEstandar() {
    }

    public CondicionEstandar(OrigenCondicion origen, String campo, OperadorComparacion operador, String valor) {
        this.origen = origen;
        this.campo = campo;
        this.operador = operador;
        this.valor = valor;
    }

    /**
     * Evalua la condicion contra un elemento y los valores vigentes de sus
     * parametros (nombre del parametro -> valor), extraidos en la misma
     * pasada de auditoria. Si el dato no existe (atributo vacio, o el otro
     * parametro no se extrajo), la condicion no se cumple.
     */
    public boolean seCumple(ElementoDeRed elemento, Map<String, String> valoresVigentes) {
        String dato = obtenerDato(elemento, valoresVigentes);
        if (dato == null) {
            return false;
        }
        try {
            return ValidadorRango.cumple(dato, operador, valor);
        } catch (IllegalArgumentException ex) {
            // Ej: MENOR_QUE contra un dato no numerico: no aplica.
            return false;
        }
    }

    private String obtenerDato(ElementoDeRed elemento, Map<String, String> valoresVigentes) {
        if (origen == OrigenCondicion.PARAMETRO) {
            return valoresVigentes.get(campo);
        }
        switch (campo.toUpperCase(Locale.ROOT)) {
            case CAMPO_NOMBRE:
                return elemento.getNombre();
            case CAMPO_TECNOLOGIA:
                return elemento.getTecnologia();
            case CAMPO_FABRICANTE:
                return elemento.getFabricante();
            default:
                throw new IllegalArgumentException("Atributo de elemento desconocido en una condicion: " + campo);
        }
    }

    /** Valida que la condicion este bien formada antes de guardarla. */
    public void validar() {
        if (origen == null || operador == null || campo == null || campo.trim().isEmpty()
                || valor == null || valor.trim().isEmpty()) {
            throw new IllegalArgumentException("Cada condicion necesita origen, campo, operador y valor");
        }
        if (operador == OperadorComparacion.RANGO) {
            throw new IllegalArgumentException(
                    "Una condicion no puede usar RANGO: usar dos condiciones (MAYOR_IGUAL y MENOR_IGUAL)");
        }
        if (origen == OrigenCondicion.ATRIBUTO_ELEMENTO) {
            String c = campo.toUpperCase(Locale.ROOT);
            if (!c.equals(CAMPO_NOMBRE) && !c.equals(CAMPO_TECNOLOGIA) && !c.equals(CAMPO_FABRICANTE)) {
                throw new IllegalArgumentException(
                        "Atributo de elemento invalido: " + campo + " (usar NOMBRE, TECNOLOGIA o FABRICANTE)");
            }
            campo = c;
        }
    }

    public String comoTexto() {
        String sujeto = origen == OrigenCondicion.PARAMETRO ? campo : campo.toLowerCase(Locale.ROOT);
        return sujeto + " " + ValorEstandar.simbolo(operador) + " " + valor;
    }

    public Long getIdCondicion() {
        return idCondicion;
    }

    public void setIdCondicion(Long idCondicion) {
        this.idCondicion = idCondicion;
    }

    public OrigenCondicion getOrigen() {
        return origen;
    }

    public void setOrigen(OrigenCondicion origen) {
        this.origen = origen;
    }

    public String getCampo() {
        return campo;
    }

    public void setCampo(String campo) {
        this.campo = campo;
    }

    public OperadorComparacion getOperador() {
        return operador;
    }

    public void setOperador(OperadorComparacion operador) {
        this.operador = operador;
    }

    public String getValor() {
        return valor;
    }

    public void setValor(String valor) {
        this.valor = valor;
    }
}
