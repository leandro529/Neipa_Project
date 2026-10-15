package neipa.modelo;

import neipa.excepciones.ValorInvalidoException;

/** Parametro cuyo rango permitido es una lista cerrada de valores. */
public class ParametroEnumerado extends Parametro {

    private final String[] valoresPermitidos;

    public ParametroEnumerado(Fabricante fabricante, String objetoGestionado, String nombre,
            String descripcion, String[] valoresPermitidos, String valorDefault) {
        super(fabricante, objetoGestionado, nombre, descripcion, "", valorDefault);
        if (valoresPermitidos == null || valoresPermitidos.length == 0) {
            throw new IllegalArgumentException("Se necesita al menos un valor permitido.");
        }
        // Copia defensiva: el arreglo interno no queda expuesto a quien lo creo.
        this.valoresPermitidos = new String[valoresPermitidos.length];
        for (int i = 0; i < valoresPermitidos.length; i++) {
            this.valoresPermitidos[i] = valoresPermitidos[i];
        }
    }

    @Override
    public boolean validarRango(String valor) {
        if (valor == null) {
            return false;
        }
        for (int i = 0; i < valoresPermitidos.length; i++) {
            if (valoresPermitidos[i].equalsIgnoreCase(valor.trim())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void validarTipo(String valor) throws ValorInvalidoException {
        if (valor == null || valor.trim().isEmpty()) {
            throw new ValorInvalidoException("El parametro " + getNombre()
                    + " necesita un valor de texto.");
        }
    }

    /** Guarda el valor con la escritura del catalogo (unlocked pasa a UNLOCKED). */
    @Override
    public String normalizar(String valor) {
        for (int i = 0; i < valoresPermitidos.length; i++) {
            if (valoresPermitidos[i].equalsIgnoreCase(valor.trim())) {
                return valoresPermitidos[i];
            }
        }
        return valor.trim();
    }

    @Override
    public boolean sonEquivalentes(String valorA, String valorB) {
        return valorA != null && valorB != null && valorA.trim().equalsIgnoreCase(valorB.trim());
    }

    @Override
    public String getRangoPermitido() {
        StringBuilder texto = new StringBuilder();
        for (int i = 0; i < valoresPermitidos.length; i++) {
            if (i > 0) {
                texto.append(" | ");
            }
            texto.append(valoresPermitidos[i]);
        }
        return texto.toString();
    }

    @Override
    public String getTipoDato() {
        return "ENUMERADO";
    }
}
