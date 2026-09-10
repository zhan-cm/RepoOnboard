import { describe, expect, it } from 'vitest'
import { createOverviewModel, displayName } from '../lib/reportOverview.js'

describe('report overview model', () => {
  it('derives reproducible overview facts from report entities when summary is unavailable', () => {
    const overview = createOverviewModel({
      status: 'SUCCESS',
      project: { name: 'sample', buildSystem: 'MAVEN' },
      modules: [{ sourceRoots: ['src/main/java'], frameworks: ['SPRING_BOOT'] }],
      sourceFiles: [{ language: 'JAVA' }, { language: 'JAVA' }],
      components: [
        { kind: 'REST_CONTROLLER', framework: 'SPRING_BOOT' },
        { kind: 'SERVICE', framework: 'SPRING_BOOT' },
        { kind: 'COMPONENT', framework: 'SPRING_BOOT' }
      ],
      endpoints: [{ framework: 'SPRING_BOOT' }],
      entryPoints: [],
      dependencies: [{ id: 'dependency:one' }],
      diagnostics: []
    })

    expect(overview.repositoryName).toBe('sample')
    expect(overview.buildSystem).toBe('Maven')
    expect(overview.languages).toEqual(['Java'])
    expect(overview.frameworks).toEqual(['Spring Boot'])
    expect(overview.sourceRoots).toEqual(['src/main/java'])
    expect(overview.metrics.map((item) => item.value)).toEqual([1, 2, 3, 1, 0, 1])
    expect(overview.componentMetrics.map((item) => item.value)).toEqual([1, 1, 0, 0, 1])
  })

  it('uses the versioned summary as the canonical count source', () => {
    const overview = createOverviewModel({
      status: 'SUCCESS',
      summary: {
        moduleCount: 4,
        sourceFileCount: 18,
        componentCount: 9,
        controllerCount: 2,
        serviceCount: 3,
        repositoryCount: 2,
        configurationCount: 1,
        endpointCount: 7,
        entryPointCount: 1,
        dependencyCount: 6,
        coverageLimited: false,
        coverageLimitationCodes: []
      }
    })

    expect(overview.metrics.map((item) => item.value)).toEqual([4, 18, 9, 7, 1, 6])
    expect(overview.componentMetrics.map((item) => item.value)).toEqual([2, 3, 2, 1, 1])
  })

  it.each([
    ['SUCCESS', false, 'success', 'Analysis completed'],
    ['PARTIAL', true, 'warning', 'Coverage is limited'],
    ['FAILED', true, 'error', 'Analysis failed']
  ])('describes %s coverage explicitly', (status, limited, variant, heading) => {
    const overview = createOverviewModel({
      status,
      summary: { coverageLimited: limited, coverageLimitationCodes: [] },
      diagnostics: []
    })

    expect(overview.coverage.variant).toBe(variant)
    expect(overview.coverage.heading).toBe(heading)
  })

  it('keeps absent values unavailable instead of inventing defaults', () => {
    const overview = createOverviewModel({ project: {}, summary: {} })

    expect(overview.repositoryName).toBeNull()
    expect(overview.buildSystem).toBeNull()
    expect(overview.languages).toEqual([])
    expect(overview.frameworks).toEqual([])
    expect(overview.sourceRoots).toEqual([])
    expect(overview.entryPoints).toBeNull()
    expect(overview.metrics.every((item) => item.value === null)).toBe(true)
    expect(overview.status).toBe('UNKNOWN')
  })

  it('formats only explicit enum values for display', () => {
    expect(displayName('SPRING_BOOT')).toBe('Spring Boot')
    expect(displayName('CUSTOM_FRAMEWORK')).toBe('Custom Framework')
    expect(displayName('')).toBeNull()
  })
})
