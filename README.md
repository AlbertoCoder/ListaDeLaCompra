# Lista De La Compra

App Android (Java, SQLite nativo) para gestionar una lista de la compra de alimentación.

## Abrir y ejecutar
1. Android Studio (Koala o superior) > File > Open > carpeta `ListaDeLaCompra`.
2. Esperar a que termine el "Gradle Sync" (Android Studio descarga Gradle y genera el wrapper).
3. Run ▶ en un emulador o dispositivo (Android 8.0 / API 26 o superior).

## Base de datos (lista_compra.db)
- `secciones`  (ID INTEGER PK, seccion TEXT, supermercado TEXT)
- `productos`  (ID INTEGER PK, nombre_producto TEXT, comprar INTEGER 0/1,
                id_seccion INTEGER FK -> secciones.ID, ON DELETE SET NULL)

## Compilar el APK con GitHub Actions
El workflow `.github/workflows/build-apk.yml` compila el APK en los servidores de GitHub
cada vez que haces push. Para descargarlo: pestaña **Actions** > última ejecución >
sección **Artifacts** > `app-debug`.
