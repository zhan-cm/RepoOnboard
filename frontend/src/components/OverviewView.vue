<script setup>
import { computed } from 'vue'
import MetricCard from './MetricCard.vue'
import StatePanel from './StatePanel.vue'
import { createOverviewModel } from '../lib/reportOverview.js'

const props = defineProps({
  report: {
    type: Object,
    required: true
  }
})

const overview = computed(() => createOverviewModel(props.report))
const visibleDiagnostics = computed(() => overview.value.diagnostics?.slice(0, 5) ?? [])
</script>

<template>
  <div class="overview">
    <StatePanel
      :variant="overview.coverage.variant === 'success'
        ? 'ready'
        : overview.coverage.variant === 'neutral' ? 'empty' : overview.coverage.variant"
      :class="`coverage-panel--${overview.coverage.variant}`"
      :heading="overview.coverage.heading"
      :message="overview.coverage.message"
    >
      <template #details>
        <div class="coverage-details">
          <span>{{ overview.coverage.diagnosticCount ?? 'Unknown' }} significant diagnostics</span>
          <template v-if="overview.coverage.limitationCodes.length">
            <span
              v-for="code in overview.coverage.limitationCodes"
              :key="code"
            >{{ code }}</span>
          </template>
          <span v-else>No limitation codes reported</span>
        </div>
      </template>
    </StatePanel>

    <section class="overview-section" aria-labelledby="overview-at-a-glance">
      <div class="section-heading">
        <div>
          <p class="eyebrow">Report summary</p>
          <h2 id="overview-at-a-glance">Project at a glance</h2>
        </div>
        <p>Counts come from the versioned report summary or directly from report entities.</p>
      </div>
      <div class="metric-grid">
        <MetricCard
          v-for="item in overview.metrics"
          :key="item.label"
          :label="item.label"
          :value="item.value"
          :description="item.description"
        />
      </div>
    </section>

    <div class="overview-columns">
      <section class="overview-section overview-card" aria-labelledby="technology-facts">
        <div class="section-heading section-heading--stacked">
          <p class="eyebrow">Confirmed metadata</p>
          <h2 id="technology-facts">Technology facts</h2>
        </div>
        <dl class="fact-list">
          <div>
            <dt>Build system</dt>
            <dd>{{ overview.buildSystem ?? 'Not available' }}</dd>
          </div>
          <div>
            <dt>Languages</dt>
            <dd>{{ overview.languages.length ? overview.languages.join(', ') : 'Not available' }}</dd>
          </div>
          <div>
            <dt>Frameworks</dt>
            <dd>{{ overview.frameworks.length ? overview.frameworks.join(', ') : 'Not available' }}</dd>
          </div>
          <div>
            <dt>Source roots</dt>
            <dd>{{ overview.sourceRoots.length ? overview.sourceRoots.length.toLocaleString() : 'Not available' }}</dd>
          </div>
        </dl>
        <ul v-if="overview.sourceRoots.length" class="source-root-list" aria-label="Reported source roots">
          <li v-for="root in overview.sourceRoots" :key="root">{{ root }}</li>
        </ul>
        <p v-else class="empty-copy">No source roots are available in this report.</p>
      </section>

      <section class="overview-section overview-card" aria-labelledby="component-profile">
        <div class="section-heading section-heading--stacked">
          <p class="eyebrow">Framework roles</p>
          <h2 id="component-profile">Component profile</h2>
        </div>
        <div class="component-metrics">
          <MetricCard
            v-for="item in overview.componentMetrics"
            :key="item.label"
            compact
            :label="item.label"
            :value="item.value"
          />
        </div>
      </section>
    </div>

    <section class="overview-section overview-card" aria-labelledby="application-entry-points">
      <div class="section-heading">
        <div>
          <p class="eyebrow">Confirmed starts</p>
          <h2 id="application-entry-points">Application entry points</h2>
        </div>
        <p>Only entry points explicitly present in the report are shown.</p>
      </div>
      <ul v-if="overview.entryPoints?.length" class="entry-point-list">
        <li v-for="entryPoint in overview.entryPoints" :key="entryPoint.id ?? entryPoint.name">
          <div>
            <strong>{{ entryPoint.name ?? 'Unnamed entry point' }}</strong>
            <span>{{ entryPoint.kind ?? 'Kind unavailable' }}</span>
          </div>
          <dl>
            <div>
              <dt>Module</dt>
              <dd>{{ entryPoint.moduleId ?? 'Not available' }}</dd>
            </div>
            <div>
              <dt>Source</dt>
              <dd>{{ entryPoint.location ?? 'Not available' }}</dd>
            </div>
          </dl>
        </li>
      </ul>
      <p v-else class="empty-copy">No application entry points are available in this report.</p>
    </section>

    <section class="overview-section overview-card" aria-labelledby="analysis-diagnostics">
      <div class="section-heading">
        <div>
          <p class="eyebrow">Coverage context</p>
          <h2 id="analysis-diagnostics">Warnings and errors</h2>
        </div>
        <p v-if="overview.diagnostics?.length">
          Showing {{ visibleDiagnostics.length }} of {{ overview.diagnostics.length }} significant diagnostics.
        </p>
      </div>
      <ul v-if="visibleDiagnostics.length" class="diagnostic-list">
        <li v-for="(diagnostic, index) in visibleDiagnostics" :key="`${diagnostic.code}-${index}`">
          <span class="diagnostic-list__severity">{{ diagnostic.severity ?? 'UNKNOWN' }}</span>
          <div>
            <strong>{{ diagnostic.code ?? 'Code unavailable' }}</strong>
            <p>{{ diagnostic.message ?? 'Diagnostic message unavailable.' }}</p>
            <span v-if="diagnostic.stage">Stage: {{ diagnostic.stage }}</span>
          </div>
        </li>
      </ul>
      <p v-else class="empty-copy">No warnings or errors are reported.</p>
    </section>
  </div>
</template>
