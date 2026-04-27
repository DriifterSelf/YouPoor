# YouPoor App - Android

App de música para descargar y reproducir audio de YouTube/SoundCloud en formato FLAC.

## Arquitectura

### Estructura de carpetas
```
app/src/main/java/com/voidfilio/youpoor/
├── AppModule.kt              # Inyección de dependencias
├── Config.kt                 # Configuración global
├── MainActivity.kt           # Actividad principal
├── audio/
│   └── AudioPlayer.kt        # Wrapper de ExoPlayer
├── data/
│   ├── api/
│   │   └── YouPoorApi.kt     # Interfaz Retrofit
│   ├── db/
│   │   ├── YouPoorDatabase.kt # Room database
│   │   └── Daos.kt           # DAOs
│   ├── downloader/
│   │   └── FileDownloader.kt # Descarga de archivos
│   ├── models/
│   │   └── Music.kt          # Data classes
│   └── repository/
│       └── MusicRepository.kt # Business logic
├── ui/
│   ├── components/
│   │   └── AudioPlayerComposable.kt
│   ├── screens/
│   │   ├── SearchScreen.kt   # Búsqueda de música
│   │   └── PlayerScreen.kt   # Reproductor
│   └── theme/
│       ├── Color.kt
│       ├── Type.kt
│       └── Theme.kt
└── viewmodel/
    ├── SearchViewModel.kt
    ├── PlayerViewModel.kt
    └── DownloadViewModel.kt
```

### Stack Tecnológico
- **UI**: Jetpack Compose
- **Networking**: Retrofit + OkHttp
- **Audio**: ExoPlayer (Media3)
- **Database**: Room
- **Inyección**: Manual (AppModule)

## Configuración

### 1. Backend URL
Edita `Config.kt`:
```kotlin
const val BACKEND_URL = "http://tu-servidor:8000"
```

Para emulador local: `http://10.0.2.2:8000`
Para física en red: `http://192.168.x.x:8000`

### 2. Permisos
Ya están configurados en `AndroidManifest.xml`:
- `INTERNET` - Conectar al backend
- `READ_EXTERNAL_STORAGE` - Leer descargas
- `WRITE_EXTERNAL_STORAGE` - Guardar descargas

### 3. API REST Expected
El backend debe tener estos endpoints:
```
POST   /api/search?q=<query>&platform=youtube
POST   /api/download
GET    /api/download/{downloadId}
GET    /api/history
```

## Uso

### Pantalla de Búsqueda (Tab 0)
- Escribe una consulta
- Ve resultados en tiempo real
- Toca un resultado para descargar

### Pantalla de Reproductor (Tab 1)
- Ve tus descargas
- Toca una para reproducir
- Controla play/pause/siguiente

## Características Implementadas

✅ Búsqueda en tiempo real desde YouTube
✅ Descarga y conversión a FLAC
✅ Reproducción local
✅ Almacenamiento en dispositivo
✅ Historial de descargas
✅ Navegación con bottom bar

## Características Futuras

- [ ] Equalizer (DSP)
- [ ] Playlist
- [ ] Búsqueda en SoundCloud
- [ ] Caché inteligente
- [ ] Sincronización de cloud

## Troubleshooting

**Error: Connection refused**
- Verifica que el backend esté corriendo: `python main.py`
- Verifica la IP en `Config.kt`
- Desde emulador: usa `10.0.2.2` para localhost

**Error: Permission denied (almacenamiento)**
- Android 10+: Solicita permisos en runtime si es necesario
- O usa `getExternalFilesDir()` (permiso automático)

**El player no reproduce**
- Verifica que el archivo FLAC se descargó correctamente
- FFmpeg en el backend convirtió correctamente a FLAC
- ExoPlayer soporta FLAC natively desde Media3

## Build & Run

```bash
# Build
./gradlew build

# Run
./gradlew installDebug

# O desde Android Studio: Run > Run 'app'
```

Min SDK: Android 10 (API 29)
Target SDK: Android 15 (API 36)
