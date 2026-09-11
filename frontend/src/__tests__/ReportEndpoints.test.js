import { describe, expect, it } from 'vitest'
import { createApiModel, createApiScope, defaultApiExploration } from '../lib/reportEndpoints.js'

describe('reportEndpoints', () => {
  it('joins endpoint scope through exact module and component identities', () => {
    const model = createApiModel(report())

    expect(model.collectionAvailable).toBe(true)
    expect(model.endpoints).toHaveLength(4)
    expect(model.endpoints.find((item) => item.id === 'get').controllerQualifiedName)
      .toBe('example.UserController')
    expect(model.endpoints.find((item) => item.id === 'get').moduleLabel).toBe('api')
    expect(model.counts.anyMethods).toBe(1)
    expect(model.counts.unresolvedMethods).toBe(1)
    expect(model.counts.unresolvedPaths).toBe(1)
    expect(model.counts.brokenControllerLinks).toBe(2)
  })

  it('keeps ANY exact while module, method, and text filters compose', () => {
    const model = createApiModel(report())
    const anyScope = createApiScope(model, { moduleId: 'api', method: 'ANY', query: '' })
    expect(anyScope.endpoints.map((item) => item.id)).toEqual(['any'])

    const searched = createApiScope(model, {
      moduleId: 'api',
      method: 'GET',
      query: 'UserController.java'
    })
    expect(searched.endpoints.map((item) => item.id)).toEqual(['get'])
    expect(searched.counts.filteredEndpoints).toBe(3)
  })

  it('preserves unresolved path, conditions, and two mapping evidence levels', () => {
    const endpoint = createApiModel(report()).endpoints.find((item) => item.id === 'unresolved')

    expect(endpoint.path).toBeNull()
    expect(endpoint.pathLabel).toBe('Unresolved path')
    expect(endpoint.pathState).toBe('unresolved')
    expect(endpoint.conditions.unresolved).toBe(true)
    expect(endpoint.conditions.headers).toEqual(['X-Tenant'])
    expect(endpoint.typeEvidence).toHaveLength(1)
    expect(endpoint.methodEvidence).toHaveLength(1)
  })

  it('distinguishes unavailable endpoint data from a known empty collection', () => {
    const unavailable = createApiModel({ modules: [], components: [], diagnostics: [] })
    const empty = createApiModel({ modules: [], components: [], endpoints: [], diagnostics: [] })

    expect(unavailable.collectionAvailable).toBe(false)
    expect(unavailable.counts.endpoints).toBeNull()
    expect(empty.collectionAvailable).toBe(true)
    expect(empty.counts.endpoints).toBe(0)
    expect(defaultApiExploration()).toEqual({ moduleId: 'all', method: 'all', query: '' })
  })
})

function report() {
  const typeEvidence = evidence('SPRING_MVC_TYPE_MAPPING', 5)
  const methodEvidence = evidence('SPRING_MVC_METHOD_MAPPING', 12)
  return {
    schemaVersion: '1.2',
    status: 'PARTIAL',
    project: { name: 'sample' },
    modules: [
      { id: 'api-module', artifactId: 'api' },
      { id: 'admin-module', artifactId: 'admin' }
    ],
    components: [{
      id: 'controller', moduleId: 'api-module', qualifiedName: 'example.UserController',
      kind: 'REST_CONTROLLER', framework: 'SPRING_BOOT', evidence: []
    }],
    endpoints: [
      endpoint('get', 'api-module', 'controller', 'GET', '/api/users', false, 'list'),
      endpoint('any', 'api-module', 'controller', 'ANY', '/api/any', false, 'anyMethod'),
      {
        ...endpoint('unresolved', 'admin-module', 'missing', 'DELETE', null, true, 'remove'),
        conditions: {
          params: [], headers: ['X-Tenant'], consumes: [], produces: [], unresolved: true
        },
        evidence: [typeEvidence, methodEvidence]
      },
      endpoint('method', 'admin-module', 'missing', 'UNRESOLVED', '/admin', false, 'admin')
    ],
    diagnostics: [{
      code: 'SPRING_MVC_PATH_UNRESOLVED', severity: 'WARNING',
      stage: 'SPRING_MVC_MAPPING', message: 'Path unresolved.'
    }]
  }
}

function endpoint(id, moduleId, componentId, httpMethod, path, unresolvedPath, handlerMethod) {
  return {
    id, moduleId, componentId, httpMethod, path, unresolvedPath, handlerMethod,
    framework: 'SPRING_BOOT',
    conditions: { params: [], headers: [], consumes: [], produces: [], unresolved: false },
    location: {
      sourceFileId: 'src/main/java/example/UserController.java',
      startLine: 12,
      startColumn: 3,
      symbol: `example.UserController.${handlerMethod}`
    },
    evidence: [evidence('SPRING_MVC_METHOD_MAPPING', 12)]
  }
}

function evidence(type, line) {
  return {
    type,
    ruleId: `rule:${type}`,
    location: { sourceFileId: 'src/main/java/example/UserController.java', startLine: line },
    relatedLocations: []
  }
}
