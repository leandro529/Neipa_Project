package com.neipa.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.neipa.domain.ValorEstandar;

public interface ValorEstandarRepository extends JpaRepository<ValorEstandar, Long> {

    List<ValorEstandar> findByParametro(String parametro);
}
