# YouTube Authentication Setup

YouTube is blocking yt-dlp searches with bot detection. To fix this, you need to provide authentication cookies.

## Option 1: Use Browser Cookies (Recommended)

1. Install browser extension: https://github.com/yt-dlp/yt-dlp/wiki/Extractors#exporting-youtube-cookies
   - For Chrome: https://github.com/cvzi/cf_clearance/releases (for Cloudflare protection)
   - For Firefox: Use "Cookie Editor" extension

2. Export YouTube cookies:
   - Visit youtube.com and log in
   - Use the extension to export cookies as Netscape format
   - Save to `./cookies.txt` in the backend directory

3. Restart the backend - it will automatically use the cookies

## Option 2: Use API Key (If you have YouTube API access)

Since you mentioned having a Google license agreement, you may have YouTube API access.

Add to `Config.kt`:
```kotlin
const val YOUTUBE_API_KEY = "YOUR_API_KEY_HERE"
```

Then we can switch to using the official YouTube API for search instead of yt-dlp.

## Option 3: Try Different Player Clients (Currently Active)

The backend is trying:
- First: `player_client=android` (if no cookies)
- If that fails: manual cookie setup needed

## Troubleshooting

If you see "Sign in to confirm you're not a bot" error:
1. Check if `./cookies.txt` exists in the backend directory
2. Verify cookies are in Netscape format (text file with cookie data)
3. Check that cookies include YouTube session tokens

## What to do next

Do you have a YouTube API key from your Google license agreement? If yes, that's the cleanest solution.
