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

    public static final String DB_NAME = "lista_compra.db";
    private static final int DB_VERSION = 3;

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
        crearTablaSupermercados(db, "supermercados");
        crearTablaSecciones(db);
        crearTablaProductos(db, "productos");

        // Datos de ejemplo
        long mercadona = insertarSupermercadoEn(db, "Mercadona");
        long carrefour = insertarSupermercadoEn(db, "Carrefour");
        long dia = insertarSupermercadoEn(db, "Dia");

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

        insertarProductoEn(db, "Manzanas", false, mercadona, fruteria);
        insertarProductoEn(db, "Plátanos", true, mercadona, fruteria);
        insertarProductoEn(db, "Pechuga de pollo", false, mercadona, carniceria);
        insertarProductoEn(db, "Leche entera", true, carrefour, lacteos);
        insertarProductoEn(db, "Yogur natural", false, carrefour, lacteos);
        insertarProductoEn(db, "Barra de pan", false, dia, panaderia);
        insertarProductoEn(db, "Merluza", false, mercadona, pescaderia);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            // La versión 1 tiene un esquema incompatible: se recrean las tablas.
            db.execSQL("DROP TABLE IF EXISTS productos");
            db.execSQL("DROP TABLE IF EXISTS secciones");
            db.execSQL("DROP TABLE IF EXISTS supermercados");
            onCreate(db);
            return;
        }
        if (oldVersion < 3) {
            migrarV2aV3(db);
        }
    }

    /** Pasa el texto de supermercado de productos a la tabla supermercados, conservando los datos. */
    private void migrarV2aV3(SQLiteDatabase db) {
        crearTablaSupermercados(db, "supermercados");
        db.execSQL("INSERT OR IGNORE INTO supermercados (supermercado) "
                + "SELECT DISTINCT TRIM(supermercado) FROM productos "
                + "WHERE supermercado IS NOT NULL AND TRIM(supermercado) <> ''");

        crearTablaProductos(db, "productos_nuevo");
        db.execSQL("INSERT INTO productos_nuevo (ID, nombre_producto, comprar, id_supermercado, id_seccion) "
                + "SELECT p.ID, p.nombre_producto, p.comprar, "
                + "(SELECT m.ID FROM supermercados m WHERE m.supermercado = TRIM(p.supermercado)), "
                + "p.id_seccion FROM productos p");
        db.execSQL("DROP TABLE productos");
        db.execSQL("ALTER TABLE productos_nuevo RENAME TO productos");
    }

    // ===================== CREACIÓN DE TABLAS =====================

    private static void crearTablaSupermercados(SQLiteDatabase db, String nombre) {
        db.execSQL("CREATE TABLE " + nombre + " ("
                + "ID INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "supermercado TEXT NOT NULL COLLATE NOCASE UNIQUE)");
    }

    private static void crearTablaSecciones(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE secciones ("
                + "ID INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "seccion TEXT NOT NULL, "
                + "color_inicio TEXT NOT NULL, "
                + "color_fin TEXT NOT NULL, "
                + "color_fuente TEXT NOT NULL)");
    }

    private static void crearTablaProductos(SQLiteDatabase db, String nombre) {
        db.execSQL("CREATE TABLE " + nombre + " ("
                + "ID INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "nombre_producto TEXT NOT NULL, "
                + "comprar INTEGER NOT NULL DEFAULT 0, "
                + "id_supermercado INTEGER, "
                + "id_seccion INTEGER, "
                + "FOREIGN KEY (id_supermercado) REFERENCES supermercados(ID) ON DELETE SET NULL, "
                + "FOREIGN KEY (id_seccion) REFERENCES secciones(ID) ON DELETE SET NULL)");
    }

    // ===================== HELPERS =====================

    private static String normalizar(String texto) {
        if (texto == null) return null;
        String t = texto.trim();
        return t.isEmpty() ? null : t;
    }

    private static ContentValues valoresSupermercado(String nombre) {
        ContentValues v = new ContentValues();
        v.put("supermercado", normalizar(nombre));
        return v;
    }

    private static ContentValues valoresSeccion(String nombre, int inicio, int fin) {
        ContentValues v = new ContentValues();
        v.put("seccion", nombre);
        v.put("color_inicio", ColorUtil.toHex(inicio));
        v.put("color_fin", ColorUtil.toHex(fin));
        // El color de la letra se calcula automáticamente (blanco o negro) y se guarda
        v.put("color_fuente", ColorUtil.toHex(ColorUtil.colorFuente(inicio, fin)));
        return v;
    }

    private static ContentValues valoresProducto(String nombre, boolean comprar,
                                                 long idSupermercado, long idSeccion) {
        ContentValues v = new ContentValues();
        v.put("nombre_producto", nombre);
        v.put("comprar", comprar ? 1 : 0);
        if (idSupermercado > 0) v.put("id_supermercado", idSupermercado);
        else v.putNull("id_supermercado");
        if (idSeccion > 0) v.put("id_seccion", idSeccion);
        else v.putNull("id_seccion");
        return v;
    }

    private long insertarSupermercadoEn(SQLiteDatabase db, String nombre) {
        return db.insert("supermercados", null, valoresSupermercado(nombre));
    }

    private long insertarSeccionEn(SQLiteDatabase db, String nombre, int inicio, int fin) {
        return db.insert("secciones", null, valoresSeccion(nombre, inicio, fin));
    }

    private long insertarProductoEn(SQLiteDatabase db, String nombre, boolean comprar,
                                    long idSupermercado, long idSeccion) {
        return db.insert("productos", null, valoresProducto(nombre, comprar, idSupermercado, idSeccion));
    }

    // ===================== SUPERMERCADOS (CRUD) =====================

    public List<Supermercado> getSupermercados() {
        List<Supermercado> lista = new ArrayList<>();
        try (Cursor c = getReadableDatabase().rawQuery(
                "SELECT ID, supermercado FROM supermercados "
                        + "ORDER BY supermercado COLLATE NOCASE", null)) {
            while (c.moveToNext()) {
                lista.add(new Supermercado(c.getLong(0), c.getString(1)));
            }
        }
        return lista;
    }

    /** true si ya existe otro supermercado (distinto de idExcluir) con ese nombre. */
    public boolean existeSupermercado(String nombre, long idExcluir) {
        try (Cursor c = getReadableDatabase().rawQuery(
                "SELECT COUNT(*) FROM supermercados WHERE supermercado = ? AND ID <> ?",
                new String[]{nombre, String.valueOf(idExcluir)})) {
            return c.moveToFirst() && c.getInt(0) > 0;
        }
    }

    public long insertarSupermercado(String nombre) {
        return getWritableDatabase().insert("supermercados", null, valoresSupermercado(nombre));
    }

    public int actualizarSupermercado(long id, String nombre) {
        return getWritableDatabase().update("supermercados", valoresSupermercado(nombre),
                "ID = ?", new String[]{String.valueOf(id)});
    }

    /** Los productos de ese supermercado quedan "sin supermercado" (ON DELETE SET NULL). */
    public int eliminarSupermercado(long id) {
        return getWritableDatabase().delete("supermercados", "ID = ?",
                new String[]{String.valueOf(id)});
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

    public long insertarProducto(String nombre, boolean comprar, long idSupermercado, long idSeccion) {
        return getWritableDatabase().insert("productos", null,
                valoresProducto(nombre, comprar, idSupermercado, idSeccion));
    }

    public int actualizarProducto(long id, String nombre, boolean comprar,
                                  long idSupermercado, long idSeccion) {
        return getWritableDatabase().update("productos",
                valoresProducto(nombre, comprar, idSupermercado, idSeccion),
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

    /**
     * @param texto        busca en nombre, supermercado y sección (vacío = sin filtro)
     * @param idSeccion    id de sección (&lt;= 0 = todas)
     * @param idSupermercado id de supermercado (&lt;= 0 = todos)
     * @param estado       0 = todos, 1 = por comprar, 2 = ya comprados
     */
    public List<Producto> getProductos(String texto, long idSeccion, long idSupermercado, int estado) {
        StringBuilder sql = new StringBuilder(
                "SELECT p.ID, p.nombre_producto, p.comprar, p.id_supermercado, m.supermercado, "
                        + "p.id_seccion, s.seccion, s.color_inicio, s.color_fin, s.color_fuente "
                        + "FROM productos p "
                        + "LEFT JOIN supermercados m ON p.id_supermercado = m.ID "
                        + "LEFT JOIN secciones s ON p.id_seccion = s.ID "
                        + "WHERE 1=1");
        List<String> args = new ArrayList<>();

        if (texto != null && !texto.isEmpty()) {
            sql.append(" AND (p.nombre_producto LIKE ? OR m.supermercado LIKE ? OR s.seccion LIKE ?)");
            String like = "%" + texto + "%";
            args.add(like);
            args.add(like);
            args.add(like);
        }
        if (idSeccion > 0) {
            sql.append(" AND p.id_seccion = ?");
            args.add(String.valueOf(idSeccion));
        }
        if (idSupermercado > 0) {
            sql.append(" AND p.id_supermercado = ?");
            args.add(String.valueOf(idSupermercado));
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
                        c.isNull(3) ? 0 : c.getLong(3),
                        c.getString(4),
                        c.isNull(5) ? 0 : c.getLong(5),
                        c.getString(6),
                        ColorUtil.parse(c.getString(7), Color.WHITE),
                        ColorUtil.parse(c.getString(8), Color.WHITE),
                        ColorUtil.parse(c.getString(9), Color.BLACK)));
            }
        }
        return lista;
    }
}
