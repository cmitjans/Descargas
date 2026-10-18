package cl.cmitjans.descargas;

import android.graphics.Color;
import android.webkit.MimeTypeMap;

import java.io.File;
import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;

/** Clasifica archivos por su extensión y da formato a tamaños y fechas. */
final class Tipos {
    static final String TODOS = "Todos";
    static final String PDF = "PDF";
    static final String IMAGEN = "Imágenes";
    static final String VIDEO = "Videos";
    static final String AUDIO = "Audio";
    static final String DOC = "Documentos";
    static final String ZIP = "Comprimidos";
    static final String APK = "Apps (APK)";
    static final String OTRO = "Otros";
    static final String[] FILTROS = {TODOS, PDF, IMAGEN, VIDEO, AUDIO, DOC, ZIP, APK, OTRO};

    private Tipos() {}

    static String extension(File f) {
        String n = f.getName();
        int i = n.lastIndexOf('.');
        return i < 0 ? "" : n.substring(i + 1).toLowerCase(Locale.ROOT);
    }

    static String tipo(File f) {
        switch (extension(f)) {
            case "pdf": return PDF;
            case "jpg": case "jpeg": case "png": case "gif": case "webp": case "heic": case "heif": case "bmp": case "svg":
                return IMAGEN;
            case "mp4": case "mkv": case "webm": case "mov": case "3gp": case "avi":
                return VIDEO;
            case "mp3": case "m4a": case "ogg": case "opus": case "wav": case "flac": case "aac":
                return AUDIO;
            case "doc": case "docx": case "xls": case "xlsx": case "ppt": case "pptx": case "odt": case "ods": case "odp":
            case "txt": case "csv": case "rtf": case "epub": case "html": case "htm": case "json": case "xml": case "md":
                return DOC;
            case "zip": case "rar": case "7z": case "tar": case "gz": case "tgz": case "xz": case "bz2":
                return ZIP;
            case "apk": case "apks": case "xapk":
                return APK;
            default: return OTRO;
        }
    }

    static int color(String tipo) {
        switch (tipo) {
            case PDF: return Color.rgb(232, 98, 86);
            case IMAGEN: return Color.rgb(86, 178, 120);
            case VIDEO: return Color.rgb(176, 112, 224);
            case AUDIO: return Color.rgb(230, 160, 60);
            case DOC: return Color.rgb(110, 162, 239);
            case ZIP: return Color.rgb(200, 170, 110);
            case APK: return Color.rgb(120, 200, 200);
            default: return Color.rgb(140, 150, 165);
        }
    }

    static String mime(File f) {
        String ext = extension(f);
        if (ext.equals("apk")) return "application/vnd.android.package-archive";
        String m = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext);
        return m != null ? m : "application/octet-stream";
    }

    static String tamano(long b) {
        if (b < 1024) return b + " B";
        if (b < 1024 * 1024) return String.format(Locale.ROOT, "%.0f KB", b / 1024.0);
        if (b < 1024L * 1024 * 1024) return String.format(Locale.ROOT, "%.1f MB", b / (1024.0 * 1024));
        return String.format(Locale.ROOT, "%.2f GB", b / (1024.0 * 1024 * 1024));
    }

    static String fecha(long ms) {
        long dias = (System.currentTimeMillis() - ms) / 86_400_000L;
        if (dias < 1) return "hoy " + DateFormat.getTimeInstance(DateFormat.SHORT).format(new Date(ms));
        if (dias < 2) return "ayer";
        if (dias < 7) return "hace " + dias + " días";
        return DateFormat.getDateInstance(DateFormat.MEDIUM).format(new Date(ms));
    }
}
