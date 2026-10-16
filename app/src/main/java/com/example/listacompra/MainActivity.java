package com.example.listacompra;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements ProductoAdapter.Listener {

    private DatabaseHelper db;
    private ProductoAdapter adapter;
    private RecyclerView recycler;
    private EditText etBuscar;
    private Spinner spSupermercado, spSeccion, spEstado;
    private TextView tvVacio, tvResumen;

    private long filtroSeccionId = -1;         // -1 = todas
    private String filtroSupermercado = null;  // null = todos

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        db = new DatabaseHelper(this);

        etBuscar = findViewById(R.id.etBuscar);
        spSupermercado = findViewById(R.id.spSupermercado);
        spSeccion = findViewById(R.id.spSeccion);
        spEstado = findViewById(R.id.spEstado);
        tvVacio = findViewById(R.id.tvVacio);
        tvResumen = findViewById(R.id.tvResumen);
        recycler = findViewById(R.id.recycler);

        adapter = new ProductoAdapter(this);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        recycler.setAdapter(adapter);

        spEstado.setAdapter(ArrayAdapter.createFromResource(this,
                R.array.filtro_estado, android.R.layout.simple_spinner_dropdown_item));

        AdapterView.OnItemSelectedListener recarga = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (parent == spSeccion) {
                    filtroSeccionId = ((Seccion) parent.getItemAtPosition(position)).getId();
                } else if (parent == spSupermercado) {
                    filtroSupermercado = position == 0 ? null : (String) parent.getItemAtPosition(position);
                }
                cargarProductos();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        };
        spSeccion.setOnItemSelectedListener(recarga);
        spSupermercado.setOnItemSelectedListener(recarga);
        spEstado.setOnItemSelectedListener(recarga);

        etBuscar.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void afterTextChanged(Editable s) { cargarProductos(); }
        });

        findViewById(R.id.fabAnadir).setOnClickListener(v -> mostrarDialogoProducto(null));
    }

    @Override
    protected void onResume() {
        super.onResume();
        cargarFiltros();   // secciones y supermercados pueden haber cambiado
        cargarProductos();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        db.close();
    }

    // ---------------- Carga de datos ----------------

    private void cargarFiltros() {
        List<Seccion> secciones = new ArrayList<>();
        secciones.add(new Seccion(-1, getString(R.string.todas_secciones), 0, 0, 0));
        secciones.addAll(db.getSecciones());

        int posSeccion = 0;
        for (int i = 0; i < secciones.size(); i++) {
            if (secciones.get(i).getId() == filtroSeccionId) {
                posSeccion = i;
                break;
            }
        }
        spSeccion.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, secciones));
        spSeccion.setSelection(posSeccion);
        filtroSeccionId = secciones.get(posSeccion).getId();

        List<String> supers = new ArrayList<>();
        supers.add(getString(R.string.todos_supermercados));
        supers.addAll(db.getSupermercados());

        int posSuper = filtroSupermercado == null ? 0 : supers.indexOf(filtroSupermercado);
        if (posSuper < 0) posSuper = 0;
        spSupermercado.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, supers));
        spSupermercado.setSelection(posSuper);
        filtroSupermercado = posSuper == 0 ? null : supers.get(posSuper);
    }

    private void cargarProductos() {
        List<Producto> lista = db.getProductos(
                etBuscar.getText().toString().trim(),
                filtroSeccionId,
                filtroSupermercado,
                spEstado.getSelectedItemPosition());
        adapter.setDatos(lista);

        int porComprar = 0;
        for (Producto p : lista) if (p.isComprar()) porComprar++;
        tvResumen.setText(getString(R.string.resumen, porComprar, lista.size() - porComprar));
        tvVacio.setVisibility(lista.isEmpty() ? View.VISIBLE : View.GONE);
    }

    // ---------------- Menú ----------------

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_secciones) {
            startActivity(new Intent(this, SeccionesActivity.class));
            return true;
        } else if (id == R.id.action_marcar) {
            db.marcarTodos(true);            // todo vuelve a "por comprar" (p. ej. para una nueva lista)
            cargarProductos();
            Toast.makeText(this, R.string.todos_marcados, Toast.LENGTH_SHORT).show();
            return true;
        } else if (id == R.id.action_desmarcar) {
            db.marcarTodos(false);           // todo queda "ya comprado"
            cargarProductos();
            Toast.makeText(this, R.string.todos_desmarcados, Toast.LENGTH_SHORT).show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // ---------------- Callbacks del adaptador ----------------

    @Override
    public void onComprarCambiado(Producto p, boolean comprar) {
        db.setComprar(p.getId(), comprar);
        p.setComprar(comprar);
        recycler.post(this::cargarProductos); // reordena tras terminar el layout actual
    }

    @Override
    public void onEditar(Producto p) {
        mostrarDialogoProducto(p);
    }

    @Override
    public void onEliminar(Producto p) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.eliminar_producto)
                .setMessage(getString(R.string.confirmar_eliminar, p.getNombre()))
                .setPositiveButton(R.string.eliminar, (d, w) -> {
                    db.eliminarProducto(p.getId());
                    cargarProductos();
                })
                .setNegativeButton(R.string.cancelar, null)
                .show();
    }

    // ---------------- Diálogo crear / editar producto ----------------

    private void mostrarDialogoProducto(Producto existente) {
        View vista = getLayoutInflater().inflate(R.layout.dialog_producto, null);
        EditText etNombre = vista.findViewById(R.id.etNombre);
        AutoCompleteTextView acSuper = vista.findViewById(R.id.acSupermercado);
        Spinner spSec = vista.findViewById(R.id.spSeccionDialogo);
        CheckBox cb = vista.findViewById(R.id.cbComprarDialogo);

        // Sugerencias de supermercados existentes; también se puede escribir uno nuevo
        acSuper.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, db.getSupermercados()));
        acSuper.setThreshold(0);

        List<Seccion> opciones = new ArrayList<>();
        opciones.add(new Seccion(0, getString(R.string.sin_seccion), 0, 0, 0));
        opciones.addAll(db.getSecciones());
        spSec.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, opciones));

        if (existente != null) {
            etNombre.setText(existente.getNombre());
            etNombre.setSelection(etNombre.getText().length());
            if (existente.getSupermercado() != null) acSuper.setText(existente.getSupermercado());
            cb.setChecked(existente.isComprar());
            for (int i = 0; i < opciones.size(); i++) {
                if (opciones.get(i).getId() == existente.getIdSeccion()) {
                    spSec.setSelection(i);
                    break;
                }
            }
        } else {
            cb.setChecked(true);
        }

        AlertDialog dialogo = new AlertDialog.Builder(this)
                .setTitle(existente == null ? R.string.nuevo_producto : R.string.editar_producto)
                .setView(vista)
                .setPositiveButton(R.string.guardar, null) // se sobrescribe abajo
                .setNegativeButton(R.string.cancelar, null)
                .create();

        dialogo.setOnShowListener(d ->
                dialogo.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                    String nombre = etNombre.getText().toString().trim();
                    if (nombre.isEmpty()) {
                        etNombre.setError(getString(R.string.nombre_obligatorio));
                        return;
                    }
                    String supermercado = acSuper.getText().toString().trim();
                    long idSeccion = ((Seccion) spSec.getSelectedItem()).getId();
                    if (existente == null) {
                        db.insertarProducto(nombre, cb.isChecked(), supermercado, idSeccion);
                    } else {
                        db.actualizarProducto(existente.getId(), nombre, cb.isChecked(),
                                supermercado, idSeccion);
                    }
                    cargarProductos();
                    dialogo.dismiss();
                }));
        dialogo.show();
    }
}
