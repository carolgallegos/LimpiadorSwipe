# Limpiador Swipe 🧹

App para Android (11 o más nuevo) que limpia tu celular estilo Tinder:
**desliza ← para borrar y → para conservar.**

## Qué limpia
- 📷 Fotos · 🎬 Videos · 📱 Capturas de pantalla · 💬 Fotos y videos de WhatsApp
- 📁 Descargas y documentos (PDFs, APKs, archivos ZIP, etc.)

## Funciones
- Botón ↶ para deshacer la última decisión
- Antes de borrar ves una cuadrícula con todo lo marcado y puedes quitar lo que quieras
- Las fotos y videos van a la **papelera de la galería**, así que puedes recuperarlos durante unos 30 días
- Lo que decides conservar ya no se vuelve a mostrar (se puede reiniciar)
- Puedes empezar por lo más reciente, lo más antiguo o **lo más pesado** (para liberar espacio rápido)
- Contador del espacio que has liberado en total

---

## Cómo instalarla en tu celular

### 1. Instala Android Studio (gratis)
Descárgalo de https://developer.android.com/studio e instálalo con las opciones por defecto.

### 2. Abre el proyecto
1. Descomprime `LimpiadorSwipe.zip`.
2. En Android Studio: **File → Open** y elige la carpeta `LimpiadorSwipe`.
3. Espera a que termine de cargar (abajo dice "Gradle sync"). La primera vez tarda varios minutos porque descarga lo necesario.
   - Si te pide instalar un "SDK" o "Android 15 (API 35)", acepta.

### 3. Prepara tu celular
1. **Ajustes → Acerca del teléfono** → toca **"Número de compilación"** 7 veces hasta que diga que ya eres desarrollador.
   (En Xiaomi: "Versión de MIUI/HyperOS". En Samsung: Información de software → Número de compilación.)
2. **Ajustes → Sistema → Opciones de desarrollador** → activa **"Depuración USB"**.
3. Conecta el celular a la computadora con el cable USB y acepta el mensaje "¿Permitir depuración USB?".

### 4. Instálala
Arriba en Android Studio debe aparecer el nombre de tu celular. Presiona el botón verde **▶ Run**.
La app se instala y se abre sola en tu celular. Después ya puedes desconectar el cable.

**Sin cable:** menú **Build → Build App Bundle(s) / APK(s) → Build APK(s)**. Cuando termine, da clic en "locate",
pásate el archivo `app-debug.apk` al celular (por Drive, WhatsApp o correo), ábrelo y permite
"instalar apps desconocidas".

---

## Permisos que pide
- **Fotos y videos:** elige "Permitir todo". Si eliges "Seleccionar fotos", solo verá las que escojas.
- **Acceso a todos los archivos:** solo se necesita para Descargas y documentos. La app abre el ajuste por ti.

La app no usa internet: todo se queda en tu celular.
