import { describe, expect, it } from 'vitest'
import { createModuleExplorerModel } from '../lib/reportModules.js'

describe('createModuleExplorerModel', () => {
  it('uses explicit hierarchy and exact report relationships without inferring UI facts', () => {
    const model = createModuleExplorerModel(report())

    expect(model.hierarchySupported).toBe(true)
    expect(model.relationshipsSupported).toBe(true)
    expect(model.modules.map((module) => [module.label, module.depth])).toEqual([
      ['workspace', 0],
      ['api', 1],
      ['library', 1]
    ])
    const api = model.modules.find((module) => module.label === 'api')
    expect(api.gav).toBe('example:api:1')
    expect(api.counts).toEqual({ sourceFiles: 1, components: 1, endpoints: 0, entryPoints: 0 })
    expect(api.languageVersions[0]).toMatchObject({ label: 'Java', version: '21' })
    expect(api.frameworkVersions[0]).toMatchObject({ label: 'Spring Boot', version: '3.5.0' })
    expect(api.outgoing[0]).toMatchObject({ targetLabel: 'library' })
  })

  it('keeps unavailable collections distinct from known zero counts', () => {
    const model = createModuleExplorerModel({
      schemaVersion: '1.1',
      project: { name: 'legacy', buildSystem: 'MAVEN' },
      modules: [{ id: 'root', artifactId: 'root', sourceRoots: [] }]
    })

    expect(model.modules[0].counts.sourceFiles).toBeNull()
    expect(model.modules[0].sourceRoots).toEqual([])
    expect(model.relationshipsSupported).toBe(false)
  })
})

function report() {
  return {
    schemaVersion: '1.2',
    status: 'SUCCESS',
    project: { name: 'workspace', buildSystem: 'MAVEN' },
    modules: [
      { id: 'root', artifactId: 'workspace', aggregationParentModuleId: null, sourceRoots: [], frameworks: [], languageVersions: [], frameworkVersions: [], evidence: [] },
      { id: 'api', aggregationParentModuleId: 'root', groupId: 'example', artifactId: 'api', version: '1', packaging: 'jar', pomFileId: 'api/pom.xml', baseDirectory: 'api', sourceRoots: ['api/src/main/java'], frameworks: ['SPRING_BOOT'], languageVersions: [{ language: 'JAVA', version: '21', evidence: [] }], frameworkVersions: [{ framework: 'SPRING_BOOT', version: '3.5.0', evidence: [] }], evidence: [] },
      { id: 'library', aggregationParentModuleId: 'root', groupId: 'example', artifactId: 'library', version: '1', packaging: 'jar', sourceRoots: [], frameworks: [], languageVersions: [], frameworkVersions: [], evidence: [] }
    ],
    sourceFiles: [{ id: 'source:api', moduleId: 'api', language: 'JAVA' }],
    components: [{ id: 'component:controller', moduleId: 'api', qualifiedName: 'example.ApiController', kind: 'REST_CONTROLLER', framework: 'SPRING_BOOT' }],
    endpoints: [],
    entryPoints: [],
    dependencies: [{ id: 'dependency:api-library', sourceId: 'api', targetId: 'library', declaredTarget: 'example:library:1', kind: 'MAVEN_DECLARATION', status: 'CONFIRMED' }],
    diagnostics: []
  }
}
