package com.neipa.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.neipa.domain.Auditoria;

public interface AuditoriaRepository extends JpaRepository<Auditoria, Long> {

    List<Auditoria> findByResultado(String resultado);

    List<Auditoria> findByIdElemento(Long idElemento);

    long countByResultado(String resultado);

    List<Auditoria> findTop20ByOrderByFechaDesc();

    /** Auditorias simuladas (sin corrida de CM). */
    List<Auditoria> findByIdCorridaIsNull();

    List<Auditoria> findByIdCorridaIsNullAndResultado(String resultado);

    List<Auditoria> findByIdCorridaIsNullAndIdElemento(Long idElemento);

    /** Filas (desvios y exceptuados) de una corrida de CM. */
    List<Auditoria> findByIdCorrida(Long idCorrida);

    List<Auditoria> findByIdCorridaAndResultado(Long idCorrida, String resultado);

    long countByIdCorridaIsNullAndResultado(String resultado);
}
