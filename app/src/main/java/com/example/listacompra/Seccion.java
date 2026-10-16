package com.example.listacompra;

public class Seccion {
    private final long id;
    private final String nombre;
    private final int colorInicio;
    private final int colorFin;
    private final int colorFuente;

    public Seccion(long id, String nombre, int colorInicio, int colorFin, int colorFuente) {
        this.id = id;
        this.nombre = nombre;
        this.colorInicio = colorInicio;
        this.colorFin = colorFin;
        this.colorFuente = colorFuente;
    }

    public long getId() { return id; }
    public String getSeccion() { return nombre; }
    public int getColorInicio() { return colorInicio; }
    public int getColorFin() { return colorFin; }
    public int getColorFuente() { return colorFuente; }

    @Override
    public String toString() { return nombre; }
}
