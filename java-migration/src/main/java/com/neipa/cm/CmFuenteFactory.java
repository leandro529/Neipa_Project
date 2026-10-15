package com.neipa.cm;

import java.nio.file.Path;

/** Abre la fuente de CM de un archivo (se reemplaza en los tests). */
@FunctionalInterface
public interface CmFuenteFactory {

    CmFuente abrir(Path archivo);
}
