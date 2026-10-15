package com.neipa.service;

import com.neipa.domain.Corrida;
import com.neipa.domain.Segmento;
import com.neipa.repository.CorridaRepository;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.neipa.domain.Auditoria;
import com.neipa.domain.CambioMasivo;
import com.neipa.domain.ElementoDeRed;
import com.neipa.domain.ResultadoAuditoria;
import com.neipa.repository.AuditoriaRepository;
import com.neipa.repository.CambioMasivoRepository;
import com.neipa.repository.ElementoDeRedRepository;
import com.neipa.repository.ExcepcionRepository;
import com.neipa.repository.SegmentoRepository;
import com.neipa.web.dto.ResumenDashboardDTO;

/**
 * Arma el resumen que consume el dashboard de control (pantalla principal
 * del frontend): cuantas auditorias salieron conformes/con desvio/
 * exceptuadas, cuantas excepciones estan activas, como se reparten los
 * elementos por fabricante y el estado de los cambios masivos — pensado para
 * la revision diaria de auditoria (CU003) y los reportes ejecutivos que
 * consumen jefes/gerentes segun la primera entrega.
 */
@Service
public class DashboardService {

    private final SegmentoRepository segmentoRepository;
    private final ElementoDeRedRepository elementoDeRedRepository;
    private final AuditoriaRepository auditoriaRepository;
    private final ExcepcionRepository excepcionRepository;
    private final CambioMasivoRepository cambioMasivoRepository;
    private final CorridaRepository corridaRepository;

    public DashboardService(SegmentoRepository segmentoRepository,
                             ElementoDeRedRepository elementoDeRedRepository,
                             AuditoriaRepository auditoriaRepository,
                             ExcepcionRepository excepcionRepository,
                             CambioMasivoRepository cambioMasivoRepository,
                             CorridaRepository corridaRepository) {
        this.segmentoRepository = segmentoRepository;
        this.elementoDeRedRepository = elementoDeRedRepository;
        this.auditoriaRepository = auditoriaRepository;
        this.excepcionRepository = excepcionRepository;
        this.cambioMasivoRepository = cambioMasivoRepository;
        this.corridaRepository = corridaRepository;
    }

    public ResumenDashboardDTO obtenerResumen() {
        ResumenDashboardDTO resumen = new ResumenDashboardDTO();

        resumen.setTotalSegmentos(segmentoRepository.count());
        resumen.setTotalElementos(elementoDeRedRepository.count());

        long conformes = auditoriaRepository.countByIdCorridaIsNullAndResultado(ResultadoAuditoria.CONFORME.toString());
        long desvios = auditoriaRepository.countByIdCorridaIsNullAndResultado(ResultadoAuditoria.DESVIO.toString());
        long exceptuadas = auditoriaRepository.countByIdCorridaIsNullAndResultado(ResultadoAuditoria.EXCEPTUADO.toString());
        // Segmentos de CM: se suma el estado actual (ultima corrida de cada uno).
        for (Segmento s : segmentoRepository.findAll()) {
            if (!s.esDeCm()) {
                continue;
            }
            java.util.Optional<Corrida> ultima = corridaRepository.findTopByIdSegmentoOrderByIdCorridaDesc(s.getIdSegmento());
            if (ultima.isPresent()) {
                conformes += ultima.get().getConformes();
                desvios += ultima.get().getDesvios();
                exceptuadas += ultima.get().getExceptuados();
            }
        }
        long total = conformes + desvios + exceptuadas;

        resumen.setTotalAuditorias(total);
        resumen.setAuditoriasConformes(conformes);
        resumen.setAuditoriasDesvio(desvios);
        resumen.setAuditoriasExceptuadas(exceptuadas);
        resumen.setTasaConformidadPct(total == 0 ? 0.0 : (conformes * 100.0) / total);

        resumen.setExcepcionesActivas(excepcionRepository.findByActivaTrue().size());

        List<ElementoDeRed> elementos = elementoDeRedRepository.findAll();
        Map<String, Long> porFabricante = elementos.stream()
                .collect(Collectors.groupingBy(
                        e -> e.getFabricante() == null ? "SIN_DEFINIR" : e.getFabricante(),
                        Collectors.counting()));
        resumen.setElementosPorFabricante(porFabricante);

        List<CambioMasivo> cambios = cambioMasivoRepository.findAll();
        Map<String, Long> porEstado = cambios.stream()
                .collect(Collectors.groupingBy(
                        c -> c.getEstado() == null ? "SIN_DEFINIR" : c.getEstado(),
                        Collectors.counting()));
        resumen.setCambiosMasivosPorEstado(porEstado);

        List<Auditoria> recientes = auditoriaRepository.findTop20ByOrderByFechaDesc();
        resumen.setAuditoriasRecientes(recientes);

        return resumen;
    }
}
