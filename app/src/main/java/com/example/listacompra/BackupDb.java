package com.example.listacompra;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.net.Uri;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public final class BackupDb {

    private static final byte[] CABECERA_SQLITE =
            "SQLite format 3\u0000".getBytes(StandardCharsets.US_ASCII);

    private BackupDb() { }

    /** Copia la base de datos actual al archivo que elija el usuario. */
    public static void exportar(Context ctx, DatabaseHelper db, Uri destino) throws IOException {
        db.close(); // vuelca todo al fichero; la base se reabre sola al usarla de nuevo
        File origen = ctx.getDatabasePath(DatabaseHelper.DB_NAME);
        try (InputStream in = new FileInputStream(origen);
             OutputStream out = ctx.getContentResolver().openOutputStream(destino)) {
            if (out == null) throw new IOException("No se pudo escribir en el destino");
            copiar(in, out);
        }
    }

    /** Sustituye los datos actuales por los del archivo elegido, tras validarlo. */
    public static void importar(Context ctx, DatabaseHelper db, Uri origen) throws IOException {
        File temporal = new File(ctx.getCacheDir(), "importado.db");
        try (InputStream in = ctx.getContentResolver().openInputStream(origen)) {
            if (in == null) throw new IOException("No se pudo leer el archivo");
            try (OutputStream out = new FileOutputStream(temporal)) {
                copiar(in, out);
            }
        }
        validar(temporal);

        db.close();
        File actual = ctx.getDatabasePath(DatabaseHelper.DB_NAME);
        File respaldo = new File(ctx.getCacheDir(), "copia_anterior.db");
        if (actual.exists()) copiarArchivo(actual, respaldo);

        try {
            borrarAsociados(actual);
            copiarArchivo(temporal, actual);
            db.getWritableDatabase(); // abre la base; migra si el archivo es de otra versión
        } catch (IOException | RuntimeException e) {
            db.close();
            borrarAsociados(actual);
            if (respaldo.exists()) copiarArchivo(respaldo, actual);
            throw new IOException("El archivo no es compatible: " + e.getMessage(), e);
        }
        temporal.delete();
    }

    private static void validar(File archivo) throws IOException {
        byte[] cabecera = new byte[CABECERA_SQLITE.length];
        try (InputStream in = new FileInputStream(archivo)) {
            int leidos = in.read(cabecera);
            if (leidos != cabecera.length || !Arrays.equals(cabecera, CABECERA_SQLITE)) {
                throw new IOException("El archivo no es una base de datos SQLite");
            }
        }

        try {
            SQLiteDatabase d = SQLiteDatabase.openDatabase(archivo.getPath(), null,
                    SQLiteDatabase.OPEN_READONLY);
            try (Cursor c = d.rawQuery("SELECT COUNT(*) FROM sqlite_master WHERE type = 'table' "
                    + "AND name IN ('productos', 'secciones')", null)) {
                c.moveToFirst();
                if (c.getInt(0) < 2) {
                    throw new IOException("La base de datos no tiene las tablas de la lista");
                }
            } finally {
                d.close();
            }
        } catch (SQLiteException e) {
            throw new IOException("No se pudo abrir la base de datos", e);
        }
    }

    private static void borrarAsociados(File base) {
        base.delete();
        new File(base.getPath() + "-journal").delete();
        new File(base.getPath() + "-wal").delete();
        new File(base.getPath() + "-shm").delete();
    }

    private static void copiarArchivo(File origen, File destino) throws IOException {
        try (InputStream in = new FileInputStream(origen);
             OutputStream out = new FileOutputStream(destino)) {
            copiar(in, out);
        }
    }

    private static void copiar(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[8192];
        int n;
        while ((n = in.read(buffer)) != -1) {
            out.write(buffer, 0, n);
        }
        out.flush();
    }
}
