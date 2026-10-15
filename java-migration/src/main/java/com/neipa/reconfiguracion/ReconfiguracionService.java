package com.neipa.reconfiguracion;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.springframework.stereotype.Service;

import com.neipa.domain.Segmento;
import com.neipa.reconfiguracion.GeneradorRaml.Tipo;
import com.neipa.repository.SegmentoRepository;

/**
 * Arma los archivos de reconfiguracion de los desvios vigentes: un par
 * plan + backup (RAML 2.0) por segmento, como los planes por region/RC que
 * ya usa el area, mas un pendientes.txt con los desvios que no tienen valor
 * objetivo y hay que resolver a mano.
 */
@Service
public class ReconfiguracionService {

    private final DesvioService desvioService;
    private final GeneradorRaml generadorRaml;
    private final SegmentoRepository segmentoRepository;

    public ReconfiguracionService(DesvioService desvioService, GeneradorRaml generadorRaml,
                                  SegmentoRepository segmentoRepository) {
        this.desvioService = desvioService;
        this.generadorRaml = generadorRaml;
        this.segmentoRepository = segmentoRepository;
    }

    /** Un solo XML (plan o backup) de los desvios de un segmento, o de todos. */
    public ArchivoGenerado xml(Long idSegmento, Tipo tipo, LocalDateTime fecha) {
        String grupo = nombreGrupo(idSegmento);
        String contenido = generadorRaml.generar(desvioService.desviosVigentes(idSegmento), grupo, tipo, fecha);
        return new ArchivoGenerado(GeneradorRaml.nombreArchivo(grupo, tipo, fecha),
                contenido.getBytes(StandardCharsets.UTF_8));
    }

    /** ZIP con plan + backup por cada segmento con desvios, y pendientes.txt. */
    public ArchivoGenerado zip(Long idSegmento, LocalDateTime fecha) {
        List<Desvio> desvios = desvioService.desviosVigentes(idSegmento);
        Map<Long, List<Desvio>> porSegmento = desvios.stream()
                .collect(Collectors.groupingBy(d -> d.getIdSegmento() == null ? 0L : d.getIdSegmento(),
                        TreeMap::new, Collectors.toList()));

        Map<String, String> archivos = new LinkedHashMap<>();
        StringBuilder pendientes = new StringBuilder();
        for (Map.Entry<Long, List<Desvio>> grupoSegmento : porSegmento.entrySet()) {
            Long id = grupoSegmento.getKey() == 0L ? null : grupoSegmento.getKey();
            String grupo = nombreGrupo(id);
            List<Desvio> lista = grupoSegmento.getValue();
            if (lista.stream().anyMatch(Desvio::isReconfigurable)) {
                for (Tipo tipo : Tipo.values()) {
                    archivos.put(GeneradorRaml.nombreArchivo(grupo, tipo, fecha),
                            generadorRaml.generar(lista, grupo, tipo, fecha));
                }
            }
            for (Desvio d : lista) {
                if (!d.isReconfigurable()) {
                    pendientes.append(grupo).append('\t').append(d.getElemento()).append('\t')
                            .append(d.getDistName()).append('\t').append(d.getParametro()).append('\t')
                            .append("vigente=").append(d.getValorVigente()).append('\t')
                            .append("regla=").append(d.getValorEstandar()).append("\r\n");
                }
            }
        }
        if (pendientes.length() > 0) {
            archivos.put("pendientes.txt",
                    "Desvios sin valor objetivo (cargar 'valor objetivo' en la configuracion o corregir a mano)\r\n"
                            + "segmento\telemento\tdistName\tparametro\tvigente\tregla\r\n" + pendientes);
        }
        if (archivos.isEmpty()) {
            archivos.put("sin-desvios.txt", "No hay desvios vigentes para reconfigurar.\r\n");
        }

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(bytes, StandardCharsets.UTF_8)) {
            for (Map.Entry<String, String> archivo : archivos.entrySet()) {
                zip.putNextEntry(new ZipEntry(archivo.getKey()));
                zip.write(archivo.getValue().getBytes(StandardCharsets.UTF_8));
                zip.closeEntry();
            }
        } catch (IOException ex) {
            throw new IllegalStateException("No se pudo armar el ZIP de reconfiguracion", ex);
        }
        String nombre = "neipa-reconfiguracion-" + (idSegmento == null ? "todos" : nombreGrupo(idSegmento))
                + "-" + fecha.format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")) + ".zip";
        return new ArchivoGenerado(nombre, bytes.toByteArray());
    }

    private String nombreGrupo(Long idSegmento) {
        if (idSegmento == null) {
            return "TODOS";
        }
        return segmentoRepository.findById(idSegmento)
                .map(Segmento::getNombre)
                .map(GeneradorRaml::normalizarGrupo)
                .orElse("SEGMENTO_" + idSegmento);
    }

    /** Nombre de archivo + contenido, para devolver como descarga. */
    public static class ArchivoGenerado {
        private final String nombre;
        private final byte[] contenido;

        public ArchivoGenerado(String nombre, byte[] contenido) {
            this.nombre = nombre;
            this.contenido = contenido;
        }

        public String getNombre() {
            return nombre;
        }

        public byte[] getContenido() {
            return contenido;
        }
    }
}
