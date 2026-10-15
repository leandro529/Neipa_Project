package neipa.control;

import neipa.estructuras.Pila;
import neipa.excepciones.ElementoNoEncontradoException;
import neipa.excepciones.ExtraccionException;
import neipa.excepciones.NeipaException;
import neipa.modelo.Auditoria;
import neipa.modelo.CambioConfiguracion;
import neipa.modelo.ElementoDeRed;
import neipa.modelo.Parametro;
import neipa.modelo.ValorEstandar;
import neipa.modelo.ValorVigente;

/**
 * Clase de control de las correcciones y su rollback (CU005). Cada
 * correccion aplicada se guarda en una pila: el rollback deshace siempre
 * la ultima.
 */
public class ControladorRollback {

    private final Pila<CambioConfiguracion> cambiosAplicados = new Pila<CambioConfiguracion>();

    /** Lleva el parametro al valor estandar y guarda el cambio para poder revertirlo. */
    public CambioConfiguracion aplicarCorreccion(Auditoria desvio) throws NeipaException {
        if (!desvio.esDesvio()) {
            throw new NeipaException("Solo se pueden corregir resultados con desvio.");
        }
        ElementoDeRed elemento = desvio.getElementoDeRed();
        Parametro parametro = desvio.getParametro();
        ValorEstandar estandar = desvio.getValorEstandar();
        verificarPlataforma(elemento);

        ValorVigente vigente = elemento.buscarValorVigente(parametro);
        if (vigente == null) {
            throw new ElementoNoEncontradoException("El elemento " + elemento.getNombre()
                    + " no tiene cargado el parametro " + parametro.getNombre() + ".");
        }
        String valorAnterior = vigente.obtenerValor();
        if (parametro.validarRango(valorAnterior) && estandar.cumple(valorAnterior)) {
            throw new NeipaException("El valor vigente (" + valorAnterior
                    + ") ya cumple el estandar: no hay nada para corregir.");
        }

        String valorNuevo = estandar.valorObjetivo(valorAnterior);
        elemento.registrarValorVigente(parametro, valorNuevo);
        CambioConfiguracion cambio =
                new CambioConfiguracion(elemento, parametro, valorAnterior, valorNuevo);
        cambiosAplicados.apilar(cambio);
        return cambio;
    }

    /** Deshace la ultima correccion aplicada y devuelve el cambio revertido. */
    public CambioConfiguracion ejecutarRollback() throws NeipaException {
        if (cambiosAplicados.estaVacia()) {
            throw new NeipaException("No hay correcciones aplicadas para deshacer.");
        }
        CambioConfiguracion ultimo = cambiosAplicados.verTope();
        verificarPlataforma(ultimo.getElementoDeRed());
        ultimo.getElementoDeRed().registrarValorVigente(
                ultimo.getParametro(), ultimo.getValorAnterior());
        return cambiosAplicados.desapilar();
    }

    private void verificarPlataforma(ElementoDeRed elemento) throws ExtraccionException {
        if (!elemento.getFabricante().isPlataformaDisponible()) {
            throw new ExtraccionException("la plataforma de "
                    + elemento.getFabricante().getNombre() + " no responde (elemento "
                    + elemento.getNombre() + ").");
        }
    }

    public int cantidadDeCambios() {
        return cambiosAplicados.tamanio();
    }
}
