package neipa.modelo;

/**
 * Se cumple si el valor vigente de OTRO parametro del mismo elemento esta
 * entre un minimo y un maximo. Ejemplo: "si pMax esta entre 40 y 46, el
 * a3Offset tiene que ser -3". Si el elemento no tiene ese parametro, o el
 * valor no es numerico, la condicion no se cumple.
 */
public class CondicionPorParametro extends Condicion {

    private final Parametro parametro;
    private final double minimo;
    private final double maximo;

    public CondicionPorParametro(Parametro parametro, double minimo, double maximo) {
        if (parametro == null) {
            throw new IllegalArgumentException("La condicion necesita el parametro a evaluar.");
        }
        if (minimo > maximo) {
            throw new IllegalArgumentException("El minimo no puede ser mayor que el maximo.");
        }
        this.parametro = parametro;
        this.minimo = minimo;
        this.maximo = maximo;
    }

    @Override
    public boolean seCumple(ElementoDeRed elemento) {
        ValorVigente vigente = elemento.buscarValorVigente(parametro);
        if (vigente == null) {
            return false;
        }
        try {
            double valor = ParametroNumerico.convertir(vigente.obtenerValor());
            return valor >= minimo && valor <= maximo;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    @Override
    public String describir() {
        if (minimo == maximo) {
            return parametro.getNombre() + " = " + ParametroNumerico.formatear(minimo);
        }
        return parametro.getNombre() + " entre " + ParametroNumerico.formatear(minimo)
                + " y " + ParametroNumerico.formatear(maximo);
    }
}
