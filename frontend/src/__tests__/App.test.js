import { afterEach, describe, expect, it, vi } from 'vitest'
import App from '../App.vue'
import { flushUi, mount } from './mount.js'

afterEach(() => {
  vi.unstubAllGlobals()
})

describe('App', () => {
  it('shows the shared loading pattern while the report is pending', () => {
    vi.stubGlobal('fetch', vi.fn(() => new Promise(() => {})))

    const view = mount(App)

    expect(view.container.textContent).toContain('Preparing workspace')
    expect(view.container.textContent).toContain('Loading analysis report')
    expect(view.container.querySelector('.state-panel--loading').getAttribute('role')).toBe('status')
    view.unmount()
  })

  it('places report context in the shell without rendering an overview page', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({
        schemaVersion: '1.1',
        status: 'PARTIAL',
        project: { name: 'sample-repository' }
      })
    }))

    const view = mount(App)
    await flushUi()

    expect(view.container.querySelector('h1').textContent).toBe('sample-repository')
    expect(view.container.querySelector('.status-badge').textContent.trim()).toBe('PARTIAL')
    expect(view.container.querySelector('.status-badge').classList.contains('status-badge--warning')).toBe(true)
    expect(view.container.textContent).toContain('Schema 1.1')
    expect(view.container.textContent).toContain('Workspace foundation ready')
    expect(view.container.textContent).not.toContain('Component statistics')
    view.unmount()
  })

  it('uses the shared error pattern when report loading fails', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue({
      ok: false,
      status: 503
    }))

    const view = mount(App)
    await flushUi()

    const error = view.container.querySelector('.state-panel--error')
    expect(error.getAttribute('role')).toBe('alert')
    expect(error.textContent).toContain('Report unavailable')
    expect(error.textContent).toContain('Report request failed (503)')
    view.unmount()
  })
})
