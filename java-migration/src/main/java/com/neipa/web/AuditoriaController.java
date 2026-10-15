package com.neipa.web;

import java.nio.charset.StandardCharsets;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import com.neipa.reconfiguracion.Desvio;
import com.neipa.reconfiguracion.DesvioService;
import com.neipa.reconfiguracion.GeneradorRaml;
import com.neipa.reconfiguracion.ReconfiguracionService;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.neipa.cm.CmAuditoriaService;
import com.neipa.domain.Auditoria;
import com.neipa.domain.Corrida;
import com.neipa.domain.Segmento;
import com.neipa.repository.SegmentoRepository;
import com.neipa.repository.AuditoriaRepository;

/**
 * Interfaz REST, equivalente a InterfazAuditoria + ControladorAuditoria
 * (Figura 9 - Diagrama de clases de diseno): dispara la auditoria (CU001) de
 * todos los elementos de red de un segmento, y expone su resultado para el
 * dashboard de control y la pantalla de auditorias del frontend.
 */
@RestController
@RequestMapping("/api/auditorias")
public class AuditoriaController {

    private final JobLauncher jobLauncher;
    private final Job auditoriaJob;
    private final AuditoriaRepository auditoriaRepository;
    private final DesvioService desvioService;
    private final ReconfiguracionService reconfiguracionService;
    private final SegmentoRepository segmentoRepository;
    private final CmAuditoriaService cmAuditoriaService;

    public AuditoriaController(JobLauncher jobLauncher, Job auditoriaJob,
                                AuditoriaRepository auditoriaRepository,
                                DesvioService desvioService, ReconfiguracionService reconfiguracionService,
                                SegmentoRepository segmentoRepository, CmAuditoriaService cmAuditoriaService) {
        this.jobLauncher = jobLauncher;
        this.auditoriaJob = auditoriaJob;
        this.auditoriaRepository = auditoriaRepository;
        this.desvioService = desvioService;
        this.reconfiguracionService = reconfiguracionService;
        this.segmentoRepository = segmentoRepository;
        this.cmAuditoriaService = cmAuditoriaService;
    }

    @PostMapping("/segmentos/{idSegmento}")
    public ResponseEntity<?> ejecutarAuditoria(@PathVariable Long idSegmento) throws Exception {
        Segmento segmento = segmentoRepository.findById(idSegmento).orElse(null);
        if (segmento != null && segmento.esDeCm()) {
            // Segmento de CM: auditoria por objeto sobre los datos importados.
            Corrida corrida = cmAuditoriaService.auditar(segmento);
            return ResponseEntity.ok(corrida);
        }
        JobParametersBuilder parametros = new JobParametersBuilder()
                .addLong("idSegmento", idSegmento)
                .addString("fecha", LocalDateTime.now().toString());

        jobLauncher.run(auditoriaJob, parametros.toJobParameters());

        return ResponseEntity.accepted().body("Auditoria iniciada para el segmento " + idSegmento);
    }

    @GetMapping
    public List<Auditoria> listar(@RequestParam(required = false) String resultado,
                                   @RequestParam(required = false) Long idElemento,
                                   @RequestParam(required = false) Long idCorrida) {
        // Las filas de las corridas de CM pueden ser muchas: se piden por corrida.
        if (idCorrida != null) {
            return resultado != null ? auditoriaRepository.findByIdCorridaAndResultado(idCorrida, resultado)
                    : auditoriaRepository.findByIdCorrida(idCorrida);
        }
        if (resultado != null) {
            return auditoriaRepository.findByIdCorridaIsNullAndResultado(resultado);
        }
        if (idElemento != null) {
            return auditoriaRepository.findByIdCorridaIsNullAndIdElemento(idElemento);
        }
        return auditoriaRepository.findByIdCorridaIsNull();
    }

    /**
     * Desvios vigentes (ultima auditoria de cada elemento y parametro en
     * DESVIO), con el valor objetivo y el objeto gestionado donde corregirlo.
     */
    @GetMapping("/desvios")
    public List<Desvio> desvios(@RequestParam(required = false) Long idSegmento) {
        return desvioService.desviosVigentes(idSegmento);
    }

    /**
     * Plan de reconfiguracion RAML 2.0 (Nokia NetAct) de los desvios
     * vigentes. tipo=plan (valor objetivo, default) o tipo=backup (valor
     * vigente antes del cambio, para hacer rollback).
     */
    @GetMapping("/reconfiguracion")
    public ResponseEntity<byte[]> reconfiguracion(@RequestParam(required = false) Long idSegmento,
                                                  @RequestParam(defaultValue = "plan") String tipo) {
        GeneradorRaml.Tipo t;
        try {
            t = GeneradorRaml.Tipo.valueOf(tipo.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("tipo invalido: " + tipo + " (usar plan o backup)");
        }
        return descarga(reconfiguracionService.xml(idSegmento, t, LocalDateTime.now()),
                new MediaType("application", "xml", StandardCharsets.UTF_8));
    }

    /** ZIP con plan + backup por segmento, y pendientes.txt si hay desvios sin valor objetivo. */
    @GetMapping("/reconfiguracion/zip")
    public ResponseEntity<byte[]> reconfiguracionZip(@RequestParam(required = false) Long idSegmento) {
        return descarga(reconfiguracionService.zip(idSegmento, LocalDateTime.now()),
                MediaType.parseMediaType("application/zip"));
    }

    private static ResponseEntity<byte[]> descarga(ReconfiguracionService.ArchivoGenerado archivo, MediaType tipo) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + archivo.getNombre() + "\"")
                .contentType(tipo)
                .body(archivo.getContenido());
    }

    @GetMapping("/recientes")
    public List<Auditoria> recientes() {
        return auditoriaRepository.findTop20ByOrderByFechaDesc();
    }
}
