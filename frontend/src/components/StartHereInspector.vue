<script setup>
defineProps({
  model: { type: Object, required: true },
  item: { type: Object, default: null }
})

defineEmits(['explore-component', 'explore-endpoint'])
</script>

<template>
  <aside class="start-here-inspector" aria-label="Reading inspector">
    <header class="start-here-inspector__header">
      <div>
        <p class="workbench-kicker">Inspector</p>
        <h2>Reading Inspector</h2>
      </div>
      <span v-if="item">Step {{ item.index }} of {{ model.totalItemCount }}</span>
    </header>

    <div v-if="item" class="start-here-inspector__body">
      <section class="start-here-inspector__identity">
        <p class="start-here-inspector__label">Selected file</p>
        <h3>{{ item.fileName }}</h3>
        <code>{{ item.sourceFileId }}</code>
        <dl>
          <div><dt>Owning module</dt><dd>{{ item.moduleLabel }}</dd></div>
          <div><dt>Source metadata</dt><dd>{{ item.sourceFile?.language ?? 'Unavailable' }}</dd></div>
        </dl>
      </section>

      <section>
        <div class="start-here-inspector__section-title">
          <p class="start-here-inspector__label">Why this file?</p>
          <span>{{ item.reasons.length }} {{ item.reasons.length === 1 ? 'reason' : 'reasons' }}</span>
        </div>
        <div class="start-here-reasons">
          <article v-for="reason in item.reasons" :key="`${reason.kind}:${reason.message}`">
            <header>
              <span class="start-here-kind" :class="`start-here-kind--${reason.kind.toLowerCase()}`">{{ reason.kindLabel }}</span>
              <small>{{ reason.factCount }} supporting {{ reason.factCount === 1 ? 'fact' : 'facts' }}</small>
            </header>
            <strong>{{ reason.message }}</strong>
            <p v-if="reason.dependencyDistance">Confirmed dependency distance: {{ reason.dependencyDistance }}</p>
          </article>
        </div>
      </section>

      <section>
        <p class="start-here-inspector__label">Source evidence</p>
        <div class="start-here-evidence">
          <template v-for="reason in item.reasons" :key="`evidence:${reason.kind}:${reason.message}`">
            <article v-for="(evidence, index) in reason.evidence" :key="`${reason.kind}:${evidence.ruleId}:${evidence.location.display}:${index}`">
              <header><strong>{{ evidence.type }}</strong><span>{{ reason.kindLabel }}</span></header>
              <dl>
                <div><dt>Rule ID</dt><dd>{{ evidence.ruleId }}</dd></div>
                <div><dt>Location</dt><dd><code>{{ evidence.location.display }}</code></dd></div>
              </dl>
              <ul v-if="evidence.relatedLocations.length">
                <li v-for="location in evidence.relatedLocations" :key="location.display"><code>{{ location.display }}</code></li>
              </ul>
            </article>
          </template>
        </div>
      </section>

      <section>
        <p class="start-here-inspector__label">Related exploration</p>
        <div v-if="item.relatedComponents.length" class="start-here-targets">
          <article v-for="component in item.relatedComponents" :key="component.id">
            <span>{{ component.kindLabel }}</span>
            <strong>{{ component.label }}</strong>
            <code>{{ component.qualifiedName }}</code>
            <button type="button" @click="$emit('explore-component', component.id)">Explore {{ component.label }} in Architecture</button>
          </article>
        </div>
        <div v-if="item.relatedEndpoints.length" class="start-here-targets start-here-targets--endpoints">
          <article v-for="endpoint in item.relatedEndpoints" :key="endpoint.id">
            <div><span class="api-method" :class="`api-method--${endpoint.method.toLowerCase()}`">{{ endpoint.methodLabel }}</span><code>{{ endpoint.pathLabel }}</code></div>
            <span>{{ endpoint.handlerMethod }}</span>
            <button type="button" @click="$emit('explore-endpoint', endpoint.id)">Explore endpoint in API Map</button>
          </article>
        </div>
        <p v-if="!item.relatedComponents.length && !item.relatedEndpoints.length" class="start-here-inspector__empty">No component or endpoint navigation target is attached to this recommendation.</p>
      </section>
    </div>

    <div v-else class="start-here-inspector__body">
      <section>
        <p class="start-here-inspector__label">Reading guide</p>
        <h3>Select a recommended file</h3>
        <p class="start-here-inspector__hint">Inspect all verified reasons, source evidence, and exact Component or Endpoint navigation targets.</p>
        <dl class="start-here-inspector__facts">
          <div><dt>Recommended files</dt><dd>{{ model.totalItemCount }}</dd></div>
          <div><dt>Analysis status</dt><dd>{{ model.analysisStatus }}</dd></div>
          <div><dt>Ranking</dt><dd>Confirmed rules only</dd></div>
        </dl>
      </section>
      <section v-if="model.coverageLimitationCodes.length">
        <p class="start-here-inspector__label">Coverage limitation codes</p>
        <ul class="start-here-inspector__codes">
          <li v-for="code in model.coverageLimitationCodes" :key="code"><code>{{ code }}</code></li>
        </ul>
      </section>
    </div>
  </aside>
</template>
