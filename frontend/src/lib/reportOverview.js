const DISPLAY_NAMES = Object.freeze({
  APPLICATION_ENTRY_POINT: 'Application entry point',
  JAVA: 'Java',
  MAVEN: 'Maven',
  SPRING_BOOT: 'Spring Boot'
})

export function displayName(value) {
  if (typeof value !== 'string' || value.trim() === '') return null
  const normalized = value.trim()
  return DISPLAY_NAMES[normalized]
    ?? normalized.toLowerCase().split('_').map(capitalize).join(' ')
}

export function createOverviewModel(report) {
  const sourceFiles = list(report?.sourceFiles)
  const modules = list(report?.modules)
  const components = list(report?.components)
  const endpoints = list(report?.endpoints)
  const entryPoints = list(report?.entryPoints)
  const dependencies = list(report?.dependencies)
  const diagnostics = list(report?.diagnostics)
  const summary = object(report?.summary)
  const status = text(report?.status) ?? text(summary?.analysisStatus) ?? 'UNKNOWN'
  const limitationCodes = unique(summary?.coverageLimitationCodes)
  const significantDiagnostics = diagnostics?.filter((item) => item?.severity !== 'INFO') ?? null

  const componentCount = count(summary, 'componentCount', components)
  const controllerCount = roleCount(summary, 'controllerCount', components, ['CONTROLLER', 'REST_CONTROLLER'])
  const serviceCount = roleCount(summary, 'serviceCount', components, ['SERVICE'])
  const repositoryCount = roleCount(summary, 'repositoryCount', components, ['REPOSITORY'])
  const configurationCount = roleCount(summary, 'configurationCount', components, ['CONFIGURATION'])

  return {
    repositoryName: text(report?.project?.name),
    buildSystem: displayName(report?.project?.buildSystem),
    status,
    statusTone: statusTone(status),
    coverage: coverage(status, summary, limitationCodes, significantDiagnostics),
    metrics: [
      metric('Modules', count(summary, 'moduleCount', modules), 'Maven modules reported'),
      metric('Source files', count(summary, 'sourceFileCount', sourceFiles), 'Analyzed source files'),
      metric('Components', componentCount, 'Framework components'),
      metric('Endpoints', count(summary, 'endpointCount', endpoints), 'HTTP endpoints'),
      metric('Entry points', count(summary, 'entryPointCount', entryPoints), 'Application starts'),
      metric('Dependencies', count(summary, 'dependencyCount', dependencies), 'Component relationships')
    ],
    componentMetrics: [
      metric('Controllers', controllerCount),
      metric('Services', serviceCount),
      metric('Repositories', repositoryCount),
      metric('Configurations', configurationCount),
      metric('Other components', remainder(componentCount, [
        controllerCount,
        serviceCount,
        repositoryCount,
        configurationCount
      ]))
    ],
    languages: unique(sourceFiles?.map((item) => item?.language)).map(displayName).filter(Boolean),
    frameworks: unique([
      ...(modules?.flatMap((item) => list(item?.frameworks) ?? []) ?? []),
      ...(components?.map((item) => item?.framework) ?? []),
      ...(endpoints?.map((item) => item?.framework) ?? []),
      ...(entryPoints?.map((item) => item?.framework) ?? [])
    ]).map(displayName).filter(Boolean),
    sourceRoots: unique(modules?.flatMap((item) => list(item?.sourceRoots) ?? []) ?? []),
    entryPoints: entryPoints?.map((item) => ({
      id: text(item?.id) ?? text(item?.qualifiedName),
      name: text(item?.qualifiedName),
      kind: displayName(item?.kind),
      moduleId: text(item?.moduleId),
      location: formatLocation(item?.location)
    })) ?? null,
    diagnostics: significantDiagnostics?.map((item) => ({
      code: text(item?.code),
      severity: text(item?.severity),
      message: text(item?.message),
      stage: text(item?.stage)
    })) ?? null
  }
}

function metric(label, value, description = '') {
  return { label, value, description }
}

function coverage(status, summary, limitationCodes, diagnostics) {
  const limited = typeof summary?.coverageLimited === 'boolean'
    ? summary.coverageLimited
    : status !== 'SUCCESS' || limitationCodes.length > 0

  if (status === 'FAILED') {
    return {
      variant: 'error',
      heading: 'Analysis failed',
      message: 'The report contains terminal diagnostics. Counts may be incomplete or unavailable.',
      limited,
      limitationCodes,
      diagnosticCount: diagnostics?.length ?? null
    }
  }
  if (status === 'PARTIAL' || limited) {
    return {
      variant: 'warning',
      heading: 'Coverage is limited',
      message: 'RepoOnboard kept the facts it could confirm. Review the reported limitations before relying on totals.',
      limited: true,
      limitationCodes,
      diagnosticCount: diagnostics?.length ?? null
    }
  }
  if (status === 'SUCCESS') {
    return {
      variant: 'success',
      heading: 'Analysis completed',
      message: 'The report contains no declared coverage limitations.',
      limited: false,
      limitationCodes,
      diagnosticCount: diagnostics?.length ?? null
    }
  }
  return {
    variant: 'neutral',
    heading: 'Analysis status unavailable',
    message: 'This report does not provide a recognized analysis status.',
    limited,
    limitationCodes,
    diagnosticCount: diagnostics?.length ?? null
  }
}

function roleCount(summary, key, components, kinds) {
  const supplied = nonNegativeInteger(summary?.[key])
  if (supplied !== null) return supplied
  if (components === null) return null
  return components.filter((item) => kinds.includes(item?.kind)).length
}

function count(summary, key, values) {
  return nonNegativeInteger(summary?.[key]) ?? values?.length ?? null
}

function remainder(total, parts) {
  if (total === null || parts.some((value) => value === null)) return null
  return Math.max(0, total - parts.reduce((sum, value) => sum + value, 0))
}

function formatLocation(location) {
  const sourceFileId = text(location?.sourceFileId)
  if (!sourceFileId) return null
  const startLine = Number.isInteger(location?.startLine) && location.startLine > 0
    ? location.startLine
    : null
  return startLine === null ? sourceFileId : `${sourceFileId}:${startLine}`
}

function statusTone(status) {
  if (status === 'SUCCESS') return 'success'
  if (status === 'PARTIAL') return 'warning'
  if (status === 'FAILED') return 'danger'
  return 'neutral'
}

function unique(values) {
  if (!Array.isArray(values)) return []
  return [...new Set(values.filter((value) => typeof value === 'string' && value.trim() !== ''))]
    .sort()
}

function list(value) {
  return Array.isArray(value) ? value : null
}

function object(value) {
  return value !== null && typeof value === 'object' && !Array.isArray(value) ? value : null
}

function text(value) {
  return typeof value === 'string' && value.trim() !== '' ? value.trim() : null
}

function nonNegativeInteger(value) {
  return Number.isInteger(value) && value >= 0 ? value : null
}

function capitalize(value) {
  return value.charAt(0).toUpperCase() + value.slice(1)
}
