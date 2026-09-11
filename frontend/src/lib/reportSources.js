import { displayName } from './reportOverview.js'

export function createSourceDetailModel(report, selection) {
  const modules = collection(report?.modules)
  const sourceFiles = collection(report?.sourceFiles)
  const components = collection(report?.components)
  const endpoints = collection(report?.endpoints)
  const entryPoints = collection(report?.entryPoints)
  const dependencies = collection(report?.dependencies)
  const diagnostics = collection(report?.diagnostics)

  const moduleById = new Map((modules ?? []).map(moduleView).filter(Boolean).map((item) => [item.id, item]))
  const componentViews = (components ?? []).map(componentView).filter(Boolean)
  const componentById = new Map(componentViews.map((item) => [item.id, item]))
  const endpointViews = (endpoints ?? []).map((item) => endpointView(item, componentById)).filter(Boolean)
  const endpointById = new Map(endpointViews.map((item) => [item.id, item]))
  const sourceFileViews = (sourceFiles ?? []).map(sourceFileView).filter(Boolean)
  const sourceFileByIdentity = new Map()
  for (const sourceFile of sourceFileViews) {
    sourceFileByIdentity.set(sourceFile.id, sourceFile)
    sourceFileByIdentity.set(sourceFile.path, sourceFile)
  }

  const entityType = selection?.type === 'component' || selection?.type === 'endpoint'
    ? selection.type
    : null
  const entity = entityType === 'component'
    ? componentById.get(text(selection?.id)) ?? null
    : entityType === 'endpoint'
      ? endpointById.get(text(selection?.id)) ?? null
      : null

  if (!entity) {
    return {
      available: false,
      reason: entityType ? 'stale' : 'missing-selection',
      entityType,
      status: text(report?.status) ?? 'UNKNOWN',
      schemaVersion: text(report?.schemaVersion)
    }
  }

  const location = entity.location
  const module = entity.moduleId ? moduleById.get(entity.moduleId) ?? null : null
  const sourceFile = location ? sourceFileByIdentity.get(location.sourceFileId) ?? null : null
  const contextComponent = entityType === 'component'
    ? entity
    : entity.controller
  const path = location?.sourceFileId ?? null

  return {
    available: true,
    entityType,
    status: text(report?.status) ?? 'UNKNOWN',
    schemaVersion: text(report?.schemaVersion),
    entity,
    contextComponent,
    location,
    locationText: formatSourceLocation(location),
    copyLocationText: copySourceLocation(location),
    module,
    sourceFile,
    evidence: entity.evidence,
    joins: {
      modulesAvailable: modules !== null,
      sourceFilesAvailable: sourceFiles !== null
    },
    related: {
      owningController: entityType === 'endpoint' ? entity.controller : null,
      controllerAvailable: entityType !== 'endpoint' || entity.controller !== null,
      sameFileComponents: components === null || !path
        ? null
        : componentViews.filter((item) => item.location?.sourceFileId === path),
      controllerEndpoints: endpoints === null || !contextComponent
        ? null
        : endpointViews.filter((item) => item.componentId === contextComponent.id && item.id !== entity.id),
      dependencyNeighbors: dependencyNeighbors(dependencies, contextComponent, componentById),
      sameFileEntryPoints: entryPoints === null || !path
        ? null
        : entryPoints.map(entryPointView).filter(Boolean)
          .filter((item) => item.location?.sourceFileId === path),
      fileDiagnostics: diagnostics === null || !path
        ? null
        : diagnostics.map(diagnosticView).filter(Boolean)
          .filter((item) => item.fileId === path || item.location?.sourceFileId === path)
    }
  }
}

export function formatSourceLocation(value) {
  const location = normalizeLocation(value)
  if (!location) return null
  const start = location.startLine
    ? `Line ${location.startLine}${location.startColumn ? `, column ${location.startColumn}` : ''}`
    : 'Exact start position unavailable'
  const end = location.endLine
    ? `Line ${location.endLine}${location.endColumn ? `, column ${location.endColumn}` : ''}`
    : null
  return { path: location.sourceFileId, start, end, symbol: location.symbol }
}

export function copySourceLocation(value) {
  const location = normalizeLocation(value)
  if (!location) return null
  if (!location.startLine) return location.sourceFileId
  if (!location.startColumn) return `${location.sourceFileId}:${location.startLine}`
  return `${location.sourceFileId}:${location.startLine}:${location.startColumn}`
}

function dependencyNeighbors(dependencies, component, componentById) {
  if (dependencies === null || !component) return dependencies === null ? null : []
  return dependencies
    .filter((dependency) => dependency?.status === 'CONFIRMED')
    .map((dependency) => {
      const source = componentById.get(text(dependency?.sourceId)) ?? null
      const target = componentById.get(text(dependency?.targetId)) ?? null
      if (!source || !target) return null
      if (source.id !== component.id && target.id !== component.id) return null
      const outgoing = source.id === component.id
      return {
        id: text(dependency?.id) ?? `${source.id}:${target.id}`,
        direction: outgoing ? 'OUTGOING' : 'INCOMING',
        kind: text(dependency?.kind) ?? 'UNAVAILABLE',
        status: 'CONFIRMED',
        declaredTarget: text(dependency?.declaredTarget),
        component: outgoing ? target : source,
        location: normalizeLocation(dependency?.location),
        evidence: normalizeEvidence(dependency?.evidence)
      }
    })
    .filter(Boolean)
    .sort((left, right) => left.direction.localeCompare(right.direction)
      || left.component.qualifiedName.localeCompare(right.component.qualifiedName)
      || left.id.localeCompare(right.id))
}

function moduleView(value) {
  const id = text(value?.id)
  if (!id) return null
  return {
    id,
    label: text(value?.artifactId) ?? id,
    baseDirectory: text(value?.baseDirectory),
    pomFileId: text(value?.pomFileId)
  }
}

function sourceFileView(value) {
  const id = text(value?.id)
  const path = text(value?.path)
  if (!id || !path) return null
  return {
    id,
    moduleId: text(value?.moduleId),
    path,
    language: displayName(value?.language),
    location: normalizeLocation(value?.location)
  }
}

function componentView(value) {
  const id = text(value?.id)
  const qualifiedName = text(value?.qualifiedName)
  if (!id || !qualifiedName) return null
  return {
    id,
    moduleId: text(value?.moduleId),
    qualifiedName,
    label: simpleName(qualifiedName),
    name: text(value?.name),
    kind: text(value?.kind) ?? 'UNAVAILABLE',
    kindLabel: displayName(value?.kind) ?? 'Unavailable',
    framework: displayName(value?.framework),
    location: normalizeLocation(value?.location),
    evidence: normalizeEvidence(value?.evidence)
  }
}

function endpointView(value, componentById) {
  const id = text(value?.id)
  if (!id) return null
  const componentId = text(value?.componentId)
  const method = text(value?.httpMethod) ?? 'UNAVAILABLE'
  const path = text(value?.path)
  return {
    id,
    moduleId: text(value?.moduleId),
    componentId,
    controller: componentId ? componentById.get(componentId) ?? null : null,
    method,
    methodLabel: method,
    path,
    pathLabel: path ?? (value?.unresolvedPath === true ? 'Unresolved path' : 'Path unavailable'),
    unresolvedPath: value?.unresolvedPath === true,
    handlerMethod: text(value?.handlerMethod),
    framework: displayName(value?.framework),
    location: normalizeLocation(value?.location),
    evidence: normalizeEvidence(value?.evidence)
  }
}

function entryPointView(value) {
  const id = text(value?.id)
  const qualifiedName = text(value?.qualifiedName)
  if (!id || !qualifiedName) return null
  return {
    id,
    qualifiedName,
    kind: text(value?.kind),
    framework: displayName(value?.framework),
    location: normalizeLocation(value?.location)
  }
}

function diagnosticView(value) {
  const code = text(value?.code)
  if (!code) return null
  return {
    code,
    severity: text(value?.severity),
    stage: text(value?.stage),
    message: text(value?.message),
    fileId: text(value?.fileId),
    location: normalizeLocation(value?.location)
  }
}

function normalizeEvidence(value) {
  if (!Array.isArray(value)) return null
  return value.map((item, index) => ({
    index,
    type: text(item?.type),
    ruleId: text(item?.ruleId),
    location: normalizeLocation(item?.location),
    relatedLocations: (collection(item?.relatedLocations) ?? []).map(normalizeLocation).filter(Boolean)
  }))
}

function normalizeLocation(value) {
  const sourceFileId = text(value?.sourceFileId)
  if (!sourceFileId) return null
  return {
    sourceFileId,
    startLine: positiveInteger(value?.startLine),
    startColumn: positiveInteger(value?.startColumn),
    endLine: positiveInteger(value?.endLine),
    endColumn: positiveInteger(value?.endColumn),
    symbol: text(value?.symbol)
  }
}

function positiveInteger(value) {
  return Number.isInteger(value) && value > 0 ? value : null
}

function simpleName(value) {
  return value.split('.').at(-1) || value
}

function collection(value) {
  return Array.isArray(value) ? value : null
}

function text(value) {
  return typeof value === 'string' && value.trim() ? value : null
}
