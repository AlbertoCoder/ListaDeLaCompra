package com.example.listacompra;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Color;

import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "lista_compra.db";
    private static final int DB_VERSION = 2;

    public DatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.setForeignKeyConstraintsEnabled(true);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE secciones ("
                + "ID INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "seccion TEXT NOT NULL, "
                + "color_inicio TEXT NOT NULL, "
                + "color_fin TEXT NOT NULL, "
                + "color_fuente TEXT NOT NULL)");

        db.execSQL("CREATE TABLE productos ("
                + "ID INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "nombre_producto TEXT NOT NULL, "
                + "comprar INTEGER NOT NULL DEFAULT 0, "
                + "supermercado TEXT, "
                + "id_seccion INTEGER, "
                + "FOREIGN KEY (id_seccion) REFERENCES secciones(ID) ON DELETE SET NULL)");

        // Datos de ejemplo
        long fruteria = insertarSeccionEn(db, "Frutería",
                Color.parseColor("#C8E6C9"), Color.parseColor("#4CAF50"));
        long carniceria = insertarSeccionEn(db, "Carnicería",
                Color.parseColor("#FFCDD2"), Color.parseColor("#F44336"));
        long lacteos = insertarSeccionEn(db, "Lácteos",
                Color.parseColor("#BBDEFB"), Color.parseColor("#2196F3"));
        long panaderia = insertarSeccionEn(db, "Panadería",
                Color.parseColor("#FFF9C4"), Color.parseColor("#FF9800"));
        long pescaderia = insertarSeccionEn(db, "Pescadería",
                Color.parseColor("#00BCD4"), Color.parseColor("#009688"));

        insertarProductoEn(db, "Manzanas", false, "Mercadona", fruteria);
        insertarProductoEn(db, "Plátanos", true, "Mercadona", fruteria);
        insertarProductoEn(db, "Pechuga de pollo", false, "Mercadona", carniceria);
        insertarProductoEn(db, "Leche entera", true, "Carrefour", lacteos);
        insertarProductoEn(db, "Yogur natural", false, "Carrefour", lacteos);
        insertarProductoEn(db, "Barra de pan", false, "Dia", panaderia);
        insertarProductoEn(db, "Merluza", false, "Mercadona", pescaderia);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // El esquema cambia por completo respecto a la versión 1: se recrean las tablas.
        db.execSQL("DROP TABLE IF EXISTS productos");
        db.execSQL("DROP TABLE IF EXISTS secciones");
        onCreate(db);
    }

    // ===================== HELPERS =====================

    private ContentValues valoresSeccion(String nombre, int inicio, int fin) {
        ContentValues v = new ContentValues();
        v.put("seccion", nombre);
        v.put("color_inicio", ColorUtil.toHex(inicio));
        v.put("color_fin", ColorUtil.toHex(fin));
        // El color de la letra se calcula automáticamente (blanco o negro) y se guarda
        v.put("color_fuente", ColorUtil.toHex(ColorUtil.colorFuente(inicio, fin)));
        return v;
    }

    private ContentValues valoresProducto(String nombre, boolean comprar,
                                          String supermercado, long idSeccion) {
        ContentValues v = new ContentValues();
        v.put("nombre_producto", nombre);
        v.put("comprar", comprar ? 1 : 0);
        v.put("supermercado", normalizar(supermercado));
        if (idSeccion > 0) v.put("id_seccion", idSeccion);
        else v.putNull("id_seccion");
        return v;
    }

    private static String normalizar(String texto) {
        if (texto == null) return null;
        String t = texto.trim();
        return t.isEmpty() ? null : t;
    }

    private long insertarSeccionEn(SQLiteDatabase db, String nombre, int inicio, int fin) {
        return db.insert("secciones", null, valoresSeccion(nombre, inicio, fin));
    }

    private long insertarProductoEn(SQLiteDatabase db, String nombre, boolean comprar,
                                    String supermercado, long idSeccion) {
        return db.insert("productos", null, valoresProducto(nombre, comprar, supermercado, idSeccion));
    }

    // ===================== SECCIONES (CRUD) =====================

    public long insertarSeccion(String nombre, int colorInicio, int colorFin) {
        return getWritableDatabase().insert("secciones", null,
                valoresSeccion(nombre, colorInicio, colorFin));
    }

    public int actualizarSeccion(long id, String nombre, int colorInicio, int colorFin) {
        return getWritableDatabase().update("secciones",
                valoresSeccion(nombre, colorInicio, colorFin),
                "ID = ?", new String[]{String.valueOf(id)});
    }

    /** Los productos de esa sección quedan "sin sección" (ON DELETE SET NULL). */
    public int eliminarSeccion(long id) {
        return getWritableDatabase().delete("secciones", "ID = ?",
                new String[]{String.valueOf(id)});
    }

    public List<Seccion> getSecciones() {
        List<Seccion> lista = new ArrayList<>();
        try (Cursor c = getReadableDatabase().rawQuery(
                "SELECT ID, seccion, color_inicio, color_fin, color_fuente FROM secciones "
                        + "ORDER BY seccion COLLATE NOCASE", null)) {
            while (c.moveToNext()) {
                lista.add(new Seccion(
                        c.getLong(0),
                        c.getString(1),
                        ColorUtil.parse(c.getString(2), Color.WHITE),
                        ColorUtil.parse(c.getString(3), Color.WHITE),
                        ColorUtil.parse(c.getString(4), Color.BLACK)));
            }
        }
        return lista;
    }

    // ===================== PRODUCTOS (CRUD) =====================

    public long insertarProducto(String nombre, boolean comprar, String supermercado, long idSeccion) {
        return getWritableDatabase().insert("productos", null,
                valoresProducto(nombre, comprar, supermercado, idSeccion));
    }

    public int actualizarProducto(long id, String nombre, boolean comprar,
                                  String supermercado, long idSeccion) {
        return getWritableDatabase().update("productos",
                valoresProducto(nombre, comprar, supermercado, idSeccion),
                "ID = ?", new String[]{String.valueOf(id)});
    }

    public int setComprar(long id, boolean comprar) {
        ContentValues v = new ContentValues();
        v.put("comprar", comprar ? 1 : 0);
        return getWritableDatabase().update("productos", v, "ID = ?",
                new String[]{String.valueOf(id)});
    }

    /** true = todos por comprar; false = todos ya comprados. */
    public int marcarTodos(boolean comprar) {
        ContentValues v = new ContentValues();
        v.put("comprar", comprar ? 1 : 0);
        return getWritableDatabase().update("productos", v, null, null);
    }

    public int eliminarProducto(long id) {
        return getWritableDatabase().delete("productos", "ID = ?",
                new String[]{String.valueOf(id)});
    }

    /** Supermercados usados por algún producto (sin repetir, sin vacíos). */
    public List<String> getSupermercados() {
        List<String> lista = new ArrayList<>();
        try (Cursor c = getReadableDatabase().rawQuery(
                "SELECT DISTINCT supermercado FROM productos "
                        + "WHERE supermercado IS NOT NULL AND supermercado <> '' "
                        + "ORDER BY supermercado COLLATE NOCASE", null)) {
            while (c.moveToNext()) lista.add(c.getString(0));
        }
        return lista;
    }

    /**
     * @param texto        busca en nombre, supermercado y sección (vacío = sin filtro)
     * @param idSeccion    id de sección (&lt;= 0 = todas)
     * @param supermercado supermercado exacto (null = todos)
     * @param estado       0 = todos, 1 = por comprar, 2 = comprados
     */
    public List<Producto> getProductos(String texto, long idSeccion, String supermercado, int estado) {
        StringBuilder sql = new StringBuilder(
                "SELECT p.ID, p.nombre_producto, p.comprar, p.supermercado, p.id_seccion, "
                        + "s.seccion, s.color_inicio, s.color_fin, s.color_fuente "
                        + "FROM productos p LEFT JOIN secciones s ON p.id_seccion = s.ID "
                        + "WHERE 1=1");
        List<String> args = new ArrayList<>();

        if (texto != null && !texto.isEmpty()) {
            sql.append(" AND (p.nombre_producto LIKE ? OR p.supermercado LIKE ? OR s.seccion LIKE ?)");
            String like = "%" + texto + "%";
            args.add(like);
            args.add(like);
            args.add(like);
        }
        if (idSeccion > 0) {
            sql.append(" AND p.id_seccion = ?");
            args.add(String.valueOf(idSeccion));
        }
        if (supermercado != null) {
            sql.append(" AND p.supermercado = ?");
            args.add(supermercado);
        }
        // estado 1 = por comprar (comprar = 1), estado 2 = ya comprados (comprar = 0)
        if (estado == 1) sql.append(" AND p.comprar = 1");
        else if (estado == 2) sql.append(" AND p.comprar = 0");

        // Lo que falta por comprar arriba; lo ya comprado abajo
        sql.append(" ORDER BY p.comprar DESC, p.nombre_producto COLLATE NOCASE ASC");

        List<Producto> lista = new ArrayList<>();
        try (Cursor c = getReadableDatabase().rawQuery(sql.toString(),
                args.toArray(new String[0]))) {
            while (c.moveToNext()) {
                lista.add(new Producto(
                        c.getLong(0),
                        c.getString(1),
                        c.getInt(2) == 1,
                        c.getString(3),
                        c.isNull(4) ? 0 : c.getLong(4),
                        c.getString(5),
                        ColorUtil.parse(c.getString(6), Color.WHITE),
                        ColorUtil.parse(c.getString(7), Color.WHITE),
                        ColorUtil.parse(c.getString(8), Color.BLACK)));
            }
        }
        return lista;
    }
}
