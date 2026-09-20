import en from './en.js'
import zhCN from './zh-CN.js'

export const LANGUAGE_MESSAGES = Object.freeze({
  en,
  'zh-CN': zhCN
})

export const LANGUAGE_OPTIONS = Object.freeze([
  Object.freeze({ id: 'en', selfName: en.language.selfName }),
  Object.freeze({ id: 'zh-CN', selfName: zhCN.language.selfName })
])
