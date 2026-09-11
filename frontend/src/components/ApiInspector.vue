<script setup>
import { computed } from 'vue'

const props = defineProps({
  model: { type: Object, default: null },
  scope: { type: Object, default: null },
  selection: { type: Object, default: null }
})

const emit = defineEmits(['open-source'])

const endpoint = computed(() => props.selection?.type === 'endpoint'
  ? props.model?.endpoints.find((item) => item.id === props.selection.id) ?? null
  : null)
const conditionGroups = computed(() => endpoint.value?.conditions
  ? [
      ['Params', endpoint.value.conditions.params],
      ['Headers', endpoint.value.conditions.headers],
      ['Consumes', endpoint.value.conditions.consumes],
      ['Produces', endpoint.value.conditions.produces]
    ]
  : [])

function shown(value) {
  return value ?? 'Unavailable'
}

function position(value) {
  if (!value?.startLine) return 'Unavailable'
  const end = value.endLine && value.endLine !== value.startLine ? `–${value.endLine}` : ''
  const column = value.startColumn ? `, column ${value.startColumn}` : ''
  return `Line ${value.startLine}${end}${column}`
}

function evidenceTitle(type) {
  if (type === 'SPRING_MVC_TYPE_MAPPING') return 'Controller class mapping'
  if (type === 'SPRING_MVC_METHOD_MAPPING') return 'Handler method mapping'
  return type ?? 'Other mapping evidence'
}
</script>

<template>
  <aside class="api-inspector" aria-label="API inspector">
    <header class="api-inspector__header">
      <div>
        <p class="workbench-kicker">Inspector</p>
        <h2>{{ endpoint ? 'Endpoint Inspector' : 'API Inspector' }}</h2>
      </div>
      <span v-if="endpoint" class="api-inspector__badge">{{ endpoint.framework ?? 'ENDPOINT' }}</span>
    </header>

    <div v-if="endpoint" class="api-inspector__body">
      <section>
        <p class="api-inspector__label">Selected endpoint</p>
        <div class="api-inspector__route">
          <span class="api-method" :class="`api-method--${(endpoint.method ?? 'unavailable').toLowerCase()}`">
            {{ endpoint.methodLabel }}
          </span>
          <code :class="{ 'api-path--unresolved': endpoint.pathState !== 'resolved' }">{{ endpoint.pathLabel }}</code>
        </div>
        <dl class="api-inspector__facts">
          <div><dt>Handler method</dt><dd>{{ shown(endpoint.handlerMethod) }}</dd></div>
          <div><dt>Controller class</dt><dd>{{ shown(endpoint.controllerQualifiedName) }}</dd></div>
          <div v-if="endpoint.controller"><dt>Component kind</dt><dd>{{ shown(endpoint.controller.kindLabel) }}</dd></div>
          <div><dt>Owning module</dt><dd>{{ endpoint.moduleLabel }}</dd></div>
          <div><dt>Framework</dt><dd>{{ shown(endpoint.framework) }}</dd></div>
        </dl>
      </section>

      <section>
        <p class="api-inspector__label">Mapping conditions</p>
        <dl v-if="endpoint.conditions" class="api-condition-list">
          <div v-for="group in conditionGroups" :key="group[0]">
            <dt>{{ group[0] }}</dt>
            <dd>
              <template v-if="group[1]?.length">
                <span v-for="value in group[1]" :key="value">{{ value }}</span>
              </template>
              <em v-else>{{ group[1] ? 'None reported' : 'Unavailable' }}</em>
            </dd>
          </div>
        </dl>
        <p v-else class="api-inspector__empty">Mapping conditions are unavailable.</p>
        <p v-if="endpoint.conditions?.unresolved" class="api-unresolved-note">Some mapping conditions remain unresolved.</p>
      </section>

      <section>
        <p class="api-inspector__label">Handler source</p>
        <dl class="api-inspector__facts">
          <div><dt>Declaration file</dt><dd>{{ shown(endpoint.location?.sourceFileId) }}</dd></div>
          <div><dt>Position</dt><dd>{{ position(endpoint.location) }}</dd></div>
          <div><dt>Symbol</dt><dd>{{ shown(endpoint.location?.symbol) }}</dd></div>
        </dl>
        <button
          v-if="endpoint.location"
          type="button"
          class="source-detail-trigger"
          @click="emit('open-source', { type: 'endpoint', id: endpoint.id })"
        >View source details</button>
      </section>

      <section>
        <p class="api-inspector__label">Mapping evidence</p>
        <div v-if="endpoint.evidence?.length" class="api-evidence-groups">
          <article v-for="item in endpoint.evidence" :key="`${item.type}:${item.ruleId}:${item.location?.display}`">
            <header><strong>{{ evidenceTitle(item.type) }}</strong><span>{{ shown(item.type) }}</span></header>
            <dl>
              <div><dt>Location</dt><dd>{{ shown(item.location?.display) }}</dd></div>
              <div><dt>Rule ID</dt><dd>{{ shown(item.ruleId) }}</dd></div>
            </dl>
            <ul v-if="item.relatedLocations.length">
              <li v-for="location in item.relatedLocations" :key="location.display">Related: {{ location.display }}</li>
            </ul>
          </article>
        </div>
        <p v-else-if="endpoint.evidence" class="api-inspector__empty">No mapping evidence was reported.</p>
        <p v-else class="api-inspector__empty">Mapping evidence is unavailable.</p>
      </section>
    </div>

    <div v-else class="api-inspector__body">
      <section>
        <p class="api-inspector__label">Current scope</p>
        <h3>HTTP endpoint index</h3>
        <p class="api-inspector__hint">Select an endpoint to inspect its handler and mapping evidence.</p>
        <dl v-if="scope" class="api-inspector__facts">
          <div><dt>Reported endpoints</dt><dd>{{ scope.counts.reportedEndpoints }}</dd></div>
          <div><dt>Shown endpoints</dt><dd>{{ scope.counts.shownEndpoints }}</dd></div>
          <div><dt>ANY methods</dt><dd>{{ scope.counts.anyMethods }}</dd></div>
          <div><dt>Unresolved methods</dt><dd>{{ scope.counts.unresolvedMethods }}</dd></div>
          <div><dt>Unresolved paths</dt><dd>{{ scope.counts.unresolvedPaths }}</dd></div>
          <div><dt>Analysis status</dt><dd>{{ model?.status ?? 'UNKNOWN' }}</dd></div>
        </dl>
      </section>
      <section v-if="model?.diagnostics?.length">
        <p class="api-inspector__label">API diagnostics</p>
        <ul class="api-diagnostic-list">
          <li v-for="item in model.diagnostics.slice(0, 5)" :key="`${item.code}:${item.message}`">
            <strong>{{ shown(item.code) }}</strong><span>{{ shown(item.message) }}</span>
          </li>
        </ul>
      </section>
    </div>
  </aside>
</template>
