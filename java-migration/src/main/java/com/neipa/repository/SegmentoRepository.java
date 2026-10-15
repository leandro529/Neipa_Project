package com.neipa.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.neipa.domain.Segmento;

public interface SegmentoRepository extends JpaRepository<Segmento, Long> {
}
