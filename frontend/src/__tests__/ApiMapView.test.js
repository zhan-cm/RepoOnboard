import { describe, expect, it } from 'vitest'
import ApiInspector from '../components/ApiInspector.vue'
import ApiMapView from '../components/ApiMapView.vue'
import { createApiModel, createApiScope, defaultApiExploration } from '../lib/reportEndpoints.js'
import { mount } from './mount.js'

describe('ApiMapView', () => {
  it('renders exact reported endpoint states and selects a route', () => {
    const model = createApiModel(report())
    const exploration = defaultApiExploration()
    const selected = []
    const view = mount(ApiMapView, {
      props: {
        model,
        exploration,
        scope: createApiScope(model, exploration),
        selection: null,
        onSelect: (value) => selected.push(value)
      }
    })

    expect(view.container.textContent).toContain('HTTP endpoints')
    expect(view.container.textContent).toContain('/api/users')
    expect(view.container.textContent).toContain('ANY')
    expect(view.container.textContent).toContain('Unresolved path')
    expect(view.container.textContent).toContain('Conditions unresolved')
    const getButton = [...view.container.querySelectorAll('.api-method-button')]
      .find((button) => button.getAttribute('aria-label').startsWith('Inspect GET'))
    getButton.click()
    expect(selected[0]).toEqual({ type: 'endpoint', id: 'get' })
    view.unmount()
  })

  it('emits composable module, method, query, and reset state', () => {
    const model = createApiModel(report())
    const exploration = defaultApiExploration()
    const updates = []
    const selections = []
    const view = mount(ApiMapView, {
      props: {
        model,
        exploration,
        scope: createApiScope(model, exploration),
        selection: null,
        onUpdateExploration: (value) => updates.push(value),
        onSelect: (value) => selections.push(value)
      }
    })
    const selects = view.container.querySelectorAll('.api-filters select')
    selects[0].value = 'api-module'
    selects[0].dispatchEvent(new Event('change', { bubbles: true }))
    selects[1].value = 'GET'
    selects[1].dispatchEvent(new Event('change', { bubbles: true }))
    const input = view.container.querySelector('.api-search input')
    input.value = 'users'
    input.dispatchEvent(new Event('input', { bubbles: true }))

    expect(updates[0]).toEqual({ moduleId: 'api-module', method: 'all', query: '' })
    expect(updates[1]).toEqual({ moduleId: 'all', method: 'GET', query: '' })
    expect(updates[2]).toEqual({ moduleId: 'all', method: 'all', query: 'users' })

    const filteredExploration = { moduleId: 'api-module', method: 'GET', query: 'users' }
    const filtered = mount(ApiMapView, {
      props: {
        model,
        exploration: filteredExploration,
        scope: createApiScope(model, filteredExploration),
        selection: { type: 'endpoint', id: 'get' },
        onUpdateExploration: (value) => updates.push(value),
        onSelect: (value) => selections.push(value)
      }
    })
    filtered.container.querySelector('.api-filters button').click()
    expect(selections.at(-1)).toBeNull()
    expect(updates.at(-1)).toEqual(defaultApiExploration())
    filtered.unmount()
    view.unmount()
  })

  it('keeps unavailable, known empty, and filtered empty states distinct', () => {
    const unavailable = createApiModel({ modules: [], components: [], diagnostics: [] })
    const unavailableView = mountView(unavailable)
    expect(unavailableView.container.textContent).toContain('Endpoint data unavailable')
    unavailableView.unmount()

    const empty = createApiModel({ modules: [], components: [], endpoints: [], diagnostics: [] })
    const emptyView = mountView(empty)
    expect(emptyView.container.textContent).toContain('No HTTP endpoints were reported')
    emptyView.unmount()

    const model = createApiModel(report())
    const exploration = { moduleId: 'all', method: 'GET', query: 'does-not-exist' }
    const filteredView = mountView(model, exploration)
    expect(filteredView.container.textContent).toContain('No endpoints match the current filters')
    filteredView.unmount()
  })
})

describe('ApiInspector', () => {
  it('shows handler source, conditions, and exact two-level mapping evidence', () => {
    const model = createApiModel(report())
    const scope = createApiScope(model)
    const view = mount(ApiInspector, {
      props: { model, scope, selection: { type: 'endpoint', id: 'get' } }
    })

    expect(view.container.textContent).toContain('example.UserController')
    expect(view.container.textContent).toContain('active=true')
    expect(view.container.textContent).toContain('Controller class mapping')
    expect(view.container.textContent).toContain('Handler method mapping')
    expect(view.container.textContent).toContain('spring.mvc.mapping.annotation:GetMapping')
    expect(view.container.textContent).not.toContain('String list()')
    view.unmount()
  })
})

function mountView(model, exploration = defaultApiExploration()) {
  return mount(ApiMapView, {
    props: {
      model,
      exploration,
      scope: createApiScope(model, exploration),
      selection: null
    }
  })
}

function report() {
  return {
    schemaVersion: '1.2', status: 'PARTIAL', project: { name: 'sample' },
    modules: [{ id: 'api-module', artifactId: 'api' }],
    components: [{
      id: 'controller', moduleId: 'api-module', qualifiedName: 'example.UserController',
      kind: 'REST_CONTROLLER', framework: 'SPRING_BOOT', evidence: []
    }],
    endpoints: [
      endpoint('get', 'GET', '/api/users', false, false),
      endpoint('any', 'ANY', '/api/any', false, false),
      endpoint('unresolved', 'DELETE', null, true, true)
    ],
    diagnostics: [{
      code: 'SPRING_MVC_PATH_UNRESOLVED', severity: 'WARNING',
      stage: 'SPRING_MVC_MAPPING', message: 'A mapping path remains unresolved.'
    }]
  }
}

function endpoint(id, httpMethod, path, unresolvedPath, unresolvedConditions) {
  return {
    id,
    moduleId: 'api-module',
    componentId: 'controller',
    httpMethod,
    path,
    unresolvedPath,
    handlerMethod: id === 'get' ? 'list' : id,
    framework: 'SPRING_BOOT',
    conditions: {
      params: id === 'get' ? ['active=true'] : [],
      headers: [], consumes: [], produces: ['application/json'],
      unresolved: unresolvedConditions
    },
    location: {
      sourceFileId: 'src/main/java/example/UserController.java',
      startLine: 12, startColumn: 3, symbol: `example.UserController.${id}`
    },
    evidence: [
      {
        type: 'SPRING_MVC_TYPE_MAPPING',
        ruleId: 'spring.mvc.mapping.annotation:RequestMapping',
        location: { sourceFileId: 'src/main/java/example/UserController.java', startLine: 5 },
        relatedLocations: []
      },
      {
        type: 'SPRING_MVC_METHOD_MAPPING',
        ruleId: 'spring.mvc.mapping.annotation:GetMapping',
        location: { sourceFileId: 'src/main/java/example/UserController.java', startLine: 12 },
        relatedLocations: []
      }
    ]
  }
}
