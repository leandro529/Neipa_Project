package neipa.reportes;

import neipa.algoritmos.ComparadorPorResultado;
import neipa.algoritmos.Ordenador;
import neipa.estructuras.ListaEnlazada;
import neipa.modelo.Auditoria;
import neipa.modelo.ResultadoAuditoria;

/** Detalle por elemento y parametro, pensado para el Ingeniero de RF (RF16). */
public class ReporteTecnico implements Reporte {

    @Override
    public String getTitulo() {
        return "REPORTE TECNICO DE AUDITORIA";
    }

    @Override
    public String generar(ListaEnlazada<Auditoria> auditorias) {
        if (auditorias.estaVacia()) {
            return "No hay resultados de auditoria para mostrar.\n";
        }
        Auditoria[] ordenadas = new Auditoria[auditorias.tamanio()];
        auditorias.copiarEn(ordenadas);
        Ordenador.ordenarPorInsercion(ordenadas, new ComparadorPorResultado());

        StringBuilder texto = new StringBuilder();
        texto.append(String.format("%-16s %-20s %-9s %-9s %-10s%n",
                "Elemento", "Parametro", "Vigente", "Estandar", "Resultado"));
        texto.append("--------------------------------------------------------------------\n");
        for (int i = 0; i < ordenadas.length; i++) {
            Auditoria a = ordenadas[i];
            texto.append(String.format("%-16s %-20s %-9s %-9s %-10s%n",
                    a.getElementoDeRed().getNombre(), a.getParametro().getNombre(),
                    a.getValorVigente(), a.getValorEstandar().obtenerEstandar(),
                    a.getResultado()));
            if (a.getResultado() != ResultadoAuditoria.CONFORME) {
                texto.append("                 > ").append(a.getObservacion()).append('\n');
            }
        }
        return texto.toString();
    }
}
