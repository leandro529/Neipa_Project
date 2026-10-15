package com.neipa.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.neipa.domain.CambioMasivo;

public interface CambioMasivoRepository extends JpaRepository<CambioMasivo, Long> {
}
