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
import org.springframework.web.bind.annotation.RestController;

import com.neipa.domain.Segmento;
import com.neipa.exception.RecursoNoEncontradoException;
import com.neipa.repository.SegmentoRepository;

/**
 * CRUD de Segmentos: la agrupacion de ElementoDeRed (por tecnologia, zona,
 * etc.) sobre la que despues se dispara una auditoria (CU001) o se define
 * una excepcion de alcance SEGMENTO (RF05-08). Pantalla "Configuraciones"
 * del frontend.
 */
@RestController
@RequestMapping("/api/segmentos")
public class SegmentoController {

    private final SegmentoRepository segmentoRepository;

    public SegmentoController(SegmentoRepository segmentoRepository) {
        this.segmentoRepository = segmentoRepository;
    }

    @GetMapping
    public List<Segmento> listar() {
        return segmentoRepository.findAll();
    }

    @GetMapping("/{id}")
    public Segmento obtener(@PathVariable Long id) {
        return buscarOFallar(id);
    }

    @PostMapping
    public ResponseEntity<Segmento> crear(@RequestBody Segmento segmento) {
        segmento.setIdSegmento(null);
        Segmento guardado = segmentoRepository.save(segmento);
        return ResponseEntity.ok(guardado);
    }

    @PutMapping("/{id}")
    public Segmento actualizar(@PathVariable Long id, @RequestBody Segmento cambios) {
        Segmento existente = buscarOFallar(id);
        existente.setNombre(cambios.getNombre());
        existente.setCriterio(cambios.getCriterio());
        existente.setRc(cambios.getRc());
        existente.setFiltroNombre(cambios.getFiltroNombre());
        return segmentoRepository.save(existente);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        buscarOFallar(id);
        segmentoRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private Segmento buscarOFallar(Long id) {
        return segmentoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el segmento con id " + id));
    }
}
