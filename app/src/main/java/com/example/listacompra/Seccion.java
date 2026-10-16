package com.example.listacompra;

public class Seccion {
    private long id;
    private String seccion;
    private String supermercado;

    public Seccion(long id, String seccion, String supermercado) {
        this.id = id;
        this.seccion = seccion;
        this.supermercado = supermercado;
    }

    public long getId() { return id; }
    public String getSeccion() { return seccion; }
    public String getSupermercado() { return supermercado; }

    /** Texto que se muestra en los Spinner y listas. */
    @Override
    public String toString() {
        if (supermercado == null || supermercado.isEmpty()) return seccion;
        return seccion + " · " + supermercado;
    }
}
