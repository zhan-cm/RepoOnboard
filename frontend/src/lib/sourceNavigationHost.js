export function createSourceNavigationHost(options = {}) {
  const clipboard = Object.hasOwn(options, 'clipboard')
    ? options.clipboard
    : globalThis.navigator?.clipboard ?? null

  return Object.freeze({
    async copyText(value) {
      if (typeof value !== 'string' || !value) {
        return { status: 'unavailable' }
      }
      if (!clipboard || typeof clipboard.writeText !== 'function') {
        return { status: 'unavailable' }
      }
      try {
        await clipboard.writeText(value)
        return { status: 'copied' }
      } catch {
        return { status: 'failed' }
      }
    }
  })
}
