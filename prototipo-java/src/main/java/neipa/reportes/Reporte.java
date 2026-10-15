package neipa.reportes;

import neipa.estructuras.ListaEnlazada;
import neipa.modelo.Auditoria;

/**
 * Contrato comun de los reportes de auditoria. El menu trabaja contra esta
 * interfaz sin conocer que nivel de reporte (tecnico o ejecutivo) tiene.
 */
public interface Reporte {

    String getTitulo();

    String generar(ListaEnlazada<Auditoria> auditorias);
}
