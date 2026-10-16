package com.example.listacompra;

import android.graphics.Paint;
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
        h.tvSeccion.setText(p.getSeccion() == null ? "—" : p.getSeccion());
        h.tvSupermercado.setText(p.getSupermercado() == null ? "—" : p.getSupermercado());

        // Evita disparar el listener al reciclar vistas
        h.cbComprar.setOnCheckedChangeListener(null);
        h.cbComprar.setChecked(p.isComprar());
        aplicarTachado(h, p.isComprar());

        h.cbComprar.setOnCheckedChangeListener((btn, checked) -> {
            aplicarTachado(h, checked);
            listener.onComprarCambiado(p, checked);
        });
        h.itemView.setOnClickListener(v -> listener.onEditar(p));
        h.btnEliminar.setOnClickListener(v -> listener.onEliminar(p));
    }

    private void aplicarTachado(VH h, boolean tachado) {
        int flag = tachado
                ? (h.tvNombre.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG)
                : (h.tvNombre.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
        h.tvNombre.setPaintFlags(flag);
        h.tvNombre.setAlpha(tachado ? 0.5f : 1f);
    }

    @Override
    public int getItemCount() {
        return datos.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        final TextView tvNombre, tvSeccion, tvSupermercado;
        final CheckBox cbComprar;
        final ImageButton btnEliminar;

        VH(@NonNull View v) {
            super(v);
            tvNombre = v.findViewById(R.id.tvNombre);
            tvSeccion = v.findViewById(R.id.tvSeccion);
            tvSupermercado = v.findViewById(R.id.tvSupermercado);
            cbComprar = v.findViewById(R.id.cbComprar);
            btnEliminar = v.findViewById(R.id.btnEliminar);
        }
    }
}
