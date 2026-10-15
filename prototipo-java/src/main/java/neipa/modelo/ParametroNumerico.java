package neipa.modelo;

import neipa.excepciones.ValorInvalidoException;

/** Parametro cuyo rango permitido es un intervalo numerico [minimo, maximo]. */
public class ParametroNumerico extends Parametro {

    private final double minimo;
    private final double maximo;
    private final double paso;

    public ParametroNumerico(Fabricante fabricante, String objetoGestionado, String nombre,
            String descripcion, String unidad, double minimo, double maximo, double paso,
            String valorDefault) {
        super(fabricante, objetoGestionado, nombre, descripcion, unidad, valorDefault);
        if (minimo > maximo) {
            throw new IllegalArgumentException("El minimo no puede ser mayor que el maximo.");
        }
        this.minimo = minimo;
        this.maximo = maximo;
        this.paso = paso;
    }

    @Override
    public boolean validarRango(String valor) {
        try {
            double numero = convertir(valor);
            return numero >= minimo && numero <= maximo;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    @Override
    public void validarTipo(String valor) throws ValorInvalidoException {
        try {
            convertir(valor);
        } catch (NumberFormatException e) {
            throw new ValorInvalidoException("El parametro " + getNombre()
                    + " es numerico y se recibio '" + valor + "'.");
        }
    }

    @Override
    public boolean sonEquivalentes(String valorA, String valorB) {
        try {
            return Math.abs(convertir(valorA) - convertir(valorB)) < 0.000001;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    @Override
    public String getRangoPermitido() {
        return formatear(minimo) + " a " + formatear(maximo) + " " + getUnidad();
    }

    @Override
    public String getTipoDato() {
        return "NUMERICO";
    }

    /** Convierte un texto a numero aceptando coma o punto decimal. */
    public static double convertir(String valor) {
        if (valor == null) {
            throw new NumberFormatException("valor nulo");
        }
        return Double.parseDouble(valor.trim().replace(',', '.'));
    }

    /** Muestra los enteros sin decimales (46 en lugar de 46.0). */
    public static String formatear(double numero) {
        if (numero == Math.rint(numero)) {
            return String.valueOf((long) numero);
        }
        return String.valueOf(numero);
    }

    public double getMinimo() {
        return minimo;
    }

    public double getMaximo() {
        return maximo;
    }

    public double getPaso() {
        return paso;
    }
}
