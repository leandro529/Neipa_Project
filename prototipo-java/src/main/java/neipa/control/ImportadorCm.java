package neipa.control;

import neipa.datos.RepositorioEnMemoria;
import neipa.estructuras.ListaEnlazada;
import neipa.excepciones.ExtraccionException;
import neipa.excepciones.NeipaException;
import neipa.excepciones.ValorInvalidoException;
import neipa.fuentes.FuenteCm;
import neipa.fuentes.ObjetoCm;
import neipa.modelo.Celda;
import neipa.modelo.ElementoDeRed;
import neipa.modelo.Fabricante;
import neipa.modelo.Parametro;
import neipa.modelo.Segmento;
import neipa.modelo.Sitio;

/**
 * Importa los datos de CM de una red NSN/Nokia desde cualquier FuenteCm.
 *
 * - Cada LNBTS es un Sitio y cada LNCEL una Celda del segmento elegido (si ya
 *   existe un elemento con ese distName, solo se actualizan sus valores).
 * - De cada clase del catalogo del fabricante (LNCEL, CADPR...) se cargan
 *   los valores vigentes de los parametros que NEIPA audita. Un objeto que no
 *   es un elemento (por ejemplo CADPR-1) aporta sus valores al elemento del
 *   que depende segun su distName.
 *
 * No sabe de donde vienen los datos: trabaja contra la clase abstracta
 * FuenteCm, asi que sirve igual para un XML RAML, CSV o una base de datos.
 */
public class ImportadorCm {

    /** Totales de una importacion. */
    public static class Resumen {
        private int sitiosNuevos;
        private int celdasNuevas;
        private int valoresCargados;
        private int valoresRechazados;
        private int objetosSinElemento;

        public int getSitiosNuevos() {
            return sitiosNuevos;
        }

        public int getCeldasNuevas() {
            return celdasNuevas;
        }

        public int getValoresCargados() {
            return valoresCargados;
        }

        public int getValoresRechazados() {
            return valoresRechazados;
        }

        public int getObjetosSinElemento() {
            return objetosSinElemento;
        }

        @Override
        public String toString() {
            return sitiosNuevos + " sitios y " + celdasNuevas + " celdas nuevos, " + valoresCargados
                    + " valores vigentes cargados" + (valoresRechazados > 0 ? ", " + valoresRechazados
                    + " rechazados" : "") + (objetosSinElemento > 0 ? ", " + objetosSinElemento
                    + " objetos sin elemento" : "");
        }
    }

    private final RepositorioEnMemoria repositorio;
    private final Fabricante fabricante;
    private final Segmento segmento;

    public ImportadorCm(RepositorioEnMemoria repositorio, Fabricante fabricante, Segmento segmento) {
        if (repositorio == null || fabricante == null || segmento == null) {
            throw new IllegalArgumentException("El importador necesita repositorio, fabricante y segmento.");
        }
        this.repositorio = repositorio;
        this.fabricante = fabricante;
        this.segmento = segmento;
    }

    public Resumen importar(FuenteCm fuente) throws ExtraccionException {
        Resumen resumen = new Resumen();
        try {
            ListaEnlazada<ObjetoCm> sitios = fuente.leerObjetos("LNBTS");
            ListaEnlazada<ObjetoCm> celdas = fuente.leerObjetos("LNCEL");
            for (ObjetoCm lnbts : sitios) {
                if (buscarPorDistName(lnbts.getDistName()) == null && lnbts.getNombre() != null) {
                    int sectores = 0;
                    for (ObjetoCm lncel : celdas) {
                        if (lncel.dependeDe(lnbts.getDistName())) {
                            sectores++;
                        }
                    }
                    Sitio sitio = new Sitio(lnbts.getNombre(), "4G", fabricante, segmento, Math.max(1, sectores));
                    if (agregar(sitio, lnbts)) {
                        resumen.sitiosNuevos++;
                    }
                }
            }
            for (ObjetoCm lncel : celdas) {
                if (buscarPorDistName(lncel.getDistName()) == null && lncel.getNombre() != null) {
                    Celda celda = new Celda(lncel.getNombre(), "4G", fabricante, segmento, banda(lncel));
                    if (agregar(celda, lncel)) {
                        resumen.celdasNuevas++;
                    }
                }
            }
            // Valores vigentes: una lectura por clase del catalogo del fabricante.
            ListaEnlazada<String> clases = new ListaEnlazada<String>();
            for (Parametro parametro : repositorio.parametrosDe(fabricante)) {
                String clase = parametro.getObjetoGestionado().toUpperCase();
                if (!contiene(clases, clase)) {
                    clases.agregarAlFinal(clase);
                }
            }
            for (String clase : clases) {
                ListaEnlazada<ObjetoCm> objetos = clase.equals("LNCEL") ? celdas
                        : clase.equals("LNBTS") ? sitios : fuente.leerObjetos(clase);
                for (ObjetoCm objeto : objetos) {
                    cargarValores(objeto, resumen);
                }
            }
        } finally {
            fuente.cerrar();
        }
        return resumen;
    }

    private void cargarValores(ObjetoCm objeto, Resumen resumen) {
        ElementoDeRed elemento = elementoDe(objeto);
        if (elemento == null) {
            resumen.objetosSinElemento++;
            return;
        }
        for (Parametro parametro : repositorio.parametrosDe(fabricante)) {
            if (!parametro.getObjetoGestionado().equalsIgnoreCase(objeto.getClase())) {
                continue;
            }
            String valor = objeto.getValor(parametro.getNombre());
            if (valor == null) {
                continue;
            }
            try {
                elemento.registrarValorVigente(parametro, valor);
                resumen.valoresCargados++;
            } catch (ValorInvalidoException e) {
                resumen.valoresRechazados++;
            }
        }
    }

    /** El elemento con ese distName o, si no hay, el mas cercano del que depende. */
    private ElementoDeRed elementoDe(ObjetoCm objeto) {
        ElementoDeRed elegido = null;
        for (ElementoDeRed e : repositorio.getElementos()) {
            if (e.getFabricante() == fabricante && objeto.dependeDe(e.getDistName())
                    && (elegido == null || e.getDistName().length() > elegido.getDistName().length())) {
                elegido = e;
            }
        }
        return elegido;
    }

    private ElementoDeRed buscarPorDistName(String distName) {
        for (ElementoDeRed e : repositorio.getElementos()) {
            if (e.getDistName().equals(distName)) {
                return e;
            }
        }
        return null;
    }

    private boolean agregar(ElementoDeRed elemento, ObjetoCm objeto) {
        elemento.asignarDistName(objeto.getDistName());
        try {
            repositorio.agregarElemento(elemento);
            return true;
        } catch (NeipaException e) {
            return false; // nombre repetido: se ignora el objeto
        }
    }

    private static int banda(ObjetoCm lncel) {
        String valor = lncel.getValor("bandaMHz");
        try {
            return valor == null ? 700 : Integer.parseInt(valor);
        } catch (NumberFormatException e) {
            return 700;
        }
    }

    private static boolean contiene(ListaEnlazada<String> lista, String valor) {
        for (String x : lista) {
            if (x.equals(valor)) {
                return true;
            }
        }
        return false;
    }
}
