package com.neipa.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.neipa.domain.Excepcion;

public interface ExcepcionRepository extends JpaRepository<Excepcion, Long> {

    List<Excepcion> findByActivaTrue();

    List<Excepcion> findByParametroAndActivaTrue(String parametro);
}
