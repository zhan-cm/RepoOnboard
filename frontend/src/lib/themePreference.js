const THEME_COOKIE = 'repoonboard-theme'
const THEME_STORAGE_KEY = 'repoonboard-theme'
const DEFAULT_THEME = 'dark'
const VALID_THEMES = new Set(['dark', 'light'])

export function readThemePreference() {
  const cookieTheme = readCookieTheme()
  if (cookieTheme) return cookieTheme

  if (typeof window !== 'undefined' && window.localStorage) {
    try {
      const storedTheme = window.localStorage.getItem(THEME_STORAGE_KEY)
      if (VALID_THEMES.has(storedTheme)) return storedTheme
    } catch {
      // Fall back to the default when storage is unavailable.
    }
  }

  return DEFAULT_THEME
}

export function persistThemePreference(theme) {
  if (!VALID_THEMES.has(theme)) return

  if (typeof document !== 'undefined') {
    document.cookie = `${THEME_COOKIE}=${theme}; Max-Age=31536000; Path=/; SameSite=Strict`
  }

  if (typeof window !== 'undefined' && window.localStorage) {
    try {
      window.localStorage.setItem(THEME_STORAGE_KEY, theme)
    } catch {
      // The host-wide cookie still preserves the preference when storage fails.
    }
  }
}

function readCookieTheme() {
  if (typeof document === 'undefined' || !document.cookie) return ''
  const prefix = `${THEME_COOKIE}=`
  const value = document.cookie
    .split(';')
    .map((item) => item.trim())
    .find((item) => item.startsWith(prefix))
    ?.slice(prefix.length)
  return VALID_THEMES.has(value) ? value : ''
}
