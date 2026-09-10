import { displayName } from './reportOverview.js'

export function createModuleExplorerModel(report) {
  const rawModules = list(report?.modules)
  const sourceFiles = list(report?.sourceFiles)
  const components = list(report?.components)
  const endpoints = list(report?.endpoints)
  const entryPoints = list(report?.entryPoints)
  const dependencies = list(report?.dependencies)
  const diagnostics = list(report?.diagnostics)
  const hierarchySupported = rawModules !== null && (
    rawModules.length <= 1
      || rawModules.every((module) => Object.hasOwn(module ?? {}, 'aggregationParentModuleId'))
  )
  const relationshipsSupported = schemaMinor(report?.schemaVersion) >= 2 && dependencies !== null
  const moduleIds = new Set(rawModules?.map((module) => text(module?.id)).filter(Boolean) ?? [])
  const moduleDependencies = relationshipsSupported
    ? dependencies.filter((dependency) =>
      dependency?.kind === 'MAVEN_DECLARATION'
        && dependency?.status === 'CONFIRMED'
        && moduleIds.has(text(dependency?.sourceId))
        && moduleIds.has(text(dependency?.targetId)))
    : null

  const modules = (rawModules ?? []).map((module) => {
    const id = text(module?.id)
    const moduleComponents = byModule(components, id)
    const moduleEndpoints = byModule(endpoints, id)
    const moduleEntryPoints = byModule(entryPoints, id)
    const moduleSourceFiles = byModule(sourceFiles, id)
    const outgoing = moduleDependencies?.filter((dependency) => dependency.sourceId === id) ?? null
    const incoming = moduleDependencies?.filter((dependency) => dependency.targetId === id) ?? null
    const frameworkVersions = normalizeVersions(module?.frameworkVersions, 'framework')
    const languageVersions = normalizeVersions(module?.languageVersions, 'language')
    const frameworks = unique([
      ...(list(module?.frameworks) ?? []),
      ...frameworkVersions.map((item) => item.kind)
    ])

    return {
      id,
      parentId: text(module?.aggregationParentModuleId),
      label: text(module?.artifactId) ?? text(module?.pomFileId) ?? 'Unnamed module',
      groupId: text(module?.groupId),
      artifactId: text(module?.artifactId),
      version: text(module?.version),
      packaging: text(module?.packaging),
      gav: gav(module),
      pomFileId: text(module?.pomFileId),
      baseDirectory: text(module?.baseDirectory),
      sourceRoots: list(module?.sourceRoots),
      frameworks: frameworks.map(displayName).filter(Boolean),
      frameworkVersions,
      languageVersions,
      evidence: normalizeEvidence([
        ...(list(module?.evidence) ?? []),
        ...(list(module?.languageVersions)?.flatMap((item) => list(item?.evidence) ?? []) ?? []),
        ...(list(module?.frameworkVersions)?.flatMap((item) => list(item?.evidence) ?? []) ?? [])
      ]),
      diagnostics: diagnostics?.filter((item) => text(item?.moduleId) === id) ?? null,
      counts: {
        sourceFiles: moduleSourceFiles?.length ?? null,
        components: moduleComponents?.length ?? null,
        endpoints: moduleEndpoints?.length ?? null,
        entryPoints: moduleEntryPoints?.length ?? null
      },
      components: moduleComponents?.map(normalizeComponent) ?? null,
      entryPoints: moduleEntryPoints?.map(normalizeEntryPoint) ?? null,
      outgoing: outgoing?.map((dependency) => ({
        id: text(dependency?.id),
        targetId: text(dependency?.targetId),
        declaredTarget: text(dependency?.declaredTarget),
        location: formatLocation(dependency?.location)
      })) ?? null,
      incoming: incoming?.map((dependency) => ({
        id: text(dependency?.id),
        sourceId: text(dependency?.sourceId),
        declaredTarget: text(dependency?.declaredTarget),
        location: formatLocation(dependency?.location)
      })) ?? null
    }
  })

  const byId = new Map(modules.map((module) => [module.id, module]))
  for (const module of modules) {
    module.parentLabel = byId.get(module.parentId)?.label ?? module.parentId
    module.outgoing = module.outgoing?.map((dependency) => ({
      ...dependency,
      targetLabel: byId.get(dependency.targetId)?.label ?? dependency.targetId
    })) ?? null
    module.incoming = module.incoming?.map((dependency) => ({
      ...dependency,
      sourceLabel: byId.get(dependency.sourceId)?.label ?? dependency.sourceId
    })) ?? null
  }

  return {
    repositoryName: text(report?.project?.name) ?? 'Repository name unavailable',
    buildSystem: displayName(report?.project?.buildSystem) ?? 'Unavailable',
    status: text(report?.status) ?? 'UNKNOWN',
    schemaVersion: text(report?.schemaVersion),
    hierarchySupported,
    relationshipsSupported,
    modules: flattenHierarchy(modules, hierarchySupported)
  }
}

function flattenHierarchy(modules, supported) {
  if (!supported) return [...modules].sort(moduleOrder).map((module) => ({ ...module, depth: 0 }))
  const byParent = new Map()
  for (const module of modules) {
    const parent = module.parentId ?? ''
    if (!byParent.has(parent)) byParent.set(parent, [])
    byParent.get(parent).push(module)
  }
  for (const children of byParent.values()) children.sort(moduleOrder)
  const result = []
  const seen = new Set()
  function visit(module, depth) {
    if (seen.has(module.id)) return
    seen.add(module.id)
    result.push({ ...module, depth })
    for (const child of byParent.get(module.id) ?? []) visit(child, depth + 1)
  }
  for (const root of byParent.get('') ?? []) visit(root, 0)
  for (const module of [...modules].sort(moduleOrder)) visit(module, 0)
  return result
}

function normalizeVersions(values, key) {
  return (list(values) ?? []).map((item) => ({
    kind: text(item?.[key]),
    label: displayName(item?.[key]) ?? 'Unknown',
    version: text(item?.version),
    evidence: normalizeEvidence(item?.evidence)
  })).filter((item) => item.kind && item.version)
}

function normalizeComponent(component) {
  return {
    id: text(component?.id),
    title: text(component?.name) ?? simpleName(component?.qualifiedName) ?? 'Unnamed component',
    qualifiedName: text(component?.qualifiedName),
    kind: displayName(component?.kind) ?? 'Component',
    framework: displayName(component?.framework),
    location: formatLocation(component?.location),
    evidence: normalizeEvidence(component?.evidence)
  }
}

function normalizeEntryPoint(entryPoint) {
  return {
    id: text(entryPoint?.id),
    title: simpleName(entryPoint?.qualifiedName) ?? 'Unnamed entry point',
    qualifiedName: text(entryPoint?.qualifiedName),
    kind: displayName(entryPoint?.kind) ?? 'Entry point',
    framework: displayName(entryPoint?.framework),
    location: formatLocation(entryPoint?.location),
    evidence: normalizeEvidence(entryPoint?.evidence)
  }
}

function normalizeEvidence(values) {
  return (list(values) ?? []).map((item) => ({
    type: displayName(item?.type) ?? 'Evidence',
    ruleId: text(item?.ruleId),
    location: formatLocation(item?.location)
  }))
}

function byModule(values, moduleId) {
  return values?.filter((item) => text(item?.moduleId) === moduleId) ?? null
}

function gav(module) {
  const parts = [text(module?.groupId), text(module?.artifactId), text(module?.version)]
  return parts.every(Boolean) ? parts.join(':') : null
}

function formatLocation(location) {
  const file = text(location?.sourceFileId)
  if (!file) return null
  const line = Number.isInteger(location?.startLine) && location.startLine > 0
    ? location.startLine
    : null
  return line ? `${file}:${line}` : file
}

function schemaMinor(value) {
  const match = /^(\d+)\.(\d+)$/.exec(text(value) ?? '')
  return match && Number(match[1]) === 1 ? Number(match[2]) : -1
}

function unique(values) {
  return [...new Set(values.filter((value) => text(value)).map((value) => text(value)))].sort()
}

function simpleName(value) {
  const normalized = text(value)
  return normalized?.split('.').at(-1) ?? null
}

function moduleOrder(left, right) {
  return left.label.localeCompare(right.label) || (left.id ?? '').localeCompare(right.id ?? '')
}

function list(value) {
  return Array.isArray(value) ? value : null
}

function text(value) {
  return typeof value === 'string' && value.trim() !== '' ? value.trim() : null
}
