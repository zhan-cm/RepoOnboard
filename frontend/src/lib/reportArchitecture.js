import { displayName } from './reportOverview.js'

export const ARCHITECTURE_NODE_BUDGET = 60
export const ARCHITECTURE_EDGE_BUDGET = 120
export const ARCHITECTURE_RELATIONSHIP_KIND = 'COMPONENT_INJECTION'

const KIND_ORDER = [
  'REST_CONTROLLER',
  'CONTROLLER',
  'SERVICE',
  'REPOSITORY',
  'COMPONENT',
  'CONFIGURATION'
]

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

export function createArchitectureScope(model, moduleId, exploration = {}) {
  const module = model.modules.find((item) => item.id === moduleId) ?? null
  const reportedNodes = module
    ? model.components.filter((component) => component.moduleId === module.id)
    : []
  const reportedNodeIds = new Set(reportedNodes.map((node) => node.id))
  const reportedEdges = model.confirmedRelations.filter(
    (edge) => reportedNodeIds.has(edge.sourceId) && reportedNodeIds.has(edge.targetId))

  const availableKinds = kindOptions(reportedNodes)
  const selectedKinds = selectedValues(exploration.componentKinds, availableKinds.map((item) => item.kind))
  const selectedKindSet = new Set(selectedKinds)
  const kindFilteredNodes = reportedNodes.filter((node) => selectedKindSet.has(node.kind))
  const kindFilteredNodeIds = new Set(kindFilteredNodes.map((node) => node.id))

  const selectedRelationshipKinds = selectedValues(
    exploration.relationshipKinds,
    model.relationshipsAvailable ? [ARCHITECTURE_RELATIONSHIP_KIND] : [])
  const selectedRelationshipSet = new Set(selectedRelationshipKinds)
  const kindFilteredEdges = reportedEdges.filter((edge) => selectedRelationshipSet.has(edge.kind)
    && kindFilteredNodeIds.has(edge.sourceId)
    && kindFilteredNodeIds.has(edge.targetId))

  const selectedComponentId = text(exploration.selectedComponentId)
  const neighborhoodRequested = exploration.neighborhood === true
  const neighborhoodCenter = neighborhoodRequested
    ? kindFilteredNodes.find((node) => node.id === selectedComponentId) ?? null
    : null
  const neighborhoodNodeIds = neighborhoodCenter
    ? firstDegreeNodeIds(neighborhoodCenter.id, kindFilteredEdges)
    : null
  const nodes = neighborhoodNodeIds
    ? kindFilteredNodes.filter((node) => neighborhoodNodeIds.has(node.id))
    : kindFilteredNodes
  const nodeIds = new Set(nodes.map((node) => node.id))
  const edges = kindFilteredEdges.filter(
    (edge) => nodeIds.has(edge.sourceId) && nodeIds.has(edge.targetId))
  const limitedRelations = model.limitedRelations.filter((edge) => nodeIds.has(edge.sourceId))

  const query = typeof exploration.query === 'string' ? exploration.query.trim() : ''
  const listNodes = query
    ? nodes.filter((node) => matchesNode(node, query))
    : nodes
  const moduleSearchMatches = query
    ? reportedNodes.filter((node) => matchesNode(node, query))
    : []
  const overBudget = nodes.length > ARCHITECTURE_NODE_BUDGET
    || edges.length > ARCHITECTURE_EDGE_BUDGET

  return {
    module,
    reportedNodes,
    reportedEdges,
    kindFilteredNodes,
    nodes,
    edges,
    listNodes,
    limitedRelations,
    overBudget,
    withheldComponents: overBudget ? nodes.length : 0,
    nodeBudget: ARCHITECTURE_NODE_BUDGET,
    edgeBudget: ARCHITECTURE_EDGE_BUDGET,
    availableKinds,
    relationshipOptions: model.relationshipsAvailable
      ? [{
          kind: ARCHITECTURE_RELATIONSHIP_KIND,
          label: 'Component injection',
          count: reportedEdges.length
        }]
      : [],
    exploration: {
      componentKinds: selectedKinds,
      relationshipKinds: selectedRelationshipKinds,
      query,
      neighborhood: Boolean(neighborhoodCenter),
      neighborhoodRequested,
      selectedComponentId
    },
    filtersActive: selectedKinds.length !== availableKinds.length
      || selectedRelationshipKinds.length !== (model.relationshipsAvailable ? 1 : 0)
      || Boolean(query)
      || neighborhoodRequested,
    searchMatchExcludedByKind: Boolean(query)
      && listNodes.length === 0
      && moduleSearchMatches.some((node) => !selectedKindSet.has(node.kind)),
    counts: {
      reportedComponents: reportedNodes.length,
      components: nodes.length,
      listResults: listNodes.length,
      filteredComponents: reportedNodes.length - nodes.length,
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

export function defaultArchitectureExploration() {
  return {
    componentKinds: null,
    relationshipKinds: [ARCHITECTURE_RELATIONSHIP_KIND],
    query: '',
    neighborhood: false
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

function kindOptions(nodes) {
  const counts = new Map()
  for (const node of nodes) counts.set(node.kind, (counts.get(node.kind) ?? 0) + 1)
  return [...counts.entries()]
    .map(([kind, count]) => ({
      kind,
      label: displayName(kind) ?? 'Unknown',
      count,
      family: kindFamily(kind)
    }))
    .sort((left, right) => {
      const leftOrder = KIND_ORDER.indexOf(left.kind)
      const rightOrder = KIND_ORDER.indexOf(right.kind)
      const normalizedLeft = leftOrder === -1 ? KIND_ORDER.length : leftOrder
      const normalizedRight = rightOrder === -1 ? KIND_ORDER.length : rightOrder
      return normalizedLeft - normalizedRight || left.label.localeCompare(right.label)
    })
}

function selectedValues(supplied, available) {
  if (!Array.isArray(supplied)) return [...available]
  const availableSet = new Set(available)
  return supplied.filter((value, index) => availableSet.has(value) && supplied.indexOf(value) === index)
}

function firstDegreeNodeIds(centerId, edges) {
  const result = new Set([centerId])
  for (const edge of edges) {
    if (edge.sourceId === centerId) result.add(edge.targetId)
    if (edge.targetId === centerId) result.add(edge.sourceId)
  }
  return result
}

function matchesNode(node, query) {
  const normalized = query.toLocaleLowerCase()
  return [
    node.label,
    node.qualifiedName,
    node.name,
    node.location?.sourceFileId,
    node.location?.symbol
  ].some((value) => typeof value === 'string' && value.toLocaleLowerCase().includes(normalized))
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
