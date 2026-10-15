package com.neipa.web;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.neipa.catalogo.CatalogoService;
import com.neipa.domain.ValorEstandar;
import com.neipa.exception.RecursoNoEncontradoException;
import com.neipa.repository.ValorEstandarRepository;

/**
 * CRUD de ValorEstandar (RF02, RF04): la regla contra la que se compara el
 * valor vigente de cada parametro de configuracion de red. Es la pantalla
 * principal de "cargar configuraciones" del frontend — reemplaza el mapa
 * hardcodeado que tenia EstandarProvider antes de esta ampliacion.
 */
@RestController
@RequestMapping("/api/configuraciones")
public class ValorEstandarController {

    private final ValorEstandarRepository valorEstandarRepository;
    private final CatalogoService catalogoService;

    public ValorEstandarController(ValorEstandarRepository valorEstandarRepository, CatalogoService catalogoService) {
        this.valorEstandarRepository = valorEstandarRepository;
        this.catalogoService = catalogoService;
    }

    @GetMapping
    public List<ValorEstandar> listar(@RequestParam(required = false) String parametro) {
        if (parametro != null) {
            return valorEstandarRepository.findByParametro(parametro);
        }
        return valorEstandarRepository.findAll();
    }

    @GetMapping("/{id}")
    public ValorEstandar obtener(@PathVariable Long id) {
        return buscarOFallar(id);
    }

    @PostMapping
    public ResponseEntity<ValorEstandar> crear(@RequestBody ValorEstandar valorEstandar) {
        valorEstandar.setIdValorEstandar(null);
        valorEstandar.setCondiciones(new java.util.ArrayList<>(valorEstandar.getCondiciones()));
        valorEstandar.validar();
        catalogoService.validar(valorEstandar);
        return ResponseEntity.ok(valorEstandarRepository.save(valorEstandar));
    }

    @PutMapping("/{id}")
    public ValorEstandar actualizar(@PathVariable Long id, @RequestBody ValorEstandar cambios) {
        ValorEstandar existente = buscarOFallar(id);
        existente.setParametro(cambios.getParametro());
        existente.setOperador(cambios.getOperador());
        existente.setMinimo(cambios.getMinimo());
        existente.setMaximo(cambios.getMaximo());
        existente.setValorReferencia(cambios.getValorReferencia());
        existente.setUnidad(cambios.getUnidad());
        existente.setIdSegmento(cambios.getIdSegmento());
        existente.setPrioridad(cambios.getPrioridad());
        existente.setObjetoGestionado(cambios.getObjetoGestionado());
        existente.setValorObjetivo(cambios.getValorObjetivo());
        existente.setDescripcion(cambios.getDescripcion());
        existente.setCondiciones(cambios.getCondiciones());
        existente.validar();
        catalogoService.validar(existente);
        return valorEstandarRepository.save(existente);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        buscarOFallar(id);
        valorEstandarRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private ValorEstandar buscarOFallar(Long id) {
        return valorEstandarRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el valor estandar con id " + id));
    }
}
