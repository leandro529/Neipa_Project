package neipa.vista;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Locale;

import neipa.algoritmos.Buscador;
import neipa.algoritmos.ComparadorPorNombre;
import neipa.algoritmos.Ordenador;
import neipa.control.ControladorAuditoria;
import neipa.control.ControladorRollback;
import neipa.control.ImportadorCm;
import neipa.datos.RepositorioEnMemoria;
import neipa.estructuras.ListaEnlazada;
import neipa.excepciones.ExtraccionException;
import neipa.fuentes.FabricaFuentesCm;
import neipa.fuentes.FuenteCm;
import neipa.excepciones.NeipaException;
import neipa.excepciones.ValorInvalidoException;
import neipa.modelo.Antena;
import neipa.modelo.Auditoria;
import neipa.modelo.CambioConfiguracion;
import neipa.modelo.Celda;
import neipa.modelo.ElementoDeRed;
import neipa.modelo.Excepcion;
import neipa.modelo.Fabricante;
import neipa.modelo.Parametro;
import neipa.modelo.Segmento;
import neipa.modelo.Sitio;
import neipa.modelo.ValorEstandar;
import neipa.modelo.ValorVigente;
import neipa.reportes.GeneradorRaml;
import neipa.reportes.Reporte;
import neipa.reportes.ReporteEjecutivo;
import neipa.reportes.ReporteTecnico;

/**
 * Menu de seleccion del prototipo (clase de interfaz). Lee la opcion del
 * usuario y delega el trabajo en las clases de control y de entidad.
 */
public class MenuPrincipal {

    private final RepositorioEnMemoria repositorio;
    private final Consola consola;
    private final ControladorAuditoria controladorAuditoria;
    private final ControladorRollback controladorRollback;

    public MenuPrincipal(RepositorioEnMemoria repositorio, Consola consola) {
        this.repositorio = repositorio;
        this.consola = consola;
        this.controladorAuditoria = new ControladorAuditoria(repositorio);
        this.controladorRollback = new ControladorRollback();
    }

    /** Ciclo principal: muestra el menu hasta que el usuario elige salir. */
    public void iniciar() {
        int opcion;
        do {
            mostrarOpciones();
            opcion = consola.leerEntero("Opcion", 0, 13);
            try {
                ejecutar(opcion);
            } catch (NeipaException e) {
                // Errores de negocio previstos: se informan y el menu continua.
                consola.mostrar("  No se pudo completar la operacion: " + e.getMessage());
            } catch (IllegalArgumentException e) {
                // Datos que no cumplen las validaciones de los constructores.
                consola.mostrar("  Dato invalido: " + e.getMessage());
            }
        } while (opcion != 0);
        consola.mostrar("Fin del programa.");
    }

    private void mostrarOpciones() {
        consola.mostrarTitulo("NEIPA - Network Element Integrity & Parameter Auditor");
        consola.mostrar(" 1. Listar elementos de red (ordenados por nombre)");
        consola.mostrar(" 2. Registrar elemento de red");
        consola.mostrar(" 3. Buscar elemento de red por nombre");
        consola.mostrar(" 4. Ver catalogo de parametros y valores estandar");
        consola.mostrar(" 5. Ejecutar auditoria de un segmento");
        consola.mostrar(" 6. Ver reporte tecnico (historial completo)");
        consola.mostrar(" 7. Ver reporte ejecutivo (historial completo)");
        consola.mostrar(" 8. Registrar excepcion");
        consola.mostrar(" 9. Aplicar correccion a un desvio");
        consola.mostrar("10. Deshacer la ultima correccion (rollback)");
        consola.mostrar("11. Simular caida o recuperacion de la plataforma de un fabricante");
        consola.mostrar("12. Generar XML de reconfiguracion (RAML 2.0: plan y backup)");
        consola.mostrar("13. Importar datos de CM de NSN (XML RAML, CSV o base de datos)");
        consola.mostrar(" 0. Salir");
    }

    private void ejecutar(int opcion) throws NeipaException {
        switch (opcion) {
            case 1:
                listarElementos();
                break;
            case 2:
                registrarElemento();
                break;
            case 3:
                buscarElemento();
                break;
            case 4:
                verCatalogo();
                break;
            case 5:
                iniciarAuditoria();
                break;
            case 6:
                mostrarReporte(new ReporteTecnico());
                break;
            case 7:
                mostrarReporte(new ReporteEjecutivo());
                break;
            case 8:
                registrarExcepcion();
                break;
            case 9:
                aplicarCorreccion();
                break;
            case 10:
                deshacerCorreccion();
                break;
            case 11:
                cambiarDisponibilidad();
                break;
            case 12:
                generarReconfiguracion();
                break;
            case 13:
                importarCm();
                break;
            default:
                break; // 0: salir
        }
    }

    // ----- Opcion 1 -------------------------------------------------------

    private void listarElementos() {
        consola.mostrarTitulo("ELEMENTOS DE RED");
        ElementoDeRed[] ordenados = elementosOrdenadosPorNombre();
        for (int i = 0; i < ordenados.length; i++) {
            ElementoDeRed elemento = ordenados[i];
            // describir() es polimorfico: cada subclase aporta su tipo y su detalle.
            consola.mostrar(String.format("#%-2d %s", elemento.getId(), elemento.describir()));
            consola.mostrar("     vigentes: " + textoDeVigentes(elemento));
        }
        consola.mostrar("Total: " + ordenados.length + " elementos.");
    }

    private ElementoDeRed[] elementosOrdenadosPorNombre() {
        ListaEnlazada<ElementoDeRed> elementos = repositorio.getElementos();
        ElementoDeRed[] arreglo = new ElementoDeRed[elementos.tamanio()];
        elementos.copiarEn(arreglo);
        Ordenador.ordenarPorInsercion(arreglo, new ComparadorPorNombre());
        return arreglo;
    }

    private String textoDeVigentes(ElementoDeRed elemento) {
        try {
            ListaEnlazada<ValorVigente> vigentes = elemento.obtenerParametrosVigentes();
            if (vigentes.estaVacia()) {
                return "(sin valores cargados)";
            }
            StringBuilder texto = new StringBuilder();
            for (ValorVigente vigente : vigentes) {
                if (texto.length() > 0) {
                    texto.append(", ");
                }
                texto.append(vigente);
            }
            return texto.toString();
        } catch (ExtraccionException e) {
            return "(plataforma del fabricante no disponible)";
        }
    }

    // ----- Opcion 2 -------------------------------------------------------

    private void registrarElemento() throws NeipaException {
        consola.mostrarTitulo("REGISTRAR ELEMENTO DE RED");
        consola.mostrar("Tipo de elemento: 1. Sitio   2. Celda   3. Antena");
        int tipo = consola.leerEntero("Tipo", 1, 3);
        String nombre = consola.leerTexto("Nombre");
        if (repositorio.existeElemento(nombre)) {
            throw new NeipaException("Ya existe un elemento de red llamado " + nombre + ".");
        }
        String tecnologia = consola.leerTexto("Tecnologia (por ejemplo 4G o 5G)");
        Fabricante fabricante = seleccionar("Fabricante", repositorio.getFabricantes());
        Segmento segmento = seleccionar("Segmento", repositorio.getSegmentos());

        // La variable es de la clase abstracta; el objeto, de la subclase elegida.
        ElementoDeRed nuevo;
        if (tipo == 1) {
            int sectores = consola.leerEntero("Cantidad de sectores", 1, 12);
            nuevo = new Sitio(nombre, tecnologia, fabricante, segmento, sectores);
        } else if (tipo == 2) {
            int banda = consola.leerEntero("Banda en MHz", 400, 6000);
            nuevo = new Celda(nombre, tecnologia, fabricante, segmento, banda);
        } else {
            double altura = consola.leerDecimalPositivo("Altura en metros");
            nuevo = new Antena(nombre, tecnologia, fabricante, segmento, altura);
        }

        // Se piden los valores vigentes de los parametros con estandar en el segmento.
        for (Parametro parametro : repositorio.parametrosDe(fabricante)) {
            if (repositorio.buscarEstandar(parametro, segmento) != null) {
                cargarValorVigente(nuevo, parametro);
            }
        }
        repositorio.agregarElemento(nuevo);
        consola.mostrar("Elemento registrado con el id " + nuevo.getId() + ":");
        consola.mostrar("  " + nuevo.describir());
    }

    private void cargarValorVigente(ElementoDeRed elemento, Parametro parametro) {
        boolean cargado = false;
        while (!cargado) {
            String valor = consola.leerTexto("Valor vigente de " + parametro.getNombre()
                    + " [" + parametro.getRangoPermitido() + "]");
            try {
                elemento.registrarValorVigente(parametro, valor);
                cargado = true;
            } catch (ValorInvalidoException e) {
                consola.mostrar("  " + e.getMessage() + " Intente de nuevo.");
            }
        }
    }

    // ----- Opcion 3 -------------------------------------------------------

    private void buscarElemento() {
        consola.mostrarTitulo("BUSCAR ELEMENTO DE RED");
        String nombre = consola.leerTexto("Nombre o parte del nombre");
        ElementoDeRed[] ordenados = elementosOrdenadosPorNombre();
        Buscador buscador = new Buscador();

        int posicion = buscador.busquedaBinaria(ordenados, nombre);
        if (posicion >= 0) {
            consola.mostrar("Encontrado por busqueda binaria en " + buscador.getComparaciones()
                    + " comparaciones (sobre " + ordenados.length + " elementos):");
            consola.mostrar("  " + ordenados[posicion].describir());
            consola.mostrar("  vigentes: " + textoDeVigentes(ordenados[posicion]));
            return;
        }

        consola.mostrar("No hay un elemento con ese nombre exacto ("
                + buscador.getComparaciones() + " comparaciones de busqueda binaria).");
        ListaEnlazada<ElementoDeRed> parecidos =
                buscador.busquedaSecuencial(repositorio.getElementos(), nombre);
        if (parecidos.estaVacia()) {
            consola.mostrar("Tampoco hay nombres que contengan '" + nombre + "'.");
        } else {
            consola.mostrar("Nombres que contienen '" + nombre + "' (busqueda secuencial, "
                    + buscador.getComparaciones() + " comparaciones):");
            for (ElementoDeRed elemento : parecidos) {
                consola.mostrar("  " + elemento.describir());
            }
        }
    }

    // ----- Opcion 4 -------------------------------------------------------

    private void verCatalogo() {
        consola.mostrarTitulo("CATALOGO DE PARAMETROS (tipo de dato y rango permitido)");
        for (Parametro parametro : repositorio.getParametros()) {
            // getTipoDato() y getRangoPermitido() se resuelven segun la subclase.
            consola.mostrar(String.format("%-43s %-10s %s",
                    parametro, parametro.getTipoDato(), parametro.getRangoPermitido()));
        }
        consola.mostrarTitulo("VALORES ESTANDAR POR SEGMENTO");
        for (ValorEstandar estandar : repositorio.getEstandares()) {
            consola.mostrar(String.format("%-26s %-43s %-6s %-9s %s",
                    estandar.getSegmento().getNombre(), estandar.getParametro(),
                    estandar.getOperador(), estandar.obtenerEstandar(),
                    estandar.describirCondiciones()));
        }
        consola.mostrarTitulo("EXCEPCIONES REGISTRADAS");
        if (repositorio.getExcepciones().estaVacia()) {
            consola.mostrar("(ninguna)");
        }
        for (Excepcion excepcion : repositorio.getExcepciones()) {
            consola.mostrar(excepcion.toString());
            consola.mostrar("  motivo: " + excepcion.getMotivo());
        }
    }

    // ----- Opcion 5 -------------------------------------------------------

    private void iniciarAuditoria() throws NeipaException {
        consola.mostrarTitulo("EJECUTAR AUDITORIA");
        Segmento segmento = seleccionar("Segmento a auditar", repositorio.getSegmentos());
        ListaEnlazada<Auditoria> resultados = controladorAuditoria.ejecutarAuditoria(segmento);

        consola.mostrarTitulo("RESULTADO - " + segmento.getNombre());
        consola.mostrarBloque(new ReporteTecnico().generar(resultados));
        consola.mostrar("");
        consola.mostrar("Elementos auditados: " + controladorAuditoria.getElementosAuditados()
                + " - con desvio: " + controladorAuditoria.getElementosConDesvio()
                + String.format(Locale.US, " (%.1f%%)", controladorAuditoria.porcentajeDesvio()));
        if (controladorAuditoria.superaUmbral()) {
            consola.mostrar(String.format(Locale.US, "Atencion: se supera el umbral de %.0f%%.",
                    ControladorAuditoria.UMBRAL_CAMBIO_MASIVO_PCT));
            consola.mostrar("La correccion es un cambio masivo y requiere autorizacion.");
        }
        for (String error : controladorAuditoria.getErroresDeExtraccion()) {
            consola.mostrar("Error de extraccion: " + error);
        }
        if (!controladorAuditoria.getErroresDeExtraccion().estaVacia()) {
            consola.mostrar("Los elementos con error no se incluyen en el reporte.");
        }
    }

    // ----- Opciones 6 y 7 -------------------------------------------------

    /** Recibe cualquier reporte: el metodo generar() se resuelve segun el objeto. */
    private void mostrarReporte(Reporte reporte) {
        consola.mostrarTitulo(reporte.getTitulo());
        consola.mostrarBloque(reporte.generar(repositorio.getHistorial()));
    }

    // ----- Opcion 8 -------------------------------------------------------

    private void registrarExcepcion() {
        consola.mostrarTitulo("REGISTRAR EXCEPCION");
        consola.mostrar("Alcance: 1. Elemento   2. Segmento   3. Parametro   4. Patron de nombre");
        int alcance = consola.leerEntero("Alcance", 1, 4);

        ElementoDeRed elemento = null;
        Segmento segmento = null;
        Parametro parametro = null;
        if (alcance == 1) {
            elemento = seleccionar("Elemento", repositorio.getElementos());
            parametro = seleccionar("Parametro", repositorio.parametrosDe(elemento.getFabricante()));
        } else if (alcance == 2) {
            segmento = seleccionar("Segmento", repositorio.getSegmentos());
        } else if (alcance == 3) {
            parametro = seleccionar("Parametro", repositorio.getParametros());
        }
        String patron = null;
        if (alcance == 4) {
            patron = consola.leerTexto("Patron (* = cualquier texto, ? = un caracter)");
            if (!consola.confirmar("Exceptuar todos los parametros?")) {
                parametro = seleccionar("Parametro", repositorio.getParametros());
            }
        }
        String motivo = consola.leerTexto("Motivo");
        int dias = consola.leerEntero("Dias de vigencia", 1, 365);
        LocalDate desde = LocalDate.now();
        LocalDate hasta = desde.plusDays(dias);

        Excepcion excepcion;
        if (alcance == 1) {
            excepcion = Excepcion.porElemento(elemento, parametro, motivo, desde, hasta);
        } else if (alcance == 2) {
            excepcion = Excepcion.porSegmento(segmento, motivo, desde, hasta);
        } else if (alcance == 3) {
            excepcion = Excepcion.porParametro(parametro, motivo, desde, hasta);
        } else {
            excepcion = Excepcion.porPatron(patron, parametro, motivo, desde, hasta);
        }
        repositorio.agregarExcepcion(excepcion);
        consola.mostrar("Excepcion registrada:");
        consola.mostrar("  " + excepcion);
        consola.mostrar("  motivo: " + excepcion.getMotivo());
    }

    // ----- Opcion 9 -------------------------------------------------------

    private void aplicarCorreccion() throws NeipaException {
        consola.mostrarTitulo("APLICAR CORRECCION");
        ListaEnlazada<Auditoria> desvios = controladorAuditoria.desviosDeUltimaAuditoria();
        if (desvios.estaVacia()) {
            throw new NeipaException("La ultima auditoria no tiene desvios. "
                    + "Ejecute primero una auditoria (opcion 5).");
        }
        Auditoria desvio = seleccionar("Desvio a corregir", desvios);
        if (controladorAuditoria.superaUmbral()
                && !consola.confirmar("Cambio masivo: requiere autorizacion del Gerente / Jefe. "
                        + "Autoriza la correccion?")) {
            consola.mostrar("Correccion no autorizada. No se modifico la configuracion.");
            return;
        }
        CambioConfiguracion cambio = controladorRollback.aplicarCorreccion(desvio);
        consola.mostrar("Correccion aplicada: " + cambio);
        consola.mostrar("Vuelva a ejecutar la auditoria para verificar el resultado.");
    }

    // ----- Opcion 10 ------------------------------------------------------

    private void deshacerCorreccion() throws NeipaException {
        consola.mostrarTitulo("ROLLBACK");
        CambioConfiguracion revertido = controladorRollback.ejecutarRollback();
        consola.mostrar("Se deshizo la correccion: " + revertido);
        consola.mostrar("El parametro volvio a " + revertido.getValorAnterior()
                + ". Correcciones que quedan por deshacer: "
                + controladorRollback.cantidadDeCambios() + ".");
    }

    // ----- Opcion 11 ------------------------------------------------------

    private void cambiarDisponibilidad() {
        consola.mostrarTitulo("PLATAFORMAS DE FABRICANTES");
        Fabricante fabricante = seleccionar("Fabricante", repositorio.getFabricantes());
        fabricante.setPlataformaDisponible(!fabricante.isPlataformaDisponible());
        consola.mostrar("La plataforma " + fabricante.getTipoOss() + " de " + fabricante.getNombre()
                + " ahora esta " + (fabricante.isPlataformaDisponible()
                        ? "DISPONIBLE." : "FUERA DE SERVICIO."));
    }

    // ----- Opcion 12 ------------------------------------------------------

    /**
     * Genera el plan de reconfiguracion de los desvios de la ultima auditoria
     * en RAML 2.0 y, con los mismos objetos, el backup con los valores
     * vigentes para poder hacer rollback. Los guarda en la carpeta "salida".
     */
    private void generarReconfiguracion() throws NeipaException {
        consola.mostrarTitulo("RECONFIGURACION (RAML 2.0)");
        ListaEnlazada<Auditoria> desvios = controladorAuditoria.desviosDeUltimaAuditoria();
        if (desvios.estaVacia()) {
            throw new NeipaException("La ultima auditoria no tiene desvios. "
                    + "Ejecute primero una auditoria (opcion 5).");
        }
        int incluidos = 0;
        for (Auditoria desvio : desvios) {
            if (GeneradorRaml.esReconfigurable(desvio)) {
                incluidos++;
            } else {
                consola.mostrar("  Pendiente (" + desvio.getElementoDeRed().getFabricante().getTipoOss()
                        + " usa otro formato): " + desvio);
            }
        }
        if (incluidos == 0) {
            consola.mostrar("Ningun desvio es de un elemento gestionado por NetAct: no se genera XML.");
            return;
        }
        LocalDateTime ahora = LocalDateTime.now();
        String segmento = controladorAuditoria.getSegmentoActual().getNombre();
        GeneradorRaml plan = new GeneradorRaml(GeneradorRaml.Tipo.PLAN, segmento, ahora);
        GeneradorRaml backup = new GeneradorRaml(GeneradorRaml.Tipo.BACKUP, segmento, ahora);
        String xmlPlan = plan.generar(desvios);
        String xmlBackup = backup.generar(desvios);

        consola.mostrar(plan.getTitulo() + " - " + incluidos + " parametros:");
        consola.mostrarBloque(xmlPlan.replace("\r", "").replace("\t", "    "));
        try {
            Path carpeta = Paths.get("salida");
            Files.createDirectories(carpeta);
            Files.write(carpeta.resolve(plan.nombreDeArchivo()), xmlPlan.getBytes(StandardCharsets.UTF_8));
            Files.write(carpeta.resolve(backup.nombreDeArchivo()), xmlBackup.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new NeipaException("No se pudieron guardar los archivos XML: " + e.getMessage());
        }
        consola.mostrar("Plan guardado en   salida/" + plan.nombreDeArchivo());
        consola.mostrar("Backup guardado en salida/" + backup.nombreDeArchivo());
        consola.mostrar("  (mismos objetos con el valor vigente, para hacer rollback)");
    }

    // ----- Auxiliar -------------------------------------------------------

    /** Muestra una lista numerada y devuelve el objeto que elige el usuario. */
    // ----- Opcion 13 ------------------------------------------------------

    private void importarCm() throws NeipaException {
        consola.mostrarTitulo("IMPORTAR DATOS DE CM (NSN / NetAct)");
        Segmento segmento = seleccionar("Segmento de los elementos nuevos", repositorio.getSegmentos());
        consola.mostrar("Origen: un XML RAML exportado de NetAct (o un plan de la opcion 12),");
        consola.mostrar("una carpeta con un CSV por clase, o una URL jdbc: de una base con la");
        consola.mostrar("estructura del dump de NetAct.");
        consola.mostrar("Ejemplos: datos-cm/netact_demo.xml   datos-cm/csv   jdbc:sqlite:cm.dba");
        String origen = consola.leerTexto("Origen");
        String usuario = null;
        String contrasena = null;
        String minuscula = origen.trim().toLowerCase(Locale.ROOT);
        if (minuscula.startsWith("jdbc:") && !minuscula.startsWith("jdbc:sqlite:")) {
            usuario = consola.leerTexto("Usuario");
            contrasena = consola.leerTexto("Contrasena");
        }
        // Factory: el menu no sabe que subclase de FuenteCm recibe.
        FuenteCm fuente = FabricaFuentesCm.crear(origen, usuario, contrasena);
        consola.mostrar("Fuente: " + fuente.describir());

        Fabricante nsn = null;
        for (Fabricante f : repositorio.getFabricantes()) {
            if (f.getTipoOss().equalsIgnoreCase("NetAct")) {
                nsn = f;
            }
        }
        if (nsn == null) {
            throw new NeipaException("No hay un fabricante con plataforma NetAct cargado.");
        }
        ImportadorCm.Resumen resumen = new ImportadorCm(repositorio, nsn, segmento).importar(fuente);
        consola.mostrar("Objetos leidos: " + fuente.getObjetosLeidos());
        consola.mostrar("Resultado: " + resumen + ".");
        consola.mostrar("Para auditarlos use la opcion 5 con el segmento " + segmento.getNombre() + ".");
    }

    private <T> T seleccionar(String titulo, ListaEnlazada<T> opciones) {
        int numero = 1;
        for (T opcion : opciones) {
            consola.mostrar("  " + numero + ". " + opcion);
            numero++;
        }
        int elegido = consola.leerEntero(titulo, 1, opciones.tamanio());
        return opciones.obtener(elegido - 1);
    }
}
