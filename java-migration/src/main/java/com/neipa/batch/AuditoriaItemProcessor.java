package com.neipa.batch;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

import com.neipa.domain.Auditoria;
import com.neipa.domain.ElementoDeRed;
import com.neipa.domain.Excepcion;
import com.neipa.domain.ResultadoAuditoria;
import com.neipa.domain.ValorEstandar;
import com.neipa.extraction.EstandarProvider;
import com.neipa.extraction.Fabricante;
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
 */
@Component
public class AuditoriaItemProcessor implements ItemProcessor<ElementoDeRed, List<Auditoria>> {

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
        Fabricante fabricante = Fabricante.valueOf(elemento.getFabricante().toUpperCase());
        VendorExtractor extractor = vendorExtractorFactory.crear(fabricante);

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
}
