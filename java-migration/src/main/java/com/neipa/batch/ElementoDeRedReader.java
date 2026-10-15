package com.neipa.batch;

import java.util.Iterator;
import java.util.List;

import org.springframework.batch.item.ItemReader;

import com.neipa.domain.ElementoDeRed;
import com.neipa.repository.ElementoDeRedRepository;

/**
 * Lee, una vez por ejecucion del job, todos los ElementoDeRed de un Segmento
 * (RF09) para auditarlos (CU001). Equivalente, simplificado, del iterador de
 * chunks que devolvia ExtractAndTransform.get_dataset_chunk_iterator() en el
 * codigo Python original.
 */
public class ElementoDeRedReader implements ItemReader<ElementoDeRed> {

    private final Iterator<ElementoDeRed> iterator;

    public ElementoDeRedReader(ElementoDeRedRepository elementoDeRedRepository, Long idSegmento) {
        List<ElementoDeRed> elementos = elementoDeRedRepository.findByIdSegmento(idSegmento);
        this.iterator = elementos.iterator();
    }

    @Override
    public ElementoDeRed read() {
        return iterator.hasNext() ? iterator.next() : null;
    }
}
