package com.neipa.catalogo;

import java.util.List;
import java.util.Map;

/** Persistencia del catalogo de parametros. */
public interface CatalogoRepositorio {

    /** Reemplaza todo el catalogo por estos parametros; devuelve la cantidad guardada. */
    int reemplazar(List<ParametroCatalogo> parametros);

    /** Todas las fuentes que describen ese parametro de esa clase (puede haber varias releases). */
    List<ParametroCatalogo> de(String clase, String parametro);

    /** Busqueda por texto en el nombre corto o completo, opcionalmente filtrando la clase. */
    List<ParametroCatalogo> buscar(String texto, String clase, int limite);

    /** Clases del catalogo con su ruta (MRBTS/LNBTS/LNCEL/...) y cantidad de parametros. */
    List<Map<String, Object>> clases();

    /** Cantidad de parametros por fuente. */
    List<Map<String, Object>> resumen();
}
