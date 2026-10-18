package cl.cmitjans.descargas;

import android.app.Activity;
import android.media.MediaScannerConnection;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/** Muestra qué trae un ZIP y permite extraerlo en una carpeta al lado. */
public class VerZip extends Activity {
    static final String ARCHIVO = "archivo";

    private File zip;
    private final List<ZipEntry> entradas = new ArrayList<>();
    private Charset charset = Charset.forName("UTF-8");
    private TextView resumen;
    private Button extraer;
    private BaseAdapter adaptador;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(Ui.BG);
        getWindow().setNavigationBarColor(Ui.BG);
        zip = new File(getIntent().getStringExtra(ARCHIVO));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Ui.BG);
        root.setPadding(Ui.dp(this, 16), Ui.dp(this, 24), Ui.dp(this, 16), Ui.dp(this, 16));

        TextView t = Ui.title(this, zip.getName());
        t.setTextSize(20);
        t.setMaxLines(2);
        t.setEllipsize(TextUtils.TruncateAt.MIDDLE);
        root.addView(t);
        resumen = Ui.text(this, "Leyendo…", 14, Ui.MUTED);
        resumen.setPadding(0, Ui.dp(this, 4), 0, Ui.dp(this, 12));
        root.addView(resumen);

        ListView lista = new ListView(this);
        lista.setDivider(null);
        adaptador = new BaseAdapter() {
            @Override public int getCount() { return entradas.size(); }
            @Override public Object getItem(int i) { return entradas.get(i); }
            @Override public long getItemId(int i) { return i; }
            @Override public View getView(int i, View v, ViewGroup p) {
                ZipEntry e = entradas.get(i);
                LinearLayout fila = new LinearLayout(VerZip.this);
                fila.setOrientation(LinearLayout.VERTICAL);
                fila.setPadding(0, Ui.dp(VerZip.this, 8), 0, Ui.dp(VerZip.this, 8));
                TextView n = Ui.text(VerZip.this, e.getName(), 14, e.isDirectory() ? Ui.MUTED : Ui.TEXT);
                n.setMaxLines(2);
                n.setEllipsize(TextUtils.TruncateAt.MIDDLE);
                fila.addView(n);
                if (!e.isDirectory() && e.getSize() >= 0) fila.addView(Ui.text(VerZip.this, Tipos.tamano(e.getSize()), 12, Ui.MUTED));
                return fila;
            }
        };
        lista.setAdapter(adaptador);
        root.addView(lista, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));

        extraer = Ui.button(this, "Extraer", true);
        extraer.setEnabled(false);
        extraer.setOnClickListener(v -> extraerTodo());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 48));
        lp.topMargin = Ui.dp(this, 12);
        root.addView(extraer, lp);
        TextView nota = Ui.text(this, "Se crea una carpeta \"" + destino().getName() + "\" junto al ZIP. El ZIP no se borra.", 12, Ui.MUTED);
        nota.setGravity(Gravity.CENTER_HORIZONTAL);
        nota.setPadding(0, Ui.dp(this, 8), 0, 0);
        root.addView(nota);

        setContentView(root);
        leer();
    }

    private File destino() {
        String n = zip.getName();
        int i = n.toLowerCase().lastIndexOf(".zip");
        File d = new File(zip.getParentFile(), i > 0 ? n.substring(0, i) : n + "_contenido");
        int k = 2;
        File libre = d;
        while (libre.exists()) libre = new File(d.getPath() + " (" + k++ + ")");
        return libre;
    }

    private ZipFile abrirZip() throws IOException {
        try {
            ZipFile z = new ZipFile(zip, charset);
            z.size();
            Enumeration<? extends ZipEntry> en = z.entries();
            while (en.hasMoreElements()) en.nextElement(); // fuerza a leer los nombres
            return z;
        } catch (IllegalArgumentException e) {
            // Nombres que no son UTF-8 (ZIP hechos en Windows antiguos)
            charset = Charset.forName("IBM437");
            return new ZipFile(zip, charset);
        }
    }

    private void leer() {
        new Thread(() -> {
            List<ZipEntry> lista = new ArrayList<>();
            String error = null;
            long total = 0;
            try (ZipFile z = abrirZip()) {
                Enumeration<? extends ZipEntry> en = z.entries();
                while (en.hasMoreElements()) {
                    ZipEntry e = en.nextElement();
                    lista.add(e);
                    if (e.getSize() > 0) total += e.getSize();
                }
            } catch (IOException | RuntimeException e) {
                error = "No se pudo leer el ZIP (puede estar dañado o protegido con contraseña).";
            }
            final String err = error;
            final long tot = total;
            runOnUiThread(() -> {
                if (err != null) { resumen.setText(err); return; }
                entradas.clear();
                entradas.addAll(lista);
                adaptador.notifyDataSetChanged();
                int archivos = 0;
                for (ZipEntry e : lista) if (!e.isDirectory()) archivos++;
                resumen.setText(archivos + (archivos == 1 ? " archivo · " : " archivos · ") + Tipos.tamano(tot) + " al extraer");
                extraer.setEnabled(archivos > 0);
            });
        }).start();
    }

    private void extraerTodo() {
        extraer.setEnabled(false);
        extraer.setText("Extrayendo…");
        File dest = destino();
        new Thread(() -> {
            String error = null;
            List<String> creados = new ArrayList<>();
            try (ZipFile z = abrirZip()) {
                String raiz = dest.getCanonicalPath() + File.separator;
                Enumeration<? extends ZipEntry> en = z.entries();
                while (en.hasMoreElements()) {
                    ZipEntry e = en.nextElement();
                    File salida = new File(dest, e.getName());
                    // Protección: un ZIP malicioso podría intentar escribir fuera de la carpeta con "../"
                    if (!salida.getCanonicalPath().startsWith(raiz)) continue;
                    if (e.isDirectory()) { salida.mkdirs(); continue; }
                    File padre = salida.getParentFile();
                    if (padre != null) padre.mkdirs();
                    try (InputStream in = new BufferedInputStream(z.getInputStream(e));
                         OutputStream out = new BufferedOutputStream(new FileOutputStream(salida))) {
                        byte[] buf = new byte[64 * 1024];
                        int n;
                        while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
                    }
                    creados.add(salida.getAbsolutePath());
                }
            } catch (IOException | RuntimeException e) {
                error = "Se interrumpió la extracción: " + e.getMessage();
            }
            if (!creados.isEmpty()) MediaScannerConnection.scanFile(this, creados.toArray(new String[0]), null, null);
            final String err = error;
            final int n = creados.size();
            runOnUiThread(() -> {
                if (err != null) {
                    Toast.makeText(this, err, Toast.LENGTH_LONG).show();
                    extraer.setText("Extraer");
                    extraer.setEnabled(true);
                } else {
                    Toast.makeText(this, n + " archivos extraídos en \"" + dest.getName() + "\"", Toast.LENGTH_LONG).show();
                    finish();
                }
            });
        }).start();
    }
}
