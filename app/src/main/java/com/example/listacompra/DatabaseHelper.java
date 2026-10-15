package com.example.listacompra;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "lista_compra.db";
    private static final int DB_VERSION = 1;

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
                + "supermercado TEXT NOT NULL)");

        db.execSQL("CREATE TABLE productos ("
                + "ID INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "nombre_producto TEXT NOT NULL, "
                + "comprar INTEGER NOT NULL DEFAULT 0, "
                + "id_seccion INTEGER, "
                + "FOREIGN KEY (id_seccion) REFERENCES secciones(ID) ON DELETE SET NULL)");

        // Datos de ejemplo
        long fruteria = insertarSeccion(db, "Frutería", "Mercadona");
        long carniceria = insertarSeccion(db, "Carnicería", "Mercadona");
        long lacteos = insertarSeccion(db, "Lácteos", "Carrefour");
        long panaderia = insertarSeccion(db, "Panadería", "Dia");
        long pescaderia = insertarSeccion(db, "Pescadería", "Mercadona");

        insertarProducto(db, "Manzanas", false, fruteria);
        insertarProducto(db, "Plátanos", true, fruteria);
        insertarProducto(db, "Pechuga de pollo", false, carniceria);
        insertarProducto(db, "Leche entera", true, lacteos);
        insertarProducto(db, "Yogur natural", false, lacteos);
        insertarProducto(db, "Barra de pan", false, panaderia);
        insertarProducto(db, "Merluza", false, pescaderia);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Sin migraciones por ahora (versión 1).
    }

    // ===================== SECCIONES =====================

    private long insertarSeccion(SQLiteDatabase db, String seccion, String supermercado) {
        ContentValues v = new ContentValues();
        v.put("seccion", seccion);
        v.put("supermercado", supermercado);
        return db.insert("secciones", null, v);
    }

    public long insertarSeccion(String seccion, String supermercado) {
        return insertarSeccion(getWritableDatabase(), seccion, supermercado);
    }

    public int actualizarSeccion(long id, String seccion, String supermercado) {
        ContentValues v = new ContentValues();
        v.put("seccion", seccion);
        v.put("supermercado", supermercado);
        return getWritableDatabase().update("secciones", v, "ID = ?",
                new String[]{String.valueOf(id)});
    }

    /** Los productos de esa sección quedan "sin sección" (ON DELETE SET NULL). */
    public int eliminarSeccion(long id) {
        return getWritableDatabase().delete("secciones", "ID = ?",
                new String[]{String.valueOf(id)});
    }

    public List<Seccion> getSecciones() {
        List<Seccion> lista = new ArrayList<>();
        try (Cursor c = getReadableDatabase().rawQuery(
                "SELECT ID, seccion, supermercado FROM secciones "
                        + "ORDER BY supermercado COLLATE NOCASE, seccion COLLATE NOCASE", null)) {
            while (c.moveToNext()) {
                lista.add(new Seccion(c.getLong(0), c.getString(1), c.getString(2)));
            }
        }
        return lista;
    }

    // ===================== PRODUCTOS =====================

    private long insertarProducto(SQLiteDatabase db, String nombre, boolean comprar, long idSeccion) {
        ContentValues v = new ContentValues();
        v.put("nombre_producto", nombre);
        v.put("comprar", comprar ? 1 : 0);
        if (idSeccion > 0) v.put("id_seccion", idSeccion);
        else v.putNull("id_seccion");
        return db.insert("productos", null, v);
    }

    public long insertarProducto(String nombre, boolean comprar, long idSeccion) {
        return insertarProducto(getWritableDatabase(), nombre, comprar, idSeccion);
    }

    public int actualizarProducto(long id, String nombre, boolean comprar, long idSeccion) {
        ContentValues v = new ContentValues();
        v.put("nombre_producto", nombre);
        v.put("comprar", comprar ? 1 : 0);
        if (idSeccion > 0) v.put("id_seccion", idSeccion);
        else v.putNull("id_seccion");
        return getWritableDatabase().update("productos", v, "ID = ?",
                new String[]{String.valueOf(id)});
    }

    public int setComprar(long id, boolean comprar) {
        ContentValues v = new ContentValues();
        v.put("comprar", comprar ? 1 : 0);
        return getWritableDatabase().update("productos", v, "ID = ?",
                new String[]{String.valueOf(id)});
    }

    public int desmarcarTodos() {
        ContentValues v = new ContentValues();
        v.put("comprar", 0);
        return getWritableDatabase().update("productos", v, null, null);
    }

    public int eliminarProducto(long id) {
        return getWritableDatabase().delete("productos", "ID = ?",
                new String[]{String.valueOf(id)});
    }

    /**
     * @param texto     busca en nombre, sección y supermercado (vacío = sin filtro)
     * @param idSeccion id de sección a filtrar (&lt;= 0 = todas)
     * @param estado    0 = todos, 1 = por comprar, 2 = comprados
     */
    public List<Producto> getProductos(String texto, long idSeccion, int estado) {
        StringBuilder sql = new StringBuilder(
                "SELECT p.ID, p.nombre_producto, p.comprar, p.id_seccion, s.seccion, s.supermercado "
                        + "FROM productos p LEFT JOIN secciones s ON p.id_seccion = s.ID "
                        + "WHERE 1=1");
        List<String> args = new ArrayList<>();

        if (texto != null && !texto.isEmpty()) {
            sql.append(" AND (p.nombre_producto LIKE ? OR s.seccion LIKE ? OR s.supermercado LIKE ?)");
            String like = "%" + texto + "%";
            args.add(like);
            args.add(like);
            args.add(like);
        }
        if (idSeccion > 0) {
            sql.append(" AND p.id_seccion = ?");
            args.add(String.valueOf(idSeccion));
        }
        if (estado == 1) sql.append(" AND p.comprar = 0");
        else if (estado == 2) sql.append(" AND p.comprar = 1");

        sql.append(" ORDER BY p.comprar ASC, p.nombre_producto COLLATE NOCASE ASC");

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
                        c.getString(5)));
            }
        }
        return lista;
    }
}
