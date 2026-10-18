package cl.cmitjans.descargas;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;

/** Colores y piezas de interfaz compartidas (la app se dibuja sin archivos de diseño). */
final class Ui {
    static final int BG = Color.rgb(14, 16, 20);
    static final int CARD = Color.rgb(28, 32, 39);
    static final int CHIP = Color.rgb(40, 45, 54);
    static final int TEXT = Color.rgb(231, 235, 240);
    static final int MUTED = Color.rgb(154, 165, 179);
    static final int ACCENT = Color.rgb(110, 162, 239);
    static final int DANGER = Color.rgb(232, 98, 86);

    private Ui() {}

    static int dp(Context c, float v) {
        return Math.round(v * c.getResources().getDisplayMetrics().density);
    }

    static TextView text(Context c, String s, int sp, int color) {
        TextView v = new TextView(c);
        v.setText(s);
        v.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        v.setTextColor(color);
        return v;
    }

    static TextView title(Context c, String s) {
        TextView t = text(c, s, 26, TEXT);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        return t;
    }

    static GradientDrawable round(int color, float radiusPx) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(radiusPx);
        return d;
    }

    static LinearLayout card(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setPadding(dp(c, 18), dp(c, 16), dp(c, 18), dp(c, 16));
        l.setBackground(round(CARD, dp(c, 18)));
        return l;
    }

    static Button button(Context c, String s, boolean primary) {
        Button b = new Button(c);
        b.setText(s);
        b.setAllCaps(false);
        b.setTextColor(primary ? BG : TEXT);
        b.setBackground(round(primary ? ACCENT : CHIP, dp(c, 22)));
        b.setPadding(dp(c, 18), 0, dp(c, 18), 0);
        b.setGravity(Gravity.CENTER);
        return b;
    }

    /** Ícono cuadrado con la extensión del archivo, del color de su tipo. */
    static TextView badge(Context c, File f) {
        String ext = f.isDirectory() ? "DIR" : Tipos.extension(f).toUpperCase();
        if (ext.isEmpty()) ext = "?";
        if (ext.length() > 4) ext = ext.substring(0, 4);
        TextView b = text(c, ext, 11, Color.WHITE);
        b.setTypeface(Typeface.DEFAULT_BOLD);
        b.setGravity(Gravity.CENTER);
        int col = f.isDirectory() ? Color.rgb(90, 100, 115) : Tipos.color(Tipos.tipo(f));
        b.setBackground(round(col, dp(c, 10)));
        return b;
    }

    /** Abre el archivo con la app que corresponda; si no hay ninguna, lo dice. */
    static void abrir(Activity a, File f, boolean elegir) {
        Intent i = new Intent(Intent.ACTION_VIEW)
                .setDataAndType(Proveedor.uri(f), Tipos.mime(f))
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            a.startActivity(elegir ? Intent.createChooser(i, "Abrir con") : i);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(a, "No hay ninguna app instalada que abra este tipo de archivo", Toast.LENGTH_LONG).show();
        }
    }

    static void compartir(Activity a, File f) {
        Intent i = new Intent(Intent.ACTION_SEND)
                .setType(Tipos.mime(f))
                .putExtra(Intent.EXTRA_STREAM, Proveedor.uri(f))
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        a.startActivity(Intent.createChooser(i, "Compartir " + f.getName()));
    }
}
