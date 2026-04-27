# YouPoor Backend

FastAPI backend para descargar y convertir música de YouTube/SoundCloud a FLAC.

## Setup

1. **Activar venv:**
   ```bash
   source venv/Scripts/activate
   ```

2. **Instalar dependencias (si no está hecho):**
   ```bash
   pip install -r requirements.txt
   ```

3. **Instalar ffmpeg (si no lo tienes):**
   - **Windows:** `choco install ffmpeg` o descarga desde https://ffmpeg.org
   - **Linux:** `sudo apt install ffmpeg`
   - **macOS:** `brew install ffmpeg`

## Ejecutar

```bash
source venv/Scripts/activate
python main.py
```

Server corre en `http://localhost:8000`

## API Endpoints

### 🔍 Búsqueda
```
POST /api/search?q=<query>&platform=youtube
→ [{id, title, artist, duration, thumbnail, platform}, ...]
```

### ⬇️ Descargar
```
POST /api/download
{
  "url": "https://youtube.com/watch?v=...",
  "platform": "youtube"
}
→ {download_id, file_url, metadata}
```

### ▶️ Obtener FLAC
```
GET /api/download/{download_id}
→ FLAC file stream
```

### 📋 Historial
```
GET /api/history
→ [{title, artist, platform, downloaded_at}, ...]
```

## Estructura

```
backend/
├── main.py           # FastAPI app con todos los endpoints
├── venv/             # Virtual environment
├── downloads/        # Archivos FLAC descargados
├── history.json      # Historial de descargas
└── requirements.txt  # Dependencias Python
```

## Troubleshooting

**Error: FFmpeg no encontrado**
- Instala ffmpeg (ver arriba)
- O asegúrate que esté en PATH

**Error: yt-dlp outdated**
```bash
pip install --upgrade yt-dlp
```

**Error: Puerto 8000 en uso**
```bash
python main.py --port 8080
```
