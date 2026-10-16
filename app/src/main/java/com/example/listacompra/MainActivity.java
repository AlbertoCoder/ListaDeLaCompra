package com.example.listacompra;

import android.content.Intent;
import android.net.Uri;
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
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements TablaProductos.Listener {

    private DatabaseHelper db;
    private TablaProductos tabla;
    private LinearLayout tablaContenido;
    private View scrollTabla;
    private EditText etBuscar;
    private Spinner spSupermercado, spSeccion, spEstado;
    private TextView tvVacio, tvResumen;

    private long filtroSeccionId = -1;        // -1 = todas
    private long filtroSupermercadoId = -1;   // -1 = todos

    private final ActivityResultLauncher<String> exportarLauncher = registerForActivityResult(
            new ActivityResultContracts.CreateDocument("application/octet-stream"),
            uri -> { if (uri != null) exportar(uri); });

    private final ActivityResultLauncher<String[]> importarLauncher = registerForActivityResult(
            new ActivityResultContracts.OpenDocument(),
            uri -> { if (uri != null) confirmarImportacion(uri); });

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
        scrollTabla = findViewById(R.id.scrollTabla);
        tablaContenido = findViewById(R.id.tablaContenido);

        tabla = new TablaProductos(this, tablaContenido, this);

        spEstado.setAdapter(ArrayAdapter.createFromResource(this,
                R.array.filtro_estado, android.R.layout.simple_spinner_dropdown_item));

        AdapterView.OnItemSelectedListener recarga = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (parent == spSeccion) {
                    filtroSeccionId = ((Seccion) parent.getItemAtPosition(position)).getId();
                } else if (parent == spSupermercado) {
                    filtroSupermercadoId = ((Supermercado) parent.getItemAtPosition(position)).getId();
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

        List<Supermercado> supers = new ArrayList<>();
        supers.add(new Supermercado(-1, getString(R.string.todos_supermercados)));
        supers.addAll(db.getSupermercados());

        int posSuper = 0;
        for (int i = 0; i < supers.size(); i++) {
            if (supers.get(i).getId() == filtroSupermercadoId) {
                posSuper = i;
                break;
            }
        }
        spSupermercado.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, supers));
        spSupermercado.setSelection(posSuper);
        filtroSupermercadoId = supers.get(posSuper).getId();
    }

    private void cargarProductos() {
        List<Producto> lista = db.getProductos(
                etBuscar.getText().toString().trim(),
                filtroSeccionId,
                filtroSupermercadoId,
                spEstado.getSelectedItemPosition());

        tabla.mostrar(lista);
        scrollTabla.setVisibility(lista.isEmpty() ? View.GONE : View.VISIBLE);
        tvVacio.setVisibility(lista.isEmpty() ? View.VISIBLE : View.GONE);

        int porComprar = 0;
        for (Producto p : lista) if (p.isComprar()) porComprar++;
        tvResumen.setText(getString(R.string.resumen, porComprar, lista.size() - porComprar));
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
        if (id == R.id.action_supermercados) {
            startActivity(new Intent(this, SupermercadosActivity.class));
            return true;
        } else if (id == R.id.action_secciones) {
            startActivity(new Intent(this, SeccionesActivity.class));
            return true;
        } else if (id == R.id.action_marcar) {
            db.marcarTodos(true);
            cargarProductos();
            Toast.makeText(this, R.string.todos_marcados, Toast.LENGTH_SHORT).show();
            return true;
        } else if (id == R.id.action_desmarcar) {
            db.marcarTodos(false);
            cargarProductos();
            Toast.makeText(this, R.string.todos_desmarcados, Toast.LENGTH_SHORT).show();
            return true;
        } else if (id == R.id.action_exportar) {
            exportarLauncher.launch(DatabaseHelper.DB_NAME);
            return true;
        } else if (id == R.id.action_importar) {
            importarLauncher.launch(new String[]{"*/*"});
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // ---------------- Exportar / importar ----------------

    private void exportar(Uri destino) {
        try {
            BackupDb.exportar(this, db, destino);
            Toast.makeText(this, R.string.exportar_ok, Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            Toast.makeText(this, getString(R.string.error_operacion, e.getMessage()),
                    Toast.LENGTH_LONG).show();
        }
    }

    private void confirmarImportacion(Uri origen) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.titulo_importar)
                .setMessage(R.string.confirmar_importar)
                .setPositiveButton(R.string.continuar, (d, w) -> importar(origen))
                .setNegativeButton(R.string.cancelar, null)
                .show();
    }

    private void importar(Uri origen) {
        try {
            BackupDb.importar(this, db, origen);
            Toast.makeText(this, R.string.importar_ok, Toast.LENGTH_LONG).show();
            cargarFiltros();
            cargarProductos();
        } catch (IOException e) {
            Toast.makeText(this, getString(R.string.error_operacion, e.getMessage()),
                    Toast.LENGTH_LONG).show();
        }
    }

    // ---------------- Callbacks de la tabla ----------------

    @Override
    public void onComprarCambiado(Producto p, boolean comprar) {
        db.setComprar(p.getId(), comprar);
        p.setComprar(comprar);
        tablaContenido.post(this::cargarProductos); // reordena tras terminar el layout actual
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
        Spinner spSuperDialogo = vista.findViewById(R.id.spSupermercadoDialogo);
        Spinner spSecDialogo = vista.findViewById(R.id.spSeccionDialogo);
        CheckBox cb = vista.findViewById(R.id.cbComprarDialogo);

        List<Supermercado> supers = new ArrayList<>();
        supers.add(new Supermercado(0, getString(R.string.sin_supermercado)));
        supers.addAll(db.getSupermercados());
        spSuperDialogo.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, supers));

        List<Seccion> secciones = new ArrayList<>();
        secciones.add(new Seccion(0, getString(R.string.sin_seccion), 0, 0, 0));
        secciones.addAll(db.getSecciones());
        spSecDialogo.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, secciones));

        if (existente != null) {
            etNombre.setText(existente.getNombre());
            etNombre.setSelection(etNombre.getText().length());
            cb.setChecked(existente.isComprar());
            for (int i = 0; i < supers.size(); i++) {
                if (supers.get(i).getId() == existente.getIdSupermercado()) {
                    spSuperDialogo.setSelection(i);
                    break;
                }
            }
            for (int i = 0; i < secciones.size(); i++) {
                if (secciones.get(i).getId() == existente.getIdSeccion()) {
                    spSecDialogo.setSelection(i);
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
                    long idSuper = ((Supermercado) spSuperDialogo.getSelectedItem()).getId();
                    long idSeccion = ((Seccion) spSecDialogo.getSelectedItem()).getId();
                    if (existente == null) {
                        db.insertarProducto(nombre, cb.isChecked(), idSuper, idSeccion);
                    } else {
                        db.actualizarProducto(existente.getId(), nombre, cb.isChecked(),
                                idSuper, idSeccion);
                    }
                    cargarProductos();
                    dialogo.dismiss();
                }));
        dialogo.show();
    }
}
