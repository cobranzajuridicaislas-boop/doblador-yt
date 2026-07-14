# Dobla YT — App de doblaje ingles a espanol (100% gratis, sin PC)

## Que hace
1. Pegas un link de YouTube y extrae/reproduce el audio (libreria NewPipeExtractor, sin API de pago).
2. Al tocar "Iniciar doblaje", la app escucha por el microfono, transcribe el ingles
   (SpeechRecognizer nativo de Android), lo traduce al espanol (ML Kit Translate de
   Google, offline) y lo lee en voz alta (TextToSpeech nativo de Android).

## Limitacion importante
El reconocimiento de voz gratuito de Android escucha el microfono, no lee archivos
de audio directamente. Por eso, para doblar un video de YouTube, tienes que
reproducirlo por el altavoz (con "Extraer y reproducir") y luego tocar "Iniciar
doblaje" para que el microfono capte ese audio. Funciona mejor en un cuarto
silencioso y a volumen medio. Para tus propios audios/videos, el mismo boton de
doblaje sirve: reproduce el archivo por el altavoz y el proceso es igual.

## Aviso legal
Extraer audio de YouTube con NewPipeExtractor es para **uso personal**. No
redistribuyas ni publiques el contenido extraido; eso si violaria los Terminos
de Servicio de YouTube.

## Como compilar el APK sin PC (usando tu Android)

### Paso 1: Crea una cuenta y repositorio en GitHub
1. Instala la app de GitHub o usa el navegador de tu telefono, entra a github.com
   y crea una cuenta si no tienes.
2. Toca "New repository", nombralo por ejemplo `doblador-yt`, ponlo como
   **Private** o Public (a tu gusto), y crea el repo (sin agregar README, ya
   tenemos uno).

### Paso 2: Sube estos archivos
Tienes dos formas, ambas desde el telefono:

**Opcion A (mas facil): subir el ZIP y que GitHub lo extraiga**
GitHub no descomprime automaticamente al subir, asi que mejor usa la opcion B,
o usa una app como "Working Directory" / "MT Manager" para descomprimir el ZIP
en tu almacenamiento y luego subir carpeta por carpeta desde el navegador
(github.com > tu repo > "Add file" > "Upload files", arrastrando cada carpeta).

**Opcion B: Termux + git (recomendado, mas confiable)**
1. Instala **Termux** (desde F-Droid, no la version vieja de Play Store).
2. En Termux:
   ```
   pkg install git -y
   ```
3. Copia la carpeta descomprimida del proyecto a la memoria de Termux, o clona
   tu repo vacio y copia los archivos dentro:
   ```
   git clone https://github.com/TU_USUARIO/doblador-yt.git
   cd doblador-yt
   # copia aqui todos los archivos del ZIP que te compartí
   git add .
   git commit -m "primer commit"
   git push
   ```
   Te pedira tu usuario y un "Personal Access Token" de GitHub como contraseña
   (lo generas en GitHub: Settings > Developer settings > Personal access tokens).

### Paso 3: Deja que GitHub compile el APK
En cuanto hagas el `push`, el archivo `.github/workflows/build.yml` se activa
solo. Para verlo:
1. Ve a tu repo en github.com (o la app) > pestaña "Actions".
2. Veras un proceso llamado "Compilar APK" corriendo (tarda unos 3-5 minutos).
3. Cuando termine (palomita verde), entra a ese resultado y baja hasta
   "Artifacts" > descarga `DobladorYT-debug-apk`.
4. Es un .zip que contiene el `app-debug.apk`. Descomprimelo e instala el APK
   en tu telefono (activa "Instalar apps de origenes desconocidos" si te lo pide).

## Estructura del proyecto
```
DobladorYT/
  app/
    build.gradle
    src/main/AndroidManifest.xml
    src/main/java/com/jc/dobladoryt/MainActivity.java
    src/main/java/com/jc/dobladoryt/YoutubeAudioHelper.java
    src/main/java/com/jc/dobladoryt/DubbingEngine.java
    src/main/res/layout/activity_main.xml
    src/main/res/values/strings.xml
  build.gradle
  settings.gradle
  gradle.properties
  .github/workflows/build.yml
```

## Proximos pasos posibles
- Cambiar la voz del TextToSpeech por una mas natural (requiere instalar un motor
  de voz alternativo, ej. desde Ajustes > Accesibilidad > Salida de texto a voz).
- Agregar boton para elegir el idioma de traduccion destino.
- Guardar el audio doblado como archivo en vez de solo reproducirlo en vivo.
