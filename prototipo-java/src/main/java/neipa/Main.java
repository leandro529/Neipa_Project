package neipa;

import java.util.NoSuchElementException;

import neipa.datos.DatosDePrueba;
import neipa.datos.RepositorioEnMemoria;
import neipa.excepciones.NeipaException;
import neipa.vista.Consola;
import neipa.vista.MenuPrincipal;

/**
 * Punto de entrada del prototipo de NEIPA (Network Element Integrity &
 * Parameter Auditor). Carga los datos de prueba y abre el menu principal.
 */
public class Main {

    public static void main(String[] args) {
        RepositorioEnMemoria repositorio = new RepositorioEnMemoria();
        try {
            DatosDePrueba.cargar(repositorio);
        } catch (NeipaException e) {
            System.out.println("No se pudieron cargar los datos de prueba: " + e.getMessage());
            return;
        }

        MenuPrincipal menu = new MenuPrincipal(repositorio, new Consola(System.in));
        try {
            menu.iniciar();
        } catch (NoSuchElementException e) {
            // La entrada se cerro (por ejemplo, Ctrl+Z o fin de un archivo de entrada).
            System.out.println();
            System.out.println("Entrada finalizada. Fin del programa.");
        }
    }
}
