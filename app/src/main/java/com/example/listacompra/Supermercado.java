package com.example.listacompra;

public class Supermercado {
    private final long id;
    private final String nombre;

    public Supermercado(long id, String nombre) {
        this.id = id;
        this.nombre = nombre;
    }

    public long getId() { return id; }
    public String getNombre() { return nombre; }

    @Override
    public String toString() { return nombre; }
}
