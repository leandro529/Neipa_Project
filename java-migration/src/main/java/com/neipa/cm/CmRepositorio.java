package com.neipa.cm;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.neipa.domain.Auditoria;

/**
 * Persistencia de los datos de CM importados y de los resultados de las
 * corridas. Separada en una interfaz para poder probar la importacion y la
 * auditoria sin MySQL.
 */
public interface CmRepositorio {

    long crearImportacion(String archivo);

    void actualizarImportacion(long id, String estado, String fechaDump, long objetos, long valores,
                               String mensaje, boolean terminada);

    List<Map<String, Object>> listarImportaciones();

    /** Upsert de objetos por distName. */
    void guardarObjetos(List<CmObjeto> objetos, long idImportacion);

    /**
     * Borra los objetos de la clase (y sus valores) que no vinieron en esta
     * importacion: el dump nuevo reemplaza al anterior (sitios dados de baja,
     * celdas borradas, etc.).
     */
    void borrarObjetosAnteriores(String clase, long idImportacion);

    /** Borra los valores de esos parametros de la clase (antes de reimportarlos). */
    void borrarValores(String clase, Collection<String> parametros);

    /** Inserta/actualiza los valores de los objetos (CmObjeto.getValores()). */
    void guardarValores(List<CmObjeto> objetos);

    /**
     * Objetos de una clase y un RC, con los valores de los parametros pedidos
     * (los que no tienen valor no aparecen en el mapa).
     */
    List<CmObjeto> objetos(String clase, String rc, Collection<String> parametros);

    /** Cantidad de objetos por clase y RC. */
    List<Map<String, Object>> resumenObjetos();

    /** Inserta las filas de auditoria (desvios y exceptuados) de una corrida. */
    void guardarAuditorias(List<Auditoria> auditorias);

    /** Resumen por regla de una corrida: id_valor_estandar -> {clase, parametro, conformes, desvios, exceptuados}. */
    void guardarResumen(long idCorrida, Collection<ResumenRegla> resumen);

    List<ResumenRegla> resumenDeCorrida(long idCorrida);
}
