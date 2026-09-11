<script setup>
import { ref } from 'vue'
import StatePanel from './StatePanel.vue'

const props = defineProps({
  model: { type: Object, required: true },
  host: { type: Object, required: true }
})

defineEmits(['close'])

const copyFeedback = ref('')

async function copy(kind, value) {
  const result = await props.host.copyText(value)
  const label = kind[0].toUpperCase() + kind.slice(1)
  if (result.status === 'copied') {
    copyFeedback.value = `${label} copied.`
  } else {
    copyFeedback.value = 'Copy unavailable — select the text manually.'
  }
}

function evidenceTitle(type) {
  if (type === 'SPRING_MVC_TYPE_MAPPING') return 'Controller class mapping'
  if (type === 'SPRING_MVC_METHOD_MAPPING') return 'Handler method mapping'
  return type ?? 'Evidence type unavailable'
}
</script>

<template>
  <main class="source-detail" aria-labelledby="source-detail-title">
    <div class="source-return-bar">
      <button type="button" class="source-back" @click="$emit('close')">
        <span aria-hidden="true">←</span>
        Back to {{ model.entityType === 'endpoint' ? 'endpoint' : 'component' }}
      </button>
      <span v-if="model.available" class="source-context-badge">
        {{ model.entityType === 'endpoint' ? 'Endpoint source detail' : 'Component source detail' }}
      </span>
      <span class="source-status" :class="`source-status--${model.status.toLowerCase()}`">{{ model.status }}</span>
    </div>

    <StatePanel
      v-if="!model.available"
      variant="warning"
      heading="Source selection unavailable"
      message="The selected entity is no longer present in this report. Return to the previous view and select it again."
    />

    <template v-else>
      <header class="source-header">
        <div>
          <p class="workbench-kicker">Source navigation · Evidence detail</p>
          <h1 id="source-detail-title">Source &amp; Evidence Detail</h1>
          <p>Trace the selected report entity to its scan-root-relative source location and recorded evidence.</p>
        </div>
        <div class="source-entity-summary">
          <span>{{ model.entityType === 'endpoint' ? 'Reported endpoint' : 'Reported component' }}</span>
          <div v-if="model.entityType === 'endpoint'" class="source-route">
            <span class="api-method" :class="`api-method--${model.entity.method.toLowerCase()}`">{{ model.entity.methodLabel }}</span>
            <code>{{ model.entity.pathLabel }}</code>
          </div>
          <strong v-else>{{ model.entity.label }}</strong>
          <code>{{ model.entityType === 'endpoint' ? model.entity.handlerMethod : model.entity.qualifiedName }}</code>
        </div>
      </header>

      <div class="source-detail__body">
        <section class="source-card" aria-labelledby="primary-source-title">
          <header class="source-card__header">
            <h2 id="primary-source-title">Primary Source Location</h2>
            <span>{{ model.entityType === 'endpoint' ? 'Handler declaration' : 'Component declaration' }}</span>
          </header>

          <div v-if="model.location" class="source-card__content">
            <div class="source-field source-field--wide">
              <div class="source-field__heading"><span>Scan-root-relative source path</span>
                <button type="button" @click="copy('path', model.location.sourceFileId)">Copy path</button>
              </div>
              <code>{{ model.location.sourceFileId }}</code>
            </div>

            <div class="source-position-grid">
              <div class="source-field">
                <span>Start position (1-based)</span>
                <strong>{{ model.locationText.start }}</strong>
              </div>
              <div class="source-field">
                <span>End position (1-based)</span>
                <strong>{{ model.locationText.end ?? 'End position unavailable' }}</strong>
              </div>
              <div class="source-field">
                <span>Source file metadata</span>
                <strong v-if="model.sourceFile">Language: {{ model.sourceFile.language ?? 'Unavailable' }}</strong>
                <strong v-else>Source file metadata unavailable</strong>
              </div>
            </div>

            <p v-if="!model.location.startLine" class="source-unavailable-note">Exact line and column were not reported.</p>

            <div class="source-field source-field--wide">
              <div class="source-field__heading"><span>Reported symbol</span>
                <button
                  type="button"
                  :disabled="!model.location.symbol"
                  @click="copy('symbol', model.location.symbol)"
                >Copy symbol</button>
              </div>
              <code>{{ model.location.symbol ?? 'Symbol unavailable' }}</code>
            </div>

            <div class="source-copy-row">
              <code>{{ model.copyLocationText }}</code>
              <button type="button" @click="copy('location', model.copyLocationText)">Copy location</button>
            </div>
            <p class="source-copy-feedback" aria-live="polite">{{ copyFeedback || 'Source text remains selectable for manual copy.' }}</p>
          </div>
          <div v-else class="source-card__content">
            <p class="source-empty">Source location is unavailable for this entity.</p>
          </div>
        </section>

        <section class="source-card" aria-labelledby="source-context-title">
          <header class="source-card__header"><h2 id="source-context-title">Module &amp; Source File</h2></header>
          <div class="source-card__content source-context-grid">
            <dl>
              <div><dt>Module</dt><dd>{{ model.module?.label ?? 'Module details unavailable' }}</dd></div>
              <div><dt>Base directory</dt><dd>{{ model.module?.baseDirectory ?? 'Unavailable' }}</dd></div>
              <div><dt>POM</dt><dd>{{ model.module?.pomFileId ?? 'Unavailable' }}</dd></div>
            </dl>
            <dl>
              <div><dt>Source file ID</dt><dd>{{ model.sourceFile?.id ?? 'Source file metadata unavailable' }}</dd></div>
              <div><dt>Language</dt><dd>{{ model.sourceFile?.language ?? 'Unavailable' }}</dd></div>
              <div><dt>Report schema</dt><dd>{{ model.schemaVersion ?? 'Unavailable' }}</dd></div>
            </dl>
          </div>
        </section>

        <section class="source-card" aria-labelledby="source-evidence-title">
          <header class="source-card__header">
            <h2 id="source-evidence-title">Evidence</h2>
            <span>{{ model.evidence ? `${model.evidence.length} reported` : 'Unavailable' }}</span>
          </header>
          <div class="source-card__content">
            <div v-if="model.evidence?.length" class="source-evidence-list">
              <article v-for="item in model.evidence" :key="`${item.index}:${item.ruleId}`">
                <header><span>{{ item.index + 1 }}</span><strong>{{ evidenceTitle(item.type) }}</strong><code>{{ item.type ?? 'Unavailable' }}</code></header>
                <dl>
                  <div><dt>Rule ID</dt><dd>{{ item.ruleId ?? 'Unavailable' }}</dd></div>
                  <div><dt>Primary location</dt><dd>{{ item.location ? `${item.location.sourceFileId}${item.location.startLine ? `:${item.location.startLine}` : ''}${item.location.startColumn ? `:${item.location.startColumn}` : ''}` : 'Unavailable' }}</dd></div>
                </dl>
                <ul v-if="item.relatedLocations.length">
                  <li v-for="location in item.relatedLocations" :key="`${location.sourceFileId}:${location.startLine}:${location.startColumn}`">
                    {{ location.sourceFileId }}<template v-if="location.startLine">:{{ location.startLine }}</template><template v-if="location.startColumn">:{{ location.startColumn }}</template>
                  </li>
                </ul>
                <p v-else>No related locations were reported.</p>
              </article>
            </div>
            <p v-else-if="model.evidence" class="source-empty">No source evidence was reported.</p>
            <p v-else class="source-empty">Source evidence is unavailable.</p>
          </div>
        </section>
      </div>
    </template>
  </main>
</template>
