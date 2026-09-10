import { describe, expect, it } from 'vitest'
import ArchitectureInspector from '../components/ArchitectureInspector.vue'
import ArchitectureWorkspaceView from '../components/ArchitectureWorkspaceView.vue'
import { createArchitectureModel, createArchitectureScope } from '../lib/reportArchitecture.js'
import { flushUi, mount } from './mount.js'

describe('ArchitectureWorkspaceView', () => {
  it('shows the bounded module graph and selects report-backed components', async () => {
    const model = createArchitectureModel(report())
    const selected = []
    const view = mount(ArchitectureWorkspaceView, {
      props: {
        model,
        moduleId: 'api',
        onSelect: (value) => selected.push(value)
      }
    })
    await flushUi()

    expect(view.container.textContent).toContain('Component relationships')
    expect(view.container.textContent).toContain('2 reported components')
    expect(view.container.textContent).toContain('1 confirmed relations')
    expect(view.container.textContent).not.toContain('Mapper')
    expect(view.container.querySelector('.architecture-graph')).not.toBeNull()

    view.container.querySelector('.architecture-mobile-list button').click()
    expect(selected[0]).toEqual({ type: 'component', id: 'controller' })
    view.unmount()
  })

  it('keeps known no-edge and empty states distinct', async () => {
    const noEdges = createArchitectureModel({
      schemaVersion: '1.2', status: 'SUCCESS',
      modules: [{ id: 'api', artifactId: 'api' }],
      components: [{
        id: 'service', moduleId: 'api', qualifiedName: 'example.Service',
        kind: 'SERVICE', framework: 'SPRING_BOOT', evidence: []
      }],
      dependencies: [], diagnostics: []
    })
    const view = mount(ArchitectureWorkspaceView, { props: { model: noEdges, moduleId: 'api' } })
    await flushUi()
    expect(view.container.textContent).toContain('No confirmed component relationships in this scope')
    view.unmount()

    const empty = createArchitectureModel({
      schemaVersion: '1.2', status: 'SUCCESS',
      modules: [{ id: 'api', artifactId: 'api' }],
      components: [], dependencies: [], diagnostics: []
    })
    const emptyView = mount(ArchitectureWorkspaceView, { props: { model: empty, moduleId: 'api' } })
    expect(emptyView.container.textContent).toContain('No components reported')
    emptyView.unmount()
  })

  it('keeps unavailable relationship data distinct from a known zero', async () => {
    const unavailable = createArchitectureModel({
      schemaVersion: '1.2', status: 'SUCCESS',
      modules: [{ id: 'api', artifactId: 'api' }],
      components: [{
        id: 'service', moduleId: 'api', qualifiedName: 'example.Service',
        kind: 'SERVICE', framework: 'SPRING_BOOT', evidence: []
      }],
      diagnostics: []
    })
    const view = mount(ArchitectureWorkspaceView, { props: { model: unavailable, moduleId: 'api' } })
    await flushUi()

    expect(view.container.textContent).toContain('Relationship data unavailable')
    expect(view.container.textContent).toContain('Component relationship data is unavailable in this report')
    expect(view.container.textContent).not.toContain('No confirmed component relationships in this scope')
    view.unmount()
  })
})

describe('ArchitectureInspector', () => {
  it('shows exact component and relationship evidence without inferred annotations', () => {
    const model = createArchitectureModel(report())
    const scope = createArchitectureScope(model, 'api')
    const componentView = mount(ArchitectureInspector, {
      props: { model, scope, selection: { type: 'component', id: 'service' } }
    })

    expect(componentView.container.textContent).toContain('example.OrderService')
    expect(componentView.container.textContent).toContain('SPRING_COMPONENT_STEREOTYPE')
    expect(componentView.container.textContent).not.toContain('@Service')
    componentView.unmount()

    const edgeView = mount(ArchitectureInspector, {
      props: { model, scope, selection: { type: 'dependency', id: 'injection' } }
    })
    expect(edgeView.container.textContent).toContain('OrderController → OrderService')
    expect(edgeView.container.textContent).toContain('example.OrderService')
    expect(edgeView.container.textContent).toContain('CONSTRUCTOR_INJECTION')
    edgeView.unmount()
  })
})

function report() {
  return {
    schemaVersion: '1.2', status: 'SUCCESS', diagnostics: [],
    modules: [{ id: 'api', artifactId: 'api' }],
    components: [
      {
        id: 'controller', moduleId: 'api', qualifiedName: 'example.OrderController',
        kind: 'REST_CONTROLLER', framework: 'SPRING_BOOT', evidence: []
      },
      {
        id: 'service', moduleId: 'api', qualifiedName: 'example.OrderService', name: 'orderService',
        kind: 'SERVICE', framework: 'SPRING_BOOT',
        location: { sourceFileId: 'api/src/main/java/example/OrderService.java', startLine: 8, symbol: 'example.OrderService' },
        evidence: [{
          type: 'SPRING_COMPONENT', ruleId: 'SPRING_COMPONENT_STEREOTYPE',
          location: { sourceFileId: 'api/src/main/java/example/OrderService.java', startLine: 8 },
          relatedLocations: []
        }]
      }
    ],
    dependencies: [{
      id: 'injection', sourceId: 'controller', targetId: 'service',
      declaredTarget: 'example.OrderService', kind: 'COMPONENT_INJECTION', status: 'CONFIRMED',
      location: { sourceFileId: 'api/src/main/java/example/OrderController.java', startLine: 15 },
      evidence: [{
        type: 'CONSTRUCTOR_PARAMETER', ruleId: 'CONSTRUCTOR_INJECTION',
        location: { sourceFileId: 'api/src/main/java/example/OrderController.java', startLine: 15 },
        relatedLocations: []
      }]
    }]
  }
}
