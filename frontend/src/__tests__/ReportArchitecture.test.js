import { describe, expect, it } from 'vitest'
import {
  ARCHITECTURE_NODE_BUDGET,
  createArchitectureModel,
  createArchitectureScope
} from '../lib/reportArchitecture.js'

describe('createArchitectureModel', () => {
  it('builds only confirmed component injection edges and preserves evidence', () => {
    const model = createArchitectureModel(report())
    const scope = createArchitectureScope(model, 'api')

    expect(model.counts).toMatchObject({
      components: 3,
      confirmedRelations: 1,
      ambiguousRelations: 1,
      unresolvedRelations: 1
    })
    expect(scope.nodes.map((node) => node.label)).toEqual([
      'OrderController',
      'OrderService',
      'OrderRepository'
    ])
    expect(scope.edges).toHaveLength(1)
    expect(scope.edges[0]).toMatchObject({
      sourceId: 'controller',
      targetId: 'service',
      kind: 'COMPONENT_INJECTION',
      status: 'CONFIRMED',
      declaredTarget: 'example.OrderService'
    })
    expect(scope.edges[0].location.display).toBe('api/src/main/java/example/OrderController.java:12:5')
    expect(scope.edges[0].evidence[0].ruleId).toBe('SPRING_CONSTRUCTOR_INJECTION')
    expect(scope.counts).toMatchObject({
      components: 3,
      confirmedRelations: 1,
      ambiguousRelations: 1,
      unresolvedRelations: 1
    })
    expect(scope.nodes[0].outgoing).toHaveLength(1)
    expect(scope.nodes[1].incoming).toHaveLength(1)
  })

  it('never turns Maven declarations into component graph edges', () => {
    const model = createArchitectureModel(report())

    expect(model.confirmedRelations.some((edge) => edge.id === 'maven')).toBe(false)
  })

  it('distinguishes unavailable collections from known empty collections', () => {
    const unavailable = createArchitectureModel({ modules: [{ id: 'api', artifactId: 'api' }] })
    const empty = createArchitectureModel({
      modules: [{ id: 'api', artifactId: 'api' }],
      components: [], dependencies: [], diagnostics: []
    })

    expect(unavailable.collectionAvailable).toBe(false)
    expect(unavailable.counts.components).toBeNull()
    expect(unavailable.counts.confirmedRelations).toBeNull()
    expect(createArchitectureScope(unavailable, 'api').counts.unresolvedRelations).toBeNull()
    expect(empty.collectionAvailable).toBe(true)
    expect(empty.counts.components).toBe(0)
  })

  it('refuses to render a misleading partial graph above the node budget', () => {
    const components = Array.from({ length: ARCHITECTURE_NODE_BUDGET + 1 }, (_, index) => ({
      id: `component-${index}`,
      moduleId: 'api',
      qualifiedName: `example.Component${index}`,
      kind: 'COMPONENT',
      framework: 'SPRING_BOOT',
      evidence: []
    }))
    const model = createArchitectureModel({
      modules: [{ id: 'api', artifactId: 'api' }],
      components,
      dependencies: [],
      diagnostics: []
    })

    expect(createArchitectureScope(model, 'api').overBudget).toBe(true)
  })
})

function report() {
  const component = (id, qualifiedName, kind) => ({
    id,
    moduleId: 'api',
    qualifiedName,
    kind,
    framework: 'SPRING_BOOT',
    location: {
      sourceFileId: `api/src/main/java/${qualifiedName.replaceAll('.', '/')}.java`,
      startLine: 7,
      symbol: qualifiedName
    },
    evidence: [{
      type: 'SPRING_COMPONENT',
      ruleId: 'SPRING_COMPONENT_STEREOTYPE',
      location: { sourceFileId: `api/src/main/java/${qualifiedName.replaceAll('.', '/')}.java`, startLine: 7 },
      relatedLocations: []
    }]
  })
  return {
    schemaVersion: '1.2',
    status: 'PARTIAL',
    modules: [{ id: 'api', artifactId: 'api', pomFileId: 'api/pom.xml' }],
    components: [
      component('controller', 'example.OrderController', 'REST_CONTROLLER'),
      component('service', 'example.OrderService', 'SERVICE'),
      component('repository', 'example.OrderRepository', 'REPOSITORY')
    ],
    dependencies: [
      {
        id: 'confirmed', sourceId: 'controller', targetId: 'service',
        declaredTarget: 'example.OrderService', kind: 'COMPONENT_INJECTION', status: 'CONFIRMED',
        location: { sourceFileId: 'api/src/main/java/example/OrderController.java', startLine: 12, startColumn: 5 },
        evidence: [{
          type: 'CONSTRUCTOR_PARAMETER', ruleId: 'SPRING_CONSTRUCTOR_INJECTION',
          location: { sourceFileId: 'api/src/main/java/example/OrderController.java', startLine: 12 },
          relatedLocations: []
        }]
      },
      {
        id: 'ambiguous', sourceId: 'service', targetId: null,
        declaredTarget: 'example.PaymentClient', kind: 'COMPONENT_INJECTION', status: 'AMBIGUOUS', evidence: []
      },
      {
        id: 'unresolved', sourceId: 'repository', targetId: null,
        declaredTarget: 'example.DataSource', kind: 'COMPONENT_INJECTION', status: 'UNRESOLVED', evidence: []
      },
      {
        id: 'maven', sourceId: 'api', targetId: 'library',
        declaredTarget: 'example:library:1', kind: 'MAVEN_DECLARATION', status: 'CONFIRMED', evidence: []
      }
    ],
    diagnostics: []
  }
}
