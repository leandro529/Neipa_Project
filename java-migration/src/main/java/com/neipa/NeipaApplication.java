package com.neipa;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Punto de entrada de la migracion a Java + MySQL del nucleo ETL de NEIPA.
 * Equivalente Java de main.py (que en el repo original era solo un script
 * de prueba de PyCharm, sin logica real).
 *
 * El codigo Python original (main.py, include/, vendor_utilities/) no esta
 * en este repositorio: queda en https://github.com/leandro529/neipa. Las
 * rutas .py que se citan en los comentarios son de ese repo.
 */
@SpringBootApplication
public class NeipaApplication {

    public static void main(String[] args) {
        SpringApplication.run(NeipaApplication.class, args);
    }
}
