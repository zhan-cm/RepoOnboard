import { displayName } from './reportOverview.js'

export const ARCHITECTURE_NODE_BUDGET = 60
export const ARCHITECTURE_EDGE_BUDGET = 120

export function createArchitectureModel(report) {
  const rawModules = list(report?.modules)
  const rawComponents = list(report?.components)
  const rawDependencies = list(report?.dependencies)
  const rawDiagnostics = list(report?.diagnostics)

  const moduleById = new Map((rawModules ?? []).map((module) => {
    const id = text(module?.id)
    return [id, {
      id,
      label: text(module?.artifactId) ?? id,
      pomFileId: text(module?.pomFileId)
    }]
  }).filter(([id]) => id))

  const components = (rawComponents ?? []).map((component) => {
    const moduleId = text(component?.moduleId)
    const qualifiedName = text(component?.qualifiedName) ?? text(component?.id) ?? 'Unnamed component'
    return {
      id: text(component?.id) ?? qualifiedName,
      moduleId,
      moduleLabel: moduleById.get(moduleId)?.label ?? moduleId ?? 'Module unavailable',
      qualifiedName,
      label: simpleName(qualifiedName),
      name: text(component?.name),
      kind: text(component?.kind) ?? 'UNKNOWN',
      kindLabel: displayName(component?.kind) ?? 'Unknown',
      kindFamily: kindFamily(component?.kind),
      framework: displayName(component?.framework),
      location: location(component?.location),
      evidence: evidence(component?.evidence),
      incoming: [],
      outgoing: []
    }
  })
  const componentById = new Map(components.map((component) => [component.id, component]))

  const componentDependencies = (rawDependencies ?? []).filter(
    (dependency) => dependency?.kind === 'COMPONENT_INJECTION'
      && componentById.has(text(dependency?.sourceId)))
  const confirmedRelations = componentDependencies
    .filter((dependency) => dependency?.status === 'CONFIRMED'
      && componentById.has(text(dependency?.targetId)))
    .map((dependency) => relation(dependency, componentById))
  const limitedRelations = componentDependencies
    .filter((dependency) => dependency?.status !== 'CONFIRMED'
      || !componentById.has(text(dependency?.targetId)))
    .map((dependency) => ({
      id: text(dependency?.id),
      sourceId: text(dependency?.sourceId),
      status: text(dependency?.status) ?? 'UNAVAILABLE',
      declaredTarget: text(dependency?.declaredTarget),
      location: location(dependency?.location),
      evidence: evidence(dependency?.evidence)
    }))

  for (const edge of confirmedRelations) {
    edge.source.outgoing.push(edge)
    edge.target.incoming.push(edge)
  }

  for (const component of components) {
    if (component.moduleId && !moduleById.has(component.moduleId)) {
      moduleById.set(component.moduleId, {
        id: component.moduleId,
        label: component.moduleId,
        pomFileId: null
      })
    }
  }

  const modules = [...moduleById.values()]
    .map((module) => ({
      ...module,
      componentCount: components.filter((component) => component.moduleId === module.id).length
    }))
    .sort((left, right) => left.label.localeCompare(right.label) || left.id.localeCompare(right.id))

  return {
    schemaVersion: text(report?.schemaVersion),
    status: text(report?.status) ?? 'UNKNOWN',
    collectionAvailable: rawComponents !== null,
    relationshipsAvailable: rawDependencies !== null,
    modules,
    components,
    confirmedRelations,
    limitedRelations,
    diagnostics: rawDiagnostics?.map((diagnostic) => ({
      code: text(diagnostic?.code),
      severity: text(diagnostic?.severity),
      message: text(diagnostic?.message),
      moduleId: text(diagnostic?.moduleId),
      location: location(diagnostic?.location)
    })) ?? null,
    defaultModuleId: modules.find((module) => module.componentCount > 0)?.id ?? modules[0]?.id ?? '',
    counts: {
      modules: modules.length,
      components: rawComponents?.length ?? null,
      confirmedRelations: rawDependencies === null ? null : confirmedRelations.length,
      ambiguousRelations: rawDependencies === null
        ? null
        : limitedRelations.filter((edge) => edge.status === 'AMBIGUOUS').length,
      unresolvedRelations: rawDependencies === null
        ? null
        : limitedRelations.filter((edge) => edge.status === 'UNRESOLVED').length
    }
  }
}

export function createArchitectureScope(model, moduleId) {
  const module = model.modules.find((item) => item.id === moduleId) ?? null
  const nodes = module
    ? model.components.filter((component) => component.moduleId === module.id)
    : []
  const nodeIds = new Set(nodes.map((node) => node.id))
  const edges = model.confirmedRelations.filter(
    (edge) => nodeIds.has(edge.sourceId) && nodeIds.has(edge.targetId))
  const limitedRelations = model.limitedRelations.filter((edge) => nodeIds.has(edge.sourceId))
  const overBudget = nodes.length > ARCHITECTURE_NODE_BUDGET
    || edges.length > ARCHITECTURE_EDGE_BUDGET

  return {
    module,
    nodes,
    edges,
    limitedRelations,
    overBudget,
    nodeBudget: ARCHITECTURE_NODE_BUDGET,
    edgeBudget: ARCHITECTURE_EDGE_BUDGET,
    counts: {
      components: nodes.length,
      confirmedRelations: model.relationshipsAvailable ? edges.length : null,
      ambiguousRelations: model.relationshipsAvailable
        ? limitedRelations.filter((edge) => edge.status === 'AMBIGUOUS').length
        : null,
      unresolvedRelations: model.relationshipsAvailable
        ? limitedRelations.filter((edge) => edge.status === 'UNRESOLVED').length
        : null
    }
  }
}

export function formatLocation(value) {
  if (!value?.sourceFileId) return null
  const line = positiveInteger(value.startLine)
  const column = positiveInteger(value.startColumn)
  if (line === null) return value.sourceFileId
  return column === null
    ? `${value.sourceFileId}:${line}`
    : `${value.sourceFileId}:${line}:${column}`
}

function relation(dependency, componentById) {
  const sourceId = text(dependency?.sourceId)
  const targetId = text(dependency?.targetId)
  return {
    id: text(dependency?.id) ?? `${sourceId}->${targetId}`,
    sourceId,
    targetId,
    source: componentById.get(sourceId),
    target: componentById.get(targetId),
    declaredTarget: text(dependency?.declaredTarget),
    kind: 'COMPONENT_INJECTION',
    kindLabel: 'Component injection',
    status: 'CONFIRMED',
    location: location(dependency?.location),
    evidence: evidence(dependency?.evidence)
  }
}

function location(value) {
  if (!value || typeof value !== 'object') return null
  const sourceFileId = text(value.sourceFileId)
  if (!sourceFileId) return null
  return {
    sourceFileId,
    startLine: positiveInteger(value.startLine),
    startColumn: positiveInteger(value.startColumn),
    endLine: positiveInteger(value.endLine),
    endColumn: positiveInteger(value.endColumn),
    symbol: text(value.symbol),
    display: formatLocation(value)
  }
}

function evidence(values) {
  const supplied = list(values)
  if (supplied === null) return null
  return supplied.map((item) => ({
    type: text(item?.type),
    ruleId: text(item?.ruleId),
    location: location(item?.location),
    relatedLocations: (list(item?.relatedLocations) ?? []).map(location).filter(Boolean)
  }))
}

function simpleName(value) {
  const segments = value.split(/[.$]/).filter(Boolean)
  return segments.at(-1) ?? value
}

function kindFamily(value) {
  if (value === 'CONTROLLER' || value === 'REST_CONTROLLER') return 'controller'
  if (value === 'SERVICE') return 'service'
  if (value === 'REPOSITORY') return 'repository'
  if (value === 'CONFIGURATION') return 'configuration'
  return 'component'
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
