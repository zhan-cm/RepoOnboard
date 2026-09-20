import { describe, expect, it, vi } from 'vitest'
import { LANGUAGE_MESSAGES, LANGUAGE_OPTIONS } from '../locales/index.js'
import { createLocalization } from '../lib/localization.js'

describe('localization', () => {
  it('ships complete English and Simplified Chinese resource trees', () => {
    expect(leafPaths(LANGUAGE_MESSAGES.en)).toEqual(leafPaths(LANGUAGE_MESSAGES['zh-CN']))
    expect(leafValues(LANGUAGE_MESSAGES.en).every((value) => value.length > 0)).toBe(true)
    expect(leafValues(LANGUAGE_MESSAGES['zh-CN']).every((value) => value.length > 0)).toBe(true)
    expect(leafValues(LANGUAGE_MESSAGES.en).every((value) => !/<[^>]+>/.test(value))).toBe(true)
    expect(leafValues(LANGUAGE_MESSAGES['zh-CN']).every((value) => !/<[^>]+>/.test(value))).toBe(true)
    expect(LANGUAGE_OPTIONS).toEqual([
      { id: 'en', selfName: 'English' },
      { id: 'zh-CN', selfName: '简体中文' }
    ])
  })

  it('uses English as the deterministic fallback for an invalid initial language', () => {
    const localization = createLocalization('fr')

    expect(localization.language.value).toBe('en')
    expect(localization.t('settings.title')).toBe('Settings')
  })

  it('falls back to the English resource when the selected locale omits a key', () => {
    const chineseTitle = LANGUAGE_MESSAGES['zh-CN'].settings.title
    delete LANGUAGE_MESSAGES['zh-CN'].settings.title

    try {
      expect(createLocalization('zh-CN').t('settings.title')).toBe('Settings')
    } finally {
      LANGUAGE_MESSAGES['zh-CN'].settings.title = chineseTitle
    }
  })

  it('translates static resources without a network request', () => {
    const fetchSpy = vi.spyOn(globalThis, 'fetch')
    const localization = createLocalization('zh-CN')

    expect(localization.t('settings.title')).toBe('设置')
    expect(localization.t('settings.offlineAvailable')).toContain('无需网络连接')
    expect(fetchSpy).not.toHaveBeenCalled()
    fetchSpy.mockRestore()
  })

  it('supports interpolation and deterministic singular or plural selection', () => {
    const localization = createLocalization('en')

    expect(localization.t('common.fileCount', { count: 1 })).toBe('1 file')
    expect(localization.t('common.fileCount', { count: 4 })).toBe('4 files')
  })

  it('changes only to supported languages and reports the explicit change', () => {
    const onLanguageChange = vi.fn()
    const localization = createLocalization('en', { onLanguageChange })

    expect(localization.setLanguage('zh-CN')).toBe(true)
    expect(localization.language.value).toBe('zh-CN')
    expect(onLanguageChange).toHaveBeenCalledOnce()
    expect(onLanguageChange).toHaveBeenCalledWith('zh-CN')

    expect(localization.setLanguage('fr')).toBe(false)
    expect(localization.language.value).toBe('zh-CN')
    expect(onLanguageChange).toHaveBeenCalledOnce()
  })

  it('returns an empty value instead of exposing an untranslated resource key', () => {
    const localization = createLocalization('en')

    expect(localization.t('missing.resource')).toBe('')
  })
})

function leafPaths(value, prefix = '') {
  return Object.entries(value)
    .flatMap(([key, child]) => {
      const path = prefix ? `${prefix}.${key}` : key
      return typeof child === 'string' ? [path] : leafPaths(child, path)
    })
    .sort()
}

function leafValues(value) {
  return Object.values(value).flatMap((child) => (
    typeof child === 'string' ? [child] : leafValues(child)
  ))
}
