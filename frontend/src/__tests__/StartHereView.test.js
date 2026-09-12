import { describe, expect, it } from 'vitest'
import StartHereInspector from '../components/StartHereInspector.vue'
import StartHereView from '../components/StartHereView.vue'
import { createStartHereModel } from '../lib/reportStartHere.js'
import { mount } from './mount.js'

describe('StartHereView', () => {
  it('renders the bounded reading path and emits selection and expansion actions', () => {
    const model = createStartHereModel(report(), guide())
    const selections = []
    const expansion = []
    const view = mount(StartHereView, {
      props: {
        model,
        expanded: false,
        selectedSourceFileId: '',
        onSelect: (value) => selections.push(value),
        onUpdateExpanded: (value) => expansion.push(value)
      }
    })

    expect(view.container.textContent).toContain('Start Here')
    expect(view.container.textContent).toContain('OrderController.java')
    expect(view.container.textContent).not.toContain('pom.xml')
    expect(view.container.textContent).toContain('Show 1 more recommended file')
    view.container.querySelector('.start-here-item').click()
    view.container.querySelector('.start-here__expansion button').click()

    expect(selections).toEqual(['src/main/java/example/OrderController.java'])
    expect(expansion).toEqual([true])
    view.unmount()
  })

  it('shows partial and known-empty semantics together', () => {
    const partialReport = { ...report(), status: 'PARTIAL' }
    const emptyGuide = {
      ...guide(), analysisStatus: 'PARTIAL', coverageLimited: true,
      coverageLimitationCodes: ['PARTIAL_TEST'],
      coverageNotice: 'Recommendations are based on partial analysis and may omit important files.',
      totalItemCount: 0, expandable: false, hiddenItemCount: 0, items: []
    }
    const view = mount(StartHereView, {
      props: { model: createStartHereModel(partialReport, emptyGuide) }
    })

    expect(view.container.textContent).toContain('Coverage is limited')
    expect(view.container.textContent).toContain('No recommended files could be produced')
    view.unmount()
  })
})

describe('StartHereInspector', () => {
  it('shows all evidence and emits exact Component and Endpoint targets', () => {
    const model = createStartHereModel(report(), guide())
    const components = []
    const endpoints = []
    const view = mount(StartHereInspector, {
      props: {
        model,
        item: model.items[0],
        onExploreComponent: (value) => components.push(value),
        onExploreEndpoint: (value) => endpoints.push(value)
      }
    })

    expect(view.container.textContent).toContain('Exposes 1 HTTP endpoint')
    expect(view.container.textContent).toContain('src/main/java/example/OrderController.java:12')
    expect(view.container.textContent).toContain('example.OrderController')
    view.container.querySelector('.start-here-targets:not(.start-here-targets--endpoints) button').click()
    view.container.querySelector('.start-here-targets--endpoints button').click()

    expect(components).toEqual(['controller'])
    expect(endpoints).toEqual(['get-orders'])
    view.unmount()
  })
})

function report() {
  const sourceFileId = 'src/main/java/example/OrderController.java'
  return {
    schemaVersion: '1.2', status: 'SUCCESS',
    project: { id: 'project:orders', name: 'orders' },
    modules: [{ id: 'module:orders', artifactId: 'orders', pomFileId: 'pom.xml' }],
    sourceFiles: [{ id: sourceFileId, path: sourceFileId, moduleId: 'module:orders', language: 'JAVA' }],
    components: [{ id: 'controller', moduleId: 'module:orders', qualifiedName: 'example.OrderController', kind: 'REST_CONTROLLER' }],
    endpoints: [{ id: 'get-orders', moduleId: 'module:orders', componentId: 'controller', httpMethod: 'GET', path: '/api/orders', unresolvedPath: false, handlerMethod: 'list' }],
    entryPoints: [], dependencies: [], diagnostics: []
  }
}

function guide() {
  const sourceFileId = 'src/main/java/example/OrderController.java'
  const reason = {
    kind: 'HTTP_ENDPOINT_EXPOSURE', message: 'Exposes 1 HTTP endpoint', factCount: 1,
    dependencyDistance: null, supportingEntityIds: ['controller', 'get-orders'],
    evidence: [{ type: 'SPRING_MVC_METHOD_MAPPING', ruleId: 'spring.mvc.GET', location: { sourceFileId, startLine: 12 }, relatedLocations: [] }]
  }
  return {
    schemaVersion: '1.0', reportSchemaVersion: '1.2', projectId: 'project:orders',
    analysisStatus: 'SUCCESS', coverageLimited: false, coverageLimitationCodes: [], coverageNotice: null,
    defaultLimit: 1, totalItemCount: 2, expandable: true, hiddenItemCount: 1,
    items: [
      { sourceFileId, moduleId: 'module:orders', reasons: [reason] },
      {
        sourceFileId: 'pom.xml', moduleId: 'module:orders', reasons: [{
          kind: 'ROOT_BUILD_FILE', message: 'Root Maven build file', factCount: 1,
          dependencyDistance: null, supportingEntityIds: ['module:orders'],
          evidence: [{ type: 'MAVEN_PROJECT', ruleId: 'maven.root', location: { sourceFileId: 'pom.xml' }, relatedLocations: [] }]
        }]
      }
    ]
  }
}
