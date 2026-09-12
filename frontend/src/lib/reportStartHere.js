import { displayName } from './reportOverview.js'

export const CURRENT_START_HERE_SCHEMA = '1.0'

const REASON_LABELS = Object.freeze({
  ROOT_BUILD_FILE: 'Build file',
  APPLICATION_ENTRY_POINT: 'Entry point',
  CONFIGURATION_COMPONENT: 'Configuration',
  HTTP_ENDPOINT_EXPOSURE: 'HTTP endpoints',
  CONFIRMED_DEPENDENCY_PATH: 'Dependency path',
  CONFIRMED_INCOMING_DEPENDENCIES: 'Incoming dependencies',
  CONFIRMED_OUTGOING_DEPENDENCIES: 'Outgoing dependencies'
})

export function createStartHereModel(report, guide) {
  if (!report || typeof report !== 'object' || !guide || typeof guide !== 'object') {
    throw new Error('Start Here requires a report and reading guide response.')
  }
  const schemaVersion = requiredText(guide.schemaVersion, 'Start Here schemaVersion')
  if (major(schemaVersion) !== major(CURRENT_START_HERE_SCHEMA)) {
    throw new Error(`Unsupported Start Here schema (${schemaVersion})`)
  }
  const reportSchemaVersion = requiredText(
    guide.reportSchemaVersion, 'Start Here reportSchemaVersion')
  if (reportSchemaVersion !== requiredText(report.schemaVersion, 'report schemaVersion')) {
    throw new Error('Start Here and the analysis report use different schema snapshots.')
  }
  const projectId = requiredText(guide.projectId, 'Start Here projectId')
  if (projectId !== requiredText(report.project?.id, 'report project.id')) {
    throw new Error('Start Here and the analysis report belong to different projects.')
  }
  const analysisStatus = requiredText(guide.analysisStatus, 'Start Here analysisStatus')
  if (analysisStatus !== requiredText(report.status, 'report status')) {
    throw new Error('Start Here and the analysis report use different analysis states.')
  }

  const rawItems = requiredList(guide.items, 'Start Here items')
  const defaultLimit = positiveInteger(guide.defaultLimit, 'Start Here defaultLimit')
  const totalItemCount = nonNegativeInteger(guide.totalItemCount, 'Start Here totalItemCount')
  const hiddenItemCount = nonNegativeInteger(guide.hiddenItemCount, 'Start Here hiddenItemCount')
  if (rawItems.length !== totalItemCount) {
    throw new Error('Start Here totalItemCount does not match items.')
  }
  if (hiddenItemCount !== Math.max(0, totalItemCount - defaultLimit)
      || Boolean(guide.expandable) !== (hiddenItemCount > 0)) {
    throw new Error('Start Here expansion metadata is inconsistent.')
  }

  const moduleById = new Map(collection(report.modules).map(moduleView).map((item) => [item.id, item]))
  const componentById = new Map(collection(report.components).map(componentView).map((item) => [item.id, item]))
  const endpointById = new Map(collection(report.endpoints).map(endpointView).map((item) => [item.id, item]))
  const sourceFileByIdentity = new Map()
  for (const sourceFile of collection(report.sourceFiles).map(sourceFileView)) {
    sourceFileByIdentity.set(sourceFile.id, sourceFile)
    sourceFileByIdentity.set(sourceFile.path, sourceFile)
  }

  const seenFiles = new Set()
  const items = rawItems.map((item, index) => {
    const sourceFileId = requiredText(item?.sourceFileId, `Start Here item ${index + 1} sourceFileId`)
    if (seenFiles.has(sourceFileId)) {
      throw new Error(`Start Here repeats source file ${sourceFileId}.`)
    }
    seenFiles.add(sourceFileId)
    const moduleId = text(item?.moduleId)
    const reasons = requiredList(item?.reasons, `Start Here item ${index + 1} reasons`)
      .map((reason, reasonIndex) => reasonView(reason, index, reasonIndex))
    if (!reasons.length) {
      throw new Error(`Start Here item ${index + 1} has no reasons.`)
    }
    const supportingIds = [...new Set(reasons.flatMap((reason) => reason.supportingEntityIds))]
    const relatedComponents = supportingIds.map((id) => componentById.get(id)).filter(Boolean)
      .sort((left, right) => left.qualifiedName.localeCompare(right.qualifiedName)
        || left.id.localeCompare(right.id))
    const relatedEndpoints = supportingIds.map((id) => endpointById.get(id)).filter(Boolean)
      .sort(endpointOrder)
    return {
      index: index + 1,
      sourceFileId,
      fileName: sourceFileId.split('/').at(-1) || sourceFileId,
      moduleId,
      module: moduleId ? moduleById.get(moduleId) ?? null : null,
      moduleLabel: moduleId ? moduleById.get(moduleId)?.label ?? 'Module details unavailable' : 'Module unavailable',
      sourceFile: sourceFileByIdentity.get(sourceFileId) ?? null,
      reasons,
      primaryReason: reasons[0],
      additionalReasonCount: Math.max(0, reasons.length - 1),
      relatedComponents,
      relatedEndpoints
    }
  })

  const coverageLimitationCodes = requiredList(
    guide.coverageLimitationCodes, 'Start Here coverageLimitationCodes')
    .map((value, index) => requiredText(value, `coverage limitation ${index + 1}`))
  const coverageLimited = guide.coverageLimited === true
  const coverageNotice = text(guide.coverageNotice)
  const expectedCoverageLimited = analysisStatus !== 'SUCCESS' || coverageLimitationCodes.length > 0
  if (coverageLimited !== expectedCoverageLimited) {
    throw new Error('Start Here coverageLimited does not match analysis status and limitations.')
  }
  if (coverageLimited && !coverageNotice) {
    throw new Error('Incomplete Start Here analysis requires a coverage notice.')
  }

  return {
    schemaVersion,
    reportSchemaVersion,
    projectId,
    analysisStatus,
    coverageLimited,
    coverageLimitationCodes,
    coverageNotice,
    defaultLimit,
    totalItemCount,
    expandable: guide.expandable === true,
    hiddenItemCount,
    items
  }
}

export function visibleStartHereItems(model, expanded) {
  return expanded ? model.items : model.items.slice(0, model.defaultLimit)
}

function reasonView(value, itemIndex, reasonIndex) {
  const prefix = `Start Here item ${itemIndex + 1} reason ${reasonIndex + 1}`
  const kind = requiredText(value?.kind, `${prefix} kind`)
  const factCount = positiveInteger(value?.factCount, `${prefix} factCount`)
  const supportingEntityIds = requiredList(
    value?.supportingEntityIds, `${prefix} supportingEntityIds`)
    .map((id, index) => requiredText(id, `${prefix} supportingEntityIds[${index}]`))
  const evidence = requiredList(value?.evidence, `${prefix} evidence`)
    .map((item, index) => evidenceView(item, `${prefix} evidence ${index + 1}`))
  if (!supportingEntityIds.length || !evidence.length) {
    throw new Error(`${prefix} must retain supporting entities and evidence.`)
  }
  const dependencyDistance = value?.dependencyDistance === null
    || value?.dependencyDistance === undefined
    ? null
    : positiveInteger(value.dependencyDistance, `${prefix} dependencyDistance`)
  return {
    kind,
    kindLabel: REASON_LABELS[kind] ?? displayName(kind) ?? kind,
    message: requiredText(value?.message, `${prefix} message`),
    factCount,
    dependencyDistance,
    supportingEntityIds: [...new Set(supportingEntityIds)],
    evidence
  }
}

function moduleView(value) {
  const id = requiredText(value?.id, 'module id')
  return {
    id,
    label: text(value?.artifactId) ?? text(value?.pomFileId) ?? id,
    pomFileId: text(value?.pomFileId),
    baseDirectory: text(value?.baseDirectory)
  }
}

function componentView(value) {
  const id = requiredText(value?.id, 'component id')
  const qualifiedName = text(value?.qualifiedName) ?? id
  return {
    id,
    moduleId: text(value?.moduleId),
    qualifiedName,
    label: qualifiedName.split('.').at(-1) || qualifiedName,
    kind: text(value?.kind),
    kindLabel: displayName(value?.kind) ?? 'Component role unavailable'
  }
}

function endpointView(value) {
  const id = requiredText(value?.id, 'endpoint id')
  const method = text(value?.httpMethod) ?? 'UNAVAILABLE'
  const path = text(value?.path)
  return {
    id,
    moduleId: text(value?.moduleId),
    componentId: text(value?.componentId),
    method,
    methodLabel: method,
    path,
    pathLabel: path ?? (value?.unresolvedPath === true ? 'Unresolved path' : 'Path unavailable'),
    handlerMethod: text(value?.handlerMethod) ?? 'Handler unavailable'
  }
}

function sourceFileView(value) {
  const id = requiredText(value?.id, 'source file id')
  return {
    id,
    path: requiredText(value?.path, 'source file path'),
    language: displayName(value?.language)
  }
}

function evidenceView(value, name) {
  return {
    type: requiredText(value?.type, `${name} type`),
    ruleId: requiredText(value?.ruleId, `${name} ruleId`),
    location: locationView(value?.location, `${name} location`),
    relatedLocations: requiredList(value?.relatedLocations, `${name} relatedLocations`)
      .map((location, index) => locationView(location, `${name} related location ${index + 1}`))
  }
}

function locationView(value, name) {
  const sourceFileId = requiredText(value?.sourceFileId, `${name} sourceFileId`)
  const startLine = optionalPositiveInteger(value?.startLine, `${name} startLine`)
  const startColumn = optionalPositiveInteger(value?.startColumn, `${name} startColumn`)
  const endLine = optionalPositiveInteger(value?.endLine, `${name} endLine`)
  const endColumn = optionalPositiveInteger(value?.endColumn, `${name} endColumn`)
  const symbol = text(value?.symbol)
  const start = startLine ? `:${startLine}${startColumn ? `:${startColumn}` : ''}` : ''
  return {
    sourceFileId,
    startLine,
    startColumn,
    endLine,
    endColumn,
    symbol,
    display: `${sourceFileId}${start}`
  }
}

function endpointOrder(left, right) {
  return left.method.localeCompare(right.method)
    || left.pathLabel.localeCompare(right.pathLabel)
    || left.handlerMethod.localeCompare(right.handlerMethod)
    || left.id.localeCompare(right.id)
}

function collection(value) {
  return Array.isArray(value) ? value : []
}

function requiredList(value, name) {
  if (!Array.isArray(value)) throw new Error(`${name} must be an array.`)
  return value
}

function requiredText(value, name) {
  const result = text(value)
  if (!result) throw new Error(`${name} must be present.`)
  return result
}

function text(value) {
  return typeof value === 'string' && value.trim() ? value : null
}

function positiveInteger(value, name) {
  if (!Number.isInteger(value) || value < 1) throw new Error(`${name} must be a positive integer.`)
  return value
}

function optionalPositiveInteger(value, name) {
  if (value === null || value === undefined) return null
  return positiveInteger(value, name)
}

function nonNegativeInteger(value, name) {
  if (!Number.isInteger(value) || value < 0) throw new Error(`${name} must not be negative.`)
  return value
}

function major(value) {
  return /^\d+\.\d+$/.test(value) ? Number(value.split('.')[0]) : null
}
