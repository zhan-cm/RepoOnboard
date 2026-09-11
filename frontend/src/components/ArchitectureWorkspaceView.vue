<script setup>
import { computed, ref, watch } from 'vue'
import ArchitectureGraph from './ArchitectureGraph.vue'
import StatePanel from './StatePanel.vue'
import { defaultArchitectureExploration } from '../lib/reportArchitecture.js'

const props = defineProps({
  model: { type: Object, required: true },
  moduleId: { type: String, default: '' },
  selection: { type: Object, default: null },
  scope: { type: Object, required: true },
  exploration: { type: Object, required: true }
})
const emit = defineEmits(['select-module', 'select', 'update-exploration'])
const graphView = ref(null)
const graphError = ref('')
const listCollapsed = ref(compactLandscape())
const coverageLimited = computed(() => props.model.status === 'PARTIAL' || props.model.status === 'FAILED')
const selectedComponentVisible = computed(() => props.selection?.type === 'component'
  && props.scope.kindFilteredNodes.some((item) => item.id === props.selection.id))
const selectedRelationshipEnabled = computed(() => props.scope.exploration.relationshipKinds
  .includes('COMPONENT_INJECTION'))
const canExploreNeighborhood = computed(() => selectedComponentVisible.value
  && selectedRelationshipEnabled.value
  && props.model.relationshipsAvailable)

function compactLandscape() {
  return typeof window !== 'undefined'
    && typeof window.matchMedia === 'function'
    && window.matchMedia('(min-width: 57.01rem) and (max-height: 50rem)').matches
}

watch(() => props.moduleId, () => {
  graphError.value = ''
  listCollapsed.value = compactLandscape()
})

function updateExploration(change) {
  emit('update-exploration', { ...props.exploration, ...change })
}

function toggleKind(kind, checked) {
  const selected = new Set(props.scope.exploration.componentKinds)
  if (checked) selected.add(kind)
  else selected.delete(kind)
  updateExploration({ componentKinds: [...selected], neighborhood: false })
}

function toggleRelationship(checked) {
  updateExploration({
    relationshipKinds: checked ? ['COMPONENT_INJECTION'] : [],
    neighborhood: false
  })
}

function toggleNeighborhood() {
  if (!canExploreNeighborhood.value) return
  updateExploration({ neighborhood: !props.scope.exploration.neighborhood })
}

function resetExploration() {
  emit('select', null)
  emit('update-exploration', defaultArchitectureExploration())
}

function selectGraphItem(reference) {
  if (!reference) {
    emit('select', null)
    return
  }
  const values = reference.type === 'component' ? props.scope.nodes : props.scope.edges
  const value = values.find((item) => item.id === reference.id)
  emit('select', value ? { type: reference.type, id: reference.id } : null)
}

function relationCount(node, direction) {
  return props.scope.edges.filter((edge) => direction === 'incoming'
    ? edge.targetId === node.id
    : edge.sourceId === node.id).length
}
</script>

<template>
  <section class="architecture-workspace" aria-labelledby="architecture-title">
    <header class="architecture-header">
      <div class="architecture-header__copy">
        <p class="workbench-kicker">Architecture workspace</p>
        <h1 id="architecture-title">Component relationships</h1>
        <p>Explore confirmed Spring component injection relationships without turning unresolved or inferred targets into facts.</p>
      </div>
      <div class="architecture-legend" aria-label="Component kind legend">
        <span><i class="architecture-kind architecture-kind--controller">C</i>Controller</span>
        <span><i class="architecture-kind architecture-kind--service">S</i>Service</span>
        <span><i class="architecture-kind architecture-kind--repository">R</i>Repository</span>
        <span><i class="architecture-kind architecture-kind--configuration">⚙</i>Config</span>
        <span><i class="architecture-kind architecture-kind--component">●</i>Component</span>
      </div>
    </header>

    <section class="architecture-exploration" aria-label="Architecture exploration controls">
      <div class="architecture-exploration__filters">
        <label class="architecture-scope">
          <span>Module</span>
          <select :value="moduleId" @change="$emit('select-module', $event.target.value)">
            <option v-for="module in model.modules" :key="module.id" :value="module.id">
              {{ module.label }} ({{ module.componentCount }} components)
            </option>
          </select>
        </label>

        <details class="architecture-filter-menu">
          <summary>
            Component kinds
            <strong>{{ scope.exploration.componentKinds.length }} / {{ scope.availableKinds.length }}</strong>
          </summary>
          <fieldset>
            <legend>Component kinds in this module</legend>
            <label v-for="option in scope.availableKinds" :key="option.kind">
              <input
                type="checkbox"
                :aria-label="`${option.label} (${option.count})`"
                :checked="scope.exploration.componentKinds.includes(option.kind)"
                @change="toggleKind(option.kind, $event.target.checked)"
              />
              <span>{{ option.label }}</span><strong>{{ option.count }}</strong>
            </label>
          </fieldset>
        </details>

        <label class="architecture-relationship-filter">
          <input
            type="checkbox"
            :checked="selectedRelationshipEnabled"
            :disabled="!model.relationshipsAvailable"
            @change="toggleRelationship($event.target.checked)"
          />
          <span>Component injection</span>
          <strong>{{ scope.relationshipOptions[0]?.count ?? '—' }} confirmed</strong>
        </label>

        <label class="architecture-search">
          <span class="sr-only">Search components</span>
          <input
            type="search"
            :value="exploration.query"
            placeholder="Search name, path, or symbol"
            @input="updateExploration({ query: $event.target.value })"
          />
        </label>
      </div>

      <div class="architecture-exploration__actions">
        <div class="architecture-active-filters">
          <span>Active scope</span>
          <strong>{{ scope.exploration.componentKinds.length }} kinds</strong>
          <strong>{{ selectedRelationshipEnabled ? 'Confirmed injection' : 'No relationships' }}</strong>
          <strong v-if="scope.exploration.neighborhood">1-hop neighborhood</strong>
        </div>
        <button class="architecture-deselect" type="button" :disabled="!scope.filtersActive" @click="resetExploration">
          Reset filters
        </button>
        <button
          class="architecture-tool-button"
          :class="{ active: scope.exploration.neighborhood }"
          type="button"
          :disabled="!canExploreNeighborhood"
          @click="toggleNeighborhood"
        >1-hop {{ scope.exploration.neighborhood ? 'on' : 'off' }}</button>
        <button
          class="architecture-tool-button"
          type="button"
          :disabled="scope.overBudget || Boolean(graphError) || !selectedComponentVisible"
          @click="graphView?.focus(selection.id)"
        >Focus selection</button>
        <button
          class="architecture-tool-button"
          type="button"
          :disabled="scope.overBudget || !scope.nodes.length || Boolean(graphError)"
          @click="graphView?.fit()"
        >Fit view</button>
      </div>
    </section>

    <div v-if="!model.relationshipsAvailable || coverageLimited || scope.counts.ambiguousRelations || scope.counts.unresolvedRelations" class="architecture-coverage" role="status">
      <strong>{{ !model.relationshipsAvailable ? 'Relationship data unavailable' : model.status === 'FAILED' ? 'Analysis failed' : model.status === 'PARTIAL' ? 'Coverage is limited' : 'Some relationships are unresolved' }}</strong>
      <span>
        <template v-if="!model.relationshipsAvailable">This report does not provide component dependency facts.</template>
        <template v-else>
          Only confirmed component relationships are drawn.
          <template v-if="scope.counts.ambiguousRelations"> {{ scope.counts.ambiguousRelations }} ambiguous.</template>
          <template v-if="scope.counts.unresolvedRelations"> {{ scope.counts.unresolvedRelations }} unresolved.</template>
        </template>
      </span>
    </div>

    <div class="architecture-scope-notice" aria-live="polite">
      <strong>{{ scope.counts.components }} of {{ scope.counts.reportedComponents }} reported components shown</strong>
      <span>{{ scope.counts.filteredComponents }} filtered</span>
      <span>{{ scope.counts.confirmedRelations ?? '—' }} confirmed relations</span>
      <span v-if="scope.exploration.neighborhood">1-hop neighborhood active</span>
      <span v-if="scope.overBudget">{{ scope.withheldComponents }} withheld from graph</span>
    </div>

    <div class="architecture-canvas">
      <StatePanel
        v-if="!model.collectionAvailable"
        variant="empty"
        heading="Component data unavailable"
        message="This report does not provide a component collection for the Architecture workspace."
      />
      <StatePanel
        v-else-if="!model.modules.length"
        variant="empty"
        heading="Module scope unavailable"
        message="No module scope is available for the reported architecture facts."
      />
      <StatePanel
        v-else-if="!scope.reportedNodes.length"
        variant="empty"
        heading="No components reported"
        message="No framework components were reported for this module scope."
      />
      <StatePanel
        v-else-if="!scope.nodes.length"
        variant="empty"
        heading="No components match the current filters"
        message="Change the component kinds or reset the Architecture filters."
      />
      <StatePanel
        v-else-if="scope.overBudget"
        variant="warning"
        heading="Graph paused for readability"
        :message="`${scope.nodes.length} components and ${scope.edges.length} confirmed relations match the current filters. ${scope.withheldComponents} components are withheld from the graph. Narrow the component kinds, or select a component from the list and show its 1-hop neighborhood.`"
      />
      <template v-else-if="graphError">
        <StatePanel
          variant="warning"
          heading="Graph rendering unavailable"
          :message="`${graphError} Use the searchable component list to inspect the same reported facts.`"
        />
      </template>
      <template v-else>
        <div v-if="!model.relationshipsAvailable" class="architecture-no-edges" role="status">
          Component relationship data is unavailable in this report. Reported components remain available for inspection.
        </div>
        <div v-else-if="!selectedRelationshipEnabled" class="architecture-no-edges" role="status">
          No relationship types selected. Reported components remain available for inspection.
        </div>
        <div v-else-if="!scope.edges.length" class="architecture-no-edges" role="status">
          No confirmed component relationships in this scope. Reported components remain available for inspection.
        </div>
        <ArchitectureGraph
          ref="graphView"
          :nodes="scope.nodes"
          :edges="scope.edges"
          :selection="selection"
          @select="selectGraphItem"
          @error="graphError = $event"
        />
      </template>
    </div>

    <section
      v-if="model.collectionAvailable && scope.module && scope.reportedNodes.length"
      class="architecture-component-tray"
      aria-labelledby="architecture-component-list-title"
    >
      <button class="architecture-component-tray__header" type="button" :aria-expanded="!listCollapsed" @click="listCollapsed = !listCollapsed">
        <span aria-hidden="true">{{ listCollapsed ? '▸' : '▾' }}</span>
        <strong id="architecture-component-list-title">Matching components</strong>
        <span>{{ scope.counts.listResults }} of {{ scope.counts.components }}</span>
        <span>{{ listCollapsed ? 'Expand list' : 'Collapse list' }}</span>
      </button>
      <div v-if="!listCollapsed" class="architecture-component-tray__body">
        <p v-if="!scope.listNodes.length" class="architecture-list-empty" role="status">
          {{ scope.searchMatchExcludedByKind
            ? 'Matching reported components are excluded by the current component-kind filter.'
            : `No reported components match “${scope.exploration.query}” in this scope.` }}
        </p>
        <ul v-else class="architecture-component-list" aria-label="Searchable architecture components">
          <li v-for="node in scope.listNodes" :key="node.id">
            <button
              type="button"
              :class="{ active: selection?.type === 'component' && selection.id === node.id }"
              :aria-pressed="selection?.type === 'component' && selection.id === node.id"
              @click="selectGraphItem({ type: 'component', id: node.id })"
            >
              <span class="architecture-component-list__identity">
                <i :class="`architecture-list-dot architecture-list-dot--${node.kindFamily}`"></i>
                <strong>{{ node.label }}</strong>
                <small>{{ node.kindLabel }}</small>
              </span>
              <code>{{ node.qualifiedName }}</code>
              <span class="architecture-component-list__relations">
                {{ relationCount(node, 'incoming') }} in / {{ relationCount(node, 'outgoing') }} out
              </span>
            </button>
          </li>
        </ul>
      </div>
    </section>

    <footer class="architecture-statusbar">
      <span><i></i> AnalysisReport schema {{ model.schemaVersion ?? 'unavailable' }}</span>
      <span>{{ scope.module ? `Scope: ${scope.module.label}` : 'Scope unavailable' }}</span>
      <span>{{ scope.counts.components }} shown / {{ scope.counts.filteredComponents }} filtered</span>
    </footer>
  </section>
</template>
