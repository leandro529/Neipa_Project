package neipa.control;

import java.time.LocalDate;

import neipa.datos.RepositorioEnMemoria;
import neipa.estructuras.Cola;
import neipa.estructuras.ListaEnlazada;
import neipa.excepciones.ExtraccionException;
import neipa.excepciones.NeipaException;
import neipa.modelo.Auditoria;
import neipa.modelo.ElementoDeRed;
import neipa.modelo.Excepcion;
import neipa.modelo.Segmento;
import neipa.modelo.ValorEstandar;
import neipa.modelo.ValorVigente;

/**
 * Clase de control de CU001 Auditar parametros de configuracion. Coordina
 * las clases de entidad siguiendo el diagrama de secuencia de la segunda
 * entrega (Figura 8).
 */
public class ControladorAuditoria {

    /** Porcentaje de elementos con desvio a partir del cual la correccion es un cambio masivo. */
    public static final double UMBRAL_CAMBIO_MASIVO_PCT = 30.0;

    private final RepositorioEnMemoria repositorio;
    private Segmento segmentoActual;
    private ListaEnlazada<Auditoria> ultimaAuditoria;
    private ListaEnlazada<String> erroresDeExtraccion;
    private int elementosAuditados;
    private int elementosConDesvio;

    public ControladorAuditoria(RepositorioEnMemoria repositorio) {
        this.repositorio = repositorio;
        this.ultimaAuditoria = new ListaEnlazada<Auditoria>();
        this.erroresDeExtraccion = new ListaEnlazada<String>();
    }

    /**
     * Audita todos los elementos de un segmento. Los elementos se procesan en
     * orden de llegada mediante una cola. Si la extraccion de un elemento
     * falla, se registra el error y se continua con el siguiente.
     */
    public ListaEnlazada<Auditoria> ejecutarAuditoria(Segmento segmento) throws NeipaException {
        ListaEnlazada<ElementoDeRed> grupo = segmento.agrupar(repositorio.getElementos());
        if (grupo.estaVacia()) {
            throw new NeipaException("El segmento " + segmento.getNombre()
                    + " no tiene elementos de red para auditar.");
        }

        segmentoActual = segmento;
        ultimaAuditoria = new ListaEnlazada<Auditoria>();
        erroresDeExtraccion = new ListaEnlazada<String>();
        elementosAuditados = 0;
        elementosConDesvio = 0;

        Cola<ElementoDeRed> pendientes = new Cola<ElementoDeRed>();
        for (ElementoDeRed elemento : grupo) {
            pendientes.encolar(elemento);
        }

        LocalDate hoy = LocalDate.now();
        while (!pendientes.estaVacia()) {
            ElementoDeRed elemento = pendientes.desencolar();
            try {
                ListaEnlazada<ValorVigente> vigentes = elemento.obtenerParametrosVigentes();
                boolean tieneDesvio = false;
                for (ValorVigente vigente : vigentes) {
                    ValorEstandar estandar =
                            repositorio.buscarEstandar(vigente.getParametro(), elemento);
                    if (estandar == null) {
                        continue; // sin estandar definido para el segmento: no se audita
                    }
                    Excepcion excepcion = repositorio.buscarExcepcionVigente(
                            elemento, vigente.getParametro(), hoy);
                    Auditoria resultado = compararYValidarRango(elemento, vigente, estandar, excepcion);
                    if (resultado.esDesvio()) {
                        tieneDesvio = true;
                    }
                }
                elementosAuditados++;
                if (tieneDesvio) {
                    elementosConDesvio++;
                }
            } catch (ExtraccionException e) {
                erroresDeExtraccion.agregarAlFinal(e.getMessage());
            }
        }
        return ultimaAuditoria;
    }

    /**
     * Compara un valor vigente contra su estandar, valida el rango permitido
     * y registra el resultado en el historial (pasos 6 y 7 de la Figura 8).
     */
    public Auditoria compararYValidarRango(ElementoDeRed elemento, ValorVigente vigente,
            ValorEstandar estandar, Excepcion excepcionVigente) {
        Auditoria auditoria = new Auditoria(elemento, vigente.getParametro(),
                vigente.obtenerValor(), estandar);
        auditoria.compararConEstandar(excepcionVigente);
        ultimaAuditoria.agregarAlFinal(auditoria);
        repositorio.agregarAuditoria(auditoria);
        return auditoria;
    }

    /** Porcentaje de elementos con al menos un desvio sobre el total auditado. */
    public double porcentajeDesvio() {
        if (elementosAuditados == 0) {
            return 0;
        }
        return elementosConDesvio * 100.0 / elementosAuditados;
    }

    public boolean superaUmbral() {
        return porcentajeDesvio() > UMBRAL_CAMBIO_MASIVO_PCT;
    }

    /** Desvios de la ultima auditoria ejecutada, candidatos a correccion. */
    public ListaEnlazada<Auditoria> desviosDeUltimaAuditoria() {
        ListaEnlazada<Auditoria> desvios = new ListaEnlazada<Auditoria>();
        for (Auditoria auditoria : ultimaAuditoria) {
            if (auditoria.esDesvio()) {
                desvios.agregarAlFinal(auditoria);
            }
        }
        return desvios;
    }

    public Segmento getSegmentoActual() {
        return segmentoActual;
    }

    public ListaEnlazada<Auditoria> getUltimaAuditoria() {
        return ultimaAuditoria;
    }

    public ListaEnlazada<String> getErroresDeExtraccion() {
        return erroresDeExtraccion;
    }

    public int getElementosAuditados() {
        return elementosAuditados;
    }

    public int getElementosConDesvio() {
        return elementosConDesvio;
    }
}
