package neipa.pruebas;

import neipa.modelo.Auditoria;
import neipa.modelo.Fabricante;
import neipa.modelo.Parametro;
import neipa.modelo.ParametroNumerico;

/**
 * Ejecuta el caso de prueba CPF02 de la segunda entrega (Tabla 13):
 * particion de equivalencia y valores de frontera sobre validarRango() para
 * el parametro Potencia de transmision, cuyo rango permitido es 30 a 46 dBm.
 */
public class PruebaCPF02 {

    public static void main(String[] args) {
        Parametro potencia = new ParametroNumerico(new Fabricante("Ericsson", "ENM"),
                "EUtranCellFDD", "PotenciaTX_dBm", "Potencia de transmision", "dBm",
                30, 46, 1, "43");

        String[] entradas = {"29", "30", "31", "45", "46", "47"};
        boolean[] esperados = {false, true, true, true, true, false};

        System.out.println("CPF02 - validarRango() de " + potencia.getNombre()
                + " (rango permitido: " + potencia.getRangoPermitido() + ")");
        System.out.println(String.format("%-8s %-9s %-9s %-6s %s",
                "Entrada", "Esperado", "Obtenido", "Caso", "Mensaje emitido por el sistema"));

        int correctos = 0;
        for (int i = 0; i < entradas.length; i++) {
            boolean obtenido = potencia.validarRango(entradas[i]);
            boolean coincide = obtenido == esperados[i];
            if (coincide) {
                correctos++;
            }
            System.out.println(String.format("%-8s %-9s %-9s %-6s %s",
                    entradas[i], texto(esperados[i]), texto(obtenido),
                    coincide ? "OK" : "FALLA",
                    obtenido ? Auditoria.MSG_CONFORME : Auditoria.MSG_FUERA_DE_RANGO));
        }
        System.out.println("Resultado: " + correctos + " de " + entradas.length
                + " casos correctos.");
    }

    private static String texto(boolean dentroDeRango) {
        return dentroDeRango ? "conforme" : "desvio";
    }
}
