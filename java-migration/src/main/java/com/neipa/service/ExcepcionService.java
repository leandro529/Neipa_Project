package com.neipa.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.neipa.domain.AlcanceExcepcion;
import com.neipa.domain.ElementoDeRed;
import com.neipa.domain.Excepcion;
import com.neipa.repository.ExcepcionRepository;
import com.neipa.validation.PatronNombre;

/**
 * Busca si hay una excepcion vigente que cubra un desvio (RF05-08).
 *
 * Orden de precedencia, de la mas puntual a la mas general:
 * ELEMENTO, PATRON (nombre del elemento coincide con un patron, ej
 * "*_test_*"), SEGMENTO y GLOBAL. Una excepcion puede cubrir un parametro
 * puntual o todos (parametro "*").
 */
@Service
public class ExcepcionService {

    private final ExcepcionRepository excepcionRepository;

    public ExcepcionService(ExcepcionRepository excepcionRepository) {
        this.excepcionRepository = excepcionRepository;
    }

    public Optional<Excepcion> buscarExcepcionAplicable(ElementoDeRed elemento, String parametro,
                                                          LocalDateTime fecha) {
        List<Excepcion> candidatas = excepcionRepository.findByActivaTrue().stream()
                .filter(e -> e.cubreParametro(parametro))
                .filter(e -> e.esVigenteEn(fecha))
                .collect(Collectors.toList());
        return elegir(candidatas, elemento);
    }

    /** Logica de eleccion, separada del repositorio para poder testearla. */
    public static Optional<Excepcion> elegir(List<Excepcion> candidatas, ElementoDeRed elemento) {
        AlcanceExcepcion[] orden = {
                AlcanceExcepcion.ELEMENTO, AlcanceExcepcion.PATRON,
                AlcanceExcepcion.SEGMENTO, AlcanceExcepcion.GLOBAL
        };
        for (AlcanceExcepcion alcance : orden) {
            Optional<Excepcion> encontrada = candidatas.stream()
                    .filter(e -> e.getAlcance() == alcance)
                    .filter(e -> coincide(e, elemento))
                    .findFirst();
            if (encontrada.isPresent()) {
                return encontrada;
            }
        }
        return Optional.empty();
    }

    private static boolean coincide(Excepcion excepcion, ElementoDeRed elemento) {
        switch (excepcion.getAlcance()) {
            case ELEMENTO:
                return Objects.equals(excepcion.getIdElemento(), elemento.getIdElemento());
            case PATRON:
                return PatronNombre.coincide(elemento.getNombre(), excepcion.getPatronNombre());
            case SEGMENTO:
                return elemento.getIdSegmento() != null
                        && Objects.equals(excepcion.getIdSegmento(), elemento.getIdSegmento());
            case GLOBAL:
                return true;
            default:
                return false;
        }
    }
}
