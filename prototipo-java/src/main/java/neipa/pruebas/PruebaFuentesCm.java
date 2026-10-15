package neipa.pruebas;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDateTime;

import neipa.control.ControladorAuditoria;
import neipa.control.ImportadorCm;
import neipa.datos.DatosDePrueba;
import neipa.datos.RepositorioEnMemoria;
import neipa.estructuras.ListaEnlazada;
import neipa.excepciones.ExtraccionException;
import neipa.excepciones.NeipaException;
import neipa.excepciones.ValorInvalidoException;
import neipa.fuentes.FabricaFuentesCm;
import neipa.fuentes.FuenteCm;
import neipa.fuentes.FuenteCmBaseDeDatos;
import neipa.fuentes.FuenteCmCsv;
import neipa.fuentes.FuenteCmRaml;
import neipa.fuentes.ObjetoCm;
import neipa.modelo.Auditoria;
import neipa.modelo.Fabricante;
import neipa.modelo.Segmento;
import neipa.reportes.GeneradorRaml;

/**
 * Prueba de las fuentes de datos de CM: la fabrica elige la fuente segun el
 * origen, las fuentes RAML y CSV leen los mismos objetos, se importan al
 * segmento "Laboratorio 4G", se auditan, y el plan RAML generado se vuelve a
 * leer como fuente para corregir los desvios.
 * Ejecutar desde la carpeta prototipo-java (usa datos-cm/).
 */
public class PruebaFuentesCm {

    private static int correctos = 0;
    private static int total = 0;

    public static void main(String[] args) throws NeipaException, IOException {
        String raml = "datos-cm/netact_demo.xml";
        String csv = "datos-cm/csv";

        System.out.println("Fabrica de fuentes (patron Factory)");
        verificar(".xml -> FuenteCmRaml", FabricaFuentesCm.crear(raml) instanceof FuenteCmRaml);
        verificar("carpeta -> FuenteCmCsv", FabricaFuentesCm.crear(csv) instanceof FuenteCmCsv);
        verificar("archivo .csv -> FuenteCmCsv de su carpeta",
                FabricaFuentesCm.crear(csv + "/LNCEL.csv") instanceof FuenteCmCsv);
        verificar("jdbc: -> FuenteCmBaseDeDatos",
                FabricaFuentesCm.crear("jdbc:sqlite:cm.dba") instanceof FuenteCmBaseDeDatos);
        boolean rechazado = false;
        try {
            FabricaFuentesCm.crear("datos.txt");
        } catch (ValorInvalidoException e) {
            rechazado = true;
        }
        verificar("Un origen desconocido se rechaza (ValorInvalidoException)", rechazado);

        System.out.println();
        System.out.println("Lectura (polimorfismo: el mismo leerObjetos en cada fuente)");
        FuenteCm fuenteRaml = FabricaFuentesCm.crear(raml);
        FuenteCm fuenteCsv = FabricaFuentesCm.crear(csv);
        ListaEnlazada<ObjetoCm> celdasRaml = fuenteRaml.leerObjetos("LNCEL");
        ListaEnlazada<ObjetoCm> celdasCsv = fuenteCsv.leerObjetos("lncel");
        verificar("RAML y CSV leen las mismas 4 celdas",
                celdasRaml.tamanio() == 4 && celdasCsv.tamanio() == 4);
        ObjetoCm b = celdasRaml.obtener(1);
        verificar("LNCEL-2 de MRBTS-101: pMax 46 y nombre CM_SITE_101_B",
                b.getDistName().equals("PLMN-PLMN/MRBTS-101/LNBTS-101/LNCEL-2")
                        && "46".equals(b.getValor("pMax")) && "CM_SITE_101_B".equals(b.getNombre())
                        && "46".equals(celdasCsv.obtener(1).getValor("PMAX")));
        verificar("Las listas (estructuras) del RAML no se leen como parametros",
                b.getValor("drxInactivityT") == null);
        verificar("Una clase que no esta en la fuente devuelve una lista vacia",
                fuenteRaml.leerObjetos("SIB").estaVacia() && fuenteCsv.leerObjetos("SIB").estaVacia());

        // Contrasena de prueba generada al ejecutar (no se versiona ninguna).
        String contrasena = "p" + java.util.UUID.randomUUID().toString().replace("-", "");
        FuenteCm base = FabricaFuentesCm.crear("jdbc:mysql://servidor/cm", "lectura", contrasena);
        String error = "";
        try {
            base.leerObjetos("LNCEL");
        } catch (ExtraccionException e) {
            error = e.getMessage();
        }
        verificar("Base sin driver JDBC: ExtraccionException (" + error + ")",
                error.contains("no hay un driver JDBC de MYSQL"));
        verificar("describir() no muestra la contrasena", !base.describir().contains(contrasena));

        System.out.println();
        System.out.println("Importacion y auditoria del segmento Laboratorio 4G");
        RepositorioEnMemoria repo = new RepositorioEnMemoria();
        DatosDePrueba.cargar(repo);
        Segmento laboratorio = segmento(repo, "Laboratorio 4G");
        ImportadorCm.Resumen resumen = new ImportadorCm(repo, nsn(repo), laboratorio)
                .importar(FabricaFuentesCm.crear(raml));
        verificar("Se importan 3 sitios, 4 celdas y 8 valores (" + resumen + ")",
                resumen.getSitiosNuevos() == 3 && resumen.getCeldasNuevas() == 4
                        && resumen.getValoresCargados() == 8);
        ControladorAuditoria controlador = new ControladorAuditoria(repo);
        ListaEnlazada<Auditoria> resultados = controlador.ejecutarAuditoria(laboratorio);
        verificar("CM_SITE_101_A: pMax 43 y a3Offset -3 CONFORME",
                resultado(resultados, "CM_SITE_101_A", "pMax").equals("CONFORME")
                        && resultado(resultados, "CM_SITE_101_A", "a3Offset").equals("CONFORME"));
        verificar("CM_SITE_101_B: pMax 46 DESVIO y a3Offset -6 DESVIO (con pMax 46 corresponde -3)",
                resultado(resultados, "CM_SITE_101_B", "pMax").equals("DESVIO")
                        && resultado(resultados, "CM_SITE_101_B", "a3Offset").equals("DESVIO"));
        verificar("CM_test_103_A: EXCEPTUADO por el patron *_test_*",
                resultado(resultados, "CM_test_103_A", "pMax").equals("EXCEPTUADO"));
        ListaEnlazada<Auditoria> desvios = controlador.desviosDeUltimaAuditoria();
        int desviosAntes = desvios.tamanio();

        RepositorioEnMemoria repoCsv = new RepositorioEnMemoria();
        DatosDePrueba.cargar(repoCsv);
        Segmento laboratorioCsv = segmento(repoCsv, "Laboratorio 4G");
        new ImportadorCm(repoCsv, nsn(repoCsv), laboratorioCsv).importar(FabricaFuentesCm.crear(csv));
        ControladorAuditoria controladorCsv = new ControladorAuditoria(repoCsv);
        controladorCsv.ejecutarAuditoria(laboratorioCsv);
        verificar("Importado desde CSV da la misma auditoria (" + desviosAntes + " desvios)",
                controladorCsv.desviosDeUltimaAuditoria().tamanio() == desviosAntes);

        System.out.println();
        System.out.println("El plan RAML de NEIPA como fuente: se aplica y se vuelve a auditar");
        String plan = new GeneradorRaml(GeneradorRaml.Tipo.PLAN, laboratorio.getNombre(),
                LocalDateTime.of(2026, 10, 15, 10, 0)).generar(desvios);
        verificar("El plan usa el distName real (.../LNBTS-101/LNCEL-2/CADPR-1)",
                plan.contains("distName=\"PLMN-PLMN/MRBTS-101/LNBTS-101/LNCEL-2/CADPR-1\""));
        File archivoPlan = File.createTempFile("neipa-plan", ".xml");
        archivoPlan.deleteOnExit();
        Files.write(archivoPlan.toPath(), plan.getBytes(StandardCharsets.UTF_8));
        ImportadorCm.Resumen aplicado = new ImportadorCm(repo, nsn(repo), laboratorio)
                .importar(FabricaFuentesCm.crear(archivoPlan.getPath()));
        verificar("El plan no crea elementos y actualiza " + aplicado.getValoresCargados() + " valores",
                aplicado.getSitiosNuevos() == 0 && aplicado.getCeldasNuevas() == 0
                        && aplicado.getValoresCargados() == desviosAntes);
        controlador.ejecutarAuditoria(laboratorio);
        verificar("Despues de aplicar el plan: 0 desvios (antes " + desviosAntes + ")",
                controlador.desviosDeUltimaAuditoria().estaVacia());

        System.out.println();
        System.out.println("Resultado: " + correctos + " de " + total + " casos correctos.");
    }

    private static Segmento segmento(RepositorioEnMemoria repo, String nombre) {
        for (Segmento s : repo.getSegmentos()) {
            if (s.getNombre().equals(nombre)) {
                return s;
            }
        }
        throw new IllegalStateException("falta el segmento " + nombre);
    }

    private static Fabricante nsn(RepositorioEnMemoria repo) {
        for (Fabricante f : repo.getFabricantes()) {
            if (f.getTipoOss().equals("NetAct")) {
                return f;
            }
        }
        throw new IllegalStateException("falta el fabricante NSN");
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
