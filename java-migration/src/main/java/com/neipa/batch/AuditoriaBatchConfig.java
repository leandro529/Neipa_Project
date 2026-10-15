package com.neipa.batch;

import java.util.List;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.neipa.domain.Auditoria;
import com.neipa.domain.ElementoDeRed;
import com.neipa.repository.ElementoDeRedRepository;

/**
 * Configuracion del job de auditoria (CU001). Equivalente Java, usando
 * Spring Batch en lugar de Airflow + include/ETL.py del repo original, del flujo
 * "run_etl_slice": por cada elemento de red del segmento (chunk-oriented,
 * tamaño de chunk = 10) se extraen sus parametros vigentes, se comparan
 * contra el estandar y se persiste el resultado.
 */
@Configuration
@EnableBatchProcessing
public class AuditoriaBatchConfig {

    private static final int CHUNK_SIZE = 10;

    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;
    private final ElementoDeRedRepository elementoDeRedRepository;
    private final AuditoriaItemProcessor auditoriaItemProcessor;
    private final AuditoriaItemWriter auditoriaItemWriter;

    public AuditoriaBatchConfig(JobBuilderFactory jobBuilderFactory,
                                 StepBuilderFactory stepBuilderFactory,
                                 ElementoDeRedRepository elementoDeRedRepository,
                                 AuditoriaItemProcessor auditoriaItemProcessor,
                                 AuditoriaItemWriter auditoriaItemWriter) {
        this.jobBuilderFactory = jobBuilderFactory;
        this.stepBuilderFactory = stepBuilderFactory;
        this.elementoDeRedRepository = elementoDeRedRepository;
        this.auditoriaItemProcessor = auditoriaItemProcessor;
        this.auditoriaItemWriter = auditoriaItemWriter;
    }

    @Bean
    @StepScope
    public ElementoDeRedReader elementoDeRedReader(
            @Value("#{jobParameters['idSegmento']}") Long idSegmento) {
        return new ElementoDeRedReader(elementoDeRedRepository, idSegmento);
    }

    @Bean
    public Step auditarSegmentoStep(ElementoDeRedReader elementoDeRedReader) {
        return stepBuilderFactory.get("auditarSegmentoStep")
                .<ElementoDeRed, List<Auditoria>>chunk(CHUNK_SIZE)
                .reader(elementoDeRedReader)
                .processor(auditoriaItemProcessor)
                .writer(auditoriaItemWriter)
                .build();
    }

    @Bean
    public Job auditoriaJob(Step auditarSegmentoStep) {
        return jobBuilderFactory.get("auditoriaJob")
                .start(auditarSegmentoStep)
                .build();
    }
}
