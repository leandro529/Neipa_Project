package com.neipa.reconfiguracion;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.neipa.domain.Auditoria;
import com.neipa.domain.Corrida;
import com.neipa.domain.Segmento;
import com.neipa.domain.ElementoDeRed;
import com.neipa.domain.ResultadoAuditoria;
import com.neipa.domain.ValorEstandar;
import com.neipa.repository.AuditoriaRepository;
import com.neipa.repository.CorridaRepository;
import com.neipa.repository.SegmentoRepository;
import com.neipa.repository.ElementoDeRedRepository;
import com.neipa.repository.ValorEstandarRepository;

/**
 * Arma la lista de desvios vigentes: para cada (elemento, parametro) toma la
 * ULTIMA auditoria y se queda con las que dieron DESVIO. Asi, si un desvio
 * ya se corrigio y la re-auditoria dio CONFORME (o se lo exceptuo), no
 * vuelve a salir en el XML.
 */
@Service
public class DesvioService {

    private final AuditoriaRepository auditoriaRepository;
    private final ElementoDeRedRepository elementoDeRedRepository;
    private final ValorEstandarRepository valorEstandarRepository;
    private final SegmentoRepository segmentoRepository;
    private final CorridaRepository corridaRepository;

    public DesvioService(AuditoriaRepository auditoriaRepository,
                         ElementoDeRedRepository elementoDeRedRepository,
                         ValorEstandarRepository valorEstandarRepository,
                         SegmentoRepository segmentoRepository,
                         CorridaRepository corridaRepository) {
        this.auditoriaRepository = auditoriaRepository;
        this.elementoDeRedRepository = elementoDeRedRepository;
        this.valorEstandarRepository = valorEstandarRepository;
        this.segmentoRepository = segmentoRepository;
        this.corridaRepository = corridaRepository;
    }

    /** @param idSegmento null = todos los segmentos */
    public List<Desvio> desviosVigentes(Long idSegmento) {
        Map<Long, ElementoDeRed> elementos = elementoDeRedRepository.findAll().stream()
                .collect(Collectors.toMap(ElementoDeRed::getIdElemento, Function.identity()));
        Map<Long, ValorEstandar> reglas = valorEstandarRepository.findAll().stream()
                .collect(Collectors.toMap(ValorEstandar::getIdValorEstandar, Function.identity()));
        List<Desvio> desvios = new ArrayList<>(
                armar(auditoriaRepository.findByIdCorridaIsNull(), elementos, reglas, idSegmento));

        // Segmentos de CM: los desvios vigentes son los de su ULTIMA corrida
        // (si un desvio se corrigio, en la corrida siguiente ya no aparece).
        for (Segmento segmento : segmentoRepository.findAll()) {
            if (!segmento.esDeCm() || (idSegmento != null && !idSegmento.equals(segmento.getIdSegmento()))) {
                continue;
            }
            Optional<Corrida> ultima = corridaRepository.findTopByIdSegmentoOrderByIdCorridaDesc(segmento.getIdSegmento());
            if (!ultima.isPresent()) {
                continue;
            }
            for (Auditoria a : auditoriaRepository.findByIdCorridaAndResultado(ultima.get().getIdCorrida(),
                    ResultadoAuditoria.DESVIO.toString())) {
                desvios.add(deCm(a, segmento.getIdSegmento()));
            }
        }
        desvios.sort(Comparator.comparing(Desvio::getElemento, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(Desvio::getDistName).thenComparing(Desvio::getParametro));
        return desvios;
    }

    /** Desvio de una auditoria de CM: el distName ya es el del objeto real. */
    static Desvio deCm(Auditoria a, Long idSegmento) {
        return new Desvio(a.getIdAuditoria(), a.getIdElemento(),
                a.getNombreObjeto() != null ? a.getNombreObjeto() : a.getDistName(), idSegmento, a.getDistName(),
                claseDe(a.getDistName()), a.getParametro(), a.getValorVigente(), a.getValorEstandar(),
                a.getReglaAplicada(), a.getValorObjetivo());
    }

    /** Logica pura (sin repositorios), para poder testearla. */
    public static List<Desvio> armar(List<Auditoria> auditorias, Map<Long, ElementoDeRed> elementos,
                                     Map<Long, ValorEstandar> reglas, Long idSegmento) {
        Map<String, Auditoria> ultimaPorClave = new HashMap<>();
        for (Auditoria a : auditorias) {
            String clave = a.getIdElemento() + "|" + a.getParametro();
            Auditoria actual = ultimaPorClave.get(clave);
            if (actual == null || a.getIdAuditoria() > actual.getIdAuditoria()) {
                ultimaPorClave.put(clave, a);
            }
        }

        List<Desvio> desvios = new ArrayList<>();
        for (Auditoria a : ultimaPorClave.values()) {
            if (!ResultadoAuditoria.DESVIO.name().equalsIgnoreCase(a.getResultado())) {
                continue;
            }
            ElementoDeRed elemento = elementos.get(a.getIdElemento());
            if (elemento == null) {
                continue;
            }
            if (idSegmento != null && !Objects.equals(idSegmento, elemento.getIdSegmento())) {
                continue;
            }
            ValorEstandar regla = a.getIdValorEstandar() == null ? null : reglas.get(a.getIdValorEstandar());
            String distName = distNameDelObjeto(elemento, regla);
            desvios.add(new Desvio(a.getIdAuditoria(), elemento.getIdElemento(), elemento.getNombre(),
                    elemento.getIdSegmento(), distName, claseDe(distName), a.getParametro(),
                    a.getValorVigente(), a.getValorEstandar(), a.getReglaAplicada(), a.getValorObjetivo()));
        }
        desvios.sort(Comparator.comparing(Desvio::getElemento).thenComparing(Desvio::getParametro));
        return desvios;
    }

    /** distName del elemento + objeto gestionado de la regla (si tiene). */
    static String distNameDelObjeto(ElementoDeRed elemento, ValorEstandar regla) {
        String base = elemento.distNameEfectivo();
        String objeto = regla == null ? null : regla.getObjetoGestionado();
        if (objeto == null || objeto.trim().isEmpty()) {
            return base;
        }
        return base + "/" + objeto.trim().replaceAll("^/+", "");
    }

    /** Clase del MO = ultimo tramo del distName sin el id ("LNCEL-1" -> "LNCEL"). */
    static String claseDe(String distName) {
        String ultimo = distName.substring(distName.lastIndexOf('/') + 1);
        int guion = ultimo.lastIndexOf('-');
        return guion > 0 ? ultimo.substring(0, guion) : ultimo;
    }
}
