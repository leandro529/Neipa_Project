package neipa.pruebas;

import java.time.LocalDateTime;

import neipa.algoritmos.PatronNombre;
import neipa.control.ControladorAuditoria;
import neipa.datos.DatosDePrueba;
import neipa.datos.RepositorioEnMemoria;
import neipa.estructuras.ListaEnlazada;
import neipa.excepciones.NeipaException;
import neipa.modelo.Auditoria;
import neipa.modelo.Segmento;
import neipa.reportes.GeneradorRaml;

/**
 * Prueba de la ampliacion: patrones de nombre, estandar con condicion,
 * excepcion por patron y XML de reconfiguracion (plan + backup) sobre el
 * segmento "Laboratorio 4G" de los datos de prueba.
 */
public class PruebaReconfiguracion {

    private static int correctos = 0;
    private static int total = 0;

    public static void main(String[] args) throws NeipaException {
        System.out.println("Patrones de nombre (PatronNombre.coincide)");
        verificar("Lab_test_004_A coincide con *_test_*", PatronNombre.coincide("Lab_test_004_A", "*_test_*"));
        verificar("Lab_SITE_001_A no coincide con *_test_*", !PatronNombre.coincide("Lab_SITE_001_A", "*_test_*"));
        verificar("AtestB no coincide con *_test_* ('_' es literal)", !PatronNombre.coincide("AtestB", "*_test_*"));
        verificar("RC2 coincide con RC? y RC12 no", PatronNombre.coincide("RC2", "RC?")
                && !PatronNombre.coincide("RC12", "RC?"));

        RepositorioEnMemoria repo = new RepositorioEnMemoria();
        DatosDePrueba.cargar(repo);
        Segmento laboratorio = null;
        for (Segmento s : repo.getSegmentos()) {
            if (s.getNombre().equals("Laboratorio 4G")) {
                laboratorio = s;
            }
        }
        ControladorAuditoria controlador = new ControladorAuditoria(repo);
        ListaEnlazada<Auditoria> resultados = controlador.ejecutarAuditoria(laboratorio);

        System.out.println();
        System.out.println("Auditoria del segmento Laboratorio 4G");
        verificar("Lab_SITE_001_A a3Offset -6: CONFORME (estandar general -6)",
                resultado(resultados, "Lab_SITE_001_A", "a3Offset").equals("CONFORME"));
        verificar("Lab_SITE_002_A a3Offset -6 con pMax 43: DESVIO (aplica el estandar -3)",
                resultado(resultados, "Lab_SITE_002_A", "a3Offset").equals("DESVIO"));
        verificar("Lab_SITE_003_A pMax 46: DESVIO (rango 30..43)",
                resultado(resultados, "Lab_SITE_003_A", "pMax").equals("DESVIO"));
        verificar("Lab_test_004_A: EXCEPTUADO por el patron *_test_*",
                resultado(resultados, "Lab_test_004_A", "a3Offset").equals("EXCEPTUADO"));

        LocalDateTime fecha = LocalDateTime.of(2026, 10, 13, 10, 0);
        ListaEnlazada<Auditoria> desvios = controlador.desviosDeUltimaAuditoria();
        GeneradorRaml plan = new GeneradorRaml(GeneradorRaml.Tipo.PLAN, laboratorio.getNombre(), fecha);
        GeneradorRaml backup = new GeneradorRaml(GeneradorRaml.Tipo.BACKUP, laboratorio.getNombre(), fecha);
        String xmlPlan = plan.generar(desvios);
        String xmlBackup = backup.generar(desvios);

        System.out.println();
        System.out.println("XML de reconfiguracion RAML 2.0");
        verificar("Nombre del plan: " + plan.nombreDeArchivo(),
                plan.nombreDeArchivo().equals("LABORATORIO_4G_online_10-13-2026.xml"));
        verificar("Nombre del backup: " + backup.nombreDeArchivo(),
                backup.nombreDeArchivo().equals("LABORATORIO_4G_online_10-13-2026_backup.xml"));
        verificar("Plan: a3Offset pasa a -3 y pMax a 43",
                xmlPlan.contains("<p name=\"a3Offset\">-3</p>") && xmlPlan.contains("<p name=\"pMax\">43</p>"));
        verificar("Backup: mismos parametros con el valor vigente (-6 y 46)",
                xmlBackup.contains("<p name=\"a3Offset\">-6</p>") && xmlBackup.contains("<p name=\"pMax\">46</p>"));
        verificar("El sitio de prueba no entra en el plan", !xmlPlan.contains("Lab_test"));
        verificar("a3Offset va en su objeto CADPR y pMax en la celda (LNCEL)",
                xmlPlan.contains("class=\"CADPR\"") && xmlPlan.contains("/CADPR-1\"")
                        && xmlPlan.contains("class=\"LNCEL\""));

        System.out.println();
        System.out.println("Resultado: " + correctos + " de " + total + " casos correctos.");
    }

    private static String resultado(ListaEnlazada<Auditoria> resultados, String elemento, String parametro) {
        for (Auditoria a : resultados) {
            if (a.getElementoDeRed().getNombre().equals(elemento)
                    && a.getParametro().getNombre().equals(parametro)) {
                return a.getResultado().name();
            }
        }
        return "(sin resultado)";
    }

    private static void verificar(String descripcion, boolean ok) {
        total++;
        if (ok) {
            correctos++;
        }
        System.out.println((ok ? "  OK     " : "  FALLA  ") + descripcion);
    }
}
