package com.neipa.web;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.neipa.catalogo.CatalogoService;
import com.neipa.catalogo.ParametroCatalogo;

/**
 * Catalogo de parametros de Nokia (diccionarios LTE / SBTS / 5G):
 * importacion del CSV, busqueda y ficha de un parametro.
 */
@RestController
@RequestMapping("/api/catalogo")
public class CatalogoController {

    private final CatalogoService catalogoService;

    public CatalogoController(CatalogoService catalogoService) {
        this.catalogoService = catalogoService;
    }

    /** CSV disponibles en la carpeta de datos (neipa.cm.carpeta). */
    @GetMapping("/archivos")
    public List<String> archivos() {
        return catalogoService.archivosDisponibles();
    }

    /** Importa y reemplaza el catalogo. Body: {"archivo": "catalogo_parametros_nokia.csv"}. */
    @PostMapping("/importaciones")
    public Map<String, Object> importar(@RequestBody Map<String, String> pedido) {
        return catalogoService.importar(pedido == null ? null : pedido.get("archivo"));
    }

    /** Clases (objetos gestionados) con su ruta y sus objetos padre utilizables en condiciones. */
    @GetMapping("/clases")
    public List<Map<String, Object>> clases() {
        return catalogoService.clases();
    }

    /** Parametros por fuente. */
    @GetMapping("/resumen")
    public List<Map<String, Object>> resumen() {
        return catalogoService.resumen();
    }

    @GetMapping
    public List<ParametroCatalogo> buscar(@RequestParam(required = false) String texto,
                                          @RequestParam(required = false) String clase,
                                          @RequestParam(defaultValue = "100") int limite) {
        return catalogoService.buscar(texto, clase, limite);
    }

    /** Ficha de un parametro para la clase de una regla (objetoGestionado vacio = LNBTS). */
    @GetMapping("/ficha")
    public List<ParametroCatalogo> ficha(@RequestParam String parametro,
                                         @RequestParam(required = false) String objetoGestionado) {
        return catalogoService.ficha(objetoGestionado, parametro);
    }
}
