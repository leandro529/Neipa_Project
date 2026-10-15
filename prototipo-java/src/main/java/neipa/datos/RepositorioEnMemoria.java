package neipa.datos;

import java.time.LocalDate;

import neipa.estructuras.ListaEnlazada;
import neipa.excepciones.NeipaException;
import neipa.modelo.Auditoria;
import neipa.modelo.ElementoDeRed;
import neipa.modelo.Excepcion;
import neipa.modelo.Fabricante;
import neipa.modelo.Parametro;
import neipa.modelo.Segmento;
import neipa.modelo.ValorEstandar;

/**
 * Almacen de los datos del prototipo. En esta entrega todo se guarda en
 * memoria con listas enlazadas; en la proxima se reemplaza por la
 * persistencia en la base MySQL definida en la segunda entrega.
 */
public class RepositorioEnMemoria {

    private final ListaEnlazada<Fabricante> fabricantes = new ListaEnlazada<Fabricante>();
    private final ListaEnlazada<Segmento> segmentos = new ListaEnlazada<Segmento>();
    private final ListaEnlazada<ElementoDeRed> elementos = new ListaEnlazada<ElementoDeRed>();
    private final ListaEnlazada<Parametro> parametros = new ListaEnlazada<Parametro>();
    private final ListaEnlazada<ValorEstandar> estandares = new ListaEnlazada<ValorEstandar>();
    private final ListaEnlazada<Excepcion> excepciones = new ListaEnlazada<Excepcion>();
    private final ListaEnlazada<Auditoria> historial = new ListaEnlazada<Auditoria>();

    public void agregarFabricante(Fabricante fabricante) {
        fabricantes.agregarAlFinal(fabricante);
    }

    public void agregarSegmento(Segmento segmento) {
        segmentos.agregarAlFinal(segmento);
    }

    public void agregarParametro(Parametro parametro) {
        parametros.agregarAlFinal(parametro);
    }

    public void agregarEstandar(ValorEstandar estandar) {
        estandares.agregarAlFinal(estandar);
    }

    public void agregarExcepcion(Excepcion excepcion) {
        excepciones.agregarAlFinal(excepcion);
    }

    public void agregarAuditoria(Auditoria auditoria) {
        historial.agregarAlFinal(auditoria);
    }

    /** Agrega un elemento al inventario; no admite nombres repetidos. */
    public void agregarElemento(ElementoDeRed elemento) throws NeipaException {
        if (existeElemento(elemento.getNombre())) {
            throw new NeipaException("Ya existe un elemento de red llamado "
                    + elemento.getNombre() + ".");
        }
        elementos.agregarAlFinal(elemento);
    }

    public boolean existeElemento(String nombre) {
        for (ElementoDeRed elemento : elementos) {
            if (elemento.getNombre().equalsIgnoreCase(nombre.trim())) {
                return true;
            }
        }
        return false;
    }

    /** Estandar de un parametro para un segmento, o null si no esta definido. */
    public ValorEstandar buscarEstandar(Parametro parametro, Segmento segmento) {
        for (ValorEstandar estandar : estandares) {
            if (estandar.getParametro() == parametro && estandar.getSegmento() == segmento) {
                return estandar;
            }
        }
        return null;
    }

    /** Primera excepcion vigente que alcanza al elemento y al parametro, o null. */
    /**
     * Estandar que corresponde a un elemento para un parametro: entre los
     * del segmento del elemento cuyas condiciones se cumplen, el mas
     * especifico (el de mas condiciones). Si empatan, el primero cargado.
     */
    public ValorEstandar buscarEstandar(Parametro parametro, ElementoDeRed elemento) {
        ValorEstandar elegido = null;
        for (ValorEstandar estandar : estandares) {
            if (estandar.getParametro() == parametro && estandar.aplicaA(elemento)
                    && (elegido == null
                        || estandar.getCantidadCondiciones() > elegido.getCantidadCondiciones())) {
                elegido = estandar;
            }
        }
        return elegido;
    }

    public Excepcion buscarExcepcionVigente(ElementoDeRed elemento, Parametro parametro,
            LocalDate fecha) {
        for (Excepcion excepcion : excepciones) {
            if (excepcion.estaVigente(fecha) && excepcion.aplicaA(elemento, parametro)) {
                return excepcion;
            }
        }
        return null;
    }

    /** Parametros del catalogo que pertenecen a un fabricante. */
    public ListaEnlazada<Parametro> parametrosDe(Fabricante fabricante) {
        ListaEnlazada<Parametro> resultado = new ListaEnlazada<Parametro>();
        for (Parametro parametro : parametros) {
            if (parametro.getFabricante() == fabricante) {
                resultado.agregarAlFinal(parametro);
            }
        }
        return resultado;
    }

    public ListaEnlazada<Fabricante> getFabricantes() {
        return fabricantes;
    }

    public ListaEnlazada<Segmento> getSegmentos() {
        return segmentos;
    }

    public ListaEnlazada<ElementoDeRed> getElementos() {
        return elementos;
    }

    public ListaEnlazada<Parametro> getParametros() {
        return parametros;
    }

    public ListaEnlazada<ValorEstandar> getEstandares() {
        return estandares;
    }

    public ListaEnlazada<Excepcion> getExcepciones() {
        return excepciones;
    }

    public ListaEnlazada<Auditoria> getHistorial() {
        return historial;
    }
}
