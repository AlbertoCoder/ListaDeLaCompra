package com.example.listacompra;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class ProductoAdapter extends RecyclerView.Adapter<ProductoAdapter.VH> {

    public interface Listener {
        void onComprarCambiado(Producto p, boolean comprar);
        void onEditar(Producto p);
        void onEliminar(Producto p);
    }

    private static final int COLOR_FILA_NORMAL = Color.WHITE;
    private static final int COLOR_FILA_PENDIENTE = Color.parseColor("#2E7D32");
    private static final int COLOR_TEXTO = Color.parseColor("#212121");
    private static final int COLOR_SIN_SECCION = Color.parseColor("#F5F5F5");

    private final List<Producto> datos = new ArrayList<>();
    private final Listener listener;

    public ProductoAdapter(Listener listener) {
        this.listener = listener;
    }

    public void setDatos(List<Producto> nuevos) {
        datos.clear();
        datos.addAll(nuevos);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_producto, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Producto p = datos.get(position);

        h.tvNombre.setText(p.getNombre());
        h.tvSupermercado.setText(p.getSupermercado() == null ? "—" : p.getSupermercado());
        h.tvSeccion.setText(p.getSeccion() == null ? "—" : p.getSeccion());

        // Celda de sección: degradado elegido por el usuario y letra blanca/negra calculada
        if (p.getIdSeccion() > 0) {
            h.tvSeccion.setBackground(ColorUtil.degradado(
                    p.getColorInicio(), p.getColorFin(), 0f));
            h.tvSeccion.setTextColor(p.getColorFuente());
        } else {
            h.tvSeccion.setBackgroundColor(COLOR_SIN_SECCION);
            h.tvSeccion.setTextColor(COLOR_TEXTO);
        }

        // Evita disparar el listener al reciclar vistas
        h.cbComprar.setOnCheckedChangeListener(null);
        h.cbComprar.setChecked(p.isComprar());
        aplicarColorFila(h, p.isComprar());

        h.cbComprar.setOnCheckedChangeListener((btn, checked) -> {
            aplicarColorFila(h, checked);
            listener.onComprarCambiado(p, checked);
        });
        h.itemView.setOnClickListener(v -> listener.onEditar(p));
        h.btnEliminar.setOnClickListener(v -> listener.onEliminar(p));
    }

    /**
     * Marcado = por comprar: fondo verde y texto blanco.
     * Desmarcado = ya comprado: colores predeterminados.
     */
    private void aplicarColorFila(VH h, boolean porComprar) {
        h.rowRoot.setBackgroundColor(porComprar ? COLOR_FILA_PENDIENTE : COLOR_FILA_NORMAL);
        int texto = porComprar ? Color.WHITE : COLOR_TEXTO;
        h.tvNombre.setTextColor(texto);
        h.tvSupermercado.setTextColor(texto);
    }

    @Override
    public int getItemCount() {
        return datos.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        final View rowRoot;
        final TextView tvNombre, tvSupermercado, tvSeccion;
        final CheckBox cbComprar;
        final ImageButton btnEliminar;

        VH(@NonNull View v) {
            super(v);
            rowRoot = v.findViewById(R.id.rowRoot);
            tvNombre = v.findViewById(R.id.tvNombre);
            tvSupermercado = v.findViewById(R.id.tvSupermercado);
            tvSeccion = v.findViewById(R.id.tvSeccion);
            cbComprar = v.findViewById(R.id.cbComprar);
            btnEliminar = v.findViewById(R.id.btnEliminar);
        }
    }
}
