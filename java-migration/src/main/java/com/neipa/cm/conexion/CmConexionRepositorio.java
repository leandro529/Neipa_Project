package com.neipa.cm.conexion;

import java.util.List;
import java.util.Optional;

/** Persistencia de las conexiones de CM. */
public interface CmConexionRepositorio {

    List<CmConexion> listar();

    Optional<CmConexion> buscar(long id);

    Optional<CmConexion> buscarPorNombre(String nombre);

    /** Alta (id null) o modificacion; devuelve la conexion con su id. */
    CmConexion guardar(CmConexion conexion);

    void borrar(long id);

    void registrarPrueba(long id, String resultado);
}
