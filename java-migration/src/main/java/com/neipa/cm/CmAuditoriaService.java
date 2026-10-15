package com.neipa.cm;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.neipa.domain.Auditoria;
import com.neipa.domain.CondicionEstandar;
import com.neipa.domain.Corrida;
import com.neipa.domain.ElementoDeRed;
import com.neipa.domain.Excepcion;
import com.neipa.domain.OrigenCondicion;
import com.neipa.domain.ResultadoAuditoria;
import com.neipa.domain.Segmento;
import com.neipa.domain.ValorEstandar;
import com.neipa.extraction.EstandarProvider;
import com.neipa.repository.CorridaRepository;
import com.neipa.repository.ElementoDeRedRepository;
import com.neipa.repository.ExcepcionRepository;
import com.neipa.repository.ValorEstandarRepository;
import com.neipa.service.ExcepcionService;
import com.neipa.validation.PatronNombre;
import com.neipa.validation.ValidadorRango;

/**
 * Auditoria de un segmento sobre los datos de CM importados (CU001 con datos
 * reales). La unidad auditada es el OBJETO GESTIONADO (distName): cada celda,
 * cada SIB, cada CADPR, etc., contra la regla que le corresponde.
 *
 * Para que escale con la red completa y con corridas repetidas, se guardan
 * solo los desvios y los exceptuados (una fila por objeto y parametro) y un
 * resumen por regla. A medida que la red se estandariza, cada corrida guarda
 * menos filas; los conformes quedan como totales de la corrida.
 */
@Service
public class CmAuditoriaService {

    private final CmRepositorio cmRepositorio;
    private final ValorEstandarRepository valorEstandarRepository;
    private final ExcepcionRepository excepcionRepository;
    private final ElementoDeRedRepository elementoDeRedRepository;
    private final CorridaRepository corridaRepository;

    public CmAuditoriaService(CmRepositorio cmRepositorio, ValorEstandarRepository valorEstandarRepository,
                              ExcepcionRepository excepcionRepository, ElementoDeRedRepository elementoDeRedRepository,
                              CorridaRepository corridaRepository) {
        this.cmRepositorio = cmRepositorio;
        this.valorEstandarRepository = valorEstandarRepository;
        this.excepcionRepository = excepcionRepository;
        this.elementoDeRedRepository = elementoDeRedRepository;
        this.corridaRepository = corridaRepository;
    }

    public Corrida auditar(Segmento segmento) {
        if (!segmento.esDeCm()) {
            throw new IllegalArgumentException("El segmento " + segmento.getNombre() + " no tiene un RC de CM asignado");
        }
        long inicio = System.currentTimeMillis();
        LocalDateTime ahora = LocalDateTime.now();
        String rc = segmento.getRc().trim().toLowerCase(Locale.ROOT);
        String filtro = segmento.getFiltroNombre() == null || segmento.getFiltroNombre().trim().isEmpty()
                ? null : segmento.getFiltroNombre().trim();

        // clase -> parametro -> reglas
        Map<String, Map<String, List<ValorEstandar>>> reglas = new LinkedHashMap<>();
        for (ValorEstandar r : valorEstandarRepository.findAll()) {
            reglas.computeIfAbsent(CmNombres.claseDeObjetoGestionado(r.getObjetoGestionado()), k -> new LinkedHashMap<>())
                    .computeIfAbsent(r.getParametro(), k -> new ArrayList<>()).add(r);
        }
        List<Excepcion> excepciones = excepcionRepository.findByActivaTrue().stream()
                .filter(e -> e.esVigenteEn(ahora)).collect(Collectors.toList());
        Map<String, Long> elementoPorLnbts = new HashMap<>();
        for (ElementoDeRed e : elementoDeRedRepository.findAll()) {
            if (e.getDistName() != null) {
                elementoPorLnbts.put(e.getDistName(), e.getIdElemento());
            }
        }

        Corrida corrida = new Corrida();
        corrida.setIdSegmento(segmento.getIdSegmento());
        corrida.setFecha(ahora);
        corrida = corridaRepository.save(corrida);

        Map<Long, ResumenRegla> resumen = new LinkedHashMap<>();
        List<Auditoria> filas = new ArrayList<>();
        Set<String> objetosEvaluados = new LinkedHashSet<>();
        long comparaciones = 0;
        long sinRegla = 0;

        for (Map.Entry<String, Map<String, List<ValorEstandar>>> porClase : reglas.entrySet()) {
            String clase = porClase.getKey();
            Map<String, List<ValorEstandar>> reglasDeLaClase = porClase.getValue();

            Set<String> propios = new LinkedHashSet<>(reglasDeLaClase.keySet());
            Map<String, Set<String>> deAncestros = new LinkedHashMap<>();
            for (List<ValorEstandar> lista : reglasDeLaClase.values()) {
                for (ValorEstandar r : lista) {
                    for (CondicionEstandar c : r.getCondiciones()) {
                        if (c.getOrigen() != OrigenCondicion.PARAMETRO) {
                            continue;
                        }
                        int punto = c.getCampo().indexOf('.');
                        if (punto > 0) {
                            deAncestros.computeIfAbsent(c.getCampo().substring(0, punto).toUpperCase(Locale.ROOT),
                                    k -> new LinkedHashSet<>()).add(c.getCampo().substring(punto + 1));
                        } else {
                            propios.add(c.getCampo());
                        }
                    }
                }
            }
            List<CmObjeto> objetos = cmRepositorio.objetos(clase, rc, propios);
            if (objetos.isEmpty()) {
                continue;
            }
            Map<String, Map<String, String>> valoresAncestros = new HashMap<>();
            for (Map.Entry<String, Set<String>> a : deAncestros.entrySet()) {
                for (CmObjeto anc : cmRepositorio.objetos(a.getKey(), rc, a.getValue())) {
                    Map<String, String> v = valoresAncestros.computeIfAbsent(anc.getDistName(), k -> new HashMap<>());
                    for (Map.Entry<String, String> x : anc.getValores().entrySet()) {
                        v.put(a.getKey() + "." + x.getKey(), x.getValue());
                    }
                }
            }

            for (CmObjeto objeto : objetos) {
                if (filtro != null && !PatronNombre.coincide(objeto.getNombre(), filtro)) {
                    continue;
                }
                Map<String, String> vigentes = new HashMap<>(objeto.getValores());
                for (String claseAncestro : deAncestros.keySet()) {
                    String dnAncestro = CmNombres.ancestro(objeto.getDistName(), claseAncestro);
                    if (dnAncestro != null && valoresAncestros.containsKey(dnAncestro)) {
                        vigentes.putAll(valoresAncestros.get(dnAncestro));
                    }
                }
                ElementoDeRed elemento = elementoDe(objeto, segmento, elementoPorLnbts);

                for (Map.Entry<String, List<ValorEstandar>> porParametro : reglasDeLaClase.entrySet()) {
                    String parametro = porParametro.getKey();
                    String valor = objeto.getValores().get(parametro);
                    if (valor == null) {
                        continue; // el objeto no tiene ese parametro en el dump
                    }
                    objetosEvaluados.add(objeto.getDistName());
                    Optional<ValorEstandar> regla = EstandarProvider.elegir(porParametro.getValue(), elemento, vigentes);
                    if (!regla.isPresent()) {
                        sinRegla++;
                        continue;
                    }
                    ValorEstandar estandar = regla.get();
                    ResultadoAuditoria resultado;
                    try {
                        resultado = ValidadorRango.validar(valor, estandar);
                    } catch (IllegalArgumentException ex) {
                        // Ej: MENOR_QUE contra un valor no numerico: se informa como desvio.
                        resultado = ResultadoAuditoria.DESVIO;
                    }
                    comparaciones++;
                    ResumenRegla r = resumen.computeIfAbsent(estandar.getIdValorEstandar(),
                            k -> new ResumenRegla(k, clase, parametro));
                    if (resultado == ResultadoAuditoria.CONFORME) {
                        r.sumarConforme();
                        continue;
                    }
                    Long idExcepcion = null;
                    List<Excepcion> candidatas = excepciones.stream().filter(e -> e.cubreParametro(parametro))
                            .collect(Collectors.toList());
                    Optional<Excepcion> excepcion = ExcepcionService.elegir(candidatas, elemento);
                    if (excepcion.isPresent()) {
                        resultado = ResultadoAuditoria.EXCEPTUADO;
                        idExcepcion = excepcion.get().getIdExcepcion();
                        r.sumarExceptuado();
                    } else {
                        r.sumarDesvio();
                    }
                    Auditoria fila = new Auditoria(elemento.getIdElemento(), parametro, valor, estandar.comoTexto(),
                            resultado, ahora, idExcepcion);
                    fila.setIdValorEstandar(estandar.getIdValorEstandar());
                    fila.setReglaAplicada(estandar.condicionesComoTexto());
                    fila.setValorObjetivo(estandar.valorObjetivoPara(valor).orElse(null));
                    fila.setIdCorrida(corrida.getIdCorrida());
                    fila.setDistName(objeto.getDistName());
                    fila.setNombreObjeto(objeto.getNombre());
                    filas.add(fila);
                }
            }
        }

        cmRepositorio.guardarAuditorias(filas);
        cmRepositorio.guardarResumen(corrida.getIdCorrida(), resumen.values());
        corrida.setObjetosEvaluados(objetosEvaluados.size());
        corrida.setComparaciones(comparaciones);
        corrida.setConformes(resumen.values().stream().mapToLong(ResumenRegla::getConformes).sum());
        corrida.setDesvios(resumen.values().stream().mapToLong(ResumenRegla::getDesvios).sum());
        corrida.setExceptuados(resumen.values().stream().mapToLong(ResumenRegla::getExceptuados).sum());
        corrida.setSinRegla(sinRegla);
        corrida.setDuracionMs(System.currentTimeMillis() - inicio);
        return corridaRepository.save(corrida);
    }

    /**
     * Elemento "de paso" para evaluar condiciones y excepciones con la logica
     * ya existente: nombre del objeto, segmento auditado y el id del elemento
     * de red de su LNBTS (para las excepciones por elemento).
     */
    private static ElementoDeRed elementoDe(CmObjeto objeto, Segmento segmento, Map<String, Long> elementoPorLnbts) {
        ElementoDeRed e = new ElementoDeRed(objeto.getNombre() != null ? objeto.getNombre() : objeto.getDistName(),
                "4G", "NSN", segmento.getIdSegmento());
        String lnbts = CmNombres.ancestro(objeto.getDistName(), CmNombres.LNBTS);
        e.setIdElemento(lnbts == null ? null : elementoPorLnbts.get(lnbts));
        e.setDistName(lnbts);
        return e;
    }
}
