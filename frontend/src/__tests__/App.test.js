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

  it('renders the report overview inside the shared product shell', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({
        schemaVersion: '1.1',
        status: 'PARTIAL',
        project: { name: 'sample-repository', buildSystem: 'MAVEN' },
        summary: {
          moduleCount: 1,
          sourceFileCount: 2,
          componentCount: 2,
          controllerCount: 1,
          serviceCount: 1,
          repositoryCount: 0,
          configurationCount: 0,
          endpointCount: 3,
          entryPointCount: 1,
          dependencyCount: 1,
          analysisStatus: 'PARTIAL',
          coverageLimited: true,
          coverageLimitationCodes: ['JAVA_PARSE_FAILED']
        },
        modules: [{ id: 'module:root', sourceRoots: ['src/main/java'], frameworks: ['SPRING_BOOT'] }],
        sourceFiles: [{ language: 'JAVA' }, { language: 'JAVA' }],
        components: [],
        endpoints: [],
        entryPoints: [{
          id: 'entry:application',
          moduleId: 'module:root',
          qualifiedName: 'sample.Application',
          kind: 'APPLICATION_ENTRY_POINT',
          framework: 'SPRING_BOOT',
          location: { sourceFileId: 'src/main/java/sample/Application.java', startLine: 7 }
        }],
        dependencies: [],
        diagnostics: [{
          code: 'JAVA_PARSE_FAILED',
          severity: 'WARNING',
          stage: 'java',
          message: 'One source file could not be parsed.'
        }]
      })
    }))

    const view = mount(App)
    await flushUi()

    expect(view.container.querySelector('h1').textContent).toBe('sample-repository')
    expect(view.container.querySelector('.status-badge').textContent.trim()).toBe('PARTIAL')
    expect(view.container.querySelector('.status-badge').classList.contains('status-badge--warning')).toBe(true)
    expect(view.container.textContent).toContain('Schema 1.1')
    expect(view.container.textContent).toContain('Project at a glance')
    expect(view.container.textContent).toContain('Technology facts')
    expect(view.container.textContent).toContain('sample.Application')
    expect(view.container.textContent).toContain('Coverage is limited')
    expect(view.container.querySelector('.nav-item--active').textContent).toContain('Overview')
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

  it('labels a missing repository name as unavailable after loading', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({
        status: 'SUCCESS',
        project: {},
        summary: { coverageLimited: false, coverageLimitationCodes: [] },
        modules: [],
        sourceFiles: [],
        components: [],
        endpoints: [],
        entryPoints: [],
        dependencies: [],
        diagnostics: []
      })
    }))

    const view = mount(App)
    await flushUi()

    expect(view.container.querySelector('h1').textContent).toBe('Repository name unavailable')
    view.unmount()
  })
})
