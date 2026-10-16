package com.example.listacompra;

import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;

public class SupermercadosActivity extends AppCompatActivity {

    private DatabaseHelper db;
    private ArrayAdapter<Supermercado> adapter;
    private TextView tvVacio;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_supermercados);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        db = new DatabaseHelper(this);
        tvVacio = findViewById(R.id.tvVacioSupermercados);

        ListView lista = findViewById(R.id.listaSupermercados);
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1,
                new ArrayList<Supermercado>());
        lista.setAdapter(adapter);
        lista.setOnItemClickListener((parent, view, position, id) ->
                mostrarDialogo(adapter.getItem(position)));

        findViewById(R.id.fabAnadirSupermercado).setOnClickListener(v -> mostrarDialogo(null));

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
        adapter.clear();
        adapter.addAll(db.getSupermercados());
        tvVacio.setVisibility(adapter.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void mostrarDialogo(Supermercado existente) {
        EditText et = new EditText(this);
        et.setHint(R.string.hint_nombre_supermercado);
        et.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        et.setSingleLine(true);
        int pad = dp(24);
        et.setPadding(pad, dp(16), pad, dp(8));
        if (existente != null) et.setText(existente.getNombre());

        AlertDialog.Builder b = new AlertDialog.Builder(this)
                .setTitle(existente == null ? R.string.nuevo_supermercado : R.string.editar_supermercado)
                .setView(et)
                .setPositiveButton(R.string.guardar, null)
                .setNegativeButton(R.string.cancelar, null);

        if (existente != null) {
            b.setNeutralButton(R.string.eliminar, (d, w) -> confirmarEliminar(existente));
        }

        AlertDialog dialogo = b.create();
        dialogo.setOnShowListener(d ->
                dialogo.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                    String nombre = et.getText().toString().trim();
                    if (nombre.isEmpty()) {
                        et.setError(getString(R.string.campo_obligatorio));
                        return;
                    }
                    long excluir = existente == null ? -1 : existente.getId();
                    if (db.existeSupermercado(nombre, excluir)) {
                        et.setError(getString(R.string.ya_existe_supermercado));
                        return;
                    }
                    if (existente == null) {
                        db.insertarSupermercado(nombre);
                    } else {
                        db.actualizarSupermercado(existente.getId(), nombre);
                    }
                    cargar();
                    dialogo.dismiss();
                }));
        dialogo.show();
    }

    private void confirmarEliminar(Supermercado s) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.eliminar_supermercado)
                .setMessage(getString(R.string.confirmar_eliminar_supermercado, s.getNombre()))
                .setPositiveButton(R.string.eliminar, (d, w) -> {
                    db.eliminarSupermercado(s.getId());
                    cargar();
                })
                .setNegativeButton(R.string.cancelar, null)
                .show();
    }

    private int dp(int valor) {
        return Math.round(valor * getResources().getDisplayMetrics().density);
    }
}
