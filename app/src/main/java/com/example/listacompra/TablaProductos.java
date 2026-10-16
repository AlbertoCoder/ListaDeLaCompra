package com.example.listacompra;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;

/**
 * Dibuja la tabla de productos con filas y columnas de ancho fijo.
 * El ancho de cada columna se ajusta al texto más largo, para que cada texto
 * quede entero en una sola línea. La tabla se desplaza en horizontal.
 */
public class TablaProductos {

    public interface Listener {
        void onComprarCambiado(Producto p, boolean comprar);
        void onEditar(Producto p);
        void onEliminar(Producto p);
    }

    private static final int COLOR_PENDIENTE = Color.parseColor("#2E7D32");
    private static final int COLOR_TEXTO_PENDIENTE = Color.WHITE;
    private static final int COLOR_NORMAL = Color.WHITE;
    private static final int COLOR_TEXTO = Color.parseColor("#212121");
    private static final int COLOR_SIN_SECCION = Color.parseColor("#F5F5F5");
    private static final int COLOR_CABECERA = Color.parseColor("#E8F5E9");
    private static final int COLOR_CABECERA_TEXTO = Color.parseColor("#1B5E20");
    private static final int COLOR_DIVISOR = Color.parseColor("#DDDDDD");

    private static final int ALTO_CABECERA_DP = 48;
    private static final int ALTO_FILA_DP = 56;
    private static final int PADDING_TEXTO_DP = 24;     // 12 dp a cada lado
    private static final int MIN_ANCHO_TEXTO_DP = 96;
    private static final int ANCHO_COMPRAR_DP = 72;
    private static final int ANCHO_ELIMINAR_DP = 48;

    private final Context ctx;
    private final LinearLayout contenedor;
    private final Listener listener;
    private final float density;

    public TablaProductos(Context ctx, LinearLayout contenedor, Listener listener) {
        this.ctx = ctx;
        this.contenedor = contenedor;
        this.listener = listener;
        this.density = ctx.getResources().getDisplayMetrics().density;
    }

    public void mostrar(List<Producto> datos) {
        contenedor.removeAllViews();
        int[] anchos = calcularAnchos(datos);
        int total = 0;
        for (int a : anchos) total += a;

        contenedor.addView(crearCabecera(anchos, total));
        for (Producto p : datos) {
            contenedor.addView(crearFila(p, anchos, total));
            contenedor.addView(divisor(total));
        }
    }

    // ---------------- Anchos ----------------

    private int[] calcularAnchos(List<Producto> datos) {
        Paint cabecera = pintura(14, true);
        Paint cuerpo = pintura(15, false);
        Paint celda = pintura(13, false);
        Paint celdaNegrita = pintura(13, true);

        float nombre = cabecera.measureText(ctx.getString(R.string.col_producto));
        float super_ = cabecera.measureText(ctx.getString(R.string.col_supermercado));
        float secc = cabecera.measureText(ctx.getString(R.string.col_seccion));
        for (Producto p : datos) {
            nombre = Math.max(nombre, cuerpo.measureText(p.getNombre()));
            if (p.getSupermercado() != null) {
                super_ = Math.max(super_, celda.measureText(p.getSupermercado()));
            }
            if (p.getSeccion() != null) {
                secc = Math.max(secc, celdaNegrita.measureText(p.getSeccion()));
            }
        }

        int[] anchos = {
                dp(ANCHO_COMPRAR_DP),
                Math.max(dp(MIN_ANCHO_TEXTO_DP), Math.round(nombre) + dp(PADDING_TEXTO_DP)),
                Math.max(dp(MIN_ANCHO_TEXTO_DP), Math.round(super_) + dp(PADDING_TEXTO_DP)),
                Math.max(dp(MIN_ANCHO_TEXTO_DP), Math.round(secc) + dp(PADDING_TEXTO_DP)),
                dp(ANCHO_ELIMINAR_DP)
        };

        // Si las columnas no llenan la pantalla, la de producto se ensancha
        int total = 0;
        for (int a : anchos) total += a;
        int pantalla = ctx.getResources().getDisplayMetrics().widthPixels;
        if (total < pantalla) anchos[1] += pantalla - total;
        return anchos;
    }

    // ---------------- Cabecera y filas ----------------

    private View crearCabecera(int[] anchos, int total) {
        LinearLayout fila = new LinearLayout(ctx);
        fila.setOrientation(LinearLayout.HORIZONTAL);
        fila.setGravity(Gravity.CENTER_VERTICAL);
        fila.setBackgroundColor(COLOR_CABECERA);
        fila.setLayoutParams(new LinearLayout.LayoutParams(total, dp(ALTO_CABECERA_DP)));

        fila.addView(celda(ctx.getString(R.string.col_comprar), anchos[0], 14, COLOR_CABECERA_TEXTO, true));
        fila.addView(celda(ctx.getString(R.string.col_producto), anchos[1], 14, COLOR_CABECERA_TEXTO, true));
        fila.addView(celda(ctx.getString(R.string.col_supermercado), anchos[2], 14, COLOR_CABECERA_TEXTO, true));
        fila.addView(celda(ctx.getString(R.string.col_seccion), anchos[3], 14, COLOR_CABECERA_TEXTO, true));
        fila.addView(celda("", anchos[4], 14, COLOR_CABECERA_TEXTO, true));
        return fila;
    }

    private View crearFila(Producto p, int[] anchos, int total) {
        boolean pendiente = p.isComprar();
        int colorTexto = pendiente ? COLOR_TEXTO_PENDIENTE : COLOR_TEXTO;

        LinearLayout fila = new LinearLayout(ctx);
        fila.setOrientation(LinearLayout.HORIZONTAL);
        fila.setGravity(Gravity.CENTER_VERTICAL);
        fila.setLayoutParams(new LinearLayout.LayoutParams(total, dp(ALTO_FILA_DP)));
        fila.setBackgroundColor(pendiente ? COLOR_PENDIENTE : COLOR_NORMAL);

        // Casilla centrada en su columna
        CheckBox cb = new CheckBox(ctx);
        cb.setChecked(pendiente);

        TextView tvNombre = celda(p.getNombre(), anchos[1], 15, colorTexto, false);
        TextView tvSuper = celda(p.getSupermercado() == null ? "—" : p.getSupermercado(),
                anchos[2], 13, colorTexto, false);
        TextView tvSeccion = celda(p.getSeccion() == null ? "—" : p.getSeccion(),
                anchos[3], 13, COLOR_TEXTO, true);
        if (p.getIdSeccion() > 0) {
            tvSeccion.setBackground(ColorUtil.degradado(p.getColorInicio(), p.getColorFin(), 0f));
            tvSeccion.setTextColor(p.getColorFuente());
        } else {
            tvSeccion.setBackgroundColor(COLOR_SIN_SECCION);
        }

        cb.setOnCheckedChangeListener((btn, checked) -> {
            // Marcado = por comprar (verde); desmarcado = ya comprado (normal)
            fila.setBackgroundColor(checked ? COLOR_PENDIENTE : COLOR_NORMAL);
            int texto = checked ? COLOR_TEXTO_PENDIENTE : COLOR_TEXTO;
            tvNombre.setTextColor(texto);
            tvSuper.setTextColor(texto);
            listener.onComprarCambiado(p, checked);
        });
        FrameLayout contCheck = new FrameLayout(ctx);
        contCheck.addView(cb, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER));
        fila.addView(contCheck, new LinearLayout.LayoutParams(
                anchos[0], LinearLayout.LayoutParams.MATCH_PARENT));

        fila.addView(tvNombre);
        fila.addView(tvSuper);
        fila.addView(tvSeccion);

        // Icono de eliminar
        ImageButton btnEliminar = new ImageButton(ctx);
        btnEliminar.setImageResource(R.drawable.ic_delete);
        btnEliminar.setBackground(null);
        btnEliminar.setContentDescription(ctx.getString(R.string.cd_eliminar));
        btnEliminar.setOnClickListener(v -> listener.onEliminar(p));
        fila.addView(btnEliminar, new LinearLayout.LayoutParams(anchos[4], dp(40)));

        // Tocar la fila edita el producto
        fila.setOnClickListener(v -> listener.onEditar(p));
        return fila;
    }

    private View divisor(int total) {
        View v = new View(ctx);
        v.setBackgroundColor(COLOR_DIVISOR);
        v.setLayoutParams(new LinearLayout.LayoutParams(total, dp(1)));
        return v;
    }

    // ---------------- Utilidades ----------------

    private TextView celda(String texto, int ancho, float sp, int color, boolean negrita) {
        TextView tv = new TextView(ctx);
        tv.setText(texto);
        tv.setSingleLine(true);
        tv.setEllipsize(null);
        tv.setGravity(Gravity.CENTER);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        tv.setTextColor(color);
        if (negrita) tv.setTypeface(Typeface.DEFAULT_BOLD);
        tv.setLayoutParams(new LinearLayout.LayoutParams(ancho, LinearLayout.LayoutParams.MATCH_PARENT));
        return tv;
    }

    private Paint pintura(float sp, boolean negrita) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setTextSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp,
                ctx.getResources().getDisplayMetrics()));
        if (negrita) p.setTypeface(Typeface.DEFAULT_BOLD);
        return p;
    }

    private int dp(int valor) {
        return Math.round(valor * density);
    }
}
