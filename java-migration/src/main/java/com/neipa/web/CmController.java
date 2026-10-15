package com.neipa.web;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.neipa.cm.CmImportService;
import com.neipa.cm.CmRepositorio;
import com.neipa.cm.MotorCm;
import com.neipa.cm.ResumenRegla;
import com.neipa.cm.conexion.CmConexion;
import com.neipa.cm.conexion.CmConexionService;
import com.neipa.domain.Corrida;
import com.neipa.repository.CorridaRepository;

/**
 * Datos de CM de NetAct: importacion del dump SQLite y seguimiento de las
 * corridas de auditoria (evolucion de los desvios corrida a corrida).
 */
@RestController
@RequestMapping("/api/cm")
public class CmController {

    private final CmImportService importService;
    private final CmRepositorio cmRepositorio;
    private final CorridaRepository corridaRepository;
    private final CmConexionService conexionService;

    public CmController(CmImportService importService, CmRepositorio cmRepositorio,
                        CorridaRepository corridaRepository, CmConexionService conexionService) {
        this.importService = importService;
        this.cmRepositorio = cmRepositorio;
        this.corridaRepository = corridaRepository;
        this.conexionService = conexionService;
    }

    /** Dumps disponibles en la carpeta de datos de CM (neipa.cm.carpeta). */
    @GetMapping("/archivos")
    public List<String> archivos() {
        return importService.archivosDisponibles();
    }

    /**
     * Inicia una importacion en segundo plano. Body: {"archivo": "20240124_sqlite.dba"} para un dump de la
     * carpeta, o {"idConexion": 3} para leer directo de una base con la estructura del CM.
     */
    @PostMapping("/importaciones")
    public ResponseEntity<Map<String, Object>> importar(@RequestBody Map<String, Object> pedido) {
        Object idConexion = pedido == null ? null : pedido.get("idConexion");
        long id;
        if (idConexion != null && !idConexion.toString().trim().isEmpty()) {
            long idc;
            try {
                idc = Long.parseLong(idConexion.toString().trim());
            } catch (NumberFormatException ex) {
                throw new IllegalArgumentException("idConexion invalido: " + idConexion);
            }
            String origen = conexionService.origen(idc);
            id = importService.iniciar(origen, () -> conexionService.abrirFuente(idc));
        } else {
            Object archivo = pedido == null ? null : pedido.get("archivo");
            id = importService.iniciar(archivo == null ? null : archivo.toString());
        }
        Map<String, Object> respuesta = new HashMap<>();
        respuesta.put("idImportacion", id);
        respuesta.put("estado", "EN_CURSO");
        return ResponseEntity.accepted().body(respuesta);
    }

    @GetMapping("/importaciones")
    public List<Map<String, Object>> importaciones() {
        return cmRepositorio.listarImportaciones();
    }

    /** Cantidad de objetos importados por clase y RC. */
    @GetMapping("/objetos/resumen")
    public List<Map<String, Object>> resumenObjetos() {
        return cmRepositorio.resumenObjetos();
    }

    @GetMapping("/corridas")
    public List<Corrida> corridas(@RequestParam(required = false) Long idSegmento) {
        return idSegmento == null ? corridaRepository.findAllByOrderByIdCorridaAsc()
                : corridaRepository.findByIdSegmentoOrderByIdCorridaAsc(idSegmento);
    }

    @GetMapping("/corridas/{id}/resumen")
    public List<ResumenRegla> resumenCorrida(@PathVariable Long id) {
        return cmRepositorio.resumenDeCorrida(id);
    }

    /** Motores soportados para conectarse directo, con una URL de ejemplo. */
    @GetMapping("/motores")
    public List<Map<String, String>> motores() {
        List<Map<String, String>> lista = new java.util.ArrayList<>();
        for (MotorCm m : MotorCm.values()) {
            if (m != MotorCm.SQLITE) {
                Map<String, String> x = new HashMap<>();
                x.put("motor", m.name());
                x.put("urlEjemplo", m.getUrlEjemplo());
                lista.add(x);
            }
        }
        return lista;
    }

    @GetMapping("/conexiones")
    public List<CmConexion> conexiones() {
        return conexionService.listar();
    }

    @GetMapping("/conexiones/{id}")
    public CmConexion conexion(@PathVariable long id) {
        return conexionService.obtener(id);
    }

    @PostMapping("/conexiones")
    public CmConexion crearConexion(@RequestBody CmConexion conexion) {
        return conexionService.crear(conexion);
    }

    @PutMapping("/conexiones/{id}")
    public CmConexion actualizarConexion(@PathVariable long id, @RequestBody CmConexion conexion) {
        return conexionService.actualizar(id, conexion);
    }

    @DeleteMapping("/conexiones/{id}")
    public ResponseEntity<Void> borrarConexion(@PathVariable long id) {
        conexionService.borrar(id);
        return ResponseEntity.noContent().build();
    }

    /** Prueba una conexion guardada: se conecta, lista las tablas y verifica la estructura del CM. */
    @PostMapping("/conexiones/{id}/prueba")
    public Map<String, Object> probarConexion(@PathVariable long id) {
        return conexionService.probar(id);
    }

    /** Prueba los datos del formulario sin guardarlos. */
    @PostMapping("/conexiones/prueba")
    public Map<String, Object> probarSinGuardar(@RequestBody CmConexion conexion) {
        return conexionService.probarSinGuardar(conexion);
    }
}
