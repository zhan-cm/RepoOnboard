import { afterEach, describe, expect, it } from 'vitest'
import {
  applyDocumentLanguage,
  persistLanguagePreference,
  readLanguagePreference
} from '../lib/languagePreference.js'

afterEach(() => {
  localStorage.clear()
  document.cookie = 'repoonboard-language=; Max-Age=0; Path=/'
  document.documentElement.removeAttribute('lang')
})

describe('languagePreference', () => {
  it('defaults to English without browser language detection', () => {
    Object.defineProperty(navigator, 'language', { configurable: true, value: 'zh-CN' })

    expect(readLanguagePreference()).toBe('en')
  })

  it('prefers the host-wide cookie over the port-scoped local fallback', () => {
    localStorage.setItem('repoonboard-language', 'en')
    document.cookie = 'repoonboard-language=zh-CN; Path=/; SameSite=Strict'

    expect(readLanguagePreference()).toBe('zh-CN')
  })

  it('falls back from an invalid cookie to a valid local preference', () => {
    localStorage.setItem('repoonboard-language', 'zh-CN')
    document.cookie = 'repoonboard-language=fr; Path=/; SameSite=Strict'

    expect(readLanguagePreference()).toBe('zh-CN')
  })

  it('ignores unsupported stored values', () => {
    localStorage.setItem('repoonboard-language', 'fr')

    expect(readLanguagePreference()).toBe('en')
  })

  it('writes both the host-wide cookie and the current-origin fallback', () => {
    persistLanguagePreference('zh-CN')

    expect(document.cookie).toContain('repoonboard-language=zh-CN')
    expect(localStorage.getItem('repoonboard-language')).toBe('zh-CN')
  })

  it('does not persist unsupported languages', () => {
    persistLanguagePreference('fr')

    expect(document.cookie).not.toContain('repoonboard-language=fr')
    expect(localStorage.getItem('repoonboard-language')).toBeNull()
  })

  it('keeps the document language on a supported deterministic value', () => {
    expect(applyDocumentLanguage('zh-CN')).toBe('zh-CN')
    expect(document.documentElement.lang).toBe('zh-CN')

    expect(applyDocumentLanguage('fr')).toBe('en')
    expect(document.documentElement.lang).toBe('en')
  })
})
