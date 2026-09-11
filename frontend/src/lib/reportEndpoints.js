import { displayName } from './reportOverview.js'

const METHOD_ORDER = [
  'GET',
  'HEAD',
  'POST',
  'PUT',
  'PATCH',
  'DELETE',
  'OPTIONS',
  'TRACE',
  'ANY',
  'UNRESOLVED'
]

const TYPE_MAPPING = 'SPRING_MVC_TYPE_MAPPING'
const METHOD_MAPPING = 'SPRING_MVC_METHOD_MAPPING'

export function createApiModel(report) {
  const rawModules = list(report?.modules)
  const rawComponents = list(report?.components)
  const rawEndpoints = list(report?.endpoints)
  const rawDiagnostics = list(report?.diagnostics)

  const moduleById = new Map((rawModules ?? []).map((module) => {
    const id = text(module?.id)
    return [id, {
      id,
      label: text(module?.artifactId) ?? text(module?.pomFileId) ?? id ?? 'Unnamed module',
      linked: true
    }]
  }).filter(([id]) => id))

  const componentById = new Map((rawComponents ?? []).map((component) => {
    const id = text(component?.id)
    const qualifiedName = text(component?.qualifiedName)
    return [id, {
      id,
      qualifiedName,
      label: simpleName(qualifiedName) ?? 'Unnamed controller',
      kind: text(component?.kind),
      kindLabel: displayName(component?.kind),
      framework: displayName(component?.framework),
      location: normalizeLocation(component?.location),
      evidence: normalizeEvidence(component?.evidence)
    }]
  }).filter(([id]) => id))

  const endpoints = (rawEndpoints ?? []).map((endpoint, index) => {
    const moduleId = text(endpoint?.moduleId)
    const componentId = text(endpoint?.componentId)
    const method = text(endpoint?.httpMethod)
    const path = text(endpoint?.path)
    const unresolvedPath = endpoint?.unresolvedPath === true
    const controller = componentById.get(componentId) ?? null
    const conditions = normalizeConditions(endpoint?.conditions)
    const evidence = normalizeEvidence(endpoint?.evidence)
    const location = normalizeLocation(endpoint?.location)
    const handlerMethod = text(endpoint?.handlerMethod)
    return {
      id: text(endpoint?.id) ?? `reported-endpoint-${index}`,
      moduleId,
      module: null,
      moduleLabel: null,
      componentId,
      controller,
      controllerLabel: controller?.label ?? null,
      controllerQualifiedName: controller?.qualifiedName ?? null,
      controllerLinked: Boolean(controller),
      method,
      methodLabel: method ?? 'Method unavailable',
      path,
      unresolvedPath,
      pathState: path ? 'resolved' : unresolvedPath ? 'unresolved' : 'unavailable',
      pathLabel: path ?? (unresolvedPath ? 'Unresolved path' : 'Path unavailable'),
      handlerMethod,
      handlerLabel: controller?.label && handlerMethod
        ? `${controller.label}.${handlerMethod}`
        : handlerMethod ?? 'Handler unavailable',
      conditions,
      conditionCount: conditionCount(conditions),
      framework: displayName(endpoint?.framework),
      location,
      evidence,
      typeEvidence: evidence?.filter((item) => item.type === TYPE_MAPPING) ?? null,
      methodEvidence: evidence?.filter((item) => item.type === METHOD_MAPPING) ?? null,
      otherEvidence: evidence?.filter(
        (item) => item.type !== TYPE_MAPPING && item.type !== METHOD_MAPPING) ?? null
    }
  })

  for (const endpoint of endpoints) {
    if (endpoint.moduleId && !moduleById.has(endpoint.moduleId)) {
      moduleById.set(endpoint.moduleId, {
        id: endpoint.moduleId,
        label: endpoint.moduleId,
        linked: false
      })
    }
    endpoint.module = moduleById.get(endpoint.moduleId) ?? null
    endpoint.moduleLabel = endpoint.module?.label ?? 'Module unavailable'
  }
  endpoints.sort(endpointOrder)

  const modules = [...moduleById.values()]
    .map((module) => ({
      ...module,
      endpointCount: endpoints.filter((endpoint) => endpoint.moduleId === module.id).length
    }))
    .sort((left, right) => left.label.localeCompare(right.label) || left.id.localeCompare(right.id))
  const methodOptions = methodCounts(endpoints)
  const diagnostics = rawDiagnostics?.filter(isApiDiagnostic).map((diagnostic) => ({
    code: text(diagnostic?.code),
    severity: text(diagnostic?.severity),
    stage: text(diagnostic?.stage),
    moduleId: text(diagnostic?.moduleId),
    fileId: text(diagnostic?.fileId),
    message: text(diagnostic?.message),
    location: normalizeLocation(diagnostic?.location)
  })) ?? null

  return {
    repositoryName: text(report?.project?.name) ?? 'Repository name unavailable',
    schemaVersion: text(report?.schemaVersion),
    status: text(report?.status) ?? 'UNKNOWN',
    collectionAvailable: rawEndpoints !== null,
    modules,
    endpoints,
    methodOptions,
    diagnostics,
    counts: {
      modules: modules.length,
      endpoints: rawEndpoints?.length ?? null,
      anyMethods: rawEndpoints === null ? null : endpoints.filter((item) => item.method === 'ANY').length,
      unresolvedMethods: rawEndpoints === null
        ? null
        : endpoints.filter((item) => item.method === 'UNRESOLVED').length,
      unresolvedPaths: rawEndpoints === null
        ? null
        : endpoints.filter((item) => item.unresolvedPath).length,
      unresolvedConditions: rawEndpoints === null
        ? null
        : endpoints.filter((item) => item.conditions?.unresolved).length,
      brokenControllerLinks: rawEndpoints === null
        ? null
        : endpoints.filter((item) => !item.controllerLinked).length
    }
  }
}

export function createApiScope(model, exploration = {}) {
  const availableModuleIds = new Set(model.modules.map((module) => module.id))
  const moduleId = exploration.moduleId === 'all' || !availableModuleIds.has(exploration.moduleId)
    ? 'all'
    : exploration.moduleId
  const availableMethods = new Set(model.methodOptions.map((option) => option.method))
  const method = exploration.method === 'all' || !availableMethods.has(exploration.method)
    ? 'all'
    : exploration.method
  const query = typeof exploration.query === 'string' ? exploration.query.trim() : ''

  const moduleEndpoints = moduleId === 'all'
    ? model.endpoints
    : model.endpoints.filter((endpoint) => endpoint.moduleId === moduleId)
  const methodEndpoints = method === 'all'
    ? moduleEndpoints
    : moduleEndpoints.filter((endpoint) => endpoint.method === method)
  const endpoints = query
    ? methodEndpoints.filter((endpoint) => matchesEndpoint(endpoint, query))
    : methodEndpoints

  return {
    endpoints,
    exploration: { moduleId, method, query },
    filtersActive: moduleId !== 'all' || method !== 'all' || Boolean(query),
    counts: {
      reportedEndpoints: model.endpoints.length,
      shownEndpoints: endpoints.length,
      filteredEndpoints: model.endpoints.length - endpoints.length,
      anyMethods: endpoints.filter((item) => item.method === 'ANY').length,
      unresolvedMethods: endpoints.filter((item) => item.method === 'UNRESOLVED').length,
      unresolvedPaths: endpoints.filter((item) => item.unresolvedPath).length,
      unresolvedConditions: endpoints.filter((item) => item.conditions?.unresolved).length
    }
  }
}

export function defaultApiExploration() {
  return { moduleId: 'all', method: 'all', query: '' }
}

function normalizeConditions(value) {
  if (!value || typeof value !== 'object') return null
  return {
    params: list(value.params),
    headers: list(value.headers),
    consumes: list(value.consumes),
    produces: list(value.produces),
    unresolved: value.unresolved === true
  }
}

function conditionCount(conditions) {
  if (!conditions) return null
  return ['params', 'headers', 'consumes', 'produces']
    .reduce((total, key) => total + (conditions[key]?.length ?? 0), 0)
}

function normalizeEvidence(values) {
  const supplied = list(values)
  if (supplied === null) return null
  return supplied.map((item) => ({
    type: text(item?.type),
    typeLabel: displayName(item?.type) ?? 'Evidence',
    ruleId: text(item?.ruleId),
    location: normalizeLocation(item?.location),
    relatedLocations: (list(item?.relatedLocations) ?? [])
      .map(normalizeLocation)
      .filter(Boolean)
  }))
}

function normalizeLocation(value) {
  if (!value || typeof value !== 'object') return null
  const sourceFileId = text(value.sourceFileId)
  if (!sourceFileId) return null
  const startLine = positiveInteger(value.startLine)
  const startColumn = positiveInteger(value.startColumn)
  const endLine = positiveInteger(value.endLine)
  const endColumn = positiveInteger(value.endColumn)
  const line = startLine === null ? '' : `:${startLine}`
  const column = startColumn === null ? '' : `:${startColumn}`
  return {
    sourceFileId,
    startLine,
    startColumn,
    endLine,
    endColumn,
    symbol: text(value.symbol),
    display: `${sourceFileId}${line}${column}`
  }
}

function methodCounts(endpoints) {
  const counts = new Map()
  for (const endpoint of endpoints) {
    if (endpoint.method) counts.set(endpoint.method, (counts.get(endpoint.method) ?? 0) + 1)
  }
  return [...counts.entries()]
    .map(([method, count]) => ({ method, count }))
    .sort((left, right) => methodRank(left.method) - methodRank(right.method)
      || left.method.localeCompare(right.method))
}

function endpointOrder(left, right) {
  return left.moduleLabel.localeCompare(right.moduleLabel)
    || Number(left.pathState !== 'resolved') - Number(right.pathState !== 'resolved')
    || left.pathLabel.localeCompare(right.pathLabel)
    || methodRank(left.method) - methodRank(right.method)
    || (left.controllerQualifiedName ?? '').localeCompare(right.controllerQualifiedName ?? '')
    || (left.handlerMethod ?? '').localeCompare(right.handlerMethod ?? '')
    || left.id.localeCompare(right.id)
}

function methodRank(method) {
  const index = METHOD_ORDER.indexOf(method)
  return index === -1 ? METHOD_ORDER.length : index
}

function matchesEndpoint(endpoint, query) {
  const normalized = query.toLocaleLowerCase()
  return [
    endpoint.pathLabel,
    endpoint.method,
    endpoint.handlerMethod,
    endpoint.controllerLabel,
    endpoint.controllerQualifiedName,
    endpoint.moduleLabel,
    endpoint.location?.sourceFileId,
    endpoint.location?.symbol
  ].some((value) => typeof value === 'string' && value.toLocaleLowerCase().includes(normalized))
}

function isApiDiagnostic(value) {
  const code = text(value?.code) ?? ''
  const stage = text(value?.stage) ?? ''
  return code.startsWith('SPRING_MVC_')
    || code.startsWith('SPRING_ENDPOINT_')
    || stage === 'SPRING_MVC_MAPPING'
    || stage === 'SPRING_ENDPOINT'
}

function simpleName(value) {
  const normalized = text(value)
  return normalized?.split(/[.$]/).filter(Boolean).at(-1) ?? null
}

function list(value) {
  return Array.isArray(value) ? value : null
}

function text(value) {
  return typeof value === 'string' && value.trim() !== '' ? value.trim() : null
}

function positiveInteger(value) {
  return Number.isInteger(value) && value > 0 ? value : null
}
