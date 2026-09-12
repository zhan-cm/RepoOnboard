import { describe, expect, it } from 'vitest'
import { createStartHereModel, visibleStartHereItems } from '../lib/reportStartHere.js'

describe('reportStartHere', () => {
  it('preserves backend order and exact Component and Endpoint joins', () => {
    const model = createStartHereModel(report(), guide())

    expect(model.items.map((item) => item.sourceFileId)).toEqual([
      'src/main/java/example/OrderController.java',
      'pom.xml'
    ])
    expect(model.items[0].moduleLabel).toBe('orders')
    expect(model.items[0].primaryReason.message).toBe('Exposes 1 HTTP endpoint')
    expect(model.items[0].relatedComponents.map((item) => item.id)).toEqual(['controller'])
    expect(model.items[0].relatedEndpoints.map((item) => item.id)).toEqual(['get-orders'])
    expect(visibleStartHereItems(model, false)).toHaveLength(1)
    expect(visibleStartHereItems(model, true)).toHaveLength(2)
  })

  it('keeps partial empty guides distinct and retains their coverage notice', () => {
    const partialReport = { ...report(), status: 'PARTIAL' }
    const partialGuide = {
      ...guide(),
      analysisStatus: 'PARTIAL',
      coverageLimited: true,
      coverageLimitationCodes: ['JAVA_PARSE_FAILED'],
      coverageNotice: 'Recommendations are based on partial analysis and may omit important files.',
      totalItemCount: 0,
      expandable: false,
      hiddenItemCount: 0,
      items: []
    }

    const model = createStartHereModel(partialReport, partialGuide)

    expect(model.items).toEqual([])
    expect(model.coverageLimited).toBe(true)
    expect(model.coverageLimitationCodes).toEqual(['JAVA_PARSE_FAILED'])
  })

  it('rejects mismatched snapshots and reasons without evidence', () => {
    expect(() => createStartHereModel(report(), {
      ...guide(), projectId: 'project:other'
    })).toThrow('different projects')
    const missingEvidence = guide()
    missingEvidence.items[0].reasons[0].evidence = []
    expect(() => createStartHereModel(report(), missingEvidence))
      .toThrow('must retain supporting entities and evidence')
  })
})

function report() {
  const sourceFileId = 'src/main/java/example/OrderController.java'
  return {
    schemaVersion: '1.2',
    status: 'SUCCESS',
    project: { id: 'project:orders', name: 'orders', buildSystem: 'MAVEN' },
    modules: [{ id: 'module:orders', artifactId: 'orders', pomFileId: 'pom.xml' }],
    sourceFiles: [{ id: sourceFileId, path: sourceFileId, moduleId: 'module:orders', language: 'JAVA' }],
    components: [{
      id: 'controller', moduleId: 'module:orders', qualifiedName: 'example.OrderController',
      kind: 'REST_CONTROLLER'
    }],
    endpoints: [{
      id: 'get-orders', moduleId: 'module:orders', componentId: 'controller',
      httpMethod: 'GET', path: '/api/orders', unresolvedPath: false, handlerMethod: 'list'
    }],
    entryPoints: [], dependencies: [], diagnostics: []
  }
}

function guide() {
  const sourceFileId = 'src/main/java/example/OrderController.java'
  return {
    schemaVersion: '1.0',
    reportSchemaVersion: '1.2',
    projectId: 'project:orders',
    analysisStatus: 'SUCCESS',
    coverageLimited: false,
    coverageLimitationCodes: [],
    coverageNotice: null,
    defaultLimit: 1,
    totalItemCount: 2,
    expandable: true,
    hiddenItemCount: 1,
    items: [
      {
        sourceFileId,
        moduleId: 'module:orders',
        reasons: [{
          kind: 'HTTP_ENDPOINT_EXPOSURE',
          message: 'Exposes 1 HTTP endpoint',
          factCount: 1,
          dependencyDistance: null,
          supportingEntityIds: ['controller', 'get-orders'],
          evidence: [evidence(sourceFileId, 12)]
        }]
      },
      {
        sourceFileId: 'pom.xml',
        moduleId: 'module:orders',
        reasons: [{
          kind: 'ROOT_BUILD_FILE',
          message: 'Root Maven build file',
          factCount: 1,
          dependencyDistance: null,
          supportingEntityIds: ['module:orders'],
          evidence: [evidence('pom.xml', null)]
        }]
      }
    ]
  }
}

function evidence(sourceFileId, startLine) {
  return {
    type: 'SOURCE_FACT',
    ruleId: 'test.source',
    location: { sourceFileId, startLine },
    relatedLocations: []
  }
}
