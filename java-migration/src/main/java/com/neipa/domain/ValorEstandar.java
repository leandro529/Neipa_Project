package com.neipa.domain;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.Table;

/**
 * Entidad de dominio "ValorEstandar" (RF02, RF04): la regla contra la que se
 * compara el valor vigente de cada parametro de configuracion de red (RF01,
 * RF03) al auditar un ElementoDeRed (CU001). Reemplaza el mapa en memoria
 * que tenia EstandarProvider en la primera version de esta migracion (ver
 * README, seccion "No se migro") por una tabla propia ("valores_estandar"),
 * para que los ingenieros de RF puedan cargar y mantener esta configuracion
 * desde la web.
 *
 * Originalmente esto era solo un rango (operador RANGO, minimo/maximo — el
 * caso de prueba CPF02 de la segunda entrega). Se amplio (ver registro del
 * proyecto) a partir de las auditorias reales que el usuario ya tenia
 * cargadas fuera de este repo (AUDIT-xx.xlsx): la mayoria de esas reglas no
 * son un rango sino una condicion puntual — "el parametro debe valer
 * exactamente X" o "debe ser menor que X" — de ahi el resto de los
 * operadores de {@link OperadorComparacion}, que usan valorReferencia en vez
 * de minimo/maximo.
 */
@Entity
@Table(name = "valores_estandar")
public class ValorEstandar {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_valor_estandar")
    private Long idValorEstandar;

    @Column(name = "parametro", length = 60, nullable = false)
    private String parametro;

    /**
     * Ampliacion "reglas por segmento": si tiene valor, la regla solo se
     * aplica a los elementos de ese segmento (asi se carga una sola vez la
     * configuracion de todo un segmento). Null = cualquier segmento.
     */
    @Column(name = "id_segmento")
    private Long idSegmento;

    /**
     * Ampliacion "reglas con condiciones": condiciones adicionales (todas se
     * tienen que cumplir) sobre el elemento o sobre otros parametros. Sin
     * condiciones ni segmento, la regla es la general del parametro.
     */
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "id_valor_estandar")
    @OrderBy("idCondicion")
    private List<CondicionEstandar> condiciones = new ArrayList<>();

    /** Desempate manual entre reglas que aplican a la vez: gana la mayor. */
    @Column(name = "prioridad", nullable = false)
    private int prioridad = 0;

    /**
     * Objeto gestionado (MO) donde vive el parametro, relativo al LNBTS del
     * elemento, para armar el distName del XML RAML 2.0 de reconfiguracion.
     * Ej: "LNCEL-1", "LTAC-1". Vacio = el parametro es del propio LNBTS.
     */
    @Column(name = "objeto_gestionado", length = 60)
    private String objetoGestionado;

    /**
     * Valor a escribir en el XML de reconfiguracion cuando hay un desvio.
     * Si esta vacio se deriva de la regla (IGUAL: el valor de referencia;
     * RANGO: el limite mas cercano; etc.), cuando se puede.
     */
    @Column(name = "valor_objetivo", length = 120)
    private String valorObjetivo;

    @Column(name = "descripcion", length = 255)
    private String descripcion;

    @Enumerated(EnumType.STRING)
    @Column(name = "operador", length = 20, nullable = false)
    private OperadorComparacion operador = OperadorComparacion.RANGO;

    @Column(name = "minimo", precision = 10, scale = 2)
    private BigDecimal minimo;

    @Column(name = "maximo", precision = 10, scale = 2)
    private BigDecimal maximo;

    /**
     * Valor de referencia para cualquier operador que no sea RANGO (por
     * ejemplo "500000" para MENOR_QUE, o "fixed" para IGUAL sobre un
     * parametro de texto como ExpectedCellSize). Para EN_LISTA, valores
     * separados por coma; para COINCIDE, un patron con comodines % y _
     * (estilo SQL LIKE).
     */
    @Column(name = "valor_referencia", length = 120)
    private String valorReferencia;

    @Column(name = "unidad", length = 20)
    private String unidad;

    public ValorEstandar() {
    }

    /** Constructor original (RF04): rango minimo/maximo. */
    public ValorEstandar(String parametro, BigDecimal minimo, BigDecimal maximo, String unidad) {
        this.parametro = parametro;
        this.operador = OperadorComparacion.RANGO;
        this.minimo = minimo;
        this.maximo = maximo;
        this.unidad = unidad;
    }

    /** Constructor para el resto de los operadores (valor de referencia unico). */
    public ValorEstandar(String parametro, OperadorComparacion operador, String valorReferencia, String unidad) {
        this.parametro = parametro;
        this.operador = operador;
        this.valorReferencia = valorReferencia;
        this.unidad = unidad;
    }

    public Long getIdValorEstandar() {
        return idValorEstandar;
    }

    public void setIdValorEstandar(Long idValorEstandar) {
        this.idValorEstandar = idValorEstandar;
    }

    public String getParametro() {
        return parametro;
    }

    public void setParametro(String parametro) {
        this.parametro = parametro;
    }

    public OperadorComparacion getOperador() {
        return operador;
    }

    public void setOperador(OperadorComparacion operador) {
        this.operador = operador;
    }

    public BigDecimal getMinimo() {
        return minimo;
    }

    public void setMinimo(BigDecimal minimo) {
        this.minimo = minimo;
    }

    public BigDecimal getMaximo() {
        return maximo;
    }

    public void setMaximo(BigDecimal maximo) {
        this.maximo = maximo;
    }

    public String getValorReferencia() {
        return valorReferencia;
    }

    public void setValorReferencia(String valorReferencia) {
        this.valorReferencia = valorReferencia;
    }

    public String getUnidad() {
        return unidad;
    }

    public void setUnidad(String unidad) {
        this.unidad = unidad;
    }

    /** Texto de referencia para guardar en auditorias.valor_estandar. */
    public String comoTexto() {
        if (operador == null || operador == OperadorComparacion.RANGO) {
            return minimo + "-" + maximo;
        }
        return simbolo(operador) + " " + valorReferencia;
    }

    public static String simbolo(OperadorComparacion operador) {
        switch (operador) {
            case RANGO:
                return "entre";
            case IGUAL:
                return "=";
            case DISTINTO:
                return "<>";
            case MENOR_QUE:
                return "<";
            case MENOR_IGUAL:
                return "<=";
            case MAYOR_QUE:
                return ">";
            case MAYOR_IGUAL:
                return ">=";
            case EN_LISTA:
                return "in";
            case COINCIDE:
                return "like";
            default:
                throw new IllegalStateException("Operador no soportado: " + operador);
        }
    }

    /** Texto de las condiciones de aplicacion, para mostrar y auditar. */
    public String condicionesComoTexto() {
        List<String> partes = new ArrayList<>();
        if (idSegmento != null) {
            partes.add("segmento = " + idSegmento);
        }
        partes.addAll(condiciones.stream().map(CondicionEstandar::comoTexto).collect(Collectors.toList()));
        return partes.isEmpty() ? "general" : String.join(" y ", partes);
    }

    /**
     * Indica si la regla se aplica al elemento: tiene que estar en el
     * segmento de la regla (si tiene) y cumplir todas sus condiciones.
     */
    public boolean aplicaA(ElementoDeRed elemento, Map<String, String> valoresVigentes) {
        if (idSegmento != null && !idSegmento.equals(elemento.getIdSegmento())) {
            return false;
        }
        for (CondicionEstandar condicion : condiciones) {
            if (!condicion.seCumple(elemento, valoresVigentes)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Cuanto mas especifica es la regla, mas pesa al elegir entre varias que
     * aplican: el segmento cuenta como una condicion mas.
     */
    public int especificidad() {
        return condiciones.size() + (idSegmento != null ? 1 : 0);
    }

    /**
     * Valor que deberia tener el parametro para dejar de estar en desvio.
     * Usa valorObjetivo si se cargo; si no, lo deriva de la regla. Vacio si
     * no se puede derivar (ej. "< 500000" o "<> 3": hay infinitos valores
     * validos y no corresponde inventar uno).
     */
    public Optional<String> valorObjetivoPara(String valorVigente) {
        if (valorObjetivo != null && !valorObjetivo.trim().isEmpty()) {
            return Optional.of(valorObjetivo.trim());
        }
        OperadorComparacion op = operador == null ? OperadorComparacion.RANGO : operador;
        switch (op) {
            case RANGO:
                return limiteMasCercano(valorVigente);
            case IGUAL:
            case MENOR_IGUAL:
            case MAYOR_IGUAL:
                return Optional.ofNullable(valorReferencia).map(String::trim);
            case EN_LISTA:
                return valorReferencia == null ? Optional.empty()
                        : Optional.of(valorReferencia.split(",")[0].trim());
            default:
                return Optional.empty();
        }
    }

    private Optional<String> limiteMasCercano(String valorVigente) {
        if (minimo == null || maximo == null) {
            return Optional.empty();
        }
        BigDecimal vigente;
        try {
            vigente = new BigDecimal(valorVigente.trim());
        } catch (NumberFormatException | NullPointerException ex) {
            return Optional.empty();
        }
        BigDecimal limite = vigente.compareTo(minimo) < 0 ? minimo : maximo;
        return Optional.of(limite.stripTrailingZeros().toPlainString());
    }

    /** Valida la regla y sus condiciones antes de guardarla. */
    public void validar() {
        if (parametro == null || parametro.trim().isEmpty()) {
            throw new IllegalArgumentException("Falta el nombre del parametro");
        }
        OperadorComparacion op = operador == null ? OperadorComparacion.RANGO : operador;
        if (op == OperadorComparacion.RANGO) {
            if (minimo == null || maximo == null) {
                throw new IllegalArgumentException("Una regla RANGO necesita minimo y maximo");
            }
            if (minimo.compareTo(maximo) > 0) {
                throw new IllegalArgumentException("El minimo no puede ser mayor que el maximo");
            }
        } else if (valorReferencia == null || valorReferencia.trim().isEmpty()) {
            throw new IllegalArgumentException("Falta el valor de referencia para el operador " + op);
        }
        if (condiciones == null) {
            condiciones = new ArrayList<>();
        }
        condiciones.forEach(CondicionEstandar::validar);
    }

    public Long getIdSegmento() {
        return idSegmento;
    }

    public void setIdSegmento(Long idSegmento) {
        this.idSegmento = idSegmento;
    }

    public List<CondicionEstandar> getCondiciones() {
        return condiciones;
    }

    /** Reemplaza las condiciones manteniendo la misma lista (orphanRemoval). */
    public void setCondiciones(List<CondicionEstandar> nuevas) {
        if (this.condiciones == null) {
            this.condiciones = new ArrayList<>();
        }
        this.condiciones.clear();
        if (nuevas != null) {
            for (CondicionEstandar c : nuevas) {
                c.setIdCondicion(null);
                this.condiciones.add(c);
            }
        }
    }

    public int getPrioridad() {
        return prioridad;
    }

    public void setPrioridad(int prioridad) {
        this.prioridad = prioridad;
    }

    public String getObjetoGestionado() {
        return objetoGestionado;
    }

    public void setObjetoGestionado(String objetoGestionado) {
        this.objetoGestionado = objetoGestionado;
    }

    public String getValorObjetivo() {
        return valorObjetivo;
    }

    public void setValorObjetivo(String valorObjetivo) {
        this.valorObjetivo = valorObjetivo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }
}
