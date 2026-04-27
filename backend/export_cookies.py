import json
import sqlite3
import os
import shutil
import tempfile
from pathlib import Path
from datetime import datetime

def export_edge_cookies():
    """Export cookies from Edge browser and format for yt-dlp"""

    # Edge stores cookies in AppData
    edge_cookie_db = Path.home() / "AppData/Local/Microsoft/Edge/User Data/Default/Network/Cookies"

    if not edge_cookie_db.exists():
        print(f"❌ Edge cookies database not found at: {edge_cookie_db}")
        return False

    print(f"📖 Lectura de cookies de Edge...")

    try:
        # Make a temporary copy to avoid locking issues
        with tempfile.NamedTemporaryFile(delete=False, suffix=".db") as tmp:
            temp_db = tmp.name

        print(f"📋 Copiando base de datos (esto evita bloqueos)...")
        shutil.copy2(str(edge_cookie_db), temp_db)

        # Connect to the temporary copy
        conn = sqlite3.connect(temp_db)
        cursor = conn.cursor()

        # Query YouTube cookies
        cursor.execute("""
            SELECT name, value, host_key, path, expires_utc, is_secure, is_httponly
            FROM cookies
            WHERE host_key LIKE '%youtube%' OR host_key LIKE '%google%'
        """)

        cookies = cursor.fetchall()
        conn.close()

        if not cookies:
            print("⚠️  No se encontraron cookies de YouTube/Google")
            return False

        # Format as Netscape cookie jar format (compatible with yt-dlp)
        netscape_format = """# Netscape HTTP Cookie File
# This is a generated file!  Do not edit.

"""

        for cookie in cookies:
            name, value, host_key, path, expires_utc, is_secure, is_httponly = cookie

            # Convert expires_utc from Windows filetime to Unix timestamp
            # Windows filetime is 100-nanosecond intervals since 1601-01-01
            # If expires_utc is 0, treat as session cookie (use a far future date)
            if expires_utc == 0:
                expires = "0"
            else:
                expires = str(expires_utc // 10000000 - 11644473600)

            secure_flag = "TRUE" if is_secure else "FALSE"
            httponly_flag = "TRUE" if is_httponly else "FALSE"

            # .youtube.com format for domain
            domain = host_key
            if domain.startswith('.'):
                domain_flag = "TRUE"
            else:
                domain_flag = "FALSE"
                domain = "." + domain

            line = f"{domain}\t{domain_flag}\t{path}\t{secure_flag}\t{expires}\t{name}\t{value}\n"
            netscape_format += line

        # Write to cookies.txt
        cookies_file = Path("./cookies.txt")
        with open(cookies_file, 'w') as f:
            f.write(netscape_format)

        print(f"✅ Cookies exportadas a: {cookies_file.absolute()}")
        print(f"📊 Total: {len(cookies)} cookies")

        # Show a few for verification
        print(f"\n🔍 Primeras cookies:")
        for i, cookie in enumerate(cookies[:3]):
            print(f"  {i+1}. {cookie[0]} ({cookie[2]})")

        return True

    except Exception as e:
        print(f"❌ Error exportando cookies: {str(e)}")
        return False

    finally:
        # Clean up temp database
        try:
            if 'temp_db' in locals() and os.path.exists(temp_db):
                os.unlink(temp_db)
        except:
            pass

if __name__ == "__main__":
    success = export_edge_cookies()
    if success:
        print("\n✅ Ahora reinicia el servidor backend")
