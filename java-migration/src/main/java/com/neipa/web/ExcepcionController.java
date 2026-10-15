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

import com.neipa.domain.Excepcion;
import com.neipa.exception.RecursoNoEncontradoException;
import com.neipa.repository.ExcepcionRepository;

/**
 * CRUD de Excepciones (RF05-08, "excepciones de 3 niveles"): permite cargar,
 * desde la web, que un parametro fuera de rango no se marque como DESVIO
 * para un elemento, un segmento, o todo el sistema, durante una ventana de
 * vigencia. Consumida por ExcepcionService al correr una auditoria (CU001).
 */
@RestController
@RequestMapping("/api/excepciones")
public class ExcepcionController {

    private final ExcepcionRepository excepcionRepository;

    public ExcepcionController(ExcepcionRepository excepcionRepository) {
        this.excepcionRepository = excepcionRepository;
    }

    @GetMapping
    public List<Excepcion> listar(@RequestParam(required = false) Boolean activa) {
        if (Boolean.TRUE.equals(activa)) {
            return excepcionRepository.findByActivaTrue();
        }
        return excepcionRepository.findAll();
    }

    @GetMapping("/{id}")
    public Excepcion obtener(@PathVariable Long id) {
        return buscarOFallar(id);
    }

    @PostMapping
    public ResponseEntity<Excepcion> crear(@RequestBody Excepcion excepcion) {
        excepcion.setIdExcepcion(null);
        excepcion.validar();
        return ResponseEntity.ok(excepcionRepository.save(excepcion));
    }

    @PutMapping("/{id}")
    public Excepcion actualizar(@PathVariable Long id, @RequestBody Excepcion cambios) {
        Excepcion existente = buscarOFallar(id);
        existente.setAlcance(cambios.getAlcance());
        existente.setIdElemento(cambios.getIdElemento());
        existente.setIdSegmento(cambios.getIdSegmento());
        existente.setParametro(cambios.getParametro());
        existente.setMotivo(cambios.getMotivo());
        existente.setVigenteDesde(cambios.getVigenteDesde());
        existente.setVigenteHasta(cambios.getVigenteHasta());
        existente.setActiva(cambios.isActiva());
        existente.setPatronNombre(cambios.getPatronNombre());
        existente.validar();
        return excepcionRepository.save(existente);
    }

    /** Atajo para dar de baja una excepcion sin tener que mandar el objeto completo. */
    @PutMapping("/{id}/desactivar")
    public Excepcion desactivar(@PathVariable Long id) {
        Excepcion existente = buscarOFallar(id);
        existente.setActiva(false);
        return excepcionRepository.save(existente);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        buscarOFallar(id);
        excepcionRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private Excepcion buscarOFallar(Long id) {
        return excepcionRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la excepcion con id " + id));
    }
}
