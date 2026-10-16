package com.example.listacompra;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;

public class SeccionesActivity extends AppCompatActivity {

    /** Paleta seleccionable para los colores del degradado. */
    private static final int[] PALETA = {
            0xFFFFFFFF, 0xFFE0E0E0, 0xFF9E9E9E, 0xFF424242, 0xFF000000, 0xFF795548,
            0xFFFFEB3B, 0xFFFF9800, 0xFFF44336, 0xFFE91E63, 0xFF9C27B0, 0xFF673AB7,
            0xFF3F51B5, 0xFF2196F3, 0xFF03A9F4, 0xFF00BCD4, 0xFF009688, 0xFF4CAF50,
            0xFF8BC34A, 0xFFCDDC39, 0xFFFFCDD2, 0xFFC8E6C9, 0xFFBBDEFB, 0xFFFFF9C4
    };
    private static final int COLUMNAS = 6;
    private static final int COLOR_INICIO_DEFECTO = 0xFFC8E6C9;
    private static final int COLOR_FIN_DEFECTO = 0xFF4CAF50;

    private DatabaseHelper db;
    private SeccionAdapter adapter;
    private TextView tvVacio;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_secciones);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        db = new DatabaseHelper(this);
        tvVacio = findViewById(R.id.tvVacioSecciones);

        RecyclerView recycler = findViewById(R.id.rvSecciones);
        adapter = new SeccionAdapter(this::mostrarDialogo);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        recycler.setAdapter(adapter);

        findViewById(R.id.fabAnadirSeccion).setOnClickListener(v -> mostrarDialogo(null));

        cargar();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        db.close();
    }

    private void cargar() {
        List<Seccion> secciones = db.getSecciones();
        adapter.setDatos(secciones);
        tvVacio.setVisibility(secciones.isEmpty() ? View.VISIBLE : View.GONE);
    }

    // ---------------- Diálogo crear / editar sección ----------------

    private void mostrarDialogo(Seccion existente) {
        View vista = getLayoutInflater().inflate(R.layout.dialog_seccion, null);
        EditText etSeccion = vista.findViewById(R.id.etSeccion);
        LinearLayout llInicio = vista.findViewById(R.id.llPaletaInicio);
        LinearLayout llFin = vista.findViewById(R.id.llPaletaFin);
        TextView tvPrevia = vista.findViewById(R.id.tvPrevia);

        final int[] colores = {
                existente != null ? existente.getColorInicio() : COLOR_INICIO_DEFECTO,
                existente != null ? existente.getColorFin() : COLOR_FIN_DEFECTO
        };

        Runnable actualizarPrevia = () -> {
            String nombre = etSeccion.getText().toString().trim();
            tvPrevia.setText(nombre.isEmpty() ? getString(R.string.vista_previa) : nombre);
            float radio = getResources().getDisplayMetrics().density * 8;
            tvPrevia.setBackground(ColorUtil.degradado(colores[0], colores[1], radio));
            tvPrevia.setTextColor(ColorUtil.colorFuente(colores[0], colores[1]));
        };

        if (existente != null) etSeccion.setText(existente.getSeccion());

        llInicio.addView(crearPaleta(colores[0], c -> {
            colores[0] = c;
            actualizarPrevia.run();
        }));
        llFin.addView(crearPaleta(colores[1], c -> {
            colores[1] = c;
            actualizarPrevia.run();
        }));

        etSeccion.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void afterTextChanged(Editable s) { actualizarPrevia.run(); }
        });
        actualizarPrevia.run();

        AlertDialog.Builder b = new AlertDialog.Builder(this)
                .setTitle(existente == null ? R.string.nueva_seccion : R.string.editar_seccion)
                .setView(vista)
                .setPositiveButton(R.string.guardar, null)
                .setNegativeButton(R.string.cancelar, null);

        if (existente != null) {
            b.setNeutralButton(R.string.eliminar, (d, w) -> confirmarEliminar(existente));
        }

        AlertDialog dialogo = b.create();
        dialogo.setOnShowListener(d ->
                dialogo.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                    String nombre = etSeccion.getText().toString().trim();
                    if (nombre.isEmpty()) {
                        etSeccion.setError(getString(R.string.campo_obligatorio));
                        return;
                    }
                    if (existente == null) {
                        db.insertarSeccion(nombre, colores[0], colores[1]);
                    } else {
                        db.actualizarSeccion(existente.getId(), nombre, colores[0], colores[1]);
                    }
                    cargar();
                    dialogo.dismiss();
                }));
        dialogo.show();
    }

    /** Cuadrícula de colores; devuelve la vista y avisa al elegir uno. */
    private View crearPaleta(int seleccionado, IntConsumer alElegir) {
        LinearLayout contenedor = new LinearLayout(this);
        contenedor.setOrientation(LinearLayout.VERTICAL);

        List<View> swatches = new ArrayList<>();
        int[] actual = {seleccionado};
        LinearLayout fila = null;

        for (int i = 0; i < PALETA.length; i++) {
            if (i % COLUMNAS == 0) {
                fila = new LinearLayout(this);
                fila.setOrientation(LinearLayout.HORIZONTAL);
                contenedor.addView(fila);
            }
            final int color = PALETA[i];
            View sw = new View(this);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(36), 1f);
            lp.setMargins(dp(3), dp(3), dp(3), dp(3));
            fila.addView(sw, lp);
            swatches.add(sw);
            pintarSwatch(sw, color, color == actual[0]);

            sw.setOnClickListener(v -> {
                actual[0] = color;
                for (int j = 0; j < swatches.size(); j++) {
                    pintarSwatch(swatches.get(j), PALETA[j], PALETA[j] == color);
                }
                alElegir.accept(color);
            });
        }
        return contenedor;
    }

    private void pintarSwatch(View v, int color, boolean seleccionado) {
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(color);
        gd.setCornerRadius(dp(6));
        gd.setStroke(seleccionado ? dp(3) : dp(1), seleccionado ? Color.BLACK : 0xFFBDBDBD);
        v.setBackground(gd);
    }

    private int dp(int valor) {
        return Math.round(valor * getResources().getDisplayMetrics().density);
    }

    private void confirmarEliminar(Seccion s) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.eliminar_seccion)
                .setMessage(getString(R.string.confirmar_eliminar_seccion, s.getSeccion()))
                .setPositiveButton(R.string.eliminar, (d, w) -> {
                    db.eliminarSeccion(s.getId());
                    cargar();
                })
                .setNegativeButton(R.string.cancelar, null)
                .show();
    }

    // ---------------- Lista de secciones ----------------

    /** Muestra cada sección con el mismo degradado y color de letra que tendrá en la tabla. */
    static class SeccionAdapter extends RecyclerView.Adapter<SeccionAdapter.VH> {

        interface OnClick {
            void onClick(Seccion s);
        }

        private final List<Seccion> datos = new ArrayList<>();
        private final OnClick onClick;

        SeccionAdapter(OnClick onClick) {
            this.onClick = onClick;
        }

        void setDatos(List<Seccion> nuevos) {
            datos.clear();
            datos.addAll(nuevos);
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_seccion, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int position) {
            Seccion s = datos.get(position);
            float radio = h.tv.getResources().getDisplayMetrics().density * 8;
            h.tv.setText(s.getSeccion());
            h.tv.setBackground(ColorUtil.degradado(s.getColorInicio(), s.getColorFin(), radio));
            h.tv.setTextColor(s.getColorFuente());
            h.itemView.setOnClickListener(v -> onClick.onClick(s));
        }

        @Override
        public int getItemCount() {
            return datos.size();
        }

        static class VH extends RecyclerView.ViewHolder {
            final TextView tv;

            VH(@NonNull View v) {
                super(v);
                tv = v.findViewById(R.id.tvSeccionItem);
            }
        }
    }
}
