package com.example.listacompra;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;

public final class ColorUtil {

    private ColorUtil() { }

    /** Convierte "#RRGGBB" a int; si no es válido devuelve porDefecto. */
    public static int parse(String hex, int porDefecto) {
        try {
            return Color.parseColor(hex);
        } catch (Exception e) {
            return porDefecto;
        }
    }

    public static String toHex(int color) {
        return String.format("#%06X", 0xFFFFFF & color);
    }

    /** Luminancia relativa según WCAG (0 = negro, 1 = blanco). */
    private static double luminancia(int color) {
        return 0.2126 * canal(Color.red(color))
             + 0.7152 * canal(Color.green(color))
             + 0.0722 * canal(Color.blue(color));
    }

    private static double canal(int valor) {
        double c = valor / 255.0;
        return c <= 0.03928 ? c / 12.92 : Math.pow((c + 0.055) / 1.055, 2.4);
    }

    private static double contraste(int a, int b) {
        double la = luminancia(a);
        double lb = luminancia(b);
        return (Math.max(la, lb) + 0.05) / (Math.min(la, lb) + 0.05);
    }

    /**
     * Devuelve blanco o negro, el que tenga mejor contraste con TODOS los fondos dados.
     * Con un degradado se pasan sus dos extremos, así la letra se lee en ambos.
     */
    public static int colorFuente(int... fondos) {
        double minBlanco = Double.MAX_VALUE;
        double minNegro = Double.MAX_VALUE;
        for (int f : fondos) {
            minBlanco = Math.min(minBlanco, contraste(Color.WHITE, f));
            minNegro = Math.min(minNegro, contraste(Color.BLACK, f));
        }
        return minBlanco >= minNegro ? Color.WHITE : Color.BLACK;
    }

    public static GradientDrawable degradado(int inicio, int fin, float radioPx) {
        GradientDrawable gd = new GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT, new int[]{inicio, fin});
        gd.setCornerRadius(radioPx);
        return gd;
    }
}
