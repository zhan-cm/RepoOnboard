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

    const modulesButton = [...view.container.querySelectorAll('.nav-item')]
      .find((button) => button.textContent.includes('Modules'))
    expect(modulesButton.disabled).toBe(false)
    modulesButton.click()
    await flushUi()
    expect(view.container.querySelector('.module-explorer')).not.toBeNull()
    expect(view.container.querySelector('.module-row--active')).not.toBeNull()
    expect(view.container.textContent).toContain('Internal module relationships are unavailable')

    const architectureButton = [...view.container.querySelectorAll('.nav-item')]
      .find((button) => button.textContent.includes('Architecture'))
    expect(architectureButton.disabled).toBe(false)
    architectureButton.click()
    await flushUi()
    expect(view.container.querySelector('.architecture-workspace')).not.toBeNull()
    expect(view.container.textContent).toContain('No framework components were reported')

    const apiButton = [...view.container.querySelectorAll('.nav-item')]
      .find((button) => button.textContent.includes('APIs'))
    expect(apiButton.disabled).toBe(false)
    apiButton.click()
    await flushUi()
    expect(view.container.querySelector('.api-map')).not.toBeNull()
    expect(view.container.textContent).toContain('No HTTP endpoints were reported')
    expect(view.container.querySelector('.page-layout')).toBeNull()
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
        schemaVersion: '1.2',
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

  it('rejects a report with an incompatible schema before rendering facts', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({
        schemaVersion: '2.0',
        status: 'SUCCESS',
        project: { name: 'must-not-render' }
      })
    }))

    const view = mount(App)
    await flushUi()

    expect(view.container.querySelector('.state-panel--error').textContent)
      .toContain('Unsupported report schema (2.0)')
    expect(view.container.textContent).not.toContain('must-not-render')
    view.unmount()
  })

  it('renders hostile repository and diagnostic strings only as text', async () => {
    const hostileName = '<img src=x onerror="globalThis.repoOnboardInjected=true">'
    const hostileDiagnostic = '<script>globalThis.repoOnboardInjected=true</script>'
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({
        schemaVersion: '1.2', status: 'PARTIAL',
        project: { name: hostileName, buildSystem: 'MAVEN' },
        summary: { coverageLimited: true, coverageLimitationCodes: ['HOSTILE_TEXT'] },
        modules: [], sourceFiles: [], components: [], endpoints: [], entryPoints: [], dependencies: [],
        diagnostics: [{
          code: 'HOSTILE_TEXT', severity: 'WARNING', stage: 'test', message: hostileDiagnostic
        }]
      })
    }))

    const view = mount(App)
    await flushUi()

    expect(view.container.textContent).toContain(hostileName)
    expect(view.container.textContent).toContain(hostileDiagnostic)
    expect(view.container.querySelector('img')).toBeNull()
    expect(view.container.querySelector('script')).toBeNull()
    expect(globalThis.repoOnboardInjected).not.toBe(true)
    view.unmount()
  })

  it('opens endpoint source detail and returns to the preserved API selection', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue({
      ok: true,
      json: async () => sourceReport()
    }))

    const view = mount(App)
    await flushUi()
    const apiButton = [...view.container.querySelectorAll('.nav-item')]
      .find((button) => button.textContent.includes('APIs'))
    apiButton.click()
    await flushUi()
    view.container.querySelector('.api-method-button').click()
    await flushUi()
    view.container.querySelector('.source-detail-trigger').click()
    await flushUi()

    expect(view.container.querySelector('.source-detail')).not.toBeNull()
    expect(view.container.querySelector('.source-related')).not.toBeNull()
    expect(view.container.textContent).toContain('src/main/java/example/UserController.java')
    expect(view.container.querySelector('.nav-item--active').textContent).toContain('APIs')

    view.container.querySelector('.source-back').click()
    await flushUi()
    expect(view.container.querySelector('.source-detail')).toBeNull()
    expect(view.container.querySelector('.api-inspector__route')).not.toBeNull()
    view.unmount()
  })
})

function sourceReport() {
  const path = 'src/main/java/example/UserController.java'
  return {
    schemaVersion: '1.2', status: 'SUCCESS',
    project: { name: 'source-sample', buildSystem: 'MAVEN' },
    summary: { moduleCount: 1, sourceFileCount: 1, componentCount: 1, endpointCount: 1 },
    modules: [{ id: 'api', artifactId: 'api', baseDirectory: 'api', pomFileId: 'pom.xml' }],
    sourceFiles: [{ id: path, moduleId: 'api', path, language: 'JAVA', location: { sourceFileId: path } }],
    components: [{
      id: 'controller', moduleId: 'api', qualifiedName: 'example.UserController',
      kind: 'REST_CONTROLLER', framework: 'SPRING_BOOT',
      location: { sourceFileId: path, startLine: 5, symbol: 'example.UserController' }, evidence: []
    }],
    endpoints: [{
      id: 'get', moduleId: 'api', componentId: 'controller', httpMethod: 'GET',
      path: '/api/users', unresolvedPath: false, handlerMethod: 'get', framework: 'SPRING_BOOT',
      conditions: { params: [], headers: [], consumes: [], produces: [], unresolved: false },
      location: { sourceFileId: path, startLine: 12, startColumn: 3, symbol: 'example.UserController#get' },
      evidence: [{
        type: 'SPRING_MVC_METHOD_MAPPING', ruleId: 'spring.mvc.GET',
        location: { sourceFileId: path, startLine: 12 }, relatedLocations: []
      }]
    }],
    entryPoints: [], dependencies: [], diagnostics: []
  }
}
