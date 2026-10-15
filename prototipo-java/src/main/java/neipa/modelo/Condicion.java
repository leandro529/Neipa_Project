package neipa.modelo;

/**
 * Condicion que tiene que cumplir un elemento para que se le aplique un
 * valor estandar. Un mismo parametro puede tener varios estandares en un
 * segmento: uno general y otros con condiciones (por ejemplo, segun el
 * nombre del elemento o segun el valor de otro parametro). Cada subclase
 * define como se evalua (polimorfismo).
 */
public abstract class Condicion {

    /** Indica si el elemento cumple la condicion. */
    public abstract boolean seCumple(ElementoDeRed elemento);

    /** Texto de la condicion, para mostrar en pantalla. */
    public abstract String describir();

    @Override
    public String toString() {
        return describir();
    }
}
