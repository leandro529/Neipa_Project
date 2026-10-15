package com.neipa.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.neipa.domain.ElementoDeRed;

public interface ElementoDeRedRepository extends JpaRepository<ElementoDeRed, Long> {

    List<ElementoDeRed> findByIdSegmento(Long idSegmento);
}
