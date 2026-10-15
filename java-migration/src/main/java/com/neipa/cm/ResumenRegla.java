package com.neipa.cm;

/** Totales de una regla en una corrida. */
public class ResumenRegla {

    private final long idValorEstandar;
    private final String clase;
    private final String parametro;
    private long conformes;
    private long desvios;
    private long exceptuados;

    public ResumenRegla(long idValorEstandar, String clase, String parametro) {
        this.idValorEstandar = idValorEstandar;
        this.clase = clase;
        this.parametro = parametro;
    }

    public ResumenRegla(long idValorEstandar, String clase, String parametro,
                        long conformes, long desvios, long exceptuados) {
        this(idValorEstandar, clase, parametro);
        this.conformes = conformes;
        this.desvios = desvios;
        this.exceptuados = exceptuados;
    }

    public void sumarConforme() {
        conformes++;
    }

    public void sumarDesvio() {
        desvios++;
    }

    public void sumarExceptuado() {
        exceptuados++;
    }

    public long getIdValorEstandar() {
        return idValorEstandar;
    }

    public String getClase() {
        return clase;
    }

    public String getParametro() {
        return parametro;
    }

    public long getConformes() {
        return conformes;
    }

    public long getDesvios() {
        return desvios;
    }

    public long getExceptuados() {
        return exceptuados;
    }
}
