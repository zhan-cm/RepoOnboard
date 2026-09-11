import { describe, expect, it } from 'vitest'
import { copySourceLocation, createSourceDetailModel, formatSourceLocation } from '../lib/reportSources.js'

describe('reportSources', () => {
  it('joins an endpoint source and related facts only through exact identities', () => {
    const model = createSourceDetailModel(report(), { type: 'endpoint', id: 'get' })

    expect(model.available).toBe(true)
    expect(model.entity.pathLabel).toBe('/api/users')
    expect(model.module.label).toBe('api')
    expect(model.sourceFile.language).toBe('Java')
    expect(model.related.owningController.qualifiedName).toBe('example.UserController')
    expect(model.related.controllerEndpoints.map((item) => item.id)).toEqual(['post'])
    expect(model.related.sameFileComponents.map((item) => item.id)).toEqual(['controller', 'helper'])
    expect(model.related.dependencyNeighbors.map((item) => [item.direction, item.component.id]))
      .toEqual([['OUTGOING', 'service']])
    expect(model.related.sameFileEntryPoints.map((item) => item.id)).toEqual(['entry'])
    expect(model.related.fileDiagnostics.map((item) => item.code)).toEqual(['JAVA_PARSE_WARNING'])
    expect(model.copyLocationText).toBe('api/src/main/java/example/UserController.java:12:3')
  })

  it('derives confirmed component neighbors without treating unresolved or Maven edges as related components', () => {
    const model = createSourceDetailModel(report(), { type: 'component', id: 'service' })

    expect(model.entityType).toBe('component')
    expect(model.related.dependencyNeighbors.map((item) => [item.direction, item.component.id]))
      .toEqual([['INCOMING', 'controller']])
    expect(model.related.dependencyNeighbors.every((item) => item.status === 'CONFIRMED')).toBe(true)
  })

  it('keeps file-only positions honest and never invents line zero', () => {
    const location = { sourceFileId: 'pom.xml' }

    expect(formatSourceLocation(location)).toEqual({
      path: 'pom.xml',
      start: 'Exact start position unavailable',
      end: null,
      symbol: null
    })
    expect(copySourceLocation(location)).toBe('pom.xml')
    expect(copySourceLocation({ sourceFileId: 'A.java', startLine: 7 })).toBe('A.java:7')
  })

  it('distinguishes known-empty related collections, unavailable data, and stale selections', () => {
    const empty = createSourceDetailModel({
      status: 'SUCCESS', modules: [], sourceFiles: [],
      components: [component('controller', 'example.UserController', 'controller.java')],
      endpoints: [], entryPoints: [], dependencies: [], diagnostics: []
    }, { type: 'component', id: 'controller' })
    expect(empty.related.dependencyNeighbors).toEqual([])
    expect(empty.related.sameFileEntryPoints).toEqual([])
    expect(empty.related.fileDiagnostics).toEqual([])

    const unavailable = createSourceDetailModel({
      status: 'PARTIAL',
      components: [component('controller', 'example.UserController', 'controller.java')]
    }, { type: 'component', id: 'controller' })
    expect(unavailable.related.dependencyNeighbors).toBeNull()
    expect(unavailable.related.sameFileEntryPoints).toBeNull()
    expect(unavailable.related.fileDiagnostics).toBeNull()
    expect(unavailable.sourceFile).toBeNull()

    const stale = createSourceDetailModel(report(), { type: 'endpoint', id: 'missing' })
    expect(stale.available).toBe(false)
    expect(stale.reason).toBe('stale')
  })
})

function report() {
  const path = 'api/src/main/java/example/UserController.java'
  return {
    schemaVersion: '1.2',
    status: 'PARTIAL',
    modules: [{ id: 'api', artifactId: 'api', baseDirectory: 'api', pomFileId: 'api/pom.xml' }],
    sourceFiles: [{
      id: path, moduleId: 'api', path, language: 'JAVA', location: { sourceFileId: path }
    }],
    components: [
      component('controller', 'example.UserController', path, 'REST_CONTROLLER'),
      component('helper', 'example.UserController.Helper', path, 'COMPONENT'),
      component('service', 'example.UserService', 'api/src/main/java/example/UserService.java', 'SERVICE')
    ],
    endpoints: [
      endpoint('get', 'GET', '/api/users', 'controller', path),
      endpoint('post', 'POST', '/api/users', 'controller', path)
    ],
    entryPoints: [{
      id: 'entry', moduleId: 'api', qualifiedName: 'example.UserController',
      kind: 'APPLICATION_ENTRY_POINT', framework: 'SPRING_BOOT',
      location: { sourceFileId: path, startLine: 5 }, evidence: []
    }],
    dependencies: [
      {
        id: 'confirmed', sourceId: 'controller', targetId: 'service',
        declaredTarget: 'example.UserService', kind: 'COMPONENT_INJECTION', status: 'CONFIRMED',
        location: { sourceFileId: path, startLine: 9 }, evidence: []
      },
      {
        id: 'unresolved', sourceId: 'controller', targetId: null,
        declaredTarget: 'example.Client', kind: 'COMPONENT_INJECTION', status: 'UNRESOLVED',
        location: { sourceFileId: path, startLine: 10 }, evidence: []
      },
      {
        id: 'maven', sourceId: 'api', targetId: 'other',
        declaredTarget: 'example:other', kind: 'MAVEN_DECLARATION', status: 'CONFIRMED',
        location: { sourceFileId: 'api/pom.xml' }, evidence: []
      }
    ],
    diagnostics: [{
      code: 'JAVA_PARSE_WARNING', severity: 'WARNING', stage: 'JAVA', fileId: path,
      message: 'A declaration was partially analyzed.'
    }]
  }
}

function component(id, qualifiedName, path, kind = 'REST_CONTROLLER') {
  return {
    id, moduleId: 'api', qualifiedName, kind, framework: 'SPRING_BOOT',
    location: { sourceFileId: path, startLine: 5, symbol: qualifiedName },
    evidence: [{
      type: 'SPRING_COMPONENT', ruleId: 'spring.component',
      location: { sourceFileId: path, startLine: 5 }, relatedLocations: []
    }]
  }
}

function endpoint(id, method, path, componentId, sourceFileId) {
  return {
    id, moduleId: 'api', componentId, httpMethod: method, path, unresolvedPath: false,
    handlerMethod: id, framework: 'SPRING_BOOT',
    location: {
      sourceFileId, startLine: 12, startColumn: 3, endLine: 14, endColumn: 3,
      symbol: `example.UserController#${id}`
    },
    evidence: [{
      type: 'SPRING_MVC_METHOD_MAPPING', ruleId: `spring.mvc.${method}`,
      location: { sourceFileId, startLine: 12, startColumn: 3 }, relatedLocations: []
    }]
  }
}
