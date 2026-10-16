package com.example.listacompra;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.util.List;

public class SeccionesActivity extends AppCompatActivity {

    private DatabaseHelper db;
    private ListView lista;
    private TextView tvVacio;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_secciones);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        db = new DatabaseHelper(this);
        lista = findViewById(R.id.listaSecciones);
        tvVacio = findViewById(R.id.tvVacioSecciones);

        lista.setOnItemClickListener((parent, view, position, id) ->
                mostrarDialogo((Seccion) parent.getItemAtPosition(position)));
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
        lista.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, secciones));
        tvVacio.setVisibility(secciones.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void mostrarDialogo(Seccion existente) {
        View vista = getLayoutInflater().inflate(R.layout.dialog_seccion, null);
        EditText etSeccion = vista.findViewById(R.id.etSeccion);
        EditText etSuper = vista.findViewById(R.id.etSupermercado);

        if (existente != null) {
            etSeccion.setText(existente.getSeccion());
            etSuper.setText(existente.getSupermercado());
        }

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
                    String sec = etSeccion.getText().toString().trim();
                    String sup = etSuper.getText().toString().trim();
                    if (sec.isEmpty()) {
                        etSeccion.setError(getString(R.string.campo_obligatorio));
                        return;
                    }
                    if (sup.isEmpty()) {
                        etSuper.setError(getString(R.string.campo_obligatorio));
                        return;
                    }
                    if (existente == null) db.insertarSeccion(sec, sup);
                    else db.actualizarSeccion(existente.getId(), sec, sup);
                    cargar();
                    dialogo.dismiss();
                }));
        dialogo.show();
    }

    private void confirmarEliminar(Seccion s) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.eliminar_seccion)
                .setMessage(getString(R.string.confirmar_eliminar_seccion, s.toString()))
                .setPositiveButton(R.string.eliminar, (d, w) -> {
                    db.eliminarSeccion(s.getId());
                    cargar();
                })
                .setNegativeButton(R.string.cancelar, null)
                .show();
    }
}
