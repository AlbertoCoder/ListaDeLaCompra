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
    private Spinner spSeccion, spEstado;
    private TextView tvVacio, tvResumen;

    /** -1 = todas las secciones */
    private long filtroSeccionId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        db = new DatabaseHelper(this);

        etBuscar = findViewById(R.id.etBuscar);
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
                }
                cargarProductos();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        };
        spSeccion.setOnItemSelectedListener(recarga);
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
        cargarSeccionesFiltro(); // las secciones pueden haber cambiado
        cargarProductos();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        db.close();
    }

    // ---------------- Carga de datos ----------------

    private void cargarSeccionesFiltro() {
        List<Seccion> opciones = new ArrayList<>();
        opciones.add(new Seccion(-1, getString(R.string.todas_secciones), ""));
        opciones.addAll(db.getSecciones());

        int seleccion = 0;
        for (int i = 0; i < opciones.size(); i++) {
            if (opciones.get(i).getId() == filtroSeccionId) {
                seleccion = i;
                break;
            }
        }
        spSeccion.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, opciones));
        spSeccion.setSelection(seleccion);
        filtroSeccionId = opciones.get(seleccion).getId();
    }

    private void cargarProductos() {
        List<Producto> lista = db.getProductos(
                etBuscar.getText().toString().trim(),
                filtroSeccionId,
                spEstado.getSelectedItemPosition());
        adapter.setDatos(lista);

        int pendientes = 0;
        for (Producto p : lista) if (!p.isComprar()) pendientes++;
        tvResumen.setText(getString(R.string.resumen, lista.size(), pendientes));
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
        } else if (id == R.id.action_desmarcar) {
            db.desmarcarTodos();
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
        // Reordena/refiltra tras terminar el layout actual
        recycler.post(this::cargarProductos);
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

    // ---------------- Diálogo crear / editar ----------------

    private void mostrarDialogoProducto(Producto existente) {
        View vista = getLayoutInflater().inflate(R.layout.dialog_producto, null);
        EditText etNombre = vista.findViewById(R.id.etNombre);
        Spinner spSec = vista.findViewById(R.id.spSeccionDialogo);
        CheckBox cb = vista.findViewById(R.id.cbComprarDialogo);

        List<Seccion> opciones = new ArrayList<>();
        opciones.add(new Seccion(0, getString(R.string.sin_seccion), ""));
        opciones.addAll(db.getSecciones());
        spSec.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, opciones));

        if (existente != null) {
            etNombre.setText(existente.getNombre());
            etNombre.setSelection(etNombre.getText().length());
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
                    long idSeccion = ((Seccion) spSec.getSelectedItem()).getId();
                    if (existente == null) {
                        db.insertarProducto(nombre, cb.isChecked(), idSeccion);
                    } else {
                        db.actualizarProducto(existente.getId(), nombre, cb.isChecked(), idSeccion);
                    }
                    cargarProductos();
                    dialogo.dismiss();
                }));
        dialogo.show();
    }
}
