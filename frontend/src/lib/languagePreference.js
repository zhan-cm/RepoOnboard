export const DEFAULT_LANGUAGE = 'en'
export const SUPPORTED_LANGUAGES = Object.freeze(['en', 'zh-CN'])

const LANGUAGE_COOKIE = 'repoonboard-language'
const LANGUAGE_STORAGE_KEY = 'repoonboard-language'
const VALID_LANGUAGES = new Set(SUPPORTED_LANGUAGES)

export function isSupportedLanguage(language) {
  return VALID_LANGUAGES.has(language)
}

export function readLanguagePreference() {
  const cookieLanguage = readCookieLanguage()
  if (cookieLanguage) return cookieLanguage

  if (typeof window !== 'undefined' && window.localStorage) {
    try {
      const storedLanguage = window.localStorage.getItem(LANGUAGE_STORAGE_KEY)
      if (isSupportedLanguage(storedLanguage)) return storedLanguage
    } catch {
      // Fall back to English when storage is unavailable.
    }
  }

  return DEFAULT_LANGUAGE
}

export function persistLanguagePreference(language) {
  if (!isSupportedLanguage(language)) return

  if (typeof document !== 'undefined') {
    document.cookie = `${LANGUAGE_COOKIE}=${language}; Max-Age=31536000; Path=/; SameSite=Strict`
  }

  if (typeof window !== 'undefined' && window.localStorage) {
    try {
      window.localStorage.setItem(LANGUAGE_STORAGE_KEY, language)
    } catch {
      // The host-wide cookie still preserves the preference when storage fails.
    }
  }
}

export function applyDocumentLanguage(language) {
  const normalizedLanguage = isSupportedLanguage(language) ? language : DEFAULT_LANGUAGE
  if (typeof document !== 'undefined' && document.documentElement) {
    document.documentElement.lang = normalizedLanguage
  }
  return normalizedLanguage
}

function readCookieLanguage() {
  if (typeof document === 'undefined' || !document.cookie) return ''
  const prefix = `${LANGUAGE_COOKIE}=`
  const value = document.cookie
    .split(';')
    .map((item) => item.trim())
    .find((item) => item.startsWith(prefix))
    ?.slice(prefix.length)
  return isSupportedLanguage(value) ? value : ''
}
