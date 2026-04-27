from fastapi import FastAPI, HTTPException, WebSocket
from fastapi.responses import FileResponse
from pydantic import BaseModel
from typing import List, Optional
import yt_dlp
import subprocess
import os
import json
from pathlib import Path
from datetime import datetime
import uuid
import time

app = FastAPI(title="YouPoor Backend")

# Storage for temporary files
DOWNLOADS_DIR = Path("./downloads")
DOWNLOADS_DIR.mkdir(exist_ok=True)

HISTORY_FILE = Path("./history.json")
DOWNLOADS_MANIFEST = Path("./downloads_manifest.json")
COOKIES_FILE = Path("./cookies.txt")

# Search cache
SEARCH_CACHE = {}

# Models
class SearchResult(BaseModel):
    id: str
    title: str
    artist: str
    duration: int
    thumbnail: str
    platform: str

class DownloadRequest(BaseModel):
    url: str
    platform: str

class DownloadResponse(BaseModel):
    download_id: str
    file_url: str
    filename: str
    metadata: dict

class HistoryEntry(BaseModel):
    title: str
    artist: str
    platform: str
    downloaded_at: str

# Utility functions
def get_ydl_opts(for_search: bool = False, player_client: str = "android") -> dict:
    """Build yt-dlp options with cookie support"""
    opts = {
        'quiet': True,
        'no_warnings': True,
        'socket_timeout': 30,
        'no_playlist': True,
        'js_runtimes': {'node': {}},
    }

    if for_search:
        opts.update({
            'extract_flat': 'in_playlist',  # Get metadata without fetching all details
            'playlistend': 10,
            'skip_download': True,
        })
    else:
        opts['extract_flat'] = False

    # Try to use cookies from file first
    if COOKIES_FILE.exists():
        opts['cookiefile'] = str(COOKIES_FILE)
        print(f"🍪 Using cookies from file: {COOKIES_FILE}")
    else:
        # Try to use Edge browser cookies directly
        try:
            opts['cookiesfrombrowser'] = ('edge', None)
            print(f"🍪 Using cookies from Edge browser")
        except:
            # Fallback: try different player clients
            opts['extractor_args'] = {'youtube': {'player_client': player_client}}
            print(f"⚙️  Trying player_client: {player_client}")

    return opts

def extract_youtube_with_retry(url: str, download: bool, for_search: bool = False, extra_opts: dict = None):
    """Try to extract YouTube info with multiple fallback strategies"""
    player_clients = ['android', 'ios', 'web', 'web_creator', 'tv']
    retry_start = time.perf_counter()
    extra_opts = extra_opts or {}

    # First try with cookies from file
    if COOKIES_FILE.exists():
        try:
            file_start = time.perf_counter()
            print(f"  🍪 [+0ms] Intentando con cookies del archivo...")
            ydl_opts = get_ydl_opts(for_search=for_search)
            ydl_opts.update(extra_opts)
            print(f"       yt-dlp opts: extract_flat={ydl_opts.get('extract_flat')}, has_cookiefile={bool(ydl_opts.get('cookiefile'))}, format={ydl_opts.get('format')}, outtmpl={ydl_opts.get('outtmpl')}")

            # Add progress hook to see what yt-dlp is doing
            def progress_hook(d):
                elapsed = (time.perf_counter() - file_start) * 1000
                if d['status'] == 'downloading':
                    print(f"       [+{elapsed:.0f}ms] ⬇️  Downloading: {d.get('_default_template', d.get('info_dict', {}).get('id', '?'))}")
                elif d['status'] == 'processing':
                    print(f"       [+{elapsed:.0f}ms] ⚙️  Processing...")
                elif d['status'] == 'finished':
                    print(f"       [+{elapsed:.0f}ms] ✓ Download finished")

            init_start = time.perf_counter()
            print(f"       [+0ms] Inicializando YoutubeDL...")
            with yt_dlp.YoutubeDL(ydl_opts) as ydl:
                init_time = (time.perf_counter() - init_start) * 1000
                print(f"       [+{init_time:.0f}ms] YoutubeDL inicializado")

                extract_start = time.perf_counter()
                print(f"       [+{init_time:.0f}ms] Extrayendo info...")
                result = ydl.extract_info(url, download=download)
                extract_time = (time.perf_counter() - extract_start) * 1000
                print(f"       [+{extract_time:.0f}ms] Info extraída, procesando...")

                file_time = (time.perf_counter() - file_start) * 1000
                print(f"  ✅ [+{file_time:.0f}ms] Éxito con cookies del archivo (init:{init_time:.0f}ms + extract:{extract_time:.0f}ms)")
                return result
        except Exception as e:
            file_time = (time.perf_counter() - file_start) * 1000
            print(f"  ❌ [+{file_time:.0f}ms] Falló con archivo: {str(e)[:100]}")

    # Try with Edge browser cookies
    try:
        edge_start = time.perf_counter()
        print(f"  🍪 [+0ms] Intentando con cookies de Edge...")
        ydl_opts = {
            'quiet': True,
            'no_warnings': True,
            'socket_timeout': 30,
            'cookiesfrombrowser': ('edge', None),
            'no_playlist': True,
            'js_runtimes': {'node': {}},
        }
        if for_search:
            ydl_opts.update({
                'extract_flat': 'in_playlist',
                'playlistend': 10,
                'skip_download': True,
            })
        else:
            ydl_opts['extract_flat'] = False
        ydl_opts.update(extra_opts)

        with yt_dlp.YoutubeDL(ydl_opts) as ydl:
            result = ydl.extract_info(url, download=download)
            edge_time = (time.perf_counter() - edge_start) * 1000
            print(f"  ✅ [+{edge_time:.0f}ms] Éxito con cookies de Edge")
            return result
    except Exception as e:
        edge_time = (time.perf_counter() - edge_start) * 1000
        print(f"  ❌ [+{edge_time:.0f}ms] Falló con Edge: {str(e)[:100]}")

    # Try different player clients
    for client in player_clients:
        try:
            client_start = time.perf_counter()
            print(f"  🔄 [+0ms] Reintentando con player_client: {client}")
            ydl_opts = get_ydl_opts(for_search=for_search, player_client=client)
            ydl_opts.update(extra_opts)

            with yt_dlp.YoutubeDL(ydl_opts) as ydl:
                result = ydl.extract_info(url, download=download)
                client_time = (time.perf_counter() - client_start) * 1000
                print(f"  ✅ [+{client_time:.0f}ms] Éxito con player_client: {client}")
                return result
        except Exception as e:
            client_time = (time.perf_counter() - client_start) * 1000
            error_str = str(e)
            error_lower = error_str.lower()
            # Fall through to the next player_client for transient/format errors;
            # different clients return different format catalogs.
            if "bot" in error_lower or "format is not available" in error_lower or "requested format" in error_lower:
                print(f"  ⚠️  [+{client_time:.0f}ms] {client} falló ({error_str[:80]}), intentando siguiente...")
                continue
            else:
                raise

    # All attempts failed
    total_retry_time = (time.perf_counter() - retry_start) * 1000
    raise Exception(f"YouTube bot detection bloqueó todos los intentos en {total_retry_time:.0f}ms. Ver YOUTUBE_AUTH.md para instrucciones de configuración.")

def load_history():
    if HISTORY_FILE.exists():
        with open(HISTORY_FILE, 'r') as f:
            return json.load(f)
    return []

def save_history(history):
    with open(HISTORY_FILE, 'w') as f:
        json.dump(history, f, indent=2)

def load_manifest():
    if DOWNLOADS_MANIFEST.exists():
        with open(DOWNLOADS_MANIFEST, 'r') as f:
            return json.load(f)
    return {}

def save_manifest(manifest):
    with open(DOWNLOADS_MANIFEST, 'w') as f:
        json.dump(manifest, f, indent=2)

def get_youtube_info(url: str) -> dict:
    """Extract metadata from YouTube URL"""
    try:
        # Use search mode for faster metadata extraction
        info = extract_youtube_with_retry(url, download=False, for_search=True)
        return {
            'id': info.get('id'),
            'title': info.get('title'),
            'duration': info.get('duration'),
            'thumbnail': info.get('thumbnail'),
        }
    except Exception as e:
        # Fallback: return minimal info
        print(f"⚠️  Metadata extraction failed: {str(e)[:100]}, using fallback")
        return {
            'id': url.split('/')[-1].split('?')[0],
            'title': 'Unknown Title',
            'duration': 0,
            'thumbnail': '',
        }

def download_and_convert(url: str, platform: str) -> tuple:
    """Download audio from YouTube using yt-dlp (WebM format, no conversion)"""
    try:
        download_id = str(uuid.uuid4())
        info = {}

        if 'youtube.com' in url or 'youtu.be' in url:
            yt_url = url
        else:
            yt_url = f"https://www.youtube.com/watch?v={url}"

        print(f"📥 Descargando: {yt_url}")

        download_opts = {
            'format': 'bestaudio/best',
            'outtmpl': str(DOWNLOADS_DIR / f"{download_id}.%(ext)s"),
            'extractor_args': {'youtube': {'player_client': ['android', 'web']}},
        }

        info_dict = extract_youtube_with_retry(yt_url, download=True, for_search=False, extra_opts=download_opts)

        info = {
            'id': info_dict.get('id'),
            'title': info_dict.get('title', 'Unknown'),
            'author': info_dict.get('uploader', 'Unknown'),
            'duration': info_dict.get('duration', 0),
            'thumbnail_url': info_dict.get('thumbnail', ''),
        }

        actual_file = list(DOWNLOADS_DIR.glob(f"{download_id}.*"))[0]
        print(f"✅ Descargado: {actual_file.name}")

        # Update manifest
        manifest = load_manifest()
        manifest[download_id] = {
            'title': info.get('title', 'Unknown'),
            'artist': info.get('author', 'Unknown'),
            'duration': info.get('duration', 0),
            'thumbnail': info.get('thumbnail_url', ''),
            'platform': platform,
            'filename': actual_file.name,
            'downloaded_at': datetime.now().isoformat(),
        }
        save_manifest(manifest)

        return actual_file, info

    except Exception as e:
        print(f"❌ Error de descarga: {str(e)}")
        raise HTTPException(status_code=400, detail=f"Download failed: {str(e)}")

# Routes
@app.get("/")
async def root():
    return {"status": "YouPoor Backend Running"}

@app.get("/api/status")
async def status():
    """Check backend status and YouTube auth"""
    has_cookies = COOKIES_FILE.exists()
    return {
        "status": "running",
        "youtube_auth": "cookies" if has_cookies else "player_client (android)",
        "cookies_file_exists": has_cookies,
        "downloads_dir": str(DOWNLOADS_DIR),
    }

@app.websocket("/ws/search")
async def websocket_search(websocket: WebSocket):
    """WebSocket endpoint for live search streaming"""
    await websocket.accept()
    print(f"\n🔌 WebSocket client connected")

    try:
        while True:
            # Wait for search query
            data = await websocket.receive_json()
            query = data.get('q', '')
            platform = data.get('platform', 'youtube')

            if not query:
                await websocket.send_json({"error": "Empty query"})
                continue

            print(f"\n{'='*60}")
            print(f"🔍 [WS] Search: {query}")

            if platform == "youtube":
                search_query = f"ytsearch{10}:{query}"
                search_start = time.perf_counter()

                try:
                    # Send start message
                    await websocket.send_json({
                        "status": "searching",
                        "message": f"Buscando: {query}"
                    })

                    # Get YouTube results
                    results = extract_youtube_with_retry(search_query, download=False, for_search=True)
                    entries = results.get('entries', [])

                    print(f"✅ Got {len(entries)} entries, sending to client...")

                    # Stream each result to client
                    for i, entry in enumerate(entries):
                        entry_start = time.perf_counter()

                        # Extract thumbnail
                        thumbnail = ''
                        thumbnails_list = entry.get('thumbnails', [])
                        if thumbnails_list:
                            if isinstance(thumbnails_list, list) and len(thumbnails_list) > 0:
                                for thumb in reversed(thumbnails_list):
                                    if isinstance(thumb, dict) and 'url' in thumb:
                                        thumbnail = thumb.get('url', '')
                                        break
                            elif isinstance(thumbnails_list, dict):
                                thumbnail = thumbnails_list.get('url', '')

                        if thumbnail and thumbnail.startswith('http://'):
                            thumbnail = thumbnail.replace('http://', 'https://', 1)

                        result = {
                            "id": entry.get('id'),
                            "title": entry.get('title', 'Unknown'),
                            "artist": entry.get('uploader', 'Unknown'),
                            "duration": entry.get('duration', 0),
                            "thumbnail": thumbnail,
                            "platform": platform,
                            "index": i + 1,
                            "total": len(entries)
                        }

                        # Send result immediately
                        await websocket.send_json({
                            "status": "result",
                            "data": result
                        })

                        entry_time = (time.perf_counter() - entry_start) * 1000
                        print(f"   [{i+1}] {result['title'][:50]} (sent in {entry_time:.1f}ms)")

                    # Send complete message
                    total_time = (time.perf_counter() - search_start) * 1000
                    await websocket.send_json({
                        "status": "complete",
                        "total": len(entries),
                        "time_ms": total_time
                    })

                    print(f"⏱️  Total: {total_time:.0f}ms")
                    print(f"{'='*60}\n")

                except Exception as e:
                    print(f"❌ Error: {str(e)}")
                    await websocket.send_json({
                        "status": "error",
                        "error": str(e)
                    })

    except Exception as e:
        print(f"🔌 WebSocket error: {str(e)}")
    finally:
        print(f"🔌 WebSocket client disconnected")

@app.get("/api/search", response_model=List[SearchResult])
async def search(q: str, platform: str = "youtube"):
    """Search for music"""
    if platform == "youtube":
        search_start = time.perf_counter()
        search_query = f"ytsearch{10}:{q}"

        try:
            print(f"\n{'='*60}")
            print(f"🔍 [0ms] Starting search: {search_query}")

            extract_start = time.perf_counter()
            results = extract_youtube_with_retry(search_query, download=False, for_search=True)
            extract_time = (time.perf_counter() - extract_start) * 1000
            entries = results.get('entries', [])
            print(f"✅ [+{extract_time:.0f}ms] Extracted {len(entries)} entries from YouTube")
            print(f"   Results keys: {list(results.keys())}")

            parse_start = time.perf_counter()
            search_results = []
            for i, entry in enumerate(entries):
                entry_start = time.perf_counter()

                entry_id = entry.get('id')
                title = entry.get('title', 'Unknown')
                artist = entry.get('uploader', 'Unknown')
                duration = entry.get('duration', 0)

                # Extract thumbnail from 'thumbnails' array (plural, not singular)
                thumbnail = ''
                thumbnails_list = entry.get('thumbnails', [])
                if thumbnails_list:
                    # Get the last/highest quality thumbnail
                    if isinstance(thumbnails_list, list) and len(thumbnails_list) > 0:
                        # Try to get the last one (usually highest quality)
                        for thumb in reversed(thumbnails_list):
                            if isinstance(thumb, dict) and 'url' in thumb:
                                thumbnail = thumb.get('url', '')
                                break
                        # If no 'url' key, try direct string
                        if not thumbnail and isinstance(thumbnails_list[-1], str):
                            thumbnail = thumbnails_list[-1]
                    elif isinstance(thumbnails_list, dict):
                        thumbnail = thumbnails_list.get('url', '')

                print(f"\n   [{i+1}] {title}")
                print(f"       ID: {entry_id}")
                print(f"       Artist: {artist}")
                print(f"       Duration: {duration}s")
                print(f"       Thumbnails (raw): {thumbnails_list}")
                print(f"       Thumbnail extracted: {thumbnail[:80] if thumbnail else 'EMPTY'}")
                print(f"       Entry keys: {list(entry.keys())}")

                if thumbnail and thumbnail.startswith('http://'):
                    thumbnail = thumbnail.replace('http://', 'https://', 1)
                    print(f"       Thumbnail (fixed): {thumbnail[:80]}")

                result = SearchResult(
                    id=entry_id,
                    title=title,
                    artist=artist,
                    duration=duration,
                    thumbnail=thumbnail,
                    platform=platform,
                )
                search_results.append(result)

                entry_time = (time.perf_counter() - entry_start) * 1000
                print(f"       Parsed in {entry_time:.1f}ms")

            parse_time = (time.perf_counter() - parse_start) * 1000
            print(f"\n📤 [+{parse_time:.0f}ms] Parsed {len(search_results)} results total")
            print(f"   First result thumbnail: {search_results[0].thumbnail[:80] if search_results else 'N/A'}")

            total_time = (time.perf_counter() - search_start) * 1000
            print(f"⏱️  Total time: {total_time:.0f}ms")
            print(f"{'='*60}\n")
            return search_results
        except Exception as e:
            error_msg = str(e)
            print(f"❌ Search Error: {error_msg}")
            detail = error_msg
            if "bot" in error_msg.lower():
                detail = f"{error_msg}. See YOUTUBE_AUTH.md for cookie setup instructions."
            raise HTTPException(status_code=400, detail=detail)

    raise HTTPException(status_code=400, detail="Platform not supported")

@app.post("/api/download", response_model=DownloadResponse)
async def download(req: DownloadRequest):
    """Download and convert to FLAC"""
    print(f"\n📥 Download request received: url='{req.url}', platform='{req.platform}'")

    # Handle both URL and ID formats
    url = req.url
    if not url.startswith('http'):
        # If it's just an ID, construct the full URL
        url = f"https://www.youtube.com/watch?v={url}"
        print(f"🔗 Constructing full URL: {url}")

    filepath, info = download_and_convert(url, req.platform)

    # Add to history
    history = load_history()
    history.append({
        "title": info.get('title'),
        "artist": info.get('uploader', 'Unknown'),
        "platform": req.platform,
        "downloaded_at": datetime.now().isoformat()
    })
    save_history(history)

    download_id = filepath.stem

    return DownloadResponse(
        download_id=download_id,
        file_url=f"/api/download/{download_id}",
        filename=filepath.name,
        metadata={
            "title": info.get('title'),
            "duration": info.get('duration'),
            "thumbnail": info.get('thumbnail_url', ''),
            "artist": info.get('author', 'Unknown'),
        }
    )

@app.get("/api/download/{download_id}")
async def get_download(download_id: str):
    """Stream audio file (WebM)"""
    # Try to find the file with any audio extension
    potential_files = list(DOWNLOADS_DIR.glob(f"{download_id}.*"))
    if not potential_files:
        raise HTTPException(status_code=404, detail="File not found")

    filepath = potential_files[0]
    media_type = "audio/webm"
    if filepath.suffix == '.mp4':
        media_type = "audio/mp4"
    elif filepath.suffix == '.m4a':
        media_type = "audio/mp4"

    return FileResponse(
        path=filepath,
        media_type=media_type,
        filename=filepath.name
    )

@app.get("/api/history", response_model=List[HistoryEntry])
async def get_history():
    """Get download history"""
    history = load_history()
    return [HistoryEntry(**entry) for entry in history]

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)
