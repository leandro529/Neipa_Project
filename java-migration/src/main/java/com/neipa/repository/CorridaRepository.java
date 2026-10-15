package com.neipa.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.neipa.domain.Corrida;

public interface CorridaRepository extends JpaRepository<Corrida, Long> {

    List<Corrida> findByIdSegmentoOrderByIdCorridaAsc(Long idSegmento);

    List<Corrida> findAllByOrderByIdCorridaAsc();

    Optional<Corrida> findTopByIdSegmentoOrderByIdCorridaDesc(Long idSegmento);
}
