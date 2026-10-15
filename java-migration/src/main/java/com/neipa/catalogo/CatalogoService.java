package com.neipa.catalogo;

import java.io.IOException;
import java.io.Reader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.neipa.cm.CmNombres;
import com.neipa.domain.CondicionEstandar;
import com.neipa.domain.OperadorComparacion;
import com.neipa.domain.OrigenCondicion;
import com.neipa.domain.ValorEstandar;

/**
 * Catalogo de parametros de Nokia: importacion del CSV (generado a partir de
 * los diccionarios .xls/.xlsx) y validacion de los valores de las reglas.
 *
 * Una regla se valida solo si su parametro esta en el catalogo para la clase
 * de la regla. Como puede haber varias releases (LTE 20, SBTS 19B, 5G21B),
 * un valor es valido si lo acepta al menos una de ellas.
 */
@Service
public class CatalogoService {

    private final CatalogoRepositorio repositorio;
    private final Path carpeta;

    public CatalogoService(CatalogoRepositorio repositorio,
                           @Value("${neipa.cm.carpeta:../external_data}") String carpeta) {
        this.repositorio = repositorio;
        this.carpeta = Paths.get(carpeta);
    }

    /** Archivos .csv de la carpeta de datos de CM. */
    public List<String> archivosDisponibles() {
        if (!Files.isDirectory(carpeta)) {
            return Collections.emptyList();
        }
        try (Stream<Path> archivos = Files.list(carpeta)) {
            return archivos.filter(Files::isRegularFile)
                    .map(p -> p.getFileName().toString())
                    .filter(n -> n.toLowerCase(Locale.ROOT).endsWith(".csv"))
                    .sorted()
                    .collect(Collectors.toList());
        } catch (IOException ex) {
            throw new IllegalStateException("No se pudo leer la carpeta de datos: " + ex.getMessage(), ex);
        }
    }

    /** Importa (reemplaza) el catalogo desde un CSV de la carpeta de datos. */
    public Map<String, Object> importar(String archivo) {
        if (archivo == null || archivo.trim().isEmpty() || archivo.contains("/") || archivo.contains("\\")
                || archivo.contains("..") || !archivo.toLowerCase(Locale.ROOT).endsWith(".csv")) {
            throw new IllegalArgumentException("Indicar el nombre de un archivo .csv de la carpeta de datos");
        }
        Path ruta = carpeta.resolve(archivo.trim());
        if (!Files.isRegularFile(ruta)) {
            throw new IllegalArgumentException("No existe el archivo " + archivo + " en " + carpeta.toAbsolutePath());
        }
        List<ParametroCatalogo> parametros = new ArrayList<>();
        try (Reader lector = Files.newBufferedReader(ruta, StandardCharsets.UTF_8)) {
            CatalogoCsv.leer(lector, parametros::add);
        } catch (IOException ex) {
            throw new IllegalStateException("No se pudo leer " + archivo + ": " + ex.getMessage(), ex);
        }
        int guardados = repositorio.reemplazar(parametros);
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("archivo", archivo);
        r.put("parametros", guardados);
        r.put("fuentes", repositorio.resumen());
        return r;
    }

    public List<ParametroCatalogo> buscar(String texto, String clase, int limite) {
        return repositorio.buscar(texto, clase, limite);
    }

    public List<Map<String, Object>> resumen() {
        return repositorio.resumen();
    }

    /** Clases conocidas para elegir el objeto de una regla. Sin catalogo, las del CM que usa NEIPA. */
    public List<Map<String, Object>> clases() {
        List<Map<String, Object>> clases = new ArrayList<>(repositorio.clases());
        if (clases.isEmpty()) {
            for (String[] c : CLASES_BASICAS) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("clase", c[0]);
                m.put("ruta", c[1]);
                m.put("parametros", 0);
                clases.add(m);
            }
        }
        for (Map<String, Object> c : clases) {
            c.put("ancestros", ancestros(String.valueOf(c.get("clase")), (String) c.get("ruta")));
        }
        return clases;
    }

    private static final String[][] CLASES_BASICAS = {
        {"LNBTS", "MRBTS/LNBTS"}, {"LNCEL", "MRBTS/LNBTS/LNCEL"}, {"LNCEL_FDD", "MRBTS/LNBTS/LNCEL/LNCEL_FDD"},
        {"LNCEL_TDD", "MRBTS/LNBTS/LNCEL/LNCEL_TDD"}, {"SIB", "MRBTS/LNBTS/LNCEL/SIB"},
        {"CADPR", "MRBTS/LNBTS/LNCEL/CADPR"}, {"LNBTS_FDD", "MRBTS/LNBTS/LNBTS_FDD"},
    };

    /**
     * Objetos padre de una clase que se pueden usar en una condicion: los de
     * su ruta desde LNBTS hacia abajo (MRBTS/LNBTS/LNCEL/SIB -> LNBTS, LNCEL).
     * Los que estan por encima de LNBTS no se importan del CM.
     */
    public static List<String> ancestros(String clase, String ruta) {
        List<String> resultado = new ArrayList<>();
        if (ruta == null || ruta.trim().isEmpty()) {
            if (!CmNombres.LNBTS.equalsIgnoreCase(clase)) {
                resultado.add(CmNombres.LNBTS);
            }
            return resultado;
        }
        String[] partes = ruta.split("/");
        boolean desdeLnbts = false;
        for (int i = 0; i < partes.length - 1; i++) {
            String p = partes[i].trim();
            if (CmNombres.LNBTS.equalsIgnoreCase(p)) {
                desdeLnbts = true;
            }
            if (desdeLnbts && !p.isEmpty()) {
                resultado.add(p);
            }
        }
        return resultado;
    }

    /** Ficha de un parametro: la clase puede venir como objeto gestionado ("LNCEL-1/SIB-0", vacio = LNBTS). */
    public List<ParametroCatalogo> ficha(String objetoGestionado, String parametro) {
        if (parametro == null || parametro.trim().isEmpty()) {
            return Collections.emptyList();
        }
        return repositorio.de(CmNombres.claseDeObjetoGestionado(objetoGestionado), parametro.trim());
    }

    /**
     * Valida la regla contra el catalogo. Lanza IllegalArgumentException
     * (HTTP 400) con todos los problemas encontrados.
     */
    public void validar(ValorEstandar regla) {
        List<String> errores = problemas(regla);
        if (!errores.isEmpty()) {
            throw new IllegalArgumentException(String.join("; ", errores));
        }
    }

    public List<String> problemas(ValorEstandar regla) {
        List<String> errores = new ArrayList<>();
        String clase = CmNombres.claseDeObjetoGestionado(regla.getObjetoGestionado());
        List<ParametroCatalogo> fichas = repositorio.de(clase, regla.getParametro());
        if (!fichas.isEmpty()) {
            OperadorComparacion op = regla.getOperador() == null ? OperadorComparacion.RANGO : regla.getOperador();
            if (op == OperadorComparacion.RANGO) {
                revisar(errores, clase, regla.getParametro(), fichas, texto(regla.getMinimo()), "minimo");
                revisar(errores, clase, regla.getParametro(), fichas, texto(regla.getMaximo()), "maximo");
            } else {
                revisarValor(errores, clase, regla.getParametro(), fichas, op, regla.getValorReferencia());
            }
            if (regla.getValorObjetivo() != null && !regla.getValorObjetivo().trim().isEmpty()) {
                revisar(errores, clase, regla.getParametro(), fichas, regla.getValorObjetivo().trim(), "valor objetivo");
            }
        }
        if (regla.getCondiciones() != null) {
            for (CondicionEstandar c : regla.getCondiciones()) {
                if (c.getOrigen() != OrigenCondicion.PARAMETRO || c.getCampo() == null) {
                    continue;
                }
                String campo = c.getCampo().trim();
                int punto = campo.indexOf('.');
                String claseCond = punto > 0 ? campo.substring(0, punto).toUpperCase(Locale.ROOT) : clase;
                String paramCond = punto > 0 ? campo.substring(punto + 1) : campo;
                List<ParametroCatalogo> fichasCond = repositorio.de(claseCond, paramCond);
                if (!fichasCond.isEmpty()) {
                    revisarValor(errores, claseCond, paramCond, fichasCond, c.getOperador(), c.getValor());
                }
            }
        }
        return errores;
    }

    private void revisarValor(List<String> errores, String clase, String parametro, List<ParametroCatalogo> fichas,
                              OperadorComparacion op, String valor) {
        if (valor == null || op == OperadorComparacion.COINCIDE) {
            return;
        }
        if (op == OperadorComparacion.EN_LISTA) {
            for (String v : valor.split(",")) {
                revisar(errores, clase, parametro, fichas, v.trim(), "valor");
            }
        } else {
            revisar(errores, clase, parametro, fichas, valor.trim(), "valor");
        }
    }

    private void revisar(List<String> errores, String clase, String parametro, List<ParametroCatalogo> fichas,
                         String valor, String que) {
        if (valor == null || valor.isEmpty()) {
            return;
        }
        for (ParametroCatalogo f : fichas) {
            if (acepta(f, valor)) {
                return;
            }
        }
        errores.add("El " + que + " " + valor + " no es valido para " + clase + "." + parametro + " segun el catalogo ("
                + permitido(fichas.get(0)) + ")");
    }

    /** True si el valor (en el formato del CM) es valido para esa ficha. */
    static boolean acepta(ParametroCatalogo f, String valor) {
        Map<String, String> enumerados = f.valoresEnumerados();
        if (!enumerados.isEmpty()) {
            for (String clave : enumerados.keySet()) {
                if (mismoNumero(clave, valor) || clave.equalsIgnoreCase(valor)) {
                    return true;
                }
            }
            return false;
        }
        if (f.getMinimoInterno() == null && f.getMaximoInterno() == null) {
            return true; // sin rango conocido (texto, estructura, formula no lineal)
        }
        BigDecimal v;
        try {
            v = new BigDecimal(valor);
        } catch (NumberFormatException ex) {
            return false;
        }
        return (f.getMinimoInterno() == null || v.compareTo(BigDecimal.valueOf(f.getMinimoInterno())) >= 0)
                && (f.getMaximoInterno() == null || v.compareTo(BigDecimal.valueOf(f.getMaximoInterno())) <= 0);
    }

    static String permitido(ParametroCatalogo f) {
        Map<String, String> enumerados = f.valoresEnumerados();
        if (!enumerados.isEmpty()) {
            String lista = enumerados.entrySet().stream().limit(20)
                    .map(e -> e.getKey() + "=" + e.getValue()).collect(Collectors.joining(", "));
            return "valores permitidos: " + lista + (enumerados.size() > 20 ? ", ..." : "");
        }
        StringBuilder s = new StringBuilder("rango en el CM: ");
        s.append(f.getMinimoInterno() == null ? "-" : texto(BigDecimal.valueOf(f.getMinimoInterno())));
        s.append("..");
        s.append(f.getMaximoInterno() == null ? "-" : texto(BigDecimal.valueOf(f.getMaximoInterno())));
        if (f.getRango() != null) {
            s.append("; documento: ").append(f.getRango().split("\n")[0]);
        }
        if (f.getFormula() != null && f.getFormula().matches(".*[*/+].*") && !f.getFormula().contains("* 1 / 1")) {
            s.append("; ").append(f.getFormula());
        }
        return s.toString();
    }

    private static boolean mismoNumero(String a, String b) {
        try {
            return new BigDecimal(a).compareTo(new BigDecimal(b)) == 0;
        } catch (NumberFormatException ex) {
            return false;
        }
    }

    private static String texto(BigDecimal v) {
        return v == null ? null : v.stripTrailingZeros().toPlainString();
    }
}
