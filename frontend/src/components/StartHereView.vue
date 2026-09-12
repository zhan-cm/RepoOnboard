<script setup>
import StatePanel from './StatePanel.vue'
import { visibleStartHereItems } from '../lib/reportStartHere.js'

const props = defineProps({
  model: { type: Object, required: true },
  expanded: { type: Boolean, default: false },
  selectedSourceFileId: { type: String, default: '' }
})

defineEmits(['select', 'update-expanded'])

function step(index) {
  return String(index).padStart(2, '0')
}
</script>

<template>
  <section class="start-here" aria-labelledby="start-here-title">
    <header class="start-here__header">
      <div>
        <p class="workbench-kicker">Onboarding guide</p>
        <h1 id="start-here-title">Start Here</h1>
        <p>Follow a deterministic, source-backed path through the repository's build, entry points, APIs, and confirmed dependencies.</p>
      </div>
      <div class="start-here__summary" aria-label="Reading guide summary">
        <article><strong>{{ model.totalItemCount }}</strong><span>recommended files</span></article>
        <article><strong>{{ model.defaultLimit }}</strong><span>default view</span></article>
        <p>Ordered by confirmed report facts · No AI score</p>
      </div>
    </header>

    <div v-if="model.coverageLimited" class="start-here__coverage" role="status">
      <strong>{{ model.analysisStatus === 'FAILED' ? 'Analysis failed' : 'Coverage is limited' }}</strong>
      <span>{{ model.coverageNotice ?? 'Analysis diagnostics may limit this reading guide.' }}</span>
      <small>{{ model.coverageLimitationCodes.length }} coverage limitation codes reported</small>
    </div>

    <div class="start-here__scope" aria-live="polite">
      <strong>{{ visibleStartHereItems(model, expanded).length }} of {{ model.totalItemCount }} recommended files shown</strong>
      <span>Immutable, source-backed sequence</span>
    </div>

    <StatePanel
      v-if="!model.items.length"
      :variant="model.analysisStatus === 'FAILED' ? 'error' : 'empty'"
      heading="No recommended files could be produced"
      message="No reading path could be derived from the confirmed build, entry point, endpoint, configuration, or dependency facts in this report."
    />

    <ol v-else class="start-here__list" aria-label="Recommended source reading order">
      <li v-for="item in visibleStartHereItems(model, expanded)" :key="item.sourceFileId">
        <button
          type="button"
          class="start-here-item"
          :class="{ 'start-here-item--selected': selectedSourceFileId === item.sourceFileId }"
          :aria-current="selectedSourceFileId === item.sourceFileId ? 'step' : undefined"
          @click="$emit('select', item.sourceFileId)"
        >
          <span class="start-here-item__step">{{ step(item.index) }}</span>
          <span class="start-here-item__content">
            <span class="start-here-item__heading">
              <strong>{{ item.fileName }}</strong>
              <span class="start-here-kind" :class="`start-here-kind--${item.primaryReason.kind.toLowerCase()}`">
                {{ item.primaryReason.kindLabel }}
              </span>
              <span class="start-here-module">{{ item.moduleLabel }}</span>
              <span v-if="item.additionalReasonCount" class="start-here-more">+{{ item.additionalReasonCount }} more {{ item.additionalReasonCount === 1 ? 'reason' : 'reasons' }}</span>
            </span>
            <code>{{ item.sourceFileId }}</code>
            <span class="start-here-item__reason">{{ item.primaryReason.message }}</span>
          </span>
          <span class="start-here-item__inspect">
            {{ item.reasons.length }} {{ item.reasons.length === 1 ? 'reason' : 'reasons' }}
            <small>{{ selectedSourceFileId === item.sourceFileId ? 'Viewing details' : 'Inspect →' }}</small>
          </span>
        </button>
      </li>
    </ol>

    <div v-if="model.expandable" class="start-here__expansion">
      <span>{{ expanded ? `Showing all ${model.totalItemCount} recommended files` : `${model.hiddenItemCount} recommendations are hidden by the default view` }}</span>
      <button
        type="button"
        :aria-expanded="expanded"
        @click="$emit('update-expanded', !expanded)"
      >{{ expanded ? `Show first ${model.defaultLimit}` : `Show ${model.hiddenItemCount} more recommended ${model.hiddenItemCount === 1 ? 'file' : 'files'}` }}</button>
    </div>

    <footer class="start-here__statusbar">
      <span><i></i> Start Here projection {{ model.schemaVersion }}</span>
      <span>Report schema {{ model.reportSchemaVersion }}</span>
      <span>{{ model.analysisStatus }}</span>
    </footer>
  </section>
</template>
