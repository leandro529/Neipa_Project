package neipa.fuentes;

import java.io.File;

import neipa.excepciones.ValorInvalidoException;

/**
 * Patron Factory: crea la fuente de CM que corresponde a un origen, sin que
 * quien la usa (el importador, el menu) tenga que conocer las subclases.
 *
 *   jdbc:...            base de datos (SQLite, MySQL, PostgreSQL, Oracle, SQL Server)
 *   archivo .xml        XML RAML 2.0 exportado de NetAct (o un plan de NEIPA)
 *   carpeta o .csv      carpeta con un CSV por clase
 *
 * Para sumar un formato nuevo se agrega una subclase de FuenteCm y una regla
 * aca; el resto del sistema no cambia.
 */
public final class FabricaFuentesCm {

    private FabricaFuentesCm() {
        // Solo metodos de clase.
    }

    public static FuenteCm crear(String origen) throws ValorInvalidoException {
        return crear(origen, null, null);
    }

    public static FuenteCm crear(String origen, String usuario, String contrasena)
            throws ValorInvalidoException {
        if (origen == null || origen.trim().isEmpty()) {
            throw new ValorInvalidoException("Indique el origen de los datos de CM.");
        }
        String o = origen.trim();
        String minuscula = o.toLowerCase();
        if (minuscula.startsWith("jdbc:")) {
            return new FuenteCmBaseDeDatos(o, usuario, contrasena);
        }
        if (minuscula.endsWith(".xml")) {
            return new FuenteCmRaml(o);
        }
        File archivo = new File(o);
        if (archivo.isDirectory()) {
            return new FuenteCmCsv(o);
        }
        if (minuscula.endsWith(".csv")) {
            // Un CSV de la carpeta: se usa la carpeta completa.
            // Un CSV de la carpeta: se usa la carpeta completa.
            File carpeta = archivo.getAbsoluteFile().getParentFile();
            return new FuenteCmCsv(carpeta.getPath());
        }
        throw new ValorInvalidoException("No se reconoce el origen " + o
                + ": use un archivo .xml (RAML), una carpeta de CSV"
                + " o una URL jdbc:");
    }
}
