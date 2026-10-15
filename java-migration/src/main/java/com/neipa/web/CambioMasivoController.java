package com.neipa.web;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.neipa.domain.CambioMasivo;
import com.neipa.exception.RecursoNoEncontradoException;
import com.neipa.repository.CambioMasivoRepository;

/**
 * Lectura de CambioMasivo para el dashboard de control. Solo GET: la logica
 * de negocio de CU004 (autorizar/monitorear cambios masivos) sigue, a
 * proposito, fuera del alcance acordado para esta iteracion (ver
 * java-migration-registro.md) — la tabla y la entidad ya estaban modeladas,
 * esto solo agrega la forma de consultarlas desde la web.
 */
@RestController
@RequestMapping("/api/cambios-masivos")
public class CambioMasivoController {

    private final CambioMasivoRepository cambioMasivoRepository;

    public CambioMasivoController(CambioMasivoRepository cambioMasivoRepository) {
        this.cambioMasivoRepository = cambioMasivoRepository;
    }

    @GetMapping
    public List<CambioMasivo> listar() {
        return cambioMasivoRepository.findAll();
    }

    @GetMapping("/{id}")
    public CambioMasivo obtener(@PathVariable Long id) {
        return cambioMasivoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el cambio masivo con id " + id));
    }
}
