package neipa.fuentes;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import neipa.estructuras.ListaEnlazada;
import neipa.excepciones.ExtraccionException;

/**
 * Fuente de CM en archivos CSV: una carpeta con un archivo por clase
 * (LNBTS.csv, LNCEL.csv, CADPR.csv...), con la misma estructura que las
 * tablas del dump de NetAct: una columna distName y una por parametro.
 * Acepta coma o punto y coma como separador.
 */
public class FuenteCmCsv extends FuenteCm {

    public FuenteCmCsv(String carpeta) {
        super(carpeta);
    }

    @Override
    public String getTipo() {
        return "Carpeta de CSV (una tabla por clase)";
    }

    @Override
    protected void abrir() throws ExtraccionException {
        if (!new File(getOrigen()).isDirectory()) {
            throw new ExtraccionException("no existe la carpeta " + getOrigen());
        }
    }

    @Override
    protected ListaEnlazada<ObjetoCm> leerFilas(String clase) throws ExtraccionException {
        ListaEnlazada<ObjetoCm> objetos = new ListaEnlazada<ObjetoCm>();
        File archivo = new File(getOrigen(), clase + ".csv");
        if (!archivo.isFile()) {
            return objetos; // la carpeta no tiene esa clase
        }
        try (BufferedReader lector = Files.newBufferedReader(archivo.toPath(), StandardCharsets.UTF_8)) {
            String cabecera = lector.readLine();
            if (cabecera == null) {
                return objetos;
            }
            String separador = cabecera.contains(";") ? ";" : ",";
            String[] columnas = partir(cabecera, separador);
            int columnaDistName = -1;
            for (int i = 0; i < columnas.length; i++) {
                if (columnas[i].equalsIgnoreCase("distName")) {
                    columnaDistName = i;
                }
            }
            if (columnaDistName < 0) {
                throw new ExtraccionException(archivo.getName() + " no tiene la columna distName");
            }
            String linea;
            while ((linea = lector.readLine()) != null) {
                if (linea.trim().isEmpty()) {
                    continue;
                }
                String[] celdas = partir(linea, separador);
                ObjetoCm objeto = nuevoObjeto(clase, celdas[columnaDistName]);
                for (int i = 0; i < columnas.length && i < celdas.length; i++) {
                    if (i != columnaDistName) {
                        agregarValor(objeto, columnas[i], celdas[i]);
                    }
                }
                objetos.agregarAlFinal(objeto);
            }
        } catch (IOException e) {
            throw new ExtraccionException("no se pudo leer " + archivo.getPath() + ": " + e.getMessage());
        } catch (IllegalArgumentException e) {
            throw new ExtraccionException(archivo.getName() + ": " + e.getMessage());
        }
        return objetos;
    }

    /** Separa una linea; quita las comillas que envuelven un valor. */
    private static String[] partir(String linea, String separador) {
        String[] partes = linea.split(separador.equals(";") ? ";" : ",", -1);
        for (int i = 0; i < partes.length; i++) {
            String p = partes[i].trim();
            if (p.length() >= 2 && p.startsWith("\"") && p.endsWith("\"")) {
                p = p.substring(1, p.length() - 1);
            }
            partes[i] = p;
        }
        return partes;
    }
}
