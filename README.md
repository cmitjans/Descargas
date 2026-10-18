# Descargas

App de Android para ver y ordenar lo que hay en la carpeta **Descargas**, sin publicidad y **sin acceso a internet**.

**→ [Descargar la app (Releases)](https://github.com/cmitjans/Descargas/releases/latest)**

## Qué hace

- Lista todo lo que hay en Descargas, incluidas sus subcarpetas (por ejemplo las de Telegram), del más nuevo al más viejo.
- Filtra por tipo (PDF, imágenes, videos, audio, documentos, comprimidos, APK) y busca por nombre.
- Ordena por fecha, tamaño o nombre. "Más pesados" sirve para liberar espacio.
- Toca un archivo para abrirlo con la app que corresponda. Mantenlo presionado para abrir con otra app, compartir, renombrar, borrar o ver detalles.
- Muestra qué trae un ZIP y lo extrae en una carpeta al lado.

## Permisos

Lista completa en [`app/AndroidManifest.xml`](app/AndroidManifest.xml):

- **Acceso a todos los archivos**: Android no tiene un permiso solo para la carpeta Descargas; para ver los PDF, ZIP o APK que bajan otras apps, este es el único que existe.
- **Instalar apps desconocidas** (opcional): solo para poder abrir un APK desde aquí. Android igual pregunta antes de instalar.
- **No pide internet**: Android no le permite conectarse a nada, así que los archivos no pueden salir del teléfono.

Cuando abres o compartes un archivo, la otra app recibe permiso de lectura solo sobre ese archivo ([`Proveedor.java`](app/src/cl/cmitjans/descargas/Proveedor.java)).

## Cómo se compila

Sin Gradle, con las herramientas del SDK de Android: `ANDROID_HOME=/ruta/al/sdk ./build.sh`. GitHub Actions lo hace en cada cambio y publica el APK en Releases.
