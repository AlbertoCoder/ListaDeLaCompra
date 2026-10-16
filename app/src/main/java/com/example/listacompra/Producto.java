package com.example.listacompra;

public class Producto {
    private final long id;
    private final String nombre;
    private boolean comprar;
    private final String supermercado;   // puede ser null
    private final long idSeccion;        // 0 = sin sección
    private final String seccion;        // puede ser null
    private final int colorInicio;
    private final int colorFin;
    private final int colorFuente;

    public Producto(long id, String nombre, boolean comprar, String supermercado,
                    long idSeccion, String seccion,
                    int colorInicio, int colorFin, int colorFuente) {
        this.id = id;
        this.nombre = nombre;
        this.comprar = comprar;
        this.supermercado = supermercado;
        this.idSeccion = idSeccion;
        this.seccion = seccion;
        this.colorInicio = colorInicio;
        this.colorFin = colorFin;
        this.colorFuente = colorFuente;
    }

    public long getId() { return id; }
    public String getNombre() { return nombre; }
    public boolean isComprar() { return comprar; }
    public void setComprar(boolean comprar) { this.comprar = comprar; }
    public String getSupermercado() { return supermercado; }
    public long getIdSeccion() { return idSeccion; }
    public String getSeccion() { return seccion; }
    public int getColorInicio() { return colorInicio; }
    public int getColorFin() { return colorFin; }
    public int getColorFuente() { return colorFuente; }
}
