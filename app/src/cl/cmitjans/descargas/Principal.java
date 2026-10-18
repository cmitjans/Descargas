package cl.cmitjans.descargas;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** Lista todo lo que hay en Descargas (incluidas sus subcarpetas), con filtros, orden y acciones. */
public class Principal extends Activity {
    private static final String[] ORDENES = {"Recientes", "Más pesados", "Nombre"};
    private static final int PROFUNDIDAD = 4;

    private final List<File> todos = new ArrayList<>();
    private final List<File> visibles = new ArrayList<>();
    private String filtro = Tipos.TODOS;
    private int orden = 0;
    private String busqueda = "";

    private File carpeta;
    private LinearLayout permiso;
    private LinearLayout contenido;
    private TextView resumen;
    private LinearLayout chipsFiltro;
    private LinearLayout chipsOrden;
    private Adaptador adaptador;
    private TextView vacio;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(Ui.BG);
        getWindow().setNavigationBarColor(Ui.BG);
        carpeta = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Ui.BG);
        root.setPadding(Ui.dp(this, 16), Ui.dp(this, 24), Ui.dp(this, 16), 0);

        root.addView(Ui.title(this, "Descargas"));
        resumen = Ui.text(this, "", 14, Ui.MUTED);
        resumen.setPadding(0, Ui.dp(this, 4), 0, Ui.dp(this, 14));
        root.addView(resumen);

        // Tarjeta que explica el permiso, si falta
        permiso = Ui.card(this);
        permiso.addView(Ui.text(this, "Para ver todo lo que hay en Descargas (PDF, ZIP, APK y lo que bajen otras apps), "
                + "Android pide el permiso de acceso a todos los archivos: no tiene uno solo para esa carpeta.\n\n"
                + "Esta app no tiene permiso de internet, así que Android no le deja enviar tus archivos a ningún lado.", 15, Ui.TEXT));
        Button dar = Ui.button(this, "Dar permiso", true);
        dar.setOnClickListener(v -> pedirPermiso());
        LinearLayout.LayoutParams lpb = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 46));
        lpb.topMargin = Ui.dp(this, 16);
        permiso.addView(dar, lpb);
        root.addView(permiso);

        contenido = new LinearLayout(this);
        contenido.setOrientation(LinearLayout.VERTICAL);

        EditText buscar = new EditText(this);
        buscar.setHint("Buscar por nombre");
        buscar.setHintTextColor(Ui.MUTED);
        buscar.setTextColor(Ui.TEXT);
        buscar.setSingleLine(true);
        buscar.setBackground(Ui.round(Ui.CARD, Ui.dp(this, 14)));
        buscar.setPadding(Ui.dp(this, 14), Ui.dp(this, 10), Ui.dp(this, 14), Ui.dp(this, 10));
        buscar.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void afterTextChanged(Editable s) {
                busqueda = s.toString().trim().toLowerCase(Locale.ROOT);
                aplicar();
            }
        });
        contenido.addView(buscar);

        chipsFiltro = filaChips();
        contenido.addView((View) chipsFiltro.getParent());
        chipsOrden = filaChips();
        contenido.addView((View) chipsOrden.getParent());

        vacio = Ui.text(this, "", 15, Ui.MUTED);
        vacio.setPadding(0, Ui.dp(this, 24), 0, 0);
        vacio.setGravity(Gravity.CENTER_HORIZONTAL);
        contenido.addView(vacio);

        ListView lista = new ListView(this);
        lista.setDivider(null);
        lista.setClipToPadding(false);
        lista.setPadding(0, Ui.dp(this, 4), 0, Ui.dp(this, 24));
        adaptador = new Adaptador();
        lista.setAdapter(adaptador);
        lista.setOnItemClickListener((p, v, pos, id) -> tocar(visibles.get(pos)));
        lista.setOnItemLongClickListener((p, v, pos, id) -> { menu(visibles.get(pos)); return true; });
        contenido.addView(lista, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));

        root.addView(contenido, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));
        setContentView(root);
        dibujarChips();
    }

    @Override
    protected void onResume() {
        super.onResume();
        boolean ok = tieneAcceso();
        permiso.setVisibility(ok ? View.GONE : View.VISIBLE);
        contenido.setVisibility(ok ? View.VISIBLE : View.GONE);
        if (ok) escanear(); else resumen.setText("Falta un permiso");
    }

    // ---------- Permiso ----------

    private boolean tieneAcceso() {
        if (Build.VERSION.SDK_INT >= 30) return Environment.isExternalStorageManager();
        return checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE) == android.content.pm.PackageManager.PERMISSION_GRANTED;
    }

    private void pedirPermiso() {
        if (Build.VERSION.SDK_INT >= 30) {
            try {
                startActivity(new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, Uri.parse("package:" + getPackageName())));
            } catch (RuntimeException e) {
                startActivity(new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION));
            }
        } else {
            requestPermissions(new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE,
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        }
    }

    @Override
    public void onRequestPermissionsResult(int code, String[] perms, int[] res) {
        onResume();
    }

    // ---------- Leer la carpeta ----------

    private void escanear() {
        resumen.setText("Revisando Descargas…");
        new Thread(() -> {
            List<File> encontrados = new ArrayList<>();
            recorrer(carpeta, 0, encontrados);
            runOnUiThread(() -> {
                todos.clear();
                todos.addAll(encontrados);
                aplicar();
            });
        }).start();
    }

    private void recorrer(File dir, int nivel, List<File> salida) {
        File[] hijos = dir.listFiles();
        if (hijos == null) return;
        for (File f : hijos) {
            String n = f.getName();
            if (n.startsWith(".")) continue; // ocultos y descargas a medio terminar (.pending-…)
            if (f.isDirectory()) {
                if (nivel < PROFUNDIDAD) recorrer(f, nivel + 1, salida);
            } else {
                salida.add(f);
            }
        }
    }

    private void aplicar() {
        visibles.clear();
        long total = 0;
        for (File f : todos) {
            if (!filtro.equals(Tipos.TODOS) && !Tipos.tipo(f).equals(filtro)) continue;
            if (!busqueda.isEmpty() && !f.getName().toLowerCase(Locale.ROOT).contains(busqueda)) continue;
            visibles.add(f);
            total += f.length();
        }
        Comparator<File> c;
        if (orden == 1) c = (a, b) -> Long.compare(b.length(), a.length());
        else if (orden == 2) c = (a, b) -> a.getName().compareToIgnoreCase(b.getName());
        else c = (a, b) -> Long.compare(b.lastModified(), a.lastModified());
        Collections.sort(visibles, c);
        adaptador.notifyDataSetChanged();
        resumen.setText(visibles.size() + (visibles.size() == 1 ? " archivo · " : " archivos · ") + Tipos.tamano(total)
                + (filtro.equals(Tipos.TODOS) && busqueda.isEmpty() ? " en total" : ""));
        vacio.setVisibility(visibles.isEmpty() ? View.VISIBLE : View.GONE);
        vacio.setText(todos.isEmpty() ? "La carpeta Descargas está vacía." : "No hay archivos con ese filtro.");
        dibujarChips();
    }

    // ---------- Filtros y orden ----------

    private LinearLayout filaChips() {
        HorizontalScrollView hs = new HorizontalScrollView(this);
        hs.setHorizontalScrollBarEnabled(false);
        hs.setPadding(0, Ui.dp(this, 10), 0, 0);
        LinearLayout fila = new LinearLayout(this);
        hs.addView(fila);
        return fila;
    }

    private void dibujarChips() {
        chipsFiltro.removeAllViews();
        for (String f : Tipos.FILTROS) {
            int n = 0;
            for (File x : todos) if (f.equals(Tipos.TODOS) || Tipos.tipo(x).equals(f)) n++;
            if (n == 0 && !f.equals(Tipos.TODOS) && !f.equals(filtro)) continue; // no mostrar tipos que no existen
            chipsFiltro.addView(chip(f + (todos.isEmpty() ? "" : " " + n), f.equals(filtro), v -> { filtro = f; aplicar(); }));
        }
        chipsOrden.removeAllViews();
        TextView et = Ui.text(this, "Ordenar:", 13, Ui.MUTED);
        et.setPadding(0, 0, Ui.dp(this, 8), 0);
        et.setGravity(Gravity.CENTER_VERTICAL);
        chipsOrden.addView(et, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, Ui.dp(this, 34)));
        for (int i = 0; i < ORDENES.length; i++) {
            final int k = i;
            chipsOrden.addView(chip(ORDENES[i], i == orden, v -> { orden = k; aplicar(); }));
        }
    }

    private TextView chip(String s, boolean activo, View.OnClickListener l) {
        TextView c = Ui.text(this, s, 13, activo ? Ui.BG : Ui.TEXT);
        c.setGravity(Gravity.CENTER);
        c.setPadding(Ui.dp(this, 14), 0, Ui.dp(this, 14), 0);
        c.setBackground(Ui.round(activo ? Ui.ACCENT : Ui.CHIP, Ui.dp(this, 17)));
        c.setOnClickListener(l);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, Ui.dp(this, 34));
        lp.rightMargin = Ui.dp(this, 8);
        c.setLayoutParams(lp);
        return c;
    }

    // ---------- Acciones ----------

    private void tocar(File f) {
        String ext = Tipos.extension(f);
        if (ext.equals("zip")) {
            startActivity(new Intent(this, VerZip.class).putExtra(VerZip.ARCHIVO, f.getAbsolutePath()));
        } else if (ext.equals("apk") && Build.VERSION.SDK_INT >= 26 && !getPackageManager().canRequestPackageInstalls()) {
            new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                    .setTitle("Permitir instalar apps")
                    .setMessage("Para abrir el instalador desde aquí, Android pide que permitas a Descargas \"instalar apps desconocidas\". "
                            + "Igual te preguntará antes de instalar cualquier cosa.")
                    .setPositiveButton("Abrir ajuste", (d, w) -> startActivity(new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                            Uri.parse("package:" + getPackageName()))))
                    .setNegativeButton("Cancelar", null)
                    .show();
        } else {
            Ui.abrir(this, f, false);
        }
    }

    private void menu(File f) {
        String[] opciones = {"Abrir con…", "Compartir", "Renombrar", "Borrar", "Detalles"};
        new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle(f.getName())
                .setItems(opciones, (d, i) -> {
                    if (i == 0) Ui.abrir(this, f, true);
                    else if (i == 1) Ui.compartir(this, f);
                    else if (i == 2) renombrar(f);
                    else if (i == 3) borrar(f);
                    else detalles(f);
                })
                .show();
    }

    private void borrar(File f) {
        new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle("¿Borrar este archivo?")
                .setMessage(f.getName() + "\n" + Tipos.tamano(f.length()) + "\n\nSe borra definitivamente: no pasa por una papelera.")
                .setPositiveButton("Borrar", (d, w) -> {
                    if (f.delete()) {
                        avisarSistema(f);
                        Toast.makeText(this, "Borrado", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "No se pudo borrar", Toast.LENGTH_LONG).show();
                    }
                    escanear();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void renombrar(File f) {
        EditText campo = new EditText(this);
        campo.setText(f.getName());
        campo.setSingleLine(true);
        campo.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        String n = f.getName();
        int punto = n.lastIndexOf('.');
        campo.setSelection(0, punto > 0 ? punto : n.length()); // selecciona el nombre sin la extensión
        LinearLayout marco = new LinearLayout(this);
        marco.setPadding(Ui.dp(this, 20), Ui.dp(this, 8), Ui.dp(this, 20), 0);
        marco.addView(campo, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle("Renombrar")
                .setView(marco)
                .setPositiveButton("Guardar", (d, w) -> {
                    String nuevo = campo.getText().toString().trim();
                    if (TextUtils.isEmpty(nuevo) || nuevo.contains("/") || nuevo.equals(n)) return;
                    File destino = new File(f.getParentFile(), nuevo);
                    if (destino.exists()) {
                        Toast.makeText(this, "Ya existe un archivo con ese nombre", Toast.LENGTH_LONG).show();
                    } else if (f.renameTo(destino)) {
                        avisarSistema(f);
                        avisarSistema(destino);
                    } else {
                        Toast.makeText(this, "No se pudo renombrar", Toast.LENGTH_LONG).show();
                    }
                    escanear();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void detalles(File f) {
        String carpetaRel = f.getParentFile().getAbsolutePath().replace(Environment.getExternalStorageDirectory().getAbsolutePath(), "");
        new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle(f.getName())
                .setMessage("Tipo: " + Tipos.tipo(f) + " (" + Tipos.mime(f) + ")"
                        + "\nTamaño: " + Tipos.tamano(f.length())
                        + "\nModificado: " + Tipos.fecha(f.lastModified())
                        + "\nCarpeta: " + carpetaRel)
                .setPositiveButton("Cerrar", null)
                .show();
    }

    /** Le avisa a Android que el archivo cambió, para que la galería y otras apps se actualicen. */
    private void avisarSistema(File f) {
        MediaScannerConnection.scanFile(this, new String[]{f.getAbsolutePath()}, null, null);
    }

    // ---------- Filas de la lista ----------

    private class Adaptador extends BaseAdapter {
        @Override public int getCount() { return visibles.size(); }
        @Override public Object getItem(int i) { return visibles.get(i); }
        @Override public long getItemId(int i) { return i; }

        @Override
        public View getView(int i, View reciclada, ViewGroup padre) {
            File f = visibles.get(i);
            LinearLayout fila = new LinearLayout(Principal.this);
            fila.setGravity(Gravity.CENTER_VERTICAL);
            fila.setPadding(0, Ui.dp(Principal.this, 10), 0, Ui.dp(Principal.this, 10));

            int s = Ui.dp(Principal.this, 44);
            fila.addView(Ui.badge(Principal.this, f), new LinearLayout.LayoutParams(s, s));

            LinearLayout textos = new LinearLayout(Principal.this);
            textos.setOrientation(LinearLayout.VERTICAL);
            textos.setPadding(Ui.dp(Principal.this, 14), 0, 0, 0);
            TextView nombre = Ui.text(Principal.this, f.getName(), 15, Ui.TEXT);
            nombre.setMaxLines(2);
            nombre.setEllipsize(TextUtils.TruncateAt.MIDDLE);
            textos.addView(nombre);
            String sub = Tipos.tamano(f.length()) + " · " + Tipos.fecha(f.lastModified());
            if (!f.getParentFile().equals(carpeta)) sub += " · en " + f.getParentFile().getName();
            textos.addView(Ui.text(Principal.this, sub, 13, Ui.MUTED));
            fila.addView(textos, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
            return fila;
        }
    }
}
