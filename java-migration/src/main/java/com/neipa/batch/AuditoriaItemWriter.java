package com.neipa.batch;

import java.util.List;

import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

import com.neipa.domain.Auditoria;
import com.neipa.repository.AuditoriaRepository;

/**
 * Persiste las Auditorias generadas por el processor (RF10). Equivalente
 * Java, usando Spring Data JPA en lugar de pandas.DataFrame.to_sql(), de
 * SqlDbLoad.load_dataframe_chunk() (include/loaders/SqlLoad.py del repo original).
 */
@Component
public class AuditoriaItemWriter implements ItemWriter<List<Auditoria>> {

    private final AuditoriaRepository auditoriaRepository;

    public AuditoriaItemWriter(AuditoriaRepository auditoriaRepository) {
        this.auditoriaRepository = auditoriaRepository;
    }

    @Override
    public void write(List<? extends List<Auditoria>> items) {
        for (List<Auditoria> auditoriasDeUnElemento : items) {
            auditoriaRepository.saveAll(auditoriasDeUnElemento);
        }
    }
}
