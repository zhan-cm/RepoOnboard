import { describe, expect, it, vi } from 'vitest'
import { createSourceNavigationHost } from '../lib/sourceNavigationHost.js'

describe('sourceNavigationHost', () => {
  it('reports copied, unavailable, and failed outcomes without throwing', async () => {
    const writeText = vi.fn().mockResolvedValue(undefined)
    expect(await createSourceNavigationHost({ clipboard: { writeText } }).copyText('a/b.java'))
      .toEqual({ status: 'copied' })
    expect(writeText).toHaveBeenCalledWith('a/b.java')

    expect(await createSourceNavigationHost({ clipboard: null }).copyText('a/b.java'))
      .toEqual({ status: 'unavailable' })
    expect(await createSourceNavigationHost({ clipboard: { writeText: vi.fn().mockRejectedValue(new Error('denied')) } })
      .copyText('a/b.java')).toEqual({ status: 'failed' })
    expect(await createSourceNavigationHost({ clipboard: { writeText } }).copyText(''))
      .toEqual({ status: 'unavailable' })
  })
})
