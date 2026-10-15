package com.neipa.extraction;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.neipa.domain.ElementoDeRed;
import com.neipa.domain.ValorEstandar;
import com.neipa.repository.ValorEstandarRepository;

/**
 * Entrega la regla (ValorEstandar) contra la que se audita cada parametro de
 * un elemento.
 *
 * Ampliacion "reglas con condiciones y por segmento": un mismo parametro
 * puede tener varias reglas — una general, otra para un segmento, otra para
 * los elementos cuyo nombre coincide con un patron, otra que depende del
 * valor de otro parametro, etc. Para cada elemento se elige, entre las que
 * aplican, la de mayor prioridad y, a igual prioridad, la mas especifica
 * (mas condiciones; el segmento cuenta como una). Si empatan, la de menor id
 * (la primera cargada), para que el resultado sea siempre el mismo.
 */
@Component
public class EstandarProvider {

    static final Comparator<ValorEstandar> ORDEN_DE_PREFERENCIA =
            Comparator.comparingInt(ValorEstandar::getPrioridad).reversed()
                    .thenComparing(Comparator.comparingInt(ValorEstandar::especificidad).reversed())
                    .thenComparing(v -> v.getIdValorEstandar() == null ? Long.MAX_VALUE : v.getIdValorEstandar());

    private final ValorEstandarRepository valorEstandarRepository;

    public EstandarProvider(ValorEstandarRepository valorEstandarRepository) {
        this.valorEstandarRepository = valorEstandarRepository;
    }

    /**
     * Regla que corresponde al elemento para ese parametro.
     *
     * @param valoresVigentes valores vigentes de TODOS los parametros del
     *                        elemento (para las condiciones sobre otros parametros)
     * @return vacio si el parametro tiene reglas pero ninguna aplica a este
     *         elemento (ej. solo hay una regla para otro segmento): ese
     *         parametro no se audita para el elemento
     * @throws IllegalArgumentException si el parametro no tiene ninguna regla cargada
     */
    public Optional<ValorEstandar> resolver(ElementoDeRed elemento, String nombreParametro,
                                            Map<String, String> valoresVigentes) {
        List<ValorEstandar> reglas = valorEstandarRepository.findByParametro(nombreParametro);
        if (reglas.isEmpty()) {
            throw new IllegalArgumentException(
                    "No hay valor estandar cargado para el parametro: " + nombreParametro
                            + ". Cargalo desde la pantalla de Configuraciones antes de auditar.");
        }
        return elegir(reglas, elemento, valoresVigentes);
    }

    /** Logica de eleccion, separada del repositorio para poder testearla. */
    public static Optional<ValorEstandar> elegir(List<ValorEstandar> reglas, ElementoDeRed elemento,
                                                 Map<String, String> valoresVigentes) {
        return reglas.stream()
                .filter(regla -> regla.aplicaA(elemento, valoresVigentes))
                .min(ORDEN_DE_PREFERENCIA);
    }
}
