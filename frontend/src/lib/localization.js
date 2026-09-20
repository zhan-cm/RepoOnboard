import { inject, readonly, ref } from 'vue'
import { LANGUAGE_MESSAGES, LANGUAGE_OPTIONS } from '../locales/index.js'
import { DEFAULT_LANGUAGE, isSupportedLanguage } from './languagePreference.js'

export const LOCALIZATION_KEY = Symbol('repoonboard-localization')

export function createLocalization(initialLanguage = DEFAULT_LANGUAGE, options = {}) {
  const language = ref(isSupportedLanguage(initialLanguage) ? initialLanguage : DEFAULT_LANGUAGE)
  const onLanguageChange = typeof options.onLanguageChange === 'function'
    ? options.onLanguageChange
    : () => {}

  function setLanguage(nextLanguage) {
    if (!isSupportedLanguage(nextLanguage)) return false
    if (language.value === nextLanguage) return true

    language.value = nextLanguage
    onLanguageChange(nextLanguage)
    return true
  }

  function t(key, values = {}) {
    const localizedValue = resolveMessage(LANGUAGE_MESSAGES[language.value], key)
    const fallbackValue = resolveMessage(LANGUAGE_MESSAGES[DEFAULT_LANGUAGE], key)
    const template = selectTemplate(localizedValue ?? fallbackValue, values.count)
    if (typeof template !== 'string') return ''

    return template.replace(/\{([A-Za-z][A-Za-z0-9]*)\}/g, (_, name) => (
      Object.prototype.hasOwnProperty.call(values, name) ? String(values[name]) : ''
    ))
  }

  return Object.freeze({
    language: readonly(language),
    languageOptions: LANGUAGE_OPTIONS,
    setLanguage,
    t
  })
}

export function useLocalization() {
  const localization = inject(LOCALIZATION_KEY)
  if (!localization) {
    throw new Error('RepoOnboard localization has not been provided.')
  }
  return localization
}

function resolveMessage(messages, key) {
  if (!messages || typeof key !== 'string' || !key) return undefined
  return key.split('.').reduce((value, segment) => (
    value && Object.prototype.hasOwnProperty.call(value, segment) ? value[segment] : undefined
  ), messages)
}

function selectTemplate(value, count) {
  if (typeof value === 'string') return value
  if (!value || typeof value !== 'object') return undefined
  return Number(count) === 1 ? value.one : value.other
}
