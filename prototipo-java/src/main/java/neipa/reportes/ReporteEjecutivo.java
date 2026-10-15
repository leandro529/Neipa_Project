package neipa.reportes;

import java.util.Locale;

import neipa.estructuras.ListaEnlazada;
import neipa.modelo.Auditoria;
import neipa.modelo.ResultadoAuditoria;

/** Resumen de totales y porcentajes, pensado para el Gerente / Jefe (RF17). */
public class ReporteEjecutivo implements Reporte {

    @Override
    public String getTitulo() {
        return "REPORTE EJECUTIVO DE AUDITORIA";
    }

    @Override
    public String generar(ListaEnlazada<Auditoria> auditorias) {
        if (auditorias.estaVacia()) {
            return "No hay resultados de auditoria para mostrar.\n";
        }
        // Un contador por cada resultado posible, en el orden del enumerado.
        ResultadoAuditoria[] resultados = ResultadoAuditoria.values();
        int[] cantidades = new int[resultados.length];
        for (Auditoria auditoria : auditorias) {
            cantidades[auditoria.getResultado().ordinal()]++;
        }

        int total = auditorias.tamanio();
        StringBuilder texto = new StringBuilder();
        texto.append("Comparaciones registradas: ").append(total).append('\n');
        for (int i = 0; i < resultados.length; i++) {
            double porcentaje = cantidades[i] * 100.0 / total;
            texto.append(String.format(Locale.US, "  %-11s %3d  %5.1f%%  %s%n",
                    resultados[i], cantidades[i], porcentaje, barra(porcentaje)));
        }
        double conformidad = cantidades[ResultadoAuditoria.CONFORME.ordinal()] * 100.0 / total;
        texto.append(String.format(Locale.US, "Tasa de conformidad: %.1f%%%n", conformidad));
        return texto.toString();
    }

    /** Barra de texto proporcional al porcentaje (un simbolo cada 5 puntos). */
    private String barra(double porcentaje) {
        StringBuilder barra = new StringBuilder();
        int simbolos = (int) Math.round(porcentaje / 5);
        for (int i = 0; i < simbolos; i++) {
            barra.append('#');
        }
        return barra.toString();
    }
}
