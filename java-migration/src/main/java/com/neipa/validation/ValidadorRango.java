package com.neipa.validation;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Locale;

import com.neipa.domain.OperadorComparacion;
import com.neipa.domain.ResultadoAuditoria;
import com.neipa.domain.ValorEstandar;
import com.neipa.extraction.RangoPermitido;

/**
 * Implementa RF04: valida que el valor vigente de un parametro cumpla la
 * regla definida para ese parametro. Los dos metodos originales
 * (validar(double, RangoPermitido) y validar(String, RangoPermitido)) son la
 * version en codigo de Parametro.validarRangoPermitido(), ya diseñada en el
 * diagrama de clases de la segunda entrega (Figura 9), y del caso de prueba
 * CPF02 (particion de equivalencia y analisis de frontera, Tablas 10 a 13) —
 * no se tocan.
 *
 * El tercer metodo (validar(String, ValorEstandar)) es una ampliacion (ver
 * registro del proyecto): generaliza la validacion a cualquier
 * OperadorComparacion, a partir de que las auditorias reales que el usuario
 * ya tenia cargadas fuera de este repo (AUDIT-xx.xlsx) casi nunca expresan
 * una regla como un rango, sino como una condicion puntual (ej.
 * "tacLimitGbrEmergency &lt; 500000" o "a3Offset = -6"). Para el caso
 * RANGO delega en el metodo original, asi que CPF02 sigue exactamente igual.
 */
public final class ValidadorRango {

    private ValidadorRango() {
    }

    public static ResultadoAuditoria validar(double valorVigente, RangoPermitido rango) {
        boolean dentroDeRango = valorVigente >= rango.getMinimo() && valorVigente <= rango.getMaximo();
        return dentroDeRango ? ResultadoAuditoria.CONFORME : ResultadoAuditoria.DESVIO;
    }

    public static ResultadoAuditoria validar(String valorVigente, RangoPermitido rango) {
        return validar(Double.parseDouble(valorVigente), rango);
    }

    public static ResultadoAuditoria validar(String valorVigente, ValorEstandar estandar) {
        OperadorComparacion operador = estandar.getOperador() == null
                ? OperadorComparacion.RANGO
                : estandar.getOperador();

        if (operador == OperadorComparacion.RANGO) {
            RangoPermitido rango = new RangoPermitido(
                    estandar.getMinimo().doubleValue(), estandar.getMaximo().doubleValue());
            return validar(valorVigente, rango);
        }

        boolean cumple = cumpleOperador(valorVigente, operador, estandar.getValorReferencia());
        return cumple ? ResultadoAuditoria.CONFORME : ResultadoAuditoria.DESVIO;
    }

    /**
     * Evalua un dato contra un operador y una referencia (cualquier operador
     * salvo RANGO). Lo usan las condiciones de aplicacion de las reglas.
     */
    public static boolean cumple(String dato, OperadorComparacion operador, String referencia) {
        if (operador == OperadorComparacion.RANGO) {
            throw new IllegalArgumentException("RANGO no se puede usar en una condicion");
        }
        return cumpleOperador(dato, operador, referencia);
    }

    private static boolean cumpleOperador(String valorVigente, OperadorComparacion operador, String referencia) {
        switch (operador) {
            case EN_LISTA:
                return Arrays.stream(referencia.split(","))
                        .map(String::trim)
                        .anyMatch(v -> v.equalsIgnoreCase(valorVigente.trim()));
            case COINCIDE:
                // Comodines estilo SQL LIKE (% y _) y tambien estilo
                // archivo (* y ?), para que "*_test_*" funcione igual que en
                // las excepciones por patron.
                return PatronNombre.coincideLike(valorVigente, referencia);
            case IGUAL:
            case DISTINTO:
            case MENOR_QUE:
            case MENOR_IGUAL:
            case MAYOR_QUE:
            case MAYOR_IGUAL:
                return cumpleComparacion(valorVigente, operador, referencia);
            default:
                throw new IllegalStateException("Operador no soportado: " + operador);
        }
    }

    private static boolean cumpleComparacion(String valorVigente, OperadorComparacion operador, String referencia) {
        BigDecimal valorNumerico = aNumeroONull(valorVigente);
        BigDecimal referenciaNumerica = aNumeroONull(referencia);

        if (valorNumerico != null && referenciaNumerica != null) {
            int comparacion = valorNumerico.compareTo(referenciaNumerica);
            switch (operador) {
                case IGUAL:
                    return comparacion == 0;
                case DISTINTO:
                    return comparacion != 0;
                case MENOR_QUE:
                    return comparacion < 0;
                case MENOR_IGUAL:
                    return comparacion <= 0;
                case MAYOR_QUE:
                    return comparacion > 0;
                case MAYOR_IGUAL:
                    return comparacion >= 0;
                default:
                    throw new IllegalStateException("Operador no soportado: " + operador);
            }
        }

        // Valor no numerico (ej. ExpectedCellSize = "fixed"): solo tienen
        // sentido IGUAL/DISTINTO, comparando como texto.
        boolean sonIguales = valorVigente.trim().equalsIgnoreCase(referencia.trim());
        if (operador == OperadorComparacion.IGUAL) {
            return sonIguales;
        }
        if (operador == OperadorComparacion.DISTINTO) {
            return !sonIguales;
        }
        throw new IllegalArgumentException(
                "El operador " + operador + " requiere un valor numerico (valor vigente: " + valorVigente + ")");
    }

    private static BigDecimal aNumeroONull(String texto) {
        if (texto == null) {
            return null;
        }
        try {
            return new BigDecimal(texto.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
