package cl.cmitjans.descargas;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * Entrega un archivo a otra app (un visor de PDF, la galería, el instalador) cuando tú eliges abrirlo
 * o compartirlo. Solo lectura, solo archivos del almacenamiento compartido, y la otra app recibe
 * permiso únicamente sobre ese archivo.
 */
public class Proveedor extends ContentProvider {
    static final String AUTORIDAD = "cl.cmitjans.descargas.archivos";

    static Uri uri(File f) {
        return new Uri.Builder().scheme("content").authority(AUTORIDAD).encodedPath(Uri.encode(f.getAbsolutePath(), "/")).build();
    }

    private static File archivo(Uri u) throws FileNotFoundException {
        File f = new File(u.getPath());
        try {
            String raiz = Environment.getExternalStorageDirectory().getCanonicalPath();
            String ruta = f.getCanonicalPath();
            if (!ruta.startsWith(raiz + "/") || !f.isFile()) throw new FileNotFoundException(u.toString());
            return new File(ruta);
        } catch (IOException e) {
            throw new FileNotFoundException(u.toString());
        }
    }

    @Override public boolean onCreate() { return true; }

    @Override
    public ParcelFileDescriptor openFile(Uri u, String modo) throws FileNotFoundException {
        if (!"r".equals(modo)) throw new SecurityException("Solo lectura");
        return ParcelFileDescriptor.open(archivo(u), ParcelFileDescriptor.MODE_READ_ONLY);
    }

    @Override
    public Cursor query(Uri u, String[] columnas, String sel, String[] args, String orden) {
        File f;
        try { f = archivo(u); } catch (FileNotFoundException e) { return null; }
        if (columnas == null) columnas = new String[]{OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE};
        MatrixCursor c = new MatrixCursor(columnas, 1);
        Object[] fila = new Object[columnas.length];
        for (int i = 0; i < columnas.length; i++) {
            if (OpenableColumns.DISPLAY_NAME.equals(columnas[i])) fila[i] = f.getName();
            else if (OpenableColumns.SIZE.equals(columnas[i])) fila[i] = f.length();
        }
        c.addRow(fila);
        return c;
    }

    @Override
    public String getType(Uri u) {
        try { return Tipos.mime(archivo(u)); } catch (FileNotFoundException e) { return null; }
    }

    @Override public Uri insert(Uri u, ContentValues v) { throw new UnsupportedOperationException(); }
    @Override public int delete(Uri u, String s, String[] a) { throw new UnsupportedOperationException(); }
    @Override public int update(Uri u, ContentValues v, String s, String[] a) { throw new UnsupportedOperationException(); }
}
