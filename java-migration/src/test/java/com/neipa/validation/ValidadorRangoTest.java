package com.neipa.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.neipa.domain.OperadorComparacion;
import com.neipa.domain.ResultadoAuditoria;
import com.neipa.domain.ValorEstandar;
import com.neipa.extraction.RangoPermitido;

/**
 * Reproduce, contra el codigo Java, el caso de prueba CPF02 de la segunda
 * entrega (particion de equivalencia y analisis de frontera sobre
 * Parametro.validarRangoPermitido(), parametro "Potencia de transmision
 * (dBm)", rango permitido 30 a 46 - Tablas 10 a 13), y agrega casos para el
 * resto de los operadores (ampliacion — ver registro del proyecto),
 * usando parametros y valores reales tomados de las auditorias que el
 * usuario ya tenia cargadas fuera de este repo.
 */
class ValidadorRangoTest {

    private final RangoPermitido rangoPotenciaTx = new RangoPermitido(30, 46);

    @ParameterizedTest(name = "valor {0} -> {1}")
    @CsvSource({
            // Tabla 13: Comportamiento del sistema.
            "29, DESVIO",   // CEI1 (p < 30)
            "30, CONFORME", // CEV1 (inf.)
            "31, CONFORME", // CEV1 (inf.) + 1
            "45, CONFORME", // CEV1 (sup.) - 1
            "46, CONFORME", // CEV1 (sup.)
            "47, DESVIO"    // CEI2 (p > 46)
    })
    void validaLosValoresDeFronteraDeCPF02(double valorVigente, ResultadoAuditoria esperado) {
        ResultadoAuditoria resultado = ValidadorRango.validar(valorVigente, rangoPotenciaTx);
        assertEquals(esperado, resultado);
    }

    @Test
    void unValorEstandarConOperadorRangoSigueUsandoLosLimitesDeCPF02() {
        ValorEstandar potenciaTx = new ValorEstandar(
                "PotenciaTX_dBm", new BigDecimal("30"), new BigDecimal("46"), "dBm");

        assertEquals(ResultadoAuditoria.CONFORME, ValidadorRango.validar("30", potenciaTx));
        assertEquals(ResultadoAuditoria.CONFORME, ValidadorRango.validar("46", potenciaTx));
        assertEquals(ResultadoAuditoria.DESVIO, ValidadorRango.validar("29", potenciaTx));
        assertEquals(ResultadoAuditoria.DESVIO, ValidadorRango.validar("47", potenciaTx));
    }

    @Test
    void validaOperadorIgualConValorNumerico() {
        // AUDIT-05_4G_SRVCC-DelayTimer.xlsx: srvccDelayTimer = 4.
        ValorEstandar srvccDelayTimer = new ValorEstandar(
                "srvccDelayTimer", OperadorComparacion.IGUAL, "4", null);

        assertEquals(ResultadoAuditoria.CONFORME, ValidadorRango.validar("4", srvccDelayTimer));
        assertEquals(ResultadoAuditoria.DESVIO, ValidadorRango.validar("5", srvccDelayTimer));
    }

    @Test
    void validaOperadorIgualConValorDeTexto() {
        // AUDIT-13_4G_ExpectedCellSize.xlsx: ExpectedCellSize = "fixed".
        ValorEstandar expectedCellSize = new ValorEstandar(
                "ExpectedCellSize", OperadorComparacion.IGUAL, "fixed", null);

        assertEquals(ResultadoAuditoria.CONFORME, ValidadorRango.validar("fixed", expectedCellSize));
        assertEquals(ResultadoAuditoria.DESVIO, ValidadorRango.validar("dynamic", expectedCellSize));
    }

    @Test
    void validaOperadorMenorQue() {
        // AUDIT-22_4G_AJUSTES-UMBRALES-MINIMOS-LTAC.xlsx: tacLimitGbrEmergency < 500000.
        ValorEstandar tacLimit = new ValorEstandar(
                "tacLimitGbrEmergency", OperadorComparacion.MENOR_QUE, "500000", null);

        assertEquals(ResultadoAuditoria.CONFORME, ValidadorRango.validar("480000", tacLimit));
        assertEquals(ResultadoAuditoria.DESVIO, ValidadorRango.validar("500000", tacLimit));
    }

    @Test
    void validaOperadorEnLista() {
        ValorEstandar modoDuplex = new ValorEstandar(
                "modoDuplex", OperadorComparacion.EN_LISTA, "FDD, TDD", null);

        assertEquals(ResultadoAuditoria.CONFORME, ValidadorRango.validar("TDD", modoDuplex));
        assertEquals(ResultadoAuditoria.DESVIO, ValidadorRango.validar("SDL", modoDuplex));
    }

    @Test
    void validaOperadorCoincide() {
        ValorEstandar nombreCelda = new ValorEstandar(
                "cellName", OperadorComparacion.COINCIDE, "CO%L100%", null);

        assertEquals(ResultadoAuditoria.CONFORME, ValidadorRango.validar("CO172L100MA1", nombreCelda));
        assertEquals(ResultadoAuditoria.DESVIO, ValidadorRango.validar("XX172L100MA1", nombreCelda));
    }
}
