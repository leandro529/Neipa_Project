package com.neipa.web;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.neipa.domain.Excepcion;
import com.neipa.domain.ValorEstandar;
import com.neipa.reconfiguracion.DesvioService;
import com.neipa.reconfiguracion.GeneradorRaml;
import com.neipa.reconfiguracion.ReconfiguracionService;
import com.neipa.repository.AuditoriaRepository;
import com.neipa.repository.ExcepcionRepository;
import com.neipa.repository.ValorEstandarRepository;

/**
 * Capa web de la ampliacion, sin base de datos: JSON de reglas con
 * condiciones, validaciones (400) y descarga de los XML de reconfiguracion.
 */
class ApiAmpliacionTest {

    private MockMvc mvc;
    private ReconfiguracionService reconfiguracionService;

    @BeforeEach
    void preparar() throws Exception {
        ValorEstandarRepository valorRepo = mock(ValorEstandarRepository.class);
        when(valorRepo.save(any(ValorEstandar.class))).thenAnswer(inv -> {
            ValorEstandar v = inv.getArgument(0);
            v.setIdValorEstandar(200L);
            return v;
        });
        ExcepcionRepository excepcionRepo = mock(ExcepcionRepository.class);
        when(excepcionRepo.save(any(Excepcion.class))).thenAnswer(inv -> inv.getArgument(0));
        Excepcion aplicada = new Excepcion();
        aplicada.setIdExcepcion(7L);
        when(excepcionRepo.findById(7L)).thenReturn(java.util.Optional.of(aplicada));
        org.mockito.Mockito.doThrow(new org.springframework.dao.DataIntegrityViolationException("fk"))
                .when(excepcionRepo).deleteById(7L);
        reconfiguracionService = mock(ReconfiguracionService.class);
        com.neipa.catalogo.MemoriaCatalogoRepositorio repoCatalogo = new com.neipa.catalogo.MemoriaCatalogoRepositorio();
        java.util.List<com.neipa.catalogo.ParametroCatalogo> filas = new java.util.ArrayList<>();
        com.neipa.catalogo.CatalogoCsv.leer(new java.io.StringReader(com.neipa.catalogo.CatalogoTestAcceso.CSV), filas::add);
        repoCatalogo.reemplazar(filas);
        com.neipa.catalogo.CatalogoService catalogo = new com.neipa.catalogo.CatalogoService(repoCatalogo, ".");

        MappingJackson2HttpMessageConverter json =
                new MappingJackson2HttpMessageConverter(Jackson2ObjectMapperBuilder.json().build());
        mvc = MockMvcBuilders.standaloneSetup(
                        new ValorEstandarController(valorRepo, catalogo),
                        new ExcepcionController(excepcionRepo),
                        new AuditoriaController(mock(JobLauncher.class), mock(Job.class),
                                mock(AuditoriaRepository.class), mock(DesvioService.class), reconfiguracionService,
                                mock(com.neipa.repository.SegmentoRepository.class), mock(com.neipa.cm.CmAuditoriaService.class)))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(new ByteArrayHttpMessageConverter(),
                        new StringHttpMessageConverter(StandardCharsets.UTF_8), json)
                .build();
    }

    @Test
    void altaDeReglaConSegmentoYCondicionDevuelveLasCondiciones() throws Exception {
        String cuerpo = "{\"parametro\":\"srvccDelayTimer\",\"operador\":\"IGUAL\",\"valorReferencia\":\"10\","
                + "\"idSegmento\":3,\"condiciones\":[{\"origen\":\"PARAMETRO\",\"campo\":\"PotenciaTX_dBm\","
                + "\"operador\":\"IGUAL\",\"valor\":\"20\"},{\"origen\":\"ATRIBUTO_ELEMENTO\",\"campo\":\"nombre\","
                + "\"operador\":\"COINCIDE\",\"valor\":\"*_URB_*\"}]}";
        mvc.perform(post("/api/configuraciones").contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idSegmento").value(3))
                .andExpect(jsonPath("$.condiciones.length()").value(2))
                .andExpect(jsonPath("$.condiciones[0].campo").value("PotenciaTX_dBm"))
                .andExpect(jsonPath("$.condiciones[1].campo").value("NOMBRE"));
    }

    @Test
    void reglasYExcepcionesMalFormadasDevuelven400() throws Exception {
        mvc.perform(post("/api/configuraciones").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"parametro\":\"x\",\"operador\":\"IGUAL\",\"valorReferencia\":\"1\",\"condiciones\":"
                                + "[{\"origen\":\"PARAMETRO\",\"campo\":\"y\",\"operador\":\"RANGO\",\"valor\":\"1\"}]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje", containsString("RANGO")));
        mvc.perform(post("/api/configuraciones").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"parametro\":\"x\",\"operador\":\"RANGO\",\"minimo\":10,\"maximo\":5}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/excepciones").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"alcance\":\"PATRON\",\"parametro\":\"*\",\"activa\":true}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/excepciones").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"alcance\":\"PATRON\",\"patronNombre\":\"*_test_*\",\"parametro\":\"*\",\"activa\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.patronNombre").value("*_test_*"));
        // Validacion contra el catalogo de parametros de Nokia.
        mvc.perform(post("/api/configuraciones").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"parametro\":\"n310Qci1\",\"objetoGestionado\":\"LNCEL\",\"operador\":\"IGUAL\",\"valorReferencia\":\"7\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje", containsString("LNCEL.n310Qci1")));
        mvc.perform(post("/api/configuraciones").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"parametro\":\"n310Qci1\",\"objetoGestionado\":\"LNCEL\",\"operador\":\"IGUAL\",\"valorReferencia\":\"6\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void borrarUnaExcepcionYaAplicadaDevuelve409() throws Exception {
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/api/excepciones/7"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje", containsString("desactivelo")));
    }

    @Test
    void descargaDelPlanYDelBackup() throws Exception {
        when(reconfiguracionService.xml(eq(3L), eq(GeneradorRaml.Tipo.BACKUP), any()))
                .thenReturn(new ReconfiguracionService.ArchivoGenerado("LABORATORIO_4G_online_10-13-2026_backup.xml",
                        "<raml/>".getBytes(StandardCharsets.UTF_8)));
        mvc.perform(get("/api/auditorias/reconfiguracion").param("idSegmento", "3").param("tipo", "backup"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition",
                        "attachment; filename=\"LABORATORIO_4G_online_10-13-2026_backup.xml\""))
                .andExpect(content().contentTypeCompatibleWith("application/xml"))
                .andExpect(content().string("<raml/>"));

        when(reconfiguracionService.zip(isNull(), any()))
                .thenReturn(new ReconfiguracionService.ArchivoGenerado("x.zip", new byte[] {1, 2}));
        mvc.perform(get("/api/auditorias/reconfiguracion/zip"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/zip"));

        mvc.perform(get("/api/auditorias/reconfiguracion").param("tipo", "otro"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje", containsString("plan o backup")));
    }
}
