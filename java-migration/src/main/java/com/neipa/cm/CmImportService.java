package com.neipa.cm;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Supplier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.neipa.domain.CondicionEstandar;
import com.neipa.domain.ElementoDeRed;
import com.neipa.domain.OrigenCondicion;
import com.neipa.domain.Segmento;
import com.neipa.domain.ValorEstandar;
import com.neipa.repository.ElementoDeRedRepository;
import com.neipa.repository.SegmentoRepository;
import com.neipa.repository.ValorEstandarRepository;

/**
 * Importa a MySQL los datos de CM de un dump SQLite de NetAct.
 *
 * Que se importa:
 * <ul>
 *   <li>El inventario de LNBTS y LNCEL (distName, RC, ids, nombre, version).</li>
 *   <li>De cada clase que usan las reglas (LNCEL, LNCEL_FDD, SIB, CADPR, ...):
 *       sus objetos y SOLO los parametros que tienen una regla o que usa una
 *       condicion. Con ~950 parametros por celda, importar todo serian
 *       cientos de millones de filas; asi, son pocas columnas por clase.</li>
 *   <li>No se importan las clases excluidas (por defecto LNREL* y LNADJ*,
 *       las vecindades: mas de 20 millones de filas que NEIPA no audita).</li>
 * </ul>
 * Ademas crea un segmento por RC (PLMN_id) si no existe y da de alta / actualiza
 * los elementos de red (un elemento por LNBTS, con su distName).
 *
 * Al agregar una regla de un parametro nuevo, hay que volver a importar el
 * dump para traer esa columna.
 */
@Service
public class CmImportService {

    private static final Logger LOG = LoggerFactory.getLogger(CmImportService.class);
    private static final List<String> EXTENSIONES = Arrays.asList(".dba", ".sqlite", ".sqlite3", ".db");

    private final CmRepositorio repositorio;
    private final ValorEstandarRepository valorEstandarRepository;
    private final SegmentoRepository segmentoRepository;
    private final ElementoDeRedRepository elementoDeRedRepository;
    private final CmFuenteFactory fuenteFactory;
    private final Path carpeta;
    private final List<String> clasesExcluidas;
    private final ExecutorService ejecutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "neipa-importacion-cm");
        t.setDaemon(true);
        return t;
    });

    @Autowired
    public CmImportService(CmRepositorio repositorio, ValorEstandarRepository valorEstandarRepository,
                           SegmentoRepository segmentoRepository, ElementoDeRedRepository elementoDeRedRepository,
                           @Value("${neipa.cm.carpeta:../external_data}") String carpeta,
                           @Value("${neipa.cm.clases-excluidas:LNREL*,LNADJ*}") String clasesExcluidas) {
        this(repositorio, valorEstandarRepository, segmentoRepository, elementoDeRedRepository,
                SqliteCmFuente::new, Paths.get(carpeta), Arrays.asList(clasesExcluidas.split(",")));
    }

    public CmImportService(CmRepositorio repositorio, ValorEstandarRepository valorEstandarRepository,
                           SegmentoRepository segmentoRepository, ElementoDeRedRepository elementoDeRedRepository,
                           CmFuenteFactory fuenteFactory, Path carpeta, List<String> clasesExcluidas) {
        this.repositorio = repositorio;
        this.valorEstandarRepository = valorEstandarRepository;
        this.segmentoRepository = segmentoRepository;
        this.elementoDeRedRepository = elementoDeRedRepository;
        this.fuenteFactory = fuenteFactory;
        this.carpeta = carpeta;
        this.clasesExcluidas = clasesExcluidas.stream().map(String::trim).filter(x -> !x.isEmpty())
                .collect(Collectors.toList());
    }

    /** Archivos de dump disponibles en la carpeta configurada (neipa.cm.carpeta). */
    public List<String> archivosDisponibles() {
        if (!Files.isDirectory(carpeta)) {
            return Collections.emptyList();
        }
        try (Stream<Path> archivos = Files.list(carpeta)) {
            return archivos.filter(Files::isRegularFile)
                    .map(p -> p.getFileName().toString())
                    .filter(n -> EXTENSIONES.stream().anyMatch(e -> n.toLowerCase(Locale.ROOT).endsWith(e)))
                    .sorted(Collections.reverseOrder())
                    .collect(Collectors.toList());
        } catch (IOException ex) {
            throw new IllegalStateException("No se pudo leer la carpeta de datos de CM: " + ex.getMessage(), ex);
        }
    }

    /** Lanza la importacion de un dump de la carpeta en segundo plano y devuelve su id. */
    public long iniciar(String archivo) {
        Path ruta = resolver(archivo);
        return iniciar(archivo, () -> fuenteFactory.abrir(ruta));
    }

    /**
     * Lanza la importacion desde cualquier fuente (por ejemplo una conexion a
     * una base con la estructura del dump) y devuelve su id (cm_importaciones).
     */
    public long iniciar(String origen, Supplier<CmFuente> apertura) {
        long id = repositorio.crearImportacion(origen);
        ejecutor.submit(() -> importar(id, apertura));
        return id;
    }

    /** Solo nombres de archivo dentro de la carpeta configurada (sin rutas ni ".."). */
    Path resolver(String archivo) {
        if (archivo == null || archivo.trim().isEmpty() || archivo.contains("/") || archivo.contains("\\")
                || archivo.contains("..")) {
            throw new IllegalArgumentException("Indicar solo el nombre de un archivo de la carpeta de datos de CM");
        }
        Path ruta = carpeta.resolve(archivo.trim());
        if (!Files.isRegularFile(ruta)) {
            throw new IllegalArgumentException("No existe el archivo " + archivo + " en la carpeta de datos de CM ("
                    + carpeta.toAbsolutePath() + ")");
        }
        return ruta;
    }

    /** Importacion sincronica (la usa iniciar() en segundo plano, y los tests). */
    public void importar(long idImportacion, Path ruta) {
        importar(idImportacion, () -> fuenteFactory.abrir(ruta));
    }

    /** Importacion sincronica desde una fuente cualquiera. */
    public void importar(long idImportacion, Supplier<CmFuente> apertura) {
        List<String> avisos = new ArrayList<>();
        long[] totales = new long[2]; // objetos, valores
        String[] fechaDump = new String[1];
        try (CmFuente fuente = apertura.get()) {
            // Sin distinguir mayusculas: una base puede tener lncel / LNCEL_ID en vez de LNCEL / LNCEL_id.
            Set<String> tablas = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
            tablas.addAll(fuente.tablas());
            Map<String, Set<String>> parametros = parametrosPorClase(avisos);

            List<String> clases = new ArrayList<>();
            clases.add(CmNombres.LNBTS);
            clases.add(CmNombres.LNCEL);
            for (String clase : parametros.keySet()) {
                if (!clases.contains(clase)) {
                    clases.add(clase);
                }
            }

            Map<String, String> nombres = new HashMap<>();
            for (String clase : clases) {
                if (!tablas.contains(clase)) {
                    avisos.add("El dump no tiene la clase " + clase + ".");
                    continue;
                }
                Set<String> pedidos = parametros.getOrDefault(clase, Collections.emptySet());
                Set<String> columnas = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
                columnas.addAll(fuente.columnas(clase));
                if (!CmNombres.LNBTS.equals(clase) && !columnas.contains("LNBTS_id")) {
                    // Solo los objetos que dependen de un LNBTS (no MRBTS/MNL/LTAC, etc.).
                    avisos.add("La clase " + clase + " no depende de LNBTS: no se importa.");
                    continue;
                }
                Set<String> disponibles = new LinkedHashSet<>();
                for (String p : pedidos) {
                    if (columnas.contains(p)) {
                        disponibles.add(p);
                    } else {
                        avisos.add("La clase " + clase + " no tiene el parametro " + p + ".");
                    }
                }
                List<CmObjeto> objetos = leer(fuente, clase, columnas, disponibles, nombres, fechaDump);
                repositorio.guardarObjetos(objetos, idImportacion);
                repositorio.borrarObjetosAnteriores(clase, idImportacion);
                repositorio.borrarValores(clase, disponibles);
                repositorio.guardarValores(objetos);
                totales[0] += objetos.size();
                totales[1] += objetos.stream().mapToLong(o -> o.getValores().size()).sum();
                if (CmNombres.LNBTS.equals(clase)) {
                    sincronizarSegmentosYElementos(objetos);
                }
                repositorio.actualizarImportacion(idImportacion, "EN_CURSO", fechaDump[0], totales[0], totales[1],
                        "Importada la clase " + clase, false);
                LOG.info("CM: {} objetos de {} ({} parametros)", objetos.size(), clase, disponibles.size());
            }
            repositorio.actualizarImportacion(idImportacion, "OK", fechaDump[0], totales[0], totales[1],
                    String.join("\n", avisos), true);
        } catch (RuntimeException ex) {
            LOG.error("Fallo la importacion de CM", ex);
            avisos.add("ERROR: " + ex.getMessage());
            repositorio.actualizarImportacion(idImportacion, "ERROR", fechaDump[0], totales[0], totales[1],
                    String.join("\n", avisos), true);
        }
    }

    /**
     * Clase -> parametros a importar, segun las reglas cargadas: el parametro
     * de cada regla en la clase de su objeto gestionado, y los parametros de
     * sus condiciones (en la misma clase, o "CLASE.parametro" para un ancestro).
     */
    Map<String, Set<String>> parametrosPorClase(List<String> avisos) {
        Map<String, Set<String>> resultado = new LinkedHashMap<>();
        for (ValorEstandar regla : valorEstandarRepository.findAll()) {
            String clase = CmNombres.claseDeObjetoGestionado(regla.getObjetoGestionado());
            agregar(resultado, clase, regla.getParametro(), avisos);
            for (CondicionEstandar c : regla.getCondiciones()) {
                if (c.getOrigen() != OrigenCondicion.PARAMETRO) {
                    continue;
                }
                String campo = c.getCampo();
                int punto = campo.indexOf('.');
                if (punto > 0) {
                    agregar(resultado, campo.substring(0, punto).toUpperCase(Locale.ROOT),
                            campo.substring(punto + 1), avisos);
                } else {
                    agregar(resultado, clase, campo, avisos);
                }
            }
        }
        return resultado;
    }

    private void agregar(Map<String, Set<String>> mapa, String clase, String parametro, List<String> avisos) {
        if (CmNombres.excluida(clase, clasesExcluidas)) {
            String aviso = "La clase " + clase + " esta excluida de la importacion (" + parametro + " no se audita).";
            if (!avisos.contains(aviso)) {
                avisos.add(aviso);
            }
            return;
        }
        mapa.computeIfAbsent(clase, k -> new LinkedHashSet<>()).add(parametro);
    }

    private List<CmObjeto> leer(CmFuente fuente, String clase, Set<String> columnas, Set<String> parametros,
                                Map<String, String> nombres, String[] fechaDump) {
        List<String> pedidas = new ArrayList<>();
        for (String c : Arrays.asList("distName", "PLMN_id", "MRBTS_id", "LNBTS_id", "LNCEL_id", "name",
                "moVersion", "timeStamp")) {
            if (columnas.contains(c)) {
                pedidas.add(c);
            }
        }
        pedidas.addAll(parametros);
        List<CmObjeto> objetos = new ArrayList<>();
        fuente.recorrer(clase, pedidas, fila -> {
            String distName = texto(fila.get("distName"));
            if (distName == null) {
                return;
            }
            if (fechaDump[0] == null) {
                fechaDump[0] = texto(fila.get("timeStamp"));
            }
            String nombre = texto(fila.get("name"));
            if (nombre == null) {
                nombre = nombreDeAncestro(distName, nombres);
            }
            if (CmNombres.LNBTS.equals(clase) || CmNombres.LNCEL.equals(clase)) {
                nombres.put(distName, nombre);
            }
            CmObjeto o = new CmObjeto(distName, clase, texto(fila.get("PLMN_id")), entero(fila.get("MRBTS_id")),
                    entero(fila.get("LNBTS_id")), entero(fila.get("LNCEL_id")), nombre, texto(fila.get("moVersion")));
            for (String p : parametros) {
                String valor = texto(fila.get(p));
                if (valor != null) {
                    o.getValores().put(p, valor);
                }
            }
            objetos.add(o);
        });
        return objetos;
    }

    private static String nombreDeAncestro(String distName, Map<String, String> nombres) {
        for (String clase : Arrays.asList(CmNombres.LNCEL, CmNombres.LNBTS)) {
            String ancestro = CmNombres.ancestro(distName, clase);
            if (ancestro != null && nombres.get(ancestro) != null) {
                return nombres.get(ancestro);
            }
        }
        return null;
    }

    /** Un segmento por RC (si no hay uno) y un elemento de red por LNBTS. */
    void sincronizarSegmentosYElementos(List<CmObjeto> lnbts) {
        Map<String, Segmento> segmentoPorRc = new HashMap<>();
        for (Segmento s : segmentoRepository.findAll()) {
            if (s.esDeCm() && (s.getFiltroNombre() == null || s.getFiltroNombre().trim().isEmpty())) {
                segmentoPorRc.putIfAbsent(s.getRc().toLowerCase(Locale.ROOT), s);
            }
        }
        for (String rc : lnbts.stream().map(CmObjeto::getRc).filter(r -> r != null)
                .map(r -> r.toLowerCase(Locale.ROOT)).collect(Collectors.toCollection(java.util.TreeSet::new))) {
            if (!segmentoPorRc.containsKey(rc)) {
                Segmento nuevo = new Segmento(rc.toUpperCase(Locale.ROOT), "CM NetAct: PLMN_id = " + rc);
                nuevo.setRc(rc);
                segmentoPorRc.put(rc, segmentoRepository.save(nuevo));
            }
        }
        Map<String, ElementoDeRed> existentes = new HashMap<>();
        for (ElementoDeRed e : elementoDeRedRepository.findAll()) {
            if (e.getDistName() != null) {
                existentes.put(e.getDistName(), e);
            }
        }
        List<ElementoDeRed> aGuardar = new ArrayList<>();
        for (CmObjeto o : lnbts) {
            Segmento segmento = o.getRc() == null ? null : segmentoPorRc.get(o.getRc().toLowerCase(Locale.ROOT));
            Long idSegmento = segmento == null ? null : segmento.getIdSegmento();
            String nombre = JdbcCmRepositorio.recortar(o.getNombre() != null ? o.getNombre() : o.getDistName(), 60);
            ElementoDeRed e = existentes.get(o.getDistName());
            if (e == null) {
                e = new ElementoDeRed(nombre, "4G", "NSN", idSegmento);
                e.setDistName(o.getDistName());
                aGuardar.add(e);
            } else if (!nombre.equals(e.getNombre()) || (idSegmento != null && !idSegmento.equals(e.getIdSegmento()))) {
                e.setNombre(nombre);
                e.setIdSegmento(idSegmento);
                aGuardar.add(e);
            }
        }
        elementoDeRedRepository.saveAll(aGuardar);
    }

    static String texto(Object valor) {
        if (valor == null) {
            return null;
        }
        if (valor instanceof Double || valor instanceof Float) {
            double d = ((Number) valor).doubleValue();
            if (d == Math.rint(d) && !Double.isInfinite(d) && Math.abs(d) < 1e15) {
                return String.valueOf((long) d);
            }
            return String.valueOf(d);
        }
        if (valor instanceof java.math.BigDecimal) { // Oracle NUMBER, DECIMAL
            return ((java.math.BigDecimal) valor).stripTrailingZeros().toPlainString();
        }
        if (valor instanceof Boolean) {
            return ((Boolean) valor) ? "1" : "0";
        }
        String s = valor.toString().trim();
        return s.isEmpty() ? null : s;
    }

    private static Integer entero(Object valor) {
        if (valor instanceof Number) {
            return ((Number) valor).intValue();
        }
        if (valor == null) {
            return null;
        }
        try {
            return Integer.valueOf(valor.toString().trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
