<script setup>
import { computed, ref, watch } from 'vue'
import ArchitectureGraph from './ArchitectureGraph.vue'
import StatePanel from './StatePanel.vue'
import { createArchitectureScope } from '../lib/reportArchitecture.js'

const props = defineProps({
  model: { type: Object, required: true },
  moduleId: { type: String, default: '' },
  selection: { type: Object, default: null }
})
const emit = defineEmits(['select-module', 'select'])
const graphView = ref(null)
const graphError = ref('')
const scope = computed(() => createArchitectureScope(props.model, props.moduleId))
const coverageLimited = computed(() => props.model.status === 'PARTIAL' || props.model.status === 'FAILED')

watch(() => props.moduleId, () => {
  graphError.value = ''
})

function selectGraphItem(reference) {
  if (!reference) {
    emit('select', null)
    return
  }
  const values = reference.type === 'component' ? scope.value.nodes : scope.value.edges
  const value = values.find((item) => item.id === reference.id)
  emit('select', value ? { type: reference.type, id: reference.id } : null)
}
</script>

<template>
  <section class="architecture-workspace" aria-labelledby="architecture-title">
    <header class="architecture-header">
      <div class="architecture-header__copy">
        <p class="workbench-kicker">Architecture workspace</p>
        <h1 id="architecture-title">Component relationships</h1>
        <p>Confirmed Spring component dependencies from the current analysis report. Directed edges show injection target (source → target).</p>
      </div>
      <div class="architecture-legend" aria-label="Component kind legend">
        <span><i class="architecture-kind architecture-kind--controller">C</i>Controller</span>
        <span><i class="architecture-kind architecture-kind--service">S</i>Service</span>
        <span><i class="architecture-kind architecture-kind--repository">R</i>Repository</span>
        <span><i class="architecture-kind architecture-kind--configuration">⚙</i>Config</span>
        <span><i class="architecture-kind architecture-kind--component">●</i>Component</span>
      </div>
    </header>

    <div class="architecture-toolbar">
      <label class="architecture-scope">
        <span>Module scope</span>
        <select :value="moduleId" @change="$emit('select-module', $event.target.value)">
          <option v-for="module in model.modules" :key="module.id" :value="module.id">
            {{ module.label }} ({{ module.componentCount }} components)
          </option>
        </select>
      </label>
      <button
        class="architecture-tool-button"
        type="button"
        :disabled="scope.overBudget || !scope.nodes.length || Boolean(graphError)"
        @click="graphView?.fit()"
      >
        <span aria-hidden="true">⌗</span> Fit view
      </button>
      <button
        v-if="selection"
        class="architecture-deselect"
        type="button"
        @click="$emit('select', null)"
      >Deselect</button>
      <div class="architecture-scope-summary" aria-live="polite">
        <strong>{{ scope.counts.components }}</strong> reported components
        <span aria-hidden="true">·</span>
        <strong>{{ scope.counts.confirmedRelations ?? '—' }}</strong> confirmed relations
      </div>
    </div>

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
        v-else-if="!scope.nodes.length"
        variant="empty"
        heading="No components reported"
        message="No framework components were reported for this module scope."
      />
      <StatePanel
        v-else-if="scope.overBudget"
        variant="warning"
        heading="Graph scope exceeds the readability budget"
        :message="`This module reports ${scope.nodes.length} components and ${scope.edges.length} confirmed relations. T-0705 renders at most ${scope.nodeBudget} nodes and ${scope.edgeBudget} edges; choose another module rather than viewing a misleading partial graph.`"
      />
      <template v-else-if="graphError">
        <StatePanel
          variant="warning"
          heading="Graph rendering unavailable"
          :message="`${graphError} Use the component list below to inspect the same confirmed facts.`"
        />
        <ul class="architecture-fallback-list" aria-label="Architecture component fallback list">
          <li v-for="node in scope.nodes" :key="node.id">
            <button type="button" @click="selectGraphItem({ type: 'component', id: node.id })">
              <strong>{{ node.label }}</strong><span>{{ node.kindLabel }} · {{ node.moduleLabel }}</span>
            </button>
          </li>
        </ul>
      </template>
      <template v-else>
        <div v-if="!scope.edges.length" class="architecture-no-edges" role="status">
          {{ model.relationshipsAvailable
            ? 'No confirmed component relationships in this scope. Reported components remain available for inspection.'
            : 'Component relationship data is unavailable in this report. Reported components remain available for inspection.' }}
        </div>
        <ArchitectureGraph
          ref="graphView"
          :nodes="scope.nodes"
          :edges="scope.edges"
          :selection="selection"
          @select="selectGraphItem"
          @error="graphError = $event"
        />
        <ul class="architecture-mobile-list" aria-label="Components in current module">
          <li v-for="node in scope.nodes" :key="node.id">
            <button
              type="button"
              :class="{ active: selection?.type === 'component' && selection.id === node.id }"
              @click="selectGraphItem({ type: 'component', id: node.id })"
            >
              <strong>{{ node.label }}</strong>
              <span>{{ node.kindLabel }} · {{ node.moduleLabel }}</span>
            </button>
          </li>
        </ul>
      </template>
    </div>

    <footer class="architecture-statusbar">
      <span><i></i> AnalysisReport schema {{ model.schemaVersion ?? 'unavailable' }}</span>
      <span>{{ scope.module ? `Scope: ${scope.module.label}` : 'Scope unavailable' }}</span>
      <span>Confirmed edges: {{ scope.counts.confirmedRelations ?? 'unavailable' }}</span>
    </footer>
  </section>
</template>
