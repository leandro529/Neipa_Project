package com.neipa.batch;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

import com.neipa.domain.Auditoria;
import com.neipa.domain.ElementoDeRed;
import com.neipa.domain.Excepcion;
import com.neipa.domain.ResultadoAuditoria;
import com.neipa.domain.ValorEstandar;
import com.neipa.extraction.EstandarProvider;
import com.neipa.extraction.ParametroVigente;
import com.neipa.extraction.VendorExtractor;
import com.neipa.extraction.VendorExtractorFactory;
import com.neipa.service.ExcepcionService;
import com.neipa.validation.ValidadorRango;

/**
 * Paso "Transform" del job de auditoria (CU001): para cada elemento extrae
 * los valores vigentes con el extractor de su fabricante, elige la regla que
 * le corresponde a cada parametro (puede depender del segmento, del propio
 * elemento o del valor de otro parametro — ver EstandarProvider), compara y,
 * si hay desvio, busca una excepcion vigente (ExcepcionService). En los
 * desvios guarda ademas el valor objetivo, que despues se usa para generar
 * el XML RAML 2.0 de reconfiguracion.
 *
 * Si el elemento es de un fabricante sin extractor (hoy Huawei y Ericsson) o
 * con un fabricante desconocido, no se audita: process() devuelve null, que
 * Spring Batch interpreta como "item filtrado", y el resto de los elementos
 * del segmento se sigue auditando. Antes la excepcion cortaba el job entero
 * y no quedaba guardado ningun resultado del segmento.
 */
@Component
public class AuditoriaItemProcessor implements ItemProcessor<ElementoDeRed, List<Auditoria>> {

    private static final Logger LOG = LoggerFactory.getLogger(AuditoriaItemProcessor.class);

    private final VendorExtractorFactory vendorExtractorFactory;
    private final EstandarProvider estandarProvider;
    private final ExcepcionService excepcionService;

    public AuditoriaItemProcessor(VendorExtractorFactory vendorExtractorFactory,
                                   EstandarProvider estandarProvider,
                                   ExcepcionService excepcionService) {
        this.vendorExtractorFactory = vendorExtractorFactory;
        this.estandarProvider = estandarProvider;
        this.excepcionService = excepcionService;
    }

    @Override
    public List<Auditoria> process(ElementoDeRed elemento) {
        VendorExtractor extractor = extractorPara(elemento);
        if (extractor == null) {
            return null; // elemento no auditable: Spring Batch lo filtra y sigue con el resto
        }

        List<ParametroVigente> parametrosVigentes = extractor.extraerParametrosVigentes(elemento);

        // Todos los valores del elemento, para las condiciones que dependen
        // de otro parametro (ej. "si PotenciaTX_dBm >= 45 ...").
        Map<String, String> valoresVigentes = new LinkedHashMap<>();
        for (ParametroVigente p : parametrosVigentes) {
            valoresVigentes.put(p.getNombreParametro(), p.getValorVigente());
        }

        List<Auditoria> auditorias = new ArrayList<>();
        for (ParametroVigente parametroVigente : parametrosVigentes) {
            String nombre = parametroVigente.getNombreParametro();
            Optional<ValorEstandar> regla = estandarProvider.resolver(elemento, nombre, valoresVigentes);
            if (!regla.isPresent()) {
                // Hay reglas para el parametro, pero ninguna para este
                // elemento (ej. solo se cargo para otro segmento).
                continue;
            }
            ValorEstandar estandar = regla.get();
            ResultadoAuditoria resultado = ValidadorRango.validar(parametroVigente.getValorVigente(), estandar);
            LocalDateTime ahora = LocalDateTime.now();

            Long idExcepcionAplicada = null;
            String valorObjetivo = null;
            if (resultado == ResultadoAuditoria.DESVIO) {
                valorObjetivo = estandar.valorObjetivoPara(parametroVigente.getValorVigente()).orElse(null);
                Optional<Excepcion> excepcion = excepcionService.buscarExcepcionAplicable(elemento, nombre, ahora);
                if (excepcion.isPresent()) {
                    resultado = ResultadoAuditoria.EXCEPTUADO;
                    idExcepcionAplicada = excepcion.get().getIdExcepcion();
                }
            }

            Auditoria auditoria = new Auditoria(
                    elemento.getIdElemento(),
                    nombre,
                    parametroVigente.getValorVigente(),
                    estandar.comoTexto(),
                    resultado,
                    ahora,
                    idExcepcionAplicada
            );
            auditoria.setIdValorEstandar(estandar.getIdValorEstandar());
            auditoria.setReglaAplicada(estandar.condicionesComoTexto());
            auditoria.setValorObjetivo(valorObjetivo);
            auditorias.add(auditoria);
        }
        return auditorias;
    }

    /**
     * Extractor del fabricante del elemento, o null si no se puede auditar
     * (sin fabricante, fabricante desconocido o sin extractor implementado).
     */
    private VendorExtractor extractorPara(ElementoDeRed elemento) {
        String fabricante = elemento.getFabricante();
        if (fabricante == null || fabricante.trim().isEmpty()) {
            LOG.warn("El elemento {} no tiene fabricante: no se audita", elemento.getNombre());
            return null;
        }
        try {
            return vendorExtractorFactory.crear(fabricante.trim());
        } catch (UnsupportedOperationException | IllegalArgumentException ex) {
            LOG.warn("El elemento {} ({}) no se audita: {}", elemento.getNombre(), fabricante, ex.getMessage());
            return null;
        }
    }
}
