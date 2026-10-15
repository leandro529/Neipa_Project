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

import com.neipa.domain.ElementoDeRed;
import com.neipa.exception.RecursoNoEncontradoException;
import com.neipa.repository.ElementoDeRedRepository;

/**
 * CRUD de ElementoDeRed (sitio/celda/core/antena de un fabricante, dentro de
 * un Segmento). Pantalla "Configuraciones" del frontend: es el inventario
 * sobre el que despues corre la auditoria (CU001) por segmento.
 */
@RestController
@RequestMapping("/api/elementos")
public class ElementoDeRedController {

    private final ElementoDeRedRepository elementoDeRedRepository;

    public ElementoDeRedController(ElementoDeRedRepository elementoDeRedRepository) {
        this.elementoDeRedRepository = elementoDeRedRepository;
    }

    @GetMapping
    public List<ElementoDeRed> listar(@RequestParam(required = false) Long idSegmento) {
        if (idSegmento != null) {
            return elementoDeRedRepository.findByIdSegmento(idSegmento);
        }
        return elementoDeRedRepository.findAll();
    }

    @GetMapping("/{id}")
    public ElementoDeRed obtener(@PathVariable Long id) {
        return buscarOFallar(id);
    }

    @PostMapping
    public ResponseEntity<ElementoDeRed> crear(@RequestBody ElementoDeRed elemento) {
        elemento.setIdElemento(null);
        return ResponseEntity.ok(elementoDeRedRepository.save(elemento));
    }

    @PutMapping("/{id}")
    public ElementoDeRed actualizar(@PathVariable Long id, @RequestBody ElementoDeRed cambios) {
        ElementoDeRed existente = buscarOFallar(id);
        existente.setNombre(cambios.getNombre());
        existente.setTecnologia(cambios.getTecnologia());
        existente.setFabricante(cambios.getFabricante());
        existente.setIdSegmento(cambios.getIdSegmento());
        existente.setDistName(cambios.getDistName());
        return elementoDeRedRepository.save(existente);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        buscarOFallar(id);
        elementoDeRedRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private ElementoDeRed buscarOFallar(Long id) {
        return elementoDeRedRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el elemento de red con id " + id));
    }
}
