import { describe, expect, it } from 'vitest'
import ArchitectureInspector from '../components/ArchitectureInspector.vue'
import ArchitectureWorkspaceView from '../components/ArchitectureWorkspaceView.vue'
import {
  createArchitectureModel,
  createArchitectureScope,
  defaultArchitectureExploration
} from '../lib/reportArchitecture.js'
import { flushUi, mount } from './mount.js'

describe('ArchitectureWorkspaceView', () => {
  it('shows the bounded module graph and selects report-backed components', async () => {
    const model = createArchitectureModel(report())
    const selected = []
    const view = mount(ArchitectureWorkspaceView, {
      props: workspaceProps(model, {
        onSelect: (value) => selected.push(value)
      })
    })
    await flushUi()

    expect(view.container.textContent).toContain('Component relationships')
    expect(view.container.textContent).toContain('2 of 2 reported components shown')
    expect(view.container.textContent).toContain('1 confirmed relations')
    expect(view.container.textContent).not.toContain('Mapper')
    expect(view.container.querySelector('.architecture-graph')).not.toBeNull()

    await expandList(view)
    view.container.querySelector('.architecture-component-list button').click()
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
    const view = mount(ArchitectureWorkspaceView, { props: workspaceProps(noEdges) })
    await flushUi()
    expect(view.container.textContent).toContain('No confirmed component relationships in this scope')
    view.unmount()

    const empty = createArchitectureModel({
      schemaVersion: '1.2', status: 'SUCCESS',
      modules: [{ id: 'api', artifactId: 'api' }],
      components: [], dependencies: [], diagnostics: []
    })
    const emptyView = mount(ArchitectureWorkspaceView, { props: workspaceProps(empty) })
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
    const view = mount(ArchitectureWorkspaceView, { props: workspaceProps(unavailable) })
    await flushUi()

    expect(view.container.textContent).toContain('Relationship data unavailable')
    expect(view.container.textContent).toContain('Component relationship data is unavailable in this report')
    expect(view.container.textContent).not.toContain('No confirmed component relationships in this scope')
    view.unmount()
  })

  it('emits deterministic kind, relationship, search, and neighborhood state changes', async () => {
    const model = createArchitectureModel(report())
    const updates = []
    const exploration = defaultArchitectureExploration()
    const scope = createArchitectureScope(model, 'api', exploration)
    const view = mount(ArchitectureWorkspaceView, {
      props: {
        model,
        moduleId: 'api',
        exploration,
        scope,
        selection: { type: 'component', id: 'service' },
        onUpdateExploration: (value) => updates.push(value)
      }
    })
    await flushUi()

    const kindCheckbox = view.container.querySelector('.architecture-filter-menu input')
    kindCheckbox.checked = false
    kindCheckbox.dispatchEvent(new Event('change', { bubbles: true }))
    const relationshipCheckbox = view.container.querySelector('.architecture-relationship-filter input')
    relationshipCheckbox.checked = false
    relationshipCheckbox.dispatchEvent(new Event('change', { bubbles: true }))
    const search = view.container.querySelector('.architecture-search input')
    search.value = 'OrderService'
    search.dispatchEvent(new Event('input', { bubbles: true }))
    const neighborhood = [...view.container.querySelectorAll('.architecture-tool-button')]
      .find((button) => button.textContent.includes('1-hop'))
    neighborhood.click()

    expect(updates[0].componentKinds).toEqual(['SERVICE'])
    expect(updates[1].relationshipKinds).toEqual([])
    expect(updates[2].query).toBe('OrderService')
    expect(updates[3].neighborhood).toBe(true)
    view.unmount()
  })

  it('keeps the searchable component list available when the graph exceeds its budget', async () => {
    const components = Array.from({ length: 61 }, (_, index) => ({
      id: `component-${index}`,
      moduleId: 'api',
      qualifiedName: `example.Component${index}`,
      kind: 'COMPONENT',
      framework: 'SPRING_BOOT',
      evidence: []
    }))
    const model = createArchitectureModel({
      schemaVersion: '1.2', status: 'SUCCESS',
      modules: [{ id: 'api', artifactId: 'api' }],
      components, dependencies: [], diagnostics: []
    })
    const view = mount(ArchitectureWorkspaceView, { props: workspaceProps(model) })

    expect(view.container.textContent).toContain('Graph paused for readability')
    expect(view.container.textContent).toContain('61 withheld from graph')
    await expandList(view)
    expect(view.container.querySelectorAll('.architecture-component-list button')).toHaveLength(61)
    expect(view.container.querySelector('.architecture-graph')).toBeNull()
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

function workspaceProps(model, extra = {}) {
  const exploration = defaultArchitectureExploration()
  return {
    model,
    moduleId: 'api',
    exploration,
    scope: createArchitectureScope(model, 'api', exploration),
    ...extra
  }
}

async function expandList(view) {
  if (!view.container.querySelector('.architecture-component-list')) {
    view.container.querySelector('.architecture-component-tray__header').click()
    await flushUi()
  }
}

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
