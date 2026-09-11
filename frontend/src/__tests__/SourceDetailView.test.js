import { describe, expect, it, vi } from 'vitest'
import SourceDetailView from '../components/SourceDetailView.vue'
import SourceRelatedInspector from '../components/SourceRelatedInspector.vue'
import { createSourceDetailModel } from '../lib/reportSources.js'
import { createSourceNavigationHost } from '../lib/sourceNavigationHost.js'
import { flushUi, mount } from './mount.js'

describe('SourceDetailView', () => {
  it('copies exact report values and provides accessible success feedback', async () => {
    const writeText = vi.fn().mockResolvedValue(undefined)
    const view = mount(SourceDetailView, {
      props: {
        model: createSourceDetailModel(report(), { type: 'endpoint', id: 'get' }),
        host: createSourceNavigationHost({ clipboard: { writeText } })
      }
    })

    const buttons = [...view.container.querySelectorAll('button')]
    buttons.find((button) => button.textContent.includes('Copy path')).click()
    await flushUi()
    expect(writeText).toHaveBeenCalledWith('src/main/java/example/UserController.java')
    expect(view.container.textContent).toContain('Path copied.')

    buttons.find((button) => button.textContent.includes('Copy symbol')).click()
    await flushUi()
    expect(writeText).toHaveBeenCalledWith('example.UserController#get')
    view.unmount()
  })

  it('shows file-only and clipboard fallback states without line zero', async () => {
    const sourceReport = report()
    sourceReport.endpoints[0].location = { sourceFileId: 'src/main/java/example/UserController.java' }
    const view = mount(SourceDetailView, {
      props: {
        model: createSourceDetailModel(sourceReport, { type: 'endpoint', id: 'get' }),
        host: createSourceNavigationHost({ clipboard: null })
      }
    })

    expect(view.container.textContent).toContain('Exact line and column were not reported')
    expect(view.container.textContent).not.toContain('Line 0')
    expect(view.container.textContent).toContain('Symbol unavailable')
    const copyLocation = [...view.container.querySelectorAll('button')]
      .find((button) => button.textContent.includes('Copy location'))
    copyLocation.click()
    await flushUi()
    expect(view.container.textContent).toContain('Copy unavailable — select the text manually.')
    view.unmount()
  })

  it('renders diagnostic and source text without executing markup', () => {
    const sourceReport = report()
    sourceReport.diagnostics[0].message = '<img src=x onerror=alert(1)>'
    const model = createSourceDetailModel(sourceReport, { type: 'endpoint', id: 'get' })
    const inspector = mount(SourceRelatedInspector, { props: { model } })

    expect(inspector.container.textContent).toContain('<img src=x onerror=alert(1)>')
    expect(inspector.container.querySelector('img')).toBeNull()
    expect(inspector.container.textContent).toContain('not a handler call chain')
    inspector.unmount()
  })
})

function report() {
  const path = 'src/main/java/example/UserController.java'
  return {
    schemaVersion: '1.2', status: 'PARTIAL',
    modules: [{ id: 'api', artifactId: 'api', baseDirectory: 'api', pomFileId: 'pom.xml' }],
    sourceFiles: [{ id: path, moduleId: 'api', path, language: 'JAVA', location: { sourceFileId: path } }],
    components: [{
      id: 'controller', moduleId: 'api', qualifiedName: 'example.UserController',
      kind: 'REST_CONTROLLER', framework: 'SPRING_BOOT',
      location: { sourceFileId: path, startLine: 5, symbol: 'example.UserController' }, evidence: []
    }, {
      id: 'service', moduleId: 'api', qualifiedName: 'example.UserService',
      kind: 'SERVICE', framework: 'SPRING_BOOT',
      location: { sourceFileId: 'src/main/java/example/UserService.java', startLine: 5 }, evidence: []
    }],
    endpoints: [{
      id: 'get', moduleId: 'api', componentId: 'controller', httpMethod: 'GET',
      path: '/api/users', unresolvedPath: false, handlerMethod: 'get', framework: 'SPRING_BOOT',
      location: {
        sourceFileId: path, startLine: 12, startColumn: 3, endLine: 14, endColumn: 3,
        symbol: 'example.UserController#get'
      },
      evidence: [{
        type: 'SPRING_MVC_METHOD_MAPPING', ruleId: 'spring.mvc.GET',
        location: { sourceFileId: path, startLine: 12 }, relatedLocations: []
      }]
    }],
    entryPoints: [],
    dependencies: [{
      id: 'dep', sourceId: 'controller', targetId: 'service',
      declaredTarget: 'example.UserService', kind: 'COMPONENT_INJECTION', status: 'CONFIRMED',
      location: { sourceFileId: path, startLine: 8 }, evidence: []
    }],
    diagnostics: [{
      code: 'JAVA_NOTE', severity: 'WARNING', stage: 'JAVA', fileId: path,
      message: 'One fact is limited.'
    }]
  }
}
