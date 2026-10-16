package com.example.listacompra;

public class Producto {
    private long id;
    private String nombre;
    private boolean comprar;
    private long idSeccion;        // 0 = sin sección
    private String seccion;        // puede ser null
    private String supermercado;   // puede ser null

    public Producto(long id, String nombre, boolean comprar, long idSeccion,
                    String seccion, String supermercado) {
        this.id = id;
        this.nombre = nombre;
        this.comprar = comprar;
        this.idSeccion = idSeccion;
        this.seccion = seccion;
        this.supermercado = supermercado;
    }

    public long getId() { return id; }
    public String getNombre() { return nombre; }
    public boolean isComprar() { return comprar; }
    public void setComprar(boolean comprar) { this.comprar = comprar; }
    public long getIdSeccion() { return idSeccion; }
    public String getSeccion() { return seccion; }
    public String getSupermercado() { return supermercado; }
}
