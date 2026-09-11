<script setup>
import StatePanel from './StatePanel.vue'

const props = defineProps({
  model: { type: Object, required: true },
  scope: { type: Object, required: true },
  exploration: { type: Object, required: true },
  selection: { type: Object, default: null }
})
const emit = defineEmits(['select', 'update-exploration'])

function updateExploration(change) {
  emit('update-exploration', { ...props.exploration, ...change })
}

function reset() {
  emit('select', null)
  emit('update-exploration', { moduleId: 'all', method: 'all', query: '' })
}

function select(endpoint) {
  emit('select', { type: 'endpoint', id: endpoint.id })
}

function source(endpoint) {
  if (!endpoint.location) return 'Source unavailable'
  return endpoint.location.startLine
    ? `${endpoint.location.sourceFileId}:${endpoint.location.startLine}`
    : endpoint.location.sourceFileId
}

function conditionLabel(endpoint) {
  if (!endpoint.conditions) return 'Conditions unavailable'
  if (endpoint.conditions.unresolved) return endpoint.conditionCount
    ? `${endpoint.conditionCount} known · Conditions unresolved`
    : 'Conditions unresolved'
  return endpoint.conditionCount ? `${endpoint.conditionCount} conditions` : 'No conditions'
}
</script>

<template>
  <section class="api-map" aria-labelledby="api-map-title">
    <header class="api-header">
      <div>
        <p class="workbench-kicker">API map</p>
        <h1 id="api-map-title">HTTP endpoints</h1>
        <p>Explore reported Spring MVC routes and trace each handler back to its mapping evidence.</p>
      </div>
      <div class="api-summary" aria-label="Endpoint summary">
        <article><strong>{{ model.counts.endpoints ?? '—' }}</strong><span>reported endpoints</span></article>
        <article><strong>{{ model.modules.filter((item) => item.endpointCount > 0).length }}</strong><span>modules with routes</span></article>
        <article :class="{ 'api-summary--warning': model.counts.unresolvedPaths }">
          <strong>{{ model.counts.unresolvedPaths ?? '—' }}</strong><span>unresolved paths</span>
        </article>
      </div>
    </header>

    <section class="api-filters" aria-label="API filters">
      <label>
        <span>Module</span>
        <select :value="scope.exploration.moduleId" @change="updateExploration({ moduleId: $event.target.value })">
          <option value="all">All modules ({{ model.counts.endpoints ?? '—' }})</option>
          <option v-for="module in model.modules" :key="module.id" :value="module.id">
            {{ module.label }} ({{ module.endpointCount }})
          </option>
        </select>
      </label>
      <label>
        <span>HTTP method</span>
        <select :value="scope.exploration.method" @change="updateExploration({ method: $event.target.value })">
          <option value="all">All methods ({{ model.counts.endpoints ?? '—' }})</option>
          <option v-for="option in model.methodOptions" :key="option.method" :value="option.method">
            {{ option.method }} ({{ option.count }})
          </option>
        </select>
      </label>
      <label class="api-search">
        <span>Search endpoints</span>
        <input
          type="search"
          :value="scope.exploration.query"
          placeholder="Path, controller, handler, source…"
          @input="updateExploration({ query: $event.target.value })"
        />
      </label>
      <button type="button" :disabled="!scope.filtersActive" @click="reset">Reset filters</button>
    </section>

    <div v-if="model.status === 'PARTIAL' || model.status === 'FAILED' || model.diagnostics?.length" class="api-coverage" role="status">
      <strong>{{ model.status === 'FAILED' ? 'Analysis failed' : model.status === 'PARTIAL' ? 'Coverage is limited' : 'Some endpoint facts are unresolved' }}</strong>
      <span>Confirmed endpoint facts remain available. {{ model.diagnostics?.length ?? 0 }} API diagnostics reported.</span>
    </div>

    <div class="api-scope-notice" aria-live="polite">
      <strong>{{ scope.counts.shownEndpoints }} of {{ scope.counts.reportedEndpoints }} reported endpoints shown</strong>
      <span>{{ scope.counts.filteredEndpoints }} filtered</span>
      <span v-if="scope.counts.unresolvedPaths">{{ scope.counts.unresolvedPaths }} unresolved path</span>
      <span v-if="scope.counts.unresolvedMethods">{{ scope.counts.unresolvedMethods }} unresolved method</span>
      <span v-if="scope.counts.anyMethods">{{ scope.counts.anyMethods }} ANY method</span>
      <span>Select a route to inspect evidence</span>
    </div>

    <div class="api-index">
      <StatePanel
        v-if="!model.collectionAvailable"
        variant="empty"
        heading="Endpoint data unavailable"
        message="This report does not provide an endpoint collection."
      />
      <StatePanel
        v-else-if="!model.endpoints.length"
        variant="empty"
        heading="No HTTP endpoints were reported"
        message="The endpoint collection is present and empty. Coverage diagnostics remain available when analysis is partial."
      />
      <StatePanel
        v-else-if="!scope.endpoints.length"
        variant="empty"
        heading="No endpoints match the current filters"
        message="Change the module, HTTP method, or search text, or reset the API filters."
      >
        <template #actions><button type="button" @click="reset">Reset filters</button></template>
      </StatePanel>

      <div v-else class="api-table-wrap">
        <table class="api-table">
          <caption class="sr-only">Reported HTTP endpoints matching the current filters</caption>
          <thead>
            <tr>
              <th scope="col">Method</th>
              <th scope="col">Path</th>
              <th scope="col">Handler / Controller</th>
              <th scope="col">Module</th>
              <th scope="col">Source / Conditions</th>
            </tr>
          </thead>
          <tbody>
            <tr
              v-for="endpoint in scope.endpoints"
              :key="endpoint.id"
              :class="{ 'api-row--selected': selection?.type === 'endpoint' && selection.id === endpoint.id }"
              @click="select(endpoint)"
            >
              <td data-label="Method">
                <button
                  type="button"
                  class="api-method-button"
                  :aria-label="`Inspect ${endpoint.methodLabel} ${endpoint.pathLabel} handled by ${endpoint.handlerLabel}`"
                  :aria-pressed="selection?.type === 'endpoint' && selection.id === endpoint.id"
                  @click.stop="select(endpoint)"
                >
                  <span class="api-method" :class="`api-method--${(endpoint.method ?? 'unavailable').toLowerCase()}`">
                    {{ endpoint.methodLabel }}
                  </span>
                </button>
              </td>
              <td data-label="Path">
                <code :class="{ 'api-path--unresolved': endpoint.pathState !== 'resolved' }">{{ endpoint.pathLabel }}</code>
              </td>
              <td data-label="Handler / Controller">
                <strong>{{ endpoint.handlerLabel }}</strong>
                <small>{{ endpoint.controllerQualifiedName ?? 'Controller unavailable' }}</small>
              </td>
              <td data-label="Module"><span class="api-module-badge">{{ endpoint.moduleLabel }}</span></td>
              <td data-label="Source / Conditions">
                <code>{{ source(endpoint) }}</code>
                <small :class="{ 'api-condition--unresolved': endpoint.conditions?.unresolved }">
                  {{ conditionLabel(endpoint) }}
                </small>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <footer class="api-statusbar">
      <span><i></i> AnalysisReport schema {{ model.schemaVersion ?? 'unavailable' }}</span>
      <span>HTTP endpoint index</span>
      <span>{{ scope.counts.shownEndpoints }} shown / {{ scope.counts.filteredEndpoints }} filtered</span>
    </footer>
  </section>
</template>
