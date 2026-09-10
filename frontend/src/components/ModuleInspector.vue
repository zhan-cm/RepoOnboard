<script setup>
defineProps({
  module: { type: Object, default: null },
  status: { type: String, default: 'UNKNOWN' },
  schemaVersion: { type: String, default: '' }
})

function shown(value) {
  return value ?? 'Unavailable'
}
</script>

<template>
  <aside class="module-inspector" aria-label="Module inspector">
    <header class="module-inspector__header">
      <p class="workbench-kicker">Inspector</p>
      <h2>{{ module?.label ?? 'No module selected' }}</h2>
      <p>Secondary metadata, diagnostics, and traceable source evidence for the selected module.</p>
    </header>
    <div v-if="module" class="module-inspector__body">
      <section>
        <h3>Coordinates</h3>
        <dl class="inspector-facts">
          <div><dt>GAV</dt><dd>{{ shown(module.gav) }}</dd></div>
          <div><dt>Packaging</dt><dd>{{ shown(module.packaging) }}</dd></div>
          <div><dt>Parent module</dt><dd>{{ shown(module.parentLabel) }}</dd></div>
        </dl>
      </section>

      <section>
        <h3>Analysis context</h3>
        <dl class="inspector-facts">
          <div><dt>Repository status</dt><dd>{{ status }}</dd></div>
          <div><dt>Report schema</dt><dd>{{ shown(schemaVersion) }}</dd></div>
        </dl>
      </section>

      <section>
        <h3>Diagnostics</h3>
        <ul v-if="module.diagnostics?.length" class="inspector-list">
          <li v-for="item in module.diagnostics" :key="`${item.code}:${item.message}`">
            <strong>{{ item.code }}</strong>
            <span>{{ item.message }}</span>
          </li>
        </ul>
        <p v-else-if="module.diagnostics" class="inspector-empty">No module-scoped diagnostics were reported.</p>
        <p v-else class="inspector-empty">Diagnostics are unavailable.</p>
      </section>

      <section>
        <h3>Source evidence</h3>
        <ul v-if="module.evidence.length" class="inspector-list">
          <li v-for="item in module.evidence" :key="`${item.ruleId}:${item.location}`">
            <strong>{{ item.type }}</strong>
            <code>{{ shown(item.location) }}</code>
            <span>{{ shown(item.ruleId) }}</span>
          </li>
        </ul>
        <p v-else class="inspector-empty">No source evidence was reported.</p>
      </section>
    </div>
    <p v-else class="module-empty-copy">Select a module to inspect its confirmed report facts.</p>
  </aside>
</template>
