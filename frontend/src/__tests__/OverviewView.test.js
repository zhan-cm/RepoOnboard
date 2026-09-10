import { describe, expect, it } from 'vitest'
import OverviewView from '../components/OverviewView.vue'
import { mount } from './mount.js'

function report(overrides = {}) {
  return {
    schemaVersion: '1.1',
    status: 'PARTIAL',
    project: { name: 'sample', buildSystem: 'MAVEN' },
    summary: {
      moduleCount: 1,
      sourceFileCount: 2,
      componentCount: 3,
      controllerCount: 1,
      serviceCount: 1,
      repositoryCount: 0,
      configurationCount: 1,
      endpointCount: 2,
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
      id: 'entry:app',
      moduleId: 'module:root',
      qualifiedName: 'sample.Application',
      kind: 'APPLICATION_ENTRY_POINT',
      framework: 'SPRING_BOOT',
      location: { sourceFileId: 'src/main/java/sample/Application.java', startLine: 7 }
    }],
    dependencies: [],
    diagnostics: [
      { code: 'SCAN_NOTE', severity: 'INFO', message: 'Informational only.', stage: 'java' },
      { code: 'JAVA_PARSE_FAILED', severity: 'WARNING', message: 'One file failed.', stage: 'java' }
    ],
    ...overrides
  }
}

describe('OverviewView', () => {
  it('renders confirmed technology, summary, entry point, and coverage facts', () => {
    const view = mount(OverviewView, { props: { report: report() } })

    expect(view.container.textContent).toContain('Coverage is limited')
    expect(view.container.querySelector('.state-panel--warning')).not.toBeNull()
    expect(view.container.textContent).toContain('Maven')
    expect(view.container.textContent).toContain('Java')
    expect(view.container.textContent).toContain('Spring Boot')
    expect(view.container.textContent).toContain('sample.Application')
    expect(view.container.textContent).toContain('src/main/java/sample/Application.java:7')
    expect(view.container.textContent).toContain('JAVA_PARSE_FAILED')
    expect(view.container.textContent).not.toContain('Informational only.')
    expect(view.container.querySelectorAll('.metric-card')).toHaveLength(11)
    view.unmount()
  })

  it('renders explicit unavailable and empty states for absent facts', () => {
    const view = mount(OverviewView, {
      props: {
        report: {
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
        }
      }
    })

    expect(view.container.textContent).toContain('Not available')
    expect(view.container.textContent).toContain('No source roots are available in this report.')
    expect(view.container.textContent).toContain('No application entry points are available in this report.')
    expect(view.container.textContent).toContain('No warnings or errors are reported.')
    view.unmount()
  })

  it('limits the overview diagnostic preview while preserving the total', () => {
    const diagnostics = Array.from({ length: 7 }, (_, index) => ({
      code: `WARNING_${index}`,
      severity: 'WARNING',
      message: `Warning ${index}`
    }))
    const view = mount(OverviewView, { props: { report: report({ diagnostics }) } })

    expect(view.container.textContent).toContain('Showing 5 of 7 significant diagnostics.')
    expect(view.container.querySelectorAll('.diagnostic-list > li')).toHaveLength(5)
    expect(view.container.textContent).not.toContain('WARNING_6')
    view.unmount()
  })
})
