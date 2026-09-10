<script setup>
import { computed } from 'vue'

const props = defineProps({
  model: { type: Object, default: null },
  scope: { type: Object, default: null },
  selection: { type: Object, default: null }
})

const component = computed(() => props.selection?.type === 'component'
  ? props.scope?.nodes.find((item) => item.id === props.selection.id) ?? null
  : null)
const dependency = computed(() => props.selection?.type === 'dependency'
  ? props.scope?.edges.find((item) => item.id === props.selection.id) ?? null
  : null)
const incoming = computed(() => component.value
  ? props.scope?.edges.filter((item) => item.targetId === component.value.id) ?? []
  : [])
const outgoing = computed(() => component.value
  ? props.scope?.edges.filter((item) => item.sourceId === component.value.id) ?? []
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
</script>

<template>
  <aside class="architecture-inspector" aria-label="Architecture inspector">
    <header class="architecture-inspector__header">
      <div>
        <p class="workbench-kicker">Inspector</p>
        <h2 v-if="component">Component Inspector</h2>
        <h2 v-else-if="dependency">Relationship Inspector</h2>
        <h2 v-else>Architecture Inspector</h2>
      </div>
      <span v-if="component" class="architecture-inspector__badge">{{ component.kind }}</span>
      <span v-else-if="dependency" class="architecture-inspector__badge">CONFIRMED</span>
    </header>

    <div v-if="component" class="architecture-inspector__body">
      <section>
        <p class="architecture-inspector__label">Selected component</p>
        <h3>{{ component.label }}</h3>
        <code class="architecture-inspector__identity">{{ component.qualifiedName }}</code>
        <dl class="architecture-inspector__facts">
          <div v-if="component.name"><dt>Declared name</dt><dd>{{ component.name }}</dd></div>
          <div><dt>Owning module</dt><dd>{{ component.moduleLabel }}</dd></div>
          <div><dt>Framework</dt><dd>{{ shown(component.framework) }}</dd></div>
          <div><dt>Component role</dt><dd>{{ component.kindLabel }}</dd></div>
        </dl>
      </section>

      <section>
        <p class="architecture-inspector__label">Confirmed relations</p>
        <div class="architecture-relation-counts">
          <article><span>Incoming injections</span><strong>{{ incoming.length }}</strong></article>
          <article><span>Outgoing targets</span><strong>{{ outgoing.length }}</strong></article>
        </div>
        <ul v-if="incoming.length || outgoing.length" class="architecture-relation-list">
          <li v-for="edge in incoming" :key="`in:${edge.id}`">← {{ edge.source.label }}</li>
          <li v-for="edge in outgoing" :key="`out:${edge.id}`">→ {{ edge.target.label }}</li>
        </ul>
        <p v-else class="architecture-inspector__empty">No confirmed component relationships.</p>
      </section>

      <section>
        <p class="architecture-inspector__label">Source location</p>
        <dl class="architecture-inspector__facts">
          <div><dt>File</dt><dd>{{ shown(component.location?.sourceFileId) }}</dd></div>
          <div><dt>Position</dt><dd>{{ position(component.location) }}</dd></div>
          <div><dt>Symbol</dt><dd>{{ shown(component.location?.symbol) }}</dd></div>
        </dl>
      </section>

      <section>
        <p class="architecture-inspector__label">Source evidence</p>
        <ul v-if="component.evidence?.length" class="architecture-evidence-list">
          <li v-for="item in component.evidence" :key="`${item.ruleId}:${item.location?.display}`">
            <strong>{{ shown(item.type) }}</strong>
            <code>{{ shown(item.location?.display) }}</code>
            <span>{{ shown(item.ruleId) }}</span>
          </li>
        </ul>
        <p v-else-if="component.evidence" class="architecture-inspector__empty">No source evidence was reported.</p>
        <p v-else class="architecture-inspector__empty">Source evidence is unavailable.</p>
      </section>
    </div>

    <div v-else-if="dependency" class="architecture-inspector__body">
      <section>
        <p class="architecture-inspector__label">Selected relationship</p>
        <h3>{{ dependency.source.label }} → {{ dependency.target.label }}</h3>
        <dl class="architecture-inspector__facts">
          <div><dt>Relationship</dt><dd>{{ dependency.kindLabel }}</dd></div>
          <div><dt>Status</dt><dd>{{ dependency.status }}</dd></div>
          <div><dt>Declared target</dt><dd>{{ shown(dependency.declaredTarget) }}</dd></div>
          <div><dt>Source module</dt><dd>{{ dependency.source.moduleLabel }}</dd></div>
          <div><dt>Target module</dt><dd>{{ dependency.target.moduleLabel }}</dd></div>
          <div><dt>Source location</dt><dd>{{ shown(dependency.location?.display) }}</dd></div>
        </dl>
      </section>
      <section>
        <p class="architecture-inspector__label">Source evidence</p>
        <ul v-if="dependency.evidence?.length" class="architecture-evidence-list">
          <li v-for="item in dependency.evidence" :key="`${item.ruleId}:${item.location?.display}`">
            <strong>{{ shown(item.type) }}</strong>
            <code>{{ shown(item.location?.display) }}</code>
            <span>{{ shown(item.ruleId) }}</span>
          </li>
        </ul>
        <p v-else-if="dependency.evidence" class="architecture-inspector__empty">No relationship evidence was reported.</p>
        <p v-else class="architecture-inspector__empty">Relationship evidence is unavailable.</p>
      </section>
    </div>

    <div v-else class="architecture-inspector__body">
      <section>
        <p class="architecture-inspector__label">Current scope</p>
        <h3>{{ scope?.module?.label ?? 'No module selected' }}</h3>
        <p class="architecture-inspector__hint">Select a component or confirmed relationship to inspect its source evidence.</p>
        <dl v-if="scope" class="architecture-inspector__facts">
          <div><dt>Reported components</dt><dd>{{ scope.counts.components }}</dd></div>
          <div><dt>Confirmed relations</dt><dd>{{ shown(scope.counts.confirmedRelations) }}</dd></div>
          <div><dt>Ambiguous</dt><dd>{{ shown(scope.counts.ambiguousRelations) }}</dd></div>
          <div><dt>Unresolved</dt><dd>{{ shown(scope.counts.unresolvedRelations) }}</dd></div>
          <div><dt>Analysis status</dt><dd>{{ model?.status ?? 'UNKNOWN' }}</dd></div>
        </dl>
      </section>
      <section v-if="model?.diagnostics?.length">
        <p class="architecture-inspector__label">Coverage diagnostics</p>
        <ul class="architecture-evidence-list">
          <li v-for="item in model.diagnostics.slice(0, 4)" :key="`${item.code}:${item.message}`">
            <strong>{{ shown(item.code) }}</strong><span>{{ shown(item.message) }}</span>
          </li>
        </ul>
      </section>
    </div>
  </aside>
</template>
