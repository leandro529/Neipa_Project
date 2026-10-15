package neipa.fuentes;

import java.io.File;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import neipa.estructuras.ListaEnlazada;
import neipa.excepciones.ExtraccionException;

/**
 * Fuente de CM en formato RAML 2.0, el XML con el que NetAct exporta la
 * configuracion (y el mismo formato del plan y del backup que genera NEIPA):
 *
 *   &lt;managedObject class="LNCEL" distName="PLMN-PLMN/MRBTS-101/LNBTS-101/LNCEL-1"&gt;
 *     &lt;p name="pMax"&gt;43&lt;/p&gt;
 *   &lt;/managedObject&gt;
 *
 * El archivo se lee una sola vez (en abrir) y despues se filtra por clase.
 */
public class FuenteCmRaml extends FuenteCm {

    private final ListaEnlazada<ObjetoCm> objetos =
            new ListaEnlazada<ObjetoCm>();

    public FuenteCmRaml(String archivo) {
        super(archivo);
    }

    @Override
    public String getTipo() {
        return "XML RAML 2.0 (NetAct)";
    }

    @Override
    protected void abrir() throws ExtraccionException {
        File archivo = new File(getOrigen());
        if (!archivo.isFile()) {
            throw new ExtraccionException("no existe el archivo " + archivo.getPath());
        }
        try {
            DocumentBuilderFactory fabrica = DocumentBuilderFactory.newInstance();
            // Un XML de afuera no puede pedir archivos ni URLs (DOCTYPE / entidades externas).
            fabrica.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            fabrica.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            fabrica.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            DocumentBuilder lector = fabrica.newDocumentBuilder();
            Document documento = lector.parse(archivo);
            NodeList managedObjects = documento.getElementsByTagName("managedObject");
            for (int i = 0; i < managedObjects.getLength(); i++) {
                Element mo = (Element) managedObjects.item(i);
                ObjetoCm objeto = nuevoObjeto(mo.getAttribute("class"), mo.getAttribute("distName"));
                NodeList hijos = mo.getChildNodes();
                for (int j = 0; j < hijos.getLength(); j++) {
                    Node hijo = hijos.item(j);
                    // Solo los <p> directos; las <list> (estructuras) no se usan en NEIPA.
                    if (hijo.getNodeType() == Node.ELEMENT_NODE && "p".equals(hijo.getNodeName())) {
                        agregarValor(objeto, ((Element) hijo).getAttribute("name"), hijo.getTextContent());
                    }
                }
                objetos.agregarAlFinal(objeto);
            }
        } catch (Exception e) {
            throw new ExtraccionException("no se pudo leer el XML " + getOrigen() + ": " + e.getMessage());
        }
    }

    @Override
    protected ListaEnlazada<ObjetoCm> leerFilas(String clase) {
        ListaEnlazada<ObjetoCm> deLaClase =
                new ListaEnlazada<ObjetoCm>();
        for (ObjetoCm objeto : objetos) {
            if (objeto.getClase().equals(clase)) {
                deLaClase.agregarAlFinal(objeto);
            }
        }
        return deLaClase;
    }
}
