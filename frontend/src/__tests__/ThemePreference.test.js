import { afterEach, describe, expect, it } from 'vitest'
import { persistThemePreference, readThemePreference } from '../lib/themePreference.js'

afterEach(() => {
  localStorage.clear()
  document.cookie = 'repoonboard-theme=; Max-Age=0; Path=/'
})

describe('themePreference', () => {
  it('prefers the host-wide cookie over the port-scoped local fallback', () => {
    localStorage.setItem('repoonboard-theme', 'dark')
    document.cookie = 'repoonboard-theme=light; Path=/; SameSite=Strict'

    expect(readThemePreference()).toBe('light')
  })

  it('keeps localStorage as a compatibility fallback and ignores invalid values', () => {
    localStorage.setItem('repoonboard-theme', 'light')
    expect(readThemePreference()).toBe('light')

    localStorage.setItem('repoonboard-theme', 'sepia')
    expect(readThemePreference()).toBe('dark')
  })

  it('writes both the host-wide cookie and the current-origin fallback', () => {
    persistThemePreference('light')

    expect(document.cookie).toContain('repoonboard-theme=light')
    expect(localStorage.getItem('repoonboard-theme')).toBe('light')
  })
})
